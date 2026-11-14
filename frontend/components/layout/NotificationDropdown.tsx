"use client";

import { useState } from "react";
import { cn } from "@/lib/cn";
import { useStore } from "@/lib/store";
import { timeAgo } from "@/lib/format";
import type { AppNotification } from "@/lib/types";
import { Button } from "@/components/ui/Button";
import { Icon, type IconName } from "@/components/ui/Icon";

type Tab = "ALL" | "COFFEE_CHAT" | "COMMUNITY";
const inTab = (n: AppNotification, t: Tab) => t === "ALL" || (t === "COMMUNITY" ? n.tab === "COMMUNITY" : n.tab !== "COMMUNITY");

/** ★[O] H-07 알림 드롭다운 — 탭 전체 · 커피챗·채팅 · 커뮤니티. 커피챗 신청은 [수락하기]/[거절하기] 바로 처리 (→ D-10 / D-11). */
export function NotificationDropdown({ onNavigate }: { onNavigate: (href: string) => void }) {
  const [tab, setTab] = useState<Tab>("ALL");
  const { notifications, alumni, coffeeChats, markNotificationsRead } = useStore();

  const count = (t: Tab) => notifications.filter((n) => !n.read && inTab(n, t)).length;
  const list = notifications.filter((n) => inTab(n, tab));

  const go = (id: string, href: string) => {
    markNotificationsRead([id]);
    onNavigate(href);
  };

  return (
    <div className="flex max-h-[644px] w-[380px] flex-col p-2">
      <div className="flex items-center justify-between px-2 py-1">
        <p className="t-label2 text-ink-900">알림</p>
        <button type="button" className="text-[12px] text-ink-500 underline underline-offset-2 hover:text-ink-900" onClick={() => markNotificationsRead()}>
          모두 읽음
        </button>
      </div>
      <div role="tablist" className="mb-1 flex gap-4 border-b border-line-200 px-1">
        {(
          [
            ["ALL", "전체"],
            ["COFFEE_CHAT", "커피챗·채팅"],
            ["COMMUNITY", "커뮤니티"],
          ] as const
        ).map(([v, label]) => (
          <button key={v} type="button" role="tab" aria-selected={tab === v} onClick={() => setTab(v)} className={cn("-mb-px flex items-center gap-1 border-b-2 px-1 pb-1.5 pt-1 t-label2", tab === v ? "border-ink-900 text-ink-900" : "border-transparent font-medium text-ink-500 hover:text-ink-900")}>
            {label}
            {v !== "ALL" && count(v) > 0 && <span className="grid h-4 min-w-4 place-items-center rounded-full bg-orange px-1 text-[10px] font-bold leading-none text-white">{count(v)}</span>}
          </button>
        ))}
      </div>
      <div className="flex flex-col gap-0.5 overflow-y-auto">
        {list.length === 0 && (
          <div className="flex flex-col items-center gap-2 py-10 text-center">
            <Icon name="bell-off" size={28} className="text-ink-400" />
            <p className="text-[13px] text-ink-500">새 알림이 없어요</p>
          </div>
        )}
        {list.map((n) => {
          const actor = n.actorId ? alumni.find((a) => a.id === n.actorId) : undefined;
          const chat = n.quick === "coffee-request" ? coffeeChats.find((c) => c.id === n.targetId) : undefined;
          const handled = !!chat && chat.status !== "PENDING";
          const icon: IconName = n.tab === "COFFEE" ? "coffee" : n.tab === "CHAT" ? "chat" : n.text.includes("번개") ? "calendar" : "users";
          return (
            <div key={n.id} className={cn("relative flex gap-2.5 rounded-[6px] px-2 py-2.5", !n.read && "bg-bg-50")}>
              <Icon name={icon} size={18} className={cn("mt-0.5", n.read ? "text-ink-500" : "text-ink-900")} />
              <div className="flex min-w-0 flex-1 flex-col gap-1">
                <button type="button" onClick={() => go(n.id, n.href)} className={cn("text-left text-[13px] leading-[18px] hover:underline", n.read ? "text-ink-500" : "font-semibold text-ink-900")}>
                  {actor?.name}
                  {n.text}
                </button>
                <p suppressHydrationWarning className="flex items-center gap-2 text-[11px] text-ink-400">
                  {timeAgo(n.at)}
                  {handled && <span className="rounded-[4px] bg-bg-100 px-1.5 py-0.5 text-[11px] font-semibold text-ink-700">처리됨</span>}
                </p>
                {chat && !handled && (
                  <div className="flex gap-1.5 pt-1">
                    <Button size="S" onClick={() => go(n.id, `${n.href}&action=accept`)}>
                      수락하기
                    </Button>
                    <Button size="S" variant="secondary" onClick={() => go(n.id, `${n.href}&action=reject`)}>
                      거절하기
                    </Button>
                  </div>
                )}
                {n.href === "/chat/requests" && !n.read && (
                  <div className="pt-1">
                    <Button size="S" variant="secondary" onClick={() => go(n.id, n.href)}>
                      요청 보기
                    </Button>
                  </div>
                )}
              </div>
              {!n.read && <span aria-label="안 읽음" className="mt-1.5 size-1.5 shrink-0 rounded-full bg-red" />}
            </div>
          );
        })}
      </div>
    </div>
  );
}
