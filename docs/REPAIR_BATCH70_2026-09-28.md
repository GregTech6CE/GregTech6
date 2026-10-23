# 第七十批：回查原始方块目标——铸币模具

## 核对结果

最初的板锭/硬币/书架仍保持完整目标。当前源码已存在 CoinItemRenderer、CoinPileRenderer、CoinGeometry 自定义币面、BookShelfVariants/Renderer 及各族测试；历史备注不能据此重复新增。客户端像素仍无本轮运行验收。

本轮找到铸币模具的实际缺口：不能手动取回未锤击的小板；只有空壳模型，没有随内容变化的金属插入物；选框包含 14/16 的凸起，而 GT6 的选框/碰撞均为 12/16。

## 原版依据与修复

MultiTileEntityMoldCoinage.onBlockActivated3 允许顶面/侧面取出任何 slot(0)，包括小板；canExtractItem2 则禁止自动化提取 plateTiny。新增 takeContents 用于手动取出，自动化规则保持。

原版 setBlockBounds2 pass2 内容边界为 [5,11,5]→[11,13,11]（像素），显示材质来自库存物品。新增 CoinMoldGeometry/CoinMoldRenderer、客户端注册，按材料 blocksolid 纹理和颜色画内容，叠加存在的原色 overlay；复用现有库存更新 tag，同步插板、铸币和清空。

补模型原版 pass3–6 的四个内壁朝内面，更新 rebuild_container_tool_models.py，定向重生成 coin_mold 模型，避免重写其它工具资源。动态内容不放进空模具物品预览。getShape/getCollisionShape 均为 12/16 高，与原版独立于可视凸起。

方块交互补 mayBuild/mayInteract。满背包不清除内容，底面不取出。原版锤击内部虽检查 remainingDurability>=2000，但 Behavior_Tool 调用传 Long.MAX_VALUE，不能误判为实际剩余耐久门槛，本轮未臆改。原版图案选择提示明确 Doesn't work right now，因此未凭空设计编辑界面。

## 验证

新增 CoinMoldCompletionTests 两项：手动取回小板与自动化限制、底面/权限/满包拒绝；原版选框/碰撞/内容边界、插板→铸币→清空的材质更新 tag、六个静态模型部分与四个单面内壁。

新世界 build/gametest-batch70-coin-mold，日志 build/gametest_batch70_coin_mold.log：15:55:11 All 1038 required tests passed，15:55:40 正常关闭，退出码 0，BUILD SUCCESSFUL（9m19s）。运行期间 Java/资源冻结。audit_porting_progress.py 已刷新 docs/porting-progress.json。

## 客户端验收

放入铜/铁等不同小板，观察模具中金属的贴图与颜色；锤成币后仍显示同材质，取出后清空。未加工小板可直接右键取回。顶部凸起可视，但选框/碰撞遵循 GT6 的 12/16。检查内壁纹理和照明。本轮服务器测试不能替代这项 GPU 渲染验收。

原始目标的所有占位功能与客户端验收仍未全部完成；电锯连伐、工具配方替代等也继续留在待办。没有执行 git commit。

## 测试包交付与暂停

要求本轮结束后打包并暂停下一轮。已执行 gradlew.bat build --offline --console=plain，退出码 0，BUILD SUCCESSFUL（5m17s），含 reobfJar 和 test/check。构建日志：build/build_batch70.log。

- Jar：`F:\Dev\GregTech6\GregTech6\build\libs\gregtech-1.0.0.jar`
- 大小：37,801,661 字节；80,432 个 ZIP 条目。
- SHA256：`19d3efc643b740fc0e416d5b8dc4088ae7e87996a77ba64d3d9ac25942fdaae6`
- ZIP 完整性通过；新模具渲染/几何、木工放置、电锯采掘类均在包内；模具模型、工作台标签、中英文语言资源与当前源码逐字节一致。
- 确认最新 Java 修改早于成功门禁结束，门禁早于 jar；验证记录 build/jar_batch70_verification.json。

### 本次一起验收的重点

1. 铸币模具：铜/铁小板放入后的内容物颜色与模型，锤击后保持材质，未锤击可手动取回，满包不丢失，取出后清空。
2. 电动扳手：有电生存拆机，机器本体与内部库存正确回收；潜行点击无方块实体目标切换普通/活动模式；空电不操作机器。
3. 电锯：树叶、普通冰、浮冰的掉落，冰位置不留下水；耗电与工具材料寿命。连伐仍未完成，不作为本包已实现功能。
4. 斧/双头斧/手锯/电锯：背包中有树苗或工作台时右键放置；树苗仅顶面，工作台拒绝木材/植物目标，创造不扣物品。
5. 管道、电线、传动轴所在区块加载与光照更新是否顺畅。服务器线程安全回归已通过，客户端视觉仍需实测。
6. 此前反馈的大型坩埚：成型后的特殊模型、Jade 内容显示；本轮未运行客户端，不能把此前未验收项写成已验证通过。

交付后暂停，不启动第七十一批。完整移植目标仍未完成；恢复时参考本报告与最近批次报告，不把历史待办直接当作当前源码缺失。
