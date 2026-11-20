package org.craft.wp.tasks;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.craft.wp.manager.GameManager;

import java.time.Duration;

public class ResetTimer extends BukkitRunnable {

    private static final String RESET_ICON = "\uE002";
    private static final Key ICON_FONT = Key.key("livedropper", "messages");

    private final GameManager gameManager;
    private final Player player;
    private int secondsLeft;

    public ResetTimer(GameManager gameManager, Player player, int totalSeconds) {
        this.gameManager = gameManager;
        this.player = player;
        this.secondsLeft = totalSeconds;
    }

    @Override
    public void run() {
        if (!player.isOnline()) {
            cancel();
            return;
        }

        if (secondsLeft <= 0) {
            // completa o reset para a fase 1
            gameManager.getLive().getReset().completeReset(player);
            cancel();
            return;
        }

        NamedTextColor color = (secondsLeft <= 10)
                ? NamedTextColor.RED
                : (secondsLeft <= 30) ? NamedTextColor.YELLOW : NamedTextColor.GREEN;

        Component countdown = Component.text(
                Integer.toString(secondsLeft),
                color,
                TextDecoration.BOLD
        ).append(Component.text(RESET_ICON, NamedTextColor.WHITE).font(ICON_FONT));

        Component subtitle = Component.text("SALVE DO RESET", NamedTextColor.RED, TextDecoration.BOLD);

        player.showTitle(
                Title.title(
                        countdown,
                        subtitle,
                        Title.Times.times(Duration.ZERO, Duration.ofMillis(1250), Duration.ZERO)
                )
        );

        if (secondsLeft <= 10) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f);
        } else {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        }

        secondsLeft--;
    }
}
