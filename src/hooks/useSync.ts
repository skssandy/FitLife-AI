import { useState, useEffect, useCallback } from "react";
import { AppState, AppStateStatus } from "react-native";
import { syncAll, syncTable } from "../services/sync/sync-engine";
import { SyncResult } from "../services/sync/types";

export interface SyncState {
  isSyncing: boolean;
  lastSyncResult: SyncResult | null;
  lastSyncAt: Date | null;
  syncError: string | null;
}

export function useSync() {
  const [state, setState] = useState<SyncState>({
    isSyncing: false,
    lastSyncResult: null,
    lastSyncAt: null,
    syncError: null,
  });

  const performSync = useCallback(async () => {
    setState((prev) => ({ ...prev, isSyncing: true, syncError: null }));

    try {
      const result = await syncAll();
      setState({
        isSyncing: false,
        lastSyncResult: result,
        lastSyncAt: new Date(),
        syncError: result.errors.length > 0 ? result.errors.join("; ") : null,
      });
    } catch (error) {
      setState((prev) => ({
        ...prev,
        isSyncing: false,
        syncError: error instanceof Error ? error.message : "Sync failed",
      }));
    }
  }, []);

  const syncSingleTable = useCallback(async (tableName: string) => {
    setState((prev) => ({ ...prev, isSyncing: true, syncError: null }));

    try {
      const result = await syncTable(tableName);
      setState({
        isSyncing: false,
        lastSyncResult: result,
        lastSyncAt: new Date(),
        syncError: result.errors.length > 0 ? result.errors.join("; ") : null,
      });
    } catch (error) {
      setState((prev) => ({
        ...prev,
        isSyncing: false,
        syncError: error instanceof Error ? error.message : "Sync failed",
      }));
    }
  }, []);

  useEffect(() => {
    const handleAppStateChange = (nextState: AppStateStatus) => {
      if (nextState === "active") {
        performSync();
      }
    };

    const subscription = AppState.addEventListener(
      "change",
      handleAppStateChange
    );

    return () => subscription.remove();
  }, [performSync]);

  return {
    ...state,
    performSync,
    syncSingleTable,
  };
}
