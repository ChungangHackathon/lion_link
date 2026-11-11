"use client";

import { Button } from "@/components/ui/Button";

/** ◇ C-05 500 · 네트워크 오류 / 다시 시도 */
export default function ErrorPage({ reset }: { error: Error; reset: () => void }) {
  return (
    <main className="flex flex-1 flex-col items-center justify-center gap-4 py-32 text-center">
      <p className="text-[64px] font-extrabold text-line-300">500</p>
      <h1 className="text-[22px] font-bold text-ink-900">잠시 후 다시 시도해주세요</h1>
      <p className="t-body2 text-ink-500">네트워크 상태를 확인하거나, 잠시 뒤 다시 시도해주세요.</p>
      <Button onClick={reset}>다시 시도</Button>
    </main>
  );
}
