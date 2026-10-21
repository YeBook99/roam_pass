'use client';

import { useEffect, useState } from 'react';

// "12초 전 데이터"를 보여 준다. 지금 시각에 따라 바뀌므로 브라우저에서 화면이 뜬 뒤 계산한다 (docs/08 §5)
export function FreshnessBadge({ fetchedAt }: { fetchedAt: string }) {
  const [seconds, setSeconds] = useState<number | null>(null);

  useEffect(() => {
    const update = () => setSeconds(Math.max(0, Math.floor((Date.now() - Date.parse(fetchedAt)) / 1000)));
    update();
    const timer = setInterval(update, 1000);
    return () => clearInterval(timer); // 화면을 떠나면 타이머를 멈춘다
  }, [fetchedAt]);

  if (seconds === null) return <span className="badge">조회 시각 확인 중</span>;
  return <span className={seconds > 60 ? 'badge badge-stale' : 'badge'}>{seconds}초 전 데이터</span>;
}
