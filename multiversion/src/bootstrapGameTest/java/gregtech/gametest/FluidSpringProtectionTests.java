package gregtech.gametest;

import gregtech.content.block.FluidSpringBlock;
import gregtech.content.blockentity.FluidSpringBlockEntity;
import gregtech.content.registry.GTFluidSprings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Issue #106: actual registered springs retain bedrock protection and creative access. */
@GameTestHolder("gt6ce_utility_fixes")
@PrefixGameTestTemplate(false)
public final class FluidSpringProtectionTests {
    private static final class Area implements AutoCloseable {
        private record Saved(BlockState state, CompoundTag data) {}
        final GameTestHelper h;
        final BlockPos pos;
        final Map<BlockPos, Saved> saved = new LinkedHashMap<>();
        final Set<Integer> originalItems;

        Area(GameTestHelper h) {
            this.h = h;
            // Keep real explosions far above the test command blocks and other fixtures.
            pos = h.absolutePos(new BlockPos(4, 80, 4));
            originalItems = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(9))
                    .stream().map(ItemEntity::getId).collect(Collectors.toSet());
        }

        void remember(BlockPos at) {
            if (!saved.containsKey(at)) {
                BlockEntity entity = h.getLevel().getBlockEntity(at);
//? if >=1.21.1 {
/*                var data = entity == null ? null : entity.saveWithoutMetadata(h.getLevel().registryAccess());
*///?} else {
                var data = entity == null ? null : entity.saveWithoutMetadata();
//?}
                saved.put(at.immutable(), new Saved(h.getLevel().getBlockState(at), data));
            }
        }

        void set(BlockPos at, BlockState state) {
            remember(at);
            h.getLevel().setBlock(at, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }

        void preserveBlastArea() {
            for (var at : BlockPos.betweenClosed(pos.offset(-8, -8, -8), pos.offset(8, 8, 8))) remember(at);
        }

        @Override public void close() {
            for (var item : h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(9)))
                if (!originalItems.contains(item.getId())) item.discard();
            for (var entry : saved.entrySet()) {
                var at = entry.getKey(); var previous = entry.getValue();
                if (!h.getLevel().getBlockState(at).equals(previous.state())) {
                    h.getLevel().removeBlockEntity(at);
                    h.getLevel().setBlock(at, previous.state(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                }
                var entity = h.getLevel().getBlockEntity(at);
                if (entity != null && previous.data() != null) {
//? if >=1.21.1 {
/*                    entity.loadWithComponents(previous.data(), h.getLevel().registryAccess());
*///?} else {
                    entity.load(previous.data());
//?}
                }
            }
        }
    }

    private static ServerPlayer player(GameTestHelper h, BlockPos pos, GameType mode) {
//? if >=1.21.1 {
/*        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),
*///?} else {
        var player = net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),
//?}
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "[gt-spring-physics]"));
        player.setGameMode(mode);
        player.getInventory().clearContent();
        player.setPos(pos.getX() - 2, pos.getY(), pos.getZ() + .5);
        return player;
    }

    @GameTest(template = "test_empty")
    public static void allRegisteredSpringsRejectSurvivalMining(GameTestHelper h) {
        try (var area = new Area(h)) {
            var player = player(h, area.pos, GameType.SURVIVAL);
            h.assertTrue(GTFluidSprings.allBlocks().length == 8, "all seven fixed springs plus the legacy carrier");
            try {
                for (var block : GTFluidSprings.allBlocks()) {
                    var state = block.defaultBlockState();
                    area.set(area.pos, state);
                    h.assertTrue(state.getDestroySpeed(h.getLevel(), area.pos) == -1.0F,
                            "actual spring has original bedrock-like unbreakable hardness");
                    h.assertTrue(block.getExplosionResistance() == Blocks.BEDROCK.getExplosionResistance(),
                            "registered spring uses native bedrock explosion resistance");
                    for (var held : new ItemStack[]{ItemStack.EMPTY, new ItemStack(Items.IRON_PICKAXE),
                            new ItemStack(Items.DIAMOND_PICKAXE), new ItemStack(Items.NETHERITE_PICKAXE)}) {
                        player.setItemInHand(InteractionHand.MAIN_HAND, held);
                        h.assertTrue(state.getDestroyProgress(player, h.getLevel(), area.pos) == 0,
                                "native survival mining cannot make progress even with a high-tier pickaxe");
                    }
                }
                area.set(area.pos, Blocks.STONE.defaultBlockState());
                h.assertTrue(Blocks.STONE.defaultBlockState().getDestroyProgress(player, h.getLevel(), area.pos) > 0,
                        "the same survival player and pickaxe can make progress on ordinary stone");
            } finally { player.discard(); }
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void actualTntBlastPreservesSpringsWhileDestroyingStone(GameTestHelper h) {
        try (var area = new Area(h)) {
            area.preserveBlastArea();
            for (var block : GTFluidSprings.allBlocks()) {
                area.set(area.pos, block.defaultBlockState());
                area.set(area.pos.east(), Blocks.AIR.defaultBlockState());
                area.set(area.pos.east(2), Blocks.STONE.defaultBlockState());
                area.set(area.pos.north(2), Blocks.BEDROCK.defaultBlockState());
                var before = h.getLevel().getBlockEntity(area.pos);
                h.getLevel().explode(null, area.pos.getX() + 1.5, area.pos.getY() + .5,
                        area.pos.getZ() + .5, 4, Level.ExplosionInteraction.TNT);
                h.assertTrue(h.getLevel().getBlockState(area.pos).is(block)
                                && h.getLevel().getBlockEntity(area.pos) == before,
                        "a real TNT-strength explosion preserves the spring and its existing block entity");
                h.assertTrue(h.getLevel().getBlockState(area.pos.east(2)).isAir(),
                        "the same real blast destroys equally close ordinary stone");
                h.assertTrue(h.getLevel().getBlockState(area.pos.north(2)).is(Blocks.BEDROCK),
                        "native bedrock is also preserved as the protection control");
            }
        }
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void creativeCloningRemovalAndEmissionRemainAvailable(GameTestHelper h) {
        try (var area = new Area(h)) {
            var player = player(h, area.pos, GameType.CREATIVE);
            try {
                for (var block : GTFluidSprings.allBlocks()) {
                    var state = block.defaultBlockState();
                    area.set(area.pos, state);
                    area.set(area.pos.above(), Blocks.AIR.defaultBlockState());
//? if >=1.21.1 {
/*                    var clone = block.getCloneItemStack(state, new BlockHitResult(Vec3.atCenterOf(area.pos),
                            Direction.UP, area.pos, false), h.getLevel(), area.pos, player);
*///?} else {
                    var clone = block.getCloneItemStack(h.getLevel(), area.pos, state);
//?}
                    h.assertTrue(clone.is(block.asItem()) && clone.getCount() == 1,
                            "creative clone preserves the actual registered spring identity");
                    var be = (FluidSpringBlockEntity) h.getLevel().getBlockEntity(area.pos);
                    if (((FluidSpringBlock) block).spring() != null) {
                        be.tick();
                        be.emit();
                        h.assertTrue(be.active() && h.getLevel().getBlockState(area.pos.above())
                                        .getFluidState().getType() == be.fluid(),
                                "protection leaves native activation and the registered fluid output intact");
                    }
                    h.assertTrue(player.gameMode.destroyBlock(area.pos) && h.getLevel().isEmptyBlock(area.pos),
                            "the real creative destruction path still removes protected springs");
                }
            } finally { player.discard(); }
        }
        h.succeed();
    }
}
