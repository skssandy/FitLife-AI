/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./App.{js,jsx,ts,tsx}",
    "./app/**/*.{js,jsx,ts,tsx}",
    "./src/**/*.{js,jsx,ts,tsx}",
    "./components/**/*.{js,jsx,ts,tsx}",
    "./lib/**/*.{js,jsx,ts,tsx}",
  ],

  presets: [require("nativewind/preset")],

  theme: {
    extend: {
      colors: {
        male: {
          "bg-primary": "#0A0A0A",
          "bg-secondary": "#141414",
          "bg-tertiary": "#1E1E1E",
          "bg-hover": "#2A2A2A",
          surface: "#333333",
          "text-primary": "#F5F5F5",
          "text-secondary": "#A0A0A0",
          "text-muted": "#6B6B6B",
          accent: "#0096FF",
          "accent-dim": "#0073CC",
          "accent-glow": "#0096FF33",
          "card-border": "#2A2A2A",
        },
        female: {
          "bg-primary": "#FFF8F6",
          "bg-secondary": "#FFFFFF",
          "bg-tertiary": "#FFF0EC",
          "bg-hover": "#FFE4DE",
          surface: "#F2D7D0",
          "text-primary": "#2D1B14",
          "text-secondary": "#7A5C52",
          "text-muted": "#B89A92",
          accent: "#FF6B6B",
          "accent-dim": "#E05555",
          secondary: "#C4A6E8",
          "secondary-dim": "#A888D4",
          "card-border": "#F2D7D0",
        },
        maleDark: {
          "bg-primary": "#050505",
          "bg-secondary": "#0F0F0F",
          "bg-tertiary": "#1A1A1A",
          surface: "#2A2A2A",
          accent: "#33ADFF",
        },
        femaleDark: {
          "bg-primary": "#1A1118",
          "bg-secondary": "#241C22",
          "bg-tertiary": "#2E242C",
          surface: "#3A2E34",
          accent: "#FF8585",
        },
        success: {
          50: "#F0FDF4",
          100: "#DCFCE7",
          500: "#22C55E",
          900: "#14532D",
        },
        warning: {
          50: "#FFFBEB",
          100: "#FEF3C7",
          500: "#F59E0B",
          900: "#78350F",
        },
        error: {
          50: "#FEF2F2",
          100: "#FEE2E2",
          500: "#EF4444",
          900: "#7F1D1D",
        },
        info: {
          50: "#EFF6FF",
          100: "#DBEAFE",
          500: "#3B82F6",
          900: "#1E3A5F",
        },
        marker: {
          low: "#3B82F6",
          optimal: "#22C55E",
          high: "#EF4444",
          critical: "#DC2626",
        },
        phase: {
          menstrual: "#E74C3C",
          menstrualSecondary: "#FADBD8",
          follicular: "#2ECC71",
          follicularSecondary: "#D5F5E3",
          ovulation: "#F39C12",
          ovulationSecondary: "#FDEBD0",
          luteal: "#9B59B6",
          lutealSecondary: "#EBDEF0",
        },
      },

      screens: {
        xs: "320px",
        sm: "375px",
        md: "414px",
        lg: "480px",
        xl: "768px",
        "2xl": "1024px",
      },

      spacing: {
        "safe-top": "env(safe-area-inset-top)",
        "safe-bottom": "env(safe-area-inset-bottom)",
        18: "72px",
        88: "352px",
        128: "512px",
      },

      fontFamily: {
        sans: [
          "Inter-Regular",
          "Inter-Medium",
          "Inter-SemiBold",
          "Inter-Bold",
        ],
        mono: ["JetBrainsMono-Regular"],
        display: ["Inter-Bold"],
        heading: ["Inter-SemiBold"],
        body: ["Inter-Regular"],
        caption: ["Inter-Regular"],
      },

      fontSize: {
        "stat-number": ["2.25rem", { lineHeight: "2.5rem", fontWeight: "700" }],
        timer: ["3rem", { lineHeight: "3.5rem", fontWeight: "700" }],
      },

      borderRadius: {
        card: "16px",
        button: "12px",
        input: "12px",
        pill: "9999px",
      },

      boxShadow: {
        glow: "0 0 20px rgba(0, 150, 255, 0.15)",
        "glow-male": "0 0 20px rgba(0, 150, 255, 0.15)",
        "glow-female": "0 0 20px rgba(255, 107, 107, 0.15)",
      },

      animation: {
        "pulse-slow": "pulse 3s cubic-bezier(0.4, 0, 0.6, 1) infinite",
        shimmer: "shimmer 1.5s infinite",
        "slide-up": "slideUp 300ms ease-out",
        "slide-down": "slideDown 250ms ease-out",
        "fade-in": "fadeIn 200ms ease-out",
        "scale-in": "scaleIn 200ms ease-out",
      },

      keyframes: {
        shimmer: {
          "0%": { transform: [{ translateX: "-100%" }] },
          "100%": { transform: [{ translateX: "100%" }] },
        },
        slideUp: {
          "0%": { transform: [{ translateY: "20px" }], opacity: "0" },
          "100%": { transform: [{ translateY: "0" }], opacity: "1" },
        },
        slideDown: {
          "0%": { transform: [{ translateY: "-20px" }], opacity: "0" },
          "100%": { transform: [{ translateY: "0" }], opacity: "1" },
        },
        fadeIn: {
          "0%": { opacity: "0" },
          "100%": { opacity: "1" },
        },
        scaleIn: {
          "0%": { transform: [{ scale: "0.9" }], opacity: "0" },
          "100%": { transform: [{ scale: "1" }], opacity: "1" },
        },
      },

      transitionDuration: {
        fast: "150ms",
        normal: "250ms",
        slow: "400ms",
      },
    },
  },

  plugins: [],
};
