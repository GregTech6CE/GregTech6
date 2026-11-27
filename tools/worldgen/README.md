# 世界生成勘探与离线工具

## 游戏内定位

需要 2 级命令权限。两个入口接受相同的参数，按 Tab 可补全当前维度启用的材料和搜索半径。

```text
/gt6ce worldgen locate ore Chromite 128
/locate gt6ce ore Chromite 128
```

将 `ore` 换成 `bedrock_ore` 可查基岩矿，换成 `layer` 可查岩层。半径单位为区块，默认 256，范围 1～512。先检查周围 3 区块内已加载的实际方块，再在指定半径内按种子和生成配置寻找候选。聊天结果标明实际方块或生成候选，点击坐标可填入传送命令。未探索区域的候选受地形与可替换方块影响。

## 扫描已有存档

关闭世界或使用已停止写入的存档副本。从仓库根目录运行，输出选一个全新的目录。下面读取从区块 (0, 0) 开始的 16×16 个区块，并为铬铁矿画热力图。

```powershell
.\gradlew.bat :worldgen-tools:scanSave '--args="D:/Minecraft/saves/Example" reports/chromite minecraft:overworld 0 0 16 Chromite'
```

`scanSave <存档> <输出目录> <维度 ID> <起点区块 X> <起点区块 Z> <边长 1～256> [材料]` 支持两端存档、原版和自定义维度。省略材料会在图中合计所有识别到的矿种。数据表始终包含扫描到的全部 GT 矿物。

| 输出 | 内容 |
| --- | --- |
| `ore-map.svg`、`ores.csv` | 按区块统计普通矿、贫矿、基岩矿和地表粗矿。 |
| `markers.csv`、`rocks.csv` | 石子的坐标、外观、材料和物品，以及已识别岩石的体积。指示石和同材料普通石子分别记录。 |
| `heights.csv`、`skipped.csv` | 区块最高非空区段，以及未保存、不完整或不可读的区块与区段。 |
| `sample.properties` | 种子、维度、区域和完整标记，数据写完后生成。有缺失的区域可查看热力图，比较工具只接受完整样本。 |

对两个版本生成的存档分别运行 `scanSave`，选择同一种子、维度、起点和边长，把结果目录分别命名为 `reports/old`、`reports/new`。下面排除边缘 2 个区块的统计起点，输出两个版本的矿量、岩石体积和最近矿点距离分位数。

```powershell
.\gradlew.bat :worldgen-tools:compareWorldgen '--args=reports/comparison 2 reports/old reports/new --mineral=Chromite'
```

## 按种子预览

```powershell
.\gradlew.bat :worldgen-tools:sampleWorldgen '--args=73 reports/layers 0 0 512 -35 --mineral=Chromite'
.\gradlew.bat :worldgen-tools:sampleWorldgen '--args=73 reports/legacy 0 0 512 -35 --legacy'
.\gradlew.bat :worldgen-tools:surveyWorldgen '--args=73 reports/survey 0 0 32 143'
.\gradlew.bat :worldgen-tools:checkWorldgenDistribution '--args=reports/check Chromite=512 reports/survey'
```

`sampleWorldgen` 的参数为种子、输出、中心方块 X/Z、边长和切片 Y，输出水平及两个垂直剖面。旧版对照读取 `9b64bc17d` 的原始岩层顺序和矿物标记，默认假设岩石延续到世界顶。已有覆盖整个切片的存档扫描时可在 `--legacy` 后加 `--heights=reports/chromite/heights.csv`，按实际最高非空区段截断。

`surveyWorldgen` 使用相同的矿源、岩层和选点规则，在指定区块区域和地表高度的连续岩体中计算矿量与距离。洞穴、海洋和玩家改造留给存档扫描统计。`checkWorldgenDistribution` 可接受多个种子或扫描样本，检查普通矿的第 99 百分位距离，阈值单位为方块。
