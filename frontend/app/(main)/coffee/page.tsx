"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useState } from "react";
import { cn } from "@/lib/cn";
import { COFFEE_STATUS_LABEL } from "@/lib/types";
import { timeLeft } from "@/lib/common";
import { formatDate, formatDateTime } from "@/lib/format";
import { errorMessage, isApiError } from "@/lib/api/client";
import { personName, topicText, type CoffeeBox, type CoffeeChatItem, type CoffeeFilter } from "@/lib/api/coffee";
import type { Id } from "@/lib/api/posts";
import { useApi } from "@/lib/api/useApi";
import { useCoffeeService } from "@/lib/api/useCoffeeService";
import { Avatar, AvatarGroup } from "@/components/ui/Avatar";
import { StatusBadge } from "@/components/ui/Badge";
import { Button, Chip } from "@/components/ui/Button";
import { Card, EmptyState, Skeleton, Tabs } from "@/components/ui/Misc";
import { useToast } from "@/components/ui/Toast";
import { CoffeeChatDetail, statusTone } from "@/components/domain/CoffeeChatDetail";

type Tab = "received" | "sent";
const FILTERS: { value: CoffeeFilter; label: string }[] = [
  { value: "ALL", label: "전체" },
  { value: "PENDING", label: "대기 중" },
  { value: "ACTIVE", label: "만남 준비 중" },
  { value: "ENDED", label: "종료" },
];
const SIZE = 20;

export default function CoffeePage() {
  return (
    <Suspense fallback={<Skeleton className="h-[600px] w-full" />}>
      <CoffeeList />
    </Suspense>
  );
}

/** ★ M-01 내 커피챗 / 받은 신청 · ◇ M-02 보낸 신청 — 목록 440 + 오른쪽 상세 (COFFEE-02·03) */
function CoffeeList() {
  const params = useSearchParams();
  const router = useRouter();
  const toast = useToast();
  const service = useCoffeeService();
  const [tab, setTab] = useState<Tab>(params.get("tab") === "sent" ? "sent" : "received");
  const [filter, setFilter] = useState<CoffeeFilter>("ALL");
  const [selectedId, setSelectedId] = useState<string | null>(params.get("id"));
  const box: CoffeeBox = tab === "received" ? "RECEIVED" : "SENT";

  const first = useApi((signal) => service.list(box, filter, 0, SIZE, signal), [service, box, filter]);
  const [more, setMore] = useState<{ items: CoffeeChatItem[]; page: number; hasNext: boolean } | null>(null);
  const [loadingMore, setLoadingMore] = useState(false);
  const [seen, setSeen] = useState(first.data);
  if (seen !== first.data) {
    setSeen(first.data);
    setMore(null);
  }
  const list = [...(first.data?.items ?? []), ...(more?.items ?? [])];
  const hasNext = more ? more.hasNext : !!first.data?.hasNext;
  const selected = list.find((c) => String(c.id) === selectedId) ?? (selectedId && first.data ? null : list[0]);
  const detailId: Id | null = selected?.id ?? selectedId;

  const loadMore = async () => {
    setLoadingMore(true);
    try {
      const page = (more?.page ?? 0) + 1;
      const res = await service.list(box, filter, page, SIZE);
      setMore((m) => ({ items: [...(m?.items ?? []), ...res.items], page, hasNext: res.hasNext }));
    } catch (e) {
      toast(errorMessage(e), { kind: "error" });
    } finally {
      setLoadingMore(false);
    }
  };

  const changeTab = (t: Tab) => {
    setTab(t);
    setFilter("ALL");
    setSelectedId(null);
    router.replace(`/coffee${t === "sent" ? "?tab=sent" : ""}`);
  };

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-end justify-between">
        <h1 className="t-title text-ink-900">내 커피챗</h1>
        {first.data && (
          <p className="text-[13px] text-ink-500">
            받은 대기 {first.data.pendingReceivedCount} · 만남 준비 중 {first.data.activeCount}
          </p>
        )}
      </div>
      <Tabs
        value={tab}
        onChange={changeTab}
        items={[
          { value: "received", label: "받은 신청" },
          { value: "sent", label: "보낸 신청" },
        ]}
      />

      <div className="grid grid-cols-[440px_minmax(0,1fr)] gap-6 pt-2">
        <Card className="h-fit overflow-hidden rounded-[10px]">
          <div className="flex gap-2 border-b border-line-200 px-4 py-[14px]">
            {FILTERS.map((f) => (
              <Chip key={f.value} selected={filter === f.value} onClick={() => setFilter(f.value)}>
                {f.label}
              </Chip>
            ))}
          </div>
          {first.error && !first.data ? (
            <EmptyState compact title="커피챗을 불러오지 못했어요" description={errorMessage(first.error)} action={{ label: "다시 시도", onClick: first.reload }} />
          ) : !first.data ? (
            <div className="flex flex-col gap-3 p-4" aria-busy="true">
              {Array.from({ length: 3 }, (_, i) => (
                <Skeleton key={i} className="h-[88px] w-full" />
              ))}
            </div>
          ) : list.length === 0 ? (
            <EmptyState compact title={tab === "received" ? "받은 신청이 없어요" : "보낸 신청이 없어요"} description={tab === "received" ? "커피챗 받기를 켜두면 사자들이 신청할 수 있어요" : "동문 찾기에서 사자에게 커피챗을 신청해보세요"} action={tab === "received" ? { label: "커피챗 설정", href: "/me/coffee-settings" } : { label: "동문 찾기", href: "/alumni" }} />
          ) : (
            <ul className={cn(first.loading && "opacity-60")}>
              {list.map((c) => (
                <ListRow key={c.id} chat={c} tab={tab} active={String(detailId) === String(c.id)} onClick={() => setSelectedId(String(c.id))} />
              ))}
            </ul>
          )}
          {hasNext && (
            <div className="flex justify-center border-t border-line-200 py-3">
              <Button size="S" variant="secondary" loading={loadingMore} onClick={loadMore}>
                더 보기
              </Button>
            </div>
          )}
        </Card>

        <Card className="h-fit rounded-[10px] p-7">
          {detailId !== null && detailId !== undefined ? (
            <DetailPane key={String(detailId)} id={detailId} initialAction={params.get("id") === String(detailId) ? (params.get("action") as "accept" | "reject" | null) : null} onChanged={first.reload} />
          ) : (
            <EmptyState title="신청을 선택해주세요" />
          )}
        </Card>
      </div>
    </div>
  );
}

