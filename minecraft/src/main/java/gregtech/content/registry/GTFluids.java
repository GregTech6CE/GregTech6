package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

//? if >=1.21.1 {
/*import gregtech.engine.fluid.FluidVisualPolicy;
*///?} else {
//?}
import gregtech.engine.addon.AddonMaterials;
import gregtech.api.material.GTMaterial;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.mod.GregTechIdentity;
import gregtech.content.data.RegisteredFluids;
import gregtech.engine.fluid.FluidPhysicalProperties;
//? if >=1.21.1 {
/*import net.minecraft.core.registries.BuiltInRegistries;
*///?} else {
import net.minecraft.network.chat.Component;
//?}
import net.minecraft.resources.ResourceLocation;
//? if >=1.21.1 {
//?} else {
import net.minecraft.sounds.SoundEvents;
//?}
import net.minecraft.world.level.material.Fluid;
//? if >=1.21.1 {
/*import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import java.util.LinkedHashMap;
import java.util.Map;
*///?} else {
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
//?}

/** Neo fluid registry adapter for the same original catalog and registry paths as Forge. */
//? if >=1.21.1 {
//?} else {
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/** GT6 fluid registry holder — DeferredRegisters and lookup maps populated by {@code Loader_Fluids}. */
//?}
public final class GTFluids {
    private static final java.util.Set<FluidType> NON_LIQUID_TYPES =
            java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

    /** Identity-scoped: never infer phase from negative density or affect another mod's fluids. */
    public static boolean isNonLiquidType(FluidType type) {
        return NON_LIQUID_TYPES.contains(type);
    }

