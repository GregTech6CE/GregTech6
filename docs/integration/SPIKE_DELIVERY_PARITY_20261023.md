# Spike 家族交付对齐（2026-10-23，fixed 验收待回填）

## 范围与判断

用户报告的“像测试品、材质少、缺 falling/spike wall”有真实交付缺口，但不能据此判定底层 Spike 全无实现，更不能据此重写矿物处理。

原版五族、十材质及 wall/omni/falling 语义此前已存在于移植的 SpikeBlock。缺口集中在创意物品枚举、状态名称、物品模型、falling 正常制作入口。本批保留当前注册身份与方块几何，补齐这些交付层；整体移植仍未完成。

## 原版身份：不是 MTE 数字 ID

原版 Loader_Blocks:94–98 → BlockBase → ST.register/GameRegistry 注册的是五个普通 Block：

| 原版注册路径 | 保留的当前 block/item ID | 主材质（meta0–7） | 副材质（meta8–15） |
|---|---|---|---|
| gt.block.spikes.sharp | gt6ce:spike_sharp | Steel | Titanium |
| gt.block.spikes.steel | gt6ce:spike_steel | Blue Steel | Red Steel |
| gt.block.spikes.super | gt6ce:spike_super | Tungstensteel | Adamantium |
| gt.block.spikes.metal | gt6ce:spike_metal | Copper | Lead |
| gt.block.spikes.fancy | gt6ce:spike_fancy | Gold | Silver |

普通 Steel 在 sharp，不在 steel；原版 BlockSpikeSteel 构造器特意说明此点。没有应照搬为现代 resource ID 的固定 MTE 编号。旧版全局 numeric block ID 依赖注册/世界映射，不能臆造。

每族 meta0–5、8–13 对应六向 WALL；6、14 为 OMNI；7、15 为 FALLING。高位区分两材质。原版创意/物品形态为0、6、7、8、14、15：**五族×两材质×三形态＝30个物品身份**，不是只有30个世界朝向状态。原版有80个有意义的 metadata 世界状态；现代 mode/secondary/facing 的笛卡尔空间另有 omni/falling 的冗余 facing。

当前五个注册 ID、mode/secondary/facing 名称和值、裸物品默认 primary WALL、方块默认 OMNI 均保持。新 SpikeBlockItem 只替换同一已注册 item 的实现；不改变已存 block/item ID，不把 Blue Steel 偷换为普通 Steel。

## 本批交付改动

- 创意页由五个裸物品扩展为30个状态物品；物品名称按原版 family+metadata 选择。
- wall 物品模型使用既有 wall 模型；omni/falling 使用既有 omni 模型。双版注册 mode predicate，沿用已有材料 tint；不重绘/替换原几何。
- 新增十材质各两方向，共20条 OMNI↔FALLING 无序转换。原有20条 wall/omni 有序制作配方保留。
- 专用 `gt6ce:spike_conversion` serializer：单个占用输入槽、同注册物品、同 secondary，只允许 OMNI↔FALLING，拒绝 WALL、同mode、另一材质/族和额外输入。不能用只匹配 item ID 的普通无序配方代替。
- Forge 与 Neo 分别在既有 serializer 注册入口注册新类型；**既有 ToolShapelessRecipe 源码和网络字段未改动**。Forge 委托 vanilla shapeless payload；Neo 用独立 MapCodec/StreamCodec。20条 recipe resource ID 固定，Neo仍通过既有 PortableCraftingRecipePack 将结果 BlockStateTag 转成 native block_state component。
- 补原版 falling 方块加入时的铁砧声调用；声音/真实放置生命周期尚未完成运行验证，不能因代码存在就宣称验收通过。

## 语言来源与策略

30个新英文键逐字取自原版五个 BlockSpike 构造器，metadata0/6/7/8/14/15。对应30个中文源键早已存在，已核对等于原始中文源。现有五条 `block.gt6ce.spike_* → gt.block.spikes.*.0` alias 不变；直接使用同名源键无需新 alias，也不允许用 alias 覆盖原始键。

