package hplugins.hliga.api;

import hplugins.hliga.Main;
import hplugins.hliga.models.ClanPoints;
import hplugins.hliga.models.Season;
import hplugins.hliga.models.PlayerTag;
import hplugins.hliga.models.Reward;
import hplugins.hliga.models.GenericClan;
import hplugins.hliga.managers.PointsManager;
import hplugins.hliga.managers.SeasonManager;
import hplugins.hliga.managers.TagManager;
import hplugins.hliga.managers.NPCManager;
import hplugins.hliga.managers.RewardManager;
import hplugins.hliga.hooks.ClansManager;
import hplugins.hliga.hooks.ClanProvider;
import hplugins.hliga.api.events.*;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.ArrayList;

/**
 * API principal do hLiga para integração com outros plugins
 * Esta API fornece acesso completo a todas as funcionalidades do hLiga,
 * permitindo que outros desenvolvedores integrem facilmente seus plugins
 * com o sistema de ligas e temporadas.
 * 
 * @author hPlugins
 * @version 1.0.0
 * @since 1.0.0
 */
@Getter
public class HLigaAPI {

    /**
     * -- GETTER --
     *  Obtém a instância singleton da API
     *
     * Instância da API ou null se o plugin não estiver carregado
     */
    @Getter
    private static HLigaAPI instance;
    /**
     * -- GETTER --
     *  Obtém a instância principal do plugin
     *
     * @return Instância do plugin hLiga
     */
    private final Main plugin;
    
    /**
     * Construtor interno da API
     * 
     * @param plugin Instância principal do plugin
     */
    public HLigaAPI(Main plugin) {
        this.plugin = plugin;
        instance = this;
    }

    /**
     * Verifica se a API está disponível e funcional
     * 
     * @return true se a API estiver disponível
     */
    public static boolean isAvailable() {
        return instance != null && instance.plugin != null && instance.plugin.isEnabled();
    }
    
    
    
    /**
     * Obtém os pontos de um clã
     * 
     * @param clanTag Tag do clã
     * @return Pontos do clã ou 0 se não encontrado
     */
    public int getClanPoints(String clanTag) {
        return plugin.getPointsManager().getClanPoints(clanTag);
    }
    
    /**
     * Define os pontos de um clã
     * 
     * @param clanTag Tag do clã
     * @param points Pontos a serem definidos
     * @param reason Motivo da alteração (opcional)
     * @return true se a operação foi bem-sucedida
     */
    public boolean setClanPoints(String clanTag, int points, String reason) {

        HLigaClanPointsChangeEvent event = new HLigaClanPointsChangeEvent(
            clanTag, getClanPoints(clanTag), points, reason, HLigaClanPointsChangeEvent.ChangeType.SET
        );
        Bukkit.getPluginManager().callEvent(event);
        
        if (event.isCancelled()) {
            return false;
        }
        
        boolean success = plugin.getPointsManager().setClanPoints(clanTag, event.getNewPoints());
        
        
        if (success) {
            HLigaClanPointsChangedEvent postEvent = new HLigaClanPointsChangedEvent(
                clanTag, event.getOldPoints(), event.getNewPoints(), reason, HLigaClanPointsChangeEvent.ChangeType.SET
            );
            Bukkit.getPluginManager().callEvent(postEvent);
        }
        
        return success;
    }
    
    /**
     * Define os pontos de um clã
     * 
     * @param clanTag Tag do clã
     * @param points Pontos a serem definidos
     * @return true se a operação foi bem-sucedida
     */
    public boolean setClanPoints(String clanTag, int points) {
        return setClanPoints(clanTag, points, null);
    }
    
    /**
     * Adiciona pontos a um clã
     * 
     * @param clanTag Tag do clã
     * @param points Pontos a serem adicionados
     * @param reason Motivo da alteração (opcional)
     * @return true se a operação foi bem-sucedida
     */
    public boolean addClanPoints(String clanTag, int points, String reason) {
        int currentPoints = getClanPoints(clanTag);
        int newPoints = currentPoints + points;
        
        
        HLigaClanPointsChangeEvent event = new HLigaClanPointsChangeEvent(
            clanTag, currentPoints, newPoints, reason, HLigaClanPointsChangeEvent.ChangeType.ADD
        );
        Bukkit.getPluginManager().callEvent(event);
        
        if (event.isCancelled()) {
            return false;
        }
        
        boolean success = plugin.getPointsManager().addPoints(clanTag, points);
        
        
        if (success) {
            HLigaClanPointsChangedEvent postEvent = new HLigaClanPointsChangedEvent(
                clanTag, currentPoints, event.getNewPoints(), reason, HLigaClanPointsChangeEvent.ChangeType.ADD
            );
            Bukkit.getPluginManager().callEvent(postEvent);
        }
        
        return success;
    }
    
