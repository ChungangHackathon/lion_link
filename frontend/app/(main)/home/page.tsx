"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import { cn } from "@/lib/cn";
import { useSession } from "@/lib/session";
import { useStore, type CoffeeSettings } from "@/lib/store";
import { ME_ID } from "@/lib/mock/users";
import { LION_PART_LABEL, POST_CATEGORY_LABEL, type Alumni, type Post } from "@/lib/types";
import { daysLeft, formatDate, formatDateTime } from "@/lib/format";
import { Avatar } from "@/components/ui/Avatar";
import { PART_STYLE } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Icon, type IconName } from "@/components/ui/Icon";
import { Radio, Switch } from "@/components/ui/Input";
import { Card } from "@/components/ui/Misc";
import { ConfirmModal, Modal } from "@/components/ui/Modal";
import { useToast } from "@/components/ui/Toast";
import { AlumniCard } from "@/components/domain/AlumniCard";
import { useAlumniActions } from "@/components/domain/useAlumniActions";

const NEWS_TABS = ["전체", "공모전·채용", "지금 사자는", "자료"] as const;
const DAY = ["일", "월", "화", "수", "목", "금", "토"];

/** ★ H-01 홈 / 전체 — 3단 300 / 576 / 312. ◇ H-04 승인 후 첫 방문 (첫 커피챗 가이드). */
export default function HomePage() {
  const { user, session, update } = useSession();
  const { alumni, coffeeChats, rooms, posts, bookmarks, settings, setSettings } = useStore();
  const { onRequest, onMessage, modals } = useAlumniActions();
  const toast = useToast();
  const [tab, setTab] = useState<(typeof NEWS_TABS)[number]>("전체");
  const [rest, setRest] = useState(false); // H-06 쉬기 팝업
  const [confirmOn, setConfirmOn] = useState(false); // CF-21

  const me = alumni.find((a) => a.id === ME_ID) ?? user!;
  const pendingReceived = coffeeChats.filter((c) => c.receiverId === ME_ID && c.status === "PENDING");
  const unread = rooms.reduce((n, r) => n + r.unread, 0);
  const requests = rooms.filter((r) => r.isRequest && r.status === "REQUESTED" && r.memberIds[0] !== ME_ID);
  const received = coffeeChats.filter((c) => c.receiverId === ME_ID).length;
  const joinedMeetups = posts.filter((p) => p.meetup?.memberIds.includes(ME_ID)).length;
  const upcoming = useMemo(() => coffeeChats.filter((c) => c.status === "SCHEDULED" && c.scheduledAt && (c.receiverId === ME_ID || c.requesterId === ME_ID)).sort((a, b) => a.scheduledAt!.localeCompare(b.scheduledAt!))[0], [coffeeChats]);
  const completion = me.completion ?? 45;

  // 사자 추천: 커피챗 가능 + 같은 파트 먼저
  const recommended = alumni.filter((a) => a.id !== ME_ID && a.receive === "OPEN").sort((a, b) => Number(b.part === me.part) - Number(a.part === me.part)).slice(0, 2);
  const similar = alumni.filter((a) => a.id !== ME_ID && a.receive === "OPEN" && (a.part === me.part || a.interests.some((i) => me.interests.includes(i)))).slice(0, 2);
  const recruits = posts.filter((p) => (p.recruit && p.recruit.status === "OPEN") || p.meetup).slice(0, 3);
  const news = posts.filter((p) => tab === "전체" || (tab === "공모전·채용" && ["CONTEST", "JOB"].includes(p.category)) || (tab === "지금 사자는" && ["NOW", "FREE"].includes(p.category)) || (tab === "자료" && p.category === "MATERIAL")).slice(0, 4);

  const toggleReceive = (on: boolean) => {
    if (!on && settings.mode === "OPEN") return setRest(true); // 받는 중 끄기 → H-06
    if (on && settings.mode === "RESTING") return setConfirmOn(true); // 쉬는 중 켜기 → CF-21
    setSettings({ mode: on ? "OPEN" : "CLOSED" });
    toast(on ? "커피챗을 받기 시작했어요" : "커피챗을 받지 않아요");
  };

  return (
    <div className="grid grid-cols-[300px_minmax(0,576px)_312px] justify-between gap-6 min-[1320px]:-mx-[26px]">
      {/* ───── Left: 내 정보 패널 ───── */}
      <aside className="flex flex-col gap-4">
        <Link href="/me" className="relative block h-[295px] overflow-hidden rounded-[10px] border border-line-200 bg-white transition-shadow hover:shadow-[0_4px_16px_rgba(17,24,39,0.06)]">
          <div className={cn("h-[71px]", PART_STYLE[me.part].ring)} />
          <span className="absolute left-[21px] top-[27px]">
            <Avatar name={me.name} part={me.part} size={96} className="!size-[94px] border-4 border-white" />
          </span>
          <div className="flex flex-col gap-2.5 px-5 pt-[66px]">
            <div className="flex items-center gap-2.5">
              <p className="text-[18px] font-bold leading-[27px] text-ink-900">{me.name}</p>
              <span className={cn("rounded-[5px] px-[9px] py-1 t-label2 text-ink-500", PART_STYLE[me.part].bg)}>
                {me.generation}기 · {LION_PART_LABEL[me.part]}
              </span>
            </div>
            <p className="text-[13px] leading-4 text-ink-700">{me.headline}</p>
            <hr className="border-line-200" />
            <p className="line-clamp-3 text-[13px] leading-[18px] text-ink-700">{me.intro}</p>
          </div>
        </Link>

        <CoffeeReceiveCard settings={settings} onToggle={toggleReceive} />

        <Card className="flex flex-col gap-[14px] rounded-[16px] p-5">
          <div className="flex items-center gap-2">
            <Icon name="mail" size={18} className="text-ink-700" />
            <p className="flex-1 t-label1 text-ink-900">연락처</p>
            <Link href="/me?edit=contact" aria-label="연락처 편집" className="rounded p-1 text-ink-700 hover:bg-bg-100">
              <Icon name="edit" size={14} />
            </Link>
          </div>
          <p className="truncate text-[14px] font-medium text-ink-900">{me.email ?? "아직 연락처가 없어요"}</p>
          <p className="flex items-center gap-1 t-caption2 text-ink-500">
            <Icon name="lock" size={13} /> 커피챗이 연결된 동문에게만 보여요
          </p>
        </Card>

        <Card className="flex items-center justify-between rounded-[16px] p-5">
          <Stat href="/coffee" value={received} label="받은 커피챗" />
          <Stat href="/me/bookmarks" value={bookmarks.length + 9} label="관심 받음" />
          <Stat href="/community?cat=MEETUP" value={joinedMeetups} label="참여한 번개" />
        </Card>
      </aside>

      {/* ───── Center ───── */}
      <section className="flex min-w-0 flex-col gap-6">
        <div className="flex flex-col gap-1">
          <h1 className="t-title text-ink-900">{me.name}님, 반가워요!</h1>
          <p className="t-body2 text-ink-500">{pendingReceived.length + unread + requests.length > 0 ? "확인해야할 메시지가 있어요 !" : "오늘도 좋은 하루 보내세요."}</p>
        </div>

        {session.firstVisit && <FirstCoffeeGuide completion={completion} people={similar} onRequest={onRequest} onClose={() => update({ firstVisit: false })} />}

        <div className="grid grid-cols-3 gap-[19px]">
          <StatTile href="/coffee" icon="coffee" label="받은 커피챗 대기" value={pendingReceived.length} unit="건" accent />
          <StatTile href="/chat" icon="chat" label="안 읽은 메시지" value={unread} unit="개" />
          <StatTile href="/chat/requests" icon="mail" label="메세지 요청" value={requests.length} unit="건" />
        </div>

        {/* H-02 완성도 배너 (첫 방문에는 가이드가 대신함) */}
        {!session.firstVisit && <CompletionBanner completion={completion} />}

        <div className="flex flex-col gap-3 pt-2">
          <div className="flex items-center justify-between">
            <h2 className="t-title3 text-ink-900">사자</h2>
            <Link href="/alumni" className="text-[13px] text-ink-500 hover:underline">
              전체보기 →
            </Link>
          </div>
          <div className="flex gap-5 py-5">
            {recommended.map((a) => (
              <AlumniCard key={a.id} person={a} onRequest={onRequest} onMessage={onMessage} />
            ))}
          </div>
        </div>

        {/* 커뮤니티 새 소식 */}
        <Card className="flex flex-col gap-5 rounded-[10px] p-5">
          <div className="flex items-center justify-between">
            <h2 className="t-title3 text-ink-900">커뮤니티 새 소식</h2>
            <Link href="/community" className="text-[13px] text-ink-500 hover:underline">
              전체 보기 →
            </Link>
          </div>
          <div role="tablist" className="flex gap-7 border-b border-line-200">
            {NEWS_TABS.map((t) => (
              <button key={t} type="button" role="tab" aria-selected={tab === t} onClick={() => setTab(t)} className={cn("-mb-px border-b-2 pb-[3px] t-body-bold", tab === t ? "border-[#1f2937] text-orange-over" : "border-transparent text-ink-500 hover:text-ink-900")}>
                {t}
              </button>
            ))}
          </div>
          <ul className="min-h-[388px]">
            {news.length === 0 && <li className="py-10 text-center text-[14px] text-ink-500">아직 올라온 글이 없어요</li>}
            {news.map((p) => (
              <NewsRow key={p.id} post={p} author={alumni.find((a) => a.id === p.authorId)} />
            ))}
          </ul>
        </Card>
      </section>

      {/* ───── Right rail ───── */}
      <aside className="flex flex-col gap-4">
        <Card className="flex flex-col gap-3 rounded-[10px] p-5">
          <h2 className="t-title3 text-ink-900">다가오는 커피챗</h2>
          {upcoming ? (
            <>
              <UpcomingRow chat={upcoming} />
              <div className="flex gap-2">
                <Button size="S" variant="secondary" className="flex-1" href={upcoming.roomId ? `/chat/${upcoming.roomId}` : "/chat"}>
                  채팅
                </Button>
                <Button size="S" className="flex-1" href={`/coffee?id=${upcoming.id}`}>
                  상세
                </Button>
              </div>
            </>
          ) : (
            <p className="py-2 text-[13px] text-ink-500">확정된 일정이 없어요. 받은 신청을 확인해보세요.</p>
          )}
        </Card>

        <Card className="flex flex-col gap-2.5 rounded-[10px] p-5">
          <div className="flex items-center justify-between">
            <h2 className="t-title3 text-ink-900">모집 중인 번개·커피챗</h2>
            <Link href="/community?cat=MEETUP" className="text-[12px] text-ink-500 hover:underline">
              더보기
            </Link>
          </div>
          <ul>
            {recruits.map((p) => (
              <li key={p.id} className="border-b border-line-200">
                <Link href={`/community/${p.id}`} className="flex flex-col gap-[3px] py-2.5 hover:bg-bg-50">
                  <span className="truncate t-label2 text-ink-900">{p.title}</span>
                  <span className="t-caption1 text-ink-500">
                    {p.meetup ? `번개 · ${formatDateTime(p.meetup.at)} · 참여 ${p.meetup.memberIds.length}/${p.meetup.max}명` : p.recruit ? `커피챗 · 마감 ${daysLeft(p.recruit.deadline)} · ${p.recruit.memberIds.length}/${p.recruit.max}명` : "행사"}
                  </span>
                </Link>
              </li>
            ))}
          </ul>
        </Card>

        <Link href="/community/p-5" className="block">
          <Card className="flex flex-col gap-2.5 rounded-[10px] p-5 hover:bg-bg-50">
            <h2 className="t-title3 text-ink-900">1년 전 오늘</h2>
            <ImagePlaceholder className="h-[120px] w-full" />
            <p className="t-body-bold text-ink-900">13기 중커톤 시상식 후기</p>
            <p className="pt-0.5 t-label2 font-medium text-ink-500">지금 사자는 · 2025년 11월 3일</p>
          </Card>
        </Link>
        <p className="text-[11px] leading-[18px] text-ink-400">이용약관 · 개인정보처리방침 · 피드백 보내기 · © 2026 LION:LINK</p>
      </aside>

      {modals}

      {/* ◇[O] H-06 쉬기 팝업 / 받는 중에 끄기 */}
      <RestModal
        open={rest}
        onClose={() => setRest(false)}
        onConfirm={(until) => {
          setRest(false);
          setSettings(until ? { mode: "RESTING", restUntil: until } : { mode: "CLOSED", restUntil: undefined });
          toast(until ? `${formatDate(until)}까지 쉬어요` : "다시 켤 때까지 커피챗을 받지 않아요");
        }}
      />
      {/* ◇[O] CF-21 쉬는 기간 끝내기 확인 */}
      <ConfirmModal
        open={confirmOn}
        onClose={() => setConfirmOn(false)}
        onConfirm={() => {
          setConfirmOn(false);
          setSettings({ mode: "OPEN", restUntil: undefined });
          toast("쉬는 기간을 끝내고 다시 받기 시작했어요");
        }}
        title="쉬는 기간을 끝내고 지금부터 받을까요?"
        confirmLabel="받기"
      />
    </div>
  );
}

