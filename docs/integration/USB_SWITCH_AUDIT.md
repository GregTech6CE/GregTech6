# USB／HDD 交换器的选择、数据与通知核查

2026-10-21，第八十九批，基线 `0eda620fb`。新增 ALIGN-289，扩展既有 132／172 的宿主；只整理源码和说明，没有修改玩法。正式记录 289 条包含已修、部分修复和保留政策，不是未修复问题数。

原版只读根为 `C:/Dev/gregtech6-master/gregtech6-master/src/main/java`。作者 Gregorius Techneticies／GregTech-6 Team 的版权和 LGPL 头随 12 份来源记录，当前 22 份输入、34 份共享视图与有限条件见[回执](verification/usb-switch-audit-20261021.json)。本批不是打印机、分子扫描仪、复制机及所有数据提供者的完整验收。

## ALIGN-289：移除非选择面板也重置所选文件

原 USB 19000 和 HDD 19001 共用 `TileEntityBase08DataSwitch.mMode`。选择面板父类 `AbstractCoverAttachmentSelector.onCoverRemove` 才调用 `setStateMode(0)`。普通红石发射面板 `CoverRedstoneEmitter → AbstractCoverAttachment → AbstractCoverDefault` 没有覆盖该方法，最终是空操作，不改变交换器的槽位。

当前 `UsbSwitchBlockEntity.removeCover` 在释放对应面后，只要 `PanelCover.of(cover)!=null` 就调用 `setMode(0)`。枚举中包含红石发射、导体、控制等非选择面板。后面的 `PanelCoverRuntime.removed` 虽然正确区分 `panel.selector()`，却不能撤销宿主已经执行的重置。

具体有效条件：USB 或 HDD 交换器的手动选择面板已选第 5 格，另一面装有红石发射面板；完成加载恢复，不装红石／定时等其他模式写入者。移除发射面板后，原版保持 5，当前变为 0。`readUsbData` 随后直接用 mode 选择 U 盘或 HDD 文件，因此这不只是菜单数字改变。`afterTick` 本身不重新施加手动选择值；后续其他选择器事件可以再次选槽，不能把这种修正事件当作本次移除正确。

两种交换器、16 个正常模式、移除手动选择／红石发射两类面板，共 64 个有限源码条件中有 30 个结果不同。只是控制流推导，没有玩家世界交互。装饰覆盖板、未知物品或全部覆盖板并不都进入这个条件。

## ALIGN-172 扩展：换槽通知没有置相邻机器库存变更标志

原 `TileEntityBase08DataSwitch.setStateMode` 在模式改变时遍历六面相邻库存。只有其中有 USB 线缆，且未限定方向或方向匹配时，才调用该库存 `markDirty`。GT 的 `TileEntityBase05Inventories.markDirty → updateInventory` 实际设置 `mInventoryChanged`。

当前交换器确实对已加载邻格调用普通 `level.neighborChanged`，不是完全没有通知。然而普通 `BasicMachineBlock.neighborChanged` 的实体接收者只调用 `adjacentEnergySourcesChanged`；该方法只设置能源来源标志，不设置配方库存变更标志。原箱子广播的是专用接口，USB 原发送者直接调用相邻库存 markDirty：两种发送机制不同，但都要继续追实际库存标志消费者，不能用普通方块更新代替。

保留恢复路径的判断：当前能源足够且没有进行中配方时，基础机仍能自行调用 `checkRecipe`；工作可能性和空闲轮询另有 12 tick 门槛。因此不能宣称换槽后永久卡住，也不能把所有配方切换都说成固定延迟 12 tick。该项只确认原通知合同未完整对应，见[库存通知专题](INVENTORY_NOTIFICATION_AUDIT.md)。

## ALIGN-132 扩展：持有 U 盘复制没有原点击声

原基类处理持有 U 盘的点击，在复制已有文件或清除文件后调用 `playClick`。当前 `UsbSwitchBlock → writeFromHeldStick → writeUsbData` 有实际数据写入，却没有对应声音调用。通用面板交互对普通 U 盘返回 PASS，不会自动为复制补声；覆盖板安装／拆除的声音是另一条路径。

本项不扩成整个 GUI 或所有数据操作无声。正常写入结果没有被本批改变，声音差异沿用[反馈合同](CONSUMABLE_FEEDBACK_AUDIT.md)的 132 编号。

## 已有的媒体、能力与保存

- USB 实体有 16 个单件 U 盘槽；HDD 实体有一个单件硬盘槽，mode 选择其中 16 个文件。菜单使用同一物品准入规则，已保存的 mode、库存及六面覆盖板都有读取入口。
- 原层级再注册将 stick／drive／cable 的高一级关联到低一级；当前内置 1–4 级范围的 `mediumTier>=requestedTier` 与正常 `fileTier<=requestedTier` 有对应。100 个读条件、20 个写条件只核声明与公式，不是活 OreDict／标签或实际 NBT 验收。
- `UsbDataMedia` 读回复制不透明文件，写入只修改选中文件／级别，清空只删除相应键；HDD 其他文件继续保留。Neo `CUSTOM_DATA` 是脱离原栈的副本，当前写路径有显式 store，不能仅看到 copyTag 就判定没有保存。
- 两种方块、共用实体类型和 Neo 物品能力都有实际注册；相邻线缆按反向面调用 `UsbDataPort`，可固定 `gt.usb.dir`，并拒绝未加载目标。Forge 基础机和 Neo `MachineContextRecipes` 都有数据端口消费者，不能把另一个版本的类路径不同当作缺少实现。
- 实体更新 tag 调用 `saveWithoutMetadata`，有实际更新包；mode 和 handler 变化会请求同步。此为源码链，不是实际网络、断线恢复或独立存档重启验证。
- 普通块入口先调用 `PanelCoverInteraction`，其建造／世界交互权限失败会返回 FAIL，块入口按非 PASS 提前返回。当前没有因块体中不直接出现 mayBuild 就误报权限缺失；完整所有菜单权限仍需独立核查。

外部媒体注册、原保留的 5–9 级扩展、负数文件级别、空 compound 与 null 的细节、完整打印／复制／自动合成、跨端 UI 和旧档仍在待核范围。现代模式钳制、文件复制和显式组件提交继续保留，不恢复越界访问或可变引用副作用。

## 可读性与验证边界

`UsbDataRules`、`UsbDataMedia`、`UsbSwitchBlockEntity` 三份维护源码合计 395→579 行，展开 ID 解析、组件读写、控制提供者、能力及保存方法。核心 tier 方法的 id／n 改为 registryPath／tierDigit；实体移除面板的旧说明改为准确描述当前过宽重置。另只修正 `GTMaterialDataRecipes` 的空闲查找说明，将调度职责指向原生机器，不改变配方代码。

四源七份完整执行投影为 277、886／1038、2034／1888、3762／3859 项；在仅恢复所列局部名后全部有序词法保持。未归一的字面量位置、其它注释、版本条件和两份用户材料表摘要分别核对。没有改媒体 ID、物品、NBT、组件键、注册顺序或运行行为。

没有 Java 编译、目标初始化、Gradle、测试、游戏、实际复制／拆板、存档重启、构建或推送。旧 P3 启动回执仍只证明当时启动范围；真实机器生存链及全面项目核查继续。
