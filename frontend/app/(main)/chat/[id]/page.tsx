"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { Suspense, use, useEffect, useLayoutEffect, useRef, useState } from "react";
import { cn } from "@/lib/cn";
import { useSession } from "@/lib/session";
import { ME_ID } from "@/lib/mock/users";
import { COFFEE_STATUS_LABEL, LION_PART_LABEL } from "@/lib/types";
import { formatDateFull, formatDateTime, formatTime } from "@/lib/format";
import { API_ENABLED, errorMessage, isApiError } from "@/lib/api/client";
import { declineLabel, REQUEST_COOLDOWN_DAYS, userName, type ChatMessage, type ChatUser, type RequestDetail, type RoomDetail } from "@/lib/api/chat";
import { avatarPeople, myIdOf, newClientId, partOf } from "@/lib/api/chatView";
import { addDays } from "@/lib/api/coffeeSettings";
import type { Id } from "@/lib/api/posts";
import { useApi } from "@/lib/api/useApi";
import { useChatRefresh, useChatService, type ChatService } from "@/lib/api/useChatService";
import { Avatar, AvatarGroup } from "@/components/ui/Avatar";
import { PartTag, StatusBadge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Banner, EmptyState, Skeleton } from "@/components/ui/Misc";
import { Glyph } from "@/components/ui/Icon";
import { Switch, Textarea } from "@/components/ui/Input";
import { ConfirmModal, Modal, Popover } from "@/components/ui/Modal";
import { useToast } from "@/components/ui/Toast";
import { statusTone } from "@/components/domain/CoffeeChatDetail";

/** 실시간 연결(WebSocket) 전까지 새 메시지를 확인하는 간격 */
const POLL_MS = 4_000;
const MEETUP_STATUS_LABEL = { OPEN: "모집 중", FULL: "정원 마감", ENDED: "끝남", CANCELED: "취소됨" } as const;

/** 화면에 그리는 메시지. 보내는 중·실패한 내 메시지도 함께 둔다 */
type Item = ChatMessage & { clientId?: string; state?: "sending" | "failed" };

let loadSeq = 0;

