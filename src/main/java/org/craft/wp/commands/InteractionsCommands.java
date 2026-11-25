package org.craft.wp.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.craft.wp.manager.GameManager;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class InteractionsCommands implements CommandExecutor {

    private final GameManager gameManager;

    public InteractionsCommands(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public String cmd1 = "shootarrow";

    public String cmd2 = "modifylevel";

    public String cmd3 = "reset";

    public String cmd4 = "removereset";

    public String cmd5 = "slowfall";

//    public String cmd5 = "jail";

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (cmd.getName().equalsIgnoreCase(cmd1)) {
            if (args.length < 1) {
                sender.sendMessage("§cUse no console/S2E: /" + cmd1 + " <player> [doador]");
                return true;
            }

            Player target = gameManager.getPlugin().getServer().getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§cJogador não encontrado ou offline.");
                return true;
            }

            String donor = args.length > 1
                    ? String.join(" ", Arrays.copyOfRange(args, 1, args.length))
                    : "Viewer";
            gameManager.queueArrow(target, donor);

            return true;
        }

        if (cmd.getName().equalsIgnoreCase(cmd2)) {
            if (args.length < 2) {
                sender.sendMessage("§cUse no console/S2E: /" + cmd2 + "<player> <quantidade> [doador]");
                return true;
            }

            Player target = gameManager.getPlugin().getServer().getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§cJogador não encontrado ou offline.");
                return true;
            }

            try {
                int qtd = Integer.parseInt(args[1]);

                String donor = (args.length >= 3) ? args[2] : "Viewer";

                gameManager.queueLevelChange(target, qtd, donor);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cA quantidade deve ser um número inteiro (positivo ou negativo).");
            }

            return true;
        }

        if (cmd.getName().equalsIgnoreCase(cmd3)) {
            if (args.length != 1) {
                sender.sendMessage("§cUse no console/S2E: /" + cmd3 + " <player>");
                return true;
            }

            Player target = gameManager.getPlugin().getServer().getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§cJogador não encontrado ou offline.");
                return true;
            }

            gameManager.queueStartReset(target);

            return true;
        }

        if (cmd.getName().equalsIgnoreCase(cmd4)) {
            if (args.length < 1) {
                sender.sendMessage("§cUse no console/S2E: /" + cmd4 + " <player>");
                return true;
            }

            Player target = gameManager.getPlugin().getServer().getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§cJogador não encontrado ou offline.");
                return true;
            }

            String donor = (args.length > 1) ? "§fO §a" + args[1] + " §fte salvou!" : "§fSeus viewers te salvaram!";

            gameManager.queueCancelReset(target, donor);

            return true;
        }

        if (cmd.getName().equalsIgnoreCase(cmd5)) {
            if (args.length < 2) {
                sender.sendMessage("§cUse no console/S2E: /" + cmd5 + " <player> <qty> [doador]");
                return true;
            }

            Player target = gameManager.getPlugin().getServer().getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§cJogador não encontrado ou offline.");
                return true;
            }

            try {
                int time = Integer.parseInt(args[1]);
                if (time <= 0) {
                    sender.sendMessage("§cO tempo deve ser um número positivo.");
                    return true;
                }

                String donor = args.length > 2
                        ? String.join(" ", Arrays.copyOfRange(args, 2, args.length))
                        : "Viewer";
                gameManager.queueSlowFalling(target, time, donor);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cO tempo deve ser um número inteiro válido.");
            }

            return true;
        }

//        if (cmd.getName().equalsIgnoreCase(cmd5)) {
//            if (args.length < 2) {
//                sender.sendMessage("§cUse: /" + cmd5 + " <player> <arrest/release> [time/donor] [donor]");
//                return true;
//            }
//
//            Player target = gameManager.getPlugin().getServer().getPlayer(args[0]);
//            if (target == null) {
//                sender.sendMessage("§cJogador não encontrado ou offline.");
//                return true;
//            }
//
//            String action = args[1].toLowerCase();
//            switch (action) {
//                case "arrest" -> {
//                    if (args.length < 3) {
//                        sender.sendMessage("§cUse: /" + cmd5 + " <player> arrest <tempo_segundos> [doador]");
//                        return true;
//                    }
//
//                    try {
//                        int time = Integer.parseInt(args[2]);
//                        if (time <= 0) {
//                            sender.sendMessage("§cO tempo deve ser um número positivo.");
//                            return true;
//                        }
//
//                        String donor = (args.length >= 4) ? args[3] : "";
//
//                        gameManager.queueJail(target, time, donor);
//                    } catch (NumberFormatException e) {
//                        sender.sendMessage("§cO tempo deve ser um número inteiro válido.");
//                    }
//                }
//                case "release" -> {
//                    String donor = (args.length >= 3) ? args[2] : null;
//                    gameManager.getLive().getJail().release(target, donor);
//                }
//                default -> sender.sendMessage("§cAção inválida! Use 'arrest' ou 'release'.");
//            }
//
//            return true;
//        }

        return false;
    }
}
