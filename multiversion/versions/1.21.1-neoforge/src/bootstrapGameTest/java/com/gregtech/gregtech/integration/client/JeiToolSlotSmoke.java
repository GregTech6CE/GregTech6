package com.gregtech.gregtech.integration.client;

import com.gregtech.gregtech.api.recipe.*;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.item.GTToolItem;
import com.gregtech.gregtech.jei.RecipeMapCategory;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.world.item.ItemStack;
import java.lang.reflect.Proxy;
import java.util.List;

/** Exercise the real JEI category's special-tool slot using recording native builders. */
final class JeiToolSlotSmoke {
    @SuppressWarnings("unchecked")
    static void check() {
        var tool=GTToolItem.create(GTToolType.AXE,GTMaterialRegistry.get("Iron"),GTMaterialRegistry.get("Wood"));
        var displayed=new java.util.ArrayList<ItemStack>();
        IRecipeSlotBuilder slot=(IRecipeSlotBuilder)Proxy.newProxyInstance(IRecipeSlotBuilder.class.getClassLoader(),
                new Class<?>[]{IRecipeSlotBuilder.class},(proxy,method,args)-> {
                    if(method.getName().equals("addItemStacks")) displayed.addAll((List<ItemStack>)args[0]);
                    return method.getReturnType().isInstance(proxy)?proxy:null;
                });
        int[] slots={0};
        IRecipeLayoutBuilder builder=(IRecipeLayoutBuilder)Proxy.newProxyInstance(IRecipeLayoutBuilder.class.getClassLoader(),
                new Class<?>[]{IRecipeLayoutBuilder.class},(proxy,method,args)-> {
                    if(method.getName().equals("addSlot")) {
                        if(args[0]!=RecipeIngredientRole.CATALYST||!args[1].equals(80)||!args[2].equals(43))
                            throw new IllegalStateException("Special tool must use original non-consumed slot");
                        slots[0]++; return slot;
                    }
                    return method.getReturnType().isInstance(proxy)?proxy:null;
                });
        IGuiHelper helper=(IGuiHelper)Proxy.newProxyInstance(IGuiHelper.class.getClassLoader(),new Class<?>[]{IGuiHelper.class},
                (proxy,method,args)-> {
                    var type=method.getReturnType();
                    return type.isInterface()?Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},
                            (drawable,m,a)->m.getReturnType()==int.class?18:null):null;
                });
        var recipe=new Recipe(null,null,new Object[]{tool,ItemStack.EMPTY},null,null,null,20,16,0);
        new RecipeMapCategory(helper,RecipeMap.RECIPE_MAP_LIST.get(0),tool).setRecipe(builder,recipe,null);
        if(slots[0]!=1||displayed.size()!=1||displayed.get(0)==tool||!ItemStack.isSameItemSameComponents(displayed.get(0),tool))
            throw new IllegalStateException("JEI special tool missing or changed original stack");
        com.mojang.logging.LogUtils.getLogger().info("JEI_TOOL_SLOT_SMOKE_SUCCESS non-consumed tool visible at 80,43, original stack preserved");
    }
}
