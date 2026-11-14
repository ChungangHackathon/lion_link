"use client";

import Link from "next/link";
import { useState } from "react";
import { cn } from "@/lib/cn";
import { formatDate } from "@/lib/format";
import { errorMessage, isApiError } from "@/lib/api/client";
import type { CoffeeTopic, TimeSlot, Weekday } from "@/lib/api/alumni";
import { COFFEE_TOPIC_LABEL } from "@/lib/api/coffee";
import { addDays, PERIOD_LIMIT, periodError, todayKst, type CoffeePeriod, type CoffeeSettings } from "@/lib/api/coffeeSettings";
import { useApi } from "@/lib/api/useApi";
import { useCoffeeSettingsService } from "@/lib/api/useCoffeeSettingsService";
import { Button, Chip } from "@/components/ui/Button";
import { Icon, type IconName } from "@/components/ui/Icon";
import { Input, Select, Switch } from "@/components/ui/Input";
import { ConfirmModal, Modal } from "@/components/ui/Modal";
import { Card, EmptyState, Skeleton } from "@/components/ui/Misc";
import { useToast } from "@/components/ui/Toast";

const TOPICS: CoffeeTopic[] = ["JOB_PREP", "CAREER", "STUDY", "PROJECT_DEV", "STARTUP", "LION_ACTIVITY", "ETC"];
const TIMES: { code: TimeSlot; label: string }[] = [
  { code: "MORNING", label: "오전" },
  { code: "AFTERNOON", label: "오후" },
  { code: "EVENING", label: "저녁" },
];
const WEEK: { code: Weekday; label: string }[] = [
  { code: "MON", label: "월" },
  { code: "TUE", label: "화" },
  { code: "WED", label: "수" },
  { code: "THU", label: "목" },
  { code: "FRI", label: "금" },
  { code: "SAT", label: "토" },
  { code: "SUN", label: "일" },
];

/** 칸 키: "EVENING-MON" */
const slotKey = (t: TimeSlot, d: Weekday) => `${t}-${d}`;
const toKeys = (slots: CoffeeSettings["availableSlots"]) => slots.flatMap((s) => s.times.map((t) => slotKey(t, s.day)));
const toSlots = (keys: string[]): CoffeeSettings["availableSlots"] =>
  WEEK.map((d) => ({ day: d.code, times: TIMES.filter((t) => keys.includes(slotKey(t.code, d.code))).map((t) => t.code) })).filter((s) => s.times.length > 0);

/** 체크한 칸을 "평일 저녁 · 토 오후"처럼 한 줄로 */
function summarize(keys: string[]) {
  const out: string[] = [];
  for (const t of TIMES) {
    const days = WEEK.filter((d) => keys.includes(slotKey(t.code, d.code))).map((d) => d.label);
    if (!days.length) continue;
    const weekdays = WEEK.slice(0, 5).map((d) => d.label);
    const hasAllWeekdays = weekdays.every((d) => days.includes(d));
    const rest = days.filter((d) => !(hasAllWeekdays && weekdays.includes(d)));
    if (hasAllWeekdays) out.push(`평일 ${t.label}`);
    if (rest.length) out.push(`${rest.join("·")} ${t.label}`);
  }
  return out.join(" · ");
}

/** ★ CS-01 커피챗 설정 / 받기 상태 · ◇[O] CS-03 기간 추가 · ◇[O] CF-5 기본 상태 변경 확인 · [S] CS-04 (PROFILE-12·13·15) */
export default function CoffeeSettingsPage() {
  const service = useCoffeeSettingsService();
  // 프로토타입 모드에서는 store가 바뀔 때마다 service가 바뀌므로, 처음 불러온 값으로 양식을 채운다
  const [loader] = useState(service);
  const { data, error, reload } = useApi(() => loader.get(), [loader]);
  if (error && !data) return <EmptyState title="커피챗 설정을 불러오지 못했어요" description={errorMessage(error)} action={{ label: "다시 시도", onClick: reload }} />;
  if (!data) return <Skeleton className="h-[600px] w-full" />;
  return <SettingsForm initial={data} />;
}

