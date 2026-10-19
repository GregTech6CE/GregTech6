# 原版对齐与源码结构审计

当前审计开始于 2026-10-19，基线 `d4e5b7009`。这是进行中的全面核查，**尚未得出全部对齐的结论**。本页取代旧 `REMAINING_WORK.md` 的当前安排；旧交付与历史回执不作改写。

第二批以 `65cd233e4` 为基线：完成 30 个活动类名标准化、共享机器配方初始化清单及起火器标签入口归一；新增材料属性／凝固目标的原版对照。当前代码入口见 [源码导航](SOURCE_LAYOUT.md)。

第三批以 `618e31b8b` 为基线：复核工作属性的工厂继承与名称映射、七种处理目标的全部直接引用，以及挤压／研钵／压缩的具体过滤分支；新增 ALIGN-010–013。详见 [材料处理审计](MATERIAL_PROCESSING_AUDIT.md)。

第四批以 `758769b41` 为基线：索引原版通用处理器的 28 个配方组、478 个注册调用点，核查轧制／车床／切割分支并新增 ALIGN-014–016；规范两个共享材料生成器的来源参数、输出目录和只读比较模式。见 [前缀配方审计](PREFIX_HANDLER_AUDIT.md)。

## 范围与判定方式

原版来源为 `C:/Dev/gregtech6-master/gregtech6-master/src/main/java`，此次清点 1,229 个 Java 文件；当前 core 与 Stonecutter 的主源码／API 共 1,740 个 Java 文件。文件数量、同名类、注释提及原版或配方条数均不能证明行为等价。逐文件路径及摘要见 [源码范围清单](verification/alignment-source-inventory-20261019.json)，可用 `tools/integration/inventory_alignment_scope.py` 重建。

每项核查需追踪原版入口、当前入口、实际调用链、物品／材料身份、参数、保存状态和平台分支。区分：确认缺失、确认差异、现代版本适配、外部模组条件、已有实现但未运行验证、尚未核查。原版 1.7.10 专属兼容模块不能按“没有同名类”直接判为移植遗漏。

用户要求的是全面盘点和代码可读性改进。本轮不把它缩小成构建通过，也不擅自把所有行为差异批量改掉。测试以确有影响的定向验证为主。

## 已重新核对的现存差异

