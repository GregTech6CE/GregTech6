# 物流数量、过滤与实际搬运核查

第七十二批，2026-10-21，基线`948871986`。继续[物流核心结构与状态](LOGISTICS_CORE_STATE_AUDIT.md)后的实际搬运链路，新增ALIGN-255–257，扩003／126／254。23份原版、25份当前输入、40个平台视图，以及本机两个原生工件的15个entry／7份只读javap视图留在[回执](verification/logistics-transfer-audit-20261021.json)。来源摘要只固定本批证据，不表示这些文件的全部功能或整套物流已验收。

原只读根为`C:/Dev/gregtech6-master/gregtech6-master/src/main/java`；作者和许可证头随来源记录，没有改原仓库。当前材料表，特别是用户调整的CompoundMaterials与WoodMaterials，保持本轮开始时的字节摘要。

## 实际消费者与已有功能

原核心`MultiTileEntityLogisticsCore.moveStacks/moveFluids`调用ST.move／FL.move；当前BE.routeNetworkOnce经过LogisticsRoutingPass和core的LogisticsRoutingRules，最终进入[物品搬运](../../multiversion/src/main/java/com/gregtech/gregtech/content/logistics/LogisticsItemRouter.java)与[流体搬运](../../multiversion/src/main/java/com/gregtech/gregtech/content/logistics/LogisticsFluidRouter.java)。两平台仅在原生能力、物品数据和序列化边界分开，实际路由业务共用。

导入优先补导出、再补存储，Filtered→Semi→Generic目的等级及Generic→Semi→Filtered来源等级、每对先流体后物品、存储整理与最后Dump均已有实现。一个物品成功搬运按数量耗EU；流体按每250L向上取整，每次流体对上限为16000L×转换CPU。双方都仅在两端目标数量均为0时，才按转换CPU重复搬物品；固定目标在一次成功后结束。原ST.move同样逐源槽尝试，没有把多个不足源槽先凑成一组，不能将这一点误报为当前独有缺失。

当前物品实际插入被拒时，会尝试塞回来源；塞不回的余物在来源位置掉落。流体没有这条掉落／暂存路径，见257。FilterRules.itemMatches的普通模板规则，与物流覆盖板和Dump保留集合使用的LogisticsCoverInteraction.matches是两条实际消费者，不能拿前者已经比较标签／组件来证明后者也比较。

## ALIGN-255：目标数量不足时的严格等待是保留政策

原ST.move先用目的槽剩余容量算`tMovable`，检查目的最小目标，进入move_后才按真实源数量缩小。当前先模拟提取，再用真实可用量判断目的数量，接收端模拟后还会再检查一次。

给定兼容的普通64堆叠物品：源10、目的32，源目标0、目的目标64、转换CPU1。原计划32，32+32达到目标64，实际只搬10，目的变42；当前只可提供10，32+10不足64，不搬。该例是逐分支算术推导，没有运行两份游戏代码。

当前严格目标语义应保留，不为逐字相同恢复原版先补不足数量的缺陷。本条是明确的行为政策差异，不计作待修复的物品丢失或全部数量控制失效。

## ALIGN-256：旧元数据药水变种在现代过滤中混同

原物品过滤保存物品和元数据，ST.hashset经ItemStackSet／Map／Container保持该身份；原ST.equal忽略NBT时仍比较元数据。原Loader_Fluids已有明确例子：治疗药水使用`Items.potionitem`元数据8197，迅捷药水使用8194，二者不能通过同一个普通过滤样本。

当前[过滤匹配](../../multiversion/src/main/java/com/gregtech/gregtech/content/logistics/LogisticsCoverInteraction.java)仅比较物品身份和getDamageValue。真实样本序列化仍保存了完整数据，但匹配时不消费药效数据。ItemRouter的源／目的过滤、其预留集合和LogisticsItemFilter均会使用该谓词。

本机Forge 1.20.1映射jar的ItemStack.getDamageValue委托Item.getDamage；Item与PotionItem没有覆盖该方法，由IForgeItem读取`Damage`，PotionUtils.setPotion另写`Potion`。Neo 1.21.1源码工件也有实际委托，IItemExtension读取`DAMAGE`组件，PotionContents.createItemStack写`POTION_CONTENTS`。所以普通治疗／迅捷药水物品身份相同、损伤均0时，当前匹配为true；不是直接把getDamageValue当作旧meta的等价实现。

