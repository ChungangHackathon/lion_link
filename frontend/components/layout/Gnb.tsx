"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useState } from "react";
import { cn } from "@/lib/cn";
import { useSession } from "@/lib/session";
import { useStore } from "@/lib/store";
import { ME_ID } from "@/lib/mock/users";
import { Avatar } from "@/components/ui/Avatar";
import { CountBadge } from "@/components/ui/Badge";
import { Icon } from "@/components/ui/Icon";
import { Popover } from "@/components/ui/Modal";
import { Logo } from "./Logo";
import { NotificationDropdown } from "./NotificationDropdown";
import { ProfileMenu } from "./ProfileMenu";

/**
 * GNB (Figma 89:817 · GD Guide 공통 연결):
 * 로고 → 홈 / 동문 찾기 · 커뮤니티 · 내 커피챗 · 채팅 / 🔔 → 알림 드롭다운 / 프로필 → 프로필 메뉴.
 * Variants: 기본 / 승인 대기(메뉴 비활성) / 로고만.
 */
export function Gnb({ variant }: { variant?: "default" | "pending" | "logo" }) {
  const pathname = usePathname();
  const router = useRouter();
  const { session, user } = useSession();
  const { coffeeChats, rooms, notifications } = useStore();
  const [open, setOpen] = useState<"bell" | "me" | null>(null);

  const mode = variant ?? (session.status === "PENDING" ? "pending" : "default");
  const pendingCoffee = coffeeChats.filter((c) => c.receiverId === ME_ID && c.status === "PENDING").length;
  const unreadChat = rooms.reduce((n, r) => n + r.unread, 0);
  const unreadNoti = notifications.filter((n) => !n.read).length;

  const NAV = [
    { href: "/alumni", label: "동문 찾기", count: 0 },
    { href: "/community", label: "커뮤니티", count: 0 },
    { href: "/coffee", label: "내 커피챗", count: pendingCoffee },
    { href: "/chat", label: "채팅", count: unreadChat },
  ];

  return (
    <header className="sticky top-0 z-30 h-16 border-b border-line-200 bg-white">
      <div className="container-page flex h-full items-center">
        <Logo href={mode === "logo" ? "/login" : "/home"} />

        {mode !== "logo" && (
          <>
            <nav className="ml-[72px] flex items-center gap-[41px]" aria-label="주 메뉴">
              {NAV.map((n) => {
                const active = pathname.startsWith(n.href);
                const disabled = mode === "pending";
                return (
                  <Link
                    key={n.href}
                    href={disabled ? "#" : n.href}
                    aria-disabled={disabled}
                    onClick={(e) => disabled && e.preventDefault()}
                    className={cn("relative flex h-16 items-center gap-1 text-[16px] leading-6 tracking-[-0.01em]", disabled ? "cursor-not-allowed font-medium text-ink-400" : active ? "font-extrabold text-navy" : "font-semibold text-navy hover:opacity-80")}
                  >
                    {n.label}
                    {!disabled && <CountBadge count={n.count} className="-ml-0.5 -mt-[14px]" />}
                    {active && !disabled && <span className="absolute inset-x-0 bottom-px h-0.5 bg-navy" />}
                  </Link>
                );
              })}
            </nav>

            <div className="ml-auto flex items-center gap-[5px]">
              <div className="relative">
                <button type="button" aria-label="알림" disabled={mode === "pending"} onClick={() => setOpen(open === "bell" ? null : "bell")} className={cn("relative grid size-10 place-items-center rounded-full hover:bg-bg-100", mode === "pending" && "opacity-60")}>
                  <Icon name="bell" size={24} className="text-ink-700" />
                  {mode !== "pending" && <CountBadge count={unreadNoti} className="absolute left-[22px] top-[4px]" />}
                </button>
                <Popover open={open === "bell"} onClose={() => setOpen(null)} className="w-[380px]">
                  <NotificationDropdown
                    onNavigate={(href) => {
                      setOpen(null);
                      router.push(href);
                    }}
                  />
                </Popover>
              </div>

              <div className="relative">
                <button type="button" aria-label="프로필 메뉴" onClick={() => setOpen(open === "me" ? null : "me")} className="flex items-center gap-1 rounded-full hover:bg-bg-100">
                  {mode === "pending" ? <Avatar size={32} /> : <Avatar name={user?.name} part={user?.part} size={40} />}
                  <Icon name="chevron-down" size={16} className="text-ink-700" />
                </button>
                <Popover open={open === "me"} onClose={() => setOpen(null)} className="w-[240px]">
                  <ProfileMenu onNavigate={() => setOpen(null)} />
                </Popover>
              </div>
            </div>
          </>
        )}
      </div>
    </header>
  );
}
