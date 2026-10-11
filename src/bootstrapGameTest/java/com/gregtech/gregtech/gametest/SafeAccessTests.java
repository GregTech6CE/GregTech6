package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.registry.*;
import com.gregtech.gregtech.block.inventory.SafeBlock;
import com.gregtech.gregtech.blockentity.inventory.SafeBlockEntity;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.item.GTDungeonKeyItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class SafeAccessTests {
    @GameTest(template="test_empty")
    public static void keySafeUsesActualKeysAndClosesAnOpenMenu(GameTestHelper h) {
        var block = GTStorage.safe(Materials.Steel, true);
        var pos = h.absolutePos(new BlockPos(1,1,1));
        h.getLevel().setBlock(pos, block.defaultBlockState(), 3);
        var safe = (SafeBlockEntity)h.getLevel().getBlockEntity(pos);
        var player=h.makeMockPlayer(); player.setPos(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5);
        var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.NORTH,pos,false);
        h.assertTrue(!safe.canOpen(player) && block.getDestroyProgress(safe.getBlockState(),player,h.getLevel(),pos)==0,"unbound key lock starts closed, including creative players");
        var key=new ItemStack(GTDungeonKeys.byIndex(0)); player.setItemInHand(InteractionHand.MAIN_HAND,key);
        var use=new UseOnContext(player,InteractionHand.MAIN_HAND,hit);
        h.assertTrue(key.getItem().onItemUseFirst(key,use).consumesAction(),"blank key binds via actual item interaction");
        h.assertTrue(GTDungeonKeyItem.keyId(key)!=0 && safe.keyId()==GTDungeonKeyItem.keyId(key) && safe.opened(),"new lock and key share nonzero id");
        h.assertTrue(safe.getBlockState().getValue(SafeBlock.OPEN),"opened appearance updates block state");
        var menu=safe.createMenu(2,player.getInventory());
        h.assertTrue(menu.slots.size()==51 && menu.stillValid(player),"server menu has exactly 15+36 slots and is usable");
        var clientMenu=new com.gregtech.gregtech.client.gui.HopperContainerMenu(2,player.getInventory(),15);
        h.assertTrue(clientMenu.getType()==menu.getType() && clientMenu.slots.size()==51,"client and server menu type/slot counts agree");
        var clone=new ItemStack(GTDungeonKeys.byIndex(1)); player.setItemInHand(InteractionHand.MAIN_HAND,clone);
        h.assertTrue(clone.getItem().onItemUseFirst(clone,use).consumesAction(),"blank key clones unlocked safe");
        h.assertTrue(GTDungeonKeyItem.keyId(clone)==safe.keyId() && safe.opened(),"cloning does not toggle lock");
        player.setItemInHand(InteractionHand.MAIN_HAND,key); key.getItem().onItemUseFirst(key,use);
        h.assertTrue(!safe.opened() && !menu.stillValid(player),"locking invalidates existing menu immediately");
        var blank=new ItemStack(GTDungeonKeys.byIndex(2)); player.setItemInHand(InteractionHand.MAIN_HAND,blank);
        h.assertTrue(!blank.getItem().onItemUseFirst(blank,use).consumesAction() && GTDungeonKeyItem.keyId(blank)==0,"locked safe cannot clone keys");
        var wrong=GTDungeonKeys.stack(3,safe.keyId()+1,1); player.setItemInHand(InteractionHand.MAIN_HAND,wrong);
        h.assertTrue(!wrong.getItem().onItemUseFirst(wrong,use).consumesAction() && !safe.opened(),"wrong key cannot open");
        safe.inventory().setItem(14,new ItemStack(Items.DIAMOND,7)); safe.load(safe.saveWithoutMetadata());
        h.assertTrue(safe.inventory().getItem(0).isEmpty() && safe.inventory().getItem(14).getCount()==7,"sparse slot indexes survive reload");
        h.assertTrue(!safe.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER).isPresent(),"no automation bypass");
        player.setItemInHand(InteractionHand.MAIN_HAND,clone); clone.getItem().onItemUseFirst(clone,use);
        h.assertTrue(safe.canOpen(player),"cloned key still opens after save/load");
        h.succeed();
    }

    @GameTest(template="test_empty")
    public static void mechanicalSafeClaimsFirstUserAndLootIsConsumedOnce(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(1,1,1)); var block=GTStorage.SAFE.get();
        h.getLevel().setBlock(pos,block.defaultBlockState(),3);
        var safe=(SafeBlockEntity)h.getLevel().getBlockEntity(pos); var player=h.makeMockPlayer();
        var side=new BlockHitResult(Vec3.atCenterOf(pos),Direction.SOUTH,pos,false);
        h.assertTrue(block.use(safe.getBlockState(),h.getLevel(),pos,player,InteractionHand.MAIN_HAND,side)==net.minecraft.world.InteractionResult.PASS,"safe GUI only opens from front");
        h.assertTrue(!safe.saveWithoutMetadata().hasUUID("gt.owner"),"back face does not claim ownership");
        block.use(safe.getBlockState(),h.getLevel(),pos,player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.NORTH,pos,false));
        h.assertTrue(safe.saveWithoutMetadata().getUUID("gt.owner").equals(player.getUUID()),"first front interaction claims mechanical safe");
        safe.setOwner(java.util.UUID.randomUUID());
        h.assertTrue(!safe.canOpen(player) && block.getDestroyProgress(safe.getBlockState(),player,h.getLevel(),pos)==0,"nonowner cannot open or mine");
        safe.load(new CompoundTag());
        safe.setDungeonLoot(ResourceLocation.parse("gregtech_repair:shelf_paper"),123);
        safe.inventory().setItem(14,new ItemStack(Items.DIAMOND,3));
        safe.load(safe.saveWithoutMetadata()); safe.generateDungeonLoot();
        int paper=0; for(int i=0;i<15;i++) if(safe.inventory().getItem(i).is(Items.PAPER)) paper+=safe.inventory().getItem(i).getCount();
        h.assertTrue(paper==14 && safe.inventory().getItem(14).getCount()==3,"one loot candidate per vacant slot; preplaced key slot preserved");
        safe.inventory().clearContent(); safe.load(safe.saveWithoutMetadata()); safe.generateDungeonLoot();
        h.assertTrue(safe.inventory().isEmpty(),"loot does not replay after consumption and reload");
        safe.inventory().setItem(14,new ItemStack(Items.DIAMOND,9));
        var area=new net.minecraft.world.phys.AABB(pos).inflate(1);
        h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,area).forEach(net.minecraft.world.entity.item.ItemEntity::discard);
        h.getLevel().destroyBlock(pos,true);
        int diamonds=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,area).stream().filter(e->e.getItem().is(Items.DIAMOND)).mapToInt(e->e.getItem().getCount()).sum();
        h.assertTrue(diamonds==9 && safe.inventory().isEmpty(),"breaking safe drops contents exactly once");
        h.succeed();
    }

    @GameTest(template="test_empty")
    public static void previous27SlotInventoryIsNotDiscarded(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(1,1,1)); h.getLevel().setBlock(pos,GTStorage.SAFE.get().defaultBlockState(),3);
        var safe=(SafeBlockEntity)h.getLevel().getBlockEntity(pos);
        var tag=new CompoundTag(); var list=new net.minecraft.nbt.ListTag();
        for(int i=0;i<15;i++) { var entry=new ItemStack(Items.DIAMOND,64).save(new CompoundTag()); entry.putInt("Slot",i); list.add(entry); }
        var entry=new ItemStack(Items.EMERALD,9).save(new CompoundTag()); entry.putInt("Slot",26); list.add(entry); tag.put("gt.items",list);
        safe.load(tag); safe.load(safe.saveWithoutMetadata());
        safe.createMenu(1,h.makeMockPlayer().getInventory());
        h.assertTrue(safe.saveWithoutMetadata().getList("gt.overflow",10).size()==1,"full new inventory keeps previous excess through save and open");
        safe.inventory().removeItemNoUpdate(4); safe.createMenu(2,h.makeMockPlayer().getInventory());
        h.assertTrue(safe.inventory().getItem(4).is(Items.EMERALD) && safe.inventory().getItem(4).getCount()==9,"freed slot recovers old excess contents");
        h.assertTrue(safe.saveWithoutMetadata().getList("gt.overflow",10).isEmpty(),"recovered contents are not retained twice");
        safe.load(new CompoundTag()); h.assertTrue(safe.inventory().isEmpty(),"empty NBT clears both inventory and excess");
        h.succeed();
    }

    @GameTest(template="test_empty")
    public static void allSafeMaterialsHaveCorrectEntitiesAndRecipes(GameTestHelper h) {
        var player=h.makeMockPlayer();
        for(var spec:GTStorageMetals.ALL) for(boolean keyed:new boolean[]{false,true}) {
            var block=GTStorage.safe(spec.material(),keyed);
            h.assertTrue(GTBlockEntities.SAFE.get().isValid(block.defaultBlockState()),"safe entity includes every variant");
            h.assertTrue(block.getExplosionResistance()==spec.resistance()*2,"GT6 doubled resistance");
            var id=ResourceLocation.fromNamespaceAndPath("gregtech","hand/storage/"+(keyed?"key_safe_":"safe_")+spec.suffix());
            var recipe=(ShapedRecipe)h.getLevel().getRecipeManager().byKey(id).orElseThrow(()->new IllegalStateException("Missing "+id));
            var grid=new TransientCraftingContainer(new CraftingMenu(0,player.getInventory()),3,3);
            for(int i=0;i<9;i++) { var options=recipe.getIngredients().get(i).getItems(); h.assertTrue(options.length>0,"safe ingredient exists: "+id); grid.setItem(i,options[0].copy()); }
            h.assertTrue(recipe.matches(grid,h.getLevel()) && recipe.assemble(grid,h.getLevel().registryAccess()).is(block.asItem()),"actual safe crafting matches and outputs its material/type");
        }
        h.succeed();
    }
}
