# 配方序列化器 API 形态

## 子原因

`RecipeSerializer` 的序列化接口在两个版本上整体换代：1.20.1 用 `fromJson(ResourceLocation, JsonObject)` / `fromNetwork(ResourceLocation, FriendlyByteBuf)` / `toNetwork(FriendlyByteBuf, T)`；1.21.1 用 `codec() → MapCodec<T>` / `streamCodec() → StreamCodec<RegistryFriendlyByteBuf, T>`。连带差异包括 `Recipe.getId()` 在 1.21 移除（id 改由 `RecipeHolder` 携带）、`CraftingContainer` ↔ `CraftingInput`、`RegistryAccess` ↔ `HolderLookup.Provider`。这些是平台 API 运行时差异，序列化器主体分支保留。

包装原版配方的 GT 配方类（如 `SpikeConversionRecipe`、`ToolShapelessRecipe`）曾在此基础上额外持有 `base` 副本并逐方法委托。构造器已把 base 的 group/category/result/ingredients 复制进 super，委托覆写与 super 完全等价，属于冗余而非平台差异，应删除。

## 例子

### 修复前

```java
//? if >=1.21.1 {
    /*private final ShapelessRecipe base;
*///?}
    public SpikeConversionRecipe(ShapelessRecipe base) {
//? if >=1.21.1 {
        /*super(base.getGroup(), base.category(), base.getResultItem(RegistryAccess.EMPTY), base.getIngredients());
        this.base = base;
*///?} else {
        super(base.getId(), base.getGroup(), base.category(), base.getResultItem(RegistryAccess.EMPTY), base.getIngredients());
//?}
    }
//? if >=1.21.1 {
    /*@Override public ItemStack assemble(CraftingInput grid, HolderLookup.Provider lookup) {
        return base.assemble(grid, lookup);
    }
    @Override public ItemStack getResultItem(HolderLookup.Provider lookup) { return base.getResultItem(lookup); }
    // getRemainingItems / canCraftInDimensions / getGroup / category / getIngredients 同构委托
    public static final MapCodec<SpikeConversionRecipe> CODEC = new ShapelessRecipe.Serializer().codec()
            .xmap(SpikeConversionRecipe::new, recipe -> recipe.base);
*///?}
```

### 修复后

```java
    public SpikeConversionRecipe(ShapelessRecipe base) {
//? if >=1.21.1 {
        /*super(base.getGroup(), base.category(), base.getResultItem(RegistryAccess.EMPTY), base.getIngredients());
*///?} else {
        super(base.getId(), base.getGroup(), base.category(), base.getResultItem(RegistryAccess.EMPTY), base.getIngredients());
//?}
    }
//? if >=1.21.1 {
    /*public static final MapCodec<SpikeConversionRecipe> CODEC = new ShapelessRecipe.Serializer().codec()
            .xmap(SpikeConversionRecipe::new, recipe -> recipe);
*///?}
```

### 为什么这么修复

- `base` 字段与六个委托覆写删除后行为不变：构造器已把 base 的状态复制进 super，`super.assemble` / `super.getResultItem` 等与委托目标逐字等价；CODEC 的 `forGetter` 改用配方自身（它本身就是 `ShapelessRecipe`）。
- 构造器分支保留：1.20 `ShapelessRecipe` 构造器带 `ResourceLocation id`，1.21 不带（`Recipe.getId()` 移除的连带差异），属平台 API 例外。
- CODEC 分支保留：`MapCodec` / `StreamCodec` 在 1.20 不存在。按 [unification-policy.md](../unification-policy.md) 四条件：API 运行时差异成立；吸收需新增跨版本转发层，成本高于保留最小分支；非自身注册/初始化/包装所致；原因与验证见本段。影响面仅序列化路径；验证方式为双节点 `stonecutterGenerate` + `compileJava`，配方经数据包 JSON 与网络同步往返一致。
- `BookBindingRecipe` 的 `base` 字段两侧都保留：1.20 `toNetwork` 需要 `recipe.base` 传给 vanilla serializer，不属于冗余。

### 其他可能的修复方式

- 为所有包装类新增共享基类承载 base 与委托：被否决。方针禁止为保留差异新增转发层，且委托本身冗余，删比包更干净。
- 保留 base 副本只删委托覆写：被否决。base 与 super 状态同源，留字段徒增分叉风险，CODEC 直接用自身即可。