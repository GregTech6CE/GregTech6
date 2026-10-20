# 保险箱与衣柜源链核查

第四十六批，2026-10-20，基线 `36833296c`。原版来源为 `C:/Dev/gregtech6-master/gregtech6-master/src/main/java`；当前来源是共享维护代码及两版本实际注册／配方入口。本批记录差异，不批量修改玩法。

## 保险箱

### ALIGN-140：覆盖板操作未检查宿主锁权限

原版 `TileEntityBase06Covers:107,141` 在普通和工具覆盖板入口先调用 `allowInteraction`。机械保险箱 `MultiTileEntitySafeMechanical:76–81` 在此认主，并使用父类 UUID 门控；钥匙箱 `MultiTileEntitySafeKeyLocked:73–75` 则要求开锁。

当前 `ComponentCoverEvents:43–65` 以最高优先级先于方块交互执行。Safe 实体由 `ComponentCoverHostMixin`／`ComponentCoverFallback` 提供宿主；组件、工具和普通面板分支只检查 `mayBuild`／`mayInteract`，没有进入 `SafeBlockEntity.canOpen`／`claimAndOpen`。有普通建造权限的玩家因此能操作别人的机械箱或闭锁钥匙箱上的覆盖板，未认主箱的覆盖板交互也不会按原版认主。GUI、挖掘与扳手已有的锁检查无法补上提前消费的事件。

这没有推翻世界保护检查，也不代表库存内容可被自动盗取：保险箱未暴露外部物品能力。

### ALIGN-141：普通非正面首次右键不认主

原版 `TileEntityBase04MultiTileEntities:148–150` 先调用 `allowRightclick`，经 Root 委派至上述机械箱 `allowInteraction`，随后才检查阻挡和具体 GUI。`MultiTileEntitySafe:76–82` 限制的是正面打开 GUI，认主已先发生。

当前 `SafeBlock:121–123` 先将非正面点击返回 PASS，再调用 `claimAndOpen`。现有 Forge `SafeAccessTests:71` 甚至断言背面不认主；它固定了与原版相反的合同，测试存在不能证明原版对齐。修订认主时机不应同时开放背面 GUI。

### ALIGN-142：遗漏原版邻块阻挡查询

原版上述普通右键以及 `TileEntityBase06Covers:142` 工具路径调用 `checkObstruction`。它排除 FakePlayer／无效面，再查询 `WD.obstructed:127–149`；默认启用，按门／活板门／梯子、邻实体快速接口和邻块中央碰撞范围判断。

当前保险箱 GUI 分支只有正面与锁判断；`ToolInteractions.canApply` 只有方向、世界权限和宿主工具权限，没有邻块阻挡查询。水平正面邻接下半砖、点击露出的上半面是可能体现差异的场景，尚未运行。只对已追踪的保险箱路径确认，不能据此断言所有方块都遗漏。

### 扩展 ALIGN-139：保险箱正面同样应禁贴板

原版 `MultiTileEntitySafe:107` 与四格抽屉一样排除 facing 面。当前通用安装链缺少该宿主排除，普通装饰板可进入正面。它与 ALIGN-140 的锁权限、ALIGN-142 的邻块阻挡是不同条件；也不同于大容量存储允许贴板后阻止取放的规则。

## 衣柜

### ALIGN-143：缺少充电衣柜家族

原版 `Loader_MultiTileEntities:138–139,186–245` 对 60 种材料分别注册普通 `7300+aID` 和充电 `7500+aID` 衣柜。`MultiTileEntityLockerCharging:42–54` 向四槽装备注入能源，接收有效方向与包范围另有明确合同。

当前 core `GTStorageMetals` 有 60 个规格；Forge `GTStorage:127–135`、Neo `GTStorageContainers:26–34` 及共享实体绑定都只生成普通家族，`LockerBlockEntity` 也没有能源注入。缺的是另一家族及其配方／充电行为，不能继续报告普通材料衣柜未注册。

### ALIGN-144：更衣面与装备门控不同

