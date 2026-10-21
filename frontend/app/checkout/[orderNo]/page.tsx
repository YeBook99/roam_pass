import { KstTime } from '@/components/KstTime';
import { getMyOrder } from '@/features/order/server';
import { ORDER_STATUS_LABEL } from '@/features/order/types';
import { PayButton } from '@/features/payment/components/PayButton';
import { orNotFound } from '@/lib/api-server';

export const dynamic = 'force-dynamic'; // 결제 상태는 캐시하지 않는다

// 결제 화면 (/checkout/주문번호)
// 주문 정보는 서버에서 그리고(Server Component), 결제 버튼만 브라우저에서 돈다
// 두 기능(order, payment)은 여기 app/에서 조립한다 (기능끼리 서로 컴포넌트를 import하지 않는다)
export default async function CheckoutPage({ params }: { params: Promise<{ orderNo: string }> }) {
  const { orderNo } = await params; // URL의 [orderNo] 부분. Next 15부터 params는 Promise라 await한다
  const order = await orNotFound(getMyOrder(orderNo)); // 없거나 남의 주문이면 404 화면

  return (
    <div className="stack">
      <h1>결제</h1>
      <section className="card stack">
        <p className="muted">주문번호 {order.orderNo}</p>
        <p>금액 {order.amount.toLocaleString('ko-KR')}원</p>
        <p>
          결제 기한 <KstTime iso={order.expiresAt} />
        </p>
        {order.status === 'PENDING' ? (
          <PayButton orderNo={order.orderNo} />
        ) : (
          <p>이미 처리된 주문입니다 ({ORDER_STATUS_LABEL[order.status]})</p>
        )}
      </section>
    </div>
  );
}
