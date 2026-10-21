# 液体泉与基岩矿生成调度核查

2026-10-22，第一百零六批，基线 `b453cf590` 加本轮工作区。上一轮已有实际提交，本轮重读调度和注册消费者。10份原版Java、15份当前Java、10项资源、24份Java视图；四个本地原生条目及24段具体引用见[回执](verification/spring-bedrock-dispatch-audit-20261022.json)。来源作者／版权、LGPL及字节摘要归档，原树只读。

## 已确认差异

| 编号 | 原版实际调用链 | 当前实际调用链 |
| --- | --- | --- |
| ALIGN-353 | GT6WorldGenerator先统一reset，再按列表逐项调用；七泉依次roll，首个通过者关闭后续泉的资格。 | 先均匀抽七种之一，再按它的概率roll。合格入口、默认参数、无先前矿场／特殊生成、独立均匀roll的理想模型下，任一泉机会原约2.964%，现约0.429%。这是数学模型，不是世界实测密度。 |
| ALIGN-354 | 基岩矿成功置GENERATED_NO_BEDROCK_ORE=false；泉在概率通过后置CAN_GENERATE_BEDROCK_ORE=false，两方共同读标志。 | 两feature没有相应区块协调；泉的底部检查还允许基岩矿。两端资源将矿放在underground_ores、泉放在后续underground_decoration，原生枚举／消费者顺序已核。阶段不同不等于互斥；若都获准，其写入意图有交叠。 |
| ALIGN-355 | 每个启用的基岩矿注册项在各区块自行roll，矿成功并不关闭CAN标志，允许后续矿再次尝试。 | 固定每16×16区块一个候选，选一种逆稀有度权重。不是原逐项概率分布；整数权重还会量化，例如128000／96000对应7／10，而非精确逆比3／4。床岩、维度与有效写入仍会限制结果，不宣称每格必生成。 |
| ALIGN-356 | GEN_FLOOR是列表数组，包含主世界和下界。默认33条共享床岩矿进入两者，下界另加7条专属，即默认源注册33／40。 | 主世界表28条、下界仅7条专属。考虑Au／Gold、Cu／Copper、V2O5／VanadiumPentoxide别名后，共享表仍未列Fe2O3、Sphalerite、Smithsonite、Pentlandite、Niter、Bauxite、Cassiterite七材质；有Zinc／Nickel条目替代部分含矿材料，Chalcopyrite声明2000变4000。只针对该基岩矿入口，不说这些矿在全世界都不存在；可选Hexorium和默认关闭自定义项不混计。 |
| ALIGN-357 | 六路roll的1、2明确调用oreSmallBedrock，与完整基岩矿不同。 | core仍发small=true，但平台sink丢弃它，统一调placeBedrockOre，查普通ore_块；OreBlock以注册prefix识别small。上层普通小矿仍传该标志，未报为所有小矿缺失。 |
| ALIGN-358 | 原指示物按4／8／16三层各尝试一次，共28候选，最大32格范围；花与石共享取舍及地面条件。 | 当前6次花、3次石，分别落在当前候选中心附近并限制于本区块。候选分布与原多尺度核不同，不能从尝试次数直接推算成功数量。 |

扩展ALIGN-004的配置消费者：原泉的Enabled／Probability／逐维度启用、GenerateInfiniteOilSources／GenerateInfiniteGasSources，以及床岩矿的Ore／指示物开关没有对应完整入口。仅登记，不在本批实现CFG；用户要求 #66 CFG后续、#71–#74跳过保持。

## 有界验证与已有对应

只编译并执行两个实际纯core写入器和一个记录探针，约2秒；没有编译整模组或执行原生Feature。固定有界回调下记录841个普通矿拟写入坐标、556个液体拟写入坐标，其中424个相交；另一次回调记录36次small-bedrock请求。探针记录拟写入，不模拟宿主许可、真实随机、区块保护或游戏中被覆盖数量；代码及输入随回执保存。

Forge枚举／区块生成器实际反汇编、Neo对应源码确认ORES在DECORATION前；这只是本机工件的静态顺序证据。七条下界专属材质和声明概率已有对应，但不能代替共享33条。两个Gold记录有不同原始名称／花，属于源目录的两个入口。原Config默认给属性名加默认值后缀，也不能仅凭重复stibnite类别名就断言概率键碰撞。

现代最低高度和原生维度键适配保持，未为对齐改回Y0。原全局static标志依赖旧串行调度，后续不能直接复制为当前协调方案。

## 源码整理

五源502→581行：MineralWorldgenRules、GTBedrockOres、GTBedrockOreFeature、GTOreBlockResolver、GTBedrockOreDimensions。展开循环、分支和回调，保留已有目录对齐；纠正“完整原版”“仅两个独立维度表”“可破坏核心”“每轴独立漂移”等说明，实际默认参数／算法未改。

删去placeSmallBedrockOre和placeSprinkle两个私有包装。追踪Java及维护脚本／构建源码中的名称引用只发现其声明；这不排除任意动态反射，但它们不是公共API。四源六视图完整代码保持，另一个类的两视图在显式去掉这两个私有方法后，其余代码／字面量／注释／版本指令保持；回执保存删除文本和比较范围，不伪称删除前后所有token相同。用户两材料摘要保持。

原生采掘、真实新世界分布／跨区块恢复及完整存档未验。本批未运行Gradle、整模组编译、GameTest、游戏、构建或推送。后续继续特殊生成区域保护、其它矿脉／地质及全系统调用链和源码队列；正式差异记录不是未修复数或完成率，全面goal保持active。
