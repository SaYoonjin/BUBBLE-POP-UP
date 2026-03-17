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
  const formattedTime = `${String(min).padStart(2, "0")}:${String(sec).padStart(2, "0")}`;

  return (
    <div
      aria-label={`${label} ${formattedTime}`}
      className={`inline-flex items-center gap-2 rounded-full border px-2.5 py-1.5 text-center ${
        isUrgent
          ? "border-red-200 bg-red-50/90 text-red-500"
          : "border-slate-200 bg-white/90 text-slate-700 shadow-soft"
      }`}
    >
      <span className="sr-only">{label}</span>
      <span
        aria-hidden="true"
        className={`flex size-6 items-center justify-center rounded-full ${
          isUrgent ? "bg-red-100 text-red-500" : "bg-primary/15 text-primary-dark"
        }`}
      >
        <span className="material-symbols-outlined text-[15px]">schedule</span>
      </span>
      <div className={`font-display tabular-nums text-[0.95rem] font-extrabold tracking-[0.02em] md:text-[1rem] ${
        isUrgent ? "text-red-500" : "text-slate-900"
      }`}>
        {formattedTime}
      </div>
    </div>
  );
}
