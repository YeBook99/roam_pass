package com.roampass.order.domain;

import java.security.SecureRandom;

// 외부에 노출하는 주문번호: "RP" + 대문자·숫자 12자리. 겹칠 확률은 매우 낮고, 겹치면 order_no UNIQUE가 막는다
public final class OrderNo {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";   // 헷갈리는 0/O, 1/I 제외
    private static final SecureRandom RANDOM = new SecureRandom();

    private OrderNo() {
    }

    public static String next() {
        StringBuilder sb = new StringBuilder("RP");
        for (int i = 0; i < 12; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
