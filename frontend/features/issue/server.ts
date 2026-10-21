import 'server-only';

import { ApiError } from '@/lib/api-error';
import { serverGet } from '@/lib/api-server';
import type { Profile } from './types';

// 내 SIM 정보. 아직 발급 전이면 백엔드가 404를 주므로 null로 바꿔 돌려준다
// (주문은 있는데 SIM이 아직 없는 것은 에러가 아니라 정상 상태다)
export async function findMyProfile(orderNo: string): Promise<Profile | null> {
  try {
    return await serverGet<Profile>(`/api/my/orders/${encodeURIComponent(orderNo)}/profile`);
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) return null;
    throw e;
  }
}
