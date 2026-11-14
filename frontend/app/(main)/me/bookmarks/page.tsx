"use client";

import { useState } from "react";
import { errorMessage } from "@/lib/api/client";
import type { BookmarkedAlumni } from "@/lib/api/alumni";
import { useApi } from "@/lib/api/useApi";
import { useAlumniService } from "@/lib/api/useAlumniService";
import { Button } from "@/components/ui/Button";
import { EmptyState, Skeleton } from "@/components/ui/Misc";
import { useToast } from "@/components/ui/Toast";
import { AlumniCard } from "@/components/domain/AlumniCard";
import { useAlumniActions } from "@/components/domain/useAlumniActions";

const SIZE = 18;

/** ◇ X-01 관심 동문 / 목록 (ALUMNI-03, 최근 저장순). ☆를 풀어도 다시 불러오기 전까지는 카드가 남는다 */
export default function BookmarksPage() {
  const service = useAlumniService();
  const toast = useToast();
  const { onRequest, onMessage, modals } = useAlumniActions();
  const first = useApi(() => service.bookmarks(0, SIZE), [service]);
  const [more, setMore] = useState<{ items: BookmarkedAlumni[]; page: number; hasNext: boolean } | null>(null);
  const [loadingMore, setLoadingMore] = useState(false);
  const [seen, setSeen] = useState(first.data);
  if (seen !== first.data) {
    setSeen(first.data);
    setMore(null);
  }

  const list = [...(first.data?.items ?? []), ...(more?.items ?? [])];
  const hasNext = more ? more.hasNext : !!first.data?.hasNext;

  const loadMore = async () => {
    setLoadingMore(true);
    try {
      const page = (more?.page ?? 0) + 1;
      const res = await service.bookmarks(page, SIZE);
      setMore((m) => ({ items: [...(m?.items ?? []), ...res.items], page, hasNext: res.hasNext }));
    } catch (e) {
      toast(errorMessage(e), { kind: "error" });
    } finally {
      setLoadingMore(false);
    }
  };

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-end justify-between">
        <h1 className="t-title text-ink-900">관심 동문</h1>
        {first.data && <p className="text-[13px] text-ink-500">최근 저장순 · {first.data.totalElements}명</p>}
      </div>
      {first.error && !first.data ? (
        <EmptyState title="관심 동문을 불러오지 못했어요" description={errorMessage(first.error)} action={{ label: "다시 시도", onClick: first.reload }} />
      ) : !first.data ? (
        <div className="grid grid-cols-3 gap-6 pt-2">
          {Array.from({ length: 3 }, (_, i) => (
            <Skeleton key={i} className="h-[300px] w-full" />
          ))}
        </div>
      ) : list.length === 0 ? (
        <EmptyState title="저장한 동문이 없어요" description="동문 카드의 ☆를 누르면 여기에 모여요" action={{ label: "동문 찾기", href: "/alumni" }} />
      ) : (
        <div className="grid grid-cols-3 gap-6 pt-2">
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
      {modals}
    </div>
  );
}
