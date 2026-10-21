# 物流直接存储的身份、能力与客户端入口核查

2026-10-21，第八十七批，基线 `7fd6111bc`。继续检查原版 6200+ 金属物流大容量存储和 32072 物流桶。新增 ALIGN-285、286；已有自动下送修复继续有效。本批只展开大容量存储实体的压行方法，没有修玩法。正式记录共 286 条，包含已修、部分修复和保留政策，不是未修复问题数。

原只读根为 `C:/Dev/gregtech6-master/gregtech6-master/src/main/java`。12 份原版、44 份当前来源、54 份共享平台视图，以及本机两工件五条目／两份只读 javap 的身份见[来源与验证回执](verification/logistics-direct-storage-audit-20261021.json)。作者 GregTech-6 Team／Gregorius Techneticies 的版权和 LGPL 头随来源摘要记录，原目录未改。

## ALIGN-285：物流桶覆盖板保存完整，但客户端更新入口缺失

原继承链为 `MultiTileEntityBarrelLogistics → TileEntityBase08Barrel → TileEntityBase07Paintable → TileEntityBase06Covers`。安装覆盖板调用 `updateClientData`，父类完整包带覆盖板 ID／meta，后续包可带视觉值，接收端 `receiveDataCovers` 恢复它们。不能因为具体物流桶类没有重新声明这些方法，就认定原版不支持同步。

当前 `LogisticsTankBlockEntity.saveAdditional/load` 确实保存和恢复物流覆盖板；这条世界保存链没有缺失。问题在另一条客户端链：

- 父类 `TankBlockEntity.getUpdateTag` 手动写温度、桶模式、罐及通用组件覆盖板，没有调用虚拟 `saveAdditional`，也没有写 `logisticsCovers`。它调用的原生 `BlockEntity.getUpdateTag` 只创建空 tag。
- 该父子类均没有覆盖 `getUpdatePacket`，两版原生默认返回 null。`LogisticsCovers.changed` 调用 `sendBlockUpdated`，不会自动把世界保存内容变成实体更新包。
- 备用 `ComponentCoverFallback` 只跟踪 `getCover` 的声明类为原生 `BlockEntity` 的宿主。桶已经自己覆盖 `getCover`，因此 `uses/track` 不接受物流桶；备用包发送和区块订阅也只遍历其活动集合，不能补上此路径。
- 配置中的 `ComponentCoverHostMixin` 在 `saveWithoutMetadata` 返回时追加数据，没有拦截 `getUpdateTag`。它的读取钩子能处理已经收到的数据，但不能生成缺失的发送内容。

这是正常初次区块数据／安装后更新缺少物流覆盖板消费者的源码结论，没有进行双端发包或截图复现。它不等于世界保存或掉落物丢失，也不应扩大为所有物流宿主都不能同步。物流大容量存储的父类 `getUpdateTag` 调用虚拟 `saveAdditional`，子类追加物流数据；它还有实际 `ClientboundBlockEntityDataPacket`，并具备备用跟踪资格，两者必须分开判断。

## ALIGN-286：专用物流桶实体漏注册覆盖板渲染器

原 `TileEntityBase06Covers` 的最终渲染方法会在本体外增加覆盖板 pass，并调用表面／支架／附件贴图；物流桶沿上述继承链使用该入口。

当前 `TankCoverRenderer` 已经写了 `LogisticsCoverHost → LogisticsCoverRenderer.renderFaces` 分支。然而 Forge 的 `GregTechClient` 只将它注册到普通 `GTBlockEntities.TANK`，Neo 的 `FluidTransportClientSetup` 也只注册普通 `FluidTransportRegistries.TANK`。物流桶实际构造使用独立 `GTBlockEntities.LOGISTICS_TANK`，Forge 普通桶类型还显式排除了物流桶方块。

本轮扫描 1250 份当前共享／Forge／Neo 生产 Java 的 `registerBlockEntityRenderer` 调用，没有物流桶专用类型注册。两版本机 `BlockEntityRenderDispatcher.getRenderer` 均以 `blockEntity.getType()` 查 map，不按实体 Java 父类回退。备用世界渲染器只遍历备用跟踪集合，也受 ALIGN-285 中的宿主资格限制。

因此已有 `TankCoverRenderer` 分支还不能进入物流桶的实际绘制路径。这与“纹理资源不存在”、普通桶外壳模型和 ALIGN-261 的 CPU 前景自亮是不同问题。此处只讨论当前内置消费者，不排除附属模组另行注册渲染器；没有客户端画面验收。

## 已有的注册、过滤和工具合同

两端均从 `GTStorageMetals.ALL` 注册普通／物流金属存储。Forge 入口为 `MachineBlockBootstrap → GTStorage.registerAll`，Neo 为 `GregTechNeoForge → StorageRegistries.register`；物流大容量存储使用自己的实体类型。物流桶由 Forge `TankDefinitions` 和 Neo `GTLogisticsTank.initialize` 注册，同样有自己的实体、服务端 ticker 和流体能力。Neo 的两个物品能力注册、物流桶流体能力注册都有实际调用，null 方向查询也有接收入口，不只是声明接口。

