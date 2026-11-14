/** 6. 커뮤니티 게시글 API (docs/api.md POST-01 ~ POST-08) */

import type { PostCategory, ProfileStatus } from "@/lib/types";
import type { ApiLionPart } from "./profile";
import { api, ApiError, type Page, type Slice } from "./client";

/** 서버 ID는 숫자, 프로토타입(mock) 데이터는 문자열이라 둘 다 받는다 */
export type Id = number | string;

export type MeetupStatus = "OPEN" | "FULL" | "ENDED" | "CANCELED";
export type RecruitStatus = "OPEN" | "APPLIED" | "ENDED";
export type PostSort = "LATEST" | "VIEWS";

/** 익명 글이면 null. 탈퇴한 작성자는 isDeleted=true이고 나머지는 null */
export interface PostAuthor {
  id: Id | null;
  name: string | null;
  generation: number | null;
  lionParts: ApiLionPart[] | null;
  photoUrl: string | null;
  headline: string | null;
  isDeleted: boolean;
}

export interface PostDetailAuthor extends PostAuthor {
  bio: string | null;
  status: ProfileStatus | null;
}

export interface PostMember {
  id: Id | null;
  name: string | null;
  generation: number | null;
  lionParts: ApiLionPart[] | null;
  photoUrl: string | null;
  isHost: boolean;
  isDeleted: boolean;
}

export interface PostSummary {
  id: Id;
  category: PostCategory;
  title: string;
  isAnonymous: boolean;
  author: PostAuthor | null;
  viewCount: number;
  commentCount: number;
  hasFile: boolean;
  thumbnailUrl: string | null;
  /** 공모전·채용 마감일 (YYYY-MM-DD) */
  deadline: string | null;
  meetup: { meetAt: string; place: string; capacity: number; participantCount: number; status: MeetupStatus } | null;
  recruit: {
    senior: { id: Id | null; name: string | null; generation: number | null; isDeleted: boolean };
    capacity: number;
    memberCount: number;
    deadline: string;
    status: RecruitStatus;
  } | null;
  createdAt: string;
}

export interface PostFile {
  id: Id;
  type: "IMAGE" | "ATTACHMENT";
  url: string;
  name: string;
  size: number;
}

export interface PostDetail {
  id: Id;
  category: PostCategory;
  title: string;
  content: string;
  isAnonymous: boolean;
  /** 익명 글이어도 내 글이면 true */
  isMine: boolean;
  author: PostDetailAuthor | null;
  links: string[];
  files: PostFile[];
  deadline: string | null;
  viewCount: number;
  commentCount: number;
  likeCount: number;
  likedByMe: boolean;
  meetup: {
    meetAt: string;
    place: string;
    capacity: number;
    participantCount: number;
    status: MeetupStatus;
    joinedByMe: boolean;
    members: PostMember[];
  } | null;
  recruit: {
    senior: { id: Id | null; name: string | null; generation: number | null; lionParts: ApiLionPart[] | null; photoUrl: string | null; acceptGroup: boolean; isDeleted: boolean };
    capacity: number;
    memberCount: number;
    deadline: string;
    status: RecruitStatus;
    joinedByMe: boolean;
    isSenior: boolean;
    hasOpenCoffeeChatWithSenior: boolean;
    members: PostMember[];
  } | null;
  createdAt: string;
  /** 수정한 적이 있을 때만 값이 있다 ('수정됨') */
  updatedAt: string | null;
}

export interface PostListQuery {
  category?: PostCategory[];
  q?: string;
  sort?: PostSort;
  recruiting?: boolean;
  period?: "WEEK";
  page?: number;
  size?: number;
}

export interface CategoryCounts {
  total: number;
  counts: Record<PostCategory, number>;
}

export interface PopularKeyword {
  keyword: string;
  count: number;
}

export interface PostWriteRequest {
  category: PostCategory;
  title: string;
  content: string;
  links?: string[];
  /** FREE에서만 true 가능 (작성 때만) */
  isAnonymous?: boolean;
  /** CONTEST·JOB만 (YYYY-MM-DD) */
  deadline?: string | null;
  /** MEETUP이면 필수. meetAt은 KST ISO-8601 */
  meetup?: { meetAt: string; place: string; capacity: number; companionIds?: Id[] };
  /** RECRUIT이면 필수 */
  recruit?: { seniorId: Id; capacity: number; deadline: string };
  /** FILE-01로 올린 파일. 이미지 5장, 첨부파일 3개까지. 보낸 순서대로 저장된다 */
  files?: PostFileInput[];
}

