"use client";

import Link from "next/link";
import { cn } from "@/lib/cn";
import { POST_CATEGORY_LABEL } from "@/lib/types";
import type { PostAuthor, PostSummary } from "@/lib/api/posts";
import { daysLeft, formatDateTime } from "@/lib/format";
import { StatusBadge } from "@/components/ui/Badge";
import { Icon } from "@/components/ui/Icon";

const monthDay = (iso: string) => {
  const d = new Date(iso);
  return `${d.getMonth() + 1}월 ${d.getDate()}일`;
};

/** 작성자 표시: 익명 → '익명 사자', 탈퇴 → '알 수 없음' */
export function authorLabel(author: PostAuthor | null, withGeneration = true): string {
  if (!author) return "익명 사자";
  if (author.isDeleted || !author.name) return "알 수 없음";
  return withGeneration && author.generation ? `${author.name} · ${author.generation}기` : author.name;
}

/** 실제 이미지 주소인지 (mock 데이터는 자리표시 문자열) */
export const isImageUrl = (url: string | null | undefined): url is string => !!url && /^(https?:\/\/|\/|blob:|data:)/.test(url);

const RECRUIT_LABEL = { OPEN: "모집 중", APPLIED: "신청 완료", ENDED: "모집 종료" } as const;
const MEETUP_LABEL = { OPEN: "모집 중", FULL: "모집 마감", ENDED: "종료", CANCELED: "취소됨" } as const;

/** Row/Post (B-01) — 카테고리 태그 + D-n/모집 중, 제목 [댓글 수], 오른쪽 작성자·날짜, 썸네일 */
export function PostRow({ post, className }: { post: PostSummary; className?: string }) {
  const who = authorLabel(post.author);
  const thumb = !!post.thumbnailUrl;
  const senior = post.recruit?.senior;
  const sub = post.recruit
    ? `${senior?.isDeleted || !senior?.name ? "알 수 없음" : `${senior.name}(${senior.generation}기)`} 사자 · 참여 ${post.recruit.memberCount}/${post.recruit.capacity}명 · 마감 ${daysLeft(post.recruit.deadline)}`
    : post.meetup
      ? `${formatDateTime(post.meetup.meetAt)} · ${post.meetup.place} · 참여 ${post.meetup.participantCount}/${post.meetup.capacity}명`
      : undefined;
  return (
    <li className={cn("mx-5 border-b border-line-200 last:border-b-0", className)}>
      <Link href={`/community/${post.id}`} className="flex items-center justify-between gap-4 px-1 py-[18px] hover:bg-bg-50">
        <div className="flex min-w-0 flex-1 flex-col gap-2">
          <div className="flex items-center gap-[5px]">
            <span className="rounded-[4px] bg-bg-100 px-2 py-1 t-caption2 text-ink-700">{POST_CATEGORY_LABEL[post.category]}</span>
            {post.deadline && <span className="grid h-[19px] min-w-[39px] place-items-center rounded-[4px] border-[0.5px] border-orange bg-orange-back px-1.5 t-caption2 text-orange-badge">{daysLeft(post.deadline)}</span>}
            {post.recruit && <StatusBadge tone={post.recruit.status === "OPEN" ? "outline" : post.recruit.status === "APPLIED" ? "filled" : "gray"}>{RECRUIT_LABEL[post.recruit.status]}</StatusBadge>}
            {post.meetup && <StatusBadge tone={post.meetup.status === "OPEN" ? "outline" : "gray"}>{MEETUP_LABEL[post.meetup.status]}</StatusBadge>}
            {post.hasFile ? <Icon name="clip" size={15} className="ml-1 text-ink-500" /> : null}
          </div>
          <div className="flex items-center justify-between gap-4">
            <p className="flex min-w-0 items-center gap-[5px] t-body-bold">
              <span className="truncate text-ink-900">{post.title}</span>
              {post.commentCount > 0 && <span className="shrink-0 text-red">[{post.commentCount}]</span>}
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
              {who} · {monthDay(post.createdAt)} · 댓글 {post.commentCount} · 조회 {post.viewCount}
            </p>
          )}
        </div>
        {thumb &&
          (isImageUrl(post.thumbnailUrl) ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img src={post.thumbnailUrl} alt="" className="h-[72px] w-[96px] shrink-0 rounded-[8px] border border-line-200 object-cover" />
          ) : (
            <span className="flex h-[72px] w-[96px] shrink-0 flex-col items-center justify-center gap-1 rounded-[8px] border border-dashed border-line-300 bg-bg-100 text-[12px] text-ink-400">
              <Icon name="image" size={20} /> Image
            </span>
          ))}
      </Link>
    </li>
  );
}
