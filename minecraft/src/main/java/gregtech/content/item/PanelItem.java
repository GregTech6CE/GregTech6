/* Adapted from Gregorius Techneticies, Copyright (c) 2019; LGPL-3.0-or-later.
 * Original MultiTileEntityPanel / Loader_MultiTileEntities; provenance in panels-source-20261006.json. */
package gregtech.content.item;

import gregtech.content.block.misc.PanelBlock;
import gregtech.content.cover.PanelCoverHost;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import java.util.List;

/** MultiTileEntityPanel.canPlace=false; wooden panels expose their source plank name. */
public final class PanelItem extends Item implements PanelItemView {
    private final gregtech.content.transport.PanelCatalog.Spec spec;
    public PanelItem(gregtech.content.transport.PanelCatalog.Spec spec, Properties properties) { super(properties.stacksTo(16));this.spec=spec; }
    @Override public gregtech.content.transport.PanelCatalog.Spec panelSpec(){return spec;}
    @Override public Component getName(ItemStack stack) { return panelName(super.getName(stack)); }
    @Override public InteractionResult useOn(UseOnContext context) {
        var player=context.getPlayer();var level=context.getLevel();var pos=context.getClickedPos();
        if(player==null||!player.mayBuild()||!level.mayInteract(player,pos))return InteractionResult.FAIL;
        if(!(level.getBlockEntity(pos) instanceof PanelCoverHost host))return InteractionResult.FAIL;
        var stack=context.getItemInHand();
        if(!gregtech.content.cover.CoverAdmission.allowCover(host.coverOwner(),context.getClickedFace(),stack))return InteractionResult.PASS;
        if(!level.isClientSide) {
            if(!host.attachCover(context.getClickedFace(),stack))return InteractionResult.FAIL;
            if(!player.getAbilities().instabuild)stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
