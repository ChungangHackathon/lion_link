"use client";

import { useState } from "react";
import { useStore } from "@/lib/store";
import type { Invite } from "@/lib/types";
import { formatDate } from "@/lib/format";
import { StatusBadge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Glyph } from "@/components/ui/Icon";
import { Input, Select } from "@/components/ui/Input";
import { ConfirmModal, Modal } from "@/components/ui/Modal";
import { Card, EmptyState, Tabs } from "@/components/ui/Misc";
import { useToast } from "@/components/ui/Toast";

type Tab = "ALL" | Invite["status"];
const LABEL: Record<Invite["status"], string> = { UNUSED: "미사용", USED: "사용됨", EXPIRED: "만료됨", CANCELED: "취소됨" };
const TONE: Record<Invite["status"], "filled" | "outline" | "gray"> = { UNUSED: "outline", USED: "filled", EXPIRED: "gray", CANCELED: "gray" };

/** ★ AD-02 초대 관리 · ◇[O] AD-03 링크 만들기 · ◇[O] AD-04 결과 · ◇ AD-07 CSV 일괄 · CF-17 · CF-19 */
export default function AdminInvitesPage() {
  const toast = useToast();
  const { invites, signups, createInvites, reissueInvite, cancelInvite } = useStore();
  const [tab, setTab] = useState<Tab>("ALL");
  const [create, setCreate] = useState(false);
  const [result, setResult] = useState<Invite | null>(null);
  const [csv, setCsv] = useState(false);
  const [csvDone, setCsvDone] = useState(false);
  const [confirm, setConfirm] = useState<{ kind: "reissue" | "cancel" | "resend"; invite: Invite } | null>(null);
  const [name, setName] = useState("");
  const [gen, setGen] = useState(14);
  const [email, setEmail] = useState("");
  const [memo, setMemo] = useState("");

  const list = invites.filter((i) => tab === "ALL" || i.status === tab);
  const link = (i: Invite) => `${typeof location !== "undefined" ? location.origin : ""}/invite/${i.token}`;
  const copy = (text: string, msg: string) => {
    navigator.clipboard?.writeText(text);
    toast(msg);
  };

  return (
    <div className="flex flex-col gap-5">
      <div className="flex items-center justify-between">
        <h1 className="t-title text-ink-900">초대 관리</h1>
        <div className="flex gap-2">
          <Button variant="secondary" onClick={() => { setCsv(true); setCsvDone(false); }}>
            CSV로 일괄 생성
          </Button>
          <Button onClick={() => setCreate(true)}>링크 만들기</Button>
        </div>
      </div>
      <Tabs
        value={tab}
        onChange={setTab}
        items={[
          { value: "ALL", label: "전체" },
          { value: "UNUSED", label: "미사용" },
          { value: "USED", label: "사용됨" },
          { value: "EXPIRED", label: "만료됨" },
          { value: "CANCELED", label: "취소됨" },
        ]}
      />
      {list.length === 0 ? (
        <EmptyState title="아직 보낸 초대가 없어요" action={{ label: "링크 만들기", onClick: () => setCreate(true) }} />
      ) : (
        <Card className="overflow-x-auto">
          <table className="w-full text-[13px]">
            <thead className="bg-bg-50 text-left text-ink-500">
              <tr>
                {["이름", "기수", "상태", "만든 날짜", "만료일", "가입일", "가입한 구글 이메일", "승인 상태", "처리"].map((h) => (
                  <th key={h} className="whitespace-nowrap px-3 py-3 font-medium">
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-line-200">
              {list.map((i) => {
                const signup = signups.find((s) => s.name === i.name && s.generation === i.generation);
                return (
                  <tr key={i.id}>
                    <td className="px-3 py-3 font-semibold text-ink-900">{i.name}</td>
                    <td className="px-3 py-3">{i.generation}기</td>
                    <td className="px-3 py-3">
                      <StatusBadge tone={TONE[i.status]}>{LABEL[i.status]}</StatusBadge>
                    </td>
                    <td className="px-3 py-3 text-ink-500">{formatDate(i.createdAt)}</td>
                    <td className="px-3 py-3 text-ink-500">{formatDate(i.expiresAt)}</td>
                    <td className="px-3 py-3 text-ink-500">{i.status === "USED" && signup ? formatDate(signup.appliedAt) : "-"}</td>
                    <td className="px-3 py-3 text-ink-700">{i.status === "USED" ? signup?.email ?? i.email ?? "-" : "-"}</td>
                    <td className="px-3 py-3">{i.status === "USED" && signup ? <StatusBadge tone={signup.status === "APPROVED" ? "filled" : signup.status === "PENDING" ? "outline" : "gray"}>{signup.status === "APPROVED" ? "승인됨" : signup.status === "PENDING" ? "승인 대기" : "반려됨"}</StatusBadge> : "-"}</td>
                    <td className="px-3 py-3">
                      {i.status === "UNUSED" && (
                        <div className="flex gap-1">
                          <Button size="S" variant="secondary" onClick={() => copy(link(i), "링크를 복사했어요")}>
                            복사
                          </Button>
                          <Button size="S" variant="secondary" onClick={() => setConfirm({ kind: "reissue", invite: i })}>
                            재발급
                          </Button>
                          <Button size="S" variant="text" onClick={() => setConfirm({ kind: "cancel", invite: i })}>
                            취소
                          </Button>
                        </div>
                      )}
                      {(i.status === "EXPIRED" || i.status === "CANCELED" || (i.status === "USED" && signup?.status === "REJECTED")) && (
                        <Button size="S" variant="secondary" onClick={() => setConfirm({ kind: "resend", invite: i })}>
                          초대 다시 보내기
                        </Button>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </Card>
      )}

      {/* ◇[O] AD-03 링크 만들기 */}
      <Modal
        open={create}
        onClose={() => setCreate(false)}
        title="링크 만들기"
        footer={
          <>
            <Button variant="secondary" onClick={() => setCreate(false)}>
              취소
            </Button>
            <Button
              disabled={name.trim().length < 2 || name.length > 20}
              onClick={() => {
                const [inv] = createInvites([{ name: name.trim(), generation: gen, email: email.trim() || undefined }]);
                setCreate(false);
                setName("");
                setEmail("");
                setMemo("");
                setResult(inv);
              }}
            >
              링크 만들기
            </Button>
          </>
        }
      >
        <div className="flex flex-col gap-4">
          <Input label="이름" required placeholder="2~20자" value={name} onChange={(e) => setName(e.target.value)} />
          <Select label="기수" required value={gen} onChange={(e) => setGen(Number(e.target.value))}>
            {Array.from({ length: 14 }, (_, i) => 14 - i).map((g) => (
              <option key={g} value={g}>
                {g}기
              </option>
            ))}
          </Select>
          <Input label="이메일 (선택)" type="email" placeholder="가입 시 구글 이메일과 대조해요" value={email} onChange={(e) => setEmail(e.target.value)} />
          <Input label="연락처 메모 (선택)" placeholder="예) 카톡으로 보냄" value={memo} onChange={(e) => setMemo(e.target.value)} />
        </div>
      </Modal>

      {/* ◇[O] AD-04 링크 생성 결과 */}
      <Modal
        open={!!result}
        onClose={() => setResult(null)}
        title={result ? `${result.name} · ${result.generation}기 초대 링크` : ""}
        footer={
          <Button variant="secondary" onClick={() => setResult(null)}>
            닫기
          </Button>
        }
      >
        {result && (
          <div className="flex flex-col gap-3">
            <div className="flex items-center justify-between rounded-[8px] border border-line-200 bg-bg-50 px-4 py-3 text-[13px] text-ink-900">
              <span className="truncate">{link(result)}</span>
              <Glyph name="copy" size={16} className="shrink-0 text-ink-500" />
            </div>
            <div className="flex gap-2">
              <Button variant="secondary" className="flex-1" onClick={() => copy(link(result), "링크를 복사했어요")}>
                링크만 복사
              </Button>
              <Button className="flex-1" onClick={() => copy(`[LION:LINK] ${result.name}님, 멋사 중앙대 동문 네트워크에 초대해요. 7일 안에 가입해주세요.\n${link(result)}`, "안내 문구와 함께 복사했어요")}>
                안내 문구와 함께 복사
              </Button>
            </div>
            <div className="rounded-[8px] bg-bg-50 p-4 text-[13px] leading-6 text-ink-700">
              <p className="t-label2 mb-1 text-ink-500">안내 문구 미리보기</p>
              [LION:LINK] {result.name}님, 멋사 중앙대 동문 네트워크 ‘멋사동네’에 초대해요. 아래 링크로 7일 안에 가입해주세요. 가입 후 운영진 승인까지 1~2일 걸려요.
              <br />
              {link(result)}
            </div>
          </div>
        )}
      </Modal>

      {/* ◇ AD-07 CSV 일괄 생성 */}
      <Modal
        open={csv}
        onClose={() => setCsv(false)}
        title="CSV 일괄 생성"
        width={560}
        footer={
          <Button variant="secondary" onClick={() => setCsv(false)}>
            닫기
          </Button>
        }
      >
        {!csvDone ? (
          <button
            type="button"
            onClick={() => {
              createInvites([
                { name: "김한강", generation: 14 },
                { name: "이노을", generation: 14, email: "noeul2@cau.ac.kr" },
                { name: "정바다", generation: 14 },
              ]);
              setCsvDone(true);
            }}
            className="flex w-full flex-col items-center gap-1 rounded-[12px] border border-dashed border-line-300 bg-bg-50 px-6 py-10 hover:bg-bg-100"
          >
            <Glyph name="image" size={22} className="text-ink-700" />
            <span className="t-label1 text-ink-900">CSV 파일을 끌어다 놓거나 클릭해서 올려주세요</span>
            <span className="text-[12px] text-ink-500">.csv · 1MB 이하 · 최대 200행 · 열: 이름, 기수, 이메일(선택)</span>
          </button>
        ) : (
          <div className="flex flex-col gap-3 rounded-[8px] border border-line-200 p-4 text-[13px]">
            <p className="t-label1 text-ink-900">14기_명단.csv · 5행</p>
            <p>
              <span className="font-semibold text-green">성공 3행</span> · <span className="font-semibold text-orange">실패 2행</span>
            </p>
            <ul className="list-disc pl-5 text-ink-700">
              <li>3행: 기수가 숫자가 아니에요</li>
              <li>4행: 이름은 2~20자로 입력해주세요</li>
            </ul>
            <Button size="S" variant="secondary" className="w-fit" onClick={() => toast("결과 CSV를 내려받았어요")}>
              결과 CSV 내려받기
            </Button>
          </div>
        )}
      </Modal>

      {/* CF-17 재발급·취소 · CF-19 다시 보내기 */}
      <ConfirmModal
        open={!!confirm}
        onClose={() => setConfirm(null)}
        onConfirm={() => {
          if (!confirm) return;
          const { kind, invite } = confirm;
          if (kind === "cancel") {
            cancelInvite(invite.id);
            toast("초대를 취소했어요");
          } else if (kind === "reissue") {
            reissueInvite(invite.id);
            toast("새 링크를 만들었어요");
            setResult({ ...invite, token: "reissued" });
          } else {
            const [inv] = createInvites([{ name: invite.name, generation: invite.generation, email: invite.email }]);
            setResult(inv);
          }
          setConfirm(null);
        }}
        title={confirm?.kind === "cancel" ? "초대를 취소할까요?" : confirm?.kind === "reissue" ? "초대를 재발급할까요?" : `${confirm?.invite.name}님에게 초대를 다시 보낼까요?`}
        description={confirm?.kind === "cancel" ? "취소하면 이 링크로 가입할 수 없어요." : confirm?.kind === "reissue" ? "기존 링크는 더 이상 쓸 수 없고, 같은 이름·기수로 새 링크가 만들어져요." : "같은 이름·기수·초대 이메일로 새 링크를 만들어요."}
        confirmLabel={confirm?.kind === "cancel" ? "초대 취소" : confirm?.kind === "reissue" ? "확인" : "새 링크 만들기"}
        danger={confirm?.kind === "cancel"}
      />
    </div>
  );
}
