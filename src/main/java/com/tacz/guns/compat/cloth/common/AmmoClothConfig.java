package com.tacz.guns.compat.cloth.common;

import com.google.common.collect.Lists;
import com.tacz.guns.config.common.AmmoConfig;
import com.tacz.guns.config.sync.SyncConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.TranslatableComponent;

public class AmmoClothConfig {
    public static void init(ConfigBuilder root, ConfigEntryBuilder entryBuilder) {
        ConfigCategory ammo = root.getOrCreateCategory(new TranslatableComponent("config.tacz.common.ammo"));

        ammo.addEntry(entryBuilder.startBooleanToggle(new TranslatableComponent("config.tacz.common.ammo.explosive_ammo_destroys_blocks"), AmmoConfig.EXPLOSIVE_AMMO_DESTROYS_BLOCKS.get())
                .setDefaultValue(false).setTooltip(new TranslatableComponent("config.tacz.common.ammo.explosive_ammo_destroys_blocks.desc"))
                .setSaveConsumer(AmmoConfig.EXPLOSIVE_AMMO_DESTROYS_BLOCKS::set).build());

        ammo.addEntry(entryBuilder.startBooleanToggle(new TranslatableComponent("config.tacz.common.ammo.explosive_ammo_fire"), AmmoConfig.EXPLOSIVE_AMMO_FIRE.get())
                .setDefaultValue(false).setTooltip(new TranslatableComponent("config.tacz.common.ammo.explosive_ammo_fire.desc"))
                .setSaveConsumer(AmmoConfig.EXPLOSIVE_AMMO_FIRE::set).build());

        ammo.addEntry(entryBuilder.startBooleanToggle(new TranslatableComponent("config.tacz.common.ammo.explosive_ammo_knock_back"), AmmoConfig.EXPLOSIVE_AMMO_KNOCK_BACK.get())
                .setDefaultValue(true).setTooltip(new TranslatableComponent("config.tacz.common.ammo.explosive_ammo_knock_back.desc"))
                .setSaveConsumer(AmmoConfig.EXPLOSIVE_AMMO_KNOCK_BACK::set).build());

        ammo.addEntry(entryBuilder.startIntField(new TranslatableComponent("config.tacz.common.ammo.explosive_ammo_visible_distance"), AmmoConfig.EXPLOSIVE_AMMO_VISIBLE_DISTANCE.get())
                .setMin(0).setMax(Integer.MAX_VALUE).setDefaultValue(192).setTooltip(new TranslatableComponent("config.tacz.common.ammo.explosive_ammo_visible_distance.desc"))
                .setSaveConsumer(AmmoConfig.EXPLOSIVE_AMMO_VISIBLE_DISTANCE::set).build());

        ammo.addEntry(entryBuilder.startStrList(new TranslatableComponent("config.tacz.common.ammo.pass_through_blocks"), AmmoConfig.PASS_THROUGH_BLOCKS.get())
                .setDefaultValue(Lists.newArrayList()).setTooltip(new TranslatableComponent("config.tacz.common.ammo.pass_through_blocks.desc"))
                .setSaveConsumer(AmmoConfig.PASS_THROUGH_BLOCKS::set).build());

        ammo.addEntry(entryBuilder.startDoubleField(new TranslatableComponent("config.tacz.common.ammo.damage_base_multiplier"), SyncConfig.DAMAGE_BASE_MULTIPLIER.get())
                .setMin(0).setMax(Integer.MAX_VALUE).setDefaultValue(1).setTooltip(new TranslatableComponent("config.tacz.common.ammo.damage_base_multiplier.desc"))
                .setSaveConsumer(SyncConfig.DAMAGE_BASE_MULTIPLIER::set).build());

        ammo.addEntry(entryBuilder.startDoubleField(new TranslatableComponent("config.tacz.common.ammo.armor_ignore_base_multiplier"), SyncConfig.ARMOR_IGNORE_BASE_MULTIPLIER.get())
                .setMin(0).setMax(Integer.MAX_VALUE).setDefaultValue(1).setTooltip(new TranslatableComponent("config.tacz.common.ammo.armor_ignore_base_multiplier.desc"))
                .setSaveConsumer(SyncConfig.ARMOR_IGNORE_BASE_MULTIPLIER::set).build());

        ammo.addEntry(entryBuilder.startDoubleField(new TranslatableComponent("config.tacz.common.ammo.head_shot_base_multiplier"), SyncConfig.HEAD_SHOT_BASE_MULTIPLIER.get())
                .setMin(0).setMax(Integer.MAX_VALUE).setDefaultValue(1).setTooltip(new TranslatableComponent("config.tacz.common.ammo.head_shot_base_multiplier.desc"))
                .setSaveConsumer(SyncConfig.HEAD_SHOT_BASE_MULTIPLIER::set).build());

        ammo.addEntry(entryBuilder.startBooleanToggle(new TranslatableComponent("config.tacz.common.ammo.destroy_glass"), AmmoConfig.DESTROY_GLASS.get())
                .setDefaultValue(true).setTooltip(new TranslatableComponent("config.tacz.common.ammo.destroy_glass.desc"))
                .setSaveConsumer(AmmoConfig.DESTROY_GLASS::set).build());

        ammo.addEntry(entryBuilder.startBooleanToggle(new TranslatableComponent("config.tacz.common.ammo.ignite_block"), AmmoConfig.IGNITE_BLOCK.get())
                .setDefaultValue(true).setTooltip(new TranslatableComponent("config.tacz.common.ammo.ignite_block.desc"))
                .setSaveConsumer(AmmoConfig.IGNITE_BLOCK::set).build());

        ammo.addEntry(entryBuilder.startBooleanToggle(new TranslatableComponent("config.tacz.common.ammo.ignite_entity"), AmmoConfig.IGNITE_ENTITY.get())
                .setDefaultValue(true).setTooltip(new TranslatableComponent("config.tacz.common.ammo.ignite_entity.desc"))
                .setSaveConsumer(AmmoConfig.IGNITE_ENTITY::set).build());
    }
}