/** [S] H-05 커피챗 받기 카드 상태 5종 */
function CoffeeReceiveCard({ settings, onToggle }: { settings: CoffeeSettings; onToggle: (on: boolean) => void }) {
  const on = settings.mode === "OPEN";
  const hasTopics = settings.keywords.length > 0;
  const openUntil = settings.periods.find((p) => p.kind === "OPEN")?.to;

  if (on && hasTopics) {
    return (
      <Card className="flex flex-col gap-[14px] rounded-[16px] p-5">
        <div className="flex items-center gap-2">
          <Icon name="coffee" size={18} className="text-ink-700" />
          <p className="flex-1 t-label1 text-ink-900">커피챗</p>
          <Switch checked onChange={onToggle} label="커피챗 받기" />
        </div>
        <span className="flex w-fit items-center gap-1.5 rounded-full bg-[#e8f7ed] px-2.5 py-[5px] t-caption1 text-[#179952]">
          <span className="size-[7px] rounded-full bg-green" /> 지금 커피챗 받는 중
        </span>
        <Kv icon="clock" k="가능 시간대" v={settings.times} />
        <Kv icon="monitor" k="방식" v={settings.modes.map((m) => (m === "ONLINE" ? "온라인" : "오프라인")).join(" · ")} />
        <Kv icon="place" k="선호 지역" v={settings.regions} />
        <p className="t-caption1 text-ink-500">이야기할 수 있는 주제</p>
        <div className="flex flex-wrap gap-1.5">
          {settings.keywords.map((k) => (
            <span key={k} className="rounded-[6px] bg-bg-100 px-2 py-1 t-label2 text-ink-700">
              {k}
            </span>
          ))}
        </div>
        <Link href="/me/coffee-settings" className="flex items-center gap-2 text-[13px] font-medium text-primary-strong hover:underline">
          커피챗 설정 바꾸기 <Icon name="chevron-right" size={14} />
        </Link>
      </Card>
    );
  }

  const copy = on
    ? { title: "사자들이 신청할 수 있어요", sub: openUntil ? `${formatDate(openUntil)}까지 받아요` : undefined, foot: "도와줄 주제를 정해두면 신청이 더 잘 와요", link: "주제·시간대 수정" }
    : settings.mode === "RESTING"
      ? { title: settings.restUntil ? `${formatDate(settings.restUntil)}까지 쉬는 중이에요` : "쉬는 중이에요", sub: settings.restUntil ? "기간이 끝나면 자동으로 다시 받아요" : "다시 켜면 바로 신청을 받아요", foot: undefined, link: "쉬는 기간 수정" }
      : settings.mode === "SCHEDULED"
        ? { title: "지금은 커피챗을 받지 않아요", sub: `${settings.openFrom ?? "곧"}부터 자동으로 받아요`, foot: undefined, link: "커피챗 받는 기간 수정" }
        : { title: "지금은 커피챗을 받지 않아요", sub: "받기를 켜면 사자들이 신청할 수 있어요", foot: undefined, link: undefined };

  return (
    <Card className="flex flex-col gap-2.5 rounded-[16px] p-5">
      <div className="flex items-center gap-2">
        <Icon name="coffee" size={18} className="text-ink-500" />
        <p className="flex-1 text-[15px] leading-5 text-ink-500">커피챗 받기</p>
        <Switch checked={on} onChange={onToggle} label="커피챗 받기" />
      </div>
      <p className="text-[18px] font-bold leading-7 text-ink-900">{copy.title}</p>
      {copy.sub && <p className="t-caption1 text-ink-500">{copy.sub}</p>}
      {on && (
        <div className="flex flex-wrap gap-1.5">
          {[settings.modes.map((m) => (m === "ONLINE" ? "온라인" : "오프라인")).join(" · "), settings.allowGroup ? "여러 명 가능" : "1:1만", settings.times.split(" · ")[0]].map((t) => (
            <span key={t} className="rounded-[4px] bg-bg-100 px-2 py-1 text-[12px] leading-4 text-ink-700">
              {t}
            </span>
          ))}
        </div>
      )}
      {copy.foot && <p className="t-caption1 text-ink-500">{copy.foot}</p>}
      {copy.link && (
        <Link href="/me/coffee-settings" className="mt-1 w-fit t-label2 text-ink-900 underline underline-offset-2">
          {copy.link} →
        </Link>
      )}
    </Card>
  );
}

