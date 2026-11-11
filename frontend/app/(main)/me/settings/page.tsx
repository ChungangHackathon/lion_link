"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { useSession } from "@/lib/session";
import { Button } from "@/components/ui/Button";
import { Modal } from "@/components/ui/Modal";
import { Card } from "@/components/ui/Misc";

/** ◇ X-03 설정 / 기본 · ◇[O] CF-3 탈퇴 확인 → A-04c 구글 본인 확인 → 로그인 */
export default function SettingsPage() {
  const router = useRouter();
  const { session, logout } = useSession();
  const [confirm, setConfirm] = useState(false);
  const isAdmin = session.role === "ADMIN";
  return (
    <div className="flex flex-col gap-6">
      <h1 className="t-title text-ink-900">설정</h1>
      <Card className="flex flex-col divide-y divide-line-200 px-6">
        <div className="flex items-center justify-between py-4 text-[14px]">
          <span className="text-ink-500">로그인 계정</span>
          <span className="text-ink-900">jungang.park@gmail.com (구글)</span>
        </div>
        <div className="flex items-center justify-between py-4 text-[14px]">
          <span className="text-ink-500">가입일</span>
          <span className="text-ink-900">2026년 10월 21일</span>
        </div>
      </Card>
      <div>
        {isAdmin ? (
          <p className="text-[13px] text-ink-500">관리자 계정은 탈퇴할 수 없어요. 다른 운영진에게 관리자 권한을 넘긴 뒤 탈퇴해주세요.</p>
        ) : (
          <Button variant="danger" onClick={() => setConfirm(true)}>
            탈퇴하기
          </Button>
        )}
      </div>

      <Modal
        open={confirm}
        onClose={() => setConfirm(false)}
        title="탈퇴하면 되돌릴 수 없어요"
        footer={
          <>
            <Button variant="secondary" onClick={() => setConfirm(false)}>
              취소
            </Button>
            <Button
              variant="danger"
              onClick={() => {
                // A-04c 구글 계정 선택 / 탈퇴 본인 확인 (구글 화면 · 디자인 대상 아님)
                logout();
                router.push("/login");
              }}
            >
              탈퇴하기
            </Button>
          </>
        }
      >
        <ul className="list-disc pl-5 text-[14px] leading-7 text-ink-700">
          <li>진행 중인 커피챗은 취소돼요</li>
          <li>내가 만든 모집 중인 번개·모집글은 종료돼요</li>
          <li>글·댓글은 ‘탈퇴한 사자’로 남아요</li>
        </ul>
        <p className="mt-3 text-[13px] text-ink-500">탈퇴하기를 누르면 구글 계정으로 본인 확인을 한 번 더 해요.</p>
      </Modal>
    </div>
  );
}
