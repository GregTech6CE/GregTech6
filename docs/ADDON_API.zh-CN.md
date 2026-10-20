# Addon 接口

[English](ADDON_API.md) | [中文](ADDON_API.zh-CN.md)

API 版本 **0** 对应模组版本 **0.0.0**，平台是 Forge 1.20.1 与 NeoForge 1.21.1。按目标加载器编译对应的 SDK。SDK 里只有附属接口，没有 GT 内部类。配方仍在自己的 `@Mod` 构造方法里注册 `GregTechAddon`；GT 完成材料/物品关联和内置机器配方后，在 common setup 调用一次 `onRecipesReady`。按 Addon 模组 ID 排序调用；重复 ID、晚注册、二次派发明确报错，回调异常使初始化失败，生命周期记为 FAILED。

旧的材料入口和机器入口在已安装的 GT 里仍会加载。它们已弃用，不在 SDK 里。新代码用 `AddonMaterialContributor` 和 `AddonMachineContributor`。

## 版本

依赖模组 id `gt6ce`，在 `mods.toml` 或 `neoforge.mods.toml` 里写 `versionRange="[0.0.0,)"`。`context.apiVersion()` 当前返回 **0**。本开发基线包含本文列出的全部能力，不要按导入代码的上游版本号屏蔽现有功能。API 号不能代替加载器依赖范围。

`gradle.properties` 的 `mod_version` 是 `0.MINOR.PATCH`。`GregTechAddons.API_VERSION` 就是这个 MINOR。PATCH 不改这份 API。1.0 之前，MINOR 升级可以破坏附属调用。破坏一个已经写明的入口，要么等下面的移除表到期，要么在同一版的变更表里写明。

要淘汰的入口同时标 `@Deprecated(since = "0.N", forRemoval = true)` 和 `@AddonApiRemoval(id = "<二进制名>", removeAt = "0.M")`，上面放同一段 Javadoc。`id` 是包名加类型，方法或字段再加 `#成员`。`@deprecated` 用 `{@link}` 或 `{@code}` 写出替代，并写上 `0.M`。`M` 至少是 `N + 2`：在 `0.N` 弃用的入口，`0.(N+1)` 仍然保留。`M` 小于等于当前 MINOR 时构建失败。到期后删掉入口，或把 `removeAt` 改晚并在本文写原因。没有 `forRemoval = true` 的 `@Deprecated` 不在这张表上。

## 变更

API **0** 是 GT6CE 的开发基线，包含下表全部能力。下列 1–7 只记录导入代码的上游开发阶段，不是 GT6CE 发版记录，也不是运行时功能门槛。

| 上游 MINOR | 导入内容 |
| --- | --- |
| 8 | 坩埚成分、熔炼目标和合金反应。附属用材料名声明：成分可以选择是否据此生成一条坩埚反应，熔炼目标决定熔化后变成什么，显式反应和禁用改合金表。表在材料链接结束时冻结。上游以 0.8.0 引入，CE API 0 包含这些能力；旧入口的删除期限延后到 0.10。 |
| 7 | 燃料能源机器。附属声明燃料表和一台流体燃料机器：效率、每 tick 一个能量包、储罐容量，以及正面、背面或背后 1 到 8 格。单方块和多方块都由 GT 注册并运行。上游模组版本从 0.7.0 起。材料数字入口保留到 0.9，避免在新增模板的同一 MINOR 里删掉它们。 |
| 6 | 行为型机器。附属声明槽、储罐、开关、能源和 GUI 贴图，并提供 `MachineBehavior`。方块、物品栏、能源、菜单和存档由 GT 持有。上游模组版本从 0.6.0 起。版本 3 的入口保留到 0.8，避免在新增外壳的同一 MINOR 里删掉它们。 |
| 4 | 附属接口单独成源码集，每个加载器一个只供编译的 SDK。材料、前缀、物品、流体、配方、工具和能源走下面的门面。上游模组版本从 0.4.0 起。版本 3 的注册和 `onRecipesReady` 保留。 |
| 5 | 附属材料只按名字声明。GT 在启动时从 10000 起分配数字 id，不写入存档。同名且字段一致就合并；字段不一致或只差大小写则启动失败。`builder(int, String)` 忽略传入的数字。上游模组版本从 0.5.0 起。 |
| 3 | 配方机器和大型配方机器。上游模组版本从 0.3.0 起。版本 2 的注册、材料和 `onRecipesReady` 保留。 |
| 2 | 排队材料和追加形态。 |
| 1 | 附属注册和 `onRecipesReady`。 |

## 弃用与移除

旧入口的弃用起点统一记为 CE 基线 `0.0`；材料数字入口的最早移除期限仍为 `0.9`，四个旧贡献者／布局入口随坩埚声明导入延后到 `0.10`。当前版本归零不提前移除任何入口。

| 入口 | 弃用版本 | 替代 | 计划移除 |
| --- | --- | --- | --- |
| com.gregtech.gregtech.api.addon.GregTechMachineContributor | 0.0 | AddonMachineContributor | 0.10 |
| com.gregtech.gregtech.api.addon.GregTechMaterialContributor | 0.0 | AddonMaterialContributor | 0.10 |
| com.gregtech.gregtech.api.addon.AddonMachineDefinition | 0.0 | MachineDeclaration | 0.10 |
| com.gregtech.gregtech.api.addon.AddonMachineLayout | 0.0 | MachineLayout | 0.10 |
| com.gregtech.gregtech.api.addon.MaterialDeclaration#builder | 0.0 | MaterialDeclaration.builder(String) | 0.9 |
| com.gregtech.gregtech.api.addon.AddonMaterials#ADDON_ID_MIN | 0.0 | MaterialDeclaration.builder(String) | 0.9 |
| com.gregtech.gregtech.api.addon.AddonMaterials#ADDON_ID_MAX | 0.0 | MaterialDeclaration.builder(String) | 0.9 |

新材料和追加形态走单独的 `ServiceLoader` 入口，见下文。不要在模组构造方法里提交材料：Forge 1.20.1 上附属构造和 GT 构造会重叠。

```java
GregTechAddons.register(new GregTechAddon() {
    public String id() { return "my_addon"; }
    public void onRecipesReady(Context context) {
        // context.platform(): forge / neoforge
        // context.minecraftVersion(): 1.20.1 / 1.21.1
        // context.apiVersion(): 0，包含本文列出的全部能力
        // 用 GregTechMaterials 和 AddonItems 查询，用 AddonRecipes.map(name).add(...) 加行。
    }
});
```

