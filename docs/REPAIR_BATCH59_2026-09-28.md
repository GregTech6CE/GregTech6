# 第五十九批：能源制作材料替代与电池盒物品闸门

## 本轮实现

- 变压器九条配方的铁双板改用材料组标签；前三级的 1x/4x 铜线接受铜和退火铜，高级变压器仍严格要求退火铜。
- MV 普通和大型电池盒的铜线／电缆位接入同样的 ANY.Cu 材料组，保持 1x 与 4x、裸线与绝缘缆严格分开。共更新 11 条既有配方，不重复新增等价配方。
- MaterialTagPack 从实际线缆注册定义生成分尺寸、分绝缘的 any_copper 标签，从 MaterialGroups.Iron 和现有双板标签生成 any_iron_or_steel。复用动态资源包，不生成成百上千个磁盘 JSON。
- 普通和大型电池盒接受 shutter 覆盖板。六面自动化各有轻量包装，共享原库存，插入／取出／模拟／isItemValid 都服从当前面的闸门；GUI 继续直接使用原库存。
- 缓存过的 IItemHandler 也会在每次操作时检查闸门，装板、螺丝刀反转、红石控制器改变状态、拆板不需要管道重新连接。拆方块时使所有面能力失效。
- 使用现有 PanelCoverRuntime 的配置、渲染、NBT 和控制器规则；不另造一套闸门开关。无覆盖板控制器时普通闸门开、反向闸门关；控制器停止覆盖板时关系反转。

## 原版核对与边界

- ANY.java:119 的 Iron 原版实际有 **11** 项，包含 Enori；当前 MaterialGroups.Iron 只有十个已注册的对应材料。此前第五十八批沿用“十种”描述指的是移植版现状，不是原版完整集合。本轮未伪造 Enori 注册，不能称包括所有兼容材料在内的 ANY.Iron 全部完成。根因已定位：transpile_gt6_materials.py 的 SKIP_FACTORIES 明确跳过 gem_aa 六种材料（原版 MT.java:1569–1574）；MT.java:332 的该工厂绑定 MD.AA 并复制基础材料、输出归一到基础材料。应先按 Actually Additions 兼容条件核对，不能简单去掉所有 skip 后批量重新生成材料。
- ANY.java:124 的 Cu 为 Cu、AnnealedCopper；MT.DATA.CABLES_01/04 和 WIRES_01/04 的 MV 位使用 ANY.Cu。
- CoverShutter 的 interceptItemInsert/Extract 条件为“普通闸门且覆盖板停止，或反向闸门且覆盖板运行”。此停止标志来自覆盖板控制器，不能误用电池盒本身的输出启停标志。
- 本轮针对电池盒物品库存；没有把闸门宣称为已实现所有连接器断开、流体、电力或其它机器行为。
- T7–T9 电路、原版 10049 依赖、变压器活动动画及控制覆盖板、旧通用电池电压归并仍待完成。大型坩埚仍待客户端实测。

## 验证

- 新增 EnergySurvivalGateTests 三项：材料组接受/拒绝正确材料及形态；MV 两类盒精确线缆替代；缓存自动化接口的模拟、关闭、其它面、存档、红石、反转、拆板和能力失效。
- 旧固定库存测试改为验证 GUI 与面包装读取同一个实际栈，不再错误要求包装对象与内部库存对象是同一个实例。
- 6 项既有资源检查通过，build/assets_batch59.log。
- 初次编译发现局部变量 form 重名，已改为 wireForm；随后 ForgeGradle 证书预检临时失败，未进入编译。按原配置重试已恢复，无需修改证书设置；最终 build/gametest_batch59_materials_shutter.log 于 05:28:13 **1008/1008 required tests passed**，退出码 0，BUILD SUCCESSFUL in 9m 33s，05:28:45 正常退出。无 failed! 或 Parsing error，使用新世界 build/gametest-batch59-materials-shutter。门禁期间 Java/资源冻结，最新 Java 时间早于最终日志。
- 已刷新 docs/porting-progress.json，缺失 token 仍为 49；未将注册数当作完整度。

## 实机测试

在 JEI 查看变压器铁双板／铜线位，应该能轮换显示对应材料组；用钢双板、退火铜线试做低档变压器，高档不能用普通铜线替代。MV 两种电池盒也应接受对应粗细的退火铜裸线和电缆。

在电池盒一面贴闸门，先接漏斗或管道，再用螺丝刀切换。关闭面不能自动放入或取出；另一个未遮挡面仍可操作；GUI 手动取放正常。安装覆盖板控制器并切换红石，再保存重进、拆下闸门测试缓存接口是否及时恢复。破坏电池盒的原保电掉落规则应保持。

本轮未构建 jar。没有执行 git commit。整体目标仍在进行中。
