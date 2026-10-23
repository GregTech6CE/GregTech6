# 分子数据浏览交付 / 2026-10-06–07

源码 `f81c74b5`：JEI/EMI 区分四种已有USB stick的文件内容和等级，改名保持身份，复合键顺序不影响身份；数据边界使用原键，真实机器匹配及存储格式未改。

Forge JEI/EMI、Neo JEI/EMI 四个开发客户端组合各打开扫描、打印、复制三页，每组303可扫描管道和103排除项索引通过。实际槽及流体图标被读取并检查，12张页面截图已查看。USB3为这些展示页的标准介质；四种物理级别的文件身份不等于所有级别/端口玩法已完整展示。官方API打开页面/查询输入，没有模拟所有R/U按键；tooltip是实际物品及槽调用，截图未显示悬停面板。

首轮Neo EMI页面功能通过但截图仍显示Baking recipes，后续索引没完成；自检现在同时等待加载与后续索引线程。最终Forge后续索引4396ms、Neo39622ms，页面截图中Baking recipes消失。没有修改或宣称改善EMI整体加载性能；开发模式既有的JEI桥合成ID警告仍可见，单列后续缺口。三次夹具编译/可选依赖/JEI19访问器失败与一次提前结束的Neo EMI记录均留在[JSON回执](data-browser-20261006.json)，不以Gradle退出0算失败场景通过。

普通构建4m18s，四核心入口2472/34/7947/53断言通过；720共享、Forge1758/Neo1565当前平台class及CRC/元数据/Mixin/夹具排除验包通过。固定成对目录：

`C:\Dev\GregTech6\build\verified\20261006-162533Z-377d2c03`

| 成品 | SHA-256 |
|---|---|
| Forge1.20.1 | 377d2c0371f7bbad07e47b40fec6125e1caeecd34667ecce9c9254713647b80c |
| NeoForge1.21.1 | 13b8ff2c6573ab0f36f549c45cef84ec42491755b6c79bc3cba27eed61e4692b |

Java25正式Forge47.4.26/Neo21.1.252隔离主菜单自启动57.52s/65.98s、182/181帧、退出0。两包四USB文件身份运行断言通过；截图仍是原建材/木板主菜单预览，不当作成品世界配方浏览证据。安装文件未改，启动后SHA/CRC相同。5原版来源、2比较参考及2语言文件未改。

复现页面检查：JDK21、offline Gradle8.8，`-I work/emi-local.init.gradle -PdirectCoreClasspath=true -PdirectCoreResources=true -PclientCreateWorldSmoke=true -PclientSmoke=false -PclientSmokeTimeout=600 -PclientSmokeHeap=4g -PrecipePresentationRuntimeOnly=true -PmaterialDataBrowserRuntimeOnly=true :runClient`；Neo换成`:neoforge:runClient`；两版EMI组合加`-PemiSmoke=true`。普通构建不带directCore参数。每个场景等待终态，禁止重叠Gradle。

玩家页面开发环境、成品主菜单、真实机器/生存、独立存档重启分别记证。后两项、旧档、HDD/线缆/端口和非标准介质的全部显示、EMI独立运行及真实外部模组仍未验收。整体移植仍未完成；本批本地提交，未推送，PR暂不处理。
