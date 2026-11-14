/**
 * 프로토타입 모드(NEXT_PUBLIC_API_URL 없음)에서 동문 화면이 쓰는 가짜 API.
 * lib/store.tsx의 mock 동문을 실제 API 응답 모양(lib/api/alumni.ts)으로 바꿔 돌려준다.
 */

import type { Alumni, CoffeeChat } from "@/lib/types";
import { INTEREST_CODE, type InterestLabel } from "@/lib/types";
import { ME_ID } from "@/lib/mock/users";
import { ApiError, type Page } from "../client";
import type { ApiLionPart } from "../profile";
import type { Id } from "../posts";
import type { AlumniCoffeeChat, AlumniQuery, AlumniSummary, BookmarkedAlumni } from "../alumni";
import { viewFromMock, type AlumniProfileView } from "../alumniView";

export interface MockAlumniState {
  alumni: Alumni[];
  bookmarks: string[];
  blocked: string[];
  coffeeChats: CoffeeChat[];
}

const delay = <T,>(value: T) => new Promise<T>((resolve) => setTimeout(() => resolve(value), 120));

/** "11/20부터 받아요" → 올해 날짜 */
function noteDate(note?: string): string | null {
  const m = note && /(\d{1,2})\/(\d{1,2})/.exec(note);
  if (!m) return null;
  const year = new Date().getFullYear();
  return `${year}-${m[1].padStart(2, "0")}-${m[2].padStart(2, "0")}`;
}

function coffeeChat(a: Alumni): AlumniCoffeeChat {
  if (a.receive === "OPEN") return { availability: "AVAILABLE", nextAvailableDate: null, acceptGroup: a.allowGroup };
  if (a.receive === "RESTING") return { availability: "RESTING", nextAvailableDate: noteDate(a.receiveNote), acceptGroup: a.allowGroup };
  return { availability: "NOT_ACCEPTING", nextAvailableDate: a.receive === "SCHEDULED" ? noteDate(a.receiveNote) : null, acceptGroup: a.allowGroup };
}

/** mock 동문 → ALUMNI-01 항목 (홈처럼 아직 mock을 쓰는 화면에서도 쓴다) */
export function summaryFromMock(a: Alumni, bookmarks: string[]): AlumniSummary {
  return {
    id: a.id,
    name: a.name,
    generation: a.generation,
    lionParts: (a.parts ?? [a.part]).filter((p): p is ApiLionPart => p !== "ETC"),
    status: a.status,
    headline: a.headline,
    bio: a.intro,
    photoUrl: null,
    company: a.company ?? null,
    interests: a.interests.map((i) => INTEREST_CODE[i as InterestLabel]).filter(Boolean),
    skills: a.skills,
    bookmarked: bookmarks.includes(a.id),
    coffeeChat: coffeeChat(a),
  };
}

export function mockListAlumni(state: MockAlumniState, query: AlumniQuery): Promise<Page<AlumniSummary>> {
  const me = state.alumni.find((a) => a.id === ME_ID);
  const kw = query.q?.trim().toLowerCase() ?? "";
  let list = state.alumni.filter((a) => {
    const s = summaryFromMock(a, state.bookmarks);
    return (
      a.id !== ME_ID &&
      !state.blocked.includes(a.id) &&
      (!query.available || a.receive === "OPEN") &&
      (!query.generations?.length || query.generations.includes(a.generation)) &&
      (!query.parts?.length || s.lionParts.some((p) => query.parts!.includes(p))) &&
      (!query.statuses?.length || query.statuses.includes(a.status)) &&
      (!query.interests?.length || s.interests.some((i) => query.interests!.includes(i))) &&
      (!kw || `${a.name} ${a.company ?? ""} ${a.role ?? ""} ${a.headline} ${a.skills.join(" ")}`.toLowerCase().includes(kw))
    );
  });
  if (query.sort === "GENERATION") list = [...list].sort((a, b) => a.generation - b.generation);
  else if (query.sort === "LATEST") list = [...list].reverse();
  else list = [...list].sort((a, b) => Number(b.receive === "OPEN") - Number(a.receive === "OPEN") || Number(b.part === me?.part) - Number(a.part === me?.part));
  const page = query.page ?? 0;
  const size = query.size ?? 20;
  return delay({ items: list.slice(page * size, (page + 1) * size).map((a) => summaryFromMock(a, state.bookmarks)), page, size, totalElements: list.length, hasNext: (page + 1) * size < list.length });
}

export function mockGetAlumni(state: MockAlumniState, id: Id): Promise<AlumniProfileView> {
  const person = state.alumni.find((a) => a.id === String(id));
  const me = state.alumni.find((a) => a.id === ME_ID);
  if (!person || !me) return Promise.reject(new ApiError(404, "USER_NOT_FOUND", "동문을 찾을 수 없어요."));
  return delay(viewFromMock(person, me, state.bookmarks, state.coffeeChats));
}

export function mockListBookmarks(state: MockAlumniState, page: number, size: number): Promise<Page<BookmarkedAlumni>> {
  const list = [...state.bookmarks]
    .reverse()
    .map((id) => state.alumni.find((a) => a.id === id))
    .filter((a): a is Alumni => !!a);
  const now = new Date().toISOString();
  return delay({ items: list.slice(page * size, (page + 1) * size).map((a) => ({ ...summaryFromMock(a, state.bookmarks), bookmarkedAt: now })), page, size, totalElements: list.length, hasNext: (page + 1) * size < list.length });
}

export function mockSetBookmark(state: MockAlumniState, toggle: (id: string) => void, id: Id, bookmarked: boolean) {
  if (String(id) === ME_ID) return Promise.reject(new ApiError(400, "CANNOT_BOOKMARK_SELF", "나는 저장할 수 없어요."));
  if (state.bookmarks.includes(String(id)) !== bookmarked) toggle(String(id));
  return delay({ userId: id, bookmarked });
}
