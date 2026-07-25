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
    public static int nightCounter = 0;
    public static int totalZombieKills = 0;
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

                boolean isSeventhNight = (nightCounter % SEVENTH_NIGHT_EVERY_N_NIGHTS == 0);

                if (isSeventhNight) {
                    isSeventhnightActive = true;
                    LOGGER.info("[SEVENTHNIGHT] ¡La Séptima Noche ha llegado! (Dia " + nightCounter + ")");
                    server.getPlayerManager().broadcast(
                            net.minecraft.text.Text.literal("§f§lDÍA: §4§l" + nightCounter),
                            false
                    );
                    world.getGameRules().get(GameRules.SPAWN_RADIUS).set(10, server);

                    SeventhnightSyncPacket packet = new SeventhnightSyncPacket(true);
                    for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                        ServerPlayNetworking.send(player, SeventhnightSyncPacket.ID, packet.toBuf());
                    }

                    com.seventhnight.SeventhnightSpawner.forceStartFirstWave(world);
                } else {
                    server.getPlayerManager().broadcast(
                            net.minecraft.text.Text.literal("§f§lDÍA: " + nightCounter),
                            false
                    );
                }
            }

            if (time < 13000) {
                nightCounted = false;
            }

            if (isSeventhnightActive) {
                boolean isDaytime = time >= 1000 && time < 12000;
                if (isDaytime) {
                    isSeventhnightActive = false;
                    com.seventhnight.SeventhnightSpawner.reset(server);
                    LOGGER.info("[SEVENTHNIGHT] Horda terminada por amanecer.");
                    world.getGameRules().get(GameRules.SPAWN_RADIUS).set(10, server);

                    SeventhnightSyncPacket packet = new SeventhnightSyncPacket(false);
                    for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                        ServerPlayNetworking.send(player, SeventhnightSyncPacket.ID, packet.toBuf());
                    }
                }

                boolean nearDawn = time >= 21000;
                com.seventhnight.SeventhnightSpawner.onSeventhnightTick(world, nearDawn);
            }

            // Heartbeat: resincroniza el estado real cada 40 ticks (2 segundos),
            // por si se perdió el paquete puntual de activación/desactivación
            if (world.getTime() % 40 == 0) {
                SeventhnightSyncPacket syncPacket = new SeventhnightSyncPacket(isSeventhnightActive);
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    ServerPlayNetworking.send(player, SeventhnightSyncPacket.ID, syncPacket.toBuf());
                }
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
            totalZombieKills++;
        }
    }
}