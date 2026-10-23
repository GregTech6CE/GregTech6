# 开发客户端主菜单冒烟测试

日期：2026-09-30。Root报告两平台bootstrap编译通过（38s）；Neo真实客户端运行38s正常退出，SUCCESS为8034ms，1280×720截图已人工查看为正常主菜单。Forge首跑在120秒资源加载窗口超时，记录FAILED；即使Gradle退出码0也不算通过，准备以240秒预算复跑。最新超时诊断修改的编译/运行仍待Root串行执行。实现只在 Forge/Neo 的 bootstrapGameTest 源集；子任务没有修改生产 main/core、Gradle或启动游戏，运行属性由Root接入。

目的：确认开发客户端到达实际绘制的原版 TitleScreen，保存可检查的本地截图，然后正常退出。通过不代表成品jar启动、进入世界、模组内容渲染、完整玩法、专服或世界保存/重载通过。

## 已核对的本机 API

Forge本机解析源码：

`C:\Users\Asus\.gradle\caches\forge_gradle\minecraft_user_repo\net\minecraftforge\forge\1.20.1-47.4.20_mapped_official_1.20.1\forge-1.20.1-47.4.20_mapped_official_1.20.1-sources.jar`

- `net/minecraftforge/client/event/ScreenEvent.java:159–241`：Render.Post是屏幕绘制后的客户端Forge总线事件，有getScreen/getGuiGraphics。
- `ForgeHooksClient.java:425–429`：screen.renderWithTooltip后发布Post。`GameRenderer.java:965,998`：调用屏幕绘制后才最终GuiGraphics.flush，所以测试在截图前明确flush。
- `net/minecraft/client/gui/screens/TitleScreen.java:233–244,279–283`：两秒淡入主菜单控件，不能只等第一帧。
- `net/minecraft/client/Screenshot.java:37–83`：实际签名是 `grab(File,String,RenderTarget,Consumer<Component>)`；从当前主渲染目标获取NativeImage，后台writeToFile成功后回调，失败回调screenshot.failure；截图事件可以取消/重定向。
- `net/minecraft/client/Minecraft.java:815,1551–1553,2774`：getMainRenderTarget、stop、getOverlay可用；stop设置running=false并发关闭事件。
- `javafmllanguage-1.20.1-47.4.20-sources.jar`中 `AutomaticEventSubscriber.java:61`以Class.forName(...,true,loader)初始化客户端限定订阅类；故opt-in watchdog在客户端自动订阅阶段启动，而非等待主菜单出现后才开始计时。

Neo本机解析源码/类：

`F:\Dev\GregtTech6New\gregtech6\neoforge\build\moddev\artifacts\neoforge-21.1.243-sources.jar`

- `net/neoforged/neoforge/client/event/ScreenEvent.java:146–224`：同样Post/getGuiGraphics，使用GAME总线与net.neoforged.bus.api.SubscribeEvent。
- `ClientHooks.java:427–430`：屏幕绘制后发布Post。
- `net/minecraft/client/gui/screens/TitleScreen.java:53,266–283`：FADE_IN_TIME=2000ms，测试等待2500ms。
- `net/minecraft/client/Screenshot.java:38–83`：本版本同样四参数grab及后台写完后回调；`Minecraft.java:841,1599–1601,2837`对应渲染目标/stop/overlay。
- 使用Java21 javap核实 `neoforge-21.1.243.jar` 的Screenshot.grab和Minecraft方法签名。
- 使用已解析 `loader-4.0.43.jar` javap核实 `net.neoforged.fml.common.EventBusSubscriber` 与Bus.GAME/Bus.MOD；`net.neoforged.fml.javafmlmod.AutomaticEventSubscriber`字节码调用Class.forName第二参数true。不是假设Forge注解包也能用于Neo。
- 两平台 `TranslatableContents.getKey()` 均通过javap核实，用于识别失败回调而不依赖系统语言。

## 行为与判定

Forge文件：`src/bootstrapGameTest/java/com/gregtech/gregtech/integration/client/ForgeClientSmoke.java`。

Neo文件：`neoforge/src/bootstrapGameTest/java/com/gregtech/gregtech/integration/client/NeoForgeClientSmoke.java`。

只有游戏JVM的 `gregtech.integration.clientSmoke=true` 才启动测试和daemon watchdog；默认false不截图、不退出、不创建watchdog。两个类都以Dist.CLIENT限定，不在专服装载Minecraft客户端类型。

测试等待当前活动screen就是事件里的TitleScreen、overlay为null，累计至少5个Post渲染帧，同时稳定2500ms，以避开原版两秒淡入。切换屏幕或出现overlay会重新计帧。测试不关闭提示、不点击菜单、不创建/加载世界。

以原版Screenshot.grab读取真实主渲染目标，在当前gameDirectory/screenshots/创建带平台、Minecraft版本及UUID的新文件；不会接受上一次运行留下的PNG。截图回调失败、取消/改变目标导致没有预期文件、文件为空或不能解码时，记录FAILED。成功回调中读取实际新图片尺寸，完成验证后通过Minecraft.execute在主线程调用stop。调用grab后不会立即退出。

