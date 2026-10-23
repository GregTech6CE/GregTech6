# 双版本初期对齐：机器、坩埚及胶囊显示

按 D011 参考 docs/RENDERING_AND_MATERIAL_VALIDATION.md、既有坩埚及普通机器阶段 MD，完整适配五个 Native 客户端类及六个既有类接线。以当前 Forge 实际实现为基准，原 donor、作者和许可证保留，来源哈希见 core/provenance/native-machine-models-integration.json。

MachineBlockClientModels 成为燃烧箱、普通机器及引擎物品模型的唯一烘焙事件入口，移除 BurningBoxClientSetup/EngineClientSetup 旧重复模型回调，保留其染色及实体渲染事件。普通机器模型按实际 BlockState 包裹 tint，保留原 facing/lit/running 旋转；物品使用原 active/idle 回退和标准三维变换。Neo typed model map 需显式 standalone 注册实际基础机器三态模型，原始材质模板和 fallback 规则保留。

CrucibleClientModels/CrucibleBowlIcons 及 41 原碗形 composite JSON 恢复坩埚二像素外壳，替代未接线时的铁块 cube 占位显示。Forge 原字节保留，Native 分支仅修改 loader 为 neoforge:composite，全部几何/层/材质不变。CrucibleBlockBakedModel 保留复制 quad 的真实 ambient-occlusion 标志，熔毁 fullbright 和既有共享 CrucibleHullTint 接回真实 ModelData/材质 RGB。

Native SmeltingCrucibleEntity 的内容/温度已有 GTEnergyBlockEntity registry-aware update packet 同步，因此不新建另一份包或存储。补 getModelData 读取真实温度警告，客户端处理 update tag 后请求模型数据和区块外壳刷新；大型坩埚沿用继承存储及现有 BER。实际网络/区块卸载重载尚未实测。

CapsuleCellRenderer 完整恢复 cell 容器四侧流体窗口，用真实 vessel item 的流体能力、纹理/tint、原 full-height 几何，注册实际 PORTABLE_CONTAINER 实体。VertexConsumer 换用 1.21 addVertex/setColor/setUv/setOverlay/setLight/setNormal，保留变换和 overlay。复用已有物品/流体/容器同步，不另造储罐。

必要集中编译和单次既有 Neo 主菜单启动/Native jar 构建回执位于 verification/p3-native-machine-models-*.json、p3-neoforge-machine-models-client-startup.json。实际烘焙日志可证明事件和模型接线，不能证明进入世界的警告、染色、旋转或 cell 流体画面；不新增测试夹具，不频繁重复启动。

待验：实际机器状态/手持、熔毁与坩埚内容画面、cell 窗口/能力与数据更新、F3+T、真实专服联机、本批生产 jar 运行、完整生存流程、独立世界重载与旧档。整体移植仍未完成。


同批晚改：本次实际客户端只剩1个block模型缺失，即Source继承但未被任何渲染使用的barometer/sprites.json请求；Native移除该多余注册，真实压力表仍取已有atlas sprite。最终Native jar/Forge compile 20s通过，不重复客户端，修正后警告数量不声称已实跑。上一批82个无用矿石材料模板请求在本次实际日志已消失；45气体材料blockstate和57462初始item JSON警告仍独立记录。
