package hplugins.hliga.hooks.providers;

import hplugins.hliga.Main;
import hplugins.hliga.hooks.BaseClanProvider;
import hplugins.hliga.models.GenericClan;
import net.sacredlabyrinth.phaed.simpleclans.Clan;
import net.sacredlabyrinth.phaed.simpleclans.ClanPlayer;
import net.sacredlabyrinth.phaed.simpleclans.SimpleClans;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Implementação do provedor de clãs para o SimpleClans
 */
public class SimpleClansHook extends BaseClanProvider {

    private final Main plugin;
    private static final String PROVIDER_NAME = "SimpleClans";

    public SimpleClansHook(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Obtém a instância do plugin SimpleClans
     *
     * @return Instância do SimpleClans ou null se não disponível
     */
    public SimpleClans getSimpleClans() {
        return (SimpleClans) Bukkit.getPluginManager().getPlugin(PROVIDER_NAME);
    }

    @Override
    public boolean isAvailable() {
        try {
            SimpleClans sc = getSimpleClans();
            return sc != null && sc.getClanManager() != null;
        } catch (Exception e) {
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().log(Level.WARNING, "Erro ao verificar disponibilidade do SimpleClans", e);
            }
            return false;
        }
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    /**
     * Obtém o clã nativo de um jogador
     *
     * @param player Jogador
     * @return Clã do jogador ou null se não pertencer a nenhum clã
     */
    public Clan getNativePlayerClan(Player player) {
        if (!isAvailable() || player == null) {
            return null;
        }

        try {
            SimpleClans sc = getSimpleClans();
            ClanPlayer cp = sc.getClanManager().getClanPlayer(player);

            if (cp != null) {
                return cp.getClan();
            }
        } catch (Exception e) {
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().log(Level.WARNING, "Erro ao obter clã do jogador no SimpleClans", e);
            }
        }

        return null;
    }

    @Override
    public String getPlayerClanTag(Player player) {
        Clan clan = getNativePlayerClan(player);
        return clan != null ? clan.getTag() : null;
    }

