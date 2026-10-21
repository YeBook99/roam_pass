import type { Metadata } from 'next';
import { Inter } from 'next/font/google';
import Link from 'next/link';
import { Providers } from './providers';
import './globals.css';

export const metadata: Metadata = { title: 'ROAMPASS' };

// Inter 글꼴. next/font가 빌드할 때 받아서 우리 서버가 직접 준다 → 화면이 Google에 요청하지 않는다
// variable로 CSS 변수(--font-inter)를 만들고, globals.css의 --font가 그 변수를 쓴다
const inter = Inter({ subsets: ['latin'], weight: ['400', '500', '600'], variable: '--font-inter' });

// 모든 화면의 틀. Server Component이고, Client Component인 Providers가 children을 감싸는 것은 허용된다
export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ko" className={inter.variable}>
      <body>
        <header className="header">
          <Link href="/" className="logo">
            ROAMPASS
          </Link>
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
