# P2：NeoForge 首条真实机器链的迁移边界

2026-09-30，只读代码审查；基准 main `de18243bb9a81a7752d778ec8a5d1d392ba6294d`，Forge 开发夹具诊断日志以正在复跑的版本为准。此审查只新增本文档，没有修改生产/core Java、Gradle，也没有启动游戏。源码位置以下均相对 `F:\Dev\GregtTech6New\gregtech6`。

目标是把项目1的真实砖燃烧箱 → 陶瓷坩埚 → 青铜 → 陶瓷模具链迁入 Neo，使用现有共享 GTMaterial/GTMaterialRegistry/MaterialPrefix/CrucibleReactions。不能用一套演示物品、第二份材料目录或直接写入4U青铜替代迁移。源码存在、纯合约通过、给定器材加工、生存获得器材、世界内渲染与真实机器保存重启分别验收。

## 已有共享核心及目前缺口

`core/src/main/java/com/gregtech/gregtech/api/material` 已包含完整材料身份、属性、组成、质量与角色；`api/machine/crucible/CrucibleMaterialStack`、`CrucibleReactions`、`CrucibleMath` 已共享。物质量使用 U=648648000；温度使用整数绝对 Kelvin。铜290、锡500、青铜8610仍是同一目录对象，不能另建 CC 每锭144的机器存储。

`api/machine/crucible/ThermalState`/`ThermalStep` 已是无 Minecraft 的候选：壳与内容总质量决定 `1 + floor(kg/100)` HU/K，保存有符号HU余量；捕获步前温度，消耗整除转换，供能后100tick窗口、无供能每10tick向环境漂移1K。它不负责材料转换、合金选择、容量或世界副作用。

边界调查时Forge上述热公式/混温/注入仍本地。随后本批已接共享ThermalStep与CrucibleProcess，源行号随抽取变化；运行替换验收仍待集中验证。因此目前不能说双版本机器热行为验收已完成。建议先把现有 Forge 的正HU链接入候选并复跑相同实际加工测试，然后让 Neo 调同一 API。保留 Forge 本来的HU标签/底面接收条件；共享纯类支持 COOLING 并不自动授权新增机器CU能力。负HU旧存档、Long.MIN_VALUE包、溢出与小于1kg混温的拒绝策略需作为明确兼容修正记账，不能在 Neo 入口静默夹成0或改变 `gt.*` 类型。

Neo 当前入口 `neoforge/src/main/java/com/gregtech/gregtech/platform/neoforge/GregTechNeoForge.java` 只有完整共享目录初始化及领域 postInit。它先绑定 ModData presence，后触发 PrefixRegistry/ModReferences/MaterialGroups、init、角色，再在 commonSetup enqueueWork 做 postInit。这一顺序已经实跑七项 GameTests，新增机器/工具静态持有者不能提前触发目录；原版物品统一、真实物品/机器注册、渲染和世界生成仍待适配。

## 必须保留的实际调用链

### 煤炭、燃烧与HU输送

原文件 `blockentity/machine/SolidBurningBoxBlockEntity.java:71–93`：正在燃烧并有至少一输出周期能量时，向顶面发送HU，大小1、数量不超过输出；每tick扣除完整输出率，不随邻居接收量退还。能量低于两周期时尝试消耗一件燃料，低于一周期时熄灭。不能把拒收情况下的既有能耗改成“仅接受才扣能”而未记录改变。

`:107–132` 在实际燃料、容器和灰产物可存放后才消耗一件，`:134–174` 是两槽/待灰的真实堆叠与容量规则；`:242–289` 是正面燃料插入、空手取出、原版打火石点火与实际耐久损耗。正面需要可用空气，燃烧箱不是纯计时器。`api/energy/EnergyTransfer.java:36–48` 查顶面邻居，以反面输入 side 调原 IEnergyBlock；本链直接HU邻接即可，不需把FE等同HU，也不需先移完整电网/第三方EnergyCompat。

