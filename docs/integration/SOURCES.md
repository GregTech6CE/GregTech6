# 来源与贡献记录

## 2026-10-04 / AdvancedCrafting 转换与 NO_AUTO

采用 Gregorius Techneticies / GregTech-6 Team 的 Loader_Recipes_Handlers:552–625、AdvancedCrafting1ToY/XToY 权限与普通配方接管、RecipeMapAutocrafting ALLOWED 过滤、CR.NO_AUTO/DEF_NAC，以及 MultiTileEntityPipeFluid 的允许拆分行。两份外部兼容源文件中的八条 NO_AUTO 只审计，未导入其物品。原目录只读，沿用 LGPL-3.0-or-later 和 NOTICE，无新增依赖/资产。八个原文件及本批代码/数据/工具散列见 [来源回执](verification/autocrafting-permissions-source-20261004.json)，具体 1,272 条静态源行绑定见 [绑定记录](verification/autocrafting-permissions-bindings-20261004.json)。

## 2026-10-04 / 自动合成机、原版蓝图结构与复制

使用 Gregorius Techneticies / GregTech-6 Team 的 RecipeMapAutocrafting、Recipe 优化、UT.NBT 蓝图格式、BasicMachine/Container 程序槽、MultiItemRandomTools 蓝图与机械臂、Loader_Fluids 四种蓝染液、ScannerVisuals/Printer 数据分支和 GT6_Main 展示行。保留 LGPL-3.0-or-later 及原作者署名，原目录只读，没有增加依赖或资产。18 个源文件散列（含最新基岩矿提问的 WorldgenOresBedrock/WD 复核）及当前文件回执见 [本批来源](verification/autocrafting-source-20261004.json)。原版 NO_AUTO 全局元数据抽取仍待完成。

## 2026-10-04 / 扫描仪 EnergyStat 与 IC2 作物接口

来源为 Gregorius Techneticies / GregTech-6 Team 的 MultiItemRandomTools:516–518、Behavior_Scanner、Behavior_Cropnalyzer、EnergyStat/EnergyStatDebug、MultiItem 能量提示/堆叠和 WD.scan。保留原作者署名与 LGPL，来源目录只读；没有新增纹理、音效或外部依赖。原版方法面用可选反射接续，九项中性接口样例不能证明真实 IC2 兼容。源文件 SHA-256 与当前改动散列见 [来源回执](verification/scanner-energy-source-20261004.json)。

## 2026-10-04 / 原版黏土模具与完整图案

制作/回收取 Gregorius Techneticies / GregTech 6 team 的 MultiItemRandomTools:80–233，烧制图案取 MTE:352,391–420；完整形状构造来自 MultiTileEntityMold:628–921。蜡/石材/塑料原料族取 MT 工厂和 ANY 嵌套，当前材料别名图未扩大修改。沿用 LGPL 和 NOTICE，来源目录只读，未引入新纹理或依赖。旧形状抽取回执标记为历史证据，当前逐行来源与散列见 [本批来源](verification/clay-molds-source-20261004.json)。

## 2026-10-04 / 原版 Technology 嵌入配方

物品配方取自 Gregorius Techneticies / GregTech 6 team 的 MultiItemRandomTools/MultiItemTechnological，扫描仪取 MTE 电池注册部分；高等级替代以 LoaderOreDictReRegistrations 为准，橡胶锤头和钻石组以 OP/MT/ANY 实际条件及工厂绑定为准。沿用仓库 LGPL 和 NOTICE，未导入纹理或外部依赖，没有修改原项目。逐项来源、作者、散列、边界见 [来源回执](verification/technology-source-20261004.json)。

## 2026-10-04 / 原版弹药与箭材质

箭杆/装药弹壳的注册、消耗/回收及工作台路线来自原 GT6 OP、Loader_Items、Loader_Recipes_Handlers/Other/Vanilla；原生弹射物参数以 EntityArrow_Material 和 PrefixItemProjectile 为准。保留原作者 Gregorius Techneticies 署名，以实际执行值 U9 而非旧注释 U4 表示箭主材料。箭 PNG 从 wolfram0108 的 GregTech 资源目录保留，按其 LICENSE.assets 的默认 CC0-1.0 分项声明处理；新增源文件散列和范围见 [来源回执](verification/ammunition-source-20261004.json)，NOTICE 及包内副本同步说明。

## 2026-10-03 / 灌木恢复与蜂巢身份扁平化

只读原 GT6 `MultiTileEntityBush` 提供主体/枝条几何、支撑继承、采摘与 byte 生长溢出规则；`WorldgenBushes` 提供成熟核心和五方向生成枝条；`CS.BushesGT` 的默认 string 条目补齐棉花。原作者 Gregorius Techneticies，沿用本仓库 NOTICE 与许可。蜂巢变种复用工程中已有的 16 色模型和纹理，没有新增外部图片或库；现代睡莲直接继承平台 API，不复制其源码。来源文件 SHA-256 及未覆盖的兼容行为见 [本批来源与验证](verification/visual-plants-20261003.md)。没有修改来源目录。

