# 2026-10-04 / 正式成品启动与完整交付

本机的两个启动失败有不同原因。1.21.1 报 `zip END header not found`，只读扫描定位到实例内的 GT JAR：4,032,573字节、没有可读ZIP目录；333个依赖JAR和其他模组的ZIP目录均可读。当前完整Neo成品为37,400,282字节，全条目CRC通过。安装副本时间14:51:05早于本轮Neo构建结束14:51:45，可能在写包期间复制，但该副本不等于最终文件前缀，不能仅凭时间证明具体损坏过程。

1.20.1 的安装副本与本轮初始Forge成品逐字节相同，ZIP及CRC正常，问题是缺少 `gregtech.refmap.json`。日志明确报 `EntityFallingOreDropMixin` 找不到 named `spawnAtLocation`，没有refmap；生产Minecraft用SRG方法名。Unsafe/JNI/native-access警告不是这两个已定位的致命错误。

## 实际修正

Forge主编译禁止部分增量编译，并声明完整refmap为输出。Mixin注解处理器必须处理全部mixins，不能因本次只改电池类而删除映射或只生成局部映射。一次完整编译19s重新生成11个类的映射，其中落矿目标为 `Entity.m_19998_(ItemLike)`；保留原有 `require=1`，没有关闭注入或吞异常。

成品验包增加所有ZIP条目CRC检查、Forge refmap存在/与当前编译结果逐字节一致、全部需要重映射的配置类、SRG分支、落矿目标和manifest注册检查。用游戏实例里的两个旧包执行负例检查，分别因缺映射/非法ZIP拒绝。

独立交付探针属于bootstrap测试源码，在单独模组内运行，Forge探针自身重混淆；生产GT成品不带探针。特意移除MixinGradle给Forge探针附加的GT refmap及MixinConfigs，启动器也拒绝含这些内容的探针，避免探针掩盖GT正式包缺陷。

新增启动器读取已安装版本JSON、依赖、资源和其他模组，在 `build/production-smoke/<平台-UUID>` 新实例运行真实正式JAR，用测试离线身份，不使用真实凭证或存档。实际标题页至少5帧/稳定3秒、PNG及退出0都齐全才通过。Windows中文路径用Unicode argv；原UTF-8 Java参数文件在本机ANSI启动器中解析错误，已修正。模拟距离改为合法5。

正式双版构建及独立探针4m37s成功；隔离Forge探针补充构建5s/6s，生产GT内容没有再次改动。最后两版探针带pack.mcmeta重建8s。最终655个共享class逐字节一致；Forge11个/Neo8个本批原生class验包，删除的占位电池和资源没有残留。实际运行 [Forge回执](production-forge-startup-20261004.json) / [Neo回执](production-neoforge-startup-20261004.json)：

| 正式包运行环境 | 主菜单稳定帧 | 探针耗时 | 进程退出 | 截图 |
| --- | ---: | ---: | --- | --- |
| Forge47.4.26 / Minecraft1.20.1 / Java25.0.2 / JEI15.62.0.217+MezzConfig0.6.8+Jade11.13.3 | 178 | 78,285ms | 0 | 1280×720，已查看 |
| NeoForge21.1.252 / Minecraft1.21.1 / Java25.0.2 / EMI1.1.24+Jade15.10.6 | 181 | 60,894ms | 0 | 1280×720，已查看 |

Forge最终加载警告页计数0；原测试探针缺pack.mcmeta导致非致命加载警告页，两轮没有成功回执的运行停止并明确不计通过，补元数据后到标题页。夹具仅在反射确认LoadingErrorScreen错误列表为空时才允许原生Continue动作，错误非零会记录FAILED并停止；最终运行没有走此动作。Neo首次独立启动器未去重版本JSON中的Gson库而在加载游戏前失败，去重后仅重跑Neo。原UTF-8参数文件失败也保留为未通过。最后新增源包/复制包哈希一致性条件，对两个已通过回执实际核对；Windows版本规则格式修正不影响此次两个版本JSON（均未声明os.version条件）。[诊断与失败账本](production-delivery-content-20261004.json)保存全部日志哈希，没有把这些失败当GT修复或成功证据。

## 稳定交付入口

`tools/integration/build_verified.ps1` 仍先运行两版普通新世界检查，再打包/验包。当前本机ignored `work/production-smoke.json` 配置了只读Minecraft根目录、Java25及Forge47.4.26/Neo21.1.252版本；有此配置时，还执行正式包的隔离主菜单检查，失败停止发布。其他机器可用 `-ProductionEnvironment` 指定同结构配置；没有配置时只有开发世界运行与成品静态验包，不能声称生产启动验证。

最后由 `publish_verified_artifacts.py` 按验包报告复制到临时目录，再次核对大小/SHA256/全ZIP CRC，两版都完成后一次rename成新的 `build/verified/<时间-哈希>` 目录，输出manifest和SHA256SUMS。之后的构建不覆盖此目录。仅给已完成目录中的文件，不给正在写入的 `build/libs` 路径。

## 边界

开发普通新世界的电池迁移/配方结果见 [本批电池记录](battery-cleanup-20261004.md)。正式包自检只证明隔离实例主菜单启动，不能代替该生产组合的新世界、旧存档迁移或完整机器链路。实际游戏实例的模组、配置、存档均未原地改动；旧非法资源路径、动画帧、tab_icon警告仍待接续。整段更新后构建脚本没有额外重复运行，独立阶段均保留实际日志和回执。

交付文件：`build/verified/20261004-072803Z-3aded12d/`。Forge39,173,074字节，SHA256 `3aded12d431296c91cf79b5c557ed7bcc155f52eda96a17497596799d412a9b1`；Neo37,400,282字节，SHA256 `d6521c5b78b7ce2231981d4cb8c7f396316d9cf0b6e211668ba10ed1d63db44f`。[发布回执](production-delivery-publication-20261004.json)记录两版再次CRC/大小/哈希通过。源提交占位电池 `a6211261`，生产映射与交付 `e70e7f95`；构建时有工作区改动，报告build_commit保持null，仅verified_source_commit关联完整源终态。
