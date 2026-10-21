import { OrderForm } from '@/features/order/components/OrderForm';
import { getPlans } from '@/features/plan/server';
import { SIM_TYPE_LABEL } from '@/features/plan/types';
import { getUserId } from '@/lib/api-server';
import { todayKst } from '@/lib/time';

export const dynamic = 'force-dynamic'; // 요청마다 새로 그린다 (쿠키에 따라 화면이 달라지므로)

// 요금제 화면. 이 화면의 질문: "어떤 요금제를 살까?"
// 넓은 쪽에 요금제 표, 좁은 쪽에 주문 입력. 서버가 표를 그리고 주문 입력만 브라우저에서 돈다
export default async function HomePage() {
  const [plans, userId] = await Promise.all([getPlans(), getUserId()]); // 두 조회를 동시에

  return (
    <div className="page">
      <div className="title-row">
        <div>
          <h1>요금제</h1>
          <p className="subtitle">현지에서 데이터를 처음 켠 순간부터 일수 × 24시간 동안 씁니다.</p>
        </div>
      </div>

      <div className="grid-main-side">
        <section className="card card-flush" aria-label="요금제 목록">
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>국가</th>
                  <th>종류</th>
                  <th className="num">기간</th>
                  <th className="num">데이터</th>
                  <th className="num">가격</th>
                </tr>
              </thead>
              <tbody>
                {plans.map((plan) => (
                  <tr key={plan.id}>
                    <td>{plan.country}</td>
                    <td>{SIM_TYPE_LABEL[plan.simType]}</td>
                    <td className="num">{plan.days}일</td>
                    <td className="num">{Math.round(plan.dataMb / 1024)} GB</td>
                    <td className="num">{plan.price.toLocaleString('ko-KR')}원</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>

        <section className="card">
          <h2>주문하기</h2>
          <OrderForm plans={plans} todayKst={todayKst()} disabled={!userId} />
        </section>
      </div>
    </div>
  );
}
