"use client";

import { useState } from "react";
import { useStore } from "@/lib/store";
import { Button } from "@/components/ui/Button";
import { Switch } from "@/components/ui/Input";
import { Modal } from "@/components/ui/Modal";
import { Card } from "@/components/ui/Misc";

/** ◇ N-01 알림 설정 / 기본 · ★ N-02 알림 메일 템플릿 미리보기 · ◇ N-04 수신 거부 완료 */
export default function NotificationSettingsPage() {
  const { notiSettings, setNotiSettings } = useStore();
  const [preview, setPreview] = useState(false);
  const [unsubscribed, setUnsubscribed] = useState(false);

  return (
    <div className="flex flex-col gap-6">
      <h1 className="t-title text-ink-900">알림 설정</h1>
      <Card className="flex flex-col divide-y divide-line-200 p-2">
        <Row title="커피챗 신청 메일 받기" desc="커피챗 신청을 받으면 메일로 알려드려요" on={notiSettings.emailCoffee} onChange={(v) => setNotiSettings({ emailCoffee: v })} />
        <Row title="메시지 요청 메일 받기" desc="메시지 요청을 받으면 메일로 알려드려요" on={notiSettings.emailMessage} onChange={(v) => setNotiSettings({ emailMessage: v })} />
        <Row title="가입 승인 메일 받기" desc="가입이 승인되면 메일로 알려드려요" on={notiSettings.emailApproved} onChange={(v) => setNotiSettings({ emailApproved: v })} />
      </Card>
      <p className="text-[13px] text-ink-500">알림 메일은 가입한 구글 이메일 jungang.park@gmail.com 으로 가요. 메일 안의 ‘이 알림 그만 받기’를 눌러도 여기서 다시 켤 수 있어요.</p>
      <Button variant="secondary" className="w-fit" onClick={() => setPreview(true)}>
        알림 메일 미리보기
      </Button>

      {/* ★ N-02 알림 메일 템플릿 */}
      <Modal open={preview} onClose={() => { setPreview(false); setUnsubscribed(false); }} title="알림 메일 미리보기" width={640}>
        <div className="rounded-[8px] border border-line-200 bg-bg-50 p-5 text-[14px] text-ink-900">
          <p className="text-[12px] text-ink-500">제목: [LION:LINK] 최흑석(14기)님이 커피챗을 신청했어요</p>
          <p className="text-[12px] text-ink-500">보낸 사람: LION:LINK · 받는 사람: jungang.park@gmail.com</p>
          <div className="mt-4 rounded-[8px] border border-line-200 bg-white p-6">
            {unsubscribed ? (
              <>
                <p className="t-body-bold">앞으로 커피챗 신청 메일을 보내지 않아요</p>
                <p className="mt-2 text-[13px] text-ink-500">다시 받으려면 마이페이지 &gt; 알림 설정에서 켜주세요.</p>
              </>
            ) : (
              <>
                <p className="t-title3 text-primary">LION:LINK</p>
                <p className="mt-4 leading-6">
                  <b>최흑석(14기)</b>님이 커피챗을 신청했어요. 주제: <b>프로젝트·개발</b>
                </p>
                <p className="mt-1 text-[13px] text-ink-500">11/18까지 답하지 않으면 신청이 자동으로 만료돼요.</p>
                <div className="mt-4">
                  <Button href="/coffee/c-1">확인하러 가기</Button>
                </div>
                <button type="button" onClick={() => { setUnsubscribed(true); setNotiSettings({ emailCoffee: false }); }} className="mt-6 text-[12px] text-ink-400 underline">
                  이 알림 그만 받기
                </button>
              </>
            )}
          </div>
        </div>
      </Modal>
    </div>
  );
}

function Row({ title, desc, on, onChange }: { title: string; desc: string; on: boolean; onChange: (v: boolean) => void }) {
  return (
    <div className="flex items-center justify-between px-4 py-4">
      <div>
        <p className="t-label1 text-ink-900">{title}</p>
        <p className="text-[12px] text-ink-500">{desc}</p>
      </div>
      <Switch checked={on} onChange={onChange} label={title} />
    </div>
  );
}
