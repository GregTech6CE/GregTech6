# 第六十二批：大型发电机与燃气轮机的电池配方替代

## 原版核对

权威源为 gregtech6-master/gregtech6-master/src/main/java/gregtech/loaders/b/Loader_MultiTileEntities.java:1259–1267。

- 17221–17224 四档大型发电机主壳统一使用 gt:re-battery1，即 LV 充电电池，不随发电机等级升高。
- 17231–17234 四档大型燃气轮机主壳分别使用 gt:re-battery1/2/3/4，即 LV/MV/HV/EV。
- 八条配方均要求 OD_CIRCUITS[6]。
- 同文件共有 33 处 re-battery 引用：25 处是化学电池本身的标签注册，八处是本轮机器配方，不能把 33 处全部统计为缺失机器配方。

## 本轮修改

八份 recipes/axial 静态 JSON 的 B 键由单一 battery_lv/mv/hv/ev 改为 gregtech:rechargeable_batteries 对应等级标签；C 键改为 circuits_tier_6_plus。支持已注册的五种化学体系以及保留旧电池 ID，不允许跨电压替代。配方形状、材料、工具、主壳、结果数量和禁止镜像规则保留。

新增 tools/normalize_axial_battery_recipes.py，从原版注册行读取电池/电路等级，校验八个目标后更新静态资源。当前运行时 MachineRecipeIngredients 已正确解析相同标签；本次缺陷是静态资源仍残留旧单一物品，不是运行时解析器错误。脚本重复运行不改变结果。

## 验证

资源专项日志：build/assets_batch62.log，6 项通过。覆盖八条配方的等级、只改 B/C、幂等、现有轴向机器资源及旧电池资源。

新增 AxialBatteryRecipeTests，直接读取游戏实际加载的八条配方，检查五种化学体系 × 五电压的接受/拒绝、旧 ID、化学电芯误用以及电路等级。

全套新世界 GameTest：build/gametest-batch62-axial-batteries，日志 build/gametest_batch62_axial_batteries.log。06:24:11 确认 All 1016 required tests passed，06:24:42 正常关闭，Gradle 退出码 0，BUILD SUCCESSFUL in 8m 53s。包含新增配方匹配测试和完整运行时配方重载专项。未构建 jar。

## 待完成与验收

- IDEA 完全重启后，在 JEI 查看四档发电机和四档燃气轮机主壳：电池槽应轮换同档化学电池，实际合成可互换；低档/高档电池不得越级替代。
- 大型坩埚沿用第四十五批修复，尚未实机验证，不能记为客户端验收通过。
- 旧非放置 BatteryItem 五档仍接受任意正电压；GTElectricItems 保留 100000→25600000 EU 容量，未查明对应原版化学身份。ElectricToolAssembly 保留旧 LV 工具变体，同时已有五类正规 LV 电池变体，不宜直接删掉旧物品或随意重命名化学类型。ChargingCraftingRepairTests 中仍有旧 LV 电池接收 512 EU 的预期，下一批须同时核对实现与这些旧测试假设。
- 本批没有改变电池电压行为、注册数量或机器运行逻辑；没有构建 jar，没有执行 git commit。完整移植目标仍未完成。
