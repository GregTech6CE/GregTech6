# P3 电池物品接口与共享包计量批次

更新：2026-10-01。本批仅源码整合，尚未编译/运行；上一批历史启动成功不覆盖本次新增代码。累计工业配方和电池两批，继续完成相关依赖后再集中进行 Neo 优先的小检查。

来源 saltnya BatteryItem、ChemicalBatteryItem、GTElectricItems 与 IItemEnergy。LegacyBatteryDefinitions 保留 battery_lv/mv/hv/ev/iv 五个原 ID、名称、100000/400000/1600000/6400000/25600000 EU 容量与1..5等级，Forge 注册通过同一表构造，Neo 原生 DeferredRegister 接入这五个实际 BatteryItem。

ItemBatteryRules 共享注入/提取包数量上限，保留原半额最小/双额最大电压包、单物品要求和末包部分接收/容量截断行为；Forge 简易和化学电池调用共享规则，Neo简易电池亦调用。原始电池的最后不足一包容量时接受整包但丢弃超额电量，是保留的 GT6 行为，不能宣称该边界无损；没有未经核对引入 FE 任意能量流替代包规则。

Neo IItemEnergy 保留完整原平台接口，BatteryItem 的 gt.charge 键写入已有 StackCustomData 的 CUSTOM_DATA 边界，保留其他自定义字段及 simulate 不写入语义；电量条和 tooltip 薄适配 1.21。五个原物品模型及十五张纹理引用闭包保留 SHA、纹理搬至共享 core。不复制其他电动工具作为空行为物品。source SHA 和改动清单见 core/provenance/item-battery-source-integration.json。

仅原 IV rechargeable_batteries tag 的全部必需成员在此子集具备，因此原字节迁入单数 item 目录。ULV..EV 标签仍需要真实化学电池，并未改为 optional 或删掉原化学种类。被动化学电池方块/物品/BE、电量面板 renderer、通用掉落数据包、充电器/电池盒和完整电动工具继续整合。当前不据简易电池注册宣称充电链、生存取得或机器能源网络完成。

本批未运行编译、物品充放电行为、simulate、客户端显示、真实存档重载及旧存档转换测试。完成下一批工厂/掉落依赖后集中检查，仍未完成。


后续验证（2026-10-01）：本批已包含在三批集中 core/Neo/Forge 编译成功范围中，27 秒；运行行为仍未验证。化学电池和掉落批次接线及缺口详见 P3_CHEMICAL_BATTERIES.md。


后续集中短启动（2026-10-01）：Neo 普通 DedicatedServer 配方/标签/掉落数据包加载、200 tick、正常保存退出通过，耗时4分37秒。工业源入口加载含5661弹药/2709焊接/444高压釜/1796破碎/8激光/5电池填充行；新线材和石墨烯入口未抛缺失或冲突异常。回执 verification/p3-neoforge-wires-startup.json。Yggdrasil公钥获取超时为隔离offline启动的非阻塞网络异常，未使游戏失败。没有客户端、真实线路/充放电/机器加工、独立进程世界重载或旧存档通过结论，也没有新 jar 验包。
