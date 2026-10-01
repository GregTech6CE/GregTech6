"""Generate lang entries for GT6 basic machines."""
import json, os

LANG_DIR = r"F:\Dev\GregTech\GregTech6\src\main\resources\assets\gregtech\lang"

# Material display names
MAT_EN = {
    "Steel": "Steel", "Invar": "Invar", "Ti": "Titanium",
    "TungstenCarbide": "Tungsten Carbide", "Bronze": "Bronze",
    "TungstenSteel": "Tungsten Steel", "SteelGalvanized": "Galvanized Steel",
    "Al": "Aluminium", "StainlessSteel": "Stainless Steel", "Cr": "Chromium",
    "Os": "Osmium",
}
MAT_CN = {
    "Steel": "钢", "Invar": "殷钢", "Ti": "钛",
    "TungstenCarbide": "碳化钨", "Bronze": "青铜",
    "TungstenSteel": "钨钢", "SteelGalvanized": "镀锌钢",
    "Al": "铝", "StainlessSteel": "不锈钢", "Cr": "铬",
    "Os": "锇",
}
MAT_SHORT = {
    "Steel": "steel", "Invar": "invar", "Ti": "titanium",
    "TungstenCarbide": "tungsten_carbide", "Bronze": "bronze",
    "TungstenSteel": "tungsten_steel", "SteelGalvanized": "galvanized_steel",
    "Al": "aluminium", "StainlessSteel": "stainless_steel", "Cr": "chromium",
    "Os": "osmium",
}

# Machine name translations
MACHINE_NAMES = {
    # HU machines
    "oven":                    ("Oven",                    "炉子"),
    "roaster":                 ("Roaster",                 "焙烧炉"),
    "distillery":              ("Distillery",              "蒸馏器"),
    "extruder":                ("Extruder",                "挤出机"),
    "smelter":                 ("Smelter",                 "冶炼炉"),
    "crystallisationcrucible": ("Crystallisation Crucible","结晶坩埚"),
    "dryer":                   ("Dryer",                   "干燥器"),
    "laminator":               ("Laminator",               "压板机"),
    "catalyticcracker":        ("Catalytic Cracker",       "催化裂化器"),
    "steamcracker":            ("Steam Cracker",           "蒸汽裂化器"),
    # RU machines
    "shredder":     ("Shredder",     "粉碎机"),
    "lathe":        ("Lathe",        "车床"),
    "buzzsaw":      ("Buzzsaw",      "圆锯"),
    "centrifuge":   ("Centrifuge",   "离心机"),
    "rollingmill":  ("Rolling Mill", "轧机"),
    "rollbender":   ("Roll Bender",  "弯辊机"),
    "rollformer":   ("Roll Former",  "滚压成型机"),
    "clustermill":  ("Cluster Mill", "连轧机"),
    "wiremill":     ("Wire Mill",    "线材轧机"),
    "mixer":        ("Mixer",        "搅拌机"),
    "loom":         ("Loom",         "织布机"),
    "sluice":       ("Sluice",       "洗矿槽"),
    "sander":       ("Sander",       "砂光机"),
    "burnmixer":    ("Burn Mixer",   "燃烧搅拌机"),
    "debarker":     ("Debarker",     "剥皮机"),
    # KU machines
    "crusher":    ("Crusher",    "破碎机"),
    "sifter":     ("Sifter",     "筛分机"),
    "squeezer":   ("Squeezer",   "压榨机"),
    "compressor": ("Compressor", "压缩机"),
    "press":      ("Press",      "冲压机"),
    # EU machines
    "electrolyzer":    ("Electrolyzer",    "电解机"),
    "canner":          ("Canner",          "装罐机"),
    "injector":        ("Injector",        "注入器"),
    "printer":         ("Printer",         "打印机"),
    "scannervisuals":  ("Scanner (Visual)","扫描仪(光学)"),
    "autocrafter":     ("Autocrafter",     "自动合成器"),
    "electricmixer":   ("Electric Mixer",   "电力搅拌机"),
    "electricloom":    ("Electric Loom",    "电力织布机"),
    "electricsifter":  ("Electric Sifter",  "电力筛分机"),
    "slicer":          ("Slicer",          "切片机"),
    "nanofab":         ("Nanofab",         "纳米制造机"),
    "plantalyzer":     ("Plantalyzer",     "植物分析仪"),
    "bumblelyzer":     ("Bumblelyzer",     "蜜蜂分析仪"),
    "boxinator":       ("Boxinator",       "装箱机"),
    "unboxinator":     ("Unboxinator",     "拆箱机"),
    # MU machines
    "polarizer":         ("Polarizer",         "偏振机"),
    "magneticseparator": ("Magnetic Separator","磁选机"),
    # LU machines
    "laserengraver": ("Laser Engraver", "激光雕刻机"),
    "laserwelder":   ("Laser Welder",   "激光焊接机"),
    # CU machines
    "freezer":   ("Freezer",   "冷冻机"),
    "cryomixer": ("Cryo Mixer","低温搅拌机"),
    # QU machines
    "massfab":          ("Mass Fabricator",    "物质制造机"),
    "scannermolecular": ("Scanner (Molecular)","扫描仪(分子)"),
    "replicator":       ("Replicator",         "复制机"),
    # Single-tier
    "autoclave":              ("Autoclave",              "高压釜"),
    "bath":                   ("Bath",                   "洗矿池"),
    "generifier":             ("Generifier",             "生成器"),
    "coagulator":             ("Coagulator",             "凝固器"),
    "fermenter":              ("Fermenter",              "发酵器"),
    "melter":                 ("Melter",                 "熔化器"),
    "cokeoven":               ("Coke Oven",              "焦炉"),
    "lightning":              ("Lightning Generator",    "闪电发生器"),
    "implosioncompressor":    ("Implosion Compressor",   "爆聚压缩机"),
    "fusionreactor":          ("Fusion Reactor",         "聚变反应堆"),
    "cryodistillationtower":  ("Cryo Distillation Tower","低温蒸馏塔"),
    "distillationtower":      ("Distillation Tower",     "蒸馏塔"),
}

