# 可视扫描、书籍地图打印与画布映射续核

2026-10-21，第九十一批，基线 `70f48587a`，已包含上游 PR82／83。本批读取 7 份原版来源、24 份当前来源及其中 40 份共享平台投影；Forge47.4.20／Neo21.1.243 两工件四条目和两次只读 javap 用于默认状态证据。作者／许可证头、摘要和完整排版证明见[回执](verification/visual-document-audit-20261021.json)。没有运行目标类或游戏，不作完整图像、文档或存档验收。

## 新增 ALIGN-291：比较器／中继器图像映射未保留原 powered 目标

原版 `RecipeMapScannerVisuals` 将比较器和中继器物品分别映射到 `Blocks.powered_comparator`、`Blocks.powered_repeater`。扫描时通过映射栈取得目标方块，将其运行 ID 写入画布数据；打印再复制到画布，`CoverTextureCanvas` 将该方块身份和 metadata 传给 `BlockTextureCopied`。这不是仅用于配方展示的表。

当前 `CanvasData.mapped` 对这两个物品分别返回 `Blocks.COMPARATOR.defaultBlockState()`、`Blocks.REPEATER.defaultBlockState()`。两个版本的原生构造默认值均是 `POWERED=false`：Forge 通过实际缓存类的只读 javap 确认，Neo 通过实际来源工件确认。

| 输入物品 | 原版扫描目标字段 | 当前两版记录的默认状态 |
| --- | --- | --- |
| 比较器 | `powered_comparator` | `minecraft:comparator`，`powered=false` |
| 中继器 | `powered_repeater` | `minecraft:repeater`，`powered=false` |

当前 `scanned → fromState` 将状态属性写入 `CanvasRules.Image`；`CanvasRecipes.scan` 写进 U 盘，`print` 写回画布，`state` 重新应用属性。`PanelCoverRenderer` 实际调用 `CanvasCoverRenderer`，后者用此状态获取方块模型。因此已追到图像消费者，未发现后续把这两类图像改成 powered 状态的分支。

本条确认的是来源映射身份／状态差异，不是实际像素对比，也不表示画布应该输出红石。没有运行 1.7.10 引擎、安装画布或逐面截图；现代方向／模式等其它映射仍待核。本批不修正映射。

## 已存在的基础路径与排除的误判

- `BlueprintRecipes.register` 实际为可视扫描仪／打印机挂上画布、蓝图和普通文档的上下文提供者；`GTMaterialDataRecipes.register` 调用它。当前基础书籍／地图／画布路径有消费者，不能沿用旧记录把整套功能列为未接入。蓝图完整数据、穿孔卡及外部模组文档不由此证明完成。
- 普通书籍扫描保留原书并输出写入文件的同种 U 盘，书籍为 512 tick／功率16，地图为64／16；画布本身扫描64／16，普通方块图像512／16。画布打印64／16，保留数据介质，每种黑／青／品红／黄染料16 mB；普通地图同样使用这四种染料。都是源参数与调用链核查，没有实际加工。
- 普通书籍打印的 50 页边界仍存在：1／50 页为3张纸、72 mB黑染料、512 tick；51页为6张纸、144 mB、1024 tick，功率均16。这里只列三项成本分支，不把它们说成三本实际可加工书或完整书目比较。后续彩色装订类有复制数据的路径，所有配方、材料限制与克隆代数另待核。
- `map_id` 沿用原键名。原版 USB 用 short，当前接受任意数字类型后读 int、写 int，并拒绝负 ID；两版分别使用原生地图 NBT／MAP_ID 组件。保留现代较大 ID，不恢复旧截断，也不声称旧世界地图数据已经迁移。
- Forge 书籍数据复制原 NBT；Neo 显式序列化原始／过滤后的标题、作者、代数、页面和 resolved，再显式写回 CUSTOM_DATA 与 WRITTEN_BOOK_CONTENT。自定义名称也有转换。该分支存在，不代表所有畸形 JSON、旧自定义标签、过滤内容及实际客户端显示都已验收；原生标题长度保护保留。
- 命中显式物品映射时先使用映射；未命中且为 BlockItem 时才读取原生 BlockStateTag／BLOCK_STATE 属性及现有 GT 颜色。图像以注册名和属性保存，不恢复运行 ID。空白、未知方块、无效属性的保护也保持当前政策。
- 原版映射表中的成熟作物 metadata 不能直接当作最终输出。实际 `NBT_CANVAS_META` 写的是 `ST.meta_(tScanned)`，不是 `ST.meta_(tMapping)`。因此本批没有因小麦等表值为7、当前默认生长值为0而直接登记回退；其它状态须继续追原始输入和渲染语义。
- 画布注册与基础染色表已存在，展示行仍是 fake，不参加机器查找。完整染料集合、全部映射、旧原生图标与现代模型的图像等价、物品栏预览、撬下／保存／同步还需继续。

## 源码整理与验证边界

展开 `VisualDocumentData`、`VisualDocumentRecipes`、`CanvasRecipes`、`CanvasData`，共397→564维护行。分开书籍字段读写、Neo组件转换、输入选择、染料构造、图像状态与配方参数；保留清楚的数组分组。只修正一条过强的“原版映射”说明，没有改字符串、数据键、参数、注册顺序或玩法。

四份共享源的八份完整代码投影分别为1095／1879、1015／1017、1275／1289、1512／1544项，全部有序代码标记保持；原始字面量位置分别为36／63、29／29、54／54、6／5项，另核其它注释、Stonecutter条件与用户 Wood／Compound 材料表摘要。

本批未编译、Gradle、测试、游戏、构建或推送；两次 javap 只是读已有类。正式记录至291条，不是291个未修复问题。穿孔卡、蓝图／自动合成、完整书籍与后续其它系统均继续，全面目标尚未完成。
