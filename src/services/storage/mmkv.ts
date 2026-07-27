import { MMKV } from "react-native-mmkv";

export const storage = new MMKV({
  id: "fitlife-ai-storage",
});

export const StorageKeys = {
  AUTH_TOKEN: "auth_token",
  USER_ID: "user_id",
  USER_THEME: "user_theme",
  USER_GENDER: "user_gender",
  ONBOARDING_COMPLETED: "onboarding_completed",
  ONBOARDING_DATA: "onboarding_data",
  LAST_SYNC_TIMESTAMP: "last_sync_timestamp",
  DAILY_LOG_CACHE: "daily_log_cache",
  WORKOUT_CACHE: "workout_cache",
  EXERCISE_LIBRARY_CACHE: "exercise_library_cache",
  AI_CHAT_DRAFTS: "ai_chat_drafts",
  HYDRATION_LOG: "hydration_log",
  CYCLE_CACHE: "cycle_cache",
  BODY_METRICS_CACHE: "body_metrics_cache",
  WEARABLE_BUFFER: "wearable_data_buffer",
  APP_VERSION: "app_version",
  MIGRATION_VERSION: "migration_version",
  UNITS: "units",
  THEME_MODE: "theme_mode",
} as const;

export function getString(key: string): string | undefined {
  return storage.getString(key);
}

export function setString(key: string, value: string): void {
  storage.set(key, value);
}

export function getNumber(key: string): number | undefined {
  return storage.getNumber(key);
}

export function setNumber(key: string, value: number): void {
  storage.set(key, value);
}

export function getBoolean(key: string): boolean | undefined {
  return storage.getBoolean(key);
}

export function setBoolean(key: string, value: boolean): void {
  storage.set(key, value);
}

export function getObject<T>(key: string): T | undefined {
  const json = storage.getString(key);
  if (!json) return undefined;
  try {
    return JSON.parse(json) as T;
  } catch {
    return undefined;
  }
}

export function setObject<T>(key: string, value: T): void {
  storage.set(key, JSON.stringify(value));
}

export function remove(key: string): void {
  storage.delete(key);
}

export function clearAll(): void {
  storage.clearAll();
}

export function getAllKeys(): string[] {
  return storage.getAllKeys();
}
