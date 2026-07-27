export type Gender = "male" | "female" | "other";
export type ThemeMode = "light" | "dark" | "system";

export interface ThemeColors {
  bgPrimary: string;
  bgSecondary: string;
  bgTertiary: string;
  bgHover: string;
  surface: string;
  textPrimary: string;
  textSecondary: string;
  textMuted: string;
  accent: string;
  accentDim: string;
  cardBorder: string;
  secondary?: string;
  secondaryDim?: string;
}

export interface Theme {
  gender: Gender;
  isDark: boolean;
  colors: ThemeColors;
}

const maleLight: ThemeColors = {
  bgPrimary: "#0A0A0A",
  bgSecondary: "#141414",
  bgTertiary: "#1E1E1E",
  bgHover: "#2A2A2A",
  surface: "#333333",
  textPrimary: "#F5F5F5",
  textSecondary: "#A0A0A0",
  textMuted: "#6B6B6B",
  accent: "#0096FF",
  accentDim: "#0073CC",
  cardBorder: "#2A2A2A",
};

const maleDark: ThemeColors = {
  bgPrimary: "#050505",
  bgSecondary: "#0F0F0F",
  bgTertiary: "#1A1A1A",
  bgHover: "#252525",
  surface: "#2A2A2A",
  textPrimary: "#F5F5F5",
  textSecondary: "#A0A0A0",
  textMuted: "#6B6B6B",
  accent: "#33ADFF",
  accentDim: "#0073CC",
  cardBorder: "#2A2A2A",
};

const femaleLight: ThemeColors = {
  bgPrimary: "#FFF8F6",
  bgSecondary: "#FFFFFF",
  bgTertiary: "#FFF0EC",
  bgHover: "#FFE4DE",
  surface: "#F2D7D0",
  textPrimary: "#2D1B14",
  textSecondary: "#7A5C52",
  textMuted: "#B89A92",
  accent: "#FF6B6B",
  accentDim: "#E05555",
  cardBorder: "#F2D7D0",
  secondary: "#C4A6E8",
  secondaryDim: "#A888D4",
};

const femaleDark: ThemeColors = {
  bgPrimary: "#1A1118",
  bgSecondary: "#241C22",
  bgTertiary: "#2E242C",
  bgHover: "#382C36",
  surface: "#3A2E34",
  textPrimary: "#F5F0F3",
  textSecondary: "#C4A6B8",
  textMuted: "#7A5C6A",
  accent: "#FF8585",
  accentDim: "#E05555",
  cardBorder: "#3A2E34",
  secondary: "#D4B8F0",
  secondaryDim: "#A888D4",
};

export const getTheme = (gender: Gender, isDark: boolean): Theme => {
  let colors: ThemeColors;

  switch (gender) {
    case "female":
      colors = isDark ? femaleDark : femaleLight;
      break;
    case "male":
    default:
      colors = isDark ? maleDark : maleLight;
      break;
  }

  return { gender, isDark, colors };
};
