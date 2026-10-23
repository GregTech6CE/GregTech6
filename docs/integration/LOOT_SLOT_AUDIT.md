# 战利品箱随机填槽核查

第四十九批，2026-10-21，基线 `bbdcb7f73`；新增 ALIGN-168。本项核对实际入口、原依赖反汇编与两版维护源码投影，没有改变生产逻辑或运行 Gradle／游戏／新测试。

**ALIGN-168：原 GT6 可配置 GUI 箱按随机槽直接替换，当前扁平化 LootChestBlock 按首个空槽插入；槽位碰撞、覆盖以及箱内分布不同。** 这不依赖抽取次数上界，也不是一次性 crate 的抽样层次问题。地牢显式设置 `dungeonLoot` 后走现代原生表的分支另列在下文，不能说所有当前箱子都按首空槽生成。

## 原版的真实入口与下游

- [原 Loader_MultiTileEntities.java](C:/Dev/gregtech6-master/gregtech6-master/src/main/java/gregtech/loaders/b/Loader_MultiTileEntities.java:151) 注册 `Mossy Stone Chest`，容量 54，`lootchest` 外壳。同文件 132–133 是金属箱与强化木箱的同一 MultiTileEntityChest 实现。
- [MultiTileEntityChest.java:273](C:/Dev/gregtech6-master/gregtech6-master/src/main/java/gregapi/block/multitileentity/example/MultiTileEntityChest.java:273) 的 `getSubItems` 对 lootchest 遍历 ST.LOOT_TABLES，产生带 `gt.dungeonloot` 的可放置物品；101 行从 NBT 读入该表名。并非只有不可达的通用工具函数。
- [Loader_Loot.java:337](C:/Dev/gregtech6-master/gregtech6-master/src/main/java/gregtech/loaders/c/Loader_Loot.java:337) 把 `gt.books` 加入该目录，338–356 设置抽取范围并登记单本书的加权条目。此表提供明确的实在入口，结论不依赖第三方扩展表。
- [MultiTileEntityChest.java:234](C:/Dev/gregtech6-master/gregtech6-master/src/main/java/gregapi/block/multitileentity/example/MultiTileEntityChest.java:234) 服务端正常开盖先调用 generateDungeonLoot，再开 GUI；261–268 行把自身库存传给 ST.generateLoot。
- [ST.java:1027](C:/Dev/gregtech6-master/gregtech6-master/src/main/java/gregapi/util/ST.java:1027) 对非 Twilight Forest 表在 1033 行调用原生 WeightedRandomChestContent.generateChestContents，传入 ChestGenHooks 的行和抽取次数。1035–1061 的后处理逐槽规范化剩余物品，不能恢复已被覆盖的早先堆。
- [Base05Inventories.java:114](C:/Dev/gregtech6-master/gregtech6-master/src/main/java/gregapi/tileentity/base/TileEntityBase05Inventories.java:114) 的 setInventorySlotContents 更新库存标记后，直接用 OM.get 返回值替换指定数组元素；没有检查空槽、重选或合并。OM 是物品规范化边界，不改变所选槽。
- 原世界替换器的另一条实际入口是 [ChestGenHooksChestReplacer.java:122](C:/Dev/gregtech6-master/gregtech6-master/src/main/java/gregtech/worldgen/ChestGenHooksChestReplacer.java:122)：生成同一 GT 箱并携带当前类别名。此处只证实会进入同一原始生成合同，不推断当前各现代世界表的内容必须逐项照搬。

## 原 Minecraft／Forge 库存重载证据

读取本机原 1.7.10 客户端的 qx 类字节码与 Forge SRG 映射。jar、class entry 和反汇编摘要哈希见[本批回执](verification/inventory-bumble-audit-20261021.json)，原生反汇编仍留在忽略的 work/。映射确认 `qx` 是 WeightedRandomChestContent，`rb` 是 IInventory，`add` 是 ItemStack；库存重载为 `(Random, qx[], rb, int)`，不使用接受 TileEntityDispenser 的另一重载证明箱子。

qx.javap 46–115 行的库存方法在字节码偏移 74–87，以及超堆叠分支 121–134，均取库存大小后随机选槽，并直接调用库存 setter。没有读取该槽是否为空。

