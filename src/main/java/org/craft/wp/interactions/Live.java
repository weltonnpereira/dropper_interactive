package org.craft.wp.interactions;

import org.bukkit.entity.Arrow;
import org.craft.wp.manager.GameManager;

public class Live {

    private final Levels levels;
    private final Reset reset;
    private final ArrowShoot arrow;
    private final Jail jail;
    private final PotionEffects potionEffects;

    public Live(GameManager gameManager) {
        this.levels = new Levels(gameManager);
        this.reset = new Reset(gameManager);
        this.arrow = new ArrowShoot(gameManager);
        this.jail = new Jail(gameManager);
        this.potionEffects = new PotionEffects(gameManager);
    }

    // getters
    public Levels getLevels() {
        return levels;
    }

    public Reset getReset() {
        return reset;
    }

    public ArrowShoot getArrow() {
        return arrow;
    }

    public Jail getJail() {
        return jail;
    }

    public PotionEffects getPotionEffects() {
        return potionEffects;
    }

}
