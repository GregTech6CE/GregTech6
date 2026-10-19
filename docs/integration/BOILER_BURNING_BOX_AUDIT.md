# 单方块锅炉与液体／气体燃烧室核查

2026-10-19，基线 `05d713ec6`。原版来源为 `C:/Dev/gregtech6-master/gregtech6-master/src/main/java`。本批覆盖 26 种单方块锅炉的额定目录、控制器主要分支，以及液体／气体燃烧室的补燃、流体接口和控制。固体、流化床、大锅炉、多方块燃料机不因共享某些类名就视为核查完成。[回执](verification/boiler-burning-box-audit-20261019.json)保存来源摘要和 core 观察。

## 确认差异

| 编号 | 原版证据 | 当前路径和影响 |
| --- | --- | --- |
| ALIGN-019 | `MultiTileEntityBoilerTank.onToolClick2:161–184`：排空器先清水，水为空后清蒸汽；温度计返回储存热量／容量。 | `BoilerTankBlockEntity.onBlockActivated` 先清蒸汽，随后才清水，排空消耗固定按 100 返回值计算；没有锅炉温度计分支。前置 `ToolInteractions` 对该方块只处理扳手。Jade 显示热量不等于温度计行为；放大镜和现有烫伤仍存在。 |
| ALIGN-020 | 原版锅炉 `onExploded:208–210` 在已有压力表 >4 时触发自身爆炸；`removedByPlayer` 另处理玩家拆除。 | 当前 `BoilerTankBlock.playerWillDestroy` 有生存拆除爆炸，控制器有自身过热／满罐爆炸，但没有外部爆炸命中回调。`getDrops` 只抑制高压掉落，不会引发额外爆炸。未发现事件层补偿，缺少的是连锁处理，不能说锅炉完全不会爆炸。 |
| ALIGN-021 | 原版 `FL:119,696` 将 `potion.mineralwater` 纳入 WATER，锅炉 `getFluidTankFillable2` 和 `funnelFill` 使用此集合。 | 当前 `BoilerTankBlockEntity.isWater` 的十项路径白名单遗漏矿泉水；共享 `FluidCatalog.MnWtr` 已声明该流体且有 WATER 标志。问题是接收端没有使用现有分类，普通水和蒸馏水仍能接收。跨模组流体身份及漏斗顶面入口未完全核查。 |
| ALIGN-022 | `MultiTileEntityGeneratorLiquid.onTick2:116–166` 在 burning 或 cooldown>0 时运行；每次补燃设置 100 tick 余火，并循环消费到至少两倍额定输出。`onEntityCollidedWithBlock:235` 的烫伤也包含余火。 | 当前共享 `BurningBoxBlockEntity.tickServer` 仅在 burning 时处理；补燃只消耗一份，成功不重置倒计时，倒计时仅保存／递减。`BurningBoxBlock` 的接触伤害只检查 burning。弱燃料可能补不足即熄火，短时断料后的自动复燃和余火伤害缺失。两版都走此控制器；不扩大到单独的固体燃烧室。 |
| ALIGN-023 | 原版液体燃烧室 `getFluidTankFillable2:213` 要求配方接受且不是气体；气体子类要求配方接受且是气体。普通管路仅在熄火且无余火时可排出；tap 是另外允许排出的入口。 | 当前 `initInventoryAndTank` 创建无过滤的 `FluidTankGT`，两版能力和玩家容器交互都直接暴露该罐。可灌入无对应燃料配方的液体，亦没有运行／余火排出限制。无配方不代表会产能，当前燃烧仍查配方；不能误报为“任意流体发电”。 |
| ALIGN-024 | 原版 `MultiTileEntityGeneratorGas:38–45` 实现邻接与手动开关：关闭会熄火并清零余火；开启不会代替人工点火。 | 当前气体燃烧室及父类均未实现 `AdjacentEnergyControl.Provider`／`MachineControl.Provider`。项目已经有相应接口和调用者，但无法到达该设备。不能把原版合同描述为“有红石就自动点火”，也不能将 addon 的 `FuelGeneratorBlockEntity` 当作内置燃烧室的实现。 |
| ALIGN-025 | 原版液体燃烧室输出热量时按效率概率点燃周边；补燃前尝试烧掉前方可燃物，再检查无碰撞、非液体和氧气（`:123,131–133`）。气体子类继承该服务端行为。 | 当前 `FLAME_RANGE` 未被使用，tick 没有周边点火和前方燃烧；`GTFacingMachineBlockEntity.isFrontAir` 只判断空气／可替换，未等价表达原版条件。原版氧气的跨模组适配另待核，先确认本地周边燃烧分支缺失。 |

