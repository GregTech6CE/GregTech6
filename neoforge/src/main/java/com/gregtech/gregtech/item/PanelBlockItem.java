/* Adapted from Gregorius Techneticies, Copyright (c) 2019; LGPL-3.0-or-later.
 * Original MultiTileEntityPanel / Loader_MultiTileEntities; provenance in panels-source-20261006.json. */
package com.gregtech.gregtech.item;

import com.gregtech.gregtech.block.misc.PanelBlock;
import com.gregtech.gregtech.content.cover.PanelCoverHost;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import java.util.List;

/** MultiTileEntityPanel.canPlace=false; wooden panels expose their source plank name. */
public final class PanelBlockItem extends BlockItem implements PanelItemView {
    public PanelBlockItem(PanelBlock block, Properties properties) { super(block, properties.stacksTo(16)); }
    @Override public com.gregtech.gregtech.content.transport.PanelCatalog.Spec panelSpec(){return ((PanelBlock)getBlock()).spec();}
    @Override public InteractionResult useOn(UseOnContext context) {
        var player=context.getPlayer();var level=context.getLevel();var pos=context.getClickedPos();
        if(player==null||!player.mayBuild()||!level.mayInteract(player,pos))return InteractionResult.FAIL;
        if(!(level.getBlockEntity(pos) instanceof PanelCoverHost host))return InteractionResult.FAIL;
        var stack=context.getItemInHand();
        if(!level.isClientSide) {
            if(!host.attachCover(context.getClickedFace(),stack))return InteractionResult.FAIL;
            if(!player.getAbilities().instabuild)stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack, Item.TooltipContext level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack,level,tooltip,flag);
        var spec=((PanelBlock)getBlock()).spec();
        if(spec.kind().equals("wood")) {
            var plank=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(spec.input()));
            if(plank!=Items.AIR)tooltip.add(new ItemStack(plank).getHoverName().copy().withStyle(ChatFormatting.AQUA));
        }
    }
}
