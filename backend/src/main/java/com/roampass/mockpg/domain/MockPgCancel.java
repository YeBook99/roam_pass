package com.roampass.mockpg.domain;

import com.roampass.global.entity.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(schema = "mock", name = "pg_cancel")
public class MockPgCancel extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String requestKey;
    private String pgTid;
    private int amount;

    public static MockPgCancel create(String requestKey, String pgTid, int amount) {
        MockPgCancel cancel = new MockPgCancel();
        cancel.requestKey = requestKey;
        cancel.pgTid = pgTid;
        cancel.amount = amount;
        return cancel;
    }
}
