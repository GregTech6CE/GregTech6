package com.gregtech.gregtech.content.multiblock;

import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.block.machine.MultiblockPortBlock;
import com.gregtech.gregtech.registry.MachineBlockRegistration;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.RegistryObject;
import java.util.*;

/** Named structural parts; original numeric IDs are confined to the layout translation boundary. */
public final class LargeMachineParts {
    private record Part(int originalId,String name,String material) {}
    private static final List<Part> DEFINITIONS=List.of(
            new Part(18031,"dense_lead_wall","Lead"),
            new Part(18044,"large_osmium_coil","Osmium"),
            new Part(18204,"conversion_processor_unit","SteelGalvanized"),
            new Part(18008,"galvanized_steel_wall","SteelGalvanized"),
            new Part(18045,"large_iridium_coil","Iridium"),
            new Part(18299,"fusion_ventilation_unit","SteelGalvanized"),
            new Part(18200,"versatile_processor_unit","SteelGalvanized"),
            new Part(18201,"logic_processor_unit","SteelGalvanized"),
            new Part(18202,"control_processor_unit","SteelGalvanized"),
            new Part(18025,"dense_adamantium_wall","Adamantium"),
            new Part(18024,"dense_tungsten_wall","Tungsten"),
            new Part(18026,"dense_titanium_wall","Titanium"),
            new Part(18003,"tungstensteel_wall","TungstenSteel"),
            new Part(18006,"titanium_wall","Titanium"),
            new Part(18007,"invar_wall","Invar"),
            new Part(18009,"steel_wall","Steel"),
            new Part(18042,"large_nichrome_coil","Nichrome"),
            new Part(18043,"large_carborundum_coil","Carborundum"),
            new Part(18106,"sluice_part","Titanium"),
            new Part(18107,"crusher_wheels","TungstenSteel"),
            new Part(17001,"wood_tank_main_valve","WoodTreated"),
            new Part(17002,"small_stainless_steel_tank_main_valve","StainlessSteel"),
            new Part(17003,"small_tungstensteel_tank_main_valve","TungstenSteel"),
            new Part(17004,"small_tungsten_tank_main_valve","Tungsten"),
            new Part(17005,"small_adamantium_tank_main_valve","Adamantium"),
            new Part(17006,"small_titanium_tank_main_valve","Titanium"),
            new Part(17007,"small_invar_tank_main_valve","Invar"),
            new Part(17022,"small_dense_stainless_steel_tank_main_valve","StainlessSteel"),
            new Part(17023,"small_dense_tungstensteel_tank_main_valve","TungstenSteel"),
            new Part(17024,"small_dense_tungsten_tank_main_valve","Tungsten"),
            new Part(17025,"small_dense_adamantium_tank_main_valve","Adamantium"),
            new Part(17026,"small_dense_titanium_tank_main_valve","Titanium"),
            new Part(17027,"small_dense_invar_tank_main_valve","Invar"),
            new Part(17042,"large_stainless_steel_tank_main_valve","StainlessSteel"),
            new Part(17043,"large_tungstensteel_tank_main_valve","TungstenSteel"),
            new Part(17044,"large_tungsten_tank_main_valve","Tungsten"),
            new Part(17045,"large_adamantium_tank_main_valve","Adamantium"),
            new Part(17046,"large_titanium_tank_main_valve","Titanium"),
            new Part(17047,"large_invar_tank_main_valve","Invar"),
            new Part(17062,"large_dense_stainless_steel_tank_main_valve","StainlessSteel"),
            new Part(17063,"large_dense_tungstensteel_tank_main_valve","TungstenSteel"),
            new Part(17064,"large_dense_tungsten_tank_main_valve","Tungsten"),
            new Part(17065,"large_dense_adamantium_tank_main_valve","Adamantium"),
            new Part(17066,"large_dense_titanium_tank_main_valve","Titanium"),
            new Part(17067,"large_dense_invar_tank_main_valve","Invar"),
            new Part(17201,"stainless_steel_boiler_main_barometer","StainlessSteel"),
            new Part(17202,"titanium_boiler_main_barometer","Titanium"),
            new Part(17203,"tungstensteel_boiler_main_barometer","TungstenSteel"),
            new Part(17204,"adamantium_boiler_main_barometer","Adamantium"),
            new Part(17205,"invar_boiler_main_barometer","Invar"),
            new Part(17211,"magnalium_steam_turbine_main_housing","StainlessSteel"),
            new Part(17212,"trinitanium_steam_turbine_main_housing","Titanium"),
            new Part(17213,"graphene_steam_turbine_main_housing","TungstenSteel"),
            new Part(17214,"vibramantium_steam_turbine_main_housing","Adamantium"),
            new Part(17221,"stainless_steel_dynamo_main_housing","StainlessSteel"),
            new Part(17222,"titanium_dynamo_main_housing","Titanium"),
            new Part(17223,"tungstensteel_dynamo_main_housing","TungstenSteel"),
            new Part(17224,"adamantium_dynamo_main_housing","Adamantium"),
            new Part(17231,"magnalium_gas_turbine_main_housing","StainlessSteel"),
            new Part(17232,"trinitanium_gas_turbine_main_housing","Titanium"),
            new Part(17233,"graphene_gas_turbine_main_housing","TungstenSteel"),
            new Part(17234,"vibramantium_gas_turbine_main_housing","Adamantium"),
            new Part(17302,"large_stainless_steel_crucible","StainlessSteel"),
            new Part(17303,"large_tungstensteel_crucible","TungstenSteel"),
            new Part(17304,"large_tungsten_crucible","Tungsten"),
            new Part(17305,"large_adamantium_crucible","Adamantium"),
            new Part(17306,"large_titanium_crucible","Titanium"),
            new Part(17307,"large_invar_crucible","Invar"),
            new Part(17309,"large_steel_crucible","Steel"),
            new Part(17312,"large_tantalum_hafnium_carbide_crucible","Ta4HfC5"),
            new Part(17996,"von_da_graagg_generator","SteelGalvanized"),
            new Part(17998,"lightning_rod_electric_output","Tungsten"),
            new Part(17999,"bedrock_mining_drill_controller","Titanium"),
            new Part(18001,"wood_wall","WoodTreated"),
            new Part(18004,"tungsten_wall","Tungsten"),
            new Part(18005,"adamantium_wall","Adamantium"),
            new Part(18010,"bronze_wall","Bronze"),
            new Part(18011,"lead_wall","Lead"),
            new Part(18012,"tantalum_hafnium_carbide_wall","Ta4HfC5"),
            new Part(18027,"dense_invar_wall","Invar"),
            new Part(18028,"dense_galvanized_steel_wall","SteelGalvanized"),
            new Part(18029,"dense_steel_wall","Steel"),
            new Part(18030,"dense_bronze_wall","Bronze"),
            new Part(18032,"dense_tantalum_hafnium_carbide_wall","Ta4HfC5"),
            new Part(18040,"large_copper_coil","AnnealedCopper"),
            new Part(18103,"bedrock_mining_drill_head","TungstenSteel"),
            new Part(18104,"lightning_rod","SteelGalvanized"),
            new Part(18203,"storage_quadcore_processor_unit","SteelGalvanized"),
            new Part(18108,"shredder_blades","TungstenSteel"));
    private static final Map<Integer,RegistryObject<Block>> BLOCKS=new LinkedHashMap<>();
    private LargeMachineParts() {}
    public static void register() {
        for(var part:DEFINITIONS) {
            TankValveSpec valve = TankValveSpec.find(part.originalId());
            if (part.originalId() == 17996) {
                BLOCKS.put(part.originalId(), MachineBlockRegistration.block(part.name(),
                        com.gregtech.gregtech.block.machine.VonDaGraaggControllerBlock::new)
                        .strength(8, 8).register());
            } else if (part.originalId() == 17998) {
                BLOCKS.put(part.originalId(), MachineBlockRegistration.block(part.name(),
                        com.gregtech.gregtech.block.machine.OriginalLightningRodControllerBlock::new)
                        .strength(10, 10).register());
            } else if (part.originalId() == 17999) {
                BLOCKS.put(part.originalId(), MachineBlockRegistration.block(part.name(),
                        com.gregtech.gregtech.block.machine.OriginalBedrockDrillControllerBlock::new)
                        .strength(9, 9).register());
            } else if (valve != null) {
                BLOCKS.put(part.originalId(), MachineBlockRegistration.block(part.name(),
                        properties -> new com.gregtech.gregtech.block.machine.TankControllerBlock(valve, properties))
                        .strength(valve.hardness(), valve.hardness()).register());
            } else if (part.originalId() >= 17201 && part.originalId() <= 17205) {
                var boiler = OriginalLargeBoilerSpecs.byOriginalId(part.originalId());
                BLOCKS.put(part.originalId(), MachineBlockRegistration.block(part.name(),
                        properties -> new com.gregtech.gregtech.block.machine.OriginalLargeBoilerControllerBlock(boiler, properties))
                        .strength(boiler.hardness(), boiler.hardness()).register());
            } else if (part.originalId() >= 17302 && part.originalId() <= 17312) {
                var crucible = LargeCrucibleSpecs.byOriginalId(part.originalId());
                BLOCKS.put(part.originalId(), MachineBlockRegistration.block(part.name(),
                        properties -> new com.gregtech.gregtech.block.machine.LargeCrucibleControllerBlock(crucible, properties))
                        .strength(crucible.hardness(), crucible.hardness()).register());
            } else if (part.originalId() >= 17211 && part.originalId() <= 17214) {
                var grade = AxialGeneratorDefinitions.STEAM.get(part.originalId() - 17211);
                BLOCKS.put(part.originalId(), MachineBlockRegistration.block(part.name(),
                        properties -> new com.gregtech.gregtech.block.machine.LargeTurbineControllerBlock(grade, properties))
                        .strength(grade.materials().hardness(), grade.materials().hardness()).register());
            } else if (part.originalId() >= 17221 && part.originalId() <= 17224) {
                var grade = AxialGeneratorDefinitions.DYNAMO.get(part.originalId() - 17221);
                BLOCKS.put(part.originalId(), MachineBlockRegistration.block(part.name(),
                        properties -> new com.gregtech.gregtech.block.machine.LargeDynamoControllerBlock(grade, properties))
                        .strength(grade.materials().hardness(), grade.materials().hardness()).register());
            } else if (part.originalId() >= 17231 && part.originalId() <= 17234) {
                var grade = GasTurbineDefinitions.GRADES.get(part.originalId() - 17231);
                BLOCKS.put(part.originalId(), MachineBlockRegistration.block(part.name(),
                        properties -> new com.gregtech.gregtech.block.machine.LargeGasTurbineControllerBlock(grade, properties))
                        .strength(grade.hardness(), grade.hardness()).register());
            } else {
                BLOCKS.put(part.originalId(),MachineBlockRegistration.block(part.name(),MultiblockPortBlock::new)
                        .strength(part.originalId()==18025?100:part.originalId()==18023?12.5f:8,
                                  part.originalId()==18025?100:part.originalId()==18023?12.5f:8).register());
            }
        }
    }
    public static Block block(int originalId) { return Objects.requireNonNull(find(originalId),"Unknown original part "+originalId).get(); }

