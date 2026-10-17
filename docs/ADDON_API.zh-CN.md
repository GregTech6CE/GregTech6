# Addon 接口

[English](ADDON_API.md) | [中文](ADDON_API.zh-CN.md)

API版本 **3** 在 Forge 1.20.1 与 NeoForge 1.21.1 保留版本 2 的入口，并加上配方机器。`GregTechAddon` 和 `GregTechMaterialContributor` 没有删除方法，`onRecipesReady` 不变。用 `context.apiVersion() >= 2` 判断，不要要求等于 2。配方仍在自己的 `@Mod` 构造方法里注册 `GregTechAddon`；GT完成材料/物品关联和内置机器配方后，在common setup调用一次 `onRecipesReady`。按Addon模组ID排序调用；重复ID、晚注册、二次派发明确报错，回调异常使初始化失败，生命周期记为FAILED。

新材料和追加形态走单独的 `ServiceLoader` 入口，见下文。不要在模组构造方法里提交材料：Forge 1.20.1 上附属构造和 GT 构造会重叠。

```java
GregTechAddons.register(new GregTechAddon() {
    public String id() { return "my_addon"; }
    public void onRecipesReady(Context context) {
        // context.platform(): forge / neoforge
        // context.minecraftVersion(): 1.20.1 / 1.21.1
        // 此时查询原有GT材料，往MachineRecipeMaps加入Addon配方。
    }
});
```

导入 `com.gregtech.gregtech.api.addon.GregTechAddon` 与 `GregTechAddons`。可直接使用现有 `GTMaterialRegistry`、`MaterialPrefix`、`GTValues.U`、`GTVoltageTiers`，机器配方仍进入原有 `MachineRecipeMaps`，不建立另一套配方底层。英文说明提供完整配方示例；你的输入物品须先由自己的加载器注册。`onRecipesReady` 只在启动时走一次。数据包和 KubeJS 走下面的覆盖层，不会再次调用这个回调。

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

`RecipeMap.addFakeRecipe` 写入给 JEI 和 EMI 看的展示行。`findRecipe` 会跳过这些假配方，机器不会按它们加工。addon 可以用它展示一条动态结果的例子，和 Bumblelyzer 在配方页上放一条扫描示例一样。展示行不能代替 provider：机器要算出真正的产出，仍然要调用 `addDynamicRecipes`。

## 材料与形态

在 `META-INF/services/com.gregtech.gregtech.api.addon.GregTechMaterialContributor` 里列出 `GregTechMaterialContributor`。GT 在材料初始化开始时用自己的类加载器读这个文件，早于内置目录的 `declare()`。返回纯数据。服务构造方法以及 `materials()` / `forms()` 里不要碰 `Materials`。`link()` 在内置目录 `link()` 之后才调用，需要点名另一种材料的组成放在这里。

