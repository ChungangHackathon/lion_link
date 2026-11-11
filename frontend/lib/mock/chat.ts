import type { ChatRoom } from "@/lib/types";
import { ME_ID } from "./users";

const at = (minAgo: number) => new Date(Date.now() - minAgo * 60_000).toISOString();

export const chatRooms: ChatRoom[] = [
  {
    id: "r-1",
    type: "DIRECT",
    status: "ACTIVE",
    memberIds: [ME_ID, "u-1"],
    unread: 2,
    messages: [
      { id: "m1", senderId: "system", text: "커피챗이 수락되어 채팅방이 열렸어요. 일정을 정해보세요.", at: at(60 * 24 * 2) },
      { id: "m2", senderId: "u-1", text: "안녕하세요 중앙님! 신청서 잘 봤어요. 이직 준비 얘기 편하게 해요.", at: at(60 * 24) },
      { id: "m3", senderId: ME_ID, text: "감사합니다! 목요일 저녁 괜찮으실까요?", at: at(60 * 5) },
      { id: "m4", senderId: "u-1", text: "네 좋아요. 8시에 온라인으로 뵐게요.", at: at(30) },
      { id: "m5", senderId: "u-1", text: "링크는 당일에 보낼게요 🙂", at: at(29) },
    ],
  },
  {
    id: "r-2",
    type: "DIRECT",
    status: "ACTIVE",
    memberIds: [ME_ID, "u-2"],
    coffeeChatId: "c-3",
    unread: 0,
    messages: [
      { id: "m1", senderId: "system", text: "일정이 확정됐어요. 내일 18:00 · 오프라인", at: at(60 * 24 * 3) },
      { id: "m2", senderId: "u-2", text: "내일 흑석역 근처 카페에서 뵐게요!", at: at(60 * 20) },
    ],
  },
  {
    id: "r-3",
    type: "GROUP",
    status: "ACTIVE",
    title: "초기 팀 빌딩 커피챗",
    memberIds: [ME_ID, "u-4", "u-5", "u-10"],
    coffeeChatId: "c-7",
    unread: 1,
    messages: [
      { id: "m1", senderId: "system", text: "시사효석님이 커피챗을 수락했어요. 함께 신청한 동문이 초대됐어요.", at: at(60 * 24 * 3) },
      { id: "m2", senderId: "u-4", text: "반가워요 여러분! 질문 미리 남겨주시면 준비해올게요.", at: at(60 * 24 * 2) },
      { id: "m3", senderId: "u-5", text: "공동창업자 찾는 과정이 제일 궁금해요!", at: at(60 * 24) },
      { id: "m4", senderId: "u-10", text: "저도요! 초기 지분 얘기도 가능할까요?", at: at(60 * 3) },
    ],
  },
  {
    id: "r-4",
    type: "DIRECT",
    status: "ACTIVE",
    memberIds: [ME_ID, "u-3"],
    coffeeChatId: "c-4",
    unread: 0,
    messages: [
      { id: "m1", senderId: "system", text: "커피챗이 완료됐어요. 감사 인사를 남겨보세요.", at: at(60 * 24 * 20) },
      { id: "m2", senderId: ME_ID, text: "오늘 정말 감사했습니다! LCP 얘기 큰 도움 됐어요.", at: at(60 * 24 * 19) },
    ],
  },
  {
    id: "r-5",
    type: "DIRECT",
    status: "ACTIVE",
    memberIds: [ME_ID, "u-9"],
    coffeeChatId: "c-8",
    unread: 0,
    messages: [{ id: "m1", senderId: "u-9", text: "다음에 또 궁금한 거 있으면 편하게 연락해요!", at: at(60 * 24 * 15) }],
  },
  // 메시지 요청 (받은)
  {
    id: "r-6",
    type: "DIRECT",
    status: "REQUESTED",
    isRequest: true,
    memberIds: ["u-11", ME_ID],
    unread: 1,
    messages: [
      { id: "m1", senderId: "u-11", text: "안녕하세요 중앙님, 브랜딩 프로젝트에 프론트 분이 필요해서 연락드려요. 잠깐 이야기 나눌 수 있을까요?", at: at(60 * 4) },
    ],
  },
  {
    id: "r-7",
    type: "DIRECT",
    status: "REQUESTED",
    isRequest: true,
    memberIds: ["u-6", ME_ID],
    unread: 1,
    messages: [{ id: "m1", senderId: "u-6", text: "커머스 프론트 인턴 지원 관련해서 짧게 여쭤봐도 될까요?", at: at(60 * 30) }],
  },
];
