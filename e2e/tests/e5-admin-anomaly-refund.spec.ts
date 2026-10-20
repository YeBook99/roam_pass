import { test, expect } from '@playwright/test';
import { resetMocks, setSupplier, setPg, adminOrder, waitOrderStatus, uniqueUser, ADMIN_ID } from '../helpers/api';
import { loginAsUser, createOrder, pay } from '../helpers/flows';

// E5. 환불까지 실패한 주문이 어드민 이상 건에 보이고, 운영자가 사유와 함께 수동 환불한다 (2.3-5, 2.9 I7)
test.describe('E5 어드민 이상 건과 수동 환불', () => {
  test.beforeEach(async ({ request }) => resetMocks(request));

  test('REFUND_FAILED 주문 → 이상 건 노출 → 수동 환불 → 타임라인에 운영자·사유', async ({ page, request }) => {
    await setSupplier(request, { issueFailRate: 1 });
    await setPg(request, { cancelFailRate: 1 });

    await loginAsUser(page, uniqueUser('e5'));
    const orderNo = await createOrder(page, 'ESIM');
    await pay(page);
    await waitOrderStatus(request, orderNo, 'REFUND_FAILED', 80_000);  // 발급 3회 실패 → 환불 3회 실패

    // 어드민: 이상 건 목록에 보여야 한다
    await page.goto('/admin/anomalies');
    await page.getByTestId('admin-id-input').fill(ADMIN_ID);
    await page.getByTestId('admin-id-submit').click();
    const row = page.getByTestId(`anomaly-row-${orderNo}`);
    await expect(row).toHaveAttribute('data-type', 'REFUND_FAILED');
    await expect(page.getByTestId('admin-freshness')).toContainText('전');   // "N초 전 데이터"

    // PG가 회복된 뒤 운영자가 수동 환불
    await setPg(request, { cancelFailRate: 0 });
    await page.goto(`/admin/orders/${orderNo}`);
    await page.getByTestId('admin-refund-button').click();
    await page.getByTestId('admin-reason-input').fill('e2e: PG 장애 복구 후 수동 환불');
    await page.getByTestId('admin-action-submit').click();

    await waitOrderStatus(request, orderNo, 'REFUNDED', 30_000);
    const { timeline } = await adminOrder(request, orderNo);
    expect(timeline.some((t) => t.actor === `ADMIN:${ADMIN_ID}`)).toBeTruthy();

    await page.reload();
    await expect(page.getByTestId('timeline-item').filter({ hasText: `ADMIN:${ADMIN_ID}` }).first()).toBeVisible();
  });
});
