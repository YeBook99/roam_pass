import { BrowserContext, Page, expect } from '@playwright/test';

// 화면 흐름 도우미. 셀렉터는 data-testid 계약(.claude/skills/e2e/SKILL.md)만 사용한다.

export function tomorrowKst(): string {
  return new Date(Date.now() + 24 * 3600 * 1000).toLocaleDateString('sv-SE', { timeZone: 'Asia/Seoul' });
}

/** 첫 화면에서 사용자 아이디 입력 (쿠키 저장) */
export async function loginAsUser(page: Page, userId: string) {
  await page.goto('/');
  await page.getByTestId('user-id-input').fill(userId);
  await page.getByTestId('user-id-submit').click();
}

/** 화면을 거치지 않고 사용자 쿠키만 심는다 (다른 시나리오의 전제 조건용) */
export async function setUserCookie(context: BrowserContext, baseURL: string, userId: string) {
  await context.addCookies([{ name: 'userId', value: userId, url: baseURL }]);
}

/** 요금제 선택 → 주문 생성 → 결제 페이지까지. orderNo 반환 */
export async function createOrder(page: Page, simType: 'ESIM' | 'USIM'): Promise<string> {
  await page.goto('/');
  const card = page.locator(`[data-testid^="plan-card-"][data-sim-type="${simType}"]`).first();
  await card.getByTestId('plan-select-button').click();
  await page.getByTestId('travel-start-date-input').fill(tomorrowKst());
  await page.getByTestId('create-order-button').click();
  await expect(page).toHaveURL(/\/checkout\/[^/]+$/);
  return page.url().split('/checkout/')[1];
}

/** 결제 버튼 클릭 → 발급 대기 화면 도착까지 */
export async function pay(page: Page) {
  await page.getByTestId('pay-button').click();
  await expect(page).toHaveURL(/\/orders\/[^/]+\/waiting$/);
}

export function orderStatus(page: Page) {
  return page.getByTestId('order-status');
}
