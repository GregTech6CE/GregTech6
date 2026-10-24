package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.blockentity.tool.BumbliaryBlockEntity;
import com.gregtech.gregtech.blockentity.energy.ReactorCoreBlockEntity;
import com.gregtech.gregtech.content.bumble.*;
import com.gregtech.gregtech.content.nuclear.ReactorCoolants;
import com.gregtech.gregtech.registry.GTBumbleBlocks;
import com.gregtech.gregtech.registry.GTEnergyNodes;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Read-only assertions shared by the two processes; no restoration/placement helpers here. */
final class RestartFixture {
    static boolean warm; // JVM-local negative control: phase 2 MUST observe false.
    static final BlockPos BASE = new BlockPos(20_000_000, 304, 20_000_000);
    static final String MARKER = "gt6-restart-phase1.properties";
    private RestartFixture() {}

    static BlockPos beePos(int index) { return BASE.offset(index * 2, 0, 0); }
    static BlockPos reactorPos(int index) { return BASE.offset(index * 2, 0, 6); }

    static Path world(GameTestHelper h) throws Exception {
        String configured = System.getenv("GT6_RESTART_DIRECTORY");
        h.assertTrue(configured != null && !configured.isBlank(), "explicit disposable directory required");
        Path directory = Path.of(configured).toRealPath();
        h.assertTrue(directory.getFileName().toString().startsWith("gt6-restart-"), "dedicated disposable directory name required");
        Path world = h.getLevel().getServer().getWorldPath(LevelResource.ROOT).toRealPath();
        h.assertTrue(world.startsWith(directory) && !world.equals(directory), "actual world must be inside explicit disposable directory");
        h.assertTrue(h.getLevel().players().isEmpty(), "no players permitted in disposable fixture world");
        return world;
    }

    static void nonTicking(GameTestHelper h) {
        h.assertTrue(BASE.getY() - 1 >= h.getLevel().getMinBuildHeight()
                && BASE.getY() + 2 < h.getLevel().getMaxBuildHeight(),
                "entire fixed checkpoint band must fit actual dimension build height");
        h.getLevel().getChunkAt(BASE); // FULL load, no force-loaded/ticking ticket.
        h.assertTrue(!h.getLevel().getChunkSource().isPositionTicking(new ChunkPos(BASE).toLong()),
                "remote fixture chunk must not naturally tick");
        BlockPos runner = h.absolutePos(BlockPos.ZERO);
        h.assertTrue(Math.abs((long) runner.getX() - BASE.getX()) > 1000
                || Math.abs((long) runner.getZ() - BASE.getZ()) > 1000,
                "runner template must remain far from persisted fixtures");
    }

    static CompoundTag genes(String nonce) {
        CompoundTag genes = new CompoundTag();
        BumbleBeeGenes.setTemperatureMin(genes, 0);
        BumbleBeeGenes.setTemperatureMax(genes, 10000);
        BumbleBeeGenes.setHumidityMin(genes, 0);
        BumbleBeeGenes.setHumidityMax(genes, 1);
        BumbleBeeGenes.setOutsideActive(genes, true);
        BumbleBeeGenes.setInsideActive(genes, true);
        BumbleBeeGenes.setLifeSpan(genes, 1200);
        BumbleBeeGenes.setOffspring(genes, 2);
        BumbleBeeGenes.setAggressiveness(genes, 0);
        BumbleBeeGenes.setDayActive(genes, false);
        BumbleBeeGenes.setNightActive(genes, false);
        genes.putString("restart.fixture.nonce", nonce);
        return genes;
    }

    static Properties readMarker(Path world) throws Exception {
        Properties properties = new Properties();
        try (var input = Files.newInputStream(world.resolve(MARKER))) { properties.load(input); }
        return properties;
    }

