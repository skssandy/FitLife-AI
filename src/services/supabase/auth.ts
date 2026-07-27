import { supabase } from "./client";
import { Session, AuthError } from "@supabase/supabase-js";

export interface AuthResult {
  data: { session: Session | null; user: any } | null;
  error: AuthError | null;
}

export async function signUp(
  email: string,
  password: string
): Promise<AuthResult> {
  const { data, error } = await supabase.auth.signUp({
    email,
    password,
  });
  return { data, error };
}

export async function signIn(
  email: string,
  password: string
): Promise<AuthResult> {
  const { data, error } = await supabase.auth.signInWithPassword({
    email,
    password,
  });
  return { data, error };
}

export async function signInWithMagicLink(
  email: string
): Promise<{ data: { email: string } | null; error: AuthError | null }> {
  const { data, error } = await supabase.auth.signInWithOtp({
    email,
    options: {
      emailRedirectTo: "fitlifeai://auth/confirm",
    },
  });
  return { data, error };
}

export async function signOut(): Promise<{ error: AuthError | null }> {
  const { error } = await supabase.auth.signOut();
  return { error };
}

export async function getSession(): Promise<{
  data: { session: Session | null };
  error: AuthError | null;
}> {
  const { data, error } = await supabase.auth.getSession();
  return { data, error };
}

export async function getUser() {
  const {
    data: { user },
    error,
  } = await supabase.auth.getUser();
  return { user, error };
}

export function onAuthStateChange(
  callback: (event: string, session: Session | null) => void
) {
  return supabase.auth.onAuthStateChange((event, session) => {
    callback(event, session);
  });
}
