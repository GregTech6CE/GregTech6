# 硬币系统移除

本端移除 GT6 的整套硬币系统——硬币材料前缀、逐材料硬币物品、硬币堆与铸币模具方块及其方块实体、客户端渲染、配置与地牢产出；地牢原硬币堆位置改由锭堆承接。物品与方块侧清单并入[物品移除](item-removal.md)。提案与讨论见 [#382](https://github.com/GregTech6CE/GregTech6/issues/382)。

硬币是原版 GT6 自有内容（`MultiTileEntityCoin`，多方块 ID 32700），不是联动附属内容。移除前运行时注册计数：材料硬币物品 351、`tab_icon_coin` 1、`coin_pile` 与 `coin_mold` 物品各 1、硬币方块 2；旧注释的 586 作废，静态推算 349（上界 475）与运行时差 2，以运行时为准。

| 内容 | 差异 | 原因 |
| --- | --- | --- |
| 硬币物品与材料前缀 | 停注 `coin` 前缀，删除 351 个 `coin_<material>` 物品与 `tab_icon_coin`，不保留旧 ID；逐材料标签 `gt6ce:coin/*`、物品成分与坩埚回收权重随前缀消失。`plateTiny` 前缀、坩埚熔炼与通用材料加工不变。 | 硬币无玩法闭环：不进任何配方，唯一消耗是坩埚熔炼回 1/9 单位，与小块金属板等价而无独立收益；装饰用途无闭环。前缀泛型机制停注后物品侧自然消失。 |
| 硬币堆方块 | 删除 `coin_pile` 方块与 `CoinPileBlockEntity`；地牢原位置由 `ingot_pile` 承接，玩家放置的硬币堆不返还其中硬币。 | 硬币堆是硬币的放置形态，随硬币身份一并移除；材料堆三方块复用既有实现，不新增方块。 |
| 铸币模具方块与配方 | 删除 `coin_mold` 方块与 `CoinMoldBlockEntity`、其合成配方（钢质 `yhf\|PPP\|III`）与工具形状，运行时生成的 `survival_repairs/coin_mold.json` 随行消失。 | 铸币模具只服务手动铸币，硬币移除后无作用对象。本端无硬币专用机器：自动锤经 `PoweredToolTarget` 作用于 `MaterialAnvilBlockEntity`，铸币模具未实现该接口。 |
| 地牢硬币堆位置 | 4 个房间类共 29 处 `coins(...)` 改为 `pile(..., INGOT)`（走廊 16、图书馆 8、兵营 4、工坊 1），各处出现概率判定不变。 | 保留 GT6 地牢的装饰与奖励位置，以锭堆承接硬币堆的生态位。 |
| 地牢金属池 | `PILE_METALS` 由 12 项扩为 17 项，追加银×2、金×2、铂×1；工坊 `WORKSHOP_METALS`、储物间 `CRATE_METALS`、宝石池 `PILE_GEMS` 三个自定义池不变。 | 追加项承接原 `COIN_METALS` 的贵金属构成。`PILE_METALS` 是无人使用的兜底默认，扩充只作用于新增替换点。贵金属占比由 5/8（62.5%）降为 5/17（约 29.4%），待合议。 |
| 地牢奖励量 | 每处期望金属量由约 2.44 锭升至 32.5 锭（约 13.3 倍），为有意加强，待合议；推导见[附录C](#附录c-奖励量推导)。 | 锭堆沿用默认堆量 `1 + nextInt(64)`；原硬币堆 16 面各 1/3 概率取 0–7、再随机一面覆盖为 1–8，期望 22.0 枚、每枚 1/9 单位。 |
| 地牢随机流与金属语义 | 删除硬币金属的独立选取流（`rollCoin`、`COIN_METALS`、`CellState.coinMetal` 及其全地牢种子），金属改由每处锭堆从主流抽取，不再全地牢统一；新生成地牢家具布局位移，已生成区块不受影响。 | 独立流随硬币身份移除；每处抽取使同一地牢可见多种金属。主流抽取次数由每处约 23.3 次降为 2 次，位移已声明并接受。 |
| 客户端渲染 | 删除两节点的硬币物品/硬币堆/铸币模具渲染器、烘焙模型、几何与着色分支；`geometry/coin_shape.json` 一并删除。 | 渲染随内容移除。维护者此前人工测试已确认手动铸币渲染有问题（铸出为无浮雕平面，与地牢/创造来源的齿轮浮雕不一致），本次一并消除。 |
| 横向接入 | 删除 Jade 硬币堆显示分支、`CoverAdmission` 两条拒绝、`ComponentCoverCapabilitiesMixin` 一条目标、NeoForge 储物容器能力一条登记、`CoinItemExpireHandler` 订阅与类（硬币掉落 200 tick 合并）。 | 接入点随内容移除；材料堆显示与封面准入走既有分支。 |
| 配置 | 删除 `general.use3DCoins`、`general.use3DCoinsInv` 两键，旧 TOML 值由加载器忽略。 | 3D 硬币开关随渲染移除。 |
| 创造页 | 删除 `coins` 页签族与 `itemGroup.gt6ce.coin*`；页签与图标由前缀泛型生成，停注后自然消失。 | 创造页随内容移除。 |
| 资源 | 删除 212 个硬币专用文件（core 207、共享 1、Forge 4），共 329,216 B；明细见[附录A](#附录a-资源清理)。 | 全部资源经引用核对为硬币专用，无与非硬币内容共用者；减少模组体积。 |
| 语言键 | 英文删 10 条、中文删 15 条，其中 9 条为遗留键（4 条现代键无注册对象、5 条旧版中文键无读取者）；明细见[附录B](#附录b-语言键)。 | 随内容清理，避免无主键。 |
| 存档兼容 | 硬币物品/方块/方块实体按既有缺失注册方式处理，`gt.coin.*` NBT 成孤儿数据；已放置硬币堆中的硬币不返还，不新增迁移工具。 | 本端无物品迁移机制；保持既有缺失注册处理方式。 |
| 附属影响 | 删除的公开类型与成员均为源码级不兼容变更（`CoinItem`、`CoinPileBlock`、`CoinMoldBlock`、两者方块实体、`GTCoinPiles`、`CoinStampPattern`、`CoinPileRules`、`GTDungeonData.COIN_METALS`、`coins(...)`、`CellState.coinMetal()`），均不在公开 API 清单内；`GTDungeonData.PILE_METALS` 为公开静态字段，扩充改变以默认池调用的附属代码，仓库内无此类调用者。coin 在 `api/` 全模块零出现，API 版本不变。 | 满足 `gt6Coin` 谓词的附属材料此前自动获得硬币物品，前缀删除后该行为消失。 |

## 附录A 资源清理

| 类型 | 数量 | 字节 | 详细信息 |
| --- | ---: | ---: | --- |
| 贴图 | 94 | 20,887 | `textures/**/coin*` 与 `textures/block/machines/tools/mold_coinage/**`；后者 8 个文件名不含 coin，按目录删除 |
| 模型 | 113 | 300,926 | `models/**/coin*`，占资源体积 91.4%；其中 65 个硬币堆模型与 `coin_minted.json` 共 66 个、292,314 B，占 88.8% |
| blockstate | 3 | 6,760 | `blockstates/coin*`，其中 Forge 孤儿 `coins.json` 1 个 |
| 几何 | 1 | 403 | `geometry/coin_shape.json` |
| 堆组 | 1 | 240 | `stack_groups/material_forms/coin.json` |
| 合计 | 212 | 329,216 | core 207 + 共享 `models/block/tool/coin_mold.json` 1 + Forge 孤儿 4（1 blockstate + 3 模型，692 B） |

`material_icons/<set>/` 删 `coin.png` 与 `coin_overlay.png` 两组共 82 个，目录本身保留；`models/item/material/<set>/` 同理只删 `coin.json`。jar 净收益 326,611 B（`compact-model-json` 去重排除 45 个文件、2,605 B）。

## 附录B 语言键

| 键 | 覆盖 | 备注 |
| --- | --- | --- |
| `block.gt6ce.coin` | 英+中 | 遗留：无注册对象 |
| `block.gt6ce.coin_mold` | 英+中 | |
| `block.gt6ce.coin_pile` | 英+中 | |
| `block.gt6ce.coins` | 英+中 | 遗留：无注册对象 |
| `item.gt6ce.coin` | 英+中 | 遗留：无注册对象（物品 ID 为 `coin_<material>`） |
| `item.gt6ce.tab_icon_coin` | 英+中 | |
| `itemGroup.gt6ce.coin` | 英+中 | 遗留：无注册对象（页签族为 `coins`） |
| `itemGroup.gt6ce.coins` | 英+中 | |
| `tooltip.gt6ce.coin_mold.1` | 英+中 | |
| `tooltip.gt6ce.coin_mold.2` | 英+中 | |
| `gt.lang.recipes.mold.coinage` | 仅中 | 遗留：旧版键，无读取者 |
| `gt.tooltip.coins.1` | 仅中 | 遗留：旧版键，无读取者 |
| `gt.tooltip.coins.2` | 仅中 | 遗留：旧版键，无读取者 |
| `gt.tooltip.coins.3` | 仅中 | 遗留：旧版键，无读取者 |
| `oredict.prefix.coin` | 仅中 | 遗留：旧版键，无读取者 |

合计英文 10 条、中文 15 条；遗留键 9 条。

## 附录C 奖励量推导

硬币堆 16 格各以 1/3 概率取 `next(8)`（0–7，期望 3.5），随后随机一格被 `setFaceCount` **覆盖**为 `1 + next(8)`（1–8，期望 4.5），故期望枚数 = 15 × (7/6) + 4.5 = **22.0**；每枚权重 `U9 = U/9`（`U = 648,648,000`），得 **2.44 锭**。锭堆 `nextStack() = 1 + nextInt(64)` 为 1–64 均匀分布，期望 **32.5** 个锭，每个权重 `U`。比值 32.5 / 2.44 ≈ **13.3**。

原 `COIN_METALS` 为铜×3、银×2、金×2、铂×1 共 8 项，贵金属 5/8 = **62.5%**；扩充后 17 项中贵金属 5 项，5/17 ≈ **29.4%**。