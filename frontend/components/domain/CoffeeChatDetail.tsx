"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { cn } from "@/lib/cn";
import type { CoffeeChat, CoffeeChatRejectReason, CoffeeChatStatus } from "@/lib/types";
import { COFFEE_REJECT_LABEL, COFFEE_STATUS_LABEL, LION_PART_LABEL, PROFILE_STATUS_LABEL } from "@/lib/types";
import { useStore } from "@/lib/store";
import { ME_ID } from "@/lib/mock/users";
import { commonalities, timeLeft } from "@/lib/common";
import { formatDate, formatDateTime } from "@/lib/format";
import { Avatar } from "@/components/ui/Avatar";
import { StatusBadge, Tag } from "@/components/ui/Badge";
import { Button, Chip } from "@/components/ui/Button";
import { Glyph } from "@/components/ui/Icon";
import { Checkbox, Input, Radio, Select, Textarea } from "@/components/ui/Input";
import { ConfirmModal, Modal } from "@/components/ui/Modal";
import { ProtoButton } from "@/components/ui/Misc";
import { useToast } from "@/components/ui/Toast";

const STEPS: { key: CoffeeChatStatus | "REQUEST"; label: string }[] = [
  { key: "REQUEST", label: "신청" },
  { key: "ACCEPTED", label: "수락" },
  { key: "SCHEDULED", label: "일정 확정" },
  { key: "COMPLETED", label: "완료" },
];
const STEP_INDEX: Record<CoffeeChatStatus, number> = { PENDING: 0, ACCEPTED: 1, SCHEDULED: 2, COMPLETED: 3, REJECTED: 0, CANCELED: 0, EXPIRED: 0 };

export const statusTone = (s: CoffeeChatStatus) => (s === "PENDING" ? "outline" : s === "SCHEDULED" || s === "COMPLETED" ? "filled" : "gray");

/**
 * 커피챗 상세 (D-01~D-07, D-06b). Rendered inside M-01/M-02 (compact) and as the full page (D-01).
 * Buttons follow the ActionCard 역할 × 상태표 (D-08).
 */
