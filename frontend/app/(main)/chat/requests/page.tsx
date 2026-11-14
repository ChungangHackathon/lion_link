"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useState } from "react";
import { cn } from "@/lib/cn";
import { LION_PART_LABEL } from "@/lib/types";
import { formatDate, formatDateTime } from "@/lib/format";
import { errorMessage } from "@/lib/api/client";
import { DECLINE_REASONS, userName, type DeclineReason, type RequestItem } from "@/lib/api/chat";
import { partOf } from "@/lib/api/chatView";
import { useApi } from "@/lib/api/useApi";
import { usePersonalEvents } from "@/lib/api/chatRealtime";
import { useAlumniService } from "@/lib/api/useAlumniService";
import { useChatRefresh, useChatService } from "@/lib/api/useChatService";
import { Avatar } from "@/components/ui/Avatar";
import { Tag } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Glyph } from "@/components/ui/Icon";
import { Input, Radio } from "@/components/ui/Input";
import { Modal } from "@/components/ui/Modal";
import { Banner, EmptyState, Skeleton } from "@/components/ui/Misc";
import { useToast } from "@/components/ui/Toast";

export default function RequestsPage() {
  return (
    <Suspense fallback={<Skeleton className="h-full w-full" />}>
      <Requests />
    </Suspense>
  );
}

/** ◇ T-12 메시지 요청함 / 목록 · ◇ T-14 메시지 요청 / 상세 · T-15 거절 사유 (CHAT-02 · 04 · 05) */
function Requests() {
  const router = useRouter();
  const toast = useToast();
  const params = useSearchParams();
  const service = useChatService();
  const refreshList = useChatRefresh();
  const { data, error, reload, setData } = useApi((signal) => service.listRequests(0, signal), [service]);
  // 새 요청이 오거나(REQUEST_RECEIVED) 보낸 사람이 요청을 취소하면(ROOM_UPDATED) 요청함을 다시 불러온다
  usePersonalEvents(() => reload());
  const [selectedId, setSelectedId] = useState<string | null>(params.get("id"));
  const [decline, setDecline] = useState(false);
  const [busy, setBusy] = useState<"accept" | "decline" | "more" | null>(null);
  const list = data?.items ?? [];
  const selected = list.find((r) => String(r.roomId) === selectedId) ?? list[0];

  const removeSelected = () => {
    if (!selected) return;
    setData((d) => d && { ...d, items: d.items.filter((x) => x.roomId !== selected.roomId) });
    setSelectedId(null);
    refreshList();
  };

  const accept = async () => {
    if (!selected) return;
    setBusy("accept");
    try {
      await service.acceptRequest(selected.roomId);
      toast("요청을 수락했어요");
      refreshList();
      router.push(`/chat/${selected.roomId}`);
    } catch (e) {
      toast(errorMessage(e), { kind: "error" });
      reload();
    } finally {
      setBusy(null);
    }
  };

  const doDecline = async (reason: DeclineReason, message: string) => {
    if (!selected) return;
    setBusy("decline");
    try {
      await service.declineRequest(selected.roomId, reason, reason === "ETC" ? message.trim() : undefined);
      setDecline(false);
      removeSelected();
      toast("요청을 거절했어요");
    } catch (e) {
      toast(errorMessage(e), { kind: "error" });
    } finally {
      setBusy(null);
    }
  };

  const more = async () => {
    if (!data) return;
    setBusy("more");
    try {
      const next = await service.listRequests(data.page + 1);
      setData((d) => d && { ...next, items: [...d.items, ...next.items.filter((x) => !d.items.some((y) => y.roomId === x.roomId))] });
    } catch (e) {
      toast(errorMessage(e), { kind: "error" });
    } finally {
      setBusy(null);
    }
  };

  return (
    <div className="grid flex-1 grid-cols-[320px_1fr]">
      <aside className="flex flex-col border-r border-line-200">
        <Link href="/chat" className="flex h-16 items-center gap-2 border-b border-line-200 px-4 t-body-bold text-ink-900 hover:bg-bg-50">
          <Glyph name="chevron-left" size={18} /> 채팅 목록 <span className="text-[13px] font-normal text-ink-500">메시지 요청 {list.length}</span>
        </Link>
        <ul className="flex-1 overflow-y-auto">
          {!data && !error && (
            <li className="p-4">
              <Skeleton className="h-[200px] w-full" />
            </li>
          )}
          {!!error && !data && <li className="px-4 py-10 text-center text-[13px] text-ink-500">{errorMessage(error)}</li>}
          {data && list.length === 0 && <li className="px-4 py-10 text-center text-[13px] text-ink-500">새 메시지 요청이 없어요</li>}
          {list.map((r) => {
            const a = r.requester;
            const active = selected?.roomId === r.roomId;
            return (
              <li key={String(r.roomId)} className="border-b border-line-200">
                <button type="button" onClick={() => setSelectedId(String(r.roomId))} className={cn("flex w-full flex-col gap-2 px-4 py-4 text-left hover:bg-bg-50", active && "bg-blue-100 hover:bg-blue-100")}>
                  <span className="flex items-center gap-2.5">
                    <Avatar name={userName(a)} part={partOf(a)} size={32} />
                    <span>
                      <span className="text-[14px] font-bold text-ink-900">{userName(a)}</span>
                      {!a.isDeleted && (
                        <span className="ml-1.5 text-[12px] text-ink-500">
                          {a.generation}기 · {LION_PART_LABEL[partOf(a)]}
                        </span>
                      )}
                    </span>
                  </span>
                  <span className="line-clamp-2 text-[13px] text-ink-700">{r.firstMessage.content}</span>
                  <span className="text-[12px] text-ink-400">{formatDate(r.requestedAt)}</span>
                </button>
              </li>
            );
          })}
          {data?.hasNext && (
            <li className="p-3 text-center">
              <button type="button" disabled={busy === "more"} onClick={() => void more()} className="text-[13px] text-ink-500 underline">
                요청 더 보기
              </button>
            </li>
          )}
        </ul>
      </aside>

      {selected ? (
        <RequestView key={String(selected.roomId)} request={selected} busy={busy} onAccept={() => void accept()} onDecline={() => setDecline(true)} />
      ) : (
        <EmptyState title="메시지 요청을 선택해주세요" description="새 요청이 오면 여기에 보여요" />
      )}

      {decline && <DeclineModal busy={busy === "decline"} onClose={() => setDecline(false)} onSubmit={(r, m) => void doDecline(r, m)} />}
    </div>
  );
}

