# 后续工作目标（2026-09-30 历史快照）

> 本文档记录 2026-09-30 时的后续工作安排，已被 2026-10 的原版对齐审计取代。当前状态以 [STATUS](integration/STATUS.md) 和 [PLAN](integration/PLAN.md) 为准，下文仅保留历史语境。

> 2026-09-30 第七十三批：榨汁器恢复原版直接消耗手持物品、各面加工、全部固体产物交还玩家及已有积液门槛；取消错误的物品管道接口，通用库存加工不再处理榨汁器。新增三项真实点击回归，包含满包掉落与权限，纠正旧苹果测试和生命周期测试中的错误库存假设。build/gametest_batch73_juicer_fixed.log：1044/1044 全绿、退出码 0。详见 docs/REPAIR_BATCH73_2026-09-30.md。未打包 jar；配方提示角与其他手工盆精确交互仍待核对，完整目标未完成。

> 2026-09-30 第七十二批：七种加工容器、粉末漏斗、普通/高级养蜂箱补齐 capability 恢复；放置式容器不再在查询或内容更新时提前复活失效接口。保留内容物及原版自动化权限。新增三项生命周期测试，旧实现三项失败；最终 build/gametest_batch72_lifecycle_verified.log 为 1041/1041 全绿、退出码 0。详见 docs/REPAIR_BATCH72_2026-09-30.md。未构建 jar，客户端与实际管道缓存兼容仍待验收，完整目标未完成。

> 2026-09-30 第七十一批：修复大型坩埚锅体各面 UV 旋转/镜像，提取实际面发射测试接口，六面顶点 UV 测试先失败后通过；12 张贴图与 GT6 原版逐字节一致。新世界 build/gametest_batch71_crucible_uv.log：1038/1038 全绿、退出码 0。详见 docs/REPAIR_BATCH71_2026-09-30.md。本轮未构建 jar，需重启 IDEA runClient 验收四个朝向；此前暂停已解除，完整移植仍未完成。

> 2026-09-28 第七十批：铸币模具补原版手动取回小板、动态材料内容物、四个内壁与 12/16 选框/碰撞。新世界全量 1038/1038 通过，退出码 0；build/reobfJar 成功，交付 build/libs/gregtech-1.0.0.jar（37,801,661 字节）。源码/门禁/jar 时间顺序和包内资源已核验。详见 docs/REPAIR_BATCH70_2026-09-28.md。按要求交付后暂停，不进入第七十一批；客户端模型和大型坩埚仍待实测，完整目标未完成。

> 2026-09-28 第六十九批：电锯、斧、双头斧和手锯共用原版树苗/工作台背包放置行为，末槽优先、树苗仅顶面、工作台拒绝木材/植物、创造不扣材料、工具电量/寿命不变；使用 ItemStack.useOn 保留 Forge 放置取消与回滚。新增内部工作台扩展标签，默认原版工作台。build/gametest_batch69_woodworking_placement.log 为 1036/1036 全绿、退出码 0，新增三项实际放置测试。详见 docs/REPAIR_BATCH69_2026-09-28.md。下一步仍需电锯连伐和树高/梁速度、枯灌木掉落、工具配方替代与携带电池补能；完整目标及客户端模型验收未完成。未构建 jar。

> 2026-09-28 第六十八批：电锯接通目标采掘、品质+1、材料速度×2、硬度耗电/磨损及叶冰掉落；现代 Forge 默认叶剪取改走剪刀战利品，自定义剪取保留接口。修复 float 硬度取整（叶10 EU、冰25 EU）。回归期间线程转储定位到管道光照跨线程等待，物品管/流体管/电线/传动轴的透光判定改为不读取邻居，新增全注册连接状态回归。修正旧极大损耗测试允许合法概率破损。最终 build/gametest_batch68_chainsaw_verified.log 为 1033/1033 全绿、退出码 0。详见 docs/REPAIR_BATCH68_2026-09-28.md。下一步继续电锯父类连伐、树高/梁速度、枯灌木掉落、树苗/工作台放置及工具配方替代；客户端模型待实测。未构建 jar，完整目标未完成。

> 2026-09-28 第六十七批：电动扳手补普通/活动扳手模式，NBT 保留材料/电池容量/电量/磨损，共用原版图标；潜行点击无方块实体目标切换，机器操作优先，外部方块实体不切换，空电可切模式但不能工作。新增两项测试。首轮因 F 盘满中断，空间恢复后在新世界重跑，build/gametest_batch67_wrench_modes_recovered.log 为 1029/1029 全绿、退出码 0。详见 docs/REPAIR_BATCH67_2026-09-28.md。下一步继续电锯完整采掘/剪叶/采冰/连伐及工具配方替代；大型坩埚与其它模型的客户端验收仍待实机验证。未构建 jar，完整目标未完成。

