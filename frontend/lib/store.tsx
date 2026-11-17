"use client";

import { createContext, useCallback, useContext, useMemo, useState } from "react";
import type { Alumni, AppNotification, ChatRoom, CoffeeChat, CoffeeChatRejectReason, Comment, Invite, PendingSignup, Post, ReceiveMode } from "@/lib/types";
import { alumni as seedAlumni, ME_ID } from "@/lib/mock/users";
import { coffeeChats as seedCoffee } from "@/lib/mock/coffee";
import { chatRooms as seedRooms } from "@/lib/mock/chat";
import { posts as seedPosts } from "@/lib/mock/posts";
import { notifications as seedNoti, pendingSignups as seedSignups, invites as seedInvites } from "@/lib/mock/notifications";

/**
 * In-memory app state for the prototype. Every flow in the wireframe mutates
 * this store so screens stay consistent (e.g. 신청 보내기 → 내 커피챗 보낸 신청).
 * Swap the actions for API calls when the backend is wired up.
 */
export interface CoffeeSettings {
  mode: ReceiveMode;
  modes: ("ONLINE" | "OFFLINE")[];
  times: string;
  regions: string;
  keywords: string[];
  allowGroup: boolean;
  restUntil?: string;
  openFrom?: string;
  periods: { id: string; from: string; to: string; kind: "REST" | "OPEN" }[];
}

interface State {
  alumni: Alumni[];
  coffeeChats: CoffeeChat[];
  rooms: ChatRoom[];
  posts: Post[];
  notifications: AppNotification[];
  signups: PendingSignup[];
  invites: Invite[];
  bookmarks: string[];
  blocked: string[];
  settings: CoffeeSettings;
  notiSettings: { emailCoffee: boolean; emailMessage: boolean; emailApproved: boolean; push: boolean };
}

const initial: State = {
  alumni: seedAlumni,
  coffeeChats: seedCoffee,
  rooms: seedRooms,
  posts: seedPosts,
  notifications: seedNoti,
  signups: seedSignups,
  invites: seedInvites,
  bookmarks: ["u-1", "u-2", "u-4"],
  blocked: [],
  settings: {
    mode: "OPEN",
    modes: ["ONLINE", "OFFLINE"],
    times: "평일 저녁 · 토요일 오후",
    regions: "강남, 판교",
    keywords: ["프로젝트·개발", "커리어·이직", "취업"],
    allowGroup: true,
    periods: [],
  },
  notiSettings: { emailCoffee: true, emailMessage: true, emailApproved: true, push: true },
};

const uid = () => Math.random().toString(36).slice(2, 9);
const now = () => new Date().toISOString();

