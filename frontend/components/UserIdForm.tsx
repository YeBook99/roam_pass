'use client'; // 입력창과 버튼이 있어서 브라우저에서 돈다

import { useRouter } from 'next/navigation';
import { useState } from 'react';
import { saveUserId } from '@/lib/user';

// 동작: 아이디를 입력하고 [저장]을 누르면 쿠키에 저장한 뒤 router.refresh()로 화면을 다시 그린다
// → 서버가 새 쿠키를 읽어 "OOO님" 표시와 주문 폼을 켠다 (로그인 대신, 과제 2.2)
// currentUserId는 서버가 쿠키에서 읽어 넘겨준 값이다 (브라우저에서 쿠키를 다시 읽지 않아도 된다)
export function UserIdForm({ currentUserId }: { currentUserId?: string }) {
  const [userId, setUserId] = useState(currentUserId ?? '');
  const router = useRouter();

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault(); // 폼 기본 동작(페이지 새로 고침)을 막는다
    const trimmed = userId.trim();
    if (!trimmed) return;
    saveUserId(trimmed);
    router.refresh(); // 페이지를 새로 받지 않고 서버 컴포넌트 부분만 다시 그린다
  }

  return (
    <form onSubmit={handleSubmit} className="row">
      <label>
        내 아이디{' '}
        <input value={userId} onChange={(e) => setUserId(e.target.value)} placeholder="예: demo" maxLength={64} />
      </label>
      <button type="submit">저장</button>
      {currentUserId && <span>{currentUserId}님으로 주문합니다</span>}
    </form>
  );
}
