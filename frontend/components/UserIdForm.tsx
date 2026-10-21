'use client'; // 입력창과 버튼이 있어서 브라우저에서 돈다

import { useRouter } from 'next/navigation';
import { useState } from 'react';
import { saveUserId } from '@/lib/user';

// 동작: 아이디를 입력하고 [저장]을 누르면 쿠키에 저장한 뒤 router.refresh()로 화면을 다시 그린다
// → 서버가 새 쿠키를 읽어 "내 것" 화면과 주문 버튼을 켠다 (로그인 대신, 과제 2.2)
// 왼쪽 메뉴 맨 위(계정 자리)에 있다. currentUserId는 서버가 쿠키에서 읽어 넘겨준 값이다
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
    <form onSubmit={handleSubmit} className="user-form">
      <label htmlFor="user-id" className="field-label">
        내 아이디
      </label>
      <div className="row">
        {/* 아이디는 HTTP 헤더(X-User-Id)로 가는데, 헤더에는 한글을 넣을 수 없어서 영문·숫자·_-만 받는다 */}
        <input
          id="user-id"
          value={userId}
          onChange={(e) => setUserId(e.target.value)}
          placeholder="예: demo"
          maxLength={64}
          pattern="[A-Za-z0-9_-]+"
          title="영문, 숫자, _, -만 쓸 수 있어요"
          required
        />
        <button type="submit">저장</button>
      </div>
      <p className="muted small">
        {currentUserId ? `${currentUserId}(으)로 주문합니다` : '아직 저장한 아이디가 없어요'}
      </p>
    </form>
  );
}
