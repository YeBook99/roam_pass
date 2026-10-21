import Link from 'next/link';
import { KstTime } from '@/components/KstTime';
import { QrReveal } from '@/features/issue/components/QrReveal';
import { RecheckButton } from '@/features/issue/components/RecheckButton';
import { findMyProfile } from '@/features/issue/server';
import { INSTALL_STATUS_LABEL } from '@/features/issue/types';
import { getMyOrder } from '@/features/order/server';
import { ORDER_STATUS_LABEL } from '@/features/order/types';
import { orNotFound } from '@/lib/api-server';

export const dynamic = 'force-dynamic';

// 내 SIM 상세 (/my-sims/주문번호) = 주문 정보 + 발급된 SIM 정보
// 화면은 서버가 그리고, [설치 코드 보기]·[다시 확인] 버튼만 브라우저에서 돈다
// 버튼이 끝나면 router.refresh()를 부르고, 그러면 이 페이지가 서버에서 다시 실행되어 최신 상태를 그린다
export default async function MySimDetailPage({ params }: { params: Promise<{ orderNo: string }> }) {
  const { orderNo } = await params;
  // 주문과 SIM 정보를 동시에 조회한다. SIM이 아직 없으면 profile은 null
  const [order, profile] = await Promise.all([orNotFound(getMyOrder(orderNo)), findMyProfile(orderNo)]);

  return (
    <div className="stack">
      <h1>내 SIM</h1>
      <section className="card stack">
        <p>
          주문번호 {order.orderNo} · {ORDER_STATUS_LABEL[order.status]} · {order.amount.toLocaleString('ko-KR')}원
        </p>
        <p className="muted">
          결제 시각 <KstTime iso={order.paidAt} />
        </p>
      </section>

      {profile === null ? (
        <section className="card stack">
          <p>아직 SIM이 발급되지 않았습니다.</p>
          {(order.status === 'PAID' || order.status === 'ISSUING') && (
            <Link href={`/orders/${order.orderNo}/waiting`}>발급 진행 상황 보기</Link>
          )}
        </section>
      ) : (
        <section className="card stack">
          <p>
            설치 상태: <strong>{INSTALL_STATUS_LABEL[profile.installStatus]}</strong>
          </p>
          <p>ICCID {profile.iccid}</p>
          <p>
            개통 시각 <KstTime iso={profile.activatedAt} /> · 만료 시각 <KstTime iso={profile.expiresAt} />
          </p>
          {/* 만료 시각 = 개통 시각 + 일수 × 24시간. 개통 전이면 "-"로 보인다 */}
          <QrReveal orderNo={order.orderNo} />
          <RecheckButton orderNo={order.orderNo} />
        </section>
      )}
    </div>
  );
}
