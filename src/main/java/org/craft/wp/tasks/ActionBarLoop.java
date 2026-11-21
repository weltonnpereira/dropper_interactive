package org.craft.wp.tasks;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.craft.wp.manager.GameManager;

import java.util.Locale;

public class ActionBarLoop extends BukkitRunnable {

    private final GameManager gameManager;

    private boolean shouldContinue = true;

    private static final String FEATHER_ICON = "\uE001";
    private static final Key FEATHER_FONT = Key.key("livedropper", "messages");

    public ActionBarLoop(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public void stopLoop() {
        shouldContinue = false;
    }

    @Override
    public void run() {
        if (!shouldContinue) {
            this.cancel();
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!gameManager.getGame().isPlaying(player)) {
                continue; // Pula quem não está jogando sem cancelar o loop para os outros
            }

            int actualLevel = gameManager.getGame().getActualLevel(player);
            int maxLevel = gameManager.getPhase().getMaxPhase();

            String color = (actualLevel < 1) ? "§c§l" : "§a§l";
            String baseText = "§e§lDROPPER §7| §fFase Atual: " + color + actualLevel + "§7/§a" + maxLevel;

            // Verifica se o jogador está com o efeito de Queda Lenta ativo
            PotionEffect slowFalling = player.getPotionEffect(PotionEffectType.SLOW_FALLING);
            if (slowFalling != null && slowFalling.getDuration() > 0) {
                double secondsLeft = slowFalling.getDuration() / 20.0;
                String formattedTime = String.format(Locale.US, "%.1fs", secondsLeft);

                // Adiciona o separador, o ícone da pena (em branco §f para manter a cor original) e o tempo
                baseText += " §7| §f" + FEATHER_ICON + " §b" + formattedTime;
            }

            player.sendActionBar(Component.text(baseText).font(FEATHER_FONT));
        }
    }
}
