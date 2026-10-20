# 原版对齐与源码结构审计

2026-10-21 续核：已合并远端`894f90ce0`，第六十三批归档核扩展、太阳能板与单块热交换器；此前工具配方源码、核生命周期和配置专题已整理。行为记录至ALIGN-221，共221条正式记录，包含已修复／部分修复与保留政策，不是221个未修复问题。旧批次证据保留日期边界。

本轮玩家反馈已修复 ALIGN-137 及 ALIGN-126 的大容量存储实例，并恢复普通流体管道的原版更新前两相调度；颜色拆分和定向验证见[反馈记录](verification/storage-direction-colors-20261020.md)。其余全面队列继续。

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
| ALIGN-003 | 确认存在未进入语言系统的提示。 | 原版提示／扫描来源需按具体行继续比对汉化补丁。 | `item/FuelRodItem:183–211` 直接拼接 Remaining、Self、Fuel、Tier、Durability 等；`item/behavior/BehaviorScanner:170–183` 拼接 Input、Output、Stored。单块热交换器放大镜也直接拼Input／Output，见[热交换器](HEAT_EXCHANGER_AUDIT.md)。语言文件格式自检不证明这些提示已汉化。 |
| ALIGN-004 | 配置覆盖仍不完整；cfg收尾安排保留。当前三个声明中两个过载选项有效，日志选项无消费者。 | 原Config真实load/save与代表链包括石层／维度、生物掉落、材料工具／处理倍率；不是仅静态常量。 | 两版COMMON注册及原生加载／保存／缓存刷新已接通，MachineOverload实际读取两个默认false选项；logMaterials未读。代表族缺配置入口，133概率沿旧项，Nexus全开是用户政策；见[配置核查](CONFIGURATION_CONTRACT_AUDIT.md)，完整矩阵／磁盘／多人仍待核。 |
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
| ALIGN-017 | 确认转换器未接入原版运行环境风险检查。 | `TileEntityBase10EnergyConverter.onTick2` 每 600 tick 检查活动设备；Root 与 Covers 按能量类型、侧面、覆盖板和配置处理火／水／雨／雷。 | `EnergyNodeBlockEntity.tickOriginalConverter` 只保留不运行时衰减过载次数；易损集合存在但未消费。电变压器 emitElectricTransformer 及电池盒 serverTick 同样缺少活动环境分支，见 [电变压器核查](ELECTRIC_TRANSFORMER_AUDIT.md)。太阳能板活动火／邻液缺口亦属此项，但原雨／雷免疫true，取消waterlogged政策保留，见[太阳能板](SOLAR_PANEL_AUDIT.md)。范围及概率见 [能源核查](ENERGY_CONVERTER_AUDIT.md)，不能推为所有蒸汽设备都应遇水损坏。 |
| ALIGN-018 | 已修复注册缺项（2026-10-20）；完整设备行为不由此保证。 | 原版注册 11023–11025、11113–11115，无外部依赖条件。 | 当前 FluxRotaryCatalog 具有五档，六台高档设备的注册、配方、模型、材料及原文名称已补齐；见[修复记录](verification/gameplay-and-single-machines-20261020.md)。 |
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
| ALIGN-031 | 部分修复：大型锅炉顺序、实时表值及未加载缓存已修正。 | 原版先煮水、冷却／排汽、更新表值，再判断结构与过热；区块缺失保留旧状态。 | 当前按当前压力判断，并保留未加载壳体缓存；主控直连接口的结构门槛等剩余分支仍需单独核对，不能继续沿用“未形成提前返回”的旧描述。见[风险核查](verification/machine-explosion-review-20261020.md)。 |
| ALIGN-032 | 确认大型锅炉多出口分配不同。 | 原版先模拟接受量，优先处理低于平均份额的小接收端。 | 当前顺序分配再补余量；100 L、三端容量 100／1／100 的源码例子原版 49／1／50，当前 33／1／66，总量均 100，不是据此判丢汽。 |
| ALIGN-033 | 确认大型锅炉缺少外部爆炸连锁。 | 原版 onExploded 在表值 >4 时引爆，威力随汽量变化。 | 当前玩家拆机、自身爆炸与高压不掉落存在，缺外部爆炸回调。 |
| ALIGN-034 | 确认大型锅炉工具损耗和除垢伤害入口不同。 | 原版排空返回实际量，温度计返回工具消耗，除垢调用热伤害及热防护判断。 | 当前排空固定 100，温度计分支只显示信息；除垢直接用 inFire，未经过现有 GTEntityHelper 热防护入口。 |
| ALIGN-035 | 确认精金大型锅炉压力传感器最大容量被截断。 | 原版 getGibblMax 返回长整数容量。 | 当前 gibblMaximum 调用 int getCapacity：2,621,440,000 变成 2,147,483,647；内部储罐和表针长整数计算未因此缩水。 |
| ALIGN-036 | 确认已核多方块不完整结构的部件绑定策略不同。 | 原焦炉／大型涡轮逐格检查并认领正确部件，缺关键区块保留旧形成状态。 | 当前共用 LargeRecipeMachine 首次遇错即清空已绑定部件，仅完整候选才整体绑定；部件工具／传感器转发也依赖这份主控引用。大型蒸汽／燃气涡轮相同认领／缺区块差异亦已扩围，见[大型涡轮](LARGE_TURBINE_AUDIT.md)。 |
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
| ALIGN-071 | 部分修复：长距离输出已接公开附属能源视图，FE-only 路径仍未覆盖。 | 原版经网络工具进入 EnergyCompat。 | 当前经 EnergyBlockViews.view 接受内部 IEnergyBlock 及附属 EnergyBlock；该视图对仅有 FE 能力的实体仍返回 null，不能视为完整通用兼容。 |
| ALIGN-072 | 已修复无条件删块差异（2026-10-20）。 | 原版 overcharge 按配置处理破坏，并发送过载反馈。 | 当前发送／接收端都经共用 MachineOverload；原版两个损坏设置默认关闭，显式开启及直接危险特例另核，见[风险核查](verification/machine-explosion-review-20261020.md)。 |
| ALIGN-073 | 确认长距离物品端点允许原版侧面合同禁止的自动化抽取。 | 原版直接 decrStackSize 委托，但 canExtractItem2=false，覆盖板父类仍强制该许可。 | 当前共用 ItemRelay.extractItem 无端点抽取限制；旧 smoke 把此行为误称原版对应，已纠正归因，玩法待修。见 [报告](PIPELINE_RELAY_AUDIT.md)。 |
| ALIGN-074 | 确认可调齿轮箱余量输出失去当 tick 输入触发要求。 | 原版 mUsedGear 门控输出，并以 mIgnorePower 控制模拟／实际回压。 | 当前只要旧缓存非空便继续尝试，模拟仍返回提供量；停供后行为不同，见 [报告](AXLE_GEARBOX_AUDIT.md)。 |
| ALIGN-075 | 确认可调齿轮箱输出轮次、份额和优先序不同。 | 原版有进展就继续，按 power/3 分配且每轮推进顺序。 | 当前最多三轮、按剩余输出面分配、tick 末推进一次；限量接收器吞吐可能不同，见 [报告](AXLE_GEARBOX_AUDIT.md)。 |
| ALIGN-076 | 确认可调齿轮箱缺少通用开关／运行状态接口。 | 原版实现 SwitchableOnOff／RunningActively。 | 当前只实现 IEnergyBlock，MachineControl.find 返回 null；软锤手动停启仍存在，见 [报告](AXLE_GEARBOX_AUDIT.md)。 |
| ALIGN-077 | 确认导线、管道及普通桶未接入原版进度指标；有些覆盖板可用但读数语义不同。 | 原版分别返回安培、搬运次数乘 64、传输液量、发酵时间及对应上限。 | 管道／桶无专用进度接口，但 CoverProgress 会回退到内容物占比；不能写成完全不可安装或无读数，见 [补查](DISPLAY_METER_AUDIT.md) 和 [导线](ELECTRIC_WIRE_AUDIT.md)、[物品管道](ITEM_PIPE_AUDIT.md)、[流体管道](FLUID_PIPE_AUDIT.md)、[普通桶](BARREL_AUDIT.md)。 |
| ALIGN-078 | 确认导线缺少建筑泡沫包覆与硬化绝缘。 | 原版连接器支持湿／干泡沫，导线硬化后免除接触电击。 | 当前导线没有包覆状态；硬化剂／去除剂只识别独立泡沫方块，见 [报告](ELECTRIC_WIRE_AUDIT.md)。 |
| ALIGN-079 | 确认物品管道库存槽数被当作单槽容量。 | 原版 NBT_INV_SIZE 创建多槽库存，每槽正常上限 64。 | 当前固定一个槽、槽上限 spec.invSize，涉及缓存种类及存档结构，见 [报告](ITEM_PIPE_AUDIT.md)。 |
| ALIGN-080 | 确认物品管道吞吐限流缺失且计数单位不同。 | 原版每次成功搬运计一堆，扫描／发送检查每秒容量。 | 当前按物品数累加，仅保存和显示，无容量准入，见 [报告](ITEM_PIPE_AUDIT.md)。 |
| ALIGN-081 | 确认物品管道改成逐段缓存转交。 | 原版每十 tick 扫描后从源库存直接交付出口。 | 当前每四 tick 向 firstHop 库存转交，堵塞／时序不同，见 [报告](ITEM_PIPE_AUDIT.md)。 |
| ALIGN-082 | 确认物品管道侧面能力未检查连接及既有入口锁定。 | 原版 connected、固定接收面、空槽准入；外部抽取也要求连接。 | 当前侧面包装只查禁入／禁出，插入成功覆盖入口；内部路由检查不能代替，见 [报告](ITEM_PIPE_AUDIT.md)。 |
| ALIGN-083 | 确认流体管道遗漏材料接触伤害开关。 | 原版 16 组材料／112 规格免接触温度伤害。 | 当前规格无 contactDamage，碰撞对所有管道调用温度伤害，见 [报告](FLUID_PIPE_AUDIT.md)。 |
| ALIGN-084 | 确认流体管道方块硬度／橡胶抗爆参数不对应。 | 原版 blocking 真为 2／6，橡胶假为 1／2。 | 当前 blocking 未传递，两版固定硬度 1.5／抗爆 6；快速阻挡语义需续核，见 [报告](FLUID_PIPE_AUDIT.md)。 |
| ALIGN-085 | 确认流体管道、自然蜂巢温度计接口及环境回退缺口。 | 原版温度计读取管道 Temperature 合同；自然巢工具还读环境温度／湿度。 | 通用行为没有接受这些宿主；管道压力计和普通液量、养殖蜂箱专用温湿读数仍有，不能一并报缺失。见[管道](FLUID_PIPE_AUDIT.md)、[传感器补查](SENSOR_EXECUTION_AUDIT.md)和[养蜂扩围](BUMBLE_LIFECYCLE_AUDIT.md)。 |
| ALIGN-086 | 确认普通桶封口未隔离流体。 | 原版封口禁普通进出液，发酵与自动排液互斥。 | 当前 fill/drain 无封口检查，tick 可先排液再发酵；空桶软锤位操作也不同，见 [报告](BARREL_AUDIT.md)。 |
| ALIGN-087 | 确认桶自动排液方向／限量与能力不同。 | 原版按气体／浮力分垂直、上、下，无额外 1000 限量。 | 当前轻流体向六面、每面最多 1000，并隐藏选定输出面整个 capability，见 [报告](BARREL_AUDIT.md)。 |
| ALIGN-088 | 确认桶发酵加载后进度清零。 | 原版重查有效配方保留已保存进度，查配方 tick 不递增。 | 当前查配方清零两种时间且同 tick 推进；正常新发酵也提前一 tick，见 [报告](BARREL_AUDIT.md)。 |
| ALIGN-089 | 确认桶世界／物品准入混用并缺少部分限制。 | 原版放置桶接受后在 tick 处理危险，便携物品先拒绝。 | 当前世界桶预拒绝热／气／酸／等离子，遗漏魔法、导能及木桶 SIMPLE 消费，见 [报告](BARREL_AUDIT.md)。 |
| ALIGN-090 | 确认桶过热后果丢失内容物分支。 | 原版至少 1000 mB 熔岩才放熔岩，否则放火并 burn。 | 两版 helper 总放 level 4 熔岩并造成 4 点 heat；正常 fill 已拦超温，触发条件另述，见 [报告](BARREL_AUDIT.md)。 |
| ALIGN-091 | 确认发酵产物数量被额外钳制。 | 原版缩放后 setFluid 不按罐容量截断。 | 当前至少 1、至多容量；core 观察 0／32000 边界，实际配方可达性未全验，见 [报告](BARREL_AUDIT.md)。 |
| ALIGN-092 | 确认物流桶空桶软锤无法清除过滤。 | 原版父类先处理空桶清身份／自动输出，非空才检查 canBeSealed。 | 当前整个 toggleSoftHammerState 为空；保留过滤本身正常，见 [报告](LOGISTICS_TANK_AUDIT.md)。 |
| ALIGN-093 | 确认共享储液容量查询／缩容改变原版合同。 | 原版缩容不改已有量，无可调表时报告名义容量。 | 当前缩容截量，查询额外以已有量作下限；部分多方块已有补偿，见 [报告](LOGISTICS_TANK_AUDIT.md)。 |
| ALIGN-094 | 确认溢出销毁的接口分支不完整。 | 原版 add／fill 启用后报告消耗整个同种输入。 | 当前 add 仅满罐报告全部，原生 fill 未用标志；未发现启用业务调用，见 [报告](LOGISTICS_TANK_AUDIT.md)。 |
| ALIGN-095 | 确认可调容量键／填充入口未按实际输入流体消费。 | 原配方／罐同用Fluid名称，空罐按输入查询；核热罐Steam可扩为10240000。 | 当前type ID混用及空罐无参fill见[原报告](ADAPTIVE_FLUID_AND_ITEM_AUDIT.md)。59批扩到核热罐capacity(stack)虚覆写未被fill消费，401L水对应64160Steam超过实际64000许可；历史超量仍保留，非全旧罐截断，见[核心](NUCLEAR_CORE_AUDIT.md)。 |
| ALIGN-096 | 确认基础配方机默认罐容量和倍率不同。 | 原版输入默认 1000／可覆盖、配方容量乘 parallel×2，输出 Long.MAX_VALUE。 | 当前输入 8000／倍率 1，基础输出 8000；独立输出覆写另列，不等同原版积压限制，见 [报告](ADAPTIVE_FLUID_AND_ITEM_AUDIT.md)。 |
| ALIGN-097 | 确认普通桶与物流桶物品缺少容器能力。 | 原版 MultiTileEntityItemInternal 转发桶的物品 fill／drain 并更新 NBT。 | 当前只有存液掉落数据和提示／堆叠限制，没有本模组提供的物品流体 handler；量杯等不在此缺口内，见 [报告](ADAPTIVE_FLUID_AND_ITEM_AUDIT.md)。 |
| ALIGN-098 | 确认泵每源罐只尝试一次，不重复使用余下预算。 | 原版 CoverPump 在有进展且有预算时循环 FL.move。 | 当前 for 每个 tank 一次；单次调用限额的接收器可能低于额定吞吐，未原生复现，见 [报告](COMPONENT_DRAIN_COVER_AUDIT.md)。 |
| ALIGN-099 | 确认排水口世界取液范围收窄。 | 原版河湖／绿洲名单、量子份额及通用流体 drain 回调。 | 当前无限水只检查河流标签，普通路径限定 LiquidBlock 源；三种 GT 水独立批量路径已有，见 [报告](COMPONENT_DRAIN_COVER_AUDIT.md)。 |
| ALIGN-100 | 确认排水口缺少方块更新即时尝试，且宿主时钟不同。 | 原版 receivedBlockUpdate 或全局相位。 | 当前只周期调用，基础机／管道本地计数、其他宿主世界计时；周期相同不是相位相同，见 [报告](COMPONENT_DRAIN_COVER_AUDIT.md)；压力阀 >2 使用覆盖板计数而非实体年龄，见 [补查](PRESSURE_VALVE_AUDIT.md)；重启定时器的持久化本地／世界时钟差异见 [控制核查](CONTROLLER_DETECTOR_AUDIT.md)。 |
| ALIGN-101 | 确认雨水气候取样位置不同。 | 原版宿主群系决定温度门控和产量，前方决定接雨条件。 | 当前从 front 群系算产量；群系边界可能不同，纯雨量公式已对应，见 [报告](COMPONENT_DRAIN_COVER_AUDIT.md)。 |
| ALIGN-102 | 确认检索器安装范围、管网搜索和取货方向不同。 | 原版仅装在可 tick 的物品管道，从管网邻接来源向覆盖板面对库存搬运；首次成功返回并计数。 | 当前取货／扫描／准入不同；金属箱可装但无检索 tick 是当前事实，原箱子本不支持覆盖板，不能以补搬运替代安装范围修正。见[能力补查](COVER_CAPABILITY_AUDIT.md)、[检索器](FILTER_RETRIEVER_COVER_AUDIT.md)及[箱子范围](CHEST_LOOT_AUDIT.md)。 |
| ALIGN-103 | 确认检索器恢复后失败持续逐 tick 重试。 | 原版通过容量等门控后，在尝试前清除 pending。 | 当前两类宿主仅成功搬运后清除，恢复失败保留；停止门控仍在，重载未验，见 [报告](FILTER_RETRIEVER_COVER_AUDIT.md)。 |
| ALIGN-104 | 确认检索器空模板与数据匹配合同不同。 | 原版无模板两模式均不筛选，有模板按物品／元数据集合匹配。 | 当前空白名单阻断，带数据模板用完整 NBT／components 比较；重复点击设置的通配字段又未读取。普通过滤器不可据此全局改规则，见 [报告](FILTER_RETRIEVER_COVER_AUDIT.md)。 |
| ALIGN-105 | 确认电池盒侧面物品能力未应用过滤器。 | 原版 final canInsertItem／canExtractItem 先问覆盖板，再问电池准入。 | 当前仅挡板和组件方向，后备 Mixin 排除原生 getCover 重写；两版 provider 均进入此路径。电池种类限制仍在，见 [报告](COVER_CAPABILITY_AUDIT.md)。 |
| ALIGN-106 | 确认压力阀未断开阀面，也未接入管道连接拦截。 | 原版每次合格 tick 先 disconnect，再看满罐；前方管道 interceptConnect=true。 | 当前 helper／宿主均不改连接，CoverConnections 无阀门分支；原错误命名谓词仅被测试调用，安装检查则已有实现。见 [报告](PRESSURE_VALVE_AUDIT.md)。 |
| ALIGN-107 | 确认固定编号选择标签错误受停止门控，安装也未立即设模式。 | 原版 placed／loaded／tickPre 强制指定模式，不检查 stopped。 | 当前 beforeTick 要求 !stopped，attached 不处理固定标签；不能据此删除其他选择器的正确暂停门控，见 [报告](SELECTOR_PANEL_AUDIT.md)。 |
| ALIGN-108 | 确认手动／按钮选择器停止加载时丢失面板恢复机会。 | 原版 onCoverLoaded 无条件用保存值恢复模式。 | 当前 stopped 跳过循环但仍清 restore，恢复后读宿主模式覆盖面板；宿主独立保存可能掩盖，未实测重启，见 [报告](SELECTOR_PANEL_AUDIT.md)。 |
| ALIGN-109 | 确认按钮复位比原版提前一个机器工作阶段。 | 原版 post-work tick 将倒计时 2 变 1 并清零模式。 | 当前 beforeTick 清零，电动机随后执行转换器；末轮机器采用模式不同，不是 10→1 数量规则错误，见 [报告](SELECTOR_PANEL_AUDIT.md)。 |
| ALIGN-110 | 确认通用检测／自动红石控制丢失成功状态。 | 原版专用成功接口，控制扩展器转发目标状态。 | MachineControl 无该字段，通用 State 固定 false，安装仅看 supportsSwitch；基础机另有 successful 专用输出，不算全失效，见 [报告](CONTROLLER_DETECTOR_AUDIT.md)。 |
| ALIGN-111 | 确认通用控制未保留独立开工条件。 | 原版直接查询目标 getStateRunningPossible。 | 当前默认 active 或 progressMax>0 近似，可能漏掉有配方但尚未启动的目标；条件案例未游戏验证，见 [报告](CONTROLLER_DETECTOR_AUDIT.md)。大型涡轮原possible恒true、当前idle默认false的具体宿主已确认，见[大型涡轮](LARGE_TURBINE_AUDIT.md)。 |
| ALIGN-112 | 确认显示面板缺少原版图层独立高亮。 | 能源显示及状态的部分指示图层 aGlow=true，构造器使用自带亮度 240。 | 当前仅字符串图层，全部传环境 light；源码确认，暗室实际观感未验，见 [报告](DISPLAY_METER_AUDIT.md)。 |
| ALIGN-113 | 确认独立传感器比例／百分比红石分档不同。 | 原版中间正值使用 1+value×14/max。 | 当前 floor(value×15/max)，50% 为 7 而非 8，零阈值也不同；不可改错覆盖板自己的量化，见 [报告](SENSOR_EXECUTION_AUDIT.md)。 |
| ALIGN-114 | 确认堆叠计错误按物品数除以 64。 | 原版数可访问的非空堆，最大为槽位数。 | 当前 count/64、slotLimit 总和/64；单个物品显示 0，见 [报告](SENSOR_EXECUTION_AUDIT.md)。 |
| ALIGN-115 | 确认独立流体／桶量计遗漏世界流体。 | 原版水／岩浆源及 IFluidBlock 模拟读取有回退。 | 当前仅在目标实体存在时读能力，无实体源方块保持零；世界测试未做，见 [报告](SENSOR_EXECUTION_AUDIT.md)。 |
| ALIGN-116 | 确认仪表样本和上限钳制不一致。 | 原版两者 bindInt 后比较／求百分比。 | 当前样本 int、大部分 maximum long；额定超导满量的合成输入 FULL 为 0 而非 15，纯规则已观察，见 [报告](SENSOR_EXECUTION_AUDIT.md)。 |
| ALIGN-117 | 确认普通传感器采样从每后 tick 改为每十 tick。 | 原版默认 getTickRate=1，仅 TPS 覆写 20。 | 当前统一节流 10，影响平均窗口时长和短脉冲；TPS 样本重复加入边界也需核，见 [报告](SENSOR_EXECUTION_AUDIT.md)。 |
| ALIGN-118 | 确认可调齿轮箱转速表量程公式不同。 | 原版 speed×16；轴另用 speed×power。 | 当前两者都用 speed×power，齿轮箱变为 speed²/4，青铜巧合相同而木／钢不同，见 [报告](SENSOR_SOURCE_AUDIT.md)。 |
| ALIGN-119 | 确认中子计未接燃料棒／冷却剂上限。 | 原版逐槽调用棒的 neutron maximum，2×2 累加。 | 当前一律 Integer.MAX_VALUE；原始计数已有来源，快照时序另待核，见 [报告](SENSOR_SOURCE_AUDIT.md)。 |
| ALIGN-120 | 确认测重使用加工载荷且大存储读数受单堆视图限制。 | 原版优先宿主重量，再按物品物理组成乘完整数量。 | 当前 CrucibleItemInput 包含回收资格／矿石产量，WEIGHT 没有 ITEM 已有的完整计数特判；单位换算本身对应，见 [报告](SENSOR_SOURCE_AUDIT.md)。 |
| ALIGN-121 | 确认扫描仪未接物理／进度诊断来源。 | WD.scan 有重量、温度、压缩体积和进度段。 | 当前遗漏独立接口读取，连已有 CompressionSensorSource 也未用；不能称设备本身无压力，见 [报告](SCANNER_BEHAVIOR_AUDIT.md)。 |
| ALIGN-122 | 确认扫描仪按通用控制对象虚增状态段和费用。 | 原版各独立接口决定段资格，每段 512 EU。 | USB supportsSwitch=false 仍被显示为 ON 并有 Running 段；原版仅模式。完整总价另受其他段影响，见 [报告](SCANNER_BEHAVIOR_AUDIT.md)。 |
| ALIGN-123 | 确认扫描仪缺少逐段异常隔离。 | 原版各段独立捕获后继续诊断。 | 当前本地调用链没有恢复；异常可中断扫描，未故障注入或复现崩溃，见 [报告](SCANNER_BEHAVIOR_AUDIT.md)。 |
| ALIGN-124 | 确认扫描缺少方块调试与可修改事件合同。 | 原版自身多方块接口提供 debug，末尾事件可改文本／费用并取消结果。 | 当前直接返回固定诊断，没有提供者／事件调用；现代扩展桥接待设计，见 [报告](SCANNER_BEHAVIOR_AUDIT.md)。 |
| ALIGN-125 | 确认通用放大镜吞掉过滤器专属诊断。 | 原版报告物品／元数据／通配或实际流体及空名单。 | 当前只报正常／反向和强／弱即取消事件；机械臂和物流有独立路径，不算全部失效，见 [报告](CROP_MAGNIFIER_AUDIT.md)。 |
| ALIGN-126 | 确认部分面板／库存工具损耗未遵循返回成本。 | 原版按目标返回 ×100/10000 向上取整，操作成本各异。 | 当前放大镜不扣、配置固定 1；大容量存储三种模式工具已在 2026-10-20 按原返回 10000 换算修复，单块换热器cold0／hot1000的可达专用入口请求10／1已扩围，通用冷罐拦截另列221；见[换热器](HEAT_EXCHANGER_AUDIT.md)。其余入口仍待接齐，不应全局乘 100；见[面板报告](CROP_MAGNIFIER_AUDIT.md)和[库存报告](MASS_STORAGE_CONTROL_AUDIT.md)。 |
| ALIGN-127 | 确认易燃起火器少一次普通概率判定。 | 原版增强判定失败后仍试基础概率；默认组合 75.5%。 | 当前只抽一次 65%；普通 30%／低耐久 100% 已对应，见[报告](IGNITION_EXTINGUISHER_AUDIT.md)。 |
| ALIGN-128 | 确认一次性点火物创造消耗未豁免。 | 原版失败或有效目标处理后均检查无限物品资格。 | 当前 singleUse 直接 usedUp；多次打火机已有豁免，见[报告](IGNITION_EXTINGUISHER_AUDIT.md)。 |
| ALIGN-129 | 确认苦力怕点火回调不同。 | 原版为服务端左键实体拦截。 | 当前多物品右键，起火工具 hurtEnemy；完整攻击效果未世界验证，见[报告](IGNITION_EXTINGUISHER_AUDIT.md)。 |
| ALIGN-130 | 确认燃烧室点火／灭火工具分派不完整。 | 原版三类宿主识别两个工具钩子。 | Forge 缺随机工具点火接口，Neo 有该接口；两版都缺喷罐灭火接口，见[报告](IGNITION_EXTINGUISHER_AUDIT.md)。 |
| ALIGN-131 | 确认原生点火返回零后错误兼容回退。 | 原版按目标类型选择原生或 ToolCompat，不以返回值切换。 | 当前零成本后仍点 TNT／相邻空气；已燃雷管可进入此路径，未世界复现，见[报告](IGNITION_EXTINGUISHER_AUDIT.md)。 |
| ALIGN-132 | 确认部分物品／库存交互缺少反馈声音。 | 原版起火尝试、成功喷射／封装和瓶框成功取放分别主动发送声音。 | 当前对应回调未调用；雷管、自动点火器、书架取物等已有声音，不列为全局无声。见[工具报告](CONSUMABLE_FEEDBACK_AUDIT.md)及[瓶框补充](BOTTLE_BOOKSHELF_AUDIT.md)。 |
| ALIGN-133 | 确认原版起火概率配置未接入。 | `GT6_Main` 读取 1–100 的 `FlintAndSteelChance`，默认 30。 | GT 工具固定传默认 30；默认一致，配置收尾仍按先前安排延后，见[报告](CONSUMABLE_FEEDBACK_AUDIT.md)。 |
| ALIGN-134 | 确认胶带创造预算及扣量与原版不同。 | 无限物品玩家使用整卷预算，成功不减剩余量。 | 当前传已存余量且成功照扣；大容量存储存在真实预算准入路径，见[报告](CONSUMABLE_FEEDBACK_AUDIT.md)。 |
| ALIGN-135 | 确认部分零余量耗材未完成空形态转换。 | 喷罐／胶带在目标无反应后仍统一检查耗尽。 | 创造喷罐受非创造分支限制，胶带提前退出；非创造喷罐已对应，见[报告](CONSUMABLE_FEEDBACK_AUDIT.md)。 |
| ALIGN-136 | 确认大容量存储缺少钳子取物与软锤清空入口。 | 原版分别处理背包转移／分批正面掉落及余料。 | 两版方块／实体及工具入口未补齐，提示仍列出这些操作；按钮和外部抽取不是替代，见[报告](MASS_STORAGE_CONTROL_AUDIT.md)。 |
| ALIGN-137 | 2026-10-20 玩家反馈修复：自动下送／溢出执行已补齐。 | 位 1／4 切换、ticker、容量 +256、超额下送、底面输入门控及封装时模式切换已接入。 | NeoForge 定向运行验证真实活动扳手、下方箱子输出和溢出；其他存储工具／覆盖板问题另列，见[修复记录](verification/storage-direction-colors-20261020.md)。 |
| ALIGN-138 | 确认大容量存储遗漏正面覆盖板门控。 | 原版任何正面覆盖板阻止普通取放，不透明板另隐藏显示。 | 普通装饰板的事件链仍 PASS 到按钮；渲染也缺可见性判断，未客户端验收，见[报告](STORAGE_COVER_BOUNDARY_AUDIT.md)。 |
| ALIGN-139 | 确认部分宿主的覆盖板范围不同。 | 抽屉／保险箱禁正面，书架禁正背，瓶框禁所有面、大型涡轮禁正面，普通箱原本无 Coverable。 | 当前通用安装或箱子本体允许原禁用范围；不能混同大容量存储的“允许贴板后阻挡”。见[抽屉](STORAGE_COVER_BOUNDARY_AUDIT.md)、[保险箱](SAFE_LOCKER_AUDIT.md)、[书架／瓶框](BOTTLE_BOOKSHELF_AUDIT.md)和[箱子](CHEST_LOOT_AUDIT.md)。大型涡轮正面禁贴也未由后备host保留，见[大型涡轮](LARGE_TURBINE_AUDIT.md)。 |
| ALIGN-140 | 确认保险箱覆盖板操作遗漏宿主锁权限。 | 原版普通／工具覆盖板入口先调用 allowInteraction，检查认主或开锁。 | 当前事件提前消费，只有世界建造权限，没有 Safe 的锁门控；不等于世界保护失效或外部能力盗取内容，见[报告](SAFE_LOCKER_AUDIT.md)。 |
| ALIGN-141 | 确认机械箱首次非正面普通右键不认主。 | 原版先 allowRightclick／allowInteraction 认主，随后限制 GUI 正面。 | 当前先非正面 PASS；现有背面不认主测试固定了不同合同，不作为对齐证据，见[报告](SAFE_LOCKER_AUDIT.md)。 |
| ALIGN-142 | 确认保险箱和普通箱遗漏点击面邻块阻挡查询。 | 原版 Base04 普通入口查询 WD.obstructed；保险箱工具路径也查询。 | 当前锁／工具和普通箱顶面开盖条件不能补该查询；下半砖示例未运行，不推定所有方块缺失，见[保险箱](SAFE_LOCKER_AUDIT.md)和[箱子](CHEST_LOOT_AUDIT.md)。 |
| ALIGN-143 | 确认缺少充电衣柜家族。 | 原版 60 材料各有普通／充电衣柜，后者可向四槽注能。 | 两版只注册普通 60 家族且实体没有充电接口；普通衣柜不缺注册，见[报告](SAFE_LOCKER_AUDIT.md)。 |
| ALIGN-144 | 确认衣柜更衣面与装备门控不同。 | 原版限正面，按槽校验盔甲并跳过特定可选背包。 | 当前任意面直接交换四槽；没有自动化入口会遮蔽部分非法装备例子，见[报告](SAFE_LOCKER_AUDIT.md)。 |
| ALIGN-145 | 确认衣柜缺少外部库存、变更通知及对应状态来源。 | 原版四槽全侧自动化、装备过滤、邻居通知和含物品运行读数。 | 私有 armor 数组未开放能力，setChanged 不通知相邻库存，覆盖板也没有内容状态来源；不是未保存装备，见[报告](SAFE_LOCKER_AUDIT.md)。 |
| ALIGN-146 | 确认旧衣柜混料配方仍实际装载。 | 原版各部件同材料。 | 旧 hand/storage/locker 用铋箱与钢部件产出不锈钢衣柜；正确 survival_repairs/locker 为另一个 ID，不会覆盖，见[报告](SAFE_LOCKER_AUDIT.md)。 |
| ALIGN-147 | 确认瓶框／书架缺少失效物品的槽位替补。 | 原版恢复原物品及 ore 字典均失败后分别替换空瓶／普通书。 | 两版直接调用 ItemStackHandler 解码，没有默认槽位替补；未旧档重启或导入原始 1.7.10，见[报告](BOTTLE_BOOKSHELF_AUDIT.md)。 |
| ALIGN-148 | 确认书架遗漏魔法材料附魔基底。 | 原版返回 MAGICAL 的额外 1，加内容书点／12。 | 当前只返回内容书点／12；普通书贡献已有对应，不应全局加 1，见[报告](BOTTLE_BOOKSHELF_AUDIT.md)。 |
| ALIGN-149 | 确认内置书架陈列物登记未接入。 | 原版 BOOK_REGISTER 实际登记选择电路、切片刀／模具、USB／HDD。 | 真实物品存在但固定白名单拒绝，手动／自动插入均受影响，显示映射也不完整；电路无书架模式功能，见[报告](BOTTLE_BOOKSHELF_AUDIT.md)。 |
| ALIGN-150 | 确认书架放大镜只显示名称。 | 原版客户端包展示完整物品 tooltip，空列表才回退名称。 | 当前只发 getHoverName，附魔书详情未接；工具成本另归 ALIGN-126，见[报告](BOTTLE_BOOKSHELF_AUDIT.md)。 |
| ALIGN-151 | 确认创造模式书架插入消耗不同。 | 原版成功插入一件后直接扣手持数量。 | 当前创造不扣、生存扣；属于待统一决策的明确差异，不能把瓶框等宿主全部改为同一规则，见[报告](BOTTLE_BOOKSHELF_AUDIT.md)。 |
| ALIGN-152 | 确认箱子缺少钳子批量转移。 | 原版合并后按物品堆叠／NBT 等五阶段回退并按搬运量返回。 | 当前只接扳手，钳子物品与提示不代表有箱子分派；不同于大容量算法，见[报告](CHEST_LOOT_AUDIT.md)。 |
| ALIGN-153 | 确认箱子原生比较器缺失。 | 原版使用库存计算比较器值。 | 当前没有 analog 覆写，覆盖板强弱输出不能补原生库存读数，见[报告](CHEST_LOOT_AUDIT.md)。 |
| ALIGN-154 | 确认金属箱／专用 loot 箱的物品栈上限不同。 | 原版两者及强化木均为 16。 | 当前金属和 loot 默认 64，强化木仍 16；不是槽内物品上限，见[报告](CHEST_LOOT_AUDIT.md)。 |
| ALIGN-155 | 确认原有 trapped 场景的开盖信号未接入。 | 原世界箱替换实际写 trapped=true，打开时强弱信号 15。 | 当前静态 isLootChest 无消费，openers 仅控制盖子；普通无陷阱箱不应因此统一加信号，见[报告](CHEST_LOOT_AUDIT.md)。 |
| ALIGN-156 | 确认箱子喷漆未接专用模型和掉落保存。 | 原版模型使用已喷颜色，父类保存该状态。 | 当前已有 paint 状态但专用模型固定材料色、掉落只存 loot；非所有基础 RGB 都缺失，见[报告](CHEST_LOOT_AUDIT.md)。 |
| ALIGN-157 | 确认专用战利品箱原型参数不同。 | 原石箱为 Stone、硬 5／抗爆 14。 | 当前专用 loot_chest 全用 Steel、硬 4／抗爆 6；不能推广成世界所有箱子都要石质，见[报告](CHEST_LOOT_AUDIT.md)。 |
| ALIGN-158 | 确认普通箱子掉落携带默认无用 loot 数据。 | 原版仅写有效 pending 表及真实自定义状态。 | 当前无条件写 false／0 再附实体数据，拆下物品与新合成不等价；实际堆叠未运行，不能误删防重掷标记，见[报告](CHEST_LOOT_AUDIT.md)。 |
| ALIGN-159 | 确认表抽取次数的上界不同。 | 原 Forge getCount 对 8／24 实际抽 8–23 次。 | 当前共用 inclusive count 可抽 24；单堆数量本来闭区间，不应一起改变，见[报告与官方来源](CHEST_LOOT_AUDIT.md)。 |
| ALIGN-160 | 确认一次性 crate 的抽样层次不同。 | 原版直接抽一个加权条目。 | 当前完整现代表输出后均匀选非空堆；现代数据内容变化本身不算缺失，适配合同待明确，见[报告](CHEST_LOOT_AUDIT.md)。 |
| ALIGN-161 | 确认成功但空的首次生成不发经验。 | 原版成功即发五颗经验并清表名。 | 当前空成功结果标记消费但仅 placed>0 发经验；缺失表早退另算，普通非空首次已有对应，见[报告](CHEST_LOOT_AUDIT.md)。 |
| ALIGN-162 | 确认普通外部爆炸遗漏箱子／书架既有库存的逐槽随机清除。 | 原 Base05 在回调中逐槽以 2/3 概率 trash，再拆除散落余物。 | 两版本实际原生链进入 onRemove 后均直接遍历全部内容；书架待生成 loot 和先打包的瓶框不能套用同一结论，见[报告](INVENTORY_EXPLOSION_AUDIT.md)。 |
| ALIGN-163 | 确认金属箱／瓶框自定义方块掉落未应用爆炸衰减门槛。 | 原 getDrops 后，整件候选仍按传入 chance 决定生成；瓶框库存先装进该候选。 | 当前 getDrops 直接返回且不消费爆炸半径；限启用破坏衰减的普通入口，不等同逐槽损失或最终实体存活率，见[报告](INVENTORY_EXPLOSION_AUDIT.md)。 |
| ALIGN-164 | 确认女王寿终没有清空旧死蜂槽。 | 原普通／高级箱对死蜂槽中的蜂调用 slotKill 置空。 | 当前 kill 后写回旧槽，尸体继续占用死亡输出空间；不是扁平 ID 问题，见[报告](BUMBLE_LIFECYCLE_AUDIT.md)。 |
| ALIGN-165 | 确认空主雄蜂槽会接走唯一公主并阻断晋升。 | 原主槽为空时短路，将后代放备用槽，再从备用槽挑公主。 | 当前总先尝试主槽；通过环境检查的唯一公主进主槽后不在晋升扫描范围，两种养蜂箱共用，见[报告](BUMBLE_LIFECYCLE_AUDIT.md)。 |
| ALIGN-166 | 确认鲜基因雨／雷阈值有细小截断差异。 | 原整数随机值直接与 rainfall×10000 浮点阈值比较。 | 当前先转 int；有天空且正的非整数阈值处于随机范围内时少一个可通过点，实际任意群系 downfall 可到达；不是所有天气概率均错误，见[报告](BUMBLE_LIFECYCLE_AUDIT.md)。 |
| ALIGN-167 | 确认无基因蜂的默认温度公式不同。 | 原 WD.envTemp(plains) 包含 −3 和截断，本机原生类确认平原温度 .8，得到 286K。 | 当前 random 回退为 289K；限制该入口，不推为所有自然巢温度固定，见[报告](BUMBLE_LIFECYCLE_AUDIT.md)。 |
| ALIGN-168 | 确认专用战利品箱的填槽／覆盖合同不同。 | 原加权条目生成堆后随机选槽直接写入，允许碰撞覆盖。 | 当前 GTLootTables.fillInto 每次填首空槽、不覆盖；地牢 dungeonLoot 走另一个现代表分支。独立于抽取次数和一次性 crate 选取层次，见[报告](LOOT_SLOT_AUDIT.md)。 |
| ALIGN-169 | 确认自然蜂巢遗漏普通火焰消费／传播参数。 | 原实体两个接口均返回 300，GT 方块和 Forge 火焰调用有实际转发。 | 当前继承默认 FireBlock 映射且没有登记该方块；ignitedByLava 是另一入口，不能替代，见[报告](BUMBLE_HIVE_PHYSICAL_AUDIT.md)。 |
| ALIGN-170 | 确认自然蜂巢遗漏末影龙破坏保护。 | 原 canEntityDestroy 拒绝 EntityDragon，旧 Forge 龙破坏链实际查询。 | 当前无覆写、immune／transparent 标签或专用事件补偿；限默认数据、允许生物破坏且无外部取消，未游戏复现，见[报告](BUMBLE_HIVE_PHYSICAL_AUDIT.md)。 |
| ALIGN-171 | 确认养蜂寿命／配对倒计时存在条件性保存回退。 | 原养蜂箱继承 NeedsSaving 标记，全局每 20 tick 调 WD.mark 将区块标脏。 | 当前纯 tick 改字段无对应补偿，两版保存跳过干净区块；库存／开箱等变化仍可保存，正常 brood 切换不算全丢，见[报告](BUMBLE_PERSISTENCE_AUDIT.md)。 |
| ALIGN-172 | 确认箱子专用邻库通知的发送／接收链未完整接入。 | 原箱变更后六面广播，漏斗、筛粉漏斗、机器等实际消费。 | 当前正常 setChanged 的原生 onNeighborChange 不等于专用回调或普通 neighborChanged；现有 20／100 tick 等重试仍在，不能断言永久卡死，见[报告](INVENTORY_NOTIFICATION_AUDIT.md)。 |
| ALIGN-173 | 确认共用菜单部分 shift 转出绕过自身库存回调，扩展至六类宿主。 | 原容器和槽明确设置库存变化标志。 | 槽许可通过、背包仅接纳部分且源堆非空时只通知 SlotItemHandler 的空容器；全部转空正常 setter。箱子、保险箱、抽屉、两种漏斗及垃圾桶同源；Queue 限合法末格／多件模式，工作台储物页、电池盒、垃圾场另有补偿或限制。Neo 合并已有堆另有入口，Forge 有例外，见[共用菜单核查](SHARED_MENU_AUDIT.md)。 |
| ALIGN-174 | 确认灌木雨天加速额外受现代群系降雨判定限制。 | 原 Bush 在见天空时查询全局雨与高度 helper，没有降雪／干燥群系门控。 | 当前 Level.isRainingAt 还要求群系降水为 RAIN；合法手动种在降雪／干燥群系时可能少一次增长，普通降雨群系已有对应，见[报告](BERRY_BUSH_AUDIT.md)。 |
| ALIGN-175 | 确认灌木屋下光照 helper 实际行为不同，原版截断可能属于历史缺陷，需明确取舍。 | 原 getLightLevel 在乘15前先把亮度转 long；普通原生亮度表0–14均小于1，实际满亮度才通过。 | 当前直接接受原始光照>9，10–14亦可生长。不在排版中盲目恢复原截断，第三方亮度表另有边界，见[报告](BERRY_BUSH_AUDIT.md)。 |
| ALIGN-176 | 确认灌木支撑／父种变化缺少原版邻居变化后的即时重查。 | 原邻居回调设 mBlockUpdated，下个服务端 tick 重查父块／种类或掉落。 | 当前没有 neighborChanged／updateShape 入口，首次后在128周期的两个相位重查，可等待约64 tick；已有失支撑掉落，不是永久悬空，见[报告](BERRY_BUSH_AUDIT.md)。 |
| ALIGN-177 | 确认自有六种彩色草未进入灌木默认可种地面集合。 | 原自有 BlockGrass 加入 plantableGreens，Bush 消费该集合或花的土壤接口。 | 两版实际注册 GTGrassBlock 能支持花，但 Bush 只查 DIRT/MOSS/FARMLAND；自带静态／动态标签无六草补偿，外部数据包可改变准入，见[报告](BERRY_BUSH_AUDIT.md)。 |
| ALIGN-178 | 确认灌木未接入放大镜 Grows 输出读数。 | 原 TOOL_magnifyingglass 先显示输出并返回，不采收。 | 两版实际放大镜目标路径均拒绝非 MultiblockToolTarget 的灌木，无事件补偿；有效输出的普通非潜行点击可进入成熟采收。物品 tooltip 不替代世界工具读数，见[报告](BERRY_BUSH_AUDIT.md)。 |
| ALIGN-179 | 确认队列漏斗手动 GUI 误用自动化仅末格提取限制。 | 原 GUI 各合法槽可取，侧面首入／末出通过独立方法限制。 | 当前 raw handler.extractItem 同时用于 Slot.mayPickup，非末格普通点击／正常 Shift 均被拒；原生 doClick 在 quickMoveStack 前检查许可。与173的操作后回调不同，见[报告](SHARED_MENU_AUDIT.md)。 |
| ALIGN-180 | 确认液态酸工厂识别／继承错配，多生成固态及部分宝石形态。 | 原19条lqudacid族只继承液体容器／植物形态；通用elec/cent是处理方式，不是宝石家族。 | 生成器漏液态酸族，16条变dust、2条elec、1条cent；后两类再继承gem。形态侧表只加不减，实际物品目录继续接纳19条粉、其中3条宝石及零件。酸性／成分19条和流体18条已有补偿，见[报告](LIQUID_ACID_FACTORY_AUDIT.md)。 |
| ALIGN-181 | 确认氟硅酸误带MAGICAL，进入本不属于原酸的管道魔法危险路径。 | 原H2SiF6的lqudacidelec授予电解标志，没有MAGICAL。 | 当前elec额外put(MAGICAL)，实际生成流体关联该材料，FluidHazards.isMagic→checkSafety在非耐魔管道有漏失／中毒／随机移管消费。没有游戏复现，黑矾尚无液体不算已触发样本，见[报告](LIQUID_ACID_FACTORY_AUDIT.md)。 |
| ALIGN-182 | 确认黑矾遗漏液态补偿及本模组流体注册。 | 原8403 BlackVitriol显式LIQUID并经液态酸工厂提供流体容器形态。 | 另外18条酸在prepare中补LIQUID，唯8403没有；FluidCatalog只消费实际GAS/LIQUID属性，不读侧表字符串，全生产搜索亦无独立同名流体入口。外部附属另算，见[报告](LIQUID_ACID_FACTORY_AUDIT.md)。 |
| ALIGN-183 | 确认灌木默认硬度／抗爆被最后的instabreak同时清零。 | 原硬度0.5，实际抗爆由实体覆写为0.2，非注册参数0.3。 | 两版真实Properties调用链均instabreak→strength(0)→strength(0,0)；当前最终0/0。没有实际挖掘／爆炸测量，见[物理核查](BERRY_BUSH_PHYSICAL_AUDIT.md)。 |
| ALIGN-184 | 确认灌木普通火消耗／传播300/300未接现代查询。 | 原实体两接口及宿主转发实际供火查询，各返回300。 | 当前无覆写或FireBlock对象表登记，原生未知方块默认0/0；不是绝对无法被其他机制破坏，见[物理核查](BERRY_BUSH_PHYSICAL_AUDIT.md)。 |
| ALIGN-185 | 确认灌木核心的墙内检测合同不同，限实体眼部嵌入条件。 | 原aBush两种normal-cube接口均false，旧墙内检测不把它当实心墙。 | 当前noOcclusion未禁isSuffocating；无覆盖板核心满碰撞形状可命中现代眼部求交／inWall伤害，枝条不满足。普通走碰不是荆棘，光遮挡与基础形状已有对应，见[物理核查](BERRY_BUSH_PHYSICAL_AUDIT.md)。 |
| ALIGN-186 | 确认灌木种类分区因半坐标整数除法改浮点而改变。 | 原aX/2,aZ/2先整数截断；相邻340/341在z128必同噪声输入。 | 相同源表的有限binary32推导在seed0得到原8/8、当前8/2；不依赖原HashMap顺序，也不是默认seed地形实测或全球频率结论，见[世界生成核查](BERRY_BUSH_WORLDGEN_AUDIT.md)。 |
| ALIGN-187 | 确认灌木地表扫描被上方原木提前截断。 | 原WorldgenOnSurface跳过wood/leaves，再找可种地面。 | 当前共用surface遇LOGS即返回，灌木随后拒绝该地面；悬枝原木上方／下方有合法空气与土壤时产生确定列差异，不称所有树林无灌木，见[世界生成核查](BERRY_BUSH_WORLDGEN_AUDIT.md)。 |
| ALIGN-188 | 确认灌木群系门控范围不同，现代映射需明确取舍。 | 原读取整个16×16区块群系名集合，任一合格非冻结plains/woods即通过；原生Taiga不在名单。 | 当前抽列后只查海平面ID并用固定名单，明确允许taiga；混合／垂直群系亦非等价。新增现代群系不直接判应删除，见[世界生成核查](BERRY_BUSH_WORLDGEN_AUDIT.md)。 |
| ALIGN-189 | 确认通用分解以全元素启发式替代原准入与处理地图。 | 原DECOMPOSABLE及独立CENTRIFUGE/ELECTROLYSER控制地图；青铜uumAloy实际追加离心，Lazurite原elec。 | 当前只查组成／分母／反物质，再按直接组分ELEMENT选唯一地图：青铜改电解、Lazurite改离心，盐粉还可能新增原未授地图的候选。不是最终配方枚举，见[分解核查](MATERIAL_DECOMPOSITION_AUDIT.md)。 |
| ALIGN-190 | 确认通用分解只尝试dust，未接原流体／气体和粉块输入合同。 | 原优先实际liquid/gas绑定及各自量纲换算，无流体再回退粉；另有Glowstone／blockDust分支。 | 当前输入无dust立即跳过，NO／甘油为具体非酸样本；盐水另有显式流体电解，不能报全部缺失。selector0缺少与现代最小输入放宽另有边界，见[分解核查](MATERIAL_DECOMPOSITION_AUDIT.md)。 |
| ALIGN-191 | 确认通用分解把原功率表达式放到时长，并固定功率。 | 原签名明确EUt在duration之前；公式分别max(16,total×14/U)和ceil(total×292/U)。 | 当前EUt固定64/16、duration用前一公式；青铜优化前原56/1168、当前64/56。结果还受优化／校验／碰撞影响，日志added不等于最终条数，见[分解核查](MATERIAL_DECOMPOSITION_AUDIT.md)。 |
| ALIGN-192 | 确认通用分解未复用原生水解析，使部分含水配方整条跳过。 | 原Water实际绑定vanilla water；CobaltHexahydrate离心输出钴和6000mB水。 | 当前只找GTFluids.still holder；水复用原生实例所以无holder，且无水粉，complete=false跳过。别处正确水helper未被此入口调用，不是水注册缺失，见[分解核查](MATERIAL_DECOMPOSITION_AUDIT.md)。 |
| ALIGN-193 | 确认通用分解输出相态按标志取代原温度／真实流体绑定。 | 原按293K环境与组件熔点调用实际fluid绑定，MethaneIce的Ice产物为近冰点水。 | 当前Ice无GAS/LIQUID直接落到冰粉，虽已有独立Ice流体；原显式分母2与总组分3不同是原合同，不误改分母。pulver目标和全部相态仍待扩核，见[分解核查](MATERIAL_DECOMPOSITION_AUDIT.md)。 |
| ALIGN-194 | 确认Forge光莲注册清空普通薄片碰撞。 | 原自有光莲保留非船实体的薄片；已采用现代睡莲政策也要求原生碰撞。 | Forge活跃工厂noCollission使默认getCollisionShape返回空；Neo由前置GTPlants保留现代碰撞，后备工厂不属活跃所有者。不恢复旧形状／船政策，见[光莲](GLOWTUS_AUDIT.md)。 |
| ALIGN-195 | 确认Forge光莲植物类型回退额外接受泥土／耕地。 | 原要求水源；现代睡莲政策保留自身水／冰准入。 | IPlantable只将精确vanilla LILY_PAD归WATER，GT默认PLAINS；真实canSustainPlant在mayPlaceOn失败后接受DIRT／FARMLAND，水面物品仍会走此分支。Neo没有该回退，GT水已有一般兼容，见[光莲](GLOWTUS_AUDIT.md)。 |
| ALIGN-196 | 确认木棍群系倍数的额外列被概率双重门控。 | 原先抽2×倍数列并去重，再每列1/2。 | 当前外层1/2成功才为额外列各roll1/2；理想全森林平地不同有效列期望约2.97→1.99，非世界密度测量，不与既有surface／biome差异重复，见[地表堆](SURFACE_PILES_AUDIT.md)。 |
| ALIGN-197 | 确认层岩指示石未关联实际接触层／最后指示矿物。 | 原保留tLastRock／tLastOre，在露头处选择实际材料或crushing目标。 | 当前整列结束后用scan[3]，即噪声Y112；131项表的有限坐标可使Y64 Limestone露头摆Gneiss。Ore记录亦未消费indicators／最后矿物；不报整个层岩机制缺失，见[地表堆](SURFACE_PILES_AUDIT.md)。 |
| ALIGN-198 | 确认基岩指示石右键额外给一份材料岩石。 | 原mRock明确存oreRaw×1，采收只给这一份。 | 当前基岩入口用material+rawOre模式，yields先给rockGt再给oreRaw；普通陨铁的explicit-item模式已正确提前返回，不一并误报，见[地表堆](SURFACE_PILES_AUDIT.md)。 |
| ALIGN-199 | 确认自然石堆破坏未接Fortune数量。 | 原自然石getDrops读取1+rng(1+fortune)，手摆石则不放大。 | 当前RockBlock.getDrops未读真实TOOL／Fortune，只用BE count；Twig已有正确工具入口。自然与手摆explicit模式尚须分清，不能直接给全部物品放大，见[地表堆](SURFACE_PILES_AUDIT.md)。 |
| ALIGN-200 | 确认普通石堆失去下方支撑未接邻居移除链。 | 原邻居变化检查支撑并掉物移除，实际方块分派已核。 | Rock只有canSurvive，默认原生updateShape原样返回、neighborChanged不重验；无其它本模组补偿。Twig已有updateShape，不误报同缺口，也不扩大到全部液体流入场景，见[地表堆](SURFACE_PILES_AUDIT.md)。 |
| ALIGN-201 | 确认普通木棍／燧石潜行手摆未找到内置对应分支。 | 原GT_Proxy将stick族／vanilla flint放为手摆堆；手摆木棍固定掉普通stick。 | 当前RockItemPlacement仅rockGt／oreRaw，全局Pile事件其后仅锭／板／宝石板；vanilla stick／flint统一信息不满足这些门槛。其它石材手摆已有，不排除外部事件提供者，见[地表堆](SURFACE_PILES_AUDIT.md)。 |
| ALIGN-202 | 确认空核棒挤压的直接输入前缀收窄。 | 原Extruder按合格U64..U前缀且整除U构造Zr输入，例如4小粉／9小搓粉。 | 当前ReactorRodRecipes仅ingot／dust，匹配仍要求同前缀；先拼整粉不等于原直接路线，通用挤压011另属PartRecipeLoader，见[核棒](NUCLEAR_ROD_AUDIT.md)。 |
| ALIGN-203 | 确认空核棒挤压工时用密度替代原每U重量。 | 原max(64,trunc((melt−293)×(1+weight(U))/6144))，Zr每U约722.89kg，216tick。 | 当前直接getDensity使Zr落最低64tick；96EU/t、1U原料与1空棒仍对应，未枚举最终配方表，见[核棒](NUCLEAR_ROD_AUDIT.md)。 |
| ALIGN-204 | 确认棒专属动态／解释提示未完整移植。 | 原核棒慢化、分钟与冷却剂改参轮播，增殖目标／loss及各类反应说明。 | 当前有定义栈只显示kind、裸Remaining和核棒基础四参数，缺对应动态／解释；与003硬编码语言问题分开，物理公式并非因此缺失，见[核棒](NUCLEAR_ROD_AUDIT.md)。 |
| ALIGN-205 | 确认两种额外遗留棒可见但没有当前反应定义。 | 原GT目录46项，不含两种旧移植MOX／Am243物品；IC2自己的MOX不作GT定义证明。 | 当前共48注册，两旧FuelType回落definition=null、Core拒入；实际OriginCreativeContents扫描仍展示，不能用旧创页filter说已隐藏，见[核棒](NUCLEAR_ROD_AUDIT.md)。 |
| ALIGN-206 | 确认核心物品自动化缺少上下侧限制。 | 原1x1／2x2仅上下访问，且核心或槽停止才可插拔。 | 当前两端对任意Direction提供同一items，保留停止槽门控；无覆盖板补偿，真实漏斗查询可从水平输入。不是漏斗水平吸取结论，见[核心](NUCLEAR_CORE_AUDIT.md)。 |
| ALIGN-207 | 确认核心冷却／故障早于旧热液排出，可能误判堵塞。 | 原普通tick先推热液，服务器后置阶段才反应／冷却。 | 当前coolHeat/fail后才pushFluids；满工业热冷却液且邻罐可收时，原能先腾空间，当前可先清棒。独立于095Steam空罐扩容，保留不地形爆炸政策，见[核心](NUCLEAR_CORE_AUDIT.md)。 |
| ALIGN-208 | 确认核心运行派生状态与消费者不同。 | 原mRunning汇总周期通量或reaction布尔，控制检测与接触同读它。 | 当前控制只lastHeat>0，接触只!stopped&&!failed；有效零热棒检测偏窄、开启空核接触门控偏宽。伤害限实际实体重叠入口，两个表现合为一项，见[核心](NUCLEAR_CORE_AUDIT.md)。 |
| ALIGN-209 | 确认核中子邻接未保留Universal Extender委托路径。 | 原getAdjacentTileEntity沿通用扩展器delegate与返回面解析目标核心。 | 当前网络只查本tick核心的直接相邻BlockPos，不把扩展器解析为目标；直连四向／槽位已对应，不报所有扩展器或普通邻核失效，取舍待定，见[网络](NUCLEAR_NETWORK_AUDIT.md)。 |
| ALIGN-210 | 确认共享辐射消费者缺原creative／IronGolem明确豁免。 | 原applyRadioactivity调用RadioHazmat helper，先豁免creative、原IronGolem／Wither。 | 当前creative会积持久dose；IronGolem缺显式豁免并尝试addEffect，未宣称六药效均生效或创意免伤失效。Wither已有undead保护，中心采样另列政策边界，见[网络](NUCLEAR_NETWORK_AUDIT.md)。 |
| ALIGN-211 | 短暂停启／取出立即清通量是已确认的更保守政策差异，保留现行为，不列作应恢复旧余通量的缺陷。 | 原软锤／on-off只改stopped，取出不清核心m/o；19后至18前短暂停再开可沿用旧通量。 | setStopped(true)／removeRod清neutrons，非19重启到下次交换前为0；给定U235+IC2为128／0，见[状态核查](NUCLEAR_TRANSITION_AUDIT.md)。 |
| ALIGN-212 | 确认禁用／停止慢化棒跨交换周期时提前清旧接触倍数。 | Moderator计数只在受活动门控的reaction于19提交，继承updateModeration为空。 | Network对所有rods无条件finishCycle；无接收时旧contacts=1变0，再启用首次正束反射A／0；正常连续工作不因此失效，见[状态核查](NUCLEAR_TRANSITION_AUDIT.md)。 |
| ALIGN-213 | 确认漏过交换后重载的单数组状态不足以表达原m/o分离。 | 18后可保存m=0/o=N，漏19后非交换相位恢复，先o=m，反应热0。 | 当前活动／current-version保存单N，gt.stopped=false恢复后非19仍以N反应；只限完整卸载漏交换条件，正常18误报仍排除，见[状态核查](NUCLEAR_TRANSITION_AUDIT.md)。 |
| ALIGN-214 | 确认END前中止且再无该Level排空时，弱key未提供有效生命周期兜底。 | 原强列表也非完美停服清理；以后若再有全局END可移除dead成员。 | PENDING强value→Core→Level反持key，真实Post非finally、Unload无本类订阅；正常先remove／本handler异常明确不留key。未测堆增长，见[网络生命周期](NUCLEAR_NETWORK_LIFECYCLE_AUDIT.md)。 |
| ALIGN-215 | 确认post反应的逐核心异常隔离边界缺失。 | 原GT_API_Proxy逐项catch回调Throwable、移相位表并记录后继续；不保证catch自身再抛或全部原tick。 | exchange／react在两端END无隔离，总线会重抛，给定E可中断后续核心；外部fill可达但原fill属普通tick，不能声称同一fill原被post保护。未注错／复现崩服，见[网络生命周期](NUCLEAR_NETWORK_LIFECYCLE_AUDIT.md)。 |
| ALIGN-216 | 确认核棒行为接入由任意Item回调收窄为FuelRodItem＋固定Kind。 | 原IItemReactorRod被手动／自动准入、收发／反应／慢化真实消费，回调获得栈／槽／核心。 | 当前静态RodPhysics只读definition并按八Kind执行；public Rod构造器仍允许第47种参数棒，自行原生注册可用，但不是等价行为回调或支持SDK。未证明实际addon损坏，见[核扩展](NUCLEAR_EXTENSION_AUDIT.md)。 |
| ALIGN-217 | 确认实现类路线自定义棒的耗尽产物受封闭目录限制。 | 原反应回调可直接改槽为自有产物，不强制内置目录。 | 新NUCLEAR棒life100／非慢化flux100≤max，product10001未进ALL，耗尽先byOriginal得null→EMPTY，外部原生注册B也不查；内置产物仍可工作，见[核扩展](NUCLEAR_EXTENSION_AUDIT.md)。 |
| ALIGN-218 | 确认已核14种能源设备默认堆叠上限16→64。 | 原两太阳能、四泵、八大型蒸汽／燃气涡轮均注册stack16并被原Item消费。 | 当前对应plain BlockItem默认64；只核这14项，未实测库存，见[太阳能](SOLAR_PANEL_AUDIT.md)、[泵](ROTATIONAL_PUMP_AUDIT.md)与[大型涡轮](LARGE_TURBINE_AUDIT.md)。 |
| ALIGN-219 | 确认单块热交换器缺少原有只读运行／活动状态提供者。 | 原RunningActively继承相关接口，检测板读取mActive与热液准入，不要求原本不存在的开关。 | 当前BE／父类无MachineControl.Provider，fallback running／active=false；安装supportsSwitch门控另属既有110，possible近似沿111。换热本身仍工作，见[热交换器](HEAT_EXCHANGER_AUDIT.md)。 |
| ALIGN-220 | 确认单块热交换器周围点火的目标／可燃准入不同。 | 原WD.fire(true)接受可燃地毯／无碰撞目标或有可燃邻居，排除无可燃邻居空气。 | 当前仅空位＋现代flame.canSurvive；石顶且无可燃邻居可放火、可燃地毯被先拒。需明确现代火险政策，不直接回退规则，见[热交换器](HEAT_EXCHANGER_AUDIT.md)。 |
| ALIGN-221 | 确认玩家排液先被通用1000L行为接管，绕过设备专用整罐／顺序。 | 原工具先Behavior_Tool专用、成功即停，才到通用排液fallback。 | 当前GTToolItem先通用，cold2000时排1000并请求100耐久，原清2000／请求20；两端原生非PASS截断Block使用已追。hot-only专用成本另扩126，不代表所有工具入口，见[热交换器](HEAT_EXCHANGER_AUDIT.md)。大型蒸汽涡轮输入2000／水1000时也先排水，绕过输入优先专用清空，见[大型涡轮](LARGE_TURBINE_AUDIT.md)。 |
| ALIGN-222 | 确认旋转泵相连液体搜索被独立区域逐列扫描替代。 | 从正面同族液体连通扩散，按轻重切层，有3000／20刻重查。 | 当前先扫入口(-64,-64)列、高度-64起，入口空气也可抽孤立远源；固定向下／source扫描不等价，见[旋转泵](ROTATIONAL_PUMP_AUDIT.md)。 |
| ALIGN-223 | 确认旋转泵RU输入范围和异常包处理不同。 | 四档有效存能R/2–2R；低速包消耗但不存，超限进保护计数。 | 当前固定8–R，青铜48被拒、8被存；无原过载反馈，不据此恢复爆炸，见[旋转泵](ROTATIONAL_PUMP_AUDIT.md)。 |
| ALIGN-224 | 确认旋转泵整包下舍入可能令合法恒定输入达不到启动门槛。 | 8192需求按包向上取整，青铜17包可达8194。 | 当前最多481包到8177，余15拒17包且<8192不抽，条件持续则停；额定32正常，既有8191测试不是原版舍入证明，见[旋转泵](ROTATIONAL_PUMP_AUDIT.md)。 |
| ALIGN-225 | 确认旋转泵世界排液对象和回调准入不同。 | 同族流动块也以最低16RU清除，IFluidBlock查询实际排液结果。 | 当前仅source并造1000L，随后整个BlockState换空气；两版含水半砖返回source，可被整体移除。未游戏复现，保留整桶／移除成功保护，见[旋转泵](ROTATIONAL_PUMP_AUDIT.md)。 |
| ALIGN-226 | 确认旋转泵只读出液罐变为允许外部填充的双向能力。 | 无盖泵fillable=null，Root.fill返回0；抽取任意面、主动推四侧。 | 两端直接返回tank，空罐fill1000可成功，四侧未排空时暂停抽新源；容量差异仅在特定量条件适用，见[旋转泵](ROTATIONAL_PUMP_AUDIT.md)。 |
| ALIGN-227 | 确认旋转泵缺少原开关与运行状态接口。 | 原SwitchableOnOff／RunningActively由控制／检测盖消费，停机拒供能及新源抽取、仍推旧液。 | 当前无stopped／历史／MachineControl.Provider；Mixin后备覆盖板宿主已有，撤回全安装失败说法。fallback只有计量、supportsSwitch=false，原控制／运行检测未接通，见[旋转泵](ROTATIONAL_PUMP_AUDIT.md)。 |
| ALIGN-228 | 确认四档旋转泵硬度／抗爆统一后丢失原规格。 | 原7/7、6/6、9/9、12.5/12.5由Paintable字段及MTE Block实际getter消费。 | 当前四档Properties.strength(4,6)，无PumpBlock覆写；未量测实际挖掘／爆炸，见[旋转泵](ROTATIONAL_PUMP_AUDIT.md)。 |
| ALIGN-229 | 确认大型燃气涡轮可变即时输出被最大包整批与下一刻队列替代。 | 燃料不足最大批次仍可按HU/RU比达到半额门槛，当刻转换。 | 最低档256L氢=4096HU，原可当刻尝试2730RU，当前<12288不转换；凑满后仍延后发送，见[大型涡轮](LARGE_TURBINE_AUDIT.md)。 |
| ALIGN-230 | 确认大型燃气涡轮拒收后的能量／燃烧政策不同。 | 原storage≥max特例在尝试后无条件回写tEnergy-max，即使NBT waste=false。 | 当前保留ruStored并停烧后续燃料；768L氢、持续拒收两刻后原HU=0／现存8192RU。保护政策不自动回退，见[大型涡轮](LARGE_TURBINE_AUDIT.md)。 |
| ALIGN-231 | 确认大型蒸汽涡轮过载变为持久锁停。 | 原前100次保护清storage但不置mStopped，启动头两刻另有处理，后续破坏受配置。 | 当前首次output超限置overloaded／stopped并清pending，远程开关不能复位；满合法罐98304L可达。该分支无爆炸，不自动恢复危险行为，见[大型涡轮](LARGE_TURBINE_AUDIT.md)。 |
| ALIGN-232 | 确认大型涡轮的能力查询反复全扫并认领结构。 | 原未变更checkStructure(false)用缓存，部件查询不重跑35格循环。 | 当前每次target→isStructureOk建立Map／扫35格／bind35次，稳定20查询至少700次；非TPS测量，缓存需保留现代失效保护，见[大型涡轮](LARGE_TURBINE_AUDIT.md)。 |
| ALIGN-233 | 确认大型涡轮未接专用结构建造与形成诊断入口。 | 原Base10把建造棒坐标／库存传到两类checkStructure2，Util可放墙认领并报告形成。 | 当前主控不是MultiblockToolTarget，普通建造棒仍复制平面；部件转发也缺目标接口。预览不替代功能，见[大型涡轮](LARGE_TURBINE_AUDIT.md)。 |

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
| 材料、前缀、物质量、统一规则 | 已清点三组TD的69个直接属性声明，核查其中15项表示／消费路径、七种处理目标、19项液态酸及8项非酸分解样本 | 差异见ALIGN-006–010、012、180–182、189–193。组成不等于处理地图；酸性／18项流体与显式化学补偿保留。其它工厂／材料全集、pulver、量纲／相态及最终配方表仍继续。 |
| 固定／动态配方、监听器 | 两版本54步初始化序列已比对并共享；原通用处理器28组／478调用点、85对表级标志、Bath／Unboxinator及通用分解调用链已核，见ALIGN-011、013–016、048–051、189–193 | 调用点／日志added不是最终配方数。继续剩余配方组、分解形态／输出／数量、材料展开、生成器跳过、运行时未解析行和回调顺序；外部依赖与内部遗漏分开 |
| 手工工具与早期生存 | 已核作物／扫描／覆盖板及点火／灭火／耗材和大容量存储工具主链，差异见 ALIGN-121–137；其余工具继续 | 工具专属行为、损耗、容器返还和自然可得原料 |
| 单物品、食物、饮料、服装 | 已有 1,278 项运行清单，待核查行为出口 | 无静态机器配方不直接判缺失，另查右键、掉落、工作台、动态回调 |
| 弹药与实体 | 已确认 ALIGN-002，未完成全类核查 | 箭／子弹命中、附魔、拾取、同步和外部武器入口 |
| 坩埚、相变、铸造 | 已确认 ALIGN-001，待核其余分支 | 致死回收、热采掘、冷却目标、过热与材料守恒 |
| 单方块机器 | 共用加工执行／中断／供能已确认 ALIGN-040–047，逐控制器及其余分支继续 | 配方映射、输入输出、额定值、超载、工具交互与持久化 |
| 多方块与部件 | 大型锅炉、焦炉、聚变／内爆的部分结构／执行链已核查；物质制造结构／参数／特殊配方声明已对照 | 结构位置、部件角色、断开行为、建造与诊断、部件共用身份；整体绑定差异见 ALIGN-036，聚变输出／控制、内爆音效及聚变连续恢复见 ALIGN-052–056；物质制造最终配方解析、其它主控及跨区块行为继续 |
| 能量生产与转换 | 已核电／Flux 动力转换、十五种单方块蒸汽涡轮、26 种小锅炉、五种大型锅炉及四种燃烧室主要路径；另核九档电变压器、二十种电池盒及二十五种化学电池目录，追踪LU水晶注册消费；另核两种太阳能板、八种单块热交换器、四旋转泵及八大型蒸汽／燃气涡轮实际主链 | 核心边界例子、额定目录与原生调用链已记录；固体自动化／外部燃料、全材料灰渣、其他多方块、旋转／长距离转换、LU 充能器／便携电源缺项已登记，太阳能堆叠218、换热状态／点火／玩家排液219–221另列，原火／邻液扩017与工具成本扩126；旋转泵搜索／输入／能力／控制及物性222–228已记录，大型涡轮转换／反馈／查询／工具229–233及已知宿主扩围；其余能源设备及实际保存重启仍待核，见 [能源核查](ENERGY_CONVERTER_AUDIT.md)、[锅炉核查](BOILER_BURNING_BOX_AUDIT.md)、[固体／流化床](SOLID_FLUIDBED_AUDIT.md) 和 [大型锅炉](LARGE_BOILER_AUDIT.md) |
| 电线、流体／物品管道、长距离物流 | 待逐网络重核 | 优先级、路由、丢包、接收端卸载与跨区块边界 |
| 储物、容器与传感器 | 普通／物流桶、计量、各类库存、原生爆炸、填槽、两版通知及共用菜单九类实际宿主已局部核查 | 差异见 ALIGN-077、083–097、112–120、136–163、168、172–173、179；六类同源回调和三类补偿／限制已分开，继续其它菜单、覆盖板归属、成组掉落、量子／中继桥、材料重量、手持仪表与保存恢复；源码链不等于游戏场景已运行 |
| 覆盖板、面板与红石 | 待逐行为重核 | 安装面、厚度、信号方向、过滤、同步与拆除 |
| 养蜂与植物 | 已追自然巢、养蜂周期／保存门控、灌木生命周期／物理／生成及光莲实际两端所有权；确认ALIGN-164–167、169–171、174–178、183–188、194–195，温度计扩展085 | 光莲仅Forge碰撞／陆地回退有具体差异，Neo与GT水一般兼容已有；现代睡莲政策保留。全种类、可选整合、其余植物物理及真实保存重启继续 |
| 核反应堆 | 已核46棒／11冷却、直连／状态／能力／保存与异常、外部参数棒／回调／耗尽解析；差异202–217及095，211保留政策 | 参数新棒可注册、普通耗尽／Neo提交／标脏／正常18已有。继续极值／全药效、实际addon／启停／重载／多人／恢复设计；不将接口库存或源码条件推演当整机验收 |
| 世界生成、Nexus、roads | 前轮石层互斥已修；灌木噪声／九种池及普通石木堆内容、指示材料、采收／手摆主链已核，差异见186–188、196–201 | 陨铁／燧石／多木种已有，有限概率／层岩坐标反例不代替生成世界。其余特征布局、保护、维度、时序、地牢更新、全地质规则和自然资源分布继续 |
| 模型、GUI、音效、颜色与光照 | 待按实际对象重核 | 引用链与可见渲染，不能继续复用旧“421 条警告”数量作为现状 |
| 原版专属提示、材料含量与中英文 | 已确认 ALIGN-003，待全注册项重核 | 提示调用路径、动态字段、原文／补丁来源，不只检查 JSON |
| 查看器与其他模组兼容 | 待逐集成重核 | JEI／EMI 转移、分类、页面、刷新与外部提供方；区分现代替代关系 |
| 配置、网络与存档 | 004已扩展COMMON真实接线、两个有效过载设置／无效日志键及原配置代表矩阵；核网络／保存条件已专题化 | 继续剩余配置现代适用性、协议／同步、全部保存字段、真实磁盘／重载／多人／独立重启，见[配置核查](CONFIGURATION_CONTRACT_AUDIT.md) |

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

