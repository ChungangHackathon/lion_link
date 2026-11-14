"use client";

import { useState } from "react";
import type { Alumni } from "@/lib/types";
import { useStore } from "@/lib/store";
import { API_ENABLED } from "@/lib/api/client";
import type { AlumniSummary } from "@/lib/api/alumni";
import { CoffeeRequestModal } from "./CoffeeRequestModal";
import { MessageRequestModal } from "./MessageRequestModal";

/**
 * Shared "커피챗 신청 / 메시지" modal state for any screen that renders AlumniCard.
 * 두 모달은 아직 mock 동문 데이터로만 동작해서, API 모드에서는 핸들러를 주지 않는다(카드 버튼이 잠긴다).
 * 커피챗·채팅 API를 연결하면 여기서 열어 준다.
 */
export function useAlumniActions() {
  const { alumni } = useStore();
  const [request, setRequest] = useState<Alumni | null>(null);
  const [message, setMessage] = useState<Alumni | null>(null);
  const find = (p: AlumniSummary) => alumni.find((a) => a.id === String(p.id)) ?? null;
  const modals = (
    <>
      {request && <CoffeeRequestModal open onClose={() => setRequest(null)} target={request} />}
      {message && <MessageRequestModal open onClose={() => setMessage(null)} target={message} />}
    </>
  );
  if (API_ENABLED) return { onRequest: undefined, onMessage: undefined, modals: null };
  return { onRequest: (p: AlumniSummary) => setRequest(find(p)), onMessage: (p: AlumniSummary) => setMessage(find(p)), modals };
}

/**
 * 홈처럼 아직 mock 동문 데이터를 그대로 그리는 화면용 (HOME API 연결 전).
 * mock 동문을 받아 커피챗 신청·메시지 모달을 연다.
 */
export function useMockAlumniActions() {
  const [request, setRequest] = useState<Alumni | null>(null);
  const [message, setMessage] = useState<Alumni | null>(null);
  const modals = (
    <>
      {request && <CoffeeRequestModal open onClose={() => setRequest(null)} target={request} />}
      {message && <MessageRequestModal open onClose={() => setMessage(null)} target={message} />}
    </>
  );
  return { onRequest: setRequest, onMessage: setMessage, modals };
}
