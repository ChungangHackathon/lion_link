/** 내 프로필 (docs/api.md PROFILE-02, 04 ~ 10) */

import type { InterestCode, ProfileStatus } from "@/lib/types";
import type { AlumniDetail, LinkType } from "./alumni";
import type { ApiLionPart } from "./profile";
import { api } from "./client";

export type Scope = "PUBLIC" | "CONNECTED" | "PRIVATE";

export interface Visibility {
  email: Scope;
  phone: Scope;
  links: Scope;
  /** 회사명은 PUBLIC·PRIVATE만 */
  company: "PUBLIC" | "PRIVATE";
}

type Sections = Pick<AlumniDetail, "careers" | "educations" | "lionActivities" | "lionProjects" | "projects" | "certifications" | "awards" | "activities" | "languages">;

/** PROFILE-02 내 프로필. 공개 범위를 적용하지 않은 전체 내용 */
export interface MyProfile extends Sections {
  userId: number | string;
  name: string;
  generation: number;
  photoUrl: string | null;
  headline: string | null;
  bio: string | null;
  major: string | null;
  minor: string | null;
  region: string | null;
  status: ProfileStatus;
  lionParts: ApiLionPart[];
  interests: InterestCode[];
  skills: string[];
  links: { type: LinkType; url: string }[];
  contact: { email: string | null; phone: string | null };
  visibility: Visibility;
}

export interface BasicInfo {
  headline: string | null;
  major: string | null;
  minor: string | null;
  region: string | null;
  status: ProfileStatus;
  lionParts: ApiLionPart[];
  interests: InterestCode[];
}

/** PROFILE-02 */
export function getMyProfile() {
  return api<MyProfile>("/me/profile");
}

/** PROFILE-04 기본 정보 (통째로 바꾼다. 안 바꾼 칸도 현재 값을 보낸다) */
export function updateBasic(body: BasicInfo) {
  return api<BasicInfo>("/me/profile/basic", { method: "PUT", body });
}

/** PROFILE-05 사진 변경 (jpg·png·webp, 5MB 이하) */
export function uploadPhoto(file: File) {
  const form = new FormData();
  form.append("file", file);
  return api<{ photoUrl: string }>("/me/profile/photo", { method: "PUT", body: form });
}

/** PROFILE-05 사진 삭제 → 기본 사자 이미지 */
export function deletePhoto() {
  return api("/me/profile/photo", { method: "DELETE" });
}

/** PROFILE-06 소개 (0~1000자, 비우면 섹션이 사라진다) */
export function updateBio(bio: string | null) {
  return api<{ bio: string | null }>("/me/profile/bio", { method: "PUT", body: { bio } });
}

/** PROFILE-07 대표 기술 (0~5개, 태그당 1~20자) */
export function updateSkills(skills: string[]) {
  return api<{ skills: string[] }>("/me/profile/skills", { method: "PUT", body: { skills } });
}

/** PROFILE-08 외부 링크 (0~5개) */
export function updateLinks(links: { type: LinkType; url: string }[]) {
  return api<{ links: { type: LinkType; url: string }[] }>("/me/profile/links", { method: "PUT", body: { links } });
}

/** PROFILE-09 연락처 (이메일 필수, 전화번호 010-0000-0000) */
export function updateContact(body: { email: string; phone: string | null }) {
  return api<{ email: string; phone: string | null }>("/me/profile/contact", { method: "PUT", body });
}

/** PROFILE-10 공개 범위 (보낸 키만 바뀐다) */
export function updateVisibility(patch: Partial<Visibility>) {
  return api<{ visibility: Visibility }>("/me/profile/visibility", { method: "PATCH", body: patch });
}

// ---------------------------------------------------------------------------
// 검증·라벨
// ---------------------------------------------------------------------------

export const SCOPE_LABEL: Record<Scope, string> = { PUBLIC: "전체 공개", CONNECTED: "연결된 동문만", PRIVATE: "비공개" };
export const LINK_TYPES: { value: LinkType; label: string }[] = [
  { value: "GITHUB", label: "깃허브" },
  { value: "LINKEDIN", label: "링크드인" },
  { value: "PORTFOLIO", label: "포트폴리오" },
  { value: "BLOG", label: "블로그" },
  { value: "NOTION", label: "노션" },
  { value: "REMEMBER", label: "리멤버" },
  { value: "ETC", label: "기타" },
];
export const linkLabel = (t: LinkType) => LINK_TYPES.find((x) => x.value === t)?.label ?? "링크";

export const isEmail = (v: string) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v);
export const isPhone = (v: string) => /^010-\d{4}-\d{4}$/.test(v);

export const PHOTO_RULE = { exts: ["jpg", "jpeg", "png", "webp"], maxBytes: 5 * 1024 * 1024, accept: ".jpg,.jpeg,.png,.webp" };
export function photoError(file: File): string | null {
  const ext = file.name.split(".").pop()?.toLowerCase() ?? "";
  if (!PHOTO_RULE.exts.includes(ext)) return "jpg·png·webp 사진만 올릴 수 있어요";
  if (file.size > PHOTO_RULE.maxBytes) return "5MB 이하 사진만 올릴 수 있어요";
  return null;
}

/** 프로필 완성도 (2.6, FE가 계산). 기본 30% + 섹션별 가중치 */
export function completionOf(p: Pick<MyProfile, "bio" | "careers" | "educations" | "lionActivities" | "skills" | "links" | "projects">): number {
  let c = 30;
  if (p.bio) c += 14;
  if (p.careers.length) c += 14;
  if (p.educations.length) c += 10;
  if (p.lionActivities.length) c += 10;
  if (p.skills.length) c += 8;
  if (p.links.length) c += 7;
  if (p.projects.length) c += 7;
  return Math.min(100, c);
}
