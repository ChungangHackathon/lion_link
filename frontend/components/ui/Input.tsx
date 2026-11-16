"use client";

import { Children, isValidElement, useCallback, useEffect, useId, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { cn } from "@/lib/cn";
import { Icon } from "./Icon";

interface FieldProps {
  label?: React.ReactNode;
  hint?: React.ReactNode; // "1~3개" 같은 보조 라벨
  error?: string;
  help?: string;
  readOnly?: boolean;
  counter?: { value: number; max: number };
  className?: string;
  required?: boolean;
}

function Label({ id, label, hint, required }: { id: string; label?: React.ReactNode; hint?: React.ReactNode; required?: boolean }) {
  if (!label) return null;
  return (
    <label htmlFor={id} className="flex items-center gap-1.5 t-body-bold text-ink-700">
      {label}
      {required && <span aria-hidden>*</span>}
      {hint && <span className="t-label2 text-ink-400 font-normal">{hint}</span>}
    </label>
  );
}

const BOX =
  "flex items-center gap-2 h-10 w-full rounded-[6px] border bg-white px-3 text-[15px] leading-5 text-ink-900 placeholder:text-ink-400 outline-none transition-colors";

export function Input({
  label,
  hint,
  error,
  help,
  readOnly,
  counter,
  className,
  required,
  id: idProp,
  ...rest
}: FieldProps & Omit<React.InputHTMLAttributes<HTMLInputElement>, "className">) {
  const auto = useId();
  const id = idProp ?? auto;
  return (
    <div className={cn("flex flex-col gap-1.5 w-full", className)}>
      <Label id={id} label={label} hint={hint} required={required} />
      <div
        className={cn(
          BOX,
          readOnly ? "bg-bg-100 border-line-300 text-ink-500 font-semibold" : "border-line-300 focus-within:border-ink-800 focus-within:border-[1.5px]",
          error && "border-orange",
        )}
      >
        <input id={id} readOnly={readOnly} aria-invalid={!!error} className="min-w-0 flex-1 bg-transparent outline-none" {...rest} />
        {readOnly && <Icon name="lock" size={16} className="text-ink-400" />}
        {counter && (
          <span className={cn("text-[12px] shrink-0", counter.value > counter.max ? "text-orange" : "text-ink-400")}>
            {counter.value}/{counter.max}
          </span>
        )}
      </div>
      {error ? <p className="t-caption2 text-orange">{error}</p> : help ? <p className="t-caption1 text-ink-500">{help}</p> : null}
    </div>
  );
}

export function Textarea({
  label,
  hint,
  error,
  help,
  counter,
  className,
  required,
  rows = 4,
  id: idProp,
  ...rest
}: FieldProps & Omit<React.TextareaHTMLAttributes<HTMLTextAreaElement>, "className">) {
  const auto = useId();
  const id = idProp ?? auto;
  return (
    <div className={cn("flex flex-col gap-1.5 w-full", className)}>
      <Label id={id} label={label} hint={hint} required={required} />
      <div className={cn("relative rounded-[6px] border bg-white focus-within:border-ink-800", error ? "border-orange" : "border-line-300")}>
        <textarea id={id} rows={rows} className="block w-full resize-none bg-transparent px-3 py-2.5 text-[15px] leading-6 text-ink-900 placeholder:text-ink-400 outline-none" {...rest} />
        {counter && (
          <span className={cn("absolute bottom-2 right-3 text-[12px]", counter.value > counter.max ? "text-orange" : "text-ink-400")}>
            {counter.value}/{counter.max}
          </span>
        )}
      </div>
      {error ? <p className="t-caption2 text-orange">{error}</p> : help ? <p className="t-caption1 text-ink-500">{help}</p> : null}
    </div>
  );
}

/** Select가 넘겨주는 값. 예전 <select>의 onChange처럼 e.target.value로 읽는다 */
export type SelectChange = { target: { value: string } };
type SelectOption = { value: string; label: React.ReactNode; disabled: boolean };

/** <option> 자식(map·Fragment·조건부 포함)을 목록으로 읽는다 */
function readOptions(children: React.ReactNode, out: SelectOption[] = []): SelectOption[] {
  Children.forEach(children, (child) => {
    if (!isValidElement(child)) return;
    const props = child.props as { value?: string | number; children?: React.ReactNode; disabled?: boolean };
    if (child.type === "option") {
      out.push({ value: String(props.value ?? props.children ?? ""), label: props.children, disabled: !!props.disabled });
    } else {
      readOptions(props.children, out);
    }
  });
  return out;
}

/**
 * 드롭다운. 브라우저 기본 <select> 목록은 OS(특히 윈도우)마다 모양이 달라서 직접 그린다.
 * 쓰는 법은 <select>와 같다: <option> 자식, value, onChange={(e) => e.target.value}.
 * 목록은 body에 띄워서 모달·카드의 overflow에 잘리지 않고, 아래 자리가 모자라면 위로 연다.
 * 키보드: ↑↓ 이동, Enter·Space 선택, Esc 닫기, Home·End.
 */
export function Select({
  label,
  hint,
  error,
  invalid,
  className,
  triggerClassName,
  required,
  children,
  id: idProp,
  value,
  onChange,
  disabled,
  "aria-label": ariaLabel,
}: Omit<FieldProps, "readOnly" | "counter" | "help"> & {
  children: React.ReactNode;
  id?: string;
  value?: string | number;
  onChange?: (e: SelectChange) => void;
  disabled?: boolean;
  "aria-label"?: string;
  /** 문구 없이 테두리만 빨갛게 (안내 문구를 밖에서 따로 보여줄 때) */
  invalid?: boolean;
  /** 버튼 모양을 바꿀 때 (예: 테두리 없는 정렬 버튼) */
  triggerClassName?: string;
}) {
  const auto = useId();
  const id = idProp ?? auto;
  const listId = `${id}-list`;
  const options = readOptions(children);
  const current = String(value ?? "");
  const selectedIndex = options.findIndex((o) => o.value === current);
  const selected = options[selectedIndex];

  const [open, setOpen] = useState(false);
  const [active, setActive] = useState(-1);
  const [pos, setPos] = useState<React.CSSProperties>({});
  const buttonRef = useRef<HTMLButtonElement>(null);
  const listRef = useRef<HTMLUListElement>(null);

  const place = useCallback(() => {
    const el = buttonRef.current;
    if (!el) return;
    const r = el.getBoundingClientRect();
    const below = window.innerHeight - r.bottom;
    const up = below < 260 && r.top > below;
    const maxHeight = Math.max(120, Math.min(280, (up ? r.top : below) - 16));
    setPos({ left: r.left, minWidth: r.width, maxHeight, ...(up ? { bottom: window.innerHeight - r.top + 4 } : { top: r.bottom + 4 }) });
  }, []);

  const step = (from: number, dir: 1 | -1) => {
    for (let i = from + dir; i >= 0 && i < options.length; i += dir) if (!options[i].disabled) return i;
    return from;
  };

  const show = () => {
    if (disabled) return;
    place();
    setActive(selectedIndex >= 0 && !options[selectedIndex].disabled ? selectedIndex : step(-1, 1));
    setOpen(true);
  };

  const choose = (i: number) => {
    const o = options[i];
    if (!o || o.disabled) return;
    setOpen(false);
    buttonRef.current?.focus();
    if (o.value !== current) onChange?.({ target: { value: o.value } });
  };

  // 바깥을 누르면 닫고, 스크롤·창 크기가 바뀌면 위치를 다시 잡는다
  useEffect(() => {
    if (!open) return;
    const onDown = (e: MouseEvent) => {
      const t = e.target as Node;
      if (!buttonRef.current?.contains(t) && !listRef.current?.contains(t)) setOpen(false);
    };
    document.addEventListener("mousedown", onDown);
    window.addEventListener("resize", place);
    window.addEventListener("scroll", place, true);
    return () => {
      document.removeEventListener("mousedown", onDown);
      window.removeEventListener("resize", place);
      window.removeEventListener("scroll", place, true);
    };
  }, [open, place]);

  // 키보드로 움직인 항목이 보이게
  useEffect(() => {
    if (open && active >= 0) listRef.current?.querySelector<HTMLElement>(`[data-index="${active}"]`)?.scrollIntoView({ block: "nearest" });
  }, [open, active]);

  const onKeyDown = (e: React.KeyboardEvent) => {
    if (disabled) return;
    if (!open) {
      if (["ArrowDown", "ArrowUp", "Enter", " "].includes(e.key)) {
        e.preventDefault();
        show();
      }
      return;
    }
    if (e.key === "ArrowDown") setActive((a) => step(a, 1));
    else if (e.key === "ArrowUp") setActive((a) => step(a, -1));
    else if (e.key === "Home") setActive(step(-1, 1));
    else if (e.key === "End") setActive(step(options.length, -1));
    else if (e.key === "Enter" || e.key === " ") choose(active);
    else if (e.key === "Escape" || e.key === "Tab") {
      setOpen(false);
      if (e.key === "Tab") return;
    } else return;
    e.preventDefault();
  };

  const placeholder = !selected || selected.value === "";

  return (
    <div className={cn("flex flex-col gap-1.5 w-full", className)}>
      <Label id={id} label={label} hint={hint} required={required} />
      <button
        ref={buttonRef}
        id={id}
        type="button"
        role="combobox"
        aria-haspopup="listbox"
        aria-expanded={open}
        aria-controls={open ? listId : undefined}
        aria-activedescendant={open && active >= 0 ? `${listId}-${active}` : undefined}
        aria-label={ariaLabel}
        aria-invalid={!!error || invalid || undefined}
        disabled={disabled}
        onClick={() => (open ? setOpen(false) : show())}
        onKeyDown={onKeyDown}
        className={cn(
          BOX,
          "relative cursor-pointer justify-between pr-9 text-left",
          error || invalid ? "border-orange" : open ? "border-ink-800" : "border-line-300 hover:border-ink-400 focus-visible:border-ink-800",
          disabled && "cursor-not-allowed bg-bg-100 text-ink-400 hover:border-line-300",
          triggerClassName,
        )}
      >
        <span className={cn("min-w-0 flex-1 truncate", placeholder && !disabled && "text-ink-400")}>{selected?.label ?? " "}</span>
        <Icon name="chevron-down" size={16} className={cn("pointer-events-none absolute right-3 text-ink-500 transition-transform", open && "rotate-180")} />
      </button>
      {error && <p className="t-caption2 text-orange">{error}</p>}
      {open &&
        createPortal(
          <ul ref={listRef} id={listId} role="listbox" aria-labelledby={label ? id : undefined} aria-label={label ? undefined : ariaLabel} className="fixed z-[70] overflow-y-auto rounded-[8px] border border-line-200 bg-white p-1.5 shadow-dropdown" style={pos}>
            {options.map((o, i) => (
              <li
                key={`${o.value}-${i}`}
                id={`${listId}-${i}`}
                data-index={i}
                role="option"
                aria-selected={i === selectedIndex}
                aria-disabled={o.disabled || undefined}
                onMouseEnter={() => !o.disabled && setActive(i)}
                onMouseDown={(e) => e.preventDefault()}
                onClick={() => choose(i)}
                className={cn(
                  "flex h-[34px] cursor-pointer items-center justify-between gap-3 whitespace-nowrap rounded-[6px] px-3 text-[14px]",
                  o.disabled ? "cursor-default text-ink-400" : i === active ? "bg-bg-100 text-ink-900" : "text-ink-900",
                  i === selectedIndex && "font-bold",
                )}
              >
                {o.label}
                {i === selectedIndex && <Icon name="check" size={16} className="text-cau" />}
              </li>
            ))}
          </ul>,
          document.body,
        )}
    </div>
  );
}

export function Checkbox({
  label,
  className,
  ...rest
}: { label: React.ReactNode; className?: string } & Omit<React.InputHTMLAttributes<HTMLInputElement>, "className" | "type">) {
  return (
    <label className={cn("inline-flex items-center gap-2 cursor-pointer select-none", className)}>
      <input type="checkbox" className="peer sr-only" {...rest} />
      <span className="grid size-[18px] place-items-center rounded-[4px] border border-ink-400 bg-white peer-checked:border-cau peer-checked:bg-cau peer-disabled:border-line-200 peer-disabled:bg-bg-100 peer-focus-visible:ring-2 peer-focus-visible:ring-primary/40 [&>svg]:opacity-0 peer-checked:[&>svg]:opacity-100">
        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
          <path d="m5 12 5 5L20 7" />
        </svg>
      </span>
      <span className="t-label1 text-ink-900">{label}</span>
    </label>
  );
}

export function Radio({
  label,
  description,
  className,
  ...rest
}: { label: React.ReactNode; description?: React.ReactNode; className?: string } & Omit<React.InputHTMLAttributes<HTMLInputElement>, "className" | "type">) {
  return (
    <label className={cn("flex items-start gap-2 cursor-pointer select-none", className)}>
      <input type="radio" className="peer sr-only" {...rest} />
      <span className="mt-0.5 grid size-[18px] place-items-center rounded-full border border-ink-400 bg-white peer-checked:border-primary peer-checked:[&>span]:scale-100 peer-focus-visible:ring-2 peer-focus-visible:ring-primary/40">
        <span className="size-2.5 scale-0 rounded-full bg-primary transition-transform" />
      </span>
      <span className="flex flex-col">
        <span className="t-label1 text-ink-900">{label}</span>
        {description && <span className="t-caption1 text-ink-500">{description}</span>}
      </span>
    </label>
  );
}

/** Toggle switch (커피챗 받기 ON/OFF) */
export function Switch({ checked, onChange, label, disabled }: { checked: boolean; onChange: (v: boolean) => void; label?: string; disabled?: boolean }) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      aria-label={label}
      disabled={disabled}
      onClick={() => onChange(!checked)}
      className={cn("relative h-[18px] w-[33px] shrink-0 rounded-full transition-colors disabled:opacity-50", checked ? "bg-green" : "bg-line-300")}
    >
      <span className={cn("absolute top-0.5 size-[14px] rounded-full bg-white shadow-sm transition-all", checked ? "left-[17px]" : "left-0.5")} />
    </button>
  );
}
