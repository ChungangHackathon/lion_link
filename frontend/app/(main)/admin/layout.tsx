"use client";

import { useSession } from "@/lib/session";
import { useStore } from "@/lib/store";
import { SideNav } from "@/components/layout/SideNav";
import { Button } from "@/components/ui/Button";

/** 관리자 페이지 shell (SideNav 관리자). 회원이면 ◇ C-03 403. */
export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const { session } = useSession();
  const { signups } = useStore();
  if (session.role !== "ADMIN") {
    return (
      <div className="flex flex-1 flex-col items-center justify-center gap-4 py-32 text-center">
        <p className="text-[64px] font-extrabold text-line-300">403</p>
        <h1 className="text-[22px] font-bold text-ink-900">접근 권한이 없어요</h1>
        <p className="t-body2 text-ink-500">관리자만 들어올 수 있는 페이지예요.</p>
        <Button href="/home">홈으로</Button>
      </div>
    );
  }
  const pending = signups.filter((s) => s.status === "PENDING").length;
  return (
    <div className="grid grid-cols-[240px_1fr] gap-10">
      <SideNav
        title="관리자 페이지"
        items={[
          { href: "/admin", label: "가입 승인", count: pending },
          { href: "/admin/posts", label: "게시글 관리" },
        ]}
      />
      <div className="min-w-0">{children}</div>
    </div>
  );
}