export function CoffeeChatDetail({ chat, compact, initialAction }: { chat: CoffeeChat; compact?: boolean; initialAction?: "accept" | "reject" | null }) {
  const router = useRouter();
  const toast = useToast();
  const { alumni, acceptCoffeeChat, rejectCoffeeChat, cancelCoffeeChat, scheduleCoffeeChat, completeCoffeeChat, setSettings } = useStore();
  const me = alumni.find((a) => a.id === ME_ID)!;
  const isReceiver = chat.receiverId === ME_ID;
  const other = alumni.find((a) => a.id === (isReceiver ? chat.requesterId : chat.receiverId));
  const members = chat.memberIds.map((id) => alumni.find((a) => a.id === id)).filter(Boolean);
  const group = members.length > 0;
  const [modal, setModal] = useState<"accept" | "reject" | "schedule" | "cancel" | "complete" | "cancelRequest" | null>(initialAction ?? null);
  const [pastMeeting, setPastMeeting] = useState(false);

  useEffect(() => {
    if (initialAction) setModal(initialAction);
  }, [initialAction, chat.id]);

  if (!other) return null;
  const left = timeLeft(chat.expiresAt);
  const step = STEP_INDEX[chat.status];
  const ended = ["REJECTED", "CANCELED", "EXPIRED"].includes(chat.status);
  const meetingPassed = chat.status === "SCHEDULED" && (pastMeeting || (chat.scheduledAt && new Date(chat.scheduledAt).getTime() < Date.now()));
  const daysToMeeting = chat.scheduledAt ? Math.ceil((new Date(chat.scheduledAt).getTime() - Date.now()) / 86_400_000) : 0;

  const subline =
    chat.status === "PENDING" ? `${left.text} · ${chat.expiresAt ? formatDate(chat.expiresAt) : ""} 만료` : chat.status === "ACCEPTED" ? `${formatDate(chat.createdAt)} 수락` : chat.status === "SCHEDULED" ? (meetingPassed ? "만남 시각이 지났어요" : `만남까지 ${daysToMeeting}일`) : chat.status === "COMPLETED" ? `${chat.scheduledAt ? formatDate(chat.scheduledAt) : formatDate(chat.createdAt)} 완료` : formatDate(chat.createdAt);

  const actions = (
    <div className={cn("flex flex-col gap-2", compact ? "" : "")}>
      {chat.status === "PENDING" && isReceiver && (
        <div className="flex gap-2">
          <Button variant="secondary" className="flex-1" onClick={() => setModal("reject")}>
            거절하기
          </Button>
          <Button className="flex-1" onClick={() => setModal("accept")}>
            수락하기
          </Button>
        </div>
      )}
      {chat.status === "PENDING" && !isReceiver && (
        <Button variant="secondary" onClick={() => setModal("cancelRequest")}>
          신청 취소
        </Button>
      )}
      {(chat.status === "ACCEPTED" || chat.status === "SCHEDULED") && (
        <>
          {meetingPassed && <Button onClick={() => setModal("complete")}>만남 완료</Button>}
          <div className="flex gap-2">
            <Button variant="secondary" className="flex-1" href={chat.roomId ? `/chat/${chat.roomId}` : "/chat"}>
              채팅으로 이동
            </Button>
            <Button variant={chat.status === "ACCEPTED" ? "primary" : "secondary"} className="flex-1" onClick={() => setModal("schedule")}>
              {chat.status === "ACCEPTED" ? "일정 확정하기" : "일정 변경"}
            </Button>
          </div>
          <button type="button" onClick={() => setModal("cancel")} className="py-1 text-[13px] text-ink-500 hover:text-orange">
            커피챗 취소
          </button>
          {chat.status === "SCHEDULED" && !meetingPassed && <ProtoButton onClick={() => setPastMeeting(true)}>만남 시각이 지난 상태로</ProtoButton>}
        </>
      )}
      {chat.status === "COMPLETED" && (
        <Button variant="secondary" href={chat.roomId ? `/chat/${chat.roomId}` : "/chat"}>
          채팅으로 이동
        </Button>
      )}
    </div>
  );

  const statusCard = (
    <div className={cn("flex flex-col gap-4", !compact && "rounded-[12px] border border-line-200 bg-white p-6")}>
      <div className="flex items-center justify-between">
        <StatusBadge tone={statusTone(chat.status)}>{COFFEE_STATUS_LABEL[chat.status]}</StatusBadge>
        <span className={cn("text-[13px]", left.urgent && chat.status === "PENDING" ? "font-semibold text-orange" : "text-ink-700", !compact && "font-bold text-[15px]")}>{subline}</span>
      </div>
      {/* Stepper */}
      <ol className="flex items-center">
        {STEPS.map((s, i) => {
          const done = !ended && i <= step;
          const skipped = chat.status === "COMPLETED" && s.key === "SCHEDULED" && !chat.scheduledAt;
          return (
            <li key={s.key} className={cn("flex items-center", i < STEPS.length - 1 && "flex-1")}>
              <div className="flex flex-col items-center gap-1">
                <span className={cn("grid size-6 place-items-center rounded-full border-2", done ? "border-ink-800 bg-ink-800 text-white" : "border-line-300 bg-white")}>{done && <Glyph name="check" size={12} />}</span>
                <span className={cn("whitespace-nowrap text-[11px]", done ? "font-semibold text-ink-900" : "text-ink-400")}>
                  {s.label}
                  {skipped && " (건너뜀)"}
                </span>
              </div>
              {i < STEPS.length - 1 && <span className={cn("mx-2 mb-4 h-px flex-1", !ended && i < step ? "bg-ink-800" : "bg-line-300")} />}
            </li>
          );
        })}
      </ol>

      {/* State-specific cards */}
      {chat.status === "ACCEPTED" && (
        <div className="flex flex-col gap-1 rounded-[8px] bg-bg-50 p-4 text-[13px]">
          <p className="text-ink-500">수락하며 보낸 일정 후보</p>
          <p className="font-semibold text-ink-900">{chat.candidates.length ? chat.candidates.map(formatDateTime).join(" · ") : "후보 없음"}</p>
          {chat.rejectNote && <p className="text-ink-700">한마디 · {chat.rejectNote}</p>}
          <p className="mt-1 text-[12px] text-ink-400">수락 후 30일 동안 일정 확정이 없으면 자동 완료돼요.</p>
        </div>
      )}
      {chat.status === "SCHEDULED" && chat.scheduledAt && (
        <div className="flex flex-col gap-1 rounded-[8px] border border-line-200 p-4 text-[13px]">
          <p className="flex items-center gap-1 text-ink-500">
            <Glyph name="calendar" size={14} /> 확정된 일정
          </p>
          <p className="text-[15px] font-bold text-ink-900">
            📅 {formatDateTime(chat.scheduledAt)}
            {chat.mode !== "ONLINE" && " · 강남역 ○○카페"}
          </p>
          <p className="text-ink-500">
            {chat.mode === "ONLINE" ? "온라인" : "오프라인"} · {meetingPassed ? "만남 시각이 지났어요" : `만남까지 ${daysToMeeting}일`}
          </p>
        </div>
      )}
      {chat.status === "COMPLETED" && !chat.scheduledAt && (
        <div className="rounded-[8px] bg-bg-50 p-4 text-[13px] text-ink-700">
          <p className="font-semibold text-ink-900">일정 기록 없이 30일이 지나 완료 처리됐어요</p>
          <p className="text-ink-500">수락 후 30일 동안 일정 확정이 없어 자동 완료됐어요.</p>
        </div>
      )}
      {actions}
    </div>
  );

  const body = (
    <div className="flex flex-col gap-6">
      {/* Counterpart */}
      <div className={cn("flex items-center justify-between gap-4", !compact && "rounded-[12px] border border-line-200 bg-white p-6")}>
        <div className="flex items-center gap-4">
          <Avatar name={other.name} part={other.part} size={48} />
          <div className="flex flex-col gap-0.5">
            <p className="flex items-center gap-2">
              <span className="text-[18px] font-bold text-ink-900">{other.name}</span>
              <span className="text-[13px] text-ink-500">
                {other.generation}기 · {LION_PART_LABEL[other.part]}
              </span>
            </p>
            <p className="text-[14px] text-ink-700">
              {PROFILE_STATUS_LABEL[other.status]}
              {other.status === "EMPLOYED" || other.status === "FOUNDER" ? ` · ${other.headline}` : other.interests[0] ? ` · 관심 분야 ${other.interests[0]}` : ""}
            </p>
            {!compact && (
              <p className="flex flex-wrap items-center gap-1.5 text-[12px] text-ink-500">
                나와의 공통점
                {commonalities(me, other).length ? commonalities(me, other).map((c) => <Tag key={c}>{c}</Tag>) : <span className="text-ink-400">아직 없어요</span>}
              </p>
            )}
          </div>
        </div>
        {compact ? (
          <Link href={`/coffee/${chat.id}`} className="t-caption1 whitespace-nowrap text-ink-500 underline underline-offset-2">
            전체 화면으로 보기 ↗
          </Link>
        ) : (
          <Button variant="secondary" href={`/alumni/${other.id}`}>
            프로필 보기
          </Button>
        )}
      </div>

      {compact && statusCard}

      {/* D-06 감사 메시지 · D-07 거절 메시지 */}
      {chat.status === "COMPLETED" && chat.rejectNote && (
        <div className="rounded-[8px] bg-part-be-bg/50 p-4 text-[14px]">
          <p className="t-label2 text-primary">{other.name}님이 남긴 감사 메시지</p>
          <p className="mt-1 text-ink-900">{chat.rejectNote}</p>
        </div>
      )}
      {chat.status === "REJECTED" && !isReceiver && (
        <div className="rounded-[8px] bg-bg-50 p-4 text-[14px]">
          <p className="font-semibold text-ink-900">
            {other.name} 선배가 이번에는 어렵다고 했어요. 사유: {chat.rejectReason ? COFFEE_REJECT_LABEL[chat.rejectReason] : "—"}
          </p>
          {chat.rejectNote && (
            <p className="mt-2 text-ink-700">
              <span className="text-ink-500">선배의 메시지</span> · {chat.rejectNote}
            </p>
          )}
        </div>
      )}

      {group && (
        <Field label={`신청자 ${members.length + 1}명`}>
          <ul className="flex flex-col gap-1 text-[14px] text-ink-900">
            <li>
              {isReceiver ? other.name : me.name} · {isReceiver ? other.generation : me.generation}기 (대표{!isReceiver && ", 나"})
            </li>
            {members.map((m) => (
              <li key={m!.id}>
                {m!.name} · {m!.generation}기
              </li>
            ))}
          </ul>
        </Field>
      )}
      <Field label="상담 주제">
        <p className="text-[15px] font-semibold text-ink-900">{chat.topic}</p>
      </Field>
      <Field label="신청 이유">
        <p className="whitespace-pre-line text-[15px] leading-6 text-ink-900">{chat.message}</p>
      </Field>
      {chat.questions.length > 0 && (
        <Field label="미리 드리는 질문">
          <ol className="flex flex-col gap-1.5 text-[15px] text-ink-900">
            {chat.questions.map((q, i) => (
              <li key={i}>
                {i + 1}. {q}
              </li>
            ))}
          </ol>
        </Field>
      )}
      <div className={cn("grid gap-3", compact ? "grid-cols-2" : "grid-cols-1 rounded-[8px] bg-bg-50 p-4")}>
        <KV k="희망 방식" v={chat.mode === "ONLINE" ? "온라인" : chat.mode === "OFFLINE" ? "오프라인" : "상관없음"} boxed={compact} />
        <KV k="희망 일정 메모" v="평일 저녁 좋아요" boxed={compact} />
      </div>
    </div>
  );

  return (
    <>
      {compact ? body : (
        <div className="grid grid-cols-[1fr_384px] gap-6">
          {body}
          <div className="h-fit">{statusCard}</div>
        </div>
      )}

      {/* ★[O] D-10 수락 모달 / 일정 후보 */}
      <AcceptModal
        open={modal === "accept"}
        onClose={() => setModal(null)}
        group={group}
        onConfirm={(cands, note) => {
          const roomId = acceptCoffeeChat(chat.id, cands);
          setModal(null);
          toast("수락했어요. 채팅방이 열렸어요.");
          router.push(`/chat/${roomId}`);
          void note;
        }}
      />
      {/* ★[O] D-11 사유 선택 모달 / 커피챗 거절 */}
      <RejectModal
        open={modal === "reject"}
        onClose={() => setModal(null)}
        onConfirm={(reason, note, rest) => {
          rejectCoffeeChat(chat.id, reason, note);
          if (rest) setSettings({ mode: "RESTING", restUntil: rest });
          setModal(null);
          toast("거절했어요. 신청자에게 안내가 전달돼요.");
        }}
      />
      {/* ◇[O] D-15 일정 확정 · 변경 */}
      <ScheduleModal open={modal === "schedule"} onClose={() => setModal(null)} candidates={chat.candidates} initial={chat.scheduledAt} mode={chat.mode} onConfirm={(at) => { scheduleCoffeeChat(chat.id, at); setModal(null); setPastMeeting(false); toast("일정을 확정했어요"); }} />
      {/* ◇[O] D-16 커피챗 취소 */}
      <CancelModal open={modal === "cancel"} onClose={() => setModal(null)} onConfirm={(note) => { cancelCoffeeChat(chat.id, note); setModal(null); toast("커피챗을 취소했어요"); }} />
      {/* ◇[O] D-17 만남 완료 · 감사 메시지 */}
      <ThanksModal open={modal === "complete"} onClose={() => setModal(null)} name={other.name} onConfirm={(t) => { completeCoffeeChat(chat.id, t); setModal(null); toast("커피챗을 완료했어요 ☕"); }} />
      {/* ◇[O] CF-7 신청 취소 확인 */}
      <ConfirmModal open={modal === "cancelRequest"} onClose={() => setModal(null)} onConfirm={() => { cancelCoffeeChat(chat.id); setModal(null); toast("신청을 취소했어요"); }} title="신청을 취소할까요?" description="취소하면 선배에게 보낸 신청서가 사라져요. 같은 선배에게 다시 신청할 수 있어요." confirmLabel="신청 취소" />
    </>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex flex-col gap-1.5">
      <p className="text-[13px] text-ink-500">{label}</p>
      {children}
    </div>
  );
}
function KV({ k, v, boxed }: { k: string; v: string; boxed?: boolean }) {
  return (
    <div className={cn("flex items-center justify-between text-[13px]", boxed && "rounded-[8px] bg-bg-50 px-4 py-3")}>
      <span className="text-ink-500">{k}</span>
      <span className="font-bold text-ink-900">{v}</span>
    </div>
  );
}

