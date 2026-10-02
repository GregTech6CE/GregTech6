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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/** Opt-in ordinary dedicated-server world restart smoke, excluded from the mod jar. */
@Mod.EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID, value = Dist.DEDICATED_SERVER,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ForgeDedicatedSmoke {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String PLATFORM = "forge";
    private static final String MINECRAFT_VERSION = "1.20.1";
    private static final String PHASE = System.getProperty("gregtech.integration.serverSmokePhase", "");
    private static final boolean ENABLED = !PHASE.isEmpty();
    private static final boolean DIESEL_POWER = Boolean.getBoolean("gregtech.integration.dieselPowerSmoke");
    private static boolean dieselCycleObserved;
    private static final String SESSION_ID = UUID.randomUUID().toString();
    private static final int REQUIRED_TICKS = 200;
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

    private ForgeDedicatedSmoke() {}
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
                Thread thread = new Thread(task, "gregtech-forge-dedicated-smoke-watchdog");
                thread.setDaemon(true);
                return thread;
            });
            watchdog.schedule(() -> fail("timeout", new TimeoutException(
                    "No completed dedicated phase and stop within 120 seconds of ServerStarted")),
                    120, TimeUnit.SECONDS);
            LOGGER.info("SERVER_SMOKE_STARTED {}", identity());
            if (ENGINE_CRAFTING) craftedSteamResults = steamEngineCrafting(level);
            if (Boolean.getBoolean("gregtech.integration.dieselCraftingSmoke")) dieselCrafting(level);
            // Explicitly load the real overworld chunk; no synthetic NBT round trip.
            level.getChunk(specimenPos);
            level.getChunk(specimenPos.east());
            if ("prepare".equals(PHASE)) {
                prepare(level);
                verifyWorld(level);
                LOGGER.info("SERVER_SMOKE_PREPARED {}", identity());
            } else {
                // Never create/repair the specimen in verify: it must have loaded from disk.
                verifyWorld(level);
                if (DIESEL_POWER) readDieselPower(level);
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
        chest.setCustomName(Component.literal("gregtech-dedicated-smoke/" + specimenId));
        chest.setItem(0, new ItemStack(Items.COPPER_INGOT, 3));
        chest.setItem(1, new ItemStack(Items.IRON_INGOT, 1));
        if (ENGINE_CRAFTING) {
            chest.setItem(2, craftedSteamResults.get("engine_steam_bronze").copy());
            chest.setItem(3, craftedSteamResults.get("engine_steam_strong_bronze").copy());
        }
        chest.setChanged();
        if (DIESEL_POWER) prepareDieselPower(level);
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
            if (!ItemStack.isSameItemSameTags(chest.getItem(2), craftedSteamResults.get("engine_steam_bronze"))
                    || chest.getItem(2).getCount()!=1
                    || !ItemStack.isSameItemSameTags(chest.getItem(3), craftedSteamResults.get("engine_steam_strong_bronze"))
                    || chest.getItem(3).getCount()!=1)
                throw new IllegalStateException("Real persisted steam crafting results differ");
        }
        for (int slot = ENGINE_CRAFTING ? 4 : 2; slot < chest.getContainerSize(); slot++) {
            if (!chest.getItem(slot).isEmpty()) {
                throw new IllegalStateException("Unexpected item in specimen chest slot " + slot);
            }
        }
        if (DIESEL_POWER && observedTicks >= REQUIRED_TICKS) finishDieselPower(level);
    }

    private static net.minecraft.core.BlockPos steamBase() { return specimenPos.south(12); }

    private static com.gregtech.gregtech.blockentity.machine.KineticDieselEngineBlockEntity diesel(ServerLevel level,int offset) {
        return (com.gregtech.gregtech.blockentity.machine.KineticDieselEngineBlockEntity)level.getBlockEntity(steamBase().east(offset));
    }

    private static void prepareDieselPower(ServerLevel level) {
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
        supplyDiesel(level,-1,100);supplyDiesel(level,4,1);supplyDiesel(level,8,1);
    }

    private static void supplyDiesel(ServerLevel level,int offset,int amount) {
        var pos=steamBase().east(offset);
        var inlet=level.getBlockEntity(pos).getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER,net.minecraft.core.Direction.UP).resolve().orElse(null);
        if(inlet==null||inlet.fill(new net.minecraftforge.fluids.FluidStack(com.gregtech.gregtech.registry.GTFluids.still("Diesel").get(),amount),net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE)!=amount)throw new IllegalStateException("Diesel actual fuel capability failed");
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
        if(!saved.get("specimenId").getAsString().equals(specimenId)||!saved.get("stage").getAsString().equals("powered"))throw new IllegalStateException("Wrong diesel saved stage");
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
        verifyDieselOutput(level); // No fresh fuel or output is supplied after reload.
    }

    private static void observeDieselCycle(ServerLevel level) {
        int offset=8;var be=diesel(level,offset);
        if("verify".equals(PHASE)||dieselCycleObserved||be.fuelTank().getAmount()!=0||be.getKuEnergy()<=0)return;
        long expected=512;
        if(be.getKuEnergy()!=expected)throw new IllegalStateException("One real mB diesel cycle: "+be.getKuEnergy()+" expected "+expected);
        if( be.getEnergyCapacity(com.gregtech.gregtech.data.GregTechTags.Energy.RU,null)<expected)throw new IllegalStateException("RU reservoir reports less than its full cycle");
        dieselCycleObserved=true;
        diesel(level,4).setStopped(true);
        LOGGER.info("DIESEL_CYCLE_OBSERVED baseline=false ordinaryTick={} oneFuelMilliBucket=true storedEnergy={} originalRecipeEnergy=512",observedTicks,be.getKuEnergy());
    }

    private static void verifyDieselOutput(ServerLevel level) {
        var machine=(com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity)level.getBlockEntity(steamBase().north());
        var chest=(ChestBlockEntity)level.getBlockEntity(steamBase().north().below());
        var recipe=machine.recipeMap().findRecipe(java.util.List.of(machineFeed()),java.util.List.of(),false,machine.inputSlots(),machine.outputSlots());
        if(recipe==null||!machine.inventory().getStackInSlot(0).isEmpty()||machine.machineControl(null).progressMax()!=0)throw new IllegalStateException("Diesel/RU/rotation/KU did not complete real crushing");
        for(int i=0;i<recipe.mOutputs.length;i++) {
            var expected=recipe.mOutputs[i];if(expected==null||expected.isEmpty())continue;if(recipe.getOutputChance(i)!=10000)throw new IllegalStateException("Needs deterministic actual recipe");
            int found=0;for(int s=machine.inputSlots();s<machine.inventory().getSlots();s++)if(ItemStack.isSameItemSameTags(expected,machine.inventory().getStackInSlot(s)))found+=machine.inventory().getStackInSlot(s).getCount();
            for(int s=0;s<chest.getContainerSize();s++)if(ItemStack.isSameItemSameTags(expected,chest.getItem(s)))found+=chest.getItem(s).getCount();
            if(found!=expected.getCount())throw new IllegalStateException("Diesel output lost or duplicated");
            int inChest=0;for(int slot=0;slot<chest.getContainerSize();slot++)if(ItemStack.isSameItemSameTags(expected,chest.getItem(slot)))inChest+=chest.getItem(slot).getCount();
            if(inChest!=expected.getCount())throw new IllegalStateException("Diesel recipe did not deliver actual output to chest");
        }
        var tank=(com.gregtech.gregtech.blockentity.machine.TankBlockEntity)level.getBlockEntity(steamBase().east(8).south());
        if(tank.getFluidInTank(0).getAmount()!=1||tank.getFluidInTank(0).getFluid()!=com.gregtech.gregtech.registry.GTFluids.still("CarbonDioxide").get())throw new IllegalStateException("Diesel real rear exhaust delivery failed");
    }

    private static void finishDieselPower(ServerLevel level) {
        var generator=diesel(level,-1);
        if("prepare".equals(PHASE)&&!dieselCycleObserved)throw new IllegalStateException("No real full diesel fuel-cycle observation");
        verifyDieselOutput(level);
        generator.setStopped(true);
        ((com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity)level.getBlockEntity(steamBase().north())).machineControl(null).setEnabled(false);
        var snapshot=dieselSnapshot(level,"powered");
        try{Files.writeString(dieselSnapshotPath(),snapshot.toString());}catch(IOException e){throw new IllegalStateException(e);}
        LOGGER.info("DIESEL_POWER_CHECKPOINT_SUCCESS {}",snapshot);
    }

    private static Block steamBlock(String id) {
        var key = new net.minecraft.resources.ResourceLocation("gregtech", id);
        if (!net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(key))
            throw new IllegalStateException("Missing steam-chain block " + key);
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(key);
    }

    private static ItemStack machineFeed() {
        return com.gregtech.gregtech.registry.GTItems.getStack(com.gregtech.gregtech.data.MaterialPrefix.gemChipped,
                com.gregtech.gregtech.content.material.Materials.Diamond);
    }

    private static java.util.Map<String,ItemStack> steamEngineCrafting(ServerLevel level) {
        var results=new java.util.HashMap<String,ItemStack>();int checked=0;
        var menu=new net.minecraft.world.inventory.AbstractContainerMenu(null,0) {
            @Override public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player,int slot){return ItemStack.EMPTY;}
            @Override public boolean stillValid(net.minecraft.world.entity.player.Player player){return true;}
        };
        for(var entry:com.gregtech.gregtech.content.energy.EngineCatalog.all()) {
            boolean strong=entry.spec() instanceof com.gregtech.gregtech.api.machine.StrongSteamEngineSpec;
            String id;com.gregtech.gregtech.api.material.GTMaterial material;
            if(strong){var spec=(com.gregtech.gregtech.api.machine.StrongSteamEngineSpec)entry.spec();id=spec.id();material=spec.material();}
            else if(entry.spec() instanceof com.gregtech.gregtech.api.machine.SteamEngineSpec spec){id=spec.id();material=spec.material();}
            else continue;
            var plate=steamForm(strong?com.gregtech.gregtech.data.MaterialPrefix.plateDense:com.gregtech.gregtech.data.MaterialPrefix.plateDouble,material);
            var rod=steamForm(com.gregtech.gregtech.data.MaterialPrefix.stick,material);
            var spring=steamForm(strong?com.gregtech.gregtech.data.MaterialPrefix.spring:com.gregtech.gregtech.data.MaterialPrefix.springSmall,material);
            var hammer=com.gregtech.gregtech.item.GTToolItem.create(com.gregtech.gregtech.api.tool.GTToolType.HARD_HAMMER,
                    com.gregtech.gregtech.content.material.Materials.Bronze,com.gregtech.gregtech.content.material.generated.WoodMaterials.Wood);
            var wrench=com.gregtech.gregtech.item.GTToolItem.create(com.gregtech.gregtech.api.tool.GTToolType.WRENCH,
                    com.gregtech.gregtech.content.material.Materials.Bronze,com.gregtech.gregtech.content.material.generated.WoodMaterials.Wood);
            var input=new net.minecraft.world.inventory.TransientCraftingContainer(menu,3,3);
            var stacks=java.util.List.of(plate.copy(),hammer,plate.copy(),rod.copy(),spring,rod.copy(),plate.copy(),wrench,plate.copy());
            for(int slot=0;slot<9;slot++)input.setItem(slot,stacks.get(slot));
            var recipe=level.getRecipeManager().byKey(new net.minecraft.resources.ResourceLocation("gregtech","engines/"+id)).orElseThrow();
            if(!(recipe instanceof net.minecraft.world.item.crafting.CraftingRecipe craft))throw new IllegalStateException("Wrong steam crafting recipe type "+id);
            for(var ingredient:craft.getIngredients())
                if(ingredient!=net.minecraft.world.item.crafting.Ingredient.EMPTY && ingredient.getItems().length==0)
                    throw new IllegalStateException("Unresolved steam ingredient "+id);
            var result=craft.assemble(input,level.registryAccess());
            var expected=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation("gregtech",id));
            if(expected==null || !craft.matches(input,level) || result.getItem()!=expected || result.getCount()!=1)
                throw new IllegalStateException("Original steam crafting shape/output "+id);
            var remains=craft.getRemainingItems(input);
            if(remains.get(1).getDamageValue()!=400 || remains.get(7).getDamageValue()!=800
                    || hammer.getDamageValue()!=0 || wrench.getDamageValue()!=0)
                throw new IllegalStateException("Original steam crafting tool wear "+id);
            for(int slot:java.util.List.of(0,2,3,4,5,6,8))if(!remains.get(slot).isEmpty())
                throw new IllegalStateException("Steam material not consumed "+id);
            input.setItem(4,steamForm(strong?com.gregtech.gregtech.data.MaterialPrefix.springSmall:com.gregtech.gregtech.data.MaterialPrefix.spring,material));
            if(craft.matches(input,level))throw new IllegalStateException("Wrong steam spring accepted "+id);
            input.setItem(4,spring);input.setItem(0,steamForm(com.gregtech.gregtech.data.MaterialPrefix.plate,material));
            if(craft.matches(input,level))throw new IllegalStateException("Wrong steam plate accepted "+id);
            if(material.resolve()==com.gregtech.gregtech.content.material.Materials.Bronze.resolve()) {
                input.setItem(0,steamForm(strong?com.gregtech.gregtech.data.MaterialPrefix.plateDense:com.gregtech.gregtech.data.MaterialPrefix.plateDouble,
                        com.gregtech.gregtech.content.material.Materials.Copper));
                if(craft.matches(input,level))throw new IllegalStateException("Wrong steam material accepted "+id);
                results.put(id,result);
            }
            checked++;
        }
        if(checked!=28 || results.size()!=2)throw new IllegalStateException("Incomplete steam crafting catalog");
        LOGGER.info("STEAM_ENGINE_CRAFTING_CHECKPOINT_SUCCESS {}","{\"platform\":\"forge\",\"recipes\":28,\"normalRows\":14,\"strongRows\":14,\"patternAndOutput\":true,\"prefixDistinction\":true,\"hammerWear\":400,\"wrenchWear\":800,\"playerCraftingClickVerified\":false}");
        return java.util.Map.copyOf(results);
    }

    private static void dieselCrafting(ServerLevel level) {
        var menu=new net.minecraft.world.inventory.AbstractContainerMenu(null,0) {
            @Override public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player,int slot){return ItemStack.EMPTY;}
            @Override public boolean stillValid(net.minecraft.world.entity.player.Player player){return true;}
        };
        var bottle=new ItemStack(java.util.Objects.requireNonNull(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation("gregtech","lubricant_bottle"))));
        var early=new ItemStack(java.util.Objects.requireNonNull(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation("gregtech","olive_oil"))));
        var cell=new ItemStack(java.util.Objects.requireNonNull(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation("gregtech","fluid_cell_tin"))));
        var handler=net.minecraftforge.fluids.FluidUtil.getFluidHandler(cell).resolve().orElseThrow();
        var fluid=com.gregtech.gregtech.registry.GTFluids.still("Lubricant");
        if(fluid==null || !fluid.isPresent() || handler.fill(new net.minecraftforge.fluids.FluidStack(fluid.get(),1000),net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE)!=1000)throw new IllegalStateException("Missing finite lubricant vessel");
        cell=handler.getContainer();int checked=0;
        for(var entry:com.gregtech.gregtech.content.energy.EngineCatalog.all()) {
            if(!(entry.spec() instanceof com.gregtech.gregtech.api.machine.DieselEngineSpec spec))continue;
            var material=spec.material();String id=spec.id();
            var plate=steamForm(com.gregtech.gregtech.data.MaterialPrefix.plateCurved,material);
            var rod=steamForm(com.gregtech.gregtech.data.MaterialPrefix.stick,material);
            var gear=steamForm(com.gregtech.gregtech.data.MaterialPrefix.gearGt,material);
            var small=steamForm(com.gregtech.gregtech.data.MaterialPrefix.gearGtSmall,material);
            var casing=com.gregtech.gregtech.registry.GTBlocks.getStack(com.gregtech.gregtech.api.prefix.BlockMaterialPrefix.casingMachineDouble,material);
            if(casing.isEmpty())throw new IllegalStateException("Missing diesel double casing "+id);
            var actual=level.getRecipeManager().byKey(new net.minecraft.resources.ResourceLocation("gregtech","engines/"+id)).orElseThrow();
            if(!(actual instanceof net.minecraft.world.item.crafting.CraftingRecipe craft))throw new IllegalStateException("Wrong diesel crafting type");
            for(var ingredient:craft.getIngredients())if(ingredient!=net.minecraft.world.item.crafting.Ingredient.EMPTY&&ingredient.getItems().length==0)throw new IllegalStateException("Missing diesel ingredient "+id);
            for(var lubricant:java.util.List.of(bottle,cell)) {
                var input=new net.minecraft.world.inventory.TransientCraftingContainer(menu,3,3);
                var slots=java.util.List.of(plate.copy(),lubricant.copy(),plate.copy(),rod.copy(),casing.copy(),rod.copy(),gear.copy(),plate.copy(),small.copy());
                for(int s=0;s<9;s++)input.setItem(s,slots.get(s));
                var result=craft.assemble(input,level.registryAccess());
                var expected=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation("gregtech",id));
                if(!craft.matches(input,level)||result.getItem()!=expected||result.getCount()!=1)throw new IllegalStateException("Original diesel shape/output "+id);
                var decoded=dieselPacket(craft);if(!decoded.matches(input,level))throw new IllegalStateException("Diesel ingredient network policy lost "+id);
                var remains=decoded.getRemainingItems(input);var returned=remains.get(1);
                if(lubricant==bottle) {if(!ItemStack.isSameItemSameTags(returned,com.gregtech.gregtech.item.BottleItem.emptyBottle())||returned.getCount()!=1)throw new IllegalStateException("Diesel 250 bottle remainder "+id);}
                else if(returned.getItem()!=lubricant.getItem()||returned.getCount()!=1||!net.minecraftforge.fluids.FluidUtil.getFluidContained(returned).orElse(net.minecraftforge.fluids.FluidStack.EMPTY).isEmpty()||net.minecraftforge.fluids.FluidUtil.getFluidContained(lubricant).orElseThrow().getAmount()!=1000)throw new IllegalStateException("Diesel finite 1000 vessel remainder/input mutation "+id);
                for(int s:java.util.List.of(0,2,3,4,5,6,7,8))if(!remains.get(s).isEmpty())throw new IllegalStateException("Diesel raw slot remainder "+id);
                input.setItem(6,small.copy());input.setItem(8,gear.copy());if(craft.matches(input,level))throw new IllegalStateException("Diesel mirror accepted "+id);
                input.setItem(6,gear.copy());input.setItem(8,small.copy());input.setItem(1,early.copy());if(craft.matches(input,level))throw new IllegalStateException("Diesel early oil accepted "+id);
            }
            checked++;
        }
        var filling=level.getRecipeManager().byKey(new net.minecraft.resources.ResourceLocation("gregtech","bottles/lubricant_bottle_x4")).orElseThrow();
        var input=new net.minecraft.world.inventory.TransientCraftingContainer(menu,3,2);input.setItem(0,cell.copy());
        for(int s=1;s<=4;s++)input.setItem(s,com.gregtech.gregtech.item.BottleItem.emptyBottle());
        if(!(filling instanceof net.minecraft.world.item.crafting.CraftingRecipe craft)||!craft.matches(input,level)||craft.assemble(input,level.registryAccess()).getCount()!=4||!dieselPacket(craft).matches(input,level))throw new IllegalStateException("Legacy filling 1000mB recipe regression");
        input.setItem(0,bottle.copy());if(craft.matches(input,level))throw new IllegalStateException("Legacy filling converts 250 bottle into four");
        if(checked!=8)throw new IllegalStateException("Incomplete diesel recipe catalog");
        LOGGER.info("DIESEL_CRAFTING_CHECKPOINT_SUCCESS {}","{\"platform\":\"forge\",\"recipes\":8,\"actualSerializerRoundTrips\":17,\"bottle250AndVessel1000\":true,\"emptyContainersReturned\":true,\"legacyFillingStill1000\":true,\"rejectEarlyOil\":true,\"nonMirrorShape\":true,\"playerCraftingClickVerified\":false}");
    }

    @SuppressWarnings({"unchecked","rawtypes"})
    private static net.minecraft.world.item.crafting.CraftingRecipe dieselPacket(net.minecraft.world.item.crafting.CraftingRecipe recipe) {
        net.minecraft.world.item.crafting.RecipeSerializer serializer=recipe.getSerializer();
        var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {serializer.toNetwork(buffer,recipe);var decoded=(net.minecraft.world.item.crafting.CraftingRecipe)serializer.fromNetwork(recipe.getId(),buffer);if(buffer.readableBytes()!=0)throw new IllegalStateException("Diesel recipe packet trailing bytes");return decoded;}finally{buffer.release();}
    }

    private static ItemStack steamForm(com.gregtech.gregtech.data.MaterialPrefix prefix,com.gregtech.gregtech.api.material.GTMaterial material) {
        var stack=com.gregtech.gregtech.registry.GTItems.getStack(prefix,material,1);
        if(stack.isEmpty())throw new IllegalStateException("Missing exact steam form "+prefix+" / "+material);
        return stack;
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        tick(event.getServer());
    }

    private static void tick(MinecraftServer server) {
        if (!ENABLED || server != activeServer || TERMINAL.get() || stopRequested) return;
        try {
            observedTicks++;
            if (DIESEL_POWER) observeDieselCycle(server.overworld());
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
                throw new IllegalStateException("Server stopped without the requested 200-tick normal lifecycle");
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
        return result;
    }
}
