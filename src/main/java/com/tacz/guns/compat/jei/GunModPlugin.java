package com.tacz.guns.compat.jei;

import com.google.common.collect.Lists;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.gun.GunItemManager;
import com.tacz.guns.init.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public class GunModPlugin implements IModPlugin {
    private static final ResourceLocation UID = new ResourceLocation(GunMod.MOD_ID, "jei");

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new GunSmithTableCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(GunSmithTableCategory.GUN_SMITH_TABLE, Lists.newArrayList(TimelessAPI.getAllRecipes().values()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(ModItems.GUN_SMITH_TABLE.get().getDefaultInstance(), GunSmithTableCategory.GUN_SMITH_TABLE);
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, ModItems.AMMO.get(), GunModSubtype.getAmmoSubtype());
        GunItemManager.getAllGunItems().forEach(item -> registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, item.get(), GunModSubtype.getGunSubtype()));
        registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, ModItems.ATTACHMENT.get(), GunModSubtype.getAttachmentSubtype());
    }

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }
}