    /**
     * Registry namespace of a generated fluid: the owning addon of the material it was derived
     * from, otherwise {@code gt6ce}. Built-in definitions keep their namespace even when an addon
     * material binds to their existing fluid; that binding supplies metadata, not ownership.
     */
    public static String namespace(RegisteredFluids.FluidEntry entry) {
        if (RegisteredFluids.isBuiltInDefinition(entry)) return GregTechIdentity.MOD_ID;
        String materialKey = entry.materialKey();
        if (materialKey == null) materialKey = RegisteredFluids.boundMaterial(entry.registryName());
        if (materialKey == null) return GregTechIdentity.MOD_ID;
        GTMaterial material = GTMaterialRegistry.get(materialKey);
        if (material == null || !material.isValid()) return GregTechIdentity.MOD_ID;
        return AddonMaterials.namespaceOf(material.resolve());
    }

//? if >=1.21.1 {
    /*public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, REGISTRY_NAMESPACE);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(BuiltInRegistries.FLUID, REGISTRY_NAMESPACE);
    private static final Map<String, DeferredRegister<FluidType>> ADDON_FLUID_TYPES = new LinkedHashMap<>();
    private static final Map<String, DeferredRegister<Fluid>> ADDON_FLUIDS = new LinkedHashMap<>();
    private static final Map<String, DeferredHolder<Fluid, ? extends Fluid>> STILL = new LinkedHashMap<>(), FLOWING = new LinkedHashMap<>();
    private static final Map<ResourceLocation, RegisteredFluids.FluidEntry> ENTRIES = new LinkedHashMap<>();
    private GTFluids() {}

    /^* The fluid-type register for a namespace; addon namespaces get their own register. ^/
    public static DeferredRegister<FluidType> fluidTypes(String namespace) {
        if (GregTechIdentity.MOD_ID.equals(namespace)) return FLUID_TYPES;
        return ADDON_FLUID_TYPES.computeIfAbsent(namespace,
                id -> DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, id));
    }

    public static DeferredRegister<Fluid> fluids(String namespace) {
        if (GregTechIdentity.MOD_ID.equals(namespace)) return FLUIDS;
        return ADDON_FLUIDS.computeIfAbsent(namespace, id -> DeferredRegister.create(BuiltInRegistries.FLUID, id));
    }

    public static void registerAddonFluids(IEventBus bus) {
        ADDON_FLUID_TYPES.values().forEach(register -> register.register(bus));
        ADDON_FLUIDS.values().forEach(register -> register.register(bus));
    }
    public static void bindFluid(String field, DeferredHolder<Fluid, ? extends Fluid> still, DeferredHolder<Fluid, ? extends Fluid> flowing, ResourceLocation id, RegisteredFluids.FluidEntry entry) {
        STILL.put(field, still); FLOWING.put(field, flowing); ENTRIES.put(id, entry);
*///?} else {
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, GregTechIdentity.REGISTRY_NAMESPACE);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(ForgeRegistries.FLUIDS, GregTechIdentity.REGISTRY_NAMESPACE);

    private static final Map<String, DeferredRegister<FluidType>> ADDON_FLUID_TYPES = new HashMap<>();
    private static final Map<String, DeferredRegister<Fluid>> ADDON_FLUIDS = new HashMap<>();
    private static final Map<String, RegistryObject<Fluid>> STILL_BY_FIELD = new HashMap<>();
    private static final Map<String, RegistryObject<Fluid>> FLOWING_BY_FIELD = new HashMap<>();
    private static final Map<ResourceLocation, RegisteredFluids.FluidEntry> ENTRY_BY_TYPE_ID = new HashMap<>();

    /** The fluid-type register for a namespace; addon namespaces get their own register. */
    public static DeferredRegister<FluidType> fluidTypes(String namespace) {
        if (GregTechIdentity.REGISTRY_NAMESPACE.equals(namespace)) return FLUID_TYPES;
        return ADDON_FLUID_TYPES.computeIfAbsent(namespace,
                id -> DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, id));
    }

    public static DeferredRegister<Fluid> fluids(String namespace) {
        if (GregTechIdentity.REGISTRY_NAMESPACE.equals(namespace)) return FLUIDS;
        return ADDON_FLUIDS.computeIfAbsent(namespace, id -> DeferredRegister.create(ForgeRegistries.FLUIDS, id));
    }

    public static void registerAddonFluids(net.minecraftforge.eventbus.api.IEventBus bus) {
        ADDON_FLUID_TYPES.values().forEach(register -> register.register(bus));
        ADDON_FLUIDS.values().forEach(register -> register.register(bus));
    }

    /** Registry metadata wins over density: some GT6 gases are heavier than air. */
    public static boolean isGas(FluidStack stack) {
        if (stack.isEmpty()) return false;
        var entry=entryForFluid(stack.getFluid());
        return entry!=null?entry.gas():stack.getFluid().getFluidType().isLighterThanAir();
//?}
    }
//? if >=1.21.1 {
    /*public static String sanitizePath(String name) { return RegisteredFluids.sanitizePath(name); }
    public static DeferredHolder<Fluid, ? extends Fluid> still(String field) { return STILL.get(RegisteredFluids.canonicalField(field)); }
    public static DeferredHolder<Fluid, ? extends Fluid> flowing(String field) { return FLOWING.get(RegisteredFluids.canonicalField(field)); }
    public static RegisteredFluids.FluidEntry entryForTypeId(ResourceLocation id) { return ENTRIES.get(id); }
    public static RegisteredFluids.FluidEntry entryForFluid(Fluid fluid) {
        var id = BuiltInRegistries.FLUID.getKey(fluid);
        var entry = ENTRIES.get(id);
        if (entry == null && id.getPath().endsWith("_flowing")) entry = ENTRIES.get(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), id.getPath().substring(0, id.getPath().length() - 8)));
        return entry;
    }
    public static Map<ResourceLocation, RegisteredFluids.FluidEntry> entries() { return Map.copyOf(ENTRIES); }
    public static FluidStack stack(String field, int mb) {
        if ("Water".equals(field) || "minecraft:water".equals(field))
            return new FluidStack(net.minecraft.world.level.material.Fluids.WATER, mb);
        var entry = RegisteredFluids.get(field);
        if (entry == null || mb <= 0) return FluidStack.EMPTY;
        if (entry.isVanillaLava()) return new FluidStack(net.minecraft.world.level.material.Fluids.LAVA, mb);
        var holder = still(field);
        return holder == null || !holder.isBound() ? FluidStack.EMPTY : new FluidStack(holder.get(), mb);
    }
*///?} else {

    private GTFluids() {}

    // ==================== factories called by Loader_Fluids ====================

    /** Creates a GT6 {@link FluidType} with GT6-specific density, viscosity, luminosity, temperature, and client extensions. */
