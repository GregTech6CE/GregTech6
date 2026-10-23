# 2026-10-04 / 自动合成权限与原版材料转换

## 原版依据和实现

接续上一批的自动合成权限缺口。本地原版的 `Loader_Recipes_Handlers:552–625` 有 64 处 `AdvancedCrafting1ToY/XToY` 注册，均传入 `F`；这包含固定形态、普通/富集矿及导线尺寸循环。`AdvancedCrafting1ToY/XToY` 保存该布尔值，普通输入相同形态/材料/格数的工作台配方由这些源配方接管；`RecipeMapAutocrafting:73–83` 从 ALLOWED 列表排除其 false 配方。另有八处显式 CR.DEF_NAC 属于未移植的 IC2/Steamcraft2 兼容物品，不凭空新增这些物品或配方。

双平台材料转换提供器改用带 `AutocraftableCraftingRecipe` 标记的配方对象；普通形态/矿块及导线大小转换保持 false。管道拆分来自 `MultiTileEntityPipeFluid:100–101` 的 `CR.DEF_NCC`，显式保留 true。普通 shaped/shapeless 默认仍允许，JSON、Forge 网络布尔字段和 NeoForge MapCodec/StreamCodec 均接回 `gregtech_autocraftable`。

运行时转换注册记录源输入、当前统一物品别名和占用格数，发布不可变共享规则快照。自动合成机对非 GT 配方检查这些规则，避免走普通铁锭、金块等原生配方时绕过源转换权限；混合不同材料不会被误当作同一种源形态。明确实现原版权限接口的 GT 配方仍按自己的标记处理。正常搜索与近期列表均检查权限，初始化时已知 false 的对象直接不加入 ALLOWED。

新增 `tools/apply_gt6_autocrafting_permissions.py`，逐条绑定源前缀、相同材料、输入格数与输出数量。应用到 1,272 条现有静态 JSON（1,260 条导线尺寸、八条板块转换、四条塑料/橡胶碎粒转换），附 false 和原版行号；不按图案或文件名一概禁用配方。当前自定义 GT 配方和未绑定的外部内容不由该脚本推断。64 处构造器、八处外部 NO_AUTO 和具体文件绑定见 [绑定回执](autocrafting-permissions-bindings-20261004.json)；`--check` 返回 0 pending。

源导线循环中的 `if (tAmount < 10)` 只控制合并，拆分没有该限制。两版运行时提供器此前将两个方向一起跳过，现恢复 10–16 根的拆分，合并仍限九格。

原版作者 Gregorius Techneticies / GregTech-6 Team，LGPL-3.0-or-later；原项目只读、保留 NOTICE，没有新增资产或依赖。[来源与当前文件散列](autocrafting-permissions-source-20261004.json)。代码提交：`78198f05a0c0309d6c54bce84b8f6f2bc8098188`；构建先于提交，不声明 JAR 内嵌 Git SHA。

## 实际验证

最初的 native 标记/重载数据边界双版快速 compileJava 29s 成功，日志 `work/autocrafting-permissions-compile-20261004.log`。加入统一别名规则与共享样例后，仅运行一次正式共享检查和双版构建：

```
.\work\gradle-runtime\gradle-8.8\bin\gradle.bat --offline -I work/emi-local.init.gradle :core:check :assemble :neoforge:assemble -x :core:processResources
```

1m08s 成功，日志 `work/autocrafting-permissions-build-20261004.log`。共有 9,778 条共享断言（行为 2,045 / 20 组、材料 7,646、机器 34、温度 53）；新增 15 项固定边界样例，覆盖统一别名、不同材料、九格与单格、未知格数、导线/流体管差别、不可变重载快照及同名不同注册身份。没有复制算法作为预期答案。

确认本批资源差异恰为 1,272 条绑定记录后，逐字节同步到已有 core 构建输出，排除重复全量 processResources。ALLOWED 初始化过滤收尾仅双版 assemble，50s 成功，日志 `work/autocrafting-permissions-final-build-20261004.log`，没有重复共享检查。

最终 [成品验包](autocrafting-permissions-artifacts-20261004.json)确认 640 个共享 class 与当前编译字节一致、平台 metadata/NOTICE、无重复/测试内容；[变更验包](autocrafting-permissions-changed-artifacts-20261004.json)确认每包 1,272 条变化资源与源码字节相同，Forge 九个变更原生 class 存在于重混淆成品，NeoForge 十个与编译字节完全一致。

## 边界和后续

没有实际客户端、网络同步往返、机器加工、生存取得或存档重载证据。新的权限通过代码/来源绑定/共享契约/编译及打包验证，不能据此声称整个工作台或自动化系统已与原版完全一致。

现有材料转换仍有前序记录的缺口：竞争转换结果、部分前缀和跨模组材料回调、统一物品的实际手动工作台匹配/顺序未完整对照；这些不会被本批权限修复自动补齐。八条外部 NO_AUTO 兼容内容未移植，其他 mod 的 recipe API 仍需实际运行证据。蓝图容器工具数据处理、附带数据保留、真实重载及整体反馈账本继续处理。整体移植仍未完成。
