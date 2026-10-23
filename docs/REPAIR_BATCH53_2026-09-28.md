# 第五十三批：四种低压电动工具材料装配

## 范围与原版依据

大型坩埚沿用第四十五批，尚未实机验证，本轮继续生存移植。未重新宣称发现／修复坩埚故障。

- GT6 `Loader_Tools.java:352-359`：电钻、电锯、电动扳手、电动螺丝刀的 LV 装配；要求材料类型 ≥3、品质 ≥1，排除木材／反物质。当前材料全集中的六种 BOUNCY/STRETCHY 聚合物工具类型均 ≤1，已被同一门槛排除；以后新增高类型弹性材料必须补属性检查，不能仅沿用这个数据前提。
- `Loader_Tools.java:382-513`：容量取电池；普通 LV 电钻用杆和螺丝，不用矿用电钻的独立工具头；其余三种用相应工具头。原版材料监听器存在该头形态才生成配方。
- `MT.java:3690`：Electric_T[1] 是镀锌钢。机身显示材料使用 Loader_Tools 的 Orange，而不是消耗掉的机壳材料。原版配方未启用镜像。
- `MultiItemTool.java:164-199`：装配成品默认 0 EU，不继承输入电池的当前电量。

## 已完成

- 新增 `content/tool/ElectricToolAssembly.java`，统一维护四类图案、材料条件、工具与外壳部件。
- 按当前注册材料生成 756 条有序配方（服务器启动日志已确认），使用既有 ToolShapedRecipe 序列化／消耗工具逻辑；四类配方均返还磨损后的螺丝刀，电钻另返还磨损后的锉刀。
- 通过现有运行时配方生命周期加入服务器，登录和 reload 同步至客户端，因此使用 JEI 标准合成页。配方结果携带材料和容量 NBT，不额外注册成百上千种电动物品。
- 杆、螺丝、环、板使用通用 Forge 材料标签加本模组实物兜底；不混用不同材料。曲板和专用头使用现有具体物品。
- `ElectricToolItem` 保存主材料及电池容量，显示材料化名称与电量；工具头按实际材料染色，LV 机身为原版橙色，覆盖层不染色。旧无材料 NBT 的创造物品保持钢工具头兜底。
- 注入／抽取先做整数除法再限制包数，避免 `amount * size` 溢出；读档电量夹在 0 和容量之间，电量条使用实例容量。

## 验证状态

compileJava 已通过（build/compile_batch53.log）。新增三项 GameTest：四类装配／加工工具返还／错误材料与电池拒绝；所有生成配方服务器存在与网络 NBT 完整性；颜色／容量存档／能量模拟与溢出。
全新目录 `build/gametest-batch53-electric`，4096 MiB / G1GC / 8 处理器：2026-09-28 03:02:39 **All 985 required tests passed**，0 条 failed!；`BUILD SUCCESSFUL in 12m 18s`，退出码 0。最终日志 `build/gametest_batch53_electric.log`。门禁期间 Java／资源冻结，最新 Java 时间早于最终日志。两项资源测试通过，日志 `build/assets_batch53.log`。

`audit_porting_progress.py --log build/gametest_batch53_electric.log` 已刷新快照；缺失 token 仍为 49。新增配方不增加物品／方块注册量；不能据此声称工具和生存系统已全部完成。

## 客户端验收

1. 完全重启 runClient，在 JEI 查询电钻、电锯、电动扳手、电动螺丝刀的合成用途，选择钢和钛等不同材料。
2. 用展示的材料、电机、低压电池和真实可用加工工具合成；加工工具应保留并磨损，部件消耗；混用螺丝和头的材料、放错电池等级或镜像排列不应合成。
3. 成品名称／头部颜色随材料变化，机身橙色。合成后空电，LV 电池装配容量为 100000 EU；充电后存档重进应保留。
4. 电钻原有嵌入炸药行为继续每次消耗 100 EU，上一批回归一起验证。

## 明确未完成与下一步

本批只闭合四种已有工具的材料装配层，不代表电动工具完整还原。电动扳手／螺丝刀的机器工具分发、钻头／链锯的采掘能力、概率性材料磨损、坏工具保留机身、可拆卸零件返还尚需下一批对照 GT6。

还需扩展原版 MV/HV 工具、矿用电钻、搅拌器、电锯片／修剪器／冲击钻，核对电池本体的完整生存路线与原版每种电池容量。当前只使用已注册的 battery_lv，不假称五类电芯已经等价于完整可充电电池系统。材质图案仍使用既有 metallic 工具头纹理，真实材料颜色已接通，不宣称全部 icon set 动态切换已完成。

本轮不构建 jar，未客户端像素验收。没有执行 git commit。

## 下批只读定位

- `GT_Tool_Drill_LV.java:106-110` 明确 isMiningTool=false，只按 drill 采收类型判定；不要直接将它改成普通电镐。矿用电钻为另外的工具族。
- `MultiItemTool.java:433-471` 电动工具每次耗能，但以 `1/max(10,quality*20)` 概率累加材料磨损；材料耐久源于 durability×100×工具倍率。
- `Loader_MultiTileEntities.java:1009-1013` 原版铅酸电池是可放置的五档方块，容量 V[tier]×2000，LV 为 64000 EU。当前 battery_lv=100000 是旧移植通用电池数值；本轮只继承既有输入容量，不把 100000 宣称为原版铅酸容量。
- 原版铅酸 LV 图案 ` Wx/PBP/ B `：电池合金板×2、已灌酸铅酸电芯×2、对应绝缘电缆、剪线钳。应优先补电池实际家族及配方，再让四种工具接受相应电池输出；不能只加一个虚构配方掩盖电池缺口。

## 本轮文件清单

Java 包根为 `src/main/java/com/gregtech/gregtech/`：
- 新增 `content/tool/ElectricToolAssembly.java`、`gametest/ElectricToolAssemblyTests.java`。
- 修改 `item/ElectricToolItem.java`、`client/GregTechClient.java`、`loaders/Loader_ToolCraftingRecipes.java`。
- 语言：`src/main/resources/assets/gregtech/lang/en_us.json`、`zh_cn.json`，各增加材料化名称和电量提示两个键。
- 资源测试：`tools/tests/test_electric_tool_resources.py`，两项通过；检查四种模型的四层纹理均存在、双语占位符和物品名齐全。
- 进度文档：本报告及本批收尾时的 NEXT_GOAL / PORTING_AUDIT / REPAIR_TRACKER / porting-progress 快照。

补充避免后续误报：项目已有 `battery_eu_8/32/128/512/2048` 高级可放置储能节点及 battery_box，并非所有电池方块都缺失。下批须区分这些通用/高级节点、普通铅酸等化学电池家族、五种电芯、battery_lv 等手持通用电池；检查各自配方和能量数据如何对接，复用已有实现。
