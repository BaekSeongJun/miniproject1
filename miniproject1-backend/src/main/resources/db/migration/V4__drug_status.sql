-- 사용자가 미등록 약품을 직접 등록할 수 있게 되면서, 관리자 승인 전까지는
-- 검색·추천에 노출되지 않도록 승인 상태를 추가한다.
ALTER TABLE drug ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'APPROVED';

UPDATE drug SET status = 'APPROVED';

CREATE INDEX idx_drug_status ON drug (status);
