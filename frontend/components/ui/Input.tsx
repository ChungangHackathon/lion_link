"use client";

import { useId } from "react";
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
        {readOnly && <Icon name="lock" />}
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

export function Select({
  label,
  hint,
  error,
  className,
  required,
  children,
  id: idProp,
  ...rest
}: FieldProps & Omit<React.SelectHTMLAttributes<HTMLSelectElement>, "className">) {
  const auto = useId();
  const id = idProp ?? auto;
  return (
    <div className={cn("flex flex-col gap-1.5 w-full", className)}>
      <Label id={id} label={label} hint={hint} required={required} />
      <div className={cn(BOX, "relative pr-9", error ? "border-orange" : "border-line-300 focus-within:border-ink-800")}>
        <select id={id} className="min-w-0 flex-1 appearance-none bg-transparent outline-none" {...rest}>
          {children}
        </select>
        <Icon name="chevron-down" className="pointer-events-none absolute right-3" />
      </div>
      {error && <p className="t-caption2 text-orange">{error}</p>}
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
      <span className="grid size-[18px] place-items-center rounded-[4px] border border-ink-400 bg-white peer-checked:border-primary peer-checked:bg-primary peer-focus-visible:ring-2 peer-focus-visible:ring-primary/40 [&>svg]:opacity-0 peer-checked:[&>svg]:opacity-100">
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
      className={cn("relative h-6 w-11 shrink-0 rounded-full transition-colors disabled:opacity-50", checked ? "bg-green" : "bg-line-300")}
    >
      <span className={cn("absolute top-0.5 size-5 rounded-full bg-white shadow transition-all", checked ? "left-[22px]" : "left-0.5")} />
    </button>
  );
}
