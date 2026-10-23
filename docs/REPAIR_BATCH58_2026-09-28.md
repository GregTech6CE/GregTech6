# 第五十八批：大型电池盒、十档系列与变压器制作链

## 已落地

- 普通电池盒由六档扩展为十档四槽；大型电池盒为十档十六槽。保留旧 energy_storage_lv..iv 注册 ID，把原来的免费大容量占位改成真实电池库存，其余五档补注册；合计新增九个电池盒方块。
- BatteryBoxDefinitions 单独维护原版电压、材料、槽数与名称；EnergyNodeSpec 增加 batterySlots 元数据，注册和库存不再依靠 battery_box_ 前缀识别。大型盒共用已经接通的逐槽充放电、控制面板、保电取出/掉落、Jade 统计。
- 材料键依次为 TinAlloy、SteelGalvanized、Aluminium、StainlessSteel、Chromium、Titanium、Iridium、OsmiumElemental、Trinitanium、Trinaquadalloy（实际显示名以 GregTech.lang 为准）；最高档代码 ID 保留 xv，但显示原版 PUV1。
- 精确注册 16 槽 MenuType。修复旧 case 16 玩家库存 Y=114 与背景 Y=84 不符；第一个电池槽为 (53,8)，最后为 (107,62)，玩家背包 Y=84、快捷栏 Y=142。隐藏会压住首行的标题，16.png 改为原版文件。
- 普通和大型各十档，共 720 个方向/缓冲状态/含水组合；复制两套原版覆盖层及动画。29 个电池盒/变压器中文名读取 GregTech.lang。
- 电力变压器从五档扩展为九档，对应原版 10040–10048，补四个注册与资源。容量恢复为高侧额定包的两倍。
- 修复变压器沿用普通转换器扣能造成的升降压能量不守恒。正常方向从正面收能，向其余五面发四个小包；反向从其余五面收能，向正面发一个大包。实际发出多少 EU 就扣多少 EU，无接收者保留缓冲，拒绝任意拉取。活动扳手反转时清空缓冲。
- 新增 23 条可解析制作配方：九档变压器、ULV–LuV 七档普通盒、七档大型盒。大型盒中央必须使用对应变压器；高档石墨烯 C 位遵循原版使用裸线。电路通过同档或更高档标签；新增 tier_0_plus 指向已有高档替代。

## 原版依据

- Loader_MultiTileEntities.java:879–895：九档变压器及十档两类电池盒，两个配方形状分别为 WIW/XMx/WIW 与 WCW/WCW/XMX。
- MT.DATA.Electric_T、WIRES_01/04、CABLES_01/04：各档材料与线缆类型。
- TileEntityBase10EnergyBatBox：4/16 槽、真实物品储能及工作缓冲。
- TileEntityBase11Bidirectional、TE_Behavior_Energy_Converter：双向包大小、倍率、实际扣能。
- MultiItemTechnological.java:700–709：原版原生通用电路明确注册 T1–T6，Magic/Enderium 为独立类别，不能凭名称当 T7/T8 使用。

## 验证记录

- compileJava 已通过；初始三项资源检查通过。
- 额外资源审计发现 16.png 与原图不同，并结合图片/代码发现玩家背包错位。第一次完整门禁主动终止，未算通过；该世界不再复用。
- 修复后 6 项 Python 资源测试通过，日志 build/assets_batch58_final.log：20 种盒的 720 个状态/着色层，两套原图/动画和 16 槽 GUI 字节一致，29 个原版中文名，23 条配方形状与依赖，选择性生成器幂等。
- LargeBatteryTransformerTests 新增四项：20 盒实际库存与空容量、最后一槽及电量的稀疏存档和 GUI 坐标、三级实际世界升降压能量守恒、断路保能/反转清空/23 条配方加载。
- 第二轮完整门禁发现旧 ordinaryElectricTransformerKeepsExistingRules 仍要求错误的通用转换器方向和反转保能；已按原版更新为 ordinaryElectricTransformerUsesOriginalBidirectionalRules。该轮未算通过，重新使用第三个新世界。
- 最终完整门禁 build/gametest_batch58_large_battery_final.log：2026-09-28 05:09:02 **1005/1005 required tests passed**，退出码 0，BUILD SUCCESSFUL in 8m 39s，05:09:29 正常退出。无 failed! 或 Parsing error。新目录 build/gametest-batch58-large-battery-final；门禁期间未修改 Java/资源，最新 Java 时间早于最终日志。
- 进度统计已刷新：材料 1156、物品 74327、方块 15607、流体 925；缺失 token 49。数量不等于功能完成度。
- 未启动客户端目视，不以服务器测试替代外观验收。

## 尚未完成，不能计为全系列生存还原

1. ZPM/UV/PUV1 六个电池盒的制作配方未写入：移植版缺少明确对应 T7–T9 的电路供应。原版最高大型盒还引用未注册的 10049，不能凭空补一个机壳替代。需先核对兼容模组的电路提供方式及原版实际可用性。
2. 本批变压器铁双板、低档铜线配方先使用现有铁/铜实体，ANY.Iron 与 ANY.Cu 的所有材料替代仍需接入统一材料组；不能说配方等价材料全集已齐。下一步可复用 MaterialTagPack.java:136 的 MaterialGroups.Iron.getReRegistrations()（当前只生成 screws/any_iron_or_steel）扩到双板，再对铜线做同类处理。
3. 电力变压器活动状态外观、覆盖板启停/模式、过载累计持久化与衰减还未完整按原版接通。本次解决核心方向、包转换与能量守恒，并未宣称其全部行为还原。
4. 旧通用电池与原版化学电池的电压规则还需归并；盒子的 shutter 物品门控仍未接通。
5. 其它静态生成器的电路等级仍需审计，例如 generate_generator_module_recipes.py 的 CIRCUITS 数组，不能把第五十五批运行时修复当作所有静态配方已正确。
6. 大型坩埚仍沿用第四十五批实现；尚未实机验证，本轮不把它标为客户端验证通过。

## 实机测试

重启 IDEA runClient 后检查普通/大型盒的名称、材料色与三态覆盖层；打开大型盒，16 个格子和玩家背包应全部对齐。把带电电池放到最后一槽，关闭 GUI、保存重进，再取出，应保留位置与电量；破坏应掉电池及覆盖板。Jade 的容量来自装入电池，空盒不应显示旧的免费容量。

用正常方向 LV–MV 变压器从正面输入 128 EU，输出侧应得到等价的四个 32 EU 包；活动扳手反转后侧面输入、正面输出。请同时测试原版线缆相连的实际机器线路与 JEI 配方。高档电池盒暂时不能按完整生存路线验收。

未构建 jar；没有执行 git commit。整体移植目标仍在进行中。
