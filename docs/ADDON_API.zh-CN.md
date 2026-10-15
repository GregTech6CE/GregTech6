# Addon 接口

[English](ADDON_API.md) | [中文](ADDON_API.zh-CN.md)

API版本 **2** 在 Forge 1.20.1 与 NeoForge 1.21.1 使用两个入口。配方仍在自己的 `@Mod` 构造方法里注册 `GregTechAddon`；GT完成材料/物品关联和内置机器配方后，在common setup调用一次 `onRecipesReady`。按Addon模组ID排序调用；重复ID、晚注册、二次派发明确报错，回调异常使初始化失败，生命周期记为FAILED。

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

导入 `com.gregtech.gregtech.api.addon.GregTechAddon` 与 `GregTechAddons`。可直接使用现有 `GTMaterialRegistry`、`MaterialPrefix`、`GTValues.U`、`GTVoltageTiers`，机器配方仍进入原有 `MachineRecipeMaps`，不建立另一套配方底层。英文说明提供完整配方示例；你的输入物品须先由自己的加载器注册。

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

运行 `gradlew.bat :core:addonApiJar` 得到 `core/build/libs/gregtech6-addon-api-0.0.0.jar`。将其作为 `compileOnly files(...)` 开发依赖，**不要再次打包或shade这些类**；运行时由已安装的GT提供。SDK只包含共享领域和接口、许可声明，不含Minecraft素材。使用平台配方/物品栈/能源适配时还须对应版本的GT开发源码/开发依赖；生产重映射JAR不是通用开发依赖。目前没有发布Maven坐标。

Addon须声明必需依赖模组 **`gregtech6`**；GT内容和保存命名空间仍为 **`gregtech`**。分别构建Forge和NeoForge版本，SDK不会让Forge二进制自动跨加载器运行。

当前支持注册和配方就绪生命周期、4291–7899 号段的排队材料和已有前缀的追加形态（生成 `gregtech:` 物品）、已有材料查询、现有机器配方、能源合同，以及 `GtSignalCarrier`、`GtSignalSource`、`GtSignalReceiver` 和空的 `gregtech:signal_wire_receivers` 方块标签。它不是数据包重载事件。API 版本 2 不承诺新前缀、流体、新机器类型、材料表的数据包重载、旧存档迁移、跨加载器物品栈或完整旧 GregAPI 替代。0.0.0 阶段仅文档列出的入口作为兼容面，内部实现可能调整。电压等级以[原GT6电压说明](VOLTAGE_TIERS.md)为准。

SDK与项目代码采用LGPL-3.0-or-later；原作者、第三方代码和资产各自声明继续保留，见根LICENSE、COPYING、NOTICE。
