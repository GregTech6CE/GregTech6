package gregtech.engine.fluid;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialProperty;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.content.data.RegisteredFluids;
import gregtech.content.registry.GTFluids;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
//?}

/** Actual crucible fluid capability: catalog identity, Kelvin and exact GT-unit transfer. */
public final class MoltenFluidHandler implements IFluidHandler {
    private final MoltenMaterialStorage storage;
    public MoltenFluidHandler(MoltenMaterialStorage storage) { this.storage = storage; }
    public int getTanks() { return 1; }
    public int getTankCapacity(int tank) {
        if (tank != 0) return 0;
        var displayed = getFluidInTank(0);
        return reportedCapacity(storage.getMoltenCapacityUnits(), material(displayed), displayed);
    }
    /** Empty tanks keep the legacy 144 scale; filled tanks use their displayed phase's dimension.
     * Preserve overfull saved U without reporting an amount larger than capacity.
     */
    public static int reportedCapacity(long units, GTMaterial material, FluidStack displayed) {
        int rated = MoltenFluidPlans.millibuckets(units, ratio(material, displayed));
        return displayed == null || displayed.isEmpty() ? rated : Math.max(rated, displayed.getAmount());
    }
    public static GTMaterial material(FluidStack fluid) {
        if (fluid == null || fluid.isEmpty()) return null;
        if (fluid.getFluid() == net.minecraft.world.level.material.Fluids.WATER
                || fluid.getFluid() == net.minecraft.world.level.material.Fluids.FLOWING_WATER)
            return GTMaterialRegistry.get("Water");
        if (fluid.getFluid() == net.minecraft.world.level.material.Fluids.LAVA
                || fluid.getFluid() == net.minecraft.world.level.material.Fluids.FLOWING_LAVA)
            return GTMaterialRegistry.get("Lava");
        var entry = GTFluids.entryForFluid(fluid.getFluid());
        if (!isMoltenPhase(entry) && !isOrdinaryLiquidPhase(entry)) return null;
        String key = entry.materialKey() != null ? entry.materialKey() : RegisteredFluids.boundMaterial(entry.registryName());
        if (key == null) key = entry.registryName().replaceFirst("^(molten[._]|liquid[._])", "");
        GTMaterial material = GTMaterialRegistry.get(key).resolve();
        return material.isValid() ? material : null;
    }

    private static boolean isMoltenPhase(RegisteredFluids.FluidEntry entry) {
        return entry != null && (entry.textureMode() == RegisteredFluids.FluidTextureMode.GENERIC_MOLTEN
                || RegisteredFluids.isMoltenFactoryDefinition(entry));
    }

    /**
     * A material that is liquid at room temperature (mercury) has an ordinary liquid entry next to its
     * molten ID. GT6 keeps one fluid for such a material, so both IDs address the same crucible content.
     */
    private static boolean isOrdinaryLiquidPhase(RegisteredFluids.FluidEntry entry) {
        if (entry == null || entry.materialKey() == null) return false;
        GTMaterial material = GTMaterialRegistry.get(entry.materialKey()).resolve();
        if (!material.isValid() || !material.has(MaterialProperty.LIQUID)) return false;
        var liquid = RegisteredFluids.get("GenLiquid_" + material.getName());
        return liquid != null
                && liquid.registryName().equals(entry.registryName())
                && RegisteredFluids.get("GenMolten_" + material.getName()) != null;
    }
    /** The special source dimension belongs to this actual phase, not every addon fluid of the same material. */
    public static MoltenMaterialUnits.Ratio ratio(GTMaterial material, FluidStack fluid) {
        if (fluid == null || fluid.isEmpty()) return MoltenMaterialUnits.STANDARD;
        if (fluid.getFluid() == net.minecraft.world.level.material.Fluids.WATER
                || fluid.getFluid() == net.minecraft.world.level.material.Fluids.FLOWING_WATER)
            return MoltenMaterialUnits.BUCKET;
        if (fluid.getFluid() == net.minecraft.world.level.material.Fluids.LAVA
                || fluid.getFluid() == net.minecraft.world.level.material.Fluids.FLOWING_LAVA)
            return MoltenMaterialUnits.LAVA;
        var entry = GTFluids.entryForFluid(fluid.getFluid());
        if (entry == null || material(fluid) != material
                || entry.textureMode() != RegisteredFluids.FluidTextureMode.GENERIC_MOLTEN)
            return MoltenMaterialUnits.STANDARD;
        String actualId = net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString();
        boolean primary = RegisteredFluids.isGeneratedMoltenDefinition("GenMolten_Alumina", entry)
                && entry.registryName().equals("molten.alumina") && actualId.equals("gt6ce:molten_alumina");
        boolean plastic = entry == RegisteredFluids.Plastic && entry == RegisteredFluids.get("Plastic")
                && entry.registryName().equals("molten.plastic") && entry.temperature() == 423
                && actualId.equals("gt6ce:molten_plastic");
        return MoltenMaterialUnits.ratio(material, primary || plastic);
    }

