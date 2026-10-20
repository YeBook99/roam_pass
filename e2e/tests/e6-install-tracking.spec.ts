import { test, expect } from '@playwright/test';
import { resetMocks, adminOrder, setProfileState, waitOrderStatus, uniqueUser } from '../helpers/api';
import { loginAsUser, createOrder, pay } from '../helpers/flows';

// E6. 설치 추적: 콜백이 빠져도 상태 조회로 메우고, 설치되면 환불 불가 (2.3-4, 2.4-Q5, 2.9 I6)
test.describe('E6 설치 추적', () => {
  test.beforeEach(async ({ request }) => resetMocks(request));

  test('콜백 누락된 설치를 다시 확인으로 반영하고, 개통되면 만료일을 KST로 보여 준다', async ({ page, request }) => {
    await loginAsUser(page, uniqueUser('e6'));
    const orderNo = await createOrder(page, 'ESIM');
    await pay(page);
    await waitOrderStatus(request, orderNo, 'ISSUED', 30_000);
    const iccid = (await adminOrder(request, orderNo)).profile!.iccid;

    // 기기에 설치됐지만 공급사 콜백이 빠진 상황
    await setProfileState(request, iccid, 'INSTALLED', false);
    await page.goto(`/my-sims/${orderNo}`);
    await page.getByTestId('recheck-button').click();               // 즉시 상태 조회
    await expect(page.getByTestId('install-status')).toHaveAttribute('data-status', 'INSTALLED');
    await expect(page.getByTestId('refund-eligibility')).toHaveAttribute('data-refundable', 'false');

    // 현지 개통 (콜백 정상)
    await setProfileState(request, iccid, 'ENABLED', true);
    await expect
      .poll(async () => (await adminOrder(request, orderNo)).profile!.installStatus, { timeout: 15_000 })
      .toBe('ACTIVE');
    await page.reload();
    await expect(page.getByTestId('install-status')).toHaveAttribute('data-status', 'ACTIVE');
    await expect(page.getByTestId('expires-at')).toContainText('KST');
  });
});
