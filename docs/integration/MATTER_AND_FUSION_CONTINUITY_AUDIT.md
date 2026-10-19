# 物质制造机与聚变连续运行核查

日期：2026-10-19；基线 `f3b298f30`。本批检查物质制造机的结构定义、参数与配方声明，追踪聚变的重载／暂停后连续启动条件，并修订聚变源文件自检。没有修改机器玩法。

## 物质制造机已有对应部分

原版来源根目录为 `C:/Dev/gregtech6-master/gregtech6-master/src/main/java`：

- `gregtech/tileentity/multiblocks/MultiTileEntityMatterFabricator.java`：逐坐标结构与部件角色、顶部处理器替换、底部自动输出。
- `gregtech/loaders/b/Loader_MultiTileEntities.java`：1241 行大型机、1542–1546 行五档单方块机。
- `gregtech/loaders/c/Loader_Recipes_Other.java`：898–928 行两种末影流体的特殊转换、969–986 行元素分离。

当前结构在 core `content/multiblock/SharedMatterFabricatorStructure.java`；运行绑定与处理器计数在共享原生 `blockentity/machine/MatterFabricatorBlockEntity.java`，加工和保存走 `LargeRecipeMachineBlockEntity`／`BasicMachineBlockEntity`。单方块效率由 `CompactMatterFabricatorBlockEntity` 使用 `LargeMachineProcessingRules.compactMatterEfficiency()`。配方入口是 `content/recipe/MatterRecipeLoader.java`，两版共用初始化清单调用它。

逐位置比较不是只看总数：解析原版所有显式 `checkAndSetTarget` 调用，去掉主控自身，将原版横向坐标减二，中心空气单独对照；顶部八格各接受 18202／18204 两种处理器。149 个坐标的部件／角色均对应，当前样例计数如下：

| 部件 | 数量 |
| --- | --- |
| 致密铅壁 18031 | 97 |
| 锇线圈 18044 | 26 |
| 通风单元 18299 | 16 |
| 中央处理器 18200 | 1 |
| 处理器 18202／18204 | 各 4 |
| 中心空气 | 1 |

原版要求两种可换位置处理器各至少四个，共八格；当前 `validStructureParts()` 要求各四个，`matchesPart()` 允许这八格互换，含义对应。线圈和铅壁的原版角色都是 `ONLY_ITEM_FLUID_ENERGY`，因此当前线圈也使用 `ITEM_FLUID_ENERGY` 不能仅凭外观判断为多给接口。顶部为 `CASING`，不提供普通物品／流体／能量角色。

当前主控 QU 最小 1、建议 1、最大 2097152、并行 64、链式耗时；大型机采用低成本超频和断电保留进度。五档单方块机额定 QU 为 32／128／512／2048／8192，效率为 5000／6250／7500／8750／10000（万分比），与原版相应注册行对应。core 实际枚举结果见回执。底部自动输出、空自动输入目标已沿当前父类特殊名单核对；外部容器实际吞吐未运行。

28 条 Dilithium／AncientDebris 特殊转换的材料、形态、选择器编号、功率 16、耗时、流体名称和数量与原版声明逐项一致。原版两种材料对应转换倍率不同，这些差别已经保留，不能直接把不同数值判为错误。当前元素分离的筛选保留 ELEMENT、排除 ANTIMATTER、核子和大于零条件，物品五种形态与五种块形态对应；块按九倍，耗时为核子和 × 131072 × 单位数，产出按质子／中子数计算。

以上是**声明与控制路径的对应**。各元素液态／气态／等离子体的单位量、附属新增材料、原生物品与流体解析、与 `GTOtherGen` 同类行的最终去重仍未完整核验。不得以 28 行数值一致声称全部配方已可用。父类已经确认的积液准入、链式并行上限、跨区块原子绑定差异也仍适用，参见 ALIGN-036、038、051；本批不重复编号。

