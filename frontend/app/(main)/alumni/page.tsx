"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";
import { cn } from "@/lib/cn";
import { useStore } from "@/lib/store";
import { ME_ID } from "@/lib/mock/users";
import { INTEREST_CODE, INTEREST_OPTIONS, LION_PART_LABEL, PROFILE_STATUS_LABEL, type InterestLabel, type ProfileStatus } from "@/lib/types";
import { API_ENABLED, errorMessage } from "@/lib/api/client";
import type { AlumniSort, AlumniSummary } from "@/lib/api/alumni";
import { getMyProfileParts, type ApiLionPart } from "@/lib/api/profile";
import { useApi } from "@/lib/api/useApi";
import { useAlumniService } from "@/lib/api/useAlumniService";
import { Button, Chip } from "@/components/ui/Button";
import { Checkbox, Switch } from "@/components/ui/Input";
import { Icon } from "@/components/ui/Icon";
import { Popover } from "@/components/ui/Modal";
import { EmptyState, Skeleton } from "@/components/ui/Misc";
import { useToast } from "@/components/ui/Toast";
import { AlumniCard } from "@/components/domain/AlumniCard";
import { useAlumniActions } from "@/components/domain/useAlumniActions";

const GENS = Array.from({ length: 14 }, (_, i) => i + 1);
const PARTS: ApiLionPart[] = ["PLANNING", "DESIGN", "FRONTEND", "BACKEND"];
const STATUSES = Object.keys(PROFILE_STATUS_LABEL) as ProfileStatus[];
/** ALUMNI-01 정렬은 세 가지 (기수 내림차순은 API에 없어서 뺐다) */
const SORT_LABEL: Record<AlumniSort, string> = { RECOMMEND: "추천순", GENERATION: "앞 기수부터", LATEST: "최근 가입순" };
const SIZE = 20;

export default function AlumniPage() {
  return (
    <Suspense fallback={<Skeleton className="h-[600px] w-full" />}>
      <AlumniList />
    </Suspense>
  );
}

function useDebounced<T>(value: T, ms = 300): T {
  const [v, setV] = useState(value);
  useEffect(() => {
    const t = setTimeout(() => setV(value), ms);
    return () => clearTimeout(t);
  }, [value, ms]);
  return v;
}

/** 내 멋사 파트 ('나와 같은 파트' 빠른 필터). API 모드는 PROFILE-02, 프로토타입은 mock */
function useMyPart(): ApiLionPart | null {
  const { alumni } = useStore();
  const mine = useApi(() => getMyProfileParts(), [], API_ENABLED).data;
  if (API_ENABLED) return mine?.lionParts[0] ?? null;
  const part = alumni.find((a) => a.id === ME_ID)?.part;
  return part && part !== "ETC" ? part : null;
}

