# 燃油引擎进液回归审计（2026-10-24）

## 结论与范围

用户报告“燃油引擎接收不了燃料”，怀疑公共机器框架。双平台原生基线确认两个独立缺口：**原版有效燃料未进入 Engine 配方表**、**流体显示物品缺少创造模式 first-use 灌液回调**。不能据此认定整个机器框架失效：实际 Diesel、8 个注册引擎的全部朝向/物理面、管道放置后输送、有限便携容器、Bath 对照、停机/NBT/盖板控制均通过原生测试。

本批只修复这两个已复现问题并校正来源能量；未修改 `KineticDieselEngineBlockEntity`、普通桶、有限容器、管道连接规则或公共机器调度。整体移植仍未完成。

## 来源合同

- `gregtech6-original/.../gregtech/loaders/b/Loader_Fuels.java:77–147` 与 `gregtech6-w/...:82–152` 同为18组 Engine 规则；按实际当前注册目录解析出25种流体身份，保留原 `.list` 别名及 RUM/WHISKEY/VINEGAR 类别。
- 每次为 **1mB 燃料→1mB CO2**，能量为 `|EUt|×duration`，没有随机产液或桶倍率。原 Diesel=64×7=448，Fuel=64×8=512，NitroFuel=64×12=768；旧移植表的512/768/1536并非原版数值。
- 八个注册体 `gt6ce:engine_diesel_{bronze,arsenic_copper,arsenic_bronze,steel,invar,titanium,tungsten_steel,iridium}` 对应原 ID9147/9146/9145/9148/9149/9197/9198/9199，输出16/16/24/32/64/128/256/512RU/t，每槽容量为输出×10。原注册 `Loader_MultiTileEntities:722–729` 的效率全部10000，故这些注册体的单次 gross/net credit 一致；不宣称任意旧自定义NBT效率兼容。
- 原 `MultiTileEntityMotorLiquid:188–199` 允许六个物理面进燃料、抽排气，停机只阻止燃烧；并无“正面不能进燃料”限制。当前 FaceConfig 并不控制该能力入口，未为了“修复”增加侧面禁用。
- 原 `ItemFluidDisplay:77–79`、W`:88–96`：服务端创造模式依次尝试被点击方块的六面，第一个成功即停止；服务端无论成功/生存/无效都消费该次点击，客户端放行。移植恢复相同分派合同，并保留权限/已加载检查、真实payload及原生侧面能力盖板过滤；不直接写入槽、不消耗无限显示物品。

## 修改

1. 新增维护型 core `OriginalEngineFuelRows`，显式记录18组来源规则；移除混合 `OriginalFuelRecipeRows` 中错误的9条 Engine 行，Burn/Gas/Hot保持。
2. `FuelRecipeLoader` 按准备完成的目录注册名/类别解析，再使用真实字段持有者取得流体；按原生流体身份去重，要求声明的1mB CO2存在。解决 `Nitrofuel`/`NitroFuel` 大小写及 `Ethanol`/`GenGas_Ethanol` 字段错配，同时补全 Kerosine/JetFuel/BioDiesel/BioFuel/BioEthanol/Reikanol 等来源燃料。未臆造外部模组注册；可选原注册名仅在目录实际存在时加入。
3. 共享 `FluidDisplayInteraction` 与 Forge `FluidItem`、Neo `FluidDisplayItem` 两个薄回调恢复创造 first-use。生存点击在物品层结束，不能落入 Bath 等更宽松方块回调制造无限燃料；有限便携容器既有行为不变。
4. DedicatedSmoke 旧普通周期512断言/日志改为来源448；Neo历史基线32分支保持。这两个专用世界生命周期协议本轮**只编译，未执行**；新增原生单次燃烧测试独立验证448及其余24种流体。

## 生产者/再生成检查

`OriginalFuelRecipeRows` 是维护型 core 源，不是本次只改了生成输出。对受跟踪的 tools/scripts/gradle/.github 进行文件名/类名/旧loader/写入模式检索，未找到重建该表的活动生产者；仅两份只读审计探针消费它。历史 `core/provenance/native-turbines-heat-fuels-integration.json:237–248` 记录从 saltnya `Loader_Recipes_Fuels` 适配；后续941ecd5直接维护hot行。`tools/transpile_gt6_chem.py:39–63` 枚举的生产集合无 fuels，`:70–71` 只写 `content/recipe/generated/GTxGen`。Stonecutter `multiversion/build.gradle:24–36` 生成 loader 构建视图，不覆盖 core 维护表。未运行历史硬编码路径生成脚本。

## 原生证据

