package com.tacz.guns.client.renderer.other;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.resource.pojo.display.gun.LayerGunShow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class HumanoidOffhandRender {
    public static void renderGun(LivingEntity entity, PoseStack matrixStack, MultiBufferSource buffer, int packedLight) {
        renderOffhandGun(entity, matrixStack, buffer, packedLight);
        renderHotbarGun(entity, matrixStack, buffer, packedLight);
    }

    private static void renderOffhandGun(LivingEntity entity, PoseStack matrixStack, MultiBufferSource buffer, int packedLight) {
        ItemStack itemStack = entity.getOffhandItem();
        if (itemStack.isEmpty()) {
            return;
        }
        IGun iGun = IGun.getIGunOrNull(itemStack);
        if (iGun == null) {
            return;
        }
        ResourceLocation gunId = iGun.getGunId(itemStack);
        TimelessAPI.getClientGunIndex(gunId).ifPresent(index -> {
            LayerGunShow offhandShow = index.getOffhandShow();
            renderGunItem(entity, matrixStack, buffer, packedLight, itemStack, offhandShow);
        });
    }

    private static void renderHotbarGun(LivingEntity entity, PoseStack matrixStack, MultiBufferSource buffer, int packedLight) {
        if (!(entity instanceof Player player)) {
            return;
        }
        Inventory inventory = player.getInventory();
        for (int i = 0; i < 9; i++) {
            if (i == inventory.selected) {
                continue;
            }
            ItemStack stack = inventory.getItem(i);
            renderHotbarGun(entity, matrixStack, buffer, packedLight, stack, i);
        }
    }

    private static void renderHotbarGun(LivingEntity entity, PoseStack matrixStack, MultiBufferSource buffer, int packedLight, ItemStack itemStack, int inventoryIndex) {
        if (itemStack.isEmpty()) {
            return;
        }
        IGun iGun = IGun.getIGunOrNull(itemStack);
        if (iGun == null) {
            return;
        }
        ResourceLocation gunId = iGun.getGunId(itemStack);
        TimelessAPI.getClientGunIndex(gunId).ifPresent(index -> {
            var hotbarShow = index.getHotbarShow();
            if (hotbarShow == null || hotbarShow.isEmpty()) {
                return;
            }
            if (!hotbarShow.containsKey(inventoryIndex)) {
                return;
            }
            LayerGunShow gunShow = hotbarShow.get(inventoryIndex);
            renderGunItem(entity, matrixStack, buffer, packedLight, itemStack, gunShow);
        });
    }

    private static void renderGunItem(LivingEntity entity, PoseStack matrixStack, MultiBufferSource buffer, int packedLight, ItemStack itemStack, LayerGunShow offhandShow) {
        ItemRenderer renderer = Minecraft.getInstance().getItemRenderer();
        Vector3f pos = offhandShow.getPos();
        Vector3f rotate = offhandShow.getRotate();
        Vector3f scale = offhandShow.getScale();
        matrixStack.pushPose();
        matrixStack.translate(pos.x() / 16f, 1.5 - pos.y() / 16f, pos.z() / 16f);
        matrixStack.scale(-scale.x(), -scale.y(), scale.z());
        matrixStack.mulPose(Quaternion.fromXYZDegrees(rotate));
        renderer.renderStatic(itemStack, ItemTransforms.TransformType.FIXED, packedLight, OverlayTexture.NO_OVERLAY, matrixStack, buffer, entity.getId());
        matrixStack.popPose();
    }
}
