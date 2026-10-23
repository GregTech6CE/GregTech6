# 第五十四批：GT6 化学电池家族与生存装配

## 本轮范围

延续第五十三批电动工具装配，补原版化学电池本体及制作、放置、掉落和充电链路。大型坩埚尚未实测，本批没有再次改坩埚。旧板／锭／硬币／书架和地牢批次的修复仍由完整门禁回归；整个移植目标没有完成。

## 原版依据

- `Loader_MultiTileEntities.java:1009-1070`，注册 ID 14000–14044 的五种化学电池，每组 ULV/LV/MV/HV/EV 五档。
- `TileEntityBase08Battery.java:54-266`：空电池 16 个一组、带电后 1 个；只有潜行时可放置；物品／方块的电量 NBT；充放电包大小为额定电压一半到两倍（ULV 最低 1 EU），每次最多接受额定电压数量的包；最后不足一个包的空间仍可接收一包，溢出电量丢弃；抽取不能取不足一包的余量。
- `MultiTileEntityBatteryEU8/32/128/512/2048` 与 Adv 对应类：碰撞箱内缩分别为 5/5/4/3/2 像素，高 8/11/11/11/13 像素，电量显示量程为 4/7/7/7/9。只有四个侧面的电量条染色，外壳不染色。
- `UT.java:1534-1537` 的 scale：非零电量至少亮一格，只有充满才显示最后一格；不是普通比例取整。
- `CS.java:405/407/413/417`：纯绿、纯蓝、浅绿和橙色的原始 RGB。
- 原版电池继承类未实现电网输出接口；放在地上是被动电池本体，不能拿自动向外放电的能源机器直接替代。
- `LoaderOreDictReRegistrations.java:375-383`：高等级电路可替代低等级电路。新增电池配方使用 tier_n_plus 标签，不接受低于所需等级的电路。

## 已实现

1. 新增 25 个电池型号：铅酸、碱性、镍镉、钴酸锂、锂锰，每种五档。
2. 容量分别为额定电压 ×2000/4000/4000/64000/128000。LV 对应 64000/128000/128000/2048000/4096000 EU，精确保存到物品／放置方块中。
3. 充放电遵循原版包大小、单件限制、包数量上限与末包规则；模拟不改变 NBT。放置只在潜行时执行，空电量可重新堆叠，存档／客户端同步／实际破坏掉落均保留电量。
4. 使用原版碰撞箱及 40 张原图；世界中由专用 renderer 显示整数电量条；背包用共享的带电模型与标准化 0–1 属性谓词显示。模型不为每个化学体系重复复制几何。
5. 空／满两种创造栏条目；Tooltip 显示电量、容量、能量包范围、潜行放置；Jade 复用已有 energy_buffer UID，放大镜读取电量。没有新增缺翻译的 Jade UID。
6. 25 条真实工作台配方由 GT6 原文提取图案：消耗已灌注电芯、电池合金板、对应电缆与电路，ULV–HV 返还磨损后的剪线钳，EV 九格全部是部件而不使用工具；不允许镜像。
7. 五类 LV 化学电池接入第五十三批四种电动工具的材料配方，新增 3780 条、总计 4536 条（启动日志确认），成品继承对应容量并按原版从空电开始。旧通用电池仍保留；新增真实化学电池不依赖旧物品才能使用。
8. `MachineRecipeIngredients` 将 gt:re-batteryN 映射到电压标签，可接受所有同等级化学体系，也保留旧通用电池。
9. 中文名称取`GregTech.lang` 的原注册 ID 翻译；新增说明句仅用于现代操作提示。已有其它翻译未重命名。

## 验证状态

- compileJava 通过；最终门禁已编译并验证电量刻度修正。
- Python 4 项电池资源测试通过：40 图逐字节一致、模型尺寸与 0–1 谓词、25 条原版图案／标准译名／电路替代方向、生成器幂等。日志 `build/assets_batch54.log`。
- Jade 配置翻译检查 1 项通过，日志 `build/jade_batch54.log`。
- 新增五项 GameTest：全 25 型号容量／碰撞箱／掉落表；包规则与堆叠；真实潜行放置／存档／同步／挖回；原配方电芯数量和剪线钳返还；已有充电工作台及五种电池容量继承。
- 初轮 `gametest-batch54-batteries` 在启动期间主动停止，以修正 scale 与原版不一致；未计为通过。只终止本批 GameTest 和 Gradle wrapper，未触碰其它游戏。
- 第二轮 verified 在加载时发现 EV 配方没有 x，但生成器多传了 x 键，五条 EV 配方被拒绝解析。已停止该运行、过滤未使用键、补资源断言，并修正测试的 EV 无工具预期。这两轮均不计通过。
- 最终新目录 `build/gametest-batch54-batteries-final`、日志 `build/gametest_batch54_batteries_final.log`，4096 MiB / G1GC / 8 处理器。2026-09-28 03:35:30 报告 **990/990 required tests passed**，Gradle 退出码 0、BUILD SUCCESSFUL in 9m；无 failed! 或 Parsing error。门禁期间 Java 和资源冻结；最新 Java 修改时间 03:26:26，早于最终日志 03:36:03。

