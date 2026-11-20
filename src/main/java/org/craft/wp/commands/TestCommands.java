package org.craft.wp.commands;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.craft.wp.manager.GameManager;
import org.jetbrains.annotations.NotNull;

public class TestCommands implements CommandExecutor {

    public String cmdChaos = "pkchaos";

    public String cmdChaosWinnerTrap = "pkchaoswinnertrap";

    public String cmdPkResteJail = "pkrestejail";

    private final GameManager gameManager;

    public TestCommands(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (cmd.getName().equalsIgnoreCase(cmdChaos)) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Apenas jogadores podem testar o caos.");
                return true;
            }

            Player player = (Player) sender;
            if (!gameManager.getGame().isPlaying(player)) {
                player.sendMessage("§c[Parkour] Você precisa iniciar o parkour (/pkstart) antes de testar o caos!");
                return true;
            }

            player.sendMessage("§4§l[☢️ CAOS SUPREMO] §cSimulando LIVE VIRAL COM SPAWN MASSIVO DE PRESENTES!");

            // Task que simula uma enxurrada assíncrona de presentes em momentos diferentes
            new BukkitRunnable() {
                int step = 0;

                @Override
                public void run() {
                    if (!player.isOnline() || !gameManager.getGame().isPlaying(player)) {
                        cancel();
                        return;
                    }

                    switch (step) {
                        case 0:
                            player.sendMessage("§7[Chaos 0s] §bMETRALHADORA DE FLECHAS: 10 flechas disparadas juntas!");
                            for (int i = 0; i < 10; i++) {
                                gameManager.queueArrow(player);
                            }
                            break;

                        case 2: // 4 ticks (~0.2s)
                            player.sendMessage("§7[Chaos 0.2s] §aSpam de níveis: +5 níveis e -2 níveis simultâneos!");
                            gameManager.queueLevelChange(player, 5, "MegaFan_01");
                            gameManager.queueLevelChange(player, -2, "Troll_99");
                            break;

                        case 4: // 8 ticks (~0.4s)
                            player.sendMessage("§7[Chaos 0.4s] §cO PRIMEIRO HATER MANDOU RESET! Streamer vai pro Nível 1.");
                            gameManager.queueStartReset(player);
                            break;

                        case 6: // 12 ticks (~0.6s) - Durante o reset total
                            player.sendMessage("§7[Chaos 0.6s] §eO chat entra em pânico! Mandando mais 5 flechas e subida de nível (deve congelar na fila).");
                            gameManager.queueArrow(player);
                            gameManager.queueArrow(player);
                            gameManager.queueLevelChange(player, 3, "DesesperadoVIP");
                            break;

                        case 8: // 16 ticks (~0.8s) - Outro hater tenta resetar de novo por cima
                            player.sendMessage("§7[Chaos 0.8s] §4OUTRO HATER EMPILHA OUTRO RESET! O timer de 60s deve renovar.");
                            gameManager.queueStartReset(player);
                            break;

                        case 12: // 24 ticks (~1.2s)
                            player.sendMessage("§7[Chaos 1.2s] §dDois fãs tentam salvar ao mesmo tempo!");
                            gameManager.queueCancelReset(player, "Salvador_A");
                            gameManager.queueCancelReset(player, "Salvador_B");
                            break;

                        case 16: // 32 ticks (~1.6s) - Pós-salvamento imediato
                            player.sendMessage("§7[Chaos 1.6s] §bLiberou a fila! Verificando se o flush dispara tudo perfeitamente.");
                            gameManager.queueArrow(player);
                            gameManager.queueLevelChange(player, 1, "FimDoCaos");
                            cancel(); // Fim da simulação caótica
                            break;
                    }
                    step++;
                }
            }.runTaskTimer(gameManager.getPlugin(), 0L, 2L); // Ação frenética a cada 2 ticks

