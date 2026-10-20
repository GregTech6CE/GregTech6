# 核反应堆核心运行、冷却与保存合同核查

第 59 批，2026-10-21。确认 ALIGN-206–208 三项差异，并将 Steam 输出容量的具体消费者证据扩展至既有 ALIGN-095。本报告记录源码与本机原生依赖合同，没有修改玩法，也没有运行测试、Gradle、游戏或构建。

行为观察基线为 `67dff604decf8f08df3c09dd12ede04a1158b22f`；核心纯排版后的基线为 `e4867cf1ee33a02cf40ad2061706d06a9d22e007`。原版参考树 `C:/Dev/gregtech6-master/gregtech6-master/src/main/java` 始终只读。原始／当前／原生依赖身份已汇入[统一来源回执](verification/nuclear-contract-audit-20261021.json)；初查底稿留在忽略的`work/nuclear-core-audit-20261021/`，不要求读者依赖该临时目录核对正式结论。

范围为核心冷却/流体/开关与模式/故障/保存的实际调用链。棒目录、棒物理及寿命公式另稿核查；中子交换相位、四向传播与周期辐射归网络专题。本稿不据类名、注释、测试断言或“已有字段写入”代替消费者证据。范围默认没有覆盖板、外部模组拦截或能力替换。

## 来源与排版身份

- 原版核心继承 `MultiTileEntityReactorCore` → `TileEntityBase10FacingDouble` → 有库存/覆盖板的原基类；1x1 与 2x2 分别是一方块的一槽、四槽核心，不能当成缺少外壳的多方块机器。
- 原注册在 `gregtech/loaders/b/Loader_MultiTileEntities.java:733–738`。1x1 的合成受原配置默认关闭影响，注册本身仍存在；2x2 是另一个实际核心注册。
- 当前共享 `GTEnergyNodes.java:47–50,106–121` 注册两核心；`GTBlockEntities.java:53–54,290–298` 分别绑定基类和 `ReactorCore2x2BlockEntity`（后者构造传入 4 槽）。两方块 ticker 分别在 `ReactorCoreBlock.java:58–61`、`ReactorCore2x2Block.java:75–80`，进入共享 `serverTick` → `ReactorNetwork.enqueue` → 实际后置事件调度 `react()`。
- `ReactorCoreBlockEntity.java` 初始磁盘 raw SHA256 为 `7e8963ad5bda51505373466307e8d293d0563a8351c25d7e62bb4d8be03949ce`。基线 Git blob SHA256 为 `06bd7b53a2f9c453f8ecc91f23d07389266d21a0816a96ce6925e8c24bd44e04`；排版后 raw 为 `ef616196f083d493deca21f66563494540f49dba01e98fceeb9c72f094b0c44b`。本文当前行号采用排版后 560 行版本，主文件为 [ReactorCoreBlockEntity.java](C:/Dev/GregTech6/multiversion/src/main/java/com/gregtech/gregtech/blockentity/energy/ReactorCoreBlockEntity.java:26)。
- `work/reactor-core-readability-20261021/proof.json` 记录两端完整执行投影、字面量位置、各自五条注释及全部条件指令保持；本稿未把纯排版当作行为修复。`sources.json` 同时记录最终磁盘身份、初始观察和该证明的身份。

## ALIGN-206：槽自动化丢失原来的上下侧门控

原 1x1 `MultiTileEntityReactorCore1x1.java:352–354` 只向 DOWN/TOP 提供槽，并要求核心停止后自动插入/抽出；2x2 `:414–416` 同样仅开放上下，并允许停掉的单槽。原基类 `TileEntityBase06Covers.java:320–342` 在无覆盖板时实际调用该 sided 方法，而不是忽略它。

当前共享 `ReactorCoreBlockEntity.java:275–287` 保留了有效棒、空槽、单槽上限以及停止/禁用槽门控，但没有侧面参数。Forge `:394–403` 对任意 `Direction` 返回同一 `items`，Neo `:353–355` 也忽略侧面。Neo 的实际注册消费者 `platform/neoforge/machine/BasicMachineRegistries.java:61–64` 为两种核心分别接入 `be.itemCapability(side)`，不是未注册的备用方法。

