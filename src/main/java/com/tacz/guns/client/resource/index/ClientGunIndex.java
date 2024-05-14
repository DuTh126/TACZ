package com.tacz.guns.client.resource.index;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.client.animation.*;
import com.tacz.guns.client.animation.gltf.AnimationStructure;
import com.tacz.guns.client.animation.internal.GunAnimationStateMachine;
import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.model.bedrock.BedrockModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.client.resource.ClientAssetManager;
import com.tacz.guns.client.resource.InternalAssetLoader;
import com.tacz.guns.client.resource.pojo.animation.bedrock.BedrockAnimationFile;
import com.tacz.guns.client.resource.pojo.display.gun.*;
import com.tacz.guns.client.resource.pojo.model.BedrockModelPOJO;
import com.tacz.guns.client.resource.pojo.model.BedrockVersion;
import com.tacz.guns.resource.CommonAssetManager;
import com.tacz.guns.resource.pojo.GunIndexPOJO;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.sound.SoundManager;
import com.tacz.guns.util.ColorHex;
import com.tacz.guns.util.math.MathUtil;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@OnlyIn(Dist.CLIENT)
public class ClientGunIndex {
    private String name;
    private String thirdPersonAnimation = "empty";
    private BedrockGunModel gunModel;
    private @Nullable Pair<BedrockGunModel, ResourceLocation> lodModel;
    private GunAnimationStateMachine animationStateMachine;
    private Map<String, ResourceLocation> sounds;
    private GunTransform transform;
    private GunData gunData;
    private ResourceLocation modelTexture;
    private ResourceLocation slotTexture;
    private ResourceLocation hudTexture;
    private @Nullable ResourceLocation hudEmptyTexture;
    private String type;
    private String itemType;
    private @Nullable ShellEjection shellEjection;
    private @Nullable MuzzleFlash muzzleFlash;
    private LayerGunShow offhandShow;
    private @Nullable Int2ObjectArrayMap<LayerGunShow> hotbarShow;
    private float ironZoom;
    private int sort;

    private ClientGunIndex() {
    }

    public static ClientGunIndex getInstance(GunIndexPOJO gunIndexPOJO) throws IllegalArgumentException {
        ClientGunIndex index = new ClientGunIndex();
        checkIndex(gunIndexPOJO, index);
        GunDisplay display = checkDisplay(gunIndexPOJO);
        checkData(gunIndexPOJO, index);
        checkName(gunIndexPOJO, index);
        checkTextureAndModel(display, index);
        checkLod(display, index);
        checkSlotTexture(display, index);
        checkHUDTexture(display, index);
        checkAnimation(display, index);
        checkSounds(display, index);
        checkTransform(display, index);
        checkShellEjection(display, index);
        checkMuzzleFlash(display, index);
        checkLayerGunShow(display, index);
        checkIronZoom(display, index);
        checkTextShow(display, index);
        return index;
    }

    private static void checkIndex(GunIndexPOJO gunIndexPOJO, ClientGunIndex index) {
        Preconditions.checkArgument(gunIndexPOJO != null, "index object file is empty");
        Preconditions.checkArgument(StringUtils.isNoneBlank(gunIndexPOJO.getType()), "index object missing type field");
        index.type = gunIndexPOJO.getType();
        index.itemType = gunIndexPOJO.getItemType();
        index.sort = Mth.clamp(gunIndexPOJO.getSort(), 0, 65536);
    }

    private static void checkName(GunIndexPOJO gunIndexPOJO, ClientGunIndex index) {
        index.name = gunIndexPOJO.getName();
        if (StringUtils.isBlank(index.name)) {
            index.name = "custom.tacz.error.no_name";
        }
    }

    private static void checkData(GunIndexPOJO gunIndexPOJO, ClientGunIndex index) {
        ResourceLocation pojoData = gunIndexPOJO.getData();
        Preconditions.checkArgument(pojoData != null, "index object missing pojoData field");
        GunData data = CommonAssetManager.INSTANCE.getGunData(pojoData);
        Preconditions.checkArgument(data != null, "there is no corresponding data file");
        // 剩下的不需要校验了，Common的读取逻辑中已经校验过了
        index.gunData = data;
    }

