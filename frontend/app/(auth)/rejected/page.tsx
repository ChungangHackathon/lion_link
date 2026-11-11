"use client";

import { Gnb } from "@/components/layout/Gnb";
import { Button } from "@/components/ui/Button";
import { Glyph } from "@/components/ui/Icon";

/** ◇ A-14 반려 안내 / 기본 */
export default function RejectedPage() {
  return (
    <div className="flex min-h-full flex-1 flex-col">
      <Gnb variant="logo" />
      <main className="flex flex-1 justify-center px-4 pt-[188px]">
        <div className="flex w-full max-w-[480px] flex-col items-center gap-5 text-center">
          <span className="grid size-20 place-items-center rounded-full bg-bg-100 text-ink-500">
            <Glyph name="alert" size={34} />
          </span>
          <h1 className="text-[22px] font-bold text-ink-900">동문 확인이 어려워 가입이 반려됐어요.</h1>
          <p className="text-[15px] leading-[1.6] text-ink-500">
            사유: <span className="font-semibold text-ink-700">동문 명단에서 이름·기수를 찾지 못했어요</span>
            <br />
            이름이나 기수를 잘못 입력했다면 초대 링크로 다시 가입할 수 있어요.
          </p>
          <div className="w-full rounded-[8px] bg-bg-100 px-4 py-3 text-left text-[13px] leading-5 text-ink-700">
            본인이 맞는데 반려됐다면 운영진(likelion@cau.ac.kr)에게 활동 당시 자료(수료증·단체 사진 등)와 함께 메일을 보내주세요.
          </div>
          <div className="flex gap-2">
            <Button variant="secondary" href="/login">
              로그인으로
            </Button>
            <Button href="mailto:likelion@cau.ac.kr">운영진에게 문의</Button>
          </div>
        </div>
      </main>
    </div>
  );
}
