#!/usr/bin/env python3
"""
공공데이터 -> V2__seed_master.sql 변환 (T-07)

입력 (공공데이터포털 OpenAPI, .env 의 PHARMACY_API_KEY / DRUG_API_KEY 필요):
  - 국립중앙의료원_전국 약국 정보 조회 서비스 (data.go.kr 15000576)
  - 식약처_의약품개요정보(e약은요) (data.go.kr 15075057)

출력:
  - miniproject1-backend/src/main/resources/db/migration/V2__seed_master.sql
  - tools/seed-generator/data/ 아래에 원본 API 응답 캐시 (재현성용)

재실행 가능: API 응답은 data/ 에 캐시되어 두 번째 실행부터는 네트워크 호출 없이 캐시를 읽는다.
"""
from __future__ import annotations

import math
import random
import re
import sys
import xml.etree.ElementTree as ET
from collections import defaultdict
from pathlib import Path

import bcrypt
import requests

ROOT = Path(__file__).resolve().parents[2]
DATA_DIR = Path(__file__).resolve().parent / "data"
OUT_SQL = ROOT / "miniproject1-backend/src/main/resources/db/migration/V2__seed_master.sql"

PHARMACY_ENDPOINT = "https://apis.data.go.kr/B552657/ErmctInsttInfoInqireService/getParmacyListInfoInqire"
DRUG_ENDPOINT = "https://apis.data.go.kr/1471000/DrbEasyDrugInfoService/getDrbEasyDrugList"

TARGET_SIDO = ["서울특별시", "경기도"]
PHARMACY_MIN, PHARMACY_MAX = 300, 500
DRUG_MIN, DRUG_MAX = 30, 50

RANDOM_SEED = 20260915

# 응급의료정보시스템(e-Gen) 약국 API 응답 필드. 시도/시군구 구분 필드가 없어 dutyAddr 문자열에서 파싱한다.
PHARMACY_FIELD_SYNONYMS = {
    "name": ["dutyName"],
    "lng": ["wgs84Lon"],
    "lat": ["wgs84Lat"],
    "addr_road": ["dutyAddr"],
    "tel": ["dutyTel1"],
    "hira_code": ["hpid"],
}

# 식약처 e약은요 API 응답 필드
DRUG_FIELD_SYNONYMS = {
    "item_seq": ["itemSeq"],
    "item_name": ["itemName"],
    "entp_name": ["entpName"],
    "efcy": ["efcyQesitm"],
    "image": ["itemImage"],
}

# e약은요 itemName은 실제 상품명(예: "타치온정50밀리그램") 위주라 상품명 키워드 매칭은 못 쓴다.
# 대신 이름 끝에 붙는 제형 접미사로 category를 분류한다.
# ROADMAP이 정한 category 값(해열진통/소화제/감기약/연고/소독약/비타민/기타)만 쓴다.
CATEGORY_BY_FORM_SUFFIX = [
    ("연고", ["연고", "크림", "겔", "파스"]),
    ("소독약", ["점안액", "액", "포비돈", "좌제"]),
    ("소화제", ["산", "환", "과립"]),
    ("해열진통", ["정", "캡슐", "질정", "트로키"]),
    ("감기약", ["시럽"]),
]

# category별 시중 대표 가격대(원). base_price는 T-08 시드 생성 기준값일 뿐 운영 로직에는 안 쓰인다.
CATEGORY_PRICE_RANGE = {
    "해열진통": (2000, 4000),
    "소화제": (3000, 6000),
    "감기약": (5000, 9000),
    "연고": (3000, 6000),
    "소독약": (2000, 5000),
    "비타민": (8000, 20000),
    "기타": (3000, 8000),
}

# 함량 표기(예: "50밀리그램", "2mg", "10%")를 package_unit으로 쓴다.
# 이 API는 포장 정 수(8정/16정)를 제공하지 않으므로 함량으로 대체한다.
PACKAGE_UNIT_RE = re.compile(r"(\d+(?:\.\d+)?)\s*(mg|mL|ml|g|%|밀리그램|밀리그람|그램|밀리리터)")


