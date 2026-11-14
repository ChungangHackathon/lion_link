"use client";

import { useMemo } from "react";
import { useStore } from "@/lib/store";
import { profileDetail } from "@/lib/mock/profile";
import { ME_ID } from "@/lib/mock/users";
import { INTEREST_CODE, INTEREST_LABEL, type Alumni, type InterestLabel } from "@/lib/types";
import { API_ENABLED } from "./client";
import type { LinkType } from "./alumni";
import type { ApiLionPart } from "./profile";
import * as my from "./myProfile";
import { ApiError } from "./client";
import { addSectionItem, deleteSectionItem, SECTION_SCHEMA, sortItems, updateSectionItem, type SectionBody, type SectionItem, type SectionKey } from "./profileSections";
import type { Id } from "./posts";
import type { BasicInfo, MyProfile, Visibility } from "./myProfile";

/**
 * 내 프로필 화면이 쓰는 API 묶음.
 * API 모드에서는 실제 서버, 프로토타입 모드에서는 mock 프로필을 같은 응답 모양으로 바꿔 준다.
 */
export interface MyProfileService {
  get: () => Promise<MyProfile>;
  updateBasic: (body: BasicInfo) => Promise<BasicInfo>;
  uploadPhoto: (file: File) => Promise<{ photoUrl: string }>;
  deletePhoto: () => Promise<void>;
  updateBio: (bio: string | null) => Promise<{ bio: string | null }>;
  updateSkills: (skills: string[]) => Promise<{ skills: string[] }>;
  updateLinks: (links: MyProfile["links"]) => Promise<{ links: MyProfile["links"] }>;
  updateContact: (body: { email: string; phone: string | null }) => Promise<{ email: string; phone: string | null }>;
  updateVisibility: (patch: Partial<Visibility>) => Promise<{ visibility: Visibility }>;
  /** PROFILE-11 목록형 섹션 */
  addItem: (section: SectionKey, body: SectionBody) => Promise<SectionItem>;
  updateItem: (section: SectionKey, id: Id, body: SectionBody) => Promise<SectionItem>;
  deleteItem: (section: SectionKey, id: Id) => Promise<void>;
}

const apiService: MyProfileService = {
  get: my.getMyProfile,
  updateBasic: my.updateBasic,
  uploadPhoto: my.uploadPhoto,
  deletePhoto: my.deletePhoto,
  updateBio: my.updateBio,
  updateSkills: my.updateSkills,
  updateLinks: my.updateLinks,
  updateContact: my.updateContact,
  updateVisibility: my.updateVisibility,
  addItem: addSectionItem,
  updateItem: updateSectionItem,
  deleteItem: deleteSectionItem,
};

// ---------------------------------------------------------------------------
// 프로토타입 mock: mock 동문(나) + mock 프로필 상세를 API 모양으로
// ---------------------------------------------------------------------------

const delay = <T,>(value: T) => new Promise<T>((resolve) => setTimeout(() => resolve(value), 120));
const ym = (v: string) => (/^\d{4}\.\d{2}$/.test(v) ? v.replace(".", "-") : null);
const LINK_FROM_LABEL: Record<string, LinkType> = { 깃허브: "GITHUB", 링크드인: "LINKEDIN", 포트폴리오: "PORTFOLIO", 블로그: "BLOG", 노션: "NOTION", 리멤버: "REMEMBER" };
const SCOPE: Record<"ALL" | "CONNECTED" | "ME", my.Scope> = { ALL: "PUBLIC", CONNECTED: "CONNECTED", ME: "PRIVATE" };

let mockProfile: MyProfile | null = null;
let mockSeq = 1000;
const fail = (status: number, code: string, message: string) => Promise.reject(new ApiError(status, code, message));

