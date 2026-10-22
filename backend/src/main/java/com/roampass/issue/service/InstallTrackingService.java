package com.roampass.issue.service;

import com.roampass.history.domain.Signal;
import com.roampass.issue.dto.ProfileResponse;

// 설치 추적. 우리는 사용자 폰을 볼 수 없어서, 공급사에 물어본 결과로 설치·개통을 판단한다 (docs/07)
// QR 열람은 우리가 직접 아는 유일한 신호지만 "봤다"일 뿐 설치 증거가 아니다
public interface InstallTrackingService {

    // 사용자가 QR을 열었다: ISSUED → QR_VIEWED. 이미 그 뒤 상태면 아무것도 바꾸지 않는다
    ProfileResponse viewQr(String orderNo, String userId);

    // 사용자가 "다시 확인"을 눌렀다: 공급사에 바로 물어보고 반영한다
    ProfileResponse refreshMine(String orderNo, String userId);

    // 공급사에 물어보고(트랜잭션 밖) 결과를 반영한다(트랜잭션 안). 사용자 확인과 스케줄러가 같이 쓴다
    void refresh(Long profileId, Signal signal);

    // 개통 후 사용 기간이 지난 SIM: ACTIVE → EXPIRED (만료 시각은 우리가 계산해 둔 expires_at)
    void expireDue();
}
