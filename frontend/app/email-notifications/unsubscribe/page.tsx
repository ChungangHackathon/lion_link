"use client";

import { Suspense } from "react";
import { useSearchParams } from "next/navigation";
import { errorMessage } from "@/lib/api/client";
import { EMAIL_TYPE_LABEL, EMAIL_TYPES, type EmailType } from "@/lib/api/notifications";
import { useApi } from "@/lib/api/useApi";
import { useNotificationService } from "@/lib/api/useNotificationService";
import { Gnb } from "@/components/layout/Gnb";
import { Button } from "@/components/ui/Button";
import { Icon } from "@/components/ui/Icon";
import { Skeleton } from "@/components/ui/Misc";

/** ◇ N-04 수신 거부 완료. 메일의 [그만 받기] 링크가 여기로 온다 (NOTI-06, 로그인 없이) */
export default function UnsubscribePage() {
  return (
    <div className="flex min-h-full flex-1 flex-col">
      <Gnb variant="logo" />
      <main className="flex flex-1 flex-col items-center justify-center px-4 py-32">
        <Suspense fallback={<Skeleton className="h-[200px] w-[420px]" />}>
          <Unsubscribe />
        </Suspense>
      </main>
    </div>
  );
}

function Unsubscribe() {
  const params = useSearchParams();
  const token = params.get("token") ?? "";
  const rawType = params.get("type") ?? "";
  const type = (EMAIL_TYPES as string[]).includes(rawType) ? (rawType as EmailType) : null;
  const service = useNotificationService();
  const valid = !!token && !!type;
  // 주소의 token·type이 바뀔 때만 다시 부른다 (같은 요청을 여러 번 보내도 결과는 같다)
  const { data, error } = useApi(() => service.unsubscribe(token, type!).then(() => true), [token, type], valid);

  if (!valid || error)
    return (
      <div className="flex w-full max-w-[440px] flex-col items-center gap-4 rounded-[12px] border border-line-200 bg-white px-8 py-10 text-center">
        <Icon name="alert" size={32} className="text-orange" />
        <h1 className="text-[20px] font-bold text-ink-900">수신 거부를 처리하지 못했어요</h1>
        <p className="t-body2 text-ink-500">{error ? errorMessage(error) : "수신 거부 링크가 올바르지 않아요."}</p>
        <p className="text-[13px] text-ink-500">로그인한 뒤 마이페이지 &gt; 알림 설정에서 직접 끌 수 있어요.</p>
        <Button href="/me/notifications">알림 설정으로</Button>
      </div>
    );

  if (!data) return <Skeleton className="h-[200px] w-[420px]" />;

  return (
    <div className="flex w-full max-w-[440px] flex-col items-center gap-4 rounded-[12px] border border-line-200 bg-white px-8 py-10 text-center">
      <Icon name="bell-off" size={32} className="text-ink-500" />
      <h1 className="text-[20px] font-bold text-ink-900">앞으로 {EMAIL_TYPE_LABEL[type]} 메일을 보내지 않아요</h1>
      <p className="t-body2 text-ink-500">다른 메일은 그대로 받아요. 다시 받으려면 마이페이지 &gt; 알림 설정에서 켜주세요.</p>
      <Button href="/me/notifications" variant="secondary">
        알림 설정으로
      </Button>
    </div>
  );
}
