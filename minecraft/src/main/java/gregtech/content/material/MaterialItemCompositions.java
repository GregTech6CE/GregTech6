package gregtech.content.material;

import gregtech.api.material.GTValues;
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
import gregtech.engine.fluid.PortableFluidContainerSpec;
import gregtech.engine.material.ItemComposition;
import gregtech.engine.material.ItemCompositionSource;
import gregtech.engine.material.ItemMaterialRegistry;
import gregtech.engine.material.ItemMaterialRuntimeFacts;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

/** Pure item and block composition submissions shared by both loader entry points. */
public final class MaterialItemCompositions {
    private MaterialItemCompositions() {}

    public static void registerCommon() {
        ItemMaterialRuntimeFacts.install(ItemMaterialContentFacts.INSTANCE);
        registerMaterialItemForms();
        registerBlockWeights();
        registerMachineConstruction();
        registerSingleItemMaterials();
        registerCapsuleCells();
        registerDiggableMaterials();
        WoodMaterialCompositions.register();
        gregtech.content.registry.GTBars.registerCompositions();
        gregtech.content.registry.GTWires.registerCompositions();
        gregtech.content.registry.GTSignalWires.registerCompositions();
        PanelMaterialCompositions.register();
        TransportMaterialCompositions.register();
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

    private static void registerDiggableMaterials() {
        for (var row : gregtech.content.recipe.DiggableRecipes.packable()) {
            if (row.material() == null) continue;
            ItemMaterialRegistry.register(item("gt6ce:" + row.block()), null, row.material(), GTValues.U * 4);
            ItemMaterialRegistry.register(item("gt6ce:" + row.ball()), null, row.material(), GTValues.U);
        }
        ItemMaterialRegistry.register(
                item("gt6ce:turf"), null, gregtech.content.material.Materials.Peat, GTValues.U * 4);
    }

    private static void registerSingleItemMaterials() {
        ItemMaterialRegistry.register(
                item("gt6ce:dry_bark"), null, gregtech.engine.material.GTMaterialRegistry.get("Bark"), GTValues.U / 2);
        for (var row : gregtech.content.recipe.OriginalSingleItemCatalog.COMPOUNDS) {
            ItemMaterialRegistry.register(
                    item("gt6ce:" + row.item()),
                    null,
                    gregtech.engine.material.GTMaterialRegistry.get(row.material()),
                    GTValues.U / 9);
        }
        ItemMaterialRegistry.register(
                item("gt6ce:scrap_meat"),
                null,
                gregtech.engine.material.GTMaterialRegistry.get("MeatRaw"),
                2 * GTValues.U);
        ItemMaterialRegistry.register(
                item("gt6ce:raw_meat_bar"),
                MaterialPrefix.ingot,
                gregtech.engine.material.GTMaterialRegistry.get("MeatRaw"),
                GTValues.U);
        ItemMaterialRegistry.register(
                item("gt6ce:cooked_meat_bar"),
                MaterialPrefix.ingot,
                gregtech.engine.material.GTMaterialRegistry.get("MeatCooked"),
                GTValues.U);
    }

    private static void registerCapsuleCells() {
        for (var spec : PortableFluidContainerSpec.values()) {
            if (!spec.shapeId().equals("cell")) continue;
            var item = item("gt6ce:fluid_" + spec.id());
            if (ItemMaterialRegistry.base(item).isPresent()) continue;
            ItemMaterialRegistry.registerDeclared(
                    item,
                    new ItemComposition(
                            null,
                            List.of(MaterialComponent.of(spec.material(), GTValues.U9)),
                            spec.material(),
                            List.of(new ItemCompositionSource(
                                    ItemCompositionSource.Kind.EXPLICIT,
                                    "gt6ce:capsule-cell/" + spec.id(),
                                    "capsule-cell material",
                                    spec.id(),
                                    "")),
                            true));
        }
    }

    private static Item item(String id) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        if (item == Items.AIR) throw new IllegalStateException("Missing material item " + id);
        return item;
    }
}