`registry/GTMachines.java:79–80` 定义正式 `gregtech:burning_box_solid_brick`：ClayBrick、efficiency2500、output16HU/t。`api/machine/FurnaceFuelHelper.java:22–41` 的单件热量是 `burnTicks*25*efficiency/10000`。`core/.../data/ImportedMaterialData.java:27` 的 Coal 是1600tick、DarkAsh U4。因此一煤=10000HU；`FurnaceFuelHelper.java:45–60` 生成 `floor(U4/U9)=2` 个 tiny dust 灰（且至少1、最多64）。正式输出通过共享对象/前缀解析，现有运行显示 `gregtech:dust_tiny_darkashes`。不要按别名 DarkAsh 手写一个新ID。当前灰分数舍弃为原行为，不能称燃料化学物质量全守恒；后续修正要单独决策。

`loader/VanillaUnificationLoader.java:27` 把原版煤绑定为 gem/Coal。若 Neo 只用原版燃烧时间 fallback 而不做该绑定，将出现“有热无GT灰”的功能退化。`FurnaceFuelValue.java:52–122` 的材料+前缀燃值、wood特殊下限、32000上限及比例函数可提到 core；ItemStack标签、残留容器和平台burnTime查询留平台。`FurnaceFuelHelper` 可拆为共享热量/灰产物计划和平台实际ItemStack构造/存储，不能把生产器材简化成只认 Items.COAL 的专用demo。

### 坩埚真实吸料、相变、反应与浇铸

`SmeltingCrucibleBlockEntity.java:67` 的普通容量16U；`:175` 的吸入区是壳内 x/z2/16..14/16、y2/16..1。`:344–359` 将区内首个非空 ItemEntity 整栈缓存并 discard实体；`:363–377` 每tick解析一件，只有容量接受后才 shrink一件。因此满16U后第17锭应留缓存，拒绝不能吞料。无法解析的物品可能占据缓存，不能为了跑通测试把它自动删除。

`:216–269` 在热步之前按当前温度处理材料、蒸发、酸蚀与熔化，再调用共享 CrucibleReactions；`:271–294` 才按壳+内容质量更新温度。这一顺序影响达到1357K后哪tick出现青铜；不应把 Neo 的反应搬到升温之后却仍宣称逐tick等价。`:380–411` 是容量检查、量化质量混温和材料合并；`:940–957` 从真实内容逐项向模具转移，只允许处于该材料熔点以上的内容，扣掉模具真实返回的已消费量。

`api/machine/crucible/CrucibleItemInput.java:29–126` 将 MaterialItem的前缀/材料，真实物块、坩埚壳、原版raw ore及可回收组成转成 U 单位。可抽出“已解析领域组成→数量/oreMultiplier/容量计划”的纯规则；Item/Block判断、耐久与封装内容检查继续留平台。优先接正式 ingot Copper/Tin/Bronze 及全部该前缀可生成的表，不能复制一份只含铜锡的材料Catalog。后续补原矿/石种输入时继续调用同一解析计划。

共享相变/容量 step 应接受当前材料列表、整数温度/前温度、最大物质量、壳 spec，返回可观测的材料变化与蒸发/酸蚀/熔毁等副作用请求。平台执行声音、火、熔毁成流动岩浆、伤害、setChanged与网络更新。当前固相过渡分支只是保留同一材料，不能凭注释宣布 mTargetSolidifying 的全部GT6行为完成。

### 模具形状、冷却、浇铸与取锭

`blockentity/machine/MoldBlockEntity.java:98–292` 是完整原形状表，含滑动窗口、90度旋转、镜像和 nugget fallback。绝大多数值已经是共享 MaterialPrefix；仅`:290` 的 BlockMaterialPrefix.blockSolid 引用阻止其直接成为纯Java。建议提完整形状表和稳定输出形式描述（prefix名称/单位/类型），用平台 form→实际Item/Block解析。不要为了首条锭链复制一份15格特例表，也不要移入含 MapColor/SoundType 的 BlockMaterialPrefix 平台类型。