function Kv({ icon, k, v }: { icon: IconName; k: string; v: string }) {
  return (
    <div className="flex items-center gap-2.5 text-[13px] leading-4">
      <span className="flex shrink-0 items-center gap-1.5 text-ink-500">
        <Icon name={icon} size={15} />
        {k}
      </span>
      <span className="min-w-0 flex-1 truncate text-right font-medium text-ink-900">{v}</span>
    </div>
  );
}

function Stat({ href, value, label }: { href: string; value: number; label: string }) {
  return (
    <Link href={href} className="flex flex-col items-center gap-0.5 hover:opacity-80">
      <span className="text-[20px] font-bold leading-6 text-ink-900">{value}</span>
      <span className="text-[12px] leading-[14px] text-ink-500">{label}</span>
    </Link>
  );
}

function StatTile({ href, icon, label, value, unit, accent }: { href: string; icon: "coffee" | "chat" | "mail"; label: string; value: number; unit: string; accent?: boolean }) {
  return (
    <Link href={href} className="flex flex-col gap-[7px] rounded-[9px] bg-white px-6 py-4 shadow-[0_4px_4px_rgba(0,0,0,0.1)] transition-shadow hover:shadow-[0_6px_12px_rgba(0,0,0,0.14)]">
      <span className="flex items-center gap-[5px] text-[13px] font-semibold leading-[17px] tracking-[-0.01em] text-black">
        <Icon name={icon} size={17} />
        {label}
      </span>
      <span className="flex items-end gap-0.5">
        <span className={cn("text-[30px] font-extrabold leading-[35px] tracking-[-0.03em]", accent ? "text-primary" : "text-text-dark")}>{value}</span>
        <span className="pb-[3px] text-[16px] font-bold leading-[23px] text-black">{unit}</span>
      </span>
    </Link>
  );
}

