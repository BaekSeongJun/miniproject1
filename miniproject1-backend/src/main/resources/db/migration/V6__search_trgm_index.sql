-- 검색 쿼리가 display_name OR name, name OR address_road 로 묶여 있어
-- 한쪽 컬럼에만 trgm 인덱스가 있으면 OR 전체가 인덱스를 못 타고 전체 스캔을 한다.
-- 나머지 컬럼에도 trgm 인덱스를 두어 BitmapOr로 두 인덱스를 함께 쓰게 한다.
-- 주의: 한글 trigram은 DB의 LC_CTYPE이 UTF-8 계열(ko-KR 등)일 때만 만들어진다. C 로케일에서는 효과가 없다.
CREATE INDEX IF NOT EXISTS idx_drug_name_trgm ON drug USING gin (name gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_pharmacy_address_road_trgm ON pharmacy USING gin (address_road gin_trgm_ops);
