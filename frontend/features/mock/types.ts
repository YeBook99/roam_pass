// 시연용 가짜 결제 회사(모의 PG)·가짜 공급사 설정. 백엔드 mockpg.dto.MockPgSettings와 1:1
// 이 설정은 서버 하나에 하나뿐이라 모든 사용자에게 바로 적용된다
export type MockPgSettings = {
  failApprove: boolean; // true면 승인 대신 거절
  responseDelayMs: number; // 승인 응답을 늦춘다. 우리 서버는 3초까지만 기다린다
  webhookCount: number; // 결제 알림(웹훅)을 몇 번 보낼지. 0 = 누락, 2 이상 = 중복
  webhookDelayMs: number; // 승인 후 알림까지 기다리는 시간
  webhookAmountDelta: number; // 알림 금액에 더할 값. 0이 아니면 금액이 틀린 알림
  cancelFailRate: number; // 결제 취소를 실패시킬 확률 (0~1)
};

// 백엔드 MockPgSettings.DEFAULT와 같은 값. "시연 설정 켜짐" 표시를 판단할 때 쓴다
export const DEFAULT_PG_SETTINGS: MockPgSettings = {
  failApprove: false,
  responseDelayMs: 0,
  webhookCount: 1,
  webhookDelayMs: 500,
  webhookAmountDelta: 0,
  cancelFailRate: 0,
};

// 한 번 누르면 설정이 바뀌는 시연 상황. what = 무슨 일이 생기나, where = 어디서 확인하나
export type Scenario = {
  id: string;
  label: string;
  settings: Partial<MockPgSettings>;
  what: string;
  where: string;
};

export const SCENARIOS: Scenario[] = [
  {
    id: 'timeout',
    label: '결제 응답 시간 초과',
    settings: { responseDelayMs: 4000 },
    what: '결제 회사가 4초 늦게 답한다. 우리 서버는 3초까지만 기다려서 "결과 모름"으로 두고, 결제 알림이나 1분마다 도는 확인 작업이 확정한다',
    where:
      '결제 버튼 → 발급 대기 화면이 "결제 대기"에서 시작해 발급 완료까지 가는지, 운영 주문 상세 타임라인의 신호(웹훅·대사)',
  },
  {
    id: 'decline',
    label: '결제 거절',
    settings: { failApprove: true },
    what: '결제 회사가 승인하지 않는다',
    where: '결제 화면에 "결제가 거절되었습니다"가 뜨고, 다시 누르면 새 결제로 시도된다',
  },
  {
    id: 'webhook-missing',
    label: '결제 알림 누락',
    settings: { webhookCount: 0 },
    what: '결제 회사가 결제 알림을 보내지 않는다',
    where: '승인 응답만으로 결제가 확정되는지(운영 타임라인에 결제 승인 신호가 "API" 하나)',
  },
  {
    id: 'webhook-duplicate',
    label: '결제 알림 3번 중복',
    settings: { webhookCount: 3 },
    what: '같은 결제 알림이 3번 온다',
    where: '운영 타임라인에서 결제 승인·주문 결제 완료가 한 번씩만 기록되는지',
  },
  {
    id: 'webhook-amount',
    label: '금액이 틀린 알림',
    settings: { webhookAmountDelta: 1000 },
    what: '결제 알림의 금액이 실제보다 1,000원 많다',
    where: '알림은 거절되고 승인 응답으로만 확정되는지(운영 타임라인)',
  },
  {
    id: 'cancel-fail',
    label: '결제 취소 실패',
    settings: { cancelFailRate: 1 },
    what: '결제 취소 요청이 항상 실패한다',
    where: '환불 기능이 아직 없어서 지금은 쓸 곳이 없다(환불을 만들면 "3번 실패 → 환불 실패"를 보여 준다)',
  },
];

// 가짜 공급사의 SIM 상태 바꾸기 (백엔드 mocksupplier.dto.MockSupplierStateRequest)
export type SupplierState = 'INSTALLED' | 'ENABLED';

export type SupplierStateRequest = {
  state: SupplierState;
  activatedAt?: string; // 개통 시각(UTC). 비우면 지금
};

// 백엔드 mocksupplier.dto.MockSupplierProfileResponse
export type SupplierProfile = {
  iccid: string;
  state: string;
  activatedAt: string | null;
};

// 과제 예시의 개통 시각. 7일 요금제면 KST 2026-05-09 00:20에 만료된다
export const EXAMPLE_ACTIVATED_AT = '2026-05-01T15:20:00Z';
