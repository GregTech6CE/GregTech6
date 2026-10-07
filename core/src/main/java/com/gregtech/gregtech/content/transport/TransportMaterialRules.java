/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Adapted from OP, MultiTileEntityPipeFluid/Item and Loader_MultiTileEntities. */
package com.gregtech.gregtech.content.transport;

import com.gregtech.gregtech.api.machine.*;
import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.data.generated.GT6Materials;
import java.util.*;

/** Original pipe associations override CR.REV; ordinary barrels retain their known recipe inputs. */
public final class TransportMaterialRules {
    private TransportMaterialRules() {}
    public static MaterialPrefix prefix(PipeSpec.PipeSize size) {
        return switch(size) {
            case TINY -> MaterialPrefix.pipeTiny;
            case SMALL -> MaterialPrefix.pipeSmall;
            case MEDIUM -> MaterialPrefix.pipeMedium;
            case LARGE -> MaterialPrefix.pipeLarge;
            case HUGE -> MaterialPrefix.pipeHuge;
            case QUADRUPLE -> MaterialPrefix.pipeQuadruple;
            case NONUPLE -> MaterialPrefix.pipeNonuple;
        };
    }
    public static MaterialPrefix prefix(ItemPipeSpec.ItemPipeSize size) {
        return switch(size) {
            case MEDIUM -> MaterialPrefix.pipeMedium;
            case LARGE -> MaterialPrefix.pipeLarge;
            case HUGE -> MaterialPrefix.pipeHuge;
            case RESTRICTIVE_MEDIUM -> MaterialPrefix.pipeRestrictiveMedium;
            case RESTRICTIVE_LARGE -> MaterialPrefix.pipeRestrictiveLarge;
            case RESTRICTIVE_HUGE -> MaterialPrefix.pipeRestrictiveHuge;
        };
    }
    public static ItemComposition pipe(PipeSpec spec) {
        var prefix=prefix(spec.size());
        return new ItemComposition(prefix,spec.material(),prefix.getMaterialWeight());
    }
    public static ItemComposition pipe(ItemPipeSpec spec) {
        // setTarget_ calls addAssociation_: the final source prefix discards the recipe's steel rings.
        var prefix=prefix(spec.size());
        return new ItemComposition(prefix,spec.material(),prefix.getMaterialWeight());
    }
    public static GTMaterial barrelRod(TankSpec spec) {
        var cheap = com.gregtech.gregtech.content.transport.fluid.CheapWoodBarrelCatalog.entry(spec.id());
        if (cheap.isPresent()) return cheap.get().rod();
        return Set.of("wood_barrel_dreamwood","wood_barrel_shimmerwood","wood_barrel_silverwood").contains(spec.id())
                ? MaterialGroups.MagicIron : MaterialGroups.Iron;
    }
    public static Optional<ItemComposition> tank(TankSpec spec) {
        if(spec.type()==TankSpec.TankType.LOGISTICS_BARREL) return Optional.of(logisticsTank());
        if(spec.type()==TankSpec.TankType.PLASTIC_CANISTER)
            return Optional.of(new ItemComposition(null,GT6Materials.Compounds.Plastic,GTValues.U*3));
        var parts=new ArrayList<MaterialComponent>();
        parts.add(MaterialComponent.of(spec.material(),GTValues.U*4));
        parts.add(MaterialComponent.of(spec.type()==TankSpec.TankType.METAL_DRUM ? spec.material() : barrelRod(spec),GTValues.U*2));
        return Optional.of(ReversibleCraftingData.perItem(parts,1,"GT6 Loader_MultiTileEntities barrel recipe"));
    }

    /** Source CR.DEF_REV sums known data; OD_CIRCUITS and glue have no automatic material data. */
    public static Map<String, ItemComposition> logisticsComponents() {
        long u = GTValues.U;
        var blank = reverse(List.of(MaterialComponent.of(GT6Materials.Elements.Al, u + u/9)));
        var processor = reverse(List.of(MaterialComponent.of(GT6Materials.Elements.Pt, u),
                MaterialComponent.of(GT6Materials.Compounds.Emerald, u)));
        var emitter = reverse(List.of(MaterialComponent.of(GT6Materials.Compounds.TinAlloy, u*4),
                MaterialComponent.of(GT6Materials.Elements.Os, u/8*4),
                MaterialComponent.of(GT6Materials.Compounds.EnderPearl, u)));
        var busParts = new ArrayList<MaterialComponent>(blank.components());
        busParts.addAll(processor.components());
        busParts.add(MaterialComponent.of(GT6Materials.Elements.Os, u/8*2));
        return Map.of("blank_cover", blank, "crystal_processor_emerald", processor,
                "compact_force_field_emitter_ulv", emitter, "generic_logistics_storage_bus", reverse(busParts));
    }
    private static ItemComposition reverse(List<MaterialComponent> parts) {
        return ReversibleCraftingData.perItem(parts, 1, "GT6 MultiItemTechnological known REV inputs");
    }
    private static ItemComposition logisticsTank() {
        var parts = new ArrayList<MaterialComponent>();
        // Source 32718: four curved tungsten plates and two long rods, plus four U/9 screws.
        parts.add(MaterialComponent.of(GT6Materials.Elements.W, GTValues.U*6 + GTValues.U/9*4));
        var components = logisticsComponents();
        parts.addAll(components.get("compact_force_field_emitter_ulv").components());
        parts.addAll(components.get("generic_logistics_storage_bus").components());
        return ReversibleCraftingData.perItem(parts, 1, "GT6 Loader_MultiTileEntities 32072 known REV inputs");
    }
}