/** ★ S-01 동문 목록 / 기본 · ◇ S-03 검색·필터 적용됨 · [S] S-04 상태 모음 (왼쪽 필터 패널 + 4열 카드 그리드) */
function AlumniList() {
  const service = useAlumniService();
  const toast = useToast();
  const { onRequest, onMessage, modals } = useAlumniActions();
  const params = useSearchParams();
  const router = useRouter();
  const myPart = useMyPart();

  const [q, setQ] = useState(params.get("q") ?? "");
  const [onlyOpen, setOnlyOpen] = useState(params.get("open") === "1");
  const [gens, setGens] = useState<number[]>(params.get("gen") ? params.get("gen")!.split(",").map(Number) : []);
  const [parts, setParts] = useState<ApiLionPart[]>(params.get("part") ? (params.get("part")!.split(",").filter((p) => PARTS.includes(p as ApiLionPart)) as ApiLionPart[]) : []);
  const [statuses, setStatuses] = useState<ProfileStatus[]>([]);
  const [interests, setInterests] = useState<InterestLabel[]>([]);
  const [moreInterests, setMoreInterests] = useState(false);
  const [sort, setSort] = useState<AlumniSort>("RECOMMEND");
  const [sortOpen, setSortOpen] = useState(false);
  const query = useDebounced(q.trim());

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
  const appliedCount = Number(onlyOpen) + Number(gens.length > 0) + parts.length + statuses.length + interests.length + Number(q.trim().length > 0);
  const applied = appliedCount > 0;
  const sortedGens = [...gens].sort((a, b) => a - b);
  const contiguous = sortedGens.length > 1 && sortedGens[sortedGens.length - 1] - sortedGens[0] === sortedGens.length - 1;
  const genLabel = contiguous ? `${sortedGens[0]}~${sortedGens[sortedGens.length - 1]}기` : sortedGens.map((g) => `${g}기`).join(", ");

  const filters = { q: query || undefined, generations: gens, parts, statuses, interests: interests.map((i) => INTEREST_CODE[i]), available: onlyOpen, sort };
  const filterKey = JSON.stringify(filters);
  const first = useApi((signal) => service.list({ ...filters, page: 0, size: SIZE }, signal), [service, filterKey]);
  const [more, setMore] = useState<{ items: AlumniSummary[]; page: number; hasNext: boolean } | null>(null);
  const [loadingMore, setLoadingMore] = useState(false);
  const [seen, setSeen] = useState(first.data);
  if (seen !== first.data) {
    setSeen(first.data);
    setMore(null);
  }
  const list = [...(first.data?.items ?? []), ...(more?.items ?? [])];
  const total = first.data?.totalElements ?? 0;
  const hasNext = more ? more.hasNext : !!first.data?.hasNext;

  const loadMore = async () => {
    setLoadingMore(true);
    try {
      const page = (more?.page ?? 0) + 1;
      const res = await service.list({ ...filters, page, size: SIZE });
      setMore((m) => ({ items: [...(m?.items ?? []), ...res.items], page, hasNext: res.hasNext }));
    } catch (e) {
      toast(errorMessage(e), { kind: "error" });
    } finally {
      setLoadingMore(false);
    }
  };

  // 빠른 필터 (S-01·S-03)
  const quick = {
    samePart: !!myPart && parts.length === 1 && parts[0] === myPart,
    open: onlyOpen,
    employed: statuses.length === 1 && statuses[0] === "EMPLOYED",
  };

  return (
    <div className="grid grid-cols-[264px_minmax(0,1fr)] gap-[43px] min-[1320px]:-mx-[26px]">
      {/* FilterPanel */}
      <aside className="flex h-fit flex-col gap-4 rounded-[10px] border border-line-200 bg-white p-5">
        <div className="flex items-center justify-between">
          <h2 className="t-title3 text-ink-900">필터</h2>
          <button type="button" onClick={reset} className="rounded-full border border-light-layout bg-light-card px-2.5 py-[3px] t-label2 text-[#5a6170] hover:bg-light-layout">
            초기화
          </button>
        </div>
        <div className="flex items-center justify-between">
          <div className="flex flex-col gap-0.5">
            <p className="t-body-bold text-ink-900">지금 커피챗 받는 동문만</p>
            <p className="t-label2 text-ink-500">오늘 기준</p>
          </div>
          <Switch checked={onlyOpen} onChange={setOnlyOpen} label="지금 커피챗 받는 동문만" />
        </div>
        <hr className="border-line-200" />
        <FilterGroup title="기수 (복수 선택)" small>
          <div className="flex flex-wrap gap-1.5">
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
            <Checkbox key={i} label={i} checked={interests.includes(i)} onChange={() => toggle(setInterests, i)} />
          ))}
          <button type="button" onClick={() => setMoreInterests((m) => !m)} className="w-fit t-caption1 text-ink-500 underline underline-offset-2">
            {moreInterests ? "접기" : `+ ${INTEREST_OPTIONS.length - 5}개 더보기`}
          </button>
        </FilterGroup>
      </aside>

      <section className="flex flex-col gap-5">
        <div className="flex flex-col gap-1">
          <h1 className="t-title text-ink-900">동문 찾기</h1>
          <p className="min-h-6 t-body-bold font-medium text-ink-500">{first.data ? (applied ? `검색 결과 ${total}명` : `총 ${total}명의 동문`) : ""}</p>
        </div>

        <label className="flex h-10 items-center gap-2 rounded-[8px] border border-line-300 bg-white px-[14px] focus-within:border-ink-700">
          <Icon name="search" size={18} className="text-ink-400" />
          <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="이름, 회사, 직무, 기술" className="min-w-0 flex-1 bg-transparent text-[15px] outline-none placeholder:text-ink-400" />
          {q && (
            <button type="button" aria-label="지우기" onClick={() => setQ("")} className="text-ink-400 hover:text-ink-900">
              <Icon name="close" size={16} />
            </button>
          )}
        </label>

        <div className="flex items-center justify-between gap-3">
          <div className="flex flex-wrap items-center gap-2">
            <span className="mr-0.5 t-caption1 text-ink-500">빠른 필터</span>
            {myPart && (
              <QuickChip icon="users" selected={quick.samePart} onClick={() => setParts(quick.samePart ? [] : [myPart])}>
                나와 같은 파트
              </QuickChip>
            )}
            <QuickChip icon="coffee" selected={quick.open} onClick={() => setOnlyOpen((o) => !o)}>
              지금 커피챗 가능
            </QuickChip>
            <QuickChip icon="user" selected={quick.employed} onClick={() => setStatuses(quick.employed ? [] : ["EMPLOYED"])}>
              재직 중
            </QuickChip>
          </div>
          <div className="relative shrink-0">
            <button type="button" aria-haspopup="listbox" aria-expanded={sortOpen} onClick={() => setSortOpen((o) => !o)} className="flex h-9 items-center gap-1.5 rounded-[8px] border border-line-300 bg-white px-3 t-label2 text-ink-900 hover:bg-bg-50">
              <Icon name="filter" size={16} className="text-ink-700" />
              {SORT_LABEL[sort]}
              <Icon name="chevron-down" size={16} className="text-ink-700" />
            </button>
            <Popover open={sortOpen} onClose={() => setSortOpen(false)} className="w-[220px] p-2">
              <ul role="listbox" aria-label="정렬">
                {(Object.keys(SORT_LABEL) as AlumniSort[]).map((k) => (
                  <li key={k}>
                    <button
                      type="button"
                      role="option"
                      aria-selected={sort === k}
                      onClick={() => {
                        setSort(k);
                        setSortOpen(false);
                      }}
                      className={cn("flex h-[34px] w-full items-center rounded-[6px] px-3 text-left text-[14px]", sort === k ? "bg-bg-100 font-bold text-ink-900" : "font-medium text-ink-900 hover:bg-bg-50")}
                    >
                      {SORT_LABEL[k]}
                    </button>
                  </li>
                ))}
              </ul>
            </Popover>
          </div>
        </div>

        {applied && (
          <div className="flex items-center gap-2 rounded-[8px] bg-bg-100 px-3 py-2">
            <span className="flex shrink-0 items-center gap-1.5 t-caption1 text-ink-500">
              적용된 필터
              <span className="grid h-[17px] min-w-[17px] place-items-center rounded-full bg-primary px-[5px] text-[11px] font-bold leading-none text-white">{appliedCount}</span>
            </span>
            <span className="h-4 w-px shrink-0 bg-line-300" />
            <div className="flex min-w-0 flex-1 flex-wrap items-center gap-1.5">
              {q.trim() && (
                <AppliedTag label="검색" onRemove={() => setQ("")}>
                  {q.trim()}
                </AppliedTag>
              )}
              {gens.length > 0 && (
                <AppliedTag label="기수" onRemove={() => setGens([])}>
                  {genLabel}
                </AppliedTag>
              )}
              {parts.map((p) => (
                <AppliedTag key={p} label="파트" onRemove={() => toggle(setParts, p)}>
                  {LION_PART_LABEL[p]}
                </AppliedTag>
              ))}
              {statuses.map((s) => (
                <AppliedTag key={s} label="상태" onRemove={() => toggle(setStatuses, s)}>
                  {PROFILE_STATUS_LABEL[s]}
                </AppliedTag>
              ))}
              {interests.map((i) => (
                <AppliedTag key={i} label="관심" onRemove={() => toggle(setInterests, i)}>
                  {i}
                </AppliedTag>
              ))}
              {onlyOpen && <AppliedTag onRemove={() => setOnlyOpen(false)}>지금 받는 동문</AppliedTag>}
            </div>
            <button type="button" onClick={reset} className="shrink-0 t-caption1 text-primary hover:underline">
              모두 지우기
            </button>
          </div>
        )}

        {first.error && !first.data ? (
          <EmptyState title="동문을 불러오지 못했어요" description={errorMessage(first.error)} action={{ label: "다시 시도", onClick: first.reload }} />
        ) : !first.data ? (
          <div className="grid grid-cols-4 gap-x-[17px] gap-y-[40px] pt-5" aria-busy="true">
            {Array.from({ length: 8 }, (_, i) => (
              <Skeleton key={i} className="h-[300px] w-full" />
            ))}
          </div>
        ) : list.length === 0 ? (
          <EmptyState icon="image" title={query ? `“${query}”에 맞는 동문이 없어요. 다른 키워드로 찾아보세요.` : "조건에 맞는 동문이 없어요"} description={query ? undefined : "필터를 줄이거나 다른 검색어로 찾아보세요"} action={{ label: "필터 초기화", onClick: reset }} />
        ) : (
          <div className={cn("grid grid-cols-4 gap-x-[17px] gap-y-[40px] pt-5", first.loading && "opacity-60")}>
            {list.map((a) => (
              <AlumniCard key={a.id} person={a} onRequest={onRequest} onMessage={onMessage} className="w-full" />
            ))}
          </div>
        )}
        {hasNext && (
          <div className="flex justify-center pt-2">
            <Button variant="secondary" loading={loadingMore} onClick={loadMore}>
              더 보기
            </Button>
          </div>
        )}
      </section>
      {modals}
    </div>
  );
}

