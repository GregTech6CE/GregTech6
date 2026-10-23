# 双版本初期对齐：传感器制作和基础机器提示

按既有 P3_NATIVE_LASER_SENSOR_LONGDISTANCE / P3_BASIC_MACHINES 记录并检查实际接线：Native已有20传感器完整注册/控制/读数/面板，但没有 SensorRecipePack；已有258基础机器实际 BasicMachineItem 只显示能源范围和材质/等级。不是因为缺少同名Forge类就全部重迁，已确认管线/漏斗/齿轮箱/数据开关绘制与流体菜单同步在既有Native事件类实现，未重复注册。

完整 SensorRecipePack 接已有共享 GTSensorRecipesGen 的20行，查询真实 Native 方块/物品，复用 MachineRecipeIngredients、材料目录和 tool_shaped 磨损serializer。保留原 machine sensor recipe id、gregtech_sensors group、StainlessSteel默认材质、LV=1的compact_sensor和 allow_mirror=false；实际图样未用键先过滤再解析，避免原表富余B/C/G键导致解析失败。一个原shaped步骤直接产各传感器物品，无独立multi-tile转换步骤。pack priority/BOTTOM覆盖行为不变；Native只适配 PackLocationInfo/supplier、format48、recipe/目录与result.id。日志新增完整生成/未注册或未解析数量，尚未启动观察计数。

MachineTooltips 补完整原 appendBasicMachine，实际 BasicMachineItem 调用它：原 RecipeMap名称、额定/最小/最大能源与方向、能源输出、物品/液体输入输出与自动面、工具操作、爆抗/采掘工具和constructionMaterials数量均按Source保留。已有燃烧箱helper不删；其他机类自己的物品提示已有实际接线，不复制GTMachineBlockItem到每类。无新注册id或存档格式。

3 Native Java，Core/资源/Forge/三个来源未改。一次集中Native compile+jar / Forge compile32秒通过；本批只核当前4个编译class与实际jar一致，不新建夹具、不重复客户端或专服。上一批烤箱专服证明基础能启动，但不覆盖本批传感器配方生成/解析、实际制作/镜像/工具余物、提示和客户端显示；这些留到随后集中短检查。成品jar运行、完整玩法、独立进程世界重载、旧档、其他系统对齐及完整三源整合仍未完成。

后续集中启动：P3_NATIVE_ENCHANTMENTS_SOUNDS.md已记录20传感器行全部生成/0缺失及解析/专服启动通过；实际制作和提示显示仍未验。