导入 `com.gregtech.gregtech.api.addon.GregTechAddon` 与 `GregTechAddons`。材料用 `GregTechMaterials`，物品用 `AddonItems`，已有配方表用 `AddonRecipes`。`GTValues`、`GTVoltageTiers`、`BasicMachineEnergy` 和 `MachineWorkCost` 仍在 SDK 里。英文说明提供完整配方示例；你的输入物品须先由自己的加载器注册。`onRecipesReady` 只在启动时走一次。数据包和 KubeJS 走下面的覆盖层，不会再次调用这个回调。

七个参数的 `Line` 表示每个物品输出都必定产出。需要概率时，按物品输出各给一个 `int`，单位是万分比。`10000` 是必定。空数组和不写一样。长度必须和物品输出一致，每个值是 1 到 10000。流体输出仍是必定。机器加工、JEI、EMI 和数据包覆盖层读的是配方行上的同一个概率。数据包物品输出本来就有 `chance` 字段。

`AddonItems.selectorTag(n)` 是 n 号编程电路，物品是 `integrated_circuit_0` 到 `integrated_circuit_24`，数量是 1。n 不在 0 到 24 就启动失败。编程电路本身是催化剂物品，机器只检查有没有，不扣除。把它的下标写进 `keptInputs` 也可以，结果不变。

`keptInputs` 是 `Line` 的最后一个数组，列出不消耗的物品输入下标。每一项数量仍写成 1 或更多。不写或空数组表示都不标记，这一行匹配的输入和以前一样。下标越界或重复会启动失败。数量 0 仍然启动失败，错误信息会指向 `keptInputs`。EMI 把标记的输入画成催化剂，和内置催化剂输入一样。数据包覆盖层不变：那里的数量 0 仍被拒绝，覆盖层也没有这个字段。

```java
var circuit = new AddonRecipes.Line(
        new ItemStack[]{new ItemStack(MyItems.MY_ORE.get()), AddonItems.selectorTag(3)},
        new ItemStack[]{AddonItems.stack("dust", "Copper")},
        null, null, 16, 16, 0);
var kept = new AddonRecipes.Line(
        new ItemStack[]{new ItemStack(MyItems.MY_ORE.get()), new ItemStack(MyItems.MY_LENS.get())},
        new ItemStack[]{AddonItems.stack("dust", "Tin")},
        null, null, 16, 16, 0, null, new int[]{1});
```

## 机器配方数据包

数据包可以给已经存在的机器配方表加静态行、删静态行。JSON 放在该版本的原版配方目录：Forge 1.20.1 用 `data/<命名空间>/recipes/`，NeoForge 1.21.1 用 `data/<命名空间>/recipe/`。不能新建配方表，也看不到、改不了动态配方。KubeJS 的 `event.custom()` 提交的就是同一份 JSON，没有单独的 KubeJS 插件。`event.remove({type: "gt6ce:machine"})` 删的是原版配方管理器里的标记配方；下一次重载按剩下的标记重建机器表。

标签绑定之后的重建顺序：

1. 恢复启动快照。快照是内置行加上 `onRecipesReady` 里 addon 加的行。兼容层和带标签的外部矿物行不在快照里。
2. 按当前标签重建这些兼容层和外部矿物行。
3. 按配方 id 排序，执行全部 `gt6ce:machine_removal`。
4. 按同样的 id 顺序，执行全部 `gt6ce:machine`。

再执行一次 `/reload` 会重走这四步，新增不会叠上去。单人游戏只有一张静态表：集成服务端应用覆盖层，客户端不再做第二次。专服客户端用原版配方同步收到标记，在本地做同样的覆盖，然后 JEI 和 EMI 重新读这些表。

```json
{
  "type": "gt6ce:machine",
  "map": "gt.recipe.crusher",
  "item_inputs": [{ "item": "minecraft:heart_of_the_sea" }, { "tag": "minecraft:coals", "count": 1 }],
  "fluid_inputs": [{ "fluid": "minecraft:water", "amount": 1000 }],
  "item_outputs": [{ "item": "minecraft:emerald", "count": 1, "chance": 5000 }],
  "fluid_outputs": [{ "fluid": "minecraft:lava", "amount": 1 }],
  "eu_per_tick": 16,
  "duration": 64,
  "special": 0
}
```

`map` 是已有配方表名字，例如 `gt.recipe.crusher`。物品输入要么写 `item`，要么写 `tag`，不能同时写。数量省略时是 1。流体数量是毫桶，新增时必填。输出 `chance` 是万分比，省略时是 10000；写 0 会被拒绝。`eu_per_tick` 至少为 0，`duration` 至少为 1，省略的 `special` 是 0。物品、标签和流体 id 必须是 `namespace:path`，字符只允许 `[a-z0-9_./-]`。配方表名字只用 `[a-z0-9_.]`。标签输入会按物品 id 排序，展开成每个成员一条具体配方。标签为空、id 不存在、槽位装不进这张表、展开组合超过 4096，或者这张表里已经有一条配方的输入被新配方的输入覆盖，这条新增都会被拒绝。要换掉旧行，就在同一个包里先写删除；删除先执行。

```json
{
  "type": "gt6ce:machine_removal",
  "map": "gt.recipe.crusher",
  "item_inputs": [{ "item": "minecraft:iron_ingot", "count": 1 }, { "tag": "minecraft:coals" }],
  "fluid_inputs": [{ "fluid": "minecraft:water", "amount": 1000 }],
  "item_outputs": [{ "item": "minecraft:emerald", "chance": 10000 }],
  "fluid_outputs": [{ "fluid": "minecraft:lava" }]
}
```

列出的每一项输入和输出都要各自对上一个不同的槽。数量、流体量或几率写 0，或者省略，表示任意值。标签在配方物品属于该标签时命中。`{"map":"gt.recipe.crusher","clear":true}` 删掉这张表上的全部静态行。`clear` 不能再带匹配列表；两者都没有的删除会被拒绝。展示用的假配方也是静态行，可以被删掉。正在加工的机器已经把产出记进存档，重载后这一轮仍会出产。

出现未知字段时这条配方会被拒绝。顶层以 `_` 开头的字段会被忽略，例如 `_comment`，以及 KubeJS 7 往 `event.custom()` 的 JSON 里加的 `_kubejs_changed_marker`。

开发环境可以用 `-PkubejsRuntime=true` 装上 KubeJS。这个依赖不进成品，也不占用 `-PcompatRuntime`。

## 信号线

信号线方块实体实现 `GtSignalCarrier`。`GtSignalCarrier.UNIT` 是一级原版红石，数值为 `Integer.MAX_VALUE`。`signal()` 是这根线上当前的精细强度。衰减、存档里的 `mode`，以及方块状态上的 0–15 级都不变。线仍然每个服务端 tick 从信号源重算网络。

