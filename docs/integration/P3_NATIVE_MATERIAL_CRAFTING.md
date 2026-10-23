# 双版本初期对齐：材料形态、石材与基岩花手工制作

沿用当前 Forge RuntimeRecipeLifecycle 的配方族，核实 Native 37工具/工具头/组装与打火石专用解码已在 ManualToolRecipePack/GTToolRecipeSerializers 接入，不再重复迁入。缺失的三类完整 builder 进入同一 pack，不复制第二套 RecipeManager 注入生命周期；原数据包覆盖规则保留。

Loader_FormConversionCraftingRecipes 保留全部 ONE_TO_MANY/MANY_TO_MANY 表、原顺序/缺失形态跳过/反物质排除/明确列出的竞争或缺前缀行，以及GT输入与 MaterialUnification.canonical 输出。块/锭/粒/坯/粉各尺度数量、密集矿→2原矿、所有原 wire size 整倍数小于10的组合/拆分、四合一液管→4中管与九合一→9小管，均查询真实 Native 注册。DenseOreBlock 查 GTSpecialOreBlocks 的实际集合；FluidTransportRegistries 持有原液管。源表既有竞争输出与跳过决策保留，未声称重设计或补齐 GT6 缺失前缀。

Loader_StoneCraftingRecipes 保留全部原石型/变体图样、齿轮/岩石/鹅卵石/阶梯/墙/半砖、雕刻/裂纹/红石砖、瓷砖互换和藤蔓苔化，以及原 drill/itemMoss 缺口记录。实际 GTBlocks 的432石型变体及对应半砖查询复用，不新注册内容。Loader_BedrockFlowerCraftingRecipes 保留A/B花染粉、Acacia/Palm木棒和锯/刀切出2棒的原条件/身份/数量；工具仍经 ToolShapedRecipe 磨损。

OriginalCraftingJson 是新增的薄 Native JSON 输出边界，编码原 Ingredient、普通输出、group/category及镜像字段，模拟 Source RuntimeRecipeLifecycle 的相同id首次条目优先。当前Source输出由普通注册物品构造；若未来出现组件输出会明确失败，不静默丢数据。1.21 vanilla shaped codec会修剪外缘空行/空列以对应Native normalized CraftingInput；部分 Source石材图样的空白行位置限制并非逐格等同，记录此适配差异，未添加全局网格Mixin。

5 Native Java（3完整loader+1输出边界+既有pack接线），Core/资源/Forge/来源未改。集中compile+Native jar/Forge compile40秒通过。仅核本批当前编译类与成品jar，无新增夹具或本批重复启动。前批专服证明旧pack基础设施可加载，不能代替本批大量新配方的生成/解析/耗时、真实匹配/数量/余物/镜像、客户端/JEI/index/reload检查；留到随后集中短检查。

成品jar、完整生存流程、独立世界保存重载和旧档兼容仍待验，其他剩余配方族/两版本对齐及完整三来源整体核对继续。

后续集中启动更新：P3_NATIVE_OVEN_BRIDGE.md 已记录本批三类实际运行生成/配方解析与短专服结果；原批次compile-only范围保留，逐条合成仍未验证。
