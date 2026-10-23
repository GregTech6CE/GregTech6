# P2：原始机器参数 record 进入共享 core

2026-09-30。此提交从 `de18243bb9a81a7752d778ec8a5d1d392ba6294d` 在隔离 `work/p2-machine-core` 分支实施。范围只有两个 record 的原字节迁移、固定规格合约和来源/复现记录；没有机器BE、Neo注册、热接收、存储、渲染或主工作区修改。

`com.gregtech.gregtech.api.machine.MachineSpec` 和 `CrucibleSpec` 从 Forge `src/main/java` 移到 `core/src/main/java`，包名、record字段、构造器、方法和原文件字节不变。原位置不保留第二份类。主构建既有 `implementation project(':core')` 提供同一 API。

原始来源是提供的saltnya快照标签 `import/saltnya-snapshot`，本地导入提交 `415617825945a9f54b11fdc359ff9b3cf694c8cd`。该提交不代表恢复了原作者历史。作者归属与适用许可仍未解决；原 All Rights Reserved 元数据与Forge MDK许可证文本没有更改，没有自行授予新许可。完整参数来源、blob与SHA见 `core/provenance/machine-spec-extractions.json`。

- MachineSpec：原blob `05bfe51c9275e4d69f4c3a3bfe5c74fc112b0b30`，SHA256 `872fb50a6656f86c201d98d094a253244e00a525c94151098b47ffa8184c86e5`。
- CrucibleSpec：原blob `8bf97aef1854170ca90f0966892201a05a5a8b3e`，SHA256 `521ed7556904427c6d1b3653fe930f9b697affd15895cb9ed528a8e80ab5fd9e`。

固定探针取原 `GTMachines.java:79–80/:334–335`、`MachineRegistry.java:84–88/:149–168` 的真实参数：砖燃烧箱25%效率/16HU/t，陶瓷显式hull density0.8181818181818182、7U壳、2500K热限，模具同一共享材质与密度、meta1055和5U壳。不能把陶瓷材料默认密度替代机壳密度，不能把GT6 U=648648000换成144。另检验原六参MachineSpec构造器、非法效率/输出拒绝、其他companion壳重量和stone63U谓词；没有凭“9x容量”注释推断机器容量。

独立质量金样采用源Java float32密度和111.111111D质量比例：陶瓷7U壳636.3636357272728kg，3U铜2986.6666763956573kg，1U锡809.6666857781968kg，合计4432.696997901126kg；已共享ThermalStep所需45HU/K。模具5U壳454.5454540909092kg。数值合约不意味着BE已接入ThermalStep，也不代表真实供热或青铜流程完成。

复现命令（无需Gradle，不启动游戏；输出在独立GUID的core/build目录）：

```powershell
python core/verify-machine-specs.py --jdk-home 'C:\Program Files\Java\jdk-17.0.4'
```

可改用实际JDK17路径或JAVA17_HOME；脚本核对javac主版本17，读取原Git blobs、不写来源项目，确认旧源位置没有重复record，独立编译全部生产core和合约。它将原record独立编译并置于classpath首位，分别运行原始/迁移版本固定探针，再比较两者公开API的javap描述符，最后运行现有domain合约。新增 `machineSpecBehaviorContracts` 已接到 `core:check`，不能用空的Gradle test报告代替这些行为证据。

实际验证：Oracle17.0.4、`--release17 --limit-modules java.base` 通过；jdeps生产core仅java.base；原始/迁移record各34断言通过、公开API描述符一致；现有基础64、材料6154、热规则53断言通过。日志 `core/build/machine-spec-verification.log`、GUID和SHA保存在provenance。38个引用这两个record的Forge生产Java文件与基准HEAD逐字节一致，其路径/聚合SHA也已记录。

本次没有调用Gradle、完整Forge平台编译、成品jar打包或游戏。源码/公开API引用保持与纯域测试通过是当前证据；合入后主线还需统一平台编译、逐字节验包及现有实际加工链回归。普通箱子重启或这些参数断言均不证明GT机器保存兼容。
