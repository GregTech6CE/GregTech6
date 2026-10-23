# 双版本模组名称和加载 ID

日期：2026-10-02。按已确认指定，两版本名称为 `GregTech 6 Integrated`，加载 ID 为 `gregtech6`。

## 实施范围

- Forge 和 NeoForge 元数据均读取根 `gradle.properties`，NeoForge 不再单独写死名称和 ID。
- 共享 `GregTechIdentity` 定义入口 ID、显示名称和保留的注册命名空间；两平台入口及所有原硬编码事件订阅同步。
- 现有 `gregtech:` 物品、方块、配方、模型、翻译和网络通道保持一致。Forge 原混用 `MODID` 的资源/注册引用改为显式 `NAMESPACE`；Native 材料注册也使用同一旧命名空间。
- 材料来源的 `ModData` 继续使用原注册前缀，平台存在性查询将该前缀映射到实际 `gregtech6` 模组，避免改名后本模组材料被判定为未加载。第三方标识及许可证不更改。
- 构建产物前缀统一为 `gregtech6`，NeoForge 开发运行的 mods 定义随根配置同步，既有 GameTest 资源命名空间保留。
- 更新现有 CI 验包 ID 约束与其元数据样本，未新增专项测试夹具。`BUILDING.md` 更新双版本构建方法和实际输出名称。

## 验证和兼容范围

集中双版本 Java 编译通过；最终资源处理通过，实际生成的两份 TOML 已核对名称、加载 ID 及 dependencies 所属 ID 完全一致。见 `verification/mod-identity-compile-20261002.json`。

本批未启动游戏、未重新打成品 JAR、未跑完整玩法或独立世界重载。当前交付为 GitHub 上传用源码和历史备份，旧测试 JAR 不代表此次改名后的版本。默认配置文件名可能随加载 ID 改变，旧自定义配置需人工核对迁移；保留注册 ID 不等于旧存档已完成兼容验证。

此前 a4b203f8bb 的 Native JAR 已在本批改名前打包成功；独立 NeoForge 服务器安装因 Java 下载 Mojang 清单超时失败，尚未加载该 JAR。此网络失败不作为模组启动失败，也不作为成品启动成功证据。本次上传包优先交付，不继续等待安装器。

原三源审计/归属/许可和已保留 Git 历史均沿用。整合的完整玩法、成品启动、存档与发布验收仍未完成，整体移植仍未完成。
