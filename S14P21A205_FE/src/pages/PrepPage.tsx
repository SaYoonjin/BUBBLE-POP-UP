import { useState } from "react";
import AppHeader from "../components/common/AppHeader";
import CountdownTimer from "../components/common/CountdownTimer";
import MenuSelector from "../components/game/MenuSelector";
import PriceSlider from "../components/game/PriceSlider";
import QuantityCounter from "../components/game/QuantityCounter";
import CozyNewspaper from "../components/game/CozyNewspaper";

const mockMenus = [
  { id: 1, emoji: "🍜", name: "마라탕" },
  { id: 2, emoji: "🐟", name: "붕어빵" },
  { id: 3, emoji: "🍪", name: "쿠키" },
  { id: 4, emoji: "🧋", name: "버블티" },
  { id: 5, emoji: "🥪", name: "샌드위치" },
];

const mockPopulationRanking = [
  { rank: 1, name: "홍대", change: "2.4%", positive: true, barWidth: "85%" },
  { rank: 2, name: "성수", change: "-", positive: true, barWidth: "65%" },
  { rank: 3, name: "부산", change: "0.8%", positive: false, barWidth: "45%" },
];

const mockRevenueRanking = [
  { rank: 1, name: "홍대", change: "5.2%", positive: true, barWidth: "92%" },
  { rank: 2, name: "성수", change: "-", positive: true, barWidth: "58%" },
  { rank: 3, name: "부산", change: "-", positive: true, barWidth: "35%" },
];

const mockNews = [
  { id: 1, title: "요즘 뜨는 디저트, '약과 쿠키' 인기 급상승 중", content: "전통 간식인 약과와 서양식 쿠키를 결합한 '약과 쿠키'가 MZ세대를 중심으로 큰 인기를 끌고 있습니다. 특히 편의점과 프랜차이즈 카페들이 앞다퉈 신제품을 출시하며 경쟁이 치열해지고 있습니다. 전문가들은 이러한 '할매니얼(할머니+밀레니얼)' 트렌드가 당분간 지속될 것으로 전망했습니다." },
  { id: 2, title: "원두 가격 3개월 만에 소폭 하락세... 카페 사장님들 '안도'", content: "국제 원두 가격이 3개월 만에 하락세로 돌아섰습니다. 브라질 수확량 증가와 물류비 안정이 주요 원인으로 분석됩니다." },
  { id: 3, title: "이번 주말 전국 비 예보, 배달 주문량 증가 예상", content: "기상청에 따르면 이번 주말 전국적으로 비가 내릴 예정입니다. 외출이 줄어드는 만큼 배달 수요가 늘어날 것으로 보입니다." },
  { id: 4, title: "성공적인 카페 운영을 위한 5가지 인테리어 팁", content: "작은 인테리어 변화가 매출에 큰 영향을 줄 수 있습니다. 조명, 좌석 배치, 음악, 향기, 그린 인테리어 5가지를 소개합니다." },
  { id: 5, title: "홍대 주변, 20대 유동인구 지난달 대비 15% 증가", content: "홍대 일대의 20대 유동인구가 지난달 대비 15% 증가한 것으로 나타났습니다. 봄 시즌 개강 효과와 맞물린 것으로 분석됩니다." },
];

type Tab = "prep" | "news";

export default function PrepPage() {
  const [tab, setTab] = useState<Tab>("prep");
  const [selectedMenu, setSelectedMenu] = useState<number | null>(3);
  const [price, setPrice] = useState(5000);
  const [quantity, setQuantity] = useState(120);
  const [expandedNewsId, setExpandedNewsId] = useState<number | null>(1);

  const costPrice = 4000;
  const totalCost = costPrice * quantity;
  const day = 3;

  return (
    <div className="min-h-screen bg-[#FDFDFB] text-slate-900 font-display flex flex-col">
      <AppHeader nickname="Owner" />

      {/* Main */}
      <main className="flex-1 flex flex-col items-center py-8 pt-24 px-4 sm:px-10">
        <div className="w-full max-w-5xl flex flex-col gap-8">
          {/* Page Header */}
          <div className="flex flex-col gap-6">
            <div className="flex items-center justify-between">
              <div className="flex flex-col gap-1">
                <div className="flex items-center gap-2 text-slate-400 text-sm font-medium">
                  <span className="material-symbols-outlined text-[1.25rem]">calendar_today</span>
                  <span>2026년 3월 17일 · DAY {day}</span>
                </div>
                <h1 className="text-slate-900 text-4xl font-black leading-tight tracking-tight">
                  {tab === "prep" ? "영업 준비" : "버블 뉴스"}
                </h1>
              </div>
              <CountdownTimer initialSeconds={50} label="준비 시간" />
            </div>

            {/* Tabs */}
            <div className="flex items-center gap-8 border-b border-slate-100">
              <button
                onClick={() => setTab("prep")}
                className={`pb-3 text-base transition-colors ${
                  tab === "prep"
                    ? "border-b-2 border-slate-900 text-slate-900 font-bold"
                    : "text-slate-400 hover:text-slate-600 font-medium"
                }`}
              >
                영업 준비
              </button>
              <button
                onClick={() => setTab("news")}
                className={`pb-3 text-base transition-colors ${
                  tab === "news"
                    ? "border-b-2 border-slate-900 text-slate-900 font-bold"
                    : "text-slate-400 hover:text-slate-600 font-medium"
                }`}
              >
                버블 뉴스
              </button>
            </div>
          </div>

          {/* Tab: 영업 준비 */}
          {tab === "prep" ? (
            <div className="flex flex-col gap-6">
              <MenuSelector menus={mockMenus} selectedId={selectedMenu} onSelect={setSelectedMenu} />

              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <PriceSlider
                  price={price}
                  min={1000}
                  max={10000}
                  step={500}
                  costPrice={costPrice}
                  onChange={setPrice}
                />
                <div className="flex flex-col gap-6">
                  <QuantityCounter quantity={quantity} min={50} max={500} onChange={setQuantity} />
                  <div className="flex flex-col gap-4">
                    <div className="flex items-center justify-between px-4 py-2">
                      <span className="text-slate-500 font-medium">총 예상 비용</span>
                      <span className="text-3xl font-black text-slate-900 tracking-tight">
                        ₩{totalCost.toLocaleString()}
                      </span>
                    </div>
                    <button className="w-full bg-primary hover:bg-primary-dark text-slate-900 hover:text-white font-bold text-lg py-5 px-8 rounded-2xl shadow-lg shadow-primary/20 transition-all flex items-center justify-center gap-2 group">
                      <span>준비 완료하기</span>
                      <span className="material-symbols-outlined group-hover:translate-x-1 transition-transform">
                        arrow_forward
                      </span>
                    </button>
                  </div>
                </div>
              </div>
            </div>
          ) : (
            /* Tab: 버블 뉴스 */
            <CozyNewspaper
              items={mockNews}
              expandedId={expandedNewsId}
              onToggle={(id) => setExpandedNewsId(expandedNewsId === id ? null : id)}
              day={day}
              rankings={[
                { title: "유동인구 순위", items: mockPopulationRanking },
                { title: "지역 매출 순위", items: mockRevenueRanking, memo: "성수동 재고와 가격대 다시 확인하기!" },
              ]}
            />
          )}
        </div>
      </main>
    </div>
  );
}
