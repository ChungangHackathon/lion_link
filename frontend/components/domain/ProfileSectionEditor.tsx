"use client";

import { useState } from "react";
import { cn } from "@/lib/cn";
import { LION_PART_LABEL } from "@/lib/types";
import { errorMessage } from "@/lib/api/client";
import type { MyProfile, Visibility } from "@/lib/api/myProfile";
import type { MyProfileService } from "@/lib/api/useMyProfileService";
import { emptyValues, LION_PARTS, MAX_GENERATION, SECTION_SCHEMA, thisMonth, toBody, validate, valuesFromItem, type Field, type FormValues, type SectionItem, type SectionKey } from "@/lib/api/profileSections";
import { Button, Chip } from "@/components/ui/Button";
import { Checkbox, Input, Select, Textarea } from "@/components/ui/Input";
import { ConfirmModal, Modal } from "@/components/ui/Modal";
import { useToast } from "@/components/ui/Toast";

type Form = { section: SectionKey; item?: SectionItem };

/**
 * ◇[O] 목록형 섹션 편집 팝업 (PROFILE-11). 섹션 하나 또는 여러 개(멋사 활동+멋사 프로젝트, 자격증·수상·대외활동·언어)를
 * 한 팝업에서 목록으로 보여주고, 항목 추가·수정(E-04 폼)·삭제를 한다. 저장할 때마다 프로필을 다시 불러와 서버 정렬을 따른다.
 */
export function SectionListModal({
  title,
  sections,
  profile,
  service,
  startAdding,
  onClose,
  onChanged,
}: {
  title: string;
  sections: SectionKey[];
  profile: MyProfile;
  service: MyProfileService;
  /** 비어 있는 섹션을 '추가'로 열면 바로 폼부터 */
  startAdding?: SectionKey;
  onClose: () => void;
  onChanged: (patch: Partial<MyProfile>) => void;
}) {
  const toast = useToast();
  const [form, setForm] = useState<Form | null>(startAdding ? { section: startAdding } : null);
  const [removing, setRemoving] = useState<Form | null>(null);
  const [busy, setBusy] = useState(false);

  const refresh = async () => onChanged(await service.get());

  const submit = async (section: SectionKey, values: FormValues, item?: SectionItem) => {
    setBusy(true);
    try {
      const body = toBody(section, values);
      if (item) await service.updateItem(section, item.id, body);
      else await service.addItem(section, body);
      await refresh();
      toast(item ? "수정했어요" : "추가했어요");
      // 처음부터 폼으로 열었으면 저장하고 닫는다
      if (startAdding && !item) onClose();
      else setForm(null);
    } catch (e) {
      toast(errorMessage(e), { kind: "error" });
    } finally {
      setBusy(false);
    }
  };

  const remove = async () => {
    if (!removing?.item) return;
    setBusy(true);
    try {
      await service.deleteItem(removing.section, removing.item.id);
      await refresh();
      toast("삭제했어요");
    } catch (e) {
      toast(errorMessage(e), { kind: "error" });
    } finally {
      setBusy(false);
      setRemoving(null);
    }
  };

  const setCompany = async (company: Visibility["company"]) => {
    try {
      const { visibility } = await service.updateVisibility({ company });
      onChanged({ visibility });
      toast("회사명 공개 범위를 바꿨어요");
    } catch (e) {
      toast(errorMessage(e), { kind: "error" });
    }
  };

  if (form)
    return (
      <SectionFormModal
        section={form.section}
        item={form.item}
        generation={profile.generation}
        busy={busy}
        onClose={() => (startAdding && !form.item ? onClose() : setForm(null))}
        onSave={(values) => submit(form.section, values, form.item)}
      />
    );

  return (
    <>
      <Modal
        open={!removing}
        onClose={onClose}
        title={title}
        width={600}
        footer={
          <Button variant="secondary" onClick={onClose}>
            닫기
          </Button>
        }
      >
        <div className="flex flex-col gap-6">
          {sections.includes("careers") && (
            <Select label="회사명 공개" value={profile.visibility.company} onChange={(e) => void setCompany(e.target.value as Visibility["company"])}>
              <option value="PUBLIC">전체 공개</option>
              <option value="PRIVATE">비공개 (다른 동문에게 &apos;비공개 회사&apos;로 보여요)</option>
            </Select>
          )}
          {sections.map((s) => {
            const schema = SECTION_SCHEMA[s];
            const items = profile[s] as SectionItem[];
            const full = items.length >= schema.max;
            return (
              <section key={s} className="flex flex-col gap-2" aria-label={schema.label}>
                {sections.length > 1 && (
                  <h3 className="t-body-bold text-ink-900">
                    {schema.label}{" "}
                    <span className="text-[12px] font-normal text-ink-400">
                      {items.length}/{schema.max}
                    </span>
                  </h3>
                )}
                <ul className="flex flex-col gap-2">
                  {items.length === 0 && <li className="rounded-[8px] bg-bg-50 px-4 py-4 text-center text-[13px] text-ink-500">아직 항목이 없어요</li>}
                  {items.map((it) => {
                    const { title: t, sub } = schema.summary(it);
                    const locked = "isDefault" in it && !!it.isDefault;
                    return (
                      <li key={String(it.id)} className="flex items-center justify-between gap-3 rounded-[8px] border border-line-200 px-4 py-3">
                        <span className="flex min-w-0 flex-col">
                          <span className="t-label1 text-ink-900">{t}</span>
                          {sub && <span className="truncate text-[12px] text-ink-500">{sub}</span>}
                        </span>
                        <span className="flex shrink-0 gap-1.5">
                          <Button size="S" variant="secondary" aria-label={`${t} 수정`} onClick={() => setForm({ section: s, item: it })}>
                            수정
                          </Button>
                          {!locked && (
                            <Button size="S" variant="text" aria-label={`${t} 삭제`} onClick={() => setRemoving({ section: s, item: it })}>
                              삭제
                            </Button>
                          )}
                        </span>
                      </li>
                    );
                  })}
                </ul>
                <Button variant="secondary" size="S" disabled={full} onClick={() => setForm({ section: s })}>
                  {full ? `${schema.label}은(는) ${schema.max}개까지예요` : `+ ${schema.label} 추가`}
                </Button>
              </section>
            );
          })}
        </div>
      </Modal>
      <ConfirmModal
        open={!!removing}
        onClose={() => setRemoving(null)}
        onConfirm={() => void remove()}
        loading={busy}
        danger
        title="이 항목을 삭제할까요?"
        description={removing?.item ? SECTION_SCHEMA[removing.section].summary(removing.item).title : undefined}
        confirmLabel="삭제"
      />
    </>
  );
}

