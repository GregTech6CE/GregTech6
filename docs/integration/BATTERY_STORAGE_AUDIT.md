# 电池盒、化学电池与能量水晶核查

日期：2026-10-19；基线 `611a75c65`。本批检查二十种电池盒、二十五种化学电池的额定目录与主要充放电路径，以及红／青 Energium 水晶的材料注册消费链。没有改动玩法，也没有运行 Minecraft。

## 已有实现及参数对应

原版 `gregtech/loaders/b/Loader_MultiTileEntities.java` 的 10080–10099 循环生成十档普通／大型电池盒，材料 Electric_T[0..9]，额定 V[0..9]，物品槽位分别 4／16。当前 `BatteryBoxDefinitions` 的二十项对应；energy_storage 前缀代表大型电池盒，并非另一套无限缓存。

原版 14000–14044 的五组化学电池各五档，对应当前 `ChemicalBatterySpec`：铅酸容量乘数 2000、碱性 4000、镍镉 4000、锂钴 64000、锂锰 128000，额定 8／32／128／512／2048 EU。后二组使用 advanced 外观。core 实际枚举与原版注册行对应；本次未验证全部颜色像素、配方及渲染。

电池盒主路径：

- 工作缓存与电池内部电量是两份存储。`BatteryBoxState.stored/capacity` 只统计物品，进度条读取 buffer／bufferCapacity；与原版能源传感器、进度接口的职责对应，不能误判“统计漏加缓存”。
- 缓存容量为额定电压 × 320 × 槽数。`tick()` 每秒按缓存分八档：0／1 档从电池取 40／20 包，6／7 档向电池充 20／40 包，其余档不搬运。实际包数还受电池自身限制；LV 电池额定 32，单次请求 40 包实际最多 32 包。
- 充电计数决定接收功率，每 tick 为可充电数量 × 额定电压 × 2；原版最后一包允许超过剩余功率。四只 LV 电池时接收预算 256，32 的包一次可接 9 包、共 288；这是原版 while 条件对应行为，不是新发现的越界。
- 接收最后整包允许超过缓存容量。本批实际 core 观察缓存剩余 4 时接受一包 32、超额 28，符合原版；不同于 ALIGN-059 的电变压器向下取整。
- 停机只阻止向外输出，仍可接收和在电池与缓存之间搬运。两者对应原版，不能把停机后仍充电列为错误。
- 物品栏逐槽保存，空槽不压缩；普通与大型槽位分别 4／16，每槽单件。加载使用有默认空值的 NonNullList，clear 会重置槽位，不是把槽位数量清零。此为源码核查，没有重启存档实测。
- 正面输出、其余面输入；输出包大小固定为额定值。选择器模式 0 按供能物品数量，1–15 限包；已有 GUI、侧面物品接口、覆盖板搬运 tick 和前后面板 tick。

## ALIGN-061：低缓存输出增加了不透支限制

原版 `TileEntityBase10EnergyBatBox.onTick2()` 只用 `mEnergy >= mOutput` 判断可工作，随后请求 `(mode==0 ? batteryCount : min(mode,batteryCount))` 包。`ITileEntityEnergy.Util.emitEnergyToNetwork/Side` 不再按发出者缓存缩减包数；如果接收者全部收下，原版直接扣包数乘电压，允许负缓存。

当前 `BatteryBoxState.offered()` 额外取 `min(buffer/voltage, ...)`，`emitted()` 也限制扣除范围。四个供能槽、缓存 32、额定 32、模式 0 时，当前 core 观察只提供一包，扣后为 0；原版在接收方能收四包的条件下提供四包，扣后为 -96（原版公式推导）。

这是确认的行为差异，同时也是当前防止缓存透支的保护。不能把它简单描述成损坏或未经决策照搬负缓存；本轮保持现状，后续对齐决策需明确是否保留这项保护。第三方物品、移除电池和低电量状态对输出的影响尚未做游戏验证。

