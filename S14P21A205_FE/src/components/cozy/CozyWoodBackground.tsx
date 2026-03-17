import { createContext, useContext, useState } from "react";

type BgMode = "wood" | "bubble";

const BgModeContext = createContext<{ mode: BgMode; setMode: (m: BgMode) => void }>({
  mode: "wood",
  setMode: () => {},
});

export function useCozyBgMode() {
  return useContext(BgModeContext);
}

function BgToggle() {
  const { mode, setMode } = useCozyBgMode();
  return (
    <div className="fixed bottom-6 right-6 z-[100]">
      <button
        onClick={() => setMode(mode === "wood" ? "bubble" : "wood")}
        className="w-12 h-12 bg-white rounded-full shadow-lg border border-gray-200 flex items-center justify-center hover:scale-110 transition-transform"
        title={mode === "wood" ? "버블 배경으로" : "나무 배경으로"}
      >
        <span className="material-symbols-outlined text-gray-600">
          {mode === "wood" ? "bubble_chart" : "forest"}
        </span>
      </button>
    </div>
  );
}

function WoodBg({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen w-full relative font-cozy-display text-cozy-ink overflow-x-hidden bg-[#3c281e]">
      {/* CSS-only wood texture instead of external image */}
      <div className="fixed inset-0 z-0 pointer-events-none" style={{
        backgroundColor: "#6B4226",
        backgroundImage: `
          repeating-linear-gradient(
            90deg,
            rgba(255,255,255,0.03) 0px,
            rgba(255,255,255,0.03) 1px,
            transparent 1px,
            transparent 12px
          ),
          repeating-linear-gradient(
            0deg,
            rgba(0,0,0,0.05) 0px,
            rgba(0,0,0,0.05) 1px,
            transparent 1px,
            transparent 80px
          ),
          linear-gradient(180deg, #7B5B3A 0%, #6B4226 30%, #5A3520 60%, #6B4226 100%)
        `,
      }} />
      {/* Warm wood grain overlay */}
      <div className="fixed inset-0 z-0 pointer-events-none opacity-30" style={{
        backgroundImage: `
          repeating-linear-gradient(
            2deg,
            transparent,
            transparent 40px,
            rgba(139,94,60,0.15) 40px,
            rgba(139,94,60,0.15) 42px
          )
        `,
      }} />
      {/* Lighting */}
      <div className="fixed inset-0 z-0 pointer-events-none"
        style={{ background: "radial-gradient(ellipse at 30% 20%, rgba(255,220,180,0.15) 0%, transparent 60%)" }}
      />
      <div className="fixed top-0 right-0 w-1/2 h-full bg-gradient-to-l from-black/15 to-transparent pointer-events-none z-0" />
      {children}
    </div>
  );
}

function BubbleBg({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen w-full relative font-cozy-display text-cozy-ink overflow-x-hidden bg-[#F9F6F1]">
      {/* Warm neutral gradient — matches paper/cream cards */}
      <div className="fixed inset-0 bg-gradient-to-br from-[#F9F6F1] via-[#F3EDE4] to-[#EDE5D8] z-0 pointer-events-none" />
      <style>{`
        @keyframes cozy-b { 0%, 100% { transform: translateY(0) scale(1); } 50% { transform: translateY(-25px) scale(1.03); } }
        @keyframes cozy-br { 0%, 100% { transform: translateY(0) scale(1); } 50% { transform: translateY(15px) scale(0.97); } }
      `}</style>
      {/* Soft sage/dusty-rose bubbles */}
      <div className="fixed top-[-8%] left-[-5%] w-[450px] h-[450px] rounded-full z-0 pointer-events-none opacity-30"
        style={{ background: "radial-gradient(circle at 30% 30%, rgba(168,191,169,0.25), rgba(168,191,169,0.08) 60%, transparent 80%)", animation: "cozy-b 12s ease-in-out infinite" }}
      />
      <div className="fixed bottom-[-5%] right-[-5%] w-[350px] h-[350px] rounded-full z-0 pointer-events-none opacity-25"
        style={{ background: "radial-gradient(circle at 60% 40%, rgba(217,169,181,0.2), rgba(217,169,181,0.06) 60%, transparent 80%)", animation: "cozy-br 10s ease-in-out infinite" }}
      />
      <div className="fixed top-[35%] right-[12%] w-[180px] h-[180px] rounded-full z-0 pointer-events-none opacity-20"
        style={{ background: "radial-gradient(circle at 40% 40%, rgba(168,191,169,0.18), transparent 70%)", animation: "cozy-b 8s ease-in-out infinite 2s" }}
      />
      {/* Small floating circles */}
      {[
        { size: 50, top: "18%", left: "22%", delay: "0s" },
        { size: 35, top: "50%", left: "78%", delay: "3s" },
        { size: 65, top: "72%", left: "45%", delay: "5s" },
        { size: 25, top: "28%", left: "68%", delay: "2s" },
      ].map((b, i) => (
        <div key={i} className="fixed rounded-full z-0 pointer-events-none border border-white/40"
          style={{
            width: b.size, height: b.size, top: b.top, left: b.left,
            background: `radial-gradient(circle at 30% 30%, rgba(255,255,255,0.7) 0%, rgba(168,191,169,0.08) 40%, transparent 70%)`,
            boxShadow: "inset 0 0 15px rgba(255,255,255,0.4), 0 4px 15px rgba(0,0,0,0.02)",
            animation: `cozy-b ${6 + i * 1.5}s ease-in-out infinite ${b.delay}`,
          }}
        />
      ))}
      {/* Paper noise */}
      <div className="fixed inset-0 z-0 pointer-events-none opacity-[0.04]"
        style={{ backgroundImage: "url('https://www.transparenttextures.com/patterns/cream-paper.png')" }}
      />
      {/* Vignette */}
      <div className="fixed inset-0 z-0 pointer-events-none"
        style={{ background: "radial-gradient(ellipse at center, transparent 50%, rgba(220,210,195,0.25) 100%)" }}
      />
      {children}
    </div>
  );
}

export default function CozyWoodBackground({ children }: { children: React.ReactNode }) {
  const [mode, setMode] = useState<BgMode>("wood");

  return (
    <BgModeContext.Provider value={{ mode, setMode }}>
      {mode === "wood" ? (
        <WoodBg>
          {children}
          <BgToggle />
        </WoodBg>
      ) : (
        <BubbleBg>
          {children}
          <BgToggle />
        </BubbleBg>
      )}
    </BgModeContext.Provider>
  );
}
