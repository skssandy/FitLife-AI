import React, { createContext, useContext, useState, useEffect, useCallback, ReactNode } from "react";
import { Gender, Theme, ThemeMode, getTheme } from "../config/theme";
import { getBoolean, setBoolean as mmkvSetBoolean, getString, setString as mmkvSetString } from "../services/storage/mmkv";
import { StorageKeys } from "../services/storage/mmkv";

interface ThemeContextType {
  theme: Theme;
  themeMode: ThemeMode;
  gender: Gender;
  toggleTheme: () => void;
  setThemeMode: (mode: ThemeMode) => void;
  setGender: (gender: Gender) => void;
}

const ThemeContext = createContext<ThemeContextType | undefined>(undefined);

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [gender, setGenderState] = useState<Gender>(() => {
    const saved = getString(StorageKeys.USER_GENDER);
    return (saved as Gender) || "male";
  });

  const [themeMode, setThemeModeState] = useState<ThemeMode>(() => {
    const saved = getString(StorageKeys.THEME_MODE);
    return (saved as ThemeMode) || "system";
  });

  const isDark =
    themeMode === "dark" ||
    (themeMode === "system" && false);

  const theme = getTheme(gender, isDark);

  const toggleTheme = useCallback(() => {
    setThemeModeState((prev) => {
      const next: ThemeMode = prev === "dark" ? "light" : "dark";
      mmkvSetString(StorageKeys.THEME_MODE, next);
      return next;
    });
  }, []);

  const setThemeMode = useCallback((mode: ThemeMode) => {
    setThemeModeState(mode);
    mmkvSetString(StorageKeys.THEME_MODE, mode);
  }, []);

  const setGender = useCallback((g: Gender) => {
    setGenderState(g);
    mmkvSetString(StorageKeys.USER_GENDER, g);
  }, []);

  return (
    <ThemeContext.Provider
      value={{
        theme,
        themeMode,
        gender,
        toggleTheme,
        setThemeMode,
        setGender,
      }}
    >
      {children}
    </ThemeContext.Provider>
  );
}

export function useThemeContext(): ThemeContextType {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error("useThemeContext must be used within a ThemeProvider");
  }
  return context;
}
