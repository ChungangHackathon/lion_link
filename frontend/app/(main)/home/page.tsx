"use client";

import Link from "next/link";
import { useState } from "react";
import { cn } from "@/lib/cn";
import { INTEREST_LABEL, LION_PART_LABEL, POST_CATEGORY_LABEL, type LionPart, type PostCategory } from "@/lib/types";
import { daysLeft, formatDateTime } from "@/lib/format";
import { errorMessage, isApiError } from "@/lib/api/client";
import type { AlumniSummary } from "@/lib/api/alumni";
import { slotsText } from "@/lib/api/alumniView";
import { COFFEE_TOPIC_LABEL } from "@/lib/api/coffee";
import { addDays, todayKst, type CoffeeSettings } from "@/lib/api/coffeeSettings";
import type { HomeData, RestFor, SwitchRequest } from "@/lib/api/home";
import { completionOf, SCOPE_LABEL } from "@/lib/api/myProfile";
import type { PostSummary } from "@/lib/api/posts";
import { useApi } from "@/lib/api/useApi";
import { useCoffeeSettingsService } from "@/lib/api/useCoffeeSettingsService";
import { useHomeService } from "@/lib/api/useHomeService";
import { useMyProfileService } from "@/lib/api/useMyProfileService";
import { usePostService } from "@/lib/api/usePostService";
import { Avatar } from "@/components/ui/Avatar";
import { PART_STYLE } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Icon, type IconName } from "@/components/ui/Icon";
import { Radio, Switch } from "@/components/ui/Input";
import { Card, EmptyState, Skeleton } from "@/components/ui/Misc";
import { ConfirmModal, Modal } from "@/components/ui/Modal";
import { useToast } from "@/components/ui/Toast";
import { AlumniCard } from "@/components/domain/AlumniCard";
import { authorLabel, isImageUrl } from "@/components/domain/PostRow";
import { useAlumniActions } from "@/components/domain/useAlumniActions";

const NEWS_TABS: { label: string; category?: PostCategory[] }[] = [
  { label: "전체" },
  { label: "공모전·채용", category: ["CONTEST", "JOB"] },
  { label: "지금 사자는", category: ["NOW", "FREE"] },
  { label: "자료", category: ["MATERIAL"] },
];
const DAY = ["일", "월", "화", "수", "목", "금", "토"];
const firstPart = (parts: string[] | null | undefined): LionPart => (parts?.[0] as LionPart) ?? "ETC";
const monthDay = (date: string) => `${Number(date.slice(5, 7))}/${Number(date.slice(8, 10))}`;

/** ★ H-01 홈 / 전체 — 3단 300 / 576 / 312. ◇ H-04 승인 후 첫 방문 (첫 커피챗 가이드). (HOME-01 · 02, PROFILE-14) */
export default function HomePage() {
  const service = useHomeService();
  const { data, error, reload, setData } = useApi(() => service.get(), [service]);
  if (error && !data) return <EmptyState title="홈을 불러오지 못했어요" description={errorMessage(error)} action={{ label: "다시 시도", onClick: reload }} />;
  if (!data) return <Skeleton className="h-[600px] w-full" />;
  return <HomeView home={data} onDismissed={() => setData((h) => (h ? { ...h, welcomeBannerVisible: false } : h))} dismiss={service.dismissWelcome} />;
}

