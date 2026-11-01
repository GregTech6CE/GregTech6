# assemble：资源、打包与产物检查

## 验证方式

`assembleBothPlatforms`、Forge `distributionJar` 与 NeoForge `jar` 证明资源处理、打包和产物组成正确：资源路径与版本目录匹配、公共 API 与 core 被正确合并、API 类只有一份、SDK 与模组产物分离、发行 JAR 不含测试类或临时文件。

修改 `src/main/resources/**`、资源处理或打包逻辑、跨版本资源路径、`META-INF` 元数据或模组声明，以及需要确认最终交付物内容时执行。

## 注意事项

- 打包成功不等于运行正常；仍需编译、GameTest 或游戏内验证。
- Forge 中间 `jar` 输出到 `build/forge-intermediates/`，不作为最终安装包。
- SDK、sources、javadoc JAR 不是模组，不能放进 `mods/`。
- 不使用 `runData` 代替只读检查；它会写生产资源。
- 资源路径大小写、有效 JSON、目标版本路径、引用存在、发行包不含临时文件；各 package 的具体规则写在对应 package 文档的验收章节。

## 运行命令

```bash
./gradlew assembleBothPlatforms
./gradlew :api:jar
./gradlew :api:assemble
./gradlew :minecraft:1.20.1-forge:distributionJar
./gradlew :minecraft:1.21.1-neoforge:jar
./gradlew :minecraft:1.20.1-forge:assemble
./gradlew :minecraft:1.21.1-neoforge:assemble
./gradlew :minecraft:1.20.1-forge:addonSdk
./gradlew :minecraft:1.21.1-neoforge:addonSdk
```

资源 JAR 检查：

```bash
python tools/resources/check_compact_model_json.py <发行JAR> --resources core/build/resources/main minecraft/versions/<node>/build/resources/main
```

Forge 使用 `distributionJar` 的 `build/libs` 产物，不使用中间 JAR。
