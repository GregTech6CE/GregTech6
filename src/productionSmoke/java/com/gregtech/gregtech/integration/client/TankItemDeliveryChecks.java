package com.gregtech.gregtech.integration.client;

import com.google.gson.JsonObject;
import com.gregtech.gregtech.block.machine.TankBlock;
import com.gregtech.gregtech.blockentity.machine.TankBlockEntity;
import com.gregtech.gregtech.api.material.ItemMaterialRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;

/** Ordinary installed-JAR method checks at the title screen; no world/harvest claim. */
final class TankItemDeliveryChecks {
    private static int quantityMismatches;
    private static void require(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
    static JsonObject verify() {

        int clean = 0, stored = 0, emptyLoaderWrappers = 0;
        for (var block : BuiltInRegistries.BLOCK) if (block instanceof TankBlock) {
            var tank = (TankBlockEntity) ((TankBlock) block).newBlockEntity(BlockPos.ZERO, block.defaultBlockState());
            CompoundTag empty = tank.saveItemData();
            require(empty.isEmpty(), "installed sparse empty tank item data " + BuiltInRegistries.BLOCK.getKey(block));
            var full = tank.saveWithoutMetadata();
            require(full.contains("gt.temperature") && full.contains("gt.tank"), "installed complete world save");
            for (String key : java.util.List.of("ForgeCaps", "ForgeData"))
                if (full.contains(key, net.minecraft.nbt.Tag.TAG_COMPOUND) && full.getCompound(key).isEmpty()) emptyLoaderWrappers++;
            tank.getFluidTank().fill(new net.minecraftforge.fluids.FluidStack(Fluids.WATER, 61), net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            CompoundTag data = tank.saveItemData();
            require(data.contains("gt.tank") && data.getCompound("gt.tank").getLong("Amount") == 61, "installed tank contents retained");
            var item = new ItemStack(block);
            item.addTagElement("BlockEntityTag", data);
            require(ItemMaterialRegistry.hasStoredContents(item) && !ItemMaterialRegistry.canRecover(item), "installed stored shell guard");
            require(item.getMaxStackSize() == 1, "installed filled barrel stack limit");
            var tooltipData = data.copy();
            tooltipData.getCompound("gt.tank").putLong("Capacity", 14789);
            tooltipData.putBoolean("gt.soft_hammer", true);
            tooltipData.putLong("gt.sealed_time", 91);
            var tooltipItem = new ItemStack(block);
            tooltipItem.addTagElement("BlockEntityTag", tooltipData);
            verifyStoredTankTooltip(tooltipItem);
            clean++; stored++;
        }
        require(clean == 35 && stored == 35, "installed existing tank identities");
        int cheap = 0;
        for (var entry : com.gregtech.gregtech.content.transport.fluid.CheapWoodBarrelCatalog.ENTRIES) {
            var item = new ItemStack(BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("gregtech:" + entry.id())));
            require(item.getMaxStackSize() == 16, "installed empty barrel stack limit");
            var block = (TankBlock) ((net.minecraft.world.item.BlockItem) item.getItem()).getBlock();
            require(block.spec().capacity() == 8000 && block.spec().maxTemperature() == 340, "installed cheap barrel specification");
            var data = ItemMaterialRegistry.get(item).orElseThrow();
            require(data.components().size() == 2 && data.components().stream().anyMatch(c -> c.material() == entry.rod() && c.amount() == com.gregtech.gregtech.api.material.GTValues.U * 2), "installed distinct cheap rod composition");
            require(com.gregtech.gregtech.content.creative.SourceCreativeCatalog.entry(entry.id()) != null, "installed source creative entry");
            var model = net.minecraft.client.Minecraft.getInstance().getItemRenderer().getModel(item, null, null, 0);
            require(model != net.minecraft.client.Minecraft.getInstance().getModelManager().getMissingModel(), "installed cheap barrel item model");
            cheap++;
        }
        var logistics = new ItemStack(BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("gregtech:logistics_tank")));
        require(ItemMaterialRegistry.get(logistics).orElseThrow().components().size() == 7, "installed complete known logistics composition");
        var result = new JsonObject();
        result.addProperty("cheapSourceVariants", cheap);
        result.addProperty("knownLogisticsComponents", 7);
        result.addProperty("cleanItemData", clean);
        result.addProperty("storedItemDataProtected", stored);
        result.addProperty("completeWorldSaveRetained", true);
        result.addProperty("observedEmptyLoaderWrappers", emptyLoaderWrappers);
        result.addProperty("scope", "installed methods at title screen; actual harvest tested separately in native server worlds");
        result.add("originalFunctionalTooltips", verifyFunctionalTooltips());
        result.add("sourceMechanicalTooltips", verifyMechanicalTooltips());
        result.add("sourceEnergyDeviceTooltips", verifyEnergyDeviceTooltips());
        result.add("sourceRotaryConverters", verifyRotaryConverterDelivery());
        result.add("machineSourceMaterials", verifyMachineMaterials());
        result.addProperty("sourceFluidPipeTooltips", verifyFluidPipeTooltips());
        result.addProperty("storedTankTooltips", stored);
        return result;
    }

