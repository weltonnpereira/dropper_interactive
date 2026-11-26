package org.craft.wp.tasks;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.craft.wp.manager.GameManager;

public class JailTask extends BukkitRunnable {

    private final GameManager gameManager;
    private final Player player;
    private int remainingSeconds;

    private String currentSubtitle;
    private int subtitleTicksRemaining = 0;

    public JailTask(GameManager gameManager, Player player, int durationSeconds, String initialDonor) {
        this.gameManager = gameManager;
        this.player = player;
        this.remainingSeconds = durationSeconds;

        if (initialDonor != null && !initialDonor.isEmpty()) {
            this.currentSubtitle = "§e" + initialDonor + " §fte mandou para a prisão!";
            this.subtitleTicksRemaining = 100;
        } else {
            this.currentSubtitle = "§cVocê foi aprisionado!";
            this.subtitleTicksRemaining = 60;
        }

        sendCurrentTitle();
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.5f, 1.0f);
    }

    @Override
    public void run() {
        if (!gameManager.getGame().isPlaying(player) || !player.isOnline() || remainingSeconds <= 0) {
            gameManager.getLive().getJail().release(player, null);
            this.cancel();
            return;
        }

        this.spawnJailParticles();

        if (subtitleTicksRemaining > 0) {
            subtitleTicksRemaining -= 20;
            if (subtitleTicksRemaining <= 0) {
                this.currentSubtitle = "";
            }
        }

        sendCurrentTitle();

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        remainingSeconds--;
    }

    /**
     * Adiciona mais tempo dinamicamente quando o chat manda outro jail (acumulador)
     */
    public void addTime(int seconds, String donor) {
        this.remainingSeconds += seconds;

        if (donor != null && !donor.isEmpty()) {
            this.currentSubtitle = "§e" + donor + " §fextendeu seu tempo em §a+" + seconds + "s§f!";
        } else {
            this.currentSubtitle = "§cSeu tempo foi estendido em +" + seconds + "s!";
        }

        this.subtitleTicksRemaining = 100; // Exibe a mensagem de extensão por 5 segundos
        sendCurrentTitle();
    }

    private void sendCurrentTitle() {
        String title = "§c§lPRESO §7| §e" + formatTime(remainingSeconds);
        String subtitle = currentSubtitle != null ? currentSubtitle : "";

        player.sendTitle(title, subtitle, 0, 25, 5);
    }

    public int getRemainingSeconds() {
        return remainingSeconds;
    }

    private String formatTime(int totalSeconds) {
        if (totalSeconds < 0) totalSeconds = 0;

        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    /**
     * Desenha partículas nas bordas da jaula simulando um campo de força / limite visual.
     */
    private void spawnJailParticles() {
        if (!player.isOnline()) return;

        Location blockLoc = gameManager.getLive().getJail().getPlayerJailLocation(player);
        if (blockLoc == null) return;

        Location base = blockLoc.clone().subtract(1, 1, 1);
        World world = base.getWorld();
        if (world == null) return;

        double minX = base.getX();
        double maxX = base.getX() + 3.0;
        double minY = base.getY();
        double maxY = base.getY() + 4.0;
        double minZ = base.getZ();
        double maxZ = base.getZ() + 3.0;

        for (double y = minY; y <= maxY; y += 0.3) {
            world.spawnParticle(Particle.FLAME, minX, y, minZ, 3, 0, 0, 0, 0);
            world.spawnParticle(Particle.FLAME, maxX, y, minZ, 3, 0, 0, 0, 0);
            world.spawnParticle(Particle.FLAME, minX, y, maxZ, 3, 0, 0, 0, 0);
            world.spawnParticle(Particle.FLAME, maxX, y, maxZ, 3, 0, 0, 0, 0);
        }
        for (double x = minX; x <= maxX; x += 0.3) {
            world.spawnParticle(Particle.FLAME, x, maxY, minZ, 3, 0, 0, 0, 0);
            world.spawnParticle(Particle.FLAME, x, maxY, maxZ, 3, 0, 0, 0, 0);
        }
        for (double z = minZ; z <= maxZ; z += 0.3) {
            world.spawnParticle(Particle.FLAME, minX, maxY, z, 3, 0, 0, 0, 0);
            world.spawnParticle(Particle.FLAME, maxX, maxY, z, 3, 0, 0, 0, 0);
        }
        for (double x = minX; x <= maxX; x += 0.3) {
            world.spawnParticle(Particle.FLAME, x, minY, minZ, 3, 0, 0, 0, 0);
            world.spawnParticle(Particle.FLAME, x, minY, maxZ, 3, 0, 0, 0, 0);
        }
        for (double z = minZ; z <= maxZ; z += 0.3) {
            world.spawnParticle(Particle.FLAME, minX, minY, z, 3, 0, 0, 0, 0);
            world.spawnParticle(Particle.FLAME, maxX, minY, z, 3, 0, 0, 0, 0);
        }
    }

    public void cancelTask() {
        player.resetTitle();
        this.cancel();
    }
}