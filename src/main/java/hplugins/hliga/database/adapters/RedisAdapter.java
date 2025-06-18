package hplugins.hliga.database.adapters;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import hplugins.hliga.Main;
import hplugins.hliga.models.ClanPoints;
import hplugins.hliga.models.PlayerTag;
import hplugins.hliga.models.Season;
import hplugins.hliga.models.TagType;
import hplugins.hliga.utils.LogUtils;
import lombok.RequiredArgsConstructor;
import org.bukkit.configuration.file.FileConfiguration;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class RedisAdapter implements DatabaseAdapter {
    
    private final Main plugin;
    
    public RedisAdapter(Main plugin) {
        this.plugin = plugin;
    }
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    private JedisPool jedisPool;
    
    private static final String KEY_CLAN_POINTS = "hliga:clan_points";
    private static final String KEY_SEASONS = "hliga:seasons";
    private static final String KEY_ACTIVE_SEASON = "hliga:active_season";
    private static final String KEY_LAST_SEASON_ID = "hliga:last_season_id";
    private static final String KEY_PLAYER_TAGS = "hliga:player_tags";
    
    @Override
    public boolean initialize() {
        try {
            FileConfiguration config = plugin.getConfig();
            String host = config.getString("database.redis.host", "localhost");
            int port = config.getInt("database.redis.port", 6379);
            String password = config.getString("database.redis.password", "");
            int database = config.getInt("database.redis.database", 0);
            int poolSize = config.getInt("database.redis.poolSize", 8);
            
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(poolSize);
            poolConfig.setMaxIdle(poolSize / 2);
            poolConfig.setMinIdle(1);
            poolConfig.setTestOnBorrow(true);
            poolConfig.setTestOnReturn(true);
            poolConfig.setTestWhileIdle(true);
            
            if (password != null && !password.isEmpty()) {
                jedisPool = new JedisPool(poolConfig, host, port, 2000, password, database);
            } else {
                jedisPool = new JedisPool(poolConfig, host, port, 2000, null, database);
            }
            
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.ping();
                LogUtils.debug("Conexão com o Redis estabelecida com sucesso!");
                return true;
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao inicializar conexão com o Redis", e);
            return false;
        }
    }
    
    @Override
    public void shutdown() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
        }
    }
    
    @Override
    public int getClanPoints(String clanTag) {
        try (Jedis jedis = jedisPool.getResource()) {
            String pointsStr = jedis.hget(KEY_CLAN_POINTS, clanTag);
            if (pointsStr != null) {
                return Integer.parseInt(pointsStr);
            }
            return 0;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao obter pontos do clã: " + clanTag, e);
            return 0;
        }
    }
    
    @Override
    public boolean setClanPoints(String clanTag, int points) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.hset(KEY_CLAN_POINTS, clanTag, String.valueOf(points));
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao definir pontos do clã: " + clanTag, e);
            return false;
        }
    }
    
    @Override
    public boolean addClanPoints(String clanTag, int points) {
        try (Jedis jedis = jedisPool.getResource()) {
            String currentPointsStr = jedis.hget(KEY_CLAN_POINTS, clanTag);
            int currentPoints = 0;
            if (currentPointsStr != null) {
                currentPoints = Integer.parseInt(currentPointsStr);
            }
            
            int newPoints = currentPoints + points;
            jedis.hset(KEY_CLAN_POINTS, clanTag, String.valueOf(newPoints));
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao adicionar pontos ao clã: " + clanTag, e);
            return false;
        }
    }
    
    @Override
    public boolean removeClanPoints(String clanTag, int points) {
        try (Jedis jedis = jedisPool.getResource()) {
            String currentPointsStr = jedis.hget(KEY_CLAN_POINTS, clanTag);
            int currentPoints = 0;
            if (currentPointsStr != null) {
                currentPoints = Integer.parseInt(currentPointsStr);
            }
            
            int newPoints = Math.max(0, currentPoints - points);
            
            if (currentPoints == newPoints) {
                return true;
            }
            
            jedis.hset(KEY_CLAN_POINTS, clanTag, String.valueOf(newPoints));
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao remover pontos do clã: " + clanTag, e);
            return false;
        }
    }
    
    @Override
    public boolean resetAllPoints() {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(KEY_CLAN_POINTS);
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao resetar pontos de todos os clãs", e);
            return false;
        }
    }
    
    @Override
    public List<ClanPoints> getTopClans(int limit) {
        try (Jedis jedis = jedisPool.getResource()) {
            Map<String, String> allPoints = jedis.hgetAll(KEY_CLAN_POINTS);
            
            List<ClanPoints> clanPointsList = new ArrayList<>();
            for (Map.Entry<String, String> entry : allPoints.entrySet()) {
                ClanPoints clanPoints = new ClanPoints(entry.getKey(), Integer.parseInt(entry.getValue()));
                clanPointsList.add(clanPoints);
            }
            
            Collections.sort(clanPointsList, new Comparator<ClanPoints>() {
                @Override
                public int compare(ClanPoints o1, ClanPoints o2) {
                    return Integer.compare(o2.points, o1.points); // Ordem decrescente
                }
            });
            
            if (clanPointsList.size() > limit) {
                return clanPointsList.subList(0, limit);
            }
            
            return clanPointsList;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao obter ranking de clãs", e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public boolean saveSeason(Season season) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (season.id <= 0) {
                long nextId = jedis.incr(KEY_LAST_SEASON_ID);
                season.id = (int) nextId;
            }
            
            String seasonJson = gson.toJson(season);
            jedis.hset(KEY_SEASONS, String.valueOf(season.id), seasonJson);
            
            if (season.active) {
                jedis.set(KEY_ACTIVE_SEASON, String.valueOf(season.id));
            } else if (jedis.get(KEY_ACTIVE_SEASON) != null && 
                    jedis.get(KEY_ACTIVE_SEASON).equals(String.valueOf(season.id))) {
                jedis.del(KEY_ACTIVE_SEASON);
            }
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao salvar temporada: " + season.id, e);
            return false;
        }
    }
    
    @Override
    public Optional<Season> getSeason(int id) {
        try (Jedis jedis = jedisPool.getResource()) {
            String seasonJson = jedis.hget(KEY_SEASONS, String.valueOf(id));
            
            if (seasonJson != null) {
                Season season = gson.fromJson(seasonJson, Season.class);
                return Optional.of(season);
            }
            
            return Optional.empty();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao obter temporada: " + id, e);
            return Optional.empty();
        }
    }
    
    @Override
    public Optional<Season> getActiveSeason() {
        try (Jedis jedis = jedisPool.getResource()) {
            String activeSeasonId = jedis.get(KEY_ACTIVE_SEASON);
            
            if (activeSeasonId != null) {
                return getSeason(Integer.parseInt(activeSeasonId));
            }
            
            return Optional.empty();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao obter temporada ativa", e);
            return Optional.empty();
        }
    }
    
    @Override
    public boolean setActiveSeason(int seasonId) {
        try (Jedis jedis = jedisPool.getResource()) {
            String seasonJson = jedis.hget(KEY_SEASONS, String.valueOf(seasonId));
            if (seasonJson == null) {
                return false;
            }
            
            String currentActiveId = jedis.get(KEY_ACTIVE_SEASON);
            if (currentActiveId != null) {
                String currentSeasonJson = jedis.hget(KEY_SEASONS, currentActiveId);
                if (currentSeasonJson != null) {
                    Season currentSeason = gson.fromJson(currentSeasonJson, Season.class);
                    currentSeason.active = false;
                    jedis.hset(KEY_SEASONS, currentActiveId, gson.toJson(currentSeason));
                }
            }
            
            Season season = gson.fromJson(seasonJson, Season.class);
            season.active = true;
            jedis.hset(KEY_SEASONS, String.valueOf(seasonId), gson.toJson(season));
            
            jedis.set(KEY_ACTIVE_SEASON, String.valueOf(seasonId));
            
            return true;
        } catch (Exception e) {
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
        
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.hset(KEY_SEASONS, String.valueOf(season.id), gson.toJson(season));
            
            jedis.del(KEY_ACTIVE_SEASON);
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao finalizar temporada ativa", e);
            return false;
        }
    }
    
    @Override
    public List<Season> getSeasonHistory() {
        try (Jedis jedis = jedisPool.getResource()) {
            Map<String, String> allSeasons = jedis.hgetAll(KEY_SEASONS);
            
            List<Season> seasons = new ArrayList<>();
            for (String json : allSeasons.values()) {
                Season season = gson.fromJson(json, Season.class);
                seasons.add(season);
            }
            
            Collections.sort(seasons, new Comparator<Season>() {
                @Override
                public int compare(Season o1, Season o2) {
                    return Long.compare(o2.startDate, o1.startDate); // Ordem decrescente
                }
            });
            
            return seasons;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao obter histórico de temporadas", e);
            return new ArrayList<>();
        }
    }
    
    public boolean deleteSeason(int id) {
        try (Jedis jedis = jedisPool.getResource()) {
            String activeSeasonId = jedis.get(KEY_ACTIVE_SEASON);
            if (activeSeasonId != null && activeSeasonId.equals(String.valueOf(id))) {
                jedis.del(KEY_ACTIVE_SEASON);
            }
            
            long result = jedis.hdel(KEY_SEASONS, String.valueOf(id));
            return result > 0;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao excluir temporada: " + id, e);
            return false;
        }
    }
    
    @Override
    public List<Season> getAllSeasons() {
        List<Season> result = new ArrayList<>();
        
        try (Jedis jedis = jedisPool.getResource()) {
            Map<String, String> seasons = jedis.hgetAll(KEY_SEASONS);
            
            for (Map.Entry<String, String> entry : seasons.entrySet()) {
                try {
                    Season season = gson.fromJson(entry.getValue(), Season.class);
                    result.add(season);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Erro ao converter temporada de ID " + entry.getKey(), e);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao obter todas as temporadas", e);
        }
        
        return result;
    }
    
    @Override
    public List<ClanPoints> getAllClanPoints() {
        List<ClanPoints> result = new ArrayList<>();
        
        try (Jedis jedis = jedisPool.getResource()) {
            Map<String, String> clans = jedis.hgetAll(KEY_CLAN_POINTS);
            
            for (Map.Entry<String, String> entry : clans.entrySet()) {
                try {
                    String clanTag = entry.getKey();
                    int points = Integer.parseInt(entry.getValue());
                    
                    result.add(new ClanPoints(clanTag, points));
                } catch (NumberFormatException e) {
                    plugin.getLogger().log(Level.WARNING, "Erro ao converter pontos para o clã " + entry.getKey(), e);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao obter todos os pontos de clãs", e);
        }
        
        return result;
    }
    
    @Override
    public boolean resetAllClanPoints() {
        try (Jedis jedis = jedisPool.getResource()) {
            Map<String, String> allClans = jedis.hgetAll(KEY_CLAN_POINTS);
            if (!allClans.isEmpty()) {
                Map<String, String> resetData = new HashMap<>();
                for (String clanTag : allClans.keySet()) {
                    resetData.put(clanTag, "0");
                }
                jedis.hmset(KEY_CLAN_POINTS, resetData);
                LogUtils.info("Pontos de " + allClans.size() + " clãs foram resetados para zero");
            }
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao resetar pontos de todos os clãs", e);
            return false;
        }
    }
    
    @Override
    public boolean saveClanPoints(String clanTag, int points) {
        if (clanTag == null || clanTag.isEmpty()) {
            return false;
        }
        
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.hset(KEY_CLAN_POINTS, clanTag, String.valueOf(points));
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao salvar pontos do clã " + clanTag, e);
            return false;
        }
    }

    
    @Override
    public boolean savePlayerTag(PlayerTag tag) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = KEY_PLAYER_TAGS + ":" + tag.getPlayerUuid().toString();
            String tagJson = gson.toJson(tag);
            jedis.hset(key, tag.getTagType().name() + ":" + tag.getPosition(), tagJson);
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao salvar tag do jogador: " + tag.getPlayerUuid(), e);
            return false;
        }
    }
    
    @Override
    public List<PlayerTag> getPlayerTags(UUID playerUuid) {
        List<PlayerTag> result = new ArrayList<>();
        try (Jedis jedis = jedisPool.getResource()) {
            String key = KEY_PLAYER_TAGS + ":" + playerUuid.toString();
            Map<String, String> tags = jedis.hgetAll(key);
            
            for (String tagJson : tags.values()) {
                PlayerTag tag = gson.fromJson(tagJson, PlayerTag.class);
                if (tag.isActive()) {
                    result.add(tag);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao obter tags do jogador: " + playerUuid, e);
        }
        return result;
    }
    
    @Override
    public boolean clearAllRankingTags() {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys(KEY_PLAYER_TAGS + ":*");
            
            for (String key : keys) {
                Map<String, String> tags = jedis.hgetAll(key);
                Map<String, String> filteredTags = new HashMap<>();
                
                for (Map.Entry<String, String> entry : tags.entrySet()) {
                    if (!entry.getKey().startsWith("RANKING")) {
                        filteredTags.put(entry.getKey(), entry.getValue());
                    }
                }
                
                jedis.del(key);
                if (!filteredTags.isEmpty()) {
                    jedis.hmset(key, filteredTags);
                }
            }
            
            plugin.getLogger().info("Tags de ranking removidas do Redis");
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao limpar tags de ranking no Redis", e);
            return false;
        }
    }
    
    @Override
    public Optional<PlayerTag> getActivePlayerTag(UUID playerUuid, TagType tagType) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = KEY_PLAYER_TAGS + ":" + playerUuid.toString();
            Map<String, String> tags = jedis.hgetAll(key);
            
            for (Map.Entry<String, String> entry : tags.entrySet()) {
                if (entry.getKey().startsWith(tagType.name())) {
                    PlayerTag tag = gson.fromJson(entry.getValue(), PlayerTag.class);
                    if (tag.isActive()) {
                        return Optional.of(tag);
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao obter tag ativa do jogador: " + playerUuid, e);
        }
        return Optional.empty();
    }
    
    @Override
    public boolean removePlayerTag(UUID playerUuid, String tagName, int seasonNumber) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = KEY_PLAYER_TAGS + ":" + playerUuid.toString();
            Map<String, String> tags = jedis.hgetAll(key);
            
            for (Map.Entry<String, String> entry : tags.entrySet()) {
                PlayerTag tag = gson.fromJson(entry.getValue(), PlayerTag.class);
                if (tag.getTagName().equals(tagName) && tag.getSeasonNumber() == seasonNumber) {
                    tag.setActive(false);
                    jedis.hset(key, entry.getKey(), gson.toJson(tag));
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao remover tag do jogador: " + playerUuid, e);
            return false;
        }
    }
    

    
    @Override
    public List<PlayerTag> getTagsByType(TagType tagType) {
        List<PlayerTag> result = new ArrayList<>();
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> playerKeys = jedis.keys(KEY_PLAYER_TAGS + ":*");
            for (String playerKey : playerKeys) {
                Map<String, String> tags = jedis.hgetAll(playerKey);
                for (Map.Entry<String, String> entry : tags.entrySet()) {
                    if (entry.getKey().startsWith(tagType.name())) {
                        PlayerTag tag = gson.fromJson(entry.getValue(), PlayerTag.class);
                        if (tag.isActive()) {
                            result.add(tag);
                        }
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao obter tags por tipo: " + tagType, e);
        }
        return result;
    }
    
    @Override
    public boolean removeAllRankingTags() {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> playerKeys = jedis.keys(KEY_PLAYER_TAGS + ":*");
            
            for (String playerKey : playerKeys) {
                Map<String, String> tags = jedis.hgetAll(playerKey);
                Map<String, String> filteredTags = new HashMap<>();
                
                for (Map.Entry<String, String> entry : tags.entrySet()) {
                    if (!entry.getKey().startsWith("RANKING")) {
                        filteredTags.put(entry.getKey(), entry.getValue());
                    }
                }
                
                jedis.del(playerKey);
                if (!filteredTags.isEmpty()) {
                    jedis.hmset(playerKey, filteredTags);
                }
            }
            
            LogUtils.debug("Tags de ranking removidas do Redis");
            return true;
            
        } catch (Exception e) {
            LogUtils.error("Erro ao remover todas as tags de ranking do Redis: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean hasAnySeasonTags() {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> playerKeys = jedis.keys(KEY_PLAYER_TAGS + ":*");
            
            for (String playerKey : playerKeys) {
                Map<String, String> playerTags = jedis.hgetAll(playerKey);
                
                for (String tagKey : playerTags.keySet()) {
                    if (tagKey.startsWith("SEASON")) {
                        String tagData = playerTags.get(tagKey);
                        if (tagData != null && !tagData.isEmpty()) {
                            return true;
                        }
                    }
                }
            }
            
            return false;
            
        } catch (Exception e) {
            LogUtils.warning("Erro ao verificar tags de temporada no Redis: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public void savePlayerTagPreference(java.util.UUID playerUuid, boolean tagsEnabled) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "player_tag_preferences:" + playerUuid.toString();
            jedis.set(key, tagsEnabled ? "1" : "0");
            LogUtils.debug("Preferência de tags salva no Redis para jogador " + playerUuid + ": " + tagsEnabled);
            
        } catch (Exception e) {
            LogUtils.warning("Erro ao salvar preferência de tags no Redis: " + e.getMessage());
        }
    }
    
    @Override
    public boolean getPlayerTagPreference(java.util.UUID playerUuid) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "player_tag_preferences:" + playerUuid.toString();
            String value = jedis.get(key);
            
            if (value != null) {
                boolean enabled = "1".equals(value);
                LogUtils.debug("Preferência de tags carregada do Redis para jogador " + playerUuid + ": " + enabled);
                return enabled;
            }
            
        } catch (Exception e) {
            LogUtils.warning("Erro ao carregar preferência de tags do Redis: " + e.getMessage());
        }
        
        return true;
    }
}