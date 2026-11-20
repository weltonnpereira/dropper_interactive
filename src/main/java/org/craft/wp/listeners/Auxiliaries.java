package org.craft.wp.listeners;

import org.bukkit.GameMode;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.craft.wp.manager.GameManager;

public class Auxiliaries implements Listener {

    private final GameManager gameManager;

    public Auxiliaries(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onHungerChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();

        if (!gameManager.getGame().isPlaying(player) || gameManager.getLive().getReset().isResetting(player) || gameManager.getGame().isWinner(player)) {
            event.setCancelled(true);
            return;
        }

        if (event.getDamager() instanceof Arrow) {
            event.setDamage(0.0);
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();

        // Se não estiver jogando, ou estiver em jail/reset/winner, protege de dano
        if (!gameManager.getGame().isPlaying(player) ||
                gameManager.getLive().getJail().isInJail(player) ||
                gameManager.getLive().getReset().isResetting(player) ||
                gameManager.getGame().isWinner(player)) {

            event.setCancelled(true);
            return;
        }

        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
            return;
        }

        // Para qualquer outro dano letal (ex: lava, fogo, etc.), tratamos como morte no dropper
        if (player.getHealth() - event.getDamage() <= 0) {
            event.setCancelled(true);
            gameManager.resetStatusPlayer(player);

            if (gameManager.getLive().getJail().isInJail(player)) {
                gameManager.getLive().getJail().returnToJail(player);
                return;
            }

            gameManager.getGame().returnCheckpoint(player, false);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        if (player.getGameMode() != GameMode.CREATIVE && gameManager.getGame().isPlaying(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        if (player.getGameMode() != GameMode.CREATIVE && gameManager.getGame().isPlaying(player)) {
            event.setCancelled(true);
        }
    }
}
