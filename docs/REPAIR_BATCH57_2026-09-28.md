# 第五十七批：电池盒原版状态外观与直接面板

## 本轮改动

- 新增 BatteryBoxBlock，沿用六个现有普通电池盒注册 ID；只给电池盒增加 charge_state=0/1/2，避免为其它能源节点扩充无用状态。
- 按 GT6 TileEntityBase10EnergyBatBox 的缓冲指示每 20 tick 更新：少于一个输出包为 0，达到 input*300*slotCount 为 1，其余为 2。注意这是工作缓冲状态，不是电池内电量百分比。
- 资源分别使用原版 overlay、overlay_active、overlay_blinking；保留闪烁贴图 mcmeta。每个型号 6 朝向 × 3 状态 × 2 含水状态共 36 种，外壳按材料染色，覆盖层不染色。
- Java spec.texture 与资产生成器统一为 energystorages/battery_electric，消除普通电池盒误指向裸电池纹理目录的问题。
- 直接在电池盒上安装手动/红石/按钮选择器、状态显示、进度/能源检测与显示、红石发射/导通和覆盖板控制器。复用 PanelCoverInteraction/Runtime/Renderer，不另造按钮坐标和控制逻辑。
- 选择器控制输出模式；状态按钮控制启停；检测器输出弱/强红石；配置工具、客户端覆盖板 NBT、拆除掉落和移除选择器复位均接通。
- 空盒也明确声明 EU capacitor 类型，因此能源显示/检测面板可以先贴，再安装电池。
- 未接通物品闸门的 shutter 覆盖板暂时明确拒绝安装，避免显示已安装却没有行为。并不代表全部覆盖板已经还原。
- 面板运行器按需分配，非电池盒节点不分配覆盖板 EnumMap，保持原有其它能源节点行为。

## 生成器

`generate_energy_node_assets.py --battery-boxes-only` 只更新这六个电池盒。完整生成分支也保留三态逻辑；相同文件内容不重写。标准中文从GregTech.lang 的 10080–10085 获取，其它设备的既有翻译不再被此生成器覆写。

## 验证

- compileJava 通过。
- Python 3 项测试通过（build/assets_batch57.log）：216 个资源状态引用/着色层，10 份原版纹理和动画逐字节比较及六个标准译名，选择性生成器幂等。
- 新增 BatteryBoxPanelTests 三项：真实世界三态更新；实际方块点击装板/选模式、状态启停、更新 NBT 与实际破坏掉落；空电池盒装能源板及物理弱/强红石输出。
- 完整门禁在新目录 build/gametest-batch57-battery-panels 完成：2026-09-28 04:31:47 **1001/1001 required tests passed**，Gradle 退出码 0，BUILD SUCCESSFUL in 8m 47s。日志 build/gametest_batch57_battery_panels.log 无 failed! 或 Parsing error，运行期间未修改 Java/资源。
- 本轮未实际启动客户端目视，不以结构测试替代贴图像素验收。

## 人工验收

重启 IDEA runClient：普通电池盒应按工作缓冲显示静止/激活/闪烁原版覆盖层；转向和含水后仍正常。空盒先贴能源显示板可成功；装入完整电池后显示随电量更新。按钮选择器点击应修改输出安培模式，状态面板右下开关控制输出；剪线钳切换检测器强/弱信号，撬棍拆下面板，拆盒掉落剩余面板和带电电池。

## 下一批依据与剩余

普通盒仍只有 ULV–IV 六档；GT6 Loader_MultiTileEntities.java:892–895 实际为十档普通四槽和十档大型十六槽。Electric_T 前十项依次 TinAlloy、SteelGalvanized、Al、StainlessSteel、Cr、Ti、Ir、Os、Trinitanium、Trinaquadalloy；中文第九索引档为 PUV1。不要沿用五档机器的材料循环冒称十档全齐。

现有 energy_storage_lv..iv 是另一组占位：使用大型电池盒贴图却具有凭空的 v*1000000 容量。下一批应优先考虑把这五个现有 ID 改为原版大型电池盒，而不是继续增加一套功能重复的新 ID；保留物品和必要存档读取，但不要保留错误免费容量作为原版规则。

HopperContainerMenu 已有 case 16 的 4×4 排列，但 GTMenuTypes.HOPPER_SIZES 未注册 16，forSlotCount(16) 会退到 18。应补精确 MenuType/客户端纹理选择并验证 16 格、玩家格位和 shift-click，而非重复写已有布局。

直接面板已接入，但 shutter 物品闸门、其它普通控制覆盖板、十档生存配方以及旧通用 BatteryItem 电压规格仍待继续。大型坩埚仍等实机测试。完整目标未完成，未构建 jar，没有执行 git commit。

大型盒配方的 M 是原版注册 10040+i 的变压器，不是普通机壳。现有 EnergyNodeDefinitions 只注册 ulv_lv 到 ev_iv 五个变压器，因此从大型 IV 起的依赖不齐；补大型盒生存路线时必须连同对应变压器/电路等级核对，不能回退成机壳假装配方完整。普通盒用一倍线缆和线，大型盒用四倍线缆和线，图案均为 WCW / WCW / XMX。

## 文件清单

新增 `block/energy/BatteryBoxBlock.java`、`gametest/BatteryBoxPanelTests.java`、`tools/tests/test_battery_box_assets.py`。修改 EnergyNodeBlockEntity、EnergyNodeBlock、EnergyNodeRenderer、GTEnergyNodes、EnergyNodeDefinitions、BatteryBoxEnergy 和 generate_energy_node_assets.py。资源更新六份电池盒 blockstates、18 份三态模型（其中 12 份新增），生成/核对六个 item parent 与双语名称；已有一致纹理/动画不重写。

最终进度审计已刷新 docs/porting-progress.json，日志 build/audit_batch57.log，缺失 token 仍为 49。最新 Java 时间 2026-09-28T04:23:31 早于最终日志 2026-09-28T04:32:19。
