# 物流桶与共享储液规则核查

日期：2026-10-19；基线 `6bf967c93`。原版根目录 `C:/Dev/gregtech6-master/gregtech6-master/src/main/java`。主要来源为 `MultiTileEntityBarrelLogistics`、`TileEntityBase08Barrel`、`FluidTankGT` 和 `Loader_MultiTileEntities` 的 32072 注册行；原作者 GregTech-6 Team / Gregorius Techneticies，原始 LGPL 文件只读，哈希见回执。

本轮核对物流桶规格、过滤记忆、软锤操作及储液底层的容量／溢出合同。不是物流核心全部路由、所有覆盖板、所有容器或存档兼容性的完整验收。

## 已有路径

- 原版 32072 的材料 ANY.W、容量 1000000、四种防护、显式温限 100000 K、硬度 1、抗爆 10 与当前 LogisticsTankSpec 中的 Tungsten 规格对应。材料本身最终属性及 ANY.W 所有替代关系不在这条结论内。
- 两版注册都读取此共享规格；方块使用独立 LogisticsTankBlockEntity，服务端 ticker 调用 TankBlockEntity.serverTick，Neo 启动入口明确调用 GTLogisticsTank.initialize。
- 原版 keepsFilter=true；现代构造器使用 setKeepFilterOnEmpty(true)，而没有误用当前会禁止抽取的 setPreventDraining。FluidTankGT 以单独的长整数记录液量，保留非空的流体身份副本。完全抽空后可以继续拒绝别种流体。
- 原版优先级依赖是否有流体身份，而不是剩余量是否大于零。当前 fluidStoragePriority 使用 getFluidLong，未填过为 1，记住类型后为 2；fluidStorageFilter 返回该类型。LogisticsFluidRouter.collect 会将直接存储宿主加入储存端列表，读取无方向的流体能力；不是只有接口名而无调用。
- saveAdditional 保存罐和物流覆盖板。FluidTankGT.writeToNBT 分别保存 Amount 与 Fluid，saveItemData 的稀疏化条件检查身份是否为空，因此抽空后的记忆不会仅因液量为零被主动删除。此为源码链，未运行两版保存重启测试。
- 现有物流桶配方形状 TQT/wFd/TMT 对应原版注册行，包含钨螺丝、通用存储总线、最低档场发生器和钨桶。这里只核对定义，不宣称所有材料标签及游戏配方匹配已实测。

## ALIGN-092：物流桶空桶软锤清除过滤失效

原版 canBeSealed=false 只限制非空桶的封口切换。父类 onToolClick2 先判断 mTank.amount() <= 0：清空流体身份、清发酵进度并清除自动排液位，然后才在非空分支检查 canBeSealed。

当前 LogisticsTankBlockEntity 覆盖 toggleSoftHammerState 为整个空方法。TankBlock 的软锤交互确实调用该方法，两版还会播放声音、消耗工具耐久，但空桶流体身份和自动输出状态均不改变。

所以“不能封口”已有实现，“抽空后用软锤改换记忆种类”没有。普通排液保留类型是正确行为，不能通过取消 keepFilterOnEmpty 来掩盖清除操作的缺失。原版柱塞通过普通移除保留过滤身份，不能把柱塞当作原版清除过滤的替代入口。

## ALIGN-093：共享容量查询和缩容改变已有内容

原版 FluidTankGT.setCapacity(long) 只在参数非负时更新名义容量，不改液量；没有可调容量表时，capacity() 返回名义容量，即使已有内容超过该容量。只有非空键进入可调容量表分支时才以已有量作下限。

当前 LongFluidStorage.setCapacity 会把已有量截到新容量，并把负容量钳制为零；capacity(null) 即使没有可调表也返回 max(amount, capacity)。两项差异要分开看：

| 条件 | 原版源码结果 | 当前 Java 17 观察 |
| --- | --- | --- |
| 1000 容量内有 900，setCapacity(500) | 保留 900，报告容量 500 | 量变 500，容量 500 |
| restore 1200 液量、1000 容量，无可调表 | 保留 1200，报告容量 1000 | 保留 1200，报告容量 1200 |

当前多方块储罐的 MultiblockTankData.setCapacity 已有保存／恢复原液量的补偿，因此不能把所有缩容调用都说成丢液。EnergyNodeBlockEntity 等加载路径则直接调用 setCapacity，存在超额存档被截的条件。是否可由特定正常玩法生成这种存档，仍需对应机器验证。当前 restore 本身没有截量，问题不能归因于所有 NBT 读取。

## ALIGN-094：溢出销毁只在部分入口生效，接收量不一致

原版启用 mVoidExcess 后，add 和 fill（含模拟）都报告消耗完整的同种流体输入，只保留罐容以内的液体。

当前 LongFluidStorage.add 只在剩余空间 <= 0 时报告完整输入；尚有少量空间时返回实际存入量。例如容量 1000、已有 900，输入 200：原版返回 200 并存至 1000，当前返回 100 并存至 1000；再输入 200 时当前才返回 200。底层观察已复现这两个分支。

当前原生 FluidTankGT.fill 直接算空间并更新 storage，完全没有读取 voidExcess 标志；仅调用 setVoidExcess 不能让此入口销毁溢出。本轮搜索维护中的生产 Java，未找到启用 setVoidExcess 的业务调用，因此将其记录为已确认的接口差异，不宣称当前物流桶正在销毁错误数量，也不宣称所有机器会实际触发。

## 继承差异和未验范围

物流桶继续使用普通桶的 tick／能力路径，自动输出方向与限额、导能流体过滤等问题沿用 [普通桶报告](BARREL_AUDIT.md) 的 ALIGN-087／089。其四防护均为 true，不能把普通木桶的不防魔法损坏示例直接套用。普通物流桶不经软锤开启封口，因此普通桶发酵重载缺陷不作为物流桶正常玩法缺陷重复列项。

需要继续核查的内容包括：物流核心路由优先级与传输时序、可调容量表的键身份、FluidStack 标签／components 语义、物品态流体能力、物流覆盖板同步与权限、完整存档重启和空过滤清除的游戏交互。现有 Forge LogisticsTankTests 包含抽空／保存／网络场景，但本轮未执行；它也没有验证软锤清除已学习的过滤类型。

## 代码标准化与验证边界

展开 LogisticsTankSpec、Neo GTLogisticsTank、共享 LogisticsTankBlockEntity 和 LongFluidStorage 的压行代码；明确原版过滤记忆与当前禁止抽取标志的不同，并修正软锤注释。四份文件代码及字面量不变，共享实体分别展开 Forge／Neo 分支核对。

Java 17 仅编译 LongFluidStorage 与 LongFluidStorageAudit，记录 11 项当前计算结果／策略标志。纯 core 的过滤标志观察不等于验证原生流体身份或 NBT。未原生编译、全量测试、游戏、存档重启、构建或推送。见 [来源和验证回执](verification/logistics-tank-audit-20261019.json)。
