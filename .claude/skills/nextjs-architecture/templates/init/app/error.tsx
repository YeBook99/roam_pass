'use client'; // error.tsx는 반드시 Client Component다 (reset 버튼을 눌러야 하므로)

// 페이지에서 처리하지 못한 에러가 나면 이 화면이 대신 보인다
export default function Error({ error, reset }: { error: Error; reset: () => void }) {
  return (
    <div>
      <h1>문제가 생겼습니다</h1>
      <p>{error.message}</p>
      <button type="button" onClick={reset}>
        다시 시도
      </button>
    </div>
  );
}
