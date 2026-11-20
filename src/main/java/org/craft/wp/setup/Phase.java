package org.craft.wp.setup;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.craft.wp.manager.GameManager;

import java.util.HashMap;
import java.util.Map;

public class Phase {

    private final GameManager gameManager;

    private final Map<Integer, Location> spawnCache = new HashMap<>();
    private final Map<Integer, Location> goalCache = new HashMap<>();

    public FileConfiguration getMapConfig() {
        return this.gameManager.getPlugin().getMapConfig();
    }

    public Phase(GameManager gameManager) {
        this.gameManager = gameManager;
        this.loadCache();
    }

    public void loadCache() {
        spawnCache.clear();
        goalCache.clear();
        int max = getMaxPhase();
        for (int i = 1; i <= max; i++) {
            Location spawn = this.loadSpawnFromConfig(i);
            if (spawn != null) spawnCache.put(i, spawn);

            Location goal = this.loadGoalFromConfig(i);
            if (goal != null) goalCache.put(i, goal);
        }
    }

    public void invalidateCache() {
        this.loadCache();
    }

    // set spawn
    public void setPhaseSpawn(int phase, Location location) {
        String path = "phase." + phase + ".spawn.";
        getMapConfig().set(path + "world", location.getWorld().getName());
        getMapConfig().set(path + "x", location.getX());
        getMapConfig().set(path + "y", location.getY());
        getMapConfig().set(path + "z", location.getZ());
        getMapConfig().set(path + "yaw", location.getYaw());
        getMapConfig().set(path + "pitch", location.getPitch());
        this.gameManager.getPlugin().saveMapConfig();
        this.invalidateCache();
    }

    // get spawn
    public Location getPhaseSpawn(int phase) {
        return spawnCache.get(phase);
    }

    // set goal
    public void setPhaseGoal(int phase, Location location) {
        String path = "phase." + phase + ".goal.";
        getMapConfig().set(path + "world", location.getWorld().getName());
        getMapConfig().set(path + "x", location.getBlockX());
        getMapConfig().set(path + "y", location.getBlockY());
        getMapConfig().set(path + "z", location.getBlockZ());
        this.gameManager.getPlugin().saveMapConfig();
        this.invalidateCache();
    }

    // get goal
    public Location getPhaseGoal(int phase) {
        return goalCache.get(phase);
    }

    public int getMaxPhase() {
        ConfigurationSection section = getMapConfig().getConfigurationSection("phase");
        if (section == null) return 1;

        int max = 1;
        for (String key : section.getKeys(false)) {
            try {
                int phaseNum = Integer.parseInt(key);
                if (phaseNum > max) max = phaseNum;
            } catch (NumberFormatException ignored) {}
        }
        return max;
    }

    public int getEffectivePhase(int level) {
        return Math.max(1, level);
    }

    private Location loadSpawnFromConfig(int phase) {
        String path = "phase." + phase + ".spawn.";
        FileConfiguration config = getMapConfig();
        if (!config.contains(path)) return null;

        String worldName = config.getString(path + "world");
        double x = config.getDouble(path + "x");
        double y = config.getDouble(path + "y");
        double z = config.getDouble(path + "z");
        float yaw = (float) config.getDouble(path + "yaw");
        float pitch = (float) config.getDouble(path + "pitch");

        return new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
    }

    private Location loadGoalFromConfig(int phase) {
        String path = "phase." + phase + ".goal.";
        FileConfiguration config = getMapConfig();
        if (!config.contains(path)) return null;

        String worldName = config.getString(path + "world");
        int x = config.getInt(path + "x");
        int y = config.getInt(path + "y");
        int z = config.getInt(path + "z");

        return new Location(Bukkit.getWorld(worldName), x, y, z);
    }

}
