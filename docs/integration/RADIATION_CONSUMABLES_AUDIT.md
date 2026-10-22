# Radaway、辐射剂量与食品属性装载核查

2026-10-22，第一百二十五批，基线 `0ea1ec71d`。上一轮重新核验 #78 的源码摘要，排除了避难所 Sky Stone 数量这一误报；前批防护服整理已完成本地提交。继续核查独立物品实际消费者，同时去重食品属性装载业务。九原版／16当前生产 Java、48段精确引用及18项实际原生来源见[回执](verification/radiation-consumables-audit-20261022.json)。原树只读，GregTech-6 Team／Gregorius Techneticies 的 LGPL 声明保留，未导入原生游戏实现。

## 422：Radaway 会重复扣减辐射剂量

原 `MultiItemFood:912` 的 Radaway 声明 `mRadiation=-50`，`FoodStat.onEaten` 直接更改一次追踪器。原普通食物事件另读 `FoodsGT`；这是另一条数据路径，`RM.crop` 明确排除 `MultiItemRandom`，食品监听也排除 GT 自有物品，不会因存在原食物事件就再扣一次。当前提取脚本也明确说明两条原版路径分开；移植表将它们合并到 `FoodStatisticRows`，所以不能继续保留同一物品的手工扣量而不排除事件。

当前两版实际注册的 `RadawayItem.finishUsingItem` 先调用 `PlayerRadiation.change(player,-50)`。原生 `LivingEntity.completeUsingItem` 在调用物品前复制完整物品栈，完成后将副本传到 `LivingEntityUseItemEvent.Finish`；事件的 `getItem()` 返回这个副本，并非已经吃空的结果栈。因此只剩最后一粒也不会靠空栈判断避开重复处理。`FoodStatEvents` 随后查到自有 `gt6ce:radaway` 的 -50 行，再扣一次。注册入口、事件发布、事件字段、表装载与具体回调分别追踪，未仅依赖方法名搜索。

有限源码见证：服务端玩家起始剂量120，正常服用本模组 Radaway，没有第三方替换／取消事件或插入剂量改动。原版扣后70；当前物品回调后70，事件后20。这是调用链与算术推导，没有执行游戏、食用方法或事件。历史文档曾把 Forge 重复处理列为待核；当前共享物品代码使两版都有该路径，此次提升为明确源码差异。本批只登记，未改变物品效果。

## 423：Radaway 服用时间为32而非16 tick

原 `MultiItemRandom.getMaxItemUseDuration` 是 `max(foodLevel*8,16)`，零营养 Radaway 因而是16 tick。当前 `RadawayItem`／`TechItem` 没有覆写服用时间：Forge 的食物 Builder 没有启用 fast，实际 `Item` 字节码选32；Neo 实际 Builder 默认 `eatSeconds=1.6`，`Item` 通过 `eatDurationTicks` 乘20，同样是32。两端实际类字节码与 Neo 映射源码相互核对，没有只凭现代版本印象推断。

此项只确认 Radaway，不推广到所有食物、饮料和药丸。装罐机64 tick、装盒机16 tick是制作耗时，不是食用时间；对应制作行存在也不能证明本项已经对齐。没有计时游戏操作或修改食品属性。

## 424：高剂量辐射多一次明确伤害请求

限定未安装 IC2、`PotionsGT.ID_RADIATION<0` 的原版回退分支：剂量达到100后，原追踪器施加凋零、反胃、饥饿、缓慢、挖掘疲劳和虚弱；这段没有额外直接伤害调用。当前保留这组药效，又每50游戏 tick 调用 `hurt(radiation,1.0F)`。这是明确的额外请求，不等于保证最终多扣1HP；无敌时间、伤害事件、护甲与药效自身伤害没有执行验收。

现有注释表明这是移植时有意加入的独立伤害入口，本批登记为策略差异并保留。另将同处“2 HP per2.5 s”的错误单位说明改成“请求1HP／2.5秒，实际由伤害链处理”，没有更改1.0F、周期或死亡逻辑。安装 IC2 时的完整药效兼容继续待核。

## 已有机制与整理范围

0–127剂量、长期存储和辐射不自然衰减已有对应。原药丸尝试移除的是可选辐射药效，不据此声称原版会清除回退的中毒／凋零；药效消除与剂量扣减分别核对。210的创造玩家／铁傀儡豁免缺口不重复登记；原生死亡、克隆及独立存档重启没有因此完成实测。

`GTFoodStatsGen` 两版重复的表遍历集中为一个 `build`，仅物品查询保留两个 `resolveItem` 适配。保持静态初始化顺序、原行顺序、空／空气判断、缺失ID收集及完整统计数据。循环局部 `r` 改为 `entry`。`NutritionRules` 的列表局部名改为 `result` 并展开分支；`PlayerRadiation`、`ReactorRadiation`、`RadawayItem` 展开压行与事件声明，public接口、持久键、原生类型、数值和行为保持。

五源170→256行，九份完整平台／core视图在上述抽取与局部名归一后有序代码一致；注释只作列明的单位修正。旧提取工具仅阅读，未运行其旧视图写入器，也没有改生成数据表或用户材料。

core与Forge1.20.1／NeoForge1.21.1 `compileJava` 实际通过，1分23秒，使用相同版本的本地API临时适配；生产构建配置未改，不声称默认离线解析／CI已修复。未新增镜像测试、执行Minecraft方法、GameTest、启动游戏、打包或推送。独立Neo源码包候选出现后续版本API文本，未将路径名当作版本证明，改核21.1.243实际通用包字节码与清单；Neo食用时间也额外核实际映射类。

累计424条含已修与保留策略，不是待修数量或完成率。食品完整药效与消耗、第三方兼容、保存恢复、其它独立工具及全系统对齐／源码标准化继续，全面goal保持active。
