# 双版本初期对齐：原注册图样手工制作入口

后续更新：storage制作入口已由 P3_NATIVE_STORAGE_CRAFTING.md 补齐；本报告保留原批次检查范围，本批与存储批的1048行生成及专服加载结果见该记录。

参考 P3_GEARBOXES_AND_TOOL_CATALOGS.md、P3_NATIVE_PUMPS_GRAAGG_ICONS.md、P3_ITEM_PIPES_AND_HOPPERS.md 等既有阶段及当前 Forge Loader_HandToolCraftingRecipes 的真实流程，补 Native 已注册方块的制作入口。此前迁入的 52 条轴配方保留，仅在同一 ManualToolRecipePack 调用 OriginalHandCraftingRows；没有另复制注册器或 Forge RecipeManager 替换生命周期。

本批完整迁入 10 个原函数族及其解析帮助方法：13 齿轮箱、13 旋转变压器、13 RU→KU 旋转发动机、4 旋转泵、电线、液体/物品/限制型管道、砧、7 材质栏杆、晶体处理器插座与5种空电池壳。以上为源规则/枚举范围，运行时实际生成数量本批尚未观察。当前 Forge 手工 loader 的轴已在前批接入，storage 家族仍待注册与标签适配，不能称全部手工入口完成。

保留原图样、gregtech:hand/<path> 身份、gt.hand 分组、源材料形态/数量、条件跳过、同材质中轴/齿轮/机壳与不锈钢转子/管尺寸。木/早期 Bronze/Brass/ArsenicCopper/ArsenicBronze 接原九种油瓶润滑等价，其余只允许专用润滑瓶；容器余物仍由原物品实现负责。

复用 ToolShapedRecipe 的真实工具磨损与非镜像规则，栏杆依源 allowMirror=true 且产出3；tiny液管产出2，huge管用双板，限制管保留原普通液管+钢环图样。砧保留原 vanilla/石种石块/金属锭选择；电池壳与处理器保留原技术物品和化学材料条件，不增加任意 vanilla 替代配方。

Native 边界对应实际 GTEngines ROTATION、FluidTransportRegistries.pipes、GTManualStations.ANVILS、GTBars 等现有身份；Ingredient.CODEC 编码原 Ingredient。原程序式网格允许多余工具 key，输出 JSON 只保留图样真正用到的 key，以符合 1.21 ShapedRecipe codec，图样和匹配行为不变。数据包保持既有 BOTTOM 与覆盖规则，重复 recipe id 显式报错。

只改两个 Native Java 文件，Core/资源/Forge/三个来源不变。第一组 compile+jar 31秒通过；补技术物品后最终 compile+jar 23秒通过，当前本批编译类和实际成品 jar 字节一致。本批不重复客户端/专服，不加专项夹具。上一批专服通过证明既有 Native 配方基础设施可启动，不能代替本批新生成逻辑的启动/数量/真实制作证据。

待后续集中验证：本批运行时生成/codec解析、实际输入匹配/工具磨损/油瓶余物/镜像和生存链、客户端配方展示、datapack重载、成品jar、独立保存重载/旧档。整体移植仍未完成。
