# Equipment crafting parity / 2026-10-02

以 cf10b65 为基础，将现有 Forge 七组 234 条制作配方原字节迁入 core 资源。来源项目只读，逐文件哈希与可获得 donor 哈希见 core/provenance/equipment-crafting-parity-20261002.json。未导入新的来源差异系统或更改授权。

## 实现与取舍

- smeltery_survival 138、energy_nodes 23、axial 21、magnets 10、chemical_batteries 25、optical 15、zpm 2。
- Forge 继续读取原 recipes 路径；NeoForge 复用既有 portable_crafting_recipes 原生数据包入口，适配 recipe 单数路径和 result.item/string 到 result.id。原图案、材料、数量、工具损耗、allow_mirror 均保留。
- 两版同一份配方数据；不存在第二套设备注册或能源底层。重复资源路径显式报错。原 38 条便携容器配方仍加载，共 272 条。

## 集中检查

双版生产代码与现有 bootstrap 检查编译通过：37 秒。一次 NeoForge 普通 DedicatedServer 启动、运行 200 tick、保存并正常退出通过：2 分钟；PID 76464，session bb1d6a0f-3bc6-43b0-ba6f-a237a304cd74。隔离世界 build/equipment-crafting-20261002/world，未触碰游戏世界。

全部 234 条在实际 RecipeManager 查到，产物非空，非空原料均有实际 item/tag 候选。选定真实 CraftingInput 检查强化青铜锅炉、致密青铜燃烧箱、ULV/LV 变压器和生粘土坩埚；锤损耗 400、扳手损耗 800，剩余物查询不修改输入，变压器不允许镜像。锅炉实际序列化器网络往返保持匹配。实际 SmeltingRecipe 查询产出陶瓷坩埚；粘土坩埚回收配方产出 7 粘土球。

上述证明实际配方查询和工具语义，不代表玩家点击工作台、炉子供燃料并自然烧制、全部 234 条逐一制作或完整生存取得。当前批次没有新增客户端/世界重启检查，前几批证据单独保留。普通活塞引擎制作定义仍未找到，不能由锅炉配方接通推断整个生存链已完成。Forge 运行、当前客户端、成品安装、旧存档与完整三源范围待后续推进。整体移植仍未完成。

## 最终产物

最终双 JAR 构建通过：3m 45s。573 个当前编译共享 class 在两版一致，平台关键类/品牌/ID/版本正确，无重复或 bootstrap 污染；全部 234 条共享资源 SHA 在两 JAR 中与迁入原件一致。

- `gregtech6-1.20.1-forge-0.0.0.jar`：38,532,988 字节；SHA-256 `527c607eab40371bed33a5e722d767ee5c5317790ee67e102e5fda3a8b8e583e`。
- `gregtech6-neoforge-1.21.1-0.0.0.jar`：36,002,977 字节；SHA-256 `d4208cd6943cc256e2579290abd3a4af342a4105dc19f8e1b613be458f78ca08`。
