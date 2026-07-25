package com.seventhnight.client;

import com.seventhnight.networking.SeventhnightSyncPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public class SeventhnightClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {

        ClientPlayNetworking.registerGlobalReceiver(SeventhnightSyncPacket.ID, (client, handler, buf, responseSender) -> {
            boolean isActive = buf.readBoolean();
            client.execute(() -> {
                ClientSeventhnightState.setActive(isActive);
            });
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientSeventhnightState.tick();
            if (client.player != null) {

                boolean tieneCeguera = client.player.hasStatusEffect(StatusEffects.BLINDNESS);

                if (ClientSeventhnightState.isActive() && !tieneCeguera) {
                    client.player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 40, 0, false, false, true));
                } else if (!ClientSeventhnightState.isActive() && tieneCeguera) {
                    client.player.removeStatusEffect(StatusEffects.BLINDNESS);
                }
            }
        });
    }
}