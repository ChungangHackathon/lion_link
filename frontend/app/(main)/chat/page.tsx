import { Icon } from "@/components/ui/Icon";

/** ★ T-01 채팅 / 방 선택 전 */
export default function ChatIndexPage() {
  return (
    <div className="flex flex-1 flex-col items-center justify-center gap-3 text-center">
      <span className="grid size-16 place-items-center rounded-full bg-bg-100">
        <Icon name="chat" size={28} />
      </span>
      <p className="t-body-bold text-ink-700">채팅방을 선택해주세요</p>
      <p className="t-caption1 text-ink-500">커피챗이 수락되면 채팅방이 자동으로 열려요.</p>
    </div>
  );
}
