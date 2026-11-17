/**
 * 프로토타입 모드(NEXT_PUBLIC_API_URL 없음)에서 커피챗 화면이 쓰는 가짜 API.
 * lib/store.tsx의 mock 커피챗을 실제 API 응답 모양(lib/api/coffee.ts)으로 바꿔 돌려준다.
 */

import type { Alumni, CoffeeChat, CoffeeChatStatus } from "@/lib/types";
import { ME_ID } from "@/lib/mock/users";
import { profileDetail } from "@/lib/mock/profile";
import { ApiError } from "../client";
import type { ApiLionPart } from "../profile";
import type { CoffeeTopic } from "../alumni";
import type { Id } from "../posts";
import { COFFEE_TOPIC_LABEL, type CoffeeAction, type CoffeeBox, type CoffeeChatDetail, type CoffeeChatItem, type CoffeeChatList, type CoffeeFilter, type CoffeePerson, type CoffeeRequest, type CoffeeRole } from "../coffee";
import type { RequestTarget } from "../useCoffeeService";

export interface MockCoffeeState {
  coffeeChats: CoffeeChat[];
  alumni: Alumni[];
}

export interface MockCoffeeActions {
  createCoffeeChat: (input: Omit<CoffeeChat, "id" | "status" | "createdAt" | "requesterId">) => string;
  cancelCoffeeChat: (id: string, note?: string) => void;
}

const delay = <T,>(value: T) => new Promise<T>((resolve) => setTimeout(() => resolve(value), 120));
const ACTIVE: CoffeeChatStatus[] = ["ACCEPTED", "SCHEDULED"];
const ENDED: CoffeeChatStatus[] = ["COMPLETED", "REJECTED", "CANCELED", "EXPIRED"];
const LABEL_TO_TOPIC = Object.fromEntries(Object.entries(COFFEE_TOPIC_LABEL).map(([code, label]) => [label, code])) as Record<string, CoffeeTopic>;

function person(state: MockCoffeeState, id: string): CoffeePerson {
  const a = state.alumni.find((x) => x.id === id);
  if (!a) return { id: null, name: null, generation: null, lionParts: null, photoUrl: null, isDeleted: true };
  return { id: a.id, name: a.name, generation: a.generation, lionParts: (a.parts ?? [a.part]).filter((p): p is ApiLionPart => p !== "ETC"), photoUrl: null, isDeleted: false };
}

function role(c: CoffeeChat): CoffeeRole | null {
  if (c.receiverId === ME_ID) return "RECEIVER";
  if (c.requesterId === ME_ID) return "REQUESTER";
  if (c.memberIds.includes(ME_ID)) return "MEMBER";
  return null;
}

/** docs/api.md '역할과 상태' 표 */
function actionsFor(c: CoffeeChat, r: CoffeeRole): CoffeeAction[] {
  if (c.status === "PENDING") return r === "RECEIVER" ? ["ACCEPT", "REJECT"] : r === "REQUESTER" ? ["WITHDRAW"] : ["LEAVE"];
  if (c.status === "ACCEPTED") return r === "MEMBER" ? ["LEAVE"] : ["SCHEDULE", "CANCEL"];
  if (c.status === "SCHEDULED") {
    if (r === "MEMBER") return ["LEAVE"];
    const passed = c.scheduledAt && new Date(c.scheduledAt).getTime() < Date.now();
    return passed ? ["SCHEDULE", "CANCEL", "COMPLETE"] : ["SCHEDULE", "CANCEL"];
  }
  return [];
}

function topic(c: CoffeeChat): { topic: CoffeeTopic; topicCustom: string | null } {
  const code = LABEL_TO_TOPIC[c.topic];
  return code ? { topic: code, topicCustom: null } : { topic: "ETC", topicCustom: c.topic };
}

function toItem(state: MockCoffeeState, c: CoffeeChat, r: CoffeeRole): CoffeeChatItem {
  return {
    id: c.id,
    status: c.status,
    myRole: r,
    counterpart: person(state, r === "RECEIVER" ? c.requesterId : c.receiverId),
    members: c.memberIds.map((id) => person(state, id)),
    ...topic(c),
    reason: c.message,
    createdAt: c.createdAt,
    expiresAt: c.expiresAt ?? null,
    scheduledAt: c.scheduledAt ?? null,
  };
}

