"use client";

import { usePathname, useRouter } from "next/navigation";
import { useEffect } from "react";
import { Gnb } from "@/components/layout/Gnb";
import { useSession } from "@/lib/session";

/**
 * Member area: GNB + page. 로그인 전 → /login, 승인 대기 → /pending, 반려 → /rejected,
 * 기본 프로필 미입력 → /onboarding.
 */
export default function MainLayout({ children }: { children: React.ReactNode }) {
  const { session, ready } = useSession();
  const router = useRouter();
  const pathname = usePathname();
  // 마이페이지·관리자 화면은 흰 바탕, 나머지는 연한 회색 바탕
  const white = pathname.startsWith("/me") || pathname.startsWith("/admin");

  const allowed = ready && session.loggedIn && session.status === "APPROVED" && session.profileCompleted;

  useEffect(() => {
    if (!ready) return;
    if (!session.loggedIn) router.replace("/login");
    else if (session.status === "REJECTED") router.replace("/rejected");
    else if (!session.profileCompleted) router.replace("/onboarding");
    else if (session.status === "PENDING") router.replace("/pending");
  }, [ready, session, router]);

  // 로그인 확인 중이거나 다른 화면으로 보내는 중에는 내용을 그리지 않는다
  if (!allowed) return <div className="flex flex-1 bg-bg-50" aria-busy="true" />;

  return (
    <div className={`flex flex-1 flex-col ${white ? "bg-white" : "bg-bg-50"}`}>
      <Gnb />
      <main className="container-page flex flex-1 flex-col py-8">{children}</main>
    </div>
  );
}
