import { test, expect } from '@playwright/test';
import { resetMocks, waitOrderStatus, uniqueUser } from '../helpers/api';
import { loginAsUser, createOrder, pay, orderStatus } from '../helpers/flows';
import { validIccid, invalidChecksumIccid } from '../helpers/iccid';

// E4. 유심: PICKUP_READY → ICCID 등록(형식·체크 디지트·중복 거절) → 개통 (2.3-3)
test.describe('E4 유심 ICCID 등록', () => {
  test.beforeEach(async ({ request }) => resetMocks(request));

  test('잘못된 ICCID는 거절하고 올바른 ICCID로 개통한다', async ({ page, request }) => {
    await loginAsUser(page, uniqueUser('e4'));
    const orderNo = await createOrder(page, 'USIM');
    await pay(page);
    await expect(orderStatus(page)).toHaveAttribute('data-status', 'PICKUP_READY', { timeout: 30_000 });

    await page.goto(`/my-sims/${orderNo}`);
    const input = page.getByTestId('iccid-input');
    const submit = page.getByTestId('iccid-submit');

    await input.fill('8982123');                                        // 자릿수 오류
    await submit.click();
    await expect(page.getByTestId('iccid-error')).toBeVisible();

    await input.fill(invalidChecksumIccid());                           // 체크 디지트 오류
    await submit.click();
    await expect(page.getByTestId('iccid-error')).toBeVisible();

    const iccid = validIccid();
    await input.fill(iccid);
    await submit.click();
    await waitOrderStatus(request, orderNo, 'ISSUED', 30_000);
    await page.reload();
    await expect(page.getByTestId('order-status')).toHaveAttribute('data-status', 'ISSUED');

    // 같은 ICCID를 다른 주문에 등록하면 거절
    const other = await createOrder(page, 'USIM');
    await pay(page);
    await expect(orderStatus(page)).toHaveAttribute('data-status', 'PICKUP_READY', { timeout: 30_000 });
    await page.goto(`/my-sims/${other}`);
    await page.getByTestId('iccid-input').fill(iccid);
    await page.getByTestId('iccid-submit').click();
    await expect(page.getByTestId('iccid-error')).toBeVisible();
    await expect(page.getByTestId('order-status')).toHaveAttribute('data-status', 'PICKUP_READY');
  });
});
