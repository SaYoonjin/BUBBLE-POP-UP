interface BankruptWarningProps {
  onRetry?: () => void;
}

export default function BankruptWarning({ onRetry }: BankruptWarningProps) {
  return (
    <div className="bg-white rounded-[20px] shadow-soft p-6 border-l-4 border-accent-rose">
      <div className="flex items-start gap-4">
        <div className="bg-rose-50 text-rose-400 p-2 rounded-lg shrink-0">
          <span className="material-symbols-outlined">warning</span>
        </div>
        <div className="flex-1">
          <h4 className="font-bold mb-1">파산했지만 다시 도전할 수 있습니다!</h4>
          <p className="text-sm text-gray-500 mb-4">아직 포기하지 마세요. 새로운 전략으로 다시 시작해보세요.</p>
          <button
            onClick={onRetry}
            className="px-4 py-2 bg-primary hover:bg-primary-dark text-white text-sm font-bold rounded-lg transition-colors flex items-center gap-1 shadow-sm"
          >
            다시 도전하기
            <span className="material-symbols-outlined text-[16px]">refresh</span>
          </button>
        </div>
      </div>
    </div>
  );
}