function buildActions(set: React.Dispatch<React.SetStateAction<State>>) {
  return {
    toggleBookmark: (id: string) => set((s) => ({ ...s, bookmarks: s.bookmarks.includes(id) ? s.bookmarks.filter((x) => x !== id) : [...s.bookmarks, id] })),
    block: (id: string) => set((s) => ({ ...s, blocked: [...s.blocked, id], bookmarks: s.bookmarks.filter((x) => x !== id) })),
    unblock: (id: string) => set((s) => ({ ...s, blocked: s.blocked.filter((x) => x !== id) })),

    /** R-01 신청 보내기 */
    createCoffeeChat: (input: Omit<CoffeeChat, "id" | "status" | "createdAt" | "requesterId">) => {
      const id = `c-${uid()}`;
      const expires = new Date();
      expires.setDate(expires.getDate() + 7);
      set((s) => ({ ...s, coffeeChats: [{ ...input, id, status: "PENDING", requesterId: ME_ID, createdAt: now(), expiresAt: expires.toISOString() }, ...s.coffeeChats] }));
      return id;
    },
    /** D-10 수락 (일정 후보) → 채팅방 열림 */
    acceptCoffeeChat: (id: string, candidates: string[]) => {
      const roomId = `r-${uid()}`;
      set((s) => {
        const c = s.coffeeChats.find((x) => x.id === id);
        if (!c) return s;
        const members = [c.receiverId, c.requesterId, ...c.memberIds];
        const room: ChatRoom = {
          id: roomId,
          type: members.length > 2 ? "GROUP" : "DIRECT",
          status: "ACTIVE",
          title: members.length > 2 ? `${c.topic} 커피챗` : undefined,
          memberIds: members,
          coffeeChatId: id,
          unread: 0,
          messages: [{ id: uid(), senderId: "system", text: "커피챗이 수락되어 채팅방이 열렸어요. 일정을 정해보세요.", at: now() }],
        };
        return { ...s, rooms: [room, ...s.rooms], coffeeChats: s.coffeeChats.map((x) => (x.id === id ? { ...x, status: "ACCEPTED", candidates, roomId } : x)) };
      });
      return roomId;
    },
    rejectCoffeeChat: (id: string, reason: CoffeeChatRejectReason, note?: string) => set((s) => ({ ...s, coffeeChats: s.coffeeChats.map((x) => (x.id === id ? { ...x, status: "REJECTED", rejectReason: reason, rejectNote: note } : x)) })),
    cancelCoffeeChat: (id: string, note?: string) => set((s) => ({ ...s, coffeeChats: s.coffeeChats.map((x) => (x.id === id ? { ...x, status: "CANCELED", rejectNote: note } : x)) })),
    scheduleCoffeeChat: (id: string, at: string) =>
      set((s) => ({
        ...s,
        coffeeChats: s.coffeeChats.map((x) => (x.id === id ? { ...x, status: "SCHEDULED", scheduledAt: at } : x)),
        rooms: s.rooms.map((r) => (r.coffeeChatId === id ? { ...r, messages: [...r.messages, { id: uid(), senderId: "system", text: `일정이 확정됐어요. ${new Date(at).toLocaleString("ko-KR", { month: "numeric", day: "numeric", hour: "numeric", minute: "2-digit" })}`, at: now() }] } : r)),
      })),
    completeCoffeeChat: (id: string, thanks?: string) =>
      set((s) => ({
        ...s,
        coffeeChats: s.coffeeChats.map((x) => (x.id === id ? { ...x, status: "COMPLETED" } : x)),
        rooms: s.rooms.map((r) => (r.coffeeChatId === id && thanks ? { ...r, messages: [...r.messages, { id: uid(), senderId: ME_ID, text: thanks, at: now() }] } : r)),
      })),

    /** T-04 메시지 요청 / T-17 메시지 */
    sendMessage: (roomId: string, text: string) => set((s) => ({ ...s, rooms: s.rooms.map((r) => (r.id === roomId ? { ...r, messages: [...r.messages, { id: uid(), senderId: ME_ID, text, at: now() }] } : r)) })),
    createMessageRequest: (toId: string, text: string) => {
      const id = `r-${uid()}`;
      set((s) => ({ ...s, rooms: [{ id, type: "DIRECT", status: "REQUESTED", isRequest: true, memberIds: [ME_ID, toId], unread: 0, messages: [{ id: uid(), senderId: ME_ID, text, at: now() }] }, ...s.rooms] }));
      return id;
    },
    acceptMessageRequest: (roomId: string) => set((s) => ({ ...s, rooms: s.rooms.map((r) => (r.id === roomId ? { ...r, status: "ACTIVE", isRequest: false, messages: [...r.messages, { id: uid(), senderId: "system", text: "메시지 요청을 수락했어요. 이제 대화할 수 있어요.", at: now() }] } : r)) })),
    declineMessageRequest: (roomId: string, reason: string) => set((s) => ({ ...s, rooms: s.rooms.map((r) => (r.id === roomId ? { ...r, status: "DECLINED", messages: [...r.messages, { id: uid(), senderId: "system", text: `요청이 거절됐어요. 사유: ${reason}`, at: now() }] } : r)) })),
    editRequestMessage: (roomId: string, text: string) => set((s) => ({ ...s, rooms: s.rooms.map((r) => (r.id === roomId ? { ...r, messages: r.messages.map((m, i) => (i === 0 ? { ...m, text } : m)) } : r)) })),
    cancelMessageRequest: (roomId: string) => set((s) => ({ ...s, rooms: s.rooms.map((r) => (r.id === roomId ? { ...r, status: "CANCELED", messages: r.messages.slice(1) } : r)) })),
    leaveRoom: (roomId: string) => set((s) => ({ ...s, rooms: s.rooms.filter((r) => r.id !== roomId) })),
    markRoomRead: (roomId: string) => set((s) => ({ ...s, rooms: s.rooms.map((r) => (r.id === roomId && r.unread ? { ...r, unread: 0 } : r)) })),

    /** B-05 글쓰기 */
    createPost: (input: Omit<Post, "id" | "views" | "likes" | "comments" | "createdAt" | "authorId">) => {
      const id = `p-${uid()}`;
      set((s) => ({ ...s, posts: [{ ...input, id, authorId: ME_ID, views: 0, likes: 0, comments: [], createdAt: now() }, ...s.posts] }));
      return id;
    },
    updatePost: (id: string, patch: Partial<Post>) => set((s) => ({ ...s, posts: s.posts.map((p) => (p.id === id ? { ...p, ...patch } : p)) })),
    deletePost: (id: string) => set((s) => ({ ...s, posts: s.posts.filter((p) => p.id !== id) })),
    addComment: (postId: string, text: string, anonymous?: boolean) => set((s) => ({ ...s, posts: s.posts.map((p) => (p.id === postId ? { ...p, comments: [...p.comments, { id: uid(), authorId: ME_ID, text, anonymous, at: now() } satisfies Comment] } : p)) })),
    likePost: (id: string) => set((s) => ({ ...s, posts: s.posts.map((p) => (p.id === id ? { ...p, likes: p.likes + 1 } : p)) })),
    joinMeetup: (id: string) => set((s) => ({ ...s, posts: s.posts.map((p) => (p.id === id && p.meetup && !p.meetup.memberIds.includes(ME_ID) ? { ...p, meetup: { ...p.meetup, memberIds: [...p.meetup.memberIds, ME_ID] } } : p)) })),
    leaveMeetup: (id: string) => set((s) => ({ ...s, posts: s.posts.map((p) => (p.id === id && p.meetup ? { ...p, meetup: { ...p.meetup, memberIds: p.meetup.memberIds.filter((m) => m !== ME_ID) } } : p)) })),
    applyRecruit: (id: string) => set((s) => ({ ...s, posts: s.posts.map((p) => (p.id === id && p.recruit && !p.recruit.memberIds.includes(ME_ID) ? { ...p, recruit: { ...p.recruit, memberIds: [...p.recruit.memberIds, ME_ID] } } : p)) })),
    setRecruitStatus: (id: string, status: "OPEN" | "APPLIED" | "ENDED") => set((s) => ({ ...s, posts: s.posts.map((p) => (p.id === id && p.recruit ? { ...p, recruit: { ...p.recruit, status } } : p)) })),

    markNotificationsRead: (ids?: string[]) => set((s) => ({ ...s, notifications: s.notifications.map((n) => (!ids || ids.includes(n.id) ? { ...n, read: true } : n)) })),

    /** CS-01 커피챗 설정 */
    setSettings: (patch: Partial<CoffeeSettings>) =>
      set((s) => {
        const settings = { ...s.settings, ...patch };
        return { ...s, settings, alumni: s.alumni.map((a) => (a.id === ME_ID ? { ...a, receive: settings.mode, allowGroup: settings.allowGroup } : a)) };
      }),
    setNotiSettings: (patch: Partial<State["notiSettings"]>) => set((s) => ({ ...s, notiSettings: { ...s.notiSettings, ...patch } })),
    updateMe: (patch: Partial<Alumni>) => set((s) => ({ ...s, alumni: s.alumni.map((a) => (a.id === ME_ID ? { ...a, ...patch } : a)) })),

    /** AD-10 가입 승인 */
    approveSignup: (id: string) => set((s) => ({ ...s, signups: s.signups.map((x) => (x.id === id ? { ...x, status: "APPROVED" } : x)) })),
    rejectSignup: (id: string) => set((s) => ({ ...s, signups: s.signups.map((x) => (x.id === id ? { ...x, status: "REJECTED" } : x)) })),
    /** AD-02 초대 관리 */
    createInvites: (list: { name: string; generation: number; email?: string }[]) => {
      const created: Invite[] = list.map((i) => {
        const exp = new Date();
        exp.setDate(exp.getDate() + 7);
        return { id: `i-${uid()}`, token: uid(), status: "UNUSED", createdAt: now(), expiresAt: exp.toISOString(), ...i };
      });
      set((s) => ({ ...s, invites: [...created, ...s.invites] }));
      return created;
    },
    reissueInvite: (id: string) => set((s) => ({ ...s, invites: s.invites.map((i) => (i.id === id ? { ...i, token: uid(), status: "UNUSED", createdAt: now(), expiresAt: new Date(Date.now() + 7 * 86_400_000).toISOString() } : i)) })),
    cancelInvite: (id: string) => set((s) => ({ ...s, invites: s.invites.map((i) => (i.id === id ? { ...i, status: "CANCELED" } : i)) })),
  };
}

type Actions = ReturnType<typeof buildActions>;
const StoreContext = createContext<(State & Actions) | null>(null);

export function StoreProvider({ children }: { children: React.ReactNode }) {
  const [state, setState] = useState<State>(initial);
  const actions = useMemo(() => buildActions(setState), []);
  const value = useMemo(() => ({ ...state, ...actions }), [state, actions]);
  return <StoreContext.Provider value={value}>{children}</StoreContext.Provider>;
}

export function useStore() {
  const ctx = useContext(StoreContext);
  if (!ctx) throw new Error("useStore must be used inside StoreProvider");
  return ctx;
}

/** Convenience selectors */
export function useAlumni(id?: string) {
  const { alumni } = useStore();
  const find = useCallback((x: string) => alumni.find((a) => a.id === x), [alumni]);
  return id ? find(id) : undefined;
}
