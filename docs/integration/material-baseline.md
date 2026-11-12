# 材料链路基线与正确性对照

本页记录[计划 01](../development/material-system-refactor/01-baseline.md)的观察口径、已审阅的历史引用和可重复测试入口。当前行为快照用于发现迁移变化；正确性依据另列，不能通过重录快照认可缺陷。

## 范围与状态

源码基线为 `2e5627aa9da706416251ae83538a62cfc07a8783`，调查日期为 2026-11-07。本次新增测试、测试数据和文档，并独立清理形态表误抽取的一个局部变量键；没有改变实际材料、配方、初始化顺序、API 或持久化格式。开工已有未跟踪的重构计划和设计提议，均保留。工作期间外部修改 `.gitignore`，新增 `/docs` 忽略规则，也保留；新增文档因此不会自动出现在 Git 未跟踪列表中。

状态：**在下述范围验证通过**。core 的独立 JVM 重复采集、SPI、归一化、引用审计已执行；双平台生产及测试源码已编译。双平台各 79 个 GameTest 已通过，包含材料领域 8 例；初次启动和真实重载分别采集。同平台两次全新启动的成分、物品／方块形态、配方和标签语义投影均逐文件一致；不把数量视为全部内容的正确性结论。游戏客户端展示和真实外部附属不在本阶段自动验收范围。

运行环境：Minecraft 1.20.1／Forge 47.4.20 与 Minecraft 1.21.1／NeoForge 21.1.243，GT6CE 1.0.6，Gradle 8.8，Stonecutter 0.7.11。core／Forge 工具链为 Java 17（Dragonwell 17.0.19），NeoForge 为 OpenJDK 21.0.12.1；Gradle 启动 JVM 为 21。

02 将原先位于 `MaterialRoleFlags.apply()` 的世界生成和石材最终角色前移到材料 READY 前发布。因此后续 core 材料快照会比本页最初采集多 87 个最终派生属性：79 个 `GENERATE_ORE`、3 个 `GENERATE_ORE`／`GENERATE_ORE_PROCESSING` 组合和 5 个 `STONE`。旧流程同样在物品注册前写入这些 flag；双平台物品、方块形态、配方、tag 和代表案例投影保持逐文件一致，它们不是 01 基线之外的新运行行为。

## 三种证据

| 类型 | 存放位置 | 用途 |
| --- | --- | --- |
| 当前行为 | 根目录 `work/material-system-refactor/core/`、`runtime/<MC>/<运行目录>/` | 全量材料、别名、引用、形态、成分、配方及代表案例；不提交原始 dump |
| 权威期望 | `MaterialBaselineTest`、`MaterialBaselineTests` 中注明依据的断言 | 固定 GT6 事实、现有 parity 决策、公开 SPI 契约 |
| 已知差异 | 本页、测试中明确标记的 observation、测试资源中的历史引用清单 | 让后续修改显露差异；不赋予当前错误行为正确性 |

core 快照在独立 JVM 中各初始化一次，不提供生产 reset。无附属环境连续采集两次，比较材料、prefix、别名和引用四份排序后的 TSV；第三个 JVM 通过临时 ServiceLoader 资源加载测试附属，并验证内建材料投影不变。游戏测试附属由 test source set 的 SPI 服务文件发现，不要求安装真实联动模组。

材料投影区分原始对象、`resolve()` 后身份与有效性；内建 ID 保留，附属数值 ID 写为 `dynamic`，用所有者和注册名表示稳定身份。属性、别名和无语义集合排序，化学组成与副产物的顺序保留。物品组件排序前单列主材料；prefix 重量与实际生成物品分开，不生成全部材料与 prefix 的笛卡尔积。

配方按 map、槽位、物品／流体 ID 和数量、tag、催化剂／presence、unit 数量与舍入、输出概率、时长、EUt、special 数值和准入标记构造语义签名。保留重复签名的数量，不把规则集合转成去重集合。unit 只记录声明与代表身份，不展开全部替代形态。tag 输入使用 tag 身份与数量，不记录无序集合选出的展示物品；另存所有被配方引用的 tag 的排序成员列表，所以 tag 内容改变仍可见。首次比较中 Forge 的 310 条和 NeoForge 的 400 条 Bath/Mixer 展示代表差异据此消除，没有忽略成分、授权或产量。

局限：配方签名不完整序列化任意物品 NBT／组件、任意特殊对象和谓词实现，因而不是通用配方等价判定器。损坏、铸模形状和储存内容的实际准入由定向案例另外验证。形态投影覆盖已生成物品，同时记录已生成 `MaterialBlockLike` 方块的 prefix、单位重量、方块／物品 ID 与可生成性；未生成候选及全部原版替代映射仍需后续形态审计。新增字段须说明验证的外部行为。

