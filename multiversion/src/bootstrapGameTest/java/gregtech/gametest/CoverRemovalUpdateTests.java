package gregtech.gametest;

import gregtech.block.energy.SignalWireBlock;
import gregtech.content.cover.CoverWorldInteraction;
import gregtech.content.cover.PanelCoverHost;
import gregtech.registry.GTSignalWires;
import gregtech.registry.GTTechnological;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Native item/crowbar entrypoints: removal must trigger vanilla support and normal drops. */
@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class CoverRemovalUpdateTests {
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private record Saved(BlockState state, CompoundTag data) {}
    private static SignalWireBlock wire(boolean insulated) {
        return GTSignalWires.all().stream().map(r -> r.get())
                .filter(b -> b.material().getName().equals("RedAlloy") && b.insulated()==insulated).findFirst().orElseThrow();
    }
    private static final class Fixture implements AutoCloseable {
        final GameTestHelper h;
        final BlockPos pos;
        final ServerPlayer player;
        final LinkedHashMap<BlockPos,Saved> saved = new LinkedHashMap<>();
        Fixture(GameTestHelper h,int index) {this(h,index,true);}
        Fixture(GameTestHelper h,int index,boolean insulated) {
            this.h=h; pos=h.absolutePos(new BlockPos(4,4,4));
            var templatePositions=new java.util.HashSet<BlockPos>();
            h.forEveryBlockInStructure(at->templatePositions.add(h.absolutePos(at)));
//? if >=1.21.1 {
/*            player=net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),
*///?} else {
            player=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),
//?}
                    new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"[gt-cover-solid]"));
            player.setGameMode(GameType.SURVIVAL); player.getInventory().clearContent(); player.setShiftKeyDown(false);
            for(var mutable:BlockPos.betweenClosed(pos.offset(-2,-2,-2),pos.offset(2,2,2))) {
                var at=mutable.immutable();
                h.assertTrue(templatePositions.contains(at),"drop fixture must fit its own dedicated template: "+at);
                h.assertTrue(h.getLevel().areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(at))
                        &&h.getLevel().isPositionEntityTicking(at),"native entity-drop fixture chunk must be loaded and ticking: "+at);
                var be=h.getLevel().getBlockEntity(at);
//? if >=1.21.1 {
/*                var data=be==null?null:be.saveWithoutMetadata(h.getLevel().registryAccess());
*///?} else {
                var data=be==null?null:be.saveWithoutMetadata();
