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

运行 `gradlew.bat :core:addonApiJar` 得到 `core/build/libs/gregtech6-addon-api-0.0.0.jar`。将其作为 `compileOnly files(...)` 开发依赖，**不要再次打包或shade这些类**；运行时由已安装的GT提供。SDK只包含共享领域和接口、许可声明，不含Minecraft素材。使用平台配方/物品栈/能源适配时还须对应版本的GT开发源码/开发依赖；生产重映射JAR不是通用开发依赖。目前没有发布Maven坐标。

Addon须声明必需依赖模组 **`gregtech6`**；GT内容和保存命名空间仍为 **`gregtech`**。分别构建Forge和NeoForge版本，SDK不会让Forge二进制自动跨加载器运行。

当前支持注册和配方就绪生命周期、已有材料查询、现有机器配方及能源合同。它不是数据包重载事件。新材料自动物品/流体注册、新机器类型、跨加载器物品栈和完整旧GregAPI替代仍待完善，不列入已支持范围。0.0.0阶段仅Addon入口与文档所列合同作为初始兼容面，内部实现可能调整。电压等级以[原GT6电压说明](VOLTAGE_TIERS.md)为准。

SDK与项目代码采用LGPL-3.0-or-later；原作者、第三方代码和资产各自声明继续保留，见根LICENSE、COPYING、NOTICE。
