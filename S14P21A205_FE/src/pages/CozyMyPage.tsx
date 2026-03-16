import { useState } from "react";
import CozyWoodBackground from "../components/cozy/CozyWoodBackground";
import CozyAppHeader from "../components/cozy/CozyAppHeader";
import CozySeasonRecord from "../components/cozy/CozySeasonRecord";

const allSeasons = [
  { season: "Season 3", rank: "1st", rankVariant: "gold" as const, location: "강남", storeName: "디저트 팝업 스토어", revenue: "₩1,250,000", rewardPoints: "500P" },
  { season: "Season 2", rank: "4th", rankVariant: "normal" as const, location: "홍대", storeName: "패션 악세사리 팝업", revenue: "₩890,000", rewardPoints: "200P" },
  { season: "Season 1.5", rank: "파산", rankVariant: "bankrupt" as const, location: "이태원", storeName: "수제 맥주 팝업", revenue: "₩0", rewardPoints: "0P", isBankrupt: true },
  { season: "Season 1", rank: "12th", rankVariant: "normal" as const, location: "성수", storeName: "비건 베이커리", revenue: "₩450,000", rewardPoints: "50P" },
  { season: "Season 0.5", rank: "8th", rankVariant: "normal" as const, location: "명동", storeName: "타코야끼 팝업", revenue: "₩320,000", rewardPoints: "30P" },
];

const INITIAL_SHOW = 3;

export default function CozyMyPage() {
  const [showAll, setShowAll] = useState(false);
  const visibleSeasons = showAll ? allSeasons : allSeasons.slice(0, INITIAL_SHOW);

  return (
    <CozyWoodBackground>
      <CozyAppHeader nickname="버블킹" />

      <main className="relative z-10 flex-1 w-full max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-10 pt-28 space-y-8">
        {/* Profile (Paper card) */}
        <section
          className="bg-cozy-paper rounded-sm shadow-cozy-paper p-8 rotate-[0.5deg] hover:rotate-0 transition-transform relative"
          style={{ backgroundImage: "url('https://www.transparenttextures.com/patterns/cream-paper.png')" }}
        >
          <div className="flex flex-col sm:flex-row items-center justify-between gap-6">
            <div className="flex-grow text-center sm:text-left space-y-2">
              <div className="flex items-center justify-center sm:justify-start gap-2">
                <h1 className="font-cozy-serif text-3xl font-black italic text-cozy-ink">버블킹</h1>
                <button className="text-cozy-ink/40 hover:text-cozy-primary transition-colors" title="닉네임 수정">
                  <span className="material-symbols-outlined text-lg">edit</span>
                </button>
              </div>
              <p className="text-cozy-ink/50 font-mono text-sm">user@ssafy.com</p>
            </div>
            <button className="px-4 py-2 bg-cozy-ink text-white text-xs font-bold uppercase tracking-widest hover:bg-cozy-primary transition-colors">
              로그아웃
            </button>
          </div>
        </section>

        {/* Season History */}
        <section className="space-y-6">
          <h2 className="font-cozy-hand text-3xl text-cozy-primary -rotate-1 ml-2">시즌 기록</h2>
          <div className="space-y-5">
            {visibleSeasons.map((s, i) => (
              <CozySeasonRecord key={i} {...s} />
            ))}
          </div>
          {!showAll && allSeasons.length > INITIAL_SHOW && (
            <div className="flex justify-center mt-8">
              <button
                onClick={() => setShowAll(true)}
                className="font-cozy-hand text-lg text-white/70 hover:text-white transition-colors flex items-center gap-2"
              >
                더 보기 ({allSeasons.length - INITIAL_SHOW}개)
                <span className="material-symbols-outlined">expand_more</span>
              </button>
            </div>
          )}
          {showAll && (
            <div className="flex justify-center mt-8">
              <button
                onClick={() => setShowAll(false)}
                className="font-cozy-hand text-lg text-white/70 hover:text-white transition-colors flex items-center gap-2"
              >
                접기
                <span className="material-symbols-outlined">expand_less</span>
              </button>
            </div>
          )}
        </section>
      </main>

      <footer className="mt-auto py-8 text-center text-white/30 font-cozy-serif italic text-xs z-10">
        © 2024 The Daily Bubble • All Rights Reserved
      </footer>
    </CozyWoodBackground>
  );
}
