# GregTech 6 Community Edition — Credits & Third-Party Attribution

## 项目

- **作者**：lombinaxmasson
- **许可证**：LGPL-3.0-or-later（见 `LICENSE`）
- **Issue Tracker**：https://github.com/GregTech6CE/GregTech6/issues

## 固定来源（GT6 / GTM）

本项目的数值、配方与结构数据来源于以下固定 revision 的源代码，代码中的
上游行号引用均相对 GregTech 6 固定提交 `3703e403`。

### GregTech 6

- **仓库**：https://github.com/GregTech6/gregtech6
- **固定 revision**：`3703e40308c8c030763fd6297dea8b210d2a77b1`
- **许可证**：LGPL-3.0-or-later
- **使用方式**：世界生成（GT6WorldGenerator / WorldgenOresLarge /
  WorldgenOresSmall / Loader_Worldgen / MT）、材料与配方分母、机器与
  前缀身份。派生数据按 SOURCE_BACKED / SOURCE_DERIVED 分类；平衡性
  调整均为显式 `DESIGN_POLICY`，不冒充来源事实。
- **资产说明**：GT6 默认资产按上游 `LICENSE.assets` 为 CC0 1.0；GregTech
  logo 与其衍生资产按上游 `LICENSE.logos` 为 CC-BY-NC-4.0。本项目不使用
  GregTech logo。
- **砧几何**：`assets/gt6ce/models/block/tool/anvil.json` 来自 GT6
  MultiTileEntityAnvil 空态 ISBRH（ID 32025），不是 TerraFirmaCraft。

### GregTech Modern

- **仓库**：https://github.com/GregTechCEu/GregTech-Modern
- **固定 revision**：`de5d2c4a4c863b94a10bfb5d0839df2de8246628`
- **许可证**：LGPL-3.0
- **使用方式**：**仅命名与几何参考**。T8 材质资格与管道统计以 GT6
  直接注册为准。

## 模板与第三方依赖

- **NeoForged MDK 模板**：MIT（NeoForged 项目，仅适用于模板文件；全文见
  `NOTICE`）
- **Jade**（可选，方块信息）：https://modrinth.com/mod/jade
- **EMI**（可选，配方查看）：https://modrinth.com/mod/emi
- **KubeJS**（可选，启动期材质注册）：https://modrinth.com/mod/kubejs

以上可选兼容模组均未捆绑；运行时按能力门控启用集成。

## 其它对照源

贴图树、GT6U、kTFRUAddon、GT6-TFRU 和配方 dump 不随本仓库发布。上游与固定
revision 见上文。GT6U 为 LGPL-3.0-or-later，默认资产 CC0 1.0，logo 例外为
CC-BY-NC-4.0。v1 不包含 GT6U 内容。

## 免责声明

- 本模组不声称是 GT6 或 GT6U 的完整移植；v1 范围为"工业主链"。
- 核裂变/聚变/等离子控制器、G10 及 GT6U 内容不在 v1 范围，亦不在
  v1 阻断清单。
