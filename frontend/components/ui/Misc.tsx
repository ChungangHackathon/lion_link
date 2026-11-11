import { cn } from "@/lib/cn";
import { Button } from "./Button";

/** EmptyState (문구 표) */
export function EmptyState({ title, description, action, className, compact }: { title: string; description?: string; action?: { label: string; href?: string; onClick?: () => void }; className?: string; compact?: boolean }) {
  return (
    <div className={cn("flex flex-col items-center justify-center gap-2 text-center", compact ? "py-6" : "py-16", className)}>
      <p className="t-body-bold text-ink-700">{title}</p>
      {description && <p className="t-caption1 text-ink-500 whitespace-pre-line">{description}</p>}
      {action && (
        <div className="mt-2">
          {action.href ? (
            <Button variant="secondary" size="S" href={action.href}>
              {action.label}
            </Button>
          ) : (
            <Button variant="secondary" size="S" onClick={action.onClick}>
              {action.label}
            </Button>
          )}
        </div>
      )}
    </div>
  );
}

/** Banner (상태 배너) */
export function Banner({ tone = "info", children, action, className }: { tone?: "info" | "warn" | "error" | "success"; children: React.ReactNode; action?: React.ReactNode; className?: string }) {
  return (
    <div
      className={cn(
        "flex items-center justify-between gap-4 rounded-[8px] px-4 py-3 text-[14px]",
        tone === "info" && "bg-bg-100 text-ink-900",
        tone === "warn" && "bg-orange-back text-ink-900",
        tone === "error" && "bg-orange-back text-orange",
        tone === "success" && "bg-part-be-bg text-primary",
        className,
      )}
    >
      <div className="min-w-0">{children}</div>
      {action}
    </div>
  );
}

/** ProgressBar (프로필 완성도) */
export function ProgressBar({ value, label = "프로필 완성도", className }: { value: number; label?: string; className?: string }) {
  return (
    <div className={cn("flex w-full flex-col gap-2", className)}>
      <div className="flex items-center justify-between text-[18px] font-bold leading-7 text-ink-900">
        <span>{label}</span>
        <span>{value}%</span>
      </div>
      <div className="relative h-2 w-full overflow-hidden rounded-full bg-line-200">
        <div className="absolute inset-y-0 left-0 rounded-full bg-primary transition-all" style={{ width: `${Math.min(100, Math.max(0, value))}%` }} />
      </div>
    </div>
  );
}

/** Skeleton block */
export function Skeleton({ className }: { className?: string }) {
  return <div className={cn("animate-pulse rounded-[6px] bg-bg-100", className)} aria-hidden />;
}

/** Card container */
export function Card({ children, className, as: Comp = "div", ...rest }: { children: React.ReactNode; className?: string; as?: "div" | "section" | "article" } & React.HTMLAttributes<HTMLElement>) {
  return (
    <Comp className={cn("rounded-[12px] border border-line-200 bg-white", className)} {...rest}>
      {children}
    </Comp>
  );
}

/** Divider */
export function Divider({ className }: { className?: string }) {
  return <hr className={cn("border-0 border-t border-line-200", className)} />;
}

/** Tabs (underline or pill) */
export function Tabs<T extends string>({ items, value, onChange, variant = "underline", className }: { items: { value: T; label: string; count?: number }[]; value: T; onChange: (v: T) => void; variant?: "underline" | "pill"; className?: string }) {
  return (
    <div role="tablist" className={cn("flex", variant === "underline" ? "gap-6 border-b border-line-200" : "gap-2", className)}>
      {items.map((it) => {
        const active = it.value === value;
        return (
          <button
            key={it.value}
            role="tab"
            type="button"
            aria-selected={active}
            onClick={() => onChange(it.value)}
            className={cn(
              "inline-flex items-center gap-1.5 whitespace-nowrap transition-colors",
              variant === "underline" && cn("-mb-px border-b-2 pb-2.5 pt-1 text-[15px] font-semibold", active ? "border-navy text-navy" : "border-transparent text-ink-500 hover:text-ink-900"),
              variant === "pill" && cn("rounded-[10px] border px-[15px] py-[5px] t-label2", active ? "border-light-layout bg-light-text text-white" : "border-light-layout text-ink-500 hover:bg-bg-100"),
            )}
          >
            {it.label}
            {!!it.count && <span className="inline-flex h-[17px] min-w-[17px] items-center justify-center rounded-full bg-orange px-[5px] text-[11px] font-bold leading-none text-white">{it.count}</span>}
          </button>
        );
      })}
    </div>
  );
}

/** Tooltip (hover) */
export function Tooltip({ text, children }: { text: string; children: React.ReactNode }) {
  return (
    <span className="group relative inline-flex">
      {children}
      <span className="pointer-events-none absolute left-1/2 top-full z-20 mt-1.5 -translate-x-1/2 whitespace-nowrap rounded-[6px] bg-ink-900 px-2.5 py-1.5 text-[12px] text-white opacity-0 transition-opacity group-hover:opacity-100">{text}</span>
    </span>
  );
}

/** Prototype-only dashed button ("▶ 프로토타입 전용") — for time-skip demos */
export function ProtoButton({ children, onClick, href }: { children: React.ReactNode; onClick?: () => void; href?: string }) {
  const cls = "inline-flex items-center rounded-[8px] border border-dashed border-ink-400 px-3.5 py-2.5 text-[12px] text-ink-500 hover:bg-bg-100";
  if (href)
    return (
      <a href={href} className={cls}>
        ▶ 프로토타입 전용: {children}
      </a>
    );
  return (
    <button type="button" onClick={onClick} className={cls}>
      ▶ 프로토타입 전용: {children}
    </button>
  );
}
