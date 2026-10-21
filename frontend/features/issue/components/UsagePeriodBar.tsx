import { KstTime } from '@/components/KstTime';

const HOUR = 60 * 60 * 1000;
const DAY = 24 * HOUR;

// 이 서비스만의 요소: 사용 기간 막대
// 개통 시각부터 만료 시각(개통 + 일수 × 24시간)까지 중에서 지금까지 지난 부분을 실제 비율대로 채운다
// Server Component다. now는 페이지가 그릴 때의 서버 시각(UTC 기준 밀리초)이라 브라우저 시계와 상관없다
export function UsagePeriodBar({
  activatedAt,
  expiresAt,
  now,
}: {
  activatedAt: string | null;
  expiresAt: string | null;
  now: number;
}) {
  if (!activatedAt || !expiresAt) {
    return <p className="muted">개통 전이에요. 현지에서 데이터를 처음 켜는 순간부터 사용 기간이 시작됩니다.</p>;
  }
  const start = Date.parse(activatedAt);
  const end = Date.parse(expiresAt);
  const ratio = Math.min(1, Math.max(0, (now - start) / (end - start)));
  const leftMs = end - now;
  const left =
    leftMs <= 0 ? '사용 기간 끝남' : `${Math.floor(leftMs / DAY)}일 ${Math.floor((leftMs % DAY) / HOUR)}시간 남음`;

  return (
    <div className="usage">
      <div className="usage-head">
        <span className="muted">사용 기간</span>
        <span className="usage-left">{left}</span>
      </div>
      <div
        className="bar"
        role="progressbar"
        aria-label="사용 기간"
        aria-valuenow={Math.round(ratio * 100)}
        aria-valuemin={0}
        aria-valuemax={100}
      >
        <div className={leftMs <= 0 ? 'bar-fill done' : 'bar-fill'} style={{ width: `${ratio * 100}%` }} />
      </div>
      <div className="usage-ends">
        <span>
          개통 <KstTime iso={activatedAt} />
        </span>
        <span>
          만료 <KstTime iso={expiresAt} />
        </span>
      </div>
    </div>
  );
}
