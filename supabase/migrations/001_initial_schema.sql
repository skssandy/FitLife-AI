-- ============================================================
-- FitLife AI — Initial Database Migration
-- Version: 001
-- Date: 2026-01-27
-- ============================================================

-- ---- Extensions ----
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================
-- 1. PROFILES
-- ============================================================
CREATE TABLE public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,

    gender TEXT CHECK (gender IN ('male', 'female', 'other')),
    date_of_birth DATE,
    height_cm NUMERIC(5,2),
    current_weight_kg NUMERIC(5,2),

    fitness_goal TEXT CHECK (fitness_goal IN (
        'strength', 'hypertrophy', 'endurance', 'fat_loss', 'general_fitness', 'testosterone_optimization',
        'lean_tone', 'weight_loss', 'strength_female', 'endurance_female',
        'hormonal_balance', 'fertility_prep', 'prenatal', 'postpartum',
        'pcos_management', 'menopause_support', 'stress_relief'
    )),
    activity_level TEXT CHECK (activity_level IN (
        'sedentary', 'lightly_active', 'moderately_active', 'very_active', 'extremely_active'
    )),
    equipment_access JSONB DEFAULT '[]'::jsonb,
    workout_days_per_week INT DEFAULT 3 CHECK (workout_days_per_week BETWEEN 1 AND 7),
    preferred_workout_duration_min INT DEFAULT 45 CHECK (preferred_workout_duration_min BETWEEN 15 AND 180),

    injuries_limitations JSONB DEFAULT '[]'::jsonb,

    testosterone_optimization BOOLEAN DEFAULT FALSE,

    menstrual_cycle_length INT,
    menstrual_cycle_regularity TEXT CHECK (menstrual_cycle_regularity IN ('regular', 'irregular', 'pcos', 'perimenopause', 'none')),
    last_period_date DATE,
    pregnancy_status TEXT CHECK (pregnancy_status IN ('none', 'trying', 'trimester_1', 'trimester_2', 'trimester_3', 'postpartum')) DEFAULT 'none',
    postpartum_weeks INT,
    breastfeeding BOOLEAN DEFAULT FALSE,
    hormonal_birth_control BOOLEAN DEFAULT FALSE,
    hormonal_conditions JSONB DEFAULT '[]'::jsonb,

    lifestyle_habits JSONB DEFAULT '{}'::jsonb,

    theme TEXT CHECK (theme IN ('light', 'dark', 'system')) DEFAULT 'system',
    units TEXT CHECK (units IN ('metric', 'imperial')) DEFAULT 'metric',
    notification_preferences JSONB DEFAULT '{}'::jsonb,
    onboarding_completed BOOLEAN DEFAULT FALSE,
    onboarding_step INT DEFAULT 0,

    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_profiles_gender ON profiles(gender);
CREATE INDEX idx_profiles_fitness_goal ON profiles(fitness_goal);
CREATE INDEX idx_profiles_onboarding ON profiles(onboarding_completed);

ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users can view own profile" ON profiles FOR SELECT USING (auth.uid() = id);
CREATE POLICY "Users can update own profile" ON profiles FOR UPDATE USING (auth.uid() = id);
CREATE POLICY "Users can insert own profile" ON profiles FOR INSERT WITH CHECK (auth.uid() = id);

-- ============================================================
-- 2. BLOOD REPORTS
-- ============================================================
CREATE TABLE public.blood_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,

    report_date DATE NOT NULL,
    source TEXT CHECK (source IN ('lab_upload', 'manual_entry', 'health_connect', 'apple_health', 'google_fit')),
    file_path TEXT,
    file_name TEXT,
    file_size_bytes BIGINT,

    status TEXT CHECK (status IN ('pending', 'processing', 'completed', 'failed')) DEFAULT 'pending',
    parsing_method TEXT CHECK (parsing_method IN ('gemini_vision', 'aws_textract', 'manual', 'hybrid')),

    raw_text TEXT,
    raw_data JSONB,
    ai_analysis JSONB,

    analyzed_at TIMESTAMPTZ,
    analyzed_by_model TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_blood_reports_user_date ON blood_reports(user_id, report_date DESC);
CREATE INDEX idx_blood_reports_status ON blood_reports(status);

ALTER TABLE blood_reports ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users own blood reports" ON blood_reports FOR ALL USING (auth.uid() = user_id);