本机原生依赖证明这会被自动化实际消费：Forge 47.4.20 `VanillaInventoryCodeHooks.java:104–107,199–204,233–247`，以及 Neo 21.1.243 同名原生类 `:98–100,209–225`，均按漏斗朝向计算邻块和相反侧，并查询物品能力。故水平朝向的原版漏斗可以向停止核心/已禁用槽插入有效棒；原版 GT 默认不能从水平侧访问。其它具有该能力的管道也能水平访问。这里没有宣称原版漏斗能在水平方向吸取物品。

全树补偿已查：`ComponentCoverCapabilitiesMixin` 的能力包装，`ComponentCoverAccess.items` 的 shutter/覆盖板/过滤器检查，以及 `ComponentCoverRuntime.allowsItem` 都没有在无覆盖板时恢复反应堆上下侧限制。本项不要求把上下自动化全部关闭；现有停止槽门控是已实现部分。

## ALIGN-207：先冷却、后排热流体，可能把可腾出的空间误判为冷却失效

原普通 tick 的 `MultiTileEntityReactorCore.java:130–142` 先排超过半罐的冷流体，再在 `:139` 推送热流体。原 `GT_API_Proxy.java:478–509` 于服务器 tick END 执行两组后置回调；1x1/2x2 的棒反应与冷却在第二组回调内。因此原默认主链是先普通 tick 排液，再后置反应/冷却。

当前方块普通 ticker 只排队；共享 `ReactorCoreBlockEntity.react():152–167` 在 `:163` 先 `coolHeat()` 并可 `fail()`，之后 `:164` 才 `pushFluids()`。原生两端后置事件最终均走这个顺序；没有找到另一个本 tick 预先排热罐的调用。`pushTank():228–253` 确实会查询已加载相邻接收器的原生流体能力并按实际接受量扣流体，所以这不是“排液方法完全没实现”。

具体有界反例：热罐已有 64000 L 热 IC2 冷却液，冷罐有有效 IC2 冷却液，核心有棒且有至少 20 HU 待冷却，主出口邻接容器已加载并可接受旧热液。原普通 tick 可先排出旧热液，再成功冷却；当前先模拟向满罐加入新热液而失败，清除棒后才把旧热液排走。该例是源码条件推演，未实际搭建复现；不能推广成每次出口满都会误故障。即便确认这一差异，也应保持原版不破坏地形的故障政策。

## ALIGN-208：原 mRunning 派生状态被拆成偏窄的控制检测与偏宽的接触危险

原 1x1 `:84–100`、2x2 `:108–127` 将本周期非零通量或棒 reaction 返回 true 汇总到 `mRunning`；原核心 `:314–316` 的 running passive/possible/active 均返回同一状态。原普通核棒 `MultiTileEntityReactorRodNuclear.java:204–225`、增殖棒 `Breeder:82–92`、吸收棒 `Absorber:45–48`、产物棒 `Product:54–57` 在零通量/零热时仍可返回 true。此处只借用它们的布尔返回合同，具体棒公式另稿负责。

当前 `ReactorCoreBlockEntity.java:47–49` 给 `MachineControls.switchable` 的 running/active 供应器都只有 `lastHeat > 0`。`MachineControls.java:18–19` 直接消费供应器，`PanelCoverRuntime.java:143–155,218–220` 的控制和检测确实读取它们。因此已开启、有效零通量/零热反应棒会在原版报告运行，在当前控制检测中报告未运行；不能把“零热所以肯定原本不运行”当作原合同。

反方向，原接触入口 `MultiTileEntityReactorCore.java:303` 也按 `mRunning` 门控。当前两方块的 `entityInside` 调用 `ReactorHazards.contact`；其 `:30–40` 只检查核心 `!stopped && !failed`，不检查棒、通量或热量。因而已开启的空核心，在活体实际与方块内部重叠时也会执行热伤害/辐射入口，原空核心且零通量不满足原运行态。这是同一缺失运行派生合同的另一表现，与控制检测合并记录于 ALIGN-208。

