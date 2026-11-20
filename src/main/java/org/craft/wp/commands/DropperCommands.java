package org.craft.wp.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.craft.wp.manager.GameManager;
import org.jetbrains.annotations.NotNull;

public class DropperCommands implements CommandExecutor {

    private final GameManager gameManager;

    public DropperCommands(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public String cmd1 = "dpstart";

    public String cmd2 = "dpstop";

    public String cmd3 = "checkpoint";

    public String cmd4 = "removeholograms";

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (cmd.getName().equalsIgnoreCase(cmd1)) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cApenas jogadores podem usar este comando.");
                return true;
            }

            Player player = (Player) sender;

            gameManager.getGame().startDropper(player);
            return true;
        }

        if (cmd.getName().equalsIgnoreCase(cmd2)) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cApenas jogadores podem usar este comando.");
                return true;
            }

            Player player = (Player) sender;

            gameManager.getGame().stopDropper(player);
            return true;
        }

        if (cmd.getName().equalsIgnoreCase(cmd3)) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cApenas jogadores podem voltar ao checkpoint.");
                return true;
            }

            if (!player.isOp()) {
                player.sendMessage("§cVocê não tem permissão para usar este comando.");
                return true;
            }

            gameManager.getGame().returnCheckpoint(player, true);

            return true;
        }

        if (cmd.getName().equalsIgnoreCase(cmd4)) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cApenas jogadores podem voltar ao checkpoint.");
                return true;
            }

            if (!player.isOp()) {
                player.sendMessage("§cVocê não tem permissão para usar este comando.");
                return true;
            }

            gameManager.getHologramManager().removeAllHolograms();
            sender.sendMessage("§a[Parkour] Todos os hologramas de fases foram removidos do mundo com sucesso!");
            return true;
        }

        return false;
    }
}
