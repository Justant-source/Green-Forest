-- 몰래뽑기 이벤트 플래그: 관리자가 조작한 뽑기를 구분하기 위한 컬럼
-- dev는 ddl-auto=update라 자동 추가되지만, prod 배포 시 이 SQL을 먼저 실행해야 함
ALTER TABLE gacha_draws ADD COLUMN secret_event BIT(1) NOT NULL DEFAULT b'0';
