// Frontend domain types. Enum values mirror backend/src/main/java/com/lionlink/backend/domain.

export type UserRole = "MEMBER" | "ADMIN";
export type UserStatus = "PENDING" | "APPROVED" | "REJECTED";

export type LionPart = "PLANNING" | "DESIGN" | "FRONTEND" | "BACKEND" | "ETC";
export const LION_PART_LABEL: Record<LionPart, string> = {
  PLANNING: "기획",
  DESIGN: "디자인",
  FRONTEND: "프론트엔드",
  BACKEND: "백엔드",
  ETC: "기타",
};

export type ProfileStatus =
  | "STUDENT"
  | "LEAVE"
  | "GRADUATED"
  | "EMPLOYED"
  | "FOUNDER"
  | "GRAD_SCHOOL"
  | "ETC";
export const PROFILE_STATUS_LABEL: Record<ProfileStatus, string> = {
  STUDENT: "재학",
  LEAVE: "휴학",
  GRADUATED: "졸업",
  EMPLOYED: "재직",
  FOUNDER: "창업",
  GRAD_SCHOOL: "대학원",
  ETC: "기타",
};

export const INTEREST_OPTIONS = [
  "기획·PM",
  "디자인",
  "프론트엔드",
  "백엔드",
  "앱",
  "데이터·AI",
  "인프라·DevOps",
  "마케팅",
  "창업",
  "기타",
] as const;
export type InterestLabel = (typeof INTEREST_OPTIONS)[number];

/** 관심 분야 API 코드 (backend domain/profile/Interest) */
export type InterestCode = "PLANNING_PM" | "DESIGN" | "FRONTEND" | "BACKEND" | "APP" | "DATA_AI" | "INFRA_DEVOPS" | "MARKETING" | "STARTUP" | "ETC";
export const INTEREST_CODE: Record<InterestLabel, InterestCode> = {
  "기획·PM": "PLANNING_PM",
  디자인: "DESIGN",
  프론트엔드: "FRONTEND",
  백엔드: "BACKEND",
  앱: "APP",
  "데이터·AI": "DATA_AI",
  "인프라·DevOps": "INFRA_DEVOPS",
  마케팅: "MARKETING",
  창업: "STARTUP",
  기타: "ETC",
};
export const INTEREST_LABEL = Object.fromEntries(Object.entries(INTEREST_CODE).map(([label, code]) => [code, label])) as Record<InterestCode, InterestLabel>;

/** 커피챗 받기 상태 (CS-01 · Badge/Availability) */
export type ReceiveMode = "OPEN" | "CLOSED" | "RESTING" | "SCHEDULED";

export interface Alumni {
  id: string;
  name: string;
  generation: number; // 기수
  part: LionPart;
  parts?: LionPart[];
  status: ProfileStatus;
  headline: string; // "핀테크 스타트업 · 백엔드 개발자"
  intro: string; // 한 줄 소개
  skills: string[];
  interests: string[];
  receive: ReceiveMode;
  receiveNote?: string; // "11/20부터 받아요"
  allowGroup: boolean; // 여러 명 신청 가능
  bookmarked?: boolean;
  email?: string;
  links?: { label: string; url: string }[];
  company?: string;
  role?: string; // 현재 직무
  isStaff?: boolean; // 운영진
  completion?: number; // 프로필 완성도 %
}

export type CoffeeChatStatus =
  | "PENDING"
  | "ACCEPTED"
  | "SCHEDULED"
  | "COMPLETED"
  | "REJECTED"
  | "CANCELED"
  | "EXPIRED";

export const COFFEE_STATUS_LABEL: Record<CoffeeChatStatus, string> = {
  PENDING: "대기 중",
  ACCEPTED: "수락됨",
  SCHEDULED: "일정 확정",
  COMPLETED: "완료",
  REJECTED: "거절됨",
  CANCELED: "취소됨",
  EXPIRED: "만료됨",
};

export type CoffeeChatRejectReason = "SCHEDULE" | "HARD_TOPIC" | "NO_TIME" | "ETC";
export const COFFEE_REJECT_LABEL: Record<CoffeeChatRejectReason, string> = {
  SCHEDULE: "일정이 맞지 않아요",
  HARD_TOPIC: "답하기 어려운 주제예요",
  NO_TIME: "당분간 여유가 없어요",
  ETC: "기타",
};

export type MeetingMode = "ONLINE" | "OFFLINE" | "ANY";

