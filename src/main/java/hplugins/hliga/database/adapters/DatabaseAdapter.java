package hplugins.hliga.database.adapters;

import hplugins.hliga.models.ClanPoints;
import hplugins.hliga.models.PlayerTag;
import hplugins.hliga.models.Season;
import hplugins.hliga.models.TagType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Interface para adaptadores de banco de dados
 */
public interface DatabaseAdapter {

    /**
     * Inicializa o adaptador de banco de dados
     *
     * @return true se a inicialização foi bem-sucedida, false caso contrário
     */
    boolean initialize();

    /**
     * Desliga o adaptador de banco de dados
     */
    void shutdown();

    /**
     * Obtém a pontuação de um clã
     *
     * @param clanTag Tag do clã
     * @return Pontuação do clã ou 0 se não existir
     */
    int getClanPoints(String clanTag);

    /**
     * Define a pontuação de um clã
     *
     * @param clanTag Tag do clã
     * @param points Pontuação a ser definida
     * @return true se a operação foi bem-sucedida, false caso contrário
     */
    boolean setClanPoints(String clanTag, int points);

    /**
     * Verifica se um clã existe no banco de dados
     *
     * @param clanTag Tag do clã
     * @return true se o clã existe, false caso contrário
     */
    boolean clanExists(String clanTag);

    /**
     * Adiciona pontos a um clã
     *
     * @param clanTag Tag do clã
     * @param points Pontos a serem adicionados
     * @return true se a operação foi bem-sucedida, false caso contrário
     */
    boolean addClanPoints(String clanTag, int points);

    /**
     * Remove pontos de um clã
     *
     * @param clanTag Tag do clã
     * @param points Pontos a serem removidos
     * @return true se a operação foi bem-sucedida, false caso contrário
     */
    boolean removeClanPoints(String clanTag, int points);

    /**
     * Reseta os pontos de todos os clãs
     *
     * @return true se a operação foi bem-sucedida, false caso contrário
     */
    boolean resetAllPoints();

    /**
     * Obtém os clãs com maior pontuação
     *
     * @param limit Limite de resultados
     * @return Lista de clãs ordenados por pontuação
     */
    List<ClanPoints> getTopClans(int limit);

    /**
     * Salva ou atualiza uma temporada
     *
     * @param season Temporada a ser salva
     * @return true se a operação foi bem-sucedida, false caso contrário
     */
    boolean saveSeason(Season season);

    /**
     * Obtém uma temporada pelo ID
     *
     * @param id ID da temporada
     * @return Temporada encontrada ou Optional vazio
     */
    Optional<Season> getSeason(int id);

    /**
     * Obtém a temporada ativa
     *
     * @return Temporada ativa ou Optional vazio
     */
    Optional<Season> getActiveSeason();

    /**
     * Define uma temporada como ativa
     *
     * @param seasonId ID da temporada
     * @return true se a operação foi bem-sucedida, false caso contrário
     */
    boolean setActiveSeason(int seasonId);

    /**
     * Finaliza a temporada ativa
     *
     * @return true se a operação foi bem-sucedida, false caso contrário
     */
    boolean endActiveSeason();

    /**
     * Obtém o histórico de temporadas
     *
     * @return Lista de temporadas anteriores
     */
    List<Season> getSeasonHistory();

    /**
     * Obtém todas as temporadas no banco de dados
     *
     * @return Lista com todas as temporadas
     */
    List<Season> getAllSeasons();

    /**
     * Obtém a pontuação de todos os clãs
     *
     * @return Lista de todos os clãs com suas pontuações
     */
    List<ClanPoints> getAllClanPoints();

    /**
     * Salva a pontuação de um clã, criando ou atualizando o registro
     *
     * @param clanTag Tag do clã
     * @param points Pontuação do clã
     * @return true se a operação foi bem-sucedida
     */
    boolean saveClanPoints(String clanTag, int points);

    /**
     * Reseta os pontos de todos os clãs para zero
     *
     * @return true se a operação foi bem-sucedida
     */
    boolean resetAllClanPoints();

    // MÉTODOS PARA SISTEMA DE TAGS

    /**
     * Salva uma tag de jogador no banco de dados
     *
     * @param tag Tag do jogador a ser salva
     * @return true se a operação foi bem-sucedida
     */
    boolean savePlayerTag(PlayerTag tag);

    /**
     * Obtém todas as tags de um jogador
     *
     * @param playerUuid UUID do jogador
     * @return Lista de tags do jogador
     */
    List<PlayerTag> getPlayerTags(UUID playerUuid);

    /**
     * Remove todas as tags de ranking (temporárias) do banco
     */
    boolean clearAllRankingTags();

    /**
     * Remove uma tag específica de um jogador
     *
     * @param playerUuid UUID do jogador
     * @param tagType Tipo da tag (RANKING ou SEASON)
     * @param position Posição da tag
     * @return true se a operação foi bem-sucedida
     */
    boolean removePlayerTag(UUID playerUuid, String tagType, int position);

    /**
     * Obtém a tag ativa de um jogador por tipo
     *
     * @param playerUuid UUID do jogador
     * @param tagType Tipo da tag
     * @return Optional com a tag se encontrada
     */
    Optional<PlayerTag> getActivePlayerTag(UUID playerUuid, TagType tagType);

    /**
     * Obtém todas as tags de um tipo específico
     *
     * @param tagType Tipo da tag
     * @return Lista de tags do tipo especificado
     */
    List<PlayerTag> getTagsByType(TagType tagType);

    /**
     * Remove todas as tags de ranking de todos os jogadores
     *
     * @return true se a operação foi bem-sucedida
     */
    boolean removeAllRankingTags();

    /**
     * Verifica se existem tags de temporada ativas no banco
     *
     * @return true se existem tags de temporada
     */
    boolean hasAnySeasonTags();

    /**
     * Salva a preferência de exibição de tags de um jogador
     *
     * @param playerUuid UUID do jogador
     * @param tagsEnabled true se as tags devem ser exibidas, false caso contrário
     */
    void savePlayerTagPreference(UUID playerUuid, boolean tagsEnabled);

    /**
     * Obtém a preferência de exibição de tags de um jogador
     *
     * @param playerUuid UUID do jogador
     * @return true se as tags devem ser exibidas (padrão), false caso contrário
     */
    boolean getPlayerTagPreference(UUID playerUuid);
}