# Same machine-material combos as the JSON generator
MACHINES = [
    ("oven", ["Steel","Invar","Ti","TungstenCarbide"]),
    ("roaster", ["Steel","Invar","Ti","TungstenCarbide"]),
    ("distillery", ["Steel","Invar","Ti","TungstenCarbide"]),
    ("extruder", ["Steel","Invar","Ti","TungstenCarbide"]),
    ("smelter", ["Steel","Invar","Ti","TungstenCarbide"]),
    ("crystallisationcrucible", ["Steel","Invar","Ti","TungstenCarbide"]),
    ("dryer", ["Steel","Invar","Ti","TungstenCarbide"]),
    ("laminator", ["Steel","Invar","Ti","TungstenCarbide"]),
    ("catalyticcracker", ["Steel","Invar","Ti","TungstenCarbide"]),
    ("steamcracker", ["Steel","Invar","Ti","TungstenCarbide"]),
    ("shredder", ["Bronze","Steel","Ti","TungstenSteel"]),
    ("lathe", ["Bronze","Steel","Ti","TungstenSteel"]),
    ("buzzsaw", ["Bronze","Steel","Ti","TungstenSteel"]),
    ("centrifuge", ["Bronze","Steel","Ti","TungstenSteel"]),
    ("rollingmill", ["Bronze","Steel","Ti","TungstenSteel"]),
    ("rollbender", ["Bronze","Steel","Ti","TungstenSteel"]),
    ("rollformer", ["Bronze","Steel","Ti","TungstenSteel"]),
    ("clustermill", ["Bronze","Steel","Ti","TungstenSteel"]),
    ("wiremill", ["Bronze","Steel","Ti","TungstenSteel"]),
    ("mixer", ["Bronze","Steel","Ti","TungstenSteel"]),
    ("loom", ["Bronze","Steel","Ti","TungstenSteel"]),
    ("sluice", ["Bronze","Steel","Ti","TungstenSteel"]),
    ("sander", ["Bronze","Steel","Ti","TungstenSteel"]),
    ("burnmixer", ["Bronze","Steel","Ti","TungstenSteel"]),
    ("debarker", ["Bronze","Steel","Ti","TungstenSteel"]),
    ("crusher", ["Bronze","Steel","Ti","TungstenSteel"]),
    ("sifter", ["Bronze","Steel","Ti","TungstenSteel"]),
    ("squeezer", ["Bronze","Steel","Ti","TungstenSteel"]),
    ("compressor", ["Bronze","Steel","Ti","TungstenSteel"]),
    ("press", ["Bronze","Steel","Ti","TungstenSteel"]),
    ("electrolyzer", ["SteelGalvanized","Al","StainlessSteel","Cr","Ti"]),
    ("canner", ["SteelGalvanized","Al","StainlessSteel","Cr","Ti"]),
    ("injector", ["SteelGalvanized","Al","StainlessSteel","Cr","Ti"]),
    ("printer", ["SteelGalvanized","Al","StainlessSteel","Cr","Ti"]),
    ("scannervisuals", ["SteelGalvanized","Al","StainlessSteel","Cr","Ti"]),
    ("autocrafter", ["SteelGalvanized","Al","StainlessSteel","Cr","Ti"]),
    ("electricmixer", ["SteelGalvanized","Al","StainlessSteel","Cr","Ti"]),
    ("electricloom", ["SteelGalvanized","Al","StainlessSteel","Cr","Ti"]),
    ("electricsifter", ["SteelGalvanized","Al","StainlessSteel","Cr","Ti"]),
    ("slicer", ["SteelGalvanized","Al","StainlessSteel","Cr","Ti"]),
    ("nanofab", ["SteelGalvanized","Al","StainlessSteel","Cr","Ti"]),
    ("plantalyzer", ["SteelGalvanized","Al","StainlessSteel","Cr","Ti"]),
    ("bumblelyzer", ["SteelGalvanized","Al","StainlessSteel","Cr","Ti"]),
    ("boxinator", ["SteelGalvanized","Al","StainlessSteel","Cr","Ti"]),
    ("unboxinator", ["SteelGalvanized","Al","StainlessSteel","Cr","Ti"]),
    ("polarizer", ["SteelGalvanized","Al","StainlessSteel","Cr","Ti"]),
    ("magneticseparator", ["SteelGalvanized","Al","StainlessSteel","Cr","Ti"]),
    ("laserengraver", ["SteelGalvanized","Al","StainlessSteel","Cr","Ti"]),
    ("laserwelder", ["SteelGalvanized","Al","StainlessSteel","Cr","Ti"]),
    ("freezer", ["SteelGalvanized","Al","StainlessSteel","Cr","Ti"]),
    ("cryomixer", ["SteelGalvanized","Al","StainlessSteel","Cr","Ti"]),
    ("massfab", ["Os"]),
    ("scannermolecular", ["Os"]),
    ("replicator", ["Os"]),
    ("autoclave", ["StainlessSteel"]),
    ("bath", ["StainlessSteel"]),
    ("generifier", ["StainlessSteel"]),
    ("coagulator", ["StainlessSteel"]),
    ("fermenter", ["StainlessSteel"]),
    ("melter", ["StainlessSteel"]),
    ("cokeoven", ["StainlessSteel"]),
    ("lightning", ["StainlessSteel"]),
    ("implosioncompressor", ["StainlessSteel"]),
    ("fusionreactor", ["StainlessSteel"]),
    ("cryodistillationtower", ["StainlessSteel"]),
    ("distillationtower", ["StainlessSteel"]),
]

