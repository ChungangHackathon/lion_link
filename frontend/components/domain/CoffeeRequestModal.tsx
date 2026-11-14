"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { cn } from "@/lib/cn";
import type { Alumni, MeetingMode } from "@/lib/types";
import { useStore } from "@/lib/store";
import { profileDetail } from "@/lib/mock/profile";
import { ME_ID } from "@/lib/mock/users";
import { Avatar } from "@/components/ui/Avatar";
import { OutlineTag } from "@/components/ui/Badge";
import { Button, Chip } from "@/components/ui/Button";
import { Glyph } from "@/components/ui/Icon";
import { Input, Textarea } from "@/components/ui/Input";
import { ConfirmModal, Modal } from "@/components/ui/Modal";
import { useToast } from "@/components/ui/Toast";
import { AlumniPicker } from "./AlumniPicker";

const TOPICS = ["커리어·이직", "취업 준비", "프로젝트·개발", "기타(직접 입력)"];
const REASON_PRESETS = ["취업 준비 중인데 …", "프로젝트를 하다가 막힌 부분이 있는데 …", "이직을 고민하고 있는데 …"];

/**
 * ★[O] R-01 커피챗 신청 모달 / 1:1 · ◇ R-01b 여러 명 · R-02 동문 추가 · R-05 미리보기.
 * 취소(변경 있음) → C-08 이탈 확인. 신청 보내기 → M-02.
 * `fromRecruitId` + `preMembers`: 모집글(K-01)에서 온 신청 — 멤버 자동 채움·수정 불가(R-04).
 */
