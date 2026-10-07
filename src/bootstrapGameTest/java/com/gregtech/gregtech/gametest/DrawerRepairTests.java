package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.block.inventory.DrawerQuadBlock;
import com.gregtech.gregtech.blockentity.inventory.DrawerQuadBlockEntity;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.*;
import com.gregtech.gregtech.worldgen.dungeon.*;
import com.gregtech.gregtech.worldgen.GTDungeonFeature;
import com.gregtech.gregtech.block.stone.StoneType;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class DrawerRepairTests {
    private static DrawerQuadBlockEntity place(GameTestHelper h) {
        h.setBlock(new BlockPos(1,1,1),GTStorage.DRAWER_QUAD.get());
        return (DrawerQuadBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(1,1,1)));
    }
    @GameTest(template="test_empty")
    public static void fourMenusShare144NormalSlotsAndClickCoordinatesMatchGT6(GameTestHelper h) {
        var drawer=place(h); var player=h.makeMockSurvivalPlayer(); var pos=drawer.getBlockPos();
        player.setPos(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5);
        for(int page=0;page<4;page++) {
            var menu=drawer.createMenu(page,page+1,player.getInventory());
            h.assertTrue(menu.slotCount()==36 && menu.slots.size()==72 && menu.stillValid(player),"four ordinary 36-slot menus");
            menu.getSlot(0).set(new ItemStack(Items.DIAMOND,page+1));
            menu.getSlot(35).set(new ItemStack(Items.IRON_INGOT,page+2));
            h.assertTrue(drawer.items().getStackInSlot(page*36).getCount()==page+1 && drawer.items().getStackInSlot(page*36+35).getCount()==page+2,"page writes map to its own offset");
        }
        h.assertTrue(drawer.items().insertItem(10,new ItemStack(Items.STONE,80),false).getCount()==16,"normal stack limit, not bulk storage");
        for(Direction facing:Direction.values()) {
            var state=drawer.getBlockState().setValue(DrawerQuadBlock.FACING,facing); h.getLevel().setBlock(pos,state,3);
            for(int page=0;page<4;page++) {
                double u=(page%2==0?.25:.75),v=(page<2?.25:.75);
                Vec3 local=switch(facing) { case NORTH -> new Vec3(1-u,1-v,0); case SOUTH -> new Vec3(u,1-v,1);
                    case WEST -> new Vec3(0,1-v,u); case EAST -> new Vec3(1,1-v,1-u); case UP -> new Vec3(u,1,v); case DOWN -> new Vec3(u,0,1-v); };
                var hit=new BlockHitResult(local.add(Vec3.atLowerCornerOf(pos)),facing,pos,false);
                h.assertTrue(DrawerQuadBlock.quadrant(facing,pos,hit.getLocation())==page,"six-facing quadrant "+facing+" / "+page);
                player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.APPLE,7));
                h.assertTrue(state.use(h.getLevel(),player,InteractionHand.MAIN_HAND,hit).consumesAction() && player.getMainHandItem().getCount()==7,"front opens without consuming held stack");
            }
            h.assertTrue(DrawerQuadBlock.quadrant(facing,pos,Vec3.atCenterOf(pos))==0,"center-line belongs to first quadrant");
            var hit=new BlockHitResult(Vec3.atCenterOf(pos),facing.getOpposite(),pos,false);
            h.assertTrue(state.use(h.getLevel(),player,InteractionHand.MAIN_HAND,hit)==InteractionResult.PASS,"back cannot open GUI");
        }
        player.setPos(pos.getX()+20,pos.getY(),pos.getZ());
        h.assertTrue(!drawer.createMenu(0,1,player.getInventory()).stillValid(player),"GUI closes at distance");
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void monkeyWrenchAndRotatingSidedAutomationInvalidateCapabilities(GameTestHelper h) {
        var drawer=place(h); var pos=drawer.getBlockPos(); var player=h.makeMockSurvivalPlayer();
        var tool=com.gregtech.gregtech.item.GTToolItem.create(com.gregtech.gregtech.api.tool.GTToolType.MONKEY_WRENCH,Materials.Steel,com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Wood"));
        player.setItemInHand(InteractionHand.MAIN_HAND,tool);
        var old=drawer.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.UP);
        h.assertTrue(old.orElseThrow(IllegalStateException::new).getSlots()==144,"default any-side access");
        drawer.getBlockState().use(h.getLevel(),player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
        h.assertTrue(drawer.sidedAccess() && !old.isPresent() && tool.getDamageValue()>0,"monkey wrench toggles, costs durability and invalidates old caps");
        int[][] rotations={{0,1,2,3,4,5},{0,1,2,3,4,5},{0,1,3,5,4,2},{0,1,5,3,2,4},{0,1,2,4,3,5},{0,1,4,2,5,3}};
        for(int i=0;i<144;i++) drawer.items().setStackInSlot(i,new ItemStack(Items.STONE,i%64+1));
        for(Direction facing:Direction.values()) {
            h.getLevel().setBlock(pos,drawer.getBlockState().setValue(DrawerQuadBlock.FACING,facing),3);
            for(Direction side:Direction.values()) {
                int group=rotations[facing.ordinal()][side.ordinal()]; var handler=drawer.getCapability(ForgeCapabilities.ITEM_HANDLER,side).orElseThrow(IllegalStateException::new);
                int index=0;
                for(int i=0;i<144;i++) if(group==0?i>=72:group==1?i<72:group==2?i/36%2==0:group==4?i/36%2==1:true) {
                    h.assertTrue(handler.getStackInSlot(index++)==drawer.items().getStackInSlot(i),"original side-to-slot order: "+facing+" / "+side);
                }
                h.assertTrue(handler.getSlots()==index,"visible slot count");
                int before=handler.getStackInSlot(0).getCount(); handler.extractItem(0,1,true);
                h.assertTrue(handler.getStackInSlot(0).getCount()==before,"simulation is read-only");
            }
        }
        var current=drawer.getCapability(ForgeCapabilities.ITEM_HANDLER,null); drawer.invalidateCaps();
        h.assertTrue(!current.isPresent() && !drawer.getCapability(ForgeCapabilities.ITEM_HANDLER,null).isPresent(),"invalidated entity cannot resurrect capability");
        drawer.reviveCaps(); h.assertTrue(drawer.getCapability(ForgeCapabilities.ITEM_HANDLER,null).isPresent(),"revival restores capability");
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void legacyBulkOverflowSurvivesSaveThenDropsExactlyOnce(GameTestHelper h) {
        var drawer=place(h); ListTag old=new ListTag();
        for(int i=0;i<4;i++) { CompoundTag row=new CompoundTag();row.put("item",new ItemStack(Items.DIAMOND).save(new CompoundTag()));row.putLong("count",4096);old.add(row); }
        CompoundTag tag=new CompoundTag();tag.put("gt.compartments",old);drawer.load(tag);
        h.assertTrue(drawer.items().getSlots()==144 && drawer.items().getStackInSlot(143).getCount()==64,"legacy contents fill fixed ordinary slots");
        var saved=drawer.saveWithoutMetadata();drawer.load(new CompoundTag());
        h.assertTrue(drawer.items().getSlots()==144 && drawer.items().getStackInSlot(0).isEmpty(),"empty load clears fixed inventory");drawer.load(saved);
        h.assertTrue(drawer.items().extractItem(0,64,false).getCount()==64 && drawer.items().getStackInSlot(0).getCount()==64,"retained overflow refills freed room");
        var pos=drawer.getBlockPos(); h.getLevel().destroyBlock(pos,true); drawer.dropContents();
        int total=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2)).stream().filter(e->e.getItem().is(Items.DIAMOND)).mapToInt(e->e.getItem().getCount()).sum();
        h.assertTrue(total==16384-64,"break drops ordinary plus overflow contents exactly once: "+total);
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void sixtyMaterialsHaveCorrectRecipesAndLoot(GameTestHelper h) {
        h.assertTrue(GTStorage.DRAWERS.size()==60,"all original metalset drawers registered");
        var pos=h.absolutePos(new BlockPos(1,1,1));var player=h.makeMockSurvivalPlayer();
        for(var spec:GTStorageMetals.ALL) {
            var block=GTStorage.drawer(spec.material()); h.getLevel().setBlock(pos,block.defaultBlockState(),3);
            h.assertTrue(h.getLevel().getBlockEntity(pos) instanceof DrawerQuadBlockEntity,"every material has its inventory entity");
            h.assertTrue(block.asItem().getDefaultInstance().getMaxStackSize()==16,"GT6 inventory block stack size");
            var recipe=h.getLevel().getRecipeManager().byKey(GregTech.id("hand/storage/drawer_quad_"+spec.suffix())).orElseThrow();
            var grid=new TransientCraftingContainer(new CraftingMenu(1,player.getInventory()),3,3);
            for(int slot:new int[]{0,2,6,8}) grid.setItem(slot,new ItemStack(GTStorage.chest(spec.material())));
            for(int slot:new int[]{1,3,5,7}) grid.setItem(slot,GTItems.getStack(MaterialPrefix.screw,spec.material(),1));
            grid.setItem(4,com.gregtech.gregtech.item.GTToolItem.create(com.gregtech.gregtech.api.tool.GTToolType.SCREWDRIVER,Materials.Steel,com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Wood")));
            h.assertTrue(((net.minecraft.world.item.crafting.CraftingRecipe)recipe).matches(grid,h.getLevel()) && recipe.getResultItem(h.getLevel().registryAccess()).is(block.asItem()),"same-material four chests and screws recipe "+spec.suffix());
            var wrench=com.gregtech.gregtech.item.GTToolItem.create(com.gregtech.gregtech.api.tool.GTToolType.WRENCH,Materials.Steel,com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Wood"));
            h.assertTrue(com.gregtech.gregtech.data.BlockHarvestPolicy.tool(block)==com.gregtech.gregtech.data.BlockHarvestPolicy.Tool.WRENCH,"wrench harvest");
            var drops=net.minecraft.world.level.block.Block.getDrops(block.defaultBlockState(),h.getLevel(),pos,h.getLevel().getBlockEntity(pos),player,wrench);
            h.assertTrue(drops.stream().anyMatch(s->s.is(block.asItem())),"self drop for "+spec.suffix());
        }
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void workshopPartsMatchOriginalRandomWritesAcrossFourPages(GameTestHelper h) {
        var origin=h.absolutePos(BlockPos.ZERO);var pos=h.absolutePos(new BlockPos(1,1,1));
        var mats=new com.gregtech.gregtech.api.material.GTMaterial[]{Materials.StainlessSteel,Materials.Bronze,Materials.Invar,Materials.Brass};
        var forms=new MaterialPrefix[]{MaterialPrefix.stick,MaterialPrefix.ingot,MaterialPrefix.plate,MaterialPrefix.plateCurved,MaterialPrefix.screw,MaterialPrefix.ring,MaterialPrefix.gearGt,MaterialPrefix.gearGtSmall};
        int[] bases={32,32,32,16,16,8,1,8},bounds={33,33,33,49,49,25,4,25};
        for(long seed=0;seed<96;seed++) {
            h.getLevel().removeBlock(pos,false);
            var data=new GTDungeonData(h.getLevel(),origin.getX(),origin.getY(),origin.getZ(),StoneType.LIMESTONE,StoneType.SLATE,3,
                new byte[5][5],2,2,0,new long[GTDungeonFeature.KEY_COUNT],new ItemStack[GTDungeonFeature.KEY_COUNT],
                new boolean[GTDungeonFeature.KEY_COUNT],new HashSet<>(),new HashSet<>(),RandomSource.create(seed));
            WorkshopDrawerSupplies.place(data,1,1,1);var drawer=(DrawerQuadBlockEntity)h.getLevel().getBlockEntity(pos);
            h.assertTrue(drawer.getBlockState().getBlock()==GTStorage.drawer(Materials.StainlessSteel) && drawer.getBlockState().getValue(DrawerQuadBlock.FACING)==Direction.EAST,"original 4011 east");
            ItemStack[] expected=new ItemStack[144];Arrays.fill(expected,ItemStack.EMPTY);var rng=RandomSource.create(seed);
            for(int page=0;page<4;page++) for(int form=0;form<8;form++) {int count=bases[form]+rng.nextInt(bounds[form]);int slot=page*36+rng.nextInt(36);expected[slot]=GTItems.getStack(forms[form],mats[page],count);}
            drawer.load(drawer.saveWithoutMetadata());
            for(int slot=0;slot<144;slot++) h.assertTrue(ItemStack.matches(expected[slot],drawer.items().getStackInSlot(slot)),"original random writes and save: seed="+seed+" slot="+slot);
            h.assertTrue(data.next(100000)==rng.nextInt(100000),"original count-before-slot random stream");
        }
        h.succeed();
    }
}
