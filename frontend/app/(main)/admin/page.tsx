"use client";

import { useState } from "react";
import { cn } from "@/lib/cn";
import { useStore } from "@/lib/store";
import { LION_PART_LABEL, type PendingSignup } from "@/lib/types";
import { formatDateTime } from "@/lib/format";
import { Button } from "@/components/ui/Button";
import { Icon } from "@/components/ui/Icon";
import { Input, Radio } from "@/components/ui/Input";
import { ConfirmModal, Modal } from "@/components/ui/Modal";
import { Card, EmptyState, Tabs } from "@/components/ui/Misc";
import { useToast } from "@/components/ui/Toast";

const REASONS = ["동문인지 확인할 수 없어요", "이름·기수가 명단과 달라요", "기타"];
type Tab = "PENDING" | "APPROVED" | "REJECTED";

/** ★ AD-10 가입 승인 / 승인 대기 탭 · ◇[O] AD-11 반려 사유 · ◇[O] CF-18 · [S] AD-12 */
export default function AdminApprovalsPage() {
  const toast = useToast();
  const { signups, approveSignup, rejectSignup } = useStore();
  const [tab, setTab] = useState<Tab>("PENDING");
  const [checked, setChecked] = useState<string[]>([]);
  const [reject, setReject] = useState<PendingSignup | null>(null);
  const [reason, setReason] = useState(REASONS[0]);
  const [etc, setEtc] = useState("");
  const [confirm, setConfirm] = useState(false);
  const [edits, setEdits] = useState<Record<string, { name: string; generation: number }>>({});
  const [editing, setEditing] = useState<{ id: string; name: string; generation: string } | null>(null);

  const list = signups.filter((s) => s.status === tab);
  const count = (t: Tab) => signups.filter((s) => s.status === t).length;
  const approve = (ids: string[]) => {
    ids.forEach(approveSignup);
    setChecked([]);
    toast(`${ids.length}명을 승인했어요. 승인 메일이 발송돼요.`);
  };

  return (
    <div className="flex flex-col gap-5">
      <div className="flex items-center justify-between">
        <h1 className="t-title3 text-ink-900">가입 승인</h1>
        {tab === "PENDING" && (
          <Button size="S" disabled={checked.length === 0} onClick={() => approve(checked)}>
            선택 승인{checked.length > 0 && ` (${checked.length})`}
          </Button>
        )}
      </div>
      <Tabs
        value={tab}
        onChange={(t) => {
          setTab(t);
          setChecked([]);
        }}
        items={[
          { value: "PENDING", label: "승인 대기", count: count("PENDING") },
          { value: "APPROVED", label: "승인됨" },
          { value: "REJECTED", label: "반려됨", count: count("REJECTED") },
        ]}
      />

      {list.length === 0 ? (
        <EmptyState title={tab === "PENDING" ? "승인 대기 중인 가입이 없어요" : tab === "APPROVED" ? "승인된 가입이 없어요" : "반려된 가입이 없어요"} />
      ) : (
        <Card className="overflow-hidden rounded-[10px]">
          <table className="w-full text-[13px]">
            <thead className="bg-bg-100 text-left text-ink-900">
              <tr>
                {tab === "PENDING" && (
                  <th className="w-12 px-4 py-3">
                    <Check label="전체 선택" checked={checked.length === list.length} onChange={(v) => setChecked(v ? list.map((x) => x.id) : [])} />
                  </th>
                )}
                <th className="w-[200px] px-3 py-3 t-label2">이름·기수</th>
                <th className="px-3 py-3 t-label2">구글 계정 · 입력 정보</th>
                <th className="w-[200px] px-3 py-3 t-label2">참고</th>
                <th className="w-[150px] px-3 py-3 t-label2">처리</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-line-200">
              {list.map((raw) => {
                const s = { ...raw, ...edits[raw.id] };
                return (
                  <tr key={s.id} className={cn(checked.includes(s.id) && "bg-blue-50")}>
                    {tab === "PENDING" && (
                      <td className="px-4 py-4 align-top">
                        <Check label={`${s.name} 선택`} checked={checked.includes(s.id)} onChange={(v) => setChecked((c) => (v ? [...c, s.id] : c.filter((x) => x !== s.id)))} />
                      </td>
                    )}
                    <td className="px-3 py-4 align-top">
                      <p className="flex items-center gap-3 t-label2 text-ink-900">
                        {s.name} · {s.generation}기
                        {s.status === "PENDING" && (
                          <button type="button" aria-label={`${s.name} 이름·기수 고치기`} onClick={() => setEditing({ id: s.id, name: s.name, generation: String(s.generation) })} className="rounded p-0.5 text-ink-700 hover:bg-bg-100">
                            <Icon name="edit" size={14} />
                          </button>
                        )}
                      </p>
                    </td>
                    <td className="px-3 py-4 align-top">
                      <p className="t-label2 text-ink-900">{s.email}</p>
                      <p className="pt-1 text-[12px] text-ink-700">
                        구글 이름 {raw.name} · 가입 {formatDateTime(s.appliedAt)}
                      </p>
                      <p className="pt-0.5 text-[12px] text-ink-500">{LION_PART_LABEL[s.part]} · 재학</p>
                    </td>
                    <td className="px-3 py-4 align-top">
                      {s.status === "REJECTED" ? (
                        <p className="text-[12px] text-ink-700">반려 사유: 동문인지 확인할 수 없어요</p>
                      ) : s.note === "DUPLICATE" ? (
                        <span className="rounded-[4px] bg-orange-back px-2 py-1 text-[12px] font-semibold text-orange-badge">같은 이름·기수 있음</span>
                      ) : s.note === "REAPPLY" ? (
                        <div className="flex flex-col items-start gap-1.5">
                          <span className="rounded-[4px] bg-bg-100 px-2 py-1 text-[12px] font-semibold text-ink-700">재신청</span>
                          <span className="text-[12px] leading-4 text-ink-700">이전 사유: 동문인지 확인할 수 없어요</span>
                        </div>
                      ) : (
                        <span className="text-ink-500">–</span>
                      )}
                    </td>
                    <td className="px-3 py-4 align-top">
                      {s.status === "PENDING" && (
                        <div className="flex gap-1.5">
                          <Button size="S" onClick={() => approve([s.id])}>
                            승인
                          </Button>
                          <Button size="S" variant="secondary" onClick={() => setReject(s)}>
                            반려
                          </Button>
                        </div>
                      )}
                      {s.status === "REJECTED" && (
                        <Button size="S" variant="secondary" onClick={() => approve([s.id])}>
                          승인으로 바꾸기
                        </Button>
                      )}
                      {s.status === "APPROVED" && <span className="text-[12px] text-ink-500">승인 메일 발송됨</span>}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </Card>
      )}

      {/* 이름·기수 고치기 (AD-10 연필) */}
      <Modal
        open={!!editing}
        onClose={() => setEditing(null)}
        title="이름·기수 고치기"
        width={420}
        footer={
          <>
            <Button variant="secondary" size="S" onClick={() => setEditing(null)}>
              취소
            </Button>
            <Button
              size="S"
              disabled={!editing || editing.name.trim().length < 2 || !(Number(editing.generation) >= 1 && Number(editing.generation) <= 14)}
              onClick={() => {
                if (!editing) return;
                setEdits((e) => ({ ...e, [editing.id]: { name: editing.name.trim(), generation: Number(editing.generation) } }));
                setEditing(null);
                toast("이름·기수를 고쳤어요");
              }}
            >
              저장
            </Button>
          </>
        }
      >
        <div className="flex gap-3">
          <Input label="이름" value={editing?.name ?? ""} onChange={(e) => setEditing((s) => s && { ...s, name: e.target.value })} />
          <Input label="기수" inputMode="numeric" value={editing?.generation ?? ""} onChange={(e) => setEditing((s) => s && { ...s, generation: e.target.value.replace(/[^0-9]/g, "") })} />
        </div>
      </Modal>

      {/* ◇[O] AD-11 사유 선택 모달 / 가입 반려 */}
      <Modal
        open={!!reject && !confirm}
        onClose={() => setReject(null)}
        title="가입 반려"
        footer={
          <>
            <Button variant="secondary" onClick={() => setReject(null)}>
              취소
            </Button>
            <Button disabled={reason === "기타" && (etc.trim().length < 5 || etc.length > 100)} onClick={() => setConfirm(true)}>
              반려
            </Button>
          </>
        }
      >
        <div className="flex flex-col gap-3">
          <p className="t-label2 text-ink-700">사유 *</p>
          {REASONS.map((r) => (
            <Radio key={r} name="reject" label={r} checked={reason === r} onChange={() => setReason(r)} />
          ))}
          {reason === "기타" && <Input placeholder="직접 입력 (5~100자)" value={etc} onChange={(e) => setEtc(e.target.value)} counter={{ value: etc.length, max: 100 }} />}
        </div>
      </Modal>
      {/* ◇[O] CF-18 가입 반려 확인 */}
      <ConfirmModal
        open={confirm}
        onClose={() => setConfirm(false)}
        onConfirm={() => {
          if (reject) rejectSignup(reject.id);
          setConfirm(false);
          setReject(null);
          toast("반려했어요. 본인이 다시 접속하면 사유가 보여요.");
        }}
        title={`${reject?.name}님의 가입을 반려할까요? 본인에게 사유가 전달돼요.`}
        description="본인이 다시 접속하면 사유가 보여요."
        confirmLabel="반려"
        danger
      />
    </div>
  );
}

function Check({ label, checked, onChange }: { label: string; checked: boolean; onChange: (v: boolean) => void }) {
  return (
    <button type="button" role="checkbox" aria-checked={checked} aria-label={label} onClick={() => onChange(!checked)} className={cn("grid size-[18px] place-items-center rounded-[4px] border", checked ? "border-cau bg-cau text-white" : "border-ink-400 bg-white")}>
      {checked && <Icon name="check" size={12} />}
    </button>
  );
}
