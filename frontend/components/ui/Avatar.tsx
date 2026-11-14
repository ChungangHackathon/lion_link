import { cn } from "@/lib/cn";
import type { LionPart } from "@/lib/types";
import { Icon } from "./Icon";

/** Avatar/Part (906:32571): 파트 색 배경 + 이름 첫 글자. 프론트=청록, 백엔드=파랑, 디자인=핑크, 기획=보라. */
const PART_AVATAR: Record<LionPart, string> = {
  FRONTEND: "bg-part-fe-bg text-part-fe-text",
  BACKEND: "bg-part-be-bg text-part-be-text",
  DESIGN: "bg-part-design-bg text-part-design-text",
  PLANNING: "bg-part-plan-bg text-part-plan-text",
  ETC: "bg-light-layout text-ink-700",
};

export type AvatarSize = 24 | 28 | 32 | 40 | 48 | 64 | 80 | 90 | 96;

const FONT: Record<AvatarSize, string> = {
  24: "text-[11px] font-semibold",
  28: "text-[12px] font-semibold",
  32: "text-[13px] leading-[18px] font-semibold",
  40: "text-[16px] leading-6 font-semibold tracking-[-0.01em]",
  48: "text-[20px] leading-7 font-bold",
  64: "text-[26px] font-bold tracking-[-0.02em]",
  80: "text-[32px] font-bold tracking-[-0.03em]",
  90: "text-[36px] leading-[46px] font-bold tracking-[-0.03em]",
  96: "text-[36px] leading-[46px] font-bold tracking-[-0.03em]",
};

/**
 * Initial on a part-coloured circle, or the generic user glyph when there is
 * no name (승인 대기) / the member left (탈퇴).
 */
export function Avatar({
  name,
  part = "ETC",
  size = 40,
  withdrawn,
  online,
  className,
}: {
  name?: string;
  part?: LionPart;
  size?: AvatarSize;
  withdrawn?: boolean;
  online?: boolean;
  className?: string;
}) {
  const initial = name?.trim().charAt(0);
  const dot = size >= 80 ? 15 : size >= 48 ? 11 : 9;
  return (
    <span className={cn("relative inline-grid shrink-0 place-items-center rounded-full select-none", FONT[size], withdrawn ? "bg-ink-400 text-white" : initial ? PART_AVATAR[part] : "bg-line-200 text-ink-500", className)} style={{ width: size, height: size }}>
      {initial && !withdrawn ? initial : <Icon name="user" size={Math.round(size * 0.5)} />}
      {online && <span aria-label="커피챗 가능" className="absolute bottom-0 right-0 rounded-full border-2 border-white bg-green" style={{ width: dot, height: dot }} />}
    </span>
  );
}

export function AvatarGroup({ people, size = 32 }: { people: { name: string; part?: LionPart }[]; size?: 28 | 32 | 40 }) {
  return (
    <span className="flex items-center">
      {people.slice(0, 4).map((p, i) => (
        <Avatar key={i} name={p.name} part={p.part} size={size} className={cn("ring-2 ring-white", i > 0 && "-ml-2")} />
      ))}
      {people.length > 4 && (
        <span className="-ml-2 grid place-items-center rounded-full bg-bg-100 text-[11px] font-bold text-ink-500 ring-2 ring-white" style={{ width: size, height: size }}>
          +{people.length - 4}
        </span>
      )}
    </span>
  );
}