> 2026-09-28 第六十六批：电动扳手接通生存拆机、头部材料速度/品质、按硬度耗电与材料磨损、机器自动收集及内部物品独立掉落；11 个旧机器/管道类统一电动采掘判定，能源节点采用原版扳手分类。修复原版耐久标志与自定义材料寿命冲突导致无法收集。最终 build/gametest_batch66_electric_harvest_complete.log 为 1027/1027 全绿、退出码 0，新增三项含实际生存拆卸测试。详见 docs/REPAIR_BATCH66_2026-09-28.md。下一批继续模式切换、电锯及工具配方替代；大型坩埚客户端仍待实机验证。未构建 jar，完整目标未完成。

> 2026-09-28 第六十五批：电动工具新增独立材料寿命、品质决定的磨损概率、GT.ToolStats.k 保存与原版同材质废料范围，接入机器点击及手钻炸药放置；损坏工具禁止继续使用，实际持有者获得废料，库存重载不重复回收，创造免损耗。最终 build/gametest_batch65_electric_wear_final.log 为 1024/1024 全绿、退出码 0；新增三项测试含强制随机边界、实际破损与 NBT 恢复。详见 docs/REPAIR_BATCH65_2026-09-28.md。下一批优先电动扳手机器拆卸的速度/采掘/能耗/材料磨损与自动收集，再补模式切换、电锯和配方工具替代；本批仍未开放电动拆卸。坩埚及客户端显示待实测，未构建 jar，完整目标未完成。

> 2026-09-28 第六十四批：电动扳手/螺丝刀接入机器工具识别、共用选面预览、旋转/覆盖板配置及耗电；正电量单件可用，普通点击 100 EU，原版返回值向上取整，空电不能再操作或误按覆盖板按钮，权限拒绝不扣电。最终 build/gametest_batch64_electric_clicks_green.log 为 1021/1021 全绿、退出码 0；首轮覆盖板测试选错 STATUS 已更正为 BUTTONS 并换新世界重跑。详见 docs/REPAIR_BATCH64_2026-09-28.md。下一批优先电动工具材料概率磨损、破损残件及机器拆卸，然后模式切换/电锯采掘；本批没有开放免费电动拆卸。坩埚及本轮客户端外观待实测，未构建 jar，完整目标未完成。

> 2026-09-28 第六十三批：旧 battery_lv..iv 补齐 GT6 电压范围、单件限制、每次包数上限、正负包绝对值、充满余包和理论能力，电池盒不再跨档使用旧电池；保留历史容量与工具变体。补电压提示及两项 GameTest，修正充电台/线缆测试中的旧无限电压假设。最终 build/gametest_batch63_legacy_voltage_green.log 为 1018/1018 全绿、退出码 0（首轮线缆测试装置失败已更换新世界修复重跑）。详见 docs/REPAIR_BATCH63_2026-09-28.md。下一步优先电动工具机器交互识别与耗电、材料磨损和原版特定采掘行为；LV 电钻不是通用镐。坩埚客户端仍待实机验证，未构建 jar，完整目标未完成。

> 2026-09-28 第六十二批：按 GT6 原注册修正八条大型发电机/燃气轮机生存配方，单一旧电池改为精确电压的充电电池标签，支持五类化学体系及旧 ID；电路恢复 T6 标签。新增原文驱动规范化脚本与实际配方匹配测试。build/gametest_batch62_axial_batteries.log 为 1016/1016 全绿、退出码 0，6 项资源检查通过。详见 docs/REPAIR_BATCH62_2026-09-28.md。旧 BatteryItem 任意正电压及其旧测试假设留待下一批，坩埚客户端待实机验证，未构建 jar，完整目标仍未完成。

> 2026-09-28 第六十一批：battery_eu_8/32/128/512/2048 五个旧占位保留 ID，但改为真实被动锂钴电池，复用物品电量、电压、潜行放置、蓝色电量条、保电掉落；旧 energy_node NBT 加载时转为 chemical_battery 并保留原缓冲电量。创造/新配方保持 25 个规范型号，底层全注册 25+5。`build/gametest_batch61_legacy_batteries.log` **1015/1015 全绿**、退出码 0，14 项资源检查通过。详见 `docs/REPAIR_BATCH61_2026-09-28.md`。下一步优先把大型燃气轮机等原版 gt:re-batteryN 配方从单一 battery_lv..ev 改回等级电池标签，并审计 BatteryItem 旧非放置电池的电压/制作依赖。其它占位与大目标仍未完成，大型坩埚待实机验证，未构建 jar。

