# 多方块 JEI 预览消失修复

## 原因与修改

`StructurePreview` 曾直接用配方局部坐标 `(0,20)-(176,128)` 调用 `GuiGraphics.enableScissor`。Minecraft 1.20.1 的此 API 不读取 PoseStack，它需要屏幕 GUI 坐标，而 JEI 的 `RecipeLayout.drawRecipe` 在调用分类绘制前已经平移到配方所在位置。

例如配方位于 `(240,100)` 时，预览中心在 `(328,177)`，裁剪框仍停在屏幕左上角，整个模型会被裁掉。该错误与材质是否存在无关。

新增 `PreviewViewport` 统一声明预览区域，在模型相机变换之前用当前 GUI 矩阵转换四个角，再计算屏幕裁剪边界。缩放与镜像使用四角包围盒，边界向外取整。窗口 GUI scale 仍由 Minecraft 应用，避免重复缩放。鼠标操作区域也引用同一组局部边界；原有拖拽旋转、右键平移、滚轮缩放和分层保留。

## GTM 参考范围

检查了提供的 GTM `MultiblockInfoJeiCategory`、`MultiblockPreviewWidget` 和 `MultiblockSchemaInfo`：它把结构数据、相机和交互放入 ModularUI 的 SchemaRenderer/SchemaWidget，而 JEI 分类负责嵌入控件和提供材料清单。本次参考其将预览区域独立管理的思路，没有复制 GTM 或 ModularUI 代码，也没有引入这些依赖。

## 验证

- `python tools/check_preview_viewport.py` 直接编译并调用生产代码中的裁剪计算，在旧逻辑下稳定失败，报“JEI recipe offset clips the whole structure”；修复后通过。
- 覆盖无偏移、JEI 页面偏移、第二行配方、非整数缩放、镜像与旋转矩阵，使用项目 JDK 17。
- Java 编译通过。该问题属于 GUI 坐标错误，没有改变服务端机器行为或资源文件，因此没有用服务端 GameTest 冒充渲染验证。
- 尚未在真实客户端执行 GPU/鼠标验收；坐标测试证明了裁剪故障与修正，但不能排除其他客户端渲染问题。

## 进游戏检查

1. 重新启动 IDEA `runClient`，打开任意多方块的 JEI 装配页，检查模型是否恢复。
2. 分别检查锅炉、大型物质制造机、聚变反应堆，切换 GUI 缩放或窗口大小后再次打开。
3. 左键拖动旋转、右键拖动平移、滚轮缩放；放大后模型应只在预览框内显示，不盖住按钮或材料栏。
4. 检查分层、All、R 和翻页后预览是否正常，其他 JEI 配方页应不受裁剪状态影响。

## 构建产物

`build -x processResources --offline --console=plain` 成功（59s）；本次没有资源修改，沿用前一轮已处理的资源。

成品：`build/libs/gregtech-1.0.0.jar`；SHA-256：`eeddf89856218ee8d36bbfa520b76295faeb553c873c112a0753dc08fe110fd5`。已确认新裁剪实现进入 JAR。
