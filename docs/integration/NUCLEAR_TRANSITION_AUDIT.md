# 核反应堆状态连续性审计

第六十一批，2026-10-21。观察基线b11081f4f，可读性提交f252bcf5d后复核。登记ALIGN-211–213三项条件合同，其中211明确保留当前更保守的停止／取出清缓存政策，不作为应恢复旧残余通量的缺陷。没有修改玩法；真实启停、耗尽和保存重启尚未运行验收。

原版根 `C:/Dev/gregtech6-master/gregtech6-master/src/main/java` 始终只读。下文当前共享根为 `C:/Dev/GregTech6/multiversion/src/main/java/com/gregtech/gregtech`。结论是具体状态转换的源码合同，不是实际产线或存档重启通过声明。

## 实际状态入口与排除重复

- 原 `MultiTileEntityReactorCore.java:185–190,317–320` 软锤和 on/off 接口只修改 `mStopped`，模式接口修改 `mMode`；原 2x2 实际实现 `ITileEntitySwitchableMode`，1x1 没有这个接口。模式位、全核心停止均参与发射、接收和反应门控。
- 当前两原生版本都使用共享 `ReactorInteraction.java:15–16` → `setStopped`；共用 MachineControl 由核心 `:47–49` 接入 `setStopped/setDisabledSlots`，`MachineControls.setEnabled/setMode` 实际提交，覆盖板控制/选择器在 `PanelCoverRuntime.java:149–164` 消费。两方块使用相同核心父类、服务端 ticker 和 ReactorNetwork 两端后置事件，不是只存在 helper。
- 当前 `setStopped(true):118–122` 立即清全部 `neutrons`；`removeRod:109–115` 清该槽；`setDisabledSlots:129–135` 不立即清中子，只改变槽活动门控。交换 `ReactorNetwork.java:58–81` 在 19 相位提交新周期数组。两平台投影均保留这些实际状态路径，差异仅在数据容器及原生事件/注册边界。
- 既有 ALIGN-206 水平能力、207 冷却与排液顺序、208 运行态消费者、209 委托邻接、210 辐射豁免，以及 095 容量宿主证据不重新编号。安全默认停止、旧格式强制停止和无地形爆炸政策继续保留。
- 第 59 批撤回的“连续运行时 18 相位少一热 tick”不再提出。原 `m` 在 18 被清，但当 tick 实际棒读取的是 `o`，正常连贯周期仍有该 tick 的热量/寿命消耗。本稿的重载反例须明确多了“错过 19 交换”条件，不能删掉该条件再套到正常运行。

## ALIGN-211（T1）：短暂停启立即清掉本周期通量，与原延后清空的合同不同

原 1x1 的 `onServerTickPost(false):84`（2x2 `:108`）每 tick 将 `m` 快照到 `o`。原全核心停止仅阻止棒发射/接收/反应，没有在开关动作中立即清数组；原软锤 `Core:185–186` 和 `setStateOnOff:317` 都证明这一点。18 相位会在反应分派内清 `m`；因此这里必须限定在本周期 19 之后、下一次 18 之前的短暂停启。

当前 `ReactorCoreBlockEntity.setStopped(true):118–122` 当场清 `neutrons`，重新启用不恢复缓存。两个入口都可实际到达：软锤直接调用，MachineControls/覆盖板通过 setEnabled 调用；即使没有覆盖板，手持软锤也足以构成该转换。

有界例：隔离的 U235（目录 9221，self=32）使用 IC2 冷却液（self×4），19 交换后双方均有通量 128；冷却液和热输出空间足够。在 0 相位停止、1 相位重新启用，且都在相应反应前完成操作：

| 状态/消费者 | 原版 | 当前两版本 |
| --- | --- | --- |
| 停止动作后数组 | m/o 仍为 128，原反应门控停止 | neutrons 当场为 0，反应门控停止 |
| 1 相位重新启用并进入反应 | o=m=128，核棒本 tick 增热 128 | 非 19 不重新交换，核棒以 0 通量反应，新增热 0 |
| 下一次正常 19 | 重新传播与提交 | 重新传播与提交 |

本例没有把停机期间还会继续烧棒当作原行为：原反应门控确实不烧；差异发生在再次启用后、下一次交换前。它也不是“完全不能重新启动”。立即清缓存是更保守的当前政策；本稿不建议擅自恢复残余通量，须由后续玩法决策明确是否有意改变这个合同。

相关取放边界：原夹钳 `Core1x1:233–237` / `Core2x2:260–265` 只 `slotKill`，`Base05Inventories.slotKill:96` 不清核心通量；当前夹钳入口调用 `removeRod` 并清该槽。原自动抽出同样经库存方法，而当前 handler 抽出委托 removeRod。若再插棒并提前启用，新棒是否继承余通量会受这个差异影响；本文把它归于取放时缓存保留政策，没有另造一条“自动化必损寿命”结论。

