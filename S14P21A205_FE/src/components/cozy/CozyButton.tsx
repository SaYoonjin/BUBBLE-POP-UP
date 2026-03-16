interface CozyButtonProps {
  variant?: "primary" | "wood" | "ghost" | "danger";
  size?: "sm" | "md" | "lg";
  fullWidth?: boolean;
  disabled?: boolean;
  loading?: boolean;
  onClick?: () => void;
  children: React.ReactNode;
}

const variantStyles: Record<string, string> = {
  primary:
    "bg-cozy-primary text-white hover:bg-cozy-primary-dark shadow-cozy-tactile active:shadow-none active:translate-y-1 border-b-4 border-cozy-primary-dark",
  wood:
    "bg-cozy-sage-green text-white hover:brightness-110 shadow-cozy-tactile active:shadow-none active:translate-y-1 border-b-4 border-cozy-sage-green/80",
  ghost:
    "text-cozy-wood-dark hover:bg-cozy-warm/50 border border-cozy-wood-light",
  danger:
    "bg-red-500 text-white hover:bg-red-600 shadow-cozy-tactile active:shadow-none active:translate-y-1 border-b-4 border-red-700",
};

const sizeStyles: Record<string, string> = {
  sm: "px-4 py-1.5 text-sm rounded-xl",
  md: "px-6 py-2.5 text-base rounded-2xl",
  lg: "px-8 py-4 text-lg rounded-2xl",
};

export default function CozyButton({
  variant = "primary",
  size = "md",
  fullWidth = false,
  disabled = false,
  loading = false,
  onClick,
  children,
}: CozyButtonProps) {
  return (
    <button
      className={`
        font-cozy-display font-bold tracking-wide transition-all duration-200
        inline-flex items-center justify-center gap-2
        ${variantStyles[variant]}
        ${sizeStyles[size]}
        ${fullWidth ? "w-full" : ""}
        ${disabled || loading ? "opacity-50 cursor-not-allowed !shadow-none !translate-y-0" : "cursor-pointer"}
      `}
      disabled={disabled || loading}
      onClick={onClick}
    >
      {loading && (
        <span className="w-4 h-4 border-2 border-current border-t-transparent rounded-full animate-spin" />
      )}
      {children}
    </button>
  );
}