function FilterGroup({ title, children, small }: { title: string; children: React.ReactNode; small?: boolean }) {
  return (
    <div className="flex flex-col gap-2.5">
      <p className={cn("text-ink-700", small ? "t-label1" : "t-body-bold")}>{title}</p>
      <div className="flex flex-col gap-2.5">{children}</div>
    </div>
  );
}

/** 빠른 필터 칩 — 선택되면 연한 파랑 + 체크 */
function QuickChip({ icon, selected, onClick, children }: { icon: "users" | "coffee" | "user"; selected: boolean; onClick: () => void; children: React.ReactNode }) {
  return (
    <button type="button" aria-pressed={selected} onClick={onClick} className={cn("flex h-9 items-center gap-1.5 rounded-full border px-[14px] t-label2 transition-colors", selected ? "border-blue-100 bg-part-be-bg text-primary" : "border-line-200 bg-white text-ink-900 hover:bg-bg-50")}>
      <Icon name={selected ? "check" : icon} size={15} className={selected ? "text-primary" : "text-ink-700"} />
      {children}
    </button>
  );
}

function AppliedTag({ label, children, onRemove }: { label?: string; children: React.ReactNode; onRemove: () => void }) {
  return (
    <span className="inline-flex h-[26px] items-center gap-1 rounded-[4px] border border-line-200 bg-white pl-2 pr-1 text-[12px] text-ink-500">
      {label}
      <b className="font-semibold text-ink-900">{children}</b>
      <button type="button" aria-label="필터 지우기" onClick={onRemove} className="grid size-[18px] place-items-center rounded text-ink-700 hover:bg-bg-100">
        <Icon name="close" size={12} />
      </button>
    </span>
  );
}
