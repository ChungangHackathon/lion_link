/** 2. 프로필 API (docs/api.md PROFILE-01 ~ PROFILE-15) */

import type { InterestCode, LionPart, ProfileStatus } from "@/lib/types";
import type { Me } from "./auth";
import { api } from "./client";

/** API의 멋사 파트. 프론트의 LionPart와 달리 기타(ETC)가 없다 */
export type ApiLionPart = Exclude<LionPart, "ETC">;

export interface StaffActivity {
  generation: number;
  parts: ApiLionPart[];
}

export interface OnboardingRequest {
  lionParts: ApiLionPart[];
  status: ProfileStatus;
  interests: InterestCode[];
  /** EMPLOYED일 때 필수, 1~30자 */
  company?: string;
  /** EMPLOYED일 때 필수, 1~30자 */
  jobTitle?: string;
  /** http(s) URL */
  link?: string;
  /** 한 줄 소개, 0~100자 */
  headline?: string;
  staffActivities?: StaffActivity[];
}

/** PROFILE-01 기본 정보 입력(온보딩). 응답은 profileCompleted=true인 내 정보 */
export function submitOnboarding(body: OnboardingRequest) {
  return api<Me>("/me/profile/onboarding", { method: "POST", body });
}

/** PROFILE-02 내 프로필 조회 (지금은 동문 찾기의 '나와 같은 파트'에 필요한 값만 쓴다) */
export function getMyProfileParts() {
  return api<{ lionParts: ApiLionPart[] }>("/me/profile");
}

/** PROFILE-12 커피챗 받기 설정 조회 (지금은 거절 창의 '당분간 쉬기'에 필요한 값만 쓴다) */
export function getCoffeeDefaultMode() {
  return api<{ defaultMode: "ON" | "OFF" }>("/me/coffee-settings");
}

/** PROFILE-15 기간 추가. 기본이 ON이면 쉬는 기간, OFF면 받는 기간. mergeOverlap이면 겹치는 기간과 합친다 */
export function addCoffeePeriod(body: { startDate: string; endDate: string; mergeOverlap?: boolean }) {
  return api<{ id: number; startDate: string; endDate: string }>("/me/coffee-periods", { method: "POST", body });
}

/** 입력한 링크에 http(s)://가 없으면 https://를 붙인다. 비어 있으면 undefined */
export function normalizeLink(value: string): string | undefined {
  const v = value.trim();
  if (!v) return undefined;
  return /^https?:\/\//i.test(v) ? v : `https://${v}`;
}

/** 서버 검증(@HttpUrl)과 같은 규칙: http(s)://로 시작, 공백 없음, 500자 이하 */
export function isHttpUrl(value: string): boolean {
  return value.length <= 500 && /^https?:\/\/\S+$/i.test(value);
}
