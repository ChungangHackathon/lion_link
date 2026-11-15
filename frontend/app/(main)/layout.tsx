"use client";

import { usePathname, useRouter } from "next/navigation";
import { useEffect } from "react";
import { Gnb } from "@/components/layout/Gnb";
import { useSession } from "@/lib/session";
import { ChatRealtimeProvider } from "@/lib/api/chatRealtime";

/**
 * Member area: GNB + page. 로그인 전 → /login, 승인 대기 → /pending, 반려 → /rejected,
 * 기본 프로필 미입력 → /onboarding.
 * 예외로 승인 대기 회원은 마이페이지(/me)에 들어올 수 있다. 승인을 기다리는 동안 프로필을 채우는 길이다.
 */
export default function MainLayout({ children }: { children: React.ReactNode }) {
  const { session, ready } = useSession();
  const router = useRouter();
  const pathname = usePathname();
  // 마이페이지·관리자 화면은 흰 바탕, 나머지는 연한 회색 바탕
  const white = pathname.startsWith("/me") || pathname.startsWith("/admin");
  // 프로필 API는 승인 전에도 열려 있다(@AllowPendingMember). 하위 화면은 승인 후에만 쓸 수 있어 /me 하나만 연다
  const pendingProfile = session.status === "PENDING" && pathname === "/me";

  const allowed = ready && session.loggedIn && session.profileCompleted && (session.status === "APPROVED" || pendingProfile);

  useEffect(() => {
    if (!ready) return;
    if (!session.loggedIn) router.replace("/login");
    else if (session.status === "REJECTED") router.replace("/rejected");
    else if (!session.profileCompleted) router.replace("/onboarding");
    else if (session.status === "PENDING" && pathname !== "/me") router.replace("/pending");
  }, [ready, session, router, pathname]);

  // 로그인 확인 중이거나 다른 화면으로 보내는 중에는 내용을 그리지 않는다
  if (!allowed) return <div className="flex flex-1 bg-bg-50" aria-busy="true" />;

  return (
    <ChatRealtimeProvider enabled={allowed && !pendingProfile}>
      <div className={`flex flex-1 flex-col ${white ? "bg-white" : "bg-bg-50"}`}>
        <Gnb />
        <main className="container-page flex flex-1 flex-col py-8">{children}</main>
      </div>
    </ChatRealtimeProvider>
  );
}