```java
if (level.getBlockEntity(pos) instanceof com.gregtech.gregtech.api.signal.GtSignalCarrier wire)
    long fractional = wire.signal();
```

addon 的方块实体实现 `GtSignalSource`，就能给线交精细值。线调用 `gtSignal(side)`，不再走原版 `Level.getSignal`。`side` 按 `Direction.ordinal()`：0 下、1 上、2 北、3 南、4 西、5 东。这是从信号线指向 addon 方块的方向，和传给 `Level.getSignal(neighborPos, side)` 的方向相同。返回值用 `UNIT` 做单位。线先把结果夹到 `[0, 15 * UNIT]`，再按原来的损耗衰减。不足一级的值在线上保持分数；对外接到其他方块时仍向上取整到 0–15。

```java
public final class MySourceBlockEntity extends BlockEntity implements GtSignalSource {
    @Override public long gtSignal(int side) {
        if (side != Direction.WEST.ordinal()) return 0;
        return GtSignalCarrier.UNIT / 2;
    }
}
```

只接收、不发出的方块，在方块实体上实现 `GtSignalReceiver`，或者把方块加进 `gt6ce:signal_wire_receivers` 方块标签。满足任一条件时，被强充能的实心方块不会被当成信号源。两个都满足时按接收端处理。这个标签出厂是空的，addon 在自己的数据包里追加：

```json
{ "replace": false, "values": ["my_addon:display"] }
```

原版发射器（含投掷器）、漏斗、活塞、TNT、音符盒、活板门、门、动力铁轨（含激活铁轨）和红石灯仍走原来的 `instanceof`，不放进这个标签。不要引用 `SignalWireBlockEntity`，它是平台类，不在 addon SDK 里。

## 多个动态配方 provider

固定配方仍然先查。没有固定行命中时，才按这个顺序问动态 provider：`RecipeMap.dynamicRecipes` 装上的内置 provider，然后是每次 `RecipeMap.addDynamicRecipes` 追加的 provider。第一个返回非空配方的 provider 生效，后面的不会再问。返回 null 表示这个输入不归它处理。`dynamicRecipes` 只替换那个内置 provider。`addDynamicRecipes` 往末尾追加一个，不替换内置 provider，也不替换更早追加的 addon provider。

`MachineRecipeMaps.Plantalyzer` 没有内置 provider。addon 可以自己追加，例如作物种子扫描，这不会拆掉 Bumblelyzer、分子扫描、打印机或复制机上已有的 provider。

## 展示行

`AddonRecipes.Map.addDisplay` 写入给 JEI 和 EMI 看的展示行。查找会跳过这些行，机器不会按它们加工。addon 可以用它展示一条动态结果的例子，和 Bumblelyzer 在配方页上放一条扫描示例一样。展示行不能代替 provider：机器要算出真正的产出，仍然要调用 `addDynamic`。

## 材料与形态

在 `META-INF/services/com.gregtech.gregtech.api.addon.AddonMaterialContributor` 里列出 `AddonMaterialContributor`。GT 在材料初始化开始时用自己的类加载器读这个文件，早于内置目录的 `declare()`。返回纯数据。服务构造方法以及 `materials()` / `forms()` 里不要碰 GT 的材料表。`compositions()`、`smeltingTargets()`、`crucibleReactions()` 和 `crucibleRemovals()` 在内置目录 `link()` 之后读取。`link()` 也在这时调用，用来放这四张表没有覆盖的跨材料链接。

```java
public final class MyMaterials implements AddonMaterialContributor {
    public String addonId() { return "my_addon"; }
    public List<MaterialDeclaration> materials() {
        return List.of(MaterialDeclaration.builder("AmmoniumNitrate")
                .displayName("Ammonium Nitrate").color(0xC8D6C0).chemicalFormula("NH4NO3")
                .dust().build());
    }
    public List<MaterialFormRequest> forms() {
        return List.of();
    }
}
```

用名字声明材料。入口是 `MaterialDeclaration.builder(String)`。`builder(int, String)` 保留到 0.9，传入的数字被忽略，并记一条警告。`AddonMaterials.ADDON_ID_MIN` 和 `ADDON_ID_MAX` 也保留到 0.9，不再是附属可以占用的区间。

名字按原文比较。只差大小写的两种写法会启动失败，因为生成的物品 id 会转成小写。报错写出两种写法；如果来自附属，也写出双方的附属 id。和内置材料同名仍然失败。要给内置材料加形态，用 `forms()`。

两个附属可以声明同一个名字。显示名、颜色、贴图、熔点、沸点、密度、化学式、属性、原子数和透明度必须全部相同。`forms()` 取并集。材料按附属 id 顺序记下每一个声明它的附属。只有一个附属时提示用 `tooltip.gt6ce.material_from_addon`。多于一个时用 `tooltip.gt6ce.material_from_addons`（“Added by the %s addons”），附属 id 用逗号拼在一起。字段不一致就启动失败，并写出双方和那个字段。

GT 在内置目录之后按名字排序分配数字 id，从 10000 起。内置 id 不变。分到的数字只在内存里：装的附属不同，同一个名字的数字可以不同，存档不会按这个数字读回来。`GregTechMaterials.find(int)` 仍能解析这次启动分到的 id。`dust()` 生成粉。`metal()` 在粉之外再生成锭、板、杆、齿轮等内置目录已经会生成的形态，不新增前缀。

存档写名字。坩埚内容在 `gt.materials` 里是 `{name, amount}`。坩埚显示字段 `gt.display.material` 是名字，没有要显示的材料时是空字符串。USB 和硬盘文件仍用键 `gt.replicator.data`，值改成名字字符串。旧构建写的数字 `id`，以及数字形式的 `gt.replicator.data` / `gt.display.material`，仍能读回内置材料，读不回附属材料。找不到的名字会跳过，和原来找不到 id 一样。附属物品 id 冲突不会加数字后缀。

生成形态注册在 **`gt6ce`** 命名空间，走两个加载器已经在用的 `MaterialItemDefinitions` 列表。附属手写的物品、方块和机器留在自己的命名空间。两个加载器在按命名空间分支时把 `gt6ce:` 当成 GT 内容，包括食物物品查找，所以生成出来的附属形态也走这些检查。新材料只拿到这份列表按它的属性已经会生成的前缀：粉物品来自 `dust()`。`forms()` 只能点名这些前缀。列表已经发出的一对不会再注册第二次。不在这份列表里的前缀，包括 `crystal`，启动失败。`crystal` 有校验器，但内置目录从不生成它，所以没有图标，也没有 `item.gt6ce.crystal` 翻译。已生成的前缀如果过不了 `isValidFor`，仍然启动失败。例外是附属材料显式请求的 `plate` 和 `foil`：没有锭也会生成这两件物品，不会因此多出双板、密板或杆。钻石的板仍然失败，因为钻石是内置材料。`oreRaw` 以及其他被拒绝的前缀仍然失败。新前缀用下一节的 `prefixes()` 声明。`forms()` 仍然不能点名生成列表以外的前缀。材料表在启动时固定，数据包重载不能增删。旧的数字 id 在方块或物品加载时读取。存档要等这个方块或物品再次保存才写成名字。