第六十五批八大型涡轮新增229–233，036／111／139／218／221扩宿主；26原／47当前、72视图、八注册／六向源坐标及有限燃料例子归档，四份源码展开并保持执行。普通掉落已有清理补偿，不报完整NBT携带缺陷；Mixin后备宿主纳入实际链。见[专题](LARGE_TURBINE_AUDIT.md)与[回执](verification/large-turbine-audit-20261021.json)，继续大型发电机／避雷针／大型换热与全面队列。

第六十四批核四旋转泵实际搜索／输入／能力／控制与物性，新增222–228并扩218；17原／24当前、30视图、两个原生工件8entry及有限17RU停泵见证归档，三份源码仅展开与局部命名，见[旋转泵](ROTATIONAL_PUMP_AUDIT.md)和[回执](verification/rotational-pump-audit-20261021.json)。不把源推导当游戏，不将RU泵误加进易水风险；大型涡轮、避雷针和全面队列继续。

第六十三批核扩展／太阳能／单块热交换器新增216–221，003／017／126扩围，110／111／132／209沿既有合同。46原版／130当前、126视图、52 API库存、39资源及6工件30entry重核；8换热和2太阳能注册参数重新解析，根算术见证不代游戏。原有新参数棒、天气输出、普通换热及保存保留，FuelRod误导Javadoc更正不改执行，见[核扩展](NUCLEAR_EXTENSION_AUDIT.md)、[太阳能](SOLAR_PANEL_AUDIT.md)、[换热器](HEAT_EXCHANGER_AUDIT.md)及[回执](verification/nuclear-solar-heat-audit-20261021.json)。全面队列继续。