export interface PostFileInput {
  type: "IMAGE" | "ATTACHMENT";
  url: string;
  name: string;
  size: number;
}

/** POST-05 수정: 보낸 필드만 바뀐다. 익명 여부·함께하는 동문은 바꿀 수 없다 */
export interface PostUpdateRequest {
  category?: PostCategory;
  title?: string;
  content?: string;
  links?: string[];
  deadline?: string | null;
  meetup?: { meetAt?: string; place?: string; capacity?: number };
  recruit?: { seniorId?: Id; capacity?: number; deadline?: string };
  /** 보내면 파일 목록을 통째로 바꾼다 (남길 파일도 다시 보낸다). 빈 배열이면 모두 지운다 */
  files?: PostFileInput[];
}

export interface LikeResult {
  postId: Id;
  liked: boolean;
  likeCount: number;
}

/** POST-01 게시글 목록·검색 */
export function listPosts(query: PostListQuery = {}, signal?: AbortSignal) {
  return api<Page<PostSummary>>("/posts", {
    query: {
      category: query.category?.length ? query.category.join(",") : undefined,
      q: query.q?.trim(),
      sort: query.sort,
      recruiting: query.recruiting || undefined,
      period: query.period,
      page: query.page,
      size: query.size,
    },
    signal,
  });
}

/** POST-02 카테고리별 게시글 수 */
export function getCategoryCounts() {
  return api<CategoryCounts>("/posts/category-counts");
}

/** POST-08 인기 키워드 */
export function getPopularKeywords(size = 10) {
  return api<{ items: PopularKeyword[] }>("/posts/popular-keywords", { query: { size } });
}

/** POST-04 게시글 상세 */
export function getPost(id: Id) {
  return api<PostDetail>(`/posts/${id}`);
}

/** POST-03 게시글 작성 */
export function createPost(body: PostWriteRequest) {
  return api<{ id: Id; category: PostCategory; createdAt: string }>("/posts", { method: "POST", body });
}

/** POST-05 게시글 수정 (보낸 필드만 바뀐다) */
export function updatePost(id: Id, body: PostUpdateRequest) {
  return api<{ id: Id; title: string; updatedAt: string }>(`/posts/${id}`, { method: "PATCH", body });
}

/** POST-06 게시글 삭제 (번개 취소·모집 취소도 이것) */
export function deletePost(id: Id) {
  return api(`/posts/${id}`, { method: "DELETE" });
}

/** POST-07 좋아요 · 좋아요 취소 */
export function setPostLike(id: Id, liked: boolean) {
  return api<LikeResult>(`/posts/${id}/like`, { method: liked ? "PUT" : "DELETE" });
}

// ---------------------------------------------------------------------------
// 댓글 (COMMENT-01 ~ 03) · 번개 참여 (MEETUP-01)
// ---------------------------------------------------------------------------

export interface PostComment {
  id: Id;
  postId: Id;
  /** 삭제된 댓글은 '삭제된 댓글이에요.' */
  content: string;
  /** 익명 글의 작성자가 쓴 댓글만 익명 (서버가 정한다) */
  isAnonymous: boolean;
  /** 익명·삭제된 댓글은 null */
  author: PostAuthor | null;
  deleted: boolean;
  isMine: boolean;
  createdAt: string;
}

export interface MeetupParticipation {
  postId: Id;
  joined: boolean;
  participantCount: number;
  capacity: number;
  status: MeetupStatus;
  members: PostMember[];
}

/** COMMENT-01 댓글 목록 (오래된 순) */
export function listComments(postId: Id, page = 0, size = 20) {
  return api<Slice<PostComment>>(`/posts/${postId}/comments`, { query: { page, size } });
}

/** COMMENT-02 댓글 작성 (1~500자) */
export function createComment(postId: Id, content: string) {
  return api<PostComment>(`/posts/${postId}/comments`, { method: "POST", body: { content } });
}

/** COMMENT-03 댓글 삭제 */
export function deleteComment(postId: Id, commentId: Id) {
  return api(`/posts/${postId}/comments/${commentId}`, { method: "DELETE" });
}

