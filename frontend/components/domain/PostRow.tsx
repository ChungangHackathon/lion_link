"use client";

import Link from "next/link";
import { cn } from "@/lib/cn";
import type { Post } from "@/lib/types";
import { POST_CATEGORY_LABEL } from "@/lib/types";
import { useStore } from "@/lib/store";
import { daysLeft, formatDate, formatDateTime } from "@/lib/format";
import { StatusBadge, Tag } from "@/components/ui/Badge";
import { Glyph } from "@/components/ui/Icon";

/** Row/Post (90:570) — category tag + D-n/모집 중, title, meta, optional thumbnail */
export function PostRow({ post, className }: { post: Post; className?: string }) {
  const { alumni } = useStore();
  const author = alumni.find((a) => a.id === post.authorId);
  const target = post.recruit ? alumni.find((a) => a.id === post.recruit!.targetId) : undefined;
  return (
    <li className={cn("border-b border-line-200 last:border-b-0", className)}>
      <Link href={`/community/${post.id}`} className="flex items-center justify-between gap-4 px-6 py-5 hover:bg-bg-50">
        <div className="flex min-w-0 flex-col gap-2">
          <div className="flex items-center gap-1.5">
            <Tag className="font-semibold">{POST_CATEGORY_LABEL[post.category]}</Tag>
            {post.deadline && <span className="rounded-[4px] border-[0.5px] border-orange bg-orange-back px-2 py-0.5 text-[12px] font-semibold text-ink-700">{daysLeft(post.deadline)}</span>}
            {post.recruit && <StatusBadge tone={post.recruit.status === "OPEN" ? "outline" : post.recruit.status === "APPLIED" ? "filled" : "gray"}>{post.recruit.status === "OPEN" ? "모집 중" : post.recruit.status === "APPLIED" ? "신청 완료" : "모집 종료"}</StatusBadge>}
            {post.meetup && <StatusBadge tone={post.meetup.memberIds.length >= post.meetup.max ? "gray" : "outline"}>{post.meetup.memberIds.length >= post.meetup.max ? "모집 마감" : "모집 중"}</StatusBadge>}
            {post.files?.length ? <Glyph name="link" size={14} className="text-ink-400" /> : null}
          </div>
          <p className="truncate text-[17px] font-bold text-ink-900">{post.title}</p>
          {post.recruit && target && (
            <p className="text-[13px] text-ink-700">
              {target.name}({target.generation}기) 사자 · 참여 {post.recruit.memberIds.length}/{post.recruit.max}명 · 마감 {daysLeft(post.recruit.deadline)}
            </p>
          )}
          {post.meetup && (
            <p className="text-[13px] text-ink-700">
              {formatDateTime(post.meetup.at)} · {post.meetup.place} · 참여 {post.meetup.memberIds.length}/{post.meetup.max}명
            </p>
          )}
          <p className="text-[13px] text-ink-500">
            {post.anonymous ? "익명" : author?.name} · {post.anonymous ? "" : `${author?.generation}기 · `}
            {formatDate(post.createdAt)} · 댓글 {post.comments.length} · 조회 {post.views}
          </p>
        </div>
        {post.images?.[0] && (
          <span className="grid h-[72px] w-[96px] shrink-0 place-items-center rounded-[8px] border border-dashed border-line-300 bg-bg-100 text-[12px] text-ink-400">
            <span className="flex flex-col items-center">
              <Glyph name="image" size={18} /> Image
            </span>
          </span>
        )}
      </Link>
    </li>
  );
}
