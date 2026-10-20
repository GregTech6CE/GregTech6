# 库存方块的原生外部爆炸分派核查

2026-10-21，基线 `bbdcb7f73f6f210c59f32864a9a1ceabb4f3f570`。只读当前 Minecraft／加载器依赖的源码、字节码及原版 GT 实现；没有修改生产或中央文档，没有运行 Gradle、编译、新测试、游戏或保存重启。第四十九批，正式记录 ALIGN-162–163；本轮只归档差异，未改变玩法。

## 范围

讨论的是一个外部原生爆炸已决定破坏该位置时，箱子、书架和瓶框的库存／方块物品如何处理。**不涉及机器自身过载、锅炉危险触发、修改爆炸开关或恢复任何已修复的自爆**。爆炸事件取消、保护拒绝移除、后续连锁爆炸再伤害已掉落实体、最终实体存活率均不在本次运行证据内。

## 本地原生依赖证据

- Forge：实际缓存 `forge-1.20.1-47.4.20_mapped_official_1.20.1.jar`（包含项目编译用的已补丁 MC 1.20.1）及同版本 `forge-1.20.1-47.4.20-sources.jar`。`forge/artifact.json` 与 `forge/forge-source-artifact.json` 记录 jar／class／源码 entry 哈希。
- Neo：项目自身 `multiversion/versions/1.21.1-neoforge/build/moddev/artifacts/neoforge-21.1.243-sources.jar` 和实际二进制 jar（MC 1.21.1）；`neo/source-artifact.json`、`neo/binary-artifact.json` 及 `neo/sources.json` 记录。原生入口与类字节哈希已合并到[本批回执](verification/inventory-bumble-audit-20261021.json)。
- 原生 1.7.10：本机已有官方客户端 `minecraft-1.7.10-client.jar` 和 Forge `1.7.10-10.13.4.1614-1.7.10-universal.jar`。后者的 deobfuscation_data 映射把 `agw.a(Z)V` 定位为 Explosion.func_77279_a；原始 class、mapping entry 和两个 jar 的哈希在 `original-native/artifact.json`。这是未启动的原客户端字节码，不把 GTNH 运行时其它模组行为当原版证据。

使用本地 JDK javap 与 Vineflower 1.10.1 只读反汇编／反编译隔离的 class。Forge 反编译对未提取的内部类及两个无关 enum switch 发出告警，未据这些方法重建启动逻辑；所采用 getDrops／onBlockExploded 顺序另有 javap 直接调用指令见证，不能把反编译文本视为原始源码。

## 原版 GT 不是一个孤立的 onExploded 方法

原 `gregapi/block/multitileentity/MultiTileEntityBlock.java:249` 是真正的 Forge 爆炸入口：取实体，若实现 IMTE_OnBlockExploded 则调用其 `onExploded`，否则直接删方块。

`TileEntityBase05Inventories.java:146–149` 实现该接口：对当时每个槽独立调用 nextInt(3)，结果非零则 `GarbageGT.trash(mInventory,i)`，再 `setToAir()`。`CS.GarbageGT:1400–1405` 确实将该库存槽设为 null，并非只是标记下次待处理。

Root.setToAir:861 → 原生替换触发 `MultiTileEntityBlock.breakBlock:145–153` → 实体 breakBlock。Base05.breakBlock:153–166 按 canDrop 分离剩余内容：金属箱／书架 canDrop=true、默认 keepSlot=false；瓶框 canDrop=false、keepSlot=true。

原书架还覆盖 breakBlock:99–102，先 generateDungeonLoot 再交给父类。因此“原有槽内容先受概率处理”不能扩大为所有在拆除期间新生成的 loot 也必然先受概率处理。普通已有书、没有待生成 loot 的场景最清楚。

## 原生 1.7.10 的先掉落、后回调顺序

本机原客户端的 Explosion `agw.a(boolean):148–154` 在可掉落时先调用 Block.func_149690_a(...,1/size,...)，随后替换为空气并调用旧破坏回调。对应映射也已从本机 Forge jar 解开保存。

