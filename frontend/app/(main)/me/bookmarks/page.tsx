"use client";

import { useStore } from "@/lib/store";
import { EmptyState } from "@/components/ui/Misc";
import { AlumniCard } from "@/components/domain/AlumniCard";
import { useAlumniActions } from "@/components/domain/useAlumniActions";

/** ◇ X-01 관심 동문 / 목록 */
export default function BookmarksPage() {
  const { alumni, bookmarks } = useStore();
  const { onRequest, onMessage, modals } = useAlumniActions();
  const list = bookmarks.map((id) => alumni.find((a) => a.id === id)!).filter(Boolean).reverse();
  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-end justify-between">
        <h1 className="t-title text-ink-900">관심 동문</h1>
        <p className="text-[13px] text-ink-500">최근 저장순 · {list.length}명</p>
      </div>
      {list.length === 0 ? (
        <EmptyState title="저장한 동문이 없어요" description="동문 카드의 ☆를 누르면 여기에 모여요" action={{ label: "동문 찾기", href: "/alumni" }} />
      ) : (
        <div className="grid grid-cols-3 gap-6 pt-2">
          {list.map((a) => (
            <AlumniCard key={a.id} person={a} onRequest={onRequest} onMessage={onMessage} className="w-full" />
          ))}
        </div>
      )}
      {modals}
    </div>
  );
}