机读日志标记后面是单行JSON：

- `CLIENT_SMOKE_STARTED`：platform、minecraft、elapsedMs、timeoutSeconds。
- `CLIENT_SMOKE_SUCCESS`：上述字段及screenshot绝对路径、width、height、renderedFrames、screen类名。
- `CLIENT_SMOKE_FAILED`：上述身份字段及phase、error，另有异常堆栈；附带最近主线程快照的screen、overlay、captureRequested、renderedFrames、snapshotSource、snapshotAgeMs。尚未观察屏幕时记录unobserved及年龄-1。
- `CLIENT_SMOKE_FAILURE_STATE`：失败先落盘后，主线程如仍可执行任务，补充当前screen/overlay和计帧状态并正常stop；这不是第二条FAILED。失败回执不会等待主线程诊断，以免加载阻塞或死锁让失败日志丢失。背景线程只读取不可变快照，不直接读取Minecraft的screen/overlay。

通过标准同时要求：进程正常结束、恰有SUCCESS而没有FAILED、新截图存在且可解码、人工打开图片看到正常主菜单。只出现SUCCESS日志或Gradle退出码0不充分；失败也使用原版正常stop，退出码可能仍0。图片解码证明文件完整性，不自动证明画面视觉正确。

watchdog在订阅类初始化后默认120秒仍无已完成截图就记录FAILED并安排主线程stop。游戏JVM系统属性 `gregtech.integration.clientSmokeTimeoutSeconds` 可设30..300整数秒；缺省120，非整数或越界记录configuration FAILED并正常安排退出，不静默放宽上限。关闭clientSmoke时不读取该可选属性。主线程死锁或加载器在订阅类装载前失败时，事件代码无法强制正常退出；Root运行器应对本次游戏子进程另设“所选预算+约20秒”运行期上限（240秒测试建议260秒），保留日志并仅终止自己启动的进程，不能按全局java/javaw名称杀进程。Gradle准备、下载和编译耗时不算该代码窗口。当前不使用System.exit，不代签EULA。调整预算不改变真实TitleScreen、至少5帧、2500ms稳定、全新可解码PNG或人工画面验收要求。

## Root需要接入的开发运行属性

现有两个源集已经配置，成品jar只取main和core输出。Root应把下列属性显式转发给游戏JVM。单独向Gradle传 `-Dgregtech.integration.clientSmoke=true` 不保证子游戏JVM继承。

Forge `minecraft.runs.client` 添加（其他配置保留）：

```groovy
property 'gregtech.integration.clientSmoke', (project.findProperty('clientSmoke') ?: 'false').toString()
property 'gregtech.integration.clientSmokeTimeoutSeconds', (project.findProperty('clientSmokeTimeout') ?: '120').toString()
if (project.findProperty('clientSmoke') == 'true') {
    workingDirectory project.file('build/client-smoke-run')
}
```

已有mods.gregtech必须继续含 `source sourceSets.bootstrapGameTest`。若需要独立clientSmoke run，可基于现有client配置显式加入同一bootstrap源集与属性，不能把测试类复制进main。

Neo `neoForge.runs.client` 添加：

```groovy
systemProperty 'gregtech.integration.clientSmoke', (rootProject.findProperty('clientSmoke') ?: 'false').toString()
systemProperty 'gregtech.integration.clientSmokeTimeoutSeconds', (rootProject.findProperty('clientSmokeTimeout') ?: '120').toString()
if (rootProject.findProperty('clientSmoke') == 'true') {
    sourceSet = sourceSets.bootstrapGameTest
    gameDirectory = project.file('build/client-smoke-run')
}
```

已有mods.gregtech应继续包含main、core、bootstrapGameTest源集。测试代码没有编辑这两段Gradle配置；Root已在串行窗口接入。Root还将两个隔离build/client-smoke-run的options.txt预置onboardAccessibility:false及声音静音，避免开发首次启动进入辅助提示；这是独立测试配置，不改变.minecraft选项。测试类不会自动点击接受提示或把未到TitleScreen改成成功。

编译命令（整合目录，Java17 Gradle launcher，Java17/21工具链仍按根脚本设置）：

```powershell
.\gradlew.bat --no-daemon --console=plain :compileBootstrapGameTestJava :neoforge:compileBootstrapGameTestJava
```

Root接线后分别串行运行并独立保存日志：

```powershell
.\gradlew.bat --no-daemon --console=plain :runClient -PclientSmoke=true -PclientSmokeTimeout=240
.\gradlew.bat --no-daemon --console=plain :neoforge:runClient -PclientSmoke=true
```

不得并发Gradle；第一次assets/native准备需留足时间。运行完成后先解析SUCCESS给出的路径，再实际查看截图。记录两个日志/图片SHA-256与退出状态；继续保持玩法、正式专服、成品jar和存档测试的单独未验证状态。

