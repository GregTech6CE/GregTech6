package com.gregtech.gregtech.integration.client;

import com.google.gson.JsonObject;
import com.gregtech.gregtech.block.plant.BushBlock;
import com.gregtech.gregtech.blockentity.BushBlockEntity;
import com.gregtech.gregtech.content.plant.MaterialBerryBushCatalog;
import com.gregtech.gregtech.registry.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import java.util.*;

/** Affected native bush paths only; supplied specimens, not a natural survival/restart claim. */
public final class MaterialBushRuntimeSmoke {
    private static final List<ItemStack> GALLERY=new ArrayList<>();
    private static void require(boolean ok,String detail){if(!ok)throw new IllegalStateException("Material bush: "+detail);}
    private static boolean plain(ItemStack stack){return !stack.has(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);}
    public static void server(MinecraftServer server,JsonObject receipt) {
        var level=server.overworld();var pos=new BlockPos(200,210,200);
        var actor=net.neoforged.neoforge.common.util.FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(UUID.fromString("a6d45170-f7eb-4128-a9c1-fbb3b4b9389e"),"MaterialBushCheckpoint"));
        actor.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);actor.moveTo(200.5,210,202.5);
        level.setBlock(pos.below(),Blocks.DIRT.defaultBlockState(),3);
        int checked=0;
        require(GTBushes.allBlocks().length==1044,"1044 native bush blocks");
        for(var variant:MaterialBerryBushCatalog.variants()) {
            var berry=BuiltInRegistries.ITEM.get(ResourceLocation.parse(variant.berryItemId()));
            var block=GTBushes.byBerry(variant.berryItemId());
            require(block!=null && BuiltInRegistries.BLOCK.getKey(block).getPath().equals(variant.blockPath()),"native block identity "+variant);
            require(GTBlockEntities.BUSH.get().isValid(block.defaultBlockState()),"BE type covers "+variant);
            level.setBlock(pos,GTBushes.BUSH.get().defaultBlockState(),3);actor.getInventory().clearContent();
            actor.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(berry,4));
            var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false);
            var planted=GTBushes.BUSH.get().interact(level.getBlockState(pos),level,pos,actor,InteractionHand.MAIN_HAND,hit);
            require(planted.consumesAction() && level.getBlockState(pos).is(block) && actor.getMainHandItem().getCount()==4,"non-consuming native specimen planting "+variant);
            var be=(BushBlockEntity)level.getBlockEntity(pos);
            var tag=new CompoundTag();tag.putInt("growth",255);tag.putString("berry","wrong_legacy_value");be.loadWithComponents(tag,level.registryAccess());
            require(be.berryId().equals(variant.berryItemId()) && !be.saveWithoutMetadata(level.registryAccess()).contains("berry"),"flat identity ignores obsolete identity tag "+variant);
            int increment=be.grow();require(increment>0 && be.stage()==1 && be.growth()==increment-1,"source byte-counter overflow "+variant);
            var drops=Block.getDrops(level.getBlockState(pos),level,pos,be);
            require(drops.size()==1 && drops.get(0).is(block.asItem()) && plain(drops.get(0)),"flat native drop "+variant);
            actor.getInventory().clearContent();level.setBlock(pos,level.getBlockState(pos).setValue(BushBlock.STAGE,3),3);
            var harvested=block.interact(level.getBlockState(pos),level,pos,actor,InteractionHand.MAIN_HAND,hit);
            int count=actor.getInventory().countItem(berry);
            require(harvested.consumesAction() && count>=1 && count<=2 && level.getBlockState(pos).getValue(BushBlock.STAGE)==0,"actual material harvest/reset "+variant);
            checked++;
        }
        actor.getInventory().clearContent();level.removeBlock(pos,false);
        receipt.addProperty("materialBushNativePlantHarvestDropChecked",checked);
        receipt.addProperty("materialBushWorldgenPool",com.gregtech.gregtech.content.plant.BerryBushCatalog.worldgenSize());
    }
    public static void client(Minecraft minecraft,JsonObject receipt) {
        GALLERY.clear();int checked=0,states=0;
        for(var variant:MaterialBerryBushCatalog.variants()) {
            var block=GTBushes.byBerry(variant.berryItemId());var stack=new ItemStack(block);
            OriginFeedbackChecks.checkModel(minecraft,stack,true);
            require(!stack.getHoverName().getString().contains("block.gregtech.")&&!stack.getHoverName().getString().contains("item.gregtech."),"localized material bush name "+variant);
            require((minecraft.getItemColors().getColor(stack,0)&0xffffff)==0x009000 && (minecraft.getItemColors().getColor(stack,1)&0xffffff)==variant.colour(),"source inventory layers "+variant);
            for(int stage=0;stage<4;stage++) {
                int expected=new int[]{0x009000,0xff9090,0x80ff80,variant.colour()}[stage];
                require((minecraft.getBlockColors().getColor(block.defaultBlockState().setValue(BushBlock.STAGE,stage),null,null,1)&0xffffff)==expected,"world stage RGB "+variant);
            }
            for(var state:block.getStateDefinition().getPossibleStates()) {
                require(minecraft.getBlockRenderer().getBlockModel(state)!=minecraft.getModelManager().getMissingModel(),"all support/stage baked models "+variant);
                states++;
            }
            if(Set.of("bush_plant_gt_berry_copper","bush_plant_gt_berry_iron","bush_plant_gt_berry_gold","bush_plant_gt_berry_diamond","bush_plant_gt_berry_acacia","bush_plant_gt_berry_tungsten","bush_plant_gt_berry_redstone","bush_plant_gt_berry_coal").contains(variant.blockPath()))GALLERY.add(stack);
            checked++;
        }
        SurfaceFeedbackChecks.client(minecraft,receipt);
        receipt.addProperty("materialBushInventoryModelsAndColorsChecked",checked);
        receipt.addProperty("materialBushStageSupportModelsChecked",states);
    }
    public static void render(net.minecraft.client.gui.GuiGraphics graphics,Minecraft minecraft) {
        graphics.pose().pushPose();graphics.pose().translate(0,0,500);
        graphics.fill(4,4,470,95,0xff16161d);
        graphics.drawString(minecraft.font,"Material berry bushes: 1034 registered variants",8,8,0xffffff,false);
        for(int i=0;i<GALLERY.size();i++) {
            var stack=GALLERY.get(i);graphics.renderItem(stack,8+i*56,28);
            graphics.drawString(minecraft.font,"#"+i,8+i*56,50,0xffffff,false);
        }
        graphics.drawString(minecraft.font,"Solid material RGB / native 1-2 berry harvest",8,73,0xffffff,false);
        graphics.pose().popPose();
    }
}
