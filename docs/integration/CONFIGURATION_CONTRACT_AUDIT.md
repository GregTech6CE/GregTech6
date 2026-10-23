# 配置架构与原版代表合同审计

第六十一批，2026-10-21。观察基线b11081f4f，后续f252bcf5d文档更新单独留痕。扩展既有ALIGN-004，起火概率沿用133，不按每个配置键新增记录。原版可配置性只完成代表矩阵，完整适用性与磁盘／重载／多人／独立重启验收继续；本批未修改生产配置或玩法。

[统一回执](verification/nuclear-transitions-config-audit-20261021.json)留存19份原版／38份当前路径身份、14份共享维护源的28份显式平台视图，以及四个本机原生工件的15个entry。九个加载器类曾用javap只读查看字节码，保留命令／成功状态／class-entry身份，没有执行其配置方法，也未复制原生完整方法体。原始根为 `C:/Dev/gregtech6-master/gregtech6-master`；来源按root标识＋相对路径可重定位。原版GregTech-6 Team／Gregorius Techneticies头及LGPL-3.0-or-later保留，当前NOTICE沿既有归属；Forge／Neo依赖仅作只读分派证据，不导入其实现或扩大许可。

## 既有记录需要更新，而不是按键增加编号

ORIGINAL_ALIGNMENT_AUDIT中的ALIGN-004早期写“只定义日志与过压爆炸两项”，本批已更正。实际[GregTechConfig.java](../../multiversion/src/main/java/com/gregtech/gregtech/GregTechConfig.java:16)双端均定义三项：

| 配置路径 | 默认 | 当前真实状态 |
| --- | --- | --- |
| logMaterials | true | 有原生spec、可以落盘；没有生产读取者，不能控制材料注册数量日志。 |
| machines.explode_by_overload | false | 有注册、getter和真实机器消费者。 |
| machines.break_by_overload | false | 有注册、getter和真实机器消费者；explode=true时有效breaking也为true。 |

适宜扩展ALIGN-004的证据是“可配置覆盖范围仍有限，且有声明但未消费项”，不再称全部配置未接入。起火概率已有ALIGN-133，环境机器危险已有ALIGN-017；不为这些代表键重复编号。cfg收尾安排仍是已接受的进度政策，不代表应立刻恢复所有旧版选项。

## 原版加载／保存并不是只有配置常量

原[GT_API.java](C:/Dev/gregtech6-master/gregtech6-master/src/main/java/gregapi/GT_API.java:289)289–294创建GregTech、Recipes、WorldGenerationNew、旧GT5世界生成、Materials、OreProcessing等配置；GREGTECH显式setUseDefaultInNames(false)，其它文件默认把默认值附加到键名。Config.java构造器88–91创建Forge Configuration、load、save并加入Abstract_Mod.sConfigs；Abstract_Mod在初始化阶段及异常路径多次执行该保存列表。

原Config.get各重载读取Property，在模组初始化收束之后也会为首次读取的动态项保存；run()保存主文件及待存ID配置。WorldgenObject还可首次遇到维度才读取并缓存维度许可。因此原配置是实际读取与保存机制，不是仅静态声明。但这也不等于原所有参数都能游戏内即时重载：例如GT_API:250–256在模组初始化时把材料参数写入对象，工具创建再消费它们。

旧数字ID配置、旧模组可选联动、废弃GT5生成器等须按现代适用性判断，不能因没复制旧文件名就统一登记功能缺失。没有要求复刻旧大写／小写改名和删除重复旧文件的行为。

## 双端原生注册到加载／缓存／保存已经接通

Forge主类[GregTech.java:60](../../multiversion/versions/1.20.1-forge/src/main/java/com/gregtech/gregtech/GregTech.java:60)调用ModLoadingContext.registerConfig(COMMON,SPEC)；Neo主类[GregTechNeoForge.java:34](../../multiversion/versions/1.21.1-neoforge/src/main/java/com/gregtech/gregtech/platform/neoforge/GregTechNeoForge.java:34)调用ModContainer.registerConfig(COMMON,SPEC)。当前MOD_ID为gt6ce，没有传自定义文件名；原生defaultConfigName采用`%s-%s.toml`，对应默认gt6ce-common.toml，和旧gregtech.cfg并非同一格式。

