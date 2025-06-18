package hplugins.hliga;

import hplugins.hliga.api.HLigaAPI;
import hplugins.hliga.commands.HLigaCommand;
import hplugins.hliga.commands.TemporadaCommand;
import hplugins.hliga.config.ConfigManager;
import hplugins.hliga.database.DatabaseManager;
import hplugins.hliga.hooks.PlaceholderAPIHook;
import hplugins.hliga.hooks.ClansManager;
import hplugins.hliga.hooks.providers.SimpleClansHook;
import hplugins.hliga.listeners.ClanListener;
import hplugins.hliga.managers.LigaManager;
import hplugins.hliga.managers.NPCManager;
import hplugins.hliga.managers.PointsManager;
import hplugins.hliga.managers.RewardManager;
import hplugins.hliga.managers.SeasonManager;
import hplugins.hliga.managers.TagManager;
import hplugins.hliga.managers.NametagManager;
import hplugins.hliga.inventory.InventoryManager;
import hplugins.hliga.utils.LogUtils;
import hplugins.hliga.utils.MinecraftVersion;
import hplugins.hliga.utils.VersionUtils;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;
import java.util.logging.Level;
import java.util.List;

@Getter
public final class Main extends JavaPlugin {

    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private SimpleClansHook simpleClansHook;
    private ClansManager clansManager;
    private PointsManager pointsManager;
    private SeasonManager seasonManager;
    private LigaManager ligaManager;
    private RewardManager rewardManager;
    private InventoryManager inventoryManager;
    private NPCManager npcManager;
    private TagManager tagManager;
    private NametagManager nametagManager;
    private HLigaAPI api;


    public HLigaAPI getAPI() {
        return api;
    }

    private long startTime;
    
