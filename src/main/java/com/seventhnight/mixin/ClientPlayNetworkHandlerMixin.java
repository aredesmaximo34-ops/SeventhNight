package com.seventhnight.mixin;

import com.seventhnight.client.ClientSeventhnightState;
import com.seventhnight.networking.SeventhnightSyncPacket;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    @Inject(method = "onCustomPayload", at = @At("HEAD"))
    private void onCustomPayload(CustomPayloadS2CPacket packet, CallbackInfo ci) {
        try {
            if (packet.getChannel().toString().equals(SeventhnightSyncPacket.ID.toString())) {
                boolean active = packet.getData().readBoolean();
                ClientSeventhnightState.setActive(active);
            }
        } catch (Exception e) {
            // Ignorar errores
        }
    }
}
