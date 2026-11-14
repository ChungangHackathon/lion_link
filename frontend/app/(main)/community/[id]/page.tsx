"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { Suspense, use, useState } from "react";
import { cn } from "@/lib/cn";
import { useSession } from "@/lib/session";
import { useStore } from "@/lib/store";
import { ME_ID } from "@/lib/mock/users";
import { LION_PART_LABEL, POST_CATEGORY_LABEL, PROFILE_STATUS_LABEL } from "@/lib/types";
import { daysLeft, formatDateTime, timeAgo } from "@/lib/format";
import { API_ENABLED, errorMessage, isApiError } from "@/lib/api/client";
import type { Id, PostComment, PostDetail, PostMember } from "@/lib/api/posts";
import { useApi } from "@/lib/api/useApi";
import { usePostService } from "@/lib/api/usePostService";
import { Avatar } from "@/components/ui/Avatar";
import { StatusBadge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Skeleton } from "@/components/ui/Misc";
import { Icon } from "@/components/ui/Icon";
import { ConfirmModal, Modal, Popover } from "@/components/ui/Modal";
import { Card, EmptyState, Tooltip } from "@/components/ui/Misc";
import { useToast } from "@/components/ui/Toast";
import { AlumniPicker } from "@/components/domain/AlumniPicker";
import { CoffeeRequestModal } from "@/components/domain/CoffeeRequestModal";
import { MessageRequestModal } from "@/components/domain/MessageRequestModal";
import { authorLabel, isImageUrl } from "@/components/domain/PostRow";

/**
 * 커피챗 모집 참여·번개 초대·메시지는 동문 찾기(RECRUIT·MEETUP-02·채팅)를 연결한 뒤 연다.
 * API 모드에서는 그 버튼들을 잠가 두고, 프로토타입 모드에서는 예전처럼 mock store로 동작한다.
 */
const PENDING_TIP = "곧 연결될 기능이에요";

const formatBytes = (n: number) => (n >= 1024 ** 2 ? `${(n / 1024 ** 2).toFixed(1)}MB` : n >= 1024 ? `${Math.round(n / 1024)}KB` : n > 0 ? `${n}B` : "");
const firstPart = (m: { lionParts: PostMember["lionParts"] } | null | undefined) => m?.lionParts?.[0];

