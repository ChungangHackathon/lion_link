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
