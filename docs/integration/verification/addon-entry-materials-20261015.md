# 2026-10-15 / 附属材料入口与形态

分支 `addon/materials-forms`，从 `origin/main` 的 `9f34cf05e` 切开。临时探针类 `AddonSpikeProbe` 和 `-PaddonSpike` 已经从源码和构建脚本里去掉。那次实验打出来的成品 jar 仍含探针，不能当正式包。

## 构造顺序

探针 addon `addon_spike` 对 `gregtech6` 声明 `ordering="BEFORE"`。构造方法打印线程名、线程 id 和 `nanoTime`。GT 构造开始时同样打印。

NeoForge 1.21.1 开发专服，三次 order，同一条 `modloading-worker-0`：

| 次 | 附属结束 | GT 开始 | 间隔 |
| --- | --- | --- | --- |
| 1 | 线程 99，`273682605219200` | 同一线程，`273682622014300` | 约 16.8 ms |
| 2 | 线程 101，`273801037143900` | 同一线程，`273801055601899` | 约 18.5 ms |
| 3 | 线程 102，`273916412182200` | 同一线程，`273916435643700` | 约 23.5 ms |

三次都在附属构造返回之后才开始 GT 构造，并且都进入了 `SERVER STARTED`。Gradle 分别是 2 分 27 秒、1 分 55 秒、2 分 10 秒。游戏日志在 `neoforge/build/addon-spike-neo-{1,2,3}/logs/latest.log`；构造的 `System.out` 当时在 Gradle 控制台。

Forge 1.20.1 开发专服三次都先打出 “Using 32 threads for parallel mod-loading”：

| 次 | 附属线程 | GT 线程 | 结果 |
| --- | --- | --- | --- |
| 1 | 34，结束 `272964203988200` | 35，开始 `272964200806099` | GT 早约 3.2 ms |
| 2 | 34，结束 `273184169971400` | 35，开始 `273184169958300` | GT 早约 13 µs |
| 3 | 34，结束 `273366554768199` | 35，开始 `273366555463599` | GT 晚约 0.70 ms |

摘录在 `work/addon-spike/logs/forge-order-1.txt` 和 `forge-order-2-3-touch.txt`。前两次构造重叠，第三次没有。`BEFORE` 不能保证 Forge 上“附属构造完整结束，GT 构造才开始”。

## ServiceLoader

GT 用自己的类加载器和当前 context 类加载器各调一次 `ServiceLoader.load`。开发环境三次 Forge、三次 NeoForge 都是 `own=1 context=1`，加载器是 `cpw.mods.modlauncher.TransformingClassLoader`。这是开发环境的共享加载器。

分开安装的成品专服各测一次，探针仍打在当时的 GT jar 里，services 文件在另一份 addon jar 里：

- Forge 1.20.1-47.4.20，`build/addon-spike-prod-forge`，`build/libs/gregtech6-1.20.1-forge-0.0.0.jar`（40,369,177 字节）加 `work/addon-spike/addon-spike-forge.jar`。`own=1 context=1`。附属线程 66，GT 线程 67，GT 开始 `274991246496900` 早于附属结束 `274991249292200`。`init` 从 `Loader_Materials` 进来。读到这行后进程被停掉，没有等世界加载完。日志 `work/addon-spike/logs/forge-prod.txt`。
- NeoForge 21.1.243，`build/addon-spike-prod-neo`，`neoforge/build/libs/gregtech6-neoforge-1.21.1-0.0.0.jar`（40,221,077 字节）加 `work/addon-spike/addon-spike-neoforge.jar`。`own=1 context=1`。同一线程 95，附属结束 `277704527632500`，GT 开始 `277704532674000`。`init` 从 `GregTechNeoForge.<init>` 进来。同样在这行之后停掉。日志 `work/addon-spike/logs/neo-prod.txt`。

## 构造方法里碰 Materials

Forge 开发专服 touch：附属线程 34 在构造里读 `Materials.Iron`，`init` 的调用栈是 `Materials.<clinit>` ← `AddonSpikeMod.<init>`，此时 GT 构造在另一条线程上跑 ServiceLoader。打印 `TOUCH error java.lang.ExceptionInInitializerError: null`，随后 `Loader_Materials` 抛 `Material initialization previously failed`。没有走到“队列已关闭”那条。Gradle 仍是 `BUILD SUCCESSFUL in 23s`、退出码 0，这次不能算通过。

NeoForge 开发专服 touch：`BEFORE` 让附属构造先跑完。碰 `Materials` 时还没 `bindPresence`，`TOUCH error java.lang.ExceptionInInitializerError: null`，GT 构造失败原因是 `Mod presence has not been bound by the platform`。ServiceLoader 仍是 `own=1`。Gradle `BUILD SUCCESSFUL in 22s` 同样不能算通过。

共享断言里，`init()` 之后再 `submit` 抛 `Addon material queue is closed`；阶段外 `registerDefinition` 抛 `Material definitions are closed`。这是支持路径上的报错，不是构造方法提前触发的那条。

