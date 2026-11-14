"use client";

import { useMemo } from "react";
import { useStore } from "@/lib/store";
import { API_ENABLED } from "./client";
import * as coffee from "./coffee";
import type { CoffeeBox, CoffeeChatDetail, CoffeeChatList, CoffeeFilter, CoffeeRequest } from "./coffee";
import { getAlumni, type CoffeeTopic } from "./alumni";
import { slotsText } from "./alumniView";
import type { Id } from "./posts";
import * as mock from "./mock/coffee";

/** 커피챗 신청 창이 보여줄 받는 사자 정보 */
export interface RequestTarget {
  id: Id;
  name: string;
  /** 사자가 받는 주제 (신청 주제는 이 중 하나 또는 ETC) */
  topics: CoffeeTopic[];
  /** 가능한 시간대 문구 */
  times: string;
  /** 여러 명 신청을 받는지 */
  acceptGroup: boolean;
}

/**
 * 커피챗 화면이 쓰는 API 묶음.
 * API 모드에서는 실제 서버, 프로토타입 모드에서는 mock 커피챗을 같은 응답 모양으로 돌려준다.
 */
export interface CoffeeService {
  list: (box: CoffeeBox, filter?: CoffeeFilter, page?: number, size?: number, signal?: AbortSignal) => Promise<CoffeeChatList>;
  get: (id: Id) => Promise<CoffeeChatDetail>;
  request: (body: CoffeeRequest) => Promise<{ id: Id }>;
  withdraw: (id: Id) => Promise<CoffeeChatDetail>;
  target: (userId: Id) => Promise<RequestTarget>;
}

const apiService: CoffeeService = {
  list: coffee.listCoffeeChats,
  get: coffee.getCoffeeChat,
  request: coffee.requestCoffeeChat,
  withdraw: coffee.withdrawCoffeeChat,
  target: async (userId) => {
    const d = await getAlumni(userId);
    return { id: d.userId, name: d.name, topics: d.coffeeChat.topics.filter((t) => t !== "ETC"), times: slotsText(d.coffeeChat.availableSlots), acceptGroup: d.coffeeChat.acceptGroup };
  },
};

export function useCoffeeService(): CoffeeService {
  const store = useStore();
  return useMemo<CoffeeService>(() => {
    if (API_ENABLED) return apiService;
    const state = { coffeeChats: store.coffeeChats, alumni: store.alumni };
    return {
      list: (box, filter = "ALL", page = 0, size = 20) => mock.mockListCoffeeChats(state, box, filter, page, size),
      get: (id) => mock.mockGetCoffeeChat(state, id),
      request: (body) => {
        const post = body.fromRecruitId ? store.posts.find((p) => p.id === String(body.fromRecruitId)) : undefined;
        return mock.mockRequestCoffeeChat(state, store, body, post?.recruit ? { targetId: post.recruit.targetId, memberIds: post.recruit.memberIds } : undefined).then((res) => {
          if (post) store.setRecruitStatus(post.id, "APPLIED");
          return res;
        });
      },
      withdraw: (id) => mock.mockWithdraw(state, store, id),
      target: (userId) => mock.mockRequestTarget(state, userId),
    };
  }, [store]);
}
