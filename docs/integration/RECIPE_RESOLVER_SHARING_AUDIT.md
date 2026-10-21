# 化学加载器与生成配方解析器共享

第五十七批，2026-10-21。ChemicalRecipeLoader以`e5c6b2021`、GeneratedRecipeResolver以`93b993c95`为比较基线；两文件之间没有其它生产变更。沿第五十五批全量盘点继续去掉最后两处整类重复，不新增ALIGN行为记录，正式账本仍201条。

## 共享主体与保留的条件

| 维护文件（共享Java根内） | 维护行数 | 实际调用与共享范围 | 必要条件 |
| --- | ---: | --- | --- |
| `content/recipe/ChemicalRecipeLoader.java` | 605→216 | MachineRecipeBootstrap运行run；盐水／氯碱、水电解、酸／氨、油／生物质蒸馏及辅助查询 | 三处：导入、make的全限定holder声明／查询、gasOrLiquid的holder声明／查询 |
| `content/recipe/generated/GeneratedRecipeResolver.java` | 1170→425 | bootstrap最后运行loadAll；公开物品／流体解析还供战利品、主配方、聚变、电池等调用 | 六处：导入、末两表顺序、v查找、tech命名空间完整表达式、tech注册表查找、named的holder声明／查询 |

两文件共1775→641行，再减少1134维护行；第五十五批四个整类候选合计2729→975行。行数包含空行和注释，不代表功能或配方数，也不把剩余原生接口差异当重复业务。

每文件现在只有一个package、一份公开类和共同业务，原有公开签名、日志字符串、解析语法、数值、跳过／null策略、缓存、异常处理、反射回退、催化剂及计数操作均保留。没有要求调用方更名或更换注册入口。

解析器仍保留 **Forge先combs后technology、Neo先technology后combs**，前八张表顺序相同且未改。Forge的`ForgeRegistries.ITEMS.getValue`与Neo的`BuiltInRegistries.ITEM.get`、`GregTech.NAMESPACE`全限定表达式与Neo的`"gt6ce"`字面量，均留在各自条件内；没有为统一写法替换成当前值恰相同的另一种表达式。

原生变量只在导入／语句条件处替换，没有增加Java运行时分支或局部作用域。Chemical的fluid在原make方法／gasOrLiquid循环中声明并继续使用；Resolver的tech key声明与查找两处相邻条件对同一平台配套选择。named原有无花括号的lava返回及后续holder声明保持原控制范围。

## 注释准确性

初次机械合并沿用原Forge注释，Neo仅改变Javadoc分隔与排版。root随后纠正少量既有过强说明，最终源hash与这个中间快照分开保留：

- Chemical只说明当前1000mB helper与显式水数量，不把本地单位写法冒称所有GT6流体数量合同，也不继续写未经本轮确认的“剩余约350条”。
- Resolver不再把每个未解析token都解释成已批准永久缺失的外部内容；本地定义或查找缺口仍需单独核查。旧`accepted-missing`日志字样未动。
- loadAll指向真实MachineRecipeBootstrap入口，不继续指向已移除的旧Loader命名。
- Resolver的added在`map.addRecipe`返回非null后增加，skipped也包含该调用拒绝；与另一些加载器只统计提交次数不同。注释不将它们混为相同统计合同，也不因此宣称最终可查询配方全集已经验证。

其它原版归因、配方数量／守恒及缺失token仍需行为审计；本次共享不自动批准所有旧说明或修正化学玩法。

## 完整执行保持与边界

| 文件 | Forge完整执行词法项／字面量 | NeoForge完整执行词法项／字面量 |
| --- | ---: | ---: |
| ChemicalRecipeLoader | 1919／66 | 1943／66 |
| GeneratedRecipeResolver | 2966／59 | 2970／60 |

root从各自Git基线重新读字节，展开维护源中的Neo注释体及嵌套转义后，显式选择每平台，逐项比较完整执行流与字面量token位置；包含包／导入、全限定类型、操作符、数值及调用顺序，不用相似度或删空白字符串代替。注释纠正后的四份最终视图全部相同；解析器十张表的调用序列另行提取，两端分别与基线一致。

[回执](verification/chemical-resolver-sharing-20261021.json)保存基线、中间共享快照、最终源／视图hash、字面量位置、表顺序及安全工具身份。独立只读审阅也核到条件成对选择、声明作用域和原生API，未发现本轮执行回归；没有把只读审阅当编译或游戏验收。

没有生成器、Gradle、Java编译／初始化、新测试、运行期配方表查询、游戏、构建或推送。用户材料表保持未动。第五十五批四个整类候选现已共享，但171项未审方法筛选、过大职责、其它原生注册边界和全面原版差异继续，见[全量盘点](NATIVE_BODY_SHARING_AUDIT.md)。
