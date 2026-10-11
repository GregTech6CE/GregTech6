# 原版小型蒸汽轮机、提示与材料数量 / 2026-10-07

本批基线 `a1b36036`，目标为原版 Loader1512..1548 的十五种小型蒸汽轮机。现有五种额定输入/输出原本正确，保留其ID及值；补入铁木、钢叶、神秘、钛、炽热钢、铝、镁铝、虚空、三钛、石墨烯十种。修正黄铜/殷钢外壳为青铜、铬为钢；外观跟随原 Kinetic_T 外壳，名称跟随转子材料。

## 原版精确记录与共享入口

- 每个原配方 `TwT/GSG/TMT`：四个4.25U转子、14U双层机壳、两个4U齿轮、1U长杆，合计23U机壳材质及17U转子材质；同材质合并为40U。原已有五条数量不改。新增十条后固定记录469；原459固定记录和252机器档位逐条一致，120既有漏斗映射保持。两端继续使用同一登记、高级提示单次段及恢复数据；实际新恢复配方只准备后续运行证据。
- ANY.Steel 保留在原配方的齿轮、杆、机壳及钢转子输入，接受现有分组成员；原版组会继承Steel外观/输出，平台实体身份采用具体Steel，不将无效负ID分组登记为方块材质。Native双层机壳解析补分组。没有扩大重写MaterialGroups。
- 十五共享配方加入catalog，合计539，路径 `gregtech:hand/turbines/<id>`；使用实际工具及当前平台储存输入保护。Source记录取原CR.REV，工具不计材料、不以任意候选原料估算。十五名称/原创造序号380..394、物品堆叠16恢复。
- 28提示键加15名称的43中文值逐字照 `C:/Dev/GregTech_zh_cn.lang`；补3英文提示键，按原显示Steam输入范围/背面、RU输出范围/正面、66.66%效率、朝向/换向、侧面排放80%提示及既有防爆段。后者来自原提示，实际凝水为原局部200L常量，不能替换为锅炉160L常量。

## 采用的原行为与平台边界

轮机接入原Motor公共转换器：2N能量存储、8N流体容量（原最大输入2N乘4，原生加载后重置），背面Steam输入/正面带符号RU输出、全罐蒸汽两tick各一半转换、逐tick固定耗能、0..15源模式。停止拒绝新输入，已有罐/延后半份/缓冲仍处理；无接收端仍耗能，possible/emitted/history与停止视觉分开。源STEAM过载头两tick清超量、后续过载，与EU/RF消费限制不同。

凝水按本类原 `STEAM_PER_WATER=200` 累积，四个垂直于朝向的面尝试蒸馏水输出，丢弃未接受部分。Monkey Wrench清输入缓冲但保留待处理半份；放大镜方向、向上踏步旋转、三态及快慢/方向贴图继承Motor。运行不要求插转子；原配方含转子，旧移植插入的转子仍可取回。Plunger清整个流体罐，并按原返回移除量计工具损耗，保留独立缓冲/延后半份/凝水计数。

五个非输出面公开罐视图，仅当前背面可填、不能排液；已缓存的按面接口每次填充复核朝向和停止状态。现代无面接口保留先前填充兼容入口；不将它称为原SIDE_ANY完全等价。盖板仍经过本平台公共包装。两个平台NBT/组件/Forge LazyOptional及Neo查询参数差异留在原生边界；其他引擎/大轮机、源配置/保存额定覆盖未在本批扩大范围。

## 检查与失败

共享基础2474断言、来源机器835断言通过；两版主源码/成品标题屏幕探针编译通过。最后只重编有限世界夹具12s通过，两版每端两项已准备：真实Steam→发电机→完整电池、侧向凝水/缓存接口转向/停止/延后半份换向/Plunger，以及十五实际配方、同步、工具损耗、恢复/坩埚精确数量及储存保护。**这些世界夹具和本批成品探针尚未运行**；方法往返也不称独立世界重启。

第一次8s字段拼写失败（Materials.Tungstensteel）；第二次10s原ANY.Steel无效原生材质身份失败，修正为原钢外观并保留配方组；第三次42s共享/两端主代码/探针通过，Forge夹具FluidStack没有Neo copyWithAmount，改现有注册流体工厂后仅重编夹具。日志保存在ignored work/。补丁第一次完成Forge后因Neo读罐API还要lookup参数暂停，修改适配再写Neo；未将失败当成通过。

十五款每端72个通配选择器/款覆盖144实际状态，共2160；210原生模型/状态/物品文件的引用和tint0、22既有PNG/mcmeta原资产SHA逐项检查。静态通过不称烘焙/动画/游戏着色通过。原459固定/252档位语义对照及43中文精确值通过。29原版/汉化/许可证引用、8 `_w` 只读比较、22源资产及210原生文件哈希见 [来源账本](steam-turbine-source-20261007.json)。原代码GregTech-6 Team/Gregorius Techneticies、LGPL-3.0-or-later、资产CC0证据分开保留。

## 后续验收

按用户减少测试要求，热能、磁铁、轮机三个批次合并普通双包/CRC/current-class核对和正式Loader隔离启动，运行预备成品方法/模型与限定世界检查。最后已验收成品仍 `build/verified/20261006-211017Z-dc39ad68`，代码6c136a60，**不含**三个新批次。完整生存、独立世界重启、旧档、专服、玩家悬停及所有方块的剩余专属提示/材料仍未完成。

仅本地提交；未推送，PR暂缓。整个移植goal保持active。

Pooled ordinary-JAR and installed-loader acceptance passed at code12010bfa. Both native fixture pairs passed in the six-test pool; ordinary distribution methods/factories/tooltips/baked states verified separately. See [pooled delivery](converter-pooled-delivery-20261007.json) for package hashes, finite scopes, all runtime attempts and remaining boundaries. Goal active; local only.