    @NotNull
    private static GunDisplay checkDisplay(GunIndexPOJO gunIndexPOJO) {
        ResourceLocation pojoDisplay = gunIndexPOJO.getDisplay();
        Preconditions.checkArgument(pojoDisplay != null, "index object missing display field");
        GunDisplay display = ClientAssetManager.INSTANCE.getGunDisplay(pojoDisplay);
        Preconditions.checkArgument(display != null, "there is no corresponding display file");
        return display;
    }

    private static void checkIronZoom(GunDisplay display, ClientGunIndex index) {
        index.ironZoom = display.getIronZoom();
        if (index.ironZoom < 1) {
            index.ironZoom = 1;
        }
    }

    private static void checkTextShow(GunDisplay display, ClientGunIndex index) {
        Map<String, TextShow> textShowMap = Maps.newHashMap();
        display.getTextShows().forEach((key, value) -> {
            if (StringUtils.isNoneBlank(key)) {
                int color = ColorHex.colorTextToRbgInt(value.getColorText());
                value.setColorInt(color);
                textShowMap.put(key, value);
            }
        });
        index.gunModel.setTextShowList(textShowMap);
    }

    private static void checkTextureAndModel(GunDisplay display, ClientGunIndex index) {
        // 检查模型
        ResourceLocation modelLocation = display.getModelLocation();
        Preconditions.checkArgument(modelLocation != null, "display object missing model field");
        BedrockModelPOJO modelPOJO = ClientAssetManager.INSTANCE.getModels(modelLocation);
        Preconditions.checkArgument(modelPOJO != null, "there is no corresponding model file");
        // 检查默认材质是否存在
        ResourceLocation textureLocation = display.getModelTexture();
        Preconditions.checkArgument(textureLocation != null, "missing default texture");
        index.modelTexture = textureLocation;
        // 先判断是不是 1.10.0 版本基岩版模型文件
        if (BedrockVersion.isLegacyVersion(modelPOJO) && modelPOJO.getGeometryModelLegacy() != null) {
            index.gunModel = new BedrockGunModel(modelPOJO, BedrockVersion.LEGACY);
        }
        // 判定是不是 1.12.0 版本基岩版模型文件
        if (BedrockVersion.isNewVersion(modelPOJO) && modelPOJO.getGeometryModelNew() != null) {
            index.gunModel = new BedrockGunModel(modelPOJO, BedrockVersion.NEW);
        }
        Preconditions.checkArgument(index.gunModel != null, "there is no model data in the model file");
    }

    private static void checkLod(GunDisplay display, ClientGunIndex index) {
        GunLod gunLod = display.getGunLod();
        if (gunLod != null) {
            ResourceLocation texture = gunLod.getModelTexture();
            if (gunLod.getModelLocation() == null) {
                return;
            }
            if (texture == null) {
                return;
            }
            BedrockModelPOJO modelPOJO = ClientAssetManager.INSTANCE.getModels(gunLod.getModelLocation());
            if (modelPOJO == null) {
                return;
            }
            // 先判断是不是 1.10.0 版本基岩版模型文件
            if (BedrockVersion.isLegacyVersion(modelPOJO) && modelPOJO.getGeometryModelLegacy() != null) {
                BedrockGunModel model = new BedrockGunModel(modelPOJO, BedrockVersion.LEGACY);
                index.lodModel = Pair.of(model, texture);
            }
            // 判定是不是 1.12.0 版本基岩版模型文件
            if (BedrockVersion.isNewVersion(modelPOJO) && modelPOJO.getGeometryModelNew() != null) {
                BedrockGunModel model = new BedrockGunModel(modelPOJO, BedrockVersion.NEW);
                index.lodModel = Pair.of(model, texture);
            }
        }
    }