| 编号 | 判定与影响 | 原版证据 | 当前证据与下一步 |
| --- | --- | --- | --- |
| ALIGN-001 | 确认缺少小坩埚碰撞致死后的材料回收。现有烫伤不等于原版完整碰撞处理。 | `gregtech/tileentity/tools/MultiTileEntitySmeltery.java:622`：温度伤害致死后按实体种类加入肉、骨、铁、雪、火药等，带相应初始温度。 | 共享 `block/machine/SmeltingCrucibleBlock.entityInside` 两版本分支均只调用温度伤害；需另核查大坩埚对应规则，不能直接复用所有小坩埚数量。 |
| ALIGN-002 | 材料箭仅保留了部分行为，命中过程未对齐。 | `gregtech/entities/projectiles/EntityArrow_Material.java:171–241`：读取箭的材料及附魔，处理魔法伤害、火焰、击退、抢夺、特殊生物与命中回调。 | 共享 `entity/MaterialArrowEntity` 目前只接材料基础伤害、塑料速度、寿命、拾取和保存，命中沿用现代 `AbstractArrow`；`MaterialArrowRules` 无原版命中规则。现代弓自身附魔不能证明箭材料附魔等价，需逐条查物品赋魔和事件路径。 |
| ALIGN-003 | 确认存在未进入语言系统的提示。 | 原版提示／扫描来源需按具体行继续比对汉化补丁。 | `item/FuelRodItem:183–211` 直接拼接 Remaining、Self、Fuel、Tier、Durability 等；`item/behavior/BehaviorScanner:170–183` 拼接 Input、Output、Stored。不能把语言文件格式自检通过当作这些提示已经汉化。 |
| ALIGN-004 | 配置功能仍明显不完整；这也符合之前将 cfg 放到收尾的安排。 | 原版 `gregapi/config/Config`、世界生成调度与加载器具有多项配置读取。 | 共享 `GregTechConfig` 只定义日志与过压爆炸两项；`GTWorldgenDefaults.STONE_LAYERS` 为常量。需完整列出原配置的现代适用性；Nexus／roads 默认开启本身不是错误。 |
| ALIGN-005 | **结构问题已修复**，原先不是游戏缺标签。 | 前轮恢复的起火器配方集合应能被两版本消费。 | `EquipmentCraftingCatalog.TAGS` 已纳入起火器标签，NeoForge 由已有适配器读取共享内容，删除逐字节相同的版本资源副本。98 项目录由生成器重建并核对，源策略检查通过。 |
| ALIGN-006 | 确认缺少 BURNING 材料属性对应的点火加成。 | `Behavior_FlintAndTinder:51,88` 判断 `FLAMMABLE` 或 `BURNING`；`MT:1275` 的 Blaze 带 `BURNING`。 | `BehaviorFlintAndTinder.useOn` 只读取 `FLAMMABLE`；当前材料属性没有 BURNING 路径。范围是材料点火概率，不是所有打火机失效。 |
| ALIGN-007 | 确认工具的材料自动收集分支缺失。 | `MultiItemTool.canCollectDropsDirectly:217` 同时考虑工具种类、主材料和副材料 `AUTO_COLLECTING`；`MT:1272` 的 Basalz 有该属性。 | `GTToolHelper.canCollectMachineDrop` 只检查 `type.canCollect()` 和耐久；材料表未携带该属性。现有扳手直接收集能力仍然存在，不能判整类收集缺失。 |
| ALIGN-008 | 确认搅拌盆材料表面未按 GLOWING 自发光。 | `MultiTileEntityMixingBowl:407` 调用材料 `getTextureDust()`；`OreDictMaterial:1009` 将 GLOWING 传给纹理。 | `ProcessingToolRenderer` 材料面与覆盖面都使用环境 `light`，没有材料发光判断。流体亮度另有 `FluidCatalog`／`FluidVisualPolicy`，不能扩大为“所有发光流体缺失”。 |
| ALIGN-009 | 确认凝固目标只供手册展示，尚未接入坩埚冷却。 | `MT:1880–1881`：H2O→Ice、Lava→Obsidian；`MultiTileEntitySmeltery:276,343` 按 `mTargetSolidifying` 转换材料和数量。 | `CrucibleProcess` 冷却分支将同材料重新加入。实际 core 两个 1U 样本降过熔点后分别仍为 Water、Lava；量仍为 648648000，见 [材料审计回执](verification/material-trait-review-20261019.json)。 |
| ALIGN-010 | 确认工作属性表遗漏工厂继承，并混用字段名与材料名。影响部分材料的配方过滤和耗时。 | `MT:62,78,168–177,439,472,1056,1129`：食物工厂、低熔点金属工厂及显式属性。 | core 实际观察肉／奶酪／巧克力 FOOD、MORTAR 缺失，锡铅 FURNACE、MORTAR 缺失，盐／二氧化硅的显式属性未匹配。其他材料和最终配方全集未验证，见 [具体样本和调用路径](MATERIAL_PROCESSING_AUDIT.md)。 |
| ALIGN-011 | 确认通用挤压注册未应用原版材料条件／锻造目标，输入前缀矩阵也较窄。 | `Loader_Recipes_Handlers:741–838` 的前缀矩阵、EXTRUDER／EXTRUDER_SIMPLE／fullforge；`RecipeMapHandlerPrefixForging` 的输出目标。 | `PartRecipeLoader` 只遍历四个输入前缀，对有效形态同时注册普通和低温路径，保留原材料输出。Rubber／Plastic 有专用路径，不包含在此判断中；未枚举最终 Minecraft 配方。 |
| ALIGN-012 | 确认材料手册的处理目标遗漏复制继承。 | `OreDictMaterial.setAllToTheOutputOf:749–777`；`MT:1245–1246,1279–1280,1732`。 | 处理木／抛光木的目标、砖的禁用目标、烧结钨的压缩目标在侧表缺失。core 八次查询已记录；不能将展示错误直接推为所有对应机器错误。 |
| ALIGN-013 | 确认通用配方部分条件不同。 | `Loader_Recipes_Handlers:79` 的 rockGt 研钵行不要求 MORTAR；`:217–233` 的压缩行排除 COATED，部分还排除 LAYERED。 | `MortarGrindingRecipes` 把 rockGt 也放进 MORTAR 过滤；`MaterialFormConversionRecipes` 缺少上述涂层／层状条件。材料实际形态和最终可见配方仍待枚举。 |
| ALIGN-014 | 确认部分通用加工固定耗时替代了原版材料工作性分支，也遗漏部分过滤条件。 | `Loader_Recipes_Handlers:262–319,371–393`；`RecipeMapHandlerPrefix.getCosts`。 | `PartRecipeLoader` 的轧制、杆拉细丝、卷弯、箔／轨道与车床方法使用固定时长。困难材料品质 3、1U 锭→板，原版参数算得 1024 tick，当前传 16；具体范围不包含已有独立工作性计算的 Welder 和物理导线入口。 |
| ALIGN-015 | 确认车床／切割的部分材料余料未返回。 | 原版车床／切割行启用 pulverized remains，`RecipeMapHandlerPrefix:213–218` 按差额和粉碎目标返回。 | `PartRecipeLoader.roll`、`cut` 只输出主产物，如板→透镜、透镜→环、小板切割。锭／宝石→杆有专用余料但未按粉碎目标转换；不能扩大成所有加工都无副产物。 |
| ALIGN-016 | 确认通用材料切割的冷却液矩阵、时长和品质用量倍率不同。 | `Loader_Recipes_Handlers:628–652` 实际循环前四种冷却液；`RecipeMapHandlerPrefix:218` 按品质+1缩放。 | 当前 `cut` 只注册水、基础时长与 `max(16,duration)` 水量；品质 3 的长杆水路线，原版参数为 64 tick／256 mB，当前 16 tick／16 mB。其他显式切割配方未全部核查。 |
| ALIGN-017 | 确认转换器未接入原版运行环境风险检查。 | `TileEntityBase10EnergyConverter.onTick2` 每 600 tick 检查活动设备；Root 与 Covers 按能量类型、侧面、覆盖板和配置处理火／水／雨／雷。 | `EnergyNodeBlockEntity.tickOriginalConverter` 只保留不运行时衰减过载次数；易损集合存在但未消费。电变压器 emitElectricTransformer 及电池盒 serverTick 同样缺少活动环境分支，见 [电变压器核查](ELECTRIC_TRANSFORMER_AUDIT.md)。范围及概率见 [能源核查](ENERGY_CONVERTER_AUDIT.md)，不能推为所有蒸汽设备都应遇水损坏。 |
| ALIGN-018 | 确认 Flux Motor／Dynamo 各缺少后三档，共六个设备。 | `Loader_MultiTileEntities:858–860,955–957` 注册 11023–11025、11113–11115，无外部模组条件。 | 两版本共用的 `EnergyNodeDefinitions` 各只有 LV／MV。五类原版目录 35 行与当前 core 29 项比对，现有项额定值匹配；缺项需连同模型／配方／材料与语言补齐。 |
| ALIGN-019 | 确认小锅炉工具交互差异。 | 原版 BoilerTank 的排空器先水后蒸汽，温度计读取储存热量。 | 当前先蒸汽后水、固定排空消耗，并缺少锅炉温度计分支；Jade 不能代替物品交互。 |
| ALIGN-020 | 确认小锅炉缺少外部爆炸触发的高压连锁处理。 | 原版 BoilerTank.onExploded 在表值 >4 时再次爆炸。 | 当前只有自身过热／满罐及玩家拆除分支；抑制高压掉落不是爆炸回调。 |
| ALIGN-021 | 确认小锅炉遗漏矿泉水接收。 | 原版 FL.WATER 包含 potion.mineralwater。 | 当前 isWater 的固定路径集合漏掉该项，而共享流体表已有 MnWtr／WATER。 |
| ALIGN-022 | 确认液体／气体燃烧室补燃和余火逻辑不完整。 | 原版 GeneratorLiquid 按 burning 或 cooldown 运行，循环补到两倍额定输出，每次补燃重置余火。 | 当前只补一份、只检查 burning；致密殷钢 64 HU/t 与氢气每份 32 HU 的实际参数会进入补不足即熄火路径。余火烫伤也缺失。 |
| ALIGN-023 | 确认液体／气体燃烧室的罐接口缺少原版过滤和排出限制。 | 原版按燃料配方及液／气类型接收，普通管路在燃烧或余火时不能抽出。 | 当前直接暴露无过滤 FluidTankGT；任意液体能填入不等于任意液体能发电。 |
| ALIGN-024 | 确认气体燃烧室没有接入邻接／手动关停接口。 | 原版 Gas 实现关闭即熄火清余火，开启不自动点火。 | 当前设备及父类不实现已存在的 AdjacentEnergyControl／MachineControl Provider。 |
| ALIGN-025 | 确认液体／气体燃烧室缺少原版周边点火和前方燃烧分支。 | 原版 GeneratorLiquid:123,131–133 按效率点火、烧前方并检查碰撞／液体／氧气。 | 当前 FLAME_RANGE 无消费；前方只判空气或可替换。跨模组氧气适配需另核。 |
| ALIGN-026 | 确认流化床内置燃料注册缺失。 | Loader_Fuels:37–43 对 11 种材料、5 种粉末形态尝试注册 55 行。 | 当前 FuelRecipeLoader／OriginalFuelRecipeRows 只有 engine、burn、gas、hot，普通 FluidBed 表没有对应内置写入。外部数据包不包含在此结论中。 |
| ALIGN-027 | 确认流化床灰渣阻塞和配方扣料合同不同。 | GeneratorFluidBed:131–140 在待输出灰渣清空后才消费配方。 | 当前无参数 canStoreAsh 恒真，同种暂存持续增长、异种新灰渣未入队；物品固定扣 1。原版内置行都是单件，数量问题仅针对多件扩展行；内置燃料缺失目前会遮蔽该路径。 |
| ALIGN-028 | 确认流化床罐容量及自动化过滤不同。 | 原版罐为 max(配方表最大输入,rate)，表默认下限 1000 L；仅燃料插入／灰渣取出，普通流体排出受燃烧状态限制。 | 当前罐为 rate×10，如铅只有 160 L，不能装 648 L 粉块配方；非正面直接暴露未过滤两槽和原始流体罐。 |
| ALIGN-029 | 确认流化床补燃失败会提前停用已有余热。 | 原版只在剩余热量小于额定值时熄火，固体／流化床每 tick 也只补一份。 | 当前共享 tick 用补燃返回值覆盖 burning；16 HU/t、40 HU 开始的例子在输出后剩 24 HU 时就熄火。不要给流化床套用液体循环补燃要求。 |
| ALIGN-030 | 确认固体燃烧灰渣缓冲、计量与原版不同。 | GeneratorSolid 允许一份暂存；RecipeMapFurnaceFuel:54–88 分容器／材料路径，含矿物倍率、材料汇总与批量粉末计量。 | 当前槽满提前拒绝加料，burnOne 固定一件且同时计算容器／主材料灰渣；灰渣统一小撮且限制 1–64。具体材料全集和外部燃料继续核查。 |
| ALIGN-031 | 确认大型锅炉结构失效后的运行／危险检查顺序不同。 | 原版 LargeBoiler 仍煮水、排汽、更新表值，再检查结构与过热；区块缺失保留旧形成状态。 | 当前未形成时冷却后提前返回，跳过后续过热／满罐检查，并用旧表值判断爆炸；主控直连接口额外受结构门槛限制。 |
| ALIGN-032 | 确认大型锅炉多出口分配不同。 | 原版先模拟接受量，优先处理低于平均份额的小接收端。 | 当前顺序分配再补余量；100 L、三端容量 100／1／100 的源码例子原版 49／1／50，当前 33／1／66，总量均 100，不是据此判丢汽。 |
| ALIGN-033 | 确认大型锅炉缺少外部爆炸连锁。 | 原版 onExploded 在表值 >4 时引爆，威力随汽量变化。 | 当前玩家拆机、自身爆炸与高压不掉落存在，缺外部爆炸回调。 |
| ALIGN-034 | 确认大型锅炉工具损耗和除垢伤害入口不同。 | 原版排空返回实际量，温度计返回工具消耗，除垢调用热伤害及热防护判断。 | 当前排空固定 100，温度计分支只显示信息；除垢直接用 inFire，未经过现有 GTEntityHelper 热防护入口。 |
| ALIGN-035 | 确认精金大型锅炉压力传感器最大容量被截断。 | 原版 getGibblMax 返回长整数容量。 | 当前 gibblMaximum 调用 int getCapacity：2,621,440,000 变成 2,147,483,647；内部储罐和表针长整数计算未因此缩水。 |
| ALIGN-036 | 确认焦炉不完整结构的部件绑定策略不同。 | 原版 CokeOven 逐格检查并认领正确部件，缺区块保留旧形成状态。 | 当前共用 LargeRecipeMachine 首次遇错即清空已绑定部件，仅完整候选才整体绑定；部件工具／传感器转发也依赖这份主控引用。 |
| ALIGN-037 | 确认焦炉自动排液时机、速率和目标优先级不同。 | 原版在 doWork 前排液，默认最大搬运量；九格目标按世界 X/Z 遍历。 | 当前停机／未形成提前返回，普通 autoIO 单次 1000 L；目标顺序随朝向旋转。外部管道主动抽取不包含在此结论中。 |
| ALIGN-038 | 确认焦炉缺少原版流体积压停产阈值。 | BasicMachine.canOutput 对同种已有液体检查 max(16000,1+单次输出×并行上限)，有指定豁免。 | 当前 fluidsFit 只看剩余容量，焦炉输出罐为 Long.MAX_VALUE；500 L／煤、16 并行的原版阈值为 16000 L。不是应把储罐容量硬改成 16000。 |
| ALIGN-039 | 确认焦炉点火窗口最后一刻的启动许可不同。 | 原版 doWork 末尾递减，剩余 1 tick 时仍可开新配方。 | 当前 beforeMachineTick 先减为 0，再检查启动；已有进行中任务不因此取消。部分输出刷新的差异另待原生状态验证。 |