## 依赖 id

`gradle.properties` 的 `mod_id` 是 `gregtech6`。内容和生成物品的命名空间仍是 `gregtech`。

## 材料实现与共享断言

`GregTechAddons.API_VERSION` 是 2。`init()` 顺序：收集 contributor、关闭队列、核心 `declare()`、按 addon id 收定义、核心 `link()`、addon `link()`、核心快照排除 addon 拥有的材料、发布形态、`READY`。id 段 4291–7899。撞名、撞 id、段外 id、`isValidFor` 失败都启动报错。已经生成过的形态对不再发第二个物品。

`:core:check` 于 17:05 通过，`BUILD SUCCESSFUL in 17s`。源码策略 2624 个 Java 源、125676 个原键、12624 个别名。addon 场景：service 3625 条（硝酸铵 `dust()` 6 个物品，铁的自动形态 96 个），name / id / addons / range / form 各 1 条，late 3612 条（含 4291–7899 全空）。core 行为 2806 条、33 组。材料 7959 条；1160 种材料、1105 个正 id。核心物品 id 金样仍是 SHA-256 `c3360a6cdc2ac4aa396b7a9bb778f11663171afb4e949709c65f3903f25a5fef`。机器规格 8035 条。热学 53 条。

`crystal` 不在自动前缀列表里。样例和断言追加的是 Diamond 的 `crystal`，物品 id `crystal_diamond`。铁上的 `crystal` 通不过 `isValidFor`。铁上再要一次自动的 `dust` 不会发出第二个 `dust_iron`。

两个平台的 `compileJava`，以及 `-PaddonSample=true` 下的两个样例源集，在 17:09 编译通过（`BUILD SUCCESSFUL in 1m 17s`）。Forge 样例有两处 `ResourceLocation` 构造弃用警告。

## 样例专服

样例在 `work/addon-sample/`，不进 git。当时的材料是硝酸铵，id 4291，`dust()`，化学式 `NH4NO3`，另加 Diamond 的 `crystal`。依赖 `gregtech6`，`ordering="NONE"`。`ServerStarted` 查到 `gregtech:dust_ammoniumnitrate` 和 `gregtech:crystal_diamond` 后 `halt(false)`。后面的客户端结果说明 `crystal` 不该放行，样例已去掉这个请求。

Forge 1.20.1 开发专服 `build/addon-sample-forge`，17:12:44：`Registered 1161 materials`，`Queued 56166 material items`。2026-10-14 不带样例的专服是 56159，差 7，等于 6 个粉形态加 1 个 `crystal_diamond`。17:15:07 打印 `[addon-sample] dust_ammoniumnitrate=true crystal_diamond=true`，随后保存并停服。`BUILD SUCCESSFUL in 3m 11s`。配方总数日志是 `Rebuilt GT runtime recipes before synchronization: 52814 recipes`，没有和上次的配方数对过。

NeoForge 1.21.1 开发专服 `build/addon-sample-neo`，17:15:59：`Registered 1161 materials`，`Queued 56166 Neo material items`。17:17:50 打印同一行 `[addon-sample] dust_ammoniumnitrate=true crystal_diamond=true`，随后保存并停服。`BUILD SUCCESSFUL in 2m 19s`。

两边都没有重跑不带样例的专服，56159 仍是 2026-10-14 的数。没有打不含探针的成品 jar，没有做箱子和坩埚的存档重开，也没有卸载样例后再加载。

## 客户端看到的形态缺口

Forge 1.20.1 开发客户端带样例和 EMI，18:08 正常退出。硝酸铵能用 Ammonium Nitrate 搜到，提示里有化学式和 “Added by the addon_sample addon”。EMI 重载打了 82046 行 `Untranslated tag`，主体是已有的 `#forge:storage_blocks`、`#gregtech:dust`、植物和箱子标签，不是配方失败。

`gregtech:crystal_diamond` 进了新建的 Crystals 分页，里面只有这一件。名字是未翻译的 `item.gregtech.crystal`。各材质集没有 `crystal.json`。加载日志仍写 56166 个材料物品都对上了模型、0 个缺失，缺文件时用的是缺失模型占位。

随后收紧：`forms()` 只能点名 `candidatePrefixes()` 里的前缀。`crystal` 启动时报 `is not a generated form`。钻石的 `plate` 仍报 `not valid`。样例不再请求晶体。`:core:check` 于 18:16 通过，`BUILD SUCCESSFUL in 18s`。service 3630 条，硝酸铵仍是 6 个物品，铁的自动形态仍是 96。form、invalid 各 1 条。核心物品 id 金样不变。这次没有再开客户端。

生成形态从 `MaterialItemDefinitions.all()` 进两个加载器的 `gregtech` 注册。按命名空间把 `gregtech` 当成 GT 内容的分支会带上这些物品，例如 `GTFoodItems` 只在命名空间不是 `gregtech` 时返回 null。这些检查没有改。
