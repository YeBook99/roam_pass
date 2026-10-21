import 'server-only';

import { serverGet } from '@/lib/api-server';
import type { Plan } from './types';

// 요금제 목록. 첫 화면(Server Component)이 그릴 때 부른다
export function getPlans(): Promise<Plan[]> {
  return serverGet<Plan[]>('/api/plans');
}
