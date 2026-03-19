interface BadgeProps {
  variant?: "green" | "rose" | "gray" | "gold";
  size?: "sm" | "md";
  children: React.ReactNode;
}

const variantStyles: Record<string, string> = {
  green: "bg-primary/15 text-primary-dark",
  rose: "bg-accent-rose/15 text-accent-rose",
  gray: "bg-gray-100 text-gray-600",
  gold: "bg-amber-100 text-amber-700",
};

const sizeStyles: Record<string, string> = {
  sm: "px-2 py-0.5 text-xs",
  md: "px-3 py-1 text-sm",
};

export default function Badge({
  variant = "green",
  size = "sm",
  children,
}: BadgeProps) {
  return (
    <span
      className={`
        inline-flex items-center font-medium rounded-full
        ${variantStyles[variant]}
        ${sizeStyles[size]}
      `}
    >
      {children}
    </span>
  );
}
