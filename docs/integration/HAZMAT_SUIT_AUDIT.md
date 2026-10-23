# 六套防护服的身份、护甲与提示核查

2026-10-22，第一百二十四批，基线 `26f7ca7b9`。上一批完成电池目录和齿轮物性共用并提交，属于进展。本批新增420／421，同时整理三源和共用纹理路径。四原／13当前生产Java、32引用、十原生条目、两官方旧Forge来源及104资源见[回执](verification/hazmat-suit-audit-20261022.json)。引用只支持列明合同，不代表整套战斗、可选模组兼容或生存流程完成。GregTech-6 Team／Gregorius Techneticies 的 LGPL 声明保留，原树只读；没有导入官方战斗实现。

## 已有六套、二十四件，不是整组缺失

原Loader_Tools注册insect、frost、heat、radiation、biochemgas、universal，每套四件，均128耐久和每件1护甲点。当前HazmatSuitCatalog／GTRadiationProtection有同样24身份；Forge经ItemBootstrap→GTFuelRods.registerAll触发套装类初始化，Neo经MATERIALS注册触发，不能只看registerAll空方法就误报未注册。

八组标签对本模组24件物品的192个成员关系与原自有套装分配对应：通用套装进入全部八组，生化套装进入生物／化学／气体，其余进入各自防护组。GTHazmat仍逐个检查四个穿戴槽；这不是所有第三方防护物品或所有危险消费者的完整兼容验收，既有210等另列。

104资源中包括24穿戴PNG、24物品图标、24模型、24配方文件和八标签。24穿戴PNG与原版参考字节相同；物品图标引用存在，配方输出ID对应，不把文件存在当成配料、真实合成或实际渲染全部通过。未重新运行历史导入脚本，也未修改素材或汉化。

## 420：护甲显示点数相同，减伤规则不同

原ItemArmorBase实现ISpecialArmor，使用材料护甲点计算吸收比例；官方旧Forge玩家受伤入口调用该接口。该路径与现代原版CombatRules不是同一计算，已核对应的旧Mojang字段映射、原生构造器和两端当前消费者。[旧Forge接口](https://raw.githubusercontent.com/MinecraftForge/MinecraftForge/1.7.10/src/main/java/net/minecraftforge/common/ISpecialArmor.java)、[玩家调用补丁](https://raw.githubusercontent.com/MinecraftForge/MinecraftForge/1.7.10/patches/minecraft/net/minecraft/entity/player/EntityPlayer.java.patch)只作为来源阅读，未当作旧安装包实测。

有限源码见证：玩家穿四件完好防护服，无附魔／效果，普通可被护甲吸收的10HP伤害，武器不修改护甲效率，且只比较护甲阶段。原每件吸收比例0.04，此范围未触及吸收上限，剩余8.4HP；现代四护甲点、零韧性得到约9.68HP。当前没有恢复旧特殊护甲规则，不能凭“每件都是1点”就称实际减伤等价。记录为对齐策略差异，本轮保留现代规则；未测玩家最终扣血、完整事件链或全部伤害类型。

原构造参数aDamageReduction的8并未被使用，不能把它误报成原每件8护甲点。原ArmorMaterial的四个值均1，原物品字段也由该值初始化；当前每件1点本身不是缺口。

## 421：普通悬停缺少原来的耐久数字行

原ItemArmorBase.addInformation对可损耗且无子类型的物品，普通悬停就加入“剩余 / 最大”数字，完好物品也显示128 / 128。当前RadiationSuitItem只追加危险防护说明；GTToolTooltips的耐久行属于GTToolItem，不会替服装补上。

两端原生ItemStack的耐久数字行要求高级提示且已经损坏，不能补齐原普通提示合同。因此登记421，范围是原耐久文字缺失，不是耐久机制、128上限或耐久条全部失效。没有据此改变英文／中文文本或未经实测宣称UI通过。

## 共用纹理路径与代码整理

RadiationSuitItem两版重复的HEAD0／CHEST1／LEGS2／其它3和路径拼接集中到private texturePath。Forge薄入口保留字符串命名空间，Neo入口保留ResourceLocation构造；不引入加载器类型到core。构造时的family验证、回退槽、公开类名和提示／护甲行为保留。

GTRadiationProtection展开六套／四槽注册、材料定义和事件边界，type／id改为armorType／itemId；core目录方法展开。三源106→176行，五份完整native／core视图在两旧路径正文和限定局部名称归一后保持，其余注释及原版权头保留。字符串命名空间只做列明的前缀分解，未宣称原始整文件字节相同。

最终core与Forge1.20.1／NeoForge1.21.1 compileJava实际通过，报告1分20秒。使用未变的同版本本地API适配器，生产Gradle配置未改，不声称默认离线元数据或GitHub CI恢复。没有新增镜像测试、执行护甲／tooltip游戏方法、GameTest、游戏、原版安装环境、存档重启、打包、推送或GitHub写操作。用户WoodMaterials.java／CompoundMaterials.java摘要保持。

可选IC2／Forestry等兼容、完整伤害／护甲磨损、所有配方配料／工具旗标、真实穿戴和悬停继续核对。421是累计差异编号，含已修与保留政策，不是待修数量或完成率，全面goal保持active。