> 2026-09-28 第六十批：电力变压器接通 64 tick 三态外观（九档 324 资源状态）、模式限压、堵塞拒收、正反转换独立状态、停止后缓冲输出，以及直接控制面板/NBT/渲染/红石/掉落。`build/gametest_batch60_transformer_controls.log` **1012/1012 全绿**、退出码 0，8 项资源检查通过。详见 `docs/REPAIR_BATCH60_2026-09-28.md`。过载计数不持久化是原版规则，已纠正此前将其列为缺漏的判断；闲置衰减已接入，启动宽限/完整结构检查仍待核对。下一步优先整合 battery_eu_* 五个旧通用储能占位与真实高级化学电池的身份、充放电、配色和保电掉落。高档电路及兼容材料仍未齐，大型坩埚待实机验证，未构建 jar，完整目标未完成。

> 2026-09-28 第五十九批：11 条能源制作配方接入 ANY 铁/钢双板和铜/退火铜线缆组，严格区分粗细和绝缘；电池盒接通 shutter 的六面自动化包装、模拟、缓存接口动态门控、控制器/反转/NBT/拆板与能力失效。`build/gametest_batch59_materials_shutter.log` **1008/1008 全绿**、退出码 0，6 项资源检查通过。详见 `docs/REPAIR_BATCH59_2026-09-28.md`。Enori 属被导入器明确跳过的 gem_aa/Actually Additions 兼容组，须按原版兼容条件审查，不能盲目补注册。下一步优先变压器活动状态、覆盖板/过载持久化与旧电池电压归并；高档电路仍待核对。大型坩埚客户端待实机验证，未构建 jar，完整目标未完成。

> 2026-09-28 第五十八批：普通/大型电池盒扩为各十档四槽/十六槽，energy_storage_* 去掉免费容量占位；修正 16 槽 MenuType、玩家背包 Y=84 与原版 GUI。电力变压器补为九档，修正双向分包能量守恒、输入输出面与反转清缓冲；新增 23 条生存配方。`build/gametest_batch58_large_battery_final.log` **1005/1005 全绿**、退出码 0，6 项资源测试通过。详见 `docs/REPAIR_BATCH58_2026-09-28.md`。高档电路及原版未注册 10049 依赖尚待核对，ANY 铁双板/铜线替代、shutter 与变压器覆盖板/动画仍待补；大型坩埚待实机验证。未构建 jar，完整目标未完成。

> 2026-09-28 第五十七批：六个普通电池盒接通原版三态缓冲覆盖层（216 个方向/状态/含水组合），复用原版闪烁贴图动画；直接面板装卸、按钮/红石模式、状态启停、能源/进度红石与显示、NBT/掉落接通。空盒声明 EU capacitor，修复空盒不能先装能源面板。`build/gametest_batch57_battery_panels.log` **1001/1001 全绿**、退出码 0，3 项资源测试通过。详见 `docs/REPAIR_BATCH57_2026-09-28.md`。下一批优先把 energy_storage_* 免费容量占位改为原版 16 槽大型盒，补精确 16 槽 MenuType、十档家族与变压器依赖制作链；shutter 门控仍未接通。客户端外观待实测，未构建 jar，完整目标未完成。

> 2026-09-28 第五十六批：普通电池盒接入完整 IItemEnergy 物品的每秒逐槽充放电、独立工作缓冲、按电池数量和模式输出、启停控制转发、Jade 物品储能统计与取出/存档/破坏保电；不再接受灌注电芯。补理论充放电能力，电动工具按 GT6 只充不放、64 包/次并校验电压；旧通用电池修正溢出。`build/gametest_batch56_battery_energy.log` **998/998 全绿**、退出码 0，无配方解析错误。详见 `docs/REPAIR_BATCH56_2026-09-28.md`。下一批：电池盒直接控制覆盖板、原版三态纹理、大型 16 槽和缺失高档系列/制作路线；旧通用电池电压规则仍待归并。大型坩埚待实机验证，未构建 jar，完整目标未完成。

