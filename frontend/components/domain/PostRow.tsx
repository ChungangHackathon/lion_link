"use client";

import Link from "next/link";
import { cn } from "@/lib/cn";
import type { Post } from "@/lib/types";
import { POST_CATEGORY_LABEL } from "@/lib/types";
import { useStore } from "@/lib/store";
import { daysLeft, formatDateTime } from "@/lib/format";
import { StatusBadge } from "@/components/ui/Badge";
import { Icon } from "@/components/ui/Icon";

const monthDay = (iso: string) => {
  const d = new Date(iso);
  return `${d.getMonth() + 1}월 ${d.getDate()}일`;
};

/** Row/Post (B-01) — 카테고리 태그 + D-n/모집 중, 제목 [댓글 수], 오른쪽 작성자·날짜, 썸네일 */
export function PostRow({ post, className }: { post: Post; className?: string }) {
  const { alumni } = useStore();
  const author = alumni.find((a) => a.id === post.authorId);
  const target = post.recruit ? alumni.find((a) => a.id === post.recruit!.targetId) : undefined;
  const who = post.anonymous ? "익명 사자" : `${author?.name} · ${author?.generation}기`;
  const thumb = !!post.images?.[0];
  const sub = post.recruit && target ? `${target.name}(${target.generation}기) 사자 · 참여 ${post.recruit.memberIds.length}/${post.recruit.max}명 · 마감 ${daysLeft(post.recruit.deadline)}` : post.meetup ? `${formatDateTime(post.meetup.at)} · ${post.meetup.place} · 참여 ${post.meetup.memberIds.length}/${post.meetup.max}명` : undefined;
  return (
    <li className={cn("mx-5 border-b border-line-200 last:border-b-0", className)}>
      <Link href={`/community/${post.id}`} className="flex items-center justify-between gap-4 px-1 py-[18px] hover:bg-bg-50">
        <div className="flex min-w-0 flex-1 flex-col gap-2">
          <div className="flex items-center gap-[5px]">
            <span className="rounded-[4px] bg-bg-100 px-2 py-1 t-caption2 text-ink-700">{POST_CATEGORY_LABEL[post.category]}</span>
            {post.deadline && <span className="grid h-[19px] min-w-[39px] place-items-center rounded-[4px] border-[0.5px] border-orange bg-orange-back px-1.5 t-caption2 text-orange-badge">{daysLeft(post.deadline)}</span>}
            {post.recruit && <StatusBadge tone={post.recruit.status === "OPEN" ? "outline" : post.recruit.status === "APPLIED" ? "filled" : "gray"}>{post.recruit.status === "OPEN" ? "모집 중" : post.recruit.status === "APPLIED" ? "신청 완료" : "모집 종료"}</StatusBadge>}
            {post.meetup && <StatusBadge tone={post.meetup.memberIds.length >= post.meetup.max ? "gray" : "outline"}>{post.meetup.memberIds.length >= post.meetup.max ? "모집 마감" : "모집 중"}</StatusBadge>}
            {post.files?.length ? <Icon name="clip" size={15} className="ml-1 text-ink-500" /> : null}
          </div>
          <div className="flex items-center justify-between gap-4">
            <p className="flex min-w-0 items-center gap-[5px] t-body-bold">
              <span className="truncate text-ink-900">{post.title}</span>
              {post.comments.length > 0 && <span className="shrink-0 text-red">[{post.comments.length}]</span>}
            </p>
            {!thumb && !sub && (
              <span className="flex shrink-0 items-center gap-[30px] t-caption1 text-ink-500">
                <span>{who}</span>
                <span>{monthDay(post.createdAt)}</span>
              </span>
            )}
          </div>
          {sub && <p className="t-caption1 text-ink-700">{sub}</p>}
          {(thumb || sub) && (
            <p className="t-caption1 text-ink-500">
              {who} · {monthDay(post.createdAt)} · 댓글 {post.comments.length} · 조회 {post.views}
            </p>
          )}
        </div>
        {thumb && (
          <span className="flex h-[72px] w-[96px] shrink-0 flex-col items-center justify-center gap-1 rounded-[8px] border border-dashed border-line-300 bg-bg-100 text-[12px] text-ink-400">
            <Icon name="image" size={20} /> Image
          </span>
        )}
      </Link>
    </li>
  );
}
