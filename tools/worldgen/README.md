# 世界生成调试

## 游戏内命令

以下命令需要二级管理权限。在聊天栏输入 `/gt6ce worldgen` 后可按 Tab 补全。

| 子命令 | 用途 |
| --- | --- |
| `inspect` | 查看当前区块的岩层、实际矿物、地表指示物和规划矿脉。 |
| `locate <ore\|layer\|bedrock_ore> <材料> [区块半径] [saved]` | 先查附近已加载的方块，再按种子推算候选位置或查已保存区块。半径默认 256，范围 1～512。 |
| `profile [x\|z [宽度]]` | 导出当前位置的实际地质剖面与规划岩层，宽度 1～512 格。 |
| `sample <半径>` | 统计半径 0～8 区块内已加载的矿物数量、高度及距离。 |
| `export <边长>` | 从当前区块向正 X、正 Z 导出 1×1 至 64×64 个区块，一次只运行一个导出任务。 |

`/gt6ce worldgen locate ore Chromite 128 saved` 只查实际方块。后台最多读取 2,048 个已保存区块。损坏的区段会记入服务端日志，其他区段继续搜索。定位不会生成新区块。

`/gt6ce worldgen export 5` 会分批读取 25 个区块。导出期间需要保持目标区域已加载。结果保存到存档的 `gt6ce-worldgen/chunk-sample-*/`，包含各区块的 `ores.csv`、各岩石的 `rocks.csv` 和最后写入的 `sample.properties`。区块缺失时命令会报告坐标并终止本次导出。

## 离线工具

从仓库根目录运行，使用 JDK 17。以下参数给出一组可直接复用的例子。

```powershell
.\gradlew.bat :buildtools:sampleWorldgen '--args=73 work/worldgen/layers 0 0 512 -35'
.\gradlew.bat :buildtools:surveyWorldgen '--args=42 work/worldgen/survey-42 120 120 64 143'
.\gradlew.bat :buildtools:compareWorldgen '--args=work/worldgen/compare 12 work/worldgen/old work/worldgen/new --mineral=Chromite'
.\gradlew.bat :buildtools:checkWorldgenDistribution '--args=work/worldgen/check Chromite=512 work/worldgen/survey-42'
```

- `sampleWorldgen` 按种子、中心 X/Z、宽度和 Y 输出岩层切片。最后可加 `--legacy` 对照旧岩层。
- `surveyWorldgen` 在指定区块起点、边长和地表高度下模拟主世界矿物分布，输出各矿的数量与距离。
- `compareWorldgen` 读取同种子、同维度和同范围的两份实际导出。第二个参数指定统计起点排除的边缘区块数。可用 `--mineral` 输出指定矿物的热力图。
- `checkWorldgenDistribution` 按材料检查到普通矿的第 99 百分位距离，阈值单位为格。可追加多个种子的报告。比较区块生成速度时，应使用实际生成到完整区块的耗时。
