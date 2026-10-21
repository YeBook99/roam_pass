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
      {/* 기다리는 동안 미리 읽어 둘 설치 방법 (docs/08 §4). 바뀌지 않는 글이라 서버에서 그린다 */}
      <section>
        <h2>이심 설치 방법</h2>
        <ul>
          <li>아이폰: 설정 → 셀룰러 → eSIM 추가 → QR 코드 사용 → 세부사항 직접 입력</li>
          <li>안드로이드: 설정 → 연결 → SIM 관리자 → eSIM 추가 → 활성화 코드 입력</li>
        </ul>
        <p>설치는 출국 전에 해 두고, 데이터 사용(개통)은 현지에 도착해서 켜세요.</p>
      </section>
    </div>
  );
}
