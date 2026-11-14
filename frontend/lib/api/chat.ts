/** 채팅 (docs/api.md CHAT-01 ~ 15). 실시간(WebSocket)은 다음 작업에서 붙인다 */

import type { CoffeeChatStatus } from "@/lib/types";
import type { ApiLionPart } from "./profile";
import type { Id } from "./posts";
import { api } from "./client";

export type RoomType = "DIRECT" | "GROUP";
export type RoomStatus = "REQUESTED" | "ACTIVE" | "DECLINED" | "CANCELED";
export type DeclineReason = "BUSY" | "NOT_MY_FIELD" | "PREFER_COFFEE_CHAT" | "ETC";

/** 사용자 요약. 탈퇴했으면 isDeleted: true이고 나머지는 null */
export interface ChatUser {
  id: Id | null;
  name: string | null;
  generation: number | null;
  lionParts: ApiLionPart[] | null;
  photoUrl: string | null;
  isDeleted: boolean;
}

export interface ChatMessage {
  id: Id;
  type: "TEXT" | "SYSTEM";
  senderId: Id | null;
  content: string;
  deleted: boolean;
  /** 보낼 때 붙인 재전송 방지 id. 실시간으로 되돌아온 내 메시지를 알아보는 데 쓴다 (SYSTEM은 null) */
  clientMsgId: string | null;
  createdAt: string;
  editedAt: string | null;
  /** 보낸 사람을 뺀 참여자 중 아직 안 읽은 사람 수 (단체방에서 내 메시지 옆 숫자) */
  unreadCount: number;
}

export interface RoomSummary {
  roomId: Id;
  type: RoomType;
  status: RoomStatus;
  requestedByMe: boolean;
  /** 1:1 방의 상대 */
  partner: ChatUser | null;
  /** 단체방의 나를 뺀 참여자 */
  members: ChatUser[];
  memberCount: number;
  coffeeChatId: Id | null;
  meetupId: Id | null;
  /** 번개 단체방 이름(번개 글 제목) */
  title: string | null;
  lastMessage: ChatMessage | null;
  unreadCount: number;
  pinned: boolean;
  muted: boolean;
}

export interface RoomDetail {
  roomId: Id;
  type: RoomType;
  status: RoomStatus;
  requestedByMe: boolean;
  /** 나를 뺀 참여자 */
  members: ChatUser[];
  coffeeChat: {
    id: Id;
    status: CoffeeChatStatus;
    scheduledAt: string | null;
    proposedSlots: string[];
    meetingMode: "ONLINE" | "OFFLINE" | null;
    meetingPlace: string | null;
    meetingLink: string | null;
    questions: string[];
  } | null;
  meetup: { postId: Id; title: string; meetAt: string; place: string; status: "OPEN" | "FULL" | "ENDED" | "CANCELED" } | null;
  pinned: boolean;
  muted: boolean;
  canSend: boolean;
}

export interface RequestItem {
  roomId: Id;
  requester: ChatUser;
  firstMessage: ChatMessage;
  requestedAt: string;
}

export interface RequestDetail extends RequestItem {
  status: RoomStatus;
  receiver: ChatUser;
  declinedAt: string | null;
  declineReason: DeclineReason | null;
  declineMessage: string | null;
}

export interface SliceOf<T> {
  items: T[];
  page: number;
  size: number;
  hasNext: boolean;
}

export const DECLINE_REASONS: { value: DeclineReason; label: string }[] = [
  { value: "BUSY", label: "요즘 바빠서 대화가 어려워요" },
  { value: "NOT_MY_FIELD", label: "제가 도움드리기 어려운 분야예요" },
  { value: "PREFER_COFFEE_CHAT", label: "커피챗으로 신청해주세요" },
  { value: "ETC", label: "기타" },
];
export const declineLabel = (r: DeclineReason | null, message: string | null) => (r === "ETC" ? message ?? "" : DECLINE_REASONS.find((x) => x.value === r)?.label ?? "");
/** 거절된 날부터 7일 뒤에 다시 요청할 수 있다 */
export const REQUEST_COOLDOWN_DAYS = 7;

