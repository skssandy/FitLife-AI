import { useState, useEffect, useCallback } from "react";
import { Session, User } from "@supabase/supabase-js";
import {
  getSession,
  getUser,
  onAuthStateChange,
  signIn as authSignIn,
  signUp as authSignUp,
  signInWithMagicLink as authMagicLink,
  signOut as authSignOut,
} from "../services/supabase/auth";

export interface AuthState {
  session: Session | null;
  user: User | null;
  isLoading: boolean;
  isAuthenticated: boolean;
}

export function useAuth() {
  const [state, setState] = useState<AuthState>({
    session: null,
    user: null,
    isLoading: true,
    isAuthenticated: false,
  });

  useEffect(() => {
    getSession().then(({ data: { session } }) => {
      setState({
        session,
        user: session?.user ?? null,
        isLoading: false,
        isAuthenticated: !!session,
      });
    });

    const {
      data: { subscription },
    } = onAuthStateChange((_event, session) => {
      setState({
        session,
        user: session?.user ?? null,
        isLoading: false,
        isAuthenticated: !!session,
      });
    });

    return () => subscription.unsubscribe();
  }, []);

  const signIn = useCallback(async (email: string, password: string) => {
    return authSignIn(email, password);
  }, []);

  const signUp = useCallback(async (email: string, password: string) => {
    return authSignUp(email, password);
  }, []);

  const signInWithMagicLink = useCallback(async (email: string) => {
    return authMagicLink(email);
  }, []);

  const signOut = useCallback(async () => {
    return authSignOut();
  }, []);

  return {
    ...state,
    signIn,
    signUp,
    signInWithMagicLink,
    signOut,
  };
}