/** 프로필 완성도 — 항목 9개 = 9칸 */
function CompletionBanner({ completion }: { completion: number }) {
  if (completion >= 100) {
    return <div className="rounded-[10px] border border-line-200 bg-white px-7 py-4 text-[14px] text-ink-700">프로필 완성도 100% · 지금은 할 일이 없어요 👍</div>;
  }
  const filled = Math.round((completion / 100) * 9);
  return (
    <div className="flex flex-col gap-2 rounded-[10px] border border-blue-100 bg-blue-50 px-7 py-[18px] shadow-[0_4px_4px_rgba(0,0,0,0.12)]">
      <p className="flex items-center gap-1.5 t-label1 text-black">
        <Icon name="edit" size={20} /> 프로필을 완성해주세요
      </p>
      <div className="flex items-center justify-between">
        <p className="flex items-center gap-[30px] text-[18px] font-bold leading-7">
          <span className="text-ink-900">프로필 완성도</span>
          <span className="text-cau">{completion}%</span>
        </p>
        <Link href="/me" className="flex items-center gap-[3px] t-body-bold text-navy hover:underline">
          프로필 채우기 <Icon name="chevron-right" size={24} />
        </Link>
      </div>
      <div className="flex gap-1" role="progressbar" aria-valuenow={completion} aria-valuemin={0} aria-valuemax={100} aria-label="프로필 완성도">
        {Array.from({ length: 9 }, (_, i) => (
          <span key={i} className={cn("h-2 flex-1 rounded-[4px]", i < filled ? "bg-primary-strong" : "bg-line-200")} />
        ))}
      </div>
      <p className="flex items-center gap-2.5 py-[14px] text-[14px] leading-[21px] text-ink-900">
        <Icon name="alert" size={18} /> 프로필을 채우면 사자들이 더 쉽게 찾아와요
      </p>
    </div>
  );
}

