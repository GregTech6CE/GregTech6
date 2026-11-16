# manual-ingame：游戏内人工验收

## 验证方式

游戏内验收覆盖自动检查无法证明的行为：世界状态、机器 tick、交互权限、渲染、Mixin 注入、网络同步、可选集成和存档迁移。它不替代 JUnit、GameTest 或编译，而是在这些检查之外确认玩家实际看到和操作到的行为。

修改世界、方块实体、机器 tick、交互、渲染、Mixin、网络、配置或可选集成，或 package 文档的验收章节要求游戏内确认时执行。纯文档改动不启动游戏；只运行一个平台不能推出另一个平台行为相同。

只选择与改动相关的场景，至少考虑：

- 无输入、资源不足、输出阻塞、能量或流体不足。
- 方向变化、拆装、区块卸载与重载、存档保存与读取。
- 客户端与专用服务端分别启动；专用服务端不加载客户端类型。
- 可选集成：未安装目标模组与安装匹配版本两组启动。
- 资源包重载、GUI 缩放、JEI/EMI 显示与快捷键。

具体到机器、配方、流体、能源、网络与可选集成的勾选项写在对应 package 文档的验收章节里。

## 注意事项

- 使用隔离实例；EULA 由操作者自行处理；完整日志、截图与实例配置放 `work/<任务名>/`，公共记录只保留脱敏结论。
- 记录提交或源码状态、JDK、Minecraft 与加载器版本、GT6CE 构建、依赖模组及版本、配置差异。
- 每条场景写清前置条件与操作、预期、实际、判定和平台；判定使用“未验证 / 部分验证 / 验证通过 / 已知失败”。
- 结束时分别列出“执行且通过”“执行但失败或阻塞”“尚未执行”，不要由一个平台启动成功推出两个平台行为相同。
- 不用人工观察替代本可自动化的规则；能在 JUnit 或 GameTest 中覆盖的行为优先补自动测试。
- 不把编译成功、客户端画面正常或单个平台启动写成完整验收。
- 不通过关闭 Mixin、放宽权限或跳过可选集成启动来获得“通过”。

## 运行命令

运行客户端与专用服务端：

```bash
./gradlew :minecraft:1.20.1-forge:runClient
./gradlew :minecraft:1.21.1-neoforge:runClient
./gradlew :minecraft:1.20.1-forge:runServer -PserverDirectory=../../../work/server-forge
./gradlew :minecraft:1.21.1-neoforge:runServer -PserverDirectory=../../../work/server-neoforge
```

Forge 默认实例在节点内 `run/`，NeoForge 客户端/服务端在 `run-client/`、`run-server/`。都属于本机数据，不提交存档或配置。

`-PserverDirectory` 由 Gradle 按节点项目目录解析；要放到仓库根 `work/` 时使用上面的 `../../../work/...`。

可选集成开关：`-PkubejsRuntime=true`、`-PcompatRuntime=ie|mek|ae|pr|hc`（每次选择一个值）、`-PemiSmoke=true`、`-PemiOnlySmoke=true`。具体依赖见 [Forge 配置](../../../../gradle/platform-forge.gradle)与 [NeoForge 配置](../../../../gradle/platform-neoforge.gradle)；加载依赖不代表集成已通过验证。

`-PdirectCoreClasspath=true`、`-PdirectCoreResources=true` 是本地运行优化；常规打包验收保持默认配置。`directCoreResources` 跳过 core 的 `processResources`，贴图以源码原样加载，`iCCP`、`pHYs` 等元数据块仍然存在，`IDAT` 块过滤也不生效：本地看到的画面不能代表发行包，资源相关验收仍需按默认配置重新打包一次。

修改 GUI 模板、别名、图标引用或 PNG 时，先做静态预览：

```bash
python tools/gui/preview.py
python tools/gui/preview.py --output work/gui-preview
```

依赖、参数、输出与预览边界见[工具说明](../../../../tools/gui/README.md)。
