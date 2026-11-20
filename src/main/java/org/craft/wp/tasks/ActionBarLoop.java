package org.craft.wp.tasks;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.craft.wp.manager.GameManager;

public class ActionBarLoop extends BukkitRunnable {

    private final GameManager gameManager;

    private boolean shouldContinue = true;

    public ActionBarLoop(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public void stopLoop() {
        shouldContinue = false;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!gameManager.getGame().isPlaying(player) || !shouldContinue) {
                this.cancel();
                return;
            }

            int actualLevel = gameManager.getGame().getActualLevel(player);
            int maxLevel = gameManager.getPhase().getMaxPhase();

            String color = (actualLevel < 1) ? "§c§l" : "§a§l";

            Component title = Component.text("§e§lDROPPER §7| §fFase Atual: " + color + actualLevel + "§7/§a" + maxLevel);

            player.sendActionBar(title);
        }
    }
}
