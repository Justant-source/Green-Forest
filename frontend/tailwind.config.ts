import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./src/pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/components/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/app/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      colors: {
        forest: {
          50: "#F5F9F0",
          100: "#E8F5E0",
          200: "#C8E6C9",
          300: "#A5D6A7",
          400: "#5BB37F",
          500: "#2D8A4E",
          600: "#1B5E20",
          700: "#145A1E",
          800: "#0D3B13",
          900: "#082A0D",
        },
        accent: {
          DEFAULT: "#4CACDE",
          light: "#7EC8E3",
          dark: "#2E8EB8",
        },
      },
    },
  },
  plugins: [],
};
export default config;
