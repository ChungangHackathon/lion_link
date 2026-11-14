"use client";

import { createContext, useContext, useMemo } from "react";
import { useStore } from "@/lib/store";
import { ME_ID } from "@/lib/mock/users";
import type { Alumni, ChatMessage as MockMessage, ChatRoom } from "@/lib/types";
import { API_ENABLED, ApiError } from "./client";
import * as chat from "./chat";
import type { ChatMessage, ChatUser, DeclineReason, RequestDetail, RequestItem, RoomDetail, RoomStatus, RoomSummary, SliceOf } from "./chat";
import type { ApiLionPart } from "./profile";
import type { Id } from "./posts";

/**
 * 채팅 화면이 쓰는 API 묶음 (CHAT-01 ~ 15).
 * 프로토타입 모드에서는 store의 mock 채팅방을 같은 응답 모양으로 바꿔 준다.
 */
export interface ChatService {
  sendRequest: (receiverId: Id, content: string) => Promise<{ roomId: Id; created: boolean; status: RoomStatus; requestedByMe: boolean }>;
  listRequests: (page?: number, signal?: AbortSignal) => Promise<SliceOf<RequestItem>>;
  getRequest: (roomId: Id, signal?: AbortSignal) => Promise<RequestDetail>;
  acceptRequest: (roomId: Id) => Promise<void>;
  declineRequest: (roomId: Id, reason: DeclineReason, message?: string) => Promise<void>;
  editRequestMessage: (roomId: Id, messageId: Id, content: string) => Promise<ChatMessage>;
  cancelRequest: (roomId: Id, messageId: Id) => Promise<void>;
  listRooms: (page?: number, signal?: AbortSignal) => Promise<SliceOf<RoomSummary>>;
  getRoom: (roomId: Id, signal?: AbortSignal) => Promise<RoomDetail>;
  listMessages: (roomId: Id, cursor?: { before?: Id; after?: Id }, signal?: AbortSignal) => Promise<{ items: ChatMessage[]; hasMore: boolean }>;
  send: (roomId: Id, content: string, clientMsgId?: string) => Promise<ChatMessage>;
  read: (roomId: Id, lastMessageId?: Id) => Promise<void>;
  setPreferences: (roomId: Id, patch: { pinned?: boolean; muted?: boolean }) => Promise<{ pinned: boolean; muted: boolean }>;
  leave: (roomId: Id) => Promise<void>;
  unreadCount: (signal?: AbortSignal) => Promise<{ unreadMessages: number; messageRequests: number }>;
}

const ROOM_PAGE = 30;

const apiService: ChatService = {
  sendRequest: chat.sendRequest,
  listRequests: (page = 0, signal) => chat.listRequests(page, 20, signal),
  getRequest: chat.getRequest,
  acceptRequest: async (roomId) => {
    await chat.acceptRequest(roomId);
  },
  declineRequest: chat.declineRequest,
  editRequestMessage: chat.editRequestMessage,
  cancelRequest: chat.cancelRequest,
  listRooms: (page = 0, signal) => chat.listRooms(page, ROOM_PAGE, signal),
  getRoom: chat.getRoom,
  listMessages: (roomId, cursor, signal) => chat.listMessages(roomId, cursor, 30, signal),
  send: chat.sendMessage,
  read: async (roomId, lastMessageId) => {
    await chat.readRoom(roomId, lastMessageId);
  },
  setPreferences: chat.updatePreferences,
  leave: chat.leaveRoom,
  unreadCount: chat.getUnreadCount,
};

// ---------------------------------------------------------------------------
// 프로토타입 mock
// ---------------------------------------------------------------------------

const delay = <T,>(value: T) => new Promise<T>((resolve) => setTimeout(() => resolve(value), 80));
const notFound = () => Promise.reject(new ApiError(404, "ROOM_NOT_FOUND", "채팅방을 찾을 수 없어요"));
/** mock store에는 상단 고정·알림 끄기가 없어서 여기에 둔다 */
const mockPrefs: Record<string, { pinned: boolean; muted: boolean }> = {};
const prefsOf = (id: string) => mockPrefs[id] ?? { pinned: false, muted: false };

function userOf(a: Alumni | undefined): ChatUser {
  if (!a) return { id: null, name: null, generation: null, lionParts: null, photoUrl: null, isDeleted: true };
  return { id: a.id, name: a.name, generation: a.generation, lionParts: a.part === "ETC" ? [] : [a.part as ApiLionPart], photoUrl: null, isDeleted: false };
}