/** MEETUP-01 번개 참여 · 참여 취소 */
export function setMeetupJoined(postId: Id, joined: boolean) {
  return api<MeetupParticipation>(`/posts/${postId}/meetup/members/me`, { method: joined ? "PUT" : "DELETE" });
}

// ---------------------------------------------------------------------------
// 번개 초대 (MEETUP-02) · 커피챗 모집 참여 (RECRUIT-01) · 파일 업로드 (FILE-01)
// ---------------------------------------------------------------------------

export interface RecruitParticipation {
  postId: Id;
  joined: boolean;
  memberCount: number;
  capacity: number;
  status: RecruitStatus;
  members: PostMember[];
}

/** MEETUP-02 번개 초대 (1~20명). 이미 참여·초대한 사람 등은 서버가 건너뛴다 */
export function inviteToMeetup(postId: Id, userIds: Id[]) {
  return api<{ sentCount: number; skippedUserIds: Id[] }>(`/posts/${postId}/meetup/invites`, { method: "POST", body: { userIds } });
}

/** RECRUIT-01 커피챗 모집 참여 (참여 취소는 아직 없음) */
export function joinRecruit(postId: Id) {
  return api<RecruitParticipation>(`/posts/${postId}/recruit/members/me`, { method: "PUT" });
}

export type UploadType = PostFileInput["type"];

/** FILE-01 규칙: 형식과 크기 */
export const UPLOAD_RULES: Record<UploadType, { exts: string[]; maxBytes: number; maxCount: number; accept: string }> = {
  IMAGE: { exts: ["jpg", "jpeg", "png", "webp"], maxBytes: 10 * 1024 * 1024, maxCount: 5, accept: ".jpg,.jpeg,.png,.webp" },
  ATTACHMENT: { exts: ["pdf", "hwp", "docx", "pptx", "xlsx", "zip"], maxBytes: 20 * 1024 * 1024, maxCount: 3, accept: ".pdf,.hwp,.docx,.pptx,.xlsx,.zip" },
};

/** 올리기 전에 형식·크기를 확인한다. 문제가 없으면 null */
export function checkUpload(type: UploadType, file: File): string | null {
  const ext = file.name.split(".").pop()?.toLowerCase() ?? "";
  const rule = UPLOAD_RULES[type];
  if (!rule.exts.includes(ext)) return "지원하지 않는 파일 형식이에요.";
  if (file.size === 0) return "빈 파일은 올릴 수 없어요.";
  if (file.size > rule.maxBytes) return "업로드할 수 있는 파일 크기를 넘었어요.";
  return null;
}

/** FILE-01 업로드 주소 발급 */
export function requestUploadUrl(type: UploadType, fileName: string, size: number) {
  return api<{ uploadUrl: string; fileUrl: string; contentType: string; expiresAt: string }>("/files", { method: "POST", body: { type, fileName, size } });
}

/** 주소를 받아 S3에 바로 올리고, 글에 넣을 파일 정보를 돌려준다 */
export async function uploadFile(type: UploadType, file: File): Promise<PostFileInput> {
  const { uploadUrl, fileUrl, contentType } = await requestUploadUrl(type, file.name, file.size);
  let res: Response;
  try {
    res = await fetch(uploadUrl, { method: "PUT", headers: { "Content-Type": contentType }, body: file });
  } catch {
    throw new ApiError(0, "UPLOAD_FAILED", "파일을 올리지 못했어요. 잠시 후 다시 시도해주세요.");
  }
  if (!res.ok) throw new ApiError(res.status, "UPLOAD_FAILED", "파일을 올리지 못했어요. 잠시 후 다시 시도해주세요.");
  return { type, url: fileUrl, name: file.name, size: file.size };
}

/** "2026-11-20", "19:00" → "2026-11-20T19:00:00+09:00" (서버는 KST 기준) */
export function toKstDateTime(date: string, time: string): string {
  return `${date}T${time.length === 5 ? `${time}:00` : time}+09:00`;
}

/** KST ISO 시각 → 입력칸용 날짜·시각 ("2026-11-20", "19:00") */
export function fromKstDateTime(iso: string): { date: string; time: string } {
  const kst = new Date(new Date(iso).getTime() + 9 * 3_600_000).toISOString();
  return { date: kst.slice(0, 10), time: kst.slice(11, 16) };
}
