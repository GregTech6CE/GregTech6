"""Verify the rebuilt jar actually ships the new hive assets, models, lang keys and datapack files."""
import zipfile

JAR = r"build\libs\gregtech-1.0.0.jar"
with zipfile.ZipFile(JAR) as jar:
    names = set(jar.namelist())
    textures = sorted(n for n in names
                      if n.startswith("assets/gt6ce/textures/block/nature/bumblehive/"))
    models = sorted(n for n in names if "/models/block/bumble_hive_" in n)
    print("hive textures in jar:", len(textures))
    print("hive block models in jar:", len(models))
    for path in ["assets/gt6ce/blockstates/bumble_hive.json",
                 "assets/gt6ce/models/item/bumble_hive.json",
                 "assets/gt6ce/lang/en_us.json",
                 "assets/gt6ce/lang/zh_cn.json",
                 "data/gt6ce/worldgen/configured_feature/gt_bumble_hives.json",
                 "data/gt6ce/worldgen/placed_feature/gt_bumble_hives.json",
                 "data/gt6ce/forge/biome_modifier/gt_bumble_hives.json",
                 "data/gt6ce/forge/biome_modifier/gt_bumble_hives_nether.json",
                 "data/gt6ce/forge/biome_modifier/gt_bumble_hives_end.json"]:
        print("%-70s %s" % (path, "OK" if path in names else "MISSING"))
    classes = [n for n in names if n.endswith(("BumbleHiveBlock.class", "BumbleHiveBlockEntity.class",
                                               "GTBumbleHivesFeature.class", "BumbleHiveTests.class",
                                               "GTCombGen.class", "BumbleBeeGenes.class",
                                               "GTBumbleSpecies.class", "BumbleBeeType.class"))]
    print("hive/bee classes in jar:", len(classes))
    for name in sorted(classes):
        print("   ", name.split("/")[-1])
