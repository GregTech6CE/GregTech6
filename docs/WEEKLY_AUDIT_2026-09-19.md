# 2026-09-19 周改动核查（2026-09-20 收尾）

本文件记录按时间戳候选范围完成的定向代码审计及其证据，不替代移植完成度清单或完整游戏验收。本轮未修改游戏逻辑，也没有重新打包。结论与下一步见 docs/AUDIT_NEXT_STEPS_2026-09-20.md；范围、删除文件和验证边界见本文收尾章节。

## 范围与基线

- Git 最新提交仍为 2026-06-15 的 `460cc52e`，本周没有可供逐提交审查的新提交。HEAD 差异混合了此前数月工作，不能全部归因于最近的改动。
- 按本地修改时间从 `2026-09-13 22:42:00` 筛选 `src / tools / docs`，共 11059 个候选文件，其中 433 个 Java、9742 个 JSON、686 个 PNG、171 个 Python。时间戳只能定位候选，不能证明作者或精确改动内容。
- 清单见 `weekly-audit-file-inventory-2026-09-19.json`。它不含根目录配置、删除文件，以及本次清单生成后的改动，不能用作完整备份。
- Java 候选包括：96 个 GameTest、70 个 content、53 个 worldgen、35 个 block、31 个 loaders、27 个 blockentity、26 个 data，以及其余注册、客户端、物品和 API 文件。分类详见 `weekly-audit-static-2026-09-19.json`。

## 已运行验证

1. `compileJava --offline --no-daemon -Dorg.gradle.jvmargs=-Xmx512m` 成功，11 秒；compileJava 为 UP-TO-DATE。这是现有构建状态检查，不是强制重新编译，更不是运行测试。日志：项目根目录 `weekly-audit-compile.log`。
2. `python -m unittest discover -s tools/tests -q`：96 项，5 失败、2 错误，17.829 秒。日志：`weekly-audit-python.log`。
3. 全量模型及方块状态引用检查已完成：**49057 个文件，29 条未解析引用，退出码 1**。结果见 `weekly-audit-render-2026-09-19.json`。该检查不覆盖外部命名空间、运行时替换模型、着色或实际光照。
4. 配方来源扫描输出 `weekly-audit-recipe-sources.log`：107 个原版配方来源文件，62 个未被移植源码文字提及，涉及 1200 个静态调用匹配。大多为旧版本第三方兼容，且“提及文件名”不代表实现；这不是缺失 1200 条可运行配方的证明。
5. 本轮没有启动客户端，没有重新跑 GameTest 全套。旧的 681 项通过不能覆盖之后修改，也不能替代玩家交互测试。

### Python 失败的真实含义

- 电子元件配方数量固定断言 55，实际 84；挤压模具断言 64，实际 78；材料分组断言 1056，实际 1058。这些首先是旧基线，不能直接认定新增内容错误。材料测试还在数量断言处停止，尚未运行后面的重新生成一致性比较。
- `worldgen_fields` 已从矿石资源生成器移除，但测试仍导入，导致 ImportError。
- 砧测试依赖未生成的 `build/registration-material-catalog.json`，需要恢复明确的测试前置流程，不能造一个空文件掩盖问题。
- 曲柄两个“缺失翻译”是误报：扫描器把注释及 GameTest 中明确检查旧键已删除的字符串也算作运行时引用。不要把 `message.gregtech.crank.full / no_target` 重新加回语言文件。
- 标准中文检测确实失败。完整比较发现 **7 项**偏离 `GregTech.lang`，详见静态报告的 `standardTranslationMismatches`，不是只有测试首个报出的键。

## 已确认的功能与维护问题

### 0. 工具配方网络格式不对称（本轮新增，优先级最高）

位置：`recipe/GTToolRecipeSerializers.java`，修改时间 9 月 16 日 01:08。

- `AssemblySerializer.toNetwork` 只写一个工具类型 UTF 字符串；继承的 `Base.fromNetwork` 读取 UTF 后还会 `readVarInt()`。单条缓冲区会越界，多条共享数据包可能吞掉下一条配方的数据。源码和当前 `build/classes/java/main` 的反汇编均确认格式不一致，见 `weekly-audit-tool-network-bytecode.log`。本轮没有启动客户端连接，所以尚未记录游戏内断线堆栈，但协议错误本身已确认。
- `GTFlintAndTinderRecipe` 返回 CRAFTING serializer；其 `CraftingSerializer.create` 总是构造 `GTToolCraftingRecipe`。专用的燧石/岩石/粒匹配和产物逻辑在客户端丢失。文件内有支持该特例的 `build(...)`，但实际 create 并不调用它。
- 检索工具/制作 GameTest 未见 `toNetwork/fromNetwork` 往返验证。仅服务器匹配和产物测试不会发现以上问题。

修复验收应包括：每类工具配方使用实际 serializer 往返、缓冲区无残留/越界、连续两条配方解码互不干扰、燧石打火工具的服务器与客户端类型及匹配一致，最后真实登录客户端。不能只改一个写入字段后凭编译成功结案。

### 1. USB/HDD 交换器：手动 HDD → HDD 路径不可达

位置：`block/inventory/UsbSwitchBlock.java` 的 `use`；`blockentity/inventory/UsbSwitchBlockEntity.java`。

方块实体允许 SOURCE 接收硬盘或数据棒，但右键代码将所有硬盘固定送入 TARGET。因此玩家无法按现有操作把源硬盘放入 SOURCE。现有两项 USB GameTest 直接写库存，绕过 `use`，检验不到此问题。

下一步：明确源/目标的面或点击区域交互，覆盖真实玩家插入、提取和复制；测试空源、满盘、重复文件、低等级目标以及破坏掉落。原版的两种交换器及 16 模式仍与当前简化实现不同，不应记为完整移植。

### 2. 瓶架和交换器 capability 生命周期不完整

位置：`BottleCrateBlockEntity.getCapability`、`UsbSwitchBlockEntity.getCapability`。

每次查询都创建新的 LazyOptional，未持有并在 `invalidateCaps` 中失效。邻接自动化可能持有已移除方块的旧库存引用。破坏后保留引用是否能造成具体复制，需要针对实际自动化路径复现；目前不能宣称已复现刷物品。

下一步：缓存 capability，处理移除/恢复生命周期，验证破坏与重新放置、区块卸载后旧接口失效。扫描还命中 FluidItem，但它是物品 capability provider，不能机械套用 BlockEntity 修法。

### 3. 瓶架交互和兼容未还原

当前只识别玻璃瓶及本模组 BottleItem，不接受原版药水瓶、经验瓶。GT6 原版 `MultiTileEntityBottleCrate.canInsertItem2` 明确接受这两类，也接受返还空瓶的容器。

原版按点击的 3×3 位置交换对应槽位，且 `swapBottles` 放入整叠、取出整叠；当前是每次一瓶、首空槽入、末槽出。世界中的内容显示也尚未实现。

特别澄清：原版九格显示并不等于最多九个物品。原版会复制整叠到槽位，不能将当前每格可堆叠认定为容量漏洞并限制为 1。

### 4. 物流核心尚未接入，范围公式还有潜在差异

`GTLasers` 仍把 `logistics_core` 注册为普通 Block；新 `LogisticsCoreStructure` 仅有自身定义，未见调用。因此它不是已完成的可运行物流核心。

新类 `range(counts)` 返回 `control`；原版 `MultiTileEntityLogisticsCore.java:439,668` 使用 `control + 2`。该差异尚未造成在线网络范围错误，因为新类还没接入。接入前应修正并验证，避免变成后续行为基线。

### 5. 资源生成脚本会再次写回非标准中文

7 项偏离涵盖控制/逻辑/通用处理器、通风单元、镀锌钢壁板、两种太阳能板。`tools/restore_fusion_assets.py` 中 PARTS 自带翻译，随后无条件更新语言文件，因此只手改 zh_cn.json 不够。

下一步：标准名称统一从 `GregTech.lang` 的映射读取，生成器不要重复维护另一份中文；随后重跑标准中文验证。

### 6. 测试和交接结论存在盲区

- `UsbSwitchTests` 用世界绝对坐标 `(2,1,2)` 放机器，没有转换模板相对坐标。测试隔离不可靠；即使当前同步执行偶然通过，也不宜保留这一模式。
- 该测试注释声称覆盖满盘、等级和破坏掉落，但两个实际测试没有这些断言。
- `tools/handoff_check.py` 只是打印旧日志、时间戳和 jar 哈希，缺少失败状态判定及源码内容快照，不能当验收门禁。
- 早先读取的 NEXT_GOAL 把已实现的量子充能器/远距离端点列作下一步；重新读取磁盘上 21:01 更新版，已改成物流核心优先。这条过时优先级现已纠正，不能继续当作当前缺陷。文件开头仍称“没有任何半成品”，却同时记录物流核心结构类未接线，该表述仍不准确。
- 物流交接将 20 台 Crystal Charger 归为“死代码，所以不用移植”的结论过强。原版接受实现 IItemEnergy 的物品；本仓库找不到 LU 物品只支持“当前原版独立环境未找到可充对象”，不能证明外部兼容或完整内容还原时永远无用。此项应保留为待确认兼容范围，不能直接从总目标删除。

### 7. 四联、九联管的手工配方违反原版组合关系

位置：`Loader_HandToolCraftingRecipes.pipes/pipe`（9 月 16 日），`Loader_FormConversionCraftingRecipes.registerPipeSizes`。

手工注册遍历所有流体管尺寸，但只为 TINY/SMALL/MEDIUM 特判，其余进入六块板的 default 分支；只有 HUGE 改用双重板。因此 QUADRUPLE 和 NONUPLE 都用 **6 块弯板**，与 LARGE 配方竞争相同格子。

与此同时四联管能拆成 4 根中管，中管各需 3 块弯板，6 单位投入得到 12 单位等价管材；九联管能拆成 9 根小管，小管各需 1 块弯板，6 单位投入得到 9 单位等价管材。由于配方冲突，玩家工作台最终选中哪个结果还需实测；这不改变注册了错误低成本配方的事实，不能称为已经复现稳定刷材料。

原版 `gregapi/tileentity/connectors/MultiTileEntityPipeFluid.java:97-101` 明确是 **4 根中管的 2×2 → 四联；9 根小管的 3×3 → 九联**，并提供拆分。当前转换器注释称“组合只属于 Boxinator”，也不符合这段原版代码。

下一步应显式处理七种尺寸、恢复两种组合配方，并验证全尺寸合成/拆分的材料量及配方冲突。不能仅删拆分来掩盖低成本配方。

### 8. 多组生存配方只在启动注入，重载生命周期缺口

已定位 8 个本周修改的加载器：BottleFilling、FormConversionCrafting、HandToolCrafting、Oven、StoneCrafting、ToolCrafting、Track、WoodCrafting。它们都在 ServerStartedEvent 直接 `RecipeManager.replaceRecipes`，没有自身重载入口；全项目也未找到为这些注入配方补回的 AddReloadListenerEvent/OnDatapackSyncEvent 或 RecipeManager mixin。

按此生命周期，数据包重载替换 RecipeManager 后，这些只存在于启动内存中的配方不会再次注入；Oven 对 NEVER_FURNACE 的改写也会失去。此为源码路径结论，尚未运行 `/reload` 端到端确认。清单及时间戳见 `weekly-audit-recipes-2026-09-19.json`。

验收必须比较启动、一次重载、连续重载、退出重进后的配方 ID/内容与客户端同步，保证不丢失、不重复，也不越过数据包覆盖规则。重新调用全部 ServerStartedEvent 不是合适修法。

### 9. 材料统一与“原版配方完整性”的边界

`Loader_FormConversionCraftingRecipes.formIngredient` 明确只接受 GT 本体材料物品，排除统一到的原版物品，理由是避开 vanilla 的 9 锭/块配方冲突。这个排除被应用到全部转换行，包括 vanilla 不提供的 2 锭→3 billet 等，导致该转换器不能用普通铁/铜等触发这些行。是否另有替代入口尚需逐链核验；不能把当前实现称为所有普通材料都严格绑定。

`MachineRecipeIngredients` 仍有“高重数板→低重数板”“缺指定导线→同尺寸任意材料导线”“缺组件→机壳”等降级替代。旧 GameTest 日志显示当时机器配方没有触发替代，因此本次不认定现有机器均使用错误原料；但此策略会让后续缺项在测试中伪装成可制作。BasicMachineCraftingTests 的形状测试主要验证格子占用及非空，不能证明每种具体材料与原版一致。后续新增材料/机器时需要零替代或明确逐项批准的标准。

## 世界生成核查：地牢（本轮新增）

已沿 `GTDungeonPlacement → GTDungeonStructure / GTDungeonFeature → GTDungeonLayout → GTDungeonData` 两条入口核对生成条件，并抽查树木放置和垃圾维度存储。其余世界生成文件尚未完成逐项对照。

### 10. 定位结构和实际地牢的生成条件不一致

`GTDungeonStructure.findGenerationPoint` 只检查概率，`GTDungeonPlacement.isPlacementChunk` 只检查网格。实际 `GTDungeonFeature.place` 还检查主世界、`passesPosition` 的坐标禁区和基岩条件。因此注释中“定位结构存在即地牢存在”的保证不成立。

具体公式反例：世界种子 **30**，锚点区块 **(5,5)**，通过当前 1/100 概率；实际方块 X/Z 是 (80,80)，小于 368 的禁区条件，feature 拒绝生成。结构是否最终被定位还受生物群系条件影响，此处证明的是两个入口条件不等价，尚未在该种子中执行 locate。

算法探针：`build/weekly-audit/DungeonSeedProbe.java`，结果 `weekly-audit-dungeon-seeds.log`。探针使用 Java Random 复现公式；同时反汇编当前依赖确认 `RandomSource.create(long)` 使用相同 LCG 的 LegacyRandomSource，字节码记录位于 `build/weekly-audit/random-source-bytecode.log`。这不是完整世界生成实测。

修复时应共享无世界读取的维度/锚点禁区/概率门槛，并明确基岩与地形等额外条件对结构定位的影响，不能只再调整概率。

### 11. 地牢钥匙编号过度缩小，跨地牢碰撞可构造

当前 `GTDungeonFeature.keySeed` 为 `1 + nextInt(1000000) - index`。原版 `WorldgenDungeonGT.java:170` 使用 `1 + max(random, System.nanoTime())`，不是只用一百万随机值。

按当前公式，世界种子 **0** 下，通过概率和坐标禁区初筛的锚点区块 **(192,3833)** 的第 5 把钥匙、**(258,5340)** 的第 1 把钥匙，编号同为 **466376**。门/传送门按编号比较，因此该设计无法保证不同地牢钥匙隔离。还存在首值很小时生成编号 0 的边界，而传送门明确拒绝 0。碰撞公式已由上述探针复现，未声称已生成并实测两座完整地牢。

下一步应保留确定性和世界/锚点稳定性，扩展编号空间并排除保留值 0；兼容已有存档的编号不能直接重算覆盖。测试需跨地牢、跨种子采样，不能只验证同一地牢五把钥匙彼此不同。

### 12. “地牢级唯一房间”被改成“每格唯一”

`GTDungeonFeature.generateCell` 为每个 cell 新建 tags、generatedKeys 等集合/数组；Workshop 和 PortalEnd 等类的注释也明确承认 tag 只在 cell 内有效。原版 `WorldgenDungeonGT` 先创建一套 tTags/tGeneratedKeys，再传给所有房间。因此工作间、传送门房间唯一性与钥匙分布约束不再等价。

按区块拆分生成可以保留，但应从地牢 seed/layout 预先确定全局房间分配，再逐 cell 放置，避免生成顺序影响；不能把局部 HashSet 当成原版全局限制已移植。

### 其他边界

- 树木 `GTTreeShapes.place` 有高度/可替换检查，但 force 路径和跨区块树冠仍需跟实际世界生成上下文一起验证，本轮不据此宣称发生跨区块破坏。
- GarbageData 新增 256 种垃圾条目的上限，运行时挤掉最老条目、读取旧存档则只保留最老 256 条。这是明确的持久化行为改变，不能称为完全保留原版；玩家丢弃物本身属于垃圾，是否保留此上限属于功能取舍，暂不当作普通箱子丢物品故障。

## 能源与库存核查（本轮新增）

### 13. 树液袋自动化不标记持久化修改

`SapBagBlockEntity`（9 月 16 日）创建 `new FluidTankGT(CAPACITY)`，没有 `setOnChanged(this::setChanged)`；其 capability 的 fill/drain 直接转发到 tank。FluidTankGT 默认回调为空。因此自动化修改液量本身不会标记所在区块需要保存。手动右键路径会显式 setChanged，之前只测手动路径无法覆盖这个问题。

复测要先保存区块，再只用管道抽取/填充，不进行采集或其他方块修改，随后卸载重进核对液量。实际是否丢失取决于该区块是否又被其他操作标记脏，不能以某一次碰巧保存成功判为无问题。

### 14. 树液袋树脂掉落和堆叠边界不完整

`SapBagBlock.playerWillDestroy` 会掉树脂，但 `onRemove` 仅清空液体、不处理 stored；未走玩家破坏回调的移除路径可能丢树脂。需要覆盖爆炸、依附方块失效等实际触发路径，尚未游戏内复现。液体破坏后销毁是原版既有行为，不应当作需要掉液体的 bug。

`SapBagBlockEntity.collect` 对相同树脂直接 `stored.grow(...)`，没有栈上限。持续收集可超过物品最大堆叠，而存储仍使用 ItemStack.save 的 byte Count，超过 127 的数量还存在存取溢出风险。原版使用 inventory 的 addStackToSlot；应按照原版库存容量处理满袋行为，而不是使用不受限的裸 ItemStack。

### 15. 已激活流体泉可能覆盖含水固体方块

`FluidSpringBlockEntity.emit` 只在“非空气、不可替换、且 fluidState 为空”时拒绝覆盖；含水台阶/楼梯等固体的 fluidState 非空，因而落入直接 `setBlock(target, fluidBlock)`。复测顺序是先激活泉，再在它上方放含水方块。原版判断的是液体方块/空气，1.20 的含水状态不能直接等同于整个方块可替换。

另外 load 对 amount 未校验，而 tick 使用 `nextInt(amount)`；原版 tick 在 amount<=0 时恢复到 600。当前 setSpring 有下限保护，所以正常生成路径不会给出 0，风险主要在旧/异常 NBT。正常激活过程也没有 setChanged，active 状态的保存应与液量一起检查。

### 16. EnergyNet 的断线反向边未清理（潜在问题）

`onConnectionChange` 清空当前节点 edges，却不从原邻居的集合删除指向当前节点的旧边，再加入新边。之后从两端遍历可得到不同连通性，onRemove 也可能无法清掉不对称的反向边。此次新增的 onRemove 邻居清理不能修复此前已产生的非对称状态。

全源码检索确认 getConnectedWires/getReceivers 当前只有定义，没有调用，因此本轮不认定这已经影响实际供电；这是仍被维护的辅助图的数据一致性问题，接入前必须验证双向连接/断开/卸载。不要为了清理此图去改动实际正在使用的能源传输路径。

## 生成一致性与客户端补充核查

### 已排除的生成疑点

材料重新生成测试原先在 1056/1058 的数量断言处中断。本轮在临时目录独立执行 parse_catalog/write_catalog，不修改项目源码：Materials.java、GT6Materials.java、Element/Compound/Ore/Stone/WoodMaterials 共 7 个文件与项目逐字一致。报告 `weekly-audit-material-regeneration-2026-09-19.json`。因此该项应修正过时的数量基线，而不是回退两种新材料；这不等于已核验全部材料数值与原版一致。

补充检查 84 条 components、78 条 extruder_shapes 配方的输出模型及递归引用：无缺失；78 条挤压模具均明确禁止镜像。报告 `weekly-audit-recipe-resource-check-2026-09-19.json`。该检查只证明这些输出资源与镜像标志，不证明配方材料量/生存可达性。

### 17. 矿车速度补丁的兼容范围比注释更广

`AbstractMinecartSpeedCapMixin` 拦截所有 AbstractMinecart.setCurrentCartSpeedCapOnRail 正数写入，直接赋值并取消 Forge 的原处理。它没有检查矿车当前轨道、GT 标志或调用来源，因此其他模组设置限速时也会绕过原本的最大速度上限。“从未经过 GT 轨道的车保持 Forge 行为”的注释仅在没有其他调用者时成立，不能作为跨模组兼容保证。

这不等于当前独立环境已经出现矿车故障；应测试 GT 高速轨道→普通轨道→第三方轨道的连续切换，并按原版移植目标决定是否保持全局改动或限制到 GT 轨道，准确记录兼容取舍。

### 菜单可访问性与构建检查边界

BasicMachineContainerMenu、BumbliaryContainerMenu 的 stillValid 仅检查方块实体未移除，没有距离/维度检查；传送或远离机器后仍可能保持操作权限。MetalChest 的菜单已经同时检查实体身份和距离，说明项目已有可复用的更完整做法。此项源码已确认，正常客户端界面移动路径/传送后行为仍需实测，暂不宣称存在物品复制。

已读取 build.gradle、gradle.properties、incremental-resources.gradle 的实际处理路径，并实际运行 `python tools/test_incremental_resources.py`。结果 PASS（退出码 0）：新增、修改、删除、未改变输出时间戳、模板参数展开、外部改坏输出修复、UP-TO-DATE、输入清空后输出清理全部通过。日志 `weekly-audit-resource-sync.log`；隔离样本及各轮 Gradle 日志在 `build/resource-sync-tests/run-af7rawwh`。没有执行 clean，没有修改游戏存档，也没有跑大型 processResources。这证明这些资源同步正确性场景，不证明数万文件下的启动耗时已经解决。增量脚本时间为 9 月 13 日 14:39，早于本次周候选起点，不应归为这一周新增问题。

## 崩溃与模型警告

### 当前仍存在的资源缺口

29 条未解析引用集中在木材与树苗资源，其中包含未使用的旧模型，不能直接等同于 29 个实际坏方块。但以下树苗路径能明确追到现用 blockstate 和 item model：`sapling_blue_mahoe / sapling_ebony / sapling_pine / sapling_white_mahoe`，都引用不存在的 `gregtech:block/iconsets/sapling_small_<species>`。

`tools/generate_gt6_tree_assets.py` 对所有物种直接拼接该贴图路径，未对原版图标命名作映射，也不检查目标存在；上述模型修改于 9 月 16 日。这是本周生成流程中已确认的资源错误，修复时必须同时改生成器和输出文件。黑檀/松/白马槿的旧木材模型亦有缺纹理，详见 JSON；需按实际引用和原版物种对应关系处理，不可随便用另一种木头替换充数。

### 旧崩溃报告的边界

9 月 18 日报告的 JVM 崩溃为本机内存分配失败，Java 25、9GB 最大堆；仅凭这一点不能认定本模组内存泄漏。

旧 latest.log 中 ModelBakery 警告 61331 条，ModelManager 缺纹理警告 66 条，SpriteContents 警告 85 条。统计见 `weekly-audit-crash-log-counts-2026-09-19.json` 与 `weekly-audit-crash-models-2026-09-19.json`。日志洪泛值得处理，但不能据此证明是原生内存崩溃根因。

抽查旧日志提到的蓝马槿树叶及 abyssalnite 原矿箱，当前相应模型/贴图已经存在。不能把旧日志所有报错再次算为当前未修复。

木轴静态模型仍含无命名空间的 `block/iconsets/axle`，默认会指向 minecraft；老生成器 `generate_wave38_assets.py` 也如此。该问题文件时间为 6 月/9 月 11 日，不应归咎本周改动。`PipeWireClientModels` 随后用正确的 GT 纹理替换轴的世界与物品模型，所以静态警告不能直接证明最终画面仍紫黑。全量静态检查只检查 `gregtech:`，会漏报这种写错命名空间的引用。

## 补充交接信息的独立核对

提供的 §125 收尾信息已核对到当前磁盘，不把交接自述直接当证据。

**已证实：** jar 大小 34,045,205 字节，ZIP 条目 72,845，SHA256 `16661AFA3C3C04847C296B49E2D9E4D334E13464A691A8407F853931B8A5C79B` 完全一致；build/gametest.log 包含 20:55:16 的 681 GAME TESTS COMPLETE / All 681 required tests passed 和 BUILD SUCCESSFUL。两个时间戳不变量均为 true。注册快照中的 1156 / 70760 / 12099 / 925 / missing 49 一致。主交接临时文件存在。

**已有实现，不重复开发：** GTLasers 的量子充能器五档、LaserSpec 的 LU→QU 参数与 LaserConverterBlockEntity 的每 tick 丢弃缓冲路径确实存在；LongDistEndpointBlockEntity 确有定向管路搜索与库存/流体代理。这里只确认实现存在及所述核心路径，不把它扩大为全部边界、联机、兼容已验证。

**需更正/保留：** 当前 LongDistancePowerTests 只有 3 个 @GameTest 方法，非 5 个；LogisticsCoreStructure.range 仍少基础 2 格。Crystal Charger 的“永远无用所以正确不移植”结论仍需限定独立原版环境，不能由本库没有 LU 物品推出所有兼容环境都无效。旧世界第一次失败的原因与归属目前只有交接叙述，不能仅凭新世界通过就证明旧失败完全无关。

**测试通过仍含真实资源缺口：** 已直接打开这份 SHA 对应的 jar，确认 sapling_blue_mahoe 的物品模型引用 `gregtech:block/iconsets/sapling_small_blue_mahoe`，但 jar 内没有对应 PNG。因此这份“全绿”交付确实未覆盖客户端贴图完整性，而不是仅工作树和 jar 不一致。校验摘要见 `weekly-audit-handoff-check-2026-09-19.json`。

**归属边界：** 提供的“最后一个会话只改这些文件”有助于划定范围，但无逐会话快照或提交，mtime 无法证明作者。不能把本周所有缺陷都归到最后的 LogisticsCoreStructure 会话；当前按本周候选文件审查，并按具体修改时间记录。

**快照工具还有一处解析异常：** porting-progress.json 的 registry.queued 中，一个 key 跨行吞进下一条日志（含 `Queued 49 fuel rod items`），说明生成器的日志解析不能直接作逐分类精确计数。总注册数本次与交接一致，不代表这个分类键正确。

**覆盖板数量口径需纠正：** GTTechnological 实际是 6 个物品/流体过滤覆盖板、3 个通用进出/存储覆盖板、1 个 dump、4 个 CPU 显示，共 14 个。原版对应 10 个物流动作具体类和 4 个显示具体类，另外 2 个是抽象基类。因此 W3 应实现 10 个动作行为，而不是把交接里的“12 个实体覆盖板”机械转成 12 个注册项；通用覆盖板本身兼管物品和流体。应按实际类逐一核对功能，不能凭数量补出不存在的覆盖板。

**交接文档内部还有历史描述残留：** §1 声称只有两项改动且没有改文档，§7 仍写“没有重建 jar”，同时 §5 已记载最终文档及构建收尾。本条消息中的最终清单更完整；旧段落应标注为收尾前快照，不能当作最终逐文件无遗漏凭据。只凭时间顺序也不能证明 jar 与测试所用源码内容完全一致，需要快照或构建时源码清单。

## 管道自动化补充核查

### 18. 物品管之间传输绕过接收面的过滤覆盖板

ItemPipeBlockEntity.pushItemsToPipe（610–626 行）只调用发送面的 coverPermitsItemTraffic，然后把接收方 BE 本身传给 insertItemStacked。接收方 canAcceptFrom 仅判断 disabledInputs 与 lastReceivedFrom，不检查覆盖板；接收面的实际过滤位于 FaceItemHandler.insertItem（1042–1044 行），该包装器在直传路径没有被调用。因此两根相连物品管之间的传输缺少接收面过滤检查。此结论来自调用链，尚未运行对应游戏内复现。

已读 ItemPipeCoverTests 的三阶段管线测试：它覆盖发送面不匹配时阻挡、匹配时放行，不能代替“过滤器装在第二根管的接收面”场景。后续回归应验证接收面拒绝、匹配放行、取反模式以及物品总量守恒。

对照 FluidPipeBlockEntity.distribute（580–590 行）：流体直传路径已显式检查发送和接收两面的过滤条件，不能因为它也直接调用 adjPipe.fill 就同样判错。

另外物品管向外部库存推送时，先按模拟结果 split，再忽略实际插入返回的余量。如果外部 handler 的模拟与执行接受量不同，会丢失未接受部分；这是待专用 handler 回归验证的兼容风险，不等于当前原生箱子已复现吞物品。

## 储罐与漏斗补充核查

以下均为当前源码调用链核查，未启动客户端复现；文件修改时间只界定候选范围，不证明哪一条工作线引入了每一行。

### 19. 储罐发酵进度读档后重置

TankBlockEntity.java（9 月 18 日 01:06）load 保存恢复 sealedTime/maxSealedTime，但将 fermentRecipe 设为 null。下一次 tickFermentation 进入 `fermentRecipe == null` 分支，无条件将 sealedTime 与 maxSealedTime 清零。因此重新加载区块、重进世界以及带 NBT 挖起再放下后，已保存的发酵进度会被丢弃。原版 TileEntityBase08Barrel:188–207 在重新查到有效配方时重算总时长，保留 mSealedTime，不是这个行为。

BarrelFermentationTests 检查连续 tick 和产物比例，没有覆盖发酵中途保存、重建 BE、继续 tick。后续回归应在进度 500/1024 时执行真正的 NBT 往返，确认不是从 1 重新计时。

### 20. 密封桶仍能自动输出及填充/抽取

TankBlockEntity.tickServer 在密封发酵前无条件执行已开启的 autoOutput；fill、两种 drain 和 handleUse 都没有密封检查。原版 TileEntityBase08Barrel:188–215 把发酵与自动输出放在互斥分支，248–275 和 292–308 行还分别封锁了物品和方块形式的填充/抽取入口。当前不仅密封语义缺失，还允许在固定发酵时长已经确定后改动内容量，破坏时长与数量关系。

复测应覆盖：先启用自动输出再密封；密封期间从 capability 的模拟和执行入口注入/抽取；手动桶交互；解除密封后恢复流动。已有发酵测试直接操作内部 tank，不能证明外部入口遵守密封限制。

附带差异：autoOutputFluid 用 isLighterThanAir 判气体，气体向全部六面输出；原版区分 gas 与 lighter，气体只走上下两面，较轻的非气态流体只向上。此项应连同材料物态映射校正，不能用一个 lighter 标志代替。

### 21. 手动倒入满桶后没有替换玩家手中容器

TankBlockEntity.handleUse:304–315 对 heldHandler 直接执行 drain，随后给储罐 fill，但从不读取 heldHandler.getContainer() 或替换手中物品。已核对本机 Forge 47.4.20 源码包内 FluidBucketWrapper：drain(EXECUTE) 通过 setFluid(EMPTY) 把包装器的 container 引用替换为新空桶，并不把原 ItemStack 原地变成空桶。因此这一调用路径不能正确返还原版空桶，手中原物品与 capability 内部容器状态脱节。重载物品后可能重新获得满桶状态，需要玩家级回归确认完整表现，不宣称已在游戏中实测无限复制。

空容器分支使用 FluidUtil.interactWithFluidHandler，满容器分支应同样按 Forge 容器结果及玩家物品栈规则处理，并覆盖非创造模式水桶、熔岩桶、NBT 容器、空间不足和堆叠容器。

### 22. 队列漏斗小容量吸取地面物品会直接丢弃余量

QueueHopperBlockEntity.java（9 月 17 日 17:24）suckWorldItems:372–379 在第 0 槽为空时，先按螺丝刀设置的格容量截取 toMove，再无条件 entity.discard()。例如格容量 1、地面实体有 64 个圆石，只存入 1 个就删除整个实体，剩下 63 个没有存入、掉落或退回路径。这是可达的普通生存操作，源码已确认；本轮未运行游戏场景。

后续测试应把容量设为 1 和 16，投下 64 个物品，断言库存增加量与地面减少量完全相等，并检查部分合并到已有槽时余量保留。普通 HopperBlockEntity 的空槽分支直接放入整个实体栈，两者实现不能互相作为正确性证明。

HopperBlockEntity.moveFrom 另有外部 handler 兼容边界：实际插入计数以模拟可接受数 acceptable 为基准，而非执行抽取所得 extracted 数量；退回来源的余量也未检查。对模拟/执行不同结果应专门测试，当前不将其描述为原生箱子已复现丢失或复制。

### 本轮未判为缺陷的路径

PortableFluidContainerItem 的可调容量 fill 入口确实先扣除现存量并限制传入量，再调用 Forge 实现；没有仅修改 tooltip 却放任填满物理容量。MetalChestBlock 的 onRemove 明确遍历库存掉落，BE 的库存修改有 setChanged、capability 有 invalidate，不能笼统归为“新箱子不掉内容”。这不代表全部箱子场景已通过运行验证。

## 传动轴、能源规格与数据提示补充核查

### 23. 连续两根传动轴的 RU 传递被访问标记阻断

AxleBlockEntity.java（9 月 17 日 12:22）transferRotation:263 先 `visited.add(nbPos)`，再调用下一根轴的 transferRotation；下一根轴在 237 行执行 `if (!visited.add(getBlockPos())) return 0`。位置已经存在，递归立即返回 0。因此经过两根相邻轴的路径无法到达后面的机器，不是 EnergyNet 辅助图上的无调用问题：EnergyTransfer.insertEnergyInto 明确直接调用 IEnergyBlock.doEnergyInjection，轴从该入口进入 transferRotation。

已检索 GameTest 目录中轴及 transferRotation 引用，找到的轴测试验证扳手采掘与掉落，没有连续轴传动断言。应先用同一接收器验证一根轴基线，再验证两根轴、三根轴、断开一端和反转，逐段核对速度损耗与实际接收量。本轮源码确认，尚未运行该场景。

### 24. 传动轴注入的能源类型与接入面检查不完整

AxleBlockEntity.doEnergyInjection:201 把 `side == null || energyType != RU || !doInject` 统一处理为返回 amount。对实际 EU 等错误能源，这会报告全部接受却没有传递或存储；EnergyTransfer.insertEnergyInto 没有先调用 isEnergyAcceptingFrom，因而不能依靠类型查询挡住此路径。它还未校验 fromSide 对应连接是否开启；transferRotation 只检查出口连接。

两根轴之间的错误访问标记修复后，仍需验证错误能源返回 0、未连接入口返回 0、模拟调用无副作用且语义一致。不要只删除 visited.add 就认为传动系统完整。

放置连接另有方向问题：autoConnectOnPlace 的 oppositeCandidate（130–131 行）仍设置 clickedFace，而 oppositePos 位于 clickedFace 方向，邻居朝向新轴的那面应为 clickedFace.getOpposite()。例如沿东西补入中间轴时，会尝试打开东侧邻居的东面而非西面。必须同时检查两端面是否对称，不只看新放轴自身模型。

### 规格与提示核查边界

GTVoltageTiers 的 tierMax/tierMin 已与原版 UT.Code:1388–1401 对照，普通边界比较方向一致；不因 tierMin 的文字“strictly below”就误改相等时的实际原版语义。EnergyNodeDefinitions 的两种太阳能板 8/16 EU 输出与原版 10050/10051 相符，不能将锗板凭 HV 名称改成 512。

电冷却器目前仅输出 CU，EnergyNodeBlockEntity 的类型查询也只支持 spec.outType；原版 10161–10165 另有 HU 输出。这是需继续移植的功能差异，当前定义注释已经承认，不应算作本周新引入的已证实回归。

BehaviorDataStorage/16 已核对提示分支：USB 用详细数据、HDD 16 槽用简要数据，区分未格式化数据标签与空标签。BehaviorDataStorage 注释仍描述旧的复制机状态，但实现已委托 GTMaterialDataRecipes.replicatorEnergy，后者当前有中性/带电物质输入；不能依据这段旧注释推断物质系统完全不存在。复制能耗与原版的差异需结合实际机器输入另行验收。