| ALIGN-040 | 确认共用机器完成后丢弃剩余工作量。 | 原版 doActive 全部输出后减去本次成本，下个有效任务沿用余量；堵塞或找不到新任务另有清零条件。 | 当前 advance 截断到 total，完成和启动下项均清零；90/100 加 32 的 core 观察为 100，原版完成后按源码应剩 22。 |
| ALIGN-041 | 确认物品产物的固定输出槽位丢失。 | 原版 canOutput/getOutputs/doActive 保留产物下标，包括未抽中的空位。 | 当前 roll 压缩成连续堆列表，itemsFit/flush 寻找任意匹配或空槽；磁铁矿砂离心的第二概率产物可移到第一空槽。 |
| ALIGN-042 | 确认基础配方机螺丝刀四模式未接入。 | 原版 bit 0 要求空输出、bit 1 仅向空输入槽补料，模式持久化。 | 当前螺丝刀配置覆盖板过滤器，基础机器库存／输出路径没有对应模式；Recipe.mNeedsEmptyOutput 已有部分物品槽检查，不算完全缺失。 |
| ALIGN-043 | 确认流体输出预判未预留空罐身份。 | 原版先数可用空罐和所需不同产物罐。 | 当前 fluidsFit 只扣容量，第二种液体仍把同一罐视为空；UF6 两产物只剩一空罐的条件例子会提前开工，实际 flush 保留塞不下的待产物，不据此判丢失。 |

| ALIGN-044 | 确认基础机器缺少软锤取消加工入口。 | 原版 onToolClick2 清进度、成本、启动能量和待产物，不返还已扣输入。 | 当前软锤只配置覆盖板，基础机私有任务状态没有对应取消方法；不等同于暂停。 |
| ALIGN-045 | 确认恒定供能机器的关停／结构失效绕过重置。 | 原版超过初始 40 tick 后在 inactive 按配置／设备豁免清进度。 | 当前提前返回保留进度，只在普通缺电分支重置；大型离心机需重置，大型破碎机豁免，不能一概而论。 |
| ALIGN-046 | 确认 KU 加工完成缺少正转负门槛。 | 原版 TD 仅 KU 属交变能源，BasicMachine 保存注入符号，完成要求旧正新负。 | 当前活塞会发 ±KU，但基础机器只累计绝对值且达到工作量就输出；目录五类共 20 个 KU 项，未逐台游戏测试。 |
| ALIGN-047 | 确认接收整包时截掉缓存超额能量。 | 原版向上取整接收包数并累加整包，可短暂超过额定缓存，tick 再扣最大输入。 | 当前 accepted 向上取整、add 却封顶；60/64 再接一包 32，原版应 92，当前实际 core 为 64。与任务完成余量 ALIGN-040 分开。 |

