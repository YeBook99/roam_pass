package com.roampass.issue.domain;

// SIM 설치 상태. 순서(rank)가 있고 앞으로만 간다 (docs/07 §3)
// 늦게 도착한 옛날 정보 때문에 ACTIVE가 INSTALLED로 되돌아가는 일을 막기 위해서다
public enum InstallStatus {
    ISSUED(1),       // 발급됨. QR·활성화 코드를 보여 줄 수 있음
    QR_VIEWED(2),    // 사용자가 우리 화면에서 QR을 봄 (우리가 직접 아는 유일한 신호, 설치 증거는 아님)
    INSTALLED(3),    // 기기에 설치됨 (공급사 기록으로만 알 수 있음)
    ACTIVE(4),       // 현지에서 개통되어 사용 시작
    EXPIRED(5);      // 사용 기간 끝

    private final int rank;

    InstallStatus(int rank) {
        this.rank = rank;
    }

    public boolean canMoveTo(InstallStatus to) {
        return to.rank > this.rank;
    }

    // 아직 공급사에 물어볼 필요가 있는 상태 (개통 전)
    public boolean isWaitingForActivation() {
        return this == ISSUED || this == QR_VIEWED || this == INSTALLED;
    }
}
