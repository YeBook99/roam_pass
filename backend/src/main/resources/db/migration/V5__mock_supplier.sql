-- 모의 공급사의 데이터.
CREATE TABLE mock.supplier_order (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_no          VARCHAR(20) NOT NULL UNIQUE,     -- 가맹점(우리) 주문번호. 같은 주문의 재요청은 같은 결과를 돌려준다
    supplier_order_no VARCHAR(64) NOT NULL UNIQUE,
    iccid             VARCHAR(22) NOT NULL UNIQUE,
    activation_code   VARCHAR(255) NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL,
    updated_at        TIMESTAMPTZ NOT NULL
);