| ALIGN-048 | 确认战利品袋／落灰书缺少机器动态拆包。 | RecipeMapUnboxinator 按 Behavior_Drop_Loot 各表抽取，要求空输出且不缓存。 | 当前只有 fake 展示行，未绑定对应内置 provider；右键开袋、材料箱等静态拆包已存在，不能扩大为整个拆包机缺失。 |
| ALIGN-049 | 确认 82 张表的 mNeedsOutputs 被关闭。 | 原版 85 对同名目录中 82 个要求配方有输出，另外三个所核标志一致。 | 当前 85 表此标志全 false，绕过无输出配方拒绝，且基础机的 mNeedsEmptyOutput 检查错误地受其控制；手作有独立检查。 |
| ALIGN-050 | 确认 Bath 通用动态分支未接入。 | 原版按装备色彩／弹药附魔／食物效果数据生成配方，手作也走 findRecipe。 | 当前指定木材、纺织、帆布静态处理存在；无对应动态 provider，手作只遍历静态表。外部 Atum 等另列兼容范围。 |
| ALIGN-051 | 确认链式并行缺少总工作量准入上限。 | 原版 parallelDuration 批次受 abs(EUt×duration)×批数 <= 最大输入×600 限制，至少一份。 | 当前只限制数量／功率／空间。大型离心机 512×2304 钴矿样例原版限 2；8 输入且输出为空时当前可批 8，原生解析／游戏未验。 |
| ALIGN-052 | 确认聚变开工未检查输出准入。 | 原版继承 checkRecipe→canOutput，在扣料前检查槽、空输出和流体积压。 | 当前专用 startRecipe 直接扣料／抽产物，最后才 flush；可以先发电再被不相容产物堵住。待产物保留，不声称丢失。 |
| ALIGN-053 | 确认聚变停机仍能接收待充能 LU。 | 原版部件转发进入主控停止门槛，mStopped 时拒绝。 | 当前部件和 injectPortEnergy 只查结构／类型／大小／欠充，没有 enabled 检查；不涉及空闲预充。 |
| ALIGN-054 | 确认聚变状态／进度与控制及覆盖板路径脱节。 | 原版加工和状态／进度接口读取同一套基础机字段。 | 当前专用 tick 更新自己的字段，继承 MachineControl 仍读父类私有字段，且跳过常规覆盖板 tick／信号刷新。外观更新和两个显示方法已存在。 |
| ALIGN-055 | 确认内爆开工缺少原版爆炸音效。 | 原版成功启动后调用 onProcessStarted→MC_EXPLODE。 | 当前内爆及基础加工链没有对应声音分派；这是音效缺失，不是缺少爆炸破坏。 |
| ALIGN-056 | 确认聚变已有任务恢复后，同配方下一批重复要求启动 LU。 | 原版恢复任务完成后 active 为 true；充能条件允许 currentRecipe 为 null，连续同配方不重充。 | 当前 load 或暂停 tick 清空 continuousRecipe，保留任务完成后立即 startRecipe，因标记为空重新收取全额；不是当前任务丢失，尚未游戏重启实测。 |
| ALIGN-057 | 确认物质制造漏读已有流体材料旁表。 | 原版材料相态对象直接供元素分离遍历。 | 当前 Hydrogen／Deuterium／Tritium／Helium／Nitrogen／Oxygen／Neon／Argon 的旁表关联已存在，但加载器仅读 materialKey，八项被跳过；不是流体未注册。 |
| ALIGN-058 | 确认普通汞液体物质分离的单位量偏大。 | 原版显式 mercury 绑定 L=144 mB/U，再用 liquid(U) 生成配方。 | 当前普通 mercury 按非 molten 名称取 1000 mB，同样输出 80 带电＋120 中性；独立 molten.mercury 的 144 不能替代普通汞。 |
| ALIGN-059 | 确认电变压器最后整包接收采用向下取整。 | 原版 Energy_Stats 在缓存未满时向上取整整包，60/64 接收 32 后为 92。 | 当前 EnergyPackets.fitting 以剩余 4/32 得 0，拒收；不同于 ALIGN-047 接收后截断，见 [报告](ELECTRIC_TRANSFORMER_AUDIT.md)。 |
| ALIGN-060 | 确认电变压器拒收幅值合法的负号 EU 包。 | 原版 Root 按绝对值过门限，Energy_Stats 按绝对值储存。 | 当前门限允许 -32，但随后非 RU 的 magnitude 得 0；限于接口边界，未声称普通供电必然触发。 |
| ALIGN-061 | 确认电池盒低缓存输出有额外的防透支限制。 | 原版有一包缓存就可按供能槽数量输出，四槽／32 缓存可在全接收后到 -96。 | 当前 offered 受 buffer/voltage 限制，最多一包且不变负；这是保护性差异，本轮不撤销，见 [储能核查](BATTERY_STORAGE_AUDIT.md)。 |
| ALIGN-062 | 确认十二种 Energium 宝石缺少原版 LU 储能行为。 | 原版红／青各六档 LU 电池注册为对应宝石的统一目标，有电量和充放电。 | 当前十二个 gem 形态均走普通 MaterialItem，两版没有转到 IItemEnergy／可摆放电池；EU 化学电池不是替代。 |
| ALIGN-063 | 确认二十种 LU 水晶充能器缺少实际设备实现。 | 原版十档普通／大型，库存 4／16，继承能源电池盒。 | 当前只有分类名称，能源目录无 LU 储能、原生电池适配固定 EU；激光转换器无水晶库存，见 [报告](SPECIAL_STORAGE_AUDIT.md)。 |
| ALIGN-064 | 确认两套便携电源共四种身份及燃料循环缺失。 | 原版氢／氦-3 灌装满电、不可充电、耗尽回空容器。 | 当前只有分类及未注册旧模型，既无专用能源物品也无耗尽替换链；不能用化学电池替代。 |
| ALIGN-065 | 确认 ZPM 放能器电容与进度合同不同。 | 原版物品电量／容量与工作缓存进度分开，空槽容量为零。 | 当前 stored 加缓存、capacity 恒为神器容量加缓存上限，MachineControl 无 progress；不是生成额外能量。 |
| ALIGN-066 | 确认 ZPM 发光档位偏低。 | 原版 UT.scale 保证任意正能量至少 1，半满为 8。 | 当前线性整数除法在能量 1 时为 0、半满为 7；空／满端点一致不能代表整个范围一致。 |
| ALIGN-067 | 确认旋转变速器停止后的输入／缓存行为相反。 | 原版停止阻止新输入，仍处理和耗损缓存。 | 当前仅冻结 emitOutput，输入面分支仍接收，见 [报告](ROTATION_TRANSFORMER_AUDIT.md)。 |
| ALIGN-068 | 确认旋转变速器没有输入过载处理，输出超限也无对应分支。 | 原版 Energy_Stats／Converter 标记过载，父类累计保护后 overcharge。 | 青铜 65 RU 包被 fitting 拒收而无过载；输出超限常规可达性未证明，见 [报告](ROTATION_TRANSFORMER_AUDIT.md)。 |
| ALIGN-069 | 确认旋转变速器缺少原版模式限速。 | 父类 mode 控制输出上限及固定耗损。 | 当前 supportsMode=false、无 mode 消费，反向切换不能替代限速模式，见 [报告](ROTATION_TRANSFORMER_AUDIT.md)。 |
| ALIGN-070 | 确认长距离线路重载后变化观察表未重建。 | 原版线路变化触发机器回调并清空目标坐标。 | 当前恢复目标却 watched.clear，后续拓扑版本变化对空表无效；同模式影响物品／流体端点。源码推导，未世界复现，见 [报告](LONG_DISTANCE_ENERGY_AUDIT.md)。 |
| ALIGN-071 | 确认长距离输出绕过通用能源适配。 | 原版经网络工具进入 EnergyCompat。 | 当前直接 instanceof IEnergyBlock，未用现有 EnergyBlockViews／FE 桥；公开附属 API 或 FE-only 邻居被排除，见 [报告](LONG_DISTANCE_ENERGY_AUDIT.md)。 |
| ALIGN-072 | 确认长距离端点超压简化为无掉落删块。 | 原版 overcharge 按配置处理破坏并发送过载声音／日志。 | 当前发送／接收端超限均 destroyBlock(false)，不能视为相同过载后果；配置收尾仍延后，见 [报告](LONG_DISTANCE_ENERGY_AUDIT.md)。 |
| ALIGN-073 | 确认长距离物品端点允许原版侧面合同禁止的自动化抽取。 | 原版直接 decrStackSize 委托，但 canExtractItem2=false，覆盖板父类仍强制该许可。 | 当前共用 ItemRelay.extractItem 无端点抽取限制；旧 smoke 把此行为误称原版对应，已纠正归因，玩法待修。见 [报告](PIPELINE_RELAY_AUDIT.md)。 |
| ALIGN-074 | 确认可调齿轮箱余量输出失去当 tick 输入触发要求。 | 原版 mUsedGear 门控输出，并以 mIgnorePower 控制模拟／实际回压。 | 当前只要旧缓存非空便继续尝试，模拟仍返回提供量；停供后行为不同，见 [报告](AXLE_GEARBOX_AUDIT.md)。 |
| ALIGN-075 | 确认可调齿轮箱输出轮次、份额和优先序不同。 | 原版有进展就继续，按 power/3 分配且每轮推进顺序。 | 当前最多三轮、按剩余输出面分配、tick 末推进一次；限量接收器吞吐可能不同，见 [报告](AXLE_GEARBOX_AUDIT.md)。 |
| ALIGN-076 | 确认可调齿轮箱缺少通用开关／运行状态接口。 | 原版实现 SwitchableOnOff／RunningActively。 | 当前只实现 IEnergyBlock，MachineControl.find 返回 null；软锤手动停启仍存在，见 [报告](AXLE_GEARBOX_AUDIT.md)。 |
| ALIGN-077 | 确认导线、物品管道、流体管道及普通桶未接入原版进度计合同。 | 原版分别返回安培、搬运次数乘 64、传输液量、发酵时间及对应上限。 | 当前缺少 ProgressSensorSource／MachineControl.Provider；功率、液量和压力读取不能替代，见 [导线](ELECTRIC_WIRE_AUDIT.md)、[物品管道](ITEM_PIPE_AUDIT.md)、[流体管道](FLUID_PIPE_AUDIT.md)、[普通桶](BARREL_AUDIT.md)。 |
| ALIGN-078 | 确认导线缺少建筑泡沫包覆与硬化绝缘。 | 原版连接器支持湿／干泡沫，导线硬化后免除接触电击。 | 当前导线没有包覆状态；硬化剂／去除剂只识别独立泡沫方块，见 [报告](ELECTRIC_WIRE_AUDIT.md)。 |
| ALIGN-079 | 确认物品管道库存槽数被当作单槽容量。 | 原版 NBT_INV_SIZE 创建多槽库存，每槽正常上限 64。 | 当前固定一个槽、槽上限 spec.invSize，涉及缓存种类及存档结构，见 [报告](ITEM_PIPE_AUDIT.md)。 |
| ALIGN-080 | 确认物品管道吞吐限流缺失且计数单位不同。 | 原版每次成功搬运计一堆，扫描／发送检查每秒容量。 | 当前按物品数累加，仅保存和显示，无容量准入，见 [报告](ITEM_PIPE_AUDIT.md)。 |
| ALIGN-081 | 确认物品管道改成逐段缓存转交。 | 原版每十 tick 扫描后从源库存直接交付出口。 | 当前每四 tick 向 firstHop 库存转交，堵塞／时序不同，见 [报告](ITEM_PIPE_AUDIT.md)。 |
| ALIGN-082 | 确认物品管道侧面能力未检查连接及既有入口锁定。 | 原版 connected、固定接收面、空槽准入；外部抽取也要求连接。 | 当前侧面包装只查禁入／禁出，插入成功覆盖入口；内部路由检查不能代替，见 [报告](ITEM_PIPE_AUDIT.md)。 |
| ALIGN-083 | 确认流体管道遗漏材料接触伤害开关。 | 原版 16 组材料／112 规格免接触温度伤害。 | 当前规格无 contactDamage，碰撞对所有管道调用温度伤害，见 [报告](FLUID_PIPE_AUDIT.md)。 |
| ALIGN-084 | 确认流体管道方块硬度／橡胶抗爆参数不对应。 | 原版 blocking 真为 2／6，橡胶假为 1／2。 | 当前 blocking 未传递，两版固定硬度 1.5／抗爆 6；快速阻挡语义需续核，见 [报告](FLUID_PIPE_AUDIT.md)。 |
| ALIGN-085 | 确认流体管道温度计接口缺失。 | 原版温度计读取管道 Temperature 合同。 | 当前温度计只认坩埚；管道已实现 CompressionSensorSource，压力计和普通液量读取仍有，见 [报告](FLUID_PIPE_AUDIT.md)。 |
| ALIGN-086 | 确认普通桶封口未隔离流体。 | 原版封口禁普通进出液，发酵与自动排液互斥。 | 当前 fill/drain 无封口检查，tick 可先排液再发酵；空桶软锤位操作也不同，见 [报告](BARREL_AUDIT.md)。 |
| ALIGN-087 | 确认桶自动排液方向／限量与能力不同。 | 原版按气体／浮力分垂直、上、下，无额外 1000 限量。 | 当前轻流体向六面、每面最多 1000，并隐藏选定输出面整个 capability，见 [报告](BARREL_AUDIT.md)。 |
| ALIGN-088 | 确认桶发酵加载后进度清零。 | 原版重查有效配方保留已保存进度，查配方 tick 不递增。 | 当前查配方清零两种时间且同 tick 推进；正常新发酵也提前一 tick，见 [报告](BARREL_AUDIT.md)。 |
| ALIGN-089 | 确认桶世界／物品准入混用并缺少部分限制。 | 原版放置桶接受后在 tick 处理危险，便携物品先拒绝。 | 当前世界桶预拒绝热／气／酸／等离子，遗漏魔法、导能及木桶 SIMPLE 消费，见 [报告](BARREL_AUDIT.md)。 |
| ALIGN-090 | 确认桶过热后果丢失内容物分支。 | 原版至少 1000 mB 熔岩才放熔岩，否则放火并 burn。 | 两版 helper 总放 level 4 熔岩并造成 4 点 heat；正常 fill 已拦超温，触发条件另述，见 [报告](BARREL_AUDIT.md)。 |
| ALIGN-091 | 确认发酵产物数量被额外钳制。 | 原版缩放后 setFluid 不按罐容量截断。 | 当前至少 1、至多容量；core 观察 0／32000 边界，实际配方可达性未全验，见 [报告](BARREL_AUDIT.md)。 |
| ALIGN-092 | 确认物流桶空桶软锤无法清除过滤。 | 原版父类先处理空桶清身份／自动输出，非空才检查 canBeSealed。 | 当前整个 toggleSoftHammerState 为空；保留过滤本身正常，见 [报告](LOGISTICS_TANK_AUDIT.md)。 |
| ALIGN-093 | 确认共享储液容量查询／缩容改变原版合同。 | 原版缩容不改已有量，无可调表时报告名义容量。 | 当前缩容截量，查询额外以已有量作下限；部分多方块已有补偿，见 [报告](LOGISTICS_TANK_AUDIT.md)。 |
| ALIGN-094 | 确认溢出销毁的接口分支不完整。 | 原版 add／fill 启用后报告消耗整个同种输入。 | 当前 add 仅满罐报告全部，原生 fill 未用标志；未发现启用业务调用，见 [报告](LOGISTICS_TANK_AUDIT.md)。 |
| ALIGN-095 | 确认可调容量键混用，空罐首次填充未按输入流体查表。 | 原版配方／罐同用 Fluid 名称，空罐按输入查询。 | 当前罐用 type ID，三种世界水查成 minecraft:water；普通静态流体多为同 ID，不一概判失效，见 [报告](ADAPTIVE_FLUID_AND_ITEM_AUDIT.md)。 |
| ALIGN-096 | 确认基础配方机默认罐容量和倍率不同。 | 原版输入默认 1000／可覆盖、配方容量乘 parallel×2，输出 Long.MAX_VALUE。 | 当前输入 8000／倍率 1，基础输出 8000；独立输出覆写另列，不等同原版积压限制，见 [报告](ADAPTIVE_FLUID_AND_ITEM_AUDIT.md)。 |
| ALIGN-097 | 确认普通桶与物流桶物品缺少容器能力。 | 原版 MultiTileEntityItemInternal 转发桶的物品 fill／drain 并更新 NBT。 | 当前只有存液掉落数据和提示／堆叠限制，没有本模组提供的物品流体 handler；量杯等不在此缺口内，见 [报告](ADAPTIVE_FLUID_AND_ITEM_AUDIT.md)。 |
| ALIGN-098 | 确认泵每源罐只尝试一次，不重复使用余下预算。 | 原版 CoverPump 在有进展且有预算时循环 FL.move。 | 当前 for 每个 tank 一次；单次调用限额的接收器可能低于额定吞吐，未原生复现，见 [报告](COMPONENT_DRAIN_COVER_AUDIT.md)。 |
| ALIGN-099 | 确认排水口世界取液范围收窄。 | 原版河湖／绿洲名单、量子份额及通用流体 drain 回调。 | 当前无限水只检查河流标签，普通路径限定 LiquidBlock 源；三种 GT 水独立批量路径已有，见 [报告](COMPONENT_DRAIN_COVER_AUDIT.md)。 |
| ALIGN-100 | 确认排水口缺少方块更新即时尝试，且宿主时钟不同。 | 原版 receivedBlockUpdate 或全局相位。 | 当前只周期调用，基础机／管道本地计数、其他宿主世界计时；周期相同不是相位相同，见 [报告](COMPONENT_DRAIN_COVER_AUDIT.md)；压力阀 >2 使用覆盖板计数而非实体年龄，见 [补查](PRESSURE_VALVE_AUDIT.md)。 |
| ALIGN-101 | 确认雨水气候取样位置不同。 | 原版宿主群系决定温度门控和产量，前方决定接雨条件。 | 当前从 front 群系算产量；群系边界可能不同，纯雨量公式已对应，见 [报告](COMPONENT_DRAIN_COVER_AUDIT.md)。 |
| ALIGN-102 | 确认检索器安装范围、管网搜索和取货方向不同。 | 原版仅装在可 tick 的物品管道，从管网邻接来源向覆盖板面对库存搬运；首次成功返回并计数。 | 当前从前方抽入宿主，可装基础机器，没有对应扫描／管道吞吐准入；金属箱可装但没有检索 tick／普通进出拦截，见 [能力补查](COVER_CAPABILITY_AUDIT.md) 和 [检索器报告](FILTER_RETRIEVER_COVER_AUDIT.md)。 |
| ALIGN-103 | 确认检索器恢复后失败持续逐 tick 重试。 | 原版通过容量等门控后，在尝试前清除 pending。 | 当前两类宿主仅成功搬运后清除，恢复失败保留；停止门控仍在，重载未验，见 [报告](FILTER_RETRIEVER_COVER_AUDIT.md)。 |
| ALIGN-104 | 确认检索器空模板与数据匹配合同不同。 | 原版无模板两模式均不筛选，有模板按物品／元数据集合匹配。 | 当前空白名单阻断，带数据模板用完整 NBT／components 比较；重复点击设置的通配字段又未读取。普通过滤器不可据此全局改规则，见 [报告](FILTER_RETRIEVER_COVER_AUDIT.md)。 |
| ALIGN-105 | 确认电池盒侧面物品能力未应用过滤器。 | 原版 final canInsertItem／canExtractItem 先问覆盖板，再问电池准入。 | 当前仅挡板和组件方向，后备 Mixin 排除原生 getCover 重写；两版 provider 均进入此路径。电池种类限制仍在，见 [报告](COVER_CAPABILITY_AUDIT.md)。 |
| ALIGN-106 | 确认压力阀未断开阀面，也未接入管道连接拦截。 | 原版每次合格 tick 先 disconnect，再看满罐；前方管道 interceptConnect=true。 | 当前 helper／宿主均不改连接，CoverConnections 无阀门分支；原错误命名谓词仅被测试调用，安装检查则已有实现。见 [报告](PRESSURE_VALVE_AUDIT.md)。 |

