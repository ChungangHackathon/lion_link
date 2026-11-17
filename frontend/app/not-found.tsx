import { Gnb } from "@/components/layout/Gnb";
import { Button } from "@/components/ui/Button";

/** ◇ C-04 404 / 찾을 수 없음 */
export default function NotFound() {
  return (
    <div className="flex min-h-full flex-1 flex-col">
      <Gnb />
      <main className="flex flex-1 flex-col items-center justify-center gap-4 py-32 text-center">
        <p className="text-[64px] font-extrabold text-line-300">404</p>
        <h1 className="text-[22px] font-bold text-ink-900">찾을 수 없는 페이지예요</h1>
        <p className="t-body2 text-ink-500">주소가 잘못됐거나 삭제된 페이지예요.</p>
        <Button href="/home">홈으로</Button>
      </main>
    </div>
  );
}
