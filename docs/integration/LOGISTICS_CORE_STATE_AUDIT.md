# 物流核心结构、CPU状态和显示核查

第七十一批，2026-10-21，基线`268f5b99d`。开始物流核心17997的完整原版核查，本批先归档实际注册、结构／CPU／能源状态、显示停止资格和右键信息，新增ALIGN-252–254，扩003／036／218／232／233／247。实际搬运、接口变化、调度相位、生命周期和客户端仍在同一全面队列内，本批不宣称整套物流已验收。

原只读根`C:/Dev/gregtech6-master/gregtech6-master/src/main/java`，当前根`C:/Dev/GregTech6`。37份原来源、63份当前输入、47份共享原生源的94个平台视图和五源码八份执行比较见[回执](verification/logistics-core-state-audit-20261021.json)。只对报告列明的声明和消费者留证，不把来源数量当完成百分比。

## 实际注册与已有结构、能源

原Loader_MultiTileEntities注册17997，stack16、镀锌钢、硬度／抗爆6／6、默认EU。Forge实际入口GregTech.initialize→MachineBlockBootstrap→GTLasers.registerAll，用laserProps(6,6)创建LogisticsCoreControllerBlock；GTMultiblocks.LOGISTICS_CORE是同ID引用。Neo由GregTechNeoForge调用共享BlockBootstrap→GTMultiblocks.registerAll直接登记同一方块，GTLasers的该行不选入Neo投影。两版GTBlockEntities和服务端ticker均指向专用BE，不因两版注册路径不同就报重复或漏注册。

结构以主控向后两格为中心，5×5×5共125格，主控占面中心一格：距离平方<4为27个CPU位，>6为44个框架墙，其余53个通风单元。CPU可用18008墙替代而不贡献计数，18200通用CPU给四计数各1，18201–18204专用CPU给对应计数4；四类都需大于0，存储计数夹到108。core.LogisticsCoreLayout与原分类／贡献公式有对应，当前原生Cell.at处理六向局部坐标；有限坐标回执只直接重建北向分类，不冒称已在世界搭出六向。

框架绑定ENERGY_INPUT、通风绑定LOGISTICS、内部CPU绑定CASING。MultiblockPortBlockEntity.canLogistics明确同时允许前两种角色，委托原物流核心身份；不能看到单一能源枚举就误报框架不能走物流。当前完整候选后整体认领，原逐格有效认领／四角关键区块未加载保留旧形成值的差异仍扩036。

全27通用CPU时四计数均27：每秒路由门槛128+27×64×27=46784EU，名义缓存128+27×256×27=186752EU，逐tick固定耗电20+108=128EU。合法整包可短时超过名义缓存，超过后先拒收；1024以上明确直接强度6危险、模拟无爆炸和有界算术仍使用先前修复的DirectMachinePowerRules，未为审计重新引入危险。默认主控六面EU输入已有，不套禁刷怪塔的主控底面限制。

原源码的108格库存／108个16000L罐没有参与内置路由，右键原文还明确标注存储CPU暂时无用；它仍影响形成资格和固定耗电。当前也有私有108缓冲而未参与路由，不把“存储计数没有搬运到核心”直接当缺失工作功能。原和当前的存储使用计数为0也不能凭直觉补造用途。

## ALIGN-252：停止覆盖板时CPU显示仍刷新

原onServerTickPre在`tCovers != null && !tCovers.mStopped`中更新CPU显示的value和visual。当前物品／流体Router.collect已检查PanelCoverHost.panels().stopped并跳过该宿主的总线；但BE.updateDisplays独立遍历所有已发现宿主，LogisticsCovers.setDisplay也没有停止门槛。两条链不能混成“当前全部覆盖板都不受停止控制”。

给定有电且形成的全通用CPU核心，网络中某宿主的覆盖板处于停止状态，控制CPU显示旧值0、上次使用量8／总数27：原保持0，当前写入value5、visual3。当前LogisticsCoverSignals和方块弱／强红石入口读取该value，源码路径已有；这里仅为资格和刻度的条件推导，没有实际贴板或测红石，也不说停止某个宿主的板会停止整个核心网络。

## ALIGN-253：结构失效后CPU计数清除改变耗电与容量

原checkStructure2在关键区块可用时先清计数，随后完整扫描，即使某框架错误仍继续统计正确CPU。返回结构失败不会额外把这些计数清零，固定耗电和名义缓存仍用它们。当前LogisticsCoreStructure.counts遇错返回null，BE.unbind清processors，随后固定耗电回退20、容量回退128。

给定27个通用CPU仍完整、缺一必需框架，原强制完整验证后四计数仍27且结构无效，当前processors=null也无效。双方从100000EU开始、不发生路由，原一tick后99872，当前99980；原名义缓存186752，当前128。另从相同100000初始缓存注入256×1，原可接受1，当前因缓存已超过128而拒收。两个例子是分别给定的源分支，不混成同一次已经执行的测试。

当前读取存档后不信任保存CPU、等待实际重建，是有意的保护性取舍；本轮记录其运行合同影响，不恢复未验证结构的缓存状态或危险行为。世界保存／重载及缺区块时序仍需独立验收，不能据本条推断当前会多爆炸或丢掉全部运行电量。

