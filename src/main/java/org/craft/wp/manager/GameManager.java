package org.craft.wp.manager;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.craft.wp.Main;
import org.craft.wp.interactions.Live;
import org.craft.wp.setup.Phase;
import org.craft.wp.tasks.BossBarLoop;
import org.craft.wp.utils.HologramManager;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class GameManager {

    private final Main plugin;

    private final Phase phase;

    private final Live live;

    private final Game game;

    private final HologramManager hologramManager;

    private BossBarLoop bossBarLoop;
    private final Map<UUID, BossBar> playerBossBars = new HashMap<>();

    //start
    public Set<UUID> playingPlayers = new HashSet<>();

    // playerLevels
    public HashMap<UUID, Integer> playerLevels = new HashMap<>();

    //winner
    public HashMap<UUID, Boolean> inWinnerMode = new HashMap<>();
    public HashMap<UUID, BukkitTask> activeWinnerTasks = new HashMap<>();

    // Actions
    public static class LevelAction {
        private final UUID playerUUID;
        private final int qty;
        private final String donor;

        public LevelAction(UUID playerUUID, int qty, String donor) {
            this.playerUUID = playerUUID;
            this.qty = qty;
            this.donor = donor;
        }

        public UUID getPlayerUUID() { return playerUUID; }
        public int getQty() { return qty; }
        public String getDonor() { return donor; }
    }

    public static class CancelResetAction {
        private final UUID playerUUID;
        private final String donor;

        public CancelResetAction(UUID playerUUID, String donor) {
            this.playerUUID = playerUUID;
            this.donor = donor;
        }

        public UUID getPlayerUUID() { return playerUUID; }
        public String getDonor() { return donor; }
    }

    public static class ArrowAction {
        private final UUID playerUUID;
        private final String donor;

        public ArrowAction(UUID playerUUID, String donor) {
            this.playerUUID = playerUUID;
            this.donor = donor;
        }

        public UUID getPlayerUUID() { return playerUUID; }
        public String getDonor() { return donor; }
    }

    public static class JailAction {
        private final UUID playerUUID;
        private final int duration;
        private final String donor;

        public JailAction(UUID playerUUID, int duration, String donor) {
            this.playerUUID = playerUUID;
            this.duration = duration;
            this.donor = donor;
        }

        public UUID getPlayerUUID() { return playerUUID; }
        public int getDuration() { return duration; }
        public String getDonor() { return donor; }
    }

    public static class SlowFallingAction {
        private final UUID playerUUID;
        private final int duration;
        private final String donor;

        public SlowFallingAction(UUID playerUUID, int duration, String donor) {
            this.playerUUID = playerUUID;
            this.duration = duration;
            this.donor = donor;
        }

        public UUID getPlayerUUID() { return playerUUID; }
        public int getDuration() { return duration; }
        public String getDonor() { return donor; }
    }

    // interações e up down level
    private final ConcurrentLinkedQueue<ArrowAction> arrowQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<SlowFallingAction> slowFallingQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<LevelAction> levelQueue = new ConcurrentLinkedQueue<>();

    // Reset
    private final ConcurrentLinkedQueue<UUID> startResetQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<CancelResetAction> cancelResetQueue = new ConcurrentLinkedQueue<>();

    //jail
    private final ConcurrentLinkedQueue<JailAction> jailQueue = new ConcurrentLinkedQueue<>();

    // flush task
    private BukkitTask flushTask;

    public GameManager(Main plugin) {
        this.plugin = plugin;
        this.phase = new Phase(this);
        this.live = new Live(this);
        this.game = new Game(this);
        this.hologramManager = new HologramManager(this);

        // Processa as filas a cada 5 ticks (250 ms).
        startFlushProcessor();
    }

    private void startFlushProcessor() {
        flushTask = Bukkit.getScheduler().runTaskTimer(plugin, this::flushQueues, 0L, 5L);
    }

    /**
     * Processa no máximo uma ação de cada fila por ciclo, na mesma ordem de antes.
     * Ações que aguardam uma condição continuam na fila para serem reavaliadas.
     */
    private void flushQueues() {
        processArrowQueue();
        processSlowFallingQueue();
        processJailQueue();
        processLevelQueue();
        processResetQueues();
    }

    private void processArrowQueue() {
        ArrowAction action = arrowQueue.peek();
        if (action == null) return;

        Player player = Bukkit.getPlayer(action.getPlayerUUID());
        if (player == null || !player.isOnline()) {
            arrowQueue.poll();
            return;
        }

        if (live.getReset().isResetting(player) || game.isWinner(player)) return;

        arrowQueue.poll();
        live.getArrow().shootFaceArrow(player, action.getDonor());
    }

    private void processSlowFallingQueue() {
        SlowFallingAction action = slowFallingQueue.peek();
        if (action == null) return;

        Player player = Bukkit.getPlayer(action.getPlayerUUID());
        if (player == null || !player.isOnline()) {
            slowFallingQueue.poll();
            return;
        }

        if (live.getReset().isResetting(player) || game.isWinner(player)) return;

        slowFallingQueue.poll();
        live.getPotionEffects().setSlowFalling(player, action.getDuration(), action.getDonor());
    }

    private void processJailQueue() {
        JailAction action = jailQueue.peek();
        if (action == null) return;

        Player player = Bukkit.getPlayer(action.getPlayerUUID());
        if (player == null || !player.isOnline()) {
            jailQueue.poll();
            return;
        }

        // Mantém a regra existente: reset e winner bloqueiam o Jail sem remover a ação da fila.
        if (live.getReset().isResetting(player) || game.isWinner(player)) return;

        // Toda ação de Jail espera o jogador voltar à altura do spawn, inclusive se ele já está preso.
        if (!isAtCurrentSpawnHeight(player)) return;

        // No spawn, uma nova ação para quem já está preso aumenta o tempo como antes.
        if (live.getJail().isInJail(player)) {
            applyJailAction(player, action);
            return;
        }

        // Não consulta blocos durante a queda; só verifica o chão ao chegar à altura do spawn.
        Block blockAtFeet = player.getLocation().getBlock();
        Block blockUnder = blockAtFeet.getRelative(BlockFace.DOWN);
        Material floor = blockUnder.getType();
        Material feet = blockAtFeet.getType();

        boolean hasSafeGround = player.isOnGround() || floor.isSolid();
        boolean isUnsafeFloor = floor.isAir() || floor == Material.WATER || floor == Material.LAVA;
        boolean isInLiquid = feet == Material.WATER || feet == Material.LAVA;

        // Exige chão seguro: cair no buraco do spawn não libera o Jail.
        if (hasSafeGround && !isUnsafeFloor && !isInLiquid) {
            applyJailAction(player, action);
        }
    }

    private boolean isAtCurrentSpawnHeight(Player player) {
        int currentLevel = game.getActualLevel(player);
        int currentPhase = phase.getEffectivePhase(currentLevel);
        Location spawn = phase.getPhaseSpawn(currentPhase);
        if (spawn == null || spawn.getWorld() == null || !spawn.getWorld().equals(player.getWorld())) {
            return false;
        }

        // Usa o mesmo limite vertical da zona segura em Game.checkDropperCollision.
        return Math.abs(player.getLocation().getBlockY() - spawn.getBlockY()) <= 2.5;
    }

    private void applyJailAction(Player player, JailAction action) {
        jailQueue.poll();
        live.getJail().handleJail(player, action.getDuration(), action.getDonor());
    }

    private void processLevelQueue() {
        LevelAction action = levelQueue.peek();
        if (action == null) return;

        Player player = Bukkit.getPlayer(action.getPlayerUUID());
        if (player == null || !player.isOnline()) {
            levelQueue.poll();
            return;
        }

        if (live.getJail().isInJail(player) || live.getReset().isResetting(player)) return;

        if (!game.isWinner(player)) {
            levelQueue.poll();
            live.getLevels().modifyLevel(player, action.getQty(), action.getDonor());
            return;
        }

        // No winner, só ações de descer nível são permitidas; procure a primeira
        // ação negativa desse jogador sem retirar as ações positivas bloqueadas.
        if (action.getQty() < 0) {
            levelQueue.poll();
            live.getLevels().modifyLevel(player, action.getQty(), action.getDonor());
            return;
        }

        UUID playerUUID = player.getUniqueId();
        Iterator<LevelAction> iterator = levelQueue.iterator();
        while (iterator.hasNext()) {
            LevelAction queuedAction = iterator.next();
            if (queuedAction.getPlayerUUID().equals(playerUUID) && queuedAction.getQty() < 0) {
                iterator.remove();
                live.getLevels().modifyLevel(player, queuedAction.getQty(), queuedAction.getDonor());
                return;
            }
        }
    }

    private void processResetQueues() {
        // Cancelamentos são consumidos mesmo quando já não há reset ativo.
        CancelResetAction cancelAction = cancelResetQueue.poll();
        if (cancelAction != null) {
            Player player = Bukkit.getPlayer(cancelAction.getPlayerUUID());
            if (player != null && player.isOnline() && live.getReset().isResetting(player)) {
                live.getReset().cancelReset(player, false, cancelAction.getDonor());
            }
        }

        // Inícios de reset ficam aguardando enquanto o jogador estiver preso.
        UUID resetUUID = startResetQueue.peek();
        if (resetUUID == null) return;

        Player player = Bukkit.getPlayer(resetUUID);
        if (player == null || !player.isOnline()) {
            startResetQueue.poll();
        } else if (!live.getJail().isInJail(player)) {
            startResetQueue.poll();
            live.getReset().startReset(player);
        }
    }

    // Métodos públicos para enfileirar de forma segura (mesmo de threads externas)
    public void queueArrow(Player player) {
        queueArrow(player, "Viewer");
    }

    public void queueArrow(Player player, String donor) {
        if (game.isPlaying(player)) {
            arrowQueue.add(new ArrowAction(player.getUniqueId(), donor));
        }
    }

    public void queueSlowFalling(Player player, int duration) {
        queueSlowFalling(player, duration, "Viewer");
    }

    public void queueSlowFalling(Player player, int duration, String donor) {
        if (game.isPlaying(player)) {
            slowFallingQueue.add(new SlowFallingAction(player.getUniqueId(), duration, donor));
        }
    }

    public void queueJail(Player player, int duration, String donor) {
        if (game.isPlaying(player)) {
            jailQueue.add(new JailAction(player.getUniqueId(), duration, donor));
        }
    }

    public void queueLevelChange(Player player, int qty, String donor) {
        if (game.isPlaying(player)) {
            levelQueue.add(new LevelAction(player.getUniqueId(), qty, donor));
        }
    }

    public void queueStartReset(Player player) {
        if (game.isPlaying(player)) {
            startResetQueue.add(player.getUniqueId());
        }
    }

    public void queueCancelReset(Player player, String donor) {
        if (game.isPlaying(player)) {
            cancelResetQueue.add(new CancelResetAction(player.getUniqueId(), donor));
        }
    }

    // getters
    public Main getPlugin() {
        return plugin;
    }

    public Phase getPhase() {
        return phase;
    }

    public Game getGame() { return game; }

    public HologramManager getHologramManager() {
        return hologramManager;
    }

    public Live getLive() {
        return live;
    }

    public void setBossBarLoop(BossBarLoop bossBarLoop) {
        if (this.bossBarLoop != null) {
            this.bossBarLoop.stopLoop();
        }

        this.bossBarLoop = bossBarLoop;
        this.bossBarLoop.runTaskTimer(this.getPlugin(), 0L, 10L);
    }

    public void stopBossBarLoop() {
        if (bossBarLoop != null) {
            bossBarLoop.stopLoop();
            bossBarLoop = null;
        }
        hideAllBossBars();
    }

    public void updateBossBar(Player player, Component title, float progress) {
        UUID uuid = player.getUniqueId();
        BossBar bossBar = playerBossBars.get(uuid);
        if (bossBar == null) {
            bossBar = BossBar.bossBar(title, progress, BossBar.Color.PURPLE, BossBar.Overlay.PROGRESS);
            playerBossBars.put(uuid, bossBar);
            player.showBossBar(bossBar);
            return;
        }

        bossBar.name(title);
        bossBar.progress(progress);
    }

    public void hideBossBar(Player player) {
        BossBar bossBar = playerBossBars.remove(player.getUniqueId());
        if (bossBar != null) {
            player.hideBossBar(bossBar);
        }
    }

    public void hideAllBossBars() {
        for (Map.Entry<UUID, BossBar> entry : playerBossBars.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                player.hideBossBar(entry.getValue());
            }
        }
        playerBossBars.clear();
    }

    public void resetStatusPlayer(Player player) {
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setFireTicks(0);
    }
}
