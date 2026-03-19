import { useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import AppHeader from "../components/common/AppHeader";
import StatCard from "../components/common/StatCard";
import Badge from "../components/common/Badge";
import CountdownTimer from "../components/common/CountdownTimer";
import ProfitChart from "../components/report/ProfitChart";
import WeatherCard from "../components/report/WeatherCard";
import BankruptModal from "../components/report/BankruptModal";

// API 응답 타입
interface ProfitByDay {
  first: number | null;
  second: number | null;
  third: number | null;
  fourth: number | null;
  fifth: number | null;
  sixth: number | null;
  seventh: number | null;
}

interface DayReportResponse {
  seasonId: number;
  day: number;
  storeName: string;
  locationName: string;
  menuName: string;
  revenue: number;
  totalCost: number;
  profit: ProfitByDay;
  visitors: number;
  salesCount: number;
  stockRemaining: number;
  stockDisposedCount: number;
  reputationScore: number;
  reputationChange: number;
  tomorrowWeather: { condition: "CLEAR" | "CLOUDY" | "RAIN" | "SNOW" | "STORM" } | null;
  isNextDayOrderDay: boolean | null;
  consecutiveDeficitDays: number;
}

const PROFIT_KEYS: (keyof ProfitByDay)[] = ["first", "second", "third", "fourth", "fifth", "sixth", "seventh"];

function profitToChartData(profit: ProfitByDay, currentDay: number) {
  return PROFIT_KEYS.map((key, i) => {
    const dayNum = i + 1;
    const value = profit[key];
    return {
      day: dayNum,
      value: value ?? 0,
      isCurrent: dayNum === currentDay,
      isFuture: value === null,
    };
  }).filter((d) => !d.isFuture || d.day === currentDay + 1);
}

/** 짝수일(2,4,6) 또는 마지막날(7)이면 재고가 전량 폐기 */
function isStockDisposalDay(day: number) {
  return day % 2 === 0 || day === 7;
}

// TODO: Replace with API call - GET /game/day/reports/{day}
const MOCK_NORMAL: DayReportResponse = {
  seasonId: 12,
  day: 3,
  storeName: "윤진이네 쫀쫀쿠키",
  locationName: "성수",
  menuName: "쫀쫀쿠키",
  revenue: 1200000,
  totalCost: 320000,
  profit: {
    first: 400000,
    second: 600000,
    third: 880000,
    fourth: null,
    fifth: null,
    sixth: null,
    seventh: null,
  },
  visitors: 150,
  salesCount: 142,
  stockRemaining: 45,
  stockDisposedCount: 8,
  reputationScore: 3.8,
  reputationChange: 0.2,
  tomorrowWeather: { condition: "CLEAR" },
  isNextDayOrderDay: false, // 홀수일 → 발주일 아님 (짝수일 리포트에서만 true)
  consecutiveDeficitDays: 0,
};

const MOCK_BANKRUPT: DayReportResponse = {
  seasonId: 12,
  day: 5,
  storeName: "윤진이네 쫀쫀쿠키",
  locationName: "성수",
  menuName: "쫀쫀쿠키",
  revenue: 120000,
  totalCost: 330000,
  profit: {
    first: 400000,
    second: -100000,
    third: -300000,
    fourth: -500000,
    fifth: -210000,
    sixth: null,
    seventh: null,
  },
  visitors: 23,
  salesCount: 18,
  stockRemaining: 120,
  stockDisposedCount: 45,
  reputationScore: 1.2,
  reputationChange: -0.8,
  tomorrowWeather: null,
  isNextDayOrderDay: null,
  consecutiveDeficitDays: 3,
};

export default function ReportPage() {
  const { day: dayParam } = useParams<{ day: string }>();
  const navigate = useNavigate();
  const day = Number(dayParam) || 1;

  // TODO: API 호출로 교체
  const data = day >= 6 ? MOCK_BANKRUPT : MOCK_NORMAL;
  const chartData = profitToChartData(data.profit, data.day);
  const todayProfit = data.profit[PROFIT_KEYS[data.day - 1]] ?? 0;

  const isBankrupt = data.consecutiveDeficitDays >= 3;
  const disposal = isStockDisposalDay(data.day);
  const [showBankruptModal, setShowBankruptModal] = useState(false);

  const fmt = (v: number) => v < 0 ? `-₩${Math.abs(v).toLocaleString()}` : `₩${v.toLocaleString()}`;

  const stockSubtext = isBankrupt
    ? "압류됨"
    : disposal
      ? "전량 폐기"
      : "다음날 이월";

  return (
    <div className="min-h-screen bg-[#FDFDFB] text-slate-900 font-display flex flex-col">
      <AppHeader />

      {showBankruptModal && (
        <BankruptModal onClose={() => { setShowBankruptModal(false); navigate("/"); }} isLastDay={data.day === 7} />
      )}

      <main className={`flex-1 flex justify-center py-8 px-4 sm:px-10 pt-24 ${showBankruptModal ? "blur-[2px] pointer-events-none" : ""}`}>
        <div className="flex flex-col max-w-[1024px] w-full gap-8">
          {/* Header */}
          <div className="flex flex-col sm:flex-row justify-between items-start sm:items-end gap-4 border-b border-slate-200 pb-6">
            <div className="flex flex-col gap-2">
              <div className="flex items-center gap-2">
                <Badge variant="gray" size="sm">시즌 {data.seasonId}</Badge>
                <Badge variant="green" size="sm">{data.locationName}</Badge>
                <Badge variant="gold" size="sm">{data.menuName}</Badge>
              </div>
              <h1 className="text-4xl font-black leading-tight tracking-tight">
                {data.storeName}
              </h1>
              {isBankrupt
                ? <p className="text-rose-dark text-base font-medium">Day {data.day} 영업 종료 — 파산 상태</p>
                : <p className="text-slate-500 text-base">Day {data.day} 운영 결과를 확인하세요.</p>}
            </div>
            <div className="flex flex-col items-end gap-1.5">
              <CountdownTimer
                initialSeconds={10}
                label={isBankrupt ? "결과 확인" : "다음날 이동"}
                onComplete={() => isBankrupt ? setShowBankruptModal(true) : navigate(`/game/${day + 1}/prep`)}
                variant="pill"
              />
              <span className="text-xs text-slate-400 font-medium pr-1">
                {isBankrupt ? "결과 화면으로 이동" : "다음날로 자동 이동"}
              </span>
            </div>
          </div>

          {/* Warning: 연속 적자 */}
          {data.consecutiveDeficitDays > 0 && (
            <div className={`rounded-xl p-4 flex items-center gap-3 ${
              isBankrupt
                ? "bg-rose-soft border border-rose-dark text-white"
                : "bg-red-50 border border-red-100 text-red-700"
            }`}>
              <span className={`material-symbols-outlined text-2xl ${isBankrupt ? "text-white" : "text-red-500"}`}>warning</span>
              <h3 className="tracking-tight text-lg font-bold">
                {isBankrupt
                  ? `파산했습니다: ${data.consecutiveDeficitDays}일 연속 적자 발생`
                  : `${data.consecutiveDeficitDays}일 연속 적자 중 (3일 연속 시 파산)`}
              </h3>
            </div>
          )}

          {/* 발주일 안내 */}
          {data.isNextDayOrderDay && (
            <div className="rounded-xl p-4 flex items-center gap-3 bg-primary/10 border border-primary/30 text-primary-dark">
              <span className="material-symbols-outlined text-2xl text-primary">local_shipping</span>
              <h3 className="tracking-tight text-base font-bold">내일은 발주일입니다! 재고를 확인하세요.</h3>
            </div>
          )}

          {/* Stats */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <StatCard label="매출" value={fmt(data.revenue)} icon="payments" iconBg="bg-green-100" iconColor="text-green-600" />
            <StatCard label="지출" value={fmt(data.totalCost)} icon="shopping_cart_checkout" iconBg="bg-red-100" iconColor="text-red-600" />
            <StatCard
              label="순이익"
              value={fmt(todayProfit)}
              icon={todayProfit >= 0 ? "savings" : "money_off"}
              iconBg={todayProfit >= 0 ? "bg-primary/20" : "bg-rose-100"}
              iconColor={todayProfit >= 0 ? "text-primary-dark" : "text-rose-dark"}
              highlight={todayProfit < 0}
            />
            <StatCard label="방문객 수" value={`${data.visitors}명`} icon="groups" iconBg="bg-slate-100" iconColor="text-slate-600" />
            <StatCard label="평판" value={String(data.reputationScore)}
              change={{ value: `${data.reputationChange >= 0 ? "+" : ""}${data.reputationChange}`, positive: data.reputationChange >= 0 }}
              icon="star" iconBg="bg-yellow-100" iconColor="text-yellow-600" />
            <StatCard label="판매 수량" value={`${data.salesCount}개`} subtext={data.menuName} icon="shopping_bag" iconBg="bg-blue-100" iconColor="text-blue-600" />
            <StatCard label="남은 재고" value={`${data.stockRemaining}개`} subtext={stockSubtext}
              icon="inventory_2" iconBg="bg-purple-100" iconColor="text-purple-600" />
            <StatCard label="폐기된 재고" value={`${data.stockDisposedCount}개`}
              subtext={data.stockDisposedCount > 50 ? "대규모 손실 발생" : "손실 발생"}
              icon="delete" iconBg="bg-slate-100" iconColor="text-slate-500"
              highlight={data.stockDisposedCount > 50} />
          </div>

          {/* Chart + Weather */}
          <div className="grid grid-cols-1 lg:grid-cols-4 gap-4">
            <ProfitChart data={chartData} isBankrupt={isBankrupt} />
            <WeatherCard
              condition={data.tomorrowWeather?.condition ?? null}
              disabled={isBankrupt}
            />
          </div>
        </div>
      </main>
    </div>
  );
}