可读性第二十六批：按用户要求为 46 份共享属性定义表补齐空格，内容与顺序保持，生成／读取工具同步适配。旧材料再生成自检的数量断言和四文件快照漂移单列为工具链后续核查，未将其直接判断为原版玩法差异；见 [源码导航](SOURCE_LAYOUT.md) 和 [回执](verification/definition-table-formatting-20261019.json)。

可读性第二十七批：材料中间快照及分表生成器已恢复当前七份生产文件的逐字再现，运行时数据不变。旧计数／路径自检已修正；仍有 14 项原版矿物处理导入跳过，未作为已解决事项，见 [材料生成链核查](MATERIAL_GENERATION_AUDIT.md)。

上述是具体范围，不表示这些系统完全未移植；ALIGN-009、010、012 有独立 core 观察，能源目录和弱燃料参数也有 core 枚举，其余结论依赖具体调用链核查，没有启动游戏。锅炉及液体／气体燃烧室的完整来源、相同部分和边界见 [专题核查](BOILER_BURNING_BOX_AUDIT.md)。

第七批补充：ALIGN-025 的环境动作缺口也在固体／流化床确认；新增 ALIGN-026–030 的来源、遮蔽关系及实际目录观察见 [固体／流化床核查](SOLID_FLUIDBED_AUDIT.md)。未因注册表存在就判断燃烧功能完整，未将潜在灰渣丢失说成已游戏复现。

