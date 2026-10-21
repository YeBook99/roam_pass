// 백엔드 issue.dto.ProfileResponse와 1:1. 발급된 SIM 한 장의 정보
// 설치 상태는 앞으로만 간다: ISSUED → QR_VIEWED → INSTALLED → ACTIVE → EXPIRED (docs/07)
export type InstallStatus = 'ISSUED' | 'QR_VIEWED' | 'INSTALLED' | 'ACTIVE' | 'EXPIRED';

export type Profile = {
  orderNo: string;
  iccid: string; // SIM 고유 번호
  activationCode: string; // LPA:1$... 폰 설정에서 직접 입력하거나 QR로 바꿔 쓴다
  installStatus: InstallStatus;
  qrViewedAt: string | null;
  installedAt: string | null;
  activatedAt: string | null; // 개통 시각 (현지에서 처음 켠 때)
  expiresAt: string | null; // 개통 시각 + 일수 × 24시간
};

export const INSTALL_STATUS_LABEL: Record<InstallStatus, string> = {
  ISSUED: '발급됨 (아직 설치 전)',
  QR_VIEWED: '설치 코드 확인함',
  INSTALLED: '기기에 설치됨',
  ACTIVE: '사용 중',
  EXPIRED: '사용 기간 끝남',
};