def env_value(key: str) -> str:
    env_path = ROOT / ".env"
    if not env_path.exists():
        sys.exit(f"[오류] {env_path} 가 없습니다. .env.example 을 참고해 .env 를 만드세요.")
    for line in env_path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if line.startswith(f"{key}="):
            value = line.split("=", 1)[1].strip()
            if value:
                return value
    sys.exit(f"[오류] .env 에 {key} 값이 비어 있습니다. 공공데이터포털에서 발급받아 채워주세요.")


def field(elem: ET.Element, synonyms: list[str]) -> str | None:
    for tag in synonyms:
        found = elem.find(tag)
        if found is not None and found.text:
            return found.text.strip()
    return None


def fetch_all_items(
    endpoint: str, service_key: str, extra_params: dict, cache_file: Path, max_items: int = 0
) -> list[ET.Element]:
    """페이지네이션을 순회해 <item> 목록을 모은다. 캐시가 있으면 캐시를 쓴다.
    max_items > 0 이면 그 개수만큼만 받고 멈춘다 (필요량보다 훨씬 큰 전체 데이터셋 호출 방지)."""
    if cache_file.exists():
        print(f"  캐시 사용: {cache_file.name}")
        root = ET.fromstring(cache_file.read_text(encoding="utf-8"))
        return root.findall(".//item")

    items: list[ET.Element] = []
    page = 1
    num_of_rows = 100
    merged_root = ET.Element("items")
    while True:
        # 공공데이터포털 서비스키는 이미 URL-encoded 상태로 발급된다.
        # requests의 params=에 넣으면 다시 인코딩되어(이중 인코딩) 400 오류가 나므로 쿼리스트링에 직접 붙인다.
        params = {"pageNo": page, "numOfRows": num_of_rows, **extra_params}
        query = requests.models.PreparedRequest()
        query.prepare_url(endpoint, params)
        url = f"{query.url}&serviceKey={service_key}"
        resp = requests.get(url, timeout=30)
        resp.raise_for_status()
        root = ET.fromstring(resp.text)
        header_code = root.findtext(".//resultCode") or root.findtext(".//header/resultCode")
        if header_code not in (None, "00", "0"):
            msg = root.findtext(".//resultMsg") or root.findtext(".//header/resultMsg") or "알 수 없는 오류"
            sys.exit(f"[오류] API 응답 실패 (code={header_code}): {msg}")

        page_items = root.findall(".//item")
        if not page_items:
            break
        for it in page_items:
            merged_root.append(it)
        items.extend(page_items)
        total_count = root.findtext(".//totalCount")
        if total_count and page * num_of_rows >= int(total_count):
            break
        if max_items and len(items) >= max_items:
            break
        page += 1

    DATA_DIR.mkdir(parents=True, exist_ok=True)
    cache_file.write_text(ET.tostring(merged_root, encoding="unicode"), encoding="utf-8")
    return items


def parse_sigungu(addr_road: str, sido: str) -> str:
    """'서울특별시 강남구 ...' 형태의 주소에서 시도 다음 토큰(시군구)을 뽑는다."""
    rest = addr_road[len(sido):].strip()
    return rest.split()[0] if rest else "기타"


