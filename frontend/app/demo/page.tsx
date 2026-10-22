import { PgScenarioPanel } from '@/features/mock/components/PgScenarioPanel';
import { SupplierStatePanel } from '@/features/mock/components/SupplierStatePanel';
import { getPgSettings } from '@/features/mock/server';
import { DEFAULT_PG_SETTINGS, type MockPgSettings } from '@/features/mock/types';

export const dynamic = 'force-dynamic'; // 설정은 바뀔 때마다 최신 값을 보여 준다

function isDefault(s: MockPgSettings): boolean {
  return (Object.keys(DEFAULT_PG_SETTINGS) as (keyof MockPgSettings)[]).every((k) => s[k] === DEFAULT_PG_SETTINGS[k]);
}

// 시연 설정 화면 (/demo). 이 화면의 질문: "지금 어떤 상황을 일부러 만들어 둘까?"
// 서버가 가짜 결제 회사의 지금 설정을 읽어 그리고, 버튼·입력칸만 브라우저에서 돈다
export default async function DemoPage() {
  const settings = await getPgSettings();
  const changed = !isDefault(settings);

  return (
    <div className="page">
      <div className="title-row">
        <div>
          <div className="row">
            <h1>시연 설정</h1>
            {changed && <span className="pill pill-warning">시연 설정 켜짐</span>}
          </div>
          <p className="subtitle">
            가짜 결제 회사와 가짜 공급사를 일부러 늦게·틀리게 동작시켜, 예외 상황을 화면에서 바로 보여 줍니다.
          </p>
        </div>
      </div>

      <p className="notice">
        이 설정은 모든 사용자에게 바로 적용됩니다. 시연이 끝나면 [정상으로 되돌리기]를 눌러 주세요.
      </p>

      <div className="grid-main-side">
        {/* key: 설정이 바뀌면 폼을 새로 만들어 입력칸도 지금 값으로 맞춘다 */}
        <PgScenarioPanel key={JSON.stringify(settings)} current={settings} />
        <SupplierStatePanel />
      </div>
    </div>
  );
}
