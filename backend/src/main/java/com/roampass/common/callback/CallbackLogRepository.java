package com.roampass.common.callback;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface CallbackLogRepository extends JpaRepository<CallbackLog, Long> {

    // 중복 체크 겸 기록. 같은 event_key가 이미 있으면 아무것도 하지 않고 0을 돌려준다.
    // 같은 키가 동시에 들어오면 PostgreSQL이 뒤의 것을 앞 트랜잭션이 끝날 때까지 기다리게 한다
    @Modifying
    @Query(value = """
            INSERT INTO callback_log (source, event_key, payload, signature_ok, received_at)
            VALUES (:source, :eventKey, :payload, true, :receivedAt)
            ON CONFLICT (event_key) DO NOTHING
            """, nativeQuery = true)
    int insertIgnore(@Param("source") String source, @Param("eventKey") String eventKey,
                     @Param("payload") String payload, @Param("receivedAt") Instant receivedAt);

    @Modifying
    @Query(value = "UPDATE callback_log SET result = :result WHERE event_key = :eventKey", nativeQuery = true)
    void markResult(@Param("eventKey") String eventKey, @Param("result") String result);
}
