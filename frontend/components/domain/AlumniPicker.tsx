"use client";

import { useEffect, useState } from "react";
import { cn } from "@/lib/cn";
import { LION_PART_LABEL } from "@/lib/types";
import { errorMessage } from "@/lib/api/client";
import { alumniHeadline, type AlumniQuery, type AlumniSummary } from "@/lib/api/alumni";
import type { Id } from "@/lib/api/posts";
import { useApi } from "@/lib/api/useApi";
import { useAlumniService } from "@/lib/api/useAlumniService";
import { Avatar } from "@/components/ui/Avatar";
import { Button } from "@/components/ui/Button";
import { Glyph } from "@/components/ui/Icon";
import { Modal } from "@/components/ui/Modal";
import { EmptyState, Skeleton } from "@/components/ui/Misc";

function useDebounced<T>(value: T, ms = 300): T {
  const [v, setV] = useState(value);
  useEffect(() => {
    const t = setTimeout(() => setV(value), ms);
    return () => clearTimeout(t);
  }, [value, ms]);
  return v;
}

/**
 * ★[O] C-20 동문 선택 모달 / 공용 · 다중 선택.
 * Used by R-02 (함께 신청할 동문), L-03 (함께하는 동문), L-11 (번개 초대), K-02 (단일 선택).
 * 동문 목록(ALUMNI-01)을 검색해서 고른다. query로 서버 조건(예: 커피챗 받는 중만)을 더할 수 있다.
 */
export function AlumniPicker(props: PickerProps) {
  // 열 때마다 처음 상태로 시작한다
  if (!props.open) return null;
  return <PickerModal {...props} />;
}

interface PickerProps {
  open: boolean;
  onClose: () => void;
  onConfirm: (ids: Id[], people: AlumniSummary[]) => void;
  title?: string;
  description?: string;
  max?: number;
  single?: boolean;
  /** 처음 선택된 사람들 */
  initial?: AlumniSummary[];
  exclude?: Id[];
  filter?: (a: AlumniSummary) => boolean;
  query?: AlumniQuery;
  confirmLabel?: string;
}

function PickerModal({ onClose, onConfirm, title = "동문 선택", description, max = 4, single, initial = [], exclude = [], filter, query, confirmLabel = "추가" }: PickerProps) {
  const service = useAlumniService();
  const [q, setQ] = useState("");
  const [sel, setSel] = useState<AlumniSummary[]>(initial);
  const keyword = useDebounced(q.trim());
  const { data, error, reload } = useApi((signal) => service.list({ ...query, q: keyword || undefined, size: 50 }, signal), [service, keyword, JSON.stringify(query ?? {})]);

  const excluded = new Set(exclude.map(String));
  const list = (data?.items ?? []).filter((a) => !excluded.has(String(a.id)) && (!filter || filter(a)));
  const isOn = (a: AlumniSummary) => sel.some((x) => String(x.id) === String(a.id));

  const toggle = (a: AlumniSummary) => {
    if (single) return setSel([a]);
    setSel((s) => (s.some((x) => String(x.id) === String(a.id)) ? s.filter((x) => String(x.id) !== String(a.id)) : s.length >= max ? s : [...s, a]));
  };

  return (
    <Modal
      open
      onClose={onClose}
      title={title}
      description={description ?? (single ? "한 명을 선택해주세요" : `최대 ${max}명까지 선택할 수 있어요`)}
      width={560}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            취소
          </Button>
          <Button
            disabled={sel.length === 0}
            onClick={() => {
              onConfirm(
                sel.map((a) => a.id),
                sel,
              );
              onClose();
            }}
          >
            {confirmLabel}
            {sel.length > 0 && ` (${sel.length})`}
          </Button>
        </>
      }
    >
      <div className="flex flex-col gap-3">
        <label className="flex h-10 items-center gap-2 rounded-[6px] border border-line-300 px-3 focus-within:border-ink-800">
          <Glyph name="search" className="text-ink-400" />
          <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="이름, 직무, 기술로 검색" className="min-w-0 flex-1 text-[15px] outline-none placeholder:text-ink-400" />
        </label>
        {!single && sel.length > 0 && (
          <div className="flex flex-wrap gap-1.5">
            {sel.map((a) => (
              <button key={a.id} type="button" onClick={() => toggle(a)} className="inline-flex items-center gap-1 rounded-full bg-part-be-bg px-2.5 py-1 text-[12px] font-semibold text-primary">
                {a.name} ✕
              </button>
            ))}
          </div>
        )}
        <div className="max-h-[320px] overflow-y-auto rounded-[8px] border border-line-200">
          {error && !data ? (
            <EmptyState compact title="동문을 불러오지 못했어요" description={errorMessage(error)} action={{ label: "다시 시도", onClick: reload }} />
          ) : !data ? (
            <div className="flex flex-col gap-2 p-3">
              {Array.from({ length: 4 }, (_, i) => (
                <Skeleton key={i} className="h-[48px] w-full" />
              ))}
            </div>
          ) : list.length === 0 ? (
            <EmptyState compact title="찾는 동문이 없어요" description="이름이나 직무를 다시 확인해주세요" />
          ) : (
            list.map((a) => {
              const on = isOn(a);
              const full = !single && !on && sel.length >= max;
              const part = a.lionParts[0] ?? "ETC";
              return (
                <button key={a.id} type="button" disabled={full} onClick={() => toggle(a)} className={cn("flex w-full items-center gap-3 border-b border-line-200 px-3 py-2.5 text-left last:border-b-0 hover:bg-bg-50 disabled:cursor-not-allowed disabled:opacity-50", on && "bg-part-be-bg/60")}>
                  <Avatar name={a.name} part={part} size={40} />
                  <span className="flex min-w-0 flex-1 flex-col">
                    <span className="t-label1 text-ink-900">
                      {a.name} <span className="ml-1 text-[12px] font-normal text-ink-500">{a.generation}기 · {LION_PART_LABEL[part]}</span>
                    </span>
                    <span className="truncate text-[12px] text-ink-500">{alumniHeadline(a)}</span>
                  </span>
                  <span className={cn("grid size-[18px] place-items-center rounded-[4px] border", single && "rounded-full", on ? "border-primary bg-primary text-white" : "border-ink-400 bg-white")}>{on && <Glyph name="check" size={12} />}</span>
                </button>
              );
            })
          )}
        </div>
      </div>
    </Modal>
  );
}
