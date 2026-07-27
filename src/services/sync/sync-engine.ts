import { supabase } from "../supabase/client";
import {
  SyncStatus,
  SyncableRecord,
  SyncConfig,
  SyncResult,
  TABLE_SYNC_CONFIGS,
} from "./types";

export async function pushPendingChanges(
  localTable: string,
  remoteTable: string
): Promise<number> {
  const { data: pendingRecords, error: fetchError } = await supabase
    .from(localTable)
    .select("*")
    .eq("sync_status", "pending");

  if (fetchError || !pendingRecords) return 0;

  let pushed = 0;
  for (const record of pendingRecords) {
    const { id, sync_status, last_synced_at, ...upsertData } = record;
    const { error } = await supabase
      .from(remoteTable)
      .upsert(upsertData, { onConflict: "id" });

    if (!error) {
      await supabase
        .from(localTable)
        .update({
          sync_status: "synced",
          last_synced_at: new Date().toISOString(),
        })
        .eq("id", id);
      pushed++;
    }
  }

  return pushed;
}

export async function pullRemoteChanges(
  remoteTable: string,
  since: string | null
): Promise<any[]> {
  let query = supabase.from(remoteTable).select("*");

  if (since) {
    query = query.gt("updated_at", since);
  }

  const { data, error } = await query;
  if (error || !data) return [];
  return data;
}

export function detectConflicts(
  local: SyncableRecord,
  remote: Record<string, any>
): boolean {
  return (
    local.sync_status === "pending" &&
    new Date(remote.updated_at) > new Date(local.last_synced_at || 0)
  );
}

export function resolveConflict(
  local: Record<string, any>,
  remote: Record<string, any>,
  strategy: SyncConfig["conflictStrategy"]
): Record<string, any> {
  switch (strategy) {
    case "local_wins":
      return local;
    case "server_wins":
      return remote;
    case "newest_wins":
      return new Date(local.updated_at) > new Date(remote.updated_at)
        ? local
        : remote;
    case "merge":
      return { ...remote, ...local, updated_at: new Date().toISOString() };
    default:
      return remote;
  }
}

export async function syncTable(localTable: string): Promise<SyncResult> {
  const config = TABLE_SYNC_CONFIGS[localTable];
  if (!config) {
    return { pushed: 0, pulled: 0, conflicts: 0, errors: [`No config for ${localTable}`] };
  }

  const result: SyncResult = {
    pushed: 0,
    pulled: 0,
    conflicts: 0,
    errors: [],
  };

  try {
    result.pushed = await pushPendingChanges(localTable, config.tableName);
  } catch (e) {
    result.errors.push(`Push failed for ${localTable}: ${e}`);
  }

  try {
    const lastSync = new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString();
    const remoteRecords = await pullRemoteChanges(config.tableName, lastSync);
    result.pulled = remoteRecords.length;
  } catch (e) {
    result.errors.push(`Pull failed for ${localTable}: ${e}`);
  }

  return result;
}

export async function syncAll(): Promise<SyncResult> {
  const result: SyncResult = {
    pushed: 0,
    pulled: 0,
    conflicts: 0,
    errors: [],
  };

  for (const localTable of Object.keys(TABLE_SYNC_CONFIGS)) {
    const tableResult = await syncTable(localTable);
    result.pushed += tableResult.pushed;
    result.pulled += tableResult.pulled;
    result.conflicts += tableResult.conflicts;
    result.errors.push(...tableResult.errors);
  }

  return result;
}