## 引用审计

`MaterialReferenceAudit` 检查已提交的组成声明、分解 ID/name 行、形态表、UUM ID 集合和七类加工目标；组成表采用当前 literal bind 语法，并核对解析行数，语法变化不能静默漏行。仅看注册后对象会遗漏被 `bind` 跳过的输入，所以源表和有效图都检查。

首次审阅 4,069 条引用后，移除 1 条误抽取行。最终审计 4,068 条：2,032 条直接有效、1,729 条可映射别名、307 条已解释的历史／停注引用；当前悬空引用与 ID/name 矛盾均为零。这些数量仅描述本次观察；测试不把固定总数当作永久容差。

历史分类依据如下：

- 有数值 ID 的停注材料从[现有移除决策](../decisions/parity/material-removal.md)读取，不能因为查不到就自动允许。
- 归一化后的 GT6 字段别名与当前注册名分别记录。能识别别名不代表生产 `MaterialForms.of` 一定命中了对应 fallback。
- `material-reference-history.tsv` 逐条限定表、owner、字段、目标和 ID，并给出固定 GT6 的 `unused`／`deprecated`、未分配 ID 的 `valgem(0)` 或现有移除决策。它只标注已确认的历史行，不创建第二份生产材料事实，也不允许同名新引用出现在其他字段。
- `MaterialForms` 的 `rmaterial` 来自 GT6 `MT.create` 的局部变量 `rMaterial`，不是材料。作为独立小清理，仅删除提交表中的该行，然后开启“当前未知错误必须为零”的严格断言；没有用期望文件豁免它。原生成脚本不在仓库，固定 GT6 原始输入已核对，未臆造可再生成流程。此键没有有效材料和 `MaterialForms.count` 生产调用者，不涉及任何材料／物品移除、ID 或存档迁移；后续恢复生成器时须排除局部变量。

新增悬空引用、ID/name 矛盾、非法物品组件会失败。负例直接构造缺失材料引用、铁 ID 配铜名和负数材料量；缺失引用异常包含表、材料、字段和目标。另在 `work/` 编译独立负例探针，注入假引用后进程确实以 1 退出；正常输入的完整 core 测试随后恢复通过；负例未改动生产输入。历史清单不豁免这些负例。01 阶段保留的 `MaterialCompositionData` 等生成表因缺少可复现脚本未重写；02 随后将其运行时事实迁入规范目录并删除旧写入入口。

## 权威依据与已知差异

