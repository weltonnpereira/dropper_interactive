package org.craft.wp.manager;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.util.BoundingBox;
import org.craft.wp.tasks.ActionBarLoop;
import org.craft.wp.tasks.WinnerTask;

import java.util.UUID;

public class Game {

    private final GameManager gameManager;

    public Game(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public boolean isPlaying(Player player) {
        return gameManager.playingPlayers.contains(player.getUniqueId());
    }

    public boolean isWinner(Player player) {
        return gameManager.inWinnerMode.getOrDefault(player.getUniqueId(), false);
    }

    public void startDropper(Player player) {
        UUID uuid = player.getUniqueId();

        if (gameManager.getLive().getReset().isResetting(player)) {
            gameManager.getLive().getReset().cancelReset(player, true, "");
        }

        if (this.isWinner(player)) {
            this.cancelWinnerTask(player);
        }

        gameManager.playingPlayers.add(uuid);
        gameManager.playerLevels.put(uuid, 1); // Nível aqui representa o número da fase/queda

        Location level1 = gameManager.getPhase().getPhaseSpawn(1);
        if (level1 == null) {
            player.sendMessage("§c[Dropper] A Fase 1 ainda não foi configurada!");
            return;
        }

        gameManager.getHologramManager().spawnAllHolograms();
        gameManager.resetStatusPlayer(player);
        ActionBarLoop actionBarLoop = new ActionBarLoop(gameManager);
        gameManager.setBossBar(actionBarLoop);

        player.setGameMode(GameMode.ADVENTURE);
        player.teleport(level1);
        player.sendMessage("§a[Dropper] Você iniciou o desafio de queda livre!");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
    }

    public void stopDropper(Player player) {
        UUID uuid = player.getUniqueId();

        if (gameManager.getLive().getReset().isResetting(player)) {
            gameManager.getLive().getReset().cancelReset(player, true, "");
        }

        if (isWinner(player)) {
            this.cancelWinnerTask(player);
        }

        gameManager.playingPlayers.remove(uuid);
        gameManager.playerLevels.remove(uuid);

        Location level1 = gameManager.getPhase().getPhaseSpawn(1);
        if (level1 != null) {
            gameManager.getHologramManager().removeAllHolograms();
            gameManager.resetStatusPlayer(player);
            player.teleport(level1);
        }

        player.sendActionBar("");
        player.sendMessage("§c[Dropper] Desafio encerrado e dados resetados.");
    }

    public int getActualLevel(Player player) {
        return gameManager.playerLevels.getOrDefault(player.getUniqueId(), 1);
    }

    public void triggerVictory(Player player) {
        cancelWinnerTask(player);
        player.sendTitle("§e§l🎉 PARABÉNS! 🎉", "§aVocê completou todos os Droppers!", 10, 80, 20);
        Bukkit.getScheduler().runTaskLater(gameManager.getPlugin(), () -> {
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            this.spawnVictoryFireworks(player);
        }, 20L);
        stopDropper(player);
    }

    private void spawnVictoryFireworks(Player player) {
        Location loc = player.getLocation();
        Firework firework = loc.getWorld().spawn(loc, Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();

        FireworkEffect effect = org.bukkit.FireworkEffect.builder()
                .flicker(true)
                .trail(true)
                .with(FireworkEffect.Type.BALL_LARGE)
                .withColor(Color.GREEN, Color.YELLOW, Color.AQUA)
                .withFade(Color.FUCHSIA)
                .build();
        meta.addEffect(effect);
        meta.setPower(1);
        firework.setFireworkMeta(meta);
    }

    public void checkDropperCollision(Player player, Location from, Location to) {
        if (!isPlaying(player) || !from.getWorld().equals(to.getWorld())) return;

        int currentLevel = this.getActualLevel(player);
        int effectivePhase = gameManager.getPhase().getEffectivePhase(currentLevel);

        Location spawnLoc = gameManager.getPhase().getPhaseSpawn(effectivePhase);
        Location goalLoc = gameManager.getPhase().getPhaseGoal(effectivePhase);

        if (spawnLoc == null || goalLoc == null) return;

        // 1. Zona segura da plataforma de spawn (permite andar antes de pular)
        if (Math.abs(to.getBlockY() - spawnLoc.getBlockY()) <= 2.5) {
            return;
        }

        BoundingBox playerBox = player.getBoundingBox();
        double halfWidthX = (playerBox.getMaxX() - playerBox.getMinX()) / 2.0;
        double halfWidthZ = (playerBox.getMaxZ() - playerBox.getMinZ()) / 2.0;
        double playerHeight = playerBox.getMaxY() - playerBox.getMinY();

        boolean atGoalHeight = to.getWorld().equals(goalLoc.getWorld())
                && Math.abs(to.getBlockY() - goalLoc.getBlockY()) <= 2;
        if (atGoalHeight && touchesMaterialAlongPath(
                from, to, halfWidthX, halfWidthZ, playerHeight, Material.WATER)) {
            this.advanceDropperPhase(player);
            return;
        }

        boolean hitSolid = player.isOnGround()
                || touchesSolidAlongPath(from, to, halfWidthX, halfWidthZ, playerHeight);

        if (hitSolid) {
            player.playSound(to, Sound.ENTITY_GENERIC_BIG_FALL, 0.2f, 1.0f);
            this.returnCheckpoint(player, false);
        }
    }

    private boolean touchesSolidAlongPath(
            Location from, Location to, double halfWidthX, double halfWidthZ, double playerHeight) {
        World world = from.getWorld();
        double minX = Math.min(from.getX(), to.getX()) - halfWidthX;
        double maxX = Math.max(from.getX(), to.getX()) + halfWidthX;
        double minY = Math.min(from.getY(), to.getY()) - playerHeight;
        double maxY = Math.max(from.getY(), to.getY());
        double minZ = Math.min(from.getZ(), to.getZ()) - halfWidthZ;
        double maxZ = Math.max(from.getZ(), to.getZ()) + halfWidthZ;
        double epsilon = 1.0E-7;

        for (int x = (int) Math.floor(minX - epsilon); x <= Math.floor(maxX + epsilon); x++) {
            for (int y = (int) Math.floor(minY - epsilon); y <= Math.floor(maxY + epsilon); y++) {
                for (int z = (int) Math.floor(minZ - epsilon); z <= Math.floor(maxZ + epsilon); z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (!isSolidBlock(block)) continue;

                    BoundingBox blockBox = block.getBoundingBox();
                    BoundingBox playerPathBounds = new BoundingBox(
                            blockBox.getMinX() - halfWidthX - epsilon,
                            blockBox.getMinY() - playerHeight - epsilon,
                            blockBox.getMinZ() - halfWidthZ - epsilon,
                            blockBox.getMaxX() + halfWidthX + epsilon,
                            blockBox.getMaxY() + epsilon,
                            blockBox.getMaxZ() + halfWidthZ + epsilon);

                    if (segmentIntersects(from, to, playerPathBounds)) return true;
                }
            }
        }
        return false;
    }

    private boolean touchesMaterialAlongPath(
            Location from, Location to,
            double halfWidthX, double halfWidthZ, double playerHeight,
            Material material) {
        World world = from.getWorld();
        double minX = Math.min(from.getX(), to.getX()) - halfWidthX;
        double maxX = Math.max(from.getX(), to.getX()) + halfWidthX;
        double minY = Math.min(from.getY(), to.getY()) - playerHeight;
        double maxY = Math.max(from.getY(), to.getY());
        double minZ = Math.min(from.getZ(), to.getZ()) - halfWidthZ;
        double maxZ = Math.max(from.getZ(), to.getZ()) + halfWidthZ;
        double epsilon = 1.0E-7;

        for (int x = (int) Math.floor(minX - epsilon); x <= Math.floor(maxX + epsilon); x++) {
            for (int y = (int) Math.floor(minY - epsilon); y <= Math.floor(maxY + epsilon); y++) {
                for (int z = (int) Math.floor(minZ - epsilon); z <= Math.floor(maxZ + epsilon); z++) {
                    if (world.getBlockAt(x, y, z).getType() != material) continue;

                    BoundingBox waterBox = new BoundingBox(x, y, z, x + 1, y + 1, z + 1);
                    BoundingBox playerPathBounds = new BoundingBox(
                            waterBox.getMinX() - halfWidthX - epsilon,
                            waterBox.getMinY() - playerHeight - epsilon,
                            waterBox.getMinZ() - halfWidthZ - epsilon,
                            waterBox.getMaxX() + halfWidthX + epsilon,
                            waterBox.getMaxY() + epsilon,
                            waterBox.getMaxZ() + halfWidthZ + epsilon);

                    if (segmentIntersects(from, to, playerPathBounds)) return true;
                }
            }
        }
        return false;
    }

    private boolean segmentIntersects(Location from, Location to, BoundingBox box) {
        double[] interval = {0.0, 1.0};
        return clipSegmentAxis(from.getX(), to.getX() - from.getX(), box.getMinX(), box.getMaxX(), interval)
                && clipSegmentAxis(from.getY(), to.getY() - from.getY(), box.getMinY(), box.getMaxY(), interval)
                && clipSegmentAxis(from.getZ(), to.getZ() - from.getZ(), box.getMinZ(), box.getMaxZ(), interval);
    }

    private boolean clipSegmentAxis(
            double start, double delta, double min, double max, double[] interval) {
        if (Math.abs(delta) < 1.0E-12) {
            return start >= min && start <= max;
        }

        double entry = (min - start) / delta;
        double exit = (max - start) / delta;
        if (entry > exit) {
            double swap = entry;
            entry = exit;
            exit = swap;
        }

        interval[0] = Math.max(interval[0], entry);
        interval[1] = Math.min(interval[1], exit);
        return interval[0] <= interval[1];
    }

    private boolean isSolidBlock(Block block) {
        Material type = block.getType();
        return type.isSolid() && type != Material.WATER;
    }

    public void advanceDropperPhase(Player player) {
        UUID uuid = player.getUniqueId();
        int currentLevel = getActualLevel(player);
        int nextLevel = currentLevel + 1;
        int maxPhase = gameManager.getPhase().getMaxPhase();

        boolean isWinner = isWinner(player); // ou gameManager.getGame().isWinner(player) dependendo de onde está o método

        // SE ATINGIU OU PASSOU DO LIMITE MÁXIMO (Fase Final / Vitória)
        if (nextLevel >= maxPhase) {
            nextLevel = maxPhase;
            gameManager.playerLevels.put(uuid, nextLevel);

            Location maxLoc = gameManager.getPhase().getPhaseSpawn(maxPhase);
            if (maxLoc != null) {
                gameManager.resetStatusPlayer(player);
                player.teleport(maxLoc);
            }

            if (!isWinner) {
                gameManager.inWinnerMode.put(uuid, true);
                player.sendMessage("§a[Dropper] Você chegou ao último nível! Sobreviva por 10 segundos!");
                WinnerTask task = new WinnerTask(gameManager, player, 10);
                gameManager.activeWinnerTasks.put(uuid, task.runTaskTimer(gameManager.getPlugin(), 0L, 20L));
            }
            return;
        }

        // TRATAMENTO PARA NÍVEIS NEGATIVOS / DÍVIDA (Se aplicável ao seu modo)
        if (nextLevel < 0) { // Ajuste conforme a sua regra de níveis negativos
            int effectivePhase = gameManager.getPhase().getEffectivePhase(nextLevel);
            Location phaseSpawn = gameManager.getPhase().getPhaseSpawn(effectivePhase);

            if (phaseSpawn == null) return;

            gameManager.playerLevels.put(uuid, nextLevel);
            gameManager.resetStatusPlayer(player);
            player.teleport(phaseSpawn);

            int leftCount = Math.abs(nextLevel);
            player.sendTitle("§e§lDÍVIDA REDUZIDA!", "§fNível atual: §c" + nextLevel + " §7(Faltam " + leftCount + "x para quitar)", 10, 45, 10);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
            return;
        }

        int effectivePhase = gameManager.getPhase().getEffectivePhase(nextLevel);
        Location nextLoc = gameManager.getPhase().getPhaseSpawn(effectivePhase);

        if (nextLoc != null) {
            gameManager.playerLevels.put(uuid, nextLevel);
            gameManager.resetStatusPlayer(player);
            player.teleport(nextLoc);

            player.sendTitle("§a§lFASE CONCLUÍDA!", "§eVocê avançou para o nível §b" + nextLevel, 10, 40, 10);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.0f);
        } else {
            player.sendMessage("§c[Dropper] A fase " + effectivePhase + " ainda não foi configurada no mapa!");
        }
    }

    public void returnCheckpoint(Player player, boolean message) {
        if (!isPlaying(player)) {
            return;
        }

        int currentLevel = this.getActualLevel(player);
        int effectivePhase = gameManager.getPhase().getEffectivePhase(currentLevel);
        Location checkpoint = gameManager.getPhase().getPhaseSpawn(effectivePhase);

        if (checkpoint != null) {
            player.teleport(checkpoint);
            player.setFireTicks(0);
            if (message) {
                player.sendMessage("§c[Dropper] Você bateu em um obstáculo! Retornando ao topo da fase " + currentLevel);
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.3f, 1.0f);
            }
        }
    }

    public void cancelWinnerTask(Player player) {
        UUID uuid = player.getUniqueId();
        gameManager.inWinnerMode.put(uuid, false);

        if (gameManager.activeWinnerTasks.containsKey(uuid)) {
            gameManager.activeWinnerTasks.get(uuid).cancel();
            gameManager.activeWinnerTasks.remove(uuid);
        }
    }

}
