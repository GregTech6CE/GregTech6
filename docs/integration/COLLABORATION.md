# 构建与协作说明

更新：2026-09-30。源码与Git工作区是 `F:\Dev\GregtTech6New\gregtech6`；三个 `Libs` 来源保持只读。当前阶段和验收看 `PLAN.md` / `STATUS.md` / `VERIFICATION.md`，来源与授权看 `SOURCES.md` / `LICENSE_REVIEW.md`。

## 模块与JDK

根项目保留完整Forge 1.20.1基线；`core` 使用Java17承载共享领域规则；`neoforge` 使用Java21适配Minecraft1.21.1。Gradle launcher使用JDK17，NeoForge编译toolchain使用JDK21，两套JDK都需可获得。不能把JRE路径当包含javac的JDK，也不继续依赖原来源的C盘硬编码路径。

本机统一入口是 `scripts/build.ps1`，以脚本当前帮助/参数为准。CI使用以下任务协议（P1模块正在建立，未建立或构建失败即为失败）：

```text
:core:check build
:core:check :neoforge:build
```

`core:check` 必须执行 `coreBehaviorContracts` 的JavaExec行为合约；`core:test` 空通过不能代替这些合约。项目1原有10个main式测试未自动接入普通Gradle test，不能把 `test` 成功写成这10项通过。Forge GameTests与其他source Python/Java检查按实际task/profile分别执行并记录。

`JAVA17_HOME`、`JAVA21_HOME` 指向两个JDK；CI先用setup-java安装并保存JDK21，再安装JDK17做launcher，明确传 `org.gradle.java.installations.fromEnv`，不依赖runner预装17。Gradle支持通过命名环境变量发现toolchain；JDK选择与编译目标是两个边界。[Gradle toolchain文档](https://docs.gradle.org/current/userguide/toolchains.html)

CI `.github/workflows/build.yml` 有固定双平台matrix、core行为合约、各平台build、平台元数据检查、带平台名制品及SHA-256。任一平台失败/缺jar/取消导致总门失败。CI上传是测试构建记录，不是客户端、专服、玩法或存档通过证明；目前不自动发布到Release/Maven。

workflow文件保存在本地；没有配置GitHub remote不代表已经有线上CI运行。远程归属、凭证、公开发布由项目方确定，不借用来源的GitHub owner。

## 开发顺序与隔离

- 2026-10-25 用户明确纠正执行节奏：以完整任务/卡片为单位连续推进，不自行缩成局部成果后停下等待用户反复推动。内部可以划分步骤，但不得因此缩减目标、制造确认节点或留下已经授权且没有真实阻塞的工作。
- 关联修改集中落实，验证按完整改动集中安排；不要逐文件、逐补丁、逐内部批次反复启动编译或 GameTest。昂贵验证前先核对依赖、参数、夹具和平台前提；失败先定位并集中修复，再执行必要验证，复用已通过且未受影响的结果。旧卡片模板的逐批测试条款按此合并执行，保持原验收覆盖。
- 用户要求暂停时停止任务及测试，只处理暂停期间明确指定的事项，用户恢复前不得自行续跑。
- 开始任务先读相关系统记录、未闭合缺口和源审计，再声明文件域、来源证据、预期行为与验收。
- 在整合仓分支/隔离checkout开发。并行任务尽量分开文件域；共享材料身份、RecipeMap、注册、build/settings与NBT/network codec的改动须明确合并顺序。
- 只建立一套材料、配方、能源/物流语义。新增平台消费共享模型，经适配器转换MC类型，不能通过复制领域类恢复编译。
- 保留已有作者头、来源指针和原文许可。没有原始Git历史时登记缺失，不伪造作者、原始commit或submodule ref。采用代码的来源/哈希/取舍进入对应记录。
- 不照搬来源的机器级hooks、旧state数据库路径、WSL清扫或GPG/合并工作流到新仓；只引入本项目有明确需求且已验证的工具。

## 变更门禁

每个系统变更执行受影响平台build和shared-core行为合约；按改动执行有价值的动态/集成测试，不编写仅重复实现的断言。阶段退出还需客户端、专服、核心玩法与真实磁盘存档重启，不能由编译或对象NBT round-trip替代。

必须分别检查两腿注册ID/材料身份和配方清单；允许差异要有记录与到期阶段。保存/组件变化需有schema/mapping策略；未知材料/物品不得静默丢弃。客户端类加载独立，common menu即使在名为client的包也要按真实类型依赖拆分。

可选mod必须覆盖拔除和组合，尤其JEI/EMI不应经Jade helper强制链接Jade类。脚本/datapack修改需验证专服客户端看到同样内容。物流/能量测试需证明simulate不改状态、transfer守恒、满输出暂停、卸载重载无复制/丢失；debug注料/注能量只证明组件行为。

worldgen修改使用新世界/新区块验证负坐标、跨区块/维度、矿物分布和掉落；旧世界兼容另测。每次测试保存命令、JDK/加载器/依赖、整合commit、制品hash、输入fixture、完整结果与未执行项。

## 审查、贡献与发布

审查关注基线功能保留、来源授权、行为一致、平台边界、资源/配方重复、存档风险与实际检查证据。提交说明写具体问题、变更后的具体行为、来源及验证；使用自己的真实提交身份，不能冒充来源作者。合并前保持双方共享文件与运行契约一致。

每次阶段更新 `STATUS.md`、`DECISIONS.md`、`VERIFICATION.md`，将已采用/延后/未知写清。构建jar只有在相应运行/玩法/存档验收达到后才可标记为功能完成。发布前完成 `LICENSE_REVIEW.md` 的分项资料门、双腿制品和校验和、已知问题与开发/发布说明；不未经归属确认创建或发布GitHub仓库。

出现阻塞时写具体错误、最小复现、已尝试方法和所需外部输入。不能把缺fork、空测试发现、占位GameTest或只有启动骨架写成通过，更不能因阶段耗时而关闭未完成项。