这里只确认这组原生药水变种，未泛化为所有组件物品。未来修正需识别原元数据所代表的语义变种，同时保留原来忽略自定义名称等NBT的规则；直接改成全数据相等也不对。本轮只纠正过强说明，没有更改过滤玩法或运行实例。

## ALIGN-257：流体返还被拒时没有保留余量

原FL.move先模拟排出，再实际填入目的端；目的端实际接受0则在实际排出来源之前返回。当前先模拟两端，实际排来源，再实际填目的端；实际填入减少或排出流体不符时，尝试向来源fill返还，却没有处理返还值，也没有保存剩余流体。

条件例：来源有1000L且该端点只许排出、不许填入；目的端初始允许填入且容量足够。来源的实际排出回调会关闭目的端后续填入权限，但不清除目的端已有内容，模拟排出不触发回调。原先实际填入1000，再实际排源1000，即使随后关闭填入权限，目的端已收到的1000仍保留。当前先排1000，关闭权限后目的实际接收0，来源返还0被忽略，报告搬运0且1000L没有缓冲。这是相同初始状态／回调规则下的顺序推导；另外，原实际填入返回0时不排源的分支本来也存在，不能把它混成这个先前尚未触发回调的原版执行。现代IFluidHandler.fill的返回值明确代表实际或模拟填入量，不能把发起返还调用视为返还成功。

这是一条有条件的可变能力路径，没有安装特定addon或证明普通稳定FluidTank会这样失败。当前先排后填也避免了原先填后排而不检查实际排出结果的复制风险，不能盲目恢复原顺序。后续应设计明确的失败保留合同，再核对可变能力和保存。本轮把“余液已返还、不会静默丢弃”的注释改成实际的尝试返还语义，未伪称已修复。

## 既有工具与信息记录扩围

| 记录 | 本轮实际证据与边界 |
| --- | --- |
| ALIGN-126 | 原物流螺丝刀返回10000、剪线钳1000、软锤重置10000、放大镜1；四种普通GT工具的Behavior_Tool因子均100，经units换算请求损耗100／10／100／1。当前配置操作固定damageForUse(1)，放大镜分支不请求损耗，即1／1／1／0。Neo桥也直接委托共用GTToolHelper；不是没有通用换算函数，而是此入口绕开它。此处说请求值，未假定创造／附魔／材质规则之后每次实际损耗固定，也没有套用到电动工具。 |
| ALIGN-254 | 扩至过滤覆盖板放大镜：原物品总线报告未设过滤或注册名＋meta，流体总线报告未设过滤或流体名；原数量0有Variable含义。当前describe只给数字优先级和目标数量，不给所选样本／未设样本说明，也不解释目标0。已有核心右键缺电量／范围的结论不变，不能泛化为Jade也无显示。 |
| ALIGN-003 | describe仍直接拼接Priority／Target Stacksize，归既有语言入口缺口，不恢复原拼写或过时开发批次提示。 |

当前未过滤流体对会枚举多个罐，首个流体不被目的接收时可继续尝试后面的流体；原无过滤FL.move(max)只询问一次无特定流体drain，失败即返回。原带过滤分支本来就会指定流体，不是所有多罐原来都失败。此选择范围扩展单列为行为观察，尚未据此改变现代策略。

## 源码整理与证据限制

展开共享定义表、原生类型映射和覆盖板交互的压行方法／条件／数据读取，previousValue与nextTargetSize取代两段局部old／next；流体路由只调整拥挤的停止条件、Neo样本读取和端点选择。未移动公开API或制造另一套平台业务。三处说明分别去掉已失效的显示板W4预留说法、限定现代损伤匹配，以及明确返还失败没有缓冲。

四份源码七份完整执行流在限定局部绑定归一后保持，词法项为540、370／370、1720／1729、1518／1521；字符串／数值位置另核，注释仅三处列明纠正，Stonecutter指令和其余注释保持。定义源58→86行，映射源37→77行；交互两平台最长行174→119／122，流体最长行136／141→118／124。并未用窄小函数检查替代整份选中源码比较。

未编译、初始化目标类、执行测试、启动游戏、构建或推送。javap仅只读反汇编本机已存在工件；有限算术不是实际搬运、兼容或存档验收。调度相位、加载／代理、同优先级平手、外部节点、异常生命周期／保存和客户端，以及其它完整项目核查继续，全面goal保持active。
