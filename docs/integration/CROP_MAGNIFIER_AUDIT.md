# 作物分析与覆盖板放大镜交互核查

日期：2026-10-19；基线 `ce8839255`。原版来源 `C:/Dev/gregtech6-master/gregtech6-master/src/main/java`，Gregorius Techneticies / GregTech-6 Team，LGPL 文件只读，已有适配版权头保留。

本批核对作物分析条件及两个放大镜／工具交互缺口；不是全部工具、每个覆盖板、外部作物提供者或真实工具损耗的完整验收。

## 作物分析已有的合同

原版 Behavior_Cropnalyzer 仅在 IC2 已加载且目标为 ICropTile 时生效。它不是原版小麦、所有 GT 浆果或所有农作物的通用扫描器，因此未支持这些普通植物本身不能直接记成遗漏。

当前有两条可用源码入口：附属模组实现纯 Java CropScanSource，或反射匹配可选 ic2.api.crops.ICropTile。后者按公开 API 返回类型取得作物卡的方法，避免非 public 实现类造成反射可见性问题。没有启动一个真实外部模组提供者，不能把反射方法存在当作所有现代作物模组都已兼容。

- 首次 scanLevel<4 收 32768 EU，重复扫描收 512，注册与纯规则常量对应原版。
- 原版先将真实作物分析等级提升至 4，再调用扫描仪 useEnergy。当前同样先改提供者、后扣能量；缺电也能提升分析等级是原版已有的顺序，未误报成新漏洞。
- 当前返回名称、growth／gain／resistance、肥料／水／除草剂、营养／湿度／空气、属性和发现者，字段及顺序对应原版格式。
- 便携扫描仪／调试扫描仪先尝试作物分析，再进入普通诊断；单独分析器在不支持目标时返回未处理。
- 空作物当前安全返回无结果；原版相关分支直接访问作物卡，异常安全不完全相同，记录为现代适配，不凭此创造无意义的空卡扫描。
- 已有 CoreBehaviorContracts 使用附属作物提供者检查首次／重复费用、真实等级提升和完整文本；本批阅读其覆盖范围，没有重新执行整组核心测试。

## ALIGN-125：通用放大镜路径吞掉覆盖板专属诊断

原版放大镜通过 Behavior_Tool 调用目标 onToolClick，将覆盖板自己的聊天信息发回玩家。CoverFilterItem 会说明黑／白名单、空过滤、目标注册 ID／元数据及通配状态；CoverFilterFluid 会报告过滤的实际流体。

当前 ComponentCoverEvents 在最高优先级依次调用物流、组件、工具类覆盖板、普通面板交互。普通过滤器不会在前三者消耗放大镜，进入 PanelCoverInteraction 后，只要有覆盖板就输出通用“正常／反向”，有需要时再输出“强／弱”，然后返回成功。事件随即取消，下面的方块／物品回调没有机会补充过滤目标信息。

因此这不是只改提示措辞能解决的问题：实际目标物品、流体和 wildcard 数据没有读取，也没有交回覆盖板专属描述。此处尚未完整核查其他覆盖板所有信息，只确认物品／流体过滤器这两个明确来源。

机械臂是例外：ComponentCoverInteraction 先处理它的放大镜，已经报告源／目标槽号，原版 CoverRobotArm 也有这条诊断；物流覆盖板还有自己的 describe。本项不能写成所有放大镜诊断全无。

## ALIGN-126：通用面板工具损耗没有遵守原版返回成本

原版 GT_Tool_MagnifyingGlass 与螺丝刀都注册 mDamage=100 的 Behavior_Tool。目标返回正成本后，统一按 `ceil(return × 100 / 10000)` 计算手工具损耗，工具本身的耐久单位是材料耐久乘 100。

当前 GTToolHelper 已有 damageForToolClickReturn／ManualToolRules.clickDamage 这一换算；但通用 PanelCoverInteraction 的放大镜分支完全不扣损耗，配置分支统一调用 damageForUse(held,1)。GT 工具的 hurt 最终直接把 amount 加进工具损伤，没有隐藏的乘 100 补偿。

| 普通 GT 手工具操作 | 原版目标返回 | 原版换算损耗 | 当前通用路径 |
| --- | --- | --- | --- |
| 放大镜查看过滤器 | 1 | 1 | 0 |
| 螺丝刀反转检测器 | 1000 | 10 | 1 |
| 螺丝刀切换按钮复位模式 | 10000 | 100 | 1 |

这些是源码单位及路径比较，不是拿真实工具连续点击后的耐久测试；创作模式、其他模组工具、电动工具与附魔影响另需各自核查。不能把 damageForUse 的 amount 在全局乘 100，因为有些调用已经传入原版单位。

机械臂组件自己按操作选择不同 cost：放大镜 1、调槽 2、方向切换 10，和对应原版返回成本相符。此处的正例进一步说明需要按具体路径对齐，不能一概改动所有覆盖板工具消耗。

## 代码标准化与验证

OriginalCropScan 反射实现中的局部 a／c 改为 cropAccess／cardAccess，反射方法名、公共附属 API 和行为不变；展开 ComponentCoverEvents 的处理与取消顺序，以及 PanelCoverInteraction 的放大镜消息分支，在不符成本的位置注明实际边界。

相对基线，三份生产文件归一两处私有局部重命名后代码／字面量等价；两个原生文件分别投影两版，共五份比较通过。Java 17 仅编译 CropScanSource、ScannerEnergyRules、OriginalCropScan，确认改名后的纯适配类可编译，没有执行模拟或整组测试。

未原生编译、运行外部 IC2 提供者、GameTest、游戏损耗测试、保存重启、构建或推送。原版与当前路径哈希见 [回执](verification/crop-magnifier-audit-20261019.json)。

继续通用工具行为、其他专属交互／声音、权限和所有宿主诊断，同时保留全面审计队列。
