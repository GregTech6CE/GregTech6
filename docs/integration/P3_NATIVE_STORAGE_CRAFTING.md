# 双版本初期对齐：存储手工制作与合并启动

完整接入现有 Forge Loader_HandToolCraftingRecipes.storage：瓶箱、储物柜和60材质的普通金属箱、强化木箱、四格抽屉、两种保险箱、大容量仓储、物流仓储、高级/充电工作台。保留原材料形态、图样/数量、条件门槛、金电缆、四级以上电路、胶水与木质螺栓，以及现有 ToolShapedRecipe 的工具磨损、非镜像和余物路径。

实际复用 GTMetalChests、GTSafes、GTStorageContainers、GTCraftingTables 与 StorageRegistries 的注册身份；查询真实材质或既有稳定 id，避免将 legacy mass_storage 误作材质成品，钢 safe/工作台和不锈钢 drawer 特殊名字由既有实体注册处理。没有复制存储底层、重新注册方块或更换存档格式。

原 forge:glue 标签内容逐字节保留，仅资源目录从1.20的tags/items变为1.21的tags/item；五种瓶胶水与可选焦油沥青标签不变。Neo已有 circuits_tier_4_plus 内容与当前 Forge 相同，直接复用。Core与Forge无改动，三个来源保留。

集中 Native compile+jar/Forge compile 50秒通过。一次既有 Neo专服短启动2分1秒通过，200 tick后正常保存停止。实际日志生成：齿轮箱13、变压器13、旋转发动机13、泵4、线30、管334、砧34、栏杆7、处理器1、电池壳5、存储542，加前批轴52，手工总计1048行；原生RecipeManager全体7515条且未记录配方解析错误。这是生成/加载证据，未逐条枚举manager身份或执行制作。

原 Forge 手工 loader 的12个入口族至此已接入同一 Native ManualToolRecipePack；之前 P3_NATIVE_HAND_CRAFTING.md 的 storage代码缺口由本批覆盖，原报告检查范围保留。实际匹配、工具磨损/容器余物、客户端配方显示、数据包重载、成品jar、完整生存链、独立进程重载及旧档仍待验；其他系统的双版本对齐与完整三来源整体核对继续。
