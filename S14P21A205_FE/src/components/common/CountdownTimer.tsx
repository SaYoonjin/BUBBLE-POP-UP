import { useEffect, useRef, useState } from "react";

interface CountdownTimerProps {
  initialSeconds?: number;
  endTimestampMs?: number;
  onComplete?: () => void;
  label?: string;
  variant?: "pill" | "display" | "inline";
  showIcon?: boolean;
}

function resolveRemainingSeconds(
  initialSeconds: number | undefined,
  endTimestampMs: number | undefined,
) {
  if (typeof endTimestampMs === "number") {
    return Math.max(0, Math.ceil((endTimestampMs - Date.now()) / 1000));
  }

  return Math.max(0, initialSeconds ?? 0);
}

export default function CountdownTimer({
  initialSeconds,
  endTimestampMs,
  onComplete,
  label = "남은 시간",
  variant = "pill",
  showIcon = true,
}: CountdownTimerProps) {
  const timerIdentity =
    typeof endTimestampMs === "number" ? `end-${endTimestampMs}` : `initial-${initialSeconds ?? 0}`;

  return (
    <CountdownTimerInstance
      key={timerIdentity}
      initialSeconds={initialSeconds}
      endTimestampMs={endTimestampMs}
      onComplete={onComplete}
      label={label}
      variant={variant}
      showIcon={showIcon}
    />
  );
}

function CountdownTimerInstance({
  initialSeconds,
  endTimestampMs,
  onComplete,
  label = "남은 시간",
  variant = "pill",
  showIcon = true,
}: CountdownTimerProps) {
  const [seconds, setSeconds] = useState(() =>
    resolveRemainingSeconds(initialSeconds, endTimestampMs),
  );
  const hasCompletedRef = useRef(false);

  useEffect(() => {
    if (seconds <= 0) {
      if (!hasCompletedRef.current) {
        hasCompletedRef.current = true;
        onComplete?.();
      }

      return;
    }

    const timer = window.setInterval(() => {
      setSeconds((currentSeconds) => {
        if (typeof endTimestampMs === "number") {
          return resolveRemainingSeconds(undefined, endTimestampMs);
        }

        return Math.max(0, currentSeconds - 1);
      });
    }, 1000);

    return () => window.clearInterval(timer);
  }, [seconds, endTimestampMs, onComplete]);

  const minutes = Math.floor(seconds / 60);
  const remainingSeconds = seconds % 60;
  const isUrgent = seconds <= 10;
  const formattedTime = `${String(minutes).padStart(2, "0")}:${String(remainingSeconds).padStart(2, "0")}`;

  if (variant === "display") {
    return (
      <div
        aria-label={`${label} ${formattedTime}`}
        className={`font-display text-[2rem] font-black tabular-nums leading-none tracking-[0.06em] sm:text-[2.4rem] ${
          isUrgent ? "text-red-500" : "text-slate-900"
        }`}
      >
        {formattedTime}
      </div>
    );
  }

  if (variant === "inline") {
    return (
      <div
        aria-label={`${label} ${formattedTime}`}
        className={`inline-flex items-center gap-2 ${
          isUrgent ? "text-red-500" : "text-slate-900"
        }`}
      >
        {showIcon && (
          <span
            aria-hidden="true"
            className={`flex size-7 items-center justify-center rounded-full ${
              isUrgent ? "bg-red-100 text-red-500" : "bg-primary/15 text-primary-dark"
            }`}
          >
            <span className="material-symbols-outlined text-[16px]">schedule</span>
          </span>
        )}
        <span className="font-display text-[1.4rem] font-black tabular-nums tracking-[0.04em]">
          {formattedTime}
        </span>
      </div>
    );
  }

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
      {showIcon && (
        <span
          aria-hidden="true"
          className={`flex size-6 items-center justify-center rounded-full ${
            isUrgent ? "bg-red-100 text-red-500" : "bg-primary/15 text-primary-dark"
          }`}
        >
          <span className="material-symbols-outlined text-[15px]">schedule</span>
        </span>
      )}
      <div
        className={`font-display text-[0.95rem] font-extrabold tabular-nums tracking-[0.02em] md:text-[1rem] ${
          isUrgent ? "text-red-500" : "text-slate-900"
        }`}
      >
        {formattedTime}
      </div>
    </div>
  );
}
