module.exports = function (api) {
  api.cache(true);
  return {
    presets: [
      ["babel-preset-expo", { jsxImportSource: "nativewind" }],
      "nativewind/babel",
    ],
    plugins: [
      "react-native-reanimated/plugin",
      [
        "module-resolver",
        {
          root: ["."],
          extensions: [".ios.js", ".android.js", ".js", ".ts", ".tsx", ".json"],
          alias: {
            "@components": "./src/components",
            "@hooks": "./src/hooks",
            "@store": "./src/store",
            "@types": "./src/types",
            "@utils": "./src/utils",
            "@constants": "./src/constants",
            "@assets": "./src/assets",
            "@config": "./src/config",
            "@context": "./src/context",
            "@models": "./src/models",
            "@screens": "./src/screens",
            "@services": "./src/services",
            "@data": "./src/data",
          },
        },
      ],
    ],
  };
};