/** 오른쪽 상세 (COFFEE-03). 상태가 바뀌면 목록도 다시 불러온다 */
function DetailPane({ id, initialAction, onChanged }: { id: Id; initialAction: "accept" | "reject" | null; onChanged: () => void }) {
  const service = useCoffeeService();
  const { data, error, reload, setData } = useApi(() => service.get(id), [service, id]);
  if (error && !data) {
    if (isApiError(error) && error.status === 404) return <EmptyState title="커피챗을 찾을 수 없어요" description="취소됐거나 볼 수 없는 신청이에요" />;
    return <EmptyState title="커피챗을 불러오지 못했어요" description={errorMessage(error)} action={{ label: "다시 시도", onClick: reload }} />;
  }
  if (!data) return <Skeleton className="h-[400px] w-full" />;
  return (
    <CoffeeChatDetail
      chat={data}
      compact
      initialAction={initialAction}
      onChanged={(next) => {
        if (next) setData(() => next);
        else reload();
        onChanged();
      }}
    />
  );
}

function ListRow({ chat, tab, active, onClick }: { chat: CoffeeChatItem; tab: Tab; active: boolean; onClick: () => void }) {
  const other = chat.counterpart;
  const part = other.lionParts?.[0] ?? "ETC";
  const members = chat.members.filter((m) => !m.isDeleted);
  const group = chat.members.length > 0;
  const left = timeLeft(chat.expiresAt ?? undefined);
  return (
    <li className="border-b border-line-200 last:border-b-0">
      <button type="button" onClick={onClick} className={cn("flex w-full items-start gap-4 px-4 py-4 text-left hover:bg-bg-50", active && "bg-light-card")}>
        {group && tab === "received" ? (
          <AvatarGroup people={[other, ...members].map((p) => ({ name: p.name ?? "", part: p.lionParts?.[0] ?? "ETC" }))} size={32} />
        ) : (
          <Avatar name={other.isDeleted ? undefined : other.name ?? undefined} part={part} size={40} withdrawn={other.isDeleted} />
        )}
        <div className="flex min-w-0 flex-1 flex-col gap-1.5">
          <div className="flex items-baseline justify-between gap-2">
            <p className="truncate">
              <span className="t-body-bold text-ink-900">
                {personName(other)}
                {group && tab === "received" ? ` 외 ${chat.members.length}명` : ""}
              </span>
              {!other.isDeleted && <span className="ml-1.5 text-[13px] text-ink-500">{other.generation}기</span>}
            </p>
            <span className="shrink-0 text-[12px] text-ink-400">{formatDate(chat.createdAt)}</span>
          </div>
          <p className="truncate text-[13px] text-ink-700">
            {topicText(chat)} · {chat.reason}
          </p>
          <div className="flex items-center gap-2.5 pt-1">
            <StatusBadge tone={statusTone(chat.status)}>{COFFEE_STATUS_LABEL[chat.status]}</StatusBadge>
            {tab === "sent" && chat.myRole === "MEMBER" && <span className="text-[12px] text-ink-500">함께 신청</span>}
            {chat.status === "PENDING" && <span className={cn("text-[12px] font-semibold", left.urgent ? "text-orange" : "text-ink-700")}>{left.text}</span>}
            {chat.status === "SCHEDULED" && chat.scheduledAt && <span className="text-[12px] text-ink-700">{formatDateTime(chat.scheduledAt)}</span>}
          </div>
        </div>
      </button>
    </li>
  );
}
