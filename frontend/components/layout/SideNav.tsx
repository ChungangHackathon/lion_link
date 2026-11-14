"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/cn";
import { CountBadge } from "@/components/ui/Badge";

interface NavItem {
  href: string;
  label: string;
  count?: number;
}

/** SideNav (90:972) — 마이페이지 / 관리자 페이지 */
export function SideNav({ title, items, className }: { title: string; items: NavItem[]; className?: string }) {
  const pathname = usePathname();
  return (
    <nav className={cn("flex w-[240px] shrink-0 flex-col gap-1", className)} aria-label={title}>
      <p className="t-label1 px-0 pb-1 text-ink-500">{title}</p>
      {items.map((it) => {
        const active = pathname === it.href || (it.href !== "/me" && it.href !== "/admin" && pathname.startsWith(it.href));
        return (
          <Link key={it.href} href={it.href} className={cn("flex items-center justify-between rounded-[6px] px-3 py-2.5 text-[15px] hover:bg-bg-100", active ? "bg-bg-100 font-semibold text-ink-900" : "text-ink-700")}>
            {it.label}
            {it.count !== undefined && <CountBadge count={it.count} />}
          </Link>
        );
      })}
    </nav>
  );
}

export const MY_NAV: NavItem[] = [
  { href: "/me", label: "내 프로필" },
  { href: "/me/coffee-settings", label: "커피챗 설정" },
  { href: "/me/notifications", label: "알림 설정" },
  { href: "/me/bookmarks", label: "관심 동문" },
  { href: "/me/settings", label: "설정" },
];
