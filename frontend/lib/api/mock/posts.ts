/**
 * 프로토타입 모드(NEXT_PUBLIC_API_URL 없음)에서 커뮤니티 화면이 쓰는 가짜 API.
 * lib/store.tsx의 mock 게시글을 실제 API 응답 모양(lib/api/posts.ts)으로 바꿔 돌려준다.
 * 화면은 API 모드와 똑같은 코드로 그려진다.
 */

import type { Alumni, CoffeeChat, Post } from "@/lib/types";
import { ME_ID } from "@/lib/mock/users";
import type { Page, Slice } from "../client";
import { ApiError } from "../client";
import type { ApiLionPart } from "../profile";
import type { CategoryCounts, Id, LikeResult, MeetupParticipation, MeetupStatus, PostAuthor, PostComment, PostDetail, PostFile, PostListQuery, PostMember, PostSummary, PostUpdateRequest, PostWriteRequest } from "../posts";

export interface MockPostState {
  posts: Post[];
  alumni: Alumni[];
  blocked: string[];
  coffeeChats: CoffeeChat[];
}

export interface MockPostActions {
  createPost: (input: Omit<Post, "id" | "views" | "likes" | "comments" | "createdAt" | "authorId">) => string;
  updatePost: (id: string, patch: Partial<Post>) => void;
  deletePost: (id: string) => void;
}

const DELETED_COMMENT = "삭제된 댓글이에요.";

const liked = new Set<string>();
const delay = <T,>(value: T) => new Promise<T>((resolve) => setTimeout(() => resolve(value), 120));
const notFound = () => new ApiError(404, "POST_NOT_FOUND", "삭제되었거나 없는 글이에요.");

const parts = (a: Alumni): ApiLionPart[] => (a.parts ?? [a.part]).filter((p): p is ApiLionPart => p !== "ETC");

function author(state: MockPostState, post: Post): PostAuthor | null {
  if (post.anonymous) return null;
  const a = state.alumni.find((x) => x.id === post.authorId);
  if (!a) return { id: null, name: null, generation: null, lionParts: null, photoUrl: null, headline: null, isDeleted: true };
  return { id: a.id, name: a.name, generation: a.generation, lionParts: parts(a), photoUrl: null, headline: a.headline, isDeleted: false };
}

function member(state: MockPostState, id: string, hostId: string): PostMember {
  const a = state.alumni.find((x) => x.id === id);
  if (!a) return { id: null, name: null, generation: null, lionParts: null, photoUrl: null, isHost: id === hostId, isDeleted: true };
  return { id: a.id, name: a.name, generation: a.generation, lionParts: parts(a), photoUrl: null, isHost: id === hostId, isDeleted: false };
}

function meetupStatus(m: NonNullable<Post["meetup"]>): MeetupStatus {
  if (new Date(m.at).getTime() < Date.now()) return "ENDED";
  return m.memberIds.length >= m.max ? "FULL" : "OPEN";
}

/** "2.1MB" → bytes */
function parseSize(size: string): number {
  const m = /([\d.]+)\s*(KB|MB|GB|B)?/i.exec(size);
  if (!m) return 0;
  const unit = (m[2] ?? "B").toUpperCase();
  return Math.round(Number(m[1]) * (unit === "GB" ? 1024 ** 3 : unit === "MB" ? 1024 ** 2 : unit === "KB" ? 1024 : 1));
}

function toSummary(state: MockPostState, p: Post): PostSummary {
  const senior = p.recruit ? state.alumni.find((a) => a.id === p.recruit!.targetId) : undefined;
  return {
    id: p.id,
    category: p.category,
    title: p.title,
    isAnonymous: !!p.anonymous,
    author: author(state, p),
    viewCount: p.views,
    commentCount: p.comments.filter((c) => !isDeletedComment(c.text)).length,
    hasFile: !!(p.files?.length || p.images?.length),
    thumbnailUrl: p.images?.[0] ?? null,
    deadline: p.deadline ?? null,
    meetup: p.meetup ? { meetAt: p.meetup.at, place: p.meetup.place, capacity: p.meetup.max, participantCount: p.meetup.memberIds.length, status: meetupStatus(p.meetup) } : null,
    recruit: p.recruit
      ? {
          senior: senior ? { id: senior.id, name: senior.name, generation: senior.generation, isDeleted: false } : { id: null, name: null, generation: null, isDeleted: true },
          capacity: p.recruit.max,
          memberCount: p.recruit.memberIds.length,
          deadline: p.recruit.deadline,
          status: p.recruit.status,
        }
      : null,
    createdAt: p.createdAt,
  };
}

