import 'server-only';

import { serverGet } from '@/lib/api-server';
import type { MockPgSettings } from './types';

// 시연 화면이 처음 그릴 때 지금 설정을 읽는다
export function getPgSettings(): Promise<MockPgSettings> {
  return serverGet<MockPgSettings>('/mock/pg/config');
}
