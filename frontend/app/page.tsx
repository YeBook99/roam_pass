import { UserIdForm } from '@/components/UserIdForm';
import { OrderForm } from '@/features/order/components/OrderForm';
import { getPlans } from '@/features/plan/server';
import { SIM_TYPE_LABEL } from '@/features/plan/types';
import { getUserId } from '@/lib/api-server';
import { todayKst } from '@/lib/time';

export const dynamic = 'force-dynamic'; // 요청마다 새로 그린다 (쿠키에 따라 화면이 달라지므로)

// 첫 화면 = 요금제 목록 + 아이디 입력 + 주문 폼
// 이 파일은 Server Component다: 서버에서 요금제를 조회해 표를 그리고, 입력·버튼이 있는 부분만 Client Component로 넘긴다
export default async function HomePage() {
  const [plans, userId] = await Promise.all([getPlans(), getUserId()]); // 두 조회를 동시에

  return (
    <div className="stack">
      <h1>여행 데이터 SIM</h1>
      <UserIdForm currentUserId={userId} />

      <table>
        <thead>
          <tr>
            <th>국가</th>
            <th>기간</th>
            <th>데이터</th>
            <th>종류</th>
            <th>가격</th>
          </tr>
        </thead>
        <tbody>
          {plans.map((plan) => (
            <tr key={plan.id}>
              <td>{plan.country}</td>
              <td>{plan.days}일</td>
              <td>{Math.round(plan.dataMb / 1024)}GB</td>
              <td>{SIM_TYPE_LABEL[plan.simType]}</td>
              <td>{plan.price.toLocaleString('ko-KR')}원</td>
            </tr>
          ))}
        </tbody>
      </table>

      <h2>주문하기</h2>
      <OrderForm plans={plans} todayKst={todayKst()} disabled={!userId} />
    </div>
  );
}