def build_regions_and_pharmacies(service_key: str) -> tuple[list[dict], list[dict]]:
    print("약국 데이터 수집 중...")
    pharmacies = []
    for sido in TARGET_SIDO:
        raw_items = fetch_all_items(
            PHARMACY_ENDPOINT, service_key, {"Q0": sido}, DATA_DIR / f"pharmacy_raw_{sido}.xml", max_items=1000
        )
        for it in raw_items:
            lat = field(it, PHARMACY_FIELD_SYNONYMS["lat"])
            lng = field(it, PHARMACY_FIELD_SYNONYMS["lng"])
            addr_road = field(it, PHARMACY_FIELD_SYNONYMS["addr_road"])
            if not lat or not lng or not addr_road or float(lat) == 0 or float(lng) == 0:
                continue
            pharmacies.append(
                {
                    "hira_code": field(it, PHARMACY_FIELD_SYNONYMS["hira_code"]),
                    "name": field(it, PHARMACY_FIELD_SYNONYMS["name"]),
                    "addr_road": addr_road,
                    "addr_jibun": None,  # e-Gen API는 도로명 주소만 제공
                    "tel": field(it, PHARMACY_FIELD_SYNONYMS["tel"]),
                    "lat": float(lat),
                    "lng": float(lng),
                    "sido": sido,
                    "sigungu": parse_sigungu(addr_road, sido),
                }
            )

    if not pharmacies:
        sys.exit(
            "[오류] 필터링 후 약국 데이터가 0건입니다. PHARMACY_FIELD_SYNONYMS 의 태그명이 "
            "실제 응답과 다를 수 있습니다. data/pharmacy_raw.xml 을 열어 실제 태그명을 확인하세요."
        )

    # 시군구당 상한을 둬서 여러 시군구가 골고루 섞이게 하되(region 다양성),
    # 시군구당 최소치도 둬서 반경 2km 안에 5개 이상 잡히는 밀집도는 유지한다.
    by_sigungu: dict[str, list[dict]] = defaultdict(list)
    for p in pharmacies:
        by_sigungu[p["sigungu"]].append(p)

    SIGUNGU_MIN, SIGUNGU_MAX = 15, 20
    random.seed(RANDOM_SEED)
    ordered_sigungu = [s for s in sorted(by_sigungu, key=lambda k: -len(by_sigungu[k])) if len(by_sigungu[s]) >= SIGUNGU_MIN]
    candidate_pool: list[dict] = []
    for sigungu in ordered_sigungu:
        group = by_sigungu[sigungu]
        random.shuffle(group)
        candidate_pool.extend(group[:SIGUNGU_MAX])

    # 시군구 경계만으로는 "시군구 안이지만 변두리라 반경 2km 안이 텅 빈" 약국을 못 거른다.
    # 실제 haversine 거리로 후보군 내 이웃 수를 세서, 고립된(반경 2km 안에 5개 미만) 약국은 뺀다.
    def haversine_m(lat1: float, lng1: float, lat2: float, lng2: float) -> float:
        R = 6_371_000
        dlat, dlng = math.radians(lat2 - lat1), math.radians(lng2 - lng1)
        a = (math.sin(dlat / 2) ** 2
             + math.cos(math.radians(lat1)) * math.cos(math.radians(lat2)) * math.sin(dlng / 2) ** 2)
        return R * 2 * math.asin(math.sqrt(a))

    # ponytail: O(n^2) 전수 비교. 후보 풀이 수천 건으로 커지면 느려지므로, 그때는 공간 격자 인덱싱으로 바꾼다.
    # "채택된 집합 안에서" 이웃 수를 반복 재계산한다 — 500건으로 자르는 순간 이웃 일부가 빠져나가
    # 밀집도가 다시 깨질 수 있어(경계 효과), 안정될 때까지(또는 최대 횟수까지) 미달분을 걸러내고 재보충한다.
    NEIGHBOR_MIN = 5  # 완료 판정: 임의 좌표 기준 반경 2km 내 약국 5개 이상
    working_set = sorted(
        candidate_pool,
        key=lambda p: -sum(1 for q in candidate_pool if haversine_m(p["lat"], p["lng"], q["lat"], q["lng"]) <= 2000),
    )[:PHARMACY_MAX]
    working_ids = {id(p) for p in working_set}
    leftover = [p for p in candidate_pool if id(p) not in working_ids]

    for _ in range(10):
        counts = {
            id(p): sum(1 for q in working_set if haversine_m(p["lat"], p["lng"], q["lat"], q["lng"]) <= 2000)
            for p in working_set
        }
        deficient_ids = {id(p) for p in working_set if counts[id(p)] < NEIGHBOR_MIN}
        if not deficient_ids:
            break
        working_set = [p for p in working_set if id(p) not in deficient_ids]
        # 뺀 만큼 밀집도가 가장 높은 지역(=이미 풍부한 이웃) 근처 후보로 채워 총량(PHARMACY_MAX)을 유지한다
        leftover.sort(
            key=lambda p: -sum(1 for q in working_set if haversine_m(p["lat"], p["lng"], q["lat"], q["lng"]) <= 2000)
        )
        refill = leftover[: len(deficient_ids)]
        refill_ids = {id(p) for p in refill}
        working_set.extend(refill)
        leftover = [p for p in leftover if id(p) not in refill_ids]

    sampled = working_set

    if len(sampled) < PHARMACY_MIN:
        sys.exit(
            f"[오류] 샘플링된 약국이 {len(sampled)}건으로 최소 {PHARMACY_MIN}건에 못 미칩니다. "
            "TARGET_SIDO 범위를 넓히세요."
        )

    regions = {}
    for p in sampled:
        code = f"{p['sido'][:2]}-{p['sigungu']}"
        p["region_code"] = code
        if code not in regions:
            regions[code] = {"code": code, "sido": p["sido"], "sigungu": p["sigungu"], "lats": [], "lngs": []}
        regions[code]["lats"].append(p["lat"])
        regions[code]["lngs"].append(p["lng"])

    region_rows = [
        {
            "code": r["code"],
            "sido": r["sido"],
            "sigungu": r["sigungu"],
            "center_lat": sum(r["lats"]) / len(r["lats"]),
            "center_lng": sum(r["lngs"]) / len(r["lngs"]),
        }
        for r in regions.values()
    ]
    return region_rows, sampled