LaserEmitterRecipes 的八种气体、每份 1 U、16 EU/t、128 tick 已对照原版 MultiItemTechnological:396–403；BatteryCellRecipes 已检查五种电解液填充定义。这里确认配方定义与查找路径，不代表这些气体的全部生产链已验证生存可达。

## 导线与反应堆接触行为核查

### 25. 导线也存在连续传输中断和错误能源接收问题

ElectricWireBlockEntity.transferElectricity 在调用相邻导线前先 visited.add(nbPos)，被调用导线又以 !visited.add(getBlockPos()) 为返回条件。与轴的第 23 项相同，两根连续导线的路径会在第二根中止。doEnergyInjection 也把错误能源类型、无面及模拟统一返回 amount，实际错误能源注入会报告接受；EnergyTransfer 的真实调用路径不先查类型。接入面的连接属性亦未在注入入口验证。

对照原版 gregapi/tileentity/connectors/MultiTileEntityWireElectric:170–187：父节点负责添加访问标记，递归入口不会重复添加后退出，并在递归前检查接收侧 isEnergyAcceptingFrom。移植时混合了两种 visited 处理规则。需要按同一规则修正电线和轴，验证一根/两根/多根、分支、双向断线、错误能源和实际接收量，而非只测方块注册。

### 26. 裸导线触电强度记录的是尝试输入，而非实际传输

ElectricWireBlockEntity.transferElectricity 在寻找接收器之前就更新 lastTransferTick 与 lastWattage，后者直接取本次输入电压×全部输入电流；没有负载也会记录通电，且同 tick 多次输入只保留最后一包。原版仅 rUsedAmperes > 0 才 addToEnergyTransferred，用损耗后的电压和实际接受电流累加，并在 tick 中交换本 tick/上一 tick 的统计。其过压/累计过流还会推进烧毁计数；移植版当前 transferElectricity 路径没有该保护。

HazardDamageTests:417–445 放置一根默认未连接导线，直接调用 transferElectricity，未断言实际接收量，随后要求记录全部输入功率并造成伤害。该测试通过不能证明忠实移植，反而固定了“无负载也按尝试输入触电”的行为。应先接真实负载、断言接受量，再测裸线伤害、绝缘及停止供能；另加无负载和多包累加对照。

### 27. 反应堆开关状态被误作正在反应状态

ReactorHazards.isRunning 仅判断 !stopped && !failed，没有检查中子或反应。原版 MultiTileEntityReactorCore1x1:84–100 由中子计数和燃料棒反应更新 mRunning，基类接触伤害检查的是 mRunning，不是 !mStopped。当前空堆开启或只有空棒也会进入热伤害与辐射路径。

HazardDamageTests.runningReactorCoreBurnsAndIrradiatesOnContact 特意装入 9201 空反应棒后只执行 setStopped(false)，未经过实际反应，即要求触碰造成 5 点伤害和辐射；这是错误条件被测试固化的另一个实例。应区分空堆开启、惰性棒、实际运行堆和关闭后的余留反应，而不是机械把伤害条件换成开关。

### 反应堆生命周期与掉落边界

ReactorCore2x2BlockEntity 确实继承 ReactorCoreBlockEntity，因此四棒方块 getDrops 中的基类 instanceof 能匹配，不是抄错类型。两种 getDrops 都从方块物品 NBT 删除 rods/overflow/neutrons，onRemove 再通过 dropContents 单独掉棒，避免把燃料同时保存在方块物品与地面物品中。仍需实际采掘/爆炸测试确认调用时序，但不将这条源码路径误报为必然复制。

ReactorNetwork 的 PENDING 改用 WeakHashMap，却以包含 ReactorCoreBlockEntity 的 Set 为强引用 value；BE 又引用 Level，所以 value → BE → Level 仍能保活 key。正常 LevelTickEvent.END 会 remove，此问题只涉及注释声称已解决的“入队后不再完成 tick/卸载维度”异常生命周期，不代表普通运行已证实持续泄漏。若要保证该边界，应有显式 level unload/server stop 清理或不强持有 Level 的排队表示。

PlayerRadiation 的 0–127 剂量限幅、死亡清除/非死亡克隆保留路径已读；严重阶段新增直接伤害是代码明确承认的移植取舍，不能以原版精确还原描述。此轮未做游戏内伤害、维度切换或死亡复测。

## 物品行为与工具交互核查

### 28. 建筑魔杖误读原版分支，所有方向都往上放

BehaviorBuilderWand 的说明把原版普通建造的 :123 引用写成 SIDE_TOP，并由 placeOnTop 固定使用 Direction.UP。直接读取当前原版 Behavior_Builderwand:82–135 后确认，普通分支传给 tryPlaceItemIntoWorld 的是 aSide/aHitX/aHitY/aHitZ；固定 SIDE_TOP 是前面的神秘时代节点建造分支 :66。当前实现因此不能正确沿墙面向外或向下扩建，长注释的“原版有效行为就是往上”结论错误。

DebugToolBehaviorTests 的建筑测试均以 Direction.UP 调用，测试文字重复了该错误引用，无法识别侧面行为。回归应增加四个侧面与底面，并检查对应方向的材料消耗与落点。

魔杖还把 REACH 固定为 4，理由是每种工具只有一个物品、无法读材料；但 GTToolHelper 已有 GT.ToolStats 和 getStatMaterial，BUILDER_WAND 也有工具头前缀。单一 item id 不代表物品栈没有材料，范围应从实际工具材料质量核实。findSourceBlock 只比较 Block 身份，未核对 BlockStateTag 等变种数据，原版则核对元数据。不能把这些差异称作完整还原。

### 29. 活塞式疏通工具未迁移到 Forge capability 接口

BehaviorPlungerFluid.plungeFluid 只接受 BE instanceof IFluidHandler；虽然写了六面循环，循环变量 side 从未用于获取能力或排液。现代 Forge 方块通常以 sided capability 暴露液体，不要求 BE 本身实现接口；例如当前 ReactorCoreBlockEntity 把 fluids 暴露为 capability，自身不实现 IFluidHandler，因而该工具不能处理它。对直接实现 IFluidHandler 的管道，又会绕过其分面包装器与过滤条件。

原版 :53 调用的是 drain(tDirection,...)，确实使用每一个面的接口，不能把 1.7 的 instanceof 与新版的无面 drain 原样拼接。回归应涵盖仅 capability 的容器、分面禁排容器、过滤管、空容器及耐久耗尽；应保留原版清除流体的目标，同时明确所走的面，不能只是让同一次无面查询重复六遍。

### 30. 胶带只有物品行为框架，没有接入实际方块

全 Java 源码搜索 BehaviorDuctTape.Tapeable/onTape，仅找到接口定义、调用和 ItemBehaviorTests 的 lambda 模拟目标；没有实际方块或 BE 实现。因此胶带物品不能完成原版 Mass Storage 的封装功能。ItemBehaviorTests 已明说实际世界点击应返回 false，不能将这一测试通过计入可用生存功能。应把胶带列作“框架及耗材逻辑已实现、目标方块未接入”。

### 调试工具与遥控器的差异边界

BehaviorChunkEraser 的高度仍为 1–249，仅按当前世界上下界裁剪，因此不等于清空 1.20 主世界除基岩外的整个高度。它不检查建造权限是当前代码刻意保留的原版调试行为；本轮没有执行删除，也不把这个历史语义擅自当作普通工具进行修复。后续兼容验收需明确调试物品的取得范围和实际权限策略。

BehaviorRemote 已按维度键存储、限制正常绑定 64 点、检查 128 格立方范围并避免加载未加载区块。范围内但未加载的坐标会因 target=null 被移出绑定，原版同类查找也可能如此；本轮不误报成已经验证的移植回归。反馈仍为硬编码英文，声音未接入，是可见的完成度缺口。

## 喷罐、点火器和扫描器核查

### 31. 一次性点火物在创造模式仍被消耗

BehaviorLighter.useOn 的 singleUse 分支在随机失败及成功点火后直接执行 can.usedUp，没有 creative 判断。match/fireStarter/fireStarterBark 的 used 均为空，走此分支；usedUp 会 shrink(1)。原版 Behavior_Lighter:94–98 在 useUp 前明确检查 !hasInfiniteItems。多次使用型打火机的 consumeFuel 已有创造豁免，所以仅测试普通打火机不能覆盖这个差异。应分别验证火柴、两种引火器的成功/失败与创造/生存数量变化。

### 32. 扫描器显示信息不消耗能源，且漏掉 capability 容器

ItemBehaviors.useOn 的 scanner 分支直接显示 BehaviorScanner.scan 返回的全部文本，再把 cost 存入 static lastScanCost，没有调用能源扣除或验证剩余电量。原版 Behavior_Scanner:49–54 调用 useEnergy(EU,...,scanCost,...)，仅成功时发送信息。当前便携扫描器仍属于功能简化，计算出成本不等于完成用电实现。应以实际玩家持有的已充电/耗尽扫描器测试，并区分 debug_scanner 的调试政策。

BehaviorScanner 的流体部分同样只接受 BE instanceof IFluidHandler，未查询点击面的 Forge capability，因而遗漏反应堆等仅通过能力包装暴露液体的容器。输出大量硬编码英文，未完成既有中文语言目标。

### 33. 去色玻璃板丢失含水状态

BehaviorSprayColorRemover.decolorize 把 StainedGlassPaneBlock 直接替换为 Blocks.GLASS_PANE.defaultBlockState，没有复制 WATERLOGGED。正常生存中含水染色玻璃板使用去色喷剂后，新方块的含水标志变为 false；流体更新是否从周围重新灌水取决于环境，不能据此认为没有状态丢失。应在孤立水源与相邻水域分别验证，并保留连接属性或触发正确重算。1.7 原版没有现代含水状态，直接复制默认状态不足以满足 1.20 语义。

### 喷剂与泡沫的完成度边界

硬化/去泡沫喷剂已接到 CFoamBlockEntity.dry 与 CFoamBlock，消耗计数走共用 Consumable；但 Extinguishable 仅找到接口与调用，没有实际机器实现，所以普通灭火/烈焰人效果不能代表燃烧室灭火已接入。泡沫的半砖费用常量存在，不代表半砖处理分支存在。此轮仅核查这些路径，未验证全部原版喷剂对象及声音。

CFoamBlockEntity 保存 timer 却在 tick 增加 timer 时不 setChanged，只有成功换块时使世界改变；区块上次保存后仅自然计时可能不被重新保存。这里记录的是持久化风险，不能直接断言泡沫永远不会干燥；其随机干燥仍会运行。dry 还在检查 setBlock 返回值前把 dried 置 true，若替换失败，剩下的湿泡沫 BE 会停止重试，宜覆盖边界验证。

## 矿石旧档迁移与堆叠方块核查

### 34. 普通旧区块的矿石背景迁移没有经过预期回调（已退出修复范围）

确认（2026-09-19）：模组尚未发布，目前仅本人测试，可以不保留旧矿石迁移。因此本项保留为审计记录，不再作为发布阻塞或要求补齐的功能，不投入真实旧区块迁移测试。后续可移除旧矿石实体、仅服务迁移的注册与测试，并同步修正文档；正常新世界的矿石背景、掉落、材料绑定和加工配方仍须保留并验证。注意 OreBlock 当前物品数据仍引用旧类的 TAG_STONE，清理时须先解耦共用数据键，不能直接删除类造成正常矿石物品背景丢失。本次仅调整审计范围，尚未删除实现。

OreBlockEntity 把旧 stone 标签迁移放在 onLoad，类说明假定所有旧实体都经过 LevelChunk.promotePendingBlockEntity。已直接读取本机 Forge 47.4.20 mapped sources：ChunkSerializer 的普通完整区块恢复路径（395–405 行）对 keepPacked=false 的标签调用 BlockEntity.loadStatic 后，直接调用 LevelChunk.setBlockEntity。后者仅在当前 state.hasBlockEntity() 时设置 level 并加入实体表；OreBlock 已移除 EntityBlock，所以旧实体被拒绝，不会进入随后已注册实体的 onLoad，stone 标签未迁入状态。keepPacked=true 的待解包路径才可能经过类注释描述的 promotePendingBlockEntity。

OreBlockStateTests.legacyBlockEntityMigratesIntoTheBlockState 手动 new 旧实体、setLevel、addAndRegisterBlockEntity，并再次手动 onLoad，证明的是人工构造路径，而不是正常存档读取。当前源内未找到 ChunkDataEvent 等额外迁移处理。因此旧基岩/黑花岗岩等背景可能退回默认 stone，而不是“旧档首载自动无损收敛”。需使用真实旧区块 NBT（正常非 keepPacked 实体）加载、保存、重载，确认背景与基岩不可采掘属性保留。此轮只核查实际加载源码，没有打开或修改玩家存档。

### 35. 金属堆与硬币堆的材料没有同步到客户端

PileBlockEntity、CoinPileBlockEntity 仅实现 saveAdditional/load，没有 getUpdateTag/getUpdatePacket；syncState 只同步数量属性（STACK 或 COINS/FILL）。GregTechClient:290–310 的颜色回调却从客户端 BE 的 stored/coinItem 取材料，缺少同步时得到空栈，回退白色。服务端持久化正确并不能使该材料自动到达客户端。

需要覆盖首次进入区块、向空堆放入铜/金、数量变化、重新登录及远处旁观玩家；补同步后还要使已烘焙的方块颜色重新渲染，不能只检查 NBT 保存成功。RockBlockEntity 则已有 updateTag/updatePacket 与材料变化刷新，是本项目中可对照的已实现路径。

### 36. 堆叠方块查询掉落会修改库存

PileBlock.getDrops 与 CoinPileBlock.getDrops 在构造掉落列表后调用 clearContents，onRemove 也依靠这个清空避免重复掉落。这使“仅查询掉落、最终未破坏”的调用也会清掉真实库存，存在机器/其他模组预览或模拟掉落时丢物品的兼容风险。还会导致第二次查询与第一次结果不同。正常玩家破坏可能恰好只查询一次而通过测试，不能据此证明该设计安全。

应把纯掉落计算与最终破坏时的库存处理分开验证，覆盖重复查询不改状态、正常采掘只掉一次、直接替换与爆炸；本轮没有进行第三方机器复现。PileBlockEntity 另按“同材料同前缀”合并而非 ItemStack 完整标签相等，可能把后放物品的自定义 NBT 转成第一栈标签；这是其注释承认的统一化取舍，应限定可统一物品，不能默认任意同材料的带数据物品都可互换。

## 传感器与生态方块核查

### 37. 多种传感器的测量对象不符原版

SensorBlockEntity.measure 的 LASEROMETER 无条件返回 0，注释仍写激光系统未移植，但当前已有 LaserFiberBlockEntity 和 LaserConverterBlockEntity。原版 MultiTileEntityLaserometer 读取光纤的 mTransferredLast，最大值 65535；现有注册及贴图不能算测量功能完成。

TACHOMETER 读取 RU 储能/容量；原版 MultiTileEntityTachometer 读取轴与齿轮箱的传输量，最大值为功率×速度。当前轴并不是储能设备，因此这个分支不能代替原版测量。GIBBLOMETER 则混在普通流体容量分支中，直接显示液量；原版读取 ITileEntityGibbl 的压缩度及其上限，并除以 1000。这两种读数的物理意义都发生了变化。

STACKOMETER 将所有槽的物品数相加再除以 64；原版 MultiTileEntityStackometer:43–68 统计可访问槽中非空物品栈的个数，并排除展示流体，最大值是槽数。例如两个槽各放 1 个石头，原版为 2，当前为 0；单个不可堆叠工具也是 0。后续测试需使用真实测量目标和不同堆叠上限，不应只验证传感器 item id 与单位图标。

TPS 采样已确认有独立 20 tick 条件，不能因为 serverTick 每 10 tick 调用 measure 就误判公式翻倍；重量解析 CrucibleItemInput.parse 按单件返回材料，再乘 stack count 合理，本轮排除重复乘数量疑点。

### 生态核查范围与差异

BushBlockEntity 的生长增量会 setChanged，berry 字段有更新包；成熟采摘后复位阶段，设置浆果时不消耗玩家浆果与原版 MultiTileEntityBush:174–177 一致，不应当成复制错误。当前 BushBlock 只有地面放置，无原版附着其他灌木并继承其浆果/速度的朝向状态，且种植地仅 DIRT tag/MOSS，原版还有 canSustainPlant 等兼容判定；属于待补的种植兼容与扩展差异。

TreeHoleBlockEntity.countTreeLeaves 对周围 3 格内树叶和上方树干直接 getBlockState，未使用 hasChunkAt；当树洞位于已加载区块边缘，刷新会读取邻区块，可能触发同步加载。需要边界区块测试，不能仅凭局部树形测试证明不会加载邻区块。临时 timer 不保存本身不判 bug，原版也使用实体运行计时器。

BumbleHiveBlockEntity 的九槽库存有持久化、变化回调并明确拒绝自动化 capability；不能因为其 rawHandler 存在就称为可接管道。此处仅核查库存层，繁殖、掉落与世界生成完整性仍须结合蜂箱方块和养蜂箱实现继续审查。

## 养蜂箱与基岩钻机核查

### 38. 养蜂箱寿命与配对倒计时不单独标记存档修改

BumbliaryBlockEntity.tickLogic 会 --life、--countdown，并修改 brood，但没有 setChanged。当前 setChanged 主要来自库存变化回调和 onOpened；在蜂后尚未产物、未死亡或公主等待配对期间，进度变化自身不会要求保存。因此存档后仅等待、再卸载区块时可能恢复旧寿命/倒计时，实际是否回退取决于是否有其他操作把区块标脏。应先保存，再在不改变库存的时间段推进、卸载并重载，验证持续进度。

### 39. 养蜂生产环境与攻击行为仍未接入

produceComb 明确跳过原版 bumbleCanProduce/bumbleCanProduct 的工作场所检查；原版 MultiTileEntityBumbliary:169–173 必须满足物种所需的花、水、石头等条件才产出。当前只检查气候、昼夜和天气，在不具备物种所需周边方块的地方仍可能产出。这是生存规则缺失，不是单纯动画细节。

tickLogic 同样跳过原版蜂群攻击，理由是没有蜜蜂伤害类型；当前 GTDamageTypes 已有 BUMBLE 和 bumble(Level)，所以该理由过时，但伤害类型存在也不代表攻击、基因概率和防护判断已接入。后续需核对普通/高级养蜂箱各自范围，不能仅接一个伤害调用就宣称一致。

GTBumbleProducts.count() 确实为 1，与原版物种单蜂巢产品一致，因此 produceComb 循环忽略多产品索引在当前 GT6 基础内容范围内不判为 bug。满输出时放不下的蜂巢被丢弃也有原版依据，不应随意改成额外缓存后再称忠实还原。

### 基岩钻机已检查路径与剩余差异

BedrockDrillControllerBlockEntity 的出货先保留目标返回余量，生产前检查输出槽、32768 RU 与 100 mB 润滑剂，确定有产物后才扣资源；NBT 中能量有限幅，结构地板检查每个区块是否加载。该路径没有发现先扣资源再因满槽丢产物的简单错误。

产物仍明确简化为 oreRaw，原版 MultiTileEntityBedrockDrill:160 之后还有不同矿石形态的选择分支；首个 stoneType 固定默认 0，而原版创建时随机选择。逆转 RU 的 doInject 用绝对值接受，getEnergyDemanded 却仅接受正 1024–4096，查询与执行不一致，需用会先询问需求的供能端验证。主控直接 fluid capability 暴露原始润滑剂罐、部件则暴露只进不出的 FluidPort，也是后续端口契约需核验的边界。

结构每 tick 重扫，注入时又重复校验，属于性能检查候选，当前没有测得其实际占用，不能据此归因启动或卡顿。

## 单方块机器加工与产出核查

### 40. 输出空间限制不会拒绝单次加工，满槽仍可扣料开工

BasicMachineBlockEntity.outputLimitedParallel:709 起在 maxParallel <= 1 时直接返回，且后续缩减批次的两个循环最多降到 1，不能返回 0。RecipeMap.findRecipe:582–603 只对 mNeedsEmptyOutput 配方检查物品输出槽为空，不是一般的输出容量检查，也没有流体输出容量参数。因此一般配方在输出槽已满或装有不兼容物品时仍可能启动，checkRecipe 随后消耗输入。原版 MultiTileEntityBasicMachine.canOutput:631–674 明确在输出不兼容、已满或需要的空罐不足时返回 0，阻止加工。

这不是已证实的产物丢失：MachineWorkOutputs.flush 保留未交付余量，机器完成后重试；saveAdditional/load 也保存 gt.pending_outputs。当前问题是提前消耗原料和能量、占用加工任务，与原版开工条件不一致。现有 PortRegressionTests.pendingOutputsSurvivePartialFlushAndReload 检查的是产物辅助类的部分交付及保存恢复；machineDropCarriesPendingWorkWithoutInventoryDuplication 人工注入完成任务，均不能证明真实机器在满槽时拒绝开工。应增加真实开工检查，断言输出满/不兼容时输入和加工状态不变，然后腾出空间确认只开工一次。

### 41. 流体并行预判未为已使用的空罐记录流体类型

BasicMachineBlockEntity.fluidsFit:758–778 只维护每罐剩余容量 free；对后续产物仍读取原始 tanksOutput[i].isEmpty/getFluid，没有像 itemsFit 那样更新虚拟内容。于是同一空罐可在模拟中先分配给流体 A，再把剩余空间分给不同流体 B。实际 flush 会改变罐内容并拒绝 B，预判与交付规则不一致。例如只有一个可用空罐、另一个罐被无关流体占满时，A/B 总量虽小于空罐容量，仍至少需要两个可接收对应流体的罐；当前模拟可能认为能容纳整批。

这可能高估并行数，使完成任务等待清空输出罐；暂存机制仍可保留余量，不能据此称为丢液或永久无法恢复。本轮为源码与原版对照，没有启动游戏复现。应补两种以上不同流体、多罐部分占用、同种流体合并的真实机器测试。

### 已核查的守恒与持久化边界

RecipeInputs.consume 在物品与流体副本上规划扣除，失败返回 null；催化物先预留再恢复，避免同一份原料同时满足消耗和催化要求。checkRecipe 成功规划后才写回输入，并一次性生成暂存产物。MachineWorkOutputs.flush 不覆盖不同物品/流体，保存的是未交付余量；本轮未发现这条普通路径中简单的重复发货或失败后部分扣料问题。此结论仅限所读路径，不能覆盖所有材料等价匹配、外部模组 handler 或整份 BasicMachineBlockEntity。

## 研磨、回收与砧配方核查

### 42. 砧粉碎生成器未使用原版的粉碎目标材料

原版 RecipeMapHandlerPrefixShredding.getOutputMaterial 明确返回 aMaterial.mTargetPulver.mMaterial，父类 RecipeMapHandlerPrefix:211–215 用该材料创建所有指定输出前缀。当前 AnvilShreddingRecipes.grind 却把主产物和副产物都构造为原输入 material；更早注册的 StoneAndToolSurvivalRecipes:48–52 也把 rockGt 直接变为同材质 dustSmall。后者在 GregTech.java:100 注册，AnvilShreddingRecipes 在 :148 注册，修复时必须同时检查先注册的同输入配方，不能只修后者。

对于粉碎目标不是自身的材料，此规则会保留本应转换的材料身份。GTMaterialRegistry.postInit 已配置 WroughtIron→Fe、IronMagnetic→Fe、Graphene→C 等目标，所以不是缺少材料关系定义。实际受影响的可用形态仍需在注册完成后逐项列出；不能把没有对应输入形态的材料也计为游戏内受影响配方。RecipeFamilyCoverageTests.anvilGrindsOreChunksByHand 只选第一个匹配材料，并把同材质输出写进期望，未覆盖转换目标。

### 43. 砧敲击交互测试清掉锤子后把取回原料当成成功

AnvilSmashingTests.smash 先 setItemInHand(MAIN_HAND, hammer)，紧接着 player.getInventory().clearContent()，然后调用 anvil.interact。已读取本机 mapped sources 中 Inventory.clearContent:578–582，清除的是包含快捷栏在内的全部 compartments，所以此处主手已经为空。MaterialAnvilBlockEntity.interact 的顶面空手分支会将对应 work 槽原料 give 给玩家并清空该槽，而不是调用 hammer。

测试最后返回玩家选中槽，oreFormsSmashWithAHammer 只断言这个栈非空，因此取回原料即可通过；没有验证输出物品身份、数量、锤子耐久或砧耐久。此测试当前不能支持“按玩家方式实际粉碎成功”的结论。修复应把清空背包放到设置锤子之前，明确校验完整主副产物，并验证原料消耗及磨损；仅修改测试顺序后看到红，不应降低断言绕过真实问题。本轮是源码路径核实，未重新运行 GameTest。

### 粉碎换算的潜在错误与已排除项

MortarGrindingRecipes.pulverize 使用 amount * U / targetAmount；原版 OM.pulverize:370–372 调用 UT.Code.units(amount,U,targetAmount,false)，实际公式为 amount * targetAmount / U，且目标量 0 返回 0。当前 helper 反用了比率，并将目标量强制至少为 1。当前 GTMaterial 默认及 GTMaterialRegistry.postInit 全部显式粉碎目标量均为 U，两式目前等价，因此不将此项计为已发生的材料增殖；扩展非 1:1 粉碎目标或禁用目标前须修正。ShredderRecyclingRecipes 与 CrusherFamilyRecipes 的残余粉末分支也调用这个 helper。

AnvilWorkInputs 独立识别空栈配方标记并要求空工作槽，不同于通用 RecipeInputs 忽略空输入的语义，因此不能仅凭通用匹配代码认定砧的空槽要求失效。CrucibleSmeltingRecipes 的配方明确以 addFakeRecipe 注册用于展示，不能把这批显示行计作普通机器的可执行无热量冶炼配方。上述文件只完成有针对性的源码核查，尚未完成所有注册配方逐项运行验证。

## 熔炉与形态转换配方核查

### 44. 熔炉生成器重复乘冶炼比例，测试也复制了错误公式

FurnaceSmeltingRecipes.form 将 solidAmount 赋为 smeltingAmount，再计算 units(smeltingAmount,U,solidAmount,false)。原版 Loader_Recipes_Furnace.Listener_Furnace_Smelting:194 的第三参数是目标材料的 mTargetSolidifying.mAmount，普通金属应为 U，而不是输入材料的冶炼比例。当前公式把比例平方：例如 MaterialOreProcessing 中 Galena→Pb 的比例为 U/3，1U 方铅矿粉本应转换 U/3 铅，当前生成器只计算 U/9。按 OM.ingot 的金属粒分支分别为 3 粒和 1 粒；更低比例可能因舍入得不到任何产物而直接跳过注册。

独立整数计算记录在 `docs/weekly-audit-furnace-arithmetic-2026-09-19.json`，包含 Galena、Tetrahedrite、Malachite 三个 FURNACE 名单内的材料。该记录证明公式差异，不是启动后最终注册表或实机熔炉结果；早期配方可能占据同一输入，实际影响清单须结合注册顺序核实。

FurnaceSmeltingTests.targetAmount 同样把 smeltingAmount 作为第三参数，无法独立检出错误；dustSmeltsBackIntoItsMetal 与 pilesAndNuggetsSmeltIntoTheirOwnForm 主要使用 ItemStack.isSameItemSameTags，不比较栈数量。另有 firstRow 采样、缺失样本时跳过，不能证明非 1:1 材料与全部形态正确。应按原版比例写独立期望，检查材料、形态、数量，并覆盖至少一种非 1:1 矿物和实际熔炉加工。

### 形态转换与焊接的核查边界

MaterialFormConversionRecipes 的常规粉末、锭、金属粒装拆比例按前缀单位核对，未发现这些列出的简单装拆对本身不守恒。装箱使用 integrated_circuit；GTTechnological.registerAll 已将这 25 个物品标记为 TechItem catalyst，Recipe.isCatalystInput 可识别，不应把每行创建一个电路栈误判为每次加工消耗电路。仍需单独审查材料等价替换、碰撞优先级及外部标签。

SharpeningRecipes 和 WelderFamilyRecipes 明确省略 COATED.NOT；这仍是对原版的加工条件缺口。SharpeningRecipes 的残余粉末调用第 43 项后所述 pulverize helper；WelderFamilyRecipes 的 skipped 文字称只有 medium 管道，已与本项目其他尺寸管道注册现状不符，不能据此继续判定所有对应管道不存在。是否已由其他注册器补齐需继续逐配方核验。本节未将这些说明当成完成证明。

## 材料扫描、打印与复制配方核查

### 45. 动态扫描缓存遗漏输入形态与 NBT，可能拒绝有效输入或覆盖硬盘数据

GTMaterialDataRecipes.scanner:358–382 缓存键只有材料 ID 与 stick/drive 加等级，缓存值却包含首次 target 和 stick 的完整副本。相同材料的锭与粉末等不同形态共用缓存；RecipeInputs.matches 不把不同前缀当成等价，所以先扫描一种形态后，另一种形态可能无法匹配首次生成的输入。原版 RecipeMapScannerMolecular:46–67 每次按实际输入构造配方，不存在这种跨形态缓存。

更严重的是介质已有文件不在缓存键内。若首次使用无 NBT 的空硬盘，随后同材质、同等级扫描遇到已有文件的硬盘，缓存输入的无 NBT 匹配允许它通过，但输出仍是第一次空盘生成的那份数据，已有其他文件不会随当前硬盘复制到输出。反之首次为带数据硬盘，后续不同 NBT 可能被拒绝。printer 同样只以材料 ID/页数档缓存，却闭包捕获第一次纸张和 U 盘副本，重命名或附加数据变化可导致错误拒绝。缓存为静态且未见清理入口，不能把不同机器之间视为隔离。

应缓存与输入无关的成本/材料信息，按本次输入创建配方及输出；若保留栈相关缓存，必须覆盖所有影响匹配和输出的数据并控制容量。复测必须按不同顺序连续扫描同材料不同形态，给已有多个文件的 HDD 追加扫描后逐槽检查数据；当前单次样本测试不能证明这一点。本轮没有在玩家存档中复现数据覆盖。

### 46. 打印与复制把原版耗时和功率参数交换

原版 Recipe 构造器（gregapi/recipes/Recipe.java:873、877）尾部参数为 aDuration、aEUt、aSpecialValue。RecipeMapPrinter:143 附近的材料词典行传入 512/1024、16、0，即 512/1024 ticks、16 EU/t；移植 GTMaterialDataRecipes.printer 返回 16 ticks、512/1024 EU/t。总能量乘积相同，但所需功率、可用机器等级、加工时长及超频行为不同，不能视为等价。

原版 RecipeMapReplicator.getReplicatorRecipe:88–114 传入 tPower、1、0，即 nucleons×256 ticks、1 单位功率；当前 replicationRecipe 传入 1、power、0，同样交换了时长和功率。现有 MaterialDataChainTests 明确把错误功率写进断言，仅验证动态配方表及物质输入，未验证这两种真实机器按原版能量包工作。应独立核对机器注册的能源类型与功率后修正并做实机测试，而不是保留测试期望去调整原版规格。

### 47. 打印词典会消耗 U 盘，未保留原版催化输入

原版打印配方使用 ST.amount(0,tUSB)，表示读取但不消耗。当前 printer 把 stick.copyWithCount(1) 放入输入，未调用 withCatalystInputs(1)；usb3_stick 也不属于 GTTechnological 标记为 catalyst 的模具或 integrated_circuit，因此 RecipeInputs.consume 会正常扣除它。产物仅有书，不返还 U 盘。现有 printerPrintsTheScannedDictionary 只查看配方结果，没有执行扣料或验证 U 盘留存。修复需显式保留数据介质，并测试连续打印两本后文件及 U 盘数量不变。

### 其他核查边界

MachineCasingRecipes 四档 6 张板加 2 长杆/4 短杆，材料总量为 8/14/26/56 U，当前列出的输入数量与档位一致；这里只核查焊接路径，未证明各级机器整条生存前置齐全。GTMaterialDataRecipes 文档仍称没有 HDD 交换机，已过时。扫描器直接写 HDD、扫描任意材料组成物品、复制不检查 UUM 等明确扩展仍与原版不同；直接返回成书是既有取舍，不等于打印页与装订路线完整移植。复制机先选物品形态、最后固定 1000 mB 流体的策略也没有实现原版低熔点流体优先与材料优先前缀，需要后续逐材料检查。

## 流体泉、基岩矿与地牢传送门核查

### 48. 自然流体泉未调用地表指示草，概率与基岩矿互斥也未还原

GTFluidSpringsFeature.place 在抽签和床岩检查后直接 return carve；carve 结束也未调用 indicators。源码检索未找到 indicators 的生产调用，因此现有地表指示草实现没有接入自然生成。FluidSpringTests.indicatorGrassFollowsGt6 仅验证三种 indicatorBlock 对象不同，不能证明生成了地表指示；craterFollowsGt6 则直接调用 carve，绕过完整入口。原版 WorldgenFluidSpring.generate:82–100 在同一次成功生成中执行地表指示。

当前入口先在七种 SPRINGS 中均匀选一种，再执行 1/probability；原版为各注册实例分别判断概率，成功后用 CAN_GENERATE_BEDROCK_ORE 排斥后续泉。单种当前无条件抽中概率为 1/(7×probability)，不是注释里的 1/probability。原版还要求 GENERATED_NO_BEDROCK_ORE，当前只检查中心底层为基岩或基岩矿，未检查该区块已有矿脉；carve 又直接覆盖上方各层，因此在泉与基岩矿落入同一区块时有覆盖矿体的路径。具体生成数量与重叠频率须用固定种子世界验证，本轮未测。

### 49. 基岩矿仍混用绝对高度与相对底层高度，小型矿也放成普通矿

GTBedrockOreFeature.placeVein 的散布段使用相对层数 y 从 7 循环到 level.getSeaLevel()-1，但放置在 floor+y。在主世界 floor=-64、seaLevel=63 时，最高候选位置为 -2，而不是接近海平面的 62；原版 WorldgenOresBedrock:213–223 从原版 y=0 地基向水位散布，移植后需要统一绝对/相对坐标。是否到达上限还受随机横向出界提前退出影响，不能断言所有散布必定达到该高度。

placeSmallBedrockOre 与普通基岩矿都调用 GTOreBlockResolver.placeBedrockOre；后者固定 lookup("ore_"+materialName)，没有选择 small ore。因此 2/6 的小型基岩矿分支实际放出普通矿。该情况不是旧矿石迁移，仍属于新世界内容差异。另有 16×16 区块网格固定候选加权选一种的规则，与原版每种矿物概率注册器不同；不能把这些网格规则当作已完整还原的原版分布。

### 地牢传送门的范围与待测边界

DungeonPortalBlock/BlockEntity 明确把地牢里的原版下界/末地传送门替换为自定义钥匙门，并借用 GT6 迷你传送门的比例搜索目的地；其实体传送不是 GT6 迷你门的管道中继功能。该路径不能作为“迷你传送门中继已移植”的证据。打火石能直接 activate 而不校验 keyId，钥匙并不构成严格开启门槛。

arrivalPlatform 无目标时直接替换 3×3 脚下方块为黑曜石、清除中心两格，没有保存替换方块内容、避开已有建筑或约束目标世界边界；大型实体也仅有一列两格净空。源码可确认这些缺少的判定，但本轮未实测玩家/宽体实体或边界坐标传送。应优先决定恢复原版地牢传送门还是保留此扩展，再按实际选定行为测试，避免把自定义实体传送包装成原版中继完成。

## 地表资源生成补充核查

