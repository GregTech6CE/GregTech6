# Ender IO

没有 `Compat_Recipes_EnderIO`。原版引用是合金归属、规范形态、扳手，以及储罐和暗铁栏杆的材料数据。这批不改规范形态，也不把 Ender IO 的物品当成 GT 的规范形态。

扳手是 `itemYetaWrench`，登记成特殊工具并删掉它自己的合成。这不是机器行，这批不恢复。

原文件里没有能用现有机器行表达的配方。因此不加开发期运行依赖，也不跑 GameTest。

`MD.EIO` / `IL.EIO` 共 56 处，全部推后。明细在 [enderio.json](enderio.json)。

旧存档没有用这份联动测过。
