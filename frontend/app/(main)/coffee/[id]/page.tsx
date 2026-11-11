"use client";

import { useSearchParams } from "next/navigation";
import { Suspense, use } from "react";
import { useStore } from "@/lib/store";
import { PageHeader } from "@/components/layout/PageHeader";
import { EmptyState, Skeleton } from "@/components/ui/Misc";
import { CoffeeChatDetail } from "@/components/domain/CoffeeChatDetail";

/** ★ D-01 커피챗 상세 (전체 화면) — D-02~D-07 variants by status/role */
function CoffeeDetailInner({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  return <Detail id={id} />;
}

function Detail({ id }: { id: string }) {
  const { coffeeChats } = useStore();
  const search = useSearchParams();
  const chat = coffeeChats.find((c) => c.id === id);
  if (!chat) return <EmptyState title="커피챗을 찾을 수 없어요" action={{ label: "내 커피챗으로", href: "/coffee" }} />;
  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="커피챗 상세" back="/coffee" />
      <CoffeeChatDetail chat={chat} initialAction={search.get("action") as "accept" | "reject" | null} />
    </div>
  );
}

/** Cache Components: params must be read inside Suspense. */
export default function CoffeeDetailPage({ params }: { params: Promise<{ id: string }> }) {
  return (
    <Suspense fallback={<Skeleton className="h-[400px] w-full" />}>
      <CoffeeDetailInner params={params} />
    </Suspense>
  );
}
