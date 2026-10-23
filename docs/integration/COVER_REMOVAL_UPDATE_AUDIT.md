## 覆盖板拆除后的原生支撑更新

用户报告「移除覆盖板后没有进行一次方块更新导致拉杆可能还留在上面」。本项仅针对本仓库 GT6CE 宿主，不扩大为其他模组兼容保证。

### 原版来源和现代差异

只读 `work/references/gregtech6-original/` 中：

| 原文件 | 行为证据 | SHA-256 |
|---|---|---|
| `src/main/java/gregapi/tileentity/base/TileEntityBase06Covers.java` | 148 行撬棍调用 `setCoverItem(..., F, T)`；306 行在覆盖数据变化后调用 `causeBlockUpdate()` | `c592d3905904adf81b14dd29e3b3e28310229314deefb232bef371cc950c0bb9` |
| `src/main/java/gregapi/tileentity/notick/TileEntityBase04Covers.java` | 177 行撬棍入口；316 行执行相同更新策略 | `d074b16c5dae1c76083b7cf9de43ac35dd821e868d65c883cdeac634745a2790` |

两文件保留 GregTech-6 Team 的 LGPL-3.0-or-later 声明；本项采用更新契约，未复制旧平台实现。

现代 `PanelCoverRuntime.removed()` 原先只执行客户端同步和信号重算；客户端更新标志 2 与 `updateNeighborsAt()` 不替代方块的 `updateShape` 支撑检查。已检查缓存 NeoForge 21.1.243 原生类：`FaceAttachedHorizontalDirectionalBlock.updateShape()` 在支撑侧变化且 `canSurvive()` 为 false 时返回空气。两版原生映射均有 `BlockStateBase.updateNeighbourShapes(LevelAccessor, BlockPos, int)`。

修复在覆盖数据已清除后，通过共享 `CoverWorldInteraction.removed(owner)` 调用原生形状传播和邻居通知。原版逻辑负责移除和掉落，不手动破坏拉杆；底层完整方块仍提供支撑时应保留拉杆。组件覆盖路径经 `PanelCoverRuntime` 进入，独立物流覆盖路径经 `LogisticsCovers.remove()` 进入。

### 修复前：两版原生基线已复现

证据为 `work/validation/cover-signal-removal-baseline/{forge,neoforge}-baseline.xml`。

| 测试 | Forge | NeoForge | 实际到达位置 |
|---|---|---|---|
| `crowbarremovaldetachesnativeleveroneverythinwireface` | 失败 | 失败 | 裸线 DOWN 面，撬棍拆除后拉杆未变为空气 |
| `crowbarremovaldistinguishesmediumandfulldiameterpipesupport` | 失败 | 失败 | medium 管 DOWN 面，同样未自动脱落 |
| `removingnonsolidvalvepreservesunderlyingfullhostsupport` | 通过 | 通过 | huge 管六面非实体压力阀拆除均保留底层支撑 |

两个失败均发生在 `crowbar must automatically update neighbor support; retained=false, side=down`，此前已通过：初始细宿主无完整支撑、真实覆盖板物品放置、真实拉杆放置及存活／数量消耗、真实手持撬棍交互、覆盖板槽清空及恰好一件返还。故不是夹具放置或返还失败。失败即终止循环，修复前未执行到后续面、绝缘线与 huge 管空白覆盖板分支，不能将设计矩阵误报为已通过覆盖。

通过的压力阀对照实际走完六面，并验证拉杆保留、无拉杆掉落、覆盖板恰好返还一次、宿主不变、重复移除不复制物品。已有 `CoverPlateSupportTests` 对薄宿主上的非实体火把／压力阀拒绝拉杆放置提供另行回归；此前其拆除测试仅查询 `canSurvive()`，未证明自动脱落。

### 修复后验证范围

`CoverRemovalUpdateTests` 设计为 3 方法、30 面向案例：裸线／绝缘线 12，medium／huge 流体管 12，huge 管非实体压力阀 6。测试经原生物品与游戏模式交互入口，拆除后不手动调用邻居或形状更新；同一服务端调用内检查拉杆状态、正常掉落数量、覆盖板返还和宿主保留。最终双版原生验证均通过，详见下方终态。

独立物流覆盖调用点目前仅有源码／编译核验，未增加独立物流撬棍原生案例。客户端画面、声音、旧存档及外部模组不在本项自动化结果内；整体移植仍未完成。

### 修复后首次 Forge 与掉落实体验证夹具诊断

`work/validation/cover-signal-removal-fixed/forge-fixed.xml` 的两个移除案例已越过「拉杆应自动变为空气」断言，随后停在「恰好一个拉杆掉落」查询断言；该轮不是全绿，也未据此宣布掉落丢失。

`work/validation/cover-removal-drop-diagnostic/forge-diagnostic.xml` 的同位置独立原版对照明确给出：`actual=0`、`tileDrops=true`、`entitiesLoaded=false`、`entityTicking=false`；独立放在石头上的原版拉杆 `controlSurvives=true`、`controlLoot=[1 lever]`、`controlDestroyed=true`，但查询到的掉落增量仍为 0。显式加入普通物品实体返回成功，却 `markerTracked=false`、`markerVisible=0`。因此此次失败由远端夹具区块尚未进入可查询的实体加载状态造成，不能归咎为生产掉落逻辑丢物品。

修正方案不降低「恰好一个」要求，不手动补掉落或改实体管理器：为这三项使用独立 9×9×9 空模板；在模板中心开展 5×5×5 快照／还原；以公共接口逐格验证属于本测试模板且实体已加载、正在 ticking。旧 `test_empty` 实际仅 1×1×1，不能只把远端坐标改为任意局部偏移而假定不与邻居测试重叠。报告校验器仅为三个精确方法标识映射新模板，231／222 的必测集合不变，错误模板、缺项及失败／错误／跳过仍拒绝。

修正夹具后的两版终态已通过；旧基线、首轮修复和诊断记录保留，不追改为通过。

首轮修复的两个循环在首个 DOWN 案例的掉落查询处停止，故仅证明该面自动脱落，不证明后续面／绝缘线／完整管空白覆盖板分支全部通过。基线关于真实拆除后拉杆残留的证据仍有效，因为方块状态查询不依赖物品实体可见性。夹具重跑未再修改生产修复；最终仍须实际走完并通过全部 30 个面向案例，才可报告此矩阵通过。

### 修正夹具后：双版终态通过

`work/validation/cover-signal-removal-fixed-retry/forge-fixed.xml` 已确认三项均通过，classname 为专用 `gregtech_issue_audit:cover_removal_empty`。本轮三项循环实际走完 30 面向案例：裸线／绝缘线共 12 面、medium／huge 管共 12 面、huge 管非实体压力阀 6 面。每例均通过本模板位置与实体加载／ticking 前置条件、真实放置与撬棍、自动脱落或保留、恰好一个或零个原生拉杆掉落、覆盖板恰好返还一次及宿主保留／重复移除不复制检查。Forge 集中报告为 231／231，通过不代表旧存档或客户端验收。NeoForge 同夹具重跑222/222通过；两端各自走完全部30面向案例。总453项、双版构建/core、137项Python检查、491组REMI及打包/SDK检查通过。回执：work/validation/cover-signal-removal-fixed-retry/local-validation-receipt.md。
