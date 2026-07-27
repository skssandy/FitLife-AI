export type SyncStatus =
  | "synced"
  | "pending"
  | "conflict"
  | "local_only"
  | "remote_only";

export interface SyncableRecord {
  id: string;
  sync_status: SyncStatus;
  last_synced_at: string | null;
  updated_at: string;
}

export interface SyncConfig {
  tableName: string;
  conflictStrategy: "local_wins" | "server_wins" | "newest_wins" | "merge";
}

export interface SyncResult {
  pushed: number;
  pulled: number;
  conflicts: number;
  errors: string[];
}

export const TABLE_SYNC_CONFIGS: Record<string, SyncConfig> = {
  local_profiles: { tableName: "profiles", conflictStrategy: "server_wins" },
  local_meals: { tableName: "daily_logs", conflictStrategy: "merge" },
  local_workout_logs: { tableName: "workout_sessions", conflictStrategy: "merge" },
  local_set_logs: { tableName: "workout_sessions", conflictStrategy: "merge" },
  local_water_logs: { tableName: "daily_logs", conflictStrategy: "merge" },
  local_body_metrics: { tableName: "body_metrics", conflictStrategy: "newest_wins" },
  local_cycle_entries: { tableName: "cycle_logs", conflictStrategy: "merge" },
};
