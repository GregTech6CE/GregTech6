# buildtools：1.21.1 静态资源转换

`tools/buildtools` 是 Gradle 子项目 `:buildtools`，把 core 中按 1.20.1 形状维护的配方与
物品标签树转换成 1.21.1 原生资源。构建期 `generateRecipeResources` 与 JUnit 共用同一份
转换器；模块只参与构建，不进入模组 JAR。

目录名不使用 `build`，避免被根 `.gitignore` 的 `build/` 规则整体忽略。

## 输入

- `core/src/main/resources/data/<namespace>/recipes/**` 与 `tags/items/**`（1.20.1 形状）
- `tools/recipes/static-publish-exclusions.txt`：不发布、由运行时行替代的静态配方
- `minecraft/versions/1.21.1-neoforge/src/main/resources/data/**`：节点自有资源，用于拒绝目标重名

## 依赖

- Java 17（Gradle toolchain）
- Gson 2.11.0
- JUnit 5.11.4

## 输出

- `minecraft/versions/1.21.1-neoforge/build/generated/recipe-resources/data/`
  - `*/recipe/**`：转换后的配方
  - `*/tags/item/**`：目录改为单数，`values` 原样保留
- `minecraft/versions/1.21.1-neoforge/build/reports/recipe-resources-manifest.txt`：本次写出的文件清单

输出整棵重建，源文件删除后不会残留；目标与节点自有资源重名时构建失败。

## 运行

```bash
./gradlew :minecraft:1.21.1-neoforge:generateRecipeResources
./gradlew :buildtools:test
```

`python tools/recipes/audit_recipes.py --converted minecraft/versions/1.21.1-neoforge/build/generated/recipe-resources`
会交叉检查生成树与源配方、源标签是否一一对应。