### 50. 钶钽矿距离平方溢出，远处可能误入内圈（原版继承问题）

GTColtanFeature.distanceSquared 用 int 相乘并相加，placeField 随后直接与 480²、64² 比较。以中心 (0,0)、区块原点 (50000,0) 为数值见证，真实距离平方 2500000000，Java int 回绕为 -1794967296，会同时通过外圈和内圈判断。整数证据在 `docs/weekly-audit-coltan-distance-2026-09-19.json`，并非实际世界生成结果；实际中心来自世界种子，世界层面仍需对应种子/坐标复测。

原版 gregtech/worldgen/overworld/WorldgenColtan:59 也使用相同 int 表达式，因此本项不能称为本周新引入的回归。若目标是正常的 480 格局部矿区，应先提升减法及乘法为 long 再比较，并增加超出 46340 格和世界边界附近的测试；原版实现有缺陷不代表移植必须保留整数溢出。现有中心高斯抽样测试验证中心生成，不等于验证远处半径判断。

### 矿坑、黑沙和散落岩石已检查的边界

GTBlackSandFeature 保留河流必要条件及海洋/海滩/沼泽排除，列替换上限为 2；GTPitFeature 上限为 7，均复用 48×48 形状并涉及邻区块。其直接列方法适用于检查形状与宿主选择，不能替代真实 WorldGenRegion 下的跨区块放置、生成顺序和群系筛选测试。两者 setBlock 后直接记录 placed=true，未核验实际写入是否成功，统计返回成功不必然代表方块确实生成。

GTWorldgenBiomes 使用 minecraft 命名空间固定集合，GTPitFeature 又维护独立 plains/savanna 集合；模组新增河流、平原等即使在标签上属于同类，也不会自动进入这些判断。这是当前兼容范围限制，不能以“生物群系类别齐全”表述。isRock 仅覆盖部分 vanilla tags/方块，是否覆盖所有本模组岩层需要注册后 tag 检查，当前不以源码 grep 直接断言每种岩层都被排除。

GTRocksFeature.litterFor 的 1/2 特殊物、其内 1/12 陨铁与 1/4 原矿分支已对照读取；GTRockPlacement 的精确放置方法会写材料、显式物品 ID 与 rawOre，成功 setBlock 后才写 BE。普通 placeRock 使用 WORLD_SURFACE_WG 高度与地面上表面坚固检查，不能仅凭这些方法证明不同植被/水面上的完整自然生成效果。以上文件标记为针对性源码核查，不是完整运行通过。

## 地表生态与水体核查

### 51. 树枝群系倍率被额外概率门槛削弱

GTSurfaceFloraFeature.placeTwigs 先对基础两次循环抽 TWIG_PROBABILITY，失败直接 continue；只有成功后才为 multiplier-1 次额外尝试再次抽概率。原版 WorldgenSticks.canGenerate 先按整个区块群系计算 mAmount×倍率，WorldgenOnSurface 再对所有标记目标各抽一次概率，两者不同。单一森林、倍率 3、概率 1/2 时，当前每个基础循环期望尝试 1/2×(1+2×1/2)=1，总计 2；原版六次随机标记后按半概率选取，考虑重复列合并后期望约 2.97。数字指进入放置的候选列，不是最终树枝数量，地形仍会拒绝放置。

当前代码还按单个随机点群系决定倍率，而原版使用整个区块群系列表。SurfaceFloraTests 中检查 twigMultiplier 返回 3/2/1 不能证明调用方正确应用倍率。应以固定随机源测试候选列选择和完整入口，再用统计检查避免把每次放置成功数量当成固定值。

### 52. 共用地表射线遇到木头/树叶即返回，与原版跳过植被不同

GTSurfaceFloraFeature.surface 的 continue 条件包含 !LEAVES && !LOGS，所以树叶/原木无论上表面是否坚固都会直接作为结果返回。调用者 placeTwig、placeLog、GTBushesFeature.placeBush 随后判断植被不是可种植地而终止。原版 WorldgenOnSurface 明确忽略 wood/leaves，继续向下找地面。MOTION_BLOCKING_NO_LEAVES 高度图可跳过顶部叶子，但树干仍计入高度，向下路径也可能遇到其他树叶，不能据此排除此分支。

影响应表述为特定植被柱下放置被拒绝，不是所有森林完全无树枝/灌木。需要有原木与树叶遮挡的真实列测试，同时保留原版对液体、耕地和可替换小植物的规则。当前调用者多要求空气，比原版 WD.easyRep 的可替换范围更窄。

### 水体与蜂巢核查范围

GTWaterBodyFeature 按海洋→河流→沼泽执行，河流排除海洋、沼泽允许覆盖前面的 GT 水体，与其原版顺序一致；仅识别实际水方块，没有直接替换任意含水方块，不应把它误报成此前 FluidSpringBlockEntity 的含水方块覆盖问题。水生植物与不同水类型共存的现代生态效果仍待实测。

GTDeepOceanFeature 只接受 deep_ocean 单个群系，未纳入现代 deep_cold_ocean 等其他深海；其柱体放置及嵌矿代码已针对性阅读，仍需世界级生成验证。GTBumbleHivesFeature 的主世界入口确实执行地下和地表两遍；embedded 要求恰好五面不透明且邻面无流体，placeHive 会设置蜂巢、基因和蜂种库存。环境温度通过 ServerLevel 辅助器查询而非 WorldGenLevel，本轮未证明并行生成时该查询不会触发额外区块读取；保留为后续检查边界，不直接定性为死锁。

## 下界矿物与地表矿床补充核查

### 53. 下界散落物的条件被从单个抽签分支提升成全局覆盖

原版 gregtech/worldgen/nether/WorldgenRacks.generate 的 24 项 switch 只有 case 3–5 检查下界砖、case 6–11 检查灵魂沙/灵魂土、case 16–23 检查沙砾。当前 GTNetherScatterFeature.placeAt 在选表后对所有 choice 执行地面覆盖：沙砾永远变燧石，下界砖统一变四分之一远古残骸原矿或四分之三下界石英，灵魂沙统一变 Gloomstone/NetherQuartz。这改变了不受这些地面条件约束的其他分支。

普通地面 case 6–11 原版应为燧石，当前表返回 Gloomstone/NetherQuartz；case 2 原版有 1/4 原矿、3/4 rockGt 选择，当前固定 rawOre=true。case 3、4 非下界砖应为石英/荧石 gem，但当前分别保留 AncientDebris 原矿、Glowstone 材料，且显式 gem 输出只在 choice 0/1 创建，遗漏其他宝石分支。应按 24×各地面类型列出原版独立期望，并校验实际躺地物品掉落的前缀与材料，不只检查 TABLE 长度或显示名称。

入口高度还把原版 y=255 是否基岩改成 y=254 是否空气，以此选择 200/80 随机范围。现代普通下界顶部在 127 附近，254 为空气，导致大量搜索从顶板之上开始；具体生成量及顶板上的散落物需下界世界复测。该实现不是原版条件的直接等价移植。

### 54. 下界晶簇错误替换天花板，测试把误读固定成期望

原版 gregtech/worldgen/nether/WorldgenNetherCrystals.generate 先 while(air(++aY)) 找到天花板，再执行 if(--aY-10 < waterLevel)；这里的前置减一有副作用，随后 setBlock(aX,aY,aZ) 使用的是天花板下方空气坐标。当前 GTNetherDepositFeature.placeCrystalAt 在 y+1 天花板处 setBlock，并令 seedY=y+1，确实把原本应保留的天花板替换掉，整个生长原点也上移一格。

NetherWorldgenTests.netherCrystalsReplaceTheCeiling 明确断言天花板被移除，注释称旧版放在空气中是错的，恰与原版执行顺序相反。这是需要同时纠正实现、测试和文档的证据，不能拿现有绿测证明还原正确。本轮已逐句对照原版，未运行新的世界测试。

### 地表黏土矿床的额外行为

GTSurfaceDepositFeature 自述是移植版额外的近水黏土圆盘，不是 GT6 原版矿坑。它与 GTPitFeature 均移除草皮覆盖，而原版 WorldgenPit 对普通 grass 不按 dirt 分支处理，会保留草皮；这应作为明确取舍或待恢复项，不能计为原版完全一致。countWater 仅接受 Blocks.WATER；与同为 top_layer_modification 的 GTWaterBodyFeature 转换海水/河水/沼泽水之间存在顺序相关兼容风险，最终排序与固定种子效果尚未核验，当前不直接断言此功能完全不生成。

## 世界生成注册与树木、泥炭复核

静态引用检查结果保存到 `docs/weekly-audit-worldgen-links-2026-09-19.json`：GTFeatures 中 20 个 FEATURES.register 类型、20 份 configured_feature、20 份 placed_feature、26 条 forge:add_features 群系引用，未发现这些本地 gregtech 引用指向不存在目标。GTFeatures.register 也调用 GTStructures.register，将结构类型、放置类型、片段类型接到事件总线。这不是实际注册事件执行、群系标签展开、阶段排序或自然地形生成测试，也不推翻此前地牢定位与实体生成不一致的问题。

GTTreesFeature.plant 与第 52 项 surface 有同类条件错误：原木/树叶会停止射线并被后续 DIRT/SAND 要求拒绝，未按原版忽略植被继续向下。其群系按精确列检查且限固定原版名单，Rainbowood 未加入自然生成列表；代码明确把后者归为只有外部群系时才生成的差异，不能由树苗可生长推断野外能找到。树形本体、树苗掉落与所有物种的获取闭环不在本次入口核查结论内。

GTTurfFeature 的列循环已与 WorldgenTurf 对照：严格 y>lower、最多两层、普通泥土/粗泥土起始、木头/瓜类上覆保护、进入泥炭层后只继续岩石，主要顺序一致。它不像当前矿坑那样直接将所有 DIRT tag 方块视为泥土，不能把矿坑移除草皮的问题直接套给泥炭。原版该方法末尾 return temp 在已通过沼泽检查时可能仍为 false；当前返回实际 placed 属于合理返回值处理，不判为移植回归。岩石宿主覆盖、跨区块写入和模组群系兼容仍与前文相同，需要运行验证。

## 地牢刷怪房功能核查

### 55. 刷怪房保留建筑形状，但杀怪、收集与初始库存没有形成闭环

GTDungeonChunkRoomFarmMobs.generate 与 makePlatform 明确跳过原版 DungeonChunkRoomFarmMobs 的物品管道，包括中心 (8,4..6,8)、漏斗下方 (8,7,8) 和储物箱之间的分流管道。四个漏斗仍按东、东、南、下连接，最后一个向下输出处保留地板，不能把掉落物送到两层储物箱。GTDecorBlocks 注册 spike_steel 使用普通 new Block，没有接触伤害实现，因此也没有原版钢刺杀怪行为。这是直接可见的功能缺口，不只是外观与大型平台数量的取舍。

原版在十四个 mass storage 中写入各 1–8 个瓶子、骨头、线等物品；移植版只设置方块状态，不填入库存，其类注释也明确承认省略。没有生成辅助方法并不等于库存系统无法支持，后续应使用真实库存接口补齐，同时验证漏斗到箱子的全链路。当前仅核对源码，未进行自然刷怪或物品传输实测；不能断言所有生物都不会因其他原因死亡，也不把这些长期注明的缺口归因于最近一周的新改动。

活塞门和水流的放置目前使用 flags=2，是否需要额外邻居更新仍需结合世界生成后处理验证，本轮不单凭该参数判定机关失效。

### 56. 曲柄红石方向多翻转一次，现有测试固化了错误期望

CrankBlock.java 修改时间 2026-09-17 12:20:58，CrankTests.java 为 12:22:19，均属于本周候选；时间只能定位复核范围，不能证明由谁引入。原版 MultiTileEntityCrank:115–122 判断查询参数 aSide == OPOS[mFacing]；MultiTileEntityBlock:202 直接转发 vanilla 的查询侧，TileEntityBase06Covers:404–415 又将 aOppositeSide 原样传给 isProvidingWeakPower2/StrongPower2，并没有先转成实际输出侧。IMultiTileEntity 的对应接口注释明确提醒 vanilla 传入反向侧。

因此原版实际给 mFacing 所在的安装墙体供电。移植版 FACING 已定义成 OPOS[mFacing]（手柄方向），正确查询应匹配 FACING；当前 isProvidingPower 却匹配 FACING.getOpposite()，又反了一次。现代 SignalGetter.getBestNeighborSignal:90–91 同样调用 getSignal(pos.relative(direction), direction)，没有可解释此翻转的引擎语义变化。例：手柄朝北、安装墙在南侧，墙体查询方向为北，当前返回 0，而手柄北侧查询南返回 15。

CrankTests:143–154 明确断言安装墙没有信号、手柄前方有信号，正好将错误解释固化。GTDungeonChunkDoorPiston.crank 的朝向换算本身合理，但依赖这个有误的曲柄输出，不能认为其原版墙内电路可正常触发。后续应同时修改方向、注释及独立期望测试，并实测四向地牢门完整开合；不应只修改测试成与代码一致。本轮已完成源码调用链核验，尚未进游戏重现门的动作。

### 57. 地牢杯子仍替换为空花盆，所依据的“没有杯子方块”注释已过时

GTDungeonData.cup 直接调用 pot，后者只放置空 FLOWER_POT。当前 PortableFluidContainerSpec.CUP 和 GTToolBlocks 的注册循环已提供 fluid_cup 可放置容器，不能再以缺少该方块作为替代依据。原版 DungeonData:313–317 放置 32739 并填入 250 L 指定流体；例如 DungeonChunkRoomLibraryNormal 四面桌上的杯子装长效夜视药水，移植版调用连流体参数都省掉了。兵营等调用也需要逐一恢复饮料或原版可选装饰分支。

仓库三种木桶/铜鼓的 16000/32000/64000 容量已与 TankDefinitions 对上，当前没有证据将这些正常数量判为容量溢出。GTDungeonData.tank 的一般能力分支忽略实际 fill 返回量，仍需对各气瓶逐项核查实际注入结果；本轮没有证明所有气瓶都被填满。crate 使用材料前缀方块，本来不是普通库存箱，不能把它不写 BlockEntity 库存误判成空箱。

### 58. 地牢自然生成的 ZPM 总为空电量，充能随机参数被忽略

GTDungeonData.zpm(ax,ay,az,active) 只设置 ZPM.defaultBlockState，完全不读取 active。ZpmModuleBlockEntity 的 energy 默认 0，仅 load 从 gt.zpm.energy 读入，ticker 只更新亮度，不产生或初始化电量。GTDungeonChunkRoomLibraryNormal 调用的无 active 重载虽执行 next2in3，却无法影响结果。因此当前这条自然生成路径总是空 ZPM，破坏掉落继续保存 0 电量，无法按原版取得充能遗物。

原版 DungeonData:309–310 写 NBT_ACTIVE_ENERGY，TileEntityBase08Battery.readFromNBT2:70–73 在该标志为真时将 mEnergy 设为 mCapacity，假时再读已有电量。当前辅助器“没有 energised block state”的注释也已过时：ZpmModuleBlock 已有 CHARGE，但修复关键是实体电量，不能只改变发光状态。ZpmRegressionTests.zpmPlacementPreservesChargeAndLegacyKinds 手动 z.load 充能 NBT，其他测试使用 ZpmEnergy.charged，均不证明地牢生成能获得电量。应补 true/false 两种生成、保存重载、挖下放入放电器的独立回归。

### 放置容器与 ZPM 检查边界

PortableContainerBlockEntity 的流体能力通过单个 vessel ItemStack 转发，不另建一份可漂移的储罐；写入 setChanged 并发更新包，缓存能力在 invalidateCaps 失效，移除后 fill/drain 拒绝执行。这些路径未发现此前瓶架那种每次查询新建能力的问题。PortableFluidContainerItem 的气体/耐温过滤和可调容量仍会在地牢填充时生效，因此 GTDungeonData.tank 返回 true 只证明放置，不证明请求数量全被接收。仓库气瓶名称、8000 容量与枚举对应；各生成流体的运行时注册温度和填充成功量尚未实测。

ZPM 模块的物品电量位于 BlockEntityTag，正常 BlockItem 放置可走引擎数据恢复，不能因 ZpmModuleBlock 未覆盖 setPlacedBy 就判为丢失电量。放电器库存、缓冲和停止状态有保存及带数据掉落路径，库存能力也有移除防护；本轮没有新跑这些回归，也没有完成与原版电池箱全部时序的逐项比对。

### 59. 弹药配方把可消耗空弹壳误作压模，拆弹额外产出压模

PressAmmunitionRecipes.press 使用 press_bullet_casing_shape_* 作为第二输入；GTTechnological.registerAll 将所有带 _shape_ 的物品设为 TechItem catalyst，Recipe.isCatalystInput 和 RecipeInputs.consume 会保留它。unbox 却将同一个压模作为额外输出。按这两条已注册配方的规则，压制时压模不减，拆弹时再产出一个压模，物品收支错误；这不是原版空弹壳回收。

原版 Loader_Recipes_Handlers:254–259 的额外输入是 OP.bulletGt*.mat(MT.Empty,1)，:454–456 返回相同空材料子弹形态。RecipeMapHandlerPrefix:207–208 原样保留该输入的数量 1，它是装填所消耗的空弹壳，不是 IL.Shape_Press_BulletCasing* 模具。当前 GTTechnological 别名注释也将两者混为一谈，修复应分开空弹壳和压模身份，而非简单把全局压模改为消耗品。AmmunitionRecipesTests 只查配方存在及拆弹输出是压模，没有执行装填→拆解的库存收支测试，且允许退化为只出粉。循环的注册规则已由源码确认，尚未在游戏机器中运行。

### 60. 高压釜结晶流体用量遗漏材料等级倍率

AutoclaveRecipes 逐行直接使用固定蒸汽和蒸馏水数量。原版当前 Loader_Recipes_Handlers:436–448 的各结晶行传入 mFlatFluidCosts=false；RecipeMapHandlerPrefix.addRecipeForMaterial 最后对输入、输出流体均调用 FL.mul(...,aMaterial.mToolQuality+1)。因此工具等级非 0 的可结晶材料，在移植版中蒸汽消耗和蒸馏水产出均偏少。粉/小粉、宝石级别、数量和电路选择器的基本行结构对应，但不能由基本数值相同认定完整对齐。AutoclaveRecipes 的原版行号与“缺少 COATED 条件”说明也应按现有权威源重新核准。

已撤回“输入罐初始 8000 导致结晶配方无法启动”的初步判断：BasicMachineBlockEntity 构造输入罐后调用 setAdjustableCapacity(recipeMap.mMinInputTankSizes,1)，RecipeMap 在注册时按流体记录所需最大量，FluidTankGT 非空后可扩容。首次向空罐填充仍按基础容量截取，后续可继续填充，不能宣称永久只能装 8000。输出罐不采用同一扩容配置；恢复原版倍率后还需覆盖大流体输出的加工与排出，不应直接假设两边都能自动扩容。

### 聚合物成型核查范围

PolymerFormingRecipes 的粉/锭输入、每单位 64 ticks / 16 GU、正常与低温挤压模均已针对性阅读，保留模具在此处符合挤压用途，不能与弹药空弹壳问题一并改成消耗模具。大齿轮使用 4 单位输入；普通杆、长杆、板等按对应数量输出。当前注册集中在 Rubber/Plastic，不证明已覆盖原版所有 EXTRUDER_SIMPLE 材料或所有可锻造形态。本轮未新跑机器测试。

### 61. 饮料返还空瓶忽略背包插入失败

BottleItem.use 与 FluidItem.use 在扣除饮料后直接 player.getInventory().add(emptyBottle)，忽略返回值，也不掉落未插入的瓶子。背包无空位、无可合并空瓶且饮料栈喝后仍非空时，返还瓶子会丢失；不能仅以喝掉最后一瓶通常腾出格子来排除问题。GTFoodItems.deliverContainer 已提供插不进则 player.drop 的实现，但这两个喝饮料入口未复用。原版 FoodStat.onEaten:146–150 经 ST.give 返还容器。应覆盖满背包堆叠饮料、单瓶、副手和创造模式，现有直接 use 测试不足以证明这些分支。

### 62. 非死亡玩家克隆没有复制食物状态，测试预先填充了目标对象

PlayerFoodStats 存在 Forge persistent data 的顶层 gt.props.food；FoodStatEvents.onClone 只处理死亡清空，注释假定其余情况引擎会自动保留。本机映射 Forge ServerPlayer.restoreFrom:1154–1159 实际只自动复制 PERSISTED_NBT_TAG 子树，再触发 Clone，并不复制整个顶层数据。因此新角色的非死亡 Clone 不会从旧角色获得这五个统计值；普通下界传送复用实体的情况不能等同于末地返回等真正角色克隆。

FoodStatsTests:278–282 先对 kept 新对象写入 30 酒精，再调用非死亡 Clone 并断言仍为 30，实际只证明空处理器没有清掉目标值，未验证原角色的值复制。应令原角色有数值、新角色为空，再检查事件结果及双方后续修改独立。PlayerRadiation.clonePlayer 已显式从原角色复制剂量，可作为行为对照。本轮核查引擎路径，未运行末地返回测试。

### 63. 展示流体的饮用逻辑忽略其绑定数据

FluidItem.getName、tooltip、isFoil 通过 FluidDisplayBinding.resolve 使用物品 NBT 中的实际流体；use 却固定从该物品类的 fluidEntry 查饮料，并写死 250 L。FluidDisplayBinding.display 对外部流体可能复用第一个展示物品，且绑定数据保留流体类型、数量、NBT，这与 use 的解释不一致。带不同绑定数据的同类物品可显示一种流体而按另一种流体饮用，或显示可饮用流体却不能喝。应明确展示物品是否允许作为真实饮料消耗；若允许，必须和绑定类型/数量保持一致，不能只修显示。当前只确认代码路径矛盾，未声称普通无 payload 的 Beer 瓶全部受影响。

### 食物表核查边界

GTFoodStats 当前按 Item 身份索引六项数值，未实现原版按 NBT 与物品字典扩展的动态匹配。GTBottles 三列生成表到 ID/流体表达式的读取、GTDrinks 流体解析及 PlayerFoodStats 的 7 位限制、50 tick 施加效果/100 tick 衰减已针对性阅读；生成表的逐条原版数值校对仍未完成。GTFoodItems 的一般食物容器返还有掉落兜底，不应与第 61 项一起判为丢容器。饮料 use 当前立即结算，原版完成使用后的饮用时长和特殊效果还需要另行逐项验证。

### 64. 三条食品发酵配方错误减半，后加载的正确生成表不能自动纠正

FermenterFoodRecipes 根据 duration==128 选 10 L，否则一律 25 L。原版 Loader_Recipes_Food:605–607 的 Milk、MilkGrC→Milk_Spoiled 及 Honeydew→ShortMead 实为 50 L 输入、50 L 输出；它们被该简化规则错误减半。静态对照结果见 weekly-audit-fermentation-2026-09-19.json。

GTFoodGen:92–94 已有正确 50→50 行，但 GregTech.commonSetup 先运行手写 FermenterFoodRecipes，再于后面执行 GTGeneratedChem.loadAll；GTGeneratedChem.register 使用 map.addRecipe(candidate) 的碰撞检查，正确的后置同行不能当作自动覆盖修复。BarrelFermentationTests 的产量断言主要验证苹果汁 50→25，牛奶/蜜露链只检查存在，漏掉不同产率。实际生效表仍应在机器和封桶两条路径复测，避免只修一个配方源而留下顺序依赖。

### 65. 蛋黄酱错误要求电路，柠檬/青柠分支只支持种子油

原版 MultiItemFood:507–511 对每一种 COOKING_OIL 都注册：蛋黄 +100 L 油 +100 L 醋/柠檬汁/青柠汁→250 L 蛋黄酱，仅一个物品输入。FoodItemRecipes.mayonnaise 无条件加入 selectorTag(0)，制造了原版没有的加工前置；mixing 的醋组合遍历所有油，但柠檬/青柠两行只用 Oil_Seed，缩窄了原版组合。应恢复实际原版输入，不能用“流体配方都要电路”的通用注释推导这里也要电路，因为这条配方本来已有蛋黄物品输入。

### 66. 工作台装瓶仍依赖展示物品，不接受真实流体容器

Loader_BottleFillingRecipes 将原版 container1000* 字典容器替换为 GTFluidItems.forFluid 的展示物品，并用 Ingredient.of(container) 匹配。没有检查流体能力、绑定 payload 类型/数量，也没有提取真实容器后的返还逻辑，因此原版牛奶桶或同类 1000 L 容器无法通过这条配方装瓶；带不同展示数据的同一物品却仍会匹配。该缺口是工作台装瓶入口，不代表所有机器灌装功能都不存在。其 ServerStarted 注入导致 /reload 丢配方已归入第 8 项，不重复计为另一个新问题。修复需同时处理真实容器匹配、数量及空壳返还，而非仅加几个物品 ID。

食品配方表还明确跳过部分材料单位冰淇淋、三输入混合冰淇淋及跨模组食物；这些不能计为已经完整移植。当前针对性检查了烹饪、切片、混合、包装的注册与辅助函数，未宣称逐一执行了全部食品制作路线。

### 67. 撬开战利品木箱抽整张箱子表，原版只抽一个条目

原版 MultiTileEntityLootCrate.onToolClick 调用 ST.generateOneVanillaLoot，后者 ST:1022–1023 返回 ChestGenHooks.getOneItem 的一个 ItemStack。当前 LootCrateBlock.rollVanillaLoot 调用现代 LootTable.getRandomItems(params) 并返回全部结果，这是执行整张箱子表的全部池/抽数，不是从旧版类别中抽一个条目。因此一次撬箱可能得到一整箱的战利品，另加空木箱。LootCrateTests 只断言不空、实体数至少两个，未限制或核验原版抽数；固定 seed 也只控制类别选择，未传给表内部抽取，不是完整固定种子测试。本轮源码确认抽取接口差异，未测游戏内产量分布。

### 68. 自然箱子注入和 GT 战利品箱的概率池均偏离原版

原版 Loader_Loot.addLoot 将条目以权重加入 ChestGenHooks 同一类别，与原有条目竞争。LootTableInjection 则保留现代原版池并额外加入 rolls=1 的纯 GT 池：有条目的目标箱固定再抽一次 GT 内容，而不是沿用混合权重。这是明确的平衡取舍，不能以条目数对齐宣称抽取行为一致。

反方向上，GTLootChests 的 dungeon/mineshaft 等使用 van.* 键，但 GTLootTables 对这些键只加载 GTLootGen 中 GT 添加的条目，并不合并原版箱子内容；未声明抽数时又默认 8–24 次。因此这些 GT 战利品箱也不是完整原版类别。gt.matdicts 实际已注册，其注册类顶部“未提供”注释过时。GTLootTables 的逐行加权抽取、复制 ItemStack、数量夹到堆叠上限已阅读，尚未完成所有生成条目数值及分布校验。

### 69. GT 战利品箱挖下重放会重新生成战利品和经验

MetalChestBlockEntity.generateLootIfNeeded 用 lootGenerated 防重，并将 GTLootGenerated 写入实体存档，这能防止同一方块重复打开/区块重载。但 LootChestBlock 继承 MetalChestBlock，没有覆盖带数据 getDrops；MetalChestBlock.onRemove 只散落库存。BlockLootPack 给这种方块生成普通自身物品掉落，无 copy_nbt；MetalChestBlockItem 也只改客户端渲染。掉落物因此不携带“已生成”标记，重新放置后的新实体默认为 false，仍按该 loot_chest_* 方块固有表生成新库存及经验。

原版生成成功后清空 mDungeonLootName，写物品数据时只保留尚未生成的表名，不会把已经打开的普通箱子重新变成未开宝箱。应给开过的箱子掉落普通对应材质箱，或明确保存已生成状态，并验证打开→取空→挖下→放置→打开不能再产物；不能只测同一实体二次打开。本轮没有游戏内执行该循环，结论来自掉落、注册、实体保存调用链。该风险针对能取得的 loot_chest_* 物品，不等于所有自然原版箱子都可重复抽取。

### 70. 树叶掉落遗漏食物及专属木棍，椰树树苗概率减半

原版 BlockTreeLeavesAB.getDrops:108–126 在树苗抽取失败后，可掉柳木/蓝桃花心木/榛木对应材料 stick；还独立抽取榛子和椰子，允许与树苗同次掉落。椰树树苗判断为 nextInt(chance)<2，其他树种为 <1。WoodLeavesBlock.getDrops 统一 ==0 掉树苗，随后只有 Willow/BlueMahoe 掉 Items.STICK；Hazel 木棍、两种食物均缺失，且提前 return 不能表达独立食物掉落。fortune=0 时椰树树苗从原版 2/50 降为 1/50。修复应按实际树种材料和独立随机分支恢复，不能只给所有树叶统一增加食物。

### 71. 树叶改了凋落检查，却仍被原版随机刻条件限制

WoodLeavesBlock 覆盖 randomTick 按同种原木范围检查，但未覆盖 LeavesBlock.isRandomlyTicking。本机 1.20.1 源码该方法只允许 DISTANCE==7 且非 PERSISTENT 的状态随机刻。当前 GT 原木已加 #minecraft:logs，原版距离传播仍会把靠近任意树种原木的叶子设为 1–6。因此同种树干移除、旁边有其他原木维持距离时，新的 GT6 同种扫描不会自动运行。LeafDecayTests 直接 state.randomTick 调用绕过调度条件，不能证明这一边界正确。

原版 BlockBaseLeaves.updateTick2 根据自身树种 mLogs/mLogMetas 扫描。移植的扫描范围与这个入口相近，但还需保留引擎更新触发、持久树叶保护和跨区块读取边界；应增加真实 tick 条件与异种邻木测试，不只反复调用 randomTick。当前未进世界复测。

### 木材注册与手钻边界

WoodLogBlock.use 允许水平轴的枫木/彩虹木原木也钻成竖直树洞，而原版 BlockTreeLogA 的枫木条件要求完整 meta==1，即未经旋转的对应原木；这是需要明确恢复或记录的差异。RegisteredWoodSurvivalRecipes 为所有树种统一返回普通 STICK 和普通 Wood/Bark 副产物，未沿用工作台生成表的各树种 rod 材料，不能由配方行数证明材料组成一致。

关联旧注册 GTWoods 存在 sp.fireproof()?base.ignitedByLava():base 的反向标志。Forge 当前熔岩引火还检查 isFlammable，故不能仅据此断言彩虹木必然燃烧；应共同核验燃烧注册、扩散和普通木种可燃性。WoodSpecies 的颜色字段注明未用于渲染，不能据其数值存在断言游戏染色已生效。本轮仅将这些列为明确代码差异/验证边界，不当作已实测着火故障。

### 72. 五种材质绳索没有攀爬能力，向下延长还忽略放置结果

GTToolBlocks 注册 rope_silk/grass/vine/plastic/steel，与 rope 共用 RopeBlock，但 minecraft:climbable 标签仅列 gregtech:rope，RopeBlock 也没有覆写 Forge isLadder。当前这五个变体只有绳索外形与延长行为，不能由原有 rope 可攀爬推断全部变体可用。RopeBlock.use 扫描到底端后直接 setBlockAndUpdate，不检查目标位置的玩家放置权限或返回值；例如从最低建造高度那节向下延伸，目标在边界外且上方绳索使 canSurvive 成功时，实际 setBlock 失败仍会消耗物品。需要补变体标签、真实攀爬和最低高度不扣物品的测试。

### 73. 炸药的质量被误当爆炸半径，丢失原版采矿爆破及引信行为

原版 MultiTileEntityDynamite 的 NBT_QUALITY 写入 mMaxExplosionResistance，普通 10/强化 40 是能破坏的方块抗性上限。DynamiteExplosion.doExplosionA 只扫描中心周围 3×3×3，排除刷怪笼与基岩，按该抗性筛选；doExplosionB 使用概率 1 和 mFortune 掉落。移植版 DynamiteBlock 将质量解释成半径四倍，调用普通 TNT 爆炸 power=2.5/10，破坏范围、筛选及采矿掉落均不等价，类注释的“原版四倍半径”不成立。

原版手动点火计时 100 ticks，遥控/红石启动 20 ticks，可用灭火工具取消；当前立即爆炸，没有这段处理。use 对 FIRE_CHARGE 调 hurtAndBreak 而非 shrink，火焰弹不可损坏，生存使用不会消耗；这与正确处理打火石耐久是两种不同操作。后续须以原版采矿爆破实现重做，不能只调小 TNT 半径。尚未执行真实爆破测试。

### 74. 加速轨调用 Forge 默认构造器，被识别为激活铁轨

TrackBlock.Booster extends PoweredRailBlock，构造器 super(properties)。本机 Forge 47.4.20 映射源码 PoweredRailBlock(Properties) 委托 this(properties,false)，双参数构造器令 isActivator=!isPoweredRail，所以当前 Booster.isActivatorRail()==true。AbstractMinecart.tick:318–319 对这种铁轨调用 activateMinecart，意味着它除了手写加速逻辑还会触发激活行为（例如乘客弹出、TNT 矿车点火等，取决于矿车类型）。同时不与真正动力铁轨按相同类型传播信号。

不能直接改构造参数后就结束：改成动力轨会进入 AbstractMinecart.moveAlongTrack 的原版动力轨加减速，再叠加当前 onMinecartPass 的 GT6 加减速；应统一处理，避免双重效果。TrackTests 当前直接调用 onMinecartPass 测速度，未覆盖矿车整 tick 的激活分支。先前全局矿车限速 mixin 的兼容性问题仍见第 17 项。本轮以上均为源码调用链核验，未运行轨道试车。

### 75. 没有桶物品的世界流体被空桶取液时先删除源方块

GTWorldFluidBlock.pickupBlock 在 LEVEL==0 时先 setBlock(AIR)，再 new ItemStack(getFluid().getBucket())。Loader_Fluids.properties 未给 ForgeFlowingFluid.Properties 配置 bucket；这些流体因此没有有效桶物品，返回空栈。现代 BucketItem.use:67–70 先调用 pickupBlock，再判断其结果是否为空，不能回滚前面的删除。因此调用该取液分支会删除流体，却不给玩家装满的桶。类注释已承认返回 AIR，但将这种行为解释成正常无桶结果，遗漏世界状态已被修改的问题。

修复应在有合法接收容器时才提交取液；若仍不支持桶，直接拒绝且保留流体。须覆盖海水/河水/沼泽水及其他源方块，验证空桶不丢流体，并确认 GT 便携容器自己的取液路径。本轮调用链由源码确认，未用客户端实际点桶。

### 水体、含水与环境效果核查边界

GTWaterloggable 接受 #minecraft:water 后只存 WATERLOGGED，getFluidState 无条件返回普通水；VanillaWaterlogging 的流入替换也明确将 SimpleWaterloggedBlock 内部变为普通水。这是有文档的适配取舍，会丢失海水/沼泽水种类和对应污染效果，不能称为“完全保留特殊水”；周围原液不会由这个辅助函数直接全变普通水。

BreathingGasEvents 以头部所在格查询流体，BathingEffectEvents 以包围盒覆盖格扫描，不比较液面高度；原版相应回调也按方块接触判定，本轮不把缺少液面高度比较直接定为移植回归。WorldFluidEffects 的呼吸/化学防护分支、沼泽史莱姆免疫、20 tick 窒息节奏已针对性阅读。path(Fluid) 仅保留注册路径并去掉 _flowing，不保留命名空间，其他模组同名流体会套用 GT 效果，属于兼容边界；未提供其他模组实际冲突复现。

FluidRenderLayers 仅将三种世界水的静止/流动流体设为透明层，其他流体沿用默认层；这不证明油和天然气的世界渲染、透明度都与原版一致。该项仍需客户端画面验证，不能由水体属性单元断言代替。

