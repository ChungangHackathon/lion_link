"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { Suspense, use, useEffect, useRef, useState } from "react";
import { cn } from "@/lib/cn";
import { useStore } from "@/lib/store";
import { ME_ID } from "@/lib/mock/users";
import { COFFEE_STATUS_LABEL, LION_PART_LABEL } from "@/lib/types";
import { formatDateFull, formatDateTime, formatTime } from "@/lib/format";
import { Avatar, AvatarGroup } from "@/components/ui/Avatar";
import { PartTag, StatusBadge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Skeleton } from "@/components/ui/Misc";
import { Glyph } from "@/components/ui/Icon";
import { Switch } from "@/components/ui/Input";
import { ConfirmModal, Popover } from "@/components/ui/Modal";
import { Banner, EmptyState } from "@/components/ui/Misc";
import { useToast } from "@/components/ui/Toast";
import { statusTone } from "@/components/domain/CoffeeChatDetail";

/** ★ T-17 1:1 채팅방 · ◇ T-27 단체 채팅방 · ◇ T-05 메시지 요청 보냄 · T-23 방 메뉴 · CF-9/CF-9b 나가기 */
function ChatRoomInner({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const router = useRouter();
  const toast = useToast();
  const { rooms, alumni, coffeeChats, sendMessage, markRoomRead, leaveRoom, blocked } = useStore();
  const room = rooms.find((r) => r.id === id);
  const [text, setText] = useState("");
  const [menu, setMenu] = useState(false);
  const [members, setMembers] = useState(false);
  const [leave, setLeave] = useState(false);
  const [pinned, setPinned] = useState(false);
  const [muted, setMuted] = useState(false);
  const [questionsOpen, setQuestionsOpen] = useState(false);
  const bottom = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (room?.unread) markRoomRead(room.id);
  }, [room?.id, room?.unread, markRoomRead]);
  useEffect(() => {
    bottom.current?.scrollIntoView({ block: "end" });
  }, [room?.messages.length]);

  if (!room) return <EmptyState title="채팅방을 찾을 수 없어요" action={{ label: "채팅 목록으로", href: "/chat" }} />;

  const others = room.memberIds.filter((m) => m !== ME_ID).map((m) => alumni.find((a) => a.id === m)!).filter(Boolean);
  const other = others[0];
  const chat = room.coffeeChatId ? coffeeChats.find((c) => c.id === room.coffeeChatId) : undefined;
  const pastChats = coffeeChats.filter((c) => c.id !== chat?.id && c.status === "COMPLETED" && other && ((c.receiverId === other.id && c.requesterId === ME_ID) || (c.requesterId === other.id && c.receiverId === ME_ID)));
  const isRequestPending = room.isRequest && room.status === "REQUESTED";
  const isDeclined = room.status === "DECLINED";
  const isBlocked = others.some((o) => blocked.includes(o.id));
  const canSend = !isRequestPending && !isDeclined && !isBlocked;
  // 선배·대표 신청자는 커피챗 진행 중 나갈 수 없음 (T-31)
  const lockedLeave = chat && ["ACCEPTED", "SCHEDULED"].includes(chat.status) && (chat.receiverId === ME_ID || chat.requesterId === ME_ID);
  const isMemberOfGroup = chat && chat.memberIds.includes(ME_ID);

  const send = () => {
    const t = text.trim();
    if (!t || t.length > 1000) return;
    sendMessage(room.id, t);
    setText("");
  };

  // group messages by day
  const days: { day: string; items: typeof room.messages }[] = [];
  room.messages.forEach((m) => {
    const day = formatDateFull(m.at);
    const last = days.at(-1);
    if (last && last.day === day) last.items.push(m);
    else days.push({ day, items: [m] });
  });

  return (
    <div className="grid flex-1 grid-cols-[1fr_320px]">
      {/* Conversation */}
      <section className="flex min-w-0 flex-col">
        <header className="flex h-16 items-center justify-between border-b border-line-200 px-6">
          <div className="flex items-center gap-3">
            {room.type === "GROUP" ? <AvatarGroup people={[...others, alumni.find((a) => a.id === ME_ID)!]} size={32} /> : <Avatar name={other?.name} part={other?.part} size={40} />}
            <div className="flex flex-col">
              <span className="t-body-bold text-ink-900">{room.title ?? other?.name}</span>
              <span className="text-[12px] text-ink-500">{room.type === "GROUP" ? `${room.memberIds.length}명` : other ? `${other.generation}기 · ${LION_PART_LABEL[other.part]}` : ""}</span>
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
              <MenuToggle label="상단 고정" on={pinned} onChange={setPinned} />
              <MenuToggle label="알림 끄기" on={muted} onChange={setMuted} />
              <button
                type="button"
                onClick={() => {
                  setMenu(false);
                  if (lockedLeave) toast("진행 중인 커피챗이 있어 나갈 수 없어요. 커피챗을 먼저 취소·완료해주세요.", { kind: "error" });
                  else setLeave(true);
                }}
                className="flex w-full items-center gap-2 rounded-[6px] px-3 py-2 text-left text-[14px] text-orange hover:bg-orange-back"
              >
                나가기
              </button>
            </Popover>
          </div>
        </header>

        {/* Sticky schedule bar (T-17) */}
        {chat?.status === "SCHEDULED" && chat.scheduledAt && (
          <div className="mx-6 mt-4 flex items-center justify-between rounded-[8px] bg-bg-50 px-4 py-3 text-[14px]">
            <span className="font-semibold text-ink-900">
              📅 {formatDateTime(chat.scheduledAt)}
              {chat.mode !== "ONLINE" && " · 강남역 ○○카페"}
            </span>
            <Link href={`/coffee/${chat.id}`} className="text-ink-700 hover:underline">
              변경
            </Link>
          </div>
        )}
        {isRequestPending && room.memberIds[0] === ME_ID && (
          <div className="mx-6 mt-4">
            <Banner>상대가 수락하면 대화를 이어갈 수 있어요</Banner>
          </div>
        )}
        {isDeclined && (
          <div className="mx-6 mt-4">
            <Banner tone="warn">요청이 거절됐어요. {room.messages.at(-1)?.text.includes("사유") ? room.messages.at(-1)?.text : ""} 11/11부터 다시 보낼 수 있어요</Banner>
          </div>
        )}
        {isBlocked && (
          <div className="mx-6 mt-4">
            <Banner tone="warn">대화할 수 없는 상대예요</Banner>
          </div>
        )}

        <div className="flex-1 overflow-y-auto px-6 py-4">
          {days.map((d) => (
            <div key={d.day} className="flex flex-col gap-3">
              <p className="my-3 flex items-center gap-3 text-[12px] text-ink-400 before:h-px before:flex-1 before:bg-line-200 after:h-px after:flex-1 after:bg-line-200">{d.day.replace(/\./g, "년 ").replace(/년 $/, "")}</p>
              {d.items.map((m) => {
                if (m.senderId === "system") return <p key={m.id} className="mx-auto w-fit rounded-full bg-orange-back px-3.5 py-1.5 text-[12px] text-ink-900">{m.text}</p>;
                const mine = m.senderId === ME_ID;
                const sender = alumni.find((a) => a.id === m.senderId);
                return (
                  <div key={m.id} className={cn("flex gap-2.5", mine ? "justify-end" : "items-start")}>
                    {!mine && <Avatar name={sender?.name} part={sender?.part} size={40} />}
                    <div className={cn("flex max-w-[70%] flex-col gap-1", mine && "items-end")}>
                      {!mine && <span className="text-[13px] text-ink-700">{sender?.name}</span>}
                      <div className={cn("flex items-end gap-1.5", mine && "flex-row-reverse")}>
                        <p className={cn("whitespace-pre-line rounded-[8px] px-3.5 py-2.5 text-[14px] leading-5", mine ? "bg-primary text-white" : "border border-line-200 bg-white text-ink-900")}>{m.text}</p>
                        <span className="flex flex-col items-end text-[11px] text-ink-400">
                          {mine && room.type === "GROUP" && <span className="text-orange">{Math.max(0, room.memberIds.length - 1 - (m.readBy?.length ?? 0))}</span>}
                          {formatTime(m.at)}
                        </span>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          ))}
          <div ref={bottom} />
        </div>

        <form
          onSubmit={(e) => {
            e.preventDefault();
            send();
          }}
          className="flex items-center gap-3 border-t border-line-200 px-5 py-4"
        >
          <input value={text} onChange={(e) => setText(e.target.value)} disabled={!canSend} placeholder={canSend ? "메시지를 입력하세요 (Enter 전송)" : isRequestPending ? "상대가 수락하면 메시지를 보낼 수 있어요" : "메시지를 보낼 수 없어요"} className="h-12 flex-1 rounded-full border border-line-300 px-5 text-[14px] outline-none placeholder:text-ink-400 focus:border-ink-800 disabled:bg-bg-100" maxLength={1000} />
          <button type="submit" aria-label="전송" disabled={!canSend || !text.trim()} className="grid size-12 place-items-center rounded-full bg-navy text-white disabled:bg-line-300">
            <Glyph name="send" size={20} />
          </button>
        </form>
        {text.length > 900 && <p className="px-5 pb-2 text-right text-[11px] text-ink-400">{text.length}/1000 · 1000자까지 보낼 수 있어요</p>}
      </section>

      {/* Right panel (T-32 states) */}
      <aside className="flex flex-col border-l border-line-200">
        {room.type === "GROUP" ? (
          <div className="flex flex-col gap-3 border-b border-line-200 p-5">
            <button type="button" onClick={() => setMembers((m) => !m)} className="flex items-center justify-between t-label1 text-ink-900">
              참여자 {room.memberIds.length}명 <Glyph name={members ? "chevron-up" : "chevron-down"} size={14} />
            </button>
            <ul className="flex flex-col gap-3">
              {room.memberIds.map((id) => {
                const a = alumni.find((x) => x.id === id);
                if (!a) return null;
                const isHost = chat?.receiverId === id;
                const isRep = chat?.requesterId === id;
                return (
                  <li key={id} className="flex items-center justify-between">
                    <Link href={id === ME_ID ? "/me" : `/alumni/${id}`} className="flex items-center gap-2.5 hover:underline">
                      <span className="relative">
                        <Avatar name={a.name} part={a.part} size={32} />
                        {isRep && <span className="absolute -bottom-0.5 -right-0.5 text-[10px]">👑</span>}
                        {id === ME_ID && <span className="absolute -left-1 -top-1 rounded-full bg-ink-700 px-1 text-[9px] text-white">나</span>}
                      </span>
                      <span className="text-[14px] font-semibold text-ink-900">
                        {a.name}
                        {isHost && " (선배)"}
                      </span>
                    </Link>
                    <span className="text-[12px] text-ink-500">{a.generation}기</span>
                  </li>
                );
              })}
            </ul>
            <p className="text-[12px] leading-5 text-ink-500">
              초대로 인원을 추가할 수 없어요
              <br />
              나간 사람은 다시 들어올 수 없어요
            </p>
          </div>
        ) : (
          other && (
            <div className="flex flex-col gap-3 border-b border-line-200 p-5">
              <p className="text-[13px] text-ink-500">상대 프로필</p>
              <div className="flex items-center gap-3">
                <Avatar name={other.name} part={other.part} size={64} />
                <div className="flex flex-col gap-1">
                  <p className="flex items-center gap-2">
                    <span className="text-[16px] font-bold text-ink-900">{other.name}</span>
                    <PartTag part={other.part} generation={other.generation} className="px-1.5 py-0.5 text-[11px]" />
                  </p>
                  <p className="text-[13px] text-ink-700">{other.intro}</p>
                </div>
              </div>
              <Button variant="secondary" full href={`/alumni/${other.id}`}>
                프로필 보기
              </Button>
            </div>
          )
        )}

        <div className="flex flex-col gap-3 border-b border-line-200 p-5">
          <p className="text-[13px] text-ink-500">이 대화의 커피챗</p>
          {chat ? (
            <>
              <div className="flex items-center gap-2">
                <StatusBadge tone={statusTone(chat.status)}>{COFFEE_STATUS_LABEL[chat.status]}</StatusBadge>
                {chat.status === "SCHEDULED" && chat.scheduledAt && <span className="text-[12px] text-ink-500">D-{Math.max(0, Math.ceil((new Date(chat.scheduledAt).getTime() - Date.now()) / 86_400_000))}</span>}
                {chat.status === "ACCEPTED" && <span className="text-[12px] text-ink-500">일정 미정</span>}
              </div>
              {chat.status === "SCHEDULED" && chat.scheduledAt ? (
                <div>
                  <p className="text-[20px] font-bold text-ink-900">{formatDateTime(chat.scheduledAt)}</p>
                  <p className="text-[13px] text-ink-700">{chat.mode === "ONLINE" ? "온라인" : "강남역 ○○카페 · 오프라인"}</p>
                </div>
              ) : chat.status === "ACCEPTED" ? (
                <p className="text-[13px] text-ink-700">일정 후보 {chat.candidates.map(formatDateTime).join(" · ") || "없음"}</p>
              ) : null}
              <div className="flex gap-2">
                <Button size="M" variant="secondary" className="flex-1" href={`/coffee/${chat.id}`}>
                  상세 보기
                </Button>
                {(chat.status === "ACCEPTED" || chat.status === "SCHEDULED") && (
                  <Button size="M" variant={chat.status === "ACCEPTED" ? "primary" : "secondary"} className="flex-1" href={`/coffee/${chat.id}`}>
                    {chat.status === "ACCEPTED" ? "일정 확정" : "일정 변경"}
                  </Button>
                )}
              </div>
              {chat.questions.length > 0 && (
                <div>
                  <button type="button" onClick={() => setQuestionsOpen((o) => !o)} className="flex w-full items-center justify-between text-[14px] font-semibold text-ink-900">
                    미리 드린 질문 ({chat.questions.length}) <span className="text-[12px] font-normal text-ink-500">{questionsOpen ? "▴ 접기" : "▾ 펼치기"}</span>
                  </button>
                  {questionsOpen && (
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
          ) : pastChats.length > 0 ? (
            <Link href="/coffee" className="flex items-center justify-between text-[14px] text-ink-900 hover:underline">
              지난 커피챗 {pastChats.length}건 <Glyph name="chevron-right" size={14} />
            </Link>
          ) : (
            <p className="text-[13px] text-ink-500">아직 커피챗이 없어요</p>
          )}
        </div>

        <div className="flex flex-col gap-3 p-5">
          <p className="text-[13px] text-ink-500">채팅방 설정</p>
          <div className="flex items-center justify-between text-[14px] text-ink-900">
            상단 고정 <Switch checked={pinned} onChange={setPinned} label="상단 고정" />
          </div>
          <div className="flex items-center justify-between text-[14px] text-ink-900">
            알림 끄기 <Switch checked={muted} onChange={setMuted} label="알림 끄기" />
          </div>
          <button type="button" onClick={() => (lockedLeave ? toast("진행 중인 커피챗이 있어 나갈 수 없어요", { kind: "error" }) : setLeave(true))} className={cn("w-fit text-[14px]", lockedLeave ? "text-ink-400" : "text-orange hover:underline")}>
            나가기
          </button>
        </div>
      </aside>

      {/* CF-9 / CF-9b 나가기 확인 */}
      <ConfirmModal
        open={leave}
        onClose={() => setLeave(false)}
        onConfirm={() => {
          setLeave(false);
          leaveRoom(room.id);
          toast("채팅방을 나갔어요");
          router.push("/chat");
        }}
        title="나가면 대화 내용이 모두 지워져요"
        description={isMemberOfGroup ? "나가면 이 커피챗에서도 빠져요. 다시 들어올 수 없어요." : "나간 뒤에는 다시 볼 수 없어요."}
        confirmLabel="나가기"
        danger
      />
    </div>
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
