package gregtech.compat.emi;

import dev.emi.emi.api.stack.EmiStack;

import java.util.ArrayList;
import java.util.Comparator;

/** Apply before EMI bakes its indices and REMI derives its groups. Preserve every stack's data. */
final class EmiCatalogRepair {
    @SuppressWarnings("unchecked")
    static void apply() {
        try {
        // EMI publishes only its API at compile time. Keep this version-specific index bridge isolated.
        var field = Class.forName("dev.emi.emi.registry.EmiStackList").getField("stacks");
        var entries = (java.util.List<EmiStack>) field.get(null);
        var representedFluids = new java.util.HashSet<net.minecraft.world.level.material.Fluid>();
        var genericDisplay = gregtech.content.registry.GTFluidItems.generic();
        for (var stack : entries) {
            var item = stack.getItemStack();
            if (!(item.getItem() instanceof gregtech.content.item.FluidDisplayItemLike)
                    || item.getItem() == genericDisplay) continue;
            var fluid = gregtech.engine.fluid.FluidDisplayBinding.resolveCurrent(item);
            if (!fluid.isEmpty()) representedFluids.add(fluid.getFluid());
        }
        var original = entries.stream()
                // Only replace GT fluids. Vanilla water/lava remain searchable beside their display items.
                .filter(stack -> !(stack.getKey() instanceof net.minecraft.world.level.material.Fluid
                        && stack.getItemStack().isEmpty()
                        && representedFluids.contains(stack.getKey())
                        && stack.getId().getNamespace().equals(gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE)))
                .filter(stack -> !(stack.getItemStack().getItem() instanceof
                        gregtech.content.block.misc.LegacyConstructionBlockItem)).toList();
        var sorted = new ArrayList<EmiStack>();
        for (var stack : original) {
            if (!stack.getId().getNamespace().equals(gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE)) continue;
            // Fluids without a GT display item stay native; the proxy adds the creative carrier.
            if (nativeFluid(stack)) {
                sorted.add(new NativeFluidEmiStack(stack));
                continue;
            }
            var item = stack.getItemStack();
            // Mirror the creative tab: EMI gives charged top-quality electric tools for testing.
            if (item.getItem() instanceof gregtech.content.item.ElectricToolItem electric) {
                sorted.add(EmiStack.of(electric.assembledCharged(gregtech.content.material.Materials.Adamantium,
                        electric.getEnergyCapacity(item, gregtech.content.data.GregTechTags.Energy.EU))));
                continue;
            }
            var fluid = gregtech.engine.fluid.FluidDisplayBinding.resolveCurrent(item);
            sorted.add(fluid.isEmpty() ? stack : new FluidDisplayEmiStack(
//? if >=1.21.1 {
                    /*EmiStack.of(fluid.getFluid(), fluid.getComponentsPatch(), fluid.getAmount()), stack, true));
*///?} else {
                    EmiStack.of(fluid.getFluid(), fluid.getTag(), fluid.getAmount()), stack, true));
//?}
        }
        var order = new java.util.IdentityHashMap<EmiStack,String>();
        for (var stack : sorted) order.put(stack, order(stack));
        sorted.sort(Comparator.comparing(order::get));
        // Addon fluid display items now live in the addon namespace; wrap them in place so EMI
        // shows the fluid instead of its carrier item, without pulling them into GT's sort order.
        var extras = new java.util.IdentityHashMap<EmiStack,EmiStack>();
        for (var stack : original) {
            if (stack.getId().getNamespace().equals(gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE)) continue;
            // Vanilla water/lava and addon fluids keep their native identity, wrapped in place.
            if (nativeFluid(stack)) {
                extras.put(stack, new NativeFluidEmiStack(stack));
                continue;
            }
            var item = stack.getItemStack();
            var fluid = gregtech.engine.fluid.FluidDisplayBinding.resolveCurrent(item);
            if (fluid.isEmpty()) continue;
            extras.put(stack, new FluidDisplayEmiStack(
//? if >=1.21.1 {
                    /*EmiStack.of(fluid.getFluid(), fluid.getComponentsPatch(), fluid.getAmount()), stack, true));
*///?} else {
                    EmiStack.of(fluid.getFluid(), fluid.getTag(), fluid.getAmount()), stack, true));
//?}
        }
        // Keep other mods and vanilla in place, replacing just the GT subsequence.
        var result = new ArrayList<EmiStack>(original.size());
        var next = sorted.iterator();
        for (var stack : original) {
            if (stack.getId().getNamespace().equals(gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE)) {
                result.add(next.next());
            } else {
                var wrapped = extras.get(stack);
                result.add(wrapped == null ? stack : wrapped);
            }
        }
        field.set(null, result);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("EMI index integration needs updating", failure);
        }
    }
    /** Fluids that EMI indexes natively, i.e. without any item form of their own. */
    private static boolean nativeFluid(EmiStack stack) {
        return stack.getKey() instanceof net.minecraft.world.level.material.Fluid && stack.getItemStack().isEmpty();
    }

    private static String order(EmiStack stack) {
        String path = stack.getId().getPath();
        // A native fluid keeps the fluid group even while its proxy exposes a creative carrier.
        if (stack instanceof NativeFluidEmiStack) return gregtech.content.creative.EmiCatalogOrder.key(path, "fluids");
        var item = stack.getItemStack();
        if (item.getItem() instanceof net.minecraft.world.item.BlockItem block
                && block.getBlock() instanceof gregtech.content.block.machine.BasicMachineBlock machine) {
            var spec = machine.basicSpec();
            return gregtech.content.creative.EmiCatalogOrder.machineKey(path, spec.machineName(), spec.tier(),
                    gregtech.content.multiblock.LargeMachineLayouts.cells(spec.machineName()) != null);
        }
        return gregtech.content.creative.EmiCatalogOrder.key(path, item.isEmpty() ? "fluids"
                : gregtech.bootstrap.registration.OriginCreativeContents.family(item.getItem()));
    }

}
