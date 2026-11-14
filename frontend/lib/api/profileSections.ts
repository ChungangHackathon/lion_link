/**
 * 프로필 목록형 섹션 (docs/api.md PROFILE-11).
 * 경력·학력·멋사 활동·멋사 프로젝트·프로젝트·자격증·수상·대외활동·언어는 경로 모양이 모두 같아서
 * 섹션마다 '입력 칸 목록(SECTION_SCHEMA)'만 정의하고, 화면은 이걸 보고 폼을 그린다.
 */

import { LION_PART_LABEL } from "@/lib/types";
import { api } from "./client";
import type { Id } from "./posts";
import type { MyProfile } from "./myProfile";
import { isHttpUrl, normalizeLink, type ApiLionPart } from "./profile";

export type SectionKey = "careers" | "educations" | "lionActivities" | "lionProjects" | "projects" | "certifications" | "awards" | "activities" | "languages";
export type SectionItem<K extends SectionKey = SectionKey> = MyProfile[K][number];
/** 서버로 보내는 항목 1개 (id 없음) */
export type SectionBody = Record<string, string | number | boolean | string[] | null>;

const PATH: Record<SectionKey, string> = {
  careers: "careers",
  educations: "educations",
  lionActivities: "lion-activities",
  lionProjects: "lion-projects",
  projects: "projects",
  certifications: "certifications",
  awards: "awards",
  activities: "activities",
  languages: "languages",
};

export function addSectionItem<K extends SectionKey>(section: K, body: SectionBody) {
  return api<SectionItem<K>>(`/me/${PATH[section]}`, { method: "POST", body });
}
export function updateSectionItem<K extends SectionKey>(section: K, id: Id, body: SectionBody) {
  return api<SectionItem<K>>(`/me/${PATH[section]}/${id}`, { method: "PUT", body });
}
export async function deleteSectionItem(section: SectionKey, id: Id): Promise<void> {
  await api(`/me/${PATH[section]}/${id}`, { method: "DELETE" });
}

// ---------------------------------------------------------------------------
// 입력 칸 정의
// ---------------------------------------------------------------------------

type Options = readonly (readonly [string, string])[];
export type Field =
  | { key: string; label: string; kind: "text" | "textarea" | "url"; required?: boolean; max?: number; placeholder?: string }
  | { key: string; label: string; kind: "select"; required?: boolean; options: Options }
  | { key: string; label: string; kind: "generation"; required?: boolean }
  /** "2024-03" 형식 년월. end는 시작보다 빠를 수 없다 */
  | { key: string; label: string; kind: "ym"; required?: boolean }
  /** 진행 중 체크. 켜면 endYm은 보내지 않는다 */
  | { key: "isCurrent"; label: string; kind: "current" }
  | { key: "parts"; label: string; kind: "parts" }
  | { key: "tags"; label: string; kind: "tags"; max: number };

export const EMPLOYMENT_OPTIONS = [["FULL_TIME", "정규직"], ["CONTRACT", "계약직"], ["INTERN", "인턴"], ["FREELANCE", "프리랜서"], ["FOUNDER", "창업"], ["ETC", "기타"]] as const;
const DEGREE_OPTIONS = [["BACHELOR", "학사"], ["MASTER", "석사"], ["DOCTOR", "박사"], ["EXCHANGE_ETC", "교환·기타"]] as const;
const LION_ROLE_OPTIONS = [["LION", "아기사자"], ["STAFF", "운영진"], ["PRESIDENT", "대표·부대표"], ["PART_LEAD", "파트장"], ["ETC", "기타"]] as const;
const EVENT_OPTIONS = [["REGULAR", "정기 프로젝트"], ["JUNGKATHON", "중커톤"], ["UNION_HACKATHON", "연합 해커톤"], ["ETC", "기타"]] as const;
const LEVEL_OPTIONS = [["BASIC", "기초"], ["DAILY", "일상 회화"], ["BUSINESS", "비즈니스"], ["NATIVE", "원어민"]] as const;
export const LION_PARTS: ApiLionPart[] = ["PLANNING", "DESIGN", "FRONTEND", "BACKEND"];
export const MAX_GENERATION = 14;

