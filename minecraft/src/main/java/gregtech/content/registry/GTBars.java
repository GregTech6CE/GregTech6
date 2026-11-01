package gregtech.content.registry;

import gregtech.api.material.GTValues;
import gregtech.api.material.MaterialComponent;
import gregtech.content.block.BarsRules;
import gregtech.content.block.misc.*;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.ItemComposition;
import gregtech.engine.material.ItemMaterialRegistry;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class GTBars {
    private GTBars() {}

    private static final java.util.Map<String, RegisteredBlock<BarsBlock>> ALL = new java.util.LinkedHashMap<>();

    public static java.util.Collection<RegisteredBlock<BarsBlock>> all() {
        return java.util.List.copyOf(ALL.values());
    }

    public static BarsBlock get(String id) {
        return ALL.get(id).get();
    }

    public static void initialize() {
        for (var spec : BarsRules.SPECS) {
            var b = RegisteredBlock.registerBlock(
                    spec.id(),
                    () -> new BarsBlock(
                            spec.material(),
                            spec.tint(),
                            spec.harvest(),
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5, spec.resistance())
                                    .requiresCorrectToolForDrops()
                                    .sound(SoundType.METAL)
                                    .noOcclusion()));
            ALL.put(spec.id(), b);
            GTBlocks.BLOCK_ITEMS.register(spec.id(), () -> new BarsBlockItem(b.get(), new Item.Properties()));
        }
    }

    /** Registers each bar item's material composition; call after the vanilla composition table. */
    public static void registerCompositions() {
        for (var holder : all()) {
            var block = holder.get();
            String materialName = switch (block.materialName()) {
                case "iron" -> "Iron";
                case "steel" -> "Steel";
                case "brass" -> "Brass";
                case "bronze" -> "Bronze";
                case "wrought_iron" -> "WroughtIron";
                case "stainless" -> "StainlessSteel";
                case "tungsten_steel" -> "Tungstensteel";
                default -> throw new IllegalStateException("Unknown railing material: " + block.materialName());
            };
            var material = GTMaterialRegistry.get(materialName);
            if (!material.isValid()) throw new IllegalStateException("Missing railing material: " + materialName);
            ItemMaterialRegistry.register(
                    block.asItem(),
                    new ItemComposition(
                            null,
                            java.util.List.of(MaterialComponent.of(material, GTValues.U)),
                            "GT6CE metal railing",
                            true));
        }
    }
}
