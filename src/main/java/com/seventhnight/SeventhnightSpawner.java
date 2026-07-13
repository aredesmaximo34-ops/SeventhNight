package com.seventhnight;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.world.Heightmap;
import java.util.*;

public class SeventhnightSpawner {

    private static int currentWave = 0;
    private static int totalMobs = 0;
    private static int mobsAliveCount = 0;
    private static final int MOBS_PER_WAVE = 5;
    private static ServerBossBar bossBar = null;
    private static int tickCounter = 0;
    private static Queue<BlockPos> spawnQueue = new LinkedList<>();
    private static int waveDelay = 0;

    public static void reset(MinecraftServer server) {
        currentWave = 0;
        totalMobs = 0;
        mobsAliveCount = 0;
        tickCounter = 0;
        waveDelay = 0;
        spawnQueue.clear();
        if (bossBar != null) {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                bossBar.removePlayer(player);
            }
            bossBar = null;
        }
    }

    // Método para forzar el inicio inmediato
    public static void forceStartFirstWave(ServerWorld world) {
        if (currentWave == 0) {
            SeventhNightMod.LOGGER.info("[SPAWNER] Forzando inicio de primera oleada...");
            startNewWave(world);
        }
    }

    private static void setupBossBar(ServerWorld world, int total) {
        if (bossBar != null) {
            for (ServerPlayerEntity player : world.getPlayers()) {
                bossBar.removePlayer(player);
            }
        }
        bossBar = new ServerBossBar(
                Text.literal("§4☠ §lHorda - Oleada " + currentWave),
                BossBar.Color.RED,
                BossBar.Style.NOTCHED_10
        );
        bossBar.setPercent(1.0f);
        for (ServerPlayerEntity player : world.getPlayers()) {
            bossBar.addPlayer(player);
        }
        SeventhNightMod.LOGGER.info("[SPAWNER] BossBar configurado para oleada " + currentWave);
    }

    private static void updateBossBar(int alive) {
        if (bossBar == null) return;
        float percent = totalMobs > 0 ? (float) alive / totalMobs : 0f;
        bossBar.setPercent(Math.max(0f, Math.min(1f, percent)));
        if (alive <= 0 && currentWave > 0) {
            bossBar.setName(Text.literal("§l§6☠ ¡Oleada " + currentWave + " §l§6completada! ☠"));
        } else {
            bossBar.setName(Text.literal("§4☠ §lHorda - Oleada " + currentWave));
        }
    }

    public static void onSeventhnightTick(ServerWorld world) {
        if (!SeventhNightMod.isSeventhnightActive) return;

        try {
            // Spawnear hasta 2 mobs por tick
            int spawned = 0;
            int attempts = 0;
            while (!spawnQueue.isEmpty() && spawned < 2 && attempts < 10) {
                BlockPos pos = spawnQueue.poll();
                attempts++;
                if (isValidSpawnPos(world, pos)) {
                    spawnMob(world, pos);
                    spawned++;
                }
            }

            tickCounter++;
            if (tickCounter < 20) return;
            tickCounter = 0;

            int realAlive = countAliveMobs(world);
            mobsAliveCount = realAlive;
            updateBossBar(mobsAliveCount);

            SeventhNightMod.LOGGER.info("[SPAWNER] Tick 20: realAlive=" + realAlive
                    + " | currentWave=" + currentWave
                    + " | totalMobs=" + totalMobs
                    + " | waveDelay=" + waveDelay
                    + " | cola=" + spawnQueue.size());

            if (mobsAliveCount <= 0) {
                if (currentWave == 0) {
                    startNewWave(world);
                } else {
                    waveDelay++;
                    if (waveDelay >= 3) {
                        waveDelay = 0;
                        startNewWave(world);
                    }
                }
            } else {
                waveDelay = 0;
            }
        } catch (Exception e) {
            SeventhNightMod.LOGGER.error("[SPAWNER] ERROR en onSeventhnightTick: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static int countAliveMobs(ServerWorld world) {
        try {
            int count = 0;
            count += world.getEntitiesByType(
                    EntityType.ZOMBIE,
                    e -> e.hasCustomName() && e.getCustomName().getString().contains("☠")
            ).size();
            count += world.getEntitiesByType(
                    EntityType.SKELETON,
                    e -> e.hasCustomName() && e.getCustomName().getString().contains("☠")
            ).size();
            return count;
        } catch (Exception e) {
            SeventhNightMod.LOGGER.error("[SPAWNER] Error contando mobs: " + e.getMessage());
            return 0;
        }
    }

    private static boolean isValidSpawnPos(ServerWorld world, BlockPos pos) {
        try {
            if (world.getBlockState(pos.down()).isAir()) return false;
            if (!world.getBlockState(pos).isAir()) return false;
            if (!world.getBlockState(pos.up()).isAir()) return false;
            if (!world.getFluidState(pos).isEmpty()) return false;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void startNewWave(ServerWorld world) {
        try {
            currentWave++;
            totalMobs = MOBS_PER_WAVE * currentWave;

            SeventhNightMod.LOGGER.info("[SPAWNER] >>> Iniciando Oleada " + currentWave + " con " + totalMobs + " mobs <<<");

            prepareWave(world, currentWave);
            mobsAliveCount = totalMobs;

            setupBossBar(world, totalMobs);

            // 🆕 Forzar actualización inmediata de la barra
            updateBossBar(mobsAliveCount);

            world.getServer().getPlayerManager().broadcast(
                    Text.literal("§c⚠ §lOleada " + currentWave + " - " + totalMobs + " Mobs"),
                    false
            );

            SeventhNightMod.LOGGER.info("[SPAWNER] === Oleada " + currentWave + " lista. Cola: " + spawnQueue.size() + " ===");
        } catch (Exception e) {
            SeventhNightMod.LOGGER.error("[SPAWNER] ERROR en startNewWave: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void spawnMob(ServerWorld world, BlockPos pos) {
        try {
            Random random = world.getRandom();
            BlockPos spawnAt = pos.up();
            double x = spawnAt.getX() + 0.5;
            double y = spawnAt.getY();
            double z = spawnAt.getZ() + 0.5;

            if (random.nextBoolean()) {
                ZombieEntity zombie = EntityType.ZOMBIE.create(world);
                if (zombie != null) {
                    zombie.setCustomName(Text.literal("§4☠"));
                    zombie.setCustomNameVisible(true);
                    zombie.addStatusEffect(new StatusEffectInstance(
                            StatusEffects.GLOWING, Integer.MAX_VALUE, 0, false, false));
                    zombie.refreshPositionAndAngles(x, y, z, 0, 0);
                    zombie.initialize(world, world.getLocalDifficulty(spawnAt),
                            SpawnReason.EVENT, null, null);
                    world.spawnEntity(zombie);
                    SeventhNightMod.LOGGER.info("[SPAWNER] ✓ Zombie ☠ en " + spawnAt);
                }
            } else {
                SkeletonEntity skeleton = EntityType.SKELETON.create(world);
                if (skeleton != null) {
                    skeleton.setCustomName(Text.literal("§4☠"));
                    skeleton.setCustomNameVisible(true);
                    skeleton.addStatusEffect(new StatusEffectInstance(
                            StatusEffects.GLOWING, Integer.MAX_VALUE, 0, false, false));
                    skeleton.refreshPositionAndAngles(x, y, z, 0, 0);
                    skeleton.initialize(world, world.getLocalDifficulty(spawnAt),
                            SpawnReason.EVENT, null, null);
                    world.spawnEntity(skeleton);
                    SeventhNightMod.LOGGER.info("[SPAWNER] ✓ Skeleton ☠ en " + spawnAt);
                }
            }
        } catch (Exception e) {
            SeventhNightMod.LOGGER.error("[SPAWNER] ERROR en spawnMob: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void onMobDied() {
        if (mobsAliveCount > 0) {
            mobsAliveCount--;
        }
    }

    private static void prepareWave(ServerWorld world, int wave) {
        try {
            Random random = world.getRandom();
            int count = MOBS_PER_WAVE * wave;
            List<BlockPos> validPositions = new ArrayList<>();

            world.getPlayers().forEach(player -> {
                BlockPos playerPos = player.getBlockPos();
                int attempts = 0;
                int maxAttempts = count * 10;

                while (validPositions.size() < count && attempts < maxAttempts) {
                    int offsetX = random.nextBetween(-25, 25);
                    int offsetZ = random.nextBetween(-25, 25);

                    if (Math.abs(offsetX) < 5 && Math.abs(offsetZ) < 5) {
                        attempts++;
                        continue;
                    }

                    BlockPos spawnPos = playerPos.add(offsetX, 0, offsetZ);
                    spawnPos = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, spawnPos);
                    validPositions.add(spawnPos);
                    attempts++;
                }
            });

            spawnQueue.addAll(validPositions);
            SeventhNightMod.LOGGER.info("[SPAWNER] Preparados " + validPositions.size() + "/" + count + " spawns");
        } catch (Exception e) {
            SeventhNightMod.LOGGER.error("[SPAWNER] ERROR en prepareWave: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
