# 机器配方数据包与文档 API

本页维护 L3 的追加、移除、优先级和 Java 文档契约（API 1.7 起）；KubeJS 字段适配见 [L3 配方 schema](l3-data-and-scripts.md#配方-schema)。

返回[附属编写指南](README.md)。

`gt6ce:machine` 向已有机器配方表追加或覆盖行，`gt6ce:machine_removal` 删除行。数据包路径按目标版本的配方目录（`data/<namespace>/recipes/`，1.21.1 为单数 `recipe/`）。处理顺序固定：恢复基线、重建标签与生成行、应用移除、最后应用追加；移除只能匹配基线和生成行，追加看到的是最终静态集合。同一行的显式 `priority` 决定查找优先级，较高者优先，相同值保持发布顺序。

## 格式

下列 JSON 是字段说明片段，标签是否存在取决于目标平台和整合包；在目标环境确认 `forge:water`、`forge:gravel` 后再使用。

追加示例：

```json
{
  "type": "gt6ce:machine",
  "map": "gt.recipe.crusher",
  "item_inputs":  [{"item": "minecraft:stone", "count": 1}],
  "fluid_inputs": [{"tag": "forge:water", "amount": 1000}],
  "item_outputs": [{"tag": "forge:gravel", "count": 1, "chance": 10000}],
  "fluid_outputs":[{"fluid": "minecraft:water", "amount": 100}],
  "eu_per_tick": 16,
  "duration": 20,
  "special": 0,
  "priority": 5
}
```

- `map` 是内部配方表名（如 `gt.recipe.crusher`），不是注册 ID；未知名字拒绝该行。追加文档至少需要一个输入和一个输出。
- `item_inputs` / `item_outputs` 的条目必须且只能写一种身份：
  - `item`：物品注册 ID，格式为 `namespace:path`。
  - `tag`：标签 ID（不带 `#`）。输入标签表示任意成员；输出标签在加载时按注册 ID 字母序取首个成员作为具体产出，空标签拒绝该行。
  - `material` + `form`：材料名和形态名，加载时解析为具体物品。两者大小写不敏感；追加时名称未知或形态不存在会拒绝该行，移除条件则记录诊断且不匹配。
- `fluid_inputs` / `fluid_outputs` 的条目写 `fluid` 或 `tag` 二选一，不接受 `material`+`form`。
- `count` 省略为 1；追加时物品输入和输出都必须至少为 1。`chance` 省略为 10000（万分比），追加时必须至少为 1。
- `fluid_inputs` 的 `amount` 在追加时必填，0 表示只检查该流体存在、不消耗数量；移除时可省略，省略表示不限数量。`fluid_outputs` 的 `amount` 在追加时必须至少为 1。流体输出也接受 `tag`，同样取字母序首个成员。
- `material_input` 是整行的材料单位规则，不是普通输入槽：`{"material":"Steel","units":"2U","forms":["ingot","block"],"rounding":"whole_outputs"}`。`units` 支持 `2U`、`1/2U` 这样的精确分数；`forms` 必须显式列出；`rounding` 为 `whole_outputs` 或 `exact_ratio`。每行最多一条，占用第一个输入槽。
- `priority` 只用于追加；移除条件带 `priority` 会报错。省略或 0 表示普通行。
- 根对象中以 `_` 开头的键是注释或脚本标记，会被忽略；其他未知字段、重复身份和类型错误都会拒绝该行。
- `material_input` 与 `material`+`form` 是两种不同概念：前者是运行时按形态换算的材料单位规则，后者在加载时直接解析成一个具体物品。

移除示例：

```json
{
  "type": "gt6ce:machine_removal",
  "map": "gt.recipe.crusher",
  "item_inputs":  [{"item": "minecraft:stone"}],
  "item_outputs": [{"tag": "forge:gravel"}]
}
```

- `clear: true` 删除该表的全部静态行；它不能和匹配条件同时出现。
- 未写 `clear` 时至少要有一个匹配条件。所有列出的条件都必须匹配；匹配是集合分配而不是按顺序贪心，因此条件书写顺序不影响结果。
- 移除条件中的 `count` 省略表示不限数量；`chance` 省略或为 0 表示不限概率。

## Java API

`gregtech.api.addon.recipe` 提供完整文档模型。API 只接收 `Map<String,Object>` 形式的 JSON 树，不内置 JSON 解析器；宿主与 KubeJS 各自用已有解析器把文件解析成 Map 后传入，附属也可以使用自己已有的 JSON 库。

```java
import gregtech.api.addon.recipe.*;

var document = MachineRecipeDocuments.addition(Map.<String, Object>of(
        "type", MachineRecipeType.ADDITION,
        "map", "gt.recipe.crusher",
        "item_inputs", List.<Object>of(
                Map.<String, Object>of("material", "Steel", "form", "ingot", "count", 1L)),
        "item_outputs", List.<Object>of(
                Map.<String, Object>of("tag", "forge:gravel", "chance", 10000L)),
        "eu_per_tick", 16L,
        "duration", 20L));
String json = MachineRecipeDocuments.canonical(document); // 不含 "type"，写文件时自行补上
```

- `MachineRecipeDocuments.addition(...)` / `removal(...)` 解析并校验；`canonical(...)` 写出字段顺序稳定的 JSON。
- `MachineRecipeDocument`、`MachineRecipeRemovalDocument` 是公开记录；`ItemComponent`、`ItemResult`、`FluidComponent` 构造时校验 ID、材料名和数量。
- `MachineRecipeType.ADDITION`/`REMOVAL` 与 `MachineRecipeFields` 提供类型 ID 和字段名常量，避免附属复制字符串。
- 1.7 起这些类型不再位于 `gregtech.engine.recipe.data`；依赖旧内部包名的附属需要迁移到公开包。运行时可用 `GregTechAddons.apiVersion() >= 1007` 做特性判断。

## 让附属自己的数据包配方进入 overlay

1. 在模组构造阶段注册一个 `MachineRecipeDocumentType`：

```java
MachineRecipeDocumentTypes.register(new MachineRecipeDocumentType() {
    @Override public String id() { return "mymod:compressor"; }

    @Override public MachineRecipeDocument addition(Map<String, Object> json) {
        return MachineRecipeDocuments.addition(json);
    }
});
```

2. 附属用加载器 API 注册自己的 `RecipeType`/`RecipeSerializer`。以下抽象基类仅演示文档桥接，具体配方类还须实现目标加载器的 `Recipe` 及对应序列化器。配方对象实现平台 API 的 `gregtech.api.addon.platform.MachineRecipeDocumentProvider`：

```java
public abstract class CompressorDocument implements MachineRecipeDocumentProvider {
    private final Map<String, Object> document;

    protected CompressorDocument(Map<String, Object> document) {
        this.document = document;
    }

    @Override public String documentType() { return "mymod:compressor"; }
    @Override public Map<String, Object> document() { return document; }
    @Override public String documentId() { return "mymod:compressor/example"; }
}
```

3. overlay 读取 `document()`，用注册的文档类型校验并得到一个追加或移除文档，然后按与内置类型相同的顺序、优先级和移除匹配规则应用。类型必须恰好返回追加或移除之一；未注册的类型和校验失败只记录诊断，不影响其他行。

注册时机见 [L0 生命周期](l0-setup.md#附属身份与生命周期)。注册在 `GregTechAddons.dispatchRecipesReady` 开始时关闭；重复 ID、迟注册和无效 `namespace:path` 直接报错。文档类型只负责把原始对象转成文档，不执行注册或世界访问。
