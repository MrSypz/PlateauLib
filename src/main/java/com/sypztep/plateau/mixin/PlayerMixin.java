package com.sypztep.plateau.mixin;

import com.sypztep.plateau.common.api.entity.AttributeTarget;
import com.sypztep.plateau.common.api.entity.PlateauAttributeRegistry;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class PlayerMixin {
    @Inject(method = "createAttributes", at = @At("RETURN"))
    private static void registryExtraStats(CallbackInfoReturnable<AttributeSupplier.Builder> cir) {
        PlateauAttributeRegistry.inject(cir.getReturnValue(), AttributeTarget.PLAYER);
    }
}
