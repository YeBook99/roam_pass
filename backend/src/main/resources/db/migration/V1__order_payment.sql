-- 결제까지 필요한 테이블. 발급·환불·어드민 테이블은 그 기능을 만들 때 다음 버전으로 추가한다.
-- 시각은 모두 timestamptz(UTC), 금액은 원 단위 정수다. (docs/03)

CREATE TABLE sim_plan (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    plan_code VARCHAR(40) NOT NULL UNIQUE,
    country   VARCHAR(10) NOT NULL,
    days      INT         NOT NULL CHECK (days > 0),
    data_mb   INT         NOT NULL CHECK (data_mb > 0),
    sim_type  VARCHAR(10) NOT NULL CHECK (sim_type IN ('ESIM', 'USIM')),
    price     INT         NOT NULL CHECK (price > 0)
);

CREATE TABLE sim_order (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_no          VARCHAR(20) NOT NULL UNIQUE,      -- 외부(PG·공급사)와 주고받는 주문번호
    user_id           VARCHAR(64) NOT NULL,
    plan_id           BIGINT      NOT NULL REFERENCES sim_plan (id),
    sim_type          VARCHAR(10) NOT NULL,
    amount            INT         NOT NULL,             -- 주문 시점 가격을 복사해 둔다
    travel_start_date DATE        NOT NULL,             -- KST 기준 날짜
    status            VARCHAR(20) NOT NULL,
    expires_at        TIMESTAMPTZ NOT NULL,             -- 결제 대기 만료
    paid_at           TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_sim_order_status_expires ON sim_order (status, expires_at);
CREATE INDEX ix_sim_order_user_created ON sim_order (user_id, created_at DESC);

CREATE TABLE payment (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id        BIGINT       NOT NULL REFERENCES sim_order (id),
    idempotency_key VARCHAR(100) NOT NULL UNIQUE,       -- 버튼 연타·타임아웃 후 재시도를 한 건으로
    pg_tid          VARCHAR(64) UNIQUE,                 -- 같은 PG 거래가 두 결제에 붙지 않게
    amount          INT          NOT NULL,
    canceled_amount INT          NOT NULL DEFAULT 0,
    status          VARCHAR(20)  NOT NULL,
    requested_at    TIMESTAMPTZ  NOT NULL,
    approved_at     TIMESTAMPTZ
);
-- 한 주문에 승인된 결제는 하나뿐 (실패한 시도는 여러 번 있어도 된다)
CREATE UNIQUE INDEX ux_payment_order_approved ON payment (order_id)
    WHERE status IN ('APPROVED', 'PARTIAL_CANCELED', 'CANCELED');
-- 한 주문에 결과를 모르는 결제 요청은 동시에 하나뿐 (다른 탭에서 동시 결제)
CREATE UNIQUE INDEX ux_payment_order_ready ON payment (order_id) WHERE status = 'READY';
CREATE INDEX ix_payment_status_requested ON payment (status, requested_at);

CREATE TABLE callback_log (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    source       VARCHAR(10)  NOT NULL,                 -- PG | SUPPLIER
    event_key    VARCHAR(200) UNIQUE,                   -- 같은 통지를 두 번 처리하지 않게. 서명이 틀리면 NULL
    payload      TEXT         NOT NULL,
    signature_ok BOOLEAN      NOT NULL,
    result       VARCHAR(30),
    received_at  TIMESTAMPTZ  NOT NULL
);

CREATE TABLE status_history (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    target_type VARCHAR(10) NOT NULL,                   -- ORDER | PAYMENT | PROFILE
    target_id   BIGINT      NOT NULL,
    from_status VARCHAR(20),                            -- 생성 이력은 NULL
    to_status   VARCHAR(20) NOT NULL,
    actor       VARCHAR(80) NOT NULL,                   -- USER | SYSTEM | PG | SUPPLIER | ADMIN:{id}
    signal      VARCHAR(20) NOT NULL,                   -- API | WEBHOOK | CALLBACK | POLLING | SCHEDULER | ADMIN
    reason      VARCHAR(500),
    occurred_at TIMESTAMPTZ NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_status_history_target ON status_history (target_type, target_id, recorded_at);
