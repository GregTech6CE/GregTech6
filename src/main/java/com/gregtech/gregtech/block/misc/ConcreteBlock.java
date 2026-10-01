package com.gregtech.gregtech.block.misc;

import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.api.material.MaterialEquivalence;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTDecorBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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

import java.util.List;
import java.util.Set;

/**
 * GT6 {@code BlockConcrete}/{@code BlockConcreteReinforced}: one block id with all sixteen dye
 * metadata values. The named colour state takes the place of 1.7.10 metadata and is copied to
 * dropped/picked items through vanilla's BlockStateTag.
 */
public final class ConcreteBlock extends Block {
    public static final EnumProperty<DyeColor> COLOR = EnumProperty.create("color", DyeColor.class);
    private static final TagKey<Item> IRON_RODS = ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge", "rods/iron"));
    private static final TagKey<Item> STEEL_RODS = ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge", "rods/steel"));
    // GT6 ANY.Iron:119 accepts these iron/steel material aliases for its stick form.
    private static final Set<String> IRON_FAMILY=com.gregtech.gregtech.block.ConstructionRules.IRON_FAMILY;
    private final boolean reinforced;

    public ConcreteBlock(boolean reinforced, Properties properties) {
        super(properties);
        this.reinforced = reinforced;
        registerDefaultState(stateDefinition.any().setValue(COLOR, DyeColor.LIGHT_GRAY));
    }

    public boolean reinforced() { return reinforced; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(COLOR);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState();
    }

    public static ItemStack coloredItem(Block block, DyeColor color) {
        ItemStack stack = new ItemStack(block);
        CompoundTag stateTag = new CompoundTag();
        stateTag.putString("color", color.getName());
        stack.addTagElement("BlockStateTag", stateTag);
        return stack;
    }

    public static DyeColor itemColor(ItemStack stack) {
        CompoundTag tag = stack.getTagElement("BlockStateTag");
        return tag == null ? DyeColor.LIGHT_GRAY : DyeColor.byName(tag.getString("color"), DyeColor.LIGHT_GRAY);
    }

    /** GT6 CS.DYES_INT, in DyeColor rather than GT6 metadata order. */
    public static int tint(DyeColor color) {
        return com.gregtech.gregtech.block.ConstructionRules.tint(color.getName());
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return List.of(coloredItem(this, state.getValue(COLOR)));
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return coloredItem(this, state.getValue(COLOR));
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
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (reinforced || !GTToolHelper.matchesTool(player.getItemInHand(hand), GTToolType.HAND_DRILL))
            return InteractionResult.PASS;
        int slot = rodSlot(player);
        if (slot < 0) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!level.setBlock(pos, GTDecorBlocks.CONCRETE_REINFORCED.get().defaultBlockState()
                .setValue(COLOR, state.getValue(COLOR)), Block.UPDATE_ALL)) return InteractionResult.FAIL;
        if (!player.getAbilities().instabuild) player.getInventory().getItem(slot).shrink(1);
        GTToolHelper.damageForToolClickReturn(player.getItemInHand(hand), 10000, player,
                hand == InteractionHand.OFF_HAND
                        ? net.minecraft.world.entity.EquipmentSlot.OFFHAND
                        : net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        return InteractionResult.CONSUME;
    }
}
