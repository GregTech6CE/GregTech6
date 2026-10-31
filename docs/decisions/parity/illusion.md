# 幻觉物品

GT6CE 重构期间凭空产生、原版 GT6 并不存在的注册项及其清理；移除重复 ID 不等于删除对应功能。

| 内容 | 差异 | 原因 |
| --- | --- | --- |
| 占位材料 | 撤销 `Wool`、`PlantMatter`、`Soil`、`Feather`、`AnimalShell`、`DyeMatter`、`MagicMatter`、`Dripstone` 占位注册。 | 缺少原版依据，不再凭物品名称编造材质。 |
| 同名材料 | 旧占位声明删除后，`Sculk/Tuff` 已单独注册，`Calcite/Prismarine/Deepslate` 仍有原材料。 | 不把占位清理误记成材料删除。 |
| 占位电池 | 移除 `battery_lv/mv/hv/ev/iv`、`battery_eu_*` 旧入口，使用按化学体系和电压分型的电池。 | 恢复原版电池分类，去掉占位型号。 |
| 虚构扩展器等级 | 去掉 `extender_basic/advanced/elite/wireless`，使用 `inventory_tank_extender` 等实际类型。 | 原版按功能分类，不按这四档分级。 |
| 多余 ZPM 档位 | 去掉 `zpm_discharger_elite`，保留原版 QU/EU 两种放电机。 | 原版没有第三档。 |
