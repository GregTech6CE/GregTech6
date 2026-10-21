# 养蜂持久化与区块保存时机核查

第五十批，2026-10-21，基线 `2d14d4009`；新增 ALIGN-171。只读核对生产代码、原模组及实际本机 Minecraft／Forge／NeoForge 依赖，没有改变保存逻辑，没有 Gradle、编译、新测试、游戏、重启、构建或推送。

原模组根：`C:/Dev/gregtech6-master/gregtech6-master/src/main/java`，原作者 Gregorius Techneticies / GregTech-6 Team；源码保留 LGPL-3.0-or-later 头。当前根：`C:/Dev/GregTech6`。本批另展开 `BumbleBeeGenes`／`GTBumbleBlocks` 排版，执行代码保持；不借它们的移动行号推导持久化结论。

## ALIGN-171：纯寿命／配对倒计时变化可能保存回退

确认当前普通/高级养蜂箱存在**条件性的寿命与配对倒计时保存回退风险**：它们有字段序列化代码，但纯 tick 改变字段后没有自行标脏，也没有查到原版等效的全局每秒标脏补偿。两版原生保存最终会跳过干净区块，连正常卸载与退出保存也没有无条件重写区块。

这不是“所有养蜂进度必丢”或“后代一定丢失”。库存变化、蜂脾产出、世代切换、开箱以及同区块其他方块/BE 改动均可能重新标脏，从而把当时全部养蜂 NBT 一起保存。这里的回退是代码路径推导，尚未用真实世界退出/重载实测。

## 原版为什么不需要在养蜂 tick 内逐次 markDirty

原 `gregtech/tileentity/tools/MultiTileEntityBumbliary.java` 与 `MultiTileEntityBumbliaryAdvanced.java`：

- `:66–85` 保存/读取 `mLife`、`mBreedingCountDown` 和 `mOffSpring`；`mSky`、`mEndedQueen`、环境温湿度不保存。
- `:105–112,118–121,193–199` 等处的纯寿命/倒计时变化没有直接标脏，不能据此认定原版也靠偶然保存。
- 继承链 `TileEntityBase07Paintable → 06Covers → 05Inventories → 04MultiTileEntities → 03TicksAndSync → 02AdjacentTEBuffer → 01Root` 中，`Base02AdjacentTEBuffer.java:35` 明确实现标记接口 `ITileEntityNeedsSaving`，因此两种养蜂箱均符合全局保存调度条件。
- `Base03TicksAndSync.java:115–118` 首 tick 调 `markDirty()`、`WD.mark(this)`；但 `Base01Root.java:157` 刻意使 `markDirty` 不通知区块，`Base05Inventories.java:108` 的覆写只是调用父类并置 `mInventoryChanged`。只引用首 tick 的 markDirty 会漏掉决定性的实际保存路径。
- **实际补偿是 `gregapi/GT_API_Proxy.java:600–604,682–685`**：服务器世界 tick 的 END 阶段，每逢 `SERVER_TIME % 20 == 1` 遍历已加载 TileEntity，对 `ITileEntityNeedsSaving` 调 `WD.mark`。该 proxy 构造器 `:154–155` 已注册 Forge 与 FML 事件总线，属于活跃代码。
- `gregapi/util/WD.java:314–330` 把接口实例所在区块显式 `setChunkModified()`。这样原养蜂箱即便无库存变化，至少每 20 tick 会再次把宿主区块标脏；这不是承诺逐 tick 或崩溃零损失。
- `Base04MultiTileEntities.java:113–122` 在真正区块序列化时调用 `writeToNBT2`，才到养蜂箱的寿命、冷却、brood 写入。字段写入和触发保存是不同环节。

另读取本机原始 1.7.10 client 的 `apx`（Chunk）与 `ms`（ChunkProviderServer）字节码，经本机 Forge `deobfuscation_data-1.7.10` SRG 核对方法身份：`apx.a(Z)Z = needsSaving/func_76601_a`，`apx.e()V = setChunkModified/func_76630_e`，`ms.a(Z,qk)Z = saveChunks/func_73151_a`。`needsSaving` 检查 `isModified` 或满足时间条件的 `hasEntities`，`saveChunks` 写后会清 `isModified`。这提供旧原生保存上下文，但**未重放旧 Forge 二进制补丁**；原 GT 每秒显式标脏的源码证据独立存在，不依赖把 vanilla client 当成完整 Forge 运行时。

