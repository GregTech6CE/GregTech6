# Issue 分类与标签

Issue 通过类型、性质、领域、分流状态、优先级、游戏版本和关闭原因七类标签完成分类。同一类标签使用同一色系，`scope:` 前缀的领域标签共用一种颜色。

## 类型

每条 issue 必须且只能有一个类型标签。

| 标签 | 颜色 | 中文意义 | 英文意义 |
| --- | --- | --- | --- |
| `bug` | `#d73a4a` | 缺陷总类 | Bug umbrella |
| `enhancement` | `#a2eeef` | 建议总类 | Suggestion umbrella |
| `rfc` | `#6f42c1` | 提案游戏系统、玩法、架构、规范或路线图 | Proposal for game systems, gameplay, architecture, specs or roadmap |

`rfc` 用于需要先讨论、后决定的提案；小型且可直接执行的功能请求使用 `enhancement`。提案形成长期决策后，按 [`docs/decisions/`](../decisions/README.md) 的现有约定记录。

## 性质

性质描述问题形态，不描述领域。建议每条 issue 使用 0–2 个。

| 标签 | 颜色 | 适用类型 | 中文意义 | 英文意义 |
| --- | --- | --- | --- | --- |
| `crash` | `#8e1623` | bug | 崩溃、无法启动或卡死 | Crash, startup failure or freeze |
| `regression` | `#b62324` | bug | 上个版本正常、当前版本失效 | Worked in a previous version, broken now |
| `perf` | `#bc4c00` | bug | 卡顿、内存、启动或注册耗时 | Lag, memory, startup or registration cost |
| `parity` | `#c33c4d` | bug / enhancement / rfc | 与 1.7.10 原版 GT6 行为不一致 | Differs from original GT6 on 1.7.10 |
| `translation` | `#e99695` | bug / enhancement | 翻译缺失、错误或硬编码 | Missing, wrong or hardcoded translation |
| `qol` | `#76d7d7` | enhancement | 信息展示、操作便利、减少重复劳动 | Information display, convenience, less repetitive work |
| `balance` | `#4db6ac` | enhancement | 数值、难度、成本或产出平衡 | Numbers, difficulty, cost or output balance |
| `refactor` | `#1b7c83` | enhancement | 重构、清理、内部实现调整，不改变对外行为 | Refactor, cleanup or internal change without external behavior change |

`parity` 只描述“与原版 GT6 行为不一致”，不决定类型：

- 当前行为与原版不一致，希望保持一致：`bug` + `parity`
- 当前行为与原版不一致，希望改变行为：`enhancement` / `rfc` + `parity`（取决于改变的范围大小）
- 原版本来就没有的系统、物品、实体等内容：不使用 `parity`，即使存在同类型的原版物品（例如新的覆盖板）

## 领域

领域是正交轴，用来筛选受影响的子系统。建议每条 issue 使用 1–3 个，信息不足时可以留空。所有领域标签共用 `#d4c5f9`。

| 标签 | 中文意义 | 英文意义 |
| --- | --- | --- |
| `scope: recipe` | 配方、配方表、砧与模具配方 | Recipes, recipe maps, anvil and mold recipes |
| `scope: machine` | 机器、多方块、坩埚、核与传感器 | Machines, multiblocks, crucible, nuclear and sensors |
| `scope: interaction` | 通用交互层：右键、放置、破坏、物品使用 | General interaction layer: right-click, placement, breaking and item use |
| `scope: ui` | GUI、tooltip、Jade/EMI/JEI 展示、模型、贴图与渲染 | GUI, tooltips, Jade/EMI/JEI display, models, textures and rendering |
| `scope: logistics` | 管道、漏斗、轨道、储物、物流核心与覆盖板 | Pipes, hoppers, tracks, storage, logistics core and covers |
| `scope: equipment` | 装备与手持物品：工具、武器、护甲、防护装备、便携容器及其行为 | Equipment and handheld items: tools, weapons, armor, protective gear, portable containers and their behavior |
| `scope: worldgen` | 矿脉、矿层、地表沉积、结构、群系与维度生成 | Ore veins, stone layers, surface deposits, structures, biomes and dimension generation |
| `scope: material` | 材料注册、ID、形态、成分、统一化与流体属性 | Material registration, IDs, forms, composition, unification and fluid properties |
| `scope: energy` | 电网、电力线缆、锅炉、涡轮、引擎、轴与电池 | Electric network, power cables, boilers, turbines, engines, axles and batteries |
| `scope: redstone` | 红石行为 | Redstone behavior |
| `scope: hazard` | 危害与伤害机制、伤害类型、伤害免疫与防护 | Hazards, damage mechanics, damage types, damage immunity and protection |
| `scope: compat` | 其他模组、加载器、附属与可选集成 | Other mods, loaders, addons and optional integration |
| `scope: api` | 附属 API、公开契约、SPI 与 API 版本 | Addon API, public contracts, SPI and API version |
| `scope: build` | Gradle、CI、工具脚本、文档与仓库流程 | Gradle, CI, tool scripts, docs and repository process |

