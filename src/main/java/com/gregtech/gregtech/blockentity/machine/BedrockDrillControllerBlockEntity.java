package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.api.inventory.BlockContents;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.multiblock.*;
import com.gregtech.gregtech.block.OreBlock;
import com.gregtech.gregtech.block.OreHostStone;
import com.gregtech.gregtech.block.machine.OriginalBedrockDrillControllerBlock;
import com.gregtech.gregtech.block.stone.*;
import com.gregtech.gregtech.blockentity.GTEnergyBlockEntity;
import com.gregtech.gregtech.content.multiblock.LargeMachineParts;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.registry.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.*;
import java.util.*;

/** GT6 drilling cycle: intact bedrock deposit, 32768 RU and 100 mB lubricant per output. */
public class BedrockDrillControllerBlockEntity extends GTEnergyBlockEntity implements MultiblockPortOwner,BlockContents {
    public static final long ENERGY_CAPACITY=40000, WORK_ENERGY=32768;
    private long energy;
    private int stoneType=java.util.concurrent.ThreadLocalRandom.current().nextInt(StoneType.values().length+2);
    private final List<GTMaterial> deposit=new ArrayList<>();
    private final PartBindings<BlockPos,MultiblockLayout.Role> bindings=new PartBindings<>();
    private final ItemStackHandler output=new ItemStackHandler(1) {
        @Override public boolean isItemValid(int slot,ItemStack stack){return false;}
        @Override protected void onContentsChanged(int slot){setChanged();}
    };
    private final FluidTank lubricant=new FluidTank(16000,stack->GTFluids.still("Lubricant")!=null&&stack.getFluid()==GTFluids.still("Lubricant").get()) {
        @Override protected void onContentsChanged(){setChanged();}
    };
    private final IFluidHandler lubricantInput=new com.gregtech.gregtech.api.fluid.FluidPort(lubricant,true,false);
    private LazyOptional<IItemHandler> items=LazyOptional.of(()->output);
    private LazyOptional<IFluidHandler> fluids=LazyOptional.of(()->lubricant);
    public BedrockDrillControllerBlockEntity(BlockPos pos,BlockState state){super(GTBlockEntities.BEDROCK_DRILL.get(),pos,state);}
    public static MultiblockLayout.Role role(int x,int y,int z) {
        return y==-4?MultiblockLayout.Role.CASING:y==-1&&((x==0)!=(z==0))?MultiblockLayout.Role.ENERGY_INPUT:MultiblockLayout.Role.FLUID_INPUT;
    }
    @Override public boolean isStructureOk(){
        if(level==null||isRemoved()||worldPosition.getY()-5<level.getMinBuildHeight())return false;
        boolean originalId=getBlockState().getBlock() instanceof OriginalBedrockDrillControllerBlock;
        var parts=new LinkedHashMap<BlockPos,MultiblockLayout.Role>();deposit.clear();
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++) {
            var floor=worldPosition.offset(x,-5,z);
            if(!level.hasChunkAt(floor)){bindings.clear(this::release);return false;}
            // §103.B: bedrock deposits are ore blocks whose host rock (block state) is bedrock.
            BlockState floorState=level.getBlockState(floor);
            if(floorState.getBlock() instanceof OreBlock ore&&OreBlock.isBedrockOre(floorState)) {
                deposit.add(ore.material());if(!ore.prefix().getName().equals("oreSmall"))deposit.add(ore.material());
            } else if(!floorState.is(Blocks.BEDROCK)){bindings.clear(this::release);return false;}
            for(int y=-4;y<=0;y++){
                if(x==0&&y==0&&z==0)continue;
                var pos=worldPosition.offset(x,y,z);
                var expected=y==-4
                        ? originalId?LargeMachineParts.block(18103):GTMultiblocks.BEDROCK_DRILL_WALL.get()
                        : LargeMachineParts.block(18026);
                if(!level.getBlockState(pos).is(expected)||!(level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity)){bindings.clear(this::release);return false;}
                parts.put(pos,role(x,y,z));
            }
        }
        return bindings.update(parts,p->((MultiblockPortBlockEntity)level.getBlockEntity(p)).canBind(worldPosition),
                (p,r)->((MultiblockPortBlockEntity)level.getBlockEntity(p)).bind(worldPosition,r),this::release);
    }
    private void release(BlockPos pos){if(level!=null&&level.hasChunkAt(pos)&&level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity part)part.release(worldPosition);}
    public static void serverTick(Level level,BlockPos pos,BlockState state,BedrockDrillControllerBlockEntity machine){machine.tick();}
    public void tick(){
        if(level==null||level.isClientSide||!isStructureOk())return;
        if(!output.getStackInSlot(0).isEmpty()&&level.hasChunkAt(worldPosition.above())){
            var above=level.getBlockEntity(worldPosition.above());
            if(above!=null)above.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.DOWN).ifPresent(target->{
                var remaining=ItemHandlerHelper.insertItemStacked(target,output.getStackInSlot(0).copy(),false);output.setStackInSlot(0,remaining);
            });
        }
        if(energy<WORK_ENERGY||!output.getStackInSlot(0).isEmpty()||lubricant.getFluidAmount()<100)return;
        ItemStack product=nextProduct();if(product.isEmpty())return;
        lubricant.drain(100,IFluidHandler.FluidAction.EXECUTE);energy-=WORK_ENERGY;output.setStackInSlot(0,product);setChanged();
    }
    private ItemStack nextProduct(){
        if(level.random.nextInt(1000)==0)stoneType=level.random.nextInt(StoneType.values().length+2);
        int selector=level.random.nextInt(128);
        if(selector<deposit.size()){
            var material=deposit.get(selector);var byproducts=material.getByProducts();
            if(!byproducts.isEmpty()&&level.random.nextInt(32)==0)material=byproducts.get(level.random.nextInt(byproducts.size()));
            // GT6 outputs oreBroken/ores_broken, not oreRaw: it can be placed and processed as a 2U ore.
            OreHostStone host=level.dimension()==Level.NETHER?OreHostStone.NETHERRACK
                    :stoneType<StoneType.values().length?OreHostStone.of(StoneType.values()[stoneType])
                    :stoneType%2==0?OreHostStone.DEEPSLATE:OreHostStone.STONE;
            ItemStack broken=OreBlock.brokenStack(material,host);
            if(!broken.isEmpty())return broken;
            // Some mod byproducts have no registered GT ore block; keep the selected material.
            ItemStack raw=GTItems.getStack(MaterialPrefix.oreRaw,material);
            if(!raw.isEmpty())return raw;
            broken=OreBlock.brokenStack(deposit.get(selector),host);
            return broken.isEmpty()?GTItems.getStack(MaterialPrefix.oreRaw,deposit.get(selector)):broken;
        }
        if(level.random.nextInt(1000)==0){
            var dust=GTItems.getStack(MaterialPrefix.dust,com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Bedrock"));
            if(!dust.isEmpty())return dust;
        }
        if(level.dimension()==Level.NETHER)return new ItemStack(Blocks.NETHERRACK);
        if(stoneType<StoneType.values().length){
            var block=GTBlocks.getStone(StoneType.values()[stoneType],StoneVariant.COBBLE);
            if(block!=null)return new ItemStack(block);
        }
        return new ItemStack(stoneType%2==0?Blocks.COBBLED_DEEPSLATE:Blocks.COBBLESTONE);
    }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction side){
        if(cap==ForgeCapabilities.ITEM_HANDLER&&(side==null||side==Direction.UP))return items.cast();
        if(cap==ForgeCapabilities.FLUID_HANDLER)return fluids.cast();
        return super.getCapability(cap,side);
    }
    @Override public void invalidateCaps(){super.invalidateCaps();items.invalidate();fluids.invalidate();}
    @Override public void reviveCaps(){super.reviveCaps();items=LazyOptional.of(()->output);fluids=LazyOptional.of(()->lubricant);}
    @Override public void setRemoved(){bindings.clear(this::release);super.setRemoved();}
    @Override public void dropContents(){BlockContents.drop(this,output);}
    @Override public IFluidHandler portFluids(MultiblockLayout.Role role){return role==MultiblockLayout.Role.FLUID_INPUT?lubricantInput:null;}
    @Override public Collection<GregTechTags.Tag> portEnergyTypes(MultiblockLayout.Role role){return role==MultiblockLayout.Role.ENERGY_INPUT?List.of(GregTechTags.Energy.RU):List.of();}
    @Override public long portEnergyInputRecommended(MultiblockLayout.Role role,GregTechTags.Tag type){return 2048;}
    @Override public long portEnergyInputMin(MultiblockLayout.Role role,GregTechTags.Tag type){return 1024;}
    @Override public long portEnergyInputMax(MultiblockLayout.Role role,GregTechTags.Tag type){return 4096;}
    @Override public long portEnergyStored(MultiblockLayout.Role role,GregTechTags.Tag type){return type==GregTechTags.Energy.RU?energy:0;}
    @Override public long portEnergyCapacity(MultiblockLayout.Role role,GregTechTags.Tag type){return type==GregTechTags.Energy.RU?ENERGY_CAPACITY:0;}
    @Override public long injectPortEnergy(MultiblockLayout.Role role,GregTechTags.Tag type,long size,long amount,boolean execute){return role==MultiblockLayout.Role.ENERGY_INPUT&&isStructureOk()?doInject(type,null,size,amount,execute):0;}
    @Override public boolean isEnergyType(GregTechTags.Tag type,Direction side,boolean emitting){return !emitting&&type==GregTechTags.Energy.RU&&side==null;}
    @Override public Collection<GregTechTags.Tag> getEnergyTypes(Direction side){return side==null?List.of(GregTechTags.Energy.RU):List.of();}
    @Override public long getEnergySizeInputRecommended(GregTechTags.Tag type,Direction side){return 2048;}
    @Override public long getEnergySizeInputMin(GregTechTags.Tag type,Direction side){return 1024;}
    @Override public long getEnergySizeInputMax(GregTechTags.Tag type,Direction side){return 4096;}
    @Override public long getEnergySizeOutputRecommended(GregTechTags.Tag type,Direction side){return 0;}
    @Override public long getEnergyOffered(GregTechTags.Tag type,Direction side,long size){return 0;}
    @Override public long getEnergyDemanded(GregTechTags.Tag type,Direction side,long size){
        if(type!=GregTechTags.Energy.RU||side!=null||size==Long.MIN_VALUE)return 0;
        long magnitude=Math.abs(size);
        return magnitude>=1024&&magnitude<=4096?(ENERGY_CAPACITY-energy)/magnitude:0;
    }
    @Override public long doInject(GregTechTags.Tag type,Direction side,long size,long amount,boolean execute){
        if(type!=GregTechTags.Energy.RU||side!=null||size==Long.MIN_VALUE||amount<=0||!isStructureOk())return 0;
        size=Math.abs(size);if(size<1024)return 0;
        if(size>4096){if(execute)level.explode(null,worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,6,Level.ExplosionInteraction.BLOCK);return amount;}
        long accepted=Math.min(amount,(ENERGY_CAPACITY-energy)/size);if(execute&&accepted>0){energy+=size*accepted;setChanged();}return accepted;
    }
    @Override public long getEnergyStored(GregTechTags.Tag type,Direction side){return type==GregTechTags.Energy.RU?energy:0;}
    @Override public long getEnergyCapacity(GregTechTags.Tag type,Direction side){return type==GregTechTags.Energy.RU?ENERGY_CAPACITY:0;}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.putLong("gt.drill_energy",energy);tag.putInt("gt.stone_type",stoneType);tag.put("gt.output",output.serializeNBT());tag.put("gt.lubricant",lubricant.writeToNBT(new CompoundTag()));}
    @Override public void load(CompoundTag tag){super.load(tag);energy=Math.max(0,Math.min(ENERGY_CAPACITY,tag.getLong("gt.drill_energy")));stoneType=Math.floorMod(tag.getInt("gt.stone_type"),StoneType.values().length+2);output.deserializeNBT(tag.getCompound("gt.output"));lubricant.readFromNBT(tag.getCompound("gt.lubricant"));}
}
