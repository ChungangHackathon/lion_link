"use client";

import { useMemo } from "react";
import { useStore } from "@/lib/store";
import { POPULAR_KEYWORDS } from "@/lib/mock/posts";
import { API_ENABLED, type Page, type Slice } from "./client";
import * as posts from "./posts";
import type { CategoryCounts, Id, LikeResult, MeetupParticipation, PopularKeyword, PostComment, PostDetail, PostFileInput, PostListQuery, PostSummary, PostUpdateRequest, PostWriteRequest, RecruitParticipation, UploadType } from "./posts";
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
  comments: (postId: Id, page?: number, size?: number) => Promise<Slice<PostComment>>;
  addComment: (postId: Id, content: string) => Promise<PostComment>;
  removeComment: (postId: Id, commentId: Id) => Promise<void>;
  setMeetupJoined: (postId: Id, joined: boolean) => Promise<MeetupParticipation>;
  inviteToMeetup: (postId: Id, userIds: Id[]) => Promise<{ sentCount: number; skippedUserIds: Id[] }>;
  joinRecruit: (postId: Id) => Promise<RecruitParticipation>;
  uploadFile: (type: UploadType, file: File) => Promise<PostFileInput>;
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
  comments: posts.listComments,
  addComment: posts.createComment,
  removeComment: posts.deleteComment,
  setMeetupJoined: posts.setMeetupJoined,
  inviteToMeetup: posts.inviteToMeetup,
  joinRecruit: posts.joinRecruit,
  uploadFile: posts.uploadFile,
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
      comments: (postId, page = 0, size = 20) => mock.mockListComments(state, postId, page, size),
      addComment: (postId, content) => mock.mockAddComment(state, store, postId, content),
      removeComment: (postId, commentId) => mock.mockRemoveComment(state, store, postId, commentId),
      setMeetupJoined: (postId, joined) => mock.mockSetMeetupJoined(state, store, postId, joined),
      inviteToMeetup: (postId, userIds) => mock.mockInviteToMeetup(state, postId, userIds),
      joinRecruit: (postId) => mock.mockJoinRecruit(state, store, postId),
      uploadFile: (type, file) => mock.mockUploadFile(type, file),
    };
  }, [store]);
}
