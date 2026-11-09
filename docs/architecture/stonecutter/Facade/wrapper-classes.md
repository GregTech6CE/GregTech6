# 包装类

## 子原因

节点侧的 `NeoToolBindings`、`FluidPipeToolInteractions`、`WireToolInteractions`、`AxleToolInteractions` 只转发共享代码，属于陈旧副本。

## 例子

### 修复前

```java
// 节点侧包装类
public final class NeoToolBindings {
    public static boolean isTool(ItemStack stack, GTToolType type) {
        return GTToolHelper.isTool(stack, type);
    }
}
```

### 修复后

删除包装类，调用点改用 `GTToolHelper` 等共享门面。

### 为什么这么修复

包装类会制造“两套入口”。删除前先确认调用点已全部迁移，否则节点编译会断。

### 其他可能的修复方式

- 保留包装类，只改方法体。入口仍然有两套。
- 把包装类改成接口默认方法。仍然需要调用点迁移。

相关链接：[issue #358](https://github.com/GregTech6CE/GregTech6/issues/358)
