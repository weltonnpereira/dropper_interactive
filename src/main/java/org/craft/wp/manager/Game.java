package org.craft.wp.manager;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.craft.wp.tasks.ActionBarLoop;
import org.craft.wp.tasks.DropperCollisionTask;
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
        this.startCollisionTask(player);
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

        this.cancelCollisionTask(uuid);
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

    /**
     * Lógica principal chamada a cada movimento ou tick do jogador para checar se ele caiu na água ou bateu em algo.
     */
//    public void checkDropperCollision(Player player) {
//        if (!isPlaying(player)) return;
//
//        Location playerLoc = player.getLocation();
//        int currentLevel = this.getActualLevel(player);
//        int effectivePhase = gameManager.getPhase().getEffectivePhase(currentLevel);
//
//        Location spawnLoc = gameManager.getPhase().getPhaseSpawn(effectivePhase);
//        Location goalLoc = gameManager.getPhase().getPhaseGoal(effectivePhase);
//
//        if (spawnLoc == null || goalLoc == null) return;
//
//        // Zona segura da plataforma de spawn (permite andar antes de pular)
//        if (Math.abs(playerLoc.getBlockY() - spawnLoc.getBlockY()) <= 2.5) {
//            return;
//        }
//
//        Block blockAtFeet = playerLoc.getBlock();
//        Block blockBelow = playerLoc.clone().subtract(0, 1, 0).getBlock();
//
//        // CONDIÇÃO DE VITÓRIA (Qualquer poça de água na altura Y do goal)
//        boolean isWater = (blockAtFeet.getType() == Material.WATER || blockBelow.getType() == Material.WATER);
//        boolean atGoalHeight = playerLoc.getWorld().equals(goalLoc.getWorld()) &&
//                Math.abs(playerLoc.getBlockY() - goalLoc.getBlockY()) <= 2;
//
//        if (isWater && atGoalHeight) {
//            this.advanceDropperPhase(player);
//            return;
//        }
//
//        BoundingBox playerBox = player.getBoundingBox().expand(0.01, 0.02, 0.01);
//
//        World world = player.getWorld();
//        boolean hitSolid = false;
//
//        int minX = (int) Math.floor(playerBox.getMinX());
//        int maxX = (int) Math.floor(playerBox.getMaxX());
//        int minY = (int) Math.floor(playerBox.getMinY());
//        int maxY = (int) Math.floor(playerBox.getMaxY());
//        int minZ = (int) Math.floor(playerBox.getMinZ());
//        int maxZ = (int) Math.floor(playerBox.getMaxZ());
//
//        // Varre os blocos ao redor e valida a interseção física
//        for (int x = minX; x <= maxX; x++) {
//            for (int y = minY; y <= maxY; y++) {
//                for (int z = minZ; z <= maxZ; z++) {
//                    Block block = world.getBlockAt(x, y, z);
//                    if (isSolidBlock(block)) {
//                        BoundingBox blockBox = block.getBoundingBox();
//
//                        // Verifica se há sobreposição real
//                        if (playerBox.overlaps(blockBox)) {
//
//                            // --- FILTRO INTELIGENTE: LATERAL VS TOPO ---
//                            // Se a parte inferior dos pés do jogador estiver abaixo do topo do bloco,
//                            // significa que ele apenas roçou/bateu na lateral. Nós IGNORAMOS!
//                            // Só consideramos derrota se ele realmente aterrissar em cima do bloco (pés >= topo do bloco).
//                            double playerFeetY = playerBox.getMinY();
//                            double blockTopY = blockBox.getMaxY();
//
//                            // Margem de tolerância pequena (0.15) para garantir que pousos no topo sejam pegos com precisão
//                            if (playerFeetY >= (blockTopY - 0.15)) {
//                                hitSolid = true;
//                                break;
//                            }
//                        }
//                    }
//                }
//                if (hitSolid) break;
//            }
//            if (hitSolid) break;
//        }
//
//        if (hitSolid) {
//            player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_BIG_FALL, 0.2f, 1.0f);
//            this.returnCheckpoint(player, false);
//        }
//    }

    public void checkDropperCollision(Player player) {
        if (!isPlaying(player)) return;

        Location playerLoc = player.getLocation();
        int currentLevel = this.getActualLevel(player);
        int effectivePhase = gameManager.getPhase().getEffectivePhase(currentLevel);

        Location spawnLoc = gameManager.getPhase().getPhaseSpawn(effectivePhase);
        Location goalLoc = gameManager.getPhase().getPhaseGoal(effectivePhase);

        if (spawnLoc == null || goalLoc == null) return;

        // 1. Zona segura da plataforma de spawn (permite andar antes de pular)
        if (Math.abs(playerLoc.getBlockY() - spawnLoc.getBlockY()) <= 2.5) {
            return;
        }

        Block blockAtFeet = playerLoc.getBlock();
        Block blockBelow = playerLoc.clone().subtract(0, 1, 0).getBlock();

        // CONDIÇÃO DE VITÓRIA (Qualquer poça de água na altura Y do goal)
        boolean isWater = (blockAtFeet.getType() == Material.WATER || blockBelow.getType() == Material.WATER);
        boolean atGoalHeight = playerLoc.getWorld().equals(goalLoc.getWorld()) &&
                Math.abs(playerLoc.getBlockY() - goalLoc.getBlockY()) <= 2;

        if (isWater && atGoalHeight) {
            this.advanceDropperPhase(player);
            return;
        }

        // Se a física nativa do Minecraft detectou que o player pisou em qualquer bloco/quina (isOnGround), já marca derrota direto
        boolean hitSolid = player.isOnGround();

        if (!hitSolid) {
            BoundingBox playerBox = player.getBoundingBox().expand(0.02, 0.05, 0.02);
            World world = player.getWorld();

            int minX = (int) Math.floor(playerBox.getMinX());
            int maxX = (int) Math.floor(playerBox.getMaxX());
            int minY = (int) Math.floor(playerBox.getMinY());
            int maxY = (int) Math.floor(playerBox.getMaxY());
            int minZ = (int) Math.floor(playerBox.getMinZ());
            int maxZ = (int) Math.floor(playerBox.getMaxZ());

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        Block block = world.getBlockAt(x, y, z);
                        if (isSolidBlock(block)) {
                            BoundingBox blockBox = block.getBoundingBox();

                            if (playerBox.overlaps(blockBox)) {
                                double playerFeetY = playerBox.getMinY();
                                double blockTopY = blockBox.getMaxY();

                                // Ignora raspões laterais; só mata se aterrisar no topo
                                if (playerFeetY >= (blockTopY - 0.25)) {
                                    hitSolid = true;
                                    break;
                                }
                            }
                        }
                    }
                    if (hitSolid) break;
                }
                if (hitSolid) break;
            }
        }

        if (hitSolid) {
            player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_BIG_FALL, 0.2f, 1.0f);
            this.returnCheckpoint(player, false);
        }
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

    public void startCollisionTask(Player player) {
        UUID uuid = player.getUniqueId();
        this.cancelCollisionTask(uuid);

        // Roda a cada 1 ou 2 ticks (1L = 20x por segundo, resposta instantânea sem delay)
        BukkitTask task = new DropperCollisionTask(gameManager, uuid)
                .runTaskTimer(gameManager.getPlugin(), 0L, 1L);

        gameManager.collisionTasks.put(uuid, task);
    }

    public void cancelCollisionTask(UUID uuid) {
        BukkitTask task = gameManager.collisionTasks.remove(uuid);
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }
}
