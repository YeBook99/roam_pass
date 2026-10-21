'use client'; // 버튼을 눌러 코드를 펼치는 상태(useState)가 있어서 브라우저에서 돈다

import { useMutation } from '@tanstack/react-query';
import { useRouter } from 'next/navigation';
import { useState } from 'react';
import { viewQr } from '../client';

// 동작: 설치 코드는 처음에 가려 둔다. [설치 코드 보기]를 누르면
// 1) 백엔드에 "QR을 봤다"를 기록하고(ISSUED → QR_VIEWED) 2) 코드를 펼치고 3) router.refresh()로 위쪽 설치 상태를 다시 그린다
// QR 이미지는 아직 그리지 않는다. 폰 설정 > eSIM 추가 > "직접 입력"에 이 코드를 넣어도 설치된다
export function QrReveal({ orderNo, activationCode }: { orderNo: string; activationCode: string }) {
  const [shown, setShown] = useState(false);
  const router = useRouter();

  const view = useMutation({
    mutationFn: () => viewQr(orderNo),
    onSuccess: () => {
      setShown(true);
      router.refresh();
    },
  });

  if (shown) {
    return (
      <div className="stack">
        <p>설치 코드 (폰 설정에서 직접 입력):</p>
        <code className="code">{activationCode}</code>
      </div>
    );
  }
  return (
    <div className="stack">
      <button type="button" disabled={view.isPending} onClick={() => view.mutate()}>
        {view.isPending ? '불러오는 중...' : '설치 코드 보기'}
      </button>
      {view.isError && <p className="error">{view.error.message}</p>}
    </div>
  );
}
