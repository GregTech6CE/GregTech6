package com.gregtech.gregtech.block.tool;
import com.gregtech.gregtech.content.tool.FluidAttachmentSpec;
import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.registry.GTFluids;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.IFluidHandler;
/** A material-defined face attachment: funnel only fills; tap/nozzle only drain their phase. */
public class FluidAttachmentBlock extends net.minecraft.world.level.block.Block implements com.gregtech.gregtech.api.tool.ToolInteractionTarget {
    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING =
            net.minecraft.world.level.block.state.properties.DirectionProperty.create("facing", d -> d != Direction.UP);
    private final java.util.EnumMap<Direction,net.minecraft.world.phys.shapes.VoxelShape> shapes = new java.util.EnumMap<>(Direction.class);
    private final FluidAttachmentSpec spec;
    public FluidAttachmentBlock(FluidAttachmentSpec spec,Properties properties){
        super(properties.noOcclusion());this.spec=spec;
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));
        var shape=net.minecraft.world.phys.shapes.Shapes.empty();
        for(int[] b:com.gregtech.gregtech.content.tool.OriginalToolShapes.bounds(spec.shape()))shape=net.minecraft.world.phys.shapes.Shapes.or(shape,box(b[0],b[1],b[2],b[3],b[4],b[5]));
        for(var direction:new Direction[]{Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST}){
            shapes.put(direction,shape.optimize());var rotated=net.minecraft.world.phys.shapes.Shapes.empty();
            for(var b:shape.toAabbs())rotated=net.minecraft.world.phys.shapes.Shapes.or(rotated,net.minecraft.world.phys.shapes.Shapes.box(1-b.maxZ,b.minY,b.minX,1-b.minZ,b.maxY,b.maxX));
            shape=rotated;
        }
        shapes.put(Direction.DOWN,net.minecraft.world.phys.shapes.Shapes.or(box(5,2,5,11,3,11),box(6,1,6,10,2,10),box(7,0,7,9,1,9)));
    }
    @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block,BlockState> builder){builder.add(FACING);}
    @Override public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context){return shapes.get(state.getValue(FACING));}
    @Override public BlockState rotate(BlockState state,net.minecraft.world.level.block.Rotation rotation){return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));}
    @Override public BlockState mirror(BlockState state,net.minecraft.world.level.block.Mirror mirror){return rotate(state,mirror.getRotation(state.getValue(FACING)));}
    @Override public com.gregtech.gregtech.api.tool.ToolInteractionSpec toolInteraction(BlockState state,net.minecraft.world.item.ItemStack tool){
        return com.gregtech.gregtech.api.tool.GTToolHelper.isMachineWrench(tool)?com.gregtech.gregtech.api.tool.ToolInteractionSpec.facing(FACING,
                spec.shape().equals("fluid_funnel")?com.gregtech.gregtech.block.machine.MachineRotationType.ALL:com.gregtech.gregtech.block.machine.MachineRotationType.HORIZONTAL):null;
    }
    public FluidAttachmentSpec spec(){return spec;}
    public int tintRgb(){return spec.material().getColor();}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){
        if(!context.getClickedFace().getAxis().isHorizontal()&&!(spec.shape().equals("fluid_funnel")&&context.getClickedFace()==Direction.UP))return null;
        // Original meshes lie against the named facing, i.e. facing points into the attached tank.
        return defaultBlockState().setValue(FACING,context.getClickedFace().getOpposite());
    }
    public boolean accepts(FluidStack fluid){
        if(fluid.isEmpty())return false;
        boolean gas=fluid.getFluid().getFluidType().isLighterThanAir(),acid=false;
        // entryForFluid, not the FluidType key: the three world waters report vanilla water's
        // FluidType (see GTWorldWaterFluid), so the type key would lose their GT6 entry.
        var entry=GTFluids.entryForFluid(fluid.getFluid());
        if(entry!=null){gas|=entry.gas();if(entry.materialKey()!=null)acid=GTMaterialRegistry.get(entry.materialKey()).resolve().has(MaterialProperty.ACID);}
        return gas==spec.shape().equals("cap_nozzle")&&(!acid||spec.acidProof());
    }
    public IFluidHandler access(IFluidHandler tank){
        boolean funnel=spec.shape().equals("fluid_funnel");
        return new IFluidHandler(){
            public int getTanks(){return tank.getTanks();}
            public FluidStack getFluidInTank(int slot){return tank.getFluidInTank(slot).copy();}
            public int getTankCapacity(int slot){return tank.getTankCapacity(slot);}
            public boolean isFluidValid(int slot,FluidStack fluid){return funnel&&accepts(fluid)&&tank.isFluidValid(slot,fluid);}
            public int fill(FluidStack fluid,FluidAction action){return funnel&&accepts(fluid)?tank.fill(fluid,action):0;}
            public FluidStack drain(FluidStack fluid,FluidAction action){return !funnel&&accepts(fluid)?tank.drain(fluid,action):FluidStack.EMPTY;}
            public FluidStack drain(int amount,FluidAction action){
                if(funnel||amount<=0)return FluidStack.EMPTY;
                for(int i=0;i<tank.getTanks();i++){
                    var fluid=tank.getFluidInTank(i).copy();if(!accepts(fluid))continue;
                    fluid.setAmount(Math.min(amount,fluid.getAmount()));var out=tank.drain(fluid,action);if(!out.isEmpty())return out;
                }
                return FluidStack.EMPTY;
            }
        };
    }
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(hand!=InteractionHand.MAIN_HAND)return InteractionResult.PASS;
        if(com.gregtech.gregtech.api.tool.ToolInteractions.use(state,level,pos,player,hand,hit))return InteractionResult.sidedSuccess(level.isClientSide);
        var facing=state.getValue(FACING);var targetPos=pos.relative(facing);
        if(!level.hasChunkAt(targetPos))return InteractionResult.PASS;
        var target=level.getBlockEntity(targetPos);if(target==null)return InteractionResult.PASS;
        var handler=target.getCapability(ForgeCapabilities.FLUID_HANDLER,facing.getOpposite()).orElse(null);if(handler==null)return InteractionResult.PASS;
        if(level.isClientSide)return InteractionResult.SUCCESS;
        if(player.getItemInHand(hand).isEmpty()&&spec.shape().equals("tap"))
            return pourBelow(level,pos,access(handler))?InteractionResult.CONSUME:InteractionResult.PASS;
        return FluidUtil.interactWithFluidHandler(player,hand,access(handler))?InteractionResult.CONSUME:InteractionResult.PASS;
    }
    /** GT6 tap click: 250 L water/ordinary liquid, 1000 L lava, one material unit of molten metal. */
    public static boolean pourBelow(Level level,BlockPos pos,IFluidHandler source){
        var below=pos.below();if(!level.hasChunkAt(below))return false;
        var fluid=source.drain(Integer.MAX_VALUE,IFluidHandler.FluidAction.SIMULATE);if(fluid.isEmpty())return false;
        var distilled=GTFluids.still("DistW");
        boolean water=fluid.getFluid().isSame(net.minecraft.world.level.material.Fluids.WATER)||(distilled!=null&&fluid.getFluid().isSame(distilled.get()));
        var state=level.getBlockState(below);
        if(water&&(state.is(net.minecraft.world.level.block.Blocks.CAULDRON)||state.is(net.minecraft.world.level.block.Blocks.WATER_CAULDRON))){
            int current=state.is(net.minecraft.world.level.block.Blocks.CAULDRON)?0:state.getValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL);
            int added=0,amount=0;
            for(int n=3-current;n>0;n--){int needed=(n*1000+2)/3;if(fluid.getAmount()>=needed){added=n;amount=needed;break;}}
            if(added==0)return false;
            var drained=source.drain(new FluidStack(fluid,amount),IFluidHandler.FluidAction.EXECUTE);
            if(drained.getAmount()!=amount)return false;
            level.setBlockAndUpdate(below,net.minecraft.world.level.block.Blocks.WATER_CAULDRON.defaultBlockState().setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL,current+added));return true;
        }
        var target=level.getBlockEntity(below);if(target==null)return false;
        var sink=target.getCapability(ForgeCapabilities.FLUID_HANDLER,Direction.UP).orElse(null);if(sink==null)return false;
        int limit=fluid.getFluid().isSame(net.minecraft.world.level.material.Fluids.LAVA)?1000:250;
        // entryForFluid, not the FluidType key: GT6's three world waters report vanilla water's
        // FluidType (see GTWorldWaterFluid), so the type key would resolve them to minecraft:water.
        var entry=GTFluids.entryForFluid(fluid.getFluid());
        if(!water&&entry!=null&&com.gregtech.gregtech.api.fluid.FluidTexturePolicy.kind(entry)==com.gregtech.gregtech.api.fluid.FluidTexturePolicy.Kind.MOLTEN)limit=144;
        return !FluidUtil.tryFluidTransfer(sink,source,limit,true).isEmpty();
    }
}
