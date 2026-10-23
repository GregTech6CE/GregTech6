# 双版本初期对齐：矿石、材料块和石材模型

按 D011 参考 docs/RENDERING_AND_MATERIAL_VALIDATION.md 和 Forge 当前实际源码，完整适配八个 Native 模型、查找及染色类。来源清单记录当前 Forge 与原 saltnya 快照哈希，原作者、许可和 donor 文件保留；未新增第三方功能或第二套材料/注册底层。

OreClientModels/OreBakedModel 恢复宿主背景、破碎背景与普通/小矿叠加图。每个真实 BlockState 的模型绑定既有 STONE/BROKEN，物品 override 调用现有 OreBlock.stoneOfStack/isBrokenStack，读取真实 1.21 BLOCK_STATE 组件，不另存一套宿主身份。材质 RGB 通过原 tint 0 回调提供；共享 overlay 几何、按宿主的有界 quad 缓存及粒子保留。

MaterialBlockClientModels/MaterialBlockBakedModel/CrateBlockBakedModel 恢复共享材质层染色及原箱体规则：世界显示木箱壳、物品显示壳和材料层，overlay 不染色。MaterialBlockClientColors 从原 GregTechClient 中恢复材料块及 MaterialBlockItem 的真实注册回调，包括矿石；只加载在客户端。StoneBlockClientModels 恢复石材物品三维变换，显式 standalone 注册原石材模型；现有半砖上下/双层状态资产保持原接线。

平台差异限制在 typed ModelResourceLocation、Neo 独立模型注册、loader 包名及六参数 BakedQuad 的 ambient-occlusion 标志。BakedModelLookup 排除真实 missing-model 哨兵。沿用现有平台 composite JSON 和共享图集，无新资源或 core Java 改动。

集中编译及一次既有 Neo 客户端主菜单加 Native jar 构建结果见 verification/p3-native-material-models-*.json 和 p3-neoforge-material-models-client-startup.json。实际烘焙日志计数只是模型接线证据，不能当作真实画面或玩法完成率。前两批提示/工具覆盖订阅及 41 模具 loader 修正参与本次启动；未新造夹具或重复客户端。

待验：进入世界后的矿石背景/破碎物品、箱体与材质染色、石材手持和半砖、F3+T、提示/选面/压力表、JEI/Jade、生产 jar 实跑、完整玩法、独立保存重载与旧档。继承资源警告继续保留记录，不填占位图掩盖缺口。整体移植仍未完成，本阶段未声明全部双版本功能对齐完成。


同批晚改：本次主菜单日志原82个block缺模型请求全为41材质xore/oresmall的无用材料cube路径，真实矿石已由OreClientModels的stone模板接线；Native现不再额外注册这两prefix。最终Native jar/Forge compile 20s通过，无重复客户端，晚改后的警告数量未实跑。此前41模具composite缺模型警告在本次实际日志中已消失；45气体材料blockstate及57462初始item JSON请求警告仍保留，不能据此断言实际全部图标缺失。