```java
public final class MyMaterials implements GregTechMaterialContributor {
    public String addonId() { return "my_addon"; }
    public List<MaterialDefinition> materials() {
        return List.of(MaterialDefinition.builder(4291, "AmmoniumNitrate")
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

在 `META-INF/services/com.gregtech.gregtech.api.addon.GregTechMachineContributor` 里列出 `GregTechMachineContributor`。GT 在材料初始化之后、注册机器方块之前，用自己的类加载器读这个文件。`recipeMaps()`、`machines()`、`layouts()` 返回纯数据。不要自己注册方块，服务构造方法里不要碰 `Materials`。`machines()` 在材料初始化之后才调用，这里可以用 `GTMaterialRegistry.get`。

GregTech 在自己的模组总线上，用附属的命名空间注册方块、物品和方块实体。方块 id 是 `my_addon:press`，方块实体 id 是 `my_addon:be_press`。内置 id 仍是 `gregtech:<路径>` 和 `gregtech:be_<路径>`。Forge 可能对这条总线打出 `Potentially Dangerous alternative prefix`；这次用的仍是 Forge 的 `DeferredRegister`。NeoForge 接受在 GT 总线上 `DeferredRegister.create(registry, addonNamespace)`。这个选择来自两个加载器的 API，还没有用启动游戏来确认。所有基础机器共用一个菜单 `gregtech:basic_machine`。客户端菜单按机器类型名查配方表。

`addonId()` 是加载器模组 id。`gregtech`、`gregtech6`、`minecraft` 保留。不同命名空间可以重复注册路径。机器类型名是全局的，不能占用内置目录或结构表里的名字。同一附属的两台机器可以共用类型名，条件是配方表、控制器种类、效率、恒定耗能和结构说明键一致。另一个附属不能再用这个名字。

机器只能是 `MachineControllerKind.BASIC` 或 `LARGE_RECIPE`。蒸馏塔、物质制造机和其他内置控制器留在 GT 里。`efficiency` 是万分比，范围 1 到 100000。`constantEnergy` 是非计时规则：机器不是 TU 机器时按恒定功率耗能。并行用 `BasicMachineParameters` 的 `parallel`，默认 1。内置机器仍使用原来按名字算出的效率、恒定耗能和并行，这些值在构造内置 spec 时写进字段。

`LARGE_RECIPE` 需要一张同名的 `AddonMachineLayout`。格子是 `right`、`up`、`back`、零件名和 `StructureGrid.Role`。控制器在原点，原点不能再放零件。零件名用 `SharedLargeMachineParts` 的名字、`air`，或 `tank_wall`、`tank_wall_dense`、`centrifuge_part`、`heat_transmitter`、`distillation_tower_part`、`electrolyzer_part`。没有附属自己的外壳或端口。空结构、重复坐标、未知零件、没有 `LARGE_RECIPE` 机器的结构、没有结构的 `LARGE_RECIPE` 机器、未知能量类型、未知配方表，都会在启动时报错并写出附属 id。配方表名用 `[a-z0-9_.]+`，必须是这个入口返回的表，或一张内置机器配方表。燃料表不是机器配方表。`structureKeys` 是结构提示的翻译键，字符集是 `[a-z0-9_.]+`。

GUI 路径如果含有 `命名空间:`，就保留这个命名空间。`textures/gui/machines/Press` 和 `gregtech:textures/gui/machines/Gas` 仍解析成 `gregtech:textures/gui/recipes/<文件名>.png`。附属路径 `my_addon:textures/gui/machines/press` 解析成 `my_addon:textures/gui/recipes/press.png`，文件放在 `assets/my_addon/textures/gui/recipes/press.png`。方块模型是 `<命名空间>:block/machine/basic/<机器类型名>`，再加 `_running` 和 `_active`。GT 只给 `gregtech` 命名空间生成物品模型，附属自己带模型和方块状态。显示名是 `block.<命名空间>.<id>`。

新配方表在 `onRecipesReady`、JEI、EMI 和数据包基线之前创建，并进入 `RecipeMap.RECIPE_MAP_LIST`。配方行从 `onRecipesReady` 加入，或用 `gregtech:machine` 数据包。JEI 催化剂和多方块预览跟着已注册的机器列表；有结构的大型机器会出现在预览里。合成配方由附属自己的数据包提供，GT 不生成。

版本 3 不提供附属外壳、自定义方块实体子类、自定义结构检查、大烤炉线圈二选一、面向相邻方块的输入输出、燃料产能模板、新前缀、新流体、KubeJS 插件、材料 id 分配，也不迁移 gt6-crops 这类旧附属。KubeJS 仍用 `gregtech:machine` 给已经存在的配方表加行，包括这个入口创建的表。内置机器继续走 `OriginalLargeRecipeMachineData` 的名字分支。附属大型机器走默认端口。

运行 `gradlew.bat :core:addonApiJar` 得到 `core/build/libs/gregtech6-addon-api-0.0.0.jar`。将其作为 `compileOnly files(...)` 开发依赖，**不要再次打包或shade这些类**；运行时由已安装的GT提供。SDK只包含共享领域和接口、许可声明，不含Minecraft素材。使用平台配方/物品栈/能源适配时还须对应版本的GT开发源码/开发依赖；生产重映射JAR不是通用开发依赖。目前没有发布Maven坐标。

Addon须声明必需依赖模组 **`gregtech6`**；GT内容和保存命名空间仍为 **`gregtech`**。分别构建Forge和NeoForge版本，SDK不会让Forge二进制自动跨加载器运行。

当前支持注册和配方就绪生命周期、4291–7899 号段的排队材料和已有前缀的追加形态（生成 `gregtech:` 物品）、已有材料查询、现有机器配方、吃 `GregTechTags.Energy` 的配方机器和用现有 GT 外壳的大型配方机器（由 GT 注册到附属命名空间）、能源合同、`gregtech:machine` 与 `gregtech:machine_removal` 两种数据包配方，以及 `GtSignalCarrier`、`GtSignalSource`、`GtSignalReceiver` 和空的 `gregtech:signal_wire_receivers` 方块标签。`onRecipesReady` 仍不是数据包重载事件。API 版本 3 不承诺新前缀、流体、附属外壳、自定义机器方块实体、燃料产能模板、不经入口就用数据包新建配方表、修改动态配方、材料表的数据包重载、旧存档迁移、跨加载器物品栈或完整旧 GregAPI 替代。0.0.0 阶段仅文档列出的入口作为兼容面，内部实现可能调整。电压等级以[原GT6电压说明](VOLTAGE_TIERS.md)为准。

SDK与项目代码采用LGPL-3.0-or-later；原作者、第三方代码和资产各自声明继续保留，见根LICENSE、COPYING、NOTICE。
