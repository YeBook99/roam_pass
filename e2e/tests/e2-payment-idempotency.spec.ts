import { test, expect } from '@playwright/test';
import { resetMocks, setPg, adminOrder, pgApproveCount, waitOrderStatus, uniqueUser } from '../helpers/api';
import { loginAsUser, createOrder } from '../helpers/flows';

// E2. 결제 중복 방지: 버튼 연타·웹훅 중복에도 결제 1번 (2.3-2, 2.4-Q3·Q4)
// 동시성 자체의 증명은 백엔드 동시성 테스트가 맡고, 여기서는 "화면에서 실제로 일어나는 중복"을 확인한다.
test.describe('E2 결제 중복 방지', () => {
  test.beforeEach(async ({ request }) => resetMocks(request));

  test('결제 버튼을 연타해도 결제는 한 번만 승인된다', async ({ page, request }) => {
    await loginAsUser(page, uniqueUser('e2-click'));
    const orderNo = await createOrder(page, 'ESIM');

    // React가 버튼을 비활성화하기 전에 클릭 5번을 한꺼번에 발생시킨다 (화면 방어가 뚫려도 서버가 막는지)
    await page.getByTestId('pay-button').evaluate((btn) => {
      for (let i = 0; i < 5; i++) (btn as HTMLButtonElement).click();
    });

    await waitOrderStatus(request, orderNo, 'ISSUED', 30_000);
    const detail = await adminOrder(request, orderNo);
    expect(detail.payments.filter((p) => p.status === 'APPROVED')).toHaveLength(1);
    expect(await pgApproveCount(request, orderNo)).toBe(1);
  });

  test('PG 웹훅이 3번 중복으로 와도 PENDING→PAID 전이는 한 번만 기록된다', async ({ page, request }) => {
    await setPg(request, { webhookCount: 3 });
    await loginAsUser(page, uniqueUser('e2-webhook'));
    const orderNo = await createOrder(page, 'ESIM');
    await page.getByTestId('pay-button').click();

    await waitOrderStatus(request, orderNo, 'ISSUED', 30_000);
    const { timeline } = await adminOrder(request, orderNo);
    const paid = timeline.filter((t) => t.target === 'ORDER' && t.from === 'PENDING' && t.to === 'PAID');
    expect(paid).toHaveLength(1);
  });
});
