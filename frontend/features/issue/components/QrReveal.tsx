'use client'; // 버튼을 누르는 동작이 있어서 브라우저에서 돈다

import { useMutation } from '@tanstack/react-query';
import { useRouter } from 'next/navigation';
import { viewQr } from '../client';

// 동작: 처음에는 버튼만 있다. [설치 코드 보기]를 누르면
// 1) 백엔드에 "코드를 봤다"를 기록하고(ISSUED → QR_VIEWED) 그 응답으로 설치 코드를 받는다
// 2) 받은 코드를 보여 주고 3) router.refresh()로 위쪽 설치 상태를 다시 그린다
// 코드는 버튼을 누른 뒤의 응답에만 있다. 페이지가 미리 코드를 브라우저로 보내지 않아서 "본 시각"이 정확하다
// QR 이미지는 아직 그리지 않는다. 폰 설정 > eSIM 추가 > "직접 입력"에 이 코드를 넣어도 설치된다
export function QrReveal({ orderNo }: { orderNo: string }) {
  const router = useRouter();
  const view = useMutation({
    mutationFn: () => viewQr(orderNo),
    onSuccess: () => router.refresh(),
  });

  if (view.data) {
    return (
      <div className="stack">
        <p>설치 코드 (폰 설정에서 직접 입력):</p>
        <code className="code">{view.data.activationCode}</code>
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
