"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";
import { cn } from "@/lib/cn";
import { POST_CATEGORIES, POST_CATEGORY_LABEL, type PostCategory } from "@/lib/types";
import { daysLeft, formatDateTime } from "@/lib/format";
import { errorMessage } from "@/lib/api/client";
import { useApi } from "@/lib/api/useApi";
import { usePostService } from "@/lib/api/usePostService";
import { StatusBadge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Icon } from "@/components/ui/Icon";
import { Card, EmptyState, Skeleton } from "@/components/ui/Misc";
import { PostRow } from "@/components/domain/PostRow";

export default function CommunityPage() {
  return (
    <Suspense fallback={<Skeleton className="h-[600px] w-full" />}>
      <Community />
    </Suspense>
  );
}

const PER = 7;
type Sort = "LATEST" | "VIEWS";

/** 입력이 멈추고 300ms 뒤의 값 (POST-08: 글자마다 검색하면 중간 글자도 인기 키워드에 남는다) */
function useDebounced<T>(value: T, ms = 300): T {
  const [v, setV] = useState(value);
  useEffect(() => {
    const t = setTimeout(() => setV(value), ms);
    return () => clearTimeout(t);
  }, [value, ms]);
  return v;
}

/** ★ B-01 커뮤니티 / 게시글 목록 — 왼쪽 세로 카테고리 · 가운데 목록 · 오른쪽 모집 중·많이 본 글·인기 키워드. B-02/B-03 상태. */
function Community() {
  const params = useSearchParams();
  const router = useRouter();
  const service = usePostService();
  const [cat, setCat] = useState<PostCategory | "ALL">((params.get("cat") as PostCategory) ?? "ALL");
  const [q, setQ] = useState(params.get("q") ?? "");
  const [sort, setSort] = useState<Sort>("LATEST");
  const [page, setPage] = useState(1);
  const query = useDebounced(q.trim());

  const listState = useApi((signal) => service.list({ category: cat === "ALL" ? undefined : [cat], q: query || undefined, sort, page: page - 1, size: PER }, signal), [service, cat, query, sort, page]);
  const counts = useApi(() => service.categoryCounts(), [service]).data;
  const recruits = useApi((signal) => service.list({ recruiting: true, size: 3 }, signal), [service]).data?.items ?? [];
  const popular = useApi((signal) => service.list({ period: "WEEK", sort: "VIEWS", size: 3 }, signal), [service]).data?.items ?? [];
  const keywords = useApi(() => service.popularKeywords(10), [service]).data?.items ?? [];

  const list = listState.data;
  const total = list?.totalElements ?? 0;
  const pages = Math.max(1, Math.ceil(total / PER));
  const count = (c: PostCategory | "ALL") => (counts ? (c === "ALL" ? counts.total : counts.counts[c]) : "");
  const searching = !!query;

  const search = (value: string) => {
    setQ(value);
    setPage(1);
  };

  return (
    <div className="grid grid-cols-[220px_1fr_312px] gap-6">
      {/* Left: 글쓰기 + 카테고리 */}
      <aside className="flex flex-col gap-4">
        <Button size="L" full href="/community/new" className="text-[18px] font-bold shadow-[0_2px_4px_rgba(0,0,0,0.15)]" leading={<Icon name="edit" size={24} />}>
          글쓰기
        </Button>
        <Card className="rounded-[10px] p-2">
          {(["ALL", ...POST_CATEGORIES] as const).map((c) => (
            <button
              key={c}
              type="button"
              onClick={() => {
                setCat(c);
                setPage(1);
                router.replace(c === "ALL" ? "/community" : `/community?cat=${c}`);
              }}
              className={cn("flex w-full items-center justify-between rounded-[6px] px-3 py-[9px] text-[14px] hover:bg-bg-50", cat === c ? "bg-bg-100 font-bold text-ink-900" : "font-medium text-ink-700")}
            >
              {c === "ALL" ? "전체" : POST_CATEGORY_LABEL[c]}
              <span className="text-[12px] text-ink-400">{count(c)}</span>
            </button>
          ))}
        </Card>
      </aside>

      {/* Center: list */}
      <Card className="flex h-fit min-w-0 flex-col rounded-[10px]">
        <div className="flex items-center justify-between gap-4 px-5 pb-2 pt-5">
          <label className="flex h-10 w-[380px] items-center gap-2 rounded-[8px] border border-line-300 px-3 focus-within:border-ink-700">
            <Icon name="search" size={18} className="text-ink-400" />
            <input value={q} onChange={(e) => search(e.target.value)} placeholder={`${cat === "ALL" ? "전체" : POST_CATEGORY_LABEL[cat]} 카테고리에서 제목·본문 검색`} className="min-w-0 flex-1 text-[14px] outline-none placeholder:text-ink-400" />
          </label>
          <select aria-label="정렬" value={sort} onChange={(e) => { setSort(e.target.value as Sort); setPage(1); }} className="h-9 rounded-[6px] border-0 bg-transparent t-label2 text-ink-900">
            <option value="LATEST">최신순</option>
            <option value="VIEWS">조회순</option>
          </select>
        </div>
        {searching && list && <p className="px-6 pb-2 text-[13px] text-ink-500">‘{query}’ 검색 결과 {total}개</p>}
        {listState.error && !list ? (
          <EmptyState title="글을 불러오지 못했어요" description={errorMessage(listState.error)} action={{ label: "다시 시도", onClick: listState.reload }} />
        ) : !list ? (
          <div className="flex flex-col gap-3 px-6 py-5" aria-busy="true">
            {Array.from({ length: 4 }, (_, i) => (
              <Skeleton key={i} className="h-[64px] w-full" />
            ))}
          </div>
        ) : list.items.length === 0 ? (
          <EmptyState title={searching ? "검색 결과가 없어요" : "아직 글이 없어요"} description={searching ? "다른 검색어로 찾아보세요" : "첫 글을 남겨보세요"} action={searching ? { label: "검색 지우기", onClick: () => search("") } : { label: "글쓰기", href: "/community/new" }} />
        ) : (
          <ul className={cn(listState.loading && "opacity-60")}>
            {list.items.map((p) => (
              <PostRow key={p.id} post={p} />
            ))}
          </ul>
        )}
        {pages > 1 && (
          <nav className="flex items-center justify-center gap-1 py-5 text-[14px]" aria-label="페이지">
            <button type="button" disabled={page === 1} onClick={() => setPage((p) => p - 1)} className="px-2 text-ink-400 disabled:opacity-30">
              ‹
            </button>
            {Array.from({ length: pages }, (_, i) => i + 1).map((n) => (
              <button key={n} type="button" onClick={() => setPage(n)} className={cn("grid size-8 place-items-center rounded-full", page === n ? "bg-primary font-bold text-white" : "text-ink-700 hover:bg-bg-100")}>
                {n}
              </button>
            ))}
            <button type="button" disabled={page === pages} onClick={() => setPage((p) => p + 1)} className="px-2 text-ink-400 disabled:opacity-30">
              ›
            </button>
          </nav>
        )}
      </Card>

      {/* Right rail */}
      <aside className="flex flex-col gap-4">
        <Card className="flex flex-col gap-2 rounded-[10px] p-5">
          <h2 className="t-title3 text-ink-900">모집 중인 번개·커피챗</h2>
          <ul>
            {recruits.length === 0 && <li className="py-3 text-[13px] text-ink-500">모집 중인 글이 없어요</li>}
            {recruits.map((p) => {
              const joined = p.recruit?.memberCount ?? p.meetup?.participantCount ?? 0;
              const max = p.recruit?.capacity ?? p.meetup?.capacity ?? 0;
              return (
                <li key={p.id} className="border-b border-line-200 py-3 last:border-b-0">
                  <Link href={`/community/${p.id}`} className="group flex flex-col gap-2">
                    <span className="flex items-center gap-2 text-[12px] text-ink-500">
                      <StatusBadge tone="outline">모집 중</StatusBadge>
                      {p.recruit ? `마감 ${daysLeft(p.recruit.deadline)}` : p.meetup ? formatDateTime(p.meetup.meetAt) : ""}
                    </span>
                    <span className="truncate t-label2 text-ink-900 group-hover:underline">{p.title}</span>
                    <span className="flex items-center gap-2 t-caption2 text-ink-700">
                      <Icon name="user" size={14} className="text-ink-500" />
                      {joined}/{max}명
                    </span>
                  </Link>
                </li>
              );
            })}
          </ul>
        </Card>
        {popular.length > 0 && (
          <Card className="flex flex-col gap-1 rounded-[10px] p-5 shadow-[0_2px_6px_rgba(0,0,0,0.12)]">
            <h2 className="pb-2 t-title3 text-ink-900">이번 주 많이 본 글</h2>
            <ol>
              {popular.map((p, i) => (
                <li key={p.id} className="border-b border-line-200 py-3 last:border-b-0">
                  <Link href={`/community/${p.id}`} className="group flex gap-4">
                    <span className={cn("w-3 text-[18px] font-bold leading-6", i === 0 ? "text-orange" : "text-primary")}>{i + 1}</span>
                    <span className="flex min-w-0 flex-1 flex-col gap-1.5">
                      <span className="truncate t-label1 text-ink-900 group-hover:underline">{p.title}</span>
                      <span className="flex items-center justify-between t-caption1">
                        <span className="font-medium text-primary">{POST_CATEGORY_LABEL[p.category]}</span>
                        <span className="flex items-center gap-1 text-ink-700">
                          <Icon name="chat" size={13} /> {p.commentCount}
                        </span>
                      </span>
                    </span>
                  </Link>
                </li>
              ))}
            </ol>
            <button type="button" onClick={() => { setSort("VIEWS"); setPage(1); }} className="w-fit pt-2 t-label2 text-ink-900 hover:underline">
              더보기
            </button>
          </Card>
        )}
        <Card className="flex flex-col gap-4 rounded-[10px] p-5">
          <h2 className="t-title3 text-ink-900">인기 키워드</h2>
          {keywords.length === 0 ? (
            <p className="text-[13px] text-ink-500">아직 인기 키워드가 없어요</p>
          ) : (
            <div className="flex flex-wrap gap-2">
              {keywords.map(({ keyword }) => (
                <button key={keyword} type="button" onClick={() => { setCat("ALL"); search(keyword); router.replace("/community"); }} className="rounded-full border border-line-300 bg-white px-3 py-1.5 text-[12px] font-semibold text-ink-700 hover:bg-bg-100">
                  #{keyword}
                </button>
              ))}
            </div>
          )}
        </Card>
        <p className="text-[11px] text-ink-400">이용약관 · 개인정보처리방침 · 피드백 보내기</p>
      </aside>
    </div>
  );
}
