# 存储方块的覆盖板边界核查

第四十五批，2026-10-20，基线 `b639b9b93`。对照原版 Mass Storage、Quad Drawer、通用覆盖板与坐标规则，追踪当前两版真实事件、Mixin、安装、按钮和渲染入口。

## ALIGN-138：大容量存储遗漏正面覆盖板阻挡

原版 `MultiTileEntityMassStorage:243` 在正面有任何覆盖板时拒绝普通取放操作；`isFaceVisible:507–509` 另按覆盖板是否不透明决定显示库存正面。两者门槛不同，不能把不透明判断用于代替所有交互阻挡。原版普通 `CoverTextureSimple` 继承默认不透明属性。

当前 `MassStorageBlock` 两版都只检查封装、朝向和像素范围，没有检查正面覆盖板。统一事件并非兜底阻挡：有权限的玩家空手点击普通装饰面板时，组件处理不消费，工具处理不匹配，过滤器／工作台专用处理不匹配，`PanelCoverRuntime.click` 对非交互面板返回 false；最终进入宿主取放代码。因此例如正面取物按钮仍能抽出内容，这是源码调用链推导，未游戏复现。

当前 `MassStorageRenderer` 仅在已封装时退出，没有原版不透明覆盖板可见性门控，仍提交物品／数量绘制。这里确认的是缺少宿主门控，不把提交绘制直接描述成每种显卡、贴图深度下必定透出，也没有进行客户端画面验收。

## ALIGN-139：四格抽屉错误允许正面安装覆盖板

原版 `MultiTileEntityDrawerQuad:119` 明确 `allowCovers(side) = side != facing`，正面留给四个 GUI 区域。

当前抽屉实体由通用 Mixin 提供覆盖板；`ComponentCoverFallback.eligible` 认可其实体方块，`ComponentCoverStorage.attach` 调用通用 `PanelCoverRuntime.canAttach`。普通装饰面板最终只检查是否为覆盖板，没有抽屉正面排除；方块／实体本身也没有补该排除。当前正面朝上／朝下的抽屉同样进入此路径。

这不是全模组都不应在正面贴板，也不应通过禁止所有水平面来修复。原版大容量存储允许贴板后阻止取放，与抽屉禁止安装是两份不同的宿主合同；其他容器仍须逐项核对。

## 已有对应与边界

统一事件和工具／面板入口已有 `mayBuild`、`mayInteract` 检查，普通装饰面板的穿透路径仅讨论有操作权限的玩家，不能将其描述成绕过世界保护。实际平台事件／Mixin 生效及全部 GUI 只读权限仍待游戏验收。

四格抽屉已有 4×36 槽、正面象限开页、活动扳手切换侧面访问和 10000 返回值损耗、能力失效重建；不会因覆盖板错误把这些功能全部列为缺失。共享六向旋转表与原版 `CS.FACING_ROTATIONS` 的六个有效方向逐元素一致；原版额外的无效方向哨兵行列没有映射到现代 `Direction`。

## 代码整理与验证

展开 `ContainerStorageRules` 的压行常量／开关／坐标公式及 `ComponentCoverStorage` 的状态操作；纠正后者和 `MassStorageRenderer` 的说明。三份文件共五份 core／双版源码投影保持执行词法和字面量，容器规则的原有注释内容也保留。没有更改安装策略、权限、渲染或库存行为。

来源哈希、旋转表对照和等价结果见[回执](verification/storage-cover-boundary-audit-20261020.json)。本批没有新增测试、编译、GameTest、客户端、重启、构建或推送；继续保险箱、衣柜和其他库存合同，全面目标未完成。
