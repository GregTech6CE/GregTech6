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
| ALIGN-017 | 确认转换器未接入原版运行环境风险检查。 | `TileEntityBase10EnergyConverter.onTick2` 每 600 tick 检查活动设备；Root 与 Covers 按能量类型、侧面、覆盖板和配置处理火／水／雨／雷。 | `EnergyNodeBlockEntity.tickOriginalConverter` 只保留不运行时衰减过载次数；易损集合存在但未消费。范围及概率见 [能源核查](ENERGY_CONVERTER_AUDIT.md)，不能推为所有蒸汽设备都应遇水损坏。 |
| ALIGN-018 | 确认 Flux Motor／Dynamo 各缺少后三档，共六个设备。 | `Loader_MultiTileEntities:858–860,955–957` 注册 11023–11025、11113–11115，无外部模组条件。 | 两版本共用的 `EnergyNodeDefinitions` 各只有 LV／MV。五类原版目录 35 行与当前 core 29 项比对，现有项额定值匹配；缺项需连同模型／配方／材料与语言补齐。 |
| ALIGN-019 | 确认小锅炉工具交互差异。 | 原版 BoilerTank 的排空器先水后蒸汽，温度计读取储存热量。 | 当前先蒸汽后水、固定排空消耗，并缺少锅炉温度计分支；Jade 不能代替物品交互。 |
| ALIGN-020 | 确认小锅炉缺少外部爆炸触发的高压连锁处理。 | 原版 BoilerTank.onExploded 在表值 >4 时再次爆炸。 | 当前只有自身过热／满罐及玩家拆除分支；抑制高压掉落不是爆炸回调。 |
| ALIGN-021 | 确认小锅炉遗漏矿泉水接收。 | 原版 FL.WATER 包含 potion.mineralwater。 | 当前 isWater 的固定路径集合漏掉该项，而共享流体表已有 MnWtr／WATER。 |
| ALIGN-022 | 确认液体／气体燃烧室补燃和余火逻辑不完整。 | 原版 GeneratorLiquid 按 burning 或 cooldown 运行，循环补到两倍额定输出，每次补燃重置余火。 | 当前只补一份、只检查 burning；致密殷钢 64 HU/t 与氢气每份 32 HU 的实际参数会进入补不足即熄火路径。余火烫伤也缺失。 |
| ALIGN-023 | 确认液体／气体燃烧室的罐接口缺少原版过滤和排出限制。 | 原版按燃料配方及液／气类型接收，普通管路在燃烧或余火时不能抽出。 | 当前直接暴露无过滤 FluidTankGT；任意液体能填入不等于任意液体能发电。 |
| ALIGN-024 | 确认气体燃烧室没有接入邻接／手动关停接口。 | 原版 Gas 实现关闭即熄火清余火，开启不自动点火。 | 当前设备及父类不实现已存在的 AdjacentEnergyControl／MachineControl Provider。 |
| ALIGN-025 | 确认液体／气体燃烧室缺少原版周边点火和前方燃烧分支。 | 原版 GeneratorLiquid:123,131–133 按效率点火、烧前方并检查碰撞／液体／氧气。 | 当前 FLAME_RANGE 无消费；前方只判空气或可替换。跨模组氧气适配需另核。 |

上述是具体范围，不表示这些系统完全未移植；ALIGN-009、010、012 有独立 core 观察，能源目录和弱燃料参数也有 core 枚举，其余结论依赖具体调用链核查，没有启动游戏。锅炉及液体／气体燃烧室的完整来源、相同部分和边界见 [专题核查](BOILER_BURNING_BOX_AUDIT.md)。

## 全面核查队列

| 系统 | 本轮状态 | 完成盘点需要的证据 |
| --- | --- | --- |
| 材料、前缀、物质量、统一规则 | 已清点三组 TD 的 69 个直接属性声明，核查其中 15 项的具体表示／消费路径；工厂样本和七种目标直接引用已扩大核查 | 已确认 ALIGN-006–010、012。FOOD／BRITTLE／SOFT 等虽有 `MaterialWorkability` 表示，继承和名称映射仍有实际缺口；成员全集未完成。 |
| 固定／动态配方、监听器 | 两版本 54 步初始化序列已比对并共享；原版通用处理器 28 组、478 个调用点已索引，具体差异见 ALIGN-011、013–016 | 调用点不是最终配方数。继续核对剩余配方组、材料展开条件、生成器跳过原因、运行时未解析行和回调顺序；外部依赖与内部遗漏分开 |
| 手工工具与早期生存 | 已有前轮局部修复，待扩大核查 | 工具专属行为、损耗、容器返还和自然可得原料 |
| 单物品、食物、饮料、服装 | 已有 1,278 项运行清单，待核查行为出口 | 无静态机器配方不直接判缺失，另查右键、掉落、工作台、动态回调 |
| 弹药与实体 | 已确认 ALIGN-002，未完成全类核查 | 箭／子弹命中、附魔、拾取、同步和外部武器入口 |
| 坩埚、相变、铸造 | 已确认 ALIGN-001，待核其余分支 | 致死回收、热采掘、冷却目标、过热与材料守恒 |
| 单方块机器 | 待逐控制器重核 | 配方映射、输入输出、额定值、超载、工具交互与持久化 |
| 多方块与部件 | 待逐结构重核 | 结构位置、部件角色、断开行为、建造与诊断、部件共用身份 |
| 能量生产与转换 | 已核电／Flux 动力转换、十五种单方块蒸汽涡轮、26 种锅炉目录与液体／气体燃烧室主要路径；新增 ALIGN-017–025 | 核心边界例子、额定目录与原生调用链已记录；固体／流化床燃烧、多方块、变压器、储能及保存重启仍待核，见 [能源核查](ENERGY_CONVERTER_AUDIT.md) 和 [锅炉核查](BOILER_BURNING_BOX_AUDIT.md) |
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

## 下一批工作

继续核查材料属性的成员与工厂继承、处理目标和前缀监听器，以及锅炉、其它能源转换／储能与网络系统。同步整理相关过大／压行实现。第五批已整理三份能源核心源码的格式与职责说明，并在 SOURCE_LAYOUT 中补充批量材料数据导航。每条结论附原版和当前调用链；完整审计仍未完成，不能以这一批编译通过结束目标。
