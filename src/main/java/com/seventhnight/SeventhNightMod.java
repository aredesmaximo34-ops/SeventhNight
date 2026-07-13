package com.seventhnight;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import com.seventhnight.networking.SeventhnightSyncPacket;

public class SeventhNightMod implements ModInitializer {

    public static final String MOD_ID = "seventhnight";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static boolean isSeventhnightActive = false;
    private static int nightCounter = 0;
    private static boolean nightCounted = false;

    private static final int SEVENTH_NIGHT_EVERY_N_NIGHTS = 7;
    public static boolean forceFirstNight = false;

    @Override
    public void onInitialize() {
        LOGGER.info("Seventhnight Mod cargado!");
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
        ServerLivingEntityEvents.AFTER_DEATH.register(this::onMobDeath);
        LOGGER.info("Eventos registrados. Horda cada " + SEVENTH_NIGHT_EVERY_N_NIGHTS + " noches.");
    }

    private void onServerTick(MinecraftServer server) {
        try {
            ServerWorld world = server.getOverworld();
            long time = world.getTimeOfDay() % 24000;

            if (time >= 13000 && time < 13020 && !nightCounted) {
                nightCounted = true;
                nightCounter++;

                if (nightCounter % SEVENTH_NIGHT_EVERY_N_NIGHTS == 0) {
                    isSeventhnightActive = true;
                    LOGGER.info("[SEVENTHNIGHT] ¡La Séptima Noche ha llegado! (Dia " + nightCounter + ")");
                    server.getPlayerManager().broadcast(
                            net.minecraft.text.Text.literal("§f§lDia" + " §4§l" + nightCounter + " - " + "§4☠"),
                            false
                    );
                    world.getGameRules().get(GameRules.SPAWN_RADIUS).set(10, server);

                    // Enviar packet a todos los clientes usando FABRIC
                    SeventhnightSyncPacket packet = new SeventhnightSyncPacket(true);
                    for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                        ServerPlayNetworking.send(player, SeventhnightSyncPacket.ID, packet.toBuf());
                    }

                    com.seventhnight.SeventhnightSpawner.forceStartFirstWave(world);

                } else {
                    int nightsUntil = SEVENTH_NIGHT_EVERY_N_NIGHTS - (nightCounter % SEVENTH_NIGHT_EVERY_N_NIGHTS);
                    server.getPlayerManager().broadcast(
                            net.minecraft.text.Text.literal("§f§lDia " + nightCounter + " -"
                                    + " §c§lFaltan " + nightsUntil + " §c§ldias para la Horda"),
                            false
                    );
                }
            }

            if (time < 13000) {
                nightCounted = false;
            }

            if (isSeventhnightActive) {
                if (time >= 23000 || time < 1000) {
                    isSeventhnightActive = false;
                    com.seventhnight.SeventhnightSpawner.reset(server);
                    LOGGER.info("[SEVENTHNIGHT] Horda terminada por amanecer.");
                    world.getGameRules().get(GameRules.SPAWN_RADIUS).set(10, server);

                    // Enviar packet para desactivar usando FABRIC
                    SeventhnightSyncPacket packet = new SeventhnightSyncPacket(false);
                    for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                        ServerPlayNetworking.send(player, SeventhnightSyncPacket.ID, packet.toBuf());
                    }
                }

                com.seventhnight.SeventhnightSpawner.onSeventhnightTick(world);
            }
        } catch (Exception e) {
            LOGGER.error("[SEVENTHNIGHT] ERROR en onServerTick: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void onMobDeath(LivingEntity entity, DamageSource damageSource) {
        if (entity.hasCustomName()
                && entity.getCustomName().getString().contains("☠")) {
            com.seventhnight.SeventhnightSpawner.onMobDied();
        }
    }
}
