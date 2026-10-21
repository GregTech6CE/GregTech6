# 核反应堆外部棒与扩展合同审计

第六十三批，2026-10-21。观察c4a95c5c4，随后545294e25只整理工具配方。ALIGN-216记录行为回调接入收窄，217记录实现类路线的外部耗尽产物解析限制；不要求恢复旧1.7.10类型或二进制兼容，也未证明某个现存addon损坏。

原版根 `C:/Dev/gregtech6-master/gregtech6-master/src/main/java` 只读；当前根 `C:/Dev/GregTech6`。本稿区分：①原版语义上的外部接入合同；②现代支持的独立 addon SDK；③依赖当前实现类的公开 Java 构造器/继承路线；④反射、Mixin、替换核心或旧 1.7.10 二进制兼容。最后一类没有据旧类名消失而登记为缺陷，也不声称任一现存 addon 已损坏。

## 结论

确认原“普通 Item 实现棒行为接口”的任意回调合同，收窄为当前 `FuelRodItem` + 八种固定 Kind 的参数模型。当前并非完全不能添加新棒：实现类公开构造器可接任意 `Rod`，核心不要求该记录已在 46 条 ALL 中；外部自行做原生注册可以进入既有公式。但没有与原行为回调相当的受支持 SDK 接口。另该实现类路线的新棒耗尽产物仍经封闭原目录查找，不能仅靠外部原生注册把新的产物加入解析。

新冷却剂没有单独发现“原本公开可注册、当前丢失”的证据：原默认核心已经硬编码 11 种准入及冷却分支，当前也封闭；注册一种新 Fluid 不等于它获得反应堆冷却合同。原委托邻接收窄已由 ALIGN-209 记录，不再另列；当前物品/流体能力转发确实存在，不能把它们也报成没有。

## 原 IItemReactorRod 是实际被调用的行为接口

原 `gregapi/item/IItemReactorRod.java:29–48` 有按栈准入 `isReactorRod(stack)`、慢化查询/交换、每周期发射、每 tick 反应（返回运行态）、接收/反射、最大通量和两种纹理回调。运行回调获得实际核心、槽下标和实际栈，核心又公开 mEnergy、m/oNeutronCounts、mTanks 及继承的 slot 读写。它不是只供 tooltip 使用的空接口。

实际入口包括：

| 原消费者 | 合同 |
| --- | --- |
| Core1x1:255–260 / Core2x2:283–289 手动插入 | 物品只需实现 IItemReactorRod 且按栈 isReactorRod 返回 true，不要求是 GT 自己的某个 Item 子类 |
| Core1x1:353 / Core2x2:415 自动插入 | 同一接口准入，再叠加停止/禁槽与空槽条件 |
| Core1x1:189–223 / Core2x2:215–250 | 实际发射、反应、反射与慢化都虚调用物品的接口方法 |
| Core1x1:298–300 / Core2x2:333–359 | 原核心显示调用物品的纹理回调 |
| MultiTileEntityItemInternal:449–502 | GT 自己的通用 Item 通过新建带栈 NBT 的 MTE container，将全部棒接口行为委托给具体棒类 |

原 `MultiTileEntityRegistry.add:148–216` 也公开接受 TileEntity class/container，`getNewTileEntityContainer` 初始化实际类和栈数据；GT 自己的棒并非靠一个封闭 enum 才能得到回调。`Base05Inventories.slot(int,ItemStack):91` 为公开槽写入，接口实现可以在反应中变更产物，而不受内置 46 条产物目录限制。

在所读原快照中，实际实现路径是 GT 的 `MultiTileEntityItemInternal` 和 `MultiTileEntityReactorRodBase`（具体棒继承后者）。没有找到独立第三方 addon 实现；以上证明接口能力与实际消费者开放，不是证明某个现存外部模组已经利用全部回调。

## ALIGN-216（E1）：当前支持参数棒扩展，但没有等价的外部行为回调

当前共享 `RodPhysics.definition:12` 仅识别 `stack.getItem() instanceof FuelRodItem`，再虚调用无栈参数 `definition()`。`ReactorCoreBlockEntity.isFuelRod:67–68`、手动/自动插入都走该判定。`ReactorNetwork` 的 self/emission/receive/finishCycle 与核心 `react` 调用的都是 RodPhysics 静态方法；后者及 `RodPhysicsMath` 按固定 Kind 分支，没有查询任意 Item 的行为 provider、能力或回调注册表。

