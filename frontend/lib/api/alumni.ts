/** 5. 동문 찾기 API (docs/api.md ALUMNI-01 ~ 05, PROFILE-03) */

import type { InterestCode, ProfileStatus, ReceiveMode } from "@/lib/types";
import type { ApiLionPart } from "./profile";
import type { Id } from "./posts";
import { api, type Page } from "./client";

/** 오늘 기준 커피챗 수신 상태. IN_PROGRESS·COOLDOWN은 상세(ALUMNI-02)에서만 온다 */
export type Availability = "AVAILABLE" | "RESTING" | "NOT_ACCEPTING" | "IN_PROGRESS" | "COOLDOWN";
export type AlumniSort = "RECOMMEND" | "GENERATION" | "LATEST";

export interface AlumniCoffeeChat {
  availability: Availability;
  /** 다시 받는 날 (YYYY-MM-DD) */
  nextAvailableDate: string | null;
  /** 여러 명 신청을 받는지 */
  acceptGroup: boolean;
}

/** ALUMNI-01 항목. ALUMNI-03, 홈 추천 사자도 같은 모양 */
export interface AlumniSummary {
  id: Id;
  name: string;
  generation: number;
  lionParts: ApiLionPart[];
  status: ProfileStatus;
  headline: string | null;
  bio: string | null;
  photoUrl: string | null;
  /** 회사 공개 범위로 가려지면 null */
  company: string | null;
  interests: InterestCode[];
  skills: string[];
  bookmarked: boolean;
  coffeeChat: AlumniCoffeeChat;
}

export interface BookmarkedAlumni extends AlumniSummary {
  bookmarkedAt: string;
}

export interface AlumniQuery {
  q?: string;
  generations?: number[];
  parts?: ApiLionPart[];
  statuses?: ProfileStatus[];
  interests?: InterestCode[];
  /** true면 오늘 커피챗을 받는 사자만 */
  available?: boolean;
  sort?: AlumniSort;
  page?: number;
  size?: number;
}

export type CommonalityType = "SAME_LION_PROJECT" | "SAME_COMPANY" | "SAME_GENERATION" | "SAME_PART" | "SHARED_INTEREST";
export type CoffeeTopic = "JOB_PREP" | "CAREER" | "STUDY" | "PROJECT_DEV" | "STARTUP" | "LION_ACTIVITY" | "ETC";
export type LinkType = "LINKEDIN" | "GITHUB" | "PORTFOLIO" | "BLOG" | "REMEMBER" | "NOTION" | "ETC";
export type Weekday = "MON" | "TUE" | "WED" | "THU" | "FRI" | "SAT" | "SUN";
export type TimeSlot = "MORNING" | "AFTERNOON" | "EVENING";

interface Period {
  startYm: string | null;
  endYm: string | null;
  isCurrent?: boolean;
}

/** ALUMNI-02 동문 프로필 상세 (= PROFILE-03). 공개 범위로 가려진 값은 키째 빠진다 */
export interface AlumniDetail {
  userId: Id;
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
  links?: { type: LinkType; url: string }[];
  contact: { email?: string | null; phone?: string | null };
  careers: (Period & { id: Id; company?: string; companyHidden?: boolean; jobTitle: string; employmentType: string | null; description: string | null })[];
  educations: (Period & { id: Id; school: string; major: string | null; degree: string | null; description?: string | null })[];
  lionActivities: { id: Id; generation: number; role: "LION" | "STAFF" | "PRESIDENT" | "PART_LEAD" | "ETC"; parts: ApiLionPart[] }[];
  lionProjects: { id: Id; name: string; eventType: "REGULAR" | "JUNGKATHON" | "UNION_HACKATHON" | "ETC"; generation: number; myRole: string | null; description: string | null; link: string | null }[];
  projects: (Period & { id: Id; name: string; role: string | null; description: string | null; link: string | null; tags?: string[] })[];
  certifications: { id: Id; name: string; issuer: string | null; acquiredYm: string | null }[];
  awards: { id: Id; name: string; host: string | null; awardedYm: string | null; description?: string | null }[];
  activities: { id: Id; name: string; organization: string | null; startYm: string | null; endYm: string | null; description?: string | null }[];
  languages: { id: Id; language: string; level: "BASIC" | "DAILY" | "BUSINESS" | "NATIVE" }[];
  isStaff: boolean;
  coffeeChat: AlumniCoffeeChat & {
    topics: CoffeeTopic[];
    modes: ("ONLINE" | "OFFLINE")[];
    availableSlots: { day: Weekday; times: TimeSlot[] }[];
    preferredArea: string | null;
    /** 내가 이 사자에게 보낸 대기 중 신청 ID */
    pendingCoffeeChatId: Id | null;
  };
  chat: { status: "NONE" | "REQUESTED" | "ACTIVE"; requestAvailableDate: string | null };
  commonalities: { type: CommonalityType; label: string }[];
  mutualCount: number;
  bookmarked: boolean;
}