# Handle MACHINE_NAMES entries that are strings for Electric prefix machines
def _resolve_names(name):
    entry = MACHINE_NAMES[name]
    return entry


def main():
    en_entries = {}
    cn_entries = {}

    for machine_name, materials in MACHINES:
        en_name, cn_name = _resolve_names(machine_name)
        for mat in materials:
            mat_short = MAT_SHORT[mat]
            key = f"block.gregtech.{machine_name}_{mat_short}"

            mat_en = MAT_EN[mat]
            mat_cn = MAT_CN[mat]

            en_entries[key] = f"{en_name} ({mat_en})"
            cn_entries[key] = f"{cn_name}({mat_cn})"

    # Load existing lang files
    for lang_file, new_entries in [("en_us.json", en_entries), ("zh_cn.json", cn_entries)]:
        path = os.path.join(LANG_DIR, lang_file)
        with open(path, "r", encoding="utf-8") as f:
            data = json.load(f)

        # Remove any existing basic machine entries to avoid duplicates
        keys_to_remove = [k for k in data if k.startswith("block.gregtech.") and any(
            k == f"block.gregtech.{m}_{MAT_SHORT[mat]}"
            for m, mats in MACHINES for mat in mats
        )]
        for k in keys_to_remove:
            del data[k]

        # Add new entries
        data.update(new_entries)

        # Sort keys: keep existing order, append new ones at end
        with open(path, "w", encoding="utf-8") as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
            f.write("\n")

    print(f"Added {len(en_entries)} entries to en_us.json")
    print(f"Added {len(cn_entries)} entries to zh_cn.json")


if __name__ == "__main__":
    main()
