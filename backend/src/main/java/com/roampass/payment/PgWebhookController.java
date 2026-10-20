package com.roampass.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roampass.common.callback.CallbackLog;
import com.roampass.common.callback.CallbackLogRepository;
import com.roampass.common.callback.Hmac;
import com.roampass.common.status.Signal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;

// PG 승인 통지(웹훅). 서명 검증을 통과해야만 처리한다
@RestController
public class PgWebhookController {

    private final PaymentConfirmService confirmService;
    private final CallbackLogRepository callbackLogRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final String secret;

    public PgWebhookController(PaymentConfirmService confirmService, CallbackLogRepository callbackLogRepository,
                               ObjectMapper objectMapper, Clock clock, @Value("${app.pg.webhook-secret}") String secret) {
        this.confirmService = confirmService;
        this.callbackLogRepository = callbackLogRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.secret = secret;
    }

    // 본문을 DTO가 아니라 byte[]로 받는다. 서명은 받은 원문 그대로 검증해야 하기 때문이다
    @PostMapping("/api/webhooks/pg")
    public ResponseEntity<Void> receive(@RequestBody byte[] body,
                                        @RequestHeader(value = "X-Signature", required = false) String signature) {
        String raw = new String(body, StandardCharsets.UTF_8);
        if (!Hmac.verify(body, signature, secret)) {
            callbackLogRepository.save(CallbackLog.invalidSignature("PG", raw, clock.instant()));   // 실패한 통지도 남긴다
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        PgResult result;
        try {
            result = objectMapper.readValue(body, PgResult.class);
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
        confirmService.confirm(result, raw, Signal.WEBHOOK);
        return ResponseEntity.ok().build();   // 커밋이 끝난 뒤에 200을 준다. 실패하면 PG가 다시 보낸다
    }
}