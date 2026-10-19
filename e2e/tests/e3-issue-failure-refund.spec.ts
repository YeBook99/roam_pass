import { test, expect } from '@playwright/test';
import { resetMocks, setSupplier, adminOrder, waitOrderStatus, uniqueUser } from '../helpers/api';
import { loginAsUser, createOrder, pay, orderStatus } from '../helpers/flows';

// E3. 결제됐는데 발급 최종 실패 → 자동 환불, 사용자에게 상황이 보임 (2.3-3, 2.4-Q2, 2.9 I4)
// e2e 프로필은 콜백 타임아웃·재시도 간격이 짧다 (docker-compose.e2e.yml)
test.describe('E3 발급 실패 보상', () => {
  test.beforeEach(async ({ request }) => resetMocks(request));

  test('공급사 발급이 계속 실패하면 재시도 상한 후 자동 환불된다', async ({ page, request }) => {
    await setSupplier(request, { issueFailRate: 1 });
    await loginAsUser(page, uniqueUser('e3'));
    const orderNo = await createOrder(page, 'ESIM');
    await pay(page);

    // 대기 화면은 실패·환불 상황을 사용자에게 보여 줘야 한다 (흔적 없이 멈추면 안 됨)
    await expect(orderStatus(page)).toHaveAttribute('data-status', /ISSUE_FAILED|REFUNDING|REFUNDED/, { timeout: 60_000 });
    await expect(page.getByTestId('issue-failed-notice')).toBeVisible();

    await waitOrderStatus(request, orderNo, 'REFUNDED', 60_000);
    const detail = await adminOrder(request, orderNo);
    expect(detail.payments[0].status).toBe('CANCELED');
    expect(detail.profile).toBeNull();                                   // 프로필은 붙지 않음
    expect(detail.timeline.some((t) => t.to === 'ISSUE_FAILED' && t.actor === 'SYSTEM')).toBeTruthy();
  });

  test('발급 콜백이 영영 오지 않아도 타임아웃 재시도 후 환불로 끝난다', async ({ page, request }) => {
    await setSupplier(request, { callbackDropRate: 1 });
    await loginAsUser(page, uniqueUser('e3-drop'));
    const orderNo = await createOrder(page, 'ESIM');
    await pay(page);

    await waitOrderStatus(request, orderNo, 'REFUNDED', 60_000);
  });
});
