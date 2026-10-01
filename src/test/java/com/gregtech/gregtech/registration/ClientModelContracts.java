package com.gregtech.gregtech.registration;

import com.gregtech.gregtech.client.BakedModelLookup;
import com.gregtech.gregtech.client.BlockItemModelHelper;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/** Exercises real lookup/reload helpers with distinct model objects; does not require a GPU. */
public final class ClientModelContracts {
    public static void main(String[] args) {
        previewNormals();
        fluidFallbackContracts();
        connectorInventoryContracts();
        // A long creative-inventory scroll must never retain every generated preview.
        var bounded = new com.gregtech.gregtech.client.BoundedCache<Integer, Object>(256);
        for (int i = 0; i < 100000; i++) bounded.computeIfAbsent(i, key -> new Object());
        if (bounded.size() != 256) throw new AssertionError("Unbounded preview cache");
        Object recent = bounded.computeIfAbsent(99999, key -> new Object());
        if (bounded.computeIfAbsent(99999, key -> new Object()) != recent)
            throw new AssertionError("Preview allocated again on cache hit");
        bounded.clear();
        if (bounded.size() != 0) throw new AssertionError("Reload retained previews");
        var models = new HashMap<ResourceLocation, BakedModel>();
        var missing = model(ItemTransforms.NO_TRANSFORMS);
        models.put(BakedModelLookup.MISSING_MODEL, missing);
        var location = new ResourceLocation("gregtech", "test/model");
        models.put(new ModelResourceLocation(location, ""), missing);
        var actual = model(transforms());
        models.put(location, actual);
        if (BakedModelLookup.find(models, location) != actual)
            throw new AssertionError("Missing sentinel must not hide a valid fallback");

        var item = new ModelResourceLocation(new ResourceLocation("minecraft", "iron_block"), "inventory");
        var first = transforms();
        var second = transforms();
        models.put(item, model(first));
        BlockItemModelHelper.captureFromVanillaBlockItem(models);
        if (BlockItemModelHelper.blockItemTransforms() != first) throw new AssertionError("Initial transforms lost");
        models.put(item, model(second));
        BlockItemModelHelper.captureFromVanillaBlockItem(models);
        if (BlockItemModelHelper.blockItemTransforms() != second) throw new AssertionError("Resource reload retained stale transforms");
        BlockItemModelHelper.captureFromVanillaBlockItem(Map.of());
        if (BlockItemModelHelper.blockItemTransforms() != ItemTransforms.NO_TRANSFORMS)
            throw new AssertionError("Missing resource pack retained stale transforms");
        System.out.println("PASS client model lookup and resource-reload contracts (headless)");
    }

    private static void previewNormals() {
        Object[][] emitted = new Object[1][];
        var sink = (com.mojang.blaze3d.vertex.VertexConsumer)Proxy.newProxyInstance(
                ClientModelContracts.class.getClassLoader(),new Class[]{com.mojang.blaze3d.vertex.VertexConsumer.class},
                (proxy,method,args)-> {
                    if(method.getName().equals("vertex") && args.length==14) emitted[0]=args.clone();
                    return method.getReturnType()==com.mojang.blaze3d.vertex.VertexConsumer.class?proxy:null;
                });
        new com.gregtech.gregtech.jei.PreviewLighting(sink).vertex(0,0,0,.2f,.4f,.8f,.5f,
                .25f,.75f,123,15728880,1,0,0);
        var vertex=emitted[0];
        if(vertex==null || Math.abs((float)vertex[3]-.15f)>.0001f
                || Math.abs((float)vertex[5]-.6f)>.0001f || (float)vertex[6]!=.5f
                || (int)vertex[9]!=123 || (int)vertex[10]!=15728880 || (float)vertex[12]!=1)
            throw new AssertionError("Preview fill must preserve tint ratios, alpha, overlay and lightmap");
        for (int yaw=0; yaw<360; yaw+=15) for (int pitch=-90; pitch<=90; pitch+=15) {
            var rotation = new org.joml.Matrix3f().rotateX((float)Math.toRadians(pitch)).rotateY((float)Math.toRadians(yaw));
            for (var face : net.minecraft.core.Direction.values()) {
                var step = face.getNormal();
                var normal = rotation.transform(new org.joml.Vector3f(step.getX(),step.getY(),step.getZ()));
                float brightness = com.gregtech.gregtech.jei.PreviewLighting.brightness(normal.x,normal.y,normal.z);
                if (!Float.isFinite(brightness) || brightness < .75f || brightness > 1)
                    throw new AssertionError("Preview face lost ambient fill at yaw="+yaw+" pitch="+pitch);
            }
        }
        for (float scale : new float[]{0.1f, 1, 15, 100}) {
            var pose = new com.mojang.blaze3d.vertex.PoseStack();
            com.gregtech.gregtech.jei.PreviewTransforms.scaleForGui(pose, scale);
            var up = pose.last().normal().transform(new org.joml.Vector3f(0, 1, 0));
            if (!Float.isFinite(up.y) || Math.abs(up.y + 1) > 0.001f)
                throw new AssertionError("Preview GUI reflection corrupts lighting normals: " + up);
        }
    }

