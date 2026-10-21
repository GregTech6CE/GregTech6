# 长距离输电与线路缓存核查

2026-10-21 后续：071的公开附属能源接口已接入，FE-only仍缺；072的两端无条件删块已修，两者不能继续按下文历史正文列为全缺失。新确认烧线邻格尝试287与活动指示门槛288；070缓存恢复缺口仍在，见[生命周期续核](LONG_DISTANCE_LIFECYCLE_AUDIT.md)。旧观察保留其日期和范围。

日期：2026-10-19；基线 `10861dbbd`。本轮确认目录、核心扫描与归属合同，核查原生传输／重载边界。整理四份代码的格式与职责说明，不修改玩法。

## 原版与当前入口

原版 `Loader_MultiTileEntities` 注册 10064–10068 五档长距离变压器，使用 Electric_T[4..8]、V[4..8]，输入输出均为 EU。`Loader_Blocks` 给 LongDistWire01 的十六个 metadata 指定档位 `{4,4,5,6,6,6,6,6,7,7,7,7,8,8,8,8}`，导线容量取 VMAX 而非 V。

当前共享 `GTLongDistance.initialize` 消费 `LongDistanceCatalog.ALL`，构造 `LongDistanceTransformerBlock`／`LongDistPipeBlock`。五档额定值 2048、8192、32768、131072、524288 及对应 Chromium、Titanium、Iridium、Osmium、Trinitanium 与原版对应；十六种线的 sourceMeta／电压档位对应。前三档旧 ID 后缀仍为 ulv／lv／mv，不应以该后缀推断额定电压，本轮没有改存档 ID。没有逐项证明十六种材料的全部配方、着色与物品预览。

原版主执行类是 `gregtech/tileentity/energy/transformers/MultiTileEntityLongDistanceTransformer.java`；当前是共享 `block/misc/LongDistanceTransformerBlockEntity.java`，调用 core 的 LongDistanceNetwork／LongDistanceLink／LongDistanceRules。来源根目录 `C:/Dev/gregtech6-master/gregtech6-master/src/main/java`，哈希见 [回执](verification/long-distance-audit-20261019.json)。

## 已对应的机制与验证范围

- 同种线路身份扫描；相同耐压但不同材料不能直接混接。选择首个朝向正确的接收端，不禁止线路分叉。
- 一个接收端只能被一个存活发送端占用；原持有者失效或目标被清空后可以重新认领。作为接收端时软锤扫描受到限制。
- 无原先任意 4096 格上限。现有纯 core 合同观察了 5000 格线；这不代表游戏内跨区块扫描性能、加载或持久化已经验证。
- 每包损耗 `max(64, distance/8)`：距离 519 仍为 64，520 为 65，5000 为 625。模拟注入只验证路径／电压而不查询最终负载需求，与原版分支一致，不能把这一点当作模拟接口缺陷。
- 原版扫描临时关闭忽略未加载区块；当前扫描读取世界方块也没有统一的 hasChunkAt 截止。两者都不应被描述为“绝不加载区块”。当前缓存持有的目标 live 检查与输出邻居检查则使用 hasChunkAt。
- 原版分层 HashSet 与当前按适配器邻居顺序的队列，等距接收端的选取顺序不保证相同。已有单一候选／简单分叉合同不能证明复杂网络选择完全一致。
- 原版 claim 判断中没有检查 previous.target 是否仍指向本接收端；当前保持了这个条件，不能将相同的边界行为误报成移植新增缺陷。

## ALIGN-070：重载后线路变更观察表为空，旧路径可能继续有效

当前变压器保存目标坐标、distance、throughput，重载时恢复 link 并清空 watched。receiver() 在已有目标且距离／容量大于零时不会调用 scan；目标区块已加载时直接找实体并 bind／claim。此过程没有重新填充 watched。

拆除远处导线时，LongDistPipeBlock.onRemove 会增加 LongDistanceTopology.version；changed() 虽看到版本变化，却只对 watched 执行 anyMatch。空表永远返回 false，因此旧目标仍有效。tickActivity 也不重新验证导线路径。

