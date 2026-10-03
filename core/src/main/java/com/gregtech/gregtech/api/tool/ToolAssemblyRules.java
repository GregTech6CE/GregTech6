package com.gregtech.gregtech.api.tool;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.data.MaterialPrefix;
import java.util.List;

/** Shared head/handle admission from saltnya's GTToolAssemblyRecipe; stacks stay on the platform. */
public final class ToolAssemblyRules {
    private ToolAssemblyRules() {}

    public record Form(MaterialPrefix prefix, GTMaterial material) {}
    public record Inputs(GTMaterial head, GTMaterial handle) {}

    /** Each form represents one nonempty crafting slot, regardless of its stack count. */
    public static Inputs match(ToolDefinition type, List<Form> forms) {
        boolean lens = type == ToolDefinition.MAGNIFYING_GLASS;
        MaterialPrefix wanted = lens ? MaterialPrefix.lens : type.headPrefix();
        if (wanted == null || forms.size() != 2) return null;
        GTMaterial head = null;
        GTMaterial handle = null;
        for (Form form : forms) {
            if (form == null || form.material() == null) return null;
            GTMaterial material = form.material().resolve();
            if (form.prefix() == wanted) {
                if (head != null || material == null || (!lens && !type.canUseHead(material))) return null;
                head = material;
            } else if (form.prefix() == MaterialPrefix.stick) {
                if (handle != null || material == null || !MaterialPrefix.stick.isValidFor(material)) return null;
                handle = material;
            } else {
                return null;
            }
        }
        return head != null && handle != null
                && com.gregtech.gregtech.content.tool.OriginalToolMaterials.acceptsAssemblyHead(type, head)
                && com.gregtech.gregtech.content.tool.OriginalToolMaterials.acceptsHandle(head, handle)
                ? new Inputs(head, handle) : null;
    }
}
