package gregtech.api.material;

import gregtech.api.addon.material.MaterialComposition;
import gregtech.api.addon.material.MaterialInfoTool;
import gregtech.api.addon.material.MaterialKey;
import gregtech.api.addon.material.MaterialProcessKey;
import gregtech.api.addon.material.MaterialProcessRule;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Isolated host for API tests of the legacy facade; production composition is covered by core tests. */
public final class MaterialInfoTestHost implements MaterialInfoTool.Host {
    private static final MaterialInfoTestHost HOST = new MaterialInfoTestHost();
    private static final Map<String, GTMaterial> MATERIALS = new LinkedHashMap<>();
    private static final Map<String, MaterialToolProfile> TOOLS = new LinkedHashMap<>();
    private static final Map<String, MaterialComposition> COMPOSITIONS = new LinkedHashMap<>();
    private static final Map<String, Map<MaterialProcessKey<?>, MaterialProcessRule<?>>> PROCESSES =
            new LinkedHashMap<>();
    private static boolean bound;

    private MaterialInfoTestHost() {}

    public static void reset(GTMaterial... materials) {
        MATERIALS.clear();
        TOOLS.clear();
        COMPOSITIONS.clear();
        PROCESSES.clear();
        for (GTMaterial material : materials) {
            MATERIALS.put(material.getName(), material);
            PROCESSES.put(material.getName(), new LinkedHashMap<>());
        }
        if (!bound) {
            MaterialInfoTool.bindHost(HOST);
            bound = true;
        }
    }

    @Override
    public MaterialInfoTool.Registrar registrar(String addonId) {
        throw new UnsupportedOperationException("The API test host is read/write only through GTMaterial");
    }

    @Override
    public Optional<MaterialToolProfile> tool(MaterialKey material) {
        return Optional.ofNullable(TOOLS.get(material.name()));
    }

    @Override
    public Optional<MaterialComposition> composition(MaterialKey material) {
        return Optional.ofNullable(COMPOSITIONS.get(material.name()));
    }

    @Override
    public Optional<GTMaterial> findMaterial(MaterialKey material) {
        return Optional.ofNullable(MATERIALS.get(material.name()));
    }

    @Override
    public <T> Optional<MaterialProcessRule<T>> currentProcess(
            MaterialKey material, MaterialProcessKey<T> process) {
        @SuppressWarnings("unchecked")
        MaterialProcessRule<T> rule = (MaterialProcessRule<T>)
                PROCESSES.getOrDefault(material.name(), Map.of()).get(process);
        return Optional.ofNullable(rule);
    }

    @Override
    public void updateTool(MaterialKey material, MaterialToolProfile profile) {
        TOOLS.put(material.name(), profile);
    }

    @Override
    public void updateComposition(MaterialKey material, Optional<MaterialComposition> composition) {
        if (composition.isEmpty()) COMPOSITIONS.remove(material.name());
        else COMPOSITIONS.put(material.name(), composition.get());
    }

    @Override
    public <T> void updateProcess(
            MaterialKey material, MaterialProcessKey<T> process, Optional<MaterialProcessRule<T>> rule) {
        Map<MaterialProcessKey<?>, MaterialProcessRule<?>> processes = PROCESSES.get(material.name());
        if (processes == null) throw new IllegalArgumentException("Unknown test material: " + material.name());
        if (rule.isEmpty()) processes.remove(process);
        else processes.put(process, rule.get());
    }
}
