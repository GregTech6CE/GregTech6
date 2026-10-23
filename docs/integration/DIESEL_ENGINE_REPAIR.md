# 燃油引擎本轮修复（2026-10-23）

用户实测指出功能、贴图和 Jade 不正确。本轮保持已有八种 `engine_diesel_*` 注册、配方和额定 RU，修正实际行为与显示。上一轮仅核实注册存在，不能作为行为验收。

原版依据为只读参考树 `gregtech6-master/gregtech6-master/src/main/java/gregtech/tileentity/energy/generators/MultiTileEntityMotorLiquid.java`，以及 `gregapi/tileentity/behavior/TE_Behavior_Active_Trinary.java`。原版输出固定在正面、自动排液在背面，各侧接受燃料并提供排出流体；燃料每周期完整记账，一 tick 尝试一个额定 RU 包，即使没有接收器也会消耗。停止禁止继续燃烧，但不抹除已经记账的能量。

本轮修改：

- 修正原先活塞引擎模型／碰撞形状，使用原版液体马达的完整方块、材质着色底层、无着色覆盖层及活动动画。八种材质、六朝向和已有含水状态均引用正确活动模型；去掉原版没有的工作发光。
- RU 固定正面输出，旧存档面掩码不再将输出移往其他面。移除燃油引擎误用的螺丝刀轮换输出面／软锤重置面逻辑。
- 背面排出按接收者实际接受量扣除；气体判断改用 GT 流体气态属性，不再误用“比空气轻”。有碰撞的方块阻挡排气，无碰撞空间允许气体排放。NeoForge 同时支持无方块实体的流体能力目标。
- 接入独立漏斗／龙头访问：漏斗填燃料；龙头先取排出槽，空后允许回收燃料。普通自动排液仍只取排出槽。补马桶搋清槽与放大镜读数。
- 按原版记录燃烧活动和 64 tick 活动历史，切换工作贴图；不足一批的有效残液按原版活动历史清除。保留原有每周期能量、排出槽容量和停机后余能输出语义。
- Jade 专门显示额定／当前 RU、剩余已燃燃料能量、燃料与排出流体名称／容量、实际输出方向和开关状态，不再显示错误的 KU 或虚构的 0 容量。
- 同包包含此前用户要求的隐藏内部模型资源包修改。

贴图来源为保留的参考移植树 `C:/Dev/gregtech6_w-main/gregtech6_w-main/src/main/resources/assets/gregtech/textures/blocks/machines/generators/motor_liquid/`，沿用原版 GT6 默认资源及项目既有 CC0 来源声明，不重新授权其他参考移植代码。9 个 PNG 和 2 个动画元数据逐字节复制；来源路径及 SHA256 记录于[回执](verification/diesel-repair-20261023.json)。本轮没有复制该参考移植的 Java 实现。

验证：NeoForge 专用两项原生 GameTest 通过，涵盖六朝向、真实柴油周期、余能单包消耗、固体挡气／二氧化碳排放、漏斗／龙头优先级及 Jade 服务端数据。Forge 与 NeoForge 打包成功；包内检查了八组方块状态、双层模型、动画资源、翻译、CRC 和测试资源排除。未运行客户端视觉验收、Forge 游戏内用例或存档重启。构建日志为 `work/diesel-repair-20261023/verify-build-resume.log`。

交付目录：`build/deliveries/20261023-diesel-repair/`。本轮修改尚未上传 GitHub；整体 GT6 对齐工作仍未完成。