第六十二批展开工具有序／无序合成422→703维护行，四份完整执行流／字面量／注释保持，条件顺序不变；一处Neo载体闭合位置使共用括号两端都结束方法。没有改配方或新增ALIGN，正式记录仍215条，见[源码导航](TOOL_CRAFTING_SOURCE_LAYOUT.md)和[证明](verification/tool-recipe-readability-20261021.json)。核扩展／太阳能板／单块热交换器专题及其它全面队列继续。

第六十一批归档核启停／禁槽／漏交换重载、异常队列／分派与配置消费者，新增211–215（211保留安全政策）并扩展004。36份原版／72份当前、58份平台视图和12工件49entry重核；给定128通量与状态见证不代世界验收。两个过载设置已有效、日志键未消费，正常18／普通耗尽／Neo提交不误报缺失；见[状态](NUCLEAR_TRANSITION_AUDIT.md)、[生命周期](NUCLEAR_NETWORK_LIFECYCLE_AUDIT.md)、[配置](CONFIGURATION_CONTRACT_AUDIT.md)和[回执](verification/nuclear-transitions-config-audit-20261021.json)。未改玩法，全面队列继续。

第六十批仅整理两份核辅助类25→97行、七处声明参数命名；完整执行词法逐作用域归一保持，11冷却表与运算不变，纠正过强职责说明。正式记录仍210条，不修核玩法，见[导航](NUCLEAR_SOURCE_LAYOUT.md)和[证明](verification/nuclear-helper-readability-20261021.json)；启停／重载、网络异常、配置消费者及全面队列继续。

