# Addon 接口

[English](ADDON_API.md) | [中文](ADDON_API.zh-CN.md)

API 版本 **4** 就是模组版本 **0.4.0**，平台是 Forge 1.20.1 与 NeoForge 1.21.1。按目标加载器编译对应的 SDK。SDK 里只有附属接口，没有 GT 内部类。配方仍在自己的 `@Mod` 构造方法里注册 `GregTechAddon`；GT 完成材料/物品关联和内置机器配方后，在 common setup 调用一次 `onRecipesReady`。按 Addon 模组 ID 排序调用；重复 ID、晚注册、二次派发明确报错，回调异常使初始化失败，生命周期记为 FAILED。

版本 3 的材料入口和机器入口在已安装的 GT 里仍会加载。它们已弃用，不在 SDK 里。新代码用 `AddonMaterialContributor` 和 `AddonMachineContributor`。

## 版本

依赖模组 id `gregtech6`，在 `mods.toml` 或 `neoforge.mods.toml` 里写 `versionRange="[0.4,)"`。GregTech 太旧时，加载器在附属的类加载之前拒绝。`context.apiVersion()` 返回 MINOR，只用于可选分支，例如 `context.apiVersion() >= 2`。它不能代替 `versionRange`，也不要要求等于某一个版本。

`gradle.properties` 的 `mod_version` 是 `0.MINOR.PATCH`。`GregTechAddons.API_VERSION` 就是这个 MINOR。PATCH 不改这份 API。1.0 之前，MINOR 升级可以破坏附属调用。破坏一个已经写明的入口，要么等下面的移除表到期，要么在同一版的变更表里写明。

要淘汰的入口同时标 `@Deprecated(since = "0.N", forRemoval = true)` 和 `@AddonApiRemoval(id = "<二进制名>", removeAt = "0.M")`，上面放同一段 Javadoc。`id` 是包名加类型，方法或字段再加 `#成员`。`@deprecated` 用 `{@link}` 或 `{@code}` 写出替代，并写上 `0.M`。`M` 至少是 `N + 2`：在 `0.N` 弃用的入口，`0.(N+1)` 仍然保留。`M` 小于等于当前 MINOR 时构建失败。到期后删掉入口，或把 `removeAt` 改晚并在本文写原因。没有 `forRemoval = true` 的 `@Deprecated` 不在这张表上。

## 变更

| MINOR | 变更 |
| --- | --- |
| 4 | 附属接口单独成源码集，每个加载器一个只供编译的 SDK。材料、前缀、物品、流体、配方、工具和能源走下面的门面。模组版本从 0.4.0 起。版本 3 的注册和 `onRecipesReady` 保留。 |
| 3 | 配方机器和大型配方机器。模组版本从 0.3.0 起。版本 2 的注册、材料和 `onRecipesReady` 保留。 |
| 2 | 排队材料和追加形态。 |
| 1 | 附属注册和 `onRecipesReady`。 |

## 弃用与移除

| 入口 | 弃用版本 | 替代 | 计划移除 |
| --- | --- | --- | --- |
| com.gregtech.gregtech.api.addon.GregTechMachineContributor | 0.4 | AddonMachineContributor | 0.6 |
| com.gregtech.gregtech.api.addon.GregTechMaterialContributor | 0.4 | AddonMaterialContributor | 0.6 |
| com.gregtech.gregtech.api.addon.AddonMachineDefinition | 0.4 | MachineDeclaration | 0.6 |
| com.gregtech.gregtech.api.addon.AddonMachineLayout | 0.4 | MachineLayout | 0.6 |

新材料和追加形态走单独的 `ServiceLoader` 入口，见下文。不要在模组构造方法里提交材料：Forge 1.20.1 上附属构造和 GT 构造会重叠。

```java
GregTechAddons.register(new GregTechAddon() {
    public String id() { return "my_addon"; }
    public void onRecipesReady(Context context) {
        // context.platform(): forge / neoforge
        // context.minecraftVersion(): 1.20.1 / 1.21.1
        // context.apiVersion(): 4，只用于可选功能分支
        // 用 GregTechMaterials 和 AddonItems 查询，用 AddonRecipes.map(name).add(...) 加行。
    }
});
```

导入 `com.gregtech.gregtech.api.addon.GregTechAddon` 与 `GregTechAddons`。材料用 `GregTechMaterials`，物品用 `AddonItems`，已有配方表用 `AddonRecipes`。`GTValues`、`GTVoltageTiers`、`BasicMachineEnergy` 和 `MachineWorkCost` 仍在 SDK 里。英文说明提供完整配方示例；你的输入物品须先由自己的加载器注册。`onRecipesReady` 只在启动时走一次。数据包和 KubeJS 走下面的覆盖层，不会再次调用这个回调。