//?}
    public static FluidType createFluidType(RegisteredFluids.FluidEntry entry) {
//? if >=1.21.1 {
        /*var properties = FluidType.Properties.create().density(FluidPhysicalProperties.density(entry))
                .viscosity(FluidPhysicalProperties.viscosity(entry))
                .temperature(entry.temperature()).lightLevel(entry.luminosity()).canConvertToSource(false).supportsBoating(false).canHydrate(false).canSwim(false).canDrown(false);
*///?} else {
        FluidType.Properties properties = FluidType.Properties.create()
                .density(FluidPhysicalProperties.density(entry))
                .viscosity(FluidPhysicalProperties.viscosity(entry))
                .lightLevel(entry.luminosity())
                .temperature(entry.temperature())
                .canConvertToSource(false)
                .supportsBoating(false)
                .canHydrate(false)
                .canSwim(false)
                .canDrown(false)
                .fallDistanceModifier(0.0F)
                .motionScale(0.007D);
//?}
        boolean nonLiquid = FluidPhysicalProperties.nonLiquid(entry);
        properties.sound(SoundActions.BUCKET_FILL, net.minecraft.sounds.SoundEvents.BUCKET_FILL)
                .sound(SoundActions.BUCKET_EMPTY, net.minecraft.sounds.SoundEvents.BUCKET_EMPTY);
        if (nonLiquid) properties.canPushEntity(false).motionScale(0.0D)
                .fallDistanceModifier(1.0F).canExtinguish(false).canSwim(false).canDrown(false);
        FluidType type = new FluidType(properties) {
//? if >=1.21.1 {
            /*@Override public net.minecraft.network.chat.Component getDescription() { return GTFluidType.describe(entry, sanitizePath(entry.registryName())); }
            @Override public net.minecraft.network.chat.Component getDescription(FluidStack stack) { return getDescription(); }
*///?} else {
            @Override
            public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                consumer.accept(clientExtensions(entry));
            }

            @Override
            public Component getDescription() {
                String path = sanitizePath(entry.registryName());
                return GTFluidType.describe(entry, path);
            }

            @Override
            public Component getDescription(FluidStack stack) {
                String path = sanitizePath(entry.registryName());
                return GTFluidType.describe(entry, path);
            }

//?}
        };
        if (nonLiquid) NON_LIQUID_TYPES.add(type);
        return type;
    }
