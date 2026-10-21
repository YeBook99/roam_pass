-- 발급 결과(이심 프로필). 설치 추적 컬럼은 설치 추적 단계에서 추가한다
CREATE TABLE sim_profile (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id          BIGINT       NOT NULL UNIQUE REFERENCES sim_order (id),   -- 한 주문에 프로필은 하나 (중복 콜백 방지)
    iccid             VARCHAR(22)  NOT NULL UNIQUE,                             -- 같은 SIM이 두 주문에 붙지 않게
    activation_code   VARCHAR(255),
    supplier_order_no VARCHAR(64)  UNIQUE,
    install_status    VARCHAR(20)  NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL,
    updated_at        TIMESTAMPTZ  NOT NULL
);
