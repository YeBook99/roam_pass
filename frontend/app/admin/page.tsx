import Link from 'next/link';
import { FreshnessBadge } from '@/components/FreshnessBadge';
import { KstTime } from '@/components/KstTime';
import { AdminFilters } from '@/features/admin/components/AdminFilters';
import { RefreshControls } from '@/features/admin/components/RefreshControls';
import { getAdminOrders } from '@/features/admin/server';
import { ADMIN_FILTER_KEYS, type AdminFilters as Filters } from '@/features/admin/types';

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

  const result = await getAdminOrders(filters, page);
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
    <div className="stack">
      <h1>어드민 · 주문 목록</h1>
      {/* key가 바뀌면 컴포넌트를 새로 만든다 → URL이 바뀌면(초기화, 뒤로 가기) 폼 값도 URL과 같아진다 */}
      <AdminFilters key={JSON.stringify(filters)} current={filters} />
      <div className="row">
        <FreshnessBadge fetchedAt={result.fetchedAt} />
        <RefreshControls />
        <span>총 {result.totalElements}건</span>
      </div>

      <table>
        <thead>
          <tr>
            <th>주문번호</th>
            <th>사용자</th>
            <th>종류</th>
            <th>금액</th>
            <th>주문 상태</th>
            <th>결제 상태</th>
            <th>설치 상태</th>
            <th>주문 시각(KST)</th>
          </tr>
        </thead>
        <tbody>
          {result.content.map((order) => (
            <tr key={order.orderNo}>
              <td>
                <Link href={`/admin/orders/${order.orderNo}`}>{order.orderNo}</Link>
              </td>
              <td>{order.userId}</td>
              <td>{order.simType}</td>
              <td>{order.amount.toLocaleString('ko-KR')}원</td>
              <td>{order.orderStatus}</td>
              <td>{order.payStatus ?? '-'}</td>
              <td>{order.installStatus ?? '-'}</td>
              <td>
                <KstTime iso={order.createdAt} />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      {result.content.length === 0 && <p>조건에 맞는 주문이 없습니다.</p>}

      <div className="row">
        {page > 0 && <Link href={pageHref(page - 1)}>← 이전</Link>}
        <span>
          {page + 1} / {lastPage + 1} 페이지
        </span>
        {page < lastPage && <Link href={pageHref(page + 1)}>다음 →</Link>}
      </div>
    </div>
  );
}