第五十九批归档36份原版／92份当前来源、110份平台视图、七个原生工件35个entry，新增核合同202–210并扩展095。root重解析46棒参数并复算常规／Zr工时、直连槽位／20N、容量／几何；原o快照真实消费排除phase18误报。已有11表、催化剂／标脏／安全政策保留，未修玩法，见[棒](NUCLEAR_ROD_AUDIT.md)、[核心](NUCLEAR_CORE_AUDIT.md)、[网络](NUCLEAR_NETWORK_AUDIT.md)和[回执](verification/nuclear-contract-audit-20261021.json)。继续启动停机、异常／外部提供者、源码职责及全面队列。

第五十八批仅展开反应堆核心236→560行、最长305→152，两份完整执行投影及字面量位置／注释／条件保持；core目录、物理／冷却参数与原生状态／网络导航另列，未把排版当核玩法完成，见[源码导航](NUCLEAR_SOURCE_LAYOUT.md)及[证明](verification/reactor-core-readability-20261021.json)。继续棒物理、核心生命周期与中子网络三组原版专题，候选未正式编号；其它方法职责和全面队列继续。

第五十七批完成Chemical／GeneratedRecipeResolver整类共享，1775→641行、四份完整执行词法及字面量位置保持，解析器十表序列另核；已有不同的表序／原生表达式继续条件化。少量过强旧说明纠正，统计按各自真实操作描述；不新增ALIGN、不修玩法，见[报告](RECIPE_RESOLVER_SHARING_AUDIT.md)及[证明](verification/chemical-resolver-sharing-20261021.json)。四个基线候选共2729→975行，未审方法／大职责和其余原版系统继续。

