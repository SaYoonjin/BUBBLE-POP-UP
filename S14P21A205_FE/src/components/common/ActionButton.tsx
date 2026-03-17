interface ActionButtonProps {
  icon: string;
  label: string;
  active?: boolean;
  disabled?: boolean;
  onClick?: () => void;
}

export default function ActionButton({
  icon,
  label,
  active = false,
  disabled = false,
  onClick,
}: ActionButtonProps) {
  return (
    <button
      className={`
        flex flex-col items-center gap-1 px-4 py-3 rounded-xl transition-all text-xs font-medium
        ${active
          ? "bg-primary text-white shadow-soft"
          : "bg-card-light text-gray-600 hover:bg-gray-50 border border-gray-100"
        }
        ${disabled ? "opacity-40 cursor-not-allowed" : "cursor-pointer"}
      `}
      disabled={disabled}
      onClick={onClick}
    >
      <span className="material-symbols-outlined text-xl">{icon}</span>
      <span>{label}</span>
    </button>
  );
}
