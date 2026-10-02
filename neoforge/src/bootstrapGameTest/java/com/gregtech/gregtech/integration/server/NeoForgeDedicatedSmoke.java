package com.gregtech.gregtech.integration.server;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.slf4j.Logger;

/** Opt-in ordinary dedicated-server world restart smoke, excluded from the mod jar. */
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID, value = Dist.DEDICATED_SERVER,
        bus = EventBusSubscriber.Bus.GAME)
public final class NeoForgeDedicatedSmoke {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String PLATFORM = "neoforge";
    private static final String MINECRAFT_VERSION = "1.21.1";
    private static final String PHASE = System.getProperty("gregtech.integration.serverSmokePhase", "");
    private static final boolean ENABLED = !PHASE.isEmpty();
    private static final boolean SMELTERY_WORLD = Boolean.getBoolean("gregtech.integration.smelteryWorldSmoke");
    private static final boolean MACHINE_WORLD = Boolean.getBoolean("gregtech.integration.machineWorldSmoke");
    private static final boolean STEAM_CHAIN = Boolean.getBoolean("gregtech.integration.steamChainSmoke");
    private static final boolean DIESEL_POWER = Boolean.getBoolean("gregtech.integration.dieselPowerSmoke");
    private static final boolean DIESEL_BASELINE = Boolean.getBoolean("gregtech.integration.dieselPowerBaseline");
    private static final boolean DIESEL_READ_ONLY = Boolean.getBoolean("gregtech.integration.dieselPowerReadOnly");
    private static boolean dieselCycleObserved;
    private static final boolean EARLY_TOOL_CHAIN = Boolean.getBoolean("gregtech.integration.earlyToolChainSmoke");
    private static final String SESSION_ID = UUID.randomUUID().toString();
    private static final int REQUIRED_TICKS = STEAM_CHAIN && "prepare".equals(PHASE) ? 12000 : 200;
    private static final AtomicBoolean TERMINAL = new AtomicBoolean();
    private static volatile MinecraftServer activeServer;
    private static volatile ScheduledExecutorService watchdog;
    private static volatile long startedAt;
    private static volatile int observedTicks;
    private static volatile String specimenId;
    private static volatile BlockPos specimenPos;
    private static volatile Path worldRoot;
    // Only the server thread writes these lifecycle flags.
    private static boolean worldReadVerified;
    private static boolean stopRequested;
    private static boolean normalStoppingObserved;

    private NeoForgeDedicatedSmoke() {}
    private static final boolean ENGINE_CRAFTING = Boolean.getBoolean("gregtech.integration.engineCraftingSmoke");
    private static java.util.Map<String,ItemStack> craftedSteamResults = java.util.Map.of();

    @SubscribeEvent
    public static void serverStarted(ServerStartedEvent event) {
        if (!ENABLED) return;
        activeServer = event.getServer();
        startedAt = System.nanoTime();
        try {
            if (!(activeServer instanceof DedicatedServer) || activeServer instanceof GameTestServer) {
                throw new IllegalStateException("Smoke requires an ordinary DedicatedServer, not GameTestServer");
            }
            if (!"prepare".equals(PHASE) && !"verify".equals(PHASE)) {
                throw new IllegalArgumentException("serverSmokePhase must be prepare or verify");
            }
            String configuredId = System.getProperty("gregtech.integration.serverSmokeId");
            if (configuredId == null || !UUID.fromString(configuredId).toString().equals(configuredId)) {
                throw new IllegalArgumentException("serverSmokeId must be an explicitly supplied canonical UUID");
            }
            specimenId = configuredId;
            worldRoot = activeServer.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
            ServerLevel level = activeServer.overworld();
            specimenPos = position(level);
            watchdog = Executors.newSingleThreadScheduledExecutor(task -> {
                Thread thread = new Thread(task, "gregtech-neoforge-dedicated-smoke-watchdog");
                thread.setDaemon(true);
                return thread;
            });
            watchdog.schedule(() -> fail("timeout", new TimeoutException(
                    "No completed dedicated phase and stop within 120 seconds of ServerStarted")),
                    120, TimeUnit.SECONDS);
            LOGGER.info("SERVER_SMOKE_STARTED {}", identity());
            if (ENGINE_CRAFTING) craftedSteamResults = NeoManualToolCheckpoint.steamEngineCrafting(level);
            if (Boolean.getBoolean("gregtech.integration.electricFluxCraftingSmoke")) NeoManualToolCheckpoint.electricFluxCrafting(level);
            if (Boolean.getBoolean("gregtech.integration.dieselCraftingSmoke")) NeoManualToolCheckpoint.dieselCrafting(level);
            // Explicitly load the real overworld chunk; no synthetic NBT round trip.
            level.getChunk(specimenPos);
            level.getChunk(specimenPos.east());
            if ("prepare".equals(PHASE)) {
                prepare(level);
                verifyWorld(level);
                LOGGER.info("SERVER_SMOKE_PREPARED {}", identity());
                if (STEAM_CHAIN) {
                    // Sprint ordinary world ticks; do not inject heat/KU or manually tick machines.
                    activeServer.getCommands().performPrefixedCommand(activeServer.createCommandSourceStack(),
                            "tick sprint " + REQUIRED_TICKS);
                }
            } else {
                // Never create/repair the specimen in verify: it must have loaded from disk.
                verifyWorld(level);
                if (DIESEL_POWER) readDieselPower(level);
                if (EARLY_TOOL_CHAIN) verifyEarlyTools(level);
                worldReadVerified = true;
                LOGGER.info("SERVER_SMOKE_WORLD_VERIFIED {}", identity());
            }
        } catch (Throwable failure) {
            fail("start_or_world_check", failure);
        }
    }

    private static BlockPos position(ServerLevel level) {
        String x = System.getProperty("gregtech.integration.serverSmokeX");
        String y = System.getProperty("gregtech.integration.serverSmokeY");
        String z = System.getProperty("gregtech.integration.serverSmokeZ");
        BlockPos pos;
        if (x == null && y == null && z == null) {
            pos = level.getSharedSpawnPos().offset(8, 12, 8);
        } else {
            if (x == null || y == null || z == null) {
                throw new IllegalArgumentException("Supply all serverSmokeX/Y/Z coordinates together");
            }
            pos = new BlockPos(Integer.parseInt(x), Integer.parseInt(y), Integer.parseInt(z));
        }
        if (Math.abs((long) pos.getX()) >= 29999983 || Math.abs((long) pos.getZ()) >= 29999983
                || level.isOutsideBuildHeight(pos) || level.isOutsideBuildHeight(pos.east())) {
            throw new IllegalArgumentException("Specimen coordinates are outside valid world bounds");
        }
        return pos.immutable();
    }

