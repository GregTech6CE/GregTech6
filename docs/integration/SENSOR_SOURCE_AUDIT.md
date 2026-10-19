# 特殊仪表上限与测重数据来源核查

日期：2026-10-19；基线 `202a57727`。原版来源 `C:/Dev/gregtech6-master/gregtech6-master/src/main/java`，Gregorius Techneticies / GregTech-6 Team，盖革计另署 Erik3003；LGPL 源码只读。

本轮继续原生测量入口，核对可调齿轮箱转速表、中子计的归一化上限、测重与大容量库存视图。未完成反应堆自身核物理、完整材料重量表、光照算法、全部兼容库存与手持工具验收。

## ALIGN-118：可调齿轮箱转速表使用了错误的上限公式

原版 MultiTileEntityTachometer 对轴取 mPower×mSpeed，对可调齿轮箱却取 mMaxThroughPut×16。MultiTileEntityGearBox 从 NBT_INPUT 读 mMaxThroughPut；原版注册使用对应档位的 VMAX。

当前 SensorBlockEntity.TACHOMETER 对轴采用 maxSpeed×maxPower，符合原版轴的结构；但对齿轮箱也套用相同乘法。GearboxCatalog 把 maxSpeed 设为原版额定速度、maxPower 设为 speed/4，因此当前齿轮箱仪表上限变成 speed²/4。

| 可调齿轮箱 | 额定速度 | 原版上限 speed×16 | 当前上限 speed²/4 |
| --- | --- | --- | --- |
| 木 | 16 | 256 | 64 |
| 青铜 | 64 | 1024 | 1024 |
| 钢 | 256 | 4096 | 16384 |
| 钛 | 1024 | 16384 | 262144 |

青铜数值相同会掩盖其他档位的差异。这影响百分比和 FULL 等相对上限模式，原始传输值不因该公式改变。表格为来源参数算术，没有搭建轴网测量；不能据此把轴和齿轮箱全部吞吐行为判作正确。

## ALIGN-119：中子计上限没有读取燃料棒与冷却剂

原版 MultiTileEntityGeigerCounter 最大值逐槽调用 IItemReactorRod.getReactorRodNeutronMaximum，并在 2×2 堆芯中累加四个槽。没有棒时对应项为 0。核燃料棒的最大值还根据氯化锂、钍盐、重水、超重水等冷却剂变化。

当前 GEIGER 读取 reactor.neutronTotal 后，maximum 一律设为 Integer.MAX_VALUE，忽略装了哪种棒、装了几根和冷却剂。当前 ReactorRodCatalog 与 RodPhysics 已有有限的 maximum 及冷却修正概念，仪表却没有接入；这是显示归一化的缺口，不能通过提高燃料棒物理上限来修。

原始中子数量仍有来源，不应写成中子计完全没有功能。原版读 oNeutronCounts，当前读 neutrons；ReactorNetwork 在世界末尾交换并逐核反应。两者的周期快照语义、旧值／当前值是否等价还需另做反应堆时序审计，本轮只确认最大值来源不同，不重复推断所有读数都滞后一轮。

## ALIGN-120：测重误用了坩埚回收合同，且漏读大容量库存

原版四种 Weightometer 先判断宿主是否实现 ITileEntityWeight，直接取真实重量；否则遍历库存调用 OM.weight。OM.weight 按 ItemStack 数量乘 OreDictItemData.getAllMaterialWeights，对有前缀的条目使用前缀 mWeight 和副材料，不执行矿石产量转换。

当前四种测重只查询 IItemHandler，遍历可见栈后调用 CrucibleItemInput.parse，再求材料 kg。这个解析器负责坩埚投料：

- 含容器／实体等存储数据的物品，因 hasStoredContents 提前返回空载荷。
- 通用材料分支还受 canRecover／recoverable 限制；材料前缀的快路径不全经过这项判断，不能把它描述成所有物品均有同一过滤。
- oreRaw／矿石块等会走 getTargetCrushingMaterial、getTargetCrushingAmount 和 oreMultiplier，得到加工载荷，未必是物品本身材料重量。
- 没有相当于 ITileEntityWeight 的独立宿主重量入口。

这是数据源合同不同；各材料、填充容器和附加物品的具体数值仍需进一步盘点。不能因单位都写 kg 就将回收载荷当成物理质量。

大容量存储还有明确的可见数量限制：MassStorageBlockEntity 的 getStackInSlot 调用 extract(Integer.MAX_VALUE,true)，内部再按物品 getMaxStackSize 限制，包装后的栈最多一堆，打包模式还会返回空。WEIGHT 直接称这一个可见栈，没有读取 stored()。原版大存储的真实库存槽能持有完整计数，OM.weight 乘的是该计数。

当前 ITEM 分支已有专门的 MassStorageBlockEntity 修正，直接读 stored() 与 CAPACITY；它不能自动修正 WEIGHT 分支。不要以物品计数器正确作为测重也正确的证据。

CrucibleInputRules 的载荷是每件物品的量，SensorBlockEntity 再乘可见 stack.getCount 是正常的数量展开，本轮没有发现可据此认定的整堆重复乘算。问题在重量来源及视图数量。

## 其他已核路径和待补边界

- 四种重量单位的 g／kg／t／kt 换算和 65535 显示上限与原版各类公式对应；实际测量来源错误不等于换算也错。
- 玩家计数器读取整个服务器在线数／最大人数，与原版范围一致，不应改成附近玩家数。
- 原版物品计和堆叠计排除流体展示物品，当前原生两处分支没有对应排除。它与 ALIGN-114 的槽／堆语义一起留在后续库存细节核查，不把各显示物品兼容问题都单独重复编号。
- 亮度计读取目标位置的亮度；现代光照与原版亮度 API 是否具有同样的天光衰减、光照更新时点和不透明度仍未验。
- 电流计的原版旧 IC2 条件分支不应因现代没有同名 API 就直接算缺失；当前 GT 线缆 mWattageLast 路径已存在，跨模组统计兼容另需定义。

## 标准化与验证

展开 SensorMeasurements 的单位转换；在 CrucibleInputRules 标明每件投料量不等于物理组成；展开原生 TACHOMETER／GEIGER 分支并说明上限差异，在测重循环注明当前数据源。三份生产文件相对基线的代码与字面量不变，原生实体分别投影 Forge／Neo，共四份比较通过。

本批仅做源码和数值来源核对，没有新增 Java 观察、原生编译、完整测试、GameTest、游戏测量、核循环跟踪、保存重启、构建或推送。参数、原作者与来源哈希见 [回执](verification/sensor-source-audit-20261019.json)。

继续传感器特殊库存、手持仪表、材料重量覆盖和反应堆的完整时序，同时保留全面审计队列。
