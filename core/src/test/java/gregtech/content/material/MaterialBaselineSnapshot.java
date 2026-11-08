package gregtech.content.material;

import gregtech.api.material.GTMaterial;
import gregtech.api.material.MaterialComponent;
import gregtech.api.mod.ModData;
import gregtech.content.data.generated.MaterialForms;
import gregtech.engine.addon.AddonMaterials;
import gregtech.engine.material.GTMaterialRegistry;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/** Test-only, stable projection; raw identity and resolved identity deliberately occupy separate columns. */
public final class MaterialBaselineSnapshot {
    private MaterialBaselineSnapshot() {}

    public static String identity(GTMaterial material) {
        if (material == null) return "-";
        // Addon numeric ids depend on the installed contributor set.
        return (AddonMaterials.owns(material) ? AddonMaterials.owner(material) : "core") + ":" + material.getName();
    }

    public static String components(List<MaterialComponent> parts, boolean ordered) {
        var stream = parts.stream().map(part -> identity(part.material().resolve()) + "=" + part.amount());
        return (ordered ? stream : stream.sorted()).collect(Collectors.joining(","));
    }

    public static List<String> materials(Collection<GTMaterial> materials) {
        return materials.stream()
                .map(raw -> {
                    var m = raw.resolve();
                    return String.join(
                            "\t",
                            identity(raw),
                            identity(m),
                            AddonMaterials.owns(raw) ? "dynamic" : Integer.toString(raw.getId()),
                            AddonMaterials.owns(m) ? "dynamic" : Integer.toString(m.getId()),
                            Boolean.toString(raw.isValid()),
                            Boolean.toString(m.isValid()),
                            m.getSourceModName(),
                            m.getProtons() + "/" + m.getElectrons() + "/" + m.getNeutrons() + "/" + m.getMass(),
                            m.getMeltingPoint() + "/" + m.getBoilingPoint() + "/" + Float.toString(m.getDensity()),
                            m.getProperties().stream().map(Enum::name).sorted().collect(Collectors.joining(",")),
                            m.getFurnaceBurnTime() + "/" + identity(m.getTargetBurningMaterial()) + "/"
                                    + m.getTargetBurningAmount(),
                            identity(m.getTargetCrushingMaterial()) + "=" + m.getTargetCrushingAmount(),
                            identity(m.getTargetSmeltingMaterial()) + "=" + m.getTargetSmeltingAmount(),
                            identity(m.getTargetPulverMaterial()) + "=" + m.getTargetPulverAmount(),
                            m.getByProducts().stream()
                                    .map(MaterialBaselineSnapshot::identity)
                                    .collect(Collectors.joining(",")),
                            Long.toString(m.getCompositionDivider()),
                            components(m.getCompositionComponents(), true),
                            m.getAliasesToThis().stream()
                                    .map(MaterialBaselineSnapshot::identity)
                                    .sorted()
                                    .collect(Collectors.joining(",")),
                            MaterialForms.of(m).stream().sorted().collect(Collectors.joining(",")));
                })
                .sorted()
                .toList();
    }

    public static List<String> prefixes() {
        return gregtech.engine.prefix.PrefixRegistry.all().stream()
                .map(p -> p.getName() + "\t" + p.getMaterialWeight())
                .sorted()
                .toList();
    }

    @SuppressWarnings("unchecked")
    private static List<String> aliases() throws Exception {
        var field = GTMaterialRegistry.class.getDeclaredField("BY_NAME");
        field.setAccessible(true);
        var names = (java.util.Map<String, GTMaterial>) field.get(null);
        return names.entrySet().stream()
                .map(entry -> entry.getKey() + "\t" + identity(entry.getValue().resolve()))
                .sorted()
                .toList();
    }

    /** Launched by JUnit in independent JVMs, without adding a production reset hook. */
    public static void main(String[] args) throws Exception {
        ModData.bindPresence(id -> false);
        if (args.length > 1) {
            try (var loader = new java.net.URLClassLoader(
                    new java.net.URL[] {Path.of(args[1]).toUri().toURL()},
                    MaterialBaselineSnapshot.class.getClassLoader())) {
                AddonMaterials.collectContributors(loader);
            }
        }
        GTMaterialRegistry.init();
        GTMaterialRegistry.postInit();
        Path output = Path.of(args[0]);
        Files.createDirectories(output);
        write(output.resolve("materials.tsv"), materials(GTMaterialRegistry.allMaterials()));
        write(output.resolve("prefixes.tsv"), prefixes());
        write(output.resolve("aliases.tsv"), aliases());
        write(
                output.resolve("references.tsv"),
                MaterialReferenceAudit.collect().stream()
                        .map(MaterialReferenceAudit::describe)
                        .sorted()
                        .toList());
    }

    static void write(Path path, List<String> rows) throws IOException {
        Files.write(path, rows);
    }
}
