"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useState } from "react";
import { cn } from "@/lib/cn";
import { useStore } from "@/lib/store";
import { ME_ID } from "@/lib/mock/users";
import { LION_PART_LABEL } from "@/lib/types";
import { commonalities } from "@/lib/common";
import { formatDate, formatDateTime } from "@/lib/format";
import { Avatar } from "@/components/ui/Avatar";
import { Tag } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Glyph } from "@/components/ui/Icon";
import { Input, Radio } from "@/components/ui/Input";
import { ConfirmModal, Modal } from "@/components/ui/Modal";
import { Banner, EmptyState, Skeleton } from "@/components/ui/Misc";
import { useToast } from "@/components/ui/Toast";

const DECLINE_REASONS = ["요즘 바빠서 대화가 어려워요", "제가 도움드리기 어려운 분야예요", "커피챗으로 신청해주세요", "기타"];

export default function RequestsPage() {
  return (
    <Suspense fallback={<Skeleton className="h-full w-full" />}>
      <Requests />
    </Suspense>
  );
}

/** ◇ T-12 메시지 요청함 / 목록 · ◇ T-14 메시지 요청 / 상세 · T-15 거절 사유 */
function Requests() {
  const router = useRouter();
  const toast = useToast();
  const params = useSearchParams();
  const { rooms, alumni, acceptMessageRequest, declineMessageRequest } = useStore();
  const me = alumni.find((a) => a.id === ME_ID)!;
  const list = rooms.filter((r) => r.isRequest && r.status === "REQUESTED" && r.memberIds[0] !== ME_ID);
  const [selectedId, setSelectedId] = useState<string | null>(params.get("id"));
  const selected = list.find((r) => r.id === selectedId) ?? list[0];
  const [decline, setDecline] = useState(false);
  const [reason, setReason] = useState(DECLINE_REASONS[0]);
  const [etc, setEtc] = useState("");

  const from = selected ? alumni.find((a) => a.id === selected.memberIds[0]) : undefined;

  return (
    <div className="grid flex-1 grid-cols-[320px_1fr]">
      <aside className="flex flex-col border-r border-line-200">
        <Link href="/chat" className="flex h-16 items-center gap-2 border-b border-line-200 px-4 t-body-bold text-ink-900 hover:bg-bg-50">
          <Glyph name="chevron-left" size={18} /> 채팅 목록 <span className="text-[13px] font-normal text-ink-500">메시지 요청 {list.length}</span>
        </Link>
        <ul className="flex-1 overflow-y-auto">
          {list.length === 0 && <li className="px-4 py-10 text-center text-[13px] text-ink-500">새 메시지 요청이 없어요</li>}
          {list.map((r) => {
            const a = alumni.find((x) => x.id === r.memberIds[0])!;
            const active = selected?.id === r.id;
            return (
              <li key={r.id} className="border-b border-line-200">
                <button type="button" onClick={() => setSelectedId(r.id)} className={cn("flex w-full flex-col gap-2 px-4 py-4 text-left hover:bg-bg-50", active && "bg-blue-100 hover:bg-blue-100")}>
                  <span className="flex items-center gap-2.5">
                    <Avatar name={a.name} part={a.part} size={32} />
                    <span className="flex flex-col">
                      <span>
                        <span className="text-[14px] font-bold text-ink-900">{a.name}</span>
                        <span className="ml-1.5 text-[12px] text-ink-500">
                          {a.generation}기 · {LION_PART_LABEL[a.part]}
                        </span>
                      </span>
                      <span className="text-[12px] text-ink-500">{a.intro}</span>
                    </span>
                  </span>
                  <span className="line-clamp-2 text-[13px] text-ink-700">{r.messages[0]?.text}</span>
                  <span className="text-[12px] text-ink-400">{formatDate(r.messages[0]?.at)}</span>
                </button>
              </li>
            );
          })}
        </ul>
      </aside>

      {selected && from ? (
        <section className="flex flex-col">
          <header className="flex h-16 items-center gap-3 border-b border-line-200 px-6">
            <Avatar name={from.name} part={from.part} size={40} />
            <div className="flex flex-col">
              <span className="t-body-bold text-ink-900">{from.name}</span>
              <span className="text-[12px] text-ink-500">
                {from.generation}기 · {LION_PART_LABEL[from.part]}
              </span>
            </div>
          </header>
          <div className="flex flex-1 flex-col gap-4 px-6 py-5">
            <Banner>
              <span className="flex items-center gap-2">
                <Glyph name="info" size={18} /> {from.name}님이 대화를 요청했어요. 수락하면 {from.name}님이 내가 메시지를 읽었는지 알 수 있어요.
              </span>
            </Banner>
            <div className="flex items-center justify-between">
              <span className="text-[12px] text-ink-400">{formatDateTime(selected.messages[0].at)}</span>
              <Button size="S" variant="secondary" href={`/alumni/${from.id}`}>
                프로필 보기
              </Button>
            </div>
            <p className="flex flex-wrap items-center gap-1.5 text-[12px] text-ink-500">
              나와의 공통점 {commonalities(me, from).map((c) => <Tag key={c}>{c}</Tag>)}
              {commonalities(me, from).length === 0 && <span className="text-ink-400">아직 없어요</span>}
            </p>
            <div className="flex items-start gap-2.5">
              <Avatar name={from.name} part={from.part} size={40} />
              <div className="flex flex-col gap-1">
                <span className="text-[13px] text-ink-700">{from.name}</span>
                <p className="max-w-[480px] rounded-[8px] border border-line-200 bg-white px-3.5 py-2.5 text-[14px] leading-5 text-ink-900">{selected.messages[0].text}</p>
              </div>
            </div>
          </div>
          <footer className="flex items-center justify-end gap-2 border-t border-line-200 px-6 py-4">
            <Button variant="secondary" onClick={() => setDecline(true)}>
              거절
            </Button>
            <Button
              onClick={() => {
                acceptMessageRequest(selected.id);
                toast("요청을 수락했어요");
                router.push(`/chat/${selected.id}`);
              }}
            >
              수락
            </Button>
          </footer>
        </section>
      ) : (
        <EmptyState title="메시지 요청을 선택해주세요" description="새 요청이 오면 여기에 보여요" />
      )}

      {/* ◇[O] T-15 사유 선택 모달 / 메시지 요청 거절 */}
      <Modal
        open={decline}
        onClose={() => setDecline(false)}
        title="메시지 요청 거절"
        description="고른 사유에 맞는 안내가 상대에게 자동으로 전달돼요"
        footer={
          <>
            <Button variant="secondary" onClick={() => setDecline(false)}>
              취소
            </Button>
            <Button
              disabled={reason === "기타" && (etc.trim().length < 5 || etc.length > 100)}
              onClick={() => {
                if (!selected) return;
                declineMessageRequest(selected.id, reason === "기타" ? etc.trim() : reason);
                setDecline(false);
                setSelectedId(null);
                toast("요청을 거절했어요");
              }}
            >
              거절하기
            </Button>
          </>
        }
      >
        <div className="flex flex-col gap-3">
          <p className="t-label2 text-ink-700">사유 *</p>
          {DECLINE_REASONS.map((r) => (
            <Radio key={r} name="decline" label={r} checked={reason === r} onChange={() => setReason(r)} />
          ))}
          {reason === "기타" && <Input placeholder="직접 입력 (5~100자)" value={etc} onChange={(e) => setEtc(e.target.value)} counter={{ value: etc.length, max: 100 }} />}
        </div>
      </Modal>
    </div>
  );
}
