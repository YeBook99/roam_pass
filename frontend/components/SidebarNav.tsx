'use client'; // 지금 주소에 맞춰 메뉴를 표시하고(usePathname), 휴대폰에서 메뉴를 열고 닫는 상태가 있어서 브라우저에서 돈다

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useEffect, useState } from 'react';

const GROUPS = [
  {
    label: '여행자',
    items: [
      { href: '/', label: '요금제', match: (p: string) => p === '/' || p.startsWith('/checkout') },
      { href: '/my-sims', label: '내 SIM', match: (p: string) => p.startsWith('/my-sims') || p.startsWith('/orders') },
    ],
  },
  {
    label: '운영',
    items: [{ href: '/admin', label: '주문', match: (p: string) => p.startsWith('/admin') }],
  },
];

// 동작: 왼쪽 메뉴. 지금 보는 화면의 메뉴 줄에 회색 바탕을 깔아 표시한다(aria-current)
// 휴대폰 폭에서는 메뉴가 접히고 [메뉴] 버튼으로 연다. 다른 화면으로 이동하면 다시 접는다
// brand(서비스 표시)와 children(아이디 입력)은 서버가 만들어서 넘겨준다
export function SidebarNav({ brand, children }: { brand: React.ReactNode; children: React.ReactNode }) {
  const pathname = usePathname();
  const [open, setOpen] = useState(false);

  useEffect(() => setOpen(false), [pathname]); // 이동하면 접는다

  return (
    <aside className="sidebar" data-open={open}>
      <div className="sidebar-top">
        {brand}
        <button type="button" className="menu-button" aria-expanded={open} onClick={() => setOpen(!open)}>
          {open ? '닫기' : '메뉴'}
        </button>
      </div>
      <div className="sidebar-body">
        {children}
        {GROUPS.map((group) => (
          <nav key={group.label} className="nav-group" aria-label={group.label}>
            <p className="nav-label">{group.label}</p>
            {group.items.map((item) => (
              <Link key={item.href} href={item.href} aria-current={item.match(pathname) ? 'page' : undefined}>
                {item.label}
              </Link>
            ))}
          </nav>
        ))}
      </div>
    </aside>
  );
}
