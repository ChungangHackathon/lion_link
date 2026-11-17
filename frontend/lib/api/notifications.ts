/** 알림 (docs/api.md NOTI-01 ~ 06) */

import type { Id } from "./posts";
import { api } from "./client";

export type NotificationTab = "COFFEE_CHAT" | "CHAT" | "COMMUNITY" | "ACCOUNT";
export type NotificationTargetType = "COFFEE_CHAT" | "ROOM" | "POST" | "USER";

export interface NotificationItem {
  id: Id;
  tab: NotificationTab;
  /** 알림 종류 (COMMENT_CREATED, COFFEE_CHAT_REQUESTED, SIGNUP_APPROVED …) */
  type: string;
  targetType: NotificationTargetType;
  targetId: Id | null;
  /** 보낸 사람. null이면 시스템 알림이고 message가 문장 전체 */
  actor: { id: Id | null; name: string | null; photoUrl: string | null; isDeleted: boolean } | null;
  /** actor가 있으면 이름 뒤에 붙는 문구 */
  message: string;
  read: boolean;
  createdAt: string;
}

export interface UnreadCounts {
  all: number;
  coffeeChat: number;
  chat: number;
  community: number;
}

export interface NotificationList {
  items: NotificationItem[];
  unreadCounts: UnreadCounts;
}

export interface EmailSettings {
  emailCoffeeChat: boolean;
  emailMessageRequest: boolean;
  emailSignupApproved: boolean;
}

export type EmailType = "COFFEE_CHAT_REQUEST" | "MESSAGE_REQUEST" | "SIGNUP_APPROVED";
export const EMAIL_TYPES: EmailType[] = ["COFFEE_CHAT_REQUEST", "MESSAGE_REQUEST", "SIGNUP_APPROVED"];
export const EMAIL_TYPE_LABEL: Record<EmailType, string> = { COFFEE_CHAT_REQUEST: "커피챗 신청", MESSAGE_REQUEST: "메시지 요청", SIGNUP_APPROVED: "가입 승인" };
/** 메일 종류 → 설정 필드 */
export const EMAIL_SETTING_KEY: Record<EmailType, keyof EmailSettings> = { COFFEE_CHAT_REQUEST: "emailCoffeeChat", MESSAGE_REQUEST: "emailMessageRequest", SIGNUP_APPROVED: "emailSignupApproved" };

/** NOTI-01 최근 알림 20개 */
export function listNotifications(tab: "ALL" | Exclude<NotificationTab, "ACCOUNT"> = "ALL", signal?: AbortSignal) {
  return api<NotificationList>("/notifications", { query: { tab }, signal });
}

/** NOTI-02 */
export async function readNotification(id: Id): Promise<void> {
  await api(`/notifications/${id}/read`, { method: "PUT" });
}

/** NOTI-03 */
export async function readAllNotifications(): Promise<void> {
  await api("/notifications/read-all", { method: "PUT" });
}

/** NOTI-04 */
export function getEmailSettings() {
  return api<EmailSettings>("/me/notification-settings");
}

/** NOTI-05 보낸 항목만 바뀐다 */
export function updateEmailSettings(patch: Partial<EmailSettings>) {
  return api<EmailSettings>("/me/notification-settings", { method: "PATCH", body: patch });
}

/** NOTI-06 메일의 [그만 받기] 링크. 로그인 없이 부른다 */
export async function unsubscribeEmail(token: string, type: EmailType): Promise<void> {
  await api("/email-notifications/unsubscribe", { query: { token, type }, auth: false });
}

/** 알림을 눌렀을 때 갈 화면 (targetType · targetId로 FE가 정한다) */
export function notificationHref(n: Pick<NotificationItem, "type" | "targetType" | "targetId">): string {
  if (n.type === "MESSAGE_REQUESTED") return "/chat/requests";
  switch (n.targetType) {
    case "COFFEE_CHAT":
      return n.targetId != null ? `/coffee?id=${n.targetId}` : "/coffee";
    case "ROOM":
      return n.targetId != null ? `/chat/${n.targetId}` : "/chat";
    case "POST":
      return n.targetId != null ? `/community/${n.targetId}` : "/community";
    default:
      return n.type === "SIGNUP_REQUESTED" ? "/admin" : "/home";
  }
}
