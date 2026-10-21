import Link from 'next/link';
import { FreshnessBadge } from '@/components/FreshnessBadge';
import { KstTime } from '@/components/KstTime';
import { RefreshControls } from '@/features/admin/components/RefreshControls';
import { getAdminOrder } from '@/features/admin/server';
import { TIMELINE_TARGET_LABEL, type TimelineItem } from '@/features/admin/types';
import { orNotFound } from '@/lib/api-server';

export const dynamic = 'force-dynamic';

// 발생 시각과 기록 시각이 이보다 많이 차이 나면 "늦게 도착한 신호"로 보고 두 시각을 모두 보여 준다 (docs/09 §2)
const LATE_SIGNAL_MS = 60_000;

function isLate(item: TimelineItem): boolean {
  return Date.parse(item.recordedAt) - Date.parse(item.occurredAt) > LATE_SIGNAL_MS;
}

// 어드민 주문 상세 (/admin/orders/주문번호)
// 주문·결제·SIM 정보와, 세 곳의 상태 이력을 기록 순서대로 한 줄씩 모은 타임라인을 보여 준다
// 화면 전체를 서버가 그리고, 새로고침·"N초 전" 표시만 브라우저에서 돈다
export default async function AdminOrderDetailPage({ params }: { params: Promise<{ orderNo: string }> }) {
  const { orderNo } = await params;
  const { order, timeline, fetchedAt } = await orNotFound(getAdminOrder(orderNo));

  // 표의 한 칸 = [이름, 값]. 값이 없으면 "-"
  const rows: [string, React.ReactNode][] = [
    ['주문번호', order.orderNo],
    ['사용자', order.userId],
    ['종류 / 금액', `${order.simType} / ${order.amount.toLocaleString('ko-KR')}원`],
    ['여행 시작일(KST)', order.travelStartDate],
    ['주문 상태', order.orderStatus],
    ['주문 시각', <KstTime key="c" iso={order.createdAt} />],
    ['결제 상태', order.payStatus ?? '-'],
    ['PG 거래번호', order.pgTid ?? '-'],
    ['결제 승인 시각', <KstTime key="a" iso={order.approvedAt} />],
    ['공급사 주문번호', order.supplierOrderNo ?? '-'],
    ['ICCID', order.iccid ?? '-'],
    ['설치 상태', order.installStatus ?? '-'],
    ['QR 확인 / 설치 / 개통', <span key="i"><KstTime iso={order.qrViewedAt} /> / <KstTime iso={order.installedAt} /> / <KstTime iso={order.activatedAt} /></span>],
    ['만료 시각', <KstTime key="e" iso={order.expiresAt} />],
  ];

  return (
    <div className="stack">
      <Link href="/admin">← 목록으로</Link>
      <h1>주문 {order.orderNo}</h1>
      <div className="row">
        <FreshnessBadge fetchedAt={fetchedAt} />
        <RefreshControls />
      </div>

      <table>
        <tbody>
          {rows.map(([label, value]) => (
            <tr key={label}>
              <th>{label}</th>
              <td>{value}</td>
            </tr>
          ))}
        </tbody>
      </table>

      <h2>타임라인</h2>
      <table>
        <thead>
          <tr>
            <th>언제 (KST)</th>
            <th>무엇이</th>
            <th>누가 / 어떤 신호로</th>
            <th>사유</th>
          </tr>
        </thead>
        <tbody>
          {timeline.map((item, index) => (
            <tr key={index}>
              <td>
                {isLate(item) ? (
                  <>
                    발생 <KstTime iso={item.occurredAt} seconds /> / 기록 <KstTime iso={item.recordedAt} seconds />
                  </>
                ) : (
                  <KstTime iso={item.recordedAt} seconds />
                )}
              </td>
              <td>
                {TIMELINE_TARGET_LABEL[item.target]} {item.fromStatus ?? '(생성)'} → {item.toStatus}
              </td>
              <td>
                {item.actor} · {item.signal}
              </td>
              <td>{item.reason ?? ''}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