function HomeView({ home, onDismissed, dismiss }: { home: HomeData; onDismissed: () => void; dismiss: () => Promise<void> }) {
  const toast = useToast();
  const { onRequest, onMessage, modals } = useAlumniActions();
  const profileService = useMyProfileService();
  const profile = useApi(() => profileService.get(), [profileService]).data;
  const postService = usePostService();
  const [tab, setTab] = useState(0);
  const news = useApi((signal) => postService.list({ category: NEWS_TABS[tab].category, size: 4 }, signal), [postService, tab]).data;
  const recruits = useApi((signal) => postService.list({ recruiting: true, size: 3 }, signal), [postService]).data;

  const { me, counts } = home;
  const part = firstPart(me.lionParts);
  const completion = profile ? completionOf(profile) : null;
  const unreadAll = counts.pendingCoffeeChats + counts.unreadMessages + counts.messageRequests;

  const closeWelcome = async () => {
    try {
      await dismiss();
      onDismissed();
    } catch (e) {
      toast(errorMessage(e), { kind: "error" });
    }
  };

  return (
    <div className="grid grid-cols-[300px_minmax(0,576px)_312px] justify-between gap-6 min-[1320px]:-mx-[26px]">
      {/* ───── Left: 내 정보 패널 ───── */}
      <aside className="flex flex-col gap-4">
        <Link href="/me" className="relative block h-[295px] overflow-hidden rounded-[10px] border border-line-200 bg-white transition-shadow hover:shadow-[0_4px_16px_rgba(17,24,39,0.06)]">
          <div className={cn("h-[71px]", PART_STYLE[part].ring)} />
          <span className="absolute left-[21px] top-[27px]">
            {isImageUrl(me.photoUrl) ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img src={me.photoUrl} alt="" className="size-[94px] rounded-full border-4 border-white object-cover" />
            ) : (
              <Avatar name={me.name} part={part} size={96} className="!size-[94px] border-4 border-white" />
            )}
          </span>
          <div className="flex flex-col gap-2.5 px-5 pt-[66px]">
            <div className="flex items-center gap-2.5">
              <p className="text-[18px] font-bold leading-[27px] text-ink-900">{me.name}</p>
              <span className={cn("rounded-[5px] px-[9px] py-1 t-label2 text-ink-500", PART_STYLE[part].bg)}>
                {me.generation}기 · {me.lionParts.map((x) => LION_PART_LABEL[x]).join("·") || LION_PART_LABEL.ETC}
              </span>
            </div>
            <p className="text-[13px] leading-4 text-ink-700">{me.headline ?? <span className="text-ink-400">한 줄 소개를 적어보세요</span>}</p>
            <hr className="border-line-200" />
            <p className="line-clamp-3 text-[13px] leading-[18px] text-ink-700">{me.bio ?? <span className="text-ink-400">아직 자기소개가 없어요</span>}</p>
          </div>
        </Link>

        <CoffeeReceiveCard />

        <Card className="flex flex-col gap-[14px] rounded-[16px] p-5">
          <div className="flex items-center gap-2">
            <Icon name="mail" size={18} className="text-ink-700" />
            <p className="flex-1 t-label1 text-ink-900">연락처</p>
            <Link href="/me" aria-label="연락처 편집" className="rounded p-1 text-ink-700 hover:bg-bg-100">
              <Icon name="edit" size={14} />
            </Link>
          </div>
          <p className="truncate text-[14px] font-medium text-ink-900">{profile ? profile.contact.email ?? "아직 연락처가 없어요" : " "}</p>
          {profile && (
            <p className="flex items-center gap-1 t-caption2 text-ink-500">
              <Icon name="lock" size={13} /> {SCOPE_LABEL[profile.visibility.email]}
            </p>
          )}
        </Card>

        <Card className="flex items-center justify-around rounded-[16px] p-5">
          <Stat href="/coffee" value={counts.activeCoffeeChats} label="진행 중 커피챗" />
          <Stat href="/me/bookmarks" value={counts.bookmarks} label="관심 동문" />
        </Card>
      </aside>

      {/* ───── Center ───── */}
      <section className="flex min-w-0 flex-col gap-6">
        <div className="flex flex-col gap-1">
          <h1 className="t-title text-ink-900">{me.name}님, 반가워요!</h1>
          <p className="t-body2 text-ink-500">{unreadAll > 0 ? "확인해야할 메시지가 있어요 !" : "오늘도 좋은 하루 보내세요."}</p>
        </div>

        {home.welcomeBannerVisible && <FirstCoffeeGuide completion={completion ?? 0} myParts={me.lionParts} people={home.recommendedSeniors} onRequest={onRequest} onClose={() => void closeWelcome()} />}

        <div className="grid grid-cols-3 gap-[19px]">
          <StatTile href="/coffee" icon="coffee" label="받은 커피챗 대기" value={counts.pendingCoffeeChats} unit="건" accent />
          <StatTile href="/chat" icon="chat" label="안 읽은 메시지" value={counts.unreadMessages} unit="개" />
          <StatTile href="/chat/requests" icon="mail" label="메세지 요청" value={counts.messageRequests} unit="건" />
        </div>

        {/* H-02 완성도 배너 (첫 방문에는 가이드가 대신함) */}
        {!home.welcomeBannerVisible && completion !== null && <CompletionBanner completion={completion} />}

        <div className="flex flex-col gap-3 pt-2">
          <div className="flex items-center justify-between">
            <h2 className="t-title3 text-ink-900">사자</h2>
            <Link href="/alumni" className="text-[13px] text-ink-500 hover:underline">
              전체보기 →
            </Link>
          </div>
          {home.recommendedSeniors.length ? (
            <div className="flex gap-5 py-5">
              {home.recommendedSeniors.map((a) => (
                <AlumniCard key={a.id} person={a} onRequest={onRequest} onMessage={onMessage} />
              ))}
            </div>
          ) : (
            <p className="py-6 text-center text-[14px] text-ink-500">지금 커피챗을 받는 선배 사자가 없어요</p>
          )}
        </div>

        {/* 커뮤니티 새 소식 (POST-01) */}
        <Card className="flex flex-col gap-5 rounded-[10px] p-5">
          <div className="flex items-center justify-between">
            <h2 className="t-title3 text-ink-900">커뮤니티 새 소식</h2>
            <Link href="/community" className="text-[13px] text-ink-500 hover:underline">
              전체 보기 →
            </Link>
          </div>
          <div role="tablist" className="flex gap-7 border-b border-line-200">
            {NEWS_TABS.map((t, i) => (
              <button key={t.label} type="button" role="tab" aria-selected={tab === i} onClick={() => setTab(i)} className={cn("-mb-px border-b-2 pb-[3px] t-body-bold", tab === i ? "border-[#1f2937] text-orange-over" : "border-transparent text-ink-500 hover:text-ink-900")}>
                {t.label}
              </button>
            ))}
          </div>
          <ul className="min-h-[388px]">
            {!news && <Skeleton className="h-[300px] w-full" />}
            {news?.items.length === 0 && <li className="py-10 text-center text-[14px] text-ink-500">아직 올라온 글이 없어요</li>}
            {news?.items.map((p) => (
              <NewsRow key={p.id} post={p} />
            ))}
          </ul>
        </Card>
      </section>

      {/* ───── Right rail ───── */}
      <aside className="flex flex-col gap-4">
        <Card className="flex flex-col gap-3 rounded-[10px] p-5">
          <h2 className="t-title3 text-ink-900">다가오는 커피챗</h2>
          {home.upcomingCoffeeChat ? (
            <>
              <UpcomingRow chat={home.upcomingCoffeeChat} />
              <div className="flex gap-2">
                <Button size="S" variant="secondary" className="flex-1" href={home.upcomingCoffeeChat.roomId ? `/chat/${home.upcomingCoffeeChat.roomId}` : "/chat"}>
                  채팅
                </Button>
                <Button size="S" className="flex-1" href={`/coffee?id=${home.upcomingCoffeeChat.id}`}>
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
            {recruits?.items.length === 0 && <li className="py-2 text-[13px] text-ink-500">지금 모집 중인 글이 없어요</li>}
            {recruits?.items.map((p) => (
              <li key={p.id} className="border-b border-line-200">
                <Link href={`/community/${p.id}`} className="flex flex-col gap-[3px] py-2.5 hover:bg-bg-50">
                  <span className="truncate t-label2 text-ink-900">{p.title}</span>
                  <span className="t-caption1 text-ink-500">
                    {p.meetup ? `번개 · ${formatDateTime(p.meetup.meetAt)} · 참여 ${p.meetup.participantCount}/${p.meetup.capacity}명` : p.recruit ? `커피챗 · 마감 ${daysLeft(p.recruit.deadline)} · ${p.recruit.memberCount}/${p.recruit.capacity}명` : POST_CATEGORY_LABEL[p.category]}
                  </span>
                </Link>
              </li>
            ))}
          </ul>
        </Card>

        {home.onThisDay && (
          <Link href={`/community/${home.onThisDay.id}`} className="block">
            <Card className="flex flex-col gap-2.5 rounded-[10px] p-5 hover:bg-bg-50">
              <h2 className="t-title3 text-ink-900">1년 전 오늘</h2>
              {isImageUrl(home.onThisDay.thumbnailUrl) ? (
                // eslint-disable-next-line @next/next/no-img-element
                <img src={home.onThisDay.thumbnailUrl} alt="" className="h-[120px] w-full rounded-[8px] object-cover" />
              ) : (
                <ImagePlaceholder className="h-[120px] w-full" />
              )}
              <p className="t-body-bold text-ink-900">{home.onThisDay.title}</p>
              <p className="pt-0.5 t-label2 font-medium text-ink-500">
                {POST_CATEGORY_LABEL[home.onThisDay.category]} · {fullDate(home.onThisDay.createdAt)}
              </p>
            </Card>
          </Link>
        )}
        <p className="text-[11px] leading-[18px] text-ink-400">이용약관 · 개인정보처리방침 · 피드백 보내기 · © 2026 LION:LINK</p>
      </aside>

      {modals}
    </div>
  );
}

const fullDate = (iso: string) => {
  const d = new Date(iso);
  return `${d.getFullYear()}년 ${d.getMonth() + 1}월 ${d.getDate()}일`;
};

/** [S] H-05 커피챗 받기 카드 상태 5종 + 스위치 (PROFILE-12 · 14) */
function CoffeeReceiveCard() {
  const toast = useToast();
  const service = useCoffeeSettingsService();
  const { data: s, setData } = useApi(() => service.get(), [service]);
  const [rest, setRest] = useState(false); // H-06 쉬기 팝업
  const [confirmOn, setConfirmOn] = useState(false); // CF-21 쉬는 기간 끝내기
  const [clearConfirm, setClearConfirm] = useState<SwitchRequest | null>(null); // 기간이 지워지는 경우
  const [busy, setBusy] = useState(false);

  if (!s) return <Skeleton className="h-[180px] w-full rounded-[16px]" />;

  const on = s.status.availability === "AVAILABLE";
  const resting = s.status.availability === "RESTING";

  const send = async (body: SwitchRequest) => {
    setBusy(true);
    try {
      const next = await service.switchReceive(body);
      setData(() => next);
      setRest(false);
      setConfirmOn(false);
      setClearConfirm(null);
      toast(switchMessage(next));
    } catch (e) {
      if (isApiError(e) && e.code === "PERIODS_WILL_BE_CLEARED") {
        setRest(false);
        setConfirmOn(false);
        setClearConfirm(body);
      } else toast(errorMessage(e), { kind: "error" });
    } finally {
      setBusy(false);
    }
  };

  const toggle = (next: boolean) => {
    if (busy) return;
    if (!next && s.defaultMode === "ON") return setRest(true); // 받는 중 끄기 → H-06
    if (next && resting) return setConfirmOn(true); // 쉬는 중 켜기 → CF-21
    void send(next ? { on: true } : { on: false, restFor: "UNTIL_RESUME" });
  };

  const openUntil = !on || s.defaultMode === "ON" ? null : s.periods.find((p) => p.startDate <= todayKst() && todayKst() <= p.endDate)?.endDate;
  const modes = s.modes.map((m) => (m === "ONLINE" ? "온라인" : "오프라인")).join(" · ");

  const body =
    on && s.topics.length > 0 ? (
      <Card className="flex flex-col gap-[14px] rounded-[16px] p-5">
        <div className="flex items-center gap-2">
          <Icon name="coffee" size={18} className="text-ink-700" />
          <p className="flex-1 t-label1 text-ink-900">커피챗</p>
          <Switch checked onChange={toggle} label="커피챗 받기" disabled={busy} />
        </div>
        <span className="flex w-fit items-center gap-1.5 rounded-full bg-[#e8f7ed] px-2.5 py-[5px] t-caption1 text-[#179952]">
          <span className="size-[7px] rounded-full bg-green" /> 지금 커피챗 받는 중{openUntil ? ` · ${monthDay(openUntil)}까지` : ""}
        </span>
        <Kv icon="clock" k="가능 시간대" v={slotsText(s.availableSlots)} />
        <Kv icon="monitor" k="방식" v={modes || "-"} />
        <Kv icon="place" k="선호 지역" v={s.preferredArea ?? "-"} />
        <p className="t-caption1 text-ink-500">이야기할 수 있는 주제</p>
        <div className="flex flex-wrap gap-1.5">
          {s.topics.map((k) => (
            <span key={k} className="rounded-[6px] bg-bg-100 px-2 py-1 t-label2 text-ink-700">
              {COFFEE_TOPIC_LABEL[k]}
            </span>
          ))}
        </div>
        <Link href="/me/coffee-settings" className="flex items-center gap-2 text-[13px] font-medium text-primary-strong hover:underline">
          커피챗 설정 바꾸기 <Icon name="chevron-right" size={14} />
        </Link>
      </Card>
    ) : (
      <ReceiveSummary s={s} on={on} resting={resting} openUntil={openUntil ?? null} busy={busy} onToggle={toggle} />
    );

  return (
    <>
      {body}
      {rest && <RestModal busy={busy} onClose={() => setRest(false)} onConfirm={(restFor, endDate) => void send(restFor === "CUSTOM" ? { on: false, restFor, startDate: todayKst(), endDate } : { on: false, restFor })} />}
      {/* ◇[O] CF-21 쉬는 기간 끝내기 확인 */}
      <ConfirmModal open={confirmOn} onClose={() => setConfirmOn(false)} onConfirm={() => void send({ on: true })} loading={busy} title="쉬는 기간을 끝내고 지금부터 받을까요?" confirmLabel="받기" />
      <ConfirmModal
        open={!!clearConfirm}
        onClose={() => setClearConfirm(null)}
        onConfirm={() => clearConfirm && void send({ ...clearConfirm, confirmClearPeriods: true })}
        loading={busy}
        title="등록한 기간이 모두 지워져요. 바꿀까요?"
        description={s.defaultMode === "ON" ? "다시 켤 때까지 받지 않도록 바꾸면 등록한 쉬는 기간이 지워져요." : "항상 받도록 바꾸면 등록한 받는 기간이 지워져요."}
        confirmLabel="바꾸기"
      />
    </>
  );
}

function switchMessage(s: CoffeeSettings): string {
  const next = s.status.nextAvailableDate;
  switch (s.status.availability) {
    case "AVAILABLE":
      return "커피챗을 받기 시작했어요";
    case "RESTING":
      return next ? `${monthDay(addDays(next, -1))}까지 쉬어요` : "커피챗 신청을 잠시 닫았어요";
    default:
      return "다시 켤 때까지 커피챗을 받지 않아요";
  }
}

function ReceiveSummary({ s, on, resting, openUntil, busy, onToggle }: { s: CoffeeSettings; on: boolean; resting: boolean; openUntil: string | null; busy: boolean; onToggle: (v: boolean) => void }) {
  const next = s.status.nextAvailableDate;
  const copy = on
    ? { title: "사자들이 신청할 수 있어요", sub: openUntil ? `${monthDay(openUntil)}까지 받아요` : undefined, foot: "도와줄 주제를 정해두면 신청이 더 잘 와요", link: "주제·시간대 수정" }
    : resting
      ? { title: next ? `${monthDay(addDays(next, -1))}까지 쉬는 중이에요` : "쉬는 중이에요", sub: "기간이 끝나면 자동으로 다시 받아요", foot: undefined, link: "쉬는 기간 수정" }
      : next
        ? { title: "지금은 커피챗을 받지 않아요", sub: `${monthDay(next)}부터 자동으로 받아요`, foot: undefined, link: "커피챗 받는 기간 수정" }
        : { title: "지금은 커피챗을 받지 않아요", sub: "받기를 켜면 사자들이 신청할 수 있어요", foot: undefined, link: undefined };
  const tags = [s.modes.map((m) => (m === "ONLINE" ? "온라인" : "오프라인")).join(" · "), s.acceptGroup ? "여러 명 가능" : "1:1만", slotsText(s.availableSlots).split(", ")[0]].filter((t) => t && t !== "-");
  return (
    <Card className="flex flex-col gap-2.5 rounded-[16px] p-5">
      <div className="flex items-center gap-2">
        <Icon name="coffee" size={18} className="text-ink-500" />
        <p className="flex-1 text-[15px] leading-5 text-ink-500">커피챗 받기</p>
        <Switch checked={on} onChange={onToggle} label="커피챗 받기" disabled={busy} />
      </div>
      <p className="text-[18px] font-bold leading-7 text-ink-900">{copy.title}</p>
      {copy.sub && <p className="t-caption1 text-ink-500">{copy.sub}</p>}
      {on && (
        <div className="flex flex-wrap gap-1.5">
          {tags.map((t) => (
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
function FirstCoffeeGuide({ completion, myParts, people, onRequest, onClose }: { completion: number; myParts: string[]; people: AlumniSummary[]; onRequest: (p: AlumniSummary) => void; onClose: () => void }) {
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
            {people.length === 0 && <p className="text-[14px] text-ink-500">지금 커피챗을 받는 선배 사자가 없어요. 동문 찾기에서 둘러보세요.</p>}
            {people.map((p) => {
              const part = firstPart(p.lionParts);
              const chips = [...p.lionParts.filter((x) => myParts.includes(x)).map((x) => `같은 ${LION_PART_LABEL[x]} 파트`), ...p.interests.slice(0, 1).map((i) => `관심 ${INTEREST_LABEL[i]}`)].slice(0, 2);
              return (
                <div key={p.id} className="flex gap-5 rounded-[10px] border border-line-200 px-6 py-5">
                  <Link href={`/alumni/${p.id}`} className="shrink-0">
                    <Avatar name={p.name} part={part} size={48} />
                  </Link>
                  <div className="flex min-w-0 flex-col gap-2">
                    <div className="flex items-center gap-2">
                      <Link href={`/alumni/${p.id}`} className="t-label1 text-ink-900 hover:underline">
                        {p.name}
                      </Link>
                      <span className={cn("rounded-[4px] px-1.5 py-0.5 text-[11px] text-ink-500", PART_STYLE[part].bg)}>
                        {p.generation}기 · {LION_PART_LABEL[part]}
                      </span>
                    </div>
                    {p.headline && <p className="truncate text-[12px] text-ink-700">{p.headline}</p>}
                    <div className="flex gap-1.5">
                      {chips.map((c) => (
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
              );
            })}
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

function NewsRow({ post: p }: { post: PostSummary }) {
  const due = p.recruit?.deadline ?? p.deadline;
  const created = new Date(p.createdAt);
  const meta = `${authorLabel(p.author)} · ${created.getMonth() + 1}월 ${created.getDate()}일`;
  const thumb = isImageUrl(p.thumbnailUrl) ? p.thumbnailUrl : null;
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
              {p.commentCount > 0 && <span className="shrink-0 text-red">[{p.commentCount}]</span>}
            </p>
            {!thumb && <span className="shrink-0 t-caption1 text-ink-500">{meta}</span>}
          </div>
          {thumb && <p className="t-caption1 text-ink-500">{meta}</p>}
        </div>
        {thumb && (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={thumb} alt="" className="h-[72px] w-[96px] shrink-0 rounded-[8px] object-cover" />
        )}
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

function UpcomingRow({ chat }: { chat: NonNullable<HomeData["upcomingCoffeeChat"]> }) {
  const d = new Date(chat.scheduledAt);
  const partner = chat.partner;
  const part = firstPart(partner.lionParts);
  return (
    <div className="flex items-center gap-3">
      <div className="flex size-[54px] shrink-0 flex-col items-center justify-center rounded-[8px] border-[0.8px] border-light-text bg-blue-50 text-navy">
        <span className="text-[11px] leading-[13px]">{d.getMonth() + 1}월</span>
        <span className="text-[22px] font-bold leading-[26px]">{d.getDate()}</span>
      </div>
      <div className="flex min-w-0 flex-col gap-1">
        <p className="flex items-center">
          <span className="t-body-bold text-ink-900">{partner.isDeleted ? "탈퇴한 사자" : `${partner.name} 사자`}</span>
          {!partner.isDeleted && (
            <>
              <span className="t-label1 text-ink-900">・</span>
              <span className="t-label2 font-medium text-ink-600">
                {partner.generation}기 {LION_PART_LABEL[part]}
              </span>
            </>
          )}
        </p>
        <p className="t-caption1 text-ink-500">
          {DAY[d.getDay()]} {String(d.getHours()).padStart(2, "0")}:{String(d.getMinutes()).padStart(2, "0")} · {chat.meetingMode === "ONLINE" ? "온라인" : "오프라인"}
        </p>
      </div>
    </div>
  );
}

/** H-06 쉬기 팝업 — 얼마나 닫아둘지 고르기 */
const REST_OPTIONS: { key: RestFor; label: string }[] = [
  { key: "WEEK_1", label: "1주" },
  { key: "WEEK_2", label: "2주" },
  { key: "MONTH_1", label: "1달" },
  { key: "CUSTOM", label: "직접 선택" },
  { key: "UNTIL_RESUME", label: "다시 켤 때까지" },
];

function RestModal({ busy, onClose, onConfirm }: { busy: boolean; onClose: () => void; onConfirm: (restFor: RestFor, endDate?: string) => void }) {
  const [choice, setChoice] = useState<RestFor>("WEEK_2");
  const [custom, setCustom] = useState("");
  const today = todayKst();
  const max = addDays(today, 179);
  const customError = custom && (custom < today || custom > max) ? "오늘부터 180일 이내로 골라주세요" : null;
  return (
    <Modal
      open
      onClose={onClose}
      width={440}
      title="얼마나 커피챗 신청을 닫아둘까요?"
      footer={
        <>
          <Button variant="secondary" size="S" onClick={onClose}>
            취소
          </Button>
          <Button size="S" loading={busy} onClick={() => onConfirm(choice, choice === "CUSTOM" ? custom : undefined)} disabled={choice === "CUSTOM" && (!custom || !!customError)}>
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
        {choice === "CUSTOM" && (
          <div className="ml-[26px] flex flex-col gap-1">
            <input type="date" aria-label="쉬는 기간 종료일" min={today} max={max} value={custom} onChange={(e) => setCustom(e.target.value)} className="h-9 w-[180px] rounded-[6px] border border-line-300 px-2 text-[14px]" />
            <span className={cn("text-[12px]", customError ? "text-orange" : "text-ink-500")}>{customError ?? "오늘부터 고른 날까지 쉬어요"}</span>
          </div>
        )}
      </div>
    </Modal>
  );
}