第五十六批由34份原版／135份当前来源、六个原生工件52个entry及74份平台视图确认光莲两项Forge差异和石木堆六项合同ALIGN-194–201。现代水面政策／一般GT水族、罕见内容／多木种／显式掉落／Twig支撑已分清；三个理想概率期望和两个实际层表坐标由root重算，不冒充世界实测，见[光莲](GLOWTUS_AUDIT.md)、[地表堆](SURFACE_PILES_AUDIT.md)及[回执](verification/surface-plant-audit-20261021.json)。继续剩余配方共享、其它植物／特征与全面队列。

第五十五批将全部1041份共享原生维护输入展开为2082份视图，确认四项完整类体候选、排除两个原生声明边界；VanillaProcessingRecipes与AlloyRecipeLoader从954→334行，四份完整执行词法与字面量位置保持。没有新增ALIGN或配方语义修复；Chemical及保留加载顺序差异的GeneratedRecipeResolver另行推进，见[结构报告](NATIVE_BODY_SHARING_AUDIT.md)与[回执](verification/native-body-sharing-audit-20261021.json)。继续光莲／地表堆和全面队列。

第五十四批由18份原版／60份当前源码与8个非酸样本确认分解准入／地图、输入形态、工作参数、原生水及相态差异ALIGN-189–193。明确保留盐水等显式补偿、现代最小输入放宽、优化前参数和最终表边界。DecompositionRecipeLoader从336行两份业务合成124行共用流程，仅保留原生类型／holder查询条件，两端850／858项执行词法与字面量位置相同，见[报告](MATERIAL_DECOMPOSITION_AUDIT.md)与[回执](verification/material-decomposition-audit-20261021.json)。继续其它材料消费、光莲／地表堆、世界生成及全面队列。