## ALIGN-212（T2）：禁用/停止的慢化棒也被 finishCycle 提交，旧接触计数提前归零

这与普通核燃料的 moderated 标记必须分开：原核燃料 `Nuclear.updateModeration:260–263` 确实由核心在每次 19 对所有现有棒调用。当前对核燃料无条件交换 next 标记已有相应目的，不能据此将所有 inactive 棒一概判错。

但原 Moderator 继承 `MultiTileEntityReactorRodBase.updateModeration:86` 的空实现，自己没有覆写。它的接触计数交换仅在 `Moderator.getReactorRodNeutronReaction:73–78`：19 相位 `oModeration=mModeration; mModeration=0`。核心 1x1 `:198` / 2x2 `:225` 在停止或该槽被禁用时不调用此反应。因此原已取得的旧接触倍数可跨停止/禁用的交换周期保留。

当前 `ReactorNetwork.java:81` 遍历所有 rods 调 `RodPhysics.finishCycle`，不问 `slotActive`。该方法的 MODERATOR 分支直接将 `gt.contacts_next`（期间无接收时为 0）提交为 `gt.contacts` 并删除 next；Forge 修改实际 tag，Neo 使用 `StackCustomData.update` 提交 CUSTOM_DATA，两个版本都发生该修改。反射入口 `RodPhysics.receive` 再读取 `gt.contacts`，是真实消费者。

具体例：2x2 中一根慢化棒已经通过正常接触获得旧 contacts=1，在某次 19 前禁用该槽并跨过该次 19，然后在下次 19 前启用，且届时有正束 A 进入该棒。原第一次反射为 1×A；当前旧 contacts 已在禁用期间被归零，第一次反射为 0×A。双方会对这次接收重新累加下一轮计数，差异不能泛化为慢化棒永久不工作。全核心停止跨 19 对该旧计数也有同样门控差别；不是普通连续运行的慢化公式错误。

## ALIGN-213（T3）：18 相位后保存、漏过 19 后重载，单数组不能表达原 m/o 的分离状态

原 `Core.writeToNBT2:86–97` 分别保存 m 和 o；`readFromNBT2:65–81` 分别恢复。正常 18 结束时可达到 `m=0, o=N>0`，因为当 tick 已先快照 o，然后仅清 m，棒仍按 o 反应。当前两版本 `saveAdditional` 均保存单 `gt.neutrons`；`load/loadAdditional:467–475` 对含当前版本标记的状态恢复它，也恢复保存的 stopped 值，没有额外相位字段。

只有在以下完整条件下才能构成反例：18 反应结束后以活动状态保存并卸载，核心在 19 不参与交换，然后在下轮 0 相位重新加载并恢复 ticking；假设比较时两边相位已对齐，不引入原 SERVER_TIME/当前各 Level getGameTime 的独立绝对时钟取舍。

| 18 后→0 重载 | 原版 | 当前两版本 |
| --- | --- | --- |
| 保存时实际可达值 | m=0、o=N，stopped=false | neutrons=N，stopped=false，current version |
| 重载恢复后、首次后置反应前 | 保留 m=0、o=N | 恢复 neutrons=N |
| 0 相位（非交换相位）进入反应 | 核心先 o=m=0，核棒热 0 | 不 exchange，核棒以 N 反应，热 N |

这是延后恢复时的实际消费者差异，不是缺少整个保存函数。若核心一直 ticking 并参加下一次 19，双方先重新交换再反应，第 59 批的正常连续周期结论仍成立。若在别的相位保存 m=o=N，并在未漏过交换的条件下恢复，此反例也不能直接套用。没有声称所有存档重启都触发，也没有将当前游戏时间持久化本身当作缺陷。

原卸载门控是 `Base01Root.onChunkUnload:377–379` → setDead，`GT_API_Proxy:478–509` 将 dead 移出后置列表；核心读取/普通 tick 可重新注册。当前网络也只收当 tick 队列，移除/卸载不参与。本例不是依靠一个仍在旧队列里偷偷反应的幽灵核心。

### 两平台原生保存/加载实际消费

- Forge 47.4.20 本机 mapped jar 的真实字节码：LevelChunk.getBlockEntityNbtForSaving 在 offset18 调 BlockEntity.saveWithFullMetadata → saveWithoutMetadata 的 offset10 虚调用 saveAdditional；promotePendingBlockEntity offset70 调 loadStatic，后者 `lambda$loadStatic$1` offset2 虚调用 load。当前 BE 覆写正是这些方法。
- Neo 21.1.243 本机合并源码：LevelChunk:416–420 保存实际 BE，:564–580 promote 调 BlockEntity.loadStatic；BlockEntity:100–118 保存调用 saveAdditional，:176–179 载入调用 loadWithComponents，:82–83 后者虚调用 loadAdditional。当前 BE 的 registry-aware 覆写实际可到达，不是仅给方块掉落使用的 restorePersistentState。
- 原 Base04MultiTileEntities:82–107 的世界 readFromNBT 路径融合缺省参数后实际调用 readFromNBT2；:113–122 的世界 writeToNBT 实际调用 writeToNBT2。m/o 不是仅给物品提示的旁路字段。

