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
        var original = ((java.util.List<EmiStack>) field.get(null)).stream()
                .filter(stack -> !(stack.getItemStack().getItem() instanceof
                        gregtech.content.block.misc.LegacyConstructionBlockItem)).toList();
        var sorted = new ArrayList<EmiStack>();
        for (var stack : original) {
            if (!stack.getId().getNamespace().equals("gt6ce")) continue;
            var item = stack.getItemStack();
//? if >=1.21.1 {
            /*var fluid = gregtech.engine.fluid.FluidDisplayBinding.resolveCurrent(item);
*///?} else {
            var fluid = gregtech.engine.fluid.FluidDisplayBinding.resolve(item);
//?}
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
            if (stack.getId().getNamespace().equals("gt6ce")) continue;
            var item = stack.getItemStack();
//? if >=1.21.1 {
            /*var fluid = gregtech.engine.fluid.FluidDisplayBinding.resolveCurrent(item);
*///?} else {
            var fluid = gregtech.engine.fluid.FluidDisplayBinding.resolve(item);
//?}
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
            if (stack.getId().getNamespace().equals("gt6ce")) {
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
    private static String order(EmiStack stack) {
        String path = stack.getId().getPath();
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