/* ---------- Modals ---------- */

function nextDays(n = 14) {
  return Array.from({ length: n }, (_, i) => {
    const d = new Date();
    d.setDate(d.getDate() + i + 1);
    d.setHours(0, 0, 0, 0);
    return d;
  });
}
const TIMES = Array.from({ length: 28 }, (_, i) => `${String(8 + Math.floor(i / 2)).padStart(2, "0")}:${i % 2 ? "30" : "00"}`);
const DAY = ["일", "월", "화", "수", "목", "금", "토"];
const dateLabel = (d: Date) => `${d.getMonth() + 1}/${d.getDate()}(${DAY[d.getDay()]})`;
const combine = (date: string, time: string) => {
  const d = new Date(date);
  const [h, m] = time.split(":").map(Number);
  d.setHours(h, m, 0, 0);
  return d.toISOString();
};

function AcceptModal({ open, onClose, onConfirm, group }: { open: boolean; onClose: () => void; onConfirm: (candidates: string[], note: string) => void; group: boolean }) {
  const days = nextDays();
  const [rows, setRows] = useState([{ date: days[2].toISOString(), time: "19:00" }]);
  const [note, setNote] = useState("");
  const past = rows.some((r) => new Date(combine(r.date, r.time)).getTime() < Date.now());
  return (
    <Modal
      open={open}
      onClose={onClose}
      title="커피챗 수락"
      width={480}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            취소
          </Button>
          <Button disabled={rows.length === 0 || past || note.length > 200} onClick={() => onConfirm(rows.map((r) => combine(r.date, r.time)), note)}>
            수락 보내기
          </Button>
        </>
      }
    >
      <div className="flex flex-col gap-4">
        <div className="flex flex-col gap-2.5">
          <p className="t-label2 text-ink-700">
            가능한 일정 후보 * <span className="font-normal text-ink-400">1~3개 · 30분 단위</span>
          </p>
          {rows.map((r, i) => (
            <div key={i} className="flex items-center gap-2">
              <Select value={r.date} onChange={(e) => setRows((s) => s.map((x, j) => (j === i ? { ...x, date: e.target.value } : x)))}>
                {days.map((d) => (
                  <option key={d.toISOString()} value={d.toISOString()}>
                    {dateLabel(d)}
                  </option>
                ))}
              </Select>
              <Select value={r.time} onChange={(e) => setRows((s) => s.map((x, j) => (j === i ? { ...x, time: e.target.value } : x)))}>
                {TIMES.map((t) => (
                  <option key={t}>{t}</option>
                ))}
              </Select>
              <button type="button" aria-label="삭제" disabled={rows.length === 1} onClick={() => setRows((s) => s.filter((_, j) => j !== i))} className="text-ink-400 hover:text-ink-900 disabled:opacity-30">
                ✕
              </button>
            </div>
          ))}
          {past && <p className="t-caption2 text-orange">지난 시간은 고를 수 없어요</p>}
          {rows.length < 3 && (
            <Button size="S" variant="secondary" className="w-fit" onClick={() => setRows((s) => [...s, { date: days[Math.min(days.length - 1, s.length + 2)].toISOString(), time: "19:30" }])}>
              + 후보 추가
            </Button>
          )}
        </div>
        <Textarea label="한마디 (선택)" rows={3} placeholder="예) 반가워요! 편한 시간 골라주세요." value={note} onChange={(e) => setNote(e.target.value)} counter={{ value: note.length, max: 200 }} />
        {group && <p className="rounded-[8px] border border-dashed border-ink-400 px-3.5 py-2.5 text-[12px] text-ink-500">여러 명 커피챗이에요. 수락하면 함께 신청한 동문까지 단체 채팅방(T-27)이 열려요.</p>}
      </div>
    </Modal>
  );
}

