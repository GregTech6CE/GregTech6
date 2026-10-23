# 2026-10-04 / 原版箭与装药弹壳

整体移植仍未完成。按要求减少测试，本批使用集中编译、共享固定样例和双平台验包，没有启动客户端或 GameTest。

## 来源与实现

原版来源、散列及上游材质许可见 [来源回执](ammunition-source-20261004.json)。以 Gregorius Techneticies 的执行代码为准：OP 将木/塑料箭的主材料设为 U9，尽管同一行旧注释写着四分之一；本批保留代码中的 U9。

- 原来的子弹压制直接使用可重复压模，跳过黄铜和火药。本批恢复五种 EMPTY 实体物品：两种无箭头箭杆及三种装药弹壳。只开放这五种部件，其他隐藏/无效材料仍不生成物品。
- 原版十二条装药弹壳配方已在共享 GTOtherGen 中，但此前 EMPTY 被加载器当成无效材料拒绝。两平台仅对这五种 item-prefix 形式接受 EMPTY，流体和其他无效形式没有放宽。
- Press 恢复两条箭和六条子弹组装规则，实际消耗箭杆/装药弹壳；只有真正的压模仍是催化剂。Unboxinator 返回箭头或弹头材料与对应箭杆/装药弹壳，取消返还压模和丢弃第二输出的降级行为。
- 补九条原版工作台规则：木/塑料棒加羽毛；棒、两片微型塑料板及锯制作箭杆；一块燧石加一至四根木箭杆生成原生箭；原生箭回收一根木箭杆。材质组遵循 ANY.Wood/ANY.Plastic 图，羽毛接受可选 forge/c 通用标签。
- 原版 Loader_Recipes_Vanilla:469 会移除原生四箭配方。两平台既有 VanillaCraftingRecipePack 现共用同一新增替换行：一块燧石和四根木箭杆制作四支箭，覆盖 minecraft:arrow；另保留共享的四种产量路线。
- 木/塑料材料箭使用真正的 ArrowItem 子类及注册实体，弓、弩、发射器使用它们；无箭头箭杆和子弹不加入 minecraft:arrows。塑料箭速度乘数 1.5，木箭 1.0；伤害基础加 max(材料等级−1,0)，保留现代弓的附魔处理。拾取保留原物品及其数据；落地不再按原生 1200 tick 删除，而按原版 3000 tick 总寿命处理，松脱时重置年龄。近战攻击生物消耗一支弹药，创造模式不消耗。
- 材料身份改用共享 MaterialFormItem 接口，因此新 ArrowItem 子类继续参与材料合成匹配、物流过滤、回收组成、库存双层图标和颜色。自己的 tooltip 仍只追加一次。EMPTY 部件使用原版 NONE 图标和原版名称，不显示虚构的 EMPTY 材料组成。
- 箭、弹壳和子弹的回收组成包含实际箭杆、黄铜、火药；EMPTY 不作为物质加入组成。箭实体材质保留自 wolfram0108 的 GregTech 资源目录，默认 CC0-1.0 资产许可；NOTICE 更新来源说明。

## 验证与边界

快速双平台 compileJava 在 20 秒内通过，日志为 work/ammunition-compile2-20261004.log。最初编译发现材料接口不能直接作为 ItemLike 传入颜色注册，已修正。

全量材料图的定义及 postInit 快照仅各改变五个 EMPTY 形式布尔值；还原这五行即精确复现旧快照散列。全部其他材料、别名和前缀数据不变，证明见 [隔离差异](ammunition-differential-20261004.json)。旧快照和新增样例的旧版单位数字分别拦住了前两次共享检查，失败没有计作成功；已对照原版 CS.U=648648000 修正。

共享检查通过 9,725 条断言：行为 1,993、材料 7,645、机器 34、温度 53。补默认箭配方后仅重跑受影响材料检查，材料为 7,646 条，合计当前证据为 9,726 条；新增共 23 条固定源数据样例检查五个 EMPTY 身份、隐藏过滤、NONE 图标、组成、关键配方参数和箭的速度/寿命。既有 GameTest 夹具按消耗/回收装药弹壳修正，只参加编译，未运行。

正式 :core:check :assemble :neoforge:assemble 在 9m32s 内通过，日志为 work/ammunition-build3-20261004.log。补最后发现的原生四箭配方替换后，只执行受影响的 :core:materialBehaviorContracts 和双版增量打包；该增量构建在 4m21s 内通过，日志为 work/ammunition-final-20261004.log。

仍需后续实现或实测：原版材料弹药附魔的自动附加、塑料箭抢夺倍率、原版特殊魔法/爆炸附魔伤害与第三方武器兼容；成品射击/拾取/重载、实际客户端渲染和生存配方链尚未游戏验证。不能将基本原生弹射物接入描述为全部原版特殊箭行为已经对齐。

包内署名第一次验收未通过：root NOTICE 已更新，但 core 源资源仍保留旧副本。同步 core 源资源与已构建资源中的同一 NOTICE 字节后，以 :assemble :neoforge:assemble -x :core:processResources 重新打包 1m11s 通过，日志为 work/ammunition-notice-final-20261004.log；没有复跑测试或复制全部未变资源。

最终 [成品回执](ammunition-artifacts-20261004.json) 验证 614 个当前共享 class、两版加载器元数据、重复条目及测试夹具排除；两版各 29 个当前新增/修改原生 class 存在，NeoForge 对这些 class 逐字节比对当前编译输出，Forge 重混淆 class 验证存在及继承/接口关系。13 个本批共享资源均与源文件一致，含箭 PNG、九条配方、两种语言及最新 NOTICE。class 静态检查确认两版 MaterialArrowItem 均继承原生 ArrowItem 且实现材料身份接口，MaterialArrowEntity 均继承 AbstractArrow；这不构成射击或渲染实测。

成品路径：C:\Dev\GregTech6\build\libs\gregtech6-1.20.1-forge-0.0.0.jar；C:\Dev\GregTech6\neoforge\build\libs\gregtech6-neoforge-1.21.1-0.0.0.jar。

最新询问的基岩矿上层已再对照 WorldgenOresBedrock:195-207 与 WD.removeBedrock:782-798：矿团上方六层在其 muffin 范围内替换母岩，主世界为深板岩、下界为下界岩，不挖成空气，保留底层与范围外基岩。既有修正 7bff1d9f 已在当前两平台代码；不自动改写旧区块。

生产代码提交：`2ea1121d03e358e3493be57b377ebb3e17dda2a4`；本批验证记录作为单独文档提交。
