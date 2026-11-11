"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { Suspense, use, useState } from "react";
import { cn } from "@/lib/cn";
import { useSession } from "@/lib/session";
import { useStore } from "@/lib/store";
import { ME_ID } from "@/lib/mock/users";
import { LION_PART_LABEL, POST_CATEGORY_LABEL, PROFILE_STATUS_LABEL } from "@/lib/types";
import { daysLeft, formatDate, formatDateTime, timeAgo } from "@/lib/format";
import { Avatar } from "@/components/ui/Avatar";
import { StatusBadge, Tag } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Skeleton } from "@/components/ui/Misc";
import { Glyph } from "@/components/ui/Icon";
import { ConfirmModal, Modal, Popover } from "@/components/ui/Modal";
import { Card, EmptyState, Tooltip } from "@/components/ui/Misc";
import { useToast } from "@/components/ui/Toast";
import { AlumniPicker } from "@/components/domain/AlumniPicker";
import { CoffeeRequestModal } from "@/components/domain/CoffeeRequestModal";
import { MessageRequestModal } from "@/components/domain/MessageRequestModal";

/** ★ B-12 게시글 상세 · ★ K-01 커피챗 모집글 상세 · ◇ L-06 번개 상세 · B-12b 이미지 크게 보기 · B-13 상태 · CF-20 */
function PostDetailInner({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const router = useRouter();
  const toast = useToast();
  const { session } = useSession();
  const { posts, alumni, coffeeChats, blocked, addComment, likePost, deletePost, joinMeetup, leaveMeetup, applyRecruit, setRecruitStatus } = useStore();
  const post = posts.find((p) => p.id === id);
  const [comment, setComment] = useState("");
  const [more, setMore] = useState(false);
  const [del, setDel] = useState(false);
  const [img, setImg] = useState<string | null>(null);
  const [request, setRequest] = useState(false);
  const [message, setMessage] = useState(false);
  const [invite, setInvite] = useState(false);

  if (!post) return <EmptyState title="404 · 삭제되었거나 없는 글이에요" action={{ label: "커뮤니티로", href: "/community" }} />;
  const author = alumni.find((a) => a.id === post.authorId);
  const mine = post.authorId === ME_ID;
  const isAdmin = session.role === "ADMIN";
  const target = post.recruit ? alumni.find((a) => a.id === post.recruit!.targetId) : undefined;
  const others = posts.filter((p) => p.id !== post.id && p.category === post.category).slice(0, 3);

  const submitComment = () => {
    const t = comment.trim();
    if (!t || t.length > 500) return;
    addComment(post.id, t, post.anonymous && mine);
    setComment("");
  };

  return (
    <div className="grid grid-cols-[1fr_384px] gap-6">
      <div className="flex flex-col gap-4">
        <Link href={`/community?cat=${post.category}`} className="text-[13px] text-ink-500 hover:underline">
          ← {POST_CATEGORY_LABEL[post.category]} 목록
        </Link>
        <Card className="p-8">
          <div className="flex items-start justify-between gap-4">
            <div className="flex flex-col gap-3">
              <div className="flex items-center gap-1.5">
                <Tag className="font-semibold">{POST_CATEGORY_LABEL[post.category]}</Tag>
                {post.deadline && <span className="rounded-[4px] border-[0.5px] border-orange bg-orange-back px-2 py-0.5 text-[12px] font-semibold">{daysLeft(post.deadline)}</span>}
              </div>
              <h1 className="text-[28px] font-bold leading-[36px] text-ink-900">{post.title}</h1>
              <div className="flex items-center gap-3">
                {(post.recruit || post.meetup) && <Avatar name={post.anonymous ? undefined : author?.name} part={author?.part} size={40} />}
                <p className="text-[13px] text-ink-500">
                  {post.recruit || post.meetup ? (
                    <span className="block text-[14px] font-semibold text-ink-900">
                      {author?.name} · {author?.generation}기
                    </span>
                  ) : null}
                  {formatDate(post.createdAt)}
                  {post.edited && " · 수정됨"} · 조회 {post.views}
                </p>
              </div>
            </div>
            {(mine || isAdmin) && (
              <div className="relative">
                <button type="button" aria-label="더보기" onClick={() => setMore((m) => !m)} className="rounded p-1 hover:bg-bg-100">
                  <Glyph name="more" size={22} />
                </button>
                <Popover open={more} onClose={() => setMore(false)} className="w-[140px] p-2">
                  {mine && (
                    <Link href={`/community/new?edit=${post.id}`} className="block rounded-[6px] px-3 py-2 text-[14px] hover:bg-bg-100">
                      수정
                    </Link>
                  )}
                  <button type="button" onClick={() => { setMore(false); setDel(true); }} className="block w-full rounded-[6px] px-3 py-2 text-left text-[14px] text-orange hover:bg-orange-back">
                    삭제{isAdmin && !mine && " (관리자)"}
                  </button>
                </Popover>
              </div>
            )}
          </div>

          <hr className="my-6 border-line-200" />
          <p className="whitespace-pre-line text-[16px] leading-7 text-ink-900">{post.body}</p>

          {post.images?.map((src, i) => (
            <button key={i} type="button" onClick={() => setImg(src)} className="mt-6 grid h-[260px] w-full place-items-center rounded-[8px] border border-line-200 bg-bg-100 text-[13px] text-ink-400 hover:bg-line-200">
              <span className="flex flex-col items-center gap-1">
                <Glyph name="image" size={22} /> Image
              </span>
            </button>
          ))}

          {(post.files?.length || post.links?.length) ? (
            <div className="mt-6 flex flex-col gap-2">
              <p className="text-[13px] text-ink-500">첨부파일 · 링크</p>
              {post.files?.map((f) => (
                <div key={f.name} className="flex items-center justify-between rounded-[8px] border border-line-200 px-4 py-3 text-[14px]">
                  <span className="flex items-center gap-2 text-ink-900">
                    <Glyph name="link" size={16} className="text-ink-400" /> {f.name}
                  </span>
                  <span className="flex items-center gap-3 text-[12px] text-ink-500">
                    {f.size} <Glyph name="chevron-down" size={16} />
                  </span>
                </div>
              ))}
              {post.links?.map((l) => (
                <a key={l} href={l} target="_blank" rel="noreferrer" className="flex items-center gap-2 rounded-[8px] border border-line-200 px-4 py-3 text-[14px] text-ink-900 underline hover:bg-bg-50">
                  <Glyph name="link" size={16} className="text-ink-400" /> {l}
                </a>
              ))}
            </div>
          ) : null}

          <div className="mt-6 flex items-center gap-3">
            <button type="button" onClick={() => likePost(post.id)} className="flex items-center gap-1.5 rounded-full border border-line-300 px-3.5 py-1.5 text-[13px] text-ink-700 hover:bg-bg-100">
              <Glyph name="heart" size={16} /> {post.likes}
            </button>
            <span className="flex items-center gap-1.5 text-[13px] text-ink-500">
              <Glyph name="comment" size={16} /> {post.comments.length}
            </span>
          </div>

          {/* Comments */}
          <section className="mt-8 flex flex-col gap-4">
            <h2 className="text-[16px] font-bold text-ink-900">댓글 {post.comments.length}</h2>
            {post.comments.length === 0 && <p className="py-4 text-[14px] text-ink-500">첫 댓글을 남겨보세요</p>}
            <ul>
              {post.comments.map((c) => {
                const ca = alumni.find((a) => a.id === c.authorId);
                const deleted = c.text === "삭제된 댓글이에요";
                const anon = c.anonymous || (post.anonymous && c.authorId === post.authorId);
                return (
                  <li key={c.id} className="flex gap-3 border-b border-line-200 py-4 last:border-b-0">
                    <Avatar name={anon ? undefined : ca?.name} part={ca?.part} size={32} withdrawn={!ca} />
                    <div className="flex flex-col gap-1">
                      <p className="text-[13px]">
                        <span className="font-bold text-ink-900">{anon ? "익명" : ca?.name ?? "탈퇴한 사자"}</span>
                        <span className="ml-1.5 text-ink-500">
                          {!anon && ca && `${ca.generation}기 · `}
                          {timeAgo(c.at)}
                        </span>
                        {c.authorId === ME_ID && !deleted && <button type="button" className="ml-2 text-ink-500 underline">삭제</button>}
                      </p>
                      <p className={cn("text-[14px]", deleted ? "text-ink-400" : "text-ink-900")}>{c.text}</p>
                    </div>
                  </li>
                );
              })}
            </ul>
            <form onSubmit={(e) => { e.preventDefault(); submitComment(); }} className="flex gap-2">
              <input value={comment} onChange={(e) => setComment(e.target.value)} placeholder="댓글을 입력하세요 (1~500자)" className="h-10 flex-1 rounded-[6px] border border-line-300 px-3 text-[14px] outline-none placeholder:text-ink-400 focus:border-ink-800" maxLength={500} />
              <Button type="submit" disabled={!comment.trim()}>
                등록
              </Button>
            </form>
          </section>
        </Card>
      </div>

      {/* Right rail */}
      <aside className="flex flex-col gap-4">
        {post.recruit && target ? (
          <RecruitCard post={post} target={target} mine={mine} onApply={() => setRequest(true)} onJoin={() => { applyRecruit(post.id); toast("참여했어요. 작성자가 신청하면 함께 들어가요."); }} onCancel={() => setDel(true)} />
        ) : post.meetup ? (
          <MeetupCard post={post} mine={mine} onJoin={() => { joinMeetup(post.id); toast("참여했어요. 단체 채팅방에 초대될 거예요."); }} onLeave={() => { leaveMeetup(post.id); toast("참여를 취소했어요"); }} onInvite={() => setInvite(true)} onCancel={() => setDel(true)} />
        ) : null}

        {!post.anonymous && author && !post.recruit && !post.meetup && (
          <Card className="flex flex-col gap-3 p-5">
            <p className="text-[13px] text-ink-500">작성자</p>
            <div className="flex items-center gap-3">
              <Avatar name={author.name} part={author.part} size={48} />
              <div>
                <p className="text-[16px] font-bold text-ink-900">
                  {author.name} · {author.generation}기
                </p>
                <p className="text-[12px] text-ink-500">
                  {LION_PART_LABEL[author.part]} · {PROFILE_STATUS_LABEL[author.status]}
                </p>
              </div>
            </div>
            <p className="text-[13px] text-ink-700">{author.intro}</p>
            {!mine && (
              <div className="flex gap-2">
                <Button variant="secondary" className="flex-1" href={`/alumni/${author.id}`}>
                  프로필 보기
                </Button>
                <Button variant="secondary" className="flex-1" onClick={() => setMessage(true)}>
                  메시지
                </Button>
              </div>
            )}
          </Card>
        )}

        <Card className="flex flex-col gap-3 p-5">
          <h2 className="text-[16px] font-bold text-ink-900">{POST_CATEGORY_LABEL[post.category]}의 다른 글</h2>
          <ul>
            {others.length === 0 && <li className="py-2 text-[13px] text-ink-500">다른 글이 없어요</li>}
            {others.map((p) => {
              const a = alumni.find((x) => x.id === p.authorId);
              return (
                <li key={p.id} className="border-b border-line-200 py-3 last:border-b-0">
                  <Link href={`/community/${p.id}`} className="flex flex-col gap-1 hover:underline">
                    <span className="truncate text-[14px] font-semibold text-ink-900">{p.title}</span>
                    <span className="text-[12px] text-ink-500">
                      {p.anonymous ? "익명" : `${a?.name} · ${a?.generation}기`} · 댓글 {p.comments.length}
                    </span>
                  </Link>
                </li>
              );
            })}
          </ul>
        </Card>
      </aside>

      {/* Modals */}
      <ConfirmModal open={del} onClose={() => setDel(false)} onConfirm={() => { deletePost(post.id); setDel(false); toast(post.recruit ? "모집을 취소했어요" : post.meetup ? "번개를 취소했어요" : "글을 삭제했어요"); router.push("/community"); }} title={post.recruit ? "모집을 취소할까요?" : post.meetup ? "번개를 취소할까요?" : "글을 삭제할까요?"} description={post.recruit ? "모집 취소 = 글 삭제예요. 참여자에게 알림이 가요." : post.meetup ? "참여자 전원에게 취소 알림이 가요." : "댓글도 함께 보이지 않게 돼요."} confirmLabel={post.recruit ? "모집 취소" : post.meetup ? "번개 취소" : "삭제"} danger />
      <Modal open={!!img} onClose={() => setImg(null)} width={960}>
        <div className="grid h-[520px] place-items-center rounded-[8px] bg-bg-100 text-ink-400">
          <span className="flex flex-col items-center gap-2">
            <Glyph name="image" size={40} /> 이미지 크게 보기
          </span>
        </div>
      </Modal>
      {request && target && <CoffeeRequestModal open onClose={() => { setRequest(false); setRecruitStatus(post.id, "APPLIED"); }} target={target} preMembers={post.recruit!.memberIds.filter((m) => m !== ME_ID)} fromRecruitId={post.id} />}
      {message && author && <MessageRequestModal open onClose={() => setMessage(false)} target={author} />}
      <AlumniPicker open={invite} onClose={() => setInvite(false)} onConfirm={(ids) => toast(`${ids.length}명에게 초대 알림을 보냈어요`)} max={20} title="번개 초대 알림 보내기" confirmLabel="알림 보내기" exclude={post.meetup?.memberIds} />
      <span className="hidden">{coffeeChats.length + blocked.length}</span>
    </div>
  );
}

/** K-01 · K-03 RecruitCard 상태표 */
function RecruitCard({ post, target, mine, onApply, onJoin, onCancel }: { post: NonNullable<ReturnType<typeof usePost>>; target: { id: string; name: string; generation: number; allowGroup: boolean; receive: string; part: "PLANNING" | "DESIGN" | "FRONTEND" | "BACKEND" | "ETC" }; mine: boolean; onApply: () => void; onJoin: () => void; onCancel: () => void }) {
  const { alumni, coffeeChats } = useStore();
  const r = post.recruit!;
  const joined = r.memberIds.includes(ME_ID);
  const full = r.memberIds.length >= r.max;
  const hasActive = coffeeChats.some((c) => c.receiverId === target.id && c.requesterId === ME_ID && ["PENDING", "ACCEPTED", "SCHEDULED"].includes(c.status));
  const isTarget = target.id === ME_ID;
  const label = r.status === "APPLIED" ? "신청 완료" : r.status === "ENDED" ? "모집 종료" : full ? "정원 마감" : "모집 중";
  return (
    <Card className="flex flex-col gap-3 p-6">
      <div className="flex items-center justify-between">
        <StatusBadge tone={r.status === "APPLIED" ? "filled" : r.status === "OPEN" && !full ? "outline" : "gray"}>{label}</StatusBadge>
        <span className="text-[13px] text-ink-500">마감 {daysLeft(r.deadline)}</span>
      </div>
      <p className="text-[17px] font-bold text-ink-900">
        {target.name}({target.generation}기) 선배 · 참여 {r.memberIds.length}/{r.max}명
      </p>
      <p className="text-[13px] text-ink-500">참여자</p>
      <ul className="flex flex-col gap-2">
        {r.memberIds.map((id) => {
          const a = alumni.find((x) => x.id === id);
          return (
            <li key={id} className="flex items-center gap-2 text-[14px]">
              <Avatar name={a?.name} part={a?.part} size={32} />
              <span className="font-semibold text-ink-900">{a?.name}</span>
              <span className="text-[12px] text-ink-500">{a?.generation}기</span>
              {id === post.authorId && <span className="rounded-[4px] border border-line-300 px-1.5 text-[11px] text-ink-500">작성자</span>}
            </li>
          );
        })}
      </ul>
      {isTarget ? null : mine ? (
        <>
          {r.status === "OPEN" ? (
            target.allowGroup ? (
              <Button size="L" full disabled={r.memberIds.length < 2} onClick={onApply}>
                이 인원으로 커피챗 신청하기
              </Button>
            ) : (
              <Tooltip text="이 선배는 지금 1:1 커피챗만 받아요">
                <span className="flex h-12 w-full items-center justify-center rounded-[8px] bg-line-200 text-[15px] font-bold text-ink-400">이 인원으로 커피챗 신청하기</span>
              </Tooltip>
            )
          ) : (
            <p className="rounded-[8px] bg-bg-50 px-4 py-3 text-center text-[13px] text-ink-500">{r.status === "APPLIED" ? "커피챗을 신청했어요. 거절·만료·취소돼도 글은 '신청 완료'로 남아요." : "모집이 종료됐어요"}</p>
          )}
          {r.status === "OPEN" && <p className="text-center text-[12px] text-ink-500">참여자가 작성자 외 1명 이상일 때 신청할 수 있어요</p>}
          <Button variant="secondary" size="L" full onClick={onCancel}>
            모집 취소
          </Button>
          <p className="text-center text-[12px] text-ink-400">모집 취소 = 글 삭제</p>
        </>
      ) : joined ? (
        <p className="rounded-[8px] bg-bg-50 px-4 py-3 text-center text-[13px] text-ink-700">{r.status === "APPLIED" ? "참여가 닫혔어요. 빠지기는 커피챗 상세에서 할 수 있어요." : "참여 중이에요. 작성자가 신청하면 함께 들어가요."}</p>
      ) : hasActive ? (
        <Tooltip text="이미 이 선배와 진행 중인 커피챗이 있어요">
          <span className="flex h-12 w-full items-center justify-center rounded-[8px] bg-line-200 text-[15px] font-bold text-ink-400">참여하기</span>
        </Tooltip>
      ) : (
        <Button size="L" full disabled={full || r.status !== "OPEN"} onClick={onJoin}>
          {full ? "정원 마감" : r.status !== "OPEN" ? label : "참여하기"}
        </Button>
      )}
    </Card>
  );
}

/** L-06 번개 상세 카드 · K-03 번개 states */
function MeetupCard({ post, mine, onJoin, onLeave, onInvite, onCancel }: { post: NonNullable<ReturnType<typeof usePost>>; mine: boolean; onJoin: () => void; onLeave: () => void; onInvite: () => void; onCancel: () => void }) {
  const { alumni, blocked } = useStore();
  const m = post.meetup!;
  const joined = m.memberIds.includes(ME_ID);
  const full = m.memberIds.length >= m.max;
  const past = new Date(m.at).getTime() < Date.now();
  const blockedRel = blocked.includes(post.authorId) || m.memberIds.some((id) => blocked.includes(id));
  return (
    <Card className="flex flex-col gap-3 p-6">
      <div className="flex items-center justify-between">
        <StatusBadge tone={past ? "gray" : full ? "gray" : "outline"}>{past ? "종료" : full ? "모집 마감" : "모집 중"}</StatusBadge>
        <span className="text-[13px] text-ink-500">정원 {m.max}명</span>
      </div>
      <p className="text-[17px] font-bold text-ink-900">
        참여 {m.memberIds.length}/{m.max}명 · {formatDateTime(m.at)}
      </p>
      <p className="flex items-center gap-1 text-[13px] text-ink-700">
        <Glyph name="pin" size={14} /> {m.place}
      </p>
      <p className="text-[13px] text-ink-500">참여자</p>
      <ul className="flex flex-col gap-2">
        {m.memberIds.map((id) => {
          const a = alumni.find((x) => x.id === id);
          return (
            <li key={id} className="flex items-center gap-2 text-[14px]">
              <Avatar name={a?.name} part={a?.part} size={32} />
              <span className="font-semibold text-ink-900">{a?.name}</span>
              <span className="text-[12px] text-ink-500">{a?.generation}기</span>
            </li>
          );
        })}
      </ul>
      {mine ? (
        <>
          <Button size="L" full variant="secondary" onClick={onInvite} disabled={past}>
            초대 알림 보내기
          </Button>
          <Button size="L" full variant="danger" onClick={onCancel} disabled={past}>
            번개 취소
          </Button>
        </>
      ) : past ? (
        <Button size="L" full disabled>
          종료된 번개
        </Button>
      ) : blockedRel ? (
        <Tooltip text="참여할 수 없는 번개예요">
          <span className="flex h-12 w-full items-center justify-center rounded-[8px] bg-line-200 text-[15px] font-bold text-ink-400">참여하기</span>
        </Tooltip>
      ) : joined ? (
        <Button size="L" full variant="secondary" onClick={onLeave}>
          참여 취소
        </Button>
      ) : (
        <Button size="L" full disabled={full} onClick={onJoin}>
          {full ? "모집 마감" : "참여하기"}
        </Button>
      )}
    </Card>
  );
}

// type helper for card props
type PostLike = ReturnType<typeof useStore>["posts"][number];
// eslint-disable-next-line @typescript-eslint/no-unused-vars
function usePost(): PostLike | undefined {
  return undefined;
}

/** Cache Components: params must be read inside Suspense. */
export default function PostDetailPage({ params }: { params: Promise<{ id: string }> }) {
  return (
    <Suspense fallback={<Skeleton className="h-[400px] w-full" />}>
      <PostDetailInner params={params} />
    </Suspense>
  );
}