边界：当前默认核心碰撞形状和具体实体是否能进入并未作为本项完成核验；不能写成正常站在完整核心外就会受伤。creative/铁傀儡/凋灵等辐射豁免沿共享 `ReactorRadiation.apply` 在网络专题单独归档，本文不重复。源中残余通量、停止开关与周期清零相位的详细差异同样交中子网络专题，不由本项擅自修成热量判断。

## 既有 ALIGN-095 的具体宿主扩围：Steam 容量覆写没有被 fill 消费

原核心 `:62` 将热罐 Steam 容量设为 `64000 * STEAM_PER_WATER`，原 `CS.java:242` 的倍率为 160，即 10240000 L。原 `gregapi/fluid/FluidTankGT.java:206–229` 空罐填充使用 `capacity(aFluid)`，已装液体的填充使用 `capacity()`；`:312,333–342` 按实际 Fluid 名称查询容量表，因此首填和后续填充都能消费 Steam 扩容。

当前核心 `:31–38` 看似也覆写 `capacity(FluidStack)` 返回 10240000，但当前共享 `api/fluid/FluidTankGT.fill():221–245` 的空罐和非空分支都调用无参 `capacity()`。其 `:151–157` 无参路径调用 `LongFluidStorage.capacity(key())`，后者 `:50–54` 在没有容量映射时使用 `max(amount, baseCapacity)`。这个匿名罐没有配置映射，因此正常新建/容量内的罐实际只接受 64000 L；流体能力 `getTankCapacity():300–302` 也走无参方法。覆写存在不足以证明扩容生效。

有界条件推演：蒸馏水不少于 401 L，空热罐，storedHeat 为 32080 HU，则本批结果为 401 × 160 = 64160 L Steam。原输出容量允许，当前 `coolHeat():175–179` 模拟填充只能接受 64000 而返回失败，有棒时进入故障。输入不在失败模拟前扣除，这一点正确。加载的历史罐若已有超过 64000 的内容，`max(amount, capacity)` 会保留其已有量；故不要把此项写成所有旧热罐都被截断或恒定硬上限 64000。

根因属于既有 ALIGN-095 空罐填充/输入容量查询合同的具体反应堆宿主扩围，不另编号。它与 ALIGN-207 排液时序是两条独立条件：即使邻居没有能力接受输出，Steam 空罐初次扩容也应能工作；即使修好 Steam 容量，普通热冷却液满罐的顺序差异仍存在。

## 已有实现、正确边界与保留政策

