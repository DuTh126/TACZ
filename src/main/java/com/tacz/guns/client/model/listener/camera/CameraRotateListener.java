package com.tacz.guns.client.model.listener.camera;

import com.mojang.math.Quaternion;
import com.tacz.guns.client.animation.AnimationListener;
import com.tacz.guns.client.animation.ObjectAnimationChannel;
import com.tacz.guns.util.math.MathUtil;

public class CameraRotateListener implements AnimationListener {
    private final CameraAnimationObject camera;

    public CameraRotateListener(CameraAnimationObject camera) {
        this.camera = camera;
    }

    @Override
    public void update(float[] values, boolean blend) {
        float[] angles = MathUtil.toEulerAngles(values);
        float xRot = angles[0];
        float yRot = angles[1];
        float zRot = angles[2];
        // 在关键帧中储存的旋转数值并不是摄像头的旋转数值，是世界箱体的旋转数值
        // 最终需要存入rotationQuaternion的是摄像机的旋转（即世界箱体旋转的反相）
        if (blend) {
            float[] q = MathUtil.toQuaternion(-xRot, -yRot, -zRot);
            Quaternion quaternion = MathUtil.toQuaternion(q);
            MathUtil.blendQuaternion(camera.rotationQuaternion, quaternion);
        } else {
            MathUtil.toQuaternion(-xRot, -yRot, -zRot, camera.rotationQuaternion);
        }
    }

    @Override
    public float[] recover() {
        return MathUtil.toQuaternion(-camera.cameraRenderer.getRotateAngleX(), camera.cameraRenderer.getRotateAngleY(), camera.cameraRenderer.getRotateAngleZ());
    }

    @Override
    public ObjectAnimationChannel.ChannelType getType() {
        return ObjectAnimationChannel.ChannelType.ROTATION;
    }
}
