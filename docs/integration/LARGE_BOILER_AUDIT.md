# 大型锅炉核查

日期：2026-10-19。基线：`70f7e06d7`。本批检查原版 `gregtech/tileentity/multiblocks/MultiTileEntityLargeBoiler` 与当前共享 `OriginalLargeBoilerControllerBlockEntity`／`OriginalLargeBoilerControllerBlock`；原版源码根目录为 `C:/Dev/gregtech6-master/gregtech6-master/src/main/java/`。玩法没有在本批修改。

## 已核对的目录和结构

原版 `gregtech/loaders/b/Loader_MultiTileEntities:1248–1252` 的五种主控与当前 core `OriginalLargeBoilerParameters` 匹配：

| 原版 ID | 材料 | 壁板 ID | 蒸汽输出 L/t | 硬度 | 热量／蒸汽容量 |
| --- | --- | --- | --- | --- | --- |
| 17201 | 不锈钢 | 18022 | 8192 | 6 | 81,920,000 |
| 17202 | 钛 | 18026 | 16384 | 9 | 163,840,000 |
| 17203 | 钨钢 | 18023 | 32768 | 12.5 | 327,680,000 |
| 17204 | 精金 | 18025 | 262144 | 100 | 2,621,440,000 |
| 17205 | 殷钢 | 18027 | 8192 | 6 | 81,920,000 |

热量容量单位 HU，蒸汽容量单位 L。原版输出使用 `STEAM_PER_EU=2`，容量为输出×10000，建议热输入为输出÷2；原生 Variant 从上述 core 表适配。这里只匹配列出的额定参数，不据此声称配方／渲染／所有工具已完整对齐。

实际 core 枚举有 36 个位置：热输入 9、水输入层 9（其中一个是主控）、蒸汽输出层 17、空气 1。主控位于底层正面中间，完整实体部件为 34 个。四个水平方向上，预览 `LAYOUT` 经 `StructureGrid.offset` 换算后与运行时 `CHECK_ORDER` 的位置／角色完全相同。原版逐位置检查也对应这层级；正面主控由原版 checkAndSetTarget 的主控处理及当前显式跳过处理。

两边都有 128000 L 水罐、80 HU／160 L 的转换比例、蒸汽容量÷2560 的单 tick 转换上限、结垢下限 50%、128 tick 冷却延迟、半罐以上主动输出、超过四分之三时双倍输出，以及压力拆机爆炸。当前矿泉水通过 WATER 标记接收，不沿用小锅炉固定白名单的遗漏。原版大型锅炉没有小锅炉式接触烫伤接口，不能凭小锅炉经验直接判大型锅炉缺碰撞伤害。

## ALIGN-031：结构失效后的运行与危险检查顺序不同

原版 `LargeBoiler.onTick2:180–275` 调用父类后仍进行煮水、冷却、主动输出、更新气压计，再检查结构破坏、过热和满蒸汽。父类 `TileEntityBase10MultiBlockBase.onTick2` 负责周期结构检查，不会阻止子类后续逻辑执行。`checkStructure2` 在相关区块缺失时保留原 `mStructureOkay`。

当前 `serverTick` 每 tick 重查结构，只在 formed 时煮水；结构不完整时冷却后，只按**旧表值**及 shellLoaded 决定是否爆炸，随即返回。于是跳过主动排汽、表值更新，以及后面的过热／满蒸汽检查。具体源码状态例子：结构已破坏、无水无汽、旧表值 0、热量超过容量、冷却延迟尚未结束，原版最后的过热条件会爆炸，当前失效分支直接返回。此为控制流证明，没有游戏复现。

区块未加载时当前判未形成、跳过爆炸，原版结构方法保留旧形成状态；这是明确的加载状态策略差异，需要实际跨区块卸载／重载验收。不能把未加载都当成结构被破坏。

接口也需区分：当前主控直接注热、填水、取汽都要求 `isStructureOk()`；原版主控自身 `doInject` 与 `getFluidTankFillable2/Drainable2` 没有该形成门槛，Root 和 Covers 的普通主控流体调用链也没有附加结构判断。原版多方块**部件**另有 target/结构检查，不能将“主控直连接口可用”扩大为所有失效部件仍可工作。