第五十三批归档灌木物理与生成差异ALIGN-183–188：实际抗爆覆写、instabreak副作用、火对象表、条件性墙内检测，及半坐标、原木射线和群系门控。基础碰撞／LightBlock1/0与九种池已有对应；现代高度、群系及原Map顺序不盲目还原。两份源码四份执行投影保持，四坐标有限数学反例独立复算，见[物理](BERRY_BUSH_PHYSICAL_AUDIT.md)、[世界生成](BERRY_BUSH_WORLDGEN_AUDIT.md)与[回执](verification/bush-physical-worldgen-audit-20261021.json)。继续材料分解输入／地图／参数、其余植物／特征及全面队列。

第五十二批沿用户两条材质写法追原工厂→生成器→属性补偿→双端注册／真实危险消费，新增ALIGN-180–182。19条酸性及成分、18条实际流体已有实现；额外固态形态、氟硅酸魔法误标、黑矾状态遗漏分别登记，不按19种材料拆问题。MaterialFactories仅展开参数／方法和纠正说明，1304执行词法项／字面量位置保持，见[报告](LIQUID_ACID_FACTORY_AUDIT.md)与[回执](verification/liquid-acid-factory-audit-20261021.json)。继续通用分解输入／地图、其余材料工厂、植物物理／世界生成及全面队列。