    private static void checkAnimation(GunDisplay display, ClientGunIndex index) {
        ResourceLocation location = display.getAnimationLocation();
        AnimationController controller;
        if (location == null) {
            controller = new AnimationController(Lists.newArrayList(), index.gunModel);
        } else {
            // 目前支持的动画为 gltf 动画。此处从缓存取出 gltf 的动画资源。
            AnimationStructure gltfAnimations = ClientAssetManager.INSTANCE.getGltfAnimations(location);
            BedrockAnimationFile bedrockAnimationFile = ClientAssetManager.INSTANCE.getBedrockAnimations(location);
            if (bedrockAnimationFile != null) {
                // 用 gltf 动画资源创建动画控制器
                controller = Animations.createControllerFromBedrock(bedrockAnimationFile, index.gunModel);
            } else if (gltfAnimations != null) {
                controller = Animations.createControllerFromGltf(gltfAnimations, index.gunModel);
            } else {
                throw new IllegalArgumentException("animation not found: " + location);
            }
            // 将默认动画填入动画控制器
            DefaultAnimation defaultAnimation = display.getDefaultAnimation();
            if (defaultAnimation != null) {
                switch (defaultAnimation) {
                    case RIFLE -> {
                        for (ObjectAnimation animation : InternalAssetLoader.getDefaultRifleAnimations()) {
                            controller.providePrototypeIfAbsent(animation.name, () -> createAnimationCopy(animation, index.gunModel));
                        }
                    }
                    case PISTOL -> {
                        for (ObjectAnimation animation : InternalAssetLoader.getDefaultPistolAnimations()) {
                            controller.providePrototypeIfAbsent(animation.name, () -> createAnimationCopy(animation, index.gunModel));
                        }
                    }
                }
            }
        }
        // 将动画控制器包装起来
        index.animationStateMachine = new GunAnimationStateMachine(controller);
        // 初始化第三人称动画
        if (StringUtils.isNoneBlank(display.getThirdPersonAnimation())) {
            index.thirdPersonAnimation = display.getThirdPersonAnimation();
        }
    }

    private static void checkSounds(GunDisplay display, ClientGunIndex index) {
        index.sounds = Maps.newHashMap();
        Map<String, ResourceLocation> soundMaps = display.getSounds();
        if (soundMaps == null || soundMaps.isEmpty()) {
            return;
        }
        // 部分音效为默认音效，不存在则需要添加默认音效
        soundMaps.putIfAbsent(SoundManager.DRY_FIRE_SOUND, new ResourceLocation(GunMod.MOD_ID, SoundManager.DRY_FIRE_SOUND));
        soundMaps.putIfAbsent(SoundManager.FIRE_SELECT, new ResourceLocation(GunMod.MOD_ID, SoundManager.FIRE_SELECT));
        soundMaps.putIfAbsent(SoundManager.HEAD_HIT_SOUND, new ResourceLocation(GunMod.MOD_ID, SoundManager.HEAD_HIT_SOUND));
        soundMaps.putIfAbsent(SoundManager.FLESH_HIT_SOUND, new ResourceLocation(GunMod.MOD_ID, SoundManager.FLESH_HIT_SOUND));
        soundMaps.putIfAbsent(SoundManager.KILL_SOUND, new ResourceLocation(GunMod.MOD_ID, SoundManager.KILL_SOUND));
        index.sounds.putAll(soundMaps);
    }

    private static void checkTransform(GunDisplay display, ClientGunIndex index) {
        GunTransform readTransform = display.getTransform();
        GunDisplay defaultDisplay = ClientAssetManager.INSTANCE.getGunDisplay(DefaultAssets.DEFAULT_GUN_DISPLAY);
        if (readTransform == null || readTransform.getScale() == null) {
            index.transform = Objects.requireNonNull(defaultDisplay.getTransform());
        } else {
            index.transform = display.getTransform();
        }
    }

    private static void checkSlotTexture(GunDisplay display, ClientGunIndex index) {
        // 加载 GUI 内枪械图标
        index.slotTexture = Objects.requireNonNullElseGet(display.getSlotTextureLocation(), MissingTextureAtlasSprite::getLocation);
    }

    private static void checkHUDTexture(GunDisplay display, ClientGunIndex index) {
        index.hudTexture = Objects.requireNonNullElseGet(display.getHudTextureLocation(), MissingTextureAtlasSprite::getLocation);
        index.hudEmptyTexture = display.getHudEmptyTextureLocation();
    }

    private static void checkShellEjection(GunDisplay display, ClientGunIndex index) {
        index.shellEjection = display.getShellEjection();
    }

    private static void checkMuzzleFlash(GunDisplay display, ClientGunIndex index) {
        index.muzzleFlash = display.getMuzzleFlash();
        if (index.muzzleFlash != null && index.muzzleFlash.getTexture() == null) {
            index.muzzleFlash = null;
        }
    }

