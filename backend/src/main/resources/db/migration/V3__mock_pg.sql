-- 모의 PG의 데이터.
CREATE SCHEMA mock;

CREATE TABLE mock.pg_payment (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    merchant_request_id VARCHAR(64) NOT NULL UNIQUE,    -- 가맹점(우리)이 보낸 요청 ID. 같은 요청은 한 번만 승인한다
    pg_tid              VARCHAR(64) NOT NULL UNIQUE,
    order_no            VARCHAR(20) NOT NULL,
    amount              INT         NOT NULL,
    canceled_amount     INT         NOT NULL DEFAULT 0,
    status              VARCHAR(20) NOT NULL,           -- APPROVED | FAILED | PARTIAL_CANCELED | CANCELED
    created_at          TIMESTAMPTZ NOT NULL
);

CREATE TABLE mock.pg_cancel (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    request_key VARCHAR(100) NOT NULL UNIQUE,           -- 같은 취소 요청은 한 번만 반영한다
    pg_tid      VARCHAR(64)  NOT NULL,
    amount      INT          NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL
);