| 阶段 | Forge | NeoForge | 结论 |
| --- | ---: | ---: | --- |
| 原生产码基线 | 194项/192通过 | 185项/183通过 | 每端仅“缺来源燃料表”和“创造显示物品完整分派”失败 |
| 修复后严格门禁 | 195/195 | 186/186 | 合计381，零失败/错误/跳过，精确case身份与3个既有命名空间校验 |

修复后完整保留7个引擎基线方法体；新增独立25行来源能量/产液检查及真实注册引擎单次转换，后方石块阻止排气，用实槽确认1mB CO2保存。Bath同一对照方法新增生存first-use取消，随后仍测试创造填充。保存基线e374引擎/b27Bath原件及XML，避免把修改后的断言当作原基线。

覆盖：8引擎×6朝向×6输入面、合法Diesel、非法水、模拟不变、提交/剩余容量守恒、有限空容器身份数量、管道真实BlockItem放置与输送、停机/运行、NBT回读、缓存侧能力与新盖板、创造/生存/无效物品完整gameMode分派。能力显示物品是无限源，不伪称有限容器守恒。

本地完整双版build/core、122 Python guards、486 REMI组、两包1014共享类及SDK边界校验均通过；输入/最终源码快照一致。证据保存在 `work/validation/fuel-engine-intake-baseline-retry/`、`work/validation/fuel-engine-intake-fixed/`；后者 `local-validation-receipt.md` 含命令、工件SHA及退出码。该回执只证明其冻结树，推送后CI须对最终提交另行核验。

## 未宣称与后续

CO2并非完全未实现：原/当前引擎都存储输出、检查排气槽容量、尝试后方转移。但原以“gas且无碰撞”放空，当前以“lighterThanAir且air”放空，存在独立来源差异。**本批未修改该行为，也无新原生排气放空证明**；未把气相等同于实测浮力。针对空气/花/玻璃片的草稿留在ignored工作目录，需另行获得本机数据/原生反例后处理。未宣称客户端/GPU、真实玩家完整流程、实际保存世界重启或整个模组玩法验收完成。

测试文件头部仍保留最初 ignored/uncompiled 草稿说明，这是历史说明，不代表当前验证状态；本节及冻结树回执记录本轮已编译并执行的真实结果。来源文件哈希、作者/许可及六个生产目标哈希见 [provenance](../../core/provenance/fuel-engine-intake-20261024.json)。

## 后续排气审计（独立于已通过的燃料入口批次）

上一批 f079e19b6 的381项通过只证明燃料入口/配方/创造物品；当时明确延后的排气差异现已加入原生反例。新增测试先读取真实注册 CO2：两端均记录 `gt6ce:carbondioxide gas=true density=756 lighterThanAir=false`。气相前提及玻璃片非空碰撞/保留1mB控制通过，随后在空气放空断言失败。因此是实际气相/浮力混淆，不是无效燃料或假气体前提。基线在空气断言中止，尚未执行后续蒲公英断言；不可宣称基线已经实测花阻挡失败。

原 `MultiTileEntityMotorLiquid:140–144`、W`:144–148` 先向后方能力转移，再用 `FL.gas && !WD.hasCollide` 放空。原 `WD:693–697` 为不透明或任何碰撞盒，W`:2143–2146` 同样使用不透明或非空碰撞形状。修正只在 `KineticDieselEngineBlockEntity.dischargeExhaust` 增加后方 `hasChunkAt` 保护，随后以 `GTFluids.isGas`、非 `isSolidRender` 且碰撞形状为空判定放空。不能把“非完整方块”当作“无碰撞”：玻璃片必须保留，空气可以放空，泥土上蒲公英虽非空气但无碰撞，也应放空。未知气体仍沿用既有 GTFluids.isGas 回退，不重写全局流体分类。

原生能力转移顺序、排气容量/燃烧背压检查、有限流体消耗保持不变。加载检查位于任何后方方块实体/方块状态读取之前，未加载后方保留排气、不强加载。新测试未单独证明跨区块卸载路径；这是显式安全保护而非伪称已有卸载测试。

证据：`work/validation/material-workability-baseline-template-retry/` 双平台日志与XML；该批同时包含独立铝加工测试，其失败数不能全部归于排气。固定407项门禁已通过：Forge208/208、NeoForge199/199，玻璃片保留、空气放空、蒲公英放空均实际通过。修正目标SHA256为 `78f7df8551febce8ac7832b485a0d3bf76b369395a52e80481fefd0d3ccab892`。此前测试文件 ignored/uncompiled 历史头注释已在本轮基线前清理；实际执行状态以回执为准。专用保存世界重启协议仍仅编译，不作执行声明。