## 2026-10-02 / 超温与木管火焰参数

以原 GT6 `MultiTileEntityPipeFluid:320-331`、`WD.burn/fire:702-727`、`TileEntityBase01Root.setToFire` 为过热顺序和火焰依据；木管可燃性来自原注册参数及 `TileEntityBase07Paintable:107-108`，并核实 `TileEntityBase10ConnectorRendered` 的泡沫屏蔽尚需迁移。GT 非易燃保护的依据是原 `IBlockBase/IItemGT`、`BlockBaseFluid` 和多实体块族；现代归属与外部标签映射的取舍、来源哈希及游戏夹具修正见 [过热记录](verification/fluid-pipe-overheat-20261002.md)。沿用作者及许可，无新外部依赖。

## 2026-10-02 / 管道魔法危害

原 GT6 `MultiTileEntityPipeFluid:284-295` 与 wolfram 对应分支提供魔法泄漏量、处理顺序、范围中毒及损毁规则；`FL:1119` 提供材料派生流体的 MAGICAL 分类来源。缺失 Thaumcraft 时的空气回退由原版 `IL.block`、`ST.block`、`CS.NB` 核实。双版沿用既有流体标志和材料解析，新增真实管道后果及共享损失规则，无新库和来源目录修改；来源哈希、可复现场景和未实现的污染兼容见 [魔法危害记录](verification/fluid-pipe-magic-20261002.md)。

## 2026-10-02 / 管材参数目录

逐项采用原 GT6 `Loader_MultiTileEntities:1846-1885` 的 40 条管材容量、四耐性及耐温参数，并用 `MultiTileEntityPipeFluid.addFluidPipes:83-98` 核对尺寸倍率和默认阈值。原版与 wolfram 本地快照的全部 40 条注册参数去除排版后相同。特殊材料别名通过原版 MT 声明核实；保留当前注册 ID。来源哈希、28 行改动明细和现代存档迁移选择见 [目录批次记录](verification/fluid-pipe-catalog-20261002.md)。原作者与许可归属沿用 NOTICE，无新依赖。

## 2026-10-02 / 流体管温度与物理危险

对照只读原 GT6 `MultiTileEntityPipeFluid.onServerTickPre:277-341` 与 wolfram0108 同路径实现，适配实际温度、独立气体/等离子/酸性危害及泄漏统计；纯规则进入共享 core，游戏实体和世界操作保留平台实现。来源哈希与验收范围见 [本批记录](verification/fluid-pipe-safety-20261002.md)。另读取原版 `Loader_MultiTileEntities:1846-1885`，确认当前管材目录的容量、耐性和专用耐温差异，留待后续逐项修正。本批未修改来源、引入新库或改变已有 NOTICE 归属。

## 2026-10-02 / 玩家流体容器事务

对照原 GT6 `TileEntityBase06Covers` 的面拦截、`MultiTileEntityPipeFluid` 的连接规则，以及 `TileEntityBase08FluidContainer.onBlockActivated3` 的单件容器消费与结果返还。Forge 的现代实现采用本项目已有 NeoForge 容器适配方式，并直接调用 Forge 47.4.20 的 `FluidUtil`；排查时读取本机相同版本源码包的 `FluidBucketWrapper` 和玩家物品能力。没有新增外部库或修改来源目录；原作者、许可、资源声明保持。详情见 [交互记录](verification/fluid-container-interaction-20261002.md)。

## 2026-10-02 / 流体分配第二批

原 GT6 `gregapi/tileentity/connectors/MultiTileEntityPipeFluid.java` 提供炼药锅耗量、多目标均值、随机目标顺序、压力、面连接和拆管转移规则；wolfram0108 同路径的 BUG-025 注释和实现提供现代空锅/水锅拆分的核对依据。GT6 `FL.java` 的 WATER 集合对应本工程共享 `FluidCatalog.FluidFlags.WATER`。本批将规则适配到现有双平台实体并共享纯数值计算，未整体导入 wolfram 的注册/运行时。作者、LGPL-3.0-or-later 与第三方资源分项声明沿用 NOTICE；参见 [实现和验收记录](verification/fluid-pipe-distribution-20261002.md)。

## 2026-10-02 / 当前本机路径与新增参考

当前工作树为 `C:\Dev\GregTech6\GregTech6-main`，无 `.git`；下文旧机器上的提交、标签及历史恢复记录不代表这些 Git 对象存在于本次解压目录。

