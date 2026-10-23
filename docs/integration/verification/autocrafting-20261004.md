# 2026-10-04 / 自动合成机、蓝图与 USB 复制

## 改动与来源

接续 Technology 缺口，双平台自动合成机增加原版 `(80,43)` 程序槽：使用已记录纸蓝图、USB 1.0 及更高等级替代物，或邻接 USB/HDD 端口的线缆。程序槽与原有 9 输入/12 输出分离，不进入自动化能力；保存、缺失旧标签加载、移除掉落和 Shift 点击路径已接入。自动输入只接受当前蓝图中的相同物品数据及既有静态配方输入。

`RecipeMapAutocrafting` 的配方查找、近期优先列表、手工具拒绝、精确九格输入聚合、每格普通容器返还与无限机械臂头保留已接续。基础 1,024 ticks / 16 EU，按 `Recipe:904–939` 原算法取消相同输入/输出并同比缩减；保留其原分支语义。原版构造器三个布尔值为 optimize/unificate/canBeBuffered，没有据此增加“输出必须全空”的限制。现代配方缓存随 RecipeManager 内容身份变化重建。

蓝图写入采用原版 `gt.blueprint.craft`，键 `0` 至 `8`、每格一件；既有 `gt.CraftingPattern` 列表仍可读取，工作台幽灵预览同步使用这两种结构。机器程序槽空提示来自 LH 原文。蓝图制作增加纸 + 144mB 靛蓝/水混蓝染液/花蓝染液/化学蓝染液的四条 Bath 声明，16 ticks、0 EU。

扫描/打印使用真实数据提供器，另补 GT6_Main 三条展示记录并删除“无蓝图物品”的过期跳过理由：Visual Scanner 消耗一张蓝图与一个 USB 再返还两者，USB 写入 tier-1 蓝图文件，64 ticks / 16 EU；Printer 保留 USB/线缆，空白蓝图 + 16mB 化学白染液、32 ticks，或纸 + 144mB 化学蓝染液、128 ticks，均 16 EU。输出命名来自实际合成结果，打印数据与自动合成共用原版格式。

来源为 Gregorius Techneticies / GregTech-6 Team，沿用 LGPL-3.0-or-later 与 NOTICE；没有修改原项目、增加依赖或引入资产。18 个原始文件与当前生产文件 SHA-256 见 [来源回执](autocrafting-source-20261004.json)。代码提交：`651d5a5fade5b73bc9dce3444be2b362225e35ce`；构建先于提交，不声明 JAR 内嵌 Git SHA。

## 本次问题：上层基岩

对照 `WorldgenOresBedrock:181–212` 和 `WD.removeBedrock:782–801`：首次矿点生成将底层上方第 1–6 层的 muffin 轮廓替换为深板岩/地狱岩，再布矿；若原版未提供深板岩，removeBedrock 使用当地普通石材。它不是替换为空气，也不删除底层或轮廓外基岩。此前 `7bff1d9f` 已将母岩替换接入两版，现代高度相对世界最低层。本批仅复核，没有重新修改该代码；既有区块不会因为新的生成逻辑自动改写。

## 实际验证

首轮快速编译发现局部变量与 lambda 参数同名，修正后第二次双版 compileJava 22s 成功，分别见 `work/autocrafting-compile-20261004.log` / `work/autocrafting-compile2-20261004.log`。失败没有计为通过。

仅两个共享语言 JSON 有资源变化；同步到已有 core 输出后，运行一次普通 classpath 的共享检查和双版打包，排除重复 `:core:processResources`：

```
.\work\gradle-runtime\gradle-8.8\bin\gradle.bat --offline -I work/emi-local.init.gradle :core:check :assemble :neoforge:assemble -x :core:processResources
```

5m 成功，日志 `work/autocrafting-build-20261004.log`。共享检查 9,763 条断言（行为 2,030 / 19 组、材料 7,646、机器 34、温度 53）；本批新增 16 项固定来源样例，覆盖同物聚合、不同数据区分、手工具拒绝、机械臂保留、容器输出阻止缩减及原版费用/坐标。

蓝图取得与复制路径补齐后，仅运行双版 assemble，55s 成功，日志 `work/autocrafting-final-build-20261004.log`，没有重复共享测试。最终 [成品验包](autocrafting-artifacts-20261004.json)确认 637 个共享 class 与当前编译字节一致、平台 metadata/NOTICE、无重复及测试内容；[变更验包](autocrafting-changed-artifacts-20261004.json)确认两份语言文件源码字节一致、两版各 38 个变更原生 class 存在，NeoForge 与编译字节完全一致。Forge 原生类只声明重混淆后存在。

## 仍有缺口

没有启动 Minecraft 客户端或 GameTest，没有实际 Bath/Scanner/Printer/Autocrafter 生存加工、原生配方注册运行或存档重载证据。普通纸与本模组媒介身份已接入，其他模组的纸/蓝图/无限工具标签需后续对照。

已提供 `AutocraftableCraftingRecipe.isAutocraftableByGT()` 与可选 JSON `gregtech_autocraftable: false` 拒绝路径，但其他导入器仍未完整保留原版 `CR.NO_AUTO` 标记；不能将全局自动合成权限声明为原版一致。蓝图写入中容器工具的 NBT 清理、空白纸附带数据保留及其他外部扫描内容仍待按源头接续。整体移植仍未完成。
