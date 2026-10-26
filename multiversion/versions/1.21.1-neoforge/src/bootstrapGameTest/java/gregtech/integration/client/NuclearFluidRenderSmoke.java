package gregtech.integration.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import gregtech.content.blockentity.energy.ReactorCoreBlockEntity;
import gregtech.content.nuclear.ReactorCoolants;
import gregtech.content.registry.GTEnergyNodes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.fluids.FluidStack;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Opt-in atlas/BER regression for issue 120; excluded from the production mod. */
@EventBusSubscriber(modid = "gt6ce", value = Dist.CLIENT)
public final class NuclearFluidRenderSmoke {
    private static final boolean ENABLED = Boolean.getBoolean("gregtech.integration.nuclearFluidSmoke");
    private static boolean terminal;
    private static int frames;

    @SubscribeEvent
    public static void render(ScreenEvent.Render.Post event) {
        var client = Minecraft.getInstance();
        if (!ENABLED || terminal || !(event.getScreen() instanceof TitleScreen) || client.getOverlay() != null) return;
        if (++frames < 5) return;
        terminal = true;
        try {
            int cases = 0;
            for (boolean four : new boolean[]{false, true}) {
                var core = core(four);
                require(capture(core).isEmpty(), "Empty core must not draw coolant");
                for (var coolant : ReactorCoolants.values()) {
                    for (int amount : new int[]{1, 64000}) {
                        core.input.setFluid(new FluidStack(coolant.inputFluid(), amount));
                        var texture = net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions
                                .of(coolant.inputFluid()).getStillTexture(core.input.getFluid());
                        require(texture != null && gregtech.client.CrucibleRenderSprites.hasTexture(texture),
                                "Native coolant sprite is stitched: " + coolant);
                        var vertices = capture(core);
                        require(vertices.size() == 24, "Six coolant faces: " + coolant + "/" + four + "/" + amount);
                        float min = 2 / 16f + .005f, max = 14 / 16f - .005f;
                        for (var vertex : vertices) {
                            require(vertex[0] >= min - .00001f && vertex[0] <= max + .00001f
                                    && vertex[2] >= min - .00001f && vertex[2] <= max + .00001f
                                    && vertex[1] >= .125f - .00001f && vertex[1] <= .875f + .00001f,
                                    "Coolant stays inside original reactor cavity");
                        }
                        require(vertices.stream().anyMatch(v -> Math.abs(v[1] - .875f) < .00001f), "Visible top surface");
                        cases++;
                    }
                }
                core.input.setEmpty();
                core.output.setFluid(new FluidStack(ReactorCoolants.INDUSTRIAL.outputFluid(), 64000));
                require(capture(core).isEmpty(), "Hot output buffer does not fill the cold coolant cavity");
            }
            var graphics = event.getGuiGraphics();
            graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), 0xFF20242C);
            graphics.drawString(client.font, "Reactor coolant: empty / distilled water / molten sodium", 8, 8, 0xFFFFFF);
            for (int row = 0; row < 2; row++) for (int col = 0; col < 3; col++) {
                var core = core(row == 1);
                if (col > 0) core.input.setFluid(new FluidStack((col == 1 ? ReactorCoolants.DISTILLED : ReactorCoolants.SODIUM).inputFluid(), 64000));
                for (int slot = 0; slot < core.rods.length; slot++)
                    core.insertRod(slot, gregtech.content.registry.GTFuelRods.stack(slot % 2 == 0 ? 9221 : 9410));
                var pose = graphics.pose();
                pose.pushPose();
                pose.translate(graphics.guiWidth() * (col + .5) / 3, 90 + row * 100, 100);
                pose.scale(60, -60, 60);
                pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(25));
                pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-35));
                pose.translate(-.5, 0, -.5);
                var buffers = client.renderBuffers().bufferSource();
                client.getBlockRenderer().renderSingleBlock(core.getBlockState(), pose, buffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                client.getBlockEntityRenderDispatcher().getRenderer(core).render(core, 0, pose, buffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                buffers.endBatch();
                pose.popPose();
            }
            graphics.flush();
            String name = "nuclear-fluid-" + UUID.randomUUID() + ".png";
            int checked = cases;
            Screenshot.grab(client.gameDirectory, name, client.getMainRenderTarget(), message -> {
                LogUtils.getLogger().info("NUCLEAR_FLUID_RENDER_SUCCESS cases={} screenshot={}", checked,
                        client.gameDirectory.toPath().resolve("screenshots").resolve(name).toAbsolutePath());
                client.execute(client::stop);
            });
        } catch (Throwable error) {
            LogUtils.getLogger().error("NUCLEAR_FLUID_RENDER_FAILED", error);
            client.execute(client::stop);
        }
    }

    private static ReactorCoreBlockEntity core(boolean four) {
        var block = four ? GTEnergyNodes.REACTOR_CORE_2X2.get() : GTEnergyNodes.REACTOR_CORE_BLOCK.get();
        return (ReactorCoreBlockEntity) ((net.minecraft.world.level.block.EntityBlock) block).newBlockEntity(BlockPos.ZERO, block.defaultBlockState());
    }

    private static List<float[]> capture(ReactorCoreBlockEntity core) {
        var vertices = new ArrayList<float[]>();
        var consumer = (VertexConsumer) Proxy.newProxyInstance(VertexConsumer.class.getClassLoader(), new Class<?>[]{VertexConsumer.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("addVertex")) {
                        int offset = args.length - 3;
                        vertices.add(new float[]{((Number) args[offset]).floatValue(), ((Number) args[offset + 1]).floatValue(), ((Number) args[offset + 2]).floatValue()});
                    }
                    return method.getReturnType() == VertexConsumer.class ? proxy : null;
                });
        MultiBufferSource buffers = type -> consumer;
        var renderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(core);
        require(renderer != null, "Registered reactor BER");
        renderer.render(core, 0, new PoseStack(), buffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        return vertices;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