export interface SectionSchema {
  label: string;
  max: number;
  fields: Field[];
  /** 목록에서 한 줄로 보여줄 내용 */
  summary: (item: SectionItem) => { title: string; sub: string };
}

const label = (opts: Options, v: unknown) => opts.find(([k]) => k === v)?.[1] ?? "";
const ymText = (v: unknown) => (typeof v === "string" && v ? v.replace("-", ".") : "");
const period = (it: { startYm?: string | null; endYm?: string | null; isCurrent?: boolean }, now = "현재") => (it.startYm ? `${ymText(it.startYm)} ~ ${it.isCurrent ? now : ymText(it.endYm)}` : "");
const join = (...xs: (string | null | undefined | false)[]) => xs.filter(Boolean).join(" · ");

/* eslint-disable @typescript-eslint/no-explicit-any */
export const SECTION_SCHEMA: Record<SectionKey, SectionSchema> = {
  careers: {
    label: "경력",
    max: 10,
    fields: [
      { key: "company", label: "회사명", kind: "text", required: true, max: 30, placeholder: "예) 핀테크 스타트업" },
      { key: "jobTitle", label: "직무", kind: "text", required: true, max: 30, placeholder: "예) 백엔드 개발자" },
      { key: "employmentType", label: "고용 형태", kind: "select", required: true, options: EMPLOYMENT_OPTIONS },
      { key: "startYm", label: "시작 년월", kind: "ym", required: true },
      { key: "endYm", label: "종료 년월", kind: "ym" },
      { key: "isCurrent", label: "현재 재직 중", kind: "current" },
      { key: "description", label: "설명", kind: "textarea", max: 300 },
    ],
    summary: (c: any) => ({ title: c.company ?? "", sub: join(c.jobTitle, label(EMPLOYMENT_OPTIONS, c.employmentType), period(c) || "시작 년월을 입력해주세요") }),
  },
  educations: {
    label: "학력",
    max: 5,
    fields: [
      { key: "school", label: "학교", kind: "text", required: true, max: 30, placeholder: "예) 중앙대학교" },
      { key: "major", label: "전공", kind: "text", max: 30 },
      { key: "degree", label: "학위", kind: "select", options: DEGREE_OPTIONS },
      { key: "startYm", label: "입학 년월", kind: "ym" },
      { key: "endYm", label: "졸업 년월", kind: "ym" },
      { key: "isCurrent", label: "재학 중", kind: "current" },
      { key: "description", label: "설명", kind: "textarea", max: 200 },
    ],
    summary: (e: any) => ({ title: e.school, sub: join(e.major, label(DEGREE_OPTIONS, e.degree), period(e, "재학 중")) }),
  },
  lionActivities: {
    label: "멋사 활동",
    max: 6,
    fields: [
      { key: "generation", label: "기수", kind: "generation", required: true },
      { key: "role", label: "역할", kind: "select", required: true, options: LION_ROLE_OPTIONS },
      { key: "parts", label: "파트 (1~2개)", kind: "parts" },
    ],
    summary: (a: any) => ({ title: `${a.generation}기 ${label(LION_ROLE_OPTIONS, a.role)}`, sub: join((a.parts as ApiLionPart[]).map((p) => LION_PART_LABEL[p]).join("·"), a.isDefault && "가입 기수") }),
  },
  lionProjects: {
    label: "멋사 프로젝트·해커톤",
    max: 5,
    fields: [
      { key: "name", label: "프로젝트 이름", kind: "text", required: true, max: 30 },
      { key: "eventType", label: "종류", kind: "select", required: true, options: EVENT_OPTIONS },
      { key: "generation", label: "기수", kind: "generation", required: true },
      { key: "myRole", label: "내 역할", kind: "text", max: 30 },
      { key: "description", label: "설명", kind: "textarea", max: 200 },
      { key: "link", label: "링크", kind: "url", placeholder: "github.com/…" },
    ],
    summary: (p: any) => ({ title: p.name, sub: join(label(EVENT_OPTIONS, p.eventType), `${p.generation}기`, p.myRole) }),
  },
  projects: {
    label: "프로젝트",
    max: 10,
    fields: [
      { key: "name", label: "프로젝트 이름", kind: "text", required: true, max: 30 },
      { key: "role", label: "역할", kind: "text", max: 30 },
      { key: "startYm", label: "시작 년월", kind: "ym" },
      { key: "endYm", label: "종료 년월", kind: "ym" },
      { key: "isCurrent", label: "진행 중", kind: "current" },
      { key: "description", label: "설명", kind: "textarea", max: 300 },
      { key: "link", label: "링크", kind: "url", placeholder: "github.com/…" },
      { key: "tags", label: "사용 기술", kind: "tags", max: 5 },
    ],
    summary: (p: any) => ({ title: p.name, sub: join(p.role, period(p)) }),
  },
  certifications: {
    label: "자격증",
    max: 10,
    fields: [
      { key: "name", label: "자격증 이름", kind: "text", required: true, max: 30 },
      { key: "issuer", label: "발급 기관", kind: "text", max: 30 },
      { key: "acquiredYm", label: "취득 년월", kind: "ym" },
    ],
    summary: (c: any) => ({ title: c.name, sub: join(c.issuer, ymText(c.acquiredYm)) }),
  },
  awards: {
    label: "수상",
    max: 10,
    fields: [
      { key: "name", label: "수상 이름", kind: "text", required: true, max: 30 },
      { key: "host", label: "주최", kind: "text", max: 30 },
      { key: "awardedYm", label: "수상 년월", kind: "ym" },
      { key: "description", label: "설명", kind: "textarea", max: 100 },
    ],
    summary: (a: any) => ({ title: a.name, sub: join(a.host, ymText(a.awardedYm)) }),
  },
  activities: {
    label: "대외활동",
    max: 10,
    fields: [
      { key: "name", label: "활동 이름", kind: "text", required: true, max: 30 },
      { key: "organization", label: "기관·단체", kind: "text", max: 30 },
      { key: "startYm", label: "시작 년월", kind: "ym" },
      { key: "endYm", label: "종료 년월", kind: "ym" },
      { key: "description", label: "설명", kind: "textarea", max: 200 },
    ],
    summary: (a: any) => ({ title: a.name, sub: join(a.organization, period(a)) }),
  },
  languages: {
    label: "언어",
    max: 5,
    fields: [
      { key: "language", label: "언어", kind: "text", required: true, max: 20, placeholder: "예) 영어" },
      { key: "level", label: "수준", kind: "select", required: true, options: LEVEL_OPTIONS },
    ],
    summary: (l: any) => ({ title: l.language, sub: label(LEVEL_OPTIONS, l.level) }),
  },
};
/* eslint-enable @typescript-eslint/no-explicit-any */

