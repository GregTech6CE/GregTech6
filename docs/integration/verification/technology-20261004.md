# 2026-10-04 / Technology 原版配方接续

整体移植仍未完成。本批按要求减少测试，不启动客户端或 GameTest；共享检查、编译、来源对照及验包分别记录。

## 实际修改

- 导入 `MultiItemRandomTools:481–490` 的十种机械臂工具头、395/398 的两种起火器、388 的空塑料打火机、352–355 的四条火柴工作台路线及 522 的遥控激活器，共 18 条。原版扫描仪和作物分析仪配方已经移动到 `Loader_MultiTileEntities:1075–1076`，另补两条；没有给调试扫描仪编造生存配方。
- `GTTechnologyRecipes` 向两平台已有 GeneratedRecipeSink 注册原版打包机、解包机、灌装机和火柴组装路线。十种一次性工具由一份微型钢板/橡胶粒与数量为零的对应机械臂头加工，产量依次为 `2,32,32,2,4,9,4,32,32,9`，16 EU/t、每件 16 tick；工具头为显式催化剂。三种打火机分别消耗 100/1000/100 mB 丁烷，16/64/16 tick，16 EU/t。
- 工具头和一次性工具进入工具合成标签。普通工作台消耗一件一次性工具，也消耗机械臂头；高级合成台按原版保留机械臂头。只扩展合成匹配，不赋予一次性扳手世界扳手点击功能。普通 GT 工具继续按原版磨损，原版手工具材料匹配器也接入合成工具身份。
- 修正 24 条旧 USB、覆盖板、物流和刷石模块配方的电路方向。`LoaderOreDictReRegistrations:375–383` 是高等级到低等级的替代链，T4 不能被 T1–T3 支付；资源与原版对应字符逐条核对，生成脚本同步修正。
- 扫描仪/作物分析仪使用 USB 等级标签；按 `LoaderOreDictReRegistrations:348–365` 允许高版本 U 盘/USB 线替代低版本，不能用低版本支付高版本。
- 刷石模块三条路线恢复非镜像策略；水/岩浆使用已有的有限 1000 mB 容器 ingredient，归还排空的实际容器，不能使用无限 fluiditem 支付。
- 引用检查发现橡胶锤头和 ANY.Diamond 为空的独立阻断。恢复 `OP:247 / MT:1301` 的橡胶锤头；按 `MT:208` diamond 工厂注册八种已有钻石到 ANY.Diamond。Diamantine 当前未注册，不把 NULL 哨兵加入材料组。

## 证据与范围

- 双平台快速编译：`work/technology-compile-20261004.log`，34 秒成功。
- 共享材料形态/注册名探针：七个材料族非空，新增 20 条配方的物品引用均能对应现有定义；共享 sink 发出 1,115 行（组装机 1,100、打包机 11、解包机 1、灌装机 3），逐项检查材料形态有效。该数量是共享数据发出量，不能当作游戏实际注册或机器执行数量。
- 完整材料图差异：definitions/post 各只有 10 行变化（一个橡胶锤头形态、八个钻石别名关系和组成员）；其他 129,222 行及所有非分组字段保持不变。更新快照前已逐字段检查，见 [独立差异](technology-differential-20261004.json)。
- 每条新增工作台配方的原版行号、散列、镜像策略，24 条电路等级原版核对及来源文件散列见 [来源回执](technology-source-20261004.json)。原作者 Gregorius Techneticies / GregTech 6 team 与 LGPL 许可声明保留；来源目录没有修改，没有导入新纹理或依赖。
- 正式 `:core:check :assemble :neoforge:assemble`：`work/technology-build-20261004.log`，9 分钟成功；共享检查合计 9,726 条断言。
- 避免普通材料标签的提前枚举：原生工具 ingredient 只展开直接引用的工具物品，标签通过已有工具标签接入一次性工具。`work/technology-final-20261004.log`，双版增量 51 秒成功，没有重复共享测试。
- USB 等级接续：只同步两条变更的 JSON 到既有 `core/build/resources/main` 输出，增量构建排除 `:core:processResources`，避免重新复制数万条未改资源。`work/technology-usb-final-20261004.log`，49 秒成功；没有重复共享测试。
- 最终成品验包通过：619 个当前共享 class，两版 metadata/署名/许可无缺失，没有重复/test-only 条目；46 个变更 JSON 逐字节匹配源资源，Forge 15 个、NeoForge 16 个变更原生 class 齐全，NeoForge 进一步逐字节匹配当前编译输出。Forge 已重混淆，不能和未混淆 class 直接作字节相等比较。见 [完整成品回执](technology-artifacts-20261004.json) 与 [变更成品回执](technology-changed-artifacts-20261004.json)。

## 未完成范围

未运行新配方的生存制作、机器加工、容器返还、合成网络同步和存档重载。扫描仪目前仍缺原版充电/扣能的完整行为；机器自动合成的动态配方与工具头保留机制还需接续，高级合成台修正不能作为机器自动合成证据。原版 Invar/铂空打火机来自掉落/战利品路线，本批没有发明其工作台制作配方。材料原版 ore-dictionary 跨模组回调的全面替代、起火材料全部获取路线、Technology 其他配方、模具预览、蜜蜂和 tooltip 全面核对仍未完成。

代码提交：`d35eac4e0db34d707c2f128048e805136eed1443`。回执中的 `source_commit` 标识验收对应源码；JAR 未嵌入 Git 提交号，`build_commit` 保持 null。