本机Forge47.4.20的ModStateProvider:64–68在COMMON_SETUP前调用ConfigTracker.loadConfigs(COMMON,CONFIGDIR)；Neo21.1.243的CommonModLoader:62–64同样加载COMMON。FML字节链可见注册→track、open/read、配置修正及写回、文件watcher；Neo的LoadedConfig.save经ConfigTracker.writeConfig写盘。没有模组自己调用旧Config.run，不表示现代文件不保存。

两端ConfigSpec的Builder.define(String,…)会拆分点号路径，所以两个machines键进入同一配置子表。ForgeConfigSpec.setConfig:79–94修正后保存并afterReload，118–129清ConfigValue缓存；NeoModConfigSpec.acceptConfig:113–125与afterReload:160–170同样处理，当前值没有restart标记，默认RestartType.NONE。实际watcher可重新load并投递事件，但本批没有改写磁盘、触发watcher或模拟加载环境，不能宣称热重载已经实测。

原生边界不同而当前后备一致：Forge的ConfigValue.get在开发环境未加载时抛IllegalStateException、生产环境可回默认；Neo getRaw在未加载时抛IllegalStateException。当前两个getter均捕获该异常并回false。SPEC在Neo为public、Forge为包可见，是Neo入口跨包访问的边界，不是漏注册。

没有发现模组自己的ModConfigEvent字段复制处理，但两个机器选项直接调用ConfigValue.get，不依赖复制到普通静态字段；因此不能用“没有事件handler”宣布这两个配置失效。

## 过载两项是已有真实玩法消费者

原GT_API:578–587读取machines的爆炸／破坏选项，overload两项默认均false；TileEntityBase01Root.overcharge:494–504按爆炸优先、否则单块移除消费。当前GregTechConfig:50–64通过原生value getter返回，breakByOverload首先检查explodeByOverload；[MachineOverload.apply:26](../../multiversion/src/main/java/com/gregtech/gregtech/api/energy/MachineOverload.java:26)之后只在服务端按有效选项执行，默认仍保留过载声音／记录。

| explode | break设置 | 当前配置效果（源码真值表） |
| --- | --- | --- |
| false | false | 不按通用过载配置破坏机器，仍有过载反馈。 |
| false | true | 0.1强度路径，只移除机器，不产生范围爆炸。 |
| true | false／true | 有效breaking=true；按能源类型／功率走范围爆炸或单块移除。 |

真实入口包括BasicMachineBlockEntity.overcharge:1270、EnergyNodeBlockEntity:598／911–912、LongDistanceTransformerBlockEntity:116／124等，后者发送与接收侧都调用共用策略。没有重核每台机器危险门槛；锅炉压力及原版直接explode特例不受这两项全部覆盖，不能把默认false说成所有机器绝对安全。

旧machineOvervoltageExplosions自动生成true的键已按既有安全默认决策弃用；当前不读取它是有记录的政策，不能当成遗失旧配置映射。也不能只看CS字段初始值就推断原默认，原GT_API的实际配置读取才是本项依据。

COMMON配置不会自动作为SERVER配置同步：实际Forge／Neo ConfigSync只取SERVER集合。但当前过载效果已服务端门控，本地客户端普通配置不同不会在本路径决定服务端删块，因此不单独报告“未同步必定导致机器危险”。原PacketConfig也只传三种硬度倍率，不曾同步所有旧键。

## 声明但未消费的日志项

LOG_MATERIALS只在GregTechConfig:17／19声明、22定义；public静态logMaterials在48行另赋true，既不从value更新也没有生产读取者。全生产源码搜索已记录，不能把两个同名样式的字段视为隐含关联。