本地 qx 来自 **Minecraft 1.7.10 vanilla client**，不是已重放 Forge 二进制补丁的类。为交叉确认 Forge 行为，独立读取了 [MinecraftForge 官方 1.7.10 补丁](https://raw.githubusercontent.com/MinecraftForge/MinecraftForge/1.7.10/patches/minecraft/net/minecraft/util/WeightedRandomChestContent.java.patch)：一基第 12–25 行的**库存**方法 hunk 增加逐行 generateChestContent 后，对返回的每个堆仍随机选库存槽并直接设置；第 38–61 行另一个 dispenser hunk 不用于本结论。Forge 的可扩展行生成钩子不取消随机替换合同。没有声称本轮运行了 1.7.10 Forge 游戏或重放了其 binary patches。

已复核的原依赖身份：

| 工件 | SHA-256 |
| --- | --- |
| Minecraft 1.7.10 client jar | `a4fc2284657544e0f4bcc964f927c2fda3e3a205178ed1d5d58883aaf9780cce` |
| Forge 1.7.10-10.13.4.1614-1.7.10 universal jar | `00d1ca02192c7efb87da95552ebf247021f1e1d642f86d0a03076314def11529` |
| qx.class 原始条目 | `8b0b70d0371e9f9482b6fb574e15e9e601fe0f45198cd5f5a4829ffe7c4a411b` |

本批回执中另留有 agw／ahu／aip 等其它核对条目；它们不作为此槽位结论的证据。远端补丁通过网页工具读取，没有下载原始字节或伪造其 hash。

## 当前两版的活注册与首开链

当前核对平台为 Minecraft 1.20.1 Forge 与 Minecraft 1.21.1 NeoForge。原生边界符号按维护源码实际投影确认，未启动构建。

| 环节 | Forge 活入口 | NeoForge 活入口 |
| --- | --- | --- |
| 启动注册 | GregTech.java:53 → MachineBlockBootstrap.run:18 → registerMachines:32 → GTLootChests.registerAll | GregTechNeoForge.java:86 → GTMetalChests.initialize:9 → GTLootChests.registerAll |
| 方块目录 | 共用 GTLootChests.java:37、54–69，按 LootChestCatalog 注册 loot_chest_books 等方块与物品 | 同一共用目录，DeferredHolder 分支 |
| 对应实体绑定 | 共用 GTBlockEntities.java:352–359，把 GTLootChests.LOOT_CHESTS 并入 METAL_CHEST 的有效方块 | 共用 GTBlockEntities.java:98 经 GTMetalChests.all；平台 GTMetalChests.java:4 把 LOOT_CHESTS 并入 |
| 玩家打开 GUI | 共用 MetalChestBlock.java:194、206 的 use → ServerPlayer.openMenu | 共用 MetalChestBlock.java:177／187 → interact:191 → openMenu:206 |
| 首开填槽 | 共用 MetalChestBlockEntity.createMenu:222–224 → generateLootIfNeeded:154 → 无 dungeonLoot 时的 LootChestBlock 分支:181–188 | 同一共用主链 |
| 平台库存类型 | Forge ItemStackHandler | NeoForge ItemStackHandler |

[LootChestCatalog.java:2](C:/Dev/GregTech6/core/src/main/java/com/gregtech/gregtech/content/loot/LootChestCatalog.java:2) 确实列出 `books|gt.books`。[GTLootGen.java:24](C:/Dev/GregTech6/core/src/main/java/com/gregtech/gregtech/content/recipe/generated/GTLootGen.java:24) 保留该表的次数记录，125–141 行有对应书籍条目；GTLootTables.load:62–84 读取这些行，roll:148–156 产生选中的堆。这些是实际注册／调用链，不以类名相似为证据。

[GTLootTables.java:191](C:/Dev/GregTech6/multiversion/src/main/java/com/gregtech/gregtech/content/loot/GTLootTables.java:191) 的维护主循环在 194–203 对每次非空结果从槽 0 开始扫描，跳过非空槽，向第一个空槽插入后终止本次扫描。两版除了 ItemStackHandler 的包名外使用同一槽选择循环：**不随机选槽，不覆盖占用槽**。

Stonecutter 对 Neo 代码有块注释遮蔽。本轮用既有 check_relay_sharing.project 先暴露维护中的 Neo 分支再选平台，已保存 14 个可阅读视图与各自代码／字面量签名 hash；没有直接删注释而误漏 Neo 主链。

## 地牢显式表的分支边界

[GTDungeonData.java:834](C:/Dev/GregTech6/multiversion/src/main/java/com/gregtech/gregtech/worldgen/dungeon/GTDungeonData.java:834) 实际放置金属箱后，在 839 行调用 setDungeonLoot。MetalChestBlockEntity.generateLootIfNeeded 的 156–180 行会优先走该分支：Forge 用 getLootData().getLootTable，NeoForge 用 reloadableRegistries().getLootTable；两版均在 167–175 把现有槽复制到 SimpleContainer，调用原生 LootTable.fill，再写回库存。

因此此分支**不经过** GTLootTables.fillInto 的首空槽循环。本报告确认它真实可达，并确认两版委托入口；没有继续反编译现代 LootTable.fill 的槽算法，也不把它称为首空槽、随机覆盖或本轮第二个缺失。若后续需要把地牢原生 fill 也纳入同一修复，应先单独核其现代方法体与所需兼容合同。

## 与已记录项的区别和影响范围

- ALIGN-159 是抽取次数闭／半开上界；本项固定抽取次数仍成立。
- ALIGN-160 是一次性 LootCrateBlock 先生成整表再均匀选堆；本项是能打开 GUI 的 LootChestBlock／MetalChestBlockEntity，不涉及那个一次性 crate。
- ALIGN-161 是空成功首开是否给经验；本项不讨论经验。
- 仅作为源码推导示例：固定相同的 8 次、每次生成一本有效书，且初始 54 槽全空。原随机槽允许两次落在同槽，早先那本被后者替换；当前每次进入不同的首空槽，得到槽 0–7 的八个单本堆。这个例子不改变次数、权重或所选行，说明可独立记录；它不是实测，也不声称所有表的数量都错。
- 原 setter 还能替换事先占用的槽；当前循环显式保留占用槽。这是方法合同的直接后果，本轮没有运行自动化预填箱子的玩法验证。
- 未核 Twilight Forest 的专用替换器、第三方自定义 WeightedRandomChestContent 子类、现代原生表的全部槽分配细节、任意种子的精确概率、跨版本同 seed 相同输出、旧存档兼容。现代 Minecraft 表内容变化本身没有被列为本项缺失。

## 证据与边界

[本批回执](verification/inventory-bumble-audit-20261021.json)合并本项六份原版、十七份当前源码／投影工具及其它两项核查的去重来源，另保存十四份实际平台视图的身份和哈希。视图已从当前维护源重新投影核对，没有直接删注释而丢掉 Neo 分支。没有把 Minecraft 反编译源码纳入项目，没有构建、游戏、保存重启或推送；不宣称所有战利品表已完整核完。
