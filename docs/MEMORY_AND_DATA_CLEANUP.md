# 创造物品栏内存与旧数据入口清理

2026-09-11。复现条件：打开创造物品栏、向下滚动一段时间后，任务管理器中的 Java 进程约占 5000 MB。本轮没有运行中的游戏进程可供附加采样，因此尚不能判断主要是存活堆对象、频繁分配后保留的堆容量，还是原生/图形驱动内存。

## 材料注册是否是主要原因

在当前工具使用的独立 JVM 中，1142 个材料及其初始化依赖的 GC 后堆增量约为 11.47 MiB。这是单次启动测量，不是材料对象的精确 retained size，不包含客户端资源、方块/物品完整注册、纹理图集和世界，也不能直接与任务管理器进程内存相减。

材料目录、`Materials` 引用表和导入符号表共享同一批对象；拆 Java 文件不会复制整套材料。名字/ID 索引和静态别名有少量引用开销，但没有观察到随创造栏滚动而追加材料定义的路径。

本地 GTM 参考：`MaterialRegistry` 以 ResourceLocation 注册同一材料对象，并分阶段关闭/冻结；`GTMaterialItems.generateMaterialItems()` 按前缀及材料属性筛选所需物品形态。它也会创建许多物品，不能认为改用 Builder 或抄它的注册容器就能解决高内存。未复制 GTM 源码。

## 本轮已修改的预览路径

- 流体预览以前每次 getQuads 都复制顶点数组并重新映射 UV；现在复用已转换的 quad，每个流体预览最多缓存 128 个。
- 流体模型缓存改为最多 256 个最近使用条目，键包含基础模型和贴图；每次资源重新烘焙清空。旧静态缓存不会再跨重载保留先前的模型/贴图引用。
- 相同 overlay 的矿石共享 OreBakedModel，材质颜色继续由方块/物品染色处理器提供。几何缓存不再随每种矿石材料各复制一份。
- 矿石网格缓存上限为每个共享模型 512 项，物品石材变体上限 64 项；未知石材 NBT 归一到原有默认石材。缓存同步访问，避免共享后并行区块构建写入 EnumMap 的问题。
- 材料方块共享染色包装模型。已有 tintindex 的网格直接返回；需要改 tintindex 的 quad 使用有上限的缓存。
- 板条箱的背包模式直接复用完整网格列表，不再每次绘制拷贝一遍。

这些修改减少重复分配和缓存保留，但还没有真实场景下的前后 JFR/堆数据，不能宣称已经消除 5 GB 占用或确定了唯一泄漏原因。

## data 目录清理

删除 `AM/ANY/BI/CS/FL/FM/IL/LH/MD/OD/OP/RM/TC/TD.java` 共 14 个旧简称入口；`MT.java` 的导入别名和特殊行集中到 `ImportedMaterialData.java`。没有建立第二份材料表。

手写内容使用 `Materials`、`MaterialPrefixes`、`MachineRecipeMaps`、`RegisteredFluids` 等完整名称。原版导入器需要保留的短符号只在 `ImportedMaterialData` 中维护。原版参考源码没有修改。工具路径、生成代码和测试已迁移，生成回归检查覆盖新目录。

这是 Java 源码入口的调整，不是存档注册 ID 变更。若另有未提供的附属模组源码导入旧类，需要一起改 import；旧类不再保留兼容入口。

## 检查与进一步采样

```text
python -m unittest discover -s tools/tests
python tools/check_registration.py --offline
```

后者包括材料身份/初始化、客户端辅助函数，以及 100000 个不同预览键仍限制在 256 个缓存项的压力检查，并输出材料初始化堆增量。显式 GC 只存在于测试探针，不在正式游戏代码中。

游戏中继续复现时，可以用任务管理器中的**游戏 Java PID**（不是 Gradle PID）采样：

```text
python tools/capture_client_memory.py --pid 12345
```

把 12345 换成真实 PID。脚本读取 JVM 参数和堆使用情况，然后录制 60 秒 JFR；启动后立即重复滚动创造物品栏。结果在 `build/memory/时间戳/`，不会上传、不强制 GC、不生成完整 heap dump，也不修改内存上限。

比较采样中的分配热点、GC 后存活量和进程内存增长，才能决定下一步应处理工具预览分配、其他模型缓存、纹理/原生内存，还是 JVM 已提交但未归还的堆。`gradle.properties` 中的 `org.gradle.jvmargs=-Xmx3G` 属于 Gradle 进程，不是游戏 JVM 的内存限制。
