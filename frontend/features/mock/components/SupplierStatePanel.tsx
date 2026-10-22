'use client'; // 입력칸과 버튼이 있어서 브라우저에서 돈다

import { useMutation } from '@tanstack/react-query';
import { useRouter } from 'next/navigation';
import { useState } from 'react';
import { changeSupplierState, findIccid } from '../client';
import { EXAMPLE_ACTIVATED_AT, type SupplierStateRequest } from '../types';

const ACTIONS: { label: string; request: SupplierStateRequest }[] = [
  { label: '설치됨으로', request: { state: 'INSTALLED' } },
  { label: '개통됨으로 (지금)', request: { state: 'ENABLED' } },
  { label: '개통됨으로 (과제 예시 시각)', request: { state: 'ENABLED', activatedAt: EXAMPLE_ACTIVATED_AT } },
];

// 동작: 가짜 공급사에서 SIM 상태를 "설치됨" 또는 "개통됨"으로 바꾼다 (사용자가 폰에 설치·개통한 것을 흉내)
// 우리 서버는 1분마다 공급사에 물어보거나, 내 SIM 화면의 [설치 상태 다시 확인]을 누르면 반영한다
// iccid를 주면(운영 주문 상세) 바로 버튼이 나오고, 없으면(시연 화면) 주문번호로 SIM 번호를 먼저 찾는다
export function SupplierStatePanel({ iccid: givenIccid }: { iccid?: string }) {
  const router = useRouter();
  const [orderNo, setOrderNo] = useState('');
  const [iccid, setIccid] = useState<string | null>(givenIccid ?? null);
  const [done, setDone] = useState<string | null>(null);

  const find = useMutation({
    mutationFn: () => findIccid(orderNo.trim()),
    onSuccess: (found) => {
      setIccid(found);
      setDone(found ? null : '이 주문은 아직 SIM이 발급되지 않았습니다');
    },
  });
  const change = useMutation({
    mutationFn: (request: SupplierStateRequest) => changeSupplierState(iccid!, request),
    onSuccess: (profile) => {
      setDone(
        `공급사 기준 상태가 ${profile.state === 'ENABLED' ? '개통됨' : '설치됨'}으로 바뀌었습니다. [설치 상태 다시 확인]이나 1분 뒤 반영됩니다`,
      );
      router.refresh();
    },
  });

  return (
    <section className="card stack">
      <h2>공급사 SIM 상태 바꾸기</h2>
      {!givenIccid && (
        <form
          className="row"
          onSubmit={(e) => {
            e.preventDefault();
            find.mutate();
          }}
        >
          <label htmlFor="demo-order-no" className="field-label">
            주문번호
          </label>
          <input
            id="demo-order-no"
            value={orderNo}
            onChange={(e) => setOrderNo(e.target.value)}
            placeholder="예: RP4Q3QRTURJ87F"
            required
          />
          <button type="submit" disabled={find.isPending}>
            SIM 찾기
          </button>
        </form>
      )}
      {iccid && (
        <>
          <p className="muted">
            SIM 번호 <span className="mono">{iccid}</span>
          </p>
          <div className="row">
            {ACTIONS.map((a) => (
              <button key={a.label} type="button" disabled={change.isPending} onClick={() => change.mutate(a.request)}>
                {a.label}
              </button>
            ))}
          </div>
          <p className="muted small">
            과제 예시 시각 = 2026-05-01 15:20 UTC (7일 요금제면 한국 시각 2026-05-09 00:20 만료)
          </p>
        </>
      )}
      {done && <p>{done}</p>}
      {(find.isError || change.isError) && <p className="error">{(find.error ?? change.error)?.message}</p>}
    </section>
  );
}
