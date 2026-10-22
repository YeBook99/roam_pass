package com.roampass.mockpg.service;

import com.roampass.mockpg.dto.MockPgApproveRequest;
import com.roampass.mockpg.dto.MockPgCancelRequest;
import com.roampass.mockpg.dto.MockPgCancelResponse;
import com.roampass.mockpg.dto.MockPgPaymentResponse;
import com.roampass.mockpg.dto.MockPgSettings;
import com.roampass.mockpg.dto.MockPgSettingsPatch;

// 모의 PG의 승인·취소·조회와 시연 설정. 시연용이라 mock.enabled=true일 때만 켜진다
public interface MockPgService {

    MockPgSettings getSettings();

    MockPgSettings updateSettings(MockPgSettingsPatch patch);

    MockPgSettings resetSettings();

    // 승인을 저장하고 웹훅을 예약한 뒤, 설정만큼 늦게 응답한다
    // 응답 지연이 우리 타임아웃(3초)보다 길면 "PG는 승인했는데 우리는 응답을 못 받은" 상황이 된다
    // 기다리는 동안 DB 커넥션을 잡지 않도록 저장만 TransactionTemplate으로 감싼다
    MockPgPaymentResponse approve(MockPgApproveRequest request);

    MockPgPaymentResponse getPayment(String merchantRequestId);

    // 전액·부분 취소. 같은 requestKey는 한 번만 반영한다
    MockPgCancelResponse cancel(MockPgCancelRequest request);
}
