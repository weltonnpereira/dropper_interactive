package org.craft.wp.interactions;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.craft.wp.manager.GameManager;

public class ArrowShoot {

    private final GameManager gameManager;

    public ArrowShoot(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public void shootFaceArrow(Player player) {
        if (!gameManager.getGame().isPlaying(player)) {
            return;
        }

        Location eyeLoc = player.getEyeLocation();

        // Força a direção estritamente horizontal
        Vector direction = eyeLoc.getDirection();
        direction.setY(0);
        if (direction.lengthSquared() == 0) {
            direction = player.getLocation().getDirection().setY(0);
        }
        direction.normalize();

        // Criamos uma versão final/effectively final para uso seguro dentro do runnable/lambda
        final Vector finalDirection = direction.clone();

        // RayTrace curto (1.2 blocos) para verificar se há parede colada no rosto do streamer
        RayTraceResult rayTrace = player.getWorld().rayTraceBlocks(eyeLoc, finalDirection, 1.2);
        boolean isGluedInCorner = (rayTrace != null && rayTrace.getHitBlock() != null);

        if (isGluedInCorner) {
            player.playSound(eyeLoc, Sound.ENTITY_ARROW_SHOOT, 1.0f, 0.5f);

            Bukkit.getScheduler().runTaskLater(gameManager.getPlugin(), () -> {
                if (!player.isOnline()) return;

                player.damage(0.0); // Flash vermelho de dano sem tirar vida

                Vector knockback = finalDirection.clone().multiply(-0.9).setY(0.15);
                player.setVelocity(knockback);
            }, 1L);

            return;
        }

        // Posiciona a flecha na altura dos olhos, projetada no plano horizontal
        Location spawnLoc = eyeLoc.clone().add(finalDirection.clone().multiply(1.5));
        Vector initialVelocity = eyeLoc.toVector().subtract(spawnLoc.toVector()).normalize().multiply(1.5);

        org.bukkit.entity.Arrow arrow = player.getWorld().spawnArrow(spawnLoc, initialVelocity, 1.5f, 0.0f);
        arrow.setCritical(true);
        arrow.setGravity(false);

        player.playSound(eyeLoc, Sound.ENTITY_ARROW_SHOOT, 1.0f, 0.5f);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    if (arrow.isValid()) arrow.remove();
                    cancel();
                    return;
                }

                // Condição de acerto ou limite de tempo
                if (ticks > 30 || arrow.getLocation().distanceSquared(player.getLocation()) < 1.2 || arrow.isInBlock() || !arrow.isValid()) {
                    player.damage(0.0, arrow);
                    player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT, 1.0f, 1.0f);

                    // Garante que o vetor de empurrão final da flecha também seja puramente horizontal
                    Vector arrowDir = arrow.getVelocity();
                    arrowDir.setY(0);
                    if (arrowDir.lengthSquared() == 0) {
                        arrowDir = finalDirection.clone().multiply(-1);
                    } else {
                        arrowDir.normalize();
                    }

                    Vector customKnockback = arrowDir.multiply(0.9).setY(0.15);
                    player.setVelocity(customKnockback);

                    if (arrow.isValid()) {
                        arrow.remove();
                    }

                    cancel();
                    return;
                }

                // Persegue a cabeça do player em tempo real
                Location targetLoc = player.getEyeLocation();
                Vector newVelocity = targetLoc.toVector().subtract(arrow.getLocation().toVector()).normalize().multiply(2.0);
                arrow.setVelocity(newVelocity);

                ticks++;
            }
        }.runTaskTimer(gameManager.getPlugin(), 1L, 1L);
    }

}