function RejectModal({ open, onClose, onConfirm }: { open: boolean; onClose: () => void; onConfirm: (reason: CoffeeChatRejectReason, note: string, restUntil?: string) => void }) {
  const [reason, setReason] = useState<CoffeeChatRejectReason>("NO_TIME");
  const [note, setNote] = useState("");
  const [rest, setRest] = useState(true);
  const [period, setPeriod] = useState<"1w" | "2w" | "1m" | "custom">("1w");
  const [custom, setCustom] = useState("");
  const until = () => {
    if (!rest) return undefined;
    if (period === "custom") return custom || undefined;
    const d = new Date();
    d.setDate(d.getDate() + (period === "1w" ? 7 : period === "2w" ? 14 : 30));
    return d.toISOString().slice(0, 10);
  };
  return (
    <Modal
      open={open}
      onClose={onClose}
      title="커피챗 거절"
      width={480}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            취소
          </Button>
          <Button onClick={() => onConfirm(reason, note, until())} disabled={note.length > 200 || (rest && period === "custom" && !custom)}>
            거절하기
          </Button>
        </>
      }
    >
      <div className="flex flex-col gap-4">
        <div className="flex flex-col gap-2.5">
          <p className="t-label2 text-ink-700">사유 *</p>
          {(Object.keys(COFFEE_REJECT_LABEL) as CoffeeChatRejectReason[]).map((r) => (
            <Radio key={r} name="reason" label={COFFEE_REJECT_LABEL[r]} checked={reason === r} onChange={() => setReason(r)} />
          ))}
        </div>
        <Textarea label="메시지 (선택)" rows={3} placeholder="예) 다음에 꼭 이야기 나눠요!" value={note} onChange={(e) => setNote(e.target.value)} counter={{ value: note.length, max: 200 }} />
        <div className="flex flex-col gap-3 rounded-[8px] border border-line-200 bg-bg-50 p-4">
          <Checkbox label="당분간 커피챗 쉬기" checked={rest} onChange={(e) => setRest(e.target.checked)} />
          {rest && (
            <div className="flex flex-wrap items-center gap-2">
              <Chip selected={period === "1w"} onClick={() => setPeriod("1w")}>
                오늘부터 1주
              </Chip>
              <Chip selected={period === "2w"} onClick={() => setPeriod("2w")}>
                2주
              </Chip>
              <Chip selected={period === "1m"} onClick={() => setPeriod("1m")}>
                1달
              </Chip>
              <Chip selected={period === "custom"} onClick={() => setPeriod("custom")}>
                직접 선택
              </Chip>
              {period === "custom" && <input type="date" value={custom} onChange={(e) => setCustom(e.target.value)} className="h-8 rounded-[6px] border border-line-300 px-2 text-[13px]" />}
            </div>
          )}
          <p className="text-[12px] text-ink-500">쉬는 기간은 최대 5개까지 등록할 수 있어요.</p>
        </div>
      </div>
    </Modal>
  );
}

