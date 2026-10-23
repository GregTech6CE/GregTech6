# 水晶充能器、便携电源与 ZPM 核查

日期：2026-10-19；基线 `ad913c4ff`。本批接续电池／水晶储能核查，区分目录缺项、已有专用实现和实际接口差异。没有修改玩法。

## ALIGN-063：二十种水晶充能器没有实际注册与控制器

原版 `Loader_MultiTileEntities.java:968–972` 按十档注册普通／大型 Crystal Charger，ID 10130–10149，材料 Electric_T[0..9]、输入输出 V[i]、能量 LU、库存 4／16。`MultiTileEntityCrystalCharger` 继承 EnergyBatBox，Large 继承普通型，因此它们是带库存的 LU 充放电设备，不能用无库存的激光发射器代替。

当前 `CreativeTabCatalog` 与 `EmiCatalogOrder` 有 crystal_chargers 名称，但生产 Java 中没有充能器注册或控制器消费。`GTEnergyNodes` 两版遍历共享 EnergyNodeDefinitions；其储能项仅来自 EU 的 BatteryBoxDefinitions，原生 BatteryBoxEnergy 也固定消费 EU。core 观察该目录的带库存 LU 设备为零。该零值只说明这个目录，再结合两版注册入口、全部维护 Java 搜索及激光独立注册链，确认本项缺口；不是仅凭没有同名类判断。

现存激光转换器是能量转换／传输设备，没有水晶库存。补齐时应接续 ALIGN-062 的十二种 LU 水晶能力，复用可明确参数化的电池盒规则，同时补齐模型、配方与物品接口。已有创造分类不代表这一整组功能已经存在。

## ALIGN-064：便携电源四种身份及罐装／耗尽循环未移植

原版明确注册：

| ID | 内容 | 输入充电 | 输出／容量 |
| --- | --- | --- | --- |
| 14700 | 空 Power Cell | 不允许 | 空容器 |
| 14701 | 氢 Power Cell | 不允许 | EU，最小 1、额定／最大 512，3200000 |
| 14600 | 空 Aneutronic Fusion Power Cell | 不允许 | 空容器 |
| 14601 | 氦-3 Aneutronic Fusion Power Cell | 不允许 | EU，最小 1、额定／最大 8192，1024000000 |

原版两条 Canner 路线分别用空容器加对应气体 `U*200`，16 EU/t、256 tick，输出写有满电标记的电源；另有退回空容器的 Canner 路线。`TileEntityBase09PowerCell.setEnergyStored()` 在剩余量小于最大包时替换为空容器，且禁止能源注入，所以不是普通可充电化学电池。

当前 portable_power_cells 只有创造分类声明。维护 Java 中没有这四种注册、专用能源物品或上述罐装／耗尽链。Forge 资源中虽仍有 powercell／aneutronic_fusion 的旧模型、blockstate 与语言键，但 `GTIconSetBlocks` 实际读取 `IconPlantCatalog.BLOCK_ICONS`，两名均不在其中（core 已观察）；这些资源不构成有效物品。不能将此描述为当前注册了两个可用电源，也不能用锂电池的充电配方替代原版燃料容器循环。

## 已有的 ZPM 实现

原版 14999 的容量 2000000000000 QU，11170／11171 两种放能器输出 QU／EU，均按 V[7]=131072。当前 ZpmWorkRules、ZpmModuleBlockEntity、ZpmDischargerBlockEntity 已对应容量、额定包、一格物品、正面输出、非充电神器、工作缓存与物品电量保存。

原版 EnergyBatBox 低缓存每秒从物品提取 40／20 包；当前 ZpmWorkRules 的补充量在低于 40 包缓存时取 40、40–79 时取 20、80 及以上取零。core 观察相应数量 5242880／2621440／0，剩余不足一整包时取零。当前是每个实体自带 tick 计数，原版使用 SERVER_TIME 的全局相位；没有证明必须把这项现代调度差别视为故障。

两版本都有 QU／EU 两种实际放能器。ELITE 字段只是 BASIC 的引用别名，没有额外注册第三种放能器，不能据字段数量误报重复注册。当前 ZPM 物品使用普通 BlockItem 与专用 ZpmEnergy NBT 访问，能被专用放能器消费，但没有原版通用 IItemEnergy 合同；这是后续通用 QU 物品兼容需核对的限制，不等于 ZPM 完全不能放电。

## ALIGN-065：ZPM 放能器电容统计和进度接口混用了两种存储

原版两个放能器继承 EnergyBatBox：能源电容的 stored／capacity 只统计槽内物品；工作缓存另外通过 progress／progressMax 提供。空槽时电容容量为零，装一枚 ZPM 后为 2000000000000；工作缓存上限为 131072×320=41943040。

当前 `ZpmDischargerBlockEntity.getEnergyStored()` 返回 `buffer + itemCharge`，capacity 无论是否有物品都返回 `CAPACITY + PACKET*320`。同时 MachineControls.switchable 的这次调用未提供进度接口，supportsProgress 为 false。例：物品电量 1000、缓存 131072 时，原版电容值 1000、独立进度 131072；当前电容值 132072、无对应进度接口（源码推导）。

这会使按电容比例和工作进度读取设备的工具／面板得到不同数据；需要进一步做相关面板验收。并非能量凭空增加，totalEnergy 作为玩家的“总能量”文字本身可以合理，问题是它被直接当成原版电容接口。

原版右键打开库存 GUI，当前右键直接放入／取出 ZPM、潜行空手切换停机；库存已能由 capability 自动化。记录此交互差别，不能声称库存不存在。原版运行环境检查也未进入专用放能器 tick，延续 ALIGN-017 的调用链缺口；覆盖板有全局 fallback，应另追踪，不因该类未实现 PanelCoverHost 就判所有覆盖板缺失。

## ALIGN-066：ZPM 发光分档使用线性取整

原版 `TileEntityBase08Battery.readFromNBT2()` 用 `UT.Code.scale(energy,capacity,15,false)` 算显示档；`MultiTileEntityZPM.getLightValue()` 返回该档。UT.scale 在 0 以下为 0、满容量为 15，中间为 `1 + energy*14/capacity`，保证任意正电量至少一级光。

当前 ZpmWorkRules.light 为 `clamp(energy)*15/capacity`。实际 core 观察电量 1 的光照为 0、半容量为 7；原版公式分别为 1、8。两者在空电、满电时都为 0／15，不能只验证端点就认定对齐。本轮保留现有计算，仅记录差异。

## 可读性与验证范围

展开 ZpmWorkRules 和 NeoForge 的 GTZpmArtifacts；将误导性的 `recharge()` 改名为 `refillAmount()`，对应“从不可充电神器向放能器缓存补充”，同步唯一生产调用。没有更改算法、参数、注册 ID、存档键和注册顺序。该名称位于内部 content 包，不是附属 API 声明；已搜索维护源码和工具引用。

归一化这一个方法名后，核心与原生调用的代码／字面量签名和基线一致，两版原生投影一致；注册类仅格式和注释变化。Java 17 独立编译 core 规则与 SpecialStorageAudit，通过目录和边界观察。没有编译原生 Minecraft 适配、运行 Gradle、完整测试、游戏、世界重启、构建或推送。原版公式结果与当前原生接口结果均标为源码推导，详见 [回执](verification/special-storage-audit-20261019.json)。

后续继续旋转／长距离转换与能源网络，并回到其余工具、容器、覆盖板、世界生成和实体系统；全面核查仍未完成。