function RequestView({ request, busy, onAccept, onDecline }: { request: RequestItem; busy: string | null; onAccept: () => void; onDecline: () => void }) {
  const from = request.requester;
  const name = userName(from);
  const alumni = useAlumniService();
  // 나와의 공통점은 동문 프로필(ALUMNI-02)에서 가져온다
  const profile = useApi(() => alumni.get(from.id!), [alumni, from.id], !from.isDeleted && from.id != null).data;
  const common = profile?.commonalities ?? [];
  return (
    <section className="flex flex-col">
      <header className="flex h-16 items-center gap-3 border-b border-line-200 px-6">
        <Avatar name={name} part={partOf(from)} size={40} />
        <div className="flex flex-col">
          <span className="t-body-bold text-ink-900">{name}</span>
          {!from.isDeleted && (
            <span className="text-[12px] text-ink-500">
              {from.generation}기 · {LION_PART_LABEL[partOf(from)]}
            </span>
          )}
        </div>
      </header>
      <div className="flex flex-1 flex-col gap-4 px-6 py-5">
        <Banner>
          <span className="flex items-center gap-2">
            <Glyph name="info" size={18} /> {name}님이 대화를 요청했어요. 수락하면 {name}님이 내가 메시지를 읽었는지 알 수 있어요.
          </span>
        </Banner>
        <div className="flex items-center justify-between">
          <span className="text-[12px] text-ink-400">{formatDateTime(request.requestedAt)}</span>
          {!from.isDeleted && (
            <Button size="S" variant="secondary" href={`/alumni/${from.id}`}>
              프로필 보기
            </Button>
          )}
        </div>
        <p className="flex flex-wrap items-center gap-1.5 text-[12px] text-ink-500">
          나와의 공통점 {common.map((c) => <Tag key={c}>{c}</Tag>)}
          {common.length === 0 && <span className="text-ink-400">{profile ? "아직 없어요" : "…"}</span>}
        </p>
        <div className="flex items-start gap-2.5">
          <Avatar name={name} part={partOf(from)} size={40} />
          <div className="flex flex-col gap-1">
            <span className="text-[13px] text-ink-700">{name}</span>
            <p className="max-w-[480px] whitespace-pre-line rounded-[8px] border border-line-200 bg-white px-3.5 py-2.5 text-[14px] leading-5 text-ink-900">{request.firstMessage.content}</p>
          </div>
        </div>
      </div>
      <footer className="flex items-center justify-end gap-2 border-t border-line-200 px-6 py-4">
        <Button variant="secondary" onClick={onDecline} disabled={!!busy}>
          거절
        </Button>
        <Button onClick={onAccept} loading={busy === "accept"} disabled={!!busy && busy !== "accept"}>
          수락
        </Button>
      </footer>
    </section>
  );
}

/** ◇[O] T-15 사유 선택 모달 / 메시지 요청 거절 */
function DeclineModal({ busy, onClose, onSubmit }: { busy: boolean; onClose: () => void; onSubmit: (reason: DeclineReason, message: string) => void }) {
  const [reason, setReason] = useState<DeclineReason>("BUSY");
  const [etc, setEtc] = useState("");
  const len = etc.trim().length;
  return (
    <Modal
      open
      onClose={onClose}
      title="메시지 요청 거절"
      description="고른 사유에 맞는 안내가 상대에게 자동으로 전달돼요"
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            취소
          </Button>
          <Button loading={busy} disabled={reason === "ETC" && (len < 5 || len > 100)} onClick={() => onSubmit(reason, etc)}>
            거절하기
          </Button>
        </>
      }
    >
      <div className="flex flex-col gap-3">
        <p className="t-label2 text-ink-700">사유 *</p>
        {DECLINE_REASONS.map((r) => (
          <Radio key={r.value} name="decline" label={r.label} checked={reason === r.value} onChange={() => setReason(r.value)} />
        ))}
        {reason === "ETC" && <Input aria-label="기타 사유" placeholder="직접 입력 (5~100자)" value={etc} onChange={(e) => setEtc(e.target.value)} counter={{ value: len, max: 100 }} />}
      </div>
    </Modal>
  );
}
