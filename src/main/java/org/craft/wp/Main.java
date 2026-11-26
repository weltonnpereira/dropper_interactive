package org.craft.wp;

import org.bukkit.Bukkit;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.craft.wp.commands.InteractionsCommands;
import org.craft.wp.commands.DropperCommands;
import org.craft.wp.commands.DropperSetup;
import org.craft.wp.listeners.Auxiliaries;
import org.craft.wp.listeners.DropperListener;
import org.craft.wp.manager.GameManager;
import org.craft.wp.network.LicenseChecker;

import java.io.File;
import java.io.IOException;
import java.util.List;

public final class Main extends JavaPlugin {

    private GameManager gameManager;
    private FileConfiguration config;
    private File configFile;

    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();

        getLogger().info("Ligando Plugin...");

        saveDefaultConfig();

        String licenseKey = getConfig().getString("license-key", "");

        if (licenseKey == null || licenseKey.trim().isEmpty() || licenseKey.equalsIgnoreCase("INSERT-YOUR-KEY-HERE")) {
            String box = "==================================================";
            getLogger().severe(box);
            getLogger().severe("A CHAVE DE LICENÇA ESTÁ AUSENTE NO config.yml!");
            getLogger().severe("Por favor, insira sua chave de licença no arquivo config.yml.");
            getLogger().severe("Após salvar, reinicie o servidor para aplicar a chave.");
            getLogger().severe(box);

            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        boolean isValid = LicenseChecker.verifyLicense(licenseKey);
        if (!isValid) {
            String box = "==================================================";
            getLogger().severe(box);
            getLogger().severe("FALHA NA AUTENTICACAO DA LICENCA!");
            getLogger().severe(" O plugin nao pode ser ativado.");
            getLogger().severe(box);

            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        Bukkit.getConsoleSender().sendMessage("Licenca autenticada com sucesso! Inicializando...");

        loadExtraWorlds();

        if(!this.getDataFolder().exists()) {
            this.getDataFolder().mkdir();
        }

        this.configFile = new File(this.getDataFolder(), "map.yaml");
        if (!this.configFile.exists()) {
            try {
                this.configFile.createNewFile();
            } catch (IOException e) {
                this.getLogger().severe("Não foi possível criar o arquivo map.yaml!");
            }
        }
        this.config = YamlConfiguration.loadConfiguration(this.configFile);

        getLogger().info("Ligando Plugin");

        this.gameManager = new GameManager(this);

        DropperSetup dropperSetup = new DropperSetup(gameManager);
        this.getCommand(dropperSetup.cmd1).setExecutor(dropperSetup);
        this.getCommand(dropperSetup.cmd2).setExecutor(dropperSetup);

        DropperCommands dropperCommands = new DropperCommands(gameManager);
        this.getCommand(dropperCommands.cmd1).setExecutor(dropperCommands);
        this.getCommand(dropperCommands.cmd2).setExecutor(dropperCommands);
        this.getCommand(dropperCommands.cmd3).setExecutor(dropperCommands);
        this.getCommand(dropperCommands.cmd4).setExecutor(dropperCommands);
        InteractionsCommands interactionsCommands = new InteractionsCommands(gameManager);
        this.getCommand(interactionsCommands.cmd1).setExecutor(interactionsCommands);
        this.getCommand(interactionsCommands.cmd2).setExecutor(interactionsCommands);
        this.getCommand(interactionsCommands.cmd3).setExecutor(interactionsCommands);
        this.getCommand(interactionsCommands.cmd4).setExecutor(interactionsCommands);
        this.getCommand(interactionsCommands.cmd5).setExecutor(interactionsCommands);

//        TestCommands testCommands = new TestCommands(gameManager);
//        this.getCommand(testCommands.cmdChaos).setExecutor(testCommands);
//        this.getCommand(testCommands.cmdChaosWinnerTrap).setExecutor(testCommands);
//        this.getCommand(testCommands.cmdPkResteJail).setExecutor(testCommands);

        this.getServer().getPluginManager().registerEvents(new DropperListener(gameManager), this);
        this.getServer().getPluginManager().registerEvents(new Auxiliaries(gameManager), this);
    }

    public FileConfiguration getMapConfig() {
        return this.config;
    }

    public void saveMapConfig() {
        try {
            this.config.save(this.configFile);
        } catch (IOException e) {
            this.getLogger().severe("Não foi possível salvar o arquivo map.yaml!");
        }
    }

    private void loadExtraWorlds() {
        List<String> worlds = getConfig().getStringList("extra-worlds");
        for (String worldName : worlds) {
            if (Bukkit.getWorld(worldName) == null) {
                getLogger().info("Carregando mundo extra: " + worldName + "...");
                new WorldCreator(worldName).createWorld();
            }
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        if (gameManager != null) {
            gameManager.stopBossBarLoop();
        }
        if (gameManager != null && gameManager.getHologramManager() != null) {
            gameManager.getHologramManager().removeAllHolograms();
        }
        getLogger().info("Desligando plugin");
    }
}
