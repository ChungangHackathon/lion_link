import type { LionPart } from "@/lib/types";

/** Detailed profile sections (P-01 · P-02). Keyed by alumni id; others get a generated default. */
export interface ProfileDetail {
  location?: string;
  about: string;
  educations: { school: string; major: string; degree: string; from: string; to: string; gpa?: string }[];
  careers: { company: string; role: string; type: string; from: string; to: string; duration: string }[];
  lion: { generation: number; role: string; part: LionPart; projects: { event: string; name: string; desc: string; role: string; link?: string }[] }[];
  projects: { name: string; type: string; from: string; to: string; skills: string[] }[];
  repSkills: string[];
  skills: string[];
  certs: { kind: "자격증" | "수상" | "대외활동" | "언어"; text: string }[];
  links: { label: string; url: string; visibility: "ALL" | "CONNECTED" | "ME" }[];
  email?: { value: string; visibility: "ALL" | "CONNECTED" | "ME" };
  phone?: { value: string; visibility: "ALL" | "CONNECTED" | "ME" };
  coffeeTopics: string[];
  coffeeTimes: string;
  coffeeModes: string;
  coffeeRegions: string;
}

const base: ProfileDetail = {
  location: "서울 강남",
  about:
    "핀테크 스타트업에서 결제·정산 서버를 만들고 있어요. 7기 아기사자로 시작해 8기 운영진까지 했고, 후배들 커리어 고민 듣는 걸 좋아합니다. 대규모 트래픽을 받는 결제 시스템을 설계하면서 겪은 장애 대응 경험, 신입 때 자주 하는 실수, 이직 준비 과정까지 편하게 풀어보려고요. 최근에는 정산 배치를 Kotlin 코루틴으로 옮기는 작업을 하고 있고, 사내 스터디에서 MSA 전환 사례를 발표했어요. 커피챗은 평일 저녁 온라인이 가장 편해요. 주말에는 사이드 프로젝트로 가계부 자동 분류 서비스를 만들고 있습니다.",
  educations: [
    { school: "중앙대학교", major: "소프트웨어학부", degree: "학사", from: "2014.03", to: "2020.02", gpa: "4.32/4.5" },
    { school: "연세대학교", major: "인공지능대학원", degree: "석사", from: "2020.03", to: "2022.02" },
  ],
  careers: [
    { company: "핀테크 스타트업", role: "백엔드 개발자", type: "정규직", from: "2024.03", to: "현재", duration: "2년 9개월" },
    { company: "IT 대기업", role: "서버 개발자", type: "정규직", from: "2022.07", to: "2023.12", duration: "1년 6개월" },
    { company: "연세대학교 인공지능 대학원", role: "석사", type: "", from: "2020.03", to: "2022.02", duration: "2년" },
  ],
  lion: [
    {
      generation: 7,
      role: "백엔드 아기사자",
      part: "BACKEND",
      projects: [
        { event: "아이디어톤 · 7기", name: "동아리 매칭 서비스", desc: "교내 동아리와 신입생을 잇는 웹 서비스", role: "백엔드 · API 설계", link: "github.com/kimsaja/club-match" },
        { event: "해커톤 · 7기", name: "동아리 매칭 서비스", desc: "교내 동아리와 신입생을 잇는 웹 서비스", role: "백엔드 · API 설계", link: "github.com/kimsaja/club-match" },
      ],
    },
    { generation: 8, role: "백엔드 운영진", part: "BACKEND", projects: [] },
  ],
  projects: [{ name: "가계부 자동 분류", type: "사이드 프로젝트 · 백엔드", from: "2023.03", to: "2023.08", skills: ["Spring", "Kotlin", "AWS"] }],
  repSkills: ["Spring", "Kotlin", "AWS", "결제"],
  skills: ["Docker", "Java", "MSA", "JPA", "Redis", "Kafka", "gRPC", "Terraform"],
  certs: [
    { kind: "자격증", text: "정보처리기사 · 2019.11" },
    { kind: "수상", text: "중커톤 대상 · 2019.08" },
    { kind: "언어", text: "영어 · 비즈니스" },
  ],
  links: [{ label: "깃허브", url: "github.com/kimsaja", visibility: "ALL" }],
  email: { value: "kimsaja@gmail.com", visibility: "CONNECTED" },
  phone: { value: "010-1234-5678", visibility: "CONNECTED" },
  coffeeTopics: ["커리어·이직", "취업 준비", "프로젝트·개발"],
  coffeeTimes: "평일 저녁 · 토요일 오후",
  coffeeModes: "온라인 · 오프라인",
  coffeeRegions: "강남, 판교",
};

const details: Record<string, Partial<ProfileDetail>> = {
  "u-1": base,
  "u-me": {
    location: "서울 동작",
    about: "React로 커머스 화면 만들고 있어요. 13기 프론트엔드 아기사자였고, 지금은 인턴으로 일하면서 취업 준비 중이에요.",
    educations: [{ school: "중앙대학교", major: "소프트웨어학부", degree: "학사", from: "2021.03", to: "재학 중" }],
    careers: [{ company: "커머스 스타트업", role: "프론트엔드 인턴", type: "인턴", from: "2026.07", to: "현재", duration: "4개월" }],
    lion: [{ generation: 13, role: "프론트엔드 아기사자", part: "FRONTEND", projects: [{ event: "중커톤 · 13기", name: "멋사동네", desc: "멋사 동문 커피챗 네트워크", role: "프론트엔드", link: "github.com/lionlink" }] }],
    projects: [],
    repSkills: ["React", "TypeScript", "Next.js"],
    skills: ["Tailwind", "Zustand", "Figma"],
    certs: [],
    links: [{ label: "깃허브", url: "github.com/jungang", visibility: "ALL" }],
    email: { value: "likelion@cau.ac.kr", visibility: "ALL" },
    coffeeTopics: ["프로젝트·개발", "커리어·이직", "취업"],
  },
};

export function profileDetail(id: string): ProfileDetail {
  return { ...base, ...(details[id] ?? { about: "", careers: [], projects: [], certs: [] }) };
}
