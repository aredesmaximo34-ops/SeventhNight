package com.seventhnight.mixin;

import com.seventhnight.client.ClientSeventhnightState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientWorld.class)
public class SkyColorMixin {

    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void onGetSkyColor(Vec3d pos, float tickDelta, CallbackInfoReturnable<Vec3d> cir) {
        if (ClientSeventhnightState.isActive()) {
            cir.setReturnValue(new Vec3d(0.65, 0.02, 0.02));
        }
    }
}