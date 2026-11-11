"use client";

import { useState } from "react";
import { cn } from "@/lib/cn";
import { useStore } from "@/lib/store";
import { timeAgo } from "@/lib/format";
import type { NotificationTab } from "@/lib/types";
import { Button } from "@/components/ui/Button";
import { Icon } from "@/components/ui/Icon";
import { Tabs } from "@/components/ui/Misc";

/** Dropdown/Notifications (H-07) — tabs 전체·커피챗·채팅·커뮤니티, quick accept/decline on coffee requests (→ D-10 / D-11). */
export function NotificationDropdown({ onNavigate }: { onNavigate: (href: string) => void }) {
  const [tab, setTab] = useState<NotificationTab>("ALL");
  const { notifications, alumni, coffeeChats, markNotificationsRead } = useStore();

  const count = (t: NotificationTab) => notifications.filter((n) => !n.read && (t === "ALL" || n.tab === t)).length;
  const list = notifications.filter((n) => tab === "ALL" || n.tab === tab);

  const go = (id: string, href: string) => {
    markNotificationsRead([id]);
    onNavigate(href);
  };

  return (
    <div className="flex max-h-[637px] flex-col px-2 pb-2">
      <div className="flex items-center justify-between py-2 pr-3">
        <p className="t-body-bold text-ink-900">알림</p>
        <button type="button" className="t-caption1 text-ink-500 hover:underline" onClick={() => markNotificationsRead()}>
          모두 읽음
        </button>
      </div>
      <Tabs
        variant="pill"
        value={tab}
        onChange={setTab}
        items={[
          { value: "ALL", label: "전체" },
          { value: "COFFEE", label: "커피챗", count: count("COFFEE") },
          { value: "CHAT", label: "채팅", count: count("CHAT") },
          { value: "COMMUNITY", label: "커뮤니티", count: count("COMMUNITY") },
        ]}
        className="mb-5"
      />
      <div className="flex flex-col overflow-y-auto rounded-[10px] border border-light-layout">
        {list.length === 0 && <p className="py-6 text-center text-[14px] text-ink-500">새 알림이 없어요</p>}
        {list.map((n) => {
          const actor = n.actorId ? alumni.find((a) => a.id === n.actorId) : undefined;
          const stillPending = n.quick === "coffee-request" && coffeeChats.find((c) => c.id === n.targetId)?.status === "PENDING";
          return (
            <div key={n.id} className={cn("flex items-center gap-[15px] border-b border-light-layout px-3 py-[18px] last:border-b-0", !n.read ? "bg-light-card" : "bg-white")}>
              <span className={cn("grid size-[38px] shrink-0 place-items-center rounded-full bg-white", n.read && "border border-light-layout")}>
                <Icon name={n.tab === "COFFEE" ? "coffee" : n.tab === "CHAT" ? "chat" : "user"} size={18} />
              </span>
              <div className="flex min-w-0 flex-1 flex-col gap-2">
                <button type="button" onClick={() => go(n.id, n.href)} className="text-left text-[16px] leading-6 tracking-[-0.01em] text-ink-900">
                  <span className="font-extrabold">{actor?.name}</span>
                  <span className="font-semibold">{n.text}</span>
                </button>
                {stillPending && (
                  <div className="flex gap-1.5">
                    <Button size="S" className="h-[25px] w-[60px] px-0 text-[12px]" onClick={() => go(n.id, `${n.href}?action=accept`)}>
                      수락하기
                    </Button>
                    <Button size="S" variant="secondary" className="h-[25px] w-[60px] px-0 text-[12px]" onClick={() => go(n.id, `${n.href}?action=reject`)}>
                      거절하기
                    </Button>
                  </div>
                )}
              </div>
              <span className="shrink-0 text-[12px] text-ink-400">{timeAgo(n.at)}</span>
            </div>
          );
        })}
      </div>
    </div>
  );
}
