"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useMemo, useState } from "react";
import { cn } from "@/lib/cn";
import { useStore } from "@/lib/store";
import { ME_ID } from "@/lib/mock/users";
import { LION_PART_LABEL } from "@/lib/types";
import { timeAgo } from "@/lib/format";
import { Avatar, AvatarGroup } from "@/components/ui/Avatar";
import { CountBadge } from "@/components/ui/Badge";
import { Glyph } from "@/components/ui/Icon";

/**
 * 채팅 3단 (v2.1): 목록 320 / 대화 / 오른쪽 패널. Left list lives here so it
 * persists across /chat, /chat/[id], /chat/requests.
 */
export default function ChatLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const { rooms, alumni, leaveRoom } = useStore();
  const [q, setQ] = useState("");

  const requests = rooms.filter((r) => r.isRequest && r.status === "REQUESTED" && r.memberIds[0] !== ME_ID);
  const list = useMemo(() => {
    const kw = q.trim().toLowerCase();
    return rooms
      .filter((r) => !(r.isRequest && r.status === "REQUESTED" && r.memberIds[0] !== ME_ID))
      .filter((r) => {
        if (!kw) return true;
        const names = r.memberIds.map((id) => alumni.find((a) => a.id === id)?.name ?? "").join(" ");
        return `${r.title ?? ""} ${names}`.toLowerCase().includes(kw);
      })
      .sort((a, b) => (b.messages.at(-1)?.at ?? "").localeCompare(a.messages.at(-1)?.at ?? ""));
  }, [rooms, alumni, q]);

  return (
    <div className="-mx-4 -my-8 grid min-h-[calc(100vh-64px)] flex-1 grid-cols-[320px_1fr] bg-white">
      <aside className="flex flex-col border-r border-line-200">
        <div className="p-4">
          <label className="flex h-10 items-center gap-2 rounded-[8px] border border-line-300 px-3 focus-within:border-ink-800">
            <Glyph name="search" className="text-ink-400" />
            <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="이름으로 채팅방 찾기" className="min-w-0 flex-1 text-[14px] outline-none placeholder:text-ink-400" />
          </label>
        </div>
        <Link href="/chat/requests" className={cn("flex items-center justify-between border-b border-line-200 px-4 py-3 hover:bg-bg-50", pathname.startsWith("/chat/requests") && "bg-bg-50")}>
          <span className="flex items-center gap-2 t-body-bold text-ink-900">
            <Glyph name="mail" size={20} /> 메시지 요청
          </span>
          <span className="flex items-center gap-2">
            <CountBadge count={requests.length} />
            <Glyph name="chevron-right" size={16} className="text-ink-400" />
          </span>
        </Link>
        <ul className="flex-1 overflow-y-auto">
          {list.length === 0 && <li className="px-4 py-10 text-center text-[13px] text-ink-500">아직 채팅방이 없어요</li>}
          {list.map((r) => {
            const others = r.memberIds.filter((id) => id !== ME_ID).map((id) => alumni.find((a) => a.id === id)!).filter(Boolean);
            const last = r.messages.at(-1);
            const active = pathname === `/chat/${r.id}`;
            const mineRequest = r.isRequest && r.status === "REQUESTED";
            const declined = r.status === "DECLINED";
            return (
              <li key={r.id} className="border-b border-line-200">
                <Link href={`/chat/${r.id}`} className={cn("flex items-center gap-3 px-4 py-3.5 hover:bg-bg-50", active && "bg-blue-100 hover:bg-blue-100")}>
                  {r.type === "GROUP" ? <AvatarGroup people={others} size={32} /> : <Avatar name={others[0]?.name} part={others[0]?.part} size={48} />}
                  <div className="flex min-w-0 flex-1 flex-col gap-1">
                    <div className="flex items-baseline justify-between gap-2">
                      <p className={cn("truncate", declined && "text-ink-400")}>
                        <span className="t-body-bold text-ink-900">{r.title ?? others[0]?.name}</span>
                        <span className="ml-1.5 text-[12px] text-ink-500">{r.type === "GROUP" ? `${r.memberIds.length}명` : `${others[0]?.generation}기`}</span>
                      </p>
                      <span suppressHydrationWarning className="shrink-0 text-[11px] text-ink-400">{last ? timeAgo(last.at) : ""}</span>
                    </div>
                    <div className="flex items-center justify-between gap-2">
                      <p className={cn("truncate text-[13px]", declined ? "text-ink-400" : "text-ink-700")}>{declined ? "거절됨" : mineRequest ? "수락 대기 중" : last ? `${r.type === "GROUP" && last.senderId !== "system" && last.senderId !== ME_ID ? `${alumni.find((a) => a.id === last.senderId)?.name}: ` : ""}${last.text}` : ""}</p>
                      {declined ? (
                        <button type="button" onClick={(e) => { e.preventDefault(); leaveRoom(r.id); }} className="shrink-0 text-[11px] text-ink-500 underline">
                          목록에서 지우기
                        </button>
                      ) : (
                        <CountBadge count={r.unread} />
                      )}
                    </div>
                  </div>
                </Link>
              </li>
            );
          })}
        </ul>
      </aside>
      <div className="flex min-w-0 flex-col">{children}</div>
      <span className="hidden">{LION_PART_LABEL.ETC}</span>
    </div>
  );
}
