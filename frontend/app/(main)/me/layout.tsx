"use client";

import Link from "next/link";
import { MY_NAV, SideNav } from "@/components/layout/SideNav";
import { Icon } from "@/components/ui/Icon";
import { useSession } from "@/lib/session";

/** 마이페이지 shell: SideNav 240 + content (P-02 · CS-01 · N-01 · X-01 · X-02 · X-03) */
export default function MeLayout({ children }: { children: React.ReactNode }) {
  const { session } = useSession();
  // 승인 대기 중에는 프로필만 쓸 수 있다. 못 가는 메뉴는 감추고 돌아가는 길을 둔다
  const pending = session.status === "PENDING";
  return (
    <div className="grid grid-cols-[240px_1fr] gap-10">
      <div className="flex flex-col gap-1">
        <SideNav title="마이페이지" items={pending ? MY_NAV.filter((item) => item.href === "/me") : MY_NAV} />
        {pending && (
          <Link href="/pending" className="flex items-center gap-1.5 rounded-[6px] px-3 py-2.5 text-[15px] text-ink-700 hover:bg-bg-100">
            <Icon name="back" size={16} />
            승인 대기 화면
          </Link>
        )}
      </div>
      <div className="min-w-0">{children}</div>
    </div>
  );
}