//?}
                saved.put(at,new Saved(h.getLevel().getBlockState(at),data));
            }
            saved.keySet().forEach(at->set(at,Blocks.AIR.defaultBlockState()));
            set(pos,wire(insulated).defaultBlockState());
        }
        void set(BlockPos at,BlockState state) {
            h.getLevel().removeBlockEntity(at); h.getLevel().setBlock(at,Blocks.AIR.defaultBlockState(),FLAGS);
            h.getLevel().setBlock(at,state,FLAGS);
        }
        PanelCoverHost host(){return (PanelCoverHost)h.getLevel().getBlockEntity(pos);}
        void click(Direction side,ItemStack stack) {
            player.setItemInHand(InteractionHand.MAIN_HAND,stack);
            var normal=Vec3.atLowerCornerOf(side.getNormal()); var point=Vec3.atCenterOf(pos).add(normal.scale(.5));
            var stand=Vec3.atCenterOf(pos).add(normal.scale(2.5));player.setPos(stand.x,stand.y,stand.z);
            player.gameMode.useItemOn(player,h.getLevel(),stack,InteractionHand.MAIN_HAND,new BlockHitResult(point,side,pos,false));
        }
        @Override public void close() {
            for(var entity:h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(pos).inflate(3)))entity.discard();
            player.closeContainer();player.getInventory().clearContent();player.discard();
            saved.keySet().forEach(at->set(at,Blocks.AIR.defaultBlockState()));
            for(var entry:saved.entrySet()) {
                set(entry.getKey(),entry.getValue().state());var be=h.getLevel().getBlockEntity(entry.getKey());
                if(be!=null&&entry.getValue().data()!=null) {
//? if >=1.21.1 {
/*                    be.loadWithComponents(entry.getValue().data(),h.getLevel().registryAccess());
*///?} else {
                    be.load(entry.getValue().data());
//?}
                }
            }
        }
    }
    private static ItemStack crowbar() {
        return gregtech.item.GTToolItem.create(gregtech.internal.tool.GTToolType.CROWBAR,
                gregtech.content.material.Materials.Steel,
                gregtech.internal.material.GTMaterialRegistry.get("Wood"));
    }
    private static int drops(Fixture f,net.minecraft.world.item.Item item) {
        return f.h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(f.pos).inflate(3)).stream()
                .filter(e->e.getItem().is(item)).mapToInt(e->e.getItem().getCount()).sum();
    }
    private static String dropDiagnostic(Fixture f) {
        var level=f.h.getLevel();
        var control=f.pos.above(2);
        f.set(control.below(),Blocks.STONE.defaultBlockState());
        var lever=Blocks.LEVER.defaultBlockState().setValue(
                net.minecraft.world.level.block.state.properties.BlockStateProperties.ATTACH_FACE,
                net.minecraft.world.level.block.state.properties.AttachFace.FLOOR);
        f.set(control,lever);
        boolean survives=lever.canSurvive(level,control);
        var loot=Block.getDrops(lever,level,control,null);
        int before=drops(f,Items.LEVER);
        boolean destroyed=level.destroyBlock(control,true);
        int after=drops(f,Items.LEVER);
        var marker=new net.minecraft.world.entity.item.ItemEntity(level,f.pos.getX()+.5,f.pos.getY()+.5,f.pos.getZ()+.5,new ItemStack(Items.STICK));
        boolean added=level.addFreshEntity(marker);
        boolean tracked=level.getEntity(marker.getId())==marker;
        int visibleMarkers=drops(f,Items.STICK);
        marker.discard();
        return "tileDrops="+level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOBLOCKDROPS)
                +", entitiesLoaded="+level.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(f.pos))
                +", entityTicking="+level.isPositionEntityTicking(f.pos)+", controlSurvives="+survives
                +", controlLoot="+loot+", controlDestroyed="+destroyed+", controlDropDelta="+(after-before)
                +", markerAdded="+added+", markerTracked="+tracked+", markerVisible="+visibleMarkers;
    }
    private static void exercise(GameTestHelper h,Fixture f,Direction side,boolean retain,net.minecraft.world.item.Item cover) {
        for(var entity:h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(f.pos).inflate(3)))entity.discard();
        h.assertTrue(h.getLevel().getBlockState(f.pos).isFaceSturdy(h.getLevel(),f.pos,side)==retain,
                "initial underlying host support must match thin/full fixture: "+side);
        f.click(side,new ItemStack(cover,2));
        h.assertTrue(f.host().getCover(side).is(cover),"native attachment: "+side);
        f.click(side,new ItemStack(Items.LEVER,2));
        var at=f.pos.relative(side);
        h.assertTrue(h.getLevel().getBlockState(at).is(Blocks.LEVER)
                &&h.getLevel().getBlockState(at).canSurvive(h.getLevel(),at)
                &&f.player.getMainHandItem().getCount()==1,"native lever placement on "+side);
        var hostState=h.getLevel().getBlockState(f.pos);
        f.click(side,crowbar());
        h.assertTrue(f.host().getCover(side).isEmpty(),"actual crowbar removes selected cover");
        h.assertTrue(f.player.getInventory().countItem(cover)==1,"exactly one removed cover returned to survival inventory");
        h.assertTrue(h.getLevel().getBlockState(at).is(retain?Blocks.LEVER:Blocks.AIR),
                "crowbar must automatically update neighbor support; retained="+retain+", side="+side);
        int actualDrops=drops(f,Items.LEVER);
        if(actualDrops!=(retain?0:1))h.assertTrue(false,
                "vanilla support loss drops exactly one lever, not cover or host; actual="+actualDrops+", side="+side+", "+dropDiagnostic(f));
        h.assertTrue(drops(f,cover)==0&&h.getLevel().getBlockState(f.pos)==hostState,"host and returned-cover preservation");
        h.assertTrue(f.host().removeCover(side).isEmpty()&&drops(f,Items.LEVER)==(retain?0:1),"repeated empty removal cannot duplicate drops");
    }
    /** GameTest loads template chunks asynchronously; wait for observable native readiness before any fixture mutation.
     * thenExecute runs the interaction matrix once, so a failed gameplay assertion is never retried into a pass.
     * The existing timeoutTicks=100 bounds this readiness wait and the whole test.
     */
    private static void whenFixtureChunksReady(GameTestHelper h,Runnable matrix) {
        h.startSequence().thenWaitUntil(()->{
            var center=h.absolutePos(new BlockPos(4,4,4));
            for(var at:BlockPos.betweenClosed(center.offset(-2,-2,-2),center.offset(2,2,2))) {
                h.assertTrue(h.getLevel().areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(at))
                        &&h.getLevel().isPositionEntityTicking(at),
                        "waiting for native entity-drop fixture chunk to load and tick: "+at);
            }
        }).thenExecute(matrix).thenSucceed();
    }
    @GameTest(template="cover_removal_empty",timeoutTicks=100)
    public static void crowbarRemovalDetachesNativeLeverOnEveryThinWireFace(GameTestHelper h) {
        whenFixtureChunksReady(h,()->{
            for(boolean insulated:new boolean[]{false,true})for(var side:Direction.values())
                try(var f=new Fixture(h,side.ordinal()+(insulated?6:0),insulated)) {
                    exercise(h,f,side,false,GTTechnological.get("blank_cover"));
                }
        });
    }
    @GameTest(template="cover_removal_empty",timeoutTicks=100)
    public static void crowbarRemovalDistinguishesMediumAndFullDiameterPipeSupport(GameTestHelper h) {
        whenFixtureChunksReady(h,()->{
            for(boolean huge:new boolean[]{false,true})for(var side:Direction.values())
                try(var f=new Fixture(h,20+side.ordinal()+(huge?6:0))) {
                    var pipe=net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6ce",huge?"pipe_huge_steel":"pipe_medium_steel"));
                    h.assertTrue(pipe instanceof gregtech.block.machine.FluidPipeBlock,"registered pipe fixture");
                    f.set(f.pos,pipe.defaultBlockState());
                    exercise(h,f,side,huge,GTTechnological.get("blank_cover"));
                }
        });
    }
    @GameTest(template="cover_removal_empty",timeoutTicks=100)
    public static void removingNonsolidValvePreservesUnderlyingFullHostSupport(GameTestHelper h) {
        whenFixtureChunksReady(h,()->{
            for(var side:Direction.values())try(var f=new Fixture(h,40+side.ordinal())) {
                var pipe=net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6ce","pipe_huge_steel"));
                f.set(f.pos,pipe.defaultBlockState());
                exercise(h,f,side,true,GTTechnological.get(gregtech.content.cover.CoverUtilityBehaviors.PRESSURE_VALVE));
            }
        });
    }
}
