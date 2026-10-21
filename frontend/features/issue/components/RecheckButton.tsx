'use client'; // 버튼이 있어서 브라우저에서 돈다

import { useMutation } from '@tanstack/react-query';
import { useRouter } from 'next/navigation';
import { refreshProfile } from '../client';

// 동작: [다시 확인]을 누르면 백엔드가 공급사에 지금 상태를 물어본다(설치됨/개통됨)
// 끝나면 router.refresh()로 페이지(서버 컴포넌트)를 다시 그려 바뀐 설치 상태·만료일을 보여 준다
// 이 버튼을 안 눌러도 서버 스케줄러가 1분마다 같은 일을 한다. 버튼은 "지금 바로" 보고 싶을 때 쓴다
export function RecheckButton({ orderNo }: { orderNo: string }) {
  const router = useRouter();
  const recheck = useMutation({
    mutationFn: () => refreshProfile(orderNo),
    onSuccess: () => router.refresh(),
  });

  return (
    <div className="stack">
      <button type="button" disabled={recheck.isPending} onClick={() => recheck.mutate()}>
        {recheck.isPending ? '확인 중...' : '설치 상태 다시 확인'}
      </button>
      {recheck.isError && <p className="error">{recheck.error.message}</p>}
    </div>
  );
}