> 2026-09-28 第五十五批：电池盒改为固定四槽，修正插入指定槽/模拟操作/取出移位/稀疏存档问题，接通 GUI 共用的六面物品能力与能力失效；旧机器电路要求统一为同档或更高档标签，纠正低档越级替代。`build/gametest_batch55_inventory_circuits.log` **994/994 全绿**、退出码 0，无配方解析错误。详见 `docs/REPAIR_BATCH55_2026-09-28.md`。下一批必须继续电池盒真实 IItemEnergy 逐槽充放电和模式控制；目前仍有灌注电芯容量占位逻辑，不能称系统还原完成。大型坩埚仍待实机验证，未构建 jar，完整目标未完成。

> 2026-09-28 第五十四批：新增五类化学体系 × 五电压共 25 种原版电池，补 25 条制作配方、充放电包规则、潜行放置与保电掉落、原版模型／电量条、标准译名及 Jade；五类 LV 电池接入电动工具装配，新增 3780 条变体、总计 4536 条。`build/gametest_batch54_batteries_final.log` **990/990 全绿**、退出码 0；4 项资源测试及 Jade 翻译检查通过。详见 `docs/REPAIR_BATCH54_2026-09-28.md`。下一批优先重做电池盒逐槽充放电、纠正旧机器配方电路替代方向，再补电动工具行为；大型坩埚仍待实机验证，未构建 jar，完整目标未完成。

> 2026-09-28 第五十三批：四种 LV 电动工具新增 756 条材料装配配方，接入加工工具磨损返还、成品材料／电池容量 NBT、实际材料染色及双语提示，修正工具充放电乘法溢出。`build/gametest_batch53_electric.log` **985/985 全绿**、退出码 0；2 项资源检查通过。详见 `docs/REPAIR_BATCH53_2026-09-28.md`。下一步优先核对普通化学电池与现有高级电池／电芯的区别，补电池制作路线和电动工具实际行为／材料磨损。大型坩埚仍待实机验证；未构建 jar，完整目标未完成。

> 2026-09-28 第五十二批：电钻接入背包逆序取炸药、嵌入放置、100 EU 消耗与快捷栏遥控绑定；统一可钻入土层规则，保留 Forge 放置取消回滚。另修地牢刷怪农场外围平台连接数越界，纠正蜂巢／砧测试误判。最终 `build/gametest_batch52_drill_g1_green.log` **982/982 全绿**、退出码 0；4 项资源检查通过。详见 `docs/REPAIR_BATCH52_2026-09-28.md`。大型坩埚沿用第四十五批实现，客户端待实机验证。下一步：电动工具材料装配、制作路线与磨损；未构建 jar，完整目标未完成。

> 2026-09-28 第五十一批：炸药改为 GT6 固定 3×3×3、质量作为抗爆阈值、完整时运掉落与物品实体免伤；接入可保存的 100/20 tick 引信、点火/灭火工具、红石输出和 Forge 爆炸事件；恢复点燃动画/嵌入模型及地牢嵌入状态。`build/gametest_batch51_dynamite.log` **977/977 全绿**、退出码 0，2 项资源专项通过。详见 `docs/REPAIR_BATCH51_2026-09-28.md`。下一步：原版钻头放置嵌入炸药行为及可钻入土层白名单；未构建 jar，完整目标未完成。

> 2026-09-27 第五十批：补齐 60 种材料脚手架（新增 59 个方块）与 60 条原版生存配方、材料纹理和标准中文名；跨材质支撑接通，基岩矿井地牢的黄铜/钢抽样同时作用于栏杆和脚手架。最终 `build/gametest_batch50_scaffold_green.log` **971/971 全绿**，退出码 0；资源专项 7 项通过，模型缺失 0。首轮因测试使用无材质展示工具失败，已换真实工具重跑。详见 `docs/REPAIR_BATCH50_2026-09-27.md`。下一批：炸药嵌入状态、倒计时及 GT6 固定 3×3×3/抗爆阈值爆炸；未构建 jar，完整目标仍未完成。

> 2026-09-27 第四十九批：脚手架补原版杆件与 HATCH 分层纹理；通用材料生成器保留硬币浮雕模型，语言生成只补缺失条目，防止删除已有标准译名。`build/assets_batch49.log` **18 项资源测试通过**，三份纹理与 GT6 原图一致。本批无 Java 修改，最新 Java 门禁仍为第四十八批 969 项通过。详见 `docs/REPAIR_BATCH49_2026-09-27.md`；未构建 jar，完整目标未完成。

