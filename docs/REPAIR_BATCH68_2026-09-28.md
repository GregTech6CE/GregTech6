# 第六十八批：电锯采掘与叶冰掉落

## 原版依据

GT_Tool_Chainsaw_LV：getBaseQuality=1、getSpeedMultiplier=2；isMinableBlock 包含 axe/saw 工具类型及木材、仙人掌、叶、藤、植物、瓜、冰/浮冰、珊瑚。convertBlockDrops 对可剪叶调用 IShearable.onSheared 替换掉落；冰在普通掉落为空时补自身并不留水。继承 GT_Tool_Axe 的方块损耗基数 50。MultiItemTool.onBlockDestroyed 使用 ceil(硬度×50) 耗能/磨损。

## 本批接线

- 新 ElectricChainsawHarvest 集中现代标签/方块族目标、材料品质+1、材料速度×2、叶/冰转换；没有将电锯伪装为通用镐。
- ElectricToolItem 区分有电可用与机器点击身份，电锯可消耗材料寿命和 EU，无须虚构扳手/螺丝刀身份。
- 正常方块沿原生 mineBlock/掉落流程，叶冰由 onBlockStartBreak 转换；该入口位于 Forge 可取消破坏事件之后，额外检查生存操作权限。读取掉落时保留原工具附魔，调用 IForgeShearable，冰已有掉落则沿原流程。
- 成功移除才扣一次硬度耗电及材料磨损，更新破坏统计/饥饿/效果；损坏时派发工具破损事件。创造不转换也不耗电。BlockHarvestEvents 接入品质+1，空电/损坏/堆叠工具不能正常采掘。

## 验证

主体 compileJava 通过。新增 ElectricChainsawHarvestTests 三项：实际生存叶/普通冰/浮冰掉落且不留水、逐次耗电；原木实际破坏与空电/堆叠/错误目标；Forge 事件取消与创造模式无掉落无耗电。

首轮 build/gametest-batch68-chainsaw-harvest 在 defaultBatch:9 的地牢测试挂起；线程转储 build/thread_batch68_stall.txt 证明服务端 snapshot 等待区块，而光照 Worker-Main-6 经 ItemPipeBlock.shapeFor 查询邻居再等待主线程，形成循环等待。这不是电锯测试失败或通过。已停止该运行并确认服务端进程退出后修改代码。

额外修复 ItemPipeBlock/FluidPipeBlock.propagatesSkylightDown：管径小于整格且无水时透光，避免默认 Block 方法调用依赖邻居的动态形状；保留原 noOcclusion 下 0/1 遮光语义、碰撞与选框。新增 PipeLightThreadSafetyTests：对全部注册两族管道、64 连接组合、干湿状态调用真实 state 光照入口，传入任何邻居读取都会抛错的 BlockGetter，验证不会访问区块且数值等价。该回归覆盖等待栈上的实际入口，原版默认方法在连接臂状态会读取邻居并触发断言。

在 build/gametest-batch68-chainsaw-light-safe 启动后，同族审查发现 ElectricWireBlock/AxleBlock 有同样路径，主动停止并确认退出后将修复和回归扩展到电线/传动轴（半径小于 8 像素）。最终新世界 build/gametest-batch68-chainsaw-all-light-safe，日志 build/gametest_batch68_chainsaw_all_light_safe.log；该轮已越过此前死锁的地牢测试，但发现两个失败：Forge 默认 IForgeShearable.onSheared 返回空，原版叶子需走剪刀战利品；以及旧电动点击测试在 Long.MAX_VALUE 损耗触发概率破损时强转空栈失败。已分别修复：默认接口走保留附魔的剪刀战利品，真正重写接口的模组保留原 onSheared 行为；旧断言同时接受电量耗尽或工具已破损，两者都禁止下一次操作。

最终新世界 build/gametest-batch68-chainsaw-final，该轮启动后复核出原版 int×float 硬度乘法与 double×float 的边界差异，主动停止后统一 mineBlock 使用 50F×hardness，再向上取整。树叶断言改为原版明确的 10 EU，冰为 25 EU，不再复制实现算式。

最终新世界 build/gametest-batch68-chainsaw-verified，日志 build/gametest_batch68_chainsaw_verified.log：14:47:37 All 1033 required tests passed；14:48:08 正常退出，退出码 0，BUILD SUCCESSFUL（8m47s）。新增四项测试全部通过，旧地牢快照测试不再在本轮死锁。已刷新 docs/porting-progress.json。运行期间 Java/资源冻结，未构建 jar。

## 明确剩余

尚未补齐父类斧的非潜行同列连伐、树高速度调整、GT 梁两倍速度、枯灌木额外死木棍规则、树苗/工作台放置、攻击与配方容器替代。因此这是电锯采掘与特殊掉落批次，不能称完整电锯已还原。下一批应继续父类连伐与速度，不重复添加本批叶冰转换。ToolStats.harvestStick 实际针对枯灌木/旧枯草等，不是树叶；原版范围 1+nextInt(2+fortune)。树苗行为只允许顶面，背包逆序寻找 treeSapling，首个匹配放置失败即返回；工作台也逆序扫描但拒绝木材/植物目标。两者均不耗工具电量，创造恢复材料数量，应复用现代物品放置接口及保护检查。

客户端测试：使用充电电锯砍原木，剪下树叶和冰，核对扣电；空电停止正常挖掘，创造不产生这些掉落。另在区块边缘连接不同粗细的物品管/流体管/电线/传动轴，重进世界并触发照明更新，确认无加载卡死且碰撞和连接臂外观仍正常。服务器确定性光照回归能证明本条邻居读取被去除，不能代替所有客户端渲染验收。大型坩埚、板锭/硬币/书架等客户端验收仍待实机验证。完整目标未完成。

没有执行 git commit。
