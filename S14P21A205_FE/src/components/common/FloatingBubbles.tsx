interface BubbleConfig {
  size: string;
  position: string;
  opacity: string;
  delay: string;
  variant?: "glass" | "solid";
}

const defaultBubbles: BubbleConfig[] = [
  { size: "w-64 h-64", position: "top-[15%] left-[10%]", opacity: "opacity-60", delay: "0s", variant: "glass" },
  { size: "w-48 h-48", position: "bottom-[15%] right-[10%]", opacity: "opacity-50", delay: "3s", variant: "glass" },
  { size: "w-32 h-32", position: "top-1/4 right-[25%]", opacity: "opacity-40", delay: "5s", variant: "solid" },
  { size: "w-20 h-20", position: "bottom-1/4 left-[20%]", opacity: "opacity-30", delay: "0s", variant: "glass" },
];

export default function FloatingBubbles({ bubbles = defaultBubbles }: { bubbles?: BubbleConfig[] }) {
  return (
    <>
      <style>{`
        @keyframes bubble-float {
          0% { transform: translateY(0px) rotate(0deg); }
          50% { transform: translateY(-20px) rotate(5deg); }
          100% { transform: translateY(0px) rotate(0deg); }
        }
      `}</style>
      {bubbles.map((b, i) => (
        <div
          key={i}
          className={`absolute rounded-full z-0 ${b.size} ${b.position} ${b.opacity}`}
          style={{
            animation: `bubble-float 8s ease-in-out infinite`,
            animationDelay: b.delay,
            background: b.variant === "solid"
              ? "linear-gradient(135deg, rgba(255,255,255,0.8), rgba(168,191,169,0.2))"
              : "rgba(168,191,169,0.15)",
            border: "1px solid rgba(255,255,255,0.4)",
            boxShadow: b.variant === "solid"
              ? "inset 2px 2px 10px rgba(255,255,255,0.8), 0 10px 20px rgba(0,0,0,0.05)"
              : "0 4px 30px rgba(0,0,0,0.02)",
            backdropFilter: "blur(2px)",
          }}
        />
      ))}
    </>
  );
}
