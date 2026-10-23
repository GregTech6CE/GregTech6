# 双版本初期对齐：机器盖板、物流面板及过滤器染色

按 D011 参考 P3_NATIVE_USB_RELAYS_AUTO.md 和 Forge GregTechClient 实际注册，补两项现有渲染入口和材料染色缺口。完整 MachineCoverRenderer 保留六面一像素盖板、外侧光照、物品粒子图标和机器盖板的底层/朝外叠加规则，直接复用 PanelCoverRenderer、ArmRenderHelper 及已有 cover 数据，不引入新盖板系统。

BasicMachineClientSetup 为 BasicMachineRegistries 的全部实际 BE 类型注册此 renderer，覆盖普通机器与当前 Native 大型配方控制器；不存在另注册一份聚变类型的必要。LogisticsClientSetup 为实际物流核心和多方块端口补注册已有完整 LogisticsCoverRenderer，原线缆入口保留，四类 CPU 的实际 displayVisual 与原图层复用。

RelayFilterClientSetup 恢复四个过滤器的 SteelGalvanized tint 0 和八种 SourceExtenderBlock 各自 spec.material() 的方块/物品染色，其他层白色；沿用现有身份和 Steel/StainlessSteel 材料。四种 legacy extender 不冒充 Source 实现，也不擅自指定材质。

仅修改四个 Native Java 文件，无资源、Core、共同存储和 Forge 修改。必要集中编译及 Native jar 构建通过；本批不重复客户端/专服启动，不新增专项夹具。当前编译类与实际最终 jar 字节一致只是包装证据。

待验：盖板实际附着、拆卸和选择显示、六面光照与朝向、物流核心/端口 CPU 动态状态、过滤器/扩展器材质画面、客户端注册运行、成品 jar、联机与完整玩法、独立保存重载和旧档。整体移植仍未完成。
