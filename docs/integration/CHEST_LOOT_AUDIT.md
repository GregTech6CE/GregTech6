# 箱子与战利品生命周期核查

第四十八批，2026-10-21。行为核查开始于 `03be2061d`，排版比较基线为 `cce17c8e3`；其间没有改变本域玩法。原版根为 `C:/Dev/gregtech6-master/gregtech6-master/src/main/java`。区分金属／强化木 GUI 箱、专用 loot_chest 目录及一次性 Loot Crate，不将三种生命周期合为一类。

## ALIGN-152：箱子缺少钳子批量转移

原 `MultiTileEntityChest:171–230` 先合并玩家已有堆，再按无 NBT 可堆叠、带 NBT 可堆叠、不可堆叠有 NBT、最后其它物品依次回退，非合并阶段使用玩家 9–35 槽；成功按实际搬运量返回成本并同步／发声。

当前 `MetalChestBlock.toolInteraction` 只提供水平扳手，物品和通用工具实际入口也没有箱子钳子分派。钳子物品与提示均存在，不能当作功能已实现；该优先级与大容量存储不同，不能直接套同一种计数算法。

## ALIGN-153：原生库存比较器缺失

原 Chest:256 使用库存计算比较器值。当前 MetalChestBlock 没有 hasAnalogOutputSignal／getAnalogOutputSignal；CoverSignalMixin 只处理覆盖板的信号源和强弱输出，不补原生比较器。不能以可贴红石板代替箱子本身的库存读数。

## ALIGN-154：部分箱子物品栈上限不同

原材料金属箱、强化木箱和专用石质 loot 箱的物品栈上限均为 16。当前强化木仍显式 stacksTo(16)，两版金属箱和专用 loot 箱却使用默认 Item.Properties，物品类未覆写上限，因而为 64。这是方块物品堆叠，不能和 54 槽中每槽的物品上限混淆。

## ALIGN-155：陷阱开盖信号未接入

原 Chest:99,111,252–253,305–306 读取／保存 trapped，开关 GUI 更新邻居，打开时强弱信号均为 15。原 ChestGenHooksChestReplacer:122 和暮色替换实际写入 trapped=true，因此不是未使用字段。

当前 LootChestBlock 虽有 isLootChest 和陷阱说明，但没有实际调用或信号覆写；实体 openers 仅驱动盖子事件。没有输出覆盖板时通用 Mixin 不会补该 15 信号。不将它扩大为所有 GT 红石都失效。

## ALIGN-156：箱子喷漆未接专用渲染和掉落保存

原 Chest:296–303 支持喷漆／混色／去色，专用模型读取 mRGBa，父类保存世界／物品颜色。当前 PaintTargets 和两版 BlockEntityPaintMixin 已有状态，不能报喷漆系统完全不存在。

但 MetalChestVisuals 永远取 block.material().getColor，世界和物品渲染都不传 Paintable／物品数据；普通 tint 查询的颜色 Mixin 不会改写此专用模型的 RGB。掉落又只取 lootItemData，CoverDrops 仅合并覆盖板键，不补 paint。因此基础材料色仍在，喷漆覆盖色及拆下保留缺失。这里确认渲染参数和数据流，未验客户端像素。

## ALIGN-157：专用战利品箱原型参数不同

原 Loader_MultiTileEntities:151 的 Mossy Stone Chest 使用 MT.Stone、硬度 5、抗爆 14。当前 GTLootChests.registerAll 的各表专用箱全部用 Steel、硬度 4、抗爆 6；栈上限差异另见 ALIGN-154。只针对 loot_chest_* 目录，不表示地牢中原本可选的金属／强化木箱也应全部变成石箱。

## ALIGN-158：普通箱子掉落携带默认无用数据

原 Chest.writeItemNBT:117–120 只在有效待生成表名时增加 loot 字段；无喷漆、名称或 loot 状态的普通箱子无需这些数据。当前 lootItemData 始终写 GTLootGenerated=false 和 GTDungeonLootSeed=0，getDrops 再无条件写入 BlockEntityTag／BLOCK_ENTITY_DATA。

因此源码上拆下普通箱子与无数据的新合成物品不等价，会影响堆叠；实际游戏堆叠尚未运行。应保留已消费 loot 箱防重掷的 true 标记及有效 pending 表，不能为了去无用 NBT 把这些真实状态一起删掉。

