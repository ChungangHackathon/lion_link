"use client";

import { createContext, useContext, useMemo } from "react";
import { useStore } from "@/lib/store";
import type { PendingSignup } from "@/lib/types";
import { API_ENABLED, ApiError, type Page } from "./client";
import * as admin from "./admin";
import type { BatchResult, RejectReasonType, SignupItem, SignupStatus } from "./admin";
import type { Id } from "./posts";

/**
 * 관리자 > 가입 승인 화면이 쓰는 API 묶음 (ADMIN-01 ~ 06).
 * 프로토타입 모드에서는 store의 mock 가입 신청을 같은 응답 모양으로 바꿔 준다.
 */
export interface AdminService {
  list: (status: SignupStatus, page?: number, signal?: AbortSignal) => Promise<Page<SignupItem>>;
  pendingCount: (signal?: AbortSignal) => Promise<{ pending: number }>;
  approve: (userId: Id) => Promise<{ name: string }>;
  reject: (userId: Id, reasonType: RejectReasonType, reasonText?: string) => Promise<void>;
  approveBatch: (userIds: Id[]) => Promise<BatchResult>;
  updateInfo: (userId: Id, body: { name: string; generation: number }) => Promise<{ name: string; generation: number }>;
}

const PAGE_SIZE = 20;

const apiService: AdminService = {
  list: (status, page = 0, signal) => admin.listSignups(status, page, PAGE_SIZE, signal),
  pendingCount: admin.getPendingCount,
  approve: admin.approveSignup,
  reject: async (userId, reasonType, reasonText) => {
    await admin.rejectSignup(userId, reasonType, reasonText);
  },
  approveBatch: admin.approveSignups,
  updateInfo: admin.updateSignupInfo,
};

// ---------------------------------------------------------------------------
// 프로토타입 mock
// ---------------------------------------------------------------------------

const delay = <T,>(value: T) => new Promise<T>((resolve) => setTimeout(() => resolve(value), 100));
const DEFAULT_REASON = "동문인지 확인할 수 없어요";
/** mock store에는 이름 수정·반려 사유가 없어서 여기에 둔다 */
const mockEdits: Record<string, { name: string; generation: number }> = {};
const mockReasons: Record<string, string> = {};

function fromMock(s: PendingSignup): SignupItem {
  const edit = mockEdits[s.id];
  return {
    userId: s.id,
    name: edit?.name ?? s.name,
    generation: edit?.generation ?? s.generation,
    email: s.email,
    googleName: s.name,
    signedUpAt: s.appliedAt,
    lionParts: s.part === "ETC" ? [] : [s.part],
    currentStatus: "STUDENT",
    company: null,
    sameNameGeneration: s.note === "DUPLICATE",
    reapplied: s.note === "REAPPLY",
    reapplyAt: s.note === "REAPPLY" ? s.appliedAt : null,
    rejectReason: s.status === "REJECTED" || s.note === "REAPPLY" ? mockReasons[s.id] ?? DEFAULT_REASON : null,
    reviewedAt: null,
    reviewedBy: null,
  };
}

export function useAdminService(): AdminService {
  const store = useStore();
  return useMemo<AdminService>(() => {
    if (API_ENABLED) return apiService;
    const find = (id: Id) => store.signups.find((s) => s.id === String(id));
    const approve = (id: Id) => {
      const s = find(id);
      if (!s) return Promise.reject(new ApiError(404, "NOT_FOUND", "회원을 찾을 수 없어요"));
      if (s.status === "APPROVED") return Promise.reject(new ApiError(409, "ALREADY_PROCESSED", "이미 처리된 가입이에요"));
      store.approveSignup(s.id);
      return delay({ name: fromMock(s).name });
    };
    return {
      list: (status, page = 0) => {
        const all = store.signups.filter((s) => s.status === status).map(fromMock);
        const items = all.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE);
        return delay({ items, page, size: PAGE_SIZE, totalElements: all.length, hasNext: (page + 1) * PAGE_SIZE < all.length });
      },
      pendingCount: () => delay({ pending: store.signups.filter((s) => s.status === "PENDING").length }),
      approve,
      reject: (id, reasonType, reasonText) => {
        const s = find(id);
        if (!s) return Promise.reject(new ApiError(404, "NOT_FOUND", "회원을 찾을 수 없어요"));
        if (s.status !== "PENDING") return Promise.reject(new ApiError(409, "ALREADY_PROCESSED", "이미 처리된 가입이에요"));
        mockReasons[s.id] = reasonType === "ETC" ? reasonText ?? "" : admin.REJECT_REASONS.find((r) => r.type === reasonType)!.label;
        store.rejectSignup(s.id);
        return delay(undefined);
      },
      approveBatch: async (ids) => {
        const result: BatchResult = { succeeded: [], failed: [] };
        for (const id of ids) {
          const s = find(id);
          if (!s) result.failed.push({ userId: id, code: "NOT_FOUND" });
          else if (s.status === "APPROVED") result.failed.push({ userId: id, code: "ALREADY_PROCESSED" });
          else {
            store.approveSignup(s.id);
            result.succeeded.push({ userId: id, name: fromMock(s).name, status: "APPROVED" });
          }
        }
        return delay(result);
      },
      updateInfo: (id, body) => {
        mockEdits[String(id)] = body;
        return delay(body);
      },
    };
  }, [store]);
}

/** 관리자 메뉴 [가입 승인] 옆 숫자를 다시 불러오는 함수. 관리자 layout이 넣어 준다 */
export const PendingCountContext = createContext<() => void>(() => undefined);
export const useRefreshPendingCount = () => useContext(PendingCountContext);