/** H-04v2 첫 커피챗 가이드 — 3단계 스텝 + 진행 표시 */
function FirstCoffeeGuide({ completion, people, onRequest, onClose }: { completion: number; people: Alumni[]; onRequest: (p: Alumni) => void; onClose: () => void }) {
  const done = Number(completion >= 100);
  return (
    <div className="flex flex-col gap-5 rounded-[16px] border border-blue-100 bg-gradient-to-b from-blue-50 to-white px-6 pb-6 pt-5">
      <div className="flex items-start justify-between">
        <div className="flex flex-col gap-1">
          <h2 className="t-title3 text-ink-900">첫 커피챗 가이드</h2>
          <p className="t-caption1 text-ink-500">3단계만 끝내면 첫 커피챗 준비 완료!</p>
        </div>
        <button type="button" onClick={onClose} className="t-caption1 text-ink-500 hover:text-ink-900">
          닫기
        </button>
      </div>
      <div className="flex items-center gap-2">
        {[0, 1, 2].map((i) => (
          <span key={i} className={cn("h-1.5 flex-1 rounded-full", i <= done ? "bg-primary/70" : "bg-light-layout")} />
        ))}
        <span className="pl-2 t-caption2 text-primary-strong">{done}/3 완료</span>
      </div>

      <ol className="relative flex flex-col gap-5">
        <span aria-hidden className="absolute bottom-10 left-[17px] top-9 w-px bg-light-text" />
        <GuideStep icon="user" active>
          <div className="flex flex-col gap-3 rounded-[12px] border border-primary/40 bg-white px-[18px] py-4 shadow-[0_4px_12px_rgba(79,111,168,0.18)]">
            <p className="t-label1 text-ink-900">① 프로필 채우기</p>
            <div className="flex items-center gap-3">
              <span className="relative h-1.5 flex-1 overflow-hidden rounded-full bg-light-layout">
                <span className="absolute inset-y-0 left-0 rounded-full bg-primary" style={{ width: `${completion}%` }} />
              </span>
              <span className="t-caption2 text-primary-strong">{completion}%</span>
            </div>
            <div className="flex items-center justify-between gap-3">
              <p className="text-[14px] text-ink-500">소개를 채우면 매칭률이 더욱 높아져요.</p>
              <Button size="S" variant="secondary" href="/me">
                쓰러 가기
              </Button>
            </div>
          </div>
        </GuideStep>
        <GuideStep icon="coffee">
          <div className="flex flex-col gap-3 rounded-[12px] border border-line-200 bg-white px-[18px] py-4">
            <p className="t-label1 text-ink-900">② 커피챗 받기 설정 확인</p>
            <div className="flex items-center justify-between gap-3">
              <p className="text-[14px] text-ink-500">커피챗 설정을 확인해주세요.</p>
              <Button size="S" variant="secondary" href="/me/coffee-settings">
                설정 보기
              </Button>
            </div>
          </div>
        </GuideStep>
        <GuideStep icon="users">
          <div className="flex flex-col gap-3 rounded-[12px] border border-line-200 bg-white px-[18px] py-4">
            <p className="t-label1 text-ink-900">③ 나와 공통점이 많은 사자</p>
            {people.map((p) => (
              <div key={p.id} className="flex gap-5 rounded-[10px] border border-line-200 px-6 py-5">
                <Link href={`/alumni/${p.id}`} className="shrink-0">
                  <Avatar name={p.name} part={p.part} size={48} />
                </Link>
                <div className="flex min-w-0 flex-col gap-2">
                  <div className="flex items-center gap-2">
                    <Link href={`/alumni/${p.id}`} className="t-label1 text-ink-900 hover:underline">
                      {p.name}
                    </Link>
                    <span className={cn("rounded-[4px] px-1.5 py-0.5 text-[11px] text-ink-500", PART_STYLE[p.part].bg)}>
                      {p.generation}기 · {LION_PART_LABEL[p.part]}
                    </span>
                  </div>
                  <p className="truncate text-[12px] text-ink-700">{p.headline}</p>
                  <div className="flex gap-1.5">
                    {[`같은 ${LION_PART_LABEL[p.part]} 파트`, ...p.interests.slice(0, 1).map((i) => `관심 ${i}`)].map((c) => (
                      <span key={c} className="rounded-[3px] bg-bg-100 px-1.5 py-0.5 text-[11px] text-ink-700">
                        {c}
                      </span>
                    ))}
                  </div>
                  <button type="button" onClick={() => onRequest(p)} className="mt-1 h-7 w-[178px] rounded-[6px] bg-light-text text-[12px] font-semibold text-navy hover:bg-blue-100">
                    커피챗 신청하기
                  </button>
                </div>
              </div>
            ))}
          </div>
        </GuideStep>
      </ol>
    </div>
  );
}