翻译键是 `material.gt6ce.<小写名>`，来自 `GTMaterial.getTranslationKey()`。放在附属自己的 `assets/gt6ce/lang/en_us.json` 和 `zh_cn.json`。Minecraft 按键把这些文件和 GT 的语言文件合并。只有一个附属时，提示用 `tooltip.gt6ce.material_from_addon`（英文 “Added by the %s addon”）。共享中文文件里这个键保留英文回退，因为它不是原版 GT6 译文。多个附属共用一种材料时用 `tooltip.gt6ce.material_from_addons`。共享中文文件里这两个键都保留英文回退，因为它们不是原版 GT6 译文。

`dust()` 材料（例如硝酸铵）生成 **6** 个物品。铁的自动形态是 **96** 个。`metal()` 就是用来声明这一大组形态的。Phase C 配方生成器会走过每一种材料，所以新材料会得到它的属性已经意味着的熔炉、粉碎机等配方行。卸掉附属后，这些 `gt6ce:` 物品按 Minecraft 原有规则变成缺失物品。坩埚或 USB 里已经找不到的名字会被跳过，卸掉附属不会把加载弄崩。卸掉附属后再开世界还没有实测。

## 前缀

`AddonMaterialContributor.prefixes()` 声明物品前缀。GT 在内置前缀之后、检查 `forms()` 之前注册。名字已经存在就启动失败，包括只差大小写、注册名相同，以及 `casingSmall` 这种别名。两个附属不能声明同一个名字。失败信息写出附属 id 和已经占用这个名字的前缀。

```java
public List<PrefixDeclaration> prefixes() {
    return List.of(new PrefixDeclaration("sampleBlank", "", " Sample Blank", 8L,
            "item/sample_blank", List.of("Iron", "SampleAlloy"), Set.of()));
}
```

`unitAmount` 是 U 的正整数个数。一块样例坯在坩埚里和粉碎回收里都是 8U。内置前缀的单位量仍走原来的表。`texture` 是这个附属命名空间下的路径：`item/sample_blank` 对应 `assets/<附属>/textures/item/sample_blank.png`。物品只有这一层，用材料颜色染色。没有贴图组目录，也没有覆盖层。

`displayPrefix` 和 `displaySuffix` 是材料名前后的英文拼法。前缀空、后缀 `" Sample Blank"`，读出来是 “Iron Sample Blank”。翻译键 `item.<附属 id>.<蛇形名>` 带一个 `%s` 时，用译文代替这句拼法。前后缀至少写一个。

`materials` 点名要生成这个前缀的材料，可以是内置材料。`conditions` 可选，取值是 `PrefixCondition`：`HAS_TOOL_STATS`、`SMITHABLE`、`NOT_WOOD`、`NOT_COAL`、`NOT_STONE`、`NOT_FOOD`、`NOT_MEAT`。选中的条件要全部命中。不能传自己的谓词。名单为空时必须带 `HAS_TOOL_STATS` 或 `SMITHABLE`，因为单独的否定条件会铺到几乎全部材料上。带了这两个之一，命中的材料会和点名的材料一起生成。没带时，否定条件只检查点名的材料，对不上就启动失败。

`NOT_COAL` 是 Coal、Charcoal、CoalCoke、Lignite、LigniteCoke、Anthracite、HydratedCoal、PetroleumCoke。食物是 GT6 的食物标记。肉是 MeatRaw、MeatCooked、MeatRotten。

物品注册在**附属自己的**命名空间，排在生成出来的 `gt6ce:` 形态之后。顺序是附属 id、声明顺序、材料名。路径和内置形态一样，例如 `sample_blank_iron`。同一个命名空间里路径撞车就启动失败。它不占用 `gt6ce:` 的 id，也不改内置物品 id 列表。`forms()` 不能点这个前缀，材料由声明上的名单决定。启动之后 `GregTechPrefixes.exists` 对这个名字返回真。`AddonItems.stack` 能取到物品。

GT 不为这个前缀添加加工配方。这一节不包括方块前缀。API 版本仍是 0。

## 坩埚

GT 形态丢进坩埚已经会化。数量是这个形态的材料重量。温度到了熔点，内容就变成这种材料的熔炼目标。不声明时，目标是它自己，1U 进 1U 出，坩埚熔炼页不列出它。模具仍按已有形状浇出熔融材料，前提是这个形态存在。

`compositions()`、`smeltingTargets()`、`crucibleReactions()` 和 `crucibleRemovals()` 是 `AddonMaterialContributor` 上的纯数据。GT 在内置目录链接之后、配方加载之前读取。名字是材料名。`Cr` 和 `Chromium` 是同一种材料。未知名字、产量、比例或除数不大于 0、新反应没有原料、产物又出现在自己的原料里，都会启动失败，并写出附属 id。

成分只能写给本附属声明的材料。每个分量是 `GTValues.U` 的正整数倍。除数会存进成分，有成分以后提示里的化学式改由成分生成。`crucible = true` 再加一条合金反应：比例是 `amount / U`，产量是除数。`crucible = false` 只存成分，不加反应。两个附属合并了同一种材料时，成分连同这个开关必须一致。

熔炼目标也只能写给本附属的材料。`amount` 是每 1U 输入对应的产出，所以 `U` 表示 1:1。声明会给材料加上熔化标记，坩埚熔炼页才列出它。不声明就保持默认：熔成自己，没有熔化标记。

显式合金行用的是系数，不是 U。3 份退火铜加 1 份锡得到 4 份青铜，就是产量 `4`、比例 `3` 和 `1`。产物可以是已有材料。禁用按产物和原料名字匹配。原料表为空就删掉这个产物的全部反应。否则原料名字的集合要相同，顺序和比例忽略。禁用放在所有附属加完之后，所以可以删掉别的附属加的行。没有命中只警告，不让启动失败。每条被删掉的行都带附属 id 记一条日志。

合成顺序是：内置显式行、内置成分行、按附属 id 排序的附属显式行、附属成分行，然后禁用。判定规则不动：每 tick 只赢一条，最多一种固态原料，温度低于产物或原料熔点不反应，产出最多的那条胜出。附属行和另一条留下的行产物、产量、原料比例都相同，启动失败。原料比例相同但产量或产物不同，两行都留着，并警告产量更高的会把另一条抢走。要替换就先禁用旧行。

