import { useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import AppHeader from "../components/common/AppHeader";
import StatCard from "../components/common/StatCard";
import CountdownTimer from "../components/common/CountdownTimer";
import ProfitChart from "../components/report/ProfitChart";
import WeatherCard from "../components/report/WeatherCard";
import BankruptModal from "../components/report/BankruptModal";

// TODO: Replace with API data
const MOCK_NORMAL = {
  revenue: 1250000, expense: 450000, netProfit: 800000,
  visitors: 128, reputation: 4.3, soldCount: 83, remainingStock: 45, discardedStock: 2,
  changes: {
    revenue: { value: "+15%", positive: true },
    expense: { value: "+5%", positive: false },
    netProfit: { value: "+20%", positive: true },
    visitors: { value: "+2%", positive: true },
    reputation: { value: "+0.2", positive: true },
  },
  chartData: [
    { day: 1, value: 400000 }, { day: 2, value: 600000 },
    { day: 3, value: 800000, isCurrent: true }, { day: 4, value: 0, isFuture: true },
  ],
  weather: { temperature: 24, condition: "맑음, 바람 약간", emoji: "☀️", tip: "야외 활동하기 완벽한 날씨! 재고를 늘리세요." },
  warning: null as string | null,
  isBankrupt: false,
};

const MOCK_BANKRUPT = {
  revenue: 250000, expense: 1450000, netProfit: -1200000,
  visitors: 12, reputation: 1.2, soldCount: 3, remainingStock: 450, discardedStock: 120,
  changes: {
    revenue: { value: "-75%", positive: false },
    expense: { value: "+300%", positive: false },
    netProfit: { value: "파산", positive: false },
    visitors: { value: "-90%", positive: false },
    reputation: { value: "-3.1", positive: false },
  },
  chartData: [
    { day: 1, value: 400000 }, { day: 2, value: -100000 }, { day: 3, value: -300000 },
    { day: 4, value: -500000 }, { day: 5, value: -800000 },
    { day: 6, value: -1200000, isCurrent: true }, { day: 7, value: 0, isFuture: true },
  ],
  weather: { temperature: null, condition: "알 수 없음", emoji: "💨", tip: "상점이 폐업했습니다." },
  warning: "파산했습니다: 3일 연속 적자 발생",
  isBankrupt: true,
};

export default function ReportPage() {
  const { day: dayParam } = useParams<{ day: string }>();
  const navigate = useNavigate();
  const day = Number(dayParam) || 1;

  const data = day >= 6 ? MOCK_BANKRUPT : MOCK_NORMAL;
  const [showBankruptModal, setShowBankruptModal] = useState(false);

  const fmt = (v: number) => v < 0 ? `-₩${Math.abs(v).toLocaleString()}` : `₩${v.toLocaleString()}`;

  return (
    <div className="min-h-screen bg-[#FDFDFB] text-slate-900 font-display flex flex-col">
      <AppHeader nickname="Owner" />

      {showBankruptModal && (
        <BankruptModal onClose={() => { setShowBankruptModal(false); navigate("/"); }} />
      )}

      <main className={`flex-1 flex justify-center py-8 px-4 sm:px-10 pt-24 ${showBankruptModal ? "blur-[2px] pointer-events-none" : ""}`}>
        <div className="flex flex-col max-w-[1024px] w-full gap-8">
          {/* Header */}
          <div className="flex flex-col sm:flex-row justify-between items-start sm:items-end gap-4 border-b border-slate-200 pb-6">
            <div className="flex flex-col gap-2">
              <h1 className="text-4xl font-black leading-tight tracking-tight">Day {day} 결과 리포트</h1>
              {data.isBankrupt
                ? <p className="text-rose-dark text-base font-medium">영업 종료 - 파산 상태</p>
                : <p className="text-slate-500 text-base">오늘 하루 매장 운영 결과를 확인하세요.</p>}
            </div>
            <div className="flex items-center gap-2 bg-slate-100 px-4 py-2 rounded-full">
              <span className="material-symbols-outlined text-primary text-xl">hourglass_top</span>
              <CountdownTimer
                initialSeconds={10}
                label={data.isBankrupt ? "결과 확인" : "다음날 이동"}
                onComplete={() => data.isBankrupt ? setShowBankruptModal(true) : navigate(`/game/${day + 1}/prep`)}
                variant="inline"
              />
            </div>
          </div>

          {/* Warning */}
          {data.warning && (
            <div className={`rounded-xl p-4 flex items-center gap-3 ${
              data.isBankrupt ? "bg-rose-soft border border-rose-dark text-white" : "bg-red-50 border border-red-100 text-red-700"
            }`}>
              <span className={`material-symbols-outlined text-2xl ${data.isBankrupt ? "text-white" : "text-red-500"}`}>warning</span>
              <h3 className="tracking-tight text-lg font-bold">{data.warning}</h3>
            </div>
          )}

          {/* Stats */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <StatCard label="매출" value={fmt(data.revenue)} change={data.changes.revenue} icon="payments" iconBg="bg-green-100" iconColor="text-green-600" />
            <StatCard label="지출" value={fmt(data.expense)} change={data.changes.expense} icon="shopping_cart_checkout" iconBg="bg-red-100" iconColor="text-red-600" />
            <StatCard label="순이익" value={fmt(data.netProfit)} change={data.changes.netProfit}
              icon={data.netProfit >= 0 ? "savings" : "money_off"}
              iconBg={data.netProfit >= 0 ? "bg-primary/20" : "bg-rose-100"}
              iconColor={data.netProfit >= 0 ? "text-primary-dark" : "text-rose-dark"} highlight />
            <StatCard label="방문객 수" value={`${data.visitors}명`} change={data.changes.visitors} icon="groups" iconBg="bg-slate-100" iconColor="text-slate-600" />
            <StatCard label="평판" value={String(data.reputation)} change={data.changes.reputation} icon="star" iconBg="bg-yellow-100" iconColor="text-yellow-600" />
            <StatCard label="판매 수량" value={`${data.soldCount}개`} subtext="총 판매 완료" icon="shopping_bag" iconBg="bg-blue-100" iconColor="text-blue-600" />
            <StatCard label="남은 재고" value={`${data.remainingStock}개`} subtext={data.isBankrupt ? "이월 예정 (압류됨)" : "이월 예정"} icon="inventory_2" iconBg="bg-purple-100" iconColor="text-purple-600" />
            <StatCard label="폐기된 재고" value={`${data.discardedStock}개`} subtext={data.discardedStock > 50 ? "대규모 손실 발생" : "손실 발생"}
              icon="delete" iconBg="bg-slate-100" iconColor="text-slate-500" highlight={data.discardedStock > 50} />
          </div>

          {/* Chart + Weather */}
          <div className="grid grid-cols-1 lg:grid-cols-4 gap-4">
            <ProfitChart data={data.chartData} isBankrupt={data.isBankrupt} />
            <WeatherCard {...data.weather} disabled={data.isBankrupt} />
          </div>
        </div>
      </main>
    </div>
  );
}
