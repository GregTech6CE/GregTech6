# 世界生成

与 GT6 原版相比，本端在世界生成与矿物生成上的处理差异。

| 内容 | 差异 | 原因 |
| --- | --- | --- |
| 下界矿物生成 | 下界金矿、远古残骸独立开关，默认开启，不随禁用普通 MC 矿石一起关闭。 | 保留现代 MC 下界资源。 |
| 下界基岩矿 | 保留 `glowstone`、`netherquartz`、`ancientdebris`、`efrine`、`firestone` 五种；`voidquartz`、`gloomstone` 停注，灵魂沙散矿的 Gloomstone 分支改为空。 | 停注材料后只恢复仍有独立来源的绿晶与火石，其余下界基岩矿不再生成。 |
| 装饰石生成 | 默认禁用 MC 花岗岩、闪长岩、安山岩矿团，可用 `DisableVanillaDecorativeStone` 调整。 | 避免与 GT 地层生成重复。 |
| 深层页岩层矿石 | GT6 `StoneLayer.DEEPSLATE` 的 Amber 有两条完全相同的条目，分别限制森林与海滩；本端合并为一条同时匹配两个群系。`Mithril` 条目随材料停注省略。 | 本端 `OreDef` 以完整条目为键，无法表达同一条目对应两组群系；`Mithril` 见[材料移除](material-removal.md)。 |
