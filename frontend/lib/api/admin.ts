/** 관리자 · 가입 승인 (docs/api.md ADMIN-01 ~ 06) */

import type { ProfileStatus } from "@/lib/types";
import type { ApiLionPart } from "./profile";
import type { Id } from "./posts";
import { api, type Page } from "./client";

export type SignupStatus = "PENDING" | "APPROVED" | "REJECTED";
export type RejectReasonType = "UNVERIFIED" | "MISMATCH" | "ETC";

export interface SignupItem {
  userId: Id;
  /** 가입할 때 본인이 입력한 이름·기수 */
  name: string;
  generation: number;
  email: string;
  /** 구글 계정 이름 (입력 이름과 대조용) */
  googleName: string | null;
  signedUpAt: string;
  lionParts: ApiLionPart[];
  currentStatus: ProfileStatus | null;
  /** 현재 재직 중 경력의 회사명 (비공개여도 관리자에게는 보인다) */
  company: string | null;
  /** 이미 승인된 회원 중 같은 이름·기수가 있음 */
  sameNameGeneration: boolean;
  /** 반려된 뒤 다시 신청함 */
  reapplied: boolean;
  reapplyAt: string | null;
  /** 반려됨 탭이거나 재신청한 사람의 (이전) 반려 사유 */
  rejectReason: string | null;
  reviewedAt: string | null;
  reviewedBy: string | null;
}

export interface BatchResult {
  succeeded: { userId: Id; name: string; status: "APPROVED" }[];
  failed: { userId: Id; code: "ALREADY_PROCESSED" | "NOT_FOUND" }[];
}

/** 반려 사유 (고정 문구는 서버가 저장한다) */
export const REJECT_REASONS: { type: RejectReasonType; label: string }[] = [
  { type: "UNVERIFIED", label: "동문인지 확인할 수 없어요" },
  { type: "MISMATCH", label: "이름·기수가 맞지 않아요" },
  { type: "ETC", label: "기타" },
];

/** ADMIN-01 탭별 가입 목록 */
export function listSignups(status: SignupStatus, page = 0, size = 20, signal?: AbortSignal) {
  return api<Page<SignupItem>>("/admin/signups", { query: { status, page, size }, signal });
}

/** ADMIN-02 승인 대기 건수 */
export function getPendingCount(signal?: AbortSignal) {
  return api<{ pending: number }>("/admin/signups/pending-count", { signal });
}

/** ADMIN-03 승인 (반려됨 → 승인으로 바꾸기도 같다) */
export function approveSignup(userId: Id) {
  return api<{ userId: Id; name: string; status: "APPROVED"; reviewedAt: string }>(`/admin/signups/${userId}/approve`, { method: "POST" });
}

/** ADMIN-04 반려 */
export function rejectSignup(userId: Id, reasonType: RejectReasonType, reasonText?: string) {
  return api<{ userId: Id; status: "REJECTED"; rejectReason: string }>(`/admin/signups/${userId}/reject`, { method: "POST", body: reasonType === "ETC" ? { reasonType, reasonText } : { reasonType } });
}

/** ADMIN-05 선택 승인 (1~50명). 일부가 실패해도 나머지는 승인된다 */
export function approveSignups(userIds: Id[]) {
  return api<BatchResult>("/admin/signups/approve-batch", { method: "POST", body: { userIds } });
}

/** ADMIN-06 이름·기수 수정 */
export function updateSignupInfo(userId: Id, body: { name: string; generation: number }) {
  return api<{ userId: Id; name: string; generation: number }>(`/admin/signups/${userId}/signup-info`, { method: "PATCH", body });
}
