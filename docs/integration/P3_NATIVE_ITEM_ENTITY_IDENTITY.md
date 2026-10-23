# 双版本初期对齐：拆机和中键物品实体身份

1.20.1 旧 BlockEntityTag 迁为 1.21.1 BLOCK_ENTITY_DATA 组件时，需要保留真实实体类型 id。固定 Native API 源码显示该组件持久化使用 CustomData.CODEC_WITH_ID，BlockEntity.saveWithId 在 saveWithoutMetadata 原字段上只追加注册类型 id；并不附加世界坐标。

统一补 12 类、16 条物品保存路径：两个反应堆壳、轴向发电机、普通/原始基岩钻、大型燃气轮机/热交换器、原大型锅炉/避雷针、多方块储罐，及流体泉和浆果灌木。前十类改用 saveWithId；后两类保持 Source 精简 spring/amount 或 berry 字段并写真实注册 id。既有移除工作库存/反应棒/溢出中子、停止标志、方向组件和放置读取语义保留；不改注册名称、单位、配方或来源目录。

既有基础机器、仓储、齿轮箱、激光转换器、过滤器、处理工具和砧等已带 id 的路径不重复改写。USB 盖板绘制已在 StorageContainerClientSetup 内联注册，没有因 Source 类名缺失而重复导入或注册。

一次集中 Neo/Forge 编译 24s 通过，无新夹具、游戏启动或全资源打包。本批源码及记录进入 Git 上传源码包；此前 d86cdfbcad 的模组 JAR 尚不含这批改动，需按 BUILDING.md 构建当前源码。真实物品编码/放回/内容守恒、客户端/完整玩法、独立世界重载与旧档仍未验证，完整仍未完成。
