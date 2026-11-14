"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { cn } from "@/lib/cn";
import type { CoffeeChatRejectReason, CoffeeChatStatus } from "@/lib/types";
import { COFFEE_REJECT_LABEL, COFFEE_STATUS_LABEL, LION_PART_LABEL, PROFILE_STATUS_LABEL } from "@/lib/types";
import { useStore } from "@/lib/store";
import { ME_ID } from "@/lib/mock/users";
import { commonalities, timeLeft } from "@/lib/common";
import { formatDate, formatDateTime } from "@/lib/format";
import { API_ENABLED, errorMessage, isApiError } from "@/lib/api/client";
import { personName, topicText, type CoffeeAction, type CoffeeChatDetail as CoffeeChatData } from "@/lib/api/coffee";
import { useCoffeeService } from "@/lib/api/useCoffeeService";
import { Avatar } from "@/components/ui/Avatar";
import { StatusBadge } from "@/components/ui/Badge";
import { Button, Chip } from "@/components/ui/Button";
import { Icon } from "@/components/ui/Icon";
import { Checkbox, Input, Radio, Select, Textarea } from "@/components/ui/Input";
import { ConfirmModal, Modal } from "@/components/ui/Modal";
import { ProtoButton, Tooltip } from "@/components/ui/Misc";
import { useToast } from "@/components/ui/Toast";

const STEPS: { key: CoffeeChatStatus | "REQUEST"; label: string }[] = [
  { key: "REQUEST", label: "신청" },
  { key: "ACCEPTED", label: "수락" },
  { key: "SCHEDULED", label: "일정 확정" },
  { key: "COMPLETED", label: "완료" },
];
const STEP_INDEX: Record<CoffeeChatStatus, number> = { PENDING: 0, ACCEPTED: 1, SCHEDULED: 2, COMPLETED: 3, REJECTED: 0, CANCELED: 0, EXPIRED: 0 };
const PENDING_TIP = "곧 연결될 기능이에요";
/** 이번 작업(신청·목록·상세·신청 취소)에서 API로 연결한 동작. 나머지는 다음 이슈에서 연다 */
const API_READY: CoffeeAction[] = ["WITHDRAW"];

export const statusTone = (s: CoffeeChatStatus) => (s === "PENDING" ? "outline" : s === "SCHEDULED" || s === "COMPLETED" ? "filled" : "gray");

/**
 * 커피챗 상세 — 내 커피챗 오른쪽 영역 (M-01 · M-02 · M-04). 전체 화면 상세(D-01~07)는 디자인에서 빠졌어요.
 * 버튼은 서버가 준 actions(역할 × 상태표, D-08)대로 보여준다.
 * onChanged: 상태가 바뀌면 부른다 (바뀐 상세를 주면 그걸로, 아니면 다시 불러온다).
 */