Forge主类97–98无条件记录材料／物品／方块数量；Neo主类166–169无条件记录material objects。将文件logMaterials改false，按当前代码没有任何分支消费它。此为一个现代移植自有设置无效，不主张原GT必须有同名日志键；宜归入ALIGN-004的声明与消费合同，不新增按键编号。

## 原版代表矩阵与配置收尾范围

| 代表族／原设置 | 原注册至消费链 | 当前真实行为／限制 |
| --- | --- | --- |
| 石层模式及维度许可 | Loader_Worldgen:59注册stonelayers默认true；WorldgenObject:49读取Enabled、64–70读取维度许可；StoneLayers:54写GENERATE_STONE；GT6WorldGenerator:125决定石层或旧主世界矿脉。 | GTWorldgenDefaults:6为final true，StoneLayerFeature:75、GTOreVeins:79、GTOreVeinFeature:54和WorldgenInfoData:63真实消费。默认选择已经对应；没有配置关闭模式／切回旧矿脉。两端biome_modifier资源安装两Feature；数据包可替换／移除现代Feature，但只移除石层不会令activeOverworldVeins由空列表变为旧矿脉表。 |
| 生物掉落铅／肉类／杂物等开关 | GT_API:519–523默认true；Override_Drops:686／690／705按开关守卫实际箭、铅替换、肉类分支。 | MobDropEvents是双端实际服务端LOWEST监听，115–145等处直接执行默认路径，MobDropRules.Context也没有开关参数。已有铅替换、肉类变化，不能报整族未实现；原设置为false时可取消相应分支，当前COMMON spec没有这种入口。普通数据包改原生掉落表并不自动取消事件里的后续替换。 |
| 每材料工具与处理倍率 | GT_API:250–256从Materials／OreProcessing写入倍率、quality、speed、durability和handle；MultiItemTool:182在创建工具时消费耐久。 | GTMaterial有setToolStats，ManualToolRules:8–12经GTToolHelper计算实际工具耐久，来源数据存在；生产配置没有材料覆盖矩阵。编辑源码／生成表或通过addon API改材料不是已提供的内置配置。未来应区分初始化覆盖与运行期已有工具数据，而非一律热改。 |
| FlintAndSteelChance | GT6_Main:111限1–100，默认30；原Behavior_FlintAndTinder:51／53及GT_Proxy:270读取。 | GTToolItem:101–104固定传DEFAULT_FLINT_CHANCE=30；默认相同，可配置概率未接入。已有ALIGN-133，不重复编号，也不重审随机分支ALIGN-127。 |
| Nexus／roads及中心五模块 | 原Loader_Worldgen:646–650五项默认false，各原Worldgen对象可配置。 | GTOriginFeature:44–53主动启用中心群系、道路、信标、Nexus、testing，OriginSpawnEvents同步出生位置。明确要求先全部开启、cfg收尾再做；当前全开是授权选择，不能作为原默认差异缺陷。本轮不改为false。 |

矩阵确认“原版可选、当前只有默认行为”与“默认行为本身错误”是不同问题。它不是完整配置清单，不能按旧所有键的数量计算缺陷，也不将此前明确延后的cfg工作说成未经授权的改动。

## 下一轮未覆盖类别及验收限制

本轮没有逐条核原Recipes按行启停／数量／能耗、完整Materials／OreProcessing可修改性、工具／原版物品接管开关、库存／饥饿／营养／实体行为、客户端可见性／声音／调试、完整世界生成概率／矿物／维度矩阵、所有外部模组兼容和旧数字ID键的现代适用性。机器环境火／雨／水风险已有独立合同，未在这里把行为缺失再拆成每个配置缺失。

没有实际启动生成／修正toml、改文件触发重载、错误值／无权限／只读文件回退、旧文件迁移、世界重启、跨服配置或多人同步验证。源码已证明原生接线与两个过载消费者存在，不等于磁盘、打包依赖和真实游戏验收；日志无读取与代表族缺乏配置是本内置源码的明确边界，不排除外部模组／脚本另加设置。
