import { Gnb } from "@/components/layout/Gnb";
import { Button } from "@/components/ui/Button";

/** ◇ C-03 403 / 접근 권한 없음 */
export default function ForbiddenPage() {
  return (
    <div className="flex min-h-full flex-1 flex-col">
      <Gnb />
      <main className="flex flex-1 flex-col items-center justify-center gap-4 py-32 text-center">
        <p className="text-[64px] font-extrabold text-line-300">403</p>
        <h1 className="text-[22px] font-bold text-ink-900">접근 권한이 없어요</h1>
        <p className="t-body2 text-ink-500">관리자만 들어올 수 있는 페이지예요.</p>
        <Button href="/home">홈으로</Button>
      </main>
    </div>
  );
}