### 76. 书架附魔加成漏除以 12，测试也固定了错误值

原版 MultiTileEntityBookShelf.getEnchantPowerBonus 返回材料魔法属性附加值加 tPoints/12.0F；普通书计 1、附魔书计 2 是分子，不能直接作为最终加成。移植版 GTBookList → BookShelfBlockEntity → BookShelfBlock 原样返回整数总分。普通材料架内 4 本普通书和 4 本附魔书原应贡献 1，当前贡献 12；BookShelfTests 恰好把 12.0F 当作原版正确结果。需要核对材料魔法属性并修改真实附魔台路径的测试，本轮未运行游戏验证。

### 77. 书架点击坐标没有按面旋转，侧面也能存取背面槽位

原版先调用 getFacingCoordsClicked，再只允许正反两面、坐标位于 1/16 到 15/16 的有效区域。BookShelfBlock.use 直接用世界 X 相对坐标作为横向坐标：东西面 X 固定为 0 或 1，点击不同列只会落在边缘列；所有非正面点击都当作背面，顶底及侧面也会存取背面库存。现有点击测试只使用 NORTH，未覆盖旋转后的实际命中坐标。后续需要四种朝向、正反面和边框拒绝测试。

### 78. 书架自动化容量与显示尚未还原

原版 getInventoryStackLimit 与 GUI 限制均为 1；BookShelfBlockEntity 的 ItemStackHandler 未覆写槽位上限，自动化可在一格插入一叠普通书，与玩家每次插入一本的行为不一致。客户端没有书架专用渲染器，当前 bookshelf.json 只是六面木板的 cube_column，实体也没有库存更新包；类注释所谓模型显示已存书籍不成立，不能把缺少逐物品贴图区别当作唯一渲染差异。已核查破坏时遍历库存掉落路径；尚未执行自动化存取和客户端显示测试。

### 79. 熊蜂基因的子代下限与默认温度偏离原版

BumbleBeeGenes.MIN_OFFSPRING=0，setter/getter 都允许 0；原版 IItemBumbleBee.Util 的对应操作调用 UT.Code.bindStack，该函数实际范围为 1..64。因此缺失 offspring 字段或写入非正数时，移植版繁殖可只产生公主、没有雄蜂，原版仍至少产生一只雄蜂。正常随机生成是 1..4，此问题不能表述成所有自然蜂都会绝育。BumbleBeeGeneticsTests 只检查了 offspring 上限，未覆盖这个下限。

默认随机基因温度也有确定的 3 K 偏差：BumbleBeeGenes.random 用 273+round(0.8×20)=289 K；原版用 WD.envTemp(plains)，公式 C-3+temperature×20 且 C=273，平原为 286 K。相同随机偏移下上下限均高 3 K。setAggressiveness 的原版写入下限为 1、读出下限为 100；移植版写入即限制到 100，当前通过 getter 的有效值一致，属于原始 NBT 表达差异，不作为额外生产故障。

### 熊蜂种类、突变表与天气测试的核查边界

静态逐行提取原版种类 ID 与组合表后确认：80 个种类 ID 无缺失/多余，20 条有向组合记录与移植版顺序和值完全相同，涉及的种类全部存在。证据见 weekly-audit-bumble-tables-2026-09-19.json。BumbleBreeding 的选雄蜂顺序、1+rng(5)/2 公主数量、同种突变和异种四路选择、主雄蜂槽有多只时消耗 2、扫描状态保留已对照原版主体；这不等于蜂箱环境、产品、攻击及游戏生命周期全通过。

BumbleBeeGenes.activeNow 无论是否在室内都检查雨雷，且没有湿度参数；原版只在 mSky 时查天气、雨还要求湿度>0。但实际 BumbliaryBlockEntity.checkWork 已保留原版这两个条件，并不调用 activeNow，所以不能把这个未接入辅助函数的问题归为现有蜂箱雨天停工故障。BumbleBeeGeneticsTests.activityChecksMatchGt6 只测该辅助函数，且 environmentGenesFollowGt6 中 rainproof||!rainproof 恒真，无法证明防雨规则。后续应直接测试真实机器在室内、干燥生物群系和室外的天气行为。

### 80. 熊蜂身份只按路径后缀判断，接受非熊蜂物品

BumbleBeeType.of 只检查注册路径是否以 _drone/_princess 等结尾，不检查命名空间或是否属于已注册熊蜂；speciesOf 同样忽略命名空间。BumbliaryBlockEntity.isItemValidForSlot 直接依赖 of，因而其他模组的普通 xxx_drone 也能进入雄蜂槽；如果路径恰好与 GT 熊蜂相同，还会被解析成该物种。原版首先要求物品实现 IItemBumbleBee，不能用任意英文后缀替代身份验证。findDroneSlot 对假雄蜂还可优先选中主槽，随后 breed 因 speciesOf 为 null 返回，阻碍备用槽的有效配对。此为源码确认的兼容入口缺陷，未装载具体第三方模组复现。

### 81. 已扫描熊蜂的跳过配方被错误要求至少 10 mB 蜂蜜

GTBumbleBeeRecipes.find 在分辨是否已扫描前统一要求 tank.amount>=10。原版 RecipeMapBumblelyzer.findRecipe 只先检查首罐存在且为蜂蜜/蜜露；未扫描分支的配方消耗 10 mB，已扫描分支无流体输入、1 tick、16 EU/t。因此罐内剩余 1..9 mB 时，原版已扫描熊蜂可跳过，移植版无法获得配方。空罐情形原版的这段动态分支同样不生成配方，不能误修成任何情况下均可无蜜跳过。现有测试仅验证未扫描蜂在 9 mB 时拒绝，已扫描蜂只用 1000 mB 测试，漏了边界。

已核对扫描输出复制整个输入 NBT、纸板消耗 1、扫描 64 ticks/16 EU/t、展示行标记 fake 并在查找时排除。动态配方按次生成没有 GTMaterialDataRecipes 那种按材料缓存带来的基因串用问题；仍未做真实机器处理及跨客户端往返验证。类注释多次把 meta<5 称为“活蜂”，实际包含 DEAD=4，代码也确实支持死蜂扫描；这是文档术语错误，不应删除死蜂配方。

### 82. 工具转换在挖掘事件中提前删块，绕过正常采掘判定并使创造模式产生掉落

ToolBlockConversionEvents.convertBlockDrops 在 BreakEvent 中直接计算掉落、取消事件、level.removeBlock，然后生成转换产物。已核对本机 Forge 映射源码 ServerPlayerGameMode.destroyBlock:235–281：该事件在创造模式分支、canHarvestBlock 判定及 onDestroyedByPlayer 调用之前触发，事件取消后这些流程均不再执行。因此只要有转换配方且耐久检查通过，当前分支就会在创造模式生成转换掉落，也不再经过生存模式的正确工具采收检查或方块自身拒绝玩家破坏的回调。经验、挖掘统计及正常 playerDestroy 路径同样被跳过。后续低优先级保护监听器不能撤回监听器内部已经完成的删块，这也是兼容风险；不据此声称所有领地插件均失效。

原版 GT_Tool_HardHammer 修改 HarvestDropsEvent 的现成掉落，不在 BreakEvent 中接管删块。现代移植须把掉落变换接入成功采掘后的正确位置，并验证生存/创造、工具等级、拒绝破坏及正常经验行为。当前 ToolBlockConversionTests 只直接调用 convert 并测若干产物及配方数量，没有执行上述事件链。本轮源码确认，未开客户端实挖。

### 工具转换的数量契约及蜂巢测试补充

工具转换的 per-drop 分支拿整叠 drop 查询配方，却按 drop.getCount 次完整复制输出；若配方每次消耗超过 1 个输入，这会放大产物且不留余数。原版硬锤查询时明确 ST.amount(1,drop)。当前尚未确认已注册可达配方中是否存在这种多输入数量组合，所以记录为边界缺陷，不能直接宣布现有生存可刷物资。roll 又将每个输出数量截断到 64，未按物品最大堆叠量分拆，同样需真实配方覆盖。

BumbleHiveBlock 的玩家破坏掉内容物、实体拒绝物品 capability 与原版禁自动化的意图一致；本轮未验证真实玩家破坏、爆炸、染色画面或龙破坏/燃烧属性。BumbleHiveTests.hive 存放测试实际还明确断言 offspring=0 时没有雄蜂，进一步证实第 79 项不是仅漏测，而是测试本身固化了错误原版预期。应随下限修复一起纠正。

### 83. 材料附魔已写入辞典，但没有应用到实际工具

GTEnchantmentTable 与 GTMaterialEnchants 当前由材料辞典及其测试读取。GTToolItem.create → GTToolHelper.write 仅写 head/handle、清耐久和隐藏属性提示；工具类也没有补充附魔的更新路径。对生产源码附魔应用入口的搜索未发现把该材料表应用到工具的实现。原版 MultiItemTool.checkEnchantment:599–627 按采矿/武器/远程类别收集材料附魔，并与工具自身附魔合并后写入物品。例如辞典展示的材料时运不能据此视为对应工具已拥有时运。

这是未完成的移植行为，不能仅凭本周新增辞典表认定本周引入了工具回归。EnchantmentDictionaryTests.dictionaryHasEnchantmentPage 只检查文本包含类别名称，不检查工具 NBT 或实际挖掘收益。后续需按原版类别与合并规则实现，并测试实际成品，而不是只修提示文字。

### 84. 打火工具在苦力怕受到攻击伤害后才点燃

原版 Behavior_FlintAndTinder.onLeftClickEntity 在苦力怕分支扣耐久、播放点火声、点燃并返回 true，以拦截正常攻击。移植版 GTToolItem 把 igniteCreeper 放在 hurtEnemy 中。已核对本机 Forge Player.attack：1142 行先 entity.hurt，只有伤害成功分支才在 1210 行调用 itemstack.hurtEnemy。因此现在先造成近战伤害，受伤被拒绝时又不会到点火逻辑，与原版左键交互不同。ItemBehaviorTests 直接调用 igniteCreeper，未经过玩家攻击，无法覆盖此接线错误。需要在现代攻击前的对应钩子实现并测试苦力怕血量不变、点火及耐久；本轮未执行实体攻击实测。

### 工具头、展示目录与耐久核查边界

GTToolHeadRecipe 直接依据匹配材料生成对应头部前缀，未发现该薄封装额外改换材料；收发协议问题仍见第 0 项。ToolAssemblyCatalog 为配方浏览器构造候选列表并只对第一组材料对齐，不代表所有轮换展示组合均可合成，更不代表所有实际工具属性已经验证。GTToolHelper.damageForBlockBreak 使用 state.getDestroySpeed(null,null)，丢失调用者已有的世界与位置；对依赖环境覆写硬度的第三方方块存在兼容风险，尚未确认本项目可达崩溃样例。当前匹配还允许部分无 GT.ToolStats 的非头柄工具通过 matchesTool，而 damageForToolClickReturn 对其直接返回，需把裸工具/展示物品与可用工具的契约统一验证；未确认正常生存路线可取得这种裸工具，不宣布生存无限耐久漏洞。

### 85. 普通机器没有使用原版并行模式，廉价超频标志也未传入

BasicMachineOriginalParams 已保存 parallelDuration，但 BasicMachineSpec 没有该字段，BasicMachineDefinitions 只取 parallel 数量，BasicMachineBlockEntity.parallelScalesDuration 固定 true。直接核对原版 MultiTileEntityBasicMachine:766–773：true 模式保持单份最低功率、总功乘并行数；false 模式对非 TU 将最低功率乘并行数，TU 则保留单份时间。原版还在 730/743 行用输入功率限制非 TU 的 false 模式并行数。移植版 computeParallel 只按输入和输出容量限制，没有对应功率上限。

确定受影响的普通机型包括焙烧炉 T2–T4、Plantalyzer/Bumblelyzer T1–T5、Generifier。这些表项均 parallel>1 且 parallelDuration=false，普通 BasicMachineBlock 的实体工厂没有为它们换成专用实现。以 Generifier 的 TU 并行为例，当前把总工作量乘并行数，丢失原版同一时长批量处理的意义。非 TU 则还改变最低运行功率与超频判定。不能仅把 parallelScalesDuration 改成 false：MachineWorkCost 当前 false 分支不乘总功也不乘最低功率，是 TU 批处理式计算，不足以表达原版非 TU 的功率并行，直接改布尔值会漏收能源。

另外原版焙烧炉注册明确 NBT_CHEAP_OVERCLOCKING=T，移植版普通实体 cheapOverclocking 固定 false，定义与参数表均未传这个标志；低功率配方的超频开销因此不能由已对齐 input/min/max 证明正确。后续应把并行模式和超频属性作为机器定义传入，并分别测非 TU 功率并行、时长并行、TU 同时批处理。

### 机器注册与生成表核查边界

本轮把 generate_machine_params_table.py 的输出重定向到 build/weekly-audit，重建的 248 行参数表与仓库内容完全相同；证据见 weekly-audit-machine-params-2026-09-19.json。这仅证明生成器与当前提取 JSON 一致，不是所有数值已逐条对照原版、更不是实体已正确读取它们。

MachineRegistry 基础机器注册把实体类型延迟到方块注册后构建，类型工厂设置 spec；Loader_MultiTileEntities 的实体注册调用位于机器注册之后，未发现本入口顺序颠倒。BasicMachineDefinitions 末尾经 DefinitionCatalog 校验 ID；注册器重复调用不具备自行去重语义，当前调用链没有证据表明正常启动重复执行，不将此直接定为启动故障。BasicMachineSpec 有 ID、材料、能量种类、并行下限和能量范围验证；仍允许部分不合理输入组合，现有参数未证明触发。上述属于针对性源码核查，未新增客户端或 GameTest 运行。

### 86. 机器界面 95% 就画满进度条，部分界面文字仍未本地化

BasicMachineBlockEntity.getProgressPercent 返回 0..100，但 BasicMachineScreen.renderBg 使用 20*min(progress,95)/95 计算箭头宽度。因此尚在加工的 95..99% 阶段就显示完整箭头；这不是同步百分比单位差异。应使用同一 100% 契约，并验证加工完成/输出等待的显示区别。RecipeMapCategory.getTitle 直接返回 map.mNameLocal 英文 literal；TankTooltips.sendTankInfo 的 Normal、Sealed、Not fermenting、Fermenting into 及部分容量连接文本也直接写英文，中文语言文件无法替换。它们尚不满足要求的完整中文界面，不应以 lang 键存在率判定已完成。

### 机器 GUI、JEI 与流体同步核查结果

对 MachineRecipeMaps 中 85 个直接声明的表按当前 MachineGuiLayout 计算 16×16 槽位矩形，没有发现槽位互相重叠。提取的 79 个 GUI 名称经当前别名规则解析后，资源全部存在且均为 256×256 PNG；结果在 weekly-audit-gui-slot-overlaps-2026-09-19.json。该检查没有渲染纹理，不能证明槽位边框与贴图像素、箭头、字体或拖动物品层级正确。

JEI 使用 GU 符合后续确认的统一单位；真实流体通过 FluidDisplayBinding 展示，并额外登记不可见 FluidStack 供搜索关联，未发现该入口重新用能源类型替代 GU。特殊温度行目前对应的坩埚表需结合实际配方数值确认；不把静态绘制坐标存在当成所有配方均正确。

MachineFluidMenuTests 确实经过 PacketSyncFluids 编解码再 apply，覆盖数量更新、NBT 改变触发、清空、窗口 ID 不符拒收、快照复制及关闭后停发。它通过 RecordingMenu 捕获包，再在服务端构造模拟客户端菜单，因此不覆盖真实网络分发和客户端渲染。本轮阅读并核对这些检查，未重新运行测试。BasicMachineScreen 使用实际流体的 stillTexture 和 tint RGB，alpha 固定 1；是否符合各流体视觉需客户端验收。菜单距离校验不足仍见前述边界记录，不重复计为新问题。

### 87. 通用矿石标签按材料数量推断，铁门、压力板和骨头被归为矿石

MaterialTagPack.data 对 prefix=null、单一材料组成的物品，只要 amount==2U 就加入 forge:ores/<material>，amount==9U 则加入 storage_blocks。这把材料含量误当成物品形态。已核对 VanillaUnificationLoader → VanillaCompositionLoader 的注册顺序和当前 vanilla_compositions.json：铁门、重质测重压力板、轻质测重压力板、骨头分别只有 Iron/Gold/Bone 的 2U 组成，且没有形态前缀，必然进入该分支。生成结果分别污染 forge:ores/iron、forge:ores/gold、forge:ores/bone，并经 add 方法传播到 forge:ores 总标签。铁门的 recoverable=false 也不能阻止归类，因为该分支不读取此标志。

证据条目写入 weekly-audit-material-tag-classification-2026-09-19.json。此处是源码及数据确定的分类错误，未在新游戏中重新加载标签。其他模组凡按这些标签选矿石时可能接受上述物品；没有具体整合包复现，不声称所有配方都可刷物资。修复应使用明确形态/方块身份建立矿石及存储块标签，材料总标签仍可包含铁门，不能因修矿石分类而删掉材料组成。

### 前缀与标签测试的核查边界

PrefixRegistry、BlockPrefixRegistry 的 all/byName 会触发对应前缀初始化；casingSmall→itemCasing 别名仅改同一形态命名，未发现把不同含量形态合并的逻辑。它们没有重复项去重，但当前读取入口没有证据表明重复初始化，不能据此声称注册量翻倍。

MaterialTagTests 主要检查 gregtech 的材料总标签、unit 和 ingot；materialTagHasUnitsAndFluids 虽在标题宣称测试流体，只把 fluid/iron 数量写入报告，并未断言大于零。它也没有检查 forge:ores 中不应出现的物品，所以原先通过不能排除第 87 项。MaterialTagPack 的拼写别名实际复制当时的值集合，而非注释声称的共享嵌套引用；后来外部数据包只添加一种拼写时，另一种不会自动同步。MaterialEquivalence 对部分别名主动双查，但不代表所有第三方标签使用者都得到同样结果。这属于明确兼容边界，尚未进行多模组运行测试。

### 88. 地牢钥匙只有编号与显示，原版空白钥匙绑定/复制行为尚未移植

GTDungeonKeyItem 实现 gt.key 读写、名字和提示，没有 onItemUseFirst/useOn。当前方块侧钥匙接入只找到 DungeonPortalBlock 的编号比对；它拒绝编号 0。原版 Behavior_Key:44–63 则通过 ITileEntityKeyInteractable 处理：已有编号调用锁；空白钥匙遇无编号锁时生成 ID 并绑定；遇已有锁时按 canCloneKey 权限决定是否复制 ID。原版该接口用于带钥匙保险箱，不是把门的激活方法改名即可代替。当前十种钥匙都已注册，不能把这个注册事实算成空白钥匙、锁和复制系统均完成。

这是剩余移植缺口，未证明本周删掉了以前存在的功能。既有传送门行为差异与测试边界仍见前述门户核查。本轮没有改变钥匙、锁或世界存档。

### 世界生成调试棒与远程激活接口核查边界

BehaviorWorldgenDebugger.useOn 固定清理 y=1..249，再按世界范围截断；1.20.1 主世界的负高度及 250 以上区域不会清理，文档所谓保留最底层基岩的传统效果不能覆盖整个现代区块。它仅保留 OreBlock，而原版保留所有 IPrefixBlock，包含材料存储块；源码已经注明这一缩减，但这仍是移植差异，后续可按调试用途决定是否恢复更宽谓词。遍历没有强制加载未载入区块，并复制可变坐标后再 setBlock；这两个保护已核对。原版调试物品也没有权限检查，不将同样缺少权限检查直接归为本周新引入的行为；本轮未调用任何清区块操作。

RemoteActivatable 的返回值确实表示是否保留绑定，不能将炸药返回 false 判为激活失败。接口自身无执行逻辑；当前炸药立即爆炸而非原版计时引信的问题已见第 73 项，不重复计数。GTDungeonKeys.registerAll 有非空保护、byIndex 越界返回 null、实际键 ID 用 long NBT，名字使用现代 JSON Component；这些针对性入口没有发现额外序列化错误，仍不代表地牢布置与全部玩家交互已实测。

### 89. elec 材料工厂错误添加魔法属性，影响流体提示与容器接受条件

原版 MT.elec:96 仅在 dcmp 基础上添加 ELECTROLYSER，表示可电解分离；移植版 MaterialFactories.elec 调用 gem 后无条件 put(MAGICAL)。当前生成定义有 14 个 elec 声明，清单见 weekly-audit-elec-materials-2026-09-19.json，包括磷灰石、氟硅酸等。原版磷灰石声明包含矿物、可燃、脆性等属性，没有因 elec 获得 MAGICAL；氟硅酸原本更是 lqudacidelec 的酸液工厂，移植定义简化为 elec。

MAGICAL 已被 GTFluidType.describeTooltip、FluidItem 以及 PortableFluidContainerItem.accepts 读取，后者将材料 MAGICAL 合入 magic 参数，再交给容器耐性检查。因此这个错误不是纯注释或未使用字段；绑定到这些材料的流体会按错误魔法属性参与对应判断。具体哪些注册流体可达、哪些容器因此拒绝，还需运行态逐项验证，不把 14 个材料声明数当成 14 个已复现流体故障。未找到后续统一移除错误 MAGICAL 的步骤。修复应以原版明确魔法标记为准，不应把可电解材料整体视为魔法材料。

### 材料工厂与形态补丁核查边界

MaterialForms 的按原版数值 ID 优先、规范化名字后备查询已阅读，但 MaterialPrefix 的多项条件把该表与工厂 GENERATE_* / GEM / METAL 等属性做 OR。因此生成表正确并不会撤销早期工厂错误赋予的额外形态；例如把酸液导入 gem 工厂后，不能靠其原版形态表缺少 GEMS 来自动纠正。MaterialFormCorrections 目前主要增加缺失属性与别名，并非重建原版属性全集，不能将补丁已运行等同于形态完全对齐。

MaterialProperty 中 MELTING、UNBURNABLE、ACID 等已具名区分，但枚举存在本身不是各材料赋值及加工路径正确的证据。MaterialFactories 的元素原子字段、合金粉末默认、木材燃烧默认，以及补丁的 Trinium/Adamantium/石墨/石英等路径已做针对性阅读；未完成所有材料逐项原版对照。MaterialForms.count 逐表项计数，ID 与名字索引可能重复代表同一材料，不能直接作唯一材料数量统计；本轮未发现生产功能依赖该统计导致错误。

### 90. 传感器配方生成把部分原版类别原料缩成单一物品

原版 Loader_MultiTileEntities.java:1988 的轻型重量计中心原料是 OD.pressurePlateWood；tools/extract_gt6_sensor_recipes.py 的 KEY_MAP 却固定映射为 item:minecraft:oak_pressure_plate。GTSensorRecipesGen 的 31010 行保留此映射，SensorRecipePack.buildRecipe 经 MachineRecipeIngredients 的 item 分支生成具体物品条件，不能接受云杉等其他木质压力板。原版的 OD.craftingChest、OD.blockGlassColorless 也被缩成 minecraft:chest、minecraft:glass，失去原类别允许的跨模组替代；原版 ANY 材料组的映射还需逐组核对，不能仅靠目标物品存在判为语义等价。

本轮运行生成器 --check 成功：20 行生成表与当前生成规则一致。这证明生成结果没有过期，却不能证明规则保留了原版替代范围。SensorMatrixTests.everySensorHasItsOriginalGt6Recipe 检查物品/标签存在、无新增 fallback、20 个配方实际载入，但没有用不同木种或兼容类别原料构造合成网格。因此既有全绿不能排除此差异。后续应在生成源映射使用合适的类别标签，并增加等价原料与非等价原料的匹配测试；本轮未修改生产配方。

### 传感器测量与配方包补充核查边界

GTSensors 的 20 个注册与生成表一一对应；SensorMeasurements 的克/千克/吨/千吨四档换算及 65535 上限，与原版 Weightometer 各类一致；时钟加 6000 tick、按分钟显示的换算也对齐。负时间使用 floorMod 属于稳健处理，不作为缺陷。SensorAverage 的窗口限制、恢复和求和已阅读；SensorControl 的八模式及原版 MultiTileEntitySensorTE 的对应分支已针对性对照，不将这些纯计算检查等同于接入所有机器的实测。测量目标错误仍见第 37 项。

SensorRecipePack 的 SERVER_DATA 注册、未使用字符键过滤、资源读取路径与关闭时缓存清理已核对；它按数据包参与重载，不应并入第 8 项 ServerStarted 临时配方注入问题。四档重量的箱子测试使用相同 CrucibleItemInput 作为期望来源，只能验证接线与缩放，不能独立证明物品质量表本身正确。TPS 测试验证计算公式与手动采样，不等于验证真实低 TPS 条件下的定时采样。本轮没有启动客户端或重新运行 GameTest。

### 91. 转译配方的 skipped 统计混合缺失内容与配方拒收，不能作为缺口总数

GTGeneratedChem.register 在找不到配方表、原料解析失败、以及 map.addRecipe 返回 null 时均增加同一个 skipped；但 loadAll 日志统一称其为 skipped (missing content)，skippedRecipes 的注释也宣称全是内容不存在。RecipeMap.addRecipe:225–238 明确会因校验失败、输入碰撞等原因返回 null，因此“跳过配方总量”不能等同于“缺失内容导致的配方数量”。MISSING 仅记录解析失败 token，缺失表名和配方拒收不写入这个集合，两种数字也不能互相替代。

GeneratedChemistryTests 的 chem.skipped <= 60 同时约束了内容缺失和上述拒收；missingContentIsReported 最后 assertTrue(true) 只是输出报告，并不校验缺失内容是否合理。后续应区分 missing-map / missing-ingredient / rejected-recipe，碰撞再核查是否有等价替代，才可据此排移植缺口。本轮没有把现有 skipped 全部记为新缺失配方。

### 注册入口与转译解析器补充核查边界

GregTech 的 A/B 阶段先排材料、物品和机器注册，再由 Loader_Submit 提交共享注册表；commonSetup 的 enqueueWork 中执行材料关联与机器配方，GTGeneratedChem.loadAll 放在手工表后。调用路径已核对，不存在“这些类均未调用”的证据。Loader_Tools 确实提交四个工具配方序列化器，但入口正确不能排除第 0 项协议格式错误。Loader_Items 按排序后的材料列表注册并过滤隐藏/重定向材料，命名冲突会加数值 ID 后缀或抛错；并非无保护地覆盖已有物品。

Loader_Fluids 的世界流体方块与 still/flowing 使用延迟引用，原版水/岩浆条目跳过自建注册；world water 的属性分支已阅读。本轮未新增流体外观验证。GTGeneratedChem 对 i/tech 零数量输入保留可见物品再标记催化剂，但 v/cell 分支直接产生零数量堆，会被 resolveItems 提前当空堆拒绝。扫描当前 loaders/c 的 10 个 *Gen.java 未找到 v/cell 零数量字面 token，因此这是待扩展时会触发的解析器缺陷，不声称已导致现有某条配方消失。同一扫描也未发现 block/crate 数量大于 64 的字面 token，不能仅凭其数量裁剪分支宣称当前已有产量丢失。扫描证据见 weekly-audit-generated-token-boundaries-2026-09-19.json。

### 92. 排水覆盖板会将含水台阶等实体方块整体删除

CoverAttachmentBehaviors.drainFluidBlock:214–226 仅检查目标 FluidState 是 source，成功向机器注入 1000 mB 后无条件 setBlock(AIR)。1.20.1 SlabBlock.getFluidState 在 WATERLOGGED=true 时明确返回 Fluids.WATER.getSource(false)，因此位于覆盖板上方或侧面的含水台阶满足这条路径，水被吸收后台阶一起消失，没有方块掉落。相同风险适用于其他返回源水状态的含水方块。原版 CoverDrain 按水/岩浆方块或 IFluidBlock 分支抽取，不是对任意带 FluidState 的固体直接置空。

生产路径 BasicMachineBlockEntity.tickDrainCover 已连接此方法。现有 CoverAttachmentTests 覆盖普通水源、GT 海水、下方重液体不能逆重力吸取等，没有含水实体方块保留断言。后续修复应区分纯流体块与可抽水的含水方块，抽水后保留宿主，并增加台阶及带方块实体的含水容器保留测试。本轮以生产源码和映射版 SlabBlock 源码确认路径，未进游戏复现。

### 93. 工作台覆盖板菜单会因宿主不是工作台而关闭

CoverUtilityBehaviors.clickCraftingCover:224–230 直接 new CraftingMenu，并把机器位置传给 ContainerLevelAccess。映射版 CraftingMenu.stillValid 调用通用 stillValid(access, player, Blocks.CRAFTING_TABLE)，通用方法先检查该坐标的方块是否是工作台，不匹配直接 false；ServerPlayer tick 检测失败会 closeContainer。因此这里不只是注释所称的“保留八格距离检查”，即使玩家贴着机器也不满足方块类型检查。BasicMachineBlock.use:141 确实调用这个入口，属于玩家可达问题。原版 CoverCrafting:50 则明确覆盖 canInteractWith 并返回 true。

CoverBehaviorTests.craftingCoverOpensAVanillaWorkbenchOnARealMachine 仅在 FakePlayer 打开菜单后立刻检查菜单类型和槽位数，随后 succeed；没有调用 stillValid，也没有让真实玩家菜单经历后续 tick。因此测试通过不证明界面能保持开启或完成合成。应使用面向覆盖板宿主的有效性逻辑，再测试打开后持续有效、合成与关闭时返还输入。映射源码证据与第 92 项一起保存于 weekly-audit-cover-host-evidence-2026-09-19.json；本轮未运行客户端。

### 覆盖板核查中排除的误报与剩余边界

CoverUtilityBehaviors 顶部仍称 pressure_value/panel_asphalt 未进入 CoverItems，但当前 portCoverId 已明确接入两者，不能把旧注释再记为未实现。流体过滤方法的 Javadoc 误称 Forge FluidStack.isFluidEqual 忽略 NBT；实际 Forge 方法比较 NBT。不过当前执行体已经改为 filter.getFluid()==candidate.getFluid()，所以这只是未更新注释，不是当前过滤器再次犯了该错误。物流覆盖板缺少派发的结论仍见第 4 项，不受上述两个特例修复影响。

检索覆盖板 tickRetriever 目前只从相邻库存拉入宿主；原版 CoverRetrieverItem 是从管网寻找源库存并送到覆盖板面对的目标库存。源码主动说明了缩减，仍需记作尚未完成的原版网络行为，而非等价实现。通风盖六相位、256000 mB 部分填充和维度空气选择、雨水公式、压力阀及沥青行为做了针对性阅读；不把纯函数/手动 tick 测试算作全部宿主的玩家实测。未证明本周作者归属；时间戳仅用于筛选。

### 94. 覆盖板物品无法经正常右键设置为物品过滤器目标

BasicMachineBlock.use、FluidPipeBlock.use、ItemPipeBlock.use 均先执行覆盖板安装分支；只要 CoverItems.isCover(held) 为 true，无论 attachCover 是否成功都返回 sidedSuccess。已有物品过滤器占用该面时 attachCover 会失败，但后面的 clickFilterCover 永远不会运行，而且该后续分支还显式排除了覆盖板物品。泵、传送带、机器人臂、红石火把、中继器和集成电路等因此无法通过正常右键被选为该过滤器的模板。面板物品还会在更早的 PanelCoverInteraction 分支被拦截。不是物品消耗错误：安装失败不会扣除物品；问题是过滤器配置不可达。

原版 CoverFilterItem.onCoverClickedRight:89–109 从玩家手持物品建立过滤模板，不将可安装覆盖板这一角色作为过滤排除条件。当前 CoverBehaviorTests/PipeCoverTests 中相关设置直接调用 setItemFilter 或 clickFilterCover，常用苹果和流体桶，绕过上述方块入口。后续应明确“点击已有过滤器”和“向空面安装”的优先级，通过 Block.use 测试普通物品、泵和火把设置模板，确认不替换覆盖板、不消耗模板。

### 95. 物品过滤模板保留完整 NBT，偏离原版忽略 NBT 的匹配规则

CoverUtilityBehaviors.setItemFilter 保存 held.copyWithCount(1)，保留了自定义名字、附魔等 NBT；FilterRules.itemMatches 在模板带标签时要求 isSameItemSameTags。于是拿重命名苹果设定模板后，普通苹果不会通过，黑名单则可能放过普通苹果。原版 CoverFilterItem 保存 ST.make(item,1,meta) 去除 NBT，匹配使用 ST.equal(...,T) 忽略 NBT。这是具体可区分的现有规则差异，并不是为了补 1.20.1 的物品 metadata 而必需比较所有标签。

CoverUtilityBehaviors 的长注释说“原版保存模板会剥离 NBT，所以等价”，却没有在本实现保存时做同样处理；再次点击已有模板直接返回 false，也没有原版同物品再次点击时的 wildcard 切换。对移植版用 NBT 承载真实品种身份的工具或容器，应定义身份字段，不能盲目删除所有标签；普通名字/附魔等不应自动成为原版无此功能的精确过滤条件。本轮未改共用 FilterRules，避免连带改变物流方块已声明的过滤语义。

### 机器和管道方块入口核查边界

BasicMachineBlock 的普通 GUI 与初始流体同步、BlockContents 掉落和带 NBT 的机器掉落路径已阅读；机器掉落去除 gt.inventory 和六面 cover 键，库存/覆盖板由 onRemove 单独掉落，pending_outputs 与流体仍保留在机器物品数据中，未仅凭保留加工状态判为重复产物。此处尚缺真实破坏/重放的整链测试，不声明已证实所有场景守恒。

两种管道 onRemove 调用对应库存/流体处理及 dropCovers，管道动态形状、含水属性和邻块变化刷新入口已核对；GTFluidPipes / GTItemPipes 按尺寸设置物品堆叠上限。可采工具与实际挖掘速度有独立分支，当前阅读不能代替玩家带耐久/能源工具的完整测试。流体管道默认爆炸抗性固定为 6，物品提示另用 MachineSpec 的常数，与物品管道从 spec 读取的方式不同，暂作为需对原版各材料核验的参数边界，不无证据宣称全部材质数值错误。本轮未修改生产代码。

### 96. 小矿石抽取次数公式误加括号，基础产量被额外减半

原版 Drops_SmallOre 的普通池抽取次数是 max(1, multiplier + fortuneRoll / 2)；SmallOreDrops.drops 写成 max(1, (multiplier + fortuneRoll) / 2)。无时运、倍率为 4 时，原版抽 4 次，移植版只抽 2 次；倍率为 2 时从 2 次变成 1 次。片麻岩/硅化木特殊分支也有同样括号变化，原版仅将时运随机项除二，再加基础倍率和 nextInt(2)。这不是时运随机波动，而是公式不同。当前 MaterialOreProcessing 明确给红石、Nikolite 等设置倍率 4；具体游戏产物还取决于粉碎目标和可用形态，未把纯公式验证说成游戏复现。

MaterialCompatibilityTests.smallOreMiningDropsProductsRatherThanItself 只断言钻石小矿不掉自身、同坐标两次调用结果一致；错误公式同样是确定性的，所以无法检出。后续用独立原版公式核对倍率 1/2/4 与多种时运，包含特殊分支。纯算术例子见 weekly-audit-small-ore-arithmetic-2026-09-19.json。

### 97. 小矿围岩材料通过字符串猜名，部分岩层粉末副产物丢失

OreBlock.getDrops 把 stone_granite_black 去前缀并仅首字母大写，查询 Granite_black；真实材料是 GraniteBlack。同理红色花岗岩及两种海晶石查成 Granite_red、Prismarine_light、Prismarine_dark，分别应关联 GraniteRed、Prismarine、PrismarineDark。GTMaterial.sanitize 不移除下划线，也不自动调整大小写；当前源码未找到这些错误拼写的别名。GTMaterialRegistry.get 对未知名返回 Invalid，SmallOreDrops 的 host.isValid 条件随后跳过围岩粉末。StoneType 已持有正确材料对象，应直接映射而非重新推断名称。

