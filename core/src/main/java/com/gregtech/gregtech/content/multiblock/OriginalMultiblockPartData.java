/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Loader_MultiTileEntities 18000..18299 and MultiTileEntityMultiBlockPart.addToolTips. */
package com.gregtech.gregtech.content.multiblock;

import java.util.List;
import java.util.Optional;

/** The 45 original structural parts. Controllers and legacy substitute walls are separate. */
public final class OriginalMultiblockPartData {
    private OriginalMultiblockPartData() {}
    public record Part(int originalId, String path, float hardness, float resistance) {
        /** Only the treated wood wall declares NBT_FLAMMABILITY; Paintable defaults to zero. */
        public int flammability() { return originalId == 18001 ? 150 : 0; }
    }
    public static final List<String> TOOLTIP_KEYS = List.of(
            "gt.lang.use.builder.wand.to.ease.building", "gt.lang.use.magnifyingglass.to.detail");
    public static final List<Part> ALL = List.of(
            new Part(18000, "coke_oven_wall", 5.0f, 5.0f),
            new Part(18001, "wood_wall", 5.0f, 5.0f),
            new Part(18011, "lead_wall", 6.0f, 6.0f),
            new Part(18010, "bronze_wall", 6.0f, 6.0f),
            new Part(18009, "steel_wall", 6.0f, 6.0f),
            new Part(18008, "galvanized_steel_wall", 6.0f, 6.0f),
            new Part(18002, "tank_wall", 6.0f, 6.0f),
            new Part(18007, "invar_wall", 6.0f, 6.0f),
            new Part(18006, "titanium_wall", 9.0f, 9.0f),
            new Part(18003, "tungstensteel_wall", 12.5f, 12.5f),
            new Part(18004, "tungsten_wall", 10.0f, 10.0f),
            new Part(18012, "tantalum_hafnium_carbide_wall", 12.5f, 12.5f),
            new Part(18005, "adamantium_wall", 100.0f, 100.0f),
            new Part(18031, "dense_lead_wall", 6.0f, 6.0f),
            new Part(18030, "dense_bronze_wall", 6.0f, 6.0f),
            new Part(18029, "dense_steel_wall", 6.0f, 6.0f),
            new Part(18028, "dense_galvanized_steel_wall", 6.0f, 6.0f),
            new Part(18022, "tank_wall_dense", 6.0f, 6.0f),
            new Part(18027, "dense_invar_wall", 6.0f, 6.0f),
            new Part(18026, "dense_titanium_wall", 9.0f, 9.0f),
            new Part(18023, "implosion_compressor_wall", 12.5f, 12.5f),
            new Part(18024, "dense_tungsten_wall", 10.0f, 10.0f),
            new Part(18032, "dense_tantalum_hafnium_carbide_wall", 12.5f, 12.5f),
            new Part(18025, "dense_adamantium_wall", 100.0f, 100.0f),
            new Part(18040, "large_copper_coil", 6.0f, 6.0f),
            new Part(18041, "large_niobium_titanium_coil", 6.0f, 6.0f),
            new Part(18042, "large_nichrome_coil", 6.0f, 6.0f),
            new Part(18043, "large_carborundum_coil", 6.0f, 6.0f),
            new Part(18044, "large_osmium_coil", 6.0f, 6.0f),
            new Part(18045, "large_iridium_coil", 6.0f, 6.0f),
            new Part(18100, "centrifuge_part", 12.5f, 12.5f),
            new Part(18105, "electrolyzer_part", 12.5f, 12.5f),
            new Part(18101, "heat_transmitter", 10.0f, 10.0f),
            new Part(18102, "distillation_tower_part", 6.0f, 6.0f),
            new Part(18103, "bedrock_mining_drill_head", 12.5f, 12.5f),
            new Part(18104, "lightning_rod", 8.0f, 8.0f),
            new Part(18106, "sluice_part", 9.0f, 9.0f),
            new Part(18107, "crusher_wheels", 9.0f, 9.0f),
            new Part(18108, "shredder_blades", 9.0f, 9.0f),
            new Part(18299, "fusion_ventilation_unit", 6.0f, 6.0f),
            new Part(18200, "versatile_processor_unit", 6.0f, 6.0f),
            new Part(18201, "logic_processor_unit", 6.0f, 6.0f),
            new Part(18202, "control_processor_unit", 6.0f, 6.0f),
            new Part(18203, "storage_quadcore_processor_unit", 6.0f, 6.0f),
            new Part(18204, "conversion_processor_unit", 6.0f, 6.0f)
    );
    public static Optional<Part> byId(int id) { return ALL.stream().filter(p -> p.originalId() == id).findFirst(); }
    public static Optional<Part> byPath(String path) { return ALL.stream().filter(p -> p.path().equals(path)).findFirst(); }
}
