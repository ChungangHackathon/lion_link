"use client";

import { useCallback, useEffect, useId, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { cn } from "@/lib/cn";
import { Icon } from "./Icon";
import { Select } from "./Input";

const DAYS = ["일", "월", "화", "수", "목", "금", "토"];
const pad = (n: number) => String(n).padStart(2, "0");
const iso = (y: number, m: number, d: number) => `${y}-${pad(m + 1)}-${pad(d)}`;

/** "2026-11-20" → "2026년 11월 20일 (금)" */
export function dateLabel(value: string) {
  const [y, m, d] = value.split("-").map(Number);
  return `${y}년 ${m}월 ${d}일 (${DAYS[new Date(y, m - 1, d).getDay()]})`;
}

/**
 * 날짜 고르기. 브라우저 기본 date 입력 대신 Select와 같은 상자를 누르면 달력이 열린다.
 * 값은 "YYYY-MM-DD". min·max 밖 날짜는 고를 수 없다.
 */
export function DatePicker({ label, required, value, onChange, min, max, disabled, error }: { label?: string; required?: boolean; value: string; onChange: (v: string) => void; min?: string; max?: string; disabled?: boolean; error?: string }) {
  const id = useId();
  const button = useRef<HTMLButtonElement>(null);
  const panel = useRef<HTMLDivElement>(null);
  const [open, setOpen] = useState(false);
  const [pos, setPos] = useState<React.CSSProperties>({});
  const start = value || min || iso(new Date().getFullYear(), new Date().getMonth(), new Date().getDate());
  const [view, setView] = useState(() => ({ y: Number(start.slice(0, 4)), m: Number(start.slice(5, 7)) - 1 }));

  const place = useCallback(() => {
    const r = button.current?.getBoundingClientRect();
    if (!r) return;
    const up = window.innerHeight - r.bottom < 340 && r.top > 340;
    setPos({ left: r.left, ...(up ? { bottom: window.innerHeight - r.top + 4 } : { top: r.bottom + 4 }) });
  }, []);

  useEffect(() => {
    if (!open) return;
    const onDown = (e: MouseEvent) => {
      const t = e.target as Node;
      if (!button.current?.contains(t) && !panel.current?.contains(t)) setOpen(false);
    };
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && setOpen(false);
    document.addEventListener("mousedown", onDown);
    document.addEventListener("keydown", onKey);
    window.addEventListener("resize", place);
    window.addEventListener("scroll", place, true);
    return () => {
      document.removeEventListener("mousedown", onDown);
      document.removeEventListener("keydown", onKey);
      window.removeEventListener("resize", place);
      window.removeEventListener("scroll", place, true);
    };
  }, [open, place]);

  const show = () => {
    if (disabled) return;
    const base = value || min;
    if (base) setView({ y: Number(base.slice(0, 4)), m: Number(base.slice(5, 7)) - 1 });
    place();
    setOpen(true);
  };
  const move = (delta: number) => setView((v) => ({ y: v.y + Math.floor((v.m + delta) / 12), m: (((v.m + delta) % 12) + 12) % 12 }));

  const first = new Date(view.y, view.m, 1).getDay();
  const days = new Date(view.y, view.m + 1, 0).getDate();
  const today = (() => {
    const n = new Date();
    return iso(n.getFullYear(), n.getMonth(), n.getDate());
  })();

  return (
    <div className="flex min-w-0 w-full flex-col gap-1.5">
      {label && (
        <label htmlFor={id} className="flex items-center gap-1.5 t-body-bold text-ink-700">
          {label}
          {required && <span aria-hidden>*</span>}
        </label>
      )}
      <button
        ref={button}
        id={id}
        type="button"
        aria-haspopup="dialog"
        aria-expanded={open}
        disabled={disabled}
        onClick={() => (open ? setOpen(false) : show())}
        className={cn(
          "relative flex h-10 w-full items-center gap-2 rounded-[6px] border bg-white px-3 pr-9 text-left text-[15px] text-ink-900 outline-none transition-colors",
          error ? "border-orange" : open ? "border-ink-800" : "border-line-300 hover:border-ink-400 focus-visible:border-ink-800",
          disabled && "cursor-not-allowed bg-bg-100 text-ink-400 hover:border-line-300",
        )}
      >
        <span className={cn("min-w-0 flex-1 truncate", !value && "text-ink-400")}>{value ? dateLabel(value) : "날짜 선택"}</span>
        <Icon name="calendar" size={16} className="pointer-events-none absolute right-3 text-ink-500" />
      </button>
      {error && <p className="t-caption2 text-orange">{error}</p>}
      {open &&
        createPortal(
          <div ref={panel} role="dialog" aria-label={label ?? "날짜 선택"} className="fixed z-[70] w-[296px] rounded-[10px] border border-line-200 bg-white p-3 shadow-dropdown" style={pos}>
            <div className="mb-2 flex items-center justify-between">
              <button type="button" aria-label="이전 달" onClick={() => move(-1)} className="rounded p-1 text-ink-500 hover:bg-bg-100">
                <Icon name="chevron-left" size={16} />
              </button>
              <p className="text-[14px] font-bold text-ink-900">
                {view.y}년 {view.m + 1}월
              </p>
              <button type="button" aria-label="다음 달" onClick={() => move(1)} className="rounded p-1 text-ink-500 hover:bg-bg-100">
                <Icon name="chevron-right" size={16} />
              </button>
            </div>
            <div className="grid grid-cols-7 gap-y-1 text-center text-[13px]">
              {DAYS.map((d, i) => (
                <span key={d} className={cn("pb-1 text-[12px]", i === 0 ? "text-orange" : "text-ink-400")}>
                  {d}
                </span>
              ))}
              {Array.from({ length: first }, (_, i) => (
                <span key={`e${i}`} />
              ))}
              {Array.from({ length: days }, (_, i) => i + 1).map((d) => {
                const v = iso(view.y, view.m, d);
                const off = (min && v < min) || (max && v > max);
                const on = v === value;
                return (
                  <button
                    key={d}
                    type="button"
                    disabled={!!off}
                    onClick={() => {
                      onChange(v);
                      setOpen(false);
                      button.current?.focus();
                    }}
                    className={cn("mx-auto grid size-9 place-items-center rounded-full transition-colors", on ? "bg-primary-strong font-bold text-white" : off ? "cursor-default text-line-300" : "text-ink-900 hover:bg-bg-100", !on && v === today && "font-bold text-primary-strong")}
                  >
                    {d}
                  </button>
                );
              })}
            </div>
          </div>,
          document.body,
        )}
    </div>
  );
}

/** "19:30" → "오후 7:30" */
export function timeLabel(value: string) {
  const [h, m] = value.split(":").map(Number);
  return `${h < 12 ? "오전" : "오후"} ${h % 12 === 0 ? 12 : h % 12}:${pad(m)}`;
}

const SLOTS = Array.from({ length: 48 }, (_, i) => `${pad(Math.floor(i / 2))}:${i % 2 ? "30" : "00"}`);

/** 시각 고르기. 30분 단위 목록 (브라우저 기본 time 입력 대신) */
export function TimeSelect({ label, required, value, onChange, disabled, error }: { label?: string; required?: boolean; value: string; onChange: (v: string) => void; disabled?: boolean; error?: string }) {
  // 예전 글의 시각이 30분 단위가 아니면 목록에 넣어 둔다
  const options = value && !SLOTS.includes(value) ? [...SLOTS, value].sort() : SLOTS;
  return (
    <Select label={label} required={required} value={value} onChange={(e) => onChange(e.target.value)} disabled={disabled} error={error}>
      <option value="" disabled>
        시각 선택
      </option>
      {options.map((t) => (
        <option key={t} value={t}>
          {timeLabel(t)}
        </option>
      ))}
    </Select>
  );
}
