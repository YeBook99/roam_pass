import { clientSend } from '@/lib/api-client';
import type { Profile } from './types';

// [설치 코드 보기]를 누를 때. 백엔드가 처음 본 시각을 기록한다 (ISSUED → QR_VIEWED, 두 번째부터는 그대로)
export function viewQr(orderNo: string): Promise<Profile> {
  return clientSend<Profile>('POST', `/api/my/orders/${encodeURIComponent(orderNo)}/profile/qr-view`);
}

// [다시 확인]을 누를 때. 백엔드가 공급사에 바로 물어보고 설치·개통 상태를 반영한다
export function refreshProfile(orderNo: string): Promise<Profile> {
  return clientSend<Profile>('POST', `/api/my/orders/${encodeURIComponent(orderNo)}/profile/refresh`);
}
