import Link from 'next/link';
import { Facts } from '@/components/Facts';
import { FreshnessBadge } from '@/components/FreshnessBadge';
import { KstTime } from '@/components/KstTime';
import { StatusPill } from '@/components/StatusPill';
import { RefreshControls } from '@/features/admin/components/RefreshControls';
import { getAdminOrder } from '@/features/admin/server';
import { SupplierStatePanel } from '@/features/mock/components/SupplierStatePanel';
import { INSTALL_STATUS_TONE } from '@/features/issue/types';
import { ORDER_STATUS_TONE } from '@/features/order/types';
import { PAYMENT_STATUS_TONE } from '@/features/payment/types';
import { PaymentResolutionLine } from '@/features/admin/components/PaymentResolutionLine';
import { SIGNAL_LABEL, TIMELINE_TARGET_LABEL, type TimelineItem } from '@/features/admin/types';
import { orNotFound } from '@/lib/api-server';

export const dynamic = 'force-dynamic';

// 발생 시각과 기록 시각이 이보다 많이 차이 나면 "늦게 도착한 신호"로 보고 두 시각을 모두 보여 준다 (docs/09 §2)
const LATE_SIGNAL_MS = 60_000;

function isLate(item: TimelineItem): boolean {
  return Date.parse(item.recordedAt) - Date.parse(item.occurredAt) > LATE_SIGNAL_MS;
}

// 어드민 주문 상세 (/admin/orders/주문번호). 이 화면의 질문: "이 주문은 지금 어디까지 왔고, 누가 언제 바꿨나?"
// 넓은 쪽 = 타임라인(세 곳의 상태 이력을 기록 순서대로 한 줄씩), 좁은 쪽 = 주문·결제·SIM 정보
// 화면 전체를 서버가 그리고, 새로고침·"N초 전" 표시만 브라우저에서 돈다
export default async function AdminOrderDetailPage({ params }: { params: Promise<{ orderNo: string }> }) {
  const { orderNo } = await params;
  const { order, timeline, fetchedAt } = await orNotFound(getAdminOrder(orderNo));

  return (
    <div className="page">
      <Link href="/admin" className="muted">
        ← 주문 목록
      </Link>
      {/* 머리: 주문 자체. 번호, 세 가지 상태, 한 줄 요약 */}
      <div className="title-row">
        <div>
          <h1 className="mono">{order.orderNo}</h1>
          <p className="subtitle">
            {order.userId} · {order.simType} · {order.amount.toLocaleString('ko-KR')}원 · 주문{' '}
            <KstTime iso={order.createdAt} />
          </p>
          <div className="row">
            <span className="muted">주문</span>
            <StatusPill tone={ORDER_STATUS_TONE[order.orderStatus]} label={order.orderStatus} />
            <span className="muted">결제</span>
            <StatusPill
              tone={order.payStatus ? PAYMENT_STATUS_TONE[order.payStatus] : 'neutral'}
              label={order.payStatus}
            />
            <span className="muted">설치</span>
            <StatusPill
              tone={order.installStatus ? INSTALL_STATUS_TONE[order.installStatus] : 'neutral'}
              label={order.installStatus}
            />
          </div>
          {/* 결제를 누가 확정했나 (요청 응답·결제 회사 알림·정기 확인 작업) */}
          <PaymentResolutionLine timeline={timeline} nowMs={Date.parse(fetchedAt)} />
        </div>
        <div className="row">
          <FreshnessBadge fetchedAt={fetchedAt} />
          <RefreshControls />
        </div>
      </div>

      <div className="grid-main-side grid-detail">
        <section className="card card-flush" aria-label="타임라인">
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th className="num">언제 (KST)</th>
                  <th>무엇이</th>
                  <th>누가 · 어떤 신호로</th>
                  <th>사유</th>
                </tr>
              </thead>
              <tbody>
                {timeline.map((item, index) => (
                  <tr key={index}>
                    <td className="num">
                      {isLate(item) ? (
                        <>
                          발생 <KstTime iso={item.occurredAt} seconds /> / 기록{' '}
                          <KstTime iso={item.recordedAt} seconds />
                        </>
                      ) : (
                        <KstTime iso={item.recordedAt} seconds />
                      )}
                    </td>
                    <td>
                      {TIMELINE_TARGET_LABEL[item.target]}{' '}
                      <span className="muted">{item.fromStatus ?? '(생성)'} →</span> {item.toStatus}
                    </td>
                    <td>
                      {item.actor} <span className="muted">· {SIGNAL_LABEL[item.signal]}</span>
                    </td>
                    <td className="muted wrap">{item.reason ?? ''}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>

        <section className="card">
          <Facts
            title="주문"
            rows={[
              [
                '여행 시작일',
                <span key="t" className="num">
                  {order.travelStartDate}
                </span>,
              ],
              ['결제 시각', order.paidAt ? <KstTime key="p" iso={order.paidAt} /> : null],
            ]}
          />
          <Facts
            title="결제"
            rows={[
              [
                'PG 거래번호',
                order.pgTid ? (
                  <span key="g" className="mono">
                    {order.pgTid}
                  </span>
                ) : null,
              ],
              ['승인 시각', order.approvedAt ? <KstTime key="a" iso={order.approvedAt} /> : null],
            ]}
          />
          <Facts
            title="SIM"
            rows={[
              [
                '공급사 주문번호',
                order.supplierOrderNo ? (
                  <span key="s" className="mono">
                    {order.supplierOrderNo}
                  </span>
                ) : null,
              ],
              [
                'ICCID',
                order.iccid ? (
                  <span key="i" className="mono">
                    {order.iccid}
                  </span>
                ) : null,
              ],
              ['QR 확인', order.qrViewedAt ? <KstTime key="q" iso={order.qrViewedAt} /> : null],
              ['설치', order.installedAt ? <KstTime key="n" iso={order.installedAt} /> : null],
              ['개통', order.activatedAt ? <KstTime key="v" iso={order.activatedAt} /> : null],
              ['만료', order.expiresAt ? <KstTime key="x" iso={order.expiresAt} /> : null],
            ]}
          />
        </section>
      </div>

      {/* 시연용: SIM이 발급된 주문이면 가짜 공급사의 설치·개통 상태를 여기서 바로 바꿀 수 있다 (시연 설정 화면과 같은 조각) */}
      {order.iccid && <SupplierStatePanel iccid={order.iccid} />}
    </div>
  );
}
