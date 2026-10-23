# 2026-10-04 / 扫描仪能量与原版作物分支

## 改动与来源

- `MultiItemRandomTools:516–518`：便携扫描仪容量 V[3]×8000 = 4,096,000 EU、电压 512；作物分析仪容量 V[2]×8000 = 1,024,000 EU、电压 128；两者每次输入最多 64 包，EnergyStat.makeTool 可充电不可供电。调试扫描仪使用原版无限能源统计，接受所有能源类型。
- `EnergyStat:68–75,108–124,128–169`：包范围为额定电压的一半至两倍；原版最后一包仍可进入不足一包的容量，存储值不钳制。普通扫描不足会余额归零、不发送信息；创造和调试扫描保持电量。`gt.energy` 存在正常充电数据，NeoForge 保存于 CUSTOM_DATA；空电工具可叠 64，带电只能单件，已有普通物品身份保持。
- `Behavior_Scanner/WD.scan`：基础块信息免费，报告项目每项 512 EU；调试扫描仪显示 Java 类，实体左/右键按原版拦截，便携扫描仪拦截但不输出实体类。流体信息通过点击面的原生能力读取，保留直接 IFluidHandler 后备。
- `Behavior_Cropnalyzer`：便携、调试扫描仪和作物分析仪优先调用原版 IC2 作物接口。首次低于 scan level 4 时先提升到 4、费用 32,768 EU，再查看为 512 EU。普通植物不生成虚构分析；无原版接口或方法面时该分支不生效。
- 能量提示及两个行为提示取 MultiItem/Behavior 原文，中文为对应翻译。现代客户端预测只消费支持的点击，不改变电量、作物扫描等级或发送报告；实际动作由服务端完成。

完整源文件与当前文件 SHA-256：[来源回执](scanner-energy-source-20261004.json)。来源目录没有编辑，沿用 Gregorius Techneticies / GregTech-6 Team 的署名及 LGPL-3.0-or-later；未引入新资产或依赖。

代码提交：`fae26809bfa0265b8187c6bd4f9744e4d4538755`。构建先于 Git 提交，来源回执逐文件散列复核后提交；不声明 JAR 自带该提交号。

## 实际验证

第一次快速双版编译暴露分支替换留下多余括号，失败日志 `work/scanner-energy-compile-20261004.log`。修复后第二次 `-PdirectCoreResources=true -PdirectCoreClasspath=true :compileJava :neoforge:compileJava` 编译 14s 成功，日志 `work/scanner-energy-compile2-20261004.log`；失败没有计为通过。

本批仅改变两个共享语言 JSON，确认 git 资源差异恰为这两份后，将原字节同步到已有 `core/build/resources/main`。随后使用普通 classpath，运行一次：

```
.\work\gradle-runtime\gradle-8.8\bin\gradle.bat --offline -I work/emi-local.init.gradle :core:check :assemble :neoforge:assemble -x :core:processResources
```

56s 成功，日志 `work/scanner-energy-build-20261004.log`。共享检查共 9,747 条断言（行为 2,014 / 18 组、材料 7,646、机器 34、温度 53）。本批新增 21 个固定原版能量边界值，涵盖容量、电压范围、64 包限制、负包、最终超额包、余额不足归零与免费头信息，不复制算法作预期结果。没有重复共享检查，也未重新复制全部静态资源。

额外用忽略目录 `work/scanner-crop-probe-src` 的公开原版方法面、非公开实现类运行九项中性接口样例，验证 32,768/512 EU、扫描等级提升先于失败付款及报告字段访问；记录 [接口样例](scanner-crop-fixture-20261004.json)。这是纯 Java 契约替身，不是真实 IC2 或 Minecraft 游戏。

[成品验收](scanner-energy-artifacts-20261004.json)确认两包 632 个共享 class 与当前编译字节一致、平台 metadata/NOTICE、无重复或测试内容；[变更验收](scanner-energy-changed-artifacts-20261004.json)确认两份语言资源与源码逐字节一致，Forge 13 个变更原生 class 存在于重混淆成品，NeoForge 11 个与当前编译字节一致。

## 未完成与证据边界

没有启动客户端或 GameTest，没有实际充电工作台/电池箱生存操作、真实 IC2 作物、成品安装或存档重载证据。反射方法面来自本地原版，不声明当前其他 IC2 版本或变更 API 已兼容。自动从 IC2 穿戴装备充电、IC2 扫描音效、其他外部扫描接口/事件、部分 GT 温度/重量/压力扫描及全部运行状态措辞仍有缺口，需继续按原版接续；本批不标记扫描体系或整体移植完成。
