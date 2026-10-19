import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.mod.ModData;
import com.gregtech.gregtech.content.creative.CreativeTabCatalog;
import com.gregtech.gregtech.content.energy.EnergyNodeDefinitions;
import com.gregtech.gregtech.content.energy.ZpmWorkRules;
import com.gregtech.gregtech.content.plant.IconPlantCatalog;

/** Pure catalog/arithmetic evidence, not a native registry or world run. */
class SpecialStorageAudit {
    public static void main(String[] args) {
        ModData.bindPresence(id -> id.equals("minecraft") || id.equals("gt6ce"));
        GTMaterialRegistry.init();
        GTMaterialRegistry.postInit();
        long chargers = EnergyNodeDefinitions.specifications().stream()
                .filter(spec -> spec.batterySlots() > 0 && spec.inType().getShortName().equals("LU"))
                .count();
        System.out.println("LU_STORAGE_IN_ENERGY_CATALOG|" + chargers);
        System.out.println("CREATIVE_FAMILY|crystal_chargers|" + CreativeTabCatalog.FAMILIES.contains("crystal_chargers"));
        System.out.println("CREATIVE_FAMILY|portable_power_cells|" + CreativeTabCatalog.FAMILIES.contains("portable_power_cells"));
        System.out.println("ICON_BLOCK|powercell|" + IconPlantCatalog.BLOCK_ICONS.contains("powercell"));
        System.out.println("ICON_BLOCK|aneutronic_fusion|" + IconPlantCatalog.BLOCK_ICONS.contains("aneutronic_fusion"));
        System.out.println("ZPM|capacity=" + ZpmWorkRules.CAPACITY + "|packet=" + ZpmWorkRules.PACKET);
        for (long energy : new long[]{0, 1, ZpmWorkRules.CAPACITY / 2, ZpmWorkRules.CAPACITY - 1, ZpmWorkRules.CAPACITY}) {
            System.out.println("LIGHT|" + energy + "|" + ZpmWorkRules.light(energy));
        }
        for (long packets : new long[]{0, 39, 40, 79, 80}) {
            System.out.println("REFILL|bufferPackets=" + packets + "|amount="
                    + ZpmWorkRules.refillAmount(packets * ZpmWorkRules.PACKET, ZpmWorkRules.CAPACITY));
        }
        System.out.println("REFILL_REMAINDER|" + ZpmWorkRules.refillAmount(0, ZpmWorkRules.PACKET - 1));
        System.out.println("CHARGE_CLAMP|" + ZpmWorkRules.charge(-1) + "|" + ZpmWorkRules.charge(Long.MAX_VALUE));
        System.out.println("BUFFER_CLAMP|" + ZpmWorkRules.savedBuffer(-1) + "|" + ZpmWorkRules.savedBuffer(Long.MAX_VALUE));
        System.out.println("DUNGEON|" + ZpmWorkRules.dungeonCharge(false) + "|" + ZpmWorkRules.dungeonCharge(true));
    }
}
