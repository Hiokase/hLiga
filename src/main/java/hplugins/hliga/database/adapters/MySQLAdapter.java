package hplugins.hliga.database.adapters;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import hplugins.hliga.Main;
import hplugins.hliga.models.ClanPoints;
import hplugins.hliga.models.PlayerTag;
import hplugins.hliga.models.Season;
import hplugins.hliga.models.TagType;
import hplugins.hliga.utils.LogUtils;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.configuration.file.FileConfiguration;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

public class MySQLAdapter implements DatabaseAdapter {
    
    private final Main plugin;
    private final DataSource dataSource;
    
    public MySQLAdapter(Main plugin, DataSource dataSource) {
        this.plugin = plugin;
        this.dataSource = dataSource;
    }
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    @Override
    public boolean initialize() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS clan_points (" +
                    "clan_tag VARCHAR(32) PRIMARY KEY, " +
                    "points INT NOT NULL DEFAULT 0" +
                    ")");
            
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS seasons (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(64) NOT NULL, " +
                    "start_date BIGINT NOT NULL, " +
                    "end_date BIGINT NOT NULL, " +
                    "active BOOLEAN NOT NULL DEFAULT FALSE, " +
                    "winner_clan VARCHAR(32), " +
                    "winner_points INT, " +
                    "top_clans TEXT" +
                    ")");
            
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS player_tags (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "player_uuid VARCHAR(36) NOT NULL, " +
                    "tag_type VARCHAR(20) NOT NULL, " +
                    "position INT NOT NULL, " +
                    "season_number INT NOT NULL, " +
                    "formatted_tag TEXT NOT NULL, " +
                    "tag_name VARCHAR(50) NOT NULL, " +
                    "obtained_date BIGINT NOT NULL, " +
                    "active BOOLEAN NOT NULL DEFAULT TRUE, " +
                    "INDEX idx_player_uuid (player_uuid), " +
                    "INDEX idx_tag_type (tag_type), " +
                    "INDEX idx_active (active)" +
                    ")");
            
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao inicializar tabelas do MySQL", e);
            return false;
        }
    }
    
    @Override
    public void shutdown() {
    }
    
    @Override
    public int getClanPoints(String clanTag) {
        String sql = "SELECT points FROM clan_points WHERE clan_tag = ?";
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, clanTag);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("points");
                }
            }
            
            return 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao obter pontos do clã: " + clanTag, e);
            return 0;
        }
    }
    
    @Override
    public boolean setClanPoints(String clanTag, int points) {
        String sql = "REPLACE INTO clan_points (clan_tag, points) VALUES (?, ?)";
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, clanTag);
            statement.setInt(2, points);
            
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao definir pontos do clã: " + clanTag, e);
            return false;
        }
    }
    
    @Override
    public boolean addClanPoints(String clanTag, int points) {
        String sql = "INSERT INTO clan_points (clan_tag, points) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE points = points + ?";
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, clanTag);
            statement.setInt(2, points);
            statement.setInt(3, points);
            
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao adicionar pontos ao clã: " + clanTag, e);
            return false;
        }
    }
    
    @Override
    public boolean removeClanPoints(String clanTag, int points) {
        int currentPoints = getClanPoints(clanTag);
        if (currentPoints < points) {
            points = currentPoints; // Não permitir pontos negativos
        }
        
        if (points <= 0) {
            return true; // Nada a remover
        }
        
        String sql = "UPDATE clan_points SET points = points - ? WHERE clan_tag = ?";
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setInt(1, points);
            statement.setString(2, clanTag);
            
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao remover pontos do clã: " + clanTag, e);
            return false;
        }
    }
    
    @Override
    public boolean resetAllPoints() {
        String sql = "DELETE FROM clan_points";
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao resetar pontos de todos os clãs", e);
            return false;
        }
    }
    
    @Override
    public List<ClanPoints> getTopClans(int limit) {
        String sql = "SELECT clan_tag, points FROM clan_points ORDER BY points DESC LIMIT ?";
        List<ClanPoints> result = new ArrayList<>();
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setInt(1, limit);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String clanTag = resultSet.getString("clan_tag");
                    int points = resultSet.getInt("points");
                    
                    result.add(new ClanPoints(clanTag, points));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao obter ranking de clãs", e);
        }
        
        return result;
    }
    
    @Override
    public boolean saveSeason(Season season) {
        if (season.id > 0) {
            String sql = "UPDATE seasons SET name = ?, start_date = ?, end_date = ?, active = ?, " +
                    "winner_clan = ?, winner_points = ?, top_clans = ? WHERE id = ?";
            
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, season.name);
                statement.setLong(2, season.startDate);
                statement.setLong(3, season.endDate);
                statement.setBoolean(4, season.active);
                statement.setString(5, season.winnerClan);
                statement.setInt(6, season.winnerPoints);
                statement.setString(7, gson.toJson(season.topClans));
                statement.setInt(8, season.id);
                
                return statement.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Erro ao atualizar temporada: " + season.id, e);
                return false;
            }
        } else {
            String sql = "INSERT INTO seasons (name, start_date, end_date, active, winner_clan, winner_points, top_clans) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
            
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                
                statement.setString(1, season.name);
                statement.setLong(2, season.startDate);
                statement.setLong(3, season.endDate);
                statement.setBoolean(4, season.active);
                statement.setString(5, season.winnerClan);
                statement.setInt(6, season.winnerPoints);
                statement.setString(7, gson.toJson(season.topClans));
                
                int affectedRows = statement.executeUpdate();
                
                if (affectedRows > 0) {
                    try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            season.id = generatedKeys.getInt(1);
                            return true;
                        }
                    }
                }
                
                return false;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Erro ao inserir temporada", e);
                return false;
            }
        }
    }
    
    @Override
    public Optional<Season> getSeason(int id) {
        String sql = "SELECT * FROM seasons WHERE id = ?";
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setInt(1, id);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Season season = new Season();
                    season.id = resultSet.getInt("id");
                    season.name = resultSet.getString("name");
                    season.startDate = resultSet.getLong("start_date");
                    season.endDate = resultSet.getLong("end_date");
                    season.active = resultSet.getBoolean("active");
                    season.winnerClan = resultSet.getString("winner_clan");
                    season.winnerPoints = resultSet.getInt("winner_points");
                    
                    String topClansJson = resultSet.getString("top_clans");
                    if (topClansJson != null && !topClansJson.isEmpty()) {
                        ClanPoints[] topClans = gson.fromJson(topClansJson, ClanPoints[].class);
                        season.topClans = Arrays.asList(topClans);
                    }
                    
                    return Optional.of(season);
                }
            }
            
            return Optional.empty();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao obter temporada: " + id, e);
            return Optional.empty();
        }
    }
    
    @Override
    public Optional<Season> getActiveSeason() {
        String sql = "SELECT * FROM seasons WHERE active = TRUE LIMIT 1";
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Season season = new Season();
                    season.id = resultSet.getInt("id");
                    season.name = resultSet.getString("name");
                    season.startDate = resultSet.getLong("start_date");
                    season.endDate = resultSet.getLong("end_date");
                    season.active = resultSet.getBoolean("active");
                    season.winnerClan = resultSet.getString("winner_clan");
                    season.winnerPoints = resultSet.getInt("winner_points");
                    
                    String topClansJson = resultSet.getString("top_clans");
                    if (topClansJson != null && !topClansJson.isEmpty()) {
                        ClanPoints[] topClans = gson.fromJson(topClansJson, ClanPoints[].class);
                        season.topClans = Arrays.asList(topClans);
                    }
                    
                    return Optional.of(season);
                }
            }
            
            return Optional.empty();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao obter temporada ativa", e);
            return Optional.empty();
        }
    }
    
    @Override
    public boolean setActiveSeason(int seasonId) {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement deactivateStatement = connection.prepareStatement("UPDATE seasons SET active = FALSE")) {
                deactivateStatement.executeUpdate();
            }
            
            try (PreparedStatement activateStatement = connection.prepareStatement("UPDATE seasons SET active = TRUE WHERE id = ?")) {
                activateStatement.setInt(1, seasonId);
                return activateStatement.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao definir temporada ativa: " + seasonId, e);
            return false;
        }
    }
    
    @Override
    public boolean endActiveSeason() {
        Optional<Season> optionalSeason = getActiveSeason();
        if (!optionalSeason.isPresent()) {
            return false;
        }
        
        Season season = optionalSeason.get();
        
        List<ClanPoints> topClans = getTopClans(10);
        season.topClans = topClans;
        
        if (!topClans.isEmpty()) {
            ClanPoints winner = topClans.get(0);
            season.winnerClan = winner.clanTag;
            season.winnerPoints = winner.points;
        }
        
        season.active = false;
        
        return saveSeason(season);
    }
    
    @Override
    public List<Season> getSeasonHistory() {
        String sql = "SELECT * FROM seasons ORDER BY start_date DESC";
        List<Season> result = new ArrayList<>();
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            
            while (resultSet.next()) {
                Season season = new Season();
                season.id = resultSet.getInt("id");
                season.name = resultSet.getString("name");
                season.startDate = resultSet.getLong("start_date");
                season.endDate = resultSet.getLong("end_date");
                season.active = resultSet.getBoolean("active");
                season.winnerClan = resultSet.getString("winner_clan");
                season.winnerPoints = resultSet.getInt("winner_points");
                
                String topClansJson = resultSet.getString("top_clans");
                if (topClansJson != null && !topClansJson.isEmpty()) {
                    ClanPoints[] topClans = gson.fromJson(topClansJson, ClanPoints[].class);
                    season.topClans = Arrays.asList(topClans);
                }
                
                result.add(season);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao obter histórico de temporadas", e);
        }
        
        return result;
    }
    
    public boolean deleteSeason(int id) {
        String sql = "DELETE FROM seasons WHERE id = ?";
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setInt(1, id);
            
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao excluir temporada: " + id, e);
            return false;
        }
    }
    
    @Override
    public List<Season> getAllSeasons() {
        String sql = "SELECT * FROM seasons";
        List<Season> result = new ArrayList<>();
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String name = resultSet.getString("name");
                long startDate = resultSet.getLong("start_date");
                long endDate = resultSet.getLong("end_date");
                boolean active = resultSet.getBoolean("active");
                String winnerClan = resultSet.getString("winner_clan");
                int winnerPoints = resultSet.getInt("winner_points");
                String topClansJson = resultSet.getString("top_clans");
                
                Season season = new Season(id, name, startDate, endDate, active);
                season.winnerClan = winnerClan;
                season.winnerPoints = winnerPoints;
                
                if (topClansJson != null && !topClansJson.isEmpty()) {
                    try {
                        List<ClanPoints> topClans = gson.fromJson(topClansJson, 
                                new com.google.gson.reflect.TypeToken<List<ClanPoints>>(){}.getType());
                        season.topClans = topClans;
                    } catch (Exception e) {
                        plugin.getLogger().warning("Erro ao converter top_clans para JSON: " + e.getMessage());
                    }
                }
                
                result.add(season);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao obter todas as temporadas", e);
        }
        
        return result;
    }
    
    @Override
    public List<ClanPoints> getAllClanPoints() {
        String sql = "SELECT * FROM clan_points";
        List<ClanPoints> result = new ArrayList<>();
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            
            while (resultSet.next()) {
                String clanTag = resultSet.getString("clan_tag");
                int points = resultSet.getInt("points");
                
                result.add(new ClanPoints(clanTag, points));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao obter pontos de todos os clãs", e);
        }
        
        return result;
    }
    
    @Override
    public boolean resetAllClanPoints() {
        String sql = "UPDATE clan_points SET points = 0";
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            int rowsAffected = statement.executeUpdate();
            LogUtils.info("Pontos de " + rowsAffected + " clãs foram resetados para zero");
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao resetar pontos de todos os clãs", e);
            return false;
        }
    }
    
    @Override
    public boolean saveClanPoints(String clanTag, int points) {
        String sql = "INSERT INTO clan_points (clan_tag, points) VALUES (?, ?) ON DUPLICATE KEY UPDATE points = ?";
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, clanTag);
            statement.setInt(2, points);
            statement.setInt(3, points);
            
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao salvar pontos do clã: " + clanTag, e);
            return false;
        }
    }

    
    @Override
    public boolean savePlayerTag(PlayerTag tag) {
        String sql = "INSERT INTO player_tags (player_uuid, tag_type, position, season_number, formatted_tag, tag_name, obtained_date, active) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE " +
                     "formatted_tag = VALUES(formatted_tag), " +
                     "tag_name = VALUES(tag_name), " +
                     "obtained_date = VALUES(obtained_date), " +
                     "active = VALUES(active)";
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, tag.getPlayerUuid().toString());
            statement.setString(2, tag.getTagType().name());
            statement.setInt(3, tag.getPosition());
            statement.setInt(4, tag.getSeasonNumber());
            statement.setString(5, tag.getFormattedTag());
            statement.setString(6, tag.getTagName());
            statement.setLong(7, tag.getObtainedDate());
            statement.setBoolean(8, tag.isActive());
            
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao salvar tag do jogador: " + tag.getPlayerUuid(), e);
            return false;
        }
    }
    
    @Override
    public List<PlayerTag> getPlayerTags(java.util.UUID playerUuid) {
        String sql = "SELECT * FROM player_tags WHERE player_uuid = ? AND active = TRUE ORDER BY obtained_date DESC";
        List<PlayerTag> result = new ArrayList<>();
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, playerUuid.toString());
            
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    UUID uuid = java.util.UUID.fromString(resultSet.getString("player_uuid"));
                    TagType type = TagType.valueOf(resultSet.getString("tag_type"));
                    int position = resultSet.getInt("position");
                    int seasonNumber = resultSet.getInt("season_number");
                    String formattedTag = resultSet.getString("formatted_tag");
                    String tagName = resultSet.getString("tag_name");
                    long obtainedDate = resultSet.getLong("obtained_date");
                    boolean active = resultSet.getBoolean("active");
                    
                    PlayerTag tag = new PlayerTag(uuid, type, position, seasonNumber, formattedTag, tagName);
                    tag.setObtainedDate(obtainedDate);
                    tag.setActive(active);
                    
                    result.add(tag);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao obter tags do jogador: " + playerUuid, e);
        }
        
        return result;
    }
    
    @Override
    public boolean clearAllRankingTags() {
        String sql = "DELETE FROM player_tags WHERE tag_type = ?";
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, TagType.RANKING.name());
            
            int deletedRows = statement.executeUpdate();
            plugin.getLogger().info("Removidas " + deletedRows + " tags de ranking");
            return true;
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao limpar tags de ranking", e);
            return false;
        }
    }
    
    @Override
    public Optional<PlayerTag> getActivePlayerTag(java.util.UUID playerUuid, TagType tagType) {
        String sql = "SELECT * FROM player_tags WHERE player_uuid = ? AND tag_type = ? AND active = TRUE ORDER BY obtained_date DESC LIMIT 1";
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, playerUuid.toString());
            statement.setString(2, tagType.name());
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    UUID uuid = java.util.UUID.fromString(resultSet.getString("player_uuid"));
                    TagType type = TagType.valueOf(resultSet.getString("tag_type"));
                    int position = resultSet.getInt("position");
                    int seasonNumber = resultSet.getInt("season_number");
                    String formattedTag = resultSet.getString("formatted_tag");
                    String tagName = resultSet.getString("tag_name");
                    long obtainedDate = resultSet.getLong("obtained_date");
                    boolean active = resultSet.getBoolean("active");
                    
                    PlayerTag tag = new PlayerTag(uuid, type, position, seasonNumber, formattedTag, tagName);
                    tag.setObtainedDate(obtainedDate);
                    tag.setActive(active);
                    
                    return Optional.of(tag);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao obter tag ativa do jogador: " + playerUuid, e);
        }
        
        return Optional.empty();
    }
    
    @Override
    public boolean removePlayerTag(java.util.UUID playerUuid, String tagName, int seasonNumber) {
        String sql = "UPDATE player_tags SET active = FALSE WHERE player_uuid = ? AND tag_name = ? AND season_number = ?";
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, playerUuid.toString());
            statement.setString(2, tagName);
            statement.setInt(3, seasonNumber);
            
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao remover tag do jogador: " + playerUuid, e);
            return false;
        }
    }
    

    
    @Override
    public List<PlayerTag> getTagsByType(TagType tagType) {
        String sql = "SELECT * FROM player_tags WHERE tag_type = ? AND active = TRUE ORDER BY obtained_date DESC";
        List<PlayerTag> result = new ArrayList<>();
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, tagType.name());
            
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    UUID playerUuid = java.util.UUID.fromString(resultSet.getString("player_uuid"));
                    TagType currentTagType = TagType.valueOf(resultSet.getString("tag_type"));
                    int position = resultSet.getInt("position");
                    int seasonNumber = resultSet.getInt("season_number");
                    String formattedTag = resultSet.getString("formatted_tag");
                    String tagName = resultSet.getString("tag_name");
                    long obtainedDate = resultSet.getLong("obtained_date");
                    boolean active = resultSet.getBoolean("active");
                    
                    PlayerTag tag = new PlayerTag(playerUuid, currentTagType, position, seasonNumber, formattedTag, tagName);
                    tag.setObtainedDate(obtainedDate);
                    tag.setActive(active);
                    
                    result.add(tag);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao obter tags por tipo: " + tagType, e);
        }
        
        return result;
    }
    
    @Override
    public boolean removeAllRankingTags() {
        String sql = "DELETE FROM player_tags WHERE tag_type = ?";
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, TagType.RANKING.name());
            
            int rowsAffected = statement.executeUpdate();
            if (rowsAffected > 0) {
                LogUtils.debug("Removidas " + rowsAffected + " tags de ranking do banco de dados MySQL");
            }
            return true;
            
        } catch (SQLException e) {
            LogUtils.error("Erro ao remover todas as tags de ranking do MySQL: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean hasAnySeasonTags() {
        String sql = "SELECT COUNT(*) FROM player_tags WHERE tag_type = ? AND active = 1";
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, TagType.SEASON.name());
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    int count = resultSet.getInt(1);
                    return count > 0;
                }
                return false;
            }
            
        } catch (SQLException e) {
            LogUtils.warning("Erro ao verificar tags de temporada no MySQL: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public void savePlayerTagPreference(java.util.UUID playerUuid, boolean tagsEnabled) {
        String sql = "INSERT INTO player_tag_preferences (player_uuid, tags_enabled) VALUES (?, ?) " +
                    "ON DUPLICATE KEY UPDATE tags_enabled = VALUES(tags_enabled)";
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, playerUuid.toString());
            statement.setInt(2, tagsEnabled ? 1 : 0);
            
            statement.executeUpdate();
            LogUtils.debug("Preferência de tags salva no MySQL para jogador " + playerUuid + ": " + tagsEnabled);
            
        } catch (SQLException e) {
            LogUtils.warning("Erro ao salvar preferência de tags no MySQL: " + e.getMessage());
        }
    }
    
    @Override
    public boolean getPlayerTagPreference(java.util.UUID playerUuid) {
        String sql = "SELECT tags_enabled FROM player_tag_preferences WHERE player_uuid = ?";
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, playerUuid.toString());
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    boolean enabled = resultSet.getInt("tags_enabled") == 1;
                    LogUtils.debug("Preferência de tags carregada do MySQL para jogador " + playerUuid + ": " + enabled);
                    return enabled;
                }
            }
            
        } catch (SQLException e) {
            LogUtils.warning("Erro ao carregar preferência de tags do MySQL: " + e.getMessage());
        }
        
        return true;
    }
}