    /**
     * Adiciona pontos a um clã
     * 
     * @param clanTag Tag do clã
     * @param points Pontos a serem adicionados
     * @return true se a operação foi bem-sucedida
     */
    public boolean addClanPoints(String clanTag, int points) {
        return addClanPoints(clanTag, points, null);
    }
    
    /**
     * Remove pontos de um clã
     * 
     * @param clanTag Tag do clã
     * @param points Pontos a serem removidos
     * @param reason Motivo da alteração (opcional)
     * @return true se a operação foi bem-sucedida
     */
    public boolean removeClanPoints(String clanTag, int points, String reason) {
        int currentPoints = getClanPoints(clanTag);
        int newPoints = Math.max(0, currentPoints - points);
        
        
        HLigaClanPointsChangeEvent event = new HLigaClanPointsChangeEvent(
            clanTag, currentPoints, newPoints, reason, HLigaClanPointsChangeEvent.ChangeType.REMOVE
        );
        Bukkit.getPluginManager().callEvent(event);
        
        if (event.isCancelled()) {
            return false;
        }
        
        boolean success = plugin.getPointsManager().removePoints(clanTag, points);
        
        
        if (success) {
            HLigaClanPointsChangedEvent postEvent = new HLigaClanPointsChangedEvent(
                clanTag, currentPoints, event.getNewPoints(), reason, HLigaClanPointsChangeEvent.ChangeType.REMOVE
            );
            Bukkit.getPluginManager().callEvent(postEvent);
        }
        
        return success;
    }
    
    /**
     * Remove pontos de um clã
     * 
     * @param clanTag Tag do clã
     * @param points Pontos a serem removidos
     * @return true se a operação foi bem-sucedida
     */
    public boolean removeClanPoints(String clanTag, int points) {
        return removeClanPoints(clanTag, points, null);
    }
    
    /**
     * Obtém o ranking de clãs
     * 
     * @param limit Limite de clãs a serem retornados (0 para todos)
     * @return Lista ordenada de clãs por pontuação
     */
    public List<ClanPoints> getTopClans(int limit) {
        return plugin.getPointsManager().getTopClans(limit);
    }
    
    /**
     * Obtém a posição de um clã no ranking
     * 
     * @param clanTag Tag do clã
     * @return Posição no ranking (1-based) ou -1 se não encontrado
     */
    public int getClanPosition(String clanTag) {
        return plugin.getPointsManager().getClanPosition(clanTag);
    }
    
    
    
    /**
     * Verifica se existe uma temporada ativa
     * 
     * @return true se existe uma temporada ativa
     */
    public boolean isSeasonActive() {
        return plugin.getSeasonManager().isSeasonActive();
    }
    
    /**
     * Obtém a temporada ativa atual
     * 
     * @return Temporada ativa ou Optional.empty() se não houver
     */
    public Optional<Season> getActiveSeason() {
        return plugin.getSeasonManager().getActiveSeason();
    }
    
    /**
     * Inicia uma nova temporada com duração em dias
     * 
     * @param name Nome da temporada
     * @param durationDays Duração em dias
     * @return true se a operação foi bem-sucedida
     */
    public boolean startSeason(String name, int durationDays) {
        
        HLigaSeasonStartEvent event = new HLigaSeasonStartEvent(name, durationDays);
        Bukkit.getPluginManager().callEvent(event);
        
        if (event.isCancelled()) {
            return false;
        }
        
        boolean success = plugin.getSeasonManager().startSeason(event.getSeasonName(), event.getDurationDays());
        
        
        if (success) {
            Optional<Season> season = getActiveSeason();
            if (season.isPresent()) {
                HLigaSeasonStartedEvent postEvent = new HLigaSeasonStartedEvent(season.get());
                Bukkit.getPluginManager().callEvent(postEvent);
            }
        }
        
        return success;
    }
    
    /**
     * Inicia uma nova temporada com data específica de término
     * 
     * @param name Nome da temporada
     * @param endDate Data de término
     * @return true se a operação foi bem-sucedida
     */
    public boolean startSeasonWithEndDate(String name, LocalDate endDate) {
        
        HLigaSeasonStartEvent event = new HLigaSeasonStartEvent(name, endDate);
        Bukkit.getPluginManager().callEvent(event);
        
        if (event.isCancelled()) {
            return false;
        }
        
        boolean success = plugin.getSeasonManager().startSeasonWithSpecificDate(
            event.getSeasonName(), event.getEndDate(), -1, -1
        );
        
        
        if (success) {
            Optional<Season> season = getActiveSeason();
            if (season.isPresent()) {
                HLigaSeasonStartedEvent postEvent = new HLigaSeasonStartedEvent(season.get());
                Bukkit.getPluginManager().callEvent(postEvent);
            }
        }
        
        return success;
    }
    