function buildMock(me: Alumni): MyProfile {
  const d = profileDetail(ME_ID);
  let seq = 1;
  return {
    userId: me.id,
    name: me.name,
    generation: me.generation,
    photoUrl: null,
    headline: me.intro || null,
    bio: d.about || null,
    major: d.educations[0]?.major ?? null,
    minor: null,
    region: d.location ?? null,
    status: me.status,
    lionParts: (me.parts ?? [me.part]).filter((p): p is ApiLionPart => p !== "ETC"),
    interests: me.interests.map((i) => INTEREST_CODE[i as InterestLabel]).filter(Boolean),
    skills: d.repSkills.slice(0, 5),
    links: d.links.map((l) => ({ type: LINK_FROM_LABEL[l.label] ?? "ETC", url: /^https?:\/\//.test(l.url) ? l.url : `https://${l.url}` })),
    contact: { email: d.email?.value ?? null, phone: d.phone?.value ?? null },
    visibility: { email: SCOPE[d.email?.visibility ?? "CONNECTED"], phone: SCOPE[d.phone?.visibility ?? "CONNECTED"], links: SCOPE[d.links[0]?.visibility ?? "ALL"], company: "PUBLIC" },
    careers: d.careers.map((c) => ({ id: seq++, company: c.company, jobTitle: c.role, employmentType: null, startYm: ym(c.from), endYm: c.to === "현재" ? null : ym(c.to), isCurrent: c.to === "현재", description: null })),
    educations: d.educations.map((e) => ({ id: seq++, school: e.school, major: e.major, degree: e.degree === "학사" ? "BACHELOR" : e.degree === "석사" ? "MASTER" : null, startYm: ym(e.from), endYm: ym(e.to), isCurrent: e.to === "재학 중" })),
    lionActivities: d.lion.map((l) => ({ id: seq++, generation: l.generation, role: l.role.includes("운영진") ? ("STAFF" as const) : ("LION" as const), parts: l.part === "ETC" ? [] : [l.part], isDefault: l.generation === me.generation })),
    lionProjects: d.lion.flatMap((l) => l.projects.map((p) => ({ id: seq++, name: p.name, eventType: "ETC" as const, generation: l.generation, myRole: p.role, description: p.desc, link: p.link ? `https://${p.link}` : null }))),
    projects: d.projects.map((p) => ({ id: seq++, name: p.name, role: p.type, description: null, link: null, startYm: ym(p.from), endYm: ym(p.to), tags: p.skills })),
    certifications: d.certs.filter((c) => c.kind === "자격증").map((c) => ({ id: seq++, name: c.text, issuer: null, acquiredYm: null })),
    awards: d.certs.filter((c) => c.kind === "수상").map((c) => ({ id: seq++, name: c.text, host: null, awardedYm: null })),
    activities: d.certs.filter((c) => c.kind === "대외활동").map((c) => ({ id: seq++, name: c.text, organization: null, startYm: null, endYm: null })),
    languages: [],
  };
}

export function useMyProfileService(): MyProfileService {
  const store = useStore();
  return useMemo<MyProfileService>(() => {
    if (API_ENABLED) return apiService;
    const me = store.alumni.find((a) => a.id === ME_ID)!;
    const current = () => (mockProfile ??= buildMock(me));
    const patch = (p: Partial<MyProfile>) => {
      mockProfile = { ...current(), ...p };
      return mockProfile;
    };
    return {
      get: () => delay(structuredClone(current())),
      updateBasic: (body) => {
        patch(body);
        // 아직 mock 동문 데이터를 쓰는 다른 화면(카드·홈)에도 반영한다
        store.updateMe({ intro: body.headline ?? "", status: body.status, part: body.lionParts[0], parts: body.lionParts, interests: body.interests.map((i) => INTEREST_LABEL[i]) });
        return delay(body);
      },
      uploadPhoto: (file) => {
        const photoUrl = URL.createObjectURL(file);
        patch({ photoUrl });
        return delay({ photoUrl });
      },
      deletePhoto: () => {
        patch({ photoUrl: null });
        return delay(undefined);
      },
      updateBio: (bio) => delay({ bio: patch({ bio }).bio }),
      updateSkills: (skills) => {
        const unique = [...new Set(skills)];
        patch({ skills: unique });
        store.updateMe({ skills: unique });
        return delay({ skills: unique });
      },
      updateLinks: (links) => delay({ links: patch({ links }).links }),
      updateContact: (body) => {
        patch({ contact: body });
        return delay(body);
      },
      updateVisibility: (v) => delay({ visibility: patch({ visibility: { ...current().visibility, ...v } }).visibility }),
      addItem: (section, body) => {
        const list = current()[section] as SectionItem[];
        if (list.length >= SECTION_SCHEMA[section].max) return fail(409, "LIMIT_EXCEEDED", `${SECTION_SCHEMA[section].label}은(는) ${SECTION_SCHEMA[section].max}개까지 추가할 수 있어요`);
        if (section === "lionActivities" && list.some((x) => (x as { generation: number }).generation === body.generation)) return fail(409, "DUPLICATE_ITEM", "이미 추가한 기수예요");
        if (section === "languages" && list.some((x) => (x as { language: string }).language === body.language)) return fail(409, "DUPLICATE_ITEM", "이미 추가한 언어예요");
        const item = { endYm: null, ...body, id: mockSeq++ } as unknown as SectionItem;
        patch({ [section]: sortItems([...list, item]) });
        return delay(item);
      },
      updateItem: (section, id, body) => {
        const list = current()[section] as SectionItem[];
        const prev = list.find((x) => x.id === id);
        if (!prev) return fail(404, "NOT_FOUND", "항목을 찾을 수 없어요");
        const item = { endYm: null, ...body, id, ...("isDefault" in prev && prev.isDefault ? { isDefault: true, generation: prev.generation } : {}) } as unknown as SectionItem;
        patch({ [section]: sortItems(list.map((x) => (x.id === id ? item : x))) });
        return delay(item);
      },
      deleteItem: (section, id) => {
        const list = current()[section] as SectionItem[];
        const prev = list.find((x) => x.id === id);
        if (prev && "isDefault" in prev && prev.isDefault) return fail(403, "DEFAULT_ITEM_NOT_DELETABLE", "가입 기수 활동은 지울 수 없어요");
        patch({ [section]: list.filter((x) => x.id !== id) });
        return delay(undefined);
      },
    };
  }, [store]);
}