`:503–515` 将顶面2/16..14/16范围映射到5×5网格并 OR 掏空bit；尽管注释说toggle，代码没有切回实心。`:131–132` 是3列×5行锭形，左边三列掏空金样mask=7576807，15次真正凿刻，所需量1U。`:480–491` 对未知非零形状返回nugget；`:719–731` fallback使用bitCount×U9。抽取时保留位序、变换冲突覆盖次序与fallback，另以全表指纹/金样验证。

`:707–710` 接受顶面/水平面，不接受底面；`:736–760` 要求足够完整所需量，拒绝酸/非防酸、已填料、待输出；接受exact required量并保存传入温度。实际可熔前提由坩埚检查，而不是在测试直接调用fillMold跳过。`:318–325` 每tick最多5K向实际环境靠近，`:371–372` 低于熔点才固化；这不是坩埚的100/10tick热步。`:585–640` 只有真实输出ItemStack存在时才清空内容，库存满则真实掉物；缺失物品输出不能消耗材料。

`event/SmelteryInteractionHandler.java:118–150` 负责空手/真实钳子拾取后尝试浇铸、凿子顶面掏格，成功工具操作会损耗；其真实路径必须保留到 Neo，不用命令/测试赋值代替玩家交互。原工具 `api/tool/GTToolType.java:40/:49` 为凿子48、钳子66；`registry/GTToolItems.java:22–25` 注册 `gregtech:tool_chisel`/`tool_pincers`。GTToolType 混有材质图标、BlockTags等平台属性，先共享工具身份/纯数值/材质耐久规则，真实GTToolItem与平台标签/客户端图标适配；不要建第二种“仅测试凿子”。原根键 `GT.ToolStats` 的材料名/损耗语义需在1.21 CUSTOM_DATA/组件边界保留并明示迁移验证范围。

盆、交叉槽、龙头、自动拉取/红石、软锤重置、扳手、灼伤/酸蚀等是现有正式行为。首提交可明确延后未注册邻接器材，但其领域接口要继续接同一内容/形状规则；不能给未完成接口返成功或注册不工作的空壳后计入验收。

## 规格、正式注册与持久化

`api/machine/MachineSpec.java` 是纯Java record；`api/machine/CrucibleSpec.java` 只依赖已共享GTMaterial/GTValues/MaterialMass，可按原包名迁入 core。实际参数从 `GTMachines` 提成单一机器定义来源，再由两个平台注册。陶瓷参数`:334–335`：id smelting_crucible_ceramic，meta1005、显式2000/4000K、hullDensity0.8181818181818182、壳7U；模具 `api/machine/MachineRegistry.java:149–168` 的 companion meta1055、壳5U，热限当前Math.round(2000×1.25)=2500K。不能用 Ceramic 材料默认密度替代机壳显式密度，也不能将“stone9x”注释当小坩埚容量实现。

首批真实块和BlockItem采用 `gregtech:burning_box_solid_brick`、`smelting_crucible_ceramic`、`mold_ceramic`。BE采用 `gregtech:solid_burning_box`、`smelting_crucible`、`mold`（`registry/GTBlockEntities.java:82–115`）；真实 EntityBlock提供注册类型校验后的仅服务端ticker。方块/BE工厂延迟取得holder，不能在材料presence绑定前构造spec/材料持有者。

物品身份来自 `core/.../data/MaterialPrefix.java:656–657` 的 getItemId 与 material.getName；例如正式 ingot_copper、ingot_tin、ingot_bronze。化学符号Cu/Sn及本地显示名不能替换registry路径。`loaders/a/Loader_Items.java:189–212` 的canonical/hidden/validator/碰撞策略必须是两平台同一生成来源；平台各有 Item→共享材质对象的适配map是必要边界，不是第二份GT材料registry。`ItemMaterialRegistry.ItemMaterialData` 的不可变领域组成DTO可先抽 core；Neo保留真实Item绑定与封装/耐久检查。

保存字段必须逐类型保留：

