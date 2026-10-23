# 第一批真实机器平台移植

最新执行要求（2026-09-30）：优先快速合并三个项目的源码。暂停所有构建、测试、游戏启动及逐项验收，连续批量移植并处理直接冲突；新增代码明确标记未验证。原验收标准留待后续恢复，不据此把当前整合标记完成。


本批以实现为主，按要求集中验证。统一编译已通过1m33s（6305现有core断言、Forge main/bootstrap、Neo main）；Neo实际加工已通过：tick3011自然1357K得到4U青铜，tick3020四次正式锭完成，1 required，游戏阶段2.844s。Forge接线后的四项也在同一次Gradle通过，合计5m，XML零failure/error/skip；回执见verification/p2-first-machines-playflow.json。

## 单一领域来源

- 原项目1 MachineSpec/CrucibleSpec已迁core；InitialSmelteryDefinitions提供25%/16HU砖燃烧箱、陶瓷坩埚7U壳与显式密度、5U陶瓷模具。两平台注册同一规格，未由材料默认密度重建壳。
- CrucibleProcess抽取原真实BE的相变/蒸发/酸蚀判定、熔炼目标、容量与混温，保持投料→相变→选定合金反应→热步顺序。世界音效、方块移除与库存留平台；合金仍使用既有完整CrucibleReactions，未引入brokestar的替代合金引擎。
- ThermalStep用于Forge真实普通/继承坩埚及Neo坩埚。HU仍只从底面接收；模拟和执行都拒绝不可表示的包乘积、绝对值或累计溢出。负buffer现在按GT6有符号转换处理；正常正HU保留整数Kelvin、余量和100/10tick冷却窗口。未新增CU接收。
- MaterialFuelRules、CrucibleInputRules、MoldCastingRules与ManualToolRules共享原燃值/灰数、原矿量、模具冷却/容量和工具耐久算术。完整MoldShapes保留旋转、镜像、覆写及未知形状nugget fallback；blockSolid为领域token，实际方块仍平台。
- ItemComposition是原不可变组成record的共享抽取；ItemMaterialRegistry继续是平台Item/ItemStack身份与损耗/封装内容边界。93项原版物品身份/重量表只保留一份VanillaUnificationDefinitions；原vanilla_compositions.json逐字节移动到core资源，两平台解析相同原文。
- 完整107候选前缀材料物品定义与过滤/碰撞后缀由同一MaterialItemDefinitions提供，Neo注册真实MaterialItem，未另建铜锡青铜演示目录。

来源文件和SHA见core/provenance/first-smeltery-extraction.json，以及material-item-extraction.json、mold-shapes-extraction.json、thermal-extraction.json。项目1作者和原快照保留；其模组授权仍未解决，原第三方文本不改，发布门保持未通过。

## Neo平台范围

实际注册ID为gregtech:burning_box_solid_brick、smelting_crucible_ceramic、mold_ceramic，BE为solid_burning_box、smelting_crucible、mold。真实ticker消费煤/材料燃料与灰、直接邻接HU、掉落物缓存、共享材料与合金、凿模、浇铸、冷却和正式材料锭取出。Neo1.21拆分的useItemOn/useWithoutItem负责交互，GT.ToolStats移到CUSTOM_DATA且真实MAX_DAMAGE/DAMAGE负责首批凿子/钳子损耗。

HolderLookup.Provider参与ItemStack、ItemStackHandler存盘与网络包；保留gt.energy、gt.active、gt.inv、gt.materials、gt.cache及gt.mold.*等原键/单位。原版完整组成适配包含可回收/损耗与封装内容拒绝，1.21实体/容器/投射物组件不当空壳吞掉。

## 明确缺口和兼容范围

- 当前Neo只移植此三个正式器材和两个工具种类。其余工具、矿物/石种方块、blockSolid铸件、basin/crossing/faucet、多方块、通用能源/物流能力、盖板等仍待按依赖移植；未支持的blockSolid模具拒绝浇铸，保留原料。
- 完整生存取得铜锡、器材及工具制作、青铜工具使用尚未打通；给定材料/器材加工测试不能替代这些。
- 材料物品视觉正在移植。Neo机器动态壳/熔体/模具渲染、热/冻保护和附近着火副作用、扳手/螺丝刀/软锤/铲子交互仍未完整对齐。不能称Neo与Forge所有机器行为等价。
- 已保留NBT方法和物品ID，尚未实测机器世界保存→独立进程重载，也未实测项目1旧存档跨版本升级。负buffer/非法输入拒绝是显式行为修正；没有承诺损坏旧数据兼容。
- ItemMaterialRegistry原嵌套ItemMaterialData改为共享ItemComposition，六处本项目调用已适配；外部编译插件需适配该Java类型变化。这不更改已保存物品材料ID或gt.*的存盘格式。
- 此前dee74双jar回执是历史材料快照。本批成品打包、生产加载、世界内客户端和远程CI仍待后续验收。

## 验证节奏

不为每个抽取增加测试或重启游戏；一批实现后集中编译与关键加工检查。Forge沿用真实煤→四锭青铜/容量断热及两项热边界，Neo追加一个真实煤→四锭青铜流程检查。首个Neo运行入口因测试辅助API旧名编译失败18s，未启动Minecraft，修正后执行；失败日志保留。各版本存档与客户端验收在功能可操作后集中进行。
