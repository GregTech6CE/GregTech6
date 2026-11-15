# 材料与加工规则

本页展开 L1 的材料贡献入口，说明所有权、类型化信息和加工规则；材料初始化 READY 后的读取见 [L5 查询与诊断](l5-query-and-diagnostics.md#材料目录)。

返回[附属编写指南](README.md)。

## 材料与材料信息

`AddonMaterialContributor` 负责材料早期声明，主要入口包括：

- `materials()`：提交 `MaterialDeclaration`；宿主分配数字 ID，附属按名称引用材料。
- `prefixes()`：新增物品前缀；名称与内置前缀冲突会失败。
- `smeltingTargets()` / `smeltingTargetRemovals()`：增加或移除坩埚熔炼目标。
- `crucibleReactions()` / `crucibleRemovals()`：增加或移除显式坩埚合金行。
- `oreVeins()` / `smallOres()`：提交矿脉和小矿；同名冲突会失败。
- `materialInfo(MaterialInfoTool.Registrar)`：新代码的统一 typed registrar；在材料定义冻结前注册工具、成分、形态、tag、metadata、分组和加工规则。
- `link()`：在宿主内置材料链接完成后处理跨材料关系。

`MaterialDeclaration` 的构造器校验相变温度、密度、显示名、配方字段和矿物副产品；`Feature` 控制基础形态和加工能力。旧接口 `forms()`、`compositions()`、`metadata()`、`materialTags()`、`materialGroups()`、`processes()` 和 `MaterialDeclaration.centrifuge()` / `electrolyzer()` 仍兼容，但新代码应使用 `materialInfo` 的 typed registration 或明确的加工规则。

附属只能定义自身 namespace 的 tag、组和 property key。可以给其他材料添加描述性 tag，但不能借此修改其他材料的 property、形态或加工能力。未知引用、同 key 异类型、环、重复声明和跨所有者冲突会阻止材料初始化进入 READY；重名或互斥声明不能靠加载顺序解决。

材料结果通过 L5 的 `GregTechMaterials.catalog()` 读取；定义期规则与实际平台绑定的区别见[材料目录](l5-query-and-diagnostics.md#材料目录)。旧文档中的 `GregTechMaterialContributor` 已不在当前公共 API 源码中，使用 `AddonMaterialContributor`。

世界生成声明的 `dimension` 接受资源位置，但宿主的矿脉或小矿生成特性必须在目标维度已有注册；内置 biome modifier 只覆盖标准维度。自定义维度不能只写一条声明就假定会生成。

## 类型化 metadata、tag 与替代组

在 `materialInfo(Registrar)` 中构造并提交 metadata、tag 和替代组。以下片段只展示值对象，`addonId` 使用附属自身的 mod ID；旧的 `metadata()` / `materialTags()` / `materialGroups()` 返回值入口仅用于兼容。

```java
MaterialMetadata metadata = MaterialMetadata.builder()
        .property(new PropertyKey<>(new DataKey(addonId, "hardness"), Integer.class), 3)
        .tag(new DataKey(addonId, "conductive"))
        .build();
MaterialTagDeclaration metals = new MaterialTagDeclaration(
        new DataKey(addonId, "metals"),
        Set.of(new DataKey(addonId, "conductive")));
MaterialGroupDeclaration family = new MaterialGroupDeclaration(
        new MaterialGroupKey(new DataKey(addonId, "family")),
        "Family",
        Set.of(new MaterialKey("Iron")),
        Set.of());
```

分类 tag 只表达描述关系，不会自动生成形态、启用燃料、允许回收或授权分解；替代组成员关系也只供明确声明了组匹配的消费者使用。

## 分解规则（API 1.8）

`MaterialProcessKeys.DECOMPOSITION` 与 `MaterialDecompositionRule` 用于在自身材料的 `materialInfo(Registrar)` 流程中提交液体／气体换算、路线和固体兜底。该 typed rule 是唯一权限权威；旧 `MaterialDeclaration.centrifuge()`／`electrolyzer()` shortcut 仅在不存在 typed rule 时生成默认兼容规则。

## 特殊处理方法（API 1.9）

`SpecialProcessMethod` 复用现有 `MaterialProcessKey` 作为 method ID，并把目标材料和辅料规则放在同一张 typed process 表中：

```java
var key = MaterialProcessKey.multiple(
        new DataKey("example", "special_process/wash"), SpecialProcessValue.class);
var method = new SpecialProcessMethod(key, "gt.recipe.bath", "example.special_process.wash");

registrar.registerSpecialProcess(method);
registrar.registerProcess(materialA, key, new MaterialProcessRule<>(
        Set.of(new FormKey("crushedPurified")),
        new SpecialProcessValue.Target(
                Map.of(new FormKey("crushedPurified"), 1),
                List.of(new SpecialProcessValue.ItemOutput(
                        materialB,
                        List.of(new SpecialProcessValue.FormCount(new FormKey("crushedCentrifuged"), 1)),
                        10_000)),
                List.of(),
                1_000,
                200,
                16)));
registrar.registerProcess(reagent, key, new MaterialProcessRule<>(
        Set.of(new FormKey("liquid")), new SpecialProcessValue.Reagent()));
```

Target 的 `inputCounts` 键必须与 `MaterialProcessRule.forms()` 完全一致。`reagentAmount` 对物品规则表示数量，对液体／气体规则表示 mB。方法发布后，`MaterialCatalogView.specialProcesses()` 暴露方法声明，材料目录的 `processes()` 暴露 target／reagent 规则；宿主同时推导 method target／reagent 材料 tag，并按声明表单生成物品／流体 tag。材料图只折叠带 method ID 的配方。