function ScheduleModal({ open, onClose, onConfirm, candidates, initial, mode }: { open: boolean; onClose: () => void; onConfirm: (at: string) => void; candidates: string[]; initial?: string; mode: string }) {
  const days = nextDays(30);
  const [date, setDate] = useState(initial ? new Date(new Date(initial).setHours(0, 0, 0, 0)).toISOString() : candidates[0] ? new Date(new Date(candidates[0]).setHours(0, 0, 0, 0)).toISOString() : days[2].toISOString());
  const [time, setTime] = useState(initial ? initial.slice(11, 16) : candidates[0] ? new Date(candidates[0]).toTimeString().slice(0, 5) : "19:00");
  const [m, setM] = useState<"ONLINE" | "OFFLINE">(mode === "ONLINE" ? "ONLINE" : "OFFLINE");
  const [place, setPlace] = useState("강남역 ○○카페");
  const valid = !!date && !!time && (m === "ONLINE" || place.trim().length > 0);
  return (
    <Modal
      open={open}
      onClose={onClose}
      title={initial ? "일정 변경" : "일정 확정"}
      width={480}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            취소
          </Button>
          <Button disabled={!valid} onClick={() => onConfirm(combine(date, time))}>
            {initial ? "변경하기" : "일정 확정하기"}
          </Button>
        </>
      }
    >
      <div className="flex flex-col gap-4">
        {candidates.length > 0 && (
          <div className="flex flex-col gap-2">
            <p className="t-label2 text-ink-700">수락할 때 보낸 후보</p>
            <div className="flex flex-wrap gap-2">
              {candidates.map((c) => (
                <Chip key={c} selected={combine(date, time) === c} onClick={() => { setDate(new Date(new Date(c).setHours(0, 0, 0, 0)).toISOString()); setTime(new Date(c).toTimeString().slice(0, 5)); }}>
                  {formatDateTime(c)}
                </Chip>
              ))}
            </div>
          </div>
        )}
        <div className="grid grid-cols-2 gap-3">
          <Select label="날짜" required value={date} onChange={(e) => setDate(e.target.value)}>
            {days.map((d) => (
              <option key={d.toISOString()} value={d.toISOString()}>
                {dateLabel(d)}
              </option>
            ))}
          </Select>
          <Select label="시각" required value={time} onChange={(e) => setTime(e.target.value)}>
            {TIMES.map((t) => (
              <option key={t}>{t}</option>
            ))}
          </Select>
        </div>
        <div className="flex flex-col gap-2.5">
          <p className="t-label2 text-ink-700">방식 *</p>
          <div className="flex gap-2">
            <Chip selected={m === "ONLINE"} onClick={() => setM("ONLINE")}>
              온라인
            </Chip>
            <Chip selected={m === "OFFLINE"} onClick={() => setM("OFFLINE")}>
              오프라인
            </Chip>
          </div>
        </div>
        {m === "OFFLINE" ? <Input label="장소" required placeholder="예) 강남역 ○○카페" value={place} onChange={(e) => setPlace(e.target.value)} /> : <Input label="링크 (선택)" placeholder="예) https://meet.google.com/…" help="온라인을 고르면 장소 대신 링크를 입력해요" />}
      </div>
    </Modal>
  );
}

