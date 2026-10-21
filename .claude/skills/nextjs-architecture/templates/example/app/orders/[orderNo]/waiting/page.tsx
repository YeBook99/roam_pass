import { getMyOrder } from '@/features/order/server';
import { IssueStatusPoller } from '@/features/order/components/IssueStatusPoller';
import { orNotFound } from '@/lib/api-server';

export const dynamic = 'force-dynamic';

// 발급 대기 화면 (/orders/주문번호/waiting)
// 첫 상태는 서버에서 한 번 조회해서 넘기고(initial), 그 뒤 변화는 IssueStatusPoller가 브라우저에서 2초마다 확인한다
export default async function WaitingPage({ params }: { params: Promise<{ orderNo: string }> }) {
  const { orderNo } = await params;
  const order = await orNotFound(getMyOrder(orderNo));

  return (
    <div className="stack">
      <h1>발급 대기</h1>
      <p>주문번호 {order.orderNo}</p>
      <IssueStatusPoller initial={order} />
    </div>
  );
}
