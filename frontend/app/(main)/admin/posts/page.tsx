"use client";

import { useState } from "react";
import { POST_CATEGORY_LABEL } from "@/lib/types";
import { formatDate } from "@/lib/format";
import { errorMessage } from "@/lib/api/client";
import type { PostSummary } from "@/lib/api/posts";
import { useApi } from "@/lib/api/useApi";
import { usePostService } from "@/lib/api/usePostService";
import { Tag } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { ConfirmModal } from "@/components/ui/Modal";
import { Card, EmptyState, Skeleton } from "@/components/ui/Misc";
import { useToast } from "@/components/ui/Toast";
import { authorLabel } from "@/components/domain/PostRow";

const PAGE_SIZE = 20;

/** 게시글 관리 — 와이어프레임에는 메뉴만 있고 화면이 없어 최소 목록으로 구성. 목록은 POST-01, 삭제는 B-13 '삭제 (관리자)'와 같은 삭제 API */
export default function AdminPostsPage() {
  const toast = useToast();
  const service = usePostService();
  const [q, setQ] = useState("");
  const [query, setQuery] = useState("");
  const { data, error, reload, setData } = useApi((signal) => service.list({ q: query || undefined, page: 0, size: PAGE_SIZE }, signal), [service, query]);
  const [removing, setRemoving] = useState<PostSummary | null>(null);
  const [busy, setBusy] = useState(false);

  const remove = async () => {
    if (!removing) return;
    setBusy(true);
    try {
      await service.remove(removing.id);
      setData((d) => d && { ...d, items: d.items.filter((p) => p.id !== removing.id), totalElements: d.totalElements - 1 });
      toast("글을 삭제했어요");
    } catch (e) {
      toast(errorMessage(e), { kind: "error" });
    } finally {
      setBusy(false);
      setRemoving(null);
    }
  };

  const more = async () => {
    if (!data) return;
    try {
      const next = await service.list({ q: query || undefined, page: data.page + 1, size: PAGE_SIZE });
      setData((d) => d && { ...next, items: [...d.items, ...next.items.filter((x) => !d.items.some((y) => y.id === x.id))] });
    } catch (e) {
      toast(errorMessage(e), { kind: "error" });
    }
  };

  return (
    <div className="flex flex-col gap-5">
      <h1 className="t-title text-ink-900">게시글 관리</h1>
      <p className="text-[13px] text-ink-500">와이어프레임에 화면이 없어 최소 목록만 보여요. 삭제는 B-13 ‘삭제 (관리자)’와 같아요. 댓글은 글 상세에서 지울 수 있어요.</p>
      <form
        className="flex max-w-[480px] gap-2"
        onSubmit={(e) => {
          e.preventDefault();
          setQuery(q.trim());
        }}
      >
        <Input aria-label="게시글 검색" placeholder="제목·본문 검색" value={q} onChange={(e) => setQ(e.target.value)} />
        <Button type="submit" variant="secondary">
          검색
        </Button>
      </form>
      {error && !data ? (
        <EmptyState title="게시글을 불러오지 못했어요" description={errorMessage(error)} action={{ label: "다시 시도", onClick: reload }} />
      ) : !data ? (
        <Skeleton className="h-[320px] w-full" />
      ) : data.items.length === 0 ? (
        <EmptyState title={query ? "검색 결과가 없어요" : "게시글이 없어요"} />
      ) : (
        <Card className="divide-y divide-line-200">
          {data.items.map((p) => (
            <div key={String(p.id)} className="flex items-center justify-between gap-4 px-5 py-3">
              <div className="flex min-w-0 flex-col gap-1">
                <span className="flex items-center gap-2">
                  <Tag>{POST_CATEGORY_LABEL[p.category]}</Tag>
                  <span className="truncate t-label1 text-ink-900">{p.title}</span>
                </span>
                <span className="text-[12px] text-ink-500">
                  {authorLabel(p.author)} · {formatDate(p.createdAt)} · 댓글 {p.commentCount} · 조회 {p.viewCount}
                </span>
              </div>
              <div className="flex shrink-0 gap-1.5">
                <Button size="S" variant="secondary" href={`/community/${p.id}`}>
                  보기
                </Button>
                <Button size="S" variant="danger" aria-label={`${p.title} 삭제`} onClick={() => setRemoving(p)}>
                  삭제
                </Button>
              </div>
            </div>
          ))}
          {data.hasNext && (
            <div className="p-3 text-center">
              <Button size="S" variant="secondary" onClick={() => void more()}>
                더 보기
              </Button>
            </div>
          )}
        </Card>
      )}
      <ConfirmModal open={!!removing} onClose={() => setRemoving(null)} onConfirm={() => void remove()} loading={busy} danger title="이 글을 삭제할까요?" description={removing ? `${removing.title} · 삭제하면 되돌릴 수 없어요.` : undefined} confirmLabel="삭제" />
    </div>
  );
}
