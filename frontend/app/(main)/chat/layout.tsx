"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useState } from "react";
import { cn } from "@/lib/cn";
import { timeAgo } from "@/lib/format";
import { useSession } from "@/lib/session";
import { API_ENABLED, errorMessage } from "@/lib/api/client";
import { roomTitle, userName, type RoomSummary } from "@/lib/api/chat";
import { avatarPeople, myIdOf, partOf } from "@/lib/api/chatView";
import { ME_ID } from "@/lib/mock/users";
import { useApi } from "@/lib/api/useApi";
import { ChatRefreshContext, useChatService } from "@/lib/api/useChatService";
import { Avatar, AvatarGroup } from "@/components/ui/Avatar";
import { CountBadge } from "@/components/ui/Badge";
import { Glyph } from "@/components/ui/Icon";
import { Skeleton } from "@/components/ui/Misc";
import { useToast } from "@/components/ui/Toast";

/** 실시간 연결(WebSocket) 전까지 목록을 다시 불러오는 간격 */
const POLL_MS = 10_000;

/**
 * 채팅 3단 (v2.1): 목록 320 / 대화 / 오른쪽 패널. Left list lives here so it
 * persists across /chat, /chat/[id], /chat/requests. (CHAT-08 · 14 · 15)
 */
export default function ChatLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const toast = useToast();
  const { me } = useSession();
  const myId = myIdOf(me, ME_ID);
  const service = useChatService();
  const rooms = useApi((signal) => service.listRooms(0, signal), [service]);
  const counts = useApi((signal) => service.unreadCount(signal), [service]);
  const [q, setQ] = useState("");
  const [more, setMore] = useState(false);
  const reloadRooms = rooms.reload;
  const reloadCounts = counts.reload;
  const refresh = () => {
    reloadRooms();
    reloadCounts();
  };

  useEffect(() => {
    if (!API_ENABLED) return;
    const timer = setInterval(() => {
      reloadRooms();
      reloadCounts();
    }, POLL_MS);
    return () => clearInterval(timer);
  }, [reloadRooms, reloadCounts]);

  const kw = q.trim().toLowerCase();
  const list = (rooms.data?.items ?? []).filter((r) => !kw || `${roomTitle(r)} ${[r.partner, ...r.members].map(userName).join(" ")}`.toLowerCase().includes(kw));

  const loadMore = async () => {
    if (!rooms.data) return;
    setMore(true);
    try {
      const next = await service.listRooms(rooms.data.page + 1);
      rooms.setData((d) => d && { ...next, items: [...d.items, ...next.items.filter((x) => !d.items.some((y) => y.roomId === x.roomId))] });
    } catch (e) {
      toast(errorMessage(e), { kind: "error" });
    } finally {
      setMore(false);
    }
  };

  const hide = async (r: RoomSummary) => {
    try {
      await service.leave(r.roomId);
      refresh();
    } catch (e) {
      toast(errorMessage(e), { kind: "error" });
    }
  };

  return (
    <ChatRefreshContext.Provider value={refresh}>
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
              <CountBadge count={counts.data?.messageRequests ?? 0} />
              <Glyph name="chevron-right" size={16} className="text-ink-400" />
            </span>
          </Link>
          <ul className="flex-1 overflow-y-auto">
            {!rooms.data && !rooms.error && (
              <li className="p-4">
                <Skeleton className="h-[240px] w-full" />
              </li>
            )}
            {!!rooms.error && !rooms.data && <li className="px-4 py-10 text-center text-[13px] text-ink-500">{errorMessage(rooms.error)}</li>}
            {rooms.data && list.length === 0 && <li className="px-4 py-10 text-center text-[13px] text-ink-500">{kw ? "찾는 채팅방이 없어요" : "아직 채팅방이 없어요"}</li>}
            {list.map((r) => (
              <RoomRow key={String(r.roomId)} room={r} meId={myId} active={pathname === `/chat/${r.roomId}`} onHide={() => void hide(r)} />
            ))}
            {rooms.data?.hasNext && !kw && (
              <li className="p-3 text-center">
                <button type="button" disabled={more} onClick={() => void loadMore()} className="text-[13px] text-ink-500 underline disabled:opacity-50">
                  채팅방 더 보기
                </button>
              </li>
            )}
          </ul>
        </aside>
        <div className="flex min-w-0 flex-col">{children}</div>
      </div>
    </ChatRefreshContext.Provider>
  );
}

function RoomRow({ room: r, meId, active, onHide }: { room: RoomSummary; meId: string; active: boolean; onHide: () => void }) {
  const ended = r.status === "DECLINED" || r.status === "CANCELED";
  const waiting = r.status === "REQUESTED" && r.requestedByMe;
  const last = r.lastMessage;
  const senderName = r.type === "GROUP" && last?.type === "TEXT" && String(last.senderId) !== String(meId) ? r.members.find((m) => String(m.id) === String(last.senderId)) : undefined;
  const preview = ended ? "거절됨" : waiting ? "수락 대기 중" : last ? `${senderName ? `${userName(senderName)}: ` : ""}${last.deleted ? "삭제된 메시지예요" : last.content}` : "";
  return (
    <li className="border-b border-line-200">
      <Link href={`/chat/${r.roomId}`} className={cn("flex items-center gap-3 px-4 py-3.5 hover:bg-bg-50", active && "bg-blue-100 hover:bg-blue-100")}>
        {r.type === "GROUP" ? <AvatarGroup people={avatarPeople(r.members)} size={32} /> : <Avatar name={userName(r.partner)} part={partOf(r.partner)} size={48} />}
        <div className="flex min-w-0 flex-1 flex-col gap-1">
          <div className="flex items-baseline justify-between gap-2">
            <p className={cn("flex min-w-0 items-center gap-1 truncate", ended && "text-ink-400")}>
              {r.pinned && <Glyph name="pin" size={12} className="shrink-0 text-ink-500" />}
              <span className="truncate t-body-bold text-ink-900">{roomTitle(r)}</span>
              <span className="ml-0.5 shrink-0 text-[12px] text-ink-500">{r.type === "GROUP" ? `${r.memberCount}명` : r.partner?.generation ? `${r.partner.generation}기` : ""}</span>
              {r.muted && <Glyph name="bell-off" size={12} className="shrink-0 text-ink-400" />}
            </p>
            <span suppressHydrationWarning className="shrink-0 text-[11px] text-ink-400">
              {last ? timeAgo(last.createdAt) : ""}
            </span>
          </div>
          <div className="flex items-center justify-between gap-2">
            <p className={cn("truncate text-[13px]", ended ? "text-ink-400" : "text-ink-700")}>{preview}</p>
            {ended ? (
              <button
                type="button"
                onClick={(e) => {
                  e.preventDefault();
                  onHide();
                }}
                className="shrink-0 text-[11px] text-ink-500 underline"
              >
                목록에서 지우기
              </button>
            ) : (
              <CountBadge count={r.unreadCount} />
            )}
          </div>
        </div>
      </Link>
    </li>
  );
}
