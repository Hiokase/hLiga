package hplugins.hliga.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import hplugins.hliga.Main;
import hplugins.hliga.utils.LogUtils;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;

public class HikariCPManager {
    
    private final Main plugin;
    private HikariDataSource dataSource;
    
    public HikariDataSource getDataSource() {
        return dataSource;
    }
    
    public HikariCPManager(Main plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Configura o pool de conexões HikariCP
     * 
     * @return true se a configuração foi bem-sucedida, false caso contrário
     */
    public boolean setupPool() {
        try {
            FileConfiguration config = plugin.getConfig();
            
            String host = config.getString("database.mysql.host", "localhost");
            int port = config.getInt("database.mysql.port", 3306);
            String database = config.getString("database.mysql.database", "hliga");
            String username = config.getString("database.mysql.username", "root");
            String password = config.getString("database.mysql.password", "");
            boolean useSSL = config.getBoolean("database.mysql.useSSL", false);
            int poolSize = config.getInt("database.mysql.poolSize", 10);
            
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
            hikariConfig.setUsername(username);
            hikariConfig.setPassword(password);
            hikariConfig.setDriverClassName("com.mysql.jdbc.Driver");
            hikariConfig.setPoolName("hLiga-HikariPool");
            
            hikariConfig.setMaximumPoolSize(poolSize);
            hikariConfig.setMinimumIdle(poolSize / 2);
            hikariConfig.setIdleTimeout(30000);
            hikariConfig.setConnectionTimeout(10000);
            hikariConfig.setMaxLifetime(1800000);
            
            hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
            hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
            hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
            hikariConfig.addDataSourceProperty("useLocalSessionState", "true");
            hikariConfig.addDataSourceProperty("rewriteBatchedStatements", "true");
            hikariConfig.addDataSourceProperty("cacheResultSetMetadata", "true");
            hikariConfig.addDataSourceProperty("cacheServerConfiguration", "true");
            hikariConfig.addDataSourceProperty("elideSetAutoCommits", "true");
            hikariConfig.addDataSourceProperty("maintainTimeStats", "false");
            hikariConfig.addDataSourceProperty("useSSL", useSSL ? "true" : "false");
            
            this.dataSource = new HikariDataSource(hikariConfig);
            
            try (Connection connection = dataSource.getConnection()) {
                if (connection.isValid(1000)) {
                    LogUtils.debug("Conexão com o banco de dados MySQL estabelecida com sucesso!");
                    return true;
                } else {
                    plugin.getLogger().severe("Conexão com o banco de dados MySQL inválida!");
                    return false;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao configurar pool de conexões MySQL", e);
            return false;
        }
    }
    
    /**
     * Obtém uma conexão do pool
     * 
     * @return Conexão com o banco de dados
     * @throws SQLException se ocorrer um erro ao obter a conexão
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("Pool de conexões não inicializado");
        }
        return dataSource.getConnection();
    }
    
    /**
     * Fecha o pool de conexões
     */
    public void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
