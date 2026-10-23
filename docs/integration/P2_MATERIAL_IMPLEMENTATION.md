# P2 共享材料与反应领域：已实施范围

日期：2026-09-30。本次是第一条玩法链的材料/规则前置工作；P2 的真实生存链、客户端、专服、存盘重启退出条件仍未达成。

## 实际迁移与来源

saltnya 导入快照 `import/saltnya-snapshot`（`41561782`）的39个真实类移到 Java17 core。34个文件保持原字节，5个文件只改必要平台边界。完整 GTMaterial/GTMaterialRegistry、所有生成与补充材料、组成/处理链接、MaterialPrefix/PrefixRegistry、CrucibleMaterialStack/Reactions 使用原包名、对象与注册表，没有第二套运行时材料目录。

最初候选36类经独立编译发现3个真实依赖，补入 `MaterialOreProcessing`、`PortableFluidContainerSpec`、`ReactorRodCatalog`。胶囊/反应棒规格保持原字节，供 MaterialFormCorrections 的既有规则直接调用，没有抄一份材料名称表。原 Forge 路径均移除同名类；core 单一输出由两个平台消费。

`core/provenance/material-extractions.json` 记录每个原路径、core路径、原SHA-256、整合后SHA-256、字节一致性及边界取舍。作者身份使用给定的 saltnya 来源标识，未恢复或捏造作者提交历史。授权仍未决，未添加、替换第三方许可；本地开发验证不通过发布许可门禁。

本批保留 saltnya 的全目录和原反应行为。brokestar 的图验证/热量与 masson 的转换/守恒/指纹思路已比较，见 P2_MATERIAL_DESIGN；本批尚未采纳它们的算法或第二套模型。下一次算法变更应在同一 GTMaterial/long U 上逐项比较原固定样本，再记录选择。

## 平台边界与兼容范围

- GTMaterial 移除 Minecraft Component 方法与 compile-only Nullable；可空 burning target 行为保持。Forge `MaterialPresentation.name(material)` 使用原 `Component.translatable(key,fallback)`，逐个迁移23个材料显示调用；其他同名流体、prefix/menu API 不改。词典原有 Component 对象拼接字符串行为也保持，未混入另一个修复。
- ModData 的 id/name/prefix、MODS 映射、loaded、equals/hashCode 和 owns(String) 留 core。`bindPresence` 要求平台在任何 ModReferences/材料 holder 之前绑定 loader查询，未绑定/晚绑定会明确失败。Forge GregTech 构造入口先绑定原 `ModList.isLoaded`；`ModOwnership` 保留 Item/Block 的 Forge registry 查询。NeoForge 接入必须完成相同早期绑定。
- GTMaterialRegistry 的 logger 改为纯 Java sink，默认 System.Logger（java.base），Forge 提供 LOGGER 回调。domain postInit 的燃料、NEVER_FURNACE、pulverization 规则留 core；原末尾两条 VanillaUnificationLoader/VanillaCompositionLoader 调用按原顺序留在 Forge Loader_MaterialPost 的 domain postInit 后。
- CrucibleMaterialStack 的原 saveList/loadList 方法体移至 Forge `CrucibleContentsNbt`，所有实际调用与原GameTest调用已迁移；材料对象/amount不另包DTO。`id` int、`amount` long、过滤行为保持；BE其余NBT字段和模具名称字符串 schema 未改。

移动纯类保持非 Minecraft 调用签名与领域行为。涉及移走的 `GTMaterial.getDisplayName()`、`ModData.owns(Item/Block)`、`CrucibleMaterialStack.saveList/loadList` 的外部二进制调用需要迁移 helper，不承诺未审计第三方的二进制兼容。旧世界兼容尚未运行验证，保留字段不是已经通过旧存档升级的证据。

## 完整目录对比与数值验证

先以原始未修改Git源码，在无游戏classpath下编译整个领域闭包。只有显示、NBT、日志、游戏注册、vanilla item绑定使用惰性或空边界stub；ModList显式模拟精确 minecraft/gregtech 为已加载。所有材料定义、链接、组成、胶囊/反应棒规则及前缀均为真实原代码。原始 source hash 在复制前核对，不把新目录的输出自动作为金样。

`core/provenance/material-baseline.json` 记录范围、stub限制及固定摘要。`MaterialCatalogSnapshot` 只存在于测试，排序观察每个材料的原始/resolve身份、原子/物理/工具/燃料/处理字段、来源元数据、组成/副产物、名称别名和每个前缀与材料的form/item/tag映射。这个测试输出不供模组运行时使用。

原目录与迁移后的结果完全一致：1,156个材料对象、1,101个唯一正数ID、1,519个名称入口、109个物品前缀、42条显式反应与173条显式/组成反应。

- definitions SHA256：`ad70436da65c6d58e9a9ed26f2819579a7002e0df7ca1f298c9cd385435797a5`。
- domain postInit SHA256：`59a5c25de59260aa8361121870528a091d0a4846729174e4ef3bd4f2fbc35136`。

快照覆盖目录与领域后处理，不包含 Forge worldgen/StoneType 后加flags或vanilla Item绑定效果。原 Forge 调用继续保持这些平台步骤；NeoForge 在注册真实内容前还需接入等价flag/映射过程，不能以这两个摘要宣称世界生成等系统已经一致。

新增 MaterialBehaviorContracts 有6,154条断言：完整摘要、重复init无变更、ID/alias引用身份、组成输入、closed phase、明确presence、铜290/锡500/青铜8610物理值与float位、prefix精确份额、铜锡熔点拒绝与3:1产率/余量/单位守恒、AnnealedCopper仍固态时的原显式合金、原CrucibleSurvivalTests的赤铁矿/碳/方解石还原产率与无flux拒绝、stack合并与copy隔离。预期数值是原代码与原GameTest金样，没有镜像重写反应算法。

特别冻结：U=648648000、L=144外部流体单位、C=273/环境293整数Kelvin。Bronze的compositionDivider=4是无量纲比例，components分别3U和1U；不能把divider再乘U。所有本批热值、ID/名称/alias、NBT单位均保留。

## 已执行与仍待执行

已执行独立 `javac --release17`，原64核心合约及新6,154材料合约通过，`jdeps --multi-release17 -summary` 仅java.base。隔离worktree统一 `:core:check :compileJava` 第二次通过（38s，workers2）；第一次发现5个遗漏材料显示调用并已全部补齐。原有56条deprecated/removal警告仍留，不在本次材料迁移中作无关修复。没有运行游戏或完整jar构建。

可独立复核：

```powershell
./core/verify-core.ps1 -JdkHome 'C:/Program Files/Java/jdk-17.0.4'
./core/verify-material-baseline.ps1 -JdkHome 'C:/Program Files/Java/jdk-17.0.4'
```

第二条从import标签读取原Git blob字节并检查原哈希，再以保留的边界stub重新产生完整摘要；它需要PowerShell7、Git、Java17，不修改来源树，也不运行游戏。stub与probe位于core/src/test/fixtures，不属于发布main输出。

仍待主线：两平台运行时目录加载与无重复类、NeoForge真实内容/机器的共享模型接入、Forge相关真实GameTests、NBT字段往返及真实关服存盘/世界重启、客户端呈现、生存可达链与工具使用、成品jar测试。已有 register失败时可能部分写入、HashSet影响反应tie顺序、composition/long溢出、default Locale命名等行为保持原样并单独留账；不能把本次搬迁解释为已修复这些问题。KubeJS/脚本接口未新增，其平台缝继续baseline，待P3独立审计。
