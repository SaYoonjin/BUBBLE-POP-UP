import { useState, useEffect } from "react";

interface CountdownTimerProps {
  /** 남은 시간 (초) */
  initialSeconds: number;
  /** 0에 도달했을 때 콜백 */
  onComplete?: () => void;
  /** 라벨 */
  label?: string;
}

export default function CountdownTimer({
  initialSeconds,
  onComplete,
  label = "남은 시간",
}: CountdownTimerProps) {
  const [seconds, setSeconds] = useState(initialSeconds);

  useEffect(() => {
    setSeconds(initialSeconds);
  }, [initialSeconds]);

  useEffect(() => {
    if (seconds <= 0) {
      onComplete?.();
      return;
    }
    const timer = setInterval(() => setSeconds((s) => s - 1), 1000);
    return () => clearInterval(timer);
  }, [seconds, onComplete]);

  const min = Math.floor(seconds / 60);
  const sec = seconds % 60;
  const isUrgent = seconds <= 10;

  // 원형 시계 진행률 (SVG)
  const radius = 28;
  const circumference = 2 * Math.PI * radius;
  const progress = initialSeconds > 0 ? seconds / initialSeconds : 0;
  const strokeDashoffset = circumference * (1 - progress);

  return (
    <div className="flex items-center gap-3">
      {/* 원형 시계 */}
      <div className="relative size-16 flex items-center justify-center">
        <svg className="absolute inset-0 -rotate-90" viewBox="0 0 64 64">
          <circle
            cx="32" cy="32" r={radius}
            fill="none"
            stroke="#f1f5f9"
            strokeWidth="4"
          />
          <circle
            cx="32" cy="32" r={radius}
            fill="none"
            stroke={isUrgent ? "#ef4444" : "#A8BFA9"}
            strokeWidth="4"
            strokeLinecap="round"
            strokeDasharray={circumference}
            strokeDashoffset={strokeDashoffset}
            className="transition-all duration-1000 ease-linear"
          />
        </svg>
        <div className={`relative z-10 font-mono text-lg font-black tracking-tight ${isUrgent ? "text-red-500" : "text-slate-900"}`}>
          {String(min).padStart(2, "0")}:{String(sec).padStart(2, "0")}
        </div>
      </div>
      <div className="flex flex-col">
        <span className="text-[10px] uppercase tracking-widest font-semibold text-slate-400">{label}</span>
        <span className={`text-xs font-medium ${isUrgent ? "text-red-500" : "text-slate-500"}`}>
          {isUrgent ? "서두르세요!" : "준비 중"}
        </span>
      </div>
    </div>
  );
}