    private static JsonObject verifyMachineMaterials() {
        int blocks = 0, machines = 0;
        for (var entry : com.gregtech.gregtech.content.machine.MachineConstructionMaterials.blocks().entrySet()) {
            var id = net.minecraft.resources.ResourceLocation.parse("gregtech:" + entry.getKey());
            require(BuiltInRegistries.ITEM.containsKey(id), "installed source material item exists " + id);
            verifyComposition(new ItemStack(BuiltInRegistries.ITEM.get(id)), entry.getValue());
            blocks++;
        }
        for (var block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof com.gregtech.gregtech.block.machine.BasicMachineBlock machine)) continue;
            var spec = machine.basicSpec();
            var expected = com.gregtech.gregtech.content.machine.OriginalMachineMaterialData.find(spec.machineName(), spec.tier());
            if (expected.isEmpty()) continue;
            verifyComposition(new ItemStack(block), expected.get());
            machines++;
        }
        var out = new JsonObject();
        require(quantityMismatches == 0, "installed source quantity tooltip mismatches " + quantityMismatches + "; see diagnostic lines");
        out.addProperty("sourceBlockIdentities", blocks);
        out.addProperty("nativeBasicMachinesAndControllers", machines);
        out.addProperty("singleAdvancedMaterialSection", true);
        out.addProperty("exactSourceComponentAmounts", true);
        out.addProperty("scope", "installed item methods and tooltip event at title screen; no recovery gameplay claim");
        return out;
    }
    private static void verifyComposition(ItemStack stack, com.gregtech.gregtech.api.material.ItemComposition expected) {
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        var actual = ItemMaterialRegistry.get(stack).orElseThrow(() -> new IllegalStateException("installed source material data " + id));
        require(actual.components().equals(expected.components()), "installed exact source components " + id);
        var lines = tooltip(stack, net.minecraft.world.item.TooltipFlag.ADVANCED);
        String header = net.minecraft.network.chat.Component.translatable("tooltip.gregtech.contained_materials").getString();
        require(lines.stream().filter(header::equals).count() == 1, "installed single material header " + id);
        for (var part : expected.components()) {
            String prefix = com.gregtech.gregtech.client.MaterialTooltips.displayUnits(part.amount()) + " "
                    + com.gregtech.gregtech.api.material.MaterialPresentation.name(part.material()).getString();
            if (lines.stream().filter(line -> line.startsWith(prefix + " (")).count() != 1) {
                quantityMismatches++;
                System.err.println("SOURCE_QUANTITY_TOOLTIP_MISMATCH " + id + " expected " + prefix + "; actual " + lines);
            }
        }
        require(tooltip(stack, net.minecraft.world.item.TooltipFlag.NORMAL).stream().noneMatch(header::equals),
                "installed quantities require advanced tooltips " + id);
    }
    private static void verifyStoredTankTooltip(ItemStack item) {
        var lines = tooltip(item, net.minecraft.world.item.TooltipFlag.NORMAL);
        require(lines.stream().anyMatch(line -> line.replace(",", "").replace("_", "").contains("61 L of ")
                && line.replace(",", "").replace("_", "").contains("Max: 14789 L)")), "installed saved barrel amount/capacity tooltip");
        require(lines.contains("Sealed (91)"), "installed saved barrel seal tooltip");
        for (String key : java.util.List.of("gt.lang.nogui.funnel.tap.tank", "gt.lang.no.powerconducting.fluids"))
            require(lines.contains(net.minecraft.network.chat.Component.translatable(key).getString()), "installed barrel original hint " + key);
    }
    private static int verifyFluidPipeTooltips() {
        int pipes = 0;
        for (var block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof com.gregtech.gregtech.block.machine.FluidPipeBlock pipe)) continue;
            var lines = tooltip(new ItemStack(block), net.minecraft.world.item.TooltipFlag.NORMAL);
            String bandwidth = net.minecraft.network.chat.Component.translatable("gt.lang.pipe.stats.bandwidth").getString()
                    + pipe.spec().bandwidthPerTank() + " L/t";
            require(lines.stream().anyMatch(line -> line.replace(",", "").replace("_", "").equals(bandwidth)), "installed per-tank source bandwidth");
            if (pipe.spec().tankCount() > 1) require(lines.contains(net.minecraft.network.chat.Component.translatable("gt.lang.pipe.stats.amount").getString()
                    + pipe.spec().tankCount()), "installed source channel count");
            String connection = net.minecraft.network.chat.Component.translatable("gt.lang.use.x.to.toggle.connection.pre")
                    .append(net.minecraft.network.chat.Component.translatable("gt.lang.tool.name.wrench"))
                    .append(net.minecraft.network.chat.Component.translatable("gt.lang.use.x.to.toggle.connection.post")).getString();
            require(lines.contains(connection), "installed source wrench pipe connection hint");
            pipes++;
        }
        require(pipes == 280 && pipes == com.gregtech.gregtech.content.transport.fluid.FluidTransportDefinitions.pipes().size(),
                "installed280 fluid pipe tooltip identities;126 item pipes are counted separately");
        return pipes;
    }
    private static java.util.List<String> tooltip(ItemStack stack, net.minecraft.world.item.TooltipFlag flag) {
        return stack.getTooltipLines(null, flag).stream().map(net.minecraft.network.chat.Component::getString).toList();
    }
    private static com.google.gson.JsonObject verifyFunctionalTooltips() {
        var result = new com.google.gson.JsonObject();
        var ids = java.util.List.of("mortar_block", "mortar_netherite", "mortar_sapphire", "mortar_diamond", "mortar_amethyst",
                "grindstone_block", "sifting_table", "mixing_bowl", "mixing_bowl_table", "juicer", "bathing_pot",
                "bathing_pot_table", "bathing_pot_wood", "bathing_pot_table_wood");
        for (String id : ids) {
            var item = new ItemStack(BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("gregtech:" + id)));
            var lines = tooltip(item, net.minecraft.world.item.TooltipFlag.NORMAL);
            var source = com.gregtech.gregtech.content.machine.OriginalFunctionalTooltipData.manual(id);
            String recipes = tr("gt.lang.recipes") + ": " + tr(source.recipeKey());
            require(lines.stream().filter(recipes::equals).count() == 1, "installed original manual recipe header " + id);
            require(lines.contains(tr(source.usageKey())), "installed original manual usage " + id);
            require(lines.contains(tr("gt.lang.nogui.rightclick.interact") + " (" + tr(source.faceKey()) + ")"), "installed original interaction face " + id);
            require(lines.contains(tr("gt.lang.use.magnifyingglass.to.detail")) == source.magnifier(), "installed source magnifier presence " + id);
            if (source.preparationKey() != null) require(lines.contains(tr(source.preparationKey())), "installed original preparation " + id);
            var block = ((net.minecraft.world.item.BlockItem) item.getItem()).getBlock();
            String blast = tr("gt.lang.blastresistance") + com.gregtech.gregtech.api.block.OriginalBlockTooltipRules.blastNumber(block.getExplosionResistance());
            require(lines.stream().anyMatch(s -> s.startsWith(blast)), "installed manual blast resistance " + id);
            require(lines.stream().noneMatch(s -> s.contains("tooltip.gregtech.manual.")), "installed no unresolved old manual key " + id);
        }
        int boilers = 0;
        for (var block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof com.gregtech.gregtech.block.machine.BoilerTankBlock boiler)) continue;
            var item = new ItemStack(block);
            var lines = tooltip(item, net.minecraft.world.item.TooltipFlag.NORMAL);
            for (String key : java.util.List.of("gt.lang.requirement.water.pure", "gt.lang.nogui.funnel.tank",
                    "gt.lang.hazard.explosion.steam", "gt.lang.hazard.meltdown", "gt.lang.use.chisel.to.decalcify", "gt.lang.use.magnifyingglass.to.detail"))
                require(lines.stream().filter(tr(key)::equals).count() == 1, "installed original boiler requirement/hazard " + key);
            require(lines.contains(tr("gt.lang.efficiency") + ": 100.00%"), "installed default boiler efficiency");
            require(lines.contains(tr("gt.lang.energy.input") + ": " + boiler.spec().heatInputRecommended() + " " + tr("gt.td.short.energy.heat") + "/t (" + tr("gt.lang.face.any") + ")"), "installed source HU input");
            require(lines.contains(tr("gt.lang.energy.capacity") + ": " + boiler.spec().steamCapacity() + " " + tr("gt.td.long.energy.steam")), "installed source Steam capacity");
            var tag = new CompoundTag();
            tag.putShort("gt.efficiency", (short) 5000);
            item.addTagElement("BlockEntityTag", tag);
            var saved = tooltip(item, net.minecraft.world.item.TooltipFlag.NORMAL);
            require(saved.contains(tr("gt.lang.efficiency") + ": 50.00%"), "installed stored boiler efficiency");
            require(saved.contains(tr("gt.lang.energy.output") + ": " + (boiler.spec().steamOutput() / 2) + " " + tr("gt.td.long.energy.steam") + "/t (" + tr("gt.lang.face.top") + ")"), "installed stored source Steam output");
            boilers++;
        }
        require(boilers == 26, "installed source boiler identities");
        result.addProperty("manualStations", ids.size());
        result.addProperty("boilersDefaultAndSavedEfficiency", boilers);
        result.addProperty("scope", "installed actual item tooltip methods at title screen; no player hover or machine operation claim");
        return result;
    }
    private static String tr(String key) { return net.minecraft.network.chat.Component.translatable(key).getString(); }


    private static JsonObject verifyEnergyDeviceTooltips() {
        int batteries = 0, solar = 0, electric = 0, rotation = 0, reversed = 0;
        for (var block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof com.gregtech.gregtech.block.energy.EnergyNodeBlock node)
                    || !com.gregtech.gregtech.content.energy.OriginalEnergyDeviceTooltipData.handles(node.spec())
                    || com.gregtech.gregtech.content.energy.OriginalRotaryConverter.handles(node.spec())) continue;
            var spec = node.spec();
            var item = new ItemStack(node);
            var source = com.gregtech.gregtech.content.energy.OriginalEnergyDeviceTooltipData.profile(spec, false);
            var lines = tooltip(item, net.minecraft.world.item.TooltipFlag.NORMAL);
            verifySourceEnergyLines(spec, source, lines);
            String wrench = tr("gt.lang.use.x.to.toggle.facing.pre") + tr("gt.lang.tool.name.wrench") + tr("gt.lang.use.x.to.toggle.facing.post");
            require(lines.stream().filter(wrench::equals).count() == 1, "installed one source energy-device facing hint " + spec.id());
            var raw = node.newBlockEntity(net.minecraft.core.BlockPos.ZERO, node.defaultBlockState());
            require(raw instanceof com.gregtech.gregtech.blockentity.energy.EnergyNodeBlockEntity, "installed actual node factory " + spec.id());
            var tile = (com.gregtech.gregtech.blockentity.energy.EnergyNodeBlockEntity) raw;
            verifyNativeEnergyStats(tile, source);
            if (source.batteryModes()) {
                for (String key : java.util.List.of("gt.tooltip.energybattery.1", "gt.tooltip.energybattery.2", "gt.tooltip.energybattery.3"))
                    require(lines.stream().filter(tr(key)::equals).count() == 1, "installed battery selector mode hint " + spec.id());
                batteries++;
            } else if (spec.kind() == com.gregtech.gregtech.api.energy.EnergyNodeSpec.Kind.SOLAR) solar++;
            else {
                require(lines.contains(tr("gt.lang.efficiency") + ": 100.00%"), "installed original transformer efficiency " + spec.id());
                require(lines.stream().filter(tr("gt.lang.use.monkey.wrench.to.toggle.direction")::equals).count() == 1, "installed original Monkey Wrench hint " + spec.id());
                var tag = new CompoundTag();
                tag.putBoolean("gt.inverted", true);
                item.addTagElement("BlockEntityTag", tag);
                var reverse = com.gregtech.gregtech.content.energy.OriginalEnergyDeviceTooltipData.profile(spec, true);
                verifySourceEnergyLines(spec, reverse, tooltip(item, net.minecraft.world.item.TooltipFlag.NORMAL));
                require(tile.toggleInverted(), "installed original converter direction toggled " + spec.id());
                verifyNativeEnergyStats(tile, reverse);
                reversed++;
                if (spec.id().startsWith("rotation_transformer_")) rotation++; else electric++;
            }
        }
        require(batteries == 20 && solar == 2 && electric == 9 && rotation == 13 && reversed == 22, "installed energy-device family coverage");
        var out = new JsonObject();
        out.addProperty("batteryBoxes", batteries);
        out.addProperty("solarPanels", solar);
        out.addProperty("electricTransformers", electric);
        out.addProperty("rotationalTransformers", rotation);
        out.addProperty("savedDirectionItemTooltipsAndNativeToggleStatistics", reversed);
        out.addProperty("scope", "installed item methods and native factory rating getters at title screen; no energy transfer or saved-world restart claim");
        return out;
    }

    private static void verifyNativeEnergyStats(com.gregtech.gregtech.blockentity.energy.EnergyNodeBlockEntity tile,
                                                com.gregtech.gregtech.content.energy.OriginalEnergyDeviceTooltipData.Profile source) {
        var spec = tile.spec();
        if (source.input() != null)
            require(tile.getEnergySizeInputMin(spec.inType(), null) == source.input().minimum()
                    && tile.getEnergySizeInputRecommended(spec.inType(), null) == source.input().recommended()
                    && tile.getEnergySizeInputMax(spec.inType(), null) == source.input().maximum(), "installed native original input range " + spec.id());
        require(tile.getEnergySizeOutputMin(spec.outType(), null) == source.output().minimum()
                && tile.getEnergySizeOutputRecommended(spec.outType(), null) == source.output().recommended()
                && tile.getEnergySizeOutputMax(spec.outType(), null) == source.output().maximum(), "installed native original output range " + spec.id());
    }

    private static void verifySourceEnergyLines(com.gregtech.gregtech.api.energy.EnergyNodeSpec spec,
                                                com.gregtech.gregtech.content.energy.OriginalEnergyDeviceTooltipData.Profile source,
                                                java.util.List<String> lines) {
        if (source.input() != null) {
            String expected = sourceEnergyLine("gt.lang.energy.input", source.input(), sourceEnergyUnit(spec.inType()), source.inputFaceKey(), source.alwaysShowRange());
            require(lines.stream().filter(expected::equals).count() == 1, "installed single source energy input " + spec.id());
        } else require(lines.stream().noneMatch(s -> s.startsWith(tr("gt.lang.energy.input") + ":")), "installed solar has no invented input");
        String expected = sourceEnergyLine("gt.lang.energy.output", source.output(), sourceEnergyUnit(spec.outType()), source.outputFaceKey(), source.alwaysShowRange());
        require(lines.stream().filter(expected::equals).count() == 1, "installed single source energy output " + spec.id());
    }

    private static String sourceEnergyLine(String key, com.gregtech.gregtech.content.energy.OriginalEnergyDeviceTooltipData.Stats stats,
                                          String unit, String faceKey, boolean alwaysRange) {
        String text = tr(key) + ": " + stats.recommended() + " " + unit + "/t";
        if (alwaysRange || stats.minimum() != stats.recommended() || stats.maximum() != stats.recommended())
            text += (stats.minimum() <= 1 ? " (up to " : " (" + stats.minimum() + " to ")
                    + stats.maximum() + ", " + tr(faceKey) + ")";
        return text;
    }


    private static JsonObject verifyRotaryConverterDelivery() {
        int motors = 0, dynamos = 0, states = 0;
        for (var block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof com.gregtech.gregtech.block.energy.EnergyNodeBlock node)
                    || !com.gregtech.gregtech.content.energy.OriginalRotaryConverter.handles(node.spec())) continue;
            var spec = node.spec();
            boolean motor = com.gregtech.gregtech.content.energy.OriginalRotaryConverter.motor(spec);
            require(block instanceof com.gregtech.gregtech.block.energy.RotaryConverterBlock, "installed converter visual state block " + spec.id());
            require((block instanceof com.gregtech.gregtech.block.energy.OriginalMotorBlock) == motor, "installed separate motor direction state " + spec.id());
            var item = new ItemStack(block);
            var lines = tooltip(item, net.minecraft.world.item.TooltipFlag.NORMAL);
            var source = com.gregtech.gregtech.content.energy.OriginalEnergyDeviceTooltipData.profile(spec, false);
            verifySourceEnergyLines(spec, source, lines);
            require(lines.contains(tr("gt.lang.efficiency") + (motor ? ": 50.00%" : ": 68.75%")), "installed original rotary efficiency " + spec.id());
            require(lines.stream().filter(tr("gt.lang.use.monkey.wrench.to.toggle.direction")::equals).count() == (motor ? 1 : 0),
                    "installed source motor-only reversal hint " + spec.id());
            if (spec.inType() == com.gregtech.gregtech.data.GregTechTags.Energy.RF)
                require(lines.contains(tr("gt.lang.accepts.redstoneflux.lossless")), "installed RF acceptance hint " + spec.id());
            if (spec.outType() == com.gregtech.gregtech.data.GregTechTags.Energy.RF)
                require(lines.contains(tr("gt.lang.emits.redstoneflux.lossless")), "installed RF emission hint " + spec.id());
            var material = com.gregtech.gregtech.content.machine.MachineConstructionMaterials.blocks().get(spec.id());
            require(material != null, "installed source converter material registration " + spec.id());
            verifyComposition(item, material);
            var tile = (com.gregtech.gregtech.blockentity.energy.EnergyNodeBlockEntity) node.newBlockEntity(BlockPos.ZERO, block.defaultBlockState());
            require(tile.isOriginalRotaryConverter(), "installed native converter route " + spec.id());
            verifyNativeEnergyStats(tile, source);
            require(tile.getEnergyCapacity(spec.inType(), null) == spec.inputRate() * 2, "installed source two-input buffer " + spec.id());
            var front = block.defaultBlockState().getValue(net.minecraft.world.level.block.DirectionalBlock.FACING);
            for (var face : net.minecraft.core.Direction.values()) {
                require(tile.isEnergyAcceptingFrom(spec.inType(), face, false) == (motor ? face != front : face == front.getOpposite()), "installed actual input face " + spec.id());
                require(tile.isEnergyEmittingTo(spec.outType(), face, false) == (face == front), "installed actual output face " + spec.id());
            }
            require(tile.doEnergyInjection(spec.inType(), front.getOpposite(), spec.inputRate(), Long.MAX_VALUE, false) == 2 && tile.stored() == 0,
                    "installed source simulation is bounded and pure " + spec.id());
            if (spec.inType() != com.gregtech.gregtech.data.GregTechTags.Energy.RF) {
                require(tile.doEnergyInjection(spec.inType(), front.getOpposite(), source.input().minimum() - 1, 3, true) == 3 && tile.stored() == 0,
                        "installed root consumes undersized packets without storage " + spec.id());
            }
            require(tile.doEnergyInjection(spec.inType(), front.getOpposite(), spec.inputRate(), 1, true) == 1 && tile.stored() == spec.inputRate(),
                    "installed native rated packet storage " + spec.id());
            if (motor) require(tile.reverseMotor() && tile.stored() == 0, "installed motor reversal clears input storage " + spec.id());
            var control = tile.machineControl(null);
            require(control.supportsMode() == motor && !control.supportsProgress(), "installed original control interfaces " + spec.id());
            control.setEnabled(false);
            require(!tile.isEnergyAcceptingFrom(spec.inType(), front.getOpposite(), false)
                    && tile.isEnergyAcceptingFrom(spec.inType(), front.getOpposite(), true), "installed stopped vs theoretical gate " + spec.id());
            require(tile.getEnergyDemanded(spec.inType(), null, spec.inputRate()) == 0
                    && tile.getEnergyOffered(spec.outType(), null, spec.outputRate()) == 0, "installed push-driven source discovery " + spec.id());
            for (var state : block.getStateDefinition().getPossibleStates()) {
                var model = net.minecraft.client.Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
                require(model != net.minecraft.client.Minecraft.getInstance().getModelManager().getMissingModel(), "installed converter state has baked model " + spec.id() + state);
                var quads = new java.util.ArrayList<net.minecraft.client.renderer.block.model.BakedQuad>();
                for (var face : net.minecraft.core.Direction.values())
                    quads.addAll(model.getQuads(state, face, net.minecraft.util.RandomSource.create(0)));
                quads.addAll(model.getQuads(state, null, net.minecraft.util.RandomSource.create(0)));
                boolean active = state.getValue(com.gregtech.gregtech.block.energy.RotaryConverterBlock.ACTIVITY) > 0;
                String folder = !active ? "/overlay/" : !motor ? "/overlay_active/"
                        : "/overlay_active_" + (state.getValue(com.gregtech.gregtech.block.energy.OriginalMotorBlock.COUNTER_CLOCKWISE) ? "l" : "r")
                        + (state.getValue(com.gregtech.gregtech.block.energy.OriginalMotorBlock.FAST) ? "f/" : "s/");
                require(quads.stream().anyMatch(q -> q.getSprite().contents().name().getPath().contains(folder)),
                        "installed actual active/inactive overlay baked " + spec.id() + state);
                require(quads.stream().noneMatch(q -> q.getSprite().contents().name().getPath().equals("missingno")), "installed converter layer textures exist " + spec.id());
                states++;
            }
            if (motor) motors++; else dynamos++;
        }
        require(motors == 7 && dynamos == 7 && states == 1260, "installed complete rotary converter families and visual states");
        var result = new JsonObject();
        result.addProperty("motors", motors); result.addProperty("dynamos", dynamos); result.addProperty("actualBakedStates", states);
        result.addProperty("exactMaterialsAndSingleAdvancedSection", true);
        result.addProperty("scope", "installed title-screen native factories, methods, tooltip events and baked models; actual world chain checked separately");
        return result;
    }

    private static String sourceEnergyUnit(com.gregtech.gregtech.data.GregTechTags.Tag type) {
        return tr(type == com.gregtech.gregtech.data.GregTechTags.Energy.RU ? "gt.td.short.energy.kinetic_rotation"
                : type == com.gregtech.gregtech.data.GregTechTags.Energy.RF ? "gt.td.short.energy.redstone_flux" : "gt.td.short.energy.electricity");
    }

    private static JsonObject verifyMechanicalTooltips() {
        int axles = 0, gearboxes = 0, largeBoilers = 0;
        for (var block : BuiltInRegistries.BLOCK) {
            var item = new ItemStack(block);
            if (block instanceof com.gregtech.gregtech.block.energy.AxleBlock axle) {
                var lines = tooltip(item, net.minecraft.world.item.TooltipFlag.NORMAL);
                require(lines.contains(tr("gt.lang.axle.stats.speed") + axle.spec().maxSpeed() + " " + tr("gt.td.short.energy.kinetic_rotation")), "installed original axle speed");
                require(lines.contains(tr("gt.lang.axle.stats.power") + axle.spec().maxPower()), "installed original axle power");
                String wrench = tr("gt.lang.use.x.to.toggle.connection.pre") + tr("gt.lang.tool.name.wrench") + tr("gt.lang.use.x.to.toggle.connection.post");
                require(lines.contains(wrench), "installed original axle connection hint");
                axles++;
            } else if (block instanceof com.gregtech.gregtech.block.energy.GearboxBlock gearbox) {
                String warning = tr("gt.tooltip.gearbox.custom.1");
                var lines = tooltip(item, net.minecraft.world.item.TooltipFlag.NORMAL);
                require(!lines.contains(warning), "installed empty gearbox suppresses wrong interlock warning");
                require(lines.contains(tr("gt.lang.axle.stats.speed") + gearbox.spec().maxSpeed() + " " + tr("gt.td.short.energy.kinetic_rotation")), "installed original gearbox speed");
                for (String key : java.util.List.of("gt.tooltip.gearbox.custom.2", "gt.tooltip.gearbox.custom.3", "gt.lang.use.soft.hammer.to.toggle", "gt.lang.use.magnifyingglass.to.detail"))
                    require(lines.contains(tr(key)), "installed original gearbox tool hint");
                var tag = new CompoundTag();
                tag.putByte("gearMask", (byte) 3);
                tag.putByte("axisCode", (byte) 0);
                item.addTagElement("BlockEntityTag", tag);
                require(tooltip(item, net.minecraft.world.item.TooltipFlag.NORMAL).contains(warning), "installed saved opposite gears warn");
                tag.putByte("axisCode", (byte) 2);
                item.addTagElement("BlockEntityTag", tag);
                require(!tooltip(item, net.minecraft.world.item.TooltipFlag.NORMAL).contains(warning), "installed saved matching axle clears warning");
                gearboxes++;
            } else if (block instanceof com.gregtech.gregtech.block.machine.OriginalLargeBoilerControllerBlock boiler) {
                var lines = tooltip(item, net.minecraft.world.item.TooltipFlag.NORMAL);
                for (String key : java.util.List.of("gt.tooltip.multiblock.largeboiler.1", "gt.tooltip.multiblock.largeboiler.2", "gt.tooltip.multiblock.largeboiler.3", "gt.tooltip.multiblock.largeboiler.4", "gt.lang.requirement.water.pure", "gt.lang.hazard.explosion.steam", "gt.lang.hazard.meltdown", "gt.lang.use.chisel.to.decalcify", "gt.lang.use.builder.wand.to.ease.building", "gt.lang.use.magnifyingglass.to.detail"))
                    require(lines.stream().filter(tr(key)::equals).count() == 1, "installed complete original large boiler line " + key);
                require(lines.contains(tr("gt.lang.structure") + ":"), "installed original structure header");
                require(lines.contains(tr("gt.lang.energy.capacity") + ": " + boiler.variant().steamCapacity() + " " + tr("gt.td.long.energy.steam")), "installed original large Steam capacity");
                var tag = new CompoundTag();
                tag.putInt("gt.efficiency", 2500);
                item.addTagElement("BlockEntityTag", tag);
                lines = tooltip(item, net.minecraft.world.item.TooltipFlag.NORMAL);
                require(lines.contains(tr("gt.lang.efficiency") + ": 25.00%"), "installed original saved efficiency below natural50percent floor");
                require(lines.contains(tr("gt.lang.energy.output") + ": " + boiler.variant().steamOutput()/4 + " " + tr("gt.td.long.energy.steam") + "/t (Pipe Holes)"), "installed original large boiler saved output");
                largeBoilers++;
            }
        }
        require(axles == 52 && gearboxes == 13 && largeBoilers == 5, "installed original mechanical/large boiler identities");
        var out = new JsonObject();
        out.addProperty("axles", axles);
        out.addProperty("customGearboxesDefaultAndSavedConfigurations", gearboxes);
        out.addProperty("originalLargeBoilersDefaultAndSavedEfficiencies", largeBoilers);
        out.addProperty("scope", "installed item methods at title screen; no mechanical transport or boiler operation claim");
        return out;
    }

}