领域判定边界（只列容易混淆的几组）：

- `scope: recipe` 管配方是否存在、是否匹配、成本多少；配方执行时的机器 tick 归 `scope: machine`。
- `scope: equipment` 管装备物品自身的行为与属性；通用交互分发归 `scope: interaction`。
- `scope: hazard` 管伤害、免疫与防护机制；产生危害的机器归 `scope: machine`，材料的危害属性归 `scope: material`。
- `scope: redstone` 管红石行为；覆盖板、机器输入输出等非红石行为归各自 scope，电力线缆归 `scope: energy`。
- `scope: ui` 管 GUI、tooltip、模型与渲染；世界内的右键、放置、破坏归 `scope: interaction`。

## 分流状态

| 标签 | 颜色 | 中文意义 | 英文意义 |
| --- | --- | --- | --- |
| `needs triage` | `#9a6700` | 等待维护者分类 | Awaiting maintainer triage |
| `needs info` | `#bf8700` | 需要提案者的更多信息 | More information needed from the proposer |
| `needs help` | `#d4a72c` | 需要社区协助、讨论、决定 | Community help, discussion or decision needed |

## 优先级

| 标签 | 颜色 | 中文意义 | 英文意义 |
| --- | --- | --- | --- |
| `priority: high` | `#c93a0a` | 崩溃、数据丢失或主要功能不可用 | Crash, data loss or major feature unusable |
| `priority: medium` | `#fbca04` | 一般的功能性错误 | General functional error |
| `priority: low` | `#c2e0c6` | 有临时方案、外观或文本等次要问题 | Has a workaround, cosmetic, text or other minor issues |

## 游戏版本

| 标签 | 颜色 | 中文意义 | 英文意义 |
| --- | --- | --- | --- |
| `1.21.1` | `#033d8b` | Minecraft 1.21.1 的 NeoForge 21.1.x 平台 | Minecraft 1.21.1 + NeoForge 21.1.x |
| `1.20.1` | `#0969da` | Minecraft 1.20.1 的 Forge 47.x 平台 | Minecraft 1.20.1 + Forge 47.x |

## 关闭原因（resolution）

| 标签 | 颜色 | 中文意义 | 英文意义 |
| --- | --- | --- | --- |
| `duplicate` | `#cfd3d7` | 重复的 issue | Duplicate issue |
| `invalid` | `#afb8c1` | 无效或无法复现 | Invalid or not reproducible |
| `out of scope` | `#8c959f` | 超出本项目范围 | Outside the scope of this project |
| `wontfix` | `#57606a` | 不予修复 | Will not be fixed |

## 组合规则

- 类型和优先级是底线；性质和领域按实际信息量添加。
- 一条 issue 最多使用 3 个领域标签。超过 3 个时，在正文说明主领域。
- 不要用领域标签代替类型标签：`scope: recipe` 不等于 `bug`，配方建议也可以是 `enhancement`。
- 关闭 issue 时使用关闭原因标签，不再保留 `needs triage`。
