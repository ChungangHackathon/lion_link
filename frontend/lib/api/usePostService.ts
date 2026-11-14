"use client";

import { useMemo } from "react";
import { useStore } from "@/lib/store";
import { POPULAR_KEYWORDS } from "@/lib/mock/posts";
import { API_ENABLED, type Page } from "./client";
import * as posts from "./posts";
import type { CategoryCounts, Id, LikeResult, PopularKeyword, PostDetail, PostListQuery, PostSummary, PostUpdateRequest, PostWriteRequest } from "./posts";
import * as mock from "./mock/posts";

/**
 * 커뮤니티 화면이 쓰는 게시글 API 묶음.
 * API 모드에서는 실제 서버, 프로토타입 모드에서는 mock 게시글을 같은 응답 모양으로 돌려준다.
 */
export interface PostService {
  list: (query?: PostListQuery, signal?: AbortSignal) => Promise<Page<PostSummary>>;
  categoryCounts: () => Promise<CategoryCounts>;
  popularKeywords: (size?: number) => Promise<{ items: PopularKeyword[] }>;
  get: (id: Id) => Promise<PostDetail>;
  create: (body: PostWriteRequest) => Promise<{ id: Id }>;
  update: (id: Id, body: PostUpdateRequest) => Promise<{ id: Id }>;
  remove: (id: Id) => Promise<void>;
  setLike: (id: Id, liked: boolean) => Promise<LikeResult>;
}

const apiService: PostService = {
  list: posts.listPosts,
  categoryCounts: posts.getCategoryCounts,
  popularKeywords: posts.getPopularKeywords,
  get: posts.getPost,
  create: posts.createPost,
  update: posts.updatePost,
  remove: posts.deletePost,
  setLike: posts.setPostLike,
};

export function usePostService(): PostService {
  const store = useStore();
  return useMemo<PostService>(() => {
    if (API_ENABLED) return apiService;
    const state = { posts: store.posts, alumni: store.alumni, blocked: store.blocked, coffeeChats: store.coffeeChats };
    return {
      list: (query = {}) => mock.mockListPosts(state, query),
      categoryCounts: () => mock.mockCategoryCounts(state),
      popularKeywords: (size = 10) => mock.mockPopularKeywords(POPULAR_KEYWORDS, size),
      get: (id) => mock.mockGetPost(state, id),
      create: (body) => mock.mockCreatePost(store, body),
      update: (id, body) => mock.mockUpdatePost(state, store, id, body),
      remove: (id) => mock.mockDeletePost(store, id),
      setLike: (id, liked) => mock.mockSetLike(state, store, id, liked),
    };
  }, [store]);
}
