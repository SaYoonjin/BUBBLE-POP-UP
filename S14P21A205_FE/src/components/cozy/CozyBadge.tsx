interface CozyBadgeProps {
  variant?: "green" | "rose" | "gray" | "gold";
  size?: "sm" | "md";
  children: React.ReactNode;
}

const variantStyles: Record<string, string> = {
  green: "bg-cozy-sage-green/20 text-[#556B2F] border border-cozy-sage-green/30",
  rose: "bg-cozy-dusty-rose/20 text-cozy-primary border border-cozy-dusty-rose/30",
  gray: "bg-cozy-warm/50 text-cozy-wood-dark border border-cozy-wood-light",
  gold: "bg-amber-100 text-amber-800 border border-amber-200",
};

const sizeStyles: Record<string, string> = {
  sm: "px-2.5 py-0.5 text-xs",
  md: "px-3 py-1 text-sm",
};

export default function CozyBadge({ variant = "green", size = "sm", children }: CozyBadgeProps) {
  return (
    <span className={`
      inline-flex items-center font-cozy-display font-bold rounded-full uppercase tracking-wider
      ${variantStyles[variant]} ${sizeStyles[size]}
    `}>
      {children}
    </span>
  );
}