第八批补充：五种大型锅炉额定值与原版注册行匹配，四向 core 预览／检查布局一致；实际原生 tick／工具／传感器另确认 ALIGN-031–035。详见 [大型锅炉核查](LARGE_BOILER_AUDIT.md)，源码边界例子没有冒称为游戏验收。

第九批补充：焦炉原版参数、25 块耐火砖／空气、四向九格排液位置集合及 TU 并行计算已核对；调用链确认 ALIGN-036–039。目标位置相同不代表选择顺序相同，罐容量够大不代表原版允许无限积压，见 [焦炉核查](COKE_OVEN_AUDIT.md)。

第十批补充：共用基础机器的准入、工作量和产物落槽调用链确认 ALIGN-040–043；核心算术观察与原生状态推导分开，见 [加工执行核查](BASIC_MACHINE_EXECUTION_AUDIT.md)。

第十一批补充：中断、交变 KU 与整包缓冲确认 ALIGN-044–047；原版豁免、初始计时和未运行的原生状态均单独标注，见 [中断与供能核查](BASIC_MACHINE_INTERRUPTION_AUDIT.md)。

第十二批补充：85 对显式配方表标志、动态入口和长配方样例确认 ALIGN-048–051；展示条目、右键行为、静态处理与实际动态处理分别核查，见 [动态配方报告](DYNAMIC_RECIPE_AUDIT.md)。

