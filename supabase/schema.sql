-- FitLife AI - Supabase schema
-- Run this in: Supabase Dashboard -> SQL Editor -> New query

-- Column names are quoted to match the exact camelCase field names the
-- Android app serializes into PostgREST JSON (WorkoutEntity, CalorieEntryEntity,
-- UserEntity). Unquoted identifiers would fold to lowercase and break sync.

create table if not exists user_profiles (
  "id" uuid primary key,
  "email" text,
  "displayName" text,
  "photoUrl" text,
  "heightCm" double precision,
  "weightKg" double precision,
  "dateOfBirth" text,
  "gender" text,
  "fitnessGoal" text,
  "activityLevel" text,
  "updatedAt" bigint
);

create table if not exists workouts (
  "id" bigint primary key,
  "userId" uuid,
  "exerciseName" text,
  "sets" integer,
  "reps" integer,
  "weightKg" double precision,
  "durationMinutes" integer,
  "caloriesBurned" integer,
  "notes" text,
  "date" bigint,
  "synced" boolean default false,
  "createdAt" bigint
);

create table if not exists calorie_entries (
  "id" bigint primary key,
  "userId" uuid,
  "foodName" text,
  "calories" integer,
  "proteinG" double precision,
  "carbsG" double precision,
  "fatG" double precision,
  "mealType" text,
  "date" bigint,
  "synced" boolean default false,
  "createdAt" bigint
);

-- PostgREST role privileges
grant select on table user_profiles to anon;
grant select, insert, update, delete on table user_profiles to authenticated;
grant select, insert, update, delete on table user_profiles to service_role;

grant select on table workouts to anon;
grant select, insert, update, delete on table workouts to authenticated;
grant select, insert, update, delete on table workouts to service_role;

grant select on table calorie_entries to anon;
grant select, insert, update, delete on table calorie_entries to authenticated;
grant select, insert, update, delete on table calorie_entries to service_role;

-- Row Level Security
alter table user_profiles enable row level security;
alter table workouts enable row level security;
alter table calorie_entries enable row level security;

drop policy if exists "profiles_own" on user_profiles;
create policy "profiles_own" on user_profiles
  for all using (auth.uid() = "id") with check (auth.uid() = "id");

drop policy if exists "workouts_own" on workouts;
create policy "workouts_own" on workouts
  for all using (auth.uid()::text = "userId"::text) with check (auth.uid()::text = "userId"::text);

drop policy if exists "calories_own" on calorie_entries;
create policy "calories_own" on calorie_entries
  for all using (auth.uid()::text = "userId"::text) with check (auth.uid()::text = "userId"::text);
