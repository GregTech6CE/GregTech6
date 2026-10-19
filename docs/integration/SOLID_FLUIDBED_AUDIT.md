# 固体与流化床燃烧核查

日期：2026-10-19。基线：`f82507cbd`。本批核查当前共享控制器及两版入口；生产代码只展开燃烧室目录的格式，没有修复下列玩法差异。原版路径以 `C:/Dev/gregtech6-master/gregtech6-master/src/main/java/` 为根。

## 入口与范围

- 原版固体：`gregtech/tileentity/energy/generators/MultiTileEntityGeneratorSolid.java`；普通燃料由 `gregapi/recipes/maps/RecipeMapFurnaceFuel.java` 动态生成配方。
- 原版流化床：同目录 `MultiTileEntityGeneratorFluidBed.java`；燃料由 `gregtech/loaders/b/Loader_Fuels.java:37–43` 注册。
- 当前固体：共享 `blockentity/machine/SolidBurningBoxBlockEntity` → `api/machine/FurnaceFuelHelper` → `FurnaceFuelValue`／core `MaterialFuelRules`。NeoForge `platform/neoforge/smeltery/SolidBurningBoxEntity` 只委托共享 tick 和交互，没有独立补救逻辑。
- 当前流化床：共享 `BurningBoxBlockEntity` 的 `FLUIDIZED_BED` 分支 → `FuelRecipeMaps.FluidBed`。Forge capability 和 NeoForge `SmelteryRegistries.capabilities` 都暴露此控制器的接口。
- `MachineRecipeBootstrap:69` 调用 `FuelRecipeLoader`，后者只分派 `engine`、`burn`、`gas`、`hot` 四种数据行。FuelRecipeMaps 从空集合构造普通 RecipeMap；不是原版 Furnace 那种自带动态配方的子类。

## ALIGN-026：流化床内置燃料注册缺失

原版循环 Charcoal、Coal、CoalCoke、Lignite、LigniteCoke、Anthracite、Prismane、Lonsdaleite、PetCoke、Peat、PeatBituminous 共 11 种材料，分别尝试注册 blockDust、dust、dustSmall、dustTiny、dustDiv72 五种形态，即 55 次注册调用；这不等于核验过 55 条有效最终配方。方解石输入分别为 648、72、18、8、1 L；热量按材料炉燃烧时间的三倍和物质量缩放，灰渣按燃烧目标的一半缩放。

当前 `OriginalFuelRecipeRows` 及其消费者没有这组分支。在受版本控制的生产 Java／资源中检索精确表名及 FluidBed 引用，找到表声明、控制器与展示文本，没有找到内置配方写入入口；普通 RecipeMap 的构造也不会自动补充它们。当前 core 枚举实际只有四种燃料行 kind。结论是**内置注册缺失**，不声称第三方附属或用户数据包绝不可能向该表写入配方，也没有启动游戏枚举最终配方。

这项缺失会遮蔽下面控制器问题：灰渣、数量及部分容量差异要在数据包提供配方或补齐原版配方后才能触发，不能把代码中的潜在问题说成已观察到的游戏丢物品。

## ALIGN-027：灰渣阻塞没有阻止流化床继续补燃

原版 `GeneratorFluidBed.onTick2` 先尝试把 `mOutput1` 放入输出槽，只在暂存输出已清空时开始下一份配方。输出槽满后可以留一份待输出灰渣，但不会无限积累。

当前 `tryConsumeFluidBedFuel` 调用的无参数 `canStoreAsh()` 始终返回 true；有参数的检查方法没有被该路径调用。`queueAsh` 对同种暂存灰渣直接 grow；不同种类只 flush 旧灰渣，没有保存本次新灰渣。因此提供有效燃料配方后：

- 输出槽满且暂存灰渣未排空，仍会扣燃料／方解石并增加热量；同种灰渣可在暂存中持续累积。
- 暂存为灰渣 A、下一份产生 B 时，B 在该分支丢失；即使 flush A 恰好成功，也没有把 B 加回来。
- 固定 `extractItem(0, 1, false)` 没按配方数量扣料；原版这里交给 `isRecipeInputEqual(true, ...)` 消耗。原版上述 55 次注册的物品输入都是 1，所以此条只针对多件输入扩展配方，不能说原版单件行都少扣燃料。

## ALIGN-028：流化床储罐容量与外部槽位合同不同