本机jar/entry SHA256、字节码方法视图身份、源行锚点及平台投影见[统一回执](verification/nuclear-transitions-config-audit-20261021.json)。未运行原生类或核心算法，没有做真实卸载/重载。

## 已有连续性和不登记为新缺口的边界

1. **禁用普通槽：** 原/当前都阻止发射、接收与反应，自动插拔仅对停止/禁用槽开放。当前禁用模式不会像全核心 setStopped 那样立即清通量；若跨 19，该槽新的入流数组为 0，与原 getEmission 对禁用槽清 m 后快照相符。T2 的 Moderator 旧接触倍数是特例，不能把全部禁槽实现说成没有。
2. **模式位持久化：** 当前保存 disabled_slots，恢复按现有槽数掩码；原保存 mMode，2x2 消费 B[slot] 位。当前 1x1 也对 ModeControl 报支持但 slotActive 忽略 mode；原 1x1 没有 SwitchableMode 接口。这一准入范围差异明确保留，本文未追齐原所有选择器的接口筛选，不据“有一个无效 bit”另分新项。
3. **正常耗尽：** 原 Nuclear:204–225 先按本 tick o 增热/减寿命，再换乏棒 metadata 并清旧 NBT；当前 RodPhysics.react 先形成相同 heat、提交 remaining，再返回 GTFuelRods.stack(product)，核心 :157–159 当场接收新 stack。当前 9321 等目录项确为已注册乏棒；下 tick 不再调用核燃料寿命分支。增殖棒也在预算耗尽当 tick 换 Product，下 tick 走 Product 半热；没有发现“耗尽后还永远按旧燃料烧”的证据。
4. **耗尽当 tick 通量：** 双方都保留当前周期通量用于该 tick 已形成的热，之后乏棒本身不产热，但残余通量/运行态/周期辐射仍须按对应消费者解释。不能把核心中子数字尚未归零直接等同仍在消耗乏棒。运行检测差异已属 ALIGN-208。
5. **棒数据不是 Neo 的临时副本：** Forge 使用栈实际 tag；Neo StackCustomData.read 虽返回 copy，但更新方法调用 CustomData.update。life/next moderated/contacts 的实际写入均通过该方法，核心 NBT 又用 registry-aware saveOptional/parseOptional。不能仅凭 read 返回 copy 断言寿命不保存。
6. **新/旧格式：** 当前新核心默认停止；没有 reactor_version 的旧形式恢复时强制停止，不恢复旧单数组，input 超容量有兼容路径。含 current version 的正常保存则恢复 stopped/failed、热量、单中子数组、禁槽、棒、overflow 和两罐。旧 coolant/water 键继续接受。现有安全和兼容政策不因本稿自动取消，真实旧世界仍未验收。
7. **标脏：** 每个实际 react 都 setChanged，控制、插拔、流体变化也标脏；本轮没有证据支持“因为没 setChanged 所以停机状态必丢档”。T3 是保存了怎样的状态及恢复后的消费，不是泛泛的未保存声明。
8. **拆除/故障：** 当前方块物品保留热/流体/出口、强制停止，棒另掉落，故障清棒并停止且不地形爆炸，这些属于第 59 批已记录政策。本轮不建议通过恢复原暂停缓存或任何旧注释重新引入危险破坏。

## 排版身份和未验范围

根代理第六十批仅排版/改有意义参数名 `core/.../RodPhysicsMath.java` 和 `ReactorCoolantRules.java`；初观察 raw 分别 `fb8d9fb6b73267b1945b8718363db3e101f663953a99b2382bf1cac72bb577ac`、`f84eb98d07c17294b7ec03e17180b8b4861c94615b1583243977ce2fc71ddc5c`。已读排版后正文，运算/分支/枚举顺序相同；[统一回执](verification/nuclear-transitions-config-audit-20261021.json)保留最终实际raw及本轮来源，不把它们算玩法修复。核心/网络/栈适配器未由本代理修改。

没有实测启停时点、模式覆盖板、手动/自动换棒、燃料耗尽、真实注册 holder、区块 tickets/卸载/重载、专服重启、旧 NBT 或极端溢出。T1–T3 是限定条件的源码推演；普通连贯周期和相应已有路径均不因此被宣布失效。原/现绝对时钟、simulation distance、非 ticking 邻核与外部实现等政策边界沿用前稿，不扩大成新专题。


根代理重读实际控制／保存／交换入口，重解析U235自中子32及IC2四倍修饰，得到给定隔离通量128；复算正常18仍热128、短暂停启128／0、禁用慢化棒首次反射128／0、漏19重载热0／128。它们是限定条件的状态算术，不是执行过的游戏脚本或独立重启；正常18的误报仍明确排除。
