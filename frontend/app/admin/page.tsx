import Link from 'next/link';
import { FreshnessBadge } from '@/components/FreshnessBadge';
import { KstTime } from '@/components/KstTime';
import { StatusPill } from '@/components/StatusPill';
import { AdminFilters } from '@/features/admin/components/AdminFilters';
import { RefreshControls } from '@/features/admin/components/RefreshControls';
import { getAdminOrders } from '@/features/admin/server';
import { INSTALL_STATUS_TONE } from '@/features/issue/types';
import { ORDER_STATUS_TONE } from '@/features/order/types';
import { PAYMENT_STATUS_TONE } from '@/features/payment/types';
import { ADMIN_FILTER_KEYS, type AdminFilters as Filters } from '@/features/admin/types';
import { ApiError } from '@/lib/api-error';

export const dynamic = 'force-dynamic'; // 운영자는 항상 최신 상태를 봐야 한다

type SearchParams = Promise<Record<string, string | string[] | undefined>>;

// 어드민 주문 목록 (/admin?orderStatus=ISSUED&page=1)
// 이 페이지(Server Component)는 URL의 쿼리를 읽어 서버에서 조회하고 표를 그린다
// 필터 폼·새로고침·"N초 전" 표시만 브라우저에서 돈다. 페이지 이동 링크도 URL이라 서버에서 그린다
export default async function AdminOrdersPage({ searchParams }: { searchParams: SearchParams }) {
  const params = await searchParams; // Next 15부터 searchParams도 Promise라 await한다

  // URL 쿼리 → 필터. 같은 이름이 여러 번 오면(?a=1&a=2) 첫 값만 쓴다
  const filters: Filters = {};
  for (const key of ADMIN_FILTER_KEYS) {
    const value = params[key];
    filters[key] = Array.isArray(value) ? value[0] : value;
  }
  const page = Math.max(0, Number(params.page) || 0);

  // 공유받은 링크에 잘못된 값이 있으면(?orderStatus=FOO, ?from=abc) 백엔드가 400을 준다
  // 에러 화면 대신 백엔드가 알려 준 이유와 [초기화] 링크를 보여 준다
  let result;
  try {
    result = await getAdminOrders(filters, page);
  } catch (e) {
    if (e instanceof ApiError && e.status === 400) {
      return (
        <div className="page">
          <div className="title-row">
            <h1>주문</h1>
          </div>
          <section className="card stack">
            <p className="error">필터 값이 올바르지 않습니다: {e.message}</p>
            <Link href="/admin">필터 초기화</Link>
          </section>
        </div>
      );
    }
    throw e;
  }
  const lastPage = Math.max(0, Math.ceil(result.totalElements / result.size) - 1);

  // 지금 필터를 유지한 채 page만 바꾼 링크 주소
  const pageHref = (target: number) => {
    const query = new URLSearchParams();
    for (const key of ADMIN_FILTER_KEYS) {
      const value = filters[key];
      if (value) query.set(key, value);
    }
    query.set('page', String(target));
    return `/admin?${query}`;
  };

  return (
    <div className="page">
      {/* 제목 줄: 왼쪽에 제목과 건수, 오른쪽에 데이터가 몇 초 전 것인지와 새로고침 */}
      <div className="title-row">
        <div>
          <h1>주문</h1>
          <p className="subtitle">
            조건에 맞는 주문 {result.totalElements.toLocaleString('ko-KR')}건 · 최근 주문이 위에 있어요
          </p>
        </div>
        <div className="row">
          <FreshnessBadge fetchedAt={result.fetchedAt} />
          <RefreshControls />
        </div>
      </div>

      {/* key가 바뀌면 컴포넌트를 새로 만든다 → URL이 바뀌면(초기화, 뒤로 가기) 폼 값도 URL과 같아진다 */}
      <AdminFilters key={JSON.stringify(filters)} current={filters} />

      <section className="card card-flush" aria-label="주문 목록">
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>주문번호</th>
                <th>사용자</th>
                <th>종류</th>
                <th>주문</th>
                <th>결제</th>
                <th>설치</th>
                <th className="num">금액</th>
                <th className="num">주문 시각 (KST)</th>
              </tr>
            </thead>
            <tbody>
              {result.content.map((order) => (
                <tr key={order.orderNo}>
                  <td>
                    <Link href={`/admin/orders/${order.orderNo}`} className="mono">
                      {order.orderNo}
                    </Link>
                  </td>
                  <td>{order.userId}</td>
                  <td>{order.simType}</td>
                  <td>
                    <StatusPill tone={ORDER_STATUS_TONE[order.orderStatus]} label={order.orderStatus} />
                  </td>
                  <td>
                    <StatusPill
                      tone={order.payStatus ? PAYMENT_STATUS_TONE[order.payStatus] : 'neutral'}
                      label={order.payStatus}
                    />
                  </td>
                  <td>
                    <StatusPill
                      tone={order.installStatus ? INSTALL_STATUS_TONE[order.installStatus] : 'neutral'}
                      label={order.installStatus}
                    />
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
        {result.content.length === 0 && page > lastPage && (
          <p className="empty">
            <Link href={pageHref(0)}>없는 페이지입니다. 첫 페이지로</Link>
          </p>
        )}
        {result.content.length === 0 && page <= lastPage && <p className="empty">조건에 맞는 주문이 없습니다.</p>}
      </section>

      {/* 범위 밖 페이지(?page=99)면 페이지 링크를 숨긴다 */}
      {page <= lastPage && (
        <div className="row pager">
          {page > 0 && <Link href={pageHref(page - 1)}>← 이전</Link>}
          <span className="muted num">
            {page + 1} / {lastPage + 1} 페이지
          </span>
          {page < lastPage && <Link href={pageHref(page + 1)}>다음 →</Link>}
        </div>
      )}
    </div>
  );
}
