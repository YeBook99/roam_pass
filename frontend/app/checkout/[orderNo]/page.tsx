import { KstTime } from '@/components/KstTime';
import { StatusPill } from '@/components/StatusPill';
import { getMyOrder } from '@/features/order/server';
import { ORDER_STATUS_LABEL, ORDER_STATUS_TONE } from '@/features/order/types';
import { PayButton } from '@/features/payment/components/PayButton';
import { SIM_TYPE_LABEL } from '@/features/plan/types';
import { orNotFound } from '@/lib/api-server';

export const dynamic = 'force-dynamic'; // 결제 상태는 캐시하지 않는다

// 결제 화면 (/checkout/주문번호). 이 화면의 질문: "이 금액을 결제할까?"
// 주문 정보는 서버에서 그리고(Server Component), 결제 버튼만 브라우저에서 돈다
// 두 기능(order, payment)은 여기 app/에서 조립한다 (기능끼리 서로 컴포넌트를 import하지 않는다)
export default async function CheckoutPage({ params }: { params: Promise<{ orderNo: string }> }) {
  const { orderNo } = await params; // URL의 [orderNo] 부분. Next 15부터 params는 Promise라 await한다
  const order = await orNotFound(getMyOrder(orderNo)); // 없거나 남의 주문이면 404 화면

  return (
    <div className="page">
      <div className="title-row">
        <div>
          <h1>결제</h1>
          <p className="subtitle mono">{order.orderNo}</p>
        </div>
      </div>

      <section className="card stack narrow">
        <dl className="kv">
          <dt>상품</dt>
          <dd>{SIM_TYPE_LABEL[order.simType]}</dd>
          <dt>여행 시작일</dt>
          <dd className="num">{order.travelStartDate}</dd>
          <dt>결제 기한</dt>
          <dd className="num">
            <KstTime iso={order.expiresAt} />
          </dd>
          <dt>상태</dt>
          <dd>
            <StatusPill tone={ORDER_STATUS_TONE[order.status]} label={ORDER_STATUS_LABEL[order.status]} />
          </dd>
          <dt>금액</dt>
          <dd className="num">
            <strong>{order.amount.toLocaleString('ko-KR')}원</strong>
          </dd>
        </dl>
        {order.status === 'PENDING' ? (
          <PayButton orderNo={order.orderNo} />
        ) : (
          <p className="muted">이미 처리된 주문입니다.</p>
        )}
      </section>
    </div>
  );
}
