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
import org.bukkit.event.entity.PlayerDeathEvent;
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
        if (!(event.getEntity() instanceof Player player)) return;

        // Enquanto isPlaying estiver ativo, o player não toma dano
        if (gameManager.getGame().isPlaying(player)) {
            // Mantém apenas o knockback da flecha (interação do chat) com dano zerado
            if (event instanceof EntityDamageByEntityEvent byEntity && byEntity.getDamager() instanceof Arrow) {
                if (gameManager.getLive().getJail().isInJail(player) ||
                        gameManager.getLive().getReset().isResetting(player) ||
                        gameManager.getGame().isWinner(player)) {
                    event.setCancelled(true);
                    return;
                }
                event.setDamage(0.0);
                return;
            }

            // Cancela qualquer outro dano (queda, fogo, lava, sufocamento, mobs, etc.)
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        if (gameManager.getGame().isPlaying(player)) {
            event.setCancelled(true);
            event.getDrops().clear();
            event.setDroppedExp(0);

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
