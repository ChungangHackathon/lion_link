"use client";

import { useState } from "react";
import { cn } from "@/lib/cn";
import type { Alumni } from "@/lib/types";
import type { ProfileDetail } from "@/lib/mock/profile";
import { PART_STYLE, Tag } from "@/components/ui/Badge";
import { Glyph, Icon } from "@/components/ui/Icon";
import { Card } from "@/components/ui/Misc";

/**
 * Profile body blocks shared by P-01 (다른 동문) and P-02 (내 프로필).
 * Section order is unified (v2.5 §4-1): 소개 → 외부 링크·연락처 → 경력 → 멋사 활동 → 프로젝트 → 기술·키워드 → 자격증·수상·대외활동·언어.
 * `onEdit(section)` turns on the 바로 고치기 pencil (P-02).
 */
export function ProfileSections({ detail, connected, onEdit }: { detail: ProfileDetail; person?: Alumni; connected?: boolean; onEdit?: (section: string) => void }) {
  const [aboutOpen, setAboutOpen] = useState(false);
  const [careersOpen, setCareersOpen] = useState(false);
  const [lionOpen, setLionOpen] = useState(false);
  const [skillsOpen, setSkillsOpen] = useState(false);
  const mine = !!onEdit;
  const canSee = (v: "ALL" | "CONNECTED" | "ME") => mine || v === "ALL" || (v === "CONNECTED" && connected);

  return (
    <>
      <Section title="소개" onEdit={onEdit && (() => onEdit("about"))}>
        {detail.about ? (
          <>
            <p className={cn("text-[14px] leading-6 text-ink-700", !aboutOpen && "line-clamp-3")}>{detail.about}</p>
            {detail.about.length > 150 && (
              <button type="button" onClick={() => setAboutOpen((o) => !o)} className="mt-1 text-[12px] text-ink-500 underline underline-offset-2">
                {aboutOpen ? "접기" : "...더 보기"}
              </button>
            )}
          </>
        ) : (
          <Empty text={mine ? "나를 소개하는 글을 써보세요. 후배들이 신청할 때 가장 먼저 읽어요." : "아직 소개가 없어요"} />
        )}
      </Section>

      <Section title="외부 링크·연락처" onEdit={onEdit && (() => onEdit("contact"))}>
        <ul className="flex flex-col gap-3 text-[13px]">
          {detail.links.map((l) => (
            <li key={l.label} className="flex items-center justify-between">
              <span className="flex items-center gap-2 text-ink-700">
                <Glyph name="link" size={14} /> {l.label}
              </span>
              {canSee(l.visibility) ? (
                <a href={`https://${l.url}`} target="_blank" rel="noreferrer" className="text-ink-900 hover:underline">
                  {l.url}
                </a>
              ) : (
                <Locked />
              )}
            </li>
          ))}
          <li className="flex items-center justify-between">
            <span className="flex items-center gap-2 text-ink-700">
              <Glyph name="mail" size={14} /> 이메일
            </span>
            {detail.email && canSee(detail.email.visibility) ? <span className="text-ink-900">{detail.email.value}</span> : <Locked />}
          </li>
          <li className="flex items-center justify-between">
            <span className="flex items-center gap-2 text-ink-700">
              <Glyph name="comment" size={14} /> 전화번호
            </span>
            {detail.phone && canSee(detail.phone.visibility) ? <span className="text-ink-900">{detail.phone.value}</span> : <Locked />}
          </li>
        </ul>
      </Section>

      <Section title="경력" onEdit={onEdit && (() => onEdit("career"))}>
        {detail.careers.length === 0 ? (
          <Empty text={mine ? "첫 경력을 추가해보세요" : "등록된 경력이 없어요"} />
        ) : (
          <>
            <ul className="flex flex-col gap-5">
              {(careersOpen ? detail.careers : detail.careers.slice(0, 3)).map((c, i) => (
                <li key={i} className="flex items-start gap-3">
                  <span className="grid size-10 shrink-0 place-items-center rounded-[6px] bg-bg-100 text-ink-400">
                    <Glyph name="image" size={16} />
                  </span>
                  <div className="flex flex-col gap-0.5 text-[12px] text-ink-500">
                    <span className="text-[14px] font-semibold text-ink-900">{c.company}</span>
                    <span>
                      {c.role}
                      {c.type && ` · ${c.type}`}
                    </span>
                    <span>
                      {c.from} ~ {c.to} · {c.duration}
                    </span>
                  </div>
                </li>
              ))}
            </ul>
            {detail.careers.length > 3 && <More open={careersOpen} onClick={() => setCareersOpen((o) => !o)} />}
          </>
        )}
      </Section>

      <Section title="멋사 활동" onEdit={onEdit && (() => onEdit("lion"))}>
        <ol className="relative ml-3 flex flex-col gap-6 border-l-2 border-part-be-line pl-7">
          {(lionOpen ? detail.lion : detail.lion.slice(0, 2)).map((l, i) => (
            <li key={i} className="relative">
              <span className={cn("absolute -left-[46px] top-0 grid size-8 place-items-center rounded-full text-[11px] font-bold", PART_STYLE[l.part].bg, PART_STYLE[l.part].text)}>{l.generation}기</span>
              <p className="pt-1.5 text-[14px] font-semibold text-ink-900">{l.role}</p>
              {l.projects.length > 0 && (
                <div className="mt-4 grid grid-cols-2 gap-5">
                  {l.projects.map((p, j) => (
                    <div key={j} className="flex flex-col gap-2 rounded-[8px] border border-line-200 p-3">
                      <div className="grid h-[100px] place-items-center rounded-[6px] bg-bg-100 text-ink-400">
                        <Glyph name="image" size={20} />
                      </div>
                      <span className="w-fit rounded-[4px] bg-part-be-bg px-2 py-0.5 text-[11px] font-semibold text-primary">{p.event}</span>
                      <p className="text-[13px] font-bold text-ink-900">{p.name}</p>
                      <p className="text-[12px] text-ink-500">{p.desc}</p>
                      <p className="text-[12px] text-ink-700">{p.role}</p>
                      {p.link && (
                        <a href={`https://${p.link}`} target="_blank" rel="noreferrer" className="flex items-center gap-1 text-[11px] text-primary hover:underline">
                          <Glyph name="link" size={12} /> {p.link}
                        </a>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </li>
          ))}
        </ol>
        {detail.lion.length > 2 && <More open={lionOpen} onClick={() => setLionOpen((o) => !o)} />}
      </Section>

      <Section title="프로젝트" onEdit={onEdit && (() => onEdit("project"))}>
        {detail.projects.length === 0 ? (
          <Empty text={mine ? "사이드 프로젝트를 추가해보세요" : "등록된 프로젝트가 없어요"} />
        ) : (
          <ul className="flex flex-col gap-4">
            {detail.projects.map((p, i) => (
              <li key={i} className="flex items-start gap-3">
                <span className="grid size-10 shrink-0 place-items-center rounded-full bg-part-be-bg text-primary">
                  <Glyph name="link" size={16} />
                </span>
                <div className="flex flex-1 flex-col gap-1">
                  <div className="flex items-center justify-between">
                    <span className="text-[14px] font-semibold text-ink-900">{p.name}</span>
                    <span className="text-[12px] text-ink-500">
                      {p.from} ~ {p.to}
                    </span>
                  </div>
                  <span className="text-[12px] text-ink-500">{p.type}</span>
                  <div className="flex gap-1.5">
                    {p.skills.map((s) => (
                      <Tag key={s}>{s}</Tag>
                    ))}
                  </div>
                </div>
              </li>
            ))}
          </ul>
        )}
      </Section>

      <Section title="기술 · 키워드" onEdit={onEdit && (() => onEdit("skills"))}>
        <div className="flex flex-wrap gap-2">
          {detail.repSkills.map((s) => (
            <Tag key={s} className="font-semibold">
              <Icon name="star-filled" size={11} className="mr-1" /> {s}
            </Tag>
          ))}
        </div>
        <div className="mt-3 flex flex-wrap gap-2">
          {(skillsOpen ? detail.skills : detail.skills.slice(0, 6)).map((s) => (
            <Tag key={s}>{s}</Tag>
          ))}
          {detail.skills.length > 6 && (
            <button type="button" onClick={() => setSkillsOpen((o) => !o)} className="text-[12px] text-ink-500 underline underline-offset-2">
              {skillsOpen ? "접기" : `+${detail.skills.length - 6}개 더 보기`}
            </button>
          )}
        </div>
      </Section>

      <Section title="자격증·수상·대외활동·언어" onEdit={onEdit && (() => onEdit("certs"))}>
        {detail.certs.length === 0 ? (
          <Empty text={mine ? "자격증·수상 이력을 추가해보세요" : "등록된 항목이 없어요"} />
        ) : (
          <dl className="flex flex-col gap-2 text-[12px]">
            {detail.certs.map((c, i) => (
              <div key={i} className="flex justify-between">
                <dt className="text-ink-500">{c.kind}</dt>
                <dd className="text-ink-900">{c.text}</dd>
              </div>
            ))}
          </dl>
        )}
      </Section>
    </>
  );
}

function Section({ title, children, onEdit }: { title: string; children: React.ReactNode; onEdit?: () => void }) {
  return (
    <Card as="section" className="flex flex-col gap-4 p-6">
      <div className="flex items-center justify-between">
        <h2 className="text-[18px] font-bold text-ink-900">{title}</h2>
        {onEdit && (
          <button type="button" aria-label={`${title} 편집`} onClick={onEdit} className="rounded p-1 text-ink-500 hover:bg-bg-100 hover:text-ink-900">
            <Glyph name="edit" size={18} />
          </button>
        )}
      </div>
      <div>{children}</div>
    </Card>
  );
}

function Locked() {
  return (
    <span className="flex items-center gap-1 text-[12px] text-ink-400">
      <Icon name="lock" size={12} /> 연결된 동문에게만 보여요
    </span>
  );
}

function Empty({ text }: { text: string }) {
  return <p className="rounded-[8px] bg-bg-50 px-4 py-5 text-center text-[13px] text-ink-500">{text}</p>;
}

function More({ open, onClick }: { open: boolean; onClick: () => void }) {
  return (
    <button type="button" onClick={onClick} className="mt-4 flex w-full flex-col items-center text-[13px] text-ink-700 hover:underline">
      {open ? "접기" : "전체보기"}
      <Glyph name={open ? "chevron-up" : "chevron-down"} size={14} />
    </button>
  );
}
