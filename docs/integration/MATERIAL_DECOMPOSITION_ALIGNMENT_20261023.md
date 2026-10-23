# 通用分解合同对齐

对应[分解合同核查](MATERIAL_DECOMPOSITION_AUDIT.md) ALIGN-189–193。目标是让通用分解重新由原版 `MT.java` 的材料标签决定，而不是由“直接组分是否全为元素”的形状推断决定。

## 数据模型

- `GTMaterial` 持有一组 `GregTechTags.Tag`，提供 `tag(...)`、`hasTag(...)`、`getTags()`。这是材料处理合同，不混入 `MaterialProperty` 的形态/物性标志。
- `MaterialProcessingTags` 是由 `MT.java` 生成的唯一标签表，`MaterialDefinitions.link()` 在组成数据应用前调用。
- 标签来源：
  - 工厂名：`*dcmp` → `Compounds.DECOMPOSABLE`；`*cent` → 追加 `Processing.CENTRIFUGE`；`*elec` → 追加 `Processing.ELECTROLYSER`。
  - 显式常量：材料行里的 `DECOMPOSABLE` / `CENTRIFUGE` / `ELECTROLYSER`。
  - 链式调用：`alloySimple` / `alloyCentrifuge` / `alloyElectrolyzer` / `setAloy` / `uumAloy`。
  - 辅助工厂：递归解析 `MT.java` 中的 `static OreDictMaterial` 方法体，例如 `mixdust → dustcent`、`alloymachnd → metalmachnd`。
- 重新生成：

```bash
python -I tools/transpile_gt6_materials.py --processing-only
```

源文件默认取 `work/gt6-ref/gregtech6/.../MT.java`，不存在时回退到旧的 `gregtech6-master` 路径；其他 checkout 用 `GT6_MT_SOURCE` 指向自己的 `MT.java`。

## 加载器合同

`DecompositionRecipeLoader` 按以下顺序处理每个规范材料：

1. 必须有组成、`0 < divider <= 64`、带 `Compounds.DECOMPOSABLE`。
2. `Processing.CENTRIFUGE` 加入离心机，`Processing.ELECTROLYSER` 加入电解槽；两个都有就两台都加。
3. 输出：组件熔点 `<= 293K` 时请求该材料的已注册流体（含原版水/熔融比例），否则请求 `mTargetPulver` 的粉尘。单个组件无法解析时丢弃该组件，其余继续。
4. 输入：有可用流体形态时优先生成流体输入配方，并抑制固体入口；否则生成 `divider` 个粉尘，`divider % 9 == 0` 时再生成 `divider/9` 个块粉。
5. 数值：`EU/t = max(16, total * 14 / U)`，`duration = ceil(total * 292 / U)`；`total` 是未除分母的组分总量。
6. 配方提交使用原版的 optimize/checkCollisions 开、fake/hidden/logErrors 关。

## 明确保留的现代差异

- 电解槽不注入 `ST.tag(0)` 选择器占位物品；`MachineRecipeMapDefinitions.Electrolyzer` 的最小输入已放宽，单流体和单粉分解仍然有效。
- 端口每种材料只有一个流体绑定解析入口，因此原版“液体输入 + 气体输入”两个变体在这里合并为一个可用形态，而不是两条配方。
- `blockDust` 在端口是方块形态，入口通过 `GTBlocks` 取方块物品，而不是原版的前缀物品。

## 样例合同

| 材料 | MT 来源 | 标签结果 |
| --- | --- | --- |
| Alumina (`Al2O3`) | `oredustdcmp` | 仅 `DECOMPOSABLE`，没有通用机器配方 |
| Rutile (`TiO2`) | `oredustdcmp` | 仅 `DECOMPOSABLE` |
| Bronze | `clloymachine` + `.uumAloy(...)` | `DECOMPOSABLE` + `CENTRIFUGE` |
| Lazurite | `elec` | `DECOMPOSABLE` + `ELECTROLYSER` |
| Saltwater | `lqudelec` | `DECOMPOSABLE` + `ELECTROLYSER` |
| Water (`H2O`) | `lquddcmp` | 仅 `DECOMPOSABLE`；水的电解仍由 `GTChemGen` 显式行提供 |
| Hydrated Coal | `mixdust` → `dustcent` | `DECOMPOSABLE` + `CENTRIFUGE` |

## 验证状态

- `materialBehaviorContracts` 增加 Alumina、Rutile、Bronze、Lazurite、Saltwater、Water、Hydrated Coal 的标签断言。
- 2026-10-23 在本机执行 `:core:check` 与两个平台 build，全部通过；材料合约 7966 条断言，日志在 `work/build-material-decomposition-20261023.log`。完整哈希、环境和未覆盖项见[验证回执](verification/material-decomposition-alignment-20261023.json)。
- 该回执只证明编译、共享行为合约和两端构建；客户端、专服、玩法链、运行期最终配方表和旧存档重启仍未验证。
