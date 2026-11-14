"use client";

import { useRef, useState } from "react";
import { errorMessage } from "@/lib/api/client";
import { checkUpload, UPLOAD_RULES, type PostFile, type PostFileInput, type UploadType } from "@/lib/api/posts";
import { usePostService } from "@/lib/api/usePostService";
import { Icon } from "@/components/ui/Icon";
import { useToast } from "@/components/ui/Toast";
import { isImageUrl } from "./PostRow";

interface DraftFile {
  key: string;
  type: UploadType;
  name: string;
  size: number;
  /** 올리기가 끝나면 서버(S3) 주소 */
  url?: string;
  /** 이미지 미리보기 (올리는 동안) */
  preview?: string;
  uploading: boolean;
}

const LIMIT_TEXT: Record<UploadType, string> = { IMAGE: "사진은 5장까지 올릴 수 있어요", ATTACHMENT: "첨부파일은 3개까지 올릴 수 있어요" };
const formatBytes = (n: number) => (n >= 1024 ** 2 ? `${(n / 1024 ** 2).toFixed(1)}MB` : `${Math.max(1, Math.round(n / 1024))}KB`);

/**
 * 글쓰기 사진·첨부파일 (FILE-01). 고르면 바로 S3에 올리고, 글을 저장할 때 files로 보낸다.
 * 이미지 jpg·png·webp 10MB, 첨부 pdf·hwp·docx·pptx·xlsx·zip 20MB. 이미지 5장·첨부 3개까지.
 */
export function usePostAttachments(initial: PostFile[] = []) {
  const service = usePostService();
  const toast = useToast();
  const imageInput = useRef<HTMLInputElement>(null);
  const fileInput = useRef<HTMLInputElement>(null);
  const [items, setItems] = useState<DraftFile[]>(() => initial.map((f) => ({ key: String(f.id), type: f.type, name: f.name, size: f.size, url: f.url, uploading: false })));

  const add = (type: UploadType, list: FileList | null) => {
    if (!list?.length) return;
    const room = UPLOAD_RULES[type].maxCount - items.filter((i) => i.type === type).length;
    const picked = Array.from(list);
    if (picked.length > room) toast(LIMIT_TEXT[type], { kind: "error" });
    picked.slice(0, Math.max(0, room)).forEach((file) => {
      const problem = checkUpload(type, file);
      if (problem) {
        toast(`${file.name}: ${problem}`, { kind: "error" });
        return;
      }
      const key = `new-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`;
      const preview = type === "IMAGE" ? URL.createObjectURL(file) : undefined;
      setItems((s) => [...s, { key, type, name: file.name, size: file.size, preview, uploading: true }]);
      service.uploadFile(type, file).then(
        (res) => setItems((s) => s.map((x) => (x.key === key ? { ...x, url: res.url, uploading: false } : x))),
        (e) => {
          setItems((s) => s.filter((x) => x.key !== key));
          toast(`${file.name}: ${errorMessage(e)}`, { kind: "error" });
        },
      );
    });
  };

  const remove = (key: string) => setItems((s) => s.filter((x) => x.key !== key));
  const images = items.filter((i) => i.type === "IMAGE");
  const attachments = items.filter((i) => i.type === "ATTACHMENT");
  const uploading = items.some((i) => i.uploading);
  /** 글에 넣을 파일 (올리기가 끝난 것만, 이미지 먼저) */
  const files: PostFileInput[] = [...images, ...attachments].filter((i) => i.url).map((i) => ({ type: i.type, url: i.url!, name: i.name, size: i.size }));

  const inputs = (
    <>
      <input ref={imageInput} type="file" accept={UPLOAD_RULES.IMAGE.accept} multiple hidden aria-label="사진 고르기" onChange={(e) => { add("IMAGE", e.target.files); e.target.value = ""; }} />
      <input ref={fileInput} type="file" accept={UPLOAD_RULES.ATTACHMENT.accept} multiple hidden aria-label="첨부파일 고르기" onChange={(e) => { add("ATTACHMENT", e.target.files); e.target.value = ""; }} />
    </>
  );

  const pickImage = () => (images.length >= UPLOAD_RULES.IMAGE.maxCount ? toast(LIMIT_TEXT.IMAGE, { kind: "error" }) : imageInput.current?.click());
  const pickFile = () => (attachments.length >= UPLOAD_RULES.ATTACHMENT.maxCount ? toast(LIMIT_TEXT.ATTACHMENT, { kind: "error" }) : fileInput.current?.click());

  const view = (
    <div className="flex flex-col gap-3">
      <div className="flex flex-wrap gap-3">
        {images.map((i) => {
          const src = i.preview ?? i.url;
          return (
            <div key={i.key} className="relative size-[64px] overflow-hidden rounded-[8px] border border-line-200 bg-bg-100">
              {isImageUrl(src) ? (
                // eslint-disable-next-line @next/next/no-img-element
                <img src={src} alt={i.name} className="size-full object-cover" />
              ) : (
                <span className="grid size-full place-items-center text-ink-400">
                  <Icon name="image" size={20} />
                </span>
              )}
              {i.uploading && <span className="absolute inset-0 grid place-items-center bg-white/70 text-[11px] font-semibold text-ink-700">올리는 중</span>}
              <button type="button" aria-label={`${i.name} 빼기`} onClick={() => remove(i.key)} className="absolute right-0.5 top-0.5 grid size-5 place-items-center rounded-full bg-ink-900/70 text-white">
                <Icon name="close" size={12} />
              </button>
            </div>
          );
        })}
        {images.length < UPLOAD_RULES.IMAGE.maxCount && (
          <button type="button" onClick={pickImage} aria-label="사진 추가" className="grid size-[64px] place-items-center rounded-[8px] border border-line-200 bg-white text-ink-500 hover:bg-bg-50">
            <Icon name="plus" size={22} />
          </button>
        )}
      </div>
      {attachments.map((i) => (
        <div key={i.key} className="flex items-center justify-between rounded-[8px] border border-line-200 px-4 py-3 text-[14px]">
          <span className="flex min-w-0 items-center gap-2 text-ink-900">
            <Icon name="clip" size={18} className="shrink-0 text-ink-700" />
            <span className="truncate">{i.name}</span>
          </span>
          <span className="flex shrink-0 items-center gap-3 text-[12px] text-ink-500">
            {i.uploading ? "올리는 중…" : formatBytes(i.size)}
            <button type="button" aria-label={`${i.name} 빼기`} onClick={() => remove(i.key)} className="rounded p-1 text-ink-700 hover:bg-bg-100">
              <Icon name="close" size={16} />
            </button>
          </span>
        </div>
      ))}
    </div>
  );

  return { inputs, view, pickImage, pickFile, files, uploading, count: items.length };
}
