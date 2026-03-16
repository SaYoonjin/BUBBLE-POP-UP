import CozyWoodBackground from "../components/cozy/CozyWoodBackground";

export default function CozyLoginPage() {
  return (
    <CozyWoodBackground>
      {/* Minimal header */}
      <header className="w-full h-16 flex items-center px-6 md:px-12 fixed top-0 left-0 z-50 bg-white/30 backdrop-blur-sm">
        <span className="text-xl font-cozy-serif font-black italic tracking-tight text-white drop-shadow-md">
          The Daily Bubble
        </span>
      </header>

      <main className="relative z-10 flex-1 flex flex-col items-center justify-center min-h-screen w-full px-4">
        {/* Login Card (The Paper) */}
        <div
          className="w-full max-w-[440px] bg-cozy-paper rounded-sm shadow-cozy-paper p-12 flex flex-col items-center text-center relative rotate-1 hover:rotate-0 transition-transform duration-500 border-b border-r border-black/5"
          style={{ backgroundImage: "url('https://www.transparenttextures.com/patterns/cream-paper.png')" }}
        >
          {/* Bubble sticker */}
          <div className="absolute -top-4 -right-4 w-16 h-16 bg-[#f0eadd] rounded-full shadow-md border-4 border-white/80 flex items-center justify-center rotate-12">
            <span className="material-symbols-outlined text-cozy-sage text-3xl">bubble_chart</span>
          </div>
          {/* Paper fold corner */}
          <div className="absolute top-0 right-0 w-0 h-0 border-l-[40px] border-l-transparent border-b-[40px] border-b-[#f4f1ea]" style={{ filter: "drop-shadow(-2px 2px 2px rgba(0,0,0,0.05))" }} />

          <div className="w-20 h-20 rounded-full bg-white/50 border border-cozy-ink/5 flex items-center justify-center text-4xl mb-8 shadow-inner">
            🏪
          </div>

          <h1 className="font-cozy-serif text-4xl font-black text-cozy-ink mb-4 tracking-tight uppercase border-b-2 border-cozy-ink pb-2 w-full">
            Daily Login
          </h1>
          <p className="font-cozy-serif italic text-cozy-ink/60 text-lg mb-10 w-full break-keep leading-snug">
            "Your morning brew of updates &<br />strategic popup management."
          </p>

          <div className="w-full flex flex-col gap-5">
            <button className="w-full h-[58px] bg-white border-b-4 border-r-2 border-gray-200 hover:border-gray-300 hover:translate-y-[1px] text-cozy-ink rounded-sm flex items-center justify-center gap-3 transition-all shadow-sm font-bold text-[15px] relative overflow-hidden">
              <svg className="w-5 h-5" viewBox="0 0 24 24">
                <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4" />
                <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853" />
                <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05" />
                <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335" />
              </svg>
              <span className="font-cozy-serif uppercase tracking-widest">Sign in with Google</span>
            </button>
            <button className="w-full h-[58px] bg-[#f0eadd] border-b-4 border-r-2 border-[#d6cfbe] hover:translate-y-[1px] text-cozy-ink rounded-sm flex items-center justify-center gap-3 transition-all shadow-sm font-bold text-[15px] relative overflow-hidden">
              <div className="absolute left-0 top-0 bottom-0 w-2 bg-cozy-sage/40" />
              <span className="text-cozy-primary font-black font-cozy-serif italic">SSAFY</span>
              <span className="font-cozy-serif uppercase tracking-widest ml-1">Staff Access</span>
            </button>
          </div>

          <div className="mt-12 text-xs font-cozy-hand text-cozy-dusty-rose -rotate-[1deg]">
            By entering, you agree to our{" "}
            <a className="underline" href="#">Terms</a> &{" "}
            <a className="underline" href="#">Privacy Ledger</a>.
          </div>
        </div>
      </main>

      <footer className="fixed bottom-6 w-full text-center z-20">
        <p className="font-cozy-hand text-white/40 text-sm tracking-widest uppercase">Property of The Daily Bubble © 1928</p>
      </footer>
    </CozyWoodBackground>
  );
}