## 你需要测试

1. 重启 IDEA runClient，在能源创造栏查找五类电池。每档应有空／满两种，25 型号的中文与 GregTech.lang 一致。
2. 空电池堆叠上限 16；充电工作台使用对应电压 EU 输入，单颗充电后上限 1。例如 LV 拒绝 15 EU 和 65 EU 的包，接受 16–64 EU。
3. 普通右键不放置；潜行右键放置。检查五档模型尺寸、四侧电量条和 Jade。挖回、重新放置、退出重进后电量保持。
4. 用 JEI 查看电池制作：空电芯不能代替灌注电芯，ULV–HV 缺剪线钳或电路等级不足不能合成。剪线钳应返还并磨损；EV 配方不需要剪线钳。
5. 用不同 LV 电池制作同一材料的电钻／电锯／电动扳手／螺丝刀，容量应分别对应上述五个数值，成品电量为 0。未进行实际客户端像素验收。

## 剩余工作和下一批

现有 battery_box 仍将“灌注电芯”当作容量模块，使用集中缓冲，尚未改为原版完整电池物品的逐槽充放电。现有 battery_eu_* 通用高级储能节点与新化学电池不是同一实现，仍需对照原版重新整理，不能称电池系统已完全还原。本批没有顺手删旧注册或制造存档迁移。

`MachineRecipeIngredients.circuitIngredient` 旧逻辑仍写成允许低档电路代替高档，与本轮核实的原版重注册方向相反；本批新增电池配方已使用正确方向，旧机器配方的全局纠正列入下一批，须相应更新既有验证而不是让错误假设决定实现。

电动工具的实际机器操作、采掘／磨损、坏工具机身与拆卸返还仍需继续；普通电钻不能冒充原版矿用电钻。原有手持 generic battery_* 的数值也未冒称为化学电池数值。

## 文件清单

Java 包根 `src/main/java/com/gregtech/gregtech/`：新增 content/energy/ChemicalBatterySpec、item/ChemicalBatteryItem、block/energy/ChemicalBatteryBlock、blockentity/energy/ChemicalBatteryBlockEntity、registry/GTChemicalBatteries、client/ChemicalBatteryRenderer、gametest/ChemicalBatteryTests；修改 loaders/b/Loader_MultiTileEntities、loaders/b/Loader_Creative、registry/GTBlockEntities、client/GregTechClient、integration/jade/EnergyBufferJadeProvider、integration/jade/GTJadePlugin、content/tool/ElectricToolAssembly、data/MachineRecipeIngredients。

生成器 `tools/generate_chemical_batteries.py`；测试 `tools/tests/test_chemical_batteries.py`；新资源为 chemical_batteries 配方、battery_<化学体系>_<等级> 方块状态／物品模型、共享 chemical_battery 模型和 circuits_tier/rechargeable_batteries 标签；en_us、zh_cn 两份语言追加新电池及提示。40 张纹理从原版复制（已有相同文件不重写）。

本轮未构建 jar，没有执行 git commit。

## 后续电池盒实现依据（只读，未计入本批实现）

`TileEntityBase10EnergyBatBox.java:105-150` 并非直接将电池容量加总后写入一个无来源的大缓冲：每 20 tick 根据缓冲区所处档位，用物品 IItemEnergy 逐槽抽取或注入 20/40 包；按可接受／可输出指定电压的物品数量计算输入和输出能力。`mMode` 限制输出安培，`mStopped` 停止输出。缓冲之外，拆出的电池自身必须带电。

当前 `EnergyNodeBlockEntity.batteryCapacityOf` 只按五个电芯 path 返回虚构固定容量，`batterySlots` 删除 List 元素会令后续槽前移，移除电池只是裁剪集中 buffer，未把电量放回电池。下一批宜将电池盒从通用 EnergyNodeBlockEntity 的 converter/storage 逻辑中分离，保留现有 registry ID、菜单入口和工具交互；以原版槽数、每 20 tick 均衡规则、带电物品移出／破坏守恒、保存／重载恢复做回归，而非只把容量判断扩到新电池。

进度审计已刷新 `docs/porting-progress.json`：材料 1156、物品 74314、方块 15594、流体 925、缺失 token 49。这些是注册统计，不代表功能已完全还原。