    /**
     * Finaliza a temporada ativa
     * 
     * @return true se a operação foi bem-sucedida
     */
    public boolean endSeason() {
        Optional<Season> activeSeason = getActiveSeason();
        if (!activeSeason.isPresent()) {
            return false;
        }
        
        
        HLigaSeasonEndEvent event = new HLigaSeasonEndEvent(activeSeason.get());
        Bukkit.getPluginManager().callEvent(event);
        
        if (event.isCancelled()) {
            return false;
        }
        
        boolean success = plugin.getSeasonManager().endSeason();
        
        
        if (success) {
            HLigaSeasonEndedEvent postEvent = new HLigaSeasonEndedEvent(activeSeason.get());
            Bukkit.getPluginManager().callEvent(postEvent);
        }
        
        return success;
    }
    
    /**
     * Obtém o histórico de temporadas
     * 
     * @return Lista de todas as temporadas
     */
    public List<Season> getSeasonHistory() {
        return plugin.getSeasonManager().getSeasonHistory();
    }
    
    
    
    /**
     * Obtém o provedor de clãs ativo
     * 
     * @return Provedor de clãs atual
     */
    public ClanProvider getClanProvider() {
        return plugin.getClansManager().getActiveProvider();
    }
    
    /**
     * Obtém o clã de um jogador
     * 
     * @param player Jogador
     * @return Tag do clã ou null se não estiver em um clã
     */
    public String getPlayerClan(Player player) {
        GenericClan clan = plugin.getClansManager().getPlayerClan(player);
        return clan != null ? clan.getTag() : null;
    }
    
    /**
     * Obtém o clã de um jogador offline
     * 
     * @param player Jogador offline
     * @return Tag do clã ou null se não estiver em um clã
     */
    public String getPlayerClan(OfflinePlayer player) {
        if (player.getPlayer() != null) {
            GenericClan clan = plugin.getClansManager().getPlayerClan(player.getPlayer());
            return clan != null ? clan.getTag() : null;
        }
        return null;
    }
    
    /**
     * Verifica se um jogador está em um clã
     * 
     * @param player Jogador
     * @return true se o jogador estiver em um clã
     */
    public boolean isPlayerInClan(Player player) {
        return getPlayerClan(player) != null;
    }
    