    static void assertState(GameTestHelper h, String nonce) {
        nonTicking(h);
        for (int i = 0; i < 4; i++) {
            boolean advanced = i >= 2, queen = i % 2 == 0;
            var block = (advanced ? GTBumbleBlocks.ADVANCED_BUMBLIARY : GTBumbleBlocks.BUMBLIARY).get();
            BlockPos pos = beePos(i);
            h.assertTrue(h.getLevel().getBlockState(pos).is(block), "persisted apiary block " + i);
            h.assertTrue(h.getLevel().getBlockEntity(pos) instanceof BumbliaryBlockEntity, "native loaded apiary " + i);
            var bee = (BumbliaryBlockEntity) h.getLevel().getBlockEntity(pos);
            h.assertTrue(bee.life() == (queen ? 1199 : 0) && bee.countdown() == (queen ? 1200 : 99), "exact persisted bee progress " + i);
            var royal = bee.inventory().getStackInSlot(bee.layout().royal());
            var type = queen ? BumbleBeeType.QUEEN : BumbleBeeType.PRINCESS;
            var expected = BumbleBeeType.stack(GTBumbleSpecies.byId(30), type, genes(nonce), 1);
            h.assertTrue(royal.is(expected.getItem()) && royal.getCount() == 1
                    && genes(nonce).equals(BumbleBeeGenes.peek(royal)), "persisted royal identity/count/genome " + i);
            var brood = bee.brood();
            h.assertTrue(queen ? brood.length >= 3 && brood.length <= 5 : brood.length == 0,
                    "native pending brood range " + i);
            h.assertTrue(bee.inventory().getStackInSlot(bee.layout().drone()).isEmpty(), "no consumed drone duplication " + i);
        }
        for (int i = 0; i < 2; i++) {
            BlockPos pos = reactorPos(i);
            var block = (i == 0 ? GTEnergyNodes.REACTOR_CORE_BLOCK : GTEnergyNodes.REACTOR_CORE_2X2).get();
            h.assertTrue(h.getLevel().getBlockState(pos).is(block), "persisted reactor block " + i);
            h.assertTrue(h.getLevel().getBlockEntity(pos) instanceof ReactorCoreBlockEntity, "native loaded reactor " + i);
            var reactor = (ReactorCoreBlockEntity) h.getLevel().getBlockEntity(pos);
            h.assertTrue(reactor.rods.length == (i == 0 ? 1 : 4) && reactor.stopped && !reactor.failed,
                    "persisted reactor variant/stopped state " + i);
            h.assertTrue(reactor.output.getAmount() == 64320 && reactor.output.capacity() == 10240000
                    && reactor.output.getFluid().getFluid() == ReactorCoolants.DISTILLED.outputFluid()
                    && reactor.input.isEmpty() && reactor.storedHeat == 0, "exact persisted expanded Steam tank " + i);
        }
    }
    static CompoundTag save(GameTestHelper h, net.minecraft.world.level.block.entity.BlockEntity be) {
//? if >=1.21.1 {
/*        return be.saveWithoutMetadata(h.getLevel().registryAccess());
*///?} else {
        return be.saveWithoutMetadata();
//?}
    }

    // Canonical comparison only: marker strings are NEVER decoded or loaded into a BE.
    static String canonical(net.minecraft.nbt.Tag tag) {
        if (tag instanceof CompoundTag compound) {
            StringBuilder out = new StringBuilder("{");
            for (String key : new java.util.TreeSet<>(compound.getAllKeys())) {
                out.append(key.length()).append(':').append(key).append('=').append(canonical(compound.get(key))).append(';');
            }
            return out.append('}').toString();
        }
        if (tag instanceof net.minecraft.nbt.ListTag list) {
            StringBuilder out = new StringBuilder("[");
            for (net.minecraft.nbt.Tag entry : list) out.append(canonical(entry)).append(';');
            return out.append(']').toString();
        }
        return tag.toString();
    }

    static void recordState(GameTestHelper h, Properties marker) {
        for (int i = 0; i < 6; i++) {
            BlockPos pos = i < 4 ? beePos(i) : reactorPos(i - 4);
            marker.setProperty("state." + i, canonical(save(h, h.getLevel().getBlockEntity(pos))));
        }
    }

    static void compareState(GameTestHelper h, Properties marker) {
        Properties actual = new Properties();
        recordState(h, actual);
        for (int i = 0; i < 6; i++) h.assertTrue(actual.getProperty("state." + i).equals(marker.getProperty("state." + i)),
                "exact native disk state including every inventory/brood/heat/fluid field " + i);
    }

    static void continueOneTick(GameTestHelper h) {
        for (int i = 0; i < 4; i++) {
            var bee = (BumbliaryBlockEntity) h.getLevel().getBlockEntity(beePos(i));
            CompoundTag before = save(h, bee);
            bee.serverTick();
            h.assertTrue(bee.life() == (i % 2 == 0 ? 1198 : 0) && bee.countdown() == (i % 2 == 0 ? 1200 : 98),
                    "exact next native progress tick " + i);
            CompoundTag after = save(h, bee);
            before.remove(BumbliaryBlockEntity.NBT_PROGRESS); before.remove(BumbliaryBlockEntity.NBT_COOLDOWN);
            after.remove(BumbliaryBlockEntity.NBT_PROGRESS); after.remove(BumbliaryBlockEntity.NBT_COOLDOWN);
            h.assertTrue(before.equals(after), "no inventory or pending brood duplicated by first post-load tick " + i);
        }
    }

}
