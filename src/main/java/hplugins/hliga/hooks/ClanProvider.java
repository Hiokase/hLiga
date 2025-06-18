package hplugins.hliga.hooks;

import hplugins.hliga.models.GenericClan;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/**
 * Interface para provedores de clãs/guildas.
 * Abstrai a implementação de diferentes plugins de clãs (SimpleClans, LeafGuilds, etc).
 * Esta interface foi projetada para ser facilmente implementada para qualquer plugin de clãs.
 */
public interface ClanProvider {

    /**
     * Verifica se o provedor está disponível
     * 
     * @return true se o provedor estiver disponível, false caso contrário
     */
    boolean isAvailable();
    
    /**
     * Obtém o nome do plugin de clãs
     * 
     * @return Nome do plugin
     */
    String getProviderName();
    
    /**
     * Obtém o objeto do clã na forma de GenericClan
     * Este método abstrai as diferenças entre implementações
     * 
     * @param tag Tag do clã
     * @return Objeto GenericClan ou null se não existir
     */
    GenericClan getClan(String tag);
    
    /**
     * Obtém o clã de um jogador como GenericClan
     * 
     * @param player Jogador
     * @return Objeto GenericClan ou null se o jogador não estiver em um clã
     */
    GenericClan getPlayerClan(Player player);
    
    /**
     * Obtém o clã/guilda de um jogador
     * 
     * @param player Jogador
     * @return Tag do clã ou null se não pertencer a nenhum clã
     */
    String getPlayerClanTag(Player player);
    
    /**
     * Verifica se um clã existe pelo tag
     * 
     * @param tag Tag do clã
     * @return true se o clã existir, false caso contrário
     */
    boolean clanExists(String tag);
    
    /**
     * Obtém o nome completo de um clã
     * 
     * @param tag Tag do clã
     * @return Nome completo do clã ou a própria tag se o clã não existir
     */
    String getClanName(String tag);
    
    /**
     * Obtém a tag colorida de um clã
     * 
     * @param tag Tag do clã
     * @return Tag colorida do clã ou a própria tag se o clã não existir
     */
    String getColoredClanTag(String tag);
    
    /**
     * Obtém o número de membros em um clã
     * 
     * @param tag Tag do clã
     * @return Número de membros ou 0 se o clã não existir
     */
    int getClanMemberCount(String tag);
    
    /**
     * Obtém todos os jogadores online de um clã
     * 
     * @param tag Tag do clã
     * @return Lista de jogadores online ou lista vazia se o clã não existir
     */
    List<Player> getOnlineClanMembers(String tag);
    
    /**
     * Obtém todos os tags de clãs/guildas disponíveis
     * 
     * @return Lista de tags de clãs
     */
    List<String> getAllClanTags();
    
    /**
     * Obtém todos os clãs disponíveis
     * 
     * @return Lista de objetos GenericClan
     */
    List<GenericClan> getAllClans();
    
    /**
     * Verifica se um jogador pertence a um clã
     * 
     * @param player Jogador
     * @return true se o jogador pertencer a um clã, false caso contrário
     */
    boolean isPlayerInClan(Player player);
    
    /**
     * Obtém todos os membros de um clã (UUIDs)
     * 
     * @param tag Tag do clã
     * @return Lista de UUIDs dos membros ou lista vazia se o clã não existir
     */
    List<UUID> getClanMembers(String tag);
    
    /**
     * Verifica se um jogador é líder de um clã
     * 
     * @param player Jogador
     * @return true se o jogador for líder, false caso contrário
     */
    boolean isPlayerLeader(Player player);
    
    /**
     * Obtém o nome do líder de um clã
     * 
     * @param tag Tag do clã
     * @return Nome do líder ou null se o clã não existir
     */
    String getClanLeaderName(String tag);
    
    /**
     * Verifica se o plugin implementa suporte para criação de clãs via API
     * 
     * @return true se a criação for suportada, false caso contrário
     */
    default boolean supportsClanCreation() {
        return false;
    }
    
    /**
     * Verifica se o plugin implementa suporte para dissolução de clãs via API
     * 
     * @return true se a dissolução for suportada, false caso contrário
     */
    default boolean supportsClanDissolution() {
        return false;
    }
    
    /**
     * Verifica se o plugin implementa suporte para adição/remoção de membros via API
     * 
     * @return true se a edição de membros for suportada, false caso contrário
     */
    default boolean supportsMemberEditing() {
        return false;
    }
    
    /**
     * Método de conveniência para converter objetos nativos
     * para o formato GenericClan
     * 
     * @param nativeObject Objeto nativo do plugin
     * @return Objeto GenericClan com os dados do objeto nativo
     */
    GenericClan convertToGenericClan(Object nativeObject);
}