## ALIGN-032：多出口蒸汽分配不同

原版先模拟五个位置的可接收量，只把能接收的目标计入分配；如果总接收空间超过预算，优先处理容量不超过平均份额的小接收端，再在剩余目标之间分配。当前收集所有存在的流体 handler 后顺序分配 `remaining/count`，最后把剩余额度再次顺序补给。

具体例子：不锈钢锅炉比半罐多 100 L，只有顶部、北侧、南侧三个目标，当前可接收量分别为 100、1、100 L。两实现这三个目标的相对顺序相同，原版分得 **49、1、50**，当前分得 **33、1、66**。二者总输出都是 100 L，所以此结论是分配行为不同，不是无条件丢蒸汽。现代接收端模拟与实际接收不一致时的行为另需实测。

## ALIGN-033：大型锅炉外部爆炸连锁分支缺失

原版 `onExploded:329–333` 在表值 >4 时触发自身爆炸，威力为 `2+max(1,sqrt(蒸汽量)/1000)`。当前威力公式及玩家拆机分支存在，但方块／方块实体没有对应外部爆炸回调。`getDrops` 在高压时不掉物品不能替代爆炸触发；检查到的覆盖板 capability mixin 只包装流体／物品接口，也不补此行为。

## ALIGN-034：大型锅炉工具的损耗和热伤害路径不同

原版排空器优先水、再蒸汽，并返回实际销毁量供工具损耗计算；当前顺序相同，但固定传 `100L`，甚至两罐空时仍走该固定消耗。原版温度计属于工具语义并返回 10000；当前识别 `gt6ce:quicksilver_thermometer` 显示热量，分支没有调用对应损耗。具体不同温度计物品的磨损行为仍需沿物品交互再验。

原版低压除垢使用 `UT.Entities.applyHeatDamage`；当前同公式阈值下调用 `player.hurt(inFire, ...)`，绕过移植版已存在的 `GTEntityHelper.applyHeatDamage`／`GTHazmat.isHeatProtected` 热防护入口，伤害类型也变了。不能声称所有防火效果都失效，因为原版 Minecraft 火伤仍有自身处理。当前高压除垢爆炸、除垢后清热／清汽／恢复效率分支已经存在。

## ALIGN-035：最高档锅炉压力传感器容量被截断

原版 `getGibblMax` 返回长整数罐容量。当前 `gibblMaximum` 返回值虽然是 long，却调用 `steam.getCapacity()` 的 int 边界；该方法经 `LongFluidStorage.bindInt` 截断。精金型实际为 **2,621,440,000 L**，传感器上限得到 **2,147,483,647 L**。

消费路径是 `SensorBlockEntity` 的 GIBBLOMETER／KILOGIBBLOMETER 分支，拿此容量计算最大值。内部 `LongFluidStorage`、煮水上限与锅炉表针使用长整数／variant 容量，未因此缩小；现代流体 capability 本身需要 int 也不等于长整数传感器接口应该一起截断。

## 可读性与验证

展开 `OriginalLargeBoilerParameters` 的常量、检查顺序、Variant 方法、查询与布局循环；展开 `StructureGrid` 的位置／角色、坐标换算、出口判断和空心储罐验证。将 fx／fy／fz 等形参和局部变量改成 frontX／frontY／frontZ、controllerX、centerX、position 等明确名称。除这份声明的改名映射外，代码 token、字符串、循环顺序、公式和原版署名保持。该批没有顺便修改上述玩法。

Java 17 独立编译通过；实际 core 观察五条额定值、四向几何和长整数边界，逐条比对原版注册行。记录见 [回执](verification/large-boiler-audit-20261019.json)。没有运行 Gradle、Minecraft、GameTest、新打包或世界保存重启。

仍待核：壁板孔口实际渲染、真实拆装与跨区块绑定、原版物品 NBT 与当前完整方块快照的差异、冷却延迟保存策略、各来源的工具入口、全部多方块燃烧／焦炉和能源网络。本批读到焦炉委托共享配方机的路径，尚未对其父类完成审计，不列为已完成。
