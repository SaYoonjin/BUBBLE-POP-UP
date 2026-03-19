type WeatherCondition = "CLEAR" | "CLOUDY" | "RAIN" | "SNOW" | "STORM";

const WEATHER_MAP: Record<WeatherCondition, { emoji: string; label: string }> = {
  CLEAR: { emoji: "☀️", label: "맑음" },
  CLOUDY: { emoji: "☁️", label: "흐림" },
  RAIN: { emoji: "🌧️", label: "비" },
  SNOW: { emoji: "❄️", label: "눈" },
  STORM: { emoji: "⛈️", label: "폭풍" },
};

interface WeatherCardProps {
  condition: WeatherCondition | null;
  disabled?: boolean;
}

export default function WeatherCard({ condition, disabled = false }: WeatherCardProps) {
  const weather = condition ? WEATHER_MAP[condition] : null;

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
          <p className="text-sm text-slate-500">
            {disabled ? "영업이 종료되었습니다." : "내일의 날씨 예보입니다."}
          </p>
        </div>
        <div className="mt-4">
          <p className={`text-xl font-bold ${disabled ? "text-slate-400" : "text-blue-600"}`}>
            {weather ? weather.label : "--"}
          </p>
        </div>
      </div>
      <div className={`text-6xl ${disabled ? "text-slate-300" : "animate-pulse"}`}>
        {weather ? weather.emoji : "💨"}
      </div>
    </div>
  );
}
