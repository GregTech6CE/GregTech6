# 第六十七批：电动扳手模式

## 原版依据与改动

GT_Tool_MonkeyWrench_LV 继承 GT_Tool_Wrench_LV，替换机器点击工具类型为 monkeywrench，仍使用相同图标、采掘、速度和磨损。Behavior_Switch_Metadata(mSwitchIndex,true,false) 在潜行点击且工具行为未处理后切换；目标为 IItemGT 方块或没有方块实体的方块。MultiTileEntityBlock 实现 IItemGT。

现代版本保留同一个注册物品，gt.monkey_wrench NBT 表示模式。GTToolHelper 按实际 ItemStack 决定交互身份，材料、电量、磨损和制造配方不变；切换不耗电，空电也可以切换，但不能配置机器或获得正常采掘资格。工具模式及操作说明加入双语提示。

Minecraft 潜行会跳过 Block.use，因此 useOn 将本模组方块操作优先交回现有处理器，消费的操作不再切换；外部方块实体返回 PASS。无方块实体目标用于可靠切换。操作遵守 mayBuild/mayInteract，服务端修改并同步背包。不同于 GT6 metadata，本实现保存独立 NBT，不新增重复注册或独立制作配方。原版两种模式共用图标，无需新建虚构模型。

## 验证

新增 ElectricWrenchModeTests 两项：切换前后完整 NBT 保持、持久化、空电切换/操作拒绝、非潜行/外部箱子/权限拒绝，以及真实变压器活动扳手点击优先于切换、扣电一次和保留挖掘资格。

首轮 build/gametest_batch67_wrench_modes.log 因 F 盘剩余 0 字节、区块写入失败中断，没有通过记录；外层命令返回码不能覆盖实际磁盘错误。临时世界清理被自动审批拒绝，未执行删除。空间恢复到约 18 GB 后，确认源文件与双语 JSON 可读，在全新世界 build/gametest-batch67-wrench-modes-recovered 重跑，日志 build/gametest_batch67_wrench_modes_recovered.log。最终于 13:24:42 报告 All 1029 required tests passed，13:25:15 正常关闭，进程退出码 0，BUILD SUCCESSFUL（10m25s）。已刷新 docs/porting-progress.json。运行期间冻结 Java/资源，不构建 jar。

## 客户端验收

1. 充电电动扳手潜行右键石头，在普通/活动扳手模式间切换，提示同步。
2. 普通模式旋转变压器；活动模式切换变压方向，保持模式且扣电一次。
3. 模式切换保留材料、电池容量、能量与寿命；空电可切换但不能操作。
4. 大型坩埚特殊模型及 Jade 仍等实机测试，服务器通过不代替渲染验收。

## 下一批核对结果

GT_Tool_Chainsaw_LV 不只是普通斧：基础品质 +1、速度倍率 2、容器制作损耗 200；目标包含 axe/saw 及木材、仙人掌、叶、藤、植物、瓜、冰/浮冰、珊瑚。叶子调用 IShearable 并替换掉落，冰在原掉落为空时补方块且清除水；还继承斧的特殊掉落逻辑。父类 GT_Tool_Axe 还包含非潜行同列同种木块/巨型蘑菇连伐，逐层累加损耗并随高度降低速度；本模组现有 ToolBlockConversionEvents 处理的是锤/凿转换，并未覆盖这条电锯链。不能仅给工具添加 axe 标签便宣布完成。攻击、树苗/工作台放置及跨模组兼容也是独立缺口。

完整目标仍未完成。没有执行 git commit。
