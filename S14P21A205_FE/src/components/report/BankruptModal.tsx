import { Link } from "react-router-dom";

interface BankruptModalProps {
  onClose: () => void;
  isLastDay?: boolean;
}

export default function BankruptModal({ onClose, isLastDay = false }: BankruptModalProps) {
  return (
    <div className="fixed inset-0 z-[60] flex items-center justify-center bg-black/40 backdrop-blur-sm px-4">
      <div className="bg-white rounded-[24px] shadow-2xl p-8 max-w-md w-full flex flex-col items-center text-center">
        <div className="text-6xl mb-4">😭</div>
        <h2 className="text-2xl font-bold text-slate-800 mb-2">아쉽습니다! 파산했습니다.</h2>
        <p className="text-slate-500 mb-8 leading-relaxed">서울의 트렌드를 다시 분석하여 다시 도전해보세요.</p>
        <div className="w-full flex flex-col gap-3">
          {isLastDay && (
            <Link
              to="/ranking"
              className="w-full py-3 px-4 rounded-xl border-2 border-primary text-primary font-bold hover:bg-primary/5 transition-colors text-center"
            >
              랭킹 확인하기
            </Link>
          )}
          <button
            onClick={onClose}
            className="w-full py-3 px-4 rounded-xl bg-primary text-white font-bold hover:bg-primary-dark transition-colors"
          >
            다시 시작하기
          </button>
        </div>
      </div>
    </div>
  );
}
