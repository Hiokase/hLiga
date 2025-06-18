package hplugins.hliga.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import hplugins.hliga.Main;
import hplugins.hliga.utils.LogUtils;
import org.bukkit.configuration.file.FileConfiguration;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Gerenciador de pool de conexões para bancos de dados
 * Suporta MySQL e SQLite com pools otimizados
 */
public class ConnectionPoolManager {
    
    private final Main plugin;
    private HikariDataSource dataSource;
    private String databaseType;
    
    public ConnectionPoolManager(Main plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Inicializa o pool de conexões baseado na configuração
     */
    public boolean initialize() {
        FileConfiguration config = plugin.getConfig();
        databaseType = config.getString("database.type", "sqlite").toLowerCase();
        
        try {
            HikariConfig hikariConfig = new HikariConfig();
            
            switch (databaseType) {
                case "mysql":
                    return initializeMySQL(hikariConfig, config);
                    
                case "sqlite":
                    return initializeSQLite(hikariConfig, config);
                    
                default:
                    LogUtils.error("Tipo de banco de dados não suportado: " + databaseType);
                    return false;
            }
        } catch (Exception e) {
            LogUtils.error("Erro ao inicializar pool de conexões: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Configura pool para MySQL
     */
    private boolean initializeMySQL(HikariConfig config, FileConfiguration pluginConfig) {
        try {
            String host = pluginConfig.getString("database.mysql.host", "localhost");
            int port = pluginConfig.getInt("database.mysql.port", 3306);
            String database = pluginConfig.getString("database.mysql.database", "hliga");
            String username = pluginConfig.getString("database.mysql.username", "root");
            String password = pluginConfig.getString("database.mysql.password", "");
            boolean useSSL = pluginConfig.getBoolean("database.mysql.useSSL", false);
            
            String jdbcUrl = String.format("jdbc:mysql://%s:%d/%s?useSSL=%s&allowPublicKeyRetrieval=true&serverTimezone=UTC", 
                    host, port, database, useSSL);
            
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(username);
            config.setPassword(password);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            
            config.setMaximumPoolSize(pluginConfig.getInt("database.mysql.poolSize", 10));
            config.setMinimumIdle(pluginConfig.getInt("database.mysql.minIdle", 2));
            config.setConnectionTimeout(pluginConfig.getLong("database.mysql.connectionTimeout", 30000));
            config.setIdleTimeout(pluginConfig.getLong("database.mysql.idleTimeout", 600000));
            config.setMaxLifetime(pluginConfig.getLong("database.mysql.maxLifetime", 1800000));
            
            config.setConnectionTestQuery("SELECT 1");
            config.setValidationTimeout(5000);
            
            config.setPoolName("hLiga-MySQL-Pool");
            
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("useLocalSessionState", "true");
            config.addDataSourceProperty("rewriteBatchedStatements", "true");
            config.addDataSourceProperty("cacheResultSetMetadata", "true");
            config.addDataSourceProperty("cacheServerConfiguration", "true");
            config.addDataSourceProperty("elideSetAutoCommits", "true");
            config.addDataSourceProperty("maintainTimeStats", "false");
            
            dataSource = new HikariDataSource(config);
            
            try (Connection connection = dataSource.getConnection()) {
                LogUtils.info("Pool de conexões MySQL inicializado com sucesso");
                LogUtils.debug("Pool configurado com " + config.getMaximumPoolSize() + " conexões máximas");
                return true;
            }
            
        } catch (Exception e) {
            LogUtils.error("Erro ao configurar pool MySQL: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Configura pool para SQLite
     */
    private boolean initializeSQLite(HikariConfig config, FileConfiguration pluginConfig) {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            
            String dbFile = pluginConfig.getString("database.sqlite.file", "database.db");
            File sqliteFile = new File(dataFolder, dbFile);
            String jdbcUrl = "jdbc:sqlite:" + sqliteFile.getAbsolutePath();
            
            config.setJdbcUrl(jdbcUrl);
            config.setDriverClassName("org.sqlite.JDBC");
            
            config.setMaximumPoolSize(1); // SQLite suporta apenas 1 conexão de escrita
            config.setMinimumIdle(1);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
            
            config.setPoolName("hLiga-SQLite-Pool");
            
            config.addDataSourceProperty("journal_mode", "WAL");
            config.addDataSourceProperty("synchronous", "NORMAL");
            config.addDataSourceProperty("cache_size", "10000");
            config.addDataSourceProperty("foreign_keys", "true");
            config.addDataSourceProperty("busy_timeout", "30000");
            
            dataSource = new HikariDataSource(config);
            
            try (Connection connection = dataSource.getConnection()) {
                LogUtils.info("Pool de conexões SQLite inicializado com sucesso");
                LogUtils.debug("Banco SQLite localizado em: " + sqliteFile.getAbsolutePath());
                return true;
            }
            
        } catch (Exception e) {
            LogUtils.error("Erro ao configurar pool SQLite: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Obtém uma conexão do pool
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("Pool de conexões não está disponível");
        }
        return dataSource.getConnection();
    }
    
    /**
     * Obtém o DataSource
     */
    public DataSource getDataSource() {
        return dataSource;
    }
    
    /**
     * Retorna o tipo de banco de dados configurado
     */
    public String getDatabaseType() {
        return databaseType;
    }
    
    /**
     * Verifica se o pool está ativo
     */
    public boolean isActive() {
        return dataSource != null && !dataSource.isClosed();
    }
    
    /**
     * Obtém estatísticas do pool
     */
    public String getPoolStats() {
        if (dataSource == null) {
            return "Pool não inicializado";
        }
        
        return String.format("Pool: %d/%d conexões ativas, %d idle", 
                dataSource.getHikariPoolMXBean().getActiveConnections(),
                dataSource.getHikariPoolMXBean().getTotalConnections(),
                dataSource.getHikariPoolMXBean().getIdleConnections());
    }
    
    /**
     * Finaliza o pool de conexões
     */
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            try {
                LogUtils.debug("Finalizando pool de conexões...");
                dataSource.close();
                LogUtils.debug("Pool de conexões finalizado com sucesso");
            } catch (Exception e) {
                LogUtils.error("Erro ao finalizar pool de conexões: " + e.getMessage());
            }
        }
    }
    
    /**
     * Força o fechamento de todas as conexões
     */
    public void forceShutdown() {
        if (dataSource != null) {
            try {
                dataSource.getHikariPoolMXBean().softEvictConnections();
                dataSource.close();
                LogUtils.debug("Shutdown forçado do pool de conexões executado");
            } catch (Exception e) {
                LogUtils.error("Erro durante shutdown forçado: " + e.getMessage());
            }
        }
    }
}