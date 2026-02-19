package com.sypztep.plateau.mixin;

import com.sypztep.plateau.common.api.entity.AttributeTarget;
import com.sypztep.plateau.common.api.entity.PlateauAttributeRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Inject(method = "createLivingAttributes", at = @At("HEAD"))
    private static void bootstrapAttributes(CallbackInfoReturnable<AttributeSupplier.Builder> cir) {
        PlateauAttributeRegistry.bootstrap();
    }
    @Inject(method = "createLivingAttributes", at = @At("RETURN"))
    private static void registryExtraStats(CallbackInfoReturnable<AttributeSupplier.Builder> cir) {
        PlateauAttributeRegistry.inject(cir.getReturnValue(), AttributeTarget.LIVING);
    }
}
