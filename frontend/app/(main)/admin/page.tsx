"use client";

import { useState } from "react";
import { cn } from "@/lib/cn";
import { LION_PART_LABEL, PROFILE_STATUS_LABEL } from "@/lib/types";
import { formatDateTime } from "@/lib/format";
import { errorMessage, isApiError } from "@/lib/api/client";
import { REJECT_REASONS, type RejectReasonType, type SignupItem, type SignupStatus } from "@/lib/api/admin";
import type { Id } from "@/lib/api/posts";
import { useApi } from "@/lib/api/useApi";
import { useAdminService, useRefreshPendingCount } from "@/lib/api/useAdminService";
import { Button } from "@/components/ui/Button";
import { Icon } from "@/components/ui/Icon";
import { Input, Radio } from "@/components/ui/Input";
import { ConfirmModal, Modal } from "@/components/ui/Modal";
import { Card, EmptyState, Skeleton, Tabs } from "@/components/ui/Misc";
import { useToast } from "@/components/ui/Toast";

const EMPTY: Record<SignupStatus, string> = { PENDING: "승인 대기 중인 가입이 없어요", APPROVED: "승인된 가입이 없어요", REJECTED: "반려된 가입이 없어요" };
const BATCH_MAX = 50;

/** ★ AD-10 가입 승인 / 승인 대기 탭 · ◇[O] AD-11 반려 사유 · ◇[O] CF-18 · [S] AD-12 (ADMIN-01 ~ 06) */
export default function AdminApprovalsPage() {
  const toast = useToast();
  const service = useAdminService();
  const refreshCount = useRefreshPendingCount();
  const [tab, setTab] = useState<SignupStatus>("PENDING");
  const { data, error, reload, setData } = useApi((signal) => service.list(tab, 0, signal), [service, tab]);
  const [checked, setChecked] = useState<Id[]>([]);
  const [busy, setBusy] = useState<Id | "batch" | "more" | null>(null);
  const [reject, setReject] = useState<SignupItem | null>(null);
  const [confirmReject, setConfirmReject] = useState<{ type: RejectReasonType; text: string } | null>(null);
  const [editing, setEditing] = useState<{ item: SignupItem; name: string; generation: string } | null>(null);

  const list = data?.items ?? [];
  const isChecked = (id: Id) => checked.includes(id);

  /** 처리한 사람은 지금 탭에서 빼고, 메뉴 숫자를 다시 센다 */
  const removeRows = (ids: Id[]) => {
    setData((d) => d && { ...d, items: d.items.filter((x) => !ids.includes(x.userId)), totalElements: Math.max(0, d.totalElements - ids.length) });
    setChecked((c) => c.filter((x) => !ids.includes(x)));
    refreshCount();
  };

  /** 다른 관리자가 먼저 처리했으면(409·404) 목록을 새로 불러온다 */
  const fail = (e: unknown) => {
    toast(errorMessage(e), { kind: "error" });
    if (isApiError(e) && (e.status === 409 || e.status === 404)) {
      reload();
      refreshCount();
    }
  };

  const approveOne = async (s: SignupItem) => {
    setBusy(s.userId);
    try {
      const res = await service.approve(s.userId);
      removeRows([s.userId]);
      toast(`${res.name}님을 승인했어요. 승인 메일이 발송돼요.`);
    } catch (e) {
      fail(e);
    } finally {
      setBusy(null);
    }
  };

  const approveChecked = async () => {
    setBusy("batch");
    try {
      const res = await service.approveBatch(checked);
      removeRows(res.succeeded.map((x) => x.userId));
      const failed = res.failed.length ? ` ${res.failed.length}명은 이미 처리됐거나 탈퇴해서 승인하지 못했어요.` : "";
      toast(`${res.succeeded.length}명을 승인했어요.${failed}`, res.failed.length ? { kind: "error" } : undefined);
      if (res.failed.length) reload();
    } catch (e) {
      fail(e);
    } finally {
      setBusy(null);
    }
  };

  const doReject = async () => {
    if (!reject || !confirmReject) return;
    setBusy(reject.userId);
    try {
      await service.reject(reject.userId, confirmReject.type, confirmReject.type === "ETC" ? confirmReject.text.trim() : undefined);
      removeRows([reject.userId]);
      toast("반려했어요. 본인이 다시 접속하면 사유가 보여요.");
    } catch (e) {
      fail(e);
    } finally {
      setBusy(null);
      setConfirmReject(null);
      setReject(null);
    }
  };

  const saveInfo = async () => {
    if (!editing) return;
    const body = { name: editing.name.trim(), generation: Number(editing.generation) };
    try {
      const res = await service.updateInfo(editing.item.userId, body);
      setData((d) => d && { ...d, items: d.items.map((x) => (x.userId === editing.item.userId ? { ...x, name: res.name, generation: res.generation } : x)) });
      setEditing(null);
      toast("이름·기수를 고쳤어요");
    } catch (e) {
      toast(errorMessage(e), { kind: "error" });
    }
  };

  const loadMore = async () => {
    if (!data) return;
    setBusy("more");
    try {
      const next = await service.list(tab, data.page + 1);
      setData((d) => d && { ...next, items: [...d.items, ...next.items.filter((x) => !d.items.some((y) => y.userId === x.userId))] });
    } catch (e) {
      toast(errorMessage(e), { kind: "error" });
    } finally {
      setBusy(null);
    }
  };

  const editValid = !!editing && editing.name.trim().length >= 2 && editing.name.trim().length <= 20 && Number(editing.generation) >= 1 && Number(editing.generation) <= 14;

  return (
    <div className="flex flex-col gap-5">
      <div className="flex items-center justify-between">
        <h1 className="t-title3 text-ink-900">가입 승인</h1>
        {tab === "PENDING" && (
          <Button size="S" disabled={checked.length === 0} loading={busy === "batch"} onClick={() => void approveChecked()}>
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
          { value: "PENDING", label: "승인 대기", count: tab === "PENDING" ? data?.totalElements : undefined },
          { value: "APPROVED", label: "승인됨" },
          { value: "REJECTED", label: "반려됨" },
        ]}
      />

      {error && !data ? (
        <EmptyState title="가입 목록을 불러오지 못했어요" description={errorMessage(error)} action={{ label: "다시 시도", onClick: reload }} />
      ) : !data ? (
        <Skeleton className="h-[320px] w-full" />
      ) : list.length === 0 ? (
        <EmptyState title={EMPTY[tab]} />
      ) : (
        <Card className="overflow-hidden rounded-[10px]">
          <table className="w-full text-[13px]">
            <thead className="bg-bg-100 text-left text-ink-900">
              <tr>
                {tab === "PENDING" && (
                  <th className="w-12 px-4 py-3">
                    <Check label="전체 선택" checked={list.length > 0 && list.slice(0, BATCH_MAX).every((x) => isChecked(x.userId))} onChange={(v) => setChecked(v ? list.slice(0, BATCH_MAX).map((x) => x.userId) : [])} />
                  </th>
                )}
                <th className="w-[200px] px-3 py-3 t-label2">이름·기수</th>
                <th className="px-3 py-3 t-label2">구글 계정 · 입력 정보</th>
                <th className="w-[220px] px-3 py-3 t-label2">참고</th>
                <th className="w-[150px] px-3 py-3 t-label2">처리</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-line-200">
              {list.map((s) => {
                const info = [s.lionParts.map((x) => LION_PART_LABEL[x]).join("·"), s.currentStatus && PROFILE_STATUS_LABEL[s.currentStatus], s.company].filter(Boolean).join(" · ");
                return (
                  <tr key={String(s.userId)} className={cn(isChecked(s.userId) && "bg-blue-50")}>
                    {tab === "PENDING" && (
                      <td className="px-4 py-4 align-top">
                        <Check
                          label={`${s.name} 선택`}
                          checked={isChecked(s.userId)}
                          onChange={(v) => {
                            if (v && checked.length >= BATCH_MAX) return toast(`한 번에 ${BATCH_MAX}명까지 승인할 수 있어요`, { kind: "error" });
                            setChecked((c) => (v ? [...c, s.userId] : c.filter((x) => x !== s.userId)));
                          }}
                        />
                      </td>
                    )}
                    <td className="px-3 py-4 align-top">
                      <p className="flex items-center gap-3 t-label2 text-ink-900">
                        {s.name} · {s.generation}기
                        {tab !== "REJECTED" && (
                          <button type="button" aria-label={`${s.name} 이름·기수 고치기`} onClick={() => setEditing({ item: s, name: s.name, generation: String(s.generation) })} className="rounded p-0.5 text-ink-700 hover:bg-bg-100">
                            <Icon name="edit" size={14} />
                          </button>
                        )}
                      </p>
                    </td>
                    <td className="px-3 py-4 align-top">
                      <p className="t-label2 text-ink-900">{s.email}</p>
                      <p className="pt-1 text-[12px] text-ink-700">
                        {s.googleName && `구글 이름 ${s.googleName} · `}가입 {formatDateTime(s.signedUpAt)}
                      </p>
                      <p className="pt-0.5 text-[12px] text-ink-500">{info || "아직 프로필 입력 전"}</p>
                    </td>
                    <td className="px-3 py-4 align-top">
                      <Note s={s} tab={tab} />
                    </td>
                    <td className="px-3 py-4 align-top">
                      {tab === "PENDING" && (
                        <div className="flex gap-1.5">
                          <Button size="S" loading={busy === s.userId} onClick={() => void approveOne(s)}>
                            승인
                          </Button>
                          <Button size="S" variant="secondary" onClick={() => setReject(s)}>
                            반려
                          </Button>
                        </div>
                      )}
                      {tab === "REJECTED" && (
                        <Button size="S" variant="secondary" loading={busy === s.userId} onClick={() => void approveOne(s)}>
                          승인으로 바꾸기
                        </Button>
                      )}
                      {tab === "APPROVED" && <span className="text-[12px] text-ink-500">{s.reviewedAt ? "승인 메일 발송됨" : "승인 없이 시작한 계정"}</span>}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
          {data.hasNext && (
            <div className="border-t border-line-200 p-3 text-center">
              <Button size="S" variant="secondary" loading={busy === "more"} onClick={() => void loadMore()}>
                더 보기
              </Button>
            </div>
          )}
        </Card>
      )}

      {/* 이름·기수 고치기 (AD-10 연필, ADMIN-06) */}
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
            <Button size="S" disabled={!editValid} onClick={() => void saveInfo()}>
              저장
            </Button>
          </>
        }
      >
        <div className="flex flex-col gap-2">
          <div className="flex gap-3">
            <Input label="이름" value={editing?.name ?? ""} onChange={(e) => setEditing((s) => s && { ...s, name: e.target.value })} counter={{ value: editing?.name.trim().length ?? 0, max: 20 }} />
            <Input label="기수" inputMode="numeric" value={editing?.generation ?? ""} onChange={(e) => setEditing((s) => s && { ...s, generation: e.target.value.replace(/[^0-9]/g, "") })} />
          </div>
          <p className="text-[12px] text-ink-500">이름 2~20자, 기수 1~14. 기수를 바꾸면 가입 기수 멋사 활동도 같이 바뀌어요.</p>
        </div>
      </Modal>

      {/* ◇[O] AD-11 사유 선택 모달 / 가입 반려 (ADMIN-04) */}
      {reject && !confirmReject && <RejectModal name={reject.name} onClose={() => setReject(null)} onNext={(type, text) => setConfirmReject({ type, text })} />}
      {/* ◇[O] CF-18 가입 반려 확인 */}
      <ConfirmModal
        open={!!confirmReject}
        onClose={() => setConfirmReject(null)}
        onConfirm={() => void doReject()}
        loading={!!reject && busy === reject.userId}
        title={`${reject?.name}님의 가입을 반려할까요? 본인에게 사유가 전달돼요.`}
        description="본인이 다시 접속하면 사유가 보여요."
        confirmLabel="반려"
        danger
      />
    </div>
  );
}

function Note({ s, tab }: { s: SignupItem; tab: SignupStatus }) {
  if (tab === "REJECTED") return <p className="text-[12px] text-ink-700">반려 사유: {s.rejectReason ?? "-"}</p>;
  if (tab === "APPROVED")
    return s.reviewedAt ? (
      <p className="text-[12px] text-ink-700">
        {formatDateTime(s.reviewedAt)}
        {s.reviewedBy && ` · ${s.reviewedBy}`}
      </p>
    ) : (
      <span className="text-ink-500">–</span>
    );
  if (!s.sameNameGeneration && !s.reapplied) return <span className="text-ink-500">–</span>;
  return (
    <div className="flex flex-col items-start gap-1.5">
      {s.sameNameGeneration && <span className="rounded-[4px] bg-orange-back px-2 py-1 text-[12px] font-semibold text-orange-badge">같은 이름·기수 있음</span>}
      {s.reapplied && (
        <>
          <span className="rounded-[4px] bg-bg-100 px-2 py-1 text-[12px] font-semibold text-ink-700">재신청{s.reapplyAt && ` · ${formatDateTime(s.reapplyAt)}`}</span>
          {s.rejectReason && <span className="text-[12px] leading-4 text-ink-700">이전 사유: {s.rejectReason}</span>}
        </>
      )}
    </div>
  );
}

function RejectModal({ name, onClose, onNext }: { name: string; onClose: () => void; onNext: (type: RejectReasonType, text: string) => void }) {
  const [type, setType] = useState<RejectReasonType>("UNVERIFIED");
  const [text, setText] = useState("");
  const len = text.trim().length;
  const invalid = type === "ETC" && (len < 5 || len > 100);
  return (
    <Modal
      open
      onClose={onClose}
      title="가입 반려"
      description={`${name}님에게 보여줄 사유를 골라주세요`}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            취소
          </Button>
          <Button disabled={invalid} onClick={() => onNext(type, text)}>
            반려
          </Button>
        </>
      }
    >
      <div className="flex flex-col gap-3">
        <p className="t-label2 text-ink-700">사유 *</p>
        {REJECT_REASONS.map((r) => (
          <Radio key={r.type} name="reject" label={r.label} checked={type === r.type} onChange={() => setType(r.type)} />
        ))}
        {type === "ETC" && <Input aria-label="기타 사유" placeholder="직접 입력 (5~100자)" value={text} onChange={(e) => setText(e.target.value)} counter={{ value: len, max: 100 }} error={text && len < 5 ? "5자 이상 적어주세요" : undefined} />}
      </div>
    </Modal>
  );
}

function Check({ label, checked, onChange }: { label: string; checked: boolean; onChange: (v: boolean) => void }) {
  return (
    <button type="button" role="checkbox" aria-checked={checked} aria-label={label} onClick={() => onChange(!checked)} className={cn("grid size-[18px] place-items-center rounded-[4px] border", checked ? "border-cau bg-cau text-white" : "border-ink-400 bg-white")}>
      {checked && <Icon name="check" size={12} />}
    </button>
  );
}
