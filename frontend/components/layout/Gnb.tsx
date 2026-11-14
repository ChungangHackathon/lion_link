"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { API_ENABLED } from "@/lib/api/client";
import { useApi } from "@/lib/api/useApi";
import { useNotificationService } from "@/lib/api/useNotificationService";
import { useChatService } from "@/lib/api/useChatService";
import { useCoffeeService } from "@/lib/api/useCoffeeService";
import { useChatRealtime, usePersonalEvents } from "@/lib/api/chatRealtime";
import { useToast } from "@/components/ui/Toast";
import { cn } from "@/lib/cn";
import { useSession } from "@/lib/session";
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
  const toast = useToast();
  const [open, setOpen] = useState<"bell" | "me" | null>(null);

  const mode = variant ?? (session.status === "PENDING" ? "pending" : "default");

  // 종 배지: 안 읽은 알림 수 (NOTI-01 unreadCounts.all). 승인 회원만 부를 수 있다
  const notiService = useNotificationService();
  const badgeEnabled = mode === "default" && session.loggedIn && session.status === "APPROVED";
  const badge = useApi((signal) => notiService.list("ALL", signal), [notiService], badgeEnabled);
  const unreadNoti = badgeEnabled ? badge.data?.unreadCounts.all ?? 0 : 0;
  const reloadBadge = badge.reload;
  // 채팅 배지: 안 읽은 메시지 + 받은 메시지 요청 (CHAT-15). 실시간 연결이 있으면 개인 채널 소식에 맞춰 다시 센다
  const chatService = useChatService();
  const chatCounts = useApi((signal) => chatService.unreadCount(signal), [chatService], badgeEnabled);
  const unreadChat = badgeEnabled && chatCounts.data ? chatCounts.data.unreadMessages + chatCounts.data.messageRequests : 0;
  const reloadChat = chatCounts.reload;
  const { connected } = useChatRealtime();
  usePersonalEvents((e) => {
    reloadChat();
    if (e.type === "REQUEST_RECEIVED" && !pathname.startsWith("/chat/requests")) toast("새 메시지 요청이 왔어요");
  });
  // [내 커피챗] 숫자: 내가 받은 대기 중 신청 수 (COFFEE-02 pendingReceivedCount). 화면을 옮길 때마다 다시 센다
  const coffeeService = useCoffeeService();
  const coffeeCounts = useApi((signal) => coffeeService.list("RECEIVED", "PENDING", 0, 1, signal), [coffeeService, pathname], badgeEnabled);
  const pendingCoffee = badgeEnabled ? coffeeCounts.data?.pendingReceivedCount ?? 0 : 0;
  useEffect(() => {
    // 알림은 1분마다 확인한다. 채팅은 실시간 연결이 없을 때만 폴링한다(채팅 화면에서는 더 자주)
    if (!API_ENABLED || !badgeEnabled) return;
    const timer = setInterval(
      () => {
        reloadBadge();
        if (!connected) reloadChat();
      },
      pathname.startsWith("/chat") && !connected ? 10_000 : 60_000,
    );
    return () => clearInterval(timer);
  }, [badgeEnabled, reloadBadge, reloadChat, pathname, connected]);

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
                    onChanged={reloadBadge}
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