//? if >=1.21.1 {
    /*public static int fluidTemperature(RegisteredFluids.FluidEntry entry) { return entry.temperature(); }
    public static boolean isGas(FluidStack stack) { var entry = entryForFluid(stack.getFluid()); return entry != null ? entry.gas() : stack.getFluid().getFluidType().isLighterThanAir(); }
    public static int resolveTint(RegisteredFluids.FluidEntry entry) { return FluidVisualPolicy.rawTint(entry); }
*///?} else {

    /** Binds a still/flowing fluid pair into lookup maps. Called by Loader_Fluids during registration. */
    public static void bindFluid(String field, RegistryObject<Fluid> still, RegistryObject<Fluid> flowing,
                                  ResourceLocation typeId, RegisteredFluids.FluidEntry entry) {
        STILL_BY_FIELD.put(field, still);
        FLOWING_BY_FIELD.put(field, flowing);
        ENTRY_BY_TYPE_ID.put(typeId, entry);
    }

    // ==================== path helpers ====================

    public static String sanitizePath(String registryName) {
        return RegisteredFluids.sanitizePath(registryName);
    }

    // ==================== lookup ====================

    public static RegistryObject<Fluid> still(String flField) {
        RegistryObject<Fluid> direct = STILL_BY_FIELD.get(flField);
        if (direct != null) return direct;
        String canonical = RegisteredFluids.canonicalField(flField);
        return canonical.equals(flField) ? null : STILL_BY_FIELD.get(canonical);
    }

    public static RegistryObject<Fluid> flowing(String flField) {
        RegistryObject<Fluid> direct = FLOWING_BY_FIELD.get(flField);
        if (direct != null) return direct;
        String canonical = RegisteredFluids.canonicalField(flField);
        return canonical.equals(flField) ? null : FLOWING_BY_FIELD.get(canonical);
    }

    /**
     * A registered GT6 fluid as a {@link FluidStack}, or {@code null} when it is not registered.
     * <p>
     * {@code Water} is vanilla water and has no catalog entry; {@code Lava} reuses the vanilla
     * fluid, so {@link FluidBootstrap} deliberately does not register a still/flowing pair for it
     * (by registry identity, not texture mode).
     * </p>
     */
    public static FluidStack stack(String flField, int mb) {
        if ("Water".equals(flField) || "minecraft:water".equals(flField)) {
            return new FluidStack(net.minecraft.world.level.material.Fluids.WATER, mb);
        }
        RegisteredFluids.FluidEntry entry = RegisteredFluids.get(flField);
        if (entry == null) return null;
        if (entry.isVanillaLava()) {
            return new FluidStack(net.minecraft.world.level.material.Fluids.LAVA, mb);
        }
        RegistryObject<Fluid> fluid = still(flField);
        if (fluid == null || !fluid.isPresent()) return null;
        return new FluidStack(fluid.get(), mb);
    }

    public static RegisteredFluids.FluidEntry entryForTypeId(ResourceLocation typeId) {
        return ENTRY_BY_TYPE_ID.get(typeId);
    }

    /**
     * The GT6 {@link RegisteredFluids.FluidEntry} of a registered fluid, still or flowing.
     *
     * <p>Keyed by the <em>fluid's</em> registry id. The ids are the same ones {@link #bindFluid}
     * was called with, so nothing else about the lookup changes; the flowing variant is registered
     * as {@code <path>_flowing} and is resolved through its still fluid. The {@code FluidType}-keyed
     * lookup is kept for callers that only have a type id (for example a synced fluid-type id from
     * a menu packet).
     */
    public static RegisteredFluids.FluidEntry entryForFluid(Fluid fluid) {
        if (fluid == null) return null;
        ResourceLocation fluidId = ForgeRegistries.FLUIDS.getKey(fluid);
        if (fluidId != null) {
            RegisteredFluids.FluidEntry direct = ENTRY_BY_TYPE_ID.get(fluidId);
            if (direct != null) return direct;
            String path = fluidId.getPath();
            if (path.endsWith("_flowing")) {
                RegisteredFluids.FluidEntry still = ENTRY_BY_TYPE_ID.get(
                        ResourceLocation.fromNamespaceAndPath(fluidId.getNamespace(),
                                path.substring(0, path.length() - "_flowing".length())));
                if (still != null) return still;
            }
        }
        // Fallback for fluids that were bound under a type id only.
        return ENTRY_BY_TYPE_ID.get(ForgeRegistries.FLUID_TYPES.get().getKey(fluid.getFluidType()));
    }

    // ==================== fluid properties ====================

    public static int fluidTemperature(RegisteredFluids.FluidEntry entry) {
        return entry.temperature();
    }

    // ==================== client extensions ====================

    private static IClientFluidTypeExtensions clientExtensions(RegisteredFluids.FluidEntry entry) {
        return new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return gregtech.client.FluidAppearance.appearance(entry).texture();
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return getStillTexture();
            }

            @Override
            public int getTintColor() {
                return gregtech.client.FluidAppearance.appearance(entry).tint();
            }
        };
    }

    /** Raw color for grayscale templates; client rendering uses FluidAppearance for texture-aware tint. */
    public static int resolveTint(RegisteredFluids.FluidEntry entry) {
        return gregtech.engine.fluid.FluidVisualPolicy.rawTint(entry);
    }
//?}
}
