import { redirect } from "next/navigation";

/** 전체 화면 상세(D-01~D-07)는 디자인에서 빠졌어요 — 내 커피챗 오른쪽 영역으로 보냅니다. */
export default async function CoffeeDetailRedirect({ params, searchParams }: { params: Promise<{ id: string }>; searchParams: Promise<{ action?: string }> }) {
  const { id } = await params;
  const { action } = await searchParams;
  redirect(`/coffee?id=${id}${action ? `&action=${action}` : ""}`);
}