> 2026-09-27 第四十八批：高级按钮修正六面贴附边界并恢复 24 个亮灭/方向状态；脚手架恢复四种结构外观的 16 个状态。通用资源生成器改为调用这两类的专用生成器，回归验证连续生成两次不覆盖。Java 门禁 `build/gametest_batch48_button.log` **969/969 全绿**、退出码 0；最终资源专项 **11 项通过**。详见 `docs/REPAIR_BATCH48_2026-09-27.md`。未构建 jar，客户端外观待验收，完整目标未完成。

> 2026-09-27 第四十七批：高级养蜂箱补齐 GT6 生存配方、十类杂交蜂巢标签及两份 1000 mB 蜂蜜容器消耗/返还；普通与高级型接入六面温湿度读取。`build/gametest_batch47_honey.log` **968/968 全绿**，BUILD SUCCESSFUL。详见 `docs/REPAIR_BATCH47_2026-09-27.md`。大型坩埚模型/Jade 修复见第四十五批；客户端外观待验收，未构建 jar。其余占位家族与原版差异仍待核对。

> 2026-09-27 第四十六批：普通/高级养蜂箱接入蜂勺取出模式及客户端权限同步，普通型补 GT6 生存配方与 ANY.Iron 螺丝组标签。`build/gametest_batch46_scoop_green.log` **965/965 全绿**，退出码 0，未构建 jar。详见 `docs/REPAIR_BATCH46_2026-09-27.md`。下一步：高级养蜂箱两份 1000 L 蜂蜜的容器消耗/返还配方、温湿度工具反馈，以及其它占位家族差异。

> 2026-09-27 第四十五批：大型坩埚补齐成型锅体、墙体模型隐藏/拆解恢复和 Jade 主控/墙面内容转发；蜂后周期攻击及开盖防御接线。新世界 `build/gametest_batch45_crucible_green.log` **963/963 全绿**，Gradle 退出码 0。12 张锅体纹理与 GT6 原图一致；实际像素仍待 IDEA runClient 目视。详见 `docs/REPAIR_BATCH45_2026-09-27.md`。下一步继续蜂箱生存配方、蜂勺界面与其它占位家族差异；未构建 jar。

> 2026-09-27 最新状态：第四十四批见 `docs/REPAIR_BATCH44_2026-09-27.md`。原版水与六种 GT 海／河／沼泽水改为不依赖标签同步的直接双向互认；九种世界流体两态共 18 个透明层注册，地热泉保留专用原版贴图。GT6 物流储罐 `NBT_CAPACITY_HU=100000` 已核正为 **100000 K 熔毁温度**，不是 HU 储能；新增 75 条书架生存配方、Bumbliary 工作场所门槛、地牢工坊独立饮料抽样。全新世界 `build/gametest_batch44_water.log` **960/960**，`compileJava` 通过；Gradle 客户端已注入 Mixin 并加载纹理图集，IDEA 进世界的水面像素仍待目视确认。下一批优先蜂箱两条生存配方与蜂刺、其它 GT6 显式储罐参数，并继续排查实机看到的具体水面异常。以下第四十三批及更早数据为历史快照。

> 2026-09-27 上一批历史状态：第四十三批见 `docs/REPAIR_BATCH43_2026-09-27.md`。最终新世界 `build/gametest_batch43_water_green.log` **953/953 项通过**，日志确认水体 Mixin 注入；新增原版水与六种 GT 世界水双向共享面 GameTest。Mass Storage 胶带封存／破坏掉落、75 种书架、地牢指示花与刷怪农场外围平台、GT6 物流储罐已接入。IDEA 游戏内水面仍需完全重启后目视核对；若仍异常，请区分接缝、颜色、透明度或水下画面并检查那次 `debug.log`。当时的下一批建议为书架生存配方、物流储罐温度参数和其它 GT6 缺项；前两项已在第四十四批处理。下文早期验证数据均为历史快照。

> 2026-09-27 上一批状态：第四十二批详情见 `docs/REPAIR_BATCH42_2026-09-27.md`，最终全新世界 `build/gametest_water_batch42_green.log` 为 **942/942**。水体 Mixin 增加客户端实际调用校验和透明层检查；GT 海/河/沼泽水生成时保留首格以下的流动等级。Gradle `runClient` 新日志已确认 Mixin 注入且客户端加载到材质图集与声音阶段；IDEA 游戏内水面像素仍需完全重启后检查。原版整叠放置的锭/板/宝石板堆、16 件小板条箱和四种手工器皿已补，另修书架支撑面与采掘标签。下一批优先物流罐专用节点、普通 Mass Storage 非包装模式掉落，并继续按 GT6 原文补生存配方与内容。下文较早的门禁／注册量是历史快照。

