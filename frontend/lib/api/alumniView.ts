/**
 * 동문 프로필 상세 화면이 그리는 모양 (AlumniProfileView).
 * API 응답(AlumniDetail)과 프로토타입 mock 데이터를 각각 이 모양으로 바꿔서, 화면은 한 가지만 다룬다.
 * 본문 섹션은 기존 ProfileSections 컴포넌트가 쓰는 ProfileDetail 모양을 그대로 쓴다.
 */

import type { Alumni, CoffeeChat, LionPart, ReceiveMode } from "@/lib/types";
import { INTEREST_LABEL, LION_PART_LABEL } from "@/lib/types";
import type { ProfileDetail } from "@/lib/mock/profile";
import { profileDetail } from "@/lib/mock/profile";
import { ME_ID } from "@/lib/mock/users";
import { cannotRequestReason, receiveBadge, type AlumniDetail, type CoffeeTopic, type LinkType, type TimeSlot, type Weekday } from "./alumni";
import type { Id } from "./posts";

export interface AlumniProfileView {
  id: Id;
  name: string;
  generation: number;
  part: LionPart;
  photoUrl: string | null;
  /** 이름 옆 '운영진' 태그 문구. 운영진이 아니면 null */
  staffLabel: string | null;
  headline: string;
  intro: string;
  region: string | null;
  bookmarked: boolean;
  commonalities: string[];
  /** 함께 아는 동문 수 (API 모드만) */
  mutualCount: number | null;
  receive: { mode: ReceiveMode; note?: string };
  canRequest: boolean;
  requestBlockedReason: string;
  pendingCoffeeChatId: Id | null;
  coffee: { topics: string[]; acceptGroup: boolean; modes: string; times: string; regions: string };
  /** 헤더 오른쪽 학력 */
  educations: { school: string; major: string; degree: string; from: string; to: string; gpa?: string }[];
  /** 본문 섹션 (ProfileSections) */
  detail: ProfileDetail;
  /** 연결된 동문인지. 'CONNECTED' 공개 항목을 보여줄지 정한다 (API 모드는 서버가 이미 걸러서 보낸다) */
  connected: boolean;
}

// ---------------------------------------------------------------------------
// 라벨
// ---------------------------------------------------------------------------

const TOPIC_LABEL: Record<CoffeeTopic, string> = { JOB_PREP: "취업 준비", CAREER: "커리어·이직", STUDY: "학업·전공", PROJECT_DEV: "프로젝트·개발", STARTUP: "창업", LION_ACTIVITY: "멋사 활동", ETC: "기타" };
const DAY_LABEL: Record<Weekday, string> = { MON: "월", TUE: "화", WED: "수", THU: "목", FRI: "금", SAT: "토", SUN: "일" };
const TIME_LABEL: Record<TimeSlot, string> = { MORNING: "오전", AFTERNOON: "오후", EVENING: "저녁" };
const LINK_LABEL: Record<LinkType, string> = { LINKEDIN: "링크드인", GITHUB: "깃허브", PORTFOLIO: "포트폴리오", BLOG: "블로그", REMEMBER: "리멤버", NOTION: "노션", ETC: "링크" };
const EMPLOYMENT_LABEL: Record<string, string> = { FULL_TIME: "정규직", CONTRACT: "계약직", INTERN: "인턴", FREELANCE: "프리랜서", FOUNDER: "창업", ETC: "기타" };
const DEGREE_LABEL: Record<string, string> = { BACHELOR: "학사", MASTER: "석사", DOCTOR: "박사", EXCHANGE_ETC: "교환·기타" };
const LION_ROLE_LABEL: Record<string, string> = { LION: "아기사자", STAFF: "운영진", PRESIDENT: "대표", PART_LEAD: "파트장", ETC: "기타" };
const EVENT_LABEL: Record<string, string> = { REGULAR: "정기 프로젝트", JUNGKATHON: "중커톤", UNION_HACKATHON: "연합 해커톤", ETC: "기타" };
const LEVEL_LABEL: Record<string, string> = { BASIC: "기초", DAILY: "일상 회화", BUSINESS: "비즈니스", NATIVE: "원어민" };

