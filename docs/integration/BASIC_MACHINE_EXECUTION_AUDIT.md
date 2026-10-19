# 共用机器加工执行核查（2026-10-19）

本批继续全面审计，基线为 `f13ef66cd`。范围是原版 `gregapi/tileentity/machines/MultiTileEntityBasicMachine.java` 与当前共享 `BasicMachineBlockEntity`、`MachineWorkOutputs`、`RecipeMap` 的加工／输出路径。原版根目录为 `C:/Dev/gregtech6-master/gregtech6-master/src/main/java`；当前原生类位于 `multiversion/src/main/java/com/gregtech/gregtech`。两平台共用这些实现，比较物品／流体身份的 API 由 Stonecutter 区分。

结论是源码调用链证据。只有工作量计算调用了实际 core 类；没有启动 Minecraft、模拟原生库存或验证存档重启。本批没有修改玩法，完整盘点继续。

## ALIGN-040：完成后的剩余工作量丢失

原版 `doActive:795–865` 把本 tick 能量加入进度，全部输出成功后在第 843 行减去本次所需工作量。下一次 `checkRecipe:683–774` 设置新成本而不清零进度，因此紧接的有效任务可以继续使用余量。若输出堵塞，原版则把进度限制到本次成本；如果下一次找不到可启动任务，也会清零。不能把“保留余量”扩写成永远保留或同 tick 无限完成任务。

当前 `BasicMachineBlockEntity.doActive:350` 首先调用 `MachineWorkCost.advance`，该函数已经把进度截断到 total；成功输出后又设置 `mProgress=0`。`checkRecipe:662` 启动下一项时也清零。立即查找下一配方不能补回被截掉的余量。

边界例子：已完成 90、成本 100、本 tick 供应 32。实际 core 返回 100；当前完成分支归零。按原版上述算术，全部输出成功后的余量为 `90+32-100=22`。这是简单工作量状态例子，不声称某一具体配方实测快慢。Long.MAX_VALUE 边界下当前截断避免溢出确实有效，后续修复需要同时保留安全边界和原版余量语义。

## ALIGN-041：物品产物失去固定输出位置

原版 `canOutput:631–648` 按 `输入槽数量+i` 检查每种产物；`Recipe.getOutputs:749` 保留原始数组下标，没抽中的产物位置保持 null；`doActive:815` 按 `输入槽数量+(i % 输出槽数量)` 投放。

当前 `BasicMachineBlockEntity.itemsFit:790` 按同类合并／任意空槽模拟，`MachineWorkOutputs.roll:21` 将抽到的产物拆成堆后连续保存，不记录原输出下标；`flush:41` 再寻找任意匹配或空输出槽。其待输出存档也只记录堆列表，后续修复不能只调整显示顺序。

实际来源样本：原版 `Loader_Recipes_Temporary:602` 的磁铁矿砂离心有花岗岩矿砂粉／五氧化二钒粉两项，概率 9000／1000；当前 `GTTemporaryGen:58` 有对应表行。假设两者输出槽原先均空，第一项未抽中而第二项抽中：原版第二项仍落第二输出槽，当前被压为列表首项并落第一个空槽。这个概率分支来自代码推导，没有实际运行抽样。另一种状态是原本第一输出槽被其他产物占据、后方仍空，原版会拒绝，新实现可能继续开工。完整配方解析可达性与管道指定槽行为尚待原生核查。

## ALIGN-042：螺丝刀四种机器进出料模式未接入

原版 `onToolClick2:358–365` 用螺丝刀切换四种模式，bit 0 控制是否要求输出为空，bit 1 控制是否仅向空输入槽补料；`canInsertItem2:551` 和 `canOutput:633/652` 消费这些标志，NBT_MODE 在读取及保存中保留。

当前 `BasicMachineBlock` 的螺丝刀分支配置的是覆盖板过滤器，`GTFacingMachineBlock` 仅处理扳手旋转。`BasicMachineBlockEntity` 没有相应模式字段／持久化／空输入槽分支：`SidedItemHandler.insertItem`、`componentItems` 和 `MachineItemHandler.insertItem` 检查面、覆盖板、输入范围及配方许可后委托普通库存。覆盖板白名单／黑名单与这四种机器模式是不同功能，不能作为已移植依据。此结论限定共用基础配方机；漏斗等其他设备的独立 mMode 不在缺失范围内。

`Recipe.mNeedsEmptyOutput` **已经存在**，`RecipeMap.findRecipe:721/729/737` 在 needsOutputs 条件下检查物品输出槽，手作处理工具也有消费。不能说这个配方标志完全缺失。但该查找函数收到的是输入液体，没有验证输出罐为空；活动源码中未发现把此标志设为 true 的内置配方生成路径。原版 Bath／Unboxinator 的 setNeedEmptyOut 生成、当前动态配方对等性需另批核查，不在本批冒称全部已确认。

## ALIGN-043：空输出罐预判没有保留流体身份

当前 `BasicMachineBlockEntity.fluidsFit:818` 对每个罐保存 free 数量，但每种新产物仍读取未改变的真实罐身份／isEmpty。一个空罐扣过第一种液体后，对第二种液体仍被视为空。实际 `MachineWorkOutputs.flush` 会填入并改变罐身份，因此预判与执行不一致。

具体条件例子：离心机六个输出罐中五个已有与此次产物不同的液体，仅剩一个空的 8000 L 罐。UF6 分离行输出 U238F6 5000 L 和 U235F6 2000 L（原版 `Loader_Recipes_Chem:182`，当前 `GTChemGen:141`；六罐目录见 `MachineRecipeMapDefinitions:23`，8000 L 构造见 BasicMachineBlockEntity.setSpec）。预判先从同一空罐扣 5000，再扣 2000，认定可装下；真正填入第一种后第二种无罐可用，于是保持待输出，直到腾出一个合适罐。原版 canOutput 会数出需要两个空罐，只有一个就不允许启动。

这是具备对应配方行的条件状态推导；尚未用游戏连续加工建立五种残液状态，也没有实例化原生罐运行它。不能把它写成已游戏复现，或称产品必然丢失。原版阈值停产另见 ALIGN-038；罐数量／身份与总容量是两个独立条件。

## 可读性整理和验证

- 展开 `core/src/api/java/.../MachineWorkCost.java` 的长参数表、成本计算、超频循环和边界返回；说明 bounded advance 与原版余量语义尚不一致。保持可执行代码、字面量与公开 API 不变。
- Java 17 独立编译该类与 `tools/integration/audit-probes/BasicMachineExecutionAudit.java`；观察 overshoot、exact、long-boundary、普通并行、TU 并行与成本溢出拒绝。这里只观察现状，不把已知差异写成“原版正确性测试”。
- 代码／字面量比较、来源 SHA-256 与观察值见 [回执](verification/basic-machine-execution-audit-20261019.json)。未执行 Gradle、全套测试、游戏、打包或推送。

下一步仍需核查加工模式之外的软锤中断、恒定供能与结构失效、交变能完成门槛、动态配方空输出要求、链式并行上限，以及其他控制器／能源和网络系统。不能因本批四个具体结论结束全面目标。