function messageOf(m: MockMessage, room: ChatRoom): ChatMessage {
  const system = m.senderId === "system";
  return {
    id: m.id,
    type: system ? "SYSTEM" : "TEXT",
    senderId: system ? null : m.senderId,
    content: m.text,
    deleted: false,
    clientMsgId: null,
    createdAt: m.at,
    editedAt: null,
    unreadCount: !system && m.senderId === ME_ID && room.type === "GROUP" ? Math.max(0, room.memberIds.length - 1 - (m.readBy?.length ?? 0)) : 0,
  };
}

export function useChatService(): ChatService {
  const store = useStore();
  return useMemo<ChatService>(() => {
    if (API_ENABLED) return apiService;
    const { rooms, alumni, coffeeChats } = store;
    const person = (id: string) => alumni.find((a) => a.id === id);
    const find = (id: Id) => rooms.find((r) => r.id === String(id));
    const receivedRequest = (r: ChatRoom) => r.isRequest && r.status === "REQUESTED" && r.memberIds[0] !== ME_ID;
    const requestedByMe = (r: ChatRoom) => !!r.isRequest && r.memberIds[0] === ME_ID;
    const others = (r: ChatRoom) => r.memberIds.filter((m) => m !== ME_ID).map((m) => userOf(person(m)));

    const summary = (r: ChatRoom): RoomSummary => ({
      roomId: r.id,
      type: r.type,
      status: r.status as RoomStatus,
      requestedByMe: requestedByMe(r),
      partner: r.type === "DIRECT" ? others(r)[0] ?? null : null,
      members: r.type === "GROUP" ? others(r) : [],
      memberCount: r.memberIds.length,
      coffeeChatId: r.coffeeChatId ?? null,
      meetupId: null,
      title: r.title ?? null,
      lastMessage: r.messages.length ? messageOf(r.messages.at(-1)!, r) : null,
      unreadCount: r.unread,
      ...prefsOf(r.id),
    });

    const requestDetail = (r: ChatRoom): RequestDetail => {
      const declined = [...r.messages].reverse().find((m) => m.senderId === "system" && m.text.includes("사유: "));
      const label = declined?.text.split("사유: ")[1] ?? null;
      const code = label ? chat.DECLINE_REASONS.find((d) => d.label === label)?.value ?? "ETC" : null;
      const first = r.messages[0];
      return {
        roomId: r.id,
        status: r.status as RoomStatus,
        requester: userOf(person(r.memberIds[0])),
        receiver: userOf(person(r.memberIds[1])),
        firstMessage: first ? messageOf(first, r) : { id: "none", type: "TEXT", senderId: null, content: "", deleted: true, clientMsgId: null, createdAt: "", editedAt: null, unreadCount: 0 },
        requestedAt: first?.at ?? "",
        declinedAt: r.status === "DECLINED" ? declined?.at ?? null : null,
        declineReason: r.status === "DECLINED" ? code : null,
        declineMessage: code === "ETC" ? label : null,
      };
    };

    return {
      sendRequest: (receiverId, content) => {
        if (String(receiverId) === ME_ID) return Promise.reject(new ApiError(400, "CANNOT_CHAT_SELF", "나에게는 요청할 수 없어요"));
        const existing = rooms.find((r) => r.type === "DIRECT" && r.memberIds.includes(String(receiverId)) && (r.status === "ACTIVE" || r.status === "REQUESTED"));
        if (existing) return delay({ roomId: existing.id, created: false, status: existing.status as RoomStatus, requestedByMe: requestedByMe(existing) || existing.status === "ACTIVE" });
        const roomId = store.createMessageRequest(String(receiverId), content);
        return delay({ roomId, created: true, status: "REQUESTED" as const, requestedByMe: true });
      },
      listRequests: () => {
        const items = rooms.filter(receivedRequest).map((r) => {
          const d = requestDetail(r);
          return { roomId: d.roomId, requester: d.requester, firstMessage: d.firstMessage, requestedAt: d.requestedAt };
        });
        return delay({ items, page: 0, size: items.length, hasNext: false });
      },
      getRequest: (roomId) => {
        const r = find(roomId);
        return r?.isRequest || r?.status === "DECLINED" ? delay(requestDetail(r)) : notFound();
      },
      acceptRequest: (roomId) => {
        if (!find(roomId)) return notFound();
        store.acceptMessageRequest(String(roomId));
        return delay(undefined);
      },
      declineRequest: (roomId, reason, message) => {
        if (!find(roomId)) return notFound();
        store.declineMessageRequest(String(roomId), chat.declineLabel(reason, message ?? null));
        return delay(undefined);
      },
      editRequestMessage: (roomId, _messageId, content) => {
        const r = find(roomId);
        if (!r) return notFound();
        store.editRequestMessage(r.id, content);
        return delay({ ...messageOf(r.messages[0], r), content, editedAt: new Date().toISOString() });
      },
      cancelRequest: (roomId) => {
        if (!find(roomId)) return notFound();
        store.cancelMessageRequest(String(roomId));
        return delay(undefined);
      },
      listRooms: () => {
        const items = rooms
          .filter((r) => !receivedRequest(r) && r.status !== "CANCELED")
          .map(summary)
          .sort((a, b) => Number(b.pinned) - Number(a.pinned) || (b.lastMessage?.createdAt ?? "").localeCompare(a.lastMessage?.createdAt ?? ""));
        return delay({ items, page: 0, size: items.length, hasNext: false });
      },
      getRoom: (roomId) => {
        const r = find(roomId);
        if (!r || receivedRequest(r)) return notFound();
        const c = r.coffeeChatId ? coffeeChats.find((x) => x.id === r.coffeeChatId) : undefined;
        const blocked = r.memberIds.some((m) => store.blocked.includes(m));
        return delay({
          roomId: r.id,
          type: r.type,
          status: r.status as RoomStatus,
          requestedByMe: requestedByMe(r),
          members: others(r),
          coffeeChat: c ? { id: c.id, status: c.status, scheduledAt: c.scheduledAt ?? null, proposedSlots: c.candidates, meetingMode: c.mode === "OFFLINE" ? "OFFLINE" : "ONLINE", meetingPlace: null, meetingLink: null, questions: c.questions } : null,
          meetup: null,
          ...prefsOf(r.id),
          canSend: r.status === "ACTIVE" && !blocked,
        });
      },
      listMessages: (roomId, cursor = {}) => {
        const r = find(roomId);
        if (!r) return notFound();
        const all = r.messages.map((m) => messageOf(m, r));
        if (cursor.after != null) {
          const i = all.findIndex((m) => m.id === cursor.after);
          return delay({ items: all.slice(i + 1), hasMore: false });
        }
        const end = cursor.before != null ? all.findIndex((m) => m.id === cursor.before) : all.length;
        const start = Math.max(0, end - 30);
        return delay({ items: all.slice(start, end), hasMore: start > 0 });
      },
      send: (roomId, content, clientMsgId) => {
        const r = find(roomId);
        if (!r) return notFound();
        if (r.status !== "ACTIVE") return Promise.reject(new ApiError(409, "INVALID_ROOM_STATUS", "지금은 메시지를 보낼 수 없어요"));
        store.sendMessage(r.id, content);
        return delay({ id: `local-${Date.now()}`, type: "TEXT" as const, senderId: ME_ID, content, deleted: false, clientMsgId: clientMsgId ?? null, createdAt: new Date().toISOString(), editedAt: null, unreadCount: r.type === "GROUP" ? r.memberIds.length - 1 : 0 });
      },
      read: (roomId) => {
        // 이미 읽은 방이면 store를 건드리지 않는다 (불필요한 다시 그리기 방지)
        if (find(roomId)?.unread) store.markRoomRead(String(roomId));
        return delay(undefined);
      },
      setPreferences: (roomId, patch) => {
        const next = { ...prefsOf(String(roomId)), ...patch };
        mockPrefs[String(roomId)] = next;
        return delay(next);
      },
      leave: (roomId) => {
        if (!find(roomId)) return notFound();
        store.leaveRoom(String(roomId));
        return delay(undefined);
      },
      unreadCount: () => delay({ unreadMessages: rooms.reduce((n, r) => n + (receivedRequest(r) ? 0 : r.unread), 0), messageRequests: rooms.filter(receivedRequest).length }),
    };
  }, [store]);
}

/** 채팅 목록·안 읽은 수를 다시 불러오는 함수. 채팅 layout이 넣어 준다 */
export const ChatRefreshContext = createContext<() => void>(() => undefined);
export const useChatRefresh = () => useContext(ChatRefreshContext);
