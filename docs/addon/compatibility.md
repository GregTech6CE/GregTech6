# 兼容性与能力边界

本页用于升级评估和确认当前公开入口的覆盖范围；API 与 SDK 版本维护在 [L0 API 版本](l0-setup.md#api-版本)。

返回[附属编写指南](README.md)。

## 已知边界

GT6CE 仍处于开发阶段，`public` 不等于长期二进制、源码或数据兼容保证。附属应固定 GT6CE 构建、目标 Minecraft 版本和加载器 SDK，升级后重新验证。契约与当前实现不一致时，以源码和实际消费入口为准，不以类名或旧包名推断。

`AddonRuntime.Services`、`AddonPlatform.Services`、`AddonLoader.Services` 和 `MaterialInfoTool.Host` 是宿主安装／实现边界；附属使用对外 facade，不替换宿主服务。`ModData`、`InstalledService`、`StructureGrid` 和 `RecipeMapSpec` 也不是当前附属扩展契约。

没有公开的 Cover 注册 API，也没有新工具身份注册 API。需要控制面互通时使用 [L4 的 `MachineControl.Provider`](l4-interop.md)；`AddonTools` 只查询宿主已有工具身份。

其他能力限制随对应专题维护，升级时按使用范围核对：

| 使用范围 | 核对位置 |
| --- | --- |
| 注册关闭、失败不回滚、无生产 reset | [L0 生命周期](l0-setup.md#附属身份与生命周期) |
| 配方机／燃料机的客户端注入点 | [L1 配方机和燃料机](l1-declarations.md#配方表配方机和燃料机) |
| 行为机 Screen、只读菜单、客户端状态与部件 Renderer | [L2 客户端表现](l2-behavior.md#客户端表现与持久状态) |
| 动态配方的中立优先级、具体批次及材料规则 | [L2 配方运行时](l2-behavior.md#配方结构与世界运行时) |
| 数据包不能替换内置布局或注册新主控 | [布局覆盖范围](layout-data.md#覆盖范围与重载) |
| KubeJS 暴露的声明子集 | [L3 KubeJS](l3-data-and-scripts.md#kubejs) |
| 诊断与结构通知的线程、成本和频率 | [L5 结构诊断](l5-query-and-diagnostics.md#结构诊断) |

## 旧入口迁移

API `1.10.0` 移除了旧材料句柄及其注册重载，并更改了模具浇铸接口。使用受影响接口的附属需要更新源代码，再用 1.10.0 公共 API 和目标平台 SDK 重新编译。

| 旧入口 | 新代码使用 |
| --- | --- |
| `GregTechMaterialContributor` | [材料贡献接口](materials.md#材料与材料信息)中的 `AddonMaterialContributor` |
| `forms()`、`compositions()`、`metadata()`、`materialTags()`、`materialGroups()`、`processes()`、`MaterialInfoTool.forAddon()` | [材料信息](materials.md#材料与材料信息)中的 `materialInfo(Registrar)` |
| `GregTechMaterials.find(String)`、`Material.resolve()` | 在 `AddonMaterialContributor.link()` 中用 `GregTechMaterials.resolve(name)` 解析规范名或别名，返回 `Optional<MaterialKey>`。材料初始化达到 `READY` 后，可通过 `catalog().find(key)` 读取 `MaterialView` |
| `GregTechMaterials.find(int)`、`all()`、`invalid()` | 材料就绪后通过 `catalog().materials()` 枚举，旧整数 ID 可比对 `MaterialView.info().id()`。查询不到材料时处理 `Optional.empty()` |
| `Material.mass()` | 在冻结的 `MaterialView` 上读取 `property(MaterialProperties.MASS)`，结果为 `Optional<Long>`，单位为核子数 |
| 接受旧 `Material` 句柄的 `MaterialInfoTool.Registrar` 重载 | 在 `materialInfo(Registrar)` 中使用接受 `MaterialKey` 的重载 |
| `CrucibleMold.fillMold(Material, ...)` | 给 `fillMold` 传入 `MaterialKey`，可从材料名解析得到 |
| `MaterialDeclaration.centrifuge()`／`electrolyzer()` | [类型化分解规则](materials.md#分解规则api-18) |
| `AddonMachineContributor.structureLayouts()`、KubeJS `addLayout()` | [显式 `controller.withStructure(layout)` 绑定](multiblocks.md#显式绑定) |
| `StructureLayout` 三参数、`LayoutCell` 六参数构造器 | [布局 builder 或完整构造器](multiblocks.md#世界匹配约束) |
| 内部 `gregtech.engine.recipe.data` 文档类型 | [公开 `gregtech.api.addon.recipe` 文档模型](recipe-data.md#java-api) |

`link()` 在 `READY` 前执行。链接回调通过 `GregTechMaterials.resolve(name)` 查询材料身份，冻结的完整目录和质量等定义属性在 `READY` 后可读。其余入口的兼容行为及旧默认值见对应专题。

## 示例状态

当前工作树没有完整的独立附属样例目录。指南中的片段用于说明接口或字段，没有作为完整工程编译和运行验证。新增可维护消费者示例放到仓库根 `examples/`，写明依赖、构建／运行命令和已验证平台；临时消费探针放 `work/`。

仓库内可参考 [StructureApiTestAddon](../../minecraft/src/test/java/gregtech/gametest/StructureApiTestAddon.java) 的公共 API 消费方式，以及 [GUI 资源示例](../../tools/gui/examples/assets/example/gui/templates/machines/washer.json)。前者属于测试源码集，后者只演示资源；它们都不是独立可分发的完整附属工程。
