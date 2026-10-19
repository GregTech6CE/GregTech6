# 显示面板与进度计读数核查

日期：2026-10-19；基线 `71900f689`。原版根目录 `C:/Dev/gregtech6-master/gregtech6-master/src/main/java`，Gregorius Techneticies / GregTech-6 Team，原始 LGPL 源码只读。

本批沿服务端量化、宿主数值、客户端图层与亮度参数核查。没有启动客户端，不宣称已完成暗室效果、所有贴图资源、包同步或所有宿主显示验收。

## ALIGN-112：显示图层丢失原版独立高亮属性

原版 CoverDisplayEnergy 的 0–10 图层以 `BlockTextureDefault.get(path, true)` 创建。该布尔参数是 aGlow：构造器把它传入 aUseMaxBrightness，设置 mBrightness=240、mUseOwnBrightness=true，并关闭 AO；renderSide 使用自己的亮度，而非环境传入的亮度。

机器状态显示的 possible／running／active 三组亮灭指示图层也使用 true；开关指示图层使用 false。因此不是“所有图层都应全亮”，原版按图层区分。

当前 PanelCoverRenderer.layers 只返回字符串路径，没有保留 glow 元数据。renderFace 对所有图层传入同一个 `LevelRenderer.getLightColor(level, front)` 或 fallbackLight；CoverSurfaceRenderer.drawAt 将该 light 直接写入两版顶点，没有针对这些图层改用独立高亮值。

这会使原版独立高亮的显示图层受环境光影响。证据是实际调用参数与原版渲染标志，不是仅因代码里没找到名为 emissive 的方法；仍需暗室客户端截图确认最终观感。第三方光照／资源包附加效果未验。

当前只展开图层定义并注明缺失的元数据，未在本轮修改渲染行为。

## ALIGN-077 修订：进度覆盖板有通用回退，但读到错误的量

此前记录“缺少原版进度接口”仍成立，但若将其理解为“完全不能装进度覆盖板／完全没有读数”，范围过大。本批追踪了 PanelCoverHost.coverControl：优先使用 MachineControl.find，找不到时会调用 CoverProgress.control。

该回退在有物品、流体或储能能力时创建支持 progress 的适配器，按储能、流体、物品的优先级求占用量与容量。因此物品管道、流体管道、普通桶可能接受进度覆盖板，显示的是当前内容物占比。

| 宿主 | 原版进度值／上限 | 当前回退 |
| --- | --- | --- |
| 物品管道 | mTransferredItems × 64／最大传输组数 × 64 | 当前物品数量／库存槽上限 |
| 流体管道 | 本轮 mTransferredAmount／通道总容量 | 当前液量／罐总容量 |
| 普通桶 | mSealedTime／mMaxSealedTime | 当前液量／容量 |

原版物品管道的 getPipeContent 返回 mTransferredItems，名称不能解释成当前缓存库存。流体管道原版另有 getGibblValue 读取占用量；它和 getProgressValue 是不同测量合同。普通桶原版进度是发酵，不是液面。

因此无需再为同一缺口新增编号，修订 ALIGN-077 的描述并保留原版指标未接入的结论。手持进度工具与覆盖板的接口入口也不能混为一谈：本批追踪的是覆盖板回退；此前工具接口检查不因该适配器存在而自动通过。

回退还通过 IFluidHandler 的 int 数量／容量视图读取。FluidTankGT.getFluid／getCapacity 会把内部 long 钳制到 int 范围；这限制了回退对超大罐的表达。当前没有独立重现一个实际长整型读数错误场景，先记录边界，不重复增加缺陷号。

## 分档与状态图层已有的对应

- CoverScaleProgress 与 CoverScaleEnergy 的原版红石量化：无量或无容量为 0，满量为 15，其他正数使用 `14-clamp((capacity-amount)*14/capacity, 0, 13)`。当前 scale(...,15) 的数学分档相同。
- 原版 CoverDisplayEnergy 的视觉为 0–10，非满量采用对应的 9 倍公式；当前 scale(...,10) 对应。当前使用 BigInteger 避免原版 long 乘法溢出，这是保留的算术防护，不能以极大值发生不同结果判为缺少分档。
- 能源传感器和能源显示读取第一个真实储能类型及其 stored／capacity，与原版选取方式对应；不会把发电输出类型自动当作储能类型。电池盒工作缓存与电池内部电量的区分已有对应，详见储能核查。
- 状态显示使用低四位状态与对应的 5–8 位能力标记，客户端按原版 bottom／top 两组贴图选取亮灭图层。扩展器缺少独立 possible 能力标记的问题沿用 ALIGN-111，不重复编号。
- 手动开关点击区域两种布局已有代码；本批没有做各朝向 hitbox 或完整贴图渲染验收。
- 原版能源显示在无任何储能类型时跳过更新；当前主动显示 0，既有测试也要求拔掉模块后归零。它是现代清除过时显示的差异，记录为现有适配行为，未改回原版滞留数值。

## 同步证据与可读性整理

PanelCoverRuntime.setValue 只有值变化时更新覆盖板数据；afterTick 汇总 visualChanged 后调用 changed，后者标记实体并发送方块更新。现有 PanelCoverTests 包含把能源显示 update tag 装入客户端实体的源码场景。本批未执行这些 GameTest，也未独立证明每种宿主、所有包时序和实际画面都正确。

展开 CoverProgress 的状态与容量读取，避免把“内容物占用”误称所有宿主的原版进度；展开 PanelCoverRenderer.layers 的图层选择与状态位；展开 scale 的长整型／大整数计算。三份生产代码相对基线按两版投影，共六份代码／字面量比较一致，行为保持。

未新增模拟测试、原生编译、GameTest、完整测试、游戏、保存重启、构建或推送。来源哈希与核对范围见 [回执](verification/display-meter-audit-20261019.json)。

继续：覆盖板同步及宿主包路径、传感器手持工具与显示计量语义、面板几何，以及完整审计队列。
