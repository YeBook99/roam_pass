import { test, expect } from '@playwright/test';
import { resetMocks, setSupplier, waitOrderStatus, uniqueUser } from '../helpers/api';
import { loginAsUser, createOrder, pay, orderStatus, setUserCookie } from '../helpers/flows';

// E1. 이심 정상 구매 한 줄기 + 브라우저를 닫아도 발급 완료 (2.1 집중할 곳, 2.3-3, 2.4-Q2)
test.describe('E1 이심 구매', () => {
  test.beforeEach(async ({ request }) => resetMocks(request));

  test('요금제 선택 → 결제 → 발급 대기 → ISSUED → QR 열람 시 QR_VIEWED', async ({ page }) => {
    await loginAsUser(page, uniqueUser('e1'));
    const orderNo = await createOrder(page, 'ESIM');
    await expect(page.getByTestId('order-status')).toHaveAttribute('data-status', 'PENDING');

    await pay(page);
    // 발급 대기 화면: 폴링으로 콜백 결과를 받아 ISSUED가 되어야 한다
    await expect(orderStatus(page)).toHaveAttribute('data-status', 'ISSUED', { timeout: 30_000 });

    await page.goto(`/my-sims/${orderNo}`);
    await expect(page.getByTestId('install-status')).toHaveAttribute('data-status', 'ISSUED');
    await expect(page.getByTestId('refund-eligibility')).toHaveAttribute('data-refundable', 'true');

    await page.getByTestId('qr-reveal-button').click();
    await expect(page.getByTestId('activation-code')).toContainText('LPA:1$');
    await expect(page.getByTestId('install-status')).toHaveAttribute('data-status', 'QR_VIEWED');
    // QR 열람은 설치 근거가 아니므로 여전히 환불 가능 (07 §1)
    await expect(page.getByTestId('refund-eligibility')).toHaveAttribute('data-refundable', 'true');
  });

  test('결제 직후 브라우저를 닫아도 서버가 발급을 끝까지 진행한다', async ({ browser, request, baseURL }) => {
    await setSupplier(request, { callbackDelayMs: 3_000 });     // 콜백이 오기 전에 창을 닫도록
    const userId = uniqueUser('e1-close');

    const ctx1 = await browser.newContext();
    const page1 = await ctx1.newPage();
    await loginAsUser(page1, userId);
    const orderNo = await createOrder(page1, 'ESIM');
    await pay(page1);
    await ctx1.close();                                          // 사용자가 브라우저를 닫음

    await waitOrderStatus(request, orderNo, 'ISSUED', 30_000);

    const ctx2 = await browser.newContext();                     // 나중에 다시 들어옴
    await setUserCookie(ctx2, baseURL!, userId);
    const page2 = await ctx2.newPage();
    await page2.goto(`/my-sims/${orderNo}`);
    await expect(page2.getByTestId('order-status')).toHaveAttribute('data-status', 'ISSUED');
    await ctx2.close();
  });
});
