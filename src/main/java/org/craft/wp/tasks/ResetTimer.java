package org.craft.wp.tasks;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.craft.wp.manager.GameManager;

public class ResetTimer extends BukkitRunnable {

    private final GameManager gameManager;
    private final Player player;
    private int secondsLeft;

    public ResetTimer(GameManager gameManager, Player player, int totalSeconds) {
        this.gameManager = gameManager;
        this.player = player;
        this.secondsLeft = totalSeconds;
    }

    @Override
    public void run() {
        if (!player.isOnline()) {
            cancel();
            return;
        }

        if (secondsLeft <= 0) {
            // completa o reset para a fase 1
            gameManager.getLive().getReset().completeReset(player);
            cancel();
            return;
        }

        String color = (secondsLeft <= 10) ? "§c§l" : (secondsLeft <= 30) ? "§e§l" : "§a§l";

        player.sendTitle(
                color + secondsLeft,
                "§c§lRESET",
                0, 25, 0
        );

        if (secondsLeft <= 10) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f);
        } else {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        }

        secondsLeft--;
    }
}
