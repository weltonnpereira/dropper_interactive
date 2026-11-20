package org.craft.wp.interactions;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.craft.wp.manager.GameManager;
import org.craft.wp.tasks.ResetTimer;
import org.craft.wp.tasks.WinnerTask;

import java.util.HashMap;
import java.util.UUID;

public class Reset {

    private final GameManager gameManager;

    public HashMap<UUID, Boolean> inResetMode = new HashMap<>();
    public HashMap<UUID, Integer> savedLevelsBeforeReset = new HashMap<>();
    public HashMap<UUID, BukkitTask> activeResetTasks = new HashMap<>();

    public Reset(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public boolean isResetting(Player player) {
        return this.inResetMode.getOrDefault(player.getUniqueId(), false);
    }

    public void startReset(Player player) {
        if (!gameManager.getGame().isPlaying(player)) {
            player.sendMessage("§c[Parkour] O parkour não foi iniciado!");
            return;
        }

        UUID uuid = player.getUniqueId();

        Jail jail = gameManager.getLive().getJail();
        if (jail.isInJail(player)) {
            jail.suspendJail(player);
            player.sendMessage("§cSua prisão foi suspensa temporariamente pelo Reset!");
        }

        if (gameManager.getGame().isWinner(player)) {
            gameManager.getGame().cancelWinnerTask(player);
        }

        if (this.activeResetTasks.containsKey(uuid)) {
            this.activeResetTasks.get(uuid).cancel();
        }

        int actualLevel = gameManager.getGame().getActualLevel(player);

        if (!this.isResetting(player)) {
            this.savedLevelsBeforeReset.put(uuid, actualLevel);
        }

        // caso o player não esteja em nenhum level (testes ou algo assim) setamos o level 1 para ele
        if (!gameManager.playerLevels.containsKey(uuid)) {
            gameManager.playerLevels.put(uuid, 1);
        }

        this.inResetMode.put(uuid, true);

        // new feature: prender o player no nível atual
        Location actualLevelLoc = gameManager.getPhase().getPhaseSpawn(actualLevel);
        if (actualLevelLoc != null) {
            gameManager.resetStatusPlayer(player);
            player.teleport(actualLevelLoc);
        } else {
            player.sendMessage("§c[Parkour] O nível " + actualLevel + " ainda não foi configurado no mapa!");
        }

        ResetTimer task = new ResetTimer(gameManager, player, 60);
        this.activeResetTasks.put(uuid, task.runTaskTimer(gameManager.getPlugin(), 0L, 20L));
    }

    public void cancelReset(Player player, boolean isStartStop, String donor) {
        if (!gameManager.getGame().isPlaying(player)) {
            player.sendMessage("§c[Parkour] O parkour não foi iniciado ou não esta no modo reset!");
            return;
        }

        if (!this.isResetting(player)) {
            return;
        }

        UUID uuid = player.getUniqueId();

        // Cancela o timer
        if (this.activeResetTasks.containsKey(uuid)) {
            this.activeResetTasks.get(uuid).cancel();
            this.activeResetTasks.remove(uuid);
        }

        this.inResetMode.put(uuid, false);

        // Restaura o nível anterior e teleportamos de volta
        int previousLevel = this.savedLevelsBeforeReset.getOrDefault(uuid, 1);
        gameManager.playerLevels.put(uuid, previousLevel);
        this.savedLevelsBeforeReset.remove(uuid);
        int maxPhase = gameManager.getPhase().getMaxPhase();

        if (previousLevel >= maxPhase) {
            gameManager.inWinnerMode.put(uuid, true);

            WinnerTask task = new WinnerTask(gameManager, player, 10);
            gameManager.activeWinnerTasks.put(uuid, task.runTaskTimer(gameManager.getPlugin(), 0L, 20L));
        }

        Location restoredLoc = gameManager.getPhase().getPhaseSpawn(previousLevel);
        if (restoredLoc != null) {
            gameManager.resetStatusPlayer(player);
            player.teleport(restoredLoc);
        }

        gameManager.getLive().getJail().restoreSuspendedJail(player);

        if (!isStartStop) {
            player.sendTitle("§a§lRESET CANCELADO!", donor + " §fDe Volta ao Nível " + previousLevel, 10, 40, 10);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        }
    }

    public void completeReset(Player player) {
        UUID uuid = player.getUniqueId();

        this.inResetMode.put(uuid, false);
        this.savedLevelsBeforeReset.remove(uuid);
        this.activeResetTasks.remove(uuid);

        // Define o nível definitivo do jogador como 1
        int levelToReturn = 1;
        gameManager.playerLevels.put(uuid, levelToReturn);

        Location lv1 = gameManager.getPhase().getPhaseSpawn(levelToReturn);
        if (lv1 != null) {
            gameManager.resetStatusPlayer(player);
            player.teleport(lv1);
        }

        gameManager.getLive().getJail().restoreSuspendedJail(player);

        player.sendTitle("§c§lRESET CONCLUÍDO :(!", "§fVocê voltou para o Nível 1!", 10, 50, 10);
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.8f, 1.0f);
    }

}
