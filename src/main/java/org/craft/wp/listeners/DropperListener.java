package org.craft.wp.listeners;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.craft.wp.manager.GameManager;

public class DropperListener implements Listener {

    private final GameManager gameManager;

    public DropperListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();

        if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) {
            return;
        }

        if (to == null) return;

        if (gameManager.getLive().getReset().isResetting(player) || gameManager.getGame().isWinner(player)) {
            if (from.getBlockX() != to.getBlockX() ||
                    from.getBlockY() != to.getBlockY() ||
                    from.getBlockZ() != to.getBlockZ()) {

                Location freezeLoc = from.clone();
                freezeLoc.setYaw(to.getYaw());
                freezeLoc.setPitch(to.getPitch());

                player.teleport(freezeLoc);
            }
            return;
        }

        if (event.isCancelled() || event instanceof PlayerTeleportEvent
                || !gameManager.getGame().isPlaying(player)) {
            return;
        }

        if (from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ()) {
            return;
        }

        // Jail (não deixar o player passar dos limites da jaula)
        if (gameManager.getLive().getJail().isInJail(player)) {
            Location centerLoc = gameManager.getLive().getJail().getPlayerJailLocation(player);
            if (centerLoc != null) {
                Location blockLoc = centerLoc.getBlock().getLocation();

                if (to != null) {
                    // Limites permitidos dentro da jaula 3x3 (margem de 1.2 blocos a partir do centro)
                    double centerX = blockLoc.getX() + 0.5;
                    double centerZ = blockLoc.getZ() + 0.5;
                    double minY = blockLoc.getY();
                    double maxY = blockLoc.getY() + 3.0; // Altura máxima da jaula

                    boolean outOfBoundsH = Math.abs(to.getX() - centerX) > 1.2 || Math.abs(to.getZ() - centerZ) > 1.2;
                    boolean outOfBoundsV = to.getY() < minY || to.getY() > maxY;

                    // Se o player tentar ultrapassar as grades/paredes da jaula
                    if (outOfBoundsH || outOfBoundsV) {
                        // Puxa ele de volta para o centro seguro mantendo a visão da câmera
                        Location safeLoc = blockLoc.clone().add(0.5, 1.0, 0.5);
                        safeLoc.setYaw(to.getYaw());
                        safeLoc.setPitch(to.getPitch());
                        player.teleport(safeLoc);
                        player.sendMessage("§cTentativa de fugir da jaula bloqueado.");
                        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.1f, 1.0f);
                    }
                }
            }
            return;
        }

        gameManager.getGame().checkDropperCollision(player, from, to);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        gameManager.hideBossBar(player);

        if (gameManager.playingPlayers.contains(player.getUniqueId())) {
            gameManager.playingPlayers.remove(player.getUniqueId());

            if (gameManager.playingPlayers.isEmpty()) {
                gameManager.getHologramManager().removeAllHolograms();
            }
        }

        if (gameManager.playingPlayers.isEmpty()) {
            gameManager.stopBossBarLoop();
        }
    }

}