export function CoffeeChatDetail({ chat, initialAction, onChanged }: { chat: CoffeeChatData; compact?: boolean; initialAction?: "accept" | "reject" | null; onChanged?: (next?: CoffeeChatData) => void }) {
  const router = useRouter();
  const toast = useToast();
  const service = useCoffeeService();
  const { alumni, acceptCoffeeChat, rejectCoffeeChat, cancelCoffeeChat, scheduleCoffeeChat, completeCoffeeChat, setSettings } = useStore();
  const isReceiver = chat.myRole === "RECEIVER";
  const other = isReceiver ? chat.requester : chat.receiver;
  const members = chat.members;
  const group = members.length > 0;
  const can = (a: CoffeeAction) => chat.actions.includes(a);
  const ready = (a: CoffeeAction) => !API_ENABLED || API_READY.includes(a);
  const [modal, setModal] = useState<"accept" | "reject" | "schedule" | "cancel" | "complete" | "cancelRequest" | null>(initialAction && ready(initialAction === "accept" ? "ACCEPT" : "REJECT") ? initialAction : null);
  const [pastMeeting, setPastMeeting] = useState(false);
  const [busy, setBusy] = useState(false);

  // 프로토타입 모드 전용: mock 동문으로 '현재 상태·나와의 공통점'을 보여준다 (API 상세에는 없는 값)
  const me = API_ENABLED ? undefined : alumni.find((a) => a.id === ME_ID);
  const mockOther = API_ENABLED || other.id === null ? undefined : alumni.find((a) => a.id === String(other.id));
  const common = me && mockOther ? commonalities(me, mockOther) : null;

  const part = other.lionParts?.[0] ?? "ETC";
  const left = timeLeft(chat.expiresAt ?? undefined);
  const step = STEP_INDEX[chat.status];
  const ended = ["REJECTED", "CANCELED", "EXPIRED"].includes(chat.status);
  const meetingPassed = chat.status === "SCHEDULED" && (pastMeeting || can("COMPLETE") || (!!chat.scheduledAt && new Date(chat.scheduledAt).getTime() < Date.now()));
  const daysToMeeting = chat.scheduledAt ? Math.ceil((new Date(chat.scheduledAt).getTime() - Date.now()) / 86_400_000) : 0;

  const subline =
    chat.status === "PENDING"
      ? `${left.text} · ${chat.expiresAt ? formatDate(chat.expiresAt) : ""} 만료`
      : chat.status === "ACCEPTED"
        ? `${formatDate(chat.acceptedAt ?? chat.createdAt)} 수락`
        : chat.status === "SCHEDULED"
          ? meetingPassed
            ? "만남 시각이 지났어요"
            : `만남까지 ${daysToMeeting}일`
          : chat.status === "COMPLETED"
            ? `${formatDate(chat.completedAt ?? chat.scheduledAt ?? chat.createdAt)} 완료`
            : formatDate(chat.canceledAt ?? chat.rejectedAt ?? chat.createdAt);

  const room = chat.roomId ? `/chat/${chat.roomId}` : "/chat";
  const modeText = (m: string | null) => (m === "ONLINE" ? "온라인" : m === "OFFLINE" ? "오프라인" : "상관없음");

  const withdraw = async () => {
    setModal(null);
    setBusy(true);
    try {
      const next = await service.withdraw(chat.id);
      toast("신청을 취소했어요");
      onChanged?.(next);
    } catch (e) {
      toast(errorMessage(e), { kind: "error" });
      if (isApiError(e) && e.status === 409) onChanged?.();
    } finally {
      setBusy(false);
    }
  };

  /** D-08 ActionCard 역할 × 상태표 — 상태 줄 오른쪽의 작은 버튼 */
  const actions = (
    <div className="flex shrink-0 items-center gap-2">
      {can("REJECT") && <ActionBtn ready={ready} busy={busy} action="REJECT" label="거절하기" onClick={() => setModal("reject")} />}
      {can("ACCEPT") && <ActionBtn ready={ready} busy={busy} action="ACCEPT" label="수락하기" variant="primary" onClick={() => setModal("accept")} />}
      {can("WITHDRAW") && <ActionBtn ready={ready} busy={busy} action="WITHDRAW" label="신청 취소" onClick={() => setModal("cancelRequest")} />}
      {(chat.status === "ACCEPTED" || chat.status === "SCHEDULED" || chat.status === "COMPLETED") && chat.roomId !== null && (
        <Button size="S" variant="secondary" href={room}>
          채팅으로 이동
        </Button>
      )}
      {meetingPassed && can("COMPLETE") ? (
        <ActionBtn ready={ready} busy={busy} action="COMPLETE" label="만남 완료" variant="primary" onClick={() => setModal("complete")} />
      ) : (
        can("SCHEDULE") && <ActionBtn ready={ready} busy={busy} action="SCHEDULE" label={chat.status === "ACCEPTED" ? "일정 확정하기" : "일정 변경"} variant={chat.status === "ACCEPTED" ? "primary" : "secondary"} onClick={() => setModal("schedule")} />
      )}
      {can("LEAVE") && <ActionBtn ready={ready} busy={busy} action="LEAVE" label="빠지기" onClick={() => undefined} />}
    </div>
  );

  const body = (
    <div className="flex flex-col gap-6">
      {/* 상대 */}
      <div className="flex items-center gap-2.5">
        {other.isDeleted || other.id === null ? (
          <Avatar size={48} withdrawn />
        ) : (
          <Link href={`/alumni/${other.id}`} className="shrink-0">
            <Avatar name={other.name ?? undefined} part={part} size={48} />
          </Link>
        )}
        <div className="flex min-w-0 flex-col gap-1.5">
          <p className="flex items-baseline gap-2">
            {other.isDeleted || other.id === null ? (
              <span className="t-title3 text-ink-900">{personName(other)}</span>
            ) : (
              <Link href={`/alumni/${other.id}`} className="t-title3 text-ink-900 hover:underline">
                {other.name}
                {group && isReceiver ? ` 외 ${members.length}명` : ""}
              </Link>
            )}
            {!other.isDeleted && (
              <span className="t-caption1 text-ink-500">
                {other.generation}기{part !== "ETC" ? ` · ${LION_PART_LABEL[part]}` : ""}
              </span>
            )}
          </p>
          {mockOther && (
            <p className="t-caption1 text-ink-700">
              {PROFILE_STATUS_LABEL[mockOther.status]}
              {mockOther.status === "EMPLOYED" || mockOther.status === "FOUNDER" ? ` · ${mockOther.headline}` : mockOther.interests[0] ? ` · 관심 분야 ${mockOther.interests[0]}` : ""}
            </p>
          )}
          {common && (
            <p className="flex flex-wrap items-center gap-1.5 text-[12px] text-ink-500">
              나와의 공통점
              {common.length ? (
                common.map((c) => (
                  <span key={c} className="rounded-[4px] bg-bg-100 px-2 py-0.5 text-[12px] font-semibold text-ink-900">
                    {c}
                  </span>
                ))
              ) : (
                <span className="text-ink-400">아직 없어요</span>
              )}
            </p>
          )}
        </div>
      </div>

      {/* 상태 · 버튼 · 진행 단계 */}
      <div className="flex flex-col gap-5">
        <div className="flex items-center justify-between gap-3">
          <div className="flex min-w-0 items-center gap-2.5">
            <StatusBadge tone={statusTone(chat.status)}>{COFFEE_STATUS_LABEL[chat.status]}</StatusBadge>
            <span className={cn("truncate", chat.status === "PENDING" ? cn("t-label1", left.urgent ? "text-orange" : "text-ink-900") : "t-caption1 text-ink-500")}>{subline}</span>
          </div>
          {actions}
        </div>
        <ol className="flex items-start">
          {STEPS.map((s, i) => {
            const done = !ended && i <= step;
            const skipped = chat.status === "COMPLETED" && s.key === "SCHEDULED" && !chat.scheduledAt;
            return (
              <li key={s.key} className={cn("flex items-start", i < STEPS.length - 1 && "flex-1")}>
                <div className="flex w-[22px] flex-col items-center gap-2.5">
                  <span className={cn("grid size-[22px] place-items-center rounded-full border", done ? "border-navy bg-navy text-white" : "border-line-300 bg-white")}>{done && <Icon name="check" size={13} />}</span>
                  <span className={cn("whitespace-nowrap text-[11px] leading-[13px]", done && i === step ? "font-bold text-ink-900" : done ? "font-semibold text-ink-900" : "text-ink-400")}>
                    {s.label}
                    {skipped && " (건너뜀)"}
                  </span>
                </div>
                {i < STEPS.length - 1 && <span className={cn("mx-3 mt-[10px] h-0.5 flex-1 rounded-full", !ended && i < step ? "bg-navy" : "h-px bg-line-300")} />}
              </li>
            );
          })}
        </ol>

        {chat.status === "ACCEPTED" && (
          <div className="flex flex-col gap-1 rounded-[8px] border border-line-200 p-4 text-[13px]">
            <p className="text-ink-500">수락하며 보낸 일정 후보</p>
            <p className="t-body-bold text-ink-900">{chat.proposedSlots.length ? chat.proposedSlots.map(formatDateTime).join(" · ") : "후보 없음"}</p>
            {chat.responseMessage && <p className="mt-1 text-ink-700">“{chat.responseMessage}”</p>}
            <p className="mt-1 text-[12px] text-ink-400">수락 후 30일 동안 일정 확정이 없으면 자동 완료돼요.</p>
          </div>
        )}
        {chat.status === "SCHEDULED" && chat.scheduledAt && (
          <div className="flex flex-col gap-1.5 rounded-[8px] border border-line-200 p-4">
            <p className="flex items-center gap-1.5 text-[12px] text-ink-700">
              <Icon name="calendar" size={16} /> 확정된 일정
            </p>
            <p className="t-body-bold text-ink-900">
              📅 {formatDateTime(chat.scheduledAt)}
              {chat.meetingMode === "OFFLINE" && chat.meetingPlace && ` · ${chat.meetingPlace}`}
            </p>
            <p className="t-caption1 text-ink-700">
              {modeText(chat.meetingMode)} · {meetingPassed ? "만남 시각이 지났어요" : `만남까지 ${daysToMeeting}일`}
            </p>
            {chat.meetingMode === "ONLINE" && chat.meetingLink && (
              <a href={chat.meetingLink} target="_blank" rel="noreferrer" className="w-fit text-[13px] text-primary underline">
                {chat.meetingLink}
              </a>
            )}
          </div>
        )}
        {chat.status === "COMPLETED" && !chat.scheduledAt && (
          <div className="rounded-[8px] bg-bg-50 p-4 text-[13px] text-ink-700">
            <p className="font-semibold text-ink-900">일정 기록 없이 30일이 지나 완료 처리됐어요</p>
            <p className="text-ink-500">수락 후 30일 동안 일정 확정이 없어 자동 완료됐어요.</p>
          </div>
        )}
        {!API_ENABLED && chat.status === "SCHEDULED" && !meetingPassed && <ProtoButton onClick={() => setPastMeeting(true)}>만남 시각이 지난 상태로</ProtoButton>}
      </div>

      <hr className="border-line-200" />

      {/* 거절·취소 안내 */}
      {chat.status === "REJECTED" && !isReceiver && (
        <div className="rounded-[8px] bg-bg-50 p-4 text-[14px]">
          <p className="font-semibold text-ink-900">
            {personName(other)} 사자가 이번에는 어렵다고 했어요. 사유: {chat.rejectReason ? COFFEE_REJECT_LABEL[chat.rejectReason] : "—"}
          </p>
          {chat.responseMessage && (
            <p className="mt-2 text-ink-700">
              <span className="text-ink-500">사자의 메시지</span> · {chat.responseMessage}
            </p>
          )}
        </div>
      )}
      {chat.status === "CANCELED" && chat.cancelReason && (
        <div className="rounded-[8px] bg-bg-50 p-4 text-[14px]">
          <p className="font-semibold text-ink-900">커피챗이 취소됐어요</p>
          <p className="mt-1 text-ink-700">
            <span className="text-ink-500">취소 사유</span> · {chat.cancelReason}
          </p>
        </div>
      )}

      {group && (
        <Field label={`신청자 ${members.length + 1}명`}>
          <ul className="flex flex-col gap-1 text-[15px] text-ink-900">
            <li>
              {personName(chat.requester)} · {chat.requester.generation}기 (대표{chat.myRole === "REQUESTER" && ", 나"})
            </li>
            {members.map((m, i) => (
              <li key={m.id ?? `deleted-${i}`}>
                {personName(m)}
                {!m.isDeleted && ` · ${m.generation}기`}
              </li>
            ))}
          </ul>
        </Field>
      )}
      <Field label="상담 주제">
        <p className="text-[15px] leading-6 text-ink-900">{topicText(chat)}</p>
      </Field>
      <Field label="신청 이유">
        <p className="whitespace-pre-line text-[15px] leading-[18px] text-ink-900">{chat.reason}</p>
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
      <div className="grid grid-cols-2 gap-6">
        <KV k="희망 방식" v={modeText(chat.preferredMode)} boxed />
        <KV k="희망 일정 메모" v={chat.scheduleNote || "—"} boxed />
      </div>
      {can("CANCEL") &&
        (ready("CANCEL") ? (
          <button type="button" onClick={() => setModal("cancel")} className="mx-auto py-2 t-label1 font-medium text-ink-500 hover:text-orange">
            커피챗 취소
          </button>
        ) : (
          <Tooltip text={PENDING_TIP} className="mx-auto">
            <span className="py-2 t-label1 font-medium text-ink-400">커피챗 취소</span>
          </Tooltip>
        ))}
    </div>
  );

  // 아래 모달들은 프로토타입 모드에서 mock store로 동작한다 (API 연결은 다음 이슈)
  const mockId = String(chat.id);
  return (
    <>
      {body}

      {/* ★[O] D-10 수락 모달 / 일정 후보 */}
      <AcceptModal
        open={modal === "accept"}
        onClose={() => setModal(null)}
        group={group}
        onConfirm={(cands, note) => {
          const roomId = acceptCoffeeChat(mockId, cands);
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
          rejectCoffeeChat(mockId, reason, note);
          if (rest) setSettings({ mode: "RESTING", restUntil: rest });
          setModal(null);
          toast("거절했어요. 신청자에게 안내가 전달돼요.");
        }}
      />
      {/* ◇[O] D-15 일정 확정 · 변경 */}
      <ScheduleModal open={modal === "schedule"} onClose={() => setModal(null)} candidates={chat.proposedSlots} initial={chat.scheduledAt ?? undefined} mode={chat.meetingMode ?? chat.preferredMode} onConfirm={(at) => { scheduleCoffeeChat(mockId, at); setModal(null); setPastMeeting(false); toast("일정을 확정했어요"); }} />
      {/* ◇[O] D-16 커피챗 취소 */}
      <CancelModal open={modal === "cancel"} onClose={() => setModal(null)} onConfirm={(note) => { cancelCoffeeChat(mockId, note); setModal(null); toast("커피챗을 취소했어요"); }} />
      {/* ◇[O] D-17 만남 완료 · 감사 메시지 */}
      <ThanksModal open={modal === "complete"} onClose={() => setModal(null)} name={personName(other)} onConfirm={(t) => { completeCoffeeChat(mockId, t); setModal(null); toast("커피챗을 완료했어요 ☕"); }} />
      {/* ◇[O] CF-7 신청 취소 확인 (COFFEE-06) */}
      <ConfirmModal open={modal === "cancelRequest"} onClose={() => setModal(null)} onConfirm={withdraw} title="신청을 취소할까요?" description="취소하면 사자에게 보낸 신청서가 사라져요. 같은 사자에게 다시 신청할 수 있어요." confirmLabel="신청 취소" />
    </>
  );
}

/** 아직 API를 연결하지 않은 동작은 잠근 버튼으로 보여준다 */
function ActionBtn({ ready, busy, action, label, variant = "secondary", onClick }: { ready: (a: CoffeeAction) => boolean; busy: boolean; action: CoffeeAction; label: string; variant?: "primary" | "secondary"; onClick: () => void }) {
  if (ready(action)) {
    return (
      <Button size="S" variant={variant} onClick={onClick} loading={busy && action === "WITHDRAW"}>
        {label}
      </Button>
    );
  }
  return (
    <Tooltip text={PENDING_TIP}>
      <Button size="S" variant={variant} disabled>
        {label}
      </Button>
    </Tooltip>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex flex-col gap-1.5">
      <p className="t-label1 text-ink-900">{label}</p>
      {children}
    </div>
  );
}
function KV({ k, v, boxed }: { k: string; v: string; boxed?: boolean }) {
  return (
    <div className={cn("flex items-center justify-between text-[13px]", boxed && "rounded-[8px] bg-bg-50 px-4 py-3")}>
      <span className="text-ink-500">{k}</span>
      <span className="font-semibold text-ink-900">{v}</span>
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
      title="사자에게 고마운 마음을 남겨보세요"
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
