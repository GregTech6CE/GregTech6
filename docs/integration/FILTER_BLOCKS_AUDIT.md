# 四种过滤器方块、模板操作和前缀展示核查

2026-10-21，第八十三批，基线 `d711cb220`。新增ALIGN-280–281，扩充277的四过滤器实例。281条正式记录包含已修、部分修复和政策，并非待修数或完成百分比。本批来源与保持证据见[回执](verification/filter-blocks-audit-20261021.json)，承接[扩展器能力](EXTENDER_CAPABILITIES_AUDIT.md)与[信号](EXTENDER_SIGNALS_AUDIT.md)，不是检索器覆盖板的重复审计。

## 已有注册、筛选和保存路径

原30256、30257、30258由MultiTileEntityFilter提供物品、流体和两者模式，30259由MultiTileEntityFilterPrefix提供前缀模式；都没有REDSTONE位。当前两版实际注册filter_items、filter_fluids、filter_items_fluids、filter_oredict及FILTER实体，MenuType工厂读取prefixMode，服务端打开时写入同一布尔值；FilterScreen在两版实际客户端注册。

普通54个模板、前缀一个选择槽、空白名单阻断／空黑名单放行、有效候选found与blacklist异或已有；主面能力操作绕过模板选择也与原版一致。四类型默认前次面相对、六朝向六查询的144个有限路由样本相符，不扩大成全部扳手和次面状态验收。物品／流体插入与抽取共用选择条件，具体槽位、数量及simulate仍由真实目标能力决定。

原前缀过滤器allowInput(FluidStack／Fluid)直接true；当前有效流体在prefixMode下同样放行，不是漏筛选。前缀未选时有效物品在两黑白模式均阻断，主面例外仍在。实际物流过滤接口对黑名单返回null，白名单暴露模板／前缀；这只是已有入口，完整物流成员集和多前缀关系继续。

原软锤清模板并复位白名单，当前clearFilter一致；螺丝刀／界面按钮可切模式。两版原生save／load和getDrops写入模板／模式已有，CoverDrops对machine清理的类型条件不包含FilterBlockEntity，合并覆盖板数据也保留已有block tag。没有实际挖掘、再放置或重启验收，不能据此承诺跨版本存档迁移。

与上一批扩展器不同，FilterBlockEntity不覆盖getCover；其CapabilityRelay祖先使ownClass成立，BlockEntity上的fallback实现提供getCover，uses=true。ComponentCoverHostMixin走按选中面查询的fallback分支，ComponentCoverCapabilitiesMixin也能包装权限。不能把275／276的原生宿主资格结论直接套在过滤器上；装有覆盖板时的完整交互仍未实测。

## ALIGN-280：模板快捷复制与右键编辑操作缺失

原普通三类型GUI在玩家物品栏Shift点击有效候选、存在空模板且尚未重复选择时，把一件物品或FL.display流体复制到第一个空槽；源物品不消耗。当前玩家槽走AbstractContainerMenu的QUICK_MOVE分支，最终调用FilterMenu.quickMoveStack，直接返回EMPTY，没有复制模板。两版原生调用者也已沿源码／只读反汇编核到；回执列两平台×三类型×两模式的12个限定分支。

原空鼠标右键普通模板时，先保留物品并去NBT，再可设置元数据通配；右键已是流体展示的模板保持原值。手持有效物品右键也可写入模板。当前clicked在PICKUP且button==1时始终传EMPTY给setGhost，因此上述操作变成清空。四个代表条件的变化已列出，左键正常写入／清除仍在，不报整个GUI不可用。

未来迁移应明确现代components和已扁平化注册身份上的匹配模式，不能简单把旧W元数据写回现代物品。当前禁止模板实体取出、拖出、数字键交换的保护应保留；原Shift操作本来就是复制配置，不是取出幽灵物品。这里仅记录，不修改交互或筛选结果。

## ALIGN-281：前缀代表物品的定期展示未移植

原FilterPrefix把mFilter前缀身份与mCycle展示分开管理。服务端onTick2在aTimer%10==1且前缀有注册物品时选择一个家族代表，设置前缀名称并规整通配meta；空前缀／空集合清mCycle。getStackInSlotGUI返回mCycle，Slot_Holo继承Slot_Base.getStack，后者每次调用实际GUI库存方法，不是未使用的展示字段。

当前模板槽保存玩家复制的具体ItemStack，selectedPrefix从它推导字符串。FilterBlock不提供getTicker，DirectionalBlock没有另供ticker，两版EntityBlock默认getTicker返回null；过滤器及能力祖先也无更新模板代表的周期入口。FilterMenu的前缀槽直接读templates，FilterScreen照普通物品槽绘制，没有家族集合轮换。fallback覆盖板更新不修改这些模板。

限定有已选前缀和非空注册集合，原第1／11刻进入选择、第2／12刻不进入；当前四阶段均无模板展示选择入口。随机选择可能连续选中同一个物品，不能写成每十刻画面必变。本批没有实际渲染画面，也没初始化全部前缀注册表；多前缀选择／成员识别范围另待核，不把它混成已证明缺失。

## ALIGN-277 的追加实例与其余边界

四种FilterBlock也使用普通完整方块属性，没有shouldCheckWeakPower覆盖；其原版父类和四个INV／TANK模式均继承显式false及0弱／强输出。无覆盖板、一个有效开启拉杆向完整方块输入15、从其它面查询的240项追加源分支，沿两版原生默认导体和SignalGetter合并得到15；原合同为0。它们属于277同一机制，不再编号。

复用第82批3工件／10条目／11只读视图的原生导体证据，并核摘要；本批新增AbstractContainerMenu／EntityBlock两条Neo来源、两份Forge只读javap。未编译或初始化目标类，也不是Minecraft1.7.10引擎复现。

流体模板读取已尝试FluidDisplayBinding和FluidUtil.getFluidContained；但原GUI另有材料回退。根核查否定“材料一定选其熔融流体”的简化：OreDictMaterial.fluid(U,true)在DEF_ENV_TEMP依熔沸点分派，不满足时会返回FL.Error。有效材料／实际流体身份、容器与桶的所有回退仍待继续，不能把错误流体分支当应恢复功能。

## 源码整理与验证范围

展开FilterBlockEntity保存／读取、模板循环和能力谓词，i→templateSlot，load局部data／stack→entryData／templateStack；注释明确主面绕过选择。FilterBlock展开两版交互、工具、掉落和Neo包装，b／ctx限定方法改builder／context。FilterMenu展开构造／幽灵槽操作，col改column；FilterScreen展开布局／两版render，partial／button改partialTick／clickedButton。成员、调用、数值、注册及玩法保持。

四源八完整执行流1355／1399、1242／1536、820／820、454／449在限定局部命名归一后保持；原始字面量位置、唯一说明纠正、其它注释／Stonecutter指令及用户CompoundMaterials／WoodMaterials摘要另核。24原／31当前源码、42共享平台视图归档。

未Java编译／目标初始化、Gradle、测试、游戏、模板实际输入、画面、真实搬运／掉落／重启、构建或推送。工具费用／放大镜、材料流体与多前缀、所有次面／旋转、其它宿主和全面项目队列继续，goal保持active。