    private static void prepare(ServerLevel level) {
        if (!level.getBlockState(specimenPos).isAir() || !level.getBlockState(specimenPos.east()).isAir()) {
            throw new IllegalStateException("Prepare refuses to overwrite non-air blocks; use a fresh isolated world/position");
        }
        if (!level.setBlock(specimenPos, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL)
                || !level.setBlock(specimenPos.east(), Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL)) {
            throw new IllegalStateException("Could not place the real chest and adjacent dirt");
        }
        if (!(level.getBlockEntity(specimenPos) instanceof ChestBlockEntity chest)) {
            throw new IllegalStateException("Placed chest has no ChestBlockEntity");
        }
        // 1.21.1 names are applied through vanilla item components, not the removed setter.
        ItemStack namedChest = new ItemStack(Items.CHEST);
        namedChest.set(DataComponents.CUSTOM_NAME, Component.literal("gregtech-dedicated-smoke/" + specimenId));
        chest.applyComponentsFromItemStack(namedChest);
        chest.setItem(0, new ItemStack(Items.COPPER_INGOT, 3));
        chest.setItem(1, new ItemStack(Items.IRON_INGOT, 1));
        if (ENGINE_CRAFTING) {
            chest.setItem(2, craftedSteamResults.get("engine_steam_bronze").copy());
            chest.setItem(3, craftedSteamResults.get("engine_steam_strong_bronze").copy());
        }
        chest.setChanged();
        if (SMELTERY_WORLD) prepareSmeltery(level);
        if (MACHINE_WORLD) prepareMachines(level);
        if (STEAM_CHAIN) prepareSteamChain(level);
        if (DIESEL_POWER) prepareDieselPower(level);
        if (EARLY_TOOL_CHAIN) prepareEarlyTools(level);
    }

    private static void verifyWorld(ServerLevel level) {
        if (!level.getBlockState(specimenPos).is(Blocks.CHEST)
                || level.getBlockState(specimenPos).getValue(ChestBlock.TYPE) != ChestType.SINGLE
                || !level.getBlockState(specimenPos.east()).is(Blocks.DIRT)) {
            throw new IllegalStateException("Real chest block/type or adjacent dirt does not match specimen");
        }
        if (!(level.getBlockEntity(specimenPos) instanceof ChestBlockEntity chest)
                || chest.getContainerSize() != 27 || chest.getCustomName() == null
                || !("gregtech-dedicated-smoke/" + specimenId).equals(chest.getCustomName().getString())) {
            throw new IllegalStateException("Chest entity, inventory size or persisted specimen UUID does not match");
        }
        if (!chest.getItem(0).is(Items.COPPER_INGOT) || chest.getItem(0).getCount() != 3
                || !chest.getItem(1).is(Items.IRON_INGOT) || chest.getItem(1).getCount() != 1) {
            throw new IllegalStateException("Real chest inventory is not 3 copper ingots plus 1 iron ingot");
        }
        if (ENGINE_CRAFTING) {
            if (!ItemStack.isSameItemSameComponents(chest.getItem(2), craftedSteamResults.get("engine_steam_bronze"))
                    || chest.getItem(2).getCount()!=1
                    || !ItemStack.isSameItemSameComponents(chest.getItem(3), craftedSteamResults.get("engine_steam_strong_bronze"))
                    || chest.getItem(3).getCount()!=1)
                throw new IllegalStateException("Real persisted steam crafting results differ");
        }
        for (int slot = EARLY_TOOL_CHAIN ? 5 : ENGINE_CRAFTING ? 4 : 2; slot < chest.getContainerSize(); slot++) {
            if (!chest.getItem(slot).isEmpty()) {
                throw new IllegalStateException("Unexpected item in specimen chest slot " + slot);
            }
        }
        if (EARLY_TOOL_CHAIN && observedTicks >= REQUIRED_TICKS) verifyEarlyTools(level);
        if (SMELTERY_WORLD) verifySmeltery(level);
        if (MACHINE_WORLD) verifyMachines(level);
        if (STEAM_CHAIN && ("verify".equals(PHASE) || observedTicks >= REQUIRED_TICKS)) verifySteamChain(level);
        if (DIESEL_POWER && observedTicks >= REQUIRED_TICKS) finishDieselPower(level);
    }

    private static ItemStack takeEarly(net.minecraft.server.level.ServerPlayer actor,net.minecraft.world.item.Item item,int count) {
        var result=new ItemStack(item,count);int left=count;
        for(int slot=0;slot<actor.getInventory().getContainerSize()&&left>0;slot++) {
            var stock=actor.getInventory().getItem(slot);if(!stock.is(item))continue;
            int amount=Math.min(left,stock.getCount());stock.shrink(amount);left-=amount;
        }
        if(left!=0)throw new IllegalStateException("Missing actually collected early material "+item);
        return result;
    }

