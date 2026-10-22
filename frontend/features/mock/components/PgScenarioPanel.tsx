'use client'; // 버튼과 입력칸이 있어서 브라우저에서 돈다

import { useMutation } from '@tanstack/react-query';
import { useRouter } from 'next/navigation';
import { useState } from 'react';
import { resetPgSettings, updatePgSettings } from '../client';
import { DEFAULT_PG_SETTINGS, SCENARIOS, type MockPgSettings, type Scenario } from '../types';

// 지금 설정이 어떤 상황 버튼과 같은지 찾는다 (처음 값 + 그 상황의 값). 없으면 null = 직접 바꾼 설정
function matchScenario(current: MockPgSettings): Scenario | null {
  return (
    SCENARIOS.find((s) => {
      const expected = { ...DEFAULT_PG_SETTINGS, ...s.settings };
      return (Object.keys(expected) as (keyof MockPgSettings)[]).every((k) => expected[k] === current[k]);
    }) ?? null
  );
}

// 동작: 상황 버튼을 누르면 가짜 결제 회사 설정을 "처음 값 + 그 상황"으로 한 번에 바꾼다(상황끼리 섞이지 않게)
// [정상으로 되돌리기]는 처음 값으로. 세부 설정 폼에서는 값을 직접 바꿔 [저장]
// 바꾸고 나면 router.refresh()로 페이지(서버)가 지금 설정을 다시 읽어 보여 준다
export function PgScenarioPanel({ current }: { current: MockPgSettings }) {
  const router = useRouter();
  const [form, setForm] = useState<MockPgSettings>(current);
  const active = matchScenario(current);

  const apply = useMutation({
    mutationFn: (settings: MockPgSettings) => updatePgSettings(settings),
    onSuccess: () => router.refresh(),
  });
  const reset = useMutation({
    mutationFn: resetPgSettings,
    onSuccess: () => router.refresh(),
  });
  const busy = apply.isPending || reset.isPending;

  function change<K extends keyof MockPgSettings>(key: K, value: MockPgSettings[K]) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  return (
    <div className="stack">
      <section className="card stack">
        <div className="row">
          <h2>결제 상황 고르기</h2>
          <span className="muted">{active ? `지금: ${active.label}` : '지금: 직접 바꾼 설정 또는 정상'}</span>
        </div>
        <div className="scenario-list">
          {SCENARIOS.map((s) => (
            <div key={s.id} className={active?.id === s.id ? 'scenario scenario-on' : 'scenario'}>
              <div className="row">
                <button
                  type="button"
                  disabled={busy}
                  onClick={() => apply.mutate({ ...DEFAULT_PG_SETTINGS, ...s.settings })}
                >
                  {s.label}
                </button>
                {active?.id === s.id && <span className="pill pill-warning">켜짐</span>}
              </div>
              <p>{s.what}</p>
              <p className="muted small">확인: {s.where}</p>
            </div>
          ))}
        </div>
        <button type="button" className="btn-primary" disabled={busy} onClick={() => reset.mutate()}>
          정상으로 되돌리기
        </button>
        {(apply.isError || reset.isError) && <p className="error">{(apply.error ?? reset.error)?.message}</p>}
      </section>

      <section className="card stack">
        <h2>세부 설정</h2>
        <form
          className="filters"
          onSubmit={(e) => {
            e.preventDefault();
            apply.mutate(form);
          }}
        >
          <div className="field">
            <label htmlFor="failApprove" className="field-label">
              결제 거절
            </label>
            <select
              id="failApprove"
              value={form.failApprove ? 'yes' : 'no'}
              onChange={(e) => change('failApprove', e.target.value === 'yes')}
            >
              <option value="no">승인</option>
              <option value="yes">거절</option>
            </select>
          </div>
          <NumberField
            id="responseDelayMs"
            label="응답 지연(밀리초, 3000 넘으면 시간 초과)"
            value={form.responseDelayMs}
            min={0}
            onChange={(v) => change('responseDelayMs', v)}
          />
          <NumberField
            id="webhookCount"
            label="알림 횟수(0 = 누락)"
            value={form.webhookCount}
            min={0}
            onChange={(v) => change('webhookCount', v)}
          />
          <NumberField
            id="webhookDelayMs"
            label="알림 지연(밀리초)"
            value={form.webhookDelayMs}
            min={0}
            onChange={(v) => change('webhookDelayMs', v)}
          />
          <NumberField
            id="webhookAmountDelta"
            label="알림 금액에 더할 값(원)"
            value={form.webhookAmountDelta}
            onChange={(v) => change('webhookAmountDelta', v)}
          />
          <NumberField
            id="cancelFailRate"
            label="취소 실패 확률(0~1)"
            value={form.cancelFailRate}
            min={0}
            max={1}
            step={0.1}
            onChange={(v) => change('cancelFailRate', v)}
          />
          <button type="submit" disabled={busy}>
            설정 저장
          </button>
        </form>
      </section>
    </div>
  );
}

// 숫자 입력칸 하나. 이름은 칸 위에
function NumberField(props: {
  id: string;
  label: string;
  value: number;
  min?: number;
  max?: number;
  step?: number;
  onChange: (value: number) => void;
}) {
  return (
    <div className="field">
      <label htmlFor={props.id} className="field-label">
        {props.label}
      </label>
      <input
        id={props.id}
        type="number"
        value={props.value}
        min={props.min}
        max={props.max}
        step={props.step ?? 1}
        onChange={(e) => props.onChange(Number(e.target.value))}
      />
    </div>
  );
}