本轮未检查所有宿主材料对应关系，已确认上述四个静态映射错误；小矿主产物仍可能正常，不能描述为整个矿不掉东西。已保存映射例子；现有小矿测试使用默认石头背景，没有覆盖这些岩层。

### 98. 地面小石头失去支撑后不会按原版掉落

RockBlock 只覆盖 canSurvive，没有 updateShape、neighborChanged 或 tick 处理；基类 BlockBehaviour.updateShape 返回原状态，neighborChanged 只发调试更新，不会自动调用 canSurvive 后移除。因此移除下方支撑后石头仍可悬空。原版 placeables.MultiTileEntityRock.onNeighborBlockChange:150–160 明确检查底面支撑，失败即掉落并置空，同时处理侧面/上方液体。当前移植的右键拾取和 getDrops 共用 yields，已阅读，但不能替代邻块变化路径。

RockAndDeepOceanTests 的相关测试验证生成落点/材料分布，没有在放置后移除支撑；应增加实际邻块更新、掉落一次且不丢材料的测试。本轮仅源码核查，未改地形或存档。

### 矿石与流体泉入口补充边界

OreBlock 当前普通矿的非精准采集返回 oreRaw，精准采集携带围岩状态；基岩围岩返回零挖掘进度和空掉落，并提升爆炸抗性。旧 BlockEntityTag 兼容仍在类内，按要求不再视作必须修复的功能；正常围岩 BlockStateTag 与旧实体常量需要先解耦再清理。OreHostStone 枚举是正常世界生成/渲染需要的数据，不应随旧存档迁移一起删除。FluidSpringBlock 的服务端 tick 确实转发给实体，不能再把实体的问题归因于入口没接线；已确认的泉水问题仍见第 15 项。

### 99. 材料字典标题拼入 Component 调试描述，触发无效书籍

GTMaterialDictionary.bookStack:77/84 将 material.getDisplayName() 直接与字符串相加；GTMaterial.getDisplayName 返回 Component，而不是 String。映射版 MutableComponent.toString 调用内容的调试表示，TranslatableContents.toString 返回 translation{key=..., fallback=..., args=...}。该字符串再追加 Material Dictionary 后写入 title 和 literal 显示名，不会被解析为材料名称。

这还不只是名字难看：1.20.1 WrittenBookItem.makeSureTagIsValid 要求 title 长度不超过 32；当前生成标题超过限制。BookViewScreen 的 WrittenBookAccess 检查失败后显示 book.invalid.tag，正常材料页面被替换为错误提示。MaterialDictionaryTests.dictionariesDescribeTheirMaterial 只检查物品非空、pages(material) 返回的源字符串、材料回查及数量，没有调用书籍合法性校验，也没有检查实际屏幕读取的页面。原版材料字典用 aMat.getLocal() 作为标题，没有上述 Component 拼接。后续应同时处理标题 32 字符限制、可翻译显示名与合法页面；单改 getString 仍不能保证长材料名加后缀后合法。本轮以映射版源码确认，不声称进行了客户端验证。

### 100. 材料字典把整类说明强行截成一页，丢失合金等数据

GTMaterialDictionary.alloys 把所有正反向合金条目拼入一个字符串，add:262–264 对超过 255 字符的页面直接 substring(0,255)，没有续页。基于当前 GTAlloyTable 的 Copper 输入条目重建，有 16 条合金说明，页面共 634 字符，只保留 255，丢失 379 字符；forms、properties、enchantments 同样经过该截断路径。测试要求每页短于 256，但不检验是否保存全部条目，因此截掉数据仍通过。

应按条目分页，再检验合金表中全部相关条目均出现在最终书页；不能仅以页长限制作为“说明完整”的证明。静态重建结果见 weekly-audit-books-2026-09-19.json。字典的“有矿石物品就会自然生成矿石”“研磨即可分离元素”等通用措辞也比实际世界生成/加工条件更宽，后续需依据实际可达配方与世界生成表生成说明，不能将物品注册当作自然生成的证据。

### 书籍生成与查询补充核查边界

GTBooksGen 经 tools/transpile_gt6_books.py 重生成到 build/weekly-audit/books 临时目录后，与当前 Java 表完全一致：16 本静态书籍、0 个超长页被生成器丢弃。本轮没有覆盖生产生成文件。GTBooks 的 title/author/pages 映射和书籍物品生成、GTMaterialRecipes 的合金输入/输出反查已阅读；生成表一致仅证明提取规则稳定，不代表每条书中教程已在移植版实现，也不证明所有标题符合现代书籍限制。四本运行时汇总手册未在这张静态表中，源码本身有注明；不把“16 本静态书籍”报告为全部手册已齐。

### 101. 背包满时树洞掉出树脂却不清空，可重复领取

TreeHoleBlock.use 令 harvested = !resin.isEmpty() && player.addItem(resin)。生存玩家背包已满且没有可合并的树脂堆时，addItem 返回 false，代码随后 player.drop(resin,false)，但是 harvested 仍为 false，立刻返回 PASS；后面的 RESIN=false 根本不执行。树脂已经作为物品掉出，树洞仍显示可采集，下次右键再次产出。原版 MultiTileEntityTreeHole.onBlockActivated3:80 先 extractResin 清空状态，再用 ST.give 发放，不因背包容量保留树脂源。

TreeHoleTests.harvestingYieldsResinAndSap 使用空背包 mock player，只测试成功收入库存后清空，没有满背包掉落路径。修复后需要用无空位、无可合并堆的生存玩家连续点击两次，断言只产生一份树脂且树洞变空；应同时覆盖背包有空位的正常情况。本轮源码确认了分支顺序，尚未启动游戏复现。

同一入口也未检查 hit.getDirection()==FACING，任何面都可采集；原版 :75 首先拒绝非洞口面，:77 在玩家手持方块时允许放置操作。当前树脂分支会拦截这类正常放置点击。树液填充直接传 held.copy() 给 capability，没有像原版 ST.amount(1,aStack) 明确拆分单个容器；实际堆叠容器是否拒绝、如何回填需按各 handler 验证，暂不将这点单独定为已复现复制/丢失。

### 树洞、蜂箱和流体附件方块补充核查边界

GTTreeHoles 确实注册橡胶/枫木/彩虹木三类树洞，TreeHoleBlock 的 ticker 转发及破坏掉原木路径已核对。蜂箱方块从顶面打开菜单、生存模式触发 onOpened、破坏前及 onRemove 都调用清空式 dropContents 的接线已阅读；蜂群寿命持久化与攻击缺口仍见第 38–39 项，不重复计数。BumbliaryScreen 根据 menu.advanced 选择两张 GUI，未绘制额外标题；本轮未做像素对齐/客户端渲染验证。

FluidAttachmentBlock 的漏斗入液、龙头出液、喷嘴出气过滤、目标侧 capability、向下方容器倾倒路径已针对性阅读。虽然漏斗声明 ALL 旋转而 FACING 不含 UP，ToolInteractionSpec.allows 同时检查 property.getPossibleValues，因此不能据此误报“朝上旋转会崩溃”。炼药锅水位使用现代三格水位分配，其他流体单次转移上限另有分支；仍需针对实际容器与材料耐性作运行测试。本轮未修改生产代码。

### 木材/石材手工配方及生成器核查补充

Loader_WoodCraftingRecipes 的七字段行解析、单字符键、结果数量、物种解析和无镜像 ToolShapedRecipe 包装已阅读。重新运行 generate_wood_recipes.py 到 build/weekly-audit/GTWoodRecipes.java，并逐文本比较：与当前 GTWoodRecipes 完全一致。生成报告为 9 个物种、68 条可表达手工配方，另 4 条 Cinnamon 棍类配方因材料缺失被跳过；128 个原版语句中仍列出 120 条未由该生成器转换，另有 38 个未映射字典类别。这些是该工具的范围说明，不能直接计为整个模组缺失数量，其他加载器可能覆盖。

注意 tools/generate_wood_recipes.py 的 --check 分支只打印“不写入”后返回，没有读取 OUT_FILE 做内容比较。因此该选项成功只能证明生成过程运行成功，不能证明已有生成表是最新；本轮采用临时生成后比较补足了这个证据。结果保存于 weekly-audit-wood-regeneration-2026-09-19.json。未写生产生成文件。

Loader_StoneCraftingRecipes 的 rocks/cobble/bricks/smooth/苔藓等分支、结果为空时跳过及工具键已针对性核对。原版 BlockStones 的 COBBL 分支确实使用 Blocks.stone_stairs（1.7 的圆石楼梯）和 cobblestone_wall，而不是每种材料的独立楼梯/墙，因此移植输出 Items.COBBLESTONE_STAIRS/WALL 不能仅按材质外观认定错误。加固砖条目明确因 drill 键未接入而跳过；当前有 HAND_DRILL，但原版 CR 的 e=drill、g=handdrill 是不同字典键，不能只看手钻已注册就直接宣布这条跳过是假阳性。需继续核对工具字典实际注册/替代关系后决定补法。

StoneVariantRecipesTests 的制造覆盖统计允许原因含 drill/moss item 的跳过；所以门禁全绿不表示所有石材制作路线齐全。木材和石材加载器都只在 ServerStartedEvent 注入，重载丢配方风险已归入第 8 项，不再重复编号。ToolShapedRecipe 的工具返还与可用性检查已阅读，但不能用这些路径证明网络协议或所有电动工具耗能均正确，相关测试范围仍需单独审查。

### 102. 铁轨配方替换越过原版删除范围，会误删其他模组配方

Loader_TrackRecipes.vanilla 从 RecipeManager.getRecipes 的全类型列表按产物删除：凡 getResultItem 是四种原版铁轨之一就 removeIf，没有限定有序工作台配方，也没有保留无序配方或特殊处理器。于是其他模组/数据包提供的无序铁轨回收、石切或机器配方，只要正常报告对应结果，也会被移除。原版 Loader_Rails:141–156 使用 DEL_OTHER_SHAPED_RECIPES，CR.remout:603–622 遍历的是工作台配方列表，并显式跳过 ShapelessRecipes/ShapelessOreRecipe，另保留不可移除的 GT 配方和特殊处理器。不能把移植版全配方表过滤称为同等行为。

TrackTests.vanillaRailRecipesAreReplaced 遍历所有铁轨结果并要求命名空间是 gregtech，实际上把上述过宽删除也作为通过条件；应新增第三方命名空间的无序配方及非工作台配方，验证替换目标有序配方时它们仍存在。当前未安装第三方模组实测，结论来自完整删除谓词及原版对照。仅 removed != 4 的统计警告既不能阻止误删，也不能识别哪些配方应该保留。重载后恢复/丢失这些修改的问题另见第 8 项。

### 轨道注册与配方参数补充核查边界

GTTrackBlocks 注册 10 种材料各 3 种轨道（普通/加速/检测），有重复调用保护；配方表为 30 条 GT 轨道和 15 条原版替代配方。逐项阅读了材质速度/抗性表及 12 种激活铁轨材料产量；当前登记不能证明加速轨完整行为正确，第 74 项构造器问题仍有效。普通配方 RSR 三行实际消耗 6 个轨条和 3 个处理木棍，加载器注释写“三个轨条”不准确，但执行图案与原版一致，不另报材料消耗错误。图案左右对称，因此 plain ShapedRecipe 默认镜像在这些条目上没有新增不同合成布局。

原版 ANY.Fe/ANY.Steel/ANY.WoodTreated 等类别在加载器中仍收窄成单个具体材料物品；跨材料替代支持需后续统一处理，不因配方可合成就宣称原字典兼容完整。本轮只核对源码，没有试跑矿车或修改生产配方。

### 菜单注册核对补记（静态检查，未进游戏）

`GTMenuTypes` 的筛选器客户端工厂读取布尔值，对应 `FilterBlock` 打开菜单时写入 `prefixMode()`，这条初始化协议一致。普通漏斗菜单客户端按注册槽数创建临时库存，服务端按实体实际槽数创建；`forSlotCount` 对未支持槽数向上取整、超过 54 回退 36，属于后续扩展隐患，不能仅凭回退分支认定现有机器已发生错槽。箱子 54、末影垃圾桶 9、电池槽 4 使用精确注册值。方块实体注册本轮只检查了部分注册项，不据此宣称全表验证通过。

### 103. 生成流体温度和亮度未按 GT6 材料状态计算，影响容器耐热判断

`RegisteredFluids.createMolten:910–912` 将 13 个静态熔融条目统一写为 300 K、亮度 0。其中耐火蜡、石蜡、橡胶材料的 setStats 熔点分别为 2600、400、410 K，注册名为 molten.waxrefractory / molten.waxparaffin / molten.rubber；后续 registerGeneratedMoltenFluids 遇到已有同名条目直接跳过，不能修正温度。原版 Loader_Fluids:204–209 调用 FL.createMolten，而 FL:1077 按熔点（低温材料结合沸点限幅）计算温度并设置亮度 10。当前 GTFluids.createFluidType 直接采用 entry.temperature，PortableFluidContainerItem:60 又用 FluidType.getTemperature 判断耐热，因此错误进入实际容器接受条件；尚未进游戏复现具体容器组合。

自动生成的熔融条目也全部用亮度 0，温度直接取熔点，没有原版低温/未知熔点分支；自动气液条目统一为 300 K，没有 FL.createLiquid:1072 与 createGas:1080 的熔点/沸点/等离子点规则。此处指新生成条目，不泛指已有专用流体（它们有单独声明值）。修复应分离“显式专用参数”和“由材料推导参数”，补耐火蜡的温度、低耐热容器拒绝接收、液气温度边界与熔融亮度测试。现有 FluidVisualTests 主要验证贴图选择、RGB 和资源存在，不验证这些物性。

### 流体注册与材质核对补记

GTFluids 的客户端贴图和 tint 同由 FluidAppearance/FluidVisualPolicy 选择；专用 PNG 优先，分类默认贴图用 rawTint。模型烘焙入口 FluidItemClientModels 会清空外观缓存，因此不能仅凭 ResourceManager 对象可能不变就报资源重载失效。FluidVisualTests 用类路径资源和模拟 exists 谓词，不能证明客户端资源包重载后的最终渲染正确。本轮未启动客户端或重跑 GameTest。

FluidDefinitions.bindMolten 方法确实只查询条目、查询材料并写 debug 日志，没有保存绑定。当前生成条目本身带 materialKey，静态 Brass/Zinc/Plastic/Ender/HSLA 等条目也已有 key，气液另经 RegisteredFluids.bindMaterial 建立旁表；故先记为误导性无效辅助方法，不将每次调用计为实际缺绑定故障。世界海/河/沼泽水的专用元数据查询使用流体注册 ID，避免三者共用原版 WATER_TYPE 时丢失各自身份；其流动和第三方模组混合仍需运行验证。

### 104. 材料形态重量遗漏及建筑杖头误套镐头重量

对 MaterialPrefix.getMaterialWeight 与原版 OP.setMaterialStats 的显式算式作静态比较，发现 9 个数值差异，清单见 weekly-audit-prefix-weight-comparison-2026-09-19.json。其中四种植物形态（berry/blossom/fiber/twig）原版为 1/9 U、wart 为 1/4 U，当前均落入 default 1 U；建筑镐头原版 OP:250 为 26/9 U，当前同样落入 1 U；建筑杖头原版 OP:249 为 1 U，却被当前 switch 与镐头并为 26/9 U。ItemMaterialRegistry.register、MaterialChemistry 以及 CrucibleItemInput.parse 对 MaterialItem 的分支使用该重量，故不能把差异视为纯显示问题。应补各形态重量和实际材料处理收支测试；目前只确认错误常量与生产调用链，未声称已在游戏复现循环刷物品。

其余两项为 dustImpure（原版 10/9 U、当前 1 U）及 bouleGt（原版 4 U、当前 1 U）。原版前者由 DIRTY_DUSTS 条件控制，后者生成条件为 FALSE，移植版另有生成规则与加工链；这两项须结合现有配方及杂质组成评估，不能简单把所有输入材料都乘上原版前缀总量。比较只涵盖能读取到显式 setMaterialStats 的 MaterialPrefix 声明，不覆盖动态副材料或所有 MaterialPrefixes 条目。

### 105. 多方块配方生成器会重新加入三个已删除单方块的表项

隔离输出到 build/weekly-audit/crafting-tables 后重跑两个生成器：BasicMachineCraftingRecipes 的 245 条与当前文件完全一致；MultiblockCraftingRecipes 当前 52 条，生成器产生 55 条。差异为 distillationtower_stainless_steel、cryodistillationtower_stainless_steel、fusionreactor_stainless_steel 三个已被 BasicMachineDefinitions.MULTIBLOCK_ONLY 排除的单方块版本，以及手工解释注释被去掉。证据见 weekly-audit-crafting-table-regeneration-2026-09-19.json，未覆盖生产文件。

MultiblockRecipePack.data 对不存在/AIR 方块直接跳过，因此这不是当前重新生成即可凭空注册三台机器，也没有证据说明当前 52 条已加载配方因此失效。问题在于生成器目标映射未同步，手工清理不具备再生成稳定性，会使表级统计或相应测试重新出现过期条目。后续应在生成器中同步排除，不继续只改生成后的 Java。生成一致性仅验证生成器与表，不证明其 JSON 来源每个配方均准确对应原版，也不证明每条运行时都能加载。

### 106. 罐头仍统一使用占位食物属性，普通食物完整性测试未覆盖

GTMultiItems.registerAll 的 cans 分支全部套 CANNED（nutrition=6、saturationMod=0.6），没有按类型与尺寸读取 GT6 的 FoodStat。原版 MultiItemCans:46–51 的 Unknown 六种尺寸分别为 2/4/6/8/10/12 饥饿值和 0.1/0.2/0.3/0.4/0.5/0.6 饱和度；:53–58 的 Rotten 系列另带饥饿效果及食物统计参数。当前它们只是使用统一 FoodProperties 的 MultiItem，生成的 GTFoodStatsGen/GTFoodItemsGen 中未找到 food_can 条目，FoodStatEvents 对查不到统计的物品直接返回。因此至少尺寸营养值及腐烂罐头行为未还原，不能把注册物品数等同食物行为完成。

FoodItemTests.everyGt6FoodItemIsRegistered 核对的是 MultiItemFood 的 266 行及普通食物统计，不包含 MultiItemCans。后续应独立提取罐头表并测试六种尺寸、腐烂效果与食用入口；本轮仅源码确认，未运行玩家进食测试。

### 通用物品及伤害注册补记

TechItem 的普通提示、食物提示和 ItemBehaviors.tooltip 分派已阅读；GTMultiItems 的 use 在行为未处理时调用 super.use，普通食物仍能启动进食，不能误报为统一 PASS 导致不可食用。瓶子、Radaway、计数器、战利品袋等已按 ID 使用专用类，并非整张表均为纯展示占位。registry/GTFoodItems 的缺口注册使用 new TechItem(id,true,...)，true 实际是 catalyst 标志；但当前 GAP 为空，属于未来补行的潜在错误，不计现有食物不可消耗故障。

GTDamageTypes 的 15 个键及工厂读取 data-driven registry，未发现工厂误用其他键。本轮运行 generate_damage_types.py --check 返回 1：检查器因 en_us.json 键未排序拒绝继续，未修改资源。这不是已经证实伤害 JSON 错误，也不应作为全表通过。伤害减免与创造模式受伤仍需实际行为验证。

### 107. 多方块主控被登记为结构端口，注册覆盖率掩盖材料系列功能缺口

LargeMachineParts.DEFINITIONS 中存在 53 个原版 ID 小于 18000 的命名条目，register 一律创建 MultiblockPortBlock。逐项回查原版注册得到对应原类，证据见 weekly-audit-multiblock-controller-classification-2026-09-19.json。不能把 53 直接称为完全缺失的机器系列数：部分系列另有可运行的通用或分级控制器；但这些命名物品本身确实只是端口，不具备名称所描述的控制器行为。

明确例子：原版 Loader_MultiTileEntities:1195 的 17001 Wood Tank Main Valve 使用 MultiTileEntityTank3x3x3Wood，:1196 的 17002 使用 MultiTileEntityTank3x3x3Metal，:1271 的 17302 使用 MultiTileEntityCrucible。当前对应 wood_tank_main_valve、small_stainless_steel_tank_main_valve、large_stainless_steel_crucible 都由 MultiblockPortBlock 创建 MultiblockPortBlockEntity；后者只在另一个控制器绑定后代理能力，自身没有建结构或加工逻辑。不能拿这些注册项证明木质储罐和各材料大型坩埚已经完成。

误判来源可追到 extract_gt6_multiblock_ids.classify：不在手写 CONTROLLERS 中但出现在 LargeMachineParts 的 ID 就归为 PART，没有以原版实际类验证其职责。MultiblockCoverageTests 对 PART 仅断言能找到非 AIR 方块，对控制器只判断路径存在/前缀匹配；因此 117 项通过只证明名称注册对应，不证明功能还原。MultiblockCraftingTests 的逐格比较也仅验证空/非空及形状，不验证材料身份，更不验证合成出的方块能作为主控工作。

后续应以原版控制器类及每档参数建立职责清单，把真实结构零件与主控变体分开注册。至少给木罐/不锈钢罐/材料坩埚增加“从实际注册方块放置→实体类型→按原版结构成型→能力/加工”的测试，修正覆盖统计后再排剩余批次。

### 108. 通用多方块储罐容量未按原版对应等级实现

MultiblockTankControllerBlockEntity.setSize 将 3 阶固定为 320000 L、5 阶固定为 1024000 L；结构分别只接受 TANK_WALL 和 TANK_WALL_DENSE。原版不锈钢 3×3×3（17002）为 1728000 L、5×5×5（17042）为 8000000 L，木罐另为 432000 L，且材料/致密等级还携带不同耐性参数。因此现有通用 tank_3x3/tank_5x5 不能当作所有主阀材料变体的等价实现，容量需与明确原版注册对应后修正。这是源码参数差异，本轮未做世界放置测试。

### 客户端注册、管道覆盖板与测试覆盖补记

GregTechClient 的物品/方块染色分派已针对性检查：反应堆读铅颜色，便携容器读材质，管线读金属/绝缘颜色，多方块部件读 GTMultiblocks.tintOf；引擎核心另按工作状态染色。物品和放置方块均有注册入口，不能根据之前的白色故障就认定当前全部漏接。MassStorage、砧、处理工具、传感器、金属箱、锅炉、流体/物品管道覆盖板等 BER 均有注册；基本机器逐个实体类型注册 MachineCoverRenderer。九瓶箱仍未见对应 BER（已在前述瓶架问题记录），本轮不重复计项。世界水 still/flowing 六 ID 的渲染层在 clientSetup 注册，菜单 Screen 在 enqueueWork 注册。

PipeCoverRenderer 对无覆盖板管道直接返回；有面板覆盖板先交 PanelCoverRenderer，否则读取物品模型粒子贴图绘制板片。代码明确将板片铺满方块边界，并向外延伸 1/16 格，未按管径/原版压力阀包围盒收缩。这是已存在的视觉简化，不代表与 GT6 完全一致；回归应观察六面、不同管径、相邻管道及面板覆盖板的遮挡、朝向和光照。本轮只检查接线与几何代码，没有客户端截图或渲染结果，不能把注册存在视为视觉验收。

MemoryGrowthTests 检查 EnergyNet 移除/卸载钩子、16 次模拟循环、垃圾条目上限和熔炉镜像报告重复调用长度；没有测堆内存、GPU 资源、创造栏模型加载或长时间真实区块卸载。因此它不能证明先前创造栏内存现象已经消失，也不能排除其他静态缓存保留对象。

CapabilityHandleTests 对部分机器确实比较同面 LazyOptional 身份及 invalidate 后旧句柄失效，但 burningBoxCapabilitiesAreCached 只断言固体燃烧室列表非空；流体燃烧室的另一个测试才实际查询能力。只抽取每类第一台，不覆盖所有变体，更不覆盖 BottleCrate/UsbSwitch 已发现的独立能力问题。MachineOutputTests 则真实断言输出异种不合并、满槽保留 pending、同种先补满和流体不混合；它使用简化 Slots 与直接 flush，不覆盖完整机器开始加工/暂停/破坏/重载流程。本轮没有重跑这些 GameTest。

### 109. 手写蒸馏配方截断原版产物，概率数组不能补回遗漏

Loader_Recipes_Chem.oilDistillation 六行仅输出 Fuel/Diesel/Kerosine/Petrol 四种流体。原版 Loader_Recipes_Chem:352–361 的对应行还有 Propane、Butane、润滑油，以及石蜡/沥青/石油焦小撮粉。例如超重油 25 mB 应额外得到丙烷 5 mB、丁烷 5 mB、润滑油 50 mB，并按 5000/10000 概率获得三种粉。移植调用传入的 new long[]{chance,chance,chance} 属于物品产出概率，但没有传入物品产物；MachineWorkOutputs.roll 仅对 mOutputs 掷概率，流体逐项复制，数组不会创造不存在的副产物。

biomassDistillation 同样遗漏原版 :350–351 的蒸馏水 50 mB，且将甘油写为 130 mB，原版使用材料液态 U50，需要按真实相态单位换算后修复。当前代码允许找不到甘油仍保留缩减后的配方，不能只凭 added 计数证明产物齐全。后续应按原版整条配方核对所有输出、概率、相态及数量，覆盖真实配方查询与加工产物，避免继续以“四种主要燃料都能出”作为还原完成标准。

### 110. 手写空气分离产量错误且遗漏稀有气体与冰粉

Loader_Recipes_Chem 的 Air 200 mB 行输出 N 937 mB、O 328 mB、CO2 60 mB，仅三种气体。原版 :363 为 N.gas(U7)、O.gas(U20)、CO2.gas(U100)，另各 U1000 的 He/Ne/Ar，以及 90% 概率的小撮冰粉；当前 GTChemGen 中生成行已列为 143/50/10/1/1/1 和冰粉，和手写行明显冲突。手写代码先加载，后续生成配方受 RecipeMap 的输入冲突检查影响，不能假定“生成表有正确配方”就会替换旧行。具体最终载入项还需运行态查询；源码中的手写产量与缺产物已经确认。

### 聚变配方参数对照补记

对 Loader_Recipes_Fusion 的 18 条与原版 Loader_Recipes_Other:949–966 逐序比较电路、时长、EU/t 和启动能量算式，18/18 一致，包括氢硼反应的特殊 8469×8192×16 和最终反应的 94956×8192×16。证据见 weekly-audit-fusion-parameters-2026-09-19.json。这只证明这四项静态参数，没有证明所有相态成功解析、配方运行或整座聚变机器完整还原。该加载器 added++ 在 addRecipe 返回之后无条件增加，不能把其日志计数单独当作成功入表证据。本轮未修改生产代码或重跑游戏测试。

### 111. 筛选误读原版替代物参数，高级宝石产量被放大

Loader_Recipes_OreProcessing.gemRouteOutputs 将 legendary/exquisite/flawless 数量写成 8/4/2。原版 Loader_Recipes_Ores:460–461 使用 gemLegendary.mat(aMat, ST.amount(8,tGem), 1) 等调用；OreDictPrefix.mat:460–461 的签名明确为 (material, replacement, stackSize)，即高级形态存在时输出 1 个，只有缺失时才返回 8 个普通宝石作为替代。当前代码把替代物的数量错误应用到高级宝石本身；还在任一级缺失时直接取消整条筛选配方，而原版有普通宝石替代路径。

OrePurificationRoutesTests.siftingYieldsGemsFromPurifiedOre 反而要求前三项数量为 8/4/2，说明这不是测试遗漏一项，而是测试复制了相同误读。修复要同时纠正产量、缺级替代及独立原版预期测试。概率和时长本身不因该发现判错。本轮为源码和方法签名核验，未运行加工。

### 112. 溜槽遗漏第八项副产物，测试也只要求七项

原版 Loader_Recipes_Ores:415–420 建立长度 8 的 tSluiceProducts，配方实际传入主产物加 [0]…[7] 共 9 项输出。当前 processSluiceRoutes 建立长度 7 的数组，仅输出主产物加七项，SLUICE_CHANCES 也只有 8 项。其注释“原版第九个概率未使用”与当前权威原版源码不符。MachineRecipeMaps.Sluice 已允许 9 个输出槽，因此不是槽位容量迫使删减。

OrePurificationRoutesTests 的溜槽测试明确断言 mOutputs.length==8；后续须改为验证原版第八副产物及其概率、正常/小份两种数量。缺少材料副产物时原版仍以主材料回退，不意味着可以删掉这一格。

### 113. 磁选副产物应筛选相反磁性，当前却只排序后全保留

原版 Loader_Recipes_Ores:389–394 按主材料磁性分支：主材料有 MAGNETIC_PASSIVE/ACTIVE 时只保留非磁性副产物，否则只保留磁性副产物。当前 magnetOrder 只接收副产物列表，先添加所有非磁性、再添加所有磁性，没有主材料参数也没有排除同类。因此会为原版不可磁选分离的组合注册副产物，并改变后续五个输出取到的材料。后续应恢复主材料判定及主动/被动磁性映射，再独立测试磁性主材、非磁性主材、无可分离副产物三种情况，不仅检查概率 600 与配方数量。

### 矿物处理加载器补充边界

已阅读 oreRaw、原版/深板岩矿、原矿块入口，以及洗矿、粉碎、回收分支。标准矿与小矿入口明确分离，不能再误报有通用小矿粉碎配方。processRecycling 注释明确只覆盖材料粉/锭的一种简化路径，不能把此方法当作全物品回收全集；其他回收入口需分别核查。矿块批量熔炼 registerSmelting 没有接收原矿块倍率，仍需与具体最终加载配方核对，暂不据源码单入口认定全局结果。上述新问题未修改生产代码。

### 114. 三明治方块仍是普通 Block，没有原版配料与保存行为

GTDecorBlocks.SANDWICH_BLOCK 使用 new Block 注册，物品也是通用 BlockItem，没有配料库存、添加配料交互或专用实体。原版 Loader_MultiTileEntities:2031 注册 MultiTileEntitySandwich；该类 :93–105 将每层配料写入 sandwich.<i>，:119–128 按当前配料构建掉落，:131 起右键调用 addIngredient 并消耗实际配料、返还容器。当前注册存在不能视为此系统已经还原。应作为后续生存内容缺口补齐，而不是仅修外观；需要加料、容器返还、保存重载、拆下再放置的回归。

### 储物、工具、装饰与导线注册补记

GTStorage 的储物主体分别使用 MassStorage/DrawerQuad/Safe/Locker/EnderGarbage 等专用类，材料箱子与大容量储物按 GTStorageMetals 参数注册，金属箱物品使用 MetalChestBlockItem；因此其旧类注释“drawers/lockers/safes to follow”不能作为未注册证据。九瓶箱/USB 交换器的行为问题仍见前述独立审计，不因专用类存在而排除。

GTToolBlocks 中磨石、臼、筛台接 ManualToolBlock，混合碗/榨汁器/浴盆接 ProcessingToolBlock，树液袋、两种蜂箱、流体附件与便携容器也有专用类；砧由 AnvilDefinitions 注册。筛台 tint 明确取 Steel。simple 方法剩余 ShapedToolBlock 项只能证明形状/通用交互接入，不能默认全部工具行为完整。

GTDecorBlocks 的五种 spike_* 仍均为普通 Block，和之前地牢刷怪场审计发现的尖刺功能缺口一致；此处补充注册端证据，不重复计数。GTDungeonBlocks 只注册两个替代传送门，并明确声明与原版地牢原生传送门布置不同，不能视为迷你传送门全集完成。

GTWires 将 WireSpec 传入 ElectricWireBlock，裸线和绝缘线列表合并用于实体注册，堆叠上限依尺寸分别计算。GTBushes 的灌木使用专用 BushBlock；GTBerryBushes 八条四阶段颜色与原版 MultiItemFood:397–429 的 BushesGT.put 对照一致。颜色表正确不证明生长条件、同步或世界生成完整。本轮为静态注册核查，未重新验证破坏掉落和客户端显示。

### 115. 通用花盆缺少原版植物支撑能力，地牢两柱甘蔗不满足存活条件

GTDungeonChunkRoomFarmCrop:241–248 在局部 (5,1,5)、(10,1,10) 放 plant_pot，再放三格甘蔗。房间水源位于 (4,1,4)、(4,1,11)、(11,1,4)、(11,1,11)，与花盆均不水平相邻；其水平邻居为非含水台阶或空气。GTToolBlocks:155/222 的花盆使用 ShapedToolBlock，没有 canSustainPlant 特例，仅被加进 minecraft:dirt 标签。

本机 Forge 47.4.20 映射源码 Block.canSustainPlant 的 BEACH 分支仍要求水平相邻水或霜冰；SugarCaneBlock.canSurvive 的后备判断也要求相邻水合条件。因此泥土标签不能解决此处存活问题。原版 Loader_MultiTileEntities:2228 的 32065 是 MultiTileEntityPlantPot，该类 :60 在顶面直接允许植物支撑，地牢原始坐标沿用了这一能力。当前把仙人掌底座改成沙子也只是绕开部分表现，没有还原通用花盆。

这是源码确认的存活条件不成立，尚未进游戏观察具体掉落时机。修复应补全花盆自身的植物支撑行为，再验证两柱甘蔗、仙人掌、树苗及花卉的邻居更新/随机刻；不能只挪动地牢水源而保留普通玩家使用花盆的缺口。

本批另外检查空房间、鱼塘、传送门房间基类和下界/末地房间的放置路径；已有标签共享问题仍见第 12 项。覆盖标记仅表示定向源码检查，不代表运行态验收。

### 116. 基岩矿井脚手架只生成外形，没有攀爬与连接碰撞行为

GTDungeonChunkRoomMiningBedrock:132–145 沿竖井生成连续 scaffold，边缘和桥面也复用该块。当前 GTToolBlocks 的 scaffold 是 ShapedToolBlock，碰撞固定为 Block.box(0,14,0,16,16,16)，无 isLadder 覆写、上下连接状态或潜行穿越判断；minecraft:climbable 标签仅包含 rope。因此脚手架柱不能提供原版梯子的上下通行能力，且每格顶板会阻挡柱内移动。

原版 MultiTileEntityScaffold:79–85 根据上下相邻和支撑设置 mDesign；:167–186 分别给桥面、竖向柱和顶部构造碰撞，并考虑实体位置与潜行；:207 对 mDesign != 3 返回梯子行为。只补 climbable 标签不足以完整恢复这一结构，必须同时核对碰撞和连接状态。DungeonTests:782–834 的 bedrockMineBuildsScaffoldAndExplosives 只确认至少有房间、炸药、脚手架，未检验玩家攀爬，不能作为通行验收。

本项为源码确认的行为缺失，尚未进行玩家移动实测。后续回归应覆盖连续竖井上下移动、顶部出入、潜行下穿，以及水平桥面站立和支撑拆除。

本轮还核对地牢入口扫描与高度截断、支柱向下填充路径；入口使用 getHeight()-32 作为绝对扫描上界，现代负最低高度下该数值不等于 getMaxBuildHeight()-32，但后续存在建造高度截断，暂不将其单独报成越界崩溃。高度极端地形的实际出口可达性仍需运行态验收。

### 117. 地牢书架忽略战利品表内容，所有非空表都变成普通书

GTDungeonData.shelf:764–780 对 lootTable 只做非 null 判断，随机放入 2–7 本 Items.BOOK，没有查询该表，也没有保存延迟生成所需的表名。GTDungeonChunkRoomLibrary、Barracks、Workshop 的不同战利品参数因此不产生各自的内容分布，显式钥匙仍单独写入，不能说钥匙因此全部丢失。

