package com.gregtech.gregtech.worldgen.dungeon;


import com.gregtech.gregtech.registry.*;
import com.gregtech.gregtech.block.inventory.BottleCrateBlock;
import com.gregtech.gregtech.blockentity.inventory.BottleCrateBlockEntity;
import com.gregtech.gregtech.item.BottleItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;

/** GT6 DungeonChunkRoomWorkshop:149–160 and 183–195, including conditional random-call order. */
public final class DungeonBottleSupplies {
    private static final Direction[] FACING={Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST};
    private static final String[] CHEMICALS={"mercury_bottle","mercury_bottle","mercury_bottle",
            "glue_bottle","lubricant_bottle","ink_bottle","purple_drink","holy_water","bottled_indigo_dye"};
    private DungeonBottleSupplies() {}

    public static void chemicals(GTDungeonData data,int x,int y,int z) { place(data,x,y,z,null); }
    public static void drinks(GTDungeonData data,int x,int y,int z,String fluidField) {
        String id=switch(fluidField) {
            case "Purple_Drink" -> "purple_drink";
            case "Vodka" -> "vodka";
            case "Mead" -> "mead";
            case "Whiskey_GlenMcKenner" -> "glen_mckenner";
            case "Wine_Grape_Purple" -> "ricardo_sanchez";
            default -> throw new IllegalArgumentException("Unmapped workshop drink: "+fluidField);
        };
        place(data,x,y,z,id);
    }

    private static void place(GTDungeonData data,int x,int y,int z,String drink) {
        var state=GTStorageContainers.BOTTLE_CRATE.get().defaultBlockState().setValue(BottleCrateBlock.FACING,FACING[data.next(4)]);
        data.set(x,y,z,state);
        var entity=data.level.getBlockEntity(new BlockPos(data.x+x,data.y+y,data.z+z));
        if(!(entity instanceof BottleCrateBlockEntity crate)) return;
        for(int slot=0;slot<9;slot++) {
            boolean filled=drink!=null ? data.next1in3() : slot<6 ? data.next3in4() : data.next1in2();
            ItemStack stack=filled ? bottle(drink==null ? CHEMICALS[slot] : drink)
                    : data.next1in2() ? BottleItem.emptyBottle() : ItemStack.EMPTY;
            if(!stack.isEmpty()) stack.setCount(1+data.next(drink==null?8:4));
            crate.items().setStackInSlot(slot,stack);
        }
    }

    private static ItemStack bottle(String id) {
        var item=BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech",id));
        if(item==null || item==Items.AIR) throw new IllegalStateException("Missing registered GT6 dungeon bottle: "+id);
        return new ItemStack(item);
    }
}