可复现场景由源码推导：建立可用线路并保存 → 重载发送／接收端且保留有效 distance／throughput → 在端点都加载时拆除中间导线 → 再注入。当前没有路径失效分支阻止旧目标传输。这里只确认控制流缺口，未在游戏世界复现；纯 core 探针仅证明 restore 后 known=true、targetOwner=null，随后可直接 bind／claim，不把它充当原生重启测试。

原版虽然同样保存目标坐标，但导线继承 BlockBaseMachineUpdate，放置／拆除触发机器更新；变压器 onMachineBlockUpdate 收到导线变化会清空 mTargetPos／mSender，下一次检查重新扫描。它不依赖仅存于内存的 watched 表。

共享 `LongDistEndpointBlockEntity` 的物品／流体管道也有相同的 restore → watched.clear → 已知目标直接绑定链，纳入本项影响范围；原版两类管道同样有管道变化时清目标的回调。尚未全面审计管道传输能力本身。

另一个相关边界：changed() 在提高 observedVersion 后跳过未加载的 watched 坐标；这些区块随后加载而没有新的拓扑版本事件时，不会重新比较。此风险仍需区块事件顺序验证，不宣称每次卸载都会出错。

## ALIGN-071：长距离输出绕过已有能源适配入口

原版 doInject 调用 ITileEntityEnergy.Util.emitEnergyToNetwork，最终进入 insertEnergyInto，除原生能源接口外还能进入 EnergyCompat。

当前长距离输出直接判断邻居 `instanceof IEnergyBlock` 并注入，未调用共用 EnergyTransfer。当前项目已存在 EnergyBlockViews（附属 API EnergyBlock → AddonEnergyAdapter）以及 EnergyCompat（Forge／NeoForge FE 能力）的统一入口，所以这不是仅因原版 IC2 API 过时而无法比较的问题。

结果：只实现公开附属 EnergyBlock 或 FE 能力的目标，即使共用能源发送器可识别，长距离接收端也直接返回零。实际第三方模组组合仍待运行验证；本项基于明确类型分支，不能泛化为“所有外部能源都不能用”。

## ALIGN-072：端点超压被简化为直接删除方块

当前发送端超压和扣损后的接收端超压都调用 destroyBlock(pos,false)。原版调用 overcharge：按 OVERCHARGE_EXPLOSIONS／OVERCHARGE_BREAKING 设置选择爆炸、破坏或不破坏，并发送过载声音与日志。输入／输出超限门槛的对应不代表后果一致。

本项记录行为差异，不要求本轮完成用户已延后的配置系统。尤其不能宣称原版一定爆炸：原版配置可关掉两类破坏。当前也没有对齐有声警报和按能量等级确定强度的调用链。导线超限另走燃线扫描，不能与端点超压混为一谈；原版燃线还调用 WD.burn，当前只将已收集线块设为火，此附带效果继续核查。

## 可读性与有限验证

LongDistanceLink 的保存目标、归属与重置方法展开；LongDistanceNetwork 的队列／距离更新展开并说明返回的是抵达目标前访问的线集合；LongDistanceRules 区分现用损耗与旧 helper；原生 LongDistanceTopology 展开。四份改动仅改变空白／注释，代码与字面量相同，原生文件两版投影相同。

Java 17 局部编译四份 core 目录／规则、现有 LongDistanceSourceContracts 与新增观察入口；现有 23 项 core 合同通过，另记录五档端点／十六档导线、七个损耗边界和三个恢复／绑定观察。未使用 Gradle 或 Minecraft，无原生编译、完整世界重启、第三方兼容实测、渲染验证、构建或推送。

现有 activity helper 对 remembered unloaded target 使用简化 unavailable 条件，而原版 onTickCheck 还有目标归属条件；因此现有“未加载目标显示 3”的 core 断言仅证明当前意图，不能作为原版完整视觉等价证据。物品／流体线路实际传输、失效时燃线覆盖范围、轴／可调齿轮箱状态及其余系统继续核查，全面目标未完成。
