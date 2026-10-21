'use client'; // error.tsx는 반드시 Client Component다 (reset 버튼을 눌러야 하므로)

// 페이지에서 처리하지 못한 에러가 나면 이 화면이 대신 보인다
// 운영 빌드에서는 Next가 서버 에러 메시지를 숨기고 digest(로그 추적용 번호)만 넘기므로 고정 문구를 쓴다
// 사용자에게 보여 줄 에러(404, 결제 불가 등)는 페이지나 컴포넌트에서 ApiError로 직접 처리한다
export default function Error({ error, reset }: { error: Error & { digest?: string }; reset: () => void }) {
  return (
    <div>
      <h1>문제가 생겼습니다</h1>
      <p>잠시 뒤 다시 시도해 주세요.</p>
      {error.digest && <p>오류 번호: {error.digest}</p>}
      <button type="button" onClick={reset}>
        다시 시도
      </button>
    </div>
  );
}