## ALIGN-159：表抽取次数错误包含上界

原八种 GT 表设 8／24，但调用的 Forge 1.7.10 getCount 对不同上下界不包含最大值，实际为 8–23 次。当前 LootRollRules.count 包含上界，可能取到 24。单个物品堆数量本来就包含上界，不能一并改为开区间。该区别已复核 [Forge 官方实现](https://raw.githubusercontent.com/MinecraftForge/MinecraftForge/1.7.10/src/main/java/net/minecraftforge/common/ChestGenHooks.java)，不是仅根据原注释推断。

## ALIGN-160：一次性板条箱的抽样层次不同

原 LootCrate → ST.generateOneVanillaLoot 直接抽一个加权条目，生成并取首堆；当前先执行完整现代表，再从非空输出堆中均匀选一个。多池／多次输出情况下不保持同一分布。原语义同见上述 [官方 getOneItem 实现](https://raw.githubusercontent.com/MinecraftForge/MinecraftForge/1.7.10/src/main/java/net/minecraftforge/common/ChestGenHooks.java)。

只掉一个堆不足以证明抽样相同；现代战利品表内容变化本身不判为移植错误。需要明确保留原加权合同或采用现代多池适配，尚未运行默认表随机统计。

## ALIGN-161：成功空表不发首次生成经验

原 ST.generateLoot:1027–1065 只要不抛异常即返回成功，Chest:262–269 在成功时发五颗经验球并清除表名，即使有效表生成空结果。当前生成后会标记消费，但只有 placed>0 才发经验。

缺失外部表提前返回是另一分支，不能称它也已被消费。普通非空首次生成的五颗经验球已有对应，双方每颗数值公式均为 5..13；旧说明 5..14 已纠正，未改实际数值。

## 既有记录的范围修订

- **ALIGN-139**：原 Chest 直接继承 Base05，没有 Base06Covers／Coverable，当前箱子本体却允许覆盖板。与抽屉、保险箱、书架的禁贴面遗漏共同记录宿主范围差异。是否保留现代扩展需要明确，不能把它当原版原有能力。
- **ALIGN-102**：金属箱上检索器可装但不运行是当前事实，不过原箱子本不接覆盖板；正确的原版对齐不能简单变成给箱子补检索 tick。物品管道／基础机器的其它检索器差异仍有效。
- **ALIGN-142**：原普通箱点击先有 Base04 的点击面邻块阻挡，再判断顶面可开盖；当前只有上方导体判断。扩展到该已追踪宿主，不推断所有方块都遗漏。

## 已有对应与未验证

两版都有 60 金属和 60 强化木家族、54 槽、菜单、实体绑定、材料配方实际装载入口、全侧物品能力、世界保存／恢复、水平放置与扳手转向、盖子和开关计数／声音。配方声明存在不代表所有材料 ingredient 已运行成功。

普通箱原版 keepSlot=false、内容散落；当前拆除逐完整栈掉出后清空，pending loot 和消费标记随方块物品保存，不会因普通拆除主动生成经验。一次性 crate 则需撬棍，掉最多一个战利品堆和空 crate，不应另加 GUI 箱的首次开箱经验。其十种表、注册、配方与工具成本已有对应。gt.matdicts 也已实际构建，旧“不提供”说明已纠正。

原版库存变化通知、随机插槽覆盖、原生爆炸随机清除、拒绝移除时的 crate、旁观／多人开箱、完整旧档重启及现代可选模组关系继续核对。现代管道有重试，不能仅缺原回调就断言永久卡死；没有把未复现边界写成已游戏确认。

## 可读性与证据

展开箱子方块／专用模型、实体覆盖板／能力流程、BookShelfRules 和当前陈列入口；展开 LootRollRules 并澄清其现状，纠正材料词典和抽样说明。八份源码十四份 core／Forge／Neo 投影的真实词法流及字面量位置与 `cce17c8e3` 保持一致，维护 Neo 体先明确解开；两组专项回执另核原生注释、指令和嵌套转义。

[来源回执](verification/chest-loot-audit-20261021.json)保存 11 份原版、46 份当前文件及官方依赖审阅位置。没有为未保存的远程文件编造哈希。没有本批 Gradle、编译、新测试、游戏、旧档重启、构建或推送；全面核查继续。
