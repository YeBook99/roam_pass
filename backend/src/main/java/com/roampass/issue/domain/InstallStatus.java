package com.roampass.issue.domain;

// SIM 설치 상태. 지금은 발급 직후(ISSUED)만 쓴다. QR_VIEWED·INSTALLED·ACTIVE·EXPIRED는 설치 추적 단계에서 추가한다
public enum InstallStatus {
    ISSUED
}
