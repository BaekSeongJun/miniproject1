#!/usr/bin/env python3
"""
가격 제보 목데이터 생성 (T-08)

DB에 접속해 V2 시드로 이미 적재된 pharmacy/drug/app_user의 실제 id를 읽어온다.
BIGSERIAL + ON CONFLICT DO NOTHING 조합이라 SQL 파일의 INSERT 순서로 id를 역산할 수 없다 (선행 태스크 T-07).

규칙: DATABASE.md §6.2

출력:
  - miniproject1-backend/src/main/resources/db/migration/V3__seed_prices.sql
"""
from __future__ import annotations

import random
import sys
from datetime import date, timedelta
from itertools import product
from pathlib import Path
from statistics import median

import psycopg2

ROOT = Path(__file__).resolve().parents[2]
OUT_SQL = ROOT / "miniproject1-backend/src/main/resources/db/migration/V3__seed_prices.sql"

RANDOM_SEED = 20260915
COVERAGE = 0.60
OUTLIER_RATE = 0.02
TODAY = date(2026, 9, 16)


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
    sys.exit(f"[오류] .env 에 {key} 값이 비어 있습니다.")


def fetch_master_data() -> tuple[list[tuple[int, int]], list[int]]:
    """(pharmacy_id, drug_id, base_price) 조합용 원본과 더미 제보자 id 목록을 DB에서 읽는다."""
    conn = psycopg2.connect(
        host=env_value("POSTGRES_HOST"),
        port=env_value("POSTGRES_PORT"),
        dbname=env_value("POSTGRES_DB"),
        user=env_value("POSTGRES_USER"),
        password=env_value("POSTGRES_PASSWORD"),
    )
    try:
        cur = conn.cursor()
        cur.execute("SELECT id FROM pharmacy ORDER BY id")
        pharmacy_ids = [r[0] for r in cur.fetchall()]
        cur.execute("SELECT id, base_price FROM drug ORDER BY id")
        drugs = cur.fetchall()
        cur.execute("SELECT id FROM app_user WHERE role = 'USER' ORDER BY id")
        user_ids = [r[0] for r in cur.fetchall()]
    finally:
        conn.close()

    if not pharmacy_ids or not drugs:
        sys.exit("[오류] pharmacy 또는 drug 데이터가 비어 있습니다. V2__seed_master.sql이 먼저 적용됐는지 확인하세요.")
    if not user_ids:
        sys.exit("[오류] 더미 제보자(app_user role=USER)가 없습니다.")
    return pharmacy_ids, drugs, user_ids


def build_reports(pharmacy_ids: list[int], drugs: list[tuple[int, int]], user_ids: list[int]) -> list[dict]:
    random.seed(RANDOM_SEED)  # 재실행해도 동일한 데이터가 나오도록 고정 (완료 판정 항목)

    factor = {pid: random.uniform(0.85, 1.25) for pid in pharmacy_ids}
    reports = []
    for pharmacy_id, (drug_id, base_price) in product(pharmacy_ids, drugs):
        if random.random() > COVERAGE:
            continue
        n = random.randint(1, 6)
        # uq_report_user_pair_day: 같은 유저가 같은 (약국,약품)에 하루 2건 이상 제보하면 유니크 인덱스에 걸린다.
        # 시드는 created_at이 전부 "오늘"이므로, 이 조합 안에서는 user_id를 중복 없이 배정한다.
        pair_user_ids = random.sample(user_ids, min(n, len(user_ids)))
        for i in range(n):
            price = round(base_price * factor[pharmacy_id] * random.gauss(1, 0.05), -1)
            age_days = int(120 * random.random() ** 1.6)  # 지수 쏠림: 최근 제보가 더 많다
            is_outlier = random.random() < OUTLIER_RATE
            if is_outlier:
                price = round(base_price * random.choice([0.3, 3.0]), -1)
            reports.append(
                {
                    "pharmacy_id": pharmacy_id,
                    "drug_id": drug_id,
                    "user_id": pair_user_ids[i] if i < len(pair_user_ids) and random.random() < 0.5 else None,
                    "price": max(100, min(200000, int(price))),  # price_report CHECK 제약 범위
                    "purchased_at": TODAY - timedelta(days=age_days),
                    "flagged": is_outlier and random.random() < 0.3,  # 이상치 대부분은 미표시로 남겨 IQR 필터(T-10) 데모용으로 쓴다
                }
            )
    return reports


