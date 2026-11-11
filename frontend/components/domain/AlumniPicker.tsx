"use client";

import { useMemo, useState } from "react";
import { cn } from "@/lib/cn";
import type { Alumni } from "@/lib/types";
import { LION_PART_LABEL } from "@/lib/types";
import { useStore } from "@/lib/store";
import { ME_ID } from "@/lib/mock/users";
import { Avatar } from "@/components/ui/Avatar";
import { Button } from "@/components/ui/Button";
import { Glyph } from "@/components/ui/Icon";
import { Modal } from "@/components/ui/Modal";
import { EmptyState } from "@/components/ui/Misc";

/**
 * ★[O] C-20 동문 선택 모달 / 공용 · 다중 선택.
 * Used by R-02 (함께 신청할 동문), L-03 (함께하는 동문), L-11 (번개 초대), K-02 (단일 선택).
 */
export function AlumniPicker({
  open,
  onClose,
  onConfirm,
  title = "동문 선택",
  description,
  max = 4,
  single,
  initial = [],
  exclude = [],
  filter,
  confirmLabel = "추가",
}: {
  open: boolean;
  onClose: () => void;
  onConfirm: (ids: string[]) => void;
  title?: string;
  description?: string;
  max?: number;
  single?: boolean;
  initial?: string[];
  exclude?: string[];
  filter?: (a: Alumni) => boolean;
  confirmLabel?: string;
}) {
  const { alumni, blocked } = useStore();
  const [q, setQ] = useState("");
  const [sel, setSel] = useState<string[]>(initial);

  const list = useMemo(() => {
    const kw = q.trim().toLowerCase();
    return alumni.filter((a) => a.id !== ME_ID && !exclude.includes(a.id) && !blocked.includes(a.id) && (!filter || filter(a)) && (!kw || `${a.name} ${a.generation}기 ${LION_PART_LABEL[a.part]} ${a.headline}`.toLowerCase().includes(kw)));
  }, [alumni, q, exclude, blocked, filter]);

  const toggle = (id: string) => {
    if (single) return setSel([id]);
    setSel((s) => (s.includes(id) ? s.filter((x) => x !== id) : s.length >= max ? s : [...s, id]));
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={title}
      description={description ?? (single ? "한 명을 선택해주세요" : `최대 ${max}명까지 선택할 수 있어요`)}
      width={560}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            취소
          </Button>
          <Button disabled={sel.length === 0} onClick={() => { onConfirm(sel); onClose(); }}>
            {confirmLabel}
            {sel.length > 0 && ` (${sel.length})`}
          </Button>
        </>
      }
    >
      <div className="flex flex-col gap-3">
        <label className="flex h-10 items-center gap-2 rounded-[6px] border border-line-300 px-3 focus-within:border-ink-800">
          <Glyph name="search" className="text-ink-400" />
          <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="이름, 기수, 파트로 검색" className="min-w-0 flex-1 text-[15px] outline-none placeholder:text-ink-400" />
        </label>
        {!single && sel.length > 0 && (
          <div className="flex flex-wrap gap-1.5">
            {sel.map((id) => {
              const a = alumni.find((x) => x.id === id);
              return (
                <button key={id} type="button" onClick={() => toggle(id)} className="inline-flex items-center gap-1 rounded-full bg-part-be-bg px-2.5 py-1 text-[12px] font-semibold text-primary">
                  {a?.name} ✕
                </button>
              );
            })}
          </div>
        )}
        <div className="max-h-[320px] overflow-y-auto rounded-[8px] border border-line-200">
          {list.length === 0 && <EmptyState compact title="찾는 동문이 없어요" description="이름이나 기수를 다시 확인해주세요" />}
          {list.map((a) => {
            const on = sel.includes(a.id);
            const full = !single && !on && sel.length >= max;
            return (
              <button key={a.id} type="button" disabled={full} onClick={() => toggle(a.id)} className={cn("flex w-full items-center gap-3 border-b border-line-200 px-3 py-2.5 text-left last:border-b-0 hover:bg-bg-50 disabled:cursor-not-allowed disabled:opacity-50", on && "bg-part-be-bg/60")}>
                <Avatar name={a.name} part={a.part} size={40} />
                <span className="flex min-w-0 flex-1 flex-col">
                  <span className="t-label1 text-ink-900">
                    {a.name} <span className="ml-1 text-[12px] font-normal text-ink-500">{a.generation}기 · {LION_PART_LABEL[a.part]}</span>
                  </span>
                  <span className="truncate text-[12px] text-ink-500">{a.headline}</span>
                </span>
                <span className={cn("grid size-[18px] place-items-center rounded-[4px] border", single && "rounded-full", on ? "border-primary bg-primary text-white" : "border-ink-400 bg-white")}>{on && <Glyph name="check" size={12} />}</span>
              </button>
            );
          })}
        </div>
      </div>
    </Modal>
  );
}