/** ★ B-12 게시글 상세 · ★ K-01 커피챗 모집글 상세 · ◇ L-06 번개 상세 · B-12b 이미지 크게 보기 · B-13 상태 · CF-20 */
function PostDetailInner({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const router = useRouter();
  const toast = useToast();
  const { session } = useSession();
  const service = usePostService();
  const store = useStore();
  const { data: post, error, reload, setData } = useApi(() => service.get(id), [service, id]);
  const others = useApi((signal) => service.list({ category: post ? [post.category] : undefined, size: 4 }, signal), [service, post?.category], !!post).data?.items.filter((p) => String(p.id) !== id).slice(0, 3) ?? [];

  const [more, setMore] = useState(false);
  const [del, setDel] = useState(false);
  const [img, setImg] = useState<string | null>(null);
  const [request, setRequest] = useState(false);
  const [message, setMessage] = useState(false);
  const [invite, setInvite] = useState(false);
  const [liking, setLiking] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [joining, setJoining] = useState(false);

  if (error && !post) {
    if (isApiError(error) && error.status === 404) return <EmptyState title="404 · 삭제되었거나 없는 글이에요" action={{ label: "커뮤니티로", href: "/community" }} />;
    return <EmptyState title="글을 불러오지 못했어요" description={errorMessage(error)} action={{ label: "다시 시도", onClick: reload }} />;
  }
  if (!post) return <Skeleton className="h-[400px] w-full" />;

  const mine = post.isMine;
  const isAdmin = session.role === "ADMIN";
  const author = post.author;
  const images = post.files.filter((f) => f.type === "IMAGE");
  const attachments = post.files.filter((f) => f.type === "ATTACHMENT");
  const kind = post.recruit ? "recruit" : post.meetup ? "meetup" : "post";

  // 프로토타입 모드 전용: mock 동문·커피챗 데이터가 필요한 기능
  const mockPost = API_ENABLED ? undefined : store.posts.find((p) => p.id === id);
  const mockAuthor = mockPost && !mockPost.anonymous ? store.alumni.find((a) => a.id === mockPost.authorId) : undefined;
  const mockTarget = mockPost?.recruit ? store.alumni.find((a) => a.id === mockPost.recruit!.targetId) : undefined;
  const blockedRel = !!mockPost?.meetup && (store.blocked.includes(mockPost.authorId) || mockPost.meetup.memberIds.some((m) => store.blocked.includes(m)));

  const toggleLike = async () => {
    if (liking) return;
    setLiking(true);
    const next = !post.likedByMe;
    setData((p) => (p ? { ...p, likedByMe: next, likeCount: p.likeCount + (next ? 1 : -1) } : p));
    try {
      const res = await service.setLike(post.id, next);
      setData((p) => (p ? { ...p, likedByMe: res.liked, likeCount: res.likeCount } : p));
    } catch (e) {
      setData((p) => (p ? { ...p, likedByMe: !next, likeCount: p.likeCount + (next ? -1 : 1) } : p));
      toast(errorMessage(e), { kind: "error" });
    } finally {
      setLiking(false);
    }
  };

  const setMeetupJoined = async (joined: boolean) => {
    if (joining) return;
    setJoining(true);
    try {
      const res = await service.setMeetupJoined(post.id, joined);
      setData((p) => (p?.meetup ? { ...p, meetup: { ...p.meetup, joinedByMe: res.joined, participantCount: res.participantCount, capacity: res.capacity, status: res.status, members: res.members } } : p));
      toast(joined ? "참여했어요. 단체 채팅방에 초대될 거예요." : "참여를 취소했어요");
    } catch (e) {
      toast(errorMessage(e), { kind: "error" });
      // 정원 마감·시작된 번개처럼 상태가 바뀌었으면 다시 불러온다
      if (isApiError(e) && e.status === 409) reload();
    } finally {
      setJoining(false);
    }
  };

  const remove = async () => {
    setDeleting(true);
    try {
      await service.remove(post.id);
      setDel(false);
      toast(kind === "recruit" ? "모집을 취소했어요" : kind === "meetup" ? "번개를 취소했어요" : "글을 삭제했어요");
      router.push("/community");
    } catch (e) {
      setDel(false);
      toast(errorMessage(e), { kind: "error" });
      reload();
    } finally {
      setDeleting(false);
    }
  };

  return (
    <div className="grid grid-cols-[minmax(0,792px)_384px] justify-between gap-6">
      <div className="flex flex-col gap-4">
        <Card className="rounded-[10px] px-[30px] pb-8 pt-6">
          <Link href={`/community?cat=${post.category}`} className="mb-5 flex w-fit items-center gap-1.5 t-caption1 text-ink-500 hover:underline">
            <Icon name="menu" size={14} /> {POST_CATEGORY_LABEL[post.category]} 목록
          </Link>
          <div className="flex items-start justify-between gap-4">
            <div className="flex flex-col gap-3">
              <div className="flex items-center gap-1.5">
                <span className="rounded-[4px] bg-bg-100 px-2 py-1 t-caption2 text-ink-700">{POST_CATEGORY_LABEL[post.category]}</span>
                {post.deadline && <span className="grid h-[19px] min-w-[39px] place-items-center rounded-[4px] border-[0.5px] border-orange bg-orange-back px-1.5 t-caption2 text-orange-badge">{daysLeft(post.deadline)}</span>}
              </div>
              <h1 className="t-title text-ink-900">{post.title}</h1>
              <div className="flex items-center gap-3">
                {kind !== "post" && <Avatar name={author && !author.isDeleted ? author.name ?? undefined : undefined} part={firstPart(author)} size={40} withdrawn={!!author?.isDeleted} />}
                <p className="text-[13px] text-ink-500">
                  {kind !== "post" ? <span className="block text-[14px] font-semibold text-ink-900">{authorLabel(author)}</span> : null}
                  {new Date(post.createdAt).getMonth() + 1}월 {new Date(post.createdAt).getDate()}일
                  {post.updatedAt && " · 수정됨"} · 조회 {post.viewCount}
                </p>
              </div>
            </div>
            {(mine || isAdmin) && (
              <div className="relative">
                <button type="button" aria-label="더보기" onClick={() => setMore((m) => !m)} className="rounded p-1 hover:bg-bg-100">
                  <Icon name="more" size={22} />
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

          <hr className="my-5 border-line-200" />
          {attachments.length || post.links.length ? (
            <div className="mb-5 flex flex-col gap-2">
              {attachments.map((f) => (
                <div key={f.id} className="flex items-center justify-between rounded-[8px] border border-line-200 px-4 py-3 text-[14px]">
                  <span className="flex items-center gap-2 text-ink-900">
                    <Icon name="clip" size={18} className="text-ink-700" /> {f.name}
                  </span>
                  <span className="flex items-center gap-3 text-[12px] text-ink-500">
                    {formatBytes(f.size)}
                    {f.url ? (
                      <a href={f.url} download={f.name} target="_blank" rel="noreferrer" aria-label={`${f.name} 내려받기`} className="rounded p-1 text-ink-700 hover:bg-bg-100">
                        <Icon name="download" size={18} />
                      </a>
                    ) : (
                      <button type="button" aria-label={`${f.name} 내려받기`} onClick={() => toast("다운로드는 백엔드 연결 후 동작해요")} className="rounded p-1 text-ink-700 hover:bg-bg-100">
                        <Icon name="download" size={18} />
                      </button>
                    )}
                  </span>
                </div>
              ))}
              {post.links.map((l) => (
                <a key={l} href={l} target="_blank" rel="noreferrer" className="flex items-center gap-2 rounded-[8px] border border-line-200 px-4 py-3 text-[14px] text-ink-900 underline hover:bg-bg-50">
                  <Icon name="link" size={18} className="text-ink-700" /> {l}
                </a>
              ))}
            </div>
          ) : null}
          <p className="whitespace-pre-line text-[16px] leading-[26px] text-ink-900">{post.content}</p>

          {images.map((f) => (
            <button key={f.id} type="button" onClick={() => setImg(f.url)} className="mt-6 grid h-[260px] w-full place-items-center overflow-hidden rounded-[8px] border border-line-200 bg-bg-100 text-[13px] text-ink-400 hover:bg-line-200">
              {isImageUrl(f.url) ? (
                // eslint-disable-next-line @next/next/no-img-element
                <img src={f.url} alt={f.name} className="h-full w-full object-contain" />
              ) : (
                <span className="flex flex-col items-center gap-1">
                  <Icon name="image" size={22} /> Image
                </span>
              )}
            </button>
          ))}

          <div className="mt-6 flex items-center gap-3 border-b border-line-200 pb-6">
            <button type="button" aria-pressed={post.likedByMe} onClick={toggleLike} disabled={liking} className={cn("flex items-center gap-1.5 rounded-full border px-3.5 py-1.5 text-[13px] hover:bg-bg-100", post.likedByMe ? "border-orange text-orange" : "border-line-300 text-ink-700")}>
              <Icon name="heart" size={16} /> {post.likeCount}
            </button>
            <span className="flex items-center gap-1.5 text-[13px] text-ink-500">
              <Icon name="comment" size={16} /> {post.commentCount}
            </span>
          </div>

          {/* Comments */}
          <section className="mt-8 flex flex-col gap-4">
            <h2 className="t-body-bold text-ink-900">댓글 {post.commentCount}</h2>
            <Comments postId={post.id} canModerate={isAdmin} onCountChange={(diff) => setData((p) => (p ? { ...p, commentCount: Math.max(0, p.commentCount + diff) } : p))} />
          </section>
        </Card>
      </div>

      {/* Right rail */}
      <aside className="flex flex-col gap-4">
        {post.recruit ? (
          <RecruitCard
            post={post}
            mine={mine}
            ready={!!mockTarget}
            onApply={() => setRequest(true)}
            onJoin={() => {
              store.applyRecruit(id);
              toast("참여했어요. 작성자가 신청하면 함께 들어가요.");
            }}
            onCancel={() => setDel(true)}
          />
        ) : post.meetup ? (
          <MeetupCard
            post={post}
            mine={mine}
            inviteReady={!API_ENABLED}
            busy={joining}
            blocked={blockedRel}
            onJoin={() => setMeetupJoined(true)}
            onLeave={() => setMeetupJoined(false)}
            onInvite={() => setInvite(true)}
            onCancel={() => setDel(true)}
          />
        ) : null}

        {author && !author.isDeleted && kind === "post" && (
          <Card className="flex flex-col gap-3 rounded-[10px] p-5">
            <p className="text-[13px] text-ink-500">작성자</p>
            <div className="flex items-center gap-3">
              <Avatar name={author.name ?? undefined} part={firstPart(author)} size={48} />
              <div>
                <p className="text-[16px] font-bold text-ink-900">{authorLabel(author)}</p>
                <p className="text-[12px] text-ink-500">{[firstPart(author) && LION_PART_LABEL[firstPart(author)!], author.status && PROFILE_STATUS_LABEL[author.status]].filter(Boolean).join(" · ")}</p>
              </div>
            </div>
            {(author.bio || author.headline) && <p className="text-[13px] text-ink-700">{author.bio ?? author.headline}</p>}
            {!mine && (
              <div className="flex gap-2">
                <Button variant="secondary" className="flex-1" href={`/alumni/${author.id}`}>
                  프로필 보기
                </Button>
                {mockAuthor ? (
                  <Button variant="secondary" className="flex-1" onClick={() => setMessage(true)}>
                    메시지
                  </Button>
                ) : (
                  <Tooltip text={PENDING_TIP} className="flex-1">
                    <Button variant="secondary" className="w-full" disabled>
                      메시지
                    </Button>
                  </Tooltip>
                )}
              </div>
            )}
          </Card>
        )}

        <Card className="flex flex-col gap-3 rounded-[10px] p-5">
          <h2 className="t-body-bold text-ink-900">{POST_CATEGORY_LABEL[post.category]}의 다른 글</h2>
          <ul>
            {others.length === 0 && <li className="py-2 text-[13px] text-ink-500">다른 글이 없어요</li>}
            {others.map((p) => (
              <li key={p.id} className="border-b border-line-200 py-3 last:border-b-0">
                <Link href={`/community/${p.id}`} className="flex flex-col gap-1 hover:underline">
                  <span className="truncate text-[14px] font-semibold text-ink-900">{p.title}</span>
                  <span className="text-[12px] text-ink-500">
                    {p.author ? authorLabel(p.author) : "익명"} · 댓글 {p.commentCount}
                  </span>
                </Link>
              </li>
            ))}
          </ul>
        </Card>
      </aside>

      {/* Modals */}
      <ConfirmModal
        open={del}
        onClose={() => !deleting && setDel(false)}
        onConfirm={remove}
        title={kind === "recruit" ? "모집을 취소할까요?" : kind === "meetup" ? "번개를 취소할까요?" : "글을 삭제할까요?"}
        description={kind === "recruit" ? "모집 취소 = 글 삭제예요. 참여자에게 알림이 가요." : kind === "meetup" ? "참여자 전원에게 취소 알림이 가요." : "댓글도 함께 보이지 않게 돼요."}
        confirmLabel={kind === "recruit" ? "모집 취소" : kind === "meetup" ? "번개 취소" : "삭제"}
        danger
      />
      <Modal open={!!img} onClose={() => setImg(null)} width={960}>
        {isImageUrl(img) ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={img} alt="" className="max-h-[70vh] w-full rounded-[8px] object-contain" />
        ) : (
          <div className="grid h-[520px] place-items-center rounded-[8px] bg-bg-100 text-ink-400">
            <span className="flex flex-col items-center gap-2">
              <Icon name="image" size={40} /> 이미지 크게 보기
            </span>
          </div>
        )}
      </Modal>
      {request && mockPost?.recruit && mockTarget && (
        <CoffeeRequestModal
          open
          onClose={() => {
            setRequest(false);
            store.setRecruitStatus(id, "APPLIED");
          }}
          target={mockTarget}
          preMembers={mockPost.recruit.memberIds.filter((m) => m !== ME_ID)}
          fromRecruitId={id}
        />
      )}
      {message && mockAuthor && <MessageRequestModal open onClose={() => setMessage(false)} target={mockAuthor} />}
      {!API_ENABLED && <AlumniPicker open={invite} onClose={() => setInvite(false)} onConfirm={(ids) => toast(`${ids.length}명에게 초대 알림을 보냈어요`)} max={20} title="번개 초대 알림 보내기" confirmLabel="알림 보내기" exclude={mockPost?.meetup?.memberIds} />}
    </div>
  );
}

const COMMENT_PAGE = 20;

/** 댓글 (COMMENT-01 ~ 03). 오래된 순이고 [댓글 더보기]로 다음 페이지를 붙인다 */
function Comments({ postId, canModerate, onCountChange }: { postId: Id; canModerate: boolean; onCountChange: (diff: number) => void }) {
  const toast = useToast();
  const service = usePostService();
  const first = useApi(() => service.comments(postId, 0, COMMENT_PAGE), [service, postId]);
  const [extra, setExtra] = useState<{ items: PostComment[]; page: number; hasNext: boolean } | null>(null);
  const [added, setAdded] = useState<PostComment[]>([]);
  const [removed, setRemoved] = useState<Set<string>>(new Set());
  const [text, setText] = useState("");
  const [posting, setPosting] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [target, setTarget] = useState<PostComment | null>(null);

  // 프로토타입 모드에서는 store가 바뀌면 첫 페이지를 다시 불러오므로, 그때 붙여 둔 값은 비운다
  const [seen, setSeen] = useState(first.data);
  if (seen !== first.data) {
    setSeen(first.data);
    setExtra(null);
    setAdded([]);
    setRemoved(new Set());
  }

  const loaded = [...(first.data?.items ?? []), ...(extra?.items ?? [])];
  const ids = new Set(loaded.map((c) => String(c.id)));
  const comments = [...loaded, ...added.filter((c) => !ids.has(String(c.id)))].map((c) => (removed.has(String(c.id)) ? { ...c, deleted: true, author: null, content: "삭제된 댓글이에요." } : c));
  const hasNext = extra ? extra.hasNext : !!first.data?.hasNext;

  const loadMore = async () => {
    setLoadingMore(true);
    try {
      const page = (extra?.page ?? 0) + 1;
      const res = await service.comments(postId, page, COMMENT_PAGE);
      setExtra((prev) => ({ items: [...(prev?.items ?? []), ...res.items], page, hasNext: res.hasNext }));
    } catch (e) {
      toast(errorMessage(e), { kind: "error" });
    } finally {
      setLoadingMore(false);
    }
  };

  const submit = async () => {
    const content = text.trim();
    if (!content || content.length > 500 || posting) return;
    setPosting(true);
    try {
      const created = await service.addComment(postId, content);
      setAdded((a) => [...a, created]);
      setText("");
      onCountChange(1);
    } catch (e) {
      toast(errorMessage(e), { kind: "error" });
    } finally {
      setPosting(false);
    }
  };

  const remove = async () => {
    if (!target) return;
    const c = target;
    setTarget(null);
    try {
      await service.removeComment(postId, c.id);
      setRemoved((r) => new Set(r).add(String(c.id)));
      onCountChange(-1);
      toast("댓글을 삭제했어요");
    } catch (e) {
      toast(errorMessage(e), { kind: "error" });
    }
  };

  if (first.error && !first.data) {
    return <EmptyState compact title="댓글을 불러오지 못했어요" description={errorMessage(first.error)} action={{ label: "다시 시도", onClick: first.reload }} />;
  }

  return (
    <>
      {!first.data ? (
        <Skeleton className="h-[80px] w-full" />
      ) : comments.length === 0 ? (
        <p className="py-4 text-[14px] text-ink-500">첫 댓글을 남겨보세요</p>
      ) : (
        <ul>
          {comments.map((c) => (
            <li key={c.id} className="flex gap-3 border-b border-line-200 py-4 last:border-b-0">
              <Avatar name={c.author && !c.author.isDeleted ? c.author.name ?? undefined : undefined} part={firstPart(c.author)} size={32} withdrawn={!c.deleted && !c.isAnonymous && (!c.author || c.author.isDeleted)} />
              <div className="flex flex-col gap-1">
                <p className="text-[13px]">
                  <span className="font-bold text-ink-900">{c.deleted ? "" : c.isAnonymous ? "익명의 사자" : c.author && !c.author.isDeleted ? c.author.name : "알 수 없음"}</span>
                  <span className={cn("text-ink-500", !c.deleted && "ml-1.5")}>
                    {!c.deleted && !c.isAnonymous && c.author && !c.author.isDeleted && `${c.author.generation}기 · `}
                    {timeAgo(c.createdAt)}
                  </span>
                  {!c.deleted && (c.isMine || canModerate) && (
                    <button type="button" onClick={() => setTarget(c)} className="ml-2 text-ink-500 underline">
                      삭제{!c.isMine && " (관리자)"}
                    </button>
                  )}
                </p>
                <p className={cn("whitespace-pre-line text-[14px]", c.deleted ? "text-ink-400" : "text-ink-900")}>{c.content}</p>
              </div>
            </li>
          ))}
        </ul>
      )}
      {hasNext && (
        <Button variant="secondary" size="S" className="self-center" loading={loadingMore} onClick={loadMore}>
          댓글 더보기
        </Button>
      )}
      <form onSubmit={(e) => { e.preventDefault(); void submit(); }} className="flex items-center gap-2">
        <input aria-label="댓글" value={text} onChange={(e) => setText(e.target.value)} placeholder="댓글을 입력하세요 (1~500자)" maxLength={500} className="h-10 flex-1 rounded-[6px] border border-line-300 px-3 text-[14px] outline-none placeholder:text-ink-400 focus:border-ink-700" />
        <Button type="submit" size="S" className="h-10" disabled={!text.trim()} loading={posting}>
          등록
        </Button>
      </form>
      <ConfirmModal open={!!target} onClose={() => setTarget(null)} onConfirm={remove} title="댓글을 삭제할까요?" description="삭제한 댓글은 '삭제된 댓글이에요'로 남아요." confirmLabel="삭제" danger />
    </>
  );
}

function MemberList({ members }: { members: PostMember[] }) {
  return (
    <ul className="flex flex-col gap-2">
      {members.map((m, i) => (
        <li key={m.id ?? `deleted-${i}`} className="flex items-center gap-2 text-[14px]">
          <Avatar name={m.isDeleted ? undefined : m.name ?? undefined} part={firstPart(m)} size={32} withdrawn={m.isDeleted} />
          <span className="font-semibold text-ink-900">{m.isDeleted ? "알 수 없음" : m.name}</span>
          {!m.isDeleted && <span className="text-[12px] text-ink-500">{m.generation}기</span>}
          {m.isHost && <span className="rounded-[4px] border border-line-300 px-1.5 text-[11px] text-ink-500">작성자</span>}
        </li>
      ))}
    </ul>
  );
}

function Locked({ children }: { children: string }) {
  return (
    <Tooltip text={PENDING_TIP} className="w-full">
      <span className="flex h-12 w-full items-center justify-center rounded-[8px] bg-line-200 text-[15px] font-bold text-ink-400">{children}</span>
    </Tooltip>
  );
}

/** K-01 · K-03 RecruitCard 상태표 */
function RecruitCard({ post, mine, ready, onApply, onJoin, onCancel }: { post: PostDetail; mine: boolean; ready: boolean; onApply: () => void; onJoin: () => void; onCancel: () => void }) {
  const r = post.recruit!;
  const full = r.memberCount >= r.capacity;
  const label = r.status === "APPLIED" ? "신청 완료" : r.status === "ENDED" ? "모집 종료" : full ? "정원 마감" : "모집 중";
  const seniorName = r.senior.isDeleted || !r.senior.name ? "알 수 없음" : `${r.senior.name}(${r.senior.generation}기)`;
  return (
    <Card className="flex flex-col gap-3 p-6">
      <div className="flex items-center justify-between">
        <StatusBadge tone={r.status === "APPLIED" ? "filled" : r.status === "OPEN" && !full ? "outline" : "gray"}>{label}</StatusBadge>
        <span className="text-[13px] text-ink-500">마감 {daysLeft(r.deadline)}</span>
      </div>
      <p className="text-[17px] font-bold text-ink-900">
        {seniorName} 사자 · 참여 {r.memberCount}/{r.capacity}명
      </p>
      <p className="text-[13px] text-ink-500">참여자</p>
      <MemberList members={r.members} />
      {r.isSenior ? null : mine ? (
        <>
          {r.status === "OPEN" ? (
            !ready ? (
              <Locked>이 인원으로 커피챗 신청하기</Locked>
            ) : r.senior.acceptGroup ? (
              <Button size="L" full disabled={r.memberCount < 2} onClick={onApply}>
                이 인원으로 커피챗 신청하기
              </Button>
            ) : (
              <Tooltip text="이 사자는 지금 1:1 커피챗만 받아요">
                <span className="flex h-12 w-full items-center justify-center rounded-[8px] bg-line-200 text-[15px] font-bold text-ink-400">이 인원으로 커피챗 신청하기</span>
              </Tooltip>
            )
          ) : (
            <p className="rounded-[8px] bg-bg-50 px-4 py-3 text-center text-[13px] text-ink-500">{r.status === "APPLIED" ? "커피챗을 신청했어요. 거절·만료·취소돼도 글은 '신청 완료'로 남아요." : "모집이 종료됐어요"}</p>
          )}
          {r.status === "OPEN" && <p className="text-center text-[12px] text-ink-500">참여자가 작성자 외 1명 이상일 때 신청할 수 있어요</p>}
          {r.status !== "APPLIED" && (
            <Button variant="secondary" size="L" full onClick={onCancel}>
              모집 취소
            </Button>
          )}
          <p className="text-center text-[12px] text-ink-400">모집 취소 = 글 삭제</p>
        </>
      ) : r.joinedByMe ? (
        <p className="rounded-[8px] bg-bg-50 px-4 py-3 text-center text-[13px] text-ink-700">{r.status === "APPLIED" ? "참여가 닫혔어요. 빠지기는 커피챗 상세에서 할 수 있어요." : "참여 중이에요. 작성자가 신청하면 함께 들어가요."}</p>
      ) : r.hasOpenCoffeeChatWithSenior ? (
        <Tooltip text="이미 이 사자와 진행 중인 커피챗이 있어요">
          <span className="flex h-12 w-full items-center justify-center rounded-[8px] bg-line-200 text-[15px] font-bold text-ink-400">참여하기</span>
        </Tooltip>
      ) : full || r.status !== "OPEN" ? (
        <Button size="L" full disabled>
          {full ? "정원 마감" : label}
        </Button>
      ) : !ready ? (
        <Locked>참여하기</Locked>
      ) : (
        <Button size="L" full onClick={onJoin}>
          참여하기
        </Button>
      )}
    </Card>
  );
}

/** L-06 번개 상세 카드 · K-03 번개 states */
function MeetupCard({ post, mine, inviteReady, busy, blocked, onJoin, onLeave, onInvite, onCancel }: { post: PostDetail; mine: boolean; inviteReady: boolean; busy: boolean; blocked: boolean; onJoin: () => void; onLeave: () => void; onInvite: () => void; onCancel: () => void }) {
  const m = post.meetup!;
  const past = m.status === "ENDED";
  const canceled = m.status === "CANCELED";
  const full = m.status === "FULL";
  return (
    <Card className="flex flex-col gap-3 p-6">
      <div className="flex items-center justify-between">
        <StatusBadge tone={m.status === "OPEN" ? "outline" : "gray"}>{canceled ? "취소됨" : past ? "종료" : full ? "모집 마감" : "모집 중"}</StatusBadge>
        <span className="text-[13px] text-ink-500">정원 {m.capacity}명</span>
      </div>
      <p className="text-[17px] font-bold text-ink-900">
        참여 {m.participantCount}/{m.capacity}명 · {formatDateTime(m.meetAt)}
      </p>
      <p className="flex items-center gap-1 text-[13px] text-ink-700">
        <Icon name="pin" size={14} /> {m.place}
      </p>
      <p className="text-[13px] text-ink-500">참여자</p>
      <MemberList members={m.members} />
      {mine ? (
        <>
          {inviteReady ? (
            <Button size="L" full variant="secondary" onClick={onInvite} disabled={past || canceled}>
              초대 알림 보내기
            </Button>
          ) : (
            <Locked>초대 알림 보내기</Locked>
          )}
          <Button size="L" full variant="danger" onClick={onCancel} disabled={past || canceled}>
            번개 취소
          </Button>
        </>
      ) : past || canceled ? (
        <Button size="L" full disabled>
          {canceled ? "취소된 번개" : "종료된 번개"}
        </Button>
      ) : blocked ? (
        <Tooltip text="참여할 수 없는 번개예요">
          <span className="flex h-12 w-full items-center justify-center rounded-[8px] bg-line-200 text-[15px] font-bold text-ink-400">참여하기</span>
        </Tooltip>
      ) : m.joinedByMe ? (
        <Button size="L" full variant="secondary" loading={busy} onClick={onLeave}>
          참여 취소
        </Button>
      ) : (
        <Button size="L" full disabled={full} loading={busy} onClick={onJoin}>
          {full ? "모집 마감" : "참여하기"}
        </Button>
      )}
    </Card>
  );
}

/** Cache Components: params must be read inside Suspense. */
export default function PostDetailPage({ params }: { params: Promise<{ id: string }> }) {
  return (
    <Suspense fallback={<Skeleton className="h-[400px] w-full" />}>
      <PostDetailInner params={params} />
    </Suspense>
  );
}
