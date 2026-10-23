# GitHub Issues #1–#6 与 NeoForge 工具显示修复

日期：2026-10-03。工作树：`C:\Dev\GregTech6`。Forge 1.20.1 / 47.4.20，NeoForge 1.21.1 / 21.1.243。问题内容来自 [SaltNya/GregTech6 Issues](https://github.com/SaltNya/GregTech6/issues)，原版规则对照提供的 GT6 1.7.10 源码。

## 修复范围

| Issue | 修复 |
| --- | --- |
| [#1 斧子没有向上连锁行为](https://github.com/SaltNya/GregTech6/issues/1) | 双版 GT 斧与双刃斧只采收正上方同一种原木/巨型蘑菇；潜行、耐久不足、保护事件、不同方块会停止。通过玩家正常采收路径生成掉落；追加耐久逐格递增，高列降低挖掘速度。保留 TreeCapitator / Dynamic Trees 排除。 |
| [#2 原版配方替换缺失](https://github.com/SaltNya/GregTech6/issues/2) | 共享 76 条工作台配方表，由双版原生必选数据包覆盖 vanilla ID。原木徒手 2 木板、锯子 4 木板；熔炉要求打火物品；纸、书、瓶、铁金工具护甲、金属容器、指南针/钟、木棍等按原版重写。补齐指南针所需的磁化铁杆。GT 工具合成后返回并结算制作耐久。 |
| [#3 EMI 配方中工具不显示](https://github.com/SaltNya/GregTech6/issues/3) | NeoForge 工具白色 tint 改为不透明 ARGB，修复配方中的无材料组件工具及工具覆盖层/柄缺失。双版 JEI 将 `mSpecialItems` 在原版 `(80,43)` 坐标加入可见、不消耗的工具槽，显示副本不修改配方。 |
| [#4 游戏内看不到任何机器配方](https://github.com/SaltNya/GregTech6/issues/4) | NeoForge 新增可选的 EMI 原生插件：读取启用且未隐藏的机器配方，注册分类、工作站、物品/流体输入输出、材料别名和不消耗的工具。使用官方 EMI 1.1.24 API，成品不捆绑 EMI。机器 ID 使用 EMI 合成配方要求的前导 /，避免开发模式对十几万条记录做二次复杂度的 ID 检查；面板高度适配实际 GUI。 |
| [#5 液体泉材质错误](https://github.com/SaltNya/GregTech6/issues/5) | 双版液体泉从交叉面改为六面流体底图加固定泉覆盖层；方块实体同步储存流体并提供模型数据，物品读取打包的泉流体，按实际流体替换纹理和颜色。 |
| [#6 石油天然气不燃烧](https://github.com/SaltNya/GregTech6/issues/6) | 双版四种原油及天然气源/流动状态恢复原版可燃性 1000；相邻普通火焰或岩浆触发燃烧，消耗流体并按原版点燃附近方块。水类不参与燃烧。 |

本批第 2 项覆盖常用 vanilla 替换及铁金工具护甲表；现代木材采用原版木材规则。原版其它扩展工作台/加工行、可配置禁用选项和可选第三方配方兼容不计入本批完整移植声明。

## 验证方法

新增 `gregtech_issues` 游戏测试：同列伐木与递增耐久、潜行及耐久停止、高列速度、76 行实际加载、熔炉与锯子制作、泉更新包/模型数据、五种油气可燃性、原版打火石实际点火，以及不燃水类。另运行已有 `gregtech_fluid_channels` 47 项回归。

客户端探针使用实际库存模型和物品渲染器，检查 37 种工具的成品/配方占位栈、木柄纹理和 alpha，以及油/气/地热泉的六面流体与覆盖层。JEI 探针调用真实分类代码检查工具槽；EMI 探针除注册合同外，还在独立复制的测试世界中检查安装后的配方管理器、金属板产出查询及实际配方页面。测试源集和隔离世界不进入发布 JAR。

Gradle 的 Java 网络连接未能取得 EMI API 的 Maven 重定向；本机从同一官方 Maven 下载原始 POM/API/runtime JAR，并使用忽略目录中的 `work/emi-local.init.gradle` 指向验证用本地仓库。提交中的依赖仍为官方 Maven 坐标；此本机镜像不捆绑到模组。

## 已通过的运行检查

- 共享合同 8,107 条断言通过。Forge 16:19:43 / NeoForge 16:22:34 各 `All 56 required tests passed`（9 项新问题测试、47 项流体管回归），正常保存停止；合并任务 6m46s。
- NeoForge + JEI、未安装 EMI：74 个真实/配方占位工具模型、木柄纹理与不透明 tint、JEI 工具槽副本合同、三种泉库存模型通过；56,036 材料 / 925 流体最终模型缺失 0。16:25:40 正常停止，任务 2m14s；实际渲染截图已查看。
- NeoForge + EMI、未安装 JEI：真实安装后的管理器包含 187,788 条机器配方、73 类；轧机与挤压机的金属板产出查询通过。实际挤压机页面、模具、材料与功耗文字可见；客户端模型数据中的油/气/地热泉和实际世界渲染通过。16:49:05 EMI 重载完成（25,468 ms），16:49:11 第二轮排序完成（11,914 ms，与前一轮后半段并行），16:49:13 测试世界正常保存停止；任务 2m47s。

初轮 EMI 测试暴露合成配方 ID 诊断的性能问题，手动正常退出后修正。中间两轮证明管理器与页面可用，但截图仍有搜索/排序提示；最终探针同时等待 EMI reload 和异步 recipe worker 完成，才计入此处完整重载证据。EMI 开发模式仍报告原有标签翻译和压缩工作台输入的剩余物品索引等警告；本批没有把这些警告或所有原版工作台的 EMI 剩余物品展示声明为修复完成。

这些是开发客户端和游戏测试证据，发布 JAR 的实际安装启动、生存全流程和旧存档升级仍需独立验证。日志/截图哈希见 [运行回执](github-issues-runtime-20261003.json)。

## 六个独立代码提交

| Issue | 提交 |
| --- | --- |
| #1 | `fa04e698` — 向上伐木 |
| #2 | `5344e528` — 原版配方替换 |
| #3 | `c4fb0aaf` — 工具、配方工具槽渲染 |
| #4 | `73c11a85` — EMI 原生机器配方与重载 |
| #5 | `0c01aa63` — 液体泉完整模型 |
| #6 | `0cd15666` — 油气点火 |

## 双版本成品

正式 `:build :neoforge:build` 构建 7m38s 成功，未启用 direct-core 快捷路径。代码提交为 `73c11a8523c359095477b53d4dfe667649b0a37a`；后续仅提交验证文档。两包 589 个当前共享 class、平台元数据、许可、无重复和测试专用条目检查通过。额外确认 NeoForge 包保留原生 EMI 插件，但两包均不含 EMI 自身类或本批测试夹具。详见 [验包回执](github-issues-artifacts-20261003.json)。

| 成品 | 字节 | SHA-256 |
| --- | ---: | --- |
| gregtech6-1.20.1-forge-0.0.0.jar | 38,517,287 | `7ec7a7bb6d8deb1e69c0fd0f8a3d586364967c0df99b87b4b2ae3022f129dcb1` |
| gregtech6-neoforge-1.21.1-0.0.0.jar | 36,715,379 | `ff1d6c1de74080c8d72da26e12443483a3c70b6fec61f8001043ca0efa63e9a1` |

本机成品位于 `build/libs/` 与 `neoforge/build/libs/`。最终构建使用 Java 17 Gradle launcher / Java 21 NeoForge toolchain，命令为 `gradle --no-daemon --offline --console=plain -I work/emi-local.init.gradle :build :neoforge:build`。本机 EMI Maven 镜像说明见上文；普通联网环境使用仓库中的官方依赖配置即可。
