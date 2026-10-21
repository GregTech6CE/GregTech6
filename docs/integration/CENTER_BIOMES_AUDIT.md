# 中心群系布局、树木委托与状态映射核查

2026-10-22，第一百零九批，基线 `de8c12896`。新增 ALIGN-361–363，分别为树高随机量、丛林附属物和新树叶距离初始化。3 份原版 Java、13 份当前生产 Java／20 份平台视图、15 段具体引用及原生证据见[回执](verification/center-biomes-audit-20261022.json)。归档整个文件供追溯，不代表其每个功能都验收；原树只读，作者／版权和 LGPL 声明保留。

## 三项确认差异

| 编号 | 原版实际路径 | 当前实际路径 |
| --- | --- | --- |
| ALIGN-361 | 中心布局给 WorldGenTrees 传入最小树高，旧生成器内部再加一次 `nextInt(3)`。普通树调用的理论树干高度为 4–8，丛林树为 9–13，是否成功生长另受空间／土壤门控。 | SourceCenterBiomes 仍算 4–6／9–11，但 NativeOriginWorld.tree 直接当作实际高度使用，少了内部随机量，也改变了随机数消费顺序。范围是源码算术界限，不是自然世界统计。 |
| ALIGN-362 | 四处丛林树调用将 vinesGrow 设为 true；旧生成器有树干／树叶藤蔓分支，并在满足高度及随机条件时生成可可。 | OriginWorld.tree 接口没有该标志，手写原生实现只放原木和树叶，缺少这组附属物。原版可可有条件，不是每棵都有；四处调用的木材／树叶 metadata 本就相同，不能把未传单独叶种也误报为本路径少了树种。 |
| ALIGN-363 | 原版委托正常树木生成器；本路径叶 metadata 为 0–3，旧随机凋落还要检查另一个衰减标志位。 | 当前直接以两版 LeavesBlock 默认状态放叶，即 DISTANCE=7、PERSISTENT=false，没有初始化距离或调用现代 TreeFeature 的更新步骤。该状态已满足随机凋落条件。默认叶属性不请求生成后处理，WorldGenRegion／LevelChunk 的相应路径不能自动弥补这项缺口。后续邻居更新或其它模组可能补算，故不宣称每片叶必定消失；实际凋落时间与世界结果待实测。 |

这些差异本批只记录，没有借排版修改树木玩法。后续不宜简单设永久树叶绕过正常采伐后的凋落，应保留正常叶距／更新合同。

## 原生证据来源

旧版结论使用 [Mojang 官方 1.7.10 元数据](https://piston-meta.mojang.com/v1/packages/ed5d8789ed29872ea2ef1c348302b0c55e3f3468/1.7.10.json) 指向的客户端 JAR，核官方 SHA-1 和大小后，只读反汇编 asq／alt 两类。使用 [Forge 官方 joined.srg](https://raw.githubusercontent.com/MinecraftForge/FML/1.7.10/conf/joined.srg) 与 [fields.csv](https://raw.githubusercontent.com/MinecraftForge/FML/1.7.10/conf/fields.csv) 确认类、树高、藤蔓及方块字段，不采用第三方反编译镜像。

另核 Forge 官方 [WorldGenTrees 补丁](https://raw.githubusercontent.com/MinecraftForge/MinecraftForge/1.7.10/patches/minecraft/net/minecraft/world/gen/feature/WorldGenTrees.java.patch) 和 [BlockLeaves 补丁](https://raw.githubusercontent.com/MinecraftForge/MinecraftForge/1.7.10/patches/minecraft/net/minecraft/block/BlockLeaves.java.patch)。土壤／叶判定等 Forge 扩展与未变的高度、藤蔓及衰减门控分开处理。

当前 Forge 47.4.20 的六类实际字节码、NeoForge 21.1.243 的六个源码条目与当前构建配置对应；工件、条目和反汇编均留摘要。Forge 默认后处理谓词还追到常量池 bootstrap 5 与返回 false 的 lambda，避免只从方法名推断。未启动旧版客户端、现代 Minecraft 或模组。

## 完整布局与已有状态映射

完整原版 generate 方法体经过列明平台适配后，与本轮开始时的当前方法体 3912 个有序代码 token 一致。适配包括 12 处群系写入、两处旧河水全局开关、10 处地基起点、10 处清空高度、12 个树木委托和三个散落物委托，以及现代粗泥土可用性选择。这种方法体对应不证明原生委托等价，正是上述树木差异需要继续追的原因。

本轮去掉恒真条件后，剩余 3669 个方法体 token 也与按同样受控删除处理的来源一致。CS 中水域名称及九个原版群系补充名归一化后，当前表的 54 项全部对应。

已有明确状态转换保留：红沙、灰化土、雪层数量、九种花、石材形态和荧光睡莲颜色均有对应分支；本路径用到的三种砂已指向具名磁铁矿砂身份。建筑玻璃／泡沫等颜色通过固定颜色方块族转换，并复制共有状态属性。这里证明映射分支存在及当前布局请求范围，不声称已做真实渲染或完整注册表验收。

## 源码整理与有界前后对照

四源 431→461 行：SourceCenterBiomes、OriginBiomeNames、GTWorldgenBiomes、BlackSandDefinitions。展开中心布局嵌套分支，整理水域名和三砂定义；纠正 RIVER 集合误称包含海洋的注释。

删除一个 `if (T)` 包装及不可达 else：T 是 OriginSupport 的编译期 true 常量，实际始终使用现代粗泥土。删除文本随回执保存，不假称删除前后完整文件 token 相同。其它三源四份完整执行视图、中心布局一份受控删除后的剩余视图，其有序代码、字面量位置、其余注释和版本指令保持；公共签名、当前实际生成路径与两份用户材料文件摘要不变。

约 0.99 秒只编译／执行四个实际 core 源、仅改两个类／构造器名称的基线副本与记录器。在 144 个中心区块上，前后返回值、计数、64 位有序请求指纹、完整树木请求列表、群系和出生点一致；区域外一个输入正确拒绝。记录 12061584 次有效块写入请求、176 次树木委托、778 次散落物委托；九个群系分布和全部 54 名称大小写／分隔符别名检查通过。

上述写入含重复位置，是记录器请求而非真实块数；树木委托不执行原生生长，也不消费随机数。有限指纹对照不是通用等价证明，需与受控代码比对合看。

未运行 Gradle、整模组双版编译、GameTest、游戏、渲染、存档重启、构建或推送。默认 Nexus 全开、#66 CFG 后续、#71–#74 跳过和 #68／#69 合并核对保持。累计 363 条含已修及保留策略，不是未修数或完成率；其它世界生成及全系统核对／源码标准化继续，全面 goal 保持 active。
