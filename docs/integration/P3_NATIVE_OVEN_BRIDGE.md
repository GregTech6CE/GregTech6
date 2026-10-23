# 双版本初期对齐：原版熔炉与 GT 烤箱桥接

参照既有阶段 MD 和当前 Forge Loader_OvenRecipes/RuntimeRecipeLifecycle，补 Native 运行时缺失入口。原版已加载 smelting 行接入 GT Furnace 表，缺少原版熔炼入口的 GT Furnace 行反向进入原版 RecipeManager。复用同一材料身份、GT 机器表和既有配方包，不另设底层或新注册内容。

完整 loader 适配 RecipeHolder 的真实 id/value、Native cooking 构造和 registry-aware ItemStack 序列化排序，空堆排序不调用严格序列化。原配方保留 group、ingredient、类别、经验与时间；相同id首次优先。GT表中本桥接新增/暂替条目分别按对象身份跟踪，清理只移除自己镜像并恢复原行与索引，不清空独立注册的表。

新增 OvenRecipeLifecycle 只处理烤箱桥接：server started HIGH，全局 datapack sync LOW 在轨道替换之后、提示索引之前，server stopped 清理。跳过单玩家登录 sync。用实际 RecipeHolder 身份集合判断重新加载，而不是只记 RecipeManager 对象，避免复用 manager 时遗漏；实际 /reload、重复执行与客户端同步仍未验证。手工配方继续由已有 Native pack 加载，不复制 Forge 的整条运行时注入管线。

集中 Native compile+jar 与 Forge compile 39秒通过。一次既有 Neo 专服短启动2分3秒通过，200ticks 正常保存并停止。日志观察：原版→GT烤箱75行、GT→原版熔炉1825行、NEVER_FURNACE改写0行。前批新增材料转换26226（其中线尺寸1620、管尺寸80）、石材597（27岩型，132原规则跳过）、基岩花21行均运行生成；RecipeManager在桥接前加载34359条，无配方解析错误。计数不是逐条合成或熔炼完成证据；桥接后manager总数未独立枚举。

保留且明确记录当前双平台基准缺口：NEVER_FURNACE只识别MaterialItem，原版铁锭不会被这条门槛改写；因此未宣称原版铁矿必然产废料，启动观察改写为0。Source firstStack把反向镜像的输入及输出数量都置1，未修复多产量熔炼。Source firstInput只镜像首个Ingredient变体，未覆盖所有标签成员。取舍属于当前版本对齐保留基准，后续需要两平台一致处理，不能用源码注释代替功能证据。

2 Native Java，Core/资源/Forge/三个来源未改。本批无新夹具，无额外客户端重复启动。实际制作/熔炼数量与余物、客户端、重载幂等、生产jar运行、完整生存流程、独立进程世界重载及旧档待验；其他系统的双版本对齐及完整三源整合仍未完成。