> 2026-09-23 更新：最新实现与验证以 `docs/REPAIR_TRACKER_2026-09-20.md` 第二十五批和实际日志为准。下文 2026-09-19 的门禁、jar、注册量以及物流核心 W1 起点都是历史快照，不能当作当前结果。物流核心已有结构/供电/扫描及物品、流体 Import→Export，下一步是 Storage/Defrag/Dump、CPU 显示和覆盖板世界外观。以 IDEA `runClient` 测试；未要求 jar 时不重复打包。旧矿石存档迁移按决定不保留。

## 1. 当时的目标

```
继续 GregTech6 → MC 1.20.1 的长线移植（工程 F:\Dev\GregTech6\GregTech6，GT6 权威源
F:\Dev\GregTech6\gregtech6-master\gregtech6-master）。按 docs/PORTING_REMAINING_2026-09-14.md
的批次节奏推进。
开工先读 docs/REPAIR_TRACKER_2026-09-20.md 最后一批、docs/HANDOFF_LOGISTICS_CORE_2026-09-19.md 的 GT6 原始规格，
并以当前代码复核旧交接单的状态。优先次序：①物流核心 Storage/Defrag/Dump 和物流仓/罐接线；
②四种 CPU 显示覆盖板、世界贴附面与同步；③GT6 迷你传送门中继线及其它仍缺的内容。
每批的闭环：读 GT6 原文（带行号）→ 实现 → 写 GameTest → 门禁全绿
（All N required tests passed）→ 更新进度文档；仅在要求测试 jar 时打包。
门禁运行期间冻结被测代码；有新改动时停掉旧门禁后重跑。
```

## 2. 开工前必读

> 2026-09-19 补充：模组尚未发布、仅本人测试，旧矿石存档迁移不必保留。后续不修复旧区块迁移，可清理相关兼容实体、注册与专项测试；须保留新世界矿石行为，并先解耦正常物品背景数据对旧实体常量的依赖。审计详见 `docs/WEEKLY_AUDIT_2026-09-19.md` 第 34 项。

| 文件 | 作用 |
|---|---|
| `C:\Users\Asus\AppData\Local\Temp\gt6-port-handoff-2026-09-17.md` | **主交接单**：§1 当前状态、§2 命令、§3 陷阱清单（70+ 条，务必先看）、§5 下一轮建议 |
| `docs/PORTING_REMAINING_2026-09-14.md` | 批次史（§1…§124），每批的取舍与坑都在里面 |
| `PORTING_AUDIT.md` | 顶部横幅＝最新批次摘要（每批在最前面插一行） |
| `docs/porting-progress.json` | 注册量/配方/缺失 token 快照（由 `tools/audit_porting_progress.py` 生成） |

## 3. 历史验证快照（2026-09-19 20:55，不代表本次重新验证）

| 项 | 值 |
|---|---|
| 门禁 | **`All 681 required tests passed :)`**（0 条 `failed!`；2026-09-19 20:55:16，**新世界**） |
| jar | `build\libs\gregtech-1.0.0.jar` **32.5 MB（34,045,205 字节）**、**72,845** 条目、sha256 `16661AFA3C3C04847C296B49E2D9E4D334E13464A691A8407F853931B8A5C79B` |
| 注册量 | 材料 **1156** / 物品 **70,760** / 方块 **12,099** / 流体 **925** / 缺失 token **49** / 多方块部件 **89** |
| 不变量 | `java newest < gate log` **True**、`gate log < jar` **True**（`tools/handoff_check.py`） |

> **2026-09-19 晚的核对**（详见 `docs/HANDOFF_LOGISTICS_CORE_2026-09-19.md` 与 docs 里的 §125）：本文 §8 原来写的「下一批起点①量子充能器 / ②长距离端点」**都已经实现**（§124 的缺口清单是"按名字对照 ⇒ 高报"的假阳性，和 §113 同族）——①`GTLasers.java:88-92` 的 `quantum_energizer_*` 5 台 + 22 份资产/配方 + 测试；②`LongDistEndpointBlockEntity` 等 + `LongDistancePowerTests`。**真正剩下的就是 §8 的 ③物流核心**，起点见新 §8。

## 4. 工作约定（当时）

1. 不执行 `git commit`，改动留在工作区。
2. 每个检查点都要通过：改了 Java 就要跑门禁，不留"改了没验证"的状态。
3. 测试使用 `runClient`；没有明确要求 jar 的批次不打包，打包时才跑对应 jar 自检。
4. 记录使用中文和确切验证数字。
5. 门禁运行期间不要改 Java 源码（会让 `java newest < gate log` 变 False）；这段时间只写 Markdown。
6. 批次连续推进，不在中途等待确认，直到出现具体阻塞或明确停止。