表在材料链接结束时冻结。更早去读会启动失败。这些行没有数据包，也没有 `/reload`。搅拌机里同一条合金是另一张表，用 `gt6ce:machine_removal` 删。这一节不加坩埚方块、外壳、龙头或模具，也不声明任意非 GT 物品丢进去化成什么，也不写坩埚的温度和内容物。

```java
public List<MaterialComposition> compositions() {
    return List.of(new MaterialComposition("SampleCrucibleAlloy", 4, List.of(
            new MaterialComposition.Part("SampleAlloy", 3 * GTValues.U),
            new MaterialComposition.Part("Copper", GTValues.U)), true));
}
public List<SmeltingTargetDeclaration> smeltingTargets() {
    return List.of(new SmeltingTargetDeclaration("SampleAlloy", "SampleAlloy", GTValues.U));
}
public List<CrucibleReactionDeclaration> crucibleReactions() {
    return List.of(new CrucibleReactionDeclaration("SampleCrucibleAlloy", 2, List.of(
            new CrucibleReactionDeclaration.Part("SampleAlloy", 1),
            new CrucibleReactionDeclaration.Part("Tin", 1))));
}
public List<CrucibleReactionRemoval> crucibleRemovals() {
    return List.of(new CrucibleReactionRemoval("StainlessSteel",
            List.of("WroughtIron", "Nichrome", "Chromium", "Manganese")));
}
```

## 配方机器

在 `META-INF/services/com.gregtech.gregtech.api.addon.AddonMachineContributor` 里列出 `AddonMachineContributor`。GT 在材料初始化之后、注册机器方块之前，用自己的类加载器读这个文件。`recipeMaps()`、`machines()`、`layouts()` 返回纯数据。不要自己注册方块。外壳材料写材料名。能源写短名，例如 `EU`。

GregTech 在自己的模组总线上，用附属的命名空间注册方块、物品和方块实体。方块 id 是 `my_addon:press`，方块实体 id 是 `my_addon:be_press`。内置 id 仍是 `gt6ce:<路径>` 和 `gt6ce:be_<路径>`。Forge 可能对这条总线打出 `Potentially Dangerous alternative prefix`；这次用的仍是 Forge 的 `DeferredRegister`。NeoForge 接受在 GT 总线上 `DeferredRegister.create(registry, addonNamespace)`。这个选择来自两个加载器的 API，还没有用启动游戏来确认。所有基础机器共用一个菜单 `gt6ce:basic_machine`。客户端菜单按机器类型名查配方表。

`addonId()` 是加载器模组 id。`gt6ce`、`gregtech6`、`minecraft` 保留。不同命名空间可以重复注册路径。机器类型名是全局的，不能占用内置目录或结构表里的名字。同一附属的两台机器可以共用类型名，条件是配方表、控制器种类、效率、恒定耗能、结构说明键、面罩、廉价超频声明和光照门一致。另一个附属不能再用这个名字。

机器只能是 `MachineDeclaration.Kind.BASIC` 或 `LARGE_RECIPE`。蒸馏塔、物质制造机和其他内置控制器留在 GT 里。`efficiency` 是万分比，范围 1 到 100000。`constantEnergy` 是非计时规则：机器不是 TU 机器时按恒定功率耗能。并行用 `MachineDeclaration.parallel`，默认 1。内置机器仍使用原来按名字算出的效率、恒定耗能和并行，这些值在构造内置 spec 时写进字段。

不调用面方法时，仍是物品和流体六面进出、能量六面进入、没有自动面。调用了任意一个面方法，面罩就按声明来。面用 `BehaviorMachineDeclaration.Face`：底、顶、右、前、左、后。自动面缺省为无。传入 null 也是无。`cheapOverclocking(true)` 打开廉价超频。单方块不调用它就保持关闭，就算机器名撞上内置的廉价表也一样。大型配方机器不调用它时保持原规则：不是 TU 就廉价超频。`minimumLightAbove(14)` 表示正上方 `Level.getRawBrightness` 不到 14 就不开始新的加工。这是 1.7.10 `getBlockLightValue`：天空光和方块光取高的那个，夜晚不压暗。`0` 不检查。已经开始的加工不会被打断。

`LARGE_RECIPE` 需要一张同名的 `MachineLayout`。多方块燃料机也一样。行为机器可以拥有一张同名结构，也可以没有。格子是 `right`、`up`、`back`、零件名和 `StructureRole`。控制器在原点，原点不能再放零件。零件名用 `SharedLargeMachineParts` 的名字、`air`、`tank_wall`、`tank_wall_dense`、`centrifuge_part`、`heat_transmitter`、`distillation_tower_part`、`electrolyzer_part`，或 `parts()` 返回的名字。空结构、重复坐标、未知零件、没有控制器的结构、没有结构的 `LARGE_RECIPE` 机器、未知能量类型、未知配方表，都会在启动时报错并写出附属 id。配方表名用 `[a-z0-9_.]+`，必须是这个入口返回的表，或一张内置机器配方表。燃料表不是机器配方表。`structureKeys` 是结构提示的翻译键，字符集是 `[a-z0-9_.]+`。

`parts()` 返回外壳和能量零件：

```java
public List<StructurePartDeclaration> parts() {
    return List.of(new StructurePartDeclaration("sample_wall", "Steel", "", 6.0F));
}
```

名字是小写注册路径。材料写材料名。贴图是附属命名空间里的路径，留空表示用 GT 的金属墙，并按材料颜色染色。写了路径就是一张不染色的立方体贴图。硬度是正数，爆炸抗性和硬度相同。方块 id 是 `<附属 id>:<名字>`。GT 在内置多方块零件之后注册，挂在自己的总线上，命名空间是附属自己的，并生成方块状态和物品模型。名字是全局的。撞上 GT 零件名（包括 `tank_wall`）或另一个附属的零件名，启动失败，并写出两边。运行时 id 从 1000000 起，先按附属 id，再按声明顺序。结构存的是方块注册名，成形时比较世界里的方块。这个数字不进存档。没有能量角色的零件只是外壳：没有储罐、能量、物品槽，也不自己 tick。

能量角色用同一个声明。EU 和 RU 只是类型字符串不同。

```java
new StructurePartDeclaration("sample_emitter", "Steel", "", 6.0F,
        new StructurePartEnergy("EU", 100_000, List.of(BehaviorMachineDeclaration.Face.FRONT), 32, 4))
```

