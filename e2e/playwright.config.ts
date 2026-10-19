import { defineConfig, devices } from '@playwright/test';

// 핵심 시나리오만 다루는 e2e. 모의 PG·공급사 설정이 서버 전역이라 테스트끼리 섞이지 않게 직렬 실행한다.
export default defineConfig({
  testDir: './tests',
  fullyParallel: false,
  workers: 1,
  retries: process.env.CI ? 1 : 0,
  timeout: 90_000,                      // 발급 콜백·재시도·환불을 기다리는 시나리오가 있음 (e2e 프로필은 짧은 타임아웃)
  expect: { timeout: 10_000 },
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:3000',
    headless: true,
    locale: 'ko-KR',
    timezoneId: 'Asia/Seoul',           // 화면의 KST 표시 검증
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
});
