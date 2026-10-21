-- 발급 요청 기록. 공급사가 접수하면 supplierOrderNo를 저장한다 (docs/06 §3)
ALTER TABLE sim_order
    ADD COLUMN supplier_order_no  VARCHAR(64) UNIQUE,   -- 공급사 주문 하나가 여러 주문에 매칭되지 않게
    ADD COLUMN issue_requested_at TIMESTAMPTZ;           -- 발급 요청(PAID→ISSUING) 시각
