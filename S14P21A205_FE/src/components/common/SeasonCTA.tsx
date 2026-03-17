import { Link } from "react-router-dom";

interface SeasonCTAProps {
  seasonNumber: number;
  day: number;
  countdown: string;
  bestRank?: number;
  linkTo: string;
}

export default function SeasonCTA({ seasonNumber, day, countdown, bestRank, linkTo }: SeasonCTAProps) {
  return (
    <div className="bg-white rounded-[24px] shadow-soft p-8 w-full flex flex-col min-h-[400px] relative overflow-hidden justify-between">
      <div className="space-y-6 relative z-10">
        <div className="flex items-center justify-between mb-4">
          <div className="w-12 h-12 flex items-center justify-center text-3xl bg-gray-50 rounded-full">🎢</div>
          <div className="bg-primary/10 text-primary-dark px-4 py-1.5 rounded-full text-xs font-bold uppercase tracking-wider">
            Day {day}
          </div>
        </div>
        <h2 className="text-2xl md:text-3xl font-bold">{seasonNumber}번째 시즌</h2>
        <div className="flex flex-col mt-4">
          <span className="text-sm text-gray-500 font-medium mb-1">오늘 마감까지</span>
          <div className="text-[64px] font-bold font-mono leading-none tracking-tight tabular-nums">
            {countdown}
          </div>
        </div>
      </div>
      <div className="mt-8 relative z-10 w-full">
        <Link
          to={linkTo}
          className="w-full h-16 bg-primary hover:bg-primary-dark text-white text-lg font-bold rounded-2xl shadow-md hover:shadow-lg transition-all transform hover:-translate-y-0.5 flex items-center justify-center gap-2 group"
        >
          게임 참여하기
          <span className="material-symbols-outlined text-[20px] font-bold group-hover:translate-x-1 transition-transform">arrow_forward</span>
        </Link>
        {bestRank && (
          <p className="text-center text-gray-500 text-sm mt-4 font-medium">최근 최고 순위: {bestRank}위</p>
        )}
      </div>
      <div className="absolute -right-10 -bottom-10 w-64 h-64 bg-gradient-to-br from-primary/10 to-transparent rounded-full opacity-50 pointer-events-none" />
    </div>
  );
}
