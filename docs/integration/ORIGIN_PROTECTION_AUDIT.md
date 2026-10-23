# Nexus、道路与普通生成的区域保护核查

2026-10-22，第一百零七批，基线 `ff8c9fac0`。本批只整理源码并记录差异，保持用户要求的 Nexus 模块默认全开，不实现 CFG、不改世界生成策略。#71–#74 跳过、#66 CFG 后续、#68／#69 视为一个问题的约束不变。

原版 10 份 Java、当前 14 份 Java、24 份平台／core 视图和 12 项资源组成这条证据链；23 段具体引用、四个原生条目的重新验摘要和排版证明见[回执](verification/origin-protection-audit-20261022.json)。归档整个文件供追溯，不代表其中每个功能已全面验收。原树只读，保留其版权／作者与 LGPL 说明。

## 两项确认差异

| 编号 | 原版实际消费者 | 当前入口与影响边界 |
| --- | --- | --- |
| ALIGN-359 | 启用道路时，`WorldgenObject.checkForMajorWorldgen` 对区块起止坐标取绝对值，任一轴小于 64 即避让；启用中心群系时再排除中心 12×12 区块。`WorldgenPit.generate` 和 `WorldgenRocks.canGenerate` 实际调用它。 | 当前 `GTPitFeature.place`、`GTRocksFeature.place` 直接进入生物群系／随机条件，没有相应区域判断。比如区块起点 `(32,1024)` 原版避让，而当前 Origin 根本不处理该区块，因此不能用“道路最后会覆盖”解释这一路缺口。满足各自其余条件时仍可尝试生成；不是断言每次都放置成功。 |
| ALIGN-360 | `GT6WorldGenerator` 先对全部 normal 项调用 reset，再运行生成；启用中心群系时，`WorldgenCenterBiomes.reset` 在 minX／minZ 为 −96…80 的 144 个区块置 `GENERATING_SPECIAL`。石层、小型矿和液体泉三个入口实际检查它。 | 当前 `GTStoneLayerFeature`、`GTSmallOreFeature`、`GTFluidSpringsFeature` 没有这项中心保护；独立 Origin 在后续阶段重写一部分地形，不能代替原本“不尝试生成”的合同。记录的是入口／调度差异，不把被随后重写的拟写入算作最终可见错误。 |

原版上述规则以对应模块开关为前提；原版默认关闭中心模块，不能把这个默认值和用户要求当前全开混为一谈。后续补协调应使用适合现代并行区块生成的区域决策，不能直接复制旧静态全局标志。

## 有界检查及排除的误报

从源文本提取布尔条件，在 −128…128、间距 16 的 289 个区块起点上作几何投影：旧地表避让 224 个、旧中心标志 144 个、当前 Origin 入口 164 个，其中 60 个旧避让区块根本不进入 Origin。这是有限表达式求值，不是原生 Feature、真实随机或世界密度测试。

当前基岩矿的候选规则要求两轴区块坐标模 16 为 8；上述 144 个中心区块全部不满足。因此本批没有声称当前基岩矿也会在中心漏生成。这个已有排除效果和原版 reset 机制不同，应分别记录，不能因为没找到相同标志名就报所有矿都失去保护。

两版资源把石层／矿放在 `underground_ores`、泉放在 `underground_decoration`、石子放在 `vegetal_decoration`、Origin 放在 `top_layer_modification`。重新核验的实际 Forge 枚举／生成器反汇编和 Neo 源码确认这些阶段顺序。黏土堆与 Origin 同属最后阶段；仅凭 `zz_gt_origin` 文件名不能证明二者先后，本批没有作这种推定。

已有正确接线也保留：

- `NativeOriginWorld.canWrite` 限制本区块和实际建造高度，世界高度接口不是固定 Y0。
- `biome` 实际填充区块群系调色板，并非空实现。
- 创建世界的出生点 `seaLevel+8` 与布局高度 `seaLevel+3` 再加原版 `+5` 一致。
- 原道路清理的是非玩家生物，当前 `LivingEntity` 过滤与这个目标对应，不误报为少清理了全部实体。

## 源码整理与验证边界

五份源码由 397 行展开为 563 行：`OriginWorld`、`OriginSupport`、`GTOriginFeature`、`NativeOriginWorld`、`OriginSpawnEvents`。展开压缩方法、分支、状态映射和版本适配，补常量间距并保持现有声明顺序；两条来源／适配说明更准确地区分共享布局、区块写入与邻区读取。

八份完整执行视图的有序代码 token、原始字面量及其位置、其余注释和 Stonecutter 指令均保持；没有删除方法或更改公共签名。两个用户材料文件的字节摘要保持。这里的等价检查是排版保护，不代替 Java 编译或游戏行为验收。

未运行 Gradle、Java 编译、GameTest、游戏、存档重启、构建或推送；未回复、关闭 issue。区域协调、中心布局的其余平台适配及其它全系统消费者仍待继续。正式差异账本累计 360 条，含已修和保留策略，不是未修数或完成率；全面 goal 保持 active。
