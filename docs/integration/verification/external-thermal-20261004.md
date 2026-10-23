# 2026-10-04 / 外部研钵、熔炉与坩埚展示

整体移植仍未完成。代码提交 `58f8cd5d`，外部 ingredient 匹配保留收尾 `ba7f8c9d`。本批对照原版处理入口，接回两个外部形态及热加工回调，并修正既有熔炉数量和原生桥接；不增加 GT 自有物品或资产。按减少测试要求集中编译、一次共享检查和双版打包，没有启动游戏。

## 来源与外部形态

原版 `OP:152` 的 dirtyGravel 重量1U、条件 ORES；`:185` 的 crystal 重量1U、条件 GEMS，两者不能都按矿石形态定义。新增类型化元数据仍排除在 GT 自有物品定义流之外。实际输入沿已有明确形态/材料标签获取，本批不猜测其他模组的全部标签拼写。

`Loader_Recipes_Handlers:85/88` 的两条 Mortar 行均16 EU/t、MORTAR、排除反物质，采用全量粉碎余料；无微量粉末，也没有 Anvil 空位要求。等级0时间16 ticks，等级2为48。共享研磨表现在有22条自适应路线，外部元数据形态11种；六条外部 Mortar 声明已全部接入，诊断不再错误声称这两个前缀缺失。

## 熔炉来源行与数量

原版 `Loader_Recipes_Furnace:151–160` 的九个外部监听器接入加载后的实际物品，原始数量及经验规则为：

| 形态 | 作为输入冶炼的材料量 | 经验 |
|---|---|---|
| rawOreChunk | 前缀重量3/8U | ceil(产物量/U × (输入质量+1)) |
| chunk / rubble | 固定2U | 同上 |
| pebbles / cluster | 固定3U | 同上 |
| cleanGravel / dirtyGravel / crystalline / reduced | 前缀重量1U | 同上 |

原版没有 clump/crystal 熔炉监听器，不生成额外行。FURNACE 和非 UNUSED_MATERIAL 条件由导入的来源标记执行，非法/零冶炼目标及不存在的产物不注册。普通铁仍需要坩埚，不因这个适配器获得熔炉配方。

原有两平台 FurnaceSmeltingRecipes 把 `mTargetSmelting.mAmount` 又当作目标的固化数量使用，导致冶炼比例被平方。源 `MT` 只有 H2O→Ice、Lava→Obsidian 改写固化目标，它们都不在来源 FURNACE 集合；本批这些监听器的固化比例为恒等U，应只算 `inputFormAmount × smeltingPerUnit / U`。例如原版 Cassiterite:3701 为3/4U Tin，Malachite:3755 为1/6U Copper，现保留正确比例。共享精确整数中间量及零/long上界保护沿上一批数量规则；不声称复现原版数值溢出结果。跨模组非恒等固化目标尚未模型化。

## 原生熔炉桥接与重载

两平台 Loader_OvenRecipes 旧 `firstStack` 把结果数量设为1，现在保留结果组数，只把单输入机器行镜像为原生料理配方，不把需要多输入的机器行误当作单物品配方。FurnaceSmeltingRecipes 的静态来源行和新的外部临时行保存经验/FOOD 元数据，原生熔炉不再统一输出0经验；按原版 Listener/RM:805–821 的分支为 FOOD 材料提供烟熏炉，其余来源材料提供高炉。现代对应料理时间使用熔炉200 ticks、烟熏炉/高炉100 ticks；原版 smoker/blast 是 Et Futurum 可选接口，现代版本已有对应原生设备。

来源材料的同输入配方按原版 RM.add_smelting 覆盖规则取得优先级，先不让原版镜像取代这些来源机器行，再在原生料理表中只剔除这些输入。混合 ingredient 用两平台自带 DifferenceIngredient 包裹原 ingredient，匹配为 `base.test(stack) && !subtracted.test(stack)`；保留原有 NBT/组件约束与序列化，不扁平化为普通候选 ingredient。仅减去该行被来源覆盖的候选，避免在每条网络配方中携带整张来源输入表；保留其他候选、原ID/分组/产物/经验/时间。此API及Codec/网络行为核对本地固定依赖 Forge47.2.20/NeoForge21.1.243源码，但未进行原生匹配或网络执行。来源食品的非指定料理分支也不保留旧候选；其他非本监听器机器行维持原有数据包优先策略。其他 RM.add_smelting 调用者的全部 smoker/blast/经验参数尚未统一模型化，本批不宣称整个料理系统完成。

