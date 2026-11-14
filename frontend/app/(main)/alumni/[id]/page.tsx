"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, use, useEffect, useState } from "react";
import { useStore } from "@/lib/store";
import { useSession } from "@/lib/session";
import { ME_ID } from "@/lib/mock/users";
import { API_ENABLED, errorMessage, isApiError } from "@/lib/api/client";
import { alumniHeadline, receiveBadge, type AlumniSummary } from "@/lib/api/alumni";
import type { AlumniProfileView } from "@/lib/api/alumniView";
import { useApi } from "@/lib/api/useApi";
import { useAlumniService } from "@/lib/api/useAlumniService";
import { Avatar } from "@/components/ui/Avatar";
import { AvailabilityBadge, OutlineTag, PartTag, Tag } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Skeleton } from "@/components/ui/Misc";
import { Icon } from "@/components/ui/Icon";
import { Card, EmptyState, Tooltip } from "@/components/ui/Misc";
import { useToast } from "@/components/ui/Toast";
import { CoffeeRequestModal } from "@/components/domain/CoffeeRequestModal";
import { MessageRequestModal } from "@/components/domain/MessageRequestModal";
import { ProfileSections } from "@/components/domain/ProfileSections";
import { useBookmarkToggle } from "@/components/domain/AlumniCard";

const PENDING_TIP = "곧 연결될 기능이에요";

