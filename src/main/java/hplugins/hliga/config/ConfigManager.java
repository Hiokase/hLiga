package hplugins.hliga.config;

import hplugins.hliga.Main;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

@Getter
public class ConfigManager {

    private final Main plugin;

    @Getter
    private FileConfiguration config;
    private File configFile;

    private FileConfiguration messagesConfig;
    private File messagesFile;

    @Getter
    private FileConfiguration menusConfig;
    private File menusFile;

    private FileConfiguration seasonsConfig;
    private File seasonsFile;

    @Getter
    private FileConfiguration sonsConfig;
    private File sonsFile;

    @Getter
    private FileConfiguration premiacoesConfig;
    private File premiacoesFile;

    @Getter
    private FileConfiguration tagsConfig;
    private File tagsFile;

    @Getter
    private FileConfiguration topsConfig;
    private File topsFile;

    @Getter
    private Messages messages;


    public ConfigManager(Main plugin) {
        this.plugin = plugin;
    }

    public void loadConfigs() {
        createConfig();
        createMessages();
        createMenus();
        createSeasons();
        createSons();
        createPremiacoes();
        createTags();
        createTops();
        createDiscordConfig();

        this.messages = new Messages(messagesConfig);
    }

    /**
     * Cria o arquivo discord.json se não existir
     */
    private void createDiscordConfig() {
        File discordFile = new File(plugin.getDataFolder(), "discord.json");
        if (!discordFile.exists()) {
            plugin.saveResource("discord.json", false);
            plugin.getLogger().info("Arquivo discord.json criado com configurações padrão");
        }
    }

    private void createConfig() {
        if (configFile == null) {
            configFile = new File(plugin.getDataFolder(), "config.yml");
        }

        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);
    }

    private void createMessages() {
        if (messagesFile == null) {
            messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        }

        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    private void createMenus() {
        if (menusFile == null) {
            menusFile = new File(plugin.getDataFolder(), "menus.yml");
        }

        if (!menusFile.exists()) {
            plugin.saveResource("menus.yml", false);
        }

        menusConfig = YamlConfiguration.loadConfiguration(menusFile);
    }

    private void createSeasons() {
        if (seasonsFile == null) {
            seasonsFile = new File(plugin.getDataFolder(), "seasons.yml");
        }

        if (!seasonsFile.exists()) {
            plugin.saveResource("seasons.yml", false);
        }

        seasonsConfig = YamlConfiguration.loadConfiguration(seasonsFile);
    }

    private void createSons() {
        if (sonsFile == null) {
            sonsFile = new File(plugin.getDataFolder(), "sons.yml");
        }

        if (!sonsFile.exists()) {
            plugin.saveResource("sons.yml", false);
        }

        sonsConfig = YamlConfiguration.loadConfiguration(sonsFile);
    }

    private void createPremiacoes() {
        if (premiacoesFile == null) {
            premiacoesFile = new File(plugin.getDataFolder(), "premiacoes.yml");
        }

        if (!premiacoesFile.exists()) {
            plugin.saveResource("premiacoes.yml", false);
        }

        premiacoesConfig = YamlConfiguration.loadConfiguration(premiacoesFile);
    }

    private void createTags() {
        if (tagsFile == null) {
            tagsFile = new File(plugin.getDataFolder(), "tags.yml");
        }

        if (!tagsFile.exists()) {
            plugin.saveResource("tags.yml", false);
        }

        tagsConfig = YamlConfiguration.loadConfiguration(tagsFile);
    }

    private void createTops() {
        if (topsFile == null) {
            topsFile = new File(plugin.getDataFolder(), "tops.yml");
        }

        if (!topsFile.exists()) {
            plugin.saveResource("tops.yml", false);
        }

        topsConfig = YamlConfiguration.loadConfiguration(topsFile);
    }


    public void saveConfig() {
        if (config == null || configFile == null) {
            return;
        }

        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Não foi possível salvar config.yml", e);
        }
    }

    public void saveMessages() {
        if (messagesConfig == null || messagesFile == null) {
            return;
        }

        try {
            messagesConfig.save(messagesFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Não foi possível salvar messages.yml", e);
        }
    }

    public void saveMenus() {
        if (menusConfig == null || menusFile == null) {
            return;
        }

        try {
            menusConfig.save(menusFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Não foi possível salvar menus.yml", e);
        }
    }

    public void saveSeasons() {
        if (seasonsConfig == null || seasonsFile == null) {
            return;
        }

        try {
            seasonsConfig.save(seasonsFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Não foi possível salvar seasons.yml", e);
        }
    }

    public void saveSons() {
        if (sonsConfig == null || sonsFile == null) {
            return;
        }

        try {
            sonsConfig.save(sonsFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Não foi possível salvar sons.yml", e);
        }
    }

    public void savePremiacoes() {
        if (premiacoesConfig == null || premiacoesFile == null) {
            return;
        }

        try {
            premiacoesConfig.save(premiacoesFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Não foi possível salvar premiacoes.yml", e);
        }
    }

    public void saveTags() {
        if (tagsConfig == null || tagsFile == null) {
            return;
        }

        try {
            tagsConfig.save(tagsFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Não foi possível salvar tags.yml", e);
        }
    }



    public void reloadConfigs() {
        createConfig();
        createMessages();
        createMenus();
        createSeasons();
        createSons();
        createPremiacoes();

        this.messages = new Messages(messagesConfig);
    }
}