## 全面核查队列

| 系统 | 本轮状态 | 完成盘点需要的证据 |
| --- | --- | --- |
| 材料、前缀、物质量、统一规则 | 已清点三组 TD 的 69 个直接属性声明，核查其中 15 项的具体表示／消费路径；工厂样本和七种目标直接引用已扩大核查 | 已确认 ALIGN-006–010、012。FOOD／BRITTLE／SOFT 等虽有 `MaterialWorkability` 表示，继承和名称映射仍有实际缺口；成员全集未完成。 |
| 固定／动态配方、监听器 | 两版本 54 步初始化序列已比对并共享；原版通用处理器 28 组、478 个调用点已索引；85 对表级标志及 Bath／Unboxinator 动态入口已核，具体差异见 ALIGN-011、013–016、048–051 | 调用点不是最终配方数。继续核对剩余配方组、材料展开条件、生成器跳过原因、运行时未解析行和回调顺序；外部依赖与内部遗漏分开 |
| 手工工具与早期生存 | 已有前轮局部修复，待扩大核查 | 工具专属行为、损耗、容器返还和自然可得原料 |
| 单物品、食物、饮料、服装 | 已有 1,278 项运行清单，待核查行为出口 | 无静态机器配方不直接判缺失，另查右键、掉落、工作台、动态回调 |
| 弹药与实体 | 已确认 ALIGN-002，未完成全类核查 | 箭／子弹命中、附魔、拾取、同步和外部武器入口 |
| 坩埚、相变、铸造 | 已确认 ALIGN-001，待核其余分支 | 致死回收、热采掘、冷却目标、过热与材料守恒 |
| 单方块机器 | 共用加工执行／中断／供能已确认 ALIGN-040–047，逐控制器及其余分支继续 | 配方映射、输入输出、额定值、超载、工具交互与持久化 |
| 多方块与部件 | 大型锅炉、焦炉、聚变／内爆的部分结构／执行链已核查；物质制造结构／参数／特殊配方声明已对照 | 结构位置、部件角色、断开行为、建造与诊断、部件共用身份；整体绑定差异见 ALIGN-036，聚变输出／控制、内爆音效及聚变连续恢复见 ALIGN-052–056；物质制造最终配方解析、其它主控及跨区块行为继续 |
| 能量生产与转换 | 已核电／Flux 动力转换、十五种单方块蒸汽涡轮、26 种小锅炉、五种大型锅炉及四种燃烧室主要路径；另核九档电变压器、二十种电池盒及二十五种化学电池目录，追踪 LU 水晶注册消费 | 核心边界例子、额定目录与原生调用链已记录；固体自动化／外部燃料、全材料灰渣、其他多方块、旋转／长距离转换、LU 充能器／便携电源缺项已登记，剩余能源设备及保存重启仍待核，见 [能源核查](ENERGY_CONVERTER_AUDIT.md)、[锅炉核查](BOILER_BURNING_BOX_AUDIT.md)、[固体／流化床](SOLID_FLUIDBED_AUDIT.md) 和 [大型锅炉](LARGE_BOILER_AUDIT.md) |
| 电线、流体／物品管道、长距离物流 | 待逐网络重核 | 优先级、路由、丢包、接收端卸载与跨区块边界 |
| 储物、容器与传感器 | 待逐类型重核 | 容量、成组掉落、量子／中继桥、仪表与单位 |
| 覆盖板、面板与红石 | 待逐行为重核 | 安装面、厚度、信号方向、过滤、同步与拆除 |
| 养蜂与植物 | 待逐周期重核 | 环境、遗传、突变、产物、成熟时间和存档状态 |
| 核反应堆 | 待逐棒／冷却规则重核 | 中子、增殖、反射、慢化、辐射、冷却失效和保存 |
| 世界生成、Nexus、roads | 前轮石层互斥已修，待全布局核查 | 维度、高度、顺序、生成条件、部件更新及自然资源分布 |
| 模型、GUI、音效、颜色与光照 | 待按实际对象重核 | 引用链与可见渲染，不能继续复用旧“421 条警告”数量作为现状 |
| 原版专属提示、材料含量与中英文 | 已确认 ALIGN-003，待全注册项重核 | 提示调用路径、动态字段、原文／补丁来源，不只检查 JSON |
| 查看器与其他模组兼容 | 待逐集成重核 | JEI／EMI 转移、分类、页面、刷新与外部提供方；区分现代替代关系 |
| 配置、网络与存档 | 已确认 ALIGN-004，余项待核 | 原配置矩阵、协议边界、所有状态字段、真实保存重启 |

## 源码结构改进

本批已经移动 50 个类，并同步生产引用、现有探针、生成脚本及读取工具：

| 旧目录 | 新职责目录 |
| --- | --- |
| `loader` | `content/material`：Minecraft 材料组成与统一绑定 |
| `loaders/a` | `bootstrap/content`：基础内容初始化 |
| `loaders/b` | `bootstrap/registration`：注册提交、材料链接与创造栏初始化 |
| `loaders/c` 的手写配方 | `content/recipe`：配方注册业务 |
| `loaders/c` 的纯数据表 | `core/.../content/recipe/generated`：共享配方数据及输出接口 |
| `GTGeneratedChem` | `multiversion/.../content/recipe/generated`：数据表的原生解析适配；与包内表接口协作，不暴露所有数据类 |
| `IGTLoader` | `bootstrap`：初始化步骤合同 |

