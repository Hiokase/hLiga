package hplugins.hliga.models;

import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Classe que representa um clã genérico independente da implementação
 * Abstrai as diferenças entre diferentes plugins de clãs (SimpleClans, LeafGuilds, etc)
 */
@Getter
public class GenericClan {

    /**
     * -- GETTER --
     *  Obtém a tag do clã
     *
     * @return Tag do clã
     */
    private final String tag;
    /**
     * -- GETTER --
     *  Obtém o nome do clã
     *
     * @return Nome do clã
     */
    private final String name;
    /**
     * -- GETTER --
     *  Obtém a tag colorida do clã
     *
     * @return Tag colorida
     */

    private final String coloredTag;
    /**
     * -- GETTER --
     *  Obtém os UUIDs de todos os membros
     *
     * @return Lista de UUIDs
     */
    private final List<UUID> memberUUIDs;
    /**
     * -- GETTER --
     *  Obtém a lista de jogadores online
     *
     * @return Lista de jogadores
     */
    private final List<Player> onlineMembers;
    /**
     * -- GETTER --
     *  Obtém o nome do líder do clã
     *
     * @return Nome do líder
     */
    private final String leaderName;
    private final Object nativeObject;
    /**
     * -- GETTER --
     *  Obtém o nome do provedor que criou este clã
     *
     * @return Nome do provedor
     */
    private final String providerName;

    /**
     * Construtor para um clã genérico
     *
     * @param tag Tag do clã (identificador único)
     * @param name Nome do clã
     * @param coloredTag Tag do clã com cores
     * @param memberUUIDs Lista de UUIDs dos membros
     * @param onlineMembers Lista de jogadores online
     * @param leaderName Nome do líder do clã
     * @param nativeObject Objeto nativo do plugin (opcional, para conversão reversa)
     * @param providerName Nome do provedor que criou este clã
     */
    public GenericClan(String tag, String name, String coloredTag, List<UUID> memberUUIDs,
                       List<Player> onlineMembers, String leaderName, Object nativeObject, String providerName) {
        this.tag = tag;
        this.name = name;
        this.coloredTag = coloredTag;
        this.memberUUIDs = memberUUIDs;
        this.onlineMembers = onlineMembers;
        this.leaderName = leaderName;
        this.nativeObject = nativeObject;
        this.providerName = providerName;
    }

    /**
     * Construtor simplificado para um clã genérico
     *
     * @param tag Tag do clã (identificador único)
     * @param name Nome do clã
     */
    public GenericClan(String tag, String name) {
        this.tag = tag;
        this.name = name;
        this.coloredTag = null;
        this.memberUUIDs = new ArrayList<>();
        this.onlineMembers = new ArrayList<>();
        this.leaderName = null;
        this.nativeObject = null;
        this.providerName = "LeafGuilds";
    }

    /**
     * Obtém os UUIDs de todos os membros (alias para getMemberUUIDs)
     *
     * @return Lista de UUIDs de todos os membros
     */
    public List<UUID> getAllMemberUUIDs() {
        return getMemberUUIDs();
    }

    /**
     * Obtém o número de membros no clã
     *
     * @return Número de membros
     */
    public int getMemberCount() {
        return memberUUIDs.size();
    }

    /**
     * Obtém o número de membros online
     *
     * @return Número de membros online
     */
    public int getOnlineMemberCount() {
        return onlineMembers.size();
    }

    /**
     * Verifica se um jogador é membro deste clã
     *
     * @param playerUUID UUID do jogador
     * @return true se for membro, false caso contrário
     */
    public boolean isMember(UUID playerUUID) {
        return memberUUIDs.contains(playerUUID);
    }

    /**
     * Verifica se um jogador está online neste clã
     *
     * @param player Jogador
     * @return true se estiver online, false caso contrário
     */
    public boolean isOnline(Player player) {
        return onlineMembers.contains(player);
    }

    /**
     * Obtém o objeto nativo deste clã
     * Utilizado para conversão reversa quando necessário
     *
     * @param <T> Tipo do objeto nativo
     * @return Objeto nativo do clã
     */
    @SuppressWarnings("unchecked")
    public <T> T getNativeObject() {
        return (T) nativeObject;
    }

    /**
     * Verifica se este clã tem um objeto nativo
     *
     * @return true se tiver um objeto nativo, false caso contrário
     */
    public boolean hasNativeObject() {
        return nativeObject != null;
    }

    /**
     * Método toString para debug
     */
    @Override
    public String toString() {
        return "GenericClan{" +
                "tag='" + tag + '\'' +
                ", name='" + name + '\'' +
                ", memberCount=" + getMemberCount() +
                ", onlineCount=" + getOnlineMemberCount() +
                ", providerName='" + providerName + '\'' +
                '}';
    }
}