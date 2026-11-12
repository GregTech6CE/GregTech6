package gregtech.content.material;

import gregtech.api.material.MaterialComponent;
import gregtech.content.block.BlockMaterialWeights;
import gregtech.content.block.MaterialBlockLike;
import gregtech.content.block.machine.BasicMachineBlock;
import gregtech.content.block.stone.GTStoneBlock;
import gregtech.content.block.stone.GTStoneSlabBlock;
import gregtech.content.block.stone.StoneMaterialWeights;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.machine.MachineConstructionMaterials;
import gregtech.content.machine.OriginalMachineMaterialData;
import gregtech.content.registry.GTBlocks;
import gregtech.content.registry.GTItems;
import gregtech.engine.material.ItemComposition;
import gregtech.engine.material.ItemCompositionSource;
import gregtech.engine.material.ItemMaterialRegistry;
import gregtech.engine.material.ItemMaterialRuntimeFacts;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

/** Pure item and block composition submissions shared by both loader entry points. */
public final class MaterialItemCompositions {
    private MaterialItemCompositions() {}

    public static void registerCommon() {
        ItemMaterialRuntimeFacts.install(ItemMaterialContentFacts.INSTANCE);
        registerMaterialItemForms();
        registerBlockWeights();
        registerMachineConstruction();
        gregtech.content.recipe.DiggableRecipes.registerMaterials();
        gregtech.content.recipe.RegisteredWoodSurvivalRecipes.registerMaterials();
        gregtech.content.recipe.VanillaWoodProcessingRecipes.registerMaterials();
        gregtech.content.registry.GTBars.registerCompositions();
        gregtech.content.registry.GTWires.registerCompositions();
        gregtech.content.registry.GTSignalWires.registerCompositions();
        gregtech.content.recipe.PanelMaterialRegistration.register();
        gregtech.content.recipe.TransportMaterialRegistration.register();
    }

    public static void registerVanillaBindings() {
        VanillaUnificationLoader.register();
        ModMaterialUnificationLoader.register();
        VanillaCompositionLoader.register();
        VanillaMaterialCorrections.register();
    }

    private static void registerMaterialItemForms() {
        for (var holder : GTItems.allEntries()) {
            if (holder.isPresent() && holder.get() instanceof gregtech.engine.material.MaterialFormItem item)
                ItemMaterialRegistry.register(holder.get(), item.getPrefix(), item.getMaterial());
        }
        // GT6 treats the placeable treated plank as one plate of WoodTreated.
        var treated = GTItems.getStack(
                        MaterialPrefix.plate, gregtech.content.material.generated.WoodMaterials.WoodTreated)
                .getItem();
        ItemMaterialRegistry.register(
                treated, MaterialPrefix.plate, gregtech.content.material.generated.WoodMaterials.WoodTreated);
    }

    private static void registerBlockWeights() {
        for (var holder : GTBlocks.allEntries()) {
            if (!holder.isPresent()) continue;
            Block block = holder.get();
            if (block instanceof gregtech.content.block.OreBlock
                    || block instanceof gregtech.content.block.DenseOreBlock
                    || block instanceof gregtech.content.block.VanillaOreBlock
                    || block instanceof gregtech.content.block.BlackSandBlock) continue;
            List<MaterialComponent> weights;
            if (block instanceof MaterialBlockLike material) {
                weights = BlockMaterialWeights.contained(material.material(), material.prefix());
            } else if (block instanceof GTStoneBlock stone) {
                weights = StoneMaterialWeights.contained(stone.stoneMaterial(), stone.variant(), false);
            } else if (block instanceof GTStoneSlabBlock slab) {
                weights = StoneMaterialWeights.contained(slab.stoneMaterial(), slab.variant(), true);
            } else {
                continue;
            }
            var components = weights.stream()
                    .filter(weight -> weight.material().isValid() && weight.amount() > 0)
                    .toList();
            if (!components.isEmpty()) {
                String itemId = BuiltInRegistries.ITEM.getKey(block.asItem()).toString();
                ItemMaterialRegistry.register(
                        block.asItem(),
                        new ItemComposition(
                                null,
                                components,
                                components.get(0).material(),
                                List.of(new ItemCompositionSource(
                                        ItemCompositionSource.Kind.BLOCK_WEIGHT,
                                        "gt6ce:block-weight/" + itemId,
                                        "GT6 block material weights",
                                        itemId,
                                        "")),
                                true));
            }
        }
    }

    private static void registerMachineConstruction() {
        for (Block block : BuiltInRegistries.BLOCK) {
            var id = BuiltInRegistries.BLOCK.getKey(block);
            if (!id.getNamespace().equals(gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE)) continue;
            if (block instanceof BasicMachineBlock machine) {
                OriginalMachineMaterialData.find(
                                machine.basicSpec().machineName(),
                                machine.basicSpec().tier())
                        .ifPresent(data -> ItemMaterialRegistry.registerDeclared(block.asItem(), data));
            }
            MachineConstructionMaterials.block(id.getPath())
                    .ifPresent(data -> ItemMaterialRegistry.registerDeclared(block.asItem(), data));
        }
    }
}