第五十一批归档灌木五项环境／支撑／工具差异及队列 GUI 取物限制 ALIGN-174–179；共用菜单九类实际宿主将173扩为六类同源范围，保留三类补偿／限制。四份源码六份执行词法投影相同，见[灌木](BERRY_BUSH_AUDIT.md)、[菜单](SHARED_MENU_AUDIT.md)和[回执](verification/bush-menu-audit-20261021.json)。继续酸类材料工厂继承、植物物理／世界生成及全面队列，不将已存在的生命周期和静态源链冒充整系统验收。

第五十批核对自然巢火焰／龙破坏、养蜂保存门控、库存通知和箱子部分 shift，新增 ALIGN-169–173。三份源码六份平台执行投影保持；实际保存／事件边界不能以同名方法替代。见[自然巢](BUMBLE_HIVE_PHYSICAL_AUDIT.md)、[保存](BUMBLE_PERSISTENCE_AUDIT.md)和[通知](INVENTORY_NOTIFICATION_AUDIT.md)。继续其它共用菜单、浆果灌木丛与全面队列；没有运行游戏或改玩法。

第四十九批核对两版外部爆炸掉落／移除顺序、战利品随机填槽及养蜂世代，新增 ALIGN-162–168，自然巢温度计扩展 085。两份 core 规则只排版且词法流／字面量位置保持。见[库存爆炸](INVENTORY_EXPLOSION_AUDIT.md)、[填槽](LOOT_SLOT_AUDIT.md)与[养蜂](BUMBLE_LIFECYCLE_AUDIT.md)。不改机器过载／自爆，不把瓶框完整包误判成逐槽仅存 1/3；继续库存通知、养蜂保存／物理边界及全面队列。

