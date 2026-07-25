package com.seventhnight;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import java.util.*;

public class SeventhnightSpawner {

    private static int currentWave = 0;
    private static int totalMobs = 0;
    private static int mobsAliveCount = 0;
    private static final int MOBS_PER_WAVE = 5;
    private static int tickCounter = 0;
    private static Queue<BlockPos> spawnQueue = new LinkedList<>();
    private static int waveDelay = 0;
    private static boolean rewardGiven = false;

    // --- Zombies explosivos ---
    private static final Set<UUID> explosiveZombies = new HashSet<>();
    private static final Map<UUID, Integer> fuseCounters = new HashMap<>();
    private static final double EXPLOSION_TRIGGER_RANGE = 3.0;
    private static final int FUSE_TICKS = 30;
    private static final float EXPLOSION_POWER = 1.5f;

    public static void reset(MinecraftServer server) {
        currentWave = 0;
        totalMobs = 0;
        mobsAliveCount = 0;
        tickCounter = 0;
        waveDelay = 0;
        rewardGiven = false;
        spawnQueue.clear();
        explosiveZombies.clear();
        fuseCounters.clear();
    }

    public static void forceStartFirstWave(ServerWorld world) {
        if (currentWave == 0) {
            SeventhNightMod.LOGGER.info("[SPAWNER] Forzando inicio de primera oleada...");
            startNewWave(world);
        }
    }

    public static void onSeventhnightTick(ServerWorld world, boolean nearDawn) {
        if (!SeventhNightMod.isSeventhnightActive) return;

        try {
            tickExplosiveZombies(world);

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

            SeventhNightMod.LOGGER.info("[SPAWNER] Tick 20: realAlive=" + realAlive
                    + " | currentWave=" + currentWave
                    + " | totalMobs=" + totalMobs
                    + " | waveDelay=" + waveDelay
                    + " | nearDawn=" + nearDawn
                    + " | cola=" + spawnQueue.size());

            if (mobsAliveCount <= 0) {
                if (currentWave == 0) {
                    startNewWave(world);
                } else if (nearDawn) {
                    if (!rewardGiven) {
                        rewardGiven = true;
                        spawnRewardChests(world);
                    }
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

    private static void tickExplosiveZombies(ServerWorld world) {
        if (explosiveZombies.isEmpty()) return;

        Iterator<UUID> it = explosiveZombies.iterator();
        while (it.hasNext()) {
            UUID uuid = it.next();
            Entity entity = world.getEntity(uuid);

            if (entity == null || !entity.isAlive()) {
                it.remove();
                fuseCounters.remove(uuid);
                continue;
            }

            ServerPlayerEntity nearest = (ServerPlayerEntity) world.getClosestPlayer(entity, EXPLOSION_TRIGGER_RANGE);

            if (nearest != null) {
                int fuse = fuseCounters.getOrDefault(uuid, FUSE_TICKS);
                fuse--;

                if (fuse % 5 == 0) {
                    entity.setGlowing(fuse % 10 == 0);
                }

                if (fuse <= 0) {
                    world.createExplosion(entity, entity.getX(), entity.getY(), entity.getZ(),
                            EXPLOSION_POWER, false, World.ExplosionSourceType.MOB);
                    entity.discard();
                    it.remove();
                    fuseCounters.remove(uuid);
                } else {
                    fuseCounters.put(uuid, fuse);
                }
            } else {
                fuseCounters.remove(uuid);
                entity.setGlowing(true);
            }
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

                    if (random.nextInt(4) == 0) { // 25% de probabilidad de ser explosivo
                        explosiveZombies.add(zombie.getUuid());
                        zombie.setCustomName(Text.literal("§c💥"));
                    }

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

    private static void spawnRewardChests(ServerWorld world) {
        try {
            int hordesSurvived = SeventhNightMod.nightCounter / 7; // 1 = primera horda, 2 = segunda, etc.
            Random random = world.getRandom();

            for (ServerPlayerEntity player : world.getPlayers()) {
                BlockPos chestPos = player.getBlockPos().add(2, 0, 2);
                chestPos = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, chestPos);

                world.setBlockState(chestPos, Blocks.CHEST.getDefaultState());

                if (world.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
                    List<ItemStack> loot = generateLootForTier(hordesSurvived, random);
                    int slot = 0;
                    for (ItemStack stack : loot) {
                        if (!stack.isEmpty() && slot < chest.size()) {
                            chest.setStack(slot, stack);
                            slot++;
                        }
                    }
                }

                SeventhNightMod.LOGGER.info("[SPAWNER] Cofre de recompensa (tier " + hordesSurvived + ") colocado en " + chestPos);
            }
        } catch (Exception e) {
            SeventhNightMod.LOGGER.error("[SPAWNER] ERROR en spawnRewardChests: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static List<ItemStack> generateLootForTier(int tier, Random random) {
        List<ItemStack> loot = new ArrayList<>();

        if (tier <= 1) {
            loot.add(new ItemStack(Items.BREAD, 4 + random.nextBetween(0, 4)));
            loot.add(new ItemStack(Items.COAL, 3 + random.nextBetween(0, 5)));
            loot.add(new ItemStack(Items.IRON_INGOT, 2 + random.nextBetween(0, 3)));
            loot.add(new ItemStack(Items.TORCH, 8));
            if (random.nextBoolean()) loot.add(new ItemStack(Items.IRON_SWORD, 1));

        } else if (tier == 2) {
            loot.add(new ItemStack(Items.IRON_INGOT, 4 + random.nextBetween(0, 4)));
            loot.add(new ItemStack(Items.GOLDEN_APPLE, 1 + random.nextBetween(0, 2)));
            loot.add(new ItemStack(Items.ARROW, 16 + random.nextBetween(0, 16)));
            loot.add(new ItemStack(Items.IRON_CHESTPLATE, 1));
            loot.add(new ItemStack(Items.SHIELD, 1));

        } else {
            loot.add(new ItemStack(Items.DIAMOND, 1 + random.nextBetween(0, 3) + (tier - 3)));
            loot.add(new ItemStack(Items.GOLDEN_APPLE, 1 + random.nextBetween(0, 2)));
            if (random.nextBoolean()) loot.add(new ItemStack(Items.DIAMOND_SWORD, 1));
            if (random.nextInt(10) == 0) loot.add(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 1));
            loot.add(new ItemStack(Items.IRON_CHESTPLATE, 1));
            loot.add(new ItemStack(Items.ARROW, 32));
        }

        return loot;
    }
}