上游为 [GregTech6/gregtech6 固定提交](https://github.com/GregTech6/gregtech6/tree/3703e40308c8c030763fd6297dea8b210d2a77b1)，LGPL-3.0-or-later。原始输入主要为 [MT.java](https://github.com/GregTech6/gregtech6/blob/3703e40308c8c030763fd6297dea8b210d2a77b1/src/main/java/gregapi/data/MT.java) 与 [OP.java](https://github.com/GregTech6/gregtech6/blob/3703e40308c8c030763fd6297dea8b210d2a77b1/src/main/java/gregapi/data/OP.java)。引用的是该提交原始文件行号，不是网页重排后的行号；一次性下载和调查脚本留在 `work/`。

| 案例 | 依据／期望 | 当前观察与归属 |
| --- | --- | --- |
| Fe／Cu 身份 | GT6 `MT.Fe`／`MT.Cu`，沿用 260／290；别名指向同一身份 | core 正确性断言；不以材料总量作为通过条件 |
| 石墨熔炼 | [已接受的 1U 碳决策](../decisions/parity/README.md#其他差异)优先于 GT6 的 0.5U | core 断言 1U，避免把旧 GT6 数值当作当前项目期望 |
| 复合钻头 | GT6 `OP.toolHeadDrill` 的 4U 主材，`OP.java:717` 的 4U 钢副组分 | 注册表、实际栈、等价形态和坩埚负载分别检查；形态查询仍只代表主材料 |
| 铁存储块 | GT6 的存储块为 9U；现代 MC 铁块由既有映射承接 | 成分量、回收授权、熔化授权和实际坩埚解析分别采集 |
| 铜存储块 | 现代内容按两平台交集统一为 9U Copper 修正成分 | 两端均 9U Copper、坩埚可用且回收工艺准入一致；版本独有铜变体按物品存在性跳过 |
| 附魔金苹果 | GT6 `LoaderItemData.java:2502` 的 72U Gold；`MaterialRecoveryRules.dust` 是当前规划入口 | 已观察规划 64U，最终 Shredder 也产出 64U；属阶段 05 的待修差异，不是认可丢弃 8U |
| 损坏工具 | 成分按剩余耐久向下取整，Shredder 从剩余成分重建实际输出 | 完好、部分损坏、耗尽逐项观察；原生 GT 工具实例缺少注册表成分另列，不能用原版铁镐代替其验收 |
| 空桶／水桶 | 坩埚不得把有内容的容器当空壳消耗 | 空桶可熔，水桶被拒；未覆盖所有第三方容器 |
| 已凿铸模 | 形状与材料组成是不同事实 | 石铸模成分不变，但形状的 block entity data 被当作储存内容而拒绝；阶段 04／05 承接 #349 |
| 压模机模具 | 注册存在性与回收可用性分别检查 | 小号弹壳模具已注册但成分缺失，承接 #350；不以掉落形状测试代替 |
| 钼钙矿分解 | [#318 已接受电解规则](../decisions/parity/README.md#其他差异) | 用 mode 0 和 6 个粉查询实际配方；水分解另用流体输入查询 |
| SPI 材料 | 公共 `AddonMaterialContributor` 契约 | core 隔离 JVM 与游戏测试夹具分别检查所有权、链接成分、生成 dust 及离心配方 |
| 资源重载 | `MinecraftServer.reloadResources`，由真实 tag 生命周期发布 overlay | 独立顺序批次等待两次真实重载，比较 ExternalOreProcessing／SharedThermalInputRows 的拥有规则数、固定样例和查询；不以直接 `rebuild()` 冒充，也不宣称所有生成器已覆盖 |

本阶段没有改动 GT6 行为或删除材料，因此没有新增生产 parity 取舍。阶段 03 已按固定 GT6 源裁决 Phosphor 不带 `MELTING`，并修正特殊木材燃料／灰烬；相应回归维护在 `FuelParityTest`。


## 双平台交集与重载差异

同名物品按注册 ID 对齐；比较 prefix、主材料、全部组件及数量、授权、两类准入、储存内容判定和实际坩埚负载。来源说明文本、原生耐久上限和版本独有物品不作为成分漂移；它们保留在原始文件中。

| 范围 | 观察 | 处理 |
| --- | --- | --- |
| 成分条目 | Forge 35,226，NeoForge 39,043；交集 35,206，单端分别 20／3,837 | 两次同平台启动一致；单端独有只另列，不直接判错 |
| 交集差异 | 421 个物品 | 有限三类：383 个 `gt6ce:ore_*` 的 prefix 元数据、32 个铜材变体、6 种生物桶内容标记；数量不是永久豁免 |
| 矿石 prefix | Forge 为 `oreVanillastone`，NeoForge 未填 prefix | 成分量与坩埚负载相同，留阶段 04 统一元数据 |
| 铜材变体 | 32 个授权不一致，其中 26 个另有成分量／蜡组分差异 | 阶段 04 按现代 MC 规则裁决；不能批量用某一平台覆盖另一平台 |
| 生物桶 | NeoForge 默认 entity 组件使 `hasStoredContents=true`，Forge 为 false | 6 种桶两端均不可熔、不可回收；记录检测路径差异 |
| 物品／方块形态 | 两端均 30,654 个实际物品、3,560 个 MaterialBlockLike 方块 | 同平台初次／重载及重复启动一致；不以总量声称形态完整 |
| tag | 486 个被机器配方引用的 tag | 成员排序后，同平台初次／重载及重复启动一致 |
| 机器配方 | Forge 初次／重载均 155,825；NeoForge 初次 136,982，重载后 136,596 | 同阶段跨独立启动一致；NeoForge 少 386 条 Fermenter 配方需单列生命周期调查，不据此减少期望或宣称全图重载无差异 |

NeoForge 的 386 条移除全部来自 `gt.recipe.fermenter` 的药水相关行；当前纯签名没有完整表示药水组件，计数不代表 386 种不同药水。这里只确认“初次有、真实重载后无”的现象，不在基线阶段修改发布顺序或复活已移除的药水系统。后续清理须先核对[药水移除决策](../decisions/parity/potion-removal.md)与生产者生命周期。

## 复现与执行记录

所有命令从仓库根执行，标准命令与注意事项见 [JUnit](../development/validation/checks/junit.md)、[编译](../development/validation/checks/compile.md)、[GameTest](../development/validation/checks/gametest.md)和[格式](../development/validation/checks/format.md)。定向入口为：

```bash
./gradlew :core:test --tests 'gregtech.content.material.MaterialBaselineTest'
./gradlew :minecraft:1.20.1-forge:compileTestJava :minecraft:1.21.1-neoforge:compileTestJava
```

GameTest 使用检查文档已有的 `gameTestDirectory` 指定不同隔离目录，避免第二次启动复用第一次的世界或配置。重复启动后逐文件比较排序的 TSV；跨版本物品以注册 ID 交集比较，版本独有物品另列，配方差异只作调查线索。

默认开发环境会加载 JEI/Jade。无可选模组采集仅从 `runtimeOnly` 移除这两个查看器，保留全部编译 API，并核对实际加载列表只有 Minecraft、加载器和 GT6CE。复现时把下面内容保存到本地 `work/material-system-refactor/no-optional.init.gradle`：

```groovy
gradle.afterProject { project ->
    if (project.path.startsWith(':minecraft:')) {
        project.configurations.findByName('runtimeOnly')?.dependencies?.removeAll {
            it.group == 'mezz.jei' || (it.group == 'maven.modrinth' && it.name == 'jade')
        }
    }
}
```

实际最终采集命令如下；`--offline` 要求依赖已缓存。网络／Java 代理的个人配置按[开发上手](../development/getting-started.md)准备，不写入生产构建：

```bash
./gradlew :minecraft:1.20.1-forge:runGameTestServer --offline -I work/material-system-refactor/no-optional.init.gradle -PgameTestHeap=4g -PgameTestDirectory=../../../work/material-system-refactor/forge-final-d
./gradlew :minecraft:1.20.1-forge:runGameTestServer --offline -I work/material-system-refactor/no-optional.init.gradle -PgameTestHeap=4g -PgameTestDirectory=../../../work/material-system-refactor/forge-final-e
./gradlew :minecraft:1.21.1-neoforge:runGameTestServer --offline -I work/material-system-refactor/no-optional.init.gradle -PgameTestHeap=4g -PgameTestDirectory=../../../work/material-system-refactor/neo-final-d
./gradlew :minecraft:1.21.1-neoforge:runGameTestServer --offline -I work/material-system-refactor/no-optional.init.gradle -PgameTestHeap=4g -PgameTestDirectory=../../../work/material-system-refactor/neo-final-e
```

已有目录不要作为下一次干净启动的目录；改用新名称，不删除他人的配置或世界。最终同平台两次采集的 25 份 TSV（含 15 份代表案例）均逐字节一致。对于同版本两次输出，逐字节比较 `startup`、`reload` 的 `items`、`forms`、`block-forms`、`recipes`、`tags` 五类 TSV，十份均须一致。跨版本则按物品 ID 求交集，比较列 2—9 的 prefix、主材料、成分、授权、两类准入、储存内容判定和坩埚负载；其余版本独有 ID 另列。

没有关闭检查或修改生产打包配置。环境、完整命令输出和原始结果保存在 `work/material-system-refactor/`，不随仓库提交。

| 检查 | 结果 | 覆盖／限制 |
| --- | --- | --- |
| D 路由与文档 | 已通过路由检查与新增链接复核 | `python tools/validation/route.py check` |
| J core | 已通过完整 93 个 JUnit 测试，包括新增 4 个基线测试 | 独立 JVM 重复采集、测试附属、负例、有效图和源表引用 |
| C | 双平台生产编译及共享测试编译已通过 | GameTest 同时复验最终测试 SPI 夹具 |
| F | 格式化和全仓 spotlessCheck 均通过 | 不扩大排除范围 |
| G Forge | 两次全新启动均 79／79 required tests 通过 | `forge-final-d`／`forge-final-e`；十份投影逐字节相同，包含 SPI 和每次两次真实重载 |
| G NeoForge | 两次全新启动均 79／79 required tests 通过 | `neo-final-d`／`neo-final-e`；十份投影逐字节相同，核对游戏日志实际测试数 |
| M／B | 尚未执行；本阶段未改生产资源或打包配置 | 未宣称客户端或发行包已验收 |

网络无 Java 代理时首次 Gradle 解析超时；离线缺 EMI 缓存时两平台编译被阻塞，随后通过环境既有代理恢复。另一次复用 Forge 运行目录时 `GT6CE/common.toml` 解析失败，加载器仍返回 Gradle 成功；该次不计为 GameTest 通过，保留日志并使用全新目录复验。

验收分类：**执行且通过**为 D、F、J、C、双平台 G、独立负例与重复采集。**执行但失败或阻塞**包括早期在线依赖超时、离线缓存缺失、两处样例选择错误，以及复用目录的配置解析失败；均已用上述最终输入和全新目录复验，不存在未解决的执行阻塞。**尚未执行**为客户端手工操作、真实外部附属环境及发行包检查；本阶段没有改变生产资源或打包配置。已知生产差异不列作“正确性通过”。
