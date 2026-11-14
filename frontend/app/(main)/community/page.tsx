"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useMemo, useState } from "react";
import { cn } from "@/lib/cn";
import { useStore } from "@/lib/store";
import { POPULAR_KEYWORDS } from "@/lib/mock/posts";
import { POST_CATEGORIES, POST_CATEGORY_LABEL, type PostCategory } from "@/lib/types";
import { daysLeft, formatDateTime } from "@/lib/format";
import { AvatarGroup } from "@/components/ui/Avatar";
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

/** ★ B-01 커뮤니티 / 게시글 목록 — 왼쪽 세로 카테고리 · 가운데 목록 · 오른쪽 모집 중·많이 본 글·인기 키워드. B-02/B-03 상태. */
function Community() {
  const params = useSearchParams();
  const router = useRouter();
  const { posts, alumni, blocked } = useStore();
  const [cat, setCat] = useState<PostCategory | "ALL">((params.get("cat") as PostCategory) ?? "ALL");
  const [q, setQ] = useState(params.get("q") ?? "");
  const [sort, setSort] = useState<"newest" | "popular">("newest");
  const [page, setPage] = useState(1);
  const PER = 7;

  const visible = useMemo(() => posts.filter((p) => !blocked.includes(p.authorId)), [posts, blocked]);
  const list = useMemo(() => {
    const kw = q.trim().toLowerCase();
    const r = visible.filter((p) => (cat === "ALL" || p.category === cat) && (!kw || `${p.title} ${p.body} ${p.keywords.join(" ")}`.toLowerCase().includes(kw)));
    return sort === "popular" ? [...r].sort((a, b) => b.views - a.views) : [...r].sort((a, b) => b.createdAt.localeCompare(a.createdAt));
  }, [visible, cat, q, sort]);
  const pages = Math.max(1, Math.ceil(list.length / PER));
  const paged = list.slice((page - 1) * PER, page * PER);
  const count = (c: PostCategory | "ALL") => visible.filter((p) => c === "ALL" || p.category === c).length;

  const recruits = visible.filter((p) => (p.recruit && p.recruit.status === "OPEN") || (p.meetup && new Date(p.meetup.at).getTime() > Date.now())).slice(0, 3);
  const weekAgo = Date.now() - 7 * 86_400_000;
  const popular = [...visible.filter((p) => new Date(p.createdAt).getTime() > weekAgo)].sort((a, b) => b.views - a.views).slice(0, 3);

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
            <input value={q} onChange={(e) => { setQ(e.target.value); setPage(1); }} placeholder={`${cat === "ALL" ? "전체" : POST_CATEGORY_LABEL[cat]} 카테고리에서 제목·본문 검색`} className="min-w-0 flex-1 text-[14px] outline-none placeholder:text-ink-400" />
          </label>
          <select aria-label="정렬" value={sort} onChange={(e) => setSort(e.target.value as "newest" | "popular")} className="h-9 rounded-[6px] border-0 bg-transparent t-label2 text-ink-900">
            <option value="newest">최신순</option>
            <option value="popular">조회순</option>
          </select>
        </div>
        {q.trim() && <p className="px-6 pb-2 text-[13px] text-ink-500">‘{q.trim()}’ 검색 결과 {list.length}개</p>}
        {paged.length === 0 ? (
          <EmptyState title={q.trim() ? "검색 결과가 없어요" : "아직 글이 없어요"} description={q.trim() ? "다른 검색어로 찾아보세요" : "첫 글을 남겨보세요"} action={q.trim() ? { label: "검색 지우기", onClick: () => setQ("") } : { label: "글쓰기", href: "/community/new" }} />
        ) : (
          <ul>
            {paged.map((p) => (
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
              const ids = p.recruit?.memberIds ?? p.meetup?.memberIds ?? [];
              const max = p.recruit?.max ?? p.meetup?.max ?? 0;
              const people = ids.map((id) => alumni.find((a) => a.id === id)).filter((a) => !!a);
              return (
                <li key={p.id} className="border-b border-line-200 py-3 last:border-b-0">
                  <Link href={`/community/${p.id}`} className="group flex flex-col gap-2">
                    <span className="flex items-center gap-2 text-[12px] text-ink-500">
                      <StatusBadge tone="outline">모집 중</StatusBadge>
                      {p.recruit ? `마감 ${daysLeft(p.recruit.deadline)}` : p.meetup ? formatDateTime(p.meetup.at) : ""}
                    </span>
                    <span className="truncate t-label2 text-ink-900 group-hover:underline">{p.title}</span>
                    <span className="flex items-center gap-2 t-caption2 text-ink-700">
                      {people.length > 0 && <AvatarGroup people={people.map((a) => ({ name: a!.name, part: a!.part }))} size={28} />}
                      {ids.length}/{max}명
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
                          <Icon name="chat" size={13} /> {p.comments.length}
                        </span>
                      </span>
                    </span>
                  </Link>
                </li>
              ))}
            </ol>
            <button type="button" onClick={() => setSort("popular")} className="w-fit pt-2 t-label2 text-ink-900 hover:underline">
              더보기
            </button>
          </Card>
        )}
        <Card className="flex flex-col gap-4 rounded-[10px] p-5">
          <h2 className="t-title3 text-ink-900">인기 키워드</h2>
          <div className="flex flex-wrap gap-2">
            {POPULAR_KEYWORDS.map((k) => (
              <button key={k} type="button" onClick={() => { setQ(k); setCat("ALL"); setPage(1); }} className="rounded-full border border-line-300 bg-white px-3 py-1.5 text-[12px] font-semibold text-ink-700 hover:bg-bg-100">
                #{k}
              </button>
            ))}
          </div>
        </Card>
        <p className="text-[11px] text-ink-400">이용약관 · 개인정보처리방침 · 피드백 보내기</p>
      </aside>
      <span className="hidden">{alumni.length}</span>
    </div>
  );
}
