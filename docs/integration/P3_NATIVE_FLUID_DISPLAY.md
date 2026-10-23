# 双版本初期对齐：流体物品与原模型资源

本批按 D011 复用现有 Forge 实现及 docs/RENDERING_AND_MATERIAL_VALIDATION.md。上次客户端日志中 56036 个材料物品已成功映射到共享模型，0 个共享模型缺失；因此 5 万多 per-item JSON 初始加载警告不能直接当作最终材料物品显示失败，也没有用占位图替换。

Native 补回完整 FluidItemClientModels、FluidItemOverrideList、FluidItemBakedModel：注册共同 fluid_item 模型、所有既有流体身份库存模型别名、真实流体图集精灵及纹理缓存、UV 重新映射、实际组件载荷纹理/染色和重载时缓存清理。1.21.1 BakedQuad 的 AO 标记保留；不增加另一套流体身份。Sprite/材质在渲染时取当前图集，染色以实际流体组件及平台 client extension 为准。

FluidDisplayItem 适配 Source1 原名称/提示、魔法/反物质/药水闪光、每次250mB饮用并返玻璃瓶，以及原创意无限流体源 IFluidHandlerItem。能力由 NeoForge RegisterCapabilitiesEvent 绑定既有 item holders；物品能力无 client 类引用。组件流体解析从真实服务器 registry provider 或仅客户端 subscriber 注入的当前世界/连接 provider 取得，不用新材料/配方底层。没有 registry provider 的早期载荷显示退回原 metadata，后续实际载荷可解析；这一早期降级仍需实际 UI 检查。

处理客户端日志所指、Forge 实际存在的154个原模型/纹理/状态文件。113个跨平台文件原字节移入共享 core，包含木材水平模型、树叶、world fluid 16种液位、JSON依赖和共同 fluid_item 模型；仅移除相同的 integrated Forge 原副本及 hash 匹配的旧加工副本，避免重复打包。单次客户端启动指出另41个坩埚模具 blocksolid 使用 forge:composite，因此这41个文件保持原 Forge 路径和字节，Native独立分支只将 loader 改为 neoforge:composite。实际 NeoForge CompositeModel.Loader 读取相同 children/item_render_order；JSON对象比较证实只有loader变化。修复后不重复客户端启动，实际新加载器渲染结果待试用。三个 donor 目录不改，不造图或通配覆盖模型。另45个 gas dust/block/crate状态文件在现有Forge中也不存在，保留缺口。

集中双编译23秒通过；随后一次既有 Neo 主菜单启动/双版本打包和资源日志变化另记。没有新增专项夹具，主菜单不能证明实际流体 UI、外部流体组件、喝饮料、能力排液、F3+T、世界渲染和存档重载。完整玩法、独立重载、旧档、成品包启动及原三来源整合目标继续未完成。


2026-10-01 D011 流体物品/模型对齐：完整3 Native流体模型/override/注册类，原实际组件纹理/染色、名称/提示/闪光、250mB饮用和创意源流体能力接线；6 Java来源记录。154原资源中113原字节共享移动、41坩埚composite模型保留Forge原路径并单独Native仅换loader；原donor目录不改。集中编译23s；一次Neo主菜单+双包2m38s，149帧/1280x720截图观察通过，9世界流体144液位警告消失，block缺模型警告126降为41且查明为composite加载器。修复41loader后只重打包，无重复客户端；570当前core类和67611资源字节验包通过。56036材料物品均映射成功，57462初始item JSON警告未当作实际显示失败；45气体材料块状态缺口仍在。真实流体UI/JEI/HUD、饮用/能力、late loader画面、重载旧档和成品jar运行仍未验，整体移植仍未完成。详见P3_NATIVE_FLUID_DISPLAY.md。
