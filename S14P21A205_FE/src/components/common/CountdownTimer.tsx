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
      className={`min-w-[104px] rounded-2xl border px-4 py-3 text-center shadow-soft ${
        isUrgent
          ? "border-red-100 bg-red-50/90 text-red-500"
          : "border-slate-200 bg-white text-slate-900"
      }`}
    >
      <span className="sr-only">{label}</span>
      <div className="font-mono text-xl font-black tracking-tight md:text-[1.35rem]">
        {formattedTime}
      </div>
    </div>
  );
}
