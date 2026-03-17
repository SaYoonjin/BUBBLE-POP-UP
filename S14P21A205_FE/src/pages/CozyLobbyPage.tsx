import { Link } from "react-router-dom";
import CozyWoodBackground from "../components/cozy/CozyWoodBackground";
import CozyGuestHeader from "../components/cozy/CozyGuestHeader";

export default function CozyLobbyPage() {
  return (
    <CozyWoodBackground>
      <CozyGuestHeader />

      <main className="relative z-10 max-w-[1400px] mx-auto px-6 pt-4 pb-20 grid grid-cols-1 lg:grid-cols-12 gap-12 items-center">
        {/* Left: Newspaper */}
        <div className="lg:col-span-7 flex flex-col gap-10">
          <div
            className="bg-cozy-paper shadow-cozy-paper rounded-sm p-10 relative -rotate-[1deg] hover:rotate-0 transition-transform duration-500"
            style={{ backgroundImage: "url('https://www.transparenttextures.com/patterns/cream-paper.png')" }}
          >
            <div className="border-b-4 border-cozy-ink mb-8 pb-4 flex justify-between items-end">
              <div>
                <h2 className="font-cozy-serif text-6xl font-black uppercase tracking-tight">버블버블</h2>
                <p className="font-cozy-serif italic text-cozy-ink/60 mt-1">
                  SEOUL EDITION • VOL. 03 •{" "}
                  <span className="text-cozy-primary font-bold not-italic text-xs tracking-widest">SEASON 3 OPEN</span>
                </p>
              </div>
              <div className="font-cozy-hand text-cozy-sage text-2xl -rotate-[5deg] mb-2">HOT!</div>
            </div>

            <div className="grid md:grid-cols-2 gap-8">
              <div className="flex flex-col gap-5">
                <h3 className="font-cozy-serif text-3xl font-bold leading-tight">
                  가장 트렌디한<br />서울의 거리를 선점하라
                </h3>
                <p className="text-cozy-ink/70 leading-relaxed text-sm">
                  성수동의 팝업스토어부터 홍대의 버스킹 거리까지.
                  나만의 전략으로 최적의 입지를 선점하고
                  최고의 수익을 달성하는 리얼 에스테이트 시뮬레이션.
                </p>
                <div className="border-t border-dashed border-cozy-ink/20 pt-4">
                  <p className="font-cozy-hand text-cozy-primary text-xl -rotate-[2deg]">"지금 바로 참여하세요!"</p>
                </div>
              </div>
              <div className="aspect-[4/5] bg-gray-200 border border-cozy-ink/10 overflow-hidden grayscale hover:grayscale-0 transition-all duration-700 shadow-inner flex items-center justify-center">
                <span className="text-9xl opacity-80">🏪</span>
              </div>
            </div>
          </div>

          {/* Feature cards */}
          <div className="flex gap-8 items-start">
            <div className="bg-white/95 shadow-lg p-6 w-64 rotate-2 border border-cozy-ink/5">
              <span className="material-symbols-outlined text-cozy-sage text-3xl mb-3">real_estate_agent</span>
              <h4 className="font-cozy-serif font-bold text-lg mb-2">실시간 랭킹 시스템</h4>
              <p className="text-xs text-cozy-ink/60 leading-tight">수천 명의 플레이어와 경쟁하세요.</p>
            </div>
            <div className="bg-white/95 shadow-lg p-6 w-64 -rotate-3 border border-cozy-ink/5 mt-8">
              <span className="material-symbols-outlined text-cozy-dusty-rose text-3xl mb-3">strategy</span>
              <h4 className="font-cozy-serif font-bold text-lg mb-2">전략적 투자</h4>
              <p className="text-xs text-cozy-ink/60 leading-tight">트렌드에 맞춰 업그레이드 하세요.</p>
            </div>
          </div>
        </div>

        {/* Right: CTA + Pen */}
        <div className="lg:col-span-5 flex flex-col items-center lg:items-end gap-16 relative">
          <div className="flex flex-col gap-6 w-full max-w-sm items-center lg:items-end">
            <h3 className="font-cozy-hand text-white/80 text-2xl -rotate-[5deg] mb-2 mr-4">Join the Bubble!</h3>
            <Link
              to="/cozy/login"
              className="group relative w-full h-20 bg-cozy-primary text-white flex items-center justify-center gap-3 rounded-sm shadow-[inset_0_0_10px_rgba(0,0,0,0.1),2px_2px_5px_rgba(0,0,0,0.2)] rotate-1 hover:rotate-0 hover:-translate-y-1 transition-all"
            >
              <div className="absolute inset-0 border-2 border-dashed border-white/30 m-1" />
              <span className="font-cozy-serif text-2xl font-black uppercase tracking-widest">게임 참여하기</span>
              <span className="material-symbols-outlined group-hover:translate-x-1 transition-transform">arrow_right_alt</span>
            </Link>
            <div className="flex items-center gap-4 mt-4 mr-2">
              <div className="flex -space-x-3">
                <div className="w-10 h-10 rounded-full bg-cozy-sage border-2 border-white shadow-sm" />
                <div className="w-10 h-10 rounded-full bg-cozy-dusty-rose border-2 border-white shadow-sm" />
                <div className="w-10 h-10 rounded-full bg-cozy-ink border-2 border-white shadow-sm flex items-center justify-center text-[10px] text-white">+1k</div>
              </div>
              <p className="text-white/70 font-cozy-serif italic text-sm">1,203명이 테이블에 앉아있습니다.</p>
            </div>
          </div>
          {/* Pen */}
          <div className="absolute -bottom-24 -right-12 w-80 h-4 bg-gradient-to-r from-black to-gray-700 rounded-full shadow-2xl rotate-[135deg] opacity-90 pointer-events-none hidden lg:block">
            <div className="absolute right-0 top-0 h-full w-14 bg-cozy-primary rounded-r-full" />
            <div className="absolute right-14 top-0 h-full w-2 bg-gray-400" />
          </div>
        </div>
      </main>
    </CozyWoodBackground>
  );
}
