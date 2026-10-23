# 双版本初期对齐：坩埚回收与有限流体合成

按既有坩埚、手工制作、流体 MD 检查当前 Forge/Native 边界，补三处实际行为遗漏，没有重新搬运已接线世界生成，也没有引入新材料或注册底层。

CrucibleItemInput 恢复 Source SmeltingCrucibleBlock 分支：从真实 CrucibleSpec 取得壳材和 hullMaterialUnits，只有有效材料才生成输入；恢复 isValid。现有 39 坩埚已使用该 spec。Native 原有含库存物品拒绝、材料/矿石/石材/原版原矿与组成解析保持。实际回炉与物质量守恒尚未运行验证。

ToolShapedRecipe 恢复 Source drainFiniteIngredients：在允许的偏移和镜像中查找完整匹配图样，只对匹配格子的 FiniteBottleFillingRecipe.ContainerIngredient 调既有 drainOne，扣除 1000 mB 并返还真实容器。适配 CraftingInput 的 width/height/size 和 Neo ICustomIngredient 包装；原工具损耗与镜像政策保留。已有杂酚油轴使用不同 ContainerIngredient，仍由自己的 recipe 扣量，不被本助手重复处理。解决原手工齿轮箱/变压器/旋转发动机/泵中有限润滑容器余物缺失，实际制作、偏移/镜像与守恒待验。

FiniteBottleFillingRecipe 的 Lubricant 条件同时接受 Lubricant 和 LubRoCant，与 Forge 一致；原 Native 只有前者不存在时才接受后者，现修正。含 gt.display_fluid 展示载荷的物品拒绝进入有限液体配方，沿用已有 FluidDisplayBinding。保留实际容器能力、前后液量差 1000 mB、Native 组件一致性检查与延迟生成示例物品；没有使用显示物品伪造耗液。

3 Native Java；Core、Forge、资源及三来源不改。集中 Native jar/Forge compile 5m 27s 通过，相关 6 个当前 class 与实际 jar 匹配。按 D010 不新增夹具或重复客户端/专服启动。上批启动不能证明本批加工合成；成品 jar 运行、完整玩法、独立进程世界重载和旧档仍未验证，双版本剩余对齐与完整三源整合仍未完成。
