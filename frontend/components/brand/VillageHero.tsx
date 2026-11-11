/* eslint-disable @next/next/no-img-element */
import { cn } from "@/lib/cn";

/**
 * A-01 hero: gradient sky, white wordmark with roof, and the 1기~14기 village
 * strip along the bottom. The invitee's 기수 house is "lit" with a tag.
 * Assets exported from Figma (public/brand).
 */
export function VillageHero({ children, generation }: { children: React.ReactNode; generation: number }) {
  return (
    <div className="relative flex min-h-full flex-1 flex-col overflow-hidden" style={{ backgroundImage: "linear-gradient(90deg, #2155a4 0%, #9293ab 53%, #ffcfb2 100%)" }}>
      <main className="relative z-10 mx-auto flex w-full max-w-[560px] flex-col items-center gap-[30px] px-4 pb-[360px] pt-20">
        <Wordmark />
        {children}
      </main>
      <Village generation={generation} />
    </div>
  );
}

function Wordmark() {
  return (
    <div className="relative h-[235px] w-[462px] max-w-full">
      {/* wordmark pieces – positions from Figma inset values */}
      <img src="/brand/logo-meot.svg" alt="" className="absolute" style={{ left: "5.95%", top: "37.5%", width: "16.27%", height: "36.72%" }} />
      <img src="/brand/logo-sa.svg" alt="" className="absolute" style={{ left: "28.57%", top: "35.94%", width: "19.05%", height: "36.72%" }} />
      <img src="/brand/logo-dong.svg" alt="" className="absolute" style={{ left: "50.4%", top: "39.06%", width: "17.85%", height: "38.67%" }} />
      <img src="/brand/logo-ne.svg" alt="" className="absolute" style={{ left: "73.81%", top: "35.94%", width: "15.87%", height: "38.28%" }} />
      <img src="/brand/logo-roof.svg" alt="멋사동네" className="absolute" style={{ left: "49.6%", top: "12.5%", width: "42.07%", height: "17.19%" }} />
      <span className="absolute border-8 border-orange" style={{ left: "85.91%", top: "4.69%", width: "11.71%", height: "23.04%" }} />
      <p className="absolute w-full text-center text-[19px] font-bold text-light-text" style={{ top: "88.09%" }}>
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

function Village({ generation }: { generation: number }) {
  const lit = Math.min(14, Math.max(1, generation));
  const litHouse = HOUSES[lit - 1];
  return (
    <div className="pointer-events-none absolute bottom-0 left-1/2 h-[331px] w-[1298px] -translate-x-1/2" aria-hidden>
      <img src="/brand/village/tree-l.svg" alt="" className="absolute" style={{ left: "1.39%", top: "70.3%", width: "2.27%", height: "12.87%" }} />
      <img src="/brand/village/tree-r.svg" alt="" className="absolute" style={{ left: "96.34%", top: "70.3%", width: "2.27%", height: "12.87%" }} />
      {HOUSES.map((h, i) => (
        <img key={i} src={`/brand/village/gen-${i + 1}.svg`} alt="" className="absolute" style={{ left: `${h.l}%`, top: `${h.t}%`, width: `${h.w}%`, height: `${83.17 - h.t}%` }} />
      ))}
      <img src="/brand/village/ground.svg" alt="" className="absolute" style={{ left: "0.76%", top: "83.17%", width: "98.48%", height: "1.5%" }} />
      <div className="absolute flex w-full justify-between px-[6.4%] text-center text-[18px]" style={{ top: "87.62%" }}>
        {HOUSES.map((_, i) => (
          <span key={i} className={cn("w-[5%]", i + 1 === lit ? "font-bold text-white" : "text-[#9fb3d6]")}>
            {i + 1}기
          </span>
        ))}
      </div>
      {/* lit tag above the invitee's house */}
      <div className="absolute flex flex-col items-center" style={{ left: `${litHouse.l + litHouse.w / 2 - 5.3}%`, top: "29.6%", width: "10.6%" }}>
        <div className="relative w-full">
          <img src="/brand/village/tag.svg" alt="" className="w-full" />
          <span className="absolute inset-0 grid place-items-center text-[18px] font-bold text-[#1a0a00]">냐냐냥의 집</span>
        </div>
        <img src="/brand/village/tag-tail.svg" alt="" className="-mt-px w-[12%]" />
      </div>
    </div>
  );
}
