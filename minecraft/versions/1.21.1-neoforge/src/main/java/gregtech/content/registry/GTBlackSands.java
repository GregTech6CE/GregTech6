package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.content.block.*;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class GTBlackSands {
    private GTBlackSands() {}

    public static void registerCompositions() {
        for (String id : BlackSandDefinitions.IDS) {
            var block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(REGISTRY_NAMESPACE, id));
            if (block instanceof BlackSandBlock sand)
                gregtech.engine.material.ItemMaterialRegistry.register(
                        sand.asItem(), null, sand.material(), gregtech.api.material.GTValues.U * 9);
        }
    }

    public static void initialize(ContentRegistrar registrar) {
        for (String id : BlackSandDefinitions.IDS) {
            var block = registrar.block(
                    REGISTRY_NAMESPACE,
                    id,
                    () -> new BlackSandBlock(
                            BlockBehaviour.Properties.of()
                                    .strength(.5f, .5f)
                                    .sound(SoundType.SAND),
                            BlackSandBlock.spec(id)),
                    GTBlocks::materialBlockItem);
            GTBlocks.bind(
                    gregtech.engine.prefix.BlockMaterialPrefix.blockDust,
                    gregtech.engine.material.GTMaterialRegistry.get(
                            BlackSandDefinitions.SPECS.get(id).material()),
                    block);
        }
    }
}