            return true;
        }

        if (cmd.getName().equalsIgnoreCase(cmdChaosWinnerTrap)) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Apenas jogadores podem rodar o teste de armadilha.");
                return true;
            }

            Player player = (Player) sender;
            if (!gameManager.getGame().isPlaying(player)) {
                player.sendMessage("§c[Parkour] Você precisa iniciar o parkour (/pkstart) antes!");
                return true;
            }

            player.sendMessage("§d§l[CHAOS WINNER TRAP] §eTestando bloqueio de topo de fila no modo Winner...");

            // 1. Força o jogador a ir para a fase máxima e entrar em isWinner
            int maxPhase = gameManager.getPhase().getMaxPhase();
            Location maxLoc = gameManager.getPhase().getPhaseSpawn(maxPhase);
            if (maxLoc != null) {
                gameManager.resetStatusPlayer(player);
                player.teleport(maxLoc);
                gameManager.playerLevels.put(player.getUniqueId(), maxPhase);
                gameManager.inWinnerMode.put(player.getUniqueId(), true);

                org.craft.wp.tasks.WinnerTask task = new org.craft.wp.tasks.WinnerTask(gameManager, player, 10);
                gameManager.activeWinnerTasks.put(player.getUniqueId(), task.runTaskTimer(gameManager.getPlugin(), 0L, 20L));
            }

            new org.bukkit.scheduler.BukkitRunnable() {
                int step = 0;

                @Override
                public void run() {
                    if (!player.isOnline() || !gameManager.getGame().isPlaying(player)) {
                        cancel();
                        return;
                    }

                    switch (step) {
                        case 1: // ~0.25s: Dispara o Reset enquanto está em Winner
                            player.sendMessage("§7[Test 0.25s] §c1. Reset disparado! (Jogador vai pro Nível 1, Winner cancelado temporariamente).");
                            gameManager.queueStartReset(player);
                            break;

                        case 2: // ~0.50s: Enquanto em RESET, enfileira SUBIR (topo) e logo em seguida DESCER (atrás)
                            player.sendMessage("§7[Test 0.50s] §e2. Enfileirando +2 Níveis (TOPO) e logo depois -1 Nível (ATRÁS).");
                            gameManager.queueLevelChange(player, 2, "Fã_Do_Topo");
                            gameManager.queueLevelChange(player, -1, "Hater_Do_Fundo");
                            break;

                        case 4: // ~1.00s: Cancela o reset, voltando para a fase máxima e REATIVANDO o isWinner
                            player.sendMessage("§7[Test 1.00s] §a3. Reset Cancelado! Jogador volta para o Nível Máximo (isWinner = TRUE).");
                            player.sendMessage("§b§l>>> O Flush agora DEVE pular o +2 e processar o -1 via Iterator! <<<");
                            gameManager.queueCancelReset(player, "Salvador_VIP");
                            cancel();
                            break;
                    }
                    step++;
                }
            }.runTaskTimer(gameManager.getPlugin(), 0L, 5L);

            return true;
        }

        if (cmd.getName().equalsIgnoreCase(cmdPkResteJail)) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Apenas jogadores podem testar o estresse da prisão.");
                return true;
            }

            Player player = (Player) sender;
            if (!gameManager.getGame().isPlaying(player)) {
                player.sendMessage("§c[Parkour] Você precisa iniciar o parkour (/pkstart) antes de testar a prisão!");
                return true;
            }

            player.sendMessage("§4§l[PRISÃO APOCALÍPTICA] §cSimulando o PIOR CENÁRIO: Prisão em meio a água, saltos, flechas e spam de chat!");

            // Task de estresse frenético focada na prisão e interações simultâneas
            new org.bukkit.scheduler.BukkitRunnable() {
                int step = 0;

                @Override
                public void run() {
                    if (!player.isOnline() || !gameManager.getGame().isPlaying(player)) {
                        cancel();
                        return;
                    }

                    switch (step) {
                        case 0:
                            player.sendMessage("§7[Estresse 0s] §e10 Doadores tentam prender o streamer ao mesmo tempo!");
                            for (int i = 0; i < 10; i++) {
                                gameManager.queueJail(player, 10, "Hater_" + i);
                            }
                            break;

                        case 2: // ~0.2s
                            player.sendMessage("§7[Estresse 0.2s] §bChuva de flechas e nível enchendo a fila enquanto ele tenta escapar!");
                            for (int i = 0; i < 5; i++) {
                                gameManager.queueArrow(player);
                            }
                            gameManager.queueLevelChange(player, 4, "Fanzaço");
                            break;

                        case 5: // ~0.5s
                            player.sendMessage("§7[Estresse 0.5s] §cUm hater manda reset bem na hora que a prisão quer fechar!");
                            gameManager.queueStartReset(player);
                            // Mais doações de prisão acumulando na fila
                            gameManager.queueJail(player, 15, "SuperTroll");
                            break;

                        case 8: // ~0.8s
                            player.sendMessage("§7[Estresse 0.8s] §eO chat se divide: tentam cancelar o reset e mudar níveis freneticamente.");
                            gameManager.queueCancelReset(player, "HeroiDoChat");
                            gameManager.queueLevelChange(player, -2, "Zoeiro");
                            gameManager.queueJail(player, 5, "Inquisidor");
                            break;

                        case 12: // ~1.2s
                            player.sendMessage("§7[Estresse 1.2s] §aLiberando tudo para o Flush processar o caos acumulado!");
                            // Dispara mais um lote de prisões e flechas para testar o esvaziamento da fila (Flush)
                            for (int i = 0; i < 3; i++) {
                                gameManager.queueJail(player, 8, "SpamDonator_" + i);
                                gameManager.queueArrow(player);
                            }
                            cancel(); // Fim do script de injeção caótica, agora o FlushProcessor assume a pressão total
                            break;
                    }
                    step++;
                }
            }.runTaskTimer(gameManager.getPlugin(), 0L, 3L);

            return true;
        }

        return false;
    }
}
