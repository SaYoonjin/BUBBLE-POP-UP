import { useState } from "react";
import { Link } from "react-router-dom";
import ProfileDropdown from "../components/common/ProfileDropdown";
import CountdownTimer from "../components/common/CountdownTimer";
import CozyPillTabs from "../components/common/CozyPillTabs";
import CozyMenuSelector from "../components/game/CozyMenuSelector";
import CozyPriceTag from "../components/game/CozyPriceTag";
import CozyQuantityDial from "../components/game/CozyQuantityDial";
import CozyBellButton from "../components/game/CozyBellButton";
import CozyNewspaper from "../components/game/CozyNewspaper";
import CozyNotebook from "../components/game/CozyNotebook";

const mockMenus = [
  { id: 1, emoji: "🍜", name: "마라탕" },
  { id: 2, emoji: "🐟", name: "붕어빵", tag: "Low Stock" },
  { id: 3, emoji: "🍪", name: "쿠키", tag: "Best Seller!" },
  { id: 4, emoji: "🧋", name: "버블티", tag: "Fresh" },
  { id: 5, emoji: "🥪", name: "샌드위치" },
];

const mockPopulationRanking = [
  { rank: 1, name: "홍대", change: "+2.4%", positive: true },
  { rank: 2, name: "성수", change: "-", positive: true },
  { rank: 3, name: "부산", change: "-0.8%", positive: false },
  { rank: 4, name: "이태원", change: "+5%", positive: true },
  { rank: 5, name: "성수동", change: "+1%", positive: true },
];

const mockRevenueRanking = [
  { rank: 1, name: "홍대", change: "+5.2%", positive: true },
  { rank: 2, name: "성수", change: "-", positive: true },
  { rank: 3, name: "부산", change: "-", positive: true },
  { rank: 4, name: "명동", change: "+3%", positive: true },
  { rank: 5, name: "강남", change: "-1.2%", positive: false },
];

const mockNews = [
  { id: 1, title: "요즘 뜨는 디저트, '약과 쿠키' 인기 급상승 중", content: "전통 간식인 약과와 서양식 쿠키를 결합한 '약과 쿠키'가 MZ세대를 중심으로 큰 인기를 끌고 있습니다. 전문가들은 이러한 '할매니얼' 트렌드가 당분간 지속될 것으로 전망했습니다." },
  { id: 2, title: "원두 가격 3개월 만에 소폭 하락세", content: "국제 원두 가격이 3개월 만에 하락세로 돌아섰습니다." },
  { id: 3, title: "이번 주말 전국 비 예보, 유동인구 감소 예상", content: "기상청에 따르면 이번 주말 전국적으로 비가 내릴 예정입니다." },
  { id: 4, title: "홍대 주변 20대 유동인구 15% 증가", content: "봄 시즌 개강 효과와 맞물린 것으로 분석됩니다." },
];

const TABS = [
  { key: "prep", label: "영업 준비", icon: "restaurant_menu" },
  { key: "news", label: "버블 뉴스", icon: "newspaper" },
];

