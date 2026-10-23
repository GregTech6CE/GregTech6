# 双版本初期对齐：采掘工具与等级

Native 早期 BlockHarvestPolicy 缺少当前 Forge 的多类判定，导致既有动态标签/采掘事件使用普通 sound fallback 或默认等级。恢复完整 Source 策略：打包作物 BaleBlock 归剑；木书架归斧、金属书架归扳手；保险柜、USB/HDD 开关、四格抽屉、高级工作台、物品管和多方块端口归扳手。恢复金属书架材料质量、栏杆原 harvestLevel，以及普通/强化混凝土 1/3 等级。保留木壳先于机器判定及已有矿石、石材、材料、仓储规则。

只在 Native 边界替换注册查询为 BuiltInRegistries，并继续用 WireMaterialLike 包含原电线与现有 Native 线实现；信号线 cutter 规则保留。标签名字来自已共用 BlockHarvestNames，现有 BlockLootPack 生成 tool/tier tags，现有 BlockHarvestEvents 处理高于原版级别的采掘判定，不新建另一套采掘规则。

检查中确认硬币的 custom renderer 已经由 CoinClientSetup 注册，Native 充能工作台/粉末漏斗的较短类是能力 API 改写，坩埚已调用同一共享加工引擎；未因 Source 类名或行数差异重复导入。

1 Native Java，Core/Forge/资源/三来源未改。集中 Neo/Forge compile 55s 通过；最近三批之后一次已有 Neo 短专服 3m 37s 通过，真实200ticks/正常保存停止。它只证明开发模式加载和短运行，不证明逐个 tool/tier tag、真正采掘/掉落磨损、上批有限液体制作或物品组件编码成功。无新夹具、重复客户端或全资源 JAR 打包；成品 JAR、完整生存链、独立世界重载与旧档仍待验。

此前交付的 Git 上传源码包固定于 8b47b7dc90，不被本批重写。本批是该提交之后的新修改；之前 d86cdfbcad 模组包也不含本批及上一批实体 id 修正。构建最新源码请按 BUILDING.md，完整三源仍未完成。
