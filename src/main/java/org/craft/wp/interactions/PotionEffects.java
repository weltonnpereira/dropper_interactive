package org.craft.wp.interactions;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.craft.wp.manager.GameManager;
import org.craft.wp.utils.ActionBarNotifier;

public class PotionEffects {

    private final GameManager gameManager;

    public PotionEffects(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    /**
     * Aplica ou acumula o efeito de Queda Lenta (Slow Falling) para o jogador.
     *
     * @param player  O jogador que receberá o efeito
     * @param seconds Quantidade de segundos a serem somados
     */w
    public void setSlowFalling(Player player, int seconds, String donor) {
        if (player == null || !player.isOnline()) return;

        if (!gameManager.getGame().isPlaying(player)) {
            return;
        }

        int ticksToAdd = seconds * 20;
        int finalDurationTicks = ticksToAdd;

        // Verifica se o player já tem o efeito ativo para acumular o tempo
        PotionEffect currentEffect = player.getPotionEffect(PotionEffectType.SLOW_FALLING);
        if (currentEffect != null) {
            int remainingTicks = currentEffect.getDuration();
            finalDurationTicks = remainingTicks + ticksToAdd;
        }

        // Opcional: Limite máximo de segurança (ex: 20 segundos / 400 ticks)
        // para o chat não stackar infinitamente e travar a gameplay.
        int maxAllowedTicks = 20 * 20;
        if (finalDurationTicks > maxAllowedTicks) {
            finalDurationTicks = maxAllowedTicks;
        }

        // Cria e aplica o efeito atualizado
        PotionEffect slowFalling = new PotionEffect(
                PotionEffectType.SLOW_FALLING,
                finalDurationTicks,
                0,     // Nível 0 (Slow Falling I)
                false, // Ambient
                true,  // Particles
                true   // Icon
        );

        player.addPotionEffect(slowFalling, true);
        ActionBarNotifier.send(player, donor, "usou queda lenta");

        // Feedback sonoro gratificante para o chat/streamer
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
    }

}
