import { useEffect, useRef } from "react";

interface Particle {
  x: number;
  y: number;
  vx: number;
  vy: number;
  size: number;
  color: string;
  rotation: number;
  rotationSpeed: number;
  opacity: number;
  type: "confetti" | "sparkle";
}

const COLORS = [
  "#EBC86E", "#C0C0C0", "#CD7F32",
  "#A8BFA9", "#D4A5A5",
  "#FFD700", "#FF6B8A", "#64D2FF", "#A78BFA", "#34D399",
];

function rand(a: number, b: number) {
  return a + Math.random() * (b - a);
}

function createParticle(canvasW: number): Particle {
  return {
    x: rand(0, canvasW),
    y: rand(-30, -250),
    vx: rand(-1.2, 1.2),
    vy: rand(1, 3),
    size: rand(5, 10),
    color: COLORS[Math.floor(Math.random() * COLORS.length)],
    rotation: rand(0, 360),
    rotationSpeed: rand(-4, 4),
    opacity: 1,
    type: Math.random() < 0.5 ? "confetti" : "sparkle",
  };
}

export default function Confetti() {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    let animId: number;
    let particles: Particle[] = [];

    const resize = () => {
      canvas.width = canvas.offsetWidth;
      canvas.height = canvas.offsetHeight;
    };
    resize();
    window.addEventListener("resize", resize);

    for (let i = 0; i < 40; i++) {
      particles.push(createParticle(canvas.width));
    }

    const draw = () => {
      ctx.clearRect(0, 0, canvas.width, canvas.height);

      if (particles.length < 35 && Math.random() < 0.15) {
        particles.push(createParticle(canvas.width));
      }

      for (let i = particles.length - 1; i >= 0; i--) {
        const p = particles[i];
        p.x += p.vx;
        p.y += p.vy;
        p.vy += 0.025;
        p.rotation += p.rotationSpeed;

        if (p.y > canvas.height * 0.65) {
          p.opacity -= 0.012;
        }

        if (p.opacity <= 0 || p.y > canvas.height + 30) {
          particles.splice(i, 1);
          continue;
        }

        ctx.save();
        ctx.globalAlpha = p.opacity;
        ctx.translate(p.x, p.y);
        ctx.rotate((p.rotation * Math.PI) / 180);

        if (p.type === "confetti") {
          ctx.fillStyle = p.color;
          ctx.fillRect(-p.size / 2, -p.size / 4, p.size, p.size / 2);
        } else {
          ctx.fillStyle = p.color;
          ctx.beginPath();
          const s = p.size / 2;
          for (let j = 0; j < 4; j++) {
            const angle = (j * Math.PI) / 2;
            ctx.lineTo(Math.cos(angle) * s, Math.sin(angle) * s);
            ctx.lineTo(
              Math.cos(angle + Math.PI / 4) * (s * 0.35),
              Math.sin(angle + Math.PI / 4) * (s * 0.35)
            );
          }
          ctx.closePath();
          ctx.fill();
        }

        ctx.restore();
      }

      animId = requestAnimationFrame(draw);
    };

    animId = requestAnimationFrame(draw);

    return () => {
      cancelAnimationFrame(animId);
      window.removeEventListener("resize", resize);
    };
  }, []);

  return (
    <canvas
      ref={canvasRef}
      className="absolute inset-0 w-full h-full pointer-events-none z-10"
    />
  );
}
