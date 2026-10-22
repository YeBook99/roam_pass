package com.roampass.history.service;

import com.roampass.history.domain.Actor;
import com.roampass.history.domain.Signal;
import com.roampass.history.domain.TargetType;

// 여러 기능이 함께 쓰는 상태 이력의 주인 (ARCHITECTURE.md §3-3)
// 각 기능 Service가 조건부 UPDATE에 성공했을 때만 같은 트랜잭션 안에서 부른다 (CLAUDE.md §3-2)
public interface StatusHistoryService {

    // MANDATORY: 상태 변경과 이력이 반드시 한 트랜잭션이어야 한다. 트랜잭션 밖에서 부르면 예외
    void record(TargetType type, long targetId, String fromStatus, String toStatus,
            Actor actor, Signal signal, String reason);
}