function CancelModal({ open, onClose, onConfirm }: { open: boolean; onClose: () => void; onConfirm: (note: string) => void }) {
  const [note, setNote] = useState("");
  return (
    <Modal
      open={open}
      onClose={onClose}
      title="커피챗 취소"
      width={480}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            닫기
          </Button>
          <Button variant="danger" disabled={note.trim().length < 5} onClick={() => onConfirm(note.trim())}>
            커피챗 취소
          </Button>
        </>
      }
    >
      <div className="flex flex-col gap-3">
        <Textarea label="취소 사유" required rows={4} placeholder="상대에게 전달돼요. 예) 급한 일정이 생겨 이번에는 어렵게 됐어요." value={note} onChange={(e) => setNote(e.target.value)} counter={{ value: note.length, max: 200 }} />
        <p className="rounded-[8px] bg-bg-50 px-4 py-3 text-[13px] text-ink-500">취소하면 상대에게 알림이 가고 채팅방에 안내가 남아요. 잦은 취소는 이용이 제한될 수 있어요.</p>
      </div>
    </Modal>
  );
}

function ThanksModal({ open, onClose, onConfirm, name }: { open: boolean; onClose: () => void; onConfirm: (text?: string) => void; name: string }) {
  const [text, setText] = useState("");
  return (
    <Modal
      open={open}
      onClose={onClose}
      title="선배에게 고마운 마음을 남겨보세요"
      description={`${name}님과의 커피챗을 완료 처리해요. 감사 메시지는 채팅방에도 남아요.`}
      width={480}
      footer={
        <>
          <Button variant="secondary" onClick={() => onConfirm()}>
            건너뛰기
          </Button>
          <Button disabled={text.trim().length === 0} onClick={() => onConfirm(text.trim())}>
            보내기
          </Button>
        </>
      }
    >
      <Textarea rows={4} placeholder="예) 덕분에 상태 관리 구조를 어떻게 잡아야 할지 감이 왔어요. 감사합니다!" value={text} onChange={(e) => setText(e.target.value)} counter={{ value: text.length, max: 300 }} />
    </Modal>
  );
}
