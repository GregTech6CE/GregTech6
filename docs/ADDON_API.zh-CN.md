# Addon 接口

[English](ADDON_API.md) | [中文](ADDON_API.zh-CN.md)

## 概述

API 版本 **0** 对应模组版本 **0.0.0**，平台是 Forge 1.20.1 与 NeoForge 1.21.1。按目标加载器编译对应的 SDK。SDK 里只有附属接口，没有 GT 内部类。配方仍在自己的 `@Mod` 构造方法里注册 `GregTechAddon`；GT 完成材料/物品关联和内置机器配方后，在 common setup 调用一次 `onRecipesReady`。按 Addon 模组 ID 排序调用；重复 ID、晚注册、二次派发明确报错，回调异常使初始化失败，生命周期记为 FAILED。

旧的材料入口和机器入口在已安装的 GT 里仍会加载。它们已弃用，不在 SDK 里。新代码用 `AddonMaterialContributor` 和 `AddonMachineContributor`。

## 目录

- [快速上手](#快速上手)
  - [基于 SDK 构建](#基于-sdk-构建)
  - [注册附属](#注册附属)
  - [在 `onRecipesReady` 里添加配方](#在-onrecipesready-里添加配方)
- [配方](#配方)
  - [机器配方数据包](#机器配方数据包)
  - [展示行](#展示行)
  - [多个动态配方 provider](#多个动态配方-provider)
- [材料](#材料)
  - [材料与形态](#材料与形态)
  - [前缀](#前缀)
  - [世界生成](#世界生成)
  - [坩埚](#坩埚)
- [机器](#机器)
  - [注册机器](#注册机器)
  - [外壳](#外壳)
  - [能量](#能量)
  - [物品和流体](#物品和流体)
  - [结构](#结构)
  - [配方表](#配方表)
  - [配方机器](#配方机器)
  - [行为机器](#行为机器)
  - [燃料机](#燃料机)
  - [管子](#管子)
  - [压缩气罐](#压缩气罐)
- [信号线](#信号线)
- [范围](#范围)
- [版本与兼容性](#版本与兼容性)
  - [版本策略](#版本策略)
  - [变更](#变更)
  - [弃用与移除](#弃用与移除)
- [许可](#许可)

## 快速上手

配置依赖和 SDK，注册附属，再添加配方。

### 基于 SDK 构建

按目标加载器构建。Forge 运行 `./gradlew :multiversion:1.20.1-forge:addonSdkJar`，得到 `multiversion/versions/1.20.1-forge/build/addon-sdk/gt6ce-addon-sdk-1.20.1-forge-0.0.0.jar`。NeoForge 运行 `./gradlew :multiversion:1.21.1-neoforge:addonSdkJar`，得到 `multiversion/versions/1.21.1-neoforge/build/addon-sdk/gt6ce-addon-sdk-1.21.1-neoforge-0.0.0.jar`。Windows 上把 `./gradlew` 换成 `gradlew.bat`。每个 jar 是该加载器的只供编译 SDK：核心接口加上该加载器的平台接口，用 Mojang 名，并带许可声明（`META-INF/gregtech6`）。没有素材、没有模组描述文件，也没有单独的 `-sources.jar` 或 `-javadoc.jar`；在 IDE 里把 `core/src/api/java` 和 `multiversion/src/api/java` 挂为源码目录即可查看源码和 Javadoc。用 `compileOnly files(...)` 依赖这一个 jar。不要再次打包或 shade 这些类。运行时由已安装的 GT 提供。Forge 生产 jar 是 SRG，不是编译依赖。目前没有 Maven 坐标，从构建目录或 CI 产物取 jar。

仓库里的 [`examples/addon-sample/`](../examples/addon-sample/)（`forge/` 和 `neoforge/`）是完整可运行的样例工程，覆盖材料、前缀、世界生成、坩埚反应、配方机器、多方块、功能零件、行为机器、燃料机、管子和压缩气罐。在 GT6CE 仓库根目录下，运行 `./gradlew :multiversion:1.20.1-forge:compileAddonSample :multiversion:1.20.1-forge:addonApiClosureCheck`（或 `1.21.1-neoforge`）即可用 SDK 编译样例并检查没有引用内部类；在 `runClient` 或 `runServer` 时加上 `-PaddonSample=true` 即可把样例放进开发运行。

独立的附属仓库在 `build.gradle` 里用 `compileOnly files(...)` 引入 SDK，开发运行时还要带上构建好的 GT6CE 模组 jar。NeoForge 1.21.1 的 jar 本身是 Mojang 名，直接放进 `run/mods/` 或加入 `runtimeOnly files(...)` 即可。Forge 1.20.1 的发布 jar（`distributionJar`）经过 reobf，是 SRG 名，而 ForgeGradle 开发环境用 Mojang 名；`fg.deobf` 只反混淆模块坐标（对 `files(...)` 只打警告不反混淆），所以要配一个 `flatDir` 仓库再用 `fg.deobf(...)` 引入，不能直接丢进 `run/mods/`：

```groovy
repositories {
    flatDir { dir "libs" }
}

dependencies {
    compileOnly files("libs/gt6ce-addon-sdk-1.20.1-forge-0.0.0.jar")
    runtimeOnly fg.deobf("blank:gt6ce-1.20.1-forge:0.0.0") // 对应 libs/gt6ce-1.20.1-forge-0.0.0.jar
}
```

附属须声明必需依赖模组 **`gt6ce`**，写 `versionRange="[0.0.0,)"`、`ordering="NONE"`、`side="BOTH"`。Java 包名沿用 `com.gregtech.gregtech.*`，加载器模组 id 以及内容和存档命名空间是 **`gt6ce`**，`gregtech6` 只是保留名（防止被附属用作 `addonId()`）。只要附属注册了材料、前缀、机器、结构零件、管子或储罐，就会新增注册项，客户端和服务端都必须安装（`side="BOTH"`）；纯数据包配方改动（`gt6ce:machine` 与 `gt6ce:machine_removal`）放在服务端数据包里，由原版配方同步发给客户端。Forge 和 NeoForge 各编一份。一个 SDK 不会让 Forge 二进制在 NeoForge 上运行。

```toml
# Forge: META-INF/mods.toml
[[dependencies.my_addon]]
modId="gt6ce"
mandatory=true
versionRange="[0.0.0,)"
ordering="NONE"
side="BOTH"

# NeoForge: META-INF/neoforge.mods.toml
[[dependencies.my_addon]]
modId="gt6ce"
type="required"
versionRange="[0.0.0,)"
ordering="NONE"
side="BOTH"
```

### 注册附属

```java
import com.gregtech.gregtech.api.addon.GregTechAddon;
import com.gregtech.gregtech.api.addon.GregTechAddons;

// 在 Forge 或 NeoForge 的 @Mod 构造方法里调用。
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

新材料和追加形态走单独的 `ServiceLoader` 入口，见下文。不要在模组构造方法里提交材料：Forge 1.20.1 上附属构造和 GT 构造会重叠。

启动按五个阶段顺序执行：

1. **模组构造（`@Mod` 构造方法）**：调用 `GregTechAddons.register(GregTechAddon)`，并注册附属自己持有的 `DeferredRegister`。不要在这里查询 `GregTechMaterials`、`AddonItems` 或 `AddonRecipes`。
2. **材料初始化（`GTMaterialRegistry.init`）**：GT 通过 `ServiceLoader` 加载 `AddonMaterialContributor`，并一次性调用它的全部列表方法。`materials()` 在内置目录之后声明；内置材料链接完成后，依次应用 `compositions()`、`smeltingTargets()`、`crucibleReactions()`、`crucibleRemovals()`，调用 `link()`，再发布矿石副产物（`MaterialDeclaration.Builder.oreByproducts`）、`oreVeins()`、`smallOres()`、`prefixes()` 和 `forms()`。
3. **机器与管子注册（`AddonMachineRuntime.install`）**：GT 通过 `ServiceLoader` 加载 `AddonMachineContributor`、`BehaviorMachineContributor` 和 `LayoutPartContributor`，创建 `recipeTables()`，并在 GT 自己的模组总线上把机器、结构零件、管子和压缩气罐注册进 `<附属 id>:` 命名空间。
4. **Common setup（`FMLCommonSetupEvent`）**：完成材料与物品关联及内置机器配方加载后，按 `id()` 升序调用一次 `GregTechAddon.onRecipesReady(Context)`，随后校验 `exclusiveOutputs` 配方表并解析 `StructureLayout` 里的普通方块 id。
5. **开服与 `/reload`**：恢复启动配方快照（内置行加 `onRecipesReady` 行），按当前标签重建矿石和兼容层配方，再应用 `gt6ce:machine_removal` 和 `gt6ce:machine` 数据包。

列在 `META-INF/services/...` 里的每个贡献者类都必须是带 `public` 无参构造方法的 `public` 类。同一个类可以同时实现多个贡献者接口（例如像 `AddonSampleMachines` 那样同时实现 `AddonMachineContributor` 和 `BehaviorMachineContributor`）；`ServiceLoader` 会按服务接口分别实例化，因此贡献者类应当是无状态的。Forge 和 NeoForge 把所有模组 jar 放进同一个游戏层，所以 GT 的类加载器通过 `ServiceLoader` 也能找到附属 jar 里的实现类。必需依赖 `gt6ce` 保证的是 GT 已安装，类可见性并不靠它。下文凡是写**启动失败**的地方，GT 都会抛出 `IllegalArgumentException` 或 `IllegalStateException`，异常会打在 `logs/latest.log` 和崩溃报告里，消息带上附属 id 和出错的条目名；贡献者方法自己抛出的异常则包一层，消息里写的是贡献者类名。

### 在 `onRecipesReady` 里添加配方

下面是一个完整的配方回调示例（其中 `MyItems.MY_ORE` 由你的附属自己注册）：

```java
import com.gregtech.gregtech.api.addon.platform.AddonItems;
import com.gregtech.gregtech.api.addon.platform.AddonRecipes;
import net.minecraft.world.item.ItemStack;

var map = AddonRecipes.map("gt.recipe.mortar");
var recipe = new AddonRecipes.Line(
        new ItemStack[]{new ItemStack(MyItems.MY_ORE.get())},
        new ItemStack[]{AddonItems.stack("dust", "Copper")},
        null, null, 20, 16, 0);
if (map.add(recipe) == null)
    throw new IllegalStateException("my_addon mortar recipe rejected");
```

材料用 `GregTechMaterials`，物品用 `AddonItems`，已有配方表用 `AddonRecipes`。`GTValues`、`GTVoltageTiers`、`BasicMachineEnergy` 和 `MachineWorkCost` 也在 SDK 里。`onRecipesReady` 只在启动时走一次。数据包和 KubeJS 走下面的覆盖层，不会再次调用这个回调。

`AddonRecipes.Line` 有三个构造方法：

- `Line(ItemStack[] itemInputs, ItemStack[] itemOutputs, FluidStack[] fluidInputs, FluidStack[] fluidOutputs, long duration, long euPerTick, long special)`
- `Line(..., int[] outputChances)`
- `Line(..., int[] outputChances, int[] keptInputs)`

不用的物品或流体数组可以传 `null` 或空数组（`FluidStack` 在 Forge 上是 `net.minecraftforge.fluids.FluidStack`，在 NeoForge 上是 `net.neoforged.neoforge.fluids.FluidStack`）。`duration` 是加工时长（单位 tick，至少为 1）。`euPerTick` 是每 tick 能量（机器配方表上为非负数；燃料表上把产出能量写成负数）。`special` 是配方表附加参数（普通配方填 `0`，坩埚展示表填开尔文温度）。七个参数的 `Line` 表示每个物品输出都必定产出。需要概率时，按物品输出各给一个 `int`，单位是万分比。`10000` 是必定。空数组和不写一样。长度必须和物品输出一致，每个值是 1 到 10000。流体输出仍是必定。机器加工、JEI、EMI 和数据包覆盖层读的是配方行上的同一个概率。数据包物品输出本来就有 `chance` 字段。

`AddonItems.selectorTag(n)` 是 n 号编程电路，物品是 `integrated_circuit_0` 到 `integrated_circuit_24`，数量是 1。n 不在 0 到 24 就启动失败。编程电路本身是催化剂物品，机器只检查有没有，不扣除。把它的下标写进 `keptInputs` 也可以，结果不变。

`keptInputs` 是 `Line` 的最后一个数组，列出不消耗的物品输入下标。每一项数量仍写成 1 或更多。不写或空数组表示都不标记，这一行匹配的输入和以前一样。下标越界或重复会启动失败。数量 0 仍然启动失败，错误信息会指向 `keptInputs`。EMI 把标记的输入画成催化剂，和内置催化剂输入一样。数据包覆盖层不变：那里的数量 0 仍被拒绝，覆盖层也没有这个字段。

```java
var circuit = new AddonRecipes.Line(
        new ItemStack[]{new ItemStack(MyItems.MY_ORE.get()), AddonItems.selectorTag(3)},
        new ItemStack[]{AddonItems.stack("dust", "Copper")},
        null, null, 20, 16, 0);
var kept = new AddonRecipes.Line(
        new ItemStack[]{new ItemStack(MyItems.MY_ORE.get()), new ItemStack(MyItems.MY_LENS.get())},
        new ItemStack[]{AddonItems.stack("dust", "Tin")},
        null, null, 20, 16, 0, null, new int[]{1});
```

## 配方

添加、展示和移除机器配方行。

### 机器配方数据包

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

内置机器配方表名（`AddonRecipes.map(name)` 和数据包 `"map"` 都可直接使用）：

- 加工与成型：`mc.recipe.furnace`、`gt.recipe.microwave`、`gt.recipe.roaster`、`gt.recipe.distillery`、`gt.recipe.extruder`、`gt.recipe.smelter`、`gt.recipe.crystallisationcrucible`、`gt.recipe.drying`、`gt.recipe.laminator`、`gt.recipe.catalyticcracking`、`gt.recipe.steamcracking`、`gt.recipe.distillationtower`、`gt.recipe.cryodistillationtower`、`gt.recipe.shredder`、`gt.recipe.lathe`、`gt.recipe.cutter`、`gt.recipe.centrifuge`、`gt.recipe.rollingmill`、`gt.recipe.rollbender`、`gt.recipe.rollformer`、`gt.recipe.clustermill`、`gt.recipe.wiremill`、`gt.recipe.mixer`、`gt.recipe.loom`、`gt.recipe.sluice`、`gt.recipe.sharpener`、`gt.recipe.burnmixer`、`gt.recipe.pressurewasher`、`gt.recipe.crusher`、`gt.recipe.sifter`、`gt.recipe.squeezer`、`gt.recipe.compressor`、`gt.recipe.press`、`gt.recipe.electrolyzer`、`gt.recipe.canner`、`gt.recipe.injector`、`gt.recipe.printer`、`gt.recipe.scannervisuals`、`gt.recipe.scannermolecular`、`gt.recipe.autocrafting`、`gt.recipe.slicer`、`gt.recipe.nanofab`、`gt.recipe.plantalyzer`、`gt.recipe.bumblelyzer`、`gt.recipe.boxinator`、`gt.recipe.unboxinator`、`gt.recipe.polarizer`、`gt.recipe.magneticseparator`、`gt.recipe.laserengraver`、`gt.recipe.welder`、`gt.recipe.freezer`、`gt.recipe.cryomixer`、`gt.recipe.massfab`、`gt.recipe.replicator`、`gt.recipe.autoclave`、`gt.recipe.bath`、`gt.recipe.generifier`、`gt.recipe.coagulator`、`gt.recipe.fermenter`、`gt.recipe.melter`、`gt.recipe.cokeoven`、`gt.recipe.lightning`、`gt.recipe.implosioncompressor`、`gt.recipe.fusionreactor`、`gt.recipe.mortar`、`gt.recipe.cooker`、`gt.recipe.blastfurnace`、`gt.recipe.vacuumfreezer`、`gt.recipe.assembler`、`gt.recipe.cncmachine`、`gt.recipe.hammer`、`gt.recipe.chisel`、`gt.recipe.calciner`、`gt.recipe.juicer`、`gt.recipe.anvil`、`gt.recipe.anvil.bend.small`、`gt.recipe.anvil.bend.big`。
- 展示与速查表：`gt.recipe.cruciblealloying`、`gt.recipe.cruciblesmelting`、`gt.recipe.bedrockorelist`、`gt.recipe.byproductlist`、`gt.recipe.bumblequeen`、`gt.recipe.trees`、`gt.recipe.other`、`gt.recipe.toolhead`。
- 内置燃料配方表：`gt.recipe.fuels.fluidbed`、`gt.recipe.fuels.burn`、`gt.recipe.fuels.gas`、`gt.recipe.fuels.hot`、`gt.recipe.fuels.plasma`、`gt.recipe.fuels.engine`、`gt.recipe.fuels.turbine`、`gt.recipe.fuels.magic`。

在 GT6CE 源码仓库里启动开发运行时，可以用 `-PkubejsRuntime=true` 装上 KubeJS。这个依赖不进成品，也不占用 `-PcompatRuntime`。

### 展示行

`AddonRecipes.Map.addDisplay` 写入给 JEI 和 EMI 看的展示行。查找会跳过这些行，机器不会按它们加工。addon 可以用它展示一条动态结果的例子，和 Bumblelyzer 在配方页上放一条扫描示例一样。展示行不能代替 provider：机器要算出真正的产出，仍然要调用 `addDynamic`。

### 多个动态配方 provider

固定配方仍然先查。没有固定行命中时，才按这个顺序问动态 provider：`RecipeMap.dynamicRecipes` 装上的内置 provider，然后是每次 `RecipeMap.addDynamicRecipes` 追加的 provider。第一个返回非空配方的 provider 生效，后面的不会再问。返回 null 表示这个输入不归它处理。`dynamicRecipes` 只替换那个内置 provider。`addDynamicRecipes` 往末尾追加一个，不替换内置 provider，也不替换更早追加的 addon provider。

`MachineRecipeMaps.Plantalyzer` 没有内置 provider。addon 可以自己追加，例如作物种子扫描，这不会拆掉 Bumblelyzer、分子扫描、打印机或复制机上已有的 provider。

## 材料

通过 `AddonMaterialContributor` 声明材料、形态、前缀、矿物生成和坩埚行为。

### 材料与形态

在 `META-INF/services/com.gregtech.gregtech.api.addon.AddonMaterialContributor` 里列出 `AddonMaterialContributor`。GT 在材料初始化开始时用自己的类加载器读这个文件，早于内置目录的 `declare()`。返回纯数据。GT 会在这时一次性调用所有列表方法：`materials()`、`forms()`、`prefixes()`、`compositions()`、`smeltingTargets()`、`crucibleReactions()`、`crucibleRemovals()`、`oreVeins()` 和 `smallOres()`。这些方法和服务构造方法里都不要碰 GT 的材料表。`compositions()`、`smeltingTargets()`、`crucibleReactions()` 和 `crucibleRemovals()` 在内置目录 `link()` 之后才应用。`link()` 也在这时调用，用来放这四张表没有覆盖的跨材料链接。

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

`ores()` 给这种材料加上 GT6 的 `ORES` 形态。原矿物品、岩石、不纯粉，以及粉碎、洗涤、离心那一串，因此按现有校验生成。纯粉和精炼粉仍然不生成，它们只看没有材料使用的 `DIRTY_DUSTS`。同时注册 `ore` 和 `oreSmall` 两种方块，追加在目录已经发出的方块之后，内置方块 id 不变。母岩仍是这一个方块上的方块状态，和内置矿石一样。`ores()` 不注册原矿储物块，也不注册原矿箱。矿脉是下面单独的声明。`oreByproducts("Copper", "Tin")` 会打开 `ores()`，并按这个顺序写下副产物；第一个名字是洗涤副产物。不写则副产物为空。洗涤和离心仍走内置的回退。副产物名字必须是启动时存在的材料，不能是隐藏材料。两个附属共用一种材料时，`ores()` 和这份名单必须一致。`forms()` 不能点名 `ore` 或 `oreSmall`。材料没有声明 `ores()` 时，`forms()` 点名 `oreRaw` 仍然失败。

`GregTechMaterials.Material.byproducts()` 返回这份名单，名字是 `resolve()` 之后的材料名，顺序不变。内置材料和附属材料都能读。在 `onRecipesReady` 里读，那时材料已经链接。链接之前调用返回当时记下的名字，不报错。未知材料返回空列表。附属不能改内置材料的副产物。

存档写名字。坩埚内容在 `gt.materials` 里是 `{name, amount}`。坩埚显示字段 `gt.display.material` 是名字，没有要显示的材料时是空字符串。USB 和硬盘文件仍用键 `gt.replicator.data`，值改成名字字符串。旧构建写的数字 `id`，以及数字形式的 `gt.replicator.data` / `gt.display.material`，仍能读回内置材料，读不回附属材料。找不到的名字会跳过，和原来找不到 id 一样。附属物品 id 冲突不会加数字后缀。

生成形态注册在 **`gt6ce`** 命名空间，走两个加载器已经在用的 `MaterialItemDefinitions` 列表。附属手写的物品、方块和机器留在自己的命名空间。两个加载器在按命名空间分支时把 `gt6ce:` 当成 GT 内容，包括食物物品查找，所以生成出来的附属形态也走这些检查。新材料只拿到这份列表按它的属性已经会生成的前缀：粉物品来自 `dust()`。`forms()` 只能点名这些前缀。列表已经发出的一对不会再注册第二次。不在这份列表里的前缀，包括 `crystal`，启动失败。`crystal` 有校验器，但内置目录从不生成它，所以没有图标，也没有 `item.gt6ce.crystal` 翻译。已生成的前缀如果过不了 `isValidFor`，仍然启动失败。例外是附属材料显式请求的 `plate` 和 `foil`：没有锭也会生成这两件物品，不会因此多出双板、密板或杆。钻石的板仍然失败，因为钻石是内置材料。没有声明 `ores()` 的材料，`oreRaw` 仍然失败。`ore` 和 `oreSmall` 是方块前缀，`forms()` 点名它们会失败。其他被拒绝的前缀仍然失败。新前缀用下一节的 `prefixes()` 声明。`forms()` 仍然不能点名生成列表以外的前缀。材料表在启动时固定，数据包重载不能增删。旧的数字 id 在方块或物品加载时读取。存档要等这个方块或物品再次保存才写成名字。

翻译键是 `material.gt6ce.<小写名>`，来自 `GTMaterial.getTranslationKey()`。放在附属自己的 `assets/gt6ce/lang/en_us.json` 和 `zh_cn.json`。Minecraft 按键把这些文件和 GT 的语言文件合并。只有一个附属时，提示用 `tooltip.gt6ce.material_from_addon`（英文 “Added by the %s addon”）。共享中文文件里这个键保留英文回退，因为它不是原版 GT6 译文。多个附属共用一种材料时用 `tooltip.gt6ce.material_from_addons`。共享中文文件里这两个键都保留英文回退，因为它们不是原版 GT6 译文。

`dust()` 材料（例如硝酸铵）生成 **6** 个物品（`dust`、`dustSmall`、`dustTiny`、`dustDiv72`、`unit` 和 `scrapGt`；`dustImpure` 要声明 `ores()` 才会生成）。铁的自动形态是 **96** 个。`metal()` 就是用来声明这一大组形态的。Phase C 配方生成器会走过每一种材料，所以新材料会得到它的属性已经意味着的熔炉、粉碎机等配方行。运行时可通过 `GregTechMaterials.all()` 和 `GregTechMaterials.find(name)` 查询已注册材料（例如 `Iron`、`Copper`、`AnnealedCopper`、`Steel`、`Tin`、`Bronze`、`StainlessSteel`、`Teflon`、`PVC`；`Cr` 与 `Chromium` 等元素符号别名会解析到同一种材料）。`forms()` 与 `AddonItems.stack(prefix, material)` 可用的生成前缀列表包括：

- 粉与矿石处理：`dust`、`dustSmall`、`dustTiny`、`dustDiv72`、`dustImpure`、`unit`、`crushed`、`crushedTiny`、`crushedPurified`、`crushedPurifiedTiny`、`crushedCentrifuged`、`crushedCentrifugedTiny`、`rockGt`、`oreRaw`。
- 宝石与光学：`gemChipped`、`gemFlawed`、`gem`、`gemFlawless`、`gemExquisite`、`gemLegendary`、`bouleGt`、`plateGemTiny`、`plateGem`、`lens`。
- 金属与零件：`nugget`、`chunkGt`、`billet`、`ingot`、`ingotHot`、`ingotDouble`、`ingotTriple`、`ingotQuadruple`、`ingotQuintuple`、`plateTiny`、`plate`、`plateDouble`、`plateTriple`、`plateQuadruple`、`plateQuintuple`、`plateDense`、`plateCurved`、`foil`、`scrapGt`、`stick`、`stickLong`、`bolt`、`screw`、`gearGt`、`gearGtSmall`、`ring`、`chain`、`spring`、`springSmall`、`rotor`、`round`、`itemCasing`（别名 `casingSmall`）、`wireFine`、`minecartWheels`、`railGt`、`glasstube`、`coin`、`arrowGtWood`、`arrowGtPlastic`、`bulletGtSmall`、`bulletGtMedium`、`bulletGtLarge`、`plantGtBerry`、`plantGtBlossom`、`plantGtFiber`、`plantGtTwig`、`plantGtWart`，以及 `toolHead*` 工具头前缀（`toolHeadSword`、`toolHeadPickaxe`、`toolHeadShovel`、`toolHeadAxe`、`toolHeadHoe`、`toolHeadHammer`、`toolHeadFile`、`toolHeadChisel`、`toolHeadSaw`、`toolHeadDrill`、`toolHeadChainsaw`、`toolHeadWrench`、`toolHeadScrewdriver`、`toolHeadSpade`、`toolHeadUniversalSpade`、`toolHeadSense`、`toolHeadPlow`、`toolHeadBuzzSaw`、`toolHeadArrow` 及其 `toolHeadRaw*` 对应项）。

卸掉附属后，这些 `gt6ce:` 物品按 Minecraft 原有规则变成缺失物品。坩埚或 USB 里已经找不到的名字会被跳过，卸掉附属不会把加载弄崩（已知限制：卸载附属后对已有存档做重开覆盖不属于自动回归测试范围）。

### 前缀

`AddonMaterialContributor.prefixes()` 声明物品前缀。GT 在内置前缀之后、检查 `forms()` 之前注册。名字已经存在就启动失败，包括只差大小写、注册名相同，以及 `casingSmall` 这种别名。两个附属不能声明同一个名字。失败信息写出附属 id 和已经占用这个名字的前缀。

```java
public List<PrefixDeclaration> prefixes() {
    return List.of(new PrefixDeclaration("sampleBlank", "", " Sample Blank", 8L,
            "item/sample_blank", List.of("Iron", "SampleAlloy"), Set.of()));
}
```

`unitAmount` 是 U 的正整数个数（`GTValues.U = 648_648_000L`，即 1 个锭或 1 份粉的材料单位量；`GTValues` 还提供 `U2`、`U3`、`U4`、`U9`、`U72` 等常用分数）。一块样例坯在坩埚里和粉碎回收里都是 8U。内置前缀的单位量仍走原来的表。`texture` 是这个附属命名空间下的路径：`item/sample_blank` 对应 `assets/<附属>/textures/item/sample_blank.png`。物品只有这一层，用材料颜色染色。没有贴图组目录，也没有覆盖层。

`displayPrefix` 和 `displaySuffix` 是材料名前后的英文拼法。前缀空、后缀 `" Sample Blank"`，读出来是 “Iron Sample Blank”。翻译键 `item.<附属 id>.<蛇形名>` 带一个 `%s` 时，用译文代替这句拼法。前后缀至少写一个。

`materials` 点名要生成这个前缀的材料，可以是内置材料。`conditions` 可选，取值是 `PrefixCondition`：`HAS_TOOL_STATS`、`SMITHABLE`、`NOT_WOOD`、`NOT_COAL`、`NOT_STONE`、`NOT_FOOD`、`NOT_MEAT`。选中的条件要全部命中。不能传自己的谓词。名单为空时必须带 `HAS_TOOL_STATS` 或 `SMITHABLE`，因为单独的否定条件会铺到几乎全部材料上。带了这两个之一，命中的材料会和点名的材料一起生成。没带时，否定条件只检查点名的材料，对不上就启动失败。

`NOT_COAL` 是 Coal、Charcoal、CoalCoke、Lignite、LigniteCoke、Anthracite、HydratedCoal、PetroleumCoke。食物是 GT6 的食物标记。肉是 MeatRaw、MeatCooked、MeatRotten。

物品注册在**附属自己的**命名空间，排在生成出来的 `gt6ce:` 形态之后。顺序是附属 id、声明顺序、材料名。路径和内置形态一样，例如 `sample_blank_iron`。同一个命名空间里路径撞车就启动失败。它不占用 `gt6ce:` 的 id，也不改内置物品 id 列表。`forms()` 不能点这个前缀，材料由声明上的名单决定。启动之后 `GregTechPrefixes.exists` 对这个名字返回真。`AddonItems.stack` 能取到物品。

GT 不为这个前缀添加加工配方。这一节不包括方块前缀。

### 世界生成

`oreVeins()` 和 `smallOres()` 挂在 `AddonMaterialContributor` 上，分别加大矿脉和小矿。GT 把它们追加在该维度内置行之后。名字是同一维度、同一种类里的身份：大矿脉和小矿可以同名，另一个维度也可以再用这个名字。撞上内置行或另一个附属，启动失败，并写出两边。

```java
public List<OreVeinDeclaration> oreVeins() {
    return List.of(new OreVeinDeclaration("sample_alloy", "minecraft:overworld",
            10, 40, 40, 3, 16, "SampleAlloy", "SampleAlloy", "Copper", "Tin"));
}

public List<SmallOreDeclaration> smallOres() {
    return List.of(new SmallOreDeclaration("sample_alloy", "minecraft:overworld",
            20, 60, 4, "SampleAlloy"));
}
```

字段和内置表一样。`weight`、`density`、`size`、`amount` 都是正数。`minY` 和 `maxY` 用内置表那套重映射之前的高度，落在 0 到 255 里，并且下限低于上限。主世界生成器会把这段高度拉开。大矿脉写四种材料：顶层、底层、中间层、散布层。小矿写一种。每种材料必须存在，并且有矿石方块。附属材料要调用 `ores()`。内置材料必须是本来就会有矿石方块的那种。未知材料或隐藏材料会启动失败。

维度是 `minecraft:overworld`、`minecraft:the_nether` 和 `minecraft:the_end`。下界没有大矿脉生成器，所以大矿脉不能写下界。别的维度直接失败，不为它单做生成器。既不是下界也不是末地的维度，生成时走主世界这张表。

石层仍是 GT 自己的表，附属矿不写进石层。石层开着时，主世界旧的大矿脉保持关闭，生成的是附属大矿脉。它们和这张表里还在的内置矿脉按权重一起抽。权重变了，`nextInt` 的上界也变，所以同一个种子可能抽到另一条内置矿脉。末地的内置大矿脉仍开着，会碰到这件事。小矿是追加在后面的，每一行只用自己的随机数，内置小矿的位置不变。

### 坩埚

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

## 机器

通过 `AddonMachineContributor` 声明机器、结构、配方表、管子和储罐。前几节是各模板共用的组成部分，后几节分别说明每种模板。

### 注册机器

在 `META-INF/services/com.gregtech.gregtech.api.addon.AddonMachineContributor` 里列出 `AddonMachineContributor`。GT 在材料初始化之后、注册机器方块之前，用自己的类加载器读这个文件。`addonMachines()`、`structureLayouts()`、`layoutParts()` 和 `recipeTables()` 返回纯数据。不要自己注册方块。`fluidPipes()`、`itemPipes()` 和 `compressedGasTanks()` 仍在这个贡献者上。管子和压缩气罐写在同名的那两节。

`addonId()` 是加载器模组 id。`gt6ce`、`gregtech6` 和 `minecraft` 保留。注册路径可以在不同命名空间里重复。`machineType` 是全局的：不能占用内置目录或布局名。同一个附属的两台方块，签名对得上时可以共用机器类型。另一个附属不能再用这个名字。

GT 在自己的模组总线上，把方块、物品和方块实体类型注册进附属的命名空间。方块 id 是 `<附属 id>:<id>`。配方机器的方块实体 id 是 `<附属 id>:be_<id>`，`id` 按声明原样使用。行为机器没写方块实体 id 时，以及所有燃料机，用 `be_<id>`，每个 `/` 换成 `_`。内置 id 仍是 `gt6ce:<路径>` 和 `gt6ce:be_<路径>`。Forge 的 `DeferredRegister` 在 GT 总线上注册 `<附属 id>:` 命名空间时会打出一行无害的 `Potentially Dangerous alternative prefix` 警告；NeoForge 直接支持在 GT 总线上调用 `DeferredRegister.create(registry, addonNamespace)`（已由 `addon_sample` 的 GameTest 服务端运行验证）。

所有配方机器共用菜单 `gt6ce:basic_machine`。客户端按机器类型名找配方表。所有行为机器共用菜单 `gt6ce:behavior_machine`。燃料机用普通的机器停机开关，没有自己的菜单。

方块模型解析到 `<命名空间>:block/machine/basic/<机器类型>`，再加 `_running` 和 `_active`。GT 仍只为 `gt6ce` 命名空间生成物品模型。附属自己带模型和方块状态。显示名是 `block.<命名空间>.<id>`。

附属 jar 按所用功能需要准备的资源与服务文件清单：

- `META-INF/services/com.gregtech.gregtech.api.addon.AddonMaterialContributor`（声明材料、前缀、世界生成和坩埚规则时）
- `META-INF/services/com.gregtech.gregtech.api.addon.AddonMachineContributor`（声明机器、配方表、结构布局、结构零件、管子和压缩气罐时）
- `META-INF/services/com.gregtech.gregtech.api.addon.platform.BehaviorMachineContributor`（声明 `BehaviorMachine` 时）
- `META-INF/services/com.gregtech.gregtech.api.addon.platform.LayoutPartContributor`（声明带功能的 `LayoutPart` 时）
- `META-INF/services/com.gregtech.gregtech.api.addon.platform.LayoutPartClient`（仅客户端，功能零件需要方块实体渲染器时）
- 机器方块状态 `assets/<附属 id>/blockstates/<id>.json`、方块模型 `assets/<附属 id>/models/block/machine/basic/<机器类型>.json`（以及 `<机器类型>_running.json` 和 `<机器类型>_active.json`）、物品模型 `assets/<附属 id>/models/item/<id>.json`（结构零件、管子和压缩气罐由 GT 生成默认方块状态与模型，附属可按需覆盖）
- 配方表 GUI 贴图 `assets/<附属 id>/textures/gui/recipes/<文件名>.png`（对应 `.gui("<附属 id>:textures/gui/machines/<文件名>")`）与行为机器 GUI 贴图 `assets/<附属 id>/textures/gui/<贴图>.png`
- 前缀物品贴图 `assets/<附属 id>/textures/<texture>.png` 以及自定义结构零件贴图 `assets/<附属 id>/textures/<texture>.png`
- 语言文件 `assets/<附属 id>/lang/en_us.json`（及 `zh_cn.json`，包含 `block.<附属 id>.<id>`、前缀 `item.<附属 id>.<蛇形名>`、行为开关键、结构提示键）和 `assets/gt6ce/lang/en_us.json`（包含 `material.gt6ce.<小写材料名>`）

```java
public final class MyMachines implements AddonMachineContributor {
    public String addonId() { return "my_addon"; }
    public List<RecipeTable> recipeTables() {
        return List.of(RecipeTable.machine("my_addon.recipe.press")
                .title("Press").gui("my_addon:textures/gui/machines/press")
                .inputItems(1).outputItems(1).minimalInputItems(1).minimalInputs(1)
                .needsOutputs(true).build());
    }
    public List<AddonMachine> addonMachines() {
        return List.of(RecipeMachine.builder(
                MachineCasing.builder("press", "Steel").machineType("mypress").tier(1).build())
                .energy(EnergyInput.builder("EU", 32).build())
                .recipeMap("my_addon.recipe.press").basic().build());
    }
}
```

外壳材料写材料名。能源写短名，例如 `EU`。

### 外壳

`MachineCasing.builder(id, material)` 是三种模板共用的外壳。`id` 匹配 `[a-z0-9/._-]+`，是方块路径。`material` 不能为空白。启动时材料必须已经存在。结构零件用同一套材料查找。

写了 `machineType` 时，它匹配 `[a-z0-9_]+`。不写时三种模板不一样。配方机器和燃料机原样复制 `id`，不替换 `/`。`id` 里有 `/` 就会失败，因为机器类型仍要匹配 `[a-z0-9_]+`。行为机器把每个 `/` 换成 `_`。

接受等级的模板里，`tier` 缺省是 1。配方机器只要求 `tier >= 1`。行为机器要求 1 到 255。燃料机没有等级。外壳调用过 `tier` 时，这台燃料机在 `build()` 失败。

### 能量

进机器的能量是 `EnergyInput`。离开行为机器或能量结构零件的能量是 `EnergyOutput`。燃料机的一包是 `FuelOutput`。配方机器拒绝 `EnergyOutput` 和 `FuelOutput`。行为机器拒绝 `FuelOutput`。燃料机拒绝 `EnergyInput` 和 `EnergyOutput`。

`EnergyInput.builder(type, perTick)` 和 `FuelOutput.builder(type, perTick)` 接收短名，例如 `EU` 或 `RU`（比较时忽略大小写）。builder 只要求名字不是空白。启动时走同一套能量标签查找。内置能量短名及包语义（`size` × `amount`）如下：

- `EU`（电能——包 `size` 是电压，`amount` 是电流安培数，电压等级见 `GTVoltageTiers`）
- `RU`（旋转能——`size` 是转速，`amount` 是功率）
- `KU`（线性推拉动能——`size` 是推拉冲程，`amount` 是功率）
- `HU`（热能——`size` 为 1，`amount` 是热量）
- `CU`（冷能——`size` 为 1，`amount` 是冷量）
- `LU`（光能——`size` 是光束强度，`amount` 是频率）
- `MU`（磁能——`size` 是场强，`amount` 为 1）
- `NU`（中子能——`size` 为 1，`amount` 是中子通量）
- `QU`（量子能）、`TU`（时间——`size` 为 1，`amount` 是 tick 数）、`AU`（气压能）
- 兼容与要素能量标签：`Steam`（蒸汽，`2 Steam = 1 EU`）、`RF`（`4 RF = 1 EU`）、`MJ`（`1 MJ = 2.5 EU`）、`Ordo`、`Aer`、`Aqua`、`Terra`、`Ignis`、`Perditio`

方向不是同一个字段。配方机器和行为机器上，`EnergyInput` 的短名是输入。燃料机上，`FuelOutput` 的短名是输出。行为机器同时还出能时，输出短名写在它的 `EnergyOutput` 上，和输入分开。

标称不是同一段范围。配方机器的 `perTick` 至少是 0，没有上限。行为机器的 `perTick` 是 1 到 1,000,000,000,000。容量为 0 时，这个数也是每 tick 抽取量。燃料机没有输入标称。`FuelOutput.perTick` 是输出的一包，1 到 1,000,000,000,000，builder 要求必须写。

`EnergyInput.range(min, max)` 是接受的包大小。两个值都是 long。不调用时两端都是 0，表示没声明。声明了就要 `1 <= min <= max`。否则 `build()` 失败。标称 `perTick` 可以落在范围外。配方机器可以声明范围。TU 配方机器声明了范围，`build()` 失败。行为机器的主输入调用 `range`，`build()` 失败。它的附加输入可以调用 `range`。燃料机没有输入范围。

`EnergyInput.anyPacket()` 不检查包大小，也不过载。它不能和 `range` 一起写，一起写就在 `build()` 失败。配方机器调用它会失败。行为机器的主输入调用它会失败。条件零件调用它会失败。唯一接受它的地方是 `BehaviorMachine.auxiliaryEnergy`。

单方块配方机器没声明范围时，接受标称的一半到两倍。TU 是 1 到 16。大型配方机器没声明范围时不用一半到两倍。它走内置大型机规则：TU 是 1 到 16，HU 是 1 到 4096，其余类型包括 EU 和 RU 是 512 到 4096。内置蒸馏塔、内爆压缩机和物质制造机仍用各自类上的上限。行为机器的主输入不能声明范围。启动时 GT 按 `perTick` 的一半到两倍算，TU 是 1 到 16。这和没声明的单方块配方机器相同，和大型配方机器不同。附加输入用它自己声明的范围；调用了 `anyPacket()` 就没有范围。

大于最大的包会被消耗，然后走过载规则。默认设置下这不会毁掉机器。功耗低于最小的配方仍会被选中。手头功率低于最小时，`MachineWorkCost` 按四倍计费。缓存上限是这个最大。物品提示、Jade，以及 JEI 或 EMI 里作为催化剂的机器物品，读的都是这个最小和最大。配方页仍显示配方功耗，不是这个范围。附属能量零件不报告控制器的最小和最大。只出的零件不收电。条件零件报告自己输入上声明的范围。共用机器类型的两台机器必须声明同一个范围。两边都不声明也算同一个范围。

面是 `MachineFace`：`BOTTOM`、`TOP`、`RIGHT`、`FRONT`、`LEFT`、`BACK`。`index()` 和 `bit()` 是掩码位置。新声明上的面列表拒绝重复的面。

`EnergyInput.faces` 不跟着 `MachineIo` 走，两种模板的缺省也不一样。配方机器不写面就是六面。可以写空表，表示没有收能量的面。行为机器不写面就是除正面外的每个面。空表失败。写了 `MachineIo` 也不改变这些面。

旧的配方入口调用过任一面方法、又没写 `energyInputs` 时，转换成 `faces()` 空表。新的 `EnergyInput` 不调用 `faces()` 仍是六面，就算这台机器同时写了 `MachineIo` 也一样。这是旧入口和新入口唯一的缺省差别。

`EnergyInput.capacity` 是已存能量的上限。行为机器的主输入上，它至少是 0，缺省是 0，包括调用 `capacity(0)`。0 表示缓冲等于标称抽取：行为返回后 GT 抽走这个量，没用掉的能量不留到下一 tick。容量大于 0 时留下剩余，上限是这个容量。配方机器调用 `capacity` 会失败。只出的零件不用这个字段。它的容量是 `LayoutPart.energyOutput` 的第二个参数，必须至少是 1。条件零件和附加输入都要求容量为正。GT 不用主输入那条规则抽这两份缓存。

出能是两种对象。行为机器和能量零件共用一个 `EnergyOutput`：类型、一个或多个面、`maxSize` 和 `maxAmount`。四样都要有。两个上限都至少是 1。面列表不能空，也不能重复。零件再加上面那个必须为正的容量。燃料机不是这个形状。`FuelOutput` 是一个 `perTick` 包加一个位置：`front()` 是缺省，`back()` 是背面，`behind(distance)` 向正面再过去这么多格注入。距离是 1 到 `FuelGenerator.MAX_BEHIND`，也就是 8。正面和背面的距离保持 0。这一包不是包大小乘包次数。这三个位置不是 `MachineFace` 的值。

### 物品和流体

`MachineIo` 只给配方机器。行为机器写了就失败。燃料机写了就失败。行为机器的物品和流体走槽，写在「行为机器」。燃料机不声明面。单方块的流体能力六面都开。多方块只开正面。两种机器从相邻压缩气罐抽流体时，都扫六面。

不写 `io` 时，物品和流体仍是六面进出，没有自动面。一旦写了 `MachineIo`，没点名的通道是空的面列表，不是六面缺省。自动面缺省为无。自动面传入 null，`build()` 失败。

```java
MachineIo.builder()
        .itemInputs(MachineFace.TOP)
        .itemOutputs(MachineFace.BOTTOM)
        .itemAutoOutput(MachineFace.BOTTOM)
        .build()
```

`IoTarget.at(right, up, back, receiver)` 是大型配方机器的一格。`right` 是朝向的顺时针方向，`up` 是向上，`back` 是朝向的反方向。接收面是 `MachineFace`。方法是 `itemOutputTarget`、`fluidOutputTarget`、`itemInputSource` 和 `fluidInputSource`。声明了的输出推到这个面。声明了的输入从这格拉。没声明的通道仍走面规则。写了目标时，这一通道的自动面保持为无，同一份物品或流体不会移动两次。两个都写，`build()` 失败。只有 `largeRecipe()` 可以写目标。单方块写了就失败。这一格不必是结构零件，可以在布局里，也可以在布局外。区块没加载就跳过这一通道这一 tick。物品在已成形的机器做自动物品传输时移动。流体输出在机器 tick 一开始就推，机器停着或结构没成形也推，和内置大型机一样。内置控制器仍用自己的位置。

三套坐标限制保持分开。结构格拒绝原点，也拒绝重复位置，没有 ±16。`IoTarget` 拒绝原点，`right`、`up`、`back` 各是 -16 到 16，两端都包括。燃料的 `behind` 是正面再过去 1 到 8 格，没有 `right` 和 `up`。

### 结构

`StructureLayout` 按名字绑定。名字等于控制器的 `machineType`。几台机器可以共用一张布局。布局不写进机器声明里面。控制器在原点，原点不是一格。

```java
StructureLayout.builder("mypress")
        .cell(0, 1, 1, CellMatch.part("sample_wall"), StructureRole.CASING)
        .cell(-1, 1, 2, CellMatch.block("minecraft:glass"), StructureRole.CASING)
        .build()
```

`LayoutCell.at(right, up, back, match, role)` 是一格。原点在 `LayoutCell.at` 失败。重复位置在 `StructureLayout` 的 `build()` 失败。`CellMatch` 是 `part(name)`、`air()`、`block(id)` 或 `tag(id)`。`part` 点 GT 结构零件、结构壳，或 `layoutParts()` 里的名字。这三者已经是同一个名字空间，附属零件不能占内置名。`block` 是带命名空间的方块 id。`tag` 是方块标签，不带开头的 `#`。`block` 和 `tag` 必须配 `StructureRole.CASING`，`LayoutCell.at` 会检查。`CellMatch.parse(String)` 按字符串读：`air`、`#` 开头是标签、含 `:` 是方块 id，其余是零件名。

方块 id 或标签是资源位置：小写命名空间、一个冒号、路径字符集 `[a-z0-9/._-]`。写法不对就失败。这种格子只比较方块。不要求方块实体，不绑定，不运送物品、流体或能量。方块 id 在 common setup 检查，那时其他模组的方块已经注册。未知 id 启动失败，并写出附属和布局。标签在成形检查时读取。空标签永远不能成形，并记一条警告。结构预览显示这个方块。标签显示注册名排在最前的那个成员；标签还是空的时候显示屏障。拆掉这块方块，下一次结构检查就失败。

`StructureRole` 是 `AIR`、`CASING`、`LOGISTICS`、`HEAT_INPUT`、`CRUCIBLE`、`FLUID_INPUT`、`FLUID_OUTPUT`、`FLUID_IO`、`ITEM_IO`、`ITEM_FLUID_IO`、`ITEM_FLUID_INPUT`、`ITEM_FLUID_OUTPUT`、`ENERGY_INPUT`、`ITEM_FLUID_ENERGY_INPUT`、`ITEM_FLUID_ENERGY` 和 `ENERGY_OUTPUT`。它不是行为槽的角色。

要不要布局、什么时候算成形，按模板分开。大型配方机器必须有一张同机器类型的布局。没有布局就启动失败。结构检查不通过就不能开工。多方块燃料机必须有一张同机器类型的布局。单方块燃料机不能挂布局，一开始就是成形的。行为机器的布局可选。没有布局时，第一次服务端 tick 把成形设为真，这一 tick 之前是假。有布局才逐格检查。三种检查匹配格子的方式相同。

不是方块 id、也不是标签的零件名，用 `SharedLargeMachineParts` 的名字、`air`、`tank_wall`、`tank_wall_dense`、`centrifuge_part`、`heat_transmitter`、`distillation_tower_part`、`electrolyzer_part`，或 `layoutParts()` 返回的名字。空布局、重复坐标、未知零件、没有控制器的布局、没有布局的大型配方机器，都会在启动时报错并写出附属。

`layoutParts()` 返回外壳、能量零件和功能零件：

```java
public List<LayoutPart> layoutParts() {
    return List.of(
            LayoutPart.builder("sample_wall", "Steel").hardness(6.0F).build(),
            LayoutPart.builder("sample_emitter", "Steel").hardness(6.0F)
                    .energyOutput(EnergyOutput.builder("EU", 32, 4).faces(MachineFace.FRONT).build(), 100_000)
                    .build());
}
```

名字匹配 `[a-z][a-z0-9_]{0,63}`：字母开头，最长 64，不能有 `/`。不能占用内置零件名。贴图是附属命名空间里的路径，或空。空则用 GT 金属墙，按材料颜色着色。路径是一块不着色的立方体，不能带命名空间。硬度是正的 float。爆炸抗性是同一个数。三种机器模板都没有硬度字段。方块 id 是 `<附属 id>:<名字>`。GT 在内置多方块零件之后注册它，走 GT 的总线，进附属的命名空间，并生成方块状态和物品模型。名字是全局的。GT 零件名，包括 `tank_wall`，或另一个附属的名字，启动失败并写出两边。运行时 id 从 1000000 起，先按附属 id，再按声明顺序。结构存的是方块注册名。成形比较世界里的方块。这个数字不是存档数据。没有能量角色、也没有功能能力的零件是外壳：没有储罐，没有能量，没有物品槽，也不 tick。`energyOutput` 和 `energyInput` 一起写，`build()` 失败。

`energyOutput(EnergyOutput, capacity)` 把它变成只出的能量零件。`capacity` 至少是 1。EU 和 RU 只差类型字符串。未知短名在启动时失败，并写出附属和零件。方块 id 仍是 `<附属 id>:<名字>`。它有水平朝向、自己的 tick，方块实体是 `gt6ce:addon_energy_part`。输出面相对这个朝向，前后左右的算法和行为机器一样。输出缓存、已设定的包和停机标记存成 `gt.part_energy`、`gt.part_output`、`gt.part_amount` 和 `gt.part_stopped`。外壳仍用 `gt6ce:multiblock_port`。

零件在输出面上按声明顺序推出已设定的包。这一格的角色要是 `ENERGY_OUTPUT`，它要绑在这台已成形的机器上，并且没有停机。邻居通过 `EnergyBlockViews` 看见这个零件。他们不抽缓存，也不能往只出的零件里注入。`ENERGY_INPUT` 的格子把输出缓存留给控制器，自己不输出。一个零件只属于一个控制器坐标。这个坐标还占着它时，第二套结构不能成形。放开这个零件，包括结构里有一格被拆掉，都会让它停机。声明上没有方法询问一个包还塞得进多少，也没有方法检查包上限。零件运行时按剩余容量截断，并拒绝超过声明上限的包。

`energyInput(EnergyInput)` 和 `condition(perTick)` 一起写成条件零件。只写输入、不写条件会失败。只写条件、不写输入会失败。`anyPacket()` 会失败。输入要有短名、包范围、至少一个面，以及正的容量。输入上的 `perTick` 是推荐包大小，必须落在这个范围内。`condition` 至少是 1，并且不超过容量。零件在这些面上收电。格子角色不决定这些面，零件在绑定之前就能收。低于最小的包被拒绝。高于最大的包被消耗，走过 `MachineOverload`，不进缓存。缓存上限是声明的容量。每个服务端 tick，缓存够 `condition` 就扣掉这个量并记为满足。不够就不扣，也不满足。控制器可以晚一 tick 看见这个结果。

输入缓存和满足标记是 `gt.part_input` 和 `gt.part_satisfied`。旧档只有 `gt.part_energy` 时仍是只出不收。`gt.part_energy` 不会被当成输入缓存来读。缺了输入键就是空缓存、不满足。方块状态 `satisfied` 缺省是 false。服务端写它，客户端读它。满足的零件亮度是 12。只出的零件保持这个属性为 false。

条件零件可以绑行为机器，也可以绑大型配方控制器。内置多方块端口仍走自己的绑定。附属能量零件不绑到内置控制器，内置端口也不绑到附属控制器。大型配方机有一个绑定的条件零件不满足时就不加工。已经有的进度留着。没有条件零件时，这台机器和以前一样。行为机器在条件零件缺电时仍调用 `tick`。

`slots(PartSlots)`、`tick()`、`use()` 和 `field(key)` 加上物品槽、服务端 tick、玩家右键和命名存档字段。写上任意一项，这个零件就是功能零件。它们可以和 `energyOutput` 写在一起，也可以和 `energyInput`、`condition` 写在一起。`PartSlots.builder(count)` 是 1 到 16 个槽。`stackLimit` 是 1 到 64，缺省 64。`insert` 和 `extract` 写相对零件水平朝向的 `MachineFace`。某一侧的面列表为空时，管子不能做这个动作。`side` 为 null 时不能放入，也不能抽出。`drops` 缺省是 true。为 true 时，拆掉零件会掉出槽里的物品，以及字段里存的物品。没有槽的零件仍会掉出字段物品。方块物品照常掉落。

字段键匹配 `[a-z0-9_.]{1,64}`。最多 16 个。非法的键、重复的键、第 17 个键都在 `build()` 失败。GT 不给键规定含义。`PartContext.state()` 读写这些已声明的键。没声明的键会抛出。已声明但还没写入的值是 0、false、空字符串或空物品。字符串最长 256。

行为是 `LayoutPartContributor`，列在 `META-INF/services/com.gregtech.gregtech.api.addon.platform.LayoutPartContributor`。`partNames()` 要写出每一个功能零件。外壳和只含能量的零件不列。每放下一个方块实体，`create` 调用一次。功能零件没有工厂时启动失败，并写出附属和零件。附属不继承方块实体。

`PartBehavior` 收到 `tick`、`onUse`、`canInsert`、`canExtract`、`onSlotsChanged`、`describe` 和 `onWrench`。`PartContext.setItem` 直接换掉一格，不问 `canInsert`，也不看面列表。数量超过该格上限时被截到上限。管子先看面列表，再问 `canInsert` 或 `canExtract`。玩家右键走 `onUse`，`PartHit` 给出点击的面，以及从方块低角量起的比例位置。扳手先调 `onWrench`。返回 true 就吃掉这次点击。返回 false 时 GT 旋转水平朝向。放大镜显示 `describe()`。空列表则这次点击不处理。

槽存成 `gt.part_slots`，按槽序每个一个 compound。空槽是空 compound。字段存成 `gt.part_fields`，里面的 `i`、`l`、`b`、`s`、`t` 和行为机器的状态相同。读取时跳过已经不再声明的键。外壳和只含能量的零件不写这些键。能量键仍是 `gt.part_energy`、`gt.part_output`、`gt.part_amount`、`gt.part_stopped`、`gt.part_input` 和 `gt.part_satisfied`。

没有能量角色的功能零件用方块实体 `gt6ce:addon_functional_part`。方块状态只有水平朝向。同时出能或收能的零件仍用 `gt6ce:addon_energy_part`，槽和字段写在能量键旁边。只含能量的零件不发方块实体更新包。功能零件会发。客户端读 `PartClientView`。`LayoutPartClient` 可以按零件名返回方块实体渲染器。把它列在 `META-INF/services/com.gregtech.gregtech.api.addon.platform.LayoutPartClient` 里。GT 在客户端初始化时读取这个文件并负责调用，附属不用自己注册渲染器。没有渲染器的零件仍会同步这份视图。

`MachineBehaviorContext.parts()` 列出这张已成形布局绑定的每一个附属零件。外壳不在里面。机器还没成形时列表是空的。不要把 `BoundPart`、它的 `behavior()` 或 `context()` 留到回调外面。既是能量零件又是功能零件的格子只出现一次，`energy()` 和 `behavior()` 都有，并且这一格也在 `energyParts()` 里。`energyParts()` 仍是这份绑定的能量视图。只含能量的零件，`behavior()` 是 null。没有能量角色的功能零件，`energy()` 是 null。把 `behavior()` 转成附属自己的类型，并把 `context()` 传进那些方法。

### 配方表

`RecipeTable.machine(name)` 和 `RecipeTable.fuel(name)` 字段相同，名字不共用。配方机器点机器表。燃料机点燃料表。行为机器的 `recipeMaps` 两种都能点，GT 不替行为加工这些行。

`name` 匹配 `[a-z0-9_.]+`。`title` 和 `gui` 必填。缺了就在 `build()` 失败。表的 `gui` 不是行为贴图：不必是带命名空间的 `.png`，也不按控件矩形检查。行为贴图的规则在「行为机器」。数量缺省 0，并且至少是 0：`inputItems`、`outputItems`、`minimalInputItems`、`inputFluids`、`outputFluids`、`minimalInputFluids` 和 `minimalInputs`。`needsOutputs`、`combinePower`、`bucketIn` 和 `bucketOut` 缺省为假。

```java
RecipeTable.machine("my_addon.recipe.press")
        .title("Press")
        .gui("my_addon:textures/gui/machines/press")
        .inputItems(1).outputItems(1).minimalInputItems(1).minimalInputs(1)
        .needsOutputs(true)
        .build()
```

燃料表上正数的 `eu_per_tick` 是每 tick 产出的能量，存成负数。`AddonRecipes.map` 取燃料表的方式和机器表一样。`find` 返回对得上的行。`Line.euPerTick` 是存进去的数，所以读回来的燃料行是负数。`AddonRecipes.add` 按写进去的数存储，从这里加的燃料行要自己写成负数。存成非负数就不是要产出的能量。这一行仍要有输出。配方机器点名燃料表时，按未知配方表失败。机器表或燃料表的名字被再用一次，启动失败并写出附属。

GUI 路径会保留 `:` 前面写下的命名空间。`textures/gui/machines/Press` 和 `gt6ce:textures/gui/machines/Gas` 仍解析到 `gt6ce:textures/gui/recipes/<文件>.png`。附属路径 `my_addon:textures/gui/machines/press` 解析到 `my_addon:textures/gui/recipes/press.png`，磁盘上的文件是 `assets/my_addon/textures/gui/recipes/press.png`。

新表在 `onRecipesReady`、JEI、EMI 和数据包基线之前建好，并进入 `RecipeMap.RECIPE_MAP_LIST`。行从 `onRecipesReady` 加，或用 `gt6ce:machine` 数据包加。配方页按表上声明的槽数画格子。燃料表声明了至少一个物品输入时，会画出这些物品槽。内置燃料表声明了 0 个物品槽时，行上带了物品也不另开一格。

### 配方机器

`RecipeMachine.builder(MachineCasing)` 只写配方自己的旋钮。外壳、能量输入、物品和流体的面、布局，用上面几节的类型。

`basic()` 是缺省，做成单方块。`largeRecipe()` 做成大型配方机器。没有种类枚举。蒸馏塔、物质制造机和其他内置控制器留在 GT 里。`largeRecipe()` 是必须有布局的那种，也是唯一可以写 `IoTarget` 的那种。什么时候必须有布局、什么时候算成形，写在「结构」。

`recipeMap` 必填。它匹配 `[a-z0-9_.]+`，必须是这个贡献者返回的机器表，或一张内置机器表。

`parallel` 缺省 1，并且至少是 1。`efficiency` 缺省 10000。它是万分比，1 到 100000。`constantEnergy` 缺省为真。机器不是 TU 时按恒定量抽取。TU 仍不这样。内置机器仍使用原来按名字算出的效率、恒定耗能和并行。这些值在构造内置 spec 时写进去。

`cheapOverclocking(true)` 打开廉价超频，并记下是附属自己设置的。不调用这个方法，这条记录保持为假。单方块不调用它就保持关闭，就算机器类型撞上内置的廉价表也一样。大型配方机器不调用它时，不是 TU 就保持廉价超频。

`minimumLightAbove(14)` 表示正上方 `Level.getRawBrightness` 不到 14 就不开始新的加工。这是 1.7.10 的 `getBlockLightValue`：天空光和方块光取高的那个，夜晚不压暗。值是 0 到 15。`0` 是缺省，不检查。已经开始的加工不会被打断。

`searchOutputTanks(true)` 在输入储罐对不上时，用输出储罐再找一次配方。物品槽仍是输入槽。这一轮记下是哪一组储罐对上的，流体包括并行的那一批都从那一组扣。输入储罐已经对上时，就用这一组。配方自己产出的流体仍送进输出储罐。下一轮输入储罐对不上时，这份流体可以被扣掉。不调用这个方法时，搜索仍只看输入储罐。

`exclusiveOutputs(true)` 只掷第一项物品输出一次，单位是万分比。掷中小于这个概率就出第一项，其余出第二项。产出数量是这项的配方数量乘这一轮的并行。流体输出仍是必定。这台机器的配方表上，每一条静态行都要正好是两项非空物品输出。别的行启动失败，并写出附属和机器。JEI 和 EMI 显示这两个概率。第二个概率是提示，掷的时候不读它。不调用这个方法时，物品输出仍是逐项自己掷。

`structureKeys` 是结构提示的翻译键。每条匹配 `[a-z0-9_.]+`。列表可以是空的。它们不是布局。

同一个附属的两台方块，在配方表、单方块或大型、效率、恒定耗能、结构提示键、面罩、有没有设置廉价超频、光照门、输出储罐搜索、二选一输出、能量范围和四个位置上都一致时，可以共用机器类型。同一台机器的两档就是这样共用界面和结构。

JEI 催化剂和多方块预览跟着已注册的机器列表。有布局的大型机器会出现在预览里。合成配方由附属自己的数据包提供。GT 不生成。附属大型机器用默认的端口路径。内置机器的名字分支仍只给内置机器。

### 行为机器

每个 `BehaviorMachine` 从 `addonMachines()` 返回。另外在 `META-INF/services/com.gregtech.gregtech.api.addon.platform.BehaviorMachineContributor` 里列出 `BehaviorMachineContributor`。这个接口在加载器 API 里，因为 Forge 和 NeoForge 的 `FluidStack` 不同。`machineTypes()` 必须盖住这个附属声明的每一种行为机器类型。每放下一台方块实体，GT 调用一次 `create`。缺少工厂时启动失败，并写出附属和机器类型。

行为不继承 GT 的类。`MachineBehavior` 有三个必须实现的方法——`tick(context)`、`canInsert(context, slot, stack)` 和 `canFill(context, tank, stack)`——以及四个带默认实现的可选方法：`acceptFluid(context, tank, stack, simulate)`、`onToggle(context, toggle)`、`beforeBreak(context)` 和 `onUse(context, player, hand)`。`MachineBehaviorContext` 读写物品、储罐、能量、开关、进度和世界。`BehaviorState` 按键存 int、long、boolean、字符串和物品。GT 把这张表写进 NBT。行为不接触 `CompoundTag`。没有这个键时读到 0、false、空字符串或空物品。键匹配 `[a-z0-9_.]{1,64}`。字符串最长 256。方块被拆掉时，GT 在 `beforeBreak` 之后丢掉库存物品和状态里的物品。

```java
public List<AddonMachine> addonMachines() {
    return List.of(BehaviorMachine.builder(
            MachineCasing.builder("irrigator", "Steel").machineType("irrigator").tier(1).build())
            .energy(EnergyInput.builder("EU", 32).build())
            .gui("my_addon:textures/gui/machine.png")
            .item(44, 26, BehaviorMachine.SlotRole.INPUT)
            .item(116, 26, BehaviorMachine.SlotRole.OUTPUT)
            .fluid(8000, 152, 8, BehaviorMachine.SlotRole.INPUT)
            .toggle("my_addon.irrigator.water", 26, 64, true)
            .build());
}
```

GT 按声明和附属贴图画 GUI。贴图同时是面板和控件表。控件像素用 `BehaviorMachine` 上的常量：槽 `UV_SLOT_U`、`UV_SLOT_V` 在 176,0（18×18）；能量空 `UV_ENERGY_U`、`UV_ENERGY_V` 在 194,0，满 `UV_ENERGY_FULL_U` 在 204,0（10×54）；储罐 `UV_TANK_U`、`UV_TANK_V` 在 214,0，刻度 `UV_TANK_MARKS_U` 在 232,0（18×54）；进度空 `UV_ARROW_U`、`UV_ARROW_V` 在 176,54，满 `UV_ARROW_FULL_V` 在 71（24×17）；开关关 `UV_TOGGLE_OFF_U` 在 176，开 `UV_TOGGLE_ON_U` 在 194，两者的 `UV_TOGGLE_V` 都是 88（18×18）。面板缺省 176×166。`guiSize` 允许宽 176 到 256，高 166 到 512。能量条缺省在 (8, 8)。进度缺省在 (80, 35)。玩家物品栏从底部往上 `PLAYER_INVENTORY_GAP`（82）像素开始。物品槽坐标是内部 16 像素。框画在左上一像素，所以槽的 `x` 和 `y` 至少是 1。控件必须停在玩家物品栏之上，并且不能重叠。最多 54 个物品槽、8 个储罐、16 个开关。开关的键匹配 `[a-z0-9_.]+`。

流体槽自己写容量。容量是 1 到 1,024,000,000 mB。燃料罐的缺省是另一条规则，写在「燃料机」。

`BehaviorMachine.SlotRole` 是 `INPUT`、`OUTPUT` 和 `ANY`。玩家和管子只在 `canInsert` 或 `canFill` 允许时，向 `INPUT` 和 `ANY` 放入。管子从 `OUTPUT` 和 `ANY` 抽出物品。管子只从 `OUTPUT` 和 `ANY` 抽流体，所以输入罐保持只进不出。`insertOutput` 先填 `OUTPUT` 再填 `ANY`，并且不问 `canInsert`。`acceptFluid` 是管子和容器的灌入路径。缺省在 `canFill` 允许时存下提供的流体。浓缩液要变成罐里的流体时，覆盖这个方法。`context.fill` 在罐子是空的或已经装着这种流体时存下提供的流体，并且不调用 `canFill`。`pushCompressedGas(face, stack)` 只把气体存进这个面上的压缩气罐。写入是立刻的，方法返回存进去的毫桶。行为从自己的罐子里扣掉这份流体。管子和普通储罐什么都不收。

输入面、接受范围和容量 0 是 `EnergyInput`，写在「能量」。GT 先问相邻的输出方，再跑行为，然后按标称输入抽缓存。`consume` 在这一步之前花掉已存的能量。`generate(amount)` 往同一个缓存加能量，返回塞进去多少。`setCapacity` 在这一 tick 里改上限。只有调用之后，GT 才把它写成 `gt.capacity`。旧存档没有这个键时用声明的容量。已存能量超过上限时外壳不爆炸：行为可以看见并拆掉方块，行为返回后 GT 丢掉超出的部分。`working(true)` 点亮表面。运行状态表示这一 tick 开始时有能量。包大于接受上限时，过压爆炸开着就会毁掉机器。

`energyOutput(EnergyOutput)` 声明「能量」里写的那种输出。`emit(size, amount)` 花掉已存能量，并在每个输出面上按声明顺序经 `EnergyTransfer.emitEnergyToSide` 推出这个包，直到送出请求的次数或邻居不再接受。它返回送出了多少包。大小或次数超过声明上限就抛出。GT 不把它钳制到上限。没有输出的机器调用 `emit` 会抛出。邻居不抽这份已存能量。由行为推出去。

机器类型有布局时，GT 在行为运行之前成形。那时 `structureFormed()` 为真。`parts()` 列出这张布局里每一个绑定的附属零件。`energyParts()` 是同一份绑定的能量视图，只出的零件和条件零件在一起。外壳不进这两份列表。没有布局时，第一次服务端 tick 把成形设为真，零件列表是空的。`charge(amount)` 把这个零件自己的缓存充到容量，返回充进去多少；这个零件不再绑在这台机器上时返回 0。条件零件上这份缓存是输入缓存，`charge` 不检查包大小。`setOutput(size, amount)` 设定只出零件的包，并清掉停机标记。请求超过零件声明的上限就抛出。条件零件调用 `setOutput` 会抛出，因为它没有输出。`stop()` 停掉只出的零件。`EnergyPart.condition()` 对条件零件为真。`satisfied()` 是这个零件在自己的 tick 里有没有付过条件。`conditionsMet()` 在每个绑定的条件零件都满足时为真。还没成形的机器没有绑定零件，所以为真。条件零件缺电时 `tick` 仍会跑。

`auxiliaryEnergy(EnergyInput)` 给行为机器加第二种输入。它要有自己的面、正的容量，以及包范围或 `anyPacket()`。两个都不写就失败。和主输入共用一个面就失败。`perTick` 是 1 到 1,000,000,000,000。写了范围时，`perTick` 必须落在范围内，并且是推荐包大小。主输入仍然拒绝 `range` 和 `anyPacket`。附加面上的包进第二份缓存。它们不进主缓存，tick 结束时的抽取也不动它们。机器声明了附加输入时，这份缓存存成 `gt.aux_energy`。缺了这个键就是空的。`energy(type)` 和 `consume(type, amount)` 只读写这份缓存。类型比较忽略大小写。别的类型，包括主能量的类型，读到 0，也扣不到。主缓存仍走 `energy()` 和 `consume(long)`。`anyPacket()` 接受任何包大小，并且不过载。写了范围的附加输入在超过自己的最大时过载，和主输入分开。低于它的最小的包被拒绝。

行为方块实体仍是附属的 `EnergyBlock`。它不再实现 `IEnergyBlock`，因为两个接口上的 `getEnergyTypes` 会擦成同一个方法。轴、激光纤维、中继、远距变压器，以及会注入或抽取的内置机器，用 `EnergyBlockViews` 解析邻居。内置方块仍走原来的路径。变速箱本来就经 `EnergyTransfer` 输出，所以本来就能送到接受能量的附属方块。扫描仪、Jade 和盖板读数仍只看 `IEnergyBlock`。

`onUse(context, player, hand)` 在服务端、扳手和流体容器之后、菜单之前运行。返回 true 表示这次点击已处理。缺省返回 false，然后打开菜单。

`maxRange(int)` 是世界动作的立方体半边长。范围是 0 到 `BehaviorRangeRules.LIMIT`，也就是 32。缺省是 0，这时两个世界方法都不做事。距离是切比雪夫距离：三个轴差距里最大的那个。机器自己这一格在这个立方体外面。

`breakBlock(pos)` 拆掉一块，并把 `Block.getDrops` 放进输出槽。区块没加载、方块是空气或硬度为负、掉落物放不下，或这一 tick 已经拆过一块时，返回 false，方块留下。拆掉之前，GT 用放置这台机器的玩家发送加载器的破坏事件。放置者在线就用这个玩家。否则平台假玩家站在机器上，带着存下来的档案；机器不是玩家放置的时候用一份固定档案。事件被取消就返回 false。扫描下标属于行为。`BehaviorState` 已经会存它，所以矿机重启后能接着扫。每一 tick 只成功破坏一块。

`collectItems(radius, voidThem)` 拿走碰撞箱碰到这个半径立方体的物品实体。半径不能超过 `maxRange`。`voidThem` 直接丢掉。否则每一叠走 `insertOutput`，没能完全放下的实体留下剩余。每一 tick 最多 16 个实体。这个方法没有物品过滤。

水车不需要另一个入口。`level()` 和 `pos()` 能看见机器旁边的方块：

```java
private static boolean adjacentWater(MachineBehaviorContext context) {
    for (Direction side : Direction.values()) {
        if (context.level().getFluidState(context.pos().relative(side)).is(Fluids.WATER)) return true;
    }
    return false;
}
```

缺省的方块实体 id 写在「注册机器」。显式的 `blockEntityId` 让几台方块共用一个方块实体类型。路径匹配 `[a-z0-9_./-]{1,64}`。它们必须是同一个附属、同一个机器类型，并且输入能量短名、附加能量短名、输出能量短名、物品角色、流体角色和开关键都相同。没有附加输入就是空名字。储罐容量、能量容量、等级、每 tick 能量、输出面和控件位置可以不同。共用路径对不上时启动失败。方块状态、模型和 GUI 贴图由附属自己带。开关的文字是声明上的翻译键。GT 的行为界面字符串是 `gui.gregtech.behavior.energy`、`progress`、`tank`、`on` 和 `off`。

行为机器不是配方机器。GT 不替它查行、扣输入或写产物。配方机器仍用 `gt6ce:basic_machine`。

`recipeMaps(String... names)` 列出机器表或燃料表。这个方块成为每个名字的 JEI 和 EMI 催化剂。名字是本附属声明的表，或内置表，例如 `gt.recipe.compressor` 或 `gt.recipe.fuels.engine`。另一个附属的表算未知。未知名字在启动时失败，并写出附属和机器。重复的名字，或不符合 `[a-z0-9_.]+` 的名字，在 `build()` 失败。不调用时不加催化剂，配方机器和燃料机已经收好的名单保持原样。这些名字不会让 GT 去跑这张表。

样例固体锅炉点名一张有 1 个物品输入和 1 个物品输出的燃料表。煤变成木炭，每 tick 16 EU，烧 20 tick。行为找到这一行，扣掉煤，把木炭放进输出槽，然后每个燃烧 tick 从顶面 `generate` 并 `emit`。计数 `left`、`eu` 和 `burn` 记在 `BehaviorState` 里。外壳不新增字段。燃料行把产出能量存成负数的 `euPerTick`，写在「配方表」。`generate` 收的是正数，所以行为把存下来的值取反。

```java
BehaviorMachine.builder(MachineCasing.builder("sample_boiler", "Steel").machineType("sampleboiler").tier(1).build())
        .energy(EnergyInput.builder("EU", 32).capacity(10_000).build())
        .gui("my_addon:textures/gui/machine.png")
        .item(44, 26, BehaviorMachine.SlotRole.INPUT)
        .item(116, 26, BehaviorMachine.SlotRole.OUTPUT)
        .energyOutput(EnergyOutput.builder("EU", 32, 1).faces(MachineFace.TOP).build())
        .recipeMaps("my_addon.fuel.solid")
        .build();
```

```java
AddonRecipes.Line line = AddonRecipes.map("my_addon.fuel.solid")
        .find(List.of(context.item(0)), List.of(), false, 1, 0);
if (line != null && line.euPerTick() < 0) {
    ItemStack fuel = context.item(0).copy();
    fuel.shrink(line.itemInputs()[0].getCount());
    context.setItem(0, fuel);
    context.generate(-line.euPerTick());
    context.emit(-line.euPerTick(), 1);
}
```

### 燃料机

`FuelGenerator.builder(MachineCasing)` 烧一张燃料表。能量包和它离开的位置是 `FuelOutput`，写在「能量」。不要把这一包改写成包大小乘包次数。

`efficiency` 是万分比，1 到 100000，缺省 10000。和配方机器是同一段范围。`tank` 是燃料罐容量，单位毫桶。缺省 4000。上限是 1 到 `FuelGenerator.MAX_TANK`，也就是 1,024,000,000，燃料罐和每个废气罐都是这个上限。行为流体槽用同一个上限，但没有缺省：槽自己写容量。废气罐的个数是燃料表的流体输出个数。燃料表至少要有一个流体输入。

`single()` 是缺省。`multi()` 是多方块。哪一种可以有布局、什么时候一开始就成形，写在「结构」。没成形的多方块不烧燃料，也不输出。两种机器都是自己那张燃料表的 JEI 和 EMI 催化剂。停机开关用普通的机器控制。多方块出现在结构预览里。

```java
public List<RecipeTable> recipeTables() {
    return List.of(RecipeTable.fuel("my_addon.fuel.gas")
            .title("Gas").gui("my_addon:textures/gui/machines/gas")
            .inputFluids(1).outputFluids(1).minimalInputFluids(1).minimalInputs(1)
            .build());
}

public List<AddonMachine> addonMachines() {
    return List.of(
            FuelGenerator.builder(MachineCasing.builder("engine", "Steel").build())
                    .fuelMap("my_addon.fuel.gas").efficiency(10_000)
                    .output(FuelOutput.builder("RU", 32).front().build())
                    .tank(4_000).single().build(),
            FuelGenerator.builder(MachineCasing.builder("turbine", "Steel").machineType("sample_turbine").build())
                    .fuelMap("gt.recipe.fuels.gas")
                    .efficiency(5_000)
                    .output(FuelOutput.builder("RU", 32).behind(4).build())
                    .tank(8_000).multi().build());
}
```

机器可以点名附属燃料表，也可以点名内置表，例如 `gt.recipe.fuels.engine` 或 `gt.recipe.fuels.gas`。缓存只在接收方接受之后才花掉这一包。这套模板不做能量种类转换，不烧固体燃料，内置柴油引擎和燃气涡轮仍用自己的方块实体。行为可以自己烧一行燃料，包括带物品输入的行。那种燃烧属于行为，不属于这套模板。

### 管子

`fluidPipes()` 和 `itemPipes()` 加管子行。GT 按内置档位展开每一行，并把这些方块接在内置管子后面。

```java
public List<FluidPipeDeclaration> fluidPipes() {
    return List.of(new FluidPipeDeclaration("teflon", "Teflon", 125, true, true, false, false, 600, 0));
}

public List<ItemPipeDeclaration> itemPipes() {
    return List.of(new ItemPipeDeclaration("pvc", "PVC", 32768, 1));
}
```

`idSuffix` 是方块路径里的材料部分。流体后缀撞上内置流体管，或另一个附属的流体管，启动失败并写出两边。物品管在物品管目录里用同一条规则。同一个后缀可以同时是流体管和物品管，因为路径不同。`maxTemperature` 为 `0` 表示材料熔点乘 1.25。`stepSize` 是中型物品管的寻路代价，必须至少是 4。

一行流体变成七个方块，从 tiny 到 nonuple。一行物品变成六个，包括三种限制档。方块 id 是 `<附属 id>:pipe_<档位>_<后缀>` 和 `<附属 id>:item_pipe_<档位>_<后缀>`。GT 在自己的总线上、附属的命名空间里注册它们，不放进 `gt6ce` 的管子注册表。它们用已有的 `gt6ce:fluid_pipe` 和 `gt6ce:item_pipe` 方块实体，所以流体和物品跟方块一起保存。

合成、焊接和材料回收走内置管子的规则。材料没有 `plateCurved` 时，合成和焊接跳过这一行。附属自己提供 `block.<命名空间>.<路径>` 翻译。GT 生成占位方块状态，动态管子模型会换掉它。这些行不是内置管子。

### 压缩气罐

`AddonMachineContributor.compressedGasTanks()` 返回 `CompressedGasTankDeclaration`。id 是小写注册路径。材料必须已经注册。容量是绝对毫桶，1 到 1,024,000,000，不是压缩倍率。气密始终为真。耐酸、耐等离子体、耐魔法是耐压档。温度上限是材料熔点，安装时解析。

```java
@Override public List<CompressedGasTankDeclaration> compressedGasTanks() {
    return List.of(new CompressedGasTankDeclaration(
            "sample_drum", "Steel", 12_800_000, false, false, false));
}
```

普通灌入关闭。管子、漏斗和容器的 `fill` 返回 0。`isFluidValid` 也是 false。抽出是开的，所以管子和燃料能源机器能抽。燃料能源机器每 tick 从相邻压缩气罐最多抽 1000 mB，并且只抽燃料表接受的气体。抽多的部分灌回去。内置燃气涡轮和柴油引擎不走这条。

压缩机是行为机器。`pushCompressedGas` 写在行为上下文上，不另做配方壳。样例压缩机从底面收 KU，把罐里的气体推向背面，提供量是这一 tick 的 KU 乘 10 毫桶。KU 即使没灌进去也扣掉。

`AddonFluids.gas` 判断一种流体是不是气体。空流体是 false。

方块 id 是 `<附属>:<id>`。硬度 1，爆炸抗性 6，堆叠 16。创造标签页按类名里的 Tank 归到流体容器。GT 生成铁方块模型，附属可以自己覆盖。显示名是 `block.<附属>.<id>`。提示是 `gt6ce.tank.compressed.store` 和 `gt6ce.tank.compressed.compressor`。这两句不是原版 GT6 译文，共享中文语言文件沿用英文。无效流体在 tick 时倒空。没有发酵，没有自动输出。大型阀多方块不做。

## 信号线

信号线方块实体实现 `GtSignalCarrier`。`GtSignalCarrier.UNIT` 是一级原版红石，数值为 `Integer.MAX_VALUE`。`signal()` 是这根线上当前的精细强度。衰减、存档里的 `mode`，以及方块状态上的 0–15 级都不变。线仍然每个服务端 tick 从信号源重算网络。

```java
if (level.getBlockEntity(pos) instanceof com.gregtech.gregtech.api.signal.GtSignalCarrier wire) {
    long fractional = wire.signal();
    // ...
}
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

## 范围

当前支持注册和配方就绪生命周期；`AddonMaterialContributor` 按名字排队材料和已有前缀的追加形态（生成 `gt6ce:` 物品），`dust()` 和 `metal()` 选择生成哪些形态，`prefixes()` 在附属自己的命名空间里增加物品前缀，读取用 `GregTechMaterials`（含 `byproducts()`），前缀是否存在用 `GregTechPrefixes.exists`，数字 id 从 10000 起在启动时分配，不写入存档；`oreVeins()` 和 `smallOres()` 把大矿脉和小矿追加在 `minecraft:overworld`、`minecraft:the_nether` 或 `minecraft:the_end` 的内置行之后；`compositions()`、`smeltingTargets()`、`crucibleReactions()` 和 `crucibleRemovals()` 用材料名，在材料链接时生效；`AddonItems.stack` 和 `AddonItems.selectorTag`（0 到 24 号编程电路）；`AddonFluids` 按 GT 流体字段名取静液或流动液（`DistW`，不是材料名），`AddonFluids.gas` 判断这种流体是不是气体；已有配方表上的 `AddonRecipes`（添加、展示、碰撞、物品输入检查、动态 provider、查找，`add` 返回 null 表示被拒绝；`keptInputs` 标出留在机器里的物品输入，数量 0 仍被拒绝）；`AddonMachineContributor` 的配方机器和大型配方机器，含 `searchOutputTanks`、`exclusiveOutputs`、`EnergyInput.range(min, max)`，没声明范围的大型机不是标称的一半到两倍，大型配方机器还可以各指定一格物品输出、流体输出、物品输入和流体输入，能源用短名例如 `EU`，多方块可以写 GT 零件名，或 `layoutParts()` 里的外壳、能量零件和功能零件，条件零件是 `energyInput` 加 `condition`，大型配方机有绑定的条件零件不满足时不加工，没有条件零件则不变，机器和零件都由 GT 注册到附属命名空间，结构角色是 `StructureRole`，结构格还可以写方块 id 或 `#` 标签，这种格子只作 `CASING` 且不绑定；`fluidPipes()` 和 `itemPipes()` 各加一行材料，GT 按内置档位展开，方块在附属自己的命名空间；`compressedGasTanks()` 声明压缩气罐，普通灌入关闭，`pushCompressedGas` 只灌相邻的这种罐，燃料能源机器每 tick 最多抽 1000 mB；`BehaviorMachine` 以及加载器上的 `BehaviorMachineContributor`、`MachineBehavior`、`MachineBehaviorContext`、`BehaviorState`，外壳由 GT 持有，附属不继承它，也不接触 NBT，`recipeMaps` 点名机器表或燃料表后这台机器成为 JEI 和 EMI 的催化剂，加工仍由行为自己做，`find` 读到的燃料行把产出能量存成负数的 `euPerTick`，`EnergyInput.capacity`、`emit`、`generate`、`setCapacity`、`onUse`、`maxRange`、`breakBlock` 和 `collectItems` 属于这层外壳，同名结构让 `structureFormed()`、`parts()`、`energyParts()` 和 `conditionsMet()` 可用，`parts()` 是绑定的附属零件，`energyParts()` 是这份列表的能量视图，`charge`、`setOutput` 和 `stop` 写只出的零件，`charge` 也给条件零件充能，条件零件缺电时 `tick` 仍会跑，`auxiliaryEnergy` 把第二种输入存成 `gt.aux_energy`，`energy(type)` 和 `consume(type, amount)` 只用这份缓存，改过的容量由 GT 写成 `gt.capacity`，零件缓存存在零件自己的 NBT；功能结构零件在 `LayoutPart` 上写 `slots`、`tick`、`use` 或 `field`，并用 `LayoutPartContributor` 按名字配对，方块实体由 GT 持有，槽存 `gt.part_slots`，字段存 `gt.part_fields`，没有能量角色的用 `gt6ce:addon_functional_part`，同时带能量角色的仍用 `gt6ce:addon_energy_part`，附属不继承方块实体，`LayoutPartClient` 可以为同步过来的 `PartClientView` 返回方块实体渲染器；`RecipeTable.fuel` 和 `FuelGenerator`，燃料表进入 JEI、EMI 和数据包覆盖层，机器烧流体燃料，每 tick 从正面、背面或背后 1 到 8 格输出一个能量包，多方块结构可以写 GT 零件名，或 `layoutParts()` 里的外壳；`GTValues`、`GTVoltageTiers`、`BasicMachineEnergy`、`MachineWorkCost`；附属实现的 `EnergyBlock`，`AddonEnergy` 走 GT 的能量包门槛，EU 电缆的插入和抽出接受这个接口，轴、激光纤维、中继、远距变压器和内置机器的注入或抽取经 `EnergyBlockViews` 也能看见这个接口，扫描仪、Jade 和盖板读数仍只看见 GT 自己的能量方块；`AddonTool` 的 `SPADE`、`UNIVERSAL_SPADE`、`MAGNIFYING_GLASS`，以及 `AddonTools.matches`、`isMagnifyingGlass`、`damageForUse`；`gt6ce:machine` 与 `gt6ce:machine_removal` 两种数据包配方；`GtSignalCarrier`、`GtSignalSource`、`GtSignalReceiver`、`CropScanSource` 和空的 `gt6ce:signal_wire_receivers` 方块标签。

这些保持关闭。`BasicMachineBlock` 和 `BasicMachineBlockEntity` 是 GT 的机器实现，附属机器是数据，不是子类。`Recipe.mRecipeList` 是内部行表，正式代码用 `add` 返回的 `AddonRecipes.Line`。`GTMaterial`、`GTMaterialRegistry` 和材料哨兵由 `GregTechMaterials` 代替。`GregTechTags` 由能源短名代替。`MachineRecipeMaps` 的字段由 `AddonRecipes.map(name)` 代替。`MaterialPrefix` 常量由前缀名字符串代替。完整工具目录留在 GT 里。

`onRecipesReady` 仍不是数据包重载事件。API 版本 0 包含坩埚成分、熔炼目标和合金反应，仍会把旧的材料数字 id 读成内置材料，并在这个方块或物品下次保存时写成名字。这一版不承诺固体燃料、能量转换、把内置引擎改到这套模板上、附属自绘 GUI、坩埚的只读视图、无线计算零件、往记下的坐标远程送能、坩埚方块、写入坩埚温度或内容物、任意非 GT 物品的熔化登记、坩埚数据包、不经入口就用数据包新建配方表、材料表的数据包重载、一份二进制同时跑两个加载器，或完整旧 GregAPI 替代。1.0 之前仅文档列出的入口作为兼容面。电压等级以[原GT6电压说明](VOLTAGE_TIERS.md)为准。

## 版本与兼容性

版本策略、导入变更记录和移除时间表，供查阅。

### 版本策略

依赖模组 id `gt6ce`，在 `mods.toml` 或 `neoforge.mods.toml` 里写 `versionRange="[0.0.0,)"`。`context.apiVersion()` 当前返回 **0**。本开发基线包含本文列出的全部能力，不要按导入代码的上游版本号屏蔽现有功能。API 号不能代替加载器依赖范围。

`gradle.properties` 的 `mod_version` 是 `0.MINOR.PATCH`。`GregTechAddons.API_VERSION` 就是这个 MINOR。PATCH 不改这份 API。1.0 之前，MINOR 升级可以破坏附属调用。破坏一个已经写明的入口，要么等下面的移除表到期，要么在同一版的变更表里写明。只使用未弃用入口时，写 `versionRange="[0.0.0,)"` 即可；如果附属仍依赖移除表里排期删除的旧入口（例如排期 `0.2` 移除的入口），建议把上限锁在该 MINOR 之前（例如 `"[0.0.0,0.2.0)"`），以免升级后因旧入口移除而报错。

要淘汰的入口同时标 `@Deprecated(since = "0.N", forRemoval = true)` 和 `@AddonApiRemoval(id = "<二进制名>", removeAt = "0.M")`，上面放同一段 Javadoc。`id` 是包名加类型，方法或字段再加 `#成员`。`@deprecated` 用 `{@link}` 或 `{@code}` 写出替代，并写上 `0.M`。`M` 至少是 `N + 2`：在 `0.N` 弃用的入口，`0.(N+1)` 仍然保留。`M` 小于等于当前 MINOR 时构建失败。到期后删掉入口，或把 `removeAt` 改晚并在本文写原因。没有 `forRemoval = true` 的 `@Deprecated` 不在这张表上。

### 变更

API **0** 是 GT6CE 的开发基线，包含下表全部能力。下列 1–7 只记录导入代码的上游开发阶段，不是 GT6CE 发版记录，也不是运行时功能门槛。

| 上游 MINOR | 导入内容 |
| --- | --- |
| 8 | 坩埚成分、熔炼目标和合金反应。附属用材料名声明：成分可以选择是否据此生成一条坩埚反应，熔炼目标决定熔化后变成什么，显式反应和禁用改合金表。表在材料链接结束时冻结。上游以 0.8.0 引入，CE API 0 包含这些能力；旧入口的删除期限延后到 0.10。 |
| 7 | 燃料能源机器。附属声明燃料表和一台流体燃料机器：效率、每 tick 一个能量包、储罐容量，以及正面、背面或背后 1 到 8 格。单方块和多方块都由 GT 注册并运行。上游模组版本从 0.7.0 起。材料数字入口保留到 0.9，避免在新增模板的同一 MINOR 里删掉它们。 |
| 6 | 行为型机器。附属声明槽、储罐、开关、能源和 GUI 贴图，并提供 `MachineBehavior`。方块、物品栏、能源、菜单和存档由 GT 持有。上游模组版本从 0.6.0 起。版本 3 的入口保留到 0.8，避免在新增外壳的同一 MINOR 里删掉它们。 |
| 5 | 附属材料只按名字声明。GT 在启动时从 10000 起分配数字 id，不写入存档。同名且字段一致就合并；字段不一致或只差大小写则启动失败。`builder(int, String)` 忽略传入的数字。上游模组版本从 0.5.0 起。 |
| 4 | 附属接口单独成源码集，每个加载器一个只供编译的 SDK。材料、前缀、物品、流体、配方、工具和能源走“范围”里列出的门面。上游模组版本从 0.4.0 起。版本 3 的注册和 `onRecipesReady` 保留。 |
| 3 | 配方机器和大型配方机器。上游模组版本从 0.3.0 起。版本 2 的注册、材料和 `onRecipesReady` 保留。 |
| 2 | 排队材料和追加形态。 |
| 1 | 附属注册和 `onRecipesReady`。 |

### 弃用与移除

旧入口的弃用起点统一记为 CE 基线 `0.0`。材料数字入口的最早移除期限仍为 `0.9`。四个旧贡献者和布局入口留到 `0.10`。机器声明上的旧类型，以及贡献者上的七个旧方法，保留到 `0.1`，在 `0.2` 移除。当前版本归零不提前移除任何入口。

| 入口 | 弃用版本 | 替代 | 计划移除 |
| --- | --- | --- | --- |
| com.gregtech.gregtech.api.addon.GregTechMachineContributor | 0.0 | AddonMachineContributor | 0.10 |
| com.gregtech.gregtech.api.addon.GregTechMaterialContributor | 0.0 | AddonMaterialContributor | 0.10 |
| com.gregtech.gregtech.api.addon.AddonMachineDefinition | 0.0 | RecipeMachine | 0.10 |
| com.gregtech.gregtech.api.addon.AddonMachineLayout | 0.0 | StructureLayout | 0.10 |
| com.gregtech.gregtech.api.addon.MaterialDeclaration#builder | 0.0 | MaterialDeclaration.builder(String) | 0.9 |
| com.gregtech.gregtech.api.addon.AddonMaterials#ADDON_ID_MIN | 0.0 | MaterialDeclaration.builder(String) | 0.9 |
| com.gregtech.gregtech.api.addon.AddonMaterials#ADDON_ID_MAX | 0.0 | MaterialDeclaration.builder(String) | 0.9 |
| com.gregtech.gregtech.api.addon.MachineDeclaration | 0.0 | RecipeMachine | 0.2 |
| com.gregtech.gregtech.api.addon.MachineDeclaration.Builder | 0.0 | RecipeMachine.builder | 0.2 |
| com.gregtech.gregtech.api.addon.MachineDeclaration.Kind | 0.0 | RecipeMachine.basic() / largeRecipe() | 0.2 |
| com.gregtech.gregtech.api.addon.MachineDeclaration.Target | 0.0 | IoTarget | 0.2 |
| com.gregtech.gregtech.api.addon.BehaviorMachineDeclaration | 0.0 | BehaviorMachine | 0.2 |
| com.gregtech.gregtech.api.addon.BehaviorMachineDeclaration.Builder | 0.0 | BehaviorMachine.builder | 0.2 |
| com.gregtech.gregtech.api.addon.BehaviorMachineDeclaration.Face | 0.0 | MachineFace | 0.2 |
| com.gregtech.gregtech.api.addon.BehaviorMachineDeclaration.SlotRole | 0.0 | BehaviorMachine.SlotRole | 0.2 |
| com.gregtech.gregtech.api.addon.BehaviorMachineDeclaration.ItemSlot | 0.0 | BehaviorMachine.ItemSlot | 0.2 |
| com.gregtech.gregtech.api.addon.BehaviorMachineDeclaration.FluidSlot | 0.0 | BehaviorMachine.FluidSlot | 0.2 |
| com.gregtech.gregtech.api.addon.BehaviorMachineDeclaration.Toggle | 0.0 | BehaviorMachine.Toggle | 0.2 |
| com.gregtech.gregtech.api.addon.BehaviorMachineDeclaration.Progress | 0.0 | BehaviorMachine.Progress | 0.2 |
| com.gregtech.gregtech.api.addon.FuelGeneratorDeclaration | 0.0 | FuelGenerator | 0.2 |
| com.gregtech.gregtech.api.addon.FuelGeneratorDeclaration.Builder | 0.0 | FuelGenerator.builder | 0.2 |
| com.gregtech.gregtech.api.addon.FuelGeneratorDeclaration.Kind | 0.0 | FuelGenerator.single() / multi() | 0.2 |
| com.gregtech.gregtech.api.addon.FuelGeneratorDeclaration.Output | 0.0 | FuelOutput | 0.2 |
| com.gregtech.gregtech.api.addon.FuelGeneratorDeclaration.Output.Side | 0.0 | FuelOutput 的 front、back、behind | 0.2 |
| com.gregtech.gregtech.api.addon.MachineLayout | 0.0 | StructureLayout | 0.2 |
| com.gregtech.gregtech.api.addon.MachineLayout.Cell | 0.0 | LayoutCell | 0.2 |
| com.gregtech.gregtech.api.addon.StructurePartDeclaration | 0.0 | LayoutPart | 0.2 |
| com.gregtech.gregtech.api.addon.StructurePartEnergy | 0.0 | LayoutPart.energyOutput(EnergyOutput, capacity) | 0.2 |
| com.gregtech.gregtech.api.addon.RecipeMapDeclaration | 0.0 | RecipeTable.machine(name) | 0.2 |
| com.gregtech.gregtech.api.addon.FuelMapDeclaration | 0.0 | RecipeTable.fuel(name) | 0.2 |
| com.gregtech.gregtech.api.addon.AddonMachineContributor#machines | 0.0 | addonMachines() | 0.2 |
| com.gregtech.gregtech.api.addon.AddonMachineContributor#behaviorMachines | 0.0 | addonMachines() | 0.2 |
| com.gregtech.gregtech.api.addon.AddonMachineContributor#fuelGenerators | 0.0 | addonMachines() | 0.2 |
| com.gregtech.gregtech.api.addon.AddonMachineContributor#layouts | 0.0 | structureLayouts() | 0.2 |
| com.gregtech.gregtech.api.addon.AddonMachineContributor#parts | 0.0 | layoutParts() | 0.2 |
| com.gregtech.gregtech.api.addon.AddonMachineContributor#recipeMaps | 0.0 | recipeTables() | 0.2 |
| com.gregtech.gregtech.api.addon.AddonMachineContributor#fuelMaps | 0.0 | recipeTables() | 0.2 |

## 许可

SDK与项目代码采用LGPL-3.0-or-later；原作者、第三方代码和资产各自声明继续保留，见根LICENSE、COPYING、NOTICE。