原版 DungeonData:281–303 将前后战利品名称和显式库存写入 NBT；gregapi/tileentity/inventories/MultiTileEntityBookShelf:104–138 调用 ST.generateLoot，根据 BooksGT.BOOK_REGISTER 保留认可书籍的单件堆栈，其他结果才转为普通书或可选模组随机书，并跳过已占槽位。当前实现缺失的是这条生成链，而不是要求直接把整个箱子战利品表不筛选地倒入书架。后续应验证不同表生成的书籍及 NBT、显式钥匙保护、保存重载后不重复生成；本轮仅源码核实。

本批已定向阅读普通/三岔/四岔通道、房间接口、图书馆及普通图书馆、工坊。通道按连接方向开口，四岔类固定四个开口；工坊使用现有燃烧室、坩埚、模具和流体容器注册入口，不再把它们一概视作原版方块占位。杯子、九瓶箱、ZPM、房间标签等已有问题保留原编号；这些源码核查不替代实际生成、通行与内容保存测试。

### 材料加工侧表的生成一致性核查

已检查 extract_gt6_processing_targets.py、extract_gt6_recyclable_prefixes.py、extract_gt6_workability.py 的提取与输出路径，将输出重定向到 build/weekly-audit/processing-tables，未覆写生产 Java 或原有数据报告。MaterialProcessingTargets、RecyclablePrefixes、MaterialWorkability 三份 Java 与重新生成结果逐文本相同；证据见 weekly-audit-processing-tables-2026-09-19.json。

ProcessingTargets 保存七种处理的字段/目标/数量，由材料字段映射建立查询表，当前主要消费者是材料辞典；表存在不等于机器处理器已经使用这些目标。Workability 的六类标志供砧/研钵、蒸压釜、熔炉和形态转换等入口读取。RecyclablePrefixes 保留原版有回收标志、无副产物且排除矿/粉/容器/管线的筛选结果。这些属于生成规则与调用入口的定向检查，不是每种材料、每条配方的独立原版对照，也未运行新的 GameTest。生成器的按行/括号平衡解析仍应在原版源结构变化时重新核验，不能仅凭一致性结果断言无遗漏。

### 118. 材料形态测试未断言多余形态，机器转换测试未比较堆栈数量

MaterialFormGapTests.portFormsStayWithinTheOriginal:121–135 计算 extra，但最终只断言 checked > 100，extra 完全未参与判断。即使出现额外形态，该测试仍可绿；同类 formGapIsReported 也只写缺失报告并断言 DUSTS 数据加载，不能把它当作“缺失为零”的门禁。修复应先界定允许的移植扩展，再逐项断言非法差异，不能不分情况地添加 extra == 0 掩盖预期差异。

MaterialFormConversionTests:63–79 的 contains/route 使用 isSameItemSameTags 或 MaterialEquivalence.matches；两者都不比较 ItemStack 数量。copperStorageRoutes 虽传入 9 个锭、4 个小粉等预期堆栈，实际只证明输入输出含有对应物品，不能排除 1 锭变块、1 粉变 9 粉等错误比例。此处确认的是测试能力缺口，不据此宣称当前生产配方实际存在上述复制。craftingGridConversions 对部分工作台产出另有数量断言，不能一概说整个测试类没有数量检查。后续应补输入/输出数量、完整附加输入与实际消耗结果的检查。

本批同时检查 MaterialFormParityTests 与 PrefixConditionParityTests：前者确有金属金/金木、黏土等独立样例，后者有 ORES/PARTS/DUSTS 全材料遍历与缺失断言，比单纯检查总注册数更强；但两者仍依赖移植的 MaterialForms 数据，不能独立证明该数据已经完整准确提取原版。本轮未改测试或生产代码，未重跑 GameTest。

### 矿脉数值表与世界生成测试核查

直接解析原版 Loader_Worldgen.java 的启用注册行，按 ORE_OVERWORLD / ORE_END / GEN_OVERWORLD / GEN_NETHER / GEN_END 分组，与 GTOreVeins 对应表按名称核对：大型矿脉 31+5 条，小矿 36+18+32 条，共 122 条。大型矿脉的 minY/maxY/weight/density/size、小矿的 minY/maxY/amount 均一致，没有缺失或额外名称。证据保存为 weekly-audit-vein-numeric-tables-2026-09-19.json。此检查未覆盖材料字段映射、布尔生成条件以及真实区块中的概率分布，不能扩张为全部世界生成已经等价。

检查了 BedrockVeinTests、BlackSandTurfTests、NetherQuartzTests、VeinTableTests。黑沙/泥炭测试实际调用 carveColumn 并检查两格深度、不同底材及树下例外，基岩矿测试构造地层后检查矿核及边界，不只是注册数量。下界石英测试在普通测试世界人工放下界岩后直接调用 placeSeams，能够验证局部替换，但绕过下界维度与生物群系的自然生成入口；expected 高度也复用生产 GTCellNoise，因此不能独立证明噪声与原版逐种子等价。VeinTableTests 对五条新增主世界矿脉检查数值，其他主要检查名称与材料可解析性，本次独立静态对照补充了全部 122 条数值范围。未重跑这些 GameTest，既有生成逻辑问题仍按原编号保留。

### 工具装配与生存配方测试的实际覆盖范围

已检查 ToolAssemblyTests、CraftingCoverageTests、SurvivalStandardizationTests、MortarGrindingTests、CrucibleTemperatureTests。工具装配目录测试会构造网格并调用 matches/assemble，不应误报为只检查注册；不过 catalogEntriesAreCraftable 对每组 forms 只选首项，直接 new 配方对象，未经过真实 RecipeManager 的网络序列化/客户端接收，因此不能排除第 0 项序列化缺陷，也不能证明所有头/柄材质组合、余物返还与工具耐久正确。石头、燧石、黑曜石的早期工具测试另有材质和拒绝错误材料的断言。

CraftingCoverageTests.itemsWithoutAnyRecipeAreReported 只遍历 GTTechnological 和 GTToolItems，并收集所有 RecipeMap 输出，未排除 mFakeRecipe 或验证输入可生存获得；末尾仅断言 checked > 300，不断言缺失为零。现有 items-without-recipes.json 的 checked=363、withoutRecipe=0 是这两组在该统计口径下的历史结果，不能推导所有方块/物品、坩埚温度展示条目或生存闭环全部完成。也不能因为这个报告方法不强制缺失为零就断言它本身设计错误，应在完成声明时明确其范围。

SurvivalStandardizationTests 有原版铁/金/铜统一、NBT保留、传送带别名和标签实际成员检查；树木处理会查询配方并验证物品消耗，但没有逐流体余量断言。MortarGrindingTests 的 routeFor 只确认粉形态和材料，不比较输出数量；gem 分支允许仅测到脆性或非脆性其中一类，报告测试还要求 skipped 非空，后续补齐所有行时应同步调整该断言。CrucibleTemperatureTests 明确把 CrucibleSmelting 当作 mFakeRecipe 展示表，并检查温度；未实际给坩埚加热推进反应。其 Smelter 查找使用流体 ID 子串 iron/wrought，验证精度有限。这些边界已与源码核对，本轮未重跑测试、未修改生产代码。

### 流体危害和世界水体测试的验证边界

定向检查 FluidHazardTests 的分类、耐性、储罐/管道单 tick 和呼吸路径，以及 WorldFluidEffectTests 的身体接触、头部效果、移动减速和计时分支。储罐测试实际调用 fill 并核对接受量，腐蚀测试直接注入不合法流体后调用 serverTick；管道检查 8/16 的单 tick 损失和耐性例外。这些不是纯注册存在性测试，但大多手动触发单次方法，不能覆盖自然持续 tick、邻接网络转移或保存重载后的完整行为。

WorldFluidEffectTests.bathingEffectsReachTheBodyAndRespectTheChemSuit 用创造模式玩家代表 chem-protected 分支，并没有实际穿戴防化服，故不能凭该方法名字认为装备组合与防护耐久已验收。effectsLandEveryTickAndTheDrownDamageEveryTwenty 只按当前 gameTime 选择一个预期分支，没有等待跨越完整 20 tick 周期。heavyOilsActLikeWebAndMediumOilDoesNot 则确实调用 entityInside 与实体 move，对比重油/中油移动距离，覆盖强于只看标志。

WaterFluidParityTests 本批检查 world-water 类型、客户端视觉策略、渲染层、含水方块与水草路径。clientTintMatchesVanillaWater 使用模拟资源存在谓词，renderLayerMatchesVanillaWater 比较字符串注册表；两者未加载客户端图集、着色器或实际像素，不能证明紫黑贴图/染色问题已全部消失。含水测试明确允许原版半砖放入 GT 水时保持干燥，仅 GT 半砖走自己的含水判断，这是已记录的兼容边界。上述均为源码覆盖分析，未新增客户端或 GameTest 运行，不将边界等同于新确认的生产故障。

### 蜂箱、遗传和灌木测试补充核查

本批定向检查 BumbliaryTests 的注册、繁殖倒计时、产蜜、自动化、菜单和保存路径；BumbleBreedingTests 的同种后代、亲本基因、20 种组合、逐种突变与固定种子确定性；以及 BushTests 的生长、收获、种植和生成条件。蜂箱产蜜测试确实逐 tick 推进到 life=600 并检查一份蜂巢产物，遗传测试也核对后代种类、数量和基因，不是只有数量门槛。

BumbliaryTests.machine 直接 new 实体并 setLevel，多数逻辑测试未把该实体安装进真实区块。progressAndBroodSurviveNbt 显式调用 saveWithFullMetadata/load，验证字段序列化但未验证区块 dirty 标记、自动保存与卸载重载，因此无法排除第 38 项倒计时变化未 setChanged 的问题。产蜜使用温湿度极宽、昼夜/室内外皆活跃且防雨防雷的基因，未验证真实花源搜索和自然环境限制，第 39 项依然成立。菜单测试显式切换假玩家的生存/创造模式，确实覆盖了不同取出权限，不能将其混同于直接 makeMockPlayer 的测试。

BushTests 通过循环 grow 验证阶段增量，并走 Block.use 检查收获后回到阶段 0；种植者来自 makeMockPlayer（创造模式），没有证明生存种植消耗一颗浆果，也没有覆盖满背包收获或落地支撑被移除后的处理。世界生成测试检查配置、生物群系谓词和索引确定性，没有实际调用完整生成并验证地形。以上为测试能力说明，未新增生产故障编号，也未重跑这些测试。

### 119. 零件加工加载器漏掉难加工材料的耗时分支

Loader_Recipes_Parts.wiremill 对所有 SMITHABLE 材料固定注册 stick→4 wireFine 为 8 tick、stickLong→8 wireFine 为 16 tick。原版 Loader_Recipes_Handlers:287–295 把 tEasyWorkable 与其反条件分成两组，固定 8/16 只适用于易加工材料；难加工组传 duration=0、multiplier=128。RecipeMapHandlerPrefix:218/226 的实际计算为 ceil(max(inputUnits,outputUnits)/U * 128 * (quality+1))。钢的工具等级为 2（CompoundMaterials:442；GTMaterial.setToolStats 第四参数），短杆 U/2 对应 192 tick，当前加载器为 8 tick。此为注册源参数不等价；最终配方表中是否另有冲突项以及玩家实际执行哪条仍需运行态查询。

该加载器的 RollingMill、RollBender、ClusterMill/RollFormer、Lathe 也使用固定值，未按原版各行的 tEasyWorkable 分支计算，且部分原版 COATED.NOT/LAYERED.NOT 条件未带入。不能只修钢这一条，应按处理器逐行恢复材质条件和成本计算；各行 multiplier 不同，不能套用一个统一常数。当前检查不把 cutter/extruder 的全部参数也一并判错，它们需各自对照。潜在缺模具时退化为无模具配方的 extrude 分支已读，当前尚未证明实际有缺失模具触发，不另计现存漏洞。

本批另核对 GTMainRecipes 的注册与解析路径：空物品或流体会跳过整行并记录原因，fake 标志确实传入 addFakeRecipe；不把所有提示行当作可执行配方。提示文字仍是英文 literal，SKIPPED 中部分“内容未注册”的静态说明需随移植进展重新核实，不能作为永久不移植依据。本轮未修改生产代码、未重跑 GameTest。

### 120. 四种元素蜂巢小撮粉产量放大九倍，黏性蜂巢树脂替代被误删

GTCombGen 的 pyro/cryo/aero/tera 四行分别写 i:dustTiny:Blaze/Blizz/Blitz/Basalz:9。原版 MultiItemFood:267–270 使用 OM.dust(material,U9)；OP.dustTiny 的材料量为 U9，OM.dust:465 以 amount*9/U 计算数量，因此原版为 1 份小撮粉，当前为 9 份。四行的首项概率都是 10000，这不是概率补偿。应改生成源/对应维护源和期望测试，不能只改 JEI 展示。当前 GTCombGen 确实由 GTGeneratedChem.loadAll 调用；最终加载冲突与实际产物仍需运行态回归。

黏性蜂巢注释把 IL.FR_Propolis_Sticky.get(1, IL.IC2_Resin.get(1, IL.Resin.get(1))) 整体判作外部模组产物并删除。但原版 MultiItemFood:104 自己注册 IL.Resin（Rubber Resin），当前 GTMultiItemsGen:480 同样有 rubber_resin。因此即使没有 Forestry/IC2，仍应保留本体树脂 30% 的后备产物。末地蜂巢的 EtFu 紫颂果也被作为外部内容省略，在 1.20.1 可用原版紫颂果，属于可补齐的适配项；Forestry 独有蜂胶不应无依据替换。

CombProcessingTests 逐种只检查配方存在、16 GU/t、64 tick、至少一种产物；精确检查主要限于蜂蜜和蜂王浆的流体数量，没有覆盖上述四种粉数量与树脂后备，故既有绿测试无法排除本项。

### 八份批量配方表的生成一致性

已将 transpile_gt6_chem.py 的八种输出全部重定向到 build/weekly-audit/recipe-tables 后重新生成：Chem 358、Other 265、Potion 500、Food 243、Extruder 322、Vanilla 228、Temporary 34、Ores 138 条，共 2088 条，八份文件均与现有 Java 逐文本一致。证据见 weekly-audit-recipe-table-regeneration-2026-09-19.json。所有表的 load 入口均由 GTGeneratedChem 调用。此结果证明当前表可由当前生成器复现，不证明未翻译调用均可忽略，也不证明 2088 条都成功注册或符合原版；既有解析、冲突与跳过问题仍保留。本轮未修改生产代码、未执行 GameTest。

## 地牢宿舍与定位、传送测试的补充核查

本批检查 GTDungeonChunkBarracks 全部放置路径，并针对 DungeonLocateAndSlabsTests 的定位/格点/半砖断言、DungeonKeysAndPortalsTests 的钥匙交互与往返断言核查覆盖范围；没有运行新 GameTest 或客户端。

原版 DungeonChunkBarracks:111–154 的四个 3010 容器均携带 NBT_KEY，并按两侧墙设置南北朝向；当前 Barracks 调用 GTDungeonData.chest，后者固定放置朝北的原版普通箱子，没有锁。这是第 88 项钥匙系统缺口在自然地牢中的具体影响：钥匙虽在书架中，宿舍箱子不需要钥匙即可开启。原版书架的南北朝向也未传入当前 shelf 帮助方法。类头还有过时说明：声称 BONUS_CHEST 无对应表、钥匙槽未传入，但当前 LOOT 已含 spawn_bonus_chest，且 keySlot 已实际传入并保存；不能用这些注释推断当前功能。杯子、书架战利品等已分别见第 57、117 项，不重复计数。

DungeonLocateAndSlabsTests:481–617 没有调用真实 ServerLevel.findNearestMapStructure 或执行 /locate，而是新建 normal structure state、调用本测试的 locateDungeonAnchor 镜像循环，并由 anchorStart 人工生成/存储结构起点。它能检查相关注册、格点计算和结构起点有效性；不能证明普通世界自然生成流程会保存对应起点，也不能证明返回位置确有完整地牢。格点期望大部分直接来自 GTDungeonLayout，因此与实现一致不等于独立证明 GT6 布局正确。半砖测试则实际建房并核对方块、方向及形状，证据比纯资源存在检查更强；仍没有覆盖玩家走过整段楼梯或所有房间组合。

DungeonKeysAndPortalsTests:512–664 实际调用 Block.use 检查钥匙编号、打火石切换，手动 save/load 检查实体字段；往返测试直接调用 DungeonPortalBlockEntity.teleport，使用盔甲架核对维度、目标坐标和冷却，并测试无对应门时的平台。它不是仅检查注册的空测试，但没有覆盖玩家 entityInside 入口、客户端维度切换和自然保存重载。后续应保留这些定向测试，同时补普通世界 /locate 到实体地牢、玩家步入门往返以及四向宿舍容器的验收。

## 合金字典、石材挤压与胶囊测试覆盖补充

GTAlloyTable 的生成器 extract_alloy_table.py 从移植版 Loader_Recipes_Alloys 的 mix 调用展开循环，并非直接解析 GT6 原版。已仅将输出重定向到 build/weekly-audit/alloy-table 重生成，95 行与当前表完全一致（忽略换行格式），证据见 weekly-audit-alloy-table-2026-09-19.json。这证明生成文件没有漂移，不证明运行时流体解析、碰撞过滤后的配方数量，也不证明原版全集完整。生成器把循环变量全局收集、按参数笛卡尔积展开；当前一致性结论不应扩大为对未来任意嵌套循环/表达式的支持。

AlloyDictionaryTests.alloyTableMatchesLoader 实际只读 GTAlloyTable，检查至少 50 行、四种常见合金的输入名字和材料存在，没有对照加载后的机器配方；hasAlloy 也不核对输入数量或排除额外输入。materialsWithoutAlloysHaveNoPage 中 Silver 的断言是 hasAlloyPage || !alloyedInto.isEmpty，Silver 参与合金时即使页面丢失仍通过，不能支持注释所说的“恰好在参与合金时显示”。Diamond 分支带条件，条件不满足就跳过。字典页测试读原始 pages，不覆盖物品书页长度限制（第 100 项）。

StoneExtrusionTests.route/contains 使用 isSameItemSameTags，不比较数量；主体同时从被测实现 stoneExtrusionShapes/Molds 取得期望，因此主要核验已声明路线被注册以及 16 EU/t、32 ticks。只有 block 自循环单独断言输出一块，不能用主体结果担保所有板/杆/工具头数量。missingForms 只写报告不使测试失败；noRegisteredRecipeIsDegenerate 检查是否有任意输入输出，流体仅看数组长度，并不证明材料守恒。其 RecipeMap.make 自消除与普通配方对照测试有实际价值，应保留而非称其为全部空测试。

CapsuleCasingTests 覆盖比上述存在性检查更强：40 种胶囊的物品流体填充、手动放置/保存/掉落精确相等，拒绝整组填充，空壳回收拒绝至少 1 L 流体，挤压单位与能源、铅机壳等级配方，以及 Geiger 配方网络往返后拒绝满胶囊。placedCapsuleUpdateCarriesCurrentFluid 手动把更新标签加载到 clientCopy，可证明标签载荷是 750 L/空，但没有真实网络投递或客户端渲染；手动 save/load 也不是区块自然卸载保存验证。本轮只读这些测试，没有重新运行，不能把历史门禁等同于当前运行结论。

## 食物、饮品、战利品与附魔生成表复核

本批完成六份生成 Java 表的当前生成器一致性核查。四个食物提取器使用其只读 --check 路径，均 exit 0：GTBottlesGen 为 169 行、跳过 1 项（clouded_bottle 无流体表达式）；GTDrinksGen 为 272 行、73 条跳过说明；GTFoodStatsGen 为原版 Minecraft 20 行加 GT6 181 行，共 201 行，47 条跳过说明、unknown 为 0；GTFoodItemsGen 为 266 行，其中 207 行含 FoodStat、181 行有六项统计、59 行仅展示，9 个空容器、7 个原版别名、1 个隐藏占位、39 条跳过说明。输出记录保存在 weekly-audit-food-table-checks-2026-09-19.json。数量是提取器分类，不可把这些分类相加当成独立物品总数。

GTEnchantmentTable、GTLootGen 仅将生成输出重定向到 build/weekly-audit/food-loot-enchants 后比较，与现有 Java 完全一致（忽略换行格式）。前者 177 种材料、507 条附魔定义；后者 219 条战利品、176 条跳过说明。证据为 weekly-audit-loot-enchant-regeneration-2026-09-19.json，完整战利品跳过记录留在临时输出 loot-rows.json，没有覆盖现有源文件或原进度报告。

接线检索确认食物/饮品表由 GTFoodStats、GTDrinks、GTBottles、GTFoodItems 消费，战利品表由 GTLootTables 消费；附魔表被 GTMaterialEnchants 用于字典展示。因此这些不是全未引用的数据文件，但附魔表不证明工具实际拥有附魔（第 83 项），食物表不证明罐头营养已经正确应用（第 106 项）。饮品跳过记录包含其他模组名称和无可解析统计的基础药水表达式，不能将全部 73 项直接认定为 73 个缺失的本模组饮品。战利品提取器同样存在跳过和运行时 resolveSpec 过滤，219 个生成行不等于游戏实际可用行或 GT6 全集。此次是生成一致性与消费入口复核，没有执行客户端饮用、开箱或工具附魔验收。

## 矿石资源、泡沫与生存配方测试补充

CFoamTests.foamDriesIntoHardenedFoam 手动 tick 恰好 100 次后要求仍为湿泡沫；CFoamBlockEntity.tick 先 timer++，仅在 timer < 100 时返回，所以第 100 次已经执行 nextInt(5900)。在起始状态正常、没有额外 tick 的情况下，该断言就有 1/5900 概率失败。这是测试边界不一致，不能据此改坏原版硬化时机。后续所谓 200000 次“随机等待”实际反复调用无随机数的 dry()，通常第一次即硬化，不覆盖随机硬化入口。建议测试拆成前 99 tick 保持湿润、受控随机数触发第 100 tick，以及单独 dry() 幂等性。本轮没有运行随机复现；结论来自准确的循环次数和条件分支。

OreAssetCoverageTests 遍历实际 OreBlock 注册，比仅按生成器输入验收更强，但只检查三个 JSON 存在、默认 blockstate 引用以及 item model 的直接 parent。它没有递归解析块模型 parent/贴图、PNG 解码、围岩状态动态模型、染色或真实模型烘焙；不能作为无紫黑纹理的完整证明。当前矿石图像与客户端模型问题仍按既有渲染审计处理。

ElectronicsTests 对 84 条组件配方实际匹配/assemble、工具损耗/破损拒绝，以及 173 条电子配方的 RecipeInputs.consume 和催化剂保留进行了检查；另有焊料限制电路等级与透镜并行竞争测试，具有实质覆盖。不过输入由配方本身构造，组件只要求结果非空，并未独立核对 GT6 数量和所有标签替代。GTMainRecipesTests 的打印机测试把任意正量流体当成黑色染料，没有检查具体流体、数量、能源和时长；地图仅锁定纸为 8，未锁定指南针/产物数量；displayRowsAreFake 强制 skipped 至少 5，未来补齐内容需要改测试而非维持缺口。

SurvivalExtensionTests 实际检查普通铁矿掉一个原矿、精准采集保存围岩状态，并扫所有普通矿的 oreRaw 形式存在；但仍额外断言 BlockEntityTag 必须存在。已允许移除旧矿石迁移，这条兼容断言也应随旧标签清理，保留 BlockStateTag 和新世界围岩/掉落检查。Unit 搜索测试检查别名集合，不是 JEI 客户端实际搜索；石材路线多数检查能找到配方而不核对数量。ToolBlockTests 检查绳索颜色字段、爆炸强度常量及配方存在，不覆盖攀爬、真实爆炸形状或引信交互；其中 2.5/10 半径期望正好与第 73 项原版差异有关，不能拿这条测试证明爆炸机制忠实。

## 材料处理与别名生成数据补充核查

### 121. 材料元数据生成器会覆盖已修正的别名和显示名

将 generate_material_metadata.py 的 OUT 重定向到 build/weekly-audit/material-links 后运行，生成结果与当前 MaterialRegistryExtras.java 存在实质差异：会删除 Ma→Magic、Osmium→OsmiumElemental 两个别名，删除 Empty/Magic/NULL 显示名设置，将 OsmiumElemental 的显示名由 Osmium 改为 OsmiumElemental；同时新增 Eudialyte、Graphene、HydratedCoal、Superconductor 显示名。该文件标注“自动生成”，但修正没有同步回生成器输入/规则，故后续正常重生成会回退。当前文件仍包含修正，本轮没有覆盖它，不应声称玩家当前已因这次检查失去映射。修复应把这些规则纳入生成源并检查别名身份，避免仅手改生成 Java。完整语句差异见 weekly-audit-material-links-2026-09-19.json。

另两份表已验证一致：MaterialOreProcessing 通过 transpile_gt6_materials 的读源、生成和拆分函数独立写到临时目录，与现有文件完全一致；本次输出为 1154 条处理调用、14 条跳过日志。GTMaterialFields 重生成的 1106 个字段映射也完全一致。GTMaterialFields 保留原版显示式名称（如 HSLA-Steel、Hard Plastic），调用方还须经材料注册表规范化；文件一致不代表所有调用方解析均成功。MaterialDefinitions.link 确实调用 applyOreProcessing、MaterialRegistryExtras.apply，故不是只有生成文件而无接线。原版循环/未生成材料的跳过项仍是覆盖边界，不能把 1154 条调用当成 GT6 矿处理全集验收。

此次首次载入材料生成器时因未加入 tools 模块路径而失败，随后补齐路径仅调用纯生成函数成功；没有运行会写正式模型/材料目录的 main。证据文件是在成功比较后写入。本批不含客户端、配方运行或生存流程验收。

## 制造、挤压、提示页与等级测试补充

ManufacturingTests 的制造核查包含实质数量约束：1320 条导线合成检查导体单位及材料族，78 个模具检查原图案匹配、组内碰撞、工具损耗及网络往返，机器导线与石墨烯路线计算输入输出材料单位并检查选择器保留；聚合物检查低温时长及乳胶液量，塑料混合检查三批液体与催化剂保留。它比单纯“配方存在”强，但调用的是配方对象/RecipeInputs.consume，不是实际方块接受能源后运行整个周期，模具组内无碰撞也不证明与所有其他合成配方无冲突。

ExtruderMatrixTests 的 route/contains 忽略数量，钢工具头和铜板变杆主要证明路线存在；钢块路线单独要求输入 9 锭，但未单独锁定产物数量。钻石路线在材料形式缺失时直接跳过。matrixCoverageIsReported 强制 skipped.size >= 4 且必须含 casingSmall，将缺失状态写进通过条件，不能以此证明全集完整；后续补齐应更新成对新增路线的校验。该问题与石材挤压、材料形式测试的覆盖边界一致。

RecipeHintTests 检查页面槽数、fake 标志、非空输入和自定义名称，hintsDescribePortChains 仅在显示标签里搜索 Cinnabar/Mercury/Steel 等词，没有执行朱砂/炼钢/镀锌链。它还主动调用 GTMainRecipes.register，故不能独立证明正常启动入口一定接线。labelled 在每行无条件加一，labelled==rows 本身恒成立；不过缺少标签同时加入 problems 并最终失败，因此不能误报为“完全漏查标签”。

VoltageTierAlignmentTests 独立列出几种机壳材料顺序，并核对注册 spec 和部件 ID，具有防止错档的价值。其电压 V 直接取项目常量、输入范围期望取 BasicMachineOriginalParams，属于内部一致性；原参数表与 GT6 的独立生成核对见此前证据。单档族、多方块和未识别能源类型在部分测试中跳过，聚变 range 专门排除，不能推广为所有机器运行验证。noMachineRecipeUsesAUlnComponent 检查不使用 ULV 部件，未按每张配方核对所有部件恰好等于机器等级。电力机壳数组只到 IV，但 suffixTier 可返回更高档：若后续在该注册集合扩展高档设备，需先扩展期望表或增加边界断言，不能直接索引导致测试异常；当前没有据此认定已有设备运行失败。

本批四文件均为源码定向审查，没有新运行测试结果，也未改动实现。

## 树木、下界散布、水域和树液袋测试补充

TreeGrowthTests 实际在世界放置树木，遍历树种检查自有树干/树叶与无外来原木，另测五种树苗经新建 GTTreeGrower 生长及蓝云杉高度边界。因此确有生成几何的服务端验证，不是仅查注册。但 all-species 主体直接调用 GTTreeShapes.grow，并不走每种树苗实际 grower/随机 tick；树木世界生成测试只锁定八项表的概率字段、非空生物群系和配置注册，未执行完整 GTTreesFeature 的自然选点/概率，也未对每格树形与 GT6 比较。不能据此排除第 51–52 项的地表选择问题。

NetherScatterTests 直接在人工地面上调用 scatter，绕过 Feature.place 的区块门槛、高度与尝试循环。12 个固定种子只要求两类产物均出现，没有统计验证概率；灵魂沙只检查材料名字，没有确认宝石物品形式，掉落测试仅要求列表包含燧石，不约束总数量或额外掉落。它能保护部分接触方块分支，不能证明第 53 项涉及的完整原版分布。

WaterBodyTests 实际调用 convertColumn 检查整列水转换、海底阻挡、保留海带和沼泽覆盖已有 GT 水，是有效的局部转换测试。biomeGatesMatchGt6 使用人工 ResourceLocation 集合，未由真实混合区块采样生物群系；没有执行自然 feature 入口、跨区块水体接缝或后续流体 tick/植物存活。立即检查海带没被改写，不能推广为邻居更新之后长期保持正常。

SapBagTests 的 collect 带人工直接调用，即使等待 4 tick 也不能证明自动 ticker 自己完成收集。breakingTheBagTrashesTheTankAndDropsTheItem 只调用 playerWillDestroy 后检查 be.stored().isEmpty，再 removeBlock 检查空气，完全没有查询 ItemEntity 或确认树脂数量；库存被清空但没有生成掉落也能通过，因此不能反证第 14 项。取树脂/250 mB 装容器测试有具体输出断言，但只用正常空背包和首个能接受树液的容器，未覆盖满背包、完整桶置换、自然保存。placePair 调用 setChunkForced(..., true)，本文件没有解除强加载；后续测试清理应成对恢复，避免复用测试世界时遗留强加载状态。此处只是确认测试文件的行为，没有据此推断历史卡顿的唯一原因。

本批为四文件源码定向核查，未运行新的世界生成或 GameTest。

## 物品生成表与方块实体注册补充

### 122. 重跑 MultiItem 生成器会移除 320 个已扫描蜜蜂变体

GTMultiItemsGen 当前共有 1278 条四元组，其中 320 条 id 含 scanned，来自 80 种蜜蜂的四种已扫描形态。文件类头也承认这些是手工追加。核查 transpile_gt6_multiitems.py:142–227：蜜蜂循环只输出 _drone/_princess/_queen/_dead，没有生成扫描形态的第二组分支，最后整体重写 Java。GTMultiItems.registerAll 直接遍历这张表注册物品，故重生成不是仅丢注释，而会取消扫描变体注册。

风险进一步扩展到资源：cleanup_previous 根据现有表删除全部旧 item model、整类纹理目录与两种语言的名称键；重建后只对新 registered 写英文 display（中文也使用英文），因此手工追加扫描模型/名称不保留。第 7 项中文回退的既有工具问题也适用于此维护流程。正常世界中当前文件仍保留变体，本次只读取生成器并统计，没有执行带清理副作用的 main。证据为 weekly-audit-multiitems-generator-2026-09-19.json。修复应先将扫描形态与中文维护规则纳入生成器，再做隔离重生成差异检查。

GTBlockEntities 的分组注册已定向检查：管线、轴、变速箱、泵、燃烧室、坩埚族、容器、传感器等从各注册列表提供有效方块；树洞/树液袋、双传送门、两种养蜂箱、三种堆叠物、硬币和湿泡沫均有实体绑定。多档燃气轮机与轴向涡轮/发电机使用定义列表，五档长距离变压器显式列出；激光转换器按实际类过滤，USB 交换机有实体，物流核心仍未见专用实体（与第 4 项一致）。旧 ORE 实体注册仍绑定矿石块，按决定可在解耦后移除；书架“八槽”注释过时，不代表实体实际只有八槽。这里验证的是注册调用与来源，不是每个方块已实际放置/重载的完整验收，不以注册存在反驳行为缺陷。

## 战利品与大蓝图回归测试的补充边界

LootInjectionTests 确实读取服务器加载后的箱子 LootTable 并抽取产物，强于仅检查生成表；但每表 12 次只要求出现任意 gregtech 物品，不核对原版权重、抽取数或是否保留/混入原版池。普通箱子表的抽取使用未固定种子的实际随机路径，小样本“必须出现”不是完整概率验证。vanillaChestRowsResolve 的 unresolved 只涵盖已经被生成器输出的行，不涵盖生成器跳过项。

lootBagsRollTheirTables 核对物品类型/表名并直接调用 GTLootTables.roll，不执行 LootBagItem 的玩家消耗及掉落实体路径。lootChestsRollTheirTables 主动调用 generateLootIfNeeded，并连续两次确认同一实体返回 0，能验证当次幂等；没有拆卸/重放、存档重载或物品态数据检查，因而不反证第 69 项可再次生成战利品的问题。书籍测试确认已有截断页短于 256、NBT 标题和页面非空，不能证明原文未丢失；第 100 项长页截断仍成立。

BlueprintRegressionTests 本批定向核查长距离物品端点、扩展器、内爆压缩机、大粉碎机、热交换器、TU 机器、燃气轮机和燃料规划的断言路径。端点/扩展器确实保留旧 capability 引用后拆掉连接再检查，内爆压缩机确实暂停/修复结构并完成加工；热交换器和燃气轮机使用已注册燃料，检查能源、排气和堵塞，是有效的局部运行覆盖。粉碎机与 TU 机器则通过 setSpec 替换为苹果变钻石的测试配方，用于隔离工作量/时序，并不证明生存配方与原配置正确。燃气轮机只在一个朝向及不锈钢主控上验证，并把接收端改为全向接受测试 spec，不能推广为全部材质、朝向、自动管路和自然重载通过。多方块布局通常从被测定义本身取格子搭建，一致性仍需与 GT6 原布局独立比对。既有第 6 项坐标问题与第 85 项工作量差异不因存在这些局部测试而自动排除。

本轮没有新运行 GameTest，历史通过日志只作为历史结果，不将本次源码审查写成运行通过。

## 材料前缀与自然沉积坑测试补充

MaterialPrefixes 的项目角色是前缀目录与块前缀构建器，物品实体实现仍委托 MaterialPrefix/PrefixRegistry。已定向核查注册、委托查找、BlockBuilder、defineBlocks 和 bootstrap：机壳四档为 8/14/26/56 U，原矿箱 128 U、原矿块 18 U，其他普通箱 64 U、普通压缩块 9 U；矿石/小矿标记为 2/1 U。这些定义本身不证明矿物加工产率正确，实际处理还结合原矿倍率、围岩和材料目标，已发现的算术差异见第 44、96–97 项。机壳条件保留旧属性判断并兼容 MaterialForms.PARTS；矿石条件兼容 GENERATE_ORE 或 ORES。Entry 可以没有物品 delegate（例如只用于分类的名字），hasDelegate 与 isBlock 是不同判断，不能把整个目录项数当成可用物品种类数，也不能按所有 null 委托直接报注册失败。

PitTests 是自然沉积坑/黏土层测试，不是烧制坑。twigsHaveASingleOwner:145–159 中 source 初始化为 null 后没有赋值，最后 source==null 恒成立；读取 class 资源只判断流存在，没有解析字节或执行排除重复生成的行为，所以完全检测不到第二个树枝生成入口。应换成明确入口/调用关系检查或受控生成行为验证。

