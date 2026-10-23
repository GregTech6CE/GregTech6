# 双版本原矿坩埚展示对齐与 NeoForge 实际采矿青铜链

日期：2026-10-02。基线：`68682331a7d354ab7d31ea49e9fffad245645d6a`。本批未改来源目录、注册身份或保存格式。

## 生产修改与来源取舍

原版 GT6 MultiTileEntitySmeltery 对 oreRaw 使用 crushing amount × ore multiplier；铜锡均为每份 1U。移植版添加的原矿展示却使用 prefix 的 2U 壳重量，造成看起来两倍产出。CrucibleInputRules.smeltingPreview 现在复用实际输入换算，再按熔炼目标比例给两版同一材料、数量、温度；保留实际 3Cu+1Sn=4U Bronze 和 1357K 反应，不提高真实产量。这里修的是移植版新增展示，原 GT6 getNEIRecipes 没有显式列 oreRaw。

Forge 输入适配器将 stored-content 判定提前到材料物品/方块分支之前，与现有 NeoForge 判定对齐，避免带内部库存的材料容器当空壳熔掉。原矿整数乘法的中间 count × multiplier 也改用 exact 运算。masson 的独立 process/store 设计作为比较，未再导入第二套热量或材料底层；已有来源、作者和历史继续保留。逐文件哈希见 core/provenance/crucible-raw-preview-20261002.json。

## 本批一次集中检查与修正

- 共享材料合同 6160 项及两版 Java 编译通过：39 秒，完整材料/prefix 快照不变。
- 第一次 NeoForge 运行已经过实际工作台制作燧石镐、四次真实矿块采收和原矿收集，在展示产物 ID 判定失败。配方注册合法地统一铜为 minecraft:copper_ingot；测试错误地要求 gregtech 铜锭。保留失败日志，改为检查材料、ingot 形态、数量与温度。
- 修正后的同一既有 GameTest 通过：1 分 49 秒，包括 NeoForge 加载。给定燧石/木棍在真实工作台结果槽制镐，实际采收铜矿三次、锡矿一次，镐磨损 300；采得的原矿直接进入坩埚，煤炭盒真实 HU 加热至 1357K，生成 4U 青铜，四次实交互浇铸取出四锭，凿子磨损 15、夹钳磨损 4。每 tick 检查实际物品/坩埚/模具/产物合计 4U，无额外单位。3020 tick 是加速 GameTest tick，不是三分钟普通服务器观察。
- 独立的旧叠盆子案例继续通过：给定 9U、手动冷却后九次输出，余量零且热态拒取；不算作采矿流程原料。

## 未完成范围

本测试给定设备、放置矿块、启动原料和煤炭/钢工具，未验证自然找矿、生存取得设备与所有工具、客户端、Forge 同一流程或此世界独立 JVM 重载。青铜锭到粗制头铸造、磨制、装柄仍需接成连续链；先前给定粗制头的工具/存档检查属于另一条证据。打包另行执行；整体移植仍未完成。

日志及范围：[raw-ore-playflow-20261002.json](raw-ore-playflow-20261002.json)。