另直接核对官方 Forge 1.7.10 [Explosion.java.patch](https://raw.githubusercontent.com/MinecraftForge/MinecraftForge/1.7.10/patches/minecraft/net/minecraft/world/Explosion.java.patch)（2026-10-21）：补丁保留前面的掉落调用，将后面的原生替换／回调改成 Block.onBlockExploded。它没有把 GT 随机清槽移到 getDrops 之前。这里是“本地原始 class＋官方 Forge 补丁”的交叉证据，不冒称已将本机旧 universal 的二进制补丁全部应用并运行。

GT `MultiTileEntityBlock.dropBlockAsItemWithChance:260` 会先调用实体 getDrops，取得物品列表，交给 Forge harvesting 事件后按 aChance 生成方块物品；Base04.getDrops:165–169 调用 writeItemNBT；Base05.writeItemNBT:70–76 将 keepSlot=true 的库存写入该物品 NBT。

**所以原瓶框先把完整库存写进候选方块物品，再进入 onExploded 随机清槽。** 后面的清槽不会把已经生成的独立物品 NBT 改成只剩 1/3 槽。不能仅看父类随机语句就给瓶框补同样的逐槽损失。

## 当前 Forge 普通爆炸完整链

实际依赖反编译 `Explosion.finalizeExplosion:280–326`：

1. 对最终被破坏的 toBlow 位置检查 canDropFromExplosion，构建带 BlockEntity 的 LootParams；DESTROY_WITH_DECAY 才加 EXPLOSION_RADIUS。
2. 先 spawnAfterBreak，再 `blockstate.getDrops(builder)` 收集物品。
3. 然后 `blockstate.onBlockExploded(level,pos,explosion)`。
4. `IForgeBlockState:620–623` 委派方块接口；`IForgeBlock:716–719` 默认 setBlock(AIR,3)，再 Block.wasExploded（原生默认空方法）。
5. Level.setBlock:236 → LevelChunk.setBlockState:273 在服务器调用旧状态的 onRemove → 对应 GT 方块方法 → 父类移除实体。

javap 的 Explosion.finalizeExplosion offset 415 调 state.getDrops，440 才调 state.onBlockExploded；BlockStateBase.getDrops offset 9 调实例 Block.getDrops(state,builder)。IForgeBlock 的默认回调同样有 setBlock 在 wasExploded 之前的字节码。

## 当前 Neo 普通爆炸完整链

MC 1.21.1 将目标处理分到 `BlockBehaviour.onExplosionHit`，未改变本任务相关顺序：Explosion.finalizeExplosion:347–358 → state.onExplosionHit → BlockBehaviour:178–197 先 state.getDrops、再 state.onBlockExploded → IBlockExtension:739–741 setBlock(AIR,3)／wasExploded → LevelChunk.setBlockState:272–274 → GT onRemove。

对应字节码中 onExplosionHit 的 offset 168 是 getDrops、189 才是 onBlockExploded。Neo TRIGGER_BLOCK／不破坏方块等模式需另分支；这里不声称那些模式也会删库存。

## ALIGN-162：散落库存遗漏逐槽随机清除

| 宿主 | 已确认的当前链 | 与原版对比 |
| --- | --- | --- |
| 金属／强化木／专用战利品箱 | 自定义 getDrops 只返回方块与 loot 状态；AIR 替换的 onRemove 遍历完整库存，由 GTItemDrops 创建物品实体并清空。helper 没有逐槽概率 | **确认遗漏原 Base05 对既有库存的逐槽 2/3 trash**。只说实际被破坏的普通回调内容处理，不说最终所有物品绝对存活 |
| 书架 | 普通方块 loot 路径处理架体；onRemove 的 contents() 生成必要的待生成 loot、遍历全部内容 popResource，再清空 handler | **已有且非待生成的书槽，同样遗漏原概率**。待生成 loot 的前后顺序另记，不能给原版所有新生成书都硬套 1/3 |
| 瓶框 | getDrops 先 packedStack 装完整 gt_bottles；之后 onRemove 才 detachInventory 保存快照并清空 | **不能与前两者合报为每槽概率缺口**。原版同样先打包后清槽，真正应对照的是整包方块物品的爆炸掉落门槛 |

## ALIGN-163：自定义方块掉落未应用爆炸衰减门槛

当前金属箱和瓶框自定义 getDrops 直接返回物品列表，不读取 EXPLOSION_RADIUS，也未调用 loot 的 survives_explosion／explosion_decay。原 GT dropBlockAsItemWithChance 则会按原生传入的 1/size（再允许事件调整）决定整件方块候选是否掉落。因此在破坏并启用衰减、无外部事件调整的普通条件下，当前方块物品缺少对应的衰减门槛；尤其瓶框完整库存包不能通过给槽随机清空来代替这个条件。它与前两者散落库存概率是独立合同。未运行真实掉落率统计。

Loot explosion_decay 是 loot 表被实际引用时按堆内单个物品处理，不是爆炸引擎对所有 getDrops 返回物品和 onRemove 生成实体的强制后处理，不能用存在该函数推断两份合同都已经补齐。

## CoverDropMixin 不在这条直接入口上

两版本普通爆炸都直接 `state.getDrops` → 实例 `block.getDrops`。CoverDropMixin 拦的是两个静态 Block.getDrops(state,server,pos,be,...) 助手，普通爆炸不经过它们；CoverHarvestMixin 又只挂玩家挖掘。

但 ComponentCoverRemovalMixin 在 LevelChunk.setBlockState HEAD 仍会处理 fallback 覆盖板，金属箱自己的 onRemove 也会掉覆盖板。不能把静态助手未经过扩大为所有移除钩子失效，或推定所有板都被装进同一库存包。这里没有完整运行覆盖板归属／数量。

## 明确未确认的运行边界

没有双版本游戏爆炸、完整随机抽样、gamerule 组合、模组事件取消或拒绝替换、各种自定义爆炸类、Mixin 实际加载、旧存档完整重启；不把 source/class 见证当这些验收。原始 Forge 1614 二进制补丁未在本任务重放，官方补丁提供顺序交叉证据；最终 old-world 运行仍需独立场景。

源码／依赖 jar 和 entry 哈希、原生投影及字节码观察摘要见[本批回执](verification/inventory-bumble-audit-20261021.json)。反编译文件留在忽略的 work/，没有作为项目源码或归档提交。没有游戏爆炸、构建或推送；全面审计继续。