原可由一个普通 Item 的回调按该栈 NBT、具体槽、核心环境修改接收/反应及产物；当前仅给普通 Item 增加相同语义的现代方法并不会被调用，准入先返回 null。即使继承 FuelRodItem，新加的同名行为方法也不会被 RodPhysics 静态分派消费；现有 `gt.reactor_life`、moderated、contacts 等固定状态仍正常由当前公式读取，不能因此报成完全不支持栈状态。

有限能力反例：原接口实现可以在 reaction 中按自有栈标记，在“热量 N”和“热量 3N”之间切换，并显式返回运行态。当前 Rod 记录仅有 kind/数值，没有 reaction callback；既有八 Kind 的热量为 N、2N、floor(N/2) 或 0，没有这个按自有标记的行为消费者。这里没有写/运行一个外部模组，也不声称任何额外类名应原样复制到现代版本；差异是可接入语义收窄。

必须保留的反例/已有入口：`FuelRodItem.java:27,50–58` 类非 final、构造器公开，并直接保存传入 Rod；`ReactorRodCatalog.java:6` 的 Rod 是公开 record。若外部依赖完整实现、自行注册一个 FuelRodItem 并传有效记录，核心定义非 null 即接受，未用 `ALL.contains` 卡住。可使用现有 8 Kind、新 self/emission/life 等参数和既有冷却剂。它不自动获得注册、模型、配方、材料形态或稳定 API 保证；`getDescriptionId:54–56` 默认还使用 gt6ce 语言命名空间，外部可以继承覆写。不能概括成“新增第 47 种棒一定被拒绝”。

现代核心扩展也有实现类路线：核心父类非 final，有接 BlockEntityType/count 的 protected 构造器，网络 enqueue 接受其子类。外部若自己注册方块/BE/ticker 并排队，可参与当前一槽/四槽网络。原邻核检查同样要求 MultiTileEntityReactorCore 子类；“不能把任意普通 BE 当核核心”不是这次新增的缺口。这不等于公开 SDK 已承诺此继承 API，也不保证任意槽数都适配当前 2×2 布局。

## ALIGN-217（E2）：实现类自定义棒的耗尽产物仍受封闭目录限制

`ReactorRodCatalog.ALL:7–53` 是 List.of 的固定 46 条，`byOriginal:54` 只在此表查询，没有公开 append/register 方法。`GTFuelRods` 的注册方法私有，内置注册循环只遍历 ALL。它的 `stack(originalId):76–84` 先查 byOriginal，查无记录立即返回 EMPTY；命中后才在实际原生 Item registry 查询 `gt6ce:<definition.id>`，不接受外部 namespace 或外部 Item 句柄。

`RodPhysics.react:78` 在 Nuclear/Breeder remaining==0 且 product!=0 时，无条件把 GTFuelRods.stack(product) 作为结果；核心 `:157–159` 立即接受该 stack。因此，假设外部已经自行注册有效 FuelRodItem A 和外部乏棒 B，A的Rod.kind=NUCLEAR、life=100、product=10001（不在ALL），非慢化且flux=100／maximum≥100时一次反应损耗100：remaining=0 → byOriginal(10001)=null → Reaction.stack=EMPTY → 核心槽清空。B 即使已由外部正确原生注册，也没有被查询。该推演不需要错误棒或坏档，但要求外部选择了这个实现类路线；并未运行注册表或实际反应。

若 A 的 product 指向现有原 ID（例如 9321），当前路径可正常产生该内置乏棒；46 条内置目录的正常耗尽仍已有对应，不由此判成全坏。若 product=0，则不进入换栈分支，不能将它谎称为外部产物已支持。外部使用 Mixin/替换核心/改源，或自己实现整套核执行器，不在本文判定的公开扩展合同中。

与原比较仅主张行为接口可在 reaction 中直接写入自有产物，没有这个目录强制查找；没有虚构原 46 目录里原本存在 10001，也没有把任意 1.7.10 物品 metadata 当作现代注册 ID。

## 支持的 SDK 与实现类 public 不能混为一层

`core/build.gradle:9–12,56–64` 的独立 API source set 只取 `core/src/api/java`；`gradle/addon-sdk.gradle:5–18,25–35` 的 native SDK 只装 core API 和 native API 输出，并明确排除实现类泄漏。FuelRodItem、RodPhysics、ReactorRodCatalog/ReactorCoolants 和核心实体均在 main，而不在这两个 API 源集中。