新增提供的 **wolfram0108 / gregtech6_w** 快照实际位于 `C:\Dev\gregtech6_w-main\gregtech6_w-main`，README 标注 Minecraft 26.1.2 / NeoForge 26.1.2.109 / Java 25，代码 LGPL-3.0-or-later。源码头保留 GregTech-6 Team 版权及 wolfram0108 的 2026 移植声明。作为行为和适配思路参考，不改变当前项目目标版本。没有将无 Git 元数据的快照认作远程最新提交。

本批对照其多通道流体管与原 GT6，实现共享流体通道选择，未整体引入该项目代码或资产。来源文件哈希、各来源比较、采用边界和验证见 [流体管道记录](verification/fluid-pipe-channels-20261002.md)。其余本机只读来源为 `C:\Dev\gregtech6-master\gregtech6-master`、`C:\Dev\cruciblecraft-master\cruciblecraft-master`、`C:\Dev\gregtech6yizhi-main\gregtech6-main`。

更新：2026-09-30。本文件记录来源归属和采用边界；功能完成以 `STATUS.md`、`VERIFICATION.md` 的当前证据为准。

## 三个来源快照

- **saltnya / gregtech6reborn**：`F:\Dev\GregtTech6New\Libs\gregtech6reborn`。指定为初始基础及功能保留基准。完整文件树已复制到整合目录；原始快照的本地导入提交为 `415617825945a9f54b11fdc359ff9b3cf694c8cd`，标签为 `import/saltnya-snapshot`。这是新仓保存的快照，不是 saltnya 的原始提交历史。基线为 Minecraft 1.20.1 Forge；没有现成 NeoForge 模块。许可范围待核实。
- **brokestar233 / gregtech6-main**：`F:\Dev\GregtTech6New\Libs\gregtech6-main`。具有 1.20.1 Forge / 1.21.1 NeoForge Stonecutter 共享源方案。作为逐系统实现及平台适配候选，尚未整体导入运行时。归档没有 `.git`，且 `third-party/modularui` 为空；缺少实际 gitlink 与可构建 fork。代码声明 LGPL-3.0-or-later，资产包含不同许可，不可统一标为 CC0。
- **masson / cruciblecraft**：`F:\Dev\GregtTech6New\Libs\cruciblecraft`。具有 NeoForge 1.21.1 实现、质量/能量算术、事务执行和逐文件来源政策。作为纯逻辑、平台实现及验证候选，尚未整体导入运行时。作者声明为 Lorbineitte Masson，代码 LGPL-3.0-or-later；作者自写资源、GT6 默认资产、MDK、参考副本需分别记账。

三份来源都是无 Git 元数据的本地文件快照。作者标识 saltnya、brokestar233、masson 来自说明，不能推定快照内每个文件均由该作者独立创作。上游作者头、第三方来源和已存在许可保持原文；可获得原仓 clone/bundle 后，另记录 URL、实际 ref/SHA、快照对应关系和来源中的未提交差异。

## 可获得原始历史的准确入口

2026-09-30执行只读 `git ls-remote --symref <源码已出现的URL> HEAD`，未设置新项目origin，未保存/改写第三方历史。以下只证明远程当前可达及所返回ref；不证明提供的文件快照已经与该历史对应：

