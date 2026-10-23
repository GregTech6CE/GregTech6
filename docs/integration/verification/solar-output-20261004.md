# 2026-10-04 / 太阳能板当前输出与工作贴图

启动崩溃修复后继续接续原版太阳能板。代码提交 `0c03444a442a038702c02714cabd4f4ecfbc6001`；成品在提交前从同一批工作区改动构建，内容回执记录对应源码提交及编译/资源指纹。整体移植仍未完成；本批只执行针对性共享样例、资源检查和集中双版构建，没有重复客户端或全套共享检查。

## 来源与行为

只读来源 `C:/Dev/gregtech6-master/gregtech6-master/src/main/java/gregtech/tileentity/energy/generators/MultiTileEntitySolarPanelElectric.java` 的 `generateEnergy`、能源接口、开关、视觉数据及纹理数组；注册来源 `gregtech/loaders/b/Loader_MultiTileEntities.java:901–902`。硅板是 TinAlloy / 8 EU，锗板是 Aluminium / 16 EU，两者引用同一组 solarpanel_electric_8eu 纹理。天空查询来源 `gregapi/tileentity/base/TileEntityBase01Root.java:258`，原版暮色判断来源 `gregapi/util/WD.java:190–192`。

| 条件（上方见天空） | 硅 / 锗每 tick 的实际输出 |
| --- | --- |
| 晴朗白天 | 8 / 16 EU |
| 湿润生物群系雨天 | 1 / 2 EU |
| 晴朗夜间 | 1 / 2 EU |
| 湿润生物群系雨夜 | 0 / 0 EU |
| 雨天、雨夜，但生物群系 rainfall 为零 | 按晴朗白天、夜间处理 |
| 雷暴或上方不见天空 | 0 / 0 EU |
| 暮色森林，无雷暴且见天空 | 4 / 8 EU，先于昼夜和雨量分支 |

现代适配读取当前生物群系 `getModifiedClimateSettings().downfall()`、维度 `hasSkyLight()` 和上方 `canSeeSky`。暮色森林官方 [1.20.x 维度定义](https://github.com/TeamTwilight/twilightforest/blob/1.20.x/src/main/java/twilightforest/init/TFDimension.java) 与 [1.21.x 维度定义](https://github.com/TeamTwilight/twilightforest/blob/1.21.x/src/main/java/twilightforest/init/TFDimension.java) 均使用 `twilightforest:twilight_forest`；相应 [1.20.x 天空属性](https://github.com/TeamTwilight/twilightforest/blob/1.20.x/src/generated/resources/data/twilightforest/dimension_type/twilight_forest_type.json) 与 [1.21.x 天空属性](https://github.com/TeamTwilight/twilightforest/blob/1.21.x/src/generated/resources/data/twilightforest/dimension_type/twilight_forest_type.json) 均开启 skylight。通过官方 GitHub Contents API 读取，blob 指纹保存在本批内容回执；没有安装/运行暮色森林。

## 修正

- 共享 `SolarPanelEnergy` 每 tick 重算当前能量，拒收时也不累加；按当前能量发送一个实际大小的 EU 包。晴朗夜间硅板发送 1 EU，而不是先蓄满 8 EU 后发送。
- 天空缓存首次检查，原版计时 `timer % 600 == 5` 刷新；邻居变化及重新开启请求下一 tick 刷新。关闭时保留原版的当前能量与 running 标志，暂停计算/发送；重启按新天空重新计算。
- 提取保留原版取绝对包大小、整次请求成功或零提取、模拟不消费的规则；对零、负数量、Long.MIN_VALUE 及乘法溢出请求安全拒绝。旧移植缓冲 256/512 EU 迁移为最多一个额定 tick 的 8/16 EU。
- 原生能源接口不再把太阳能板声明为电容器；不接受输入，仅从合法正面输出。状态面板区分可工作、正在产生和正在输出；没有虚构进度/程序槽。遮挡面板沿用覆盖板控制器的门控规则，不把覆盖板停止标志误作太阳能开关。
- 两版专用 `SolarPanelBlock.solar_active` 跟随是否产生能量，而不是是否有接收者，接入原版 active overlay。保留完整 16³ 方块、着色底层和未染色覆盖层；向下及四个水平正面分别选 bottom_facing/side_facing，顶面始终为收集面。旧非法 UP 状态加载后迁移 DOWN，并保留原方块、覆盖板和 waterlogged 状态。
- Tooltip 删除错误的仅白天描述，显示额定输出、原版 1/8 至全额范围和正面。两份语言文件其余差异只是既有键排序，未修改其值。

## 针对性验证

- `SolarPanelEnergyContracts` 独立执行通过 **64项**固定来源样例，覆盖两种额定值的天气分支、拒收不蓄能、真实小包、工作/输出分离、首次/5/605 tick 天空检查、邻居和重启刷新、停止保留、模拟/整次提取、非法包及旧缓冲迁移。已接入未来 `CoreBehaviorContracts` 第29组；本批没有执行完整共享套件。
- 静态资源检查通过：双端共 **40个**活动/非活动模型、**48个**方块状态映射、**15份**现有纹理，检查合法输出面、UP兼容映射、完整方块几何、六面纹理、RGB tint、平台 composite loader、物品父模型及当前共享资源副本一致。
- 正式 `:core:compileTestJava :assemble :neoforge:assemble -x :core:processResources` **5m58s终态成功**，Java 21.0.7；共享语言资源已同步为当前源码，其他共享资源未改动。没有快速构建/完整共享检查/客户端的重复执行。
- [成品验包](solar-output-artifacts-20261004.json)：**655个**当前共享 class、两端 loader metadata/NOTICE 正确，无重复条目或测试夹具。
- [太阳能内容回执](solar-output-content-20261004.json)：每平台 **14个**变更原生 class 在包中；Forge 重混淆后核对存在/哈希，NeoForge 逐字节等于当前编译输出。两个语言文件、双端太阳能模型/状态/物品父模型与15份纹理均逐字节等于当前资源；原版源码及官方暮色森林 blob 指纹已记录。

日志：`work/solar-output-contracts-20261004.log`、`work/solar-output-build-20261004.log`；轻量检查脚本 `work/verify-solar-output.py`。没有把检查夹具加入成品。

## 仍待接续

原版在工作中的每600 tick第5 tick调用继承的 `doDefaultStructuralChecks`：火/水接触、覆盖板防护、过载声音冷却及可配置破坏。本批没有接入该公共系统；原版 rainproof/thunderproof 均为 true，天气影响发电并不等于遇雨爆炸。后续能源设备公共结构检查和收尾 cfg 需一起对照。

太阳能板的原生网络、面板点击、工作材质切换、旧存档迁移及实际暮色森林均没有新的游戏/成品安装证据。上一批 Neo 主菜单证据只属于启动修复，不能代替本批太阳能玩法验证。占位电池和其他完整移植缺口继续保留。
