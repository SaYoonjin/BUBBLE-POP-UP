interface WeatherCardProps {
  temperature: number | null;
  condition: string;
  emoji: string;
  tip: string;
  disabled?: boolean;
}

export default function WeatherCard({ temperature, condition, emoji, tip, disabled = false }: WeatherCardProps) {
  return (
    <div className={`lg:col-span-2 rounded-xl p-6 shadow-soft border flex items-center justify-between ${
      disabled
        ? "bg-gradient-to-br from-slate-50 to-slate-100 border-slate-200 opacity-50 grayscale"
        : "bg-gradient-to-br from-blue-50 to-indigo-50 border-blue-100"
    }`}>
      <div className="flex flex-col justify-between h-full">
        <div>
          <h3 className="text-lg font-bold text-slate-800 mb-1 flex items-center gap-2">
            <span className={`material-symbols-outlined ${disabled ? "text-slate-400" : "text-blue-500"}`}>calendar_month</span>
            내일 날씨
          </h3>
          <p className="text-sm text-slate-500">{disabled ? "영업이 종료되었습니다." : "야외 판매 전략을 세워보세요."}</p>
        </div>
        <div className="mt-4">
          <p className="text-3xl font-black text-slate-800">{temperature !== null ? `${temperature}°C` : "--°C"}</p>
          <p className={`text-base font-medium ${disabled ? "text-slate-400" : "text-blue-600"}`}>{condition}</p>
        </div>
      </div>
      <div className="flex flex-col items-end gap-4">
        <div className={`text-6xl ${disabled ? "text-slate-300" : "animate-pulse"}`}>{emoji}</div>
        <div className="bg-white/60 rounded-lg p-3 text-sm text-slate-500 text-right backdrop-blur-sm max-w-[200px]">
          {tip}
        </div>
      </div>
    </div>
  );
}
