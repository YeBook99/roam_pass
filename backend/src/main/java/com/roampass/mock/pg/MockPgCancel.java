package com.roampass.mock.pg;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(schema = "mock", name = "pg_cancel")
public class MockPgCancel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String requestKey;
    private String pgTid;
    private int amount;
    private Instant createdAt;

    protected MockPgCancel() {
    }

    MockPgCancel(String requestKey, String pgTid, int amount, Instant createdAt) {
        this.requestKey = requestKey;
        this.pgTid = pgTid;
        this.amount = amount;
        this.createdAt = createdAt;
    }

    String getPgTid() {
        return pgTid;
    }
}