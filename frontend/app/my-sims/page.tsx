import Link from 'next/link';
import { KstTime } from '@/components/KstTime';
import { StatusPill } from '@/components/StatusPill';
import { getMyOrders } from '@/features/order/server';
import { ORDER_STATUS_LABEL } from '@/features/order/types';
import { SIM_TYPE_LABEL } from '@/features/plan/types';

export const dynamic = 'force-dynamic';

// 내 SIM 목록 (/my-sims). 이 화면의 질문: "내가 산 SIM이 어떤 상태지?"
// 버튼이나 타이머가 없어서 페이지 전체가 Server Component다 (새로고침으로 충분)
export default async function MySimsPage() {
  const orders = await getMyOrders(); // 아이디 쿠키가 없으면 serverGet이 첫 화면으로 보낸다

  return (
    <div className="page">
      <div className="title-row">
        <div>
          <h1>내 SIM</h1>
          <p className="subtitle">주문 {orders.length}건 · 최근 주문이 위에 있어요</p>
        </div>
      </div>

      {orders.length === 0 ? (
        <section className="card">
          <p>
            아직 주문이 없습니다. <Link href="/">요금제 보러 가기</Link>
          </p>
        </section>
      ) : (
        <section className="card card-flush">
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>주문번호</th>
                  <th>종류</th>
                  <th>상태</th>
                  <th className="num">금액</th>
                  <th className="num">주문 시각</th>
                </tr>
              </thead>
              <tbody>
                {orders.map((order) => (
                  <tr key={order.orderNo}>
                    <td>
                      <Link href={`/my-sims/${order.orderNo}`} className="mono">
                        {order.orderNo}
                      </Link>
                    </td>
                    <td>{SIM_TYPE_LABEL[order.simType]}</td>
                    <td>
                      <StatusPill status={order.status} label={ORDER_STATUS_LABEL[order.status]} />
                    </td>
                    <td className="num">{order.amount.toLocaleString('ko-KR')}원</td>
                    <td className="num">
                      <KstTime iso={order.createdAt} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}
    </div>
  );
}