// ---------------------------------------------------------------------------
// 폼 값 ↔ 항목
// ---------------------------------------------------------------------------

/** 폼이 들고 있는 값. 글자는 string, 체크는 boolean, 파트는 string[] */
export type FormValues = Record<string, string | boolean | string[]>;

/** 이번 달 "YYYY-MM" (한국 시간) */
export function thisMonth(now = new Date()): string {
  const kst = new Date(now.getTime() + 9 * 3600_000);
  return `${kst.getUTCFullYear()}-${String(kst.getUTCMonth() + 1).padStart(2, "0")}`;
}

export function emptyValues(section: SectionKey, defaults: { generation?: number } = {}): FormValues {
  const v: FormValues = {};
  for (const f of SECTION_SCHEMA[section].fields) {
    if (f.kind === "current") v[f.key] = false;
    else if (f.kind === "parts") v[f.key] = [];
    else if (f.kind === "select" && f.required) v[f.key] = f.options[0][0];
    else if (f.kind === "generation") v[f.key] = defaults.generation ? String(defaults.generation) : "";
    else v[f.key] = "";
  }
  return v;
}

export function valuesFromItem(section: SectionKey, item: SectionItem): FormValues {
  const src = item as unknown as Record<string, unknown>;
  const v = emptyValues(section);
  for (const f of SECTION_SCHEMA[section].fields) {
    const x = src[f.key];
    if (f.kind === "current") v[f.key] = !!x;
    else if (f.kind === "parts") v[f.key] = Array.isArray(x) ? (x as string[]) : [];
    else if (f.kind === "tags") v[f.key] = Array.isArray(x) ? (x as string[]).join(", ") : "";
    else v[f.key] = x == null ? "" : String(x);
  }
  return v;
}

