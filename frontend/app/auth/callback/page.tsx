"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { landingPath } from "@/lib/api/auth";
import { useSession } from "@/lib/session";

/**
 * 구글 로그인 콜백 (AUTH-01 → AUTH-03 → AUTH-05).
 * 서버가 refreshToken 쿠키를 심고 이 주소로 보내면, SessionProvider가 accessToken을 받고
 * GET /me까지 불러온다. 여기서는 그 결과로 화면만 나눈다.
 */
export default function AuthCallbackPage() {
  const router = useRouter();
  const { ready, me, session } = useSession();

  useEffect(() => {
    if (!ready) return;
    if (me) router.replace(landingPath(me));
    else if (session.loggedIn) router.replace("/home"); // 프로토타입 모드
    else router.replace("/login?error=SESSION_EXPIRED");
  }, [ready, me, session.loggedIn, router]);

  return (
    <div className="flex flex-1 items-center justify-center py-32" aria-busy="true">
      <p className="t-body2 text-ink-500">로그인하는 중이에요…</p>
    </div>
  );
}
