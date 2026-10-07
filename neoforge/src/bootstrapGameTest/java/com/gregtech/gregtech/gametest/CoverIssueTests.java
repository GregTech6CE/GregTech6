package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.machine.*;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.block.energy.*;
import com.gregtech.gregtech.block.machine.*;
import com.gregtech.gregtech.blockentity.energy.SignalWireBlockEntity;
import com.gregtech.gregtech.blockentity.machine.*;
import com.gregtech.gregtech.content.cover.*;
import com.gregtech.gregtech.content.logistics.*;
import com.gregtech.gregtech.content.multiblock.LargeMachineParts;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.item.GTToolItem;
import com.gregtech.gregtech.registry.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

/** Finite native regression set for issues 19-24; registered blocks, held tools and harvest path. */
@GameTestHolder("gregtech_cover_issues") @PrefixGameTestTemplate(false)
public final class CoverIssueTests {
    private static BlockPos site(int x) { return new BlockPos(113200+x,180,113200); }
    private static net.minecraft.world.level.block.entity.BlockEntity place(GameTestHelper h,BlockPos pos,Block block) {
        h.getLevel().getChunkAt(pos);
        h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos,block.defaultBlockState());
        return h.getLevel().getBlockEntity(pos);
    }
    private static ItemStack cover(String id) { return new ItemStack(GTTechnological.get(id)); }
    private static ItemStack tool(GTToolType kind) {
        return GTToolItem.create(kind,com.gregtech.gregtech.content.material.Materials.Steel,
                com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Wood"));
    }
    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper h,BlockPos pos) {
        var profile=new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"CoverIssueTest");
        var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false);
        var player=new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(),h.getLevel(),profile,cookie.clientInformation());
        // Exercise the real menu/harvest implementation; no login or third-party payload negotiation.
        player.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(h.getLevel().getServer(),
                new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),player,cookie){
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet){}
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet,net.minecraft.network.PacketSendListener listener){}
        };
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setPos(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5);return player;
    }
    private static void clearOldItems(GameTestHelper h,BlockPos pos){
        for(var entity:h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(3)))entity.discard();
        for(var entity:h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class,new AABB(pos).inflate(3)))entity.discard();
    }
    private static BlockHitResult hit(BlockPos pos,Direction side) { return new BlockHitResult(Vec3.atCenterOf(pos),side,pos,false); }
    private static PanelCoverHost host(GameTestHelper h,BlockPos pos,String id) {
        return (PanelCoverHost)place(h,pos,BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gregtech",id)));
    }
    private static BasicMachineBlockEntity machine(GameTestHelper h,int x) {
        return (BasicMachineBlockEntity)place(h,site(x),BuiltInRegistries.BLOCK.stream().filter(b->b instanceof BasicMachineBlock).findFirst().orElseThrow());
    }

    @GameTest(template="test_empty",timeoutTicks=60)
    public static void coreTickInputAndDisplayDirection(GameTestHelper h) {
        var core=(LogisticsCoreControllerBlockEntity)place(h,site(0),BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gregtech","logistics_core")));
        var front=core.getBlockState().getValue(LogisticsCoreControllerBlock.FACING);
        for(var cell:LogisticsCoreStructure.cells()) {
            int id=switch(cell.kind()){case CPU->LogisticsCoreStructure.VERSATILE;case VENT->LogisticsCoreStructure.VENT;case WALL->LogisticsCoreStructure.WALL;};
            place(h,cell.at(core.getBlockPos(),front),LargeMachineParts.block(id));
        }
        h.assertTrue(core.isStructureOk()&&core.processorCounts().fixedEnergyPerTick()==128,"27 CPUs form with 128 EU/t standby");
        for(var side:Direction.values())h.assertTrue(core.isEnergyAcceptingFrom(GregTechTags.Energy.EU,side,false)
                &&core.getEnergySizeInputMin(GregTechTags.Energy.EU,side)==256
                &&core.getEnergySizeInputMax(GregTechTags.Energy.EU,side)==1024,"controller input on "+side);
        core.doEnergyInjection(GregTechTags.Energy.EU,Direction.UP,512,20,true);
        long before=core.storedEU();
        for(int i=0;i<20;i++)LogisticsCoreControllerBlockEntity.serverTick(h.getLevel(),core.getBlockPos(),core.getBlockState(),core);
        h.assertTrue(core.storedEU()==before-2560,"standby charges on all ticks, not one second pass");
        var wallCell=LogisticsCoreStructure.cells().stream().filter(c->c.kind()==LogisticsCoreStructure.Kind.WALL).findFirst().orElseThrow();
        var wall=(MultiblockPortBlockEntity)h.getLevel().getBlockEntity(wallCell.at(core.getBlockPos(),front));
        var wire=(com.gregtech.gregtech.blockentity.machine.LogisticsWireBlockEntity)place(h,site(10),BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gregtech","logistics_wire")));
        for(var target:new LogisticsCoverHost[]{core,wall,wire}) {
            h.assertTrue(target.logisticsCovers().attach(Direction.UP,cover(LogisticsCoverType.CPU_LOGIC.id())),"CPU display attaches");
            target.logisticsCovers().setDisplay(Direction.UP,2,4);
            var pos=((net.minecraft.world.level.block.entity.BlockEntity)target).getBlockPos();
            h.assertTrue(h.getLevel().getSignal(pos,Direction.DOWN)==target.logisticsCovers().displaySignal(Direction.UP)
                    &&h.getLevel().getSignal(pos,Direction.UP)==0
                    &&h.getLevel().getDirectSignal(pos,Direction.DOWN)>0,"display emits toward attached face, including strong power");
        }
        h.getLevel().setBlockAndUpdate(wall.getBlockPos(),Blocks.AIR.defaultBlockState());before=core.storedEU();
        LogisticsCoreControllerBlockEntity.serverTick(h.getLevel(),core.getBlockPos(),core.getBlockState(),core);
        h.assertTrue(core.storedEU()==before-20,"unformed controller still charges standby");h.succeed();
    }

    @GameTest(template="test_empty",timeoutTicks=40)
    public static void logisticsStorageAndCoveredWireStayDisconnected(GameTestHelper h) {
        var tank=place(h,site(30),BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gregtech","logistics_tank")));
        var mass=place(h,site(34),BuiltInRegistries.BLOCK.stream().filter(b->b instanceof com.gregtech.gregtech.block.inventory.LogisticsMassStorageBlock).findFirst().orElseThrow());
        for(var owner:new net.minecraft.world.level.block.entity.BlockEntity[]{tank,mass}) {
            var logistics=(LogisticsCoverHost)owner;
            h.assertTrue(logistics.logisticsCovers().attach(Direction.NORTH,cover(LogisticsCoverType.GENERIC_STORAGE.id())),"logistics storage is an attachment host");
            var restored=owner.saveWithoutMetadata(h.getLevel().registryAccess());owner.loadWithComponents(restored,h.getLevel().registryAccess());
            h.assertTrue(!logistics.logisticsCovers().get(Direction.NORTH).isEmpty(),"storage cover survives native save/load");
            h.assertTrue(!((PanelCoverHost)owner).attachCover(Direction.NORTH,cover("blank_cover")),"face cannot contain two cover families");
        }
        var owner=place(h,site(40),BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gregtech","logistics_wire")));var logistics=(LogisticsCoverHost)owner;
        h.assertTrue(logistics.logisticsCovers().attach(Direction.EAST,cover(LogisticsCoverType.CPU_LOGIC.id())),"display attaches");
        var adjacent=owner.getBlockPos().east();place(h,adjacent,BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gregtech","logistics_wire")));
        h.getLevel().setBlockAndUpdate(adjacent,h.getLevel().getBlockState(adjacent).setValue(com.gregtech.gregtech.api.transport.PipeConnections.propFor(Direction.WEST),true));
        h.assertTrue(!owner.getBlockState().getValue(com.gregtech.gregtech.api.transport.PipeConnections.propFor(Direction.EAST)),"neighbor placement cannot open covered face");
        var player=player(h,owner.getBlockPos());player.setItemInHand(InteractionHand.MAIN_HAND,tool(GTToolType.WIRE_CUTTER));
        h.assertTrue(LogisticsCoverInteraction.use(logistics,h.getLevel(),player,InteractionHand.MAIN_HAND,Direction.EAST).consumesAction()
                &&!owner.getBlockState().getValue(com.gregtech.gregtech.api.transport.PipeConnections.propFor(Direction.EAST)),"held cutter is consumed by covered face");h.succeed();
    }

    @GameTest(template="test_empty",timeoutTicks=40)
    public static void heldToolsConfigureNativeDetectorAndRemovalResets(GameTestHelper h) {
        var m=machine(h,60);var side=Direction.NORTH;var p=player(h,m.getBlockPos());
        h.assertTrue(m.attachCover(side,cover(MachineCoverSpec.PROCESSING.id)),"processing detector attaches");
        for(var kind:new GTToolType[]{GTToolType.SCREWDRIVER,GTToolType.WIRE_CUTTER}) {
            var held=tool(kind);p.setItemInHand(InteractionHand.MAIN_HAND,held);
            h.assertTrue(PanelCoverInteraction.use(m,p,InteractionHand.MAIN_HAND,hit(m.getBlockPos(),side),true).consumesAction(),"held tool enters player interaction path: "+kind);
            h.assertTrue(held.getDamageValue()>0,"configuration applies tool wear");
        }
        h.assertTrue(MachineCoverSpec.inverted(m.getCover(side))&&MachineCoverSpec.strong(m.getCover(side)),"both flags toggled through held tools");
        BasicMachineBlockEntity.serverTick(h.getLevel(),m.getBlockPos(),m.getBlockState(),m);
        h.assertTrue(h.getLevel().getDirectSignal(m.getBlockPos(),Direction.SOUTH)==15,"inverted idle detector emits strong signal from its own face");
        var removed=m.removeCover(side);
        h.assertTrue(!MachineCoverSpec.inverted(removed)&&!MachineCoverSpec.strong(removed),"crowbar item defaults stack with new covers");
        h.assertTrue(m.attachCover(side,cover(PanelCover.STATUS.id)),"status display attaches");m.machineControl(null).setEnabled(false);m.removeCover(side);
        h.assertTrue(m.machineControl(null).enabled(),"status removal restores machine on state");
        h.assertTrue(!m.attachCover(side,cover("integrated_circuit_7")),"processing machine without modes rejects circuit selector");h.succeed();
    }

    @GameTest(template="test_empty",timeoutTicks=40)
    public static void selectorsFallbackSignalsAndLatchedCoverStop(GameTestHelper h) {
        var host=host(h,site(80),"usb_switch");var c=host.coverControl(Direction.NORTH);
        h.assertTrue(host.attachCover(Direction.NORTH,cover("integrated_circuit_7")),"mode-capable USB accepts circuit");
        c.setMode(2);host.panels().beforeTick();h.assertTrue(c.mode()==7,"circuit locks mode each tick");
        host.removeCover(Direction.NORTH);h.assertTrue(c.mode()==0,"selector removal returns mode to zero");
        h.assertTrue(host.attachCover(Direction.UP,cover(PanelCover.EMITTER.id)),"USB accepts emitting panel");host.panels().click(Direction.UP,.125,.125);
        h.assertTrue(h.getLevel().getSignal(host.coverOwner().getBlockPos(),Direction.DOWN)==15,"non-machine host exposes real vanilla redstone");
        host.attachCover(Direction.SOUTH,cover(PanelCover.CONTROLLER.id));host.panels().beforeTick();host.removeCover(Direction.SOUTH);
        host.panels().beforeTick();h.assertTrue(host.panels().stopped(),"removing controller keeps stopped flag while panel remains");
        var saved=host.coverOwner().saveWithoutMetadata(h.getLevel().registryAccess());host.coverOwner().loadWithComponents(saved,h.getLevel().registryAccess());host.panels().beforeTick();
        h.assertTrue(host.panels().stopped(),"latched stop survives native save/load");
        host.removeCover(Direction.UP);h.assertTrue(!host.panels().stopped(),"last cover removal clears stopped flag");h.succeed();
    }

    @GameTest(template="test_empty",timeoutTicks=60)
    public static void heldItemWorkbenchStaysOpenAndHarvestRetainsCover(GameTestHelper h) {
        clearOldItems(h,h.absolutePos(new BlockPos(4,4,4)));
        var m=(BasicMachineBlockEntity)place(h,h.absolutePos(new BlockPos(4,4,4)),BuiltInRegistries.BLOCK.stream().filter(b->b instanceof BasicMachineBlock).findFirst().orElseThrow());var p=player(h,m.getBlockPos());m.attachCover(Direction.NORTH,cover(CoverUtilityBehaviors.CRAFTING_TABLE));
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.APPLE));
        h.assertTrue(UtilityCoverInteraction.use(m,p,InteractionHand.MAIN_HAND,hit(m.getBlockPos(),Direction.NORTH)).consumesAction(),"held item opens workbench cover");
        h.assertTrue(p.containerMenu instanceof net.minecraft.world.inventory.CraftingMenu,"native workbench opens");
        h.runAfterDelay(8,()->{
            h.assertTrue(p.containerMenu instanceof net.minecraft.world.inventory.CraftingMenu&&p.containerMenu.stillValid(p),"workbench remains valid across subsequent ticks");
            m.removeCover(Direction.NORTH);m.attachCover(Direction.NORTH,cover(PanelCover.PROGRESS.id));m.panels().configure(Direction.NORTH,true,false);
            var droppedItem=m.getBlockState().getBlock().asItem();p.closeContainer();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_PICKAXE));
            h.assertTrue(m.getBlockState().canHarvestBlock(h.getLevel(),m.getBlockPos(),p),"actual survival harvest has correct tool");
            h.assertTrue(p.gameMode.destroyBlock(m.getBlockPos()),"native player game mode harvest destroys machine");
            var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(m.getBlockPos()).inflate(2));
            var candidates=new java.util.ArrayList<ItemStack>();for(var entity:drops)candidates.add(entity.getItem());candidates.addAll(p.getInventory().items);
            var machineDrop=candidates.stream().filter(s->s.is(droppedItem)).findFirst().orElseThrow(()->new IllegalStateException("harvest drops="+candidates+", tileDrops="+h.getLevel().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOBLOCKDROPS)));

            var data=machineDrop.getOrDefault(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
            h.assertTrue(data!=null&&data.contains("gt_cover_"+Direction.NORTH.ordinal()),"machine drop retains attached cover data");
            h.assertTrue(drops.stream().noneMatch(e->e.getItem().is(GTTechnological.get(PanelCover.PROGRESS.id))),"retained cover does not also drop separately");
            h.getLevel().setBlockAndUpdate(m.getBlockPos().below(),Blocks.STONE.defaultBlockState());
            p.setItemInHand(InteractionHand.MAIN_HAND,machineDrop.copy());
            var floor=m.getBlockPos().below();var placeHit=new BlockHitResult(Vec3.atCenterOf(floor).add(0,.5,0),Direction.UP,floor,false);
            h.assertTrue(p.getMainHandItem().getItem().useOn(new net.minecraft.world.item.context.UseOnContext(p,InteractionHand.MAIN_HAND,placeHit)).consumesAction(),"harvested machine item actually places again");
            var restored=(PanelCoverHost)h.getLevel().getBlockEntity(m.getBlockPos());
            h.assertTrue(restored!=null&&restored.getCover(Direction.NORTH).is(GTTechnological.get(PanelCover.PROGRESS.id))&&MachineCoverSpec.strong(restored.getCover(Direction.NORTH)),"native item placement restores cover and strong setting");
            h.succeed();
        });
    }

    @GameTest(template="test_empty",timeoutTicks=40)
    public static void wireOnlyTorchAndShuttersGatePipes(GameTestHelper h) {
        var m=machine(h,120);h.assertTrue(!m.attachCover(Direction.NORTH,new ItemStack(Items.REDSTONE_TORCH)),"machine refuses wire torch");
        var wireBlock=GTSignalWires.all().stream().map(r->r.get()).filter(SignalWireBlock::insulated).findFirst().orElseThrow();
        var wire=(SignalWireBlockEntity)place(h,site(124),wireBlock);var host=(PanelCoverHost)(Object)wire;
        h.assertTrue(host.attachCover(Direction.NORTH,new ItemStack(Items.REDSTONE_TORCH))&&host.attachCover(Direction.SOUTH,new ItemStack(Items.REPEATER)),"insulated signal wire accepts both attachments");
        host.panels().afterTick();h.assertTrue(!wire.connected(Direction.NORTH)&&wire.output(Direction.NORTH)==15&&wire.output(Direction.SOUTH)==0,"unpowered wire lights torch and turns repeater off");
        wire.setMode(15);wire.tickNetwork();host.panels().afterTick();
        h.assertTrue(wire.output(Direction.NORTH)==0&&wire.output(Direction.SOUTH)==15,"wire signal, not machine state, drives attachments");
        var bare=(PanelCoverHost)(Object)place(h,site(127),GTSignalWires.all().stream().map(r->r.get()).filter(b->!b.insulated()).findFirst().orElseThrow());
        h.assertTrue(!bare.attachCover(Direction.NORTH,new ItemStack(Items.REDSTONE_TORCH)),"bare wire rejects insulated-only torch");
        var pipe=(FluidPipeBlockEntity)host(h,site(132),"pipe_huge_steel");var side=Direction.EAST;
        h.assertTrue(pipe.attachCover(side,cover(PanelCover.SHUTTER.id)),"fluid pipe accepts shutter");
        pipe.panels().configure(side,false,false);
        h.assertTrue(!pipe.getBlockState().getValue(FluidPipeBlock.propFor(side))&&h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,pipe.getBlockPos(),side).fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER,100),FluidAction.EXECUTE)==0,"closed shutter disconnects face and rejects fluid");
        pipe.panels().configure(side,false,false);h.assertTrue(pipe.getBlockState().getValue(FluidPipeBlock.propFor(side)),"opening shutter reconnects pipe face");h.succeed();
    }

    @GameTest(template="test_empty",timeoutTicks=40)
    public static void itemFiltersIgnoreComponentsAndDecorationsUseHeldChisel(GameTestHelper h) {
        var host=host(h,site(150),"logistics_mass_storage_steel");var p=player(h,host.coverOwner().getBlockPos());
        h.assertTrue(host.attachCover(Direction.NORTH,cover(CoverUtilityBehaviors.FILTER_ITEM)),"inventory host accepts filter");
        var held=new ItemStack(Items.IRON_PICKAXE);held.setDamageValue(9);held.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("fixture"));p.setItemInHand(InteractionHand.MAIN_HAND,held);
        UtilityCoverInteraction.use(host,p,InteractionHand.MAIN_HAND,hit(host.coverOwner().getBlockPos(),Direction.NORTH));
        var filter=host.getCover(Direction.NORTH);var different=new ItemStack(Items.IRON_PICKAXE);different.setDamageValue(10);
        h.assertTrue(!CoverUtilityBehaviors.itemFilter(filter,h.getLevel().registryAccess()).has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)&&!CoverUtilityBehaviors.itemFilterPermits(filter,false,different,h.getLevel().registryAccess()),"specimen omits unrelated data and initially matches exact damage");
        UtilityCoverInteraction.use(host,p,InteractionHand.MAIN_HAND,hit(host.coverOwner().getBlockPos(),Direction.NORTH));
        h.assertTrue(CoverUtilityBehaviors.itemFilterPermits(filter,false,different,h.getLevel().registryAccess()),"repeated matching click enables damage wildcard");
        h.assertTrue(host.attachCover(Direction.UP,cover("warning_cover")),"inventory host accepts warning cover");p.setItemInHand(InteractionHand.MAIN_HAND,tool(GTToolType.CHISEL));
        for(int i=0;i<21;i++)UtilityCoverInteraction.use(host,p,InteractionHand.MAIN_HAND,hit(host.coverOwner().getBlockPos(),Direction.UP));
        h.assertTrue(CoverUtilityBehaviors.design(host.getCover(Direction.UP))==1,"held chisel cycles all twenty original designs");
        host.removeCover(Direction.UP);
        var basaltPlate=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gregtech","plate_basalt")));
        h.assertTrue(!basaltPlate.isEmpty()&&host.attachCover(Direction.UP,basaltPlate),"registered basalt plate accepts decorative cover");
        for(int i=0;i<17;i++)UtilityCoverInteraction.use(host,p,InteractionHand.MAIN_HAND,hit(host.coverOwner().getBlockPos(),Direction.UP));
        h.assertTrue(CoverUtilityBehaviors.design(host.getCover(Direction.UP))==1&&MaterialCoverRules.designCount("plate","Basalt")==16,"stone plate cycles sixteen original masonry designs");
        host.removeCover(Direction.NORTH);
        var coverShape=CoverWorldInteraction.collision(h.getLevel(),host.coverOwner().getBlockPos(),net.minecraft.world.phys.shapes.Shapes.empty());
        h.assertTrue(coverShape.min(Direction.Axis.Y)==.875&&coverShape.max(Direction.Axis.Y)==1
                &&host.coverOwner().getBlockState().getCollisionShape(h.getLevel(),host.coverOwner().getBlockPos()).max(Direction.Axis.Y)==1,"two-pixel plate occupies original in-block bounds");
        var thin=(ItemPipeBlockEntity)place(h,site(158),GTItemPipes.PIPE_MEDIUM_BRASS.get());
        h.assertTrue(thin.getBlockState().getCollisionShape(h.getLevel(),thin.getBlockPos()).max(Direction.Axis.Y)<1,"uncovered native pipe is narrow");
        h.assertTrue(thin.attachCover(Direction.UP,cover("blank_cover"))&&thin.getBlockState().getCollisionShape(h.getLevel(),thin.getBlockPos()).max(Direction.Axis.Y)==1,"native cached collision incorporates cover on thin pipe");
        h.assertTrue(UtilityCoverInteraction.sample(new ItemStack(Items.WATER_BUCKET)).getFluid()==net.minecraft.world.level.material.Fluids.WATER,"vanilla container sampling returns fluid");h.succeed();
    }
    @GameTest(template="test_empty",timeoutTicks=60)
    public static void drainBulkExperienceWalkAndFilterConnectionBoundary(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(4,4,4));clearOldItems(h,pos);var tank=(TankBlockEntity)place(h,pos,BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gregtech","drum_steel")));
        h.assertTrue(tank.attachCover(Direction.UP,cover(CoverAttachmentBehaviors.DRAIN)),"ticking tank accepts drain");
        var sea=GTFluids.still("Ocean").get();var front=pos.east();
        h.getLevel().setBlockAndUpdate(front,sea.defaultFluidState().createLegacyBlock());
        h.assertTrue(CoverAttachmentBehaviors.tickDrain(h.getLevel(),pos,Direction.EAST,tank,5)
                &&tank.getFluidInTank(0).getAmount()==16000&&!h.getLevel().getFluidState(front).isEmpty(),"bulk water yields 16000 and retains world source");
        tank.removeCover(Direction.UP);tank.drain(Integer.MAX_VALUE,FluidAction.EXECUTE);h.getLevel().setBlockAndUpdate(front,Blocks.AIR.defaultBlockState());
        var orb=new net.minecraft.world.entity.ExperienceOrb(h.getLevel(),pos.getX()+2.5,pos.getY()+.5,pos.getZ()+.5,5);
        h.assertTrue(h.getLevel().addFreshEntity(orb),"XP fixture enters native entity manager");
        h.runAfterDelay(2,()->{
        var nearby=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class,new AABB(pos.relative(Direction.EAST,2)).inflate(1));
        int accepts=tank.fill(new FluidStack(GTFluids.still("XP").get(),100),FluidAction.SIMULATE);
        boolean collected=CoverAttachmentBehaviors.tickDrain(h.getLevel(),pos,Direction.EAST,tank,50);
        h.assertTrue(collected&&orb.isRemoved()&&tank.getFluidInTank(0).getFluid()==GTFluids.still("XP").get()&&tank.getFluidInTank(0).getAmount()==100,
                "XP orb becomes original twenty mB per point: entities="+nearby.size()+", accepts="+accepts+", collected="+collected+", removed="+orb.isRemoved()+", value="+orb.getValue()+", count="+((com.gregtech.gregtech.mixin.ExperienceOrbCountAccessor)orb).gregtech$count()+", fluid="+tank.getFluidInTank(0));
        var pipe=(ItemPipeBlockEntity)place(h,site(182),GTItemPipes.PIPE_MEDIUM_BRASS.get());
        var receiver=(ItemPipeBlockEntity)place(h,site(183),GTItemPipes.PIPE_MEDIUM_BRASS.get());
        h.getLevel().setBlockAndUpdate(pipe.getBlockPos(),pipe.getBlockState().setValue(ItemPipeBlock.EAST,true));
        h.assertTrue(!pipe.attachCover(Direction.EAST,cover(CoverUtilityBehaviors.FILTER_ITEM))
                &&!pipe.getBlockState().getValue(ItemPipeBlock.EAST),"item filter rejects and disconnects pipe-to-pipe face");
        place(h,pipe.getBlockPos().north(),Blocks.CHEST);
        h.getLevel().setBlockAndUpdate(pipe.getBlockPos(),pipe.getBlockState().setValue(ItemPipeBlock.NORTH,true));
        h.assertTrue(pipe.attachCover(Direction.NORTH,cover(CoverUtilityBehaviors.FILTER_ITEM))
                &&pipe.getBlockState().getValue(ItemPipeBlock.NORTH),"filter preserves connection to ordinary inventory");
        tank.attachCover(Direction.UP,cover(CoverAttachmentBehaviors.DRAIN));
        int delay=(5-(int)(h.getLevel().getGameTime()%20)+20)%20;if(delay==0)delay=20;
        h.runAfterDelay(delay,()->{
            tank.drain(Integer.MAX_VALUE,FluidAction.EXECUTE);
            var squid=net.minecraft.world.entity.EntityType.SQUID.create(h.getLevel());
            tank.getBlockState().getBlock().stepOn(h.getLevel(),pos,tank.getBlockState(),squid);
            h.assertTrue(tank.getFluidInTank(0).getFluid()==GTFluids.still("InkSquid").get()&&tank.getFluidInTank(0).getAmount()==1,"native step-on dispatch produces squid ink");
            h.succeed();
        });
        });
    }

}
