package com.roampass.common.time;

import java.time.ZoneId;

// 운영 기준 시간대. 저장은 UTC로 하고, 날짜 판단과 화면 표시에만 쓴다
public final class Kst {

    public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private Kst() {
    }
}
