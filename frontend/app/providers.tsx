'use client';

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useState } from 'react';

// TanStack Query를 쓰려면 화면 전체를 Provider로 한 번 감싸야 한다
// useState로 만들어서 다시 그려져도 같은 QueryClient를 쓴다
export function Providers({ children }: { children: React.ReactNode }) {
  const [client] = useState(() => new QueryClient({ defaultOptions: { queries: { retry: 1 } } }));
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}