第四十八批核对箱子与战利品实际生命周期，新增 ALIGN-152–161，覆盖板范围／邻块阻挡扩展 139／142并澄清 102。八份源码十四份执行投影保持，见[报告](CHEST_LOOT_AUDIT.md)。继续随机库存填充、邻居通知、爆炸分派、物流中继与全面队列，不能据已登记差异宣布该系统完全核完。

第四十七批核对瓶框整组取放、保存与禁贴面，书架附魔／陈列／放大镜与原生恢复入口，新增 ALIGN-147–151，并扩展音效 ALIGN-132／禁贴面 ALIGN-139。六份源码十一份执行投影保持，见[报告](BOTTLE_BOOKSHELF_AUDIT.md)。继续箱子、原生爆炸分派、其它库存与全系统队列。

第四十六批沿保险箱锁／覆盖板和衣柜装备／配方实际入口，新增 ALIGN-140–146，保险箱禁贴面扩展 ALIGN-139；库存保存／拆除已有对应，未误报为 NBT 保留缺失。十二份整理代码十七份投影保持，见[报告](SAFE_LOCKER_AUDIT.md)。全量布局盘点确认旧命名路径已移除，但仍有压行和大职责类，见[可读性盘点](SOURCE_READABILITY_INVENTORY.md)。继续箱子、书架及全面队列。

第四十五批核对统一事件与宿主正面合同，新增大容量存储遮挡、抽屉禁贴面差异 ALIGN-138–139；权限入口和六向旋转表已有证据。三份源码排版／说明保持五份投影不变；继续保险箱、衣柜等库存与全面队列，见[报告](STORAGE_COVER_BOUNDARY_AUDIT.md)。

第四十四批核对大容量存储的真实胶带宿主、封装掉落、工具与模式，新增 ALIGN-136–137、扩展 ALIGN-126。两份压行类只排版，两份宿主说明纠错，七份执行投影保持；继续覆盖板遮挡、交互权限、材料／物流边界和全面队列，见[报告](MASS_STORAGE_CONTROL_AUDIT.md)。

第四十三批先解决远端 Addon 壁板／管道与本地审计记录冲突，再核对消耗工具声音、概率配置和创造／耗尽生命周期。两份共享说明纠错不改两版执行代码；继续大容量存储的封装／拆包、其他工具、植物养蜂与世界生成等队列，见[报告](CONSUMABLE_FEEDBACK_AUDIT.md)。

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

第四十二批恢复核查：暂停前五组工具证据已按当前代码归档，同时回写既有设备／过载修复状态，避免旧结论继续计为缺项。两份 NeoForge 压行文件只做词法等价排版；继续声音、配置、喷罐生命周期及其余系统，见[报告](IGNITION_EXTINGUISHER_AUDIT.md)。