function toDetail(state: MockCoffeeState, c: CoffeeChat, r: CoffeeRole): CoffeeChatDetail {
  return {
    id: c.id,
    status: c.status,
    myRole: r,
    receiver: person(state, c.receiverId),
    requester: person(state, c.requesterId),
    members: c.memberIds.map((id) => person(state, id)),
    ...topic(c),
    reason: c.message,
    questions: c.questions,
    preferredMode: c.mode,
    scheduleNote: null,
    proposedSlots: c.candidates,
    responseMessage: c.status === "REJECTED" ? c.rejectNote ?? null : null,
    rejectReason: c.rejectReason ?? null,
    meetingMode: c.scheduledAt ? (c.mode === "ONLINE" ? "ONLINE" : "OFFLINE") : null,
    scheduledAt: c.scheduledAt ?? null,
    meetingPlace: c.scheduledAt && c.mode !== "ONLINE" ? "강남역 ○○카페" : null,
    meetingLink: null,
    cancelReason: c.status === "CANCELED" ? c.rejectNote ?? null : null,
    canceledBy: null,
    roomId: c.roomId ?? null,
    recruitPostId: c.fromRecruitId ?? null,
    createdAt: c.createdAt,
    expiresAt: c.expiresAt ?? null,
    acceptedAt: null,
    rejectedAt: null,
    canceledAt: null,
    completedAt: null,
    actions: actionsFor(c, r),
  };
}

export function mockListCoffeeChats(state: MockCoffeeState, box: CoffeeBox, filter: CoffeeFilter, page: number, size: number): Promise<CoffeeChatList> {
  const mine = state.coffeeChats.filter((c) => (box === "RECEIVED" ? c.receiverId === ME_ID : c.requesterId === ME_ID || c.memberIds.includes(ME_ID)));
  const list = mine.filter((c) => filter === "ALL" || (filter === "PENDING" && c.status === "PENDING") || (filter === "ACTIVE" && ACTIVE.includes(c.status)) || (filter === "ENDED" && ENDED.includes(c.status)));
  const items = list.slice(page * size, (page + 1) * size).map((c) => toItem(state, c, role(c)!));
  return delay({
    items,
    page,
    size,
    hasNext: (page + 1) * size < list.length,
    pendingReceivedCount: state.coffeeChats.filter((c) => c.receiverId === ME_ID && c.status === "PENDING").length,
    activeCount: state.coffeeChats.filter((c) => role(c) && ACTIVE.includes(c.status)).length,
  });
}

export function mockGetCoffeeChat(state: MockCoffeeState, id: Id): Promise<CoffeeChatDetail> {
  const c = state.coffeeChats.find((x) => x.id === String(id));
  const r = c && role(c);
  if (!c || !r) return Promise.reject(new ApiError(404, "COFFEE_CHAT_NOT_FOUND", "커피챗을 찾을 수 없어요."));
  return delay(toDetail(state, c, r));
}

export function mockRequestCoffeeChat(state: MockCoffeeState, actions: MockCoffeeActions, body: CoffeeRequest, recruit?: { targetId: string; memberIds: string[] }) {
  const receiverId = recruit?.targetId ?? String(body.receiverId);
  const receiver = state.alumni.find((a) => a.id === receiverId);
  if (!receiver) return Promise.reject(new ApiError(404, "USER_NOT_FOUND", "받는 사자를 찾을 수 없어요."));
  if (receiver.receive !== "OPEN") return Promise.reject(new ApiError(409, "NOT_ACCEPTING", "지금은 커피챗을 받지 않는 사자예요."));
  const open = state.coffeeChats.some((c) => c.receiverId === receiverId && c.requesterId === ME_ID && ["PENDING", "ACCEPTED", "SCHEDULED"].includes(c.status));
  if (open) return Promise.reject(new ApiError(409, "OPEN_COFFEE_CHAT_EXISTS", "이 사자와 진행 중인 커피챗이 있어요."));
  const id = actions.createCoffeeChat({
    receiverId,
    memberIds: recruit ? recruit.memberIds.filter((m) => m !== ME_ID) : (body.memberIds ?? []).map(String),
    topic: body.topic === "ETC" ? body.topicCustom ?? "기타" : COFFEE_TOPIC_LABEL[body.topic],
    message: body.reason,
    questions: body.questions ?? [],
    mode: body.preferredMode,
    candidates: [],
    fromRecruitId: body.fromRecruitId ? String(body.fromRecruitId) : undefined,
  });
  return delay({ id });
}

export function mockWithdraw(state: MockCoffeeState, actions: MockCoffeeActions, id: Id) {
  const c = state.coffeeChats.find((x) => x.id === String(id));
  if (!c || c.requesterId !== ME_ID || c.status !== "PENDING") return Promise.reject(new ApiError(409, "INVALID_STATUS", "지금은 신청을 취소할 수 없어요."));
  actions.cancelCoffeeChat(c.id);
  return delay(toDetail(state, { ...c, status: "CANCELED" }, "REQUESTER"));
}

export function mockRequestTarget(state: MockCoffeeState, id: Id): Promise<RequestTarget> {
  const a = state.alumni.find((x) => x.id === String(id));
  if (!a) return Promise.reject(new ApiError(404, "USER_NOT_FOUND", "동문을 찾을 수 없어요."));
  const d = profileDetail(a.id);
  const topics = d.coffeeTopics.map((t) => LABEL_TO_TOPIC[t]).filter((t): t is CoffeeTopic => !!t && t !== "ETC");
  return delay({ id: a.id, name: a.name, topics, times: d.coffeeTimes, acceptGroup: a.allowGroup });
}
