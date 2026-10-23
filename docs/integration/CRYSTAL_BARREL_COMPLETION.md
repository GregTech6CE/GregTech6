# 物品桶、晶体充能器与能量水晶

2026-10-24。Forge 1.20.1 / NeoForge 1.21.1 共用 Stonecutter 源码。本轮按用户要求优先复用移植版已有接口。

## 接入方式

| 内容 | 已有入口 | 本轮改动 |
|---|---|---|
| 13 种物品桶 | `MassStorageBlock` / `MassStorageBlockEntity` | 复用单种物品库存、面操作、自动输出、封装和保存；容量改由具体方块提供 |
| 20 种晶体充能器 | `EnergyNodeSpec` / `BatteryBoxBlock` / `EnergyNodeBlockEntity` | 普通 4 槽、大型 16 槽，各 T0–T9；沿用电池盒 GUI、面板、朝向和更新逻辑 |
| LU 充放能 | `BatteryBoxEnergy` / core `BatteryBoxState` | 把原生物品边界写死的 EU 改为设备能量类型；旧构造器继续默认 EU，缓冲规则不复制 |
| 12 种能量水晶 | `ChemicalBatteryBlock` / `ChemicalBatteryBlockEntity` / `ChemicalBatteryItem` / `IItemEnergy` | 提取共用 `PortableBatterySpec`，新增 `EnergyCrystalSpec`；继续使用原电池的存电、包校验、放置、掉落与同步 |
| 材料与配方身份 | `MaterialFormItem` / `GTItems` | 使用既有红、青 Energium 的六级宝石 ID，不能另注册一套同材料水晶物品 |

核心定义表：

- `core/src/main/java/com/gregtech/gregtech/content/storage/ItemBarrelCatalog.java`
- `core/src/main/java/com/gregtech/gregtech/content/energy/CrystalChargerDefinitions.java`
- `core/src/main/java/com/gregtech/gregtech/content/energy/EnergyCrystalSpec.java`
- 原生边界：`multiversion/src/main/java/com/gregtech/gregtech/item/EnergyCrystalItem.java`、`content/energy/BatteryBoxEnergy.java`。

物品桶容量按原版分 5,000 / 10,000 件；廉价木桶的四种金属箍及 Weedwood 为 5,000，其余为 10,000。共享库存中的输入上限、溢出模式和传感器容量使用实际桶容量，量子存储器保留既有容量。前面显示沿用现有字符渲染器，木桶采用四位灰色数字、隐藏前导零、满载红色 `100%`。

晶体充能器输入、输出都是 LU，正面输出，其余五面输入；不是 EU 转 LU 设备。每 20 tick 的高低缓冲区充放能、模式输出包数继续由现有电池盒规则处理。原版水晶在一次物品传输中还有 `mSizeRec` 包数上限，例如 LV 水晶一次最多 32 包，即使盒子请求 40 包也只转移 32 包。

红水晶容量为额定包大小 × 400,000，青水晶为 × 800,000；额定包大小依次 8、32、128、512、2,048、8,192 LU。沿用 `gem_chipped_energiumred` 到 `gem_legendary_energiumred` 以及 `energiumcyan` 的现有 12 个物品 ID。既有混合机 Energium 粉和高压釜六级生长配方直接指向这些功能物品。未另加替代配方或重复水晶 ID。旧化学电池列表继续只返回 25 种 EU 电池，避免 LU 水晶进入电动工具装配。

补齐 20 个充能器和 13 个桶的原版手工配方、原版贴图、创造分页、双语名称、双版掉落表及 4 个 EMI 分类组。原版可选木材在当前移植中通过已有材料木板解析，未引入外部模组的硬依赖。Jade 复用能量容器接口读取 LU；能量水晶同时保留材料属性提示。六种尺寸采用原版三面贴图，世界中的水晶由现有电池渲染器按储能使用独立亮度，物品栏使用对应尺寸和储能颜色；具体视觉效果仍需客户端验收。

## 同轮显示修正

- CO2 激光器和激光接收器接入 `EnergyDeviceTooltips`，显示原版输入／输出推荐值与范围、对应面、效率、扳手转向和抗爆提示；补充 LU、QU 的名称与颜色映射。
- 六种绳子的物品模型改为原版默认方向的两层绳卷，保持现有世界悬挂模型。
- 末影垃圾堆及末影垃圾桶补上原版钢材底层染色，覆盖层保留自身颜色；物品和世界共用这一规则。

## 原版来源

对照保留原版 `Loader_MultiTileEntities` 的 6983–6999、10130–10149、14500–14515 注册，以及 `TileEntityBase08Battery`、`TileEntityBase10EnergyBatBox`、`MultiTileEntityMassStorageBarrel`、六种 `MultiTileEntityBatteryLU*`、`MultiTileEntityRope` 和激光转换器类。

代码规则来源：GregTech-6 Team / Gregorius Techneticies，LGPL-3.0-or-later。新增默认贴图从已保留的原版资源逐字节复制，沿用项目已有 CC0 来源记录。没有复制参考移植的 Java 业务实现。45 个新增方块名称直接对应 `C:/Dev/GregTech_zh_cn.lang` 的原版数字 ID。文件路径与 SHA256 见 [来源回执](verification/crystal-barrel-20261024.json)。

## 验证范围

- 双版生产源码已编译通过。NeoForge / Forge 各三项集中原生 GameTest 全部通过。首次检查修正了 LV 水晶单次传输上限的测试预期，并使原有化学电池测试显式使用化学电池规格。
- 测试检查：12 种水晶的唯一物品身份、高压釜实际输出、LU / EU 区分、模拟注入、存电及放置后电量；33 个新手工配方和全部原料非空；充能器五个输入面、输出面、真实槽位、缓冲守恒和保存；13 种桶的真实填满拒收、单物品限制、自动输出模式和 NBT 恢复。
- 静态检查：45 份双版掉落表一致、125 份模型及 46 个贴图引用存在、45 个名称与原版汉化补丁对应、4 个 EMI 组覆盖所有新增变种。受保护的 `WoodMaterials.java` 哈希未变。
- 日志：`work/crystal-barrel-final-tests.log`、`work/crystal-barrel-forge-tests.log`；原生测试类为共享 `CrystalBarrelTests`。
- 未进行客户端视觉验收、真实生存全链或旧存档重启验收；本轮未制作交付 JAR，未提交或推送 GitHub。整体移植仍未完成。
