"use client";

import { useState } from "react";
import { cn } from "@/lib/cn";
import { useStore } from "@/lib/store";
import { formatDate } from "@/lib/format";
import { Button, Chip } from "@/components/ui/Button";
import { Input, Select, Switch } from "@/components/ui/Input";
import { ConfirmModal, Modal } from "@/components/ui/Modal";
import { Card } from "@/components/ui/Misc";
import { useToast } from "@/components/ui/Toast";

const TOPICS = ["취업 준비", "커리어·이직", "학업·전공", "프로젝트·개발", "창업", "멋사 활동", "기타"];
const TIMES = ["오전", "오후", "저녁"];

/** ★ CS-01 커피챗 설정 / 받기 상태 · ◇[O] CS-03 기간 추가 · ◇[O] CF-5 기본 상태 변경 확인 · [S] CS-04 */
export default function CoffeeSettingsPage() {
  const toast = useToast();
  const { settings, setSettings } = useStore();
  const [base, setBase] = useState<"OPEN" | "CLOSED">(settings.mode === "CLOSED" || settings.mode === "SCHEDULED" ? "CLOSED" : "OPEN");
  const [topics, setTopics] = useState<string[]>(settings.keywords);
  const [modes, setModes] = useState(settings.modes);
  const [group, setGroup] = useState(settings.allowGroup);
  const [times, setTimes] = useState<string[]>(["저녁"]);
  const [regions, setRegions] = useState(settings.regions);
  const [periods, setPeriods] = useState(settings.periods.length ? settings.periods : [{ id: "p1", from: "2026-11-14", to: "2026-11-20", kind: "REST" as const }]);
  const [addOpen, setAddOpen] = useState(false);
  const [confirmBase, setConfirmBase] = useState<"OPEN" | "CLOSED" | null>(null);

  const kind = base === "OPEN" ? "REST" : "OPEN";
  const today = new Date().toISOString().slice(0, 10);
  const activeRest = periods.find((p) => p.kind === "REST" && p.from <= today && today <= p.to);
  const nextOpen = periods.filter((p) => p.kind === "OPEN").sort((a, b) => a.from.localeCompare(b.from))[0];

  const statusText = base === "OPEN" ? (activeRest ? `${formatDate(activeRest.to)}까지 쉬는 중이에요` : "지금 커피챗을 받고 있어요") : nextOpen ? `지금은 커피챗을 받지 않아요 · 여는 기간: ${formatDate(nextOpen.from)} ~ ${formatDate(nextOpen.to)}` : "지금은 커피챗을 받지 않아요";

  const changeBase = (v: "OPEN" | "CLOSED") => {
    if (v === base) return;
    if (periods.length) return setConfirmBase(v); // CF-5
    setBase(v);
  };

  const save = () => {
    if (modes.length === 0) return toast("가능한 방식을 1개 이상 골라주세요", { kind: "error" });
    const mode = base === "OPEN" ? (activeRest ? "RESTING" : "OPEN") : nextOpen ? "SCHEDULED" : "CLOSED";
    setSettings({ mode, keywords: topics, modes, allowGroup: group, regions, periods, restUntil: activeRest ? formatDate(activeRest.to) : undefined, openFrom: nextOpen ? formatDate(nextOpen.from) : undefined });
    toast("커피챗 설정을 저장했어요");
  };

  return (
    <div className="flex flex-col gap-6">
      <h1 className="t-title text-ink-900">커피챗 설정</h1>

      <Card className="flex flex-col gap-5 p-6">
        <p className="text-[18px] font-bold text-ink-900">{statusText}</p>
        <div className="flex flex-col gap-2">
          <p className="t-label2 text-ink-700">기본 상태</p>
          <div className="flex gap-2">
            <Chip size="M" selected={base === "OPEN"} onClick={() => changeBase("OPEN")}>
              받기
            </Chip>
            <Chip size="M" selected={base === "CLOSED"} onClick={() => changeBase("CLOSED")}>
              안 받기
            </Chip>
          </div>
        </div>
        <div className="flex flex-col gap-2">
          <div className="flex items-center justify-between">
            <p className="t-label2 text-ink-700">
              {kind === "REST" ? "쉬는 기간" : "여는 기간"} <span className="font-normal text-ink-400">({periods.length}/5)</span>
            </p>
            <Button size="S" variant="secondary" disabled={periods.length >= 5} onClick={() => setAddOpen(true)}>
              + 기간 추가
            </Button>
          </div>
          <p className="text-[12px] text-ink-500">{kind === "REST" ? "이 기간에는 커피챗을 받지 않아요 · 최대 5개" : "이 기간에만 커피챗을 받아요 · 최대 5개"}</p>
          <ul className="flex flex-col gap-1.5">
            {periods.map((p) => (
              <li key={p.id} className="flex items-center justify-between rounded-[8px] bg-bg-50 px-4 py-2.5 text-[14px]">
                <span className="font-semibold text-ink-900">
                  {formatDate(p.from)}({DAY(p.from)}) ~ {formatDate(p.to)}({DAY(p.to)})
                </span>
                <button type="button" onClick={() => setPeriods((s) => s.filter((x) => x.id !== p.id))} className="text-[13px] text-ink-500 hover:text-orange">
                  삭제
                </button>
              </li>
            ))}
          </ul>
          <MiniCalendar periods={periods} />
        </div>
      </Card>

      <Card className="flex flex-col gap-6 p-6">
        <Group label="도와줄 수 있는 주제">
          <div className="flex flex-wrap gap-2">
            {TOPICS.map((t) => (
              <Chip key={t} selected={topics.includes(t)} onClick={() => setTopics((s) => (s.includes(t) ? s.filter((x) => x !== t) : [...s, t]))}>
                {t}
              </Chip>
            ))}
          </div>
        </Group>
        <Group label="가능한 방식 *" error={modes.length === 0 ? "가능한 방식을 1개 이상 골라주세요" : undefined}>
          <div className="flex gap-2">
            {(["ONLINE", "OFFLINE"] as const).map((m) => (
              <Chip key={m} selected={modes.includes(m)} onClick={() => setModes((s) => (s.includes(m) ? s.filter((x) => x !== m) : [...s, m]))}>
                {m === "ONLINE" ? "온라인" : "오프라인"}
              </Chip>
            ))}
          </div>
        </Group>
        <div className="flex items-center justify-between">
          <div>
            <p className="t-label1 text-ink-900">여러 명 커피챗 받기</p>
            <p className="text-[12px] text-ink-500">끄면 1:1 신청만 받아요{!group && " · 진행 중인 여러 명 커피챗은 그대로 이어져요"}</p>
          </div>
          <Switch checked={group} onChange={setGroup} label="여러 명 커피챗 받기" />
        </div>
        <Group label="가능한 시간대">
          <div className="flex gap-2">
            {TIMES.map((t) => (
              <Chip key={t} selected={times.includes(t)} onClick={() => setTimes((s) => (s.includes(t) ? s.filter((x) => x !== t) : [...s, t]))}>
                {t}
              </Chip>
            ))}
          </div>
        </Group>
        <Input label="선호 지역 (선택)" placeholder="예) 강남, 판교" value={regions} onChange={(e) => setRegions(e.target.value)} />
        <div className="flex justify-end">
          <Button size="L" onClick={save}>
            저장
          </Button>
        </div>
      </Card>

      {/* CS-03 기간 추가 */}
      <PeriodModal open={addOpen} onClose={() => setAddOpen(false)} kind={kind} existing={periods} onConfirm={(from, to) => { setPeriods((s) => [...s, { id: Math.random().toString(36).slice(2), from, to, kind }]); setAddOpen(false); }} />
      {/* CF-5 */}
      <ConfirmModal open={!!confirmBase} onClose={() => setConfirmBase(null)} onConfirm={() => { setBase(confirmBase!); setPeriods([]); setConfirmBase(null); }} title="등록한 기간이 모두 지워져요. 바꿀까요?" description="기본 상태를 바꾸면 쉬는 기간·여는 기간이 모두 삭제돼요." confirmLabel="바꾸기" />
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

function MiniCalendar({ periods, month = new Date() }: { periods: { from: string; to: string; kind: "REST" | "OPEN" }[]; month?: Date }) {
  const y = month.getFullYear();
  const m = month.getMonth();
  const first = new Date(y, m, 1).getDay();
  const days = new Date(y, m + 1, 0).getDate();
  const iso = (d: number) => `${y}-${String(m + 1).padStart(2, "0")}-${String(d).padStart(2, "0")}`;
  return (
    <div className="mt-2 w-[320px] rounded-[8px] border border-line-200 p-3">
      <p className="mb-2 text-center text-[13px] font-semibold text-ink-900">
        {y}년 {m + 1}월
      </p>
      <div className="grid grid-cols-7 gap-y-1 text-center text-[12px]">
        {["일", "월", "화", "수", "목", "금", "토"].map((d) => (
          <span key={d} className="text-ink-400">
            {d}
          </span>
        ))}
        {Array.from({ length: first }).map((_, i) => (
          <span key={`e${i}`} />
        ))}
        {Array.from({ length: days }, (_, i) => i + 1).map((d) => {
          const p = periods.find((x) => x.from <= iso(d) && iso(d) <= x.to);
          return (
            <span key={d} className={cn("mx-auto grid size-7 place-items-center rounded-full", p ? (p.kind === "REST" ? "bg-bg-100 text-ink-500" : "bg-primary text-white") : "text-ink-900")}>
              {d}
            </span>
          );
        })}
      </div>
    </div>
  );
}

/** ◇[O] CS-03 기간 추가 모달 */
function PeriodModal({ open, onClose, kind, existing, onConfirm }: { open: boolean; onClose: () => void; kind: "REST" | "OPEN"; existing: { from: string; to: string }[]; onConfirm: (from: string, to: string) => void }) {
  const today = new Date();
  const fmt = (d: Date) => d.toISOString().slice(0, 10);
  const plus = (n: number) => {
    const d = new Date();
    d.setDate(d.getDate() + n);
    return fmt(d);
  };
  const [preset, setPreset] = useState<"1w" | "2w" | "1m" | "custom">("1w");
  const [from, setFrom] = useState(fmt(today));
  const [to, setTo] = useState(plus(7));
  const apply = (p: typeof preset) => {
    setPreset(p);
    if (p !== "custom") {
      setFrom(fmt(today));
      setTo(plus(p === "1w" ? 7 : p === "2w" ? 14 : 30));
    }
  };
  const error = to < from ? "종료일이 시작일보다 빨라요" : existing.some((e) => from <= e.to && e.from <= to) ? "이미 등록한 기간과 겹쳐요" : undefined;
  return (
    <Modal
      open={open}
      onClose={onClose}
      title={kind === "REST" ? "쉬는 기간 추가" : "여는 기간 추가"}
      width={480}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            취소
          </Button>
          <Button disabled={!!error} onClick={() => onConfirm(from, to)}>
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
        <MiniCalendar periods={[...existing.map((e) => ({ ...e, kind: "REST" as const })), { from, to, kind: "OPEN" }]} />
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