/** 받침에 따라 '을'/'를'을 붙인다 */
function eulReul(word: string): string {
  const code = word.charCodeAt(word.length - 1) - 0xac00;
  return word + (code >= 0 && code <= 11171 && code % 28 !== 0 ? "을" : "를");
}

const tagsOf = (v: string) => [...new Set(v.split(",").map((t) => t.trim()).filter(Boolean))];

/** 칸마다 에러 문구. 비어 있으면 저장할 수 있다 */
export function validate(section: SectionKey, v: FormValues, now = thisMonth()): Record<string, string> {
  const errors: Record<string, string> = {};
  const fields = SECTION_SCHEMA[section].fields;
  const hasCurrent = fields.some((f) => f.kind === "current");
  const current = hasCurrent && v.isCurrent === true;
  for (const f of fields) {
    const raw = v[f.key];
    const s = typeof raw === "string" ? raw.trim() : "";
    if (f.kind === "parts") {
      const n = (raw as string[]).length;
      if (n < 1 || n > 2) errors[f.key] = "파트를 1~2개 골라주세요";
      continue;
    }
    if (f.kind === "current") continue;
    if (f.kind === "ym" && f.key === "endYm" && current) continue;
    if ("required" in f && f.required && !s) errors[f.key] = `${eulReul(f.label.replace(/ \(.*\)$/, ""))} 입력해주세요`;
    else if ((f.kind === "text" || f.kind === "textarea") && f.max && s.length > f.max) errors[f.key] = `${f.max}자 이내로 적어주세요`;
    else if (f.kind === "url" && s && !isHttpUrl(normalizeLink(s) ?? "")) errors[f.key] = "주소를 확인해주세요";
    else if (f.kind === "tags" && tagsOf(s).length > f.max) errors[f.key] = `${f.max}개까지 적을 수 있어요`;
    else if (f.kind === "ym" && s && f.key !== "endYm" && s > now) errors[f.key] = "이번 달까지만 고를 수 있어요";
  }
  const start = typeof v.startYm === "string" ? v.startYm : "";
  const end = typeof v.endYm === "string" ? v.endYm : "";
  if (!current && start && end && end < start) errors.endYm = "종료가 시작보다 빠를 수 없어요";
  if (section === "careers" && !current && !end && !errors.endYm) errors.endYm = "종료 년월을 고르거나 '현재 재직 중'을 체크해주세요";
  return errors;
}

/** 폼 값 → 요청 본문. 빈 칸은 null, 진행 중이면 endYm을 빼고 보낸다 */
export function toBody(section: SectionKey, v: FormValues): SectionBody {
  const body: SectionBody = {};
  const current = v.isCurrent === true;
  for (const f of SECTION_SCHEMA[section].fields) {
    const raw = v[f.key];
    if (f.kind === "current") body.isCurrent = current;
    else if (f.kind === "parts") body.parts = raw as string[];
    else if (f.kind === "tags") body.tags = tagsOf(raw as string);
    else if (f.kind === "generation") body[f.key] = Number(raw);
    else if (f.key === "endYm" && current) continue;
    else {
      const s = (raw as string).trim();
      body[f.key] = !s ? null : f.kind === "url" ? normalizeLink(s) ?? null : s;
    }
  }
  return body;
}

/** 목록 정렬: 진행 중이 맨 위, 그다음 날짜 최신순 (서버와 같은 규칙. mock에서 쓴다) */
export function sortItems<T>(items: T[]): T[] {
  const date = (x: T) => {
    const r = x as Record<string, unknown>;
    return String(r.startYm ?? r.acquiredYm ?? r.awardedYm ?? (typeof r.generation === "number" ? String(r.generation).padStart(4, "0") : ""));
  };
  return [...items].sort((a, b) => Number(!!(b as Record<string, unknown>).isCurrent) - Number(!!(a as Record<string, unknown>).isCurrent) || date(b).localeCompare(date(a)));
}