// CHAT-01 ~ 07 메시지 요청 ----------------------------------------------------

export function sendRequest(receiverId: Id, content: string) {
  return api<{ roomId: Id; status: RoomStatus; requestedByMe: boolean; created: boolean }>("/chat-requests", { method: "POST", body: { receiverId, content } });
}
export function listRequests(page = 0, size = 20, signal?: AbortSignal) {
  return api<SliceOf<RequestItem>>("/chat-requests", { query: { page, size }, signal });
}
export function getRequest(roomId: Id, signal?: AbortSignal) {
  return api<RequestDetail>(`/chat-requests/${roomId}`, { signal });
}
export function acceptRequest(roomId: Id) {
  return api<RoomDetail>(`/chat-requests/${roomId}/accept`, { method: "POST" });
}
export async function declineRequest(roomId: Id, reason: DeclineReason, message?: string): Promise<void> {
  await api(`/chat-requests/${roomId}/decline`, { method: "POST", body: reason === "ETC" ? { reason, message } : { reason } });
}
export function editRequestMessage(roomId: Id, messageId: Id, content: string) {
  return api<ChatMessage>(`/chat-requests/${roomId}/messages/${messageId}`, { method: "PATCH", body: { content } });
}
export async function cancelRequest(roomId: Id, messageId: Id): Promise<void> {
  await api(`/chat-requests/${roomId}/messages/${messageId}`, { method: "DELETE" });
}

// CHAT-08 ~ 15 채팅방 ----------------------------------------------------------

export function listRooms(page = 0, size = 20, signal?: AbortSignal) {
  return api<SliceOf<RoomSummary>>("/chat-rooms", { query: { page, size }, signal });
}
export function getRoom(roomId: Id, signal?: AbortSignal) {
  return api<RoomDetail>(`/chat-rooms/${roomId}`, { signal });
}
/** before: 그 메시지보다 오래된 것, after: 그 다음부터. 항상 오래된 것부터 */
export function listMessages(roomId: Id, cursor: { before?: Id; after?: Id } = {}, size = 30, signal?: AbortSignal) {
  return api<{ items: ChatMessage[]; hasMore: boolean }>(`/chat-rooms/${roomId}/messages`, { query: { before: cursor.before, after: cursor.after, size }, signal });
}
export function sendMessage(roomId: Id, content: string, clientMsgId?: string) {
  return api<ChatMessage>(`/chat-rooms/${roomId}/messages`, { method: "POST", body: { content, clientMsgId } });
}
export function readRoom(roomId: Id, lastMessageId?: Id) {
  return api<{ roomId: Id; lastReadMessageId: Id }>(`/chat-rooms/${roomId}/read`, { method: "PUT", body: lastMessageId != null ? { lastMessageId } : {} });
}
export function updatePreferences(roomId: Id, patch: { pinned?: boolean; muted?: boolean }) {
  return api<{ roomId: Id; pinned: boolean; muted: boolean }>(`/chat-rooms/${roomId}/preferences`, { method: "PATCH", body: patch });
}
export async function leaveRoom(roomId: Id): Promise<void> {
  await api(`/chat-rooms/${roomId}/members/me`, { method: "DELETE" });
}
export function getUnreadCount(signal?: AbortSignal) {
  return api<{ unreadMessages: number; messageRequests: number }>("/chat-rooms/unread-count", { signal });
}

/** 화면에 쓸 이름 */
export const userName = (u: ChatUser | null | undefined) => (!u || u.isDeleted ? "탈퇴한 동문" : u.name ?? "");
/** 방 이름: 번개방은 제목, 1:1은 상대 이름, 단체방은 참여자 이름 나열 */
export function roomTitle(r: Pick<RoomSummary, "type" | "title" | "partner" | "members">): string {
  if (r.title) return r.title;
  if (r.type === "DIRECT") return userName(r.partner);
  const names = r.members.map(userName);
  return names.length > 3 ? `${names.slice(0, 3).join(", ")} 외 ${names.length - 3}명` : names.join(", ");
}
