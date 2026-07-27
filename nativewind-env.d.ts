/// <reference types="nativewind/types" />

declare module "*.svg" {
  import { SvgProps } from "react-native-svg";
  const content: React.FC<SvgProps>;
  export default content;
}

declare module "*.png" {
  const value: import("react-native").ImageSourcePropType;
  export default value;
}

declare module "*.jpg" {
  const value: import("react-native").ImageSourcePropType;
  export default value;
}

declare module "*.jpeg" {
  const value: import("react-native").ImageSourcePropType;
  export default value;
}

declare module "*.gif" {
  const value: import("react-native").ImageSourcePropType;
  export default value;
}

declare module "*.webp" {
  const value: import("react-native").ImageSourcePropType;
  export default value;
}

declare module "*.ttf" {
  const value: string;
  export default value;
}

declare namespace NodeJS {
  interface ProcessEnv {
    readonly EXPO_PUBLIC_SUPABASE_URL: string;
    readonly EXPO_PUBLIC_SUPABASE_ANON_KEY: string;
    readonly EXPO_PUBLIC_GEMINI_API_KEY: string;
    readonly EXPO_PUBLIC_GROQ_API_KEY: string;
    readonly EXPO_PUBLIC_OPENROUTER_API_KEY: string;
    readonly EXPO_PUBLIC_NVIDIA_API_KEY: string;
    readonly EXPO_PUBLIC_SENTRY_DSN: string;
    readonly EXPO_PUBLIC_POSTHOG_KEY: string;
    readonly EXPO_PUBLIC_POSTHOG_HOST: string;
    readonly EXPO_PUBLIC_HEALTH_CONNECT_APP_ID: string;
    readonly EXPO_PUBLIC_ENV: "development" | "preview" | "production";
  }
}