function GuideStep({ icon, active, children }: { icon: IconName; active?: boolean; children: React.ReactNode }) {
  return (
    <li className="relative grid grid-cols-[36px_1fr] gap-[14px]">
      <span className={cn("relative z-10 grid size-9 place-items-center rounded-full border", active ? "border-primary bg-primary text-white" : "border-light-text bg-white text-ink-700")}>
        <Icon name={icon} size={17} />
      </span>
      {children}
    </li>
  );
}

function NewsRow({ post: p, author }: { post: Post; author?: Alumni }) {
  const due = p.recruit?.deadline ?? p.deadline;
  const created = new Date(p.createdAt);
  const meta = `${p.anonymous ? "익명" : `${author?.name} · ${author?.generation}기`} · ${created.getMonth() + 1}월 ${created.getDate()}일`;
  const thumb = !!p.images?.[0];
  return (
    <li className="border-b border-line-200">
      <Link href={`/community/${p.id}`} className="flex items-center justify-between gap-4 px-1 py-[18px] hover:bg-bg-50">
        <div className="flex min-w-0 flex-1 flex-col gap-2">
          <div className="flex items-center gap-[5px]">
            <span className="rounded-[4px] bg-bg-100 px-2 py-1 t-caption2 text-ink-700">{POST_CATEGORY_LABEL[p.category]}</span>
            {due && <span className="grid h-[19px] min-w-[39px] place-items-center rounded-[4px] border-[0.5px] border-orange bg-orange-back px-1.5 t-caption2 text-orange-badge">{daysLeft(due)}</span>}
          </div>
          <div className="flex items-center justify-between gap-4">
            <p className="flex min-w-0 items-center gap-[5px] t-body-bold">
              <span className="truncate text-ink-900">{p.title}</span>
              {p.comments.length > 0 && <span className="shrink-0 text-red">[{p.comments.length}]</span>}
            </p>
            {!thumb && <span className="shrink-0 t-caption1 text-ink-500">{meta}</span>}
          </div>
          {thumb && <p className="t-caption1 text-ink-500">{meta}</p>}
        </div>
        {thumb && <ImagePlaceholder className="h-[72px] w-[96px]" />}
      </Link>
    </li>
  );
}

