# BlockCodec

本页守卫方块类型 codec（`Block.codec()`、`MapCodec`、`BLOCK_TYPE`）相关的跨版本分支，说明其来源与当前作用，并规定继承原版基类被迫实现 codec 时的处理方式。

## 来源

Mojang 在 1.21 将方块类型定义纳入数据驱动序列化：

- `BlockBehaviour.codec()` 为抽象方法；`Block` 提供具体默认 `Block.CODEC = simpleCodec(Block::new)`。
- 七个原版中间基类把 `codec()` 重声明为抽象，强制具体子类覆写：`DirectionalBlock`、`HorizontalDirectionalBlock`、`FallingBlock`、`BushBlock`、`BaseRailBlock`、`BaseEntityBlock`、`AbstractFurnaceBlock`。
- `BLOCK_TYPE` 注册表（`Registry<MapCodec<? extends Block>>`）由 `BlockTypes.bootstrap` 填入原版方块 codec；`BlockTypes.CODEC = BLOCK_TYPE.byNameCodec().dispatchMap(Block::codec, ...)`。

## 当前作用

- 唯一消费端是数据生成器 `BlockListReport`，输出 `reports/blocks.json` 的 `definition` 字段。
- 不参与运行时方块注册：方块按实例注册进 `BuiltInRegistries.BLOCK`；颜色/材料变体用独立实例或 BlockState 属性表达，与 codec 无关。
- 不参与方块状态序列化：`BlockState.CODEC` 走方块注册 ID + `StateDefinition` 属性。
- 不参与方块实体数据：BE 走 NBT（`saveAdditional` / `loadAdditional`）。
- `simpleCodec` 的 `properties` 字段是 `Codec.unit` 空占位，只承载类型键，不序列化真实属性。
- 1.20.1 无 `codec()`、`BlockTypes`、`BLOCK_TYPE`，整栈仅 1.21 存在。

## 对模组编写的影响

- 继承上述抽象中间基类必须覆写 `codec()`；直继 `Block` 或继承具体基类（`RailBlock`、`SlabBlock`、`LeavesBlock`、`WaterlilyBlock` 等）继承具体 codec，无需覆写。
- `EntityBlock` 是接口（`newBlockEntity` + 默认 `getTicker`），不产生 codec 义务；`BaseEntityBlock` 是抽象类且重声明 codec，二者不同。本模组方块已用 `Block` + `EntityBlock` 接口组合，不继承 `BaseEntityBlock`。
- 原版范式：纯 `Properties` 构造器用 `simpleCodec(X::new)`；带构造参数用 `RecordCodecBuilder` 序列化参数（如 `ColoredFallingBlock` 的 `falling_dust_color`、`PistonBaseBlock` 的 `sticky`）；编译期常量参数用 `simpleCodec` + lambda 捕获（如 `ChestBlock`）。原版方块不用 `MapCodec.unit`，`unit` 用于无状态单例。
- 基类的非 codec 行为（朝向 `FACING` / `rotate` / `mirror`、下落重力、植物生存、铁轨矿车物理）跨版本字节码稳定，可借用。

## 跨版本兼容

- codec 整栈仅 1.21 存在，采用即强制 `//?` 分支；方块注册与变体表达走按实例注册或 BlockState 属性，跨版本稳定，codec 无收益。
- 基类行为稳定，但 API 形态有版本变更（`FallingBlock` / `BushBlock` 在 1.21 变抽象、`SandBlock` / `GravelBlock` 并入 `ColoredFallingBlock`、`GlassBlock` → `TransparentBlock`、`isPathfindable` 减参、可见性 public → protected）。这些由既有 extends / 签名分支处理，与 codec 无关。

## 守卫规则

本模组不注册 `BLOCK_TYPE`、不运行 `BlockListReport`，方块 codec 无运行时消费端。据此：

**codec 覆写只在编译强制时才允许存在。** 继承抽象中间基类、`codec()` 被重声明为抽象而必须覆写时，才建立 stub；凡可写可不写（继承具体基类或直继 `Block`，已有具体 codec 可继承）的 codec 一律不写。

1. 借用原版基类的稳定行为，不为消除 codec 而盲目改直继 `Block`。铁轨族因 `AbstractMinecart` 硬转 `BaseRailBlock`，必须保留基类。
2. **仅当继承抽象中间基类、编译强制要求覆写 `codec()` 时**，允许单独建立 1.21-only 的 stub codec：`//? if >=1.21.1` 内 `return MapCodec.unit(this);`，1.20.1 侧留空。按 [unification-policy.md](../unification-policy.md) 平台 API 例外在分支处写明原因（1.21 抽象重声明、无消费端、unit 占位），双节点编译验证。
3. **可写可不写的 codec 一律不写**：继承具体基类（`RailBlock`、`SlabBlock`、`LeavesBlock`、`WaterlilyBlock` 等）或直继 `Block` 的方块已有具体 codec 可继承，不写 codec 覆写；已有的冗余 `unit` 覆写删除。
4. stub 只中和 codec 形式义务，不追求功能化序列化（无消费端，写 `RecordCodecBuilder` 无收益）。
5. 配方 codec（`RecipeSerializer.codec()` / `streamCodec()`）与世界生成 codec 是活序列化、无稳定替代，不在此 stub 范围，按真平台差异保留最小分支。

## stub 示例

`RoadStripeRailBlock` 继承 `BaseRailBlock`（抽象 codec），纯 `Properties` 构造器，建立 1.21-only stub：

```java
public final class RoadStripeRailBlock extends BaseRailBlock {
    public RoadStripeRailBlock(Properties properties) {
        super(true, properties.noCollission());
        registerDefaultState(stateDefinition.any()
                .setValue(SHAPE, RailShape.NORTH_SOUTH)
                .setValue(REFLECTOR, false).setValue(WATERLOGGED, false));
    }

//? if >=1.21.1 {
    /*@Override
    public MapCodec<? extends BaseRailBlock> codec() {
        return MapCodec.unit(this);
    }
*///?} else {
//?}
    ...
}
```

- `MapCodec.unit(this)` 编码不写字段、解码恒返回该实例，仅满足抽象重声明；本模组无消费端，无运行时影响。
- 对照：`TrackBlock.Straight` 继承 `RailBlock`（具体 codec），无需 stub，删除冗余覆写即继承默认。

## 验证

- 双节点 `stonecutterGenerate` + `compileJava`：stub 在 1.21.1 满足抽象方法，1.20.1 侧无 codec。
- `spotlessCheck`；扫描确认具体基类 / `Block` 子类的冗余覆写已删除。
- stub 无运行时影响，无需行为 playtest；基类行为变更按 parity 流程另行记录。