原版 `Recipe.RecipeMap.mMaxFluidInputSize` 初始为 1000，添加配方时取输入量最大值；FluidBed 罐容量为 `max(mMaxFluidInputSize, mRate)`。当前统一采用 `mRate * 10`：铅流化床额定 16 HU/t 时容量只有 160 L，原版为 1000 L，无法装下原版 blockDust 行所需的 648 L。core 目录枚举有 28 种流化床，其中 9 种容量不足 648 L（逐项见回执）；计算是对当前构造公式的应用，不是原生罐实例测试。

原版非正面只允许给 0 号槽插入表中燃料、从 1 号槽取灰；当前非正面直接返回两槽 ItemStackHandler，无对应插入／取出过滤。正面排除仍存在，不能说所有方向全部开放。

原版流体入口只接受燃料表包含的流体，普通管路燃烧时不能抽出；当前直接返回原始 FluidTankGT，无这两项限制。原版独立 tapDrain 允许特殊排出，因此修复时不应统一封死所有工具／龙头出口。

## ALIGN-029：流化床补燃失败会过早关闭已有余热输出

原版单 tick 也只尝试一份固体／流化床燃料，**不要把液体燃烧室的循环补燃要求套过来**。但原版最终只在 `mEnergy < mRate` 时熄火。

当前共享 tick 在 `energy < rate * 2` 时执行 `burning = tryConsumeFuel()`。例如额定 16 HU/t、tick 开始有 40 HU，先输出后剩 24 HU；若燃料已耗尽，当前立即设为不燃烧，下一 tick 不再输出这 24 HU。原版仍保持燃烧，下一 tick 继续输出 16 HU，再因只余 8 HU 熄火。这里是逐分支源码推导，没有运行世界模拟；同一赋值也影响液体／气体燃烧室。固体独立控制器没有这个赋值，不包含在此项内。

## ALIGN-030：固体燃烧的灰渣容量策略及计量不等价

原版固体控制器允许在输出槽满、暂存为空时再消耗一份配方，把产物保留在 `mOutput1`；只在这份暂存未排出时停止继续加料。当前 `tryConsumeFuel` 提前要求槽位能容纳本次容器及灰渣，满槽时更早停止，没有利用原版的一份待输出缓冲。

更深的计量区别在原版 `RecipeMapFurnaceFuel:54–88`：有容器时只返回容器；无容器时汇总材料燃烧目标，oreRaw／blockRaw 有矿物倍率，并在 1–63 个燃料的批量内寻找可以表示的灰渣粉末。当前 `FurnaceFuelHelper.burnOne` 固定消耗一件，同时计算容器及主材料灰渣；`ashTinyCount` 强制使用小撮粉，正数至少 1、最多 64。由此不能把当前灰渣当作原版精确物质量回收。具体全部材料／外部燃料与原版 Firestone 兼容分支仍待枚举，本批没有凭该差异随意改燃料收益。

## 环境检查与已存在部分

ALIGN-025 的缺口也存在于固体／流化床：原版 tick 有周边点火、前方烧除、碰撞／非液体／氧气判断；当前固体和共享流化床 tick 没有对应环境动作，`isFrontAir` 为 `isAir || canBeReplaced`。外部模组氧气环境的现代适配未核验。

固体／流化床本来的单次补燃、向上 HU 输出、无接收端仍扣已发出的额定热量、火焰概率点燃入口并非一概遗漏。当前固体有前方取热灰烫伤；不能因缺环境点火就声称没有接触伤害。两控制器有热量／燃烧／暂存灰渣持久化字段，字段存在不等于通过保存重启。

## 可读性和验证边界

`core/.../api/machine/BurningBoxDefinitions.java` 的 113 项定义、构造与查询展开为正常缩进；没有更改任何代码 token、字符串、注册顺序或额定数值。Java 17 独立编译成功，core 枚举得到固体 29 项、液体／气体／流化床各 28 项。这是当前目录清点，不声称已逐项匹配原版全部目录。

证据与源码摘要见 [回执](verification/solid-fluidbed-audit-20261019.json)。没有运行 Gradle、客户端、GameTest、新构建或真实世界重启。后续还需固体自动化实际接入、外部燃料、全材料灰渣、燃烧箱所有额定目录、多方块燃烧／锅炉及储能网络核查。
