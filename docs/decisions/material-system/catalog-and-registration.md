# 目录声明与生命周期安装

本文说明内置材料与附属材料如何从静态目录声明进入运行时只读目录，以及安装过程中的阶段状态机、原子校验、附属所有权与冻结边界。整个注册管道确保惰性声明不触发隐式初始化，唯一性检查先于索引写入，任一阶段出错即持续拒绝后续访问而不会留下半完成状态。

## 规范目录与惰性收集

- **七类规范目录**：全部内置材料按职责组织在 `core` 的 `gregtech.content.material.catalog` 下：`ElementMaterials`、`CompoundMaterials`、`OreMaterials`、`StoneMaterials`、`WoodMaterials`、`SpecialMaterials` 与 `GroupMaterials`；离心与电解分解声明统一由 `DecompositionMaterials` 持有。
- **主次信息拆分**：`MaterialDefinition` 仅承载身份（数值 ID、规范名、别名）、展示（本地化键、色号、纹理集）、热学与原子物性、基础属性及来源；工具参数、化学组成与 `divider`、关联组及机器处理规则归入 `MaterialInfoDefinition`，通过 `MaterialInfoRegistry` 按键关联。
- **稳定排序与兼容符号**：`MaterialDefinitions` 在装配期收集目录、附加分解规则并按规范名稳定排序建立键索引，避免依赖 `HashMap`/`HashSet` 遍历顺序影响下游分配。读取 `Materials`、`GT6Materials`、`ImportedMaterialData` 等兼容门面不会隐式触发材料注册，这些符号仅解析指向已安装的同一规范对象。

## 五阶段安装状态机

`GTMaterialRegistry.init()` 按严格单向顺序推进材料安装，重复初始化保持幂等：

| 阶段 | 允许操作 | 阶段约束 |
| --- | --- | --- |
| `DEFINITIONS` | 收集规范声明与附属声明；通过 `MaterialRegistryState` 校验并安装材料与别名 | 读取 `Materials.*` 等符号不得隐式推进阶段 |
| `LINKS` | `MaterialLinker` 校验所有跨材料引用与替代组无环路后写入成员关系与附属联系 | 禁止普通调用方在链接窗口外创建新真实材料 |
| `FINALIZING` | 收敛布尔属性闭包、写入世界生成与石材角色 flag、单向投影分解兼容属性并冻结 `MaterialInfoRegistry` | 完成后主信息与辅助状态一并不可变 |
| `READY` | 发布不可变 `MaterialCatalogView` 与定义期元数据；触发 `MaterialPublication.materialDefinitionsPublished()` | `postInit()` 仅校验处于此状态，不再补写静态事实 |
| `FAILED` | 记录启动阶段故障并保持公开访问入口拒绝请求 | 不允许通过重置标记位掩盖半完成状态 |

## 原子安装内核与模式隔离

`MaterialRegistryState` 统一管理名称表、数值 ID 表与规范集合，在修改任何索引前完成前置合法性校验：

- **四种安装模式**：明确区分 `SENTINEL`（仅允许 `INVALID` 与 `EMPTY` 哨兵）、`BUILTIN`（正整数 ID 严格位于 `1..ADDON_ID_START-1`）、`GROUP`（仅限替代组，统一使用 `GTMaterial.INVALID_ID`）与 `ADDON`（ID 在 `ADDON_ID_START` 及以上）。
- **先校验后写入**：规范名必须非空且与 `GTMaterial.sanitize` 结果一致；规范名冲突、正整数 ID 冲突或别名规范化冲突时直接抛出异常，名称表与 ID 索引保持完全未改变状态。
- **显式别名收敛**：别名写入前验证目标材料已安装且不可跨材料重绑定；相同别名对同一目标重复注册保持幂等。

## 附属材料所有权与合并规则

附属通过 `AddonMaterialContributor` 向 `AddonMaterials` 提交材料声明及后续 `materialInfo` 注册：

- **ID 区间与确定性分配**：附属材料的数值 ID 由宿主在 `ADDON_ID_START` 之上按稳定键顺序分配，不侵入内置保留区间。
- **同名一致合并与冲突定位**：多个附属声明同一非 core 材料且关键定义一致时合并身份并记录多所有者；core 内置材料不可被附属改写；出现不兼容字段冲突时直接报错指明材料、冲突字段与双方来源附属，不误报为普通重复内置材料。
- **所有权隔离**：在元数据、独立标签或过程规则注册阶段，非材料所有者不得篡改内置材料或其他附属材料的内在属性与专有 `metadata` 属性。

## 引用链接、冻结与兼容边界

所有声明安装完成后，系统统一校验跨材料依赖图并在冻结后封存兼容入口：

- **全量引用检查**：`MaterialLinker.validate()` 核查组成组分（禁止引用无效材料或自循环）、替代组成员、燃料残留目标、粉碎/熔炼/压粉目标及矿石副产物是否均指向有效材料。
- **组环路检查**：对全部 `Group` 声明构造有向图执行环路遍历，拒绝任何嵌套组环；通过后再关联组成员。
- **冻结与透传边界**：`FINALIZING` 结尾调用 `MaterialRegistryState.freeze()` 与辅助信息冻结锁死所有写入。旧 `GTMaterial` 上的工具、燃料、加工目标与倍率 getter/setter 仅作为兼容透传，背后读写同一个已收敛的辅助条目，并在冻结后拒绝任何修改。
