package com.roampass.issue.controller;

import com.roampass.issue.dto.ProfileResponse;
import com.roampass.issue.service.InstallTrackingService;
import com.roampass.issue.service.IssueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

// 내 SIM. 남의 주문이거나 아직 발급 전이면 404
@RestController
@RequiredArgsConstructor
public class ProfileController {

    private final IssueService issueService;
    private final InstallTrackingService installTrackingService;

    @GetMapping("/api/my/orders/{orderNo}/profile")
    public ProfileResponse getMyProfile(@RequestHeader("X-User-Id") String userId, @PathVariable String orderNo) {
        return issueService.getMyProfile(orderNo, userId);
    }

    // QR 화면을 열 때 부른다 (ISSUED → QR_VIEWED)
    @PostMapping("/api/my/orders/{orderNo}/profile/qr-view")
    public ProfileResponse viewQr(@RequestHeader("X-User-Id") String userId, @PathVariable String orderNo) {
        return installTrackingService.viewQr(orderNo, userId);
    }

    // "다시 확인" 버튼: 공급사에 바로 물어보고 설치·개통 상태를 반영한다
    @PostMapping("/api/my/orders/{orderNo}/profile/refresh")
    public ProfileResponse refresh(@RequestHeader("X-User-Id") String userId, @PathVariable String orderNo) {
        return installTrackingService.refreshMine(orderNo, userId);
    }
}