## 当前活跃实现的状态变化

两平台共用 `multiversion/src/main/java/com/gregtech/gregtech/blockentity/tool/BumbliaryBlockEntity.java`，`:126` 直接继承 Minecraft `BlockEntity`，没有原 `ITileEntityNeedsSaving` 标记或 GT Base 保存调度；方块 `block/tool/BumbliaryBlock.java:83–87` 直接调用 `serverTick()`。

| 状态 | 当前写入/变化 | 标脏与保存合同 |
| --- | --- | --- |
| `life` | `:312` 有效环境逐 tick `--life`；`:333,345` 重置；`:374` 配对时写 lifespan | `:672` 保存、`:640` 读取。纯递减/重置不直接 setChanged；配对/死亡通常随后改库存而标脏。 |
| `countdown` | `:307–309` 工作女王保持/抬至 1200；`:336–337` 公主递减并在重试时重设；`:341,347,372` 状态重置 | `:673` 保存、`:641` 读取。纯等待与无雄蜂重试不直接 setChanged。`onOpened:736–738` 改 6000 并显式 setChanged。 |
| `brood` | `:373` 接收繁殖结果，`:426` 女王寿终清空；`:334,346` 非正常工作状态清空 | `:674–683` 保存，`:646–657` 读取。正常繁殖 `:372–383` 与寿终 `:404–412,437–439` 都有库存 setter，因此不能概括“brood 完全不标脏”。异常/外部原地改库存导致状态重置另需单独范围。 |
| `endedQueen` | `:298` 每 tick 先 false，`:397` 寿终当 tick true | 当前和原版都不保存，是瞬时运行成功状态；没有新持久化缺口。 |
| `sky` | `refreshEnvironment:268–277` 只锁存 true | 当前和原版都不保存，重新加载/首次 tick 根据现场环境重新取得。不要把此运行态当成漏写 NBT 的新缺陷。 |
| 环境温湿度、`environmentKnown` | `:250–256,268–277` 首次与周期刷新 | 当前和原版环境值均为派生运行状态；重新取得本身不要求恢复旧值。 |

当前直接 `setChanged` 只有库存 `onContentsChanged:216–217` 和 `onOpened:736–738`。`saveAdditional:630–685` 的 Forge/Neo 分支都有完整字段写读，问题在何时进入原生序列化，不是这两个版本完全没写字段。

已检索共享及两平台 `src/main/java` 的 `ITileEntityNeedsSaving/NeedsSaving/setUnsaved/blockEntityChanged/markChunk/ChunkEvent/ChunkDataEvent/LevelTickEvent/WorldTickEvent/ServerTickEvent` 等入口，未发现给普通养蜂箱无条件周期标脏的等效实现。现有 fluid pipe/reactor/material washing tick 是各自业务；`ComponentCoverFallback.tick:118–139` 只遍历跟踪的 cover 宿主并 tick/sync cover，不是全部养蜂箱保存器。本结论的最小情形不需要任何覆盖板、其他机器或插件。

## 两版本实际原生保存链

读取实际缓存 Forge **1.20.1-47.4.20 mapped official** 二进制（`javap -p -c`，不执行 Minecraft）和 NeoForge **21.1.243 / Minecraft 1.21.1** 构建依赖的源码/二进制，身份与 SHA-256/entry 哈希见[本批回执](verification/inventory-persistence-audit-20261021.json)。

### 显式标脏确实能到宿主区块

两版 `BlockEntity.setChanged()` → 静态 `setChanged(Level,BlockPos,BlockState)` → `Level.blockEntityChanged` → `LevelChunk.setUnsaved(true)`。Neo 源行 `BlockEntity.java:191–200`、`Level.java:941–944`；Forge mapped class 同名方法字节码已核。普通 `BlockEntity` ticker wrapper 在调用业务 ticker 后没有自动 setChanged/setUnsaved；Neo `LevelChunk.BoundTickingBlockEntity.tick:704–738`，Forge 同名内部类 `tick()` 字节码对应。`ChunkAccess.incrementInhabitedTime` 两版也只是递增字段，没有顺带置 unsaved，不能用“玩家在附近/区块仍在 tick”代替保存通知。

### 自动保存、卸载、退出没有绕过 isUnsaved

