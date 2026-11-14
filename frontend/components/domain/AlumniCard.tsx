"use client";

import Link from "next/link";
import { useState } from "react";
import { cn } from "@/lib/cn";
import { errorMessage } from "@/lib/api/client";
import { alumniHeadline, cannotRequestReason, type AlumniSummary } from "@/lib/api/alumni";
import { useAlumniService } from "@/lib/api/useAlumniService";
import { Avatar } from "@/components/ui/Avatar";
import { PART_STYLE, PartTag, Tag } from "@/components/ui/Badge";
import { Icon } from "@/components/ui/Icon";
import { Tooltip } from "@/components/ui/Misc";
import { useToast } from "@/components/ui/Toast";

const PENDING_TIP = "곧 연결될 기능이에요";

/**
 * ☆ 관심 동문 저장 (ALUMNI-04·05). 누르면 바로 바뀌고, 실패하면 되돌린다.
 * override를 주면 서버 대신 그 함수를 부른다 (아직 mock 데이터를 쓰는 홈 화면용).
 */
export function useBookmarkToggle(person: Pick<AlumniSummary, "id" | "bookmarked">, override?: (next: boolean) => void) {
  const service = useAlumniService();
  const toast = useToast();
  const [saved, setSaved] = useState(person.bookmarked);
  const [busy, setBusy] = useState(false);
  // 목록을 다시 불러와 값이 바뀌면 따라간다
  const [prev, setPrev] = useState(person.bookmarked);
  if (prev !== person.bookmarked) {
    setPrev(person.bookmarked);
    setSaved(person.bookmarked);
  }
  const toggle = async () => {
    if (busy) return;
    const next = !saved;
    setSaved(next);
    if (override) return override(next);
    setBusy(true);
    try {
      await service.setBookmark(person.id, next);
    } catch (e) {
      setSaved(!next);
      toast(errorMessage(e), { kind: "error" });
    } finally {
      setBusy(false);
    }
  };
  return { saved, toggle };
}

/**
 * Card/Alumni (수정 화면 S-01·H-01): 파트 색 커버, 흰 테두리 이니셜 아바타 + 커피챗 가능 점,
 * 이름 · 파트 배지 · ☆, 소속, 소개 2줄, 대표 기술, [커피챗 신청] [💬].
 * 커피챗 신청 → R-01, 💬 → 메시지(T-04). 받지 않는 동문은 비활성 + 툴팁.
 * onRequest·onMessage가 없으면(커피챗·채팅 API 연결 전) 버튼을 잠근다.
 */
export function AlumniCard({ person, onRequest, onMessage, onBookmark, className }: { person: AlumniSummary; onRequest?: (p: AlumniSummary) => void; onMessage?: (p: AlumniSummary) => void; onBookmark?: (next: boolean) => void; className?: string }) {
  const { saved, toggle } = useBookmarkToggle(person, onBookmark);
  const part = person.lionParts[0] ?? "ETC";
  const canRequest = person.coffeeChat.availability === "AVAILABLE";
  const style = PART_STYLE[part];

  return (
    <article className={cn("group relative flex w-[264px] flex-col overflow-hidden rounded-[11px] border border-light-layout bg-bg-50 transition-shadow hover:shadow-[0_4px_16px_rgba(17,24,39,0.08)]", className)}>
      <Link href={`/alumni/${person.id}`} className="block">
        <div className={cn("h-[60px] w-full", style.ring)} />
        <div className="absolute left-[7px] top-[18px]">
          <Avatar name={person.name} part={part} size={90} online={canRequest} className="border-[6px] border-bg-50" />
        </div>
        <div className="flex flex-col gap-2.5 px-5 pb-[14px] pt-[55px]">
          <div className="flex h-[26px] items-center gap-2 pr-7">
            <h3 className="shrink-0 text-[18px] font-bold leading-[22px] text-ink-900">{person.name}</h3>
            <PartTag part={part} generation={person.generation} className="min-w-0 truncate" />
          </div>
          <p className="line-clamp-2 min-h-4 text-[13px] leading-4 text-ink-700">{alumniHeadline(person)}</p>
          <hr className="border-line-200" />
          <p className="line-clamp-2 h-[36px] text-[14px] leading-[18px] text-ink-500">{person.bio}</p>
          <div className="flex h-[17px] items-center gap-2 overflow-hidden">
            {person.skills.slice(0, 3).map((s) => (
              <Tag key={s}>{s}</Tag>
            ))}
            {person.skills.length > 3 && <Tag>+{person.skills.length - 3}</Tag>}
          </div>
        </div>
      </Link>

      <button type="button" aria-label={saved ? "관심 동문 해제" : "관심 동문 저장"} aria-pressed={saved} onClick={toggle} className="absolute right-[16px] top-[115px] rounded p-0.5 text-navy hover:bg-line-200">
        <Icon name={saved ? "star-filled" : "star"} size={21} />
      </button>

      <div className="flex gap-2 px-5 pb-5">
        {canRequest && onRequest ? (
          <button type="button" onClick={() => onRequest(person)} className="h-8 min-w-0 flex-1 rounded-[6px] bg-primary px-3 text-[12px] font-semibold leading-4 text-white hover:bg-primary-hover">
            커피챗 신청
          </button>
        ) : (
          <Tooltip text={canRequest ? PENDING_TIP : cannotRequestReason(person.coffeeChat)} className="min-w-0 flex-1">
            <span className="flex h-8 w-full cursor-not-allowed items-center justify-center rounded-[6px] bg-line-200 px-3 text-[12px] font-semibold leading-4 text-ink-400">커피챗 신청</span>
          </Tooltip>
        )}
        {onMessage ? (
          <button type="button" aria-label="메시지 보내기" onClick={() => onMessage(person)} className="grid h-8 w-[49px] shrink-0 place-items-center rounded-[6px] border border-line-300 text-ink-900 hover:bg-white">
            <Icon name="chat" size={17} />
          </button>
        ) : (
          <Tooltip text={PENDING_TIP} className="shrink-0">
            <span aria-label="메시지 보내기" className="grid h-8 w-[49px] cursor-not-allowed place-items-center rounded-[6px] border border-line-200 text-ink-400">
              <Icon name="chat" size={17} />
            </span>
          </Tooltip>
        )}
      </div>
    </article>
  );
}