export function CoffeeRequestModal({ open, onClose, target, preMembers, fromRecruitId }: { open: boolean; onClose: () => void; target: Alumni; preMembers?: string[]; fromRecruitId?: string }) {
  const router = useRouter();
  const toast = useToast();
  const { alumni, createCoffeeChat } = useStore();
  const detail = profileDetail(target.id);

  const [guide, setGuide] = useState(false);
  const [members, setMembers] = useState<string[]>(preMembers ?? []);
  const [picker, setPicker] = useState(false);
  const [topic, setTopic] = useState<string | null>(null);
  const [topicEtc, setTopicEtc] = useState("");
  const [reason, setReason] = useState("");
  const [mode, setMode] = useState<MeetingMode | null>(null);
  const [memo, setMemo] = useState("");
  const [questions, setQuestions] = useState<string[]>([""]);
  const [preview, setPreview] = useState(false);
  const [leave, setLeave] = useState(false);
  const [sending, setSending] = useState(false);

  const dirty = !!topic || reason.length > 0 || !!mode || memo.length > 0 || questions.some((q) => q.length > 0) || (members.length > 0 && !preMembers);
  const topicValue = topic === "기타(직접 입력)" ? topicEtc.trim() : topic;
  const qErrors = questions.map((q) => (q.length > 0 && (q.length < 5 || q.length > 200) ? "5~200자로 입력해주세요" : undefined));
  const valid = !!topicValue && reason.trim().length >= 10 && reason.length <= 500 && !!mode && memo.length <= 100 && qErrors.every((e) => !e);

  const requestClose = () => (dirty ? setLeave(true) : onClose());

  const submit = () => {
    if (!valid) return;
    setSending(true);
    setTimeout(() => {
      createCoffeeChat({
        receiverId: target.id,
        memberIds: members,
        topic: topicValue!,
        message: reason.trim(),
        questions: questions.filter((q) => q.trim()),
        mode: mode!,
        candidates: [],
        fromRecruitId,
      });
      setSending(false);
      onClose();
      toast("신청을 보냈어요. 사자가 7일 안에 답해요.");
      router.push("/coffee?tab=sent");
    }, 500);
  };

  const group = members.length > 0;

  return (
    <>
      <Modal open={open} onClose={requestClose} title={`${target.name} 사자에게 커피챗 신청`} width={640}>
        <div className="flex flex-col gap-[22px]">
          {/* 작성 가이드 */}
          <div className="rounded-[8px] border border-light-layout bg-[#cce4ff]">
            <button type="button" onClick={() => setGuide((g) => !g)} className="flex h-10 w-full items-center justify-between px-3.5 text-[14px] font-medium text-ink-900">
              <span className="flex items-center gap-3">
                <Glyph name="edit" size={16} /> 작성 가이드
              </span>
              <Glyph name={guide ? "chevron-up" : "chevron-down"} size={18} />
            </button>
            {guide && (
              <ul className="list-disc space-y-1 px-9 pb-3 text-[13px] leading-5 text-ink-700">
                <li>사자가 도와줄 수 있는 주제 안에서 질문을 구체적으로 적어주세요.</li>
                <li>내 상황(기수·현재 상태·준비 중인 것)을 한두 줄로 먼저 소개하면 좋아요.</li>
                <li>가능한 시간대를 참고해 희망 일정을 남기면 수락이 빨라요.</li>
                <li>7일 안에 답이 없으면 자동 만료돼요.</li>
              </ul>
            )}
          </div>

          {/* 사자 정보 */}
          <div className="flex flex-col gap-5 rounded-[8px] border border-line-200 bg-bg-50 px-2.5 py-4">
            <div className="flex flex-col gap-2.5">
              <p className="t-label1 text-ink-700">사자가 도와줄 수 있는 주제</p>
              <div className="flex flex-wrap gap-1.5">
                {detail.coffeeTopics.map((t) => (
                  <OutlineTag key={t}>{t}</OutlineTag>
                ))}
              </div>
            </div>
            <div className="flex flex-col gap-2">
              <p className="t-label1 text-ink-700">가능한 시간대</p>
              <p className="text-[13px] font-medium text-ink-600">{detail.coffeeTimes}</p>
            </div>
          </div>

          {/* 함께 신청할 동문 (R-01b / R-02 / R-04) */}
          {target.allowGroup && (
            <div className="flex flex-col gap-3 rounded-[8px] border border-line-200 bg-white p-4">
              <div className="flex items-center justify-between">
                <div className="flex flex-col gap-0.5">
                  <p className="t-label1 text-ink-700">함께 신청할 동문 (선택)</p>
                  <p className="t-label2 text-ink-400">{fromRecruitId ? "모집글에서 모인 동문이 자동으로 들어가요" : "최대 4명 · 나 포함 5명까지"}</p>
                </div>
                {!fromRecruitId && (
                  <Button size="S" variant="secondary" onClick={() => setPicker(true)}>
                    + 함께 신청할 동문 추가
                  </Button>
                )}
              </div>
              {group && (
                <div className="flex flex-wrap gap-2">
                  {members.map((id) => {
                    const a = alumni.find((x) => x.id === id);
                    if (!a) return null;
                    return (
                      <span key={id} className="inline-flex items-center gap-1.5 rounded-full border border-line-300 py-1 pl-1 pr-2.5 text-[13px] font-semibold text-ink-900">
                        <Avatar name={a.name} part={a.part} size={32} className="!size-6 !text-[11px]" />
                        {a.name} · {a.generation}기
                        {!fromRecruitId && (
                          <button type="button" aria-label="제외" onClick={() => setMembers((m) => m.filter((x) => x !== id))} className="text-ink-400 hover:text-ink-900">
                            ✕
                          </button>
                        )}
                      </span>
                    );
                  })}
                </div>
              )}
              {group && <p className="t-caption1 text-ink-500">수락되면 함께 신청한 동문 모두가 단체 채팅방에 초대돼요. 대표 신청자는 나예요.</p>}
            </div>
          )}

          {/* 상담 주제 */}
          <Section label="상담 주제 *">
            <div className="flex flex-wrap gap-2">
              {TOPICS.map((t) => (
                <Chip key={t} selected={topic === t} onClick={() => setTopic(t)}>
                  {t}
                </Chip>
              ))}
            </div>
            {topic === "기타(직접 입력)" && <Input placeholder="주제를 직접 입력해주세요" value={topicEtc} onChange={(e) => setTopicEtc(e.target.value)} counter={{ value: topicEtc.length, max: 30 }} />}
          </Section>

          {/* 신청 이유 */}
          <Section label="이 사자에게 신청하는 이유 *">
            <div className="flex flex-wrap gap-2">
              {REASON_PRESETS.map((p) => (
                <Chip key={p} onClick={() => setReason((r) => (r ? r : p.replace(" …", " ")))}>
                  {p}
                </Chip>
              ))}
            </div>
            <Textarea placeholder="사자님의 ○○ 경험이 궁금해서 신청했어요. 저는 지금 ..." rows={4} value={reason} onChange={(e) => setReason(e.target.value)} counter={{ value: reason.length, max: 500 }} error={reason.length > 500 ? "500자 이내로 적어주세요" : reason.length > 0 && reason.trim().length < 10 ? "10자 이상 적어주세요" : undefined} />
          </Section>

          <div className="grid grid-cols-[266px_1fr] gap-4">
            <Section label="희망 방식 *">
              <div className="flex flex-wrap gap-2">
                <Chip selected={mode === "ONLINE"} onClick={() => setMode("ONLINE")}>
                  온라인
                </Chip>
                <Chip selected={mode === "OFFLINE"} onClick={() => setMode("OFFLINE")}>
                  오프라인
                </Chip>
                <Chip selected={mode === "ANY"} onClick={() => setMode("ANY")}>
                  상관없음
                </Chip>
              </div>
            </Section>
            <Section label="희망 일정 메모 (선택)">
              <Input placeholder="예) 평일 저녁 좋아요" value={memo} onChange={(e) => setMemo(e.target.value)} counter={{ value: memo.length, max: 100 }} />
            </Section>
          </div>

          {/* 질문 */}
          <Section
            label={
              <>
                미리 드리는 질문 <span className="text-ink-400">(선택)</span>
              </>
            }
          >
            {questions.map((q, i) => (
              <div key={i} className="flex items-start gap-2">
                <Input placeholder="5~200자로 입력해주세요" value={q} onChange={(e) => setQuestions((qs) => qs.map((x, j) => (j === i ? e.target.value : x)))} counter={{ value: q.length, max: 200 }} error={qErrors[i]} />
                {questions.length > 1 && (
                  <button type="button" aria-label="질문 삭제" onClick={() => setQuestions((qs) => qs.filter((_, j) => j !== i))} className="mt-2.5 text-ink-400 hover:text-ink-900">
                    ✕
                  </button>
                )}
              </div>
            ))}
            {questions.length < 3 && (
              <button type="button" onClick={() => setQuestions((qs) => [...qs, ""])} className="h-10 w-full rounded-[6px] border border-line-300 text-[12px] font-semibold text-ink-500 hover:bg-bg-50">
                + 질문 추가
              </button>
            )}
          </Section>

          {/* 미리보기 (R-05) */}
          {preview && (
            <div className="flex flex-col gap-3 rounded-[8px] border border-primary/30 bg-part-be-bg/40 p-4 text-[14px] leading-6 text-ink-900">
              <p className="t-label2 text-primary">사자에게 이렇게 보여요</p>
              <div className="flex items-center gap-2">
                <Avatar name="박중앙" part="FRONTEND" size={32} />
                <span className="font-semibold">박중앙 · 13기 · 프론트엔드</span>
                {group && <span className="text-[12px] text-ink-500">외 {members.length}명</span>}
              </div>
              <p>
                <b>주제</b> {topicValue || "—"} · <b>방식</b> {mode === "ONLINE" ? "온라인" : mode === "OFFLINE" ? "오프라인" : mode === "ANY" ? "상관없음" : "—"}
              </p>
              <p className="whitespace-pre-line">{reason || "(신청 이유)"}</p>
              {memo && <p className="text-ink-500">희망 일정: {memo}</p>}
              {questions.filter(Boolean).length > 0 && (
                <ol className="list-decimal pl-5">
                  {questions.filter(Boolean).map((q, i) => (
                    <li key={i}>{q}</li>
                  ))}
                </ol>
              )}
            </div>
          )}

          <div className="flex items-center justify-between pt-1">
            <button type="button" onClick={() => setPreview((p) => !p)} className={cn("h-10 rounded-[8px] border border-light-layout px-3.5 text-[14px] font-medium text-ink-900", preview ? "bg-light-text text-white" : "bg-light-text/60")}>
              {preview ? "미리 보기 닫기" : "신청서 미리 보기"}
            </button>
            <div className="flex gap-2.5">
              <Button variant="secondary" onClick={requestClose}>
                취소
              </Button>
              <Button disabled={!valid} loading={sending} onClick={submit} className="text-[16px]">
                신청 보내기
              </Button>
            </div>
          </div>
        </div>
      </Modal>

      <AlumniPicker open={picker} onClose={() => setPicker(false)} onConfirm={setMembers} initial={members} max={4} title="함께 신청할 동문 추가" description="최대 4명 · 나 포함 5명까지" exclude={[target.id, ME_ID]} />

      {/* ◇[O] C-08 이탈 확인창 / CF-1 */}
      <ConfirmModal open={leave} onClose={() => setLeave(false)} onConfirm={() => { setLeave(false); onClose(); }} title="작성 중인 내용이 사라져요" description="지금 나가면 적은 내용은 저장되지 않아요." confirmLabel="나가기" cancelLabel="계속 작성" danger />
    </>
  );
}

function Section({ label, children }: { label: React.ReactNode; children: React.ReactNode }) {
  return (
    <div className="flex flex-col gap-3 rounded-[8px] border border-light-layout p-4">
      <p className="t-label1 text-ink-700">{label}</p>
      {children}
    </div>
  );
}