## ALIGN-254：右键信息少了当前电量、实际范围和变动成本说明

原onBlockActivated3在结构已形成时报告Power、固定消耗并注明每次物品／流体搬运另耗能、四类CPU，以及2+control的立方范围。当前方块在裸点击面／空手／无覆盖板交互拦截时，仅发送四CPU计数及固定EU/t。

例如形成结构、100000EU与27控制CPU，原消息明确有100000EU和29m范围，当前没有这两项，也没有逐次搬运附加成本提示。BE已有storedEU与范围计算，不是底层没有数值。本条仅核这条右键消息，不扩大为Jade或所有界面都无法读能量。当前直接拼接的Structure Incomplete／Logic等同时扩003；不要求保留原Comsumption拼写错误或照搬已过时说明。

第七十二批继续实际搬运后，254另扩过滤覆盖板放大镜缺所选样本／未设置提示和数量0含义；不是新造重复编号，详见[搬运专题](LOGISTICS_TRANSFER_AUDIT.md)。

## 已追到的路由入口与待续范围

当前serverTick每20个世界gameTime尝试路由，逐tick扣固定能量；LogisticsNetwork以坐标快照扫描加载宿主并要求相邻双方开放。LogisticsRoutingPass共用core顺序，物品／流体先后、Generic／Semi／Filtered等级、存储整理及Dump有实际实现，直接物流存储、半过滤和保留物品也有分支。它们不是只注册空壳；本批读取这些声明主要为排除状态归因错误，尚未逐项接受所有搬运结果。

原由GT_API_Proxy的SERVER_TICK_PRE／SYNC_SECOND驱动，当前由方块tick和世界gameTime驱动；两种时点的实际世界顺序后果还需追原生调用。WD.te的允许加载参数、原相邻代理、当前hasChunkAt／直接坐标、同优先级顺序、模拟与执行期间能力变化、数量控制和返还、外部节点扩展，以及网络／覆盖板保存与客户端都留在后续物流核查。不能仅凭一次路由核心算术或绿色构建就结束整套物流验收。

| 既有记录 | 本轮范围 |
| --- | --- |
| 003 | 右键Structure Incomplete／Logic等硬编码未进入语言入口 |
| 036 | 完整候选／有效子集认领和关键区块缓存差别 |
| 218 | 17997原stack16，两版实际普通BlockItem默认64，已核能源设备22→23项 |
| 232 | 完整查询至少124格状态读取再建候选／绑定，20次至少2480格；每秒路径还重复验证，不是TPS测量 |
| 233 | 已有活动扳手六向ToolInteractionTarget和覆盖板配置，不等于原Base10专用结构建造／放大镜；BE无MultiblockToolTarget |
| 247 | 原gt.energy.accepted允许已知RU等身份，当前固定EU且load未读该键；默认EU正常 |

## 五份源码整理

[core布局／计数](../../core/src/main/java/com/gregtech/gregtech/content/logistics/LogisticsCoreLayout.java)、[core输入计划](../../core/src/main/java/com/gregtech/gregtech/content/logistics/LogisticsCorePowerRules.java)、[主控BE](../../multiversion/src/main/java/com/gregtech/gregtech/blockentity/machine/LogisticsCoreControllerBlockEntity.java)、[方块入口](../../multiversion/src/main/java/com/gregtech/gregtech/block/machine/LogisticsCoreControllerBlock.java)和[路由边界](../../multiversion/src/main/java/com/gregtech/gregtech/content/logistics/LogisticsRoutingPass.java)已展开压行与错误缩进；距离、tick参数改用depthFromCenter／distanceSquared、controller及明确ticker名称。删除BE一条完全相同的重复LogisticsCoverHost import，保留全部公共类型、字段、公式及分支。

比较只先从旧源删除这一条精确重复import，再在列明方法区域归一局部绑定，其余完整执行词法及字面量位置相同：core490／129，BE Forge3094／Neo3135，Block1112／1287，路由边界394／394。不是全局替换core／controller；四处说明分别限定软上限计划、无电使用量复位、扫描会验证／绑定及需求值单位，许可证、其它注释和Stonecutter指令保持。

原MultiTileEntityLogisticsCore头载GregTech-6 Team2025、Gregorius Techneticies、LGPL-3.0-or-later，其余来源各留头／摘要；未将原生／原作者方法复制进生产。用户CompoundMaterials／WoodMaterials摘要保持，未Java编译／目标初始化、Gradle、测试、游戏、实际路由／UI／覆盖板、pack生成、磁盘重启、构建或推送。正式254条包含已修复／部分修复、扩展合同和保留政策，不是254个待修故障；全面目标保持进行。

第七十三批继续[网络与生命周期](LOGISTICS_NETWORK_AUDIT.md)：新增258–260的撬下样本、万能代理和回调异常，区块保护／平手／同步链也已补证。实际世界调度、外部节点、重启和客户端仍未整体接受；此前待续项不作为永久未核结论。

第七十四批继续[客户端与线形状](LOGISTICS_CLIENT_AUDIT.md)：新增261前景自亮、126扩连接成本；图集／刻度／红石与两端renderer订阅已有。源码与资源核查不代实际客户端、磁盘重启或完整物流验收。
