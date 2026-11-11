"use client";

import { useStore } from "@/lib/store";
import { POST_CATEGORY_LABEL } from "@/lib/types";
import { formatDate } from "@/lib/format";
import { Tag } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Misc";
import { useToast } from "@/components/ui/Toast";

/** 게시글 관리 — 와이어프레임에는 메뉴만 있고 화면이 없어 최소 목록으로 구성 */
export default function AdminPostsPage() {
  const toast = useToast();
  const { posts, alumni, deletePost } = useStore();
  return (
    <div className="flex flex-col gap-5">
      <h1 className="t-title text-ink-900">게시글 관리</h1>
      <p className="text-[13px] text-ink-500">와이어프레임에 화면이 없어 임시로 목록만 보여요. 삭제는 B-13 ‘삭제 (관리자)’와 같아요.</p>
      <Card className="divide-y divide-line-200">
        {posts.map((p) => {
          const a = alumni.find((x) => x.id === p.authorId);
          return (
            <div key={p.id} className="flex items-center justify-between gap-4 px-5 py-3">
              <div className="flex min-w-0 flex-col gap-1">
                <span className="flex items-center gap-2">
                  <Tag>{POST_CATEGORY_LABEL[p.category]}</Tag>
                  <span className="truncate t-label1 text-ink-900">{p.title}</span>
                </span>
                <span className="text-[12px] text-ink-500">
                  {p.anonymous ? "익명" : a?.name} · {formatDate(p.createdAt)} · 댓글 {p.comments.length}
                </span>
              </div>
              <div className="flex shrink-0 gap-1.5">
                <Button size="S" variant="secondary" href={`/community/${p.id}`}>
                  보기
                </Button>
                <Button size="S" variant="danger" onClick={() => { deletePost(p.id); toast("글을 삭제했어요"); }}>
                  삭제
                </Button>
              </div>
            </div>
          );
        })}
      </Card>
    </div>
  );
}