本轮检查 core/src/api/java 和 multiversion/src/api/java 的公开源库存；现有 Contributor 提供材料、形态、前缀、机器、行为机器、燃料发电机、结构部件及管道，platform API 提供物品/流体查询、能量/工具/配方和行为机上下文，没有核棒/冷却剂/中子委托声明或 provider。一般 BehaviorMachine 是另一类机器的入口，当前核核心没有调用它作棒反应补偿。

`AddonItems.stack` 查询材料形态，`AddonFluids.still` → AddonLoaderServices → GTFluids.stack 是真实现存查询路径；它们不是任意物品 factory 或核冷却规则注册器。没有在本轮构建 SDK，也没有说依赖完整 mod 实现的 Java addon 无法编译；只是这个路线不属于当前独立 SDK 的可见合同。

## 冷却剂：注册 Fluid 与注册反应堆冷却规则不同

原默认核心 `getFluidTankFillable2:255–267` 和 `funnelFill:282–295` 都硬编码 11 种冷液，1x1/2x2 的冷却处理同样写明各身份、热值和产物；IItemReactorRod 本身没有 registerCoolant 方法。因此，当前 `ReactorCoolants.values/of` 和共享 ReactorCoolantRules 的封闭 11 项，不能仅据不能 append enum 就判为丢失一项原公开冷却剂注册 API。

当前 core FluidCatalog.registerDefinition:154–174 可在 definitionsClosed 前接受有效新流体定义；FluidDefinitions 的材料状态也可产生新材料流体，原生两端最终注册/绑定。它们是一般流体内容路径，新增身份仍不会被 ReactorCoolants.of 识别；核心流体 handler 的 isFluidValid 也直接调用这个识别器。这确认当前没有自动把新增流体变成可用核冷却剂，不代表当前连 Fluid 本身都不能扩展。

原外部棒回调可以触及核心公开 tanks/energy，或外部可以派生新核心，故可自行做更多逻辑；但这不是默认核心的通用新冷却剂表注册，不应在本稿把它包装成原已有专门 API。现代完全替换/派生执行器同样可自行处理新冷却剂；未验收任何一个外部实现。

## 委托器与显示的现代适用性

原 Base01Root.getAdjacentTileEntity:210,219–224 经 ITileEntityDelegating.getDelegateTileEntity 返回目标和方向，原核网络实际消费。当前 ReactorNetwork 建立本 tick 核心位置表并直接取相邻位置，没有调用核中子委托 provider；这个具体收窄已是 ALIGN-209，不另造“外部委托器全失效”。当前 CapabilityRelayBlockEntity 对 item/fluid 查询确实转发到目标原生能力，相关工具/容器流通可以存在，它不等于中子位置网络重定向。

原纹理回调使用 1.7.10 的 ITexture，而现代 ReactorRodRenderer:10–13 对真实槽中 ItemStack 调 ItemRenderer.renderStatic。外部原生物品模型可以通过这条实际渲染路径显示；不能因旧 ITexture 方法不存在就声称外部棒都无图。按核心 active 参数切换的旧回调没有由这份源码证明逐项等价，既未添加新缺口，也未验收渲染。

## 来源与未验边界

原 IItemReactorRod 保留 GregTech-6 Team 2021、Gregorius Techneticies、LGPL-3.0-or-later 头；委托接口和各原文件按各自头记录，不统一伪造年份。当前 NOTICE、native-nuclear-integration.json 和 SDK 的 SPDX 继续作为来源证据。本轮没有复制实现进生产、引入依赖或重许可以前资产；[统一回执](verification/nuclear-solar-heat-audit-20261021.json) 保存绝对/相对路径、raw SHA256、源声明/消费者锚点、公开 API 库存及两平台完整投影。

没有创建/编译外部 addon，没有加载 holder 或实际调用新棒、耗尽、委托器、SDK sample、渲染、旧二进制或旧存档。没有核到独立第三方 IItemReactorRod 使用者，不能据此宣称某个真实 addon 崩溃。E1/E2 是可现代等价表达的接口能力/实现类调用边界，不是要求恢复旧 Minecraft 类型、旧类路径或修改既有核安全政策。冷却/相位/故障/队列等既有 202–215 和 095 内容继续以各正式专题为准。


## 本批源码说明纠错

观察后的FuelRodItem仅更正一条Javadoc，去掉“原1x1使用裸材料杆”和未证的enrichment状态归因，改述definition驱动、RodPhysics栈状态及legacy health字段；无definition的兼容物品仍不被当前核心接受。Forge1689／Neo1771项完整执行词法和字面量位置保持，原观察／Git／最终raw身份均在[回执](verification/nuclear-solar-heat-audit-20261021.json)，没有借说明更正改变准入或扩展合同。
