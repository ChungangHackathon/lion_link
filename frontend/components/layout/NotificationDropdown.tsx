"use client";

import { useState } from "react";
import { cn } from "@/lib/cn";
import { timeAgo } from "@/lib/format";
import { errorMessage } from "@/lib/api/client";
import { notificationHref, type NotificationItem, type NotificationList } from "@/lib/api/notifications";
import { useApi } from "@/lib/api/useApi";
import { useNotificationService, type NotificationService } from "@/lib/api/useNotificationService";
import { Button } from "@/components/ui/Button";
import { Icon, type IconName } from "@/components/ui/Icon";
import { Skeleton } from "@/components/ui/Misc";
import { useToast } from "@/components/ui/Toast";

/** 드롭다운 탭. '커피챗·채팅'은 API의 COFFEE_CHAT · CHAT 두 탭을 합쳐 보여준다. 계정 알림은 '전체'에서만 보인다 */
type Tab = "ALL" | "COFFEE_CHAT" | "COMMUNITY";
const TABS: [Tab, string][] = [
  ["ALL", "전체"],
  ["COFFEE_CHAT", "커피챗·채팅"],
  ["COMMUNITY", "커뮤니티"],
];

async function load(service: NotificationService, tab: Tab, signal: AbortSignal): Promise<NotificationList> {
  if (tab !== "COFFEE_CHAT") return service.list(tab, signal);
  const [coffee, chat] = await Promise.all([service.list("COFFEE_CHAT", signal), service.list("CHAT", signal)]);
  const items = [...coffee.items, ...chat.items].sort((a, b) => b.createdAt.localeCompare(a.createdAt)).slice(0, 20);
  return { items, unreadCounts: chat.unreadCounts };
}

function iconOf(n: NotificationItem): IconName {
  if (n.tab === "COFFEE_CHAT") return "coffee";
  if (n.tab === "CHAT") return "chat";
  if (n.tab === "ACCOUNT") return "user";
  return n.type.startsWith("MEETUP") ? "calendar" : "users";
}

/** ★[O] H-07 알림 드롭다운 — 탭 전체 · 커피챗·채팅 · 커뮤니티. 커피챗 신청은 [수락하기]/[거절하기]로 바로 이동 (→ D-10 / D-11). (NOTI-01 ~ 03) */
export function NotificationDropdown({ onNavigate, onChanged }: { onNavigate: (href: string) => void; onChanged?: () => void }) {
  const toast = useToast();
  const service = useNotificationService();
  const [tab, setTab] = useState<Tab>("ALL");
  const { data, error, reload, setData } = useApi((signal) => load(service, tab, signal), [service, tab]);
  const counts = data?.unreadCounts;
  const count = (t: Tab) => (!counts ? 0 : t === "COFFEE_CHAT" ? counts.coffeeChat + counts.chat : t === "COMMUNITY" ? counts.community : counts.all);

  const go = async (n: NotificationItem, href = notificationHref(n)) => {
    if (!n.read) {
      // 읽음 처리가 실패해도 이동은 한다
      await service.read(n.id).catch(() => undefined);
      onChanged?.();
    }
    onNavigate(href);
  };

  const readAll = async () => {
    try {
      await service.readAll();
      setData((d) => d && { items: d.items.map((x) => ({ ...x, read: true })), unreadCounts: { all: 0, coffeeChat: 0, chat: 0, community: 0 } });
      onChanged?.();
    } catch (e) {
      toast(errorMessage(e), { kind: "error" });
    }
  };

  return (
    <div className="flex max-h-[644px] w-[380px] flex-col p-2">
      <div className="flex items-center justify-between px-2 py-1">
        <p className="t-label2 text-ink-900">알림</p>
        <button type="button" disabled={!counts?.all} className="text-[12px] text-ink-500 underline underline-offset-2 hover:text-ink-900 disabled:no-underline disabled:opacity-50" onClick={() => void readAll()}>
          모두 읽음
        </button>
      </div>
      <div role="tablist" className="mb-1 flex gap-4 border-b border-line-200 px-1">
        {TABS.map(([v, label]) => (
          <button key={v} type="button" role="tab" aria-selected={tab === v} onClick={() => setTab(v)} className={cn("-mb-px flex items-center gap-1 border-b-2 px-1 pb-1.5 pt-1 t-label2", tab === v ? "border-ink-900 text-ink-900" : "border-transparent font-medium text-ink-500 hover:text-ink-900")}>
            {label}
            {v !== "ALL" && count(v) > 0 && <span className="grid h-4 min-w-4 place-items-center rounded-full bg-orange px-1 text-[10px] font-bold leading-none text-white">{count(v)}</span>}
          </button>
        ))}
      </div>
      <div className="flex flex-col gap-0.5 overflow-y-auto">
        {!data && !error && <Skeleton className="m-2 h-[160px]" />}
        {!!error && !data && (
          <div className="flex flex-col items-center gap-2 py-8 text-center">
            <p className="text-[13px] text-ink-500">{errorMessage(error)}</p>
            <Button size="S" variant="secondary" onClick={reload}>
              다시 시도
            </Button>
          </div>
        )}
        {data?.items.length === 0 && (
          <div className="flex flex-col items-center gap-2 py-10 text-center">
            <Icon name="bell-off" size={28} className="text-ink-400" />
            <p className="text-[13px] text-ink-500">새 알림이 없어요</p>
          </div>
        )}
        {data?.items.map((n) => {
          const href = notificationHref(n);
          const quick = n.type === "COFFEE_CHAT_REQUESTED" && !n.read && n.targetId != null;
          return (
            <div key={String(n.id)} className={cn("relative flex gap-2.5 rounded-[6px] px-2 py-2.5", !n.read && "bg-bg-50")}>
              <Icon name={iconOf(n)} size={18} className={cn("mt-0.5", n.read ? "text-ink-500" : "text-ink-900")} />
              <div className="flex min-w-0 flex-1 flex-col gap-1">
                <button type="button" onClick={() => void go(n)} className={cn("text-left text-[13px] leading-[18px] hover:underline", n.read ? "text-ink-500" : "text-ink-900")}>
                  {n.actor && <b className="font-semibold">{n.actor.isDeleted || !n.actor.name ? "탈퇴한 회원" : n.actor.name}</b>}
                  {n.message}
                </button>
                <p suppressHydrationWarning className="text-[11px] text-ink-400">
                  {timeAgo(n.createdAt)}
                </p>
                {quick && (
                  <div className="flex gap-1.5 pt-1">
                    <Button size="S" onClick={() => void go(n, `${href}&action=accept`)}>
                      수락하기
                    </Button>
                    <Button size="S" variant="secondary" onClick={() => void go(n, `${href}&action=reject`)}>
                      거절하기
                    </Button>
                  </div>
                )}
                {n.type === "MESSAGE_REQUESTED" && !n.read && (
                  <div className="pt-1">
                    <Button size="S" variant="secondary" onClick={() => void go(n)}>
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