同时补 tools/localization/english_source.json 的30项固定声明。五个源类文件哈希与该文件既有 source_files 条目相符。全量 check_english（候选英文＋源中文＋现有aliases＋新pins）已通过；客户端 EN/ZH 可视验收仍待做。

## 真实 baseline 与测试覆盖界限

API-neutral 两项共享测试先于生产修复进入基线，不依赖新 SpikeBlockItem/serializer 类导入，不注入配方。它们通过现有 SpikeBlock 原生 pick-block 生成状态物品，通过真实 RecipeManager 取配方。

| 平台 | 执行 | 通过 | 失败 |
|---|---:|---:|---:|
| Forge baseline | 260 | 258 | 2 |
| Neo baseline | 251 | 249 | 2 |
| 合计 | 511 | 507 | 4 |

所有既有507项通过；新增两项在每版各失败一次，原因一致：

1. `thirtysourcevariantitems` → `source subtype name`：当前普通 BlockItem 使用固定 family 主材wall名称，未到达后面的30项创意计数断言。因此不能说 baseline 已独立验证全部创意数量。
2. `twentyconversionsmatchonlytheirsourcevariant` → `No value present`：row() 中 RecipeManager.byKey 的 Optional 为空；首个请求为 `gt6ce:catalog_repairs/spike_metal_copper_convert_to_omni`。生产无此资源/目录项；不是缺测试模板或编译依赖。日志/XML只留消息，未保留Java调用栈；归因依据唯一throw入口与独立资源缺失证据。

**baseline 在首项缺口处停止，600对矩阵未执行到。** 固定版全511原生门禁已通过：Forge260/260、Neo251/251。不得把候选 cached javac 或启动成功当作 fixed 游戏通过。

fixed 第二项测试实际覆盖每版：20条真实配方×30种输入＝600对匹配（应20成功），另600对网络往返后匹配；一格64件输入、额外格/裸物品拒绝、输出数量1/材料/形态正确。recipe assemble 不负责扣输入，测试不伪造“已验证实际制作扣料”。第一项覆盖30种原生状态/源名称和实际创意枚举。

## 发布前与后续仍需区分

1. fixed 门禁：保留全部507旧身份，新两共享身份使 Forge260、Neo251，总511；检查实际XML每个身份、无failure/error/skipped。最终实际XML全部通过，无失败/错误/跳过。
2. 材质采掘等级仍开放。原版按metadata材质 mToolQuality；目前block级tag不能安全表达secondary差异。未用“整族最高等级”掩盖问题。
3. 真实 BlockItem 放置 FALLING→安排tick→下落/落地/失败掉落/声音仍待原生测试。不能只用直接setBlock的falling状态代替；初始 placement WALL 与后续item state overlay时机必须验证。
4. 实际制作菜单取出结果的消耗/63件余量、实体物品/箱内状态、备份世界重启仍待验证。跨Forge/Neo世界迁移不在此批承诺内。
5. 无GPU截图或客户端EN/ZH GUI/手持/落地模型验收；“模型/predicate存在”不等于视觉通过。EMI/JEI输入展示仍可能只给裸族物品，需要独立adapter；不能因此放宽真实recipe匹配。
6. 原版optional实体兼容、falling卸载/落地行为和材料采掘属于后续有边界的功能核对；本批不承诺完整Spike行为/整个GT6 parity。

来源：原版 GregTech6 原有版权/许可声明保持；源码快照、文件哈希、基线XML与代码工件见随附 `spike-delivery-provenance.json`。本稿在ignored目录准备，未于运行门禁期间修改tracked文档或生产代码。

## 最终本地验证

完整511项通过；双版本构建/core、139项Python检查、491组REMI、每端1028共享打包类、SDK112核心/46原生类通过。输入/终态快照一致；回执 work/validation/spike-delivery-fixed/local-validation-receipt.md。远程CI另行验证。上述后续边界不因本批通过而取消。
