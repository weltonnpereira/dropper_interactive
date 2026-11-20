package org.craft.wp.listeners;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.craft.wp.manager.GameManager;

public class DropperListener implements Listener {

    private final GameManager gameManager;

    public DropperListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) {
            return;
        }

        // Reset/Winner
        if (gameManager.getLive().getReset().isResetting(player) || gameManager.getGame().isWinner(player)) {
            Location from = event.getFrom();
            Location to = event.getTo();

            if (to != null && (from.getBlockX() != to.getBlockX() ||
                    from.getBlockY() != to.getBlockY() ||
                    from.getBlockZ() != to.getBlockZ())) {

                Location freezeLoc = from.clone();
                freezeLoc.setYaw(to.getYaw());
                freezeLoc.setPitch(to.getPitch());

                player.teleport(freezeLoc);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (gameManager.playingPlayers.contains(player.getUniqueId())) {
            gameManager.playingPlayers.remove(player.getUniqueId());

            if (gameManager.playingPlayers.isEmpty()) {
                gameManager.getHologramManager().removeAllHolograms();
            }
        }
    }

}
