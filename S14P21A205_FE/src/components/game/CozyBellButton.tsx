interface CozyBellButtonProps {
  onClick?: () => void;
  label?: string;
}

export default function CozyBellButton({ onClick, label = "벨을 눌러 영업을 시작하세요!" }: CozyBellButtonProps) {
  return (
    <div className="flex flex-col items-center gap-2">
      <button
        onClick={onClick}
        className="group relative size-28 rounded-full flex items-center justify-center outline-none transition-transform active:scale-95 drop-shadow-[0_10px_20px_rgba(0,0,0,0.25)]"
      >
        {/* Base plate */}
        <div className="absolute bottom-0 w-28 h-10 bg-gray-300 rounded-[50%] shadow-xl z-0 border border-gray-400" />
        {/* Bell dome */}
        <div className="absolute bottom-3 w-24 h-20 bg-gradient-to-br from-[#FFD700] via-[#FDB931] to-[#D4AF37] rounded-t-full rounded-b-[20px] shadow-[inset_-4px_-4px_10px_rgba(0,0,0,0.2),inset_4px_4px_10px_rgba(255,255,255,0.6)] z-10 border-b-4 border-[#B8860B] group-hover:brightness-110 transition-all flex flex-col items-center justify-center">
          {/* Shine spot */}
          <div className="w-3 h-3 rounded-full bg-[#fff9c4] absolute top-5 right-6 blur-[1px] opacity-60" />
          <span className="text-[#5D4037] font-black text-base tracking-wider mt-3">READY!</span>
        </div>
        {/* Top button */}
        <div className="absolute -top-1 w-6 h-6 bg-gray-200 rounded-full border border-gray-300 shadow-sm z-0 group-active:top-1 transition-all" />
      </button>
      <p className="text-center text-cozy-wood-dark text-sm font-medium">{label}</p>
    </div>
  );
}
