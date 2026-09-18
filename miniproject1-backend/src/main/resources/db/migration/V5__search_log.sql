-- 약품별 검색 순위 집계용 로그. drugId는 SearchService.search()에서 항상 필수라 NOT NULL.
CREATE TABLE search_log (
    id         BIGSERIAL PRIMARY KEY,
    drug_id    BIGINT NOT NULL REFERENCES drug (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_search_log_drug_created ON search_log (drug_id, created_at DESC);

-- 약국별 제보 건수 Top N 랭킹용 (관리자 통계)
CREATE INDEX idx_report_pharmacy_created ON price_report (pharmacy_id, created_at DESC) WHERE status = 'ACTIVE';