def recalc_stats(reports: list[dict]) -> list[dict]:
    """DATABASE.md §5.2 IQR 재계산 쿼리를 그대로 파이썬으로 재현한다."""
    by_pair: dict[tuple[int, int], list[dict]] = {}
    for r in reports:
        if r["flagged"]:
            continue
        by_pair.setdefault((r["pharmacy_id"], r["drug_id"]), []).append(r)

    stats = []
    for (pharmacy_id, drug_id), rs in by_pair.items():
        prices = sorted(r["price"] for r in rs)
        n = len(prices)
        if n >= 4:
            q1 = percentile(prices, 0.25)
            q3 = percentile(prices, 0.75)
            iqr = q3 - q1
            lo, hi = q1 - 1.5 * iqr, q3 + 1.5 * iqr
            trimmed = [p for p in prices if lo <= p <= hi]
        else:
            trimmed = prices

        stats.append(
            {
                "pharmacy_id": pharmacy_id,
                "drug_id": drug_id,
                "rep_price": round(median(trimmed)),
                "min_price": min(trimmed),
                "max_price": max(trimmed),
                "avg_price": round(sum(trimmed) / len(trimmed)),
                "report_count": len(trimmed),
                "last_reported_at": max(r["purchased_at"] for r in rs),
                "window_days": 90,
            }
        )
    return stats


def percentile(sorted_values: list[int], p: float) -> float:
    """percentile_cont와 동일한 선형 보간 방식."""
    if len(sorted_values) == 1:
        return sorted_values[0]
    idx = p * (len(sorted_values) - 1)
    lo, hi = int(idx), min(int(idx) + 1, len(sorted_values) - 1)
    frac = idx - lo
    return sorted_values[lo] + (sorted_values[hi] - sorted_values[lo]) * frac


def sql_escape_date(d: date) -> str:
    return f"'{d.isoformat()}'"


def render_sql(reports: list[dict], stats: list[dict]) -> str:
    lines = ["-- V3__seed_prices.sql (자동 생성: tools/seed-generator/build_price_seed.py)", ""]

    lines.append("-- price_report")
    for chunk_start in range(0, len(reports), 500):
        chunk = reports[chunk_start : chunk_start + 500]
        values = ", ".join(
            f"({r['pharmacy_id']}, {r['drug_id']}, {r['user_id'] if r['user_id'] else 'NULL'}, "
            f"{r['price']}, {sql_escape_date(r['purchased_at'])}, 'SEED', 'ACTIVE', "
            f"{'true' if r['flagged'] else 'false'})"
            for r in chunk
        )
        lines.append(
            "INSERT INTO price_report (pharmacy_id, drug_id, user_id, price, purchased_at, source, status, flagged) "
            f"VALUES {values};"
        )
    lines.append("")

    lines.append("-- pharmacy_drug_price_stat (초기 계산)")
    for chunk_start in range(0, len(stats), 500):
        chunk = stats[chunk_start : chunk_start + 500]
        values = ", ".join(
            f"({s['pharmacy_id']}, {s['drug_id']}, {s['rep_price']}, {s['min_price']}, {s['max_price']}, "
            f"{s['avg_price']}, {s['report_count']}, {sql_escape_date(s['last_reported_at'])}, {s['window_days']})"
            for s in chunk
        )
        lines.append(
            "INSERT INTO pharmacy_drug_price_stat "
            "(pharmacy_id, drug_id, rep_price, min_price, max_price, avg_price, report_count, last_reported_at, window_days) "
            f"VALUES {values} "
            "ON CONFLICT (pharmacy_id, drug_id) DO UPDATE SET "
            "rep_price = EXCLUDED.rep_price, min_price = EXCLUDED.min_price, max_price = EXCLUDED.max_price, "
            "avg_price = EXCLUDED.avg_price, report_count = EXCLUDED.report_count, "
            "last_reported_at = EXCLUDED.last_reported_at, window_days = EXCLUDED.window_days, "
            "calculated_at = now();"
        )

    return "\n".join(lines) + "\n"


def main() -> None:
    pharmacy_ids, drugs, user_ids = fetch_master_data()
    reports = build_reports(pharmacy_ids, drugs, user_ids)
    stats = recalc_stats(reports)

    OUT_SQL.parent.mkdir(parents=True, exist_ok=True)
    OUT_SQL.write_text(render_sql(reports, stats), encoding="utf-8")

    print(f"완료: price_report {len(reports)}건, pharmacy_drug_price_stat {len(stats)}건")
    print(f"출력: {OUT_SQL}")


if __name__ == "__main__":
    main()