这些是源码调用链结论，尚未在实际世界触发。语言方面，小锅炉的 Calcification／Water／排空消息仍为字面量，归入已有 ALIGN-003，不另重复编号。

## 弱燃料的具体例子

当前 core 实际枚举得到：`burning_box_gas_dense_invar` 为 64 HU/t、效率 10000；氢气配方每 2 L、16×2=32 HU。原版 `Loader_Fuels:153` 也使用该份额。

在燃烧标志已打开、缓冲为零、前方条件满足、至少有 8 L 氢气时：原版一刻连续消费四份，得到 128 HU，下一刻可以按额定值输出。当前共享代码只消费一份得到 32 HU，随即因低于 64 将 burning 清为 false；余火计时又不参与运行条件，下一刻无法靠剩余燃料继续补足。这是使用实际参数沿源码推导的差异，没有冒称游戏复现。

## 已匹配的锅炉部分

原版 `Loader_MultiTileEntities:553–579` 的 13 种普通、13 种致密锅炉，与当前 `BoilerCatalog.all()` 的输出、硬度、默认蒸汽／热缓冲容量及建议热输入逐项匹配。ANY.Steel／ANY.W 在设备边界使用具体 Steel／Tungsten；本批只核额定值，不据此认定配方的材料组替代已验收。

主要运算路径已对照：每份水消耗 80 HU、满效率产生 160 L 蒸汽；单刻转换上限为蒸汽容量/2560；结垢判定在移除本批水后执行，保留“刚好烧空一罐水时不结垢”的原版顺序；128 tick 冷却等待和按输出倍率散热；蒸汽超过半罐才输出、超过四分之三时提高输出；热量超过容量或蒸汽满罐爆炸。

原版和当前均在蒸汽排出后判断满罐爆炸，不能把“某刻产生的蒸汽曾经达到容量”当成必须立即爆炸。当前 `max(1, capacity/2560)` 与原版直接除法在这 26 个额定容量下结果一致，没有因此新增缺陷编号。

碰撞形状缩入两像素、碰撞烫伤最高 10、低压除垢伤害不封顶、压力表 >15 除垢爆炸、>4 生存拆除爆炸和按蒸汽平方根计算爆炸强度，均可定位到当前代码。大坩埚／大锅炉的规则不能从这里推定。

## 未完成的边界与后续

- 普通锅炉 HU 能量查询与原版不同：原版最小输入为 1、最大为 Long.MAX_VALUE，当前继承半额定／双额定查询；HU 是大小无关类型，默认注入路径跳过最小值且锅炉 `doInject` 不按该最大值拒绝，故不能直接断言“大包热量被拒绝”。外部调用者影响待查。
- 当前保存冷却倒计时，而原版小锅炉不保存该字段；属于明确的保存行为差异，需确定现代存档策略。未做世界重启验证。
- 当前锅炉每刻请求同步，仪表使用平滑显示；原版主要按表针变化同步。性能影响尚未测量，不据此给出帧率／网络结论。
- 流化床使用同一个移植控制器，但其原版物品份额、石灰石、灰渣、槽位和火焰条件需单独查。`canStoreAsh()` 恒为 true 和 pendingAsh 替换路径是下一步重点，尚未在本批下最终结论。
- 多方块锅炉、固体燃烧室、燃料涡轮与变压器／储能仍在总审计队列。

## 可读性改进和轻量验证

`BoilerCatalog` 将压在一起的 imports／构造器展开，私有 Mat、MATS、m、out 及字段缩写改成 MaterialVariant、VARIANTS、variant、specifications 和明确的额定值名称；公共 `all()`、顺序、ID、数值和显示字符串保持。`BoilerHazards`、`BurningBoxFuelRules` 展开计算并说明单位和调用边界。

三份代码在仅归一化声明的私有重命名之后，代码及字面量与基线相同；Java 17 独立编译通过。`BoilerCatalogAudit.java` 只枚举 core 数据及一个实际燃料参数，不模拟原生机器。没有运行 Gradle、GameTest、客户端、完整测试或新构建；本批没有修复上述玩法差异。
