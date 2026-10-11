# 原版电池盒、太阳能板与变压器提示 / 2026-10-07

本批承接 `d7b7c3d3`，继续用户要求的全方块专属提示和原版材料记录。共用 `OriginalEnergyDeviceTooltipData` 保存原范围及提示选择，Forge/Neo 的 `EnergyDeviceTooltips` 仅负责原生文字，物品的 `BlockEntityTag`/`BLOCK_ENTITY_DATA` 读取留在两端。

## 实施范围

- 原10080–10099：20种4/16槽电池盒，输入为额定电压的一半至两倍，输出固定包大小，原三条选择器覆盖板/模式0/模式1–15说明和扳手朝向提示。原 `TileEntityBase01Root` 的 ULV 最低4EU 接入两端实际能量入口，撤去此前错误1EU。
- 原10050/10051：2太阳能板，只有额定八分之一至额定的输出、原朝向提示；不添加非原效率或蓄电容量。
- 原10040–10048与13材质旋转变压器：22设备的普通/反向范围、原反向输入推荐值、100.00%效率、扳手与活口扳手提示。读取物品中的实际 `gt.inverted`，不忽略反向状态。
- 格式依原 `LH.addEnergyToolTips` 和 `TE_Behavior_Energy_Stats.addToolTips` 两种分支：电池固定输出不显示范围/面名；转换器总是显示范围。保留原整数、硬编码 `up to`/`to` 和换向后仍固定的本地化面名。实际换向后的物理面行为未改。EU蓝、RU绿照TD/TagData原颜色。
- 44设备沿用既有精确材料登记与单次高级提示。439固定方块、252机器档位、120漏斗的材料生成/登记未修改；这些数有索引交叠，不等于不同机器/物品合计。不存在10049导致XV大型电池盒原REV拒绝、无数据输入跳过等既有边界保留，不能编造组件。

## 来源与中文

[来源清单](energy-device-tooltip-source-20261007.json)固定19原版/许可证/汉化文件及4个 `_w` 比较文件的SHA、字节数、作者及LGPL-3.0-or-later证据。以用户指定的1.7.10为行为权威；`_w` 同一电池/太阳能/转换器/双向分支用于对照，不导入第二套平台实现。其他两快照和saltnya的材料提示选择见前轮 [来源](functional-tooltip-source-20261007.json)。

15个相关中文键均逐字匹配 `C:/Dev/GregTech_zh_cn.lang`，中文JSON值与本批基线相同；只补8个缺失英文键。没有生成翻译。只读源/比较文件复核保持相同。

## 有限检查

- `:core:machineSpecBehaviorContracts`：199断言通过，其中新增76条源固定范围、各档电池槽数/输出模式、ULV小包吞掉且不入储存/边界4EU、9电力变压器双向值及2太阳能/木旋转变压器原参数检查。
- 两端主源码及 `compileProductionSmokeJava` 合并增量编译23秒通过；复核TD颜色后最终增量编译21秒通过，后次core/探针任务up-to-date。既有弃用API警告仍存在。
- 下轮普通成品探针新增44设备提示单次完整性、实际原生工厂对象的六个范围getter，以及22换向物品数据形状/原生切换后getter一致性检查；本轮仅编译，没有运行。前轮52轴/13自定义齿轮箱/5大锅炉的新探针同样尚未运行。
- 首次来源账本脚本猜测 `addBlock` 行名，得到0而失败；查明生成表使用 `BLOCK_DATA.put`，仅修正账本解析。没有改变生产数据或减少测试/覆盖范围。
- 未生成新普通JAR；按用户要求数轮集中普通打包/验包/正式加载器自启动。最后已验收目录仍是 `build/verified/20261006-195055Z-1fceed96`，源码 `ae3e1857`，不含本批及 `d7b7c3d3`。

可重现入口：Gradle8.8/JDK21，`--offline --no-daemon --console=plain -I work/emi-local.init.gradle :core:machineSpecBehaviorContracts :compileJava :neoforge:compileJava :compileProductionSmokeJava :neoforge:compileProductionSmokeJava`。两个日志及SHA固定在来源清单，原始日志在忽略的 `work/energy-device-tooltips-compile-20261007.log` 与 `work/energy-device-tooltips-final-compile-20261007.log`。

## 继续范围

电动机/发电机/磁铁/热冷转换器/涡轮的完整原行为和专属提示仍是独立缺口，没有借本批通用范围冒称已对齐。其余全方块材料与提示、采集工具等级/火焰/所有者/覆盖状态、旋转泵和legacy大锅炉原型、完整生存供能、玩家悬停、独立世界保存重启/历史档及实际外部提供方继续未验收。ULV行为修正本轮证据为源算术/gate及原生编译，不是实际世界能量输送。

本批仅本地提交，不推送，PR暂缓；整个持续移植goal active。