Neo 真实源码：

- `MinecraftServer.java:924–929` 自动保存 → `saveEverything` → `saveAllChunks` → `ServerLevel.save:807–819` → `ServerChunkCache.save:299–301` → `ChunkMap.saveAllChunks`。
- `ChunkMap.java:413–438`：普通保存调 `saveChunkIfNeeded`；带 flush 的全保存也通过 `this::save`。`:716–734` 的定期选中/cooldown 判断后仍调用 `save(chunk)`。
- `ChunkMap.save:742–747` 先检查 `!chunk.isUnsaved()` 则 `return false`，通过后先清 unsaved，再 `:763` 调 `ChunkSerializer.write`。**只有到这里**，`ChunkSerializer.java:359–366` 才遍历 BE，`LevelChunk.java:416–422` 调 BE `saveWithFullMetadata` 进入养蜂箱 `saveAdditional`。
- `scheduleUnload:505–519` 卸载同样调 `save(chunk)`，没有先无条件 setUnsaved。
- `MinecraftServer.stopServer:622` 的退出保存传 flush=true；`ServerChunkCache.close:306` 再 save(true)，仍走同一门控。
- `ChunkAccess.java:269–274` 的 `isUnsaved()` 只返回 `unsaved` 字段，`LevelChunk` 不覆写成“只要有 BE 就必须保存”。

Forge mapped official 47.4.20 字节码中逐个确认：

- `ChunkMap.save(ChunkAccess)` offset 11 调 isUnsaved，15 分支，18–19 返回 false；offset20–22 清标志，106–111 才调用 `ChunkSerializer.write`。
- `ChunkMap.saveAllChunks(boolean)` 的 flush/普通两路均接同一 save 方法；`lambda$scheduleUnload$14` offset73–79 调同一 save，无强制标脏。
- `ChunkAccess.isUnsaved` offset0–4 只返回布尔字段；`incrementInhabitedTime` offset0–10 不改该布尔字段；`LevelChunk` 无 isUnsaved 覆写；BE ticker wrapper 未自动标脏。

保存事件 `ChunkDataEvent.Save` 在序列化后触发，并不能替跳过的干净区块触发养蜂字段写入。玩家/实体独立存储自动保存不等于 BlockEntity NBT 自动保存。

## 可由代码构造的最小风险条件

1. 正常放置/繁殖/开箱造成区块 unsaved，进行一次成功保存后标志清 false，磁盘记录 `life=L` 或 `countdown=C`。
2. 保持区块 tick。女王基因适应环境但当前时段/天气不产蜂脾，或公主还在等待且没有成功配对；连续若干 tick 只递减寿命/冷却，不触发库存 setter、开箱，也没有同区块其他方块/BE/覆盖板/计划 tick 等事件重新标脏。
3. 卸载或正常退出最终进入 `ChunkMap.save`，因 unsaved 仍 false 而跳过当前 BE 快照。再次加载会读到先前的 L/C，而非刚才内存已递减的值。

这个条件说明回退路径有实际保存门控支持，不是仅因缺少 setChanged 进行猜测。它也说明为什么通常测试时可能看不出：产物、成功配对、女王死亡、容器操作或旁边机器不断标脏，会把寿命/冷却顺便保存。不要把它描述成全部世界、全部 tick、必定重置，或声称已游戏复现。

## 后续边界

- 不在本次审计中修复。若后续实现修复，可以决定按持续状态变化标脏或增加有界周期保存通知，并避免无变化机器每 tick 引发额外比较器/邻居更新；选择本身应另立实现任务。
- 需要独立真实世界保存/卸载/重启核验才能确认玩家体验、具体回退量和额外插件影响。当前没有进行原生运行、计时测试或性能测量。
- 原每秒标脏仍有最多一个调度间隔/未保存修改的通用边界，不是 crash-safe 保证；不要把这一合同夸成事务持久化。
- 本次不扩展核反应堆/管道/其他机器相似代码，也不新增 prior 生命周期报告已经记录的世代分配问题。

[本批回执](verification/inventory-persistence-audit-20261021.json)保存原版／当前来源摘要、实际 jar／entry 哈希、原生方法观察和内存 javap 输出摘要；原生方法体与反编译源码未纳入项目生产或提交。完整目标继续，不因本域已登记一项差异就宣布所有保存合同核完。