    @Override
    public void onEnable() {
        startTime = System.currentTimeMillis();
        this.configManager = new ConfigManager(this);
        configManager.loadConfigs();

        LogUtils.init(this);
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', 
            "&8[&2hLiga&8] &7Inicializando v" + getDescription().getVersion()));
        VersionUtils.initializeCompatibility();

        checkDependencies();
        
        
        this.databaseManager = new DatabaseManager(this);
        if (!databaseManager.initialize()) {
            LogUtils.severe("Falha ao inicializar o banco de dados! Desativando plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        
        this.simpleClansHook = new SimpleClansHook(this);
        this.clansManager = new ClansManager(this);
        this.pointsManager = new PointsManager(this);
        this.seasonManager = new SeasonManager(this);
        this.ligaManager = new LigaManager(this);
        this.rewardManager = new RewardManager(this);
        this.inventoryManager = new InventoryManager(this);
        this.npcManager = new NPCManager(this);
        this.tagManager = new TagManager(this);
        this.nametagManager = null;
        this.api = new HLigaAPI(this);
        
        
        Bukkit.getScheduler().runTaskLater(this, () -> {
            LogUtils.debug("🚀 Plugin iniciado - verificando NPCs salvos...");
            npcManager.recreateNPCsFromFile();
            
            
            seasonManager.initialize();
            
            
            startAutoUpdateSystem();
        }, 20L); 
        
        

        HLigaCommand hligaCommand = new HLigaCommand(this);
        Objects.requireNonNull(getCommand("liga")).setExecutor(hligaCommand);
        Objects.requireNonNull(getCommand("liga")).setTabCompleter(hligaCommand);
        Objects.requireNonNull(getCommand("temporada")).setExecutor(new TemporadaCommand(this));

        registerClanListeners();
        
        getServer().getPluginManager().registerEvents(new hplugins.hliga.listeners.ArmorStandListener(this), this);
        
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderAPIHook(this).register();
            Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', 
                "&8[&2hLiga&8] &a✓ PlaceholderAPI integrado"));
        }

        if (getConfig().getBoolean("clans.sincronizar_ao_iniciar", true)) {
            Bukkit.getScheduler().runTaskLaterAsynchronously(this, () -> {
                try {
                    LogUtils.debugMedium("Iniciando sincronização automática de clãs...");
                    clansManager.syncClansWithDatabase();
                } catch (Exception e) {
                    LogUtils.severe("Erro ao sincronizar clãs!", e);
                }
            }, 80L);
        }
        
        
        int intervaloSincronizacao = getConfig().getInt("clans.intervalo_sincronizacao", 0);
        if (intervaloSincronizacao > 0) {
            long intervaloTicks = intervaloSincronizacao * 60 * 20L;
            Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
                try {
                    LogUtils.debugMedium("Executando sincronização periódica de clãs...");
                    
                    clansManager.syncClansWithDatabase();
                } catch (Exception e) {
                    LogUtils.severe("Erro ao sincronizar clãs periodicamente!", e);
                }
            }, intervaloTicks, intervaloTicks);
            LogUtils.debugMedium("Sincronização periódica de clãs configurada para cada " + intervaloSincronizacao + " minutos.");
        }
        
        
        long endTime = System.currentTimeMillis();
        long durationMs = endTime - startTime;
        double durationSeconds = durationMs / 1000.0;

        String version = getDescription().getVersion();
        String mcVersion = MinecraftVersion.getVersion();

        LogUtils.debugHigh("Versão do servidor Minecraft detectada: " + Bukkit.getVersion());
        LogUtils.debugHigh("Versão do pacote NMS: " + Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3]);

        String timeFormat = String.format("%.1f", durationSeconds);
        Bukkit.getConsoleSender().sendMessage(colorize("&8[&2hLiga&8] &aAtivado v" + version + " &7(" + timeFormat + "s)"));
    }
    
    @Override
    public void onDisable() {
        if (databaseManager != null) {databaseManager.shutdown();}
        if (inventoryManager != null) {inventoryManager.clearAll();}
        if (nametagManager != null) {nametagManager.disable();}
        
        
        if (npcManager != null) {
            try {
                LogUtils.debug("🔄 Servidor desligando - removendo NPCs físicos...");
                npcManager.cleanup();
                npcManager.forceCleanCitizensNPCs();
                LogUtils.debug("Sistema de NPCs limpo completamente");
            } catch (Exception e) {
                LogUtils.error("Erro durante limpeza de NPCs: " + e.getMessage());
            }
        }

        if (tagManager != null) {
            try {
                tagManager.shutdown();
                LogUtils.debug("Sistema de tags finalizado");
            } catch (Exception e) {
                LogUtils.error("Erro durante finalização do sistema de tags: " + e.getMessage());
            }
        }
        
        
        String version = getDescription().getVersion();
        String mcVersion = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        LogUtils.debug("Desativando hLiga v" + version + " (NMS: " + mcVersion + ")");
        Bukkit.getConsoleSender().sendMessage(colorize("&8[&2hLiga&8] &cDesativado v" + version));
    }
    
    private void checkDependencies() {
        boolean simpleClansFound = Bukkit.getPluginManager().getPlugin("SimpleClans") != null;
        boolean leafGuildsFound = Bukkit.getPluginManager().getPlugin("LeafGuilds") != null;
        if (simpleClansFound) {LogUtils.debugHigh("SimpleClans encontrado para integração");}
        if (leafGuildsFound) {LogUtils.debugHigh("LeafGuilds encontrado para integração");}
        if (!simpleClansFound && !leafGuildsFound) {LogUtils.debugHigh("Nenhum plugin de clãs reconhecido encontrado");}
    }
    
    /**
     * Registra os listeners de clãs de forma condicional para evitar erros
     * quando as dependências não estão disponíveis
     */
    private void registerClanListeners() {

        ClanListener clanListener = new ClanListener(this);

        try {
            getServer().getPluginManager().registerEvents(clanListener, this);
        } catch (Exception e) {
            
            sendConsoleMessage("&cErro ao registrar eventos básicos: &f" + e.getMessage());
            if (getConfig().getBoolean("debug", false)) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Envia mensagem colorida para o console
     */
    private void sendConsoleMessage(String message) {
        Bukkit.getConsoleSender().sendMessage(colorize("&8[&2hLiga&8] " + message));
    }
    
    public void reload() {
        configManager.loadConfigs();
        databaseManager.reconnect();
        
    }
    
    /**
     * Inicia o sistema de atualização automática dos NPCs baseado em horário real
     */
    private void startAutoUpdateSystem() {
        try {
            
            File topsFile = new File(getDataFolder(), "tops.yml");
            if (!topsFile.exists()) {
                LogUtils.debug("Arquivo tops.yml não existe - sistema de atualização automática não iniciado");
                return;
            }
            
            YamlConfiguration topsConfig = YamlConfiguration.loadConfiguration(topsFile);
            int intervalMinutos = topsConfig.getInt("configuracoes.intervalo_atualizacao", 5);
            
            
            if (intervalMinutos < 1) {
                intervalMinutos = 1;
                LogUtils.warn("Intervalo de atualização muito baixo, definido para 1 minuto");
            }
            
            final int intervaloFinal = intervalMinutos; 
            
            LogUtils.info("Sistema de atualização automática iniciado - Intervalo: " + intervalMinutos + " minutos");
            
            
            long intervaloTicks = intervaloFinal * 1200L;
            
            
            Bukkit.getScheduler().runTaskLater(this, () -> {
                try {
                    LogUtils.debug("Executando primeira atualização automática dos NPCs");
                    int updated = npcManager.updateAllNPCs();
                    if (updated > 0) {
                        LogUtils.info("Primeira atualização automática concluída - " + updated + " NPCs atualizados");
                    }
                } catch (Exception e) {
                    LogUtils.error("Erro na primeira atualização automática: " + e.getMessage());
                }
            }, 1200L);
            
            
            Bukkit.getScheduler().runTaskTimer(this, () -> {
                try {
                    LogUtils.debug("Executando atualização automática periódica dos NPCs");
                    int updated = npcManager.updateAllNPCs();
                    if (updated > 0) {
                        LogUtils.info("Atualização automática concluída - " + updated + " NPCs atualizados às " + 
                            java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));
                    }
                } catch (Exception e) {
                    LogUtils.error("Erro na atualização automática dos NPCs: " + e.getMessage());
                }
            }, intervaloTicks, intervaloTicks);
            
        } catch (Exception e) {
            LogUtils.error("Erro ao iniciar sistema de atualização automática: " + e.getMessage());
        }
    }

    private String colorize(String text) {
        return text.replace('&', '§');
    }
}