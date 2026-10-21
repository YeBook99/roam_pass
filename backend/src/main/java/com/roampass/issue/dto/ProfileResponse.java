package com.roampass.issue.dto;

import com.roampass.issue.domain.InstallStatus;
import com.roampass.issue.domain.SimProfile;

// 내 SIM 화면에 보여 줄 발급 결과. 활성화 코드는 QR로 바꿔 보여 준다
public record ProfileResponse(String orderNo, String iccid, String activationCode, InstallStatus installStatus) {

    public static ProfileResponse from(String orderNo, SimProfile profile) {
        return new ProfileResponse(orderNo, profile.getIccid(), profile.getActivationCode(), profile.getInstallStatus());
    }
}
