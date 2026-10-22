package com.roampass.history.service.impl;

import com.roampass.history.domain.Actor;
import com.roampass.history.domain.Signal;
import com.roampass.history.domain.StatusHistory;
import com.roampass.history.domain.TargetType;
import com.roampass.history.repository.StatusHistoryRepository;
import com.roampass.history.service.StatusHistoryService;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// StatusHistoryService 구현. 설명은 인터페이스 StatusHistoryService에 있다
@Service
@RequiredArgsConstructor
public class StatusHistoryServiceImpl implements StatusHistoryService {

    private final StatusHistoryRepository historyRepository;
    private final Clock clock;

    // MANDATORY: 상태 변경과 이력이 반드시 한 트랜잭션이어야 한다. 트랜잭션 밖에서 부르면 예외
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(TargetType type, long targetId, String fromStatus, String toStatus,
                       Actor actor, Signal signal, String reason) {
        Instant now = Instant.now(clock);
        historyRepository.save(StatusHistory.of(type, targetId, fromStatus, toStatus, actor, signal, reason, now));
    }
}