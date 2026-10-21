# 工具参与的工作台配方源码导航

第六十二批，2026-10-21，基线c4a95c5c4。本次整理两份现有工具配方执行类的长行、循环与序列化边界，未修改配方玩法、不新增ALIGN编号。旧清单中的FiniteBottleFillingRecipe、GeneratedRecipeSink和GTEngines此前已经展开，本批重新读取后没有重复改写。

## 先区分工具制作与工具参与合成

以下原生路径相对于`multiversion/src/main/java/com/gregtech/gregtech/`，专有版本文件另标。

| 要找的职责 | 当前入口 |
| --- | --- |
| 合成中使用工具、镜像／空容器限制、颜色匹配及工具返还 | `recipe/ToolShapedRecipe.java` |
| 无序合成的工具返还、材料形态过滤及格位选择 | `recipe/ToolShapelessRecipe.java`，core `content/recipe/FormConversionSelector` |
| 实际有限流体容器匹配与排出1000mB | `recipe/FiniteBottleFillingRecipe.java` 的ContainerIngredient |
| 锯／锤等工具本身的头、柄和整体制作 | `recipe/GTToolRecipes.java`、GTToolPatternRecipe／GTToolHeadRecipe／GTToolAssemblyRecipe等；GTToolPatternRecipe又继承ToolShapedRecipe |
| 两个包装配方的Forge serializer注册 | Forge专有`bootstrap/content/ToolBootstrap.java`的tool_shaped／tool_shapeless |
| 两个包装配方的Neo serializer注册 | Neo专有`recipe/NeoRecipeSerializers.java` |
| 工具种类／pattern索引等专用配方的编码 | `recipe/GTToolRecipeSerializers.java`，不要把它与两个包装类的SERIALIZER混成同一入口 |
| 生成的原版机器行临时接入原生解析器 | core `content/recipe/generated/GeneratedRecipeSink.java`，不是工具返还逻辑 |

这些是真实源码职责导航，不表示所有工具、耗能、配方转移或查看器合同都已验收。

## 有序配方的阅读顺序

先看构造器保存allowMirror、requireEmptyFluidContainers、autocraftable和constructionColor。matches先用允许镜像的原生匹配或matchesUnmirrored，再检查颜色和toolsUsable；displayConstructionColor只处理直接物品引用，不提前解析tag成员。源码原有tag绑定时序说明保留。

matchesUnmirrored按left／top尝试格位，逐y／x匹配。getRemainingItems先取原生返还列表，再逐槽处理电动工具能耗或普通工具磨损，最后调用drainFiniteIngredients；电动工具分支的continue只结束当前槽，不能误读成跳过最终的容器返还。

drainFiniteIngredients另做允许偏移／镜像的实际格位映射，只在匹配的流体槽返还排空容器。此次展开显式显示left→top→mirror、y→x两组循环和找到首个匹配后的return，没有改变循环范围、退出位置、流体量或工具损耗。

## 无序配方与版本边界

ToolShapelessRecipe保留材料输入／材料身份成对校验、空容器限制、工具可用性及FormConversionSelector。Forge在匹配循环记录first／occupied；Neo仍经CraftingFormPosition查询原格位上下文，未为共享代码删除该差异。返还过程沿现有GTToolItem判断及损耗实现，本文没有额外证明其它物品类型的完整合同。

两版数据／网络序列化API不同。Forge的buffer读写顺序、Neo Codec字段顺序／默认值、原生base配方委托均保持；没有把看起来相似的字段重新排序或将版本类体强行合并。

## 具体整理与保持证据

| 维护类／平台 | 行数前→后 | 最长行前→后 | 完整执行词法项 |
| --- | ---: | ---: | ---: |
| ToolShapedRecipe Forge视图 | 148→233 | 499→150 | 2040 |
| ToolShapedRecipe Neo视图 | 127→247 | 588→150 | 2101 |
| ToolShapelessRecipe Forge视图 | 118→152 | 238→148 | 1157 |
| ToolShapelessRecipe Neo视图 | 113→171 | 242→149 | 1251 |

维护输入分别254→456、168→247行。ToolShapedRecipe的一处Neo循环闭合括号从后一masked体开头挪到前一masked体结尾，使随后共用的闭合括号在两版本都结束方法；这是条件载体的边界整理，不改变任何选中平台的控制范围。原条件指令文本与顺序保持。

四份真实平台视图的全部执行词法、字面量原位置及实际Java注释逐项相同，不需要参数改名归一。原始Git／工作树与最终文件身份分开记录，详见[回执](verification/tool-recipe-readability-20261021.json)。独立审阅确认循环、工具返还、Codec和共用括号范围清楚；两处额外空白建议采纳后再次复核完整视图。

没有运行生成器、Gradle、Java初始化／编译、新测试、游戏、实际配方重载／转移、独立存档重启、构建或推送。当前行为保持不证明原版玩法全部对齐；全面审计、其它方法职责和真实运行验证继续。用户CompoundMaterials／WoodMaterials的排版未改。