    /**
     * Non-throwing lookup of the block that stands in for a GT6 multiblock part id.
     * <p>
     * Parts owned by {@code GTMultiblocks} live outside {@link #DEFINITIONS}; the ids below are
     * the ones GT6 uses in its "Multiblock Machines" crafting recipes. GT6's {@code 18000}
     * ("Fire Bricks") has no dedicated block in this port, so the Coke Oven wall stands in.
     * </p>
     *
     * @return the registered block, or {@code null} when this port has no counterpart yet
     */
    public static RegistryObject<Block> find(int originalId) {
        RegistryObject<Block> hosted = switch (originalId) {
            case 18000 -> com.gregtech.gregtech.registry.GTMultiblocks.COKE_OVEN_WALL;           // GT6 "Fire Bricks"
            case 18002 -> com.gregtech.gregtech.registry.GTMultiblocks.TANK_WALL;                // GT6 "Stainless Steel Wall"
            case 18022 -> com.gregtech.gregtech.registry.GTMultiblocks.TANK_WALL_DENSE;          // GT6 "Dense Stainless Steel Wall"
            case 18023 -> com.gregtech.gregtech.registry.GTMultiblocks.IMPLOSION_COMPRESSOR_WALL;
            case 18041 -> com.gregtech.gregtech.registry.GTMultiblocks.LARGE_NIOBIUM_TITANIUM_COIL; // §113
            case 18100 -> com.gregtech.gregtech.registry.GTMultiblocks.CENTRIFUGE_PART;
            case 18101 -> com.gregtech.gregtech.registry.GTMultiblocks.HEAT_TRANSMITTER;
            case 18102 -> com.gregtech.gregtech.registry.GTMultiblocks.DISTILLATION_TOWER_PART;
            case 18105 -> com.gregtech.gregtech.registry.GTMultiblocks.ELECTROLYZER_PART;
            default -> null;
        };
        return hosted != null ? hosted : BLOCKS.get(originalId);
    }
    public static List<RegistryObject<Block>> blocks() {return List.copyOf(BLOCKS.values());}
    public static Integer tint(Block block) {
        for(var part:DEFINITIONS) {
            var registered=BLOCKS.get(part.originalId());
            if(registered!=null&&registered.isPresent()&&registered.get()==block) return GTMaterialRegistry.get(part.material()).getColor();
        }
        return null;
    }
}
