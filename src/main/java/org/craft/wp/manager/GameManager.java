package org.craft.wp.manager;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.craft.wp.Main;
import org.craft.wp.interactions.Live;
import org.craft.wp.setup.Phase;
import org.craft.wp.tasks.ActionBarLoop;
import org.craft.wp.utils.HologramManager;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class GameManager {

    private final Main plugin;

    private final Phase phase;

    private final Live live;

    private final Game game;

    private final HologramManager hologramManager;

    private ActionBarLoop actionBarLoop;

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

        public SlowFallingAction(UUID playerUUID, int duration) {
            this.playerUUID = playerUUID;
            this.duration = duration;
        }

        public UUID getPlayerUUID() { return playerUUID; }
        public int getDuration() { return duration; }
    }

    // interações e up down level
    private final ConcurrentLinkedQueue<UUID> arrowQueue = new ConcurrentLinkedQueue<>();
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

        // processador da fila (Flush) que roda a cada 4 ticks (~200ms)
        startFlushProcessor();
    }

    private void startFlushProcessor() {
        flushTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // Processa Flecha
            UUID arrowUuid = arrowQueue.peek();
            if (arrowUuid != null) {
                Player player = Bukkit.getPlayer(arrowUuid);
                if (player == null || !player.isOnline()) {
                    arrowQueue.poll();
                } else if (!live.getReset().isResetting(player) && !game.isWinner(player)) {
                    arrowQueue.poll();
                    live.getArrow().shootFaceArrow(player);
                }
            }

            SlowFallingAction slowFallingAction = slowFallingQueue.peek();
            if (slowFallingAction != null) {
                Player player = Bukkit.getPlayer(slowFallingAction.getPlayerUUID());
                if (player == null || !player.isOnline()) {
                    slowFallingQueue.poll();
                } else if (!live.getReset().isResetting(player) && !game.isWinner(player)) {
                    slowFallingQueue.poll();
                    live.getPotionEffects().setSlowFalling(player, slowFallingAction.getDuration());
                }
            }

            // Processa jaula
            JailAction jailAction = jailQueue.peek();
            if (jailAction != null) {
                Player player = Bukkit.getPlayer(jailAction.getPlayerUUID());

                if (player == null || !player.isOnline()) {
                    jailQueue.poll();
                } else if (!live.getReset().isResetting(player) && !game.isWinner(player)) {
                    boolean isActuallyOnGround = player.isOnGround() ||
                            player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType().isSolid();

                    Block blockUnder = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
                    Block blockAtFeet = player.getLocation().getBlock();

                    // Verifica se o bloco debaixo ou o bloco onde o player está é Ar, Água ou Lava
                    boolean isUnsafeSurface = blockUnder.getType().isAir()
                            || blockUnder.getType() == Material.WATER
                            || blockUnder.getType() == Material.LAVA;

                    boolean isImmersedInLiquid = blockAtFeet.getType() == Material.WATER
                            || blockAtFeet.getType() == Material.LAVA;

                    // Se ele já estiver preso por algum motivo lógico anterior, processa direto
                    if (live.getJail().isInJail(player)) {
                        jailQueue.poll();
                        live.getJail().handleJail(player, jailAction.getDuration(), jailAction.getDonor());
                    }
                    // Se ele estiver em solo firme e totalmente fora de água/lava, libera a prisão
                    else if (isActuallyOnGround && !isUnsafeSurface && !isImmersedInLiquid) {
                        jailQueue.poll();
                        live.getJail().handleJail(player, jailAction.getDuration(), jailAction.getDonor());
                    }
                }
            }

            // Processa Nível
            if (!levelQueue.isEmpty()) {
                LevelAction levelAction = levelQueue.peek();
                if (levelAction != null) {
                    Player player = Bukkit.getPlayer(levelAction.getPlayerUUID());

                    if (player == null || !player.isOnline()) {
                        levelQueue.poll();
                    } else if (live.getJail().isInJail(player)) {
                    } else if (!live.getReset().isResetting(player)) {
                        if (game.isWinner(player)) {
                            // Se o topo for qty < 0, processa direto
                            if (levelAction.getQty() < 0) {
                                levelQueue.poll();
                                live.getLevels().modifyLevel(player, levelAction.getQty(), levelAction.getDonor());
                            } else {
                                // Se o topo for qty > 0 (bloqueado no Winner), percorre a fila
                                // via Iterator para buscar e processar o primeiro 'qty < 0'
                                Iterator<LevelAction> iterator = levelQueue.iterator();
                                while (iterator.hasNext()) {
                                    LevelAction action = iterator.next();
                                    Player p = Bukkit.getPlayer(action.getPlayerUUID());

                                    if (p != null && p.getUniqueId().equals(player.getUniqueId()) && action.getQty() < 0) {
                                        iterator.remove(); // Remove o elemento encontrado
                                        live.getLevels().modifyLevel(player, action.getQty(), action.getDonor());
                                        break; // Processa apenas um por tick para manter o cadenciamento
                                    }
                                }
                            }
                        } else {
                            levelQueue.poll();
                            live.getLevels().modifyLevel(player, levelAction.getQty(), levelAction.getDonor());
                        }
                    }
                }
            }

            // reset
            CancelResetAction cancelAction = cancelResetQueue.poll();
            if (cancelAction != null) {
                Player player = Bukkit.getPlayer(cancelAction.getPlayerUUID());
                if (player != null && player.isOnline() && live.getReset().isResetting(player)) {
                    live.getReset().cancelReset(player, false, cancelAction.getDonor());
                }
            }

            UUID resetUuid = startResetQueue.peek();
            if (resetUuid != null) {
                Player player = Bukkit.getPlayer(resetUuid);
                if (player == null || !player.isOnline()) {
                    startResetQueue.poll();
                } else if (!live.getJail().isInJail(player)) {
                    // Se já estiver em reset, ele reinicia o timer
                    // e se estiver no Winner ou normal, o startReset cancela a vitória e aplica o reset
                    startResetQueue.poll();
                    live.getReset().startReset(player);
                }
            }
        }, 0L, 5L);
    }

    // Métodos públicos para enfileirar de forma segura (mesmo de threads externas)
    public void queueArrow(Player player) {
        if (game.isPlaying(player)) {
            arrowQueue.add(player.getUniqueId());
        }
    }

    public void queueSlowFalling(Player player, int duration) {
        if (game.isPlaying(player)) {
            slowFallingQueue.add(new SlowFallingAction(player.getUniqueId(), duration));
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

    public ActionBarLoop getBossBarTask() {
        return actionBarLoop;
    }

    //setters
    /**
     * Define e inicia um novo bossbar.
     *
     * @param bossBar
     */
    public void setBossBar(ActionBarLoop bossBar) {
        if (this.actionBarLoop != null) {
            this.actionBarLoop.stopLoop();
        }

        this.actionBarLoop = bossBar;
        this.actionBarLoop.runTaskTimer(this.getPlugin(), 0L, 10L);
    }

    public void resetStatusPlayer(Player player) {
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setFireTicks(0);
    }
}
