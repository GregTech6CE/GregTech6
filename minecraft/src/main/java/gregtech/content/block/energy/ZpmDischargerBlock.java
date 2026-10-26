package gregtech.content.block.energy;
import gregtech.content.block.misc.AutoToolBlock;
import gregtech.content.blockentity.energy.ZpmDischargerBlockEntity;
import gregtech.engine.tool.ToolInteractions;
import gregtech.content.energy.ZpmEnergy;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
public final class ZpmDischargerBlock extends AutoToolBlock {
    public static final BooleanProperty ACTIVE=BooleanProperty.create("active");
    public static final BooleanProperty LOADED=BooleanProperty.create("loaded");
    private final boolean electric;
    public ZpmDischargerBlock(Properties p,boolean electric){super(p,ZpmEnergy.PACKET,0);this.electric=electric;registerDefaultState(defaultBlockState().setValue(ACTIVE,false).setValue(LOADED,false));}
    public boolean electric(){return electric;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState>b){super.createBlockStateDefinition(b);b.add(ACTIVE,LOADED);}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new ZpmDischargerBlockEntity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide?null:(w,p,state,e)->{if(e instanceof ZpmDischargerBlockEntity z)z.tick();};}
//? if >=1.21.1 {
    /*@Override public InteractionResult interact(BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
*///?} else {
    @Override public InteractionResult use(BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
//?}
        if(hand!=InteractionHand.MAIN_HAND)return InteractionResult.PASS;
        if(ToolInteractions.use(s,l,p,player,hand,hit))return InteractionResult.sidedSuccess(l.isClientSide);
        if(!(l.getBlockEntity(p) instanceof ZpmDischargerBlockEntity z))return InteractionResult.PASS;
        if(!l.isClientSide){
            var held=player.getItemInHand(hand);
            if(held.isEmpty()&&player.isShiftKeyDown())z.toggleStopped();
            else if(ZpmEnergy.isModule(held))player.setItemInHand(hand,z.inventory().insertItem(0,held,false));
            else if(held.isEmpty()){var extracted=z.inventory().extractItem(0,1,false);if(!player.addItem(extracted))player.drop(extracted,false);}
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("gt6ce.zpm.status",z.totalEnergy(),net.minecraft.network.chat.Component.translatable(z.stopped()?"gui.gt6ce.behavior.off":"gui.gt6ce.behavior.on"),z.outputType().getShortName()),true);
        }
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
    @Override public java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState s,net.minecraft.world.level.storage.loot.LootParams.Builder b){
        var stack=new net.minecraft.world.item.ItemStack(asItem());
//? if >=1.21.1 {
        /*if(b.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY) instanceof ZpmDischargerBlockEntity z)stack.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.CustomData.of(gregtech.content.cover.ComponentCoverFallback.forItem(z,z.saveWithId(b.getLevel().registryAccess()))));
*///?} else {
        if(b.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY) instanceof ZpmDischargerBlockEntity z)stack.getOrCreateTag().put("BlockEntityTag",gregtech.content.cover.ComponentCoverFallback.forItem(z,z.saveWithoutMetadata()));
//?}
        return java.util.List.of(stack);
    }
//? if >=1.21.1 {
    /*@Override public void appendHoverText(net.minecraft.world.item.ItemStack s,net.minecraft.world.item.Item.TooltipContext context,java.util.List<net.minecraft.network.chat.Component> lines,net.minecraft.world.item.TooltipFlag f){
*///?} else {
    @Override public void appendHoverText(net.minecraft.world.item.ItemStack s,net.minecraft.world.level.BlockGetter w,java.util.List<net.minecraft.network.chat.Component> lines,net.minecraft.world.item.TooltipFlag f){
//?}
        lines.add(net.minecraft.network.chat.Component.translatable("gt6ce.zpm.output",ZpmEnergy.PACKET,electric?"EU":"QU"));
        lines.add(net.minecraft.network.chat.Component.translatable("gt6ce.zpm.controls"));
    }
}
