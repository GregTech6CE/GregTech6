# 2026-10-04 / 占位电池移除与原生旧物品迁移

反馈的五个按电压命名的占位电池仍有独立注册、创造栏、工具配方与资源。本批按只读 GT6 `Loader_MultiTileEntities` 14000..14044、`Loader_Tools` 的 re-battery 等级组继续推进；整体移植仍未完成。

## 实际改变

- 删除两端 BatteryItem、LegacyBatteryDefinitions、Neo LegacyBatteryRegistries 及五个独立物品注册。旧 Java 字段仅保留为真实化学电池的延迟兼容句柄，不进入注册/创造列表。
- 工具生成只保留五种原版 LV 化学电池行，删除每个材料/工具的额外占位电池行；默认 Java 配方也采用镍镉 LV 的实际容量128,000EU及规范ID。旧已制成工具自己的容量/电量数据没有改写。
- 删除十个旧物品模型、两端 IV 假电池标签；LV/MV/HV/EV 标签移除旧 ID，保留五种源电池和既有可放置化学电池兼容ID。删除两份语言中的十个占位翻译，其他语言值不变。25种原版化学电池及五个旧可放置 block ID 保留。
- Forge 注册阶段用 ForgeRegistry.addAlias 处理物品解析，MissingMappingsEvent 另在 Forge 总线处理旧注册表快照；Neo在 DeferredRegister 的注册事件前添加别名。旧ID不是新增隐形物品；实际注册 keySet 不包含它们，重新保存使用目标ID。
- Neo化学电池在原生组件加载后规范电量/最大堆叠，并动态限制带电物品为1，避免旧 CUSTOM_DATA 有电量但没有 max_stack_size 时可堆叠；名称和其余自定义数据保留。

迁移选择属于现代移植的兼容政策，不是原作者的旧ID映射：前四种用同电压镍镉，容量覆盖全部旧合法电量；原版没有 IV 化学电池，旧IV降至原版最高EV等级钴酸锂，同时保留合法电量。

| 旧物品 | 真实目标 | 旧→新容量 EU | 旧→新电压 EU |
| --- | --- | ---: | ---: |
| battery_lv | battery_nickel_cadmium_lv | 100,000→128,000 | 32→32 |
| battery_mv | battery_nickel_cadmium_mv | 400,000→512,000 | 128→128 |
| battery_hv | battery_nickel_cadmium_hv | 1,600,000→2,048,000 | 512→512 |
| battery_ev | battery_nickel_cadmium_ev | 6,400,000→8,192,000 | 2,048→2,048 |
| battery_iv | battery_lithium_cobalt_ev | 25,600,000→131,072,000 | 8,192→2,048 |

[来源检查](battery-cleanup-source-20261004.json)记录25条作者电池定义、十个等级标签、模型/语言删除及其他语言值不变。Forge47.2.20缓存源码和当前47.4.20源码均核对 addAlias 与 MissingMappingsEvent 的总线；Neo21.1.243的 DeferredRegister/BaseMappedRegistry、EMI重载完成条件也记录源码哈希。既有 NOTICE/许可和历史来源记录保留，[新来源与迁移记录](../../../core/provenance/battery-placeholder-removal-20261004.json)单独说明本批变化。

## 构建前实际世界检查

扩展已有 opt-in 新世界夹具，不增加独立游戏轮次。实际客户端从标题页创建全新 UUID 普通世界，进入真实玩家/集成服务器后，逐项验证：五个旧ID解析为真实目标、没有独立注册、旧空/满电各一次原生保存读取（十次），保留电量/数量/名称/自定义字段，保存规范ID，带电最大堆叠1/空电16；25个源电池等级标签；四种钢制电动工具×五种LV电池共20条服务器配方输出的实际容量，以及额外占位配方不存在。

| 实际开发运行 | 普通世界/玩家 | 物品规范化 | 实际配方 | 游戏帧 | 成功回执耗时 | Gradle |
| --- | --- | ---: | ---: | ---: | ---: | --- |
| NeoForge21.1.243 / Java21.0.7 / EMI1.1.24 | 成功 | 75,940 | 42,726 | 3,868 | 151,730ms | 2m59s成功 |
| Forge47.4.20 / Java17.0.10 / JEI15.20.0.119 | 成功 | 75,863 | 42,610 | 30 | 176,982ms | 4m22s成功 |

两份成功回执均为5别名/10原生栈往返/25电池标签/20钢工具配方；两张1280×720截图实际查看，包含HUD、roads和文字路牌，不当成电池物品渲染/手动交互证明。两端配方数比前批少756条，与去除额外占位配方一致；Neo数据包实际保留3,780条化学电池工具组装行。

首次Neo已得到上述电池检查结果，但夹具在EMI仍重载时退出，EMI worker读取已卸载的client.level触发空指针；构建入口拒绝，未打包。夹具现在等待 EMI.isLoaded（包括后台线程结束），重跑Neo仅一次；EMI实际36,180ms重载完成，成功退出后没有空指针。此项修改只修正自检退出时机，不宣称修复全部整合包EMI问题。

第二次脚本调用完成上述Neo和首次Forge运行，但其已解析的旧条件还要求Forge加载EMI，因实际Forge开发环境只有JEI而拒绝回执。当前条件已按平台修正，用当前脚本的AST原样取出验证条件，核对同一两份完整日志/唯一成功回执/截图和所有计数后，再继续正式双版assemble；没有为重复脚本而重跑已成功的客户端。日志在 `build/preflight/{neoforge,forge}-world-creation.log`，失败日志另保留于ignored work。报告保留失败边界，不把第二次整段脚本退出写成成功。

## 正式交付

初始正式双版10m45s完成，但实机测试暴露Forge缺refmap及Neo安装副本ZIP损坏，未作为最终交付。Forge完整编译与最终双版/独立探针4m37s成功；[验包](battery-cleanup-artifacts-20261004.json)核对655当前共享class、ZIP全条目CRC、完整生产映射和测试类排除，[内容报告](battery-cleanup-content-20261004.json)核对Forge11个/Neo8个本批原生class及电池资源删除。

正式Forge47.4.26/Neo21.1.252和Java25.0.2的隔离客户端均已到真实主菜单，正常退出及1280×720截图实际查看；没有在此生产组合再次新建世界。完整成品保存到新的稳定目录，见 [启动与交付记录](production-delivery-startup-20261004.md) 和 [大小/哈希/路径](production-delivery-publication-20261004.json)。没有重复完整共享套件或其他机器测试；原有相关GameTest/Neo电动工具夹具仅同步真实容量/配方ID并参加编译，没有执行。

## 仍未证明

原生物品解析/规范保存已实际执行；旧存档和Forge旧注册表快照的MissingMappingsEvent尚未实际重载。Neo 21.1.252/Forge47.4.26/Java25实例没有原地改动。新建世界开发检查和成品主菜单启动不等于充电/电池箱网络、完整生存工具制作、外部电池兼容、物品栏交互或Nexus全部布局验收。非法资源路径、动画帧及EMI未翻译标签仍在；其他面板、反应堆内容、蜜蜂、全面tooltip和最终cfg继续接续。