## ALIGN-056：聚变重载或任务中途暂停后，下批同配方被重新收取启动能量

当前聚变 `FusionReactorControllerBlockEntity` 有独立的 `continuousRecipe`。`startRecipe()` 仅在它与下一条配方是同一对象时令启动需求为零；其他情况均为 `recipe.mSpecialValue`。保存的 `gt.fusion` 包含 charge、totalCharge、progress、duration、outputEU 和 products，没有连续配方身份；加载时直接令 `continuousRecipe = null`。暂停／结构失效的 tick 也会清空该字段，但保留当前任务。

具体对照场景：反应 A 已充完 LU 并在加工中，保存后加载，或者中途停机至少一个 tick 再恢复；原任务继续并正常完成，输入端已有下一批相同反应 A，输出可用。当前执行器在交付后立即 `startRecipe()`，因连续标记为空而再次要求 A 的完整启动能量。

原版 `MultiTileEntityBasicMachine` 也不保存 `mCurrentRecipe` 对象，但不能据此判断同样要重充。它保存／恢复 `NBT_ACTIVE`；更重要的是，恢复中的已有任务由 `doActive()` 推进、完成并返回 true，使 `mActive` 为 true。下一 tick 开始同配方时，充能判断是：

```java
mSpecialIsStartEnergy && (!mActive || (mCurrentRecipe != null && mCurrentRecipe != tRecipe))
```

加载后 `mCurrentRecipe == null` 且刚完成的任务 `mActive == true`，不会再次充能；中途暂停后恢复任务完成也重新进入 active，原配方对象未被清空。当前执行器则丢失了这个连续运行信息。

范围仅限**已有任务恢复后紧接相同配方**。不能推广成停机永远不需要再启动，或者任务产物丢失。当前剩余 LU、进度和待产物有保存逻辑；原有保存测试证明的目标是恢复同一任务，不包含它之后的连续下一批。此结论来自源码状态顺序，没有执行 Minecraft 存档重启。配方表重载导致对象身份变更、不同配方切换、超过最大 LU 包时的过载路径另待核。

## 可读性与源文件自检整理

`SharedMatterFabricatorStructure` 展开多层循环、中心空气、壳体／线圈及顶层处理器选择，保留代码和字面量、遍历顺序与样例分配。Java 17 独立编译通过。

旧 `tools/tests/test_fusion_resources.py` 通过 `native_path()` 准备全部原生工具视图，再从原生 `FusionStructure` 适配器寻找 886 个坐标；迁移后实际列表在 core，这个入口已不正确。本批改为直接读维护源码：

- 结构来自 core `SharedFusionStructure`，检查格数、位置唯一性、部件数量和四个能源出口。
- 纹理从版本／共享／core 资源位置查找；确保有模型，避免目录空时循环零次却通过。检查声明的纹理存在和 base／overlay tint 字段。
- Forge 主控 `fusion_reactor_main` 与 NeoForge 主控 `fusionreactor_stainless_steel` 分别检查四朝向 × 两种 lit × 两种 running 的 16 组合及模型引用。
- 18 条聚变数值声明与原版对照，并固定原版读取行数，防止两边解析为空误通过。

补充纠正上批“旧主控 blockstate 路径”的描述：`fusion_reactor_main.json` 实际保留在 core，Forge 仍注册该主控；它不是已不存在的资源。问题是旧检查只走平台临时视图并未分别检查两版实际主控。这一轮不改注册名称或渲染行为。

四项定向 Python 检查通过，导入该检查模块不再导入 `native_layout`，也不再因此创建工具视图。检查仅证明源资源引用／字段与数值行，不证明资源打包覆盖顺序、模型烘焙结果、NeoForge 运行渲染或机器执行。未构建、启动游戏或运行全套测试。详细来源摘要和观察见 [回执](verification/matter-controller-audit-20261019.json)。

下一步继续物质制造配方的最终解析／单位量与储能、变压器、跨区块网络；全面审计仍在进行。
