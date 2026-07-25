package com.seventhnight.mixin;

import com.seventhnight.client.ClientSeventhnightState;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BackgroundRenderer.class)
public class FogRendererMixin {

    // Color de niebla objetivo (rojo sangre intenso, tipo Blood Moon)
    private static final float FOG_RED = 0.65f;
    private static final float FOG_GREEN = 0.02f;
    private static final float FOG_BLUE = 0.02f;

    @Inject(method = "render", at = @At("TAIL"))
    private static void onRenderFog(Camera camera, float tickDelta, ClientWorld world, int i, float f, CallbackInfo ci) {
        float intensity = ClientSeventhnightState.getIntensity();
        if (intensity > 0.0f) {
            float r = FOG_RED * intensity;
            float g = FOG_GREEN * intensity;
            float b = FOG_BLUE * intensity;

            RenderSystem.setShaderFogColor(r, g, b);

            float currentFogEnd = RenderSystem.getShaderFogEnd();
            float newEnd = currentFogEnd * (1.0f - (intensity * 0.85f));
            float newStart = newEnd * 0.1f;

            RenderSystem.setShaderFogStart(newStart);
            RenderSystem.setShaderFogEnd(newEnd);
        }
    }
}