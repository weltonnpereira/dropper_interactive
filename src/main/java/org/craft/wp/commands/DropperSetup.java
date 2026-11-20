package org.craft.wp.commands;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.craft.wp.manager.GameManager;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;

public class DropperSetup implements CommandExecutor {

    private final GameManager gameManager;

    public DropperSetup(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public String cmd1 = "setphase";

    public String cmd2 = "dp";

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (cmd.getName().equalsIgnoreCase(cmd1)) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cApenas jogadores podem definir fases.");
                return true;
            }

            if (!player.isOp()) {
                player.sendMessage("§cVocê não tem permissão para usar este comando.");
                return true;
            }

            if (args.length != 2) {
                player.sendMessage("§cUse: /setphase <numero> <spawn|goal>");
                return true;
            }

            try {
                int phase = Integer.parseInt(args[0]);
                if (phase <= 0) {
                    player.sendMessage("§cO número da fase deve ser maior que 0.");
                    return true;
                }

                String type = args[1].toLowerCase();
                switch (type) {
                    case "spawn" -> {
                        gameManager.getPhase().setPhaseSpawn(phase, player.getLocation());
                        player.sendMessage("§a[Dropper] §fSpawn da fase §e" + phase + " §fsalvo!");
                    }
                    case "goal" -> {
                        gameManager.getPhase().setPhaseGoal(phase, player.getLocation());
                        player.sendMessage("§a[Dropper] §fDestino (água) da fase §e" + phase + " §fsalvo!");
                    }
                    default -> {
                        player.sendMessage("§cTipo inválido! Use 'spawn' para o início ou 'goal' para a água.");
                    }
                }
            } catch (NumberFormatException e) {
                player.sendMessage("§cO número da fase deve ser um valor inteiro.");
            }

            return true;
        }

        if (cmd.getName().equalsIgnoreCase(cmd2)) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cApenas jogadores podem usar este comando.");
                return true;
            }

            if (!player.isOp()) {
                player.sendMessage("§cSem permissão.");
                return true;
            }

            if (args.length == 0) {
                player.sendMessage("§cUse: /dp <nome_do_mundo>");
                return true;
            }

            String targetWorldName = args[0];
            World targetWorld = Bukkit.getWorld(targetWorldName);

            if (targetWorld == null) {
                File worldFolder = new File(Bukkit.getWorldContainer(), targetWorldName);
                if (!worldFolder.exists() || !worldFolder.isDirectory()) {
                    player.sendMessage("§cA pasta do mundo '" + targetWorldName + "' não existe na raiz do servidor!");
                    return true;
                }

                player.sendMessage("§eCarregando novo mundo §b" + targetWorldName + "§e...");
                targetWorld = new WorldCreator(targetWorldName).createWorld();

                List<String> extraWorlds = gameManager.getPlugin().getConfig().getStringList("extra-worlds");
                if (!extraWorlds.contains(targetWorldName)) {
                    extraWorlds.add(targetWorldName);
                    gameManager.getPlugin().getConfig().set("extra-worlds", extraWorlds);
                    gameManager.getPlugin().saveConfig();
                }
            }

            if (targetWorld != null) {
                player.teleport(targetWorld.getSpawnLocation());
                player.sendMessage("§aTeleportado para o mundo §b" + targetWorldName + "§a!");
            } else {
                player.sendMessage("§cFalha ao carregar o mundo " + targetWorldName + ".");
            }
            return true;
        }

        return false;
    }
}
