package gregtech.block.energy;

import gregtech.block.misc.AutoToolBlock;
import gregtech.blockentity.energy.LaserConverterBlockEntity;
import gregtech.content.energy.LaserSpec;
import gregtech.api.tool.ToolInteractions;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class LaserConverterBlock extends AutoToolBlock {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    private final LaserSpec spec;
    public LaserConverterBlock(Properties properties, LaserSpec spec) {
        super(properties, spec.input(), 0); this.spec = spec;
        registerDefaultState(defaultBlockState().setValue(ACTIVE, false));
    }
    public LaserSpec spec() { return spec; }
    @Override public java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state,net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
        var stack=new net.minecraft.world.item.ItemStack(this);
        if(builder.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY) instanceof LaserConverterBlockEntity laser)
//? if >=1.21.1 {
            /*stack.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.CustomData.of(gregtech.content.cover.ComponentCoverFallback.forItem(laser,laser.saveWithId(builder.getLevel().registryAccess()))));
*///?} else {
            stack.getOrCreateTag().put("BlockEntityTag",gregtech.content.cover.ComponentCoverFallback.forItem(laser,laser.saveWithoutMetadata()));
//?}
        return java.util.List.of(stack);
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) {
        super.createBlockStateDefinition(builder); builder.add(ACTIVE);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new LaserConverterBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (world,pos,current,entity) -> { if(entity instanceof LaserConverterBlockEntity laser) laser.tick(); };
    }
//? if >=1.21.1 {
    /*@Override public InteractionResult interact(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
*///?} else {
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
//?}
        if(hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if(ToolInteractions.use(state,level,pos,player,hand,hit)) return InteractionResult.sidedSuccess(level.isClientSide);
        if(player.isShiftKeyDown() && player.getItemInHand(hand).isEmpty() && level.getBlockEntity(pos) instanceof LaserConverterBlockEntity laser) {
            if(!level.isClientSide)laser.toggleStopped();
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }
//? if >=1.21.1 {
    /*@Override public void appendHoverText(net.minecraft.world.item.ItemStack stack,net.minecraft.world.item.Item.TooltipContext context,
*///?} else {
    @Override public void appendHoverText(net.minecraft.world.item.ItemStack stack,net.minecraft.world.level.BlockGetter world,
//?}
            java.util.List<net.minecraft.network.chat.Component> tooltip,net.minecraft.world.item.TooltipFlag flag) {
        gregtech.client.EnergyDeviceTooltips.appendLaser(spec, getExplosionResistance(), tooltip);
    }
}