/** ★ P-01 동문 프로필 / 다른 동문 — 763 본문 + 387 ActionCard rail. ⊕R-01, →T-04 */
function AlumniProfileInner({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const router = useRouter();
  const preview = useSearchParams().get("preview") === "1";
  const { me } = useSession();
  const service = useAlumniService();
  const isMe = !preview && (API_ENABLED ? !!me && String(me.id) === id : id === ME_ID);

  useEffect(() => {
    if (isMe) router.replace("/me");
  }, [isMe, router]);

  const { data: person, error, reload } = useApi(() => service.get(id), [service, id], !isMe);

  if (isMe) return null;
  if (error && !person) {
    if (isApiError(error) && error.status === 404) return <EmptyState title="동문을 찾을 수 없어요" description="탈퇴했거나 주소가 잘못됐어요" action={{ label: "동문 찾기로", href: "/alumni" }} />;
    return <EmptyState title="프로필을 불러오지 못했어요" description={errorMessage(error)} action={{ label: "다시 시도", onClick: reload }} />;
  }
  if (!person) return <Skeleton className="h-[400px] w-full" />;
  return <AlumniProfile key={String(person.id)} person={person} />;
}

function AlumniProfile({ person }: { person: AlumniProfileView }) {
  const toast = useToast();
  const service = useAlumniService();
  const { alumni } = useStore();
  const { saved, toggle } = useBookmarkToggle({ id: person.id, bookmarked: person.bookmarked });
  const [request, setRequest] = useState(false);
  const [message, setMessage] = useState(false);
  // 커피챗 신청·메시지 모달은 아직 mock 동문으로만 동작한다 (커피챗·채팅 API 연결 전)
  const mockPerson = API_ENABLED ? undefined : alumni.find((a) => a.id === String(person.id));
  const detail = person.detail;

  const openList = useApi((signal) => service.list({ available: true, size: 6 }, signal), [service]).data?.items.filter((a) => String(a.id) !== String(person.id)).slice(0, 5) ?? [];
  const similarParts = person.part !== "ETC" ? [person.part] : undefined;
  const similar = useApi((signal) => service.list({ parts: similarParts, size: 6 }, signal), [service, person.part]).data?.items.filter((a) => String(a.id) !== String(person.id)).slice(0, 5) ?? [];

  const onBookmark = async () => {
    const next = !saved;
    await toggle();
    toast(next ? "관심 동문에 저장했어요" : "관심 동문에서 뺐어요");
  };

  return (
    <div className="grid grid-cols-[minmax(0,763px)_387px] justify-between gap-6">
      <div className="flex flex-col gap-6">
        {/* Header card */}
        <Card className="rounded-[10px] p-[30px]">
          <div className="flex gap-[19px]">
            <div className="flex w-[150px] flex-col items-center gap-2">
              <Avatar name={person.name} part={person.part} size={96} className="!size-[150px] !text-[56px]" />
              <AvailabilityBadge mode={person.receive.mode} note={person.receive.note} className="-mt-5 bg-white" />
            </div>
            <div className="flex flex-1 flex-col gap-4 pt-6">
              <div className="flex items-start justify-between">
                <div className="flex items-center gap-2.5">
                  <h1 className="text-[28px] font-bold leading-[34px] text-ink-900">{person.name}</h1>
                  <PartTag part={person.part} generation={person.generation} />
                  {person.staffLabel && <span className="rounded-[5px] bg-orange-back px-2 py-[3px] text-[12px] font-semibold text-orange-badge">{person.staffLabel}</span>}
                </div>
                <div className="relative flex items-center gap-2">
                  <button type="button" aria-label="관심 동문" aria-pressed={saved} onClick={onBookmark} className="rounded p-1 hover:bg-bg-100">
                    <Icon name={saved ? "star-filled" : "star"} size={24} className="text-navy" />
                  </button>
                </div>
              </div>
              <p className="t-body2 text-ink-700">{person.intro}</p>
            </div>
          </div>

          <div className="mt-6 grid grid-cols-[1fr_300px] gap-6">
            <div className="flex flex-col gap-3 text-[13px]">
              <p className="t-label1 text-ink-900">{person.headline}</p>
              <div className="flex items-center gap-2">
                <span className="text-ink-500">나와의 공통점</span>
                {person.commonalities.length ? person.commonalities.map((c) => <Tag key={c}>{c}</Tag>) : <span className="text-ink-400">아직 없어요</span>}
              </div>
              <div className="flex items-center gap-2">
                <span className="text-ink-500">대표 기술</span>
                {detail.repSkills.map((s) => (
                  <Tag key={s}>{s}</Tag>
                ))}
              </div>
              {person.region && (
                <p className="flex items-center gap-1 text-ink-700">
                  <Icon name="pin" size={14} /> {person.region}
                </p>
              )}
              {!!person.mutualCount && <p className="text-ink-500">함께 아는 동문 {person.mutualCount}명</p>}
            </div>
            <div className="flex flex-col gap-4">
              {person.educations.map((e, i) => (
                <div key={i} className="flex items-center gap-3">
                  <span className="grid size-10 shrink-0 place-items-center rounded-full bg-part-be-bg text-[11px] font-bold text-primary">{e.school.slice(0, 2)}</span>
                  <div className="flex flex-col text-[12px] text-ink-500">
                    <span className="text-[14px] font-semibold text-ink-900">{e.school}</span>
                    <span>{[e.major, e.degree].filter(Boolean).join(" · ")}</span>
                    <span>
                      {e.from} ~ {e.to}
                      {e.gpa && <span className="ml-2 text-primary">GPA : {e.gpa}</span>}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </Card>

        {/* API 모드는 서버가 공개 범위를 적용해 보내므로, 받은 값은 모두 보이고 빠진 값은 '비공개'로 남는다 */}
        <ProfileSections detail={detail} connected={person.connected} />
      </div>

      {/* Right rail: ActionCard (P-03 상태표) */}
      <aside className="flex flex-col gap-5">
        <Card className="flex flex-col gap-2.5 rounded-[10px] p-6">
          {person.pendingCoffeeChatId ? (
            <>
              <Button size="M" full disabled>
                신청 대기 중
              </Button>
              <Link href={`/coffee?tab=sent&id=${person.pendingCoffeeChatId}`} className="text-center text-[12px] text-ink-500 underline">
                내 신청 보기
              </Link>
            </>
          ) : person.canRequest && mockPerson ? (
            <Button size="L" full onClick={() => setRequest(true)} leading={<Icon name="coffee" size={20} />}>
              커피챗 신청하기
            </Button>
          ) : (
            <Tooltip text={person.canRequest ? PENDING_TIP : person.requestBlockedReason}>
              <span className="flex h-10 w-full items-center justify-center rounded-[8px] bg-line-200 text-[13px] font-bold text-ink-400">커피챗 신청하기</span>
            </Tooltip>
          )}
          {mockPerson ? (
            <Button size="M" variant="secondary" full onClick={() => setMessage(true)} leading={<Icon name="chat" size={18} />}>
              메시지 보내기
            </Button>
          ) : (
            <Tooltip text={PENDING_TIP} className="w-full">
              <Button size="M" variant="secondary" full disabled leading={<Icon name="chat" size={18} />}>
                메시지 보내기
              </Button>
            </Tooltip>
          )}
          <div className="mt-3 flex flex-col gap-2 text-[12px]">
            <p className="t-label2 text-ink-900">커피챗 설정</p>
            <div className="flex flex-wrap gap-1.5">
              {person.coffee.topics.map((t) => (
                <OutlineTag key={t}>{t}</OutlineTag>
              ))}
              {person.coffee.acceptGroup && <OutlineTag>여러 명 신청 가능</OutlineTag>}
            </div>
            <dl className="mt-1 flex flex-col gap-1.5">
              <div className="flex justify-between">
                <dt className="text-ink-500">방식</dt>
                <dd className="font-medium text-ink-900">{person.coffee.modes}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-ink-500">가능한 시간대</dt>
                <dd className="font-medium text-ink-900">{person.coffee.times}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-ink-500">선호 지역</dt>
                <dd className="font-medium text-ink-900">{person.coffee.regions}</dd>
              </div>
            </dl>
          </div>
        </Card>

        <RailList title="지금 커피챗이 가능한 사자들" people={openList} href="/alumni?open=1" />
        <RailList title="관심사가 비슷한 사자들" people={similar} href={similarParts ? `/alumni?part=${person.part}` : "/alumni"} />
      </aside>

      {request && mockPerson && <CoffeeRequestModal open onClose={() => setRequest(false)} target={mockPerson} />}
      {message && mockPerson && <MessageRequestModal open onClose={() => setMessage(false)} target={mockPerson} />}
    </div>
  );
}

function RailList({ title, people, href }: { title: string; people: AlumniSummary[]; href: string }) {
  return (
    <div className="flex flex-col gap-3">
      <h2 className="t-body-bold text-ink-900">{title}</h2>
      <div className="flex flex-col gap-0.5">
        {people.length === 0 && <p className="rounded-[10px] border border-line-200 bg-white px-5 py-4 text-[13px] text-ink-500">아직 없어요</p>}
        {people.map((a) => (
          <RailRow key={a.id} person={a} />
        ))}
        <Link href={href} className="flex flex-col items-center rounded-[10px] border border-line-200 bg-white py-3 text-[13px] font-medium text-ink-900 hover:bg-bg-50">
          전체 보기 <Icon name="chevron-down" size={16} />
        </Link>
      </div>
    </div>
  );
}

function RailRow({ person: a }: { person: AlumniSummary }) {
  const { saved, toggle } = useBookmarkToggle(a);
  const part = a.lionParts[0] ?? "ETC";
  const badge = receiveBadge(a.coffeeChat);
  return (
    <div className="flex items-start gap-4 rounded-[10px] border border-line-200 bg-white px-5 py-3">
      <Link href={`/alumni/${a.id}`} className="flex shrink-0 flex-col items-center">
        <Avatar name={a.name} part={part} size={96} />
        <AvailabilityBadge mode={badge.mode} note={badge.note} small className="-mt-3 bg-white" />
      </Link>
      <div className="flex min-w-0 flex-1 flex-col gap-2.5 pt-5">
        <div className="flex items-center gap-2">
          <Link href={`/alumni/${a.id}`} className="t-title3 text-ink-900 hover:underline">
            {a.name}
          </Link>
          <PartTag part={part} generation={a.generation} />
        </div>
        <p className="truncate text-[13px] text-ink-900">{alumniHeadline(a)}</p>
      </div>
      <button type="button" aria-label="관심 동문" aria-pressed={saved} onClick={toggle} className="mt-4 rounded p-0.5 text-navy hover:bg-bg-100">
        <Icon name={saved ? "star-filled" : "star"} size={22} />
      </button>
    </div>
  );
}

/** Cache Components: params must be read inside Suspense. */
export default function AlumniProfilePage({ params }: { params: Promise<{ id: string }> }) {
  return (
    <Suspense fallback={<Skeleton className="h-[400px] w-full" />}>
      <AlumniProfileInner params={params} />
    </Suspense>
  );
}
