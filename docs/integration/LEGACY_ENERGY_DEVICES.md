# 燃料电池、魔法力场吸收器与稀土指示花补齐

2026-10-24。共享 Stonecutter 实现，适用于 Forge 1.20.1 和 NeoForge 1.21.1。

## 原版依据

核对官方 GregTech6 master 提交 `3703e40308c8c030763fd6297dea8b210d2a77b1` 的 `Loader_MultiTileEntities`、`MultiTileEntityMagicFieldAbsorber`、`MultiTileEntityKiloGibblometer`、`MultiTileEntityLargeBoiler` 和 `BlockFlowersA`。便携电池的保存、放电、空壳和外形同时对照保留的原版 `TileEntityBase08Battery`、`TileEntityBase09PowerCell`、`MultiTileEntityPowerCell`、`MultiTileEntityAneutronicFusion`。

代码来源为 GregTech-6 Team / Gregorius Techneticies，LGPL-3.0-or-later。新增贴图从保留参考树逐字节复制原版默认资源，沿用项目已有原版资源 CC0 来源声明。未复制参考移植的 Java 业务实现。文件来源及 SHA256 见 [来源回执](verification/legacy-energy-devices-20261024.json)。

## 本轮内容

| 注册 ID（命名空间 gt6ce） | 原版对应 | 功能 |
|---|---|---|
| `power_cell_empty` / `power_cell_hydrogen` | 14700 / 14701 | 氢气燃料电池，3,200,000 EU，最大 512 EU/包 |
| `aneutronic_fusion_power_cell_empty` / `aneutronic_fusion_power_cell_helium_3` | 14600 / 14601 | 无中子聚变电池，1,024,000,000 EU，最大 8192 EU/包 |
| `magic_field_absorber` | 10180 | 从世界坐标上方的魔法战利品持续产生能量 |
| `flower_vindicator_flower` | FlowersA:9 | 独居石、氟碳铈矿基岩矿床的稀土指示花 |
| `sensor_kilogibblometer`（既有 ID） | 31023 | 大型锅炉压力传感器，显示 kGibbl |

燃料电池空壳配方与原版材料及形状对应；装罐机使用 200,000 L 氢气或氦-3 填充，16 EU/t、256 tick。另有装罐机清空回收空壳配方。不能通过电力充电。电池盒放电后不足一个最大能量包即按原版归零并原位换成空壳。放置需潜行；放置后的电池是被动容器，不充当发电机。回收保留剩余能量，空壳不携带零电量 NBT，保持堆叠。模型、原版贴图、光照等级、物品能量条、Jade 与创造分页均接入，EMI 中单独归入便携电池组。

魔法力场吸收器固定检测顶部：龙蛋 64 QU/t，原版头颅 1 TU/t；暮色森林原版五类战利品对应 64 KU/QU/HU/LU/CU。KU 每 64 tick 改变符号；数量型能量以大小 1 的多个包输出。输出方向默认下方，扳手可改为任一水平侧面，不能朝上。软锤及共用机器控制可开关。无战利品立即停止；不消耗战利品，不缓存或通过拉取再次生成能量。补齐钯着色底层、无着色覆盖层、活动贴图、合成、提示和 Jade 实际输出。

Vindicator 花具有独立注册、原版纹理、掉落、盆栽、植物处理及指示提示，归入 EMI 基岩矿指示花组。独居石和氟碳铈矿新生成的指示植物不再误用矿兰；不回填既有区块。

最新版原版的 31023 已完整存在于移植版注册、钻石合成、数字渲染、红石模式和压力源接口中，没有新的独立“大锅炉专用”类。原来的中文“千吉伯计传感器”难以辨认，本轮改为“大型锅炉压力传感器（单位：kGibbl）”并增加用途提示。换算与原版相同：蒸汽量除以 1,000,000；普通 Gibbl 传感器除以 1,000。大型锅炉完整外壳及热量接口能够转发到主控，红石模式仍使用原有传感器面板控制。

电池与吸收器中文名称直接使用用户提供的原版汉化补丁。该旧补丁未包含 Vindicator 花和 31023，因此保留 Vindicator 专名并补充清楚的大锅炉传感器用途名称。

## 验证和边界

- 双版生产源码编译通过。
- NeoForge 三项集中 GameTest 通过；Forge 同三项通过，并增加全部合成原料非空和空壳可堆叠检查。检查实际电池盒放电与换壳、放置后电量恢复、装罐机配方、指示花真实注册、龙蛋/头颅输出、五个有效输出面、停机和移走战利品。
- 锅炉场景实际组装原版大型锅炉，向蒸汽槽写入 1,234,567 L，贴在绑定部件上的两种压力传感器分别读出 1234 和 1；拆墙、修墙及移除主控的读数亦验证。
- 15 份新增主体模型、105 个贴图引用存在；两种语言键和两类稀土指示映射通过静态核对。暮色森林五种普通/壁挂战利品 ID 已对照其官方 `TFBlocks` 源码；未装入暮色森林进行联动测试。
- 本轮未进行客户端视觉、真实玩家操作或存档重启验收，没有制作新的交付 JAR，也没有提交或推送到 GitHub。编译与服务端测试不能替代这些验收。

日志：`work/original-energy-reference/compile-native.log`、`gametest.log`、`gametest-forge.log`。整体 GT6 移植仍未完成，本轮仅补齐上述缺项并澄清传感器的现有对应关系。
