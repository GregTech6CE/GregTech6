# 第七十一批：大型坩埚外表面纹理方向

## 反馈与复现

反馈大型坩埚外表面错误旋转，随后要求继续项目。先处理当前可复现故障。

把 LargeCrucibleRenderer 原有顶点发射抽为 LargeCrucibleHullFace，在不改变旧 UV 算法时，用 VertexConsumer 捕获真实六面顶点。tools/check_large_crucible_uv.py 在旧实现上失败（build/crucible_uv_before.log）：南/西面顶端与底端使用了横向 UV，造成 90 度旋转；北/东面有水平镜像，顶/底也未遵循各自平面轴。

排查顺序：各面顶点顺序与统一索引 UV 不匹配；主控朝向导致重复旋转；源贴图方向错误。最终首项得到顶点证据。主控朝向只选 front 纹理，不改变锅体 PoseStack；12 张 colored/overlay 与 front 贴图逐字节等同 GT6 原版。

## 修改

- LargeCrucibleRenderer 调用独立锅体面发射方法。
- LargeCrucibleHullFace 按每面空间坐标计算 UV：侧面 V 随高度向下，水平 U 按从外部观察的左右方向；顶面 X/Z，底面 X/反 Z。以每个墙面的范围归一化，保留三格锅体原有纹理覆盖范围。
- 彩色底层与 overlay 走同一路径；保留绕序、法线、主控 front 选择、材料色、熔毁警示色和光照。
- 新增 LargeCrucibleUvContracts 与独立运行脚本。测试捕获生产代码发出的 UV，六面使用空间轴断言，不复制实现的顶点索引表。

## 验证

修复后 tools/check_large_crucible_uv.py 退出码 0；日志 build/crucible_uv_after.log。这是 GPU 之前的实际顶点数据验证，并非客户端截图验收。

全量服务器回归使用新世界 build/gametest-batch71-crucible-uv，日志 build/gametest_batch71_crucible_uv.log，09:59:24 全部 1038 项必需测试通过，09:59:56 正常关闭，退出码 0，BUILD SUCCESSFUL（10m8s）。进度审计已刷新 docs/porting-progress.json。

## 玩家验收

完全重启 IDEA runClient；成型大型坩埚后，从东南西北绕行观察锅体，纹理应竖直、正面图案无镜像；改变主控朝向，front 贴图应跟随朝向且保持正立。再检查顶部边缘、内壁与熔融液面的显示。

本轮未重新构建 jar，9 月 28 日交付的 jar 不包含这次修复。不要用旧 jar 判定这次修改是否生效。

## 后续移植

第 70 批前记录的电锯连伐、树高/梁采掘速度、工具配方替代及携带电池补能仍未宣称完成。本次原版源码复核确认：GT_Tool_Axe 连伐只沿同一 X/Z 向上连续同种方块，并不是任意相邻木头泛洪；潜行关闭，兼容树木模组时跳过。后续应同时实现保护事件、递增损耗和对应速度规则，不能单独把连锁破坏加上就算完整移植。

没有执行 git commit。
