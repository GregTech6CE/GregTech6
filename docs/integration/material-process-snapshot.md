# 材料处理快照与共享图验证

## 范围与状态

本页记录材料来源／用途快照、core 图及 viewer 投影的代码核对与回归方法。状态：部分验证；不能据此把[计划 08](../development/material-system-refactor/08-material-data-api.md)整体标为完成。

源码依据：`de16ac298`、`8722ac629` 与其上的工作区修复。当前实现增加 API 1.9 的 cross-material special-process 声明、target／reagent typed rules、派生标签、通用配方 producer 和 method-aware 展示折叠；没有修改注册 ID、存档或网络协议。

## 已确认问题与处理

| 问题 | 影响与处理 |
| --- | --- |
| overlay 与炉桥接各自构建快照 | 中间行集被提前发布并重复消耗构建时间。服务端改在 `RecipePublication` 提交后发布，独立客户端在 overlay／炉镜像后发布；完整上下文未变时复用索引。失败 overlay 不替换成功快照。 |
| 输入 tag 被用于同槽号输出 | 物品或流体输出会变成输入 tag 的代表。输出只读取自身原生身份，输入的 tag、unit 和取整规则保持独立。 |
| 世界生成物品使用驼峰转下划线名称，查找失败回退石头 | 与实际生成器的小写注册名不符，会把矿物来源画成石头。按实际注册名查找；未注册矿物不作为有效生成来源。 |
| 世界生成端口写死 `ore` | 与注册物品的 `oreVanillastone`、`oreDense` 等形态无法连接。改从原生物品解析真实形态，小矿单独保留身份；优先选择普通矿物而非小矿作为总览根。 |
| 世界生成 revision 漏字段并使用含材料对象的哈希 | 权重或种类改变不能使 revision 改变，跨 JVM 的对象哈希也不表达相同事实。按完整事实字段及稳定材料名计算 revision。 |
| 仅在根材料分桶内遍历 | 分解、合金和副产物进入另一材料后路线停止。改为一个跨材料索引，查询仍受同一深度、步骤、端口和边预算限制。 |
| 预算裁剪后仍输出关联 | 关联可能指向已裁掉的端口，导致展示构造拒绝整个查询。返回边只引用保留下来的端口；深度边界和非展开侧的端口裁剪也报告部分覆盖。根候选不先复制全集再截断。 |
| 资源 `DataKey` 可跨快照复用 | 新代查询可能接受旧资源。资源引用加入每次构建的作用域，原生导航仍校验当前快照；服务器停止和客户端断线清理 Store。 |
| 特殊系列依赖名称与手写生成器 | API 1.9 用 `SpecialProcessMethod` + `MaterialProcessKey<SpecialProcessValue>` 声明目标材料、辅料、配方表和展示名。Target 规则生成配方并携带 method ID；手工旧行也按同一规则匹配。材料图只折叠带 method ID 的步骤。 |
| target／reagent 标签与配方声明双写 | target／reagent 材料 tag 从 typed process 规则派生，item／fluid tag 再按规则表单生成，避免维护第二份名单。 |
| 普通无形态原料／产物被静默跳过 | 保留原生物品／流体端口及未分类说明，不把单一图标推断成某种材料；带额外原生条件的输入不与无条件资源合并。 |
| EMI 动态查询只显示第一页 | 来源／用途查询改为一种材料一张页；JEI 与 EMI 共用可平移图，不再按两个步骤拆页或维护配方级页码。 |
| 文档状态矛盾与两节点导入顺序错误 | 更新设计状态、人工验收步骤及包入口；修复 Spotless 报告的导入顺序。 |

模具、工具等辅助物品不扩展为总览资源路线，其数量、状态引用与原配方保留在步骤说明／原生配方中。这与普通消耗原料不能静默消失是两种情况。图不会因为工具没有材料成分而虚构一条工具生产链。

JEI 与 EMI 同装时，JEI 提前返回是已有桥接去重约定；本模组的 EMI 原生处理页已经注册。不能仅据 JEI 分支为空认定两 viewer 同装丢失处理页。实际双装交互仍须单独人工确认。

## 复现与检查

从仓库根目录运行：

```bash
./gradlew format -PspotlessRatchet=de16ac298^
./gradlew :core:test --tests gregtech.engine.material.MaterialProcessGraphTest spotlessCheck -PspotlessRatchet=de16ac298^ compileBothPlatforms :minecraft:1.20.1-forge:compileTestJava :minecraft:1.21.1-neoforge:compileTestJava
python tools/validation/route.py check
python tools/comments/check_comment_refs.py --diff HEAD
git diff --check
```

`MaterialProcessGraphTest` 包含循环、共同输入、前反向查询、各项预算、深度边界的端口截断及裁剪后的边合法性。展示投影、世界生成形态、物品／流体状态和布局示例不再保留逐场景 GameTest；这些行为按[人工检查](../development/validation/checks/manual-ingame.md#矿物处理展示页)使用真实 JEI／EMI 验收。

## 验证结果与剩余事项

以下 GameTest 结果是移除一次性展示快照测试前的历史证据，不能代表当前测试数。当前常驻的纯规则入口为 `MaterialProcessGraphTest`，客户端展示仍需人工验收。

- 历史执行且通过：`MaterialProcessGraphTest`、API `SpecialProcessValueTest`、`apiContracts`，以及清理前 Forge 99 项、NeoForge 104 项 GameTest。
- 尚未执行：独立远程客户端、JEI-only／EMI-only／双装实际拖拽、路线折叠、全屏、无 viewer 的独立客户端、附属差异与世界生成开关矩阵。

08 的 A—C 已有定义与形态入口；D—E 已有内部来源／用途快照、符号选择器、共享材料页图和 API 1.9 的显式特殊工艺所有权。tag／unit 仍保持符号投影，不能声称枚举了全部动态工艺或完整可执行路线；本轮不新增网络同步协议。远程同代数据、断线／换服、实际 viewer 拖拽／折叠与精确跳转仍需要按[人工检查](../development/validation/checks/manual-ingame.md#矿物处理展示页)记录实际结果。局部图模型通过不能替代这些场景。
