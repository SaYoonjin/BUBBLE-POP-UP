import type { Config } from "tailwindcss";

const config: Config = {
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      colors: {
        primary: "#A8BFA9",
        "primary-dark": "#8DA98E",
        "accent-rose": "#D4A5A5",
        "background-light": "#FDFDFB",
        "background-dark": "#171b18",
        "card-light": "#FFFFFF",
        "card-dark": "#232624",
      },
      fontFamily: {
        display: ["Spline Sans", "Noto Sans KR", "sans-serif"],
        body: ["Spline Sans", "Noto Sans KR", "sans-serif"],
        mono: ["ui-monospace", "SFMono-Regular", "monospace"],
      },
      boxShadow: {
        soft: "0 2px 8px rgba(0,0,0,0.02), 0 1px 2px rgba(0,0,0,0.02)",
        premium: "0 10px 40px rgba(0,0,0,0.04)",
      },
      borderRadius: {
        lg: "1rem",
        xl: "1.5rem",
        "2xl": "2rem",
      },
    },
  },
  plugins: [],
};

export default config;