function toDetail(state: MockPostState, p: Post): PostDetail {
  const a = p.anonymous ? undefined : state.alumni.find((x) => x.id === p.authorId);
  const senior = p.recruit ? state.alumni.find((x) => x.id === p.recruit!.targetId) : undefined;
  const files: PostFile[] = [
    ...(p.images ?? []).map((url, i) => ({ id: `${p.id}-img-${i}`, type: "IMAGE" as const, url, name: `image-${i + 1}`, size: 0 })),
    ...(p.files ?? []).map((f, i) => ({ id: `${p.id}-file-${i}`, type: "ATTACHMENT" as const, url: "", name: f.name, size: parseSize(f.size) })),
  ];
  return {
    id: p.id,
    category: p.category,
    title: p.title,
    content: p.body,
    isAnonymous: !!p.anonymous,
    isMine: p.authorId === ME_ID,
    author: p.anonymous ? null : a ? { ...author(state, p)!, bio: a.intro, status: a.status } : { ...author(state, p)!, bio: null, status: null },
    links: p.links ?? [],
    files,
    deadline: p.deadline ?? null,
    viewCount: p.views,
    commentCount: p.comments.filter((c) => !isDeletedComment(c.text)).length,
    likeCount: p.likes,
    likedByMe: liked.has(p.id),
    meetup: p.meetup
      ? {
          meetAt: p.meetup.at,
          place: p.meetup.place,
          capacity: p.meetup.max,
          participantCount: p.meetup.memberIds.length,
          status: meetupStatus(p.meetup),
          joinedByMe: p.meetup.memberIds.includes(ME_ID),
          members: p.meetup.memberIds.map((id) => member(state, id, p.authorId)),
        }
      : null,
    recruit: p.recruit
      ? {
          senior: senior
            ? { id: senior.id, name: senior.name, generation: senior.generation, lionParts: parts(senior), photoUrl: null, acceptGroup: senior.allowGroup && senior.receive === "OPEN", isDeleted: false }
            : { id: null, name: null, generation: null, lionParts: null, photoUrl: null, acceptGroup: false, isDeleted: true },
          capacity: p.recruit.max,
          memberCount: p.recruit.memberIds.length,
          deadline: p.recruit.deadline,
          status: p.recruit.status,
          joinedByMe: p.recruit.memberIds.includes(ME_ID),
          isSenior: p.recruit.targetId === ME_ID,
          hasOpenCoffeeChatWithSenior: state.coffeeChats.some((c) => c.receiverId === p.recruit!.targetId && c.requesterId === ME_ID && ["PENDING", "ACCEPTED", "SCHEDULED"].includes(c.status)),
          members: p.recruit.memberIds.map((id) => member(state, id, p.authorId)),
        }
      : null,
    createdAt: p.createdAt,
    updatedAt: p.edited ? p.createdAt : null,
  };
}

function visible(state: MockPostState) {
  return state.posts.filter((p) => !state.blocked.includes(p.authorId));
}

function isRecruiting(p: Post) {
  if (p.recruit) return p.recruit.status === "OPEN" && p.recruit.memberIds.length < p.recruit.max;
  if (p.meetup) return meetupStatus(p.meetup) === "OPEN";
  return false;
}

export function mockListPosts(state: MockPostState, query: PostListQuery): Promise<Page<PostSummary>> {
  const kw = query.q?.trim().toLowerCase() ?? "";
  const weekAgo = Date.now() - 7 * 86_400_000;
  let list = visible(state).filter(
    (p) =>
      (!query.category?.length || query.category.includes(p.category)) &&
      (!kw || `${p.title} ${p.body}`.toLowerCase().includes(kw)) &&
      (!query.recruiting || isRecruiting(p)) &&
      (query.period !== "WEEK" || new Date(p.createdAt).getTime() > weekAgo),
  );
  list = query.sort === "VIEWS" ? [...list].sort((a, b) => b.views - a.views) : [...list].sort((a, b) => b.createdAt.localeCompare(a.createdAt));
  const page = query.page ?? 0;
  const size = query.size ?? 20;
  const items = list.slice(page * size, (page + 1) * size).map((p) => toSummary(state, p));
  return delay({ items, page, size, totalElements: list.length, hasNext: (page + 1) * size < list.length });
}

