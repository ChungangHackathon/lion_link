-- 재직·창업 회사와 직무를 경력(careers)과 분리해 프로필에 저장한다 (이슈 #171).
ALTER TABLE profiles
    ADD COLUMN company VARCHAR(30) NULL,
    ADD COLUMN job_title VARCHAR(30) NULL;

-- 기존 회원 백필: 재직·창업 회원만, 재직 중 경력 중 시작 년월이 가장 큰 행(같으면 id가 큰 행) 값으로 채운다.
-- 같은 테이블 서브쿼리 제약을 피하려고 UPDATE ... JOIN을 쓴다. start_ym이 NULL인 행은 가장 오래된 것으로 본다(1000-01-01).
UPDATE profiles p
JOIN careers c ON c.user_id = p.user_id AND c.is_current = 1
LEFT JOIN careers newer ON newer.user_id = c.user_id AND newer.is_current = 1
    AND (COALESCE(newer.start_ym, '1000-01-01') > COALESCE(c.start_ym, '1000-01-01')
         OR (COALESCE(newer.start_ym, '1000-01-01') = COALESCE(c.start_ym, '1000-01-01') AND newer.id > c.id))
SET p.company = c.company,
    p.job_title = c.title
WHERE p.status IN ('EMPLOYED', 'FOUNDER')
  AND newer.id IS NULL;