- 燃烧箱 `SolidBurningBoxBlockEntity.java:313–334`：long gt.energy、boolean gt.active、long gt.output、int gt.efficiency、ItemStack gt.pending_ash；基类 `GTFacingMachineBlockEntity.java:74–86` 的 gt.inv 两槽。
- 坩埚 `SmeltingCrucibleBlockEntity.java:791–822`：long gt.temperature/gt.temperature.old/gt.energy.buffer、int gt.cooldown、boolean gt.meltdown、byte gt.display.height、int gt.display.material、boolean gt.display.molten、int Meta，gt.materials 列表中材料int id/long amount，gt.cache 实际ItemStackHandler。加载缺键默认温度293、前温度当前值、冷却100；无效/非正材料由 CrucibleContentsNbt 做已知过滤，不重新映射成CC IDs。
- 模具 `MoldBlockEntity.java:788–835`：int gt.mold.shape、byte gt.mold.auto_pull、boolean gt.mold.use_redstone、long gt.temperature、string gt.content.material（与坩埚int ID不同）、long gt.content.amount、boolean gt.content.solidified、实际ItemStack gt.mold.solid_output及旧presence兼容键。不要统一成不同新schema而不做旧存档测试。

Neo loadAdditional/saveAdditional/updateTag 必须使用本版本 HolderLookup.Provider；库存与 ItemStack 的1.21 codec也必须使用同一provider。世界邻接、Direction、实体、ItemStackHandler、NBT、同步包留Neo平台，不放core。有效状态与缺省/非法旧NBT边界用纯快照规则加平台codec分层验证；最终门禁仍需真实关服保存、另一JVM读取这些机器的内容/缓存/能量/温度/形状/耐久，原版箱子重启不覆盖它们。

## 已核对本机Neo 21.1.243的API边界

以下是读取已解析官方源码的签名证据，不是仅凭版本记忆。Minecraft+Neo patched源码 `C:\Users\Asus\.gradle\caches\neoformruntime\intermediate_results\sourcesWithNeoForge_29e789f438d59e04022ec9c96d365da83c794d58_output.zip`，SHA256 `dae21dab7a8be7344282cce011c09d288c64966d08e84a56f123752e158a786c`；Neo源码 `C:\Users\Asus\.gradle\caches\modules-2\files-2.1\net.neoforged\neoforge\21.1.243\7c3c8a58e626916d133b20bc249e1a7e5f1a306b\neoforge-21.1.243-sources.jar`，SHA256 `c11c83b344cb92ce9e815db2ed4b53fd7770b97098d5ebd10edde28595c12af0`。冒号行号为对应archive内源码一基行号。

