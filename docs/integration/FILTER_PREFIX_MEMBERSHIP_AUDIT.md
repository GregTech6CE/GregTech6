# 前缀过滤器成员集与原版物品识别核查

2026-10-21，第八十五批，基线 `533628204`。新增ALIGN-283，承接[模板与展示核查](FILTER_BLOCKS_AUDIT.md)及[材料相态核查](FILTER_FLUID_TOOLS_AUDIT.md)。来源、摘要、平台视图及完整执行流保持证据见[回执](verification/filter-prefix-membership-audit-20261021.json)。283条正式记录包含已修、部分修复及政策，不是未修问题数量或完成百分比；全面核查继续。

## ALIGN-283：原版物品的实际前缀成员未被当前适配器完整消费

原版MultiTileEntityFilterPrefix保存独立的OreDictPrefix。GUI先尝试OM.anyassociation_返回的主关联；没有主关联或主关联等于当前选择时，再扫描包含候选物品的其它前缀。只有备用扫描排除UNIFICATABLE和NO_PREFIX_FILTERING，直接采用主关联不受这一条件限制。allowInput按mFilter.contains判断实际注册成员，再与黑名单异或。

已核实两个具体身份族，而非仅凭“多集合改字符串”的结构差异下结论：

- 原GT_API把原版铁块明确设为OP.blockIngot／MT.Fe目标，setTarget_在未注册时提交矿辞，并写入前缀关联。OreDictManager解析前缀后分派onOreRegistration，OreDictPrefix将物品加入mRegisteredItems；contains消费这个集合。因此原版铁块有可直接采用的blockIngot主关联。
- 原LoaderWoodDictionary明确把六种原版木板meta0–5注册为plankWood，OP.plank没有备用筛选排除标志，注册事件将其加入plank集合。GT_API_Post确实运行该加载器。PlankEntry还处理树种材料数据；材料组成和集合成员是两个不同职责。本批证明plank成员及备用选择分支存在，不声称枚举了全部成员关系或测过完整点击轮转顺序。

当前两版原生Items／Blocks声明及Forge映射字节码表明，上述现代原版木板和铁块是普通BlockItem包裹普通Block。它们不会走GT的MaterialFormItem、容器形状、管道或MaterialBlockLike分支。在这之后，[FilterRules.prefix](../../multiversion/src/main/java/com/gregtech/gregtech/content/logistics/FilterRules.java)只按顺序检查ingots、nuggets、dusts、plates、rods、gems六个通用标签；Neo额外接受对应c命名空间，但仍返回同样六个前缀。无论六组标签如何组合，这条原生物品路径都不能返回plank或blockIngot。

这不是“木板材料没有登记”。VanillaWoodProcessingRecipes已为现代原版木板登记MaterialPrefix.plate及对应树种，MaterialTagPack.registeredForm也会读取显式组成并生成通用板标签和父标签。因此木板可以沿当前数据路径被识别为plate；这仍不能表达原版独立的plank家族。外部数据包可能添加其它标签，不能把所有现有标签都报成空；只要物品仍是上述原生类型，当前六组返回值依然无法表达这两个家族。

缺口抵达实际消费者：[FilterMenu.setGhost](../../multiversion/src/main/java/com/gregtech/gregtech/client/gui/FilterMenu.java)在前缀模式拒绝无法推导前缀的候选；[FilterBlockEntity](../../multiversion/src/main/java/com/gregtech/gregtech/block/misc/FilterBlockEntity.java)从模板重新推导selectedPrefix，筛选时比较同一字符串。不存在独立保存／选择多个真实成员前缀的入口。[LogisticsItemFilter](../../multiversion/src/main/java/com/gregtech/gregtech/content/logistics/LogisticsItemFilter.java)的前缀预留也委托相同适配器；本批未据此宣称整个物流调度已验收。

两平台×七个身份形成14项源码身份条件；两平台×六组布尔标签的64种组合形成128项有限返回值分析，均不能产生所需plank／blockIngot。它们不是实际Minecraft注册表、界面点击或搬运测试。没有凭空设定当前普通GUI能保存一个它无法选择的plank模板，再把该假设当成实测输送故障。

## 已有路径与需要保留的边界

MaterialFormItem主前缀、cell容器的capcellcon、按粗细拼接的管道前缀、GT材料方块的BlockMaterialPrefix及六种通用标签转换已经存在。物品模板本身的registry identity／components匹配也没有在本轮改变。两版filter_oredict注册仍在，不能报成整个过滤器尚未注册。

本批没有修改任何标签、配方、材料关联、选择操作、保存格式或筛选结果。后续修复需要明确现代扁平ID／components与多成员前缀的合同，复用真实材料数据，区分主关联与额外集合，不能把任意forge／c标签都直接当作原版前缀。ALIGN-281的周期展示与这里的身份选择／成员匹配是不同缺口。

原生证据来自Neo21.1.243源码及实际使用的Forge1.20.1-47.4.20映射工件：四条源码、四份只读javap输出，均记录摘要；javap只读取保存的类，没有初始化目标类。另对31个追踪生产Mixin.java文件做接口声明盘点，未发现直接向原生类添加这四种GT识别接口的声明。该有限盘点不冒充第三方注入或运行后类型验收。

## 源码可读性与验证范围

展开core的ContainerShapeLike、PipeFormLike接口，保持原签名；展开共享FilterRules的短条件和六组映射，template／candidate等参数、item／pipe／block模式变量及entry／result／word局部变量改为能说明用途的名称。LogisticsItemFilter展开短条件，并把两处说明改为准确描述当前“模板物品或一个推导前缀”的职责。

四源六执行视图的完整有序Java token流在限定局部命名归一后保持：21、21、514／505、235／235；未归一字符串／数值／字符字面量及其位置、其它注释、Stonecutter指令分别核对。14份原版／28份当前输入和34份共享平台视图归档；原版版权／作者头保留。用户CompoundMaterials的570项对齐和WoodMaterials现有修改保持，不编辑或提交用户材料表。

未Java编译／目标初始化、Gradle、测试、游戏、最终注册表／数据包、实际选择／UI／运输、保存重启、构建或推送。全部前缀关系、现代身份政策、其它宿主与全面功能／可读性队列仍未完成，goal保持active。