该类其他部分不能因此否定：pitCarvesGt6Columns 实际改写沙柱、检查不超过七层和纯石拒绝；表测试锁定五种填料与 48×48 掩码的一些参数。黏土测试实际构造地形、预刷新 OCEAN_FLOOR_WG 高度图，执行 placeClaySeam 及完整 surface deposit feature，检查表面黏土、厚度和外围不变。完整 feature 测试是在人工岸边运行 128 个固定种子尝试并要求至少一次放置，不验证自然概率、地形混合与生物群系覆盖；树枝恒真断言不影响这些独立断言的价值。本轮是定向源码检查，没有运行新的生成验收。

## 硬币堆资源与木材配方测试补充

CoinAndPileAssetTests 本批定向核查 coinStackRoundTripsNbt、coinPileTakesRealCoinsOnly、pileStateShowsItsStack 与 pileAssetsResolve。它实际检查银币 NBT/数量、堆中币种与面计数、拒绝混币、64→63 时服务器 blockstate 更新；这些是有效的服务端状态测试。资源部分检查 variant 数、模型及 PNG 资源存在、face 的 tintindex、双语键，但跳过 # 贴图变量解析，不解码 PNG，不验证客户端颜色回调和材料更新包，也未进行模型烘焙。因此不能以此排除第 35 项客户端堆叠物材料同步或发白问题。手动创建实体 load(savedTag) 不是自然区块保存重载。

WoodRecipeTests 本批核查 woodRowsUseThePortsOwnSpecies、woodRowGridsMatchExactlyTheirOwnRow、woodSpeciesCarryTheirMaterial：68 条已注册木材配方逐项比较产物种类/数量、树种输入和锯/锉需求，构建合成格后与 recipe manager 全部配方比较冲突，强于只在木材组内查重。仍仅选每个 Ingredient 的第一个候选，未穷举标签替代、偏移和镜像输入，也没有代替玩家合成/数据包 reload 验收。第 8 项启动后注入配方重载消失风险仍须独立验证。

该测试显式要求 cinnamon 材料映射为 null、原版 ID 9317 不存在，并允许四条肉桂杆路线缺失。这是已知缺口的固定期望，不是肉桂木完整移植的证据；后续添加材料必须更新生成表和测试，不能为保持门禁绿色继续留空。此前 68 条生成表一致性核查只证明当前快照，没有覆盖该缺口。以上为定向源码审查，没有新运行或客户端渲染结果。

## 最后四份 Java 测试的定向核查（2026-09-20）

PileBlockTests.breakingAPileDropsItsContentsOnce 明确要求 getDrops 后库存清空，第二次查询不给内容；这把第 36 项的有副作用掉落查询固化为通过条件。列表数量确有检查，不能说完全没测掉落；但真实 destroyBlock 路径只断言方块消失/实体清空，地面 ItemEntity 数量仅写诊断而不作断言，因此未证明真实破坏恰好掉一次。还显式要求额外掉一个空 pile 方块，注明属于移植差异，不能作为忠实 GT6 的依据。

PortRegressionTests 的 basicMachineRemovalDropsInventory 则实际清理旧实体、删除机器后求和带 NBT 的钻石，断言 17 个及库存清空，是更强的局部掉落验证。pendingOutputsSurvivePartialFlushAndReload 检查部分填充后余下 3 颗钻石/500 mB，machineDropCarriesPendingWorkWithoutInventoryDuplication 检查打包工作不复制库存、重载后产出 4 颗并清除 pending；后者通过人工构造 NBT 与直接 load 恢复，未覆盖玩家正常放置物品全过程。既有砧空手错误测试等见第 43 项，不因这些有效库存断言而撤销。

ItemBehaviorTests 本批核查胶带、疏通器、脱色喷罐路径。胶带测试使用人工 Tapeable lambda，且显式承认当前无实际方块实现；不能当成机器已可贴胶带。疏通器测试使用 BasicMachineBlockEntity 的内部输出罐，未检查只通过 Forge capability 提供罐的其他方块；脱色测试只放默认玻璃板状态，没有含水/连接状态保持检查。这些覆盖边界对应第 29、30、33 项，属于解释历史通过为何没有捕获实际问题，未新增重复功能缺陷。

DungeonTests 本批补查农场和瓶箱路径，并结合此前房间/仓库/矿井核查。农场用 24 个种子直接生成房间，检查出现自有及原版树苗、最后房间有花；没有检查甘蔗相邻水存活或所有植物延迟更新。workshopPlacesItsBottleCrate 名称及注释声称测试房间放置，实际直接 server.setBlock 放瓶箱，未调用 Workshop.generate；insert/extractLast 也没有经过右键交互。破坏后只要求 4 格内任意 ItemEntity 数量 > 0，既不过滤瓶子也不求和八瓶、更不先清除旧实体，可被其他掉落满足。应补真实房间生成、正常右键及准确瓶数验证。

当前时间戳清单的 433 个 Java 文件均已有定向源码或生成器核查记录（390 源码、43 源码/生成器），不是逐行形式化验证或客户端全功能验收。所有 runtimeVerifiedThisAudit 仍保留 false；整体审计还需汇总非 Java 改动与验证证据，不能仅由 Java 文件计数宣布完成。

## 非 Java 清单格式与脚本副作用复核（2026-09-20）

对原时间戳清单重新检查：9742 个 JSON、20 个 mcmeta 均能解析，171 个 Python 均能 AST 解析；686 张 PNG 先检查结构，再逐张完整解码，均无错误，清单中无文件消失。结果见 weekly-audit-format-recheck-2026-09-20.json。这证明格式可读，不证明引用目标存在、颜色/光照正确、配方语义正确或所有已注册对象有资源；之前已确认的缺失 sapling_small_* PNG 是“引用目标不存在”，不是已有 PNG 无法解码。

建立 weekly-audit-python-safety-index-2026-09-20.json，按 AST 索引 171 个脚本内常见写入、删除/重命名与 subprocess 调用。这是副作用定位工具，不是任意 Python 的安全证明（动态调用、包装函数及间接写入仍要读源）。命中六个需查看的脚本：MultiItem 清理问题已见第 122 项；generate_ore_block_assets 的删除局限于三个资源目录 ore_*.json 且先并入已发货矿石集合；generate_pile_assets 删除指定旧 decoration 模型后生成新分层模型；generate_bumble_hive_assets 删除固定 build/_hive_tmp.png 临时缓冲；lowercase_materialicons 默认只读，显式 --apply 才重命名且先检查同名冲突；javac_selfcheck 启动指定 javac，使用现有 build 类与依赖缓存，不能替代全量干净构建。本轮仅读取这些副作用代码，没有执行删除/重命名或写正式资源的入口。

补查根目录范围：本周时间戳有 build.gradle、PORTING_AUDIT.md、hs_err_pid53444.log，以及本次审计自己的日志。构建资源同步路径此前已读并通过隔离场景测试，见本报告对应章节。hs_err_pid53444.log 显示 GradleWrapperMain compileJava --offline 在启动 0.015 秒时原生 malloc 申请 1 MiB 失败，命令行仅 Xmx64m/Xms64m；不是游戏运行几分钟后的 Java 堆异常，不能当成模组内存泄漏证据，也不据此给出唯一系统内存成因。日志不是当前正在运行的进程。

剩余收尾需要把脚本定向证据、资源语义检查及文档交接结论统一列清，仍不能由格式检查通过推出整个周改动没有问题。

## 交接入口更正与后续执行清单（2026-09-20）

重新核对当前源码：LongDistancePowerTests 有 3 个 @GameTest；GTTechnological 注册 6 个 filtered、3 个 generic、1 个 dump 路由用途及 4 个显示用途物流覆盖板；GTLasers 的 logistics_core 仍为普通 Block，logistics_wire 仍列于 iconset 装饰注册；LogisticsCoreStructure.range 返回 control，energyPerOperation 返回 128 + logic × 64 × conversion。与先前审计一致，未发现这些问题已经由其他工作修复。

本次已修改 NEXT_GOAL 顶部“没有半成品”断言，明确 681 项和 jar 为历史快照，并把修复优先级置于新增物流核心之前。物流对接单顶部新增优先勘误，纠正测试数、覆盖板分类、范围和能耗含义，同时撤回“Crystal Chargers 永远不可用、无需移植”的过强结论：没有内置 LU 物品不能排除外部接口实现。历史原文保留作追溯。新的 AUDIT_NEXT_STEPS_2026-09-20.md 列出修复顺序、验证方式、生存链和客户端测试，以及允许删除旧矿石迁移的边界。

本次只修改三份交接/推进 Markdown 和本报告，没有修改 Java、正式资源或测试，没有运行新 GameTest/客户端或打包。未修改临时目录中的其他交接单，以免覆盖其历史记录。剩余非 Java 清单中还包括 test_fusion_empty.nbt、materials-source.java.txt、missing-content-tokens.txt 及机器配方/历史进度文档；格式统计不能替代这些内容与来源的核对，整体审计尚未据此标记完成。

### 123. 缺失内容清单在零缺失日志下不会清空，进度会沿用旧值

审查 tools/audit_porting_progress.py:149–164，refresh_missing_tokens 仅在 tokens 非空时写回文件；日志明确报告零缺失或没有匹配行时，均读取旧清单并作为返回结果。用 build/weekly-audit/token-refresh 下的临时日志和清单实际调用该函数：预置 stale:old，零缺失返回 total=1 且保留旧行；无相关日志同样返回旧值；正常单条日志能正确覆盖。证据见 weekly-audit-token-refresh-2026-09-20.json。没有修改正式 missing-content-tokens.txt。

这不表示当前 49 项是虚假的：当前文件为 49 个唯一 token，与 build/gametest.log 的唯一匹配行完全一致。问题在下次统计；应区分“成功解析的零缺失”“日志缺失/无报告”“正常非零”，前者写空清单，后者明确标注未知或陈旧，不能静默当成本轮结果。此前记录的正则跨行风险仍应一并修复。

## 剩余结构模板与文本快照检查（2026-09-20）

test_fusion_empty.nbt 112 字节 gzip 可解压为 114 字节 NBT，独立解析到根 compound 结束且无尾随字节：DataVersion=3465，size=[25,9,25]，entities/blocks 为空，palette 仅 minecraft:air。与 restore_fusion_assets.py 写出的模板定义一致。它是测试场地，不是预先存好的聚变结构；FusionRegressionTests 的 build 在运行时按 FusionStructure.CELLS 放置结构。模板格式正确不证明布局忠实原版，也不等于已经运行客户端/服务端。证据见 weekly-audit-template-and-token-check-2026-09-20.json。

materials-source.java.txt 是生成器中间快照，不参与 Java 编译；modular_materials.write_catalog 可重写它，多种资产/别名/字段工具会读取它。因此快照仍含旧符号不等于运行代码仍使用这些符号，但其过时会传播到生成结果。用实际 parse_catalog 读取为 Elements 172、Compounds 570、Ores 125、Stones 71、Woods 120，共 1058 条；不能与含额外注册和别名的运行时 1156 材料数直接比较并判丢失。此前隔离重生成核对及第 121 项别名回退仍适用。

MACHINE_CRAFTING_RECIPES_2026-09-14.md 是多轮追加历史，既有后续修复记录，也保留旧的“缺口/下一批”段落，不能逐句作为最新待办；其机器/配方生成表与参数检查已见前述证据，本文新增的实际行为差异优先于历史“已对齐”表述。未把历史日志中的通过重新记成本次运行通过。

## 六个资源工具隔离重生成检查（2026-09-20）

读取并在 build/weekly-audit 子目录隔离执行 generate_gt6_tree_assets、generate_wood_tags、generate_rope_dynamite_assets；正式资源未写入。树木脚本在模块顶层直接生成文件，不能以普通 import 当作无副作用读取；本次将工作目录限定为临时目录。绳索脚本通过 main 全局 ASSETS/LANG 改到临时目录。共比较 68 份 JSON，语义均与正式文件一致，见 weekly-audit-tree-rope-regeneration-2026-09-20.json。

树木脚本为 12 种树写叶片 14 个 distance/persistent 组合、树苗两阶段及模型；不复制或检查所引用的 sapling_small_* PNG，因此一致性不消除既有缺图。额外 waterlogged 属性未逐项列入 variants 本身不能认定错误，模型选择可不依赖该属性。木材标签脚本只生成这 12 种树的 logs/leaves，当前隔离结果未删去已有额外值；今后增加树种需同步扩充。绳索/炸药脚本复用原模板和灰度模型，名称使用 setdefault 保留已有翻译，不能据此证明客户端材料着色或绳索攀爬功能通过。

另读取并隔离执行 generate_loot_bottle_assets、generate_guide_book_assets、generate_material_dictionary_assets。前两者各 4 份产物与当前文件一致，原版 PNG 为逐字节比较；词典脚本模型/PNG/英文一致，但 data.update 会把 zh_cn 的 item.gregtech.dusty_material_dictionary 从“落灰的材料词典”覆盖为“沾满灰尘的材料词典”。这是已有中文生成源回退问题的具体复现，记录于 weekly-audit-loot-asset-regeneration-2026-09-20.json；应使用标准中文映射维护生成源，不能只手改正式 lang。此处没有新确认战利品行为，相关运行逻辑仍以此前源码审查和缺陷为准。

### 124. 镜像规则工具的 --check 与 --reset 组合仍会写文件

apply_recipe_mirror_flags.py 的 reset 分支在 check_only 判断之前调用 write_text。将 RECIPES 指向 build/weekly-audit/mirror-check-reset，预置一个带 allow_mirror=false 的测试配方，实际调用 main 并传 --check --reset：文件中的 allow_mirror 被删除，输出却仍说 would be written。证据见 weekly-audit-small-recipe-generators-2026-09-20.json。本次没有操作正式配方。应让 check 模式约束所有写入分支，或明确拒绝该参数组合，并加入文件哈希不变测试。

### 125. 石头生成模块接受原版未允许的左右镜像配方

GT6 MultiItemRandomTools.java:424 使用 CR.DEF_REV_NCC，CR.java:161–168 显示该组合不含 MIR。当前 stone_generator_module.json 和 generate_generator_module_recipes.py 均使用 minecraft:crafting_shaped，图案 CPC/LMW/COC 的左右输入分别为岩浆桶与水桶。检查本地 Forge 映射 ShapedRecipe.java:97–103：匹配明确尝试镜像 true 和 false，所以互换两桶仍匹配，与 GT6 的非镜像标志不同。apply_recipe_mirror_flags 只处理 gregtech:tool_shaped，无法修正这种原版 serializer。另两种生成模块为单列图案，镜像无实际差异，不应一并算成三条行为 bug。后续应用支持禁止镜像的配方类型并核查正常与镜像输入、空桶返还；没有在本次启动游戏复测。

## 木管、绳索和生成模块配方生成源补查（2026-09-20）

三份生成脚本的 13 条定义与正式 JSON 逐键比较：5 条绳索/炸药和 3 条生成模块完全一致；5 条木管只相差正式文件额外的 allow_mirror 与 _mirror_flags。木管生成器遇到已有文件直接跳过，故当前重跑不会覆盖已有标志，但从空目录重建仍需要另行运行镜像后处理；其 --check 也只报告文件存在/缺失，不校验内容正确性。生成结果一致不表示 GT6 配方完全保真，第 125 项即是一例。

绳索模板使用 tool_shaped、显式禁止镜像，丝绳四根线、草绳七份干草、藤绳四根藤、钢绳四份细钢丝；工具耐久/返还仍由共享 serializer 和物品行为负责，不能单凭 JSON 类型宣称已验收。绳索脚本明确未生成 hemp 和 plastic 路线，不能把这 5 条视作全族生存闭环。生成模块仍明确缩窄为原版两种活塞及水/岩浆桶，未覆盖原版矿辞典允许的全部外部活塞和 1000 mB 容器。以上证据由只读比较和临时目录复现得到，未修改正式配方。

## 十二个一次性修补脚本的当前重跑行为（2026-09-20）

逐份读取并将目标源码/资源复制到 build/weekly-audit/one-shot 的各独立目录，随后执行原脚本顶层逻辑；所有工作目录均指向副本，未在正式源码执行。证据见 weekly-audit-one-shot-tools-2026-09-20.json。

8 个无语义内容变化：fix_duplicate_didyouknow、fix_duplicate_machine_parts、rename_nether_lava_level、wire_nether_bedrock_ores、wire_cfoam、wire_coltan、wire_water_bodies、wire_rocks_deep_ocean。其中部分仍重新写入相同内容，mtime 可能变化，不能把无内容差异当作完全无副作用。现有 LargeMachineParts 保留 89 条，该去重脚本本次没有删除；它用 GTMultiblocks 的任意 ID 形态字符串推断所有权，未来不能当作精确注册解析器。

3 个在写入前因旧锚点不存在停止：fix_hint_zinc_fluid、fix_nether_quartz_test、fix_deposit_twigs。这是历史补丁不适用于当前源码，不是对应游戏功能重新坏了。特别是 quartz 补丁有先写 feature、再验证 test 的顺序，若未来只恢复第一处旧锚点而第二处不匹配，会留下部分修改；不应把这类脚本直接列入日常重生成入口。

fix_flowing_fluid_import 重跑会把正确的 FlowingFluid 导入从 1 条变成 2 条，因为它无条件在 LiquidBlock 导入前插入；这是非幂等维护问题，重复同一导入本身不据此声称会导致 Java 编译失败。后续可归档一次性脚本或增加已应用检测，避免不断制造噪声。

四个世界生成接入工具的当前输出与既有配置一致，不能据此证明自然生成概率、维度边界、邻块更新及地形保存已经正确；相关功能问题仍见此前世界生成审查。此次仅验证维护工具在当前输入上的行为，不扩大成游戏运行验收。

### 126. 缺失材料扫描会被同名材料和任意字符串掩盖

report_missing_materials.py 将“ID 已出现”或“归一化名称已出现”任一成立都当作已覆盖，并把文件名含 material 的 Java 中几乎所有普通字符串加入候选名称。当前运行只报 5 个 Hexorium，但这不是全部缺失材料。具体反例：CompoundMaterials.Cinnamon 是 id 9785 的肉桂粉材料；原版 WOODS.Cinnamon id 9317 的肉桂木并未实现，WoodRecipeTests:537 附近仍明确检查该缺口。MaterialForms 还包含 9317 与 cinnamon 的形态元数据，但元数据不等于注册了材料。相同 Cinnamon 名称使该工具不报告肉桂木，直接影响生存木材配方的缺口判断。后续需按原版完整字段路径/ID 与实际注册对象映射，别把展示名、别名表或形态元数据当作材料存在证明。

### 127. 缺失内容排行漏掉三段材料 token，并把出现次数混称配方数

rank_missing_content.py 当前读取 49 个 token，输出 recipe occurrences blocked:120；实现只是逐生成表字符串出现次数求和，没有按配方去重，同一条配方有多个缺失输入还会多计，不能理解为 120 条独立配方。材料形态排行要求 len(parts)==4，而当前 i:gearGt:Stone 等缺失 token 是三段，因此实际运行“most common material forms still missing”一节全空。应按 token 协议解析，并明确区分引用次数、独立配方数与可恢复配方数；补一个物品也未必解锁同时缺少另一输入的配方。

## 八个缺口报告工具的证据边界（2026-09-20）

读取并运行 report_items_without_recipes、report_missing_machine_recipes、report_missing_materials、rank_missing_content、report_material_form_gaps、report_material_form_idless、report_unused_form_flags、report_material_form_key_collisions；均为只读报告入口，输出保存在 weekly-audit-gap-report-tools-2026-09-20.json。

物品报告称 74 个 technological 物品无数据包配方，并同时展示历史游戏报告的 363 项检查/0 缺失/248442 机器配方行。二者范围不同：脚本不读运行时机器配方，只扫描 data 下所有 JSON 的 result，还丢弃命名空间；同名外部产物可能误当成本模组已有配方。没有运行本轮游戏，所以历史 0 缺失不能代替本轮生存链验收。

机器报告的 248 参数条目、245 配方条目、52 多方块配方与此前检查一致；它对 MULTIBLOCK_CONTROLLERS 中的名称直接豁免，不验证被豁免的具体等级确实有可解析配方，也不证明所有方块注册都存在于 Params 表。因此“every registered variant has a crafting row”只是该静态集合差值为空。

形态缺口报告显示 0 flags still have missing examples，只列包含 examplesMissing 的现存报告条目，不自行解析原版或实测物品；此前第 118 项报告不完整的边界仍适用。idless 报告发现 79 个无捕获 ID 的条目，不代表 79 个原版材料没有 ID或未移植。unused_form_flags 从生成表同时统计名称行和 ID 行，数量不等于独立材料数；“原版永远无用途”的结论还须排除动态和兼容扩展，不能只据当前生成快照判断。key_collisions 当前 2232 行、0 重复键，CO/Co、HF/Hf、NO/No 三组均有 ID 覆盖；这是表键一致性，不证明原版同名材料映射完整。

## 材料形态可达性与前缀扫描边界（2026-09-20）

读取并运行 report_material_form_lookup、report_material_form_unreachable、report_missing_item_prefixes、report_missing_prefix_usage，输出追加到 weekly-audit-gap-report-tools-2026-09-20.json。lookup 只成功把 1063 个解析出的移植材料中的 226 个与原版声明按 ID 联结，所谓 still unreachable=0 只覆盖此联结子集。unreachable 则扫描同样 1063 个显式工厂声明，1055 可找到 ID/名称键，8 个未找到；正则无法代表运行时全部 1156 个注册对象，而且“键存在”不同于目标形态已生成，更不等于形态对应的是正确原版材料。

前缀工具扫描到原版比较表的 215 项条件、移植 108 个物品字段和 19 个 block 构造名，报 93 个未匹配前缀；其中 49 个在原版 loaders/items 出现。这个结果不是 93 种可玩内容全部缺失：例如 bottle、wireGt 系列可能由独立注册系统提供，不能按 MaterialPrefix 字段名缺失直接重建一套。usage 工具直接对 Java 文本匹配 OP.name，包含注释、兼容和非配方语句，不计算执行条件，也不覆盖其他目录或静态导入，所以“never referenced”应仅解读为本扫描范围内没有该形式的文本引用。

新增 weekly-audit-python-coverage-2026-09-20.json，列出 171 个时间戳候选脚本与报告具名章节的对应关系。该索引特意不把“名称出现在报告”当作语义通过：没有对应关系的脚本可能是此前按模块检查但未登记名称，也可能尚未定向检查，后续要查证并补证，不能把它们自动标为已完成。所有候选的 AST 解析已另有格式证据。

### 128. 树苗名称偏离标准中文，补键脚本也无法补齐部分缺失

原版 BlockTreeSaplingAB.java:51–53、59–61 确认 Hazel/Cinnamon/Coconut 对应 meta 4/5/6（及 12/13/14）。GregTech.lang 的 gt.block.sapling.4/.5/.6 分别是“榛树树苗”“肉桂树苗”“椰子树木树苗”；当前 zh_cn 对应值是“榛木树苗”“桂皮树苗”“椰木树苗”。这三项为新增具体标准差异，不能把此前仅材料相关的 7 项比较当作全部语言覆盖。

add_tree_species_lang.py 未读取标准中文源；只检查 entries[0]（hazel）是否存在，存在则跳过本语言全部四项。隔离副本保留 hazel、移除 coconut 后运行，英文和中文 coconut 都未补回，脚本仍报告 already has 1 of the new keys, skipping。修复应逐键补齐，并按原版 meta 映射取标准译名，而非另起中文名。证据见 weekly-audit-tree-lang-standard-2026-09-20.json；正式语言文件未改动。

## 七个语言维护工具复查（2026-09-20）

读取并在独立副本上运行 add_tree_species_lang、add_loot_crate_lang、add_valve_part_lang、add_usb_tooltip_lang、drop_retired_cover_lang、add_track_lang、add_bumble_scanned_lang，当前两种语言文件语义均未变化，见 weekly-audit-lang-tools-2026-09-20.json。但无变化不代表名称符合标准，第 128 项是反例。

valve_part 直接给两种语言添加原版英文，仅跳过已有键；USB 工具则逐键强制覆盖其硬编码中英文。track 从当前材料中文拼接族名，scanned bee 从已有普通蜜蜂名拼扫描后缀、缺中文时退回英文，并要求恰好 320 个扫描变体；它们没有证明与GregTech.lang 的专属译名一致。蜜蜂计数前提与第 122 项重生成丢注册问题相关。drop_retired_cover_lang 仅删列出的六个精确键行，当前无删项，不应扩大为可安全删除任意覆盖板翻译的工具。

文字插入工具依赖一键一行、逗号和锚点布局，不能当通用 JSON 合并器。当前文件隔离重跑后可解析；本次没有把未覆盖的任意文件布局假设成当前游戏损坏。

### 129. 岩层统计使用失效的方法锚点，错误报告全部材料为零

list_layer_seams.py 使用 text.find("static void load()") 后直接 text[start:]，没有处理 -1。GTStoneLayersGen 当前为静态初始化表，没有该方法；切片只剩文件最后一个字符，实际执行得到 0 distinct/0 defs，并把煤、褐煤、铝土矿等全报 0。源码 :44/:45/:48 明确有 Coal/Lignite/Bauxite 的 layer 调用，:131 还有褐煤层。这是诊断脚本失效，不是这些材料在当前生成表中缺失。应解析实际 layer 参数并对缺失锚点报错，不能以成功退出和空集合表示零内容。

## 十二个只读诊断视图核查（2026-09-20）

逐份读取并运行 find_alloy_recipes、find_multiitem_id、find_water_fluids、show_basic_machine_recipes、show_machine_parts、show_material_enchantments、show_material_form_entry、show_missing_veins、show_vein_order、list_layer_seams、list_recipe_ingredients、list_tooltip_keys；输出保存于 weekly-audit-diagnostic-views-2026-09-20.json。这些入口只读取文件并打印，没有更新正式源码/资源。

find_alloy_recipes 只查固定七种材料名字和单行 CrucibleAlloying 文本，本轮只打印文件标题，不能说明合金不存在；find_water_fluids 只匹配 fluid("字面量") 而非全部流体注册来源。find_multiitem_id 限定四元组最后一列为空，故有非空资源字段的条目可能不在视图中。show_basic_machine_recipes 读取提取快照、区分大小写，输入 oven 得到 NOT PRESENT 不证明已注册机器没有配方；list_recipe_ingredients 同样是快照的表达式使用计数，非配方加载验证。

show_machine_parts 输出 89 个 Part 记录，不包含由其他注册入口承载的所有部件。show_material_enchantments 按六个硬编码名字匹配，不解析别名、全部材料或实际工具附魔事件；show_material_form_entry 只展示缓存报告条目，不访问材料注册表。list_tooltip_keys 只展示特定 tooltip.gregtech 前缀与关键词，字符串正则也不是 JSON 转义解析器，不能证明翻译完整。

show_missing_veins 的 WANT 是固定八个名字，不执行“当前是否缺失”的比较；同批 show_vein_order 反而能看到这些矿脉已在主世界表中。其当前主世界 31 条、末地 5 条的名字顺序与所读原版报告一致；这仅是顺序比较，不等于分布/参数/维度行为验收。list_layer_seams 的实际误报单列第 129 项。

### 130. 退役覆盖板检查找到坏引用也不会使检查失败

check_retired_cover_refs.py 的说明是 Fail loudly，但实现仅打印 hits，没有非零退出或异常。当前正式资源扫描无命中；另在 build/weekly-audit/retired-cover-check 放一份引用 gregtech:cover_pump 的临时配方，执行原脚本确实打印命中，但仍正常返回（exit 0）。证据见 weekly-audit-small-check-tools-2026-09-20.json。这是校验器缺陷，不表示当前正式配方有该坏引用；如果作为自动门禁，应在命中后失败，并区分实际 item 字段与注释中的同名字符串。

## 七个小型校验工具的实际覆盖（2026-09-20）

读取并运行 check_form_flags、check_material_forms、check_nether_ore_forms、check_retired_cover_refs、check_enchant_table_coverage、check_material_id_agreement、check_material_lang_collisions，输出见 weekly-audit-small-check-tools-2026-09-20.json。前三者只是打印已有表/报告样本，没有断言注册或矿石存在；nether 工具甚至在整个 entry 的字符串里找 ORES，而非专门查询 flags。

附魔覆盖工具从 zh_cn 的 material.gregtech 键推断“已注册材料”，报 1530 个材料名、177 行中 173 可解析/4 未解析；这不是实际材料注册或附魔生效检查，语言键可能保留未注册内容。原代码还需经过真实字段解析和工具应用路径，第 83 项不因这里打印可解析而撤销。

ID 一致性工具只联结 226 个显式声明，打印 0 名称差异；它允许名字前缀相同即通过，用 setdefault 折叠同 ID 的多声明，故不能充当重复 ID 检测器或全量身份校验。语言碰撞工具英文比较 displayName，中文只检查空字符串；当前两者输出 0，也不能否定第 128 项三个非空但不符合标准的树苗译名，且其遍历范围本身仅限 material.gregtech 键。后续把诊断视图与真正能阻止错误的门禁区分，避免“运行无异常”被写成“全部内容正确”。

## 扫描蜜蜂、轨道、树洞与灌木资源补查（2026-09-20）

读取 generate_bumble_scanned_models、generate_tree_hole_assets、generate_track_assets 并检查产物。树洞与轨道在隔离目录生成后共比较 201 份 JSON（含两份语言副本），均与正式文件语义一致；直接 gregtech 贴图引用均有目标。扫描蜜蜂工具 --check 报 would write 0，现有扫描模型与相应普通变体文件一致。证据为 weekly-audit-tracks-tree-bee-assets-2026-09-20.json。脚本的“340 models”是调用写模型函数的次数，轨道实际上 160 个唯一模型 + 30 个 blockstate，重复方向复用同名模型，不能按 340 算新增资产数。

树洞模型将孔置于 north 并由 blockstate 旋转四向、resin 两状态；轨道模型声明 cutout，普通轨道十种 shape、两种功能轨道各六种 shape × powered 状态。当前文件一致并不等于客户端光照/方向已验收，且不会修复第 74 项助推轨道继承行为。树洞语言与树苗脚本类似，只看第一项名称存在就跳过整组；当前三项均存在，本轮没有将该潜在补键边界另算游戏缺陷。扫描模型工具缺普通源文件时仅打印并返回 0，不应用返回码单独作资产完整门禁。

读取 generate_bush_assets 并仅在内存中将输入/输出路径指向原版已存在资源及临时目录，运行后 14 个文件（6 PNG、6 模型/状态 JSON、2 语言副本）与正式资源一致。它读取原版 build/resources 而非 src；当前所读 PNG 与 src 对应文件逐字节一致，未发生旧构建素材覆盖。颜色层使用 tintindex 0/1，overlay 不染色、cutout；这只是模型定义，非客户端颜色验收。证据为 weekly-audit-bush-asset-regeneration-2026-09-20.json。

extract_bush_colours 只打印提取结果，读取的是原版 build/sources/java/MultiItemFood.java。当前该文件与 src 原文逐字节一致，提取 8 条 BushesGT.put，见 weekly-audit-bush-colour-source-2026-09-20.json；不能把内置 13 个 PORT_IDS 映射键数当作提取到 13 条配色，也不把存在未使用映射直接当作缺失原版内容。

### 131. 失败日志查看器不验证测试完成，空日志或启动崩溃也返回成功

show_gate_failures.py 仅统计含 LogTestReporter]: 的行，无此匹配就返回 0，没有要求 GAME TESTS COMPLETE 或 All N required tests passed。用三个临时日志分别模拟空文件、RuntimeException 启动失败、681 tests are now running，均返回 0。本工具本来用于列失败，但不能把其返回码作为门禁通过证明；应单独校验结束标记、必需测试失败数以及启动/运行进程退出情况。证据见 weekly-audit-log-artifact-tools-2026-09-20.json。

### 132. 非 ASCII 查看器的 BOM 检测使用了转义文本而不是真正字节

report_non_ascii_chars.py 的 startswith 检查字节字面量包含反斜线转义文本，而非 EF BB BF 三个字节。临时文件以真实 UTF-8 BOM 开头，脚本仍打印 bom=False；utf-8-sig 解码又会移除 BOM，所以后续字符统计也不会显示它。该脚本没有改动文件，此问题影响诊断准确性，不代表当前语言文件有编码损坏。

## 内存、门禁和 jar 交接工具复查（2026-09-20）

读取并运行 _heap_report、show_gate_failures、handoff_check、check_hive_jar_contents、report_non_ascii_chars，结果保存在 weekly-audit-log-artifact-tools-2026-09-20.json。当前历史日志仍为 681 项通过、0 个该格式失败；jar SHA256 仍为 16661AFA3C3C04847C296B49E2D9E4D334E13464A691A8407F853931B8A5C79B，两个时间不等式仍 True。这是重新读取旧产物，不是重新编译、GameTest 或启动客户端。

handoff_check 的题头 working tree still green 超出其实际能力：只打印日志片段与 mtime，不根据失败返回非零，也不检查资源/生成器修改时间、构建输入内容或源码与 class 对应；当日志不存在但 jar 存在时，gate log < jar 甚至会因 not exists(LOG) 被打印 True。蜂箱 jar 工具仅检查文件名、计数和八个 class 名，缺失也只打印 MISSING，不读 lang 键或模型/PNG 内容。因此必须保留此前对历史成功证据的范围限定。

_heap_report 只处理已有 build/heap-world.log，没有采样当前 Java 进程。它将后续直方图行里的未匹配值以 None 覆盖先前计数，最后一个样本缺少计数；零值也会被过滤为缺失。现有日志里旧矿石实体数和堆内存变化不能直接归因到当前代码，更不能据最后四次历史样本判断现在仍有泄漏。已经允许取消旧矿石迁移，后续按新世界实际场景测量，不用旧表反复推动兼容修复。

## 八个解析辅助视图复核（2026-09-20）

读取并运行 debug_early_tool_rows、debug_masonry_parse、debug_material_ids、debug_prefix_conditions、resolve_port_materials、probe_port_form_families、list_material_prefixes、report_form_flag_materials，输出见 weekly-audit-parser-probes-2026-09-20.json。运行前检查其导入模块入口：辅助脚本只调用 extract/parse/helpers 等读取函数，没有执行被导入模块带正式写入的 main。

早期工具视图实际调用工具提取器，输出带材料变量、柄和镜像标志的行；它不负责在运行时展开 tMaterial 或检验配方 serializer，因此第 0 项协议问题仍须独立修复。砌块视图解析出 hammer/crusher/shredder 各 16 行、28 条 crafting，其中包括 4 条 preamble；它只展示原版解析结果，不能证明移植配方逐条一致，更不能将模板循环里的行数直接等同加载后的配方数量。

材料 ID 视图扫描原版 230 个显式声明、移植 1063 个显式工厂 ID，和 1081 形态 ID 比较，打印 26 个集合差值；很多原版名字为问号是声明正则未捕获，不是未知原版材料。resolve_port_materials 的 Cinnamon 输出明确指向 CompoundMaterials 的粉末 id 9785，与第 126 项同名漏报证据吻合，但该脚本本身只是按 Java 字段名检索 accessor。

prefix 条件调试器能解析 31 个 helper，并展示部分展开标志；这里只核查该辅助调用，不把“能展开名字”视为整个条件布尔语义等价。probe_port_form_families 报 pipe/cable 等字符串命中，不分注册、配方、测试和注释，仅适合定位代码。list_material_prefixes 报 107 条 helper 初始化，而前述字段扫描报 108，二者数的是不同语法结构，不能仅凭差一认定少注册。report_form_flag_materials 默认 STONES 得到 0；它不验证输入是合法生成标志，且按表键行计数，所以不能将默认空输出解读成没有石材形态。

