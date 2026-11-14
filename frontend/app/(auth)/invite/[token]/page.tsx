import { redirect } from "next/navigation";

/** 초대 링크 가입(A-02·A-05~07)은 디자인에서 빠졌어요 — 가입 신청(A-01)으로 보냅니다. */
export default function InviteRedirect() {
  redirect("/signup");
}