def strip_parens(name: str) -> str:
    # 괄호가 중첩(예: "...(A(B))")될 수 있어 non-greedy로는 안쪽 짝만 지워 괄호가 남는다.
    # 이름에 괄호 그룹이 한 덩어리로 오는 경우가 대부분이라 greedy로 통째 제거한다.
    return re.sub(r"\(.*\)", "", name).strip()


def guess_category(name: str) -> str:
    core = strip_parens(name)
    for category, suffixes in CATEGORY_BY_FORM_SUFFIX:
        if any(core.endswith(suf) or re.search(rf"{suf}\d", core) for suf in suffixes):
            return category
    return "기타"


def guess_package_unit(name: str) -> str:
    m = PACKAGE_UNIT_RE.search(name)
    return f"{m.group(1)}{m.group(2)}" if m else "함량미상"


def guess_display_name(name: str) -> str:
    core = strip_parens(name)
    core = PACKAGE_UNIT_RE.sub("", core).strip()  # 함량 표기 제거
    return core or name


def build_drugs(service_key: str) -> list[dict]:
    print("의약품 데이터 수집 중...")
    raw_items = fetch_all_items(DRUG_ENDPOINT, service_key, {}, DATA_DIR / "drug_raw.xml", max_items=300)

    random.seed(RANDOM_SEED)  # base_price 재현성 (약국 샘플링과 별개로 고정)
    seen_names: set[str] = set()
    drugs = []
    for it in raw_items:
        name = field(it, DRUG_FIELD_SYNONYMS["item_name"])
        item_seq = field(it, DRUG_FIELD_SYNONYMS["item_seq"])
        if not name or not item_seq or name in seen_names:
            continue
        seen_names.add(name)
        package_unit = guess_package_unit(name)
        category = guess_category(name)
        low, high = CATEGORY_PRICE_RANGE[category]
        base_price = round(random.uniform(low, high), -2)  # 100원 단위로 보기 좋게
        drugs.append(
            {
                "item_seq": item_seq,
                "name": name,
                "display_name": guess_display_name(name),
                "maker": field(it, DRUG_FIELD_SYNONYMS["entp_name"]) or "미상",
                "category": category,
                "package_unit": package_unit,
                "base_price": int(base_price),
                "image_url": field(it, DRUG_FIELD_SYNONYMS["image"]),
            }
        )
        if len(drugs) >= DRUG_MAX:
            break

    if len(drugs) < DRUG_MIN:
        sys.exit(
            f"[오류] 수집된 의약품이 {len(drugs)}종으로 최소 {DRUG_MIN}종에 못 미칩니다. "
            "typeName 필터를 완화하거나 API 파라미터를 조정하세요."
        )
    return drugs