## 5. 命令（照抄，省内存版）

```powershell
# 编译（秒级~40 秒，先跑它拿编译错误）
cmd /c ".\gradlew.bat compileJava --offline --no-daemon -Dorg.gradle.jvmargs=-Xmx512m -Dnet.minecraftforge.gradle.check.certs=false --console=plain > build\compile.log 2>&1"
# 门禁（换新世界是硬要求：测试数一变、或上一轮有红，就先把世界挪走）
Move-Item build\gametest-run\world build\gametest-run\world-<原因>-<时间>
cmd /c ".\gradlew.bat runGameTestServer --offline --no-daemon -Dorg.gradle.jvmargs=-Xmx512m -PgameTestHeap=4G -PgameTestJvmArgs=-XX:+UseSerialGC -Dnet.minecraftforge.gradle.check.certs=false --console=plain > build\gametest.log 2>&1"
# 结果必须看到 All N required tests passed
python -c "import io;t=io.open('build/gametest.log',encoding='utf-8',errors='replace').read();[print(l[:200]) for l in t.splitlines() if 'required tests' in l or 'failed!' in l]"
# jar / 进度 / 自检
cmd /c ".\gradlew.bat build --offline --no-daemon -Dorg.gradle.jvmargs=-Xmx512m -Dnet.minecraftforge.gradle.check.certs=false --console=plain > build\build.log 2>&1"
python tools\audit_porting_progress.py
python tools\handoff_check.py
# 改过 Java 时的秒级自查（文件列表必须含"被改动的那个类"，否则会从过期的 build/classes 解析）
python tools\javac_selfcheck.py <改过的 .java 文件…>
```

## 6. 每个批次的标准闭环

1. 读 GT6 原文（用行号引用），把**取舍**想清楚：值不值得做、移植版已有哪一半、抽哪几条 roll。
2. 实现（新方块/BE/配方时照 `BottleCrateBlock`/`UsbSwitchBlock` 的骨架抄：`HorizontalDirectionalBlock implements EntityBlock` + `ItemStackHandler` + NBT + 能力 + `onRemove` 掉落）。
3. **写 GameTest**（新功能至少 1 项；断言要能区分"旧实现会红"）。
4. `tools/javac_selfcheck.py` → `compileJava` → 门禁；只有要求 jar 时才执行 `build` 和 jar 自检。
5. 文档：`docs/PORTING_REMAINING_2026-09-14.md` 新 §（含 GT6 行号、取舍表、坑、验证数字）、`PORTING_AUDIT.md` 顶部插一行横幅（整行，保持 `ls[0]/ls[2]/ls[4]` 是横幅、`ls[1]/ls[3]/ls[5]` 是 `>`）、`docs/porting-progress.json`、交接单 §1/§3/§5。

## 7. 最近几批学到的、最容易再踩的坑（详见交接单 §3）

- **门禁在跑时别改 Java**；**javac 自查要把被改动的类列进文件列表**。
- **"上次跑绿"≠"那张 GameTest 世界还能用"**（§125.6 新记档）：只加了一个**未被引用**的类，复用世界跑门禁也出了 3 条无关红（`found 2`＝前几轮遗留掉落物 + `Can't keep up! … 225 ticks behind`）；处置是 `Move-Item build\gametest-run\world build\gametest-run\world-<原因>-<时间>` 换新世界重跑，**不是改代码**。
- **同 tick 看不到刚生成的掉落物** ⇒ 用 `runAfterDelay(1)` + `getAllEntities()`。
- **同状态 `setBlock` 是 no-op**，旧方块实体会留存 ⇒ 测试先 `removeBlock`。
- **植物/掉落要真有支撑面**：花盆不是支撑面（§120 的 bug）；树苗/甘蔗问 `#minecraft:dirt`，仙人掌只认 `Blocks.SAND`。
- **审计工具会假绿/高报**：路径前缀的分隔符、命名空间默认值、"名字不同"都当"没有"（§116/§113/§124 各踩一次）——结论必须回注册表逐条核实。
- **Forge 的 `ItemStackHandler` 钩子是 `onContentsChanged(int slot)`**（带槽号）。
- **配方 id 要先核实存在**（看 `models/item/<id>.json`），别猜（`processor_logic` ✗ → `logic_processor_unit` ✓）。

## 8. 历史起点（W1/W2 已实现，下方仅供追溯）

