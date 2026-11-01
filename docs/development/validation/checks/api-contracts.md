# api-contracts：公共 API 契约检查

## 验证方式

用打包后的 API JAR 编译并运行外部消费者，检查公开类签名可解析、能源契约与范围正确、宿主未绑定时的失败行为符合约定。验证对象是外部附属看到的 API，不是主模组内部实现。

修改 `api/common/src/main/java/**` 的公开签名、默认方法、异常或生命周期声明，修改 `api/minecraft/src/main/java/**` 中随 SDK 提供给附属的游戏侧类型，或修改 API 版本、发布坐标与 SDK 组成时执行。

## 注意事项

- 不借用 main 输出或 core 类；测试通过不代表主模组行为已验证。
- 不替代附属 SPI 发现、冲突失败信息和基础启动、启用集成两种运行验证。
- 需要全新生命周期的测试使用独立 JVM，不添加生产 reset 入口。
- 含 Minecraft 类型的 API 还要按目标平台编译；两个平台 SDK 不能互换。

## 运行命令

```bash
./gradlew :api:apiContracts
./gradlew :api:check
./gradlew :api:assemble
```

修改平台 SDK 时，按目标平台执行：

```bash
./gradlew :minecraft:1.20.1-forge:addonSdk
./gradlew :minecraft:1.21.1-neoforge:addonSdk
```
