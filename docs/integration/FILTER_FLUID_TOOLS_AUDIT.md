# 过滤器材料流体回退、工具入口和展示接口核查

2026-10-21，第八十四批，基线 `49f8eda2e`。新增ALIGN-282，扩充125／126；正式282条含已修、部分修复和政策，不是待修数或完成百分比。来源及保持证据见[回执](verification/filter-fluid-tools-audit-20261021.json)。承接[过滤器方块](FILTER_BLOCKS_AUDIT.md)，完整目标继续。

## ALIGN-282：纯流体过滤器缺少材料环境流体的模板转换

原30257只有TANK模式。原GUI的正常持物左键分支先FL.getFluid，未得到流体时调用OM.anyassociation_，再取材料fluid(U,true)，有效则存FL.display。它不要求材料形态本身提供IFluidHandler；这与物品过滤器直接复制模板是两条路径。

有效例子是牛奶粉：原MT.Milk明确有DUSTS，熔点273、沸点373；Loader_Recipes_Food还实际引用OM.dust(MT.Milk)。原环境温度C+20=293，材料fluid在这个温度选择liquid，Loader_Fluids以液态状态给MT.Milk绑定milk，FL.create确实把该流体写入材料liquid。该路径不会落入FL.Error，不是拿铜熔融状态作推测。

当前CompoundMaterials.Milk声明dust并保留273／373；MaterialFactories、MaterialPrefix的HAS_DUST及MaterialItemDefinitions保留声明生成路径，两版实际队列都据定义创建MaterialItem。milk也在共享FluidCatalog及实际流体／展示物注册流中。没有运行材料初始化或最终注册表，所以这里证明声明生成及消费链，不报实际物品总数或全部外部别名状态。

当前FilterMenu.setGhost仅复制牛奶粉，FilterBlockEntity.permitsFluid只尝试FluidDisplayBinding和FluidUtil.getFluidContained。前者明确只解析两版自己的FluidItem／FluidDisplayItem载体；普通MaterialItem不属于它们。后者查询实际流体容器能力，不能由材料身份自动推导环境相态。MaterialItem没有自带流体处理器，Neo内置ITEM流体能力注册的三组候选也不含它；Forge默认IForgeItem能力入口仅为列举原版潜影盒创建物品库存包装，并非默认给粉末供液。

因此在本模组内置路径、无第三方额外能力注入、纯流体过滤器非主面、已加载且愿意接收牛奶的目标条件下，原白名单牛奶粉模板会匹配牛奶，当前不匹配；黑名单相应反转。两平台×两模式×六主面×五筛选面，共120个有限源条件。主面仍绕过模板，实际转移量／模拟回调／世界结果没有测量。

现成流体展示模板及真正容器能力的入口仍在，不报所有流体模板不可用。修复应显式恢复有效的材料相态转换；不能让FluidDisplayBinding泛化成任意ItemStack解析器，也不能把原fluid(U,true)一概替换成molten/liquid。铜在293低于1357熔点、旧helper会回Error的反例继续保留为政策边界，未自动恢复。

## ALIGN-125／126：无覆盖板过滤器的工具读数与费用

原普通／前缀过滤器onToolClick2用放大镜报告当前黑白模式，返回1；螺丝刀反转返回2000，软锤清除并复位返回10000。三种原GT工具都实际安装mDamage=100的Behavior_Tool，后者经IBlockToolable取得返回值，再调用units(return,10000,100,true)。这不是从名称推定费用。

当前限定普通GT工具、非创造、可用、非潜行且无覆盖板。GTToolItem.onItemUseFirst先尝试MultiblockTools，但过滤器不是该专用目标；放大镜随后无本机处理。空覆盖板的组件／通用／面板事件路径均不消费这一点击；ToolInteractions对放大镜没有转向描述，FilterBlock只识别螺丝刀／软锤，最后调用打开配置菜单。缺少原专用模式消息与相应扣费；菜单自身能显示模式，不写成玩家完全无法查看模式。

| 工具操作 | 原返回 | 原普通GT工具请求单位 | 当前请求单位 |
| --- | ---: | ---: | ---: |
| 螺丝刀切模式 | 2000 | 20 | 1 |
| 软锤清模板 | 10000 | 100 | 1 |
| 放大镜读模式 | 1 | 1 | 0 |

当前螺丝刀／软锤的状态改变仍存在，问题是固定damageForUse(1)没有消费原返回合同。两平台六项请求条件与八项无覆盖板诊断入口留痕；不扩大到电动、外部工具、创造或真实损耗刻度。按已有125／126追加宿主，不能全局给所有工具乘100。

## 真实流体展示接口与源码整理

FluidDisplayBinding的两版实现保留实际流体身份、数量和NBT／components序列化；材料粉末不会被当作展示载体。Neo的lookup获取／空lookup防护、外部流体使用备用载体的路径，以及Forge自己的载体查找都未改变。解析不等于已验证所有外部流体往返、客户端切服或失效lookup。

展开FluidDisplayBinding，局部id／item／out／tag明确为fluidId／carrierItem／displayStack／payloadData；展开两版流体物品的读取、展示和handler方法，handler局部f改resolvedFluid。ManualToolRules只展开方法排版，公式不变。四源五执行视图421／697、1297、1510、129及原始字面量位置保持，五处注释据实际消费者纠正。

两个物品的显示名／tooltip／光效及供液handler会读取实际payload；饮用use仍按载体fluidEntry选注册饮料，非创造时消耗一件并尝试把空瓶加入物品栏。handler本身没有玩家权限检查，也不消耗载体。这些是现有实现事实和注释边界，不在本批推定新的世界漏洞，也未验收原显示物品／现代瓶子适配或获取政策的完整一致性。

## 证据边界与后续

24原版／41当前源码、44共享平台视图归档。原MT有两处已存在的U+FFFD注释字符，原始摘要及数量单列、只读保留。CompoundMaterials与WoodMaterials摘要不动。

原生工件定位沿用第82批实际Forge47.4.20／Neo21.1.243；本批另取FluidUtil、IForgeItem及默认潜影盒包装四源码条目，三份只读javap核映射方法。原先若把IForgeItem.initCapabilities概括成恒null会不准确，根核查已追其潜影盒分支，不能靠该错误简化证明粉末没有流体能力。

未Java编译／目标初始化、Gradle、测试、游戏、实际材料／前缀注册表、UI、供液／饮用、损耗、保存重启、构建或推送。其它材料与温度、容器和外部能力、多前缀真实成员／选择、电动工具／权限、展示载体的其它消费者及全项目队列继续，整体移植仍未完成。
