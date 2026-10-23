# 当前任务与继续执行入口

更新：2026-09-30。整体移植仍未完成，不代表任何未验证阶段完成。

- 主工作区：`F:/Dev/GregtTech6New/gregtech6`，main。
- 原始项目1导入：41561782 / import/saltnya-snapshot。
- P1脚手架快照：31b827601e / integration/p1-scaffold，提交说明明确runtime pending。
- 原始历史准确标签：source/brokestar233-snapshot=3118acbf83a84d05fa37d57af1705cf00e423ca9；source/masson-snapshot=2ba4e4b60f7a770360b2ec7ca2adad3507ec4a28；source/modularui-brokestar-snapshot=ec524db7a5724f8cf3c837436b1ae742c778c62f。

## P1主线门禁

串行执行该工作区Gradle。原始Forge构建、core64断言、移动后Forge编译、Neo jar、双jar及bootstrap源码编译均通过。Forge/Neo实际开发服务器各3项bootstrap测试已通过并正常退出。初次Neo资源重复及Forge资源下载超时均有失败日志和修复记录。

下一步：Forge distributionJar复验两jar相同且与当前core一致 → 游戏客户端真实启动/呈现 → 正常专服与真实玩法另验。验包/CI门禁已强化；Forge重混淆改写core字节由两阶段打包解决中，严格SHA门不放宽。不能把游戏开发server的自动EULA测试放行当正常专服同意。

主日志：work/baseline-forge-build.log、p1-core-forge-compile-neoforge-build.log、p1-dual-build-bootstrap-compile.log、p1-neoforge-bootstrap-runtime.log、p1-neoforge-bootstrap-runtime-retry1.log。完整验收摘要写VERIFICATION.md，不将未执行检查合并成一个“通过”。

## P2共享材料实现

负责人：audit_saltnya。

隔离Git工作区：`F:/Dev/GregtTech6New/gregtech6/work/p2-shared-materials`，分支 `work/p2-shared-materials`，基于31b827601e；所有修改仍在指定整合目录内部。主工作区测试期间禁止把P2代码合入主线。

范围：依P2_MATERIAL_DESIGN.md迁移同一GTMaterial/Registry/完整目录/组成/前缀/CrucibleMaterialStack/Reactions；Forge loader/Component/日志/原版Item绑定/NBT移到平台边界，改必要调用点；不会创建第二MaterialCatalog。允许在隔离工作区跑限定2 workers的corecheck/编译，不启动游戏或全量jar。

门禁：迁移前完整目录金样 → Java17/java.base闭包 → 全目录ID/alias/组成/铜锡合金/反应产率守恒 → 原Forge调用编译 → 来源/单位/K整数/NBT键边界记录。完成分支提交交回主，由独立审查和统一runtime回归后决定合并。

## 其它并行域

- audit_masson：热量比较已完成；接着只编辑两平台独立bootstrap源码中的integration/client目录及CLIENT_SMOKE.md，实现客户端主菜单截图/正常退出探针，不修改Gradle/main/core。
- audit_brokestar：验包/CI修复及reobf classfile调查已完成；接着只读审核P2隔离分支的材料身份、单位、平台seam和存档键边界，不运行Gradle。

## 环境与缺失信息

Java17=C:/Program Files/Java/jdk-17.0.4；Java21=C:/Users/Asus/AppData/Roaming/.minecraft/runtime/java-runtime-delta。通过scripts/build.ps1检测，路径不写死到Gradle配置。

常规来源核对脚本支持Python3.10；TOML验包用Python3.11+内置tomllib。本机已验证的Python3.12为 `C:/Users/Asus/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe`。

待提供：项目1可获得的原仓/历史与明确授权范围。新origin归属尚未需要，未猜测或设置。缺此发布信息不妨碍本地功能整合，但发布门不得标绿。
