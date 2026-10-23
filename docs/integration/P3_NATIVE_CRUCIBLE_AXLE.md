# 双版本初期对齐：坩埚提示、轴制作与配方重载索引

依据既有 MD 和实际 Forge 接线继续 D011。原 P3_NATIVE_USB_RELAYS_AUTO.md 中“手工站/砧未移植”的旧缺口已由 P3_NATIVE_MANUAL_UTILITY.md 的真实 PoweredToolTarget/usePoweredHammer 实现覆盖，本批不重复移植自动锤。

CrucibleTooltips 完整保留原普通/高级提示：能量转换、热质量、熔毁上限、炼钢、耐酸、火灾与接触伤害、温度计、铲空、爆炸抗性、采掘、化学式、壳体材料数量/质量/熔沸点。SmeltingCrucibleBlockItem 使用 1.21 TooltipContext，全部 39 个现有坩埚注册采用它，身份和堆叠大小不变；已有 hull/mold 提示不另替换。

原 Loader_HandToolCraftingRecipes.axles 的 13 材料×4 尺寸共 52 行进入既有 ManualToolRecipePack，保持 gregtech:hand/axle/<spec.id>、gt.hand、原非镜像图样、成品数量、文件/锤磨损及棒/长棒/多锭选择。棒标签保留 Forge 并兼容 Native c 名称，精确材料成品作为回退；大号处理木轴为 rS/Bf，实际 wooden_beams 加一个有限杂酚油容器。

CreosoteAxleRecipe 采用原有限容器匹配/余物流程，适配 Native ICustomIngredient、MapCodec 和 registry-friendly stream codec，例示 tin cell 仅在需要时生成。检查真实 drain 前后减少 1000 mB，排除流体显示项、带显示载荷及无限处理器；返还同一物理容器，保留 ToolShapedRecipe 实际工具磨损。不引入第二套容器储存。

Native 已有配方数据包处理 reload 与覆盖顺序，未照搬 Forge RuntimeRecipeLifecycle 的整套手动替换管线。只补 ShapelessRecipeTooltipIndexLoader 的全局 OnDatapackSyncEvent LOWEST 刷新，使用真实 server.registryAccess()；单玩家同步跳过，避免每次加入重复全量扫描。

7 Java 文件，Core/资源/Forge/来源项目未改。集中 Native compile+jar/Forge compile 通过；随后只跑一次既有 Neo 专服短启动，日志观察 52 行生成，配方加载、200 tick、正常保存/停止通过。无新增专项夹具。编译与启动不证明实际容器合成、工具磨损、提示画面、索引 reload 内容、成品 jar、完整玩法、独立世界重载或旧存档；均待集中验收。整体移植仍未完成。
