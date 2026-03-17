interface CozyActionButtonProps {
  icon: string;
  label: string;
  active?: boolean;
  disabled?: boolean;
  onClick?: () => void;
}

export default function CozyActionButton({ icon, label, active = false, disabled = false, onClick }: CozyActionButtonProps) {
  return (
    <button
      className={`
        flex flex-col items-center gap-2 px-5 py-4 rounded-2xl transition-all font-cozy-display text-xs font-bold
        ${active
          ? "bg-cozy-primary text-white shadow-cozy-tactile border-b-4 border-cozy-primary-dark"
          : "bg-cozy-cream text-cozy-wood-dark shadow-cozy-clay hover:-translate-y-0.5 border-2 border-white"
        }
        ${disabled ? "opacity-40 cursor-not-allowed !shadow-none !translate-y-0" : "cursor-pointer active:translate-y-1 active:shadow-none"}
      `}
      disabled={disabled}
      onClick={onClick}
    >
      <span className="material-symbols-outlined text-2xl">{icon}</span>
      <span className="uppercase tracking-wider">{label}</span>
    </button>
  );
}
