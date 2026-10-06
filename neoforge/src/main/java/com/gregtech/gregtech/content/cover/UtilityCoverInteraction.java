package com.gregtech.gregtech.content.cover;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Native cover clicks shared by GT machine, container, pipe and fallback hosts. */
public final class UtilityCoverInteraction {
    private UtilityCoverInteraction(){}
    public static FluidStack sample(ItemStack held){
        var container=held.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM);
        if(container!=null){var fluid=container.drain(Integer.MAX_VALUE,IFluidHandler.FluidAction.SIMULATE);if(!fluid.isEmpty())return fluid;}
        if(held.getItem() instanceof com.gregtech.gregtech.item.BottleItem bottle&&bottle.fluid()!=null)return new FluidStack(bottle.fluid(),250);
        if(held.getItem() instanceof net.minecraft.world.item.BucketItem bucket)return new FluidStack(((com.gregtech.gregtech.mixin.BucketContentAccessor)bucket).gregtech$bucketContent(),1000);
        // These registered GT containers encode their fluid identity in the item catalog, not NBT.
        var key=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(held.getItem());
        if(key.getNamespace().equals("gregtech")){
            for(String prefix:new String[]{"bottle_","capsule_","cell_","bucket_"})if(key.getPath().startsWith(prefix)){
                var name=key.getPath().substring(prefix.length());
                var fluid=net.minecraft.core.registries.BuiltInRegistries.FLUID.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech",name));
                if(fluid!=net.minecraft.world.level.material.Fluids.EMPTY)return new FluidStack(fluid,1000);
            }
        }
        return FluidStack.EMPTY;
    }
    public static InteractionResult use(PanelCoverHost host,Player player,InteractionHand hand,BlockHitResult hit){
        var owner=host.coverOwner();var level=owner.getLevel();if(level==null)return InteractionResult.PASS;
        var held=player.getItemInHand(hand);var side=hit.getDirection();var installed=host.getCover(side);var id=CoverItems.behavior(installed);
        if(!player.mayBuild()||!level.mayInteract(player,owner.getBlockPos()))return InteractionResult.FAIL;
        if(installed.isEmpty()&&CoverItems.isCover(held)&&com.gregtech.gregtech.content.logistics.LogisticsCoverType.of(held)==null){
            if(!level.isClientSide&&host.attachCover(side,held)&&!player.getAbilities().instabuild)held.shrink(1);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if(id==null)return InteractionResult.PASS;
        if(com.gregtech.gregtech.platform.neoforge.NeoToolBindings.matches(held,"chisel")&&(CoverUtilityBehaviors.designCount(id)>0||installed.getItem() instanceof com.gregtech.gregtech.api.material.MaterialFormItem)){
            if(!level.isClientSide){CoverUtilityBehaviors.cycleDesign(installed,installed.getItem() instanceof com.gregtech.gregtech.api.material.MaterialFormItem material?MaterialCoverRules.designCount(material.getPrefix().getName(),material.getMaterial().getName()):CoverUtilityBehaviors.designCount(id));host.panels().changed();com.gregtech.gregtech.platform.neoforge.NeoToolBindings.damageForUse(held,1,player);}
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        boolean item=CoverUtilityBehaviors.FILTER_ITEM.equals(id)||CoverUtilityBehaviors.RETRIEVER_ITEM.equals(id),fluid=CoverUtilityBehaviors.FILTER_FLUID.equals(id);
        if(item||fluid){
            if(com.gregtech.gregtech.platform.neoforge.NeoToolBindings.isScrewdriver(held)||com.gregtech.gregtech.platform.neoforge.NeoToolBindings.matches(held,"soft_hammer")){
                if(!level.isClientSide){if(com.gregtech.gregtech.platform.neoforge.NeoToolBindings.matches(held,"soft_hammer"))CoverUtilityBehaviors.clearFilter(installed);else CoverUtilityBehaviors.toggleFilterMode(installed);host.panels().changed();com.gregtech.gregtech.platform.neoforge.NeoToolBindings.damageForUse(held,1,player);}
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            if(!com.gregtech.gregtech.platform.neoforge.transport.FluidPipeToolInteractions.isInteractionTool(held)){
                if(!level.isClientSide){if(item)CoverUtilityBehaviors.setItemFilter(installed,held,level.registryAccess());else CoverUtilityBehaviors.setFluidFilter(installed,sample(held),level.registryAccess());host.panels().changed();}
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        if(CoverUtilityBehaviors.CRAFTING_TABLE.equals(id)&&!com.gregtech.gregtech.platform.neoforge.transport.FluidPipeToolInteractions.isInteractionTool(held)){
            if(!level.isClientSide)CoverUtilityBehaviors.clickCraftingCover(player,(net.minecraft.server.level.ServerLevel)level,owner.getBlockPos());
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }
}
