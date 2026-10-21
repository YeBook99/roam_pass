// 백엔드 plan.dto.PlanResponse와 1:1. 백엔드가 바뀌면 같이 고친다
export type SimType = 'ESIM' | 'USIM';

export type Plan = {
  id: number;
  planCode: string;
  country: string; // JP, US, VN, EU
  days: number; // 사용 일수 (개통 시각부터 days × 24시간)
  dataMb: number;
  simType: SimType;
  price: number; // 원
};

export const SIM_TYPE_LABEL: Record<SimType, string> = { ESIM: '이심', USIM: '유심' };
