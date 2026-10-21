-- 설치 추적 (docs/07). 시각은 모두 UTC로 저장하고, KST는 화면에서만 바꾼다
ALTER TABLE sim_profile
    ADD COLUMN qr_viewed_at TIMESTAMPTZ,   -- 사용자가 우리 화면에서 QR을 처음 연 시각
    ADD COLUMN installed_at TIMESTAMPTZ,   -- 공급사 기준으로 기기에 설치된 것을 우리가 확인한 시각
    ADD COLUMN activated_at TIMESTAMPTZ,   -- 공급사가 알려 준 개통 시각
    ADD COLUMN expires_at   TIMESTAMPTZ;   -- activated_at + 일수 × 24시간
CREATE INDEX ix_sim_profile_install_status ON sim_profile (install_status);

-- 모의 공급사의 프로필 상태 (시연용으로 바꿀 수 있다)
ALTER TABLE mock.supplier_order
    ADD COLUMN state        VARCHAR(20) NOT NULL DEFAULT 'RELEASED',   -- RELEASED | INSTALLED | ENABLED
    ADD COLUMN activated_at TIMESTAMPTZ;
