package com.sypztep.plateau.mixin.effect;

import com.sypztep.plateau.common.EffectRemoval;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Inject(method = "onEffectUpdated", at = @At("HEAD"))
    private void oEffectUpgraded(MobEffectInstance effect, boolean reapplyEffect, @Nullable Entity source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (reapplyEffect && !self.level().isClientSide()) {
            MobEffect statusEffect = effect.getEffect().value();
            if (statusEffect instanceof EffectRemoval effectRemoval) effectRemoval.onRemoved(self);
        }
    }

    @Inject(method = "onEffectsRemoved", at = @At("HEAD"))
    private void onEffectRemoved(Collection<MobEffectInstance> collection, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (!self.level().isClientSide()) {

            for (MobEffectInstance mobEffectInstance : collection) {
                MobEffect statusEffect = mobEffectInstance.getEffect().value();
                if (statusEffect instanceof EffectRemoval effectRemoval) effectRemoval.onRemoved(self);
            }
        }
    }
}