-- ============================================================
-- 3. BLOOD MARKERS
-- ============================================================
CREATE TABLE public.blood_markers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    blood_report_id UUID REFERENCES blood_reports(id) ON DELETE CASCADE NOT NULL,

    marker_name TEXT NOT NULL,
    display_name TEXT NOT NULL,
    category TEXT CHECK (category IN (
        'hormones_male', 'hormones_female', 'thyroid', 'metabolic', 'lipids',
        'nutrients', 'inflammation', 'organ_function', 'blood_count', 'other'
    )),

    value NUMERIC(12,3) NOT NULL,
    unit TEXT NOT NULL,

    ref_low NUMERIC(12,3),
    ref_high NUMERIC(12,3),
    optimal_low NUMERIC(12,3),
    optimal_high NUMERIC(12,3),

    status TEXT CHECK (status IN ('critical_low', 'low', 'suboptimal_low', 'optimal', 'suboptimal_high', 'high', 'critical_high')),
    flagged BOOLEAN DEFAULT FALSE,

    marker_code TEXT,
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_blood_markers_report ON blood_markers(blood_report_id);
CREATE INDEX idx_blood_markers_name ON blood_markers(marker_name);
CREATE INDEX idx_blood_markers_status ON blood_markers(status);

ALTER TABLE blood_markers ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users own markers via report" ON blood_markers
    FOR SELECT USING (EXISTS (SELECT 1 FROM blood_reports WHERE id = blood_report_id AND user_id = auth.uid()));

-- ============================================================
-- 4. NUTRITION PLANS
-- ============================================================
CREATE TABLE public.nutrition_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,

    generated_by TEXT CHECK (generated_by IN ('ai_nutritionist', 'human_dietitian', 'user_manual')) DEFAULT 'ai_nutritionist',
    based_on_blood_report_id UUID REFERENCES blood_reports(id) ON DELETE SET NULL,

    goal TEXT CHECK (goal IN ('weight_loss', 'muscle_gain', 'maintenance', 'hormonal_balance', 'fertility', 'pregnancy_support', 'postpartum_recovery', 'performance')) NOT NULL,

    calories_target INT NOT NULL,
    protein_g_target INT NOT NULL,
    carbs_g_target INT NOT NULL,
    fat_g_target INT NOT NULL,
    fiber_g_target INT DEFAULT 25,
    water_ml_target INT DEFAULT 3000,

    meal_structure JSONB NOT NULL,

    dietary_restrictions JSONB DEFAULT '[]'::jsonb,
    allergies JSONB DEFAULT '[]'::jsonb,
    cuisine_preferences JSONB DEFAULT '[]'::jsonb,

    supplement_recommendations JSONB DEFAULT '[]'::jsonb,
    macro_cycle JSONB,

    is_active BOOLEAN DEFAULT TRUE,
    version INT DEFAULT 1,
    parent_plan_id UUID REFERENCES nutrition_plans(id),

    expires_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_nutrition_plans_user_active ON nutrition_plans(user_id, is_active) WHERE is_active = TRUE;
CREATE INDEX idx_nutrition_plans_blood_report ON nutrition_plans(based_on_blood_report_id);

ALTER TABLE nutrition_plans ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users own nutrition plans" ON nutrition_plans FOR ALL USING (auth.uid() = user_id);

-- ============================================================
-- 5. DAILY LOGS
-- ============================================================
CREATE TABLE public.daily_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
    date DATE NOT NULL,

    nutrition JSONB DEFAULT '{}'::jsonb,
    water_ml INT DEFAULT 0,
    workout JSONB DEFAULT '{}'::jsonb,
    body_metrics JSONB DEFAULT '{}'::jsonb,
    wellbeing JSONB DEFAULT '{}'::jsonb,

    synced_at TIMESTAMPTZ,
    local_version INT DEFAULT 1,
    server_version INT DEFAULT 1,

    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),

    UNIQUE(user_id, date)
);

CREATE INDEX idx_daily_logs_user_date ON daily_logs(user_id, date DESC);
CREATE INDEX idx_daily_logs_synced ON daily_logs(synced_at) WHERE synced_at IS NULL;

ALTER TABLE daily_logs ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users own daily logs" ON daily_logs FOR ALL USING (auth.uid() = user_id);

