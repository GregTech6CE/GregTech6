package com.gregtech.gregtech.content.transport.fluid;
import com.gregtech.gregtech.api.machine.TankSpec;
import com.gregtech.gregtech.api.machine.PipeSpec;
import com.gregtech.gregtech.block.machine.TankBlock;
import com.gregtech.gregtech.block.machine.FluidPipeBlock;
import com.gregtech.gregtech.blockentity.machine.TankBlockEntity;
import com.gregtech.gregtech.blockentity.machine.FluidPipeBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import java.util.List;
import java.util.ArrayList;
/** Original complete ordinary tank/pipe registration over the shared catalog. The parent owns logistics tanks. */
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID,bus=EventBusSubscriber.Bus.MOD)
public final class FluidTransportRegistries {
    private FluidTransportRegistries() {}
    public static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks("gregtech");
    public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems("gregtech");
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,"gregtech");
    public static final DeferredRegister<SoundEvent> SOUNDS=DeferredRegister.create(Registries.SOUND_EVENT,"gregtech");
    public static final DeferredHolder<SoundEvent,SoundEvent> WRENCH=SOUNDS.register("wrench",()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("gregtech","wrench")));
    private static final List<DeferredBlock<TankBlock>> TANKS=new ArrayList<>();
    private static final List<DeferredBlock<FluidPipeBlock>> PIPES=new ArrayList<>();
    public static DeferredBlock<TankBlock> WOOD_BARREL,DRUM_BRONZE,DRUM_STEEL;
    public static DeferredBlock<FluidPipeBlock> PIPE_MEDIUM_STEEL;
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<TankBlockEntity>> TANK=ENTITIES.register("tank",()->BlockEntityType.Builder.of(TankBlockEntity::new,TANKS.stream().map(DeferredHolder::get).toArray(Block[]::new)).build(null));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<FluidPipeBlockEntity>> FLUID_PIPE=ENTITIES.register("fluid_pipe",()->BlockEntityType.Builder.of(FluidPipeBlockEntity::new,PIPES.stream().map(DeferredHolder::get).toArray(Block[]::new)).build(null));
    public static List<DeferredBlock<TankBlock>> tanks(){return List.copyOf(TANKS);}
    public static List<DeferredBlock<FluidPipeBlock>> pipes(){return List.copyOf(PIPES);}
    public static void register(IEventBus bus){
        for(TankSpec spec:FluidTransportDefinitions.tanks()){
            var block=BLOCKS.register(spec.id(),()->new TankBlock(spec,TankBlock.defaultProperties(spec)));
            TANKS.add(block);ITEMS.register(spec.id(),()->new FluidTransportBlockItem(block.get(),new Item.Properties()));
            if(spec.id().equals("wood_barrel"))WOOD_BARREL=block;
            if(spec.id().equals("drum_bronze"))DRUM_BRONZE=block;
            if(spec.id().equals("drum_steel"))DRUM_STEEL=block;
        }
        for(PipeSpec spec:FluidTransportDefinitions.pipes()){
            var block=BLOCKS.register(spec.id(),()->new FluidPipeBlock(spec,FluidPipeBlock.defaultProperties(spec)));
            PIPES.add(block);ITEMS.register(spec.id(),()->new FluidTransportBlockItem(block.get(),new Item.Properties().stacksTo(stackSize(spec.size()))));
            if(spec.id().equals("pipe_medium_steel"))PIPE_MEDIUM_STEEL=block;
        }
        BLOCKS.register(bus);ITEMS.register(bus);ENTITIES.register(bus);SOUNDS.register(bus);
    }
    private static int stackSize(PipeSpec.PipeSize size){return switch(size){case TINY,SMALL->64;case MEDIUM->32;default->16;};}
    @SubscribeEvent public static void capabilities(RegisterCapabilitiesEvent event){
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,TANK.get(),TankBlockEntity::capabilityHandler);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,FLUID_PIPE.get(),FluidPipeBlockEntity::capabilityHandler);
    }
}
