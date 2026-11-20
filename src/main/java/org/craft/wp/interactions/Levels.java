package org.craft.wp.interactions;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.craft.wp.manager.GameManager;
import org.craft.wp.tasks.WinnerTask;

import java.util.UUID;

public class Levels {

    private final GameManager gameManager;

    public Levels(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public void modifyLevel(Player player, int qty, String donor) {
        if (!gameManager.getGame().isPlaying(player)) {
            player.sendMessage("§c[Parkour] O parkour não foi iniciado");
            return;
        }

        UUID uuid = player.getUniqueId();

        boolean isWinner = gameManager.getGame().isWinner(player);

        if (isWinner && qty < 0) {
            this.gameManager.getGame().cancelWinnerTask(player);
        }

        int actualLevel = gameManager.getGame().getActualLevel(player);
        int newLevel = actualLevel + qty;

        int maxPhase = gameManager.getPhase().getMaxPhase();
        if (newLevel >= maxPhase) {
            newLevel = maxPhase;
            gameManager.playerLevels.put(uuid, newLevel);

            Location maxLoc = gameManager.getPhase().getPhaseSpawn(newLevel);
            if (maxLoc != null) {
                gameManager.resetStatusPlayer(player);
                player.teleport(maxLoc);
            }

            // Inicia a contagem de vitória de 10s citando o doador se ainda não estiver em Winner
            if (!isWinner) {
                gameManager.inWinnerMode.put(uuid, true);
                player.sendMessage("§a§lRETA FINAL! §e" + donor + " §fcolocou você no último nível!");
                WinnerTask task = new WinnerTask(gameManager, player, 10);
                gameManager.activeWinnerTasks.put(uuid, task.runTaskTimer(gameManager.getPlugin(), 0L, 20L));
            }
            return;
        }

        int effectivePhase = gameManager.getPhase().getEffectivePhase(newLevel);
        Location loc = gameManager.getPhase().getPhaseSpawn(effectivePhase);

        if (loc != null) {
            gameManager.resetStatusPlayer(player);
            player.teleport(loc);
            gameManager.playerLevels.put(uuid, newLevel);

            if (qty > 0) {
                String title = "§a§l+ " + qty + " NÍVEL(IS)!";
                String subtitle = (newLevel <= 0)
                        ? "§e" + donor + " §faliviou sua dívida! Nível atual: §c" + newLevel
                        : "§e" + donor + " §fajudou você a ir para a §bFase " + newLevel;
                player.sendTitle(title, subtitle, 10, 40, 10);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.0f);
            } else if (qty < 0) {
                String title = "§c§l" + qty + " NÍVEL(IS)!";
                String subtitle = (newLevel <= 0)
                        ? "§e" + donor + " §fte deixou negativado! Nível: §4" + newLevel
                        : "§e" + donor + " §fte jogou para a §bFase " + newLevel;
                player.sendTitle(title, subtitle, 10, 40, 10);
                player.playSound(player.getLocation(), Sound.ENTITY_WITHER_HURT, 0.5f, 1.0f);
            }
        } else {
            player.sendMessage("§c[Dropper] A fase " + effectivePhase + " ainda não foi configurado no mapa!");
        }
    }

}
