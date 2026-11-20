package org.craft.wp.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.craft.wp.manager.GameManager;
import org.craft.wp.setup.Phase;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HologramManager {

    private final GameManager gameManager;
    private final Map<Integer, ArmorStand> phaseHolograms = new HashMap<>();
    private BukkitTask distanceCheckTask;

    // Raio de 12 blocos (12 * 12 = 144) em distanceSquared
    private static final double MAX_DISTANCE_SQUARED = 144.0;

    public HologramManager(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public void spawnAllHolograms() {
        this.removeAllHolograms();

        Phase phaseManager = gameManager.getPhase();
        int maxPhase = phaseManager.getMaxPhase();

        for (int i = 1; i <= maxPhase; i++) {
            Location phaseLoc = phaseManager.getPhaseSpawn(i);
            if (phaseLoc != null && phaseLoc.getWorld() != null) {
                Location holoLoc = phaseLoc.clone().add(0, 2.1, 0);

                ArmorStand holo = (ArmorStand) phaseLoc.getWorld().spawnEntity(holoLoc, EntityType.ARMOR_STAND);
                holo.setInvisible(true);
                holo.setGravity(false);
                holo.setMarker(true);
                holo.setCustomName("§b§lFASE §e§l" + i);
                holo.setCustomNameVisible(false);

                phaseHolograms.put(i, holo);
            }
        }

        startDistanceChecker();
    }

    private void startDistanceChecker() {
        if (distanceCheckTask != null) {
            distanceCheckTask.cancel();
        }

        distanceCheckTask = Bukkit.getScheduler().runTaskTimer(gameManager.getPlugin(), () -> {
            if (phaseHolograms.isEmpty()) return;

            for (UUID uuid : gameManager.playingPlayers) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null || !player.isOnline()) continue;

                int currentLevel = gameManager.getGame().getActualLevel(player);
                Location playerLoc = player.getLocation();

                // Checa apenas um raio reduzido de fases em volta da fase atual do jogador (ex: -2 até +3)
                int minCheck = Math.max(1, currentLevel - 2);
                int maxCheck = Math.min(phaseHolograms.size(), currentLevel + 3);

                for (Map.Entry<Integer, ArmorStand> entry : phaseHolograms.entrySet()) {
                    int phaseNum = entry.getKey();
                    ArmorStand holo = entry.getValue();

                    if (holo == null || !holo.isValid()) continue;

                    // Fases fora do alcance das adjacentes são forçadamente ocultadas sem medir distância
                    if (phaseNum < minCheck || phaseNum > maxCheck) {
                        if (holo.isCustomNameVisible()) {
                            holo.setCustomNameVisible(false);
                        }
                        continue;
                    }

                    // Para as fases próximas, verifica a distância exata
                    boolean isNear = holo.getWorld().equals(playerLoc.getWorld()) &&
                            holo.getLocation().distanceSquared(playerLoc) <= MAX_DISTANCE_SQUARED;

                    if (holo.isCustomNameVisible() != isNear) {
                        holo.setCustomNameVisible(isNear);
                    }
                }
            }
        }, 0L, 5L);
    }

    public void removeAllHolograms() {
        if (distanceCheckTask != null) {
            distanceCheckTask.cancel();
            distanceCheckTask = null;
        }

        for (ArmorStand holo : phaseHolograms.values()) {
            if (holo != null && holo.isValid()) {
                holo.remove();
            }
        }
        phaseHolograms.clear();

        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (ArmorStand stand : world.getEntitiesByClass(ArmorStand.class)) {
                if (stand.getCustomName() != null && stand.getCustomName().contains("FASE ")) {
                    stand.remove();
                }
            }
        }
    }
}