- `BlockBehaviour.java:201–208`：protected `useWithoutItem(BlockState,Level,BlockPos,Player,BlockHitResult)` 返回 InteractionResult；protected `useItemOn(ItemStack,BlockState,Level,BlockPos,Player,InteractionHand,BlockHitResult)` 返回 ItemInteractionResult，默认PASS_TO_DEFAULT_BLOCK_INTERACTION。原1.20 `Block.use`不能只改imports。带物品钳子/凿子/点火与空手必须落在正确路径，且不能同时事件和Block回调消费两次。`:751–759` 的BlockState分派还经过UseItemOnBlockEvent；测试应按真实分派/事件路径操作。
- `BlockEntity.java:77/:94/:217`：protected loadAdditional/saveAdditional 的第二参均HolderLookup.Provider；getUpdateTag亦携带provider。`IBlockEntityExtension.java:35–50`：onDataPacket(Connection,ClientboundBlockEntityDataPacket,Provider)及handleUpdateTag(CompoundTag,Provider)默认loadWithComponents。`ClientboundBlockEntityDataPacket.java:30–36` 的create(BlockEntity)通过getUpdateTag和registryAccess组包，避免自写丢掉provider的网络格式。
- `ItemStack.java:291–297/:398–417`：parse/parseOptional(Provider,Tag/CompoundTag)，save/saveOptional必须provider；save空堆栈会抛异常而saveOptional给空compound。`ItemStackHandler.java:139–166` 使用serializeNBT(Provider)/deserializeNBT(Provider,CompoundTag)，且每槽真实save/parse；`:69` 堆叠比较isSameItemSameComponents。不能把旧ItemStack.of/save或isSameItemSameTags直接照搬。
- `IItemStackExtension.java:86–92` 的stack.getBurnTime(nullableRecipeType)实际委托Item并拒绝负燃值，可供Neo fallback。优先保留领域燃值与真实Coal绑定，fallback不是GT灰规则替代。
- `DataComponents.java:57/:144/:171–178/:216–231` 实际存在 CUSTOM_DATA、CHARGED_PROJECTILES、ENTITY_DATA、BUCKET_ENTITY_DATA、BLOCK_ENTITY_DATA、CONTAINER、CONTAINER_LOOT。旧getTag是否含BlockEntityTag/Items/EntityTag的封装内容检查要改为真实组件内容；未拆除的容器库存/实体数据不能被坩埚吞掉。工具旧自定义字段可放CUSTOM_DATA，真实耐久还需MAX_DAMAGE/DAMAGE语义与损坏物料量一起验证。
- `DeferredRegister.java:142–156/:431/:530–563`：createItems/createBlocks、registerBlock(name,Function<Properties,B>,Properties)、registerSimpleBlockItem(name,Supplier<Block>,Properties)及holder重载；BE用BuiltInRegistries/Registries.BLOCK_ENTITY_TYPE的DeferredRegister，保持模组事件总线注册与延迟factory。`EntityBlock.java:18` 的getTicker(Level,BlockState,BlockEntityType<T>)保持泛型类型匹配。`RegisterCapabilitiesEvent.java:59` 的registerBlockEntity(BlockCapability<T,C>,BlockEntityType<BE>,ICapabilityProvider)是Neo能力注册接口；首条手动+直接HU邻接链不需先引入新版通用FE网络，但以后库存自动化不能照搬旧LazyOptional capability。

## 依赖顺序与每步门禁

1. 原包名移 MachineSpec/CrucibleSpec、GregTechTags 的纯Java能源身份及统一机器规格定义；抽材料燃值、HU缩放、灰计划和ItemMaterialData。保留来源hash/作者/授权证据，core用jdeps/编译证明无MC。先独立原实现与共享实现输入金样。
2. 抽完整 MoldShape 表/requiredUnits、容量和相变计划、模具冷却与浇铸/固化/出料事务。工具身份/数值也从现有表提取。接回Forge，原始行为金样、现有真实加工链与负例继续通过；此步才证明迁移没有用另一个近似算法。
3. Neo正式 MaterialItem/GTToolItem、同一prefix+material注册定义；Coal等必要原版绑定及tiny灰实际产物。先检查registry ID/canonical对象/实际堆栈与工具损耗，补 CUSTOM_DATA 与容器数据组件判定。所有该首链会访问的形式必须可解析，否则显式拒绝并保持输入。
4. Neo三个正式方块、BE注册/工厂/ticker、HU邻接适配、实体缓存与真实玩家工具交互、NBT/更新包。首运行复用 Forge 同样3Cu+1Sn/8coal/15凿刻/四次浇铸金样，必须在自然tick达到反应；同时跑airgap、17锭容量、已填模具/空模具拒绝和每tick4U守恒。并发测试采用物理隔离夹具，不改生产吸料。
5. 移真实模型/材质别名、物品颜色、点亮燃烧箱、坩埚熔体/模具形状渲染及服务端无client类门禁。原静态 ceramic模型是占位铁cube，`client/CrucibleClientModels.java:29–36` 才动态别名正确壳模型；`client/MaterialClientModels.java:38–48` 为动态前缀物品模型。只复制JSON不代表视觉移植完成。需要世界内真实操作截图/视觉检查及客户端接专服。
6. 移1.21 recipe文件格式、正式工具配方/制作、陶土器材烧制、铜锡生存取得及青铜工具使用。`recipes/smeltery_survival/burning_box_solid_brick.json` 依赖 gregtech:tool_shaped 与砖ingot tag，`ceramic_crucible_firing.json` 依赖 clay_crucible；迁它们前须真实serializer与前置器材。1.21 data使用singular recipe/structure且部分result/stack codec变化，不能原样复制后凭文件存在宣称可达。
7. 两平台产物重新构建/验包、成品环境启动、真实机器保存重启，记录相同与差异。给定钢工具/器材的加工测试不是“生存制得青铜工具”验收；原Forge或其他两源旧存档兼容继续用副本实测后定义。

