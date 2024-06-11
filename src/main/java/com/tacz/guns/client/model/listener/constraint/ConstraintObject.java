package com.tacz.guns.client.model.listener.constraint;

import com.mojang.math.Vector3f;
import com.tacz.guns.client.animation.AnimationListener;
import com.tacz.guns.client.animation.AnimationListenerSupplier;
import com.tacz.guns.client.animation.ObjectAnimationChannel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.client.resource.pojo.model.BonesItem;

import javax.annotation.Nullable;

import static com.tacz.guns.client.model.BedrockAnimatedModel.CONSTRAINT_NODE;

public class ConstraintObject implements AnimationListenerSupplier {
    public Vector3f translationConstraint = new Vector3f(0, 0, 0);
    public Vector3f rotationConstraint = new Vector3f(0, 0, 0);
    /**
     * 当相机的节点为根时，node为空
     */
    public BedrockPart node;
    /**
     * 当相机的节点不为根时，bonesItem为空
     */
    public BonesItem bonesItem;

    @Nullable
    @Override
    public AnimationListener supplyListeners(String nodeName, ObjectAnimationChannel.ChannelType type) {
        if (!nodeName.equals(CONSTRAINT_NODE)) {
            return null;
        }
        if (type.equals(ObjectAnimationChannel.ChannelType.ROTATION)) {
            return new ConstraintRotateListener(this);
        }
        if (type.equals(ObjectAnimationChannel.ChannelType.TRANSLATION)) {
            return new ConstraintTranslateListener(this);
        }
        return null;
    }
}
