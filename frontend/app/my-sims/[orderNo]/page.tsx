import Link from 'next/link';
import { KstTime } from '@/components/KstTime';
import { StatusPill } from '@/components/StatusPill';
import { QrReveal } from '@/features/issue/components/QrReveal';
import { RecheckButton } from '@/features/issue/components/RecheckButton';
import { UsagePeriodBar } from '@/features/issue/components/UsagePeriodBar';
import { findMyProfile } from '@/features/issue/server';
import { INSTALL_STATUS_LABEL, INSTALL_STATUS_TONE } from '@/features/issue/types';
import { getMyOrder } from '@/features/order/server';
import { ORDER_STATUS_LABEL, ORDER_STATUS_TONE } from '@/features/order/types';
import { SIM_TYPE_LABEL } from '@/features/plan/types';
import { orNotFound } from '@/lib/api-server';

export const dynamic = 'force-dynamic';

// 내 SIM 상세 (/my-sims/주문번호). 이 화면의 질문: "설치했나, 언제까지 쓸 수 있나?"
// 넓은 쪽 = SIM 카드(설치 상태, 사용 기간 막대, 설치 코드), 좁은 쪽 = 주문 정보
// 화면은 서버가 그리고, [설치 코드 보기]·[다시 확인] 버튼만 브라우저에서 돈다
// 버튼이 끝나면 router.refresh()를 부르고, 그러면 이 페이지가 서버에서 다시 실행되어 최신 상태를 그린다
export default async function MySimDetailPage({ params }: { params: Promise<{ orderNo: string }> }) {
  const { orderNo } = await params;
  // 주문과 SIM 정보를 동시에 조회한다. SIM이 아직 없으면 profile은 null
  const [order, profile] = await Promise.all([orNotFound(getMyOrder(orderNo)), findMyProfile(orderNo)]);
  const now = Date.now(); // 사용 기간 막대의 "지금". 서버에서 그릴 때의 시각

  return (
    <div className="page">
      <div className="title-row">
        <div>
          <h1>내 SIM</h1>
          <p className="subtitle mono">{order.orderNo}</p>
        </div>
      </div>

      <div className="grid-main-side">
        {profile === null ? (
          <section className="card stack">
            <h2>아직 SIM이 발급되지 않았습니다</h2>
            {(order.status === 'PAID' || order.status === 'ISSUING') && (
              <Link href={`/orders/${order.orderNo}/waiting`}>발급 진행 상황 보기</Link>
            )}
          </section>
        ) : (
          <section className="card stack">
            <div className="row">
              <h2>설치 상태</h2>
              <StatusPill
                tone={INSTALL_STATUS_TONE[profile.installStatus]}
                label={INSTALL_STATUS_LABEL[profile.installStatus]}
              />
            </div>
            <UsagePeriodBar activatedAt={profile.activatedAt} expiresAt={profile.expiresAt} now={now} />
            <dl className="kv">
              <dt>ICCID</dt>
              <dd className="mono">{profile.iccid}</dd>
            </dl>
            <QrReveal orderNo={order.orderNo} />
            <RecheckButton orderNo={order.orderNo} />
          </section>
        )}

        <section className="card">
          <h2>주문</h2>
          <dl className="kv">
            <dt>상태</dt>
            <dd>
              <StatusPill tone={ORDER_STATUS_TONE[order.status]} label={ORDER_STATUS_LABEL[order.status]} />
            </dd>
            <dt>종류</dt>
            <dd>{SIM_TYPE_LABEL[order.simType]}</dd>
            <dt>여행 시작일</dt>
            <dd className="num">{order.travelStartDate}</dd>
            <dt>결제 시각</dt>
            <dd className="num">
              <KstTime iso={order.paidAt} />
            </dd>
            <dt>금액</dt>
            <dd className="num">
              <strong>{order.amount.toLocaleString('ko-KR')}원</strong>
            </dd>
          </dl>
        </section>
      </div>
    </div>
  );
}