export interface CoffeeChat {
  id: string;
  status: CoffeeChatStatus;
  /** 받는 사자 (사자) */
  receiverId: string;
  /** 대표 신청자 */
  requesterId: string;
  /** 함께 신청한 동문 */
  memberIds: string[];
  topic: string;
  message: string;
  questions: string[];
  mode: MeetingMode;
  candidates: string[]; // 일정 후보 (ISO)
  scheduledAt?: string;
  createdAt: string;
  expiresAt?: string;
  rejectReason?: CoffeeChatRejectReason;
  rejectNote?: string;
  roomId?: string;
  fromRecruitId?: string;
}

export type ChatRoomType = "DIRECT" | "GROUP";
export type ChatRoomStatus = "REQUESTED" | "ACTIVE" | "DECLINED" | "EXPIRED" | "CANCELED";

export interface ChatMessage {
  id: string;
  senderId: string | "system";
  text: string;
  at: string;
  readBy?: string[];
}

export interface ChatRoom {
  id: string;
  type: ChatRoomType;
  status: ChatRoomStatus;
  memberIds: string[];
  title?: string;
  coffeeChatId?: string;
  messages: ChatMessage[];
  unread: number;
  isRequest?: boolean; // 메시지 요청
}

/** 커뮤니티 카테고리 (B-01 왼쪽 세로 메뉴 순서) */
export type PostCategory = "CONTEST" | "JOB" | "MATERIAL" | "EVENT" | "NOW" | "FREE" | "MEETUP" | "RECRUIT";
export const POST_CATEGORY_LABEL: Record<PostCategory, string> = {
  CONTEST: "공모전",
  JOB: "채용·협업",
  MATERIAL: "족보·자료",
  EVENT: "세미나·행사",
  NOW: "지금 사자는",
  FREE: "자유",
  MEETUP: "번개",
  RECRUIT: "커피챗",
};
export const POST_CATEGORIES = Object.keys(POST_CATEGORY_LABEL) as PostCategory[];
/** 글쓰기 본문 placeholder (B-08) */
export const POST_CATEGORY_HINT: Record<PostCategory, string> = {
  CONTEST: "공모전 이름, 주최, 마감일, 신청 링크를 적어주세요",
  JOB: "회사·팀 이름, 하는 일, 마감일, 지원 방법을 적어주세요",
  MATERIAL: "과목명, 교수님, 학기, 자료 설명을 적어주세요",
  EVENT: "일시, 장소(또는 링크), 신청 방법을 적어주세요",
  NOW: "이번 주 우리 기수 활동을 공유해주세요",
  FREE: "자유롭게 이야기해요",
  MEETUP: "모임 소개를 적어주세요",
  RECRUIT: "모임 소개를 적어주세요",
};

export type RecruitStatus = "OPEN" | "APPLIED" | "ENDED";

export interface Comment {
  id: string;
  authorId: string;
  anonymous?: boolean;
  text: string;
  at: string;
}

export interface Post {
  id: string;
  category: PostCategory;
  title: string;
  body: string;
  authorId: string;
  anonymous?: boolean;
  keywords: string[];
  views: number;
  likes: number;
  comments: Comment[];
  createdAt: string;
  images?: string[];
  files?: { name: string; size: string }[];
  links?: string[];
  deadline?: string; // 공모전·채용 마감 (D-n 태그)
  edited?: boolean;
  // 모집글 (RECRUIT)
  recruit?: {
    targetId: string; // 신청할 사자
    status: RecruitStatus;
    max: number;
    memberIds: string[];
    deadline: string;
  };
  // 번개 (MEETUP)
  meetup?: {
    at: string;
    place: string;
    max: number;
    memberIds: string[];
  };
}

export type NotificationTab = "ALL" | "COFFEE" | "CHAT" | "COMMUNITY";

export interface AppNotification {
  id: string;
  tab: Exclude<NotificationTab, "ALL">;
  actorId?: string;
  text: string;
  at: string;
  read: boolean;
  href: string;
  quick?: "coffee-request";
  targetId?: string;
}

export interface PendingSignup {
  id: string;
  name: string;
  generation: number;
  email: string;
  part: LionPart;
  appliedAt: string;
  match: "MATCH" | "CHECK"; // 동문 명단 대조 결과
  /** AD-10 참고 열: 같은 이름·기수가 이미 있음 / 반려 후 재신청 */
  note?: "DUPLICATE" | "REAPPLY";
  status: UserStatus;
}

export interface Invite {
  id: string;
  name: string;
  generation: number;
  email?: string;
  token: string;
  status: "UNUSED" | "USED" | "EXPIRED" | "CANCELED";
  createdAt: string;
  expiresAt: string;
}
