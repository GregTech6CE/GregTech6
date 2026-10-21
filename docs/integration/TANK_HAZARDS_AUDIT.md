# 多方块储罐危害、木阀火属性与自动输出核查

第七十六批，2026-10-21，基线`a9da9ba85`，承接[主阀与保存核查](MULTIBLOCK_TANK_AUDIT.md)。新增ALIGN-264木主阀缺原可燃性、265熔毁邻格点火政策差异。14份原版、28份当前输入、36个平台视图及本机两个原生工件6个entry／3份只读javap视图见[回执](verification/tank-hazards-audit-20261021.json)。这不是全部危害、物性、工具或实际游戏验收。

原只读根为`C:/Dev/gregtech6-master/gregtech6-master/src/main/java`。作者／LGPL来源头随回执留存，原目录未动；MT原文件独居石注释已有两个替换字符，单列来源计数，未修改或借此证明该注释正确。CompoundMaterials和用户未提交的WoodMaterials保持本批起点字节摘要。

## ALIGN-264：木储罐壁板已有可燃性，主阀未接原属性

原Loader_MultiTileEntities对18001木壁板和17001木罐主阀均明确写NBT_FLAMMABILITY=150。Base07Paintable读取该值，getFlammability与getFireSpreadSpeed都返回mFlammability；实际MultiTileEntityBlock经对应接口委托到BE。没有将纹理或木材名称当作火属性证据。

当前[OriginalMultiblockPartData](../../core/src/main/java/com/gregtech/gregtech/content/multiblock/OriginalMultiblockPartData.java)已给18001返回150，[MultiblockPortBlock](../../multiversion/src/main/java/com/gregtech/gregtech/block/machine/MultiblockPortBlock.java)的两个原生火回调实际消费它。因此木壁板不列为缺失。

17001则由MultiblockBlockFactory通过metal属性构建TankControllerBlock。TankValveParameters没有可燃性字段，TankControllerBlock也未覆写这两个火回调，当前生产输入没有为该主阀注册原生燃烧表条目。两版实际原生默认回调分别读取FireBlock.getBurnOdds／getIgniteOdds，不能从主阀材质名称自动得到原150。这里报告缺原显式属性，不推为任何模组环境下永远不可燃，也没有运行点火、传播速度或工具提示测试。

## ALIGN-265：熔毁时邻格点火对象与保护政策不同

原3³／5³的meltdown对整个立方体逐格调用WD.burn(F,F)，后者访问六个相邻格。WD.fire先排除lava／fire，再允许carpet材质或无碰撞包围盒的方块；它还具有Thaumcraft节点与IItemGT排除分支，不能概括为无条件点燃所有格子。

当前[MultiblockTankControllerBlockEntity](../../multiversion/src/main/java/com/gregtech/gregtech/blockentity/machine/MultiblockTankControllerBlockEntity.java)要求邻格已加载、不是FIRE、流体状态为空且碰撞形状为空，才尝试放置火。有限的源分支差异为：

| 邻格条件 | 原版 | 当前 |
| --- | --- | --- |
| 已加载、普通且有碰撞形状的地毯 | 尝试替换为火 | 不进入替换分支 |
| 已加载、无碰撞的非岩浆流体，无GT／节点排除 | 尝试替换为火 | 因非空流体状态跳过 |

两版现代CarpetBlock的原生输入保留了1像素形状证据；这里比较条件与放置尝试，不宣称火在世界中一定持续存在。保留当前已加载／液体保护，本批不为逐字对齐扩大破坏范围。265是已确认的政策差异，不应直接计为必须恢复的旧行为。

## 已有危害分支及范围

原tick在服务端且结构形成时处理非空罐，依次检查温度熔毁、魔法、酸、等离子、气体、allowFluid中的导能／温度／木罐simple限制，之后才自动输出。当前同样已有这些分支和对应防护门槛，没有“整套危害没实现”的证据。是否命中同一物性集合仍需分别验收。

原酸与魔法破坏、当前damageShell都遍历整个3³／5³立方体，不只外壳，每格1/3，另强制处理主阀；熔毁的格内随机替换是1/4。整个立方体分别27／125格，邻格访问在排除前最多162／750次，这是源码循环计数，不是世界破坏数量。3³的原岩浆中心分支与当前仅在原版岩浆／流动岩浆且至少1000 L时处理；5³没有对应岩浆池分支。没有据此验收全部火传播、声音或跨区块效果。

原可选Thaumcraft Flux替换在相关内容存在时有额外污染效果；当前用空气替代且未接该污染。原未安装时的默认缺对象路径不能直接当作所有现代环境都必须生成污染，完整兼容仍待核查。

## 自动排液的明确正向证据

原条件为水平输出，或气体，或按密度负值向上／非负值向下。当前[TankFlowRules](../../multiversion/src/main/java/com/gregtech/gregtech/content/multiblock/TankFlowRules.java)委托core StructureGrid.canAutoOutput，同一公式。六向×气体真假×密度-100／0／1000共36个源输入的允许结果一致；这不证明每个实际流体都被分类为同一状态。

原FL.move(IFluidTank,DelegatorTileEntity)默认以Long.MAX_VALUE请求，经整数限制向目标填充，再排出对应源量；当前pushFluid也模拟源可用量、填目标、再排源。主阀目录最大容量小于Integer.MAX_VALUE，两边此处都没有额外1000 L节流。源2000 L、目标全接受的有限分支均转移2000 L。面向与目标反面能力已有显式连接，但没有摆管道、跨tick、拒收／回调故障或真实吞吐测试。

## 物性边界与排除的误报

当前温度先用已注册entry，再对未知流体用原生类型；GTFluids创建两版类型时也写入entry温度。生成熔体的最终entry明确使用材料熔点，不能只读fluidMolten辅助方法的默认环境温度就报所有熔体常温。蒸汽两边明确373 K。其它专用熔体声明／覆盖及外部动态温度仍未全部验收。

当前主阀魔法分支只看显式MAGIC，FluidHazards.isMagic还会解析MAGICAL材料。材料导入属性本身已有其它审计，必须找到经过前序熔毁等分支后仍可达的实际流体才能作完整行为见证；本批仅记录待核边界，没有将条件性不同伪装成已复现腐蚀。

未知外部气体当前用原生lighter-than-air近似，旧isGaseous是独立标志。已声明GT气体优先读entry，不能把重于空气的所有GT气体都报成液体，也不能把近似说成完整复刻。外部酸／等离子／魔法的兼容集合仍待核查。

## 源码整理与验收限制

展开MultiblockPortBlock的Neo交互、原生火回调、渲染状态与掉落方法，两处局部id改registryId；纠正基础掉落说明，明确最终覆盖板数据由Mixin处理。展开TankFlowRules委托。FluidHazards缩短过长的总说明，明确依赖注册表与外部气体近似，不再把它描述为独立于注册状态的纯函数。

三源码六份完整执行流保持：Port1230／1469、Flow74／74、Hazards771／771，仅归一明确局部绑定并排除成员访问；原始字符串／数值位置另核。三处说明纠正，其余注释／来源及Stonecutter指令保持，材料表不动。

未Java编译／目标初始化、Gradle、测试、游戏、构建或推送；javap只读取现有工件。真实起火／灭火、客户端／Jade、音效、存档重启、配方和完整物性验收仍在全面队列，goal保持active。
