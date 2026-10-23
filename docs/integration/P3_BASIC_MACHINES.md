# 基础机器与集中 NeoForge 检查

更新：2026-10-01。要求连续批量移植，每几批进行一次小检查，以 NeoForge 为重点。

## 本批来源与实现

主要来源 saltnya 的完整 BasicMachineDefinitions、BasicMachineSpec、FaceConfig、MachineRecipeMaps.byMachineName、BasicMachineBlockEntity、MachineWorkOutputs、BasicMachineBlock 和真实菜单/界面。原始项目与作者历史保持不动。

机器规格表移入单一 core BasicMachineCatalog/BasicMachineParameters；面掩码与 builder 移入 MachineFaceMasks；平台 FaceConfig 保留 Direction API 并委托共享定义。机器配方别名使用同一 MachineRecipeNames；两版本的原生 RecipeMap 及 ItemStack/FluidStack 留平台边界。菜单坐标与 GUI 纹理命名亦共享。

NeoForge 迁入普通基础机器原处理流程：输入检查与消耗、并行/超频工作计量、单次随机产物与背压保留、暂停与覆盖物控制、物品/流体侧向能力、自动输入输出、能量包输入与过压处理。NBT 使用 HolderLookup.Provider，物品使用 components，机器拆卸数据使用 BLOCK_ENTITY_DATA。罐体使用此前共享 LongFluidStorage 的原生包装。覆盖物过滤读取与保存使用同一 provider 边界。

实际原生 DeferredRegister 负责块/物品/独立 BE type/menu；能力通过 RegisterCapabilitiesEvent 注册；菜单流体初始同步与后续 payload 使用 RegistryFriendlyByteBuf/FluidStack.OPTIONAL_STREAM_CODEC。注册与客户端 screen/tint 入口均接线。

2658 条原始资源引用闭包记录于 core/provenance/basic-machine-source-integration.json：模型与 blockstate 适配 loader 名；原图与 GUI 图移至 core 共享，来源 SHA 保留。不是 2658 个新增玩法功能。

31 普通罐/280 管道已 cherry-pick 538d077740，并在 Neo 模组入口接入 FluidTransportRegistries 和 FermentationAccess.bindOriginalRecipeMaps，绑定发生在科技物品与发酵 recipes 初始化后。

## 本次小检查

只执行 :neoforge:compileJava，包含依赖的 core 编译。首轮 29 秒停止于 FluidCatalog 引用平台 FluidVisualPolicy；修复为共享 FluidTintRules。第二轮 23 秒发现 17 个 Neo 编译错误；修复组件比较、遗漏 MaterialPresentation、provider 数据包签名、原生 LiquidBlock 构造、Holder.value、正确 BlockMaterialPrefix 包名及多方块端口类型边界。第三轮成功，14 秒，25 条 deprecated API 警告。日志：work/p3-neoforge-checkpoint-20261001*.log。

没有本批客户端/专服启动、GameTests、世界保存重载或完整玩法检查。编译通过只确认类型和 API 接线，不证明注册、资产加载、游戏交互或存档功能完成。本批未重新编译 Forge。

## 明确缺口

- 多方块 factory、massfab/large 系列仍未移植到 Neo，完整规格仍保留在共享 catalog；本批不把这些机器注册成普通单方块机器。待专用实体/factory 整合后接入。
- USB 相邻端口与动态材料数据链暂经 MachineContextRecipes 显式未绑定入口保留，普通 recipe map 查询真实工作；不宣称 USB chain 已完成。
- 除食物发酵等已迁入表之外，大量原 machine recipe loader 行仍仅在 Forge；Neo 新机器并不因此自动具有全部配方。
- 完整电动工具、工具行为总线、完整原机器 tooltip、覆盖物外观 renderer 和材料方块动态 baked renderer 尚待后续整合。
- Native components 保存键保留，但旧世界跨大版本转换与历史 recipe job 状态兼容尚未经运行验证。

## 2026-10-02 后续：完整源码恢复与机器存档

上文记录的是 2026-10-01 初次基础机器迁移时的范围；完整快照恢复后已包含后续多方块、USB、配方及工具代码，旧缺口条目不能直接作为当前源码清单或运行完成声明。当前六个生产文件的库存/搬运/流体身份修复已双版构建；Neo 普通专服同一世界两个独立 JVM 读取未完成粉碎机作业并继续完成，同时保存命名物品/流体及暂停状态，验证边界见 verification/machine-io-parity-20261002.md / .json。未证明完整生存获取、物理供能链、全部多方块/USB/随机流体配方、客户端、Forge 对应运行和旧存档。
