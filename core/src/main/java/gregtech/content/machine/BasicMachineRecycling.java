/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Adapted from CR.shaped, OreDictItemData and MultiItemTechnological. */
package gregtech.content.machine;

import static gregtech.api.material.GTValues.U;
import static gregtech.content.data.ImportedMaterialData.*;
import static gregtech.content.data.MaterialPrefix.*;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialComponent;
import gregtech.content.data.BasicMachineCraftingRecipes.*;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.recipe.ElectricalCraftingRules;
import gregtech.content.transport.TransportMaterialRules;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.ItemComposition;
import gregtech.engine.material.ReversibleCraftingData;
import java.util.ArrayList;
import java.util.List;

/** Follows CR.REV: expand known material data for each occupied recipe slot. */
final class BasicMachineRecycling {
    private BasicMachineRecycling() {}

    static ItemComposition derive(Recipe recipe, GTMaterial casing, int sourceId) {
        var inputs = new ArrayList<MaterialComponent>();
        for (String row : recipe.rows()) {
            for (int slot = 0; slot < row.length(); slot++) {
                char key = row.charAt(slot);
                if (key != ' ') inputs.addAll(parts(recipe.keys().get(key), casing));
            }
        }
        return ReversibleCraftingData.perItem(
                inputs, 1, "GT6 Loader_MultiTileEntities " + sourceId + " known CR.REV inputs");
    }

    private static List<MaterialComponent> parts(Ingredient ingredient, GTMaterial casing) {
        if (ingredient instanceof Form form)
            return List.of(part(form.material() == null ? casing : form.material(), form.prefix()));
        if (ingredient instanceof Casing shell)
            return List.of(MaterialComponent.of(casing, shell.prefix().getMaterialWeight()));
        if (ingredient instanceof Pipe pipe)
            return List.of(part(
                    pipe.material() == null ? casing : pipe.material(), TransportMaterialRules.prefix(pipe.size())));
        if (ingredient instanceof Wire wire) return List.of(wire(wire.material(), wire.size()));
        if (ingredient instanceof TierWire wire) {
            var parts = new ArrayList<MaterialComponent>();
            addWireTier(parts, wire.tier(), wire.cable() ? 1 : 4, 1, wire.cable());
            return parts;
        }
        if (ingredient instanceof Crucible crucible)
            return originalBlock(crucible.spec().id());
        if (ingredient instanceof ItemRef item) return itemParts(item);
        if (ingredient instanceof TagRef tag) return tagParts(tag);
        if (ingredient instanceof Component component) return componentParts(component.type(), component.tier());
        if (ingredient instanceof Alternatives alternatives) {
            var first = parts(alternatives.options().get(0), casing);
            for (var option : alternatives.options()) if (!first.equals(parts(option, casing))) return List.of();
            return first;
        }
        // Tools are reused, circuit grades have no automatic composition, and the honey vessel is a remainder.
        if (ingredient instanceof Tool || ingredient instanceof Circuit || ingredient instanceof FluidContainer)
            return List.of();
        throw new IllegalArgumentException("Unknown basic machine crafting ingredient " + ingredient);
    }

    private static List<MaterialComponent> originalBlock(String id) {
        return OriginalMachineMaterialData.block(id)
                .orElseThrow(() -> new IllegalStateException("Missing composition for " + id))
                .components();
    }

    private static List<MaterialComponent> itemParts(ItemRef item) {
        return switch (item) {
            case BRICKS -> List.of(MaterialComponent.of(Brick, 4 * U));
            case HONEY_BOTTLE -> List.of();
            case CRYSTAL_PROCESSOR_DIAMOND -> processor(Diamond);
            case CRYSTAL_PROCESSOR_EMERALD -> processor(Emerald);
            case CRYSTAL_PROCESSOR_RUBY -> processor(Ruby);
            case CRYSTAL_PROCESSOR_SAPPHIRE -> processor(Sapphire);
            // All filled lasers inherit the Empty Gas Laser Emitter's composition.
            case LASER_EMITTER_ARGON, LASER_EMITTER_KRYPTON, LASER_EMITTER_XENON ->
                List.of(
                        MaterialComponent.of(Cu, U),
                        MaterialComponent.of(Rubber, 2 * U),
                        MaterialComponent.of(Ag, 2 * U),
                        MaterialComponent.of(Glass, 9 * U),
                        part(StainlessSteel, screw));
        };
    }

