import type { Metadata } from 'next';
import { Geist, Geist_Mono } from 'next/font/google';
import Link from 'next/link';
import { SidebarNav } from '@/components/SidebarNav';
import { SimMark } from '@/components/SimMark';
import { UserIdForm } from '@/components/UserIdForm';
import { getUserId } from '@/lib/api-server';
import { Providers } from './providers';
import './globals.css';

export const metadata: Metadata = { title: 'ROAMPASS' };

// 글꼴: 본문은 Geist 하나, 주문번호·SIM 번호처럼 자리를 맞춰 읽는 번호만 같은 폭 글꼴(Geist Mono)
// next/font가 빌드할 때 받아서 우리 서버가 직접 준다 → 화면이 Google에 요청하지 않는다
const geist = Geist({ subsets: ['latin'], weight: ['400', '500', '700'], variable: '--font-geist' });
const geistMono = Geist_Mono({ subsets: ['latin'], weight: ['400', '500'], variable: '--font-geist-mono' });

// 모든 화면의 틀: 왼쪽 메뉴(서비스 표시, 내 아이디, 메뉴) + 오른쪽 작업 영역
// Server Component다. 쿠키의 아이디를 읽어 아이디 입력칸에 넘긴다
export default async function RootLayout({ children }: { children: React.ReactNode }) {
  const userId = await getUserId();

  return (
    <html lang="ko" className={`${geist.variable} ${geistMono.variable}`}>
      <body>
        <div className="shell">
          <SidebarNav
            brand={
              <Link href="/" className="brand">
                <SimMark />
                ROAMPASS
              </Link>
            }
          >
            <UserIdForm currentUserId={userId} />
          </SidebarNav>
          <main className="canvas">
            <Providers>{children}</Providers>
          </main>
        </div>
      </body>
    </html>
  );
}
