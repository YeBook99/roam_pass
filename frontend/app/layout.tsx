import type { Metadata } from 'next';
import Link from 'next/link';
import { Providers } from './providers';
import './globals.css';

export const metadata: Metadata = { title: 'ROAMPASS' };

// 모든 화면의 틀. Server Component이고, Client Component인 Providers가 children을 감싸는 것은 허용된다
export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ko">
      <body>
        <header className="header">
          <Link href="/">ROAMPASS</Link>
          <nav>
            <Link href="/my-sims">내 SIM</Link>
            <Link href="/admin">어드민</Link>
          </nav>
        </header>
        <main className="main">
          <Providers>{children}</Providers>
        </main>
      </body>
    </html>
  );
}
