import { cn } from "@/lib/cn";
import { LION_PART_LABEL, type LionPart, type ReceiveMode } from "@/lib/types";

/** Orange numeric count (GNB, tabs, side nav) */
export function CountBadge({ count, className }: { count: number; className?: string }) {
  if (!count) return null;
  return (
    <span className={cn("inline-flex h-[17px] min-w-[17px] items-center justify-center rounded-full bg-orange px-[5px] text-[11px] font-bold leading-none text-white", className)}>
      {count > 99 ? "99+" : count}
    </span>
  );
}

/** StatusBadge — 모양 3종(채움/테두리/회색)에 라벨만 바꿔 씀 */
export type StatusTone = "filled" | "outline" | "gray";
export function StatusBadge({ tone = "gray", children, className }: { tone?: StatusTone; children: React.ReactNode; className?: string }) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-[4px] px-2.5 py-[3px] t-label2 whitespace-nowrap",
        tone === "filled" && "bg-primary text-white",
        tone === "outline" && "bg-white border border-ink-600 text-ink-600 text-[12px]",
        tone === "gray" && "bg-bg-100 text-ink-500",
        className,
      )}
    >
      {children}
    </span>
  );
}

/** Badge/Availability: 가능 · 쉬는 중 · 받지 않음 · 예정 */
export function AvailabilityBadge({ mode, note, className, small }: { mode: ReceiveMode; note?: string; className?: string; small?: boolean }) {
  const map: Record<ReceiveMode, { dot: string; text: string; box: string; label: string }> = {
    OPEN: { dot: "bg-green", text: "text-green", box: "bg-white border border-green", label: "커피챗 가능" },
    RESTING: { dot: "bg-ink-400", text: "text-ink-500", box: "bg-bg-100", label: note ?? "쉬는 중" },
    CLOSED: { dot: "bg-line-300", text: "text-ink-400", box: "bg-bg-100", label: "커피챗 받지 않음" },
    SCHEDULED: { dot: "bg-ink-500", text: "text-ink-700", box: "bg-white border border-line-300", label: note ?? "곧 받아요" },
  };
  const m = map[mode];
  return (
    <span className={cn("inline-flex items-center gap-1 rounded-full font-bold whitespace-nowrap", small ? "h-[21px] px-1.5 text-[9px]" : "h-[26px] px-2 text-[13px]", m.box, m.text, className)}>
      <span className={cn("rounded-[3px]", small ? "size-1" : "size-1.5", m.dot)} />
      {m.label}
    </span>
  );
}

/** 파트 · 기수 태그 ("백엔드 ・7기") with part colour */
export const PART_STYLE: Record<LionPart, { bg: string; text: string; ring: string }> = {
  FRONTEND: { bg: "bg-part-fe-bg", text: "text-part-fe-text", ring: "bg-part-fe-line" },
  BACKEND: { bg: "bg-part-be-bg", text: "text-part-be-text", ring: "bg-part-be-line" },
  DESIGN: { bg: "bg-part-design-bg", text: "text-part-design-text", ring: "bg-part-design-line" },
  PLANNING: { bg: "bg-part-plan-bg", text: "text-part-plan-text", ring: "bg-part-plan-line" },
  ETC: { bg: "bg-bg-100", text: "text-ink-700", ring: "bg-light-layout" },
};

export function PartTag({ part, generation, className }: { part: LionPart; generation?: number; className?: string }) {
  return (
    <span className={cn("inline-flex items-center rounded-[5px] px-2.5 py-[3px] t-label2 text-ink-900 whitespace-nowrap", PART_STYLE[part].bg, className)}>
      {LION_PART_LABEL[part]}
      {generation !== undefined && ` ・${generation}기`}
    </span>
  );
}

/** Gray keyword tag ("Spring", "Kotlin") */
export function Tag({ children, className, onClick }: { children: React.ReactNode; className?: string; onClick?: () => void }) {
  const Comp = onClick ? "button" : "span";
  return (
    <Comp type={onClick ? "button" : undefined} onClick={onClick} className={cn("inline-flex items-center rounded-[2px] bg-light-card px-[7px] py-0.5 text-[11px] leading-[13px] text-ink-900 whitespace-nowrap", onClick && "hover:bg-line-200", className)}>
      {children}
    </Comp>
  );
}

/** Outlined small tag ("여러 명 신청 가능") */
export function OutlineTag({ children, className }: { children: React.ReactNode; className?: string }) {
  return <span className={cn("inline-flex items-center rounded-[4px] border border-line-300 px-2 py-0.5 text-[12px] text-ink-500 whitespace-nowrap", className)}>{children}</span>;
}
