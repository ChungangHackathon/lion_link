-- 멋사 파트는 1개만 선택한다 (QA 43). 기획+디자인 조합은 PLANNING_DESIGN 하나로 합치고,
-- 나머지 조합은 첫 번째 파트(part_1)만 남긴다. 그 뒤 두 번째 파트 컬럼을 지운다.
UPDATE profiles SET lion_part_1 = 'PLANNING_DESIGN'
WHERE (lion_part_1 = 'PLANNING' AND lion_part_2 = 'DESIGN')
   OR (lion_part_1 = 'DESIGN' AND lion_part_2 = 'PLANNING');

UPDATE lion_activities SET part_1 = 'PLANNING_DESIGN'
WHERE (part_1 = 'PLANNING' AND part_2 = 'DESIGN')
   OR (part_1 = 'DESIGN' AND part_2 = 'PLANNING');

ALTER TABLE profiles DROP COLUMN lion_part_2;
ALTER TABLE lion_activities DROP COLUMN part_2;
