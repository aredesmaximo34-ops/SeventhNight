package com.seventhnight.mixin;

import com.seventhnight.SeventhNightMod;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class SleepMixin {

    @Inject(method = "trySleep", at = @At("HEAD"), cancellable = true)
    private void onTrySleep(net.minecraft.util.math.BlockPos pos, CallbackInfoReturnable<PlayerEntity.SleepFailureReason> cir) {
        if (SeventhNightMod.isSeventhnightActive) {
            PlayerEntity player = (PlayerEntity)(Object)this;
            player.sendMessage(Text.literal("§c¡No puedes dormir durante la Blood Moon!"), true);
            cir.setReturnValue(PlayerEntity.SleepFailureReason.OTHER_PROBLEM);
        }
    }
}
