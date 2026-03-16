import { useState } from "react";
import AppHeader from "../components/common/AppHeader";
import SeasonCard from "../components/common/SeasonCard";

const allSeasons = [
  {
    season: "Season 3", rank: "1st", rankVariant: "gold" as const,
    location: "강남", storeName: "디저트 팝업 스토어",
    revenue: "₩1,250,000", rewardPoints: "500P", usedPoints: "1,200 P",
    pointDetails: [
      { label: "임대료 할인", value: "300P" },
      { label: "원재료값 할인", value: "200P" },
      { label: "광고 패키지", value: "500P" },
      { label: "알바 고용", value: "200P" },
    ],
  },
  {
    season: "Season 2", rank: "4th", rankVariant: "normal" as const,
    location: "홍대", storeName: "패션 악세사리 팝업",
    revenue: "₩890,000", rewardPoints: "200P", usedPoints: "800 P",
    pointDetails: [
      { label: "인테리어", value: "500P" },
      { label: "임대료 할인", value: "300P" },
    ],
  },
  {
    season: "Season 1.5", rank: "파산", rankVariant: "bankrupt" as const,
    location: "이태원", storeName: "수제 맥주 팝업",
    revenue: "₩0", rewardPoints: "0P", usedPoints: "350 P", isBankrupt: true,
    pointDetails: [
      { label: "초기 투자", value: "150P" },
      { label: "긴급 자금", value: "200P" },
    ],
  },
  {
    season: "Season 1", rank: "12th", rankVariant: "normal" as const,
    location: "성수", storeName: "비건 베이커리",
    revenue: "₩450,000", rewardPoints: "50P", usedPoints: "500 P",
    pointDetails: [{ label: "임대료 할인", value: "500P" }],
  },
  {
    season: "Season 0.5", rank: "8th", rankVariant: "normal" as const,
    location: "명동", storeName: "타코야끼 팝업",
    revenue: "₩320,000", rewardPoints: "30P", usedPoints: "200 P",
    pointDetails: [{ label: "광고", value: "200P" }],
  },
];

const INITIAL_SHOW = 3;

export default function MyPage() {
  const [showAll, setShowAll] = useState(false);
  const visibleSeasons = showAll ? allSeasons : allSeasons.slice(0, INITIAL_SHOW);

  return (
    <div className="min-h-screen bg-[#FDFDFB] text-slate-900 font-display flex flex-col">
      <AppHeader nickname="버블킹" />

      <main className="flex-grow w-full max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-10 pt-24 space-y-8">
        {/* Profile */}
        <section>
          <div className="bg-white rounded-xl shadow-soft p-8 flex flex-col sm:flex-row items-center justify-between gap-6">
            <div className="flex-grow text-center sm:text-left space-y-2">
              <div className="flex items-center justify-center sm:justify-start gap-2">
                <h1 className="text-2xl font-bold">버블킹</h1>
                <button className="text-gray-400 hover:text-primary transition-colors" title="닉네임 수정">
                  <span className="material-symbols-outlined text-lg">edit</span>
                </button>
              </div>
              <p className="text-gray-500 font-mono text-sm">user@ssafy.com</p>
            </div>
            <button className="px-4 py-2 border border-gray-300 rounded-lg text-sm font-medium text-gray-500 hover:bg-gray-50 transition-colors">
              로그아웃
            </button>
          </div>
        </section>

        {/* Season History */}
        <section className="space-y-6">
          <h2 className="text-xl font-bold border-l-4 border-primary pl-3">시즌 기록</h2>
          <div className="space-y-4">
            {visibleSeasons.map((s, i) => (
              <SeasonCard key={i} {...s} />
            ))}
          </div>
          {!showAll && allSeasons.length > INITIAL_SHOW && (
            <div className="flex justify-center mt-8">
              <button
                onClick={() => setShowAll(true)}
                className="flex items-center gap-2 text-sm text-gray-500 hover:text-primary transition-colors"
              >
                <span>더 보기 ({allSeasons.length - INITIAL_SHOW}개)</span>
                <span className="material-symbols-outlined text-lg">expand_more</span>
              </button>
            </div>
          )}
          {showAll && allSeasons.length > INITIAL_SHOW && (
            <div className="flex justify-center mt-8">
              <button
                onClick={() => setShowAll(false)}
                className="flex items-center gap-2 text-sm text-gray-500 hover:text-primary transition-colors"
              >
                <span>접기</span>
                <span className="material-symbols-outlined text-lg">expand_less</span>
              </button>
            </div>
          )}
        </section>
      </main>

      <footer className="bg-white border-t border-gray-100 mt-12 py-8">
        <div className="max-w-4xl mx-auto px-4 text-center text-xs text-gray-400">
          <p>© 2026 BubbleBubble. All rights reserved.</p>
          <div className="mt-2 space-x-4">
            <a className="hover:underline" href="#">이용약관</a>
            <a className="hover:underline" href="#">개인정보처리방침</a>
          </div>
        </div>
      </footer>
    </div>
  );
}