ExternalOreProcessing 清理前先撤销 Oven 自身镜像并恢复其暂存的来源行，然后移除本适配器拥有的行，避免后续镜像把旧标签输入复活。组成和料理元数据随标签轮次更新，不永久写入静态材料注册或存档。Forge 的运行时配方生命周期改为比较实际 Recipe 身份集合，使同一 RecipeManager 被重载更新后仍能重新生成；NeoForge 已有对应 holder 身份判断。未变化的登录同步仍跳过生成。

## 坩埚展示

原版 RecipeMapCrucible:68–76 列出 chunk/rubble/pebbles/cluster/cleanGravel/dirtyGravel/crystalline/reduced 八个外部输入；本批按各前缀实际材料量、冶炼目标和熔点生成 fake 展示行，支持 JEI/EMI 的热加工信息。没有额外列出源文件未列出的 rawOreChunk/clump/crystal，也不把展示行改为可直接消耗电能的机器加工。实际坩埚仍通过已有临时组成和材料转换执行。

## 已取得的证据

快速双版编译51s终态通过，日志 `work/external-thermal-compile-20261004.log`。一次共享行为2,291、机器34、材料7,659、温度53，共10,037条断言通过；新增47项来源时间/重量/熔炼比例/经验/列表和自有物品边界，七项完整材料样例确认晶体条件、FURNACE 允许/拒绝及原始矿物比例。已有 Forge 熔炉 GameTest 中照抄旧平方式的夹具已同步正确恒等固化量，只参加编译，没有执行。

完整定义及post-init每阶段保留全部139,681条旧观察，只增加两条前缀和2,320条材料形态记录；材料对象、别名、旧属性/目标/形态无变化。前缀总数120，指纹更新依据见 [全图差异](external-thermal-material-differential-20261004.json)。

最终正常classpath的 `:core:check :assemble :neoforge:assemble -x :core:processResources` 5m31s终态通过，包含优先级/混合候选收尾改动的双平台编译，日志 `work/external-thermal-build-20261004.log`。共享资源本批未修改，命令使用现有资源输出；没有重复共享检查或原生游戏运行。

随后 `ba7f8c9d` 保留外部 ingredient 原始匹配约束，仅增量 `:assemble :neoforge:assemble -x :core:processResources` 50s终态通过，日志 `work/external-thermal-ingredients-build-20261004.log`，不包含 :core:check。下述成品/内容回执已用此次最终JAR重做，source_commit为收尾代码树，source_commits记录两次代码变更；DifferenceIngredient 的固定依赖源文件散列也已记录。

两包648个共享类与当前编译输出一致，metadata、NOTICE、重复条目及独立测试夹具排除通过，见 [成品回执](external-thermal-artifacts-20261004.json)。Forge十个变更原生类在重混淆成品内存在（含一个已有 GameTest 夹具类，只编译）；NeoForge八个与当前编译字节相同。十一份原版源文件及日志散列见 [内容回执](external-thermal-content-20261004.json)。source_commit表示本批构建代码树，build_commit为null，不代表JAR含该字段。成品位于 `C:\Dev\GregTech6\build\libs\gregtech6-1.20.1-forge-0.0.0.jar` 和 `C:\Dev\GregTech6\neoforge\build\libs\gregtech6-neoforge-1.21.1-0.0.0.jar`。

没有真实外部模组、原生熔炉/烟熏炉/高炉/经验执行、混合原料匹配、研钵、标签重载、坩埚显示/实际熔炼、JEI/EMI 转移、客户端或存档的新运行证据；编译与共享样例不能替代这些验证。dustPure/dustRefined、完整材料回调、所有料理调用者与更广原版处理链继续留在缺口账本中。
