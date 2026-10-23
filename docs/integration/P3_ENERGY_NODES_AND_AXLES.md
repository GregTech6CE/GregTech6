# P3 电池盒、能源节点、库存与传动轴

2026-10-01。本次按三批共享状态/库存、原生能源设备、传动轴集中整合。采用 saltnya 已有完整节点行为及前批共享规格；沿用 D005 已记录的统一能源身份、brokestar 包桥接及既有材料/流体边界，不引入第二套材料、库存或能量单位。来源、哈希、保留授权记录见 core/provenance/energy-node-state-source-integration.json、native-energy-node-source-integration.json 和 native-axle-source-integration.json。原始作者与历史引用保持原样；此本地整合没有对外发布授权结论。

BatteryBoxState 保留原物品电量与内部工作缓冲两个存储层、20 tick 四档物品充放电频率、单物品包规则调用、接收余量、0..15 模式、提供者计数、buffer display 和原容量截断。两版本 BatteryBoxEnergy 仅负责实际 IItemEnergy/ItemStack 回调、短时世界上下文及原 gt.buffer/gt.battery_stopped/gt.battery_mode NBT 读写，try/finally 恢复上下文。TransformerControlState 共享模式、两个方向的 possible/emitted 和64位活动历史，平台保留原控制接口及 gt.transformer_* 字段。原末包/耗损行为没有改为无损承诺。

原 HopperContainerMenu 的17种精确槽位坐标/玩家背包高度共享 InventorySlotLayout，Forge 原菜单调用同一布局，Neo 接入17个原 hopper_N MenuType、实际 handler 菜单、shift-click 和原屏幕/18份共享原图。原未知槽数 fallback 行为保持；实际本批电池盒为4槽和16槽。没有为未移植的漏斗放置空方块，本菜单也将供后续原库存设备使用。

Neo 原生注册完整70个能源目录设备：10电动机/电发电机、9电变压器、4 FE 转换设备、5蒸汽轮机、2太阳能板、20小/大电池盒、10加热器/冷却器、10双极磁铁。使用原936行能源实体的完整状态/物品槽、转子与磨损、太阳/蒸汽、正负极磁场、包注入输出、爆压、覆盖物和控制逻辑，薄适配 provider-aware NBT/空物品存档、原生 sided FE/item/fluid capability 与客户端覆盖物/材质着色。保持 gregtech 路径及旧键。原 energy_node BE 支持集合中的5个保留电池路径也接入，以保留原实体加载后转换入口；只有代码支持，没有旧存档实际验证。核裂变反应堆工厂及核燃料系统不在本次节点目录内，后续继续整合。

AxleCatalog 保留13种原材料与各4规格，共52传动轴，Forge 与 Neo 共用。AxleConnectionRules 按 down/up/north/south/west/east 连接位保留至多两个同轴面的规则。Neo 原生轴实体保留有符号 RU、转速/功率上限、递归去重、原零额定线损、过载与运行旋转、区块卸载能源图清理；原动态模型和动画 texture metadata 原字节搬入共享资源，水浸/碰撞/双向接线和邻接粗细查询接原模块。13个可装齿轮箱与13个旋转变压器是下一批依赖，完整手动/电动工具和这些设备的制作/生存取得也尚待整合。

三批集中 :neoforge:compileJava :compileJava，首轮27秒因 Neo 爆炸 SoundEvent 的 Holder 引用差异失败，取 value() 修复后27秒通过，core/Neo/Forge Java 都成功。之后核对原 BE 支持集合补齐5个保留电池路径，最后单独 Neo 编译通过，耗时见 verification/p3-energy-nodes-axles-compile.json。回执保留各轮日志 SHA；没有为本批启动游戏、测试实际充放电/转换/过载/轴网、打开界面或世界独立重载，也没有新 jar 验包。上一轮线缆专服通过不能覆盖这批新工厂。仍未完成。