def sql_escape(value: str | None) -> str:
    if value is None:
        return "NULL"
    return "'" + value.replace("'", "''") + "'"


def render_sql(regions: list[dict], pharmacies: list[dict], drugs: list[dict]) -> str:
    lines = ["-- V2__seed_master.sql (자동 생성: tools/seed-generator/build_master_seed.py)", ""]

    lines.append("-- region")
    for r in regions:
        lines.append(
            "INSERT INTO region (code, sido, sigungu, center_lat, center_lng) VALUES "
            f"({sql_escape(r['code'])}, {sql_escape(r['sido'])}, {sql_escape(r['sigungu'])}, "
            f"{r['center_lat']}, {r['center_lng']}) "
            "ON CONFLICT (code) DO NOTHING;"
        )
    lines.append("")

    lines.append("-- pharmacy")
    for p in pharmacies:
        lines.append(
            "INSERT INTO pharmacy (hira_code, name, address_road, address_jibun, region_code, lat, lng, phone, is_active) "
            "VALUES ("
            f"{sql_escape(p['hira_code'])}, {sql_escape(p['name'])}, {sql_escape(p['addr_road'])}, "
            f"{sql_escape(p['addr_jibun'])}, {sql_escape(p['region_code'])}, {p['lat']}, {p['lng']}, "
            f"{sql_escape(p['tel'])}, true) "
            "ON CONFLICT (hira_code) DO NOTHING;"
        )
    lines.append("")

    lines.append("-- drug (base_price는 category별 시중 가격대 참고 자동 산정. 특정 제품 가격이 부정확하면 수기로 조정)")
    for d in drugs:
        lines.append(
            "INSERT INTO drug (item_seq, name, display_name, maker, category, package_unit, otc_flag, base_price, image_url) "
            "VALUES ("
            f"{sql_escape(d['item_seq'])}, {sql_escape(d['name'])}, {sql_escape(d['display_name'])}, "
            f"{sql_escape(d['maker'])}, {sql_escape(d['category'])}, {sql_escape(d['package_unit'])}, "
            f"true, {d['base_price']}, {sql_escape(d['image_url'])}) "
            "ON CONFLICT (item_seq) DO NOTHING;"
        )
    lines.append("")

    lines.append("-- app_user (admin 1 + 더미 제보자 20)")
    admin_hash = bcrypt.hashpw(b"Admin1234!", bcrypt.gensalt()).decode()
    lines.append(
        "INSERT INTO app_user (email, password_hash, nickname, role, status, report_count) VALUES "
        f"('admin@example.com', {sql_escape(admin_hash)}, '관리자', 'ADMIN', 'ACTIVE', 0) "
        "ON CONFLICT (email) DO NOTHING;"
    )
    dummy_hash = bcrypt.hashpw(b"User1234!", bcrypt.gensalt()).decode()
    for i in range(1, 21):
        lines.append(
            "INSERT INTO app_user (email, password_hash, nickname, role, status, report_count) VALUES "
            f"('user{i:02d}@example.com', {sql_escape(dummy_hash)}, '제보자{i:02d}', 'USER', 'ACTIVE', 0) "
            "ON CONFLICT (email) DO NOTHING;"
        )

    return "\n".join(lines) + "\n"


def main() -> None:
    pharmacy_key = env_value("PHARMACY_API_KEY")
    drug_key = env_value("DRUG_API_KEY")

    regions, pharmacies = build_regions_and_pharmacies(pharmacy_key)
    drugs = build_drugs(drug_key)

    OUT_SQL.parent.mkdir(parents=True, exist_ok=True)
    OUT_SQL.write_text(render_sql(regions, pharmacies, drugs), encoding="utf-8")

    print(f"완료: region {len(regions)}건, pharmacy {len(pharmacies)}건, drug {len(drugs)}종")
    print(f"출력: {OUT_SQL}")


if __name__ == "__main__":
    main()