当前起点：物流核心已经形成 5×5×5 结构、墙面收 EU、扫描线缆和已安装覆盖板，并能按同一逻辑处理器循环做物品/流体 Import→Export。接下来按 GT6 `MultiTileEntityLogisticsCore.java:242-263,451-499` 补 Storage/Defrag/Dump 的路由顺序，再按 `:300-319` 补四种 CPU 显示的红石值、外观及同步。物流大宗仓和物流罐要接成真实存储节点；这两类与九瓶箱 BER、传送门中继线仍是后续工作。最新门禁结果查 `docs/REPAIR_TRACKER_2026-09-20.md` 末尾。

存储阶段的两项前置修正：普通路由目前用大容量仓对外显示的最多 64 件推断剩余容量，这会把实际 1,000,000 容量的仓库误判为满；要依据真实存量与模拟插入。移植版 `FluidTankGT.setPreventDraining(true)` 是禁止抽取，而 GT6 物流罐同名配置表示抽空后保留流体过滤模板，不可直接套用。原版核心的 108 私有槽/罐尚未参与路由，不要拿它们替代外部物流仓/罐。实现次序宜为专用存储方块和端点 → Import→Storage、Storage→Export、Defrag、Dump → CPU 显示与世界外观，并补配方、掉落、NBT 与容量测试。

**物流核心 W1（GT6 `17997`「Logistics Core」）**——完整对接单：`docs/HANDOFF_LOGISTICS_CORE_2026-09-19.md`（§4.2 是逐行核过的 GT6 规格，§4.4 是 W1→W4 步骤，§4.5 是坑）。要点：

1. **现状**：14 个物流覆盖板**物品**+47 条配方+36 份资产**全在**，但**行为 0 个**（`CoverItems.portCoverId` 不含物流 id ⇒ 能贴、永不派发）；`logistics_core` 是 `registry/GTLasers.java:101` 的 `new Block(qProps())` 占位（无 BE/无结构/无配方）；`logistics_wire` 只是 `GTIconSetBlocks:281` 的 iconset 装饰块；结构零件 18008/18299/18200-18204 **已全部注册且都是 `MultiblockPortBlock`**。
2. **本批已落地的一半**：`content/logistics/LogisticsCoreStructure.java`（157 行，javac exit 0）＝GT6 `MultiTileEntityLogisticsCore:109-147` 的结构分类（44 墙 / 53 通风 / 27 内核）+ CPU 计数（`18200` versatile 四表各 +1、`18201-18204` 各 +4、内核放墙 +0、storage 夹 108）+ `energyPerOperation()`＝`128+logic*64*conversion`。**未接线**。
3. **W1 要做**：`LogisticsCoreControllerBlock`（照 `CokeOvenControllerBlock` 骨架）+ `LogisticsCoreControllerBlockEntity`（`StructureController`；EU 包 256/512/1024、容量 `128+logic*256*conversion`、每轮开销 `20+logic+control+storage+conversion`；NBT 用 GT6 键名 `gt.cpu.*`）；**替换 `GTLasers.java:101` 占位**（id 仍是 `logistics_core`）；墙上进电走 `portEnergyTypes/injectPortEnergy`（照 `FusionReactorControllerBlockEntity:111-127`）；**配方必须走生成器**（`tools/gt6_multiblock_recipes.json` 加 `Logistics Core` 条目 + `tools/generate_multiblock_recipe_table.py` 的 `PART_TARGETS` 加 `"Logistics Core": "logistics_core"`——GT6 的 `M` 是**机壳**不能按零件替换；改完重跑生成器，否则手改的 `MultiblockCraftingRecipes.java` 会被覆盖）；新 `gametest/LogisticsCoreTests.java`（格子数 44/53/27、四计数、缺一类→无效、能量数值、NBT 往返、配方可加载）。
4. **之后**：W2 网络扫描（`LogisticsHost` ＝ GT6 `ITileEntityLogistics.canLogistics(side)`，Chebyshev ≤ `control+2` + 泛洪）+ 真 `logistics_wire` + 108 罐×16000 L / 108 槽缓冲；W3 12 个覆盖板行为 + 路由循环（Export↔Import → Storage → Defrag → Dump）+ **`CoverItems.portCoverId` 白名单**；W4 4 个 CPU 显示覆盖板（值/视觉公式在核心 `:300-319`）。
5. **不要再做**：量子充能器 T1–T5、长距离端点 10060/10061（**都已实现**，见 §3 的核对注）；Crystal Chargers（GT6 死代码：没有任何物品存 LU ⇒ 机器恒收不到能量）。