- brokestar README:3,46,88出现的 [Meow404club/gregtech6](https://github.com/Meow404club/gregtech6)：公开读取成功，`refs/heads/main` 当前HEAD为 `f009fc3cbba5dd833d775b9a7c7ed4c8294865a0`。可在 `work/upstream-history` 保存完整bare clone后逐文件/tree对照；恢复ModularUI需读取匹配提交的gitlink，不采用归档注释里的旧SHA。
- masson模板neoforge.mods.toml:4,10与CREDITS:7出现 [icodestuljh/cruciblecraft](https://github.com/icodestuljh/cruciblecraft)；README:13出现 [lombinaxmasson/cruciblecraft](https://github.com/lombinaxmasson/cruciblecraft)。两个准确URL均可公开读取，返回 `refs/heads/master`、HEAD `2ba4e4b60f7a770360b2ec7ca2adad3507ec4a28`。同HEAD尚不能证明两者全ref一致或发生重定向；先保留两条来源记录，再保存一个完整历史并核对快照，不能猜作者remote归属。
- saltnya快照中README、mod元数据、全部Markdown/properties/toml与脚本扫描仅找到Forge/MCPConfig、toml-lang公共链接，没有项目自身仓库URL。该源仍需作者提供准确仓库或bundle/备份，不能按作者名称猜仓库。

历史保存与快照对应核对的完成状态以主线记录为准。精确可达URL是历史恢复线索，不授权在该owner下发布新项目；也不把从远程读取的当前HEAD伪装为原本解压快照的commit。

逐文件 SHA-256 清单在 `provenance/`，其中的 `git_metadata_available=false` 明确历史缺失。清单描述被提供的文件；不替代授权、功能验收或缺失子模块。原文审计保存在：

- [saltnya 审计](audits/audit-saltnya.md)
- [brokestar233 审计](audits/audit-brokestar.md)
- [masson 审计](audits/audit-masson.md)

## 原始归属与分项证据

`audits/notices/<source>/` 保存本次可获得的原始许可、credits、notice、mod 元数据和相关资产授权证据的字节副本。文件名带 `source-` 的元数据副本不是整合项目生效的配置。缺失文件明确写入 [许可证核查](LICENSE_REVIEW.md)，不由新的统一声明填补。

- GT6 原作归属 Gregorius Techneticies / GregTech-6 Team。brokestar 的代码头保留 LGPL-3.0-or-later 与部分原始版权；masson 的来源记录固定 GT6 ref `3703e40308c8c030763fd6297dea8b210d2a77b1`。该 ref 只证明其声明的参考来源，尚未核对三源全部文件是否来自该提交。
- ModularUI 归属 brachy84 / ModularUI-Modern；brokestar 的 fork 地址是 Meow404club/modularui。当前缺 fork 源码和 gitlink；不猜测 commit，不把 LGPL-3.0 改为 or-later。
- amazawa UI 归属天沢香 / tfc-amazawa-light-gui v1.0.5g。brokestar 的资产 manifest 声明 Apache-2.0，并有允许该高版本移植借用及要求署名的截图。这一分项不能被其根 README 的 CC0 概述覆盖。
- masson 保留 GTM `de5d2c4a4c863b94a10bfb5d0839df2de8246628` 的管几何/命名参考记录；不视为当前模组运行依赖，也不把参考副本加入生产源码集。

## 新整合的贡献记录格式

每次采用、改写或替代记录到 `DECISIONS.md` 或对应系统记录，并包含：

1. 功能、整合文件范围、来源文件路径及 SHA-256/可获得的原始提交。
2. 原作者、许可证和资产分项证据；第三方原文保留位置。
3. 三源行为比较、采用/替代/延后理由、基线功能是否保留。
4. registry ID、material ID、单位、配方、NBT/组件及网络兼容变化。
5. 执行过的构建/行为检查、输入夹具、结果路径与整合 Git commit；未执行项及已知缺口。

不要把自己的导入提交冒充原作者提交，也不要为恢复历史重写作者信息。已有源头引用要随抽取代码保留；仅作研究比较的未采用代码与真正分发内容区分记账。GitHub 远程所有者由项目方提供，本文件出现的来源 URL 不授权把新仓发布到该所有者名下。

## 已恢复的原始历史与准确快照对应

2026-09-30 已将公开原始仓库的分支和标签历史 fetch 到本地Git独立refs，保留原始提交和作者：

- brokestar：`upstream-brokestar/main` 可达3055个提交。快照并非当前远程HEAD；完整220275个tracked文件的SHA-256与 `3118acbf83a84d05fa37d57af1705cf00e423ca9` 全部一致，无改变/缺失/额外文件。本地标签 `source/brokestar233-snapshot` 指向这个原始提交。
- masson：`upstream-masson/master` 可达202个提交，完整61192个tracked文件与 `2ba4e4b60f7a770360b2ec7ca2adad3507ec4a28` 全部一致。本地标签 `source/masson-snapshot`。
- brokestar原快照的ModularUI gitlink为 `ec524db7a5724f8cf3c837436b1ae742c778c62f`；已获取 `https://github.com/Meow404club/modularui.git` 历史并建立 `source/modularui-brokestar-snapshot` 标签。恢复的是引用和历史，未在运行时导入ModularUI。
- saltnya 的原Git仓仍未提供，只有 `import/saltnya-snapshot` 的本地原字节文件快照和hash清单。不能伪造其原作者提交。

匹配证据在 `provenance/brokestar-history-snapshot-match.json`、`masson-history-match.json`；当前brokestar HEAD的差异报告单独保留以避免误认。上述原历史尚未合入整合主分支，不意味着采用了整套代码，也不是新项目origin。备份本地仓库时保留 `.git` 或用 `git bundle create work/history-backup.bundle --all` 保存所有原refs；日后发布新远程时先约定如何迁移这些历史refs。


## 2026-10-19 / 新报告的工具、木桶与坩埚交互修复

本轮工具／木桶／坩埚／测温／引擎接口依据原版GT6 Java；171木桶原图来自_w保留的GT6资产目录，CC0-1.0，已保留资产授权文本及NOTICE。木桶中文逐字使用指定补丁。 [详细范围、来源和验证边界](verification/gameplay-feedback-20261019.md)。仅本地，无推送或发布。
