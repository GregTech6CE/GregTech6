# 第六十四批：电动扳手、螺丝刀机器点击接线

## 原版依据

- GT_Tool_Wrench_LV / GT_Tool_Screwdriver_LV 继承对应手工工具的 Behavior_Tool，普通点击 mDamage=100。
- Behavior_Tool.onItemUseFirst 先执行 onToolClick，再以 ceil(return×100/10000) 交给 MultiItemTool.doDamage；EnergyStat.useEnergy 扣 EU，不足则耗尽当前电量。
- TileEntityBase09FacingSingle:67 选中当前有效方向也返回 10000，因此电动扳手的同方向点击仍扣 100 EU。
- 原版空电工具有独立不可用形态。本移植物品 ID 不切换，使用单件且正电量作为机器点击可用判定。完整原版工具形态切换与玩家携带电源自动补电仍待移植。

## 修改

- ElectricToolItem 暴露 WRENCH / SCREWDRIVER 两种机器交互身份；电钻和电锯不冒充它们。
- GTToolHelper.matchesTool 识别有电的电动版本；getType 返回正确类型，公共 damageForUse 与 damageForToolClickReturn 转到电量消耗，创造能力免耗电。
- 普通点击按现有 helper 的一个操作单位扣 100 EU；原版返回值路径用整数向上取整，不会因 Long.MAX_VALUE 溢出。
- ToolInteractions.canApply 再校验电量，缓存的方向预览/spec 不能绕过已空电状态。现有权限/所有者检查继续生效；同方向有效点击仍消耗电量。
- 新增 isInteractionTool 区分覆盖板的工具身份和空手点击。空电螺丝刀不配置覆盖板，也不意外按下按钮；控制器边缘工具转发识别电动工具。
- 未扩展 GTToolHelper.isTool 的材料采掘语义；电动拆卸暂不获得免费机器自动收集，待实现 mineBlock 能耗/材料磨损后一起接通。

## 验证

新增 ElectricToolInteractionTests 三项，使用实际注册变压器及覆盖板：旋转/同面点击/电量耗尽/缓存 spec 拒绝/权限拒绝、螺丝刀配置与空电按钮隔离、成本取整/极值/创造豁免/电钻非镐。

初次启动门禁后主动停止，统一创造模式免耗电判定为项目现有的 abilities.instabuild；未将中断日志算作通过。随后门禁发现测试错误要求 STATUS 状态板支持螺丝刀反转；现有规格和实现均未提供此功能，改用 BUTTONS 复位/保持配置，并独立保留 STATUS 防误按检查。停止旧门禁、确认进程退出后更换新世界。

最终新世界 build/gametest-batch64-electric-clicks-green，日志 build/gametest_batch64_electric_clicks_green.log：07:07:01 报告 All 1021 required tests passed，07:07:34 正常关闭，Gradle 退出码 0，BUILD SUCCESSFUL in 8m 47s。测试期间 Java/资源冻结，未构建 jar。方向覆盖层在 ToolFaceOverlay/SlabPlacementOverlayRenderer 中复用 ToolInteractions.describe，代码路径已核对，但未进行本轮客户端像素验收。

## 下一步与测试建议

- IDEA 重启后，充电电动扳手应可显示机器方向预览并旋转，常见点击扣 100 EU。空电后应失效，充电后恢复。电动螺丝刀应可调整按钮覆盖板的复位/保持模式。
- 还未完成电动扳手/活动扳手模式切换、材料概率磨损与坏工具残件、电动机器拆卸、电锯采掘/剪叶/冰块行为。不能将本轮三项点击测试作为整个电动工具系统完成的证据。
- GT6 LV 电钻 isMiningTool=false，只匹配专用 drill，不应直接赋予普通镐采矿能力。
- 大型坩埚模型/Jade 的客户端显示继续待实机验证；其它占位仍待核对。没有构建 jar，没有执行 git commit。
