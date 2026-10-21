package com.roampass.issue.service;

import com.roampass.history.domain.Signal;
import com.roampass.issue.domain.InstallStatus;
import com.roampass.issue.domain.SimProfile;
import com.roampass.issue.repository.SimProfileRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 설치 상태 주기 확인. 공급사 콜백이 빠져도 주기적인 조회가 결국 맞춰 준다 (docs/07 §1)
// 지금은 개통 전인 SIM을 모두 1분마다 조회한다. SIM마다 주기를 다르게 하는 것(§5)은 보완 단계
@Slf4j
@Component
@RequiredArgsConstructor
public class InstallPollingScheduler {

    private static final List<InstallStatus> WAITING = List.of(InstallStatus.ISSUED, InstallStatus.QR_VIEWED, InstallStatus.INSTALLED);

    private final SimProfileRepository profileRepository;
    private final InstallTrackingService installTrackingService;

    @Scheduled(fixedDelayString = "${app.install.poll-delay-ms}")
    public void poll() {
        for (SimProfile profile : profileRepository.findByInstallStatusIn(WAITING)) {
            try {
                installTrackingService.refresh(profile.getId(), Signal.POLLING);
            } catch (RuntimeException e) {
                log.error("설치 상태 확인 실패, 다음 주기에 다시 시도 profileId={}", profile.getId(), e);
            }
        }
        installTrackingService.expireDue();
    }
}
