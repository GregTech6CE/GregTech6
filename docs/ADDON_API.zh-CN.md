# Addon 接口

[English](ADDON_API.md) | [中文](ADDON_API.zh-CN.md)

API版本 **1** 在 Forge 1.20.1 与 NeoForge 1.21.1 使用同一个 `GregTechAddon` / `GregTechAddons` 入口。Addon 在自己的 `@Mod` 构造方法中注册实现；GT完成材料/物品关联和内置机器配方后，在common setup调用一次 `onRecipesReady`。按Addon模组ID排序调用；重复ID、晚注册、二次派发明确报错，回调异常使初始化失败，生命周期记为FAILED。

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

运行 `gradlew.bat :core:addonApiJar` 得到 `core/build/libs/gregtech6-addon-api-0.0.0.jar`。将其作为 `compileOnly files(...)` 开发依赖，**不要再次打包或shade这些类**；运行时由已安装的GT提供。SDK只包含共享领域和接口、许可声明，不含Minecraft素材。使用平台配方/物品栈/能源适配时还须对应版本的GT开发源码/开发依赖；生产重映射JAR不是通用开发依赖。目前没有发布Maven坐标。

Addon须声明必需依赖模组 **`gregtech6`**；GT内容和保存命名空间仍为 **`gregtech`**。分别构建Forge和NeoForge版本，SDK不会让Forge二进制自动跨加载器运行。

当前支持注册和配方就绪生命周期、已有材料查询、现有机器配方、能源合同，以及 `GtSignalCarrier`、`GtSignalSource`、`GtSignalReceiver` 和空的 `gregtech:signal_wire_receivers` 方块标签。它不是数据包重载事件。新材料自动物品/流体注册、新机器类型、跨加载器物品栈和完整旧GregAPI替代仍待完善，不列入已支持范围。0.0.0阶段仅Addon入口与文档所列合同作为初始兼容面，内部实现可能调整。电压等级以[原GT6电压说明](VOLTAGE_TIERS.md)为准。

SDK与项目代码采用LGPL-3.0-or-later；原作者、第三方代码和资产各自声明继续保留，见根LICENSE、COPYING、NOTICE。
