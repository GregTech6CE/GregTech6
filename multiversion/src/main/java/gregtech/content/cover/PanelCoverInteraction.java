package gregtech.content.cover;

//? if >=1.21.1 {
/*import gregtech.platform.neoforge.NeoToolBindings;
import gregtech.platform.neoforge.transport.FluidPipeToolInteractions;
*///?} else {
import gregtech.api.tool.*;
//?}
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

/** Shared attachment, panel buttons and tool configuration for every panel host. */
public final class PanelCoverInteraction {
    private PanelCoverInteraction(){}
    public static InteractionResult use(PanelCoverHost host,Player player,InteractionHand hand,BlockHitResult hit,boolean manageAllCovers){
        var owner = host.coverOwner();
        var level = owner.getLevel();
        var held = player.getItemInHand(hand);
        var side = hit.getDirection();
        if(level==null)return InteractionResult.PASS;
        if(!player.mayBuild()||!level.mayInteract(player,owner.getBlockPos()))return InteractionResult.FAIL;
        if(PanelCover.of(held)!=null){
            if(!level.isClientSide){
                if(host.attachCover(side,held)){if(!player.getAbilities().instabuild)held.shrink(1);}
                else player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.gt6ce.panel.unsupported"),true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
//? if >=1.21.1 {
        /*if(manageAllCovers&&NeoToolBindings.matches(held,"crowbar")){
            if(!level.isClientSide){var removed=host.removeCover(side);if(!removed.isEmpty()){if(!player.addItem(removed))player.drop(removed,false);NeoToolBindings.damageForUse(held,1,player);}}
*///?} else {
        if(manageAllCovers&&GTToolHelper.matchesTool(held,GTToolType.CROWBAR)){
            if(!level.isClientSide){var removed=host.removeCover(side);if(!removed.isEmpty()){if(!player.addItem(removed))player.drop(removed,false);GTToolHelper.damageForUse(held,1,player);}}
//?}
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        // Source controller forwards edge tool actions to the selected neighboring cover.
//? if >=1.21.1 {
        /*if (PanelCover.of(host.getCover(side)) == PanelCover.CONTROLLER
                && FluidPipeToolInteractions.isInteractionTool(held)) side = FluidPipeToolInteractions.selectedFace(hit);
        if (gregtech.platform.neoforge.NeoToolBindings.isMagnifyingGlass(held)
                && !host.getCover(side).isEmpty()) {
*///?} else {
        if (PanelCover.of(host.getCover(side)) == PanelCover.CONTROLLER
                && GTToolHelper.isInteractionTool(held)) side = ToolInteractions.selectedFace(hit);
        if (GTToolHelper.isMagnifyingGlass(held) && !host.getCover(side).isEmpty()) {
//?}
            // Generic mode/strength output currently replaces cover-specific diagnostics.
            // This consumed path has no tool-click damage charge.
            if (!level.isClientSide) {
                var stack = host.getCover(side);
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                        MachineCoverSpec.inverted(stack) ? "message.gt6ce.cover.inverted" : "message.gt6ce.cover.normal"), false);
                if (MachineCoverSpec.of(stack) != null && MachineCoverSpec.of(stack).detector()
                        || PanelCover.of(stack) != null && PanelCover.of(stack).strongConfig())
                    player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                            MachineCoverSpec.strong(stack) ? "message.gt6ce.cover.strong" : "message.gt6ce.cover.weak"), false);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
//? if >=1.21.1 {
        /*boolean cutter=NeoToolBindings.isWireCutter(held),screw=NeoToolBindings.isScrewdriver(held),chisel=NeoToolBindings.matches(held,"chisel");
*///?} else {
        boolean cutter=GTToolHelper.isWireCutter(held),screw=GTToolHelper.isScrewdriver(held),chisel=GTToolHelper.matchesTool(held,GTToolType.CHISEL);
//?}
        if((cutter||screw||chisel)&&(host.panels().configure(side,cutter,chisel)
                ||!chisel&&host instanceof gregtech.blockentity.machine.BasicMachineBlockEntity machine&&machine.configureControlCover(side,cutter))){
            // Fixed one-point charge; original cover tool-return costs differ by action.
            if(!level.isClientSide){
//? if >=1.21.1 {
                /*NeoToolBindings.damageForUse(held,1,player);
*///?} else {
                GTToolHelper.damageForUse(held,1,player);
//?}
                var stack=host.getCover(side);var panel=PanelCover.of(stack);
                String message=chisel?"message.gt6ce.panel.style":cutter?(MachineCoverSpec.strong(stack)?"message.gt6ce.cover.strong":"message.gt6ce.cover.weak"):
//? if >=1.21.1 {
                    /*panel==PanelCover.BUTTONS?(CoverStackData.readOrEmpty(stack).getBoolean(PanelCoverRuntime.RESET)?"message.gt6ce.panel.reset":"message.gt6ce.panel.latch"):
*///?} else {
                    panel==PanelCover.BUTTONS?(stack.getOrCreateTag().getBoolean(PanelCoverRuntime.RESET)?"message.gt6ce.panel.reset":"message.gt6ce.panel.latch"):
//?}
                    MachineCoverSpec.inverted(stack)?"message.gt6ce.cover.inverted":"message.gt6ce.cover.normal";
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable(message,PanelCoverRuntime.style(stack)+1),true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
//? if >=1.21.1 {
        /*if(!FluidPipeToolInteractions.isInteractionTool(held)){
*///?} else {
        if(!GTToolHelper.isInteractionTool(held)){
//?}
            var point=hit.getLocation().subtract(owner.getBlockPos().getX(),owner.getBlockPos().getY(),owner.getBlockPos().getZ());
            var uv=CoverFaceCoordinates.from(side,point.x,point.y,point.z);
            if(host.panels().click(side,uv.u(),uv.v()))return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }
}
