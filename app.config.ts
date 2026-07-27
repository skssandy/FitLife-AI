import { ExpoConfig, ConfigContext } from "expo/config";

const IS_DEV = process.env.EXPO_PUBLIC_ENV === "development";
const IS_PREVIEW = process.env.EXPO_PUBLIC_ENV === "preview";

export default ({ config }: ConfigContext): ExpoConfig => ({
  ...config,
  name: "FitLife AI",
  slug: "fitlife-ai",
  version: "1.0.0",
  orientation: "portrait",
  icon: "./assets/icon.png",
  scheme: "fitlifeai",
  userInterfaceStyle: "automatic",
  newArchEnabled: true,

  splash: {
    image: "./assets/splash-icon.png",
    resizeMode: "contain",
    backgroundColor: "#0A0A0A",
  },

  ios: {
    supportsTablet: true,
    bundleIdentifier: "com.fitlife.ai",
    buildNumber: "1",
    infoPlist: {
      NSHealthShareUsageDescription:
        "FitLife AI reads your health data to provide personalized fitness and nutrition recommendations.",
      NSHealthUpdateUsageDescription:
        "FitLife AI writes workout and nutrition data to Apple Health for a unified health view.",
      NSCameraUsageDescription:
        "FitLife AI uses the camera to photograph blood reports for AI analysis and track progress photos.",
      NSPhotoLibraryUsageDescription:
        "FitLife AI accesses your photo library to upload blood reports and progress photos for analysis.",
      NSMicrophoneUsageDescription:
        "FitLife AI uses the microphone for voice notes during workouts and AI coach conversations.",
    },
  },

  android: {
    package: "com.fitlife.ai",
    versionCode: 1,
    adaptiveIcon: {
      foregroundImage: "./assets/adaptive-icon.png",
      backgroundColor: "#0A0A0A",
    },
    permissions: [
      "android.permission.ACTIVITY_RECOGNITION",
      "android.permission.BODY_SENSORS",
      "android.permission.HEALTH_CONNECT",
      "android.permission.POST_NOTIFICATIONS",
      "android.permission.CAMERA",
      "android.permission.READ_EXTERNAL_STORAGE",
      "android.permission.WRITE_EXTERNAL_STORAGE",
      "android.permission.INTERNET",
      "android.permission.ACCESS_NETWORK_STATE",
    ],
    healthConnect: {
      permissions: [
        "android.permission.health.READ_HEART_RATE",
        "android.permission.health.READ_STEPS",
        "android.permission.health.READ_SLEEP",
        "android.permission.health.READ_BODY_MEASUREMENTS",
        "android.permission.health.READ_ACTIVE_CALORIES_BURNED",
        "android.permission.health.READ_EXERCISE",
        "android.permission.health.READ_DISTANCE",
        "android.permission.health.READ_TOTAL_CALORIES_BURNED",
        "android.permission.health.READ_RESTING_HEART_RATE",
        "android.permission.health.READ_OXYGEN_SATURATION",
        "android.permission.health.READ_BLOOD_PRESSURE",
        "android.permission.health.READ_MENSTRUATION",
        "android.permission.health.READ_BASAL_BODY_TEMPERATURE",
        "android.permission.health.WRITE_WORKOUT",
        "android.permission.health.WRITE_EXERCISE",
        "android.permission.health.WRITE_BODY_MEASUREMENTS",
      ],
    },
  },

  plugins: [
    [
      "expo-router",
      {
        scheme: "fitlifeai",
      },
    ],
    [
      "expo-camera",
      {
        cameraPermission:
          "FitLife AI uses the camera to photograph blood reports for AI analysis.",
      },
    ],
    [
      "expo-image-picker",
      {
        photosPermission:
          "FitLife AI accesses your photos to upload blood reports and progress photos.",
      },
    ],
    [
      "expo-notifications",
      {
        icon: "./assets/notification-icon.png",
        color: "#0096FF",
      },
    ],
    "expo-secure-store",
    "expo-local-authentication",
    "expo-haptics",
    "expo-web-browser",
  ],

  extra: {
    supabaseUrl: process.env.EXPO_PUBLIC_SUPABASE_URL,
    supabaseAnonKey: process.env.EXPO_PUBLIC_SUPABASE_ANON_KEY,
    geminiApiKey: process.env.EXPO_PUBLIC_GEMINI_API_KEY,
    groqApiKey: process.env.EXPO_PUBLIC_GROQ_API_KEY,
    openRouterApiKey: process.env.EXPO_PUBLIC_OPENROUTER_API_KEY,
    nvidiaApiKey: process.env.EXPO_PUBLIC_NVIDIA_API_KEY,
    sentryDsn: process.env.EXPO_PUBLIC_SENTRY_DSN,
    posthogKey: process.env.EXPO_PUBLIC_POSTHOG_KEY,
    posthogHost: process.env.EXPO_PUBLIC_POSTHOG_HOST,
    healthConnectAppId: process.env.EXPO_PUBLIC_HEALTH_CONNECT_APP_ID,
    eas: {
      projectId: "your-eas-project-id",
    },
  },

  experiments: {
    typedRoutes: true,
  },
});