/** ★[O] E-04 항목 추가·수정 폼 (Modal/Form 마스터). 섹션 정의를 보고 칸을 그린다 */
function SectionFormModal({ section, item, generation, busy, onClose, onSave }: { section: SectionKey; item?: SectionItem; generation: number; busy: boolean; onClose: () => void; onSave: (v: FormValues) => void }) {
  const schema = SECTION_SCHEMA[section];
  const [values, setValues] = useState<FormValues>(() => (item ? valuesFromItem(section, item) : emptyValues(section, { generation: section === "lionProjects" ? generation : undefined })));
  const [touched, setTouched] = useState(false);
  const errors = validate(section, values);
  const isDefault = !!item && "isDefault" in item && !!item.isDefault;
  const set = (key: string, v: FormValues[string]) => setValues((s) => ({ ...s, [key]: v }));
  const shown = touched ? errors : {};

  return (
    <Modal
      open
      onClose={onClose}
      title={`${schema.label} ${item ? "수정" : "추가"}`}
      width={540}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            취소
          </Button>
          <Button
            loading={busy}
            onClick={() => {
              setTouched(true);
              if (Object.keys(errors).length === 0) onSave(values);
            }}
          >
            저장
          </Button>
        </>
      }
    >
      <div className="flex flex-col gap-4">
        {isDefault && <p className="rounded-[8px] bg-bg-50 px-4 py-2.5 text-[12px] text-ink-500">가입 기수 활동이에요. 역할과 파트만 바꿀 수 있어요.</p>}
        {schema.fields.map((f) => (
          <FieldInput key={f.key} field={f} values={values} error={shown[f.key]} set={set} lockGeneration={isDefault} />
        ))}
      </div>
    </Modal>
  );
}

