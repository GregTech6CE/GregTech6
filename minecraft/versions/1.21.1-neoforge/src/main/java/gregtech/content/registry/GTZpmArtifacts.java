package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.content.block.energy.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class GTZpmArtifacts {
    private GTZpmArtifacts() {}

    public static RegisteredBlock<ZpmModuleBlock> ZPM;
    public static RegisteredBlock<ZpmDischargerBlock> BASIC, ADVANCED, ELITE;

    public static void initialize(ContentRegistrar registrar) {
        ZPM = registrar.block(
                REGISTRY_NAMESPACE,
                "zpm",
                () -> new ZpmModuleBlock(BlockBehaviour.Properties.of().strength(.5f, 1)),
                bare -> new BlockItem(bare, new Item.Properties()));
        BASIC = discharger(registrar, "zpm_discharger_basic", false);
        ADVANCED = discharger(registrar, "zpm_discharger_advanced", true);
        // Existing references share the quantum discharger; this does not register a third block.
        ELITE = BASIC;
    }

    private static RegisteredBlock<ZpmDischargerBlock> discharger(
            ContentRegistrar registrar, String id, boolean electric) {
        return registrar.block(
                REGISTRY_NAMESPACE,
                id,
                () -> new ZpmDischargerBlock(
                        BlockBehaviour.Properties.of().strength(4, 50).requiresCorrectToolForDrops(), electric),
                bare -> new BlockItem(bare, new Item.Properties()));
    }
}