-- ============================================================
-- 6. WORKOUT PROGRAMS (Templates)
-- ============================================================
CREATE TABLE public.workout_programs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    name TEXT NOT NULL,
    description TEXT,
    gender_target TEXT CHECK (gender_target IN ('male', 'female', 'both')) NOT NULL,
    goal_tags JSONB NOT NULL DEFAULT '[]'::jsonb,

    duration_weeks INT NOT NULL,
    days_per_week INT NOT NULL CHECK (days_per_week BETWEEN 1 AND 7),
    level TEXT CHECK (level IN ('beginner', 'intermediate', 'advanced')) NOT NULL,

    equipment_required JSONB DEFAULT '[]'::jsonb,

    created_by TEXT CHECK (created_by IN ('system', 'coach')) DEFAULT 'system',
    coach_id UUID REFERENCES auth.users(id),
    is_premium BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,

    version INT DEFAULT 1,
    parent_program_id UUID REFERENCES workout_programs(id),

    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE workout_programs ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Anyone can view active programs" ON workout_programs FOR SELECT USING (is_active = TRUE);

-- ============================================================
-- 7. WORKOUT DAYS
-- ============================================================
CREATE TABLE public.workout_days (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    program_id UUID REFERENCES workout_programs(id) ON DELETE CASCADE NOT NULL,
    day_index INT NOT NULL,

    name TEXT NOT NULL,
    focus TEXT CHECK (focus IN ('strength', 'hypertrophy', 'cardio', 'mobility', 'recovery', 'yoga', 'skill')) NOT NULL,
    estimated_duration_min INT,

    exercises JSONB NOT NULL,

    cycle_phase_modifications JSONB,
    pregnancy_modifications JSONB,

    UNIQUE(program_id, day_index)
);

CREATE INDEX idx_workout_days_program ON workout_days(program_id);

ALTER TABLE workout_days ENABLE ROW LEVEL SECURITY;
CREATE POLICY "View via program" ON workout_days FOR SELECT USING (
    EXISTS (SELECT 1 FROM workout_programs WHERE id = program_id AND is_active = TRUE)
);

