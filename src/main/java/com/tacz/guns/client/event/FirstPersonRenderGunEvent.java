package com.tacz.guns.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.event.RenderItemInHandBobEvent;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.client.other.KeepingItemRenderer;
import com.tacz.guns.api.event.common.GunFireEvent;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.client.animation.internal.GunAnimationStateMachine;
import com.tacz.guns.client.gui.GunRefitScreen;
import com.tacz.guns.client.model.BedrockAttachmentModel;
import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.model.bedrock.BedrockModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.client.model.functional.MuzzleFlashRender;
import com.tacz.guns.client.model.functional.ShellRender;
import com.tacz.guns.client.renderer.item.GunItemRenderer;
import com.tacz.guns.client.resource.InternalAssetLoader;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import com.tacz.guns.client.resource.index.ClientGunIndex;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.util.math.Easing;
import com.tacz.guns.util.math.MathUtil;
import com.tacz.guns.util.math.PerlinNoise;
import com.tacz.guns.util.math.SecondOrderDynamics;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.ItemTransforms.TransformType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static net.minecraft.client.renderer.block.model.ItemTransforms.TransformType.FIRST_PERSON_RIGHT_HAND;

/**
 * 负责第一人称的枪械模型渲染。其他人称参见 {@link GunItemRenderer}
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = GunMod.MOD_ID)
public class FirstPersonRenderGunEvent {
    // 用于生成瞄准动作的运动曲线，使动作看起来更平滑
    private static final SecondOrderDynamics AIMING_DYNAMICS = new SecondOrderDynamics(1.2f, 1.2f, 0.5f, 0);
    // 用于打开改装界面时枪械运动的平滑
    private static final SecondOrderDynamics REFIT_OPENING_DYNAMICS = new SecondOrderDynamics(1f, 1.2f, 0.5f, 0);
    // 用于跳跃延滞动画的平滑
    private static final SecondOrderDynamics JUMPING_DYNAMICS = new SecondOrderDynamics(0.28f, 1f, 0.65f, 0);
    private static final float JUMPING_Y_SWAY = -2f;
    private static final float JUMPING_SWAY_TIME = 0.3f;
    private static final float LANDING_SWAY_TIME = 0.15f;
    // 用于枪械后座的程序动画
    private static final PerlinNoise SHOOT_X_SWAY_NOISE = new PerlinNoise(-0.2f, 0.2f, 400);
    private static final PerlinNoise SHOOT_Y_ROTATION_NOISE = new PerlinNoise(-0.0136f, 0.0136f, 100);
    private static final float SHOOT_Y_SWAY = -0.1f;
    private static final float SHOOT_ANIMATION_TIME = 0.3f;

    private static float jumpingSwayProgress = 0;
    private static boolean lastOnGround = false;
    private static long jumpingTimeStamp = -1;
    private static long shootTimeStamp = -1;

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (event.getHand() == InteractionHand.OFF_HAND) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof IGun iGun)) {
            return;
        }

        // 获取 TransformType
        TransformType transformType;
        if (event.getHand() == InteractionHand.MAIN_HAND) {
            transformType = FIRST_PERSON_RIGHT_HAND;
        } else {
            transformType = TransformType.FIRST_PERSON_LEFT_HAND;
        }

        ResourceLocation gunId = iGun.getGunId(stack);
        TimelessAPI.getClientGunIndex(gunId).ifPresentOrElse(gunIndex -> {
            BedrockGunModel gunModel = gunIndex.getGunModel();
            GunAnimationStateMachine animationStateMachine = gunIndex.getAnimationStateMachine();
            if (gunModel == null) {
                return;
            }
            // 在渲染之前，先更新动画，让动画数据写入模型
            if (animationStateMachine != null) {
                animationStateMachine.update(event.getPartialTicks(), player);
            }

            PoseStack poseStack = event.getPoseStack();
            poseStack.pushPose();
            // 逆转原版施加在手上的延滞效果，改为写入模型动画数据中
            float xRotOffset = Mth.lerp(event.getPartialTicks(), player.xBobO, player.xBob);
            float yRotOffset = Mth.lerp(event.getPartialTicks(), player.yBobO, player.yBob);
            float xRot = player.getViewXRot(event.getPartialTicks()) - xRotOffset;
            float yRot = player.getViewYRot(event.getPartialTicks()) - yRotOffset;
            poseStack.mulPose(Vector3f.XP.rotationDegrees(xRot * -0.1F));
            poseStack.mulPose(Vector3f.YP.rotationDegrees(yRot * -0.1F));
            BedrockPart rootNode = gunModel.getRootNode();
            if (rootNode != null) {
                xRot = (float) Math.tanh(xRot / 25) * 25;
                yRot = (float) Math.tanh(yRot / 25) * 25;
                rootNode.offsetX += yRot * 0.1F / 16F / 3F;
                rootNode.offsetY += -xRot * 0.1F / 16F / 3F;
                rootNode.additionalQuaternion.mul(Vector3f.XP.rotationDegrees(xRot * 0.05F));
                rootNode.additionalQuaternion.mul(Vector3f.YP.rotationDegrees(yRot * 0.05F));
            }
            // 从渲染原点 (0, 24, 0) 移动到模型原点 (0, 0, 0)
            poseStack.translate(0, 1.5f, 0);
            // 基岩版模型是上下颠倒的，需要翻转过来。
            poseStack.mulPose(Vector3f.ZP.rotationDegrees(180f));
            // 应用持枪姿态变换，如第一人称摄像机定位
            applyFirstPersonGunTransform(player, stack, gunIndex, poseStack, gunModel, event.getPartialTicks());

            // 开启第一人称弹壳和火焰渲染
            MuzzleFlashRender.isSelf = true;
            ShellRender.isSelf = true;
            {
                // 如果正在打开改装界面，则取消手臂渲染
                boolean renderHand = gunModel.getRenderHand();
                if (GunRefitScreen.getOpeningProgress() != 0) {
                    gunModel.setRenderHand(false);
                }
                // 调用枪械模型渲染
                RenderType renderType = RenderType.itemEntityTranslucentCull(gunIndex.getModelTexture());
                gunModel.render(poseStack, stack, transformType, renderType, event.getPackedLight(), OverlayTexture.NO_OVERLAY);
                // 调用曳光弹渲染
                renderBulletTracer(player, poseStack, gunModel, event.getPartialTicks());
                // 恢复手臂渲染
                gunModel.setRenderHand(renderHand);
                // 渲染完成后，将动画数据从模型中清除，不对其他视角下的模型渲染产生影响
                poseStack.popPose();
                gunModel.cleanAnimationTransform();
            }
            // 关闭第一人称弹壳和火焰渲染
            MuzzleFlashRender.isSelf = false;
            ShellRender.isSelf = false;

            // 放这里，只有渲染了枪械，才取消后续（虽然一般来说也没有什么后续了）
            event.setCanceled(true);
        }, () -> renderBulletTracer(player, event.getPoseStack(), null, event.getPartialTicks()));
    }

    private static void renderBulletTracer(LocalPlayer player, PoseStack poseStack, BedrockGunModel gunModel, float partialTicks) {
        if (!RenderConfig.FIRST_PERSON_BULLET_TRACER_ENABLE.get()) {
            return;
        }
        Optional<BedrockModel> modelOptional = InternalAssetLoader.getBedrockModel(InternalAssetLoader.DEFAULT_BULLET_MODEL);
        if (modelOptional.isEmpty()) {
            return;
        }
        BedrockModel model = modelOptional.get();
        Level level = player.getLevel();
        AABB renderArea = player.getBoundingBox().inflate(256, 256, 256);
        for (Entity entity : level.getEntities(player, renderArea, FirstPersonRenderGunEvent::bulletFromPlayer)) {
            EntityKineticBullet entityBullet = (EntityKineticBullet) entity;
            if (!entityBullet.isTracerAmmo()) {
                continue;
            }
            Vec3 deltaMovement = entityBullet.getDeltaMovement().multiply(partialTicks, partialTicks, partialTicks);
            Vec3 entityPosition = entityBullet.getPosition(0).add(deltaMovement);
            Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
            Vec3 cameraPosition = camera.getPosition();
            Vec3 originCameraPosition = entityBullet.getOriginCameraPosition();
            if (originCameraPosition == null) {
                if (gunModel == null) {
                    continue;
                }
                if (gunModel.getMuzzleFlashPosPath() != null) {
                    poseStack.pushPose();
                    for (BedrockPart bedrockPart : gunModel.getMuzzleFlashPosPath()) {
                        bedrockPart.translateAndRotateAndScale(poseStack);
                    }
                    Matrix4f pose = poseStack.last().pose();
                    originCameraPosition = new Vec3(cameraPosition.x, cameraPosition.y, cameraPosition.z);
                    entityBullet.setOriginCameraPosition(originCameraPosition);
                    entityBullet.setOriginRenderOffset(new Vec3(pose.m03, pose.m13, pose.m23));
                    poseStack.popPose();
                } else {
                    continue;
                }
            }
            Vec3 originRenderOffset = entityBullet.getOriginRenderOffset();
            Vec3 alphaCameraTranslation = originCameraPosition.subtract(cameraPosition);
            double distance = entityPosition.distanceTo(originCameraPosition);
            Vec3 bulletDirection = entityPosition.subtract(originCameraPosition);
            double yRot = MathUtil.getTwoVecAngle(new Vec3(0, 0, -1), new Vec3(bulletDirection.x, 0, bulletDirection.z));
            double xRot = MathUtil.getTwoVecAngle(new Vec3(bulletDirection.x, 0, bulletDirection.z), bulletDirection);
            if (yRot == -1) {
                yRot = Math.toRadians(camera.getYRot() + 180f);
            }
            if (xRot == -1) {
                xRot = Math.toRadians(camera.getXRot());
            }
            xRot *= bulletDirection.y > 0 ? -1 : 1;
            yRot *= bulletDirection.x > 0 ? 1 : -1;
            PoseStack poseStack1 = new PoseStack();
            // 逆转摄像机的旋转，回到起始坐标
            poseStack1.mulPose(Vector3f.XP.rotationDegrees(camera.getXRot()));
            poseStack1.mulPose(Vector3f.YP.rotationDegrees(camera.getYRot() + 180f));
            poseStack1.translate(alphaCameraTranslation.x, alphaCameraTranslation.y, alphaCameraTranslation.z);
            // 恢复旋转角度，应用枪口定位偏移
            poseStack1.mulPose(Vector3f.YN.rotation((float) yRot));
            poseStack1.mulPose(Vector3f.XN.rotation((float) xRot));
            poseStack1.translate(originRenderOffset.x, originRenderOffset.y, originRenderOffset.z - distance);
            float trailLength = 0.5f * (float) entityBullet.getDeltaMovement().length();
            poseStack1.translate(0, 0, -trailLength / 2);
            poseStack1.scale(0.03f, 0.03f, trailLength);
            ResourceLocation ammoId = entityBullet.getAmmoId();
            TimelessAPI.getClientAmmoIndex(ammoId).ifPresent(index -> {
                float[] tracerColor = index.getTracerColor();
                RenderType type = RenderType.energySwirl(InternalAssetLoader.DEFAULT_BULLET_TEXTURE, 15, 15);
                model.render(poseStack1, ItemTransforms.TransformType.NONE, type, LightTexture.pack(15, 15),
                        OverlayTexture.NO_OVERLAY, tracerColor[0], tracerColor[1], tracerColor[2], 1);
            });
        }
    }

    /**
     * 当主手拿着枪械物品的时候，取消应用在它上面的 viewBobbing，以便应用自定义的跑步/走路动画。
     */
    @SubscribeEvent
    public static void cancelItemInHandViewBobbing(RenderItemInHandBobEvent.BobView event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        ItemStack itemStack = ((KeepingItemRenderer) Minecraft.getInstance().getItemInHandRenderer()).getCurrentItem();
        if (IGun.getIGunOrNull(itemStack) != null) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onGunFire(GunFireEvent event) {
        if (event.getLogicalSide().isClient()) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }
            ItemStack mainhandItem = player.getMainHandItem();
            IGun iGun = IGun.getIGunOrNull(mainhandItem);
            if (iGun == null) {
                return;
            }
            TimelessAPI.getClientGunIndex(iGun.getGunId(mainhandItem)).ifPresent(gunIndex -> {
                // 记录开火时间戳，用于后坐力程序动画
                shootTimeStamp = System.currentTimeMillis();
                // 记录枪口火焰数据
                MuzzleFlashRender.onShoot();
                // 抛壳
                if (gunIndex.getShellEjection() != null) {
                    ShellRender.addShell(gunIndex.getShellEjection().getRandomVelocity());
                }
            });
        }
    }

    private static boolean bulletFromPlayer(Entity entity) {
        if (entity instanceof EntityKineticBullet entityBullet) {
            return entityBullet.getOwner() instanceof LocalPlayer;
        }
        return false;
    }

    private static void applyFirstPersonGunTransform(LocalPlayer player, ItemStack gunItemStack, ClientGunIndex gunIndex, PoseStack poseStack, BedrockGunModel model, float partialTicks) {
        // 配合运动曲线，计算改装枪口的打开进度
        float refitScreenOpeningProgress = REFIT_OPENING_DYNAMICS.update(GunRefitScreen.getOpeningProgress());
        // 配合运动曲线，计算瞄准进度
        float aimingProgress = AIMING_DYNAMICS.update(IClientPlayerGunOperator.fromLocalPlayer(player).getClientAimingProgress(partialTicks));
        // 应用枪械动态，如后坐力、持枪跳跃等
        applyGunMovements(model, aimingProgress, partialTicks);
        // 应用各种摄像机定位组的变换（默认持枪、瞄准、改装界面等）
        applyFirstPersonPositioningTransform(poseStack, model, gunItemStack, aimingProgress, refitScreenOpeningProgress);
        // 应用动画约束变换
        applyAnimationConstraintTransform(poseStack, model, aimingProgress * (1 - refitScreenOpeningProgress));
    }

    private static void applyGunMovements(BedrockGunModel model, float aimingProgress, float partialTicks) {
        applyShootSwayAndRotation(model, aimingProgress);
        applyJumpingSway(model, partialTicks);
    }

    /**
     * 应用瞄具摄像机定位组、机瞄摄像机定位组和 Idle 摄像机定位组的变换。会在几个摄像机定位之间插值。
     */
    private static void applyFirstPersonPositioningTransform(PoseStack poseStack, BedrockGunModel model, ItemStack stack, float aimingProgress, float refitScreenOpeningProgress) {
        IGun iGun = IGun.getIGunOrNull(stack);
        if (iGun == null) {
            return;
        }
        Matrix4f transformMatrix = new Matrix4f();
        transformMatrix.setIdentity();
        // 应用瞄准定位
        List<BedrockPart> idleNodePath = model.getIdleSightPath();
        List<BedrockPart> aimingNodePath = null;
        ItemStack scopeItem = iGun.getAttachment(stack, AttachmentType.SCOPE);
        if (scopeItem.isEmpty()) {
            // 未安装瞄具，使用机瞄定位组
            aimingNodePath = model.getIronSightPath();
        } else {
            // 安装瞄具，组合瞄具定位组和瞄具视野定位组
            List<BedrockPart> scopeNodePath = model.getScopePosPath();
            if (scopeNodePath != null) {
                aimingNodePath = new ArrayList<>(scopeNodePath);
                IAttachment iAttachment = IAttachment.getIAttachmentOrNull(scopeItem);
                if (iAttachment != null) {
                    ResourceLocation scopeId = iAttachment.getAttachmentId(scopeItem);
                    Optional<ClientAttachmentIndex> indexOptional = TimelessAPI.getClientAttachmentIndex(scopeId);
                    if (indexOptional.isPresent()) {
                        BedrockAttachmentModel attachmentModel = indexOptional.get().getAttachmentModel();
                        if (attachmentModel.getScopeViewPath() != null) {
                            aimingNodePath.addAll(attachmentModel.getScopeViewPath());
                        }
                    }
                }
            }
        }
        MathUtil.applyMatrixLerp(transformMatrix, getPositioningNodeInverse(idleNodePath), transformMatrix, (1 - refitScreenOpeningProgress));
        MathUtil.applyMatrixLerp(transformMatrix, getPositioningNodeInverse(aimingNodePath), transformMatrix, (1 - refitScreenOpeningProgress) * aimingProgress);
        // 应用改装界面开启时的定位
        float refitTransformProgress = (float) Easing.easeOutCubic(GunRefitScreen.getTransformProgress());
        AttachmentType oldType = GunRefitScreen.getOldTransformType();
        AttachmentType currentType = GunRefitScreen.getCurrentTransformType();
        List<BedrockPart> fromNode = model.getRefitAttachmentViewPath(oldType);
        List<BedrockPart> toNode = model.getRefitAttachmentViewPath(currentType);
        MathUtil.applyMatrixLerp(transformMatrix, getPositioningNodeInverse(fromNode), transformMatrix, refitScreenOpeningProgress);
        MathUtil.applyMatrixLerp(transformMatrix, getPositioningNodeInverse(toNode), transformMatrix, refitScreenOpeningProgress * refitTransformProgress);
        // 应用变换到 PoseStack
        poseStack.translate(0, 1.5f, 0);
        poseStack.mulPoseMatrix(transformMatrix);
        poseStack.translate(0, -1.5f, 0);
    }

    /**
     * 获取摄像机定位组的反相矩阵
     */
    @Nonnull
    private static Matrix4f getPositioningNodeInverse(List<BedrockPart> nodePath) {
        Matrix4f matrix4f = new Matrix4f();
        matrix4f.setIdentity();
        if (nodePath != null) {
            for (int i = nodePath.size() - 1; i >= 0; i--) {
                BedrockPart part = nodePath.get(i);
                // 计算反向的旋转
                matrix4f.multiply(Vector3f.XN.rotation(part.xRot));
                matrix4f.multiply(Vector3f.YN.rotation(part.yRot));
                matrix4f.multiply(Vector3f.ZN.rotation(part.zRot));
                // 计算反向的位移
                if (part.getParent() != null) {
                    matrix4f.multiplyWithTranslation(-part.x / 16.0F, -part.y / 16.0F, -part.z / 16.0F);
                } else {
                    matrix4f.multiplyWithTranslation(-part.x / 16.0F, (1.5F - part.y / 16.0F), -part.z / 16.0F);
                }
            }
        }
        return matrix4f;
    }

    private static void applyShootSwayAndRotation(BedrockGunModel model, float aimingProgress) {
        BedrockPart rootNode = model.getRootNode();
        if (rootNode != null) {
            float progress = 1 - (System.currentTimeMillis() - shootTimeStamp) / (SHOOT_ANIMATION_TIME * 1000);
            if (progress < 0) {
                progress = 0;
            }
            progress = (float) Easing.easeOutCubic(progress);
            rootNode.offsetX += SHOOT_X_SWAY_NOISE.getValue() / 16 * progress * (1 - aimingProgress);
            // 基岩版模型 y 轴上下颠倒，sway 值取相反数
            rootNode.offsetY += -SHOOT_Y_SWAY / 16 * progress * (1 - aimingProgress);
            rootNode.additionalQuaternion.mul(Vector3f.YP.rotation(SHOOT_Y_ROTATION_NOISE.getValue() * progress));
        }
    }

    private static void applyJumpingSway(BedrockGunModel model, float partialTicks) {
        if (jumpingTimeStamp == -1) {
            jumpingTimeStamp = System.currentTimeMillis();
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            double posY = Mth.lerp(partialTicks, Minecraft.getInstance().player.yOld, Minecraft.getInstance().player.getY());
            float velocityY = (float) (posY - Minecraft.getInstance().player.yOld) / partialTicks;
            if (player.isOnGround()) {
                if (!lastOnGround) {
                    jumpingSwayProgress = velocityY / -0.1f;
                    if (jumpingSwayProgress > 1) {
                        jumpingSwayProgress = 1;
                    }
                    lastOnGround = true;
                } else {
                    jumpingSwayProgress -= (System.currentTimeMillis() - jumpingTimeStamp) / (LANDING_SWAY_TIME * 1000);
                    if (jumpingSwayProgress < 0) {
                        jumpingSwayProgress = 0;
                    }
                }
            } else {
                if (lastOnGround) {
                    // 0.42 是玩家自然起跳的速度
                    jumpingSwayProgress = velocityY / 0.42f;
                    if (jumpingSwayProgress > 1) {
                        jumpingSwayProgress = 1;
                    }
                    lastOnGround = false;
                } else {
                    jumpingSwayProgress -= (System.currentTimeMillis() - jumpingTimeStamp) / (JUMPING_SWAY_TIME * 1000);
                    if (jumpingSwayProgress < 0) {
                        jumpingSwayProgress = 0;
                    }
                }
            }
        }
        jumpingTimeStamp = System.currentTimeMillis();
        float ySway = JUMPING_DYNAMICS.update(JUMPING_Y_SWAY * jumpingSwayProgress);
        BedrockPart rootNode = model.getRootNode();
        if (rootNode != null) {
            // 基岩版模型 y 轴上下颠倒，sway 值取相反数
            rootNode.offsetY += -ySway / 16;
        }
    }

    /**
     * 获取动画约束点的变换数据。
     *
     * @param originTranslation   用于输出约束点的原坐标
     * @param animatedTranslation 用于输出约束点经过动画变换之后的坐标
     * @param rotation            用于输出约束点的旋转
     */
    private static void getAnimationConstraintTransform(List<BedrockPart> nodePath, @Nonnull Vector3f originTranslation, @Nonnull Vector3f animatedTranslation, @Nonnull Vector3f rotation) {
        if (nodePath == null) {
            return;
        }
        // 约束点动画变换矩阵
        Matrix4f animeMatrix = new Matrix4f();
        // 约束点初始变换矩阵
        Matrix4f originMatrix = new Matrix4f();
        animeMatrix.setIdentity();
        originMatrix.setIdentity();
        BedrockPart constrainNode = nodePath.get(nodePath.size() - 1);
        for (BedrockPart part : nodePath) {
            // 乘动画位移
            if (part != constrainNode) {
                animeMatrix.multiplyWithTranslation(part.offsetX, part.offsetY, part.offsetZ);
            }
            // 乘组位移
            if (part.getParent() != null) {
                animeMatrix.multiplyWithTranslation(part.x / 16.0F, part.y / 16.0F, part.z / 16.0F);
            } else {
                animeMatrix.multiplyWithTranslation(part.x / 16.0F, (part.y / 16.0F - 1.5F), part.z / 16.0F);
            }
            // 乘动画旋转
            if (part != constrainNode) {
                animeMatrix.multiply(part.additionalQuaternion);
            }
            // 乘组旋转
            animeMatrix.multiply(Vector3f.ZP.rotation(part.zRot));
            animeMatrix.multiply(Vector3f.YP.rotation(part.yRot));
            animeMatrix.multiply(Vector3f.XP.rotation(part.xRot));

            // 乘组位移
            if (part.getParent() != null) {
                originMatrix.multiplyWithTranslation(part.x / 16.0F, part.y / 16.0F, part.z / 16.0F);
            } else {
                originMatrix.multiplyWithTranslation(part.x / 16.0F, (part.y / 16.0F - 1.5F), part.z / 16.0F);
            }
            // 乘组旋转
            originMatrix.multiply(Vector3f.ZP.rotation(part.zRot));
            originMatrix.multiply(Vector3f.YP.rotation(part.yRot));
            originMatrix.multiply(Vector3f.XP.rotation(part.xRot));

        }
        // 把变换数据写入输出
        animatedTranslation.set(animeMatrix.m03, animeMatrix.m13, animeMatrix.m23);
        originTranslation.set(originMatrix.m03, originMatrix.m13, originMatrix.m23);
        Vector3f animatedRotation = MathUtil.getEulerAngles(animeMatrix);
        Vector3f originRotation = MathUtil.getEulerAngles(originMatrix);
        animatedRotation.sub(originRotation);
        rotation.set(animatedRotation.x(), animatedRotation.y(), animatedRotation.z());
    }

    /**
     * 应用动画约束变换。
     *
     * @param weight 控制约束变换的权重，用于插值。
     */
    public static void applyAnimationConstraintTransform(PoseStack poseStack, BedrockGunModel gunModel, float weight) {
        List<BedrockPart> nodePath = gunModel.getConstraintPath();
        if (nodePath == null) {
            return;
        }
        if (gunModel.getConstraintObject() == null) {
            return;
        }
        // 获取动画约束点的变换信息
        Vector3f originTranslation = new Vector3f();
        Vector3f animatedTranslation = new Vector3f();
        Vector3f rotation = new Vector3f();
        Vector3f translationICA = gunModel.getConstraintObject().translationConstraint;
        Vector3f rotationICA = gunModel.getConstraintObject().rotationConstraint;
        getAnimationConstraintTransform(nodePath, originTranslation, animatedTranslation, rotation);
        // 配合约束系数，计算约束位移需要的反向位移
        Vector3f inverseTranslation = originTranslation.copy();
        inverseTranslation.sub(animatedTranslation);
        inverseTranslation.mul(1 - translationICA.x(), 1 - translationICA.y(), 1 - translationICA.z());
        // 计算约束旋转需要的反向旋转。因需要插值，获取的是欧拉角
        Vector3f inverseRotation = rotation.copy();
        inverseRotation.mul(rotationICA.x() - 1, rotationICA.y() - 1, rotationICA.z() - 1);
        // 约束旋转
        poseStack.translate(animatedTranslation.x(), animatedTranslation.y() + 1.5f, animatedTranslation.z());
        poseStack.mulPose(Vector3f.XP.rotation(inverseRotation.x() * weight));
        poseStack.mulPose(Vector3f.YP.rotation(inverseRotation.y() * weight));
        poseStack.mulPose(Vector3f.ZP.rotation(inverseRotation.z() * weight));
        poseStack.translate(-animatedTranslation.x(), -animatedTranslation.y() - 1.5f, -animatedTranslation.z());
        // 约束位移
        poseStack.last().pose().translate(new Vector3f(-inverseTranslation.x() * weight, -inverseTranslation.y() * weight, inverseTranslation.z() * weight));
    }
}
