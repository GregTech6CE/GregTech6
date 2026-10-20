# 材料表空格与生成端保持

2026-10-20，基线 `36833296c`。用户亲自将 `CompoundMaterials.java` 的 570 条声明对齐：等号第 66 列、颜色第 141 列。本轮保留该编辑；相对基线，执行代码、1058 个字面量、注释及顺序均未改变。

## 两种贴图集写法

`DisulfuricAcid` 使用 `dust(..., 0xFF9600).setTextureSet(FINE)`；紧随其后的 `HexafluorosilicicAcid` 使用 `elec(..., 0xBEC8BE, SHINY)`。

`MaterialFactories.dust` 不接收贴图集参数，默认赋予 FINE，后置 setter 再写一次同一字段。`elec` 把贴图集传给 `gem`。生成器 `transpile_gt6_materials.emit_material` 对 gem／elec／cent 直接输出贴图参数，对其他分支输出后置 setter，因此语法不一致。相同贴图集用参数或 setter 设置的目标字段相同；FINE 与 SHINY 则是不同外观。两种工厂还会添加不同属性，不能为统一排版直接互换。`setStats` 的三个参数依次为熔点、沸点、密度，与贴图调用的位置无关。本轮没有改变材料属性。

## 避免重生成覆盖用户排版

旧工具只在同类工厂／相同参数个数组内对齐。`format_definition_tables.align_java_tables` 现增加可选的全表 RGB 列排版，仅 CompoundMaterials 启用：先保持工厂内部参数对齐，再根据最长前缀计算公共颜色列。列宽自动计算，没有写死 66／141；其他表继续使用原格式。

`modular_materials.write_catalog` 对 CompoundMaterials 传入该选项，独立排版命令也使用同一规则。没有重写用户生产文件。

## 定向验证

实际从原版 MT 经 `read_source → collect_materials → generate_java → split_ore_processing → write_catalog` 输出到隔离的 `work/`。七份材料输出的 UTF-8 文本与当前生产文件逐字一致：用户材料表保持新格式，其余六表保持原样，中间快照也不变。三个现有读取器分别产生相同顺序／分类的 570、570、441 条结果；排版幂等，独立入口报告零文件需要改写。

来源和结果见[回执](verification/material-table-formatting-20261020.json)。生成链原有 14 条矿物处理跳过仍然存在。本轮未运行 Gradle、游戏或完整测试，也不把排版核对当作全部材料语义已对齐。
