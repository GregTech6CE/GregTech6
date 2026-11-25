package gregtech.content.block.misc;

import gregtech.engine.tool.GTToolHelper;
import gregtech.engine.tool.GTToolType;
import gregtech.engine.material.MaterialEquivalence;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTConstructionBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

import gregtech.content.block.GTBlock;
/**
 * GT6 concrete/reinforced-concrete behavior. Retained palette IDs read the old color state;
 * {@link FixedConstructionBlocks.Concrete} stores the color in each registered identity.
 */
public class ConcreteBlock extends GTBlock {
    public static final EnumProperty<DyeColor> COLOR = EnumProperty.create("color", DyeColor.class);
    private static final TagKey<Item> IRON_RODS = ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge", "rods/iron"));
    private static final TagKey<Item> STEEL_RODS = ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge", "rods/steel"));
    // GT6 ANY.Iron accepts these iron/steel material aliases for its stick form.
    private static final Set<String> IRON_FAMILY=gregtech.content.block.ConstructionRules.IRON_FAMILY;
    private final boolean reinforced;

    public ConcreteBlock(boolean reinforced, Properties properties) {
        super(properties.isValidSpawn((s, l, p, e) -> false));
        this.reinforced = reinforced;
        registerDefaultState(stateDefinition.any().hasProperty(COLOR)
                ? stateDefinition.any().setValue(COLOR, DyeColor.LIGHT_GRAY) : stateDefinition.any());
    }

    public boolean reinforced() { return reinforced; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        if (!(this instanceof FixedConstructionColor)) builder.add(COLOR);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState();
    }

    public static ItemStack coloredItem(Block block, DyeColor color) {
        Block variant = gregtech.content.registry.GTConstructionVariants.coloredBlock(block, color);
        if (variant != null) return new ItemStack(variant);
        ItemStack stack = new ItemStack(block);
        gregtech.engine.stack.BlockStateData.writeProperties(stack, java.util.Map.of("color", color.getName()));
        return stack;
    }

    public static DyeColor itemColor(ItemStack stack) {
        if (stack.getItem() instanceof net.minecraft.world.item.BlockItem item
                && item.getBlock() instanceof FixedConstructionColor fixed) return fixed.color();
        String color = gregtech.engine.stack.BlockStateData.getProperty(stack, "color");
        return DyeColor.byName(color == null ? "light_gray" : color, DyeColor.LIGHT_GRAY);
    }

    /** GT6 CS.DYES_INT, in DyeColor rather than GT6 metadata order. */
    public static int tint(DyeColor color) {
        return gregtech.content.block.ConstructionRules.tint(color.getName());
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return List.of(coloredItem(this, FixedConstructionColor.colorOf(state)));
    }

    @Override
    protected ItemStack onGetCloneItemStack(BlockState state, @Nullable HitResult target,
            BlockGetter level, BlockPos pos, @Nullable Player player) {
        return coloredItem(this, FixedConstructionColor.colorOf(state));
    }

    private static int rodSlot(Player player) {
        // The GT6 loop scans mainInventory from its last slot; armour/offhand do not count.
        for (int i = player.getInventory().items.size() - 1; i >= 0; i--) {
            ItemStack candidate = player.getInventory().getItem(i);
            if (ironFamilyRod(candidate)) return i;
        }
        return -1;
    }

    private static boolean ironFamilyRod(ItemStack stack) {
        if (stack.is(IRON_RODS) || stack.is(STEEL_RODS)) return true;
        var form = MaterialEquivalence.form(stack);
        return form != null && form.prefix() == MaterialPrefix.stick
                && IRON_FAMILY.contains(form.material().getName());
    }

    /** GT6 BlockConcrete: drill-click consumes one iron/steel stick and preserves the dye. */
    @Override
    protected InteractionResult useBlock(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (reinforced || !GTToolHelper.matchesTool(player.getItemInHand(hand), GTToolType.HAND_DRILL))
            return InteractionResult.PASS;
        int slot = rodSlot(player);
        if (slot < 0) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!level.setBlock(pos, FixedConstructionColor.recolor(GTConstructionBlocks.CONCRETE_REINFORCED.get().defaultBlockState(),
                FixedConstructionColor.colorOf(state)), Block.UPDATE_ALL)) return InteractionResult.FAIL;
        if (!player.getAbilities().instabuild) player.getInventory().getItem(slot).shrink(1);
        GTToolHelper.damageForToolClickReturn(player.getItemInHand(hand), 10000, player,
                hand == InteractionHand.OFF_HAND
                        ? net.minecraft.world.entity.EquipmentSlot.OFFHAND
                        : net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        return InteractionResult.CONSUME;
    }
}
