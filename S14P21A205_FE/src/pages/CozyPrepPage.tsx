import { useState } from "react";
import { Link } from "react-router-dom";
import CozyNewspaper from "../components/game/CozyNewspaper";

const mockPopulationRanking = [
  { rank: 1, name: "홍대", change: "+2.4%", positive: true, barWidth: "92%" },
  { rank: 2, name: "성수", change: "-", positive: true, barWidth: "78%" },
  { rank: 3, name: "부산", change: "-0.8%", positive: false, barWidth: "58%" },
  { rank: 4, name: "이태원", change: "+5%", positive: true, barWidth: "44%" },
  { rank: 5, name: "성수동", change: "+1%", positive: true, barWidth: "32%" },
];

const mockRevenueRanking = [
  { rank: 1, name: "홍대", change: "+5.2%", positive: true, barWidth: "96%" },
  { rank: 2, name: "성수", change: "-", positive: true, barWidth: "74%" },
  { rank: 3, name: "부산", change: "-", positive: true, barWidth: "56%" },
  { rank: 4, name: "명동", change: "+3%", positive: true, barWidth: "46%" },
  { rank: 5, name: "강남", change: "-1.2%", positive: false, barWidth: "38%" },
];

const mockNews = [
  {
    id: 1,
    title: "요즘 뜨는 디저트, '약과 쿠키' 인기 급상승 중",
    content:
      "전통 간식인 약과와 서양식 쿠키를 결합한 '약과 쿠키'가 MZ세대를 중심으로 큰 인기를 끌고 있습니다. 전문가들은 이러한 '할매니얼' 트렌드가 당분간 지속될 것으로 전망했습니다.",
  },
  { id: 2, title: "원두 가격 3개월 만에 소폭 하락세", content: "국제 원두 가격이 3개월 만에 하락세로 돌아섰습니다." },
  { id: 3, title: "이번 주말 전국 비 예보, 유동인구 감소 예상", content: "기상청에 따르면 이번 주말 전국적으로 비가 내릴 예정입니다." },
  { id: 4, title: "홍대 주변 20대 유동인구 15% 증가", content: "봄 시즌 개강 효과와 맞물린 것으로 분석됩니다." },
];

export default function CozyPrepPage() {
  const [expandedNewsId, setExpandedNewsId] = useState<number | null>(1);
  const day = 3;

  return (
    <div className="min-h-screen font-cozy-display text-cozy-ink bg-cozy-warm/30">
      <header className="sticky top-0 z-50 flex items-center justify-between bg-cozy-cream/90 backdrop-blur-md px-6 py-4 lg:px-12 border-b border-cozy-wood-light shadow-sm">
        <Link to="/" className="flex items-center gap-3 group">
          <div className="size-10 bg-cozy-primary/10 rounded-full flex items-center justify-center text-cozy-primary group-hover:bg-cozy-primary group-hover:text-white transition-colors">
            <span className="material-symbols-outlined text-2xl">bubble_chart</span>
          </div>
          <h2 className="text-xl font-black tracking-tight text-cozy-primary">BubbleBubble</h2>
        </Link>
        <div className="hidden sm:flex items-center gap-2 rounded-full border border-cozy-wood-light bg-white/70 px-4 py-2 shadow-sm">
          <span className="material-symbols-outlined text-cozy-primary text-lg">newspaper</span>
          <span className="text-sm font-bold text-cozy-ink">Market Brief</span>
        </div>
      </header>

      <main className="w-full relative">
        <div
          className="absolute inset-0 opacity-[0.07] pointer-events-none"
          style={{
            backgroundImage:
              "repeating-linear-gradient(45deg, rgba(139,94,60,0.3) 0px, rgba(139,94,60,0.3) 2px, transparent 2px, transparent 10px)",
          }}
        />

        <div className="relative z-10 max-w-6xl mx-auto px-4 py-8 lg:px-8 flex flex-col gap-8">
          <section className="bg-cozy-paper rounded-sm border border-black/5 px-6 py-8 shadow-[0_12px_30px_-8px_rgba(0,0,0,0.18),0_4px_10px_-2px_rgba(0,0,0,0.08)]">
            <div className="text-center">
              <div className="text-sm text-cozy-wood-dark font-medium mb-2">
                <span className="material-symbols-outlined text-[14px] align-middle mr-1">calendar_today</span>
                2026년 3월 17일 · DAY {day}
              </div>
              <h1 className="text-4xl md:text-5xl font-black text-[#5D4037] drop-shadow-sm">Bubble Newsroom</h1>
              <p className="text-cozy-wood-dark font-medium text-lg mt-1">
                코지 프렙 화면에서 쓰던 뉴스 레이아웃만 따로 모아둔 브리핑 화면입니다.
              </p>
            </div>
          </section>

          <CozyNewspaper
            items={mockNews}
            expandedId={expandedNewsId}
            onToggle={(id) => setExpandedNewsId(expandedNewsId === id ? null : id)}
            day={day}
            rankings={[
              { title: "유동인구 순위", eyebrow: "Foot Traffic Ranking", items: mockPopulationRanking },
              { title: "지역 매출 순위", eyebrow: "Regional Revenue Ranking", items: mockRevenueRanking },
            ]}
          />
        </div>
      </main>
    </div>
  );
}
