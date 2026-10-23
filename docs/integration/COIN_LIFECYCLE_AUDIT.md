# 硬币保存、客户端分派与原生选取核查

2026-10-22，第一百一十五批，基线 `042ff2957`。上一轮已提交铸币核查与源码整理，属于实际进展。本批新增 ALIGN-387／388，配置消费者扩展 ALIGN-004。9份原版、23份当前生产Java、4份资源、26段源码引用及两端12个原生条目／14段原生引用见[回执](verification/coin-lifecycle-audit-20261022.json)。文件摘要用于追溯；不代表这些大文件的所有功能、完整材料目录或全部生命周期均已验收。原树只读，GregTech-6 Team／Gregorius Techneticies与LGPL来源声明保留。

## 确认差异

| 编号 | 原版实际消费者 | 当前消费者与限制 |
| --- | --- | --- |
| ALIGN-387 | 硬币实现IMTE_GetLightOpacity，返回LIGHT_OPACITY_WATER=3；实际世界方块MultiTileEntityBlock将光照查询转给实体。 | 当前CoinPileBlock没有相应覆盖；dynamicShape、noOcclusion、固定16×2×16选框和空流体经过两端原生规则得到0。限源码／实际缓存工件推导，没有调用运行中的光照引擎或比较世界亮度。 |
| ALIGN-388 | 世界方块将中键选取交给IMTE_GetPickBlock；Base04用writeItemNBT构造硬币，保留材料、币模、unique及回收信息。 | 当前直接继承Block，没有专用clone覆盖；两端带玩家参数的原生接口继续委托Block默认方法，构造已注册的裸coin_pile BlockItem，未读取堆内真实硬币。限普通选取的后备链，不包含Ctrl选取、外部模组拦截或实际按键测试。 |

原版注册表的mBlock是内部物品块，不能据此猜世界中的继承关系。已继续核对：铸币登记使用aMetalChips，后者来自MultiTileEntityBlock；内部placeBlock实际放置容器的mBlock。上述选取／光照结论依据这条真实世界方块路径。

ALIGN-004补充：原版客户端分别读取use3DCoins与use3DCoinsInv，当前配置及两个渲染消费者没有对应开关。当前默认3D路径已存在；配置仍按用户安排留到收尾，本批不改变默认效果。全零世界图案缺平面分支与Unique提示仍归既有385／386，不重复新增。

## 已有功能与边界

当前实体写出全部16格数量，并保存真实硬币ItemStack；对于物品已携带的币模／unique数据，保存整个物品会保留相应负载。两端读取后均进入storeCoin，重新解析图案缓存。这里只确认读写代码和调用关系，不宣称旧版本存档、异常手写数据或跨版本迁移均已验证。

进入区块用getUpdateTag返回完整实体数据，单次更新也提供ClientboundBlockEntityDataPacket。实际Forge接口的onDataPacket／handleUpdateTag调用实体load；Neo对应接口调用loadWithComponents，进而进入实体loadAdditional。不能因为本类没有重新覆写两个接收接口，就报告“缺同步”。但真实服务器发包时机、客户端收包、区块卸载再进及存档重启尚未实测。

Forge客户端已有硬币堆渲染器及物品自定义渲染扩展；Neo的CoinClientSetup同样注册两者。MaterialClientModels给两端硬币安装CoinItemBakedModel，后者根据实际币模数据选择堆栈渲染器，未标记的物品继续使用默认烘焙模型。原回收数量U9在当前coin前缀也已有对应，不能把单独注册材质物品当作数量信息缺失。这里没有验收全部材料成员、实际画面或第三方查看器生命周期。

原shuffle完整函数主体与当前实际core CoinPileRules.repack做了约0.48秒的隔离比对：0–256总数、16组等价随机序列，共4,112例、558,109次随机抽取，格数结果、抽取次数、总量和每格16上限对应。原函数通过记录器代替随机来源和客户端更新；这不是原生NBT、玩家取放、过期落地或网络世界测试，也不是统计所有随机序列。

## 源码整理

整理CoinGeometry、CoinPileBlockEntity、CoinItemBakedModel、CoinItemRenderer、CoinPileRenderer，以及Neo的CoinClientSetup。六源855→936行，展开压行方法和事件注册链；两处b改为bounds，事件参数e改为event，流参数v改为item。共22个明确的标识符出现位置，均为局部名称／参数名，类型和公共方法签名保持。

纠正取币说明中“移植版总是交给玩家”的旧注释：实际方块回调先检查库存准入，创造玩家只移除而不接收。11份完整平台代码视图在记录的标识符归一后，有序代码、字面量位置、其余注释与Stonecutter指令保持；不借整理改变玩法、注册、资源或API类型。用户WoodMaterials.java、CompoundMaterials.java字节摘要保持。

准备阶段的标识符匹配和注解出现次数守卫已纠正，引用选择也改用完整的pick签名。无Gradle、整模组编译、GameTest、Minecraft、渲染、存档重启、打包、推送或GitHub写操作。累计388条含已修与保留策略，不是未修数或完成率；原生交互验收、完整目录／配方查看、其余独立工具和全系统源码标准化继续，goal保持active。
