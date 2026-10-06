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
        return Set.of("wood_barrel_dreamwood","wood_barrel_shimmerwood","wood_barrel_silverwood").contains(spec.id())
                ? MaterialGroups.MagicIron : MaterialGroups.Iron;
    }
    public static Optional<ItemComposition> tank(TankSpec spec) {
        // The inherited generic Wood barrel is not one of the source's four cheap-barrel identities.
        // Logistics barrels include electronics; these require their own source composition audit.
        if(spec.id().equals("wood_barrel")||spec.type()==TankSpec.TankType.LOGISTICS_BARREL) return Optional.empty();
        if(spec.type()==TankSpec.TankType.PLASTIC_CANISTER)
            return Optional.of(new ItemComposition(null,GT6Materials.Compounds.Plastic,GTValues.U*3));
        var parts=new ArrayList<MaterialComponent>();
        parts.add(MaterialComponent.of(spec.material(),GTValues.U*4));
        parts.add(MaterialComponent.of(spec.type()==TankSpec.TankType.METAL_DRUM ? spec.material() : barrelRod(spec),GTValues.U*2));
        return Optional.of(ReversibleCraftingData.perItem(parts,1,"GT6 Loader_MultiTileEntities barrel recipe"));
    }
}
