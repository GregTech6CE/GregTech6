# L5：查询与诊断

本页用于读取宿主就绪后的材料、物品、流体与配方视图，并通过显式结构诊断排查失败。

返回[附属编写指南](README.md)。

查询接口在宿主就绪后读取当前会话的快照。旧宿主不支持的新服务会抛出 `UnsupportedOperationException`，未知 map/selector 会抛出 `IllegalArgumentException`；不要用空集合或默认值掩盖这些失败。

| 查询 | 用途 |
| --- | --- |
| `GregTechMaterials.catalog()` | 读取不可变材料目录；`resolve` 解析名称和别名，`find` 只返回真实材料，`forms` / `formBindings` / `processes` / `specialProcesses` 提供分域视图 |
| `AddonItems.stack(prefix, material, count)` | 取得材料形态物品；未知前缀或材料返回 `ItemStack.EMPTY`，不是 null |
| `AddonItems.composition(stack)` | 读取宿主已保存的前缀和材料单元；未知物品返回空组合 |
| `AddonItems.selectorTag(n)` | 取得程序化电路；范围 0..24 之外或未注册时抛出 |
| `AddonFluids.still/fluid/gas/acid/plasma/magic` | 取得 GT 流体和属性；未知字段返回 `FluidStack.EMPTY`，属性查询对空值返回 false |
| `AddonTools.matches/isMagnifyingGlass` | 查询宿主已有工具身份；不是新工具注册入口 |
| `AddonRecipes.map/find/collision` | 查询已有机器配方表和静态/动态匹配结果 |
| `StructureAccess` | 在成型行为回调中读取能源部件、功能部件、完整单元快照、重复份数和条件状态 |
| `AddonStructures.diagnose/describe/notifyChanged` | 显式结构诊断、问题描述和匹配数据变化通知 |
| `GregTechAddons.apiVersion/phase/recipesReady/registeredIds` | 判断 API 特性和附属生命周期；不能用来检查 Minecraft/加载器版本 |

## 材料目录

材料和 metadata 仍通过 `AddonMaterialContributor` 提交。材料初始化 READY 后，`GregTechMaterials.catalog()` 返回当前会话的不可变 `MaterialCatalogView`：`resolve(name)` 解析规范名和别名，`find(MaterialKey)` 只返回真实材料，材料组不会伪装成可加工材料。旧宿主没有该服务时明确抛出 `UnsupportedOperationException`，未就绪时明确报告阶段，不以空集合掩盖失败。

`catalog().forms()` 返回冻结的 item／block／fluid 定义期规则，`MaterialFormRule` 区分种类、默认材料量和显式附属请求；平台完成注册后，`catalog().formBindings()` 返回实际 item／block／fluid 绑定，并包含 `ItemMaterialRegistry` 的显式统一物品。规则允许不等于实际注册，实际绑定也不推导配方或获取来源。

材料所有权、metadata、tag 与替代组声明见[材料与加工规则](materials.md)。

## 结构诊断

附属不需要引用内部追踪器。行为回调中按需调用 `context.structure().diagnose()`；独立工具可使用公共入口：

```java
List<StructureProblem> problems = AddonStructures.diagnose(serverLevel, controllerOrPartPos);
for (StructureProblem problem : problems) {
    player.sendSystemMessage(AddonStructures.describe(problem));
    if (problem instanceof StructureProblem.Count count) {
        int missing = Math.max(0, count.minimum() - count.actual());
        // 自定义 UI 可直接使用实际数量和上下限，不解析提示字符串。
    }
}
```

结果为不可变数据，区分错误方块（含全部可选方块或标签）、未加载区块、重复层不足、数量不足/超限、同组方块不一致、被其他主控占用、附加条件拒绝及无法诊断。包含世界坐标；不一致/占用还包含参照方块或占用主控坐标。`describe` 复用放大镜的本地化文本。

必须在逻辑服务端线程调用，适合玩家请求或工具操作，不应每 tick 扫描。查询不强制加载区块、不抢占部件、不触发成型回调；空结果只代表当前扫描未发现问题，不承诺之后的世界状态。普通方块返回 `Unavailable`，已关联部件转查所属主控；旧宿主不支持时明确抛出异常。数量是已成功匹配的单元数，未知区块不会被误报为数量不足。条件部件的工作许可仍由 `conditionsMet()` 提供，不当作结构搭建错误。

## 通知结构数据变化

`AddonStructures.notifyChanged(level, pos)` 只在逻辑服务端调用，用于匹配相关数据改变但方块状态没有改变的情况。它使缓存失效并安排下一 tick 重新评估，不立即扫描、不强制加载区块；不能用于客户端预测。