| 状态 | 原版来源 | 当前消费者 |
| --- | --- | --- |
| 从未选择物品的物流大容量存储 | `slotHas(1)` 为假，优先级 1 | 模板为空，`itemStoragePriority=1` |
| 记住物品但整件数量为零 | `slotHas` 判断非 null，不判断数量，优先级 2 | 模板由计数之外的独立身份保留，优先级 2；显式重置条件另行处理 |
| 从未灌液的物流桶 | `mFluid==null`，优先级 1 | `getFluidLong` 身份为空，优先级 1 |
| 抽空但保留流体种类 | 原 `setPreventDraining(true)` 保留身份，优先级 2 | `setKeepFilterOnEmpty(true)`，独立液量为零但身份副本仍非空，优先级 2 |

当前原生罐首次灌液将身份副本数量设为 1，实际液量另存 long；抽空不会使该身份随实际量归零而变成原生 EMPTY。普通 `getFluid()` 与身份查询 `getFluidLong()` 不能混用。原 `preventDraining` 的名字在当前 API 中另指禁止抽取，物流桶没有误用它。

`LogisticsItemRouter/LogisticsFluidRouter` 实际收集直接存储端，使用无方向能力并按优先级进入对应列表。原核心与当前代码都先收集直接存储，再检查覆盖板停止位，因此这不是新发现的停止位绕过。大容量存储的严格过滤方法返回 null，也不代表缺少半过滤预留：物品路由另读取模板并调用 `MassStorageMaterialForms.reserves`。

原版声明的十组形态和当前窄家族定义，在 40 种命名形态的家族归属上对应；1600 个有序形态对的家族相同／不同投影一致。此项只比较已声明家族，未执行材料注册、外部 OreDict／标签、全部材质、NBT／components 或实际转换。当前罐的进出液仍分别使用 Forge `isFluidEqual`、Neo `isSameFluidSameComponents`，不能误报它完全没有比较附加数据；原 `FL.equal` 默认也比较 NBT。

活动扳手、剪线钳和螺丝刀已分别消费位 1、4、2；封装时仍允许切换。父类 ticker 接受 `MassStorageBlockEntity` 子类，因此物流存储也执行自动下送、+256 溢出容量与底面输入限制。此前 [ALIGN-137 修复及定向验证](verification/storage-direction-colors-20261020.md) 仍有效；本批仅重查源码，没有重复运行当时场景，也未把 Neo 旧验证冒称当前 Forge 运行通过。

物流大容量存储用 `Materials.Black` 表示外观，但另存制作金属。`BlockHarvestPolicy` 先识别物流子类，读取 `constructionMaterial().getToolQuality()`。原注册同样用制作金属工具等级、黑色外壳和调用者硬度／抗爆；不能由黑色外观误报所有变种丢失工具等级。

## 保存、掉落和仍未覆盖的范围

两种直接存储子类均在父类之后保存／加载物流覆盖板。大容量存储更新 tag 通过虚拟保存包含子类数据；Neo 默认接收路径调用 `loadWithComponents → loadAdditional`。物流桶世界保存也完整，客户端缺口另列 285。

最终掉落的 `CoverDropMixin → CoverDrops.capture` 会提取 `gt.logistics.cover.*`，并合入实际方块掉落数据。玩家破坏还有预先快照；通用机器数据清理不包括桶。物流大容量存储缺少一个同名 `dropComponentCovers` 方法，不能据此判定最终收获必丢附件。完整实际采掘、放回与存档重启仍未运行；撬下样本丢失沿用 ALIGN-258，不能拿收获保存路径代替撬棍路径。

原物流桶 ANY.W 的所有材料替代关系、空桶软锤清记忆 ALIGN-092、共享容量／销毁合同 ALIGN-093–094、物品态流体能力 ALIGN-097、大容量存储钳子／软锤 ALIGN-136 与正面覆盖板门控 ALIGN-138 等仍保持原有待办边界。没有取消过滤记忆，也没有把本批正向核查视为完整物流验收。

## 可读性与验证

`MassStorageBlockEntity.java` 维护输入由 374 行展开为 475 行，拆开模板／模式 getter、提取后的更新、物品能力匿名类和能力失效清理。没有改标识符、import、注释、版本条件、NBT、数量或玩法。Forge／Neo 全部有序执行词法分别为 2577／2354 项，字面量原始位置各 60 项，连同注释和 Stonecutter 指令保持一致。

同步前已提交的五份存储可读性源码另按原回执重新核对，九份执行视图的既有证明仍成立，不计作本批新增改动。两份用户材料文件的原始字节摘要保持。

本批没有 Java 编译、目标初始化、Gradle、测试、游戏、自启动、实际存档重启、构建或推送。原生 javap 只读取类文件，不执行目标逻辑。完整项目核查继续。
