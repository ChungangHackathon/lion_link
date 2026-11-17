/** 8. 커피챗 API (docs/api.md COFFEE-01 ~ 10) */

import type { CoffeeChatRejectReason, CoffeeChatStatus, MeetingMode } from "@/lib/types";
import type { ApiLionPart } from "./profile";
import type { CoffeeTopic } from "./alumni";
import type { Id } from "./posts";
import { api } from "./client";

export type CoffeeRole = "RECEIVER" | "REQUESTER" | "MEMBER";
export type CoffeeBox = "RECEIVED" | "SENT";
export type CoffeeFilter = "ALL" | "PENDING" | "ACTIVE" | "ENDED";
export type CoffeeAction = "ACCEPT" | "REJECT" | "WITHDRAW" | "LEAVE" | "SCHEDULE" | "CANCEL" | "COMPLETE";

/** 사람 요약. 탈퇴한 사람은 isDeleted=true, 나머지는 null */
export interface CoffeePerson {
  id: Id | null;
  name: string | null;
  generation: number | null;
  lionParts: ApiLionPart[] | null;
  photoUrl: string | null;
  isDeleted: boolean;
}

export interface CoffeeChatItem {
  id: Id;
  status: CoffeeChatStatus;
  myRole: CoffeeRole;
  /** 받은 신청이면 대표 신청자, 보낸 신청이면 받는 사자 */
  counterpart: CoffeePerson;
  /** 함께 신청자 (빠진 사람 제외) */
  members: CoffeePerson[];
  topic: CoffeeTopic;
  topicCustom: string | null;
  reason: string;
  createdAt: string;
  expiresAt: string | null;
  scheduledAt: string | null;
}

export interface CoffeeChatList {
  items: CoffeeChatItem[];
  page: number;
  size: number;
  hasNext: boolean;
  pendingReceivedCount: number;
  activeCount: number;
}

export interface CoffeeChatDetail {
  id: Id;
  status: CoffeeChatStatus;
  myRole: CoffeeRole;
  receiver: CoffeePerson;
  requester: CoffeePerson;
  members: CoffeePerson[];
  topic: CoffeeTopic;
  topicCustom: string | null;
  reason: string;
  questions: string[];
  preferredMode: MeetingMode;
  scheduleNote: string | null;
  proposedSlots: string[];
  responseMessage: string | null;
  rejectReason: CoffeeChatRejectReason | null;
  meetingMode: "ONLINE" | "OFFLINE" | null;
  scheduledAt: string | null;
  meetingPlace: string | null;
  meetingLink: string | null;
  cancelReason: string | null;
  canceledBy: Id | null;
  roomId: Id | null;
  recruitPostId: Id | null;
  createdAt: string;
  expiresAt: string | null;
  acceptedAt: string | null;
  rejectedAt: string | null;
  canceledAt: string | null;
  completedAt: string | null;
  /** 지금 내가 할 수 있는 동작 */
  actions: CoffeeAction[];
}

export interface CoffeeRequest {
  /** 모집글이 아니면 필수 */
  receiverId?: Id;
  topic: CoffeeTopic;
  /** topic이 ETC면 필수, 1~30자 */
  topicCustom?: string;
  /** 10~500자 */
  reason: string;
  /** 0~3개, 각 5~200자 */
  questions?: string[];
  preferredMode: MeetingMode;
  /** 0~100자 */
  scheduleNote?: string;
  /** 함께 신청할 동문 0~4명 */
  memberIds?: Id[];
  /** 커뮤니티 커피챗 모집글에서 신청할 때 */
  fromRecruitId?: Id;
}

/** COFFEE-01 커피챗 신청 */
export function requestCoffeeChat(body: CoffeeRequest) {
  return api<{ id: Id; status: CoffeeChatStatus; createdAt: string; expiresAt: string }>("/coffee-chats", { method: "POST", body });
}

/** COFFEE-02 내 커피챗 목록 (최근 신청순) */
export function listCoffeeChats(box: CoffeeBox, filter: CoffeeFilter = "ALL", page = 0, size = 20, signal?: AbortSignal) {
  return api<CoffeeChatList>("/coffee-chats", { query: { box, filter, page, size }, signal });
}

/** COFFEE-03 커피챗 상세 */
export function getCoffeeChat(id: Id) {
  return api<CoffeeChatDetail>(`/coffee-chats/${id}`);
}

/** COFFEE-06 신청 취소 (대표 신청자, 대기 중) */
export function withdrawCoffeeChat(id: Id) {
  return api<CoffeeChatDetail>(`/coffee-chats/${id}/withdraw`, { method: "POST" });
}

/** COFFEE-04 수락 (받는 사자, 대기 중). 일정 후보 1~3개, 한마디 0~200자 */
export function acceptCoffeeChat(id: Id, body: { proposedSlots: string[]; responseMessage?: string }) {
  return api<CoffeeChatDetail>(`/coffee-chats/${id}/accept`, { method: "POST", body });
}

/** COFFEE-05 거절 (받는 사자, 대기 중) */
export function rejectCoffeeChat(id: Id, body: { rejectReason: CoffeeChatRejectReason; responseMessage?: string }) {
  return api<CoffeeChatDetail>(`/coffee-chats/${id}/reject`, { method: "POST", body });
}

/** COFFEE-07 일정 확정·변경. 오프라인이면 장소 필수, 온라인이면 링크(선택) */
export function scheduleCoffeeChat(id: Id, body: { scheduledAt: string; meetingMode: "ONLINE" | "OFFLINE"; meetingPlace?: string; meetingLink?: string }) {
  return api<CoffeeChatDetail>(`/coffee-chats/${id}/schedule`, { method: "PUT", body });
}

/** COFFEE-08 커피챗 취소 (수락 이후). 사유 5~200자, 상대에게 보인다 */
export function cancelCoffeeChat(id: Id, cancelReason: string) {
  return api<CoffeeChatDetail>(`/coffee-chats/${id}/cancel`, { method: "POST", body: { cancelReason } });
}

/** COFFEE-09 만남 완료 (만남 시각이 지난 뒤) */
export function completeCoffeeChat(id: Id) {
  return api<CoffeeChatDetail>(`/coffee-chats/${id}/complete`, { method: "POST" });
}

/** COFFEE-10 함께 신청자 빠지기 */
export function leaveCoffeeChat(id: Id) {
  return api(`/coffee-chats/${id}/members/me`, { method: "DELETE" });
}

// ---------------------------------------------------------------------------
// 라벨
// ---------------------------------------------------------------------------

export const COFFEE_TOPIC_LABEL: Record<CoffeeTopic, string> = {
  JOB_PREP: "취업 준비",
  CAREER: "커리어·이직",
  STUDY: "학업·전공",
  PROJECT_DEV: "프로젝트·개발",
  STARTUP: "창업",
  LION_ACTIVITY: "멋사 활동",
  ETC: "기타",
};

export const topicText = (c: { topic: CoffeeTopic; topicCustom: string | null }) => (c.topic === "ETC" && c.topicCustom ? c.topicCustom : COFFEE_TOPIC_LABEL[c.topic] ?? c.topic);

export const personName = (p: CoffeePerson | null | undefined) => (!p || p.isDeleted || !p.name ? "알 수 없음" : p.name);
