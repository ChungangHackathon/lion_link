/** 1. 계정·인증 API (docs/api.md AUTH-01 ~ AUTH-09) */

import type { UserRole, UserStatus } from "@/lib/types";
import { api, tokenStore, type TokenResponse } from "./client";

/** AUTH-05 내 정보. AUTH-02·06·07의 user도 이 형식이다. */
export interface Me {
  id: number;
  name: string;
  generation: number;
  email: string;
  role: UserRole;
  status: UserStatus;
  /** false면 프로필 기본 정보 입력(온보딩)으로 보낸다 */
  profileCompleted: boolean;
  /** REJECTED일 때만 값이 있다 */
  rejectReason: string | null;
  /** PENDING일 때만 true */
  signupInfoEditable: boolean;
  photoUrl: string | null;
}

export interface SignupInfo {
  name: string;
  generation: number;
}

export interface SignupRequest extends SignupInfo {
  privacyAgreed: boolean;
}

export interface SignupResponse extends TokenResponse {
  user: Me;
}

export interface GoogleInfo {
  email: string;
  name: string | null;
}

/** AUTH-09 가입 화면 구글 정보 (signupToken 쿠키) */
export function getGoogleInfo() {
  return api<GoogleInfo>("/auth/signup/google-info", { auth: false });
}

/** AUTH-02 가입 신청. 성공하면 바로 로그인된다. */
export async function signup(body: SignupRequest) {
  const res = await api<SignupResponse>("/auth/signup", { method: "POST", body, auth: false });
  tokenStore.set(res.accessToken);
  return res;
}

/** AUTH-04 로그아웃. 서버 호출이 실패해도 저장된 토큰은 지운다. */
export async function logout() {
  tokenStore.clear();
  try {
    await api("/auth/logout", { method: "POST", auth: false });
  } catch {
    // 1.6: 실패해도 로그인 화면으로 보낸다
  }
}

/** AUTH-05 내 정보 조회 */
export function getMe() {
  return api<Me>("/me");
}

/** AUTH-06 가입 정보 수정 (승인 대기만) */
export function updateSignupInfo(body: SignupInfo) {
  return api<Me>("/me/signup-info", { method: "PATCH", body });
}

/** AUTH-07 다시 신청 (반려만) */
export function reapply(body: SignupInfo) {
  return api<Me>("/me/reapply", { method: "POST", body });
}

/** AUTH-08 회원 탈퇴. confirm은 정확히 "탈퇴" */
export function withdraw(confirm: string) {
  return api("/me/withdraw", { method: "POST", body: { confirm } });
}

/** 회원 상태에 맞는 첫 화면 (docs/api.md "회원 상태별 접근") */
export function landingPath(me: Pick<Me, "status" | "profileCompleted">): string {
  if (me.status === "REJECTED") return "/rejected";
  if (!me.profileCompleted) return "/onboarding";
  if (me.status === "PENDING") return "/pending";
  return "/home";
}

/** 구글 로그인 실패 코드(?error=) 안내 문구 */
export function googleErrorMessage(code: string): string {
  if (code === "GOOGLE_UNAVAILABLE") return "구글과 연결이 원활하지 않아요. 잠시 후 다시 시도해주세요.";
  if (code === "SESSION_EXPIRED") return "로그인 정보가 만료됐어요. 다시 로그인해주세요.";
  return "구글 로그인에 실패했어요. 다시 시도해주세요.";
}
