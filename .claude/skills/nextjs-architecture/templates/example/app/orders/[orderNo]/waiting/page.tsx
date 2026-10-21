import { getMyOrder } from '@/features/order/server';
import { IssueStatusPoller } from '@/features/order/components/IssueStatusPoller';
import { orNotFound } from '@/lib/api-server';

export const dynamic = 'force-dynamic';

// 발급 대기: 첫 상태는 서버에서 한 번 조회하고, 이후 변화는 IssueStatusPoller가 폴링한다
export default async function WaitingPage({ params }: { params: Promise<{ orderNo: string }> }) {
  const { orderNo } = await params;
  const order = await orNotFound(getMyOrder(orderNo));

  return (
    <div>
      <h1>발급 대기</h1>
      <IssueStatusPoller initial={order} />
    </div>
  );
}
