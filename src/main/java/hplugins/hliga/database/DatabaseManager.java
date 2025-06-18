package hplugins.hliga.database;

import hplugins.hliga.Main;
import hplugins.hliga.database.adapters.DatabaseAdapter;
import hplugins.hliga.database.adapters.MySQLAdapter;
import hplugins.hliga.database.adapters.RedisAdapter;
import hplugins.hliga.database.adapters.RedisCacheAdapter;
import hplugins.hliga.database.adapters.SQLiteAdapter;
import hplugins.hliga.models.ClanPoints;
import hplugins.hliga.models.Season;
import hplugins.hliga.utils.LogUtils;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.logging.Level;

public class DatabaseManager {
    
    private final Main plugin;
    private DatabaseAdapter adapter;
    private ConnectionPoolManager poolManager;
    private RedisCacheAdapter cacheAdapter;
    
    public DatabaseAdapter getAdapter() {
        return adapter;
    }
    
    public RedisCacheAdapter getCacheAdapter() {
        return cacheAdapter;
    }
    
    public boolean isCacheAvailable() {
        return cacheAdapter != null && cacheAdapter.isAvailable();
    }
    
    @Getter
    private ConnectionPoolManager connectionPoolManager;
    
    public DatabaseManager(Main plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Inicializa o gerenciador de banco de dados
     * 
     * @return true se a inicialização foi bem-sucedida, false caso contrário
     */
    public boolean initialize() {
        FileConfiguration config = plugin.getConfig();
        String databaseType = config.getString("database.type", "SQLITE").toUpperCase();
        
        try {
            switch (databaseType) {
                case "MYSQL":
                case "MARIADB":
                    this.poolManager = new ConnectionPoolManager(plugin);
                    this.connectionPoolManager = poolManager;
                    
                    if (!poolManager.initialize()) {
                        LogUtils.error("Falha ao inicializar pool de conexões para " + databaseType);
                        return false;
                    }
                    this.adapter = new MySQLAdapter(plugin, poolManager.getDataSource());
                    break;
                case "REDIS":
                    this.adapter = new RedisAdapter(plugin);
                    break;
                case "SQLITE":
                default:
                    this.adapter = new SQLiteAdapter(plugin);
                    break;
            }
            
            if (config.getBoolean("database.redis.enabled", false)) {
                this.cacheAdapter = new RedisCacheAdapter(plugin);
                if (cacheAdapter.initialize()) {
                    LogUtils.info("Cache Redis inicializado com sucesso");
                } else {
                    LogUtils.debug("Cache Redis não disponível, continuando sem cache");
                }
            }
            
            return adapter.initialize();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao inicializar banco de dados", e);
            return false;
        }
    }
    
    /**
     * Reconecta ao banco de dados
     * 
     * @return true se a reconexão foi bem-sucedida, false caso contrário
     */
    public boolean reconnect() {
        shutdown();
        return initialize();
    }
    
    /**
     * Desliga o gerenciador de banco de dados
     */
    public void shutdown() {
        if (adapter != null) {
            adapter.shutdown();
        }
        
        if (cacheAdapter != null) {
            cacheAdapter.shutdown();
        }
        
        if (poolManager != null) {
            poolManager.shutdown();
        }
    }
    
    /**
     * Transfere dados entre diferentes tipos de banco de dados
     *
     * @param sourceType Tipo de banco de dados de origem (sqlite, mysql, redis)
     * @param targetType Tipo de banco de dados de destino (sqlite, mysql, redis)
     * @return true se a transferência foi bem-sucedida
     */
    public boolean transferData(String sourceType, String targetType) {
        if (sourceType == null || targetType == null) {
            return false;
        }
        
        sourceType = sourceType.toUpperCase();
        targetType = targetType.toUpperCase();
        
        if (!isValidDatabaseType(sourceType) || !isValidDatabaseType(targetType)) {
            plugin.getLogger().warning("Tipo de banco de dados inválido para transferência: " + 
                    sourceType + " -> " + targetType);
            return false;
        }
        
        try {
            DatabaseAdapter sourceAdapter = createAdapter(sourceType);
            if (sourceAdapter == null) {
                plugin.getLogger().severe("Falha ao criar adaptador para o banco de dados de origem: " + sourceType);
                return false;
            }
            
            if (!sourceAdapter.initialize()) {
                plugin.getLogger().severe("Falha ao inicializar adaptador para o banco de dados de origem: " + sourceType);
                sourceAdapter.shutdown();
                return false;
            }
            
            DatabaseAdapter targetAdapter;
            if (targetType.equals(getCurrentDatabaseType())) {
                targetAdapter = this.adapter;
            } else {
                targetAdapter = createAdapter(targetType);
                if (targetAdapter == null) {
                    plugin.getLogger().severe("Falha ao criar adaptador para o banco de dados de destino: " + targetType);
                    sourceAdapter.shutdown();
                    return false;
                }
                
                if (!targetAdapter.initialize()) {
                    plugin.getLogger().severe("Falha ao inicializar adaptador para o banco de dados de destino: " + targetType);
                    sourceAdapter.shutdown();
                    targetAdapter.shutdown();
                    return false;
                }
            }
            
            LogUtils.debug("Iniciando transferência de pontos de clãs...");
            List<ClanPoints> clanPoints = sourceAdapter.getAllClanPoints();
            int pointsCount = 0;
            
            for (ClanPoints clan : clanPoints) {
                targetAdapter.saveClanPoints(clan.getClanTag(), clan.getPoints());
                pointsCount++;
            }
            LogUtils.debug("Transferidos " + pointsCount + " registros de pontos de clãs.");
            
            LogUtils.debug("Iniciando transferência de temporadas...");
            List<Season> seasons = sourceAdapter.getAllSeasons();
            int seasonsCount = 0;
            
            for (Season season : seasons) {
                targetAdapter.saveSeason(season);
                seasonsCount++;
            }
            LogUtils.debug("Transferidas " + seasonsCount + " temporadas.");
            
            if (!sourceType.equals(getCurrentDatabaseType())) {
                sourceAdapter.shutdown();
            }
            
            if (!targetType.equals(getCurrentDatabaseType()) && targetAdapter != this.adapter) {
                targetAdapter.shutdown();
            }
            
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao transferir dados entre bancos de dados", e);
            return false;
        }
    }
    
    /**
     * Obtém o tipo atual do banco de dados
     * 
     * @return O tipo do banco de dados atual em formato string (SQLITE, MYSQL, REDIS)
     */
    private String getCurrentDatabaseType() {
        if (adapter instanceof SQLiteAdapter) {
            return "SQLITE";
        } else if (adapter instanceof MySQLAdapter) {
            return "MYSQL";
        } else if (adapter instanceof RedisAdapter) {
            return "REDIS";
        }
        return "UNKNOWN";
    }
    
    /**
     * Verifica se o tipo de banco de dados é válido
     * 
     * @param type Tipo de banco de dados
     * @return true se for válido
     */
    private boolean isValidDatabaseType(String type) {
        return type.equals("SQLITE") || type.equals("MYSQL") || type.equals("REDIS");
    }
    
    /**
     * Cria um adaptador para o tipo de banco de dados especificado
     * 
     * @param type Tipo de banco de dados
     * @return O adaptador criado ou null em caso de falha
     */
    private DatabaseAdapter createAdapter(String type) {
        try {
            switch (type) {
                case "MYSQL":
                case "MARIADB":
                    HikariCPManager tempHikari = new HikariCPManager(plugin);
                    if (!tempHikari.setupPool()) {
                        return null;
                    }
                    return new MySQLAdapter(plugin, tempHikari.getDataSource());
                case "REDIS":
                    return new RedisAdapter(plugin);
                case "SQLITE":
                    return new SQLiteAdapter(plugin);
                default:
                    return null;
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao criar adaptador para o banco de dados: " + type, e);
            return null;
        }
    }
}