`capacity` 是正的缓存。`outputFaces` 用 `BehaviorMachineDeclaration.Face`，不能空，也不能重复。`maxSize` 和 `maxAmount` 是 `setOutput` 能设定的最大包。容量或上限不是正数，或面重复，声明时就失败。未知能量短名在启动时失败，并写出附属和零件。方块 id 仍是 `<附属 id>:<名字>`。它有水平朝向、自己的 tick，方块实体是 `gt6ce:addon_energy_part`。输出面相对这个朝向，前后左右的算法和行为机器一样。缓存、已设定的包和停机标记存在零件自己的 NBT 里：`gt.part_energy`、`gt.part_output`、`gt.part_amount`、`gt.part_stopped`。外壳仍用 `gt6ce:multiblock_port`。

`fluidPipes()` 和 `itemPipes()` 增加管子行。GT 把每一行展开成内置档位，追加在内置管子后面。

```java
public List<FluidPipeDeclaration> fluidPipes() {
    return List.of(new FluidPipeDeclaration("teflon", "Teflon", 125, true, true, false, false, 600, 0));
}

public List<ItemPipeDeclaration> itemPipes() {
    return List.of(new ItemPipeDeclaration("pvc", "PVC", 32768, 1));
}
```

`idSuffix` 是方块路径里的材料段。流体管后缀撞上内置流体管，或另一个附属的流体管，启动失败，并写出两边。物品管在物品管目录里用同一条规则。同一个后缀可以一边是流体管、一边是物品管，因为路径不同。`maxTemperature` 为 `0` 时按材料熔点的 1.25 倍计算。`stepSize` 是中号物品管的寻路代价，至少是 4。

一行流体管变成七个方块，从细管到九联。一行物品管变成六个，含三档限流。方块 id 是 `<附属 id>:pipe_<档>_<后缀>` 和 `<附属 id>:item_pipe_<档>_<后缀>`。GT 把它们挂在自己的总线上，命名空间是附属自己的，不插入 `gt6ce` 的管子注册表。它们仍用 `gt6ce:fluid_pipe` 和 `gt6ce:item_pipe` 这两种方块实体，流体和物品跟方块一起存。

合成、焊接和材料回收走内置管子的规则。材料没有 `plateCurved` 时，合成和焊接跳过这一行。附属自己提供 `block.<命名空间>.<路径>` 翻译。GT 生成占位方块状态，动态管子模型会换掉它。这些行不是内置管子。API 版本仍是 0。

GUI 路径如果含有 `命名空间:`，就保留这个命名空间。`textures/gui/machines/Press` 和 `gt6ce:textures/gui/machines/Gas` 仍解析成 `gt6ce:textures/gui/recipes/<文件名>.png`。附属路径 `my_addon:textures/gui/machines/press` 解析成 `my_addon:textures/gui/recipes/press.png`，文件放在 `assets/my_addon/textures/gui/recipes/press.png`。方块模型是 `<命名空间>:block/machine/basic/<机器类型名>`，再加 `_running` 和 `_active`。GT 只给 `gt6ce` 命名空间生成物品模型，附属自己带模型和方块状态。显示名是 `block.<命名空间>.<id>`。

新配方表在 `onRecipesReady`、JEI、EMI 和数据包基线之前创建，并进入 `RecipeMap.RECIPE_MAP_LIST`。配方行从 `onRecipesReady` 加入，或用 `gt6ce:machine` 数据包。JEI 催化剂和多方块预览跟着已注册的机器列表；有结构的大型机器会出现在预览里。合成配方由附属自己的数据包提供，GT 不生成。

API 0 会给附属材料分配 id，并在坩埚和 USB 里存材料名字。已经注册的 GT 流体字段名可以通过 `AddonFluids` 取到。KubeJS 仍用 `gt6ce:machine` 给已经存在的配方表加行，包括这个入口创建的表。内置机器继续走 `OriginalLargeRecipeMachineData` 的名字分支。附属大型机器走默认端口。

## 行为型机器

`AddonMachineContributor.behaviorMachines()` 返回 `BehaviorMachineDeclaration`。另外在 `META-INF/services/com.gregtech.gregtech.api.addon.platform.BehaviorMachineContributor` 里列出 `BehaviorMachineContributor`。这个接口在加载器 API 里，因为 Forge 和 NeoForge 的 `FluidStack` 不同。`machineTypes()` 必须盖住这个附属声明的每一种行为机器类型。每放下一台方块实体，GT 调用一次 `create`。缺少工厂时启动失败，并写出附属和机器类型。

行为不继承 GT 的类。`MachineBehaviorContext` 读写物品、储罐、能源、开关、进度和世界。`BehaviorState` 按键保存 int、long、boolean、字符串和物品。GT 把这张表写入 NBT。行为不接触 `CompoundTag`。缺键读成 0、false、空字符串或空物品。键匹配 `[a-z0-9_.]{1,64}`。字符串最长 256。方块被拆时，GT 在 `beforeBreak` 之后掉落物品栏和状态里的物品。

所有行为机器共用菜单 `gt6ce:behavior_machine`。GT 按声明和附属贴图画 GUI。贴图同时是面板和控件表。控件 UV 用 `BehaviorMachineDeclaration` 上的常量：槽 176,0（18×18）；能量空 194,0、满 204,0（10×54）；储罐 214,0、刻度 232,0（18×54）；进度空 176,54、满 176,71（24×17）；开关关 176,88、开 194,88（18×18）。面板宽 176 到 256，高 166 到 512。玩家物品栏从底部往上 82 像素开始。物品槽坐标是内部 16 像素，框画在左上一像素，所以槽的 `x` 和 `y` 至少是 1。控件必须停在玩家物品栏之上，并且不能重叠。最多 54 个物品槽、8 个储罐、16 个开关。储罐容量是 1 到 1,024,000,000 mB。

槽角色是 `INPUT`、`OUTPUT`、`ANY`。玩家和管道只在 `canInsert` 或 `canFill` 允许时放入 `INPUT` 和 `ANY`。管道从 `OUTPUT` 和 `ANY` 抽出物品。管道只从 `OUTPUT` 和 `ANY` 抽流体，所以输入罐不能被抽走。`insertOutput` 先填 `OUTPUT` 再填 `ANY`，不问 `canInsert`。`acceptFluid` 是管道和容器的灌入路径。默认在 `canFill` 允许时存下这种流体。浓缩液要换成罐里已有的流体时，覆盖这个方法。`context.fill` 在罐空着或已经是这种流体时存入，不调用 `canFill`。