目录迁移不更改注册 ID、资源、初始化顺序、配方数值或保存格式。方法体和字符串已按 Forge／NeoForge 两种条件展开，与基线归一化比较。聚变配方需要的两个解析方法由包内访问显式改为 public；它们仍不是对外 SDK API。完整映射见 [迁移记录](verification/loader-package-migration-20261019.json)。

首批双版主代码及探针编译通过（1m28s）；105 个 Java 文件两版归一化比较通过。当时额外源策略检查因 ALIGN-005 的标签目录遗漏失败；第二批已修复并通过。首批未运行游戏或打包，之后按用户要求交付的当前工作树包另见 [构建回执](verification/current-build-20261019.json)。

第二批已经完成 `Loader_...`／`IGTLoader`／`GTGeneratedChem` 共 30 个活动类名整理及共用机器配方清单。原版来源注释和历史回执保留原名。78 个非启动入口 Java 文件在正确展开 Stonecutter 条件后，除声明的接口适配外代码／字面量等价；两平台各 54 步调用序列相同，日志和异常定位改用明确职责名称。NeoForge 旧独立配方加载类已删除。

第三批展开三份加工计算类的压行实现，并修正三份配方类的重复／过时说明。六个 Java 文件双版展开后代码和字符串与基线一致，三个计算类独立编译通过；没有把格式整理夹带成配方行为调整。新增只读原版目标引用索引工具，旧生成器的硬编码来源路径仍待统一。

第四批已规范工作属性／处理目标两个生成器：显式 `--source`，真实 core 输出，`--check` 不写文件，共用 UTF-8 输出助手；不再依赖固定 `F:` 或在导入时创建原生视图。现有两表可重现，错误输出可被核对模式拒绝且保持不变；提取语义缺口仍开放。本批未改生产 Java／资源。

尚未完成的标准化：过大职责类拆分、紧密压行代码整理、其它重复注册适配、生成器路径／来源参数的统一，以及审计中发现的材料元数据消费边界。不能把改名和一份清单当作全部重构完成。

第八批展开大型锅炉参数／布局与共用 StructureGrid，坐标缩写改成 front／controller／center 对应的明确变量名。归一化声明的形参／局部变量改名后代码及字面量一致；两类独立编译通过，未调整坐标公式和玩法。

## 下一批工作

第二十五批核查流体管道 40 组／280 规格与安全／分配源链，新增接触伤害标志、方块属性、温度计接口 ALIGN-083–085，进度接口扩展 ALIGN-077。压力计已有接口，不列为缺失。目录与两版注册命名／格式整理保持行为；继续全网时序、特殊库存／覆盖板和其它系统，见 [报告](FLUID_PIPE_AUDIT.md)。

第二十四批核查物品管道 21 组／126 规格目录与实际库存、路由、能力入口，新增 ALIGN-079–082；进度接口缺口扩展 ALIGN-077。纠正过时过滤器说明、展开 Neo 注册并修复源码投影对嵌套注释的处理。继续库存特例、交互、通知和其余系统，见 [报告](ITEM_PIPE_AUDIT.md)。

第二十三批对照普通导线 30 组／620 个变种目录和正常输电源链，新增电流进度接口、泡沫包覆 ALIGN-077–078。整理压行代码并纠正辅助连接索引职责；复杂环路、首 tick、区块／兼容边界和物流仍待继续，见 [报告](ELECTRIC_WIRE_AUDIT.md)。

第十五批沿物质制造流体关联与单位量新增 ALIGN-057–058，追踪了双重声明的碰撞拒绝及选择器保留路径，未冒称最终有效配方枚举。28 条数据移入 core 目录，说明及观察见 [物质流体核查](MATTER_FLUID_AUDIT.md)。其余相态、外部覆盖、储能／变压器与网络仍在队列。

第十四批逐位置核对物质制造的 149 个坐标／角色和处理器替换，28 条特殊配方声明及额定目录对应；聚变恢复后连续下一批新增 ALIGN-056。修订旧聚变源自检以直接读取维护目录，分别检查两版主控资源，见 [报告](MATTER_AND_FUSION_CONTINUITY_AUDIT.md)。完整物质制造配方解析／相态单位量和其它系统仍待核。

第十三批完成聚变／内爆的结构目录、额定参数和四项执行差异核查，详见 [专题报告](ADVANCED_CONTROLLER_AUDIT.md)。两份参数／布局代码展开且代码和字面量等价；聚变结构生成器统一到共享 core，886 格可全文复现。继续特殊主控的重载／停机连续性、物质制造机、储能／变压器和网络；旧聚变资源自检的过时路径也需修订。全面目标仍未完成。

继续核查材料属性的成员与工厂继承、处理目标和前缀监听器，以及锅炉、其它能源转换／储能与网络系统。同步整理相关过大／压行实现。第五批已整理三份能源核心源码的格式与职责说明，并在 SOURCE_LAYOUT 中补充批量材料数据导航。每条结论附原版和当前调用链；完整审计仍未完成，不能以这一批编译通过结束目标。

第十六批核对九档电变压器目录和主要方向／控制路径，新增输入边界 ALIGN-059–060，扩展环境检查 ALIGN-017；两份压行控制类展开。详见 [电变压器核查](ELECTRIC_TRANSFORMER_AUDIT.md)。电池盒、旋转与长距离转换及网络仍待继续。

第十七批核对电池盒与化学电池目录、充放电和 LU 水晶注册消费，新增保护性差异 ALIGN-061 与功能缺口 ALIGN-062，见 [储能核查](BATTERY_STORAGE_AUDIT.md)。继续水晶充能器、便携电源、旋转与长距离转换及网络。

第十八批沿水晶与便携电源注册链、ZPM 电容／进度及光照分档新增 ALIGN-063–066，见 [特殊储能核查](SPECIAL_STORAGE_AUDIT.md)。继续旋转／长距离转换、网络和其余未核系统，全面目标未完成。

第十九批核对旋转变速器，新增 ALIGN-067–069，并将末包取整 ALIGN-059 与环境检查 ALIGN-017 扩展至其共享路径。齿轮纯函数只完成格式整理与局部观察；轴／齿轮箱完整状态、长距离转换和网络继续核查。见 [报告](ROTATION_TRANSFORMER_AUDIT.md)。

第二十批核对长距离输电目录、扫描／归属、损耗与原生保存边界，新增 ALIGN-070–072；四份代码展开并保持行为。管道完整传输、燃线／区块事件、轴／可调齿轮箱及其余系统继续核查。见 [报告](LONG_DISTANCE_ENERGY_AUDIT.md)。

第二十一批核对长距离管道侧面／温度／递归转发，新增 ALIGN-073，纠正运行时自检归因；十三项重复平台业务方法合并并验证源码等价。原版流体保存温度键不一致另作来源说明，不复制其错误。完整物流与其他队列继续，见 [报告](PIPELINE_RELAY_AUDIT.md)。

第二十二批核对 52 种轴与可调齿轮箱运行状态，新增 ALIGN-074–076，整理目录私有命名和轴输送代码。保护性饱和算术保留；装入齿轮的回收、声音／视觉、物流网络与其它系统继续核查，见 [报告](AXLE_GEARBOX_AUDIT.md)。