本轮没有改动运行代码或正式资源，也没有把这些只读视图当作新 GameTest 通过记录。

## 硬币生成与多方块部件补丁核查（2026-09-20）

读取 add_coin_assets、add_valve_parts、audit_multiblock_parts；前两者仅在隔离目录执行，后者只读。结果见 weekly-audit-coin-valve-tools-2026-09-20.json。

硬币工具为 41 个现有材质图标目录各生成原版 COIN 图、透明 overlay 和共享 item 模型，加两份语言副本共 125 个文件；JSON 语义及 PNG 字节均与正式产物一致，来源 manifest 也一致。工具按磁盘已有材质目录枚举而非实际注册 MaterialTextureSet，因此此一致性不证明遗漏新材质目录时仍能覆盖所有材料。模型共用不证明第 35 项币堆放置后的颜色同步正常；它是资源生成检查，不是币堆 BER 验收。

阀门部件工具当前重跑不改变 LargeMachineParts，报告 already complete。WANTED 实际是 17001 到 18399 排除 171xx 的整数范围，不是原版真实注册 ID 列表；大段 missing ids 输出包含原版本来也没注册的编号，不能照这个列表补方块。其按名称去重还可能把不同旧 ID 合并为一个方块，需按实际功能核对，不能用“已存在名字”证明每个原版变体还原。

多方块审计工具读原版 Multiblock Machines 分类共 117 条，再只与 LargeMachineParts 的 89 条定义及 find switch 比 ID，打印覆盖 98、缺失 19。该分类混入 17000/171xx 主控，而移植的主控不一定列在部件表；当前 MultiblockDefinitions.java:45–48 明确注册蒸馏塔，:133 注册聚变主控，即两项确定假阳性。完整 19 项名单已存证，但不能把它直接当新缺失清单，也不能反过来声称所有主控功能已验收。应将主控、结构件与已映射旧 ID 分开进行实际注册和行为对照。

## 四个配方来源统计器与既有提取证据归档（2026-09-20）

读取并运行 census_gt6_recipe_map_sources、census_gt6_recipe_sites、count_gt6_recipe_map_usage、audit_recipe_sources，输出见 weekly-audit-recipe-census-tools-2026-09-20.json。全部是源码文字统计，不解析原版运行环境、循环展开、材料枚举或多分支配置，不把它们解释为独立运行配方数。

map_sources 包含 compat，对 Drying 报 28 个匹配，其中有旧版兼容来源；只显示出现的表，查询无匹配时不会输出显式 0。sites 排除 loaders/worldgen/compat 和以 MultiItem 等开头的文件，且包括 RM.java 中 helper 定义里的调用；它本轮报 26 个文件，不是全局未移植 26 个文件。map_usage 排除兼容，只统计 RM.X.method 的点调用，返回 Furnace 1 add/2 total 不能说明炉炼只有一条，其他注册助手和原版机制不在模式内。audit_recipe_sources 以移植源码是否出现原版文件名/stem 判 claimed，注释、测试或待办也能满足，不能作为该文件已完整移植的证明；此前 107 来源/62 未提及的边界不变。

为补全脚本索引，重新读取既有 weekly-audit-food-table-checks-2026-09-19.json：extract_gt6_bottles.py 的 --check 169 行/1 跳过，extract_gt6_drinks.py 272 行/73 跳过，extract_gt6_food_stats.py 20 原版+181 GT 食物统计共 201 行，extract_gt6_food_items.py 266 行（207 有 FoodStat、181 携带统计、59 展示）。四个记录退出码为 0。此处是把既有运行证据关联到具体工具名称，不是新运行提取器，也不证明跳过行均不需要移植；食品、容器、发酵逻辑问题已在前述章节单独列出。

同时确认既有 loot/enchant 隔离结果文件仍记录 GTEnchantmentTable 与 GTLootGen 重生成一致，材料隔离证据仍记录 1058 条及七份目录/门面输出一致。这些只证明对应生成产物，没有把生成一致改写成整体生存内容已完成。正式资源、代码和旧证据未覆盖重写。

## 八个报告视图的当前输出与解释范围（2026-09-20）

读取并运行 report_extruder_skips、report_gt6_books、report_gt6_loot_rows、report_gt6_only_prefixes、report_prefixless_delegates、report_tool_shaped_mirror、report_unparsed_prefix_conditions、report_wood_leaf_colours，输出见 weekly-audit-report-views-2026-09-20.json；全部是读取既有日志/表/PNG，没有启动新游戏或修改资源。

挤压跳过视图在现存门禁日志中统计 44 行（gearGt 22、gearGtSmall 22）；按行优先归类 out，再看输入和流体，不去重，也不自行核验原版是否生成该材料形态。不能由“missing item form”标签就认定每行均需补新物品。书籍视图重组 Java 字符串为 16 本并展示最长页长度，没有与 GT6 原文比较；第 100 项长页截断不会被其长度输出发现。转义解析仅适配当前生成器常用形式，非完整 Java 字符串解释器。

战利品视图读取 219 行/176 跳过的旧 JSON，skip reasons 是报告保留的样本，不代表全部跳过行的可移植性。tool_shaped 镜像视图读取历史运行报告共 1759 行，只做分类和未解析输入数统计；既不证明报告来自当前代码，也不覆盖 minecraft:crafting_shaped，因此无法发现第 125 项生成模块的镜像差异。

GT6-only 前缀视图当前读到 10 项，其说明中“122”已过时；这是比较表的命名差集，和“93 条条件未解析”不是同一个集合。prefixless 视图输出 434 个 register 名、108 个 item 字段、25 个 block 名/字段及 320 个无匹配代理名称，既未实际调用 delegate，也不处理所有独立注册方式。分类/语义名称可以无物品代理，不能由 320 推导缺失 320 类物品。unparsed 视图把 conditions 总数 215 标为 shared，实际包括未能解析的 93 项，标题不应被理解为 215 项均已在移植中实现。

叶色视图计算 PNG 中 alpha>8 像素的平均 RGB，并与脚本里的旧硬编码颜色并排打印；不读取当前枚举值或客户端颜色回调。平均贴图色与材质 tint 本就可能表达不同对象，不能按两列不相等认定染色错误。当前 Pillow 仅给 getdata 弃用提示，脚本完成，未据此报现有资源损坏。

## 传感器变体与补充树叶工具核查（2026-09-20）

完整读取 add_sensor_variants.py，--check 本轮返回 0，四个变体共 24 张贴图与 12 份模型/状态/item 文件保持一致，已有 8 个双语键。该模式逐文件比较内容而非只看存在，对当前生成范围是有效的一致性检查；但函数最终始终返回 0，若以后出现 would write/would copy 不能仅按退出码判通过。模型从 weightometric 模板替换材质目录，六向旋转在脚本固定定义；未执行客户端模型烘焙或传感器实际读数，不排除此前功能差异。未对正式文件写入。

完整读取 generate_missing_leaves_assets.py，--check 只查四种模型和贴图存在。本次另将四份模型与生成模板比较、完整解码现有 PNG，并在内存中按生成函数对照像素：blue_mahoe 与原版 LEAVES_BLUEMAHOE 一致；pine、ebony、white_mahoe 与基于 LEAVES_RUBBER 亮度和固定颜色的算法一致，均 16×16。后面三种是明确合成的替代素材，不可写成全部照搬原版。blue_mahoe 树叶存在也不能反证其树苗 PNG 缺失，它们是不同资源。

该树叶工具说明写“write what is missing”，实际非 check 模式会无条件保存全部四张 PNG 和四个模型，可能覆盖后续人工修正；check 又不会检测像素/模型内容回退。当前像素一致，没有据此新增现有纹理损坏结论。证据为 weekly-audit-sensor-leaf-tools-2026-09-20.json，本轮只生成审计 JSON，不生成或覆盖图片资源。

## 世界生成接入、坑轮廓和 ID 检索工具补查（2026-09-20）

读取 wire_black_sand_turf、wire_fluid_springs，并在独立副本执行；相关三份 Java 及九份配置 JSON 均没有内容差异，见 weekly-audit-worldgen-wiring-pit-2026-09-20.json。它们按字段/类名字符串判断 already wired，仍会重写配置；不是实际 RegistryObject、实体绑定和生物群系分布的验证，也不覆盖第 15/48 项流体泉行为差异。

读取 generate_pit_shape，当前原版 build/sources 的 WorldgenPit 与 src 原文逐字节一致。独立解析原版 SHAPE 得 48×48、2032 个内部格；GTPitShape 的 48 个字符串与原始行全部相同。代码把原版 SHAPE[x][z] 转成 ROWS[z].charAt(x)，但本例矩阵转置差异为 0，所以当前未发现由转置造成的形状错误，不应仅看下标写法就报 bug。该工具会整体重写 GTPitShape，本轮只读取和比较，没有运行正式生成入口；生成概率、地形条件仍需与 mask 一致性分开。

读取并运行 inventory_port_ids、search_gt6_statements，输出见 weekly-audit-id-statement-views-2026-09-20.json。前者读语言键而非注册表，筛 casing 返回 17 个名称，其中可以有前缀翻译模板，并不能保证每个都是可取出的物品 ID。后者通过括号计数拼接文本，直接删 // 后缀且未处理字符串/块注释里的括号，不是完整 Java 解析器；文档示例 --any 也未在 argparse 中实现。实际查询可以定位石头模块原版配方，有助于逐行查证，但不能凭匹配次数判断全量配方覆盖。

## 机器等级映射与参数视图核查（2026-09-20）

完整读取 gt6_machine_map、gt6_machine_params、gt6_part_ids，并检查 generate_machine_recipe_table 的等级枚举、lookup 回退和额外多方块行逻辑。实际从当前 BasicMachineDefinitions 得到 243 个单方块映射，对照 251 条原版提取记录无缺失，原版 (machine,tier) 索引无重复。9 个落到唯一行的回退分别是 Matter Fabricator T2–T5、Molecular Scanner T3、Matter Replicator T2–T5，均名称已含等级；不能把这些回退直接判成低等级配方混用。结果见 weekly-audit-machine-mapping-2026-09-20.json。

参数视图当前可读出蒸馏塔 HU 512/1–1024 和聚变 TU 8192/1–16384；这里是原版注册参数，不代表聚变端口应该输入 TU（当前实现另有启动 LU 与发电 EU 路径）。查询 Molecular Scanner 裸名返回 NOT FOUND 是匹配器要求完整显示名，与实际已定位 T3 注册不冲突。该视图按 aRegistry.add 到下次注册切片、数字字面量正则提取，会漏表达式且可能包含夹在两次注册间的代码，不能替代精确注册参数解析。

gt6_machine_map 类头称两种生成表共用映射、不会不一致，但配方生成器实际复制自己的映射常量和 tier_counts/lookup。本次五个主要映射字典相同，SKIP 只多出当前未被枚举的 laserengraver_alias，未造成当前配方遗漏。配方生成器另加 Coke Oven/Implosion Compressor 两行，所以 243 + 2 = 245，与此前配方表数量一致。维护上应统一来源并测试关键集合，不用文档承诺代替比较。

部件 ID 视图 --used 只统计缓存多方块配方中完全匹配 aRegistry.getItem(整数) 的 key 表达式，未解析变量、数量或运行时替代；打印的 xN 是 key 出现次数，不是配方需要 N 个部件。它和先前主控/部件误报工具均用于导航，不作为全部材料等级已实现的证明。本轮未执行任何正式代码生成入口。

### 133. 食品资源检查报告缺图却仍返回成功

完整读取 generate_food_item_assets.py：缺图时将行加入 missing_textures 并 continue，最终返回失败只检查 wrote_models 或 lang_added，没有检查 missing_textures。当前正式资源 --check 输出 266 贴图存在、0 模型/语言变化、exit 0；将 PORT_TEXTURES 仅在内存中指向不存在的临时目录再次 --check，输出 266 个 MISSING TEXTURE、textures present:0，仍 exit 0。没有删除或改动任何正式图片。证据见 weekly-audit-food-loot-assets-2026-09-20.json。门禁应把缺图纳入失败条件，并测试缺源图、缺目标图、空 PNG 与缺模型各自结果。

该工具对现有 PNG 只判断文件存在/非零长度，不比较原版图片或完整解码；模型存在时不解析引用。missing_models 列表没有实际 append，不能据没有 MISSING MODEL 输出排除错误模型。当前食品资源通过只是工具现有检查范围，先前全量解码/引用审查应作为另一份证据保留。补语言时两种语言都使用英文 display 和 Food 行，不读取标准中文；现有键保留，但若补缺键不能据“键齐全”声称完成中文。

## 战利品箱资源生成器补查（2026-09-20）

完整读取 generate_loot_chest_assets，在隔离目录运行，18 组 blockstate/item、共享模型、原版 PNG、双语副本及金属箱模板共 41 份文件与正式文件一致。说明中“17”是过时数量，代码 TABLES 实际 18。模型复用金属箱几何并替换 layer0 贴图，item 仍继承 metal_chest；本轮确认生成结果一致，不证明战利品箱专属物品渲染和开盖动画符合原版，也不反证第 69 项重放刷战利品问题。未运行客户端、未更新正式资源。

### 化工 token 诊断与别名工具的静态边界（2026-09-20）

本次完整读取 `tools/diagnose_chem_transpile.py`、`tools/derive_missing_tech_aliases.py`、`tools/audit_tech_item_tokens.py`，未执行会覆盖正式报告的入口。

- `diagnose_chem_transpile.py` 统计选定源文件经展开后的调用，并复用转换器解析；输出是解析覆盖率，不是配方运行验证。其独立 `reason()` 只是失败原因近似分类。
- `derive_missing_tech_aliases.py` 的文档声明支持 `--json`，但入口没有解析参数，始终写固定 `docs/tech-item-token-aliases.json`。未经重定向不要作为只读检查运行。其原版注册匹配未去注释，原版物品元数据仅少量专门映射，其余直接按旧名称构造 `minecraft:` 目标；命名空间目标没有经过实际注册表验证，结果只能作为人工核对候选。
- `audit_tech_item_tokens.py` 仅在指定 `--json` 时写文件。注册名来自文本匹配，包含任意注册器的 `.register()`，不能保证都是物品；带冒号的别名目标直接算已解析，也没有验证注册表实际存在。输出中的 recipes 是 token 出现次数，同一配方可含多个 token，不能当作独立配方数。

以上是维护工具的实际边界，不据此断言当前游戏中已存在相同数量的错误配方。旧矿石迁移依决定不保留；后续清理仍需先解除正常物品背景数据对旧实体常量的依赖。

### 134. 旧书架资源脚本会恢复已失效的贴图路径

完整读取并在独立副本执行 `tools/wire_book_shelf.py`：正式文件使用 `gregtech:block/iconsets/planks_wood`，脚本却覆盖为 `gregtech:block/wood/planks/oak`，后者对应 PNG 不存在。当前正式书架没有因此被改坏；这是再次执行脚本会带回紫黑贴图的已验证风险。其“已注册”判断只跳过 Java 注册，不跳过资源覆盖。

同批 `tools/register_spring_fluid_blocks.py` 依赖旧 `SPRING_FLUID_PATHS` 名称；当前已改为 `WORLD_FLUID_PATHS`，脚本误入旧接线分支并在写盘前因 registration anchor 断言退出，副本未改变。`tools/register_water_body_blocks.py` 生成的 9 份资源与当前正式文件相同，Java/语言副本也无变化。三个脚本均在模块顶层执行，不能将普通 import 当作只读检查。证据：`weekly-audit-fluid-bookshelf-wiring-2026-09-20.json`。

### 135. 材料前缀生成器会批量删除非前缀物品翻译

完整读取并隔离执行 `tools/generate_prefix_assets.py`：其语言过滤器删除所有非 `item.gregtech.tool_` 开头的 `item.gregtech.*` 键，以及多数创造分类，再只补回前缀列表。与正式语言文件比较，中英文各净删除 **2,539 键**，包含食物、检测器及其 tooltip；中文另有 **160 个值**被内置名称覆盖。这不是“补齐缺失前缀”的安全生成器，必须先改为仅更新自己负责的键，并保留标准中文来源。

模型方面 4,264 文件语义相同，另有 41 份 chemtube 模型是现有目录未提供的新增候选，不能把“不同”直接说成 41 个坏模型。证据：`weekly-audit-prefix-asset-regeneration-2026-09-20.json`。此次正式语言与模型均未修改。

### 136. 旧配方提示脚本会重复声明配方类别，另一个脚本会撤销提示修复

完整读取并隔离执行 `tools/port_recipe_hints.py`：脚本通过字符串 `gt.recipe.didyouknow` 判断是否已有类别；正式 `MachineRecipeMaps.java:111` 已有 `DidYouKnow`，沿用 GT6 的 `gt.recipe.other`，因此脚本又插入同名 `DidYouKnow` 字段。这会造成重复 Java 字段声明，不能重新运行于正式工作树。

同批 `tools/fix_recipe_hint_rows.py` 将现有锌流体 token 从 `m:Zn:144` 改回 `f:Zn:144`，并把黑色染料提示的 144 改回 1000。`GTGeneratedChem` 区分 materialFluid 与 named，两种 token 不能当作同义替换。副本差异已保存，未覆盖正式配方。证据：`weekly-audit-hints-material-report-tools-2026-09-20.json`。

## 能源、多方块与地牢资源生成复核（2026-09-20）

完整读取 `tools/generate_energy_node_assets.py`、`tools/restore_multiblock_appearance.py`、`tools/wire_dungeon_keys_and_portals.py`，分别重定向写入隔离目录，保留读取正式源纹理的路径。

- 多方块脚本处理 111 种定义（其中 74 项来自零件表），比较 652 份实际落盘文件全部一致；日志 1,128 次纹理/元数据复制包含重复目标，不能当作独立文件数。该工具的 `--check` 仅验证直接模型引用存在，不验证贴图、染色与运行时渲染。
- 能源脚本处理 42 种设备，282 份文件全部一致；只生成固定设备列表，不能证明所有 GT6 等级均已移植。缺失源目录时 `os.walk` 可静默不复制，缺少底层模型时只打印并跳过，不宜单靠退出码作为完整性门禁。
- 地牢脚本的 10 张钥匙原图、末地传送门染色图及元数据、18 份模型/状态、两种语言文件合计 32 份全部一致。资源包缺失只警告；其“1.20.1 模型无法对局部染色”的注释也不能作为技术限制（模型面支持 tintindex）。此次未宣称客户端透明排序、光照或开启动画已验证。

证据：`weekly-audit-energy-multiblock-portal-regeneration-2026-09-20.json`。

## 方块资源补齐与材料报告工具的边界（2026-09-20）

完整读取 `tools/generate_missing_block_assets.py` 与 `tools/audit_missing_models.py`。前者只修复缺少 blockstate 的条目；已有 blockstate 而模型/物品模型缺失不在其范围。其模板选择按名称前后缀，不能保证对应机器的真实形状；模型纹理和父级引用也不递归验证。后者的直接模型引用检查已在前文资源审计使用（49,057 文件、29 个待解释引用），不应把动态模型直接判成缺失；它不验证纹理内容。只有显式 `--fix-bom` 分支会写文件，本批未运行该分支。

完整读取并运行 `tools/audit_bedrock_ores.py`、`tools/report_block_condition_delta.py`、`tools/report_material_form_flags.py`，输出保存在 `weekly-audit-hints-material-report-tools-2026-09-20.json`。这些是过渡时期的文本推算：

- bedrock 工具只识别旧工厂/旧属性，打印 126 个候选、25 可放置/23 不可放置；当前 `MaterialPrefixes` 的 ore/oreSmall 同时使用 `MaterialForms.has(m, "ORES")`，所以不能拿 23 当作当前矿物缺口。其 MnCO3→Magnesium Carbonate 别名本身也错误，且有些 display name 被拿来和 Java 字段名比较。
- block condition 工具打印 1,063 材料、484 ORES/359 非 ore 工厂，这恰好说明只按工厂名估算缺口不成立；当前代码已使用这些 form flags。
- form flags 工具打印 48 项“不可达”，却仅比较名称、不比较当前按数字 ID 查询的路径，不能据此宣称 48 项没有材料形态。保持前文真实运行注册表与逐项材料 ID 审计的结论。

## 工具与石材配方比对器复核（2026-09-20）

完整读取 `tools/check_stone_extrusions.py`、`tools/check_masonry_rows.py`、`tools/check_tool_recipes.py` 及后者调用的 `tools/extract_gt6_tool_recipes.py`；三个检查入口均未指定写入正式报告参数。当前三者退出码均为 0，输出保存在 `weekly-audit-tool-stone-parity-checkers-2026-09-20.json`。

石材挤压的 60 条源表覆盖 STONE/COBBL 两组，验证 13 种物品形状、2 类模具、2 个方块输出及五个标记的匹配；它不验证每个材料运行时能解析出输出。砌筑检查依赖已有运行报告，记录 597 已注册、132 明确跳过、27 数据包配方，跳过项参与数量相等判断；其 crafting 比较主要是图案计数，机器比较主要是每岩种数量，不能称为所有输入、产量和运行行为逐项相同。工具检查比较 35 类、18 类工具头的 35 个图案，并验证早期图案与已有无序装配类型；其数据来自历史 ToolAssemblyTests 报告，未重新跑游戏，也不覆盖网络序列化（本报告第 0 项仍成立）。抽取器只支持当前源码表达方式，不是完整 Java 解析器；不要用通过这些检查来证明全部工具配方已正确。

既有生成器证据补充索引：`tools/generate_rope_dynamite_recipes.py` 与 `tools/generate_wooden_pipe_recipes.py` 的隔离重生成结果已在 `weekly-audit-small-recipe-generators-2026-09-20.json` 和第 124–125 项附近的说明中核对，仍保留木管生成后需镜像标记处理的限制，此处仅补全文件名索引，不重复运行。

### 137. 前缀条件审计跨越下一个声明，零缺失不等于条件一致

完整读取并隔离执行 `tools/compare_prefix_conditions.py`。其 `gt6_prefixes()` 从每个 create 声明向后搜索 6,000 字符的 setCondition，未以上一个声明的结束位置为界；当前源码中 **21 个前缀**取到了后续声明的条件，例：oreLightprismarine、oreDarkprismarine、oreKimberlite、oreQuartzite。即使某些前缀通过原版继承关系最终确实共享 ORES，也不能用这种跨声明扫描证明条件。

输出 225 个原版条件、215 个共有前缀，其中只解析出 122 个移植版条件，剩余 93 个不产生 missingFlags。该工具比较的是出现过哪些 flag，既不比较 And/Or/Not 的逻辑，也不完整递归原版前缀引用；“8 个 dead flags”仅表示生成表中未出现，不能证明整个原版运行过程中永远不添加这些标记。因此输出 0 个缺失 flag 不能作为材料形态完全一致的验收门禁。证据：`weekly-audit-prefix-condition-tool-2026-09-20.json`；正式比较报告未覆盖。

## 矿物、附魔与蜜蜂抽取工具复核（2026-09-20）

完整读取并核对七个工具：

- `tools/extract_gt6_stack_sizes.py` 仅记录字面量堆叠数；helper 分支只处理电线，管道 helper、循环展开、动态表达式不完整。分类计数中的电线不进入 rows，因此 rows 不能当作所有方块堆叠上限清单。
- `tools/extract_gt6_small_ores.py` 与 `tools/extract_gt6_large_ores.py` 重新只读执行。常开小矿参数表主世界/下界/末地分别 36/18/32 项、大矿 31/0/5 项，名称与比较到的数字参数没有差异。比较不含完整材料映射、指示物、实际地形分布；默认关闭项和其他模组条件项不在“常开”范围，默认关闭也不能称为绝对死代码。两脚本发现 mismatch 仍正常返回，不应只检查退出码。
- `tools/inventory_gt6_worldgen.py` 的 unclassified=0 是硬编码映射或源码文字出现的分类结果，不验证映射文件存在、注册调用或行为。末行 not ported on purpose=[] 来自筛选以 NOT PORTED 开头的理由，现有 SKIPPED 理由没有这个前缀，不能据此说没有主动省略项。
- `tools/extract_enchantment_table.py` 在模块顶层写文件；本次在独立工作目录运行，177 个材料、507 个附魔条目生成文件与正式文件逐字一致。原版运行时按组赋值、别名解析及真正应用到工具的行为仍需独立审计，第 83 项未因此解决。
- `tools/extract_gt6_comb_recipes.py` 隔离生成 20 行且与正式文件逐字相同。第 120 项指出的 U9→9 个小撮粉错误即来自生成器，不能靠重生成修复。未知表达式可仅丢弃一个输出而保留原 chance 数组，未来扩展需同时核验输出位置；当前不额外推断未复现的概率错位。
- `tools/extract_gt6_bumbles.py` 隔离生成 80 个物种、20 个蜂巢等级，输出与正式文件逐字相同；文件头声称 84 已过时。未知蜂巢等级只警告并跳过，生成器不负责遗传、气候、工作逻辑或 scanned 物品注册，不能替代这些行为的验证。

证据：`weekly-audit-worldgen-comb-enchant-extractors-2026-09-20.json`、`weekly-audit-bumble-species-regeneration-2026-09-20.json`。运行检查写入的产物仅在隔离目录与审计证据中。

### 138. 方块配方抽取器的默认分类匹配会把有效分类报成零条

完整读取 `tools/extract_gt6_block_recipes.py` 并按其文档运行：`Storage` 返回 0 rows，而 `Storage --contains` 返回 28 rows。原因是抽取到的 category 仍含 Java 字符串两侧双引号，却直接与无引号的命令行分类作相等比较。结果容易被误读为原版没有对应注册/配方，后续使用前应修正分类解析；本次未修改脚本。它显示注册语句与未展开的键，28 rows 也不是 28 条可直接搬运的完整合成配方。

## 注册与镜像清单、材料字段及模具生成复核（2026-09-20）

完整读取并隔离执行 `tools/census_gt6_recipe_mirror_flags.py`、`tools/census_gt6_registrations.py`、`tools/generate_shape_recipes.py`、`tools/extract_gt6_material_fields.py`，证据保存为 `weekly-audit-census-fields-shapes-2026-09-20.json`，未覆盖正式统计或生成文件。

镜像清单按源码调用统计，不展开循环；同一图案的 mirror 值用逻辑或合并，忽略具体原料与产物身份，不能直接据同图案给另一条配方开启镜像。解析长常量也是 token 集合展开而非完整按位表达式求值。第 125 项仍需按原版对应配方验证，不能只看图案清单。

注册清单只抽取字面量 aRegistry.add，不展开 helper/循环；它把 Large*/Controller 当作多方块机器，还用单方块 BasicMachineDefinitions 名称集合判断多方块缺失。这既会把线圈/主阀等列为机器，又会漏看独立多方块注册。因此清单应作为导航，而非把 missingMultiblockMachines 全部认定为真实缺口；量子充能器、长距离端点和物流核心仍按前文实际代码对照结论处理。

模具生成 14 份配方与正式 JSON 语义完全一致。脚本的 --check 仅检查文件是否存在，不比对内容；其注释说 CR.DEF_REV 会开启镜像不准确（REV 与 MIR 为独立位），当前 allow_mirror=false 本身并不因此判错。输出目录需预先存在，已有配方一律跳过，不能靠它修复已存在的错误内容。

材料字段表重生成与正式 Java 逐字相同；解析器主动不写 WITH_ID 的第二别名，只识别特定单行工厂格式，不能证明 MT 的全部别名均已收录。上文已核查的材料解析器后备路径与真实未解析条目仍适用。

## 标准中文导入入口复核（2026-09-20）

完整读取 `tools/import_gt_lang.py`，三个输入材料文件当前均存在；只在内存执行材料合并，匹配 1,067 个条目，规避 1 个遮蔽已有规范名的字段别名，对当前中英文文件的材料键均无新增或变化。证据：`weekly-audit-lang-import-merge-2026-09-20.json`。

其真正命令行入口在 main() 之后还调用 sync_standard_chinese.sync()；本批未运行该写入入口，内存合并结果也不代表整个语言流程无需改进。它依赖固定生成快照/两份附加表，不遍历所有运行时注册材料；局部名称只在含空格时用作英文显示名、缺文件仅跳过。因此全量语言验收仍以GregTech.lang 和真实注册键为准，不能用“成功合并 1,067 条”替代。

## 最后六个生成器的定向复核（2026-09-20）

完整读取并隔离运行 `tools/extract_basic_machine_recipes.py`、`tools/extract_gt6_form_flags.py`、`tools/generate_covers_usb_logistics_recipes.py`、`tools/generate_missing_component_recipes.py`、`tools/generate_multiblock_recipe_table.py`、`tools/transpile_gt6_loot.py`。证据：`weekly-audit-final-six-generators-2026-09-20.json`。

- 基础机器抽取出 251 条注册，多方块 117 条与输入快照 entries 相同。它支持引号内括号、顶层参数拆分，但不执行 Java 分支/循环；参数 regex 只识别字面量，parallelDuration 仅判断键出现，tier 从之前最近 aMat 赋值取得，因此不能用此抽取器证明所有动态参数完全正确。前文机器逐档映射核对仍是必要补充。
- 材料形态表的 Java 与 JSON 重生成均与正式文件相同。工厂按参数个数选择，而不是 Java 参数类型；声明后的 put 只抽取首个标记，复杂动态行为不完整。ID 映射优先于名称的实现已验证，`count` 会同时数 ID 键和名称键，不能当作独立材料数量。第 137 项对条件审计工具的限制没有因重生成一致而消失。
- 覆盖板/USB/物流脚本生成 71 条配方和 1 份 selector tag，与当前 72 份文件完全一致。此处只是脚本负责的集合，不是全部物流配方总数，更不证明物流核心与覆盖板行为已接线。
- 补缺组件脚本只生成指定高等级与 ULV 的 28 条，不是完整 84 条组件配方。22 条与正式文件完全一致；另 6 条泵/活塞配方只缺正式文件中的 allow_mirror=false 与 _mirror_flags（和木管同属生成后还需镜像后处理的问题）。脚本跳过已有文件，故不会直接撤销当前标记，但从空目录重建不能省略后处理。--check 只列缺失文件并正常返回，不验证内容。
- 多方块脚本重生成仍带回蒸馏塔、低温蒸馏塔、聚变的三个已移除单方块目标，证实第 105 项。结构 M 的无条件替换、IL 组件索引归约也不能用于所有新控制器；物流核心应遵循交接更正，保留原版所要求的机壳材料。
- 战利品 Java 与统计 JSON 重生成均相同；抽取器只接受字面量表名或 ChestGenHooks，跳过项包含本来可补齐的模组自身物品、动态书籍、逐材料循环，不能把 skip 等同于不需要移植。其元数据转换表只有有限条目，未识别值不能保证正确对应 1.20.1 物品；当前世界箱混合规则及反复开箱问题仍见第 67–69 项。

首次隔离运行对 form flags 的路径替换误用了 Path 类型，在输出提示处出现 TypeError；随即改为该脚本原有 str 类型并重跑，正式文件从未作为写入目标。最终证据仅采用完成的隔离运行。

## 审计范围收尾与删除文件复核（2026-09-20）

本次按可用时间戳范围的代码审计已收尾；完成的是改动核查、问题定位和后续推进准备，不是修复全部问题或完整游戏验收。时间戳不能证明作者，六月 HEAD 到现在的差异不能一律归属这一周。

候选 433 个 Java 已有定向源码/生成器核查记录，171 个 Python 均有明确章节证据索引；9742 JSON、20 mcmeta、686 PNG 已完成格式/完整解码与相应资源族的生成/引用核查。一个 NBT 空模板、两个文本输入、四份候选 MD 的相关生成来源及进度声明已交叉检查。资源格式正确不代表每个视觉效果正确，索引有引用也不代表脚本无缺陷，具体边界保留在各节。

根目录有本周时间戳的 build.gradle、PORTING_AUDIT.md 和旧 JVM 崩溃日志，已分别核对构建/资源路径、历史进度声明和崩溃阶段；更早的配置文件作为运行依赖读取，但不归为本周新增。审计自己的日志单列，未当作其他工作线的产物。

六月 HEAD 至当前另有 **36 个删除文件**，无删除时间戳可判断发生周次，已补查其替代路径：

- 15 个 data 缩写类已由可读命名模块承担；源码不存在对被删除类的旧 import。现有材料、前缀、配方表、标签、流体与物品引用模块仍需按报告修复各自的问题，不应恢复缩写类来掩盖问题。
- 旧独立 GTToolAssemblyRecipeSerializer 已由 GTToolRecipeSerializers.ASSEMBLY 接管，Loader_Tools 仍注册 tool_assembly。不是序列化器丢失；新实现的协议缺陷已经在第 0 项确认。
- 19 个旧工具 JSON 由 Loader_ToolCraftingRecipes 和 GTToolRecipes 的运行注册替代；因此“JSON 删除”不等于工具不可合成。但 ServerStarted-only 的 reload 丢失风险和客户端配方序列化问题仍须修复。
- atlas 从 assets/gregtech/atlases/blocks.json 转到 assets/minecraft/atlases/blocks.json；原有目录来源保留，void 改为现有 iconsets/void，并补 axle。没有据旧位置缺文件判为贴图删除。

相对最初 11059 项候选清单，当前没有候选文件丢失，也没有 src/tools 文件尺寸或 mtime 漂移。30 项文档/报告有时间变化，其中包括另一条工作线的历史门禁刷新与本次交接更正；不能把同尺寸自动报告视为内容逐字证明，也不能把其刷新当作本次新跑 GameTest。清单与差异存于 `weekly-audit-scope-closure-2026-09-20.json`。

### 完成验收与明确留给修复阶段的工作

要求的四项已分别落实：按时间筛选并补查范围外删除/根配置；核对代码、资源、生成来源与测试覆盖并记录确证问题；交叉检查他人交接的实现、规格、历史门禁及 jar 声明；在 AUDIT_NEXT_STEPS_2026-09-20.md 和 NEXT_GOAL.md 给出继续工作的优先级及验收路径。旧矿石迁移按决定不保留，不再作为移植阻塞项。

本次未修改游戏 Java/资源/测试，未 commit，未新打包，未启动客户端或重跑整套 GameTest。已执行的编译、Python 测试、引用/格式检查与隔离复现各按真实结果记录；未通过项目没有标绿。后续进入修复阶段先处理协议与丢失/复制问题，再处理 reload、能源/配方和生成器回退，最后继续物流核心 W1–W4。完整游戏兼容性和客户端视觉验证仍需在修复后执行。

## 下一步顺序与玩家复测（修复排序）

1. 先处理工具配方收发格式、四联/九联管配方、重载丢配方风险，再修 USB 手动插槽交互和两个库存方块的 capability 生命周期；增加真实协议往返、玩家操作、自动化缓存的回归测试。
2. 校正中文生成源；修复失效的测试前置及扫描误报，让测试确实检查内容而非旧数量。
3. 对本周配方、坩埚、材料映射逐链核验消耗/产出守恒、可取得的工具及机器前置，特别检查是否有生存配方循环依赖。当前只审了配方来源扫描机制，未完成逐配方核验。
4. 核查 53 个世界生成文件、能源机器、容器、客户端渲染改动；模型引用检查通过也仍需客户端验证染色和光照。
5. 纠正进度清单后再接入物流核心，避免在不可复现的“全绿”基础上扩展。

玩家可优先复测：源硬盘→目标硬盘；瓶架放药水/经验瓶并按指定位置取瓶；带自动化管道的库存方块破坏及重新放置；木轴世界/物品预览；新世界开局至坩埚、手动工具及第一台机器的完整制作链。本轮尚未完成这些游戏内测试。