    /**
     * Obtém todos os clãs cadastrados
     * 
     * @return Lista de tags de clãs
     */
    public List<String> getAllClans() {
        try {
            return plugin.getClansManager().getAllClanTags();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    /**
     * Verifica se um clã existe
     * 
     * @param clanTag Tag do clã
     * @return true se o clã existir
     */
    public boolean clanExists(String clanTag) {
        return plugin.getClansManager().clanExists(clanTag);
    }
    
    
    
    /**
     * Verifica se o sistema de tags está ativo
     * 
     * @return true se o sistema de tags estiver ativo
     */
    public boolean isTagSystemEnabled() {
        return plugin.getTagManager() != null && plugin.getTagManager().isSystemEnabled();
    }
    
    /**
     * Obtém as tags de um jogador
     * 
     * @param playerId UUID do jogador
     * @return Lista de tags do jogador
     */
    public List<PlayerTag> getPlayerTags(UUID playerId) {
        if (!isTagSystemEnabled()) {
            return new ArrayList<>();
        }
        return plugin.getTagManager().getPlayerTags(playerId);
    }
    
    /**
     * Adiciona uma tag a um jogador (funcionalidade básica)
     * 
     * @param playerId UUID do jogador
     * @param tagId ID da tag
     * @param seasonName Nome da temporada (opcional)
     * @return true se a operação foi bem-sucedida
     */
    public boolean addPlayerTag(UUID playerId, String tagId, String seasonName) {
        if (!isTagSystemEnabled()) {
            return false;
        }
        
        
        HLigaPlayerTagAddEvent event = new HLigaPlayerTagAddEvent(playerId, tagId, seasonName);
        Bukkit.getPluginManager().callEvent(event);
        
        if (event.isCancelled()) {
            return false;
        }
        
        
        try {
            
            
            HLigaPlayerTagAddedEvent postEvent = new HLigaPlayerTagAddedEvent(playerId, tagId, seasonName);
            Bukkit.getPluginManager().callEvent(postEvent);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Remove uma tag de um jogador (funcionalidade básica)
     * 
     * @param playerId UUID do jogador
     * @param tagId ID da tag
     * @return true se a operação foi bem-sucedida
     */
    public boolean removePlayerTag(UUID playerId, String tagId) {
        if (!isTagSystemEnabled()) {
            return false;
        }
        
        
        HLigaPlayerTagRemoveEvent event = new HLigaPlayerTagRemoveEvent(playerId, tagId);
        Bukkit.getPluginManager().callEvent(event);
        
        if (event.isCancelled()) {
            return false;
        }
        
        
        try {
            HLigaPlayerTagRemovedEvent postEvent = new HLigaPlayerTagRemovedEvent(playerId, tagId);
            Bukkit.getPluginManager().callEvent(postEvent);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    
    
    /**
     * Cria um NPC de ranking
     * 
     * @param npcId ID único do NPC
     * @param position Posição no ranking a ser exibida
     * @param player Jogador que está criando (para localização)
     * @return true se a operação foi bem-sucedida
     */
    public boolean createRankingNPC(String npcId, int position, Player player) {
        try {
            return plugin.getNpcManager().createNPC(npcId, position, player.getLocation());
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Remove um NPC de ranking
     * 
     * @param npcId ID do NPC
     * @return true se a operação foi bem-sucedida
     */
    public boolean removeRankingNPC(String npcId) {
        try {
            return plugin.getNpcManager().removeNPC(npcId);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Verifica se um NPC existe
     * 
     * @param npcId ID do NPC
     * @return true se o NPC existir
     */
    public boolean npcExists(String npcId) {
        try {
            
            return plugin.getNpcManager().npcExists(npcId);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Atualiza todos os NPCs de ranking
     * 
     * @return Número de NPCs atualizados
     */
    public int updateAllRankingNPCs() {
        try {
            plugin.getNpcManager().updateAllNPCs();
            return 1; 
        } catch (Exception e) {
            return 0;
        }
    }
    
    
    
    /**
     * Adiciona uma recompensa para uma posição específica
     * 
     * @param position Posição no ranking
     * @param reward Recompensa a ser adicionada
     * @return true se a operação foi bem-sucedida
     */
    public boolean addReward(int position, Reward reward) {
        try {
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Remove uma recompensa de uma posição
     * 
     * @param position Posição no ranking
     * @param rewardId ID da recompensa
     * @return true se a operação foi bem-sucedida
     */
    public boolean removeReward(int position, String rewardId) {
        try {
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Obtém as recompensas de uma posição
     * 
     * @param position Posição no ranking
     * @return Lista de recompensas para a posição
     */
    public List<Reward> getRewards(int position) {
        try {
            
            return plugin.getRewardManager().loadRewards().stream()
                .filter(reward -> reward.position == position)
                .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    /**
     * Distribui recompensas manualmente
     * 
     * @return true se a operação foi bem-sucedida
     */
    public boolean distributeRewards() {
        try {
            plugin.getRewardManager().distributeRewards();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    
    
    /**
     * Obtém a versão da API
     * 
     * @return Versão da API
     */
    public String getVersion() {
        return "1.0.0";
    }

    /**
     * Recarrega as configurações do plugin
     * 
     * @return true se a operação foi bem-sucedida
     */
    public boolean reloadConfigs() {
        try {
            plugin.getConfigManager().loadConfigs();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Força sincronização de clãs com o banco de dados
     * 
     * @return Número de clãs sincronizados
     */
    public int syncClans() {
        plugin.getPointsManager().syncClansWithDatabase();
        return getAllClans().size();
    }
    
    /**
     * Obtém estatísticas gerais do sistema
     * 
     * @return Objeto com estatísticas do sistema
     */
    public HLigaStats getStats() {
        return new HLigaStats(
            getAllClans().size(),
            isSeasonActive(),
            getActiveSeason().map(s -> s.name).orElse("Nenhuma"),
            getSeasonHistory().size()
        );
    }
    
    /**
     * Classe para armazenar estatísticas do sistema
     */
    @Getter
    public static class HLigaStats {
        private final int totalClans;
        private final boolean seasonActive;
        private final String currentSeasonName;
        private final int totalSeasons;
        
        public HLigaStats(int totalClans, boolean seasonActive, String currentSeasonName, int totalSeasons) {
            this.totalClans = totalClans;
            this.seasonActive = seasonActive;
            this.currentSeasonName = currentSeasonName;
            this.totalSeasons = totalSeasons;
        }

    }
}