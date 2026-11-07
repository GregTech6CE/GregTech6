package gregtech.content.material;

import gregtech.api.material.GTMaterial;
import gregtech.content.data.generated.MaterialForms;
import gregtech.engine.material.GTMaterialRegistry;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Audits the committed inputs as well as the live graph, so silently skipped rows remain visible. */
final class MaterialReferenceAudit {
    record Reference(String table, String owner, String field, String name, int id) {}

    enum Kind {
        VALID,
        ALIAS,
        RETIRED,
        DANGLING,
        ID_NAME_MISMATCH
    }

    private MaterialReferenceAudit() {}

    static Path root() {
        Path here = Path.of("").toAbsolutePath();
        return Files.isDirectory(here.resolve("core")) ? here : here.getParent();
    }

    static String input(String name) throws Exception {
        return Files.readString(root().resolve("core/src/main/java/gregtech/content/data/generated/" + name + ".java"));
    }

    static List<Reference> collect() throws Exception {
        var refs = new ArrayList<Reference>();
        // The retired import is test-only historical input, never fed to runtime link resolution.
        try (var stream =
                MaterialReferenceAudit.class.getResourceAsStream("/gregtech/material-import-references.tsv")) {
            if (stream == null) throw new IllegalStateException("Missing historical import audit");
            for (String line : new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)
                    .lines()
                    .toList()) {
                if (line.isBlank() || line.startsWith("#")) continue;
                var cells = line.split("\t", -1);
                if (cells.length != 6) throw new IllegalStateException("Invalid historical reference: " + line);
                refs.add(new Reference(cells[0], cells[1], cells[2], cells[3], Integer.parseInt(cells[4])));
            }
        }
        for (var definition : MaterialDefinitions.all()) {
            if (definition.id() > 0)
                refs.add(new Reference("catalog", definition.name(), "owner", definition.name(), definition.id()));
            var facts = definition.facts();
            int slot = 0;
            for (var part : facts.composition())
                refs.add(new Reference("catalog", definition.name(), "component[" + slot++ + "]", part.material(), 0));
            for (String target : facts.byproducts())
                refs.add(new Reference("catalog", definition.name(), "byproduct", target, 0));
            for (var target : new gregtech.engine.material.MaterialFacts.Target[] {
                facts.burning(), facts.crushing(), facts.smelting(), facts.pulverizing()
            })
                if (target != null)
                    refs.add(new Reference("catalog", definition.name(), "target", target.material(), 0));
            for (var target : facts.processing().values())
                refs.add(new Reference("catalog", definition.name(), "processing", target.material(), 0));
            for (String target : facts.members())
                refs.add(new Reference("catalog", definition.name(), "member", target, 0));
        }
        return refs;
    }

    static Map<String, String> retired() throws Exception {
        var result = new HashMap<String, String>();
        String doc = Files.readString(root().resolve("docs/decisions/parity/material-removal.md"));
        var names = Pattern.compile("`([^`]+)`\\((\\d+)").matcher(doc);
        while (names.find()) {
            result.put(names.group(1), names.group(2));
            result.put(names.group(2), names.group(1));
            result.put(MaterialForms.normalize(names.group(1)), names.group(2));
        }
        return result;
    }

    static Kind classify(Reference ref) throws Exception {
        if (ref.table().equals("decomposition") && ref.id() <= 0) return Kind.ID_NAME_MISMATCH;
        GTMaterial byName = GTMaterialRegistry.get(ref.name());
        GTMaterial byId = GTMaterialRegistry.get(ref.id());
        if (ref.id() > 0) {
            if (byId.isValid()) {
                if (!ref.name().isEmpty() && (!byName.isValid() || byName.resolve() != byId.resolve()))
                    return Kind.ID_NAME_MISMATCH;
                return Kind.VALID;
            }
            if (ref.table().equals("catalog") && MaterialDefinitions.find(ref.name()) != null) return Kind.VALID;
            if (byName.isValid()) return Kind.ID_NAME_MISMATCH;
            var retired = retired();
            if (!retired.containsKey(Integer.toString(ref.id()))) return Kind.DANGLING;
            return ref.name().isEmpty() || Integer.toString(ref.id()).equals(retired.get(ref.name()))
                    ? Kind.RETIRED
                    : Kind.ID_NAME_MISMATCH;
        }
        if (ref.table().equals("catalog") && MaterialDefinitions.find(ref.name()) != null) return Kind.VALID;
        if (byName.isValid()) return byName.getName().equals(ref.name()) ? Kind.VALID : Kind.ALIAS;
        if (ref.table().equals("forms")) {
            for (var entry : GTMaterialRegistry.registeredNames().entrySet())
                if (MaterialForms.normalize(entry.getKey()).equals(ref.name())
                        && entry.getValue().isValid()) return Kind.ALIAS;
            long candidates = GTMaterialRegistry.allMaterials().stream()
                    .map(GTMaterial::resolve)
                    .distinct()
                    .filter(m -> MaterialForms.normalize(m.getName()).equals(ref.name()))
                    .count();
            if (candidates == 1) return Kind.ALIAS;
        }
        // Each historical exception is tied to one table/owner/field/target, with a reviewed source.
        // It cannot admit a new occurrence of that name elsewhere.
        try (var stream =
                MaterialReferenceAudit.class.getResourceAsStream("/gregtech/material-reference-history.tsv")) {
            if (stream == null) throw new IllegalStateException("Missing material reference history");
            String key = ref.table() + "\t" + ref.owner() + "\t" + ref.field() + "\t" + ref.name() + "\t" + ref.id();
            for (String line : new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)
                    .lines()
                    .toList()) {
                if (line.startsWith("#") || line.isBlank()) continue;
                String[] cells = line.split("\t", -1);
                if (cells.length != 7 || !cells[5].equals("RETIRED") || cells[6].isBlank())
                    throw new IllegalStateException("Invalid reference history: " + line);
                if (line.startsWith(key + "\t")) return Kind.valueOf(cells[5]);
            }
        }
        return retired().containsKey(ref.name()) ? Kind.RETIRED : Kind.DANGLING;
    }

    static String describe(Reference ref) {
        try {
            return ref.table() + "\t" + ref.owner() + "\t" + ref.field() + "\t" + ref.name() + "\t" + ref.id() + "\t"
                    + classify(ref);
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }

    static void requireValid(Reference ref) throws Exception {
        Kind kind = classify(ref);
        if (kind == Kind.DANGLING || kind == Kind.ID_NAME_MISMATCH) throw new IllegalArgumentException(describe(ref));
    }
}
