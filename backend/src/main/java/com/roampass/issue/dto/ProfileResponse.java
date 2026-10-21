package com.roampass.issue.dto;

import com.roampass.issue.domain.InstallStatus;
import com.roampass.issue.domain.SimProfile;
import java.time.Instant;

// 내 SIM 화면에 보여 줄 발급·설치 정보. 활성화 코드는 QR로 바꿔 보여 준다
// 시각은 UTC(Instant)로 내보내고, KST 표시는 화면에서 한다 (ARCHITECTURE.md §8-6)
public record ProfileResponse(String orderNo, String iccid, String activationCode, InstallStatus installStatus,
                              Instant qrViewedAt, Instant installedAt, Instant activatedAt, Instant expiresAt) {

    public static ProfileResponse from(String orderNo, SimProfile profile) {
        return new ProfileResponse(orderNo, profile.getIccid(), profile.getActivationCode(), profile.getInstallStatus(),
                profile.getQrViewedAt(), profile.getInstalledAt(), profile.getActivatedAt(), profile.getExpiresAt());
    }
}
