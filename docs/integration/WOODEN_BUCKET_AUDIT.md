# 木桶交互核对与注册代码整理

2026-10-23，第一百二十九批，开始于10月22日，基线 `c430c3f47`。上一轮 #78 原版生成链复核和已提交的128批均为进展。本批沿便携木桶实际注册／回调核对，5原版／6当前重点Java、18段引用、13个实际原生条目及12段原生引用见[回执](verification/wooden-bucket-audit-20261023.json)。原树只读，保留原GregTech-6 Team／Gregorius Techneticies LGPL来源；未导入或执行原游戏方法。

## 已有范围与边界

原MultiItemRandomTools注册9种金属箍、空桶加18种内容物。当前WoodenBucketCatalog有相应9×19＝171种目录形态，共享GTMultiItems遍历它们，两端实际生成源码均有这一循环。不能因Stonecutter非活动分支的排版，误报仅Forge有木桶。

WoodenBucketItem已有1000L容器、空满变体／金属箍保留、合成剩余桶、世界流体交互及成年牛挤奶。Forge物品能力和Neo FluidCapabilities实际注册均存在。以上只确认入口存在，不代表全部流体别名、每条配方、材质、创造交互或存档都已验收；原实体精确类型与现代Cow继承范围等仍待续核。

## 430：海水／脏水桶的倒空入口和创造策略不同

原偏移5、9分别挂载Behavior_Switch_Metadata，目标是同箍空桶，两个标志均false。实际实现是服务器onItemUseFirst：玩家潜行点击方块时直接切换元数据，没有创造模式保留满桶的例外；它没有在该类实现空气右键倒空。

当前discardOnly对应两种内容物，但分支位于use，潜行右键即可返回空桶，创造玩家则继续返回原满桶。onItemUseFirst先走炼药锅／通用流体接口，没有原专用倒空分支。登记430，限回调与创造策略，不误报成所有木桶均不能倒空，也不把原行为写成无条件右键清空。本批保持现玩法。

## 431：发射器未接木桶装倒液行为

原ItemBase构造器注册GT_Item_Dispense；MultiItem按已挂载行为的canDispense分派。Behavior_Bucket_Simple有实际吸取水源、GT河／海／沼泽水及放置流体逻辑。海水／脏水使用另一个行为，不能把这些分支概括成所有形态都能放置。

当前木桶工厂及TechItem没有对应发射器注册；维护原生代码中的DispenserBlock.registerBehavior实际用于MaterialArrowItem。两端实际游戏字节码按物品身份查询行为表，默认DefaultDispenseItemBehavior分出一个物品并生成掉落实体；不会因物品有流体能力便自动调用桶的use。登记431为木桶专用发射器行为缺口，未运行发射器世界。

## 432：补满未满炼药锅及创造消耗策略不同

原Behavior_Bucket_Simple.onItemUseFirst对BlockCauldron的水位小于3均设置为3，并把手中水桶切换为空容器；这里也没有创造模式免消耗分支。当前直接分支只匹配空Blocks.CAULDRON，遗漏已有1／2格水的WATER_CAULDRON；创造模式向空锅倒水时则保留满桶。

已继续追通用后备，不能只看直接if：Forge FluidUtil只查对应方块实体能力，原生炼药锅交互按物品身份查表，没有木桶映射。Neo另有CauldronFluidContent／CauldronWrapper能力，原生水容量1000、最高水位3。fill以gcd(1000,3)划分整步，单位1000、一步3水位；已有水位1或2时，剩余水位不足一个整步，返回0。因而这条后备同样不能完成原来的补满操作。具体公式及四水位的有限算术记录在回执；不是执行原生方法或客户端实测。

登记432，限水桶补满／创造消耗合同。现代空桶从满水锅取水的新增路径、世界流体放置、乳液和可选联动没有据此宣布全部不兼容。

## 代码整理与验证

- WoodenBucketCatalog展开构造器、记录方法和查询方法，保留全部数据值／顺序及许可注释。
- WoodenBucketItem展开能力包装与容器方法，把填充时的两次状态赋值分行，修正续行和嵌套循环缩进；ID、模拟／执行、1000L门槛和交互策略均保持。
- GTMultiItems把木桶和喷漆两族提为registerWoodenBuckets／registerPaintSprays，在原位置按原顺序调用。没有挪动注册事件或建立第二份目录。

三源486→568行。五个完整core／平台代码视图和注释受控保持；两族循环的完整正文分别在两平台投影保持，四个实际Stonecutter编译输入对应当前源码。一次core／双平台Java编译138秒通过，使用已有同版本缓存API适配，未改生产构建配置。此处不是默认离线／CI、游戏或存档验收，也未打包。

432是累计差异编号，包含此前已修复／保留策略的账本不能当成待修数量或完成率。按用户随后要求，下一步先合并并上传代码、生成双版构建；全面核对与源码标准化目标仍继续。
