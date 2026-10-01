package com.gregtech.gregtech.client;


import com.gregtech.gregtech.block.BookShelfBlock;
import com.gregtech.gregtech.blockentity.inventory.BookShelfBlockEntity;
import com.gregtech.gregtech.content.book.BookShelfGeometry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Original GT6 book cuboids and book spine/side textures, driven by the synchronized inventory. */
public final class BookShelfRenderer implements BlockEntityRenderer<BookShelfBlockEntity> {
    public BookShelfRenderer(BlockEntityRendererProvider.Context context) {}

    private static String texture(ItemStack stack) {
        if (stack.is(Items.ENCHANTED_BOOK)) return "book_enchanted";
        // GT6 LoaderBookList display IDs 2 and 255, respectively.
        if (stack.is(Items.STONE_BUTTON) || stack.is(Items.REDSTONE_TORCH)) return "book_enchanted";
        if (stack.is(Items.FILLED_MAP)) return "folder_red";
        if (stack.is(Items.PAPER) || stack.is(Items.MAP) || stack.is(Items.NAME_TAG)) return "folder";
        if (stack.is(Items.ITEM_FRAME) || stack.is(Items.PAINTING)) return "frame";
        var key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (key != null && key.getPath().equals("dusty_guide_book")) return "book_dusty";
        if (key != null && key.getPath().equals("dusty_material_dictionary")) return "book_matdict";
        return "book_vanilla";
    }

    private static ResourceLocation bookTexture(ItemStack stack, String texture, String face) {
        return stack.is(Items.COBBLESTONE)
                ? ResourceLocation.parse("minecraft:textures/block/cobblestone.png")
                : ResourceLocation.fromNamespaceAndPath("gregtech","textures/block/books/" + texture + "_" + face + ".png");
    }

    @Override
    public void render(BookShelfBlockEntity shelf, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(.5, 0, .5);
        pose.mulPose(Axis.YP.rotationDegrees(180 - shelf.getBlockState().getValue(BookShelfBlock.FACING).toYRot()));
        pose.translate(-.5, 0, -.5);
        for (int slot = 0; slot < shelf.inventory().getSlots(); slot++) {
            var stack = shelf.inventory().getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            var b = BookShelfGeometry.bookBounds(slot);
            String texture = texture(stack);
            var spine = buffers.getBuffer(RenderType.entityCutout(bookTexture(stack, texture, "back")));
            float x0=(float)b.minX, x1=(float)b.maxX, y0=(float)b.minY, y1=(float)b.maxY, z0=(float)b.minZ, z1=(float)b.maxZ;
            if (slot < 14) {
                AnvilCuboidRenderer.face(pose.last(),spine,0,1,0,1,0xFFFFFF,light,Direction.NORTH,
                        new float[]{x0,y0,z0,x0,y1,z0,x1,y1,z0,x1,y0,z0});
            } else {
                AnvilCuboidRenderer.face(pose.last(),spine,0,1,0,1,0xFFFFFF,light,Direction.SOUTH,
                        new float[]{x0,y0,z1,x1,y0,z1,x1,y1,z1,x0,y1,z1});
            }
            var side = buffers.getBuffer(RenderType.entityCutout(bookTexture(stack, texture, "side")));
            AnvilCuboidRenderer.face(pose.last(),side,0,1,0,1,0xFFFFFF,light,Direction.WEST,
                    new float[]{x0,y0,z0,x0,y0,z1,x0,y1,z1,x0,y1,z0});
            AnvilCuboidRenderer.face(pose.last(),side,0,1,0,1,0xFFFFFF,light,Direction.EAST,
                    new float[]{x1,y0,z0,x1,y1,z0,x1,y1,z1,x1,y0,z1});
        }
        pose.popPose();
    }
}
