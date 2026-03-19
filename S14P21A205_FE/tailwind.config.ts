import type { Config } from "tailwindcss";

const config: Config = {
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      colors: {
        // Modern
        primary: "#A8BFA9",
        "primary-dark": "#8DA98E",
        "primary-light": "#d3e0d4",
        "accent-rose": "#D4A5A5",
        "rose-soft": "#e6a5a5",
        "rose-dark": "#d18a8a",
        "background-light": "#FDFDFB",
        "background-dark": "#171b18",
        "card-light": "#FFFFFF",
        "card-dark": "#232624",
        // Cozy
        "cozy-primary": "#f4257b",
        "cozy-primary-dark": "#d01663",
        "cozy-sage": "#A8BFA9",
        "cozy-dusty-rose": "#D9A9B5",
        "cozy-paper": "#fdfbf7",
        "cozy-wood-light": "#e4d4c5",
        "cozy-wood-dark": "#8b5e3c",
        "cozy-ink": "#2c2c2c",
        "cozy-cream": "#F3F1EC",
        "cozy-warm": "#E6D8C3",
        "cozy-sage-green": "#8BA888",
      },
      fontFamily: {
        display: ["Spline Sans", "Noto Sans KR", "sans-serif"],
        body: ["Spline Sans", "Noto Sans KR", "sans-serif"],
        mono: ["ui-monospace", "SFMono-Regular", "monospace"],
        countdown: ["Space Grotesk", "sans-serif"],
        // Cozy
        "cozy-display": ["Be Vietnam Pro", "sans-serif"],
        "cozy-serif": ["Playfair Display", "serif"],
        "cozy-hand": ["Caveat", "cursive"],
      },
      boxShadow: {
        soft: "0 2px 8px rgba(0,0,0,0.02), 0 1px 2px rgba(0,0,0,0.02)",
        premium: "0 10px 40px rgba(0,0,0,0.04)",
        // Cozy
        "cozy-paper": "0 4px 20px -2px rgba(0,0,0,0.1), 0 10px 40px -5px rgba(0,0,0,0.05)",
        "cozy-float": "0 20px 25px -5px rgba(0,0,0,0.2), 0 10px 10px -5px rgba(0,0,0,0.1)",
        "cozy-clay": "8px 8px 16px #d1c4b3, -8px -8px 16px #ffffff",
        "cozy-tactile": "0 6px 0px 0px rgba(139,94,60,0.4), 0 10px 10px rgba(0,0,0,0.1)",
      },
      borderRadius: {
        lg: "1rem",
        xl: "1.5rem",
        "2xl": "2rem",
      },
      keyframes: {
        fadeUp: {
          "0%": { opacity: "0", transform: "translateY(24px)" },
          "100%": { opacity: "1", transform: "translateY(0)" },
        },
        slideInLeft: {
          "0%": { opacity: "0", transform: "translateX(-20px)" },
          "100%": { opacity: "1", transform: "translateX(0)" },
        },
        scaleIn: {
          "0%": { opacity: "0", transform: "scale(0.9)" },
          "100%": { opacity: "1", transform: "scale(1)" },
        },
      },
      animation: {
        "fade-up": "fadeUp 0.5s ease-out both",
        "slide-in-left": "slideInLeft 0.4s ease-out both",
        "scale-in": "scaleIn 0.4s ease-out both",
      },
    },
  },
  plugins: [],
};

export default config;
