package gregtech.engine.multiblock.tracking;

import gregtech.api.multiblock.CellMatch;
import gregtech.api.multiblock.StructureConstraint;
import gregtech.content.multiblock.SharedLargeMachineParts;
import gregtech.engine.multiblock.MultiblockLayout.Role;
import java.util.List;

/** Resolves public names once when compiling a pattern; evaluation only reads its cached choices. */
public final class StructureConstraints {
    private StructureConstraints() {}

    public static List<Aggregate> compile(List<StructureConstraint> constraints) {
        return constraints.stream().map(StructureConstraints::compile).toList();
    }

    private static Aggregate compile(StructureConstraint constraint) {
        var selection = constraint.selection();
        var match = selection.match() == null ? Aggregate.all() : matcher(selection.match());
        Role role =
                selection.role() == null ? null : Role.valueOf(selection.role().name());
        Aggregate.CellSelector selector = (part, plain, actualRole) ->
                (role == null || role == actualRole) && match.selects(part, plain, actualRole);
        String description = selection.match() == null ? "" : selection.match().toString();
        if (selection.role() != null) description += " [" + selection.role().name() + "]";
        if (constraint instanceof StructureConstraint.Count count)
            return new Aggregate.Count(selector, count.min(), count.max(), description);
        return new Aggregate.SameBlock(selector, description);
    }

    private static Aggregate.CellSelector matcher(CellMatch match) {
        if (match instanceof CellMatch.OneOf one) {
            var members =
                    one.members().stream().map(StructureConstraints::matcher).toList();
            return (part, plain, role) -> members.stream().anyMatch(member -> member.selects(part, plain, role));
        }
        if (match instanceof CellMatch.Part named) {
            int id = SharedLargeMachineParts.idForName(named.name());
            if (id < 0) throw new IllegalArgumentException("Unknown constraint part: " + named.name());
            return (part, plain, role) -> plain == null && part == id;
        }
        if (match instanceof CellMatch.Air) return (part, plain, role) -> role == Role.AIR;
        return Aggregate.plain(match.toString());
    }
}
