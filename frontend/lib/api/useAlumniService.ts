"use client";

import { useMemo } from "react";
import { useStore } from "@/lib/store";
import { API_ENABLED, type Page } from "./client";
import * as alumni from "./alumni";
import type { AlumniQuery, AlumniSummary, BookmarkedAlumni } from "./alumni";
import { viewFromApi, type AlumniProfileView } from "./alumniView";
import type { Id } from "./posts";
import * as mock from "./mock/alumni";

/**
 * 동문 화면이 쓰는 API 묶음.
 * API 모드에서는 실제 서버, 프로토타입 모드에서는 mock 동문을 같은 응답 모양으로 돌려준다.
 */
export interface AlumniService {
  list: (query?: AlumniQuery, signal?: AbortSignal) => Promise<Page<AlumniSummary>>;
  /** 프로필 상세를 화면 모양(AlumniProfileView)으로 */
  get: (id: Id) => Promise<AlumniProfileView>;
  bookmarks: (page?: number, size?: number) => Promise<Page<BookmarkedAlumni>>;
  setBookmark: (id: Id, bookmarked: boolean) => Promise<{ userId: Id; bookmarked: boolean }>;
}

const apiService: AlumniService = {
  list: alumni.listAlumni,
  get: async (id) => viewFromApi(await alumni.getAlumni(id)),
  bookmarks: alumni.listBookmarks,
  setBookmark: alumni.setBookmark,
};

export function useAlumniService(): AlumniService {
  const store = useStore();
  return useMemo<AlumniService>(() => {
    if (API_ENABLED) return apiService;
    const state = { alumni: store.alumni, bookmarks: store.bookmarks, blocked: store.blocked, coffeeChats: store.coffeeChats };
    return {
      list: (query = {}) => mock.mockListAlumni(state, query),
      get: (id) => mock.mockGetAlumni(state, id),
      bookmarks: (page = 0, size = 20) => mock.mockListBookmarks(state, page, size),
      setBookmark: (id, bookmarked) => mock.mockSetBookmark(state, store.toggleBookmark, id, bookmarked),
    };
  }, [store]);
}
