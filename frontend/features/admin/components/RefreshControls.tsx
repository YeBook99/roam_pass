'use client'; // 버튼과 타이머가 있어서 브라우저에서 돈다

import { useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';

const AUTO_REFRESH_MS = 10_000;

// 동작: [새로고침]을 누르거나 자동 새로고침을 켜 두면 router.refresh()를 부른다
// router.refresh()는 페이지 전체를 새로 받지 않고, 이 페이지의 Server Component만 서버에서 다시 실행해 최신 데이터로 갈아 끼운다
// 그러면 응답의 fetchedAt도 새로 오므로 옆의 "N초 전 데이터"가 0초부터 다시 센다 (docs/08 §5)
export function RefreshControls() {
  const router = useRouter();
  const [auto, setAuto] = useState(false);

  useEffect(() => {
    if (!auto) return;
    const timer = setInterval(() => router.refresh(), AUTO_REFRESH_MS);
    return () => clearInterval(timer); // 끄거나 화면을 떠나면 타이머를 지운다
  }, [auto, router]);

  return (
    <div className="row">
      <button type="button" onClick={() => router.refresh()}>
        새로고침
      </button>
      <label>
        <input type="checkbox" checked={auto} onChange={(e) => setAuto(e.target.checked)} /> 10초마다 자동 새로고침
      </label>
    </div>
  );
}