    private static FluidStack fluid(GTMaterial material, int amount) {
        if (material.resolve() == GTMaterialRegistry.get("Water"))
            return new FluidStack(net.minecraft.world.level.material.Fluids.WATER, amount);
        if (material.resolve() == GTMaterialRegistry.get("Lava"))
            return new FluidStack(net.minecraft.world.level.material.Fluids.LAVA, amount);
        if (MoltenMaterialUnits.ratio(material).equals(MoltenMaterialUnits.ALUMINA)) {
            var primary = GTFluids.stack("GenMolten_Alumina", amount);
            if (ratio(material, primary).equals(MoltenMaterialUnits.ALUMINA)) return primary;
        }
        if (MoltenMaterialUnits.ratio(material).equals(MoltenMaterialUnits.PLASTIC)) {
            var primary = GTFluids.stack("Plastic", amount);
            if (ratio(material, primary).equals(MoltenMaterialUnits.PLASTIC)) return primary;
        }
        // The original molten salts and room-temperature liquid metals use these established liquid IDs.
        String liquidField = "GenLiquid_" + material.getName();
        var liquidEntry = RegisteredFluids.get(liquidField);
        if (liquidEntry != null
                && (RegisteredFluids.isMoltenFactoryDefinition(liquidEntry) || isOrdinaryLiquidPhase(liquidEntry))) {
            var primary = GTFluids.stack(liquidField, amount);
            if (material(primary) == material.resolve()) return primary;
        }
        for (var entry : RegisteredFluids.all().entrySet()) {
            var definition = entry.getValue();
            if (!isMoltenPhase(definition)) continue;
            var stack = GTFluids.stack(entry.getKey(), amount);
            if (stack != null && !stack.isEmpty() && material(stack) == material.resolve()) return stack;
        }
        return FluidStack.EMPTY;
    }
    /** The material's canonical molten phase, or empty when the catalog has none. */
    public static FluidStack moltenFluid(GTMaterial material, int amount) { return fluid(material, amount); }
    public boolean isFluidValid(int tank, FluidStack fluid) { return tank == 0 && material(fluid) != null; }
    public FluidStack getFluidInTank(int tank) { return tank == 0 ? drain(Integer.MAX_VALUE, FluidAction.SIMULATE) : FluidStack.EMPTY; }
    public int fill(FluidStack resource, FluidAction action) {
        var material = material(resource);
        return material == null ? 0 : storage.fillMoltenMaterial(material, resource.getAmount(), resource.getFluid().getFluidType().getTemperature(resource), action.execute(), ratio(material, resource));
    }
    public FluidStack drain(FluidStack resource, FluidAction action) {
        var material = material(resource);
        if (material == null) return FluidStack.EMPTY;
        int planned = storage.drainMoltenMaterial(material, resource.getAmount(), false, ratio(material, resource));
        if (planned <= 0) return FluidStack.EMPTY;
        FluidStack result = resource.copy(); result.setAmount(planned);
        if (action.execute()) storage.drainMoltenMaterial(material, planned, true, ratio(material, resource));
        return result;
    }
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (maxDrain <= 0) return FluidStack.EMPTY;
        var contents = storage.getContentView().stream().filter(stack -> stack.amount > 0 && storage.getTemperature() >= stack.material.getMeltingPoint())
                .sorted(java.util.Comparator.comparingDouble(stack -> stack.material.getDensity())).toList();
        for (var entry : contents) {
            FluidStack result = fluid(entry.material, 1);
            if (result.isEmpty()) continue;
            var ratio = ratio(entry.material, result);
            int planned = storage.drainMoltenMaterial(entry.material, maxDrain, false, ratio);
            if (planned <= 0) continue;
            result.setAmount(planned);
            if (action.execute()) storage.drainMoltenMaterial(entry.material, planned, true, ratio);
            return result;
        }
        return FluidStack.EMPTY;
    }
}
