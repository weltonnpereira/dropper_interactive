package org.craft.wp.interactions;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.craft.wp.manager.GameManager;
import org.craft.wp.tasks.JailTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Jail {

    private final GameManager gameManager;

    public final HashMap<UUID, Boolean> inJail = new HashMap<>();

    // Agora guardamos diretamente a JailTask, que já controla o tempo, BossBar e estados internos!
    public final HashMap<UUID, JailTask> activeJailTasks = new HashMap<>();

    private final Map<UUID, Location> playerOriginalLocations = new HashMap<>();
    private final Map<UUID, Map<Location, BlockData>> placedBlocksSnapshots = new HashMap<>();
    private final Map<UUID, Location> playerJailLocations = new HashMap<>();
    private final HashMap<UUID, Integer> suspendedJailTimes = new HashMap<>();

    public Jail(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    /**
     * Método principal chamado quando alguém interage para prender o player
     */
    public void handleJail(Player player, int durationSeconds, String donor) {
        if (!gameManager.getGame().isPlaying(player)) {
            player.sendMessage("§c[Parkour] O parkour não foi iniciado!");
            return;
        }

        UUID uuid = player.getUniqueId();

        if (this.isInJail(player)) {
            JailTask task = activeJailTasks.get(uuid);
            if (task != null) {
                task.addTime(durationSeconds, donor);
            }
            player.playSound(player.getLocation(), Sound.ENTITY_GHAST_SHOOT, 0.6f, 1.0f);
            return;
        }

        this.setInJail(player, true);
        this.buildStructureAndTeleport(player);

        JailTask task = new JailTask(gameManager, player, durationSeconds, donor);
        task.runTaskTimer(gameManager.getPlugin(), 0L, 20L);
        activeJailTasks.put(uuid, task);

        player.sendMessage("§cVocê foi enviado para a prisão!");
        player.playSound(player.getLocation(), Sound.ENTITY_GHAST_SHOOT, 0.6f, 1.0f);
    }

    /**
     * Suspende a prisão atual limpa a estrutura do mundo e guarda o tempo restante.
     */
    public void suspendJail(Player player) {
        if (!this.isInJail(player)) return;
        if (!gameManager.getGame().isPlaying(player)) {

        }

        UUID uuid = player.getUniqueId();

        JailTask task = activeJailTasks.remove(uuid);
        if (task != null) {
            // Guarda o tempo exato que faltava para quando for restaurar
            suspendedJailTimes.put(uuid, task.getRemainingSeconds());
            task.cancelTask();
        }

        Map<Location, BlockData> originalBlocks = this.removePlacedBlocksSnapshots(player);
        if (originalBlocks != null) {
            for (Map.Entry<Location, BlockData> entry : originalBlocks.entrySet()) {
                entry.getKey().getBlock().setBlockData(entry.getValue());
            }
        }

        this.removePlayerOriginalLocations(player);
        this.setInJail(player, false);
        this.removePlayerJailLocation(player);
    }

    /**
     * Restaura a prisão suspensa no novo local onde o player aterrissou após o reset.
     */
    public void restoreSuspendedJail(Player player) {
        UUID uuid = player.getUniqueId();
        if (!suspendedJailTimes.containsKey(uuid)) return;

        int remaining = suspendedJailTimes.remove(uuid);

        // Pequeno delay de 1 tick para garantir que o teleporte do Reset finalizou na Main Thread
        Bukkit.getScheduler().runTaskLater(gameManager.getPlugin(), () -> {
            if (player.isOnline() && !this.isInJail(player)) {
                this.handleJail(player, remaining, null);
            }
        }, 1L);
    }

    public void returnToJail(Player player) {
        if (!gameManager.getGame().isPlaying(player)) {
            player.sendMessage("§c[Parkour] O parkour não foi iniciado!");
            return;
        }

        if (gameManager.getLive().getReset().isResetting(player) || gameManager.getGame().isWinner(player)) return;

        UUID uuid = player.getUniqueId();
        if (!isInJail(player)) return;

        Location jailLoc = playerJailLocations.get(uuid);
        if (jailLoc != null) {
            Location center = jailLoc.clone().add(0.5, 1.0, 0.5);
            center.setYaw(player.getYaw());
            center.setPitch(player.getPitch());

            player.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
            player.setFallDistance(0.0f);

            player.teleport(center);
        }
    }

    private void buildStructureAndTeleport(Player player) {
        this.setPlayerOriginalLocations(player, player.getLocation().clone());

        Location blockLoc = player.getLocation().getBlock().getLocation();
        this.setPlayerJailLocation(player, blockLoc.clone());

        player.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
        player.setFallDistance(0.0f);

        Location base = blockLoc.clone().subtract(1, 1, 1);
        Map<Location, BlockData> worldSnapshotBlocks = new HashMap<>();

        // Verifica se a zona da jaula contém Água, Cactos ou Portas.
        boolean safeToBuildBlocks = !isAreaBlocked(base);

        if (safeToBuildBlocks) {
            for (int x = 0; x < 3; x++) {
                for (int z = 0; z < 3; z++) {
                    for (int y = 0; y < 4; y++) {
                        Location loc = base.clone().add(x, y, z);
                        Block block = loc.getBlock();
                        Material type = block.getType();

                        if (y == 0) {
                            // Chão seguro embaixo dos pés
                            if (x == 1 && z == 1 && type.isAir()) {
                                worldSnapshotBlocks.putIfAbsent(loc, block.getBlockData());
                                block.setType(Material.COBBLESTONE);
                            }
                        } else {
                            boolean isBorder = (x == 0 || x == 2 || z == 0 || z == 2);
                            boolean isRoof = (y == 3);

                            // Apenas paredes e teto em espaços de ar puro
                            if ((isBorder || isRoof) && type.isAir()) {
                                worldSnapshotBlocks.putIfAbsent(loc, block.getBlockData());
                                block.setType(Material.IRON_BARS);
                            }
                        }
                    }
                }
            }
        }

        this.setPlacedBlocksSnapshots(player, worldSnapshotBlocks);

        Location safeTeleport = base.clone().add(1.5, 1.0, 1.5);
        safeTeleport.setYaw(player.getYaw());
        safeTeleport.setPitch(player.getPitch());
        player.teleport(safeTeleport);
    }

    private boolean isAreaBlocked(Location base) {
        World world = base.getWorld();
        if (world == null) return true;

        for (int x = -1; x <= 3; x++) {
            for (int z = -1; z <= 3; z++) {
                for (int y = 0; y < 4; y++) {
                    Material mat = base.clone().add(x, y, z).getBlock().getType();
                    String name = mat.name();

                    if (mat == Material.WATER || mat == Material.CACTUS || mat == Material.LADDER || name.endsWith("_DOOR") /*|| name.endsWith("_STAIRS")*/) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Libera o jogador, cancela a task, esconde a BossBar e restaura o mundo.
     */
    public void release(Player player, String donor) {
        if (player == null) return;
        if (!isInJail(player)) return;

        UUID uuid = player.getUniqueId();
        JailTask task = activeJailTasks.remove(uuid);
        if (task != null) {
            task.cancelTask();
        }

        Location originalLocation = this.removePlayerOriginalLocations(player);
        if (originalLocation != null && player.isOnline()) {
            player.setVelocity(new Vector(0, 0, 0));
            player.setFallDistance(0.0f);
            gameManager.resetStatusPlayer(player);
            player.teleport(originalLocation);
        }

        Map<Location, BlockData> snapshot = this.removePlacedBlocksSnapshots(player);
        if (snapshot != null) {
            for (Map.Entry<Location, BlockData> entry : snapshot.entrySet()) {
                entry.getKey().getBlock().setBlockData(entry.getValue());
            }
        }

        this.setInJail(player, false);
        this.removePlayerJailLocation(player);

        if (player.isOnline()) {
            if (donor != null && !donor.trim().isEmpty()) {
                player.sendTitle("§a§lLIBERADO!", "§e" + donor + " §fresgatou você da prisão!", 10, 50, 10);
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                player.sendMessage("§a[Prisão] §e" + donor + " §fpagou sua fiança e tirou você da cadeia!");
            } else {
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                player.sendMessage("§a[Prisão] Seu tempo acabou e você foi solto com sucesso!");
            }
        }
    }

    // original loc
    public void setPlayerOriginalLocations(Player player, Location location) {
        playerOriginalLocations.put(player.getUniqueId(), location);
    }

    public Location removePlayerOriginalLocations(Player player) {
        return playerOriginalLocations.remove(player.getUniqueId());
    }

    // snapshot bloc
    public Map<Location, BlockData> getPlacedBlocksSnapshots(Player player) {
        return placedBlocksSnapshots.get(player.getUniqueId());
    }

    public void setPlacedBlocksSnapshots(Player player, Map<Location, BlockData> blocks) {
        placedBlocksSnapshots.put(player.getUniqueId(), blocks);
    }

    public Map<Location, BlockData> removePlacedBlocksSnapshots(Player player) {
        return placedBlocksSnapshots.remove(player.getUniqueId());
    }

    // jail
    public Location getPlayerJailLocation(Player player) {
        return playerJailLocations.get(player.getUniqueId());
    }

    public void setPlayerJailLocation(Player player, Location location) {
        playerJailLocations.put(player.getUniqueId(), location);
    }

    public void removePlayerJailLocation(Player player) {
        playerJailLocations.remove(player.getUniqueId());
    }

    public boolean isInJail(Player player) {
        return inJail.getOrDefault(player.getUniqueId(), false);
    }

    public void setInJail(Player player, boolean status) {
        this.inJail.put(player.getUniqueId(), status);
    }

}