-- ============================================================
-- 8. EXERCISES (Library)
-- ============================================================
CREATE TABLE public.exercises (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    name TEXT NOT NULL,
    slug TEXT UNIQUE NOT NULL,
    aliases JSONB DEFAULT '[]'::jsonb,

    category TEXT CHECK (category IN (
        'squat', 'hinge', 'push_horizontal', 'push_vertical',
        'pull_horizontal', 'pull_vertical', 'lunge', 'carry',
        'core_anti_extension', 'core_anti_rotation', 'core_rotation',
        'cardio_steady', 'cardio_intervals', 'mobility', 'pelvic_floor',
        'breathing', 'skill'
    )) NOT NULL,

    primary_muscles JSONB NOT NULL,
    secondary_muscles JSONB DEFAULT '[]'::jsonb,
    equipment JSONB DEFAULT '[]'::jsonb,

    video_url TEXT,
    thumbnail_url TEXT,
    form_cues JSONB DEFAULT '[]'::jsonb,
    common_mistakes JSONB DEFAULT '[]'::jsonb,

    progression_exercises JSONB DEFAULT '[]'::jsonb,
    regression_exercises JSONB DEFAULT '[]'::jsonb,

    contraindications JSONB DEFAULT '[]'::jsonb,
    pregnancy_safe JSONB DEFAULT '{}'::jsonb,

    difficulty TEXT CHECK (difficulty IN ('beginner', 'intermediate', 'advanced')),
    is_unilateral BOOLEAN DEFAULT FALSE,
    is_bodyweight BOOLEAN DEFAULT FALSE,
    met_value NUMERIC(4,2),

    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_exercises_category ON exercises(category);
CREATE INDEX idx_exercises_equipment ON exercises USING GIN(equipment);

ALTER TABLE exercises ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Anyone can view exercises" ON exercises FOR SELECT USING (TRUE);

-- ============================================================
-- 9. USER PROGRAMS (Active Program Instance)
-- ============================================================
CREATE TABLE public.user_programs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
    program_id UUID REFERENCES workout_programs(id) ON DELETE RESTRICT NOT NULL,

    started_at DATE DEFAULT CURRENT_DATE,
    current_week INT DEFAULT 1,
    current_day_index INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    completed_at DATE,

    adaptations JSONB DEFAULT '{}'::jsonb,

    preferred_time TEXT,
    reminder_enabled BOOLEAN DEFAULT TRUE,
    rest_day_preferences JSONB DEFAULT '{}'::jsonb,

    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_user_programs_user_active ON user_programs(user_id) WHERE is_active = TRUE;
CREATE INDEX idx_user_programs_user ON user_programs(user_id);

ALTER TABLE user_programs ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users own programs" ON user_programs FOR ALL USING (auth.uid() = user_id);

-- ============================================================
-- 10. WORKOUT SESSIONS (Logged Workouts)
-- ============================================================
CREATE TABLE public.workout_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
    user_program_id UUID REFERENCES user_programs(id) ON DELETE SET NULL,
    workout_day_id UUID REFERENCES workout_days(id) ON DELETE SET NULL,

    scheduled_date DATE,
    actual_date DATE,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,

    status TEXT CHECK (status IN ('completed', 'skipped', 'partial', 'in_progress')) DEFAULT 'completed',

    duration_min INT,
    exercises_performed JSONB NOT NULL DEFAULT '[]'::jsonb,

    avg_hr INT,
    max_hr INT,
    hr_zones JSONB,

    calories_burned INT,
    rpe_overall NUMERIC(2,1),
    energy_pre INT CHECK (energy_pre BETWEEN 1 AND 10),
    energy_post INT CHECK (energy_post BETWEEN 1 AND 10),

    wearable_workout_id UUID,

    notes TEXT,
    voice_note_url TEXT,

    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_workout_sessions_user_date ON workout_sessions(user_id, actual_date DESC);
CREATE INDEX idx_workout_sessions_program ON workout_sessions(user_program_id);

ALTER TABLE workout_sessions ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users own sessions" ON workout_sessions FOR ALL USING (auth.uid() = user_id);

-- ============================================================
-- 11. CYCLE LOGS (Female-Specific)
-- ============================================================
CREATE TABLE public.cycle_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,

    period_start_date DATE NOT NULL,
    period_end_date DATE,
    cycle_length INT,

    symptoms JSONB DEFAULT '[]'::jsonb,
    flow_intensity TEXT CHECK (flow_intensity IN ('spotting', 'light', 'medium', 'heavy', 'very_heavy')),

    ovulation_confirmed BOOLEAN DEFAULT FALSE,
    ovulation_date DATE,
    ovulation_method TEXT CHECK (ovulation_method IN ('lh_strip', 'bbt', 'cervical_mucus', 'wearable', 'symptoms', 'ultrasound')),
    luteal_phase_length INT,

    notes TEXT,

    predicted_next_period DATE,
    predicted_ovulation DATE,
    predicted_fertile_window JSONB,

    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_cycle_logs_user_date ON cycle_logs(user_id, period_start_date DESC);
CREATE UNIQUE INDEX idx_cycle_logs_user_period ON cycle_logs(user_id, period_start_date);

ALTER TABLE cycle_logs ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users own cycles" ON cycle_logs FOR ALL USING (auth.uid() = user_id);

-- ============================================================
-- 12. BODY METRICS
-- ============================================================
CREATE TABLE public.body_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,

    date DATE NOT NULL,
    weight_kg NUMERIC(5,2),
    body_fat_pct NUMERIC(4,1),
    muscle_mass_kg NUMERIC(5,2),
    measurements JSONB,

    photos JSONB,

    source TEXT CHECK (source IN ('manual', 'scale', 'dexa', 'caliper', 'wearable')),

    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_body_metrics_user_date ON body_metrics(user_id, date DESC);

ALTER TABLE body_metrics ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users own body metrics" ON body_metrics FOR ALL USING (auth.uid() = user_id);

-- ============================================================
-- 13. WEARABLE DEVICES
-- ============================================================
CREATE TABLE public.wearable_devices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,

    platform TEXT CHECK (platform IN ('health_connect', 'google_fit', 'garmin', 'fitbit', 'oura', 'whoop', 'samsung_health', 'gadgetbridge', 'manual')) NOT NULL,
    device_id TEXT,
    device_name TEXT,
    device_model TEXT,

    is_primary BOOLEAN DEFAULT FALSE,
    sync_enabled BOOLEAN DEFAULT TRUE,

    permissions JSONB DEFAULT '{}'::jsonb,

    last_sync_at TIMESTAMPTZ,
    last_sync_status TEXT CHECK (last_sync_status IN ('success', 'partial', 'failed', 'unauthorized')),
    last_sync_error TEXT,

    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),

    UNIQUE(user_id, platform, device_id)
);

