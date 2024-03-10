package com.tac.guns.entity;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

public class EntityBullet extends ThrowableProjectile {
    public static final EntityType<EntityBullet> TYPE = EntityType.Builder.<EntityBullet>of(EntityBullet::new, MobCategory.MISC)
            .sized(0.0625F, 0.0625F).clientTrackingRange(6).updateInterval(10).build("bullet");

    public EntityBullet(EntityType<? extends ThrowableProjectile> type, Level worldIn) {
        super(type, worldIn);
    }

    public EntityBullet(Level worldIn, LivingEntity throwerIn) {
        super(TYPE, throwerIn, worldIn);
    }

    public EntityBullet(Level worldIn, double x, double y, double z) {
        super(TYPE, x, y, z, worldIn);
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount > 100) {
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (result.getEntity() instanceof LivingEntity livingEntity) {
            livingEntity.hurt(DamageSource.thrown(this, this.getOwner()), 5);
        }
        super.onHitEntity(result);
    }

    @Override
    protected float getGravity() {
        return 0;
    }
}
