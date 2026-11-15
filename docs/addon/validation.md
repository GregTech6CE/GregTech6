# 开发流程与验证

本页用于从第一个附属声明推进到目标平台验收，逐项记录执行结果及未覆盖场景。

返回[附属编写指南](README.md)。

## 推荐开发流程

1. 固定目标 Minecraft、加载器和 GT6CE 构建，构建匹配的公共 API 与平台 SDK；确认附属不使用另一节点的 SDK。
2. 在附属模组构造阶段注册 `GregTechAddon`，确定自己的 mod ID 和 namespace。
3. 按目标选择 L1 声明入口；先能通过启动校验，再增加运行时行为。材料、机器和世界生成声明不要依赖尚未就绪的运行时目录。
4. 为每个需要平台行为的机器或功能部件添加对应 `ServiceLoader` provider；保证类型集合完整，并在 provider 中只创建行为实例，不重复注册方块。
5. 只有宿主处理器无法表达的状态机才进入 L2；在 `tick`、结构回调和玩家交互中遵守服务端边界和上下文生命周期。
6. 需要可重载的数据或脚本支持时，另做数据包或 KubeJS。数据包只修改已有机器配方或附属可替换布局，不创建新主控。
7. 需要和已有 GT 能量、传感器、管道、控制或配方系统互通时，在附属自己的方块实体或配方对象上实现 L4 接口；不要继承宿主内部类。
8. 按目标平台的客户端、专用服务端和数据重载路径验证，再记录实际执行结果。

## 验证清单

纯文档改动不要求编译或启动游戏；改动 Java、资源或数据入口时，至少按下列范围逐项判断并在交付中列明是否执行：

- 公共 API 与目标平台 SDK 构建成功，附属编译使用匹配的 Java 版本和 `compileOnly` 依赖。
- `META-INF/services/` 中的 provider 类可实例化，`addonId` 正确，行为机和功能部件的类型集合完整且没有重复 provider。
- 材料启动完成并进入 READY；重名、跨所有者冲突、未知引用和环能在启动期报错，而不是静默覆盖。
- 配方表、配方机、燃料机和多方块在成型、未成型、输入不足、输出阻塞、拆解和区块重载场景下行为正确。
- 行为机的能源、槽位、流体、范围、压缩气体、结构回调和客户端同步分别检查；专用服务端不加载客户端 SPI。
- 自定义数据包配方在重载、移除、追加、优先级和错误行隔离场景下检查；自定义文档类型在注册关闭后拒绝迟注册。
- `gt6ce_layouts` 只替换可替换的附属/脚本布局；重载后服务端匹配、部件归属和客户端预览同步失效并重建。
- KubeJS 在两个目标节点分别使用匹配版本检查启动、数据生成和配方 schema；不能以 JSON 解析测试代替脚本加载或游戏行为验证。
- L4 接口检查面方向、模拟/执行、包大小、空输入、未知方块实体、区块卸载和专用服务端行为。
- 诊断接口检查未知资源、未加载目标、客户端调用、旧宿主和重复通知；不要把一次静态编译或单个 GameTest 说成完整游戏验收。

结果报告按“执行且通过”“执行但失败或阻塞”“尚未执行”分开，写明平台、命令、环境、操作和未覆盖场景；仓库内的检查路由见[验证与验收](../development/validation/README.md)。

## 仓库内的检查入口

以下检查在 GT6CE 仓库根目录按实际改动选择；外部附属工程还须运行自己的构建与目标加载器测试。命令和注意事项统一维护在检查文档，不在此复制。

| 改动 | 检查入口 |
| --- | --- |
| 公共契约或 SDK 消费 | [API 契约](../development/validation/checks/api-contracts.md)、[JUnit](../development/validation/checks/junit.md) |
| 游戏侧 API 与平台适配 | [双平台编译](../development/validation/checks/compile.md) |
| SDK 与发行产物 | [打包](../development/validation/checks/assemble.md) |
| Java 格式 | [format](../development/validation/checks/format.md) |
| 多方块注册和传输 | [GameTest](../development/validation/checks/gametest.md)、[游戏内验收](../development/validation/checks/manual-ingame.md) |
| GUI 资源 | [GUI 工具](../../tools/gui/README.md)、[游戏内验收](../development/validation/checks/manual-ingame.md) |
| 本指南的纯文档改动 | [文档一致性要求](../development/validation/README.md#工作流程) |
