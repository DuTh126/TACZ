package com.tacz.guns.api.item;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.resource.pojo.data.gun.AttachmentPass;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

/**
 * 这里不包含枪械的逻辑，只包含枪械的各种 nbt 访问。你可以在 {@link AbstractGunItem} 看到枪械逻辑
 */
public interface IGun {
    /**
     * @return 如果物品类型为 IGun 则返回显式转换后的实例，否则返回 null。
     */
    @Nullable
    static IGun getIGunOrNull(@Nullable ItemStack stack) {
        if (stack == null) {
            return null;
        }
        if (stack.getItem() instanceof IGun iGun) {
            return iGun;
        }
        return null;
    }

    /**
     * 是否主手持枪
     */
    static boolean mainhandHoldGun(LivingEntity livingEntity) {
        return livingEntity.getMainHandItem().getItem() instanceof IGun;
    }

    /**
     * 获取主手枪械的开火模式
     */
    static FireMode getMainhandFireMode(LivingEntity livingEntity) {
        ItemStack mainhandItem = livingEntity.getMainHandItem();
        if (mainhandItem.getItem() instanceof IGun iGun) {
            return iGun.getFireMode(mainhandItem);
        }
        return FireMode.UNKNOWN;
    }

    /**
     * 获取瞄准进度
     *
     * @return 0-1，1 代表 100% 进度
     */
    float getAimingZoom(ItemStack gunItem);

    /**
     * 获取枪械 ID
     */
    @Nonnull
    ResourceLocation getGunId(ItemStack gun);

    /**
     * 设置枪械 ID
     */
    void setGunId(ItemStack gun, @Nullable ResourceLocation gunId);

    /**
     * 获取输入的经验值对应的等级。
     *
     * @param exp 经验值
     * @return 对应的等级
     */
    int getLevel(int exp);

    /**
     * 获取输入的等级需要至少多少的经验值。
     *
     * @param level 等级
     * @return 至少需要的经验值
     */
    int getExp(int level);

    /**
     * 返回允许的最大等级。
     *
     * @return 最大等级
     */
    int getMaxLevel();

    /**
     * 获取枪械当前等级
     */
    int getLevel(ItemStack gun);

    /**
     * 获取积累的全部经验值。
     *
     * @param gun 输入物品
     * @return 全部经验值
     */
    int getExp(ItemStack gun);

    /**
     * 获取到下个等级需要的经验值。
     *
     * @param gun 输入物品
     * @return 到下个等级需要的经验值。如果等级已经到达最大，则返回 0
     */
    int getExpToNextLevel(ItemStack gun);

    /**
     * 获取当前等级已经积累的经验值。
     *
     * @param gun 输入物品
     * @return 当前等级已经积累的经验值
     */
    int getExpCurrentLevel(ItemStack gun);

    /**
     * 获取开火模式
     *
     * @param gun 枪
     * @return 开火模式
     */
    FireMode getFireMode(ItemStack gun);

    /**
     * 设置开火模式
     */
    void setFireMode(ItemStack gun, @Nullable FireMode fireMode);

    /**
     * 获取当前枪械弹药数
     */
    int getCurrentAmmoCount(ItemStack gun);

    /**
     * 设置当前枪械弹药数
     */
    void setCurrentAmmoCount(ItemStack gun, int ammoCount);

    /**
     * 减少一个当前枪械弹药数
     */
    void reduceCurrentAmmoCount(ItemStack gun);

    /**
     * 获取当前枪械指定类型的配件
     */
    @Nonnull
    ItemStack getAttachment(ItemStack gun, AttachmentType type);

    /**
     * 安装配件
     */
    void installAttachment(@Nonnull ItemStack gun, @Nonnull ItemStack attachment);

    /**
     * 卸载配件
     */
    void unloadAttachment(@Nonnull ItemStack gun, AttachmentType type);

    /**
     * 该枪械是否允许装配该配件
     */
    default boolean allowAttachment(ItemStack gun, ItemStack attachmentItem) {
        IAttachment iAttachment = IAttachment.getIAttachmentOrNull(attachmentItem);
        IGun iGun = IGun.getIGunOrNull(gun);
        if (iGun != null && iAttachment != null) {
            AttachmentType type = iAttachment.getType(attachmentItem);
            ResourceLocation attachmentId = iAttachment.getAttachmentId(attachmentItem);
            return TimelessAPI.getCommonGunIndex(iGun.getGunId(gun)).map(gunIndex -> {
                Map<AttachmentType, AttachmentPass> map = gunIndex.getGunData().getAllowAttachments();
                if (map == null) {
                    return false;
                }
                AttachmentPass pass = map.get(type);
                if (pass == null) {
                    return false;
                }
                return pass.isAllow(attachmentId);
            }).orElse(false);
        } else {
            return false;
        }
    }

    /**
     * 该枪械是否允许某类型配件
     */
    default boolean allowAttachmentType(ItemStack gun, AttachmentType type) {
        IGun iGun = IGun.getIGunOrNull(gun);
        if (iGun != null) {
            return TimelessAPI.getCommonGunIndex(iGun.getGunId(gun)).map(gunIndex -> {
                Map<AttachmentType, AttachmentPass> map = gunIndex.getGunData().getAllowAttachments();
                if (map == null) {
                    return false;
                }
                return map.containsKey(type);
            }).orElse(false);
        } else {
            return false;
        }
    }

    /**
     * 枪管中是否有子弹，用于闭膛待击的枪械
     */
    boolean hasBulletInBarrel(ItemStack gun);

    /**
     * 设置枪管中的子弹有无，用于闭膛待击的枪械
     */
    void setBulletInBarrel(ItemStack gun, boolean bulletInBarrel);
}