## ALIGN-062：十二种 Energium 水晶只有普通宝石身份，没有 LU 储能

原版注册 14500–14505 红水晶、14510–14515 青水晶，继承 `TileEntityBase08Battery`，六档使用 V[0..5]、能量类型 LU；容量分别为额定 × 400000、× 800000。随后 `OreDictManager.setTarget()` 将 gemChipped、gemFlawed、gem、gemFlawless、gemExquisite、gemLegendary 指向这些电池物品。它们具有实际充放电、电量 NBT、空／满创造栏变体和可摆放电池形态，而不只是同名材质。

当前 core 材料定义保留 EnergiumRed／EnergiumCyan。`MaterialItemDefinitions.all()` 实际生成两种材料各六种宝石，共十二项；观察结果收录在回执。

沿两版本真正注册入口追踪：Forge `bootstrap/content/ItemBootstrap` 与 NeoForge 的共享 `registry/GTItems` 只为硬币、材料箭选择专用类，其余包括这些宝石都构造 `MaterialItem`。该类直接继承 Minecraft Item、只实现 MaterialFormItem，既没有 IItemEnergy，也没有电量读写和电池放置逻辑。`GTChemicalBatteries` 只遍历二十五种 EU 化学电池；不是 LU 水晶的替代入口。

因此本项不是“材料没注册”或“少一个标签”，而是已有十二个宝石身份缺少原版储能行为。不能仅新增图标／配方就声称已补齐。后续应连同水晶充能器、LU 消费接口、空满变体、摆放与保存继续核对；本批没有把整个激光系统都判为缺失。

## 已有单体电池行为与保留边界

`ChemicalBatteryItem` 的充放电包数来自 `ItemBatteryRules`，对应原版电池循环：单次最多额定值数量的包；最后不足整包的充电仍接一包；不足整包的放电拒收。当前写入即将电量截在容量内，原版在重新读取电池 NBT 时截断，不能仅看 setter 就断言原版永远保存超额电量。

当前空电池可堆叠 16、充电后单件，潜行才可放置；placed ChemicalBatteryBlockEntity 保存电量，掉落与克隆转回电池物品。原版摆放后的化学电池同样是被动电池，不是电网电池盒；没有主动输出本身不是遗漏。原版 pick-block 使用显示档估算电量，当前使用同步电量，属于已观察的实现差别，未作为生存功能缺口。

原版的兼容 EU 物品路径依赖旧 IC2 接口，当前仅识别本项目 IItemEnergy。现代外部模组适配需逐接口确认，不能按旧兼容类名缺失直接要求复刻 1.7.10 模组。

原版 emits 标志在无输出调用的 tick 可保留旧值，当前每 tick 先清零且不保存此标志；原版 active 在输出前取值，当前 running 查询当前缓存。此为状态／面板时序差异，尚未完成面板可见影响验收。原版电池盒活动时每 600 tick 调用环境风险检查，当前电池盒 tick 无相应分支，扩展既有 ALIGN-017，不重复编号。

## 源码整理与验证

展开 `BatteryBoxState`、`ItemBatteryRules`、原生 `BatteryBoxEnergy` 的压行代码，补充物品电量／工作缓存、理论接口与实际转移、按槽读取及世界回调上下文说明。三份源代码与基线的代码和字符串签名一致，原生适配两版本投影也一致。

Java 17 仅编译修改的两个 core 类、两份额定目录和 `BatteryStorageAudit`。观察使用明确的假电池槽适配器，真实调用 core 帮助函数，不冒充原生物品／世界模拟。验证二十项盒目录、二十五项电池目录、十二个水晶物品定义和八档充放电、停机接收、模拟不修改、末包与 snapshot 恢复。回执见 [验证记录](verification/battery-storage-audit-20261019.json)。

未运行原生编译、Gradle、全套测试、Minecraft、世界重启、构建或推送。后续继续 LU 充能器及便携电源、旋转／长距离转换和网络。全面审计仍未完成。
