# compile：双平台编译

## 验证方式

`compileBothPlatforms` 与目标节点 `compileJava` 证明共享输入和 Stonecutter 两个节点都能编译。它捕获平台 API 签名差异、版本条件分支错误、共享代码误用只在单一版本可用的类型，以及改共享输入后只修复一个节点的问题。

修改 `minecraft/src/**` 共享输入（尤其是带 Stonecutter `//?` 条件的文件）、`minecraft/versions/<node>/src/**` 版本专属实现、`api/minecraft/src/**` 中含 Minecraft 类型的公开 API，或 core 中被游戏层调用的跨层签名时执行。

## 注意事项

- 编译通过不等于行为验证；世界、机器 tick、交互、渲染、Mixin、网络和可选集成仍需游戏内验证。
- 不替代资源处理与打包；涉及资源或发行包时使用 [assemble](assemble.md)。
- 不替代格式检查；格式问题用 [format](format.md) 处理。
- 修改共享输入后重新运行双平台编译；只编译一个节点时，写出未覆盖节点。
- 检查节点 `build/generated/stonecutter/`，但不在生成副本上修复。

## 运行命令

```bash
./gradlew compileBothPlatforms
./gradlew :minecraft:1.20.1-forge:compileJava
./gradlew :minecraft:1.21.1-neoforge:compileJava
./gradlew :api-platform:1.20.1-forge:compileJava
./gradlew :api-platform:1.21.1-neoforge:compileJava
```