    private static void checkLayerGunShow(GunDisplay display, ClientGunIndex index) {
        index.offhandShow = display.getOffhandShow();
        if (index.offhandShow == null) {
            index.offhandShow = new LayerGunShow();
        }
        Map<String, LayerGunShow> show = display.getHotbarShow();
        if (show == null || show.isEmpty()) {
            return;
        }
        index.hotbarShow = new Int2ObjectArrayMap<>();
        for (String key : show.keySet()) {
            try {
                index.hotbarShow.put(Integer.parseInt(key), show.get(key));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("index number is error: " + key);
            }
        }
    }

    private static ObjectAnimation createAnimationCopy(ObjectAnimation prototype, BedrockModel model) {
        ObjectAnimation animation = new ObjectAnimation(prototype);
        for (Map.Entry<String, List<ObjectAnimationChannel>> entry : animation.getChannels().entrySet()) {
            BedrockPart node = model.getNode(entry.getKey());
            float offsetX = 0, offsetY = 0, offsetZ = 0;
            float rotationX = 0, rotationY = 0, rotationZ = 0;
            if (node != null) {
                if (node.getParent() != null) {
                    // 因为模型是上下颠倒的，因此x轴和y轴的偏移也进行取反
                    offsetX = -node.x / 16f;
                    offsetY = -node.y / 16f;
                    offsetZ = node.z / 16f;
                } else {
                    // 节点为根时，x轴和y轴取反，并且y轴坐标需要加上 24 。
                    offsetX = -node.x / 16f;
                    offsetY = (24 - node.y) / 16f;
                    offsetZ = node.z / 16f;
                }
                rotationX = node.xRot;
                rotationY = node.yRot;
                rotationZ = node.zRot;
            }
            for (ObjectAnimationChannel channel : entry.getValue()) {
                if (channel.type == ObjectAnimationChannel.ChannelType.TRANSLATION) {
                    channel.content = new AnimationChannelContent(channel.content);
                    for (int i = 0; i < channel.content.values.length; i++) {
                        float[] value = channel.content.values[i];
                        value[0] += offsetX;
                        value[1] += offsetY;
                        value[2] += offsetZ;
                    }
                    channel.interpolator = channel.interpolator.clone();
                    channel.interpolator.compile(channel.content);
                    continue;
                }
                if (channel.type == ObjectAnimationChannel.ChannelType.ROTATION) {
                    channel.content = new AnimationChannelContent(channel.content);
                    for (int i = 0; i < channel.content.values.length; i++) {
                        float[] value = channel.content.values[i];
                        float[] angles = MathUtil.toEulerAngles(value);
                        angles[0] += rotationX;
                        angles[1] += rotationY;
                        angles[2] += rotationZ;
                        channel.content.values[i] = MathUtil.toQuaternion(angles[0], angles[1], angles[2]);
                    }
                    channel.interpolator = channel.interpolator.clone();
                    channel.interpolator.compile(channel.content);
                }
            }
        }
        return animation;
    }

    public String getType() {
        return type;
    }

    public String getItemType() {
        return itemType;
    }

    public int getSort() {
        return sort;
    }

    public String getName() {
        return name;
    }

    public BedrockGunModel getGunModel() {
        return gunModel;
    }

    @Nullable
    public Pair<BedrockGunModel, ResourceLocation> getLodModel() {
        return lodModel;
    }

    public GunAnimationStateMachine getAnimationStateMachine() {
        return animationStateMachine;
    }

    public ResourceLocation getSounds(String name) {
        return sounds.get(name);
    }

    public GunTransform getTransform() {
        return transform;
    }

    public ResourceLocation getSlotTexture() {
        return slotTexture;
    }

    public ResourceLocation getHUDTexture() {
        return hudTexture;
    }

    @Nullable
    public ResourceLocation getHudEmptyTexture() {
        return hudEmptyTexture;
    }

    public ResourceLocation getModelTexture() {
        return modelTexture;
    }

    public GunData getGunData() {
        return gunData;
    }

    public String getThirdPersonAnimation() {
        return thirdPersonAnimation;
    }

    @Nullable
    public ShellEjection getShellEjection() {
        return shellEjection;
    }

    @Nullable
    public MuzzleFlash getMuzzleFlash() {
        return muzzleFlash;
    }

    public LayerGunShow getOffhandShow() {
        return offhandShow;
    }

    @Nullable
    public Int2ObjectArrayMap<LayerGunShow> getHotbarShow() {
        return hotbarShow;
    }

    public float getIronZoom() {
        return ironZoom;
    }
}