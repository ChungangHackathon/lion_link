"use client";

import Link from "next/link";
import { cn } from "@/lib/cn";
import type { Alumni } from "@/lib/types";
import { useStore } from "@/lib/store";
import { Avatar } from "@/components/ui/Avatar";
import { PART_STYLE, PartTag, Tag } from "@/components/ui/Badge";
import { Icon } from "@/components/ui/Icon";
import { Tooltip } from "@/components/ui/Misc";

/**
 * Card/Alumni (90:349, v2.6 variant): part-coloured cover strip, initial avatar
 * with online dot, name + part tag, headline, 2-line intro, skills, quick actions.
 * 커피챗 신청 → R-01, 💬 → 메시지(T-04). Disabled with tooltip when 받지 않음.
 */
export function AlumniCard({ person, onRequest, onMessage, className }: { person: Alumni; onRequest?: (p: Alumni) => void; onMessage?: (p: Alumni) => void; className?: string }) {
  const { bookmarks, toggleBookmark } = useStore();
  const saved = bookmarks.includes(person.id);
  const canRequest = person.receive === "OPEN";
  const style = PART_STYLE[person.part];

  return (
    <article className={cn("group relative flex w-[264px] flex-col overflow-hidden rounded-[11px] border border-light-layout bg-bg-50 transition-shadow hover:shadow-[0_4px_16px_rgba(17,24,39,0.08)]", className)}>
      <Link href={`/alumni/${person.id}`} className="block">
        <div className={cn("h-[60px] w-full", style.ring)} />
        <div className="absolute left-[7px] top-[18px]">
          <Avatar name={person.name} part={person.part} size={80} online={person.receive === "OPEN"} className="ring-[6px] ring-bg-50" />
        </div>
        <div className="flex flex-col gap-2.5 px-5 pb-3 pt-[57px]">
          <div className="flex items-center gap-2">
            <h3 className="text-[18px] font-bold text-ink-900">{person.name}</h3>
            <PartTag part={person.part} generation={person.generation} />
          </div>
          <p className="text-[13px] text-ink-700">{person.headline}</p>
          <hr className="border-line-200" />
          <p className="line-clamp-2 h-[36px] text-[14px] leading-[18px] text-ink-500">{person.intro}</p>
          <div className="flex flex-wrap items-center gap-2">
            {person.skills.slice(0, 3).map((s) => (
              <Tag key={s} className="bg-light-card">
                {s}
              </Tag>
            ))}
            {person.skills.length > 3 && <Tag className="bg-light-card">+{person.skills.length - 3}</Tag>}
          </div>
        </div>
      </Link>

      <button type="button" aria-label={saved ? "관심 동문 해제" : "관심 동문 저장"} aria-pressed={saved} onClick={() => toggleBookmark(person.id)} className="absolute right-[18px] top-[138px] rounded p-0.5 hover:bg-bg-100">
        <Icon name={saved ? "star-filled" : "star"} size={21} />
      </button>

      <div className="flex gap-2 px-5 pb-5">
        {canRequest ? (
          <button type="button" onClick={() => onRequest?.(person)} className="h-8 flex-1 rounded-[6px] bg-primary text-[12px] font-semibold text-white hover:bg-primary-hover">
            커피챗 신청
          </button>
        ) : (
          <Tooltip text={person.receive === "RESTING" ? "지금은 쉬는 중인 동문이에요" : person.receive === "SCHEDULED" ? person.receiveNote ?? "곧 받을 예정이에요" : "커피챗을 받지 않는 동문이에요"}>
            <span className="flex h-8 w-full flex-1 cursor-not-allowed items-center justify-center rounded-[6px] bg-line-200 text-[12px] font-bold text-ink-400">커피챗 신청</span>
          </Tooltip>
        )}
        <button type="button" aria-label="메시지 보내기" onClick={() => onMessage?.(person)} className="grid h-8 w-[49px] place-items-center rounded-[6px] border border-line-300 hover:bg-bg-100">
          <Icon name="chat" size={17} />
        </button>
      </div>
    </article>
  );
}

/** Compact row used in P-01 right rail (지금 커피챗이 가능한 선배들) and 관심 동문 */
export function AlumniRow({ person, trailing }: { person: Alumni; trailing?: React.ReactNode }) {
  const { bookmarks, toggleBookmark } = useStore();
  const saved = bookmarks.includes(person.id);
  return (
    <div className="flex items-center gap-3 border-b border-line-200 px-3 py-3 last:border-b-0">
      <Link href={`/alumni/${person.id}`} className="shrink-0">
        <Avatar name={person.name} part={person.part} size={48} online={person.receive === "OPEN"} />
      </Link>
      <div className="flex min-w-0 flex-1 flex-col gap-1">
        <div className="flex items-center gap-2">
          <Link href={`/alumni/${person.id}`} className="t-body-bold truncate text-ink-900 hover:underline">
            {person.name}
          </Link>
          <PartTag part={person.part} generation={person.generation} className="px-1.5 py-0.5 text-[11px]" />
        </div>
        <p className="truncate text-[12px] text-ink-500">{person.headline}</p>
      </div>
      {trailing ?? (
        <button type="button" aria-label="관심 동문" aria-pressed={saved} onClick={() => toggleBookmark(person.id)} className="rounded p-0.5 hover:bg-bg-100">
          <Icon name={saved ? "star-filled" : "star"} size={18} />
        </button>
      )}
    </div>
  );
}
