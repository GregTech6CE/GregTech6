# 2026-10-04 / 原版粉末监听器与炼药锅清洗

本批继续移植，没有结束整体任务。确认此前两个新 issues 已完成，接着要求检查其他缺口；本批没有向 GitHub 发评论、关闭或重新开启 issues。

## 来源与实现

- `OP.java:158-160`：dustImpure/dustPure/dustRefined 重量分别为 10/9U、11/9U、12/9U，即 720,720,000 / 792,792,000 / 864,864,000。修正已存在的不纯粉末重量；后两种仅增加兼容元数据。来源 `Loader_Items` 没有创建后两种 GT 自有物品，`MaterialItemDefinitions.candidatePrefixes()` 保持不变。
- 新前缀保留来源 `DIRTY_DUSTS` 条件和 refined 的 Obsidian/Glowstone 禁生成规则。源 `MT`/`TD` 定义表没有给现有材料设置 DIRTY_DUSTS；不把 ORES 当作这个标记。真实已注册的外部输入仍可触发处理监听器，不能把禁止自有生成误当成禁止外部加工。
- `Loader_Recipes_Handlers:114-116,126-128`：三个 Shredder 分支输出 1 dust + 1/2/3 dustTiny，按来源 targetPulver 材料输出；排除 ANTIMATTER 和 MT.Bedrock，保留 MORTAR 的 16/256 倍率。指定产物数量不再按 target amount 缩放，不新增 Mortar/Anvil 行。微量数量从布尔值扩展为实际数量，原有 22 行不变。
- `Loader_Recipes_Furnace:141-143`：实际标签中的三种粉末输入按自己的前缀重量熔炼，不给经验。已有自有不纯粉末行仍由原加载器保留，适配器只拥有新注册的实际 Recipe 身份。
- `OP:570-573` / `OreDictListenerItem_Washing:64-89`：crushed → crushedPurified，50% 机会给一份 crushedPurifiedTiny；三种脏粉末 → 同材料的 1 dust，不返还杂质。副产从原材料的有序列表随机选择，列表为空时用本材料，不去重。
- `GT_API_Proxy:599-646`：双平台服务器世界 tick 结束、LOWEST 优先级执行掉落物监听，覆盖实际标签中的外部物品。处理前保存本轮 ItemEntity 快照，防止新产物参与同轮遍历；临时组成新增 foreign crushed/dustImpure，连同十三种兼容形态共十五种。
- 清洗检查坐标 `floor(x), floor(y-0.25), floor(z)`；每 tick 消耗一个源物品和一层水，主产物不存在时不消耗。仅原版 WATER_CAULDRON 执行，使用平台 lowerFillLevel 处理最后一层水及更新；不接受熔岩/细雪炼药锅。产物落在原物品位置，余料归零速度、移到锅内中心高度0.9，拾取延迟40；最后一个源物品移除。
- 来源清洗提示 `gt.behaviour.washing` 在统一 tooltip 事件入口增加一次，保留原有材料属性防重逻辑。英文使用源文案，增加中文翻译。

粉末处理代码提交 `9e67a168`；来源文件、编译日志及成品对应字节哈希见 [内容回执](dust-listeners-content-20261004.json)。

## 既有 #11 的重载收尾

公开 API 本次读到最新仍为 #11/#12，两项均已关闭；确认已完成。复核发现 MaterialDisplayBinding 的静态材料候选索引没有随标签组成变化失效。双平台标签重绑/服务器停止及每次 EMI register 清空该索引，随后按本次数据重新建立模板；保留先前 #11 的全部配方与候选缓存。提交 `aef65a78`。

#12 的源材料属性、粉末块/16U/64U箱点燃和连锁规则此前已在 `a67d2b41`；管状炸药仍保留源3×3×3和10/40抗爆阈值。本批只复核源码，没有声称复现“只炸泥土”或取得新的爆炸运行证据。

## 验证与成品

- 一次双版快速编译 27s 成功，日志 `work/dust-listeners-compile-20261004.log`。
- 首次共享检查 3s 在新增样例使用超范围临时材料编号时失败；改为独立的无编号样例后，最终集中检查/正常 classpath 双版 assemble 50s 成功。两个日志分别为 `work/dust-listeners-build-20261004.log`、`work/dust-listeners-build-final-20261004.log`。
- 共享行为 2,355（27组）、材料 7,666、机器34、温度53，共 **10,108 条断言**。增加64项重量、时间取整、微量数量、Bedrock、经验、清洗消耗、负坐标和副产选择固定来源样例，以及七项完整材料样例。没有运行客户端或 GameTest。
- 定义和 post-init 全图逐行审计：每阶段只有原 dustImpure 的一条 P 重量记录改变，新增两个 P 和2,320个 F，其余142,002条旧记录保持。前缀122，材料1160，名称1523；凭 [完整差异](dust-listeners-material-differential-20261004.json) 更新指纹。
- 两份变更语言源文件单独同步到既有 core 资源输出，正式构建使用 `-x :core:processResources`，避免重复复制完整静态目录；实际两包语言逐字节等于当前源文件，并解析为有效 JSON。没有新增自有物品资源。
- [双版验包](dust-listeners-artifacts-20261004.json) 通过：652个当前共享类、metadata/NOTICE、重复条目和测试夹具排除。Forge九个变更原生类仅声明重混淆后存在；NeoForge十一个变更原生类逐字节等于当前编译输出，两版两份语言资源也一致。

成品：`C:\Dev\GregTech6\build\libs\gregtech6-1.20.1-forge-0.0.0.jar`、`C:\Dev\GregTech6\neoforge\build\libs\gregtech6-neoforge-1.21.1-0.0.0.jar`。

## 仍待接续

真实外部模组标签、清洗 ItemEntity/水位/副产/拾取、重载及 EMI/Reliable EMI 最终耗时没有新运行证据。自定义水炼药锅能力未扩展；全量材料监听器、其他来源处理链和料理调用者、客户端/存档仍未完成。其余机器、面板/覆盖板、蜜蜂、占位电池、tooltip 与收尾配置缺口保留在接续账本。