/** "2024-03" → "2024.03" */
const ym = (v: string | null | undefined) => (v ? v.replace("-", ".") : "");
const stripScheme = (url: string) => url.replace(/^https?:\/\//i, "");

/** "2024-03" ~ 지금/"2025-02" → "2년 9개월" */
function duration(start: string | null, end: string | null, current?: boolean): string {
  if (!start) return "";
  const [sy, sm] = start.split("-").map(Number);
  const now = new Date();
  const [ey, em] = current || !end ? [now.getFullYear(), now.getMonth() + 1] : end.split("-").map(Number);
  const months = (ey - sy) * 12 + (em - sm) + 1;
  if (months <= 0) return "";
  const y = Math.floor(months / 12);
  const m = months % 12;
  return [y && `${y}년`, m && `${m}개월`].filter(Boolean).join(" ");
}

export function slotsText(slots: { day: Weekday; times: TimeSlot[] }[]): string {
  if (!slots.length) return "-";
  return slots.map((s) => `${DAY_LABEL[s.day]} ${s.times.map((t) => TIME_LABEL[t]).join("·")}`).join(", ");
}

// ---------------------------------------------------------------------------
// API → 화면
// ---------------------------------------------------------------------------

export function viewFromApi(d: AlumniDetail): AlumniProfileView {
  const part: LionPart = d.lionParts[0] ?? "ETC";
  const staff = [...d.lionActivities].filter((a) => a.role !== "LION" && a.role !== "ETC").sort((a, b) => b.generation - a.generation)[0];
  const current = d.careers.find((c) => c.isCurrent);
  const badge = receiveBadge(d.coffeeChat);

  const detail: ProfileDetail = {
    location: d.region ?? undefined,
    about: d.bio ?? "",
    educations: [],
    careers: d.careers.map((c) => ({
      company: c.companyHidden ? "비공개 회사" : c.company ?? "",
      role: c.jobTitle,
      type: c.employmentType ? EMPLOYMENT_LABEL[c.employmentType] ?? "" : "",
      from: ym(c.startYm),
      to: c.isCurrent ? "현재" : ym(c.endYm),
      duration: duration(c.startYm, c.endYm, c.isCurrent),
    })),
    lion: [...d.lionActivities]
      .sort((a, b) => a.generation - b.generation)
      .map((a) => ({
        generation: a.generation,
        role: `${a.parts.map((p) => LION_PART_LABEL[p]).join("·")} ${LION_ROLE_LABEL[a.role] ?? ""}`.trim(),
        part: a.parts[0] ?? "ETC",
        projects: d.lionProjects
          .filter((p) => p.generation === a.generation)
          .map((p) => ({ event: `${EVENT_LABEL[p.eventType] ?? ""} · ${p.generation}기`, name: p.name, desc: p.description ?? "", role: p.myRole ?? "", link: p.link ? stripScheme(p.link) : undefined })),
      })),
    projects: d.projects.map((p) => ({ name: p.name, type: p.role ?? "", from: ym(p.startYm), to: p.isCurrent ? "현재" : ym(p.endYm), skills: p.tags ?? [] })),
    repSkills: d.skills,
    skills: [],
    certs: [
      ...d.certifications.map((c) => ({ kind: "자격증" as const, text: [c.name, c.issuer, ym(c.acquiredYm)].filter(Boolean).join(" · ") })),
      ...d.awards.map((a) => ({ kind: "수상" as const, text: [a.name, a.host, ym(a.awardedYm)].filter(Boolean).join(" · ") })),
      ...d.activities.map((a) => ({ kind: "대외활동" as const, text: [a.name, a.organization, a.startYm && `${ym(a.startYm)} ~ ${ym(a.endYm)}`].filter(Boolean).join(" · ") })),
      ...d.languages.map((l) => ({ kind: "언어" as const, text: `${l.language} · ${LEVEL_LABEL[l.level] ?? l.level}` })),
    ],
    // 서버가 공개 범위를 적용해서, 받은 값은 모두 볼 수 있는 값이다. 빠진 값은 '비공개'로 보인다.
    links: (d.links ?? []).map((l) => ({ label: LINK_LABEL[l.type] ?? "링크", url: stripScheme(l.url), visibility: "ALL" as const })),
    email: d.contact.email ? { value: d.contact.email, visibility: "ALL" } : undefined,
    phone: d.contact.phone ? { value: d.contact.phone, visibility: "ALL" } : undefined,
    coffeeTopics: d.coffeeChat.topics.map((t) => TOPIC_LABEL[t]),
    coffeeTimes: slotsText(d.coffeeChat.availableSlots),
    coffeeModes: d.coffeeChat.modes.map((m) => (m === "ONLINE" ? "온라인" : "오프라인")).join(" · ") || "-",
    coffeeRegions: d.coffeeChat.preferredArea ?? "-",
  };

  return {
    id: d.userId,
    name: d.name,
    generation: d.generation,
    part,
    photoUrl: d.photoUrl,
    staffLabel: d.isStaff ? (staff ? `${staff.generation}기 ${staff.parts.map((p) => LION_PART_LABEL[p]).join("·")} ${LION_ROLE_LABEL[staff.role]}` : "운영진") : null,
    headline: d.headline ?? (current ? [current.companyHidden ? null : current.company, current.jobTitle].filter(Boolean).join(" · ") : ""),
    intro: d.bio ?? "",
    region: d.region,
    bookmarked: d.bookmarked,
    commonalities: d.commonalities.slice(0, 3).map((c) => c.label),
    mutualCount: d.mutualCount,
    receive: badge,
    canRequest: d.coffeeChat.availability === "AVAILABLE",
    requestBlockedReason: cannotRequestReason(d.coffeeChat),
    pendingCoffeeChatId: d.coffeeChat.pendingCoffeeChatId,
    coffee: { topics: detail.coffeeTopics, acceptGroup: d.coffeeChat.acceptGroup, modes: detail.coffeeModes, times: detail.coffeeTimes, regions: detail.coffeeRegions },
    educations: d.educations.map((e) => ({ school: e.school, major: e.major ?? "", degree: e.degree ? DEGREE_LABEL[e.degree] ?? "" : "", from: ym(e.startYm), to: e.isCurrent ? "재학 중" : ym(e.endYm) })),
    detail,
    connected: true,
  };
}

// ---------------------------------------------------------------------------
// 프로토타입 mock → 화면
// ---------------------------------------------------------------------------

export function viewFromMock(person: Alumni, me: Alumni, bookmarks: string[], coffeeChats: CoffeeChat[]): AlumniProfileView {
  const detail = profileDetail(person.id);
  const common: string[] = [];
  if (person.generation === me.generation) common.push(`${me.generation}기`);
  if (person.part === me.part) common.push(`${LION_PART_LABEL[me.part]} 파트`);
  person.interests.filter((i) => me.interests.includes(i)).forEach((i) => common.push(i));
  person.skills.filter((s) => me.skills.includes(s)).forEach((s) => common.push(s));
  const pending = coffeeChats.find((c) => c.status === "PENDING" && c.requesterId === ME_ID && c.receiverId === person.id);
  const connected = coffeeChats.some((c) => ["ACCEPTED", "SCHEDULED", "COMPLETED"].includes(c.status) && ((c.receiverId === person.id && c.requesterId === ME_ID) || (c.requesterId === person.id && c.receiverId === ME_ID)));
  const reason = person.receive === "RESTING" ? "지금은 쉬는 중인 동문이에요" : person.receive === "SCHEDULED" ? person.receiveNote ?? "곧 받을 예정이에요" : "커피챗을 받지 않는 동문이에요";
  return {
    id: person.id,
    name: person.name,
    generation: person.generation,
    part: person.part,
    photoUrl: null,
    staffLabel: person.isStaff ? `${person.generation + 1}기 ${LION_PART_LABEL[person.part]} 운영진` : null,
    headline: person.headline,
    intro: person.intro,
    region: detail.location ?? null,
    bookmarked: bookmarks.includes(person.id),
    commonalities: common,
    mutualCount: null,
    receive: { mode: person.receive, note: person.receiveNote },
    canRequest: person.receive === "OPEN",
    requestBlockedReason: reason,
    pendingCoffeeChatId: pending?.id ?? null,
    coffee: { topics: detail.coffeeTopics, acceptGroup: person.allowGroup, modes: detail.coffeeModes, times: detail.coffeeTimes, regions: detail.coffeeRegions },
    educations: detail.educations,
    detail,
    connected,
  };
}

/** 관심 분야 코드 → 한글 */
export const interestLabel = (code: keyof typeof INTEREST_LABEL) => INTEREST_LABEL[code] ?? code;
