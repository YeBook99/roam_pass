package com.roampass.issue.controller;

import com.roampass.issue.service.IssueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

// 공급사 발급 콜백. 서명이 틀리면 401, 본문을 읽지 못하면 400 (GlobalExceptionHandler가 응답을 만든다)
@RestController
@RequiredArgsConstructor
public class IssueCallbackController {

    private final IssueService issueService;

    // 본문을 byte[]로 받는다. 서명은 받은 원문 그대로 검증해야 하기 때문이다
    @PostMapping("/api/webhooks/supplier")
    public void receive(@RequestBody byte[] body,
                        @RequestHeader(value = "X-Signature", required = false) String signature) {
        issueService.receiveCallback(body, signature);
    }
}
