"use client";

import { usePathname, useRouter } from "next/navigation";
import { useEffect } from "react";
import { Gnb } from "@/components/layout/Gnb";
import { useSession } from "@/lib/session";

/** Member area: GNB + page. Pending members are sent to /pending, logged-out to /login. */
export default function MainLayout({ children }: { children: React.ReactNode }) {
  const { session } = useSession();
  const router = useRouter();
  const pathname = usePathname();
  // 마이페이지·관리자 화면은 흰 바탕, 나머지는 연한 회색 바탕
  const white = pathname.startsWith("/me") || pathname.startsWith("/admin");

  useEffect(() => {
    if (!session.loggedIn) router.replace("/login");
    else if (session.status === "PENDING") router.replace("/pending");
    else if (session.status === "REJECTED") router.replace("/rejected");
  }, [session, router]);

  return (
    <div className={`flex flex-1 flex-col ${white ? "bg-white" : "bg-bg-50"}`}>
      <Gnb />
      <main className="container-page flex flex-1 flex-col py-8">{children}</main>
    </div>
  );
}
