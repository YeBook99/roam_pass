package com.roampass.issue.controller;

import com.roampass.issue.dto.ProfileResponse;
import com.roampass.issue.service.IssueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ProfileController {

    private final IssueService issueService;

    // 내 주문의 발급 결과(ICCID·활성화 코드). 남의 주문이거나 아직 발급 전이면 404
    @GetMapping("/api/my/orders/{orderNo}/profile")
    public ProfileResponse getMyProfile(@RequestHeader("X-User-Id") String userId, @PathVariable String orderNo) {
        return issueService.getMyProfile(orderNo, userId);
    }
}
