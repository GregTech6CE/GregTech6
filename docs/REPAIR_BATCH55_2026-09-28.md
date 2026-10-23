# 第五十五批：电池盒固定库存与机器电路等级

## 范围与结论

本批修正电池盒库存的基础错误，并纠正旧机器配方的电路替代方向。电池盒完整能量行为尚未重做，不把这一批称为电池盒还原完成。大型坩埚仍等待实机验证，未重复修改。

## 已修改

- 电池盒四槽由可压缩 ArrayList 改成固定 NonNullList：插入槽 3 就留在槽 3，移除槽 1 不再推动槽 2/3 的物品。
- 已占用槽拒绝再次插入；模拟插入、模拟抽取均不修改库存，模拟抽取返回独立副本。GUI 设置每槽最多一个物品。
- crowbar/右键辅助操作查找空槽或最后一个非空槽，不再用 list.size 判断库存满空。
- 序列化保留空槽位置、物品 NBT 和槽号顺序；旧紧凑列表仍按已有顺序读取。没有删除物品。
- 电池盒所有面开放同一 ITEM_HANDLER，GUI 与自动化共享库存；方块失效时使能力失效，拆除走现有 BlockContents 掉落。
- MachineRecipeIngredients 的 circuit:n 从错误的“接受低于需求的电路”改为对应 circuits_tier_n_plus 标签，与第五十四批电池配方一致。标签接受本档及更高档，并保留可选 Forge 电路标签。

## 原版证据

- gregapi/tileentity/energy/TileEntityBase10EnergyBatBox.java：固定库存，一槽一个；getAccessibleSlotsFromSide2 提供所有槽，canExtractItem2 允许提取。
- gregapi/load/LoaderOreDictReRegistrations.java:375-383：OD_CIRCUITS 高档向低档重注册，允许高档替代低档，反向不成立。

## 验证

- 初次 compileJava 通过。
- 新增 BatteryBoxInventoryTests 四项：指定槽位与模拟/取出；稀疏槽 NBT 与所有面能力/失效；真实破坏稀疏库存恰好掉落一次；六档电路的 36 组替代方向及红石拒绝。
- 全量门禁在新目录 build/gametest-batch55-inventory-circuits 完成：2026-09-28 03:58:31 **994/994 required tests passed**，Gradle 退出码 0，BUILD SUCCESSFUL in 8m 50s。日志 build/gametest_batch55_inventory_circuits.log 无 failed! 或 Parsing error。运行期间没有修改 Java 或资源；最新 Java 时间 03:50:12，早于最终日志 03:59:03。

## 下批必须继续的电池盒行为

当前仍接受灌注电芯并把它们视作虚构容量模块，集中 buffer 并不把电量写进电池物品。这个问题没有被本批槽位修复解决。下一批应独立提取电池盒的能量状态，保留固定库存、原注册 ID 和 GUI 入口：

1. 接受 IItemEnergy 完整物品，替换按电芯 path 判断容量的旧分支；原版允许放入能量物品，实际能否传输再由电压和输入/输出能力决定。
2. 补现代 IItemEnergy 缺失的 canEnergyInjection/canEnergyExtraction 等理论能力查询，不能用“当前是否有剩余电量/空间”冒充理论能力，满电电池也仍算可充电设备，空电池也仍算放电设备。
3. 每 20 tick 按 buffer/(input*40*slotCount) 的 0/1/6/7 档逐槽提取/注入 40/20/20/40 包；其它档不交换。内部缓冲上限 input*320*slotCount。
4. 接收预算为可充电物品数量*input*2；原版最后一包使用向上取整，需实现溢出安全。正常前面输出，其余面输入；模式限制安培，停止状态停止输出。
5. Jade 汇报物品内电量和容量之和，内部 buffer 另有含义。取出电池、拆除、保存/重载必须保持物品真实电量；无谓裁剪 buffer 会丢电，需移除旧逻辑。
6. 旧 BatteryItem 自身 amount*size 存在溢出且缺电压限制；接入前一并核正，不能把旧通用电池当成已正确实现的标准。
7. 增加空槽/满电/空电/错电压/模式/停止/模拟/存档/破坏的真实网络回归。普通四槽、大型十六槽及控制覆盖板需逐项对照，不拿只支持四槽作为完整系列完成。

## 实机测试

重启 runClient：电池盒在第 4 格放入当前可用电芯，再放第 2 格并取走第 2 格；第 4 格不应移动。关开 GUI、退出重进后槽位与物品 NBT 保持。漏斗/管道可以使用相同库存，拆除掉落内容。

查看要求高级/精英电路的机器配方：基础电路不再可以替代；更高档电路仍可用。客户端 GUI 像素尚未实际验收。

本轮未构建 jar，没有执行 git commit。完整移植目标仍在进行。

## 文件与审计

本批修改 EnergyNodeBlockEntity.java、MachineRecipeIngredients.java；新增 gametest/BatteryBoxInventoryTests.java。新增本报告，更新 NEXT_GOAL、PORTING_AUDIT、REPAIR_TRACKER 和 porting-progress.json。审计日志 build/audit_batch55.log，缺失 token 仍为 49；注册统计不是功能完整度。
