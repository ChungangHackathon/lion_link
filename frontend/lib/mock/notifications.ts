import { MOCK_NOW } from "./now";
import type { AppNotification, PendingSignup, Invite } from "@/lib/types";

const ago = (min: number) => new Date(MOCK_NOW - min * 60_000).toISOString();

export const notifications: AppNotification[] = [
  { id: "n-1", tab: "COFFEE", actorId: "u-5", text: "님이 커피챗을 신청했어요", at: ago(1), read: false, href: "/coffee/c-1", quick: "coffee-request", targetId: "c-1" },
  { id: "n-2", tab: "COFFEE", actorId: "u-10", text: "님이 커피챗을 신청했어요", at: ago(180), read: false, href: "/coffee/c-2", quick: "coffee-request", targetId: "c-2" },
  { id: "n-3", tab: "COFFEE", actorId: "u-4", text: "님이 커피챗을 수락했어요", at: ago(60 * 24), read: false, href: "/coffee/c-7" },
  { id: "n-4", tab: "CHAT", actorId: "u-1", text: "님이 메시지를 보냈어요", at: ago(30), read: false, href: "/chat/r-1" },
  { id: "n-5", tab: "CHAT", actorId: "u-11", text: "님이 메시지 요청을 보냈어요", at: ago(240), read: false, href: "/chat/requests" },
  { id: "n-6", tab: "CHAT", actorId: "u-10", text: "님이 단체 채팅방에 메시지를 보냈어요", at: ago(180), read: true, href: "/chat/r-3" },
  { id: "n-7", tab: "COMMUNITY", actorId: "u-10", text: "님이 내 모집글에 댓글을 남겼어요", at: ago(120), read: false, href: "/community/p-1" },
  { id: "n-8", tab: "COMMUNITY", actorId: "u-5", text: "님이 내 모집글에 신청했어요", at: ago(60 * 5), read: true, href: "/community/p-1" },
];

export const pendingSignups: PendingSignup[] = [
  { id: "s-1", name: "박중앙", generation: 13, email: "jungang@cau.ac.kr", part: "FRONTEND", appliedAt: ago(60 * 2), match: "MATCH", status: "PENDING" },
  { id: "s-2", name: "김한강", generation: 14, email: "hangang@gmail.com", part: "BACKEND", appliedAt: ago(60 * 5), match: "CHECK", note: "DUPLICATE", status: "PENDING" },
  { id: "s-3", name: "이노을", generation: 12, email: "noeul@cau.ac.kr", part: "DESIGN", appliedAt: ago(60 * 26), match: "MATCH", status: "PENDING" },
  { id: "s-4", name: "정바다", generation: 11, email: "bada@naver.com", part: "PLANNING", appliedAt: ago(60 * 50), match: "CHECK", note: "REAPPLY", status: "PENDING" },
  { id: "s-5", name: "최하늘", generation: 13, email: "sky@cau.ac.kr", part: "FRONTEND", appliedAt: ago(60 * 80), match: "MATCH", status: "APPROVED" },
  { id: "s-6", name: "강별", generation: 9, email: "star@gmail.com", part: "BACKEND", appliedAt: ago(60 * 100), match: "CHECK", status: "REJECTED" },
];

export const invites: Invite[] = [
  { id: "i-1", name: "박중앙", generation: 13, email: "jungang@cau.ac.kr", token: "abc123", status: "USED", createdAt: ago(60 * 48), expiresAt: ago(-60 * 24 * 5) },
  { id: "i-2", name: "김한강", generation: 14, token: "def456", status: "UNUSED", createdAt: ago(60 * 24), expiresAt: ago(-60 * 24 * 6) },
  { id: "i-3", name: "이노을", generation: 12, email: "noeul@cau.ac.kr", token: "ghi789", status: "UNUSED", createdAt: ago(60 * 10), expiresAt: ago(-60 * 24 * 6) },
  { id: "i-4", name: "정바다", generation: 11, token: "jkl012", status: "EXPIRED", createdAt: ago(60 * 24 * 10), expiresAt: ago(60 * 24 * 3) },
  { id: "i-5", name: "오하람", generation: 10, token: "mno345", status: "CANCELED", createdAt: ago(60 * 24 * 4), expiresAt: ago(-60 * 24 * 3) },
];
