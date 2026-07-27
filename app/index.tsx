import { View, Text } from "react-native";

export default function HomeScreen() {
  return (
    <View className="flex-1 items-center justify-center bg-male-bg-primary">
      <Text className="text-2xl font-bold text-male-accent">
        FitLife AI
      </Text>
      <Text className="text-base text-male-text-secondary mt-2">
        Foundation loaded successfully
      </Text>
    </View>
  );
}
