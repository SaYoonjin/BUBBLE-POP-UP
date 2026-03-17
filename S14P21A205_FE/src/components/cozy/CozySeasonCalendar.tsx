import { Link } from "react-router-dom";

interface CozySeasonCalendarProps {
  seasonNumber: number;
  day: number;
  countdown: string;
  bestRank?: number;
  linkTo: string;
}

export default function CozySeasonCalendar({ seasonNumber, day, countdown, bestRank, linkTo }: CozySeasonCalendarProps) {
  return (
    <div
      className="bg-white rounded-xl overflow-hidden border-t-[12px] border-cozy-primary rotate-[1deg] hover:rotate-0 transition-transform duration-500 shadow-[0_20px_40px_-8px_rgba(0,0,0,0.25),0_8px_16px_-4px_rgba(0,0,0,0.15)]"
      style={{
        backgroundImage: "url('https://www.transparenttextures.com/patterns/cream-paper.png')",
        backgroundBlendMode: "multiply",
      }}
    >
      <div className="p-8 flex flex-col items-center text-center">
        <div className="w-full flex justify-between items-start mb-10">
          <div className="text-left">
            <span className="font-cozy-serif italic text-cozy-ink/40 text-sm">Season Status</span>
            <h2 className="font-cozy-serif text-4xl font-black text-cozy-ink">{seasonNumber}번째 시즌</h2>
          </div>
          <div className="bg-cozy-sage/10 px-4 py-1 rounded-full text-cozy-sage font-black text-xs uppercase tracking-tighter border border-cozy-sage/20">
            Day {day}
          </div>
        </div>

        <div className="my-6">
          <span className="font-cozy-hand text-sm text-cozy-dusty-rose block mb-2">TIME UNTIL CLOSING</span>
          <div className="text-[80px] font-black text-cozy-ink leading-none tracking-tighter font-mono tabular-nums">
            {countdown}
          </div>
        </div>

        <div className="w-full mt-10 space-y-4">
          <Link
            to={linkTo}
            className="w-full py-5 bg-cozy-ink text-white font-cozy-serif text-xl font-bold rounded-lg shadow-lg hover:bg-cozy-primary transition-all group flex items-center justify-center gap-3"
          >
            게임 참여하기
            <span className="material-symbols-outlined group-hover:translate-x-1 transition-transform">arrow_forward</span>
          </Link>
          {bestRank && <p className="font-cozy-hand text-cozy-ink/40 text-sm">최근 최고 순위: {bestRank}위</p>}
        </div>
      </div>
    </div>
  );
}
