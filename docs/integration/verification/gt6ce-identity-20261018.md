# 2026-10-18 / GT6CE 模组身份

指定加载器 ID 为 `gt6ce`，显示名称为 `GregTech 6 Community Edition`。名称在此前代码中已正确，本批将 `GregTechIdentity.MOD_ID` 与 Gradle 的 `mod_id` 改为新 ID；Forge／NeoForge 的入口、事件订阅、mod 配置和元数据使用该统一身份。

更新根构建名称、正式包与 addon API 包名前缀、CI 成品命名、验包属性、两版生产探针依赖及本地交付选择。保留历史验证回执和冻结迁移清单；`META-INF/gregtech6` 是许可证与来源目录，并非加载器 ID。现有物品／资源 `gregtech:`、Java 包和原版配方符号保持独立身份。

两版 `ModData` presence 通过入口将逻辑 namespace `gregtech` 映射到新加载器 ID；`ModReferences.GT` 的 namespace 仍用于识别已有物品。动态物品模型的 `shared_inventory` 几何加载器声明和注册均使用统一 `MOD_ID`，一同成为 `gt6ce:shared_inventory`。默认配置名随容器成为 `gt6ce-common.toml`，本批不搬移运行目录里的旧配置。

首次成品注解核验发现 `FluidDisplayTooltips` 的两版分支仍写死加载器 ID `gregtech`，该回调此前未与模组容器一致。本批改用统一身份常量，并在修正后重新打包，核对全包入口和事件订阅注解。

正式输出为 `gt6ce-1.20.1-forge-0.0.0.jar`、`gt6ce-neoforge-1.21.1-0.0.0.jar`；SDK 为 `gt6ce-addon-api-0.0.0.jar`。本地交付工具按当前属性精确选择成品，旧构建输出不会被误选。

最终双版打包和元数据、当前字节码、模型加载器身份的核对结果见 [JSON 回执](gt6ce-identity-20261018.json)。本轮不启动游戏、不重跑全套测试，静态／编译／验包结果不代表运行和旧存档兼容验收。
