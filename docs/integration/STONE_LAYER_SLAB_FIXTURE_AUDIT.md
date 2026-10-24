# 石层台阶回归：固定逻辑坐标，保留真实单格执行

2026-10-24。仅修复测试夹具，不改生产世界生成、层表或台阶转换规则。

## 已证实的失败

提交 `9ce86aa7edcdedb7e95ea5802f2f7f1a54cb2f62` 的远端 focused CI
`40068166870` 的**下载工件 XML**确认：NeoForge 304 项中
`structureCobbleSlabsFollowOnlyTheirLocalLayer` 报
`cannot find bounded deterministic fixture seed`，位置为
`(-12996951,-60,-2694830)`。Forge 313 项的该台阶用例通过；Forge 唯一失败是
独立的 `drilltorchcancellationandcreativepreserveinventory`（火把事件/世界还原）。
`gh` 获取的 job 日志曾把重复的 NeoForge 内容标成 Forge，因此平台归属以两个
平台各自下载工件中的 `build/reports/gametest/issues-64-81.xml` 为准，不依赖该标签。

XML 所报位置不能直接认定为测试的 `absolutePos(1,1,1)`；纯 Java 重现同时覆盖
所报位置及 `+1,+1,+1` 偏移候选，不宣称该候选就是实际原生中心。

使用仓库实际 `GTCellNoise`、`GTStoneLayersGen`，不复制或模拟噪声算法：

| 坐标 | 原 0..99,999 搜索 | 首个 noDeep=false / stoneType=null 种子 |
|---|---|---|
| 所报位置 `(-12996951,-60,-2694830)` | 无负例；seed 0 有正例 | 1,307,465 |
| +1 偏移候选 `(-12996950,-59,-2694829)` | 无负例；seed 0 有正例 | 144,263 |

根因不是矿物离心配方。噪声的 seed 只改变最近晶胞的偏移；最终层值仅由晶胞坐标
计算，不含 seed。`hash` 的低八位经 `hash >> 13` 混合，只依赖输入 seed 的低
21 位，因此 `2^21` 是完整的偏移周期（不宣称是最小周期）。在 `(0,0,0)` 穷举
完整周期，全部落在第 65 层，没有所需负例。单纯扩大随机/顺序重试不是普适修复。

## 测试边界与修复

- 固定逻辑采样点 `(1,-59,1)`：seed 0 → 第 72 层 `LIMESTONE`；seed 126 →
  第 106 层 `stoneType=null`、`material=Stone`。两者均 `noDeep=false`，不会以
  深层提前跳过冒充无转换目标。
- 仍使用完整 `GTStoneLayerFeature.place`。代理仅将这一逻辑点映射到 GameTest
  专用 3×3×3 模板中心的**同一个真实原生方块**。正例整块校准、台阶转换和负例
  均保持相同逻辑/原生坐标；仅 seed 不同。
- 其他方块读取统一为空气；只接受该逻辑点的写入，写入实际原生中心。仅放行当前
  特性需要的种子、区块内状态与高度查询；未知回调直接失败，不再反射委托到世界，
  因而不能误访问逻辑坐标对应的真实区块。
- 原有模板边界、已加载区块、无方块实体、`finally` 还原保持。表面碎石高度固定
  为最小建造高度，原有隔离边界保持。
- 保留圆石/苔石两种变体、上下半块、干燥双台阶、非规范含水双台阶、所有 GT 台阶
  六轴/含水状态及不相关台阶/楼梯保护。负例增加两种整块及所有半块/含水状态。
- **这证明固定逻辑局部石层对真实方块状态的完整特性处理，不证明随机自然地形
  世界位置的生成，也不证明客户端画面、旧存档或完整地形生成。**

## 回归与当前验证

`tools/tests/test_stone_layer_slab_fixture.py` 调用 JDK 编译实际两个 core 源文件、
测试专用夹具与纯 Java 合同，覆盖已失败坐标、两固定分支、完整 seed 周期、
负 seed/高位 seed、世界边界附近浮点坐标，并检查原生代理和玩法断言未被替换。
focused CI 的现有 Python 回归步骤会执行该测试。

草案阶段：JDK 17 与 JDK 21 各通过 3 个 Python 回归，其中实际 Java 合同为
**2,297,350 个检查**，约 2 秒。独立代码审查未发现阻断项。另四项故意破坏
负例种子、固定坐标、读取边界、写入边界的变异全部被回归拒绝。缺少 JDK 会
明确失败，不会将仅源码检查伪装成已执行 Java 合同。

修改后的 `StoneLayerSlabAuditTests` 和夹具 helper 另经缓存原生 API 的直接
`javac` 编译：Forge/JDK 17、NeoForge/JDK 21 均通过（仅弃用提示），未调用 Gradle。
这只确认源代码与两版 API 兼容，不是实际世界/代理运行验证。

**修改后的双平台原生 GameTest 与完整构建已实际通过：Forge 329、NeoForge 320，合计 649 项。** 该结果来自真实原生执行，不是将纯 Java 检查当作游戏通过。远端 CI 将对新提交另行验证。

## Final local 649-case receipt

Forge 329/329 (7m39s), NeoForge 320/320 (4m10s), zero failures/errors/skips, native and strict-checker exits 0. Dual/core build, 161 Python guards, 491 REMI groups, 1039 shared packaged classes per loader and SDK 118 core + 46 native passed. Runtime/test/CI inputs remained unchanged during the gate; documentation updates are recorded separately. Evidence: `work/validation/ore-centrifuge-eight-fixed-retry/local-validation-receipt.md`. Forge XML SHA256 `924819b1dce8bf7948943a729b1d3f2a9845a1650e7a091b2bd16459c69552f8`; NeoForge `fddc4111b1b35f3fc0a0c621b99fb2d8e50c9e713865d3b26030dd659860154f`. This does not relabel the earlier remote 9ce86aa7e failure as passed.
