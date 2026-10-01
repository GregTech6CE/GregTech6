package com.gregtech.gregtech.block.wood;

import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.content.material.generated.WoodMaterials;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/** GT6 {@code BlockTreeLog1}: the naturally fallen dry, rotten, mossy and frozen logs. */
public final class FallenLogBlock extends RotatedPillarBlock {
    public enum Kind { DRY, ROTTEN, MOSSY, FROZEN }

    private final Kind kind;

    public FallenLogBlock(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public Kind kind() { return kind; }

    /** GT6 returns bark, rotten/mossy wood dust (Forestry mulch fallback), or ice dust. */
    public ItemStack toolProduct() {
        return switch (kind) {
            case DRY -> {
                Item bark = ForgeRegistries.ITEMS.getValue(
                        ResourceLocation.fromNamespaceAndPath("gregtech", "dry_bark"));
                yield bark == null ? ItemStack.EMPTY : new ItemStack(bark);
            }
            case ROTTEN -> GTItems.getStack(MaterialPrefix.dust, WoodMaterials.WoodRotten);
            case MOSSY -> GTItems.getStack(MaterialPrefix.dust, WoodMaterials.WoodMossy);
            case FROZEN -> GTItems.getStack(MaterialPrefix.dust, Materials.Ice);
        };
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        ItemStack tool = player.getItemInHand(hand);
        if (!GTToolHelper.matchesTool(tool, GTToolType.AXE)
                && !GTToolHelper.matchesTool(tool, GTToolType.SAW)
                && !GTToolHelper.matchesTool(tool, GTToolType.KNIFE)) return InteractionResult.PASS;
        ItemStack output = toolProduct();
        if (output.isEmpty()) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!level.removeBlock(pos, false)) return InteractionResult.FAIL;
        com.gregtech.gregtech.api.fluid.HandContainerTransfer.give(player, output);
        // GT6 BlockTreeLog1 returns a tool-click cost of 1000, which the
        // port's shared tool-cost conversion translates to ten durability.
        GTToolHelper.damageForToolClickReturn(tool, 1000L, player);
        return InteractionResult.CONSUME;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return List.of(new ItemStack(this));
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return 5;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return 5;
    }
}
