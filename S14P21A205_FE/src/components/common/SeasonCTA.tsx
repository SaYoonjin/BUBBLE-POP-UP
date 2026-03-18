import { Link } from "react-router-dom";
import { useEffect, useState } from "react";

type SeasonStatus = "joinable" | "between-seasons";

interface SeasonCTAProps {
  seasonNumber: number;
  day: number;
  totalDays: number;
  deadlineTime: Date;
  linkTo: string;
  status: SeasonStatus;
}

function formatTime(totalSeconds: number): string {
  const h = Math.floor(totalSeconds / 3600);
  const m = Math.floor((totalSeconds % 3600) / 60);
  const s = totalSeconds % 60;
  const pad = (n: number) => String(n).padStart(2, "0");
  return h > 0 ? `${pad(h)}:${pad(m)}:${pad(s)}` : `${pad(m)}:${pad(s)}`;
}

export default function SeasonCTA({ seasonNumber, day, totalDays, deadlineTime, linkTo, status }: SeasonCTAProps) {
  const [remaining, setRemaining] = useState(() =>
    Math.max(0, Math.floor((deadlineTime.getTime() - Date.now()) / 1000))
  );

  useEffect(() => {
    const timer = setInterval(() => {
      const diff = Math.max(0, Math.floor((deadlineTime.getTime() - Date.now()) / 1000));
      setRemaining(diff);
      if (diff <= 0) clearInterval(timer);
    }, 1000);
    return () => clearInterval(timer);
  }, [deadlineTime]);

  const isUrgent = remaining <= 10 && remaining > 0;
  const isExpired = remaining <= 0;
  const isBetweenSeasons = status === "between-seasons";

  const timerLabel = isBetweenSeasons
    ? "다음 시즌 시작까지"
    : `${day}일차 마감까지`;

  const titleText = isBetweenSeasons
    ? "시즌 마무리 중"
    : `${seasonNumber}번째 시즌`;

  const badgeText = isBetweenSeasons
    ? "대기 중"
    : `Day ${day} / ${totalDays}`;

  const buttonText = isBetweenSeasons
    ? "결과 확인하기"
    : "게임 참여하기";

  return (
    <div className="bg-white rounded-[24px] shadow-soft p-8 w-full flex flex-col min-h-[400px] relative overflow-hidden justify-between">
      <div className="space-y-6 relative z-10">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <span className="material-symbols-outlined text-primary text-[28px]">bubble_chart</span>
            <span className="text-sm font-bold text-slate-400 uppercase tracking-wider">Season</span>
          </div>
          <div className={`px-4 py-1.5 rounded-full text-xs font-bold uppercase tracking-wider ${
            isBetweenSeasons
              ? "bg-slate-100 text-slate-500"
              : "bg-primary/10 text-primary-dark"
          }`}>
            {badgeText}
          </div>
        </div>
        <h2 className="text-2xl md:text-3xl font-bold">{titleText}</h2>
        <div className="flex flex-col mt-4">
          <span className="text-sm text-gray-500 font-medium mb-1">{timerLabel}</span>
          <div className={`text-[56px] font-bold font-countdown leading-none tracking-tight tabular-nums transition-colors duration-300 ${
            isExpired
              ? "text-slate-300"
              : isUrgent
                ? "text-red-500"
                : "text-slate-900"
          }`}>
            {formatTime(remaining)}
          </div>
        </div>
      </div>
      <div className="mt-8 relative z-10 w-full">
        {!isExpired ? (
          <Link
            to={linkTo}
            className={`w-full h-16 text-white text-lg font-bold rounded-2xl shadow-md hover:shadow-lg transition-all transform hover:-translate-y-0.5 flex items-center justify-center gap-2 group ${
              isBetweenSeasons
                ? "bg-slate-600 hover:bg-slate-700"
                : "bg-primary hover:bg-primary-dark"
            }`}
          >
            {buttonText}
            <span className="material-symbols-outlined text-[20px] font-bold group-hover:translate-x-1 transition-transform">arrow_forward</span>
          </Link>
        ) : (
          <div className="w-full h-16 bg-slate-100 text-slate-400 text-lg font-bold rounded-2xl flex items-center justify-center">
            시간 종료
          </div>
        )}
      </div>
      <div className="absolute -right-10 -bottom-10 w-64 h-64 bg-gradient-to-br from-primary/10 to-transparent rounded-full opacity-50 pointer-events-none" />
    </div>
  );
}
