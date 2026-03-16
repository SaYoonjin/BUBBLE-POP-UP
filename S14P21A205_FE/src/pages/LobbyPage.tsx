import { Link } from "react-router-dom";
import GuestHeader from "../components/common/GuestHeader";
import FloatingBubbles from "../components/common/FloatingBubbles";

const landingBubbles = [
  { size: "w-96 h-96", position: "top-[-10%] left-[-10%]", opacity: "opacity-40", delay: "0s", variant: "glass" as const },
  { size: "w-64 h-64", position: "bottom-10 right-[-5%]", opacity: "opacity-30", delay: "2s", variant: "glass" as const },
  { size: "w-32 h-32", position: "top-1/4 right-[20%]", opacity: "opacity-60", delay: "4s", variant: "solid" as const },
  { size: "w-20 h-20", position: "bottom-1/3 left-[10%]", opacity: "opacity-20", delay: "1s", variant: "glass" as const },
];

export default function LobbyPage() {
  return (
    <div className="relative min-h-screen w-full flex flex-col bg-gradient-to-br from-[#F4F7F4] to-[#FFFBF2] text-slate-900 overflow-hidden font-display">
      <FloatingBubbles bubbles={landingBubbles} />
      <GuestHeader />

      <main className="flex-1 flex items-center justify-center w-full px-6 md:px-16 z-10 pt-20">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 lg:gap-20 w-full max-w-[1280px] mx-auto items-center">
          {/* Hero */}
          <div className="lg:col-span-7 flex flex-col gap-8 order-2 lg:order-1">
            <div className="relative w-full aspect-[16/10] bg-white rounded-[32px] shadow-soft overflow-hidden group border border-white/40">
              <div className="h-3/4 bg-gradient-to-br from-[#EAE5DC] to-[#E0D8CC] flex items-center justify-center">
                <div className="text-center">
                  <div className="text-8xl mb-4">🏪</div>
                  <p className="text-[#5c5446] font-bold text-lg">Your Popup Store</p>
                </div>
              </div>
              <div className="h-1/4 bg-white p-8 flex items-center justify-between">
                <div>
                  <h2 className="text-2xl md:text-3xl font-bold leading-tight tracking-tight">가장 트렌디한 서울의 거리</h2>
                  <p className="text-gray-500 mt-2 text-base">성수부터 홍대까지, 최적의 입지를 선점하세요.</p>
                </div>
                <div className="flex gap-2 self-end mb-1">
                  <div className="w-2 h-2 bg-gray-200 rounded-full" />
                  <div className="w-8 h-2 bg-primary rounded-full" />
                  <div className="w-2 h-2 bg-gray-200 rounded-full" />
                </div>
              </div>
            </div>
          </div>

          {/* CTA */}
          <div className="lg:col-span-5 flex flex-col justify-center order-1 lg:order-2 mb-8 lg:mb-0">
            <div className="bg-white/90 backdrop-blur-sm rounded-[32px] shadow-xl p-10 lg:p-12 border border-white flex flex-col gap-8 relative overflow-hidden">
              <div className="absolute top-0 right-0 w-32 h-32 bg-primary/10 rounded-bl-full opacity-50 -mr-8 -mt-8" />
              <div className="space-y-4 z-10">
                <span className="text-primary font-bold tracking-wider text-sm uppercase">Season 3 Open</span>
                <h2 className="text-4xl md:text-5xl font-bold leading-[1.15]">지금 바로<br />시작하세요!</h2>
                <p className="text-gray-500 text-lg font-light leading-relaxed">
                  나만의 전략으로 최고의 수익을 달성하고<br />실시간 랭킹에 도전해보세요.
                </p>
              </div>
              <div className="space-y-6 z-10 pt-4">
                <Link
                  to="/login"
                  className="w-full h-[72px] bg-primary hover:bg-primary-dark text-white text-xl font-bold rounded-2xl shadow-lg hover:shadow-xl transition-all transform hover:-translate-y-1 flex items-center justify-center gap-3 group"
                >
                  게임 참여하러 가기
                  <span className="material-symbols-outlined group-hover:translate-x-1 transition-transform">arrow_forward</span>
                </Link>
                <div className="flex items-center justify-center gap-2 text-gray-500">
                  <span className="material-symbols-outlined text-sm">schedule</span>
                  <span className="text-sm font-medium">Season 3 마감까지</span>
                  <span className="font-mono font-bold text-primary ml-1 bg-primary/10 px-2 py-1 rounded text-sm">02:00</span>
                </div>
              </div>
            </div>
            <div className="mt-8 flex items-center justify-center gap-4 opacity-60">
              <div className="flex -space-x-3">
                <div className="w-8 h-8 rounded-full bg-gray-200 border-2 border-white" />
                <div className="w-8 h-8 rounded-full bg-gray-300 border-2 border-white" />
                <div className="w-8 h-8 rounded-full bg-gray-400 border-2 border-white" />
              </div>
              <span className="text-sm text-gray-500 font-medium">1,203명이 참여 중</span>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}
