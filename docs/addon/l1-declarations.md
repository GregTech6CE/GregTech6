# L1：声明式内容

本页用于把配方表、机器、功能部件和管道交给宿主注册；材料和结构细节按需进入专题页。

返回[附属编写指南](README.md)。

L1 的返回值应是不可变、无世界访问或无副作用的声明。宿主会在固定阶段归一化、校验并安装它们；不要在声明方法里扫描世界、注册方块或读取尚未就绪的运行时目录。

## 材料与世界生成

材料、前缀、metadata、形态、加工规则、矿脉与小矿通过 `AddonMaterialContributor` 提交，完整流程见[材料与加工规则](materials.md)。

## 配方表、配方机和燃料机

`AddonMachineContributor.recipeTables()` 提交 `RecipeTable`。`RecipeTable.machine(name)` 和 `RecipeTable.fuel(name)` 共用字段但处于不同命名空间；配方机读取机器表，燃料机读取燃料表。表名由 `AddonNames.RECIPE_MAP` 校验，槽位数量和最小输入数量也在这里检查。

`AddonMachineContributor.addonMachines()` 提交 `AddonMachine`：

- `RecipeMachine` 适合由 GT 配方表驱动的加工。builder 提供 `basic()` / `largeRecipe()`、能源输入、配方表、并行、效率、常量能耗、廉价超频、光照门槛、输出空间和结构键等参数。能源类型、包范围、容量和结构要求由声明构造器校验；配方机不能动态选择能源类型、关闭 overload 或自行声明输入容量。
- `FuelGenerator` 适合固定燃料表发电机。它没有 tier，输出使用 `FuelOutput`，可选单方块或多方块，并声明燃料罐和安培数。
- 两者都由宿主注册方块、方块实体和默认菜单。当前没有为配方机或燃料发电机暴露自定义 Screen/Renderer SPI；需要自定义交互表现时使用行为机。

`MachineCasing` 决定机器的基础方块、材质和机器类型；机器 ID 与机器类型不是一回事，ID 含 `/` 等不能作为机器类型的情况要通过 `machineType(...)` 指定合法类型。声明冲突、内置机器名称占用、未知配方表和不支持的能量形状在启动期失败。

## 多方块

使用 `controller.withStructure(layout)` 显式绑定主控与布局；配方机使用 `largeRecipe()`，燃料机使用 `multi()`。结构约束、端口权限、重复层、主动输出和预览见[多方块声明与端口](multiblocks.md)。

## 功能部件、管道与压缩气罐

`AddonMachineContributor.layoutParts()` 提交 `LayoutPart`。部件可以是壳体、能源输出部件、能源条件输入部件，或带槽位、罐、菜单、tick、点击和命名字段的功能部件。两端输出/输入的角色、面、包范围和条件能耗由声明校验；声明本身不实现运行时行为。

功能部件还需要平台 `LayoutPartContributor`。纯壳体或只带能源能力、没有槽位/tick/点击/字段的部件不需要部件行为；需要 tick、玩家点击、槽位过滤或持久字段的部件，其名称必须出现在 `partNames()` 中并在 `create()` 返回行为。

`fluidPipes()` 和 `itemPipes()` 按材料声明管道族，宿主展开为内置尺寸；`compressedGasTanks()` 声明压缩气罐。压缩气罐的普通流体插入是关闭的，行为通过 `context.world().pushCompressedGas(...)` 写入，燃料机和管道按宿主规则抽取。管道和压缩气罐声明提供内容和属性，不提供任意自定义管路方块逻辑。

## 内置机器调参

`MachineTuningContributor.tunings()` 只覆盖内置机器变体，键为 `machineName + tier`。`MachineTuning` 中非空字段才替换，其余字段保留内置值；相同值可以由多个附属重复声明，不同值对同一字段会导致启动失败。调参在启动时重建规格，不写入存档；不要把它用于附属自有机器或运行时动态改参。

需要宿主处理器以外的状态机时，继续 [L2 运行时行为](l2-behavior.md)；界面资源见 [GUI 模板](gui-templates.md)。