function ImagePlaceholder({ className }: { className?: string }) {
  return (
    <span className={cn("flex shrink-0 flex-col items-center justify-center gap-1 rounded-[8px] border border-dashed border-line-300 bg-bg-100 text-[12px] text-ink-400", className)}>
      <Icon name="image" size={20} />
      Image
    </span>
  );
}

function UpcomingRow({ chat }: { chat: { receiverId: string; requesterId: string; scheduledAt?: string; mode: string } }) {
  const { alumni } = useStore();
  const other = alumni.find((a) => a.id === (chat.receiverId === ME_ID ? chat.requesterId : chat.receiverId));
  const d = new Date(chat.scheduledAt!);
  return (
    <div className="flex items-center gap-3">
      <div className="flex size-[54px] shrink-0 flex-col items-center justify-center rounded-[8px] border-[0.8px] border-light-text bg-blue-50 text-navy">
        <span className="text-[11px] leading-[13px]">{d.getMonth() + 1}월</span>
        <span className="text-[22px] font-bold leading-[26px]">{d.getDate()}</span>
      </div>
      <div className="flex min-w-0 flex-col gap-1">
        <p className="flex items-center">
          <span className="t-body-bold text-ink-900">{other?.name} 사자</span>
          <span className="t-label1 text-ink-900">・</span>
          <span className="t-label2 font-medium text-ink-600">
            {other?.generation}기 {other && LION_PART_LABEL[other.part]}
          </span>
        </p>
        <p className="t-caption1 text-ink-500">
          {DAY[d.getDay()]} {String(d.getHours()).padStart(2, "0")}:{String(d.getMinutes()).padStart(2, "0")} · {chat.mode === "ONLINE" ? "온라인" : "오프라인"}
        </p>
      </div>
    </div>
  );
}

