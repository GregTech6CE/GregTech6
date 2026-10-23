# 2026-10-18 / GT6CE 内容命名空间与模组归属显示

当前状态：源码／资源迁移、双版构建、四组核心合约、NeoForge 开发新世界与最终正式包／SDK 验包全部通过。交付分支为 `experiment/stonecutter`，main 未合并；开发运行与成品安装运行的证据保持区分。

## 范围

要求将内容与资源 ID 也改为 `gt6ce:`，并将物品提示中的模组名称显示为 `GregTech 6 Community Edition`。本批在 `experiment/stonecutter` 分支执行以下迁移：

- 物品、方块、流体、配方、标签、模型、声音、网络等自有身份统一使用 `gt6ce:`；加载器 ID 仍为 `gt6ce`。
- 共用与版本专有资源的 9 个 `assets/gregtech`／`data/gregtech` 根目录改为 `assets/gt6ce`／`data/gt6ce`，资源引用及已有结构数据同步。
- 移植版翻译键同步改为 `gt6ce` 前缀；原版 `gt.*` 等语言来源键、文本、提取快照和压缩汉化源保持原样。
- 材料拥有者、原生物品归属映射和 EMI 通过新命名空间找到同一模组容器，显示完整产品名称。
- 活动生成器、自检、验包及当前使用指南同步；验包明确拒绝旧资源根目录和资源 JSON 中的 `gregtech:` 引用，包括转义字符和 `item:gregtech:` 嵌套标识。

Java 包 `com.gregtech.gregtech`、原作者路径与 URL、`META-INF/gregtech6` 来源／许可记录、Mixin 文件名、测试内部协议和冻结迁移清单保持独立身份。此前要求不保留旧重复身份，本批也不添加 `gregtech:` 到 `gt6ce:` 的存档或注册兼容映射；引用旧 ID 的世界与脚本需要另行迁移。

## 静态与核心验证

资源检查统计 79,761 个受管理资源文件，解析 61,195 个 JSON／mcmeta 文档；未发现遗漏迁移文件、活动旧命名空间引用或结构 NBT 旧 ID。双版模型引用图没有缺失模型，3 个叶子贴图缺失已确认在迁移前存在。

严格双语检查通过：原始汉化源 125,676 键、移植绑定 12,704 条、英文 15,905 键、中文 141,732 键，差异为零。原始汉化源 SHA-256 为 `0e5b3906712020b71c9c5718319bbf3fd90b002d79d25614f1bfcaf3c8e887c0`。全部固定英文声明的来源行与文本未改动；451 份 REMI 静态分组资源与生成器一致。

2,647 份 Java 源码的边界／语言检查、399 份 Python 工具的语法检查，以及 54 项语言、身份与验包失败用例通过。首次工具用例因沙箱临时目录权限失败，允许在项目 `work/` 创建和清理临时目录后重跑通过；该失败不是业务断言失败。

四组核心合约分别通过：核心行为 2,809 项、机器参数 8,274 项、材料 7,947 项、热量 53 项。材料合约最初因摘要包含旧身份而失败，未直接放宽断言：使用未修改的 HEAD 材料观察器，隔离比较变更前后各 155,935 行，定义和后初始化阶段均只有 1,160 条材料记录的身份字段改变；归一化这些身份字段可还原旧摘要，物性和材料形态无变化。更新摘要期望后材料合约通过。详见[身份差分回执](gt6ce-material-differential-20261018.json)。

原始构建日志 `work/gt6ce-namespace-20261018/build-with-empty-directories.log` 记录双版主代码、探针、正式包和 SDK 生成成功，耗时 7m38s。它是修复空目录前的构建证据，不能代替最终验包。

## NeoForge 开发运行

一次 NeoForge 1.21.1 开发新世界运行通过，整体任务 3m11s，安装 EMI 1.1.24 与 Reliable EMI 4.7.12。完整[运行回执](gt6ce-namespace-runtime-20261018.json)保留开发环境范围，原回执位于 `multiversion/versions/1.21.1-neoforge/build/world-creation-smoke-run/world-creation-e56a998d-1d6a-4801-8e7e-6de9146ca040.json`。

| 检查 | 实际结果 |
| --- | --- |
| 物品／方块／流体注册样本 | `gt6ce:copper_key`／`gt6ce:clay_brown`／`gt6ce:seawater` |
| 对应旧命名空间样本 | 均不存在 |
| 材料拥有者 ID | `gt6ce`，实际 owner 映射一致 |
| 加载器、材料拥有者和真实 EMI 名称 | `GregTech 6 Community Edition` |
| REMI 运行分组 | 观察到 434 个有成员的组；静态资源共 451 组 |
| 全部列表／搜索 | 分组折叠通过，搜索索引重载完成 |
| 保险箱展开／收起 | 60 个成员通过 |

该运行使用开发类路径，没有安装本批最终 JAR。它证明当前开发内容的注册归属、显示名称及指定查看器流程，不能扩大为全部物品渲染或完整玩法的验证。

## 最终构建与验包

首次验包拒绝了 Forge 1,420 个和 NeoForge 1,699 个旧命名空间空目录项，旧文件均为 0。根构建对所有 JAR 设置 `includeEmptyDirs=false`，以避免增量资源迁移后的空目录进入正式包；没有放宽验包规则。

最后一次 `verifyStonecutterSources`、Forge `distributionJar`、NeoForge `jar` 和 SDK 构建 7m24s 成功，记录于 `work/gt6ce-namespace-20261018/build-final.log`。[最终成品回执](gt6ce-namespace-packaging-20261018.json)核对了当前 818 个共享类、Forge 1,514／NeoForge 1,612 个原生类，68／123 个事件订阅及双版各一个实际模组入口。身份常量、元数据、蓝色引擎页脚、全部当前字节码、双语资源、ZIP CRC 与 SDK 均通过。

Forge 61,081／NeoForge 60,260 份成品 JSON 检查通过；两个正式包中均没有 `assets/gregtech/`、`data/gregtech/` 目录或旧资源引用，转义形式亦检查。

| 成品 | 字节数 | SHA-256 |
| --- | ---: | --- |
| `gt6ce-1.20.1-forge-0.0.0.jar` | 40,019,454 | `cad9ef723d90cd5055d3e4e4b2b82bb28c1cf9237ba133238b01339809965b3f` |
| `gt6ce-neoforge-1.21.1-0.0.0.jar` | 39,869,574 | `80045e1900cf19cc11fd6ae9cfc05a764727ea16c2c9d6dcddb6bf40938b849d` |
| `gt6ce-addon-api-0.0.0.jar` | 1,920,643 | `e0889717a6e5bbd19837dc25db7fa60ff86e84d3592b71729865fad382cd5de7` |

原始输入边界另作隔离对照：基础配方 1,061 次、多方块 834 次转换与 HEAD 的差异仅为输出 namespace；7 份原始提取快照未改。见[源输入适配审计](gt6ce-snapshot-adapters-20261018.json)。分支提交与交付包的对应关系由 `build/delivery/` 中的 `build-info.json` 记录。

## 本轮边界

未验证 Forge 游戏运行、专用服务器、新成品安装启动、旧世界真实保存重载、跨版本存档升级或完整生存流程。静态检查发现的 3 个叶子缺图及运行中报告的 4 个非法纹理文件名是此前已存在的问题，本批没有扩展修复范围。未以冻结迁移清单重写历史签名，也没有新增旧命名空间兼容入口。
