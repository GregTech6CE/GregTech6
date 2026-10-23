# 第四十九批：脚手架原版材质与硬币生成回退修复

## 修改与依据

GT6 `MultiTileEntityScaffold.java:117-128` 按渲染通道区分 PLATE、材料 blockSolid 和 HATCH。此前脚手架全部面统一用了 PLATE。本批修改 `tools/generate_scaffold_models.py`：踏板/横档使用 PLATE，支柱和悬臂支撑杆使用金属 blockSolid；design=1 的盖板和横档上下表面叠加 HATCH，保留材料染色及透明裁切。四种状态几何与上一批一致。

原版 `iconsets/PLATE.png`、`iconsets/HATCH.png`、`materialicons/METALLIC/blockSolid.png` 与当前使用的三份图片逐字节相同，无需制作替代图片。这里只补已注册钢脚手架的材质，不能据此宣称原版所有材料脚手架都已注册。

检查硬币的生成链发现：`add_coin_assets.py` 已保留浮雕模型，但 `generate_prefix_assets.py` 的通用路径仍写 `minecraft:item/generated`，重新生成会将硬币变回平面。现改为引用共享 `gregtech:item/coin_minted`。同一脚本还会批量删除既有物品/分类翻译，现改为仅补缺失默认条目，保留已核定的 GregTech.lang 名称及其他注册物品名称。

## 验证

- 新增材质分层检查，旧脚手架在杆件使用 `#plate` 处失败，修复后通过。
- 新增临时目录真实执行通用材料生成器的测试，修复前分别复现硬币变平面、已有译名被删除，修复后通过。连续生成两次验证不会回退。
- `build/assets_batch49.log`：**18 项 Python 资源测试全部通过**。覆盖脚手架、按钮、板锭、书架、硬币原版图案/内部面消隐、生成器重复执行与译名保留。
- 两个修改的生成脚本通过 Python 编译检查。
- 本批没有修改 Java，没有重跑 Java 门禁；最新代码门禁仍是第四十八批 `build/gametest_batch48_button.log` 的 **969 项通过**。新资源由专项测试验证，未进行游戏内像素验收。
- 没有在实际项目运行通用材料生成器，只在临时目录测试；实际语言文件保持不变。

## 实机测试与剩余事项

在 runClient 查看脚手架顶部的活板盖图案、侧面金属杆与钢色；竖直叠放时应保留各结构状态外观。硬币现有立体外观不应变化，本批防止的是未来重新生成造成回退。

完整目标仍未完成：脚手架其他原版材料变体、其他占位家族逐项功能对照、地牢原版内容对照以及客户端视觉验收仍需继续。原版地牢本来使用的 Minecraft 方块按确认保留。

没有构建 jar，没有执行 git commit。
