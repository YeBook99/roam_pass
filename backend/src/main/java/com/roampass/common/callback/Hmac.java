package com.roampass.common.callback;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

// 웹훅·콜백 서명: X-Signature = hex(HMAC-SHA256(원문 바이트, 공유 비밀키))
// 원문 바이트 그대로 계산해야 한다. JSON을 객체로 바꿨다가 다시 만들면 공백·순서가 달라져 서명이 맞지 않는다
public final class Hmac {

    private static final String ALGORITHM = "HmacSHA256";

    private Hmac() {
    }

    public static String sign(byte[] body, String secret) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(body));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC 계산 실패", e);
        }
    }

    // MessageDigest.isEqual은 앞 글자가 달라도 끝까지 비교한다(고정 시간 비교). equals는 시간 차이로 서명이 새어 나갈 수 있다
    public static boolean verify(byte[] body, String signature, String secret) {
        if (signature == null) {
            return false;
        }
        return MessageDigest.isEqual(sign(body, secret).getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8));
    }
}
