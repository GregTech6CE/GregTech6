package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.misc.SpikeBlock;
import com.gregtech.gregtech.registry.GTDecorBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Exact damage and collision regression for GT6 BlockBaseSpike's port. */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class SpikeBlockTests {
    private static final BlockPos ORIGIN = new BlockPos(2, 2, 2);

    private static BlockPos floor(GameTestHelper helper, int dx) {
        var p = helper.absolutePos(ORIGIN.offset(dx, 0, 0));
        helper.getLevel().setBlockAndUpdate(p.below(), Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
        return p;
    }

    private static <T extends Entity> T spawnOnFloor(GameTestHelper helper, EntityType<T> type, int dx) {
        floor(helper, dx);
        return helper.spawn(type, ORIGIN.offset(dx, 0, 0));
    }

    @GameTest(template = "test_fusion_empty", timeoutTicks = 200)
    public static void spikeShapeDirectionAndLootPassThrough(GameTestHelper helper) {
        SpikeBlock spike = (SpikeBlock) GTDecorBlocks.SPIKE_STEEL.get();
        var level = helper.getLevel();
        var p = floor(helper, 0);
        var omni = spike.defaultBlockState();
        helper.assertTrue(omni.getValue(SpikeBlock.MODE) == SpikeBlock.Mode.OMNI,
                "dungeon spike defaults to GT6 omni variant (meta 6/14)");
        helper.assertTrue(omni.getCollisionShape(level, p).bounds().minY == 2.0 / 16
                        && omni.getCollisionShape(level, p).bounds().maxY == 14.0 / 16,
                "omni collision is GT6's inset 2..14 px cube");
        var down = omni.setValue(SpikeBlock.MODE, SpikeBlock.Mode.WALL)
                .setValue(SpikeBlock.FACING, Direction.DOWN);
        var up = down.setValue(SpikeBlock.FACING, Direction.UP);
        helper.assertTrue(down.getCollisionShape(level, p).bounds().maxY == 0.6,
                "floor wall spikes occupy the lower 0.6 block");
        helper.assertTrue(up.getCollisionShape(level, p).bounds().minY == 0.4,
                "ceiling wall spikes occupy the upper 0.6 block");
        helper.assertTrue(down.getShape(level, p).equals(Shapes.block()),
                "GT6's selection outline remains a full block");
        var configured = omni.setValue(SpikeBlock.MODE, SpikeBlock.Mode.FALLING)
                .setValue(SpikeBlock.SECONDARY, true);
        level.setBlock(p, configured, 3);
        var drops = net.minecraft.world.level.block.Block.getDrops(configured, level, p, null);
        helper.assertTrue(drops.size() == 1 && drops.get(0).is(spike.asItem()),
                "configured spike drops exactly one spike item");
        var stateTag = drops.get(0).getTagElement("BlockStateTag");
        helper.assertTrue(stateTag != null && "falling".equals(stateTag.getString("mode"))
                        && "true".equals(stateTag.getString("secondary")),
                "mined GT6 variant preserves material and falling mode");
        var picked = spike.getCloneItemStack(level, p, configured);
        var pickedState = picked.getTagElement("BlockStateTag");
        helper.assertTrue(pickedState != null && "falling".equals(pickedState.getString("mode"))
                        && "true".equals(pickedState.getString("secondary")),
                "creative pick-block preserves the configured variant");
        ItemEntity loot = new ItemEntity(level, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5,
                new ItemStack(Items.IRON_INGOT));
        helper.assertTrue(down.getCollisionShape(level, p, CollisionContext.of(loot)).isEmpty(),
                "spikes do not obstruct item drops falling through mob farm");
        loot.discard();
        helper.succeed();
    }

    @GameTest(template = "test_fusion_empty", timeoutTicks = 200)
    public static void spikeDamageAndMaterialImmunities(GameTestHelper helper) {
        var level = helper.getLevel();
        var steel = (SpikeBlock) GTDecorBlocks.SPIKE_STEEL.get();
        var wall = steel.defaultBlockState().setValue(SpikeBlock.MODE, SpikeBlock.Mode.WALL);
        var omni = steel.defaultBlockState();
        var a = spawnOnFloor(helper, EntityType.COW, 4);
        var b = spawnOnFloor(helper, EntityType.COW, 8);
        a.invulnerableTime = 0;
        b.invulnerableTime = 0;
        steel.entityInside(wall, level, a.blockPosition(), a);
        steel.entityInside(omni, level, b.blockPosition(), b);
        helper.assertTrue(a.getHealth() == 2.0F, "steel wall spike deals GT6's 8 damage");
        helper.assertTrue(b.getHealth() == 6.0F, "steel omni spike deals half damage, 4");

        var golem = spawnOnFloor(helper, EntityType.IRON_GOLEM, 12);
        helper.assertTrue(steel.damageFor(wall, golem) == 0,
                "blue/red steel spikes exempt iron golems");
        var slime = spawnOnFloor(helper, EntityType.SLIME, 16);
        var metal = (SpikeBlock) GTDecorBlocks.SPIKE_METAL.get();
        var metalWall = metal.defaultBlockState().setValue(SpikeBlock.MODE, SpikeBlock.Mode.WALL);
        helper.assertTrue(metal.damageFor(metalWall, slime) == 20,
                "copper spike does GT6's anti-slime 20 damage");
        helper.assertTrue(metal.damageFor(metalWall.setValue(SpikeBlock.SECONDARY, true), slime) == 0,
                "lead spike excludes slimes");
        var sharp = (SpikeBlock) GTDecorBlocks.SPIKE_SHARP.get();
        helper.assertTrue(sharp.damageFor(sharp.defaultBlockState()
                        .setValue(SpikeBlock.MODE, SpikeBlock.Mode.WALL), golem) == 0,
                "steel sharp spike excludes golems");
        helper.assertTrue(sharp.damageFor(sharp.defaultBlockState()
                        .setValue(SpikeBlock.MODE, SpikeBlock.Mode.WALL)
                        .setValue(SpikeBlock.SECONDARY, true), golem) == 10,
                "titanium sharp spike affects golems");
        var skeleton = spawnOnFloor(helper, EntityType.SKELETON, 20);
        var fancy = (SpikeBlock) GTDecorBlocks.SPIKE_FANCY.get();
        helper.assertTrue(fancy.damageFor(fancy.defaultBlockState()
                        .setValue(SpikeBlock.MODE, SpikeBlock.Mode.WALL), skeleton) == 20,
                "gold spike's undead bonus also applies to skeletons");
        var stronger = (SpikeBlock) GTDecorBlocks.SPIKE_SUPER.get();
        helper.assertTrue(stronger.damageFor(stronger.defaultBlockState()
                        .setValue(SpikeBlock.MODE, SpikeBlock.Mode.WALL), a) == 15
                        && stronger.damageFor(stronger.defaultBlockState()
                        .setValue(SpikeBlock.SECONDARY, true), a) == 25,
                "tungstensteel wall and adamantium omni damage match GT6's 15/25 table");
        a.discard(); b.discard(); golem.discard(); slime.discard(); skeleton.discard();
        helper.succeed();
    }

    @GameTest(template = "test_fusion_empty", timeoutTicks = 200)
    public static void fallingSpikeActuallyFalls(GameTestHelper helper) {
        var level = helper.getLevel();
        var spike = (SpikeBlock) GTDecorBlocks.SPIKE_SHARP.get();
        var p = helper.absolutePos(ORIGIN.offset(0, 5, 6));
        for (int dy = -3; dy <= 0; dy++) level.setBlockAndUpdate(p.offset(0, dy, 0), Blocks.AIR.defaultBlockState());
        var falling = spike.defaultBlockState().setValue(SpikeBlock.MODE, SpikeBlock.Mode.FALLING)
                .setValue(SpikeBlock.SECONDARY, true);
        level.setBlockAndUpdate(p, falling);
        spike.tick(falling, level, p, RandomSource.create(1));
        helper.assertTrue(level.getBlockState(p).isAir(), "GT6 falling spike releases its source block");
        var entities = level.getEntitiesOfClass(FallingBlockEntity.class,
                new net.minecraft.world.phys.AABB(p).inflate(2));
        helper.assertTrue(entities.stream().anyMatch(e -> e.getBlockState().is(spike)
                        && e.getBlockState().getValue(SpikeBlock.SECONDARY)),
                "falling entity preserves secondary material state");
        entities.forEach(FallingBlockEntity::discard);
        helper.succeed();
    }
}
