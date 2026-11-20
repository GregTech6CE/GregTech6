package gregtech.content.material;

import gregtech.bootstrap.registration.OriginCreativeContents;
import gregtech.content.block.energy.ElectricWireBlock;
import gregtech.content.block.energy.SignalWireBlock;
import gregtech.content.block.machine.BasicMachineBlock;
import gregtech.content.block.misc.SpikeBlock;
import gregtech.content.block.tool.MaterialAnvilBlock;
import gregtech.content.block.tool.RopeBlock;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.item.CreativeTabIconItem;
import gregtech.content.item.RadiationSuitItem;
import gregtech.content.item.SelectorTagItem;
import gregtech.content.item.behavior.BehaviorDuctTape;
import gregtech.content.item.behavior.ItemBehaviors;
import gregtech.content.recipe.ClayMoldCatalog;
import gregtech.content.recipe.ClayUtilityCatalog;
import gregtech.content.registry.GTItems;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.MaterialFormItem;
import gregtech.engine.mod.GregTechIdentity;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class RecipeCompositionItems {
    private static final List<String> FAMILIES = List.of(
            "reactor_core",
            "auto_hammer_",
            "auto_igniter_",
            "flux_laser_",
            "co2_laser_",
            "fluid_measuring_pot",
            "long_dist_",
            "usb",
            "hdd",
            "compass_",
            "robot_arm_",
            "spray_paint_",
            "activity_detector_",
            "auto_reboot_switch_",
            "scannermolecular_",
            "scannervisuals_",
            "sensor_");
    private static final Set<String> ITEMS = Set.of(
            "aneutronic_fusion_power_cell_empty",
            "aneutronic_fusion_power_cell_helium_3",
            "power_cell_empty",
            "power_cell_hydrogen",
            "dust_funnel",
            "ender_garbage_bin",
            "magic_field_absorber",
            "laser_fiber_wire",
            "fluid_thermos",
            "empty_spray_can",
            "lighter",
            "lighter_empty",
            "lighter_full",
            "shiny_lighter",
            "shiny_lighter_empty",
            "shiny_lighter_full",
            "plastic_lighter_empty",
            "plastic_lighter",
            "plastic_lighter_full",
            "plastic_lighter_broken",
            "tape",
            "tape_2",
            "duct_tape",
            "duct_tape_2",
            "braintech_aerospace_advanced_reinforced_duct_tape_fal_84",
            "braintech_aerospace_advanced_reinforced_duct_tape_fal_84_2",
            "stone_generator_module",
            "basalt_generator_module",
            "blackstone_generator_module",
            "portable_scanner",
            "portable_cropnalyzer",
            "electrometer",
            "geiger_counter",
            "geiger_counter_empty",
            "quicksilver_thermometer",
            "automatic_machine_switch",
            "redstone_machine_switch",
            "auto_redstone_machine_switch",
            "tachometer");
    private static final Set<String> RAW_CLAY = Stream.of(
                    ClayMoldCatalog.RAW.stream().map(ClayMoldCatalog.Raw::id),
                    ClayUtilityCatalog.ROWS.stream().map(ClayUtilityCatalog.Row::raw),
                    Stream.of("clay_tap", "clay_funnel", "clay_jug", "clay_measuring_pot", "clay_crucible"))
            .flatMap(stream -> stream)
            .collect(Collectors.toUnmodifiableSet());
    private static final Set<String> FIRED_CLAY =
            ClayUtilityCatalog.ROWS.stream().map(ClayUtilityCatalog.Row::fired).collect(Collectors.toUnmodifiableSet());

    private RecipeCompositionItems() {}

    static boolean tracks(Item item) {
        var id = BuiltInRegistries.ITEM.getKey(item);
        if (!id.getNamespace().equals(GregTechIdentity.REGISTRY_NAMESPACE)
                || item instanceof MaterialFormItem
                || item instanceof CreativeTabIconItem) return false;
        if (item instanceof RadiationSuitItem) return true;
        if (item instanceof BlockItem blockItem) {
            var block = blockItem.getBlock();
            if (block instanceof BasicMachineBlock machine && CraftingItemCompositions.tracks(machine)) return true;
            if (isCable(item)
                    || block instanceof MaterialAnvilBlock
                    || block instanceof SpikeBlock
                    || block instanceof RopeBlock) return true;
        }
        String path = id.getPath();
        return ITEMS.contains(path)
                || RAW_CLAY.contains(path)
                || FIRED_CLAY.contains(path)
                || path.equals("mold_ceramic")
                || path.endsWith("_key")
                || path.contains("_spray")
                || path.startsWith("extruder_shape_")
                || path.startsWith("low_heat_extruder_shape_")
                || path.startsWith("press_bullet_casing_shape_")
                || FAMILIES.stream().anyMatch(path::startsWith)
                || OriginCreativeContents.family(item).equals("technology");
    }

    static boolean isRawClay(Item item) {
        var id = BuiltInRegistries.ITEM.getKey(item);
        return id.getNamespace().equals(GregTechIdentity.REGISTRY_NAMESPACE) && RAW_CLAY.contains(id.getPath());
    }

    static List<ItemStack> originalInputs(Item item) {
        // MultiItemRandomTools declares the two loot-only lighter shells as curved plates.
        // Loader_MultiTileEntities supplies the disabled 1x1 reactor and the optical cable layout.
        return switch (BuiltInRegistries.ITEM.getKey(item).getPath()) {
            case "lighter_empty" -> List.of(form(MaterialPrefix.plateCurved, "Invar", 2));
            case "shiny_lighter_empty" -> List.of(form(MaterialPrefix.plateCurved, "Platinum", 2));
            case "anvil_blackstone" -> List.of(new ItemStack(Items.BLACKSTONE, 7));
            case "loot_bottle" -> List.of(new ItemStack(Items.GLASS_BOTTLE));
            case "circuit_part_enderpearl" -> enderCircuitInputs("EnderPearl");
            case "circuit_part_endereye" -> enderCircuitInputs("EnderEye");
            case "circuit_board_power_module" ->
                List.of(
                        form(MaterialPrefix.plate, "HSLA", 1),
                        new ItemStack(item("circuit_wire_gold")),
                        new ItemStack(item("circuit_part_endereye"), 4));
            case "reactor_core" ->
                List.of(
                        new ItemStack(item("casing_machine_dense_lead")),
                        new ItemStack(item("compact_electric_piston_ev")));
            case "laser_fiber_wire" ->
                List.of(
                        form(MaterialPrefix.plate, "Silver", 2),
                        form(MaterialPrefix.gem, "Diamond", 2),
                        new ItemStack(Items.GLASS, 2),
                        new ItemStack(Items.REDSTONE, 2));
            default -> List.of();
        };
    }

    private static List<ItemStack> enderCircuitInputs(String material) {
        // MultiItemTechnological's press variants share these two ingredients.
        return List.of(form(MaterialPrefix.plateGemTiny, material, 1), form(MaterialPrefix.dustTiny, "Redstone", 1));
    }

    private static ItemStack form(MaterialPrefix prefix, String name, int count) {
        return GTItems.getStack(prefix, GTMaterialRegistry.get(name), count);
    }

    static Item compositionSource(Item item) {
        String path = BuiltInRegistries.ITEM.getKey(item).getPath();
        if (item instanceof SelectorTagItem && !path.equals("integrated_circuit_0"))
            return item("integrated_circuit_0");
        if (path.startsWith("spike_ball_falling_")) return item(path.replace("spike_ball_falling_", "spike_ball_"));
        if ((path.contains("_spray") || path.startsWith("spray_paint_")) && !path.equals("empty_spray_can"))
            return item("empty_spray_can");
        return switch (path) {
            case "tape_2", "duct_tape_2", "braintech_aerospace_advanced_reinforced_duct_tape_fal_84_2" ->
                item(path.substring(0, path.length() - 2));
            case "lighter", "shiny_lighter", "plastic_lighter", "plastic_lighter_broken" -> {
                String shell = path.equals("plastic_lighter_broken") ? "plastic_lighter" : path;
                yield item(shell + "_empty");
            }
            case "compass_face", "compass_spawn", "compass_center", "compass_death" -> item("compass_north");
            case "geiger_counter" -> item("geiger_counter_empty");
            default -> item;
        };
    }

    static ItemBehaviors.Consumable tape(ItemStack stack) {
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!id.getNamespace().equals(GregTechIdentity.REGISTRY_NAMESPACE)) return null;
        return switch (id.getPath()) {
            case "tape_2" -> BehaviorDuctTape.consumable(BehaviorDuctTape.TAPE_QUALITY);
            case "duct_tape_2" -> BehaviorDuctTape.consumable(BehaviorDuctTape.DUCT_TAPE_QUALITY);
            case "braintech_aerospace_advanced_reinforced_duct_tape_fal_84_2" ->
                BehaviorDuctTape.consumable(BehaviorDuctTape.BRAIN_TAPE_QUALITY);
            default -> null;
        };
    }

    static boolean isEmptyPowerCell(ItemStack stack) {
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return path.equals("power_cell_empty") || path.equals("aneutronic_fusion_power_cell_empty");
    }

    static Item clayBlank() {
        return item("clay_mold");
    }

    private static Item item(String path) {
        return BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath(GregTechIdentity.REGISTRY_NAMESPACE, path));
    }

    static boolean isCable(Item item) {
        return item instanceof BlockItem block
                && (block.getBlock() instanceof ElectricWireBlock wire
                                && wire.spec().insulated()
                        || block.getBlock() instanceof SignalWireBlock signal && signal.insulated());
    }
}
