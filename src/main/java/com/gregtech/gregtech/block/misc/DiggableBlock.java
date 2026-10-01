package com.gregtech.gregtech.block.misc;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.PlantType;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;

/**
 * GT6 {@code BlockDiggable} metadata 0–6 split into named blocks. Mud and turf have gravity and
 * halve a walking creature's horizontal motion; the five clays stay put and cannot grow plants.
 * Each variant always drops four of its exact GT6 ball/ingot item, independently of Fortune.
 */
public final class DiggableBlock extends FallingBlock {
    public enum Variant {
        MUD("mud", "mud_2", MapColor.DIRT),
        BROWN_CLAY("clay_brown", "brown_clay", MapColor.CLAY),
        TURF("turf", null, MapColor.COLOR_BROWN),
        RED_CLAY("clay_red", "red_clay", MapColor.COLOR_RED),
        YELLOW_CLAY("clay_yellow", "yellow_clay", MapColor.COLOR_YELLOW),
        BLUE_CLAY("clay_blue", "blue_clay", MapColor.COLOR_BLUE),
        WHITE_CLAY("clay_white", "white_clay", MapColor.SNOW);

        private final String iconName;
        private final String dropItemId;
        private final MapColor mapColor;

        Variant(String iconName, String dropItemId, MapColor mapColor) {
            this.iconName = iconName;
            this.dropItemId = dropItemId;
            this.mapColor = mapColor;
        }

        public String iconName() { return iconName; }
        public boolean isClay() { return this != MUD && this != TURF; }
        public boolean isLoose() { return !isClay(); }

        /** GT6's {@code OM.data}: four material units for every clay and peat block. */
        @Nullable public GTMaterial material() {
            return switch (this) {
                case MUD -> null; // GT6's MT.UNUSED.Mud has no material composition.
                case BROWN_CLAY -> Materials.ClayBrown;
                case TURF -> Materials.Peat;
                case RED_CLAY -> Materials.ClayRed;
                case YELLOW_CLAY -> Materials.Bentonite;
                case BLUE_CLAY -> Materials.Palygorskite;
                case WHITE_CLAY -> Materials.Kaolinite;
            };
        }

        public static Variant forIconName(String name) {
            for (Variant value : values()) if (value.iconName.equals(name)) return value;
            throw new IllegalArgumentException("Not a GT6 diggable variant: " + name);
        }
    }

    private final Variant variant;

    public DiggableBlock(Variant variant) {
        super(BlockBehaviour.Properties.of().mapColor(variant.mapColor).strength(0.5F, 0.5F)
                .sound(SoundType.GRAVEL));
        this.variant = variant;
    }

    public Variant variant() { return variant; }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        if (variant == Variant.TURF) {
            ItemStack peat = GTItems.getStack(MaterialPrefix.ingot, Materials.Peat, 4);
            if (peat.isEmpty()) throw new IllegalStateException("Missing GT6 peat ingot drop");
            return List.of(peat);
        }
        Item item = ForgeRegistries.ITEMS.getValue(GregTech.id(variant.dropItemId));
        if (item == null || item == net.minecraft.world.item.Items.AIR)
            throw new IllegalStateException("Missing GT6 diggable drop " + variant.dropItemId);
        return List.of(new ItemStack(item, 4));
    }

    @Override
    public boolean canSustainPlant(BlockState state, BlockGetter level, BlockPos pos,
                                   Direction side, IPlantable plant) {
        if (variant.isClay()) return false;
        if (plant == Blocks.SUGAR_CANE || plant instanceof BushBlock) return true;
        PlantType type = plant.getPlantType(level, pos.relative(side));
        return type == PlantType.PLAINS || type == PlantType.WATER
                || type == PlantType.DESERT || type == PlantType.BEACH;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (variant.isLoose() && entity instanceof LivingEntity)
            entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.5, 1.0, 0.5));
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        if (variant.isLoose()) super.onPlace(state, level, pos, oldState, moved);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return variant.isLoose() ? super.updateShape(state, side, neighbor, level, pos, neighborPos) : state;
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (variant.isLoose()) super.tick(state, level, pos, random);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (variant.isLoose()) super.animateTick(state, level, pos, random);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level,
                                List<Component> tooltip, TooltipFlag flag) {
        if (variant.isLoose()) {
            tooltip.add(Component.translatable("gt.lang.walkspeed").withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable("gt.lang.gravity").withStyle(ChatFormatting.GOLD));
        }
        tooltip.add(Component.translatable("gt.lang.pistonpush").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("gt.lang.harvest.shovel").withStyle(ChatFormatting.GRAY));
    }
}
