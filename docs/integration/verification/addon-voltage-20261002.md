# Shared tool assembly, addon API, original voltage and code license — 2026-10-02

两版原头/柄装配规则合并为共享ToolAssemblyRules，平台只转换物品栈和调用实际配方接口。两版生存FakePlayer实际安装砂岩磨料（8次）、十次右键磨制给定青铜粗制镐头（余7次），用所得成品头和3木棍点击原版工作台结果槽，得到真实Bronze/Wood镐并留下2木棍。未加工头、双镐头和第三输入槽均拒绝装配。两版独立新JVM实际读原保存镐、余木棍及磨石完整NBT，前tick与200tick后语义一致，正常保存停止。没有手造load或补物品。

响应坩埚工艺作者反馈，新增共享GregTechAddon/GregTechAddons API版本1。Addon在自己的模组构造中注册；两版材料/物品关联及内置机器配方完成后派发一次配方就绪回调，按ID排序，重复/晚注册和失败显式报错。API JAR只含共享Java17类和许可，不含素材，不重新shade进Addon。新增材料自动注册/新机器/完整旧GregAPI仍待完善。域合同实际跑两个Provider；当前两版服务器确认生产入口初始化，但没有安装外部Addon，不把空列表日志当成完整生态验证。

核对原GT6 CS.java:146-157，纠正显示层UV后的GT5/GTCE名称为PUV1..PUV5/XV，并把旧GregTechConstants.V末两项2147483647/Long.MAX_VALUE改成原2147483648/8589934592。旧常量改由同一规范表复制，避免两份规则漂移；HU/KU/RU不是EU电压。

确认本人是saltnya，并要求沿用另外两份来源许可证，因此本项目代码采用LGPL-3.0-or-later，双版元数据和中英README同步，GNU原文及NOTICE进入两包和SDK。旧Forge MDK文本原文仍在审计记录，原第三方资产/组件和作者署名未重授权。CI增加SDK构建/验包，并移除已失效的saltnya授权未决公开测试包限制；没有推送或创建GitHub Release，远程CI未验证。

- neo_prepare：2m，PID 21396，200普通tick，实际退出0、正常保存停止。
- forge_prepare：2m 20s，PID 82744，200普通tick，实际退出0、正常保存停止。
- neo_initial_reload：1m 55s，PID 68808，200普通tick，实际退出0、正常保存停止。
- neo_current_reload：2m 22s，PID 16004，200普通tick，实际退出0、正常保存停止。
- forge_current_reload：3m 18s，PID 70056，200普通tick，实际退出0、正常保存停止。

集中工具编译52s，Addon/电压编译与75条域断言35s，最终双包7m 58s；581共享class逐字节匹配，SDK 1380921 bytes、Java17。

## 验证范围

- Grinding/workbench checks start with a supplied registered raw Bronze head, sandstone, grindstone/workbench and wooden sticks. They do not bridge the previous actually mined raw Cu/Tin through actual heating/alloying/casting.
- Real survival server FakePlayer performs sandstone installation and ten right clicks, then actual workbench result-slot click. No client click timing/rendering or full survival equipment acquisition is claimed.
- Both selected real saved Bronze tools, two remaining sticks and actual grindstone NBT are read semantically in independent current-code JVMs before ticks and again at tick 200. No stock repair or synthetic load.
- Addon domain contracts exercise two providers and deterministic once-only/context behavior. Actual loader startups exercise the wired callback lifecycle with no external addons installed; a separately built third-party addon has not been runtime-validated.
- API version 1 is the initial recipe-ready/material-query/energy-domain surface; automatic new-material/item/fluid/machine registration and old GregAPI completeness remain pending.
- Original table values/names are checked; this is not all-tiers machine/bridge/overvoltage runtime coverage or universal saved-world migration.
- Production mod JARs are packaged and checked, while runtime launches use the current development source sets. Installed production-JAR client/server launches, complete source integration and full survival remain pending.

复现工具段：Java17 Gradle及Native已有Java21工具链；隔离flat/offline/loopback目录和获同意EULA副本，`-PdirectCoreResources=true -PdirectCoreClasspath=true -PtoolAssemblySmoke=true -PserverSmokePhase=prepare|verify -PserverSmokeId=<同UUID> -PserverDirectory=build/<隔离目录> -PserverSmokeX=0 -PserverSmokeY=240 -PserverSmokeZ=0 :runServer`或`:neoforge:runServer`。正式打包不加directCoreClasspath。整体移植仍未完成。
