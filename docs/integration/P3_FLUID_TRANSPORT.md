# 罐与流体管道源码迁移

本批来自 saltnya 现有 Forge 实现，保留 31 个普通罐、40 材料 × 7 尺寸的 280 个管道 IDs、容量、耐性和注册次序。规格表只保留在 core `FluidTransportDefinitions`；两端 `FluidTankGT` 共用 `LongFluidStorage`，保留 long 数量、过滤身份、可变容量、精确抽取和 int 能力视图。`BarrelFermentationRules` 共享原发酵时间/产量公式。没有引入第二套材料、流体或 CC 容量单位。

Neo 移植原 Tank/FluidPipe block、BE、自动输出、均分/回流标记、多槽管、危险介质泄漏、拆除转移、覆盖物过滤/阀门/排水/通风/面板、工具点击、动态管道模型和覆盖物渲染。实际注册 Neo `Capabilities.FluidHandler.BLOCK`，邻居查询走 Neo capability API；玩家容器采用官方事务 API 返回替换容器，保留原先有液体容器优先排入罐的交互顺序。普通罐 item 原堆叠 64，管道为 64/32/16。罐保留供物流罐继承的 protected `(type,pos,state)` 和 `getFluidTank()`。

根节点接线：构造阶段调用 `content.transport.fluid.FluidTransportRegistries.register(modEventBus)`；真实 `MachineRecipeMaps`、`GTTechnological` 和 `FermenterFoodRecipes` 准备后调用 `FermentationAccess.bindOriginalRecipeMaps()`。绑定前 `isBound()` 明确返回 false，封桶不凭空产生配方。能力和客户端订阅由独立 MOD subscribers 注册。此模块注册 `gregtech:wrench` 声音；后续完整声音模块应复用其 holder。入口、GTBlocks、GTBlockEntities、物流罐注册及 smeltery BEs 未在本批修改。

原资源依赖闭包 1510 个引用，其中 1365 个文件逐字节移到 core，49 个由主线先前提供；另将原完整 en_us/zh_cn 语言文件移至 core。来源 blob、SHA-256、逐资源记录见 `core/provenance/fluid-transport-extraction.json`。Neo API 参照本机 21.1.243/1.21.1 官方已解析源码，以及 masson `FluidBarrelTank`/普通 TankBlockEntity 的 Neo 能力和长量边界；没有复制其 barrel catalog/144 单位底层。原作者、许可证和发布授权未决状态不变。

存档保留 `gt.temperature`、罐模式/封桶时间、`gt.tank`/`gt.tank.N`、`Amount`/`Capacity` long、回流字节和 `gt_cover_N`。Neo 用 provider-aware codec 写流体/物品组件；读取原 Forge `FluidName` 封套与已知 cover/filter item IDs，原 Tag 保留为不透明 custom data。没有承诺 1.20.1 世界整体转换或第三方旧 Tag 的功能语义；不存在的注册 ID、旧 item damage/组件转换、其它机器与全世界版本升级仍有边界。

继承原缺口：管道安全检查未完成流体温度驱动及超温熔毁；magicProof/simpleOnly 在原普通罐中没有完整判定；负数精确抽取和超大容量乘法沿用原调用前提。玩家容器路径仍未加入按点击面的 cover 过滤（原代码也是如此）。完整电动工具、虚拟蒸汽机连接和未迁机器控制能力继续等待对应模块；原工具/cover ID 不等于全部相关内容已可获取。

按最新要求，本批没有构建、测试、游戏启动或新增夹具。数量/路径记录属于源码与资源搬迁记录，不代表运行、视觉、玩法或跨版本旧存档验收。
