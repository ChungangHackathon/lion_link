"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useMemo, useState } from "react";
import { cn } from "@/lib/cn";
import { useStore } from "@/lib/store";
import { ME_ID } from "@/lib/mock/users";
import { INTEREST_OPTIONS, LION_PART_LABEL, PROFILE_STATUS_LABEL, type LionPart, type ProfileStatus } from "@/lib/types";
import { Button, Chip } from "@/components/ui/Button";
import { Checkbox, Switch } from "@/components/ui/Input";
import { Glyph } from "@/components/ui/Icon";
import { EmptyState, Skeleton } from "@/components/ui/Misc";
import { AlumniCard } from "@/components/domain/AlumniCard";
import { useAlumniActions } from "@/components/domain/useAlumniActions";

const GENS = Array.from({ length: 14 }, (_, i) => i + 1);
const PARTS: LionPart[] = ["PLANNING", "DESIGN", "FRONTEND", "BACKEND"];
const STATUSES = Object.keys(PROFILE_STATUS_LABEL) as ProfileStatus[];
type Sort = "recommend" | "newest" | "generation";

export default function AlumniPage() {
  return (
    <Suspense fallback={<Skeleton className="h-[600px] w-full" />}>
      <AlumniList />
    </Suspense>
  );
}

/** ★ S-01 동문 목록 / 기본 · ◇ S-03 검색·필터 적용됨 · [S] S-04 상태 모음 (4열 → 3열 카드 그리드 + 왼쪽 필터 패널) */
function AlumniList() {
  const { alumni, blocked } = useStore();
  const { onRequest, onMessage, modals } = useAlumniActions();
  const params = useSearchParams();
  const router = useRouter();
  const me = alumni.find((a) => a.id === ME_ID)!;

  const [q, setQ] = useState(params.get("q") ?? "");
  const [onlyOpen, setOnlyOpen] = useState(params.get("open") === "1");
  const [gens, setGens] = useState<number[]>(params.get("gen") ? params.get("gen")!.split(",").map(Number) : []);
  const [parts, setParts] = useState<LionPart[]>(params.get("part") ? (params.get("part")!.split(",") as LionPart[]) : []);
  const [statuses, setStatuses] = useState<ProfileStatus[]>([]);
  const [interests, setInterests] = useState<string[]>([]);
  const [moreInterests, setMoreInterests] = useState(false);
  const [sort, setSort] = useState<Sort>("recommend");

  const toggle = <T,>(set: React.Dispatch<React.SetStateAction<T[]>>, v: T) => set((s) => (s.includes(v) ? s.filter((x) => x !== v) : [...s, v]));
  const reset = () => {
    setQ("");
    setOnlyOpen(false);
    setGens([]);
    setParts([]);
    setStatuses([]);
    setInterests([]);
    router.replace("/alumni");
  };
  const applied = onlyOpen || gens.length > 0 || parts.length > 0 || statuses.length > 0 || interests.length > 0 || q.trim().length > 0;

  const list = useMemo(() => {
    const kw = q.trim().toLowerCase();
    const r = alumni.filter(
      (a) =>
        a.id !== ME_ID &&
        !blocked.includes(a.id) &&
        (!onlyOpen || a.receive === "OPEN") &&
        (gens.length === 0 || gens.includes(a.generation)) &&
        (parts.length === 0 || parts.includes(a.part)) &&
        (statuses.length === 0 || statuses.includes(a.status)) &&
        (interests.length === 0 || a.interests.some((i) => interests.includes(i))) &&
        (!kw || `${a.name} ${a.company ?? ""} ${a.role ?? ""} ${a.headline} ${a.skills.join(" ")}`.toLowerCase().includes(kw)),
    );
    if (sort === "generation") r.sort((a, b) => a.generation - b.generation);
    else if (sort === "newest") r.reverse();
    else r.sort((a, b) => Number(b.receive === "OPEN") - Number(a.receive === "OPEN") || Number(b.part === me.part) - Number(a.part === me.part));
    return r;
  }, [alumni, blocked, q, onlyOpen, gens, parts, statuses, interests, sort, me.part]);

  // 빠른 필터 (S-01·S-03)
  const quick = {
    samePart: parts.length === 1 && parts[0] === me.part,
    open: onlyOpen,
    employed: statuses.length === 1 && statuses[0] === "EMPLOYED",
  };

  return (
    <div className="grid grid-cols-[264px_1fr] gap-[44px]">
      {/* FilterPanel */}
      <aside className="flex h-fit flex-col gap-4 rounded-[12px] border border-line-200 bg-white p-5">
        <div className="flex items-center justify-between">
          <h2 className="text-[20px] font-bold text-ink-900">필터</h2>
          <button type="button" onClick={reset} className="rounded-full bg-bg-100 px-2.5 py-1 text-[12px] font-semibold text-ink-700 hover:bg-line-200">
            초기화
          </button>
        </div>
        <div className="flex items-center justify-between">
          <div>
            <p className="t-label1 text-ink-900">지금 커피챗 받는 동문만</p>
            <p className="text-[12px] text-ink-500">오늘 기준</p>
          </div>
          <Switch checked={onlyOpen} onChange={setOnlyOpen} label="지금 커피챗 받는 동문만" />
        </div>
        <hr className="border-line-200" />
        <FilterGroup title="기수 (복수 선택)">
          <div className="flex flex-wrap gap-2">
            {GENS.map((g) => (
              <Chip key={g} selected={gens.includes(g)} onClick={() => toggle(setGens, g)}>
                {g}기
              </Chip>
            ))}
          </div>
        </FilterGroup>
        <hr className="border-line-200" />
        <FilterGroup title="멋사 파트">
          {PARTS.map((p) => (
            <Checkbox key={p} label={LION_PART_LABEL[p]} checked={parts.includes(p)} onChange={() => toggle(setParts, p)} />
          ))}
        </FilterGroup>
        <hr className="border-line-200" />
        <FilterGroup title="현재 상태">
          {STATUSES.map((s) => (
            <Checkbox key={s} label={PROFILE_STATUS_LABEL[s]} checked={statuses.includes(s)} onChange={() => toggle(setStatuses, s)} />
          ))}
        </FilterGroup>
        <hr className="border-line-200" />
        <FilterGroup title="관심 분야">
          {(moreInterests ? INTEREST_OPTIONS : INTEREST_OPTIONS.slice(0, 5)).map((i) => (
            <Checkbox key={i} label={i} checked={interests.includes(i)} onChange={() => toggle(setInterests, i as string)} />
          ))}
          <button type="button" onClick={() => setMoreInterests((m) => !m)} className="w-fit text-[13px] text-ink-500 underline underline-offset-2">
            {moreInterests ? "접기" : `+ ${INTEREST_OPTIONS.length - 5}개 더보기`}
          </button>
        </FilterGroup>
      </aside>

      <section className="flex flex-col gap-5">
        <div className="flex flex-col gap-1">
          <h1 className="t-title text-ink-900">동문 찾기</h1>
          <p className="t-body2 text-ink-500">{applied ? `${list.length}명의 동문이 조건에 맞아요` : `총 ${alumni.length - 1}명의 동문`}</p>
        </div>

        <label className="flex h-10 items-center gap-2 rounded-[8px] border border-line-300 bg-white px-3 focus-within:border-ink-800">
          <Glyph name="search" className="text-ink-400" />
          <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="이름, 회사, 직무, 기술" className="min-w-0 flex-1 text-[15px] outline-none placeholder:text-ink-400" />
          {q && (
            <button type="button" aria-label="지우기" onClick={() => setQ("")} className="text-ink-400 hover:text-ink-900">
              ✕
            </button>
          )}
        </label>

        <div className="flex items-center justify-between">
          <div className="flex flex-wrap gap-2">
            <Chip size="M" selected={quick.samePart} onClick={() => setParts(quick.samePart ? [] : [me.part])}>
              나와 같은 파트 선배
            </Chip>
            <Chip size="M" selected={quick.open} onClick={() => setOnlyOpen((o) => !o)}>
              지금 커피챗 가능
            </Chip>
            <Chip size="M" selected={quick.employed} onClick={() => setStatuses(quick.employed ? [] : ["EMPLOYED"])}>
              재직 중
            </Chip>
          </div>
          <select value={sort} onChange={(e) => setSort(e.target.value as Sort)} className="h-9 rounded-[6px] border border-line-300 bg-white px-2 text-[13px] text-ink-700">
            <option value="recommend">추천순</option>
            <option value="generation">기수순</option>
            <option value="newest">최근 가입순</option>
          </select>
        </div>

        {applied && (
          <div className="flex flex-wrap items-center gap-2 text-[13px] text-ink-500">
            적용된 필터:
            {gens.map((g) => (
              <AppliedTag key={`g${g}`} onRemove={() => toggle(setGens, g)}>
                {g}기
              </AppliedTag>
            ))}
            {parts.map((p) => (
              <AppliedTag key={p} onRemove={() => toggle(setParts, p)}>
                {LION_PART_LABEL[p]}
              </AppliedTag>
            ))}
            {statuses.map((s) => (
              <AppliedTag key={s} onRemove={() => toggle(setStatuses, s)}>
                {PROFILE_STATUS_LABEL[s]}
              </AppliedTag>
            ))}
            {interests.map((i) => (
              <AppliedTag key={i} onRemove={() => toggle(setInterests, i)}>
                {i}
              </AppliedTag>
            ))}
            {onlyOpen && <AppliedTag onRemove={() => setOnlyOpen(false)}>커피챗 가능</AppliedTag>}
            {q.trim() && <AppliedTag onRemove={() => setQ("")}>“{q.trim()}”</AppliedTag>}
          </div>
        )}

        {list.length === 0 ? (
          <EmptyState title={q.trim() ? `“${q.trim()}”에 맞는 동문이 없어요` : "조건에 맞는 동문이 없어요"} description="필터를 줄이거나 다른 검색어로 찾아보세요" action={{ label: "필터 초기화", onClick: reset }} />
        ) : (
          <div className="grid grid-cols-3 gap-x-[50px] gap-y-[55px] py-4">
            {list.map((a) => (
              <AlumniCard key={a.id} person={a} onRequest={onRequest} onMessage={onMessage} className="w-full" />
            ))}
          </div>
        )}
        {list.length > 9 && (
          <div className="flex justify-center">
            <Button variant="secondary">더 보기</Button>
          </div>
        )}
      </section>
      {modals}
    </div>
  );
}

function FilterGroup({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="flex flex-col gap-3">
      <p className="t-label1 text-ink-900">{title}</p>
      <div className="flex flex-col gap-2.5">{children}</div>
    </div>
  );
}

function AppliedTag({ children, onRemove }: { children: React.ReactNode; onRemove: () => void }) {
  return (
    <button type="button" onClick={onRemove} className={cn("inline-flex items-center gap-1 rounded-full bg-part-be-bg px-2.5 py-1 text-[12px] font-semibold text-primary hover:bg-part-be-line")}>
      {children} ✕
    </button>
  );
}
