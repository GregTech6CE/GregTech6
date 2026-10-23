# 双版本初期对齐：机器工具交互和仓储物品边界

参照已有工具、基础机器、仓储 MD 并检查当前代码，确认 GTFacingMachineBlock 仍使用早期简化 shell，而现有完整 GTToolHelper / MachineRotationType / ToolInteractions 已在 Native 可用。现按当前 Forge 全类保留 spec、水平朝向、外壳形状/光、工具声明、采掘/直接收集与速度判定；只适配 Native useItemOn、既有子类 handleWrench、既有rotate/mirror hooks和实际已注册wrench音效，不复制新的工具底层。

基础机器与两类燃烧箱现在继承 ToolInteractionTarget。原水平扳手契约进入既有声明/操作路径，工具磨损、玩家/世界交互许可、电动工具可用性和客户端网格预览使用同一操作描述。以前handleWrench直接按raw100磨损且没有该声明，现在按Source既有工具操作执行。恢复Source canCollectMachineDrop门槛、入背包/溢出掉落与拾取/扳手声音，以及电动扳手能量/目标/品质采掘判定；手动扳手与原版/GT镐速度按Source保留。实际工具动作、许可和预览未运行证明。

BasicMachineBlock掉落组件保留原保存字段和移除库存/盖板语义，新增实际注册的block entity type id。读取固定Neo源码证实BLOCK_ENTITY_DATA持久化采用CustomData.CODEC_WITH_ID；原saveWithoutMetadata结果不含id，这不是凭旧NBT假定可保存。没有改变注册id、gt字段或物质量单位；真实编码/放回/状态恢复与库存/盖板掉落守恒仍未验证。

MassStorageBlock沿用既有真实百万件状态、fraction loose drops、封箱壳与Native组件格式，补缺失Source边界：剪刀/刀拆封调用damageForToolClickReturn(1000)；螺丝刀模式切换经GTToolHelper.damageForUse(1)并显示filter keep/reset消息；空仓Shift消息显示原empty翻译。新增原creative pick完整update tag（含未封箱库存/分量，不走harvest扣分量壳），按实际LevelReader.registryAccess保存并写实体id。物品tooltip恢复原3行与已打包库存数量/名称，使用实际TooltipContext.registries解析组件；无provider时不伪造材料查询。普通/物流仓储继承该实现，不重新注册内容；实际pick/数量/分量与封箱拆封待验。

3 Native Java，Core/资源/Forge/三个来源未改。集中Native compile+jar / Forge compile28秒通过，当前3个class与实际jar一致；无新夹具、不重复游戏。前次短专服不覆盖本批工具/组件/仓储行为。客户端、reload、成品jar、完整生存流程、独立进程存档重载和旧档仍待验；其他系统双版本对齐和完整三源整体移植仍未完成。