## 机器配方数据包

数据包可以给已经存在的机器配方表加静态行、删静态行。JSON 放在该版本的原版配方目录：Forge 1.20.1 用 `data/<命名空间>/recipes/`，NeoForge 1.21.1 用 `data/<命名空间>/recipe/`。不能新建配方表，也看不到、改不了动态配方。KubeJS 的 `event.custom()` 提交的就是同一份 JSON，没有单独的 KubeJS 插件。`event.remove({type: "gregtech:machine"})` 删的是原版配方管理器里的标记配方；下一次重载按剩下的标记重建机器表。

标签绑定之后的重建顺序：

1. 恢复启动快照。快照是内置行加上 `onRecipesReady` 里 addon 加的行。兼容层和带标签的外部矿物行不在快照里。
2. 按当前标签重建这些兼容层和外部矿物行。
3. 按配方 id 排序，执行全部 `gregtech:machine_removal`。
4. 按同样的 id 顺序，执行全部 `gregtech:machine`。

再执行一次 `/reload` 会重走这四步，新增不会叠上去。单人游戏只有一张静态表：集成服务端应用覆盖层，客户端不再做第二次。专服客户端用原版配方同步收到标记，在本地做同样的覆盖，然后 JEI 和 EMI 重新读这些表。

```json
{
  "type": "gregtech:machine",
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
  "type": "gregtech:machine_removal",
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

只接收、不发出的方块，在方块实体上实现 `GtSignalReceiver`，或者把方块加进 `gregtech:signal_wire_receivers` 方块标签。满足任一条件时，被强充能的实心方块不会被当成信号源。两个都满足时按接收端处理。这个标签出厂是空的，addon 在自己的数据包里追加：

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

在 `META-INF/services/com.gregtech.gregtech.api.addon.AddonMaterialContributor` 里列出 `AddonMaterialContributor`。GT 在材料初始化开始时用自己的类加载器读这个文件，早于内置目录的 `declare()`。返回纯数据。服务构造方法以及 `materials()` / `forms()` 里不要碰 GT 的材料表。`link()` 在内置目录 `link()` 之后才调用，需要点名另一种材料的组成放在这里。

```java
public final class MyMaterials implements AddonMaterialContributor {
    public String addonId() { return "my_addon"; }
    public List<MaterialDeclaration> materials() {
        return List.of(MaterialDeclaration.builder(4291, "AmmoniumNitrate")
                .displayName("Ammonium Nitrate").color(0xC8D6C0).chemicalFormula("NH4NO3")
                .dust().build());
    }
    public List<MaterialFormRequest> forms() {
        return List.of();
    }
}
```

材料 id 必须在 **4291–7899**（含两端）。内置目录里这段是空的。7900–7912 是原版物质材料，矿石 id 从 8000 起。名字或 id 已被占用时启动失败，并同时写出双方：核心对附属，或附属对附属。附属物品 id 冲突不会加数字后缀。

生成形态注册在 **`gregtech`** 命名空间，走两个加载器已经在用的 `MaterialItemDefinitions` 列表。附属手写的物品、方块和机器留在自己的命名空间。两个加载器在按命名空间分支时把 `gregtech:` 当成 GT 内容，包括食物物品查找，所以生成出来的附属形态也走这些检查。新材料只拿到这份列表按它的属性已经会生成的前缀：粉物品来自 `dust()`。`forms()` 只能点名这些前缀。列表已经发出的一对不会再注册第二次。不在这份列表里的前缀，包括 `crystal`，启动失败。`crystal` 有校验器，但内置目录从不生成它，所以没有图标，也没有 `item.gregtech.crystal` 翻译。已生成的前缀如果过不了 `isValidFor`，例如钻石的板，同样启动失败。附属不能新增前缀。材料表在启动时固定，数据包重载不能增删。旧存档不迁移。

翻译键是 `material.gregtech.<小写名>`，来自 `GTMaterial.getTranslationKey()`。放在附属自己的 `assets/gregtech/lang/en_us.json` 和 `zh_cn.json`。Minecraft 按键把这些文件和 GT 的语言文件合并。附属材料的提示用 `tooltip.gregtech.material_from_addon`（英文 “Added by the %s addon”）。共享中文文件里这个新键保留英文回退，因为它不是原版 GT6 译文。

`dust()` 材料（例如硝酸铵）生成 **6** 个物品。铁的自动形态是 **96** 个。一种金属因此可能增加大约一百个物品。Phase C 配方生成器会走过每一种材料，所以新材料会得到它的属性已经意味着的熔炉、粉碎机等配方行。卸掉附属后，这些 `gregtech:` 物品按 Minecraft 原有规则变成缺失物品。坩埚内容存的是材料数字 id，读档时跳过已经解析不到的 id，所以存档里的附属材料变成无效哨兵，而不是把加载弄崩。这条坩埚路径由现有 NBT 读取覆盖；卸掉附属后再开世界还没有实测。

## 配方机器

在 `META-INF/services/com.gregtech.gregtech.api.addon.AddonMachineContributor` 里列出 `AddonMachineContributor`。GT 在材料初始化之后、注册机器方块之前，用自己的类加载器读这个文件。`recipeMaps()`、`machines()`、`layouts()` 返回纯数据。不要自己注册方块。外壳材料写材料名。能源写短名，例如 `EU`。

GregTech 在自己的模组总线上，用附属的命名空间注册方块、物品和方块实体。方块 id 是 `my_addon:press`，方块实体 id 是 `my_addon:be_press`。内置 id 仍是 `gregtech:<路径>` 和 `gregtech:be_<路径>`。Forge 可能对这条总线打出 `Potentially Dangerous alternative prefix`；这次用的仍是 Forge 的 `DeferredRegister`。NeoForge 接受在 GT 总线上 `DeferredRegister.create(registry, addonNamespace)`。这个选择来自两个加载器的 API，还没有用启动游戏来确认。所有基础机器共用一个菜单 `gregtech:basic_machine`。客户端菜单按机器类型名查配方表。

`addonId()` 是加载器模组 id。`gregtech`、`gregtech6`、`minecraft` 保留。不同命名空间可以重复注册路径。机器类型名是全局的，不能占用内置目录或结构表里的名字。同一附属的两台机器可以共用类型名，条件是配方表、控制器种类、效率、恒定耗能和结构说明键一致。另一个附属不能再用这个名字。

机器只能是 `MachineDeclaration.Kind.BASIC` 或 `LARGE_RECIPE`。蒸馏塔、物质制造机和其他内置控制器留在 GT 里。`efficiency` 是万分比，范围 1 到 100000。`constantEnergy` 是非计时规则：机器不是 TU 机器时按恒定功率耗能。并行用 `MachineDeclaration.parallel`，默认 1。内置机器仍使用原来按名字算出的效率、恒定耗能和并行，这些值在构造内置 spec 时写进字段。

`LARGE_RECIPE` 需要一张同名的 `MachineLayout`。格子是 `right`、`up`、`back`、零件名和 `StructureRole`。控制器在原点，原点不能再放零件。零件名用 `SharedLargeMachineParts` 的名字、`air`，或 `tank_wall`、`tank_wall_dense`、`centrifuge_part`、`heat_transmitter`、`distillation_tower_part`、`electrolyzer_part`。没有附属自己的外壳或端口。空结构、重复坐标、未知零件、没有 `LARGE_RECIPE` 机器的结构、没有结构的 `LARGE_RECIPE` 机器、未知能量类型、未知配方表，都会在启动时报错并写出附属 id。配方表名用 `[a-z0-9_.]+`，必须是这个入口返回的表，或一张内置机器配方表。燃料表不是机器配方表。`structureKeys` 是结构提示的翻译键，字符集是 `[a-z0-9_.]+`。

GUI 路径如果含有 `命名空间:`，就保留这个命名空间。`textures/gui/machines/Press` 和 `gregtech:textures/gui/machines/Gas` 仍解析成 `gregtech:textures/gui/recipes/<文件名>.png`。附属路径 `my_addon:textures/gui/machines/press` 解析成 `my_addon:textures/gui/recipes/press.png`，文件放在 `assets/my_addon/textures/gui/recipes/press.png`。方块模型是 `<命名空间>:block/machine/basic/<机器类型名>`，再加 `_running` 和 `_active`。GT 只给 `gregtech` 命名空间生成物品模型，附属自己带模型和方块状态。显示名是 `block.<命名空间>.<id>`。

新配方表在 `onRecipesReady`、JEI、EMI 和数据包基线之前创建，并进入 `RecipeMap.RECIPE_MAP_LIST`。配方行从 `onRecipesReady` 加入，或用 `gregtech:machine` 数据包。JEI 催化剂和多方块预览跟着已注册的机器列表；有结构的大型机器会出现在预览里。合成配方由附属自己的数据包提供，GT 不生成。

版本 4 不提供附属外壳、自定义方块实体子类、自定义结构检查、大烤炉线圈二选一、面向相邻方块的输入输出、燃料产能模板、新前缀、新流体种类、KubeJS 插件、材料 id 分配，也不迁移 gt6-crops 这类旧附属。已经注册的 GT 流体字段名可以通过 `AddonFluids` 取到。KubeJS 仍用 `gregtech:machine` 给已经存在的配方表加行，包括这个入口创建的表。内置机器继续走 `OriginalLargeRecipeMachineData` 的名字分支。附属大型机器走默认端口。

## 构建附属

按目标加载器构建。Forge 运行 `gradlew.bat :addonSdkJar`，得到 `build/addon-sdk/gregtech6-addon-sdk-1.20.1-forge-0.4.0.jar`。NeoForge 运行 `gradlew.bat :neoforge:addonSdkJar`，得到 `neoforge/build/addon-sdk/gregtech6-addon-sdk-1.21.1-neoforge-0.4.0.jar`。每个 jar 是该加载器的只供编译 SDK：核心接口加上该加载器的平台接口，用 Mojang 名，并带许可声明。没有素材，也没有模组描述文件。用 `compileOnly files(...)` 依赖这一个 jar。不要再次打包或 shade 这些类。运行时由已安装的 GT 提供。Forge 生产 jar 是 SRG，不是编译依赖。目前没有 Maven 坐标，从构建目录或 CI 产物取 jar。

附属须声明必需依赖模组 **`gregtech6`**，并写 `versionRange="[0.4,)"`。GT 内容和存档命名空间仍是 **`gregtech`**。Forge 和 NeoForge 各编一份。一个 SDK 不会让 Forge 二进制在 NeoForge 上运行。

## 支持范围

当前支持注册和配方就绪生命周期；`AddonMaterialContributor` 在 4291–7899 号段排队材料和已有前缀的追加形态（生成 `gregtech:` 物品），读取用 `GregTechMaterials`，前缀是否存在用 `GregTechPrefixes.exists`；`AddonItems.stack`；`AddonFluids` 按 GT 流体字段名取静液或流动液（`DistW`，不是材料名）；已有配方表上的 `AddonRecipes`（添加、展示、碰撞、物品输入检查、动态 provider、查找，`add` 返回 null 表示被拒绝）；`AddonMachineContributor` 的配方机器和大型配方机器，能源用短名例如 `EU`，多方块用现有 GT 零件名，由 GT 注册到附属命名空间，结构角色是 `StructureRole`；`GTValues`、`GTVoltageTiers`、`BasicMachineEnergy`、`MachineWorkCost`；附属实现的 `EnergyBlock`，`AddonEnergy` 走 GT 的能量包门槛，EU 电缆的插入和抽出接受这个接口，其他能量网络仍只看见 GT 自己的能量方块；`AddonTool` 的 `SPADE`、`UNIVERSAL_SPADE`、`MAGNIFYING_GLASS`，以及 `AddonTools.matches`、`isMagnifyingGlass`、`damageForUse`；`gregtech:machine` 与 `gregtech:machine_removal` 两种数据包配方；`GtSignalCarrier`、`GtSignalSource`、`GtSignalReceiver`、`CropScanSource` 和空的 `gregtech:signal_wire_receivers` 方块标签。

这些保持关闭。`BasicMachineBlock` 和 `BasicMachineBlockEntity` 是 GT 的机器实现，附属机器是数据，不是子类。`Recipe.mRecipeList` 是内部行表，正式代码用 `add` 返回的 `AddonRecipes.Line`。`GTMaterial`、`GTMaterialRegistry` 和材料哨兵由 `GregTechMaterials` 代替。`GregTechTags` 由能源短名代替。`MachineRecipeMaps` 的字段由 `AddonRecipes.map(name)` 代替。`MaterialPrefix` 常量由前缀名字符串代替。完整工具目录留在 GT 里。

`onRecipesReady` 仍不是数据包重载事件。API 版本 4 不承诺新前缀、附属外壳、自定义机器方块实体、燃料产能模板、不经入口就用数据包新建配方表、材料表的数据包重载、旧存档迁移、一份二进制同时跑两个加载器，或完整旧 GregAPI 替代。1.0 之前仅文档列出的入口作为兼容面。电压等级以[原GT6电压说明](VOLTAGE_TIERS.md)为准。

SDK与项目代码采用LGPL-3.0-or-later；原作者、第三方代码和资产各自声明继续保留，见根LICENSE、COPYING、NOTICE。