    private static void fluidFallbackContracts() {
        var preferred = ResourceLocation.fromNamespaceAndPath("gregtech", "block/fluids/test_liquid");
        var fluid = ResourceLocation.fromNamespaceAndPath("gregtech", "block/material_icons/fluid/molten");
        var gas = ResourceLocation.fromNamespaceAndPath("gregtech", "block/material_icons/fluid/gas");
        var molten = ResourceLocation.fromNamespaceAndPath("gregtech", "block/material_icons/metallic/molten");
        var available = new java.util.HashSet<>(java.util.Set.of(fluid, gas, molten));
        var plasma = com.gregtech.gregtech.api.fluid.FluidTexturePolicy.Kind.PLASMA.standard();
        available.add(plasma);
        if (!com.gregtech.gregtech.api.fluid.FluidTexturePolicy.select(preferred,
                com.gregtech.gregtech.api.fluid.FluidTexturePolicy.Kind.PLASMA, available::contains).equals(plasma))
            throw new AssertionError("Plasma has its own fallback category");
        if (com.gregtech.gregtech.api.fluid.FluidTexturePolicy.kind(new com.gregtech.gregtech.data.RegisteredFluids.FluidEntry("molten.copper"))
                != com.gregtech.gregtech.api.fluid.FluidTexturePolicy.Kind.MOLTEN
                || com.gregtech.gregtech.api.fluid.FluidTexturePolicy.kind(new com.gregtech.gregtech.data.RegisteredFluids.FluidEntry("oxygen").withGas())
                != com.gregtech.gregtech.api.fluid.FluidTexturePolicy.Kind.GAS)
            throw new AssertionError("Actual registry entries must classify by phase");
        if (!com.gregtech.gregtech.api.fluid.FluidTexturePolicy.select(preferred, false, false, available::contains).equals(fluid)
                || !com.gregtech.gregtech.api.fluid.FluidTexturePolicy.select(preferred, false, true, available::contains).equals(gas)
                || !com.gregtech.gregtech.api.fluid.FluidTexturePolicy.select(preferred, true, false, available::contains).equals(molten))
            throw new AssertionError("Missing fluid sprite must use the appropriate standard texture");
        available.add(preferred);
        if (!com.gregtech.gregtech.api.fluid.FluidTexturePolicy.select(preferred, false, false, available::contains).equals(preferred))
            throw new AssertionError("Resource-pack dedicated sprite must override fallback");
        available.clear();
        if (!com.gregtech.gregtech.api.fluid.FluidTexturePolicy.select(preferred, false, false, available::contains)
                .equals(ResourceLocation.withDefaultNamespace("block/water_still")))
            throw new AssertionError("Missing standard texture needs final vanilla fallback");
    }

    private static void connectorInventoryContracts() {
        // Verify the geometry policy used by the item renderer; atlas baking needs a Forge client.
        int count = 0;
        for (var dir : net.minecraft.core.Direction.values()) {
            if (!com.gregtech.gregtech.api.machine.PipeGeometry.itemConnected(dir)) continue;
            count++;
            var bounds = com.gregtech.gregtech.api.machine.PipeGeometry.armBox(dir, 2,
                    com.gregtech.gregtech.api.machine.PipeGeometry.NONE);
            if (bounds == null || (dir == net.minecraft.core.Direction.SOUTH ? bounds[5] != 16
                    : dir != net.minecraft.core.Direction.NORTH || bounds[2] != 0))
                throw new AssertionError("Inventory connector must reach both block boundaries");
        }
        if (count != 2) throw new AssertionError("Inventory must have exactly two opposite arms");
    }

    private static ItemTransforms transforms() {
        var none = ItemTransform.NO_TRANSFORM;
        return new ItemTransforms(none, none, none, none, none, none, none, none);
    }

    private static BakedModel model(ItemTransforms transforms) {
        return (BakedModel) Proxy.newProxyInstance(BakedModel.class.getClassLoader(), new Class[]{BakedModel.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getTransforms" -> transforms;
                    case "isGui3d" -> true;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "TestBakedModel";
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