/** H-06 쉬기 팝업 — 얼마나 닫아둘지 고르기 */
const REST_OPTIONS = [
  { key: "1w", label: "1주", days: 7 },
  { key: "2w", label: "2주", days: 14 },
  { key: "1m", label: "1달", days: 30 },
  { key: "custom", label: "직접 선택", days: 0 },
  { key: "off", label: "다시 켤 때까지", days: 0 },
] as const;

function RestModal({ open, onClose, onConfirm }: { open: boolean; onClose: () => void; onConfirm: (until?: string) => void }) {
  const [choice, setChoice] = useState<(typeof REST_OPTIONS)[number]["key"]>("2w");
  const [custom, setCustom] = useState("");
  const after = (days: number) => {
    const t = new Date();
    t.setDate(t.getDate() + days);
    return t.toISOString().slice(0, 10);
  };
  const submit = () => {
    const opt = REST_OPTIONS.find((o) => o.key === choice)!;
    if (choice === "off") return onConfirm(undefined);
    if (choice === "custom") return custom ? onConfirm(custom) : undefined;
    onConfirm(after(opt.days));
  };
  return (
    <Modal
      open={open}
      onClose={onClose}
      width={440}
      title="얼마나 커피챗 신청을 닫아둘까요?"
      footer={
        <>
          <Button variant="secondary" size="S" onClick={onClose}>
            취소
          </Button>
          <Button size="S" onClick={submit} disabled={choice === "custom" && !custom}>
            쉬기
          </Button>
        </>
      }
    >
      <div className="flex flex-col gap-3">
        <p className="text-[14px] text-ink-500">이미 받은 신청과 진행 중인 커피챗은 그대로 진행돼요.</p>
        {REST_OPTIONS.map((o) => (
          <Radio key={o.key} name="rest" label={o.label} checked={choice === o.key} onChange={() => setChoice(o.key)} />
        ))}
        {choice === "custom" && <input type="date" aria-label="쉬는 기간 종료일" min={after(1)} value={custom} onChange={(e) => setCustom(e.target.value)} className="ml-[26px] h-9 w-[180px] rounded-[6px] border border-line-300 px-2 text-[14px]" />}
      </div>
    </Modal>
  );
}
