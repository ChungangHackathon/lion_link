import type { NextConfig } from "next";

// 서버 전용 환경변수(NEXT_PUBLIC 아님). 빌드할 때 읽히므로 바꾸면 다시 배포해야 한다.
const BACKEND_ORIGIN = (process.env.BACKEND_ORIGIN ?? "https://lionlink.n-e.kr").replace(/\/+$/, "");

const nextConfig: NextConfig = {
  /* config options here */
  experimental: {
    agentFeedback: true,
  },
  // The UI currently runs on client-side mock data + localStorage session, so
  // static prerendering/cache components are turned off. Re-enable once pages
  // fetch from the backend.
  cacheComponents: false,
  partialPrefetching: false,
  // Safari는 서드파티 쿠키를 막는다. 브라우저가 프론트 도메인만 부르게 하고 백엔드로 넘겨서
  // 인증 쿠키(refreshToken, signupToken, OAuth state 세션)가 프론트 도메인의 퍼스트파티 쿠키가 되게 한다.
  // WebSocket(/ws)은 rewrites가 넘기지 못하므로 NEXT_PUBLIC_WS_URL로 백엔드에 직접 연결한다.
  async rewrites() {
    return [
      { source: "/api/:path*", destination: `${BACKEND_ORIGIN}/api/:path*` },
      { source: "/oauth2/:path*", destination: `${BACKEND_ORIGIN}/oauth2/:path*` },
      { source: "/login/oauth2/:path*", destination: `${BACKEND_ORIGIN}/login/oauth2/:path*` },
    ];
  },
  turbopack: {
    rules: {
      "*.css": {
        loaders: ["@tailwindcss/turbopack"],
        as: "*.css",
      },
    },
  },
};

export default nextConfig;
