import { cn } from "@/lib/cn";
import type { LionPart } from "@/lib/types";
import { Icon } from "./Icon";

const PART_AVATAR: Record<LionPart, string> = {
  FRONTEND: "bg-part-fe-bg text-part-fe-text",
  BACKEND: "bg-part-be-line text-part-be-text",
  DESIGN: "bg-part-design-bg text-part-design-text",
  PLANNING: "bg-part-plan-bg text-part-plan-text",
  ETC: "bg-light-layout text-ink-700",
};

/**
 * Avatar: initial letter on a part-coloured circle (Figma 2.5+), or the generic
 * user glyph when there is no name (승인 대기·탈퇴).
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
  size?: 32 | 40 | 48 | 64 | 80 | 96;
  withdrawn?: boolean;
  online?: boolean;
  className?: string;
}) {
  const initial = name?.trim().charAt(0);
  const fontSize = size >= 80 ? 36 : size >= 64 ? 28 : size >= 48 ? 20 : size >= 40 ? 18 : 14;
  return (
    <span className={cn("relative inline-grid shrink-0 place-items-center rounded-full font-bold select-none", withdrawn ? "bg-ink-400 text-white" : initial ? PART_AVATAR[part] : "bg-line-200 text-ink-500", className)} style={{ width: size, height: size, fontSize }}>
      {initial && !withdrawn ? initial : <Icon name="user" size={Math.round(size * 0.55)} />}
      {online && <span className="absolute bottom-0 right-0 size-[22%] rounded-[3px] bg-green ring-2 ring-white" />}
    </span>
  );
}

export function AvatarGroup({ people, size = 32 }: { people: { name: string; part?: LionPart }[]; size?: 32 | 40 }) {
  return (
    <span className="flex items-center">
      {people.slice(0, 4).map((p, i) => (
        <Avatar key={i} name={p.name} part={p.part} size={size} className={cn("ring-2 ring-white", i > 0 && "-ml-2")} />
      ))}
      {people.length > 4 && <span className="-ml-2 grid size-8 place-items-center rounded-full bg-bg-100 text-[11px] font-bold text-ink-500 ring-2 ring-white">+{people.length - 4}</span>}
    </span>
  );
}
