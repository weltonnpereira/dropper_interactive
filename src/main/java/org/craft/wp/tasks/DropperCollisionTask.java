package org.craft.wp.tasks;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.craft.wp.manager.GameManager;

import java.util.UUID;

public class DropperCollisionTask extends BukkitRunnable {

    private final GameManager gameManager;
    private final UUID playerUuid;

    public DropperCollisionTask(GameManager gameManager, UUID playerUuid) {
        this.gameManager = gameManager;
        this.playerUuid = playerUuid;
    }

    @Override
    public void run() {
        Player player = Bukkit.getPlayer(playerUuid);

        if (player == null || !player.isOnline() || !gameManager.getGame().isPlaying(player)) {
            gameManager.getGame().cancelCollisionTask(playerUuid);
            return;
        }

        // Ignora verificação se o jogador estiver congelado em reset, jail ou modo vencedor
        if (gameManager.getLive().getReset().isResetting(player) || gameManager.getGame().isWinner(player)) {
            return;
        }

        gameManager.getGame().checkDropperCollision(player);
    }
}