ALTER TABLE wearable_devices ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users own devices" ON wearable_devices FOR ALL USING (auth.uid() = user_id);

-- ============================================================
-- 14. DAILY METRICS (Aggregated from Wearables)
-- ============================================================
CREATE TABLE public.daily_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
    date DATE NOT NULL,
    source TEXT NOT NULL,

    steps INT,
    active_calories INT,
    basal_calories INT,
    total_calories INT,
    exercise_minutes INT,
    stand_hours INT,
    distance_km NUMERIC(6,2),
    floors_climbed INT,

    resting_hr INT,
    avg_hr INT,
    max_hr INT,
    hrv_ms NUMERIC(5,1),
    hrv_status TEXT CHECK (hrv_status IN ('optimal', 'good', 'low', 'very_low')),

    sleep_duration_min INT,
    sleep_efficiency_pct INT,
    deep_sleep_min INT,
    rem_sleep_min INT,
    light_sleep_min INT,
    awake_min INT,
    sleep_consistency_score INT,
    sleep_score INT,

    readiness_score INT,
    recovery_score INT,
    strain_score NUMERIC(4,1),

    weight_kg NUMERIC(5,2),
    body_fat_pct NUMERIC(4,1),
    muscle_mass_kg NUMERIC(5,2),
    visceral_fat_rating INT,
    vo2_max NUMERIC(4,1),

    basal_body_temp_c NUMERIC(3,1),
    cycle_phase TEXT CHECK (cycle_phase IN ('menstrual', 'follicular', 'ovulatory', 'luteal')),

    temp_exposure_min INT,
    altitude_m INT,

    created_at TIMESTAMPTZ DEFAULT NOW(),

    UNIQUE(user_id, date, source)
);

CREATE INDEX idx_daily_metrics_user_date ON daily_metrics(user_id, date DESC);
CREATE INDEX idx_daily_metrics_source ON daily_metrics(source);

ALTER TABLE daily_metrics ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users own metrics" ON daily_metrics FOR ALL USING (auth.uid() = user_id);

-- ============================================================
-- 15. WEARABLE WORKOUTS (Auto-Detected)
-- ============================================================
CREATE TABLE public.wearable_workouts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
    source TEXT NOT NULL,
    external_id TEXT NOT NULL,

    workout_type TEXT,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    duration_min INT,
    calories INT,
    avg_hr INT,
    max_hr INT,
    distance_km NUMERIC(6,2),
    pace_min_per_km NUMERIC(4,2),
    elevation_gain_m INT,
    steps INT,
    route_geo JSONB,
    hr_zones JSONB,

    matched_to_session UUID,
    match_confidence NUMERIC(3,2),
    match_method TEXT CHECK (match_method IN ('auto_time', 'auto_gps', 'manual', 'user_confirmed')),

    created_at TIMESTAMPTZ DEFAULT NOW(),

    UNIQUE(user_id, source, external_id)
);

CREATE INDEX idx_wearable_workouts_user_time ON wearable_workouts(user_id, start_time DESC);
CREATE INDEX idx_wearable_workouts_matched ON wearable_workouts(matched_to_session);

ALTER TABLE wearable_workouts ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users own wearable workouts" ON wearable_workouts FOR ALL USING (auth.uid() = user_id);

-- ============================================================
-- 16. AI CONVERSATIONS
-- ============================================================
CREATE TABLE public.ai_conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,

    type TEXT CHECK (type IN ('nutritionist', 'coach', 'general', 'form_check', 'injury_support')) DEFAULT 'general',

    context JSONB DEFAULT '{}'::jsonb,

    title TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    message_count INT DEFAULT 0,

    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_ai_conversations_user ON ai_conversations(user_id, updated_at DESC);

ALTER TABLE ai_conversations ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users own conversations" ON ai_conversations FOR ALL USING (auth.uid() = user_id);