原版 `MultiTileEntityLocker:58–77` 只允许正面更衣；按槽校验衣柜中的装备能否穿戴，玩家穿着 BTRS Backpack／Thaumpack 时跳过该槽，仅实际交换后同步并播放 click。

当前 `LockerBlock:109–119` 两版本都未比较点击面和 FACING，`LockerBlockEntity:30–44` 无条件交换四槽。背面／侧面也能更衣，未保留装备校验与可选背包保护。当前无自动化入口会遮蔽部分非法装备例子，因此不声称普通生存已能随意塞入任何物品。枚举顺序不同本身不是装备错槽。

### ALIGN-145：普通衣柜缺少外部库存与变更通知

原版 `MultiTileEntityLocker:49–55,82–89` 提供四槽、全侧访问、按装备槽过滤插入、允许抽出，并在库存改变后通知六侧接收者；四种运行状态都按是否含物品返回。

当前实体只保存私有 armor 数组，未提供 Container／物品能力；Neo 的实际能力登记也没有衣柜。交换只调用 `setChanged`，不通知相邻库存。通用覆盖板 Mixin 不会把私有数组变成能力；`MachineControl.find`／`CoverProgress` 也没有对应状态来源。因此自动化和相应检测读数均缺失，后者作为同项影响，不重复增加控制问题编号。

### ALIGN-146：旧混料衣柜配方仍与正确配方并存

原版注册要求箱子、杆、螺丝与机器外壳都用该衣柜材料。当前 `OriginalHandCraftingRows:650–662` 却从 `GTStorageMetals.ALL.get(1)` 选择铋箱，配钢杆／钢螺丝／钢外壳，产出默认不锈钢 `locker`。

这条记录经 `register:757–783` 生成 `gt6ce:hand/storage/locker`，Forge 的 `RuntimeRecipeLifecycle → HandToolCraftingRecipeInjector` 和 Neo 的 `ManualToolRecipePack` 均实际装载该表。`UtilityCraftingRows:42–48` 的正确同材料配方又经 `MultiblockRecipePack:131–151` 生成独立 `gt6ce:survival_repairs/locker`，并不会覆盖旧 ID。这是错误额外路径，不能描述成所有衣柜无配方或方块重复注册；未运行 RecipeManager 验收。

## 已有对应与未验证范围

保险箱已有机械认主的主要入口、十种钥匙绑定／复制／开关、15 槽、前脸 GUI、距离／锁变化失效、槽索引和 owner/key/open 保存、旧 27 槽档余料回填。原版保险箱拒绝外部自动化，当前无物品能力与之对应。

普通衣柜 60 材料、BlockItem、实体绑定和四装备槽保存均有对应。保险箱和衣柜原版默认 `keepSlot=false`，拆除单独掉出内容物；当前同样掉出并清空，不能把未整件保留库存 NBT 误报为缺陷。覆盖板自己的保存与整箱库存不是同一合同。

原版钥匙接口接受任意实现宿主；当前钥匙主要针对 Safe。原版快照只有一个内置锁实现，扩展 API 的收窄留给 API 专项，不把十种钥匙再列为无功能。衣柜声效类型差异留给声音核查，本批不另计编号。

没有验证双玩家权限、实际事件／Mixin、全部材料画面、爆炸掉落概率、旧 CE 世界完整重启或原始 1.7.10 世界导入。源码推导与历史测试均不能替代这些验收。

## 可读性及证据

展开 SafeLockRules、OriginalStorageTooltipData、两份衣柜源码和 Neo 的 GTSafes／SafeClientSetup；纠正钥匙 ID／注册归属及地牢已使用钥匙箱的过时说明。另展开 Neo 的 StorageContainerClientSetup、EngineClientSetup、GTRelaysFilters。十二份源码共十七份 core／两版投影的 Java 词法和字面量与基线一致，Neo 维护注释体先显式展开；未改玩法、注册顺序或保存格式。

[来源和比较回执](verification/safe-locker-audit-20261020.json)保存 23 份原版和 49 份当前文件哈希。本批没有运行 Gradle、编译、新测试、游戏、重启或构建；此前用户请求的双包交付基于 `36833296c`，单独保留构建回执。全面审计继续。
