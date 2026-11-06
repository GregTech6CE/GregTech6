# format：格式检查

## 验证方式

Spotless 使用 Palantir Java Format 检查 Java 源码格式：4 空格缩进、120 列。CI 的 `verify` 与双节点 `build` 都包含 `spotlessCheck`，格式问题不能留到 CI 才处理。

| 项目 | 是否接入 Spotless |
| --- | --- |
| `:core` | 是 |
| `:buildtools` | 是 |
| `:minecraft` 共享源码 | 是 |
| `:minecraft:1.20.1-forge` | 是 |
| `:minecraft:1.21.1-neoforge` | 是 |
| `:api` | 否，已知缺口 |
| `:api-platform` | 否，已知缺口 |

`:api` 与 `:api-platform` 当前未接入 Spotless。修改这两层时仍要按现有文件风格书写，但不能把 `spotlessCheck` 通过解读为这两层已格式化；补齐接入属于单独的构建改动。

Spotless 排除 `**/generated/**` 与包含 Stonecutter `//?` 标记的共享文件；后两者由节点级处理。不要直接对生成副本套用格式任务。

## 注意事项

- 代码改动必须在本地先通过 `spotlessCheck`，不要留到 CI。
- 格式通过不证明编译、测试或运行行为。
- 不为了通过而关闭 Spotless、扩大排除范围或跳过项目。
- 不顺手批量格式化未改动的文件；`format` 后检查 diff，保留无关文件的原有格式。

## 运行命令

```bash
./gradlew spotlessCheck
./gradlew format

./gradlew :core:spotlessApply
./gradlew :buildtools:spotlessApply
./gradlew :minecraft:spotlessApply
./gradlew :minecraft:1.20.1-forge:spotlessApply
./gradlew :minecraft:1.21.1-neoforge:spotlessApply
```