    private static List<MaterialComponent> tagParts(TagRef tag) {
        return switch (tag) {
            case GLASS_COLORLESS -> List.of(MaterialComponent.of(Glass, 9 * U));
            case HARDENED_CLAY -> List.of(MaterialComponent.of(Ceramic, U));
            case IRON_MACHINE_CASING ->
                List.of(MaterialComponent.of(Fe, Casing.STANDARD.prefix().getMaterialWeight()));
            case IRON_PIPE_MEDIUM -> List.of(part(Fe, pipeMedium));
            case IRON_WIRE_01 -> List.of(wire(Fe, 1));
            case IRON_WIRE_02 -> List.of(wire(Fe, 2));
            case IRON_WIRE_04 -> List.of(wire(Fe, 4));
            case IRON_WIRE_08 -> List.of(wire(Fe, 8));
            case IRON_WIRE_16 -> List.of(wire(Fe, 16));
            case SANDSTONE, SAPLINGS, LENS -> List.of();
        };
    }

    private static List<MaterialComponent> processor(GTMaterial gem) {
        return List.of(MaterialComponent.of(Pt, U), part(gem, MaterialPrefix.gem));
    }

    private static MaterialComponent part(GTMaterial material, MaterialPrefix prefix) {
        return MaterialComponent.of(material, prefix.getMaterialWeight());
    }

    private static MaterialComponent wire(GTMaterial material, int size) {
        return MaterialComponent.of(material, size * U / 2);
    }

    private static void addWireTier(List<MaterialComponent> parts, int tier, int size, int count, boolean cable) {
        var material = GTMaterialRegistry.requireInstalled(ElectricalCraftingRules.wireMaterial(tier));
        parts.add(wire(material, size * count));
        if (cable && ElectricalCraftingRules.insulatedCable(tier)) parts.add(MaterialComponent.of(Rubber, count * U));
    }

    private static void addForms(
            List<MaterialComponent> parts, GTMaterial material, int count, MaterialPrefix... forms) {
        for (var form : forms) parts.add(MaterialComponent.of(material, count * form.getMaterialWeight()));
    }

    /** Known inputs to the component recipes in MultiItemTechnological. */
    private static List<MaterialComponent> componentParts(ComponentType type, int tier) {
        var material = BasicMachineCatalog.electricTierMaterial(tier);
        var parts = new ArrayList<MaterialComponent>();
        switch (type) {
            case MOTORS -> addMotor(parts, material, tier);
            case PUMPS -> {
                addMotor(parts, material, tier);
                addForms(parts, material, 1, rotor, plateCurved);
                addForms(parts, material, 2, screw);
                addForms(parts, Rubber, 2, ring);
            }
            case PISTONS -> addPiston(parts, material, tier);
            case CONVEYERS -> {
                addMotor(parts, material, tier);
                addMotor(parts, material, tier);
                addWireTier(parts, tier, 1, 1, true);
                addForms(parts, Rubber, 6, plate);
            }
            case ROBOT_ARMS -> {
                addWireTier(parts, tier, 1, 3, true);
                addMotor(parts, material, tier);
                addMotor(parts, material, tier);
                addPiston(parts, material, tier);
                addForms(parts, material, 2, stick);
            }
            case EMITTERS, SENSORS -> {
                if (type == ComponentType.EMITTERS) {
                    addWireTier(parts, tier, 4, 2, false);
                    addWireTier(parts, tier, 1, 2, true);
                    addForms(parts, material, 2, plateCurved);
                } else {
                    addWireTier(parts, tier, 1, 1, false);
                    addForms(parts, material, 4, plateCurved);
                }
                parts.add(part(
                        switch (tier) {
                            case 1, 2 -> SiO2;
                            case 3 -> Emerald;
                            case 4 -> EnderPearl;
                            default -> EnderEye;
                        },
                        gem));
            }
            case FIELD_GENERATORS -> {
                int wireSize = switch (tier) {
                    case 1 -> 1;
                    case 2 -> 2;
                    case 3 -> 4;
                    case 4 -> 6;
                    default -> 8;
                };
                parts.add(wire(Os, 4 * wireSize));
                addForms(parts, material, 2, plateDouble);
                parts.add(part(
                        switch (tier) {
                            case 1 -> EnderPearl;
                            case 2, 3 -> EnderEye;
                            default -> NetherStar;
                        },
                        gem));
            }
        }
        return parts;
    }

    private static void addMotor(List<MaterialComponent> parts, GTMaterial material, int tier) {
        addWireTier(parts, tier, 1, 2, true);
        addForms(parts, material, 1, plateCurved, stick);
        parts.add(wire(tier <= 3 ? Cu : AnnealedCopper, 4 * tier));
        addForms(
                parts,
                switch (tier) {
                    case 1 -> IronMagnetic;
                    case 2, 3 -> SteelMagnetic;
                    default -> NeodymiumMagnetic;
                },
                1,
                stick);
    }

    private static void addPiston(List<MaterialComponent> parts, GTMaterial material, int tier) {
        addMotor(parts, material, tier);
        addForms(parts, material, 2, plate, stick, screw);
        addForms(parts, material, 1, gearGtSmall);
    }
}
