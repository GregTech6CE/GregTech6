# 世界生成

与 GT6 原版相比，本端在世界生成与矿物生成上的处理差异。

| 内容 | 差异 | 原因 |
| --- | --- | --- |
| 下界矿物生成 | 下界金矿、远古残骸独立开关，默认开启，不随禁用普通 MC 矿石一起关闭。 | 保留现代 MC 下界资源。 |
| 下界基岩矿 | 保留 `glowstone`、`netherquartz`、`ancientdebris`、`efrine`、`firestone` 五种；`voidquartz`、`gloomstone` 停注，灵魂沙散矿的 Gloomstone 分支改为空。 | 停注材料后只恢复仍有独立来源的绿晶与火石，其余下界基岩矿不再生成。 |
| DominicanAmber 接触矿 | 删除 `Coal`/`Lignite`/`Oilshale` 与 `Stone` 接触面的 `AmberDominican` 条目及 SHROOM 群系规则；普通 `Amber` 保留。 | 对应材料停注，见[材料移除](material-removal.md)。 |
| 装饰石生成 | 默认禁用 MC 花岗岩、闪长岩、安山岩矿团，可用 `DisableVanillaDecorativeStone` 调整。 | 避免与 GT 地层生成重复。 |
| 地表散落特殊石子 | 移除 GT6 `WorldgenRocks` 中 1/12 概率的 `MeteoricIron` 粗矿/碎石分支，保留 1/2 空与 1/2 燧石。 | `MeteoricIron` 及其陨钢材料链已停注，见[材料移除](material-removal.md)。 |
