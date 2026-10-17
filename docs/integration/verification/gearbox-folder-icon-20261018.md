# 变速箱分类图标空白：源码修正，未构建

后续纠正：新截图确认实际异常家族是 `rotation_transformer` 普通变速箱；这里针对 `gearbox` 可调变速箱的绘制回退未解决根因，现已删除。见[普通变速箱根因记录](transformer-gearbox-clothing-20261018.md)。下文保留为历史排查记录，不代表修复成功。

用户确认：最新 1.21.1 NeoForge 成品中，空白位置是 Reliable EMI 左侧减号旁的分类图标；展开后的材料变速箱本身可以显示。此前 13 个物品模型、78 个面及 alpha 检查不覆盖分类封面的绘制，不能作为此问题已解决的证据。

## 修改

- Forge 与 NeoForge 增加可选客户端 mixin，仅让 `gregtech:material_forms/functional_gearbox` 分类返回 `isUnbatchable=true`。EMI 因而调用普通 `render`，绕过该分类三个重叠图标的批量绘制。其他分类与成员物品的绘制不变。
- 分类成员直接取共享 `GearboxCatalog` 的 13 种正式材料，删除从旧译名混入的无效 `gregtech:gearbox`。REMI 封面使用实际成员，不能把这个无效 ID 单独认定为图标空白的根因。
- 后续 NeoForge 分组探针增加分类成员数及普通绘制路径检查；该检查本身也不等同于像素或人工视觉验收。
- 不修改变速箱贴图、物品模型或世界渲染。

## 证据与边界

只读检查本地 REMI 4.7.12、EMI 1.1.24 字节码：分类 ID 保留 `material_forms/functional_gearbox` 路径；`EmiGroupStack` 有独立批量绘制；`StackBatcher` 在 `isUnbatchable=true` 时调用普通绘制。当前兼容修正绕过疑似异常路径，底层导致空白的具体渲染状态尚未实机定位。

`generate_remi_material_groups.py --check` 和 `git diff --check` 通过。按用户“先修这个不构建”的要求，本批未编译、未打包、未启动客户端；现有成品不包含本批修改。

下一次允许构建后，在 NeoForge 最新 REMI 中核对：空搜索及 `@gregtech` 搜索时，变速箱分类折叠／展开后的左侧图标可见，13 种材料仍可展开查看；再核对 Forge。若仍空白，需要采集分类普通绘制的实际画面和渲染状态。
