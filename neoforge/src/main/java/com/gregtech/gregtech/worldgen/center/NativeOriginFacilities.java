package com.gregtech.gregtech.worldgen.center;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import com.gregtech.gregtech.blockentity.inventory.DrawerQuadBlockEntity;
import com.gregtech.gregtech.blockentity.energy.ZpmModuleBlockEntity;

/** Modern native identities for the original Nexus test facilities. */
public final class NativeOriginFacilities {
    private NativeOriginFacilities() {}
    public static String block(int legacyId) { return switch(legacyId) {
        case 7133 -> "bookshelf_metal_adamantium"; case 4033 -> "drawer_quad_adamantium";
        case 6033 -> "mass_storage_adamantium"; case 5033 -> "advanced_crafting_table_adamantium";
        case 8033 -> "hopper_adamantium"; case 14999 -> "zpm"; case 26304 -> "pipe_huge_adamantium";
        case 32048 -> "anvil_adamantium"; case 32057 -> "fluid_barometer_gas_cylinder_tungsten";
        case 32062 -> "cap_nozzle_adamantium"; case 32702 -> "sifting_table"; case 32703 -> "grindstone_block";
        case 32705 -> "mixing_bowl_table"; case 32707 -> "bathing_pot_table"; case 32709 -> "ender_garbage_bin";
        case 32711 -> "advanced_button"; case 32719 -> "drum_adamantium"; case 32722 -> "juicer";
        case 32727 -> "fluid_funnel_adamantium"; case 32732 -> "tap_adamantium"; case 32735 -> "mortar_block";
        case 32737 -> "fluid_thermos"; case 32739 -> "fluid_cup"; case 32744 -> "fluid_measuring_pot_tungsten";
        case 32750 -> "nozzle_adamantium"; default -> throw new IllegalArgumentException("Unknown source origin tile "+legacyId);
    }; }
    public static void place(NativeOriginWorld world, WorldGenLevel level, int x,int y,int z,int legacy,String data) {
        if(!world.canWrite(x,y,z)) return;

        var key=ResourceLocation.fromNamespaceAndPath("gregtech",block(legacy));
        if(!BuiltInRegistries.BLOCK.containsKey(key)) throw new IllegalStateException("Missing origin facility "+key);
        var state=BuiltInRegistries.BLOCK.get(key).defaultBlockState();
        var side=java.util.regex.Pattern.compile("NBT_FACING, SIDE_([XYZ])_(NEG|POS)").matcher(data);
        if(side.find()) {
            Direction direction=switch(side.group(1)) {case "X"->side.group(2).equals("POS")?Direction.EAST:Direction.WEST;
                case "Y"->side.group(2).equals("POS")?Direction.UP:Direction.DOWN;
                default->side.group(2).equals("POS")?Direction.SOUTH:Direction.NORTH;};
            for(var property:state.getProperties()) if(property instanceof DirectionProperty facing && property.getName().equals("facing") && facing.getPossibleValues().contains(direction))
                state=state.setValue(facing,direction);
        }
        if(legacy==26304) {
            int mask=0;
            String[] sourceBits={"SBIT_D","SBIT_U","SBIT_N","SBIT_S","SBIT_W","SBIT_E"};
            for(int sideIndex=0;sideIndex<6;sideIndex++)if(data.contains(sourceBits[sideIndex]))mask|=1<<sideIndex;
            for(var property:state.getProperties()) if(property instanceof BooleanProperty connected) {
                for(var direction:Direction.values()) if(property.getName().equals(direction.getName()))
                    state=state.setValue(connected,(mask&(1<<direction.get3DDataValue()))!=0);
            }
        }
        if(legacy==32703 && state.hasProperty(com.gregtech.gregtech.block.tool.ManualToolBlock.STONE))
            state=state.setValue(com.gregtech.gregtech.block.tool.ManualToolBlock.STONE,true);
        var pos=new BlockPos(x,y,z);
        level.setBlock(pos,state,2);
        var entity=level.getBlockEntity(pos);
        if(entity instanceof ZpmModuleBlockEntity zpm) zpm.initializeDungeonEnergy(data.contains("NBT_ACTIVE_ENERGY, T"));
        if(entity instanceof DrawerQuadBlockEntity drawer && data.contains("NBT_INV_LIST")) {
            for(int slot=0;slot<Math.min(drawer.items().getSlots(),OriginTestInventory.ROWS.size());slot++)
                drawer.items().setStackInSlot(slot,NativeOriginItems.stack(OriginTestInventory.ROWS.get(slot)));
        }
        if(entity instanceof com.gregtech.gregtech.blockentity.tool.ManualToolBlockEntity station && legacy==32703)
            station.setDungeonGrindstoneUses(4);
        if(entity instanceof com.gregtech.gregtech.blockentity.tool.AdvancedButtonBlockEntity button && data.contains("NBT_MODE, T")) {
            var config=button.saveItemConfig();config.putBoolean("Inverted",true);button.loadItemConfig(config);
        }
    }
}