function SettingsForm({ initial }: { initial: CoffeeSettings }) {
  const toast = useToast();
  const service = useCoffeeSettingsService();
  const [saved, setSaved] = useState(initial);
  const [base, setBase] = useState<"OPEN" | "CLOSED">(initial.defaultMode === "ON" ? "OPEN" : "CLOSED");
  const [topics, setTopics] = useState<CoffeeTopic[]>(initial.topics);
  const [modes, setModes] = useState(initial.modes);
  const [group, setGroup] = useState(initial.acceptGroup);
  const [slots, setSlots] = useState<string[]>(toKeys(initial.availableSlots));
  const [regions, setRegions] = useState(initial.preferredArea ?? "");
  const [periods, setPeriods] = useState<CoffeePeriod[]>(initial.periods);
  const [clearPending, setClearPending] = useState(false);
  const [addOpen, setAddOpen] = useState(false);
  const [confirmBase, setConfirmBase] = useState<"OPEN" | "CLOSED" | null>(null);
  const [confirmSave, setConfirmSave] = useState(false);
  const [saving, setSaving] = useState(false);
  // 기간을 추가·삭제하면 오늘 상태가 바뀔 수 있어서 설정을 다시 불러온다 (다음 렌더의 service로 부른다)
  const [refreshTick, setRefreshTick] = useState(0);
  const fresh = useApi(() => service.get(), [refreshTick], refreshTick > 0).data;
  const [seenFresh, setSeenFresh] = useState(fresh);
  if (fresh && fresh !== seenFresh) {
    setSeenFresh(fresh);
    setSaved(fresh);
    setPeriods(fresh.periods);
  }

  const savedBase = saved.defaultMode === "ON" ? "OPEN" : "CLOSED";
  const baseDirty = base !== savedBase;
  const kind = base === "OPEN" ? "REST" : "OPEN";
  const shownPeriods = clearPending ? [] : periods;

  // 상태 문구: 저장된 서버 상태 기준. 기본 상태를 바꾸고 아직 저장 전이면 저장 후 모습을 안내한다
  const status = saved.status;
  const next = status.nextAvailableDate ? formatDate(status.nextAvailableDate) : null;
  const statusText = baseDirty
    ? base === "OPEN"
      ? "저장하면 커피챗을 받기 시작해요"
      : "저장하면 커피챗을 받지 않아요"
    : status.availability === "AVAILABLE"
      ? "지금 커피챗을 받고 있어요"
      : status.availability === "RESTING"
        ? next
          ? `쉬는 중이에요 · ${next}부터 다시 받아요`
          : "지금은 쉬는 중이에요"
        : next
          ? `${next}부터 커피챗을 받아요`
          : "지금은 커피챗을 받지 않아요";
  const receiving = baseDirty ? base === "OPEN" : status.availability === "AVAILABLE";
  const resting = !baseDirty && status.availability === "RESTING";

  const changeBase = (v: "OPEN" | "CLOSED") => {
    if (v === base) return;
    if (v === savedBase) {
      setBase(v);
      setClearPending(false);
      return;
    }
    if (periods.length) return setConfirmBase(v); // CF-5
    setBase(v);
  };

  const save = async (confirmClear = clearPending) => {
    if (modes.length === 0) return toast("가능한 방식을 1개 이상 골라주세요", { kind: "error" });
    if (regions.trim().length > 30) return toast("선호 지역은 30자 이내로 적어주세요", { kind: "error" });
    setSaving(true);
    try {
      const res = await service.save({
        defaultMode: base === "OPEN" ? "ON" : "OFF",
        acceptGroup: group,
        topics,
        modes,
        availableSlots: toSlots(slots),
        preferredArea: regions.trim() || undefined,
        confirmClearPeriods: confirmClear || undefined,
      });
      setSaved(res);
      setPeriods(res.periods);
      setClearPending(false);
      setBase(res.defaultMode === "ON" ? "OPEN" : "CLOSED");
      toast("커피챗 설정을 저장했어요");
    } catch (e) {
      if (isApiError(e, "PERIODS_WILL_BE_CLEARED")) setConfirmSave(true);
      else toast(errorMessage(e), { kind: "error" });
    } finally {
      setSaving(false);
    }
  };

  const addPeriod = async (startDate: string, endDate: string) => {
    try {
      const p = await service.addPeriod({ startDate, endDate });
      setPeriods((s) => [...s.filter((x) => String(x.id) !== String(p.id)), p].sort((a, b) => a.startDate.localeCompare(b.startDate)));
      setAddOpen(false);
      toast(kind === "REST" ? "쉬는 기간을 추가했어요" : "커피챗 받는 기간을 추가했어요");
      setRefreshTick((t) => t + 1);
    } catch (e) {
      toast(errorMessage(e), { kind: "error" });
    }
  };

  const removePeriod = async (id: CoffeePeriod["id"]) => {
    try {
      await service.removePeriod(id);
      setPeriods((s) => s.filter((x) => String(x.id) !== String(id)));
      toast("기간을 지웠어요");
      setRefreshTick((t) => t + 1);
    } catch (e) {
      toast(errorMessage(e), { kind: "error" });
    }
  };

  const timeSummary = summarize(slots);
  const calendarPeriods = shownPeriods.map((p) => ({ from: p.startDate, to: p.endDate, kind: saved.periodKind }));

  return (
    <div className="flex flex-col gap-5">
      <div className="flex items-end justify-between border-t border-line-200 pt-1">
        <div className="flex flex-col gap-1">
          <h1 className="t-title3 text-ink-900">커피챗 설정</h1>
          <p className="t-caption1 text-ink-500">사자·동문에게 보이는 내 커피챗 카드를 여기서 정해요</p>
        </div>
        <Link href="/me" className="flex items-center gap-1.5 t-label2 text-primary-strong hover:underline">
          <Icon name="user" size={16} /> 내 프로필에서 보기 <Icon name="chevron-right" size={14} />
        </Link>
      </div>

      <div className="grid grid-cols-[minmax(0,1fr)_372px] items-start gap-6">
        {/* ── 왼쪽: 설정 ── */}
        <div className="flex flex-col gap-5">
          <div className={cn("flex flex-col gap-4 rounded-[14px] border p-6", receiving ? "border-[#bfe5cf] bg-[#e8f7ed]" : "border-line-200 bg-bg-100")}>
            <div className="flex items-center gap-4">
              <span className="grid size-11 shrink-0 place-items-center rounded-full bg-white text-ink-900">
                <Icon name="coffee" size={22} />
              </span>
              <div className="flex flex-col gap-0.5">
                <p className="t-title3 text-ink-900">{statusText}</p>
                <p className="t-caption1 text-ink-700">{baseDirty ? "오른쪽 아래 [저장]을 눌러야 반영돼요" : receiving ? "사자들이 내 프로필에서 바로 신청할 수 있어요" : resting ? "쉬는 기간이 끝나면 자동으로 다시 받아요" : "받기를 켜면 사자들이 신청할 수 있어요"}</p>
              </div>
            </div>
            <div role="radiogroup" aria-label="기본 상태" className="grid grid-cols-2 gap-2">
              {(["OPEN", "CLOSED"] as const).map((v) => (
                <button key={v} type="button" role="radio" aria-checked={base === v} onClick={() => changeBase(v)} className={cn("h-11 rounded-full border t-label1 transition-colors", base === v ? "border-light-text bg-blue-100/60 text-navy shadow-[inset_0_0_0_1px_rgba(79,111,168,0.35)]" : "border-line-200 bg-white text-ink-900 hover:bg-bg-50")}>
                  {v === "OPEN" ? "받기" : "안 받기"}
                </button>
              ))}
            </div>
          </div>

          <Card className="flex flex-col gap-3 rounded-[14px] p-6">
            <div className="flex items-center justify-between">
              <p className="t-body-bold text-ink-900">
                {kind === "REST" ? "쉬는 기간" : "커피챗 받는 기간"} <span className="t-caption1 text-ink-400">({shownPeriods.length}/{PERIOD_LIMIT})</span>
              </p>
              <button type="button" disabled={shownPeriods.length >= PERIOD_LIMIT || baseDirty} onClick={() => setAddOpen(true)} className="t-label2 text-ink-900 underline underline-offset-2 disabled:text-ink-400 disabled:no-underline">
                + 기간 추가
              </button>
            </div>
            <p className="t-caption1 text-ink-500">
              {baseDirty
                ? "기본 상태를 바꾼 뒤에는 저장하고 나서 기간을 추가할 수 있어요."
                : shownPeriods.length >= PERIOD_LIMIT
                  ? "기간은 5개까지 등록할 수 있어요. 지난 기간을 지우고 추가해주세요."
                  : kind === "REST"
                    ? "이 기간에는 커피챗을 받지 않아요 · 최대 5개 · 추가·삭제는 바로 저장돼요"
                    : "이 기간에만 커피챗을 받아요 · 최대 5개 · 추가·삭제는 바로 저장돼요"}
            </p>
            <ul className="flex flex-col gap-2">
              {shownPeriods.length === 0 && <li className="rounded-[8px] bg-bg-50 px-4 py-3 text-[13px] text-ink-500">{clearPending ? "저장하면 등록한 기간이 모두 지워져요" : "등록한 기간이 없어요"}</li>}
              {shownPeriods.map((p) => (
                <li key={p.id} className="flex items-center justify-between rounded-[8px] bg-light-card px-4 py-3">
                  <span className="flex items-center gap-2 t-label1 text-ink-900">
                    <Icon name="calendar" size={16} className="text-ink-700" />
                    {formatDate(p.startDate)}({DAY(p.startDate)}) ~ {formatDate(p.endDate)}({DAY(p.endDate)})
                  </span>
                  <button type="button" onClick={() => void removePeriod(p.id)} className="t-caption1 text-ink-500 hover:text-orange">
                    삭제
                  </button>
                </li>
              ))}
            </ul>
          </Card>

          <Card className="flex flex-col gap-7 rounded-[14px] p-6">
            <Group label="도와줄 수 있는 주제">
              <div className="flex flex-wrap gap-2">
                {TOPICS.map((t) => (
                  <SoftChip key={t} selected={topics.includes(t)} onClick={() => setTopics((x) => (x.includes(t) ? x.filter((y) => y !== t) : [...x, t]))}>
                    {COFFEE_TOPIC_LABEL[t]}
                  </SoftChip>
                ))}
              </div>
            </Group>
            <Group label="가능한 방식 *" error={modes.length === 0 ? "가능한 방식을 1개 이상 골라주세요" : undefined}>
              <div className="flex gap-2">
                {(["ONLINE", "OFFLINE"] as const).map((m) => (
                  <SoftChip key={m} selected={modes.includes(m)} onClick={() => setModes((x) => (x.includes(m) ? x.filter((y) => y !== m) : [...x, m]))}>
                    {m === "ONLINE" ? "온라인" : "오프라인"}
                  </SoftChip>
                ))}
              </div>
            </Group>
            <div className="flex items-center justify-between rounded-[10px] bg-bg-100 px-4 py-3.5">
              <div className="flex flex-col gap-0.5">
                <p className="t-body-bold text-ink-900">여러 명 커피챗 받기</p>
                <p className="text-[12px] text-ink-500">
                  끄면 1:1 신청만 받아요{!group && " · 진행 중인 여러 명 커피챗은 그대로 이어져요"}
                  {!group && saved.acceptGroup && " · 나를 신청할 사자로 고른 모집 중인 커피챗 모집글은 저장하면 마감돼요"}
                </p>
              </div>
              <Switch checked={group} onChange={setGroup} label="여러 명 커피챗 받기" />
            </div>
            <Group label="가능한 시간대">
              <table className="w-full border-separate border-spacing-y-2 text-center">
                <thead>
                  <tr className="t-caption1 text-ink-500">
                    <th className="w-14" />
                    {WEEK.map((d) => (
                      <th key={d.code} scope="col" className="font-normal">
                        {d.label}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {TIMES.map((t) => (
                    <tr key={t.code}>
                      <th scope="row" className="text-left t-caption1 font-normal text-ink-700">
                        {t.label}
                      </th>
                      {WEEK.map((d) => {
                        const key = slotKey(t.code, d.code);
                        const on = slots.includes(key);
                        return (
                          <td key={d.code}>
                            <button type="button" role="checkbox" aria-checked={on} aria-label={`${d.label}요일 ${t.label}`} onClick={() => setSlots((x) => (on ? x.filter((y) => y !== key) : [...x, key]))} className={cn("mx-auto grid size-[18px] place-items-center rounded-[4px] border", on ? "border-primary bg-primary text-white" : "border-line-200 bg-white hover:border-ink-400")}>
                              {on && <Icon name="check" size={12} />}
                            </button>
                          </td>
                        );
                      })}
                    </tr>
                  ))}
                </tbody>
              </table>
            </Group>
            <Input label="선호 지역 (선택)" placeholder="예) 강남, 흑석" maxLength={30} value={regions} onChange={(e) => setRegions(e.target.value)} counter={{ value: regions.length, max: 30 }} />
          </Card>
        </div>

        {/* ── 오른쪽: 달력 · 미리보기 · 저장 ── */}
        <aside className="sticky top-[88px] flex flex-col gap-5">
          <MiniCalendar periods={calendarPeriods} large />
          <div className="flex flex-col gap-3">
            <p className="flex items-center gap-2 t-label2 text-ink-900">
              <Icon name="monitor" size={16} /> 내 프로필에 이렇게 보여요
            </p>
            <Card className="flex flex-col gap-[14px] rounded-[16px] p-5">
              <div className="flex items-center gap-2">
                <Icon name="coffee" size={18} className="text-ink-700" />
                <p className="flex-1 t-label1 text-ink-900">커피챗</p>
                <span className={cn("relative h-[18px] w-[33px] rounded-full", receiving ? "bg-green" : "bg-line-300")} aria-hidden>
                  <span className={cn("absolute top-0.5 size-[14px] rounded-full bg-white", receiving ? "left-[17px]" : "left-0.5")} />
                </span>
              </div>
              <span className={cn("flex w-fit items-center gap-1.5 rounded-full px-2.5 py-[5px] t-caption1", receiving ? "bg-[#e8f7ed] text-[#179952]" : "bg-bg-100 text-ink-500")}>
                <span className={cn("size-[7px] rounded-full", receiving ? "bg-green" : "bg-ink-400")} /> {receiving ? "지금 커피챗 받는 중" : resting ? "쉬는 중" : "커피챗 받지 않음"}
              </span>
              <Preview icon="clock" k="가능 시간대" v={timeSummary || "미정"} />
              <Preview icon="monitor" k="방식" v={modes.map((m) => (m === "ONLINE" ? "온라인" : "오프라인")).join(" · ") || "—"} />
              <Preview icon="place" k="선호 지역" v={regions || "—"} />
              <p className="t-caption1 text-ink-500">이야기할 수 있는 주제</p>
              <div className="flex flex-wrap gap-1.5">
                {topics.length === 0 && <span className="text-[12px] text-ink-400">아직 고르지 않았어요</span>}
                {topics.map((k) => (
                  <span key={k} className="rounded-[6px] bg-bg-100 px-2 py-1 t-label2 text-ink-700">
                    {COFFEE_TOPIC_LABEL[k]}
                  </span>
                ))}
              </div>
            </Card>
          </div>
          <Button size="L" full loading={saving} onClick={() => void save()}>
            저장
          </Button>
          <p className="t-caption1 text-ink-500">저장하면 내 프로필 커피챗 카드에 바로 반영돼요</p>
        </aside>
      </div>

      {/* CS-03 기간 추가 (PROFILE-15) */}
      {addOpen && <PeriodModal open onClose={() => setAddOpen(false)} kind={kind} existing={periods} onConfirm={addPeriod} />}
      {/* CF-5: 기본 상태를 바꾸면 저장할 때 기간이 모두 지워진다 (confirmClearPeriods) */}
      <ConfirmModal
        open={!!confirmBase}
        onClose={() => setConfirmBase(null)}
        onConfirm={() => {
          setBase(confirmBase!);
          setClearPending(true);
          setConfirmBase(null);
        }}
        title="등록한 기간이 모두 지워져요. 바꿀까요?"
        description="기본 상태를 바꾸면 쉬는 기간·커피챗 받는 기간이 모두 삭제돼요. [저장]을 눌러야 반영돼요."
        confirmLabel="바꾸기"
      />
      {/* 저장했는데 서버가 기간 삭제 확인을 요구할 때 (다른 기기에서 기간을 추가한 경우 등) */}
      <ConfirmModal
        open={confirmSave}
        onClose={() => setConfirmSave(false)}
        onConfirm={() => {
          setConfirmSave(false);
          setClearPending(true);
          void save(true);
        }}
        title="등록한 기간이 모두 지워져요. 바꿀까요?"
        description="기본 상태를 바꾸면 쉬는 기간·커피챗 받는 기간이 모두 삭제돼요."
        confirmLabel="바꾸고 저장"
      />
    </div>
  );
}

const DAY = (iso: string) => ["일", "월", "화", "수", "목", "금", "토"][new Date(iso).getDay()];

function Group({ label, children, error }: { label: string; children: React.ReactNode; error?: string }) {
  return (
    <div className="flex flex-col gap-2.5">
      <p className="t-label1 text-ink-900">{label}</p>
      {children}
      {error && <p className="t-caption2 text-orange">{error}</p>}
    </div>
  );
}

function SoftChip({ selected, onClick, children }: { selected: boolean; onClick: () => void; children: React.ReactNode }) {
  return (
    <button type="button" aria-pressed={selected} onClick={onClick} className={cn("h-[34px] rounded-full border px-[14px] t-label2 transition-colors", selected ? "border-light-text bg-blue-100/60 text-navy shadow-[inset_0_0_0_1px_rgba(79,111,168,0.35)]" : "border-line-300 bg-white text-ink-700 hover:bg-bg-50")}>
      {children}
    </button>
  );
}

function Preview({ icon, k, v }: { icon: IconName; k: string; v: string }) {
  return (
    <div className="flex items-center gap-2.5 text-[13px] leading-4">
      <span className="flex shrink-0 items-center gap-1.5 text-ink-500">
        <Icon name={icon} size={15} />
        {k}
      </span>
      <span className="min-w-0 flex-1 truncate text-right font-medium text-ink-900">{v}</span>
    </div>
  );
}

function MiniCalendar({ periods, large }: { periods: { from: string; to: string; kind: "REST" | "OPEN" }[]; large?: boolean }) {
  const [offset, setOffset] = useState(0);
  const base = new Date();
  const month = new Date(base.getFullYear(), base.getMonth() + offset, 1);
  const y = month.getFullYear();
  const m = month.getMonth();
  const first = new Date(y, m, 1).getDay();
  const days = new Date(y, m + 1, 0).getDate();
  const iso = (d: number) => `${y}-${String(m + 1).padStart(2, "0")}-${String(d).padStart(2, "0")}`;
  const today = new Date().toISOString().slice(0, 10);
  return (
    <div className={cn("rounded-[14px] border border-line-200 bg-white", large ? "p-5" : "mt-2 w-[320px] p-3")}>
      <div className="mb-3 flex items-center justify-between">
        <button type="button" aria-label="이전 달" onClick={() => setOffset((o) => o - 1)} className="rounded p-1 text-ink-500 hover:bg-bg-100">
          <Icon name="chevron-left" size={16} />
        </button>
        <p className={cn("text-ink-900", large ? "t-body-bold" : "text-[13px] font-semibold")}>
          {y}년 {m + 1}월
        </p>
        <button type="button" aria-label="다음 달" onClick={() => setOffset((o) => o + 1)} className="rounded p-1 text-ink-500 hover:bg-bg-100">
          <Icon name="chevron-right" size={16} />
        </button>
      </div>
      <div className={cn("grid grid-cols-7 text-center", large ? "gap-y-2 text-[14px]" : "gap-y-1 text-[12px]")}>
        {["일", "월", "화", "수", "목", "금", "토"].map((d) => (
          <span key={d} className="pb-1 text-[12px] text-ink-400">
            {d}
          </span>
        ))}
        {Array.from({ length: first }).map((_, i) => (
          <span key={`e${i}`} />
        ))}
        {Array.from({ length: days }, (_, i) => i + 1).map((d) => {
          const p = periods.find((x) => x.from <= iso(d) && iso(d) <= x.to);
          const past = iso(d) < today;
          return (
            <span key={d} className={cn("grid place-items-center", large ? "h-9" : "h-7", p && (p.kind === "REST" ? "bg-[#dfe6f3]" : "bg-blue-100"), p && iso(d) === p.from && "rounded-l-[6px]", p && iso(d) === p.to && "rounded-r-[6px]", iso(d) === today ? "font-bold text-ink-900" : past && !p ? "text-ink-400" : "text-ink-900")}>
              {d}
            </span>
          );
        })}
      </div>
    </div>
  );
}

/** ◇[O] CS-03 기간 추가 모달 */
function PeriodModal({ open, onClose, kind, existing, onConfirm }: { open: boolean; onClose: () => void; kind: "REST" | "OPEN"; existing: CoffeePeriod[]; onConfirm: (from: string, to: string) => Promise<void> }) {
  const today = todayKst();
  const plus = (n: number) => addDays(today, n);
  const [preset, setPreset] = useState<"1w" | "2w" | "1m" | "custom">("1w");
  const [from, setFrom] = useState(today);
  const [to, setTo] = useState(plus(6));
  const [busy, setBusy] = useState(false);
  const apply = (p: typeof preset) => {
    setPreset(p);
    if (p !== "custom") {
      setFrom(today);
      setTo(plus(p === "1w" ? 6 : p === "2w" ? 13 : 29));
    }
  };
  const error = periodError(from, to, existing) ?? undefined;
  return (
    <Modal
      open={open}
      onClose={onClose}
      title={kind === "REST" ? "쉬는 기간 추가" : "커피챗 받는 기간 추가"}
      width={480}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            취소
          </Button>
          <Button
            disabled={!!error}
            loading={busy}
            onClick={() => {
              setBusy(true);
              onConfirm(from, to).finally(() => setBusy(false));
            }}
          >
            저장
          </Button>
        </>
      }
    >
      <div className="flex flex-col gap-4">
        <div className="flex flex-wrap gap-2">
          <Chip selected={preset === "1w"} onClick={() => apply("1w")}>
            오늘부터 1주
          </Chip>
          <Chip selected={preset === "2w"} onClick={() => apply("2w")}>
            2주
          </Chip>
          <Chip selected={preset === "1m"} onClick={() => apply("1m")}>
            1달
          </Chip>
          <Chip selected={preset === "custom"} onClick={() => apply("custom")}>
            직접 선택
          </Chip>
        </div>
        <MiniCalendar periods={[...existing.map((e) => ({ from: e.startDate, to: e.endDate, kind: "REST" as const })), { from, to, kind: "OPEN" }]} />
        <div className="grid grid-cols-2 gap-3">
          <Select label="시작일" value={from} onChange={(e) => { setPreset("custom"); setFrom(e.target.value); }}>
            {Array.from({ length: 60 }, (_, i) => plus(i)).map((d) => (
              <option key={d} value={d}>
                {formatDate(d)}({DAY(d)})
              </option>
            ))}
          </Select>
          <Select label="종료일" value={to} onChange={(e) => { setPreset("custom"); setTo(e.target.value); }} error={error}>
            {Array.from({ length: 90 }, (_, i) => plus(i)).map((d) => (
              <option key={d} value={d}>
                {formatDate(d)}({DAY(d)})
              </option>
            ))}
          </Select>
        </div>
      </div>
    </Modal>
  );
}
