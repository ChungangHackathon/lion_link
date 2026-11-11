"use client";

import { useState } from "react";
import { cn } from "@/lib/cn";
import { useStore } from "@/lib/store";
import { LION_PART_LABEL, type PendingSignup } from "@/lib/types";
import { formatDateTime } from "@/lib/format";
import { StatusBadge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Glyph } from "@/components/ui/Icon";
import { Input, Radio } from "@/components/ui/Input";
import { ConfirmModal, Modal } from "@/components/ui/Modal";
import { Card, EmptyState, Tabs } from "@/components/ui/Misc";
import { useToast } from "@/components/ui/Toast";

const REASONS = ["동문인지 확인할 수 없어요", "초대받은 본인이 아니에요", "기타"];
type Tab = "PENDING" | "APPROVED" | "REJECTED";

/** ★ AD-10 가입 승인 / 승인 대기 탭 · ◇[O] AD-11 반려 사유 · ◇[O] CF-18 · [S] AD-12 */
export default function AdminApprovalsPage() {
  const toast = useToast();
  const { signups, invites, approveSignup, rejectSignup } = useStore();
  const [tab, setTab] = useState<Tab>("PENDING");
  const [checked, setChecked] = useState<string[]>([]);
  const [reject, setReject] = useState<PendingSignup | null>(null);
  const [reason, setReason] = useState(REASONS[0]);
  const [etc, setEtc] = useState("");
  const [confirm, setConfirm] = useState(false);

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
        <h1 className="t-title text-ink-900">가입 승인</h1>
        {tab === "PENDING" && (
          <Button disabled={checked.length === 0} onClick={() => approve(checked)}>
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
        <Card className="overflow-hidden">
          <table className="w-full text-[13px]">
            <thead className="bg-bg-50 text-left text-ink-500">
              <tr>
                {tab === "PENDING" && (
                  <th className="w-10 px-4 py-3">
                    <input type="checkbox" aria-label="전체 선택" checked={checked.length === list.length} onChange={(e) => setChecked(e.target.checked ? list.map((s) => s.id) : [])} className="accent-primary" />
                  </th>
                )}
                <th className="px-4 py-3 font-medium">초대 정보</th>
                <th className="px-4 py-3 font-medium">가입 정보</th>
                <th className="px-4 py-3 font-medium">대조 결과</th>
                <th className="px-4 py-3 font-medium">처리</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-line-200">
              {list.map((s) => {
                const inv = invites.find((i) => i.name === s.name && i.generation === s.generation);
                return (
                  <tr key={s.id} className={cn(checked.includes(s.id) && "bg-light-card")}>
                    {tab === "PENDING" && (
                      <td className="px-4 py-4 align-top">
                        <input type="checkbox" checked={checked.includes(s.id)} onChange={(e) => setChecked((c) => (e.target.checked ? [...c, s.id] : c.filter((x) => x !== s.id)))} className="accent-primary" />
                      </td>
                    )}
                    <td className="px-4 py-4 align-top">
                      <p className="text-[15px] font-bold text-ink-900">
                        {s.name} · {s.generation}기
                      </p>
                      <p className="text-ink-500">초대 이메일 {inv?.email ?? "(안 적음)"}</p>
                      <p className="text-ink-500">메모 {inv?.email ? "카톡으로 보냄" : "-"}</p>
                    </td>
                    <td className="px-4 py-4 align-top">
                      <p className="text-ink-900">{s.email}</p>
                      <p className="text-ink-500">
                        구글 이름 {s.name} · 가입 {formatDateTime(s.appliedAt)}
                      </p>
                      <p className="text-ink-500">{LION_PART_LABEL[s.part]} · 재학</p>
                    </td>
                    <td className="px-4 py-4 align-top">
                      {s.status === "REJECTED" ? (
                        <StatusBadge tone="gray">반려됨</StatusBadge>
                      ) : s.status === "APPROVED" ? (
                        <StatusBadge tone="filled">승인됨</StatusBadge>
                      ) : s.match === "MATCH" ? (
                        <span className="flex items-center gap-1 font-semibold text-green">
                          <Glyph name="check" size={14} /> 일치
                        </span>
                      ) : (
                        <span>
                          <span className="flex items-center gap-1 font-semibold text-orange-over">
                            <Glyph name="alert" size={14} /> 확인 필요
                          </span>
                          <span className="text-[12px] text-ink-500">{inv?.email ? "초대 이메일과 구글 이메일이 달라요" : "초대 이메일을 안 적어둠"}</span>
                        </span>
                      )}
                    </td>
                    <td className="px-4 py-4 align-top">
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
                        <div className="flex flex-col gap-1.5">
                          <p className="text-[12px] text-ink-500">반려 사유: 동문인지 확인할 수 없어요</p>
                          <div className="flex gap-1.5">
                            <Button size="S" variant="secondary" onClick={() => approve([s.id])}>
                              승인으로 바꾸기
                            </Button>
                            <Button size="S" variant="secondary" href="/admin/invites">
                              초대 다시 보내기
                            </Button>
                          </div>
                        </div>
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
