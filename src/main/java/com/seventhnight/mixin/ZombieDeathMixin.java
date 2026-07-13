package com.seventhnight.mixin;

import com.seventhnight.SeventhNightMod;
import com.seventhnight.SeventhnightSpawner;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.ZombieEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class ZombieDeathMixin {

    @Inject(method = "onDeath", at = @At("HEAD"))
    private void onDeath(DamageSource source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity)(Object)this;
        if (self instanceof ZombieEntity &&
                SeventhNightMod.isSeventhnightActive &&
                self.hasCustomName() &&
                self.getCustomName().getString().equals("§4Horda")) {
            SeventhnightSpawner.onMobDied();
        }
    }
}