    private static void prepareEarlyTools(ServerLevel level) {
        try {
        var base=specimenPos.south(6);
        var actor=net.neoforged.neoforge.common.util.FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(UUID.fromString(specimenId),"[gt-early]"));
        actor.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);actor.getInventory().clearContent();actor.getInventory().selected=8;
        if(actor.getAbilities().instabuild)throw new IllegalStateException("Early actor must be survival");
        for(int offset=0;offset<9;offset++) {
            var pos=base.east(offset);level.getChunk(pos);
            if(!level.getBlockState(pos).isAir()||!level.getBlockState(pos.below()).isAir())throw new IllegalStateException("Early specimens refuse overwrite");
            level.setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());
        }
        for(int i=0;i<4;i++) {
            var pos=base.east(i);level.setBlockAndUpdate(pos,i==3?com.gregtech.gregtech.registry.GTBlocks.TWIGS.get().defaultBlockState():com.gregtech.gregtech.registry.GTBlocks.ROCK.get().defaultBlockState());
            if(i<3)((com.gregtech.gregtech.blockentity.RockBlockEntity)level.getBlockEntity(pos)).setItemId("minecraft:flint");
            actor.setPos(pos.getX()+0.5,pos.getY(),pos.getZ()+1.5);actor.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),net.minecraft.core.Direction.UP,pos,false);
            actor.gameMode.useItemOn(actor,level,ItemStack.EMPTY,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
            if(!level.getBlockState(pos).isAir())throw new IllegalStateException("Actual right-click did not collect rock/twig");
        }
        var bench=base.east(4);level.setBlockAndUpdate(bench,Blocks.CRAFTING_TABLE.defaultBlockState());
        var menu=new net.minecraft.world.inventory.CraftingMenu(0,actor.getInventory(),net.minecraft.world.inventory.ContainerLevelAccess.create(level,bench));
        actor.containerMenu=menu;
        for(int x=0;x<3;x++)menu.getSlot(1+x).set(takeEarly(actor,Items.FLINT,1));
        menu.getSlot(5).set(takeEarly(actor,Items.STICK,1));
        menu.clicked(0,0,net.minecraft.world.inventory.ClickType.PICKUP,actor);
        var pick=menu.getCarried().copy();
        if(!com.gregtech.gregtech.api.tool.GTToolHelper.isUsable(pick)||com.gregtech.gregtech.api.tool.GTToolHelper.getType(pick)!=com.gregtech.gregtech.api.tool.GTToolType.PICKAXE||com.gregtech.gregtech.api.tool.GTToolHelper.getHead(pick)!=com.gregtech.gregtech.content.material.Materials.Flint||com.gregtech.gregtech.api.tool.GTToolHelper.getHandle(pick)!=com.gregtech.gregtech.content.material.generated.WoodMaterials.Wood)throw new IllegalStateException("Actual early crafting result slot did not produce Flint/Wood pick "+pick);
        for(int slot=1;slot<=9;slot++)if(!menu.getSlot(slot).getItem().isEmpty())throw new IllegalStateException("Actual result-slot click did not consume early grid");
        menu.setCarried(ItemStack.EMPTY);actor.containerMenu=actor.inventoryMenu;
        actor.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,pick);int before=pick.getDamageValue();
        for(int i=0;i<4;i++) {
            var material=i==3?com.gregtech.gregtech.content.material.Materials.Tin:com.gregtech.gregtech.content.material.Materials.Copper;
            var pos=base.east(5+i);var ore=com.gregtech.gregtech.registry.GTBlocks.getObject(com.gregtech.gregtech.api.prefix.BlockMaterialPrefix.ore,material);
            if(ore==null)throw new IllegalStateException("Missing registered early ore "+material);
            level.setBlockAndUpdate(pos,ore.get().defaultBlockState());actor.setPos(pos.getX()+0.5,pos.getY(),pos.getZ()+1.5);
            if(!pick.isCorrectToolForDrops(level.getBlockState(pos))||!actor.gameMode.destroyBlock(pos)||!level.getBlockState(pos).isAir())throw new IllegalStateException("Crafted flint pick cannot harvest actual ore "+material);
            var raw=com.gregtech.gregtech.registry.GTItems.getStack(com.gregtech.gregtech.data.MaterialPrefix.oreRaw,material);
            int found=0;
            for(var drop:level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(1))) {
                if(!ItemStack.isSameItemSameComponents(drop.getItem(),raw))continue;
                found+=drop.getItem().getCount();if(!actor.getInventory().add(drop.getItem().copy()))throw new IllegalStateException("Actual ore pickup inventory full");drop.discard();
            }
            if(found!=1)throw new IllegalStateException("Wrong real harvest drop "+material+" / "+found);
        }
        pick=actor.getMainHandItem();if(!com.gregtech.gregtech.api.tool.GTToolHelper.isUsable(pick)||pick.getDamageValue()<=before)throw new IllegalStateException("Harvested early pick lost usability or mining wear");
        var chest=(ChestBlockEntity)level.getBlockEntity(specimenPos);chest.setItem(2,pick.copy());
        chest.setItem(3,takeEarly(actor,com.gregtech.gregtech.registry.GTItems.getStack(com.gregtech.gregtech.data.MaterialPrefix.oreRaw,com.gregtech.gregtech.content.material.Materials.Copper).getItem(),3));
        chest.setItem(4,takeEarly(actor,com.gregtech.gregtech.registry.GTItems.getStack(com.gregtech.gregtech.data.MaterialPrefix.oreRaw,com.gregtech.gregtech.content.material.Materials.Tin).getItem(),1));chest.setChanged();
        var saved=new JsonObject();saved.addProperty("specimenId",specimenId);saved.addProperty("workbenchResultSlotClicked",true);saved.addProperty("actualOreHarvests",4);saved.addProperty("pickWear",pick.getDamageValue()-before);
        var items=new JsonObject();for(int slot=2;slot<=4;slot++)items.addProperty(Integer.toString(slot),chest.getItem(slot).saveOptional(level.registryAccess()).toString());saved.add("items",items);
        Files.writeString(worldRoot.getParent().resolve("early-tool-snapshot.json"),saved.toString());LOGGER.info("EARLY_TOOL_COLLECT_CRAFT_HARVEST_SUCCESS {}",saved);
        } catch(Exception e) {throw new IllegalStateException(e);}
    }

    private static void verifyEarlyTools(ServerLevel level) {
        try {
        var saved=com.google.gson.JsonParser.parseString(Files.readString(worldRoot.getParent().resolve("early-tool-snapshot.json"))).getAsJsonObject();
        if(!saved.get("specimenId").getAsString().equals(specimenId))throw new IllegalStateException("Wrong early saved specimen");
        var chest=(ChestBlockEntity)level.getBlockEntity(specimenPos);
        for(int slot=2;slot<=4;slot++)if(!chest.getItem(slot).saveOptional(level.registryAccess()).equals(net.minecraft.nbt.TagParser.parseTag(saved.getAsJsonObject("items").get(Integer.toString(slot)).getAsString())))throw new IllegalStateException("Early actual saved component stock differs "+slot);
        LOGGER.info("EARLY_TOOL_SAVED_STOCK_SUCCESS {}",saved);
        } catch(Exception e) {throw new IllegalStateException(e);}
    }

    private static net.minecraft.core.BlockPos steamBase() { return specimenPos.south(12); }

    private static com.gregtech.gregtech.blockentity.machine.KineticDieselEngineBlockEntity diesel(ServerLevel level,int offset) {
        return (com.gregtech.gregtech.blockentity.machine.KineticDieselEngineBlockEntity)level.getBlockEntity(steamBase().east(offset));
    }

    private static void prepareDieselPower(ServerLevel level) {
        if (!DIESEL_BASELINE) throw new IllegalStateException("Prepare diesel specimen with original-source baseline first");
        var base=steamBase();
        for (var pos:java.util.List.of(base,base.west(),base.east(4),base.east(8))) level.setChunkForced(pos.getX()>>4,pos.getZ()>>4,true);
        var placements=new java.util.LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        placements.put(base,steamBlock("engine_rotation_bronze").defaultBlockState().setValue(net.minecraft.world.level.block.DirectionalBlock.FACING,net.minecraft.core.Direction.NORTH));
        placements.put(base.west(),steamBlock("engine_diesel_steel").defaultBlockState().setValue(net.minecraft.world.level.block.DirectionalBlock.FACING,net.minecraft.core.Direction.EAST));
        placements.put(base.north(),steamBlock("crusher_bronze").defaultBlockState().setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING,net.minecraft.core.Direction.NORTH));
        placements.put(base.north().below(),Blocks.CHEST.defaultBlockState());
        for(int offset:new int[]{4,8}) {
            placements.put(base.east(offset),steamBlock("engine_diesel_bronze").defaultBlockState().setValue(net.minecraft.world.level.block.DirectionalBlock.FACING,net.minecraft.core.Direction.NORTH));
            placements.put(base.east(offset).north(),Blocks.STONE.defaultBlockState());
        }
        placements.put(base.east(4).south(),Blocks.STONE.defaultBlockState());
        placements.put(base.east(8).south(),steamBlock("drum_bronze").defaultBlockState());
        placements.put(base.west(2),Blocks.STONE.defaultBlockState());
        for(var e:placements.entrySet()) {level.getChunk(e.getKey());if(!level.getBlockState(e.getKey()).isAir())throw new IllegalStateException("Diesel specimen refuses overwrite "+e.getKey());level.setBlockAndUpdate(e.getKey(),e.getValue());}
        var machine=(com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity)level.getBlockEntity(base.north());
        machine.inventory().setStackInSlot(0,machineFeed());
        supplyDiesel(level,-1,100);supplyDiesel(level,4,1);
    }

    private static void supplyDiesel(ServerLevel level,int offset,int amount) {
        var pos=steamBase().east(offset);
        var inlet=level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,pos,net.minecraft.core.Direction.UP);
        if(inlet==null||inlet.fill(new net.neoforged.neoforge.fluids.FluidStack(com.gregtech.gregtech.registry.GTFluids.still("Diesel").get(),amount),net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE)!=amount)throw new IllegalStateException("Diesel actual fuel capability failed");
    }

    private static Path dieselSnapshotPath(){return worldRoot.getParent().resolve("diesel-power-snapshot.json");}
    private static JsonObject dieselSnapshot(ServerLevel level,String stage) {
        var object=new JsonObject();object.addProperty("stage",stage);object.addProperty("specimenId",specimenId);
        for(int offset:new int[]{-1,4,8}) {
            var be=diesel(level,offset);var row=new JsonObject();row.addProperty("energy",be.getKuEnergy());row.addProperty("fuel",be.fuelTank().getAmount());row.addProperty("exhaust",be.exhaustTank().getAmount());row.addProperty("stopped",be.isStopped());object.add(Integer.toString(offset),row);
        }
        return object;
    }

    private static void readDieselPower(ServerLevel level) throws IOException {
        var saved=com.google.gson.JsonParser.parseString(Files.readString(dieselSnapshotPath())).getAsJsonObject();
        if(!saved.get("specimenId").getAsString().equals(specimenId)||!saved.get("stage").getAsString().equals(DIESEL_READ_ONLY?"powered":"baseline"))throw new IllegalStateException("Wrong diesel saved stage");
        for(int offset:new int[]{-1,4,8}) {
            level.getChunk(steamBase().east(offset));var actual=dieselSnapshot(level,"loaded").getAsJsonObject(Integer.toString(offset));
            if(!actual.equals(saved.getAsJsonObject(Integer.toString(offset))))throw new IllegalStateException("Diesel real disk stock/stopped mismatch "+offset+" "+actual);
            var be=diesel(level,offset);
            var ru=com.gregtech.gregtech.data.GregTechTags.Energy.RU;
            var ku=com.gregtech.gregtech.data.GregTechTags.Energy.KU;
            long rated=be.spec().outputRate();
            if(be.getEnergySizeOutputMin(ru,null)!=rated||be.getEnergySizeOutputRecommended(ru,null)!=rated||be.getEnergySizeOutputMax(ru,null)!=rated||be.getEnergySizeOutputMin(ku,null)!=0||be.getEnergySizeOutputMax(ku,null)!=0)throw new IllegalStateException("Diesel exact rated RU packet range mismatch");
            long stored=be.getKuEnergy();
            if(be.doInject(com.gregtech.gregtech.data.GregTechTags.Energy.KU,null,16,1,true)!=0 || be.doInject(com.gregtech.gregtech.data.GregTechTags.Energy.RU,null,16,1,true)!=0 || be.getKuEnergy()!=stored)throw new IllegalStateException("Fuel motor accepted external energy");
            if(!be.isEnergyType(com.gregtech.gregtech.data.GregTechTags.Energy.RU,null,true)||be.isEnergyType(com.gregtech.gregtech.data.GregTechTags.Energy.KU,null,true)||be.getEnergyStored(com.gregtech.gregtech.data.GregTechTags.Energy.RU,null)!=be.getKuEnergy())throw new IllegalStateException("Diesel RU API not aligned");
        }
        for(var pos:java.util.List.of(steamBase(),steamBase().west(),steamBase().east(8)))level.setChunkForced(pos.getX()>>4,pos.getZ()>>4,true);
        LOGGER.info("DIESEL_POWER_DISK_READ {}",saved);
        if(DIESEL_READ_ONLY) verifyDieselOutput(level);
        else supplyDiesel(level,8,1); // New real fuel only after reading all original disk states.
    }

    private static void observeDieselCycle(ServerLevel level) {
        int offset=DIESEL_BASELINE?4:8;var be=diesel(level,offset);
        if(DIESEL_READ_ONLY||dieselCycleObserved||be.fuelTank().getAmount()!=0||be.getKuEnergy()<=0)return;
        long expected=DIESEL_BASELINE?32:512;
        if(be.getKuEnergy()!=expected)throw new IllegalStateException("One real mB diesel cycle: "+be.getKuEnergy()+" expected "+expected);
        if(!DIESEL_BASELINE && be.getEnergyCapacity(com.gregtech.gregtech.data.GregTechTags.Energy.RU,null)<expected)throw new IllegalStateException("RU reservoir reports less than its full cycle");
        dieselCycleObserved=true;
        if(DIESEL_BASELINE)be.setStopped(true);
        LOGGER.info("DIESEL_CYCLE_OBSERVED baseline={} ordinaryTick={} oneFuelMilliBucket=true storedEnergy={} originalRecipeEnergy=512",DIESEL_BASELINE,observedTicks,be.getKuEnergy());
    }

    private static void verifyDieselOutput(ServerLevel level) {
        var machine=(com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity)level.getBlockEntity(steamBase().north());
        var chest=(ChestBlockEntity)level.getBlockEntity(steamBase().north().below());
        var recipe=machine.recipeMap().findRecipe(java.util.List.of(machineFeed()),java.util.List.of(),false,machine.inputSlots(),machine.outputSlots());
        if(recipe==null||!machine.inventory().getStackInSlot(0).isEmpty()||machine.machineControl(null).progressMax()!=0)throw new IllegalStateException("Diesel/RU/rotation/KU did not complete real crushing");
        for(int i=0;i<recipe.mOutputs.length;i++) {
            var expected=recipe.mOutputs[i];if(expected==null||expected.isEmpty())continue;if(recipe.getOutputChance(i)!=10000)throw new IllegalStateException("Needs deterministic actual recipe");
            int found=0;for(int s=machine.inputSlots();s<machine.inventory().getSlots();s++)if(ItemStack.isSameItemSameComponents(expected,machine.inventory().getStackInSlot(s)))found+=machine.inventory().getStackInSlot(s).getCount();
            for(int s=0;s<chest.getContainerSize();s++)if(ItemStack.isSameItemSameComponents(expected,chest.getItem(s)))found+=chest.getItem(s).getCount();
            if(found!=expected.getCount())throw new IllegalStateException("Diesel output lost or duplicated");
            int inChest=0;for(int slot=0;slot<chest.getContainerSize();slot++)if(ItemStack.isSameItemSameComponents(expected,chest.getItem(slot)))inChest+=chest.getItem(slot).getCount();
            if(inChest!=expected.getCount())throw new IllegalStateException("Diesel recipe did not deliver actual output to chest");
        }
        var tank=(com.gregtech.gregtech.blockentity.machine.TankBlockEntity)level.getBlockEntity(steamBase().east(8).south());
        if(tank.getFluidInTank(0).getAmount()!=1||tank.getFluidInTank(0).getFluid()!=com.gregtech.gregtech.registry.GTFluids.still("CarbonDioxide").get())throw new IllegalStateException("Diesel real rear exhaust delivery failed");
    }

    private static void finishDieselPower(ServerLevel level) {
        var generator=diesel(level,-1);
        if(DIESEL_BASELINE) {
            if(!dieselCycleObserved||generator.isEnergyType(com.gregtech.gregtech.data.GregTechTags.Energy.RU,null,true)||!generator.isEnergyType(com.gregtech.gregtech.data.GregTechTags.Energy.KU,null,true))throw new IllegalStateException("Original wrong-output baseline changed unexpectedly");
            var machine=(com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity)level.getBlockEntity(steamBase().north());
            if(machine.inventory().getStackInSlot(0).isEmpty()||machine.machineControl(null).progressMax()!=0)throw new IllegalStateException("Unexpected baseline powered recipe");
        } else {
            if(!DIESEL_READ_ONLY&&!dieselCycleObserved)throw new IllegalStateException("No real full diesel fuel-cycle observation");
            verifyDieselOutput(level);
            generator.setStopped(true);
            ((com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity)level.getBlockEntity(steamBase().north())).machineControl(null).setEnabled(false);
        }
        var snapshot=dieselSnapshot(level,DIESEL_BASELINE?"baseline":"powered");
        try{Files.writeString(dieselSnapshotPath(),snapshot.toString());}catch(IOException e){throw new IllegalStateException(e);}
        LOGGER.info("DIESEL_POWER_CHECKPOINT_SUCCESS {}",snapshot);
    }

    private static Block steamBlock(String id) {
        var key = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech", id);
        if (!net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(key))
            throw new IllegalStateException("Missing steam-chain block " + key);
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(key);
    }

    private static void prepareSteamChain(ServerLevel level) {
        var base = steamBase();
        // A playerless dedicated world needs real ticking tickets, not just getChunk().
        for (var pos : java.util.List.of(base, base.north(2), base.east(4)))
            level.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, true);
        var pipePos = base.above(2);
        var enginePos = pipePos.north();
        var machinePos = pipePos.north(2);
        for (var pos : java.util.List.of(base, base.above(), pipePos, enginePos, machinePos, machinePos.below())) {
            level.getChunk(pos);
            if (!level.getBlockState(pos).isAir()) throw new IllegalStateException("Steam chain refuses to overwrite " + pos);
        }
        level.setBlockAndUpdate(base.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(base, steamBlock("burning_box_solid_dense_bronze").defaultBlockState());
        level.setBlockAndUpdate(base.above(), steamBlock("strong_steam_boiler_bronze").defaultBlockState());
        var pipe = steamBlock("pipe_medium_steel").defaultBlockState()
                .setValue(com.gregtech.gregtech.block.machine.FluidPipeBlock.propFor(net.minecraft.core.Direction.DOWN), true)
                .setValue(com.gregtech.gregtech.block.machine.FluidPipeBlock.propFor(net.minecraft.core.Direction.NORTH), true);
        level.setBlockAndUpdate(pipePos, pipe);
        level.setBlockAndUpdate(enginePos, steamBlock("engine_steam_strong_bronze").defaultBlockState()
                .setValue(net.minecraft.world.level.block.DirectionalBlock.FACING, net.minecraft.core.Direction.NORTH));
        level.setBlockAndUpdate(machinePos, steamBlock("crusher_bronze").defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, net.minecraft.core.Direction.NORTH));
        level.setBlockAndUpdate(machinePos.below(), Blocks.CHEST.defaultBlockState());
        var boiler = (com.gregtech.gregtech.blockentity.machine.BoilerTankBlockEntity) level.getBlockEntity(base.above());
        if (boiler.storedHeat() != 0 || !boiler.steamTank().isEmpty()) throw new IllegalStateException("Boiler must start cold");
        boiler.waterTank().fill(new net.neoforged.neoforge.fluids.FluidStack(
                com.gregtech.gregtech.registry.GTFluids.still("DistW").get(), 4000),
                net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        var waterPipePos = base.east().above();
        var waterSourcePos = base.east(2).above(2);
        for (var pos : java.util.List.of(waterPipePos, waterPipePos.east(), waterSourcePos)) {
            if (!level.getBlockState(pos).isAir()) throw new IllegalStateException("Water supply refuses overwrite");
        }
        var waterPipe = steamBlock("pipe_medium_steel").defaultBlockState()
                .setValue(com.gregtech.gregtech.block.machine.FluidPipeBlock.propFor(net.minecraft.core.Direction.EAST), true)
                .setValue(com.gregtech.gregtech.block.machine.FluidPipeBlock.propFor(net.minecraft.core.Direction.WEST), true);
        level.setBlockAndUpdate(waterPipePos, waterPipe);
        level.setBlockAndUpdate(waterPipePos.east(), waterPipe
                .setValue(com.gregtech.gregtech.block.machine.FluidPipeBlock.propFor(net.minecraft.core.Direction.EAST), false)
                .setValue(com.gregtech.gregtech.block.machine.FluidPipeBlock.propFor(net.minecraft.core.Direction.UP), true));
        level.setBlockAndUpdate(waterSourcePos, steamBlock("drum_bronze").defaultBlockState());
        var waterSource = (com.gregtech.gregtech.blockentity.machine.TankBlockEntity) level.getBlockEntity(waterSourcePos);
        if (waterSource.fill(new net.neoforged.neoforge.fluids.FluidStack(
                com.gregtech.gregtech.registry.GTFluids.still("DistW").get(), 50000),
                net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE) != 50000)
            throw new IllegalStateException("Could not supply distilled water reserve");
        waterSource.toggleAutoOutput();
        var engine = (com.gregtech.gregtech.blockentity.machine.KineticSteamEngineBlockEntity) level.getBlockEntity(enginePos);
        if (engine.getKuEnergy() != 0 || !engine.steamTank().isEmpty()) throw new IllegalStateException("Engine must start cold");
        var machine = (com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity) level.getBlockEntity(machinePos);
        machine.inventory().setStackInSlot(0, machineFeed());
        var box = (com.gregtech.gregtech.blockentity.machine.SolidBurningBoxBlockEntity) level.getBlockEntity(base);
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.COAL, 64));
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(base),
                net.minecraft.core.Direction.NORTH, base, false);
        steamClick(level, player, hit);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.FLINT_AND_STEEL));
        steamClick(level, player, hit);
        if (!box.isBurning() || box.getFuelStack().isEmpty() || player.getMainHandItem().getDamageValue() != 1)
            throw new IllegalStateException("Actual coal ignition/wear failed");
        var verticalPos = base.east(4).above(2);
        if (!level.getBlockState(verticalPos).isAir() || !level.getBlockState(verticalPos.north()).isAir())
            throw new IllegalStateException("Vertical exhaust specimen refuses overwrite");
        level.setBlockAndUpdate(verticalPos, steamBlock("engine_steam_bronze").defaultBlockState()
                .setValue(net.minecraft.world.level.block.DirectionalBlock.FACING, net.minecraft.core.Direction.UP));
        level.setBlockAndUpdate(verticalPos.north(), steamBlock("drum_bronze").defaultBlockState());
        var verticalInlet = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                verticalPos, net.minecraft.core.Direction.DOWN);
        if (verticalInlet == null || verticalInlet.fill(new net.neoforged.neoforge.fluids.FluidStack(
                com.gregtech.gregtech.registry.GTFluids.still("Steam").get(), 200),
                net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE) != 200)
            throw new IllegalStateException("Vertical engine back-face steam inlet failed");
        // Covers/exhaust depend on both directions using the same six-way mapping.
        for (int facing = 0; facing < 6; facing++) for (int side = 0; side < 6; side++) {
            int relative = com.gregtech.gregtech.api.energy.EngineFaceRotation.toRelative(facing, side);
            if (com.gregtech.gregtech.api.energy.EngineFaceRotation.toWorld(facing, relative) != side)
                throw new IllegalStateException("Engine face round trip failed");
        }
    }

    private static void verifySteamChain(ServerLevel level) {
        var base = steamBase();
        level.getChunk(base); level.getChunk(base.north(2));
        var boiler = (com.gregtech.gregtech.blockentity.machine.BoilerTankBlockEntity) level.getBlockEntity(base.above());
        var box = (com.gregtech.gregtech.blockentity.machine.SolidBurningBoxBlockEntity) level.getBlockEntity(base);
        var machine = (com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity) level.getBlockEntity(base.above(2).north(2));
        var chest = (ChestBlockEntity) level.getBlockEntity(machine.getBlockPos().below());
        var waterSource = (com.gregtech.gregtech.blockentity.machine.TankBlockEntity) level.getBlockEntity(base.east(2).above(2));
        var recipe = machine.recipeMap().findRecipe(java.util.List.of(machineFeed()), java.util.List.of(),
                false, machine.inputSlots(), machine.outputSlots());
        if (recipe == null || !machine.inventory().getStackInSlot(0).isEmpty()
                || machine.machineControl(null).progressMax() != 0 || waterSource.getFluidInTank(0).getAmount() >= 50000
                || box.getFuelStack().getCount() >= 64 || box.getAshStack().isEmpty())
            throw new IllegalStateException("Natural steam chain did not complete: water=" + boiler.waterTank().getAmount()
                    + " progress=" + machine.machineControl(null).progress() + "/" + machine.machineControl(null).progressMax());
        for (int output = 0; output < recipe.mOutputs.length; output++) {
            var expected = recipe.mOutputs[output];
            if (expected == null || expected.isEmpty()) continue;
            if (recipe.getOutputChance(output) != 10000) throw new IllegalStateException("Expected deterministic recipe");
            int found = 0;
            for (int slot = machine.inputSlots(); slot < machine.inventory().getSlots(); slot++)
                if (ItemStack.isSameItemSameComponents(expected, machine.inventory().getStackInSlot(slot)))
                    found += machine.inventory().getStackInSlot(slot).getCount();
            for (int slot = 0; slot < chest.getContainerSize(); slot++)
                if (ItemStack.isSameItemSameComponents(expected, chest.getItem(slot))) found += chest.getItem(slot).getCount();
            if (found != expected.getCount()) throw new IllegalStateException("Steam powered recipe output mismatch " + found);
        }
        var exhaust = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                base.east(4).above(2).north(), net.minecraft.core.Direction.SOUTH);
        var recovered = exhaust == null ? net.neoforged.neoforge.fluids.FluidStack.EMPTY : exhaust.getFluidInTank(0);
        if (recovered.getAmount() != 1 || recovered.getFluid() != com.gregtech.gregtech.registry.GTFluids.still("DistW").get())
            throw new IllegalStateException("Vertical engine failed to recover one-L north-side condensate");
        LOGGER.info("STEAM_CHAIN_COMPLETE ordinaryTicks={} coldStart=true directEnergyInjection=false manualMachineTicks=false water={} coal={}",
                observedTicks, boiler.waterTank().getAmount(), box.getFuelStack().getCount());
    }

    private static void steamClick(ServerLevel level, net.minecraft.world.entity.player.Player player,
                                   net.minecraft.world.phys.BlockHitResult hit) {
        var event = new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock(
                player, net.minecraft.world.InteractionHand.MAIN_HAND, hit.getBlockPos(), hit);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
        if (!event.isCanceled()) level.getBlockState(hit.getBlockPos()).useItemOn(player.getMainHandItem(), level,
                player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
    }

    private static void prepareSmeltery(ServerLevel level) {
        BlockPos moldPos = specimenPos.south(2), basinPos = specimenPos.south(4);
        for (BlockPos pos : new BlockPos[]{moldPos, basinPos, basinPos.above()}) {
            if (!level.getBlockState(pos).isAir())
                throw new IllegalStateException("Smeltery prepare refuses to overwrite " + pos);
        }
        var ceramic = com.gregtech.gregtech.content.material.Materials.Ceramic;
        var basinBlock = com.gregtech.gregtech.platform.neoforge.smeltery.SmelteryRegistries.basins().stream()
                .filter(holder -> holder.get().spec().material().equals(ceramic)).findFirst().orElseThrow().get();
        level.setBlockAndUpdate(moldPos, com.gregtech.gregtech.platform.neoforge.smeltery.SmelteryRegistries.CERAMIC_MOLD.get().defaultBlockState());
        level.setBlockAndUpdate(basinPos, basinBlock.defaultBlockState());
        var mold = (com.gregtech.gregtech.blockentity.machine.MoldBlockEntity) level.getBlockEntity(moldPos);
        // Initialize through the public carving/filling API; verify never seeds this state.
        for (int z = 0; z < 5; z++) for (int x = 0; x < 3; x++)
            mold.trySelectShape(null, net.minecraft.world.InteractionHand.MAIN_HAND,
                    0.125 + (x + 0.5) * 0.15, 0.125 + (z + 0.5) * 0.15);
        long unit = com.gregtech.gregtech.api.material.GTValues.U;
        if (mold.fillMold(com.gregtech.gregtech.content.material.Materials.Copper, unit, 300,
                net.minecraft.core.Direction.UP.ordinal()) != unit)
            throw new IllegalStateException("Could not prepare filled one-U copper mold");
        var basin = (com.gregtech.gregtech.blockentity.machine.MoldBasinBlockEntity) level.getBlockEntity(basinPos);
        if (basin.fillMold(com.gregtech.gregtech.content.material.Materials.Bronze, 9L * unit, 300,
                net.minecraft.core.Direction.UP.ordinal()) != 9L * unit || basin.consumeContent(unit) != unit)
            throw new IllegalStateException("Could not prepare partly drained eight-U bronze basin");
    }

    private static void verifySmeltery(ServerLevel level) {
        level.getChunk(specimenPos.south(2));
        level.getChunk(specimenPos.south(4));
        long unit = com.gregtech.gregtech.api.material.GTValues.U;
        if (!(level.getBlockEntity(specimenPos.south(2)) instanceof com.gregtech.gregtech.blockentity.machine.MoldBlockEntity mold)
                || mold.getMoldContentMaterial() != com.gregtech.gregtech.content.material.Materials.Copper
                || mold.getMoldContentAmount() != unit || mold.getMoldRequiredMaterialUnits() != unit)
            throw new IllegalStateException("Real-world copper mold shape/material/amount did not survive");
        if (!(level.getBlockEntity(specimenPos.south(4)) instanceof com.gregtech.gregtech.blockentity.machine.MoldBasinBlockEntity basin)
                || basin.getMoldContentMaterial() != com.gregtech.gregtech.content.material.Materials.Bronze
                || basin.getMoldContentAmount() != 8L * unit || !basin.getSolidOutput().isEmpty())
            throw new IllegalStateException("Real-world eight-U bronze basin changed or created a full block");
    }

    private static com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity machine(ServerLevel level, int offset) {
        BlockPos pos = specimenPos.south(offset);
        level.getChunk(pos);
        if (level.getBlockEntity(pos) instanceof com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity machine)
            return machine;
        throw new IllegalStateException("Missing real basic machine at " + pos);
    }

    private static ItemStack machineFeed() {
        return com.gregtech.gregtech.registry.GTItems.getStack(com.gregtech.gregtech.data.MaterialPrefix.gemChipped,
                com.gregtech.gregtech.content.material.Materials.Diamond);
    }

    private static net.neoforged.neoforge.fluids.FluidStack markedWater() {
        var fluid = new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000);
        fluid.set(DataComponents.CUSTOM_NAME, Component.literal("machine-water/" + specimenId));
        return fluid;
    }

    private static void prepareMachines(ServerLevel level) {
        for (int offset : new int[]{6, 8}) {
            BlockPos pos = specimenPos.south(offset);
            if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.below()).isAir())
                throw new IllegalStateException("Machine prepare refuses to overwrite " + pos);
            String id = offset == 6 ? "crusher_bronze" : "mixer_bronze";
            var block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech", id));
            if (!(block instanceof com.gregtech.gregtech.block.machine.BasicMachineBlock))
                throw new IllegalStateException("Missing registered machine " + id);
            level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(pos, block.defaultBlockState());
        }
        var crusher = machine(level, 6);
        crusher.inventory().setStackInSlot(0, machineFeed());
        for (int tick = 0; tick < 2; tick++) {
            if (crusher.doInject(com.gregtech.gregtech.data.GregTechTags.Energy.KU, net.minecraft.core.Direction.SOUTH, 32, 1, true) != 1)
                throw new IllegalStateException("Actual crusher back face refused a valid KU packet");
            com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity.serverTick(level,
                    crusher.getBlockPos(), crusher.getBlockState(), crusher);
        }
        crusher.machineControl(null).setEnabled(false);
        var mixer = machine(level, 8);
        mixer.machineControl(null).setEnabled(false);
        ItemStack marker = new ItemStack(Items.COPPER_INGOT, 3);
        marker.set(DataComponents.CUSTOM_NAME, Component.literal("machine-stock/" + specimenId));
        mixer.inventory().setStackInSlot(0, marker);
        if (mixer.getTanksInput().length == 0 || mixer.getTanksInput()[0].fill(markedWater(),
                net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE) != 1000)
            throw new IllegalStateException("Actual mixer did not accept marked water");
    }

    private static void verifyMachines(ServerLevel level) {
        var crusher = machine(level, 6);
        var saved = crusher.saveWithoutMetadata(level.registryAccess());
        if (!crusher.spec().id().equals("crusher_bronze") || !saved.getBoolean("gt.control_stopped")
                || !crusher.inventory().getStackInSlot(0).isEmpty()
                || (!machineJobCompleted && (saved.getLong("gt.progress") != 64
                    || saved.getLong("gt.max_progress") <= 64 || !saved.contains("gt.pending_outputs")))
                || (machineJobCompleted && (saved.getLong("gt.progress") != 0
                    || saved.getLong("gt.max_progress") != 0 || saved.contains("gt.pending_outputs"))))
            throw new IllegalStateException("Saved crusher job/progress/consumed input did not survive");
        var mixer = machine(level, 8);
        ItemStack stock = mixer.inventory().getStackInSlot(0);
        if (!mixer.spec().id().equals("mixer_bronze") || mixer.machineControl(null).enabled()
                || !stock.is(Items.COPPER_INGOT) || stock.getCount() != 3
                || !Component.literal("machine-stock/" + specimenId).equals(stock.get(DataComponents.CUSTOM_NAME)))
            throw new IllegalStateException("Stopped mixer inventory/components did not survive");
        var water = mixer.getTanksInput()[0].getFluid();
        if (water.getAmount() != 1000 || !net.neoforged.neoforge.fluids.FluidStack.isSameFluidSameComponents(water, markedWater()))
            throw new IllegalStateException("Mixer input fluid/components did not survive");
        var different = markedWater();
        different.set(DataComponents.CUSTOM_NAME, Component.literal("different-water"));
        var tank = mixer.getTanksInput()[0];
        if (tank.fill(different, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE) != 0
                || !tank.drain(different, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE).isEmpty()
                || tank.getAmount() != 1000)
            throw new IllegalStateException("Tank merged or drained different fluid components");
        if ("verify".equals(PHASE) && !worldReadVerified) {
            // Called only on the first disk read, before the ordinary server tick resumes.
            resumeMachineJob(level, crusher);
        }
    }

    private static void resumeMachineJob(ServerLevel level, com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity crusher) {
        var recipe = crusher.recipeMap().findRecipe(java.util.List.of(machineFeed()), java.util.List.of(),
                false, crusher.inputSlots(), crusher.outputSlots());
        if (recipe == null || recipe.mFluidInputs.length != 0 || recipe.mFluidOutputs.length != 0)
            throw new IllegalStateException("Missing actual registered diamond crushing recipe");
        crusher.machineControl(null).setEnabled(true);
        for (int tick = 0; tick < 5000 && crusher.machineControl(null).progressMax() > 0; tick++) {
            crusher.doInject(com.gregtech.gregtech.data.GregTechTags.Energy.KU, net.minecraft.core.Direction.SOUTH, 32, 1, true);
            com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity.serverTick(level,
                    crusher.getBlockPos(), crusher.getBlockState(), crusher);
        }
        if (crusher.machineControl(null).progressMax() != 0)
            throw new IllegalStateException("Reloaded job did not complete under valid KU supply");
        for (int i = 0; i < recipe.mOutputs.length; i++) {
            var expected = recipe.mOutputs[i];
            if (expected == null || expected.isEmpty()) continue;
            if (recipe.getOutputChance(i) != 10000) throw new IllegalStateException("Checkpoint requires deterministic real output");
            int found = 0;
            for (int slot = crusher.inputSlots(); slot < crusher.inventory().getSlots(); slot++) {
                var output = crusher.inventory().getStackInSlot(slot);
                if (ItemStack.isSameItemSameComponents(expected, output)) found += output.getCount();
            }
            if (found != expected.getCount()) throw new IllegalStateException("Reloaded real job lost or duplicated output");
        }
        // Later ordinary ticks check the completed state; never recreate the job.
        machineJobCompleted = true;
        crusher.machineControl(null).setEnabled(false);
        LOGGER.info("MACHINE_WORLD_JOB_COMPLETED persistedProgress=64 consumedInputEmpty=true registeredRecipe=true");
    }

    private static boolean machineJobCompleted;

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post event) {
        tick(event.getServer());
    }

    private static void tick(MinecraftServer server) {
        if (!ENABLED || server != activeServer || TERMINAL.get() || stopRequested) return;
        try {
            observedTicks++;
            if(DIESEL_POWER) observeDieselCycle(server.overworld());
            if (observedTicks < REQUIRED_TICKS) return;
            verifyWorld(server.overworld());
            stopRequested = true;
            LOGGER.info("SERVER_SMOKE_STOP_REQUESTED {}", identity());
            // The ordinary loop exits and stopServer saves players/chunks and closes levels.
            server.halt(false);
        } catch (Throwable failure) {
            fail("tick_or_stop", failure);
        }
    }

    @SubscribeEvent
    public static void serverStopping(ServerStoppingEvent event) {
        if (!ENABLED || event.getServer() != activeServer) return;
        normalStoppingObserved = true;
        LOGGER.info("SERVER_SMOKE_STOPPING {}", identity());
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent event) {
        if (!ENABLED || event.getServer() != activeServer) return;
        try {
            LOGGER.info("SERVER_SMOKE_STOPPED {}", identity());
            if (TERMINAL.get()) return;
            if (!stopRequested || !normalStoppingObserved || observedTicks != REQUIRED_TICKS) {
                throw new IllegalStateException("Server stopped without the requested ordinary-tick normal lifecycle");
            }
            Path levelData = worldRoot.resolve("level.dat");
            Path region = worldRoot.resolve("region").resolve("r." + (specimenPos.getX() >> 9)
                    + "." + (specimenPos.getZ() >> 9) + ".mca");
            if (!Files.isRegularFile(levelData) || Files.size(levelData) == 0) {
                throw new IOException("Stopped server has no nonempty level.dat: " + levelData);
            }
            requireSavedChunk(region);
            JsonObject result = identity();
            result.addProperty("normalStopObserved", true);
            result.addProperty("restartWorldReadVerified", worldReadVerified);
            result.addProperty("levelDat", levelData.toString());
            result.addProperty("levelDatBytes", Files.size(levelData));
            result.addProperty("region", region.toString());
            result.addProperty("regionBytes", Files.size(region));
            if (TERMINAL.compareAndSet(false, true)) LOGGER.info("SERVER_SMOKE_SUCCESS {}", result);
        } catch (Throwable failure) {
            // Already stopped; do not schedule another shutdown task.
            fail("stopped_or_saved_files", failure);
        } finally {
            if (watchdog != null) watchdog.shutdownNow();
        }
    }

    private static void requireSavedChunk(Path region) throws IOException {
        if (!Files.isRegularFile(region) || Files.size(region) < 8192) {
            throw new IOException("Specimen chunk has no valid region header: " + region);
        }
        int index = (specimenPos.getX() >> 4 & 31) + (specimenPos.getZ() >> 4 & 31) * 32;
        try (DataInputStream input = new DataInputStream(Files.newInputStream(region))) {
            input.skipNBytes(index * 4L);
            int location = input.readInt();
            int sector = location >>> 8;
            int count = location & 255;
            if (sector < 2 || count == 0 || (long) (sector + count) * 4096 > Files.size(region)) {
                throw new IOException("Specimen chunk has no allocated saved region sectors: " + region);
            }
        }
        // Region allocation is save evidence, not an NBT-content proof. The separate
        // verify JVM must load and check the actual block entity, UUID and all 27 slots.
    }

    private static void fail(String reason, Throwable failure) {
        if (!TERMINAL.compareAndSet(false, true)) return;
        JsonObject result = identity();
        result.addProperty("reason", reason);
        result.addProperty("error", failure.toString());
        LOGGER.error("SERVER_SMOKE_FAILED {}", result, failure);
        if (watchdog != null) watchdog.shutdownNow();
        MinecraftServer server = activeServer;
        if (server != null && !server.isStopped()) {
            try {
                server.execute(() -> server.halt(false));
            } catch (Throwable schedulingFailure) {
                LOGGER.error("SERVER_SMOKE_STOP_SCHEDULING_ERROR", schedulingFailure);
            }
        }
    }

    private static JsonObject identity() {
        JsonObject result = new JsonObject();
        result.addProperty("platform", PLATFORM);
        result.addProperty("minecraft", MINECRAFT_VERSION);
        result.addProperty("phase", PHASE);
        result.addProperty("sessionId", SESSION_ID);
        result.addProperty("pid", ProcessHandle.current().pid());
        result.addProperty("specimenId", specimenId);
        result.addProperty("serverClass", activeServer == null ? "unobserved" : activeServer.getClass().getName());
        result.addProperty("worldRoot", worldRoot == null ? null : worldRoot.toString());
        if (specimenPos != null) {
            result.addProperty("x", specimenPos.getX());
            result.addProperty("y", specimenPos.getY());
            result.addProperty("z", specimenPos.getZ());
            result.addProperty("dirtX", specimenPos.getX() + 1);
            result.addProperty("dirtY", specimenPos.getY());
            result.addProperty("dirtZ", specimenPos.getZ());
        }
        result.addProperty("copperCount", 3);
        result.addProperty("ironCount", 1);
        result.addProperty("observedTicks", observedTicks);
        result.addProperty("elapsedMs", startedAt == 0 ? 0
                : TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt));
        result.addProperty("restartWorldReadVerified", worldReadVerified);
        result.addProperty("smelteryWorldChecked", SMELTERY_WORLD);
        result.addProperty("machineWorldChecked", MACHINE_WORLD);
        result.addProperty("machineJobCompleted", machineJobCompleted);
        result.addProperty("steamChainChecked", STEAM_CHAIN);
        return result;
    }
}
