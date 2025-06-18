package hplugins.hliga.database.adapters;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import hplugins.hliga.Main;
import hplugins.hliga.models.ClanPoints;
import hplugins.hliga.models.PlayerTag;
import hplugins.hliga.models.Season;
import hplugins.hliga.models.TagType;
import hplugins.hliga.utils.LogUtils;
import org.bukkit.configuration.file.FileConfiguration;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.*;
import java.util.logging.Level;

/**
 * Redis Cache Adapter - Sistema de cache para melhorar performance
 * NÃO É UM BANCO DE DADOS PRIMÁRIO, apenas cache para dados frequentemente acessados
 */
public class RedisCacheAdapter {
    
    private final Main plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private JedisPool jedisPool;
    
    private static final String KEY_CLAN_POINTS = "hliga:cache:clan_points";
    private static final String KEY_RANKINGS = "hliga:cache:rankings";
    private static final String KEY_ACTIVE_SEASON = "hliga:cache:active_season";
    private static final String KEY_PLAYER_TAGS = "hliga:cache:player_tags:";
    
    private static final int CACHE_TTL_RANKINGS = 300; // 5 minutos
    private static final int CACHE_TTL_SEASON = 1800;  // 30 minutos
    private static final int CACHE_TTL_TAGS = 600;     // 10 minutos
    
    public RedisCacheAdapter(Main plugin) {
        this.plugin = plugin;
    }
    
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
                LogUtils.debug("Redis Cache inicializado com sucesso!");
                return true;
            }
        } catch (Exception e) {
            LogUtils.debug("Redis Cache não disponível: " + e.getMessage());
            return false;
        }
    }
    
    public void shutdown() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
        }
    }
    
    public boolean isAvailable() {
        return jedisPool != null && !jedisPool.isClosed();
    }
    
    
    public void cacheRankings(List<ClanPoints> rankings) {
        if (!isAvailable()) return;
        
        try (Jedis jedis = jedisPool.getResource()) {
            String rankingsJson = gson.toJson(rankings);
            jedis.setex(KEY_RANKINGS, CACHE_TTL_RANKINGS, rankingsJson);
        } catch (Exception e) {
            LogUtils.debug("Erro ao cachear rankings: " + e.getMessage());
        }
    }
    
    public Optional<List<ClanPoints>> getCachedRankings() {
        if (!isAvailable()) return Optional.empty();
        
        try (Jedis jedis = jedisPool.getResource()) {
            String rankingsJson = jedis.get(KEY_RANKINGS);
            if (rankingsJson != null) {
                ClanPoints[] rankings = gson.fromJson(rankingsJson, ClanPoints[].class);
                return Optional.of(Arrays.asList(rankings));
            }
        } catch (Exception e) {
            LogUtils.debug("Erro ao obter rankings do cache: " + e.getMessage());
        }
        
        return Optional.empty();
    }
    
    
    public void cacheActiveSeason(Season season) {
        if (!isAvailable()) return;
        
        try (Jedis jedis = jedisPool.getResource()) {
            String seasonJson = gson.toJson(season);
            jedis.setex(KEY_ACTIVE_SEASON, CACHE_TTL_SEASON, seasonJson);
        } catch (Exception e) {
            LogUtils.debug("Erro ao cachear temporada ativa: " + e.getMessage());
        }
    }
    
    public Optional<Season> getCachedActiveSeason() {
        if (!isAvailable()) return Optional.empty();
        
        try (Jedis jedis = jedisPool.getResource()) {
            String seasonJson = jedis.get(KEY_ACTIVE_SEASON);
            if (seasonJson != null) {
                Season season = gson.fromJson(seasonJson, Season.class);
                return Optional.of(season);
            }
        } catch (Exception e) {
            LogUtils.debug("Erro ao obter temporada ativa do cache: " + e.getMessage());
        }
        
        return Optional.empty();
    }
    
    
    public void cachePlayerTags(UUID playerUuid, List<PlayerTag> tags) {
        if (!isAvailable()) return;
        
        try (Jedis jedis = jedisPool.getResource()) {
            String key = KEY_PLAYER_TAGS + playerUuid.toString();
            String tagsJson = gson.toJson(tags);
            jedis.setex(key, CACHE_TTL_TAGS, tagsJson);
        } catch (Exception e) {
            LogUtils.debug("Erro ao cachear tags do jogador: " + e.getMessage());
        }
    }
    
    public Optional<List<PlayerTag>> getCachedPlayerTags(UUID playerUuid) {
        if (!isAvailable()) return Optional.empty();
        
        try (Jedis jedis = jedisPool.getResource()) {
            String key = KEY_PLAYER_TAGS + playerUuid.toString();
            String tagsJson = jedis.get(key);
            if (tagsJson != null) {
                PlayerTag[] tags = gson.fromJson(tagsJson, PlayerTag[].class);
                return Optional.of(Arrays.asList(tags));
            }
        } catch (Exception e) {
            LogUtils.debug("Erro ao obter tags do jogador do cache: " + e.getMessage());
        }
        
        return Optional.empty();
    }
    
    
    public void cacheClanPoints(String clanTag, int points) {
        if (!isAvailable()) return;
        
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.hset(KEY_CLAN_POINTS, clanTag, String.valueOf(points));
            jedis.expire(KEY_CLAN_POINTS, CACHE_TTL_RANKINGS);
        } catch (Exception e) {
            LogUtils.debug("Erro ao cachear pontos do clã: " + e.getMessage());
        }
    }
    
    public Optional<Integer> getCachedClanPoints(String clanTag) {
        if (!isAvailable()) return Optional.empty();
        
        try (Jedis jedis = jedisPool.getResource()) {
            String pointsStr = jedis.hget(KEY_CLAN_POINTS, clanTag);
            if (pointsStr != null) {
                return Optional.of(Integer.parseInt(pointsStr));
            }
        } catch (Exception e) {
            LogUtils.debug("Erro ao obter pontos do clã do cache: " + e.getMessage());
        }
        
        return Optional.empty();
    }
    
    
    public void invalidateRankings() {
        if (!isAvailable()) return;
        
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(KEY_RANKINGS);
            jedis.del(KEY_CLAN_POINTS);
        } catch (Exception e) {
            LogUtils.debug("Erro ao invalidar cache de rankings: " + e.getMessage());
        }
    }
    
    public void invalidateActiveSeason() {
        if (!isAvailable()) return;
        
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(KEY_ACTIVE_SEASON);
        } catch (Exception e) {
            LogUtils.debug("Erro ao invalidar cache de temporada ativa: " + e.getMessage());
        }
    }
    
    public void invalidatePlayerTags(UUID playerUuid) {
        if (!isAvailable()) return;
        
        try (Jedis jedis = jedisPool.getResource()) {
            String key = KEY_PLAYER_TAGS + playerUuid.toString();
            jedis.del(key);
        } catch (Exception e) {
            LogUtils.debug("Erro ao invalidar cache de tags do jogador: " + e.getMessage());
        }
    }
    
    public void clearAllCache() {
        if (!isAvailable()) return;
        
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys("hliga:cache:*");
            if (!keys.isEmpty()) {
                jedis.del(keys.toArray(new String[0]));
            }
        } catch (Exception e) {
            LogUtils.debug("Erro ao limpar todo o cache: " + e.getMessage());
        }
    }
}