/** ★ T-17 1:1 채팅방 · ◇ T-27 단체 채팅방 · ◇ T-05 메시지 요청 보냄 · T-23 방 메뉴 · CF-9/CF-9b 나가기 (CHAT-06 ~ 14) */
function ChatRoomInner({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const service = useChatService();
  const { data, error, reload } = useApi(
    async (signal) => {
      const [room, page] = await Promise.all([service.getRoom(id, signal), service.listMessages(id, {}, signal)]);
      return { room, messages: page.items, hasMore: page.hasMore, version: ++loadSeq };
    },
    [service, id],
  );

  if (error && !data) {
    if (isApiError(error) && error.status === 404) return <EmptyState title="채팅방을 찾을 수 없어요" description="나갔거나 없는 채팅방이에요" action={{ label: "채팅 목록으로", href: "/chat" }} />;
    return <EmptyState title="채팅방을 불러오지 못했어요" description={errorMessage(error)} action={{ label: "다시 시도", onClick: reload }} />;
  }
  if (!data) return <Skeleton className="m-6 h-[400px]" />;
  return <RoomView key={data.version} service={service} initialRoom={data.room} initialMessages={data.messages} initialHasMore={data.hasMore} reloadRoom={reload} />;
}

function RoomView({ service, initialRoom, initialMessages, initialHasMore, reloadRoom }: { service: ChatService; initialRoom: RoomDetail; initialMessages: ChatMessage[]; initialHasMore: boolean; reloadRoom: () => void }) {
  const router = useRouter();
  const toast = useToast();
  const refreshList = useChatRefresh();
  const { me, user } = useSession();
  const myId = myIdOf(me, ME_ID);
  const myName = me?.name ?? user?.name ?? "나";
  const [room, setRoom] = useState(initialRoom);
  const [messages, setMessages] = useState<Item[]>(initialMessages);
  const [hasMore, setHasMore] = useState(initialHasMore);
  const [loadingOlder, setLoadingOlder] = useState(false);
  const [text, setText] = useState("");
  const [menu, setMenu] = useState(false);
  const [leave, setLeave] = useState(false);
  const [leaving, setLeaving] = useState(false);
  const [questionsOpen, setQuestionsOpen] = useState(false);
  const scroller = useRef<HTMLDivElement>(null);
  const keepScroll = useRef<number | null>(null);
  const lastRead = useRef<Id | null>(null);
  const roomId = room.roomId;

  const isMine = (m: ChatMessage) => m.type === "TEXT" && String(m.senderId) === myId;
  const confirmed = messages.filter((m) => !m.state);
  const lastId = confirmed.at(-1)?.id;
  const partner = room.type === "DIRECT" ? room.members[0] : undefined;
  const pendingRequest = room.status === "REQUESTED" && room.requestedByMe;
  const ended = room.status === "DECLINED" || room.status === "CANCELED";

  // 내가 보낸 요청이면 요청 정보(첫 메시지 · 거절 사유)를 불러온다 (CHAT-03)
  const request = useApi((signal) => service.getRequest(roomId, signal), [service, roomId], room.requestedByMe && room.status !== "ACTIVE");

  // 새 메시지가 아래에 붙으면 맨 아래로, 위에 이전 메시지를 붙였으면 보던 위치를 지킨다
  useLayoutEffect(() => {
    const el = scroller.current;
    if (!el) return;
    if (keepScroll.current != null) {
      el.scrollTop = el.scrollHeight - keepScroll.current;
      keepScroll.current = null;
    } else el.scrollTop = el.scrollHeight;
  }, [messages]);

  // 남이 보낸 메시지가 마지막이면 읽음 처리 (CHAT-12). 내가 보낸 메시지는 서버가 이미 읽은 것으로 친다
  const lastFromOther = confirmed.length > 0 && !isMine(confirmed[confirmed.length - 1]);
  useEffect(() => {
    if (lastId == null || lastRead.current === lastId) return;
    lastRead.current = lastId;
    if (!lastFromOther) return;
    service
      .read(roomId, lastId)
      .then(refreshList)
      .catch(() => undefined);
  }, [lastId, lastFromOther, roomId, service, refreshList]);

  // 새 메시지 확인 (after 커서). 수락 대기 중이면 방 상태가 바뀌었는지도 본다
  useEffect(() => {
    if (!API_ENABLED || ended) return;
    const timer = setInterval(async () => {
      try {
        if (room.status !== "ACTIVE") {
          const next = await service.getRoom(roomId);
          if (next.status !== room.status) return reloadRoom();
        }
        if (lastId == null) return;
        const page = await service.listMessages(roomId, { after: lastId });
        if (page.items.length) setMessages((ms) => mergeNew(ms, page.items));
      } catch {
        // 다음 주기에 다시 시도한다
      }
    }, POLL_MS);
    return () => clearInterval(timer);
  }, [service, roomId, lastId, room.status, ended, reloadRoom]);

  const loadOlder = async () => {
    const first = confirmed[0];
    if (!first) return;
    setLoadingOlder(true);
    try {
      const page = await service.listMessages(roomId, { before: first.id });
      keepScroll.current = (scroller.current?.scrollHeight ?? 0) - (scroller.current?.scrollTop ?? 0);
      setMessages((ms) => [...page.items.filter((x) => !ms.some((y) => y.id === x.id)), ...ms]);
      setHasMore(page.hasMore);
    } catch (e) {
      toast(errorMessage(e), { kind: "error" });
    } finally {
      setLoadingOlder(false);
    }
  };

  const deliver = async (content: string, clientId: string) => {
    try {
      const sent = await service.send(roomId, content, clientId);
      setMessages((ms) => mergeNew(ms.filter((m) => m.clientId !== clientId), [sent]));
      refreshList();
    } catch (e) {
      setMessages((ms) => ms.map((m) => (m.clientId === clientId ? { ...m, state: "failed" } : m)));
      toast(errorMessage(e), { kind: "error" });
    }
  };

  const send = () => {
    const content = text.trim();
    if (!content || content.length > 1000 || !room.canSend) return;
    const clientId = newClientId();
    setMessages((ms) => [...ms, { id: `tmp-${clientId}`, clientId, state: "sending", type: "TEXT", senderId: myId, content, deleted: false, createdAt: new Date().toISOString(), editedAt: null, unreadCount: 0 }]);
    setText("");
    void deliver(content, clientId);
  };

  const retry = (m: Item) => {
    if (!m.clientId) return;
    setMessages((ms) => ms.map((x) => (x.clientId === m.clientId ? { ...x, state: "sending" } : x)));
    void deliver(m.content, m.clientId);
  };

  const setPref = async (patch: { pinned?: boolean; muted?: boolean }) => {
    const prev = { pinned: room.pinned, muted: room.muted };
    setRoom((r) => ({ ...r, ...patch }));
    try {
      const res = await service.setPreferences(roomId, patch);
      setRoom((r) => ({ ...r, pinned: res.pinned, muted: res.muted }));
      refreshList();
    } catch (e) {
      setRoom((r) => ({ ...r, ...prev }));
      toast(errorMessage(e), { kind: "error" });
    }
  };

  const doLeave = async () => {
    setLeaving(true);
    try {
      await service.leave(roomId);
      toast(ended ? "목록에서 지웠어요" : "채팅방을 나갔어요");
      refreshList();
      router.push("/chat");
    } catch (e) {
      toast(errorMessage(e), { kind: "error" });
    } finally {
      setLeaving(false);
      setLeave(false);
    }
  };

  const senderOf = (m: ChatMessage): ChatUser | undefined => room.members.find((u) => String(u.id) === String(m.senderId));
  const title = room.meetup?.title ?? (room.type === "DIRECT" ? userName(partner) : room.members.map(userName).join(", "));
  const placeholder = room.canSend ? "메시지를 입력하세요 (Enter 전송)" : pendingRequest ? "상대가 수락하면 메시지를 보낼 수 있어요" : "메시지를 보낼 수 없어요";

  // 날짜별로 묶기
  const days: { day: string; items: Item[] }[] = [];
  messages.forEach((m) => {
    const day = formatDateFull(m.createdAt);
    const last = days.at(-1);
    if (last && last.day === day) last.items.push(m);
    else days.push({ day, items: [m] });
  });

  return (
    <div className="grid flex-1 grid-cols-[1fr_320px]">
      {/* Conversation */}
      <section className="flex min-w-0 flex-col">
        <header className="flex h-16 items-center justify-between border-b border-line-200 px-6">
          <div className="flex min-w-0 items-center gap-3">
            {room.type === "GROUP" ? <AvatarGroup people={[...avatarPeople(room.members), { name: myName }]} size={32} /> : <Avatar name={userName(partner)} part={partOf(partner)} size={40} />}
            <div className="flex min-w-0 flex-col">
              <span className="truncate t-body-bold text-ink-900">{title}</span>
              <span className="text-[12px] text-ink-500">{room.type === "GROUP" ? `${room.members.length + 1}명` : partner && !partner.isDeleted ? `${partner.generation}기 · ${LION_PART_LABEL[partOf(partner)]}` : ""}</span>
            </div>
          </div>
          <div className="relative">
            <button type="button" aria-label="방 메뉴" onClick={() => setMenu((m) => !m)} className="rounded p-1.5 hover:bg-bg-100">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round">
                <path d="M4 7h16M4 12h16M4 17h16" />
              </svg>
            </button>
            {/* [O] T-23 방 메뉴 */}
            <Popover open={menu} onClose={() => setMenu(false)} className="w-[200px] p-2">
              <MenuToggle label="상단 고정" on={room.pinned} onChange={(v) => void setPref({ pinned: v })} />
              <MenuToggle label="알림 끄기" on={room.muted} onChange={(v) => void setPref({ muted: v })} />
              {!pendingRequest && (
                <button
                  type="button"
                  onClick={() => {
                    setMenu(false);
                    setLeave(true);
                  }}
                  className="flex w-full items-center gap-2 rounded-[6px] px-3 py-2 text-left text-[14px] text-orange hover:bg-orange-back"
                >
                  {ended ? "목록에서 지우기" : "나가기"}
                </button>
              )}
            </Popover>
          </div>
        </header>

        {/* Sticky schedule bar (T-17) */}
        {room.coffeeChat?.status === "SCHEDULED" && room.coffeeChat.scheduledAt && (
          <div className="mx-6 mt-4 flex items-center justify-between rounded-[8px] bg-bg-50 px-4 py-3 text-[14px]">
            <span className="font-semibold text-ink-900">
              📅 {formatDateTime(room.coffeeChat.scheduledAt)}
              {room.coffeeChat.meetingMode === "OFFLINE" && room.coffeeChat.meetingPlace && ` · ${room.coffeeChat.meetingPlace}`}
            </span>
            <Link href={`/coffee?id=${room.coffeeChat.id}`} className="text-ink-700 hover:underline">
              변경
            </Link>
          </div>
        )}
        {pendingRequest && <PendingRequestBanner service={service} roomId={roomId} request={request.data} onEdited={(m) => setMessages((ms) => ms.map((x) => (x.id === m.id ? m : x)))} />}
        {room.status === "DECLINED" && (
          <div className="mx-6 mt-4">
            <Banner tone="warn">{declinedText(request.data)}</Banner>
          </div>
        )}
        {room.status === "ACTIVE" && !room.canSend && (
          <div className="mx-6 mt-4">
            <Banner tone="warn">{partner?.isDeleted ? "탈퇴한 동문에게는 메시지를 보낼 수 없어요" : room.meetup?.status === "CANCELED" ? "번개가 취소돼서 메시지를 보낼 수 없어요" : "대화할 수 없는 상대예요"}</Banner>
          </div>
        )}

        <div ref={scroller} className="flex-1 overflow-y-auto px-6 py-4" aria-label="대화 내용">
          {hasMore && (
            <div className="pb-2 text-center">
              <button type="button" disabled={loadingOlder} onClick={() => void loadOlder()} className="text-[12px] text-ink-500 underline disabled:opacity-50">
                이전 메시지 더 보기
              </button>
            </div>
          )}
          {days.map((d) => (
            <div key={d.day} className="flex flex-col gap-3">
              <p className="my-3 flex items-center gap-3 text-[12px] text-ink-400 before:h-px before:flex-1 before:bg-line-200 after:h-px after:flex-1 after:bg-line-200">{dayLabel(d.day)}</p>
              {d.items.map((m) => {
                if (m.type === "SYSTEM")
                  return (
                    <p key={String(m.id)} className="mx-auto w-fit rounded-full bg-orange-back px-3.5 py-1.5 text-[12px] text-ink-900">
                      {m.content}
                    </p>
                  );
                const mine = isMine(m);
                const sender = mine ? undefined : senderOf(m);
                return (
                  <div key={String(m.id)} className={cn("flex gap-2.5", mine ? "justify-end" : "items-start")}>
                    {!mine && <Avatar name={userName(sender)} part={partOf(sender)} size={40} />}
                    <div className={cn("flex max-w-[70%] flex-col gap-1", mine && "items-end")}>
                      {!mine && <span className="text-[13px] text-ink-700">{userName(sender)}</span>}
                      <div className={cn("flex items-end gap-1.5", mine && "flex-row-reverse")}>
                        <p className={cn("whitespace-pre-line rounded-[8px] px-3.5 py-2.5 text-[14px] leading-5", mine ? "bg-primary-strong text-white" : "border border-line-200 bg-white text-ink-900", m.state && "opacity-60", m.deleted && "italic opacity-60")}>{m.deleted ? "삭제된 메시지예요" : m.content}</p>
                        <span className="flex flex-col items-end text-[11px] text-ink-400">
                          {m.state === "failed" ? (
                            <button type="button" onClick={() => retry(m)} className="text-orange underline">
                              다시 보내기
                            </button>
                          ) : m.state === "sending" ? (
                            "보내는 중"
                          ) : (
                            <>
                              {mine && room.type === "GROUP" && m.unreadCount > 0 && <span className="text-orange">{m.unreadCount}</span>}
                              {formatTime(m.createdAt)}
                              {m.editedAt && " · 수정됨"}
                            </>
                          )}
                        </span>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          ))}
        </div>

        <form
          onSubmit={(e) => {
            e.preventDefault();
            send();
          }}
          className="flex items-center gap-3 border-t border-line-200 px-5 py-4"
        >
          <input value={text} onChange={(e) => setText(e.target.value)} disabled={!room.canSend} placeholder={placeholder} aria-label="메시지 입력" className="h-12 flex-1 rounded-full border border-line-300 px-5 text-[14px] outline-none placeholder:text-ink-400 focus:border-ink-800 disabled:bg-bg-100" maxLength={1000} />
          <button type="submit" aria-label="전송" disabled={!room.canSend || !text.trim()} className="grid size-12 place-items-center rounded-full bg-navy text-white disabled:bg-line-300">
            <Glyph name="send" size={20} />
          </button>
        </form>
        {text.length > 900 && <p className="px-5 pb-2 text-right text-[11px] text-ink-400">{text.length}/1000 · 1000자까지 보낼 수 있어요</p>}
      </section>

      {/* Right panel (T-32 states) */}
      <aside className="flex flex-col border-l border-line-200">
        {room.type === "GROUP" ? (
          <div className="flex flex-col gap-3 border-b border-line-200 p-5">
            <p className="t-label1 text-ink-900">참여자 {room.members.length + 1}명</p>
            <ul className="flex flex-col gap-3">
              <li className="flex items-center justify-between">
                <Link href="/me" className="flex items-center gap-2.5 hover:underline">
                  <span className="relative">
                    <Avatar name={myName} size={32} />
                    <span className="absolute -left-1 -top-1 rounded-full bg-ink-700 px-1 text-[9px] text-white">나</span>
                  </span>
                  <span className="text-[14px] font-semibold text-ink-900">{myName}</span>
                </Link>
              </li>
              {room.members.map((u, i) => (
                <li key={String(u.id ?? `gone-${i}`)} className="flex items-center justify-between">
                  {u.isDeleted ? (
                    <span className="flex items-center gap-2.5 text-[14px] text-ink-400">
                      <Avatar size={32} /> 탈퇴한 동문
                    </span>
                  ) : (
                    <Link href={`/alumni/${u.id}`} className="flex items-center gap-2.5 hover:underline">
                      <Avatar name={userName(u)} part={partOf(u)} size={32} />
                      <span className="text-[14px] font-semibold text-ink-900">{userName(u)}</span>
                    </Link>
                  )}
                  {u.generation && <span className="text-[12px] text-ink-500">{u.generation}기</span>}
                </li>
              ))}
            </ul>
            <p className="text-[12px] leading-5 text-ink-500">
              초대로 인원을 추가할 수 없어요
              <br />
              나간 사람은 다시 들어올 수 없어요
            </p>
          </div>
        ) : (
          partner && (
            <div className="flex flex-col gap-3 border-b border-line-200 p-5">
              <p className="text-[13px] text-ink-500">상대 프로필</p>
              <div className="flex items-center gap-3">
                <Avatar name={userName(partner)} part={partOf(partner)} size={64} />
                <p className="flex items-center gap-2">
                  <span className="text-[16px] font-bold text-ink-900">{userName(partner)}</span>
                  {!partner.isDeleted && partner.generation && <PartTag part={partOf(partner)} generation={partner.generation} className="px-1.5 py-0.5 text-[11px]" />}
                </p>
              </div>
              {!partner.isDeleted && (
                <Button variant="secondary" full href={`/alumni/${partner.id}`}>
                  프로필 보기
                </Button>
              )}
            </div>
          )
        )}

        {room.meetup ? (
          <div className="flex flex-col gap-2 border-b border-line-200 p-5">
            <p className="text-[13px] text-ink-500">이 대화의 번개</p>
            <p className="flex items-center gap-2">
              <StatusBadge tone={room.meetup.status === "OPEN" ? "outline" : "gray"}>{MEETUP_STATUS_LABEL[room.meetup.status]}</StatusBadge>
              <span className="truncate t-label1 text-ink-900">{room.meetup.title}</span>
            </p>
            <p className="text-[13px] text-ink-700">
              {formatDateTime(room.meetup.meetAt)} · {room.meetup.place}
            </p>
            <Button size="M" variant="secondary" href={`/community/${room.meetup.postId}`}>
              번개 글 보기
            </Button>
          </div>
        ) : (
          <div className="flex flex-col gap-3 border-b border-line-200 p-5">
            <p className="text-[13px] text-ink-500">이 대화의 커피챗</p>
            {room.coffeeChat ? (
              <CoffeeCard chat={room.coffeeChat} open={questionsOpen} onToggle={() => setQuestionsOpen((o) => !o)} />
            ) : (
              <p className="text-[13px] text-ink-500">아직 커피챗이 없어요</p>
            )}
          </div>
        )}

        <div className="flex flex-col gap-3 p-5">
          <p className="text-[13px] text-ink-500">채팅방 설정</p>
          <div className="flex items-center justify-between text-[14px] text-ink-900">
            상단 고정 <Switch checked={room.pinned} onChange={(v) => void setPref({ pinned: v })} label="상단 고정" />
          </div>
          <div className="flex items-center justify-between text-[14px] text-ink-900">
            알림 끄기 <Switch checked={room.muted} onChange={(v) => void setPref({ muted: v })} label="알림 끄기" />
          </div>
          {!pendingRequest && (
            <button type="button" onClick={() => setLeave(true)} className="w-fit text-[14px] text-orange hover:underline">
              {ended ? "목록에서 지우기" : "나가기"}
            </button>
          )}
        </div>
      </aside>

      {/* CF-9 / CF-9b 나가기 확인 */}
      <ConfirmModal
        open={leave}
        onClose={() => setLeave(false)}
        onConfirm={() => void doLeave()}
        loading={leaving}
        title={ended ? "이 요청을 목록에서 지울까요?" : room.type === "GROUP" ? "단체 채팅방에서 나갈까요?" : "나가면 대화 내용이 모두 지워져요"}
        description={ended ? undefined : room.type === "GROUP" ? "나간 사람은 다시 들어올 수 없어요." : "상대가 새 메시지를 보내면 방이 다시 나타나요. 이전 대화는 보이지 않아요."}
        confirmLabel={ended ? "지우기" : "나가기"}
        danger
      />
    </div>
  );
}

/** 이미 있는 메시지는 건너뛰고 새 메시지만 뒤에 붙인다 (보내는 중인 내 메시지는 맨 뒤 유지) */
function mergeNew(ms: Item[], incoming: ChatMessage[]): Item[] {
  const known = new Set(ms.filter((m) => !m.state).map((m) => String(m.id)));
  const fresh = incoming.filter((m) => !known.has(String(m.id)));
  if (!fresh.length) return ms;
  const confirmed = ms.filter((m) => !m.state);
  const local = ms.filter((m) => m.state);
  return [...confirmed, ...fresh, ...local];
}

/** "2026.11.12" → "2026년 11월 12일" */
const dayLabel = (day: string) => {
  const [y, m, d] = day.split(".");
  return `${y}년 ${Number(m)}월 ${Number(d)}일`;
};

function declinedText(r: RequestDetail | undefined): string {
  if (!r) return "요청이 거절됐어요";
  const reason = declineLabel(r.declineReason, r.declineMessage);
  const again = r.declinedAt ? addDays(r.declinedAt.slice(0, 10), REQUEST_COOLDOWN_DAYS) : null;
  return `요청이 거절됐어요.${reason ? ` 사유: ${reason}.` : ""}${again ? ` ${Number(again.slice(5, 7))}월 ${Number(again.slice(8, 10))}일부터 다시 보낼 수 있어요` : ""}`;
}

/** ◇ T-05 메시지 요청 보냄 · 수락 대기. 수락 전에는 첫 메시지를 고치거나 요청을 취소할 수 있다 (CHAT-06 · 07) */
function PendingRequestBanner({ service, roomId, request, onEdited }: { service: ChatService; roomId: Id; request: RequestDetail | undefined; onEdited: (m: ChatMessage) => void }) {
  const router = useRouter();
  const toast = useToast();
  const refreshList = useChatRefresh();
  const [editing, setEditing] = useState<string | null>(null);
  const [canceling, setCanceling] = useState(false);
  const [busy, setBusy] = useState(false);
  const first = request?.firstMessage;

  const save = async () => {
    if (!first || editing == null) return;
    setBusy(true);
    try {
      const m = await service.editRequestMessage(roomId, first.id, editing.trim());
      onEdited(m);
      setEditing(null);
      toast("요청 메시지를 고쳤어요");
    } catch (e) {
      toast(errorMessage(e), { kind: "error" });
    } finally {
      setBusy(false);
    }
  };

  const cancel = async () => {
    if (!first) return;
    setBusy(true);
    try {
      await service.cancelRequest(roomId, first.id);
      toast("요청을 취소했어요");
      refreshList();
      router.push("/chat");
    } catch (e) {
      toast(errorMessage(e), { kind: "error" });
      setBusy(false);
      setCanceling(false);
    }
  };

  const len = editing?.trim().length ?? 0;
  return (
    <div className="mx-6 mt-4">
      <Banner
        action={
          first && (
            <span className="flex gap-1.5">
              <Button size="S" variant="secondary" onClick={() => setEditing(first.content)}>
                요청 수정
              </Button>
              <Button size="S" variant="text" onClick={() => setCanceling(true)}>
                요청 취소
              </Button>
            </span>
          )
        }
      >
        상대가 수락하면 대화를 이어갈 수 있어요
      </Banner>
      <Modal
        open={editing != null}
        onClose={() => setEditing(null)}
        title="요청 메시지 고치기"
        footer={
          <>
            <Button variant="secondary" onClick={() => setEditing(null)}>
              취소
            </Button>
            <Button loading={busy} disabled={len < 5 || len > 300} onClick={() => void save()}>
              저장
            </Button>
          </>
        }
      >
        <Textarea aria-label="요청 메시지" rows={4} value={editing ?? ""} onChange={(e) => setEditing(e.target.value)} counter={{ value: len, max: 300 }} help="5~300자 · 상대가 수락하기 전까지만 고칠 수 있어요" />
      </Modal>
      <ConfirmModal open={canceling} onClose={() => setCanceling(false)} onConfirm={() => void cancel()} loading={busy} danger title="메시지 요청을 취소할까요?" description="보낸 첫 메시지가 지워지고 상대의 요청함에서도 사라져요." confirmLabel="요청 취소" />
    </div>
  );
}

function CoffeeCard({ chat, open, onToggle }: { chat: NonNullable<RoomDetail["coffeeChat"]>; open: boolean; onToggle: () => void }) {
  const [now] = useState(() => Date.now());
  const dDay = chat.scheduledAt ? Math.max(0, Math.ceil((new Date(chat.scheduledAt).getTime() - now) / 86_400_000)) : null;
  return (
    <>
      <div className="flex items-center gap-2">
        <StatusBadge tone={statusTone(chat.status)}>{COFFEE_STATUS_LABEL[chat.status]}</StatusBadge>
        {chat.status === "SCHEDULED" && dDay != null && <span className="rounded-[4px] bg-primary px-2 py-0.5 text-[12px] font-bold text-white">D-{dDay}</span>}
        {chat.status === "ACCEPTED" && <span className="text-[12px] text-ink-500">일정 미정</span>}
      </div>
      {chat.status === "SCHEDULED" && chat.scheduledAt ? (
        <div>
          <p className="text-[20px] font-bold text-ink-900">{formatDateTime(chat.scheduledAt)}</p>
          <p className="text-[13px] text-ink-700">{chat.meetingMode === "OFFLINE" ? `${chat.meetingPlace ?? "장소 미정"} · 오프라인` : "온라인"}</p>
        </div>
      ) : chat.status === "ACCEPTED" ? (
        <p className="text-[13px] text-ink-700">일정 후보 {chat.proposedSlots.map(formatDateTime).join(" · ") || "없음"}</p>
      ) : null}
      <div className="flex gap-2">
        <Button size="M" variant="secondary" className="flex-1" href={`/coffee?id=${chat.id}`}>
          상세 보기
        </Button>
        {(chat.status === "ACCEPTED" || chat.status === "SCHEDULED") && (
          <Button size="M" variant={chat.status === "ACCEPTED" ? "primary" : "secondary"} className="flex-1" href={`/coffee?id=${chat.id}`}>
            {chat.status === "ACCEPTED" ? "일정 확정" : "일정 변경"}
          </Button>
        )}
      </div>
      {chat.questions.length > 0 && (
        <div>
          <button type="button" onClick={onToggle} className="flex w-full items-center justify-between text-[14px] font-semibold text-ink-900">
            미리 드린 질문 ({chat.questions.length}) <span className="text-[12px] font-normal text-ink-500">{open ? "▴ 접기" : "▾ 펼치기"}</span>
          </button>
          {open && (
            <ol className="mt-2 flex flex-col gap-1 text-[13px] text-ink-700">
              {chat.questions.map((q, i) => (
                <li key={i}>
                  {i + 1}. {q}
                </li>
              ))}
            </ol>
          )}
        </div>
      )}
    </>
  );
}

function MenuToggle({ label, on, onChange }: { label: string; on: boolean; onChange: (v: boolean) => void }) {
  return (
    <div className="flex items-center justify-between rounded-[6px] px-3 py-2 text-[14px] text-ink-900">
      {label}
      <Switch checked={on} onChange={onChange} label={label} />
    </div>
  );
}

/** Cache Components: params must be read inside Suspense. */
export default function ChatRoomPage({ params }: { params: Promise<{ id: string }> }) {
  return (
    <Suspense fallback={<Skeleton className="h-[400px] w-full" />}>
      <ChatRoomInner params={params} />
    </Suspense>
  );
}