能源复用 `BasicMachineEnergy` 和声明的面。默认是除正面以外的所有面。标称包大小是 `energyPerTick`。接受范围是它的一半到两倍，TU 除外。更大的包在超压爆炸开启时毁掉机器。`energyCapacity` 缺省是 0。GT 先向相邻输出端取能，再跑行为，然后按额定输入排空缓冲，没用掉的能量不留到下一 tick。容量大于 0 时，行为返回后把剩余能量封到这个容量。`consume` 在这一步之前扣已存能量。`generate(amount)` 往同一个缓冲里加能量，返回实际加进去的量。缓冲是容量；容量为 0 时，缓冲是额定输入上限。`setCapacity` 在这一 tick 里改容量。只有调用过它，GT 才把结果写成 `gt.capacity`。旧存档没有这个键时，用声明的容量。外壳不过充爆炸：行为可以读到存能高于容量并自己拆掉方块，GT 在行为返回后丢掉超出的部分。`working(true)` 点亮工作面。运行状态表示这一 tick 开始时有能量。

`energyOutputs`、`energyOut` 和 `energyOutMax` 一起声明出能。缺一项、面重复，或上限不是正数，启动失败。`emit(size, amount)` 从已存能量里扣，按声明顺序对每个出能面调用 `EnergyTransfer.emitEnergyToSide`，直到送完或邻居不再收。返回实际送出的包数。包大小或包数超过声明上限会抛错，GT 不截断。没有声明出能时调用 `emit` 也会抛错。邻居不会来抽这份能量，由行为自己推出去。

机器名有结构时，GT 在行为之前成形。成形后 `structureFormed()` 为真，`energyParts()` 列出结构里的能量零件。没有结构的机器视为已成形，列表是空的。`charge(amount)` 往零件里加到容量为止，返回实际加上的量；零件不再属于这台机器时返回 0。`setOutput(size, amount)` 设定零件每 tick 推出的包，并清掉停机。超过零件声明上限会抛错，GT 不截断。`stop()` 让零件停。零件自己按声明顺序，从输出面调用 `EnergyTransfer.emitEnergyToSide`。只有格子角色是 `ENERGY_OUTPUT`、仍绑在这台已成形的行为机器上、并且没有停机时才推。邻居经 `EnergyBlockViews` 看见它。邻居不抽这份缓存，也不能往里灌。`ENERGY_INPUT` 只给控制器读写缓存，不对外发能。一个零件只属于一个控制器坐标。那个坐标还占着它时，第二台结构不能成形。零件被释放时停机，拆掉结构里的一格也会释放。

行为方块实体仍是附属 `EnergyBlock`。它不同时实现 `IEnergyBlock`，因为两个接口的 `getEnergyTypes` 擦除后是同一个方法。轴、激光纤维、中继、远距变压器，以及会注入或抽取的内置机器，现在用 `EnergyBlockViews` 认邻居。内置方块走的还是原来的对象。齿轮箱本来就经 `EnergyTransfer` 出能，所以早就能够把能量送进会接收的附属方块。扫描仪、Jade 和盖板读数仍只看 `IEnergyBlock`。

`onUse(context, player, hand)` 在服务端、扳手和流体容器之后、菜单之前调用。返回 true 表示这次点击已处理。缺省返回 false，菜单照常打开。

水车不需要新入口。`level()` 和 `pos()` 能看到机器旁边的方块：

```java
private static boolean adjacentWater(MachineBehaviorContext context) {
    for (Direction side : Direction.values()) {
        if (context.level().getFluidState(context.pos().relative(side)).is(Fluids.WATER)) return true;
    }
    return false;
}
```

`blockEntityId` 留空时变成 `be_<id>`，`/` 换成 `_`。写明路径可以让多台方块共用一种方块实体。它们必须是同一附属、同一机器类型，并且能源短名、出能短名、物品角色、流体角色和开关键一致。储罐容量、储能容量、等级、每 tick EU、出能面和控件位置可以不同。共用路径但签名不一致时启动失败。行为机器可以拥有上面说的那张结构。这一节没有附属自绘 GUI。

方块 id 是 `<附属>:<id>`。模型是 `<附属>:block/machine/basic/<机器类型>`，再加 `_running` 和 `_active`。方块状态、模型和 GUI 贴图由附属自己带。显示名是 `block.<附属>.<id>`。开关文案是声明上的翻译键。GT 的行为 GUI 字符串是 `gui.gregtech.behavior.energy`、`progress`、`tank`、`on`、`off`。

行为机器不是配方机器。它不创建 `RecipeMap`，也不是 JEI 或 EMI 的催化剂。配方机器仍用 `gt6ce:basic_machine`。

## 燃料能源机器

`fuelMaps()` 返回 `FuelMapDeclaration`。`fuelGenerators()` 返回 `FuelGeneratorDeclaration`。燃料表不是机器表。名字用 `[a-z0-9_.]+`，槽数和内置燃料表一样。GT 在数据包基线之前建好这张表，所以它进入 JEI、EMI 和 `gt6ce:machine`。燃料表上正数的 `eu_per_tick` 是每 tick 产出的能量，存成负数。这一行仍要有输出。机器可以点名附属燃料表，也可以点名内置表，例如 `gt.recipe.fuels.engine` 或 `gt.recipe.fuels.gas`。配方机器点名燃料表时，按未知配方表失败。燃料表和机器表或已有燃料表重名时，启动失败并写出附属。

`efficiency` 是万分比，范围 1 到 100000。`energy` 是短名，例如 `RU`。`outputPerTick` 是一个包，范围 1 到 1,000,000,000,000。缓冲只在对方收下这个包之后才扣掉它。储罐容量是 1 到 1,024,000,000 mB，燃料罐和每个废液罐都用这个容量。废液罐数量等于燃料表的流体输出数。燃料表至少要有一个流体输入。`outputFront()` 和 `outputBack()` 向相邻面输出。`outputBehind(distance)` 注入到正面背后这么多格的方块。距离是 1 到 8。燃气涡轮的 4 格就是这种形式。没有任意坐标。

`single()` 是单方块。`multi()` 需要一张名字等于 `machineType` 的 `MachineLayout`。格子写 GT 零件名，或 `parts()` 里的外壳。结构没成型时不烧燃料，也不输出。方块 id 是 `<附属>:<id>`。方块实体是 `be_<id>`，`/` 换成 `_`。模型是 `<附属>:block/machine/basic/<机器类型>`，再加 `_running` 和 `_active`。方块状态和模型由附属自己带。多方块出现在结构预览里。两种机器都是自己那张燃料表的 JEI 和 EMI 催化剂。停机开关用普通的机器控制。