1. 11 种冷却规则的输入/输出身份与热值表已集中在共享 `core/.../content/nuclear/ReactorCoolantRules.java:5–15`，实际 `ReactorCoolants.java:11–31` 被核心 `coolant()/coolHeat()` 消费。不是只定义了表却没调用。表值分别为蒸馏水 80/160 倍、IC2 20、锡 40/热量除 3、钠 30/除 6、半重水 40、重水 50、氚化水 60、LiCl 15、CO2 20、He 30、钍盐 2560000 HU/L，输出与原两核心的冷却分支一致。
2. `GTFluids.still` 两端使用 canonical field lookup；`FluidDefinitions.prepare` 生成材料液体/气体定义，两端原生注册遍历 `RegisteredFluids.all()` 并 `bindFluid`，而 `ReactorCoolants.resolve` 在 Neo 检查 bound、Forge 检查 present，按 Fluid 身份匹配。这里只建立静态注册/解析路径和规则表对应，没有运行初始化去枚举 11 项实际 holder；不据本项宣布完整冷却生存链已通过。
3. `coolHeat():170–183` 按整个批次先模拟，再扣冷液、加热液和减热量；不足一单位返回成功、保留余热。输出堵塞不会先吃掉冷却液。它没有把热量直接输出为免费 HU。空冷罐且本 tick 无新增热保留余热，与原 `:166–168` 不因纯余热单独触发失效的边界相符。
4. 原主/次出口继承 `TileEntityBase10FacingDouble` 的扳手/活动扳手和 NBT，当前 `ReactorPorts` 与两块 `toolInteraction` 进入共用九宫格工具分派；HOT/COLD 分别可选，冷输出仅在不同于热输出且超过半罐时推送。默认朝下本身不是缺功能。
5. 原本没有 GUI，原 tooltip 明示漏斗/龙头访问。当前实际手持入口 `ReactorInteraction` 支持顶部选槽、有效棒只插 1 个并停止核心、夹钳取棒、软锤开关、手持流体容器、放大镜/测量工具路径；因此未实现同名菜单不是缺陷。夹钳手动取棒并非原自动化停槽规则，不能误报原手动也必须停止。
6. 当前自动化流体 handler 填冷罐、只抽热罐；手持 handler 在热罐空时可回收冷液。手持和能力流体入口均实际挂到核心，不是缺少全部流体交互。
7. 故障仍清除棒、播放声音并辐射，没有调用 `level.explode`。原 1x1 `:170–183` 和 2x2 对应位置的地形爆炸调用是休眠注释，而清棒与辐射活动；既有机器风险政策和 `P3_NATIVE_NUCLEAR.md` 都要求保留不破坏地形的边界。不能因 TODO 或注释恢复危险地形爆炸。
8. 当前新核心默认停止、旧版本状态载入强制停止、手动插棒停止、故障 stopped/failed，以及 `setStopped` 立即清缓存，和原活动默认/停止后的相位存在差异。此稿将新核心/旧版载入视为现有安全与兼容政策边界，不自动建议恢复原默认开启；周期缓存差异另由网络专题核查。
9. 当前两端 NBT 保存 `disabled_slots/reactor_version/stopped/failed/heat/last_heat/neutrons/rods/overflow/input/output`。Forge 使用 ItemStack.save/of，Neo 使用 registry-aware saveOptional/parseOptional；旧 `rod` 包装、coolant/water 键及超出当前槽数的棒有恢复路径。运行 `react():166` 每次标脏，流体 onChanged、控制/插取棒的 `changed()` 也实际标脏。原普通保存继承 NeedsSaving 并由 `GT_API_Proxy:682–686` 定期标脏；本项没有养蜂式“纯 tick 字段没有标脏”的证据。
10. 当前拆除时核心方块保留热量/流体/出口并强制停止，棒与 overflow 另掉落，方块物品移除 rod/overflow/neutrons。此为 `P3_NATIVE_NUCLEAR.md` 已记录的当前政策，不在这份原版对照中突然改掉。字段和标脏链存在不等于实际独立存档重启验证已经通过。

## 未验范围

没有实际运行核心、测量冷却/失效、搭建上下或水平漏斗、读取覆盖板输出、做碰撞伤害实验、独立保存重启或旧世界迁移。未重新核完外部覆盖板全部组合、实体几何、材料物理、原生爆炸回调或所有手持工具的完整玩法。棒目录与中子网络分稿不能由本文替代。上述 ALIGN-206–208 和 ALIGN-095 扩围是静态源码及本机原生消费者合同证据，不是新构建或游戏验证通过声明。

## 第六十一批状态连续性续核

短暂停启／取出清缓存已用实际入口登记为ALIGN-211保留安全政策；禁用慢化棒跨周期旧contacts和漏19后的活动重载登记212／213，见[状态报告](NUCLEAR_TRANSITION_AUDIT.md)。耗尽换产物、Neo持久写入和标脏已有路径，正常18仍消费o快照；新反例不得删掉禁槽／漏交换条件。网络未排空与异常分派另见[生命周期](NUCLEAR_NETWORK_LIFECYCLE_AUDIT.md)。原生入口和有限推演见[回执](verification/nuclear-transitions-config-audit-20261021.json)，不是游戏／独立重启验收。
