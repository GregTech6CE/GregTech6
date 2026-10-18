# 2026-10-18 / lombinaxmasson 上游附属 API 栈接入 Stonecutter

## 来源与接入范围

本批响应用户“将 lombinaxmasson 最新 main PR 整理到 Stonecutter，并说明各 PR 内容”的要求。目标为 `experiment/stonecutter`；主线来源固定为 [`c899462a616100a1007f9dec56372ea6489c9f0a`](https://github.com/SaltNya/GregTech6/commit/c899462a616100a1007f9dec56372ea6489c9f0a)，即 [PR #45](https://github.com/SaltNya/GregTech6/pull/45)，2026-10-18 22:14:24（上海）合入 main。本地接入前版本为 `a36f35f251fc0f36c13d4e2953082d4c619ea008`，共同祖先为 `9b244838acf887f45cba6b256fc64aa9d8588ea5`。

作者为 GitHub `lombinaxmasson`，提交署名 **Lorbineitte Masson <JeunesseRouge@protonmail.com>**。采用真实 Git 历史及 PR API 数据，不将存档快照伪造为作者提交。代码原有 LGPL-3.0-or-later 标识、项目 NOTICE 和来源许可证文件保留。逐项来源、提交与文件摘要见[来源回执](main-addon-stack-provenance-20261018.json)。

功能适配先独立提交，再以明确的 `ours` 合并提交将固定的上游 `c899462a6` 记录为已接入祖先。第二步仅记录真实上游历史，树内容保持已验证的 Stonecutter 适配结果，防止把上游旧目录、命名空间和已清理身份重新合入；不改 main 分支。后续同步据此从本次上游版本继续比较。

## 各 PR 的内容

| PR | 主要内容 | 本分支处理 |
| --- | --- | --- |
| [#29](https://github.com/SaltNya/GregTech6/pull/29) | 先分配 GT 自带材料物品 ID；附属动态配方提供者追加而非覆盖 | 接入，保持已有 56,085 个材料物品 ID |
| [#30](https://github.com/SaltNya/GregTech6/pull/30) | 附属可发送、传递和接收小数信号；修正实心方块端口和工具操作 | 接入公共信号接口及双版原生接线 |
| [#31](https://github.com/SaltNya/GregTech6/pull/31) | 材料目录冻结前声明附属材料 | 接入最终 v7 材料生命周期 |
| [#32](https://github.com/SaltNya/GregTech6/pull/32) | 数据包增删机器配方、重载和查看器同步；线缆/轴动态碰撞形状；Forge 大包限制 | 接入；本模组配方类型使用 `gt6ce:machine` / `gt6ce:machine_removal` |
| [#33](https://github.com/SaltNya/GregTech6/pull/33) | 共用原生资源树、构建入口调整；湿建筑泡沫方块实体映射修复 | 资源与构建沿用 Stonecutter；采用泡沫功能修复 |
| [#34](https://github.com/SaltNya/GregTech6/pull/34) | 32 对近似平台类通过加载器门面共用 | 目录重构由 Stonecutter 条件源码取代；新 API 实现所需 `Mc` 门面接入现有结构 |
| [#35](https://github.com/SaltNya/GregTech6/pull/35) | 269 对相同平台类合并；石头配方标识大小写处理 | 不恢复第二套共用目录；当前石头查找已保留大小写语义 |
| [#36](https://github.com/SaltNya/GregTech6/pull/36) | 附属声明配方单方块机器及多方块机器，复用壁板和结构 | 接入注册、实体、能量、界面及 JEI/EMI 归属 |
| [#37](https://github.com/SaltNya/GregTech6/pull/37) | 模组/API 版本和弃用移除期限约定，当时为 0.3.0 | 采用栈最终版本 **0.7.0 / API 7** |
| [#38](https://github.com/SaltNya/GregTech6/pull/38) | 将附属配方机器带到共用平台线 | 合并传递，不重复增加功能或注册 |
| [#39](https://github.com/SaltNya/GregTech6/pull/39) | 将共用平台 Java 树带到 main | 合并传递；保留当前 Stonecutter 树 |
| [#40](https://github.com/SaltNya/GregTech6/pull/40) | 覆盖统计与 Stonecutter 可行性记录 | 历史评估不覆盖本分支已经完成的迁移记录 |
| [#41](https://github.com/SaltNya/GregTech6/pull/41) | 每加载器独立、仅公共接口的附属 SDK；禁止依赖内部实现 | 接入独立 API 源集、SDK、样例和 CI 校验 |
| [#42](https://github.com/SaltNya/GregTech6/pull/42) | 附属材料通过名称标识，不再占固定全局数字段；命名材料存储 | 接入，旧数字材料字段只作为既有存储格式读取兜底 |
| [#43](https://github.com/SaltNya/GregTech6/pull/43) | 附属行为机器外壳：槽、罐、开关与自定义 tick；GT 处理容器/能量/保存/掉落 | 接入双版行为机器注册与界面 |
| [#44](https://github.com/SaltNya/GregTech6/pull/44) | 声明流体燃料发电机及大型燃气涡轮，配置燃料图、效率、储罐和能量包 | 接入；不替换 GT 现有柴油机/燃气轮机实体 |
| [#45](https://github.com/SaltNya/GregTech6/pull/45) | 将上述最终附属 API 栈完整带入 main | 本批以其最终树为功能来源 |

## 当前实现位置与保留项

- `core/src/api/java/`：独立纯 Java 公共 API；`core/src/main/java/`：材料、声明与内部实现，不依赖 Minecraft。
- `multiversion/src/api/java/`：Stonecutter 管理的原生公共 API；`multiversion/src/main/java/`：双版共用实现；版本目录仅保留必要的平台差异。
- `gradle/addon-sdk.gradle`：每版 SDK、仅 SDK 的样例编译、API 引用闭合验证；NeoForge 样例明确使用 JDK 21，Forge 使用 JDK 17。
- `examples/addon-sample/{forge,neoforge}`：可编译运行的材料、配方机器、多方块、行为机器与流体燃料发电机示例；只在显式启用时加入开发运行，不进入正式模组或 SDK。
- `docs/ADDON_API.md` / `docs/ADDON_API.zh-CN.md`：最终公共接口说明。

完整保留本分支的 `gt6ce:` 注册/资源身份、`GregTech 6 Community Edition` 显示名、66 个旧重复身份清理和共用业务工厂。没有恢复 `gregtech:` 注册别名、旧测试机器、旧并行原生目录、旧兼容重映射或冻结清单重写。新增 7 个上游英文提示/界面键同时作为中英文包的英文兜底，原版 `gregtech.lang` / 汉化补丁绑定原文不改写。

## 整理时发现并修复的问题

1. 上游数据包覆盖先执行，普通炉到 GT 烤炉的镜像随后可能重新引入已经删除的 Furnace 配方，或覆盖自定义输出。现在镜像前撤销上次 Furnace 增量，镜像后仅重施 Furnace 规则；重复执行不累加，也不重新构建其它配方图。新增覆盖清空、精确删除、自定义输出、重复应用和撤销恢复的定向探针。
2. API 闭合校验必须检查泛型内层参数和类型边界，不能因外层是 `java.util.List` 就放过内部实现引用；增加相应拒绝案例。
3. 自定义 SDK 样例编译任务显式选择目标 JDK，避免 NeoForge 在 Gradle 的 Java 17 启动器下错误使用 `--release 21`。

## 验证与边界

双版主代码、公共 API、bootstrapGameTest 及 productionSmoke 源集均编译通过；Forge JDK 17、NeoForge JDK 21。最后一轮合并验证耗时 **3m32s**，含实际 NeoForge 定向服务端运行。完整结果见[验证回执](main-addon-stack-validation-20261018.json)，静态完整性见[逐项审计](main-addon-stack-completeness-20261018.json)。

- 41 个隔离核心合约进程全部通过，覆盖已有核心／机器／材料／热量合约和新增材料、机器、行为、燃料、配方覆盖案例。材料物性摘要不变；56,085 个内置材料物品 ID 摘要保持，增加附属材料不改内置身份。
- API 版本合约通过 84 项断言、7 项计划移除检查。独立编译只暴露公共 API：core 49 个 class，原生每版另 19 个，共 68 个。双版样例仅使用 SDK 与外部 Minecraft／加载器依赖编译通过；SDK 的当前字节码、API 闭合、ZIP CRC、许可证和无运行资源检查通过。
- Stonecutter 当前树和工具投影检查通过：Forge API 13／main 1082、NeoForge API 13／main 1141，完整源集无遗漏、重复或旧并行树；2758 份 Java 源码边界及严格双语检查通过。原始中文来源 125,676 键、12,704 个绑定保持，原有中英文值变更为零。
- NeoForge 21.1.243 开发 GameTest 服务器加载 **GregTech 6 Community Edition 0.7.0** 及 `addon_sample`，7 个选定命名空间共 **32 个必需测试全部通过**，测试执行 5.891 秒。覆盖配方机器／多方块、行为机器、流体燃料单方块／涡轮、材料命名存取／熔铸／扫描复制、动态提供者、小数信号以及新增 Furnace 覆盖修复。保存测试为受控实体存取，不等于真实关服后重启。
- 最后清理 93 份已改 Java 文件的混合换行与尾随空白；逐份与已编译的工具投影比较，代码及字面量签名保持。`git diff --check` 通过；没有因此重跑游戏。

前几轮在编译阶段暴露并修复了 NeoForge 样例 JDK、测试注解包和测试用 RecipeManager 构造器的版本差异。最终实际运行是第一次启动本批定向测试世界；没有重复运行全套测试。

未安排 Forge 实机、客户端 JEI/EMI 刷新及视觉验收、完整数据包重载／性能探针、全套 GameTest、真实旧存档迁移、完整生存链或成品客户端安装测试。本批没有交付新的正式模组 JAR；旧交付包不能代表此版本。GitHub Actions 结果需与本地验证区分。
