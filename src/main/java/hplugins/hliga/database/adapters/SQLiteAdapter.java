package hplugins.hliga.database.adapters;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import hplugins.hliga.Main;
import hplugins.hliga.models.ClanPoints;
import hplugins.hliga.models.PlayerTag;
import hplugins.hliga.models.Season;
import hplugins.hliga.models.TagType;
import hplugins.hliga.utils.LogUtils;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Arrays;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public class SQLiteAdapter implements DatabaseAdapter {

    private final Main plugin;

    public SQLiteAdapter(Main plugin) {
        this.plugin = plugin;
    }
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private Connection connection;



    @Override
    public boolean initialize() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File dbFile = new File(dataFolder, "database.db");

        try {
            Class.forName("org.sqlite.JDBC");

            String jdbcUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            connection = DriverManager.getConnection(jdbcUrl);

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA journal_mode=DELETE");  // Não usar WAL para evitar arquivos extras
                stmt.execute("PRAGMA synchronous=NORMAL");
                stmt.execute("PRAGMA temp_store=MEMORY");
            }

            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE IF NOT EXISTS clan_points (" +
                        "clan_tag TEXT PRIMARY KEY, " +
                        "points INTEGER NOT NULL DEFAULT 0" +
                        ")");

                statement.execute("CREATE TABLE IF NOT EXISTS seasons (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "name TEXT NOT NULL, " +
                        "start_date INTEGER NOT NULL, " +
                        "end_date INTEGER NOT NULL, " +
                        "active INTEGER NOT NULL DEFAULT 0, " +
                        "winner_clan TEXT, " +
                        "winner_points INTEGER, " +
                        "top_clans TEXT" +
                        ")");

                statement.execute("CREATE TABLE IF NOT EXISTS player_tags (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "player_uuid TEXT NOT NULL, " +
                        "tag_type TEXT NOT NULL, " +
                        "position INTEGER NOT NULL, " +
                        "season_number INTEGER DEFAULT 0, " +
                        "formatted_tag TEXT NOT NULL, " +
                        "tag_name TEXT NOT NULL, " +
                        "obtained_date INTEGER NOT NULL, " +
                        "active INTEGER NOT NULL DEFAULT 1" +
                        ")");

                statement.execute("CREATE TABLE IF NOT EXISTS player_tag_preferences (" +
                        "player_uuid TEXT PRIMARY KEY, " +
                        "tags_enabled INTEGER NOT NULL DEFAULT 1" +
                        ")");

                checkDatabaseIntegrity();
            }

            LogUtils.debugHigh("Banco de dados SQLite inicializado com sucesso");
            return true;
        } catch (ClassNotFoundException | SQLException e) {
            LogUtils.severe("Erro ao inicializar banco de dados SQLite", e);
            return false;
        }
    }

    /**
     * Verifica a integridade do banco de dados SQLite
     */
    private void checkDatabaseIntegrity() {
        try (Statement statement = connection.createStatement()) {
            ResultSet rs = statement.executeQuery("PRAGMA integrity_check(10)");

            boolean hasProblems = false;
            while (rs.next()) {
                String result = rs.getString(1);
                if (!"ok".equalsIgnoreCase(result)) {
                    LogUtils.warning("Problema de integridade detectado no banco de dados: " + result);
                    hasProblems = true;
                }
            }

            if (!hasProblems) {
                LogUtils.debug("Verificação de integridade do banco de dados concluída - OK");
            }

        } catch (SQLException e) {
            LogUtils.warning("Erro ao verificar integridade do banco de dados: " + e.getMessage());
        }
    }

    @Override
    public void shutdown() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                LogUtils.debugHigh("Conexão SQLite fechada com sucesso");
            }
        } catch (SQLException e) {
            LogUtils.warning("Erro ao fechar conexão SQLite", e);
        }
    }

    @Override
    public int getClanPoints(String clanTag) {
        String sql = "SELECT points FROM clan_points WHERE clan_tag = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, clanTag);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("points");
                }
            }

            return 0;
        } catch (SQLException e) {
            LogUtils.warning("Erro ao obter pontos do clã: " + clanTag, e);
            return 0;
        }
    }

    @Override
    public boolean setClanPoints(String clanTag, int points) {
        String sql = "INSERT OR REPLACE INTO clan_points (clan_tag, points) VALUES (?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, clanTag);
            statement.setInt(2, points);

            boolean success = statement.executeUpdate() > 0;
            if (success) {
                LogUtils.debugMedium("Pontos do clã " + clanTag + " definidos para " + points);
            }
            return success;
        } catch (SQLException e) {
            LogUtils.warning("Erro ao definir pontos do clã: " + clanTag, e);
            return false;
        }
    }

    @Override
    public boolean addClanPoints(String clanTag, int points) {
        int currentPoints = getClanPoints(clanTag);
        return setClanPoints(clanTag, currentPoints + points);
    }

    @Override
    public boolean removeClanPoints(String clanTag, int points) {
        int currentPoints = getClanPoints(clanTag);
        int newPoints = Math.max(0, currentPoints - points);

        if (currentPoints == newPoints) {
            return true; // Nada a remover
        }

        return setClanPoints(clanTag, newPoints);
    }

    /**
     * Verifica se um clã existe na tabela de pontos
     */
    public boolean clanExists(String clanTag) {
        String sql = "SELECT 1 FROM clan_points WHERE clan_tag = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, clanTag);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            LogUtils.warning("Erro ao verificar existência do clã " + clanTag, e);
            return false;
        }
    }

    @Override
    public boolean resetAllPoints() {
        String sql = "DELETE FROM clan_points";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
            LogUtils.debug("Pontos de todos os clãs foram resetados");
            return true;
        } catch (SQLException e) {
            LogUtils.warning("Erro ao resetar pontos de todos os clãs", e);
            return false;
        }
    }

    @Override
    public List<ClanPoints> getTopClans(int limit) {
        String sql = "SELECT clan_tag, points FROM clan_points ORDER BY points DESC LIMIT ?";
        List<ClanPoints> result = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String clanTag = resultSet.getString("clan_tag");
                    int points = resultSet.getInt("points");

                    result.add(new ClanPoints(clanTag, points));
                }
            }

            LogUtils.debugHigh("Obtidos " + result.size() + " clãs para o ranking (limite: " + limit + ")");
        } catch (SQLException e) {
            LogUtils.warning("Erro ao obter ranking de clãs", e);
        }

        return result;
    }

    @Override
    public boolean saveSeason(Season season) {
        if (season.id > 0) {
            String sql = "UPDATE seasons SET name = ?, start_date = ?, end_date = ?, active = ?, " +
                    "winner_clan = ?, winner_points = ?, top_clans = ? WHERE id = ?";

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, season.name);
                statement.setLong(2, season.startDate);
                statement.setLong(3, season.endDate);
                statement.setInt(4, season.active ? 1 : 0);
                statement.setString(5, season.winnerClan);
                statement.setInt(6, season.winnerPoints);
                statement.setString(7, gson.toJson(season.topClans));
                statement.setInt(8, season.id);

                boolean success = statement.executeUpdate() > 0;
                if (success) {
                    LogUtils.debug("Temporada " + season.name + " (ID: " + season.id + ") atualizada com sucesso");
                }
                return success;
            } catch (SQLException e) {
                LogUtils.warning("Erro ao atualizar temporada: " + season.id, e);
                return false;
            }
        } else {
            String sql = "INSERT INTO seasons (name, start_date, end_date, active, winner_clan, winner_points, top_clans) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, season.name);
                statement.setLong(2, season.startDate);
                statement.setLong(3, season.endDate);
                statement.setInt(4, season.active ? 1 : 0);
                statement.setString(5, season.winnerClan);
                statement.setInt(6, season.winnerPoints);
                statement.setString(7, gson.toJson(season.topClans));

                if (statement.executeUpdate() > 0) {
                    try (Statement idStatement = connection.createStatement();
                         ResultSet resultSet = idStatement.executeQuery("SELECT last_insert_rowid()")) {

                        if (resultSet.next()) {
                            season.id = resultSet.getInt(1);
                            LogUtils.debug("Nova temporada " + season.name + " (ID: " + season.id + ") criada com sucesso");
                            return true;
                        }
                    }
                }

                return false;
            } catch (SQLException e) {
                LogUtils.warning("Erro ao inserir temporada", e);
                return false;
            }
        }
    }

    @Override
    public Optional<Season> getSeason(int id) {
        String sql = "SELECT * FROM seasons WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Season season = new Season();
                    season.id = resultSet.getInt("id");
                    season.name = resultSet.getString("name");
                    season.startDate = resultSet.getLong("start_date");
                    season.endDate = resultSet.getLong("end_date");
                    season.active = resultSet.getInt("active") == 1;
                    season.winnerClan = resultSet.getString("winner_clan");
                    season.winnerPoints = resultSet.getInt("winner_points");

                    String topClansJson = resultSet.getString("top_clans");
                    if (topClansJson != null && !topClansJson.isEmpty()) {
                        ClanPoints[] topClans = gson.fromJson(topClansJson, ClanPoints[].class);
                        List<ClanPoints> topClansList = new ArrayList<>();
                        for (ClanPoints clan : topClans) {
                            topClansList.add(clan);
                        }
                        season.topClans = topClansList;
                    }

                    LogUtils.debugHigh("Temporada obtida com sucesso: " + season.name + " (ID: " + id + ")");
                    return Optional.of(season);
                }
            }

            LogUtils.debugMedium("Temporada não encontrada: ID " + id);
            return Optional.empty();
        } catch (SQLException e) {
            LogUtils.warning("Erro ao obter temporada: " + id, e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<Season> getActiveSeason() {
        String sql = "SELECT * FROM seasons WHERE active = 1 LIMIT 1";

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                Season season = new Season();
                season.id = resultSet.getInt("id");
                season.name = resultSet.getString("name");
                season.startDate = resultSet.getLong("start_date");
                season.endDate = resultSet.getLong("end_date");
                season.active = resultSet.getInt("active") == 1;
                season.winnerClan = resultSet.getString("winner_clan");
                season.winnerPoints = resultSet.getInt("winner_points");

                String topClansJson = resultSet.getString("top_clans");
                if (topClansJson != null && !topClansJson.isEmpty()) {
                    ClanPoints[] topClans = gson.fromJson(topClansJson, ClanPoints[].class);
                    List<ClanPoints> topClansList = new ArrayList<>();
                    for (ClanPoints clan : topClans) {
                        topClansList.add(clan);
                    }
                    season.topClans = topClansList;
                }

                LogUtils.debugHigh("Temporada ativa encontrada: " + season.name + " (ID: " + season.id + ")");
                return Optional.of(season);
            }

            LogUtils.debugMedium("Nenhuma temporada ativa encontrada");
            return Optional.empty();
        } catch (SQLException e) {
            LogUtils.warning("Erro ao obter temporada ativa", e);
            return Optional.empty();
        }
    }

    @Override
    public boolean setActiveSeason(int seasonId) {
        try {
            try (PreparedStatement deactivateStatement = connection.prepareStatement("UPDATE seasons SET active = 0")) {
                deactivateStatement.executeUpdate();
            }

            try (PreparedStatement activateStatement = connection.prepareStatement("UPDATE seasons SET active = 1 WHERE id = ?")) {
                activateStatement.setInt(1, seasonId);
                boolean success = activateStatement.executeUpdate() > 0;
                if (success) {
                    LogUtils.debug("Temporada ID " + seasonId + " definida como ativa");
                } else {
                    LogUtils.warning("Falha ao definir temporada ID " + seasonId + " como ativa - temporada não encontrada");
                }
                return success;
            }
        } catch (SQLException e) {
            LogUtils.warning("Erro ao definir temporada ativa: " + seasonId, e);
            return false;
        }
    }

    @Override
    public boolean endActiveSeason() {
        Optional<Season> optionalSeason = getActiveSeason();
        if (!optionalSeason.isPresent()) {
            LogUtils.warning("Tentativa de encerrar temporada, mas nenhuma temporada ativa foi encontrada");
            return false;
        }

        Season season = optionalSeason.get();

        if (!season.active) {
            LogUtils.warning("Tentativa de encerrar uma temporada que já foi finalizada: " + season.name + " (ID: " + season.id + ")");
            return false;
        }

        LogUtils.debug("Encerrando temporada ativa: " + season.name + " (ID: " + season.id + ")");

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

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                Season season = new Season();
                season.id = resultSet.getInt("id");
                season.name = resultSet.getString("name");
                season.startDate = resultSet.getLong("start_date");
                season.endDate = resultSet.getLong("end_date");
                season.active = resultSet.getInt("active") == 1;
                season.winnerClan = resultSet.getString("winner_clan");
                season.winnerPoints = resultSet.getInt("winner_points");

                String topClansJson = resultSet.getString("top_clans");
                if (topClansJson != null && !topClansJson.isEmpty()) {
                    ClanPoints[] topClans = gson.fromJson(topClansJson, ClanPoints[].class);
                    List<ClanPoints> topClansList = new ArrayList<>();
                    for (ClanPoints clan : topClans) {
                        topClansList.add(clan);
                    }
                    season.topClans = topClansList;
                }

                result.add(season);
            }

            LogUtils.debugMedium("Obtidas " + result.size() + " temporadas do histórico");
        } catch (SQLException e) {
            LogUtils.warning("Erro ao obter histórico de temporadas", e);
        }

        return result;
    }

    @Override
    public List<Season> getAllSeasons() {
        List<Season> result = new ArrayList<>();

        try {
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM seasons");

            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String name = resultSet.getString("name");
                long startDate = resultSet.getLong("start_date");
                long endDate = resultSet.getLong("end_date");
                boolean active = resultSet.getInt("active") == 1;
                String winnerClan = resultSet.getString("winner_clan");
                int winnerPoints = resultSet.getInt("winner_points");
                String topClansJson = resultSet.getString("top_clans");

                Season season = new Season(id, name, startDate, endDate, active);
                season.winnerClan = winnerClan;
                season.winnerPoints = winnerPoints;

                if (topClansJson != null && !topClansJson.isEmpty()) {
                    try {
                        List<ClanPoints> topClansList = gson.fromJson(topClansJson,
                                new com.google.gson.reflect.TypeToken<List<ClanPoints>>(){}.getType());
                        season.topClans = topClansList;
                    } catch (Exception e) {
                        plugin.getLogger().warning("Erro ao converter top_clans para lista: " + e.getMessage());
                    }
                }

                result.add(season);
            }

            resultSet.close();
            statement.close();
        } catch (SQLException e) {
            plugin.getLogger().warning("Erro ao obter todas as temporadas: " + e.getMessage());
        }

        return result;
    }

    @Override
    public List<ClanPoints> getAllClanPoints() {
        List<ClanPoints> result = new ArrayList<>();

        try {
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM clan_points");

            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                String clanTag = resultSet.getString("clan_tag");
                int points = resultSet.getInt("points");

                result.add(new ClanPoints(clanTag, points));
            }

            resultSet.close();
            statement.close();
        } catch (SQLException e) {
            plugin.getLogger().warning("Erro ao obter todos os pontos de clãs: " + e.getMessage());
        }

        return result;
    }

    @Override
    public boolean resetAllClanPoints() {
        try {
            PreparedStatement statement = connection.prepareStatement("UPDATE clan_points SET points = 0");
            int rowsAffected = statement.executeUpdate();
            statement.close();

            LogUtils.info("Pontos de " + rowsAffected + " clãs foram resetados para zero");
            return true;
        } catch (SQLException e) {
            LogUtils.error("Erro ao resetar pontos de todos os clãs: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean saveClanPoints(String clanTag, int points) {
        if (clanTag == null || clanTag.isEmpty()) {
            return false;
        }

        try {
            PreparedStatement checkStatement = connection.prepareStatement(
                    "SELECT * FROM clan_points WHERE clan_tag = ?");

            checkStatement.setString(1, clanTag);
            ResultSet resultSet = checkStatement.executeQuery();

            boolean exists = resultSet.next();
            resultSet.close();
            checkStatement.close();

            PreparedStatement statement;

            if (exists) {
                statement = connection.prepareStatement(
                        "UPDATE clan_points SET points = ? WHERE clan_tag = ?");
                statement.setInt(1, points);
                statement.setString(2, clanTag);
            } else {
                statement = connection.prepareStatement(
                        "INSERT INTO clan_points (clan_tag, points) VALUES (?, ?)");
                statement.setString(1, clanTag);
                statement.setInt(2, points);
            }

            int result = statement.executeUpdate();
            statement.close();

            return result > 0;
        } catch (SQLException e) {
            plugin.getLogger().warning("Erro ao salvar pontos do clã " + clanTag + ": " + e.getMessage());
            return false;
        }
    }

    // IMPLEMENTAÇÃO DOS MÉTODOS DE TAGS

    @Override
    public boolean savePlayerTag(PlayerTag tag) {
        if (tag == null || !tag.isValid()) {
            return false;
        }

        try {
            String sql = "INSERT OR REPLACE INTO player_tags " +
                    "(player_uuid, tag_type, position, season_number, formatted_tag, tag_name, obtained_date, active) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, tag.getPlayerUuid().toString());
                statement.setString(2, tag.getTagType().name());
                statement.setInt(3, tag.getPosition());
                statement.setInt(4, tag.getSeasonNumber());
                statement.setString(5, tag.getFormattedTag());
                statement.setString(6, tag.getTagName());
                statement.setLong(7, tag.getObtainedDate());
                statement.setInt(8, tag.isActive() ? 1 : 0);

                return statement.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            LogUtils.error("Erro ao salvar tag do jogador: " + e.getMessage());
            return false;
        }
    }

    @Override
    public List<PlayerTag> getPlayerTags(UUID playerUuid) {
        List<PlayerTag> tags = new ArrayList<>();
        String sql = "SELECT * FROM player_tags WHERE player_uuid = ? AND active = 1 ORDER BY obtained_date DESC";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerUuid.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    PlayerTag tag = new PlayerTag();
                    tag.setPlayerUuid(UUID.fromString(resultSet.getString("player_uuid")));
                    tag.setTagType(TagType.valueOf(resultSet.getString("tag_type")));
                    tag.setPosition(resultSet.getInt("position"));
                    tag.setSeasonNumber(resultSet.getInt("season_number"));
                    tag.setFormattedTag(resultSet.getString("formatted_tag"));
                    tag.setTagName(resultSet.getString("tag_name"));
                    tag.setObtainedDate(resultSet.getLong("obtained_date"));
                    tag.setActive(resultSet.getInt("active") == 1);

                    tags.add(tag);
                }
            }

            LogUtils.debug("Obtidas " + tags.size() + " tags para jogador " + playerUuid);

        } catch (SQLException e) {
            LogUtils.warning("Erro ao obter tags do jogador: " + e.getMessage());
        }

        return tags;
    }

    @Override
    public boolean clearAllRankingTags() {
        try {
            String sql = "DELETE FROM player_tags WHERE tag_type = ?";

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, TagType.RANKING.name());

                int deletedRows = statement.executeUpdate();
                LogUtils.info("Removidas " + deletedRows + " tags de ranking");
                return true;
            }
        } catch (SQLException e) {
            LogUtils.error("Erro ao limpar tags de ranking: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean removePlayerTag(UUID playerUuid, String tagType, int position) {
        String sql = "UPDATE player_tags SET active = 0 WHERE player_uuid = ? AND tag_type = ? AND position = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerUuid.toString());
            statement.setString(2, tagType);
            statement.setInt(3, position);

            int result = statement.executeUpdate();
            LogUtils.debug("Tag removida para jogador " + playerUuid + ": " + tagType + " posição " + position);
            return result > 0;

        } catch (SQLException e) {
            LogUtils.warning("Erro ao remover tag do jogador: " + e.getMessage());
            return false;
        }
    }

    @Override
    public Optional<PlayerTag> getActivePlayerTag(UUID playerUuid, TagType tagType) {
        String sql = "SELECT * FROM player_tags WHERE player_uuid = ? AND tag_type = ? AND active = 1 ORDER BY obtained_date DESC LIMIT 1";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerUuid.toString());
            statement.setString(2, tagType.name());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    UUID uuid = UUID.fromString(resultSet.getString("player_uuid"));
                    TagType type = TagType.valueOf(resultSet.getString("tag_type"));
                    int position = resultSet.getInt("position");
                    int seasonNumber = resultSet.getInt("season_number");
                    String formattedTag = resultSet.getString("formatted_tag");
                    String tagName = resultSet.getString("tag_name");

                    PlayerTag tag = new PlayerTag(uuid, type, position, seasonNumber, formattedTag, tagName);
                    tag.setObtainedDate(resultSet.getLong("obtained_date"));
                    tag.setActive(resultSet.getBoolean("active"));

                    return Optional.of(tag);
                }
            }

        } catch (SQLException e) {
            LogUtils.warning("Erro ao obter tag ativa do jogador: " + e.getMessage());
        }

        return Optional.empty();
    }

    @Override
    public List<PlayerTag> getTagsByType(TagType tagType) {
        String sql = "SELECT * FROM player_tags WHERE tag_type = ? AND active = 1 ORDER BY obtained_date DESC";
        List<PlayerTag> tags = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tagType.name());

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    UUID uuid = UUID.fromString(resultSet.getString("player_uuid"));
                    TagType type = TagType.valueOf(resultSet.getString("tag_type"));
                    int position = resultSet.getInt("position");
                    int seasonNumber = resultSet.getInt("season_number");
                    String formattedTag = resultSet.getString("formatted_tag");
                    String tagName = resultSet.getString("tag_name");

                    PlayerTag tag = new PlayerTag(uuid, type, position, seasonNumber, formattedTag, tagName);
                    tag.setObtainedDate(resultSet.getLong("obtained_date"));
                    tag.setActive(resultSet.getBoolean("active"));

                    tags.add(tag);
                }
            }

        } catch (SQLException e) {
            LogUtils.warning("Erro ao obter tags por tipo: " + e.getMessage());
        }

        return tags;
    }

    @Override
    public boolean removeAllRankingTags() {
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM player_tags WHERE tag_type = ?");
            statement.setString(1, TagType.RANKING.name());

            int rowsAffected = statement.executeUpdate();
            statement.close();

            if (rowsAffected > 0) {
                LogUtils.debug("Removidas " + rowsAffected + " tags de ranking do banco de dados");
            }
            return true;

        } catch (SQLException e) {
            LogUtils.error("Erro ao remover todas as tags de ranking: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean hasAnySeasonTags() {
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT COUNT(*) FROM player_tags WHERE tag_type = ? AND active = 1");
            statement.setString(1, TagType.SEASON.name());

            ResultSet resultSet = statement.executeQuery();
            boolean hasSeasonTags = false;

            if (resultSet.next()) {
                int count = resultSet.getInt(1);
                hasSeasonTags = count > 0;
            }

            resultSet.close();
            statement.close();

            return hasSeasonTags;

        } catch (SQLException e) {
            LogUtils.warning("Erro ao verificar tags de temporada: " + e.getMessage());
            return false;
        }
    }

    /**
     * Salva a preferência de exibição de tags de um jogador
     */
    public void savePlayerTagPreference(UUID playerUuid, boolean tagsEnabled) {
        String sql = "INSERT OR REPLACE INTO player_tag_preferences (player_uuid, tags_enabled) VALUES (?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerUuid.toString());
            statement.setInt(2, tagsEnabled ? 1 : 0);

            statement.executeUpdate();
            LogUtils.debug("Preferência de tags salva para jogador " + playerUuid + ": " + tagsEnabled);

        } catch (SQLException e) {
            LogUtils.warning("Erro ao salvar preferência de tags: " + e.getMessage());
        }
    }

    /**
     * Carrega a preferência de exibição de tags de um jogador
     * Retorna true por padrão se não existir registro
     */
    public boolean getPlayerTagPreference(UUID playerUuid) {
        String sql = "SELECT tags_enabled FROM player_tag_preferences WHERE player_uuid = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerUuid.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    boolean enabled = resultSet.getInt("tags_enabled") == 1;
                    LogUtils.debug("Preferência de tags carregada para jogador " + playerUuid + ": " + enabled);
                    return enabled;
                }
            }

        } catch (SQLException e) {
            LogUtils.warning("Erro ao carregar preferência de tags: " + e.getMessage());
        }

        return true;
    }
}