## 日志与截图机器验收

`tools/integration/verify_client_smoke.py`兼容Python3.10+；对每次单独日志要求恰一STARTED、恰一SUCCESS、无FAILED，身份/version/TitleScreen一致，STARTED elapsedMs允许0，SUCCESS必须为正且不早于STARTED，至少5帧，SUCCESS之后有BUILD SUCCESSFUL且传入真实退出码0。预期截图名包含本回执的平台/版本/规范UUID v4，必须绝对路径、新UUID文件存在非空；检查PNG签名、IHDR、全部chunk CRC及最后IEND，尺寸与回执一致，再用Pillow verify+load实际解码。文件名UUID与同一日志回执关联；日志不是带签名的进程身份凭证，调用者仍应传本次运行完整日志及真实退出码。

Pillow不存在时仅记录结构检查，不声称可解码、不判完整通过，脚本返回1。成功JSON含日志/截图SHA-256、路径、UUID、版本和尺寸；始终保留visual_review_pending=true、gameplay_verified=false，Root随后实际view_image并单独记录画面检验结果。

```powershell
python tools/integration/verify_client_smoke.py --log work/forge-client-smoke.log --platform forge --exit-code 0 --output work/forge-client-smoke-receipt.json
python tools/integration/verify_client_smoke.py --log work/neoforge-client-smoke.log --platform neoforge --exit-code 0 --output work/neoforge-client-smoke-receipt.json
python tools/integration/verify_client_smoke.py --self-test
```

验收失败也写JSON并返回1；不要用固定0代替实际命令退出码。小型自检覆盖旧日志无标记、FAILED、重复标记、非零退出、PNG缺IEND/截断、回执尺寸不符的拒绝，并有1×1合成PNG作为结构/解码夹具；夹具绝不是游戏截图或视觉通过证据。

## 当前证据

- 本机官方解析的两版本源码与关键API：已核对。
- 开发测试实现：已写入两平台bootstrap源集。
- 两平台原探针bootstrap编译：Root实跑通过（38s）；Forge可配置预算/快照诊断已在重试命令自动重编译并运行。Neo新诊断版本尚未重跑，不重复声称该新版本的运行结果。
- Neo原探针真实运行：Root报告38s正常退出，SUCCESS elapsedMs=8034，1280×720主菜单截图人工查看正常；日志、截图SHA及机读回执由Root集中记录。
- Forge首跑：STARTED 21:18:27、atlas 21:19:54、recipes/advancements 21:20:12，21:20:27到120秒预算并FAILED；Gradle退出码0不足以通过。240秒上限重试实际82566ms/152帧成功，1280×720新PNG解码及实际视觉检验通过，正常退出；详见VERIFICATION账本。
- 验收脚本自检：Python3.13运行通过；Python3.10语法检查通过，尚未在3.10解释器运行。
- 成品jar不包含测试类：源集/打包配置符合隔离设计，新增类的最终jar缺席检查仍待Root构建后执行。

## 2026-10-02 完整恢复源码后的运行

当前批使用既有 bootstrap opt-in smoke；Native 增加 `-PclientModelSmoke=true` 可选模型检查与六物品主菜单画廊，`-PclientSmokeHeap=4g` 仅在 clientSmoke=true 时覆盖其原 2g 预算，不改变普通运行。Forge 也可用 directCoreClasspath=true 避免开发启动前重复生成大 core JAR，普通/发布构建默认归档依赖保持。两当前开发客户端主菜单已通过，Native 全材料/流体非 missing 检查和画廊通过；Forge 未包含该画廊。此证据替代早期简化源码主菜单作为当前加载证据，但不证明客户端世界/完整像素/成品。木辞典贴图缺口及 Blue Mahoe 更正的运行范围见 verification/client-model-parity-20261002.md。

## Neo 真实世界 opt-in（2026-10-02）

既有 NeoForgeClientSmoke 增加 clientWorldSmoke=true，必须提供 prepare|verify、client-world-* 隔离副本目录和规范 clientWorldId UUID。仅 build/client-world-smoke-run 生效，两次独立 JVM 使用同一完整存档副本。检查实际右键菜单/shift-click/正常流体包，原版 level.disconnect 再 Minecraft.disconnect 保存。不会直接生成菜单或把 probe 的过早读库存/错误退出当生产 bug。三种截图角色 world、crusher-gui、mixer-gui 与正常服务器关闭回执齐全才成功。启动/保存/退出不是成品安装或完整生存验收。

复用 verify_client_smoke.py 的 `--world-prepare-log ... --world-verify-log ... --world-manifest <复制前文件 SHA 清单> --world-exit-codes <两真实退出码> --output ...`，要求两独立 PID、同一世界/UUID、真实阶段、三截图/PNG 解码、原存档全 SHA 未变。日志/画面不是签名的进程身份凭证，需调用者实际检查画面。最终范围见 verification/client-world-parity-20261002.md。