export default function CozyPrepPage() {
  const [tab, setTab] = useState("prep");
  const [selectedMenu, setSelectedMenu] = useState<number | null>(3);
  const [price, setPrice] = useState(4500);
  const [quantity, setQuantity] = useState(42);
  const [expandedNewsId, setExpandedNewsId] = useState<number | null>(1);

  const costPrice = 4000;
  const day = 3;

  return (
    <div className="min-h-screen font-cozy-display text-cozy-ink bg-cozy-warm/30">
      {/* ── Header ── */}
      <header className="sticky top-0 z-50 flex items-center justify-between bg-cozy-cream/90 backdrop-blur-md px-6 py-4 lg:px-12 border-b border-cozy-wood-light shadow-sm">
        <Link to="/" className="flex items-center gap-3 group">
          <div className="size-10 bg-cozy-primary/10 rounded-full flex items-center justify-center text-cozy-primary group-hover:bg-cozy-primary group-hover:text-white transition-colors">
            <span className="material-symbols-outlined text-2xl">bubble_chart</span>
          </div>
          <h2 className="text-xl font-black tracking-tight text-cozy-primary">BubbleBubble</h2>
        </Link>
        <ProfileDropdown nickname="Chef Amy" />
      </header>

      {/* ── Main ── */}
      <main className="w-full relative">
        {/* Wood texture */}
        <div
          className="absolute inset-0 opacity-[0.07] pointer-events-none"
          style={{ backgroundImage: "repeating-linear-gradient(45deg, rgba(139,94,60,0.3) 0px, rgba(139,94,60,0.3) 2px, transparent 2px, transparent 10px)" }}
        />

        <div className="relative z-10 max-w-6xl mx-auto px-4 py-8 lg:px-8 flex flex-col gap-8">
          {/* Page Header */}
          <div className="flex items-center justify-between">
            <div className="text-center flex-1">
              <div className="text-sm text-cozy-wood-dark font-medium mb-2">
                <span className="material-symbols-outlined text-[14px] align-middle mr-1">calendar_today</span>
                2026년 3월 17일 · DAY {day}
              </div>
              <h1 className="text-4xl md:text-5xl font-black text-[#5D4037] drop-shadow-sm">
                {tab === "prep" ? "Morning Prep" : "Bubble News"}
              </h1>
              <p className="text-cozy-wood-dark font-medium text-lg mt-1">
                {tab === "prep" ? "오늘 장사 준비를 시작합시다!" : "오늘의 시장 소식을 확인하세요."}
              </p>
            </div>
            <CountdownTimer initialSeconds={50} label="준비 시간" />
          </div>

          {/* Tabs */}
          <CozyPillTabs tabs={TABS} activeKey={tab} onChange={setTab} />

          {/* ── Tab: 영업 준비 ── */}
          {tab === "prep" ? (
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
              <div className="lg:col-span-8">
                <CozyMenuSelector
                  menus={mockMenus}
                  selectedId={selectedMenu}
                  onSelect={setSelectedMenu}
                  dayLabel={`DAY ${day} 추천`}
                />
              </div>

              <div className="lg:col-span-4 flex flex-col gap-6">
                <CozyPriceTag
                  price={price}
                  min={1000}
                  max={10000}
                  step={500}
                  costPrice={costPrice}
                  onChange={setPrice}
                />

                <CozyQuantityDial
                  quantity={quantity}
                  min={1}
                  max={500}
                  onChange={setQuantity}
                />

                {/* Total Cost */}
                <div className="flex items-center justify-between px-2">
                  <span className="text-cozy-wood-dark font-medium">총 예상 비용</span>
                  <span className="font-cozy-hand text-3xl font-bold text-[#5D4037]">
                    ₩{(costPrice * quantity).toLocaleString()}
                  </span>
                </div>

                <CozyBellButton onClick={() => alert("영업 시작!")} />
              </div>
            </div>
          ) : (
            /* ── Tab: 버블 뉴스 ── */
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
              {/* Left: Rankings (2 compact notebooks stacked) */}
              <div className="lg:col-span-4 flex flex-col gap-6 sticky top-24">
                <CozyNotebook
                  title="유동인구 순위"
                  items={mockPopulationRanking}
                  compact
                />
                <CozyNotebook
                  title="지역 매출 순위"
                  items={mockRevenueRanking}
                  memo="홍대 재고 확인하기!"
                  compact
                />
              </div>
              {/* Right: Newspaper (wider) */}
              <div className="lg:col-span-8">
                <CozyNewspaper
                  items={mockNews}
                  expandedId={expandedNewsId}
                  onToggle={(id) => setExpandedNewsId(expandedNewsId === id ? null : id)}
                  day={day}
                />
              </div>
            </div>
          )}
        </div>
      </main>
    </div>
  );
}
