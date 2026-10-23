# 万能花盆与格雷灯笼核查

2026-10-22，第一百零四批，基线 `14167b5d6` 加本轮工作区。上一轮只汇总 issue 状态，没有新增核查成果；本轮重新读取实际输入，继续全面核查及源码整理。不扩大 #71–#74 的处理范围，#66 配置项仍留后续。

## 入口与证据

原版 32065 花盆、32758 灯笼及其基类、方块转发、注册、配方材料组和纹理消费者，共 21 份 Java。原树只读，作者／版权和 LGPL 声明随摘要归档。当前读取 32 份生产 Java、1 份既有 GameTest 源码、16 项资源及三份构建／工具输入，共 55 个 Java 平台视图。三份本地原生工件七个条目包括 Forge 反汇编、Neo 源码及原生模型 JSON；四项旧证据重新核对工件、条目和反汇编字节。35 段具体引用见[回执](verification/plant-lantern-audit-20261022.json)。

## 已确认差异

| 编号 | 原版入口及行为 | 当前实际消费者 |
| --- | --- | --- |
| ALIGN-339 | 花盆的两段 `setBlockBounds2` 用于绘制。碰撞经 Covers → Root 的 `box()`，选中框经默认 `shrunkBox()`，均为完整方块；MTE 方块转发到这两个入口。 | ShapedToolBlock 将两段绘制坐标也用于碰撞／选中框。16³ 单位格中心投影中，四朝向各少 600 格，当前覆盖 3496／4096 格。不是实际实体测试；既有 GameTest 断言当前内缩底座，不能证明原碰撞对齐。 |
| ALIGN-340 | 花盆明确返回 `LIGHT_OPACITY_WATER = 3`。 | 共享基类 noOcclusion、非完整轮廓、空流体，经两版原生 `isSolidRender`／`propagatesSkylightDown`／`getLightBlock` 推得 0。没有执行光照 API 或量测生长效果。 |
| ALIGN-341 | 花盆和灯笼继承 Paintable 的喷漆／清漆及颜色状态，纹理使用该状态。 | 两者没有实体，不属建筑色族或原版替换族，实际 PaintTargets 不接受；花盆 Ceramic 基础 tint 已有，不能报全部 RGB 缺失。灯笼模型也没有喷漆 tint 面。 |
| ALIGN-342 | 灯笼继承 FacingSingle 的 GT 扳手选面、水平合法面及更新。 | 有放置、原生 rotate／mirror，但不实现 ToolInteractionTarget，也未调 GT 工具分派；不推断外部模组旋转工具。 |
| ALIGN-343 | 灯笼正面 Default 和其余面 Copied 纹理选择固定亮度 240／恒定亮度；正面关闭 AO。 | 当前普通 orientable 模型未移植固定表面亮度合同。发光 15／透光 0 已有，环境照明与表面亮度分开；实际像素差异待客户端核验。 |
| ALIGN-344 | 花盆石粉／黏土粉使用 ANY.Stone／ANY.Clay，原 stone／clay 工厂赋予家族；黑色花岗岩和红黏土是成员例。 | 生存表转成 mat:dust@Stone／mat:dust@Clay，消费者生成规范材料 forge:dusts/stone／clay。默认标签按实际材料名赋成员，没有展开这两个家族，拼写别名也未补范围。配方已有，缺的是原替代范围；完整活标签／配方枚举未运行，外部包仍可扩展。 |

扩展 ALIGN-139：原灯笼继承 Covers 全侧准入及完整表面，当前没有实体，事件和 PanelItem 找不到宿主。这是“原允许、当前拒绝”，与此前某些宿主的反向缺口分开。花盆原全侧禁贴，当前无宿主，未列作此缺口。

## 已有对应与源码整理

花盆两版顶面种植 hook、青色提示、堆叠 16、陶瓷主层染色和两段绘制坐标已有；灯笼四向模型、发光 15、遮光 0、堆叠 64 已有。动态生存配方包实际消费花盆九格和灯笼南瓜／火把／刀配方，不报整件没有配方。未在原花盆发现额外浇水、骨粉或加速机制，不凭名称编造缺失。旧 #76 树苗 cutout 修复与本次花盆物性不同，实际画面仍待验。

五份维护源由 174 展开为 254 行：PlantPotBlock、GregLanternBlock、SharedDecorBlocks、RegisteredBlock、Neo GTRemainingDecor。分开方法、工厂、循环和 switch；私有 reg 改为 registerPanelBlock，局部 v／b 改为 variant／panelBlockHandle。公共 API、ID、注册顺序和条件保持。

另外纠正 GTToolBlocks 的旧来源注释：采集袋是 32736，32758 是灯笼；花盆说明不再暗示原碰撞等于渲染网格。六源 11 个完整执行视图，经限定三标识符归一后逐项保持，字面量位置、其它注释和 Stonecutter 指令保持。未随整理改动玩法，用户两材料字节摘要保持。

## 覆盖清单与后续

重新清点原版 1229、当前 1825 份维护 Java，活动路径无 loader／loaders／a／b／c 目录片段。新增[引用筛查工具](../../tools/integration/inventory_audit_references.py)，兼容路径作为对象键、历史摘要字段及 CRLF 归一形式，避免将已引用的核反应堆源码误列为没有引用。

当前 98 份 `*audit*.json` 回执中匹配 292 个原文件摘要，原注册 class 字面量另有 123 个无匹配引用候选。**这些不是已核／未移植数量，不能换算完成率。** 其它回执名称、正文、完整继承／条件注册不在筛查合同内，已有引用也不代表全文件行为已验。见[筛查结果](verification/source-reference-screen-20261022.json)。

继续液体泉、其它日用方块／独立物品、完整材料／配方、原可选整合及全面系统队列，同时整理相应源码。原支持者证书此前由用户要求移除，不能因筛查器列出原类就重新加入。

本批没有 Gradle、Java 编译、GameTest、游戏、像素比较、构建或推送。几何投影、六种回执格式条件和词法证明不代替真实客户端／服务器、全种类植物、存档重启验收。全面 goal 保持 active。
