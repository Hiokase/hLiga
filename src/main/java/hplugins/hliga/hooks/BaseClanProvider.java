package hplugins.hliga.hooks;

import hplugins.hliga.models.GenericClan;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementação base para provedores de clãs.
 * Fornece implementações padrão para métodos que podem ser derivados de outros,
 * reduzindo a quantidade de código necessária para adicionar um novo provedor.
 */
public abstract class BaseClanProvider implements ClanProvider {
    
    /**
     * Obtém o clã de um jogador como GenericClan
     * 
     * @param player Jogador
     * @return Objeto GenericClan ou null se o jogador não estiver em um clã
     */
    @Override
    public GenericClan getPlayerClan(Player player) {
        String clanTag = getPlayerClanTag(player);
        if (clanTag == null) {
            return null;
        }
        return getClan(clanTag);
    }
    
    /**
     * Obtém o clã/guilda de um jogador
     * 
     * @param player Jogador
     * @return Tag do clã ou null se não pertencer a nenhum clã
     */
    @Override
    public abstract String getPlayerClanTag(Player player);
    
    /**
     * Verifica se um jogador pertence a um clã
     * 
     * @param player Jogador
     * @return true se o jogador pertencer a um clã, false caso contrário
     */
    @Override
    public boolean isPlayerInClan(Player player) {
        return getPlayerClanTag(player) != null;
    }
    
    /**
     * Verifica se um clã existe pelo tag
     * 
     * @param tag Tag do clã
     * @return true se o clã existir, false caso contrário
     */
    @Override
    public boolean clanExists(String tag) {
        return getClan(tag) != null;
    }
    
    /**
     * Obtém o nome completo de um clã.
     * Implementação padrão que usa getClan() para obter o nome.
     * 
     * @param tag Tag do clã
     * @return Nome completo do clã ou a própria tag se o clã não existir
     */
    @Override
    public String getClanName(String tag) {
        GenericClan clan = getClan(tag);
        return clan != null ? clan.getName() : tag;
    }
    
    /**
     * Obtém a tag colorida de um clã.
     * Implementação padrão que usa getClan() para obter a tag colorida.
     * 
     * @param tag Tag do clã
     * @return Tag colorida do clã ou a própria tag se o clã não existir
     */
    @Override
    public String getColoredClanTag(String tag) {
        GenericClan clan = getClan(tag);
        return clan != null ? clan.getColoredTag() : tag;
    }
    
    /**
     * Obtém o número de membros em um clã.
     * Implementação padrão que usa getClan() para obter o número de membros.
     * 
     * @param tag Tag do clã
     * @return Número de membros ou 0 se o clã não existir
     */
    @Override
    public int getClanMemberCount(String tag) {
        GenericClan clan = getClan(tag);
        return clan != null ? clan.getMemberCount() : 0;
    }
    
    /**
     * Obtém todos os jogadores online de um clã.
     * Implementação padrão que usa getClan() para obter os membros online.
     * 
     * @param tag Tag do clã
     * @return Lista de jogadores online ou lista vazia se o clã não existir
     */
    @Override
    public List<Player> getOnlineClanMembers(String tag) {
        GenericClan clan = getClan(tag);
        return clan != null ? clan.getOnlineMembers() : new ArrayList<>();
    }
    
    /**
     * Obtém todos os membros de um clã (UUIDs).
     * Implementação padrão que usa getClan() para obter os membros.
     * 
     * @param tag Tag do clã
     * @return Lista de UUIDs dos membros ou lista vazia se o clã não existir
     */
    @Override
    public List<UUID> getClanMembers(String tag) {
        GenericClan clan = getClan(tag);
        return clan != null ? clan.getMemberUUIDs() : new ArrayList<>();
    }
    
    /**
     * Obtém o nome do líder de um clã.
     * Implementação padrão que usa getClan() para obter o líder.
     * 
     * @param tag Tag do clã
     * @return Nome do líder ou null se o clã não existir
     */
    @Override
    public String getClanLeaderName(String tag) {
        GenericClan clan = getClan(tag);
        return clan != null ? clan.getLeaderName() : null;
    }
    
    /**
     * Obtém todos os clãs disponíveis.
     * Implementação padrão que converte tags em objetos GenericClan.
     * 
     * @return Lista de objetos GenericClan
     */
    @Override
    public List<GenericClan> getAllClans() {
        return getAllClanTags().stream()
                .map(this::getClan)
                .filter(clan -> clan != null)
                .collect(Collectors.toList());
    }
    
    /**
     * Verifica se o plugin implementa suporte para criação de clãs via API
     * 
     * @return true se a criação for suportada, false caso contrário
     */
    @Override
    public boolean supportsClanCreation() {
        return false;
    }
    
    /**
     * Verifica se o plugin implementa suporte para dissolução de clãs via API
     * 
     * @return true se a dissolução for suportada, false caso contrário
     */
    @Override
    public boolean supportsClanDissolution() {
        return false;
    }
    
    /**
     * Verifica se o plugin implementa suporte para adição/remoção de membros via API
     * 
     * @return true se a edição de membros for suportada, false caso contrário
     */
    @Override
    public boolean supportsMemberEditing() {
        return false;
    }
}