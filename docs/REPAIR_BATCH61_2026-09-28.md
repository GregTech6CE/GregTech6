# 第六十一批：旧五档电池占位归并到真实锂钴电池

## 原版身份与实现

- 原 battery_eu_8/32/128/512/2048 使用 advanced 电池模型和 V×64000 容量。Loader_MultiTileEntities.java:1054–1058 的锂钴电池正是该组合，原版颜色为蓝色；锂锰则为 V×128000，不应混用。
- 五个原 ID 保留，但从 EnergyNodeDefinitions 移出，改由 GTChemicalBatteries 注册真实 ChemicalBatteryBlock/ChemicalBatteryItem，共用锂钴规格。放下后为被动电池，不再凭空作为网络发电节点。
- 共用额定电压范围、包数限制、充放电模拟、带电单件/空电池堆叠、潜行放置、物品到方块电量传递、放大镜、Jade 和保电掉落实现。
- GTChemicalBatteries.all 仍为 25 个规范型号；allRegistered 为 25+5，供方块实体类型和客户端颜色/物品电量谓词注册。创造栏和电动工具配方枚举仍使用规范系列，避免新添五个重复型号及重复工具配方。
- 旧 ID 的物品/方块模型完整引用对应规范锂钴资源，包括电量条覆盖模型；中文名称与 GregTech.lang 中规范锂钴名称一致。
- 充电电池等级标签包含旧 ID，旧物品能放入电池盒和充电设备。规范生存配方仍产出规范电池，不另造重复制作配方。
- 清理 EnergyNodeRenderer 旧白色电量条和 EnergyNodeBlock 的硬编码旧电池形状；真实形状及彩色电量条由化学电池实现统一负责。
- generate_energy_node_assets 不再生成旧电池占位资源；generate_chemical_batteries 维护五个保留 ID 的资源；add_waterlogged_variants 不再给它们添加不存在的含水属性。

## 旧世界方块实体

保留旧 energy_node 方块实体类型对这五个方块的读取许可。旧实体 onLoad 时，若发现自己已是化学电池方块，就把 gt.buffer 转为 gt.charge 并替换为 ChemicalBatteryBlockEntity。电量仍按同一 V×64000 容量限制；之后存档写入 chemical_battery 类型。已移除旧实体再次收到 onLoad 不得重复迁移。

这仅针对该五种旧电池，未添加旧矿石迁移，也未修改其它 energy_node 实例。迁移后的客户端外观与旧测试世界实际重载仍建议人工验收。

## 验证

- 初始 compileJava 通过。
- 14 项资源检查通过，build/assets_batch61.log：既有电池/电池盒/变压器检查，新增保留 ID 与规范锂钴模型/电量谓词/名称一致、旧生成器不会恢复通用节点或含水属性。
- 新增 LegacyBatteryStandardizationTests 三项：25 规范+5保留注册的职责、五档真实电量/电压/标签与被动身份；实际破坏掉落保留电量；用旧 energy_node NBT 构造实际实体、执行加载转换、确认电量与新类型及不可重复转换。
- 完整门禁 build/gametest_batch61_legacy_batteries.log：06:07:04 **1015/1015 required tests passed**，退出码 0，BUILD SUCCESSFUL in 9m 18s，06:07:36 正常退出；无 failed! 或 Parsing error。使用新世界 build/gametest-batch61-legacy-batteries，期间冻结 Java/资源，最新 Java 时间早于最终日志。未启动客户端目视。
- docs/porting-progress.json 已刷新，缺失 token 仍为 49。

## 后续与人工验收

1. 在旧测试世界加载已经放置的 battery_eu_*，检查电量与模型；挖回后应为可充放电的锂钴电池物品。放置后不应再向电缆直接供电，需装入电池盒。
2. 创造栏选择规范锂钴电池，测试潜行放置、带电掉落、电量条颜色以及对应电池盒充放电。
3. 本轮不是全部旧电池清理完成。GTElectricItems 还有 battery_lv..iv 五种旧非放置 BatteryItem，容量 100000×4^(tier−1)，不能凭名称认定对应某种 GT6 化学电池；其电压规则、制作路线和旧工具依赖仍需继续核对。
4. 下一轮优先修正制作配方电池映射：原版 Loader_MultiTileEntities.java:1264–1267 的燃气轮机 B 位是 gt:re-battery1..4；现 recipes/axial/large_gas_turbine_*.json 错误缩成 battery_lv..ev 单一物品。应追踪 generate_multiblock_recipe_table.py:308 之后的 token 到 ingredient 链路，改成已存在的 rechargeable_batteries 等级标签，并审计其它同类配方，不是改造原配方形状。
5. 高档电路及 10049 制作依赖、原版兼容材料、变压器启动过载宽限/完整结构检查等仍未齐。大型坩埚仍待实机验证。

本轮未构建 jar。没有执行 git commit。整体移植目标仍在进行中。