-- ============================================================
-- 17. AI MESSAGES
-- ============================================================
CREATE TABLE public.ai_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID REFERENCES ai_conversations(id) ON DELETE CASCADE NOT NULL,

    role TEXT CHECK (role IN ('user', 'assistant', 'system', 'function')) NOT NULL,
    content TEXT NOT NULL,

    metadata JSONB DEFAULT '{}'::jsonb,

    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_ai_messages_conversation ON ai_messages(conversation_id, created_at);

ALTER TABLE ai_messages ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users own messages via conversation" ON ai_messages
    FOR SELECT USING (EXISTS (SELECT 1 FROM ai_conversations WHERE id = conversation_id AND user_id = auth.uid()));
CREATE POLICY "Users insert own messages" ON ai_messages
    FOR INSERT WITH CHECK (EXISTS (SELECT 1 FROM ai_conversations WHERE id = conversation_id AND user_id = auth.uid()));

-- ============================================================
-- 18. HYDRATION SETTINGS
-- ============================================================
CREATE TABLE public.hydration_settings (
    user_id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,

    daily_target_ml INT DEFAULT 3000,
    reminder_enabled BOOLEAN DEFAULT TRUE,
    reminder_interval_min INT DEFAULT 60,
    reminder_start_time TIME DEFAULT '07:00',
    reminder_end_time TIME DEFAULT '22:00',
    smart_adjustments BOOLEAN DEFAULT TRUE,
    notification_style TEXT CHECK (notification_style IN ('gentle', 'persistent', 'gamified', 'minimal')) DEFAULT 'gentle',
    vessel_capacity_ml INT DEFAULT 500,
    electrolyte_reminders BOOLEAN DEFAULT FALSE,

    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE hydration_settings ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users own hydration settings" ON hydration_settings FOR ALL USING (auth.uid() = user_id);

-- ============================================================
-- 19. USER PREFERENCES
-- ============================================================
CREATE TABLE public.user_preferences (
    user_id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,

    auto_start_workout_timer BOOLEAN DEFAULT TRUE,
    haptic_feedback BOOLEAN DEFAULT TRUE,
    sound_enabled BOOLEAN DEFAULT TRUE,
    voice_cues_enabled BOOLEAN DEFAULT FALSE,
    rest_timer_default_sec INT DEFAULT 120,
    plate_calculator_enabled BOOLEAN DEFAULT TRUE,

    default_tab TEXT CHECK (default_tab IN ('workout', 'nutrition', 'progress', 'cycle')) DEFAULT 'workout',
    compact_mode BOOLEAN DEFAULT FALSE,
    show_exercise_videos BOOLEAN DEFAULT TRUE,
    video_autoplay BOOLEAN DEFAULT FALSE,

    analytics_opt_out BOOLEAN DEFAULT FALSE,
    crash_reporting_opt_out BOOLEAN DEFAULT FALSE,
    local_only_mode BOOLEAN DEFAULT FALSE,

    share_with_partner BOOLEAN DEFAULT FALSE,
    partner_user_id UUID,
    coach_access_enabled BOOLEAN DEFAULT FALSE,
    coach_user_id UUID,

    auto_export_enabled BOOLEAN DEFAULT FALSE,
    export_frequency TEXT CHECK (export_frequency IN ('weekly', 'monthly', 'quarterly')),
    export_format TEXT CHECK (export_format IN ('csv', 'pdf', 'json')) DEFAULT 'csv',

    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE user_preferences ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users own preferences" ON user_preferences FOR ALL USING (auth.uid() = user_id);

-- ============================================================
-- 20. TRIGGERS
-- ============================================================

-- Updated_at trigger function
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply updated_at trigger to all tables
CREATE TRIGGER update_profiles_updated_at BEFORE UPDATE ON profiles FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_blood_reports_updated_at BEFORE UPDATE ON blood_reports FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_nutrition_plans_updated_at BEFORE UPDATE ON nutrition_plans FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_daily_logs_updated_at BEFORE UPDATE ON daily_logs FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_workout_programs_updated_at BEFORE UPDATE ON workout_programs FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_user_programs_updated_at BEFORE UPDATE ON user_programs FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_workout_sessions_updated_at BEFORE UPDATE ON workout_sessions FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_cycle_logs_updated_at BEFORE UPDATE ON cycle_logs FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_body_metrics_updated_at BEFORE UPDATE ON body_metrics FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_wearable_devices_updated_at BEFORE UPDATE ON wearable_devices FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_ai_conversations_updated_at BEFORE UPDATE ON ai_conversations FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_hydration_settings_updated_at BEFORE UPDATE ON hydration_settings FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_user_preferences_updated_at BEFORE UPDATE ON user_preferences FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Auto-create profile on user signup
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.profiles (id, gender, onboarding_completed, onboarding_step)
    VALUES (NEW.id, 'other', FALSE, 0);
    INSERT INTO public.hydration_settings (user_id) VALUES (NEW.id);
    INSERT INTO public.user_preferences (user_id) VALUES (NEW.id);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- ============================================================
-- 21. STORAGE BUCKETS
-- ============================================================

INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES ('blood-reports', 'blood-reports', false, 10485760, ARRAY['application/pdf', 'image/jpeg', 'image/png', 'image/heic']);

INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES ('progress-photos', 'progress-photos', false, 5242880, ARRAY['image/jpeg', 'image/png', 'image/heic']);

INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES ('exercise-videos', 'exercise-videos', true, 52428800, ARRAY['video/mp4', 'video/webm']);

INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES ('avatars', 'avatars', true, 2097152, ARRAY['image/jpeg', 'image/png', 'image/webp']);

INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES ('ai-attachments', 'ai-attachments', false, 5242880, ARRAY['image/jpeg', 'image/png', 'image/heic', 'audio/m4a', 'audio/wav']);

-- Blood reports storage policies
CREATE POLICY "Users can upload own blood reports" ON storage.objects
    FOR INSERT WITH CHECK (bucket_id = 'blood-reports' AND auth.uid()::text = (storage.foldername(name))[1]);
CREATE POLICY "Users can view own blood reports" ON storage.objects
    FOR SELECT USING (bucket_id = 'blood-reports' AND auth.uid()::text = (storage.foldername(name))[1]);
CREATE POLICY "Users can delete own blood reports" ON storage.objects
    FOR DELETE USING (bucket_id = 'blood-reports' AND auth.uid()::text = (storage.foldername(name))[1]);

-- Progress photos storage policies
CREATE POLICY "Users can upload own progress photos" ON storage.objects
    FOR INSERT WITH CHECK (bucket_id = 'progress-photos' AND auth.uid()::text = (storage.foldername(name))[1]);
CREATE POLICY "Users can view own progress photos" ON storage.objects
    FOR SELECT USING (bucket_id = 'progress-photos' AND auth.uid()::text = (storage.foldername(name))[1]);
CREATE POLICY "Users can delete own progress photos" ON storage.objects
    FOR DELETE USING (bucket_id = 'progress-photos' AND auth.uid()::text = (storage.foldername(name))[1]);

-- Exercise videos (public read)
CREATE POLICY "Anyone can view exercise videos" ON storage.objects
    FOR SELECT USING (bucket_id = 'exercise-videos');

-- Avatars (public read, owner write)
CREATE POLICY "Anyone can view avatars" ON storage.objects
    FOR SELECT USING (bucket_id = 'avatars');
CREATE POLICY "Users can upload own avatar" ON storage.objects
    FOR INSERT WITH CHECK (bucket_id = 'avatars' AND auth.uid()::text = (storage.foldername(name))[1]);
CREATE POLICY "Users can update own avatar" ON storage.objects
    FOR UPDATE USING (bucket_id = 'avatars' AND auth.uid()::text = (storage.foldername(name))[1]);
CREATE POLICY "Users can delete own avatar" ON storage.objects
    FOR DELETE USING (bucket_id = 'avatars' AND auth.uid()::text = (storage.foldername(name))[1]);

-- AI attachments storage policies
CREATE POLICY "Users can upload own AI attachments" ON storage.objects
    FOR INSERT WITH CHECK (bucket_id = 'ai-attachments' AND auth.uid()::text = (storage.foldername(name))[1]);
CREATE POLICY "Users can view own AI attachments" ON storage.objects
    FOR SELECT USING (bucket_id = 'ai-attachments' AND auth.uid()::text = (storage.foldername(name))[1]);
CREATE POLICY "Users can delete own AI attachments" ON storage.objects
    FOR DELETE USING (bucket_id = 'ai-attachments' AND auth.uid()::text = (storage.foldername(name))[1]);
