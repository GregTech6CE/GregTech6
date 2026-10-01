package com.gregtech.gregtech.block.plant;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

/**
 * GT6 crop and grass bales. Grass has four material states; only fresh and
 * moldy bales continue ageing after placement (BlockBaleGrass).
 */
public final class BaleBlock extends RotatedPillarBlock {
    public enum Stage { CROP, FRESH, DRY, MOLDY, ROTTEN }

    private final Stage stage;

    public BaleBlock(Stage stage, Properties properties) {
        super(properties);
        this.stage = stage;
    }

    public Stage stage() { return stage; }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState previous, boolean moving) {
        super.onPlace(state, level, pos, previous, moving);
        if (!level.isClientSide && state.getBlock() != previous.getBlock() && canAge()) {
            level.scheduleTick(pos, this, 2400 + level.random.nextInt(2400));
        }
    }

    private boolean canAge() {
        return stage == Stage.FRESH || stage == Stage.MOLDY;
    }

    /** Mirrors GT6's scheduled drying/rotting checks while retaining pillar orientation. */
    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!canAge()) return;
        level.scheduleTick(pos, this, 1100 + random.nextInt(200));
        if (random.nextInt(3) > 0) return;
        if (level.dimension() == Level.NETHER && stage == Stage.FRESH) {
            replace(state, level, pos, Stage.DRY);
            return;
        }

        // GT6 WD.envTemp = 270 + biomeTemperature * 20 Kelvin; C + 10 is 283 K.
        boolean warm = level.getBiome(pos).value().getBaseTemperature() >= 0.65F;
        if (random.nextInt(3) > 0 && !warm) return;
        boolean sunny = level.isDay() && !level.isRaining() && level.canSeeSky(pos.above(2));
        if (random.nextInt(3) > 0 && !sunny) return;

        boolean wet = level.getBiome(pos).value().getModifiedClimateSettings().downfall() > 0.8F;
        if (!wet) {
            for (Direction direction : Direction.values()) {
                if (level.getFluidState(pos.relative(direction)).is(FluidTags.WATER)) {
                    wet = true;
                    break;
                }
            }
        }
        wet |= level.isRainingAt(pos.above(2));
        if (stage == Stage.FRESH) {
            replace(state, level, pos, wet ? Stage.MOLDY : Stage.DRY);
        } else if (wet || random.nextInt(42) == 0) {
            replace(state, level, pos, Stage.ROTTEN);
        }
    }

    private static void replace(BlockState state, ServerLevel level, BlockPos pos, Stage target) {
        String id = switch (target) {
            case DRY -> "bale_grass_dry";
            case MOLDY -> "bale_grass_moldy";
            case ROTTEN -> "bale_grass_rotten";
            default -> throw new IllegalArgumentException("Not an ageing result: " + target);
        };
        var block = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech", id));
        if (block instanceof BaleBlock) {
            level.setBlock(pos, block.defaultBlockState().setValue(AXIS, state.getValue(AXIS)), 3);
        }
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return 150;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return 150;
    }
}