const YEARS = (() => {
  const now = Number(thisMonth().slice(0, 4));
  return Array.from({ length: now - 1989 }, (_, i) => String(now - i));
})();
const MONTHS = Array.from({ length: 12 }, (_, i) => String(i + 1).padStart(2, "0"));

function FieldInput({ field: f, values, error, set, lockGeneration }: { field: Field; values: FormValues; error?: string; set: (k: string, v: FormValues[string]) => void; lockGeneration: boolean }) {
  const raw = values[f.key];
  const str = typeof raw === "string" ? raw : "";
  const required = "required" in f && !!f.required;
  switch (f.kind) {
    case "text":
    case "url":
      return <Input label={f.label} required={required} value={str} placeholder={f.kind === "url" ? f.placeholder ?? "https://" : f.placeholder} onChange={(e) => set(f.key, e.target.value)} counter={f.max ? { value: str.length, max: f.max } : undefined} error={error} />;
    case "textarea":
      return <Textarea label={`${f.label} (선택)`} rows={3} value={str} onChange={(e) => set(f.key, e.target.value)} counter={f.max ? { value: str.length, max: f.max } : undefined} error={error} />;
    case "select":
      return (
        <Select label={f.label} required={required} value={str} onChange={(e) => set(f.key, e.target.value)} error={error}>
          {!required && <option value="">선택 안 함</option>}
          {f.options.map(([v, l]) => (
            <option key={v} value={v}>
              {l}
            </option>
          ))}
        </Select>
      );
    case "generation":
      return (
        <Select label={f.label} required={required} value={str} disabled={lockGeneration} onChange={(e) => set(f.key, e.target.value)} error={error}>
          <option value="">기수 선택</option>
          {Array.from({ length: MAX_GENERATION }, (_, i) => MAX_GENERATION - i).map((g) => (
            <option key={g} value={String(g)}>
              {g}기
            </option>
          ))}
        </Select>
      );
    case "ym": {
      const disabled = f.key === "endYm" && values.isCurrent === true;
      const [y, m] = str ? str.split("-") : ["", ""];
      return (
        <div className="flex flex-col gap-1.5">
          <p className="t-body-bold text-ink-700">
            {f.label}
            {required && " *"}
          </p>
          <div className="grid grid-cols-2 gap-2">
            <Select aria-label={`${f.label} 연도`} value={disabled ? "" : y} disabled={disabled} onChange={(e) => set(f.key, e.target.value ? `${e.target.value}-${m || "01"}` : "")}>
              <option value="">연도</option>
              {YEARS.map((yy) => (
                <option key={yy} value={yy}>
                  {yy}년
                </option>
              ))}
            </Select>
            <Select aria-label={`${f.label} 월`} value={disabled ? "" : m} disabled={disabled || !y} onChange={(e) => set(f.key, `${y}-${e.target.value}`)}>
              <option value="" disabled>
                월
              </option>
              {MONTHS.map((mm) => (
                <option key={mm} value={mm}>
                  {Number(mm)}월
                </option>
              ))}
            </Select>
          </div>
          {error && <p className="t-caption2 text-orange">{error}</p>}
        </div>
      );
    }
    case "current":
      return <Checkbox label={f.label} checked={raw === true} onChange={(e) => set(f.key, e.target.checked)} />;
    case "parts": {
      const parts = (raw as string[]) ?? [];
      return (
        <div className="flex flex-col gap-2">
          <p className="t-body-bold text-ink-700">{f.label} *</p>
          <div className="flex flex-wrap gap-2">
            {LION_PARTS.map((p) => (
              <Chip key={p} selected={parts.includes(p)} onClick={() => set(f.key, parts.includes(p) ? parts.filter((x) => x !== p) : parts.length >= 2 ? parts : [...parts, p])}>
                {LION_PART_LABEL[p]}
              </Chip>
            ))}
          </div>
          {error && <p className={cn("t-caption2 text-orange")}>{error}</p>}
        </div>
      );
    }
    case "tags":
      return <Input label={`${f.label} (선택)`} value={str} placeholder="쉼표로 구분 · 예) React, Spring" onChange={(e) => set(f.key, e.target.value)} help={`최대 ${f.max}개`} error={error} />;
  }
}
