package gregtech.platform;

//? if >=1.21.1 {
/*import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.ItemInteractionResult;
*///?} else {
import gregtech.GregTech;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.MobType;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
//? if >=1.21.1 {
/**///?} else {
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;
//?}
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.StackWalker.Option;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/**
 * Platform shim. NeoForge has a class with the same name and the same methods.
 * Each method keeps the behavior of the Forge call it replaces, except {@link #logger()} which
 * uses per-class SLF4J loggers on both loaders.
 */
public final class Mc {
    private Mc() {}


    /** Names the logger after the calling class. */
    public static Logger logger() {
        Class<?> caller = StackWalker.getInstance(Option.RETAIN_CLASS_REFERENCE)
                .walk(frames -> frames.skip(1).findFirst().orElseThrow().getDeclaringClass());
        return LoggerFactory.getLogger(caller);
    }

    /** The Forge path is lowercased by {@code GregTech.id}; the NeoForge path is not. */
    public static ResourceLocation gregtechId(String path) {
//? if >=1.21.1 {
/*        return ResourceLocation.fromNamespaceAndPath("gt6ce", path);
*///?} else {
        return GregTech.id(path);
//?}
    }

    public static ResourceLocation id(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    public static ResourceLocation parseId(String id) {
        return ResourceLocation.parse(id);
    }

    public static Item item(ResourceLocation id) {
//? if >=1.21.1 {
/*        return BuiltInRegistries.ITEM.get(id);
*///?} else {
        return ForgeRegistries.ITEMS.getValue(id);
//?}
    }

    public static ResourceLocation itemId(Item item) {
//? if >=1.21.1 {
/*        return BuiltInRegistries.ITEM.getKey(item);
*///?} else {
        return ForgeRegistries.ITEMS.getKey(item);
//?}
    }

    public static Block block(ResourceLocation id) {
//? if >=1.21.1 {
/*        return BuiltInRegistries.BLOCK.get(id);
*///?} else {
        return ForgeRegistries.BLOCKS.getValue(id);
//?}
    }

    public static ResourceLocation blockId(Block block) {
//? if >=1.21.1 {
/*        return BuiltInRegistries.BLOCK.getKey(block);
*///?} else {
        return ForgeRegistries.BLOCKS.getKey(block);
//?}
    }

    public static Fluid fluid(ResourceLocation id) {
//? if >=1.21.1 {
/*        return BuiltInRegistries.FLUID.get(id);
*///?} else {
        return ForgeRegistries.FLUIDS.getValue(id);
//?}
    }

    public static ResourceLocation fluidId(Fluid fluid) {
//? if >=1.21.1 {
/*        return BuiltInRegistries.FLUID.getKey(fluid);
*///?} else {
        return ForgeRegistries.FLUIDS.getKey(fluid);
//?}
    }

    public static boolean hasFluid(ResourceLocation id) {
//? if >=1.21.1 {
/*        return BuiltInRegistries.FLUID.containsKey(id);
*///?} else {
        return ForgeRegistries.FLUIDS.containsKey(id);
//?}
    }

    public static Iterable<Fluid> fluids() {
//? if >=1.21.1 {
/*        return BuiltInRegistries.FLUID;
*///?} else {
        return ForgeRegistries.FLUIDS.getValues();
//?}
    }

    public static Iterable<Block> blocks() {
//? if >=1.21.1 {
/*        return BuiltInRegistries.BLOCK;
*///?} else {
        return ForgeRegistries.BLOCKS.getValues();
//?}
    }

    public static Iterable<Item> items() {
//? if >=1.21.1 {
/*        return BuiltInRegistries.ITEM;
*///?} else {
        return ForgeRegistries.ITEMS.getValues();
//?}
    }

    public static Stream<Item> itemStream() {
//? if >=1.21.1 {
/*        return BuiltInRegistries.ITEM.stream();
*///?} else {
        return ForgeRegistries.ITEMS.getValues().stream();
//?}
    }

    public static boolean sameItemData(ItemStack a, ItemStack b) {
//? if >=1.21.1 {
/*        return ItemStack.isSameItemSameComponents(a, b);
*///?} else {
        return ItemStack.isSameItemSameTags(a, b);
//?}
    }

    public static void setCustomName(ItemStack stack, Component name) {
//? if >=1.21.1 {
/*        stack.set(DataComponents.CUSTOM_NAME, name);
*///?} else {
        stack.setHoverName(name);
//?}
    }

    /**
     * The stack a player picks when they middle-click this block.
     *
     * <p>Forge 1.20.1 exposes the hook through {@code IForgeBlockState} and NeoForge 1.21.1 through
     * {@code IBlockStateExtension}; both dispatch to the block's own {@code getCloneItemStack}, and
     * {@code LevelReader} narrows to the 1.20.1 {@code BlockGetter} parameter, so one call serves
     * both nodes.</p>
     */
    public static ItemStack cloneItemStack(BlockState state, @Nullable HitResult target,
            LevelReader level, BlockPos pos, @Nullable Player player) {
        return state.getCloneItemStack(target, level, pos, player);
    }

    public static InteractionResult useBlock(BlockState state, ItemStack stack, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
//? if >=1.21.1 {
/*        ItemInteractionResult itemResult = state.useItemOn(stack, level, player, hand, hit);
        return itemResult == ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
                ? state.useWithoutItem(level, player, hit)
                : itemResult.result();
*///?} else {
        return state.use(level, player, hand, hit);
//?}
    }

//? if >=1.21.1 {
/*    public static boolean isArthropod(LivingEntity entity) {
        return entity.getType().is(EntityTypeTags.ARTHROPOD);
    }

    public static boolean isUndead(LivingEntity entity) {
        return entity.getType().is(EntityTypeTags.UNDEAD);
    }
*///?} else {
    public static boolean isArthropod(LivingEntity entity) {
        return entity.getMobType() == MobType.ARTHROPOD;
    }

    public static boolean isUndead(LivingEntity entity) {
        return entity.getMobType() == MobType.UNDEAD;
    }
//?}

//? if >=1.21.1 {
/*    public static void openMenu(ServerPlayer player, MenuProvider provider,
            Consumer<RegistryFriendlyByteBuf> extraData) {
        player.openMenu(provider, extraData);
    }
*///?} else {
    public static void openMenu(ServerPlayer player, MenuProvider provider,
            Consumer<FriendlyByteBuf> extraData) {
        NetworkHooks.openScreen(player, provider, extraData);
    }
//?}
}
