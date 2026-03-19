import GuestHeader from "../components/common/GuestHeader";
import FloatingBubbles from "../components/common/FloatingBubbles";

export default function LoginPage() {
  const startLogin = (provider: "google" | "ssafy") => {
    const query = new URLSearchParams({
      provider,
      redirect: "/auth/callback",
    });
    window.location.href = `/api/auth/login?${query.toString()}`;
  };

  return (
    <div className="relative min-h-screen w-full flex flex-col bg-[#FDFDFB] text-slate-900 overflow-hidden font-display">
      <FloatingBubbles />
      <GuestHeader />

      <main className="flex-1 flex flex-col items-center justify-center w-full px-4 z-10 pt-20">
        <div className="w-full max-w-[420px] bg-white rounded-[24px] shadow-soft p-10 flex flex-col items-center text-center">
          <div className="w-16 h-16 rounded-full bg-gray-50 flex items-center justify-center text-3xl mb-6 shadow-sm">🏪</div>
          <h1 className="text-2xl font-bold mb-3 tracking-tight">버블버블 시작하기</h1>
          <p className="text-gray-500 text-[15px] leading-relaxed mb-10 w-[90%] break-keep">
            나만의 팝업스토어를 성공시키는<br />최고의 경영 전략 시뮬레이션
          </p>
          <div className="w-full flex flex-col gap-3">
            <button
              type="button"
              onClick={() => startLogin("google")}
              className="w-full h-[52px] bg-primary hover:bg-primary-dark text-white rounded-[12px] flex items-center justify-center gap-3 transition-colors shadow-sm hover:shadow font-medium text-[15px]"
            >
              <svg className="w-5 h-5 bg-white rounded-full p-0.5" viewBox="0 0 24 24">
                <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4" />
                <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853" />
                <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05" />
                <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335" />
              </svg>
              Google로 계속하기
            </button>
            <button
              type="button"
              onClick={() => startLogin("ssafy")}
              className="w-full h-[52px] bg-white border border-gray-200 hover:bg-gray-50 text-slate-900 rounded-[12px] flex items-center justify-center gap-3 transition-colors font-medium text-[15px]"
            >
              <span className="text-blue-500 font-bold">SSAFY</span>
              SSAFY 계정으로 로그인
            </button>
          </div>
          <div className="mt-8 text-xs text-gray-400">
            로그인 시 <a className="underline hover:text-slate-900 decoration-gray-300 underline-offset-2" href="#">이용약관</a> 및{" "}
            <a className="underline hover:text-slate-900 decoration-gray-300 underline-offset-2" href="#">개인정보처리방침</a>에 동의하게 됩니다.
          </div>
        </div>
      </main>
    </div>
  );
}
