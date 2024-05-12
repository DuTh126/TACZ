package com.tacz.guns.api.item.builder;

import com.google.common.base.Preconditions;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.api.item.gun.GunItemManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.RegistryObject;

public final class GunItemBuilder {
    private int count = 1;
    private int ammoCount = 0;
    private ResourceLocation gunId;
    private FireMode fireMode = FireMode.UNKNOWN;
    private boolean bulletInBarrel = false;

    private GunItemBuilder() {
    }

    public static GunItemBuilder create() {
        return new GunItemBuilder();
    }

    public GunItemBuilder setCount(int count) {
        this.count = Math.max(count, 1);
        return this;
    }

    public GunItemBuilder setAmmoCount(int count) {
        this.ammoCount = Math.max(count, 0);
        return this;
    }

    public GunItemBuilder setId(ResourceLocation id) {
        this.gunId = id;
        return this;
    }

    public GunItemBuilder setFireMode(FireMode fireMode) {
        this.fireMode = fireMode;
        return this;
    }

    public GunItemBuilder setAmmoInBarrel(boolean ammoInBarrel) {
        this.bulletInBarrel = ammoInBarrel;
        return this;
    }

    public ItemStack build() {
        String itemType = TimelessAPI.getCommonGunIndex(gunId).map(index -> index.getPojo().getItemType()).orElse(null);
        Preconditions.checkArgument(itemType != null, "Could not found gun id: " + gunId);

        RegistryObject<? extends AbstractGunItem> gunItemRegistryObject = GunItemManager.getGunItemRegistryObject(itemType);
        Preconditions.checkArgument(gunItemRegistryObject != null, "Could not found gun item type: " + itemType);

        ItemStack gun = new ItemStack(gunItemRegistryObject.get(), this.count);
        if (gun.getItem() instanceof IGun iGun) {
            iGun.setGunId(gun, this.gunId);
            iGun.setFireMode(gun, this.fireMode);
            iGun.setCurrentAmmoCount(gun, this.ammoCount);
            iGun.setBulletInBarrel(gun, this.bulletInBarrel);
        }
        return gun;
    }
}
