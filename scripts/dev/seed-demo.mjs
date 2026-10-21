// 화면 캡처·시연용 고정 데이터: 사용자 demo-user 에게 이심 ISSUED 1건, 유심 PICKUP_READY 1건을 만든다.
// 사용: node scripts/dev/seed-demo.mjs   (API_URL 기본 http://localhost:8080, 백엔드가 실행 중이어야 함)
const API = process.env.API_URL ?? 'http://localhost:8080';
const USER = { 'X-User-Id': 'demo-user', 'Content-Type': 'application/json' };

async function call(method, path, body, headers = USER) {
  const res = await fetch(API + path, { method, headers, body: body && JSON.stringify(body) });
  if (!res.ok) throw new Error(`${method} ${path} → ${res.status} ${await res.text()}`);
  return res.status === 204 ? null : res.json();
}

async function waitStatus(orderNo, want) {
  for (let i = 0; i < 60; i++) {
    const o = await call('GET', `/api/my/orders/${orderNo}`);
    if (o.status === want) return o;
    await new Promise((r) => setTimeout(r, 1000));
  }
  throw new Error(`${orderNo}: ${want} 상태가 되지 않음`);
}

async function buy(simType) {
  const plans = await call('GET', '/api/plans');
  const plan = plans.find((p) => p.simType === simType);
  const travelStartDate = new Date(Date.now() + 86400000).toLocaleDateString('sv-SE', { timeZone: 'Asia/Seoul' });
  const { orderNo } = await call('POST', '/api/orders', { planId: plan.id, travelStartDate });
  await call('POST', `/api/orders/${orderNo}/payments`, null, { ...USER, 'Idempotency-Key': crypto.randomUUID() });
  return orderNo;
}

await call('POST', '/mock/reset');
const esim = await buy('ESIM');
await waitStatus(esim, 'ISSUED');
const usim = await buy('USIM');
await waitStatus(usim, 'PICKUP_READY');

console.log(JSON.stringify({ userId: 'demo-user', esimOrderNo: esim, usimOrderNo: usim }, null, 2));