## 可计算金样和已观察范围

源码固定数值可直接用于共同合约：一煤10000HU，砖箱16HU/t；3U铜+1U锡在至少1357K由共享反应得4U青铜；陶瓷壳质量约636.3636357272728kg，3铜+1锡连壳约4432.696997901126kg（使用原F精度密度8.96F/7.287F与111.111111D），每K需45HU；左三列锭mask7576807/15格/1U；从1357K向283K环境的模具第一tick降5K至1352K，低于青铜熔点而可固化，不能在尚液态时取锭。

以上质量数字是本次用Python标准库按Java float32密度独立计算，没有运行机器。实现需共享同一函数并对源版本与抽取版本验证，固定动作不要求所有随机生物群系同tick完成。环境代码 `SmelteryBlockEntityHelper.java:27` 是 `273+round(biomeBaseTemp*20)`，不能把测试的ambient写死293。

实际观察：首跑 `work/p2-forge-bronze-playflow-runtime.log` 主链在tick3011合金、3020四锭完成；同次负例因冷坩埚有额外物料失败，整次失败不可删。新world诊断 `work/p2-forge-bronze-playflow-diagnostic.log:1396/1528–1532` 两项通过：冷坩埚283K/0HU/空，容量16U铜+缓存一锭；tick3028合金、3037四锭完成。物理隔离/XML复跑已通过4m17s，见verification/p2-forge-playflow.json；Neo真实链尚未运行。本审查不把静态金样或这些Forge日志替代Neo玩法/世界重载。

## 冷坩埚额外Leather/Steel：已定位事实与尚未确定来源

首跑失败值 Leather3891888000(6U)+Steel1297296000(2U) 与 `src/main/resources/data/gregtech/materials/vanilla_compositions.json:7150–7153` 的原版 saddle组成逐字相同。吸料代码会接受该ItemEntity，冷却不需要熔化即可留下材料。`SmeltingCrucibleBlock.entityInside:78–85` 只有温度伤害，未把生物转成这两种材料；289K/0HU仍符合断热和生物群系环境。应追踪进入吸入区的鞍，不把夹具污染当合金/供热规则失败。

模板实际gzip/NBT完整解码114/114字节：112字节，SHA256 `6c01f87bfc554e3e1ede220e784470c02b04ad9ada25cf366d4b1b9e157d8f0e`，root DataVersion3465、size[9,8,9]、palette仅minecraft:air、blocks[]、entities[]。它自身不含鞍或生成实体。

本机官方映射源码jar `C:\Users\Asus\.gradle\caches\forge_gradle\minecraft_user_repo\net\minecraftforge\forge\1.20.1-47.4.20_mapped_official_1.20.1\forge-1.20.1-47.4.20_mapped_official_1.20.1-sources.jar`：`StructureUtils.java:177–188` 先清扩展范围的方块，再discard当时范围内的非Player实体；`:301–303` 通过BlockInput.place置块。`ChestBlock.java:218–223` 的容器移除会掉库存，RandomizableContainerBlockEntity.getItem:96–98还可能生成其loot；但框架随后会清当时的drops，所以这只证明一个可能路径，不能定论本次鞍来自被清的箱子。边界外/后续生成/邻接测试区的实体仍需运行坐标证据。GameTestHelper.makeMockSurvivalPlayer:197–206仅new Player并返回，没有addFreshEntity，不应臆测测试玩家被spawn并死亡掉落。

给未投料冷坩埚加真实玻璃罩，保留燃烧箱与冷坩埚之间一格空气的热断路和另一个满容量坩埚的真实投料，可消除外来实体对该负例的干扰。不能关闭实际吸入、每tick重置内容或删掉空内容断言后宣布同一测试通过。
