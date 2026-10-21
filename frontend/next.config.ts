import type { NextConfig } from 'next';

// 브라우저가 부르는 /api/*, /mock/*를 Spring으로 넘긴다. 같은 도메인이 되어 CORS 설정이 필요 없다
// 주의: rewrites 주소는 `npm run build` 때 정해진다. Docker 빌드에서는 API_INTERNAL_URL을 build arg로 넘긴다
const apiUrl = process.env.API_INTERNAL_URL ?? 'http://localhost:8080';

const nextConfig: NextConfig = {
  output: 'standalone', // Docker 이미지에 node server.js 하나로 돌 수 있는 결과물을 만든다
  async rewrites() {
    return [
      { source: '/api/:path*', destination: `${apiUrl}/api/:path*` },
      { source: '/mock/:path*', destination: `${apiUrl}/mock/:path*` }, // 시연용 모의 PG·공급사 설정
    ];
  },
};

export default nextConfig;