export function mockCategoryCounts(state: MockPostState): Promise<CategoryCounts> {
  const list = visible(state);
  const counts = { CONTEST: 0, JOB: 0, MATERIAL: 0, EVENT: 0, NOW: 0, FREE: 0, MEETUP: 0, RECRUIT: 0 };
  list.forEach((p) => (counts[p.category] += 1));
  return delay({ total: list.length, counts });
}

export function mockGetPost(state: MockPostState, id: Id): Promise<PostDetail> {
  const p = state.posts.find((x) => x.id === String(id));
  return p ? delay(toDetail(state, p)) : Promise.reject(notFound());
}

function toMockInput(body: PostWriteRequest, prev?: Post): Omit<Post, "id" | "views" | "likes" | "comments" | "createdAt" | "authorId"> {
  return {
    category: body.category,
    title: body.title,
    body: body.content,
    anonymous: prev?.anonymous ?? (body.category === "FREE" && !!body.isAnonymous),
    keywords: body.title.split(/\s+/).filter((w) => w.length > 1).slice(0, 3),
    links: body.links ?? [],
    deadline: body.deadline ?? undefined,
    images: prev?.images,
    files: prev?.files,
    meetup: body.meetup
      ? { at: new Date(body.meetup.meetAt).toISOString(), place: body.meetup.place, max: body.meetup.capacity, memberIds: prev?.meetup?.memberIds ?? [ME_ID, ...(body.meetup.companionIds ?? []).map(String)] }
      : undefined,
    recruit: body.recruit
      ? { targetId: String(body.recruit.seniorId), status: prev?.recruit?.status ?? "OPEN", max: body.recruit.capacity, memberIds: prev?.recruit?.memberIds ?? [ME_ID], deadline: new Date(`${body.recruit.deadline}T23:59:00+09:00`).toISOString() }
      : undefined,
  };
}

export function mockCreatePost(actions: MockPostActions, body: PostWriteRequest) {
  const id = actions.createPost(toMockInput(body));
  return delay({ id, category: body.category, createdAt: new Date().toISOString() });
}

export function mockUpdatePost(state: MockPostState, actions: MockPostActions, id: Id, patch: PostUpdateRequest) {
  const prev = state.posts.find((p) => p.id === String(id));
  if (!prev) return Promise.reject(notFound());
  const category = patch.category ?? prev.category;
  const body: PostWriteRequest = {
    category,
    title: patch.title ?? prev.title,
    content: patch.content ?? prev.body,
    links: patch.links ?? prev.links,
    deadline: patch.deadline === undefined ? prev.deadline : patch.deadline,
    meetup:
      category === "MEETUP"
        ? { meetAt: patch.meetup?.meetAt ?? prev.meetup?.at ?? new Date().toISOString(), place: patch.meetup?.place ?? prev.meetup?.place ?? "", capacity: patch.meetup?.capacity ?? prev.meetup?.max ?? 2 }
        : undefined,
    recruit:
      category === "RECRUIT"
        ? { seniorId: patch.recruit?.seniorId ?? prev.recruit?.targetId ?? "", capacity: patch.recruit?.capacity ?? prev.recruit?.max ?? 2, deadline: patch.recruit?.deadline ?? prev.recruit?.deadline.slice(0, 10) ?? "" }
        : undefined,
  };
  actions.updatePost(prev.id, { ...toMockInput(body, prev), edited: true });
  return delay({ id, title: body.title, updatedAt: new Date().toISOString() });
}

export function mockDeletePost(actions: MockPostActions, id: Id) {
  actions.deletePost(String(id));
  return delay(undefined);
}

export function mockSetLike(state: MockPostState, actions: MockPostActions, id: Id, like: boolean): Promise<LikeResult> {
  const p = state.posts.find((x) => x.id === String(id));
  if (!p) return Promise.reject(notFound());
  const was = liked.has(p.id);
  const likes = p.likes + (like === was ? 0 : like ? 1 : -1);
  if (like) liked.add(p.id);
  else liked.delete(p.id);
  actions.updatePost(p.id, { likes });
  return delay({ postId: id, liked: like, likeCount: likes });
}

