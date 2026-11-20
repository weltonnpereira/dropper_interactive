package org.craft.wp.tasks;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.craft.wp.manager.GameManager;

public class WinnerTask extends BukkitRunnable {

    private final GameManager gameManager;
    private final Player player;
    private int timeLeft;
    private int holdCounter = 0;
    private int lastPlayedTime = -1;

    public WinnerTask(GameManager gameManager, Player player, int timeLeft) {
        this.gameManager = gameManager;
        this.player = player;
        this.timeLeft = timeLeft;
    }

    @Override
    public void run() {
        if (!player.isOnline() || !gameManager.getGame().isPlaying(player) || !gameManager.getGame().isWinner(player)) {
            cancel();
            return;
        }

        if (timeLeft <= 0) {
            gameManager.getGame().triggerVictory(player);
            cancel();
            return;
        }

        if (timeLeft != lastPlayedTime) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f);
            lastPlayedTime = timeLeft;
        }

        String color = (timeLeft <= 3) ? "§c§l" : "§e§l";
        player.sendTitle(color + timeLeft, "", 0, 25, 0);

        if (timeLeft <= 3) {
            holdCounter++;
            if (holdCounter >= 3) {
                holdCounter = 0;
                timeLeft--;
            }
        } else {
            timeLeft--;
        }
    }
}
