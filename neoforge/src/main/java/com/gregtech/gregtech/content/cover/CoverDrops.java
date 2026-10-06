package com.gregtech.gregtech.content.cover;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;
/** Original cover data follows the harvested GT block; a crowbar returns a default attachment. */
public final class CoverDrops {
    private CoverDrops(){}
    private static final Set<BlockEntity> RETAINED=Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<BlockEntity,CompoundTag> HARVEST=new WeakHashMap<>();
    public static boolean retained(BlockEntity owner){return RETAINED.contains(owner);}
    public static void saveRuntime(BlockEntity owner,CompoundTag tag){
        if(!ComponentCoverFallback.ownClass(owner)||!(owner instanceof PanelCoverHost host))return;
        boolean any=false;for(var side:Direction.values())any|=!host.getCover(side).isEmpty();
        if(any)tag.putBoolean("gt.cover.stopped",host.panels().stopped());
    }
    public static void loadRuntime(BlockEntity owner,CompoundTag tag){
        if(ComponentCoverFallback.ownClass(owner)&&owner instanceof PanelCoverHost host&&tag.contains("gt.cover.stopped"))host.panels().restoreStopped(tag.getBoolean("gt.cover.stopped"));
    }
    private static CompoundTag coverData(BlockEntity owner,ServerLevel level){
        var saved=owner.saveWithoutMetadata(level.registryAccess());var result=new CompoundTag();
        for(String key:saved.getAllKeys())if(key.startsWith("gt_cover_")||key.startsWith("gt.logistics.cover.")||key.equals("gt.component_covers")||key.equals("gt.cover.stopped"))result.put(key,saved.get(key).copy());
        return result;
    }
    /** Player destruction removes the block before calculating its loot: snapshot attachments first. */
    public static BlockEntity beginHarvest(ServerLevel level,BlockPos pos,net.minecraft.server.level.ServerPlayer player){
        var state=level.getBlockState(pos);var owner=level.getBlockEntity(pos);
        if(owner==null||!ComponentCoverFallback.ownClass(owner)||player.isCreative()||player.isSpectator()
                ||!state.canHarvestBlock(level,pos,player)||state.getBlock().asItem()==net.minecraft.world.item.Items.AIR)return null;
        var saved=coverData(owner,level);if(saved.isEmpty())return null;
        HARVEST.put(owner,saved);RETAINED.add(owner);return owner;
    }
    public static void finishHarvest(BlockEntity owner,boolean success){
        if(owner==null)return;
        HARVEST.remove(owner);if(!success)RETAINED.remove(owner);
    }
    public static List<ItemStack> capture(BlockState state,ServerLevel level,BlockEntity owner,List<ItemStack> drops){
        if(owner==null||!ComponentCoverFallback.ownClass(owner))return drops;
        var coverData=HARVEST.get(owner);
        if(coverData==null)coverData=coverData(owner,level);
        if(coverData.isEmpty())return drops;
        for(var stack:drops)if(!stack.isEmpty()&&stack.is(state.getBlock().asItem())){
            var data=stack.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);var blockTag=data==null?new CompoundTag():data.copyTag();
            blockTag.merge(coverData);stack.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.CustomData.of(blockTag));
            RETAINED.add(owner);break;
        }
        return drops;
    }
}
