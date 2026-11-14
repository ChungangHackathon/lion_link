"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, use, useState } from "react";
import { useStore } from "@/lib/store";
import { profileDetail } from "@/lib/mock/profile";
import { ME_ID } from "@/lib/mock/users";
import { LION_PART_LABEL } from "@/lib/types";
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

/** ★ P-01 동문 프로필 / 다른 동문 — 763 본문 + 387 ActionCard rail. ⊕R-01, →T-04 */
function AlumniProfileInner({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const router = useRouter();
  const preview = useSearchParams().get("preview") === "1";
  const toast = useToast();
  const { alumni, bookmarks, toggleBookmark, coffeeChats } = useStore();
  const person = alumni.find((a) => a.id === id);
  const me = alumni.find((a) => a.id === ME_ID)!;
  const [request, setRequest] = useState(false);
  const [message, setMessage] = useState(false);

  if (!person) return <EmptyState title="동문을 찾을 수 없어요" description="탈퇴했거나 주소가 잘못됐어요" action={{ label: "동문 찾기로", href: "/alumni" }} />;
  if (id === ME_ID && !preview) {
    router.replace("/me");
    return null;
  }
  const detail = profileDetail(person.id);
  const saved = bookmarks.includes(person.id);
  const connected = coffeeChats.some((c) => ["ACCEPTED", "SCHEDULED", "COMPLETED"].includes(c.status) && ((c.receiverId === person.id && c.requesterId === ME_ID) || (c.requesterId === person.id && c.receiverId === ME_ID)));
  const pending = coffeeChats.find((c) => c.status === "PENDING" && c.requesterId === ME_ID && c.receiverId === person.id);

  // 나와의 공통점 (P-05)
  const common: string[] = [];
  if (person.generation === me.generation) common.push(`${me.generation}기`);
  if (person.part === me.part) common.push(`${LION_PART_LABEL[me.part]} 파트`);
  person.interests.filter((i) => me.interests.includes(i)).forEach((i) => common.push(i));
  person.skills.filter((s) => me.skills.includes(s)).forEach((s) => common.push(s));

  const openList = alumni.filter((a) => a.id !== ME_ID && a.id !== person.id && a.receive === "OPEN").slice(0, 5);
  const similar = alumni.filter((a) => a.id !== ME_ID && a.id !== person.id && (a.part === person.part || a.interests.some((i) => person.interests.includes(i)))).slice(0, 5);

  return (
    <div className="grid grid-cols-[minmax(0,763px)_387px] justify-between gap-6">
      <div className="flex flex-col gap-6">
        {/* Header card */}
        <Card className="rounded-[10px] p-[30px]">
          <div className="flex gap-[19px]">
            <div className="flex w-[150px] flex-col items-center gap-2">
              <Avatar name={person.name} part={person.part} size={96} className="!size-[150px] !text-[56px]" />
              <AvailabilityBadge mode={person.receive} note={person.receiveNote} className="-mt-5 bg-white" />
            </div>
            <div className="flex flex-1 flex-col gap-4 pt-6">
              <div className="flex items-start justify-between">
                <div className="flex items-center gap-2.5">
                  <h1 className="text-[28px] font-bold leading-[34px] text-ink-900">{person.name}</h1>
                  <PartTag part={person.part} generation={person.generation} />
                  {person.isStaff && <span className="rounded-[5px] bg-orange-back px-2 py-[3px] text-[12px] font-semibold text-orange-badge">{person.generation + 1}기 {LION_PART_LABEL[person.part]} 운영진</span>}
                </div>
                <div className="relative flex items-center gap-2">
                  <button type="button" aria-label="관심 동문" aria-pressed={saved} onClick={() => { toggleBookmark(person.id); toast(saved ? "관심 동문에서 뺐어요" : "관심 동문에 저장했어요"); }} className="rounded p-1 hover:bg-bg-100">
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
                {common.length ? common.map((c) => <Tag key={c}>{c}</Tag>) : <span className="text-ink-400">아직 없어요</span>}
              </div>
              <div className="flex items-center gap-2">
                <span className="text-ink-500">대표 기술</span>
                {detail.repSkills.map((s) => (
                  <Tag key={s}>{s}</Tag>
                ))}
              </div>
              {detail.location && (
                <p className="flex items-center gap-1 text-ink-700">
                  <Icon name="pin" size={14} /> {detail.location}
                </p>
              )}
            </div>
            <div className="flex flex-col gap-4">
              {detail.educations.map((e, i) => (
                <div key={i} className="flex items-center gap-3">
                  <span className="grid size-10 shrink-0 place-items-center rounded-full bg-part-be-bg text-[11px] font-bold text-primary">{e.school.slice(0, 2)}</span>
                  <div className="flex flex-col text-[12px] text-ink-500">
                    <span className="text-[14px] font-semibold text-ink-900">{e.school}</span>
                    <span>
                      {e.major} · {e.degree}
                    </span>
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

        <ProfileSections detail={detail} person={person} connected={connected} />
      </div>

      {/* Right rail: ActionCard (P-03 상태표) */}
      <aside className="flex flex-col gap-5">
        <Card className="flex flex-col gap-2.5 rounded-[10px] p-6">
          {pending ? (
            <>
              <Button size="M" full disabled>
                신청 대기 중
              </Button>
              <Link href={`/coffee?tab=sent&id=${pending.id}`} className="text-center text-[12px] text-ink-500 underline">
                내 신청 보기
              </Link>
            </>
          ) : person.receive === "OPEN" ? (
            <Button size="L" full onClick={() => setRequest(true)} leading={<Icon name="coffee" size={20} />}>
              커피챗 신청하기
            </Button>
          ) : (
            <Tooltip text={person.receive === "RESTING" ? "지금은 쉬는 중인 동문이에요" : person.receive === "SCHEDULED" ? person.receiveNote ?? "곧 받을 예정이에요" : "커피챗을 받지 않는 동문이에요"}>
              <span className="flex h-10 w-full items-center justify-center rounded-[8px] bg-line-200 text-[13px] font-bold text-ink-400">커피챗 신청하기</span>
            </Tooltip>
          )}
          <Button size="M" variant="secondary" full onClick={() => setMessage(true)} leading={<Icon name="chat" size={18} />}>
            메시지 보내기
          </Button>
          <div className="mt-3 flex flex-col gap-2 text-[12px]">
            <p className="t-label2 text-ink-900">커피챗 설정</p>
            <div className="flex flex-wrap gap-1.5">
              {detail.coffeeTopics.map((t) => (
                <OutlineTag key={t}>{t}</OutlineTag>
              ))}
              {person.allowGroup && <OutlineTag>여러 명 신청 가능</OutlineTag>}
            </div>
            <dl className="mt-1 flex flex-col gap-1.5">
              <div className="flex justify-between">
                <dt className="text-ink-500">방식</dt>
                <dd className="font-medium text-ink-900">{detail.coffeeModes}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-ink-500">가능한 시간대</dt>
                <dd className="font-medium text-ink-900">{detail.coffeeTimes}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-ink-500">선호 지역</dt>
                <dd className="font-medium text-ink-900">{detail.coffeeRegions}</dd>
              </div>
            </dl>
          </div>
        </Card>

        <RailList title="지금 커피챗이 가능한 사자들" people={openList} href="/alumni?open=1" />
        <RailList title="관심사가 비슷한 사자들" people={similar} href={`/alumni?part=${person.part}`} />
      </aside>

      {request && <CoffeeRequestModal open onClose={() => setRequest(false)} target={person} />}
      {message && <MessageRequestModal open onClose={() => setMessage(false)} target={person} />}
    </div>
  );
}

function RailList({ title, people, href }: { title: string; people: { id: string }[]; href: string }) {
  const { alumni, bookmarks, toggleBookmark } = useStore();
  return (
    <div className="flex flex-col gap-3">
      <h2 className="t-body-bold text-ink-900">{title}</h2>
      <div className="flex flex-col gap-0.5">
        {people.map((p) => {
          const a = alumni.find((x) => x.id === p.id)!;
          const saved = bookmarks.includes(a.id);
          return (
            <div key={a.id} className="flex items-start gap-4 rounded-[10px] border border-line-200 bg-white px-5 py-3">
              <Link href={`/alumni/${a.id}`} className="flex shrink-0 flex-col items-center">
                <Avatar name={a.name} part={a.part} size={96} />
                <AvailabilityBadge mode={a.receive} note={a.receiveNote} small className="-mt-3 bg-white" />
              </Link>
              <div className="flex min-w-0 flex-1 flex-col gap-2.5 pt-5">
                <div className="flex items-center gap-2">
                  <Link href={`/alumni/${a.id}`} className="t-title3 text-ink-900 hover:underline">
                    {a.name}
                  </Link>
                  <PartTag part={a.part} generation={a.generation} />
                </div>
                <p className="truncate text-[13px] text-ink-900">{a.headline}</p>
              </div>
              <button type="button" aria-label="관심 동문" aria-pressed={saved} onClick={() => toggleBookmark(a.id)} className="mt-4 rounded p-0.5 text-navy hover:bg-bg-100">
                <Icon name={saved ? "star-filled" : "star"} size={22} />
              </button>
            </div>
          );
        })}
        <Link href={href} className="flex flex-col items-center rounded-[10px] border border-line-200 bg-white py-3 text-[13px] font-medium text-ink-900 hover:bg-bg-50">
          전체 보기 <Icon name="chevron-down" size={16} />
        </Link>
      </div>
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