```java
@Override public List<FuelMapDeclaration> fuelMaps() {
    return List.of(new FuelMapDeclaration("my_addon.fuel.gas", "Gas",
            "my_addon:textures/gui/machines/gas",
            0, 0, 0, 1, 1, 1, 1, false, false, false, false));
}

@Override public List<FuelGeneratorDeclaration> fuelGenerators() {
    return List.of(
            FuelGeneratorDeclaration.builder("engine", "Steel")
                    .fuelMap("my_addon.fuel.gas").efficiency(10_000)
                    .energy("RU", 32).tank(4_000).outputFront().single().build(),
            FuelGeneratorDeclaration.builder("turbine", "Steel")
                    .machineType("sample_turbine").fuelMap("gt.recipe.fuels.gas")
                    .efficiency(5_000).energy("RU", 32).tank(8_000).outputBehind(4).multi().build());
}
```

这一节不做能量转换，不烧固体燃料，内置柴油引擎和燃气涡轮仍走自己的方块实体。

## 构建附属

按目标加载器构建。Forge 运行 `gradlew.bat :multiversion:1.20.1-forge:addonSdkJar`，得到 `multiversion/versions/1.20.1-forge/build/addon-sdk/gt6ce-addon-sdk-1.20.1-forge-0.0.0.jar`。NeoForge 运行 `gradlew.bat :multiversion:1.21.1-neoforge:addonSdkJar`，得到 `multiversion/versions/1.21.1-neoforge/build/addon-sdk/gt6ce-addon-sdk-1.21.1-neoforge-0.0.0.jar`。每个 jar 是该加载器的只供编译 SDK：核心接口加上该加载器的平台接口，用 Mojang 名，并带许可声明。没有素材，也没有模组描述文件。用 `compileOnly files(...)` 依赖这一个 jar。不要再次打包或 shade 这些类。运行时由已安装的 GT 提供。Forge 生产 jar 是 SRG，不是编译依赖。目前没有 Maven 坐标，从构建目录或 CI 产物取 jar。

附属须声明必需依赖模组 **`gt6ce`**，并写 `versionRange="[0.0.0,)"`。GT 内容和存档命名空间仍是 **`gt6ce`**。Forge 和 NeoForge 各编一份。一个 SDK 不会让 Forge 二进制在 NeoForge 上运行。

## 支持范围

当前支持注册和配方就绪生命周期；`AddonMaterialContributor` 按名字排队材料和已有前缀的追加形态（生成 `gt6ce:` 物品），`dust()` 和 `metal()` 选择生成哪些形态，`prefixes()` 在附属自己的命名空间里增加物品前缀，读取用 `GregTechMaterials`，前缀是否存在用 `GregTechPrefixes.exists`，数字 id 从 10000 起在启动时分配，不写入存档；`compositions()`、`smeltingTargets()`、`crucibleReactions()` 和 `crucibleRemovals()` 用材料名，在材料链接时生效；`AddonItems.stack` 和 `AddonItems.selectorTag`（0 到 24 号编程电路）；`AddonFluids` 按 GT 流体字段名取静液或流动液（`DistW`，不是材料名）；已有配方表上的 `AddonRecipes`（添加、展示、碰撞、物品输入检查、动态 provider、查找，`add` 返回 null 表示被拒绝；`keptInputs` 标出留在机器里的物品输入，数量 0 仍被拒绝）；`AddonMachineContributor` 的配方机器和大型配方机器，能源用短名例如 `EU`，多方块可以写 GT 零件名，或 `parts()` 里的外壳和能量零件，机器和零件都由 GT 注册到附属命名空间，结构角色是 `StructureRole`；`fluidPipes()` 和 `itemPipes()` 各加一行材料，GT 按内置档位展开，方块在附属自己的命名空间；`BehaviorMachineDeclaration` 以及加载器上的 `BehaviorMachineContributor`、`MachineBehavior`、`MachineBehaviorContext`、`BehaviorState`，外壳由 GT 持有，附属不继承它，也不接触 NBT，`energyCapacity`、`emit`、`generate`、`setCapacity` 和 `onUse` 属于这层外壳，同名结构让 `structureFormed()` 和 `energyParts()` 可用，`charge`、`setOutput` 和 `stop` 写能量零件，改过的容量由 GT 写成 `gt.capacity`，零件缓存存在零件自己的 NBT；`FuelMapDeclaration` 和 `FuelGeneratorDeclaration`，燃料表进入 JEI、EMI 和数据包覆盖层，机器烧流体燃料，每 tick 从正面、背面或背后 1 到 8 格输出一个能量包，多方块结构可以写 GT 零件名，或 `parts()` 里的外壳；`GTValues`、`GTVoltageTiers`、`BasicMachineEnergy`、`MachineWorkCost`；附属实现的 `EnergyBlock`，`AddonEnergy` 走 GT 的能量包门槛，EU 电缆的插入和抽出接受这个接口，轴、激光纤维、中继、远距变压器和内置机器的注入或抽取经 `EnergyBlockViews` 也能看见这个接口，扫描仪、Jade 和盖板读数仍只看见 GT 自己的能量方块；`AddonTool` 的 `SPADE`、`UNIVERSAL_SPADE`、`MAGNIFYING_GLASS`，以及 `AddonTools.matches`、`isMagnifyingGlass`、`damageForUse`；`gt6ce:machine` 与 `gt6ce:machine_removal` 两种数据包配方；`GtSignalCarrier`、`GtSignalSource`、`GtSignalReceiver`、`CropScanSource` 和空的 `gt6ce:signal_wire_receivers` 方块标签。

这些保持关闭。`BasicMachineBlock` 和 `BasicMachineBlockEntity` 是 GT 的机器实现，附属机器是数据，不是子类。`Recipe.mRecipeList` 是内部行表，正式代码用 `add` 返回的 `AddonRecipes.Line`。`GTMaterial`、`GTMaterialRegistry` 和材料哨兵由 `GregTechMaterials` 代替。`GregTechTags` 由能源短名代替。`MachineRecipeMaps` 的字段由 `AddonRecipes.map(name)` 代替。`MaterialPrefix` 常量由前缀名字符串代替。完整工具目录留在 GT 里。

`onRecipesReady` 仍不是数据包重载事件。API 版本 0 包含坩埚成分、熔炼目标和合金反应，仍会把旧的材料数字 id 读成内置材料，并在这个方块或物品下次保存时写成名字。这一版不承诺固体燃料、能量转换、把内置引擎改到这套模板上、附属自绘 GUI、外壳和能量零件以外的功能结构零件、坩埚方块、写入坩埚温度或内容物、任意非 GT 物品的熔化登记、坩埚数据包、不经入口就用数据包新建配方表、材料表的数据包重载、一份二进制同时跑两个加载器，或完整旧 GregAPI 替代。1.0 之前仅文档列出的入口作为兼容面。电压等级以[原GT6电压说明](VOLTAGE_TIERS.md)为准。

SDK与项目代码采用LGPL-3.0-or-later；原作者、第三方代码和资产各自声明继续保留，见根LICENSE、COPYING、NOTICE。
