/* eslint-disable @next/next/no-img-element */
import { cn } from "@/lib/cn";

/**
 * A-01 hero: gradient sky, white wordmark with roof, and the 1기~14기 village
 * strip along the bottom. The applicant's 기수 house is "lit" (gen-N-lit.svg) with a tag.
 * Assets exported from Figma (public/brand).
 *
 * 한 화면에 다 보이도록 로고·마을 높이를 화면 높이(vh)에 맞춰 줄인다.
 * 안의 글자는 컨테이너 높이 단위(cqh)라 그림과 같이 줄어든다.
 * 화면이 아주 낮으면(약 640px 미만) 그때만 세로 스크롤이 생긴다.
 */
export function VillageHero({ children, generation, tag }: { children: React.ReactNode; generation: number | null; tag?: string }) {
  return (
    <div className="relative flex min-h-dvh flex-1 flex-col overflow-x-hidden" style={{ backgroundImage: "linear-gradient(90deg, #2155a4 0%, #9293ab 53%, #ffcfb2 100%)" }}>
      <main className="relative z-10 mx-auto flex w-full max-w-[560px] flex-1 flex-col items-center justify-center gap-[clamp(12px,2.5vh,30px)] px-4 py-[clamp(16px,4vh,80px)]">
        <Wordmark />
        {children}
      </main>
      <Village generation={generation} tag={tag} />
    </div>
  );
}

function Wordmark() {
  return (
    <div className="relative max-w-full shrink-0 [container-type:size]" style={{ height: "clamp(90px, 15vh, 235px)", aspectRatio: "462 / 235" }}>
      {/* wordmark pieces – positions from Figma inset values */}
      <img src="/brand/logo-meot.svg" alt="" className="absolute" style={{ left: "5.95%", top: "37.5%", width: "16.27%", height: "36.72%" }} />
      <img src="/brand/logo-sa.svg" alt="" className="absolute" style={{ left: "28.57%", top: "35.94%", width: "19.05%", height: "36.72%" }} />
      <img src="/brand/logo-dong.svg" alt="" className="absolute" style={{ left: "50.4%", top: "39.06%", width: "17.85%", height: "38.67%" }} />
      <img src="/brand/logo-ne.svg" alt="" className="absolute" style={{ left: "73.81%", top: "35.94%", width: "15.87%", height: "38.28%" }} />
      <img src="/brand/logo-roof.svg" alt="멋사동네" className="absolute" style={{ left: "49.6%", top: "12.5%", width: "42.07%", height: "17.19%" }} />
      <span className="absolute border-orange" style={{ left: "85.91%", top: "4.69%", width: "11.71%", height: "23.04%", borderWidth: "3.4cqh" }} />
      <p className="absolute w-full text-center font-bold leading-none text-light-text" style={{ top: "88.09%", fontSize: "8.1cqh" }}>
        멋쟁이사자처럼 동문 네트워크
      </p>
    </div>
  );
}

const HOUSES = [
  { l: 4.55, t: 59.41, w: 5.55 },
  { l: 11.49, t: 50.5, w: 4.8 },
  { l: 17.42, t: 60.4, w: 6.06 },
  { l: 24.62, t: 61.39, w: 4.8 },
  { l: 31.69, t: 50.5, w: 3.79 },
  { l: 37.37, t: 59.41, w: 5.56 },
  { l: 43.69, t: 60.4, w: 6.06 },
  { l: 50.88, t: 50.5, w: 4.8 },
  { l: 57.45, t: 61.39, w: 4.8 },
  { l: 64.52, t: 50.5, w: 3.79 },
  { l: 70.2, t: 59.41, w: 5.56 },
  { l: 76.52, t: 60.4, w: 6.06 },
  { l: 83.71, t: 50.5, w: 4.8 },
  { l: 90.28, t: 61.39, w: 4.8 },
];

/** 집 그림이 바뀌면 올린다 (예전 그림이 브라우저 캐시에 남지 않게). 13기 그림이 불 켜진 채로 들어 있던 적이 있다. */
const HOUSE_VERSION = 2;

/**
 * 1298×331 Figma 마을. 높이만 화면에 맞추고 너비는 비율대로 (넘치면 양옆이 잘린다).
 * generation이 없으면(기수 선택 전) 어떤 집도 불을 켜지 않고 이름표도 숨긴다.
 */
function Village({ generation, tag = "냐냐냥의 집" }: { generation: number | null; tag?: string }) {
  const lit = generation && generation >= 1 && generation <= 14 ? generation : null;
  const litHouse = lit ? HOUSES[lit - 1] : null;
  return (
    <div className="pointer-events-none relative shrink-0 self-center [container-type:size]" style={{ height: "clamp(150px, 24vh, 331px)", aspectRatio: "1298 / 331" }} aria-hidden>
      <img src="/brand/village/tree-l.svg" alt="" className="absolute" style={{ left: "1.39%", top: "70.3%", width: "2.27%", height: "12.87%" }} />
      <img src="/brand/village/tree-r.svg" alt="" className="absolute" style={{ left: "96.34%", top: "70.3%", width: "2.27%", height: "12.87%" }} />
      {HOUSES.map((h, i) => (
        <img key={i} src={`/brand/village/gen-${i + 1}${i + 1 === lit ? "-lit" : ""}.svg?v=${HOUSE_VERSION}`} alt="" className="absolute" style={{ left: `${h.l}%`, top: `${h.t}%`, width: `${h.w}%`, height: `${83.17 - h.t}%` }} />
      ))}
      <img src="/brand/village/ground.svg" alt="" className="absolute" style={{ left: "0.76%", top: "83.17%", width: "98.48%", height: "1.5%" }} />
      <div className="absolute flex w-full justify-between px-[6.4%] text-center" style={{ top: "87.62%", fontSize: "5.44cqh" }}>
        {HOUSES.map((_, i) => (
          <span key={i} className={cn("w-[5%]", i + 1 === lit ? "font-bold text-white" : "text-[#9fb3d6]")}>
            {i + 1}기
          </span>
        ))}
      </div>
      {/* lit tag above the applicant's house */}
      {litHouse && (
        <div className="absolute flex flex-col items-center" style={{ left: `${litHouse.l + litHouse.w / 2 - 5.3}%`, top: "29.6%", width: "10.6%" }}>
          <div className="relative w-full">
            <img src="/brand/village/tag.svg" alt="" className="w-full" />
            <span className="absolute inset-0 grid place-items-center truncate px-[3.6cqh] font-bold text-[#1a0a00]" style={{ fontSize: "5.44cqh" }}>
              {tag}
            </span>
          </div>
          <img src="/brand/village/tag-tail.svg" alt="" className="-mt-px w-[12%]" />
        </div>
      )}
    </div>
  );
}
