package com.roampass.issue.service;

import com.roampass.global.error.BusinessException;
import com.roampass.history.domain.Actor;
import com.roampass.history.domain.Signal;
import com.roampass.history.domain.TargetType;
import com.roampass.history.service.StatusHistoryService;
import com.roampass.infra.supplier.SupplierClient;
import com.roampass.infra.supplier.SupplierProfileState;
import com.roampass.issue.domain.ExpiryCalculator;
import com.roampass.issue.domain.InstallStatus;
import com.roampass.issue.domain.SimProfile;
import com.roampass.issue.dto.ProfileResponse;
import com.roampass.issue.exception.IssueErrorCode;
import com.roampass.issue.repository.SimProfileRepository;
import com.roampass.order.service.OrderService;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

// 설치 추적. 우리는 사용자 폰을 볼 수 없어서, 공급사에 물어본 결과로 설치·개통을 판단한다 (docs/07)
// QR 열람은 우리가 직접 아는 유일한 신호지만 "봤다"일 뿐 설치 증거가 아니다
@Service
@RequiredArgsConstructor
public class InstallTrackingService {

    private final SimProfileRepository profileRepository;
    private final OrderService orderService;
    private final SupplierClient supplierClient;
    private final StatusHistoryService historyService;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    // 사용자가 QR을 열었다: ISSUED → QR_VIEWED. 이미 그 뒤 상태면 아무것도 바꾸지 않는다
    @Transactional
    public ProfileResponse viewQr(String orderNo, String userId) {
        SimProfile profile = getMyProfile(orderNo, userId);
        Instant now = Instant.now(clock);
        if (transit(profile, InstallStatus.QR_VIEWED, Actor.USER, Signal.API, "QR 열람", now)) {
            profileRepository.recordQrViewedAt(profile.getId(), now);
        }
        return ProfileResponse.from(orderNo, profileRepository.findById(profile.getId()).orElseThrow());
    }

    // 사용자가 "다시 확인"을 눌렀다: 공급사에 바로 물어보고 반영한다
    public ProfileResponse refreshMine(String orderNo, String userId) {
        SimProfile profile = transactionTemplate.execute(status -> getMyProfile(orderNo, userId));
        refresh(profile.getId(), Signal.API);
        return ProfileResponse.from(orderNo, profileRepository.findById(profile.getId()).orElseThrow());
    }

    // 공급사에 물어보고(트랜잭션 밖) 결과를 반영한다(트랜잭션 안). 사용자 확인과 스케줄러가 같이 쓴다
    public void refresh(Long profileId, Signal signal) {
        SimProfile profile = profileRepository.findById(profileId).orElseThrow();
        Optional<SupplierProfileState> state = supplierClient.getProfile(profile.getIccid());
        if (state.isEmpty()) {
            return;   // 공급사가 응답하지 않으면 다음에 다시 물어본다
        }
        transactionTemplate.executeWithoutResult(status -> apply(profileId, state.get(), signal));
    }

    // 개통 후 사용 기간이 지난 SIM: ACTIVE → EXPIRED (만료 시각은 우리가 계산해 둔 expires_at)
    @Transactional
    public void expireDue() {
        Instant now = Instant.now(clock);
        for (SimProfile profile : profileRepository.findByInstallStatusAndExpiresAtBefore(InstallStatus.ACTIVE, now)) {
            transit(profile, InstallStatus.EXPIRED, Actor.SYSTEM, Signal.SCHEDULER, "사용 기간 종료", now);
        }
    }

    // 공급사 상태 → 우리 설치 상태. INSTALLED → INSTALLED, ENABLED → ACTIVE(+ 만료일 계산)
    // RELEASED(아직 안 내려받음)는 바꿀 것이 없다
    private void apply(Long profileId, SupplierProfileState state, Signal signal) {
        SimProfile profile = profileRepository.findById(profileId).orElseThrow();
        Instant now = Instant.now(clock);
        if ("INSTALLED".equals(state.state())) {
            if (transit(profile, InstallStatus.INSTALLED, Actor.SUPPLIER, signal, "공급사 기준 설치됨", now)) {
                profileRepository.recordInstalledAt(profileId, now);
            }
        } else if ("ENABLED".equals(state.state()) && state.activatedAt() != null) {
            if (transit(profile, InstallStatus.ACTIVE, Actor.SUPPLIER, signal, "공급사 기준 개통됨", now)) {
                if (profile.getInstalledAt() == null) {
                    profileRepository.recordInstalledAt(profileId, now);   // 설치 확인 없이 바로 개통으로 건너뛴 경우
                }
                int days = orderService.getPlanDays(profile.getOrderId());
                Instant expiresAt = ExpiryCalculator.expiresAt(state.activatedAt(), days);
                profileRepository.recordActivation(profileId, state.activatedAt(), expiresAt, now);
            }
        }
    }

    // 상태는 앞으로만 간다. 지금보다 앞선 상태일 때만 조건부 UPDATE → 바뀌었을 때만 이력 (CLAUDE.md §3-2)
    private boolean transit(SimProfile profile, InstallStatus to, Actor actor, Signal signal, String reason, Instant now) {
        InstallStatus from = profile.getInstallStatus();
        if (!from.canMoveTo(to)) {
            return false;   // 이미 그 상태이거나 더 나아간 상태 (늦게 온 옛날 정보)
        }
        if (profileRepository.updateInstallStatusIf(profile.getId(), from.name(), to.name(), now) == 0) {
            return false;   // 다른 요청이 먼저 바꿨다
        }
        historyService.record(TargetType.PROFILE, profile.getId(), from.name(), to.name(), actor, signal, reason);
        return true;
    }

    private SimProfile getMyProfile(String orderNo, String userId) {
        Long orderId = orderService.getMyOrderId(orderNo, userId);   // 남의 주문이면 404
        return profileRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException(IssueErrorCode.PROFILE_NOT_FOUND));
    }
}