/** ALUMNI-01 동문 목록·검색 */
export function listAlumni(query: AlumniQuery = {}, signal?: AbortSignal) {
  const join = <T,>(v?: T[]) => (v?.length ? v.join(",") : undefined);
  return api<Page<AlumniSummary>>("/users", {
    query: {
      q: query.q?.trim(),
      generations: join(query.generations),
      parts: join(query.parts),
      statuses: join(query.statuses),
      interests: join(query.interests),
      available: query.available || undefined,
      sort: query.sort,
      page: query.page,
      size: query.size,
    },
    signal,
  });
}

/** ALUMNI-02 동문 프로필 상세 */
export function getAlumni(id: Id) {
  return api<AlumniDetail>(`/users/${id}`);
}

/** ALUMNI-03 관심 동문 목록 (최근 저장순) */
export function listBookmarks(page = 0, size = 20) {
  return api<Page<BookmarkedAlumni>>("/users/me/bookmarks", { query: { page, size } });
}

/** ALUMNI-04 저장 · ALUMNI-05 저장 취소 */
export async function setBookmark(id: Id, bookmarked: boolean): Promise<{ userId: Id; bookmarked: boolean }> {
  if (bookmarked) return api<{ userId: Id; bookmarked: boolean }>(`/users/${id}/bookmark`, { method: "PUT" });
  await api(`/users/${id}/bookmark`, { method: "DELETE" });
  return { userId: id, bookmarked: false };
}

// ---------------------------------------------------------------------------
// 화면 표시 도우미
// ---------------------------------------------------------------------------

const monthDay = (date: string) => {
  const [, m, d] = date.split("-").map(Number);
  return `${m}/${d}`;
};

/** 커피챗 상태 → 배지(AvailabilityBadge) 모드와 문구 */
export function receiveBadge(c: Pick<AlumniCoffeeChat, "availability" | "nextAvailableDate">): { mode: ReceiveMode; note?: string } {
  const next = c.nextAvailableDate ? `${monthDay(c.nextAvailableDate)}부터 받아요` : undefined;
  switch (c.availability) {
    case "AVAILABLE":
    case "IN_PROGRESS":
      return { mode: "OPEN" };
    case "RESTING":
      return { mode: "RESTING", note: next ? `쉬는 중 · ${next}` : undefined };
    case "COOLDOWN":
      return { mode: "SCHEDULED", note: c.nextAvailableDate ? `${monthDay(c.nextAvailableDate)}부터 신청 가능` : undefined };
    default:
      return next ? { mode: "SCHEDULED", note: next } : { mode: "CLOSED" };
  }
}

/** 커피챗 신청을 못 할 때 안내 문구 */
export function cannotRequestReason(c: Pick<AlumniCoffeeChat, "availability" | "nextAvailableDate">): string {
  const next = c.nextAvailableDate ? monthDay(c.nextAvailableDate) : null;
  switch (c.availability) {
    case "RESTING":
      return next ? `지금은 쉬는 중이에요 · ${next}부터 받아요` : "지금은 쉬는 중인 동문이에요";
    case "IN_PROGRESS":
      return "이미 진행 중인 커피챗이 있어요";
    case "COOLDOWN":
      return next ? `${next}부터 다시 신청할 수 있어요` : "지금은 다시 신청할 수 없어요";
    default:
      return next ? `${next}부터 받아요` : "커피챗을 받지 않는 동문이에요";
  }
}

/** 카드 둘째 줄: 한 줄 소개, 없으면 회사 */
export const alumniHeadline = (a: Pick<AlumniSummary, "headline" | "company">) => a.headline ?? a.company ?? "";
