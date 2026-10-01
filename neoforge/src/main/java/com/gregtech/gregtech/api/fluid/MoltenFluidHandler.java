package com.gregtech.gregtech.api.fluid;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.data.RegisteredFluids;
import com.gregtech.gregtech.registry.GTFluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Actual crucible fluid capability: catalog identity, Kelvin and exact GT-unit transfer. */
public final class MoltenFluidHandler implements IFluidHandler {
    private final MoltenMaterialStorage storage;
    public MoltenFluidHandler(MoltenMaterialStorage storage) { this.storage = storage; }
    public int getTanks() { return 1; }
    public int getTankCapacity(int tank) { return tank == 0 ? MoltenFluidPlans.millibuckets(storage.getMoltenCapacityUnits()) : 0; }
    private static GTMaterial material(FluidStack fluid) {
        if (fluid == null || fluid.isEmpty()) return null;
        var entry = GTFluids.entryForFluid(fluid.getFluid());
        if (entry == null || entry.textureMode() != RegisteredFluids.FluidTextureMode.GENERIC_MOLTEN) return null;
        String key = entry.materialKey() != null ? entry.materialKey() : RegisteredFluids.boundMaterial(entry.registryName());
        if (key == null) key = entry.registryName().replaceFirst("^(molten[._]|liquid[._])", "");
        GTMaterial material = GTMaterialRegistry.get(key).resolve();
        return material.isValid() ? material : null;
    }
    private static FluidStack fluid(GTMaterial material, int amount) {
        for (var entry : RegisteredFluids.all().entrySet()) {
            var definition = entry.getValue();
            if (definition.textureMode() != RegisteredFluids.FluidTextureMode.GENERIC_MOLTEN) continue;
            var stack = GTFluids.stack(entry.getKey(), amount);
            if (stack != null && !stack.isEmpty() && material(stack) == material.resolve()) return stack;
        }
        return FluidStack.EMPTY;
    }
    public boolean isFluidValid(int tank, FluidStack fluid) { return tank == 0 && material(fluid) != null; }
    public FluidStack getFluidInTank(int tank) { return tank == 0 ? drain(Integer.MAX_VALUE, FluidAction.SIMULATE) : FluidStack.EMPTY; }
    public int fill(FluidStack resource, FluidAction action) {
        var material = material(resource);
        return material == null ? 0 : storage.fillMoltenMaterial(material, resource.getAmount(), resource.getFluid().getFluidType().getTemperature(resource), action.execute());
    }
    public FluidStack drain(FluidStack resource, FluidAction action) {
        var material = material(resource);
        if (material == null) return FluidStack.EMPTY;
        int planned = storage.drainMoltenMaterial(material, resource.getAmount(), false);
        if (planned <= 0) return FluidStack.EMPTY;
        FluidStack result = resource.copy(); result.setAmount(planned);
        if (action.execute()) storage.drainMoltenMaterial(material, planned, true);
        return result;
    }
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (maxDrain <= 0) return FluidStack.EMPTY;
        var contents = storage.getContentView().stream().filter(stack -> stack.amount > 0 && storage.getTemperature() >= stack.material.getMeltingPoint())
                .sorted(java.util.Comparator.comparingDouble(stack -> stack.material.getDensity())).toList();
        for (var entry : contents) {
            int planned = storage.drainMoltenMaterial(entry.material, maxDrain, false);
            if (planned <= 0) continue;
            FluidStack result = fluid(entry.material, planned);
            if (result.isEmpty()) continue;
            if (action.execute()) storage.drainMoltenMaterial(entry.material, planned, true);
            return result;
        }
        return FluidStack.EMPTY;
    }
}
