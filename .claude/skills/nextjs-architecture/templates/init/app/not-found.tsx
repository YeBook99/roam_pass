import Link from 'next/link';

// notFound()를 부르거나 없는 URL이면 이 화면이 보인다
export default function NotFound() {
  return (
    <div>
      <h1>찾을 수 없습니다</h1>
      <p>주문번호를 다시 확인해 주세요.</p>
      <Link href="/">처음으로</Link>
    </div>
  );
}