export function mockPopularKeywords(keywords: readonly string[], size: number) {
  return delay({ items: keywords.slice(0, size).map((keyword, i) => ({ keyword, count: keywords.length - i })) });
}

// ---------------------------------------------------------------------------
// 댓글 · 번개 참여
// ---------------------------------------------------------------------------

/** 예전 mock 데이터는 마침표 없이 '삭제된 댓글이에요'로 표시했다 */
function isDeletedComment(text: string) {
  return text.startsWith("삭제된 댓글이에요");
}

function toComment(state: MockPostState, post: Post, c: Post["comments"][number]): PostComment {
  const deleted = isDeletedComment(c.text);
  // 서버 규칙: 익명 글의 작성자가 쓴 댓글만 익명
  const anonymous = !!post.anonymous && c.authorId === post.authorId;
  const a = state.alumni.find((x) => x.id === c.authorId);
  return {
    id: c.id,
    postId: post.id,
    content: deleted ? DELETED_COMMENT : c.text,
    isAnonymous: anonymous,
    author: deleted || anonymous ? null : a ? { id: a.id, name: a.name, generation: a.generation, lionParts: parts(a), photoUrl: null, headline: a.headline, isDeleted: false } : { id: null, name: null, generation: null, lionParts: null, photoUrl: null, headline: null, isDeleted: true },
    deleted,
    isMine: c.authorId === ME_ID,
    createdAt: c.at,
  };
}

export function mockListComments(state: MockPostState, postId: Id, page: number, size: number): Promise<Slice<PostComment>> {
  const post = state.posts.find((p) => p.id === String(postId));
  if (!post) return Promise.reject(notFound());
  const items = post.comments.slice(page * size, (page + 1) * size).map((c) => toComment(state, post, c));
  return delay({ items, page, size, hasNext: (page + 1) * size < post.comments.length });
}

export function mockAddComment(state: MockPostState, actions: MockPostActions & { addComment: (postId: string, text: string, anonymous?: boolean) => void }, postId: Id, content: string): Promise<PostComment> {
  const post = state.posts.find((p) => p.id === String(postId));
  if (!post) return Promise.reject(notFound());
  const anonymous = !!post.anonymous && post.authorId === ME_ID;
  actions.addComment(post.id, content, anonymous);
  const created = { id: `c-${Date.now()}`, authorId: ME_ID, text: content, anonymous, at: new Date().toISOString() };
  return delay(toComment(state, post, created));
}

export function mockRemoveComment(state: MockPostState, actions: MockPostActions, postId: Id, commentId: Id): Promise<void> {
  const post = state.posts.find((p) => p.id === String(postId));
  if (!post) return Promise.reject(notFound());
  actions.updatePost(post.id, { comments: post.comments.map((c) => (c.id === String(commentId) ? { ...c, text: DELETED_COMMENT } : c)) });
  return delay(undefined);
}

export function mockSetMeetupJoined(state: MockPostState, actions: MockPostActions, postId: Id, joined: boolean): Promise<MeetupParticipation> {
  const post = state.posts.find((p) => p.id === String(postId));
  if (!post) return Promise.reject(notFound());
  if (!post.meetup) return Promise.reject(new ApiError(404, "MEETUP_NOT_FOUND", "번개 글이 아니에요."));
  const m = post.meetup;
  if (!joined && post.authorId === ME_ID) return Promise.reject(new ApiError(403, "HOST_CANNOT_LEAVE", "작성자는 참여를 취소할 수 없어요. 번개를 취소해 주세요."));
  if (joined && !m.memberIds.includes(ME_ID)) {
    if (meetupStatus(m) === "ENDED") return Promise.reject(new ApiError(409, "MEETUP_CLOSED", "이미 시작된 번개예요."));
    if (m.memberIds.length >= m.max) return Promise.reject(new ApiError(409, "MEETUP_FULL", "정원이 다 찼어요."));
  }
  const memberIds = joined ? (m.memberIds.includes(ME_ID) ? m.memberIds : [...m.memberIds, ME_ID]) : m.memberIds.filter((id) => id !== ME_ID);
  const next = { ...m, memberIds };
  actions.updatePost(post.id, { meetup: next });
  return delay({
    postId,
    joined,
    participantCount: memberIds.length,
    capacity: m.max,
    status: meetupStatus(next),
    members: memberIds.map((id) => member(state, id, post.authorId)),
  });
}