    /**
     * Obtém um clã nativo pela tag
     *
     * @param tag Tag do clã
     * @return Clã ou null se não existir
     */
    public Clan getNativeClanByTag(String tag) {
        if (!isAvailable() || tag == null) {
            return null;
        }

        try {
            SimpleClans sc = getSimpleClans();
            return sc.getClanManager().getClan(tag);
        } catch (Exception e) {
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().log(Level.WARNING, "Erro ao obter clã pela tag no SimpleClans", e);
            }
            return null;
        }
    }

    /**
     * Obtém todos os clãs nativos
     *
     * @return Lista de todos os clãs
     */
    public List<Clan> getAllNativeClans() {
        if (!isAvailable()) {
            return new ArrayList<>();
        }

        try {
            SimpleClans sc = getSimpleClans();
            return sc.getClanManager().getClans();
        } catch (Exception e) {
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().log(Level.WARNING, "Erro ao obter todos os clãs do SimpleClans", e);
            }
            return new ArrayList<>();
        }
    }

    @Override
    public List<String> getAllClanTags() {
        return getAllNativeClans().stream()
                .map(Clan::getTag)
                .collect(Collectors.toList());
    }

    @Override
    public GenericClan getClan(String tag) {
        Clan nativeClan = getNativeClanByTag(tag);
        if (nativeClan == null) {
            return null;
        }

        return convertToGenericClan(nativeClan);
    }

    @Override
    public boolean isPlayerLeader(Player player) {
        if (!isAvailable() || player == null) {
            return false;
        }

        try {
            SimpleClans sc = getSimpleClans();
            ClanPlayer cp = sc.getClanManager().getClanPlayer(player);

            return cp != null && cp.isLeader();
        } catch (Exception e) {
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().log(Level.WARNING, "Erro ao verificar se jogador é líder no SimpleClans", e);
            }
            return false;
        }
    }

    @Override
    public List<UUID> getClanMembers(String tag) {
        Clan clan = getNativeClanByTag(tag);
        if (clan == null) {
            return new ArrayList<>();
        }

        try {
            List<UUID> uuids = new ArrayList<>();
            for (ClanPlayer member : clan.getMembers()) {
                try {
                    // O método getUniqueId pode retornar um UUID ou uma String, tratamos ambos os casos
                    Object uuidObj = member.getUniqueId();
                    UUID uuid = null;

                    if (uuidObj instanceof UUID) {
                        uuid = (UUID) uuidObj;
                    } else if (uuidObj instanceof String) {
                        String uuidStr = (String) uuidObj;
                        if (uuidStr != null && !uuidStr.isEmpty()) {
                            uuid = UUID.fromString(uuidStr);
                        }
                    }

                    if (uuid != null) {
                        uuids.add(uuid);
                    }
                } catch (Exception e) {
                    if (plugin.getConfig().getBoolean("debug", false)) {
                        plugin.getLogger().log(Level.WARNING, "Erro ao converter UUID: " + String.valueOf(member.getUniqueId()), e);
                    }
                }
            }
            return uuids;
        } catch (Exception e) {
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().log(Level.WARNING, "Erro ao obter membros do clã no SimpleClans", e);
            }
            return new ArrayList<>();
        }
    }

    @Override
    public String getClanLeaderName(String tag) {
        Clan clan = getNativeClanByTag(tag);
        if (clan == null) {
            return null;
        }

        try {
            List<ClanPlayer> leaders = clan.getLeaders();
            if (!leaders.isEmpty()) {
                return leaders.get(0).getName();
            }
        } catch (Exception e) {
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().log(Level.WARNING, "Erro ao obter líder do clã no SimpleClans", e);
            }
        }

        return null;
    }

    @Override
    public boolean supportsClanCreation() {
        return true;
    }

    @Override
    public boolean supportsClanDissolution() {
        return true;
    }

    @Override
    public boolean supportsMemberEditing() {
        return true;
    }

    @Override
    public String getColoredClanTag(String tag) {
        if (!isAvailable()) {
            return tag;
        }

        try {
            Clan clan = getNativeClanByTag(tag);
            if (clan != null) {
                // No SimpleClans, podemos usar colorTag ou uma tag com o prefixo de cor do clã
                String colorTag = clan.getColorTag();
                if (colorTag != null && !colorTag.isEmpty()) {
                    return colorTag;
                }

                // Alternativa: usar a capa do clã para obter a cor
                try {
                    String clanColorCode = clan.getColorTag().substring(0, 2);
                    return clanColorCode + clan.getTag();
                } catch (Exception e) {
                    // Se não conseguir obter a cor, retorna a tag normal
                }
            }
            return tag;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao obter tag colorida do clã: " + tag, e);
            return tag;
        }
    }

    @Override
    public GenericClan convertToGenericClan(Object nativeObject) {
        if (!(nativeObject instanceof Clan)) {
            return null;
        }

        try {
            Clan clan = (Clan) nativeObject;

            // Construir objeto GenericClan com dados do SimpleClans
            List<UUID> memberUUIDs = new ArrayList<>();
            for (ClanPlayer member : clan.getMembers()) {
                try {
                    // O método getUniqueId pode retornar um UUID ou uma String, tratamos ambos os casos
                    Object uuidObj = member.getUniqueId();
                    UUID uuid = null;

                    if (uuidObj instanceof UUID) {
                        uuid = (UUID) uuidObj;
                    } else if (uuidObj instanceof String) {
                        String uuidStr = (String) uuidObj;
                        if (uuidStr != null && !uuidStr.isEmpty()) {
                            uuid = UUID.fromString(uuidStr);
                        }
                    }

                    if (uuid != null) {
                        memberUUIDs.add(uuid);
                    }
                } catch (Exception e) {
                    if (plugin.getConfig().getBoolean("debug", false)) {
                        plugin.getLogger().log(Level.WARNING, "Erro ao converter UUID: " + String.valueOf(member.getUniqueId()), e);
                    }
                }
            }

            List<Player> onlineMembers = clan.getOnlineMembers().stream()
                    .map(ClanPlayer::toPlayer)
                    .filter(player -> player != null)
                    .collect(Collectors.toList());

            String leaderName = null;
            List<ClanPlayer> leaders = clan.getLeaders();
            if (!leaders.isEmpty()) {
                leaderName = leaders.get(0).getName();
            }

            return new GenericClan(
                    clan.getTag(),
                    clan.getName(),
                    clan.getColorTag(),
                    memberUUIDs,
                    onlineMembers,
                    leaderName,
                    clan,
                    PROVIDER_NAME
            );
        } catch (Exception e) {
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().log(Level.WARNING, "Erro ao converter clã nativo para GenericClan", e);
            }
            return null;
        }
    }
}