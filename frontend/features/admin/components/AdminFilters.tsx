'use client'; // 선택·입력이 있어서 브라우저에서 돈다

import { useRouter } from 'next/navigation';
import { useState } from 'react';
import { ADMIN_FILTER_KEYS, type AdminFilters as Filters } from '../types';

// 각 필터의 선택지. 값은 백엔드 enum 문자열 그대로, 빈 문자열 = 전체
const OPTIONS = {
  orderStatus: ['PENDING', 'EXPIRED', 'PAID', 'ISSUING', 'ISSUED', 'PICKUP_READY', 'ISSUE_FAILED', 'REFUNDING', 'REFUNDED', 'REFUND_FAILED'],
  payStatus: ['READY', 'APPROVED', 'FAILED', 'PARTIAL_CANCELED', 'CANCELED'],
  installStatus: ['ISSUED', 'QR_VIEWED', 'INSTALLED', 'ACTIVE', 'EXPIRED'],
  simType: ['ESIM', 'USIM'],
};

const SELECTS: { key: keyof typeof OPTIONS; label: string }[] = [
  { key: 'orderStatus', label: '주문 상태' },
  { key: 'payStatus', label: '결제 상태' },
  { key: 'installStatus', label: '설치 상태' },
  { key: 'simType', label: 'SIM 종류' },
];

// 동작: 필터를 고르고 [조회]를 누르면 URL만 바꾼다 (/admin?orderStatus=ISSUED&from=2026-10-21)
// 그러면 페이지(Server Component)가 새 URL로 다시 실행되어 서버에서 조회한다
// 이 컴포넌트는 데이터를 직접 가져오지 않는다. 필터 값은 URL 한 곳에만 있다 (ARCHITECTURE.md §7)
export function AdminFilters({ current }: { current: Filters }) {
  const [filters, setFilters] = useState<Filters>(current);
  const router = useRouter();

  function change(key: keyof Filters, value: string) {
    setFilters((prev) => ({ ...prev, [key]: value }));
  }

  function search(e: React.FormEvent) {
    e.preventDefault();
    const query = new URLSearchParams();
    for (const key of ADMIN_FILTER_KEYS) {
      const value = filters[key];
      if (value) query.set(key, value);
    }
    // page를 넣지 않는다 → 필터를 바꾸면 첫 페이지부터 본다
    router.push(`/admin?${query}`);
  }

  return (
    <form onSubmit={search} className="row">
      {SELECTS.map(({ key, label }) => (
        <label key={key}>
          {label}{' '}
          <select value={filters[key] ?? ''} onChange={(e) => change(key, e.target.value)}>
            <option value="">전체</option>
            {OPTIONS[key].map((value) => (
              <option key={value} value={value}>
                {value}
              </option>
            ))}
          </select>
        </label>
      ))}
      {/* 기간은 한국 날짜로 고른다. UTC 범위로 바꾸는 것은 백엔드가 한다 */}
      <label>
        주문일(KST){' '}
        <input type="date" value={filters.from ?? ''} onChange={(e) => change('from', e.target.value)} />
      </label>
      <label>
        ~ <input type="date" value={filters.to ?? ''} onChange={(e) => change('to', e.target.value)} />
      </label>
      <button type="submit">조회</button>
      <button type="button" onClick={() => router.push('/admin')}>
        초기화
      </button>
    </form>
  );
}
